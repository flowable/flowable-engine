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
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.IdentityLinkUtil;

/**
 * An abstract command supporting functionality around identity link management for process instances.
 *
 * @author Micha Kiener
 */
public abstract class AbstractProcessInstanceIdentityLinkCmd {

    /**
     * Returns the process instance entity for the given id, if it exists, otherwise an exception will be thrown.
     *
     * @param commandContext the command context within which the process instance is loaded
     * @param processInstanceId the id of the process instance to be loaded
     * @return the process instance entity, if found, never null
     * @throws FlowableIllegalArgumentException if the provided process instance id is not valid
     */
    protected ExecutionEntity getProcessInstanceEntity(CommandContext commandContext, String processInstanceId) {
        ExecutionEntity processInstance = CommandContextUtil.getExecutionEntityManager(commandContext).findById(processInstanceId);
        if (processInstance == null) {
            throw new FlowableIllegalArgumentException(
                "The process instance with id '" + processInstanceId + "' could not be found as an active process instance.");
        }
        return processInstance;
    }

    /**
     * This will remove ALL identity links with the given type, no mather whether they are user or group based.
     *
     * @param commandContext the command context within which to remove the identity links
     * @param processInstanceId the id of the process instance to remove the identity links for
     * @param identityType the identity link type (e.g. assignee or owner, etc) to be removed
     */
    protected void removeIdentityLinkType(CommandContext commandContext, String processInstanceId, String identityType) {
        ExecutionEntity processInstanceEntity = getProcessInstanceEntity(commandContext, processInstanceId);

        // this will remove ALL identity links with the given identity type (for users AND groups)
        IdentityLinkUtil.deleteProcessInstanceIdentityLinks(processInstanceEntity, null, null, identityType);
    }

    /**
     * Creates a new identity link entry for the given process instance, which can either be a user or group based one, but not both the same time.
     * If both the user and group ids are null, no new identity link is created.
     *
     * @param commandContext the command context within which to perform the identity link creation
     * @param processInstanceId the id of the process instance to create an identity link for
     * @param userId the user id if this is a user based identity link, otherwise null
     * @param groupId the group id if this is a group based identity link, otherwise null
     * @param identityType the type of identity link (e.g. owner or assignee, etc)
     */
    protected void createIdentityLinkType(CommandContext commandContext, String processInstanceId, String userId, String groupId, String identityType) {
        // if both user and group ids are null, don't create an identity link
        if (userId == null && groupId == null) {
            return;
        }

        // if both are set the same time, throw an exception as this is not allowed
        if (userId != null && groupId != null) {
            throw new FlowableIllegalArgumentException("Either set the user id or the group id for an identity link, but not both the same time.");
        }

        ExecutionEntity processInstanceEntity = getProcessInstanceEntity(commandContext, processInstanceId);
        IdentityLinkUtil.createProcessInstanceIdentityLink(processInstanceEntity, userId, groupId, identityType);
    }
}
