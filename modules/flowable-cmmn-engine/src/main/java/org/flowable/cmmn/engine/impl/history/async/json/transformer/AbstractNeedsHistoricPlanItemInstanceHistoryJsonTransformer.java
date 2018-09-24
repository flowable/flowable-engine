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

import java.util.Date;

import org.flowable.cmmn.engine.impl.history.async.CmmnAsyncHistoryConstants;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricPlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.impl.interceptor.CommandContext;

import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.getDateFromJson;
import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.getStringFromJson;

/**
 * @author Joram Barrez
 */
public abstract class AbstractNeedsHistoricPlanItemInstanceHistoryJsonTransformer extends AbstractPlanItemInstanceHistoryJsonTransformer {
    
    @Override
    public boolean isApplicable(ObjectNode historicalData, CommandContext commandContext) {
        return getHistoricPlanItemInstanceEntity(historicalData, commandContext) != null;
    }

    protected HistoricPlanItemInstanceEntity getHistoricPlanItemInstanceEntity(ObjectNode historicalData, CommandContext commandContext) {
        return CommandContextUtil.getHistoricPlanItemInstanceEntityManager(commandContext)
                .findById(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_ID));
    }
    
    public HistoricPlanItemInstanceEntity updateCommonProperties(ObjectNode historicalData, CommandContext commandContext) {
        HistoricPlanItemInstanceEntity historicPlanItemInstanceEntity = getHistoricPlanItemInstanceEntity(historicalData, commandContext);
        Date lastUpdatedTime = getDateFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_LAST_UPDATE_TIME);
        if (lastUpdatedTime != null 
                && (historicPlanItemInstanceEntity.getLastUpdatedTime() == null || lastUpdatedTime.after(historicPlanItemInstanceEntity.getLastUpdatedTime()))) {
            copyCommonPlanItemInstanceProperties(historicPlanItemInstanceEntity, historicalData);
        }
        return historicPlanItemInstanceEntity;
    }

}
