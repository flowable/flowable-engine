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

import java.io.Serializable;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.compatibility.Flowable5CompatibilityHandler;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.Flowable5Util;
import org.flowable.engine.impl.util.IdentityLinkUtil;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class DeleteIdentityLinkForProcessInstanceCmd implements Command<Object>, Serializable {

    private static final long serialVersionUID = 1L;

    protected String processInstanceId;

    protected String userId;

    protected String groupId;

    protected String type;

    public DeleteIdentityLinkForProcessInstanceCmd(String processInstanceId, String userId, String groupId, String type) {
        validateParams(userId, groupId, processInstanceId, type);
        this.processInstanceId = processInstanceId;
        this.userId = userId;
        this.groupId = groupId;
        this.type = type;
    }

    protected void validateParams(String userId, String groupId, String processInstanceId, String type) {
        if (processInstanceId == null) {
            throw new FlowableIllegalArgumentException("processInstanceId is null");
        }

        if (type == null) {
            throw new FlowableIllegalArgumentException("type is required when deleting a process identity link");
        }

        if (userId == null && groupId == null) {
            throw new FlowableIllegalArgumentException("userId and groupId cannot both be null");
        }
    }

    @Override
    public Void execute(CommandContext commandContext) {
        ExecutionEntity processInstance = CommandContextUtil.getExecutionEntityManager(commandContext).findById(processInstanceId);

        if (processInstance == null) {
            throw new FlowableObjectNotFoundException("Cannot find process instance with id " + processInstanceId, ExecutionEntity.class);
        }

        if (Flowable5Util.isFlowable5ProcessDefinitionId(commandContext, processInstance.getProcessDefinitionId())) {
            Flowable5CompatibilityHandler compatibilityHandler = Flowable5Util.getFlowable5CompatibilityHandler();
            compatibilityHandler.deleteIdentityLinkForProcessInstance(processInstanceId, userId, groupId, type);
            return null;
        }

        IdentityLinkUtil.deleteProcessInstanceIdentityLinks(processInstance, userId, groupId, type);
        CommandContextUtil.getHistoryManager(commandContext).createProcessInstanceIdentityLinkComment(processInstance, userId, groupId, type, false);

        return null;
    }

}
