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
package org.flowable.idm.api.event;

import java.util.ArrayList;
import java.util.List;

import org.flowable.common.engine.api.delegate.event.FlowableEventType;

/**
 * Enumeration containing all possible types of {@link FlowableIdmEvent}s.
 * 
 * @author Frederik Heremans
 * 
 */
public enum FlowableIdmEventType implements FlowableEventType {

    /**
     * New entity is created.
     */
    ENTITY_CREATED,

    /**
     * New entity has been created and all child-entities that are created as a result of the creation of this particular entity are also created and initialized.
     */
    ENTITY_INITIALIZED,

    /**
     * Existing entity us updated.
     */
    ENTITY_UPDATED,

    /**
     * Existing entity is deleted.
     */
    ENTITY_DELETED,

    /**
     * An event type to be used by custom events. These types of events are never thrown by the engine itself, only be an external API call to dispatch an event.
     */
    CUSTOM,

    /**
     * The process-engine that dispatched this event has been created and is ready for use.
     */
    ENGINE_CREATED,

    /**
     * The process-engine that dispatched this event has been closed and cannot be used anymore.
     */
    ENGINE_CLOSED,

    /**
     * A new membership has been created.
     */
    MEMBERSHIP_CREATED,

    /**
     * A single membership has been deleted.
     */
    MEMBERSHIP_DELETED,

    /**
     * All memberships in the related group have been deleted. No individual {@link #MEMBERSHIP_DELETED} events will be dispatched due to possible performance reasons. The event is dispatched before
     * the memberships are deleted, so they can still be accessed in the dispatch method of the listener.
     */
    MEMBERSHIPS_DELETED;

    public static final FlowableIdmEventType[] EMPTY_ARRAY = new FlowableIdmEventType[] {};

    /**
     * @param string
     *            the string containing a comma-separated list of event-type names
     * @return a list of {@link FlowableIdmEventType} based on the given list.
     * @throws IllegalArgumentException
     *             when one of the given string is not a valid type name
     */
    public static FlowableIdmEventType[] getTypesFromString(String string) {
        List<FlowableIdmEventType> result = new ArrayList<>();
        if (string != null && !string.isEmpty()) {
            String[] split = string.split(",");
            for (String typeName : split) {
                boolean found = false;
                for (FlowableIdmEventType type : values()) {
                    if (typeName.equals(type.name())) {
                        result.add(type);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new IllegalArgumentException("Invalid event-type: " + typeName);
                }
            }
        }

        return result.toArray(EMPTY_ARRAY);
    }
}
