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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
import org.flowable.cmmn.model.CaseTask;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.HumanTask;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.ProcessTask;
import org.flowable.cmmn.model.Stage;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.task.service.TaskService;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntityImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 */
public abstract class AbstractCmmnDynamicStateManager {

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    //-- Move container preparation section start
    public List<MovePlanItemInstanceEntityContainer> resolveMovePlanItemInstanceEntityContainers(ChangePlanItemStateBuilderImpl changePlanItemStateBuilder, String migrateToCaseDefinitionId, Map<String, Object> variables, CommandContext commandContext) {
        List<MovePlanItemInstanceEntityContainer> movePlanItemInstanceEntityContainerList = new ArrayList<>();
        if (changePlanItemStateBuilder.getMovePlanItemInstanceIdList().size() > 0) {
            for (MovePlanItemInstanceIdContainer planItemInstanceContainer : changePlanItemStateBuilder.getMovePlanItemInstanceIdList()) {
                
                Map<String, List<PlanItemInstanceEntity>> planItemInstancesByParent = new HashMap<>();
                for (String planItemInstanceId : planItemInstanceContainer.getPlanItemInstanceIds()) {
                    PlanItemInstanceEntity planItemInstance = resolvePlanItemInstance(planItemInstanceId, commandContext);
                    List<PlanItemInstanceEntity> currentPlanItemInstanceEntities = planItemInstancesByParent.computeIfAbsent(planItemInstance.getStageInstanceId(), k -> new ArrayList<>());
                    currentPlanItemInstanceEntities.add(planItemInstance);
                }
                
                planItemInstancesByParent.values().forEach(planItemInstances -> {
                    MovePlanItemInstanceEntityContainer movePlanItemInstanceEntityContainer = new MovePlanItemInstanceEntityContainer(planItemInstances, planItemInstanceContainer.getMoveToPlanItemDefinitionIds());
                    PlanItemInstanceEntity firstPlanItemInstance = planItemInstances.get(0);
                    movePlanItemInstanceEntityContainer.setCaseDefinitionId(firstPlanItemInstance.getCaseDefinitionId());
                    movePlanItemInstanceEntityContainer.setCaseInstanceId(firstPlanItemInstance.getCaseInstanceId());
                    movePlanItemInstanceEntityContainer.setTenantId(firstPlanItemInstance.getTenantId());
                    if (planItemInstanceContainer.getNewAssigneeId() != null) {
                        movePlanItemInstanceEntityContainer.setNewAssigneeId(planItemInstanceContainer.getNewAssigneeId());
                    }
                    movePlanItemInstanceEntityContainerList.add(movePlanItemInstanceEntityContainer);
                });
            }
        }

        if (changePlanItemStateBuilder.getMovePlanItemDefinitionIdList().size() > 0) {
            for (MovePlanItemDefinitionIdContainer planItemDefinitionContainer : changePlanItemStateBuilder.getMovePlanItemDefinitionIdList()) {
                for (String planItemDefinitionId : planItemDefinitionContainer.getPlanItemDefinitionIds()) {
                    List<PlanItemInstanceEntity> currentPlanItemInstances = resolvePlanItemInstances(changePlanItemStateBuilder.getCaseInstanceId(), planItemDefinitionId, commandContext);
                    if (!currentPlanItemInstances.isEmpty()) {
                        movePlanItemInstanceEntityContainerList.add(createMovePlanItemInstanceEntityContainer(planItemDefinitionContainer, currentPlanItemInstances, commandContext));
                    }
                }
            }
        }

        return movePlanItemInstanceEntityContainerList;
    }

    protected PlanItemInstanceEntity resolvePlanItemInstance(String planItemInstanceId, CommandContext commandContext) {
        PlanItemInstanceEntityManager planItemInstanceEntityManager = CommandContextUtil.getPlanItemInstanceEntityManager(commandContext);
        PlanItemInstanceEntity planItemInstance = planItemInstanceEntityManager.findById(planItemInstanceId);

        if (planItemInstance == null) {
            throw new FlowableException("PlanItemInstance could not be found with id " + planItemInstanceId);
        }

        return planItemInstance;
    }

    protected List<PlanItemInstanceEntity> resolvePlanItemInstances(String caseInstanceId, String planItemDefinitionId, CommandContext commandContext) {
        CaseInstanceEntityManager caseInstanceEntityManager = CommandContextUtil.getCaseInstanceEntityManager(commandContext);
        CaseInstanceEntity caseInstance = caseInstanceEntityManager.findById(caseInstanceId);

        if (caseInstance == null) {
            throw new FlowableException("Case instance could not be found with id " + caseInstanceId);
        }

        PlanItemInstanceEntityManager planItemInstanceEntityManager = CommandContextUtil.getPlanItemInstanceEntityManager(commandContext);
        List<PlanItemInstanceEntity> planItemInstances = planItemInstanceEntityManager.findByCaseInstanceId(caseInstanceId);

        List<PlanItemInstanceEntity> resultPlanItemInstances = planItemInstances.stream()
            .filter(e -> e.getPlanItemDefinitionId() != null)
            .filter(e -> e.getPlanItemDefinitionId().equals(planItemDefinitionId))
            .filter(e -> e.getEndedTime() == null)
            .collect(Collectors.toList());

        if (resultPlanItemInstances.isEmpty()) {
            throw new FlowableException("Plan item instance could not be found with plan item definition id " + planItemDefinitionId);
        }

        return resultPlanItemInstances;
    }

    protected MovePlanItemInstanceEntityContainer createMovePlanItemInstanceEntityContainer(MovePlanItemDefinitionIdContainer planItemDefinitionContainer, 
                    List<PlanItemInstanceEntity> planItemInstances, CommandContext commandContext) {
        
        MovePlanItemInstanceEntityContainer movePlanItemInstanceEntityContainer = new MovePlanItemInstanceEntityContainer(planItemInstances, 
                        planItemDefinitionContainer.getMoveToPlanItemDefinitionIds());
        PlanItemInstanceEntity firstPlanItemInstance = planItemInstances.get(0);
        movePlanItemInstanceEntityContainer.setCaseDefinitionId(firstPlanItemInstance.getCaseDefinitionId());
        movePlanItemInstanceEntityContainer.setCaseInstanceId(firstPlanItemInstance.getCaseInstanceId());
        movePlanItemInstanceEntityContainer.setTenantId(firstPlanItemInstance.getTenantId());
        if (planItemDefinitionContainer.getNewAssigneeId() != null) {
            movePlanItemInstanceEntityContainer.setNewAssigneeId(planItemDefinitionContainer.getNewAssigneeId());
        }
        
        return movePlanItemInstanceEntityContainer;
    }

    protected void prepareMovePlanItemInstanceEntityContainer(MovePlanItemInstanceEntityContainer movePlanItemInstanceContainer, CommandContext commandContext) {
        for (String planItemDefinitionId : movePlanItemInstanceContainer.getMoveToPlanItemDefinitionIds()) {
            // Get first plan item instance to get case definition id
            PlanItemInstanceEntity firstPlanItemInstance = movePlanItemInstanceContainer.getPlanItemInstances().get(0);
            CmmnModel cmmnModel = CaseDefinitionUtil.getCmmnModel(firstPlanItemInstance.getCaseDefinitionId());
            String currentPlanItemId = firstPlanItemInstance.getElementId();
            PlanItem currentPlanItem = resolvePlanItemFromCmmnModel(cmmnModel, currentPlanItemId, firstPlanItemInstance.getCaseDefinitionId());
            PlanItem newPlanItem = resolvePlanItemFromCmmnModelWithDefinitionId(cmmnModel, planItemDefinitionId, firstPlanItemInstance.getCaseDefinitionId());
            
            movePlanItemInstanceContainer.addMoveToPlanItem(currentPlanItem.getDefinitionRef(), currentPlanItem, newPlanItem);
        }
    }
    
    protected PlanItem resolvePlanItemFromCmmnModelWithDefinitionId(CmmnModel cmmnModel, String planItemDefinitionId, String caseDefinitionId) {
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
        Map<String, List<PlanItemInstance>> currentStages = resolveActiveStagePlanItemInstances(caseInstanceChangeState.getCaseInstanceId(), commandContext);
        caseInstanceChangeState.setCurrentStageInstances(currentStages);
        
        // Set the case variables first so they are available during the change state logic
        CaseInstanceEntityManager caseInstanceEntityManager = CommandContextUtil.getCaseInstanceEntityManager(commandContext);
        CaseInstanceEntity caseInstance = caseInstanceEntityManager.findById(caseInstanceChangeState.getCaseInstanceId());
        caseInstance.setVariables(caseInstanceChangeState.getCaseVariables());

        executeMovePlanItemInstances(caseInstanceChangeState, caseInstance, cmmnEngineConfiguration, commandContext);
        
        Map<String, PlanItemInstanceEntity> movingPlanItemInstanceMap = new HashMap<>();
        for (MovePlanItemInstanceEntityContainer movePlanItemInstanceContainer : caseInstanceChangeState.getMovePlanItemInstanceEntityContainers()) {
            List<PlanItemInstanceEntity> planItemInstancesToMove = movePlanItemInstanceContainer.getPlanItemInstances();
            for (PlanItemInstanceEntity planItemInstanceEntity : planItemInstancesToMove) {
                movingPlanItemInstanceMap.put(planItemInstanceEntity.getId(), planItemInstanceEntity);
            }
        }
        
        executeActivatePlanItemInstances(caseInstanceChangeState, caseInstance, movingPlanItemInstanceMap.values(), cmmnEngineConfiguration, commandContext);
        executeChangePlanItemInstancesToAvailableState(caseInstanceChangeState, caseInstance, commandContext);
    }
    
    protected void executeMovePlanItemInstances(CaseInstanceChangeState caseInstanceChangeState, CaseInstanceEntity caseInstance, 
                    CmmnEngineConfiguration cmmnEngineConfiguration, CommandContext commandContext) {
        
        for (MovePlanItemInstanceEntityContainer movePlanItemInstanceContainer : caseInstanceChangeState.getMovePlanItemInstanceEntityContainers()) {
            prepareMovePlanItemInstanceEntityContainer(movePlanItemInstanceContainer, commandContext);

            List<PlanItemInstanceEntity> planItemInstancesToMove = movePlanItemInstanceContainer.getPlanItemInstances();

            List<PlanItemMoveEntry> moveToPlanItemMoveEntries = movePlanItemInstanceContainer.getMoveToPlanItems();

            Set<String> planItemInstanceIdsNotToDelete = new HashSet<>();
            for (PlanItemInstanceEntity planItemInstance : planItemInstancesToMove) {
                planItemInstanceIdsNotToDelete.add(planItemInstance.getId());

                Date currentTime = cmmnEngineConfiguration.getClock().getCurrentTime();
                planItemInstance.setEndedTime(currentTime);
                planItemInstance.setTerminatedTime(currentTime);
                planItemInstance.setState(PlanItemInstanceState.TERMINATED);
                
                if (planItemInstance.getPlanItem().getPlanItemDefinition() instanceof HumanTask) {
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
                
                // Delete the parent plan item instances for each current plan item instance when the move to plan item definition id has the same stage scope
                PlanItemInstanceEntity continueParentPlanItemInstance = deleteParentPlanItemInstances(planItemInstance.getStageInstanceId(), moveToPlanItemMoveEntries, planItemInstanceIdsNotToDelete, commandContext);
                movePlanItemInstanceContainer.addContinueParentPlanItemInstance(planItemInstance.getId(), continueParentPlanItemInstance);
            }

            List<PlanItemInstanceEntity> newPlanItemInstances = createStagesAndPlanItemInstances(moveToPlanItemMoveEntries, planItemInstancesToMove, 
                            movePlanItemInstanceContainer, caseInstance, caseInstanceChangeState, commandContext);

            CmmnEngineAgenda agenda = CommandContextUtil.getAgenda(commandContext);
            for (PlanItemInstanceEntity newPlanItemInstance : newPlanItemInstances) {
                PlanItemDefinition planItemDefinition = newPlanItemInstance.getPlanItem().getPlanItemDefinition();
                if (caseInstanceChangeState.getChildInstanceTaskVariables().containsKey(newPlanItemInstance.getPlanItemDefinitionId()) && 
                                planItemDefinition instanceof ProcessTask || planItemDefinition instanceof CaseTask) {
                    
                    agenda.planStartPlanItemInstanceOperation(newPlanItemInstance, null, 
                                    caseInstanceChangeState.getChildInstanceTaskVariables().get(newPlanItemInstance.getPlanItemDefinitionId()));
                    
                } else {
                    agenda.planStartPlanItemInstanceOperation(newPlanItemInstance, null);
                }
            }
            
            agenda.planEvaluateCriteriaOperation(caseInstance.getId());
        }
    }
    
    protected void executeActivatePlanItemInstances(CaseInstanceChangeState caseInstanceChangeState, CaseInstanceEntity caseInstance, 
                    Collection<PlanItemInstanceEntity> movingPlanItemInstances, CmmnEngineConfiguration cmmnEngineConfiguration, CommandContext commandContext) {
        
        if (caseInstanceChangeState.getActivatePlanItemDefinitionIds() == null || caseInstanceChangeState.getActivatePlanItemDefinitionIds().isEmpty()) {
            return;
        }
        
        CmmnModel cmmnModel = CaseDefinitionUtil.getCmmnModel(caseInstance.getCaseDefinitionId());
        
        for (String planItemDefinitionId : caseInstanceChangeState.getActivatePlanItemDefinitionIds()) {
            
            PlanItem planItem = resolvePlanItemFromCmmnModelWithDefinitionId(cmmnModel, planItemDefinitionId, caseInstance.getCaseDefinitionId());

            PlanItemInstanceEntity newPlanItemInstance = createStagesAndPlanItemInstances(planItem, 
                            caseInstance, movingPlanItemInstances, caseInstanceChangeState, commandContext);

            CmmnEngineAgenda agenda = CommandContextUtil.getAgenda(commandContext);
            if (caseInstanceChangeState.getChildInstanceTaskVariables().containsKey(planItemDefinitionId) && 
                            planItem.getPlanItemDefinition() instanceof ProcessTask || planItem.getPlanItemDefinition() instanceof CaseTask) {
                
                agenda.planStartPlanItemInstanceOperation(newPlanItemInstance, null, caseInstanceChangeState.getChildInstanceTaskVariables().get(planItemDefinitionId));
            } else {
                agenda.planStartPlanItemInstanceOperation(newPlanItemInstance, null);
            }
            
            agenda.planEvaluateCriteriaOperation(caseInstance.getId());
        }
    }
    
    protected void executeChangePlanItemInstancesToAvailableState(CaseInstanceChangeState caseInstanceChangeState, 
                    CaseInstanceEntity caseInstance, CommandContext commandContext) {
        
        if (caseInstanceChangeState.getChangePlanItemToAvailableIdList() == null || caseInstanceChangeState.getChangePlanItemToAvailableIdList().isEmpty()) {
            return;
        }
        
        PlanItemInstanceEntityManager planItemInstanceEntityManager = CommandContextUtil.getPlanItemInstanceEntityManager(commandContext);
        
        for (String planItemDefinitionId : caseInstanceChangeState.getChangePlanItemToAvailableIdList()) {
            
            List<PlanItemInstance> planItemInstances = planItemInstanceEntityManager.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId(planItemDefinitionId).list();
            if (planItemInstances == null || planItemInstances.isEmpty()) {
                throw new FlowableException("No plan item instances found for plan item definition " + planItemDefinitionId);
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

    protected abstract Map<String, List<PlanItemInstance>> resolveActiveStagePlanItemInstances(String caseInstanceId, CommandContext commandContext);

    protected abstract boolean isDirectPlanItemDefinitionMigration(PlanItemDefinition currentPlanItemDefinition, PlanItemDefinition newPlanItemDefinition);

    protected PlanItemInstanceEntity deleteParentPlanItemInstances(String parentId, List<PlanItemMoveEntry> moveToPlanItemMoveEntries, Set<String> planItemInstanceIdsNotToDelete, CommandContext commandContext) {
        PlanItemInstanceEntityManager planItemInstanceEntityManager = CommandContextUtil.getPlanItemInstanceEntityManager(commandContext);

        PlanItemInstanceEntity parentPlanItemInstance = null;
        
        if (parentId != null) {
            parentPlanItemInstance = planItemInstanceEntityManager.findById(parentId);
        }
        
        if (parentPlanItemInstance != null && parentPlanItemInstance.isStage()) {
            Stage parentStage = (Stage) parentPlanItemInstance.getPlanItem().getPlanItemDefinition();
            if (!isStageAncestorOfAnyNewPlanItemDefinitions(parentStage.getId(), moveToPlanItemMoveEntries)) {
                PlanItemInstanceEntity toDeleteParentExecution = resolveParentPlanItemInstanceToDelete(parentPlanItemInstance, moveToPlanItemMoveEntries);
                PlanItemInstanceEntity finalDeleteExecution = null;
                if (toDeleteParentExecution != null) {
                    finalDeleteExecution = toDeleteParentExecution;
                } else {
                    finalDeleteExecution = parentPlanItemInstance;
                }

                parentPlanItemInstance = finalDeleteExecution.getStagePlanItemInstanceEntity();

                CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
                Date currentTime = cmmnEngineConfiguration.getClock().getCurrentTime();
                finalDeleteExecution.setEndedTime(currentTime);
                finalDeleteExecution.setTerminatedTime(currentTime);
                finalDeleteExecution.setState(PlanItemInstanceState.TERMINATED);
            }
        }

        return parentPlanItemInstance;
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

    protected List<PlanItemInstanceEntity> createStagesAndPlanItemInstances(List<PlanItemMoveEntry> moveToPlanItems, List<PlanItemInstanceEntity> movingPlanItemInstances, 
                    MovePlanItemInstanceEntityContainer movePlanItemInstanceEntityContainer, CaseInstanceEntity caseInstance,
                    CaseInstanceChangeState caseInstanceChangeState, CommandContext commandContext) {

        PlanItemInstanceEntityManager planItemInstanceEntityManager = CommandContextUtil.getPlanItemInstanceEntityManager(commandContext);

        // Resolve the stage elements that need to be created for each move to plan item definition
        Map<String, Stage> stagesToCreate = new HashMap<>();
        for (PlanItemMoveEntry planItemMoveEntry : moveToPlanItems) {
            PlanItemDefinition newPlanItemDefinition = planItemMoveEntry.getNewPlanItem().getPlanItemDefinition();
            Stage stage = newPlanItemDefinition.getParentStage();
            
            while (stage != null) {
                if (!stage.isPlanModel() && !caseInstanceChangeState.getCreatedStageInstances().containsKey(stage.getId()) && 
                                !isStageAncestorOfAnyPlanItemInstance(stage.getId(), movingPlanItemInstances)) {
                    
                    stagesToCreate.put(stage.getId(), stage);
                }
                stage = stage.getParentStage();
            }
        }

        // The default parent execution is retrieved from the match with the first source execution
        PlanItemInstanceEntity defaultContinueParentPlanItemInstance = movePlanItemInstanceEntityContainer.getContinueParentPlanItemInstance(movingPlanItemInstances.get(0).getId());
        Set<String> movingPlanItemInstanceIds = movingPlanItemInstances.stream().map(PlanItemInstanceEntity::getId).collect(Collectors.toSet());

        // Build the stage hierarchy
        for (Stage stage : stagesToCreate.values()) {
            if (!caseInstanceChangeState.getCreatedStageInstances().containsKey(stage.getId())) {
                PlanItemInstanceEntity stageInstance = createStageHierarchy(stage, defaultContinueParentPlanItemInstance, stagesToCreate, 
                                movingPlanItemInstanceIds, caseInstanceChangeState, caseInstance, commandContext);
                caseInstanceChangeState.addCreatedStageInstance(stage.getId(), stageInstance);
            }
        }

        // Adds the plan item instance (leaf) to the stage instance
        List<PlanItemInstanceEntity> newChildPlanItemInstances = new ArrayList<>();
        for (PlanItemMoveEntry planItemMoveEntry : moveToPlanItems) {
            PlanItem newPlanItem = planItemMoveEntry.getNewPlanItem();
            PlanItemDefinition newPlanItemDefinition = newPlanItem.getPlanItemDefinition();
            PlanItemInstanceEntity parentPlanItemInstance = null;
            if (newPlanItemDefinition.getParentStage() != null && caseInstanceChangeState.getCreatedStageInstances().containsKey(newPlanItemDefinition.getParentStage().getId())) {
                parentPlanItemInstance = caseInstanceChangeState.getCreatedStageInstances().get(newPlanItemDefinition.getParentStage().getId());
            } else {
                parentPlanItemInstance = defaultContinueParentPlanItemInstance;
            }
            
            List<PlanItemInstanceEntity> existingPlanItemInstances = planItemInstanceEntityManager.findByCaseInstanceIdAndPlanItemId(movePlanItemInstanceEntityContainer.getCaseInstanceId(), newPlanItem.getId());
            PlanItemInstanceEntity newPlanItemInstance = null;
            if (!existingPlanItemInstances.isEmpty()) {
                for (PlanItemInstanceEntity existingPlanItemInstance : existingPlanItemInstances) {
                    if (PlanItemInstanceState.AVAILABLE.equals(existingPlanItemInstance.getState())) {
                        newPlanItemInstance = existingPlanItemInstance;
                    }
                }
            }
            
            if (newPlanItemInstance == null) {
                newPlanItemInstance = planItemInstanceEntityManager.createChildPlanItemInstance(newPlanItem, 
                                movePlanItemInstanceEntityContainer.getCaseDefinitionId(), movePlanItemInstanceEntityContainer.getCaseInstanceId(), 
                                parentPlanItemInstance != null ? parentPlanItemInstance.getId() : null, 
                                movePlanItemInstanceEntityContainer.getTenantId(), true);
                
                if (newPlanItem.getParentStage() != null) {
                    for (PlanItem stagePlanItem : newPlanItem.getParentStage().getPlanItems()) {
                        if (!stagePlanItem.getId().equals(newPlanItem.getId())) {
                            PlanItemInstanceEntity childStagePlanItemInstance = CommandContextUtil.getPlanItemInstanceEntityManager(commandContext)
                                .createChildPlanItemInstance(stagePlanItem, newPlanItemInstance.getCaseDefinitionId(), newPlanItemInstance.getCaseInstanceId(), 
                                                newPlanItemInstance.getStageInstanceId(), newPlanItemInstance.getTenantId(), true);
                            
                            CommandContextUtil.getAgenda(commandContext).planCreatePlanItemInstanceOperation(childStagePlanItemInstance);
                        }
                    }
                }
            }

            if (movePlanItemInstanceEntityContainer.getNewAssigneeId() != null && newPlanItemDefinition instanceof HumanTask) {
                handleHumanTaskNewAssignee(newPlanItemInstance, movePlanItemInstanceEntityContainer.getNewAssigneeId(), commandContext);
            }

            newChildPlanItemInstances.add(newPlanItemInstance);
        }

        return newChildPlanItemInstances;
    }
    
    protected PlanItemInstanceEntity createStagesAndPlanItemInstances(PlanItem planItem, CaseInstanceEntity caseInstance, 
                    Collection<PlanItemInstanceEntity> movingPlanItemInstances, CaseInstanceChangeState caseInstanceChangeState, CommandContext commandContext) {
        
        PlanItemInstanceEntityManager planItemInstanceEntityManager = CommandContextUtil.getPlanItemInstanceEntityManager(commandContext);
        
        // Resolve the stage elements that need to be created for each move to plan item definition
        Map<String, Stage> stagesToCreate = new HashMap<>();
        PlanItemDefinition planItemDefinition = planItem.getPlanItemDefinition();
        Stage stage = planItemDefinition.getParentStage();
            
        while (stage != null) {
            if (!stage.isPlanModel() && !caseInstanceChangeState.getCreatedStageInstances().containsKey(stage.getId()) && 
                            !isStageAncestorOfAnyPlanItemInstance(stage.getId(), movingPlanItemInstances)) {
                
                stagesToCreate.put(stage.getId(), stage);
            }
            stage = stage.getParentStage();
        }

        Set<String> movingPlanItemInstanceIds = movingPlanItemInstances.stream().map(PlanItemInstanceEntity::getId).collect(Collectors.toSet());

        // Build the stage hierarchy
        for (Stage stageToCreate : stagesToCreate.values()) {
            if (!caseInstanceChangeState.getCreatedStageInstances().containsKey(stageToCreate.getId())) {
                PlanItemInstanceEntity stageInstance = createStageHierarchy(stageToCreate, null, stagesToCreate, 
                                movingPlanItemInstanceIds, caseInstanceChangeState, caseInstance, commandContext);
                caseInstanceChangeState.addCreatedStageInstance(stageToCreate.getId(), stageInstance);
            }
        }
        
        // Adds the plan item instance (leaf) to the stage instance
        PlanItemInstanceEntity parentPlanItemInstance = null;
        if (planItemDefinition.getParentStage() != null && caseInstanceChangeState.getCreatedStageInstances().containsKey(planItemDefinition.getParentStage().getId())) {
            parentPlanItemInstance = caseInstanceChangeState.getCreatedStageInstances().get(planItemDefinition.getParentStage().getId());
        }
            
        List<PlanItemInstanceEntity> existingPlanItemInstances = planItemInstanceEntityManager.findByCaseInstanceIdAndPlanItemId(caseInstance.getId(), planItem.getId());
        PlanItemInstanceEntity newPlanItemInstance = null;
        if (!existingPlanItemInstances.isEmpty()) {
            for (PlanItemInstanceEntity existingPlanItemInstance : existingPlanItemInstances) {
                if (PlanItemInstanceState.AVAILABLE.equals(existingPlanItemInstance.getState())) {
                    newPlanItemInstance = existingPlanItemInstance;
                }
            }
        }
            
        if (newPlanItemInstance == null) {
            newPlanItemInstance = planItemInstanceEntityManager.createChildPlanItemInstance(planItem, 
                            caseInstance.getCaseDefinitionId(), caseInstance.getId(), 
                            parentPlanItemInstance != null ? parentPlanItemInstance.getId() : null, 
                                            caseInstance.getTenantId(), true);
            
            if (planItem.getParentStage() != null) {
                for (PlanItem stagePlanItem : planItem.getParentStage().getPlanItems()) {
                    if (!stagePlanItem.getId().equals(planItem.getId())) {
                        PlanItemInstanceEntity childStagePlanItemInstance = CommandContextUtil.getPlanItemInstanceEntityManager(commandContext)
                            .createChildPlanItemInstance(stagePlanItem, newPlanItemInstance.getCaseDefinitionId(), newPlanItemInstance.getCaseInstanceId(), 
                                            newPlanItemInstance.getStageInstanceId(), newPlanItemInstance.getTenantId(), true);
                        
                        CommandContextUtil.getAgenda(commandContext).planCreatePlanItemInstanceOperation(childStagePlanItemInstance);
                    }
                }
            }
        }

        return newPlanItemInstance;
    }

    protected boolean isStageAncestorOfAnyPlanItemInstance(String stageId, Collection<PlanItemInstanceEntity> planItemInstances) {
        for (PlanItemInstanceEntity planItemInstance : planItemInstances) {
            PlanItemDefinition planItemDefinition = planItemInstance.getPlanItem().getPlanItemDefinition();

            if (isStageAncestor(stageId, planItemDefinition)) {
                return true;
            }
        }
        return false;
    }

    protected boolean isStageAncestorOfAnyNewPlanItemDefinitions(String stageId, List<PlanItemMoveEntry> planItems) {
        for (PlanItemMoveEntry planItemMoveEntry : planItems) {
            if (isStageAncestor(stageId, planItemMoveEntry.getNewPlanItem().getPlanItemDefinition())) {
                return true;
            }
        }
        return false;
    }

    private boolean isStageAncestor(String stageId, PlanItemDefinition planItemDefinition) {
        while (planItemDefinition.getParentStage() != null) {
            String currentStageId = planItemDefinition.getParentStage().getId();
            if (currentStageId != null && currentStageId.equals(stageId)) {
                return true;
            }
            planItemDefinition = planItemDefinition.getParentStage();
        }
        return false;
    }

    protected PlanItemInstanceEntity createStageHierarchy(Stage stage, PlanItemInstanceEntity defaultParentPlanItemInstance, Map<String, Stage> stagesToCreate, 
                    Set<String> movingPlanItemInstanceIds, CaseInstanceChangeState caseInstanceChangeState, 
                    CaseInstanceEntity caseInstance, CommandContext commandContext) {
        
        if (caseInstanceChangeState.getCurrentStageInstances().containsKey(stage.getId())) {
            return (PlanItemInstanceEntity) caseInstanceChangeState.getCurrentStageInstances().get(stage.getId()).get(0);
        }

        if (caseInstanceChangeState.getCreatedStageInstances().containsKey(stage.getId())) {
            return caseInstanceChangeState.getCreatedStageInstances().get(stage.getId());
        }

        // Create the parent, if needed
        PlanItemInstanceEntity parentStageInstance = defaultParentPlanItemInstance;
        if (stage.getParentStage() != null && !stage.getParentStage().isPlanModel()) {
            parentStageInstance = createStageHierarchy(stage.getParentStage(), defaultParentPlanItemInstance, stagesToCreate, 
                            movingPlanItemInstanceIds, caseInstanceChangeState, caseInstance, commandContext);
            caseInstanceChangeState.getCreatedStageInstances().put(stage.getParentStage().getId(), parentStageInstance);
        }
        
        PlanItemInstanceEntityManager planItemInstanceEntityManager = CommandContextUtil.getPlanItemInstanceEntityManager(commandContext);

        List<PlanItemInstanceEntity> existingPlanItemInstances = planItemInstanceEntityManager.findByCaseInstanceIdAndPlanItemId(caseInstance.getId(), stage.getPlanItem().getId());
        PlanItemInstanceEntity newPlanItemInstance = null;
        if (!existingPlanItemInstances.isEmpty()) {
            for (PlanItemInstanceEntity existingPlanItemInstance : existingPlanItemInstances) {
                if (PlanItemInstanceState.AVAILABLE.equals(existingPlanItemInstance.getState())) {
                    newPlanItemInstance = existingPlanItemInstance;
                }
            }
        }
        
        if (newPlanItemInstance == null) {
            newPlanItemInstance = planItemInstanceEntityManager.createChildPlanItemInstance(stage.getPlanItem(), caseInstance.getCaseDefinitionId(), 
                            caseInstance.getId(), parentStageInstance != null ? parentStageInstance.getId() : null, 
                                            caseInstance.getTenantId(), true);
        }
        
        CommandContextUtil.getAgenda().planStartPlanItemInstanceOperation(newPlanItemInstance, null);

        return newPlanItemInstance;
    }

    protected void handleHumanTaskNewAssignee(PlanItemInstanceEntity taskPlanItemInstance, String newAssigneeId, CommandContext commandContext) {
        TaskService taskService = CommandContextUtil.getTaskService(commandContext);
        TaskEntityImpl task = (TaskEntityImpl) taskService.createTaskQuery().subScopeId(taskPlanItemInstance.getId()).scopeType(ScopeTypes.CMMN).singleResult();
        TaskHelper.changeTaskAssignee(task, newAssigneeId);
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
}