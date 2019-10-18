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
package org.flowable.common.engine.api.eventregistry.definition;

import java.util.Collection;

import org.flowable.common.engine.api.eventregistry.EventRegistry;

/**
 * @author Joram Barrez
 */
public interface EventDefinitionBuilder {

    /**
     * Each event type will uniquely be identified with a key
     * (similar to the key of a process/case/decision/... definition),
     * which is typically referenced in process/case/... models.
     */
    EventDefinitionBuilder key(String key);

    /**
     * Some event definitions are unique to a certain channel (e.g. an external message queue).
     * When the event definition only applies to one channel only, set the logical key with this method.
     *
     * When there's only one channel or the event can be received through all channels,
     * this method doesn't need to be called.
     */
    EventDefinitionBuilder channelKey(String channelKey);

    /**
     * Allows to set multiple channel keys. See {@link #channelKey(String)}.
     */
    EventDefinitionBuilder channelKeys(Collection<String> channelKeys);

    /**
     * Defines one payload element of an event definition.
     * Such payload elements are data that is contained within an event.
     * If certain payload needs to be used to correlate runtime instances,
     * use the {@link #correlationParameter(String, String)} method.
     *
     * One {@link EventDefinition} typically has multiple such elements.
     */
    EventDefinitionBuilder payload(String name, String type);

    /**
     * Defines one parameters for correlation that can be used in models to map onto.
     * Each correlation parameter is automatically a {@link #payload(String, String)} element.
     *
     * Will create a {@link EventCorrelationParameterDefinition} behind the scenes.
     */
    EventDefinitionBuilder correlationParameter(String name, String type);

    /**
     * Registers the new {@link EventDefinition} based on the passed parameters
     * to this instance with the {@link EventRegistry}.
     */
    EventDefinition register();

}
