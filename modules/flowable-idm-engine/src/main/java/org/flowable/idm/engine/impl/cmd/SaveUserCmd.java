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
import org.flowable.idm.api.PasswordEncoder;
import org.flowable.idm.api.PasswordSalt;
import org.flowable.idm.api.User;
import org.flowable.idm.engine.IdmEngineConfiguration;
import org.flowable.idm.engine.impl.persistence.entity.UserEntity;
import org.flowable.idm.engine.impl.util.CommandContextUtil;

/**
 * @author Joram Barrez
 */
public class SaveUserCmd implements Command<Void>, Serializable {

    private static final long serialVersionUID = 1L;
    
    protected IdmEngineConfiguration idmEngineConfiguration;
    
    protected User user;

    public SaveUserCmd(User user, IdmEngineConfiguration idmEngineConfiguration) {
        this.user = user;
        this.idmEngineConfiguration = idmEngineConfiguration;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        if (user == null) {
            throw new FlowableIllegalArgumentException("user is null");
        }
        
        if (idmEngineConfiguration.getUserEntityManager().isNewUser(user)) {
            if (user.getPassword() != null) {
                PasswordEncoder passwordEncoder = idmEngineConfiguration.getPasswordEncoder();
                PasswordSalt passwordSalt = idmEngineConfiguration.getPasswordSalt();
                user.setPassword(passwordEncoder.encode(user.getPassword(), passwordSalt));
            }
            
            if (user instanceof UserEntity) {
                idmEngineConfiguration.getUserEntityManager().insert((UserEntity) user, true);
            } else {
                CommandContextUtil.getDbSqlSession(commandContext).insert((Entity) user, idmEngineConfiguration.getIdGenerator());
            }
        } else {
            UserEntity dbUser = idmEngineConfiguration.getUserEntityManager().findById(user.getId());
            user.setPassword(dbUser.getPassword());
            idmEngineConfiguration.getUserEntityManager().updateUser(user);
        }

        return null;
    }
}
