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

import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.history.async.HistoryJsonConstants;
import org.flowable.engine.impl.persistence.entity.HistoricActivityInstanceEntity;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class ActivityEndHistoryJsonTransformer extends AbstractNeedsUnfinishedHistoricActivityHistoryJsonTransformer {

    @Override
    public String getType() {
        return HistoryJsonConstants.TYPE_ACTIVITY_END;
    }

    @Override
    public void transformJson(HistoryJobEntity job, ObjectNode historicalData, CommandContext commandContext) {
        String executionId = getStringFromJson(historicalData, HistoryJsonConstants.EXECUTION_ID);
        String activityId = getStringFromJson(historicalData, HistoryJsonConstants.ACTIVITY_ID);
        HistoricActivityInstanceEntity historicActivityInstanceEntity = findUnfinishedHistoricActivityInstance(commandContext, executionId, activityId);
        if (historicActivityInstanceEntity != null) {
            Date endTime = getDateFromJson(historicalData, HistoryJsonConstants.END_TIME);
            historicActivityInstanceEntity.setEndTime(endTime);
            historicActivityInstanceEntity.setDeleteReason(getStringFromJson(historicalData, HistoryJsonConstants.DELETE_REASON));

            Date startTime = historicActivityInstanceEntity.getStartTime();
            if (startTime != null && endTime != null) {
                historicActivityInstanceEntity.setDurationInMillis(endTime.getTime() - startTime.getTime());
            }

            dispatchEvent(commandContext, FlowableEventBuilder.createEntityEvent(
                            FlowableEngineEventType.HISTORIC_ACTIVITY_INSTANCE_ENDED, historicActivityInstanceEntity));
        }
    }

}
