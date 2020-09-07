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

import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.getDateFromJson;
import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.getIntegerFromJson;
import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.getStringFromJson;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.history.async.HistoryJsonConstants;
import org.flowable.engine.impl.persistence.entity.HistoricActivityInstanceEntity;
import org.flowable.engine.impl.persistence.entity.HistoricActivityInstanceEntityManager;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class ActivityFullHistoryJsonTransformer extends AbstractHistoryJsonTransformer {

    public ActivityFullHistoryJsonTransformer(ProcessEngineConfigurationImpl processEngineConfiguration) {
        super(processEngineConfiguration);
    }
    
    @Override
    public List<String> getTypes() {
        return Collections.singletonList(HistoryJsonConstants.TYPE_ACTIVITY_FULL);
    }

    @Override
    public boolean isApplicable(ObjectNode historicalData, CommandContext commandContext) {
        return true;
    }

    @Override
    public void transformJson(HistoryJobEntity job, ObjectNode historicalData, CommandContext commandContext) {
        HistoricActivityInstanceEntityManager historicActivityInstanceEntityManager = processEngineConfiguration.getHistoricActivityInstanceEntityManager();
        
        HistoricActivityInstanceEntity historicActivityInstanceEntity = createHistoricActivityInstanceEntity(historicalData, commandContext, historicActivityInstanceEntityManager);

        historicActivityInstanceEntity.setProcessDefinitionId(getStringFromJson(historicalData, HistoryJsonConstants.PROCESS_DEFINITION_ID));
        historicActivityInstanceEntity.setProcessInstanceId(getStringFromJson(historicalData, HistoryJsonConstants.PROCESS_INSTANCE_ID));
        historicActivityInstanceEntity.setExecutionId(getStringFromJson(historicalData, HistoryJsonConstants.EXECUTION_ID));
        historicActivityInstanceEntity.setActivityId(getStringFromJson(historicalData, HistoryJsonConstants.ACTIVITY_ID));
        historicActivityInstanceEntity.setActivityName(getStringFromJson(historicalData, HistoryJsonConstants.ACTIVITY_NAME));
        historicActivityInstanceEntity.setActivityType(getStringFromJson(historicalData, HistoryJsonConstants.ACTIVITY_TYPE));
        historicActivityInstanceEntity.setStartTime(getDateFromJson(historicalData, HistoryJsonConstants.START_TIME));
        historicActivityInstanceEntity.setTenantId(getStringFromJson(historicalData, HistoryJsonConstants.TENANT_ID));
        historicActivityInstanceEntity.setTransactionOrder(getIntegerFromJson(historicalData, HistoryJsonConstants.TRANSACTION_ORDER));
        
        Date endTime = getDateFromJson(historicalData, HistoryJsonConstants.END_TIME);
        historicActivityInstanceEntity.setEndTime(endTime);
        historicActivityInstanceEntity.setDeleteReason(getStringFromJson(historicalData, HistoryJsonConstants.DELETE_REASON));

        Date startTime = historicActivityInstanceEntity.getStartTime();
        if (startTime != null && endTime != null) {
            historicActivityInstanceEntity.setDurationInMillis(endTime.getTime() - startTime.getTime());
        }

        historicActivityInstanceEntityManager.insert(historicActivityInstanceEntity);
        dispatchEvent(commandContext, FlowableEventBuilder.createEntityEvent(
                FlowableEngineEventType.HISTORIC_ACTIVITY_INSTANCE_CREATED, historicActivityInstanceEntity));
        
        dispatchEvent(commandContext, FlowableEventBuilder.createEntityEvent(
                FlowableEngineEventType.HISTORIC_ACTIVITY_INSTANCE_ENDED, historicActivityInstanceEntity));
    }

}
