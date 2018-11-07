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
import org.flowable.idm.api.Group;
import org.flowable.idm.engine.impl.persistence.entity.GroupEntity;
import org.flowable.idm.engine.impl.util.CommandContextUtil;

/**
 * @author Joram Barrez
 */
public class SaveGroupCmd implements Command<Void>, Serializable {

    private static final long serialVersionUID = 1L;
    protected Group group;

    public SaveGroupCmd(Group group) {
        this.group = group;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        if (group == null) {
            throw new FlowableIllegalArgumentException("group is null");
        }

        if (CommandContextUtil.getGroupEntityManager(commandContext).isNewGroup(group)) {
            if (group instanceof GroupEntity) {
                CommandContextUtil.getGroupEntityManager(commandContext).insert((GroupEntity) group);
            } else {
                CommandContextUtil.getDbSqlSession(commandContext).insert((Entity) group);
            }
        } else {
            if (group instanceof GroupEntity) {
                CommandContextUtil.getGroupEntityManager(commandContext).update((GroupEntity) group);
            } else {
                CommandContextUtil.getDbSqlSession(commandContext).update((Entity) group);
            }

        }
        return null;
    }

}
