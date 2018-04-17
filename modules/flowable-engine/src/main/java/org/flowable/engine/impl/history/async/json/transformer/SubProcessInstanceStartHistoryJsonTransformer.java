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

import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.history.async.HistoryJsonConstants;
import org.flowable.engine.impl.persistence.entity.HistoricActivityInstanceEntity;
import org.flowable.engine.impl.persistence.entity.HistoricProcessInstanceEntity;
import org.flowable.engine.impl.persistence.entity.HistoricProcessInstanceEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class SubProcessInstanceStartHistoryJsonTransformer extends AbstractHistoryJsonTransformer {

    @Override
    public String getType() {
        return HistoryJsonConstants.TYPE_SUBPROCESS_INSTANCE_START;
    }

    @Override
    public boolean isApplicable(ObjectNode historicalData, CommandContext commandContext) {
        String activityId = getStringFromJson(historicalData, HistoryJsonConstants.ACTIVITY_ID);
        HistoricActivityInstance activityInstance = findHistoricActivityInstance(commandContext, 
                getStringFromJson(historicalData, HistoryJsonConstants.EXECUTION_ID), activityId);
            
        if (activityInstance == null) {
            return false;
        }
        
        return true;
    }

    @Override
    public void transformJson(HistoryJobEntity job, ObjectNode historicalData, CommandContext commandContext) {
        HistoricProcessInstanceEntityManager historicProcessInstanceEntityManager = CommandContextUtil.getHistoricProcessInstanceEntityManager(commandContext);

        String id = getStringFromJson(historicalData, HistoryJsonConstants.ID);
        String processInstanceId = getStringFromJson(historicalData, HistoryJsonConstants.PROCESS_INSTANCE_ID);
        HistoricProcessInstanceEntity historicProcessInstance = historicProcessInstanceEntityManager.findById(id);
        if (historicProcessInstance == null) {
            HistoricProcessInstanceEntity subProcessInstance = historicProcessInstanceEntityManager.create();

            subProcessInstance.setId(id);
            subProcessInstance.setProcessInstanceId(processInstanceId);
            subProcessInstance.setBusinessKey(getStringFromJson(historicalData, HistoryJsonConstants.BUSINESS_KEY));
            subProcessInstance.setProcessDefinitionId(getStringFromJson(historicalData, HistoryJsonConstants.PROCESS_DEFINITION_ID));
            subProcessInstance.setProcessDefinitionKey(getStringFromJson(historicalData, HistoryJsonConstants.PROCESS_DEFINITION_KEY));
            subProcessInstance.setProcessDefinitionName(getStringFromJson(historicalData, HistoryJsonConstants.PROCESS_DEFINITION_NAME));
            String versionString = getStringFromJson(historicalData, HistoryJsonConstants.PROCESS_DEFINITION_VERSION);
            subProcessInstance.setProcessDefinitionVersion(versionString != null ? Integer.valueOf(versionString) : 0);
            subProcessInstance.setDeploymentId(getStringFromJson(historicalData, HistoryJsonConstants.DEPLOYMENT_ID));
            subProcessInstance.setStartTime(getDateFromJson(historicalData, HistoryJsonConstants.START_TIME));
            subProcessInstance.setStartUserId(getStringFromJson(historicalData, HistoryJsonConstants.START_USER_ID));
            subProcessInstance.setStartActivityId(getStringFromJson(historicalData, HistoryJsonConstants.START_ACTIVITY_ID));
            subProcessInstance.setSuperProcessInstanceId(getStringFromJson(historicalData, HistoryJsonConstants.SUPER_PROCESS_INSTANCE_ID));
            subProcessInstance.setCallbackId(getStringFromJson(historicalData, HistoryJsonConstants.CALLBACK_ID));
            subProcessInstance.setCallbackType(getStringFromJson(historicalData, HistoryJsonConstants.CALLBACK_TYPE));
            subProcessInstance.setTenantId(getStringFromJson(historicalData, HistoryJsonConstants.TENANT_ID));
        
            historicProcessInstanceEntityManager.insert(subProcessInstance, false);
    
            // Fire event
            dispatchEvent(commandContext, FlowableEventBuilder.createEntityEvent(
                    FlowableEngineEventType.HISTORIC_PROCESS_INSTANCE_CREATED, subProcessInstance));
        }
        
        String executionId = getStringFromJson(historicalData, HistoryJsonConstants.EXECUTION_ID);
        String activityId = getStringFromJson(historicalData, HistoryJsonConstants.ACTIVITY_ID);

        HistoricActivityInstanceEntity activityInstance = findHistoricActivityInstance(commandContext, executionId, activityId);
        if (activityInstance != null) {
            activityInstance.setCalledProcessInstanceId(processInstanceId);
        }
    }

}
