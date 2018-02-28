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
import org.flowable.identitylink.service.IdentityLinkType;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.variable.api.type.VariableScopeType;

/**
 * @author Tijs Rademakers
 */
public class IdentityLinkUtil {
    
    public static IdentityLinkEntity createCaseInstanceIdentityLink(CaseInstance caseInstance, String userId, String groupId, String type) {
        IdentityLinkEntity identityLinkEntity = CommandContextUtil.getIdentityLinkService().createScopeIdentityLink(
                        caseInstance.getCaseDefinitionId(), caseInstance.getId(), VariableScopeType.CMMN, userId, groupId, type);
        
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
                        caseInstance.getId(), VariableScopeType.CMMN, userId, groupId, type);
        
        for (IdentityLinkEntity identityLinkEntity : removedIdentityLinkEntities) {
            CommandContextUtil.getCmmnHistoryManager().recordIdentityLinkDeleted(identityLinkEntity.getId());
        }
    }
    
    public static void handleTaskIdentityLinkAdditions(TaskEntity taskEntity, List<IdentityLinkEntity> identityLinkEntities) {
        for (IdentityLinkEntity identityLinkEntity : identityLinkEntities) {
            handleTaskIdentityLinkAddition(taskEntity, identityLinkEntity);
        }
    }
    
    public static void handleTaskIdentityLinkAddition(TaskEntity taskEntity, IdentityLinkEntity identityLinkEntity) {
        CommandContextUtil.getCmmnHistoryManager().recordIdentityLinkCreated(identityLinkEntity);

        taskEntity.getIdentityLinks().add(identityLinkEntity);
        if (identityLinkEntity.getUserId() != null && taskEntity.getScopeId() != null && VariableScopeType.CMMN.equals(taskEntity.getScopeType())) {
            CaseInstance caseInstance = CommandContextUtil.getCaseInstanceEntityManager().findById(taskEntity.getScopeId());
            if (caseInstance != null) {
                List<IdentityLinkEntity> identityLinks = CommandContextUtil.getIdentityLinkService().findIdentityLinksByScopeIdAndType(taskEntity.getScopeId(), VariableScopeType.CMMN);
                for (IdentityLinkEntity identityLink : identityLinks) {
                    if (identityLink.isUser() && identityLink.getUserId().equals(identityLinkEntity.getUserId())) {
                        return;
                    }
                }
                
                createCaseInstanceIdentityLink(caseInstance, identityLinkEntity.getUserId(), null, IdentityLinkType.PARTICIPANT);
            }
        }
    }
    
    public static void handleTaskIdentityLinkDeletions(TaskEntity taskEntity, List<IdentityLinkEntity> identityLinks, boolean cascaseHistory) {
        for (IdentityLinkEntity identityLinkEntity : identityLinks) {
            if (cascaseHistory) {
                CommandContextUtil.getCmmnHistoryManager().recordIdentityLinkDeleted(identityLinkEntity.getId());
            }
        }
        
        taskEntity.getIdentityLinks().removeAll(identityLinks);
    }
}