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
package org.flowable.eventregistry.impl.payload;

import java.util.ArrayList;
import java.util.Collection;

import org.flowable.eventregistry.api.FlowableEventInfo;
import org.flowable.eventregistry.api.InboundEventInfoAwarePayloadExtractor;
import org.flowable.eventregistry.api.InboundEventPayloadExtractor;
import org.flowable.eventregistry.api.runtime.EventPayloadInstance;
import org.flowable.eventregistry.model.EventModel;

/**
 * @author Filip Hrisafov
 */
public class CompositePayloadExtractor<T> implements InboundEventInfoAwarePayloadExtractor<T> {

    protected final Collection<InboundEventPayloadExtractor<T>> payloadExtractors;

    public CompositePayloadExtractor(Collection<InboundEventPayloadExtractor<T>> payloadExtractors) {
        this.payloadExtractors = payloadExtractors;
    }

    @Override
    public Collection<EventPayloadInstance> extractPayload(EventModel eventModel, FlowableEventInfo<T> event) {
        if (payloadExtractors.size() == 1) {
            return payloadExtractors.iterator().next().extractPayload(eventModel, event);
        } else {
            Collection<EventPayloadInstance> payloadInstances = new ArrayList<>();
            for (InboundEventPayloadExtractor<T> payloadExtractor : payloadExtractors) {
                payloadInstances.addAll(payloadExtractor.extractPayload(eventModel, event));
            }

            return payloadInstances;
        }
    }
}
