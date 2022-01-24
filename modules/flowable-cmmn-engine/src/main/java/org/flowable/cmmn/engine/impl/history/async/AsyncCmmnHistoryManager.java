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

import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.getStringFromJson;
import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.putIfNotNull;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricCaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricCaseInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.MilestoneInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.common.engine.impl.context.Context;
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
        if (getHistoryConfigurationSettings().isHistoryEnabledForCaseInstance(caseInstanceEntity)) {
            ObjectNode data = cmmnEngineConfiguration.getObjectMapper().createObjectNode();
            addCommonCaseInstanceFields(caseInstanceEntity, data);
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), CmmnAsyncHistoryConstants.TYPE_CASE_INSTANCE_START, data, caseInstanceEntity.getTenantId());
        }
    }

    @Override
    public void recordCaseInstanceEnd(CaseInstanceEntity caseInstanceEntity, String state, Date endTime) {
        if (getHistoryConfigurationSettings().isHistoryEnabledForCaseInstance(caseInstanceEntity)) {
            ObjectNode data = cmmnEngineConfiguration.getObjectMapper().createObjectNode();
            addCommonCaseInstanceFields(caseInstanceEntity, data);
            
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_END_TIME, endTime);
            
            if (caseInstanceEntity.getStartTime() != null) {
                putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_DURATION, endTime.getTime() - caseInstanceEntity.getStartTime().getTime());
            }

            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_STATE, state);
            
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), CmmnAsyncHistoryConstants.TYPE_CASE_INSTANCE_END, data, caseInstanceEntity.getTenantId());
        }
    }

    @Override
    public void recordHistoricCaseInstanceReactivated(CaseInstanceEntity caseInstanceEntity) {
        if (getHistoryConfigurationSettings().isHistoryEnabledForCaseInstance(caseInstanceEntity)) {
            ObjectNode data = cmmnEngineConfiguration.getObjectMapper().createObjectNode();
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_ID, caseInstanceEntity.getId());
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_STATE, caseInstanceEntity.getState());
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_LAST_REACTIVATION_TIME, caseInstanceEntity.getLastReactivationTime());
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_LAST_REACTIVATION_USER_ID, caseInstanceEntity.getLastReactivationUserId());

            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), CmmnAsyncHistoryConstants.TYPE_CASE_INSTANCE_REACTIVATE, data, caseInstanceEntity.getTenantId());
        }
    }

    @Override
    public void recordUpdateCaseInstanceName(CaseInstanceEntity caseInstanceEntity, String name) {
        if (getHistoryConfigurationSettings().isHistoryEnabledForCaseInstance(caseInstanceEntity)) {
            ObjectNode data = cmmnEngineConfiguration.getObjectMapper().createObjectNode();
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_ID, caseInstanceEntity.getId());
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_NAME, caseInstanceEntity.getName());
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_REVISION, caseInstanceEntity.getRevision());
            
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), CmmnAsyncHistoryConstants.TYPE_UPDATE_CASE_INSTANCE_NAME, data, caseInstanceEntity.getTenantId());
        }
    }

    @Override
    public void recordUpdateBusinessKey(CaseInstanceEntity caseInstanceEntity, String businessKey) {
        if (getHistoryConfigurationSettings().isHistoryEnabledForCaseInstance(caseInstanceEntity)) {
            ObjectNode data = cmmnEngineConfiguration.getObjectMapper().createObjectNode();
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_ID, caseInstanceEntity.getId());
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_BUSINESS_KEY, caseInstanceEntity.getBusinessKey());
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_REVISION, caseInstanceEntity.getRevision());

            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), CmmnAsyncHistoryConstants.TYPE_UPDATE_CASE_INSTANCE_BUSINESS_KEY, data,
                caseInstanceEntity.getTenantId());
        }
    }
    
    @Override
    public void recordUpdateBusinessStatus(CaseInstanceEntity caseInstanceEntity, String businessKey) {
        if (caseInstanceEntity != null) {
            if (getHistoryConfigurationSettings().isHistoryEnabledForCaseInstance(caseInstanceEntity)) {
                ObjectNode data = cmmnEngineConfiguration.getObjectMapper().createObjectNode();
                putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_ID, caseInstanceEntity.getId());
                putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_BUSINESS_STATUS, caseInstanceEntity.getBusinessStatus());
                putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_REVISION, caseInstanceEntity.getRevision());

                getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), CmmnAsyncHistoryConstants.TYPE_UPDATE_CASE_INSTANCE_BUSINESS_STATUS,
                        data,
                        caseInstanceEntity.getTenantId());
            }
        }
    }

    @Override
    public void recordHistoricCaseInstanceDeleted(String caseInstanceId, String tenantId) {
        // Can only be done after the case instance has been fully ended (see DeleteHistoricCaseInstanceCmd)
        if (getHistoryConfigurationSettings().isHistoryEnabled()) {
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
        if (getHistoryConfigurationSettings().isHistoryEnabledForMilestone(milestoneInstanceEntity)) {
            ObjectNode data = cmmnEngineConfiguration.getObjectMapper().createObjectNode();
            addCommonMilestoneInstanceFields(milestoneInstanceEntity, data);

            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), CmmnAsyncHistoryConstants.TYPE_MILESTONE_REACHED, data, milestoneInstanceEntity.getTenantId());
        }
    }

    @Override
    public void recordIdentityLinkCreated(IdentityLinkEntity identityLink) {
        if (getHistoryConfigurationSettings().isHistoryEnabledForIdentityLink(identityLink)
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
        if (getHistoryConfigurationSettings().isHistoryEnabledForIdentityLink(identityLink)) {
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
        if (getHistoryConfigurationSettings().isHistoryEnabledForEntityLink(entityLink) && entityLink.getScopeId() != null) {
            ObjectNode data = cmmnEngineConfiguration.getObjectMapper().createObjectNode();
            addCommonEntityLinkFields(entityLink, data);
            
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), CmmnAsyncHistoryConstants.TYPE_ENTITY_LINK_CREATED, data, null);
        }
    }

    @Override
    public void recordEntityLinkDeleted(EntityLinkEntity entityLink) {
        if (getHistoryConfigurationSettings().isHistoryEnabledForEntityLink(entityLink)) {
            ObjectNode data = cmmnEngineConfiguration.getObjectMapper().createObjectNode();
            addCommonEntityLinkFields(entityLink, data);
            
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), CmmnAsyncHistoryConstants.TYPE_ENTITY_LINK_DELETED, data, null);
        }
    }
    
    @Override
    public void recordVariableCreate(VariableInstanceEntity variableInstanceEntity, Date createTime) {
        if (getHistoryConfigurationSettings().isHistoryEnabledForVariableInstance(variableInstanceEntity)) {
            ObjectNode data = cmmnEngineConfiguration.getObjectMapper().createObjectNode();
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CREATE_TIME, createTime);
            addCommonVariableFields(variableInstanceEntity, data, createTime);
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), CmmnAsyncHistoryConstants.TYPE_VARIABLE_CREATED, data);
        }
    }

    @Override
    public void recordVariableUpdate(VariableInstanceEntity variableInstanceEntity, Date updateTime) {
        if (getHistoryConfigurationSettings().isHistoryEnabledForVariableInstance(variableInstanceEntity)) {
            ObjectNode data = cmmnEngineConfiguration.getObjectMapper().createObjectNode();
            addCommonVariableFields(variableInstanceEntity, data, updateTime);
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), CmmnAsyncHistoryConstants.TYPE_VARIABLE_UPDATED, data);
        }
    }

    @Override
    public void recordVariableRemoved(VariableInstanceEntity variableInstanceEntity) {
        if (getHistoryConfigurationSettings().isHistoryEnabledForVariableInstance(variableInstanceEntity)) {
            ObjectNode data = cmmnEngineConfiguration.getObjectMapper().createObjectNode();
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_ID, variableInstanceEntity.getId());
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_REVISION, variableInstanceEntity.getRevision());
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), CmmnAsyncHistoryConstants.TYPE_VARIABLE_REMOVED, data);
        }
    }
    
    @Override
    public void recordTaskCreated(TaskEntity task) {
        if (getHistoryConfigurationSettings().isHistoryEnabledForUserTask(task)) {
            ObjectNode data = cmmnEngineConfiguration.getObjectMapper().createObjectNode();
            addCommonTaskFields(task, data);
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), CmmnAsyncHistoryConstants.TYPE_TASK_CREATED, data, task.getTenantId());
        }
    }

    @Override
    public void recordTaskInfoChange(TaskEntity task, Date changeTime) {
        if (getHistoryConfigurationSettings().isHistoryEnabledForUserTask(task)) {
            ObjectNode data = cmmnEngineConfiguration.getObjectMapper().createObjectNode();
            addCommonTaskFields(task, data);
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_LAST_UPDATE_TIME, changeTime);
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), CmmnAsyncHistoryConstants.TYPE_TASK_UPDATED, data, task.getTenantId());
        }
    }
    
    @Override
    public void recordTaskEnd(TaskEntity task, String deleteReason, Date endTime) {
        if (getHistoryConfigurationSettings().isHistoryEnabledForUserTask(task)) {
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
        recordPlanItemInstanceFull(planItemInstanceEntity, null);
    }

    @Override
    public void recordPlanItemInstanceReactivated(PlanItemInstanceEntity planItemInstanceEntity) {
        recordPlanItemInstanceFull(planItemInstanceEntity, null);
        // TODO: do we need a specific reactivation flag to mark this item being created because of a reactivation?
    }

    @Override
    public void recordPlanItemInstanceUpdated(PlanItemInstanceEntity planItemInstanceEntity) {
        recordPlanItemInstanceFull(planItemInstanceEntity, null);
    }

    @Override
    public void recordPlanItemInstanceAvailable(PlanItemInstanceEntity planItemInstanceEntity) {
        recordPlanItemInstanceFull(planItemInstanceEntity, planItemInstanceEntity.getLastAvailableTime());
    }

    @Override
    public void recordPlanItemInstanceUnavailable(PlanItemInstanceEntity planItemInstanceEntity) {
        recordPlanItemInstanceFull(planItemInstanceEntity, planItemInstanceEntity.getLastUnavailableTime());
    }

    @Override
    public void recordPlanItemInstanceEnabled(PlanItemInstanceEntity planItemInstanceEntity) {
        recordPlanItemInstanceFull(planItemInstanceEntity, planItemInstanceEntity.getLastEnabledTime());
    }

    @Override
    public void recordPlanItemInstanceDisabled(PlanItemInstanceEntity planItemInstanceEntity) {
        recordPlanItemInstanceFull(planItemInstanceEntity, planItemInstanceEntity.getLastDisabledTime());
    }

    @Override
    public void recordPlanItemInstanceStarted(PlanItemInstanceEntity planItemInstanceEntity) {
        recordPlanItemInstanceFull(planItemInstanceEntity, planItemInstanceEntity.getLastStartedTime());
    }

    @Override
    public void recordPlanItemInstanceSuspended(PlanItemInstanceEntity planItemInstanceEntity) {
        recordPlanItemInstanceFull(planItemInstanceEntity, planItemInstanceEntity.getLastSuspendedTime());
    }

    @Override
    public void recordPlanItemInstanceCompleted(PlanItemInstanceEntity planItemInstanceEntity) {
        recordPlanItemInstanceFull(planItemInstanceEntity, planItemInstanceEntity.getCompletedTime());
    }

    @Override
    public void recordPlanItemInstanceOccurred(PlanItemInstanceEntity planItemInstanceEntity) {
        recordPlanItemInstanceFull(planItemInstanceEntity, planItemInstanceEntity.getOccurredTime());
    }

    @Override
    public void recordPlanItemInstanceTerminated(PlanItemInstanceEntity planItemInstanceEntity) {
        recordPlanItemInstanceFull(planItemInstanceEntity, planItemInstanceEntity.getTerminatedTime());
    }

    @Override
    public void recordPlanItemInstanceExit(PlanItemInstanceEntity planItemInstanceEntity) {
        recordPlanItemInstanceFull(planItemInstanceEntity, planItemInstanceEntity.getExitTime());
    }
    
    @Override
    public void updateCaseDefinitionIdInHistory(CaseDefinition caseDefinition, CaseInstanceEntity caseInstance) {
        if (getHistoryConfigurationSettings().isHistoryEnabled(caseDefinition.getId())) {
            ObjectNode data = cmmnEngineConfiguration.getObjectMapper().createObjectNode();
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_ID, caseInstance.getId());
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CASE_DEFINITION_ID, caseDefinition.getId());
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_CASE_INSTANCE_ID, caseInstance.getId());
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), CmmnAsyncHistoryConstants.TYPE_UPDATE_CASE_DEFINITION_CASCADE, data);
        }
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

    protected void recordPlanItemInstanceFull(PlanItemInstanceEntity planItemInstanceEntity, Date lastUpdateTime) {
        if (getHistoryConfigurationSettings().isHistoryEnabledForPlanItemInstance(planItemInstanceEntity)) {
            // When there are multiple changes on a PlanItemInstance within the same transaction
            // we need to use only the last one (that one will contain the latest data)
            removePlanItemInstanceFull(planItemInstanceEntity.getId());

            ObjectNode data = cmmnEngineConfiguration.getObjectMapper().createObjectNode();
            addCommonPlanItemInstanceFields(planItemInstanceEntity, data);
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_LAST_UPDATE_TIME, lastUpdateTime);
            putIfNotNull(data, CmmnAsyncHistoryConstants.FIELD_IS_SHOW_IN_OVERVIEW, evaluateShowInOverview(planItemInstanceEntity));
            
            getAsyncHistorySession().addHistoricData(getJobServiceConfiguration(), CmmnAsyncHistoryConstants.TYPE_PLAN_ITEM_INSTANCE_FULL, data);
        }
    }

    /* Helper methods */

    protected void removePlanItemInstanceFull(String planItemInstanceId) {
        Map<JobServiceConfiguration, AsyncHistorySession.AsyncHistorySessionData> sessionData = getAsyncHistorySession().getSessionData();
        if (sessionData != null) {
            AsyncHistorySession.AsyncHistorySessionData asyncHistorySessionData = sessionData.get(getJobServiceConfiguration());
            if (asyncHistorySessionData != null) {
                Map<String, List<ObjectNode>> jobData = asyncHistorySessionData.getJobData();
                if (jobData != null && jobData.containsKey(CmmnAsyncHistoryConstants.TYPE_PLAN_ITEM_INSTANCE_FULL)) {
                    List<ObjectNode> planItemInstanceDataList = jobData.get(CmmnAsyncHistoryConstants.TYPE_PLAN_ITEM_INSTANCE_FULL);
                    Iterator<ObjectNode> planItemInstanceDataIterator = planItemInstanceDataList.listIterator();
                    while (planItemInstanceDataIterator.hasNext()) {
                        ObjectNode planItemInstanceData = planItemInstanceDataIterator.next();
                        if (planItemInstanceId.equals(getStringFromJson(planItemInstanceData, CmmnAsyncHistoryConstants.FIELD_ID))) {
                            planItemInstanceDataIterator.remove();
                        }
                    }
                }
            }
        }
    }
    
    protected JobServiceConfiguration getJobServiceConfiguration() {
        return cmmnEngineConfiguration.getJobServiceConfiguration();
    }

}
