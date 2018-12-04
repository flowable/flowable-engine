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

import org.flowable.common.engine.api.query.Query;

/**
 * Allows programmatic querying of {@link TaskLogEntry}s;
 * 
 * @author martin.grofcik
 */
public interface TaskLogEntryQuery extends Query<TaskLogEntryQuery, TaskLogEntry> {

    TaskLogEntryQuery taskId(String taskId);

    TaskLogEntryQuery type(String type);

    TaskLogEntryQuery userId(String userId);

    TaskLogEntryQuery processInstanceId(String processInstanceId);

    TaskLogEntryQuery scopeId(String scopeId);

    TaskLogEntryQuery subScopeId(String subScopeId);

    TaskLogEntryQuery scopeType(String scopeType);

    TaskLogEntryQuery from(Date fromDate);

    TaskLogEntryQuery to(Date toDate);

    TaskLogEntryQuery tenantId(String tenantId);

    TaskLogEntryQuery fromLogNumber(long fromLogNumber);

    TaskLogEntryQuery toLogNumber(long toLogNumber);

}
