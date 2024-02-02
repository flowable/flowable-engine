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

/**
 * This interface provides a way to determine the record key that needs to be sent for a particular message
 *
 * @param <K> The type of the record key
 * @author Roman Saratz
 */
public interface KafkaMessageKeyProvider<K> {

    /**
     * Determine the record key for the outbound event.
     * Can be {@code null} if you do not want to use a specific value.
     *
     * @param outboundEvent the outbound event
     * @return the record key to use
     */
    K determineMessageKey(OutboundEvent<?> outboundEvent);

}
