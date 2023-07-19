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
package org.flowable.engine.impl.persistence.entity;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.flowable.bpmn.model.FlowElement;
import org.flowable.common.engine.impl.persistence.entity.EntityManager;
import org.flowable.engine.impl.ExecutionQueryImpl;
import org.flowable.engine.impl.ProcessInstanceQueryImpl;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;

/**
 * @author Joram Barrez
 */
public interface ExecutionEntityManager extends EntityManager<ExecutionEntity> {

    ExecutionEntity createProcessInstanceExecution(ProcessDefinition processDefinition, String predefinedProcessInstanceId,
            String businessKey, String businessStatus, String processInstanceName, String callbackId, String callbackType, String referenceId,
            String referenceType, String propagatedStageInstanceId, String tenantId, String initiatorVariableName, String startActivityId);

    ExecutionEntity createChildExecution(ExecutionEntity parentExecutionEntity);

    ExecutionEntity createSubprocessInstance(ProcessDefinition processDefinition, ExecutionEntity superExecutionEntity, 
                    String businessKey, String startActivityId);

    /**
     * Finds the {@link ExecutionEntity} for the given root process instance id. All children will have been fetched and initialized.
     */
    ExecutionEntity findByRootProcessInstanceId(String rootProcessInstanceId);

    ExecutionEntity findSubProcessInstanceBySuperExecutionId(String superExecutionId);

    List<ExecutionEntity> findChildExecutionsByParentExecutionId(String parentExecutionId);

    List<ExecutionEntity> findChildExecutionsByProcessInstanceId(String processInstanceId);

    List<ExecutionEntity> findExecutionsByParentExecutionAndActivityIds(String parentExecutionId, Collection<String> activityIds);

    long findExecutionCountByQueryCriteria(ExecutionQueryImpl executionQuery);

    List<ExecutionEntity> findExecutionsByQueryCriteria(ExecutionQueryImpl executionQuery);

    long findProcessInstanceCountByQueryCriteria(ProcessInstanceQueryImpl executionQuery);

    List<ProcessInstance> findProcessInstanceByQueryCriteria(ProcessInstanceQueryImpl executionQuery);

    List<ProcessInstance> findProcessInstanceAndVariablesByQueryCriteria(ProcessInstanceQueryImpl executionQuery);

    Collection<ExecutionEntity> findInactiveExecutionsByProcessInstanceId(String processInstanceId);

    Collection<ExecutionEntity> findInactiveExecutionsByActivityIdAndProcessInstanceId(String activityId, String processInstanceId);

    List<Execution> findExecutionsByNativeQuery(Map<String, Object> parameterMap);

    List<ProcessInstance> findProcessInstanceByNativeQuery(Map<String, Object> parameterMap);

    long findExecutionCountByNativeQuery(Map<String, Object> parameterMap);

    long countActiveExecutionsByParentId(String parentId);

    /**
     * Returns all child executions of a given {@link ExecutionEntity}. 
     * In the list, child executions will be behind parent executions.
     * Children include subprocessinstances and its children.
     */
    List<ExecutionEntity> collectChildren(ExecutionEntity executionEntity);

    ExecutionEntity findFirstScope(ExecutionEntity executionEntity);

    ExecutionEntity findFirstMultiInstanceRoot(ExecutionEntity executionEntity);

    void updateExecutionTenantIdForDeployment(String deploymentId, String newTenantId);

    String updateProcessInstanceBusinessKey(ExecutionEntity executionEntity, String businessKey);
    
    String updateProcessInstanceBusinessStatus(ExecutionEntity executionEntity, String businessStatus);

    void deleteProcessInstancesByProcessDefinition(String processDefinitionId, String deleteReason, boolean cascade);
    
    void deleteProcessInstance(String processInstanceId, String deleteReason, boolean cascade);

    void deleteProcessInstance(String processInstanceId, String deleteReason, boolean cascade, boolean directDeleteInDatabase);

    void deleteProcessInstanceExecutionEntity(String processInstanceId, String currentFlowElementId,
            String deleteReason, boolean cascade, boolean cancel, boolean fireEvents);

    void deleteChildExecutions(ExecutionEntity executionEntity, Collection<String> executionIdsNotToDelete, 
            Collection<String> executionIdsNotToSendCancelledEventsFor, String deleteReason,
            boolean cancel, FlowElement cancelActivity);
    
    void deleteChildExecutions(ExecutionEntity executionEntity, String deleteReason, boolean cancel);

    void deleteExecutionAndRelatedData(ExecutionEntity executionEntity, String deleteReason, boolean deleteHistory, 
            boolean directDeleteInDatabase, boolean cancel, FlowElement cancelActivity);
    
    void deleteExecutionAndRelatedData(ExecutionEntity executionEntity, String deleteReason, boolean deleteHistory, boolean directDeleteInDatabase);
    
    void deleteExecutionAndRelatedData(ExecutionEntity executionEntity, String deleteReason, boolean deleteHistory);
    
    void deleteRelatedDataForExecution(ExecutionEntity executionEntity, String deleteReason, boolean directDeleteInDatabase);

    void updateProcessInstanceLockTime(String processInstanceId, String lockOwner, Date lockTime);

    void clearProcessInstanceLockTime(String processInstanceId);

    void clearAllProcessInstanceLockTimes(String lockOwner);

}