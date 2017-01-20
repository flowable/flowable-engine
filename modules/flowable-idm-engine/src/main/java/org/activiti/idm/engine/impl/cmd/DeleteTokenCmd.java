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
import org.activiti.idm.engine.impl.interceptor.Command;
import org.activiti.idm.engine.impl.interceptor.CommandContext;

/**
 * @author Tijs Rademakers
 */
public class DeleteTokenCmd implements Command<Void>, Serializable {

  private static final long serialVersionUID = 1L;
  String tokenId;

  public DeleteTokenCmd(String tokenId) {
    this.tokenId = tokenId;
  }

  public Void execute(CommandContext commandContext) {
    if (tokenId == null) {
      throw new ActivitiIllegalArgumentException("tokenId is null");
    }
    commandContext.getTokenEntityManager().delete(tokenId);

    return null;
  }
}
