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

import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.task.service.impl.persistence.CountingTaskEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class IdentityLinkUtil {
    
    public static IdentityLinkEntity createProcessInstanceIdentityLink(ExecutionEntity processInstanceExecution, String userId, String groupId, String type) {
        IdentityLinkEntity identityLinkEntity = CommandContextUtil.getIdentityLinkService().createProcessInstanceIdentityLink(
                        processInstanceExecution.getId(), userId, groupId, type);
        
        CommandContextUtil.getHistoryManager().recordIdentityLinkCreated(identityLinkEntity);
        processInstanceExecution.getIdentityLinks().add(identityLinkEntity);
        
        return identityLinkEntity;
    }
    
    public static void deleteTaskIdentityLinks(TaskEntity taskEntity, String userId, String groupId, String type) {
        List<IdentityLinkEntity> removedIdentityLinkEntities = CommandContextUtil.getIdentityLinkService().deleteTaskIdentityLink(
                        taskEntity.getId(), taskEntity.getIdentityLinks(), userId, groupId, type);
        
        handleTaskIdentityLinkDeletions(taskEntity, removedIdentityLinkEntities, true, true);
    }

    public static void deleteProcessInstanceIdentityLinks(ExecutionEntity processInstanceEntity, String userId, String groupId, String type) {
        List<IdentityLinkEntity> removedIdentityLinkEntities = CommandContextUtil.getIdentityLinkService().deleteProcessInstanceIdentityLink(
                        processInstanceEntity.getId(), userId, groupId, type);
        for (IdentityLinkEntity identityLinkEntity : removedIdentityLinkEntities) {
            CommandContextUtil.getHistoryManager().recordIdentityLinkDeleted(identityLinkEntity);
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
        
        taskEntity.getIdentityLinks().add(identityLinkEntity);
        if (identityLinkEntity.getUserId() != null && taskEntity.getProcessInstanceId() != null) {
            ExecutionEntity executionEntity = CommandContextUtil.getExecutionEntityManager().findById(taskEntity.getProcessInstanceId());
            for (IdentityLinkEntity identityLink : executionEntity.getIdentityLinks()) {
                if (identityLink.isUser() && identityLink.getUserId().equals(identityLinkEntity.getUserId())) {
                    return;
                }
            }
            
            IdentityLinkUtil.createProcessInstanceIdentityLink(executionEntity, identityLinkEntity.getUserId(), null, IdentityLinkType.PARTICIPANT);
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
    
}