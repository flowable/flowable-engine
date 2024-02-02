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

import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.idm.engine.IdmEngineConfiguration;
import org.flowable.idm.engine.impl.persistence.entity.PrivilegeMappingEntity;
import org.flowable.idm.engine.impl.persistence.entity.PrivilegeMappingEntityManager;

public class AddPrivilegeMappingCmd implements Command<Void>, Serializable {

    private static final long serialVersionUID = 1L;
    
    protected IdmEngineConfiguration idmEngineConfiguration;

    protected String privilegeId;
    protected String userId;
    protected String groupId;

    public AddPrivilegeMappingCmd(String privilegeId, String userId, String groupId, IdmEngineConfiguration idmEngineConfiguration) {
        this.privilegeId = privilegeId;
        this.userId = userId;
        this.groupId = groupId;
        this.idmEngineConfiguration = idmEngineConfiguration;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        PrivilegeMappingEntityManager privilegeMappingEntityManager = idmEngineConfiguration.getPrivilegeMappingEntityManager();
        PrivilegeMappingEntity entity = privilegeMappingEntityManager.create();
        entity.setPrivilegeId(privilegeId);
        if (userId != null) {
            entity.setUserId(userId);
        } else if (groupId != null) {
            entity.setGroupId(groupId);
        }
        privilegeMappingEntityManager.insert(entity);

        return null;
    }
}
