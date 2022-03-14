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

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.flowable.eventregistry.api.InboundEventContextExtractor;
import org.flowable.eventregistry.api.model.EventPayloadTypes;
import org.flowable.eventregistry.model.EventHeader;
import org.flowable.eventregistry.model.EventModel;

public class KafkaConsumerRecordInboundEventContextExtractor implements InboundEventContextExtractor {
    
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> extractContextInfo(Object event, EventModel eventModel) {
        Map<String, Object> contextInfo = new HashMap<>();
        
        ConsumerRecord<Object, Object> consumerRecord = (ConsumerRecord<Object, Object>) event;
        Headers headers = consumerRecord.headers();
        Iterator<Header> itHeader = headers.iterator();
        while (itHeader.hasNext()) {
            Header header = itHeader.next();
            EventHeader eventHeaderDef = eventModel.getHeader(header.key());
            if (eventHeaderDef != null) {
                if (EventPayloadTypes.STRING.equals(eventHeaderDef.getType())) {
                    contextInfo.put(header.key(), convertToString(header.value()));

                } else if (EventPayloadTypes.DOUBLE.equals(eventHeaderDef.getType())) {
                    contextInfo.put(header.key(), Double.valueOf(convertToString(header.value())));

                } else if (EventPayloadTypes.INTEGER.equals(eventHeaderDef.getType())) {
                    contextInfo.put(header.key(), Integer.valueOf(convertToString(header.value())));

                } else if (EventPayloadTypes.LONG.equals(eventHeaderDef.getType())) {
                    contextInfo.put(header.key(), Long.valueOf(convertToString(header.value())));

                } else if (EventPayloadTypes.BOOLEAN.equals(eventHeaderDef.getType())) {
                    contextInfo.put(header.key(), Boolean.valueOf(convertToString(header.value())));
                    
                } else {
                    contextInfo.put(header.key(), header.value());
                }
                
            } else {
                contextInfo.put(header.key(), header.value());
            }
        }
        
        return contextInfo;
    }

    protected String convertToString(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
