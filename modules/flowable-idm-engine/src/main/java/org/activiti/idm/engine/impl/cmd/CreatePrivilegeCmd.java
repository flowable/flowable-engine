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

import org.activiti.engine.common.api.ActivitiIllegalArgumentException;
import org.activiti.idm.api.Privilege;
import org.activiti.idm.engine.impl.interceptor.Command;
import org.activiti.idm.engine.impl.interceptor.CommandContext;
import org.activiti.idm.engine.impl.persistence.entity.PrivilegeEntity;

/**
 * @author Joram Barrez
 */
public class CreatePrivilegeCmd implements Command<Privilege>, Serializable {

  private static final long serialVersionUID = 1L;

  protected String name;

  public CreatePrivilegeCmd(String name) {
    if (name == null) {
      throw new ActivitiIllegalArgumentException("Privilege name is null");
    }
    this.name = name;
  }

  public Privilege execute(CommandContext commandContext) {
    long count = commandContext.getPrivilegeEntityManager().createNewPrivilegeQuery().privilegeName(name).count();
    if (count > 0) {
      throw new ActivitiIllegalArgumentException("Provided privilege name already exists");
    }
    
    PrivilegeEntity entity = commandContext.getPrivilegeEntityManager().create();
    entity.setName(name);
    commandContext.getPrivilegeEntityManager().insert(entity);
    return entity;
  }
}
