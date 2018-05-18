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
package org.flowable.cmmn.engine.impl.history;

import java.util.Date;
import java.util.function.Consumer;

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricCaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricCaseInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricMilestoneInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricMilestoneInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricPlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricPlanItemInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.MilestoneInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.identitylink.service.HistoricIdentityLinkService;
import org.flowable.identitylink.service.impl.persistence.entity.HistoricIdentityLinkEntity;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

/**
 * @author Joram Barrez
 */
public class DefaultCmmnHistoryManager implements CmmnHistoryManager {

    protected CmmnEngineConfiguration cmmnEngineConfiguration;

    public DefaultCmmnHistoryManager(CmmnEngineConfiguration cmmnEngineConfiguration) {
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
    }

    @Override
    public void recordCaseInstanceStart(CaseInstanceEntity caseInstanceEntity) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricCaseInstanceEntityManager historicCaseInstanceEntityManager = cmmnEngineConfiguration.getHistoricCaseInstanceEntityManager();
            HistoricCaseInstanceEntity historicCaseInstanceEntity = cmmnEngineConfiguration.getHistoricCaseInstanceEntityManager().create();
            historicCaseInstanceEntity.setId(caseInstanceEntity.getId());
            historicCaseInstanceEntity.setName(caseInstanceEntity.getName());
            historicCaseInstanceEntity.setBusinessKey(caseInstanceEntity.getBusinessKey());
            historicCaseInstanceEntity.setParentId(caseInstanceEntity.getParentId());
            historicCaseInstanceEntity.setCaseDefinitionId(caseInstanceEntity.getCaseDefinitionId());
            historicCaseInstanceEntity.setState(caseInstanceEntity.getState());
            historicCaseInstanceEntity.setStartUserId(caseInstanceEntity.getStartUserId());
            historicCaseInstanceEntity.setStartTime(caseInstanceEntity.getStartTime());
            historicCaseInstanceEntity.setTenantId(caseInstanceEntity.getTenantId());
            historicCaseInstanceEntityManager.insert(historicCaseInstanceEntity);
        }
    }

    @Override
    public void recordCaseInstanceEnd(CaseInstanceEntity caseInstanceEntity, String state) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricCaseInstanceEntityManager historicCaseInstanceEntityManager = cmmnEngineConfiguration.getHistoricCaseInstanceEntityManager();
            HistoricCaseInstanceEntity historicCaseInstanceEntity = historicCaseInstanceEntityManager.findById(caseInstanceEntity.getId());
            if (historicCaseInstanceEntity != null) {
                historicCaseInstanceEntity.setEndTime(cmmnEngineConfiguration.getClock().getCurrentTime());
                historicCaseInstanceEntity.setState(state);
            }
        }
    }

    @Override
    public void recordMilestoneReached(MilestoneInstanceEntity milestoneInstance) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricMilestoneInstanceEntityManager historicMilestoneInstanceEntityManager = cmmnEngineConfiguration.getHistoricMilestoneInstanceEntityManager();
            HistoricMilestoneInstanceEntity historicMilestoneInstanceEntity = historicMilestoneInstanceEntityManager.create();
            historicMilestoneInstanceEntity.setId(milestoneInstance.getId());
            historicMilestoneInstanceEntity.setName(milestoneInstance.getName());
            historicMilestoneInstanceEntity.setCaseInstanceId(milestoneInstance.getCaseInstanceId());
            historicMilestoneInstanceEntity.setCaseDefinitionId(milestoneInstance.getCaseDefinitionId());
            historicMilestoneInstanceEntity.setElementId(milestoneInstance.getElementId());
            historicMilestoneInstanceEntity.setTimeStamp(cmmnEngineConfiguration.getClock().getCurrentTime());
            historicMilestoneInstanceEntity.setTenantId(milestoneInstance.getTenantId());
            historicMilestoneInstanceEntityManager.insert(historicMilestoneInstanceEntity);
        }
    }

    @Override
    public void recordHistoricCaseInstanceDeleted(String caseInstanceId) {
        if (cmmnEngineConfiguration.getHistoryLevel() != HistoryLevel.NONE) {
            CmmnHistoryHelper.deleteHistoricCaseInstance(cmmnEngineConfiguration, caseInstanceId);
        }
    }

    @Override
    public void recordIdentityLinkCreated(IdentityLinkEntity identityLink) {
        if (cmmnEngineConfiguration.getHistoryLevel() != HistoryLevel.NONE && (identityLink.getScopeId() != null || identityLink.getTaskId() != null)) {
            HistoricIdentityLinkService historicIdentityLinkService = CommandContextUtil.getHistoricIdentityLinkService();
            HistoricIdentityLinkEntity historicIdentityLinkEntity = historicIdentityLinkService.createHistoricIdentityLink();
            historicIdentityLinkEntity.setId(identityLink.getId());
            historicIdentityLinkEntity.setGroupId(identityLink.getGroupId());
            historicIdentityLinkEntity.setScopeDefinitionId(identityLink.getScopeDefinitionId());
            historicIdentityLinkEntity.setScopeId(identityLink.getScopeId());
            if (identityLink.getScopeId() != null) {
                historicIdentityLinkEntity.setScopeType(ScopeTypes.CMMN);
            }
            historicIdentityLinkEntity.setTaskId(identityLink.getTaskId());
            historicIdentityLinkEntity.setType(identityLink.getType());
            historicIdentityLinkEntity.setUserId(identityLink.getUserId());
            historicIdentityLinkService.insertHistoricIdentityLink(historicIdentityLinkEntity, false);
        }
    }

    @Override
    public void recordIdentityLinkDeleted(IdentityLinkEntity identityLink) {
        if (cmmnEngineConfiguration.getHistoryLevel() != HistoryLevel.NONE) {
            CommandContextUtil.getHistoricIdentityLinkService().deleteHistoricIdentityLink(identityLink.getId());
        }
    }

    @Override
    public void recordVariableCreate(VariableInstanceEntity variable) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
            CommandContextUtil.getHistoricVariableService().createAndInsert(variable);
        }
    }

    @Override
    public void recordVariableUpdate(VariableInstanceEntity variableInstanceEntity) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
            CommandContextUtil.getHistoricVariableService().recordVariableUpdate(variableInstanceEntity);
        }
    }

    @Override
    public void recordVariableRemoved(VariableInstanceEntity variableInstanceEntity) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
            CommandContextUtil.getHistoricVariableService().recordVariableRemoved(variableInstanceEntity);
        }
    }

    @Override
    public void recordTaskCreated(TaskEntity task) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
            CommandContextUtil.getHistoricTaskService().recordTaskCreated(task);
        }
    }

    @Override
    public void recordTaskEnd(TaskEntity task, String deleteReason) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
            CommandContextUtil.getHistoricTaskService().recordTaskEnd(task, deleteReason);
        }
    }

    @Override
    public void recordTaskInfoChange(TaskEntity taskEntity) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
            CommandContextUtil.getHistoricTaskService().recordTaskInfoChange(taskEntity);
        }
    }

    @Override
    public void recordPlanItemInstanceCreated(PlanItemInstanceEntity planItemInstanceEntity) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricPlanItemInstanceEntityManager historicPlanItemInstanceEntityManager = cmmnEngineConfiguration.getHistoricPlanItemInstanceEntityManager();
            HistoricPlanItemInstanceEntity historicPlanItemInstanceEntity = historicPlanItemInstanceEntityManager.create();
            historicPlanItemInstanceEntity.setId(planItemInstanceEntity.getId());
            historicPlanItemInstanceEntity.setName(planItemInstanceEntity.getName());
            historicPlanItemInstanceEntity.setState(planItemInstanceEntity.getState());
            historicPlanItemInstanceEntity.setCaseDefinitionId(planItemInstanceEntity.getCaseDefinitionId());
            historicPlanItemInstanceEntity.setCaseInstanceId(planItemInstanceEntity.getCaseInstanceId());
            historicPlanItemInstanceEntity.setStageInstanceId(planItemInstanceEntity.getStageInstanceId());
            historicPlanItemInstanceEntity.setStage(planItemInstanceEntity.isStage());
            historicPlanItemInstanceEntity.setElementId(planItemInstanceEntity.getElementId());
            historicPlanItemInstanceEntity.setPlanItemDefinitionId(planItemInstanceEntity.getPlanItemDefinitionId());
            historicPlanItemInstanceEntity.setPlanItemDefinitionType(planItemInstanceEntity.getPlanItemDefinitionType());
            historicPlanItemInstanceEntity.setStartUserId(planItemInstanceEntity.getStartUserId());
            historicPlanItemInstanceEntity.setReferenceId(planItemInstanceEntity.getReferenceId());
            historicPlanItemInstanceEntity.setReferenceType(planItemInstanceEntity.getReferenceType());
            historicPlanItemInstanceEntity.setTenantId(planItemInstanceEntity.getTenantId());
            historicPlanItemInstanceEntity.setCreatedTime(planItemInstanceEntity.getStartTime());
            historicPlanItemInstanceEntityManager.insert(historicPlanItemInstanceEntity);
        }
    }

    @Override
    public void recordPlanItemInstanceAvailable(PlanItemInstanceEntity planItemInstanceEntity) {
        recordHistoricPlanItemInstanceEntity(planItemInstanceEntity, h -> h.setLastAvailableTime(cmmnEngineConfiguration.getClock().getCurrentTime()));
    }

    @Override
    public void recordPlanItemInstanceEnabled(PlanItemInstanceEntity planItemInstanceEntity) {
        recordHistoricPlanItemInstanceEntity(planItemInstanceEntity, h -> h.setLastEnabledTime(cmmnEngineConfiguration.getClock().getCurrentTime()));
    }

    @Override
    public void recordPlanItemInstanceDisabled(PlanItemInstanceEntity planItemInstanceEntity) {
        recordHistoricPlanItemInstanceEntity(planItemInstanceEntity, h -> h.setLastDisabledTime(cmmnEngineConfiguration.getClock().getCurrentTime()));
    }

    @Override
    public void recordPlanItemInstanceStarted(PlanItemInstanceEntity planItemInstanceEntity) {
        recordHistoricPlanItemInstanceEntity(planItemInstanceEntity, h -> h.setLastStartedTime(cmmnEngineConfiguration.getClock().getCurrentTime()));
    }

    @Override
    public void recordPlanItemInstanceSuspended(PlanItemInstanceEntity planItemInstanceEntity) {
        recordHistoricPlanItemInstanceEntity(planItemInstanceEntity, h -> h.setLastSuspendedTime(cmmnEngineConfiguration.getClock().getCurrentTime()));
    }

    @Override
    public void recordPlanItemInstanceCompleted(PlanItemInstanceEntity planItemInstanceEntity) {
        recordHistoricPlanItemInstanceEntity(planItemInstanceEntity, h -> {
            Date currentTime = cmmnEngineConfiguration.getClock().getCurrentTime();
            h.setEndedTime(currentTime);
            h.setCompletedTime(currentTime);
        });
    }

    @Override
    public void recordPlanItemInstanceTerminated(PlanItemInstanceEntity planItemInstanceEntity) {
        recordHistoricPlanItemInstanceEntity(planItemInstanceEntity, h -> {
            Date currentTime = cmmnEngineConfiguration.getClock().getCurrentTime();
            h.setEndedTime(currentTime);
            h.setTerminatedTime(currentTime);
        });
    }

    @Override
    public void recordPlanItemInstanceOccurred(PlanItemInstanceEntity planItemInstanceEntity) {
        recordHistoricPlanItemInstanceEntity(planItemInstanceEntity, h -> {
            Date currentTime = cmmnEngineConfiguration.getClock().getCurrentTime();
            h.setEndedTime(currentTime);
            h.setOccurredTime(currentTime);
        });
    }

    @Override
    public void recordPlanItemInstanceExit(PlanItemInstanceEntity planItemInstanceEntity) {
        recordHistoricPlanItemInstanceEntity(planItemInstanceEntity, h -> {
            Date currentTime = cmmnEngineConfiguration.getClock().getCurrentTime();
            h.setEndedTime(currentTime);
            h.setExitTime(currentTime);
        });
    }

    protected void recordHistoricPlanItemInstanceEntity(PlanItemInstanceEntity planItemInstanceEntity, Consumer<HistoricPlanItemInstanceEntity> changes) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricPlanItemInstanceEntityManager historicPlanItemInstanceEntityManager = cmmnEngineConfiguration.getHistoricPlanItemInstanceEntityManager();
            HistoricPlanItemInstanceEntity historicPlanItemInstanceEntity = historicPlanItemInstanceEntityManager.findById(planItemInstanceEntity.getId());
            if (historicPlanItemInstanceEntity != null) {
                historicPlanItemInstanceEntity.setState(planItemInstanceEntity.getState());
                historicPlanItemInstanceEntity.setLastUpdatedTime(cmmnEngineConfiguration.getClock().getCurrentTime());
                changes.accept(historicPlanItemInstanceEntity);
            }
        }
    }
}
