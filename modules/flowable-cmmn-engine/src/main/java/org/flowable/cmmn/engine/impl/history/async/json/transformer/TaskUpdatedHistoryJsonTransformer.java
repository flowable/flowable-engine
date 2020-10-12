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

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.history.async.CmmnAsyncHistoryConstants;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.service.impl.history.async.AsyncHistorySession;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskInstanceEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Joram Barrez
 */
public class TaskUpdatedHistoryJsonTransformer extends AbstractTaskHistoryJsonTransformer {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskUpdatedHistoryJsonTransformer.class);
    
    public TaskUpdatedHistoryJsonTransformer(CmmnEngineConfiguration cmmnEngineConfiguration) {
        super(cmmnEngineConfiguration);
    }

    @Override
    public List<String> getTypes() {
        return Collections.singletonList(CmmnAsyncHistoryConstants.TYPE_TASK_UPDATED);
    }
    
    @Override
    public boolean isApplicable(ObjectNode historicalData, CommandContext commandContext) {
        return getHistoricTaskEntity(historicalData, commandContext) != null;
    }

    @Override
    public void transformJson(HistoryJobEntity job, ObjectNode historicalData, CommandContext commandContext) {
        HistoricTaskInstanceEntity historicTaskInstance = getHistoricTaskEntity(historicalData, commandContext);

        Date lastUpdateTime = getDateFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_LAST_UPDATE_TIME);
        if (lastUpdateTime == null) {
            // this is for backwards compatibility for jobs that don't have the last update time set
            lastUpdateTime = getDateFromJson(historicalData, AsyncHistorySession.TIMESTAMP);
        }
        if (historicTaskInstance.getLastUpdateTime() == null || !historicTaskInstance.getLastUpdateTime().after(lastUpdateTime)) {
           copyCommonHistoricTaskInstanceFields(historicalData, historicTaskInstance);
        
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("History job (id={}) has expired and will be ignored.", job.getId());
            }
        }
    }

}
