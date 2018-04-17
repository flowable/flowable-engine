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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.CompensateEventDefinition;
import org.flowable.bpmn.model.FieldExtension;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowElementsContainer;
import org.flowable.bpmn.model.GraphicInfo;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.UserTask;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.util.IoUtil;
import org.flowable.dmn.api.DmnDecisionTable;
import org.flowable.dmn.api.DmnRepositoryService;
import org.flowable.engine.impl.persistence.entity.DeploymentEntity;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.persistence.entity.ResourceEntity;
import org.flowable.engine.impl.persistence.entity.ResourceEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.form.api.FormDefinition;
import org.flowable.form.api.FormRepositoryService;

/**
 * @author Tijs Rademakers
 */
public class BaseDynamicSubProcessInjectUtil {
    
    public static void processFlowElements(CommandContext commandContext, FlowElementsContainer process, BpmnModel bpmnModel, 
                    ProcessDefinitionEntity originalProcessDefinitionEntity, DeploymentEntity newDeploymentEntity) {
        
        for (FlowElement flowElement : process.getFlowElements()) {

            processUserTask(flowElement, originalProcessDefinitionEntity, newDeploymentEntity, commandContext);
            processDecisionTask(flowElement, originalProcessDefinitionEntity, newDeploymentEntity, commandContext);
                
            if (flowElement instanceof SubProcess) {
                processFlowElements(commandContext, ((SubProcess) flowElement), bpmnModel, originalProcessDefinitionEntity, newDeploymentEntity);
            }
        }
    }
    
    protected static void processSubProcessFlowElements(CommandContext commandContext, String prefix, Process process, BpmnModel bpmnModel, 
                    SubProcess subProcess, BpmnModel subProcessBpmnModel, ProcessDefinition originalProcessDefinition, 
                    DeploymentEntity newDeploymentEntity, Map<String, FlowElement> generatedIds, boolean includeDiInfo) {
        
        Collection<FlowElement> flowElementsOfSubProcess = subProcess.getFlowElementMap().values(); 
        for (FlowElement flowElement : flowElementsOfSubProcess) {

            if (process.getFlowElement(flowElement.getId(), true) != null) {
                generateIdForDuplicateFlowElement(prefix, process, bpmnModel, subProcessBpmnModel, flowElement, generatedIds, includeDiInfo);
            } else {
                if (includeDiInfo) {
                    if (flowElement instanceof SequenceFlow) {
                        List<GraphicInfo> wayPoints = subProcessBpmnModel.getFlowLocationGraphicInfo(flowElement.getId());
                        if (wayPoints != null) {
                            bpmnModel.addFlowGraphicInfoList(flowElement.getId(), wayPoints);
                        }
                        
                    } else {
                        GraphicInfo graphicInfo = subProcessBpmnModel.getGraphicInfo(flowElement.getId());
                        if (graphicInfo != null) {
                            bpmnModel.addGraphicInfo(flowElement.getId(), subProcessBpmnModel.getGraphicInfo(flowElement.getId()));
                        }
                    }
                }
            }
            
            processUserTask(flowElement, originalProcessDefinition, newDeploymentEntity, commandContext);
            processDecisionTask(flowElement, originalProcessDefinition, newDeploymentEntity, commandContext);

            if (flowElement instanceof SubProcess) {
                processSubProcessFlowElements(commandContext, prefix, process, bpmnModel, (SubProcess) flowElement, 
                        subProcessBpmnModel, originalProcessDefinition, newDeploymentEntity, generatedIds, includeDiInfo);
            }
        }
    }
    
    protected static void generateIdForDuplicateFlowElement(String prefix, org.flowable.bpmn.model.Process process, BpmnModel bpmnModel, 
                    BpmnModel subProcessBpmnModel, FlowElement duplicateFlowElement, Map<String, FlowElement> generatedIds, boolean includeDiInfo) {
        
        String originalFlowElementId = duplicateFlowElement.getId();
        if (process.getFlowElement(originalFlowElementId, true) != null) {
            String newFlowElementId = prefix + "-" + originalFlowElementId;
            int counter = 0;
            boolean maxLengthReached = false;
            while (!maxLengthReached && process.getFlowElement(newFlowElementId, true) != null) {
                newFlowElementId = prefix + (counter++) + "-" + originalFlowElementId;
                if (newFlowElementId.length() > 255) {
                    maxLengthReached = true;
                }
            }

            if (maxLengthReached) {
                newFlowElementId = prefix + "-" + UUID.randomUUID().toString();
            }

            duplicateFlowElement.setId(newFlowElementId);
            generatedIds.put(originalFlowElementId, duplicateFlowElement);
            
            if (includeDiInfo) {
                if (duplicateFlowElement instanceof SequenceFlow) {
                    bpmnModel.addFlowGraphicInfoList(newFlowElementId, subProcessBpmnModel.getFlowLocationGraphicInfo(originalFlowElementId));
                    
                } else {
                    bpmnModel.addGraphicInfo(newFlowElementId, subProcessBpmnModel.getGraphicInfo(originalFlowElementId));
                }
            }

            for (FlowElement flowElement : duplicateFlowElement.getParentContainer().getFlowElements()) {
                if (flowElement instanceof SequenceFlow) {
                    SequenceFlow sequenceFlow = (SequenceFlow) flowElement; 
                    if (sequenceFlow.getSourceRef().equals(originalFlowElementId)) {
                        sequenceFlow.setSourceRef(newFlowElementId);
                    }
                    if (sequenceFlow.getTargetRef().equals(originalFlowElementId)) {
                        sequenceFlow.setTargetRef(newFlowElementId);
                    }

                } else if (flowElement instanceof BoundaryEvent) {
                    BoundaryEvent boundaryEvent = (BoundaryEvent) flowElement;
                    if (boundaryEvent.getAttachedToRefId().equals(originalFlowElementId)) {
                        boundaryEvent.setAttachedToRefId(newFlowElementId);
                    }
                    if (boundaryEvent.getEventDefinitions() != null 
                            && boundaryEvent.getEventDefinitions().size() > 0
                            && (boundaryEvent.getEventDefinitions().get(0) instanceof CompensateEventDefinition)) {
                        
                        CompensateEventDefinition compensateEventDefinition = (CompensateEventDefinition) boundaryEvent.getEventDefinitions().get(0);
                        if (compensateEventDefinition.getActivityRef().equals(originalFlowElementId)) {
                            compensateEventDefinition.setActivityRef(newFlowElementId);
                        }
                    }
                } 
            }
            
            
        }

        if (duplicateFlowElement instanceof FlowElementsContainer) {
            FlowElementsContainer flowElementsContainer = (FlowElementsContainer) duplicateFlowElement;
            for (FlowElement childFlowElement : flowElementsContainer.getFlowElements()) {
                generateIdForDuplicateFlowElement(prefix, process, bpmnModel, subProcessBpmnModel, childFlowElement, generatedIds, includeDiInfo);
            }
        }
    }
    
    protected static void processUserTask(FlowElement flowElement, ProcessDefinition originalProcessDefinitionEntity, 
                    DeploymentEntity newDeploymentEntity, CommandContext commandContext) {
        
        if (flowElement instanceof UserTask) {
            FormRepositoryService formRepositoryService = CommandContextUtil.getFormRepositoryService();
            if (formRepositoryService != null) {
                UserTask userTask = (UserTask) flowElement;
                if (StringUtils.isNotEmpty(userTask.getFormKey())) {
                    FormDefinition formDefinition = formRepositoryService.createFormDefinitionQuery()
                            .formDefinitionKey(userTask.getFormKey()).parentDeploymentId(originalProcessDefinitionEntity.getDeploymentId()).latestVersion().singleResult();
                    if (formDefinition != null) {
                        String name = formDefinition.getResourceName();
                        InputStream inputStream = formRepositoryService.getFormDefinitionResource(formDefinition.getId());
                        addResource(commandContext, newDeploymentEntity, name, IoUtil.readInputStream(inputStream, name));
                        IoUtil.closeSilently(inputStream);
                    }
                }
            }
        }
    }
    
    protected static void processDecisionTask(FlowElement flowElement, ProcessDefinition originalProcessDefinitionEntity, 
                    DeploymentEntity newDeploymentEntity, CommandContext commandContext) {
        
        if (flowElement instanceof ServiceTask && ServiceTask.DMN_TASK.equals(((ServiceTask) flowElement).getType())) {
                    
            DmnRepositoryService dmnRepositoryService = CommandContextUtil.getDmnRepositoryService();
            if (dmnRepositoryService != null) {
                ServiceTask serviceTask = (ServiceTask) flowElement;
                if (serviceTask.getFieldExtensions() != null && serviceTask.getFieldExtensions().size() > 0) {
                    String decisionTableReferenceKey = null;
                    for (FieldExtension fieldExtension : serviceTask.getFieldExtensions()) {
                        if ("decisionTableReferenceKey".equals(fieldExtension.getFieldName())) {
                            decisionTableReferenceKey = fieldExtension.getStringValue();
                            break;
                        }
                    }
    
                    if (decisionTableReferenceKey != null) {
                        DmnDecisionTable dmnDecisionTable = dmnRepositoryService.createDecisionTableQuery()
                                .decisionTableKey(decisionTableReferenceKey).parentDeploymentId(originalProcessDefinitionEntity.getDeploymentId()).latestVersion().singleResult();
                        if (dmnDecisionTable != null) {
                            String name = dmnDecisionTable.getResourceName();
                            InputStream inputStream = dmnRepositoryService.getDmnResource(dmnDecisionTable.getId());
                            addResource(commandContext, newDeploymentEntity, name, IoUtil.readInputStream(inputStream, name));
                            IoUtil.closeSilently(inputStream);
                        }
                    }
                }
            }
        }
    }
    
    public static void addResource(CommandContext commandContext, DeploymentEntity deploymentEntity, String resourceName, byte[] bytes) {
        if (!deploymentEntity.getResources().containsKey(resourceName)) { 
            ResourceEntityManager resourceEntityManager = CommandContextUtil.getResourceEntityManager(commandContext);
            ResourceEntity resourceEntity = resourceEntityManager.create();
            resourceEntity.setDeploymentId(deploymentEntity.getId());
            resourceEntity.setName(resourceName);
            resourceEntity.setBytes(bytes);
            resourceEntityManager.insert(resourceEntity);
            deploymentEntity.addResource(resourceEntity);
        }
    }
    
    protected static List<GraphicInfo> createWayPoints(double x1, double y1, double x2, double y2) {
        List<GraphicInfo> wayPoints = new ArrayList<>();
        wayPoints.add(new GraphicInfo(x1, y1));
        wayPoints.add(new GraphicInfo(x2, y2));
        
        return wayPoints;
    }
    
    protected static List<GraphicInfo> createWayPoints(double x1, double y1, double x2, double y2, double x3, double y3) {
        List<GraphicInfo> wayPoints = createWayPoints(x1, y1, x2, y2);
        wayPoints.add(new GraphicInfo(x3, y3));
        
        return wayPoints;
    }
}
