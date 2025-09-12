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
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.task.api.history.HistoricTaskLogEntryType;
import org.flowable.task.service.TaskServiceConfiguration;
import org.flowable.task.service.impl.BaseHistoricTaskLogEntryBuilderImpl;
import org.flowable.task.service.impl.persistence.CountingTaskEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tijs Rademakers
 */
public class IdentityLinkUtil {

    public static IdentityLinkEntity createCaseInstanceIdentityLink(CaseInstanceEntity caseInstanceEntity, String userId, String groupId, String type,
            CmmnEngineConfiguration cmmnEngineConfiguration) {
        
        IdentityLinkEntity identityLinkEntity = cmmnEngineConfiguration.getIdentityLinkServiceConfiguration()
                .getIdentityLinkService().createScopeIdentityLink(null, caseInstanceEntity.getId(), ScopeTypes.CMMN, userId, groupId, type);

        CommandContextUtil.getCmmnHistoryManager().recordIdentityLinkCreated(caseInstanceEntity, identityLinkEntity);
        return identityLinkEntity;
    }
    
    public static void deleteTaskIdentityLinks(TaskEntity taskEntity, String userId, String groupId, String type, CmmnEngineConfiguration cmmnEngineConfiguration) {
        List<IdentityLinkEntity> removedIdentityLinkEntities = cmmnEngineConfiguration.getIdentityLinkServiceConfiguration()
                .getIdentityLinkService().deleteTaskIdentityLink(taskEntity.getId(), taskEntity.getIdentityLinks(), userId, groupId, type);
        handleTaskIdentityLinkDeletions(taskEntity, removedIdentityLinkEntities, true, cmmnEngineConfiguration);
    }

    public static void deleteCaseInstanceIdentityLinks(CaseInstance caseInstance, String userId, String groupId, String type, CmmnEngineConfiguration cmmnEngineConfiguration) {
        List<IdentityLinkEntity> removedIdentityLinkEntities = cmmnEngineConfiguration.getIdentityLinkServiceConfiguration()
                .getIdentityLinkService().deleteScopeIdentityLink(caseInstance.getId(), ScopeTypes.CMMN, userId, groupId, type);
        
        for (IdentityLinkEntity identityLinkEntity : removedIdentityLinkEntities) {
            CommandContextUtil.getCmmnHistoryManager().recordIdentityLinkDeleted(identityLinkEntity);
        }
    }

    public static void handleTaskIdentityLinkAdditions(TaskEntity taskEntity, List<IdentityLinkEntity> identityLinkEntities, CmmnEngineConfiguration cmmnEngineConfiguration) {
        for (IdentityLinkEntity identityLinkEntity : identityLinkEntities) {
            handleTaskIdentityLinkAddition(taskEntity, identityLinkEntity, cmmnEngineConfiguration);
        }
    }
    
    public static IdentityLinkEntity createPlanItemInstanceIdentityLink(PlanItemInstance planItemInstance, String userId, 
            String groupId, String type, CmmnEngineConfiguration cmmnEngineConfiguration) {
        
        IdentityLinkEntity identityLinkEntity = cmmnEngineConfiguration.getIdentityLinkServiceConfiguration().getIdentityLinkService()
                .createSubScopeIdentityLink(null, planItemInstance.getCaseInstanceId(), planItemInstance.getId(), ScopeTypes.PLAN_ITEM, userId, groupId, type);
        
        cmmnEngineConfiguration.getCmmnHistoryManager().recordIdentityLinkCreated(identityLinkEntity);
        
        return identityLinkEntity;
    }

    public static void handleTaskIdentityLinkAddition(TaskEntity taskEntity, IdentityLinkEntity identityLinkEntity, CmmnEngineConfiguration cmmnEngineConfiguration) {
        cmmnEngineConfiguration.getCmmnHistoryManager().recordIdentityLinkCreated(identityLinkEntity);

        CountingTaskEntity countingTaskEntity = (CountingTaskEntity) taskEntity;
        if (countingTaskEntity.isCountEnabled()) {
            countingTaskEntity.setIdentityLinkCount(countingTaskEntity.getIdentityLinkCount() + 1);
        }

        logTaskIdentityLinkEvent(HistoricTaskLogEntryType.USER_TASK_IDENTITY_LINK_ADDED.name(), taskEntity, 
                identityLinkEntity, cmmnEngineConfiguration);

        taskEntity.getIdentityLinks().add(identityLinkEntity);
        
        if (cmmnEngineConfiguration.getIdentityLinkInterceptor() != null) {
            cmmnEngineConfiguration.getIdentityLinkInterceptor().handleAddIdentityLinkToTask(taskEntity, identityLinkEntity);
        }
    }

    public static void handleTaskIdentityLinkDeletions(TaskEntity taskEntity, List<IdentityLinkEntity> identityLinks, boolean cascaseHistory,
            CmmnEngineConfiguration cmmnEngineConfiguration) {
        
        for (IdentityLinkEntity identityLinkEntity : identityLinks) {
            CountingTaskEntity countingTaskEntity = (CountingTaskEntity) taskEntity;
            if (countingTaskEntity.isCountEnabled()) {
                countingTaskEntity.setIdentityLinkCount(countingTaskEntity.getIdentityLinkCount() - 1);
            }
            if (cascaseHistory) {
                CommandContextUtil.getCmmnHistoryManager().recordIdentityLinkDeleted(identityLinkEntity);
            }
            logTaskIdentityLinkEvent(HistoricTaskLogEntryType.USER_TASK_IDENTITY_LINK_REMOVED.name(), taskEntity, 
                    identityLinkEntity, cmmnEngineConfiguration);
        }

        taskEntity.getIdentityLinks().removeAll(identityLinks);
    }

    protected static void logTaskIdentityLinkEvent(String eventType, TaskEntity taskEntity, IdentityLinkEntity identityLinkEntity,
            CmmnEngineConfiguration cmmnEngineConfiguration) {
        
        TaskServiceConfiguration taskServiceConfiguration = cmmnEngineConfiguration.getTaskServiceConfiguration();
        if (taskServiceConfiguration.isEnableHistoricTaskLogging()) {
            BaseHistoricTaskLogEntryBuilderImpl taskLogEntryBuilder = new BaseHistoricTaskLogEntryBuilderImpl(taskEntity);
            ObjectNode data = taskServiceConfiguration.getObjectMapper().createObjectNode();
            if (identityLinkEntity.isUser()) {
                data.put("userId", identityLinkEntity.getUserId());
            } else if (identityLinkEntity.isGroup()) {
                data.put("groupId", identityLinkEntity.getGroupId());
            }
            data.put("type", identityLinkEntity.getType());
            taskLogEntryBuilder.timeStamp(taskServiceConfiguration.getClock().getCurrentTime());
            taskLogEntryBuilder.userId(Authentication.getAuthenticatedUserId());
            taskLogEntryBuilder.data(data.toString());
            taskLogEntryBuilder.type(eventType);
            taskServiceConfiguration.getInternalHistoryTaskManager().recordHistoryUserTaskLog(taskLogEntryBuilder);
        }
    }

}