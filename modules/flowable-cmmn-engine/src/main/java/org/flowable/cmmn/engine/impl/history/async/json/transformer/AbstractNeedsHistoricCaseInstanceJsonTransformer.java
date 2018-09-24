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
package org.flowable.cmmn.engine.impl.history.async.json.transformer;

import org.flowable.cmmn.engine.impl.history.async.CmmnAsyncHistoryConstants;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricCaseInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.impl.interceptor.CommandContext;

import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.getStringFromJson;

/**
 * @author Joram Barrez
 */
public abstract class AbstractNeedsHistoricCaseInstanceJsonTransformer extends AbstractHistoryJsonTransformer {

    @Override
    public boolean isApplicable(ObjectNode historicalData, CommandContext commandContext) {
        HistoricCaseInstanceEntity historicCaseInstanceEntity = getHistoricCaseInstanceEntity(historicalData, commandContext);
        return historicCaseInstanceEntity != null;
    }

    protected HistoricCaseInstanceEntity getHistoricCaseInstanceEntity(ObjectNode historicalData, CommandContext commandContext) {
        String id = getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_ID);
        HistoricCaseInstanceEntity historicCaseInstanceEntity = CommandContextUtil.getHistoricCaseInstanceEntityManager(commandContext).findById(id);
        return historicCaseInstanceEntity;
    }

}
