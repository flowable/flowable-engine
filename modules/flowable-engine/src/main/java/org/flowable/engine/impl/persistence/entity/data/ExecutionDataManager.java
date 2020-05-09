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
package org.flowable.engine.impl.persistence.entity.data;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.persistence.entity.data.DataManager;
import org.flowable.engine.impl.ExecutionQueryImpl;
import org.flowable.engine.impl.ProcessInstanceQueryImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;

/**
 * @author Joram Barrez
 */
public interface ExecutionDataManager extends DataManager<ExecutionEntity> {

    ExecutionEntity findSubProcessInstanceBySuperExecutionId(final String superExecutionId);

    List<ExecutionEntity> findChildExecutionsByParentExecutionId(final String parentExecutionId);

    List<ExecutionEntity> findChildExecutionsByProcessInstanceId(final String processInstanceId);

    List<ExecutionEntity> findExecutionsByParentExecutionAndActivityIds(final String parentExecutionId, final Collection<String> activityIds);

    long findExecutionCountByQueryCriteria(ExecutionQueryImpl executionQuery);

    List<ExecutionEntity> findExecutionsByQueryCriteria(ExecutionQueryImpl executionQuery);

    long findProcessInstanceCountByQueryCriteria(ProcessInstanceQueryImpl executionQuery);

    List<ProcessInstance> findProcessInstanceByQueryCriteria(ProcessInstanceQueryImpl executionQuery);

    List<ExecutionEntity> findExecutionsByRootProcessInstanceId(String rootProcessInstanceId);

    List<ExecutionEntity> findExecutionsByProcessInstanceId(String processInstanceId);

    List<ProcessInstance> findProcessInstanceAndVariablesByQueryCriteria(ProcessInstanceQueryImpl executionQuery);

    Collection<ExecutionEntity> findInactiveExecutionsByProcessInstanceId(final String processInstanceId);

    Collection<ExecutionEntity> findInactiveExecutionsByActivityIdAndProcessInstanceId(final String activityId, final String processInstanceId);

    List<String> findProcessInstanceIdsByProcessDefinitionId(String processDefinitionId);

    List<Execution> findExecutionsByNativeQuery(Map<String, Object> parameterMap);

    List<ProcessInstance> findProcessInstanceByNativeQuery(Map<String, Object> parameterMap);

    long findExecutionCountByNativeQuery(Map<String, Object> parameterMap);

    void updateExecutionTenantIdForDeployment(String deploymentId, String newTenantId);

    void updateAllExecutionRelatedEntityCountFlags(boolean newValue);

    void updateProcessInstanceLockTime(String processInstanceId, Date lockDate, String lockOwner, Date expirationTime);

    void clearProcessInstanceLockTime(String processInstanceId);

    void clearAllProcessInstanceLockTimes(String lockOwner);

}
