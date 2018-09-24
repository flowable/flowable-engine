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
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.identitylink.service.HistoricIdentityLinkService;
import org.flowable.identitylink.service.impl.persistence.entity.HistoricIdentityLinkEntity;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;

import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.getStringFromJson;

/**
 * @author Joram Barrez
 */
public class IdentityLinkCreatedHistoryJsonTransformer extends AbstractHistoryJsonTransformer {

    @Override
    public String getType() {
        return CmmnAsyncHistoryConstants.TYPE_IDENTITY_LINK_CREATED;
    }

    @Override
    public boolean isApplicable(ObjectNode historicalData, CommandContext commandContext) {
        return true;
    }

    @Override
    public void transformJson(HistoryJobEntity job, ObjectNode historicalData, CommandContext commandContext) {
        HistoricIdentityLinkService historicIdentityLinkService = CommandContextUtil.getHistoricIdentityLinkService();
        HistoricIdentityLinkEntity historicIdentityLinkEntity = historicIdentityLinkService.createHistoricIdentityLink();
        historicIdentityLinkEntity.setId(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_ID));
        historicIdentityLinkEntity.setGroupId(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_GROUP_ID));
        historicIdentityLinkEntity.setScopeId(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_SCOPE_ID));
        historicIdentityLinkEntity.setScopeType(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_SCOPE_TYPE));
        historicIdentityLinkEntity.setScopeDefinitionId(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_SCOPE_DEFINITION_ID));
        historicIdentityLinkEntity.setTaskId(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_TASK_ID));
        historicIdentityLinkEntity.setType(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_IDENTITY_LINK_TYPE));
        historicIdentityLinkEntity.setUserId(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_USER_ID));
        historicIdentityLinkService.insertHistoricIdentityLink(historicIdentityLinkEntity, false);
    }

}
