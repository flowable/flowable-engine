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
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionUtil;
import org.flowable.cmmn.engine.impl.runtime.AbstractCmmnDynamicStateManager;
import org.flowable.cmmn.engine.impl.runtime.CaseInstanceQueryImpl;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.interceptor.CommandContext;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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
        throw new UnsupportedOperationException("not implemented"); // TODO
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
        CaseDefinition caseDefinition = resolveCaseDefinition(document, commandContext);
        if (caseDefinition == null) {
            throw new FlowableException("Cannot find the case definition to migrate to, identified by " + printCaseDefinitionIdentifierMessage(document));
        }

        CaseInstanceQueryImpl caseInstanceQueryByCaseDefinitionId = new CaseInstanceQueryImpl().caseDefinitionId(caseDefinitionId);
        CaseInstanceEntityManager caseInstanceEntityManager = CommandContextUtil.getCaseInstanceEntityManager(commandContext);
        List<CaseInstance> caseInstances = caseInstanceEntityManager.findByCriteria(caseInstanceQueryByCaseDefinitionId);

        for (CaseInstance caseInstance : caseInstances) {
            doMigrateCaseInstance(caseInstance, caseDefinition, document, commandContext);
        }
    }

    protected void doMigrateCaseInstance(CaseInstance caseInstance, CaseDefinition caseDefinition, CaseInstanceMigrationDocument document, CommandContext commandContext) {
        throw new UnsupportedOperationException("not implemented"); // TODO
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
        return Collections.emptyMap();
    }

    @Override
    protected boolean isDirectPlanItemDefinitionMigration(PlanItemDefinition currentPlanItemDefinition, PlanItemDefinition newPlanItemDefinition) {
        throw new UnsupportedOperationException("not implemented"); // TODO
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
