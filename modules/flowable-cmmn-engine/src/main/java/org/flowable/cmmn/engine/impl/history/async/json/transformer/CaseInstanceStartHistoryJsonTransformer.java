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
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricCaseInstanceEntityManager;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;

import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.getDateFromJson;
import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.getStringFromJson;

/**
 * @author Joram Barrez
 */
public class CaseInstanceStartHistoryJsonTransformer extends AbstractHistoryJsonTransformer {

    @Override
    public String getType() {
        return CmmnAsyncHistoryConstants.TYPE_CASE_INSTANCE_START;
    }

    @Override
    public boolean isApplicable(ObjectNode historicalData, CommandContext commandContext) {
        return true;
    }

    @Override
    public void transformJson(HistoryJobEntity job, ObjectNode historicalData, CommandContext commandContext) {
           HistoricCaseInstanceEntityManager historicCaseInstanceEntityManager = CommandContextUtil.getHistoricCaseInstanceEntityManager(commandContext);
           
           String id = getStringFromJson(historicalData, "id");
           HistoricCaseInstanceEntity historicCaseInstanceEntity = historicCaseInstanceEntityManager.findById(id);

           // If the entity is already persisted, no need to do anything. 
           // The end event already has inserted the latest state and there is no new data in the json here.
           if (historicCaseInstanceEntity == null) {
               historicCaseInstanceEntity = historicCaseInstanceEntityManager.create();
               historicCaseInstanceEntity.setId(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_ID));
               historicCaseInstanceEntity.setName(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_NAME));
               historicCaseInstanceEntity.setBusinessKey(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_BUSINESS_KEY));
               historicCaseInstanceEntity.setParentId(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_PARENT_ID));
               historicCaseInstanceEntity.setCaseDefinitionId(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_CASE_DEFINITION_ID));
               historicCaseInstanceEntity.setState(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_STATE));
               historicCaseInstanceEntity.setStartUserId(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_START_USER_ID));
               historicCaseInstanceEntity.setStartTime(getDateFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_START_TIME));
               historicCaseInstanceEntity.setTenantId(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_TENANT_ID));
               historicCaseInstanceEntityManager.insert(historicCaseInstanceEntity);
           }
    }

}
