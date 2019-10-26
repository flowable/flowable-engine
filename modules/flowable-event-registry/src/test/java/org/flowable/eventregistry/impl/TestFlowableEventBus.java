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
package org.flowable.eventregistry.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.flowable.common.engine.api.eventbus.FlowableEventBus;
import org.flowable.common.engine.api.eventbus.FlowableEventBusConsumer;
import org.flowable.common.engine.api.eventbus.FlowableEventBusEvent;

/**
 * @author Filip Hrisafov
 */
public class TestFlowableEventBus implements FlowableEventBus {

    protected Map<String, Collection<FlowableEventBusConsumer>> eventConsumersByTypeMap = new HashMap<>();

    @Override
    public void sendEvent(FlowableEventBusEvent event) {
        handleEventConsumers(event.getType(), event);
        handleEventConsumers(null, event);
    }

    protected void handleEventConsumers(String eventType, FlowableEventBusEvent event) {
        Collection<FlowableEventBusConsumer> eventConsumers =  eventConsumersByTypeMap.getOrDefault(eventType, Collections.emptyList());
        if (!eventConsumers.isEmpty()) {
            for (FlowableEventBusConsumer eventConsumer : eventConsumers) {
                eventConsumer.eventReceived(event);
            }
        }
    }

    @Override
    public void addFlowableEventConsumer(FlowableEventBusConsumer eventConsumer) {
        Collection<String> supportedTypes = eventConsumer.getSupportedTypes();
        if (supportedTypes != null && !supportedTypes.isEmpty()) {
            for (String supportedType : supportedTypes) {
                eventConsumersByTypeMap.computeIfAbsent(supportedType, key -> new LinkedHashSet<>()).add(eventConsumer);
            }
        } else {
            eventConsumersByTypeMap.computeIfAbsent(null, key -> new LinkedHashSet<>()).add(eventConsumer);
        }
    }

    @Override
    public void removeFlowableEventConsumer(FlowableEventBusConsumer eventConsumer) {
        Collection<String> supportedTypes = eventConsumer.getSupportedTypes();
        if (supportedTypes != null && !supportedTypes.isEmpty()) {
            for (String supportedType : supportedTypes) {
                removeConsumer(eventConsumer, supportedType);
            }
        } else {
            removeConsumer(eventConsumer, null);
        }

    }

    protected void removeConsumer(FlowableEventBusConsumer eventConsumer, String type) {
        Collection<FlowableEventBusConsumer> consumers = eventConsumersByTypeMap.get(type);
        if (consumers != null) {
            if (consumers.size() == 1) {
                eventConsumersByTypeMap.remove(type);
            } else {
                consumers.remove(eventConsumer);
            }
        }
    }
}
