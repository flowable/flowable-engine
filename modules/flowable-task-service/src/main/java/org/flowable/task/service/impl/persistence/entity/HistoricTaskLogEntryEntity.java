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

import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.task.api.history.HistoricTaskLogEntry;

/**
 * @author martin.grofcik
 */
public interface HistoricTaskLogEntryEntity extends HistoricTaskLogEntry, Entity {

    void setLogNumber(long logNumber);

    void setType(String type);

    void setTaskId(String taskId);

    void setTimeStamp(Date timeStamp);

    void setUserId(String userId);

    void setData(String data);

    void setExecutionId(String executionId);

    void setProcessInstanceId(String processInstanceId);

    void setProcessDefinitionId(String processDefinitionId);

    void setScopeId(String scopeId);

    void setScopeDefinitionId(String scopeDefinitionId);

    void setSubScopeId(String subScopeId);

    void setScopeType(String scopeType);

    void setTenantId(String tenantId);
}
