/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.cdi.impl.context;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.inject.Scope;

import org.flowable.cdi.FlowableCdiException;
import org.flowable.cdi.impl.util.ProgrammaticBeanLookup;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.impl.context.ExecutionContext;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.runtime.Execution;
import org.flowable.task.api.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of the business process association manager. Uses a fallback-strategy to associate the process instance with the "broadest" active scope, starting with the conversation.
 * <p/>
 * Subclass in order to implement custom association schemes and association with custom scopes.
 *
 * @author Daniel Meyer
 */
@SuppressWarnings("serial")
public class DefaultContextAssociationManager implements ContextAssociationManager, Serializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultContextAssociationManager.class);

    protected static class ScopedAssociation {

        @Inject
        private RuntimeService runtimeService;

        protected Map<String, Object> cachedVariables = new HashMap<>();
        protected Execution execution;
        protected Task task;

        public Execution getExecution() {
            return execution;
        }

        public void setExecution(Execution execution) {
            this.execution = execution;
        }

        public Task getTask() {
            return task;
        }

        public void setTask(Task task) {
            this.task = task;
        }

        public <T> T getVariable(String variableName) {
            Object value = cachedVariables.get(variableName);
            if (value == null) {
                if (execution != null) {
                    value = runtimeService.getVariable(execution.getId(), variableName);
                    cachedVariables.put(variableName, value);
                }
            }
            return (T) value;
        }

        public void setVariable(String variableName, Object value) {
            cachedVariables.put(variableName, value);
        }

        public Map<String, Object> getCachedVariables() {
            return cachedVariables;
        }

    }

    @ConversationScoped
    protected static class ConversationScopedAssociation extends ScopedAssociation implements Serializable {
    }

    @RequestScoped
    protected static class RequestScopedAssociation extends ScopedAssociation implements Serializable {
    }

    @Inject
    private BeanManager beanManager;

    protected Class<? extends ScopedAssociation> getBroadestActiveContext() {
        for (Class<? extends ScopedAssociation> scopeType : getAvailableScopedAssociationClasses()) {
            Annotation scopeAnnotation = scopeType.getAnnotations().length > 0 ? scopeType.getAnnotations()[0] : null;
            if (scopeAnnotation == null || !beanManager.isScope(scopeAnnotation.annotationType())) {
                throw new FlowableException("ScopedAssociation must carry exactly one annotation and it must be a @Scope annotation");
            }
            try {
                beanManager.getContext(scopeAnnotation.annotationType());
                return scopeType;
            } catch (ContextNotActiveException e) {
                LOGGER.trace("Context {} not active.", scopeAnnotation.annotationType());
            }
        }
        throw new FlowableException("Could not determine an active context to associate the current process instance / task instance with.");
    }

    /**
     * Override to add different / additional contexts.
     *
     * @return a list of {@link Scope}-types, which are used in the given order to resolve the broadest active context (@link #getBroadestActiveContext()})
     */
    protected List<Class<? extends ScopedAssociation>> getAvailableScopedAssociationClasses() {
        ArrayList<Class<? extends ScopedAssociation>> scopeTypes = new ArrayList<>();
        scopeTypes.add(ConversationScopedAssociation.class);
        scopeTypes.add(RequestScopedAssociation.class);
        return scopeTypes;
    }

    protected ScopedAssociation getScopedAssociation() {
        return ProgrammaticBeanLookup.lookup(getBroadestActiveContext(), beanManager);
    }

    @Override
    public void setExecution(Execution execution) {
        if (execution == null) {
            throw new FlowableCdiException("Cannot associate with execution: null");
        }

        if (Context.getCommandContext() != null) {
            throw new FlowableCdiException("Cannot work with scoped associations inside command context.");
        }

        ScopedAssociation scopedAssociation = getScopedAssociation();
        Execution associatedExecution = scopedAssociation.getExecution();
        if (associatedExecution != null && !associatedExecution.getId().equals(execution.getId())) {
            throw new FlowableCdiException("Cannot associate " + execution + ", already associated with " + associatedExecution + ". Disassociate first!");
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Associating {} (@{})", execution, scopedAssociation.getClass().getAnnotations()[0].annotationType().getSimpleName());
        }
        scopedAssociation.setExecution(execution);
    }

    @Override
    public void disAssociate() {
        if (Context.getCommandContext() != null) {
            throw new FlowableCdiException("Cannot work with scoped associations inside command context.");
        }
        ScopedAssociation scopedAssociation = getScopedAssociation();
        if (scopedAssociation.getExecution() == null) {
            throw new FlowableException("Cannot disassociate execution, no " + scopedAssociation.getClass().getAnnotations()[0].annotationType().getSimpleName() + " execution associated. ");
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Disassociating");
        }
        scopedAssociation.setExecution(null);
        scopedAssociation.setTask(null);
    }

    @Override
    public String getExecutionId() {
        Execution execution = getExecution();
        if (execution != null) {
            return execution.getId();
        } else {
            return null;
        }
    }

    @Override
    public Execution getExecution() {
        ExecutionEntity execution = getExecutionFromContext();
        if (execution != null) {
            return execution;
        } else {
            return getScopedAssociation().getExecution();
        }
    }

    @Override
    public Object getVariable(String variableName) {
        ExecutionEntity execution = getExecutionFromContext();
        if (execution != null) {
            return execution.getVariable(variableName);
        } else {
            return getScopedAssociation().getVariable(variableName);
        }
    }

    @Override
    public void setVariable(String variableName, Object value) {
        ExecutionEntity execution = getExecutionFromContext();
        if (execution != null) {
            execution.setVariable(variableName, value);
            execution.getVariable(variableName);
        } else {
            getScopedAssociation().setVariable(variableName, value);
        }
    }

    protected ExecutionEntity getExecutionFromContext() {
        if (Context.getCommandContext() != null) {
            ExecutionContext executionContext = ExecutionContextHolder.getExecutionContext();
            if (executionContext != null) {
                return executionContext.getExecution();
            }
        }
        return null;
    }

    @Override
    public Task getTask() {
        if (Context.getCommandContext() != null) {
            throw new FlowableCdiException("Cannot work with tasks in an active command.");
        }
        return getScopedAssociation().getTask();
    }

    @Override
    public void setTask(Task task) {
        if (Context.getCommandContext() != null) {
            throw new FlowableCdiException("Cannot work with tasks in an active command.");
        }
        getScopedAssociation().setTask(task);
    }

    @Override
    public Map<String, Object> getCachedVariables() {
        if (Context.getCommandContext() != null) {
            throw new FlowableCdiException("Cannot work with cached variables in an active command.");
        }
        return getScopedAssociation().getCachedVariables();
    }

}
