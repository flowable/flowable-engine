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

package org.flowable.engine.impl.bpmn.behavior;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.CollectionHandler;
import org.flowable.bpmn.model.CompensateEventDefinition;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.ImplementationType;
import org.flowable.bpmn.model.Process;
import org.flowable.engine.DynamicBpmnConstants;
import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.api.delegate.Expression;
import org.flowable.engine.common.api.delegate.event.FlowableEngineEventType;
import org.flowable.engine.common.impl.el.ExpressionManager;
import org.flowable.engine.common.impl.util.CollectionUtil;
import org.flowable.engine.delegate.BpmnError;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.delegate.event.FlowableMultiInstanceActivityCompletedEvent;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.bpmn.helper.ClassDelegateCollectionHandler;
import org.flowable.engine.impl.bpmn.helper.DelegateExpressionCollectionHandler;
import org.flowable.engine.impl.bpmn.helper.DelegateExpressionUtil;
import org.flowable.engine.impl.bpmn.helper.ErrorPropagation;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.context.BpmnOverrideContext;
import org.flowable.engine.impl.delegate.ActivityBehavior;
import org.flowable.engine.impl.delegate.FlowableCollectionHandler;
import org.flowable.engine.impl.delegate.SubProcessActivityBehavior;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Implementation of the multi-instance functionality as described in the BPMN 2.0 spec.
 * 
 * Multi instance functionality is implemented as an {@link ActivityBehavior} that wraps the original {@link ActivityBehavior} of the activity.
 * 
 * Only subclasses of {@link AbstractBpmnActivityBehavior} can have multi-instance behavior. As such, special logic is contained in the {@link AbstractBpmnActivityBehavior} to delegate to the
 * {@link MultiInstanceActivityBehavior} if needed.
 * 
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public abstract class MultiInstanceActivityBehavior extends FlowNodeActivityBehavior implements SubProcessActivityBehavior {

    private static final long serialVersionUID = 1L;

    protected static final Logger LOGGER = LoggerFactory.getLogger(MultiInstanceActivityBehavior.class);
    protected static final String DELETE_REASON_END = "MI_END";

    // Variable names for outer instance(as described in spec)
    protected final String NUMBER_OF_INSTANCES = "nrOfInstances";
    protected final String NUMBER_OF_ACTIVE_INSTANCES = "nrOfActiveInstances";
    protected final String NUMBER_OF_COMPLETED_INSTANCES = "nrOfCompletedInstances";

    // Instance members
    protected Activity activity;
    protected AbstractBpmnActivityBehavior innerActivityBehavior;
    protected Expression loopCardinalityExpression;
    protected String completionCondition;
    protected Expression collectionExpression;
    protected String collectionVariable; // Not used anymore. Left here for backwards compatibility.
    protected String collectionElementVariable;
    protected String collectionString;
    protected CollectionHandler collectionHandler;
    // default variable name for loop counter for inner instances (as described in the spec)
    protected String collectionElementIndexVariable = "loopCounter";

    /**
     * @param activity
     * @param innerActivityBehavior
     *            The original {@link ActivityBehavior} of the activity that will be wrapped inside this behavior.
     */
    public MultiInstanceActivityBehavior(Activity activity, AbstractBpmnActivityBehavior innerActivityBehavior) {
        this.activity = activity;
        setInnerActivityBehavior(innerActivityBehavior);
    }

    @Override
    public void execute(DelegateExecution delegateExecution) {
        ExecutionEntity execution = (ExecutionEntity) delegateExecution;
        if (getLocalLoopVariable(execution, getCollectionElementIndexVariable()) == null) {

            int nrOfInstances = 0;

            try {
                nrOfInstances = createInstances(delegateExecution);
            } catch (BpmnError error) {
                ErrorPropagation.propagateError(error, execution);
            }

            if (nrOfInstances == 0) {
                cleanupMiRoot(execution);
            }

        } else {
            // for synchronous, history was created already in ContinueMultiInstanceOperation,
            // but that would lead to wrong timings for asynchronous which is why it's here
            if (activity.isAsynchronous()) {
                CommandContextUtil.getHistoryManager().recordActivityStart((ExecutionEntity) execution);
            }
            innerActivityBehavior.execute(execution);
        }
    }

    protected abstract int createInstances(DelegateExecution execution);
    
    @Override
    public void leave(DelegateExecution execution) {
        cleanupMiRoot(execution);
    }

    protected void cleanupMiRoot(DelegateExecution execution) {
        // Delete multi instance root and all child executions.
        // Create a fresh execution to continue
        
        ExecutionEntity multiInstanceRootExecution = (ExecutionEntity) getMultiInstanceRootExecution(execution);
        FlowElement flowElement = multiInstanceRootExecution.getCurrentFlowElement();
        ExecutionEntity parentExecution = multiInstanceRootExecution.getParent();
        
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager();
        Collection<String> executionIdsNotToSendCancelledEventsFor = execution.isMultiInstanceRoot() ? null : Collections.singletonList(execution.getId());
        executionEntityManager.deleteChildExecutions(multiInstanceRootExecution, null, executionIdsNotToSendCancelledEventsFor, DELETE_REASON_END, true, flowElement);
        executionEntityManager.deleteRelatedDataForExecution(multiInstanceRootExecution, DELETE_REASON_END);
        executionEntityManager.delete(multiInstanceRootExecution);

        ExecutionEntity newExecution = executionEntityManager.createChildExecution(parentExecution);
        newExecution.setCurrentFlowElement(flowElement);
        super.leave(newExecution);
    }

    protected void executeCompensationBoundaryEvents(FlowElement flowElement, DelegateExecution execution) {

        // Execute compensation boundary events
        Collection<BoundaryEvent> boundaryEvents = findBoundaryEventsForFlowNode(execution.getProcessDefinitionId(), flowElement);
        if (CollectionUtil.isNotEmpty(boundaryEvents)) {

            // The parent execution becomes a scope, and a child execution is created for each of the boundary events
            for (BoundaryEvent boundaryEvent : boundaryEvents) {

                if (CollectionUtil.isEmpty(boundaryEvent.getEventDefinitions())) {
                    continue;
                }

                if (boundaryEvent.getEventDefinitions().get(0) instanceof CompensateEventDefinition) {
                    ExecutionEntity childExecutionEntity = CommandContextUtil.getExecutionEntityManager()
                            .createChildExecution((ExecutionEntity) execution);
                    childExecutionEntity.setParentId(execution.getId());
                    childExecutionEntity.setCurrentFlowElement(boundaryEvent);
                    childExecutionEntity.setScope(false);

                    ActivityBehavior boundaryEventBehavior = ((ActivityBehavior) boundaryEvent.getBehavior());
                    boundaryEventBehavior.execute(childExecutionEntity);
                }
            }
        }
    }

    protected Collection<BoundaryEvent> findBoundaryEventsForFlowNode(final String processDefinitionId, final FlowElement flowElement) {
        Process process = getProcessDefinition(processDefinitionId);

        // This could be cached or could be done at parsing time
        List<BoundaryEvent> results = new ArrayList<>(1);
        Collection<BoundaryEvent> boundaryEvents = process.findFlowElementsOfType(BoundaryEvent.class, true);
        for (BoundaryEvent boundaryEvent : boundaryEvents) {
            if (boundaryEvent.getAttachedToRefId() != null && boundaryEvent.getAttachedToRefId().equals(flowElement.getId())) {
                results.add(boundaryEvent);
            }
        }
        return results;
    }

    protected Process getProcessDefinition(String processDefinitionId) {
        return ProcessDefinitionUtil.getProcess(processDefinitionId);
    }

    // Intercepts signals, and delegates it to the wrapped {@link ActivityBehavior}.
    @Override
    public void trigger(DelegateExecution execution, String signalName, Object signalData) {
        innerActivityBehavior.trigger(execution, signalName, signalData);
    }

    // required for supporting embedded subprocesses
    public void lastExecutionEnded(DelegateExecution execution) {
        // ScopeUtil.createEventScopeExecution((ExecutionEntity) execution);
        leave(execution);
    }

    // required for supporting external subprocesses
    @Override
    public void completing(DelegateExecution execution, DelegateExecution subProcessInstance) throws Exception {
    }

    // required for supporting external subprocesses
    @Override
    public void completed(DelegateExecution execution) throws Exception {
        leave(execution);
    }
    
    public boolean completionConditionSatisfied(DelegateExecution execution) {
        if (completionCondition != null) {
            
            ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
            ExpressionManager expressionManager = processEngineConfiguration.getExpressionManager();
            
            String activeCompletionCondition = null;

            if (CommandContextUtil.getProcessEngineConfiguration().isEnableProcessDefinitionInfoCache()) {
                ObjectNode taskElementProperties = BpmnOverrideContext.getBpmnOverrideElementProperties(activity.getId(), execution.getProcessDefinitionId());
                activeCompletionCondition = getActiveValue(completionCondition, DynamicBpmnConstants.MULTI_INSTANCE_COMPLETION_CONDITION, taskElementProperties);

            } else {
                activeCompletionCondition = completionCondition;
            }
            
            Object value = expressionManager.createExpression(activeCompletionCondition).getValue(execution);
            
            if (!(value instanceof Boolean)) {
                throw new FlowableIllegalArgumentException("completionCondition '" + activeCompletionCondition + "' does not evaluate to a boolean value");
            }

            Boolean booleanValue = (Boolean) value;
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Completion condition of multi-instance satisfied: {}", booleanValue);
            }
            return booleanValue;
        }
        return false;
    }
    
    public Integer getLoopVariable(DelegateExecution execution, String variableName) {
        Object value = execution.getVariableLocal(variableName);
        DelegateExecution parent = execution.getParent();
        while (value == null && parent != null) {
            value = parent.getVariableLocal(variableName);
            parent = parent.getParent();
        }
        return (Integer) (value != null ? value : 0);
    }

    // Helpers
    // //////////////////////////////////////////////////////////////////////

    protected void sendCompletedWithConditionEvent(DelegateExecution execution) {
        CommandContextUtil.getEventDispatcher(CommandContextUtil.getCommandContext()).dispatchEvent(
                buildCompletedEvent(execution, FlowableEngineEventType.MULTI_INSTANCE_ACTIVITY_COMPLETED_WITH_CONDITION));
    }

    protected void sendCompletedEvent(DelegateExecution execution) {
        CommandContextUtil.getEventDispatcher(CommandContextUtil.getCommandContext()).dispatchEvent(
                buildCompletedEvent(execution, FlowableEngineEventType.MULTI_INSTANCE_ACTIVITY_COMPLETED));
    }

    protected FlowableMultiInstanceActivityCompletedEvent buildCompletedEvent(DelegateExecution execution, FlowableEngineEventType eventType) {
        FlowElement flowNode = execution.getCurrentFlowElement();

        return FlowableEventBuilder.createMultiInstanceActivityCompletedEvent(eventType,
                (int) execution.getVariable(NUMBER_OF_INSTANCES),
                (int) execution.getVariable(NUMBER_OF_ACTIVE_INSTANCES),
                (int) execution.getVariable(NUMBER_OF_COMPLETED_INSTANCES),
                flowNode.getId(),
                flowNode.getName(), execution.getId(), execution.getProcessInstanceId(), execution.getProcessDefinitionId(), flowNode);
    }

    @SuppressWarnings("rawtypes")
    protected int resolveNrOfInstances(DelegateExecution execution) {
        if (loopCardinalityExpression != null) {
            return resolveLoopCardinality(execution);

        } else if (usesCollection()) {
            Collection collection = resolveAndValidateCollection(execution);
            return collection.size();

        } else {
            throw new FlowableIllegalArgumentException("Couldn't resolve collection expression nor variable reference");
        }
    }

    @SuppressWarnings("rawtypes")
    protected void executeOriginalBehavior(DelegateExecution execution, int loopCounter) {
        if (usesCollection() && collectionElementVariable != null) {
            Collection collection = (Collection) resolveAndValidateCollection(execution);

            Object value = null;
            int index = 0;
            Iterator it = collection.iterator();
            while (index <= loopCounter) {
                value = it.next();
                index++;
            }
            setLoopVariable(execution, collectionElementVariable, value);
        }

        execution.setCurrentFlowElement(activity);
        CommandContextUtil.getAgenda().planContinueMultiInstanceOperation((ExecutionEntity) execution, loopCounter);
    }

    @SuppressWarnings("rawtypes")
    protected Collection resolveAndValidateCollection(DelegateExecution execution) {
        Object obj = resolveCollection(execution);
        if (collectionHandler != null ) {           
            return createFlowableCollectionHandler(collectionHandler, execution).resolveCollection(obj, execution);
        } else {
            if (obj instanceof Collection) {
                return (Collection) obj;
                
            } else if (obj instanceof String) {
                Object collectionVariable = execution.getVariable((String) obj);
                if (collectionVariable instanceof Collection) {
                    return (Collection) collectionVariable;
                } else if (collectionVariable == null) {
                    throw new FlowableIllegalArgumentException("Variable '" + obj + "' is not found");
                } else {
                    throw new FlowableIllegalArgumentException("Variable '" + obj + "':" + collectionVariable + " is not a Collection");
                }
                
            } else {
                throw new FlowableIllegalArgumentException("Couldn't resolve collection expression, variable reference or string");
                
            }
        }
    }

    protected Object resolveCollection(DelegateExecution execution) {
        Object collection = null;
        if (collectionExpression != null) {
            collection = collectionExpression.getValue(execution);

        } else if (collectionVariable != null) {
            collection = execution.getVariable(collectionVariable);
            
        } else if (collectionString != null) {
            collection = collectionString;
        }
        return collection;
    }

    protected boolean usesCollection() {
        return collectionExpression != null || collectionVariable != null || collectionString != null;
    }

    protected boolean isExtraScopeNeeded(FlowNode flowNode) {
        return flowNode.getSubProcess() != null;
    }

    protected int resolveLoopCardinality(DelegateExecution execution) {
        // Using Number since expr can evaluate to eg. Long (which is also the default for Juel)
        Object value = loopCardinalityExpression.getValue(execution);
        if (value instanceof Number) {
            return ((Number) value).intValue();

        } else if (value instanceof String) {
            return Integer.valueOf((String) value);

        } else {
            throw new FlowableIllegalArgumentException("Could not resolve loopCardinality expression '" + loopCardinalityExpression.getExpressionText() + "': not a number nor number String");
        }
    }

    protected void setLoopVariable(DelegateExecution execution, String variableName, Object value) {
        execution.setVariableLocal(variableName, value);
    }

    protected Integer getLocalLoopVariable(DelegateExecution execution, String variableName) {
        return (Integer) execution.getVariableLocal(variableName);
    }

    /**
     * Since no transitions are followed when leaving the inner activity, it is needed to call the end listeners yourself.
     */
    protected void callActivityEndListeners(DelegateExecution execution) {
        CommandContextUtil.getProcessEngineConfiguration().getListenerNotificationHelper()
                .executeExecutionListeners(activity, execution, ExecutionListener.EVENTNAME_END);
    }

    protected void logLoopDetails(DelegateExecution execution, String custom, int loopCounter, int nrOfCompletedInstances, int nrOfActiveInstances, int nrOfInstances) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Multi-instance '{}' {}. Details: loopCounter={}, nrOrCompletedInstances={},nrOfActiveInstances={},nrOfInstances={}",
                    execution.getCurrentFlowElement() != null ? execution.getCurrentFlowElement().getId() : "", custom, loopCounter,
                    nrOfCompletedInstances, nrOfActiveInstances, nrOfInstances);
        }
    }

    protected DelegateExecution getMultiInstanceRootExecution(DelegateExecution executionEntity) {
        DelegateExecution multiInstanceRootExecution = null;
        DelegateExecution currentExecution = executionEntity;
        while (currentExecution != null && multiInstanceRootExecution == null && currentExecution.getParent() != null) {
            if (currentExecution.isMultiInstanceRoot()) {
                multiInstanceRootExecution = currentExecution;
            } else {
                currentExecution = currentExecution.getParent();
            }
        }
        return multiInstanceRootExecution;
    }
    
    protected String getActiveValue(String originalValue, String propertyName, ObjectNode taskElementProperties) {
        String activeValue = originalValue;
        if (taskElementProperties != null) {
            JsonNode overrideValueNode = taskElementProperties.get(propertyName);
            if (overrideValueNode != null) {
                if (overrideValueNode.isNull()) {
                    activeValue = null;
                } else {
                    activeValue = overrideValueNode.asText();
                }
            }
        }
        return activeValue;
    }

    protected FlowableCollectionHandler createFlowableCollectionHandler(CollectionHandler handler, DelegateExecution execution) {
    	FlowableCollectionHandler collectionHandler = null;

        if (ImplementationType.IMPLEMENTATION_TYPE_CLASS.equalsIgnoreCase(handler.getImplementationType())) {
        	collectionHandler = new ClassDelegateCollectionHandler(handler.getImplementation(), null);
        
        } else if (ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION.equalsIgnoreCase(handler.getImplementationType())) {
        	Object delegate = DelegateExpressionUtil.resolveDelegateExpression(CommandContextUtil.getProcessEngineConfiguration().getExpressionManager().createExpression(handler.getImplementation()), execution);
            if (delegate instanceof FlowableCollectionHandler) {
                collectionHandler = new DelegateExpressionCollectionHandler(execution, CommandContextUtil.getProcessEngineConfiguration().getExpressionManager().createExpression(handler.getImplementation()));   
            } else {
                throw new FlowableIllegalArgumentException("Delegate expression " + handler.getImplementation() + " did not resolve to an implementation of " + FlowableCollectionHandler.class);
            }
        }
        return collectionHandler;
    }
    
    // Getters and Setters
    // ///////////////////////////////////////////////////////////

    public Expression getLoopCardinalityExpression() {
        return loopCardinalityExpression;
    }

    public void setLoopCardinalityExpression(Expression loopCardinalityExpression) {
        this.loopCardinalityExpression = loopCardinalityExpression;
    }

    public String getCompletionCondition() {
        return completionCondition;
    }

    public void setCompletionCondition(String completionCondition) {
        this.completionCondition = completionCondition;
    }

    public Expression getCollectionExpression() {
        return collectionExpression;
    }

    public void setCollectionExpression(Expression collectionExpression) {
        this.collectionExpression = collectionExpression;
    }

    public String getCollectionVariable() {
        return collectionVariable;
    }

    public void setCollectionVariable(String collectionVariable) {
        this.collectionVariable = collectionVariable;
    }

    public String getCollectionElementVariable() {
        return collectionElementVariable;
    }

    public void setCollectionElementVariable(String collectionElementVariable) {
        this.collectionElementVariable = collectionElementVariable;
    }

    public String getCollectionString() {
        return collectionString;
    }

    public void setCollectionString(String collectionString) {
        this.collectionString = collectionString;
    }

	public CollectionHandler getHandler() {
		return collectionHandler;
	}

	public void setHandler(CollectionHandler collectionHandler) {
		this.collectionHandler = collectionHandler;
	}

	public String getCollectionElementIndexVariable() {
        return collectionElementIndexVariable;
    }

    public void setCollectionElementIndexVariable(String collectionElementIndexVariable) {
        this.collectionElementIndexVariable = collectionElementIndexVariable;
    }

    public void setInnerActivityBehavior(AbstractBpmnActivityBehavior innerActivityBehavior) {
        this.innerActivityBehavior = innerActivityBehavior;
        this.innerActivityBehavior.setMultiInstanceActivityBehavior(this);
    }

    public AbstractBpmnActivityBehavior getInnerActivityBehavior() {
        return innerActivityBehavior;
    }
}
