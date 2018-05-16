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

import java.util.Date;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.history.async.HistoryJsonConstants;
import org.flowable.engine.impl.persistence.entity.HistoricProcessInstanceEntity;
import org.flowable.engine.impl.persistence.entity.HistoricProcessInstanceEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class ProcessInstanceEndHistoryJsonTransformer extends AbstractNeedsProcessInstanceHistoryJsonTransformer {

    @Override
    public String getType() {
        return HistoryJsonConstants.TYPE_PROCESS_INSTANCE_END;
    }
    
    @Override
    public void transformJson(HistoryJobEntity job, ObjectNode historicalData, CommandContext commandContext) {
        HistoricProcessInstanceEntityManager historicProcessInstanceEntityManager = CommandContextUtil.getHistoricProcessInstanceEntityManager(commandContext);

        String processInstanceId = getStringFromJson(historicalData, HistoryJsonConstants.PROCESS_INSTANCE_ID);
        HistoricProcessInstanceEntity historicProcessInstance = historicProcessInstanceEntityManager.findById(processInstanceId);
        
        if (historicProcessInstance != null) {
            historicProcessInstance.setEndActivityId(getStringFromJson(historicalData, HistoryJsonConstants.ACTIVITY_ID));
    
            Date endTime = getDateFromJson(historicalData, HistoryJsonConstants.END_TIME);
            historicProcessInstance.setEndTime(endTime);
            historicProcessInstance.setDeleteReason(getStringFromJson(historicalData, HistoryJsonConstants.DELETE_REASON));
    
            Date startTime = historicProcessInstance.getStartTime();
            if (startTime != null && endTime != null) {
                historicProcessInstance.setDurationInMillis(endTime.getTime() - startTime.getTime());
            }
    
            dispatchEvent(commandContext, FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.HISTORIC_PROCESS_INSTANCE_ENDED, historicProcessInstance));
        
        } else {
            historicProcessInstance = historicProcessInstanceEntityManager.create();
            historicProcessInstance.setId(getStringFromJson(historicalData, HistoryJsonConstants.ID));
            historicProcessInstance.setProcessInstanceId(getStringFromJson(historicalData, HistoryJsonConstants.PROCESS_INSTANCE_ID));
            historicProcessInstance.setBusinessKey(getStringFromJson(historicalData, HistoryJsonConstants.BUSINESS_KEY));
            historicProcessInstance.setProcessDefinitionId(getStringFromJson(historicalData, HistoryJsonConstants.PROCESS_DEFINITION_ID));
            historicProcessInstance.setProcessDefinitionKey(getStringFromJson(historicalData, HistoryJsonConstants.PROCESS_DEFINITION_KEY));
            historicProcessInstance.setProcessDefinitionName(getStringFromJson(historicalData, HistoryJsonConstants.PROCESS_DEFINITION_NAME));
            String versionString = getStringFromJson(historicalData, HistoryJsonConstants.PROCESS_DEFINITION_VERSION);
            historicProcessInstance.setProcessDefinitionVersion(versionString != null ? Integer.valueOf(versionString) : 0);
            historicProcessInstance.setDeploymentId(getStringFromJson(historicalData, HistoryJsonConstants.DEPLOYMENT_ID));
            historicProcessInstance.setStartTime(getDateFromJson(historicalData, HistoryJsonConstants.START_TIME));
            historicProcessInstance.setStartUserId(getStringFromJson(historicalData, HistoryJsonConstants.START_USER_ID));
            historicProcessInstance.setStartActivityId(getStringFromJson(historicalData, HistoryJsonConstants.START_ACTIVITY_ID));
            historicProcessInstance.setSuperProcessInstanceId(getStringFromJson(historicalData, HistoryJsonConstants.SUPER_PROCESS_INSTANCE_ID));
            historicProcessInstance.setCallbackId(getStringFromJson(historicalData, HistoryJsonConstants.CALLBACK_ID));
            historicProcessInstance.setCallbackType(getStringFromJson(historicalData, HistoryJsonConstants.CALLBACK_TYPE));
            historicProcessInstance.setTenantId(getStringFromJson(historicalData, HistoryJsonConstants.TENANT_ID));
    
            historicProcessInstanceEntityManager.insert(historicProcessInstance, false);
    
            dispatchEvent(commandContext, FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.HISTORIC_PROCESS_INSTANCE_CREATED, historicProcessInstance));
            
            historicProcessInstance.setEndActivityId(getStringFromJson(historicalData, HistoryJsonConstants.ACTIVITY_ID));
            
            Date endTime = getDateFromJson(historicalData, HistoryJsonConstants.END_TIME);
            historicProcessInstance.setEndTime(endTime);
            historicProcessInstance.setDeleteReason(getStringFromJson(historicalData, HistoryJsonConstants.DELETE_REASON));
    
            Date startTime = historicProcessInstance.getStartTime();
            if (startTime != null && endTime != null) {
                historicProcessInstance.setDurationInMillis(endTime.getTime() - startTime.getTime());
            }
            
            historicProcessInstanceEntityManager.update(historicProcessInstance, false);
    
            dispatchEvent(commandContext, FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.HISTORIC_PROCESS_INSTANCE_ENDED, historicProcessInstance));
        }
    }
    
}
