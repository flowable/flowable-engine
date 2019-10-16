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
package org.flowable.common.engine.impl.eventregistry.transformer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.flowable.common.engine.api.eventbus.FlowableEventBusEvent;
import org.flowable.common.engine.api.eventregistry.EventProcessingContext;
import org.flowable.common.engine.api.eventregistry.InboundEventTransformer;
import org.flowable.common.engine.api.eventregistry.runtime.EventInstance;
import org.flowable.common.engine.impl.eventregistry.event.EventRegistryEvent;

/**
 * @author Joram Barrez
 */
public class DefaultInboundEventTransformer implements InboundEventTransformer {

    @Override
    public List<FlowableEventBusEvent> transform(EventProcessingContext eventProcessingContext) {
        Collection<EventInstance> eventInstances = eventProcessingContext.getEventInstances();
        List<FlowableEventBusEvent> events = new ArrayList<>(eventInstances.size());
        for (EventInstance eventInstance : eventInstances) {
            events.add(new EventRegistryEvent(eventInstance));
        }
        return events;
    }

}
