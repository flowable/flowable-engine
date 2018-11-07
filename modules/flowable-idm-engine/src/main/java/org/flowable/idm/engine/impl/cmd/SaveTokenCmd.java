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
package org.flowable.idm.engine.impl.cmd;

import java.io.Serializable;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.idm.api.Token;
import org.flowable.idm.engine.impl.persistence.entity.TokenEntity;
import org.flowable.idm.engine.impl.util.CommandContextUtil;

/**
 * @author Tijs Rademakers
 */
public class SaveTokenCmd implements Command<Void>, Serializable {

    private static final long serialVersionUID = 1L;
    protected Token token;

    public SaveTokenCmd(Token token) {
        this.token = token;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        if (token == null) {
            throw new FlowableIllegalArgumentException("token is null");
        }

        if (CommandContextUtil.getTokenEntityManager(commandContext).isNewToken(token)) {
            if (token instanceof TokenEntity) {
                CommandContextUtil.getTokenEntityManager(commandContext).insert((TokenEntity) token, true);
            } else {
                CommandContextUtil.getDbSqlSession(commandContext).insert((Entity) token);
            }
        } else {
            CommandContextUtil.getTokenEntityManager(commandContext).updateToken(token);
        }

        return null;
    }
}
