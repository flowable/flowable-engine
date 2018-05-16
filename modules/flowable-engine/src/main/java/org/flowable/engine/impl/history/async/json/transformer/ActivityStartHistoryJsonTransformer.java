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
import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.getStringFromJson;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.history.async.HistoryJsonConstants;
import org.flowable.engine.impl.persistence.entity.HistoricActivityInstanceEntity;
import org.flowable.engine.impl.persistence.entity.HistoricActivityInstanceEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class ActivityStartHistoryJsonTransformer extends AbstractHistoryJsonTransformer {

    @Override
    public String getType() {
        return HistoryJsonConstants.TYPE_ACTIVITY_START;
    }

    @Override
    public boolean isApplicable(ObjectNode historicalData, CommandContext commandContext) {
        return true;
    }

    @Override
    public void transformJson(HistoryJobEntity job, ObjectNode historicalData, CommandContext commandContext) {
        HistoricActivityInstanceEntityManager historicActivityInstanceEntityManager = CommandContextUtil.getProcessEngineConfiguration(commandContext).getHistoricActivityInstanceEntityManager();
        
        HistoricActivityInstanceEntity historicActivityInstanceEntity = historicActivityInstanceEntityManager.create();
        historicActivityInstanceEntity.setId(CommandContextUtil.getProcessEngineConfiguration(commandContext).getIdGenerator().getNextId());
        historicActivityInstanceEntity.setProcessDefinitionId(getStringFromJson(historicalData, HistoryJsonConstants.PROCESS_DEFINITION_ID));
        historicActivityInstanceEntity.setProcessInstanceId(getStringFromJson(historicalData, HistoryJsonConstants.PROCESS_INSTANCE_ID));
        historicActivityInstanceEntity.setExecutionId(getStringFromJson(historicalData, HistoryJsonConstants.EXECUTION_ID));
        historicActivityInstanceEntity.setActivityId(getStringFromJson(historicalData, HistoryJsonConstants.ACTIVITY_ID));
        historicActivityInstanceEntity.setActivityName(getStringFromJson(historicalData, HistoryJsonConstants.ACTIVITY_NAME));
        historicActivityInstanceEntity.setActivityType(getStringFromJson(historicalData, HistoryJsonConstants.ACTIVITY_TYPE));
        historicActivityInstanceEntity.setAssignee(getStringFromJson(historicalData, HistoryJsonConstants.ASSIGNEE));
        historicActivityInstanceEntity.setStartTime(getDateFromJson(historicalData, HistoryJsonConstants.START_TIME));
        historicActivityInstanceEntity.setTenantId(getStringFromJson(historicalData, HistoryJsonConstants.TENANT_ID));

        historicActivityInstanceEntityManager.insert(historicActivityInstanceEntity);
        dispatchEvent(commandContext, FlowableEventBuilder.createEntityEvent(
                FlowableEngineEventType.HISTORIC_ACTIVITY_INSTANCE_CREATED, historicActivityInstanceEntity));
    }

}
