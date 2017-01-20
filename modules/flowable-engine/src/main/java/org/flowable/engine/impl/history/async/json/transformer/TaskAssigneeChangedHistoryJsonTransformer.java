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

import org.apache.commons.lang3.StringUtils;
import org.flowable.engine.impl.history.async.HistoryJsonConstants;
import org.flowable.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.HistoricActivityInstanceEntity;
import org.flowable.engine.impl.persistence.entity.JobEntity;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class TaskAssigneeChangedHistoryJsonTransformer extends TaskPropertyChangedHistoryJsonTransformer {
  
  public static final String TYPE = "task-assignee-changed";
  
  @Override
  public String getType() {
    return TYPE;
  }
  
  @Override
  public boolean isApplicable(ObjectNode historicalData, CommandContext commandContext) {
    String executionId = getStringFromJson(historicalData, HistoryJsonConstants.EXECUTION_ID);
    if (StringUtils.isNotEmpty(executionId)) {
      return super.isApplicable(historicalData, commandContext) 
          && historicActivityInstanceExistsForData(historicalData, commandContext);
      
    } else {
      return super.isApplicable(historicalData, commandContext);
    }
  }

  @Override
  public void transformJson(JobEntity job, ObjectNode historicalData, CommandContext commandContext) {
    
    super.transformJson(job, historicalData, commandContext);
    
    String executionId = getStringFromJson(historicalData, HistoryJsonConstants.EXECUTION_ID);
    if (StringUtils.isNotEmpty(executionId)) {
      String activityId = getStringFromJson(historicalData, HistoryJsonConstants.ACTIVITY_ID);
      if (StringUtils.isNotEmpty(activityId)) {
        HistoricActivityInstanceEntity historicActivityInstanceEntity = findUnfinishedHistoricActivityInstance(commandContext, executionId, activityId);
        if (historicActivityInstanceEntity != null) {
          String assignee = getStringFromJson(historicalData, HistoryJsonConstants.ASSIGNEE);
          historicActivityInstanceEntity.setAssignee(assignee);
        }
      }
    }
  }

}
