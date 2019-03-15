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

import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.putIfNotNull;

import java.util.Date;

import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricCaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricCaseInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.MilestoneInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.entitylink.service.impl.persistence.entity.EntityLinkEntity;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.history.async.AsyncHistorySession;
import org.flowable.task.api.history.HistoricTaskLogEntryBuilder;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Joram Barrez
 */
public class AsyncCmmnHistoryManager extends AbstractAsyncCmmnHistoryManager {
    
    public AsyncCmmnHistoryManager(CmmnEngineConfiguration cmmnEngineConfiguration) {
        super(cmmnEngineConfiguration);
    }
    
    protected AsyncHistorySession getAsyncHistorySession() {
        return Context.getCommandContext().getSession(AsyncHistorySession.class);
    }
    
    @Override
    public void recordCaseInstanceStart(CaseInstanceEntity caseInstanceEntity) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            ObjectNode data = cmmnEngineConfiguration.getObjectMapper().createObjectNode();
            addCommonCaseInstanceFields(caseInstanceEntity, data);
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), CmmnAsyncHistoryConstants.TYPE_CASE_INSTANCE_START, data, caseInstanceEntity.getTenantId());
        }
    }

    @Override
    public void recordCaseInstanceEnd(CaseInstanceEntity caseInstanceEntity, String state, Date endTime) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            ObjectNode data = cmmnEngineConfiguration.getObjectMapper().createObjectNode();
            addCommonCaseInstanceFields(caseInstanceEntity, data);
            
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_END_TIME, endTime);
            
            if (caseInstanceEntity.getStartTime() != null) {
                putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_DURATION, endTime.getTime() - caseInstanceEntity.getStartTime().getTime());
            }
            
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), CmmnAsyncHistoryConstants.TYPE_CASE_INSTANCE_END, data, caseInstanceEntity.getTenantId());
        }
    }
    
    @Override
    public void recordUpdateCaseInstanceName(CaseInstanceEntity caseInstanceEntity, String name) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            ObjectNode data = cmmnEngineConfiguration.getObjectMapper().createObjectNode();
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_ID, caseInstanceEntity.getId());
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_NAME, caseInstanceEntity.getName());
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_REVISION, caseInstanceEntity.getRevision());
            
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), CmmnAsyncHistoryConstants.TYPE_UPDATE_CASE_INSTANCE_NAME, data, caseInstanceEntity.getTenantId());
        }
    }
    
    @Override
    public void recordHistoricCaseInstanceDeleted(String caseInstanceId) {
        // Can only be done after the case instance has been fully ended (see DeleteHistoricCaseInstanceCmd)
        if (cmmnEngineConfiguration.getHistoryLevel() != HistoryLevel.NONE) {
            ObjectNode data = cmmnEngineConfiguration.getObjectMapper().createObjectNode();
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_ID, caseInstanceId);
            
            HistoricCaseInstanceEntityManager historicCaseInstanceEntityManager = cmmnEngineConfiguration.getHistoricCaseInstanceEntityManager();
            HistoricCaseInstanceEntity historicCaseInstanceEntity = historicCaseInstanceEntityManager.findById(caseInstanceId);
            if (historicCaseInstanceEntity != null) {
                addCommonHistoricCaseInstanceFields(historicCaseInstanceEntity, data);
            }
            
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), CmmnAsyncHistoryConstants.TYPE_HISTORIC_CASE_INSTANCE_DELETED, data, 
                    historicCaseInstanceEntity != null ? historicCaseInstanceEntity.getTenantId() : null);
        }
    }

    @Override
    public void recordMilestoneReached(MilestoneInstanceEntity milestoneInstanceEntity) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            ObjectNode data = cmmnEngineConfiguration.getObjectMapper().createObjectNode();
            addCommonMilestoneInstanceFields(milestoneInstanceEntity, data);

            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), CmmnAsyncHistoryConstants.TYPE_MILESTONE_REACHED, data, milestoneInstanceEntity.getTenantId());
        }
    }

    @Override
    public void recordIdentityLinkCreated(IdentityLinkEntity identityLink) {
        if (cmmnEngineConfiguration.getHistoryLevel() != HistoryLevel.NONE 
                && (identityLink.getScopeId() != null || identityLink.getTaskId() != null)) {
            
            ObjectNode data = cmmnEngineConfiguration.getObjectMapper().createObjectNode();
            addCommonIdentityLinkFields(identityLink, data);
            
            CaseDefinition caseDefinition = getCaseDefinition(identityLink);
            if (caseDefinition != null) {
                addCaseDefinitionFields(data, caseDefinition);
            }
            
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), CmmnAsyncHistoryConstants.TYPE_IDENTITY_LINK_CREATED, data,
                    caseDefinition != null ? caseDefinition.getTenantId() : null);
        }
    }

    @Override
    public void recordIdentityLinkDeleted(IdentityLinkEntity identityLink) {
        if (cmmnEngineConfiguration.getHistoryLevel() != HistoryLevel.NONE) {
            
            ObjectNode data = cmmnEngineConfiguration.getObjectMapper().createObjectNode();
            addCommonIdentityLinkFields(identityLink, data);
            
            CaseDefinition caseDefinition = getCaseDefinition(identityLink);
            if (caseDefinition != null) {
                addCaseDefinitionFields(data, caseDefinition);
            }
            
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), CmmnAsyncHistoryConstants.TYPE_IDENTITY_LINK_DELETED, data,
                    caseDefinition != null ? caseDefinition.getTenantId() : null);
        }
    }
    
    @Override
    public void recordEntityLinkCreated(EntityLinkEntity entityLink) {
        if (cmmnEngineConfiguration.getHistoryLevel() != HistoryLevel.NONE && entityLink.getScopeId() != null) {
            
            ObjectNode data = cmmnEngineConfiguration.getObjectMapper().createObjectNode();
            addCommonEntityLinkFields(entityLink, data);
            
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), CmmnAsyncHistoryConstants.TYPE_ENTITY_LINK_CREATED, data, null);
        }
    }

    @Override
    public void recordEntityLinkDeleted(EntityLinkEntity entityLink) {
        if (cmmnEngineConfiguration.getHistoryLevel() != HistoryLevel.NONE) {
            
            ObjectNode data = cmmnEngineConfiguration.getObjectMapper().createObjectNode();
            addCommonEntityLinkFields(entityLink, data);
            
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), CmmnAsyncHistoryConstants.TYPE_ENTITY_LINK_DELETED, data, null);
        }
    }
    
    @Override
    public void recordVariableCreate(VariableInstanceEntity variable, Date createTime) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
            ObjectNode data = cmmnEngineConfiguration.getObjectMapper().createObjectNode();
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CREATE_TIME, createTime);
            addCommonVariableFields(variable, data, createTime);
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), CmmnAsyncHistoryConstants.TYPE_VARIABLE_CREATED, data);
        }
    }

    @Override
    public void recordVariableUpdate(VariableInstanceEntity variable, Date updateTime) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) { 
            ObjectNode data = cmmnEngineConfiguration.getObjectMapper().createObjectNode();
            addCommonVariableFields(variable, data, updateTime);
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), CmmnAsyncHistoryConstants.TYPE_VARIABLE_UPDATED, data);
        }
    }

    @Override
    public void recordVariableRemoved(VariableInstanceEntity variable) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
            ObjectNode data = cmmnEngineConfiguration.getObjectMapper().createObjectNode();
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_ID, variable.getId());
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_REVISION, variable.getRevision());
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), CmmnAsyncHistoryConstants.TYPE_VARIABLE_REMOVED, data);
        }
    }
    
    @Override
    public void recordTaskCreated(TaskEntity task) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
            ObjectNode data = cmmnEngineConfiguration.getObjectMapper().createObjectNode();
            addCommonTaskFields(task, data);
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), CmmnAsyncHistoryConstants.TYPE_TASK_CREATED, data, task.getTenantId());
        }
    }

    @Override
    public void recordTaskInfoChange(TaskEntity task, Date changeTime) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
            ObjectNode data = cmmnEngineConfiguration.getObjectMapper().createObjectNode();
            addCommonTaskFields(task, data);
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_LAST_UPDATE_TIME, changeTime);
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), CmmnAsyncHistoryConstants.TYPE_TASK_UPDATED, data, task.getTenantId());
        }
    }
    
    @Override
    public void recordTaskEnd(TaskEntity task, String deleteReason, Date endTime) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
            ObjectNode data = cmmnEngineConfiguration.getObjectMapper().createObjectNode();
            addCommonTaskFields(task, data);
            
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_DELETE_REASON, deleteReason);
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_END_TIME, endTime);
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_LAST_UPDATE_TIME, endTime);
            
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), CmmnAsyncHistoryConstants.TYPE_TASK_REMOVED, data, task.getTenantId());
        }
    }
    
    @Override
    public void recordPlanItemInstanceCreated(PlanItemInstanceEntity planItemInstanceEntity) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            ObjectNode data = cmmnEngineConfiguration.getObjectMapper().createObjectNode();
            addCommonPlanItemInstanceFields(planItemInstanceEntity, data);
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), CmmnAsyncHistoryConstants.TYPE_PLAN_ITEM_INSTANCE_CREATED, data, planItemInstanceEntity.getTenantId());
        }
    }

    @Override
    public void recordPlanItemInstanceAvailable(PlanItemInstanceEntity planItemInstanceEntity) {
        updatePlanItemInstanceTimeStamp(planItemInstanceEntity, planItemInstanceEntity.getLastAvailableTime(),
            CmmnAsyncHistoryConstants.TYPE_PLAN_ITEM_INSTANCE_AVAILABLE, CmmnAsyncHistoryConstants.FIELD_LAST_AVAILABLE_TIME);
    }

    @Override
    public void recordPlanItemInstanceEnabled(PlanItemInstanceEntity planItemInstanceEntity) {
        updatePlanItemInstanceTimeStamp(planItemInstanceEntity, planItemInstanceEntity.getLastEnabledTime(),
            CmmnAsyncHistoryConstants.TYPE_PLAN_ITEM_INSTANCE_ENABLED, CmmnAsyncHistoryConstants.FIELD_LAST_ENABLED_TIME);
    }

    @Override
    public void recordPlanItemInstanceDisabled(PlanItemInstanceEntity planItemInstanceEntity) {
        updatePlanItemInstanceTimeStamp(planItemInstanceEntity, planItemInstanceEntity.getLastDisabledTime(),
            CmmnAsyncHistoryConstants.TYPE_PLAN_ITEM_INSTANCE_DISABLED, CmmnAsyncHistoryConstants.FIELD_LAST_DISABLED_TIME);
    }

    @Override
    public void recordPlanItemInstanceStarted(PlanItemInstanceEntity planItemInstanceEntity) {
        updatePlanItemInstanceTimeStamp(planItemInstanceEntity, planItemInstanceEntity.getLastStartedTime(),
            CmmnAsyncHistoryConstants.TYPE_PLAN_ITEM_INSTANCE_STARTED, CmmnAsyncHistoryConstants.FIELD_LAST_STARTED_TIME);
    }

    @Override
    public void recordPlanItemInstanceSuspended(PlanItemInstanceEntity planItemInstanceEntity) {
        updatePlanItemInstanceTimeStamp(planItemInstanceEntity, planItemInstanceEntity.getLastSuspendedTime(),
            CmmnAsyncHistoryConstants.TYPE_PLAN_ITEM_INSTANCE_SUSPENDED, CmmnAsyncHistoryConstants.FIELD_LAST_SUSPENDED_TIME);
    }

    @Override
    public void recordPlanItemInstanceCompleted(PlanItemInstanceEntity planItemInstanceEntity) {
        updatePlanItemInstanceTimeStamp(planItemInstanceEntity, planItemInstanceEntity.getCompletedTime(),
            CmmnAsyncHistoryConstants.TYPE_PLAN_ITEM_INSTANCE_COMPLETED, CmmnAsyncHistoryConstants.FIELD_END_TIME, CmmnAsyncHistoryConstants.FIELD_COMPLETED_TIME);
    }

    @Override
    public void recordPlanItemInstanceOccurred(PlanItemInstanceEntity planItemInstanceEntity) {
        updatePlanItemInstanceTimeStamp(planItemInstanceEntity, planItemInstanceEntity.getOccurredTime(),
            CmmnAsyncHistoryConstants.TYPE_PLAN_ITEM_INSTANCE_OCCURRED, CmmnAsyncHistoryConstants.FIELD_END_TIME, CmmnAsyncHistoryConstants.FIELD_OCCURRED_TIME);
    }

    @Override
    public void recordPlanItemInstanceTerminated(PlanItemInstanceEntity planItemInstanceEntity) {
        updatePlanItemInstanceTimeStamp(planItemInstanceEntity, planItemInstanceEntity.getTerminatedTime(),
            CmmnAsyncHistoryConstants.TYPE_PLAN_ITEM_INSTANCE_TERMINATED, CmmnAsyncHistoryConstants.FIELD_END_TIME, CmmnAsyncHistoryConstants.FIELD_TERMINATED_TIME);
    }

    @Override
    public void recordPlanItemInstanceExit(PlanItemInstanceEntity planItemInstanceEntity) {
        updatePlanItemInstanceTimeStamp(planItemInstanceEntity, planItemInstanceEntity.getExitTime(),
            CmmnAsyncHistoryConstants.TYPE_PLAN_ITEM_INSTANCE_EXIT, CmmnAsyncHistoryConstants.FIELD_END_TIME, CmmnAsyncHistoryConstants.FIELD_EXIT_TIME);
    }

    @Override
    public void recordHistoricUserTaskLogEntry(HistoricTaskLogEntryBuilder taskLogEntryBuilder) {
        if (cmmnEngineConfiguration.getTaskServiceConfiguration().isEnableHistoricTaskLogging()) {
            ObjectNode data = cmmnEngineConfiguration.getObjectMapper().createObjectNode();
            addCommonHistoricTaskLogEntryFields(taskLogEntryBuilder, data);

            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), CmmnAsyncHistoryConstants.TYPE_HISTORIC_USER_TASK_LOG_RECORD, data, taskLogEntryBuilder.getTenantId());
        }
    }

    @Override
    public void deleteHistoricUserTaskLogEntry(long logNumber) {
        if (cmmnEngineConfiguration.getTaskServiceConfiguration().isEnableHistoricTaskLogging()) {
            ObjectNode data = cmmnEngineConfiguration.getObjectMapper().createObjectNode();
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_LOG_ENTRY_LOGNUMBER, logNumber);

            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), CmmnAsyncHistoryConstants.TYPE_HISTORIC_USER_TASK_LOG_DELETE, data);
        }
    }

    protected void updatePlanItemInstanceTimeStamp(PlanItemInstanceEntity planItemInstanceEntity, Date time, String type, String...fields) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            ObjectNode data = cmmnEngineConfiguration.getObjectMapper().createObjectNode();
            addCommonPlanItemInstanceFields(planItemInstanceEntity, data);
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_LAST_UPDATE_TIME, time);
            for (String field : fields) {
                putIfNotNull(data, field, time);
            }
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), type, data, planItemInstanceEntity.getTenantId());
        }
    }
    
    protected JobServiceConfiguration getJobServiceConfiguration() {
        return cmmnEngineConfiguration.getJobServiceConfiguration();
    }

}
