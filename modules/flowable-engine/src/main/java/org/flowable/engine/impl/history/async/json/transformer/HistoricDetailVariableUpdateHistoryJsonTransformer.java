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

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.impl.history.async.HistoryJsonConstants;
import org.flowable.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.HistoricDetailEntityManager;
import org.flowable.engine.impl.persistence.entity.HistoricDetailVariableInstanceUpdateEntity;
import org.flowable.engine.impl.persistence.entity.HistoryJobEntity;
import org.flowable.engine.impl.persistence.entity.data.HistoricDetailDataManager;
import org.flowable.engine.impl.variable.VariableType;
import org.flowable.engine.impl.variable.VariableTypes;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class HistoricDetailVariableUpdateHistoryJsonTransformer extends AbstractHistoryJsonTransformer {

    public static final String TYPE = "historic-detail-variable-update";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public boolean isApplicable(ObjectNode historicalData, CommandContext commandContext) {
        String activityId = getStringFromJson(historicalData, HistoryJsonConstants.ACTIVITY_ID);
        if (StringUtils.isNotEmpty(activityId)) {
            HistoricActivityInstance activityInstance = findHistoricActivityInstance(commandContext, 
                    getStringFromJson(historicalData, HistoryJsonConstants.SOURCE_EXECUTION_ID), activityId);
            
            if (activityInstance == null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void transformJson(HistoryJobEntity job, ObjectNode historicalData, CommandContext commandContext) {
        HistoricDetailDataManager historicDetailDataManager = commandContext.getProcessEngineConfiguration().getHistoricDetailDataManager();
        HistoricDetailVariableInstanceUpdateEntity historicDetailEntity = historicDetailDataManager.createHistoricDetailVariableInstanceUpdate();
        historicDetailEntity.setProcessInstanceId(getStringFromJson(historicalData, HistoryJsonConstants.PROCESS_INSTANCE_ID));
        historicDetailEntity.setExecutionId(getStringFromJson(historicalData, HistoryJsonConstants.EXECUTION_ID));
        historicDetailEntity.setTaskId(getStringFromJson(historicalData, HistoryJsonConstants.TASK_ID));
        historicDetailEntity.setRevision(getIntegerFromJson(historicalData, HistoryJsonConstants.REVISION));
        historicDetailEntity.setName(getStringFromJson(historicalData, HistoryJsonConstants.NAME));
        
        String activityId = getStringFromJson(historicalData, HistoryJsonConstants.ACTIVITY_ID);
        if (StringUtils.isNotEmpty(activityId)) {
            HistoricActivityInstance activityInstance = findHistoricActivityInstance(commandContext, 
                    getStringFromJson(historicalData, HistoryJsonConstants.SOURCE_EXECUTION_ID), activityId);
            
            if (activityInstance != null) {
                historicDetailEntity.setActivityInstanceId(activityInstance.getId());
            }
        }
        
        VariableTypes variableTypes = Context.getProcessEngineConfiguration().getVariableTypes();
        VariableType variableType = variableTypes.getVariableType(getStringFromJson(historicalData, HistoryJsonConstants.VARIABLE_TYPE));
        
        historicDetailEntity.setVariableType(variableType);

        historicDetailEntity.setTextValue(getStringFromJson(historicalData, HistoryJsonConstants.VARIABLE_TEXT_VALUE));
        historicDetailEntity.setTextValue2(getStringFromJson(historicalData, HistoryJsonConstants.VARIABLE_TEXT_VALUE2));
        historicDetailEntity.setDoubleValue(getDoubleFromJson(historicalData, HistoryJsonConstants.VARIABLE_DOUBLE_VALUE));
        historicDetailEntity.setLongValue(getLongFromJson(historicalData, HistoryJsonConstants.VARIABLE_LONG_VALUE));
        
        String variableBytes = getStringFromJson(historicalData, HistoryJsonConstants.VARIABLE_BYTES_VALUE);
        if (StringUtils.isNotEmpty(variableBytes)) {
            historicDetailEntity.setBytes(Base64.decodeBase64(variableBytes));
        }
        
        Date time = getDateFromJson(historicalData, HistoryJsonConstants.CREATE_TIME);
        historicDetailEntity.setTime(time);
        
        HistoricDetailEntityManager historicDetailEntityManager = commandContext.getProcessEngineConfiguration().getHistoricDetailEntityManager();
        historicDetailEntityManager.insert(historicDetailEntity);
    }

}
