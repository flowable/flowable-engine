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

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.SendEventServiceTask;
import org.flowable.bpmn.model.ServiceTask;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tijs Rademakers
 */
public class SendEventTaskJsonConverter extends BaseBpmnJsonConverter {

    public static void fillTypes(Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap, Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {

        fillJsonTypes(convertersToBpmnMap);
        fillBpmnTypes(convertersToJsonMap);
    }

    public static void fillJsonTypes(Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap) {
        convertersToBpmnMap.put(STENCIL_TASK_SEND_EVENT, SendEventTaskJsonConverter.class);
    }

    public static void fillBpmnTypes(Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {
        convertersToJsonMap.put(SendEventServiceTask.class, SendEventTaskJsonConverter.class);
    }

    @Override
    protected String getStencilId(BaseElement baseElement) {
        return STENCIL_TASK_SEND_EVENT;
    }

    @Override
    protected void convertElementToJson(ObjectNode propertiesNode, BaseElement baseElement,
        BpmnJsonConverterContext converterContext) {
        SendEventServiceTask sendEventServiceTask = (SendEventServiceTask) baseElement;
        
        String eventType = sendEventServiceTask.getEventType();
        if (StringUtils.isNotEmpty(eventType)) {
            setPropertyValue(PROPERTY_EVENT_REGISTRY_EVENT_KEY, sendEventServiceTask.getEventType(), propertiesNode);
            setPropertyValue(PROPERTY_EVENT_REGISTRY_EVENT_NAME, getExtensionValue("eventName", sendEventServiceTask), propertiesNode);
            addEventInIOParameters(sendEventServiceTask.getEventInParameters(), propertiesNode);
            
            setPropertyValue(PROPERTY_EVENT_REGISTRY_CHANNEL_KEY, getExtensionValue("channelKey", sendEventServiceTask), propertiesNode);
            setPropertyValue(PROPERTY_EVENT_REGISTRY_CHANNEL_NAME, getExtensionValue("channelName", sendEventServiceTask), propertiesNode);
            setPropertyValue(PROPERTY_EVENT_REGISTRY_CHANNEL_TYPE, getExtensionValue("channelType", sendEventServiceTask), propertiesNode);
            setPropertyValue(PROPERTY_EVENT_REGISTRY_CHANNEL_DESTINATION, getExtensionValue("channelDestination", sendEventServiceTask), propertiesNode);
            
            if (sendEventServiceTask.isTriggerable()) {
                propertiesNode.put(PROPERTY_SERVICETASK_TRIGGERABLE, sendEventServiceTask.isTriggerable());
            }
            
            setPropertyValue(PROPERTY_EVENT_REGISTRY_TRIGGER_EVENT_KEY, sendEventServiceTask.getTriggerEventType(), propertiesNode);
            setPropertyValue(PROPERTY_EVENT_REGISTRY_TRIGGER_EVENT_NAME, getExtensionValue("triggerEventName", sendEventServiceTask), propertiesNode);
            addEventOutIOParameters(sendEventServiceTask.getEventOutParameters(), propertiesNode);
            
            setPropertyValue(PROPERTY_EVENT_REGISTRY_TRIGGER_CHANNEL_KEY, getExtensionValue("triggerChannelKey", sendEventServiceTask), propertiesNode);
            setPropertyValue(PROPERTY_EVENT_REGISTRY_TRIGGER_CHANNEL_NAME, getExtensionValue("triggerChannelName", sendEventServiceTask), propertiesNode);
            setPropertyValue(PROPERTY_EVENT_REGISTRY_TRIGGER_CHANNEL_TYPE, getExtensionValue("triggerChannelType", sendEventServiceTask), propertiesNode);
            setPropertyValue(PROPERTY_EVENT_REGISTRY_TRIGGER_CHANNEL_DESTINATION, getExtensionValue("triggerChannelDestination", sendEventServiceTask), propertiesNode);
            
            addEventCorrelationParameters(sendEventServiceTask.getExtensionElements().get("triggerEventCorrelationParameter"), propertiesNode);
            
            String keyDetectionType = getExtensionValue("keyDetectionType", sendEventServiceTask);
            String keyDetectionValue = getExtensionValue("keyDetectionValue", sendEventServiceTask);
            if (StringUtils.isNotEmpty(keyDetectionType) && StringUtils.isNotEmpty(keyDetectionValue)) {
                if ("fixedValue".equalsIgnoreCase(keyDetectionType)) {
                    setPropertyValue(PROPERTY_EVENT_REGISTRY_KEY_DETECTION_FIXED_VALUE, keyDetectionValue, propertiesNode);
                    
                } else if ("jsonField".equalsIgnoreCase(keyDetectionType)) {
                    setPropertyValue(PROPERTY_EVENT_REGISTRY_KEY_DETECTION_JSON_FIELD, keyDetectionValue, propertiesNode);
                    
                } else if ("jsonPointer".equalsIgnoreCase(keyDetectionType)) {
                    setPropertyValue(PROPERTY_EVENT_REGISTRY_KEY_DETECTION_JSON_POINTER, keyDetectionValue, propertiesNode);
                }
            }
        }   
    }

    @Override
    protected FlowElement convertJsonToElement(JsonNode elementNode, JsonNode modelNode, Map<String, JsonNode> shapeMap,
        BpmnJsonConverterContext converterContext) {
        SendEventServiceTask task = new SendEventServiceTask();
        task.setType(ServiceTask.SEND_EVENT_TASK);
        
        String eventKey = getPropertyValueAsString(PROPERTY_EVENT_REGISTRY_EVENT_KEY, elementNode);
        if (StringUtils.isNotEmpty(eventKey)) {
            task.setEventType(eventKey);
            addFlowableExtensionElementWithValue("eventName", getPropertyValueAsString(PROPERTY_EVENT_REGISTRY_EVENT_NAME, elementNode), task);
            convertJsonToInIOParameters(elementNode, task);
            
            addFlowableExtensionElementWithValue("channelKey", getPropertyValueAsString(PROPERTY_EVENT_REGISTRY_CHANNEL_KEY, elementNode), task);
            addFlowableExtensionElementWithValue("channelName", getPropertyValueAsString(PROPERTY_EVENT_REGISTRY_CHANNEL_NAME, elementNode), task);
            addFlowableExtensionElementWithValue("channelType", getPropertyValueAsString(PROPERTY_EVENT_REGISTRY_CHANNEL_TYPE, elementNode), task);
            addFlowableExtensionElementWithValue("channelDestination", getPropertyValueAsString(PROPERTY_EVENT_REGISTRY_CHANNEL_DESTINATION, elementNode), task);
            
            String triggerEventKey = getPropertyValueAsString(PROPERTY_EVENT_REGISTRY_TRIGGER_EVENT_KEY, elementNode);
            if (StringUtils.isNotEmpty(triggerEventKey)) {
                task.setTriggerEventType(triggerEventKey);
                
                if (getPropertyValueAsBoolean(PROPERTY_SERVICETASK_TRIGGERABLE, elementNode)) {
                    task.setTriggerable(true);
                }
                
                addFlowableExtensionElementWithValue("triggerEventName", getPropertyValueAsString(PROPERTY_EVENT_REGISTRY_TRIGGER_EVENT_NAME, elementNode), task);
                convertJsonToOutIOParameters(elementNode, task);
                
                addFlowableExtensionElementWithValue("triggerChannelKey", getPropertyValueAsString(PROPERTY_EVENT_REGISTRY_TRIGGER_CHANNEL_KEY, elementNode), task);
                addFlowableExtensionElementWithValue("triggerChannelName", getPropertyValueAsString(PROPERTY_EVENT_REGISTRY_TRIGGER_CHANNEL_NAME, elementNode), task);
                addFlowableExtensionElementWithValue("triggerChannelType", getPropertyValueAsString(PROPERTY_EVENT_REGISTRY_TRIGGER_CHANNEL_TYPE, elementNode), task);
                addFlowableExtensionElementWithValue("triggerChannelDestination", getPropertyValueAsString(PROPERTY_EVENT_REGISTRY_TRIGGER_CHANNEL_DESTINATION, elementNode), task);
                
                convertJsonToEventCorrelationParameters(elementNode, "triggerEventCorrelationParameter", task);
                
                String fixedValue = getPropertyValueAsString(PROPERTY_EVENT_REGISTRY_KEY_DETECTION_FIXED_VALUE, elementNode);
                String jsonField = getPropertyValueAsString(PROPERTY_EVENT_REGISTRY_KEY_DETECTION_JSON_FIELD, elementNode);
                String jsonPointer = getPropertyValueAsString(PROPERTY_EVENT_REGISTRY_KEY_DETECTION_JSON_POINTER, elementNode);
                if (StringUtils.isNotEmpty(fixedValue)) {
                    addFlowableExtensionElementWithValue("keyDetectionType", "fixedValue", task);
                    addFlowableExtensionElementWithValue("keyDetectionValue", fixedValue, task);
                    
                } else if (StringUtils.isNotEmpty(jsonField)) {
                    addFlowableExtensionElementWithValue("keyDetectionType", "jsonField", task);
                    addFlowableExtensionElementWithValue("keyDetectionValue", jsonField, task);
                    
                } else if (StringUtils.isNotEmpty(jsonPointer)) {
                    addFlowableExtensionElementWithValue("keyDetectionType", "jsonPointer", task);
                    addFlowableExtensionElementWithValue("keyDetectionValue", jsonPointer, task);
                }
            }
        }
        
        return task;
    }
    
    @Override
    protected String getExtensionValue(String name, FlowElement flowElement) {
        List<ExtensionElement> extensionElements = flowElement.getExtensionElements().get(name);
        if (extensionElements != null && extensionElements.size() > 0) {
            return extensionElements.get(0).getElementText();
        }
        
        return null;
    }
    
    @Override
    protected void setPropertyValue(String name, String value, ObjectNode propertiesNode) {
        if (StringUtils.isNotEmpty(value)) {
            propertiesNode.put(name, value);
        }
    }
}
