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

import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.getStringFromJson;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.history.async.HistoryJsonConstants;
import org.flowable.engine.impl.persistence.entity.HistoricActivityInstanceEntity;
import org.flowable.engine.impl.persistence.entity.HistoricActivityInstanceEntityManager;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author martin.grofcik
 */
public class ActivityUpdateHistoryJsonTransformer extends AbstractHistoryJsonTransformer {

    public ActivityUpdateHistoryJsonTransformer(ProcessEngineConfigurationImpl processEngineConfiguration) {
        super(processEngineConfiguration);
    }
    
    @Override
    public List<String> getTypes() {
        return Collections.singletonList(HistoryJsonConstants.TYPE_UPDATE_HISTORIC_ACTIVITY_INSTANCE);
    }

    @Override
    public boolean isApplicable(ObjectNode historicalData, CommandContext commandContext) {
        String activityInstanceId = getStringFromJson(historicalData, HistoryJsonConstants.RUNTIME_ACTIVITY_INSTANCE_ID);
        if (StringUtils.isNotEmpty(activityInstanceId)) {
            HistoricActivityInstanceEntity historicActivityInstance = processEngineConfiguration.getHistoricActivityInstanceEntityManager().findById(activityInstanceId);
            if (historicActivityInstance == null) {
                return false;
            }
        }
        return true;

    }

    @Override
    public void transformJson(HistoryJobEntity job, ObjectNode historicalData, CommandContext commandContext) {
        String activityInstanceId = getStringFromJson(historicalData, HistoryJsonConstants.RUNTIME_ACTIVITY_INSTANCE_ID);
        if (StringUtils.isNotEmpty(activityInstanceId)) {
            HistoricActivityInstanceEntityManager historicActivityInstanceEntityManager = processEngineConfiguration.getHistoricActivityInstanceEntityManager();
            HistoricActivityInstanceEntity historicActivityInstance = historicActivityInstanceEntityManager.findById(activityInstanceId);
            if (historicActivityInstance != null) {
                if (historicalData.has(HistoryJsonConstants.TASK_ID)) {
                    String taskId = getStringFromJson(historicalData, HistoryJsonConstants.TASK_ID);
                    historicActivityInstance.setTaskId(taskId);
                }
                
                if (historicalData.has(HistoryJsonConstants.ASSIGNEE)) {
                    String assignee = getStringFromJson(historicalData, HistoryJsonConstants.ASSIGNEE);
                    historicActivityInstance.setAssignee(assignee);
                }
                
                if (historicalData.has(HistoryJsonConstants.CALLED_PROCESS_INSTANCE_ID)) {
                    String calledProcessInstanceId = getStringFromJson(historicalData, HistoryJsonConstants.CALLED_PROCESS_INSTANCE_ID);
                    historicActivityInstance.setCalledProcessInstanceId(calledProcessInstanceId);
                }
            }

        }

    }

}
