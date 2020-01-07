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
import org.flowable.bpmn.model.Event;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.eventregistry.model.ChannelEventKeyDetection;
import org.flowable.eventregistry.model.ChannelModel;
import org.flowable.eventregistry.model.EventCorrelationParameter;
import org.flowable.eventregistry.model.EventModel;
import org.flowable.eventregistry.model.EventPayload;
import org.flowable.eventregistry.model.InboundChannelModel;
import org.flowable.eventregistry.model.JmsInboundChannelModel;
import org.flowable.eventregistry.model.KafkaInboundChannelModel;
import org.flowable.eventregistry.model.RabbitInboundChannelModel;

public class BpmnEventModelUtil {
    
    public static void fillChannelModelMap(List<Event> event, Map<String, ChannelModel> channelModelMap) {
        for (Event eventObject : event) {
            String channelKey = getElementValue("channelKey", eventObject);
            String channelType = getElementValue("channelType", eventObject);
            String channelDestination = getElementValue("channelDestination", eventObject);
            if (StringUtils.isNotEmpty(channelKey) && !channelModelMap.containsKey(channelKey) && StringUtils.isNotEmpty(channelType) &&
                            StringUtils.isNotEmpty(channelDestination)) {
                
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
                
                String channelName = getElementValue("channelName", eventObject);
                channelModel.setKey(channelKey);
                if (StringUtils.isNotEmpty(channelName)) {
                    channelModel.setName(channelName);
                } else {
                    channelModel.setName(channelKey);
                }
                
                channelModel.setChannelType("inbound");
                String deserializerType = getElementValue("deserializerType", eventObject);
                if (StringUtils.isEmpty(deserializerType)) {
                    deserializerType = "json";
                }
                channelModel.setDeserializerType(deserializerType);
                
                String keyDetectionType = getElementValue("keyDetectionType", eventObject);
                String keyDetectionValue = getElementValue("keyDetectionValue", eventObject);
                if (StringUtils.isNotEmpty(keyDetectionType) && StringUtils.isNotEmpty(keyDetectionValue)) {
                    ChannelEventKeyDetection channelEventKeyDetection = new ChannelEventKeyDetection();
                    if ("fixedValue".equalsIgnoreCase(keyDetectionType)) {
                        channelEventKeyDetection.setFixedValue(keyDetectionValue);
                    
                    } else if ("jsonField".equalsIgnoreCase(keyDetectionType)) {
                        channelEventKeyDetection.setJsonField(keyDetectionValue);
                        
                    } else if ("jsonPath".equalsIgnoreCase(keyDetectionType)) {
                        channelEventKeyDetection.setJsonPathExpression(keyDetectionValue);
                    }
                    
                    channelModel.setChannelEventKeyDetection(channelEventKeyDetection);
                }
                
                channelModelMap.put(channelKey, channelModel);
            }
        }
    }

    public static void fillEventModelMap(List<Event> event, Map<String, EventModel> eventModelMap) {
        for (Event eventObject : event) {
            String eventKey = getElementValue("eventType", eventObject);
            if (StringUtils.isNotEmpty(eventKey) && !eventModelMap.containsKey(eventKey)) {
                EventModel eventModel = new EventModel();
                String eventName = getElementValue("eventName", eventObject);
                eventModel.setKey(eventKey);
                if (StringUtils.isNotEmpty(eventName)) {
                    eventModel.setName(eventName);
                } else {
                    eventModel.setName(eventKey);
                }
                
                eventModel.setPayload(getOutParameterEventPayload(eventObject.getExtensionElements().get("eventOutParameter")));
                eventModel.setCorrelationParameters(getEventCorrelationParameters(eventObject.getExtensionElements().get("eventCorrelationParameter")));
                
                String channelKey = getElementValue("channelKey", eventObject);
                if (StringUtils.isNotEmpty(channelKey)) {
                    eventModel.addInboundChannelKey(channelKey);
                }
                
                eventModelMap.put(eventKey, eventModel);
            }
        }
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
    
    protected static String getElementValue(String name, Event eventObject) {
        List<ExtensionElement> elementList = eventObject.getExtensionElements().get(name);
        if (elementList != null && elementList.size() > 0) {
            return elementList.get(0).getElementText();
        }
        
        return null;
    }
}
