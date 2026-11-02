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
package org.flowable.engine.impl.event;

import org.flowable.bpmn.model.Event;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.EventRegistryEventDefinition;

public final class EventRegistryEventDefinitionUtil {

    private EventRegistryEventDefinitionUtil() {
    }

    /**
     * Returns the first {@link EventRegistryEventDefinition} on the given event, or {@code null} if none.
     */
    public static EventRegistryEventDefinition findOn(Event event) {
        if (event == null || event.getEventDefinitions() == null) {
            return null;
        }
        for (EventDefinition def : event.getEventDefinitions()) {
            if (def instanceof EventRegistryEventDefinition eventRegistry) {
                return eventRegistry;
            }
        }
        return null;
    }
}
