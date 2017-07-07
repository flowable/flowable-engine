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

import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.impl.history.async.HistoryJsonConstants;
import org.flowable.engine.impl.persistence.entity.HistoricVariableInstanceEntity;
import org.flowable.engine.impl.persistence.entity.HistoricVariableInstanceEntityManager;
import org.flowable.engine.impl.persistence.entity.HistoryJobEntity;
import org.flowable.engine.impl.util.CommandContextUtil;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class VariableRemovedHistoryJsonTransformer extends AbstractHistoryJsonTransformer {

    @Override
    public String getType() {
        return HistoryJsonConstants.TYPE_VARIABLE_REMOVED;
    }

    @Override
    public boolean isApplicable(ObjectNode historicalData, CommandContext commandContext) {
        return CommandContextUtil.getHistoricVariableInstanceEntityManager(commandContext).findById(getStringFromJson(historicalData, HistoryJsonConstants.ID)) != null;
    }

    @Override
    public void transformJson(HistoryJobEntity job, ObjectNode historicalData, CommandContext commandContext) {
        HistoricVariableInstanceEntityManager historicVariableInstanceEntityManager = CommandContextUtil.getProcessEngineConfiguration(commandContext).getHistoricVariableInstanceEntityManager();
        HistoricVariableInstanceEntity historicVariable = historicVariableInstanceEntityManager.findById(getStringFromJson(historicalData, HistoryJsonConstants.ID));
        
        if (historicVariable != null) {
            historicVariableInstanceEntityManager.delete(historicVariable);
        }
    }

}
