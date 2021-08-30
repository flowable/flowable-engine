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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.CancelEventDefinition;
import org.flowable.bpmn.model.CompensateEventDefinition;
import org.flowable.bpmn.model.ConditionalEventDefinition;
import org.flowable.bpmn.model.ErrorEventDefinition;
import org.flowable.bpmn.model.EscalationEventDefinition;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.GraphicInfo;
import org.flowable.bpmn.model.MessageEventDefinition;
import org.flowable.bpmn.model.SignalEventDefinition;
import org.flowable.bpmn.model.VariableListenerEventDefinition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tijs Rademakers
 */
public class BoundaryEventJsonConverter extends BaseBpmnJsonConverter {

    public static void fillTypes(Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap,
            Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {

        fillJsonTypes(convertersToBpmnMap);
        fillBpmnTypes(convertersToJsonMap);
    }

    public static void fillJsonTypes(Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap) {
        convertersToBpmnMap.put(STENCIL_EVENT_BOUNDARY_TIMER, BoundaryEventJsonConverter.class);
        convertersToBpmnMap.put(STENCIL_EVENT_BOUNDARY_CONDITIONAL, BoundaryEventJsonConverter.class);
        convertersToBpmnMap.put(STENCIL_EVENT_BOUNDARY_ERROR, BoundaryEventJsonConverter.class);
        convertersToBpmnMap.put(STENCIL_EVENT_BOUNDARY_ESCALATION, BoundaryEventJsonConverter.class);
        convertersToBpmnMap.put(STENCIL_EVENT_BOUNDARY_SIGNAL, BoundaryEventJsonConverter.class);
        convertersToBpmnMap.put(STENCIL_EVENT_BOUNDARY_MESSAGE, BoundaryEventJsonConverter.class);
        convertersToBpmnMap.put(STENCIL_EVENT_BOUNDARY_EVENT_REGISTRY, BoundaryEventJsonConverter.class);
        convertersToBpmnMap.put(STENCIL_EVENT_BOUNDARY_VARIABLE_LISTENER, BoundaryEventJsonConverter.class);
        convertersToBpmnMap.put(STENCIL_EVENT_BOUNDARY_CANCEL, BoundaryEventJsonConverter.class);
        convertersToBpmnMap.put(STENCIL_EVENT_BOUNDARY_COMPENSATION, BoundaryEventJsonConverter.class);
    }

    public static void fillBpmnTypes(Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {
        convertersToJsonMap.put(BoundaryEvent.class, BoundaryEventJsonConverter.class);
    }

    @Override
    protected String getStencilId(BaseElement baseElement) {
        BoundaryEvent boundaryEvent = (BoundaryEvent) baseElement;
        List<EventDefinition> eventDefinitions = boundaryEvent.getEventDefinitions();

        if (eventDefinitions.isEmpty()) {
            if (boundaryEvent.getExtensionElements().get("eventType") != null && boundaryEvent.getExtensionElements().get("eventType").size() > 0) {
                String eventType = boundaryEvent.getExtensionElements().get("eventType").get(0).getElementText();
                if (StringUtils.isNotEmpty(eventType)) {
                    return STENCIL_EVENT_BOUNDARY_EVENT_REGISTRY;
                }
            }
        }

        if (eventDefinitions.size() != 1) {
            // return timer event as default;
            return STENCIL_EVENT_BOUNDARY_TIMER;
        }

        EventDefinition eventDefinition = eventDefinitions.get(0);
        if (eventDefinition instanceof ConditionalEventDefinition) {
            return STENCIL_EVENT_BOUNDARY_CONDITIONAL;
        } else if (eventDefinition instanceof ErrorEventDefinition) {
            return STENCIL_EVENT_BOUNDARY_ERROR;
        } else if (eventDefinition instanceof EscalationEventDefinition) {
            return STENCIL_EVENT_BOUNDARY_ESCALATION;
        } else if (eventDefinition instanceof SignalEventDefinition) {
            return STENCIL_EVENT_BOUNDARY_SIGNAL;
        } else if (eventDefinition instanceof MessageEventDefinition) {
            return STENCIL_EVENT_BOUNDARY_MESSAGE;
        } else if (eventDefinition instanceof CancelEventDefinition) {
            return STENCIL_EVENT_BOUNDARY_CANCEL;
        } else if (eventDefinition instanceof CompensateEventDefinition) {
            return STENCIL_EVENT_BOUNDARY_COMPENSATION;
        } else if (eventDefinition instanceof VariableListenerEventDefinition) {
            return STENCIL_EVENT_BOUNDARY_VARIABLE_LISTENER;
        } else {
            return STENCIL_EVENT_BOUNDARY_TIMER;
        }
    }

    @Override
    protected void convertElementToJson(ObjectNode propertiesNode, BaseElement baseElement, BpmnJsonConverterContext converterContext) {
        BoundaryEvent boundaryEvent = (BoundaryEvent) baseElement;
        ArrayNode dockersArrayNode = objectMapper.createArrayNode();
        ObjectNode dockNode = objectMapper.createObjectNode();
        GraphicInfo graphicInfo = model.getGraphicInfo(boundaryEvent.getId());
        GraphicInfo parentGraphicInfo = model.getGraphicInfo(boundaryEvent.getAttachedToRef().getId());
        BigDecimal parentX = new BigDecimal(parentGraphicInfo.getX());
        BigDecimal parentY = new BigDecimal(parentGraphicInfo.getY());

        BigDecimal boundaryX = new BigDecimal(graphicInfo.getX());
        BigDecimal boundaryWidth = new BigDecimal(graphicInfo.getWidth());
        BigDecimal boundaryXMid = boundaryWidth.divide(new BigDecimal(2));

        BigDecimal boundaryY = new BigDecimal(graphicInfo.getY());
        BigDecimal boundaryHeight = new BigDecimal(graphicInfo.getHeight());
        BigDecimal boundaryYMid = boundaryHeight.divide(new BigDecimal(2));

        BigDecimal xBound = boundaryX.add(boundaryXMid).subtract(parentX).setScale(0, RoundingMode.HALF_UP);
        BigDecimal yBound = boundaryY.add(boundaryYMid).subtract(parentY).setScale(0,RoundingMode.HALF_UP);

        dockNode.put(EDITOR_BOUNDS_X, xBound.intValue());
        dockNode.put(EDITOR_BOUNDS_Y, yBound.intValue());
        dockersArrayNode.add(dockNode);

        flowElementNode.set("dockers", dockersArrayNode);

        propertiesNode.put(PROPERTY_CANCEL_ACTIVITY, boundaryEvent.isCancelActivity());

        addEventProperties(boundaryEvent, propertiesNode);
        addEventRegistryProperties(boundaryEvent, propertiesNode);
    }

    @Override
    protected FlowElement convertJsonToElement(JsonNode elementNode, JsonNode modelNode, Map<String, JsonNode> shapeMap,
        BpmnJsonConverterContext converterContext) {
        BoundaryEvent boundaryEvent = new BoundaryEvent();
        String stencilId = BpmnJsonConverterUtil.getStencilId(elementNode);
        if (STENCIL_EVENT_BOUNDARY_TIMER.equals(stencilId)) {
            convertJsonToTimerDefinition(elementNode, boundaryEvent);

        } else if (STENCIL_EVENT_BOUNDARY_CONDITIONAL.equals(stencilId)) {
            convertJsonToConditionalDefinition(elementNode, boundaryEvent);

        } else if (STENCIL_EVENT_BOUNDARY_ERROR.equals(stencilId)) {
            convertJsonToErrorDefinition(elementNode, boundaryEvent);

        } else if (STENCIL_EVENT_BOUNDARY_ESCALATION.equals(stencilId)) {
            convertJsonToEscalationDefinition(elementNode, boundaryEvent);

        } else if (STENCIL_EVENT_BOUNDARY_SIGNAL.equals(stencilId)) {
            convertJsonToSignalDefinition(elementNode, boundaryEvent);

        } else if (STENCIL_EVENT_BOUNDARY_MESSAGE.equals(stencilId)) {
            convertJsonToMessageDefinition(elementNode, boundaryEvent);

        } else if (STENCIL_EVENT_BOUNDARY_CANCEL.equals(stencilId)) {
            CancelEventDefinition cancelEventDefinition = new CancelEventDefinition();
            boundaryEvent.getEventDefinitions().add(cancelEventDefinition);
            boundaryEvent.setCancelActivity(false);

        } else if (STENCIL_EVENT_BOUNDARY_COMPENSATION.equals(stencilId)) {
            CompensateEventDefinition compensateEventDefinition = new CompensateEventDefinition();
            boundaryEvent.getEventDefinitions().add(compensateEventDefinition);
            boundaryEvent.setCancelActivity(false);

        } else if (STENCIL_EVENT_BOUNDARY_EVENT_REGISTRY.equals(stencilId)) {
            addReceiveEventExtensionElements(elementNode, boundaryEvent);
        
        } else if (STENCIL_EVENT_BOUNDARY_VARIABLE_LISTENER.equals(stencilId)) {
            convertJsonToVariableListenerDefinition(elementNode, boundaryEvent);
        }
        
        if (!STENCIL_EVENT_BOUNDARY_ERROR.equals(stencilId) && !(STENCIL_EVENT_BOUNDARY_CANCEL.equals(stencilId)) &&
                !STENCIL_EVENT_BOUNDARY_COMPENSATION.equals(stencilId) && !STENCIL_EVENT_BOUNDARY_CONDITIONAL.equals(stencilId)) {
            
            boundaryEvent.setCancelActivity(getPropertyValueAsBoolean(PROPERTY_CANCEL_ACTIVITY, elementNode));
        }
        boundaryEvent.setAttachedToRefId(lookForAttachedRef(elementNode.get(EDITOR_SHAPE_ID).asText(), modelNode.get(EDITOR_CHILD_SHAPES)));
        return boundaryEvent;
    }

    protected String lookForAttachedRef(String boundaryEventId, JsonNode childShapesNode) {
        String attachedRefId = null;

        if (childShapesNode != null) {

            for (JsonNode childNode : childShapesNode) {
                ArrayNode outgoingNode = (ArrayNode) childNode.get("outgoing");
                if (outgoingNode != null && outgoingNode.size() > 0) {
                    for (JsonNode outgoingChildNode : outgoingNode) {
                        JsonNode resourceNode = outgoingChildNode.get(EDITOR_SHAPE_ID);
                        if (resourceNode != null && boundaryEventId.equals(resourceNode.asText())) {
                            attachedRefId = BpmnJsonConverterUtil.getElementId(childNode);
                            break;
                        }
                    }

                    if (attachedRefId != null) {
                        break;
                    }
                }

                attachedRefId = lookForAttachedRef(boundaryEventId, childNode.get(EDITOR_CHILD_SHAPES));

                if (attachedRefId != null) {
                    break;
                }
            }
        }

        return attachedRefId;
    }

    @Override
    protected void setPropertyValue(String name, String value, ObjectNode propertiesNode) {
        if (StringUtils.isNotEmpty(value)) {
            propertiesNode.put(name, value);
        }
    }
}
