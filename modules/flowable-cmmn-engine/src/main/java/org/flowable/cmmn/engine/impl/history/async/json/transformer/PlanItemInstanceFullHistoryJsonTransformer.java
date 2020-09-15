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

import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.getBooleanFromJson;
import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.getDateFromJson;
import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.getStringFromJson;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.history.async.CmmnAsyncHistoryConstants;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricPlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricPlanItemInstanceEntityManager;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Filip Hrisafov
 */
public class PlanItemInstanceFullHistoryJsonTransformer extends AbstractPlanItemInstanceHistoryJsonTransformer {
    
    public PlanItemInstanceFullHistoryJsonTransformer(CmmnEngineConfiguration cmmnEngineConfiguration) {
        super(cmmnEngineConfiguration);
    }
    
    @Override
    public List<String> getTypes() {
        return Collections.singletonList(CmmnAsyncHistoryConstants.TYPE_PLAN_ITEM_INSTANCE_FULL);
    }

    @Override
    public boolean isApplicable(ObjectNode historicalData, CommandContext commandContext) {
        return true;
    }

    @Override
    public void transformJson(HistoryJobEntity job, ObjectNode historicalData, CommandContext commandContext) {
        HistoricPlanItemInstanceEntity historicPlanItemInstanceEntity = getHistoricPlanItemInstanceEntity(historicalData, commandContext);
        if (historicPlanItemInstanceEntity == null) {
            HistoricPlanItemInstanceEntityManager historicPlanItemInstanceEntityManager = cmmnEngineConfiguration.getHistoricPlanItemInstanceEntityManager();
            historicPlanItemInstanceEntity = historicPlanItemInstanceEntityManager.create();
            copyCommonPlanItemInstanceProperties(historicPlanItemInstanceEntity, historicalData);
            historicPlanItemInstanceEntityManager.insert(historicPlanItemInstanceEntity);
            
        } else {
            // If there is already a historic plan item instance it means that the last update time must not be null
            Date lastUpdateTime = getDateFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_LAST_UPDATE_TIME);
            if (lastUpdateTime != null && (historicPlanItemInstanceEntity.getLastUpdatedTime() == null
                            || lastUpdateTime.after(historicPlanItemInstanceEntity.getLastUpdatedTime())
                            || lastUpdateTime.equals(historicPlanItemInstanceEntity.getLastUpdatedTime()))) { // last in wins in case of ties
                
                copyCommonPlanItemInstanceProperties(historicPlanItemInstanceEntity, historicalData);
                cmmnEngineConfiguration.getHistoricPlanItemInstanceEntityManager().update(historicPlanItemInstanceEntity);
            }
        }
    }

    @Override
    protected void copyCommonPlanItemInstanceProperties(HistoricPlanItemInstanceEntity historicPlanItemInstanceEntity, ObjectNode historicalData) {
        super.copyCommonPlanItemInstanceProperties(historicPlanItemInstanceEntity, historicalData);

        historicPlanItemInstanceEntity.setLastAvailableTime(getDateFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_LAST_AVAILABLE_TIME));
        historicPlanItemInstanceEntity.setLastUnavailableTime(getDateFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_LAST_UNAVAILABLE_TIME));
        historicPlanItemInstanceEntity.setLastEnabledTime(getDateFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_LAST_ENABLED_TIME));
        historicPlanItemInstanceEntity.setLastDisabledTime(getDateFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_LAST_DISABLED_TIME));
        historicPlanItemInstanceEntity.setLastStartedTime(getDateFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_LAST_STARTED_TIME));
        historicPlanItemInstanceEntity.setLastSuspendedTime(getDateFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_LAST_SUSPENDED_TIME));
        historicPlanItemInstanceEntity.setCompletedTime(getDateFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_COMPLETED_TIME));
        historicPlanItemInstanceEntity.setOccurredTime(getDateFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_OCCURRED_TIME));
        historicPlanItemInstanceEntity.setTerminatedTime(getDateFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_TERMINATED_TIME));
        historicPlanItemInstanceEntity.setExitTime(getDateFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_EXIT_TIME));
        historicPlanItemInstanceEntity.setEndedTime(getDateFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_END_TIME));
        historicPlanItemInstanceEntity.setExtraValue(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_EXTRA_VALUE));
        
        if (historicalData.has(CmmnAsyncHistoryConstants.FIELD_IS_SHOW_IN_OVERVIEW)) {
            historicPlanItemInstanceEntity.setShowInOverview(getBooleanFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_IS_SHOW_IN_OVERVIEW));
        }
    }

    protected HistoricPlanItemInstanceEntity getHistoricPlanItemInstanceEntity(ObjectNode historicalData, CommandContext commandContext) {
        return cmmnEngineConfiguration.getHistoricPlanItemInstanceEntityManager()
            .findById(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_ID));
    }
}
