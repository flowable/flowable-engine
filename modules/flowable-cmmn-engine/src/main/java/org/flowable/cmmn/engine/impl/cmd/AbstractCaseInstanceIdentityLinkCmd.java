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
package org.flowable.cmmn.engine.impl.cmd;

import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.impl.util.IdentityLinkUtil;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * An abstract command supporting functionality around identity link management for case instances.
 *
 * @author Micha Kiener
 */
public abstract class AbstractCaseInstanceIdentityLinkCmd {

    /**
     * Returns the case instance entity for the given id, if it exists, otherwise an exception will be thrown.
     *
     * @param commandContext the command context within which the case instance is loaded
     * @param caseInstanceId the id of the case instance to be loaded
     * @return the case instance entity, if found, never null
     * @throws FlowableIllegalArgumentException if the provided case instance id is not valid (could not be found)
     */
    protected CaseInstanceEntity getCaseInstanceEntity(CommandContext commandContext, String caseInstanceId) {
        CaseInstanceEntity caseInstanceEntity = CommandContextUtil.getCaseInstanceEntityManager(commandContext).findById(caseInstanceId);
        if (caseInstanceEntity == null) {
            throw new FlowableIllegalArgumentException(
                "The case instance with id '" + caseInstanceId + "' could not be found as an active case instance.");
        }
        return caseInstanceEntity;
    }

    /**
     * This will remove ALL identity links with the given type, no mather whether they are user or group based.
     *
     * @param commandContext the command context within which to remove the identity links
     * @param caseInstanceId the id of the case instance to remove the identity links for
     * @param identityType the identity link type (e.g. assignee or owner, etc) to be removed
     */
    protected void removeIdentityLinkType(CommandContext commandContext, String caseInstanceId, String identityType) {
        CaseInstanceEntity caseInstanceEntity = getCaseInstanceEntity(commandContext, caseInstanceId);

        // this will remove ALL identity links with the given identity type (for users AND groups)
        IdentityLinkUtil.deleteCaseInstanceIdentityLinks(caseInstanceEntity, null, null, identityType,
            CommandContextUtil.getCmmnEngineConfiguration(commandContext));
    }

    /**
     * Creates a new identity link entry for the given case instance, which can either be a user or group based one, but not both the same time.
     * If both the user and group ids are null, no new identity link is created.
     *
     * @param commandContext the command context within which to perform the identity link creation
     * @param caseInstanceId the id of the case instance to create an identity link for
     * @param userId the user id if this is a user based identity link, otherwise null
     * @param groupId the group id if this is a group based identity link, otherwise null
     * @param identityType the type of identity link (e.g. owner or assignee, etc)
     */
    protected void createIdentityLinkType(CommandContext commandContext, String caseInstanceId, String userId, String groupId, String identityType) {
        // if both user and group ids are null, don't create an identity link
        if (userId == null && groupId == null) {
            return;
        }

        // if both are set the same time, throw an exception as this is not allowed
        if (userId != null && groupId != null) {
            throw new FlowableIllegalArgumentException("Either set the user id or the group id for an identity link, but not both the same time.");
        }

        CaseInstanceEntity caseInstanceEntity = getCaseInstanceEntity(commandContext, caseInstanceId);
        IdentityLinkUtil.createCaseInstanceIdentityLink(caseInstanceEntity, userId, groupId, identityType,
            CommandContextUtil.getCmmnEngineConfiguration(commandContext));
    }
}

