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

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.history.async.HistoryJsonConstants;
import org.flowable.engine.impl.persistence.entity.HistoricProcessInstanceEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class ProcessInstancePropertyChangedHistoryJsonTransformer extends AbstractNeedsProcessInstanceHistoryJsonTransformer {

    public static final String PROPERTY_NAME = "name";
    public static final String PROPERTY_BUSINESS_KEY = "businessKey";

    @Override
    public String getType() {
        return HistoryJsonConstants.TYPE_PROCESS_INSTANCE_PROPERTY_CHANGED;
    }

    @Override
    public void transformJson(HistoryJobEntity job, ObjectNode historicalData, CommandContext commandContext) {
        String processInstanceId = getStringFromJson(historicalData, HistoryJsonConstants.PROCESS_INSTANCE_ID);
        String property = getStringFromJson(historicalData, HistoryJsonConstants.PROPERTY);
        if (StringUtils.isNotEmpty(processInstanceId) && StringUtils.isNotEmpty(property)) {
            HistoricProcessInstanceEntity historicProcessInstance = CommandContextUtil.getHistoricProcessInstanceEntityManager(commandContext).findById(processInstanceId);

            if (PROPERTY_NAME.equals(property)) {
                historicProcessInstance.setName(getStringFromJson(historicalData, HistoryJsonConstants.NAME));

            } else if (PROPERTY_BUSINESS_KEY.equals(property)) {
                historicProcessInstance.setBusinessKey(getStringFromJson(historicalData, HistoryJsonConstants.BUSINESS_KEY));
            }
        }
    }

}
