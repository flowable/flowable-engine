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

import org.apache.commons.lang3.StringUtils;
import org.flowable.engine.impl.history.async.HistoryJsonConstants;
import org.flowable.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.HistoricIdentityLinkEntity;
import org.flowable.engine.impl.persistence.entity.HistoricIdentityLinkEntityManager;
import org.flowable.engine.impl.persistence.entity.HistoricTaskInstanceEntity;
import org.flowable.engine.impl.persistence.entity.HistoryJobEntity;
import org.flowable.engine.task.IdentityLinkType;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class TaskPropertyChangedHistoryJsonTransformer extends AbstractNeedsTaskHistoryJsonTransformer {

    public static final String TYPE = "task-property-changed";

    public static final String PROPERTY_ASSIGNEE = "assignee";
    public static final String PROPERTY_CLAIM_TIME = "claimTime";
    public static final String PROPERTY_OWNER = "owner";
    public static final String PROPERTY_NAME = "name";
    public static final String PROPERTY_DESCRIPTION = "description";
    public static final String PROPERTY_DUE_DATE = "dueDate";
    public static final String PROPERTY_PRIORITY = "priority";
    public static final String PROPERTY_CATEGORY = "category";
    public static final String PROPERTY_FORM_KEY = "formKey";
    public static final String PROPERTY_PARENT_TASK_ID = "parentTaskId";
    public static final String PROPERTY_TASK_DEFINITION_KEY = "taskDefinitionKey";
    public static final String PROPERTY_PROCESS_DEFINITION_ID = "processDefinitionId";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void transformJson(HistoryJobEntity job, ObjectNode historicalData, CommandContext commandContext) {
        String taskId = getStringFromJson(historicalData, HistoryJsonConstants.ID);
        String property = getStringFromJson(historicalData, HistoryJsonConstants.PROPERTY);
        if (StringUtils.isNotEmpty(taskId) && StringUtils.isNotEmpty(property)) {
            HistoricTaskInstanceEntity historicTaskInstance = commandContext.getHistoricTaskInstanceEntityManager().findById(taskId);

            if (PROPERTY_ASSIGNEE.equals(property)) {
                historicTaskInstance.setAssignee(getStringFromJson(historicalData, HistoryJsonConstants.ASSIGNEE));

            } else if (PROPERTY_OWNER.equals(property)) {
                String owner = getStringFromJson(historicalData, HistoryJsonConstants.OWNER);
                historicTaskInstance.setOwner(owner);
                HistoricIdentityLinkEntityManager historicIdentityLinkEntityManager = commandContext.getProcessEngineConfiguration().getHistoricIdentityLinkEntityManager();
                HistoricIdentityLinkEntity historicIdentityLinkEntity = historicIdentityLinkEntityManager.create();
                historicIdentityLinkEntity.setTaskId(taskId);
                historicIdentityLinkEntity.setType(IdentityLinkType.OWNER);
                historicIdentityLinkEntity.setUserId(owner);
                historicIdentityLinkEntity.setCreateTime(getDateFromJson(historicalData, HistoryJsonConstants.CREATE_TIME)); 
                historicIdentityLinkEntityManager.insert(historicIdentityLinkEntity, false);

            } else if (PROPERTY_NAME.equals(property)) {
                historicTaskInstance.setName(getStringFromJson(historicalData, HistoryJsonConstants.NAME));

            } else if (PROPERTY_CLAIM_TIME.equals(property)) {
                historicTaskInstance.setClaimTime(getDateFromJson(historicalData, HistoryJsonConstants.CLAIM_TIME));

            } else if (PROPERTY_DESCRIPTION.equals(property)) {
                historicTaskInstance.setDescription(getStringFromJson(historicalData, HistoryJsonConstants.DESCRIPTION));

            } else if (PROPERTY_DUE_DATE.equals(property)) {
                historicTaskInstance.setDueDate(getDateFromJson(historicalData, HistoryJsonConstants.DUE_DATE));

            } else if (PROPERTY_PRIORITY.equals(property)) {
                historicTaskInstance.setPriority(getIntegerFromJson(historicalData, HistoryJsonConstants.PRIORITY));

            } else if (PROPERTY_CATEGORY.equals(property)) {
                historicTaskInstance.setCategory(getStringFromJson(historicalData, HistoryJsonConstants.CATEGORY));

            } else if (PROPERTY_FORM_KEY.equals(property)) {
                historicTaskInstance.setFormKey(getStringFromJson(historicalData, HistoryJsonConstants.FORM_KEY));

            } else if (PROPERTY_PARENT_TASK_ID.equals(property)) {
                historicTaskInstance.setParentTaskId(getStringFromJson(historicalData, HistoryJsonConstants.PARENT_TASK_ID));

            } else if (PROPERTY_TASK_DEFINITION_KEY.equals(property)) {
                historicTaskInstance.setTaskDefinitionKey(getStringFromJson(historicalData, HistoryJsonConstants.TASK_DEFINITION_KEY));

            } else if (PROPERTY_PROCESS_DEFINITION_ID.equals(property)) {
                historicTaskInstance.setProcessDefinitionId(getStringFromJson(historicalData, HistoryJsonConstants.PROCESS_DEFINITION_ID));
            }
        }
    }

}
