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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.flowable.batch.api.Batch;
import org.flowable.cmmn.api.migration.ActivatePlanItemDefinitionMapping;
import org.flowable.cmmn.api.migration.CaseInstanceMigrationCallback;
import org.flowable.cmmn.api.migration.CaseInstanceMigrationDocument;
import org.flowable.cmmn.api.migration.CaseInstanceMigrationValidationResult;
import org.flowable.cmmn.api.migration.MoveToAvailablePlanItemDefinitionMapping;
import org.flowable.cmmn.api.migration.PlanItemDefinitionMapping;
import org.flowable.cmmn.api.migration.TerminatePlanItemDefinitionMapping;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.history.CmmnHistoryManager;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseDefinitionEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntityManager;
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionUtil;
import org.flowable.cmmn.engine.impl.runtime.AbstractCmmnDynamicStateManager;
import org.flowable.cmmn.engine.impl.runtime.CaseInstanceChangeState;
import org.flowable.cmmn.engine.impl.runtime.CaseInstanceQueryImpl;
import org.flowable.cmmn.engine.impl.runtime.ChangePlanItemStateBuilderImpl;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Valentin Zickner
 */
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
    public void migrateCaseInstancesOfCaseDefinition(String caseDefinitionKey, int caseDefinitionVersion, String caseDefinitionTenantId, CaseInstanceMigrationDocument document, CommandContext commandContext) {
        CaseDefinition caseDefinition = resolveCaseDefinition(caseDefinitionKey, caseDefinitionVersion, caseDefinitionTenantId, commandContext);
        migrateCaseInstancesOfCaseDefinition(caseDefinition.getId(), document, commandContext);
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

    protected void doMigrateCaseInstance(CaseInstanceEntity caseInstance, CaseDefinition caseDefinitionToMigrateTo, CaseInstanceMigrationDocument document, CommandContext commandContext) {
        LOGGER.debug("Start migration of case instance with Id:'{}' to case definition identified by {}", caseInstance.getId(), printCaseDefinitionIdentifierMessage(document));
        ChangePlanItemStateBuilderImpl changePlanItemStateBuilder = prepareChangeStateBuilder(caseInstance, caseDefinitionToMigrateTo, document, commandContext);

        LOGGER.debug("Updating case definition reference of case root execution with id:'{}' to '{}'", caseInstance.getId(), caseDefinitionToMigrateTo.getId());
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
                .setCaseVariables(document.getCaseInstanceVariables())
                .setChildInstanceTaskVariables(document.getPlanItemLocalVariables());
        doMovePlanItemState(caseInstanceChangeState, commandContext);

        LOGGER.debug("Updating case definition reference in plan item instances");
        CommandContextUtil.getPlanItemInstanceEntityManager(commandContext).updatePlanItemInstancesCaseDefinitionId(caseInstance.getId(), caseDefinitionToMigrateTo.getId());

        LOGGER.debug("Updating case definition reference in history");
        changeCaseDefinitionReferenceOfHistory(caseInstance, caseDefinitionToMigrateTo, commandContext);
        
        List<CaseInstanceMigrationCallback> migrationCallbacks = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getCaseInstanceMigrationCallbacks();
        if (migrationCallbacks != null && !migrationCallbacks.isEmpty()) {
            for (CaseInstanceMigrationCallback caseInstanceMigrationCallback : migrationCallbacks) {
                caseInstanceMigrationCallback.caseInstanceMigrated(caseInstance, caseDefinitionToMigrateTo, document);
            }
        }
    }

    protected ChangePlanItemStateBuilderImpl prepareChangeStateBuilder(CaseInstance caseInstance, CaseDefinition caseDefinitionToMigrateTo, 
            CaseInstanceMigrationDocument document, CommandContext commandContext) {
        
        String destinationTenantId = caseDefinitionToMigrateTo.getTenantId();
        if (!Objects.equals(caseInstance.getTenantId(), destinationTenantId)) {
            
            CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
            if (cmmnEngineConfiguration.isFallbackToDefaultTenant() && cmmnEngineConfiguration.getDefaultTenantProvider() != null) {
                
                if (!Objects.equals(destinationTenantId, cmmnEngineConfiguration.getDefaultTenantProvider().getDefaultTenant(caseInstance.getId(), ScopeTypes.CMMN, caseDefinitionToMigrateTo.getKey()))) {
                    throw new FlowableException("Tenant mismatch between Case Instance ('" + caseInstance.getTenantId() + "') and Case Definition ('" + destinationTenantId + "') to migrate to");
                }
            
            } else {
                throw new FlowableException("Tenant mismatch between Case Instance ('" + caseInstance.getTenantId() + "') and Case Definition ('" + destinationTenantId + "') to migrate to");
            }
        }

        String caseInstanceId = caseInstance.getId();
        ChangePlanItemStateBuilderImpl changePlanItemStateBuilder = new ChangePlanItemStateBuilderImpl();
        changePlanItemStateBuilder.caseInstanceId(caseInstanceId);

        for (ActivatePlanItemDefinitionMapping planItemDefinitionMapping : document.getActivatePlanItemDefinitionMappings()) {
            changePlanItemStateBuilder.activatePlanItemDefinition(planItemDefinitionMapping);
        }
        
        for (TerminatePlanItemDefinitionMapping planItemDefinitionMapping : document.getTerminatePlanItemDefinitionMappings()) {
            changePlanItemStateBuilder.terminatePlanItemDefinitionId(planItemDefinitionMapping.getPlanItemDefinitionId());
        }
        
        for (MoveToAvailablePlanItemDefinitionMapping planItemDefinitionMapping : document.getMoveToAvailablePlanItemDefinitionMappings()) {
            changePlanItemStateBuilder.changeToAvailableStateByPlanItemDefinitionId(planItemDefinitionMapping.getPlanItemDefinitionId());
        }

        return changePlanItemStateBuilder;
    }

    protected void changeCaseDefinitionReferenceOfHistory(CaseInstanceEntity caseInstance, CaseDefinition caseDefinitionToMigrateTo, CommandContext commandContext) {
        CmmnHistoryManager historyManager = CommandContextUtil.getCmmnHistoryManager(commandContext);
        historyManager.updateCaseDefinitionIdInHistory(caseDefinitionToMigrateTo, caseInstance);
    }

    @Override
    public Batch batchMigrateCaseInstancesOfCaseDefinition(String caseDefinitionKey, int caseDefinitionVersion, String caseDefinitionTenantId, CaseInstanceMigrationDocument document, CommandContext commandContext) {
        CaseDefinition caseDefinition = resolveCaseDefinition(caseDefinitionKey, caseDefinitionVersion, caseDefinitionTenantId, commandContext);
        return batchMigrateCaseInstancesOfCaseDefinition(caseDefinition.getId(), document, commandContext);
    }

    @Override
    public Batch batchMigrateCaseInstancesOfCaseDefinition(String caseDefinitionId, CaseInstanceMigrationDocument document, CommandContext commandContext) {
        throw new UnsupportedOperationException("not implemented"); // TODO
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

    protected String printCaseDefinitionIdentifierMessage(CaseInstanceMigrationDocument document) {
        String id = document.getMigrateToCaseDefinitionId();
        String key = document.getMigrateToCaseDefinitionKey();
        Integer version = document.getMigrateToCaseDefinitionVersion();
        String tenantId = document.getMigrateToCaseDefinitionTenantId();
        return id != null ? "[id:'" + id + "']" : "[key:'" + key + "', version:'" + version + "', tenantId:'" + tenantId + "']";
    }

}
