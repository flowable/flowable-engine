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
package org.flowable.common.engine.impl.eventregistry.definition;

import java.util.ArrayList;
import java.util.Collection;

import org.flowable.common.engine.api.eventregistry.definition.EventCorrelationParameterDefinition;
import org.flowable.common.engine.api.eventregistry.definition.EventDefinition;
import org.flowable.common.engine.api.eventregistry.definition.EventPayloadDefinition;

/**
 * @author Joram Barrez
 */
public class EventDefinitionImpl implements EventDefinition {

    protected String key;
    protected Collection<String> channelKeys;
    protected Collection<EventCorrelationParameterDefinition> correlationParameterDefinitions = new ArrayList<>();
    protected Collection<EventPayloadDefinition> eventPayloadDefinitions = new ArrayList<>();

    @Override
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public Collection<String> getChannelKeys() {
        return channelKeys;
    }

    public void setChannelKeys(Collection<String> channelKeys) {
        this.channelKeys = channelKeys;
    }

    @Override
    public Collection<EventCorrelationParameterDefinition> getCorrelationParameterDefinitions() {
        return correlationParameterDefinitions;
    }

    public void setCorrelationParameterDefinitions(Collection<EventCorrelationParameterDefinition> correlationParameterDefinitions) {
        this.correlationParameterDefinitions = correlationParameterDefinitions;
    }

    @Override
    public Collection<EventPayloadDefinition> getEventPayloadDefinitions() {
        return eventPayloadDefinitions;
    }

    public void setEventPayloadDefinitions(Collection<EventPayloadDefinition> eventPayloadDefinitions) {
        this.eventPayloadDefinitions = eventPayloadDefinitions;
    }

}
