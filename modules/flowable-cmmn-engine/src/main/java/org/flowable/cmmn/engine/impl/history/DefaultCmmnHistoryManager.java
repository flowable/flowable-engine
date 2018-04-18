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

import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.history.HistoricMilestoneInstance;
import org.flowable.cmmn.api.runtime.MilestoneInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricCaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricCaseInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricMilestoneInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricMilestoneInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricPlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricPlanItemInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.identitylink.service.HistoricIdentityLinkService;
import org.flowable.identitylink.service.impl.persistence.entity.HistoricIdentityLinkEntity;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

import java.util.List;

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
    public void recordCaseInstanceEnd(String caseInstanceId, String state) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricCaseInstanceEntityManager historicCaseInstanceEntityManager = cmmnEngineConfiguration.getHistoricCaseInstanceEntityManager();
            HistoricCaseInstanceEntity historicCaseInstanceEntity = historicCaseInstanceEntityManager.findById(caseInstanceId);
            historicCaseInstanceEntity.setEndTime(cmmnEngineConfiguration.getClock().getCurrentTime());
            historicCaseInstanceEntity.setState(state);
        }
    }

    @Override
    public void recordMilestoneReached(MilestoneInstance milestoneInstance) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricMilestoneInstanceEntityManager historicMilestoneInstanceEntityManager = cmmnEngineConfiguration.getHistoricMilestoneInstanceEntityManager();
            HistoricMilestoneInstanceEntity historicMilestoneInstanceEntity = historicMilestoneInstanceEntityManager.create();
            historicMilestoneInstanceEntity.setName(milestoneInstance.getName());
            historicMilestoneInstanceEntity.setCaseInstanceId(milestoneInstance.getCaseInstanceId());
            historicMilestoneInstanceEntity.setCaseDefinitionId(milestoneInstance.getCaseDefinitionId());
            historicMilestoneInstanceEntity.setElementId(milestoneInstance.getElementId());
            historicMilestoneInstanceEntity.setTimeStamp(cmmnEngineConfiguration.getClock().getCurrentTime());
            historicMilestoneInstanceEntityManager.insert(historicMilestoneInstanceEntity);
        }
    }

    @Override
    public void recordCaseInstanceDeleted(String caseInstanceId) {
        if (cmmnEngineConfiguration.getHistoryLevel() != HistoryLevel.NONE) {
            HistoricCaseInstanceEntityManager historicCaseInstanceEntityManager = cmmnEngineConfiguration.getHistoricCaseInstanceEntityManager();
            HistoricCaseInstanceEntity historicCaseInstance = historicCaseInstanceEntityManager.findById(caseInstanceId);

            HistoricMilestoneInstanceEntityManager historicMilestoneInstanceEntityManager = cmmnEngineConfiguration.getHistoricMilestoneInstanceEntityManager();
            List<HistoricMilestoneInstance> historicMilestoneInstances = historicMilestoneInstanceEntityManager
                    .findHistoricMilestoneInstancesByQueryCriteria(new HistoricMilestoneInstanceQueryImpl().milestoneInstanceCaseInstanceId(historicCaseInstance.getId()));
            for (HistoricMilestoneInstance historicMilestoneInstance : historicMilestoneInstances) {
                historicMilestoneInstanceEntityManager.delete(historicMilestoneInstance.getId());
            }

            CommandContextUtil.getHistoricIdentityLinkService().deleteHistoricIdentityLinksByScopeIdAndScopeType(historicCaseInstance.getId(), ScopeTypes.CMMN);

            if (historicCaseInstance != null) {
                historicCaseInstanceEntityManager.delete(historicCaseInstance);
            }

            // Also delete any sub cases that may be active

            List<HistoricCaseInstance> selectList = historicCaseInstanceEntityManager.createHistoricCaseInstanceQuery().caseInstanceParentId(caseInstanceId).list();
            for (HistoricCaseInstance child : selectList) {
                recordCaseInstanceDeleted(child.getId());
            }
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
    public void recordIdentityLinkDeleted(String identityLinkId) {
        if (cmmnEngineConfiguration.getHistoryLevel() != HistoryLevel.NONE) {
            CommandContextUtil.getHistoricIdentityLinkService().deleteHistoricIdentityLink(identityLinkId);
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
            historicPlanItemInstanceEntity.setStartTime(planItemInstanceEntity.getStartTime());
            historicPlanItemInstanceEntityManager.insert(historicPlanItemInstanceEntity);
        }
    }

    @Override
    public void recordPlanItemInstanceActivated(PlanItemInstanceEntity planItemInstanceEntity) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricPlanItemInstanceEntityManager historicPlanItemInstanceEntityManager = cmmnEngineConfiguration.getHistoricPlanItemInstanceEntityManager();
            HistoricPlanItemInstanceEntity historicPlanItemInstanceEntity = historicPlanItemInstanceEntityManager.findById(planItemInstanceEntity.getId());
            historicPlanItemInstanceEntity.setActivationTime(cmmnEngineConfiguration.getClock().getCurrentTime());
            historicPlanItemInstanceEntity.setEndTime(null); //In case of reactivation from a Fail state
            historicPlanItemInstanceEntity.setState(planItemInstanceEntity.getState());
        }
    }

    @Override
    public void recordPlanItemIntanceEnded(PlanItemInstanceEntity planItemInstanceEntity) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricPlanItemInstanceEntityManager historicPlanItemInstanceEntityManager = cmmnEngineConfiguration.getHistoricPlanItemInstanceEntityManager();
            HistoricPlanItemInstanceEntity historicPlanItemInstanceEntity = historicPlanItemInstanceEntityManager.findById(planItemInstanceEntity.getId());
            historicPlanItemInstanceEntity.setEndTime(cmmnEngineConfiguration.getClock().getCurrentTime());
            historicPlanItemInstanceEntity.setState(planItemInstanceEntity.getState());
        }
    }
}
