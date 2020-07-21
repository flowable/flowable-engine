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
package org.flowable.ui.modeler.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.Artifact;
import org.flowable.bpmn.model.Association;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ConditionalEventDefinition;
import org.flowable.bpmn.model.DataObject;
import org.flowable.bpmn.model.ErrorEventDefinition;
import org.flowable.bpmn.model.EscalationEventDefinition;
import org.flowable.bpmn.model.Event;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.EventSubProcess;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.GraphicInfo;
import org.flowable.bpmn.model.Lane;
import org.flowable.bpmn.model.MessageEventDefinition;
import org.flowable.bpmn.model.Pool;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.bpmn.model.SignalEventDefinition;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.TextAnnotation;
import org.flowable.bpmn.model.TimerEventDefinition;
import org.flowable.editor.language.json.converter.BpmnJsonConverter;
import org.flowable.editor.language.json.converter.util.CollectionUtils;
import org.flowable.ui.modeler.domain.AbstractModel;
import org.flowable.ui.modeler.service.mapper.EventInfoMapper;
import org.flowable.ui.modeler.service.mapper.InfoMapper;
import org.flowable.ui.modeler.service.mapper.ReceiveTaskInfoMapper;
import org.flowable.ui.modeler.service.mapper.SequenceFlowInfoMapper;
import org.flowable.ui.modeler.service.mapper.ServiceTaskInfoMapper;
import org.flowable.ui.modeler.service.mapper.UserTaskInfoMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Component
public class BpmnDisplayJsonConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(BpmnDisplayJsonConverter.class);

    protected BpmnJsonConverter bpmnJsonConverter = new BpmnJsonConverter();

    protected ObjectMapper objectMapper = new ObjectMapper();
    protected List<String> eventElementTypes = new ArrayList<>();
    protected Map<String, InfoMapper> propertyMappers = new HashMap<>();

    public BpmnDisplayJsonConverter() {
        eventElementTypes.add("StartEvent");
        eventElementTypes.add("EndEvent");
        eventElementTypes.add("BoundaryEvent");
        eventElementTypes.add("IntermediateCatchEvent");
        eventElementTypes.add("ThrowEvent");

        propertyMappers.put("BoundaryEvent", new EventInfoMapper());
        propertyMappers.put("EndEvent", new EventInfoMapper());
        propertyMappers.put("IntermediateCatchEvent", new EventInfoMapper());
        propertyMappers.put("ReceiveTask", new ReceiveTaskInfoMapper());
        propertyMappers.put("StartEvent", new EventInfoMapper());
        propertyMappers.put("SequenceFlow", new SequenceFlowInfoMapper());
        propertyMappers.put("ServiceTask", new ServiceTaskInfoMapper());
        propertyMappers.put("ThrowEvent", new EventInfoMapper());
        propertyMappers.put("UserTask", new UserTaskInfoMapper());
    }

    public void processProcessElements(AbstractModel processModel, ObjectNode displayNode, GraphicInfo diagramInfo) {
        BpmnModel pojoModel = null;
        if (!StringUtils.isEmpty(processModel.getModelEditorJson())) {
            try {
                JsonNode modelNode = objectMapper.readTree(processModel.getModelEditorJson());
                pojoModel = bpmnJsonConverter.convertToBpmnModel(modelNode);
            } catch (Exception e) {
                LOGGER.error("Error transforming json to pojo {}", processModel.getId(), e);
            }
        }

        if (pojoModel == null || pojoModel.getLocationMap().isEmpty())
            return;

        ArrayNode elementArray = objectMapper.createArrayNode();
        ArrayNode flowArray = objectMapper.createArrayNode();

        if (CollectionUtils.isNotEmpty(pojoModel.getPools())) {
            ArrayNode poolArray = objectMapper.createArrayNode();
            boolean firstElement = true;
            for (Pool pool : pojoModel.getPools()) {
                ObjectNode poolNode = objectMapper.createObjectNode();
                poolNode.put("id", pool.getId());
                poolNode.put("name", pool.getName());
                GraphicInfo poolInfo = pojoModel.getGraphicInfo(pool.getId());
                fillGraphicInfo(poolNode, poolInfo, true);
                org.flowable.bpmn.model.Process process = pojoModel.getProcess(pool.getId());
                if (process != null && CollectionUtils.isNotEmpty(process.getLanes())) {
                    ArrayNode laneArray = objectMapper.createArrayNode();
                    for (Lane lane : process.getLanes()) {
                        ObjectNode laneNode = objectMapper.createObjectNode();
                        laneNode.put("id", lane.getId());
                        laneNode.put("name", lane.getName());
                        fillGraphicInfo(laneNode, pojoModel.getGraphicInfo(lane.getId()), true);
                        laneArray.add(laneNode);
                    }
                    poolNode.set("lanes", laneArray);
                }
                poolArray.add(poolNode);

                double rightX = poolInfo.getX() + poolInfo.getWidth();
                double bottomY = poolInfo.getY() + poolInfo.getHeight();
                double middleX = poolInfo.getX() + (poolInfo.getWidth() / 2);
                if (firstElement || middleX < diagramInfo.getX()) {
                    diagramInfo.setX(middleX);
                }
                if (firstElement || poolInfo.getY() < diagramInfo.getY()) {
                    diagramInfo.setY(poolInfo.getY());
                }
                if (rightX > diagramInfo.getWidth()) {
                    diagramInfo.setWidth(rightX);
                }
                if (bottomY > diagramInfo.getHeight()) {
                    diagramInfo.setHeight(bottomY);
                }
                firstElement = false;
            }
            displayNode.set("pools", poolArray);

        } else {
            // in initialize with fake x and y to make sure the minimal values are set
            diagramInfo.setX(9999);
            diagramInfo.setY(1000);
        }

        for (org.flowable.bpmn.model.Process process : pojoModel.getProcesses()) {
            processElements(process.getFlowElements(), pojoModel, elementArray, flowArray, diagramInfo);
            processArtifacts(process.getArtifacts(), pojoModel, elementArray, flowArray, diagramInfo);
        }

        displayNode.set("elements", elementArray);
        displayNode.set("flows", flowArray);

        displayNode.put("diagramBeginX", diagramInfo.getX());
        displayNode.put("diagramBeginY", diagramInfo.getY());
        displayNode.put("diagramWidth", diagramInfo.getWidth());
        displayNode.put("diagramHeight", diagramInfo.getHeight());
    }

    protected void processElements(Collection<FlowElement> elementList, BpmnModel model, ArrayNode elementArray, ArrayNode flowArray, GraphicInfo diagramInfo) {

        for (FlowElement element : elementList) {
            // ignore data objects in visual representation
            if (DataObject.class.isInstance(element)) {
                continue;

            } else if (element instanceof SequenceFlow) {
                ObjectNode elementNode = objectMapper.createObjectNode();
                SequenceFlow flow = (SequenceFlow) element;
                elementNode.put("id", flow.getId());
                elementNode.put("type", "sequenceFlow");
                elementNode.put("sourceRef", flow.getSourceRef());
                elementNode.put("targetRef", flow.getTargetRef());
                elementNode.put("name", flow.getName());
                List<GraphicInfo> flowInfo = model.getFlowLocationGraphicInfo(flow.getId());
                if (CollectionUtils.isNotEmpty(flowInfo)) {
                    ArrayNode waypointArray = objectMapper.createArrayNode();
                    for (GraphicInfo graphicInfo : flowInfo) {
                        ObjectNode pointNode = objectMapper.createObjectNode();
                        fillGraphicInfo(pointNode, graphicInfo, false);
                        waypointArray.add(pointNode);
                        fillDiagramInfo(graphicInfo, diagramInfo);
                    }
                    elementNode.set("waypoints", waypointArray);

                    String className = element.getClass().getSimpleName();
                    if (propertyMappers.containsKey(className)) {
                        elementNode.set("properties", propertyMappers.get(className).map(element));
                    }

                    flowArray.add(elementNode);
                }

            } else {

                ObjectNode elementNode = objectMapper.createObjectNode();
                elementNode.put("id", element.getId());
                elementNode.put("name", element.getName());

                GraphicInfo graphicInfo = model.getGraphicInfo(element.getId());
                if (graphicInfo != null) {
                    fillGraphicInfo(elementNode, graphicInfo, true);
                    fillDiagramInfo(graphicInfo, diagramInfo);
                }

                String className = element.getClass().getSimpleName();
                elementNode.put("type", className);
                fillEventTypes(className, element, elementNode);

                if (element instanceof ServiceTask) {
                    ServiceTask serviceTask = (ServiceTask) element;
                    if (ServiceTask.MAIL_TASK.equals(serviceTask.getType())) {
                        elementNode.put("taskType", "mail");

                    } else if ("camel".equals(serviceTask.getType())) {
                        elementNode.put("taskType", "camel");

                    } else if ("mule".equals(serviceTask.getType())) {
                        elementNode.put("taskType", "mule");

                    } else if (ServiceTask.HTTP_TASK.equals(serviceTask.getType())) {
                        elementNode.put("taskType", "http");
                    } else if (ServiceTask.SHELL_TASK.equals(serviceTask.getType())) {
                        elementNode.put("taskType", "shell");
                    }

                } else if (element instanceof BoundaryEvent) {
                    BoundaryEvent boundaryEvent = (BoundaryEvent) element;
                    elementNode.put("cancelActivity", boundaryEvent.isCancelActivity());

                } else if (element instanceof StartEvent) {
                    StartEvent startEvent = (StartEvent) element;
                    if (startEvent.getSubProcess() instanceof EventSubProcess && !startEvent.isInterrupting()) {
                        elementNode.put("interrupting", false);
                    } else {
                        elementNode.put("interrupting", true);
                    }
                }

                if (propertyMappers.containsKey(className)) {
                    elementNode.set("properties", propertyMappers.get(className).map(element));
                }

                elementArray.add(elementNode);

                if (element instanceof SubProcess) {
                    SubProcess subProcess = (SubProcess) element;

                    // skip collapsed sub processes
                    if (graphicInfo != null && graphicInfo.getExpanded() != null && !graphicInfo.getExpanded()) {
                        continue;
                    }

                    processElements(subProcess.getFlowElements(), model, elementArray, flowArray, diagramInfo);
                    processArtifacts(subProcess.getArtifacts(), model, elementArray, flowArray, diagramInfo);
                }
            }
        }
    }

    protected void processArtifacts(Collection<Artifact> artifactList, BpmnModel model, ArrayNode elementArray, ArrayNode flowArray, GraphicInfo diagramInfo) {

        for (Artifact artifact : artifactList) {

            if (artifact instanceof Association) {
                ObjectNode elementNode = objectMapper.createObjectNode();
                Association flow = (Association) artifact;
                elementNode.put("id", flow.getId());
                elementNode.put("type", "association");
                elementNode.put("sourceRef", flow.getSourceRef());
                elementNode.put("targetRef", flow.getTargetRef());
                fillWaypoints(flow.getId(), model, elementNode, diagramInfo);
                flowArray.add(elementNode);

            } else {

                ObjectNode elementNode = objectMapper.createObjectNode();
                elementNode.put("id", artifact.getId());

                if (artifact instanceof TextAnnotation) {
                    TextAnnotation annotation = (TextAnnotation) artifact;
                    elementNode.put("text", annotation.getText());
                }

                GraphicInfo graphicInfo = model.getGraphicInfo(artifact.getId());
                if (graphicInfo != null) {
                    fillGraphicInfo(elementNode, graphicInfo, true);
                    fillDiagramInfo(graphicInfo, diagramInfo);
                }

                String className = artifact.getClass().getSimpleName();
                elementNode.put("type", className);

                elementArray.add(elementNode);
            }
        }
    }

    protected void fillWaypoints(String id, BpmnModel model, ObjectNode elementNode, GraphicInfo diagramInfo) {
        List<GraphicInfo> flowInfo = model.getFlowLocationGraphicInfo(id);
        ArrayNode waypointArray = objectMapper.createArrayNode();
        for (GraphicInfo graphicInfo : flowInfo) {
            ObjectNode pointNode = objectMapper.createObjectNode();
            fillGraphicInfo(pointNode, graphicInfo, false);
            waypointArray.add(pointNode);
            fillDiagramInfo(graphicInfo, diagramInfo);
        }
        elementNode.set("waypoints", waypointArray);
    }

    protected void fillEventTypes(String className, FlowElement element, ObjectNode elementNode) {
        if (eventElementTypes.contains(className)) {
            Event event = (Event) element;
            if (CollectionUtils.isNotEmpty(event.getEventDefinitions())) {
                EventDefinition eventDef = event.getEventDefinitions().get(0);
                ObjectNode eventNode = objectMapper.createObjectNode();
                if (eventDef instanceof TimerEventDefinition) {
                    TimerEventDefinition timerDef = (TimerEventDefinition) eventDef;
                    eventNode.put("type", "timer");
                    if (StringUtils.isNotEmpty(timerDef.getTimeCycle())) {
                        eventNode.put("timeCycle", timerDef.getTimeCycle());
                    }
                    if (StringUtils.isNotEmpty(timerDef.getTimeDate())) {
                        eventNode.put("timeDate", timerDef.getTimeDate());
                    }
                    if (StringUtils.isNotEmpty(timerDef.getTimeDuration())) {
                        eventNode.put("timeDuration", timerDef.getTimeDuration());
                    }
                    
                } else if (eventDef instanceof ConditionalEventDefinition) {
                    ConditionalEventDefinition conditionalDef = (ConditionalEventDefinition) eventDef;
                    eventNode.put("type", "conditional");
                    if (StringUtils.isNotEmpty(conditionalDef.getConditionExpression())) {
                        eventNode.put("condition", conditionalDef.getConditionExpression());
                    }

                } else if (eventDef instanceof ErrorEventDefinition) {
                    ErrorEventDefinition errorDef = (ErrorEventDefinition) eventDef;
                    eventNode.put("type", "error");
                    if (StringUtils.isNotEmpty(errorDef.getErrorCode())) {
                        eventNode.put("errorCode", errorDef.getErrorCode());
                    }
                    
                } else if (eventDef instanceof EscalationEventDefinition) {
                    EscalationEventDefinition escalationDef = (EscalationEventDefinition) eventDef;
                    eventNode.put("type", "escalation");
                    if (StringUtils.isNotEmpty(escalationDef.getEscalationCode())) {
                        eventNode.put("escalationCode", escalationDef.getEscalationCode());
                    }

                } else if (eventDef instanceof SignalEventDefinition) {
                    SignalEventDefinition signalDef = (SignalEventDefinition) eventDef;
                    eventNode.put("type", "signal");
                    if (StringUtils.isNotEmpty(signalDef.getSignalRef())) {
                        eventNode.put("signalRef", signalDef.getSignalRef());
                    }

                } else if (eventDef instanceof MessageEventDefinition) {
                    MessageEventDefinition messageDef = (MessageEventDefinition) eventDef;
                    eventNode.put("type", "message");
                    if (StringUtils.isNotEmpty(messageDef.getMessageRef())) {
                        eventNode.put("messageRef", messageDef.getMessageRef());
                    }
                }
                elementNode.set("eventDefinition", eventNode);
            
            } else {
                if (event.getExtensionElements().get("eventType") != null) {
                    List<ExtensionElement> eventTypeElements = event.getExtensionElements().get("eventType");
                    if (eventTypeElements.size() > 0) {
                        ObjectNode eventNode = objectMapper.createObjectNode();
                        eventNode.put("type", "eventRegistry");
                        eventNode.put("eventKey", eventTypeElements.get(0).getElementText());
                        elementNode.set("eventDefinition", eventNode);
                    }
                }
            }
        }
    }

    protected void fillGraphicInfo(ObjectNode elementNode, GraphicInfo graphicInfo, boolean includeWidthAndHeight) {
        commonFillGraphicInfo(elementNode, graphicInfo.getX(), graphicInfo.getY(), graphicInfo.getWidth(), graphicInfo.getHeight(), includeWidthAndHeight);
    }

    protected void commonFillGraphicInfo(ObjectNode elementNode, double x, double y, double width, double height, boolean includeWidthAndHeight) {

        elementNode.put("x", x);
        elementNode.put("y", y);
        if (includeWidthAndHeight) {
            elementNode.put("width", width);
            elementNode.put("height", height);
        }
    }

    protected void fillDiagramInfo(GraphicInfo graphicInfo, GraphicInfo diagramInfo) {
        double rightX = graphicInfo.getX() + graphicInfo.getWidth();
        double bottomY = graphicInfo.getY() + graphicInfo.getHeight();
        double middleX = graphicInfo.getX() + (graphicInfo.getWidth() / 2);
        if (middleX < diagramInfo.getX()) {
            diagramInfo.setX(middleX);
        }
        if (graphicInfo.getY() < diagramInfo.getY()) {
            diagramInfo.setY(graphicInfo.getY());
        }
        if (rightX > diagramInfo.getWidth()) {
            diagramInfo.setWidth(rightX);
        }
        if (bottomY > diagramInfo.getHeight()) {
            diagramInfo.setHeight(bottomY);
        }
    }
}
