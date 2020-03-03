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

import org.flowable.batch.api.Batch;
import org.flowable.cmmn.api.migration.CaseInstanceMigrationDocument;
import org.flowable.cmmn.api.migration.CaseInstanceMigrationValidationResult;
import org.flowable.cmmn.api.migration.PlanItemMigrationMapping;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseDefinitionEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntityManager;
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionUtil;
import org.flowable.cmmn.engine.impl.runtime.AbstractCmmnDynamicStateManager;
import org.flowable.cmmn.engine.impl.runtime.CaseInstanceChangeState;
import org.flowable.cmmn.engine.impl.runtime.CaseInstanceQueryImpl;
import org.flowable.cmmn.engine.impl.runtime.ChangePlanItemStateBuilderImpl;
import org.flowable.cmmn.engine.impl.runtime.MovePlanItemInstanceEntityContainer;
import org.flowable.cmmn.engine.impl.runtime.PlanItemInstanceQueryImpl;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.interceptor.CommandContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Valentin Zickner
 */
public class CaseInstanceMigrationManagerImpl extends AbstractCmmnDynamicStateManager implements CaseInstanceMigrationManager {

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

                CaseInstanceEntityManager caseInstanceEntityManager = CommandContextUtil.getCaseInstanceEntityManager(commandContext);
                List<CaseInstance> caseInstances = caseInstanceEntityManager.findByCriteria(new CaseInstanceQueryImpl().caseDefinitionId(caseDefinitionId));

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

    protected void doValidateCaseInstanceMigration(String caseInstanceId, CmmnModel newModel, CaseInstanceMigrationDocument document, CaseInstanceMigrationValidationResult validationResult, CommandContext commandContext) {
        // Check that the caseInstance exists
        CaseInstanceEntityManager caseInstanceEntityManager = CommandContextUtil.getCaseInstanceEntityManager(commandContext);
        CaseInstanceEntity caseInstance = caseInstanceEntityManager.findById(caseInstanceId);
        if (caseInstance == null) {
            validationResult.addValidationMessage("Cannot find case instance with id:'" + caseInstanceId + "'");
            return;
        }

        doValidatePlanItemMappings(caseInstanceId, document.getPlanItemMigrationMappings(), newModel, document, validationResult, commandContext);
    }

    protected void doValidatePlanItemMappings(String caseInstanceId, List<PlanItemMigrationMapping> planItemMigrationMappings, CmmnModel newModel, CaseInstanceMigrationDocument document, CaseInstanceMigrationValidationResult validationResult, CommandContext commandContext) {
        PlanItemInstanceEntityManager planItemInstanceEntityManager = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getPlanItemInstanceEntityManager();

        List<PlanItemInstanceEntity> activeMainPlanItemInstances = planItemInstanceEntityManager.findByCaseInstanceId(caseInstanceId);
        Map<String, PlanItemMigrationMapping> mappingLookupMap = groupByFromPlanItemId(planItemMigrationMappings, validationResult);

        for (PlanItemInstanceEntity activeMainPlanItemInstance : activeMainPlanItemInstances) {
            String elementId = activeMainPlanItemInstance.getElementId();
            if (!mappingLookupMap.containsKey(elementId)) {
                checkAutoMapping(caseInstanceId, newModel, validationResult, activeMainPlanItemInstance, elementId);
            } else {
                checkManualMapping(newModel, validationResult, mappingLookupMap, elementId);
            }
        }

    }

    protected void checkAutoMapping(String caseInstanceId, CmmnModel newModel, CaseInstanceMigrationValidationResult validationResult, PlanItemInstanceEntity activeMainPlanItemInstance, String elementId) {
        if (!hasPlanItemDefined(newModel, elementId)) {
            validationResult.addValidationMessage("Case instance (id:'" + caseInstanceId + "') has a running plan item (id:'" + activeMainPlanItemInstance.getId() + "') that is not mapped for migration");
        }
    }

    protected void checkManualMapping(CmmnModel newModel, CaseInstanceMigrationValidationResult validationResult, Map<String, PlanItemMigrationMapping> mappingLookupMap, String elementId) {
        PlanItemMigrationMapping migrationMapping = mappingLookupMap.get(elementId);
        for (String toPlanItemId : migrationMapping.getToPlanItemIds()) {
            if (!hasPlanItemDefined(newModel, toPlanItemId)) {
                validationResult.addValidationMessage("Invalid mapping for '" + elementId + "' to '" + toPlanItemId + "', cannot be found in the case definition");
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

        CaseInstanceQueryImpl caseInstanceQueryByCaseDefinitionId = new CaseInstanceQueryImpl().caseDefinitionId(caseDefinitionId);
        CaseInstanceEntityManager caseInstanceEntityManager = CommandContextUtil.getCaseInstanceEntityManager(commandContext);
        List<CaseInstance> caseInstances = caseInstanceEntityManager.findByCriteria(caseInstanceQueryByCaseDefinitionId);

        for (CaseInstance caseInstance : caseInstances) {
            doMigrateCaseInstance(caseInstance, caseDefinitionToMigrateTo, document, commandContext);
        }
    }

    protected void doMigrateCaseInstance(CaseInstance caseInstance, CaseDefinition caseDefinitionToMigrateTo, CaseInstanceMigrationDocument document, CommandContext commandContext) {
        LOGGER.debug("Start migration of case instance with Id:'{}' to case definition identified by {}", caseInstance.getId(), printCaseDefinitionIdentifierMessage(document));
        List<String> activePlanItemDefinitions = new ArrayList<>();
        List<String> availablePlanItemDefinitions = new ArrayList<>();
        List<ChangePlanItemStateBuilderImpl> changePlanItemStateBuilders = prepareChangeStateBuilders(caseInstance, caseDefinitionToMigrateTo, document, availablePlanItemDefinitions, activePlanItemDefinitions, commandContext);

        LOGGER.debug("Updating case definition reference of case root execution with id:'{}' to '{}'", caseInstance.getId(), caseDefinitionToMigrateTo.getId());
        ((CaseInstanceEntity) caseInstance).setCaseDefinitionId(caseDefinitionToMigrateTo.getId());

        LOGGER.debug("Resolve plan item instances to migrate");
        List<MovePlanItemInstanceEntityContainer> moveExecutionEntityContainerList = new ArrayList<>();
        for (ChangePlanItemStateBuilderImpl builder : changePlanItemStateBuilders) {
            moveExecutionEntityContainerList.addAll(resolveMovePlanItemInstanceEntityContainers(builder, caseDefinitionToMigrateTo.getId(), document.getCaseInstanceVariables(), commandContext));
        }

        CaseInstanceChangeState caseInstanceChangeState = new CaseInstanceChangeState()
                .setCaseInstanceId(caseInstance.getId())
                .setCaseDefinitionToMigrateTo(caseDefinitionToMigrateTo)
                .setMovePlanItemInstanceEntityContainers(moveExecutionEntityContainerList)
                .setActivatePlanItemDefinitionIds(activePlanItemDefinitions)
                .setChangePlanItemToAvailableIdList(availablePlanItemDefinitions)
                .setCaseVariables(document.getCaseInstanceVariables())
                .setChildInstanceTaskVariables(document.getPlanItemLocalVariables());
        doMovePlanItemState(caseInstanceChangeState, commandContext);

        LOGGER.debug("Updating case definition of unchanged case tasks");
        // TODO? need to handle case tasks?

        LOGGER.debug("Updating case definition reference in plan item instances");
        CommandContextUtil.getPlanItemInstanceEntityManager(commandContext).updatePlanItemInstancesCaseDefinitionId(caseInstance.getId(), caseDefinitionToMigrateTo.getId());

        LOGGER.debug("Updating case definition reference in history");
        changeCaseDefinitionReferenceOfHistory(caseInstance, caseDefinitionToMigrateTo, commandContext);
    }

    protected List<ChangePlanItemStateBuilderImpl> prepareChangeStateBuilders(CaseInstance caseInstance, CaseDefinition caseDefinitionToMigrateTo, CaseInstanceMigrationDocument document, List<String> availablePlanItemDefinitions, List<String> activePlanItemDefinitions, CommandContext commandContext) {
        PlanItemInstanceEntityManager planItemInstanceEntityManager = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getPlanItemInstanceEntityManager();

        String destinationTenantId = caseDefinitionToMigrateTo.getTenantId();
        if (!Objects.equals(caseInstance.getTenantId(), destinationTenantId)) {
            throw new FlowableException("Tenant mismatch between Case Instance ('" + caseInstance.getTenantId() + "') and Case Definition ('" + destinationTenantId + "') to migrate to");
        }

        String caseInstanceId = caseInstance.getId();
        List<ChangePlanItemStateBuilderImpl> changePlanItemStateBuilders = new ArrayList<>();
        ChangePlanItemStateBuilderImpl changePlanItemStateBuilder = new ChangePlanItemStateBuilderImpl();
        changePlanItemStateBuilder.caseInstanceId(caseInstanceId);
        changePlanItemStateBuilders.add(changePlanItemStateBuilder);


        Map<String, List<PlanItemInstance>> planItemInstances = planItemInstanceEntityManager.findByCriteria(new PlanItemInstanceQueryImpl().caseInstanceId(caseInstanceId).planItemInstanceStateActive())
                .stream()
                .collect(Collectors.groupingBy(PlanItemInstance::getPlanItemDefinitionId));
        Map<String, PlanItemMigrationMapping> mappingLookupMap = groupByFromPlanItemId(document.getPlanItemMigrationMappings(), null);
        Set<String> mappedPlanItems = mappingLookupMap.keySet();

        // Partition the plan items by explicitly mapped or not
        Map<Boolean, List<String>> partitionedExecutionActivityIds = planItemInstances.keySet()
                .stream()
                .collect(Collectors.partitioningBy(mappedPlanItems::contains));
        List<String> planItemsIdsToAutoMap = partitionedExecutionActivityIds.get(false);
        List<String> planItemsIdsToMapExplicitly = partitionedExecutionActivityIds.get(true);

        for (String planItemDefinitionId : planItemsIdsToAutoMap) {
            List<PlanItemInstance> planItemInstanceEntities = planItemInstances.get(planItemDefinitionId);
            if (planItemInstanceEntities.size() > 1) {
                List<String> planItemInstanceIds = planItemInstanceEntities.stream().map(PlanItemInstance::getId).collect(Collectors.toList());
                changePlanItemStateBuilder.movePlanItemInstancesToSinglePlanItemDefinitionId(planItemInstanceIds, planItemDefinitionId);
            } else {
                PlanItemInstance planItemInstanceEntity = planItemInstanceEntities.get(0);
                changePlanItemStateBuilder.movePlanItemInstanceToPlanItemDefinitionId(planItemInstanceEntity.getId(), planItemDefinitionId);
            }
        }

        for (PlanItemMigrationMapping planItemMigrationMapping : document.getPlanItemMigrationMappings()) {
            if (planItemMigrationMapping instanceof PlanItemMigrationMapping.OneToOneMapping) {
                String fromPlanItemId = ((PlanItemMigrationMapping.OneToOneMapping) planItemMigrationMapping).getFromPlanItemId();
                String toPlanItemId = ((PlanItemMigrationMapping.OneToOneMapping) planItemMigrationMapping).getToPlanItemId();
                String newAssignee = ((PlanItemMigrationMapping.OneToOneMapping) planItemMigrationMapping).getWithNewAssignee();

                if (planItemsIdsToMapExplicitly.contains(fromPlanItemId)) {
                    changePlanItemStateBuilder.movePlanItemDefinitionIdTo(fromPlanItemId, toPlanItemId, newAssignee);
                    planItemsIdsToMapExplicitly.remove(fromPlanItemId);
                }
            } else {
                throw new UnsupportedOperationException("Unknown migration type or not yet implemented");
            }
        }

        return changePlanItemStateBuilders;
    }

    protected void changeCaseDefinitionReferenceOfHistory(CaseInstance caseInstance, CaseDefinition caseDefinitionToMigrateTo, CommandContext commandContext) {
 //       throw new UnsupportedOperationException("not implemented"); // TODO
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
    protected Map<String, List<PlanItemInstance>> resolveActiveStagePlanItemInstances(String caseInstanceId, CommandContext commandContext) {
        // TODO is this correct?
        PlanItemInstanceEntityManager planItemInstanceEntityManager = CommandContextUtil.getPlanItemInstanceEntityManager(commandContext);
        PlanItemInstanceQueryImpl planItemInstanceQuery = new PlanItemInstanceQueryImpl(commandContext);
        planItemInstanceQuery.caseInstanceId(caseInstanceId)
                .onlyStages()
                .planItemInstanceStateActive();
        List<PlanItemInstance> planItemInstances = planItemInstanceEntityManager.findByCriteria(planItemInstanceQuery);

        return planItemInstances.stream()
                .collect(Collectors.groupingBy(PlanItemInstance::getPlanItemDefinitionId));
    }

    @Override
    protected boolean isDirectPlanItemDefinitionMigration(PlanItemDefinition currentPlanItemDefinition, PlanItemDefinition newPlanItemDefinition) {
        return false;
    }

    protected Map<String, PlanItemMigrationMapping> groupByFromPlanItemId(List<PlanItemMigrationMapping> planItemMigrationMappings, CaseInstanceMigrationValidationResult validationResult) {
        Map<String, PlanItemMigrationMapping> lookupMap = new HashMap<>();
        for (PlanItemMigrationMapping planItemMigrationMapping : planItemMigrationMappings) {
            for (String planItemId : planItemMigrationMapping.getFromPlanItemIds()) {
                if (lookupMap.containsKey(planItemId) && validationResult != null) {
                    validationResult.addValidationMessage("Duplicate mapping for '" + planItemId + "', the latest mapping is going to be used");
                }
                lookupMap.put(planItemId, planItemMigrationMapping);
            }
        }
        return lookupMap;
    }

    protected boolean hasPlanItemDefined(CmmnModel model, String elementId) {
        return model.getPrimaryCase()
                .getAllCaseElements()
                .containsKey(elementId);
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
