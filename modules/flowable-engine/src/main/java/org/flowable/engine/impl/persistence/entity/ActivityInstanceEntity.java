/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.flowable.engine.impl.persistence.entity;

import java.util.Date;

import org.flowable.common.engine.impl.db.HasRevision;
import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.engine.runtime.ActivityInstance;

/**
 * @author martin.grofcik
 */
public interface ActivityInstanceEntity extends ActivityInstance, Entity, HasRevision {

    void markEnded(String deleteReason);

    @Override
    String getProcessInstanceId();

    @Override
    String getProcessDefinitionId();

    @Override
    Date getStartTime();

    @Override
    Date getEndTime();

    @Override
    Long getDurationInMillis();

    void setProcessInstanceId(String processInstanceId);

    void setProcessDefinitionId(String processDefinitionId);

    void setStartTime(Date startTime);

    void setEndTime(Date endTime);

    void setDurationInMillis(Long durationInMillis);
    
    void setTransactionOrder(Integer transactionOrder);

    @Override
    String getDeleteReason();

    void setDeleteReason(String deleteReason);

    void setActivityId(String activityId);

    void setActivityName(String activityName);

    void setActivityType(String activityType);

    void setExecutionId(String executionId);

    void setAssignee(String assignee);

    void setTaskId(String taskId);

    void setCalledProcessInstanceId(String calledProcessInstanceId);

    void setTenantId(String tenantId);

}
