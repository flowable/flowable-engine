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
package org.flowable.task.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.flowable.identitylink.api.IdentityLinkInfo;
import org.flowable.identitylink.service.IdentityLinkType;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskInfo;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.service.TaskService;
import org.flowable.task.service.TaskServiceConfiguration;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class TaskServiceImpl extends ServiceImpl implements TaskService {

    public TaskServiceImpl(TaskServiceConfiguration taskServiceConfiguration) {
        super(taskServiceConfiguration);
    }

    @Override
    public TaskEntity getTask(String id) {
        return getTaskEntityManager().findById(id);
    }

    @Override
    public List<TaskEntity> findTasksByExecutionId(String executionId) {
        return getTaskEntityManager().findTasksByExecutionId(executionId);
    }

    @Override
    public List<TaskEntity> findTasksByProcessInstanceId(String processInstanceId) {
        return getTaskEntityManager().findTasksByProcessInstanceId(processInstanceId);
    }

    @Override
    public List<Task> findTasksByParentTaskId(String parentTaskId) {
        return getTaskEntityManager().findTasksByParentTaskId(parentTaskId);
    }
    
    @Override
    public List<TaskEntity> findTasksBySubScopeIdScopeType(String subScopeId, String scopeType) {
        return getTaskEntityManager().findTasksBySubScopeIdAndScopeType(subScopeId, scopeType);
    }

    @Override
    public TaskQuery createTaskQuery() {
        return new TaskQueryImpl();
    }

    @Override
    public void changeTaskAssignee(TaskEntity taskEntity, String userId) {
        getTaskEntityManager().changeTaskAssignee(taskEntity, userId);
    }

    @Override
    public void changeTaskOwner(TaskEntity taskEntity, String ownerId) {
        getTaskEntityManager().changeTaskOwner(taskEntity, ownerId);
    }

    @Override
    public void updateTaskTenantIdForDeployment(String deploymentId, String tenantId) {
        getTaskEntityManager().updateTaskTenantIdForDeployment(deploymentId, tenantId);
    }

    @Override
    public void updateTask(TaskEntity taskEntity, boolean fireUpdateEvent) {
        getTaskEntityManager().update(taskEntity, fireUpdateEvent);
    }
    
    @Override
    public void updateAllTaskRelatedEntityCountFlags(boolean configProperty) {
        getTaskEntityManager().updateAllTaskRelatedEntityCountFlags(configProperty);
    }

    @Override
    public TaskEntity createTask() {
        return getTaskEntityManager().create();
    }

    @Override
    public void insertTask(TaskEntity taskEntity, boolean fireCreateEvent) {
        getTaskEntityManager().insert(taskEntity, fireCreateEvent);
    }

    @Override
    public void deleteTask(TaskEntity task, boolean fireEvents) {
        getTaskEntityManager().delete(task, fireEvents);
    }
    
    @Override
    public void deleteTasksByExecutionId(String executionId) {
        getTaskEntityManager().deleteTasksByExecutionId(executionId);
    }

    @Override
    public Task createTask(TaskInfo taskTemplate) {
        TaskInfo task = this.taskServiceConfiguration.getTaskBuilderPostProcessor().apply(taskTemplate);
        TaskEntity taskEntity = getTaskEntityManager().create();
        taskEntity.setId(task.getId());
        taskEntity.setName(task.getName());
        taskEntity.setDescription(task.getDescription());
        taskEntity.setPriority(task.getPriority());
        taskEntity.setOwner(task.getOwner());
        taskEntity.setAssignee(task.getAssignee());
        taskEntity.setDueDate(task.getDueDate());
        taskEntity.setCategory(task.getCategory());
        taskEntity.setParentTaskId(task.getParentTaskId());
        taskEntity.setTenantId(task.getTenantId());
        taskEntity.setFormKey(task.getFormKey());
        taskEntity.setTaskDefinitionId(task.getTaskDefinitionId());
        taskEntity.setTaskDefinitionKey(task.getTaskDefinitionKey());
        getTaskEntityManager().insert(taskEntity);
        taskEntity.addCandidateGroups(task.getIdentityLinks().stream().
                filter( identityLink -> StringUtils.isNotEmpty(identityLink.getGroupId()) && IdentityLinkType.CANDIDATE.equals(identityLink.getType())).
                map(IdentityLinkInfo::getGroupId).
                collect(Collectors.toSet())
        );
        taskEntity.addCandidateUsers(task.getIdentityLinks().stream().
                filter( identityLink -> StringUtils.isNotEmpty(identityLink.getUserId()) && IdentityLinkType.CANDIDATE.equals(identityLink.getType())).
                map(IdentityLinkInfo::getUserId).
                collect(Collectors.toSet())
        );
        return taskEntity;
    }
}
