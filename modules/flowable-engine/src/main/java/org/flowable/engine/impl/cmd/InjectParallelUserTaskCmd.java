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

import java.util.ArrayList;
import java.util.List;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowElementsContainer;
import org.flowable.bpmn.model.GraphicInfo;
import org.flowable.bpmn.model.ParallelGateway;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.UserTask;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.impl.dynamic.BaseDynamicSubProcessInjectUtil;
import org.flowable.engine.impl.dynamic.DynamicUserTaskBuilder;
import org.flowable.engine.impl.persistence.entity.DeploymentEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

public class InjectParallelUserTaskCmd extends AbstractDynamicInjectionCmd implements Command<Void> {

    protected String taskId;
    protected DynamicUserTaskBuilder dynamicUserTaskBuilder;

    public InjectParallelUserTaskCmd(String taskId, DynamicUserTaskBuilder dynamicUserTaskBuilder) {
        this.taskId = taskId;

        this.dynamicUserTaskBuilder = dynamicUserTaskBuilder;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        createDerivedProcessDefinitionForTask(commandContext, taskId);
        return null;
    }

    @Override
    protected void updateBpmnProcess(CommandContext commandContext, Process process,
            BpmnModel bpmnModel, ProcessDefinitionEntity originalProcessDefinitionEntity, DeploymentEntity newDeploymentEntity) {
        
        TaskEntity taskEntity = CommandContextUtil.getTaskService().getTask(taskId);
        FlowElement flowElement = process.getFlowElement(taskEntity.getTaskDefinitionKey(), true);
        if (flowElement == null || !(flowElement instanceof UserTask)) {
            throw new FlowableException("No UserTask instance found for task definition key " + taskEntity.getTaskDefinitionKey());
        }
        
        UserTask userTask = (UserTask) flowElement;

        SubProcess subProcess = new SubProcess();
        String subProcessId = dynamicUserTaskBuilder.nextSubProcessId(process.getFlowElementMap());
        subProcess.setId(subProcessId);
        subProcess.setName(flowElement.getName());
        
        for (SequenceFlow incomingFlow : userTask.getIncomingFlows()) {
            incomingFlow.setTargetRef(subProcess.getId());
        }
        subProcess.setIncomingFlows(userTask.getIncomingFlows());
        
        for (SequenceFlow outgoingFlow : userTask.getOutgoingFlows()) {
            outgoingFlow.setSourceRef(subProcess.getId());
        }
        subProcess.setOutgoingFlows(userTask.getOutgoingFlows());
        
        userTask.setIncomingFlows(new ArrayList<SequenceFlow>());
        userTask.setOutgoingFlows(new ArrayList<SequenceFlow>());
        
        GraphicInfo elementGraphicInfo = bpmnModel.getGraphicInfo(flowElement.getId());
        if (elementGraphicInfo != null) {
            elementGraphicInfo.setExpanded(false);
            bpmnModel.addGraphicInfo(subProcess.getId(), elementGraphicInfo);
        }
        
        FlowElementsContainer parentContainer = userTask.getParentContainer();
        
        parentContainer.removeFlowElement(userTask.getId());
        bpmnModel.removeGraphicInfo(userTask.getId());
        subProcess.addFlowElement(userTask);
        
        parentContainer.addFlowElement(subProcess);
        
        StartEvent startEvent = new StartEvent();
        startEvent.setId(dynamicUserTaskBuilder.nextStartEventId(process.getFlowElementMap()));
        subProcess.addFlowElement(startEvent);

        ParallelGateway fork = new ParallelGateway();
        fork.setId(dynamicUserTaskBuilder.nextForkGatewayId(process.getFlowElementMap()));
        subProcess.addFlowElement(fork);
        
        SequenceFlow startFlow1 = new SequenceFlow(startEvent.getId(), fork.getId());
        startFlow1.setId(dynamicUserTaskBuilder.nextFlowId(process.getFlowElementMap()));
        subProcess.addFlowElement(startFlow1);
        
        UserTask newUserTask = new UserTask();
        if (dynamicUserTaskBuilder.getId() != null) {
            newUserTask.setId(dynamicUserTaskBuilder.getId());
        } else {
            newUserTask.setId(dynamicUserTaskBuilder.nextTaskId(process.getFlowElementMap()));
        }
        dynamicUserTaskBuilder.setDynamicTaskId(newUserTask.getId());
        
        newUserTask.setName(dynamicUserTaskBuilder.getName());
        newUserTask.setAssignee(dynamicUserTaskBuilder.getAssignee());
        subProcess.addFlowElement(newUserTask);

        SequenceFlow forkFlow1 = new SequenceFlow(fork.getId(), userTask.getId());
        forkFlow1.setId(dynamicUserTaskBuilder.nextFlowId(process.getFlowElementMap()));
        subProcess.addFlowElement(forkFlow1);

        SequenceFlow forkFlow2 = new SequenceFlow(fork.getId(), newUserTask.getId());
        forkFlow2.setId(dynamicUserTaskBuilder.nextFlowId(process.getFlowElementMap()));
        subProcess.addFlowElement(forkFlow2);

        EndEvent endEvent = new EndEvent();
        endEvent.setId(dynamicUserTaskBuilder.nextEndEventId(process.getFlowElementMap()));
        subProcess.addFlowElement(endEvent);
        
        ParallelGateway join = new ParallelGateway();
        join.setId(dynamicUserTaskBuilder.nextJoinGatewayId(process.getFlowElementMap()));
        subProcess.addFlowElement(join);

        SequenceFlow joinFlow1 = new SequenceFlow(userTask.getId(), join.getId());
        joinFlow1.setId(dynamicUserTaskBuilder.nextFlowId(process.getFlowElementMap()));
        subProcess.addFlowElement(joinFlow1);

        SequenceFlow joinFlow2 = new SequenceFlow(newUserTask.getId(), join.getId());
        joinFlow2.setId(dynamicUserTaskBuilder.nextFlowId(process.getFlowElementMap()));
        subProcess.addFlowElement(joinFlow2);
            
        SequenceFlow endFlow = new SequenceFlow(join.getId(), endEvent.getId());
        endFlow.setId(dynamicUserTaskBuilder.nextFlowId(process.getFlowElementMap()));
        subProcess.addFlowElement(endFlow);

        if (dynamicUserTaskBuilder.getDynamicUserTaskCallback() != null) {
            dynamicUserTaskBuilder.getDynamicUserTaskCallback().handleCreatedDynamicUserTask(newUserTask, subProcess, parentContainer, process);
        }
        
        if (elementGraphicInfo != null) {
            GraphicInfo startGraphicInfo = new GraphicInfo(45, 135, 30, 30);
            bpmnModel.addGraphicInfo(startEvent.getId(), startGraphicInfo);
            
            GraphicInfo forkGraphicInfo = new GraphicInfo(120, 130, 40, 40);
            bpmnModel.addGraphicInfo(fork.getId(), forkGraphicInfo);
            
            bpmnModel.addFlowGraphicInfoList(startFlow1.getId(), createWayPoints(75, 150.093, 120.375, 150.375));
            
            GraphicInfo taskGraphicInfo = new GraphicInfo(205, 30, 80, 100);
            bpmnModel.addGraphicInfo(userTask.getId(), taskGraphicInfo);
            
            bpmnModel.addFlowGraphicInfoList(forkFlow1.getId(), createWayPoints(140.5, 130.5, 140.5, 70, 205, 70));
            
            GraphicInfo newTaskGraphicInfo = new GraphicInfo(205, 195, 80, 100);
            bpmnModel.addGraphicInfo(newUserTask.getId(), newTaskGraphicInfo);
            
            bpmnModel.addFlowGraphicInfoList(forkFlow2.getId(), createWayPoints(140.5, 169.5, 140.5, 235, 205, 235));
            
            GraphicInfo joinGraphicInfo = new GraphicInfo(350, 130, 40, 40);
            bpmnModel.addGraphicInfo(join.getId(), joinGraphicInfo);
            
            bpmnModel.addFlowGraphicInfoList(joinFlow1.getId(), createWayPoints(305, 70, 370, 70, 370, 130));
            
            bpmnModel.addFlowGraphicInfoList(joinFlow2.getId(), createWayPoints(305, 235, 370, 235, 370, 169.5));
            
            GraphicInfo endGraphicInfo = new GraphicInfo(435, 136, 28, 28);
            bpmnModel.addGraphicInfo(endEvent.getId(), endGraphicInfo);
            
            bpmnModel.addFlowGraphicInfoList(endFlow.getId(), createWayPoints(389.621, 150.378, 435, 150.089));
        }
        
        BaseDynamicSubProcessInjectUtil.processFlowElements(commandContext, process, bpmnModel, originalProcessDefinitionEntity, newDeploymentEntity);
    }

    @Override
    protected void updateExecutions(CommandContext commandContext, ProcessDefinitionEntity processDefinitionEntity, 
            ExecutionEntity processInstance, List<ExecutionEntity> childExecutions) {
        
        TaskEntity taskEntity = CommandContextUtil.getTaskService().getTask(taskId);
        
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);
        ExecutionEntity executionAtTask = executionEntityManager.findById(taskEntity.getExecutionId());

        BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(processDefinitionEntity.getId());
        FlowElement taskElement = bpmnModel.getFlowElement(executionAtTask.getCurrentActivityId());
        FlowElement subProcessElement = bpmnModel.getFlowElement(((SubProcess) taskElement.getParentContainer()).getId());
        ExecutionEntity subProcessExecution = executionEntityManager.createChildExecution(executionAtTask.getParent());
        subProcessExecution.setScope(true);
        subProcessExecution.setCurrentFlowElement(subProcessElement);
        CommandContextUtil.getHistoryManager(commandContext).recordActivityStart(subProcessExecution);
        
        executionAtTask.setParent(subProcessExecution);
        
        ExecutionEntity taskExecution = executionEntityManager.createChildExecution(subProcessExecution);

        FlowElement userTaskElement = bpmnModel.getFlowElement(dynamicUserTaskBuilder.getDynamicTaskId());
        taskExecution.setCurrentFlowElement(userTaskElement);
        
        Context.getAgenda().planContinueProcessOperation(taskExecution);
    }
}