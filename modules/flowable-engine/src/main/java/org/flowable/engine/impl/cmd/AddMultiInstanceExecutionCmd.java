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
package org.flowable.engine.impl.cmd;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.MultiInstanceLoopCharacteristics;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.Flowable5Util;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.engine.runtime.Execution;

/**
 * @author Tijs Rademakers
 */
public class AddMultiInstanceExecutionCmd implements Command<Execution>, Serializable {

    private static final long serialVersionUID = 1L;
    
    protected final String NUMBER_OF_INSTANCES = "nrOfInstances";
    
    protected String activityId;
    protected String parentExecutionId;
    protected Map<String, Object> executionVariables;

    public AddMultiInstanceExecutionCmd(String activityId, String parentExecutionId, Map<String, Object> executionVariables) {
        this.activityId = activityId;
        this.parentExecutionId = parentExecutionId;
        this.executionVariables = executionVariables;
    }

    @Override
    public Execution execute(CommandContext commandContext) {
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager();
        
        ExecutionEntity miExecution = searchForMultiInstanceActivity(activityId, parentExecutionId, executionEntityManager);
        
        if (miExecution == null) {
            throw new FlowableException("No multi instance execution found for activity id " + activityId);
        }
        
        if (Flowable5Util.isFlowable5ProcessDefinitionId(commandContext, miExecution.getProcessDefinitionId())) {
            throw new FlowableException("Flowable 5 process definitions are not supported");
        }
        
        ExecutionEntity childExecution = executionEntityManager.createChildExecution(miExecution);
        childExecution.setCurrentFlowElement(miExecution.getCurrentFlowElement());
        
        BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(miExecution.getProcessDefinitionId());
        Activity miActivityElement = (Activity) bpmnModel.getFlowElement(miExecution.getActivityId());
        MultiInstanceLoopCharacteristics multiInstanceLoopCharacteristics = miActivityElement.getLoopCharacteristics();
        
        Integer currentNumberOfInstances = (Integer) miExecution.getVariable(NUMBER_OF_INSTANCES);
        miExecution.setVariableLocal(NUMBER_OF_INSTANCES, currentNumberOfInstances + 1);
        
        if (executionVariables != null) {
            childExecution.setVariablesLocal(executionVariables);
        }
        
        if (!multiInstanceLoopCharacteristics.isSequential()) {
            miExecution.setActive(true);
            miExecution.setScope(false);
            
            childExecution.setCurrentFlowElement(miActivityElement);
            CommandContextUtil.getAgenda().planContinueMultiInstanceOperation(childExecution, miExecution, currentNumberOfInstances);
        }
        
        return childExecution;
    }
    
    protected ExecutionEntity searchForMultiInstanceActivity(String activityId, String parentExecutionId, ExecutionEntityManager executionEntityManager) {
        List<ExecutionEntity> childExecutions = executionEntityManager.findChildExecutionsByParentExecutionId(parentExecutionId);
        
        ExecutionEntity miExecution = null;
        for (ExecutionEntity childExecution : childExecutions) {
            if (activityId.equals(childExecution.getActivityId()) && childExecution.isMultiInstanceRoot()) {
                if (miExecution != null) {
                    throw new FlowableException("Multiple multi instance executions found for activity id " + activityId);
                }
                miExecution = childExecution;
            }
            
            ExecutionEntity childMiExecution = searchForMultiInstanceActivity(activityId, childExecution.getId(), executionEntityManager);
            if (childMiExecution != null) {
                if (miExecution != null) {
                    throw new FlowableException("Multiple multi instance executions found for activity id " + activityId);
                }
                miExecution = childMiExecution;
            }
        }
        
        return miExecution;
    }
}
