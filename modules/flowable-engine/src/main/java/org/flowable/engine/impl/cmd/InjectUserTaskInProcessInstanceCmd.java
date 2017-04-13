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
import org.flowable.bpmn.model.ParallelGateway;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.impl.dynamic.DynamicUserTaskBuilder;
import org.flowable.engine.impl.interceptor.Command;
import org.flowable.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.DeploymentEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;

public class InjectUserTaskInProcessInstanceCmd extends AbstractDynamicInjectionCmd implements Command<Void> {

    protected String processInstanceId;
    protected DynamicUserTaskBuilder dynamicUserTaskBuilder;

    public InjectUserTaskInProcessInstanceCmd(String processInstanceId, DynamicUserTaskBuilder dynamicUserTaskBuilder) {
        this.processInstanceId = processInstanceId;
        this.dynamicUserTaskBuilder = dynamicUserTaskBuilder;

        if (this.dynamicUserTaskBuilder.getId() == null) {
            this.dynamicUserTaskBuilder.setId(UUID.randomUUID().toString());
        }
    }

    @Override
    public Void execute(CommandContext commandContext) {
        createDerivedProcessDefinitionForProcessInstance(commandContext, processInstanceId);
        return null;
    }

    @Override
    protected void updateBpmnProcess(CommandContext commandContext, Process process,
            BpmnModel bpmnModel, ProcessDefinitionEntity originalProcessDefinitionEntity, DeploymentEntity newDeploymentEntity) {
        
        List<StartEvent> startEvents = process.findFlowElementsOfType(StartEvent.class);
        StartEvent initialStartEvent = null;
        for (StartEvent startEvent : startEvents) {
            if (startEvent.getEventDefinitions().size() == 0) {
                initialStartEvent = startEvent;
                break;
                
            } else if (initialStartEvent == null) {
                initialStartEvent = startEvent;
            }
        }

        UserTask userTask = new UserTask();
        userTask.setId(dynamicUserTaskBuilder.getId());
        userTask.setName(dynamicUserTaskBuilder.getName());
        userTask.setAssignee(dynamicUserTaskBuilder.getAssignee());
        process.addFlowElement(userTask);

        SequenceFlow flowToUserTask = new SequenceFlow(initialStartEvent.getId(), userTask.getId());
        flowToUserTask.setId("userTask-" + UUID.randomUUID().toString());
        process.addFlowElement(flowToUserTask);

        EndEvent endEvent = new EndEvent();
        endEvent.setId("end-" + UUID.randomUUID().toString());
        process.addFlowElement(endEvent);

        SequenceFlow flowFromUserTask = new SequenceFlow(userTask.getId(), endEvent.getId());
        flowFromUserTask.setId("flow-" + UUID.randomUUID().toString());
        process.addFlowElement(flowFromUserTask);

        ParallelGateway parallelGateway = new ParallelGateway();
        parallelGateway.setId("gateway-" + UUID.randomUUID().toString());
        process.addFlowElement(parallelGateway);

        SequenceFlow forkFlow = new SequenceFlow(parallelGateway.getId(), userTask.getId());
        forkFlow.setId("flow-" + UUID.randomUUID().toString());
        process.addFlowElement(forkFlow);

        initialStartEvent.getOutgoingFlows().get(0).setSourceRef(parallelGateway.getId());

        SequenceFlow flowFromStart = new SequenceFlow(initialStartEvent.getId(), parallelGateway.getId());
        flowFromStart.setId("flow-" + UUID.randomUUID().toString());
        process.addFlowElement(flowFromStart);
    }

    @Override
    protected void updateExecutions(CommandContext commandContext, ProcessDefinitionEntity processDefinitionEntity, 
            ExecutionEntity processInstance, List<ExecutionEntity> childExecutions) {

        ExecutionEntityManager executionEntityManager = commandContext.getExecutionEntityManager();
        ExecutionEntity execution = executionEntityManager.create();
        execution.setProcessInstanceId(processInstance.getId());
        execution.setParentId(processInstance.getId());
        execution.setProcessDefinitionId(processDefinitionEntity.getId());
        execution.setRootProcessInstanceId(processInstance.getRootProcessInstanceId());
        execution.setActive(true);
        execution.setScope(false);
        execution.setTenantId(processInstance.getTenantId());
        execution.setStartTime(processInstance.getStartTime());
        execution.setStartUserId(processInstance.getStartUserId());

        executionEntityManager.insert(execution);

        BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(processDefinitionEntity.getId());
        UserTask userTask = (UserTask) bpmnModel.getProcessById(processDefinitionEntity.getKey()).getFlowElement(dynamicUserTaskBuilder.getId());
        execution.setCurrentFlowElement(userTask);
        
        Context.getAgenda().planContinueProcessOperation(execution);
    }

}