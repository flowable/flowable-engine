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
package org.flowable.engine.impl.history.async.json.transformer;

import java.util.Date;

import org.flowable.engine.impl.history.async.HistoryJsonConstants;
import org.flowable.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.HistoricTaskInstanceEntity;
import org.flowable.engine.impl.persistence.entity.JobEntity;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class TaskEndedHistoryJsonTransformer extends AbstractNeedsTaskHistoryJsonTransformer {
  
  public static final String TYPE = "task-ended";

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public void transformJson(JobEntity job, ObjectNode historicalData, CommandContext commandContext) {
    String taskId = getStringFromJson(historicalData, HistoryJsonConstants.ID);
    HistoricTaskInstanceEntity historicTaskInstance = commandContext.getHistoricTaskInstanceEntityManager().findById(taskId);
    Date endTime = getDateFromJson(historicalData, HistoryJsonConstants.END_TIME);
    historicTaskInstance.setEndTime(endTime);
    historicTaskInstance.setDeleteReason(getStringFromJson(historicalData, HistoryJsonConstants.DELETE_REASON));
    
    Date startTime = historicTaskInstance.getStartTime();
    if (startTime != null && endTime != null) {
      historicTaskInstance.setDurationInMillis(endTime.getTime() - startTime.getTime());
    }
  }

}
