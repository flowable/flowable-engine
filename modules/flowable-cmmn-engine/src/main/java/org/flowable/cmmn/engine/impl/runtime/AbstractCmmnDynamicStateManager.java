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
package org.flowable.cmmn.engine.impl.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.migration.ActivatePlanItemDefinitionMapping;
import org.flowable.cmmn.api.migration.ChangePlanItemDefinitionWithNewTargetIdsMapping;
import org.flowable.cmmn.api.migration.ChangePlanItemIdMapping;
import org.flowable.cmmn.api.migration.ChangePlanItemIdWithDefinitionIdMapping;
import org.flowable.cmmn.api.migration.MoveToAvailablePlanItemDefinitionMapping;
import org.flowable.cmmn.api.migration.PlanItemDefinitionMapping;
import org.flowable.cmmn.api.migration.RemoveWaitingForRepetitionPlanItemDefinitionMapping;
import org.flowable.cmmn.api.migration.TerminatePlanItemDefinitionMapping;
import org.flowable.cmmn.api.migration.WaitingForRepetitionPlanItemDefinitionMapping;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.MilestoneInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.agenda.CmmnEngineAgenda;
import org.flowable.cmmn.engine.impl.behavior.impl.ChildTaskActivityBehavior;
import org.flowable.cmmn.engine.impl.deployer.CmmnDeploymentManager;
import org.flowable.cmmn.engine.impl.history.CmmnHistoryManager;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseDefinitionEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricMilestoneInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricMilestoneInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.MilestoneInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.MilestoneInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.SentryPartInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.SentryPartInstanceEntityManager;
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionUtil;
import org.flowable.cmmn.engine.impl.runtime.MovePlanItemInstanceEntityContainer.PlanItemMoveEntry;
import org.flowable.cmmn.engine.impl.task.TaskHelper;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.impl.util.ExpressionUtil;
import org.flowable.cmmn.engine.interceptor.MigrationContext;
import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CaseTask;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.Criterion;
import org.flowable.cmmn.model.EventListener;
import org.flowable.cmmn.model.HumanTask;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.ProcessTask;
import org.flowable.cmmn.model.RepetitionRule;
import org.flowable.cmmn.model.Sentry;
import org.flowable.cmmn.model.SentryIfPart;
import org.flowable.cmmn.model.SentryOnPart;
import org.flowable.cmmn.model.Stage;
import org.flowable.cmmn.model.TimerEventListener;
import org.flowable.cmmn.model.UserEventListener;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.eventsubscription.service.EventSubscriptionService;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntity;
import org.flowable.job.api.Job;
import org.flowable.job.service.JobService;
import org.flowable.task.service.TaskService;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntityImpl;
import org.flowable.task.service.impl.persistence.entity.TaskEntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 * @author Valentin Zickner
 */
public abstract class AbstractCmmnDynamicStateManager {

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
    
    protected CmmnEngineConfiguration cmmnEngineConfiguration;
    
    public AbstractCmmnDynamicStateManager(CmmnEngineConfiguration cmmnEngineConfiguration) {
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
    }

    protected PlanItem resolvePlanItemFromCmmnModelWithDefinitionId(String planItemDefinitionId, String caseDefinitionId) {
        CmmnModel cmmnModel = CaseDefinitionUtil.getCmmnModel(caseDefinitionId);
        PlanItem planItem = cmmnModel.findPlanItemByPlanItemDefinitionId(planItemDefinitionId);
        if (planItem == null) {
            throw new FlowableException("Cannot find plan item with definition id '" + planItemDefinitionId + "' in case definition with id '" + caseDefinitionId + "'");
        }
        return planItem;
    }

    protected PlanItem resolvePlanItemFromCmmnModel(CmmnModel cmmnModel, String planItemId, String caseDefinitionId) {
        PlanItem planItem = cmmnModel.findPlanItem(planItemId);
        if (planItem == null) {
            throw new FlowableException("Cannot find plan item '" + planItemId + "' in case definition with id '" + caseDefinitionId + "'");
        }
        return planItem;
    }

    protected void doMovePlanItemState(CaseInstanceChangeState caseInstanceChangeState, String originalCaseDefinitionId, CommandContext commandContext) {
        CaseInstanceEntityManager caseInstanceEntityManager = cmmnEngineConfiguration.getCaseInstanceEntityManager();
        CaseInstanceEntity caseInstance = caseInstanceEntityManager.findById(caseInstanceChangeState.getCaseInstanceId());
        
        Map<String, List<PlanItemInstanceEntity>> currentPlanItemInstances = retrievePlanItemInstances(caseInstanceChangeState.getCaseInstanceId());
        caseInstanceChangeState.setCurrentPlanItemInstances(currentPlanItemInstances);
        
        executeVerifySatisfiedSentryParts(caseInstanceChangeState, caseInstance, originalCaseDefinitionId, commandContext);
        
        executeTerminatePlanItemInstances(caseInstanceChangeState, caseInstance, commandContext);
        
        executeTerminateNonExistingPlanItemInstancesInTargetCmmnModel(caseInstanceChangeState, commandContext);
        
        setCaseDefinitionIdForPlanItemInstances(currentPlanItemInstances, caseInstanceChangeState.getCaseDefinitionToMigrateTo());
        
        executeChangePlanItemIds(caseInstanceChangeState, originalCaseDefinitionId, commandContext);
        
        executeChangePlanItemDefinitionWithNewTargetIds(caseInstanceChangeState, originalCaseDefinitionId, commandContext);
        
        navigatePlanItemInstances(currentPlanItemInstances, caseInstanceChangeState.getCaseDefinitionToMigrateTo(), originalCaseDefinitionId);
        
        // Set the case variables first so they are available during the change state logic
        caseInstance.setVariables(caseInstanceChangeState.getCaseVariables());
        
        executeActivatePlanItemInstances(caseInstanceChangeState, caseInstance, true, commandContext);
        executeActivatePlanItemInstances(caseInstanceChangeState, caseInstance, false, commandContext);
        executeChangePlanItemInstancesToAvailableState(caseInstanceChangeState, caseInstance, true, commandContext);
        executeChangePlanItemInstancesToAvailableState(caseInstanceChangeState, caseInstance, false, commandContext);
        executeAddWaitingForRepetitionPlanItemInstances(caseInstanceChangeState, caseInstance, commandContext);
        executeRemoveWaitingForRepetitionPlanItemInstances(caseInstanceChangeState, caseInstance, commandContext);
        
        CmmnEngineAgenda agenda = CommandContextUtil.getAgenda(commandContext);
        MigrationContext migrationContext = new MigrationContext();
        migrationContext.setFetchPlanItemInstances(true);
        agenda.planEvaluateCriteriaOperation(caseInstance.getId(), migrationContext);
    }
    
    protected void executeChangePlanItemIds(CaseInstanceChangeState caseInstanceChangeState, String originalCaseDefinitionId, CommandContext commandContext) {
        if ((caseInstanceChangeState.getChangePlanItemIds() == null || caseInstanceChangeState.getChangePlanItemIds().isEmpty()) &&
                (caseInstanceChangeState.getChangePlanItemIdsWithDefinitionId() == null || caseInstanceChangeState.getChangePlanItemIdsWithDefinitionId().isEmpty())) {
            return;
        }
        
        Map<String, String> changePlanItemIdMap = new HashMap<>();
        if (caseInstanceChangeState.getChangePlanItemIds() != null && !caseInstanceChangeState.getChangePlanItemIds().isEmpty()) {
            for (ChangePlanItemIdMapping changePlanItemIdMapping : caseInstanceChangeState.getChangePlanItemIds()) {
                changePlanItemIdMap.put(changePlanItemIdMapping.getExistingPlanItemId(), changePlanItemIdMapping.getNewPlanItemId());
            }
            
        } else {
            CmmnModel originalCmmnModel = CaseDefinitionUtil.getCmmnModel(originalCaseDefinitionId);
            CmmnModel targetCmmnModel = CaseDefinitionUtil.getCmmnModel(caseInstanceChangeState.getCaseDefinitionToMigrateTo().getId());
            for (ChangePlanItemIdWithDefinitionIdMapping definitionIdMapping : caseInstanceChangeState.getChangePlanItemIdsWithDefinitionId()) {
                PlanItem existingPlanItem = originalCmmnModel.findPlanItemByPlanItemDefinitionId(definitionIdMapping.getExistingPlanItemDefinitionId());
                PlanItem newPlanItem = targetCmmnModel.findPlanItemByPlanItemDefinitionId(definitionIdMapping.getNewPlanItemDefinitionId());
                
                if (existingPlanItem != null && newPlanItem != null) {
                    changePlanItemIdMap.put(existingPlanItem.getId(), newPlanItem.getId());
                }
            }
            
        }
        
        PlanItemInstanceEntityManager planItemInstanceEntityManager = cmmnEngineConfiguration.getPlanItemInstanceEntityManager();
        CmmnHistoryManager cmmnHistoryManager = cmmnEngineConfiguration.getCmmnHistoryManager();
        for (String planItemDefinitionId : caseInstanceChangeState.getCurrentPlanItemInstances().keySet()) {
            for (PlanItemInstanceEntity currentPlanItemInstance : caseInstanceChangeState.getCurrentPlanItemInstances().get(planItemDefinitionId)) {
                if (changePlanItemIdMap.containsKey(currentPlanItemInstance.getElementId())) {
                    currentPlanItemInstance.setElementId(changePlanItemIdMap.get(currentPlanItemInstance.getElementId()));
                    planItemInstanceEntityManager.update(currentPlanItemInstance);
                    cmmnHistoryManager.recordPlanItemInstanceUpdated(currentPlanItemInstance);
                }
            }
        }
        
        MilestoneInstanceEntityManager milestoneInstanceEntityManager = cmmnEngineConfiguration.getMilestoneInstanceEntityManager();
        HistoricMilestoneInstanceEntityManager historicMilestoneInstanceEntityManager = cmmnEngineConfiguration.getHistoricMilestoneInstanceEntityManager();
        MilestoneInstanceQueryImpl milestoneInstanceQuery = new MilestoneInstanceQueryImpl(cmmnEngineConfiguration.getCommandExecutor());
        milestoneInstanceQuery.milestoneInstanceCaseInstanceId(caseInstanceChangeState.getCaseInstanceId());
        List<MilestoneInstance> milestoneInstances = milestoneInstanceEntityManager.findMilestoneInstancesByQueryCriteria(milestoneInstanceQuery);
        for (MilestoneInstance milestoneInstance : milestoneInstances) {
            if (changePlanItemIdMap.containsKey(milestoneInstance.getElementId())) {
                MilestoneInstanceEntity milestoneInstanceEntity = (MilestoneInstanceEntity) milestoneInstance;
                milestoneInstanceEntity.setElementId(changePlanItemIdMap.get(milestoneInstance.getElementId()));
                milestoneInstanceEntity.setCaseDefinitionId(caseInstanceChangeState.getCaseDefinitionToMigrateTo().getId());
                milestoneInstanceEntityManager.update(milestoneInstanceEntity);
                
                HistoricMilestoneInstanceEntity historicMilestoneInstanceEntity = historicMilestoneInstanceEntityManager.findById(milestoneInstanceEntity.getId());
                historicMilestoneInstanceEntity.setElementId(milestoneInstanceEntity.getElementId());
                historicMilestoneInstanceEntity.setCaseDefinitionId(caseInstanceChangeState.getCaseDefinitionToMigrateTo().getId());
                historicMilestoneInstanceEntityManager.update(historicMilestoneInstanceEntity);
            }
        }
    }
    
    protected void executeChangePlanItemDefinitionWithNewTargetIds(CaseInstanceChangeState caseInstanceChangeState, String originalCaseDefinitionId, CommandContext commandContext) {
        if ((caseInstanceChangeState.getChangePlanItemDefinitionWithNewTargetIds() == null || caseInstanceChangeState.getChangePlanItemDefinitionWithNewTargetIds().isEmpty())) {
            return;
        }
        
        Map<String, ChangePlanItemDefinitionWithNewTargetIdsMapping> changePlanItemIdMap = new HashMap<>();
        Map<String, ChangePlanItemDefinitionWithNewTargetIdsMapping> changePlanItemDefinitionIdMap = new HashMap<>();
        
        CmmnModel originalCmmnModel = CaseDefinitionUtil.getCmmnModel(originalCaseDefinitionId);
        CmmnModel targetCmmnModel = CaseDefinitionUtil.getCmmnModel(caseInstanceChangeState.getCaseDefinitionToMigrateTo().getId());
        for (ChangePlanItemDefinitionWithNewTargetIdsMapping definitionIdMapping : caseInstanceChangeState.getChangePlanItemDefinitionWithNewTargetIds()) {
            PlanItem existingPlanItem = originalCmmnModel.findPlanItemByPlanItemDefinitionId(definitionIdMapping.getExistingPlanItemDefinitionId());
            PlanItem newPlanItem = targetCmmnModel.findPlanItemByPlanItemDefinitionId(definitionIdMapping.getNewPlanItemDefinitionId());
            
            if (existingPlanItem != null && newPlanItem != null) {
                changePlanItemIdMap.put(existingPlanItem.getId(), definitionIdMapping);
                changePlanItemDefinitionIdMap.put(definitionIdMapping.getExistingPlanItemDefinitionId(), definitionIdMapping);
            }
        }
        
        PlanItemInstanceEntityManager planItemInstanceEntityManager = cmmnEngineConfiguration.getPlanItemInstanceEntityManager();
        CmmnHistoryManager cmmnHistoryManager = cmmnEngineConfiguration.getCmmnHistoryManager();
        for (String planItemDefinitionId : caseInstanceChangeState.getCurrentPlanItemInstances().keySet()) {
            for (PlanItemInstanceEntity currentPlanItemInstance : caseInstanceChangeState.getCurrentPlanItemInstances().get(planItemDefinitionId)) {
                if (changePlanItemIdMap.containsKey(currentPlanItemInstance.getElementId())) {
                    ChangePlanItemDefinitionWithNewTargetIdsMapping definitionIdMapping = changePlanItemIdMap.get(currentPlanItemInstance.getElementId());
                    currentPlanItemInstance.setElementId(definitionIdMapping.getNewPlanItemId());
                    currentPlanItemInstance.setPlanItemDefinitionId(definitionIdMapping.getNewPlanItemDefinitionId());
                    planItemInstanceEntityManager.update(currentPlanItemInstance);
                    cmmnHistoryManager.recordPlanItemInstanceUpdated(currentPlanItemInstance);
                }
            }
        }
        
        TaskEntityManager taskEntityManager = cmmnEngineConfiguration.getTaskServiceConfiguration().getTaskEntityManager();
        List<TaskEntity> tasks = taskEntityManager.findTasksByScopeIdAndScopeType(caseInstanceChangeState.getCaseInstanceId(), ScopeTypes.CMMN);
        for (TaskEntity task : tasks) {
            if (changePlanItemDefinitionIdMap.containsKey(task.getTaskDefinitionKey())) {
                ChangePlanItemDefinitionWithNewTargetIdsMapping definitionIdMapping = changePlanItemDefinitionIdMap.get(task.getTaskDefinitionKey());
                task.setTaskDefinitionKey(definitionIdMapping.getNewPlanItemDefinitionId());
                taskEntityManager.update(task);
                cmmnHistoryManager.recordTaskInfoChange(task, cmmnEngineConfiguration.getClock().getCurrentTime());
            }
        }
    }
    
    protected void executeActivatePlanItemInstances(CaseInstanceChangeState caseInstanceChangeState, CaseInstanceEntity caseInstance, 
                    boolean onlyStages, CommandContext commandContext) {
        
        if (caseInstanceChangeState.getActivatePlanItemDefinitions() == null || caseInstanceChangeState.getActivatePlanItemDefinitions().isEmpty()) {
            return;
        }
        
        for (ActivatePlanItemDefinitionMapping planItemDefinitionMapping : caseInstanceChangeState.getActivatePlanItemDefinitions()) {
            
            PlanItem planItem = resolvePlanItemFromCmmnModelWithDefinitionId(planItemDefinitionMapping.getPlanItemDefinitionId(), 
                    caseInstance.getCaseDefinitionId());
            
            if ((!(planItem.getPlanItemDefinition() instanceof Stage) && onlyStages) ||
                    (planItem.getPlanItemDefinition() instanceof Stage && !onlyStages)) {
                
                continue;
            }

            PlanItemInstanceEntity newPlanItemInstance = createStagesAndPlanItemInstances(planItem, 
                            caseInstance, caseInstanceChangeState, planItemDefinitionMapping, commandContext);

            if (newPlanItemInstance == null) {
                continue; // condition evaluated to false, this plan item should not be activated
            }
            
            if (planItemDefinitionMapping.getWithLocalVariables() != null && !planItemDefinitionMapping.getWithLocalVariables().isEmpty()) {
                newPlanItemInstance.setVariablesLocal(planItemDefinitionMapping.getWithLocalVariables());
            }

            CmmnEngineAgenda agenda = CommandContextUtil.getAgenda(commandContext);
            if (planItemDefinitionMapping.getNewAssignee() != null && planItem.getPlanItemDefinition() instanceof HumanTask) {
                MigrationContext migrationContext = new MigrationContext();
                migrationContext.setAssignee(planItemDefinitionMapping.getNewAssignee());
                agenda.planStartPlanItemInstanceOperation(newPlanItemInstance, null, migrationContext);
                
            } else if (caseInstanceChangeState.getChildInstanceTaskVariables().containsKey(planItemDefinitionMapping.getPlanItemDefinitionId()) && 
                            (planItem.getPlanItemDefinition() instanceof ProcessTask || planItem.getPlanItemDefinition() instanceof CaseTask)) {

                agenda.planStartPlanItemInstanceOperation(newPlanItemInstance, null,
                        new ChildTaskActivityBehavior.VariableInfo(
                                caseInstanceChangeState.getChildInstanceTaskVariables().get(planItemDefinitionMapping.getPlanItemDefinitionId())));
                
            } else {
                agenda.planStartPlanItemInstanceOperation(newPlanItemInstance, null);
            }
            
            if (!newPlanItemInstance.getPlanItem().getEntryCriteria().isEmpty() && hasRepetitionRule(newPlanItemInstance)) {
                if (evaluateRepetitionRule(newPlanItemInstance, commandContext)) {
                    createPlanItemInstanceDuplicateForRepetition(newPlanItemInstance, commandContext);
                }
            
            } else if (hasRepetitionRule(newPlanItemInstance)) {
                setRepetitionCounter(newPlanItemInstance, 1);
            }
        }
    }
    
    protected void executeChangePlanItemInstancesToAvailableState(CaseInstanceChangeState caseInstanceChangeState, 
                    CaseInstanceEntity caseInstance, boolean onlyStages, CommandContext commandContext) {
        
        if (caseInstanceChangeState.getChangePlanItemDefinitionsToAvailable() == null || caseInstanceChangeState.getChangePlanItemDefinitionsToAvailable().isEmpty()) {
            return;
        }
        
        PlanItemInstanceEntityManager planItemInstanceEntityManager = cmmnEngineConfiguration.getPlanItemInstanceEntityManager();

        for (MoveToAvailablePlanItemDefinitionMapping planItemDefinitionMapping : caseInstanceChangeState.getChangePlanItemDefinitionsToAvailable()) {
            
            PlanItem planItem = resolvePlanItemFromCmmnModelWithDefinitionId(planItemDefinitionMapping.getPlanItemDefinitionId(), caseInstance.getCaseDefinitionId());
            if ((!(planItem.getPlanItemDefinition() instanceof Stage) && onlyStages) ||
                    (planItem.getPlanItemDefinition() instanceof Stage && !onlyStages)) {
                
                continue;
            }
            
            List<PlanItemInstance> planItemInstances = planItemInstanceEntityManager.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                    .planItemDefinitionId(planItemDefinitionMapping.getPlanItemDefinitionId())
                    .list();
            
            List<PlanItemInstance> activePlanItemInstances = new ArrayList<>();
            if (planItemInstances != null && !planItemInstances.isEmpty()) {
                for (PlanItemInstance planItemInstance : planItemInstances) {
                    if (!PlanItemInstanceState.TERMINAL_STATES.contains(planItemInstance.getState())) {
                        activePlanItemInstances.add(planItemInstance);
                    }
                }
            }
            
            if (activePlanItemInstances.isEmpty()) {
                PlanItemInstanceEntity parentPlanItemInstance = null;
                if (planItem.getParentStage() != null && caseInstanceChangeState.getCreatedStageInstances().containsKey(planItem.getParentStage().getId())) {
                    parentPlanItemInstance = caseInstanceChangeState.getCreatedStageInstances().get(planItem.getParentStage().getId());
                    
                } else if (planItem.getParentStage() != null) {
                    List<PlanItemInstanceEntity> caseInstancePlanItemInstances = planItemInstanceEntityManager.findByCaseInstanceId(caseInstance.getId());
                    for (PlanItemInstanceEntity caseInstancePlanItemInstance : caseInstancePlanItemInstances) {
                        if (caseInstancePlanItemInstance.getPlanItemDefinitionId().equals(planItem.getParentStage().getId()) &&
                                !PlanItemInstanceState.WAITING_FOR_REPETITION.equalsIgnoreCase(caseInstancePlanItemInstance.getState()) &&
                                !PlanItemInstanceState.isInTerminalState(caseInstancePlanItemInstance)) {
                            
                            parentPlanItemInstance = caseInstancePlanItemInstance;
                            break;
                        }
                    }
                }

                boolean conditionResult = (parentPlanItemInstance != null && evaluateCondition(parentPlanItemInstance, planItemDefinitionMapping))
                        || (parentPlanItemInstance == null && evaluateCondition(caseInstance, planItemDefinitionMapping));
                if (!conditionResult) {
                    continue;
                }
                
                PlanItemInstanceEntity availablePlanItemInstance = planItemInstanceEntityManager.createPlanItemInstanceEntityBuilder()
                        .planItem(planItem)
                        .caseDefinitionId(caseInstance.getCaseDefinitionId())
                        .caseInstanceId(caseInstance.getId())
                        .stagePlanItemInstance(parentPlanItemInstance)
                        .tenantId(caseInstance.getTenantId())
                        .addToParent(true)
                        .create();
                
                if (planItem.getPlanItemDefinition() instanceof Stage) {
                    caseInstanceChangeState.addCreatedStageInstance(planItemDefinitionMapping.getPlanItemDefinitionId(), availablePlanItemInstance);
                }
                
                if (hasRepetitionRule(availablePlanItemInstance)) {
                    setRepetitionCounter(availablePlanItemInstance, 1);
                }
                
                if (planItemDefinitionMapping.getWithLocalVariables() != null && !planItemDefinitionMapping.getWithLocalVariables().isEmpty()) {
                    availablePlanItemInstance.setVariablesLocal(planItemDefinitionMapping.getWithLocalVariables());
                }
                
                CmmnHistoryManager cmmnHistoryManager = cmmnEngineConfiguration.getCmmnHistoryManager();
                cmmnHistoryManager.recordPlanItemInstanceCreated(availablePlanItemInstance);
                
                CmmnEngineAgenda agenda = CommandContextUtil.getAgenda(commandContext);
                agenda.planChangePlanItemInstanceToAvailableOperation(availablePlanItemInstance);
                
                continue;
            }
            
            PlanItemInstance existingPlanItemInstance = null;
            boolean allExistingPlanItemsAreAvailable = true;
            for (PlanItemInstance planItemInstance : planItemInstances) {
                if (PlanItemInstanceState.ACTIVE.equals(planItemInstance.getState()) || PlanItemInstanceState.ENABLED.equals(planItemInstance.getState())) {
                    if (existingPlanItemInstance != null) {
                        throw new FlowableException("multiple active or enabled plan item instances found for plan item definition " + planItemDefinitionMapping.getPlanItemDefinitionId());
                    } else {
                        existingPlanItemInstance = planItemInstance;
                    }
                }
                if (!PlanItemInstanceState.AVAILABLE.equals(planItemInstance.getState())) {
                    allExistingPlanItemsAreAvailable = false;
                }
            }

            if (allExistingPlanItemsAreAvailable) {
                // all existing plan items are available, we can continue without any changes
                continue;
            }
            
            if (existingPlanItemInstance == null) {
                throw new FlowableException("No active or enabled plan item instances found for plan item definition " + planItemDefinitionMapping.getPlanItemDefinitionId());
            }
            
            PlanItemInstanceEntity existingPlanItemInstanceEntity = (PlanItemInstanceEntity) existingPlanItemInstance;

            if (!evaluateCondition(existingPlanItemInstanceEntity, planItemDefinitionMapping)) {
                continue;
            }

            if (existingPlanItemInstanceEntity.getPlanItem().getPlanItemDefinition() instanceof HumanTask) {
                TaskService taskService = cmmnEngineConfiguration.getTaskServiceConfiguration().getTaskService();
                List<TaskEntity> taskEntities = taskService.findTasksBySubScopeIdScopeType(existingPlanItemInstanceEntity.getId(), ScopeTypes.CMMN);
                if (taskEntities == null || taskEntities.isEmpty()) {
                    throw new FlowableException("No task entity found for plan item instance " + existingPlanItemInstanceEntity.getId());
                }

                // Should be only one
                for (TaskEntity taskEntity : taskEntities) {
                    if (!taskEntity.isDeleted()) {
                        TaskHelper.deleteTask(taskEntity, "Change plan item state", false, false, cmmnEngineConfiguration);
                    }
                }
            }

            CmmnEngineAgenda agenda = CommandContextUtil.getAgenda(commandContext);
            agenda.planChangePlanItemInstanceToAvailableOperation(existingPlanItemInstanceEntity);
        }
    }
    
    protected void executeAddWaitingForRepetitionPlanItemInstances(CaseInstanceChangeState caseInstanceChangeState, 
            CaseInstanceEntity caseInstance, CommandContext commandContext) {

        if (caseInstanceChangeState.getWaitingForRepetitionPlanItemDefinitions() == null || caseInstanceChangeState.getWaitingForRepetitionPlanItemDefinitions().isEmpty()) {
            return;
        }
        
        PlanItemInstanceEntityManager planItemInstanceEntityManager = cmmnEngineConfiguration.getPlanItemInstanceEntityManager();
        
        for (WaitingForRepetitionPlanItemDefinitionMapping planItemDefinitionMapping : caseInstanceChangeState.getWaitingForRepetitionPlanItemDefinitions()) {
            
            PlanItem planItem = resolvePlanItemFromCmmnModelWithDefinitionId(planItemDefinitionMapping.getPlanItemDefinitionId(), caseInstance.getCaseDefinitionId());
            
            List<PlanItemInstance> planItemInstances = planItemInstanceEntityManager.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                    .planItemDefinitionId(planItemDefinitionMapping.getPlanItemDefinitionId())
                    .list();
            
            List<PlanItemInstance> waitingForRepetitionPlanItemInstances = new ArrayList<>();
            PlanItemInstance activePlanItemInstance = null;
            if (planItemInstances != null && !planItemInstances.isEmpty()) {
                for (PlanItemInstance planItemInstance : planItemInstances) {
                    if (planItemInstance.getState().equalsIgnoreCase(PlanItemInstanceState.WAITING_FOR_REPETITION)) {
                        waitingForRepetitionPlanItemInstances.add(planItemInstance);
                    } else if (planItemInstance.getState().equalsIgnoreCase(PlanItemInstanceState.ACTIVE)) {
                        activePlanItemInstance = planItemInstance;
                    }
                }
            }
            
            if (waitingForRepetitionPlanItemInstances.isEmpty()) {
                PlanItemInstanceEntity parentPlanItemInstance = null;
                if (planItem.getParentStage() != null && caseInstanceChangeState.getCreatedStageInstances().containsKey(planItem.getParentStage().getId())) {
                    parentPlanItemInstance = caseInstanceChangeState.getCreatedStageInstances().get(planItem.getParentStage().getId());
                    
                } else if (planItem.getParentStage() != null) {
                    List<PlanItemInstanceEntity> caseInstancePlanItemInstances = planItemInstanceEntityManager.findByCaseInstanceId(caseInstance.getId());
                    for (PlanItemInstanceEntity caseInstancePlanItemInstance : caseInstancePlanItemInstances) {
                        if (caseInstancePlanItemInstance.getPlanItemDefinitionId().equals(planItem.getParentStage().getId())) {
                            parentPlanItemInstance = caseInstancePlanItemInstance;
                            break;
                        }
                    }
                }

                if ((activePlanItemInstance == null && parentPlanItemInstance == null && evaluateCondition(caseInstance, planItemDefinitionMapping))
                        || (activePlanItemInstance instanceof PlanItemInstanceEntity
                        && evaluateCondition((PlanItemInstanceEntity) activePlanItemInstance, planItemDefinitionMapping))
                        || (parentPlanItemInstance != null && evaluateCondition(parentPlanItemInstance, planItemDefinitionMapping))) {

                    PlanItemInstanceEntity waitingForRepetitionPlanItemInstance = planItemInstanceEntityManager.createPlanItemInstanceEntityBuilder()
                            .planItem(planItem)
                            .caseDefinitionId(caseInstance.getCaseDefinitionId())
                            .caseInstanceId(caseInstance.getId())
                            .stagePlanItemInstance(parentPlanItemInstance)
                            .tenantId(caseInstance.getTenantId())
                            .addToParent(true)
                            .create();

                    if (planItem.getPlanItemDefinition() instanceof Stage) {
                        caseInstanceChangeState.addCreatedStageInstance(planItemDefinitionMapping.getPlanItemDefinitionId(), waitingForRepetitionPlanItemInstance);
                    }

                    CmmnHistoryManager cmmnHistoryManager = cmmnEngineConfiguration.getCmmnHistoryManager();
                    cmmnHistoryManager.recordPlanItemInstanceCreated(waitingForRepetitionPlanItemInstance);

                    waitingForRepetitionPlanItemInstance.setState(PlanItemInstanceState.WAITING_FOR_REPETITION);
                    cmmnHistoryManager.recordPlanItemInstanceAvailable(waitingForRepetitionPlanItemInstance);
                }
            }
        }
    }
    
    protected void executeRemoveWaitingForRepetitionPlanItemInstances(CaseInstanceChangeState caseInstanceChangeState, 
            CaseInstanceEntity caseInstance, CommandContext commandContext) {

        if (caseInstanceChangeState.getRemoveWaitingForRepetitionPlanItemDefinitions() == null || caseInstanceChangeState.getRemoveWaitingForRepetitionPlanItemDefinitions().isEmpty()) {
            return;
        }
        
        PlanItemInstanceEntityManager planItemInstanceEntityManager = cmmnEngineConfiguration.getPlanItemInstanceEntityManager();
        
        for (RemoveWaitingForRepetitionPlanItemDefinitionMapping planItemDefinitionMapping : caseInstanceChangeState.getRemoveWaitingForRepetitionPlanItemDefinitions()) {
            
            List<PlanItemInstance> planItemInstances = planItemInstanceEntityManager.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                    .planItemDefinitionId(planItemDefinitionMapping.getPlanItemDefinitionId())
                    .list();
            
            if (planItemInstances != null && !planItemInstances.isEmpty()) {
                for (PlanItemInstance planItemInstance : planItemInstances) {
                    if (planItemInstance.getState().equalsIgnoreCase(PlanItemInstanceState.WAITING_FOR_REPETITION)) {
                        PlanItemInstanceEntity planItemInstanceEntity = (PlanItemInstanceEntity) planItemInstance;

                        if (evaluateCondition(planItemInstanceEntity, planItemDefinitionMapping)) {
                            Date currentTime = cmmnEngineConfiguration.getClock().getCurrentTime();
                            CmmnHistoryManager cmmnHistoryManager = cmmnEngineConfiguration.getCmmnHistoryManager();
                            planItemInstanceEntity.setState(PlanItemInstanceState.TERMINATED);
                            planItemInstanceEntity.setEndedTime(currentTime);
                            planItemInstanceEntity.setTerminatedTime(currentTime);
                            cmmnHistoryManager.recordPlanItemInstanceTerminated(planItemInstanceEntity);

                            planItemInstanceEntityManager.delete(planItemInstanceEntity);
                        }
                    }
                }
            }
        }
    }
    
    protected void executeVerifySatisfiedSentryParts(CaseInstanceChangeState caseInstanceChangeState, 
            CaseInstanceEntity caseInstance, String originalCaseDefinitionId, CommandContext commandContext) {
        
        if (caseInstanceChangeState.getCaseDefinitionToMigrateTo() == null) {
            return;
        }
        
        SentryPartInstanceEntityManager sentryPartInstanceEntityManager = cmmnEngineConfiguration.getSentryPartInstanceEntityManager();
        List<SentryPartInstanceEntity> sentryPartInstances = sentryPartInstanceEntityManager.findSentryPartInstancesByCaseInstanceId(caseInstance.getId());
        if (sentryPartInstances.isEmpty()) {
            return;
        }
        
        Map<String, List<SentryPartInstanceEntity>> sentryInstanceMap = new HashMap<>();
        for (SentryPartInstanceEntity sentryPartInstanceEntity : sentryPartInstances) {
            if (!sentryInstanceMap.containsKey(sentryPartInstanceEntity.getPlanItemInstanceId())) {
                sentryInstanceMap.put(sentryPartInstanceEntity.getPlanItemInstanceId(), new ArrayList<>());
            }
            
            sentryInstanceMap.get(sentryPartInstanceEntity.getPlanItemInstanceId()).add(sentryPartInstanceEntity);
        }
        
        PlanItemInstanceEntityManager planItemInstanceEntityManager = cmmnEngineConfiguration.getPlanItemInstanceEntityManager();
        List<PlanItemInstanceEntity> planItemInstances = planItemInstanceEntityManager.findByCaseInstanceId(caseInstance.getId());
        
        CmmnDeploymentManager deploymentManager = cmmnEngineConfiguration.getDeploymentManager();
        CmmnModel targetCmmnModel = deploymentManager.resolveCaseDefinition(caseInstanceChangeState.getCaseDefinitionToMigrateTo()).getCmmnModel();
        
        for (PlanItemInstanceEntity planItemInstanceEntity : planItemInstances) {
            List<String> skipSentryPartInstanceForDeleteIds = new ArrayList<>();
            if (PlanItemInstanceState.AVAILABLE.equalsIgnoreCase(planItemInstanceEntity.getState()) && 
                    sentryInstanceMap.containsKey(planItemInstanceEntity.getId())) {
                
                if (planItemInstanceEntity.getPlanItem() == null) {
                    throw new FlowableException("Plan item could not be found for " + planItemInstanceEntity.getElementId());
                }
                
                if (planItemInstanceEntity.getPlanItem().getEntryCriteria().isEmpty()) {
                    continue;
                }
                
                for (Criterion criterion : planItemInstanceEntity.getPlanItem().getEntryCriteria()) {
                    verifySatisfiedSentryPartsForCriterion(criterion, planItemInstanceEntity, sentryInstanceMap, 
                            skipSentryPartInstanceForDeleteIds, false, targetCmmnModel, sentryPartInstanceEntityManager);
                }
                
            } else if (PlanItemInstanceState.ACTIVE.equalsIgnoreCase(planItemInstanceEntity.getState()) && 
                    sentryInstanceMap.containsKey(planItemInstanceEntity.getId())) {
                
                if (planItemInstanceEntity.getPlanItem() == null) {
                    throw new FlowableException("Plan item could not be found for " + planItemInstanceEntity.getElementId());
                }
                
                if (planItemInstanceEntity.getPlanItem().getExitCriteria().isEmpty()) {
                    continue;
                }
                
                for (Criterion criterion : planItemInstanceEntity.getPlanItem().getExitCriteria()) {
                    verifySatisfiedSentryPartsForCriterion(criterion, planItemInstanceEntity, sentryInstanceMap, 
                            skipSentryPartInstanceForDeleteIds, true, targetCmmnModel, sentryPartInstanceEntityManager);
                }
            }
            
            List<SentryPartInstanceEntity> planItemSentryInstances = sentryInstanceMap.get(planItemInstanceEntity.getId());
            if (planItemSentryInstances != null) {
                for (SentryPartInstanceEntity planItemSentryInstanceEntity : planItemSentryInstances) {
                    if (!skipSentryPartInstanceForDeleteIds.contains(planItemSentryInstanceEntity.getId())) {
                        sentryPartInstanceEntityManager.delete(planItemSentryInstanceEntity);
                    }
                }
            }
        }
        
        if (sentryInstanceMap.containsKey(null)) {
            List<String> skipSentryPartInstanceForDeleteIds = new ArrayList<>();
            CaseDefinition sourceCaseDefinition = cmmnEngineConfiguration.getCaseDefinitionEntityManager().findById(originalCaseDefinitionId);
            CmmnModel sourceCmmnModel = deploymentManager.resolveCaseDefinition(sourceCaseDefinition).getCmmnModel();
            Case sourceCase = sourceCmmnModel.getCaseById(sourceCaseDefinition.getKey());
            Case targetCase = targetCmmnModel.getCaseById(caseInstance.getCaseDefinitionKey());
            if (!sourceCase.getPlanModel().getExitCriteria().isEmpty()) {
                for (Criterion criterion : sourceCase.getPlanModel().getExitCriteria()) {
                    Sentry sentry = criterion.getSentry();
                    if (sentry.getOnParts().size() > 1 || 
                            (!sentry.getOnParts().isEmpty() && sentry.getSentryIfPart() != null)) {
                        
                        List<SentryPartInstanceEntity> planItemSentryInstances = sentryInstanceMap.get(null);
                        if (sentry.getSentryIfPart() != null) {
                            for (SentryPartInstanceEntity planItemSentryInstanceEntity : planItemSentryInstances) {
                                if (sentry.getSentryIfPart().getId().equals(planItemSentryInstanceEntity.getIfPartId())) {
                                    for (Criterion targetCriterion : targetCase.getPlanModel().getExitCriteria()) {
                                        if (targetCriterion.getSentry().getSentryIfPart() == null) {
                                            continue;
                                        }
                                        
                                        SentryIfPart targetSentryIfPart = targetCriterion.getSentry().getSentryIfPart();
                                        
                                        if (criterion.getAttachedToRefId().equals(targetCriterion.getAttachedToRefId()) &&
                                                sentry.getId().equals(targetCriterion.getSentryRef()) &&
                                                sentry.getSentryIfPart().getCondition().equals(targetSentryIfPart.getCondition())) {
                                            
                                            if (!sentry.isOnEventTriggerMode() && targetCriterion.getSentry().isOnEventTriggerMode()) {
                                                continue;
                                            }
                                            
                                            skipSentryPartInstanceForDeleteIds.add(planItemSentryInstanceEntity.getId());
                                            
                                            if (!planItemSentryInstanceEntity.getIfPartId().equals(targetSentryIfPart.getId())) {
                                                planItemSentryInstanceEntity.setIfPartId(targetSentryIfPart.getId());
                                                sentryPartInstanceEntityManager.update(planItemSentryInstanceEntity);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        for (SentryOnPart sentryOnPart : sentry.getOnParts()) {
                            for (SentryPartInstanceEntity planItemSentryInstanceEntity : planItemSentryInstances) {
                                if (sentryOnPart.getId().equals(planItemSentryInstanceEntity.getOnPartId())) {
                                    for (Criterion targetCriterion : targetCase.getPlanModel().getExitCriteria()) {
                                        if (targetCriterion.getSentry().getOnParts().isEmpty()) {
                                            continue;
                                        }
                                        
                                        for (SentryOnPart targetSentryOnPart : targetCriterion.getSentry().getOnParts()) {
                                            if (criterion.getAttachedToRefId().equals(targetCriterion.getAttachedToRefId()) &&
                                                    sentryOnPart.getSourceRef().equals(targetSentryOnPart.getSourceRef()) &&
                                                    sentry.getId().equals(targetCriterion.getSentryRef()) &&
                                                    sentryOnPart.getStandardEvent().equals(targetSentryOnPart.getStandardEvent())) {
                                                
                                                if (!sentry.isOnEventTriggerMode() && targetCriterion.getSentry().isOnEventTriggerMode()) {
                                                    continue;
                                                }
                                                
                                                skipSentryPartInstanceForDeleteIds.add(planItemSentryInstanceEntity.getId());
                                                
                                                if (!planItemSentryInstanceEntity.getOnPartId().equals(targetSentryOnPart.getId())) {
                                                    planItemSentryInstanceEntity.setOnPartId(targetSentryOnPart.getId());
                                                    sentryPartInstanceEntityManager.update(planItemSentryInstanceEntity);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            List<SentryPartInstanceEntity> planItemSentryInstances = sentryInstanceMap.get(null);
            for (SentryPartInstanceEntity planItemSentryInstanceEntity : planItemSentryInstances) {
                if (!skipSentryPartInstanceForDeleteIds.contains(planItemSentryInstanceEntity.getId())) {
                    sentryPartInstanceEntityManager.delete(planItemSentryInstanceEntity);
                }
            }
        }
        
    }
    
    protected void verifySatisfiedSentryPartsForCriterion(Criterion criterion, PlanItemInstanceEntity planItemInstanceEntity,
            Map<String, List<SentryPartInstanceEntity>> sentryInstanceMap, List<String> skipSentryPartInstanceForDeleteIds, 
            boolean isExitCriterion, CmmnModel cmmnModel, SentryPartInstanceEntityManager sentryPartInstanceEntityManager) {
        
        Sentry sentry = criterion.getSentry();
        if (sentry.getOnParts().size() > 1 || 
                (!sentry.getOnParts().isEmpty() && sentry.getSentryIfPart() != null)) {
            
            List<SentryPartInstanceEntity> planItemSentryInstances = sentryInstanceMap.get(planItemInstanceEntity.getId());
            if (sentry.getSentryIfPart() != null) {
                for (SentryPartInstanceEntity planItemSentryInstanceEntity : planItemSentryInstances) {
                    if (sentry.getSentryIfPart().getId().equals(planItemSentryInstanceEntity.getIfPartId())) {
                        PlanItem targetPlanItem = cmmnModel.findPlanItemByPlanItemDefinitionId(planItemInstanceEntity.getPlanItemDefinitionId());
                        if (targetPlanItem == null) {
                            continue;
                        }
                        
                        List<Criterion> targetCriteria = null;
                        if (isExitCriterion) {
                            targetCriteria = targetPlanItem.getExitCriteria();
                        } else {
                            targetCriteria = targetPlanItem.getEntryCriteria();
                        }
                        
                        for (Criterion targetCriterion : targetCriteria) {
                            if (targetCriterion.getSentry().getSentryIfPart() == null) {
                                continue;
                            }
                            
                            SentryIfPart targetSentryIfPart = targetCriterion.getSentry().getSentryIfPart();
                            
                            if (criterion.getAttachedToRefId().equals(targetCriterion.getAttachedToRefId()) &&
                                    sentry.getId().equals(targetCriterion.getSentryRef()) &&
                                    sentry.getSentryIfPart().getCondition().equals(targetSentryIfPart.getCondition())) {
                                
                                if (!sentry.isOnEventTriggerMode() && targetCriterion.getSentry().isOnEventTriggerMode()) {
                                    continue;
                                }
                                
                                skipSentryPartInstanceForDeleteIds.add(planItemSentryInstanceEntity.getId());
                                
                                if (!planItemSentryInstanceEntity.getIfPartId().equals(targetSentryIfPart.getId())) {
                                    planItemSentryInstanceEntity.setIfPartId(targetSentryIfPart.getId());
                                    sentryPartInstanceEntityManager.update(planItemSentryInstanceEntity);
                                }
                            }
                        }
                    }
                }
            }
            
            for (SentryOnPart sentryOnPart : sentry.getOnParts()) {
                for (SentryPartInstanceEntity planItemSentryInstanceEntity : planItemSentryInstances) {
                    if (sentryOnPart.getId().equals(planItemSentryInstanceEntity.getOnPartId())) {
                        PlanItem targetPlanItem = cmmnModel.findPlanItemByPlanItemDefinitionId(planItemInstanceEntity.getPlanItemDefinitionId());
                        if (targetPlanItem == null) {
                            continue;
                        }
                        
                        List<Criterion> targetCriteria = null;
                        if (isExitCriterion) {
                            targetCriteria = targetPlanItem.getExitCriteria();
                        } else {
                            targetCriteria = targetPlanItem.getEntryCriteria();
                        }
                        
                        for (Criterion targetCriterion : targetCriteria) {
                            if (targetCriterion.getSentry().getOnParts().isEmpty()) {
                                continue;
                            }
                            
                            for (SentryOnPart targetSentryOnPart : targetCriterion.getSentry().getOnParts()) {
                                if (criterion.getAttachedToRefId().equals(targetCriterion.getAttachedToRefId()) &&
                                        sentryOnPart.getSourceRef().equals(targetSentryOnPart.getSourceRef()) &&
                                        sentry.getId().equals(targetCriterion.getSentryRef()) &&
                                        sentryOnPart.getStandardEvent().equals(targetSentryOnPart.getStandardEvent())) {
                                    
                                    if (!sentry.isOnEventTriggerMode() && targetCriterion.getSentry().isOnEventTriggerMode()) {
                                        continue;
                                    }
                                    
                                    skipSentryPartInstanceForDeleteIds.add(planItemSentryInstanceEntity.getId());
                                    
                                    if (!planItemSentryInstanceEntity.getOnPartId().equals(targetSentryOnPart.getId())) {
                                        planItemSentryInstanceEntity.setOnPartId(targetSentryOnPart.getId());
                                        sentryPartInstanceEntityManager.update(planItemSentryInstanceEntity);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    protected void executeTerminatePlanItemInstances(CaseInstanceChangeState caseInstanceChangeState, CaseInstanceEntity caseInstance, CommandContext commandContext) {
        if (caseInstanceChangeState.getTerminatePlanItemDefinitions() == null || caseInstanceChangeState.getTerminatePlanItemDefinitions().isEmpty()) {
            return;
        }
        
        Map<String, List<PlanItemInstanceEntity>> currentPlanItemInstanceMap = caseInstanceChangeState.getCurrentPlanItemInstances();
        for (TerminatePlanItemDefinitionMapping planItemDefinitionMapping : caseInstanceChangeState.getTerminatePlanItemDefinitions()) {
            if (currentPlanItemInstanceMap.containsKey(planItemDefinitionMapping.getPlanItemDefinitionId())) {
                List<PlanItemInstanceEntity> currentPlanItemInstanceList = currentPlanItemInstanceMap.get(planItemDefinitionMapping.getPlanItemDefinitionId());
                for (PlanItemInstanceEntity planItemInstance : currentPlanItemInstanceList) {
                    if (!PlanItemInstanceState.TERMINAL_STATES.contains(planItemInstance.getState()) && 
                            !PlanItemInstanceState.WAITING_FOR_REPETITION.equals(planItemInstance.getState()) &&
                            evaluateCondition(planItemInstance, planItemDefinitionMapping)) {
                        
                        terminatePlanItemInstance(planItemInstance, commandContext);
                        caseInstanceChangeState.addTerminatedPlanItemInstance(planItemInstance.getPlanItemDefinitionId(), planItemInstance);
                    }
                }
            }
        }
    }
    
    protected void executeTerminateNonExistingPlanItemInstancesInTargetCmmnModel(CaseInstanceChangeState caseInstanceChangeState, CommandContext commandContext) {
        if (caseInstanceChangeState.getCaseDefinitionToMigrateTo() != null) {
            CmmnModel targetCmmnModel = CaseDefinitionUtil.getCmmnModel(caseInstanceChangeState.getCaseDefinitionToMigrateTo().getId());
            List<String> excludePlanItemDefinitionIds = new ArrayList<>();
            for (TerminatePlanItemDefinitionMapping planItemDefinitionMapping : caseInstanceChangeState.getTerminatePlanItemDefinitions()) {
                excludePlanItemDefinitionIds.add(planItemDefinitionMapping.getPlanItemDefinitionId());
            }
            
            for (ChangePlanItemDefinitionWithNewTargetIdsMapping newTargetIdsMapping : caseInstanceChangeState.getChangePlanItemDefinitionWithNewTargetIds()) {
                excludePlanItemDefinitionIds.add(newTargetIdsMapping.getExistingPlanItemDefinitionId());
            }
            
            for (ChangePlanItemIdWithDefinitionIdMapping definitionIdMapping : caseInstanceChangeState.getChangePlanItemIdsWithDefinitionId()) {
                excludePlanItemDefinitionIds.add(definitionIdMapping.getExistingPlanItemDefinitionId());
            }
            
            for (String currentPlanItemDefinitionId : caseInstanceChangeState.getCurrentPlanItemInstances().keySet()) {
                if (!excludePlanItemDefinitionIds.contains(currentPlanItemDefinitionId) && targetCmmnModel.findPlanItemDefinition(currentPlanItemDefinitionId) == null) {
                    for (PlanItemInstanceEntity currentPlanItemInstance : caseInstanceChangeState.getCurrentPlanItemInstances().get(currentPlanItemDefinitionId)) {
                        if (!PlanItemInstanceState.TERMINAL_STATES.contains(currentPlanItemInstance.getState())) {
                            terminatePlanItemInstance(currentPlanItemInstance, commandContext);
                            caseInstanceChangeState.addTerminatedPlanItemInstance(currentPlanItemInstance.getPlanItemDefinitionId(), currentPlanItemInstance);
                        }
                    }
                }
            }
        }
    }
    
    protected abstract boolean isDirectPlanItemDefinitionMigration(PlanItemDefinition currentPlanItemDefinition, PlanItemDefinition newPlanItemDefinition);

    protected Map<String, List<PlanItemInstanceEntity>> retrievePlanItemInstances(String caseInstanceId) {
        PlanItemInstanceEntityManager planItemInstanceEntityManager = cmmnEngineConfiguration.getPlanItemInstanceEntityManager();
        List<PlanItemInstanceEntity> planItemInstances = planItemInstanceEntityManager.findByCaseInstanceId(caseInstanceId);
        
        Map<String, List<PlanItemInstanceEntity>> stagesByPlanItemDefinitionId = planItemInstances.stream()
            .collect(Collectors.groupingBy(PlanItemInstance::getPlanItemDefinitionId));
        return stagesByPlanItemDefinitionId;
    }
    
    protected void setCaseDefinitionIdForPlanItemInstances(Map<String, List<PlanItemInstanceEntity>> stagesByPlanItemDefinitionId, CaseDefinition caseDefinition) {
        if (caseDefinition != null) {
            for (List<PlanItemInstanceEntity> planItemInstances : stagesByPlanItemDefinitionId.values()) {
                for (PlanItemInstanceEntity planItemInstance : planItemInstances) {
                    planItemInstance.setCaseDefinitionId(caseDefinition.getId());
                }
            }
        }
    }
    
    protected void navigatePlanItemInstances(Map<String, List<PlanItemInstanceEntity>> stagesByPlanItemDefinitionId, CaseDefinition caseDefinition, String originalCaseDefinitionId) {
        if (caseDefinition != null) {
            TaskService taskService = cmmnEngineConfiguration.getTaskServiceConfiguration().getTaskService();
            CmmnModel originalCmmnModel = CaseDefinitionUtil.getCmmnModel(originalCaseDefinitionId);
            CmmnModel targetCmmnModel = CaseDefinitionUtil.getCmmnModel(caseDefinition.getId());
            for (List<PlanItemInstanceEntity> planItemInstances : stagesByPlanItemDefinitionId.values()) {
                for (PlanItemInstanceEntity planItemInstance : planItemInstances) {
                    
                    if (!PlanItemInstanceState.AVAILABLE.equals(planItemInstance.getState()) && 
                            planItemInstance.getPlanItemDefinition() instanceof HumanTask) {
                        
                        TaskEntityImpl task = (TaskEntityImpl) taskService.createTaskQuery(cmmnEngineConfiguration.getCommandExecutor(), cmmnEngineConfiguration)
                                .subScopeId(planItemInstance.getId()).scopeType(ScopeTypes.CMMN).singleResult();
                        if (task != null) {
                            task.setScopeDefinitionId(caseDefinition.getId());
                            PlanItemDefinition originalTaskDef = originalCmmnModel.findPlanItemDefinition(task.getTaskDefinitionKey());
                            PlanItemDefinition targetTaskDef = targetCmmnModel.findPlanItemDefinition(task.getTaskDefinitionKey());
                            if (originalTaskDef != null && targetTaskDef != null && originalTaskDef instanceof HumanTask originalHumanTask && targetTaskDef instanceof HumanTask targetHumanTask) {

                                if (taskPropertyValueIsDifferent(originalHumanTask.getName(), targetHumanTask.getName())) {
                                    task.setName(targetHumanTask.getName());
                                }
                                
                                if (taskPropertyValueIsDifferent(originalHumanTask.getFormKey(), targetHumanTask.getFormKey())) {
                                    task.setFormKey(targetHumanTask.getFormKey());
                                }
                                
                                if (taskPropertyValueIsDifferent(originalHumanTask.getCategory(), targetHumanTask.getCategory())) {
                                    task.setCategory(targetHumanTask.getCategory());
                                }
                                
                                if (taskPropertyValueIsDifferent(originalHumanTask.getDocumentation(), targetHumanTask.getDocumentation())) {
                                    task.setDescription(targetHumanTask.getDocumentation());
                                }
                            }
                            
                            CmmnHistoryManager cmmnHistoryManager = cmmnEngineConfiguration.getCmmnHistoryManager();
                            cmmnHistoryManager.recordTaskInfoChange(task, cmmnEngineConfiguration.getClock().getCurrentTime());
                        }
                    }
                }
            }
        }
    }

    protected boolean isStageContainerOfAnyPlanItemDefinition(String stageId, Collection<PlanItemMoveEntry> moveToPlanItems) {
        Optional<Stage> isUsed = moveToPlanItems.stream()
            .map(PlanItemMoveEntry::getNewPlanItem)
            .map(PlanItem::getPlanItemDefinition)
            .map(PlanItemDefinition::getParentStage)
            .filter(Objects::nonNull)
            .filter(elementStage -> elementStage.getId().equals(stageId))
            .findAny();

        return isUsed.isPresent();
    }

    protected PlanItemInstanceEntity resolveParentPlanItemInstanceToDelete(PlanItemInstanceEntity planItemInstance, List<PlanItemMoveEntry> moveToPlanItems) {
        if (planItemInstance.getStageInstanceId() == null) {
            return  null;
        }
        
        PlanItemInstanceEntity parentPlanItemInstance = planItemInstance.getStagePlanItemInstanceEntity();

        if (!isStageContainerOfAnyPlanItemDefinition(parentPlanItemInstance.getPlanItemDefinitionId(), moveToPlanItems)) {
            PlanItemInstanceEntity stageParentExecution = resolveParentPlanItemInstanceToDelete(parentPlanItemInstance, moveToPlanItems);
            if (stageParentExecution != null) {
                return stageParentExecution;
            } else {
                return parentPlanItemInstance;
            }
        }

        return null;
    }
    
    protected PlanItemInstanceEntity createStagesAndPlanItemInstances(PlanItem planItem, CaseInstanceEntity caseInstance, 
            CaseInstanceChangeState caseInstanceChangeState, ActivatePlanItemDefinitionMapping planItemDefinitionMapping, CommandContext commandContext) {
        
        PlanItemInstanceEntityManager planItemInstanceEntityManager = cmmnEngineConfiguration.getPlanItemInstanceEntityManager();
        
        Map<String, Stage> stagesToCreate = new HashMap<>();
        PlanItemDefinition planItemDefinition = planItem.getPlanItemDefinition();
        Stage stage = planItemDefinition.getParentStage();
            
        Map<String, List<PlanItemInstanceEntity>> runtimePlanItemInstanceMap = caseInstanceChangeState.getRuntimePlanItemInstances();
        PlanItemInstanceEntity closestAncestorPlanItemInstance = null;
        while (stage != null) {
            if (!stage.isPlanModel() && !caseInstanceChangeState.getCreatedStageInstances().containsKey(stage.getId())) {
                PlanItemInstanceEntity stageAncestorInstance = getStageAncestorOfAnyPlanItemInstance(stage.getId(), runtimePlanItemInstanceMap);
                if (stageAncestorInstance == null) {
                    stagesToCreate.put(stage.getId(), stage);
                } else if (closestAncestorPlanItemInstance == null) {
                    closestAncestorPlanItemInstance = stageAncestorInstance;
                }
            }
            stage = stage.getParentStage();
        }

        if (closestAncestorPlanItemInstance == null) {
            if (!evaluateCondition(caseInstance, planItemDefinitionMapping)) {
                return null;
            }
        } else {
            if (!evaluateCondition(closestAncestorPlanItemInstance, planItemDefinitionMapping)) {
                return null;
            }
        }

        // Build the stage hierarchy
        for (Stage stageToCreate : stagesToCreate.values()) {
            if (!caseInstanceChangeState.getCreatedStageInstances().containsKey(stageToCreate.getId())) {
                PlanItemInstanceEntity stageInstance = createStageHierarchy(stageToCreate, null, stagesToCreate, 
                        caseInstanceChangeState, caseInstance, commandContext);
                caseInstanceChangeState.addCreatedStageInstance(stageToCreate.getId(), stageInstance);
            }
        }
        
        // Adds the plan item instance (leaf) to the stage instance
        PlanItemInstanceEntity parentPlanItemInstance = null;
        if (planItemDefinition.getParentStage() != null) {
            String parentStageId = planItemDefinition.getParentStage().getId();
            if (caseInstanceChangeState.getCreatedStageInstances().containsKey(parentStageId)) {
                parentPlanItemInstance = caseInstanceChangeState.getCreatedStageInstances().get(parentStageId);
            
            } else {
                PlanItemInstanceEntity possibleParentPlanItemInstance = caseInstanceChangeState.getRuntimePlanItemInstance(parentStageId);
                if (possibleParentPlanItemInstance != null) {
                    parentPlanItemInstance = possibleParentPlanItemInstance;
                    if (PlanItemInstanceState.AVAILABLE.equals(parentPlanItemInstance.getState())) {
                        parentPlanItemInstance.setState(PlanItemInstanceState.ACTIVE);
                        planItemInstanceEntityManager.update(parentPlanItemInstance);
                        
                        if (!parentPlanItemInstance.getPlanItem().getEntryCriteria().isEmpty() && hasRepetitionRule(parentPlanItemInstance)) {
                            if (evaluateRepetitionRule(parentPlanItemInstance, commandContext)) {
                                createPlanItemInstanceDuplicateForRepetition(parentPlanItemInstance, commandContext);
                            }
                        }
                    }
                }
            }
        }
            
        PlanItemInstanceEntity newPlanItemInstance = null;
        for (List<PlanItemInstanceEntity> existingPlanItemInstances : runtimePlanItemInstanceMap.values()) {
            for (PlanItemInstanceEntity existingPlanItemInstance : existingPlanItemInstances) {
                if (existingPlanItemInstance.getPlanItemDefinitionId().equals(planItemDefinition.getId()) &&
                        PlanItemInstanceState.AVAILABLE.equals(existingPlanItemInstance.getState())) {
                    
                    newPlanItemInstance = existingPlanItemInstance;
                }
            }
        }
            
        if (newPlanItemInstance == null) {
            newPlanItemInstance = planItemInstanceEntityManager.createPlanItemInstanceEntityBuilder()
                .planItem(planItem)
                .caseDefinitionId(caseInstance.getCaseDefinitionId())
                .caseInstanceId(caseInstance.getId())
                .stagePlanItemInstance(parentPlanItemInstance)
                .tenantId(caseInstance.getTenantId())
                .addToParent(true)
                .create();
            
            CmmnHistoryManager cmmnHistoryManager = cmmnEngineConfiguration.getCmmnHistoryManager();
            cmmnHistoryManager.recordPlanItemInstanceCreated(newPlanItemInstance);

            createChildPlanItemInstancesForStage(Collections.singletonList(newPlanItemInstance), runtimePlanItemInstanceMap,
                    caseInstanceChangeState.getTerminatedPlanItemInstances(), Collections.singleton(planItem.getId()), 
                    caseInstanceChangeState, commandContext);
        }

        return newPlanItemInstance;
    }

    protected void createChildPlanItemInstancesForStage(List<PlanItemInstanceEntity> newPlanItemInstances, Map<String, List<PlanItemInstanceEntity>> runtimePlanItemInstanceMap,
            Map<String, PlanItemInstanceEntity> terminatedPlanItemInstances, Set<String> newPlanItemInstanceIds, 
            CaseInstanceChangeState caseInstanceChangeState, CommandContext commandContext) {
        
        if (newPlanItemInstances.size() == 0) {
            return;
        }
        
        PlanItemInstanceEntity newPlanItemInstance = newPlanItemInstances.get(0);
        PlanItem planItem = newPlanItemInstance.getPlanItem();
        if (planItem != null && planItem.getParentStage() != null) {
            for (PlanItem stagePlanItem : planItem.getParentStage().getPlanItems()) {
                if (!newPlanItemInstanceIds.contains(stagePlanItem.getId()) && !runtimePlanItemInstanceMap.containsKey(stagePlanItem.getPlanItemDefinition().getId()) 
                        && !terminatedPlanItemInstances.containsKey(stagePlanItem.getPlanItemDefinition().getId())
                        && !caseInstanceChangeState.getCurrentPlanItemInstances().containsKey(stagePlanItem.getPlanItemDefinition().getId())) {
                    
                    PlanItemInstance parentStagePlanItem = newPlanItemInstance.getStagePlanItemInstanceEntity();
                    if (parentStagePlanItem == null && newPlanItemInstance.getStageInstanceId() != null) {
                        parentStagePlanItem = CommandContextUtil.getPlanItemInstanceEntityManager(commandContext).findById(newPlanItemInstance.getStageInstanceId());
                    }
                    
                    if (stagePlanItem.getPlanItemDefinition() instanceof Stage) {
                        PlanItemInstanceEntity childStagePlanItemInstance = cmmnEngineConfiguration.getPlanItemInstanceEntityManager()
                            .createPlanItemInstanceEntityBuilder()
                            .planItem(stagePlanItem)
                            .caseDefinitionId(newPlanItemInstance.getCaseDefinitionId())
                            .caseInstanceId(newPlanItemInstance.getCaseInstanceId())
                            .stagePlanItemInstance(parentStagePlanItem)
                            .tenantId(newPlanItemInstance.getTenantId())
                            .addToParent(true)
                            .create();
    
                        CommandContextUtil.getAgenda(commandContext).planCreatePlanItemInstanceOperation(childStagePlanItemInstance);
                    }
                }
            }
        }
    }

    protected PlanItemInstanceEntity getStageAncestorOfAnyPlanItemInstance(String stageId, Map<String, List<PlanItemInstanceEntity>> planItemInstanceMap) {
        for (List<PlanItemInstanceEntity> planItemInstanceList : planItemInstanceMap.values()) {
            for (PlanItemInstanceEntity planItemInstance : planItemInstanceList) {
                if (planItemInstance.getPlanItem() != null) {
                    PlanItemDefinition planItemDefinition = planItemInstance.getPlanItem().getPlanItemDefinition();

                    if (planItemDefinition.getId().equals(stageId)) {
                        return planItemInstance;
                    }

                    if (isStageAncestor(stageId, planItemDefinition)) {
                        return planItemInstance;
                    }
                }
            }
        }
        return null;
    }

    protected boolean isStageAncestor(String stageId, PlanItemDefinition planItemDefinition) {
        while (planItemDefinition.getParentStage() != null) {
            String currentStageId = planItemDefinition.getParentStage().getId();
            if (currentStageId != null && currentStageId.equals(stageId)) {
                return true;
            }
            planItemDefinition = planItemDefinition.getParentStage();
        }
        return false;
    }

    protected PlanItemInstanceEntity createStageHierarchy(Stage stage, PlanItemInstanceEntity defaultParentPlanItemInstance, 
            Map<String, Stage> stagesToCreate, CaseInstanceChangeState caseInstanceChangeState, 
            CaseInstanceEntity caseInstance, CommandContext commandContext) {
        
        Map<String, List<PlanItemInstanceEntity>> runtimePlanItemInstanceMap = caseInstanceChangeState.getRuntimePlanItemInstances();
        if (runtimePlanItemInstanceMap.containsKey(stage.getId())) {
            return (PlanItemInstanceEntity) runtimePlanItemInstanceMap.get(stage.getId()).get(0);
        }

        if (caseInstanceChangeState.getCreatedStageInstances().containsKey(stage.getId())) {
            return caseInstanceChangeState.getCreatedStageInstances().get(stage.getId());
        }

        // Create the parent, if needed
        PlanItemInstanceEntity parentStageInstance = defaultParentPlanItemInstance;
        if (stage.getParentStage() != null && !stage.getParentStage().isPlanModel()) {
            parentStageInstance = createStageHierarchy(stage.getParentStage(), defaultParentPlanItemInstance, stagesToCreate, 
                            caseInstanceChangeState, caseInstance, commandContext);
            caseInstanceChangeState.getCreatedStageInstances().put(stage.getParentStage().getId(), parentStageInstance);
        }
        
        PlanItemInstanceEntityManager planItemInstanceEntityManager = cmmnEngineConfiguration.getPlanItemInstanceEntityManager();
        PlanItemInstanceEntity newPlanItemInstance = planItemInstanceEntityManager.createPlanItemInstanceEntityBuilder()
                .planItem(stage.getPlanItem())
                .caseDefinitionId(caseInstance.getCaseDefinitionId())
                .caseInstanceId(caseInstance.getId())
                .stagePlanItemInstance(parentStageInstance)
                .tenantId(caseInstance.getTenantId())
                .addToParent(true)
                .create();
        
        // Special care needed in case the plan item instance is repeating
        if (!newPlanItemInstance.getPlanItem().getEntryCriteria().isEmpty() && hasRepetitionRule(newPlanItemInstance)) {
            if (evaluateRepetitionRule(newPlanItemInstance, commandContext)) {
                createPlanItemInstanceDuplicateForRepetition(newPlanItemInstance, commandContext);
            }
        }
        
        CommandContextUtil.getAgenda(commandContext).planStartPlanItemInstanceOperation(newPlanItemInstance, null);

        return newPlanItemInstance;
    }
    
    protected void terminatePlanItemInstance(PlanItemInstanceEntity planItemInstance, CommandContext commandContext) {
        String currentPlanItemInstanceState = planItemInstance.getState();
        
        Date currentTime = cmmnEngineConfiguration.getClock().getCurrentTime();
        planItemInstance.setEndedTime(currentTime);
        planItemInstance.setTerminatedTime(currentTime);
        planItemInstance.setState(PlanItemInstanceState.TERMINATED);
        
        CommandContextUtil.getCmmnHistoryManager(commandContext).recordPlanItemInstanceTerminated(planItemInstance);
        
        cmmnEngineConfiguration.getListenerNotificationHelper().executeLifecycleListeners(
                commandContext, planItemInstance, currentPlanItemInstanceState, planItemInstance.getState());
        
        if (planItemInstance.getPlanItem() != null) {
            PlanItemDefinition planItemDefinition = planItemInstance.getPlanItem().getPlanItemDefinition();
            if (planItemDefinition instanceof HumanTask) {
                if (PlanItemInstanceState.ACTIVE.equals(currentPlanItemInstanceState)) {
                    TaskService taskService = cmmnEngineConfiguration.getTaskServiceConfiguration().getTaskService();
                    List<TaskEntity> taskEntities = taskService.findTasksBySubScopeIdScopeType(planItemInstance.getId(), ScopeTypes.CMMN);
                    if (taskEntities == null || taskEntities.isEmpty()) {
                        throw new FlowableException("No task entity found for plan item instance " + planItemInstance.getId());
                    }
        
                    // Should be only one
                    for (TaskEntity taskEntity : taskEntities) {
                        if (!taskEntity.isDeleted()) {
                            TaskHelper.deleteTask(taskEntity, "Change plan item state", false, false, cmmnEngineConfiguration);
                        }
                    }
                }
                
            } else if (planItemDefinition instanceof Stage) {
                deleteChildPlanItemInstances(planItemInstance, commandContext);
            
            } else if (planItemDefinition instanceof ProcessTask) {
                if (planItemInstance.getReferenceId() != null) {
                    cmmnEngineConfiguration.getProcessInstanceService().deleteProcessInstance(planItemInstance.getReferenceId());
                }

            } else if (planItemDefinition instanceof EventListener) {
                
                if (planItemDefinition instanceof TimerEventListener) {
                    JobService jobService = cmmnEngineConfiguration.getJobServiceConfiguration().getJobService();
                    List<Job> timerJobs = jobService.createTimerJobQuery()
                        .caseInstanceId(planItemInstance.getCaseInstanceId())
                        .planItemInstanceId(planItemInstance.getId())
                        .elementId(planItemInstance.getPlanItemDefinitionId())
                        .list();
                    
                    if (timerJobs != null && !timerJobs.isEmpty()) {
                        for (Job job : timerJobs) {
                            cmmnEngineConfiguration.getJobServiceConfiguration().getTimerJobEntityManager().delete(job.getId());
                        }
                    }
                
                } else if (!(planItemDefinition instanceof UserEventListener)) {
                    EventSubscriptionService eventSubscriptionService = cmmnEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService();
                    List<EventSubscriptionEntity> eventSubscriptions = eventSubscriptionService.findEventSubscriptionsBySubScopeId(planItemInstance.getId());
                    
                    if (eventSubscriptions != null && !eventSubscriptions.isEmpty()) {
                        for (EventSubscriptionEntity eventSubscription : eventSubscriptions) {
                            eventSubscriptionService.deleteEventSubscription(eventSubscription);
                        }
                    }
                }
            } else if (planItemDefinition instanceof CaseTask) {
                if (planItemInstance.getReferenceId() != null) {
                    cmmnEngineConfiguration.getCmmnRuntimeService().deleteCaseInstance(planItemInstance.getReferenceId());
                }
            }
        }
    }
    
    protected void deleteChildPlanItemInstances(PlanItemInstanceEntity planItemInstance, CommandContext commandContext) {
        List<PlanItemInstanceEntity> childPlanItemInstances = planItemInstance.getChildPlanItemInstances();
        if (childPlanItemInstances != null) {
            for (PlanItemInstanceEntity childPlanItemInstance : childPlanItemInstances) {
                deleteChildPlanItemInstances(childPlanItemInstance, commandContext);
                terminatePlanItemInstance(childPlanItemInstance, commandContext);
            }
        }
    }

    protected void handleHumanTaskNewAssignee(PlanItemInstanceEntity taskPlanItemInstance, String newAssigneeId, CommandContext commandContext) {
        TaskService taskService = cmmnEngineConfiguration.getTaskServiceConfiguration().getTaskService();
        TaskEntityImpl task = (TaskEntityImpl) taskService.createTaskQuery(cmmnEngineConfiguration.getCommandExecutor(), cmmnEngineConfiguration)
                .subScopeId(taskPlanItemInstance.getId()).scopeType(ScopeTypes.CMMN).singleResult();
        TaskHelper.changeTaskAssignee(task, newAssigneeId, cmmnEngineConfiguration);
    }

    protected boolean hasRepetitionRule(PlanItemInstanceEntity planItemInstanceEntity) {
        if (planItemInstanceEntity != null && planItemInstanceEntity.getPlanItem() != null) {
            return planItemInstanceEntity.getPlanItem().getItemControl() != null && 
                            planItemInstanceEntity.getPlanItem().getItemControl().getRepetitionRule() != null;
        }
        return false;
    }
    
    protected boolean evaluateRepetitionRule(PlanItemInstanceEntity planItemInstanceEntity, CommandContext commandContext) {
        if (hasRepetitionRule(planItemInstanceEntity)) {
            String repetitionCondition = planItemInstanceEntity.getPlanItem().getItemControl().getRepetitionRule().getCondition();
            return evaluateRepetitionRule(planItemInstanceEntity, repetitionCondition, commandContext);
        }
        return false;
    }

    protected boolean evaluateRepetitionRule(VariableContainer variableContainer, String repetitionCondition, CommandContext commandContext) {
        if (StringUtils.isNotEmpty(repetitionCondition)) {
            return ExpressionUtil.evaluateBooleanExpression(commandContext, variableContainer, repetitionCondition);
        } else {
            return true; // no condition set, but a repetition rule defined is assumed to be defaulting to true
        }
    }
    
    protected PlanItemInstanceEntity createPlanItemInstanceDuplicateForRepetition(PlanItemInstanceEntity planItemInstanceEntity, CommandContext commandContext) {
        PlanItemInstanceEntity childPlanItemInstanceEntity = copyAndInsertPlanItemInstance(commandContext, planItemInstanceEntity, false);

        String oldState = childPlanItemInstanceEntity.getState();
        String newState = PlanItemInstanceState.WAITING_FOR_REPETITION;
        childPlanItemInstanceEntity.setState(newState);
        cmmnEngineConfiguration.getListenerNotificationHelper().executeLifecycleListeners(commandContext, planItemInstanceEntity, oldState, newState);

        // createPlanItemInstance operations will also sync planItemInstance history
        CommandContextUtil.getAgenda(commandContext).planCreatePlanItemInstanceForRepetitionOperation(childPlanItemInstanceEntity);
        return childPlanItemInstanceEntity;
    }
    
    protected PlanItemInstanceEntity copyAndInsertPlanItemInstance(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntityToCopy, boolean addToParent) {
        PlanItemInstance stagePlanItem = planItemInstanceEntityToCopy.getStagePlanItemInstanceEntity();
        if (stagePlanItem == null && planItemInstanceEntityToCopy.getStageInstanceId() != null) {
            stagePlanItem = cmmnEngineConfiguration.getPlanItemInstanceEntityManager().findById(planItemInstanceEntityToCopy.getStageInstanceId());
        }

        PlanItemInstanceEntity planItemInstanceEntity = CommandContextUtil.getPlanItemInstanceEntityManager(commandContext).createPlanItemInstanceEntityBuilder()
            .planItem(planItemInstanceEntityToCopy.getPlanItem())
            .caseDefinitionId(planItemInstanceEntityToCopy.getCaseDefinitionId())
            .caseInstanceId(planItemInstanceEntityToCopy.getCaseInstanceId())
            .stagePlanItemInstance(stagePlanItem)
            .tenantId(planItemInstanceEntityToCopy.getTenantId())
            .addToParent(addToParent)
            .create();

        if (hasRepetitionRule(planItemInstanceEntityToCopy)) {
            RepetitionRule repetitionRule = planItemInstanceEntity.getPlanItem().getItemControl().getRepetitionRule();
            if (repetitionRule.getAggregations() != null || !repetitionRule.isIgnoreRepetitionCounterVariable()) {
                int counter = getRepetitionCounter(planItemInstanceEntityToCopy);
                setRepetitionCounter(planItemInstanceEntity, counter);
            }
        }

        return planItemInstanceEntity;
    }
    
    protected int getRepetitionCounter(PlanItemInstanceEntity repeatingPlanItemInstanceEntity) {
        Integer counter = (Integer) repeatingPlanItemInstanceEntity.getVariableLocal(getCounterVariable(repeatingPlanItemInstanceEntity));
        if (counter == null) {
            return 0;
        } else {
            return counter.intValue();
        }
    }
    
    protected void setRepetitionCounter(PlanItemInstanceEntity repeatingPlanItemInstanceEntity, int counterValue) {
        repeatingPlanItemInstanceEntity.setVariableLocal(getCounterVariable(repeatingPlanItemInstanceEntity), counterValue);
    }

    protected String getCounterVariable(PlanItemInstanceEntity repeatingPlanItemInstanceEntity) {
        String repetitionCounterVariableName = repeatingPlanItemInstanceEntity.getPlanItem().getItemControl().getRepetitionRule().getRepetitionCounterVariableName();
        return repetitionCounterVariableName;
    }
    
    protected boolean isExpression(String variableName) {
        return variableName.startsWith("${") || variableName.startsWith("#{");
    }

    protected CaseDefinition resolveCaseDefinition(String caseDefinitionKey, Integer caseDefinitionVersion, String tenantId, CommandContext commandContext) {
        CaseDefinitionEntityManager caseDefinitionEntityManager = CommandContextUtil.getCaseDefinitionEntityManager(commandContext);
        CaseDefinition caseDefinition = null;
        if (caseDefinitionVersion != null) {
            caseDefinition = caseDefinitionEntityManager.findCaseDefinitionByKeyAndVersionAndTenantId(caseDefinitionKey, caseDefinitionVersion, tenantId);
        } else {
            if (tenantId == null || CmmnEngineConfiguration.NO_TENANT_ID.equals(tenantId)) {
                caseDefinition = caseDefinitionEntityManager.findLatestCaseDefinitionByKey(caseDefinitionKey);
            } else {
                caseDefinition = caseDefinitionEntityManager.findLatestCaseDefinitionByKeyAndTenantId(caseDefinitionKey, tenantId);
            }
        }

        if (caseDefinition == null) {
            CmmnDeploymentManager deploymentManager = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getDeploymentManager();
            if (tenantId == null || CmmnEngineConfiguration.NO_TENANT_ID.equals(tenantId)) {
                caseDefinition = deploymentManager.findDeployedLatestCaseDefinitionByKey(caseDefinitionKey);
            } else {
                caseDefinition = deploymentManager.findDeployedLatestCaseDefinitionByKeyAndTenantId(caseDefinitionKey, tenantId);
            }
        }
        return caseDefinition;
    }

    protected String getCaseDefinitionIdToMigrateTo(CaseInstanceChangeState caseInstanceChangeState) {
        String caseDefinitionIdToMigrateTo = null;
        if (caseInstanceChangeState.getCaseDefinitionToMigrateTo() != null) {
            caseDefinitionIdToMigrateTo = caseInstanceChangeState.getCaseDefinitionToMigrateTo().getId();
        }
        return caseDefinitionIdToMigrateTo;
    }

    protected <T extends PlanItemDefinitionMapping> boolean evaluateCondition(VariableContainer variableContainer, T planItemDefinitionMapping) {
        if (planItemDefinitionMapping.getCondition() != null) {
            Object conditionResult = cmmnEngineConfiguration.getExpressionManager().createExpression(planItemDefinitionMapping.getCondition())
                    .getValue(variableContainer);
            return conditionResult instanceof Boolean condition && condition;
        }
        return true;
    }

    protected boolean taskPropertyValueIsDifferent(String originalValue, String targetValue) {
        return (StringUtils.isNotEmpty(originalValue) && !originalValue.equals(targetValue)) ||
                (StringUtils.isEmpty(originalValue) && StringUtils.isNotEmpty(targetValue));
    }
}
