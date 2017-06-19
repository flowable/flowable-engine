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
package org.flowable.engine.impl.history.async.json.transformer;

import org.flowable.engine.impl.history.async.HistoryJsonConstants;
import org.flowable.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.HistoricIdentityLinkEntity;
import org.flowable.engine.impl.persistence.entity.HistoricIdentityLinkEntityManager;
import org.flowable.engine.impl.persistence.entity.HistoryJobEntity;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class IdentityLinkCreatedHistoryJsonTransformer extends AbstractHistoryJsonTransformer {

    public static final String TYPE = "identitylink-created";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public boolean isApplicable(ObjectNode historicalData, CommandContext commandContext) {
        return true;
    }

    @Override
    public void transformJson(HistoryJobEntity job, ObjectNode historicalData, CommandContext commandContext) {
        HistoricIdentityLinkEntityManager historicIdentityLinkEntityManager = commandContext.getProcessEngineConfiguration().getHistoricIdentityLinkEntityManager();
        HistoricIdentityLinkEntity historicIdentityLinkEntity = historicIdentityLinkEntityManager.create();
        historicIdentityLinkEntity.setId(getStringFromJson(historicalData, HistoryJsonConstants.ID));
        historicIdentityLinkEntity.setGroupId(getStringFromJson(historicalData, HistoryJsonConstants.GROUP_ID));
        historicIdentityLinkEntity.setProcessInstanceId(getStringFromJson(historicalData, HistoryJsonConstants.PROCESS_INSTANCE_ID));
        historicIdentityLinkEntity.setTaskId(getStringFromJson(historicalData, HistoryJsonConstants.TASK_ID));
        historicIdentityLinkEntity.setType(getStringFromJson(historicalData, HistoryJsonConstants.IDENTITY_LINK_TYPE));
        historicIdentityLinkEntity.setUserId(getStringFromJson(historicalData, HistoryJsonConstants.USER_ID));
        historicIdentityLinkEntityManager.insert(historicIdentityLinkEntity, false);
    }

}
