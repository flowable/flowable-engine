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
package org.flowable.eventregistry.spring.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.model.InboundChannelModel;
import org.springframework.kafka.listener.MessageListener;

/**
 * @author Filip Hrisafov
 */
public class KafkaChannelMessageListenerAdapter implements MessageListener<String, String> {

    protected EventRegistry eventRegistry;
    protected InboundChannelModel inboundChannelModel;

    public KafkaChannelMessageListenerAdapter(EventRegistry eventRegistry, InboundChannelModel inboundChannelModel) {
        this.eventRegistry = eventRegistry;
        this.inboundChannelModel = inboundChannelModel;
    }

    @Override
    public void onMessage(ConsumerRecord<String, String> data) {
        eventRegistry.eventReceived(inboundChannelModel, data.value());
    }

    public EventRegistry getEventRegistry() {
        return eventRegistry;
    }

    public void setEventRegistry(EventRegistry eventRegistry) {
        this.eventRegistry = eventRegistry;
    }

    public InboundChannelModel getInboundChannelModel() {
        return inboundChannelModel;
    }

    public void setInboundChannelModel(InboundChannelModel inboundChannelModel) {
        this.inboundChannelModel = inboundChannelModel;
    }

}
