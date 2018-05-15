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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.CallActivity;
import org.flowable.bpmn.model.CompensateEventDefinition;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.IOParameter;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.ValuedDataObject;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.dynamic.DynamicStateManager;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.delegate.ActivityBehavior;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.Flowable5Util;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.engine.impl.util.ProcessInstanceHelper;
import org.flowable.engine.repository.ProcessDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 */
public class DefaultDynamicStateManager implements DynamicStateManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDynamicStateManager.class);
    
    @Override
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
    
    @Override
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
    
    @Override
    public void moveExecutionState(List<MoveExecutionEntityContainer> moveExecutionEntityContainerList, Map<String, Object> processVariables,
                                   Map<String, Map<String, Object>> localVariables, CommandContext commandContext) {
        
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);
        
        for (MoveExecutionEntityContainer moveExecutionContainer : moveExecutionEntityContainerList) {
            
            // Get FlowElement objects for every move to activity id
            for (String activityId : moveExecutionContainer.getMoveToActivityIds()) {
                
                BpmnModel bpmnModel = null;
                if (moveExecutionContainer.isMoveToParentProcess()) {
                    String parentProcessDefinitionId = moveExecutionContainer.getSuperExecution().getProcessDefinitionId();
                    bpmnModel = ProcessDefinitionUtil.getBpmnModel(parentProcessDefinitionId);
                    
                } else if (moveExecutionContainer.isMoveToSubProcessInstance()) {
                    // Get first execution to get process definition id
                    ExecutionEntity firstExecution = moveExecutionContainer.getExecutions().get(0);
                    bpmnModel = ProcessDefinitionUtil.getBpmnModel(firstExecution.getProcessDefinitionId());
                    
                    FlowElement callActivityElement = bpmnModel.getFlowElement(moveExecutionContainer.getCallActivityId());
                    
                    if (callActivityElement == null) {
                        throw new FlowableException("Call activity could not be found in process definition for id " + activityId);
                    }
                    
                    CallActivity callActivity = (CallActivity) callActivityElement;
                    moveExecutionContainer.setCallActivity(callActivity);
                    
                    ProcessDefinition subProcessDefinition = null;
                    if (callActivity.isSameDeployment()) {
                        ProcessDefinition processDefinition = ProcessDefinitionUtil.getProcessDefinition(firstExecution.getProcessDefinitionId());
                        ProcessDefinitionEntityManager processDefinitionEntityManager = CommandContextUtil.getProcessDefinitionEntityManager(commandContext);
                        if (firstExecution.getTenantId() == null || ProcessEngineConfiguration.NO_TENANT_ID.equals(firstExecution.getTenantId())) {
                            subProcessDefinition = processDefinitionEntityManager.findProcessDefinitionByDeploymentAndKey(processDefinition.getDeploymentId(), callActivity.getCalledElement());
                        } else {
                            subProcessDefinition = processDefinitionEntityManager.findProcessDefinitionByDeploymentAndKeyAndTenantId(processDefinition.getDeploymentId(), 
                                            callActivity.getCalledElement(), firstExecution.getTenantId());
                        }
                    }
                    
                    if (subProcessDefinition == null) {
                        if (firstExecution.getTenantId() == null || ProcessEngineConfiguration.NO_TENANT_ID.equals(firstExecution.getTenantId())) {
                            subProcessDefinition = CommandContextUtil.getProcessEngineConfiguration().getDeploymentManager()
                                            .findDeployedLatestProcessDefinitionByKey(callActivity.getCalledElement());
                        } else {
                            subProcessDefinition = CommandContextUtil.getProcessEngineConfiguration().getDeploymentManager()
                                            .findDeployedLatestProcessDefinitionByKeyAndTenantId(callActivity.getCalledElement(), firstExecution.getTenantId());
                        }
                    }
                    
                    BpmnModel subProcessModel = ProcessDefinitionUtil.getBpmnModel(subProcessDefinition.getId());
                    moveExecutionContainer.setSubProcessDefinition(subProcessDefinition);
                    moveExecutionContainer.setSubProcessModel(subProcessModel);
                    
                    FlowElement newFlowElement = subProcessModel.getFlowElement(activityId);
                    
                    if (newFlowElement == null) {
                        throw new FlowableException("Activity could not be found in sub process definition for id " + activityId);
                    }
                    
                    moveExecutionContainer.addMoveToFlowElement(activityId, newFlowElement);
                    
                } else {
                    // Get first execution to get process definition id
                    ExecutionEntity firstExecution = moveExecutionContainer.getExecutions().get(0);
                    bpmnModel = ProcessDefinitionUtil.getBpmnModel(firstExecution.getProcessDefinitionId());
                }
                
                if (!moveExecutionContainer.isMoveToSubProcessInstance()) {
                    FlowElement newFlowElement = bpmnModel.getFlowElement(activityId);
                    
                    if (newFlowElement == null) {
                        throw new FlowableException("Activity could not be found in process definition for id " + activityId);
                    }
                    
                    moveExecutionContainer.addMoveToFlowElement(activityId, newFlowElement);
                }
            }
            
            if (moveExecutionContainer.isMoveToParentProcess()) {
                ExecutionEntity firstExecution = moveExecutionContainer.getExecutions().get(0);
                List<ExecutionEntity> subProcessExecutions = executionEntityManager.findChildExecutionsByProcessInstanceId(firstExecution.getProcessInstanceId());
                List<String> executionIds = new ArrayList<>();
                for (ExecutionEntity execution : moveExecutionContainer.getExecutions()) {
                    executionIds.add(execution.getId());
                }
                
                for (ExecutionEntity subProcessExecution : subProcessExecutions) {
                    if (!executionIds.contains(subProcessExecution.getId())) {
                        throw new FlowableException("Execution of sub process instance is not moved " + subProcessExecution.getId());
                    }
                }
                
                // delete the sub process instance
                executionEntityManager.deleteProcessInstance(firstExecution.getProcessInstanceId(), "Change activity to parent process activity ids: " + 
                                printFlowElementIds(moveExecutionContainer.getMoveToFlowElements()), true);
            }
                
            List<ExecutionEntity> currentExecutions = null;
            if (moveExecutionContainer.isMoveToParentProcess()) {
                currentExecutions = Collections.singletonList(moveExecutionContainer.getSuperExecution());
                
            } else {
                currentExecutions = moveExecutionContainer.getExecutions();
            }
            
            Collection<FlowElement> moveToFlowElements = null;
            if (moveExecutionContainer.isMoveToSubProcessInstance()) {
                moveToFlowElements = Collections.singletonList((FlowElement) moveExecutionContainer.getCallActivity());
            } else {
                moveToFlowElements = moveExecutionContainer.getMoveToFlowElements();
            }
            
            // Delete the parent executions for each current execution when the move to activity id has the same sub process scope
            for (ExecutionEntity execution : currentExecutions) {
                if (execution.getParentId() == null) {
                    throw new FlowableException("Execution has no parent execution " + execution.getParentId());
                }
                
                ExecutionEntity continueParentExecution = deleteParentExecutions(execution.getParentId(), moveToFlowElements, commandContext);
                moveExecutionContainer.addContinueParentExecution(execution.getId(), continueParentExecution);
            }
    
            for (ExecutionEntity execution : currentExecutions) {
                executionEntityManager.deleteExecutionAndRelatedData(execution, "Change activity to " + printFlowElementIds(moveToFlowElements));
            }
            
            List<ExecutionEntity> newChildExecutions = createEmbeddedSubProcessExecutions(moveToFlowElements, currentExecutions, moveExecutionContainer, commandContext);
            
            ExecutionEntity defaultContinueParentExecution = null;
            if (moveExecutionContainer.isMoveToSubProcessInstance()) {
                CallActivity callActivity = moveExecutionContainer.getCallActivity();
                Process subProcess = moveExecutionContainer.getSubProcessModel().getProcessById(callActivity.getCalledElement());
                ExecutionEntity subProcessChildExecution = createSubProcessInstance(callActivity, moveExecutionContainer.getSubProcessDefinition(), 
                                newChildExecutions.get(0), subProcess.getInitialFlowElement(), commandContext);
                List<ExecutionEntity> currentSubProcessExecutions = Collections.singletonList(subProcessChildExecution);
                
                MoveExecutionEntityContainer subProcessMoveExecutionEntityContainer = new MoveExecutionEntityContainer(currentSubProcessExecutions, moveExecutionContainer.getMoveToActivityIds());
                subProcessMoveExecutionEntityContainer.addMoveToFlowElement(callActivity.getId(), callActivity);
                
                ExecutionEntity continueParentExecution = deleteParentExecutions(subProcessChildExecution.getParentId(), moveExecutionContainer.getMoveToFlowElements(), commandContext);
                subProcessMoveExecutionEntityContainer.addContinueParentExecution(subProcessChildExecution.getId(), continueParentExecution);
                
                executionEntityManager.deleteExecutionAndRelatedData(subProcessChildExecution, "Change activity to " + printFlowElementIds(moveToFlowElements));
                
                newChildExecutions = createEmbeddedSubProcessExecutions(moveExecutionContainer.getMoveToFlowElements(), 
                                currentSubProcessExecutions, subProcessMoveExecutionEntityContainer, commandContext);
                
                defaultContinueParentExecution = newChildExecutions.get(0);
            
            } else {
            
                // The default parent execution is retrieved from the match with the first source execution 
                defaultContinueParentExecution = moveExecutionContainer.getContinueParentExecution(currentExecutions.get(0).getId());
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
    
    protected List<ExecutionEntity> createEmbeddedSubProcessExecutions(Collection<FlowElement> moveToFlowElements, List<ExecutionEntity> currentExecutions, 
                    MoveExecutionEntityContainer moveExecutionContainer, CommandContext commandContext) {
        
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);
        
        // Resolve the sub process elements that need to be created for each move to flow element
        for (FlowElement flowElement : moveToFlowElements) {
            resolveSubProcesExecutionsToCreate(flowElement.getId(), flowElement.getSubProcess(), currentExecutions, moveExecutionContainer);
        }
        
        // The default parent execution is retrieved from the match with the first source execution 
        ExecutionEntity defaultContinueParentExecution = moveExecutionContainer.getContinueParentExecution(currentExecutions.get(0).getId());
        
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
        for (FlowElement newFlowElement : moveToFlowElements) {
            ExecutionEntity newChildExecution = null;
            
            // Check if a sub process child execution was created for this move to flow element, otherwise use the default continue parent execution
            if (moveExecutionContainer.getSubProcessesToCreateMap().containsKey(newFlowElement.getId())) {
                newChildExecution = moveExecutionContainer.getNewSubProcessChildExecution(
                                moveExecutionContainer.getSubProcessesToCreateMap().get(newFlowElement.getId()).get(0).getId());
            } else {
                newChildExecution = executionEntityManager.createChildExecution(defaultContinueParentExecution);
            }
            
            newChildExecution.setCurrentFlowElement(newFlowElement);
            
            if (newFlowElement instanceof CallActivity) {
                CommandContextUtil.getHistoryManager(commandContext).recordActivityStart(newChildExecution);
            }
            
            newChildExecutions.add(newChildExecution);
        }
        
        return newChildExecutions;
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
    
    protected ExecutionEntity deleteParentExecutions(String parentExecutionId, Collection<FlowElement> moveToFlowElements, CommandContext commandContext) {
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);
        
        ExecutionEntity continueParentExecution = executionEntityManager.findById(parentExecutionId);
        if (continueParentExecution.getCurrentFlowElement() instanceof SubProcess) {
            SubProcess parentSubProcess = (SubProcess) continueParentExecution.getCurrentFlowElement();
            
            if (!isSubProcessUsedInNewFlowElements(parentSubProcess.getId(), moveToFlowElements)) {
                ExecutionEntity toDeleteParentExecution = resolveParentExecutionToDelete(continueParentExecution, moveToFlowElements);
                ExecutionEntity finalDeleteExecution = null;
                if (toDeleteParentExecution != null) {
                    finalDeleteExecution = toDeleteParentExecution;
                } else {
                    finalDeleteExecution = continueParentExecution;
                }
                
                continueParentExecution = finalDeleteExecution.getParent();
                
                String flowElementIdsLine = printFlowElementIds(moveToFlowElements);
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
    
    protected ExecutionEntity createSubProcessInstance(CallActivity callActivity, ProcessDefinition subProcessDefinition, ExecutionEntity parentExecution, 
                    FlowElement initialFlowElement, CommandContext commandContext) {
        
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        ExpressionManager expressionManager = processEngineConfiguration.getExpressionManager();
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);
        
        Process subProcess = ProcessDefinitionUtil.getProcess(subProcessDefinition.getId());
        if (subProcess == null) {
            throw new FlowableException("Cannot start a sub process instance. Process model " + 
                            subProcessDefinition.getName() + " (id = " + subProcessDefinition.getId() + ") could not be found");
        }
        
        String businessKey = null;

        if (!StringUtils.isEmpty(callActivity.getBusinessKey())) {
            Expression expression = expressionManager.createExpression(callActivity.getBusinessKey());
            businessKey = expression.getValue(parentExecution).toString();

        } else if (callActivity.isInheritBusinessKey()) {
            ExecutionEntity processInstance = executionEntityManager.findById(parentExecution.getProcessInstanceId());
            businessKey = processInstance.getBusinessKey();
        }

        ExecutionEntity subProcessInstance = executionEntityManager.createSubprocessInstance(
                        subProcessDefinition, parentExecution, businessKey, initialFlowElement.getId());
        CommandContextUtil.getHistoryManager(commandContext).recordSubProcessInstanceStart(parentExecution, subProcessInstance);

        FlowableEventDispatcher eventDispatcher = processEngineConfiguration.getEventDispatcher();
        if (eventDispatcher.isEnabled()) {
            CommandContextUtil.getProcessEngineConfiguration().getEventDispatcher().dispatchEvent(
                    FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.PROCESS_CREATED, subProcessInstance));
        }

        // process template-defined data objects
        subProcessInstance.setVariables(processDataObjects(subProcess.getDataObjects()));

        Map<String, Object> variables = new HashMap<>();

        if (callActivity.isInheritVariables()) {
            Map<String, Object> executionVariables = parentExecution.getVariables();
            for (Map.Entry<String, Object> entry : executionVariables.entrySet()) {
                variables.put(entry.getKey(), entry.getValue());
            }
        }

        // copy process variables
        for (IOParameter ioParameter : callActivity.getInParameters()) {
            Object value = null;
            if (StringUtils.isNotEmpty(ioParameter.getSourceExpression())) {
                Expression expression = expressionManager.createExpression(ioParameter.getSourceExpression().trim());
                value = expression.getValue(parentExecution);

            } else {
                value = parentExecution.getVariable(ioParameter.getSource());
            }
            variables.put(ioParameter.getTarget(), value);
        }

        if (!variables.isEmpty()) {
            subProcessInstance.setVariables(variables);
        }

        if (eventDispatcher.isEnabled()) {
            eventDispatcher.dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_INITIALIZED, subProcessInstance));
        }

        // Create the first execution that will visit all the process definition elements
        ExecutionEntity subProcessInitialExecution = executionEntityManager.createChildExecution(subProcessInstance);
        subProcessInitialExecution.setCurrentFlowElement(initialFlowElement);
        
        CommandContextUtil.getHistoryManager(commandContext).recordActivityStart(subProcessInitialExecution);
        
        return subProcessInitialExecution;
    }
    
    protected void resolveSubProcesExecutionsToCreate(String searchActivitiyId, SubProcess searchSubProcess, 
                    List<ExecutionEntity> currentExecutions, MoveExecutionEntityContainer moveExecutionContainer) {
        
        if (searchSubProcess != null) {
            
            if (!hasSubProcessId(searchSubProcess.getId(), currentExecutions)) {
                moveExecutionContainer.addSubProcessToCreate(searchActivitiyId, searchSubProcess);
            }
            
            resolveSubProcesExecutionsToCreate(searchActivitiyId, searchSubProcess.getSubProcess(), currentExecutions, moveExecutionContainer);
        }
    }
    
    protected ExecutionEntity resolveParentExecutionToDelete(ExecutionEntity execution, Collection<FlowElement> moveToFlowElements) {
        ExecutionEntity parentExecution = execution.getParent();
        
        if (parentExecution.isProcessInstanceType()) {
            return null;
        }
        
        if (!isSubProcessUsedInNewFlowElements(parentExecution.getActivityId(),  moveToFlowElements)) {
            ExecutionEntity subProcessParentExecution = resolveParentExecutionToDelete(parentExecution, moveToFlowElements);
            if (subProcessParentExecution != null) {
                return subProcessParentExecution;
            } else {
                return parentExecution;
            }
        }
        
        return null;
    }
    
    protected boolean isSubProcessUsedInNewFlowElements(String subProcessId, Collection<FlowElement> moveToFlowElements) {
        boolean isUsed = false;
        for (FlowElement flowElement : moveToFlowElements) {
            SubProcess elementSubProcess = flowElement.getSubProcess();
            if (elementSubProcess != null && elementSubProcess.getId().equals(subProcessId)) {
                isUsed = true;
                break;
            }
        }
        
        return isUsed;
    }
    
    protected boolean hasSubProcessId(String subProcessId, List<ExecutionEntity> currentExecutions) {
        for (ExecutionEntity execution : currentExecutions) {
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
    
    protected Map<String, Object> processDataObjects(Collection<ValuedDataObject> dataObjects) {
        Map<String, Object> variablesMap = new HashMap<>();
        // convert data objects to process variables
        if (dataObjects != null) {
            variablesMap = new HashMap<>(dataObjects.size());
            for (ValuedDataObject dataObject : dataObjects) {
                variablesMap.put(dataObject.getName(), dataObject.getValue());
            }
        }
        return variablesMap;
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
