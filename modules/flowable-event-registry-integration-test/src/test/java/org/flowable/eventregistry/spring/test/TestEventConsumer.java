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
package org.flowable.eventregistry.spring.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.flowable.eventregistry.api.EventConsumerInfo;
import org.flowable.eventregistry.api.EventRegistryEvent;
import org.flowable.eventregistry.api.EventRegistryEventConsumer;
import org.flowable.eventregistry.api.EventRegistryProcessingInfo;
import org.opentest4j.AssertionFailedError;

/**
 * @author Filip Hrisafov
 */
public class TestEventConsumer implements EventRegistryEventConsumer {

    protected final List<EventRegistryEvent> events = new ArrayList<>();
    
    @Override
    public String getConsumerKey() {
        return "testEventConsumer";
    }

    @Override
    public EventRegistryProcessingInfo eventReceived(EventRegistryEvent event) {
        events.add(event);
        EventRegistryProcessingInfo eventRegistryProcessingInfo = new EventRegistryProcessingInfo();
        eventRegistryProcessingInfo.addEventConsumerInfo(new EventConsumerInfo());
        return eventRegistryProcessingInfo;
    }

    public List<EventRegistryEvent> getEvents() {
        return events;
    }

    public EventRegistryEvent getEvent(String eventType) {
        return events.stream()
            .filter(event -> Objects.equals(event.getType(), eventType))
            .findAny()
            .orElseThrow(() -> new AssertionFailedError(events + " does not container an event with type " + eventType));
    }

    public void clear() {
        events.clear();
    }
}
