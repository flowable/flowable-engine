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
import java.util.List;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.common.impl.util.CollectionUtil;
import org.flowable.engine.dynamic.DynamicStateManager;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.Flowable5Util;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.engine.impl.util.ProcessInstanceHelper;

/**
 * @author Tijs Rademakers
 */
public class DefaultDynamicStateManager implements DynamicStateManager {
    
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
    
    public void moveExecutionState(ExecutionEntity execution, String fromActivityId, String toActivityId, CommandContext commandContext) {
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);
        
        BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(execution.getProcessDefinitionId());
        FlowElement cancelActivityElement = bpmnModel.getFlowElement(fromActivityId);
        FlowElement startActivityElement = bpmnModel.getFlowElement(toActivityId);

        if (startActivityElement == null) {
            throw new FlowableException("Activity could not be found in process definition for id " + toActivityId);
        }

        ExecutionEntity continueParentExecution = deleteParentExecutions(execution, cancelActivityElement, startActivityElement, commandContext);

        if (!execution.isDeleted()) {
            executionEntityManager.deleteExecutionAndRelatedData(execution, "Change activity to " + toActivityId);
        }
        
        List<SubProcess> subProcessesToCreate = new ArrayList<>();
        resolveSubProcesExecutionsToCreate(cancelActivityElement, startActivityElement, subProcessesToCreate);
        
        ExecutionEntity lastSubProcessChildExecution = null;
        for (SubProcess subProcess : subProcessesToCreate) {
            FlowElement startElement = getStartElement(subProcess);

            if (startElement == null) {
                throw new FlowableException("No initial activity found for subprocess " + subProcess.getId());
            }
            
            ExecutionEntity newChildExecution = executionEntityManager.createChildExecution(continueParentExecution);
            newChildExecution.setCurrentFlowElement(subProcess);
            newChildExecution.setScope(true);
            
            CommandContextUtil.getHistoryManager(commandContext).recordActivityStart(newChildExecution);
            
            ProcessInstanceHelper processInstanceHelper = CommandContextUtil.getProcessEngineConfiguration(commandContext).getProcessInstanceHelper();
            processInstanceHelper.processAvailableEventSubProcesses(newChildExecution, subProcess, commandContext);

            ExecutionEntity startSubProcessExecution = CommandContextUtil.getExecutionEntityManager(commandContext)
                    .createChildExecution(newChildExecution);
            startSubProcessExecution.setCurrentFlowElement(startElement);
            
            lastSubProcessChildExecution = startSubProcessExecution;
        }
        
        ExecutionEntity newChildExecution = null;
        if (lastSubProcessChildExecution != null) {
            newChildExecution = lastSubProcessChildExecution;
        } else {
            newChildExecution = executionEntityManager.createChildExecution(continueParentExecution);
        }
        
        newChildExecution.setCurrentFlowElement(startActivityElement);
        CommandContextUtil.getAgenda().planContinueProcessOperation(newChildExecution);
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
    
    protected ExecutionEntity deleteParentExecutions(ExecutionEntity execution, FlowElement cancelActivityElement, FlowElement startActivityElement, CommandContext commandContext) {
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);
        
        ExecutionEntity continueParentExecution = execution.getParent();
        if (cancelActivityElement.getSubProcess() != null) {
            ExecutionEntity toDeleteParentExecution = resolveParentExecutionToDelete(execution, cancelActivityElement, startActivityElement);
            if (toDeleteParentExecution != null) {
                continueParentExecution = toDeleteParentExecution.getParent();
                executionEntityManager.deleteChildExecutions(toDeleteParentExecution, "Change activity to " + startActivityElement.getId(), true);
                executionEntityManager.deleteExecutionAndRelatedData(toDeleteParentExecution, "Change activity to " + startActivityElement.getId());
            }
        }
        
        return continueParentExecution;
    }
    
    protected void resolveSubProcesExecutionsToCreate(FlowElement cancelActivityElement, FlowElement searchElement, List<SubProcess> subProcesses) {
        if (searchElement.getSubProcess() != null) {
            if (cancelActivityElement.getSubProcess() == null) {
                subProcesses.add(0, cancelActivityElement.getSubProcess());
                
            } else {
                boolean foundSubProcessId = false;
                FlowElement childElement = cancelActivityElement;
                while (childElement.getSubProcess() != null) {
                    if (childElement.getSubProcess().getId().equals(searchElement.getSubProcess().getId())) {
                        foundSubProcessId = true;
                        break;
                    }
                    
                    childElement = childElement.getSubProcess();
                }
                
                if (!foundSubProcessId) {
                    subProcesses.add(0, cancelActivityElement.getSubProcess());
                }
            }
            
            resolveSubProcesExecutionsToCreate(cancelActivityElement, searchElement.getSubProcess(), subProcesses);
        }
    }
    
    protected ExecutionEntity resolveParentExecutionToDelete(ExecutionEntity execution, FlowElement cancelActivityElement, FlowElement startActivityElement) {
        ExecutionEntity parentExecution = execution.getParent();
        
        if (parentExecution.isProcessInstanceType()) {
            return null;
        }
        
        if (cancelActivityElement.getSubProcess() != null) {
            if (startActivityElement.getSubProcess() == null ||
                    !startActivityElement.getSubProcess().getId().equals(parentExecution.getActivityId())) {

                ExecutionEntity subProcessParentExecution = resolveParentExecutionToDelete(parentExecution, cancelActivityElement.getSubProcess(), startActivityElement);
                if (subProcessParentExecution != null) {
                    return subProcessParentExecution;
                } else {
                    return parentExecution;
                }
            }
        }
        
        return null;
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

}
