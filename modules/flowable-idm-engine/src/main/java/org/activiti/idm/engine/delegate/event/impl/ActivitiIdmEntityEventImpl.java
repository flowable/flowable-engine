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

import org.activiti.engine.common.api.ActivitiIllegalArgumentException;
import org.activiti.engine.common.api.delegate.event.ActivitiEntityEvent;
import org.activiti.idm.api.event.ActivitiIdmEventType;

/**
 * Base class for all {@link ActivitiIdmEvent} implementations, related to entities.
 * 
 * @author Tijs Rademakers
 */
public class ActivitiIdmEntityEventImpl extends ActivitiIdmEventImpl implements ActivitiEntityEvent {

  protected Object entity;

  public ActivitiIdmEntityEventImpl(Object entity, ActivitiIdmEventType type) {
    super(type);
    if (entity == null) {
      throw new ActivitiIllegalArgumentException("Entity cannot be null.");
    }
    this.entity = entity;
  }

  @Override
  public Object getEntity() {
    return entity;
  }
}
