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
package org.flowable.eventregistry.impl.transformer;

import java.util.Collection;
import java.util.Collections;

import org.flowable.eventregistry.api.EventRegistryEvent;
import org.flowable.eventregistry.api.InboundEventTransformer;
import org.flowable.eventregistry.api.runtime.EventInstance;
import org.flowable.eventregistry.impl.event.FlowableEventRegistryEvent;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class DefaultInboundEventTransformer implements InboundEventTransformer {

    @Override
    public Collection<EventRegistryEvent> transform(EventInstance eventInstance) {
        return Collections.singletonList(new FlowableEventRegistryEvent(eventInstance));
    }
}
