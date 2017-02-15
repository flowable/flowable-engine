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

import java.util.List;

import org.flowable.bpmn.model.CallActivity;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.event.FlowableEngineEventType;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.history.DeleteReason;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.impl.history.HistoryLevel;
import org.flowable.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.persistence.entity.HistoricActivityInstanceEntity;

/**
 * @author Joram Barrez
 */
public class TerminateEndEventActivityBehavior extends FlowNodeActivityBehavior {

    private static final long serialVersionUID = 1L;

    protected boolean terminateAll;
    protected boolean terminateMultiInstance;

    public TerminateEndEventActivityBehavior() {

    }

    @Override
    public void execute(DelegateExecution execution) {

        CommandContext commandContext = Context.getCommandContext();
        ExecutionEntityManager executionEntityManager = commandContext.getExecutionEntityManager();

        if (terminateAll) {
            terminateAllBehaviour(execution, commandContext, executionEntityManager);
        } else if (terminateMultiInstance) {
            terminateMultiInstanceRoot(execution, commandContext, executionEntityManager);
        } else {
            defaultTerminateEndEventBehaviour(execution, commandContext, executionEntityManager);
        }
    }

    protected void terminateAllBehaviour(DelegateExecution execution, CommandContext commandContext, ExecutionEntityManager executionEntityManager) {
        ExecutionEntity rootExecutionEntity = executionEntityManager.findByRootProcessInstanceId(execution.getRootProcessInstanceId());
        String deleteReason = createDeleteReason(execution.getCurrentActivityId());
        deleteExecutionEntities(executionEntityManager, rootExecutionEntity, deleteReason);
        endAllHistoricActivities(rootExecutionEntity.getId(), deleteReason);
        commandContext.getHistoryManager().recordProcessInstanceEnd(rootExecutionEntity.getId(),
                deleteReason, execution.getCurrentActivityId());
    }

    protected void defaultTerminateEndEventBehaviour(DelegateExecution execution, CommandContext commandContext,
            ExecutionEntityManager executionEntityManager) {

        ExecutionEntity scopeExecutionEntity = executionEntityManager.findFirstScope((ExecutionEntity) execution);
        sendProcessInstanceCompletedEvent(scopeExecutionEntity, execution.getCurrentFlowElement());

        // If the scope is the process instance, we can just terminate it all
        // Special treatment is needed when the terminated activity is a subprocess (embedded/callactivity/..)
        // The subprocess is destroyed, but the execution calling it, continues further on.
        // In case of a multi-instance subprocess, only one instance is terminated, the other instances continue to exist.

        String deleteReason = createDeleteReason(execution.getCurrentActivityId());

        if (scopeExecutionEntity.isProcessInstanceType() && scopeExecutionEntity.getSuperExecutionId() == null) {

            endAllHistoricActivities(scopeExecutionEntity.getId(), deleteReason);
            deleteExecutionEntities(executionEntityManager, scopeExecutionEntity, deleteReason);
            commandContext.getHistoryManager().recordProcessInstanceEnd(scopeExecutionEntity.getId(), deleteReason, execution.getCurrentActivityId());

        } else if (scopeExecutionEntity.getCurrentFlowElement() != null
                && scopeExecutionEntity.getCurrentFlowElement() instanceof SubProcess) { // SubProcess

            SubProcess subProcess = (SubProcess) scopeExecutionEntity.getCurrentFlowElement();

            scopeExecutionEntity.setDeleteReason(deleteReason);
            if (subProcess.hasMultiInstanceLoopCharacteristics()) {

                commandContext.getAgenda().planDestroyScopeOperation(scopeExecutionEntity);
                MultiInstanceActivityBehavior multiInstanceBehavior = (MultiInstanceActivityBehavior) subProcess.getBehavior();
                multiInstanceBehavior.leave(scopeExecutionEntity);

            } else {
                commandContext.getAgenda().planDestroyScopeOperation(scopeExecutionEntity);
                ExecutionEntity outgoingFlowExecution = executionEntityManager.createChildExecution(scopeExecutionEntity.getParent());
                outgoingFlowExecution.setCurrentFlowElement(scopeExecutionEntity.getCurrentFlowElement());
                commandContext.getAgenda().planTakeOutgoingSequenceFlowsOperation(outgoingFlowExecution, true);
            }

        } else if (scopeExecutionEntity.getParentId() == null
                && scopeExecutionEntity.getSuperExecutionId() != null) { // CallActivity

            ExecutionEntity callActivityExecution = scopeExecutionEntity.getSuperExecution();
            CallActivity callActivity = (CallActivity) callActivityExecution.getCurrentFlowElement();

            if (callActivity.hasMultiInstanceLoopCharacteristics()) {

                MultiInstanceActivityBehavior multiInstanceBehavior = (MultiInstanceActivityBehavior) callActivity.getBehavior();
                multiInstanceBehavior.leave(callActivityExecution);
                executionEntityManager.deleteProcessInstanceExecutionEntity(scopeExecutionEntity.getId(), execution.getCurrentFlowElement().getId(), "terminate end event", false, false);

            } else {

                executionEntityManager.deleteProcessInstanceExecutionEntity(scopeExecutionEntity.getId(), execution.getCurrentFlowElement().getId(), "terminate end event", false, false);
                ExecutionEntity superExecutionEntity = executionEntityManager.findById(scopeExecutionEntity.getSuperExecutionId());
                commandContext.getAgenda().planTakeOutgoingSequenceFlowsOperation(superExecutionEntity, true);

            }

        }
    }

    protected void endAllHistoricActivities(String processInstanceId, String deleteReason) {

        if (!Context.getProcessEngineConfiguration().getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            return;
        }

        List<HistoricActivityInstanceEntity> historicActivityInstances = Context.getCommandContext().getHistoricActivityInstanceEntityManager()
                .findUnfinishedHistoricActivityInstancesByProcessInstanceId(processInstanceId);

        for (HistoricActivityInstanceEntity historicActivityInstance : historicActivityInstances) {
            historicActivityInstance.markEnded(deleteReason);

            // Fire event
            ProcessEngineConfigurationImpl config = Context.getProcessEngineConfiguration();
            if (config != null && config.getEventDispatcher().isEnabled()) {
                config.getEventDispatcher().dispatchEvent(
                        FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.HISTORIC_ACTIVITY_INSTANCE_ENDED, historicActivityInstance));
            }
        }

    }

    protected void terminateMultiInstanceRoot(DelegateExecution execution, CommandContext commandContext,
            ExecutionEntityManager executionEntityManager) {

        // When terminateMultiInstance is 'true', we look for the multi instance root and delete it from there.
        ExecutionEntity miRootExecutionEntity = executionEntityManager.findFirstMultiInstanceRoot((ExecutionEntity) execution);
        if (miRootExecutionEntity != null) {

            // Create sibling execution to continue process instance execution before deletion
            ExecutionEntity siblingExecution = executionEntityManager.createChildExecution(miRootExecutionEntity.getParent());
            siblingExecution.setCurrentFlowElement(miRootExecutionEntity.getCurrentFlowElement());

            deleteExecutionEntities(executionEntityManager, miRootExecutionEntity, createDeleteReason(miRootExecutionEntity.getActivityId()));

            commandContext.getAgenda().planTakeOutgoingSequenceFlowsOperation(siblingExecution, true);
        } else {
            defaultTerminateEndEventBehaviour(execution, commandContext, executionEntityManager);
        }
    }

    protected void deleteExecutionEntities(ExecutionEntityManager executionEntityManager, ExecutionEntity rootExecutionEntity, String deleteReason) {

        List<ExecutionEntity> childExecutions = executionEntityManager.collectChildren(rootExecutionEntity);
        for (int i = childExecutions.size() - 1; i >= 0; i--) {
            executionEntityManager.deleteExecutionAndRelatedData(childExecutions.get(i), deleteReason, false);
        }
        executionEntityManager.deleteExecutionAndRelatedData(rootExecutionEntity, deleteReason, false);
    }

    protected void sendProcessInstanceCompletedEvent(DelegateExecution execution, FlowElement terminateEndEvent) {
        if (Context.getProcessEngineConfiguration().getEventDispatcher().isEnabled()) {
            if ((execution.isProcessInstanceType() && execution.getSuperExecutionId() == null) ||
                    (execution.getParentId() == null && execution.getSuperExecutionId() != null)) {

                Context.getProcessEngineConfiguration().getEventDispatcher()
                        .dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT, execution));
            }
        }

        dispatchExecutionCancelled(execution, terminateEndEvent);
    }

    protected void dispatchExecutionCancelled(DelegateExecution execution, FlowElement terminateEndEvent) {

        ExecutionEntityManager executionEntityManager = Context.getCommandContext().getExecutionEntityManager();

        // subprocesses
        for (DelegateExecution subExecution : executionEntityManager.findChildExecutionsByParentExecutionId(execution.getId())) {
            dispatchExecutionCancelled(subExecution, terminateEndEvent);
        }

        // call activities
        ExecutionEntity subProcessInstance = Context.getCommandContext().getExecutionEntityManager().findSubProcessInstanceBySuperExecutionId(execution.getId());
        if (subProcessInstance != null) {
            dispatchExecutionCancelled(subProcessInstance, terminateEndEvent);
        }

        // activity with message/signal boundary events
        FlowElement currentFlowElement = execution.getCurrentFlowElement();
        if (currentFlowElement instanceof FlowNode) {
            dispatchActivityCancelled(execution, terminateEndEvent);
        }
    }

    protected void dispatchActivityCancelled(DelegateExecution execution, FlowElement terminateEndEvent) {
        Context.getProcessEngineConfiguration()
                .getEventDispatcher()
                .dispatchEvent(
                        FlowableEventBuilder.createActivityCancelledEvent(execution.getCurrentFlowElement().getId(),
                                execution.getCurrentFlowElement().getName(), execution.getId(), execution.getProcessInstanceId(),
                                execution.getProcessDefinitionId(), parseActivityType((FlowNode) execution.getCurrentFlowElement()), terminateEndEvent));
    }

    protected String createDeleteReason(String activityId) {
        return DeleteReason.TERMINATE_END_EVENT + " (" + activityId + ")";
    }

    public boolean isTerminateAll() {
        return terminateAll;
    }

    public void setTerminateAll(boolean terminateAll) {
        this.terminateAll = terminateAll;
    }

    public boolean isTerminateMultiInstance() {
        return terminateMultiInstance;
    }

    public void setTerminateMultiInstance(boolean terminateMultiInstance) {
        this.terminateMultiInstance = terminateMultiInstance;
    }

}
