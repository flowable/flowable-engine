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
import java.util.UUID;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowElementsContainer;
import org.flowable.bpmn.model.ParallelGateway;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.impl.dynamic.DynamicUserTaskBuilder;
import org.flowable.engine.impl.interceptor.Command;
import org.flowable.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.DeploymentEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.persistence.entity.TaskEntity;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;

public class InjectParallelUserTaskCmd extends AbstractDynamicInjectionCmd implements Command<Void> {

    protected String taskId;
    protected DynamicUserTaskBuilder dynamicUserTaskBuilder;

    public InjectParallelUserTaskCmd(String taskId, DynamicUserTaskBuilder dynamicUserTaskBuilder) {
        this.taskId = taskId;

        this.dynamicUserTaskBuilder = dynamicUserTaskBuilder;
        if (this.dynamicUserTaskBuilder.getId() == null) {
            this.dynamicUserTaskBuilder.setId(UUID.randomUUID().toString());
        }
    }

    @Override
    public Void execute(CommandContext commandContext) {
        createDerivedProcessDefinitionForTask(commandContext, taskId);
        return null;
    }

    @Override
    protected void updateBpmnProcess(CommandContext commandContext, Process process,
            ProcessDefinitionEntity originalProcessDefinitionEntity, DeploymentEntity newDeploymentEntity) {
        
        TaskEntity taskEntity = commandContext.getTaskEntityManager().findById(taskId);
        FlowElement flowElement = process.getFlowElement(taskEntity.getTaskDefinitionKey());
        if (flowElement == null || !(flowElement instanceof UserTask)) {
            throw new FlowableException("No UserTask instance found for task definition key " + taskEntity.getTaskDefinitionKey());
        }

        UserTask userTask = (UserTask) flowElement;
        FlowElementsContainer parentContainer = userTask.getParentContainer();

        UserTask newUserTask = new UserTask();
        newUserTask.setId(dynamicUserTaskBuilder.getId());
        newUserTask.setName(dynamicUserTaskBuilder.getName());
        newUserTask.setAssignee(dynamicUserTaskBuilder.getAssignee());
        parentContainer.addFlowElement(newUserTask);

        ParallelGateway fork = new ParallelGateway();
        fork.setId("fork-" + UUID.randomUUID().toString());
        parentContainer.addFlowElement(fork);
        userTask.getIncomingFlows().get(0).setTargetRef(fork.getId());

        SequenceFlow forkFlow1 = new SequenceFlow(fork.getId(), userTask.getId());
        forkFlow1.setId("flow-" + UUID.randomUUID().toString());
        parentContainer.addFlowElement(forkFlow1);

        SequenceFlow forkFlow2 = new SequenceFlow(fork.getId(), newUserTask.getId());
        forkFlow2.setId("flow-" + UUID.randomUUID().toString());
        parentContainer.addFlowElement(forkFlow2);

        if (dynamicUserTaskBuilder.isJoinParallelActivitiesOnComplete()) {
            ParallelGateway join = new ParallelGateway();
            join.setId("join-" + UUID.randomUUID().toString());
            parentContainer.addFlowElement(join);

            SequenceFlow joinFlow1 = new SequenceFlow(userTask.getId(), join.getId());
            joinFlow1.setId("flow-" + UUID.randomUUID().toString());
            parentContainer.addFlowElement(joinFlow1);

            SequenceFlow joinFlow2 = new SequenceFlow(newUserTask.getId(), join.getId());
            joinFlow2.setId("flow-" + UUID.randomUUID().toString());
            parentContainer.addFlowElement(joinFlow2);

            SequenceFlow outgoingFlow = userTask.getOutgoingFlows().get(0);
            outgoingFlow.setSourceRef(join.getId());

        } else {
            EndEvent endEvent = new EndEvent();
            endEvent.setId("end-" + UUID.randomUUID().toString());
            parentContainer.addFlowElement(endEvent);

            SequenceFlow endFlow = new SequenceFlow(newUserTask.getId(), endEvent.getId());
            endFlow.setId("flow-" + UUID.randomUUID().toString());
            parentContainer.addFlowElement(endFlow);

        }
    }

    @Override
    protected void updateExecutions(CommandContext commandContext, ProcessDefinitionEntity processDefinitionEntity, 
            ExecutionEntity processInstance, List<ExecutionEntity> childExecutions) {
        
        TaskEntity taskEntity = commandContext.getTaskEntityManager().findById(taskId);
        ExecutionEntity executionAtTask = taskEntity.getExecution();

        ExecutionEntityManager executionEntityManager = commandContext.getExecutionEntityManager();
        ExecutionEntity execution = executionEntityManager.create();
        execution.setProcessInstanceId(processInstance.getId());
        execution.setParentId(executionAtTask.getParentId());
        execution.setProcessDefinitionId(processDefinitionEntity.getId());
        execution.setRootProcessInstanceId(processInstance.getRootProcessInstanceId());
        execution.setActive(true);
        execution.setScope(false);
        execution.setTenantId(processInstance.getTenantId());
        execution.setStartTime(processInstance.getStartTime());
        execution.setStartUserId(processInstance.getStartUserId());
        executionEntityManager.insert(execution);

        BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(processDefinitionEntity.getId());
        UserTask userTask = (UserTask) bpmnModel.getFlowElement(dynamicUserTaskBuilder.getId());
        execution.setCurrentFlowElement(userTask);
        
        Context.getAgenda().planContinueProcessOperation(execution);
    }

}