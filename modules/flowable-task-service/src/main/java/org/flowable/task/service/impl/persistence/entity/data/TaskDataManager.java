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
package org.flowable.task.service.impl.persistence.entity.data;

import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.persistence.entity.data.DataManager;
import org.flowable.task.api.Task;
import org.flowable.task.service.impl.TaskQueryImpl;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

/**
 * @author Tijs Rademakers
 */
public interface TaskDataManager extends DataManager<TaskEntity> {

    List<TaskEntity> findTasksByExecutionId(final String executionId);

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

    void updateAllTaskRelatedEntityCountFlags(boolean newValue);
    
    void deleteTasksByExecutionId(String executionId);

}
