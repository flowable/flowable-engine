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

import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.MultiInstanceLoopCharacteristics;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.bpmn.behavior.MultiInstanceActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.SequentialMultiInstanceBehavior;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.Flowable5Util;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;

/**
 * @author Tijs Rademakers
 */
public class DeleteMultiInstanceExecutionCmd implements Command<Void>, Serializable {

    private static final long serialVersionUID = 1L;
    
    protected final String NUMBER_OF_INSTANCES = "nrOfInstances";
    protected final String NUMBER_OF_COMPLETED_INSTANCES = "nrOfCompletedInstances";
    
    protected String executionId;
    protected boolean executionIsCompleted;

    public DeleteMultiInstanceExecutionCmd(String executionId, boolean executionIsCompleted) {
        this.executionId = executionId;
        this.executionIsCompleted = executionIsCompleted;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager();
        ExecutionEntity execution = executionEntityManager.findById(executionId);
        
        BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(execution.getProcessDefinitionId());
        Activity miActivityElement = (Activity) bpmnModel.getFlowElement(execution.getActivityId());
        MultiInstanceLoopCharacteristics multiInstanceLoopCharacteristics = miActivityElement.getLoopCharacteristics();
        
        if (miActivityElement.getLoopCharacteristics() == null) {
            throw new FlowableException("No multi instance execution found for execution id " + executionId);
        }
        
        if (!(miActivityElement.getBehavior() instanceof MultiInstanceActivityBehavior)) {
            throw new FlowableException("No multi instance behavior found for execution id " + executionId);
        }
        
        if (Flowable5Util.isFlowable5ProcessDefinitionId(commandContext, execution.getProcessDefinitionId())) {
            throw new FlowableException("Flowable 5 process definitions are not supported");
        }
        
        ExecutionEntity miExecution = getMultiInstanceRootExecution(execution);
        executionEntityManager.deleteChildExecutions(execution, "Delete MI execution", false);
        executionEntityManager.deleteExecutionAndRelatedData(execution, "Delete MI execution");
        
        int loopCounter = 0;
        if (multiInstanceLoopCharacteristics.isSequential()) {
            SequentialMultiInstanceBehavior miBehavior = (SequentialMultiInstanceBehavior) miActivityElement.getBehavior();
            loopCounter = miBehavior.getLoopVariable(execution, miBehavior.getCollectionElementIndexVariable());
        }
        
        if (executionIsCompleted) {
            Integer numberOfCompletedInstances = (Integer) miExecution.getVariable(NUMBER_OF_COMPLETED_INSTANCES);
            miExecution.setVariableLocal(NUMBER_OF_COMPLETED_INSTANCES, numberOfCompletedInstances + 1);
            loopCounter++;
            
        } else {
            Integer currentNumberOfInstances = (Integer) miExecution.getVariable(NUMBER_OF_INSTANCES);
            miExecution.setVariableLocal(NUMBER_OF_INSTANCES, currentNumberOfInstances - 1);
        }
        
        ExecutionEntity childExecution = executionEntityManager.createChildExecution(miExecution);
        childExecution.setCurrentFlowElement(miExecution.getCurrentFlowElement());
        
        if (multiInstanceLoopCharacteristics.isSequential()) {
            SequentialMultiInstanceBehavior miBehavior = (SequentialMultiInstanceBehavior) miActivityElement.getBehavior();
            miBehavior.continueSequentialMultiInstance(childExecution, loopCounter, childExecution);
        }
        
        return null;
    }
    
    protected ExecutionEntity getMultiInstanceRootExecution(ExecutionEntity executionEntity) {
        ExecutionEntity multiInstanceRootExecution = null;
        ExecutionEntity currentExecution = executionEntity;
        while (currentExecution != null && multiInstanceRootExecution == null && currentExecution.getParent() != null) {
            if (currentExecution.isMultiInstanceRoot()) {
                multiInstanceRootExecution = currentExecution;
            } else {
                currentExecution = currentExecution.getParent();
            }
        }
        return multiInstanceRootExecution;
    }
}
