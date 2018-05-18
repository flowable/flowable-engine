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

import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.convertToBase64;
import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.putIfNotNull;

/**
 * @author Joram Barrez
 */
public class AsyncCmmnHistoryManager implements CmmnHistoryManager {
    
    protected CmmnEngineConfiguration cmmnEngineConfiguration;
    
    public AsyncCmmnHistoryManager(CmmnEngineConfiguration cmmnEngineConfiguration) {
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
    }
    
    protected AsyncHistorySession getAsyncHistorySession() {
        return Context.getCommandContext().getSession(AsyncHistorySession.class);
    }
    
    @Override
    public void recordCaseInstanceStart(CaseInstanceEntity caseInstanceEntity) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            Map<String, String> data = new HashMap<>();
            addCommonCaseInstanceFields(caseInstanceEntity, data);
            getAsyncHistorySession().addHistoricData(CmmnAsyncHistoryConstants.TYPE_CASE_INSTANCE_START, data, caseInstanceEntity.getTenantId());
        }
    }

    @Override
    public void recordCaseInstanceEnd(CaseInstanceEntity caseInstanceEntity, String state) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            Map<String, String> data = new HashMap<>();
            addCommonCaseInstanceFields(caseInstanceEntity, data);
            
            Date endTime = cmmnEngineConfiguration.getClock().getCurrentTime();
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_END_TIME, endTime);
            
            if (caseInstanceEntity.getStartTime() != null) {
                putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_DURATION, endTime.getTime() - caseInstanceEntity.getStartTime().getTime());
            }
            
            getAsyncHistorySession().addHistoricData(CmmnAsyncHistoryConstants.TYPE_CASE_INSTANCE_END, data, caseInstanceEntity.getTenantId());
        }
    }
    
    protected void addCommonCaseInstanceFields(CaseInstanceEntity caseInstanceEntity, Map<String, String> data) {
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
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CASE_DEFINITION_ID, caseDefinition.getId());
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
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
            Map<String, String> data = new HashMap<>();
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CREATE_TIME, cmmnEngineConfiguration.getClock().getCurrentTime());
            addCommonVariableFields(variable, data);
            getAsyncHistorySession().addHistoricData(CmmnAsyncHistoryConstants.TYPE_VARIABLE_CREATED, data);
        }
    }

    @Override
    public void recordVariableUpdate(VariableInstanceEntity variable) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) { 
            Map<String, String> data = new HashMap<>();
            addCommonVariableFields(variable, data);
            getAsyncHistorySession().addHistoricData(CmmnAsyncHistoryConstants.TYPE_VARIABLE_UPDATED, data);
        }
    }

    @Override
    public void recordVariableRemoved(VariableInstanceEntity variable) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
            Map<String, String> data = new HashMap<>();
            addCommonVariableFields(variable, data);
            getAsyncHistorySession().addHistoricData(CmmnAsyncHistoryConstants.TYPE_VARIABLE_REMOVED, data);
        }
    }
    
    protected void addCommonVariableFields(VariableInstanceEntity variable, Map<String, String> data) {
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_ID, variable.getId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_TASK_ID, variable.getTaskId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_REVISION, variable.getRevision());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_NAME, variable.getName());
        
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CASE_INSTANCE_ID, variable.getScopeId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_SCOPE_ID, variable.getScopeId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CASE_INSTANCE_ID, variable.getScopeId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_SUB_SCOPE_ID, variable.getSubScopeId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_PLAN_ITEM_INSTANCE_ID, variable.getSubScopeId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_SCOPE_TYPE, variable.getScopeType());
        
        Date time = cmmnEngineConfiguration.getClock().getCurrentTime();
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_LAST_UPDATE_TIME, time);
        
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_VARIABLE_TYPE, variable.getType().getTypeName());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_VARIABLE_TEXT_VALUE, variable.getTextValue());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_VARIABLE_TEXT_VALUE2, variable.getTextValue2());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_VARIABLE_DOUBLE_VALUE, variable.getDoubleValue());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_VARIABLE_LONG_VALUE, variable.getLongValue());
        if (variable.getByteArrayRef() != null) {
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_VARIABLE_BYTES_VALUE, convertToBase64(variable));
        }
        
        if (variable.getScopeId() != null && ScopeTypes.CMMN.equals(variable.getScopeType())) {
            CaseInstance caseInstance = cmmnEngineConfiguration.getCaseInstanceEntityManager().findById(variable.getScopeId());
            addCaseDefinitionFields(data, CaseDefinitionUtil.getCaseDefinition(caseInstance.getCaseDefinitionId()));
        }
    }

    @Override
    public void recordTaskCreated(TaskEntity task) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
            Map<String, String> data = new HashMap<>();
            addCommonTaskFields(task, data);
            getAsyncHistorySession().addHistoricData(CmmnAsyncHistoryConstants.TYPE_TASK_CREATED, data, task.getTenantId());
        }
    }

    @Override
    public void recordTaskInfoChange(TaskEntity task) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
            Map<String, String> data = new HashMap<>();
            addCommonTaskFields(task, data);
            getAsyncHistorySession().addHistoricData(CmmnAsyncHistoryConstants.TYPE_TASK_UPDATED, data, task.getTenantId());
        }
    }
    
    @Override
    public void recordTaskEnd(TaskEntity task, String deleteReason) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
            Map<String, String> data = new HashMap<>();
            addCommonTaskFields(task, data);
            
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_DELETE_REASON, deleteReason);
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_END_TIME, cmmnEngineConfiguration.getClock().getCurrentTime());
            
            getAsyncHistorySession().addHistoricData(CmmnAsyncHistoryConstants.TYPE_TASK_REMOVED, data, task.getTenantId());
        }
    }
    
    protected void addCommonTaskFields(TaskEntity task, Map<String, String> data) {
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_ID, task.getId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_NAME, task.getName());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_PARENT_TASK_ID, task.getParentTaskId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_DESCRIPTION, task.getDescription());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_OWNER, task.getOwner());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_ASSIGNEE, task.getAssignee());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_START_TIME, cmmnEngineConfiguration.getClock().getCurrentTime());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_TASK_DEFINITION_KEY, task.getTaskDefinitionKey());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_TASK_DEFINITION_ID, task.getTaskDefinitionId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_PRIORITY, task.getPriority());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_DUE_DATE, task.getDueDate());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CATEGORY, task.getCategory());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_FORM_KEY, task.getFormKey());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_TENANT_ID, task.getTenantId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CLAIM_TIME, task.getClaimTime());
        
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CASE_INSTANCE_ID, task.getScopeId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_SCOPE_ID, task.getScopeId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CASE_INSTANCE_ID, task.getScopeId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_SUB_SCOPE_ID, task.getSubScopeId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_PLAN_ITEM_INSTANCE_ID, task.getSubScopeId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_SCOPE_TYPE, task.getScopeType());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_SCOPE_DEFINITION_ID, task.getScopeDefinitionId());
        
        if (task.getScopeDefinitionId() != null) {
            addCaseDefinitionFields(data, CaseDefinitionUtil.getCaseDefinition(task.getScopeDefinitionId()));
        }
    }

    @Override
    public void recordPlanItemInstanceCreated(PlanItemInstanceEntity planItemInstanceEntity) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            Map<String, String> data = new HashMap<>();
            addCommonPlanItemInstanceFields(planItemInstanceEntity, data);
            getAsyncHistorySession().addHistoricData(CmmnAsyncHistoryConstants.TYPE_PLAN_ITEM_INSTANCE_CREATED, data, planItemInstanceEntity.getTenantId());
        }
    }

    @Override
    public void recordPlanItemInstanceAvailable(PlanItemInstanceEntity planItemInstanceEntity) {
        updatePlanItemInstanceTimeStamp(planItemInstanceEntity, CmmnAsyncHistoryConstants.TYPE_PLAN_ITEM_INSTANCE_AVAILABLE, CmmnAsyncHistoryConstants.FIELD_LAST_AVAILABLE_TIME);
    }

    @Override
    public void recordPlanItemInstanceEnabled(PlanItemInstanceEntity planItemInstanceEntity) {
        updatePlanItemInstanceTimeStamp(planItemInstanceEntity, CmmnAsyncHistoryConstants.TYPE_PLAN_ITEM_INSTANCE_ENABLED, CmmnAsyncHistoryConstants.FIELD_LAST_ENABLED_TIME);
    }

    @Override
    public void recordPlanItemInstanceDisabled(PlanItemInstanceEntity planItemInstanceEntity) {
        updatePlanItemInstanceTimeStamp(planItemInstanceEntity, CmmnAsyncHistoryConstants.TYPE_PLAN_ITEM_INSTANCE_DISABLED, CmmnAsyncHistoryConstants.FIELD_LAST_DISABLED_TIME);
    }

    @Override
    public void recordPlanItemInstanceStarted(PlanItemInstanceEntity planItemInstanceEntity) {
        updatePlanItemInstanceTimeStamp(planItemInstanceEntity, CmmnAsyncHistoryConstants.TYPE_PLAN_ITEM_INSTANCE_STARTED, CmmnAsyncHistoryConstants.FIELD_LAST_STARTED_TIME);
    }

    @Override
    public void recordPlanItemInstanceSuspended(PlanItemInstanceEntity planItemInstanceEntity) {
        updatePlanItemInstanceTimeStamp(planItemInstanceEntity, CmmnAsyncHistoryConstants.TYPE_PLAN_ITEM_INSTANCE_SUSPENDED, CmmnAsyncHistoryConstants.FIELD_LAST_SUSPENDED_TIME);      
    }

    @Override
    public void recordPlanItemInstanceCompleted(PlanItemInstanceEntity planItemInstanceEntity) {
        updatePlanItemInstanceTimeStamp(planItemInstanceEntity, CmmnAsyncHistoryConstants.TYPE_PLAN_ITEM_INSTANCE_COMPLETED, 
                CmmnAsyncHistoryConstants.FIELD_END_TIME, CmmnAsyncHistoryConstants.FIELD_COMPLETED_TIME);    
    }

    @Override
    public void recordPlanItemInstanceOccurred(PlanItemInstanceEntity planItemInstanceEntity) {
        updatePlanItemInstanceTimeStamp(planItemInstanceEntity, CmmnAsyncHistoryConstants.TYPE_PLAN_ITEM_INSTANCE_OCCURRED, 
                CmmnAsyncHistoryConstants.FIELD_END_TIME, CmmnAsyncHistoryConstants.FIELD_OCCURRED_TIME);      
    }

    @Override
    public void recordPlanItemInstanceTerminated(PlanItemInstanceEntity planItemInstanceEntity) {
        updatePlanItemInstanceTimeStamp(planItemInstanceEntity, CmmnAsyncHistoryConstants.TYPE_PLAN_ITEM_INSTANCE_TERMINATED, 
                CmmnAsyncHistoryConstants.FIELD_END_TIME, CmmnAsyncHistoryConstants.FIELD_TERMINATED_TIME);      
    }

    @Override
    public void recordPlanItemInstanceExit(PlanItemInstanceEntity planItemInstanceEntity) {
        updatePlanItemInstanceTimeStamp(planItemInstanceEntity, CmmnAsyncHistoryConstants.TYPE_PLAN_ITEM_INSTANCE_EXIT, 
                CmmnAsyncHistoryConstants.FIELD_END_TIME, CmmnAsyncHistoryConstants.FIELD_EXIT_TIME);         
    }
    
    protected void addCommonPlanItemInstanceFields(PlanItemInstanceEntity planItemInstanceEntity, Map<String, String> data) {
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_ID, planItemInstanceEntity.getId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_NAME, planItemInstanceEntity.getName());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_STATE, planItemInstanceEntity.getState());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CASE_DEFINITION_ID, planItemInstanceEntity.getCaseDefinitionId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CASE_INSTANCE_ID, planItemInstanceEntity.getCaseInstanceId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_STAGE_INSTANCE_ID, planItemInstanceEntity.getStageInstanceId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_IS_STAGE, planItemInstanceEntity.isStage());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_ELEMENT_ID, planItemInstanceEntity.getElementId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_PLAN_DEFINITION_ID, planItemInstanceEntity.getPlanItemDefinitionId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_PLAN_DEFINITION_TYPE, planItemInstanceEntity.getPlanItemDefinitionType());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_START_USER_ID, planItemInstanceEntity.getStartUserId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_REFERENCE_ID, planItemInstanceEntity.getReferenceId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_REFERENCE_TYPE, planItemInstanceEntity.getReferenceType());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_TENANT_ID, planItemInstanceEntity.getTenantId());
        putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CREATE_TIME, planItemInstanceEntity.getStartTime());
        
        if (planItemInstanceEntity.getCaseDefinitionId() != null) {
            addCaseDefinitionFields(data, CaseDefinitionUtil.getCaseDefinition(planItemInstanceEntity.getCaseDefinitionId()));
        }
    }
    
    protected void updatePlanItemInstanceTimeStamp(PlanItemInstanceEntity planItemInstanceEntity, String type, String...fields) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            Map<String, String> data = new HashMap<>();
            addCommonPlanItemInstanceFields(planItemInstanceEntity, data);
            Date time = cmmnEngineConfiguration.getClock().getCurrentTime();
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_LAST_UPDATE_TIME, time);
            for (String field : fields) {
                putIfNotNull(data, field, time);
            }
            getAsyncHistorySession().addHistoricData(type, data, planItemInstanceEntity.getTenantId());
        }
    }

}
