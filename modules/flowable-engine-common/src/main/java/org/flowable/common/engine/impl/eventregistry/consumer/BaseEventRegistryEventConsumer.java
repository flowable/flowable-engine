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
package org.flowable.common.engine.impl.eventregistry.consumer;

import java.util.ArrayList;
import java.util.List;

import org.flowable.common.engine.api.eventbus.FlowableEventBusEvent;
import org.flowable.common.engine.api.eventregistry.EventRegistry;
import org.flowable.common.engine.api.eventregistry.EventRegistryEventBusConsumer;
import org.flowable.common.engine.api.eventregistry.runtime.EventInstance;
import org.flowable.common.engine.impl.eventregistry.event.EventRegistryEvent;

/**
 * @author Joram Barrez
 */
public abstract class BaseEventRegistryEventConsumer implements EventRegistryEventBusConsumer {

    protected List<String> supportedTypes = new ArrayList<>();
    protected EventRegistry eventRegistry;

    public BaseEventRegistryEventConsumer(EventRegistry eventRegistry) {
        this.eventRegistry = eventRegistry;
    }

    public void addSupportedType(String supportedType) {
        if (!supportedTypes.contains(supportedType)) {
            supportedTypes.add(supportedType);
        }
    }

    @Override
    public List<String> getSupportedTypes() {
        return supportedTypes;
    }

    @Override
    public void eventReceived(FlowableEventBusEvent event) {
        EventRegistryEvent eventRegistryEvent = (EventRegistryEvent) event;
        if (eventRegistryEvent.getEventInstance() != null) {
            eventReceived(eventRegistryEvent.getEventInstance());
        } else {
            // TODO: what should happen in this case?
        }
    }

    protected abstract void eventReceived(EventInstance eventInstance);

}
