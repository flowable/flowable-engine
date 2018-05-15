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
import java.util.HashMap;
import java.util.Map;

import org.flowable.bpmn.converter.BpmnXMLConverter;
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
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.util.io.BytesStreamSource;
import org.flowable.engine.impl.persistence.entity.DeploymentEntity;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.persistence.entity.ResourceEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

/**
 * @author Tijs Rademakers
 */
public class DynamicSubProcessJoinInjectUtil extends BaseDynamicSubProcessInjectUtil {
    
    public static void injectSubProcessWithJoin(String taskId, Process process, BpmnModel bpmnModel, DynamicEmbeddedSubProcessBuilder dynamicEmbeddedSubProcessBuilder,
                    ProcessDefinitionEntity originalProcessDefinitionEntity, DeploymentEntity newDeploymentEntity, CommandContext commandContext) {
        
        TaskEntity taskEntity = CommandContextUtil.getTaskService().getTask(taskId);
        FlowElement taskFlowElement = process.getFlowElement(taskEntity.getTaskDefinitionKey(), true);
        if (taskFlowElement == null || !(taskFlowElement instanceof UserTask)) {
            throw new FlowableException("No UserTask instance found for task definition key " + taskEntity.getTaskDefinitionKey());
        }
        UserTask userTask = (UserTask) taskFlowElement;

        if (dynamicEmbeddedSubProcessBuilder.getId() != null && process.getFlowElement(dynamicEmbeddedSubProcessBuilder.getId(), true) != null) {
            throw new FlowableIllegalArgumentException("Invalid sub-process identifier: identifier already exists in host process definition");
        }
        
        GraphicInfo elementGraphicInfo = bpmnModel.getGraphicInfo(userTask.getId());
        
        SubProcess parentSubProcess = new SubProcess();
        String subProcessId = dynamicEmbeddedSubProcessBuilder.nextSubProcessId(process.getFlowElementMap());
        parentSubProcess.setId(subProcessId);
        parentSubProcess.setName(userTask.getName());
        
        for (SequenceFlow incomingFlow : userTask.getIncomingFlows()) {
            incomingFlow.setTargetRef(parentSubProcess.getId());
        }
        parentSubProcess.setIncomingFlows(userTask.getIncomingFlows());
        
        for (SequenceFlow outgoingFlow : userTask.getOutgoingFlows()) {
            outgoingFlow.setSourceRef(parentSubProcess.getId());
        }
        parentSubProcess.setOutgoingFlows(userTask.getOutgoingFlows());
        
        userTask.setIncomingFlows(new ArrayList<>());
        userTask.setOutgoingFlows(new ArrayList<>());
        
        if (elementGraphicInfo != null) {
            elementGraphicInfo.setExpanded(false);
            bpmnModel.addGraphicInfo(parentSubProcess.getId(), elementGraphicInfo);
        }
        
        FlowElementsContainer parentContainer = userTask.getParentContainer();
        
        parentContainer.removeFlowElement(userTask.getId());
        bpmnModel.removeGraphicInfo(userTask.getId());
        parentSubProcess.addFlowElement(userTask);
        
        parentContainer.addFlowElement(parentSubProcess);

        SubProcess subProcess = new SubProcess();
        if (dynamicEmbeddedSubProcessBuilder.getId() != null) {
            subProcess.setId(dynamicEmbeddedSubProcessBuilder.getId());
        } else {
            subProcess.setId(dynamicEmbeddedSubProcessBuilder.nextSubProcessId(process.getFlowElementMap()));
        }
        dynamicEmbeddedSubProcessBuilder.setDynamicSubProcessId(subProcess.getId());

        ProcessDefinition subProcessDefinition = ProcessDefinitionUtil.getProcessDefinition(dynamicEmbeddedSubProcessBuilder.getProcessDefinitionId());
        ResourceEntity subProcessBpmnResource = CommandContextUtil.getResourceEntityManager(commandContext)
                .findResourceByDeploymentIdAndResourceName(subProcessDefinition.getDeploymentId(), subProcessDefinition.getResourceName());
        BpmnModel bpmnModelSubProcess = new BpmnXMLConverter().convertToBpmnModel(new BytesStreamSource(subProcessBpmnResource.getBytes()), false, false);
        for (FlowElement flowElement : bpmnModelSubProcess.getMainProcess().getFlowElements()) {
            subProcess.addFlowElement(flowElement);
        }
        
        processFlowElements(commandContext, process, bpmnModel, originalProcessDefinitionEntity, newDeploymentEntity);
        
        Map<String, FlowElement> generatedIds = new HashMap<>();
        processSubProcessFlowElements(commandContext, subProcess.getId(), process, bpmnModel, subProcess, bpmnModelSubProcess, 
                        originalProcessDefinitionEntity, newDeploymentEntity, generatedIds, (elementGraphicInfo != null));
        
        for (String originalFlowElementId : generatedIds.keySet()) {
            FlowElement duplicateFlowElement = generatedIds.get(originalFlowElementId);
            duplicateFlowElement.getParentContainer().removeFlowElementFromMap(originalFlowElementId);
            duplicateFlowElement.getParentContainer().addFlowElementToMap(duplicateFlowElement);
        }

        parentSubProcess.addFlowElement(subProcess);
        
        StartEvent startEvent = new StartEvent();
        startEvent.setId(dynamicEmbeddedSubProcessBuilder.nextStartEventId(process.getFlowElementMap()));
        parentSubProcess.addFlowElement(startEvent);

        ParallelGateway fork = new ParallelGateway();
        fork.setId(dynamicEmbeddedSubProcessBuilder.nextForkGatewayId(process.getFlowElementMap()));
        parentSubProcess.addFlowElement(fork);
        
        SequenceFlow startFlow1 = new SequenceFlow(startEvent.getId(), fork.getId());
        startFlow1.setId(dynamicEmbeddedSubProcessBuilder.nextFlowId(process.getFlowElementMap()));
        parentSubProcess.addFlowElement(startFlow1);

        SequenceFlow forkFlow1 = new SequenceFlow(fork.getId(), userTask.getId());
        forkFlow1.setId(dynamicEmbeddedSubProcessBuilder.nextFlowId(process.getFlowElementMap()));
        parentSubProcess.addFlowElement(forkFlow1);

        SequenceFlow forkFlow2 = new SequenceFlow(fork.getId(), subProcess.getId());
        forkFlow2.setId(dynamicEmbeddedSubProcessBuilder.nextFlowId(process.getFlowElementMap()));
        parentSubProcess.addFlowElement(forkFlow2);
        
        EndEvent endEvent = new EndEvent();
        endEvent.setId(dynamicEmbeddedSubProcessBuilder.nextEndEventId(process.getFlowElementMap()));
        parentSubProcess.addFlowElement(endEvent);
        
        ParallelGateway join = new ParallelGateway();
        join.setId(dynamicEmbeddedSubProcessBuilder.nextJoinGatewayId(process.getFlowElementMap()));
        parentSubProcess.addFlowElement(join);

        SequenceFlow joinFlow1 = new SequenceFlow(userTask.getId(), join.getId());
        joinFlow1.setId(dynamicEmbeddedSubProcessBuilder.nextFlowId(process.getFlowElementMap()));
        parentSubProcess.addFlowElement(joinFlow1);

        SequenceFlow joinFlow2 = new SequenceFlow(subProcess.getId(), join.getId());
        joinFlow2.setId(dynamicEmbeddedSubProcessBuilder.nextFlowId(process.getFlowElementMap()));
        parentSubProcess.addFlowElement(joinFlow2);

        SequenceFlow endFlow = new SequenceFlow(join.getId(), endEvent.getId());
        endFlow.setId(dynamicEmbeddedSubProcessBuilder.nextFlowId(process.getFlowElementMap()));
        parentSubProcess.addFlowElement(endFlow);

        if (elementGraphicInfo != null) {
            GraphicInfo startGraphicInfo = new GraphicInfo(45, 135, 30, 30);
            bpmnModel.addGraphicInfo(startEvent.getId(), startGraphicInfo);
            
            GraphicInfo forkGraphicInfo = new GraphicInfo(120, 130, 40, 40);
            bpmnModel.addGraphicInfo(fork.getId(), forkGraphicInfo);
            
            bpmnModel.addFlowGraphicInfoList(startFlow1.getId(), createWayPoints(75, 150.093, 120.375, 150.375));
            
            GraphicInfo taskGraphicInfo = new GraphicInfo(205, 30, 80, 100);
            bpmnModel.addGraphicInfo(userTask.getId(), taskGraphicInfo);
            
            bpmnModel.addFlowGraphicInfoList(forkFlow1.getId(), createWayPoints(140.5, 130.5, 140.5, 70, 205, 70));
            
            GraphicInfo newSubProcessGraphicInfo = new GraphicInfo(205, 195, 80, 100);
            newSubProcessGraphicInfo.setExpanded(false);
            bpmnModel.addGraphicInfo(subProcess.getId(), newSubProcessGraphicInfo);
            
            bpmnModel.addFlowGraphicInfoList(forkFlow2.getId(), createWayPoints(140.5, 169.5, 140.5, 235, 205, 235));
            
            GraphicInfo joinGraphicInfo = new GraphicInfo(350, 130, 40, 40);
            bpmnModel.addGraphicInfo(join.getId(), joinGraphicInfo);
            
            bpmnModel.addFlowGraphicInfoList(joinFlow1.getId(), createWayPoints(305, 70, 370, 70, 370, 130));
            
            bpmnModel.addFlowGraphicInfoList(joinFlow2.getId(), createWayPoints(305, 235, 370, 235, 370, 169.5));
            
            GraphicInfo endGraphicInfo = new GraphicInfo(435, 136, 28, 28);
            bpmnModel.addGraphicInfo(endEvent.getId(), endGraphicInfo);
            
            bpmnModel.addFlowGraphicInfoList(endFlow.getId(), createWayPoints(389.621, 150.378, 435, 150.089));
        }
    }
}
