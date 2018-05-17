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
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricMilestoneInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricMilestoneInstanceEntityManager;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;

import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.*;

/**
 * @author Joram Barrez
 */
public class MilestoneReachedHistoryJsonTransformer extends AbstractHistoryJsonTransformer {
    
    @Override
    public boolean isApplicable(ObjectNode historicalData, CommandContext commandContext) {
        return true;
    }

    @Override
    public String getType() {
        return CmmnAsyncHistoryConstants.TYPE_MILESTONE_REACHED;
    }

    @Override
    public void transformJson(HistoryJobEntity job, ObjectNode historicalData, CommandContext commandContext) {
        HistoricMilestoneInstanceEntityManager historicMilestoneInstanceEntityManager = CommandContextUtil.getHistoricMilestoneInstanceEntityManager(commandContext);
        HistoricMilestoneInstanceEntity historicMilestoneInstanceEntity = historicMilestoneInstanceEntityManager.create();
        historicMilestoneInstanceEntity.setId(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_ID));
        historicMilestoneInstanceEntity.setName(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_NAME));
        historicMilestoneInstanceEntity.setCaseInstanceId(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_CASE_INSTANCE_ID));
        historicMilestoneInstanceEntity.setCaseDefinitionId(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_CASE_DEFINITION_ID));
        historicMilestoneInstanceEntity.setElementId(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_ELEMENT_ID));
        historicMilestoneInstanceEntity.setTimeStamp(getDateFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_CREATE_TIME));
        historicMilestoneInstanceEntity.setTenantId(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_TENANT_ID));
        historicMilestoneInstanceEntityManager.insert(historicMilestoneInstanceEntity);
    }

}
