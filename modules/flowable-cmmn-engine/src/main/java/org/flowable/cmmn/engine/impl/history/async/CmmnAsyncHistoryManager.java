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
package org.flowable.cmmn.engine.impl.history.async;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.MilestoneInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.history.CmmnHistoryManager;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionUtil;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.job.service.impl.history.async.AsyncHistorySession;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.putIfNotNull;

/**
 * @author Joram Barrez
 */
public class CmmnAsyncHistoryManager implements CmmnHistoryManager {
    
    protected CmmnEngineConfiguration cmmnEngineConfiguration;
    
    public CmmnAsyncHistoryManager(CmmnEngineConfiguration cmmnEngineConfiguration) {
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
    }
    
    protected AsyncHistorySession getAsyncHistorySession() {
        return Context.getCommandContext().getSession(AsyncHistorySession.class);
    }
    
    @Override
    public void recordCaseInstanceStart(CaseInstanceEntity caseInstanceEntity) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            Map<String, String> data = new HashMap<>();
            addCommonCaseInstanceProperties(caseInstanceEntity, data);
            getAsyncHistorySession().addHistoricData(CmmnAsyncHistoryConstants.TYPE_CASE_INSTANCE_START, data, caseInstanceEntity.getTenantId());
        }
    }

    @Override
    public void recordCaseInstanceEnd(CaseInstanceEntity caseInstanceEntity, String state) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            Map<String, String> data = new HashMap<>();
            addCommonCaseInstanceProperties(caseInstanceEntity, data);
            
            Date endTime = cmmnEngineConfiguration.getClock().getCurrentTime();
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_END_TIME, endTime);
            
            if (caseInstanceEntity.getStartTime() != null) {
                putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_DURATION, endTime.getTime() - caseInstanceEntity.getStartTime().getTime());
            }
            
            getAsyncHistorySession().addHistoricData(CmmnAsyncHistoryConstants.TYPE_CASE_INSTANCE_END, data, caseInstanceEntity.getTenantId());
        }
    }
    
    protected void addCommonCaseInstanceProperties(CaseInstanceEntity caseInstanceEntity, Map<String, String> data) {
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_ID, caseInstanceEntity.getId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_NAME, caseInstanceEntity.getName());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_STATE, caseInstanceEntity.getState());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_BUSINESS_KEY, caseInstanceEntity.getBusinessKey());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_PARENT_ID, caseInstanceEntity.getParentId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CASE_DEFINITION_ID, caseInstanceEntity.getCaseDefinitionId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_START_USER_ID, caseInstanceEntity.getStartUserId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_START_TIME, caseInstanceEntity.getStartTime());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_TENANT_ID, caseInstanceEntity.getTenantId());
        
        if (caseInstanceEntity.getCaseDefinitionId() != null) {
            CaseDefinition caseDefinition = CaseDefinitionUtil.getCaseDefinition(caseInstanceEntity.getCaseDefinitionId());
            if (caseDefinition != null) {
                putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CASE_DEFINITION_CATEGORY, caseDefinition.getCategory());
                putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CASE_DEFINITION_DEPLOYMENT_ID, caseDefinition.getDeploymentId());
                putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CASE_DEFINITION_DESCRIPTION, caseDefinition.getDescription());
                putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CASE_DEFINITION_KEY, caseDefinition.getKey());
                putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CASE_DEFINITION_NAME, caseDefinition.getName());
                putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CASE_DEFINITION_VERSION, caseDefinition.getVersion());
            }
        }
    }
    
    @Override
    public void recordMilestoneReached(MilestoneInstance milestoneInstance) {
        
        
    }

    @Override
    public void recordCaseInstanceDeleted(String caseInstanceId) {
        
        
    }

    @Override
    public void recordIdentityLinkCreated(IdentityLinkEntity identityLink) {
        
        
    }

    @Override
    public void recordIdentityLinkDeleted(String identityLinkId) {
        
        
    }

    @Override
    public void recordVariableCreate(VariableInstanceEntity variable) {
        
        
    }

    @Override
    public void recordVariableUpdate(VariableInstanceEntity variable) {
        
        
    }

    @Override
    public void recordVariableRemoved(VariableInstanceEntity variable) {
        
        
    }

    @Override
    public void recordTaskCreated(TaskEntity task) {
        
        
    }

    @Override
    public void recordTaskEnd(TaskEntity task, String deleteReason) {
        
        
    }

    @Override
    public void recordTaskInfoChange(TaskEntity taskEntity) {
        
        
    }

    @Override
    public void recordPlanItemInstanceCreated(PlanItemInstanceEntity planItemInstanceEntity) {
        
        
    }

    @Override
    public void recordPlanItemInstanceAvailable(PlanItemInstanceEntity planItemInstanceEntity) {
        
        
    }

    @Override
    public void recordPlanItemInstanceEnabled(PlanItemInstanceEntity planItemInstanceEntity) {
        
        
    }

    @Override
    public void recordPlanItemInstanceDisabled(PlanItemInstanceEntity planItemInstanceEntity) {
        
        
    }

    @Override
    public void recordPlanItemInstanceStarted(PlanItemInstanceEntity planItemInstanceEntity) {
        
        
    }

    @Override
    public void recordPlanItemInstanceSuspended(PlanItemInstanceEntity planItemInstanceEntity) {
        
        
    }

    @Override
    public void recordPlanItemInstanceCompleted(PlanItemInstanceEntity planItemInstanceEntity) {
        
        
    }

    @Override
    public void recordPlanItemInstanceOccurred(PlanItemInstanceEntity planItemInstanceEntity) {
        
        
    }

    @Override
    public void recordPlanItemInstanceTerminated(PlanItemInstanceEntity planItemInstanceEntity) {
        
        
    }

    @Override
    public void recordPlanItemInstanceExit(PlanItemInstanceEntity planItemInstanceEntity) {
        
        
    }

}
