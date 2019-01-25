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
import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.getDoubleFromJson;
import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.getIntegerFromJson;
import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.getLongFromJson;
import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.getStringFromJson;

import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.history.async.HistoryJsonConstants;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;
import org.flowable.variable.api.types.VariableType;
import org.flowable.variable.api.types.VariableTypes;
import org.flowable.variable.service.HistoricVariableService;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInstanceEntity;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class VariableCreatedHistoryJsonTransformer extends AbstractHistoryJsonTransformer {

    @Override
    public List<String> getTypes() {
        return Collections.singletonList(HistoryJsonConstants.TYPE_VARIABLE_CREATED);
    }

    @Override
    public boolean isApplicable(ObjectNode historicalData, CommandContext commandContext) {
        return true;
    }

    @Override
    public void transformJson(HistoryJobEntity job, ObjectNode historicalData, CommandContext commandContext) {
        HistoricVariableService historicVariableService = CommandContextUtil.getHistoricVariableService();
        HistoricVariableInstanceEntity historicVariableInstanceEntity = historicVariableService.createHistoricVariableInstance();
        historicVariableInstanceEntity.setId(getStringFromJson(historicalData, HistoryJsonConstants.ID));
        historicVariableInstanceEntity.setProcessInstanceId(getStringFromJson(historicalData, HistoryJsonConstants.PROCESS_INSTANCE_ID));
        historicVariableInstanceEntity.setExecutionId(getStringFromJson(historicalData, HistoryJsonConstants.EXECUTION_ID));
        historicVariableInstanceEntity.setTaskId(getStringFromJson(historicalData, HistoryJsonConstants.TASK_ID));
        historicVariableInstanceEntity.setRevision(getIntegerFromJson(historicalData, HistoryJsonConstants.REVISION));
        historicVariableInstanceEntity.setName(getStringFromJson(historicalData, HistoryJsonConstants.NAME));
        historicVariableInstanceEntity.setScopeId(getStringFromJson(historicalData, HistoryJsonConstants.SCOPE_ID));
        historicVariableInstanceEntity.setSubScopeId(getStringFromJson(historicalData, HistoryJsonConstants.SUB_SCOPE_ID));
        historicVariableInstanceEntity.setScopeType(getStringFromJson(historicalData, HistoryJsonConstants.SCOPE_TYPE));
        
        VariableTypes variableTypes = CommandContextUtil.getProcessEngineConfiguration().getVariableTypes();
        VariableType variableType = variableTypes.getVariableType(getStringFromJson(historicalData, HistoryJsonConstants.VARIABLE_TYPE));
        
        historicVariableInstanceEntity.setVariableType(variableType);

        historicVariableInstanceEntity.setTextValue(getStringFromJson(historicalData, HistoryJsonConstants.VARIABLE_TEXT_VALUE));
        historicVariableInstanceEntity.setTextValue2(getStringFromJson(historicalData, HistoryJsonConstants.VARIABLE_TEXT_VALUE2));
        historicVariableInstanceEntity.setDoubleValue(getDoubleFromJson(historicalData, HistoryJsonConstants.VARIABLE_DOUBLE_VALUE));
        historicVariableInstanceEntity.setLongValue(getLongFromJson(historicalData, HistoryJsonConstants.VARIABLE_LONG_VALUE));
        
        String variableBytes = getStringFromJson(historicalData, HistoryJsonConstants.VARIABLE_BYTES_VALUE);
        if (StringUtils.isNotEmpty(variableBytes)) {
            historicVariableInstanceEntity.setBytes(Base64.getDecoder().decode(variableBytes));
        }
        
        Date time = getDateFromJson(historicalData, HistoryJsonConstants.CREATE_TIME);
        historicVariableInstanceEntity.setCreateTime(time);
        historicVariableInstanceEntity.setLastUpdatedTime(time);

        historicVariableService.insertHistoricVariableInstance(historicVariableInstanceEntity);
    }

}
