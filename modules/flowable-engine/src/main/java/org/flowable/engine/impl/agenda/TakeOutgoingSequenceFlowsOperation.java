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
package org.flowable.engine.impl.agenda;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.AdhocSubProcess;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.CancelEventDefinition;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.Gateway;
import org.flowable.bpmn.model.InclusiveGateway;
import org.flowable.bpmn.model.ParallelGateway;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.logging.LoggingSessionConstants;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.delegate.BpmnError;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.Condition;
import org.flowable.engine.impl.bpmn.helper.ErrorPropagation;
import org.flowable.engine.impl.bpmn.helper.SkipExpressionUtil;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.el.UelExpressionCondition;
import org.flowable.engine.impl.jobexecutor.AsyncLeaveJobHandler;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.util.BpmnLoggingSessionUtil;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.JobUtil;
import org.flowable.engine.impl.util.condition.ConditionUtil;
import org.flowable.job.service.JobService;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Operation which purpose is to leave a {@link FlowNode}.
 * This can be done by setting either the {@link FlowNode} or selecting a particular {@link SequenceFlow}:
 *
 * - when the execution currently is at a {@link FlowNode}, leaves it by following the outgoing sequence flow, evaluating conditions if necessary.
 * - when the execution currently is at a {@link SequenceFlow}, this sequence flow will be followed. Any condition is ignored, as the assumed
 *   use case for this situation is a custom {@link org.flowable.engine.impl.delegate.ActivityBehavior} (such as a gateway) that has non-default
 *   behavior of leaving the {@link FlowNode} by checking conditions on all sequence flow and taking those which evaluate to true.
 *
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class TakeOutgoingSequenceFlowsOperation extends AbstractOperation {

    private static final Logger LOGGER = LoggerFactory.getLogger(TakeOutgoingSequenceFlowsOperation.class);

    protected boolean evaluateConditions;
    protected boolean forcedSynchronous;

    public TakeOutgoingSequenceFlowsOperation(CommandContext commandContext, ExecutionEntity executionEntity, boolean evaluateConditions, boolean forcedSynchronous) {
        super(commandContext, executionEntity);
        this.evaluateConditions = evaluateConditions;
        this.forcedSynchronous = forcedSynchronous;
    }

    @Override
    public void run() {
        FlowElement currentFlowElement = getCurrentFlowElement(execution);

        // Compensation check
        if ((currentFlowElement instanceof Activity) && ((Activity) currentFlowElement).isForCompensation()) {

            /*
             * If the current flow element is part of a compensation, we don't always want to follow the regular rules of leaving an activity. More specifically, if there are no outgoing sequenceflow,
             * we simply must stop the execution there and don't go up in the scopes as we usually do to find the outgoing sequenceflow
             */

            cleanupCompensation();
            return;
        }

        // When leaving the current activity, we need to delete any related execution (eg active boundary events)
        cleanupExecutions(currentFlowElement);

        FlowNode sourceFlowNode = getFlowNode(currentFlowElement);
        if (!forcedSynchronous && sourceFlowNode != null && sourceFlowNode.isAsynchronousLeave()) {
            handleAsynchronousLeave(currentFlowElement, sourceFlowNode);

        } else if (currentFlowElement instanceof FlowNode) {
            handleFlowNode((FlowNode) currentFlowElement);

        } else if (currentFlowElement instanceof SequenceFlow) {
            handleSequenceFlow();

        } else {
            throw new FlowableException("Programmatic error: this operation needs either a FlowNode or a SequenceFlow as current FlowElement for " + execution);

        }
    }

    protected FlowNode getFlowNode(FlowElement currentFlowElement) {
        FlowNode sourceFlowNode = null;
        if (currentFlowElement instanceof FlowNode) {
            sourceFlowNode = (FlowNode) currentFlowElement;

        } else if (currentFlowElement instanceof SequenceFlow sequenceFlow){
            FlowElement sourceFlowElement = sequenceFlow.getSourceFlowElement();
            if (sourceFlowElement instanceof FlowNode) {
                sourceFlowNode = (FlowNode) sourceFlowElement;
            }
        }
        return sourceFlowNode;
    }

    protected void handleAsynchronousLeave(FlowElement currentFlowElement, FlowNode sourceFlowNode) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        JobService jobService = processEngineConfiguration.getJobServiceConfiguration().getJobService();
        JobEntity job = JobUtil.createJob(execution, sourceFlowNode, AsyncLeaveJobHandler.TYPE, processEngineConfiguration);

        String jobHandlerConfig = null;
        if (currentFlowElement instanceof FlowNode) {
            jobHandlerConfig = AsyncLeaveJobHandler.createJobConfiguration(processEngineConfiguration, evaluateConditions);
        } else {
            jobHandlerConfig = AsyncLeaveJobHandler.createJobConfiguration(processEngineConfiguration, (SequenceFlow) currentFlowElement);
        }
        job.setJobHandlerConfiguration(jobHandlerConfig);

        jobService.createAsyncJob(job, sourceFlowNode.isAsynchronousLeaveExclusive());
        jobService.scheduleAsyncJob(job);
    }

    protected void handleFlowNode(FlowNode flowNode) {
        boolean continueNormally = handleActivityEnd(flowNode);
        if (continueNormally) {
            // Only continue here, when no BpmnException has been thrown by end listeners.
            if (flowNode.getParentContainer() != null && flowNode.getParentContainer() instanceof AdhocSubProcess) {
                handleAdhocSubProcess(flowNode);
            } else {
                leaveFlowNode(flowNode);
            }
        }
    }

    protected boolean handleActivityEnd(FlowNode flowNode) {
        // a process instance execution can never leave a flow node, but it can pass here whilst cleaning up
        // hence the check for NOT being a process instance
        boolean continueNormally = true;
        if (!execution.isProcessInstanceType()) {
            if (shouldExecuteEndListeners(flowNode)) {
                try {
                    executeExecutionListeners(flowNode, ExecutionListener.EVENTNAME_END);
                } catch (BpmnError bpmnError) {
                    ErrorPropagation.propagateError(bpmnError, execution);
                    // We don't return here immediately, because the activity needs to be ended properly and the event dispatched
                    continueNormally = false;
                }
            }

            if (execution.isActive()
                    && !flowNode.getOutgoingFlows().isEmpty()
                    && !(flowNode instanceof ParallelGateway) // Parallel gw takes care of its own history
                    && !(flowNode instanceof InclusiveGateway) // Inclusive gw takes care of its own history
                    && !(flowNode instanceof SubProcess) // Subprocess handling creates and destroys scoped execution. The execution taking the seq flow is different from the one entering
                    && (!(flowNode instanceof Activity) || ((Activity) flowNode).getLoopCharacteristics() == null) // Multi instance root execution leaving the node isn't stored in history
                    ) {  
                // If no sequence flow: will be handled by the deletion of executions
                CommandContextUtil.getActivityInstanceEntityManager(commandContext).recordActivityEnd(execution, null);
            }

            if (!(execution.getCurrentFlowElement() instanceof SubProcess) &&
                    !(flowNode instanceof Activity && ((Activity) flowNode).hasMultiInstanceLoopCharacteristics())) {
                
                ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
                CommandContextUtil.getEventDispatcher(commandContext).dispatchEvent(
                        FlowableEventBuilder.createActivityEvent(FlowableEngineEventType.ACTIVITY_COMPLETED, flowNode.getId(), flowNode.getName(),
                                execution.getId(), execution.getProcessInstanceId(), execution.getProcessDefinitionId(), flowNode),
                        processEngineConfiguration.getEngineCfgKey());
            }
        }
        return continueNormally;
    }
    
    protected void leaveFlowNode(FlowNode flowNode) {

        LOGGER.debug("Leaving flow node {} with id '{}' by following it's {} outgoing sequenceflow",
                flowNode.getClass(), flowNode.getId(), flowNode.getOutgoingFlows().size());

        // Get default sequence flow (if set)
        String defaultSequenceFlowId = null;
        if (flowNode instanceof Activity) {
            defaultSequenceFlowId = ((Activity) flowNode).getDefaultFlow();
        } else if (flowNode instanceof Gateway) {
            defaultSequenceFlowId = ((Gateway) flowNode).getDefaultFlow();
        }

        // Determine which sequence flows can be used for leaving
        List<SequenceFlow> outgoingSequenceFlows = new ArrayList<>();
        for (SequenceFlow sequenceFlow : flowNode.getOutgoingFlows()) {

            String skipExpressionString = sequenceFlow.getSkipExpression();
            if (!SkipExpressionUtil.isSkipExpressionEnabled(skipExpressionString, sequenceFlow.getId(), execution, commandContext)) {

                if (!evaluateConditions
                        || (evaluateConditions && ConditionUtil.hasTrueCondition(sequenceFlow, execution) && (defaultSequenceFlowId == null || !defaultSequenceFlowId.equals(sequenceFlow.getId())))) {
                    outgoingSequenceFlows.add(sequenceFlow);
                }

            } else if (flowNode.getOutgoingFlows().size() == 1 || SkipExpressionUtil.shouldSkipFlowElement(
                            skipExpressionString, sequenceFlow.getId(), execution, commandContext)) {
                
                // The 'skip' for a sequence flow means that we skip the condition, not the sequence flow.
                outgoingSequenceFlows.add(sequenceFlow);
            }
        }

        // Check if there is a default sequence flow
        if (outgoingSequenceFlows.size() == 0 && evaluateConditions) { // The elements that set this to false also have no support for default sequence flow
            if (defaultSequenceFlowId != null) {
                for (SequenceFlow sequenceFlow : flowNode.getOutgoingFlows()) {
                    if (defaultSequenceFlowId.equals(sequenceFlow.getId())) {
                        outgoingSequenceFlows.add(sequenceFlow);
                        break;
                    }
                }
            }
        }

        // No outgoing found. Ending the execution
        if (outgoingSequenceFlows.size() == 0) {
            if (flowNode.getOutgoingFlows() == null || flowNode.getOutgoingFlows().size() == 0) {
                LOGGER.debug("No outgoing sequence flow found for flow node '{}'.", flowNode.getId());
                agenda.planEndExecutionOperation(execution);

            } else {
                throw new FlowableException("No outgoing sequence flow of element '" + flowNode.getId() + "' could be selected for continuing the process for " + execution);
            }

        } else {

            // Leave, and reuse the incoming sequence flow, make executions for all the others (if applicable)
            ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
            ExecutionEntityManager executionEntityManager = processEngineConfiguration.getExecutionEntityManager();
            List<ExecutionEntity> outgoingExecutions = new ArrayList<>(flowNode.getOutgoingFlows().size());

            SequenceFlow sequenceFlow = outgoingSequenceFlows.get(0);

            // Reuse existing one
            execution.setCurrentFlowElement(sequenceFlow);
            execution.setActive(false);
            outgoingExecutions.add(execution);

            // Executions for all the other one
            if (outgoingSequenceFlows.size() > 1) {
                for (int i = 1; i < outgoingSequenceFlows.size(); i++) {

                    ExecutionEntity parent = execution.getParentId() != null ? execution.getParent() : execution;
                    ExecutionEntity outgoingExecutionEntity = processEngineConfiguration.getExecutionEntityManager().createChildExecution(parent);

                    SequenceFlow outgoingSequenceFlow = outgoingSequenceFlows.get(i);
                    outgoingExecutionEntity.setActive(false);
                    outgoingExecutionEntity.setCurrentFlowElement(outgoingSequenceFlow);

                    executionEntityManager.insert(outgoingExecutionEntity);
                    outgoingExecutions.add(outgoingExecutionEntity);
                }
            }

            // Leave (only done when all executions have been made, since some queries depend on this)
            for (ExecutionEntity outgoingExecution : outgoingExecutions) {
                agenda.planContinueProcessOperation(outgoingExecution);
                if (processEngineConfiguration.isLoggingSessionEnabled()) {
                    BpmnLoggingSessionUtil.addSequenceFlowLoggingData(LoggingSessionConstants.TYPE_SEQUENCE_FLOW_TAKE, outgoingExecution);
                }
            }
        }
    }

    protected void handleAdhocSubProcess(FlowNode flowNode) {
        boolean completeAdhocSubProcess = false;
        AdhocSubProcess adhocSubProcess = (AdhocSubProcess) flowNode.getParentContainer();
        if (adhocSubProcess.getCompletionCondition() != null) {
            Expression expression = CommandContextUtil.getProcessEngineConfiguration(commandContext).getExpressionManager().createExpression(adhocSubProcess.getCompletionCondition());
            Condition condition = new UelExpressionCondition(expression);
            if (condition.evaluate(adhocSubProcess.getId(), execution)) {
                completeAdhocSubProcess = true;
            }
        }

        if (flowNode.getOutgoingFlows().size() > 0) {
            leaveFlowNode(flowNode);
        } else {
            CommandContextUtil.getExecutionEntityManager(commandContext).deleteExecutionAndRelatedData(execution, null, false);
        }

        if (completeAdhocSubProcess) {
            boolean endAdhocSubProcess = true;
            if (!adhocSubProcess.isCancelRemainingInstances()) {
                List<ExecutionEntity> childExecutions = CommandContextUtil.getExecutionEntityManager(commandContext).findChildExecutionsByParentExecutionId(execution.getParentId());
                for (ExecutionEntity executionEntity : childExecutions) {
                    if (!executionEntity.getId().equals(execution.getId())) {
                        endAdhocSubProcess = false;
                        break;
                    }
                }
            }

            if (endAdhocSubProcess) {
                agenda.planEndExecutionOperation(execution.getParent());
            }
        }
    }

    protected void handleSequenceFlow() {
        CommandContextUtil.getActivityInstanceEntityManager(commandContext).recordActivityEnd(execution, null);
        agenda.planContinueProcessOperation(execution);
    }

    protected void cleanupCompensation() {

        // The compensation is at the end here. Simply stop the execution.
        CommandContextUtil.getExecutionEntityManager(commandContext).deleteExecutionAndRelatedData(execution, null, false);

        ExecutionEntity parentExecutionEntity = execution.getParent();
        if (parentExecutionEntity.isScope() && !parentExecutionEntity.isProcessInstanceType()) {

            if (allChildExecutionsEnded(parentExecutionEntity, null)) {

                // Go up the hierarchy to check if the next scope is ended too.
                // This could happen if only the compensation activity is still active, but the
                // main process is already finished.

                ExecutionEntity executionEntityToEnd = parentExecutionEntity;
                ExecutionEntity scopeExecutionEntity = findNextParentScopeExecutionWithAllEndedChildExecutions(parentExecutionEntity, parentExecutionEntity);
                while (scopeExecutionEntity != null) {
                    executionEntityToEnd = scopeExecutionEntity;
                    scopeExecutionEntity = findNextParentScopeExecutionWithAllEndedChildExecutions(scopeExecutionEntity, parentExecutionEntity);
                }

                if (executionEntityToEnd.isProcessInstanceType()) {
                    agenda.planEndExecutionOperation(executionEntityToEnd);
                } else {
                    agenda.planDestroyScopeOperation(executionEntityToEnd);
                }

            }
        }
    }

    protected void cleanupExecutions(FlowElement currentFlowElement) {
        if (execution.getParentId() != null && execution.isScope()) {

            // If the execution is a scope (and not a process instance), the scope must first be
            // destroyed before we can continue and follow the sequence flow

            agenda.planDestroyScopeOperation(execution);

        } else if (currentFlowElement instanceof Activity activity) {

            // If the current activity is an activity, we need to remove any currently active boundary events

            if (CollectionUtil.isNotEmpty(activity.getBoundaryEvents())) {

                // Cancel events are not removed
                List<String> notToDeleteEvents = new ArrayList<>();
                for (BoundaryEvent event : activity.getBoundaryEvents()) {
                    if (CollectionUtil.isNotEmpty(event.getEventDefinitions()) &&
                            event.getEventDefinitions().get(0) instanceof CancelEventDefinition) {
                        
                        notToDeleteEvents.add(event.getId());
                    }
                }

                // Delete all child executions
                Collection<ExecutionEntity> childExecutions = CommandContextUtil.getExecutionEntityManager(commandContext).findChildExecutionsByParentExecutionId(execution.getId());
                for (ExecutionEntity childExecution : childExecutions) {
                    if (childExecution.getCurrentFlowElement() == null || !notToDeleteEvents.contains(childExecution.getCurrentFlowElement().getId())) {
                        CommandContextUtil.getExecutionEntityManager(commandContext).deleteExecutionAndRelatedData(childExecution, null, false);
                    }
                }
            }
        }
    }

    // Compensation helper methods

    /**
     * @param executionEntityToIgnore
     *            The execution entity which we can ignore to be ended, as it's the execution currently being handled in this operation.
     */
    protected ExecutionEntity findNextParentScopeExecutionWithAllEndedChildExecutions(ExecutionEntity executionEntity, ExecutionEntity executionEntityToIgnore) {
        if (executionEntity.getParentId() != null) {
            ExecutionEntity scopeExecutionEntity = executionEntity.getParent();

            // Find next scope
            while (!scopeExecutionEntity.isScope() || !scopeExecutionEntity.isProcessInstanceType()) {
                scopeExecutionEntity = scopeExecutionEntity.getParent();
            }

            // Return when all child executions for it are ended
            if (allChildExecutionsEnded(scopeExecutionEntity, executionEntityToIgnore)) {
                return scopeExecutionEntity;
            }

        }
        return null;
    }

    protected boolean allChildExecutionsEnded(ExecutionEntity parentExecutionEntity, ExecutionEntity executionEntityToIgnore) {
        for (ExecutionEntity childExecutionEntity : parentExecutionEntity.getExecutions()) {
            if (executionEntityToIgnore == null || !executionEntityToIgnore.getId().equals(childExecutionEntity.getId())) {
                if (!childExecutionEntity.isEnded()) {
                    return false;
                }
                if (childExecutionEntity.getExecutions() != null && childExecutionEntity.getExecutions().size() > 0) {
                    if (!allChildExecutionsEnded(childExecutionEntity, executionEntityToIgnore)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    protected boolean shouldExecuteEndListeners(FlowNode flowNode) {
        if (CollectionUtil.isEmpty(flowNode.getExecutionListeners())) {
            return false;
        }
        if (flowNode instanceof Activity) {
            // Execution end listeners should not be executed if the activity has multi instance loop characteristics
            // That is handled in the MultiInstanceActivityBehaviour
            return !((Activity) flowNode).hasMultiInstanceLoopCharacteristics();
        }

        return true;
    }

}
