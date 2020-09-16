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

import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.getStringFromJson;

import java.util.Collections;
import java.util.List;

import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.history.HistoricPlanItemInstanceQueryImpl;
import org.flowable.cmmn.engine.impl.history.async.CmmnAsyncHistoryConstants;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricCaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricCaseInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricPlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricPlanItemInstanceEntityManager;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.service.HistoricTaskService;
import org.flowable.task.service.impl.HistoricTaskInstanceQueryImpl;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskInstanceEntity;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class UpdateCaseDefinitionCascadeHistoryJsonTransformer extends AbstractNeedsHistoricCaseInstanceJsonTransformer {

    public UpdateCaseDefinitionCascadeHistoryJsonTransformer(CmmnEngineConfiguration cmmnEngineConfiguration) {
        super(cmmnEngineConfiguration);
    }
    
    @Override
    public List<String> getTypes() {
        return Collections.singletonList(CmmnAsyncHistoryConstants.TYPE_UPDATE_CASE_DEFINITION_CASCADE);
    }
    
    @Override
    public void transformJson(HistoryJobEntity job, ObjectNode historicalData, CommandContext commandContext) {
        String caseDefinitionId = getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_CASE_DEFINITION_ID);
        String caseInstanceId = getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_CASE_INSTANCE_ID);
        
        HistoricCaseInstanceEntityManager historicCaseInstanceEntityManager = cmmnEngineConfiguration.getHistoricCaseInstanceEntityManager();
        HistoricCaseInstanceEntity historicCaseInstance = historicCaseInstanceEntityManager.findById(caseInstanceId);
        historicCaseInstance.setCaseDefinitionId(caseDefinitionId);
        historicCaseInstanceEntityManager.update(historicCaseInstance);

        HistoricTaskService historicTaskService = cmmnEngineConfiguration.getTaskServiceConfiguration().getHistoricTaskService();
        HistoricTaskInstanceQueryImpl taskQuery = new HistoricTaskInstanceQueryImpl();
        taskQuery.caseInstanceId(caseInstanceId);
        List<HistoricTaskInstance> historicTasks = historicTaskService.findHistoricTaskInstancesByQueryCriteria(taskQuery);
        if (historicTasks != null) {
            for (HistoricTaskInstance historicTaskInstance : historicTasks) {
                HistoricTaskInstanceEntity taskEntity = (HistoricTaskInstanceEntity) historicTaskInstance;
                taskEntity.setScopeDefinitionId(caseDefinitionId);
                historicTaskService.updateHistoricTask(taskEntity, true);
            }
        }

        // because of upgrade runtimeActivity instances can be only subset of historicActivity instances
        HistoricPlanItemInstanceQueryImpl historicPlanItemQuery = new HistoricPlanItemInstanceQueryImpl();
        historicPlanItemQuery.planItemInstanceCaseInstanceId(caseInstanceId);
        HistoricPlanItemInstanceEntityManager historicPlanItemInstanceEntityManager = cmmnEngineConfiguration.getHistoricPlanItemInstanceEntityManager();
        List<HistoricPlanItemInstance> historicPlanItems = historicPlanItemInstanceEntityManager.findByCriteria(historicPlanItemQuery);
        if (historicPlanItems != null) {
            for (HistoricPlanItemInstance historicPlanItemInstance : historicPlanItems) {
                HistoricPlanItemInstanceEntity planItemEntity = (HistoricPlanItemInstanceEntity) historicPlanItemInstance;
                planItemEntity.setCaseDefinitionId(caseDefinitionId);
                historicPlanItemInstanceEntityManager.update(planItemEntity);
            }
        }
    }

}
