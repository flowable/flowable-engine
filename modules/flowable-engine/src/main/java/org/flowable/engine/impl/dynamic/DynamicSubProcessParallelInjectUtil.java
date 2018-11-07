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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.GraphicInfo;
import org.flowable.bpmn.model.ParallelGateway;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.util.io.BytesStreamSource;
import org.flowable.engine.impl.persistence.entity.DeploymentEntity;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.persistence.entity.ResourceEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.engine.repository.ProcessDefinition;

/**
 * @author Tijs Rademakers
 */
public class DynamicSubProcessParallelInjectUtil extends BaseDynamicSubProcessInjectUtil {
    
    public static void injectParallelSubProcess(Process process, BpmnModel bpmnModel, DynamicEmbeddedSubProcessBuilder dynamicEmbeddedSubProcessBuilder,
                    ProcessDefinitionEntity originalProcessDefinitionEntity, DeploymentEntity newDeploymentEntity, CommandContext commandContext) {
        
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
        
        GraphicInfo elementGraphicInfo = bpmnModel.getGraphicInfo(initialStartEvent.getId());
        
        ParallelGateway parallelGateway = new ParallelGateway();
        parallelGateway.setId(dynamicEmbeddedSubProcessBuilder.nextForkGatewayId(process.getFlowElementMap()));
        process.addFlowElement(parallelGateway);

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
                        subProcessDefinition, newDeploymentEntity, generatedIds, (elementGraphicInfo != null));
        
        for (String originalFlowElementId : generatedIds.keySet()) {
            FlowElement duplicateFlowElement = generatedIds.get(originalFlowElementId);
            duplicateFlowElement.getParentContainer().removeFlowElementFromMap(originalFlowElementId);
            duplicateFlowElement.getParentContainer().addFlowElementToMap(duplicateFlowElement);
        }
        
        process.addFlowElement(subProcess);
        
        EndEvent endEvent = new EndEvent();
        endEvent.setId(dynamicEmbeddedSubProcessBuilder.nextEndEventId(process.getFlowElementMap()));
        process.addFlowElement(endEvent);

        SequenceFlow flowToSubProcess = new SequenceFlow(parallelGateway.getId(), subProcess.getId());
        flowToSubProcess.setId(dynamicEmbeddedSubProcessBuilder.nextFlowId(process.getFlowElementMap()));
        process.addFlowElement(flowToSubProcess);

        SequenceFlow flowFromSubProcess = new SequenceFlow(subProcess.getId(), endEvent.getId());
        flowFromSubProcess.setId(dynamicEmbeddedSubProcessBuilder.nextFlowId(process.getFlowElementMap()));
        process.addFlowElement(flowFromSubProcess);

        SequenceFlow initialFlow = initialStartEvent.getOutgoingFlows().get(0);
        initialFlow.setSourceRef(parallelGateway.getId());

        SequenceFlow flowFromStart = new SequenceFlow(initialStartEvent.getId(), parallelGateway.getId());
        flowFromStart.setId(dynamicEmbeddedSubProcessBuilder.nextFlowId(process.getFlowElementMap()));
        process.addFlowElement(flowFromStart);
        
        if (elementGraphicInfo != null) {
            double yDiff = 0;
            double xDiff = 80;
            if (elementGraphicInfo.getY() < 173) {
                yDiff = 173 - elementGraphicInfo.getY();
                elementGraphicInfo.setY(173);
            }
            
            Map<String, GraphicInfo> locationMap = bpmnModel.getLocationMap();
            for (String locationId : locationMap.keySet()) {
                if (initialStartEvent.getId().equals(locationId)) {
                    continue;
                }
                
                GraphicInfo locationGraphicInfo = locationMap.get(locationId);
                locationGraphicInfo.setX(locationGraphicInfo.getX() + xDiff);
                locationGraphicInfo.setY(locationGraphicInfo.getY() + yDiff);
            }
            
            Map<String, List<GraphicInfo>> flowLocationMap = bpmnModel.getFlowLocationMap();
            for (String flowId : flowLocationMap.keySet()) {
                if (flowFromStart.getId().equals(flowId)) {
                    continue;
                }
                
                List<GraphicInfo> flowGraphicInfoList = flowLocationMap.get(flowId);
                for (GraphicInfo flowGraphicInfo : flowGraphicInfoList) {
                    flowGraphicInfo.setX(flowGraphicInfo.getX() + xDiff);
                    flowGraphicInfo.setY(flowGraphicInfo.getY() + yDiff);
                }
            }
            
            GraphicInfo forkGraphicInfo = new GraphicInfo(elementGraphicInfo.getX() + 75, elementGraphicInfo.getY() - 5, 40, 40);
            bpmnModel.addGraphicInfo(parallelGateway.getId(), forkGraphicInfo);
            
            bpmnModel.addFlowGraphicInfoList(flowFromStart.getId(), createWayPoints(elementGraphicInfo.getX() + 30, elementGraphicInfo.getY() + 15, 
                            elementGraphicInfo.getX() + 75, elementGraphicInfo.getY() + 15));
            
            GraphicInfo newSubProcessGraphicInfo = new GraphicInfo(elementGraphicInfo.getX() + 185, elementGraphicInfo.getY() - 163, 80, 100);
            newSubProcessGraphicInfo.setExpanded(false);
            bpmnModel.addGraphicInfo(subProcess.getId(), newSubProcessGraphicInfo);
            
            bpmnModel.addFlowGraphicInfoList(flowToSubProcess.getId(), createWayPoints(elementGraphicInfo.getX() + 95, elementGraphicInfo.getY() - 5, 
                            elementGraphicInfo.getX() + 95, elementGraphicInfo.getY() - 123, elementGraphicInfo.getX() + 185, elementGraphicInfo.getY() - 123));
            
            GraphicInfo endGraphicInfo = new GraphicInfo(elementGraphicInfo.getX() + 335, elementGraphicInfo.getY() - 137, 28, 28);
            bpmnModel.addGraphicInfo(endEvent.getId(), endGraphicInfo);
            
            bpmnModel.addFlowGraphicInfoList(flowFromSubProcess.getId(), createWayPoints(elementGraphicInfo.getX() + 285, elementGraphicInfo.getY() - 123, 
                            elementGraphicInfo.getX() + 335, elementGraphicInfo.getY() - 123));
        }
    }
}
