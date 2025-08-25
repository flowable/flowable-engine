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
package org.flowable.eventregistry.api;

import java.util.Collection;

import org.flowable.eventregistry.api.runtime.EventPayloadInstance;
import org.flowable.eventregistry.model.EventModel;

/**
 * @author Filip Hrisafov
 */
@FunctionalInterface
public interface InboundEventInfoAwarePayloadExtractor<T> extends InboundEventPayloadExtractor<T> {

    @Override
    default Collection<EventPayloadInstance> extractPayload(EventModel eventModel, T payload) {
        throw new UnsupportedOperationException("Payload extraction should never call this ");
    }

    @Override
    Collection<EventPayloadInstance> extractPayload(EventModel eventModel, FlowableEventInfo<T> event);

}
