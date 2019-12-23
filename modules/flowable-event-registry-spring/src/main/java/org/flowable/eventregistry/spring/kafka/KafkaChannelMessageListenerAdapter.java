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
import org.springframework.kafka.listener.MessageListener;

/**
 * @author Filip Hrisafov
 */
public class KafkaChannelMessageListenerAdapter implements MessageListener<String, String> {

    protected EventRegistry eventRegistry;
    protected String channelKey;

    public KafkaChannelMessageListenerAdapter(EventRegistry eventRegistry, String channelKey) {
        this.eventRegistry = eventRegistry;
        this.channelKey = channelKey;
    }

    @Override
    public void onMessage(ConsumerRecord<String, String> data) {
        eventRegistry.eventReceived(channelKey, data.value());
    }

    public EventRegistry getEventRegistry() {
        return eventRegistry;
    }

    public void setEventRegistry(EventRegistry eventRegistry) {
        this.eventRegistry = eventRegistry;
    }

    public String getChannelKey() {
        return channelKey;
    }

    public void setChannelKey(String channelKey) {
        this.channelKey = channelKey;
    }

}
