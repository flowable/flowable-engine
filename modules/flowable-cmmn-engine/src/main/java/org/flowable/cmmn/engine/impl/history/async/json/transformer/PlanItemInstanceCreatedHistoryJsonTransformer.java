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
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricPlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricPlanItemInstanceEntityManager;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Joram Barrez
 */
public class PlanItemInstanceCreatedHistoryJsonTransformer extends AbstractPlanItemInstanceHistoryJsonTransformer {
    
    @Override
    public boolean isApplicable(ObjectNode historicalData, CommandContext commandContext) {
        return true;
    }

    @Override
    public String getType() {
        return CmmnAsyncHistoryConstants.TYPE_PLAN_ITEM_INSTANCE_CREATED;
    }

    @Override
    public void transformJson(HistoryJobEntity job, ObjectNode historicalData, CommandContext commandContext) {
        HistoricPlanItemInstanceEntityManager historicPlanItemInstanceEntityManager = CommandContextUtil.getHistoricPlanItemInstanceEntityManager();
        HistoricPlanItemInstanceEntity historicPlanItemInstanceEntity = historicPlanItemInstanceEntityManager.create();
        copyCommonPlanItemInstanceProperties(historicPlanItemInstanceEntity, historicalData);
        historicPlanItemInstanceEntityManager.insert(historicPlanItemInstanceEntity);
    }

}
