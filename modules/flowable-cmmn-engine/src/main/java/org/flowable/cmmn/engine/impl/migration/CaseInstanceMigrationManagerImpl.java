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

package org.flowable.cmmn.engine.impl.migration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.flowable.batch.api.Batch;
import org.flowable.batch.api.BatchPart;
import org.flowable.batch.api.BatchService;
import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.history.HistoricMilestoneInstance;
import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.migration.ActivatePlanItemDefinitionMapping;
import org.flowable.cmmn.api.migration.CaseInstanceBatchMigrationResult;
import org.flowable.cmmn.api.migration.CaseInstanceMigrationCallback;
import org.flowable.cmmn.api.migration.CaseInstanceMigrationDocument;
import org.flowable.cmmn.api.migration.CaseInstanceMigrationValidationResult;
import org.flowable.cmmn.api.migration.ChangePlanItemDefinitionWithNewTargetIdsMapping;
import org.flowable.cmmn.api.migration.ChangePlanItemIdMapping;
import org.flowable.cmmn.api.migration.ChangePlanItemIdWithDefinitionIdMapping;
import org.flowable.cmmn.api.migration.HistoricCaseInstanceMigrationDocument;
import org.flowable.cmmn.api.migration.MoveToAvailablePlanItemDefinitionMapping;
import org.flowable.cmmn.api.migration.PlanItemDefinitionMapping;
import org.flowable.cmmn.api.migration.RemoveWaitingForRepetitionPlanItemDefinitionMapping;
import org.flowable.cmmn.api.migration.TerminatePlanItemDefinitionMapping;
import org.flowable.cmmn.api.migration.WaitingForRepetitionPlanItemDefinitionMapping;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstanceState;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.history.CmmnHistoryManager;
import org.flowable.cmmn.engine.impl.history.HistoricCaseInstanceQueryImpl;
import org.flowable.cmmn.engine.impl.history.HistoricMilestoneInstanceQueryImpl;
import org.flowable.cmmn.engine.impl.history.HistoricPlanItemInstanceQueryImpl;
import org.flowable.cmmn.engine.impl.job.CaseInstanceMigrationJobHandler;
import org.flowable.cmmn.engine.impl.job.CaseInstanceMigrationStatusJobHandler;
import org.flowable.cmmn.engine.impl.job.HistoricCaseInstanceMigrationJobHandler;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseDefinitionEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricCaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricCaseInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricMilestoneInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricMilestoneInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricPlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricPlanItemInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntityManager;
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionUtil;
import org.flowable.cmmn.engine.impl.runtime.AbstractCmmnDynamicStateManager;
import org.flowable.cmmn.engine.impl.runtime.CaseInstanceChangeState;
import org.flowable.cmmn.engine.impl.runtime.CaseInstanceQueryImpl;
import org.flowable.cmmn.engine.impl.runtime.ChangePlanItemStateBuilderImpl;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.Stage;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.calendar.BusinessCalendar;
import org.flowable.common.engine.impl.calendar.CycleBusinessCalendar;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.service.JobService;
import org.flowable.job.service.TimerJobService;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.service.HistoricTaskService;
import org.flowable.task.service.impl.HistoricTaskInstanceQueryImpl;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskInstanceEntity;

public class CaseInstanceMigrationManagerImpl extends AbstractCmmnDynamicStateManager implements CaseInstanceMigrationManager {
    
    public CaseInstanceMigrationManagerImpl(CmmnEngineConfiguration cmmnEngineConfiguration) {
        super(cmmnEngineConfiguration);
    }

    @Override
    public CaseInstanceMigrationValidationResult validateMigrateCaseInstancesOfCaseDefinition(String caseDefinitionKey, int caseDefinitionVersion, String caseDefinitionTenantId, CaseInstanceMigrationDocument document, CommandContext commandContext) {
        CaseDefinition caseDefinition = resolveCaseDefinition(caseDefinitionKey, caseDefinitionVersion, caseDefinitionTenantId, commandContext);
        return validateMigrateCaseInstancesOfCaseDefinition(caseDefinition.getId(), document, commandContext);
    }

    @Override
    public CaseInstanceMigrationValidationResult validateMigrateCaseInstancesOfCaseDefinition(String caseDefinitionId, CaseInstanceMigrationDocument document, CommandContext commandContext) {
        CaseInstanceMigrationValidationResult validationResult = new CaseInstanceMigrationValidationResult();
        CaseDefinition caseDefinition = resolveCaseDefinition(document, commandContext);
        if (caseDefinition == null) {
            validationResult.addValidationMessage("Cannot find the case definition to migrate to " + printCaseDefinitionIdentifierMessage(document));
        } else {
            CmmnModel cmmnModel = CaseDefinitionUtil.getCmmnModel(caseDefinition.getId());
            if (cmmnModel == null) {
                validationResult.addValidationMessage("Cannot find the CMMN model of the case definition to migrate to, with " + printCaseDefinitionIdentifierMessage(document));
            } else {
                CmmnModel newModel = CaseDefinitionUtil.getCmmnModel(caseDefinition.getId());

                CaseInstanceEntityManager caseInstanceEntityManager = cmmnEngineConfiguration.getCaseInstanceEntityManager();
                List<CaseInstance> caseInstances = caseInstanceEntityManager.findByCriteria(
                        new CaseInstanceQueryImpl(commandContext, cmmnEngineConfiguration).caseDefinitionId(caseDefinitionId));

                for (CaseInstance caseInstance : caseInstances) {
                    doValidateCaseInstanceMigration(caseInstance.getId(), newModel, document, validationResult, commandContext);
                }
            }
        }

        return validationResult;
    }

    @Override
    public CaseInstanceMigrationValidationResult validateMigrateCaseInstance(String caseInstanceId, CaseInstanceMigrationDocument document, CommandContext commandContext) {
        CaseInstanceMigrationValidationResult validationResult = new CaseInstanceMigrationValidationResult();
        // Check that the caseDefinition exists and get its associated CmmnModel
        CaseDefinition caseDefinition = resolveCaseDefinition(document, commandContext);
        if (caseDefinition == null) {
            validationResult.addValidationMessage(("Cannot find the case definition to migrate to, with " + printCaseDefinitionIdentifierMessage(document)));
        } else {
            CmmnModel cmmnModel = CaseDefinitionUtil.getCmmnModel(caseDefinition.getId());
            if (cmmnModel == null) {
                validationResult.addValidationMessage("Cannot find the Cmmn model of the case definition to migrate to, with " + printCaseDefinitionIdentifierMessage(document));
            } else {
                doValidateCaseInstanceMigration(caseInstanceId, cmmnModel, document, validationResult, commandContext);
            }
        }

        return validationResult;
    }

    protected void doValidateCaseInstanceMigration(String caseInstanceId, CmmnModel newModel, CaseInstanceMigrationDocument document, 
            CaseInstanceMigrationValidationResult validationResult, CommandContext commandContext) {
        
        // Check that the caseInstance exists
        CaseInstanceEntityManager caseInstanceEntityManager = CommandContextUtil.getCaseInstanceEntityManager(commandContext);
        CaseInstanceEntity caseInstance = caseInstanceEntityManager.findById(caseInstanceId);
        if (caseInstance == null) {
            validationResult.addValidationMessage("Cannot find case instance with id:'" + caseInstanceId + "'");
            return;
        }

        doValidatePlanItemMappings(caseInstanceId, newModel, document, validationResult, commandContext);
    }

    protected void doValidatePlanItemMappings(String caseInstanceId, CmmnModel cmmnModel, 
            CaseInstanceMigrationDocument document, CaseInstanceMigrationValidationResult validationResult, CommandContext commandContext) {
        
        Map<String, PlanItemDefinitionMapping> activeMappingLookupMap = groupByFromPlanItemId(document.getActivatePlanItemDefinitionMappings(), validationResult);
        for (String planItemDefinitionId : activeMappingLookupMap.keySet()) {
            if (!hasPlanItemDefinition(cmmnModel, planItemDefinitionId)) {
                validationResult.addValidationMessage("Invalid mapping for activate plan item definition '" + planItemDefinitionId + "' cannot be found in the case definition");
            }
        }
        
        Map<String, PlanItemDefinitionMapping> terminateMappingLookupMap = groupByFromPlanItemId(document.getTerminatePlanItemDefinitionMappings(), validationResult);
        for (String planItemDefinitionId : terminateMappingLookupMap.keySet()) {
            if (!hasPlanItemDefinition(cmmnModel, planItemDefinitionId)) {
                validationResult.addValidationMessage("Invalid mapping for terminate plan item definition '" + planItemDefinitionId + "' cannot be found in the case definition");
            }
        }
        
        Map<String, PlanItemDefinitionMapping> moveToAvailableMappingLookupMap = groupByFromPlanItemId(document.getMoveToAvailablePlanItemDefinitionMappings(), validationResult);
        for (String planItemDefinitionId : moveToAvailableMappingLookupMap.keySet()) {
            if (!hasPlanItemDefinition(cmmnModel, planItemDefinitionId)) {
                validationResult.addValidationMessage("Invalid mapping for move to available plan item definition '" + planItemDefinitionId + "' cannot be found in the case definition");
            }
        }
        
        Map<String, PlanItemDefinitionMapping> waitingForRepetitionMappingLookupMap = groupByFromPlanItemId(document.getWaitingForRepetitionPlanItemDefinitionMappings(), validationResult);
        for (String planItemDefinitionId : waitingForRepetitionMappingLookupMap.keySet()) {
            if (!hasPlanItemDefinition(cmmnModel, planItemDefinitionId)) {
                validationResult.addValidationMessage("Invalid mapping for add waiting for repetition plan item definition '" + planItemDefinitionId + "' cannot be found in the case definition");
            }
        }
        
        Map<String, PlanItemDefinitionMapping> removeWaitingForRepetitionMappingLookupMap = groupByFromPlanItemId(document.getRemoveWaitingForRepetitionPlanItemDefinitionMappings(), validationResult);
        for (String planItemDefinitionId : removeWaitingForRepetitionMappingLookupMap.keySet()) {
            if (!hasPlanItemDefinition(cmmnModel, planItemDefinitionId)) {
                validationResult.addValidationMessage("Invalid mapping for remove waiting for repetition plan item definition '" + planItemDefinitionId + "' cannot be found in the case definition");
            }
        }
        
        for (ChangePlanItemDefinitionWithNewTargetIdsMapping changePlanItemDefinitionIdMapping : document.getChangePlanItemDefinitionWithNewTargetIdsMappings()) {
            if (!hasPlanItemDefinition(cmmnModel, changePlanItemDefinitionIdMapping.getNewPlanItemDefinitionId())) {
                validationResult.addValidationMessage("Invalid mapping for changing the plan item definition id from '" + changePlanItemDefinitionIdMapping.getExistingPlanItemDefinitionId() + 
                        "' to '" + changePlanItemDefinitionIdMapping.getNewPlanItemDefinitionId() + "', because the target can not be found in the case definition");
            }
        }
    }

    @Override
    public void migrateCaseInstance(String caseInstanceId, CaseInstanceMigrationDocument document, CommandContext commandContext) {
        CaseInstanceEntityManager caseInstanceEntityManager = CommandContextUtil.getCaseInstanceEntityManager(commandContext);
        CaseInstanceEntity caseInstance = caseInstanceEntityManager.findById(caseInstanceId);
        if (caseInstance == null) {
            throw new FlowableException("Cannot find the case to migrate, with id" + caseInstanceId);
        }

        CaseDefinition caseDefinitionToMigrateTo = resolveCaseDefinition(document, commandContext);
        doMigrateCaseInstance(caseInstance, caseDefinitionToMigrateTo, document, commandContext);
    }
    
    @Override
    public void migrateHistoricCaseInstance(String caseInstanceId, HistoricCaseInstanceMigrationDocument document, CommandContext commandContext) {
        HistoricCaseInstanceEntityManager historicCaseInstanceEntityManager = CommandContextUtil.getHistoricCaseInstanceEntityManager(commandContext);
        HistoricCaseInstanceEntity caseInstance = historicCaseInstanceEntityManager.findById(caseInstanceId);
        if (caseInstance == null) {
            throw new FlowableException("Cannot find the historic case instance to migrate, with id" + caseInstanceId);
        }
        
        if (!CaseInstanceState.END_STATES.contains(caseInstance.getState())) {
            throw new FlowableException("Historic case instance has not ended and can only be migrated with the regular case instance migrate method (migrateCaseInstance) for id " + caseInstanceId);
        }

        CaseDefinition caseDefinitionToMigrateTo = resolveCaseDefinition(document, commandContext);
        doMigrateHistoricCaseInstance(caseInstance, caseDefinitionToMigrateTo, document, commandContext);
    }

    @Override
    public void migrateCaseInstancesOfCaseDefinition(String caseDefinitionKey, int caseDefinitionVersion, String caseDefinitionTenantId, CaseInstanceMigrationDocument document, CommandContext commandContext) {
        CaseDefinition caseDefinition = resolveCaseDefinition(caseDefinitionKey, caseDefinitionVersion, caseDefinitionTenantId, commandContext);
        migrateCaseInstancesOfCaseDefinition(caseDefinition.getId(), document, commandContext);
    }
    
    @Override
    public void migrateHistoricCaseInstancesOfCaseDefinition(String caseDefinitionKey, int caseDefinitionVersion, String caseDefinitionTenantId, HistoricCaseInstanceMigrationDocument document, CommandContext commandContext) {
        CaseDefinition caseDefinition = resolveCaseDefinition(caseDefinitionKey, caseDefinitionVersion, caseDefinitionTenantId, commandContext);
        migrateHistoricCaseInstancesOfCaseDefinition(caseDefinition.getId(), document, commandContext);
    }

    @Override
    public void migrateCaseInstancesOfCaseDefinition(String caseDefinitionId, CaseInstanceMigrationDocument document, CommandContext commandContext) {
        CaseDefinition caseDefinitionToMigrateTo = resolveCaseDefinition(document, commandContext);
        if (caseDefinitionToMigrateTo == null) {
            throw new FlowableException("Cannot find the case definition to migrate to, identified by " + printCaseDefinitionIdentifierMessage(document));
        }

        CaseInstanceQueryImpl caseInstanceQueryByCaseDefinitionId = new CaseInstanceQueryImpl(commandContext, cmmnEngineConfiguration).caseDefinitionId(caseDefinitionId);
        CaseInstanceEntityManager caseInstanceEntityManager = cmmnEngineConfiguration.getCaseInstanceEntityManager();
        List<CaseInstance> caseInstances = caseInstanceEntityManager.findByCriteria(caseInstanceQueryByCaseDefinitionId);

        for (CaseInstance caseInstance : caseInstances) {
            doMigrateCaseInstance((CaseInstanceEntity) caseInstance, caseDefinitionToMigrateTo, document, commandContext);
        }
    }
    
    @Override
    public void migrateHistoricCaseInstancesOfCaseDefinition(String caseDefinitionId, HistoricCaseInstanceMigrationDocument document, CommandContext commandContext) {
        CaseDefinition caseDefinitionToMigrateTo = resolveCaseDefinition(document, commandContext);
        if (caseDefinitionToMigrateTo == null) {
            throw new FlowableException("Cannot find the case definition to migrate to, identified by " + printCaseDefinitionIdentifierMessage(document));
        }

        HistoricCaseInstanceQueryImpl historicCaseInstanceQuery = new HistoricCaseInstanceQueryImpl(commandContext, cmmnEngineConfiguration).caseDefinitionId(caseDefinitionId).finished();
        HistoricCaseInstanceEntityManager historicCaseInstanceEntityManager = cmmnEngineConfiguration.getHistoricCaseInstanceEntityManager();
        List<HistoricCaseInstance> historicCaseInstances = historicCaseInstanceEntityManager.findByCriteria(historicCaseInstanceQuery);

        for (HistoricCaseInstance historicCaseInstance : historicCaseInstances) {
            doMigrateHistoricCaseInstance((HistoricCaseInstanceEntity) historicCaseInstance, caseDefinitionToMigrateTo, document, commandContext);
        }
    }

    protected void doMigrateCaseInstance(CaseInstanceEntity caseInstance, CaseDefinition caseDefinitionToMigrateTo, CaseInstanceMigrationDocument document, CommandContext commandContext) {
        LOGGER.debug("Start migration of case instance with Id:'{}' to case definition identified by {}", caseInstance.getId(), printCaseDefinitionIdentifierMessage(document));
        if (document.getPreUpgradeExpression() != null && !document.getPreUpgradeExpression().isEmpty()) {
            cmmnEngineConfiguration.getExpressionManager().createExpression(document.getPreUpgradeExpression()).getValue(caseInstance);
        }

        ChangePlanItemStateBuilderImpl changePlanItemStateBuilder = prepareChangeStateBuilder(caseInstance, caseDefinitionToMigrateTo, document, commandContext);

        LOGGER.debug("Updating case definition reference of case root execution with id:'{}' to '{}'", caseInstance.getId(), caseDefinitionToMigrateTo.getId());
        String originalCaseDefinitionId = caseInstance.getCaseDefinitionId();
        caseInstance.setCaseDefinitionId(caseDefinitionToMigrateTo.getId());
        caseInstance.setCaseDefinitionKey(caseDefinitionToMigrateTo.getKey());
        caseInstance.setCaseDefinitionName(caseDefinitionToMigrateTo.getName());
        caseInstance.setCaseDefinitionVersion(caseDefinitionToMigrateTo.getVersion());
        caseInstance.setCaseDefinitionDeploymentId(caseDefinitionToMigrateTo.getDeploymentId());
        CommandContextUtil.getCaseInstanceEntityManager(commandContext).update(caseInstance);

        CaseInstanceChangeState caseInstanceChangeState = new CaseInstanceChangeState()
                .setCaseInstanceId(caseInstance.getId())
                .setCaseDefinitionToMigrateTo(caseDefinitionToMigrateTo)
                .setActivatePlanItemDefinitions(changePlanItemStateBuilder.getActivatePlanItemDefinitions())
                .setTerminatePlanItemDefinitions(changePlanItemStateBuilder.getTerminatePlanItemDefinitions())
                .setChangePlanItemDefinitionsToAvailable(changePlanItemStateBuilder.getChangeToAvailableStatePlanItemDefinitions())
                .setWaitingForRepetitionPlanItemDefinitions(changePlanItemStateBuilder.getWaitingForRepetitionPlanItemDefinitions())
                .setRemoveWaitingForRepetitionPlanItemDefinitions(changePlanItemStateBuilder.getRemoveWaitingForRepetitionPlanItemDefinitions())
                .setChangePlanItemIds(changePlanItemStateBuilder.getChangePlanItemIds())
                .setChangePlanItemIdsWithDefinitionId(changePlanItemStateBuilder.getChangePlanItemIdsWithDefinitionId())
                .setChangePlanItemDefinitionWithNewTargetIds(changePlanItemStateBuilder.getChangePlanItemDefinitionWithNewTargetIds())
                .setCaseVariables(document.getCaseInstanceVariables())
                .setChildInstanceTaskVariables(document.getPlanItemLocalVariables());
        doMovePlanItemState(caseInstanceChangeState, originalCaseDefinitionId, commandContext);

        LOGGER.debug("Updating case definition reference in plan item instances");
        CommandContextUtil.getPlanItemInstanceEntityManager(commandContext).updatePlanItemInstancesCaseDefinitionId(caseInstance.getId(), caseDefinitionToMigrateTo.getId());
        
        LOGGER.debug("Updating case definition reference in milestone instances");
        CommandContextUtil.getMilestoneInstanceEntityManager(commandContext).updateMilestoneInstancesCaseDefinitionId(caseInstance.getId(), caseDefinitionToMigrateTo.getId());
        
        LOGGER.debug("Updating case definition reference in sentry part instances");
        CommandContextUtil.getSentryPartInstanceEntityManager(commandContext).updateSentryPartInstancesCaseDefinitionId(caseInstance.getId(), caseDefinitionToMigrateTo.getId());

        LOGGER.debug("Updating case definition reference in history");
        changeCaseDefinitionReferenceOfHistory(caseInstance, caseDefinitionToMigrateTo, commandContext);
        
        List<CaseInstanceMigrationCallback> migrationCallbacks = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getCaseInstanceMigrationCallbacks();
        if (migrationCallbacks != null && !migrationCallbacks.isEmpty()) {
            for (CaseInstanceMigrationCallback caseInstanceMigrationCallback : migrationCallbacks) {
                caseInstanceMigrationCallback.caseInstanceMigrated(caseInstance, caseDefinitionToMigrateTo, document);
            }
        }

        if (document.getPostUpgradeExpression() != null && !document.getPostUpgradeExpression().isEmpty()) {
            cmmnEngineConfiguration.getExpressionManager().createExpression(document.getPostUpgradeExpression()).getValue(caseInstance);
        }
    }

    protected void doMigrateHistoricCaseInstance(HistoricCaseInstanceEntity historicCaseInstance, CaseDefinition caseDefinitionToMigrateTo, HistoricCaseInstanceMigrationDocument document, CommandContext commandContext) {
        LOGGER.debug("Start migration of historic case instance with Id:'{}' to case definition identified by {}", historicCaseInstance.getId(), printCaseDefinitionIdentifierMessage(document));
        
        String destinationTenantId = caseDefinitionToMigrateTo.getTenantId();
        if (!Objects.equals(historicCaseInstance.getTenantId(), destinationTenantId)) {
            
            CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
            if (cmmnEngineConfiguration.isFallbackToDefaultTenant() && cmmnEngineConfiguration.getDefaultTenantProvider() != null) {
                
                if (!Objects.equals(destinationTenantId, cmmnEngineConfiguration.getDefaultTenantProvider().getDefaultTenant(historicCaseInstance.getId(), ScopeTypes.CMMN, caseDefinitionToMigrateTo.getKey()))) {
                    throw new FlowableException("Tenant mismatch between Historic Case Instance ('" + historicCaseInstance.getTenantId() + "') and Case Definition ('" + destinationTenantId + "') to migrate to");
                }
            
            } else {
                throw new FlowableException("Tenant mismatch between Historic Case Instance ('" + historicCaseInstance.getTenantId() + "') and Case Definition ('" + destinationTenantId + "') to migrate to");
            }
        }
        
        LOGGER.debug("Updating case definition reference of case root execution with id:'{}' to '{}'", historicCaseInstance.getId(), caseDefinitionToMigrateTo.getId());
        historicCaseInstance.setCaseDefinitionId(caseDefinitionToMigrateTo.getId());
        historicCaseInstance.setCaseDefinitionKey(caseDefinitionToMigrateTo.getKey());
        historicCaseInstance.setCaseDefinitionName(caseDefinitionToMigrateTo.getName());
        historicCaseInstance.setCaseDefinitionVersion(caseDefinitionToMigrateTo.getVersion());
        historicCaseInstance.setCaseDefinitionDeploymentId(caseDefinitionToMigrateTo.getDeploymentId());
        CommandContextUtil.getHistoricCaseInstanceEntityManager(commandContext).update(historicCaseInstance);

        LOGGER.debug("Updating case definition reference in history");
        changeCaseDefinitionReferenceForHistoricCaseInstance(historicCaseInstance, caseDefinitionToMigrateTo, commandContext);
        
        List<CaseInstanceMigrationCallback> migrationCallbacks = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getCaseInstanceMigrationCallbacks();
        if (migrationCallbacks != null && !migrationCallbacks.isEmpty()) {
            for (CaseInstanceMigrationCallback caseInstanceMigrationCallback : migrationCallbacks) {
                caseInstanceMigrationCallback.historicCaseInstanceMigrated(historicCaseInstance, caseDefinitionToMigrateTo, document);
            }
        }
    }

    protected ChangePlanItemStateBuilderImpl prepareChangeStateBuilder(CaseInstance caseInstance, CaseDefinition caseDefinitionToMigrateTo, 
            CaseInstanceMigrationDocument document, CommandContext commandContext) {
        
        String destinationTenantId = caseDefinitionToMigrateTo.getTenantId();
        if (!Objects.equals(caseInstance.getTenantId(), destinationTenantId)) {
            
            CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
            if (cmmnEngineConfiguration.isFallbackToDefaultTenant() && cmmnEngineConfiguration.getDefaultTenantProvider() != null) {
                
                if (!Objects.equals(destinationTenantId, cmmnEngineConfiguration.getDefaultTenantProvider().getDefaultTenant(caseInstance.getTenantId(), ScopeTypes.CMMN, caseDefinitionToMigrateTo.getKey()))) {
                    throw new FlowableException("Tenant mismatch between Case Instance ('" + caseInstance.getTenantId() + "') and Case Definition ('" + destinationTenantId + "') to migrate to");
                }
            
            } else {
                throw new FlowableException("Tenant mismatch between Case Instance ('" + caseInstance.getTenantId() + "') and Case Definition ('" + destinationTenantId + "') to migrate to");
            }
        }

        String caseInstanceId = caseInstance.getId();
        ChangePlanItemStateBuilderImpl changePlanItemStateBuilder = new ChangePlanItemStateBuilderImpl();
        changePlanItemStateBuilder.caseInstanceId(caseInstanceId);

        List<String> mappedPlanItemDefinitionIds = new ArrayList<>();
        for (ActivatePlanItemDefinitionMapping planItemDefinitionMapping : document.getActivatePlanItemDefinitionMappings()) {
            mappedPlanItemDefinitionIds.add(planItemDefinitionMapping.getPlanItemDefinitionId());
            changePlanItemStateBuilder.activatePlanItemDefinition(planItemDefinitionMapping);
        }
        
        for (TerminatePlanItemDefinitionMapping planItemDefinitionMapping : document.getTerminatePlanItemDefinitionMappings()) {
            mappedPlanItemDefinitionIds.add(planItemDefinitionMapping.getPlanItemDefinitionId());
            changePlanItemStateBuilder.terminatePlanItemDefinition(planItemDefinitionMapping);
        }
        
        for (MoveToAvailablePlanItemDefinitionMapping planItemDefinitionMapping : document.getMoveToAvailablePlanItemDefinitionMappings()) {
            mappedPlanItemDefinitionIds.add(planItemDefinitionMapping.getPlanItemDefinitionId());
            changePlanItemStateBuilder.changeToAvailableStateByPlanItemDefinition(planItemDefinitionMapping);
        }
        
        for (WaitingForRepetitionPlanItemDefinitionMapping planItemDefinitionMapping : document.getWaitingForRepetitionPlanItemDefinitionMappings()) {
            mappedPlanItemDefinitionIds.add(planItemDefinitionMapping.getPlanItemDefinitionId());
            changePlanItemStateBuilder.addWaitingForRepetitionPlanItemDefinition(planItemDefinitionMapping);
        }
        
        for (RemoveWaitingForRepetitionPlanItemDefinitionMapping planItemDefinitionMapping : document.getRemoveWaitingForRepetitionPlanItemDefinitionMappings()) {
            mappedPlanItemDefinitionIds.add(planItemDefinitionMapping.getPlanItemDefinitionId());
            changePlanItemStateBuilder.removeWaitingForRepetitionPlanItemDefinition(planItemDefinitionMapping);
        }
        
        for (ChangePlanItemIdMapping changePlanItemIdMapping : document.getChangePlanItemIdMappings()) {
            changePlanItemStateBuilder.changePlanItemId(changePlanItemIdMapping.getExistingPlanItemId(), changePlanItemIdMapping.getNewPlanItemId());
        }
        
        for (ChangePlanItemIdWithDefinitionIdMapping changePlanItemIdWithDefinitionMapping : document.getChangePlanItemIdWithDefinitionIdMappings()) {
            changePlanItemStateBuilder.changePlanItemIdWithDefinitionId(changePlanItemIdWithDefinitionMapping.getExistingPlanItemDefinitionId(), changePlanItemIdWithDefinitionMapping.getNewPlanItemDefinitionId());
        }
        
        for (ChangePlanItemDefinitionWithNewTargetIdsMapping changePlanItemDefinitionWithNewTargetIdsMapping : document.getChangePlanItemDefinitionWithNewTargetIdsMappings()) {
            mappedPlanItemDefinitionIds.add(changePlanItemDefinitionWithNewTargetIdsMapping.getNewPlanItemDefinitionId());
            changePlanItemStateBuilder.changePlanItemDefinitionWithNewTargetIds(changePlanItemDefinitionWithNewTargetIdsMapping.getExistingPlanItemDefinitionId(), 
                    changePlanItemDefinitionWithNewTargetIdsMapping.getNewPlanItemId(), changePlanItemDefinitionWithNewTargetIdsMapping.getNewPlanItemDefinitionId());
        }
        
        if (document.getEnableAutomaticPlanItemInstanceCreation() != null && document.getEnableAutomaticPlanItemInstanceCreation()) {
            PlanItemInstanceEntityManager planItemInstanceEntityManager = CommandContextUtil.getPlanItemInstanceEntityManager(commandContext);
            List<PlanItemInstanceEntity> planItemInstances = planItemInstanceEntityManager.findByCaseInstanceId(caseInstance.getId());
            Map<String, PlanItemInstance> planItemDefinitionIdsWithPlanItemInstanceMap = new HashMap<>();
            for (PlanItemInstanceEntity planItemInstance : planItemInstances) {
                if (!planItemDefinitionIdsWithPlanItemInstanceMap.containsKey(planItemInstance.getPlanItemDefinitionId())) {
                    planItemDefinitionIdsWithPlanItemInstanceMap.put(planItemInstance.getPlanItemDefinitionId(), planItemInstance);
                
                } else if (PlanItemInstanceState.AVAILABLE.equals(planItemInstance.getState()) || PlanItemInstanceState.ACTIVE.equals(planItemInstance.getState())) {
                    planItemDefinitionIdsWithPlanItemInstanceMap.put(planItemInstance.getPlanItemDefinitionId(), planItemInstance);
                }
            }
            
            CmmnModel targetModel = CaseDefinitionUtil.getCmmnModel(caseDefinitionToMigrateTo.getId());
            Stage planModelStage = targetModel.getPrimaryCase().getPlanModel();
            checkForNewPlanItemInstancesInStage(planModelStage, mappedPlanItemDefinitionIds, planItemDefinitionIdsWithPlanItemInstanceMap, changePlanItemStateBuilder);
        }
        
        return changePlanItemStateBuilder;
    }
    
    protected void checkForNewPlanItemInstancesInStage(Stage stage, List<String> mappedPlanItemDefinitionIds, 
            Map<String, PlanItemInstance> planItemDefinitionIdsWithPlanItemInstanceMap, ChangePlanItemStateBuilderImpl changePlanItemStateBuilder) {
        
        for (PlanItemDefinition childPlanItemDefinition : stage.getPlanItemDefinitions()) {
            if (!mappedPlanItemDefinitionIds.contains(childPlanItemDefinition.getId()) && !planItemDefinitionIdsWithPlanItemInstanceMap.containsKey(childPlanItemDefinition.getId())) {
                MoveToAvailablePlanItemDefinitionMapping availableMappping = new MoveToAvailablePlanItemDefinitionMapping(childPlanItemDefinition.getId());
                changePlanItemStateBuilder.changeToAvailableStateByPlanItemDefinition(availableMappping);
            
            } else if (childPlanItemDefinition instanceof Stage) {
                Stage childStage = (Stage) childPlanItemDefinition;
                if (!mappedPlanItemDefinitionIds.contains(childPlanItemDefinition.getId())) {
                    PlanItemInstance stagePlanItemInstance = planItemDefinitionIdsWithPlanItemInstanceMap.get(childStage.getId());
                    if (PlanItemInstanceState.ACTIVE.equals(stagePlanItemInstance.getState())) {
                        checkForNewPlanItemInstancesInStage(childStage, mappedPlanItemDefinitionIds, planItemDefinitionIdsWithPlanItemInstanceMap, changePlanItemStateBuilder);
                    }
                }
            }
        }
    }

    protected void changeCaseDefinitionReferenceOfHistory(CaseInstanceEntity caseInstance, CaseDefinition caseDefinitionToMigrateTo, CommandContext commandContext) {
        CmmnHistoryManager historyManager = CommandContextUtil.getCmmnHistoryManager(commandContext);
        historyManager.updateCaseDefinitionIdInHistory(caseDefinitionToMigrateTo, caseInstance);
    }
    
    protected void changeCaseDefinitionReferenceForHistoricCaseInstance(HistoricCaseInstanceEntity historicCaseInstance, CaseDefinition caseDefinitionToMigrateTo, CommandContext commandContext) {
        if (CommandContextUtil.getCmmnEngineConfiguration(commandContext).getCmmnHistoryConfigurationSettings().isHistoryEnabled(caseDefinitionToMigrateTo.getId())) {
            HistoricTaskService historicTaskService = cmmnEngineConfiguration.getTaskServiceConfiguration().getHistoricTaskService();
            HistoricTaskInstanceQueryImpl taskQuery = new HistoricTaskInstanceQueryImpl();
            taskQuery.caseInstanceId(historicCaseInstance.getId());
            List<HistoricTaskInstance> historicTasks = historicTaskService.findHistoricTaskInstancesByQueryCriteria(taskQuery);
            if (historicTasks != null) {
                for (HistoricTaskInstance historicTaskInstance : historicTasks) {
                    HistoricTaskInstanceEntity taskEntity = (HistoricTaskInstanceEntity) historicTaskInstance;
                    taskEntity.setScopeDefinitionId(caseDefinitionToMigrateTo.getId());
                    historicTaskService.updateHistoricTask(taskEntity, true);
                }
            }

            // because of upgrade runtimeActivity instances can be only subset of historicActivity instances
            HistoricPlanItemInstanceQueryImpl historicPlanItemQuery = new HistoricPlanItemInstanceQueryImpl();
            historicPlanItemQuery.planItemInstanceCaseInstanceId(historicCaseInstance.getId());
            HistoricPlanItemInstanceEntityManager historicPlanItemInstanceEntityManager = cmmnEngineConfiguration.getHistoricPlanItemInstanceEntityManager();
            List<HistoricPlanItemInstance> historicPlanItems = historicPlanItemInstanceEntityManager.findByCriteria(historicPlanItemQuery);
            if (historicPlanItems != null) {
                for (HistoricPlanItemInstance historicPlanItemInstance : historicPlanItems) {
                    HistoricPlanItemInstanceEntity planItemEntity = (HistoricPlanItemInstanceEntity) historicPlanItemInstance;
                    planItemEntity.setCaseDefinitionId(caseDefinitionToMigrateTo.getId());
                    historicPlanItemInstanceEntityManager.update(planItemEntity);
                }
            }
            
            HistoricMilestoneInstanceQueryImpl historicMilestoneInstanceQuery = new HistoricMilestoneInstanceQueryImpl();
            historicMilestoneInstanceQuery.milestoneInstanceCaseInstanceId(historicCaseInstance.getId());
            HistoricMilestoneInstanceEntityManager historicMilestoneInstanceEntityManager = cmmnEngineConfiguration.getHistoricMilestoneInstanceEntityManager();
            List<HistoricMilestoneInstance> historicMilestoneInstances = historicMilestoneInstanceEntityManager.findHistoricMilestoneInstancesByQueryCriteria(historicMilestoneInstanceQuery);
            if (historicMilestoneInstances != null) {
                for (HistoricMilestoneInstance historicMilestoneInstance : historicMilestoneInstances) {
                    HistoricMilestoneInstanceEntity milestoneEntity = (HistoricMilestoneInstanceEntity) historicMilestoneInstance;
                    milestoneEntity.setCaseDefinitionId(caseDefinitionToMigrateTo.getId());
                    historicMilestoneInstanceEntityManager.update(milestoneEntity);
                }
            }
        }
    }

    @Override
    public Batch batchMigrateCaseInstancesOfCaseDefinition(String caseDefinitionKey, int caseDefinitionVersion, String caseDefinitionTenantId, CaseInstanceMigrationDocument document, CommandContext commandContext) {
        CaseDefinition caseDefinition = resolveCaseDefinition(caseDefinitionKey, caseDefinitionVersion, caseDefinitionTenantId, commandContext);
        return batchMigrateCaseInstancesOfCaseDefinition(caseDefinition.getId(), document, commandContext);
    }
    
    @Override
    public Batch batchMigrateHistoricCaseInstancesOfCaseDefinition(String caseDefinitionKey, int caseDefinitionVersion, String caseDefinitionTenantId, HistoricCaseInstanceMigrationDocument document, CommandContext commandContext) {
        CaseDefinition caseDefinition = resolveCaseDefinition(caseDefinitionKey, caseDefinitionVersion, caseDefinitionTenantId, commandContext);
        return batchMigrateHistoricCaseInstancesOfCaseDefinition(caseDefinition.getId(), document, commandContext);
    }

    @Override
    public Batch batchMigrateCaseInstancesOfCaseDefinition(String caseDefinitionId, CaseInstanceMigrationDocument document, CommandContext commandContext) {
        CaseDefinition caseDefinition = resolveCaseDefinition(document, commandContext);

        CmmnEngineConfiguration engineConfiguration = CommandContextUtil.getCmmnEngineConfiguration();
        List<CaseInstanceEntity> caseInstances = engineConfiguration.getCaseInstanceEntityManager()
                .findCaseInstancesByCaseDefinitionId(caseDefinitionId);

        BatchService batchService = engineConfiguration.getBatchServiceConfiguration().getBatchService();
        Batch batch = batchService.createBatchBuilder().batchType(Batch.CASE_MIGRATION_TYPE)
                .searchKey(caseDefinitionId)
                .searchKey2(caseDefinition.getId())
                .status(CaseInstanceBatchMigrationResult.STATUS_IN_PROGRESS)
                .batchDocumentJson(document.asJsonString())
                .tenantId(caseDefinition.getTenantId())
                .create();

        JobService jobService = engineConfiguration.getJobServiceConfiguration().getJobService();
        for (CaseInstance caseInstance : caseInstances) {
            BatchPart batchPart = batchService.createBatchPart(batch, CaseInstanceBatchMigrationResult.STATUS_WAITING,
                    caseInstance.getId(), null, ScopeTypes.CMMN);

            JobEntity job = jobService.createJob();
            job.setJobHandlerType(CaseInstanceMigrationJobHandler.TYPE);
            job.setTenantId(caseInstance.getTenantId());
            job.setScopeId(caseInstance.getId());
            job.setScopeType(ScopeTypes.CMMN);
            job.setJobHandlerConfiguration(CaseInstanceMigrationJobHandler.getHandlerCfgForBatchPartId(batchPart.getId()));
            jobService.createAsyncJob(job, false);
            job.setRetries(0);
            jobService.scheduleAsyncJob(job);
        }

        if (!caseInstances.isEmpty()) {
            TimerJobService timerJobService = engineConfiguration.getJobServiceConfiguration().getTimerJobService();
            TimerJobEntity timerJob = timerJobService.createTimerJob();
            timerJob.setJobType(JobEntity.JOB_TYPE_TIMER);
            timerJob.setTenantId(batch.getTenantId());
            timerJob.setRevision(1);
            timerJob.setRetries(0);
            timerJob.setJobHandlerType(CaseInstanceMigrationStatusJobHandler.TYPE);
            timerJob.setJobHandlerConfiguration(CaseInstanceMigrationJobHandler.getHandlerCfgForBatchId(batch.getId()));
            timerJob.setScopeType(ScopeTypes.CMMN);

            BusinessCalendar businessCalendar = engineConfiguration.getBusinessCalendarManager().getBusinessCalendar(CycleBusinessCalendar.NAME);
            timerJob.setDuedate(businessCalendar.resolveDuedate(engineConfiguration.getBatchStatusTimeCycleConfig()));
            timerJob.setRepeat(engineConfiguration.getBatchStatusTimeCycleConfig());

            timerJobService.scheduleTimerJob(timerJob);
        }

        return batch;
    }
    
    @Override
    public Batch batchMigrateHistoricCaseInstancesOfCaseDefinition(String caseDefinitionId, HistoricCaseInstanceMigrationDocument document, CommandContext commandContext) {
        CaseDefinition caseDefinition = resolveCaseDefinition(document, commandContext);

        CmmnEngineConfiguration engineConfiguration = CommandContextUtil.getCmmnEngineConfiguration();
        HistoricCaseInstanceQueryImpl historicCaseInstanceQuery = new HistoricCaseInstanceQueryImpl(commandContext, cmmnEngineConfiguration).caseDefinitionId(caseDefinitionId).finished();
        List<HistoricCaseInstance> historicCaseInstances = engineConfiguration.getHistoricCaseInstanceEntityManager()
                .findByCriteria(historicCaseInstanceQuery);

        BatchService batchService = engineConfiguration.getBatchServiceConfiguration().getBatchService();
        Batch batch = batchService.createBatchBuilder().batchType(Batch.CASE_MIGRATION_TYPE)
                .searchKey(caseDefinitionId)
                .searchKey2(caseDefinition.getId())
                .status(CaseInstanceBatchMigrationResult.STATUS_IN_PROGRESS)
                .batchDocumentJson(document.asJsonString())
                .tenantId(caseDefinition.getTenantId())
                .create();

        JobService jobService = engineConfiguration.getJobServiceConfiguration().getJobService();
        for (HistoricCaseInstance historicCaseInstance : historicCaseInstances) {
            BatchPart batchPart = batchService.createBatchPart(batch, CaseInstanceBatchMigrationResult.STATUS_WAITING,
                    historicCaseInstance.getId(), null, ScopeTypes.CMMN);

            JobEntity job = jobService.createJob();
            job.setJobHandlerType(HistoricCaseInstanceMigrationJobHandler.TYPE);
            job.setScopeId(historicCaseInstance.getId());
            job.setScopeType(ScopeTypes.CMMN);
            job.setJobHandlerConfiguration(HistoricCaseInstanceMigrationJobHandler.getHandlerCfgForBatchPartId(batchPart.getId()));
            job.setTenantId(historicCaseInstance.getTenantId());
            jobService.createAsyncJob(job, false);
            jobService.scheduleAsyncJob(job);
        }

        if (!historicCaseInstances.isEmpty()) {
            TimerJobService timerJobService = engineConfiguration.getJobServiceConfiguration().getTimerJobService();
            TimerJobEntity timerJob = timerJobService.createTimerJob();
            timerJob.setJobType(JobEntity.JOB_TYPE_TIMER);
            timerJob.setRevision(1);
            timerJob.setJobHandlerType(CaseInstanceMigrationStatusJobHandler.TYPE);
            timerJob.setJobHandlerConfiguration(HistoricCaseInstanceMigrationJobHandler.getHandlerCfgForBatchId(batch.getId()));
            timerJob.setScopeType(ScopeTypes.CMMN);
            timerJob.setTenantId(batch.getTenantId());

            BusinessCalendar businessCalendar = engineConfiguration.getBusinessCalendarManager().getBusinessCalendar(CycleBusinessCalendar.NAME);
            timerJob.setDuedate(businessCalendar.resolveDuedate(engineConfiguration.getBatchStatusTimeCycleConfig()));
            timerJob.setRepeat(engineConfiguration.getBatchStatusTimeCycleConfig());

            timerJobService.scheduleTimerJob(timerJob);
        }

        return batch;
    }

    @Override
    protected boolean isDirectPlanItemDefinitionMigration(PlanItemDefinition currentPlanItemDefinition, PlanItemDefinition newPlanItemDefinition) {
        return false;
    }

    protected Map<String, PlanItemDefinitionMapping> groupByFromPlanItemId(List<? extends PlanItemDefinitionMapping> planItemDefinitionMappings, CaseInstanceMigrationValidationResult validationResult) {
        Map<String, PlanItemDefinitionMapping> lookupMap = new HashMap<>();
        for (PlanItemDefinitionMapping planItemDefinitionMapping : planItemDefinitionMappings) {
            if (lookupMap.containsKey(planItemDefinitionMapping.getPlanItemDefinitionId()) && validationResult != null) {
                validationResult.addValidationMessage("Duplicate mapping for '" + planItemDefinitionMapping.getPlanItemDefinitionId() + 
                        "', the latest mapping is going to be used");
            }
            lookupMap.put(planItemDefinitionMapping.getPlanItemDefinitionId(), planItemDefinitionMapping);
        }
        return lookupMap;
    }

    protected boolean hasPlanItemDefinition(CmmnModel model, String elementId) {
        return model.getPrimaryCase().getAllCaseElements().containsKey(elementId);
    }

    protected CaseDefinition resolveCaseDefinition(CaseInstanceMigrationDocument document, CommandContext commandContext) {
        if (document.getMigrateToCaseDefinitionId() != null) {
            CaseDefinitionEntityManager caseDefinitionEntityManager = CommandContextUtil.getCaseDefinitionEntityManager(commandContext);
            return caseDefinitionEntityManager.findById(document.getMigrateToCaseDefinitionId());

        } else {
            return resolveCaseDefinition(document.getMigrateToCaseDefinitionKey(), document.getMigrateToCaseDefinitionVersion(), document.getMigrateToCaseDefinitionTenantId(), commandContext);
        }
    }
    
    protected CaseDefinition resolveCaseDefinition(HistoricCaseInstanceMigrationDocument document, CommandContext commandContext) {
        if (document.getMigrateToCaseDefinitionId() != null) {
            CaseDefinitionEntityManager caseDefinitionEntityManager = CommandContextUtil.getCaseDefinitionEntityManager(commandContext);
            return caseDefinitionEntityManager.findById(document.getMigrateToCaseDefinitionId());

        } else {
            return resolveCaseDefinition(document.getMigrateToCaseDefinitionKey(), document.getMigrateToCaseDefinitionVersion(), document.getMigrateToCaseDefinitionTenantId(), commandContext);
        }
    }

    protected String printCaseDefinitionIdentifierMessage(CaseInstanceMigrationDocument document) {
        String id = document.getMigrateToCaseDefinitionId();
        String key = document.getMigrateToCaseDefinitionKey();
        Integer version = document.getMigrateToCaseDefinitionVersion();
        String tenantId = document.getMigrateToCaseDefinitionTenantId();
        return id != null ? "[id:'" + id + "']" : "[key:'" + key + "', version:'" + version + "', tenantId:'" + tenantId + "']";
    }

    protected String printCaseDefinitionIdentifierMessage(HistoricCaseInstanceMigrationDocument document) {
        String id = document.getMigrateToCaseDefinitionId();
        String key = document.getMigrateToCaseDefinitionKey();
        Integer version = document.getMigrateToCaseDefinitionVersion();
        String tenantId = document.getMigrateToCaseDefinitionTenantId();
        return id != null ? "[id:'" + id + "']" : "[key:'" + key + "', version:'" + version + "', tenantId:'" + tenantId + "']";
    }
}
