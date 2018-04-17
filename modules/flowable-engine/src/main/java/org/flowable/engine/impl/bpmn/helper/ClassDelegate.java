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

package org.flowable.engine.impl.bpmn.helper;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.MapExceptionEntry;
import org.flowable.bpmn.model.Task;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.engine.DynamicBpmnConstants;
import org.flowable.engine.delegate.BpmnError;
import org.flowable.engine.delegate.CustomPropertiesResolver;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.engine.delegate.TransactionDependentExecutionListener;
import org.flowable.engine.delegate.TransactionDependentTaskListener;
import org.flowable.engine.impl.bpmn.behavior.AbstractBpmnActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.ServiceTaskJavaDelegateActivityBehavior;
import org.flowable.engine.impl.bpmn.parser.FieldDeclaration;
import org.flowable.engine.impl.context.BpmnOverrideContext;
import org.flowable.engine.impl.delegate.ActivityBehavior;
import org.flowable.engine.impl.delegate.SubProcessActivityBehavior;
import org.flowable.engine.impl.delegate.TriggerableActivityBehavior;
import org.flowable.engine.impl.delegate.invocation.ExecutionListenerInvocation;
import org.flowable.engine.impl.delegate.invocation.TaskListenerInvocation;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.task.service.delegate.DelegateTask;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Helper class for bpmn constructs that allow class delegation.
 * 
 * This class will lazily instantiate the referenced classes when needed at runtime.
 * 
 * @author Joram Barrez
 * @author Falko Menge
 * @author Saeid Mirzaei
 * @author Yvo Swillens
 */
public class ClassDelegate extends AbstractClassDelegate implements TaskListener, ExecutionListener, TransactionDependentExecutionListener, TransactionDependentTaskListener, SubProcessActivityBehavior, CustomPropertiesResolver {

    private static final long serialVersionUID = 1L;

    protected ActivityBehavior activityBehaviorInstance;
    protected Expression skipExpression;
    protected List<MapExceptionEntry> mapExceptions;
    protected CustomPropertiesResolver customPropertiesResolverInstance;
    protected boolean triggerable;

    public ClassDelegate(String className, List<FieldDeclaration> fieldDeclarations, Expression skipExpression) {
        super(className, fieldDeclarations);
        this.skipExpression = skipExpression;
    }

    public ClassDelegate(String id, String className, List<FieldDeclaration> fieldDeclarations, boolean triggerable, Expression skipExpression,
                         List<MapExceptionEntry> mapExceptions) {
        this(className, fieldDeclarations, skipExpression);
        this.triggerable = triggerable;
        this.serviceTaskId = id;
        this.mapExceptions = mapExceptions;
    }

    public ClassDelegate(Class<?> clazz, List<FieldDeclaration> fieldDeclarations, Expression skipExpression) {
        this(clazz.getName(), fieldDeclarations, skipExpression);
    }
    
    public ClassDelegate(String className, List<FieldDeclaration> fieldDeclarations) {
        super(className, fieldDeclarations);
    }
    
    public ClassDelegate(Class<?> clazz, List<FieldDeclaration> fieldDeclarations) {
        super(clazz, fieldDeclarations);
    }

    // Execution listener
    @Override
    public void notify(DelegateExecution execution) {
        ExecutionListener executionListenerInstance = getExecutionListenerInstance();
        CommandContextUtil.getProcessEngineConfiguration().getDelegateInterceptor().handleInvocation(new ExecutionListenerInvocation(executionListenerInstance, execution));
    }

    // Transaction Dependent execution listener
    @Override
    public void notify(String processInstanceId, String executionId, FlowElement flowElement, Map<String, Object> executionVariables, Map<String, Object> customPropertiesMap) {
        TransactionDependentExecutionListener transactionDependentExecutionListenerInstance = getTransactionDependentExecutionListenerInstance();

        // Note that we can't wrap it in the delegate interceptor like usual here due to being executed when the context is already removed.
        transactionDependentExecutionListenerInstance.notify(processInstanceId, executionId, flowElement, executionVariables, customPropertiesMap);
    }

    @Override
    public Map<String, Object> getCustomPropertiesMap(DelegateExecution execution) {
        if (customPropertiesResolverInstance == null) {
            customPropertiesResolverInstance = getCustomPropertiesResolverInstance();
        }
        return customPropertiesResolverInstance.getCustomPropertiesMap(execution);
    }

    // Task listener
    @Override
    public void notify(DelegateTask delegateTask) {
        TaskListener taskListenerInstance = getTaskListenerInstance();

        try {
            CommandContextUtil.getProcessEngineConfiguration().getDelegateInterceptor().handleInvocation(new TaskListenerInvocation(taskListenerInstance, delegateTask));
        } catch (Exception e) {
            throw new FlowableException("Exception while invoking TaskListener: " + e.getMessage(), e);
        }
    }

    @Override
    public void notify(String processInstanceId, String executionId, Task task, Map<String, Object> executionVariables, Map<String, Object> customPropertiesMap) {
        TransactionDependentTaskListener transactionDependentTaskListenerInstance = getTransactionDependentTaskListenerInstance();
        transactionDependentTaskListenerInstance.notify(processInstanceId, executionId, task, executionVariables, customPropertiesMap);
    }

    protected ExecutionListener getExecutionListenerInstance() {
        Object delegateInstance = instantiateDelegate(className, fieldDeclarations);
        if (delegateInstance instanceof ExecutionListener) {
            return (ExecutionListener) delegateInstance;
        } else if (delegateInstance instanceof JavaDelegate) {
            return new ServiceTaskJavaDelegateActivityBehavior((JavaDelegate) delegateInstance, triggerable, skipExpression);
        } else {
            throw new FlowableIllegalArgumentException(delegateInstance.getClass().getName() + " doesn't implement " + ExecutionListener.class + " nor " + JavaDelegate.class);
        }
    }

    protected TransactionDependentExecutionListener getTransactionDependentExecutionListenerInstance() {
        Object delegateInstance = instantiateDelegate(className, fieldDeclarations);
        if (delegateInstance instanceof TransactionDependentExecutionListener) {
            return (TransactionDependentExecutionListener) delegateInstance;
        } else {
            throw new FlowableIllegalArgumentException(delegateInstance.getClass().getName() + " doesn't implement " + TransactionDependentExecutionListener.class);
        }
    }

    protected CustomPropertiesResolver getCustomPropertiesResolverInstance() {
        Object delegateInstance = instantiateDelegate(className, fieldDeclarations);
        if (delegateInstance instanceof CustomPropertiesResolver) {
            return (CustomPropertiesResolver) delegateInstance;
        } else {
            throw new FlowableIllegalArgumentException(delegateInstance.getClass().getName() + " doesn't implement " + CustomPropertiesResolver.class);
        }
    }

    protected TaskListener getTaskListenerInstance() {
        Object delegateInstance = instantiateDelegate(className, fieldDeclarations);
        if (delegateInstance instanceof TaskListener) {
            return (TaskListener) delegateInstance;
        } else {
            throw new FlowableIllegalArgumentException(delegateInstance.getClass().getName() + " doesn't implement " + TaskListener.class);
        }
    }

    protected TransactionDependentTaskListener getTransactionDependentTaskListenerInstance() {
        Object delegateInstance = instantiateDelegate(className, fieldDeclarations);
        if (delegateInstance instanceof TransactionDependentTaskListener) {
            return (TransactionDependentTaskListener) delegateInstance;
        } else {
            throw new FlowableIllegalArgumentException(delegateInstance.getClass().getName() + " doesn't implement " + TransactionDependentTaskListener.class);
        }
    }

    // Activity Behavior
    @Override
    public void execute(DelegateExecution execution) {
        if (CommandContextUtil.getProcessEngineConfiguration().isEnableProcessDefinitionInfoCache()) {
            ObjectNode taskElementProperties = BpmnOverrideContext.getBpmnOverrideElementProperties(serviceTaskId, execution.getProcessDefinitionId());
            if (taskElementProperties != null && taskElementProperties.has(DynamicBpmnConstants.SERVICE_TASK_CLASS_NAME)) {
                String overrideClassName = taskElementProperties.get(DynamicBpmnConstants.SERVICE_TASK_CLASS_NAME).asText();
                if (StringUtils.isNotEmpty(overrideClassName) && !overrideClassName.equals(className)) {
                    className = overrideClassName;
                    activityBehaviorInstance = null;
                }
            }
        }

        if (activityBehaviorInstance == null) {
            activityBehaviorInstance = getActivityBehaviorInstance();
        }

        try {
            activityBehaviorInstance.execute(execution);
        } catch (BpmnError error) {
            ErrorPropagation.propagateError(error, execution);
        } catch (RuntimeException e) {
            if (!ErrorPropagation.mapException(e, (ExecutionEntity) execution, mapExceptions))
                throw e;
        }
    }

    // Signallable activity behavior
    @Override
    public void trigger(DelegateExecution execution, String signalName, Object signalData) {
        if (activityBehaviorInstance == null) {
            activityBehaviorInstance = getActivityBehaviorInstance();
        }

        if (activityBehaviorInstance instanceof TriggerableActivityBehavior) {
            ((TriggerableActivityBehavior) activityBehaviorInstance).trigger(execution, signalName, signalData);
            if(triggerable) {
                leave(execution);
            }
        } else {
            throw new FlowableException("signal() can only be called on a " + TriggerableActivityBehavior.class.getName() + " instance");
        }
    }

    // Subprocess activityBehaviour

    @Override
    public void completing(DelegateExecution execution, DelegateExecution subProcessInstance) throws Exception {
        if (activityBehaviorInstance == null) {
            activityBehaviorInstance = getActivityBehaviorInstance();
        }

        if (activityBehaviorInstance instanceof SubProcessActivityBehavior) {
            ((SubProcessActivityBehavior) activityBehaviorInstance).completing(execution, subProcessInstance);
        } else {
            throw new FlowableException("completing() can only be called on a " + SubProcessActivityBehavior.class.getName() + " instance");
        }
    }

    @Override
    public void completed(DelegateExecution execution) throws Exception {
        if (activityBehaviorInstance == null) {
            activityBehaviorInstance = getActivityBehaviorInstance();
        }

        if (activityBehaviorInstance instanceof SubProcessActivityBehavior) {
            ((SubProcessActivityBehavior) activityBehaviorInstance).completed(execution);
        } else {
            throw new FlowableException("completed() can only be called on a " + SubProcessActivityBehavior.class.getName() + " instance");
        }
    }

    protected ActivityBehavior getActivityBehaviorInstance() {
        Object delegateInstance = instantiateDelegate(className, fieldDeclarations);

        if (delegateInstance instanceof ActivityBehavior) {
            return determineBehaviour((ActivityBehavior) delegateInstance);
        } else if (delegateInstance instanceof JavaDelegate) {
            return determineBehaviour(new ServiceTaskJavaDelegateActivityBehavior((JavaDelegate) delegateInstance, triggerable, skipExpression));
        } else {
            throw new FlowableIllegalArgumentException(delegateInstance.getClass().getName() + " doesn't implement " + JavaDelegate.class.getName() + " nor " + ActivityBehavior.class.getName());
        }
    }

    // Adds properties to the given delegation instance (eg multi instance) if needed
    protected ActivityBehavior determineBehaviour(ActivityBehavior delegateInstance) {
        if (hasMultiInstanceCharacteristics()) {
            multiInstanceActivityBehavior.setInnerActivityBehavior((AbstractBpmnActivityBehavior) delegateInstance);
            return multiInstanceActivityBehavior;
        }
        return delegateInstance;
    }

}
