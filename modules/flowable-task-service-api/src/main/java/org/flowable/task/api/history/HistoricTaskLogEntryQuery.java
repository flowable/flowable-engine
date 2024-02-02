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

import org.flowable.common.engine.api.query.Query;

/**
 * Allows programmatic querying of {@link HistoricTaskLogEntry}s;
 * 
 * @author martin.grofcik
 */
public interface HistoricTaskLogEntryQuery extends Query<HistoricTaskLogEntryQuery, HistoricTaskLogEntry> {

    HistoricTaskLogEntryQuery taskId(String taskId);

    HistoricTaskLogEntryQuery type(String type);

    HistoricTaskLogEntryQuery userId(String userId);

    HistoricTaskLogEntryQuery processInstanceId(String processInstanceId);

    HistoricTaskLogEntryQuery processDefinitionId(String processDefinitionId);

    HistoricTaskLogEntryQuery scopeId(String scopeId);

    HistoricTaskLogEntryQuery scopeDefinitionId(String scopeDefinitionId);

    HistoricTaskLogEntryQuery caseInstanceId(String caseInstanceId);

    HistoricTaskLogEntryQuery caseDefinitionId(String caseDefinitionId);

    HistoricTaskLogEntryQuery subScopeId(String subScopeId);

    HistoricTaskLogEntryQuery scopeType(String scopeType);

    HistoricTaskLogEntryQuery from(Date fromDate);

    HistoricTaskLogEntryQuery to(Date toDate);

    HistoricTaskLogEntryQuery tenantId(String tenantId);

    HistoricTaskLogEntryQuery fromLogNumber(long fromLogNumber);

    HistoricTaskLogEntryQuery toLogNumber(long toLogNumber);

    HistoricTaskLogEntryQuery orderByLogNumber();

    HistoricTaskLogEntryQuery orderByTimeStamp();
}
