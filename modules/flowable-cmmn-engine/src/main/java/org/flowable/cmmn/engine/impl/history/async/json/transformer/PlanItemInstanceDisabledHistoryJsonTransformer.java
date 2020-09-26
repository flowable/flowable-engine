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

import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.getDateFromJson;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.history.async.CmmnAsyncHistoryConstants;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricPlanItemInstanceEntity;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Joram Barrez
 * @deprecated
 */
@Deprecated
public class PlanItemInstanceDisabledHistoryJsonTransformer extends AbstractNeedsHistoricPlanItemInstanceHistoryJsonTransformer {
    
    public PlanItemInstanceDisabledHistoryJsonTransformer(CmmnEngineConfiguration cmmnEngineConfiguration) {
        super(cmmnEngineConfiguration);
    }
    
    @Override
    public List<String> getTypes() {
        return Collections.singletonList(CmmnAsyncHistoryConstants.TYPE_PLAN_ITEM_INSTANCE_DISABLED);
    }

    @Override
    public void transformJson(HistoryJobEntity job, ObjectNode historicalData, CommandContext commandContext) {
        HistoricPlanItemInstanceEntity historicPlanItemInstanceEntity = updateCommonProperties(historicalData, commandContext);
        
        Date lastDisabledTime = getDateFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_LAST_DISABLED_TIME);
        if (historicPlanItemInstanceEntity.getLastDisabledTime() == null 
                || historicPlanItemInstanceEntity.getLastDisabledTime().before(lastDisabledTime)) {
            historicPlanItemInstanceEntity.setLastDisabledTime(lastDisabledTime);
        }
    }

}
