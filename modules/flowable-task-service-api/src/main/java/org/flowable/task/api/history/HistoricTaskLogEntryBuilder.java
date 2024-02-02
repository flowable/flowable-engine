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
package org.flowable.task.api.history;

import java.util.Date;

/**
 * Interface to create and add task log entry
 *
 * @author martin.grofcik
 */
public interface HistoricTaskLogEntryBuilder {

    HistoricTaskLogEntryBuilder taskId(String taskId);

    HistoricTaskLogEntryBuilder type(String type);

    HistoricTaskLogEntryBuilder timeStamp(Date timeStamp);

    HistoricTaskLogEntryBuilder userId(String userId);

    HistoricTaskLogEntryBuilder processInstanceId(String processInstanceId);

    HistoricTaskLogEntryBuilder processDefinitionId(String processDefinitionId);

    HistoricTaskLogEntryBuilder executionId(String executionId);

    HistoricTaskLogEntryBuilder scopeId(String scopeId);

    HistoricTaskLogEntryBuilder scopeDefinitionId(String scopeDefinitionId);

    HistoricTaskLogEntryBuilder subScopeId(String subScopeId);

    HistoricTaskLogEntryBuilder scopeType(String scopeType);

    HistoricTaskLogEntryBuilder tenantId(String tenantId);

    HistoricTaskLogEntryBuilder data(String data);

    String getType();

    String getTaskId();

    Date getTimeStamp();

    String getUserId();

    String getData();

    String getExecutionId();

    String getProcessInstanceId();

    String getProcessDefinitionId();

    String getScopeId();

    String getScopeDefinitionId();

    String getSubScopeId();

    String getScopeType();

    String getTenantId();

    void create();

}
