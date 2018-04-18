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
import org.flowable.idm.api.Privilege;
import org.flowable.idm.engine.impl.persistence.entity.PrivilegeEntity;
import org.flowable.idm.engine.impl.util.CommandContextUtil;

/**
 * @author Joram Barrez
 */
public class CreatePrivilegeCmd implements Command<Privilege>, Serializable {

    private static final long serialVersionUID = 1L;

    protected String name;

    public CreatePrivilegeCmd(String name) {
        if (name == null) {
            throw new FlowableIllegalArgumentException("Privilege name is null");
        }
        this.name = name;
    }

    @Override
    public Privilege execute(CommandContext commandContext) {
        long count = CommandContextUtil.getPrivilegeEntityManager(commandContext).createNewPrivilegeQuery().privilegeName(name).count();
        if (count > 0) {
            throw new FlowableIllegalArgumentException("Provided privilege name already exists");
        }

        PrivilegeEntity entity = CommandContextUtil.getPrivilegeEntityManager(commandContext).create();
        entity.setName(name);
        CommandContextUtil.getPrivilegeEntityManager(commandContext).insert(entity);
        return entity;
    }
}
