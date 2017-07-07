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
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.impl.context.Context;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.event.FlowableEngineEventType;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.history.DeleteReason;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.delegate.SubProcessActivityBehavior;
import org.flowable.engine.impl.history.HistoryLevel;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.persistence.entity.HistoricActivityInstanceEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 */
public class TerminateEndEventActivityBehavior extends FlowNodeActivityBehavior {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TerminateEndEventActivityBehavior.class);

    private static final long serialVersionUID = 1L;

    protected boolean terminateAll;
    protected boolean terminateMultiInstance;

    public TerminateEndEventActivityBehavior() {

    }

    @Override
    public void execute(DelegateExecution execution) {

        CommandContext commandContext = Context.getCommandContext();
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);

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
        deleteExecutionEntities(executionEntityManager, rootExecutionEntity, execution.getCurrentFlowElement(), deleteReason);
        endAllHistoricActivities(rootExecutionEntity.getId(), deleteReason);
        CommandContextUtil.getHistoryManager(commandContext).recordProcessInstanceEnd(rootExecutionEntity,
                deleteReason, execution.getCurrentActivityId());
    }

    protected void defaultTerminateEndEventBehaviour(DelegateExecution execution, CommandContext commandContext,
            ExecutionEntityManager executionEntityManager) {

        ExecutionEntity scopeExecutionEntity = executionEntityManager.findFirstScope((ExecutionEntity) execution);

        // If the scope is the process instance, we can just terminate it all
        // Special treatment is needed when the terminated activity is a subprocess (embedded/callactivity/..)
        // The subprocess is destroyed, but the execution calling it, continues further on.
        // In case of a multi-instance subprocess, only one instance is terminated, the other instances continue to exist.

        String deleteReason = createDeleteReason(execution.getCurrentActivityId());

        if (scopeExecutionEntity.isProcessInstanceType() && scopeExecutionEntity.getSuperExecutionId() == null) {

            endAllHistoricActivities(scopeExecutionEntity.getId(), deleteReason);
            deleteExecutionEntities(executionEntityManager, scopeExecutionEntity, execution.getCurrentFlowElement(), deleteReason);
            CommandContextUtil.getHistoryManager(commandContext).recordProcessInstanceEnd(scopeExecutionEntity, deleteReason, execution.getCurrentActivityId());

        } else if (scopeExecutionEntity.getCurrentFlowElement() != null
                && scopeExecutionEntity.getCurrentFlowElement() instanceof SubProcess) { // SubProcess

            SubProcess subProcess = (SubProcess) scopeExecutionEntity.getCurrentFlowElement();

            scopeExecutionEntity.setDeleteReason(deleteReason);
            if (subProcess.hasMultiInstanceLoopCharacteristics()) {
                sendProcessInstanceCompletedEvent(scopeExecutionEntity, execution.getCurrentFlowElement());
                CommandContextUtil.getAgenda(commandContext).planDestroyScopeOperation(scopeExecutionEntity);
                MultiInstanceActivityBehavior multiInstanceBehavior = (MultiInstanceActivityBehavior) subProcess.getBehavior();
                multiInstanceBehavior.leave(scopeExecutionEntity);

            } else {
                sendProcessInstanceCompletedEvent(scopeExecutionEntity, execution.getCurrentFlowElement());
                CommandContextUtil.getAgenda(commandContext).planDestroyScopeOperation(scopeExecutionEntity);
                ExecutionEntity outgoingFlowExecution = executionEntityManager.createChildExecution(scopeExecutionEntity.getParent());
                outgoingFlowExecution.setCurrentFlowElement(scopeExecutionEntity.getCurrentFlowElement());
                CommandContextUtil.getAgenda(commandContext).planTakeOutgoingSequenceFlowsOperation(outgoingFlowExecution, true);
            }

        } else if (scopeExecutionEntity.getParentId() == null
                && scopeExecutionEntity.getSuperExecutionId() != null) { // CallActivity

            ExecutionEntity callActivityExecution = scopeExecutionEntity.getSuperExecution();
            CallActivity callActivity = (CallActivity) callActivityExecution.getCurrentFlowElement();
            
            SubProcessActivityBehavior subProcessActivityBehavior = null;

            // copy variables before destroying the ended sub process instance (call activity)
            subProcessActivityBehavior = (SubProcessActivityBehavior) callActivity.getBehavior();
            try {
                subProcessActivityBehavior.completing(callActivityExecution, scopeExecutionEntity);
            } catch (RuntimeException e) {
                LOGGER.error("Error while completing sub process of execution {}", scopeExecutionEntity, e);
                throw e;
            } catch (Exception e) {
                LOGGER.error("Error while completing sub process of execution {}", scopeExecutionEntity, e);
                throw new FlowableException("Error while completing sub process of execution " + scopeExecutionEntity, e);
            }

            if (callActivity.hasMultiInstanceLoopCharacteristics()) {

                sendProcessInstanceCompletedEvent(scopeExecutionEntity, execution.getCurrentFlowElement());
                MultiInstanceActivityBehavior multiInstanceBehavior = (MultiInstanceActivityBehavior) callActivity.getBehavior();
                multiInstanceBehavior.leave(callActivityExecution);
                executionEntityManager.deleteProcessInstanceExecutionEntity(scopeExecutionEntity.getId(), 
                                execution.getCurrentFlowElement().getId(), "terminate end event", false, false, false);

            } else {

                sendProcessInstanceCompletedEvent(scopeExecutionEntity, execution.getCurrentFlowElement());
                executionEntityManager.deleteProcessInstanceExecutionEntity(scopeExecutionEntity.getId(), 
                                execution.getCurrentFlowElement().getId(), "terminate end event", false, false, false);
                ExecutionEntity superExecutionEntity = executionEntityManager.findById(scopeExecutionEntity.getSuperExecutionId());
                CommandContextUtil.getAgenda(commandContext).planTakeOutgoingSequenceFlowsOperation(superExecutionEntity, true);

            }

        }
    }

    protected void endAllHistoricActivities(String processInstanceId, String deleteReason) {

        if (!CommandContextUtil.getProcessEngineConfiguration().getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            return;
        }

        List<HistoricActivityInstanceEntity> historicActivityInstances = CommandContextUtil.getHistoricActivityInstanceEntityManager()
                .findUnfinishedHistoricActivityInstancesByProcessInstanceId(processInstanceId);

        for (HistoricActivityInstanceEntity historicActivityInstance : historicActivityInstances) {
            historicActivityInstance.markEnded(deleteReason);

            // Fire event
            ProcessEngineConfigurationImpl config = CommandContextUtil.getProcessEngineConfiguration();
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

            deleteExecutionEntities(executionEntityManager, miRootExecutionEntity, miRootExecutionEntity.getCurrentFlowElement(), createDeleteReason(miRootExecutionEntity.getActivityId()));

            CommandContextUtil.getAgenda(commandContext).planTakeOutgoingSequenceFlowsOperation(siblingExecution, true);
        } else {
            defaultTerminateEndEventBehaviour(execution, commandContext, executionEntityManager);
        }
    }

    protected void deleteExecutionEntities(ExecutionEntityManager executionEntityManager, ExecutionEntity rootExecutionEntity,
                    FlowElement terminateEndEvent, String deleteReason) {

        List<ExecutionEntity> childExecutions = executionEntityManager.collectChildren(rootExecutionEntity);
        for (int i = childExecutions.size() - 1; i >= 0; i--) {
            sendProcessInstanceCompletedEvent(childExecutions.get(i), terminateEndEvent);
            executionEntityManager.deleteExecutionAndRelatedData(childExecutions.get(i), deleteReason, false);
        }

        sendProcessInstanceCompletedEvent(rootExecutionEntity, terminateEndEvent);
        executionEntityManager.deleteExecutionAndRelatedData(rootExecutionEntity, deleteReason, false);
    }

    protected void sendProcessInstanceCompletedEvent(ExecutionEntity execution, FlowElement terminateEndEvent) {
        if (CommandContextUtil.getProcessEngineConfiguration().getEventDispatcher().isEnabled()) {
            if ((execution.isProcessInstanceType() && execution.getSuperExecutionId() == null) ||
                    (execution.getParentId() == null && execution.getSuperExecutionId() != null)) {

                CommandContextUtil.getProcessEngineConfiguration().getEventDispatcher()
                        .dispatchEvent(FlowableEventBuilder.createTerminateEvent(execution, terminateEndEvent));
            }
        }

        dispatchExecutionCancelled(execution, terminateEndEvent);
    }

    protected void dispatchExecutionCancelled(ExecutionEntity execution, FlowElement terminateEndEvent) {

        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager();

        // subprocesses
        for (ExecutionEntity subExecution : executionEntityManager.findChildExecutionsByParentExecutionId(execution.getId())) {
            dispatchExecutionCancelled(subExecution, terminateEndEvent);
        }

        // call activities
        ExecutionEntity subProcessInstance = CommandContextUtil.getExecutionEntityManager().findSubProcessInstanceBySuperExecutionId(execution.getId());
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
        CommandContextUtil.getProcessEngineConfiguration()
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
