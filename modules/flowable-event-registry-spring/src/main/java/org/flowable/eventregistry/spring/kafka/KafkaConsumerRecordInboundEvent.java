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

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.flowable.eventregistry.api.InboundEvent;

/**
 * @author Filip Hrisafov
 */
public class KafkaConsumerRecordInboundEvent implements InboundEvent {

    protected final ConsumerRecord<?, ?> consumerRecord;
    protected Map<String, Object> headers;

    public KafkaConsumerRecordInboundEvent(ConsumerRecord<?, ?> consumerRecord) {
        this.consumerRecord = consumerRecord;
    }

    @Override
    public Object getRawEvent() {
        return consumerRecord;
    }

    @Override
    public Object getBody() {
        return consumerRecord.value();
    }

    @Override
    public Map<String, Object> getHeaders() {
        if (headers == null) {
            headers = retrieveHeaders();
        }
        return headers;
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> retrieveHeaders() {
        Map<String, Object> headers = new HashMap<>();
        Headers consumerRecordHeaders = consumerRecord.headers();
        for (Header consumerRecordHeader : consumerRecordHeaders) {
            headers.put(consumerRecordHeader.key(), consumerRecordHeader.value());
        }

        return headers;
    }
}
