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
package org.flowable.idm.engine.delegate.event.impl;

import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.idm.api.event.FlowableIdmEventType;
import org.flowable.idm.api.event.FlowableIdmMembershipEvent;

/**
 * Builder class used to create {@link FlowableEvent} implementations.
 *
 * @author Tijs Rademakers
 */
public class FlowableIdmEventBuilder {

    /**
     * @param type
     *            type of event
     * @return an {@link FlowableEvent} that doesn't have it's execution context-fields filled, as the event is a global event, independent of any running execution.
     */
    public static FlowableEvent createGlobalEvent(FlowableIdmEventType type) {
        FlowableIdmEventImpl newEvent = new FlowableIdmEventImpl(type);
        return newEvent;
    }

    /**
     * @param type
     *            type of event
     * @param entity
     *            the entity this event targets
     * @return an {@link FlowableEntityEvent}. In case an {@link ExecutionContext} is active, the execution related event fields will be populated. If not, execution details will be retrieved from the
     *         {@link Object} if possible.
     */
    public static FlowableEntityEvent createEntityEvent(FlowableIdmEventType type, Object entity) {
        FlowableIdmEntityEventImpl newEvent = new FlowableIdmEntityEventImpl(entity, type);
        return newEvent;
    }

    public static FlowableIdmMembershipEvent createMembershipEvent(FlowableIdmEventType type, String groupId, String userId) {
        FlowableIdmMembershipEventImpl newEvent = new FlowableIdmMembershipEventImpl(type);
        newEvent.setUserId(userId);
        newEvent.setGroupId(groupId);
        return newEvent;
    }
}
