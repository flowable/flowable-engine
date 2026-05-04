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
package org.flowable.bpmn.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public class EventRegistryEventDefinition extends EventDefinition implements CustomBpmnEventDefinition {

    private static final Set<EventDefinitionLocation> SUPPORTED_LOCATIONS = Collections.unmodifiableSet(EnumSet.of(
            EventDefinitionLocation.START_EVENT,
            EventDefinitionLocation.EVENT_SUBPROCESS_START_EVENT,
            EventDefinitionLocation.INTERMEDIATE_CATCH_EVENT,
            EventDefinitionLocation.BOUNDARY_EVENT));

    @Override
    public Set<EventDefinitionLocation> getSupportedLocations() {
        return SUPPORTED_LOCATIONS;
    }

    protected String eventDefinitionKey;

    public EventRegistryEventDefinition() {
    }

    public EventRegistryEventDefinition(String eventDefinitionKey) {
        this.eventDefinitionKey = eventDefinitionKey;
    }

    public String getEventDefinitionKey() {
        return eventDefinitionKey;
    }

    public void setEventDefinitionKey(String eventDefinitionKey) {
        this.eventDefinitionKey = eventDefinitionKey;
    }

    @Override
    public EventRegistryEventDefinition clone() {
        EventRegistryEventDefinition clone = new EventRegistryEventDefinition();
        clone.setValues(this);
        return clone;
    }

    public void setValues(EventRegistryEventDefinition otherDefinition) {
        super.setValues(otherDefinition);
        setEventDefinitionKey(otherDefinition.getEventDefinitionKey());
    }
}
