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

import java.util.Date;
import java.util.List;

import org.flowable.common.engine.impl.db.HasRevision;
import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInstanceEntity;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public interface HistoricTaskInstanceEntity extends Entity, HistoricTaskInstance, HasRevision {

    void setExecutionId(String executionId);

    void setName(String name);

    /** Sets an optional localized name for the task. */
    void setLocalizedName(String name);

    void setDescription(String description);

    /** Sets an optional localized description for the task. */
    void setLocalizedDescription(String description);

    void setAssignee(String assignee);

    void setTaskDefinitionKey(String taskDefinitionKey);

    void setFormKey(String formKey);

    void setPriority(int priority);

    void setDueDate(Date dueDate);

    void setCategory(String category);

    void setOwner(String owner);

    void setParentTaskId(String parentTaskId);

    void setClaimTime(Date claimTime);

    void setTenantId(String tenantId);
    
    Date getLastUpdateTime();
    
    void setLastUpdateTime(Date lastUpdateTime);

    List<HistoricVariableInstanceEntity> getQueryVariables();

    void setQueryVariables(List<HistoricVariableInstanceEntity> queryVariables);
    
    void markEnded(String deleteReason);

    void setProcessInstanceId(String processInstanceId);

    void setProcessDefinitionId(String processDefinitionId);

    void setTaskDefinitionId(String taskDefinitionId);

    void setScopeId(String scopeId);

    void setSubScopeId(String subScopeId);

    void setScopeType(String scopeType);

    void setScopeDefinitionId(String scopeDefinitionId);

    void setStartTime(Date startTime);

    void setEndTime(Date endTime);

    void setDurationInMillis(Long durationInMillis);

    void setDeleteReason(String deleteReason);

}
