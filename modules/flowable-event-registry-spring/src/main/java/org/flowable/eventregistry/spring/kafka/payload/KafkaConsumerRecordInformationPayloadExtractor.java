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
package org.flowable.eventregistry.spring.kafka.payload;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.flowable.eventregistry.api.FlowableEventInfo;
import org.flowable.eventregistry.api.InboundEventInfoAwarePayloadExtractor;
import org.flowable.eventregistry.api.runtime.EventPayloadInstance;
import org.flowable.eventregistry.impl.runtime.EventPayloadInstanceImpl;
import org.flowable.eventregistry.model.EventModel;
import org.flowable.eventregistry.model.EventPayload;
import org.flowable.eventregistry.model.InboundChannelModel;
import org.flowable.eventregistry.model.KafkaInboundChannelModel;

/**
 * @author Filip Hrisafov
 */
public class KafkaConsumerRecordInformationPayloadExtractor<T> implements InboundEventInfoAwarePayloadExtractor<T> {

    @Override
    public Collection<EventPayloadInstance> extractPayload(EventModel eventModel, FlowableEventInfo<T> event) {
        InboundChannelModel inboundChannel = event.getInboundChannel();
        Object rawEvent = event.getRawEvent();
        if (inboundChannel instanceof KafkaInboundChannelModel && rawEvent instanceof ConsumerRecord) {
            return extractPayload(eventModel, (KafkaInboundChannelModel) inboundChannel, (ConsumerRecord<?, ?>) rawEvent);
        }

        return Collections.emptyList();
    }

    protected Collection<EventPayloadInstance> extractPayload(EventModel eventModel, KafkaInboundChannelModel inboundChannel,
            ConsumerRecord<?, ?> consumerRecord) {

        Collection<EventPayloadInstance> payloadInstances = new ArrayList<>();
        addPayloadIfAvailable(inboundChannel.getTopicOutputName(), eventModel, consumerRecord::topic, payloadInstances::add);
        addPayloadIfAvailable(inboundChannel.getPartitionOutputName(), eventModel, consumerRecord::partition, payloadInstances::add);
        addPayloadIfAvailable(inboundChannel.getOffsetOutputName(), eventModel, consumerRecord::offset, payloadInstances::add);

        return payloadInstances;
    }

    protected void addPayloadIfAvailable(String payloadName, EventModel model, Supplier<?> valueSupplier,
            Consumer<EventPayloadInstance> payloadInstanceConsumer) {
        if (StringUtils.isNotBlank(payloadName)) {
            EventPayload payloadDefinition = model.getPayload(payloadName);
            if (payloadDefinition != null) {
                payloadInstanceConsumer.accept(new EventPayloadInstanceImpl(payloadDefinition, valueSupplier.get()));
            }
        }
    }
}
