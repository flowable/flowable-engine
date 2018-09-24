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
package org.flowable.cmmn.engine.impl.history.async.json.transformer;

import java.util.Date;

import org.flowable.cmmn.engine.impl.history.async.CmmnAsyncHistoryConstants;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.service.impl.history.async.AsyncHistorySession;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;
import org.flowable.task.service.HistoricTaskService;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskInstanceEntity;

import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.getDateFromJson;
import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.getStringFromJson;

public class TaskEndedHistoryJsonTransformer extends AbstractTaskHistoryJsonTransformer {
    
    @Override
    public String getType() {
        return CmmnAsyncHistoryConstants.TYPE_TASK_REMOVED;
    }
    
    @Override
    public boolean isApplicable(ObjectNode historicalData, CommandContext commandContext) {
        return getHistoricTaskEntity(historicalData, commandContext) != null;
    }
    
    @Override
    public void transformJson(HistoryJobEntity job, ObjectNode historicalData, CommandContext commandContext) {
        HistoricTaskInstanceEntity historicTaskInstance = getHistoricTaskEntity(historicalData, commandContext);
        
        if (historicTaskInstance != null) {
            Date lastUpdateTime = getDateFromJson(historicalData, AsyncHistorySession.TIMESTAMP);
            if (historicTaskInstance.getLastUpdateTime() == null || !historicTaskInstance.getLastUpdateTime().after(lastUpdateTime)) {
                historicTaskInstance.setLastUpdateTime(lastUpdateTime);
                copyCommonHistoricTaskInstanceFields(historicalData, historicTaskInstance);
            }
            setEndProperties(historicalData, historicTaskInstance);
            
        } else {
            HistoricTaskService historicTaskService = CommandContextUtil.getHistoricTaskService(commandContext);
            HistoricTaskInstanceEntity historicTaskInstanceEntity = historicTaskService.createHistoricTask();
            copyCommonHistoricTaskInstanceFields(historicalData, historicTaskInstanceEntity);
            setEndProperties(historicalData, historicTaskInstance);
            historicTaskService.insertHistoricTask(historicTaskInstanceEntity, false);
        }
    }

    protected void setEndProperties(ObjectNode historicalData, HistoricTaskInstanceEntity historicTaskInstance) {
        Date endTime = getDateFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_END_TIME);
        historicTaskInstance.setEndTime(endTime);
        historicTaskInstance.setDeleteReason(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_DELETE_REASON));
   
        Date startTime = historicTaskInstance.getStartTime();
        if (startTime != null && endTime != null) {
            historicTaskInstance.setDurationInMillis(endTime.getTime() - startTime.getTime());
        }
    }

}
