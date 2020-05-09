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
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.agenda.CmmnEngineAgenda;
import org.flowable.cmmn.engine.impl.deployer.CmmnDeploymentManager;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseDefinitionEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntityManager;
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionUtil;
import org.flowable.cmmn.engine.impl.runtime.MovePlanItemInstanceEntityContainer.PlanItemMoveEntry;
import org.flowable.cmmn.engine.impl.task.TaskHelper;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.impl.util.ExpressionUtil;
import org.flowable.cmmn.model.CaseTask;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.HumanTask;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.ProcessTask;
import org.flowable.cmmn.model.Stage;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.task.service.TaskService;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntityImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 * @author Valentin Zickner
 */
public abstract class AbstractCmmnDynamicStateManager {

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

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

    protected void doMovePlanItemState(CaseInstanceChangeState caseInstanceChangeState, CommandContext commandContext) {
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        Map<String, List<PlanItemInstanceEntity>> currentPlanItemInstances = resolvePlanItemInstances(caseInstanceChangeState.getCaseInstanceId(), 
                caseInstanceChangeState.getCaseDefinitionToMigrateTo(), commandContext);
        caseInstanceChangeState.setCurrentPlanItemInstances(currentPlanItemInstances);
        
        // Set the case variables first so they are available during the change state logic
        CaseInstanceEntityManager caseInstanceEntityManager = CommandContextUtil.getCaseInstanceEntityManager(commandContext);
        CaseInstanceEntity caseInstance = caseInstanceEntityManager.findById(caseInstanceChangeState.getCaseInstanceId());
        caseInstance.setVariables(caseInstanceChangeState.getCaseVariables());
        
        executeTerminatePlanItemInstances(caseInstanceChangeState, caseInstance, cmmnEngineConfiguration, commandContext);
        executeActivatePlanItemInstances(caseInstanceChangeState, caseInstance, cmmnEngineConfiguration, commandContext);
        executeChangePlanItemInstancesToAvailableState(caseInstanceChangeState, caseInstance, commandContext);
        
        CmmnEngineAgenda agenda = CommandContextUtil.getAgenda(commandContext);
        agenda.planEvaluateCriteriaOperation(caseInstance.getId());
    }
    
    protected void executeActivatePlanItemInstances(CaseInstanceChangeState caseInstanceChangeState, CaseInstanceEntity caseInstance, 
                    CmmnEngineConfiguration cmmnEngineConfiguration, CommandContext commandContext) {
        
        if (caseInstanceChangeState.getActivatePlanItemDefinitionIds() == null || caseInstanceChangeState.getActivatePlanItemDefinitionIds().isEmpty()) {
            return;
        }
        
        for (String planItemDefinitionId : caseInstanceChangeState.getActivatePlanItemDefinitionIds()) {
            
            PlanItem planItem = resolvePlanItemFromCmmnModelWithDefinitionId(planItemDefinitionId, caseInstance.getCaseDefinitionId());

            PlanItemInstanceEntity newPlanItemInstance = createStagesAndPlanItemInstances(planItem, 
                            caseInstance, caseInstanceChangeState, commandContext);

            CmmnEngineAgenda agenda = CommandContextUtil.getAgenda(commandContext);
            if (caseInstanceChangeState.getChildInstanceTaskVariables().containsKey(planItemDefinitionId) && 
                            (planItem.getPlanItemDefinition() instanceof ProcessTask || planItem.getPlanItemDefinition() instanceof CaseTask)) {
                
                agenda.planStartPlanItemInstanceOperation(newPlanItemInstance, null, caseInstanceChangeState.getChildInstanceTaskVariables().get(planItemDefinitionId));
            } else {
                agenda.planStartPlanItemInstanceOperation(newPlanItemInstance, null);
            }
            
            if (!newPlanItemInstance.getPlanItem().getEntryCriteria().isEmpty() && hasRepetitionRule(newPlanItemInstance)) {
                if (evaluateRepetitionRule(newPlanItemInstance, commandContext)) {
                    createPlanItemInstanceDuplicateForRepetition(newPlanItemInstance, commandContext);
                }
            }
        }
    }
    
    protected void executeChangePlanItemInstancesToAvailableState(CaseInstanceChangeState caseInstanceChangeState, 
                    CaseInstanceEntity caseInstance, CommandContext commandContext) {
        
        if (caseInstanceChangeState.getChangePlanItemToAvailableIds() == null || caseInstanceChangeState.getChangePlanItemToAvailableIds().isEmpty()) {
            return;
        }
        
        PlanItemInstanceEntityManager planItemInstanceEntityManager = CommandContextUtil.getPlanItemInstanceEntityManager(commandContext);

        for (String planItemDefinitionId : caseInstanceChangeState.getChangePlanItemToAvailableIds()) {
            
            List<PlanItemInstance> planItemInstances = planItemInstanceEntityManager.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId(planItemDefinitionId).list();
            List<PlanItemInstance> activePlanItemInstances = new ArrayList<>();
            if (planItemInstances != null && !planItemInstances.isEmpty()) {
                for (PlanItemInstance planItemInstance : planItemInstances) {
                    if (!PlanItemInstanceState.TERMINAL_STATES.contains(planItemInstance.getState())) {
                        activePlanItemInstances.add(planItemInstance);
                    }
                }
            }
            
            if (activePlanItemInstances.isEmpty()) {
                PlanItem planItem = resolvePlanItemFromCmmnModelWithDefinitionId(planItemDefinitionId, caseInstance.getCaseDefinitionId());
                
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
                
                PlanItemInstanceEntity availablePlanItemInstance = planItemInstanceEntityManager.createPlanItemInstanceEntityBuilder()
                        .planItem(planItem)
                        .caseDefinitionId(caseInstance.getCaseDefinitionId())
                        .caseInstanceId(caseInstance.getId())
                        .stagePlanItemInstance(parentPlanItemInstance)
                        .tenantId(caseInstance.getTenantId())
                        .addToParent(true)
                        .create();
                
                CmmnEngineAgenda agenda = CommandContextUtil.getAgenda(commandContext);
                agenda.planChangePlanItemInstanceToAvailableOperation(availablePlanItemInstance);
                
                continue;
            }
            
            PlanItemInstance existingPlanItemInstance = null;
            for (PlanItemInstance planItemInstance : planItemInstances) {
                if (PlanItemInstanceState.ACTIVE.equals(planItemInstance.getState()) || PlanItemInstanceState.ENABLED.equals(planItemInstance.getState())) {
                    if (existingPlanItemInstance != null) {
                        throw new FlowableException("multiple active or enabled plan item instances found for plan item definition " + planItemDefinitionId);
                    } else {
                        existingPlanItemInstance = planItemInstance;
                    }
                }
            }
            
            if (existingPlanItemInstance == null) {
                throw new FlowableException("No active or enabled plan item instances found for plan item definition " + planItemDefinitionId);
            }
            
            PlanItemInstanceEntity existingPlanItemInstanceEntity = (PlanItemInstanceEntity) existingPlanItemInstance;
            
            if (existingPlanItemInstanceEntity.getPlanItem().getPlanItemDefinition() instanceof HumanTask) {
                TaskService taskService = CommandContextUtil.getTaskService(commandContext);
                List<TaskEntity> taskEntities = taskService.findTasksBySubScopeIdScopeType(existingPlanItemInstanceEntity.getId(), ScopeTypes.CMMN);
                if (taskEntities == null || taskEntities.isEmpty()) {
                    throw new FlowableException("No task entity found for plan item instance " + existingPlanItemInstanceEntity.getId());
                }

                // Should be only one
                for (TaskEntity taskEntity : taskEntities) {
                    if (!taskEntity.isDeleted()) {
                        TaskHelper.deleteTask(taskEntity, "Change plan item state", false, false);
                    }
                }
            }

            CmmnEngineAgenda agenda = CommandContextUtil.getAgenda(commandContext);
            agenda.planChangePlanItemInstanceToAvailableOperation(existingPlanItemInstanceEntity);
        }
    }

    protected void executeTerminatePlanItemInstances(CaseInstanceChangeState caseInstanceChangeState, CaseInstanceEntity caseInstance, 
            CmmnEngineConfiguration cmmnEngineConfiguration, CommandContext commandContext) {

        if (caseInstanceChangeState.getTerminatePlanItemDefinitionIds() == null || caseInstanceChangeState.getTerminatePlanItemDefinitionIds().isEmpty()) {
            return;
        }
        
        Map<String, List<PlanItemInstanceEntity>> currentPlanItemInstanceMap = caseInstanceChangeState.getCurrentPlanItemInstances();
        for (String planItemDefinitionId : caseInstanceChangeState.getTerminatePlanItemDefinitionIds()) {
            if (currentPlanItemInstanceMap.containsKey(planItemDefinitionId)) {
                List<PlanItemInstanceEntity> currentPlanItemInstanceList = currentPlanItemInstanceMap.get(planItemDefinitionId);
                for (PlanItemInstanceEntity planItemInstance : currentPlanItemInstanceList) {
                    if (!PlanItemInstanceState.TERMINAL_STATES.contains(planItemInstance.getState()) && 
                            !PlanItemInstanceState.WAITING_FOR_REPETITION.equals(planItemInstance.getState())) {
                        
                        terminatePlanItemInstance(planItemInstance, commandContext, cmmnEngineConfiguration);
                    }
                }
            }
        }
    }
    
    protected abstract boolean isDirectPlanItemDefinitionMigration(PlanItemDefinition currentPlanItemDefinition, PlanItemDefinition newPlanItemDefinition);

    protected Map<String, List<PlanItemInstanceEntity>> resolvePlanItemInstances(String caseInstanceId, CaseDefinition caseDefinition, CommandContext commandContext) {
        PlanItemInstanceEntityManager planItemInstanceEntityManager = CommandContextUtil.getPlanItemInstanceEntityManager(commandContext);
        List<PlanItemInstanceEntity> planItemInstances = planItemInstanceEntityManager.findByCaseInstanceId(caseInstanceId);
        
        if (caseDefinition != null) {
            TaskService taskService = CommandContextUtil.getTaskService(commandContext);
            for (PlanItemInstanceEntity planItemInstance : planItemInstances) {
                
                planItemInstance.setCaseDefinitionId(caseDefinition.getId());
                
                if (!PlanItemInstanceState.AVAILABLE.equals(planItemInstance.getState()) && 
                        planItemInstance.getPlanItemDefinition() instanceof HumanTask) {
                    
                    TaskEntityImpl task = (TaskEntityImpl) taskService.createTaskQuery().subScopeId(planItemInstance.getId()).scopeType(ScopeTypes.CMMN).singleResult();
                    if (task != null) {
                        task.setScopeDefinitionId(caseDefinition.getId());
                    }
                }
            }
        }

        Map<String, List<PlanItemInstanceEntity>> stagesByPlanItemDefinitionId = planItemInstances.stream()
            .collect(Collectors.groupingBy(PlanItemInstance::getPlanItemDefinitionId));
        return stagesByPlanItemDefinitionId;
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
            CaseInstanceChangeState caseInstanceChangeState, CommandContext commandContext) {
        
        PlanItemInstanceEntityManager planItemInstanceEntityManager = CommandContextUtil.getPlanItemInstanceEntityManager(commandContext);
        
        Map<String, Stage> stagesToCreate = new HashMap<>();
        PlanItemDefinition planItemDefinition = planItem.getPlanItemDefinition();
        Stage stage = planItemDefinition.getParentStage();
            
        Map<String, List<PlanItemInstanceEntity>> runtimePlanItemInstanceMap = caseInstanceChangeState.getRuntimePlanItemInstances();
        while (stage != null) {
            if (!stage.isPlanModel() && !caseInstanceChangeState.getCreatedStageInstances().containsKey(stage.getId()) && 
                    !isStageAncestorOfAnyPlanItemInstance(stage.getId(), runtimePlanItemInstanceMap)) {
                
                stagesToCreate.put(stage.getId(), stage);
            }
            stage = stage.getParentStage();
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

            createChildPlanItemInstancesForStage(Collections.singletonList(newPlanItemInstance), Collections.singleton(planItem.getId()), commandContext);
        }

        return newPlanItemInstance;
    }

    protected void createChildPlanItemInstancesForStage(List<PlanItemInstanceEntity> newPlanItemInstances, Set<String> newPlanItemInstanceIds, CommandContext commandContext) {
        if (newPlanItemInstances.size() == 0) {
            return;
        }
        
        PlanItemInstanceEntity newPlanItemInstance = newPlanItemInstances.get(0);
        PlanItem planItem = newPlanItemInstance.getPlanItem();
        if (planItem.getParentStage() != null) {
            for (PlanItem stagePlanItem : planItem.getParentStage().getPlanItems()) {
                if (!newPlanItemInstanceIds.contains(stagePlanItem.getId())) {
                    PlanItemInstance parentStagePlanItem = newPlanItemInstance.getStagePlanItemInstanceEntity();
                    if (parentStagePlanItem == null && newPlanItemInstance.getStageInstanceId() != null) {
                        parentStagePlanItem = CommandContextUtil.getPlanItemInstanceEntityManager(commandContext).findById(newPlanItemInstance.getStageInstanceId());
                    }
                    
                    if (stagePlanItem.getPlanItemDefinition() instanceof Stage) {

                        PlanItemInstanceEntity childStagePlanItemInstance = CommandContextUtil.getPlanItemInstanceEntityManager(commandContext)
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

    protected boolean isStageAncestorOfAnyPlanItemInstance(String stageId, Map<String, List<PlanItemInstanceEntity>> planItemInstanceMap) {
        for (List<PlanItemInstanceEntity> planItemInstanceList : planItemInstanceMap.values()) {
            for (PlanItemInstanceEntity planItemInstance : planItemInstanceList) {
                PlanItemDefinition planItemDefinition = planItemInstance.getPlanItem().getPlanItemDefinition();
                
                if (planItemDefinition.getId().equals(stageId)) {
                    return true;
                }
    
                if (isStageAncestor(stageId, planItemDefinition)) {
                    return true;
                }
            }
        }
        return false;
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
        
        PlanItemInstanceEntityManager planItemInstanceEntityManager = CommandContextUtil.getPlanItemInstanceEntityManager(commandContext);
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
        
        CommandContextUtil.getAgenda().planStartPlanItemInstanceOperation(newPlanItemInstance, null);

        return newPlanItemInstance;
    }
    
    protected void terminatePlanItemInstance(PlanItemInstanceEntity planItemInstance, CommandContext commandContext, CmmnEngineConfiguration cmmnEngineConfiguration) {
        String currentPlanItemInstanceState = planItemInstance.getState();
        
        Date currentTime = cmmnEngineConfiguration.getClock().getCurrentTime();
        planItemInstance.setEndedTime(currentTime);
        planItemInstance.setTerminatedTime(currentTime);
        planItemInstance.setState(PlanItemInstanceState.TERMINATED);
        
        PlanItemDefinition planItemDefinition = planItemInstance.getPlanItem().getPlanItemDefinition();
        if (planItemDefinition instanceof HumanTask) {
            if (PlanItemInstanceState.ACTIVE.equals(currentPlanItemInstanceState)) {
                TaskService taskService = CommandContextUtil.getTaskService(commandContext);
                List<TaskEntity> taskEntities = taskService.findTasksBySubScopeIdScopeType(planItemInstance.getId(), ScopeTypes.CMMN);
                if (taskEntities == null || taskEntities.isEmpty()) {
                    throw new FlowableException("No task entity found for plan item instance " + planItemInstance.getId());
                }
    
                // Should be only one
                for (TaskEntity taskEntity : taskEntities) {
                    if (!taskEntity.isDeleted()) {
                        TaskHelper.deleteTask(taskEntity, "Change plan item state", false, false);
                    }
                }
            }
            
        } else if (planItemDefinition instanceof Stage) {
            deleteChildPlanItemInstances(planItemInstance, commandContext, cmmnEngineConfiguration);
        
        } else if (planItemDefinition instanceof ProcessTask) {
            if (planItemInstance.getReferenceId() != null) {
                cmmnEngineConfiguration.getProcessInstanceService().deleteProcessInstance(planItemInstance.getReferenceId());
            }
        }
    }
    
    protected void deleteChildPlanItemInstances(PlanItemInstanceEntity planItemInstance, CommandContext commandContext, CmmnEngineConfiguration cmmnEngineConfiguration) {
        List<PlanItemInstanceEntity> childPlanItemInstances = planItemInstance.getChildPlanItemInstances();
        if (childPlanItemInstances != null) {
            for (PlanItemInstanceEntity childPlanItemInstance : childPlanItemInstances) {
                deleteChildPlanItemInstances(childPlanItemInstance, commandContext, cmmnEngineConfiguration);
                terminatePlanItemInstance(childPlanItemInstance, commandContext, cmmnEngineConfiguration);
            }
        }
    }

    protected void handleHumanTaskNewAssignee(PlanItemInstanceEntity taskPlanItemInstance, String newAssigneeId, CommandContext commandContext) {
        TaskService taskService = CommandContextUtil.getTaskService(commandContext);
        TaskEntityImpl task = (TaskEntityImpl) taskService.createTaskQuery().subScopeId(taskPlanItemInstance.getId()).scopeType(ScopeTypes.CMMN).singleResult();
        TaskHelper.changeTaskAssignee(task, newAssigneeId);
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
        CommandContextUtil.getCmmnEngineConfiguration(commandContext).getListenerNotificationHelper()
            .executeLifecycleListeners(commandContext, planItemInstanceEntity, oldState, newState);

        // createPlanItemInstance operations will also sync planItemInstance history
        CommandContextUtil.getAgenda(commandContext).planCreatePlanItemInstanceForRepetitionOperation(childPlanItemInstanceEntity);
        return childPlanItemInstanceEntity;
    }
    
    protected PlanItemInstanceEntity copyAndInsertPlanItemInstance(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntityToCopy, boolean addToParent) {
        PlanItemInstance stagePlanItem = planItemInstanceEntityToCopy.getStagePlanItemInstanceEntity();
        if (stagePlanItem == null && planItemInstanceEntityToCopy.getStageInstanceId() != null) {
            stagePlanItem = CommandContextUtil.getPlanItemInstanceEntityManager(commandContext).findById(planItemInstanceEntityToCopy.getStageInstanceId());
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
            int counter = getRepetitionCounter(planItemInstanceEntityToCopy);
            setRepetitionCounter(planItemInstanceEntity, counter);
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

}
