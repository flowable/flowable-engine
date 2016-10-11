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
package org.activiti.idm.engine.delegate.event.impl;

import org.activiti.idm.api.event.ActivitiIdmEntityEvent;
import org.activiti.idm.api.event.ActivitiIdmEvent;
import org.activiti.idm.api.event.ActivitiIdmEventType;
import org.activiti.idm.api.event.ActivitiIdmMembershipEvent;

/**
 * Builder class used to create {@link ActivitiEvent} implementations.
 *
 * @author Tijs Rademakers
 */
public class ActivitiIdmEventBuilder {

  /**
   * @param type
   *          type of event
   * @return an {@link ActivitiEvent} that doesn't have it's execution context-fields filled, as the event is a global event, independent of any running execution.
   */
  public static ActivitiIdmEvent createGlobalEvent(ActivitiIdmEventType type) {
    ActivitiIdmEventImpl newEvent = new ActivitiIdmEventImpl(type);
    return newEvent;
  }

  /**
   * @param type
   *          type of event
   * @param entity
   *          the entity this event targets
   * @return an {@link ActivitiEntityEvent}. In case an {@link ExecutionContext} is active, the execution related event fields will be populated. If not, execution details will be retrieved from the
   *         {@link Object} if possible.
   */
  public static ActivitiIdmEntityEvent createEntityEvent(ActivitiIdmEventType type, Object entity) {
    ActivitiIdmEntityEventImpl newEvent = new ActivitiIdmEntityEventImpl(entity, type);
    return newEvent;
  }

  public static ActivitiIdmMembershipEvent createMembershipEvent(ActivitiIdmEventType type, String groupId, String userId) {
    ActivitiIdmMembershipEventImpl newEvent = new ActivitiIdmMembershipEventImpl(type);
    newEvent.setUserId(userId);
    newEvent.setGroupId(groupId);
    return newEvent;
  }
}
