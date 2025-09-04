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
package org.flowable.engine.impl.util;

import java.util.List;

import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.task.api.history.HistoricTaskLogEntryType;
import org.flowable.task.service.TaskServiceConfiguration;
import org.flowable.task.service.impl.BaseHistoricTaskLogEntryBuilderImpl;
import org.flowable.task.service.impl.persistence.CountingTaskEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class IdentityLinkUtil {

    public static IdentityLinkEntity createProcessInstanceIdentityLink(ExecutionEntity processInstanceExecution, String userId, String groupId, String type) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        IdentityLinkEntity identityLinkEntity = processEngineConfiguration.getIdentityLinkServiceConfiguration().getIdentityLinkService(
                ).createProcessInstanceIdentityLink(processInstanceExecution.getId(), userId, groupId, type);
        
        CommandContextUtil.getHistoryManager().recordIdentityLinkCreated(processInstanceExecution, identityLinkEntity);
        processInstanceExecution.getIdentityLinks().add(identityLinkEntity);
        
        return identityLinkEntity;
    }
    
    public static void deleteTaskIdentityLinks(TaskEntity taskEntity, String userId, String groupId, String type) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        List<IdentityLinkEntity> removedIdentityLinkEntities = processEngineConfiguration.getIdentityLinkServiceConfiguration().getIdentityLinkService().deleteTaskIdentityLink(
                        taskEntity.getId(), taskEntity.getIdentityLinks(), userId, groupId, type);
        
        handleTaskIdentityLinkDeletions(taskEntity, removedIdentityLinkEntities, true, true);
    }

    public static void deleteProcessInstanceIdentityLinks(ExecutionEntity processInstanceEntity, String userId, String groupId, String type) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        List<IdentityLinkEntity> removedIdentityLinkEntities = processEngineConfiguration.getIdentityLinkServiceConfiguration().getIdentityLinkService()
                .deleteProcessInstanceIdentityLink(processInstanceEntity.getId(), userId, groupId, type);
        for (IdentityLinkEntity identityLinkEntity : removedIdentityLinkEntities) {
            processEngineConfiguration.getHistoryManager().recordIdentityLinkDeleted(identityLinkEntity);
        }
        processInstanceEntity.getIdentityLinks().removeAll(removedIdentityLinkEntities);
    }

    public static void handleTaskIdentityLinkAdditions(TaskEntity taskEntity, List<IdentityLinkEntity> identityLinkEntities) {
        for (IdentityLinkEntity identityLinkEntity : identityLinkEntities) {
            handleTaskIdentityLinkAddition(taskEntity, identityLinkEntity);
        }
    }

    public static void handleTaskIdentityLinkAddition(TaskEntity taskEntity, IdentityLinkEntity identityLinkEntity) {
        CommandContextUtil.getHistoryManager().recordIdentityLinkCreated(identityLinkEntity);

        if (CountingEntityUtil.isTaskRelatedEntityCountEnabledGlobally()) {
            CountingTaskEntity countingTaskEntity = (CountingTaskEntity) taskEntity;
            if (CountingEntityUtil.isTaskRelatedEntityCountEnabled(countingTaskEntity)) {
                countingTaskEntity.setIdentityLinkCount(countingTaskEntity.getIdentityLinkCount() + 1);
            }
        }

        logTaskIdentityLinkEvent(HistoricTaskLogEntryType.USER_TASK_IDENTITY_LINK_ADDED.name(), taskEntity, identityLinkEntity);


        if (taskEntity.isIdentityLinksInitialized()) {
            /*
             * If the links have been initialized prior to creating the link, it would not have been added to the list and that is why we need to add it.
             * If the list is not initialized then the link will be fetched from the cache when we fetch the links from the DB
             */
            List<IdentityLinkEntity> identityLinks = taskEntity.getIdentityLinks();
            identityLinks.add(identityLinkEntity);
        }

        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        if (processEngineConfiguration.getIdentityLinkInterceptor() != null) {
            processEngineConfiguration.getIdentityLinkInterceptor().handleAddIdentityLinkToTask(taskEntity, identityLinkEntity);
        }
    }

    public static void handleTaskIdentityLinkDeletions(TaskEntity taskEntity, List<IdentityLinkEntity> identityLinks, boolean cascadeHistory, boolean updateTaskCounts) {
        for (IdentityLinkEntity identityLinkEntity : identityLinks) {
            if (cascadeHistory) {
                CommandContextUtil.getHistoryManager().recordIdentityLinkDeleted(identityLinkEntity);
            }
            if (updateTaskCounts) {
                handleTaskCountsForIdentityLinkDeletion(taskEntity, identityLinkEntity);
            }
            logTaskIdentityLinkEvent(HistoricTaskLogEntryType.USER_TASK_IDENTITY_LINK_REMOVED.name(), taskEntity, identityLinkEntity);
        }
        
        taskEntity.getIdentityLinks().removeAll(identityLinks);
    }

    protected static void handleTaskCountsForIdentityLinkDeletion(TaskEntity taskEntity, IdentityLinkEntity identityLink) {
        if (CountingEntityUtil.isTaskRelatedEntityCountEnabledGlobally()) {
            CountingTaskEntity countingTaskEntity = (CountingTaskEntity) taskEntity;
            if (CountingEntityUtil.isTaskRelatedEntityCountEnabled(countingTaskEntity)) {
                countingTaskEntity.setIdentityLinkCount(countingTaskEntity.getIdentityLinkCount() - 1);
            }   
        }
    }

    protected static void logTaskIdentityLinkEvent(String eventType, TaskEntity taskEntity, IdentityLinkEntity identityLinkEntity) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        TaskServiceConfiguration taskServiceConfiguration = processEngineConfiguration.getTaskServiceConfiguration();
        if (taskServiceConfiguration.isEnableHistoricTaskLogging()) {
            BaseHistoricTaskLogEntryBuilderImpl taskLogEntryBuilder = new BaseHistoricTaskLogEntryBuilderImpl(taskEntity);
            ObjectNode data = processEngineConfiguration.getObjectMapper().createObjectNode();
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