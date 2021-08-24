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
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.repository.CaseDefinition;
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
import org.flowable.cmmn.model.Milestone;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.Stage;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.entitylink.api.history.HistoricEntityLinkService;
import org.flowable.entitylink.service.impl.persistence.entity.EntityLinkEntity;
import org.flowable.entitylink.service.impl.persistence.entity.HistoricEntityLinkEntity;
import org.flowable.identitylink.service.HistoricIdentityLinkService;
import org.flowable.identitylink.service.impl.persistence.entity.HistoricIdentityLinkEntity;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskLogEntryBuilder;
import org.flowable.task.service.HistoricTaskService;
import org.flowable.task.service.impl.HistoricTaskInstanceQueryImpl;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskInstanceEntity;
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
            HistoricCaseInstanceEntity historicCaseInstanceEntity = cmmnEngineConfiguration.getHistoricCaseInstanceEntityManager().create(caseInstanceEntity);
            historicCaseInstanceEntityManager.insert(historicCaseInstanceEntity);
        }
    }

    @Override
    public void recordCaseInstanceEnd(CaseInstanceEntity caseInstanceEntity, String state, Date endTime) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricCaseInstanceEntityManager historicCaseInstanceEntityManager = cmmnEngineConfiguration.getHistoricCaseInstanceEntityManager();
            HistoricCaseInstanceEntity historicCaseInstanceEntity = historicCaseInstanceEntityManager.findById(caseInstanceEntity.getId());
            if (historicCaseInstanceEntity != null) {
                historicCaseInstanceEntity.setEndTime(endTime);
                historicCaseInstanceEntity.setState(state);
            }
        }
    }

    @Override
    public void recordHistoricCaseInstanceReactivated(CaseInstanceEntity caseInstanceEntity) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            // Update the historic one to NOT be ended anymore as we reactivated it again
            HistoricCaseInstanceEntityManager historicCaseInstanceEntityManager = cmmnEngineConfiguration.getHistoricCaseInstanceEntityManager();
            HistoricCaseInstanceEntity historicCaseInstanceEntity = historicCaseInstanceEntityManager.findById(caseInstanceEntity.getId());
            if (historicCaseInstanceEntity != null) {
                historicCaseInstanceEntity.setEndTime(null);
                historicCaseInstanceEntity.setState(caseInstanceEntity.getState());
                historicCaseInstanceEntity.setLastReactivationTime(caseInstanceEntity.getLastReactivationTime());
                historicCaseInstanceEntity.setLastReactivationUserId(caseInstanceEntity.getLastReactivationUserId());
            }
        }
    }

    @Override
    public void recordUpdateCaseInstanceName(CaseInstanceEntity caseInstanceEntity, String name) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricCaseInstanceEntityManager historicCaseInstanceEntityManager = cmmnEngineConfiguration.getHistoricCaseInstanceEntityManager();
            HistoricCaseInstanceEntity historicCaseInstanceEntity = historicCaseInstanceEntityManager.findById(caseInstanceEntity.getId());
            if (historicCaseInstanceEntity != null) {
                historicCaseInstanceEntity.setName(name);
            }
        }
    }

    @Override
    public void recordUpdateBusinessKey(CaseInstanceEntity caseInstanceEntity, String businessKey) {
        if (caseInstanceEntity != null) {
            if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
                HistoricCaseInstanceEntityManager historicCaseInstanceEntityManager = cmmnEngineConfiguration.getHistoricCaseInstanceEntityManager();
                HistoricCaseInstanceEntity historicCaseInstanceEntity = historicCaseInstanceEntityManager.findById(caseInstanceEntity.getId());
                if (historicCaseInstanceEntity != null) {
                    historicCaseInstanceEntity.setBusinessKey(businessKey);
                }
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
    public void recordHistoricCaseInstanceDeleted(String caseInstanceId, String tenantId) {
        if (cmmnEngineConfiguration.getHistoryLevel() != HistoryLevel.NONE) {
            CmmnHistoryHelper.deleteHistoricCaseInstance(cmmnEngineConfiguration, caseInstanceId);
        }
    }

    @Override
    public void recordIdentityLinkCreated(IdentityLinkEntity identityLink) {
        if (cmmnEngineConfiguration.getHistoryLevel() != HistoryLevel.NONE && (identityLink.getScopeId() != null || identityLink.getTaskId() != null)) {
            HistoricIdentityLinkService historicIdentityLinkService = cmmnEngineConfiguration.getIdentityLinkServiceConfiguration().getHistoricIdentityLinkService();
            HistoricIdentityLinkEntity historicIdentityLinkEntity = historicIdentityLinkService.createHistoricIdentityLink();
            historicIdentityLinkEntity.setId(identityLink.getId());
            historicIdentityLinkEntity.setGroupId(identityLink.getGroupId());
            historicIdentityLinkEntity.setScopeDefinitionId(identityLink.getScopeDefinitionId());
            historicIdentityLinkEntity.setScopeId(identityLink.getScopeId());
            historicIdentityLinkEntity.setSubScopeId(identityLink.getSubScopeId());
            historicIdentityLinkEntity.setScopeType(identityLink.getScopeType());
            historicIdentityLinkEntity.setTaskId(identityLink.getTaskId());
            historicIdentityLinkEntity.setType(identityLink.getType());
            historicIdentityLinkEntity.setUserId(identityLink.getUserId());
            historicIdentityLinkService.insertHistoricIdentityLink(historicIdentityLinkEntity, false);
        }
    }

    @Override
    public void recordIdentityLinkDeleted(IdentityLinkEntity identityLink) {
        if (cmmnEngineConfiguration.getHistoryLevel() != HistoryLevel.NONE) {
            cmmnEngineConfiguration.getIdentityLinkServiceConfiguration().getHistoricIdentityLinkService().deleteHistoricIdentityLink(identityLink.getId());
        }
    }
    
    @Override
    public void recordEntityLinkCreated(EntityLinkEntity entityLink) {
        if (cmmnEngineConfiguration.getHistoryLevel() != HistoryLevel.NONE && entityLink.getScopeId() != null) {
            HistoricEntityLinkService historicEntityLinkService = cmmnEngineConfiguration.getEntityLinkServiceConfiguration().getHistoricEntityLinkService();
            HistoricEntityLinkEntity historicEntityLinkEntity = (HistoricEntityLinkEntity) historicEntityLinkService.createHistoricEntityLink();
            historicEntityLinkEntity.setId(entityLink.getId());
            historicEntityLinkEntity.setLinkType(entityLink.getLinkType());
            historicEntityLinkEntity.setCreateTime(entityLink.getCreateTime());
            historicEntityLinkEntity.setScopeId(entityLink.getScopeId());
            historicEntityLinkEntity.setSubScopeId(entityLink.getSubScopeId());
            historicEntityLinkEntity.setScopeType(entityLink.getScopeType());
            historicEntityLinkEntity.setScopeDefinitionId(entityLink.getScopeDefinitionId());
            historicEntityLinkEntity.setParentElementId(entityLink.getParentElementId());
            historicEntityLinkEntity.setReferenceScopeId(entityLink.getReferenceScopeId());
            historicEntityLinkEntity.setReferenceScopeType(entityLink.getReferenceScopeType());
            historicEntityLinkEntity.setReferenceScopeDefinitionId(entityLink.getReferenceScopeDefinitionId());
            historicEntityLinkEntity.setRootScopeId(entityLink.getRootScopeId());
            historicEntityLinkEntity.setRootScopeType(entityLink.getRootScopeType());
            historicEntityLinkEntity.setHierarchyType(entityLink.getHierarchyType());
            historicEntityLinkService.insertHistoricEntityLink(historicEntityLinkEntity, false);
        }
    }

    @Override
    public void recordEntityLinkDeleted(EntityLinkEntity entityLink) {
        if (cmmnEngineConfiguration.getHistoryLevel() != HistoryLevel.NONE) {
            cmmnEngineConfiguration.getEntityLinkServiceConfiguration().getHistoricEntityLinkService().deleteHistoricEntityLink(entityLink.getId());
        }
    }

    @Override
    public void recordVariableCreate(VariableInstanceEntity variable, Date createTime) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
            cmmnEngineConfiguration.getVariableServiceConfiguration().getHistoricVariableService().createAndInsert(variable, createTime);
        }
    }

    @Override
    public void recordVariableUpdate(VariableInstanceEntity variableInstanceEntity, Date updateTime) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
            cmmnEngineConfiguration.getVariableServiceConfiguration().getHistoricVariableService().recordVariableUpdate(variableInstanceEntity, updateTime);
        }
    }

    @Override
    public void recordVariableRemoved(VariableInstanceEntity variableInstanceEntity) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
            cmmnEngineConfiguration.getVariableServiceConfiguration().getHistoricVariableService().recordVariableRemoved(variableInstanceEntity);
        }
    }

    @Override
    public void recordTaskCreated(TaskEntity task) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
            cmmnEngineConfiguration.getTaskServiceConfiguration().getHistoricTaskService().recordTaskCreated(task);
        }
    }

    @Override
    public void recordTaskEnd(TaskEntity task, String deleteReason, Date endTime) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
            cmmnEngineConfiguration.getTaskServiceConfiguration().getHistoricTaskService().recordTaskEnd(task, deleteReason, endTime);
        }
    }

    @Override
    public void recordTaskInfoChange(TaskEntity taskEntity, Date changeTime) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
            cmmnEngineConfiguration.getTaskServiceConfiguration().getHistoricTaskService().recordTaskInfoChange(taskEntity, changeTime, cmmnEngineConfiguration);
        }
    }

    @Override
    public void recordPlanItemInstanceCreated(PlanItemInstanceEntity planItemInstanceEntity) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricPlanItemInstanceEntityManager historicPlanItemInstanceEntityManager = cmmnEngineConfiguration.getHistoricPlanItemInstanceEntityManager();
            HistoricPlanItemInstanceEntity historicPlanItemInstanceEntity = historicPlanItemInstanceEntityManager.create(planItemInstanceEntity);
            historicPlanItemInstanceEntity.setShowInOverview(evaluateShowInOverview(planItemInstanceEntity));
            
            historicPlanItemInstanceEntityManager.insert(historicPlanItemInstanceEntity);
        }
    }

    @Override
    public void recordPlanItemInstanceReactivated(PlanItemInstanceEntity planItemInstanceEntity) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricPlanItemInstanceEntityManager historicPlanItemInstanceEntityManager = cmmnEngineConfiguration.getHistoricPlanItemInstanceEntityManager();
            HistoricPlanItemInstanceEntity historicPlanItemInstanceEntity = historicPlanItemInstanceEntityManager.create(planItemInstanceEntity);
            historicPlanItemInstanceEntity.setShowInOverview(evaluateShowInOverview(planItemInstanceEntity));

            historicPlanItemInstanceEntityManager.insert(historicPlanItemInstanceEntity);
            // TODO: do we need a specific flag to mark this entity being created because of a reactivation?
        }
    }

    @Override
    public void recordPlanItemInstanceUpdated(PlanItemInstanceEntity planItemInstanceEntity) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricPlanItemInstanceEntityManager historicPlanItemInstanceEntityManager = cmmnEngineConfiguration.getHistoricPlanItemInstanceEntityManager();
            HistoricPlanItemInstanceEntity historicPlanItemInstanceEntity = historicPlanItemInstanceEntityManager.findById(planItemInstanceEntity.getId());
            if (historicPlanItemInstanceEntity != null) {
                historicPlanItemInstanceEntity.setFormKey(planItemInstanceEntity.getFormKey());
            }
        }
    }

    @Override
    public void recordPlanItemInstanceAvailable(PlanItemInstanceEntity planItemInstanceEntity) {
        recordHistoricPlanItemInstanceEntity(planItemInstanceEntity, planItemInstanceEntity.getLastAvailableTime(),
            h -> h.setLastAvailableTime(planItemInstanceEntity.getLastAvailableTime()));
    }

    @Override
    public void recordPlanItemInstanceUnavailable(PlanItemInstanceEntity planItemInstanceEntity) {
        recordHistoricPlanItemInstanceEntity(planItemInstanceEntity, planItemInstanceEntity.getLastUnavailableTime(),
            h -> h.setLastUnavailableTime(planItemInstanceEntity.getLastUnavailableTime()));
    }

    @Override
    public void recordPlanItemInstanceEnabled(PlanItemInstanceEntity planItemInstanceEntity) {
        recordHistoricPlanItemInstanceEntity(planItemInstanceEntity, planItemInstanceEntity.getLastEnabledTime(),
            h -> h.setLastEnabledTime(planItemInstanceEntity.getLastEnabledTime()));
    }

    @Override
    public void recordPlanItemInstanceDisabled(PlanItemInstanceEntity planItemInstanceEntity) {
        recordHistoricPlanItemInstanceEntity(planItemInstanceEntity, planItemInstanceEntity.getLastDisabledTime(),
            h -> h.setLastDisabledTime(planItemInstanceEntity.getLastDisabledTime()));
    }

    @Override
    public void recordPlanItemInstanceStarted(PlanItemInstanceEntity planItemInstanceEntity) {
        recordHistoricPlanItemInstanceEntity(planItemInstanceEntity, planItemInstanceEntity.getLastStartedTime(),
            h -> {
                h.setLastStartedTime(planItemInstanceEntity.getLastStartedTime());
                h.setShowInOverview(evaluateShowInOverview(planItemInstanceEntity));

                // Can be updated when the plan item instance starts
                h.setReferenceId(planItemInstanceEntity.getReferenceId());
                h.setReferenceType(planItemInstanceEntity.getReferenceType());
            });
    }

    @Override
    public void recordPlanItemInstanceSuspended(PlanItemInstanceEntity planItemInstanceEntity) {
        recordHistoricPlanItemInstanceEntity(planItemInstanceEntity, planItemInstanceEntity.getLastSuspendedTime(),
            h -> h.setLastSuspendedTime(planItemInstanceEntity.getLastSuspendedTime()));
    }

    @Override
    public void recordPlanItemInstanceCompleted(PlanItemInstanceEntity planItemInstanceEntity) {
        Date completedTime = planItemInstanceEntity.getCompletedTime();
        recordHistoricPlanItemInstanceEntity(planItemInstanceEntity, completedTime,
            h -> {
                h.setEndedTime(completedTime);
                h.setCompletedTime(completedTime);
                h.setShowInOverview(evaluateShowInOverview(planItemInstanceEntity));
            });
    }

    @Override
    public void recordPlanItemInstanceTerminated(PlanItemInstanceEntity planItemInstanceEntity) {
        Date terminatedTime = planItemInstanceEntity.getTerminatedTime();
        recordHistoricPlanItemInstanceEntity(planItemInstanceEntity, terminatedTime,
            h -> {
                h.setEndedTime(terminatedTime);
                h.setTerminatedTime(terminatedTime);
                h.setShowInOverview(evaluateShowInOverview(planItemInstanceEntity));
            });
    }

    @Override
    public void recordPlanItemInstanceOccurred(PlanItemInstanceEntity planItemInstanceEntity) {
        Date occurredTime = planItemInstanceEntity.getOccurredTime();
        recordHistoricPlanItemInstanceEntity(planItemInstanceEntity, occurredTime,
            h -> {
                h.setEndedTime(occurredTime);
                h.setOccurredTime(occurredTime);
                h.setShowInOverview(evaluateShowInOverview(planItemInstanceEntity));
        });
    }

    @Override
    public void recordPlanItemInstanceExit(PlanItemInstanceEntity planItemInstanceEntity) {
        Date exitTime = planItemInstanceEntity.getExitTime();
        recordHistoricPlanItemInstanceEntity(planItemInstanceEntity, exitTime,
            h -> {
                h.setEndedTime(exitTime);
                h.setExitTime(exitTime);
            });
    }
    
    @Override
    public void updateCaseDefinitionIdInHistory(CaseDefinition caseDefinition, CaseInstanceEntity caseInstance) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricCaseInstanceEntityManager historicCaseInstanceEntityManager = cmmnEngineConfiguration.getHistoricCaseInstanceEntityManager();
            HistoricCaseInstanceEntity historicCaseInstance = historicCaseInstanceEntityManager.findById(caseInstance.getId());
            historicCaseInstance.setCaseDefinitionId(caseDefinition.getId());
            historicCaseInstanceEntityManager.update(historicCaseInstance);
    
            HistoricTaskService historicTaskService = cmmnEngineConfiguration.getTaskServiceConfiguration().getHistoricTaskService();
            HistoricTaskInstanceQueryImpl taskQuery = new HistoricTaskInstanceQueryImpl();
            taskQuery.caseInstanceId(caseInstance.getId());
            List<HistoricTaskInstance> historicTasks = historicTaskService.findHistoricTaskInstancesByQueryCriteria(taskQuery);
            if (historicTasks != null) {
                for (HistoricTaskInstance historicTaskInstance : historicTasks) {
                    HistoricTaskInstanceEntity taskEntity = (HistoricTaskInstanceEntity) historicTaskInstance;
                    taskEntity.setScopeDefinitionId(caseDefinition.getId());
                    historicTaskService.updateHistoricTask(taskEntity, true);
                }
            }

            // because of upgrade runtimeActivity instances can be only subset of historicActivity instances
            HistoricPlanItemInstanceQueryImpl historicPlanItemQuery = new HistoricPlanItemInstanceQueryImpl();
            historicPlanItemQuery.planItemInstanceCaseInstanceId(caseInstance.getId());
            HistoricPlanItemInstanceEntityManager historicPlanItemInstanceEntityManager = cmmnEngineConfiguration.getHistoricPlanItemInstanceEntityManager();
            List<HistoricPlanItemInstance> historicPlanItems = historicPlanItemInstanceEntityManager.findByCriteria(historicPlanItemQuery);
            if (historicPlanItems != null) {
                for (HistoricPlanItemInstance historicPlanItemInstance : historicPlanItems) {
                    HistoricPlanItemInstanceEntity planItemEntity = (HistoricPlanItemInstanceEntity) historicPlanItemInstance;
                    planItemEntity.setCaseDefinitionId(caseDefinition.getId());
                    historicPlanItemInstanceEntityManager.update(planItemEntity);
                }
            }
        }
    }

    @Override
    public void recordHistoricUserTaskLogEntry(HistoricTaskLogEntryBuilder taskLogEntryBuilder) {
        cmmnEngineConfiguration.getTaskServiceConfiguration().getHistoricTaskService().createHistoricTaskLogEntry(taskLogEntryBuilder);
    }

    @Override
    public void deleteHistoricUserTaskLogEntry(long logNumber) {
        cmmnEngineConfiguration.getTaskServiceConfiguration().getHistoricTaskService().deleteHistoricTaskLogEntry(logNumber);
    }

    protected void recordHistoricPlanItemInstanceEntity(PlanItemInstanceEntity planItemInstanceEntity, Date lastUpdatedTime, Consumer<HistoricPlanItemInstanceEntity> changes) {
        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricPlanItemInstanceEntityManager historicPlanItemInstanceEntityManager = cmmnEngineConfiguration.getHistoricPlanItemInstanceEntityManager();
            HistoricPlanItemInstanceEntity historicPlanItemInstanceEntity = historicPlanItemInstanceEntityManager.findById(planItemInstanceEntity.getId());
            if (historicPlanItemInstanceEntity != null) {

                historicPlanItemInstanceEntity.setState(planItemInstanceEntity.getState());
                historicPlanItemInstanceEntity.setLastUpdatedTime(lastUpdatedTime);
                changes.accept(historicPlanItemInstanceEntity);

                // Can be updated on state change
                historicPlanItemInstanceEntity.setEntryCriterionId(planItemInstanceEntity.getEntryCriterionId());
                historicPlanItemInstanceEntity.setExitCriterionId(planItemInstanceEntity.getExitCriterionId());
            }
        }
    }
    
    public boolean evaluateShowInOverview(PlanItemInstanceEntity planItemInstanceEntity) {
        boolean showInOverview = false;
        
        PlanItemDefinition planItemDefinition = planItemInstanceEntity.getPlanItem().getPlanItemDefinition();
        String includeInStageOverviewValue = null;
        if (planItemInstanceEntity.isStage()) {
            if (planItemDefinition instanceof Stage) {
                Stage stage = (Stage) planItemDefinition;
                includeInStageOverviewValue = stage.getIncludeInStageOverview();
            }
            
        } else if (planItemDefinition instanceof Milestone) {
            Milestone milestone = (Milestone) planItemDefinition;
            includeInStageOverviewValue = milestone.getIncludeInStageOverview();
        }
        
        if (StringUtils.isNotEmpty(includeInStageOverviewValue)) {
            if ("true".equalsIgnoreCase(includeInStageOverviewValue)) {
                showInOverview = true;
            
            } else if (!"false".equalsIgnoreCase(includeInStageOverviewValue)) {
                Expression stageExpression = cmmnEngineConfiguration.getExpressionManager().createExpression(includeInStageOverviewValue);
                Object stageValueObject = stageExpression.getValue(planItemInstanceEntity);
                if (!(stageValueObject instanceof Boolean)) {
                    throw new FlowableException("Include in stage overview expression does not resolve to a boolean value " + 
                                    includeInStageOverviewValue + ": " + stageValueObject);
                }
                
                showInOverview = (Boolean) stageValueObject;
            }
        }
        
        return showInOverview;
    }
}
