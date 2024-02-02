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
import org.flowable.idm.engine.impl.util.CommandContextUtil;

/**
 * @author Tom Baeyens
 */
public class CreateMembershipCmd implements Command<Object>, Serializable {

    private static final long serialVersionUID = 1L;

    String userId;
    String groupId;

    public CreateMembershipCmd(String userId, String groupId) {
        this.userId = userId;
        this.groupId = groupId;
    }

    @Override
    public Object execute(CommandContext commandContext) {
        if (userId == null) {
            throw new FlowableIllegalArgumentException("userId is null");
        }
        if (groupId == null) {
            throw new FlowableIllegalArgumentException("groupId is null");
        }
        CommandContextUtil.getMembershipEntityManager(commandContext).createMembership(userId, groupId);
        return null;
    }
}
