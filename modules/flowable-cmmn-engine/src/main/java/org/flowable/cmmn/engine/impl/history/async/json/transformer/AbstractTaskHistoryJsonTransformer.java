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
import org.flowable.task.service.impl.persistence.entity.HistoricTaskInstanceEntity;

import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.getDateFromJson;
import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.getIntegerFromJson;
import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.getStringFromJson;

/**
 * @author Joram Barrez
 */
public abstract class AbstractTaskHistoryJsonTransformer extends AbstractHistoryJsonTransformer {
    
    protected HistoricTaskInstanceEntity getHistoricTaskEntity(ObjectNode historicalData, CommandContext commandContext) {
        return CommandContextUtil.getHistoricTaskService(commandContext)
                .getHistoricTask(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_ID));
    }

    protected void copyCommonHistoricTaskInstanceFields(ObjectNode historicalData,HistoricTaskInstanceEntity historicTaskInstanceEntity) {
        historicTaskInstanceEntity.setId(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_ID));
        historicTaskInstanceEntity.setTaskDefinitionId(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_TASK_DEFINITION_ID));
        historicTaskInstanceEntity.setTaskDefinitionKey(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_TASK_DEFINITION_KEY));
        historicTaskInstanceEntity.setScopeId(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_SCOPE_ID));
        historicTaskInstanceEntity.setSubScopeId(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_SUB_SCOPE_ID));
        historicTaskInstanceEntity.setScopeType(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_SCOPE_TYPE));
        historicTaskInstanceEntity.setScopeDefinitionId(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_SCOPE_DEFINITION_ID));
        historicTaskInstanceEntity.setName(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_NAME));
        historicTaskInstanceEntity.setParentTaskId(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_PARENT_TASK_ID));
        historicTaskInstanceEntity.setDescription(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_DESCRIPTION));
        historicTaskInstanceEntity.setOwner(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_OWNER));
        historicTaskInstanceEntity.setAssignee(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_ASSIGNEE));
        historicTaskInstanceEntity.setStartTime(getDateFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_START_TIME));
        historicTaskInstanceEntity.setFormKey(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_FORM_KEY));
        historicTaskInstanceEntity.setPriority(getIntegerFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_PRIORITY));
        historicTaskInstanceEntity.setDueDate(getDateFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_DUE_DATE));
        historicTaskInstanceEntity.setCategory(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_CATEGORY));
        historicTaskInstanceEntity.setTenantId(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_TENANT_ID));
        historicTaskInstanceEntity.setLastUpdateTime(getDateFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_LAST_UPDATE_TIME));
    }

}
