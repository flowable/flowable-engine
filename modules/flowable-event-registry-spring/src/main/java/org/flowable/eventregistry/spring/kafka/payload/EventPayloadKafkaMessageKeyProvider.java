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

import org.flowable.eventregistry.api.OutboundEvent;
import org.flowable.eventregistry.api.runtime.EventPayloadInstance;
import org.flowable.eventregistry.spring.kafka.KafkaMessageKeyProvider;

/**
 * @author Roman Saratz
 */
public class EventPayloadKafkaMessageKeyProvider implements KafkaMessageKeyProvider {

    protected final String eventField;

    public EventPayloadKafkaMessageKeyProvider(String eventField) {
        this.eventField = eventField;
    }

    @Override
    public String determineMessageKey(OutboundEvent<?> eventInstance) {
        for (EventPayloadInstance payloadInstance: eventInstance.getEventInstance().getPayloadInstances()) {
            if (eventField.equals(payloadInstance.getDefinitionName())) {
                return parseValue(payloadInstance.getValue());
            }
        }
        return null;
    }

    protected String parseValue(Object value) {
        if (value instanceof String) {
            return (String) value;
        } else if (value instanceof Integer) {
            return value.toString();
        } else if (value != null) {
            throw new IllegalStateException(
                    "The [" + eventField + "] must resolve to a String or an Integer. "
                            + "Resolved to [" + value.getClass() + "] for [" + value + "]");
        }

        return null;
    }
}
