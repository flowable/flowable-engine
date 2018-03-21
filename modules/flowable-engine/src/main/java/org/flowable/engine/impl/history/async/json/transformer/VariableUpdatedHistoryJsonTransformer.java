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

import java.util.Base64;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.impl.history.async.HistoryJsonConstants;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;
import org.flowable.variable.api.types.VariableType;
import org.flowable.variable.api.types.VariableTypes;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInstanceEntity;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class VariableUpdatedHistoryJsonTransformer extends AbstractHistoryJsonTransformer {

    @Override
    public String getType() {
        return HistoryJsonConstants.TYPE_VARIABLE_UPDATED;
    }

    @Override
    public boolean isApplicable(ObjectNode historicalData, CommandContext commandContext) {
        return CommandContextUtil.getHistoricVariableService().getHistoricVariableInstance(getStringFromJson(historicalData, HistoryJsonConstants.ID)) != null;
    }

    @Override
    public void transformJson(HistoryJobEntity job, ObjectNode historicalData, CommandContext commandContext) {
        HistoricVariableInstanceEntity historicVariable = CommandContextUtil.getHistoricVariableService().getHistoricVariableInstance(
                        getStringFromJson(historicalData, HistoryJsonConstants.ID));
        
        Date time = getDateFromJson(historicalData, HistoryJsonConstants.LAST_UPDATED_TIME);
        if (historicVariable.getLastUpdatedTime().after(time)) {
            // If the historic variable already has a later time, we don't need to change its details
            // to something that is already superseded by later data.
            return;
        }
        
        VariableTypes variableTypes = CommandContextUtil.getProcessEngineConfiguration().getVariableTypes();
        VariableType variableType = variableTypes.getVariableType(getStringFromJson(historicalData, HistoryJsonConstants.VARIABLE_TYPE));
        
        historicVariable.setVariableType(variableType);

        historicVariable.setTextValue(getStringFromJson(historicalData, HistoryJsonConstants.VARIABLE_TEXT_VALUE));
        historicVariable.setTextValue2(getStringFromJson(historicalData, HistoryJsonConstants.VARIABLE_TEXT_VALUE2));
        historicVariable.setDoubleValue(getDoubleFromJson(historicalData, HistoryJsonConstants.VARIABLE_DOUBLE_VALUE));
        historicVariable.setLongValue(getLongFromJson(historicalData, HistoryJsonConstants.VARIABLE_LONG_VALUE));
        
        String variableBytes = getStringFromJson(historicalData, HistoryJsonConstants.VARIABLE_BYTES_VALUE);
        if (StringUtils.isNotEmpty(variableBytes)) {
            historicVariable.setBytes(Base64.getDecoder().decode(variableBytes));
        }
        
        historicVariable.setLastUpdatedTime(time);
    }

}
