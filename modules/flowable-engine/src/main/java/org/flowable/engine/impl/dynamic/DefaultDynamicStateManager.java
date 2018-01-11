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

package org.flowable.engine.impl.dynamic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.CompensateEventDefinition;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.common.impl.util.CollectionUtil;
import org.flowable.engine.dynamic.DynamicStateManager;
import org.flowable.engine.impl.cmd.ChangeActivityStateCmd.MoveExecutionEntityContainer;
import org.flowable.engine.impl.delegate.ActivityBehavior;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.Flowable5Util;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.engine.impl.util.ProcessInstanceHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 */
public class DefaultDynamicStateManager implements DynamicStateManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDynamicStateManager.class);
    
    public ExecutionEntity resolveActiveExecution(String executionId, CommandContext commandContext) {
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);
        ExecutionEntity execution = executionEntityManager.findById(executionId);

        if (execution == null) {
            throw new FlowableException("Execution could not be found with id " + executionId);
        }

        if (Flowable5Util.isFlowable5ProcessDefinitionId(commandContext, execution.getProcessDefinitionId())) {
            throw new FlowableException("Flowable 5 process definitions are not supported");
        }
        
        return execution;
    }
    
    public ExecutionEntity resolveActiveExecution(String processInstanceId, String activityId, CommandContext commandContext) {
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);
        ExecutionEntity processExecution = executionEntityManager.findById(processInstanceId);

        if (processExecution == null) {
            throw new FlowableException("Execution could not be found with id " + processInstanceId);
        }

        if (!processExecution.isProcessInstanceType()) {
            throw new FlowableException("Execution is not a process instance type execution for id " + processInstanceId);
        }

        if (Flowable5Util.isFlowable5ProcessDefinitionId(commandContext, processExecution.getProcessDefinitionId())) {
            throw new FlowableException("Flowable 5 process definitions are not supported");
        }
        
        ExecutionEntity execution = getActiveExecution(activityId, processExecution, commandContext);
        
        return execution;
    }
    
    public void moveExecutionState(List<MoveExecutionEntityContainer> moveExecutionEntityContainerList, Map<String, Object> processVariables, 
                    Map<String, Map<String, Object>> localVariables, CommandContext commandContext) {
        
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);
        
        for (MoveExecutionEntityContainer moveExecutionContainer : moveExecutionEntityContainerList) {
            
            // Get first execution to get process definition id
            ExecutionEntity firstExecution = moveExecutionContainer.getExecutions().get(0);
            BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(firstExecution.getProcessDefinitionId());
            
            // Get FlowElement objects for every move to activity id
            for (String activityId : moveExecutionContainer.getMoveToActivityIds()) {
                FlowElement newFlowElement = bpmnModel.getFlowElement(activityId);
                
                if (newFlowElement == null) {
                    throw new FlowableException("Activity could not be found in process definition for id " + activityId);
                }
                
                moveExecutionContainer.addMoveToFlowElement(activityId, newFlowElement);
            }
            
            // Delete the parent executions for each current execution when the no move to activity id has the same sub process scope
            for (ExecutionEntity execution : moveExecutionContainer.getExecutions()) {
                if (execution.getParentId() == null) {
                    throw new FlowableException("Execution has no parent execution " + execution.getParentId());
                }
                
                ExecutionEntity continueParentExecution = deleteParentExecutions(execution.getParentId(), moveExecutionContainer, commandContext);
                moveExecutionContainer.addContinueParentExecution(execution.getId(), continueParentExecution);
            }
    
            for (ExecutionEntity execution : moveExecutionContainer.getExecutions()) {
                executionEntityManager.deleteExecutionAndRelatedData(execution, "Change activity to " + printFlowElementIds(moveExecutionContainer.getMoveToFlowElements()));
            }
            
            // Resolve the sub process elements that need to be created for each move to flow element
            for (FlowElement flowElement : moveExecutionContainer.getMoveToFlowElements()) {
                resolveSubProcesExecutionsToCreate(flowElement.getId(), flowElement.getSubProcess(), moveExecutionContainer);
            }
            
            // The default parent execution is retrieved from the match with the first source execution 
            ExecutionEntity defaultContinueParentExecution = moveExecutionContainer.getContinueParentExecution(moveExecutionContainer.getExecutions().get(0).getId());
            
            for (String activityId : moveExecutionContainer.getSubProcessesToCreateMap().keySet()) {
                List<SubProcess> subProcessesToCreate = moveExecutionContainer.getSubProcessesToCreateMap().get(activityId);
                for (SubProcess subProcess : subProcessesToCreate) {
                    
                    // Check if sub process execution was not already created
                    if (moveExecutionContainer.getNewSubProcessChildExecution(subProcess.getId()) == null) {
                        FlowElement startElement = getStartElement(subProcess);
            
                        if (startElement == null) {
                            throw new FlowableException("No initial activity found for subprocess " + subProcess.getId());
                        }
                        
                        ExecutionEntity subProcessExecution = executionEntityManager.createChildExecution(defaultContinueParentExecution);
                        subProcessExecution.setCurrentFlowElement(subProcess);
                        subProcessExecution.setScope(true);
                        
                        CommandContextUtil.getHistoryManager(commandContext).recordActivityStart(subProcessExecution);
                        
                        List<BoundaryEvent> boundaryEvents = subProcess.getBoundaryEvents();
                        if (CollectionUtil.isNotEmpty(boundaryEvents)) {
                            executeBoundaryEvents(boundaryEvents, subProcessExecution);
                        }
                        
                        ProcessInstanceHelper processInstanceHelper = CommandContextUtil.getProcessEngineConfiguration(commandContext).getProcessInstanceHelper();
                        processInstanceHelper.processAvailableEventSubProcesses(subProcessExecution, subProcess, commandContext);
            
                        ExecutionEntity startSubProcessExecution = CommandContextUtil.getExecutionEntityManager(commandContext)
                                .createChildExecution(subProcessExecution);
                        startSubProcessExecution.setCurrentFlowElement(startElement);
                        
                        moveExecutionContainer.addNewSubProcessChildExecution(subProcess.getId(), startSubProcessExecution);
                    }
                }
            }
            
            List<ExecutionEntity> newChildExecutions = new ArrayList<>();
            for (FlowElement newFlowElement : moveExecutionContainer.getMoveToFlowElements()) {
                ExecutionEntity newChildExecution = null;
                
                // Check if a sub process child execution was created for this move to flow element, otherwise use the default continue parent execution
                if (moveExecutionContainer.getSubProcessesToCreateMap().containsKey(newFlowElement.getId())) {
                    newChildExecution = moveExecutionContainer.getNewSubProcessChildExecution(
                                    moveExecutionContainer.getSubProcessesToCreateMap().get(newFlowElement.getId()).get(0).getId());
                } else {
                    newChildExecution = executionEntityManager.createChildExecution(defaultContinueParentExecution);
                }
                
                newChildExecution.setCurrentFlowElement(newFlowElement);
                newChildExecutions.add(newChildExecution);
            }
            
            if (processVariables != null && processVariables.size() > 0) {
                ExecutionEntity processInstanceExecution = defaultContinueParentExecution.getProcessInstance();
                processInstanceExecution.setVariables(processVariables);
            }
            
            if (moveExecutionContainer.getMoveToFlowElements().size() == 1 && localVariables != null && localVariables.size() > 0) {
                FlowElement moveToFlowElement = moveExecutionContainer.getMoveToFlowElements().iterator().next();
                if (localVariables.containsKey(moveToFlowElement.getId())) {
                    List<SubProcess> toCreateSubProcesses = moveExecutionContainer.getSubProcessesToCreateMap().get(moveToFlowElement.getId());
                    if (toCreateSubProcesses != null && toCreateSubProcesses.size() > 0) {
                        moveExecutionContainer.getNewSubProcessChildExecution(toCreateSubProcesses.get(0).getId())
                            .setVariablesLocal(localVariables.get(moveToFlowElement.getId()));
                    } else {
                        newChildExecutions.get(0).setVariablesLocal(localVariables.get(moveToFlowElement.getId()));
                    }
                }
            }
            
            for (ExecutionEntity newChildExecution : newChildExecutions) {
                CommandContextUtil.getAgenda().planContinueProcessOperation(newChildExecution);
            }
        }
    }
    
    protected ExecutionEntity getActiveExecution(String activityId, ExecutionEntity processExecution, CommandContext commandContext) {
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);
        
        ExecutionEntity activeExecutionEntity = null;
        List<ExecutionEntity> childExecutions = executionEntityManager.findChildExecutionsByProcessInstanceId(processExecution.getId());
        for (ExecutionEntity childExecution : childExecutions) {
            if (childExecution.getCurrentActivityId().equals(activityId)) {
                activeExecutionEntity = childExecution;
            }
        }

        if (activeExecutionEntity == null) {
            throw new FlowableException("Active execution could not be found with activity id " + activityId);
        }
        
        return activeExecutionEntity;
    }
    
    protected ExecutionEntity deleteParentExecutions(String parentExecutionId, MoveExecutionEntityContainer moveExecutionContainer, CommandContext commandContext) {
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);
        
        ExecutionEntity continueParentExecution = executionEntityManager.findById(parentExecutionId);
        if (continueParentExecution.getCurrentFlowElement() instanceof SubProcess) {
            SubProcess parentSubProcess = (SubProcess) continueParentExecution.getCurrentFlowElement();
            
            if (!isSubProcessUsedInNewFlowElements(parentSubProcess.getId(), moveExecutionContainer)) {
                ExecutionEntity toDeleteParentExecution = resolveParentExecutionToDelete(continueParentExecution, moveExecutionContainer);
                ExecutionEntity finalDeleteExecution = null;
                if (toDeleteParentExecution != null) {
                    finalDeleteExecution = toDeleteParentExecution;
                } else {
                    finalDeleteExecution = continueParentExecution;
                }
                
                continueParentExecution = finalDeleteExecution.getParent();
                
                String flowElementIdsLine = printFlowElementIds(moveExecutionContainer.getMoveToFlowElements());
                executionEntityManager.deleteChildExecutions(finalDeleteExecution, "Change activity to " + flowElementIdsLine, true);
                executionEntityManager.deleteExecutionAndRelatedData(finalDeleteExecution, "Change activity to " + flowElementIdsLine);
            }
        }
        
        return continueParentExecution;
    }
    
    protected void executeBoundaryEvents(Collection<BoundaryEvent> boundaryEvents, ExecutionEntity execution) {

        // The parent execution becomes a scope, and a child execution is created for each of the boundary events
        for (BoundaryEvent boundaryEvent : boundaryEvents) {

            if (CollectionUtil.isEmpty(boundaryEvent.getEventDefinitions())
                    || (boundaryEvent.getEventDefinitions().get(0) instanceof CompensateEventDefinition)) {
                continue;
            }

            // A Child execution of the current execution is created to represent the boundary event being active
            ExecutionEntity childExecutionEntity = CommandContextUtil.getExecutionEntityManager().createChildExecution(execution);
            childExecutionEntity.setParentId(execution.getId());
            childExecutionEntity.setCurrentFlowElement(boundaryEvent);
            childExecutionEntity.setScope(false);

            ActivityBehavior boundaryEventBehavior = ((ActivityBehavior) boundaryEvent.getBehavior());
            LOGGER.debug("Executing boundary event activityBehavior {} with execution {}", boundaryEventBehavior.getClass(), childExecutionEntity.getId());
            boundaryEventBehavior.execute(childExecutionEntity);
        }
    }
    
    protected void resolveSubProcesExecutionsToCreate(String searchActivitiyId, SubProcess searchSubProcess, MoveExecutionEntityContainer moveExecutionContainer) {
        if (searchSubProcess != null) {
            
            if (!hasSubProcessId(searchSubProcess.getId(), moveExecutionContainer)) {
                moveExecutionContainer.addSubProcessToCreate(searchActivitiyId, searchSubProcess);
            }
            
            resolveSubProcesExecutionsToCreate(searchActivitiyId, searchSubProcess.getSubProcess(), moveExecutionContainer);
        }
    }
    
    protected ExecutionEntity resolveParentExecutionToDelete(ExecutionEntity execution, MoveExecutionEntityContainer moveExecutionContainer) {
        ExecutionEntity parentExecution = execution.getParent();
        
        if (parentExecution.isProcessInstanceType()) {
            return null;
        }
        
        if (!isSubProcessUsedInNewFlowElements(parentExecution.getActivityId(),  moveExecutionContainer)) {
            ExecutionEntity subProcessParentExecution = resolveParentExecutionToDelete(parentExecution, moveExecutionContainer);
            if (subProcessParentExecution != null) {
                return subProcessParentExecution;
            } else {
                return parentExecution;
            }
        }
        
        return null;
    }
    
    protected boolean isSubProcessUsedInNewFlowElements(String subProcessId, MoveExecutionEntityContainer moveExecutionContainer) {
        boolean isUsed = false;
        for (FlowElement flowElement : moveExecutionContainer.getMoveToFlowElements()) {
            SubProcess elementSubProcess = flowElement.getSubProcess();
            if (elementSubProcess != null && elementSubProcess.getId().equals(subProcessId)) {
                isUsed = true;
                break;
            }
        }
        
        return isUsed;
    }
    
    protected boolean hasSubProcessId(String subProcessId, MoveExecutionEntityContainer moveExecutionContainer) {
        for (ExecutionEntity execution : moveExecutionContainer.getExecutions()) {
            FlowElement executionElement = execution.getCurrentFlowElement();
            
            if (executionElement.getSubProcess() != null) {
                while (executionElement.getSubProcess() != null) {
                    if (executionElement.getSubProcess().getId().equals(subProcessId)) {
                        return true;
                    }
                    
                    executionElement = executionElement.getSubProcess();
                }
            }
        }
        
        return false;
    }
    
    protected FlowElement getStartElement(SubProcess subProcess) {
        if (CollectionUtil.isNotEmpty(subProcess.getFlowElements())) {
            for (FlowElement subElement : subProcess.getFlowElements()) {
                if (subElement instanceof StartEvent) {
                    StartEvent startEvent = (StartEvent) subElement;
                    if (CollectionUtil.isEmpty(startEvent.getEventDefinitions())) {
                        return startEvent;
                    }
                }
            }
        }
        return null;
    }

    protected String printFlowElementIds(Collection<FlowElement> flowElements) {
        StringBuilder elementBuilder = new StringBuilder();
        for (FlowElement flowElement : flowElements) {
            if (elementBuilder.length() > 0) {
                elementBuilder.append(", ");
            }
            
            elementBuilder.append(flowElement.getId());
        }
        return elementBuilder.toString();
    }
}
