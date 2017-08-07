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
package org.flowable.identitylink.service.event;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.api.delegate.event.FlowableEvent;
import org.flowable.engine.common.api.delegate.event.FlowableEventType;

/**
 * Enumeration containing all possible types of {@link FlowableEvent}s.
 * 
 * @author Frederik Heremans
 * 
 */
public enum FlowableIdentityLinkServiceEventType implements FlowableEventType {

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
    CUSTOM;

    public static final FlowableIdentityLinkServiceEventType[] EMPTY_ARRAY = new FlowableIdentityLinkServiceEventType[] {};

    /**
     * @param string
     *            the string containing a comma-separated list of event-type names
     * @return a list of {@link FlowableIdentityLinkServiceEventType} based on the given list.
     * @throws FlowableIllegalArgumentException
     *             when one of the given string is not a valid type name
     */
    public static FlowableIdentityLinkServiceEventType[] getTypesFromString(String string) {
        List<FlowableIdentityLinkServiceEventType> result = new ArrayList<>();
        if (string != null && !string.isEmpty()) {
            String[] split = StringUtils.split(string, ",");
            for (String typeName : split) {
                boolean found = false;
                for (FlowableIdentityLinkServiceEventType type : values()) {
                    if (typeName.equals(type.name())) {
                        result.add(type);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new FlowableIllegalArgumentException("Invalid event-type: " + typeName);
                }
            }
        }

        return result.toArray(EMPTY_ARRAY);
    }
}
