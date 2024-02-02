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

import org.flowable.eventregistry.api.OutboundEvent;
import org.flowable.eventregistry.api.runtime.EventPayloadInstance;

/**
 * @author Filip Hrisafov
 */
public class EventPayloadKafkaPartitionProvider implements KafkaPartitionProvider {

    protected final String eventField;

    public EventPayloadKafkaPartitionProvider(String eventField) {
        this.eventField = eventField;
    }

    @Override
    public Integer determinePartition(OutboundEvent<?> outboundEvent) {
        for (EventPayloadInstance payloadInstance : outboundEvent.getEventInstance()
                .getPayloadInstances()) {
            if (eventField.equals(payloadInstance.getDefinitionName())) {
                return parseValue(payloadInstance.getValue());
            }
        }

        return null;
    }

    protected Integer parseValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            return Integer.parseInt(value.toString());
        } else if (value != null) {
            throw new IllegalStateException(
                    "The [" + eventField + "] must resolve to an Number or a String that can be parsed as an Integer. "
                            + "Resolved to [" + value.getClass() + "] for [" + value + "]");
        }

        return null;
    }
}
