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

import java.util.Date;

import org.flowable.common.engine.impl.db.HasRevision;
import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.engine.runtime.EventSubscription;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public interface EventSubscriptionEntity extends EventSubscription, Entity, HasRevision {

    void setEventType(String eventType);

    void setEventName(String eventName);

    void setExecutionId(String executionId);

    ExecutionEntity getExecution();

    void setExecution(ExecutionEntity execution);

    void setProcessInstanceId(String processInstanceId);

    void setConfiguration(String configuration);

    void setActivityId(String activityId);

    void setCreated(Date created);

    void setProcessDefinitionId(String processDefinitionId);

    void setTenantId(String tenantId);
}
