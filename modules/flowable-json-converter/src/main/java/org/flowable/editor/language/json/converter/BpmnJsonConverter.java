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
package org.flowable.editor.language.json.converter;

import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.Artifact;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.Event;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowElementsContainer;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.Gateway;
import org.flowable.bpmn.model.GraphicInfo;
import org.flowable.bpmn.model.Lane;
import org.flowable.bpmn.model.Message;
import org.flowable.bpmn.model.MessageEventDefinition;
import org.flowable.bpmn.model.MessageFlow;
import org.flowable.bpmn.model.Pool;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.Signal;
import org.flowable.bpmn.model.SignalEventDefinition;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.ValuedDataObject;
import org.flowable.editor.constants.EditorJsonConstants;
import org.flowable.editor.constants.StencilConstants;
import org.flowable.editor.language.json.converter.util.CollectionUtils;
import org.flowable.editor.language.json.converter.util.JsonConverterUtil;
import org.flowable.editor.language.json.model.ModelInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tijs Rademakers
 */
public class BpmnJsonConverter implements EditorJsonConstants, StencilConstants, ActivityProcessor {

    protected static final Logger LOGGER = LoggerFactory.getLogger(BpmnJsonConverter.class);

    protected ObjectMapper objectMapper = new ObjectMapper();

    protected static Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap = new HashMap<>();
    protected static Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap = new HashMap<>();

    public static final String MODELER_NAMESPACE = "http://flowable.org/modeler";
    protected static final DateFormat defaultFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    protected static final DateFormat entFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");

    static {

        // start and end events
        StartEventJsonConverter.fillTypes(convertersToBpmnMap, convertersToJsonMap);
        EndEventJsonConverter.fillTypes(convertersToBpmnMap, convertersToJsonMap);

        // connectors
        SequenceFlowJsonConverter.fillTypes(convertersToBpmnMap, convertersToJsonMap);
        MessageFlowJsonConverter.fillTypes(convertersToBpmnMap, convertersToJsonMap);
        AssociationJsonConverter.fillTypes(convertersToBpmnMap, convertersToJsonMap);

        // task types
        BusinessRuleTaskJsonConverter.fillTypes(convertersToBpmnMap, convertersToJsonMap);
        MailTaskJsonConverter.fillTypes(convertersToBpmnMap, convertersToJsonMap);
        ManualTaskJsonConverter.fillTypes(convertersToBpmnMap, convertersToJsonMap);
        ReceiveTaskJsonConverter.fillTypes(convertersToBpmnMap, convertersToJsonMap);
        ScriptTaskJsonConverter.fillTypes(convertersToBpmnMap, convertersToJsonMap);
        ServiceTaskJsonConverter.fillTypes(convertersToBpmnMap, convertersToJsonMap);
        ShellTaskJsonConverter.fillTypes(convertersToBpmnMap, convertersToJsonMap);
        UserTaskJsonConverter.fillTypes(convertersToBpmnMap, convertersToJsonMap);
        CallActivityJsonConverter.fillTypes(convertersToBpmnMap, convertersToJsonMap);
        CamelTaskJsonConverter.fillTypes(convertersToBpmnMap, convertersToJsonMap);
        MuleTaskJsonConverter.fillTypes(convertersToBpmnMap, convertersToJsonMap);
        HttpTaskJsonConverter.fillTypes(convertersToBpmnMap, convertersToJsonMap);
        SendTaskJsonConverter.fillTypes(convertersToBpmnMap, convertersToJsonMap);
        DecisionTaskJsonConverter.fillTypes(convertersToBpmnMap, convertersToJsonMap);

        // gateways
        ExclusiveGatewayJsonConverter.fillTypes(convertersToBpmnMap, convertersToJsonMap);
        InclusiveGatewayJsonConverter.fillTypes(convertersToBpmnMap, convertersToJsonMap);
        ParallelGatewayJsonConverter.fillTypes(convertersToBpmnMap, convertersToJsonMap);
        EventGatewayJsonConverter.fillTypes(convertersToBpmnMap, convertersToJsonMap);

        // scope constructs
        SubProcessJsonConverter.fillTypes(convertersToBpmnMap, convertersToJsonMap);
        EventSubProcessJsonConverter.fillTypes(convertersToBpmnMap, convertersToJsonMap);
        AdhocSubProcessJsonConverter.fillTypes(convertersToBpmnMap, convertersToJsonMap);

        // catch events
        CatchEventJsonConverter.fillTypes(convertersToBpmnMap, convertersToJsonMap);

        // throw events
        ThrowEventJsonConverter.fillTypes(convertersToBpmnMap, convertersToJsonMap);

        // boundary events
        BoundaryEventJsonConverter.fillTypes(convertersToBpmnMap, convertersToJsonMap);

        // artifacts
        TextAnnotationJsonConverter.fillTypes(convertersToBpmnMap, convertersToJsonMap);
        DataStoreJsonConverter.fillTypes(convertersToBpmnMap, convertersToJsonMap);
    }

    private static final List<String> DI_CIRCLES = new ArrayList<>();
    private static final List<String> DI_RECTANGLES = new ArrayList<>();
    private static final List<String> DI_GATEWAY = new ArrayList<>();

    static {
        DI_CIRCLES.add(STENCIL_EVENT_START_ERROR);
        DI_CIRCLES.add(STENCIL_EVENT_START_MESSAGE);
        DI_CIRCLES.add(STENCIL_EVENT_START_NONE);
        DI_CIRCLES.add(STENCIL_EVENT_START_TIMER);
        DI_CIRCLES.add(STENCIL_EVENT_START_SIGNAL);

        DI_CIRCLES.add(STENCIL_EVENT_BOUNDARY_ERROR);
        DI_CIRCLES.add(STENCIL_EVENT_BOUNDARY_SIGNAL);
        DI_CIRCLES.add(STENCIL_EVENT_BOUNDARY_TIMER);
        DI_CIRCLES.add(STENCIL_EVENT_BOUNDARY_MESSAGE);
        DI_CIRCLES.add(STENCIL_EVENT_BOUNDARY_CANCEL);
        DI_CIRCLES.add(STENCIL_EVENT_BOUNDARY_COMPENSATION);

        DI_CIRCLES.add(STENCIL_EVENT_CATCH_MESSAGE);
        DI_CIRCLES.add(STENCIL_EVENT_CATCH_SIGNAL);
        DI_CIRCLES.add(STENCIL_EVENT_CATCH_TIMER);

        DI_CIRCLES.add(STENCIL_EVENT_THROW_NONE);
        DI_CIRCLES.add(STENCIL_EVENT_THROW_SIGNAL);

        DI_CIRCLES.add(STENCIL_EVENT_END_NONE);
        DI_CIRCLES.add(STENCIL_EVENT_END_ERROR);
        DI_CIRCLES.add(STENCIL_EVENT_END_CANCEL);
        DI_CIRCLES.add(STENCIL_EVENT_END_TERMINATE);

        DI_RECTANGLES.add(STENCIL_CALL_ACTIVITY);
        DI_RECTANGLES.add(STENCIL_SUB_PROCESS);
        DI_RECTANGLES.add(STENCIL_COLLAPSED_SUB_PROCESS);
        DI_RECTANGLES.add(STENCIL_EVENT_SUB_PROCESS);
        DI_RECTANGLES.add(STENCIL_ADHOC_SUB_PROCESS);
        DI_RECTANGLES.add(STENCIL_TASK_BUSINESS_RULE);
        DI_RECTANGLES.add(STENCIL_TASK_MAIL);
        DI_RECTANGLES.add(STENCIL_TASK_MANUAL);
        DI_RECTANGLES.add(STENCIL_TASK_RECEIVE);
        DI_RECTANGLES.add(STENCIL_TASK_SCRIPT);
        DI_RECTANGLES.add(STENCIL_TASK_SEND);
        DI_RECTANGLES.add(STENCIL_TASK_SERVICE);
        DI_RECTANGLES.add(STENCIL_TASK_USER);
        DI_RECTANGLES.add(STENCIL_TASK_CAMEL);
        DI_RECTANGLES.add(STENCIL_TASK_MULE);
        DI_RECTANGLES.add(STENCIL_TASK_HTTP);
        DI_RECTANGLES.add(STENCIL_TASK_DECISION);
        DI_RECTANGLES.add(STENCIL_TASK_SHELL);
        DI_RECTANGLES.add(STENCIL_TEXT_ANNOTATION);

        DI_GATEWAY.add(STENCIL_GATEWAY_EVENT);
        DI_GATEWAY.add(STENCIL_GATEWAY_EXCLUSIVE);
        DI_GATEWAY.add(STENCIL_GATEWAY_INCLUSIVE);
        DI_GATEWAY.add(STENCIL_GATEWAY_PARALLEL);
    }

    protected double lineWidth = 0.05d;

    public ObjectNode convertToJson(BpmnModel model) {
        return convertToJson(model, null, null);
    }

    public ObjectNode convertToJson(BpmnModel model, Map<String, ModelInfo> formKeyMap, Map<String, ModelInfo> decisionTableKeyMap) {
        ObjectNode modelNode = objectMapper.createObjectNode();
        double maxX = 0.0;
        double maxY = 0.0;
        for (GraphicInfo flowInfo : model.getLocationMap().values()) {
            if ((flowInfo.getX() + flowInfo.getWidth()) > maxX) {
                maxX = flowInfo.getX() + flowInfo.getWidth();
            }

            if ((flowInfo.getY() + flowInfo.getHeight()) > maxY) {
                maxY = flowInfo.getY() + flowInfo.getHeight();
            }
        }
        maxX += 50;
        maxY += 50;

        if (maxX < 1485) {
            maxX = 1485;
        }

        if (maxY < 700) {
            maxY = 700;
        }

        modelNode.set("bounds", BpmnJsonConverterUtil.createBoundsNode(maxX, maxY, 0, 0));
        modelNode.put("resourceId", "canvas");

        ObjectNode stencilNode = objectMapper.createObjectNode();
        stencilNode.put("id", "BPMNDiagram");
        modelNode.set("stencil", stencilNode);

        ObjectNode stencilsetNode = objectMapper.createObjectNode();
        stencilsetNode.put("namespace", "http://b3mn.org/stencilset/bpmn2.0#");
        stencilsetNode.put("url", "../editor/stencilsets/bpmn2.0/bpmn2.0.json");
        modelNode.set("stencilset", stencilsetNode);

        ArrayNode shapesArrayNode = objectMapper.createArrayNode();

        Process mainProcess = null;
        if (model.getPools().size() > 0) {
            mainProcess = model.getProcess(model.getPools().get(0).getId());
        } else {
            mainProcess = model.getMainProcess();
        }

        ObjectNode propertiesNode = objectMapper.createObjectNode();
        if (StringUtils.isNotEmpty(mainProcess.getId())) {
            propertiesNode.put(PROPERTY_PROCESS_ID, mainProcess.getId());
        }
        if (StringUtils.isNotEmpty(mainProcess.getName())) {
            propertiesNode.put(PROPERTY_NAME, mainProcess.getName());
        }
        if (StringUtils.isNotEmpty(mainProcess.getDocumentation())) {
            propertiesNode.put(PROPERTY_DOCUMENTATION, mainProcess.getDocumentation());
        }
        if (!mainProcess.isExecutable()) {
            propertiesNode.put(PROPERTY_IS_EXECUTABLE, "false");
        }
        if (StringUtils.isNoneEmpty(model.getTargetNamespace())) {
            propertiesNode.put(PROPERTY_PROCESS_NAMESPACE, model.getTargetNamespace());
        }
        if (CollectionUtils.isNotEmpty(mainProcess.getCandidateStarterGroups())) {
            propertiesNode.put(PROPERTY_PROCESS_POTENTIALSTARTERGROUP, StringUtils.join(mainProcess.getCandidateStarterGroups(), ","));
        }
        if (CollectionUtils.isNotEmpty(mainProcess.getCandidateStarterUsers())) {
            propertiesNode.put(PROPERTY_PROCESS_POTENTIALSTARTERUSER, StringUtils.join(mainProcess.getCandidateStarterUsers(), ","));
        }
        
        if (mainProcess.getExtensionElements().containsKey("historyLevel")) {
            List<ExtensionElement> historyExtensionElements = mainProcess.getExtensionElements().get("historyLevel");
            if (historyExtensionElements != null && historyExtensionElements.size() > 0) {
                String historyLevel = historyExtensionElements.get(0).getElementText();
                if (StringUtils.isNotEmpty(historyLevel)) {
                    propertiesNode.put(PROPERTY_PROCESS_HISTORYLEVEL, historyLevel);
                }
            }
        }
        
        propertiesNode.put(PROPERTY_IS_EAGER_EXECUTION_FETCHING, Boolean.valueOf(mainProcess.isEnableEagerExecutionTreeFetching()));

        BpmnJsonConverterUtil.convertMessagesToJson(model.getMessages(), propertiesNode);

        BpmnJsonConverterUtil.convertListenersToJson(mainProcess.getExecutionListeners(), true, propertiesNode);
        BpmnJsonConverterUtil.convertEventListenersToJson(mainProcess.getEventListeners(), propertiesNode);
        BpmnJsonConverterUtil.convertSignalDefinitionsToJson(model, propertiesNode);
        BpmnJsonConverterUtil.convertMessagesToJson(model, propertiesNode);

        if (CollectionUtils.isNotEmpty(mainProcess.getDataObjects())) {
            BpmnJsonConverterUtil.convertDataPropertiesToJson(mainProcess.getDataObjects(), propertiesNode);
        }

        modelNode.set(EDITOR_SHAPE_PROPERTIES, propertiesNode);

        boolean poolHasDI = false;
        if (model.getPools().size() > 0) {
            for (Pool pool : model.getPools()) {
                GraphicInfo graphicInfo = model.getGraphicInfo(pool.getId());
                if (graphicInfo != null) {
                    poolHasDI = true;
                    break;
                }
            }
        }

        if (model.getPools().size() > 0 && poolHasDI) {
            for (Pool pool : model.getPools()) {
                GraphicInfo poolGraphicInfo = model.getGraphicInfo(pool.getId());
                if (poolGraphicInfo == null)
                    continue;
                ObjectNode poolNode = BpmnJsonConverterUtil.createChildShape(pool.getId(), STENCIL_POOL, poolGraphicInfo.getX() + poolGraphicInfo.getWidth(),
                        poolGraphicInfo.getY() + poolGraphicInfo.getHeight(), poolGraphicInfo.getX(), poolGraphicInfo.getY());
                shapesArrayNode.add(poolNode);
                ObjectNode poolPropertiesNode = objectMapper.createObjectNode();
                poolPropertiesNode.put(PROPERTY_OVERRIDE_ID, pool.getId());
                poolPropertiesNode.put(PROPERTY_PROCESS_ID, pool.getProcessRef());
                if (!pool.isExecutable()) {
                    poolPropertiesNode.put(PROPERTY_IS_EXECUTABLE, "false");
                }
                if (StringUtils.isNotEmpty(pool.getName())) {
                    poolPropertiesNode.put(PROPERTY_NAME, pool.getName());
                }
                poolNode.set(EDITOR_SHAPE_PROPERTIES, poolPropertiesNode);

                ArrayNode laneShapesArrayNode = objectMapper.createArrayNode();
                poolNode.set(EDITOR_CHILD_SHAPES, laneShapesArrayNode);

                ArrayNode outgoingArrayNode = objectMapper.createArrayNode();
                poolNode.set("outgoing", outgoingArrayNode);

                Process process = model.getProcess(pool.getId());
                if (process != null) {
                    Map<String, ArrayNode> laneMap = new HashMap<>();
                    for (Lane lane : process.getLanes()) {
                        GraphicInfo laneGraphicInfo = model.getGraphicInfo(lane.getId());
                        if (laneGraphicInfo == null)
                            continue;
                        ObjectNode laneNode = BpmnJsonConverterUtil.createChildShape(lane.getId(), STENCIL_LANE, laneGraphicInfo.getX() + laneGraphicInfo.getWidth() - poolGraphicInfo.getX(),
                                laneGraphicInfo.getY() + laneGraphicInfo.getHeight() - poolGraphicInfo.getY(), laneGraphicInfo.getX() - poolGraphicInfo.getX(), laneGraphicInfo.getY() - poolGraphicInfo.getY());
                        laneShapesArrayNode.add(laneNode);
                        ObjectNode lanePropertiesNode = objectMapper.createObjectNode();
                        lanePropertiesNode.put(PROPERTY_OVERRIDE_ID, lane.getId());
                        if (StringUtils.isNotEmpty(lane.getName())) {
                            lanePropertiesNode.put(PROPERTY_NAME, lane.getName());
                        }
                        laneNode.set(EDITOR_SHAPE_PROPERTIES, lanePropertiesNode);

                        ArrayNode elementShapesArrayNode = objectMapper.createArrayNode();
                        laneNode.set(EDITOR_CHILD_SHAPES, elementShapesArrayNode);
                        laneNode.set("outgoing", objectMapper.createArrayNode());

                        laneMap.put(lane.getId(), elementShapesArrayNode);
                    }

                    for (FlowElement flowElement : process.getFlowElements()) {

                        Lane laneForElement = null;
                        GraphicInfo laneGraphicInfo = null;

                        FlowElement lookForElement = null;
                        if (flowElement instanceof SequenceFlow) {
                            SequenceFlow sequenceFlow = (SequenceFlow) flowElement;
                            lookForElement = model.getFlowElement(sequenceFlow.getSourceRef());

                        } else {
                            lookForElement = flowElement;
                        }

                        for (Lane lane : process.getLanes()) {
                            if (lane.getFlowReferences().contains(lookForElement.getId())) {
                                laneGraphicInfo = model.getGraphicInfo(lane.getId());
                                if (laneGraphicInfo != null) {
                                    laneForElement = lane;
                                }
                                break;
                            }
                        }

                        if (flowElement instanceof SequenceFlow || laneForElement != null) {
                            processFlowElement(flowElement, process, model, laneMap.get(laneForElement.getId()), formKeyMap,
                                    decisionTableKeyMap, laneGraphicInfo.getX(), laneGraphicInfo.getY());
                        }
                    }

                    processArtifacts(process, model, shapesArrayNode, 0.0, 0.0);
                }

                for (MessageFlow messageFlow : model.getMessageFlows().values()) {
                    if (messageFlow.getSourceRef().equals(pool.getId())) {
                        outgoingArrayNode.add(BpmnJsonConverterUtil.createResourceNode(messageFlow.getId()));
                    }
                }
            }

            processMessageFlows(model, shapesArrayNode);

        } else {
            processFlowElements(model.getMainProcess(), model, shapesArrayNode, formKeyMap, decisionTableKeyMap, 0.0, 0.0);
            processMessageFlows(model, shapesArrayNode);
        }

        modelNode.set(EDITOR_CHILD_SHAPES, shapesArrayNode);
        return modelNode;
    }

    @Override
    public void processFlowElements(FlowElementsContainer container, BpmnModel model, ArrayNode shapesArrayNode,
            Map<String, ModelInfo> formKeyMap, Map<String, ModelInfo> decisionTableKeyMap, double subProcessX, double subProcessY) {

        for (FlowElement flowElement : container.getFlowElements()) {
            processFlowElement(flowElement, container, model, shapesArrayNode, formKeyMap, decisionTableKeyMap, subProcessX, subProcessY);
        }

        processArtifacts(container, model, shapesArrayNode, subProcessX, subProcessY);
    }

    protected void processFlowElement(FlowElement flowElement, FlowElementsContainer container, BpmnModel model,
            ArrayNode shapesArrayNode, Map<String, ModelInfo> formKeyMap, Map<String, ModelInfo> decisionTableKeyMap, double containerX, double containerY) {

        Class<? extends BaseBpmnJsonConverter> converter = convertersToJsonMap.get(flowElement.getClass());
        if (converter != null) {
            try {
                BaseBpmnJsonConverter converterInstance = converter.newInstance();
                if (converterInstance instanceof FormKeyAwareConverter) {
                    ((FormKeyAwareConverter) converterInstance).setFormKeyMap(formKeyMap);
                }
                if (converterInstance instanceof DecisionTableKeyAwareConverter) {
                    ((DecisionTableKeyAwareConverter) converterInstance).setDecisionTableKeyMap(decisionTableKeyMap);
                }

                converterInstance.convertToJson(flowElement, this, model, container, shapesArrayNode, containerX, containerY);

            } catch (Exception e) {
                LOGGER.error("Error converting {}", flowElement, e);
            }
        }
    }

    protected void processArtifacts(FlowElementsContainer container, BpmnModel model, ArrayNode shapesArrayNode, double containerX, double containerY) {

        for (Artifact artifact : container.getArtifacts()) {
            Class<? extends BaseBpmnJsonConverter> converter = convertersToJsonMap.get(artifact.getClass());
            if (converter != null) {
                try {
                    converter.newInstance().convertToJson(artifact, this, model, container, shapesArrayNode, containerX, containerY);
                } catch (Exception e) {
                    LOGGER.error("Error converting {}", artifact, e);
                }
            }
        }
    }

    protected void processMessageFlows(BpmnModel model, ArrayNode shapesArrayNode) {
        for (MessageFlow messageFlow : model.getMessageFlows().values()) {
            MessageFlowJsonConverter jsonConverter = new MessageFlowJsonConverter();
            jsonConverter.convertToJson(messageFlow, this, model, null, shapesArrayNode, 0.0, 0.0);
        }
    }

    public BpmnModel convertToBpmnModel(JsonNode modelNode) {
        return convertToBpmnModel(modelNode, null, null);
    }

    public BpmnModel convertToBpmnModel(JsonNode modelNode, Map<String, String> formKeyMap, Map<String, String> decisionTableKeyMap) {

        BpmnModel bpmnModel = new BpmnModel();

        bpmnModel.setTargetNamespace("http://flowable.org/test");
        Map<String, JsonNode> shapeMap = new HashMap<>();
        Map<String, JsonNode> sourceRefMap = new HashMap<>();
        Map<String, JsonNode> edgeMap = new HashMap<>();
        Map<String, List<JsonNode>> sourceAndTargetMap = new HashMap<>();

        readShapeDI(modelNode, 0, 0, shapeMap, sourceRefMap, bpmnModel);
        filterAllEdges(modelNode, edgeMap, sourceAndTargetMap, shapeMap, sourceRefMap);
        readEdgeDI(edgeMap, sourceAndTargetMap, bpmnModel);

        ArrayNode shapesArrayNode = (ArrayNode) modelNode.get(EDITOR_CHILD_SHAPES);

        if (shapesArrayNode == null || shapesArrayNode.size() == 0) {
            return bpmnModel;
        }

        boolean nonEmptyPoolFound = false;
        Map<String, Lane> elementInLaneMap = new HashMap<>();
        // first create the pool structure
        for (JsonNode shapeNode : shapesArrayNode) {
            String stencilId = BpmnJsonConverterUtil.getStencilId(shapeNode);
            if (STENCIL_POOL.equals(stencilId)) {
                Pool pool = new Pool();
                pool.setId(BpmnJsonConverterUtil.getElementId(shapeNode));
                pool.setName(JsonConverterUtil.getPropertyValueAsString(PROPERTY_NAME, shapeNode));
                pool.setProcessRef(JsonConverterUtil.getPropertyValueAsString(PROPERTY_PROCESS_ID, shapeNode));
                pool.setExecutable(JsonConverterUtil.getPropertyValueAsBoolean(PROPERTY_IS_EXECUTABLE, shapeNode, true));
                bpmnModel.getPools().add(pool);

                Process process = new Process();
                process.setId(pool.getProcessRef());
                process.setName(pool.getName());
                process.setExecutable(pool.isExecutable());
                process.setEnableEagerExecutionTreeFetching(JsonConverterUtil.getPropertyValueAsBoolean(PROPERTY_IS_EAGER_EXECUTION_FETCHING, shapeNode, false));
                bpmnModel.addProcess(process);

                ArrayNode laneArrayNode = (ArrayNode) shapeNode.get(EDITOR_CHILD_SHAPES);
                for (JsonNode laneNode : laneArrayNode) {
                    // should be a lane, but just check to be certain
                    String laneStencilId = BpmnJsonConverterUtil.getStencilId(laneNode);
                    if (STENCIL_LANE.equals(laneStencilId)) {
                        nonEmptyPoolFound = true;
                        Lane lane = new Lane();
                        lane.setId(BpmnJsonConverterUtil.getElementId(laneNode));
                        lane.setName(JsonConverterUtil.getPropertyValueAsString(PROPERTY_NAME, laneNode));
                        lane.setParentProcess(process);
                        process.getLanes().add(lane);

                        processJsonElements(laneNode.get(EDITOR_CHILD_SHAPES), modelNode, lane, shapeMap, formKeyMap, decisionTableKeyMap, bpmnModel);
                        if (CollectionUtils.isNotEmpty(lane.getFlowReferences())) {
                            for (String elementRef : lane.getFlowReferences()) {
                                elementInLaneMap.put(elementRef, lane);
                            }
                        }
                    }
                }
            }
        }

        // Signal Definitions exist on the root level
        JsonNode signalDefinitionNode = BpmnJsonConverterUtil.getProperty(PROPERTY_SIGNAL_DEFINITIONS, modelNode);
        signalDefinitionNode = BpmnJsonConverterUtil.validateIfNodeIsTextual(signalDefinitionNode);
        signalDefinitionNode = BpmnJsonConverterUtil.validateIfNodeIsTextual(signalDefinitionNode); // no idea why this needs to be done twice ..
        if (signalDefinitionNode != null) {
            if (signalDefinitionNode instanceof ArrayNode) {
                ArrayNode signalDefinitionArrayNode = (ArrayNode) signalDefinitionNode;
                Iterator<JsonNode> signalDefinitionIterator = signalDefinitionArrayNode.iterator();
                while (signalDefinitionIterator.hasNext()) {
                    JsonNode signalDefinitionJsonNode = signalDefinitionIterator.next();
                    String signalId = signalDefinitionJsonNode.get(PROPERTY_SIGNAL_DEFINITION_ID).asText();
                    String signalName = signalDefinitionJsonNode.get(PROPERTY_SIGNAL_DEFINITION_NAME).asText();
                    String signalScope = signalDefinitionJsonNode.get(PROPERTY_SIGNAL_DEFINITION_SCOPE).asText();

                    if (StringUtils.isNotEmpty(signalId) && StringUtils.isNotEmpty(signalName)) {
                        Signal signal = new Signal();
                        signal.setId(signalId);
                        signal.setName(signalName);
                        signal.setScope((signalScope.toLowerCase().equals("processinstance")) ? Signal.SCOPE_PROCESS_INSTANCE : Signal.SCOPE_GLOBAL);
                        bpmnModel.addSignal(signal);
                    }
                }
            }
        }

        if (!nonEmptyPoolFound) {
            Process process = new Process();
            bpmnModel.getProcesses().add(process);
            process.setId(BpmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_PROCESS_ID, modelNode));
            process.setName(BpmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_NAME, modelNode));
            String namespace = BpmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_PROCESS_NAMESPACE, modelNode);
            if (StringUtils.isNotEmpty(namespace)) {
                bpmnModel.setTargetNamespace(namespace);
            }
            process.setDocumentation(BpmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_DOCUMENTATION, modelNode));
            JsonNode processExecutableNode = JsonConverterUtil.getProperty(PROPERTY_IS_EXECUTABLE, modelNode);
            if (processExecutableNode != null && StringUtils.isNotEmpty(processExecutableNode.asText())) {
                process.setExecutable(JsonConverterUtil.getPropertyValueAsBoolean(PROPERTY_IS_EXECUTABLE, modelNode));
            }
            String historyLevel = BpmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_PROCESS_HISTORYLEVEL, modelNode);
            if (StringUtils.isNotEmpty(historyLevel)) {
                ExtensionElement historyExtensionElement = new ExtensionElement();
                historyExtensionElement.setName("historyLevel");
                historyExtensionElement.setNamespace("http://flowable.org/bpmn");
                historyExtensionElement.setNamespacePrefix("flowable");
                historyExtensionElement.setElementText(historyLevel);
                process.addExtensionElement(historyExtensionElement);
            }

            BpmnJsonConverterUtil.convertJsonToMessages(modelNode, bpmnModel);

            BpmnJsonConverterUtil.convertJsonToListeners(modelNode, process);
            JsonNode eventListenersNode = BpmnJsonConverterUtil.getProperty(PROPERTY_EVENT_LISTENERS, modelNode);
            if (eventListenersNode != null) {
                eventListenersNode = BpmnJsonConverterUtil.validateIfNodeIsTextual(eventListenersNode);
                BpmnJsonConverterUtil.parseEventListeners(eventListenersNode.get(PROPERTY_EVENTLISTENER_VALUE), process);
            }

            JsonNode processDataPropertiesNode = modelNode.get(EDITOR_SHAPE_PROPERTIES).get(PROPERTY_DATA_PROPERTIES);

            if (processDataPropertiesNode != null) {
                List<ValuedDataObject> dataObjects = BpmnJsonConverterUtil.convertJsonToDataProperties(processDataPropertiesNode, process);
                process.setDataObjects(dataObjects);
                process.getFlowElements().addAll(dataObjects);
            }

            String userStarterValue = BpmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_PROCESS_POTENTIALSTARTERUSER, modelNode);
            String groupStarterValue = BpmnJsonConverterUtil.getPropertyValueAsString(PROPERTY_PROCESS_POTENTIALSTARTERGROUP, modelNode);

            if (StringUtils.isNotEmpty(userStarterValue)) {
                List<String> userStarters = new ArrayList<>();
                String userStartArray[] = userStarterValue.split(",");

                userStarters.addAll(Arrays.asList(userStartArray));

                process.setCandidateStarterUsers(userStarters);
            }

            if (StringUtils.isNotEmpty(groupStarterValue)) {
                List<String> groupStarters = new ArrayList<>();
                String groupStarterArray[] = groupStarterValue.split(",");

                groupStarters.addAll(Arrays.asList(groupStarterArray));

                process.setCandidateStarterGroups(groupStarters);
            }
            
            process.setEnableEagerExecutionTreeFetching(JsonConverterUtil.getPropertyValueAsBoolean(PROPERTY_IS_EAGER_EXECUTION_FETCHING, modelNode, false));

            processJsonElements(shapesArrayNode, modelNode, process, shapeMap, formKeyMap, decisionTableKeyMap, bpmnModel);

        } else {
            // sequence flows are on root level so need additional parsing for pools
            for (JsonNode shapeNode : shapesArrayNode) {
                if (STENCIL_SEQUENCE_FLOW.equalsIgnoreCase(BpmnJsonConverterUtil.getStencilId(shapeNode)) || STENCIL_ASSOCIATION.equalsIgnoreCase(BpmnJsonConverterUtil.getStencilId(shapeNode))) {

                    String sourceRef = BpmnJsonConverterUtil.lookForSourceRef(shapeNode.get(EDITOR_SHAPE_ID).asText(), modelNode.get(EDITOR_CHILD_SHAPES));
                    if (sourceRef != null) {
                        Lane lane = elementInLaneMap.get(sourceRef);
                        SequenceFlowJsonConverter flowConverter = new SequenceFlowJsonConverter();
                        if (lane != null) {
                            flowConverter.convertToBpmnModel(shapeNode, modelNode, this, lane, shapeMap, bpmnModel);
                        } else {
                            flowConverter.convertToBpmnModel(shapeNode, modelNode, this, bpmnModel.getProcesses().get(0), shapeMap, bpmnModel);
                        }
                    }
                }
            }
        }

        // sequence flows are now all on root level
        Map<String, SubProcess> subShapesMap = new HashMap<>();
        for (Process process : bpmnModel.getProcesses()) {
            for (FlowElement flowElement : process.findFlowElementsOfType(SubProcess.class)) {
                SubProcess subProcess = (SubProcess) flowElement;
                fillSubShapes(subShapesMap, subProcess);
            }

            if (subShapesMap.size() > 0) {
                List<String> removeSubFlowsList = new ArrayList<>();
                for (FlowElement flowElement : process.findFlowElementsOfType(SequenceFlow.class)) {
                    SequenceFlow sequenceFlow = (SequenceFlow) flowElement;
                    if (subShapesMap.containsKey(sequenceFlow.getSourceRef())) {
                        SubProcess subProcess = subShapesMap.get(sequenceFlow.getSourceRef());
                        if (subProcess.getFlowElement(sequenceFlow.getId()) == null) {
                            subProcess.addFlowElement(sequenceFlow);
                            removeSubFlowsList.add(sequenceFlow.getId());
                        }
                    }
                }

                List<SubProcess> collapsedSubProcess = new ArrayList<>();
                for (SubProcess subProcess : subShapesMap.values()) {
                    // determine if its a collapsed subprocess
                    GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(subProcess.getId());
                    if (graphicInfo != null && Boolean.FALSE.equals(graphicInfo.getExpanded())){
                        collapsedSubProcess.add(subProcess);
                    }
                }

                for (String flowId : removeSubFlowsList) {
                    process.removeFlowElement(flowId);

                    // check if the sequenceflow to remove is not assigned to a collapsed subprocess.
                    for (SubProcess subProcess : collapsedSubProcess) {
                        subProcess.removeFlowElement(flowId);
                    }
                }
            }
        }

        Map<String, FlowWithContainer> allFlowMap = new HashMap<>();
        List<Gateway> gatewayWithOrderList = new ArrayList<>();

        // post handling of process elements
        for (Process process : bpmnModel.getProcesses()) {
            postProcessElements(process, process.getFlowElements(), edgeMap, bpmnModel, allFlowMap, gatewayWithOrderList);
        }

        // sort the sequence flows
        for (Gateway gateway : gatewayWithOrderList) {
            List<ExtensionElement> orderList = gateway.getExtensionElements().get("EDITOR_FLOW_ORDER");
            if (CollectionUtils.isNotEmpty(orderList)) {
                for (ExtensionElement orderElement : orderList) {
                    String flowValue = orderElement.getElementText();
                    if (StringUtils.isNotEmpty(flowValue)) {
                        if (allFlowMap.containsKey(flowValue)) {
                            FlowWithContainer flowWithContainer = allFlowMap.get(flowValue);
                            flowWithContainer.getFlowContainer().removeFlowElement(flowWithContainer.getSequenceFlow().getId());
                            flowWithContainer.getFlowContainer().addFlowElement(flowWithContainer.getSequenceFlow());
                        }
                    }
                }
            }
            gateway.getExtensionElements().remove("EDITOR_FLOW_ORDER");
        }

        return bpmnModel;
    }

    @Override
    public void processJsonElements(JsonNode shapesArrayNode, JsonNode modelNode, BaseElement parentElement, Map<String, JsonNode> shapeMap,
            Map<String, String> formMap, Map<String, String> decisionTableMap, BpmnModel bpmnModel) {

        for (JsonNode shapeNode : shapesArrayNode) {
            String stencilId = BpmnJsonConverterUtil.getStencilId(shapeNode);
            Class<? extends BaseBpmnJsonConverter> converter = convertersToBpmnMap.get(stencilId);
            try {
                BaseBpmnJsonConverter converterInstance = converter.newInstance();
                if (converterInstance instanceof DecisionTableAwareConverter) {
                    ((DecisionTableAwareConverter) converterInstance).setDecisionTableMap(decisionTableMap);
                }

                if (converterInstance instanceof FormAwareConverter) {
                    ((FormAwareConverter) converterInstance).setFormMap(formMap);
                }

                converterInstance.convertToBpmnModel(shapeNode, modelNode, this, parentElement, shapeMap, bpmnModel);
            } catch (Exception e) {
                LOGGER.error("Error converting {}", BpmnJsonConverterUtil.getStencilId(shapeNode), e);
            }
        }
    }

    private void fillSubShapes(Map<String, SubProcess> subShapesMap, SubProcess subProcess) {
        for (FlowElement flowElement : subProcess.getFlowElements()) {
            if (flowElement instanceof SubProcess) {
                SubProcess childSubProcess = (SubProcess) flowElement;
                subShapesMap.put(childSubProcess.getId(), subProcess);
                fillSubShapes(subShapesMap, childSubProcess);
            } else {
                subShapesMap.put(flowElement.getId(), subProcess);
            }
        }
    }

    private void postProcessElements(FlowElementsContainer parentContainer, Collection<FlowElement> flowElementList, Map<String, JsonNode> edgeMap, BpmnModel bpmnModel,
            Map<String, FlowWithContainer> allFlowMap, List<Gateway> gatewayWithOrderList) {

        for (FlowElement flowElement : flowElementList) {

            parentContainer.addFlowElementToMap(flowElement);

            if (flowElement instanceof Event) {
                Event event = (Event) flowElement;
                if (CollectionUtils.isNotEmpty(event.getEventDefinitions())) {
                    EventDefinition eventDef = event.getEventDefinitions().get(0);
                    if (eventDef instanceof SignalEventDefinition) {
                        SignalEventDefinition signalEventDef = (SignalEventDefinition) eventDef;
                        if (StringUtils.isNotEmpty(signalEventDef.getSignalRef())) {
                            if (bpmnModel.getSignal(signalEventDef.getSignalRef()) == null) {
                                bpmnModel.addSignal(new Signal(signalEventDef.getSignalRef(), signalEventDef.getSignalRef()));
                            }
                        }

                    } else if (eventDef instanceof MessageEventDefinition) {
                        MessageEventDefinition messageEventDef = (MessageEventDefinition) eventDef;
                        if (StringUtils.isNotEmpty(messageEventDef.getMessageRef())) {
                            if (bpmnModel.getMessage(messageEventDef.getMessageRef()) == null) {
                                bpmnModel.addMessage(new Message(messageEventDef.getMessageRef(), messageEventDef.getMessageRef(), null));
                            }
                        }
                    }
                }
            }

            if (flowElement instanceof BoundaryEvent) {
                BoundaryEvent boundaryEvent = (BoundaryEvent) flowElement;
                Activity activity = retrieveAttachedRefObject(boundaryEvent.getAttachedToRefId(), parentContainer.getFlowElements());

                if (activity == null) {
                    LOGGER.warn("Boundary event {} is not attached to any activity", boundaryEvent.getId());
                } else {
                    boundaryEvent.setAttachedToRef(activity);
                    activity.getBoundaryEvents().add(boundaryEvent);
                }

            } else if (flowElement instanceof Gateway) {
                if (flowElement.getExtensionElements().containsKey("EDITOR_FLOW_ORDER")) {
                    gatewayWithOrderList.add((Gateway) flowElement);
                }

            } else if (flowElement instanceof SubProcess) {
                SubProcess subProcess = (SubProcess) flowElement;
                postProcessElements(subProcess, subProcess.getFlowElements(), edgeMap, bpmnModel, allFlowMap, gatewayWithOrderList);

            } else if (flowElement instanceof SequenceFlow) {
                SequenceFlow sequenceFlow = (SequenceFlow) flowElement;
                FlowElement sourceFlowElement = parentContainer.getFlowElement(sequenceFlow.getSourceRef());
                if (sourceFlowElement instanceof FlowNode) {

                    FlowWithContainer flowWithContainer = new FlowWithContainer(sequenceFlow, parentContainer);
                    if (sequenceFlow.getExtensionElements().get("EDITOR_RESOURCEID") != null && sequenceFlow.getExtensionElements().get("EDITOR_RESOURCEID").size() > 0) {
                        allFlowMap.put(sequenceFlow.getExtensionElements().get("EDITOR_RESOURCEID").get(0).getElementText(), flowWithContainer);
                        sequenceFlow.getExtensionElements().remove("EDITOR_RESOURCEID");
                    }

                    ((FlowNode) sourceFlowElement).getOutgoingFlows().add(sequenceFlow);
                    JsonNode edgeNode = edgeMap.get(sequenceFlow.getId());
                    if (edgeNode != null) {
                        boolean isDefault = JsonConverterUtil.getPropertyValueAsBoolean(PROPERTY_SEQUENCEFLOW_DEFAULT, edgeNode);
                        if (isDefault) {
                            if (sourceFlowElement instanceof Activity) {
                                ((Activity) sourceFlowElement).setDefaultFlow(sequenceFlow.getId());
                            } else if (sourceFlowElement instanceof Gateway) {
                                ((Gateway) sourceFlowElement).setDefaultFlow(sequenceFlow.getId());
                            }
                        }
                    }
                }
                FlowElement targetFlowElement = parentContainer.getFlowElement(sequenceFlow.getTargetRef());
                if (targetFlowElement instanceof FlowNode) {
                    ((FlowNode) targetFlowElement).getIncomingFlows().add(sequenceFlow);
                }
            }
        }
    }

    private Activity retrieveAttachedRefObject(String attachedToRefId, Collection<FlowElement> flowElementList) {
        Activity activity = null;
        if (StringUtils.isNotEmpty(attachedToRefId)) {
            for (FlowElement flowElement : flowElementList) {
                if (attachedToRefId.equals(flowElement.getId())) {
                    activity = (Activity) flowElement;
                    break;

                } else if (flowElement instanceof SubProcess) {
                    SubProcess subProcess = (SubProcess) flowElement;
                    Activity retrievedActivity = retrieveAttachedRefObject(attachedToRefId, subProcess.getFlowElements());
                    if (retrievedActivity != null) {
                        activity = retrievedActivity;
                        break;
                    }
                }
            }
        }
        return activity;
    }

    private void readShapeDI(JsonNode objectNode, double parentX, double parentY, Map<String, JsonNode> shapeMap, Map<String, JsonNode> sourceRefMap, BpmnModel bpmnModel) {

        if (objectNode.get(EDITOR_CHILD_SHAPES) != null) {
            for (JsonNode jsonChildNode : objectNode.get(EDITOR_CHILD_SHAPES)) {

                String stencilId = BpmnJsonConverterUtil.getStencilId(jsonChildNode);
                if (!STENCIL_SEQUENCE_FLOW.equals(stencilId)) {

                    GraphicInfo graphicInfo = new GraphicInfo();

                    JsonNode boundsNode = jsonChildNode.get(EDITOR_BOUNDS);
                    ObjectNode upperLeftNode = (ObjectNode) boundsNode.get(EDITOR_BOUNDS_UPPER_LEFT);
                    ObjectNode lowerRightNode = (ObjectNode) boundsNode.get(EDITOR_BOUNDS_LOWER_RIGHT);

                    graphicInfo.setX(upperLeftNode.get(EDITOR_BOUNDS_X).asDouble() + parentX);
                    graphicInfo.setY(upperLeftNode.get(EDITOR_BOUNDS_Y).asDouble() + parentY);
                    graphicInfo.setWidth(lowerRightNode.get(EDITOR_BOUNDS_X).asDouble() - graphicInfo.getX() + parentX);
                    graphicInfo.setHeight(lowerRightNode.get(EDITOR_BOUNDS_Y).asDouble() - graphicInfo.getY() + parentY);

                    String childShapeId = jsonChildNode.get(EDITOR_SHAPE_ID).asText();
                    bpmnModel.addGraphicInfo(BpmnJsonConverterUtil.getElementId(jsonChildNode), graphicInfo);

                    shapeMap.put(childShapeId, jsonChildNode);

                    ArrayNode outgoingNode = (ArrayNode) jsonChildNode.get("outgoing");
                    if (outgoingNode != null && outgoingNode.size() > 0) {
                        for (JsonNode outgoingChildNode : outgoingNode) {
                            JsonNode resourceNode = outgoingChildNode.get(EDITOR_SHAPE_ID);
                            if (resourceNode != null) {
                                sourceRefMap.put(resourceNode.asText(), jsonChildNode);
                            }
                        }
                    }

                    //The graphic info of the collapsed subprocess is relative to its parent.
                    //But the children of the collapsed subprocess are relative to the canvas upper corner. (always 0,0)
                    if (STENCIL_COLLAPSED_SUB_PROCESS.equals(stencilId)) {
                        readShapeDI(jsonChildNode, 0,0, shapeMap, sourceRefMap, bpmnModel);
                    } else {
                        readShapeDI(jsonChildNode, graphicInfo.getX(), graphicInfo.getY(), shapeMap, sourceRefMap, bpmnModel);
                    }
                }
            }
        }
    }

    private void filterAllEdges(JsonNode objectNode, Map<String, JsonNode> edgeMap, Map<String, List<JsonNode>> sourceAndTargetMap, Map<String, JsonNode> shapeMap, Map<String, JsonNode> sourceRefMap) {

        if (objectNode.get(EDITOR_CHILD_SHAPES) != null) {
            for (JsonNode jsonChildNode : objectNode.get(EDITOR_CHILD_SHAPES)) {

                ObjectNode childNode = (ObjectNode) jsonChildNode;
                String stencilId = BpmnJsonConverterUtil.getStencilId(childNode);
                if (STENCIL_SUB_PROCESS.equals(stencilId) || STENCIL_POOL.equals(stencilId) || STENCIL_LANE.equals(stencilId) ||
                        STENCIL_COLLAPSED_SUB_PROCESS.equals(stencilId) || STENCIL_EVENT_SUB_PROCESS.equals(stencilId)) {

                    filterAllEdges(childNode, edgeMap, sourceAndTargetMap, shapeMap, sourceRefMap);

                } else if (STENCIL_SEQUENCE_FLOW.equals(stencilId) || STENCIL_ASSOCIATION.equals(stencilId)) {

                    String childEdgeId = BpmnJsonConverterUtil.getElementId(childNode);
                    JsonNode targetNode = childNode.get("target");
                    if (targetNode != null && !targetNode.isNull()) {
                        String targetRefId = targetNode.get(EDITOR_SHAPE_ID).asText();
                        List<JsonNode> sourceAndTargetList = new ArrayList<>();
                        sourceAndTargetList.add(sourceRefMap.get(childNode.get(EDITOR_SHAPE_ID).asText()));
                        sourceAndTargetList.add(shapeMap.get(targetRefId));
                        sourceAndTargetMap.put(childEdgeId, sourceAndTargetList);
                    }
                    edgeMap.put(childEdgeId, childNode);
                }
            }
        }
    }

    private void readEdgeDI(Map<String, JsonNode> edgeMap, Map<String, List<JsonNode>> sourceAndTargetMap, BpmnModel bpmnModel) {

        for (String edgeId : edgeMap.keySet()) {

            JsonNode edgeNode = edgeMap.get(edgeId);
            List<JsonNode> sourceAndTargetList = sourceAndTargetMap.get(edgeId);

            JsonNode sourceRefNode = null;
            JsonNode targetRefNode = null;

            if (sourceAndTargetList != null && sourceAndTargetList.size() > 1) {
                sourceRefNode = sourceAndTargetList.get(0);
                targetRefNode = sourceAndTargetList.get(1);
            }

            if (sourceRefNode == null) {
                LOGGER.info("Skipping edge {} because source ref is null", edgeId);
                continue;
            }

            if (targetRefNode == null) {
                LOGGER.info("Skipping edge {} because target ref is null", edgeId);
                continue;
            }

            JsonNode dockersNode = edgeNode.get(EDITOR_DOCKERS);
            double sourceDockersX = dockersNode.get(0).get(EDITOR_BOUNDS_X).asDouble();
            double sourceDockersY = dockersNode.get(0).get(EDITOR_BOUNDS_Y).asDouble();

            GraphicInfo sourceInfo = bpmnModel.getGraphicInfo(BpmnJsonConverterUtil.getElementId(sourceRefNode));
            GraphicInfo targetInfo = bpmnModel.getGraphicInfo(BpmnJsonConverterUtil.getElementId(targetRefNode));

            double sourceRefLineX = sourceInfo.getX() + sourceDockersX;
            double sourceRefLineY = sourceInfo.getY() + sourceDockersY;

            double nextPointInLineX;
            double nextPointInLineY;

            nextPointInLineX = dockersNode.get(1).get(EDITOR_BOUNDS_X).asDouble();
            nextPointInLineY = dockersNode.get(1).get(EDITOR_BOUNDS_Y).asDouble();
            if (dockersNode.size() == 2) {
                nextPointInLineX += targetInfo.getX();
                nextPointInLineY += targetInfo.getY();
            }

            java.awt.geom.Line2D firstLine = new java.awt.geom.Line2D.Double(sourceRefLineX, sourceRefLineY, nextPointInLineX, nextPointInLineY);

            String sourceRefStencilId = BpmnJsonConverterUtil.getStencilId(sourceRefNode);
            String targetRefStencilId = BpmnJsonConverterUtil.getStencilId(targetRefNode);

            List<GraphicInfo> graphicInfoList = new ArrayList<>();

            Area source2D = null;
            if (DI_CIRCLES.contains(sourceRefStencilId)) {
                source2D = createEllipse(sourceInfo, sourceDockersX, sourceDockersY);

            } else if (DI_RECTANGLES.contains(sourceRefStencilId)) {
                source2D = createRectangle(sourceInfo);

            } else if (DI_GATEWAY.contains(sourceRefStencilId)) {
                source2D = createGateway(sourceInfo);
            }

            if (source2D != null) {
                Collection<java.awt.geom.Point2D> intersections = getIntersections(firstLine, source2D);
                if (intersections != null && intersections.size() > 0) {
                    java.awt.geom.Point2D intersection = intersections.iterator().next();
                    graphicInfoList.add(createGraphicInfo(intersection.getX(), intersection.getY()));
                } else {
                    graphicInfoList.add(createGraphicInfo(sourceRefLineX, sourceRefLineY));
                }
            }

            java.awt.geom.Line2D lastLine = null;

            if (dockersNode.size() > 2) {
                for (int i = 1; i < dockersNode.size() - 1; i++) {
                    double x = dockersNode.get(i).get(EDITOR_BOUNDS_X).asDouble();
                    double y = dockersNode.get(i).get(EDITOR_BOUNDS_Y).asDouble();
                    graphicInfoList.add(createGraphicInfo(x, y));
                }

                double startLastLineX = dockersNode.get(dockersNode.size() - 2).get(EDITOR_BOUNDS_X).asDouble();
                double startLastLineY = dockersNode.get(dockersNode.size() - 2).get(EDITOR_BOUNDS_Y).asDouble();

                double endLastLineX = dockersNode.get(dockersNode.size() - 1).get(EDITOR_BOUNDS_X).asDouble();
                double endLastLineY = dockersNode.get(dockersNode.size() - 1).get(EDITOR_BOUNDS_Y).asDouble();

                endLastLineX += targetInfo.getX();
                endLastLineY += targetInfo.getY();

                lastLine = new java.awt.geom.Line2D.Double(startLastLineX, startLastLineY, endLastLineX, endLastLineY);

            } else {
                lastLine = firstLine;
            }

            Area target2D = null;
            if (DI_RECTANGLES.contains(targetRefStencilId)) {
                target2D = createRectangle(targetInfo);

            } else if (DI_CIRCLES.contains(targetRefStencilId)) {

                double targetDockersX = dockersNode.get(dockersNode.size() - 1).get(EDITOR_BOUNDS_X).asDouble();
                double targetDockersY = dockersNode.get(dockersNode.size() - 1).get(EDITOR_BOUNDS_Y).asDouble();

                target2D = createEllipse(targetInfo, targetDockersX, targetDockersY);

            } else if (DI_GATEWAY.contains(targetRefStencilId)) {
                target2D = createGateway(targetInfo);
            }

            if (target2D != null) {
                Collection<java.awt.geom.Point2D> intersections = getIntersections(lastLine, target2D);
                if (intersections != null && intersections.size() > 0) {
                    java.awt.geom.Point2D intersection = intersections.iterator().next();
                    graphicInfoList.add(createGraphicInfo(intersection.getX(), intersection.getY()));
                } else {
                    graphicInfoList.add(createGraphicInfo(lastLine.getX2(), lastLine.getY2()));
                }
            }

            bpmnModel.addFlowGraphicInfoList(edgeId, graphicInfoList);
        }
    }

    protected Area createEllipse(GraphicInfo sourceInfo, double halfWidth, double halfHeight) {
        Area outerCircle = new Area(new Ellipse2D.Double(
                sourceInfo.getX(), sourceInfo.getY(), 2 * halfWidth, 2 * halfHeight
        ));
        Area innerCircle = new Area(new Ellipse2D.Double(
                sourceInfo.getX() + lineWidth, sourceInfo.getY() + lineWidth, 2 * (halfWidth - lineWidth), 2 * (halfHeight - lineWidth)
        ));
        outerCircle.subtract(innerCircle);
        return outerCircle;
    }

    protected Collection<java.awt.geom.Point2D> getIntersections(java.awt.geom.Line2D line, Area shape) {
        Area intersectionArea = new Area(getLineShape(line));
        intersectionArea.intersect(shape);
        if (!intersectionArea.isEmpty()) {
            Rectangle2D bounds2D = intersectionArea.getBounds2D();
            HashSet<java.awt.geom.Point2D> intersections = new HashSet<>(1);
            intersections.add(new java.awt.geom.Point2D.Double(bounds2D.getX(), bounds2D.getY()));
            return intersections;
        }
        return Collections.EMPTY_SET;
    }

    protected Shape getLineShape(java.awt.geom.Line2D line2D) {
        Path2D line = new Path2D.Double(Path2D.WIND_NON_ZERO, 4);
        line.moveTo(line2D.getX1(), line2D.getY1());
        line.lineTo(line2D.getX2(), line2D.getY2());
        line.lineTo(line2D.getX2() + lineWidth, line2D.getY2() + lineWidth);
        line.closePath();
        return line;
    }

    protected Area createRectangle(GraphicInfo graphicInfo) {
        Area outerRectangle = new Area(new Rectangle2D.Double(
                graphicInfo.getX(), graphicInfo.getY(),
                graphicInfo.getWidth(), graphicInfo.getHeight()
        ));
        Area innerRectangle = new Area(new Rectangle2D.Double(
                graphicInfo.getX() + lineWidth, graphicInfo.getY() + lineWidth,
                graphicInfo.getWidth() - 2*lineWidth, graphicInfo.getHeight() - 2*lineWidth
        ));
        outerRectangle.subtract(innerRectangle);
        return outerRectangle;
    }

    protected Area createGateway(GraphicInfo graphicInfo) {
        Area outerGatewayArea = new Area(
                        createGatewayShape(graphicInfo.getX(), graphicInfo.getY(), graphicInfo.getWidth(), graphicInfo.getHeight())
                );
        Area innerGatewayArea = new Area(
                        createGatewayShape(graphicInfo.getX()+lineWidth, graphicInfo.getY()+lineWidth,
                                graphicInfo.getWidth()-2*lineWidth, graphicInfo.getHeight()-2*lineWidth)
                );
        outerGatewayArea.subtract(innerGatewayArea);
        return outerGatewayArea;
    }

    private Path2D.Double createGatewayShape(double x, double y, double width, double height) {
        double middleX = x + (width / 2);
        double middleY = y + (height / 2);

        Path2D.Double gatewayShape = new Path2D.Double(Path2D.WIND_NON_ZERO, 4);
        gatewayShape.moveTo(x, middleY);
        gatewayShape.lineTo(middleX, y);
        gatewayShape.lineTo(x + width, middleY);
        gatewayShape.lineTo(middleX, y + height);
        gatewayShape.closePath();
        return gatewayShape;
    }

    private GraphicInfo createGraphicInfo(double x, double y) {
        GraphicInfo graphicInfo = new GraphicInfo();
        graphicInfo.setX(x);
        graphicInfo.setY(y);
        return graphicInfo;
    }

    class FlowWithContainer {
        protected SequenceFlow sequenceFlow;
        protected FlowElementsContainer flowContainer;

        public FlowWithContainer(SequenceFlow sequenceFlow, FlowElementsContainer flowContainer) {
            this.sequenceFlow = sequenceFlow;
            this.flowContainer = flowContainer;
        }

        public SequenceFlow getSequenceFlow() {
            return sequenceFlow;
        }

        public void setSequenceFlow(SequenceFlow sequenceFlow) {
            this.sequenceFlow = sequenceFlow;
        }

        public FlowElementsContainer getFlowContainer() {
            return flowContainer;
        }

        public void setFlowContainer(FlowElementsContainer flowContainer) {
            this.flowContainer = flowContainer;
        }
    }
}
