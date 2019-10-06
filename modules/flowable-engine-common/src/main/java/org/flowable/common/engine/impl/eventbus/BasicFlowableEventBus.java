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
package org.flowable.common.engine.impl.eventbus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.eventbus.FlowableEventBus;
import org.flowable.common.engine.api.eventbus.FlowableEventBusEvent;
import org.flowable.common.engine.api.eventbus.FlowableEventBusConsumer;

public class BasicFlowableEventBus implements FlowableEventBus {

    protected Map<String, List<FlowableEventBusConsumer>> eventConsumersByTypeMap = new HashMap<>();

    @Override
    public void sendEvent(FlowableEventBusEvent event) {
        handleEventConsumers(event.getType(), event);
        handleEventConsumers(null, event);
    }

    @Override
    public void addFlowableEventConsumer(FlowableEventBusConsumer eventConsumer) {
        List<String> supportedTypes = eventConsumer.getSupportedTypes();
        if (supportedTypes != null && !supportedTypes.isEmpty()) {
            for (String type : supportedTypes) {
                putEventConsumer(type, eventConsumer);
            }
            
        } else {
            putEventConsumer(null, eventConsumer);
        }
    }

    @Override
    public void removeFlowableEventConsumer(FlowableEventBusConsumer eventConsumer) {
        List<String> supportedTypes = eventConsumer.getSupportedTypes();
        if (supportedTypes != null && !supportedTypes.isEmpty()) {
            for (String type : supportedTypes) {
                removeConsumer(eventConsumer, type);
            }

        } else {
            removeConsumer(eventConsumer, null);

        }
    }

    protected void removeConsumer(FlowableEventBusConsumer eventConsumer, String type) {
        List<FlowableEventBusConsumer> consumers = eventConsumersByTypeMap.get(type);
        if (consumers != null) {
            if (consumers.size() == 1) {
                eventConsumersByTypeMap.remove(type);
            } else {
                consumers.remove(eventConsumer);
            }
        }
    }

    public List<FlowableEventBusConsumer> getEventConsumersForType(String type) {
        return eventConsumersByTypeMap.get(type);
    }
    
    public Map<String, List<FlowableEventBusConsumer>> getEventConsumersByTypeMap() {
        return eventConsumersByTypeMap;
    }
    
    public void setEventConsumersByTypeMap(Map<String, List<FlowableEventBusConsumer>> eventConsumersByTypeMap) {
        this.eventConsumersByTypeMap = eventConsumersByTypeMap;
    }

    protected void handleEventConsumers(String eventType, FlowableEventBusEvent event) {
        List<FlowableEventBusConsumer> eventConsumers =  eventConsumersByTypeMap.get(eventType);
        if (eventConsumers != null && !eventConsumers.isEmpty()) {
            for (FlowableEventBusConsumer eventConsumer : eventConsumers) {
                eventConsumer.eventReceived(event);
            }
        }
    }
    
    protected void putEventConsumer(String type, FlowableEventBusConsumer eventConsumer) {
        List<FlowableEventBusConsumer> existingEventConsumers = null;
        if (eventConsumersByTypeMap.containsKey(type)) {
            existingEventConsumers = eventConsumersByTypeMap.get(type);
        } else {
            existingEventConsumers = new ArrayList<>();
        }
        
        existingEventConsumers.add(eventConsumer);
        eventConsumersByTypeMap.put(type, existingEventConsumers);
    }
    
    protected void removeEventConsumer(String type, FlowableEventBusConsumer eventConsumer) {
        if (eventConsumersByTypeMap.containsKey(type)) {
            List<FlowableEventBusConsumer> existingEventConsumers = eventConsumersByTypeMap.get(type);
            existingEventConsumers.remove(eventConsumer);
        }
    }
}
