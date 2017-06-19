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
import org.flowable.engine.task.IdentityLinkType;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class TaskOwnerChangedHistoryJsonTransformer extends AbstractNeedsTaskHistoryJsonTransformer {

    public static final String TYPE = "task-owner-changed";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void transformJson(HistoryJobEntity job, ObjectNode historicalData, CommandContext commandContext) {
        String owner = getStringFromJson(historicalData, HistoryJsonConstants.OWNER);
        String taskId = getStringFromJson(historicalData, HistoryJsonConstants.ID);
        
        HistoricIdentityLinkEntityManager historicIdentityLinkEntityManager = commandContext.getProcessEngineConfiguration().getHistoricIdentityLinkEntityManager();
        HistoricIdentityLinkEntity historicIdentityLinkEntity = historicIdentityLinkEntityManager.create();
        historicIdentityLinkEntity.setTaskId(taskId);
        historicIdentityLinkEntity.setType(IdentityLinkType.OWNER);
        historicIdentityLinkEntity.setUserId(owner);
        historicIdentityLinkEntity.setCreateTime(getDateFromJson(historicalData, HistoryJsonConstants.CREATE_TIME)); 
        historicIdentityLinkEntityManager.insert(historicIdentityLinkEntity, false);
    }

}
