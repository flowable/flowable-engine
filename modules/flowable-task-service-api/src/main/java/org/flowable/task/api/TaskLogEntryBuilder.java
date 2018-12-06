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
package org.flowable.task.api;

import java.util.Date;

/**
 * Interface to create and add task log entry
 *
 * @author martin.grofcik
 */
public interface TaskLogEntryBuilder {

    TaskLogEntryBuilder taskId(String taskId);

    TaskLogEntryBuilder type(String type);

    TaskLogEntryBuilder timeStamp(Date timeStamp);

    TaskLogEntryBuilder userId(String userId);

    TaskLogEntryBuilder processInstanceId(String processInstanceId);

    TaskLogEntryBuilder processDefinitionId(String processDefinitionId);

    TaskLogEntryBuilder scopeId(String scopeId);

    TaskLogEntryBuilder scopeDefinitionId(String scopeDefinitionId);

    TaskLogEntryBuilder subScopeId(String subScopeId);

    TaskLogEntryBuilder scopeType(String scopeType);

    TaskLogEntryBuilder tenantId(String tenantId);

    TaskLogEntryBuilder data(String data);

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

    void add();

}
