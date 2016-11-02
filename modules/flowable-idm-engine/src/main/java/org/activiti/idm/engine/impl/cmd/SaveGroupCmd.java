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
import org.activiti.engine.impl.persistence.entity.Entity;
import org.activiti.idm.api.Group;
import org.activiti.idm.engine.impl.interceptor.Command;
import org.activiti.idm.engine.impl.interceptor.CommandContext;
import org.activiti.idm.engine.impl.persistence.entity.GroupEntity;

/**
 * @author Joram Barrez
 */
public class SaveGroupCmd implements Command<Void>, Serializable {

  private static final long serialVersionUID = 1L;
  protected Group group;

  public SaveGroupCmd(Group group) {
    this.group = group;
  }

  public Void execute(CommandContext commandContext) {
    if (group == null) {
      throw new ActivitiIllegalArgumentException("group is null");
    }

    if (commandContext.getGroupEntityManager().isNewGroup(group)) {
      if (group instanceof GroupEntity) {
        commandContext.getGroupEntityManager().insert((GroupEntity) group);
      } else {
        commandContext.getDbSqlSession().insert((Entity) group);
      }
    } else {
      if (group instanceof GroupEntity) {
        commandContext.getGroupEntityManager().update((GroupEntity) group);
      } else {
        commandContext.getDbSqlSession().update((Entity) group);
      }
      
    }
    return null;
  }

}
