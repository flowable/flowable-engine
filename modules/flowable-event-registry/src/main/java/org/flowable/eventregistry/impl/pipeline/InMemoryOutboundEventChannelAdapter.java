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
package org.flowable.eventregistry.impl.pipeline;

import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.OutboundEventChannelAdapter;
import org.flowable.eventregistry.api.runtime.EventInstance;
import org.flowable.eventregistry.impl.event.FlowableEventRegistryEvent;

/**
 * @author Filip Hrisafov
 */
public class InMemoryOutboundEventChannelAdapter implements OutboundEventChannelAdapter<EventInstance> {

    protected final EventRegistry eventRegistry;

    public InMemoryOutboundEventChannelAdapter(EventRegistry eventRegistry) {
        this.eventRegistry = eventRegistry;
    }

    @Override
    public void sendEvent(EventInstance rawEvent) {
        eventRegistry.sendEventToConsumers(new FlowableEventRegistryEvent(rawEvent));
    }
}
