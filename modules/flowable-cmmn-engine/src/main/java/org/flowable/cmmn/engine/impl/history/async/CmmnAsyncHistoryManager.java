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
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.history.CmmnHistoryManager;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricCaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricCaseInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.MilestoneInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionUtil;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.scope.ScopeTypes;
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
            addCaseDefinitionFields(data, caseDefinition);
        }
    }
    
    @Override
    public void recordHistoricCaseInstanceDeleted(String caseInstanceId) {
        // Can only be done after the case instance has been fully ended (see DeleteHistoricCaseInstanceCmd)
        if (cmmnEngineConfiguration.getHistoryLevel() != HistoryLevel.NONE) {
            Map<String, String> data = new HashMap<>();
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_ID, caseInstanceId);
            
            HistoricCaseInstanceEntityManager historicCaseInstanceEntityManager = cmmnEngineConfiguration.getHistoricCaseInstanceEntityManager();
            HistoricCaseInstanceEntity historicCaseInstanceEntity = historicCaseInstanceEntityManager.findById(caseInstanceId);
            if (historicCaseInstanceEntity != null) {
                putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_ID, historicCaseInstanceEntity.getId());
                putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_NAME, historicCaseInstanceEntity.getName());
                putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_STATE, historicCaseInstanceEntity.getState());
                putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_BUSINESS_KEY, historicCaseInstanceEntity.getBusinessKey());
                putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_PARENT_ID, historicCaseInstanceEntity.getParentId());
                putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CASE_DEFINITION_ID, historicCaseInstanceEntity.getCaseDefinitionId());
                putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_START_USER_ID, historicCaseInstanceEntity.getStartUserId());
                putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_START_TIME, historicCaseInstanceEntity.getStartTime());
                putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_TENANT_ID, historicCaseInstanceEntity.getTenantId());
                
                if (historicCaseInstanceEntity.getCaseDefinitionId() != null) {
                    addCaseDefinitionFields(data, CaseDefinitionUtil.getCaseDefinition(historicCaseInstanceEntity.getCaseDefinitionId()));
                }
            }
            
            getAsyncHistorySession().addHistoricData(CmmnAsyncHistoryConstants.TYPE_HISTORIC_CASE_INSTANCE_DELETED, data, 
                    historicCaseInstanceEntity != null ? historicCaseInstanceEntity.getTenantId() : null);
        }
    }

    protected void addCaseDefinitionFields(Map<String, String> data, CaseDefinition caseDefinition) {
        if (caseDefinition != null) {
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CASE_DEFINITION_CATEGORY, caseDefinition.getCategory());
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CASE_DEFINITION_DEPLOYMENT_ID, caseDefinition.getDeploymentId());
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CASE_DEFINITION_DESCRIPTION, caseDefinition.getDescription());
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CASE_DEFINITION_KEY, caseDefinition.getKey());
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CASE_DEFINITION_NAME, caseDefinition.getName());
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CASE_DEFINITION_VERSION, caseDefinition.getVersion());
        }
    }
    
    @Override
    public void recordMilestoneReached(MilestoneInstanceEntity milestoneInstanceEntity) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            Map<String, String> data = new HashMap<>();
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_ID, milestoneInstanceEntity.getId());
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_NAME, milestoneInstanceEntity.getName());
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CASE_INSTANCE_ID, milestoneInstanceEntity.getCaseInstanceId());
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CASE_DEFINITION_ID, milestoneInstanceEntity.getCaseDefinitionId());
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_ELEMENT_ID, milestoneInstanceEntity.getElementId());
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CREATE_TIME, milestoneInstanceEntity.getTimeStamp());
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_TENANT_ID, milestoneInstanceEntity.getTenantId());
            
            if (milestoneInstanceEntity.getCaseDefinitionId() != null) {
                addCaseDefinitionFields(data, CaseDefinitionUtil.getCaseDefinition(milestoneInstanceEntity.getCaseDefinitionId()));
            }
            
            getAsyncHistorySession().addHistoricData(CmmnAsyncHistoryConstants.TYPE_MILESTONE_REACHED, data, milestoneInstanceEntity.getTenantId());
        }
    }

    @Override
    public void recordIdentityLinkCreated(IdentityLinkEntity identityLink) {
        if (cmmnEngineConfiguration.getHistoryLevel() != HistoryLevel.NONE 
                && (identityLink.getScopeId() != null || identityLink.getTaskId() != null)) {
            
            Map<String, String> data = new HashMap<>();
            addCommonIdentityLinkFields(identityLink, data);
            
            CaseDefinition caseDefinition = getCaseDefinition(identityLink);
            if (caseDefinition != null) {
                addCaseDefinitionFields(data, caseDefinition);
            }
            
            getAsyncHistorySession().addHistoricData(CmmnAsyncHistoryConstants.TYPE_IDENTITY_LINK_CREATED, data,
                    caseDefinition != null ? caseDefinition.getTenantId() : null);
        }
    }

    @Override
    public void recordIdentityLinkDeleted(IdentityLinkEntity identityLink) {
        if (cmmnEngineConfiguration.getHistoryLevel() != HistoryLevel.NONE) {
            
            Map<String, String> data = new HashMap<>();
            addCommonIdentityLinkFields(identityLink, data);
            
            CaseDefinition caseDefinition = getCaseDefinition(identityLink);;
            if (caseDefinition != null) {
                addCaseDefinitionFields(data, caseDefinition);
            }
            
            getAsyncHistorySession().addHistoricData(CmmnAsyncHistoryConstants.TYPE_IDENTITY_LINK_DELETED, data,
                    caseDefinition != null ? caseDefinition.getTenantId() : null);
        }
    }
    
    protected void addCommonIdentityLinkFields(IdentityLinkEntity identityLink, Map<String, String> data) {
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_ID, identityLink.getId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_GROUP_ID, identityLink.getGroupId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_SCOPE_DEFINITION_ID, identityLink.getScopeDefinitionId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_SCOPE_ID, identityLink.getScopeId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CASE_INSTANCE_ID, identityLink.getScopeId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_SCOPE_TYPE, identityLink.getScopeType());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CASE_DEFINITION_ID, identityLink.getScopeType());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_TASK_ID, identityLink.getTaskId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_IDENTITY_LINK_TYPE, identityLink.getType());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_USER_ID, identityLink.getUserId());
    }
    
    protected CaseDefinition getCaseDefinition(IdentityLinkEntity identityLink) {
        String caseDefinitionId = null;
        if (ScopeTypes.CMMN.equals(identityLink.getScopeType()) && identityLink.getScopeId() != null) {
            CaseInstance caseInstance = CommandContextUtil.getCaseInstanceEntityManager().findById(identityLink.getScopeId());
            if (caseInstance != null) {
                caseDefinitionId = caseInstance.getCaseDefinitionId();
            }
            
        } else if (identityLink.getTaskId() != null) {
            TaskEntity task = CommandContextUtil.getTaskService().getTask(identityLink.getTaskId());
            if (task != null && ScopeTypes.CMMN.equals(task.getScopeType())) {
                caseDefinitionId = task.getScopeDefinitionId();
            }
        }
        return CaseDefinitionUtil.getCaseDefinition(caseDefinitionId);
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
