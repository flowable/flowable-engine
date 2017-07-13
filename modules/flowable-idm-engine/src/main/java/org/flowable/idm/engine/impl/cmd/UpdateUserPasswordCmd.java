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

import org.flowable.idm.api.PasswordEncoder;
import org.flowable.idm.api.PasswordSalt;
import org.flowable.idm.api.User;
import org.flowable.idm.engine.impl.interceptor.Command;
import org.flowable.idm.engine.impl.interceptor.CommandContext;

/**
 * @author faizal-manan
 */
public class UpdateUserPasswordCmd implements Command<User>, Serializable {

    private static final long serialVersionUID = 1L;
    
    private User user;

    public UpdateUserPasswordCmd(User user) {
        this.user = user;
    }

    @Override
    public User execute(CommandContext commandContext) {
        if (!commandContext.getUserEntityManager().isNewUser(user)) {
            PasswordEncoder passwordEncoder = commandContext.getIdmEngineConfiguration().getPasswordEncoder();
            PasswordSalt passwordSalt = commandContext.getIdmEngineConfiguration().getPasswordSalt();
            
            user.setPassword(passwordEncoder.encode(user.getPassword(), passwordSalt));
            commandContext.getUserEntityManager().updateUser(user);
        }
        return user;
    }
}
