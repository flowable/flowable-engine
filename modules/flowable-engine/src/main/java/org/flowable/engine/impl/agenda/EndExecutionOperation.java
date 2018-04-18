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
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.CompensateEventDefinition;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.EventSubProcess;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.Transaction;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.bpmn.behavior.MultiInstanceActivityBehavior;
import org.flowable.engine.impl.bpmn.helper.ScopeUtil;
import org.flowable.engine.impl.delegate.ActivityBehavior;
import org.flowable.engine.impl.delegate.SubProcessActivityBehavior;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This operations ends an execution and follows the typical BPMN rules to continue the process (if possible).
 * 
 * This operations is typically not scheduled from an {@link ActivityBehavior}, but rather from another operation. This happens when the conditions are so that the process can't continue via the
 * regular ways and an execution cleanup needs to happen, potentially opening up new ways of continuing the process instance.
 * 
 * @author Joram Barrez
 */
public class EndExecutionOperation extends AbstractOperation {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndExecutionOperation.class);

    public EndExecutionOperation(CommandContext commandContext, ExecutionEntity execution) {
        super(commandContext, execution);
    }

    @Override
    public void run() {
        if (execution.isProcessInstanceType()) {
            handleProcessInstanceExecution(execution);
        } else {
            handleRegularExecution();
        }
    }

    protected void handleProcessInstanceExecution(ExecutionEntity processInstanceExecution) {
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);

        String processInstanceId = processInstanceExecution.getId(); // No parent execution == process instance id
        LOGGER.debug("No parent execution found. Verifying if process instance {} can be stopped.", processInstanceId);

        ExecutionEntity superExecution = processInstanceExecution.getSuperExecution();
        SubProcessActivityBehavior subProcessActivityBehavior = null;

        // copy variables before destroying the ended sub process instance (call activity)
        if (superExecution != null) {
            FlowNode superExecutionElement = (FlowNode) superExecution.getCurrentFlowElement();
            subProcessActivityBehavior = (SubProcessActivityBehavior) superExecutionElement.getBehavior();
            try {
                subProcessActivityBehavior.completing(superExecution, processInstanceExecution);
            } catch (RuntimeException e) {
                LOGGER.error("Error while completing sub process of execution {}", processInstanceExecution, e);
                throw e;
            } catch (Exception e) {
                LOGGER.error("Error while completing sub process of execution {}", processInstanceExecution, e);
                throw new FlowableException("Error while completing sub process of execution " + processInstanceExecution, e);
            }
        }

        int activeExecutions = getNumberOfActiveChildExecutionsForProcessInstance(executionEntityManager, processInstanceId);
        if (activeExecutions == 0) {
            LOGGER.debug("No active executions found. Ending process instance {} ", processInstanceId);

            // note the use of execution here vs processinstance execution for getting the flow element
            executionEntityManager.deleteProcessInstanceExecutionEntity(processInstanceId,
                    execution.getCurrentFlowElement() != null ? execution.getCurrentFlowElement().getId() : null, null, false, false, true);
        } else {
            LOGGER.debug("Active executions found. Process instance {} will not be ended.", processInstanceId);
        }

        Process process = ProcessDefinitionUtil.getProcess(processInstanceExecution.getProcessDefinitionId());

        // Execute execution listeners for process end.
        if (CollectionUtil.isNotEmpty(process.getExecutionListeners())) {
            executeExecutionListeners(process, processInstanceExecution, ExecutionListener.EVENTNAME_END);
        }

        // and trigger execution afterwards if doing a call activity
        if (superExecution != null) {
            superExecution.setSubProcessInstance(null);
            try {
                subProcessActivityBehavior.completed(superExecution);
            } catch (RuntimeException e) {
                LOGGER.error("Error while completing sub process of execution {}", processInstanceExecution, e);
                throw e;
            } catch (Exception e) {
                LOGGER.error("Error while completing sub process of execution {}", processInstanceExecution, e);
                throw new FlowableException("Error while completing sub process of execution " + processInstanceExecution, e);
            }

        }
    }

    protected void handleRegularExecution() {

        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);

        // There will be a parent execution (or else we would be in the process instance handling method)
        ExecutionEntity parentExecution = executionEntityManager.findById(execution.getParentId());

        // If the execution is a scope, all the child executions must be deleted first.
        if (execution.isScope()) {
            executionEntityManager.deleteChildExecutions(execution, null, false);
        }

        // Delete current execution
        LOGGER.debug("Ending execution {}", execution.getId());
        executionEntityManager.deleteExecutionAndRelatedData(execution, null);

        LOGGER.debug("Parent execution found. Continuing process using execution {}", parentExecution.getId());

        // When ending an execution in a multi instance subprocess , special care is needed
        if (isEndEventInMultiInstanceSubprocess(execution)) {
            handleMultiInstanceSubProcess(executionEntityManager, parentExecution);
            return;
        }

        SubProcess subProcess = execution.getCurrentFlowElement().getSubProcess();

        if (subProcess instanceof EventSubProcess) {
            EventSubProcess eventSubProcess = (EventSubProcess) subProcess;

            boolean hasNonInterruptingStartEvent = false;
            for (FlowElement eventSubElement : eventSubProcess.getFlowElements()) {
                if (eventSubElement instanceof StartEvent) {
                    StartEvent subStartEvent = (StartEvent) eventSubElement;
                    if (!subStartEvent.isInterrupting()) {
                        hasNonInterruptingStartEvent = true;
                        break;
                    }
                }
            }

            if (hasNonInterruptingStartEvent) {
                executionEntityManager.deleteChildExecutions(parentExecution, null, false);
                executionEntityManager.deleteExecutionAndRelatedData(parentExecution, null);

                CommandContextUtil.getEventDispatcher(commandContext).dispatchEvent(
                        FlowableEventBuilder.createActivityEvent(FlowableEngineEventType.ACTIVITY_COMPLETED, subProcess.getId(), subProcess.getName(),
                                parentExecution.getId(), parentExecution.getProcessInstanceId(), parentExecution.getProcessDefinitionId(), subProcess));

                ExecutionEntity subProcessParentExecution = parentExecution.getParent();
                if (getNumberOfActiveChildExecutionsForExecution(executionEntityManager, subProcessParentExecution.getId()) == 0) {
                    if (subProcessParentExecution.getCurrentFlowElement() instanceof SubProcess) {
                        SubProcess parentSubProcess = (SubProcess) subProcessParentExecution.getCurrentFlowElement();
                        if (parentSubProcess.getOutgoingFlows().size() > 0) {
                            ExecutionEntity executionToContinue = handleSubProcessEnd(executionEntityManager, subProcessParentExecution, parentSubProcess);
                            agenda.planTakeOutgoingSequenceFlowsOperation(executionToContinue, true);
                            return;
                        }
                        
                    }
                    
                    agenda.planEndExecutionOperation(subProcessParentExecution);
                }

                return;
            }
        }

        // If there are no more active child executions, the process can be continued
        // If not (eg an embedded subprocess still has active elements, we cannot continue)
        List<ExecutionEntity> eventScopeExecutions = getEventScopeExecutions(executionEntityManager, parentExecution);
        
        // Event scoped executions need to be deleted when there are no active siblings anymore, 
        // unless instances of the event subprocess itself. If there are no active siblings anymore,
        // the current scope had ended and the event subprocess start event should stop listening to any trigger.
        if (!eventScopeExecutions.isEmpty()) {
            List<? extends ExecutionEntity> childExecutions = parentExecution.getExecutions();
            boolean activeSiblings = false;
            for (ExecutionEntity childExecutionEntity : childExecutions) {
                if (!isInEventSubProcess(childExecutionEntity) && childExecutionEntity.isActive() && !childExecutionEntity.isEnded()) {
                    activeSiblings = true;
                }
            }
            
            if (!activeSiblings) {
                for (ExecutionEntity eventScopeExecution : eventScopeExecutions) {
                    executionEntityManager.deleteExecutionAndRelatedData(eventScopeExecution, null);                    
                }
            }
        }
        
        if (getNumberOfActiveChildExecutionsForExecution(executionEntityManager, parentExecution.getId()) == 0) {

            ExecutionEntity executionToContinue = null;

            if (subProcess != null) {

                // In case of ending a subprocess: go up in the scopes and continue via the parent scope
                // unless its a compensation, then we don't need to do anything and can just end it

                if (subProcess.isForCompensation()) {
                    agenda.planEndExecutionOperation(parentExecution);
                } else {
                    executionToContinue = handleSubProcessEnd(executionEntityManager, parentExecution, subProcess);
                }

            } else {

                // In the 'regular' case (not being in a subprocess), we use the parent execution to
                // continue process instance execution

                executionToContinue = handleRegularExecutionEnd(executionEntityManager, parentExecution);
            }

            if (executionToContinue != null) {
                // only continue with outgoing sequence flows if the execution is
                // not the process instance root execution (otherwise the process instance is finished)
                if (executionToContinue.isProcessInstanceType()) {
                    handleProcessInstanceExecution(executionToContinue);

                } else {
                    agenda.planTakeOutgoingSequenceFlowsOperation(executionToContinue, true);
                }
            }

        }
    }

    protected ExecutionEntity handleSubProcessEnd(ExecutionEntityManager executionEntityManager, ExecutionEntity parentExecution, SubProcess subProcess) {

        ExecutionEntity executionToContinue = null;
        // create a new execution to take the outgoing sequence flows
        executionToContinue = executionEntityManager.createChildExecution(parentExecution.getParent());
        executionToContinue.setCurrentFlowElement(subProcess);
        executionToContinue.setActive(false);

        boolean hasCompensation = false;
        if (subProcess instanceof Transaction) {
            hasCompensation = true;
        } else {
            for (FlowElement subElement : subProcess.getFlowElements()) {
                if (subElement instanceof Activity) {
                    Activity subActivity = (Activity) subElement;
                    if (CollectionUtil.isNotEmpty(subActivity.getBoundaryEvents())) {
                        for (BoundaryEvent boundaryEvent : subActivity.getBoundaryEvents()) {
                            if (CollectionUtil.isNotEmpty(boundaryEvent.getEventDefinitions()) &&
                                    boundaryEvent.getEventDefinitions().get(0) instanceof CompensateEventDefinition) {
                                
                                hasCompensation = true;
                                break;
                            }
                        }
                    }
                }
            }
        }

        // All executions will be cleaned up afterwards. However, for compensation we need
        // a copy of these executions so we can use them later on when the compensation is thrown.
        // The following method does exactly that, and moves the executions beneath the process instance.
        if (hasCompensation) {
            ScopeUtil.createCopyOfSubProcessExecutionForCompensation(parentExecution);
        }

        executionEntityManager.deleteChildExecutions(parentExecution, null, false);
        executionEntityManager.deleteExecutionAndRelatedData(parentExecution, null);

        CommandContextUtil.getEventDispatcher(commandContext).dispatchEvent(
                FlowableEventBuilder.createActivityEvent(FlowableEngineEventType.ACTIVITY_COMPLETED, subProcess.getId(), subProcess.getName(),
                        parentExecution.getId(), parentExecution.getProcessInstanceId(), parentExecution.getProcessDefinitionId(), subProcess));
        return executionToContinue;
    }

    protected ExecutionEntity handleRegularExecutionEnd(ExecutionEntityManager executionEntityManager, ExecutionEntity parentExecution) {
        ExecutionEntity executionToContinue = null;

        if (!parentExecution.isProcessInstanceType()
                && !(parentExecution.getCurrentFlowElement() instanceof SubProcess)) {
            parentExecution.setCurrentFlowElement(execution.getCurrentFlowElement());
        }

        if (execution.getCurrentFlowElement() instanceof SubProcess) {
            SubProcess currentSubProcess = (SubProcess) execution.getCurrentFlowElement();
            if (currentSubProcess.getOutgoingFlows().size() > 0) {
                // create a new execution to take the outgoing sequence flows
                executionToContinue = executionEntityManager.createChildExecution(parentExecution);
                executionToContinue.setCurrentFlowElement(execution.getCurrentFlowElement());

            } else {
                if (!parentExecution.getId().equals(parentExecution.getProcessInstanceId())) {
                    // create a new execution to take the outgoing sequence flows
                    executionToContinue = executionEntityManager.createChildExecution(parentExecution.getParent());
                    executionToContinue.setCurrentFlowElement(parentExecution.getCurrentFlowElement());

                    executionEntityManager.deleteChildExecutions(parentExecution, null, false);
                    executionEntityManager.deleteExecutionAndRelatedData(parentExecution, null);

                } else {
                    executionToContinue = parentExecution;
                }
            }

        } else {
            executionToContinue = parentExecution;
        }
        return executionToContinue;
    }

    protected void handleMultiInstanceSubProcess(ExecutionEntityManager executionEntityManager, ExecutionEntity parentExecution) {
        List<ExecutionEntity> activeChildExecutions = getActiveChildExecutionsForExecution(executionEntityManager, parentExecution.getId());
        boolean containsOtherChildExecutions = false;
        for (ExecutionEntity activeExecution : activeChildExecutions) {
            if (!activeExecution.getId().equals(execution.getId())) {
                containsOtherChildExecutions = true;
            }
        }

        if (!containsOtherChildExecutions) {

            // Destroy the current scope (subprocess) and leave via the subprocess

            ScopeUtil.createCopyOfSubProcessExecutionForCompensation(parentExecution);
            agenda.planDestroyScopeOperation(parentExecution);

            SubProcess subProcess = execution.getCurrentFlowElement().getSubProcess();
            MultiInstanceActivityBehavior multiInstanceBehavior = (MultiInstanceActivityBehavior) subProcess.getBehavior();
            parentExecution.setCurrentFlowElement(subProcess);
            multiInstanceBehavior.leave(parentExecution);
        }
    }

    protected boolean isEndEventInMultiInstanceSubprocess(ExecutionEntity executionEntity) {
        if (executionEntity.getCurrentFlowElement() instanceof EndEvent) {
            SubProcess subProcess = ((EndEvent) execution.getCurrentFlowElement()).getSubProcess();
            return !executionEntity.getParent().isProcessInstanceType()
                    && subProcess != null
                    && subProcess.getLoopCharacteristics() != null
                    && subProcess.getBehavior() instanceof MultiInstanceActivityBehavior;
        }
        return false;
    }

    protected int getNumberOfActiveChildExecutionsForProcessInstance(ExecutionEntityManager executionEntityManager, String processInstanceId) {
        Collection<ExecutionEntity> executions = executionEntityManager.findChildExecutionsByProcessInstanceId(processInstanceId);
        int activeExecutions = 0;
        for (ExecutionEntity execution : executions) {
            if (execution.isActive() && !processInstanceId.equals(execution.getId())) {
                activeExecutions++;
            }
        }
        return activeExecutions;
    }

    protected int getNumberOfActiveChildExecutionsForExecution(ExecutionEntityManager executionEntityManager, String executionId) {
        List<ExecutionEntity> executions = executionEntityManager.findChildExecutionsByParentExecutionId(executionId);
        int activeExecutions = 0;

        // Filter out the boundary events
        for (ExecutionEntity activeExecution : executions) {
            if (!(activeExecution.getCurrentFlowElement() instanceof BoundaryEvent)) {
                activeExecutions++;
            }
        }

        return activeExecutions;
    }

    protected List<ExecutionEntity> getActiveChildExecutionsForExecution(ExecutionEntityManager executionEntityManager, String executionId) {
        List<ExecutionEntity> activeChildExecutions = new ArrayList<>();
        List<ExecutionEntity> executions = executionEntityManager.findChildExecutionsByParentExecutionId(executionId);

        for (ExecutionEntity activeExecution : executions) {
            if (!(activeExecution.getCurrentFlowElement() instanceof BoundaryEvent)) {
                activeChildExecutions.add(activeExecution);
            }
        }

        return activeChildExecutions;
    }

    protected List<ExecutionEntity> getEventScopeExecutions(ExecutionEntityManager executionEntityManager, ExecutionEntity parentExecution) {
        List<ExecutionEntity> eventScopeExecutions = new ArrayList<>(1);
        List<ExecutionEntity> executions = executionEntityManager.findChildExecutionsByParentExecutionId(parentExecution.getId());
        for (ExecutionEntity childExecution : executions) {
            if (childExecution.isEventScope()) {
                eventScopeExecutions.add(childExecution);
                
            } 
        }
        return eventScopeExecutions;
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
    
    protected boolean isInEventSubProcess(ExecutionEntity executionEntity) {
        ExecutionEntity currentExecutionEntity = executionEntity;
        while (currentExecutionEntity != null) {
            if (currentExecutionEntity.getCurrentFlowElement() instanceof EventSubProcess) {
                return true;
            }
            currentExecutionEntity = currentExecutionEntity.getParent();
        }
        return false;
    }
    
}
