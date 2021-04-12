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
import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.getStringFromJson;

import java.util.Collections;
import java.util.List;

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.history.async.CmmnAsyncHistoryConstants;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricCaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricCaseInstanceEntityManager;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Resets the end time and reactivates the historic case instance by changing its state.
 *
 * @author Micha Kiener
 */
public class CaseInstanceReactivateHistoryJsonTransformer extends AbstractNeedsHistoricCaseInstanceJsonTransformer {

    public CaseInstanceReactivateHistoryJsonTransformer(CmmnEngineConfiguration cmmnEngineConfiguration) {
        super(cmmnEngineConfiguration);
    }

    @Override
    public List<String> getTypes() {
        return Collections.singletonList(CmmnAsyncHistoryConstants.TYPE_CASE_INSTANCE_REACTIVATE);
    }

    @Override
    public void transformJson(HistoryJobEntity job, ObjectNode historicalData, CommandContext commandContext) {
       HistoricCaseInstanceEntityManager historicCaseInstanceEntityManager = cmmnEngineConfiguration.getHistoricCaseInstanceEntityManager();
       HistoricCaseInstanceEntity historicCaseInstanceEntity = getHistoricCaseInstanceEntity(historicalData, commandContext);

       if (historicCaseInstanceEntity != null) {
           historicCaseInstanceEntity.setState(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_STATE));
           historicCaseInstanceEntity.setLastReactivationTime(getDateFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_LAST_REACTIVATION_TIME));
           historicCaseInstanceEntity.setLastReactivationUserId(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_LAST_REACTIVATION_USER_ID));
           historicCaseInstanceEntity.setEndTime(null);
           historicCaseInstanceEntityManager.update(historicCaseInstanceEntity);
       }
    }
}
