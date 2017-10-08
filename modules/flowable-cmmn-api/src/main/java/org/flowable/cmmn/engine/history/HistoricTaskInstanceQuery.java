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
package org.flowable.cmmn.engine.history;

import java.util.Date;

import org.flowable.task.service.TaskInfoQuery;
import org.flowable.task.service.history.HistoricTaskInstance;

/**
 * @author Joram Barrez
 */
public interface HistoricTaskInstanceQuery extends TaskInfoQuery<HistoricTaskInstanceQuery, HistoricTaskInstance> {
    
    HistoricTaskInstanceQuery caseInstanceId(String caseInstanceId);
    
    HistoricTaskInstanceQuery caseDefinitionId(String caseDefinitionId);
    
    HistoricTaskInstanceQuery planItemInstanceId(String planItemInstanceId);
    
    HistoricTaskInstanceQuery taskDeleteReason(String taskDeleteReason);

    HistoricTaskInstanceQuery taskDeleteReasonLike(String taskDeleteReasonLike);

    HistoricTaskInstanceQuery finished();

    HistoricTaskInstanceQuery unfinished();

    HistoricTaskInstanceQuery taskParentTaskId(String parentTaskId);

    HistoricTaskInstanceQuery taskCompletedOn(Date endDate);

    HistoricTaskInstanceQuery taskCompletedBefore(Date endDate);

    HistoricTaskInstanceQuery taskCompletedAfter(Date endDate);

    HistoricTaskInstanceQuery orderByHistoricTaskInstanceDuration();
    HistoricTaskInstanceQuery orderByHistoricTaskInstanceStartTime();
    HistoricTaskInstanceQuery orderByHistoricTaskInstanceEndTime();
    HistoricTaskInstanceQuery orderByDeleteReason();

}
