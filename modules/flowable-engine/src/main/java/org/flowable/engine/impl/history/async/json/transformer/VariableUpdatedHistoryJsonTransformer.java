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
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.impl.history.async.HistoryJsonConstants;
import org.flowable.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.HistoricVariableInstanceEntity;
import org.flowable.engine.impl.persistence.entity.HistoricVariableInstanceEntityManager;
import org.flowable.engine.impl.persistence.entity.HistoryJobEntity;
import org.flowable.engine.impl.variable.VariableType;
import org.flowable.engine.impl.variable.VariableTypes;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class VariableUpdatedHistoryJsonTransformer extends AbstractHistoryJsonTransformer {

    public static final String TYPE = "variable-updated";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public boolean isApplicable(ObjectNode historicalData, CommandContext commandContext) {
        return commandContext.getHistoricVariableInstanceEntityManager().findById(getStringFromJson(historicalData, HistoryJsonConstants.ID)) != null;
    }

    @Override
    public void transformJson(HistoryJobEntity job, ObjectNode historicalData, CommandContext commandContext) {
        HistoricVariableInstanceEntityManager historicVariableInstanceEntityManager = commandContext.getProcessEngineConfiguration().getHistoricVariableInstanceEntityManager();
        HistoricVariableInstanceEntity historicVariable = historicVariableInstanceEntityManager.findById(getStringFromJson(historicalData, HistoryJsonConstants.ID));
        
        VariableTypes variableTypes = Context.getProcessEngineConfiguration().getVariableTypes();
        VariableType variableType = variableTypes.getVariableType(getStringFromJson(historicalData, HistoryJsonConstants.VARIABLE_TYPE));
        
        historicVariable.setVariableType(variableType);

        historicVariable.setTextValue(getStringFromJson(historicalData, HistoryJsonConstants.VARIABLE_TEXT_VALUE));
        historicVariable.setTextValue2(getStringFromJson(historicalData, HistoryJsonConstants.VARIABLE_TEXT_VALUE2));
        historicVariable.setDoubleValue(getDoubleFromJson(historicalData, HistoryJsonConstants.VARIABLE_DOUBLE_VALUE));
        historicVariable.setLongValue(getLongFromJson(historicalData, HistoryJsonConstants.VARIABLE_LONG_VALUE));
        
        String variableBytes = getStringFromJson(historicalData, HistoryJsonConstants.VARIABLE_BYTES_VALUE);
        if (StringUtils.isNotEmpty(variableBytes)) {
            historicVariable.setBytes(Base64.decodeBase64(variableBytes));
        }
        
        Date time = getDateFromJson(historicalData, HistoryJsonConstants.LAST_UPDATED_TIME);
        historicVariable.setLastUpdatedTime(time);
    }

}
