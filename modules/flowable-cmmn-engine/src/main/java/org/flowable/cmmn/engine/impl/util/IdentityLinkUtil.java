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
package org.flowable.cmmn.engine.impl.util;

import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.task.service.impl.persistence.CountingTaskEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

/**
 * @author Tijs Rademakers
 */
public class IdentityLinkUtil {
    
    public static IdentityLinkEntity createCaseInstanceIdentityLink(CaseInstance caseInstance, String userId, String groupId, String type) {
        IdentityLinkEntity identityLinkEntity = CommandContextUtil.getIdentityLinkService().createScopeIdentityLink(
                        caseInstance.getCaseDefinitionId(), caseInstance.getId(), ScopeTypes.CMMN, userId, groupId, type);
        
        CommandContextUtil.getCmmnHistoryManager().recordIdentityLinkCreated(identityLinkEntity);
        
        return identityLinkEntity;
    }
    
    public static void deleteTaskIdentityLinks(TaskEntity taskEntity, String userId, String groupId, String type) {
        List<IdentityLinkEntity> removedIdentityLinkEntities = CommandContextUtil.getIdentityLinkService().deleteTaskIdentityLink(
                        taskEntity.getId(), taskEntity.getIdentityLinks(), userId, groupId, type);
        handleTaskIdentityLinkDeletions(taskEntity, removedIdentityLinkEntities, true);
    }

    public static void deleteCaseInstanceIdentityLinks(CaseInstance caseInstance, String userId, String groupId, String type) {
        List<IdentityLinkEntity> removedIdentityLinkEntities = CommandContextUtil.getIdentityLinkService().deleteScopeIdentityLink(
                        caseInstance.getId(), ScopeTypes.CMMN, userId, groupId, type);
        
        for (IdentityLinkEntity identityLinkEntity : removedIdentityLinkEntities) {
            CommandContextUtil.getCmmnHistoryManager().recordIdentityLinkDeleted(identityLinkEntity);
        }
    }

    public static void handleTaskIdentityLinkAdditions(TaskEntity taskEntity, List<IdentityLinkEntity> identityLinkEntities) {
        for (IdentityLinkEntity identityLinkEntity : identityLinkEntities) {
            handleTaskIdentityLinkAddition(taskEntity, identityLinkEntity);
        }
    }

    public static void handleTaskIdentityLinkAddition(TaskEntity taskEntity, IdentityLinkEntity identityLinkEntity) {
        CommandContextUtil.getCmmnHistoryManager().recordIdentityLinkCreated(identityLinkEntity);

        CountingTaskEntity countingTaskEntity = (CountingTaskEntity) taskEntity;
        if (countingTaskEntity.isCountEnabled()) {
            countingTaskEntity.setIdentityLinkCount(countingTaskEntity.getIdentityLinkCount() + 1);
        }

        taskEntity.getIdentityLinks().add(identityLinkEntity);
        CommandContextUtil.getInternalTaskAssignmentManager().addUserIdentityLinkToParent(taskEntity, identityLinkEntity.getUserId());
    }

    public static void handleTaskIdentityLinkDeletions(TaskEntity taskEntity, List<IdentityLinkEntity> identityLinks, boolean cascaseHistory) {
        for (IdentityLinkEntity identityLinkEntity : identityLinks) {
            CountingTaskEntity countingTaskEntity = (CountingTaskEntity) taskEntity;
            if (countingTaskEntity.isCountEnabled()) {
                countingTaskEntity.setIdentityLinkCount(countingTaskEntity.getIdentityLinkCount() - 1);
            }
            if (cascaseHistory) {
                CommandContextUtil.getCmmnHistoryManager().recordIdentityLinkDeleted(identityLinkEntity);
            }
        }

        taskEntity.getIdentityLinks().removeAll(identityLinks);
    }
}