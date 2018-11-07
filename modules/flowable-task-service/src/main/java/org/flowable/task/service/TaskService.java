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
package org.flowable.task.service;

import org.flowable.task.api.Task;
import org.flowable.task.api.TaskBuilder;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

import java.util.List;

/**
 * Service which provides access to {@link Task} and form related operations.
 * 
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public interface TaskService {

    TaskEntity getTask(String id);
    
    List<TaskEntity> findTasksByExecutionId(String executionId);
    
    List<TaskEntity> findTasksByProcessInstanceId(String processInstanceId);
    
    List<Task> findTasksByParentTaskId(String parentTaskId);
    
    List<TaskEntity> findTasksBySubScopeIdScopeType(String subScopeId, String scopeType);
    
    TaskQuery createTaskQuery();
    
    void changeTaskAssignee(TaskEntity taskEntity, String userId);
    
    void changeTaskOwner(TaskEntity taskEntity, String ownerId);
    
    void updateTaskTenantIdForDeployment(String deploymentId, String tenantId);
    
    void updateTask(TaskEntity taskEntity, boolean fireUpdateEvent);
    
    void updateAllTaskRelatedEntityCountFlags(boolean configProperty);
    
    TaskEntity createTask();
    
    Task createTask(TaskBuilder taskBuilder);
    
    void insertTask(TaskEntity taskEntity, boolean fireCreateEvent);
    
    void deleteTask(TaskEntity task, boolean fireEvents);
    
    void deleteTasksByExecutionId(String executionId);
}
