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
package org.flowable.ui.modeler.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.IOParameter;
import org.flowable.bpmn.model.SendEventServiceTask;
import org.flowable.eventregistry.model.ChannelEventKeyDetection;
import org.flowable.eventregistry.model.ChannelModel;
import org.flowable.eventregistry.model.EventCorrelationParameter;
import org.flowable.eventregistry.model.EventModel;
import org.flowable.eventregistry.model.EventPayload;
import org.flowable.eventregistry.model.InboundChannelModel;
import org.flowable.eventregistry.model.JmsInboundChannelModel;
import org.flowable.eventregistry.model.JmsOutboundChannelModel;
import org.flowable.eventregistry.model.KafkaInboundChannelModel;
import org.flowable.eventregistry.model.KafkaOutboundChannelModel;
import org.flowable.eventregistry.model.OutboundChannelModel;
import org.flowable.eventregistry.model.RabbitInboundChannelModel;
import org.flowable.eventregistry.model.RabbitOutboundChannelModel;

public class BpmnEventModelUtil {
    
    public static void fillChannelModelMap(List<FlowElement> flowElements, Map<String, ChannelModel> channelModelMap) {
        for (FlowElement elementObject : flowElements) {
            String channelKey = getElementValue("channelKey", elementObject);
            String channelType = getElementValue("channelType", elementObject);
            String channelDestination = getElementValue("channelDestination", elementObject);
            if (StringUtils.isNotEmpty(channelKey) && !channelModelMap.containsKey(channelKey) && StringUtils.isNotEmpty(channelType) &&
                            StringUtils.isNotEmpty(channelDestination)) {
                
                if (elementObject instanceof SendEventServiceTask) {
                    createOutboundChannelInMap(channelKey, channelType, channelDestination, elementObject, channelModelMap);
                    SendEventServiceTask task = (SendEventServiceTask) elementObject;
                    
                    String triggerChannelKey = getElementValue("triggerChannelKey", elementObject);
                    String triggerChannelType = getElementValue("triggerChannelType", elementObject);
                    String triggerChannelDestination = getElementValue("triggerChannelDestination", elementObject);
                    if (task.isTriggerable() && StringUtils.isNotEmpty(triggerChannelKey) && !channelModelMap.containsKey(triggerChannelKey) && 
                                    StringUtils.isNotEmpty(triggerChannelType) && StringUtils.isNotEmpty(triggerChannelDestination)) {
                        
                        createInboundChannelInMap(triggerChannelKey, triggerChannelType, triggerChannelDestination, 
                                        "triggerChannelName", elementObject, channelModelMap);
                    }
                    
                } else {
                    createInboundChannelInMap(channelKey, channelType, channelDestination, "channelName", elementObject, channelModelMap);
                }
            }
        }
    }

    public static void fillEventModelMap(List<FlowElement> flowElements, Map<String, EventModel> eventModelMap) {
        for (FlowElement flowElement : flowElements) {
            String eventKey = null;
            if (flowElement instanceof SendEventServiceTask) {
                SendEventServiceTask task = (SendEventServiceTask) flowElement;
                eventKey = task.getEventType();
            } else {
                eventKey = getElementValue("eventType", flowElement);
            }
            
            if (StringUtils.isNotEmpty(eventKey) && !eventModelMap.containsKey(eventKey)) {
                EventModel eventModel = new EventModel();
                String eventName = getElementValue("eventName", flowElement);
                eventModel.setKey(eventKey);
                if (StringUtils.isNotEmpty(eventName)) {
                    eventModel.setName(eventName);
                } else {
                    eventModel.setName(eventKey);
                }
                
                if (flowElement instanceof SendEventServiceTask) {
                    SendEventServiceTask task = (SendEventServiceTask) flowElement;
                    eventModel.setPayload(getInIOParameterEventPayload(task.getEventInParameters()));
                    String channelKey = getElementValue("channelKey", flowElement);
                    if (StringUtils.isNotEmpty(channelKey)) {
                        eventModel.addOutboundChannelKey(channelKey);
                    }
                    
                    if (task.isTriggerable() && StringUtils.isNotEmpty(task.getTriggerEventType())) {
                        EventModel triggerEventModel = new EventModel();
                        String triggerEventName = getElementValue("triggerEventName", flowElement);
                        triggerEventModel.setKey(task.getTriggerEventType());
                        if (StringUtils.isNotEmpty(triggerEventName)) {
                            triggerEventModel.setName(triggerEventName);
                        } else {
                            triggerEventModel.setName(task.getTriggerEventType());
                        }
                        
                        triggerEventModel.setPayload(getOutIOParameterEventPayload(task.getEventOutParameters()));
                        triggerEventModel.setCorrelationParameters(getEventCorrelationParameters(flowElement.getExtensionElements().get("triggerEventCorrelationParameter")));
                        
                        String triggerChannelKey = getElementValue("triggerChannelKey", flowElement);
                        if (StringUtils.isNotEmpty(triggerChannelKey)) {
                            triggerEventModel.addInboundChannelKey(triggerChannelKey);
                        }
                        
                        eventModelMap.put(task.getTriggerEventType(), triggerEventModel);
                    }
                    
                } else {
                    eventModel.setPayload(getOutParameterEventPayload(flowElement.getExtensionElements().get("eventOutParameter")));
                    eventModel.setCorrelationParameters(getEventCorrelationParameters(flowElement.getExtensionElements().get("eventCorrelationParameter")));
                    
                    String channelKey = getElementValue("channelKey", flowElement);
                    if (StringUtils.isNotEmpty(channelKey)) {
                        eventModel.addInboundChannelKey(channelKey);
                    }
                }
                
                eventModelMap.put(eventKey, eventModel);
            }
        }
    }
    
    protected static void createOutboundChannelInMap(String channelKey, String channelType, String channelDestination, 
                    FlowElement elementObject, Map<String, ChannelModel> channelModelMap) {
        
        OutboundChannelModel channelModel = null;
        if ("jms".equalsIgnoreCase(channelType)) {
            JmsOutboundChannelModel jmsChannelModel = new JmsOutboundChannelModel();
            jmsChannelModel.setDestination(channelDestination);
            channelModel = jmsChannelModel;
        
        } else if ("kafka".equalsIgnoreCase(channelType)) {
            KafkaOutboundChannelModel kafkaChannelModel = new KafkaOutboundChannelModel();
            kafkaChannelModel.setTopic(channelDestination);
            channelModel = kafkaChannelModel;
        
        } else if ("rabbitmq".equalsIgnoreCase(channelType)) {
            RabbitOutboundChannelModel rabbitChannelModel = new RabbitOutboundChannelModel();
            rabbitChannelModel.setRoutingKey(channelDestination);
            channelModel = rabbitChannelModel;
        }
        
        String channelName = getElementValue("channelName", elementObject);
        channelModel.setKey(channelKey);
        if (StringUtils.isNotEmpty(channelName)) {
            channelModel.setName(channelName);
        } else {
            channelModel.setName(channelKey);
        }
        
        channelModel.setChannelType("outbound");
        String serializerType = getElementValue("serializerType", elementObject);
        if (StringUtils.isEmpty(serializerType)) {
            serializerType = "json";
        }
        channelModel.setSerializerType(serializerType);
        
        channelModelMap.put(channelKey, channelModel);
    }
    
    protected static void createInboundChannelInMap(String channelKey, String channelType, String channelDestination, 
                    String channelNameProperty, FlowElement elementObject, Map<String, ChannelModel> channelModelMap) {
        
        InboundChannelModel channelModel = null;
        if ("jms".equalsIgnoreCase(channelType)) {
            JmsInboundChannelModel jmsChannelModel = new JmsInboundChannelModel();
            jmsChannelModel.setDestination(channelDestination);
            channelModel = jmsChannelModel;
        
        } else if ("kafka".equalsIgnoreCase(channelType)) {
            KafkaInboundChannelModel kafkaChannelModel = new KafkaInboundChannelModel();
            kafkaChannelModel.setTopics(Collections.singletonList(channelDestination));
            channelModel = kafkaChannelModel;
        
        } else if ("rabbitmq".equalsIgnoreCase(channelType)) {
            RabbitInboundChannelModel rabbitChannelModel = new RabbitInboundChannelModel();
            rabbitChannelModel.setQueues(Collections.singletonList(channelDestination));
            channelModel = rabbitChannelModel;
        }
        
        String channelName = getElementValue(channelNameProperty, elementObject);
        channelModel.setKey(channelKey);
        if (StringUtils.isNotEmpty(channelName)) {
            channelModel.setName(channelName);
        } else {
            channelModel.setName(channelKey);
        }
        
        channelModel.setChannelType("inbound");
        String deserializerType = getElementValue("deserializerType", elementObject);
        if (StringUtils.isEmpty(deserializerType)) {
            deserializerType = "json";
        }
        channelModel.setDeserializerType(deserializerType);
        
        String keyDetectionType = getElementValue("keyDetectionType", elementObject);
        String keyDetectionValue = getElementValue("keyDetectionValue", elementObject);
        if (StringUtils.isNotEmpty(keyDetectionType) && StringUtils.isNotEmpty(keyDetectionValue)) {
            ChannelEventKeyDetection channelEventKeyDetection = new ChannelEventKeyDetection();
            if ("fixedValue".equalsIgnoreCase(keyDetectionType)) {
                channelEventKeyDetection.setFixedValue(keyDetectionValue);
            
            } else if ("jsonField".equalsIgnoreCase(keyDetectionType)) {
                channelEventKeyDetection.setJsonField(keyDetectionValue);
                
            } else if ("jsonPointer".equalsIgnoreCase(keyDetectionType)) {
                channelEventKeyDetection.setJsonPointerExpression(keyDetectionValue);
            }
            
            channelModel.setChannelEventKeyDetection(channelEventKeyDetection);
        }
        
        channelModelMap.put(channelKey, channelModel);
    }
    
    protected static List<EventPayload> getOutParameterEventPayload(List<ExtensionElement> parameterList) {
        List<EventPayload> eventPayloadList = new ArrayList<>();
        if (parameterList != null && parameterList.size() > 0) {
            for (ExtensionElement parameterElement : parameterList) {
                String name = parameterElement.getAttributeValue(null, "source");
                String type = parameterElement.getAttributeValue(null, "sourceType");
                if (StringUtils.isEmpty(type)) {
                    type = "string";
                }
                
                eventPayloadList.add(new EventPayload(name, type));
            }
        }
        
        return eventPayloadList;
    }
    
    protected static List<EventPayload> getOutIOParameterEventPayload(List<IOParameter> parameterList) {
        List<EventPayload> eventPayloadList = new ArrayList<>();
        if (parameterList != null && parameterList.size() > 0) {
            for (IOParameter parameter : parameterList) {
                String name = parameter.getSource();
                String type = parameter.getAttributeValue(null, "sourceType");
                if (StringUtils.isEmpty(type)) {
                    type = "string";
                }
                
                eventPayloadList.add(new EventPayload(name, type));
            }
        }
        
        return eventPayloadList;
    }
    
    protected static List<EventPayload> getInIOParameterEventPayload(List<IOParameter> parameterList) {
        List<EventPayload> eventPayloadList = new ArrayList<>();
        if (parameterList != null && parameterList.size() > 0) {
            for (IOParameter parameter : parameterList) {
                String name = parameter.getTarget();
                String type = parameter.getAttributeValue(null, "targetType");
                if (StringUtils.isEmpty(type)) {
                    type = "string";
                }
                
                eventPayloadList.add(new EventPayload(name, type));
            }
        }
        
        return eventPayloadList;
    }
    
    protected static List<EventCorrelationParameter> getEventCorrelationParameters(List<ExtensionElement> parameterList) {
        List<EventCorrelationParameter> correlationParameterList = new ArrayList<>();
        if (parameterList != null && parameterList.size() > 0) {
            for (ExtensionElement parameterElement : parameterList) {
                String name = parameterElement.getAttributeValue(null, "name");
                String type = parameterElement.getAttributeValue(null, "type");
                if (StringUtils.isEmpty(type)) {
                    type = "string";
                }
                
                correlationParameterList.add(new EventCorrelationParameter(name, type));
            }
        }
        
        return correlationParameterList;
    }
    
    protected static String getElementValue(String name, FlowElement elementObject) {
        List<ExtensionElement> elementList = elementObject.getExtensionElements().get(name);
        if (elementList != null && elementList.size() > 0) {
            return elementList.get(0).getElementText();
        }
        
        return null;
    }
}
