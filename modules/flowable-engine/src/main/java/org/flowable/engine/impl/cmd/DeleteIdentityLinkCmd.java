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

package org.flowable.engine.impl.cmd;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.compatibility.Flowable5CompatibilityHandler;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.Flowable5Util;
import org.flowable.engine.impl.util.IdentityLinkUtil;
import org.flowable.engine.impl.util.TaskHelper;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

/**
 * @author Tom Baeyens
 * @author Falko Menge
 * @author Joram Barrez
 */
public class DeleteIdentityLinkCmd extends NeedsActiveTaskCmd<Void> {

    private static final long serialVersionUID = 1L;

    public static int IDENTITY_USER = 1;
    public static int IDENTITY_GROUP = 2;

    protected String userId;

    protected String groupId;

    protected String type;

    public DeleteIdentityLinkCmd(String taskId, String userId, String groupId, String type) {
        super(taskId);
        validateParams(userId, groupId, type, taskId);
        this.taskId = taskId;
        this.userId = userId;
        this.groupId = groupId;
        this.type = type;
    }

    protected void validateParams(String userId, String groupId, String type, String taskId) {
        if (taskId == null) {
            throw new FlowableIllegalArgumentException("taskId is null");
        }

        if (type == null) {
            throw new FlowableIllegalArgumentException("type is required when adding a new task identity link");
        }

        // Special treatment for assignee and owner: group cannot be used and userId may be null
        if (IdentityLinkType.ASSIGNEE.equals(type) || IdentityLinkType.OWNER.equals(type)) {
            if (groupId != null) {
                throw new FlowableIllegalArgumentException("Incompatible usage: cannot use type '" + type + "' together with a groupId");
            }
        } else {
            if (userId == null && groupId == null) {
                throw new FlowableIllegalArgumentException("userId and groupId cannot both be null");
            }
        }
    }

    @Override
    protected Void execute(CommandContext commandContext, TaskEntity task) {
        if (task.getProcessDefinitionId() != null && Flowable5Util.isFlowable5ProcessDefinitionId(commandContext, task.getProcessDefinitionId())) {
            Flowable5CompatibilityHandler compatibilityHandler = Flowable5Util.getFlowable5CompatibilityHandler();
            compatibilityHandler.deleteIdentityLink(taskId, userId, groupId, type);
            return null;
        }

        if (IdentityLinkType.ASSIGNEE.equals(type)) {
            TaskHelper.changeTaskAssignee(task, null);
        } else if (IdentityLinkType.OWNER.equals(type)) {
            TaskHelper.changeTaskOwner(task, null);
        } else {
            IdentityLinkUtil.deleteTaskIdentityLinks(task, userId, groupId, type);
        }

        CommandContextUtil.getHistoryManager(commandContext).createIdentityLinkComment(task, userId, groupId, type, false);

        return null;
    }

}
