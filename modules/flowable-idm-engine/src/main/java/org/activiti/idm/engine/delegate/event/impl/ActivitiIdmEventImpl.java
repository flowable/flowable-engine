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

import org.activiti.engine.ActivitiIllegalArgumentException;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.idm.api.event.ActivitiIdmEventType;

/**
 * Base class for all {@link ActivitiIdmEvent} implementations.
 * 
 * @author Tijs Rademakers
 */
public class ActivitiIdmEventImpl implements ActivitiEvent {

  protected ActivitiIdmEventType type;

  /**
   * Creates a new event implementation, not part of an execution context.
   */
  public ActivitiIdmEventImpl(ActivitiIdmEventType type) {
    if (type == null) {
      throw new ActivitiIllegalArgumentException("type is null");
    }
    this.type = type;
  }

  public ActivitiIdmEventType getType() {
    return type;
  }

  public void setType(ActivitiIdmEventType type) {
    this.type = type;
  }

  @Override
  public String toString() {
    return getClass() + " - " + type;
  }
  
}