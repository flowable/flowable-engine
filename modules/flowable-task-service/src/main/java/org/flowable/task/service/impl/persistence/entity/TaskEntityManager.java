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
package org.flowable.task.service.impl.persistence.entity;

import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.persistence.entity.EntityManager;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskBuilder;
import org.flowable.task.api.TaskInfo;
import org.flowable.task.service.impl.TaskQueryImpl;

public interface TaskEntityManager extends EntityManager<TaskEntity> {

    /**
     * Creates {@link TaskEntity} according to {@link TaskInfo} template
     *
     * @param taskBuilder template to use when the task is created
     * @return created task entity
     */
    TaskEntity createTask(TaskBuilder taskBuilder);

    void changeTaskAssignee(TaskEntity taskEntity, String assignee);

    void changeTaskOwner(TaskEntity taskEntity, String owner);

    List<TaskEntity> findTasksByExecutionId(String executionId);

    List<TaskEntity> findTasksByProcessInstanceId(String processInstanceId);
    
    List<TaskEntity> findTasksByScopeIdAndScopeType(String scopeId, String scopeType);
    
    List<TaskEntity> findTasksBySubScopeIdAndScopeType(String subScopeId, String scopeType);

    List<Task> findTasksByQueryCriteria(TaskQueryImpl taskQuery);

    List<Task> findTasksWithRelatedEntitiesByQueryCriteria(TaskQueryImpl taskQuery);

    long findTaskCountByQueryCriteria(TaskQueryImpl taskQuery);

    List<Task> findTasksByNativeQuery(Map<String, Object> parameterMap);

    long findTaskCountByNativeQuery(Map<String, Object> parameterMap);

    List<Task> findTasksByParentTaskId(String parentTaskId);

    void updateTaskTenantIdForDeployment(String deploymentId, String newTenantId);
    
    void updateAllTaskRelatedEntityCountFlags(boolean configProperty);
    
    void deleteTasksByExecutionId(String executionId);
}