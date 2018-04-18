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
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.history.async.HistoryJsonConstants;
import org.flowable.engine.impl.persistence.entity.HistoricActivityInstanceEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.identitylink.service.HistoricIdentityLinkService;
import org.flowable.identitylink.service.IdentityLinkType;
import org.flowable.identitylink.service.impl.persistence.entity.HistoricIdentityLinkEntity;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class TaskAssigneeChangedHistoryJsonTransformer extends AbstractNeedsTaskHistoryJsonTransformer {

    @Override
    public String getType() {
        return HistoryJsonConstants.TYPE_TASK_ASSIGNEE_CHANGED;
    }

    @Override
    public boolean isApplicable(ObjectNode historicalData, CommandContext commandContext) {
        String activityAssigneeHandled = getStringFromJson(historicalData, HistoryJsonConstants.ACTIVITY_ASSIGNEE_HANDLED);
        if (activityAssigneeHandled != null && Boolean.valueOf(activityAssigneeHandled)) {
            return super.isApplicable(historicalData, commandContext);
            
        } else {
            String executionId = getStringFromJson(historicalData, HistoryJsonConstants.EXECUTION_ID);
            if (StringUtils.isNotEmpty(executionId)) {
                return super.isApplicable(historicalData, commandContext)
                                && historicActivityInstanceExistsForDataIncludingFinished(historicalData, commandContext);
    
            } else {
                return super.isApplicable(historicalData, commandContext);
            }
        }
    }

    @Override
    public void transformJson(HistoryJobEntity job, ObjectNode historicalData, CommandContext commandContext) {
        String assignee = getStringFromJson(historicalData, HistoryJsonConstants.ASSIGNEE);

        String executionId = getStringFromJson(historicalData, HistoryJsonConstants.EXECUTION_ID);
        String activityId = getStringFromJson(historicalData, HistoryJsonConstants.ACTIVITY_ID);
        if (StringUtils.isNotEmpty(executionId) && StringUtils.isNotEmpty(activityId)) {
            
            String activityAssigneeHandled = getStringFromJson(historicalData, HistoryJsonConstants.ACTIVITY_ASSIGNEE_HANDLED);
            
            if (activityAssigneeHandled == null || !Boolean.valueOf(activityAssigneeHandled)) {
                HistoricActivityInstanceEntity historicActivityInstanceEntity = findHistoricActivityInstance(commandContext, executionId, activityId);
                
                if (historicActivityInstanceEntity == null) {
                    // activity instance not found, ignoring event
                    return;
                }
                
                historicActivityInstanceEntity.setAssignee(assignee);
            }
        }
        
        String taskId = getStringFromJson(historicalData, HistoryJsonConstants.ID);
        if (StringUtils.isNotEmpty(taskId)) {
            HistoricIdentityLinkService historicIdentityLinkService = CommandContextUtil.getHistoricIdentityLinkService();
            HistoricIdentityLinkEntity historicIdentityLinkEntity = historicIdentityLinkService.createHistoricIdentityLink();
            historicIdentityLinkEntity.setTaskId(taskId);
            historicIdentityLinkEntity.setType(IdentityLinkType.ASSIGNEE);
            historicIdentityLinkEntity.setUserId(assignee);
            historicIdentityLinkEntity.setCreateTime(getDateFromJson(historicalData, HistoryJsonConstants.CREATE_TIME)); 
            historicIdentityLinkService.insertHistoricIdentityLink(historicIdentityLinkEntity, false);
        }
    }

}
