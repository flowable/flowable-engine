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
import org.flowable.common.engine.api.eventbus.FlowableEventBusItem;
import org.flowable.common.engine.api.eventbus.FlowableEventConsumer;

public class BasicFlowableEventBus implements FlowableEventBus {

    protected Map<String, List<FlowableEventConsumer>> eventConsumersByTypeMap = new HashMap<>();

    public void sendEvent(FlowableEventBusItem event) {
        handleEventConsumers(event.getType(), event);
        handleEventConsumers(null, event);
    }
    
    public void addFlowableEventConsumer(FlowableEventConsumer eventConsumer) {
        List<String> supportedTypes = eventConsumer.getSupportedTypes();
        if (supportedTypes != null && !supportedTypes.isEmpty()) {
            for (String type : supportedTypes) {
                putEventConsumer(type, eventConsumer);
            }
            
        } else {
            putEventConsumer(null, eventConsumer);
        }
    }
    
    public void removeFlowableEventConsumer(FlowableEventConsumer eventConsumer) {
        List<String> supportedTypes = eventConsumer.getSupportedTypes();
        if (supportedTypes != null && !supportedTypes.isEmpty()) {
            for (String type : supportedTypes) {
                putEventConsumer(type, eventConsumer);
            }
            
        } else {
            putEventConsumer(null, eventConsumer);
        }
    }
    
    public List<FlowableEventConsumer> getEventConsumersForType(String type) {
        return eventConsumersByTypeMap.get(type);
    }
    
    public Map<String, List<FlowableEventConsumer>> getEventConsumersByTypeMap() {
        return eventConsumersByTypeMap;
    }
    
    public void setEventConsumersByTypeMap(Map<String, List<FlowableEventConsumer>> eventConsumersByTypeMap) {
        this.eventConsumersByTypeMap = eventConsumersByTypeMap;
    }

    protected void handleEventConsumers(String eventType, FlowableEventBusItem event) {
        List<FlowableEventConsumer> eventConsumers =  eventConsumersByTypeMap.get(eventType);
        if (eventConsumers != null && !eventConsumers.isEmpty()) {
            for (FlowableEventConsumer eventConsumer : eventConsumers) {
                eventConsumer.eventReceived(event);
            }
        }
    }
    
    protected void putEventConsumer(String type, FlowableEventConsumer eventConsumer) {
        List<FlowableEventConsumer> existingEventConsumers = null;
        if (eventConsumersByTypeMap.containsKey(type)) {
            existingEventConsumers = eventConsumersByTypeMap.get(type);
        } else {
            existingEventConsumers = new ArrayList<>();
        }
        
        existingEventConsumers.add(eventConsumer);
        eventConsumersByTypeMap.put(type, existingEventConsumers);
    }
    
    protected void removeEventConsumer(String type, FlowableEventConsumer eventConsumer) {
        if (eventConsumersByTypeMap.containsKey(type)) {
            List<FlowableEventConsumer> existingEventConsumers = eventConsumersByTypeMap.get(type);
            existingEventConsumers.remove(eventConsumer);
        }
    }
}
