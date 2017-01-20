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
import org.activiti.engine.common.impl.persistence.entity.Entity;
import org.activiti.idm.api.Token;
import org.activiti.idm.engine.impl.interceptor.Command;
import org.activiti.idm.engine.impl.interceptor.CommandContext;
import org.activiti.idm.engine.impl.persistence.entity.TokenEntity;
import org.activiti.idm.engine.impl.persistence.entity.UserEntity;

/**
 * @author Tijs Rademakers
 */
public class SaveTokenCmd implements Command<Void>, Serializable {

  private static final long serialVersionUID = 1L;
  protected Token token;

  public SaveTokenCmd(Token token) {
    this.token = token;
  }

  public Void execute(CommandContext commandContext) {
    if (token == null) {
      throw new ActivitiIllegalArgumentException("token is null");
    }
    
    if (commandContext.getTokenEntityManager().isNewToken(token)) {
      if (token instanceof UserEntity) {
        commandContext.getTokenEntityManager().insert((TokenEntity) token, true);
      } else {
        commandContext.getDbSqlSession().insert((Entity) token);
      }
    } else {
      commandContext.getTokenEntityManager().updateToken(token);
    }

    return null;
  }
}
