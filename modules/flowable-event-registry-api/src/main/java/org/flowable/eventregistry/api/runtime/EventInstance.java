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
package org.flowable.eventregistry.api.runtime;

import java.util.Collection;

import org.flowable.eventregistry.model.EventModel;

/**
 * Represents a runtime event (either received or sent).
 *
 * The event instance is based on an {@link EventModel} through an associated {@link org.flowable.eventregistry.api.EventDefinition},
 * which typically is determined by the channel pipeline (key detection phase).
 *
 * The {@link org.flowable.eventregistry.model.ChannelModel} represents the channel (and pipeline)
 * on which the event was received or, in the case of sending, needs to be sent out (can be multiple).
 *
 *
 * @author Joram Barrez
 */
public interface EventInstance {

    String getEventKey();

    Collection<EventPayloadInstance> getPayloadInstances();

    Collection<EventPayloadInstance> getCorrelationParameterInstances();

    String getTenantId();

}
