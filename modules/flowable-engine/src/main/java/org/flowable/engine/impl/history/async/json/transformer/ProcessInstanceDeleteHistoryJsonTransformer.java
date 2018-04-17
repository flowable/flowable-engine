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

import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.history.async.HistoryJsonConstants;
import org.flowable.engine.impl.persistence.entity.HistoricProcessInstanceEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class ProcessInstanceDeleteHistoryJsonTransformer extends AbstractProcessInstanceDeleteHistoryTransformer {

    @Override
    public String getType() {
        return HistoryJsonConstants.TYPE_PROCESS_INSTANCE_DELETED;
    }

    @Override
    public boolean isApplicable(ObjectNode historicalData, CommandContext commandContext) {
        String id = getStringFromJson(historicalData, HistoryJsonConstants.PROCESS_INSTANCE_ID);
        HistoricProcessInstanceEntityManager historicProcessInstanceEntityManager = CommandContextUtil.getHistoricProcessInstanceEntityManager(commandContext);
        return historicProcessInstanceEntityManager.findById(id) != null;
    }

    @Override
    public void transformJson(HistoryJobEntity job, ObjectNode historicalData, CommandContext commandContext) {
        String processInstanceId = getStringFromJson(historicalData, HistoryJsonConstants.PROCESS_INSTANCE_ID);
        deleteProcessInstance(processInstanceId, commandContext);
    }
}
