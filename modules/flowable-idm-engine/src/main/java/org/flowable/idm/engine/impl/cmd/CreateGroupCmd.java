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
import org.flowable.idm.api.Group;
import org.flowable.idm.engine.impl.util.CommandContextUtil;

/**
 * @author Tom Baeyens
 */
public class CreateGroupCmd implements Command<Group>, Serializable {

    private static final long serialVersionUID = 1L;

    protected String groupId;

    public CreateGroupCmd(String groupId) {
        if (groupId == null) {
            throw new FlowableIllegalArgumentException("groupId is null");
        }
        this.groupId = groupId;
    }

    @Override
    public Group execute(CommandContext commandContext) {
        return CommandContextUtil.getGroupEntityManager(commandContext).createNewGroup(groupId);
    }

}
