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

import java.util.List;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.impl.interceptor.Command;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.runtime.ChangeActivityStateBuilderImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.Flowable5Util;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;

/**
 * @author Tijs Rademakers
 */
public class ChangeActivityStateCmd implements Command<Void> {

    protected final String processInstanceId;
    protected final String cancelActivityId;
    protected final String startActivityId;

    public ChangeActivityStateCmd(ChangeActivityStateBuilderImpl changeActivityStateBuilder) {
        this.processInstanceId = changeActivityStateBuilder.getProcessInstanceId();
        this.cancelActivityId = changeActivityStateBuilder.getCancelActivityId();
        this.startActivityId = changeActivityStateBuilder.getStartActivityId();
    }

    public Void execute(CommandContext commandContext) {
        if (processInstanceId == null) {
            throw new FlowableIllegalArgumentException("Process instance id is required");
        }

        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);
        ExecutionEntity execution = executionEntityManager.findById(processInstanceId);

        if (execution == null) {
            throw new FlowableException("Execution could not be found with id " + processInstanceId);
        }

        if (!execution.isProcessInstanceType()) {
            throw new FlowableException("Execution is not a process instance type execution for id " + processInstanceId);
        }

        if (Flowable5Util.isFlowable5ProcessDefinitionId(commandContext, execution.getProcessDefinitionId())) {
            throw new FlowableException("Flowable 5 process definitions are not supported");
        }

        ExecutionEntity activeExecutionEntity = null;
        List<ExecutionEntity> childExecutions = executionEntityManager.findChildExecutionsByProcessInstanceId(execution.getId());
        for (ExecutionEntity childExecution : childExecutions) {
            if (childExecution.getCurrentActivityId().equals(cancelActivityId)) {
                activeExecutionEntity = childExecution;
            }
        }

        if (activeExecutionEntity == null) {
            throw new FlowableException("Active execution could not be found with activity id " + cancelActivityId);
        }

        BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(execution.getProcessDefinitionId());
        FlowElement cancelActivityElement = bpmnModel.getFlowElement(cancelActivityId);
        FlowElement startActivityElement = bpmnModel.getFlowElement(startActivityId);

        if (startActivityElement == null) {
            throw new FlowableException("Activity could not be found in process definition for id " + startActivityId);
        }

        boolean deleteParentExecution = false;
        ExecutionEntity parentExecution = activeExecutionEntity.getParent();
        if (cancelActivityElement.getSubProcess() != null) {
            if (startActivityElement.getSubProcess() == null ||
                    !startActivityElement.getSubProcess().getId().equals(parentExecution.getActivityId())) {

                deleteParentExecution = true;
            }
        }

        executionEntityManager.deleteExecutionAndRelatedData(activeExecutionEntity, "Change activity to " + startActivityId);

        if (deleteParentExecution) {
            executionEntityManager.deleteExecutionAndRelatedData(parentExecution, "Change activity to " + startActivityId);
        }

        ExecutionEntity newChildExecution = executionEntityManager.createChildExecution(execution);
        newChildExecution.setCurrentFlowElement(startActivityElement);
        CommandContextUtil.getAgenda().planContinueProcessOperation(newChildExecution);

        return null;
    }

}
