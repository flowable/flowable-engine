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

import java.util.Set;

/**
 * @author Tijs Rademakers
 */
public abstract class EventDefinition extends BaseElement {

    @Override
    public abstract EventDefinition clone();

    /**
     * Returns the set of {@link EventDefinitionLocation}s where this {@link EventDefinition} is allowed.
     * Consulted by the BPMN process validators (start / event-subprocess / intermediate-catch / boundary)
     * to decide whether this event definition is valid in a given event host.
     */
    public abstract Set<EventDefinitionLocation> getSupportedLocations();
}
