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

import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.getBooleanFromJson;
import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.getDateFromJson;
import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.getStringFromJson;

/**
 * @author Joram Barrez
 */
public abstract class AbstractPlanItemInstanceHistoryJsonTransformer extends AbstractHistoryJsonTransformer {
    
    protected void copyCommonPlanItemInstanceProperties(HistoricPlanItemInstanceEntity historicPlanItemInstanceEntity, ObjectNode historicalData) {
        historicPlanItemInstanceEntity.setId(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_ID));
        historicPlanItemInstanceEntity.setName(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_NAME));
        historicPlanItemInstanceEntity.setState(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_STATE));
        historicPlanItemInstanceEntity.setCaseDefinitionId(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_CASE_DEFINITION_ID));
        historicPlanItemInstanceEntity.setCaseInstanceId(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_CASE_INSTANCE_ID));
        historicPlanItemInstanceEntity.setStageInstanceId(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_STAGE_INSTANCE_ID));
        historicPlanItemInstanceEntity.setStage(getBooleanFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_IS_STAGE));
        historicPlanItemInstanceEntity.setElementId(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_ELEMENT_ID));
        historicPlanItemInstanceEntity.setPlanItemDefinitionId(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_PLAN_DEFINITION_ID));
        historicPlanItemInstanceEntity.setPlanItemDefinitionType(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_PLAN_DEFINITION_TYPE));
        historicPlanItemInstanceEntity.setStartUserId(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_START_USER_ID));
        historicPlanItemInstanceEntity.setReferenceId(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_REFERENCE_ID));
        historicPlanItemInstanceEntity.setReferenceType(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_REFERENCE_TYPE));
        historicPlanItemInstanceEntity.setTenantId(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_TENANT_ID));
        historicPlanItemInstanceEntity.setCreatedTime(getDateFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_CREATE_TIME));
        historicPlanItemInstanceEntity.setLastUpdatedTime(getDateFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_LAST_UPDATE_TIME));
    }
    
}
