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

package org.activiti.idm.engine.impl.cmd;

import java.io.Serializable;

import org.activiti.engine.ActivitiIllegalArgumentException;
import org.activiti.idm.api.Capability;
import org.activiti.idm.api.User;
import org.activiti.idm.engine.impl.interceptor.Command;
import org.activiti.idm.engine.impl.interceptor.CommandContext;
import org.activiti.idm.engine.impl.persistence.entity.CapabilityEntity;

/**
 * @author Joram Barrez
 */
public class CreateCapabilityCmd implements Command<Capability>, Serializable {

  private static final long serialVersionUID = 1L;

  protected String name;
  protected String userId;
  protected String groupId;

  public CreateCapabilityCmd(String name, String userId, String groupId) {
    if (name == null) {
      throw new ActivitiIllegalArgumentException("Capability name is null");
    }
    if (userId == null && groupId == null) {
      throw new ActivitiIllegalArgumentException("both userId and groupId are null");
    }
    
    this.name = name;
    this.userId = userId;
    this.groupId = groupId;
  }

  public Capability execute(CommandContext commandContext) {
    CapabilityEntity capabilityEntity = commandContext.getCapabilityEntityManager().create();
    capabilityEntity.setCapabilityName(name);
    capabilityEntity.setUserId(userId);
    capabilityEntity.setGroupId(groupId);
    commandContext.getCapabilityEntityManager().insert(capabilityEntity);
    return capabilityEntity;
  }
}
