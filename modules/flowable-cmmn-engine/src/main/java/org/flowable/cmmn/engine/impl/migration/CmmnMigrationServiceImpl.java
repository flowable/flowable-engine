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
import org.flowable.cmmn.api.CmmnMigrationService;
import org.flowable.cmmn.api.migration.CaseInstanceBatchMigrationResult;
import org.flowable.cmmn.api.migration.CaseInstanceMigrationBuilder;
import org.flowable.cmmn.api.migration.CaseInstanceMigrationDocument;
import org.flowable.cmmn.api.migration.CaseInstanceMigrationValidationResult;
import org.flowable.cmmn.api.migration.HistoricCaseInstanceMigrationBuilder;
import org.flowable.cmmn.api.migration.HistoricCaseInstanceMigrationDocument;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.cmd.CaseInstanceMigrationBatchCmd;
import org.flowable.cmmn.engine.impl.cmd.CaseInstanceMigrationCmd;
import org.flowable.cmmn.engine.impl.cmd.CaseInstanceMigrationValidationCmd;
import org.flowable.cmmn.engine.impl.cmd.GetCaseInstanceMigrationBatchResultCmd;
import org.flowable.cmmn.engine.impl.cmd.HistoricCaseInstanceMigrationBatchCmd;
import org.flowable.cmmn.engine.impl.cmd.HistoricCaseInstanceMigrationCmd;
import org.flowable.common.engine.impl.service.CommonEngineServiceImpl;

/**
 * @author Valentin Zickner
 */
public class CmmnMigrationServiceImpl extends CommonEngineServiceImpl<CmmnEngineConfiguration> implements CmmnMigrationService {

    public CmmnMigrationServiceImpl(CmmnEngineConfiguration configuration) {
        super(configuration);
    }

    @Override
    public CaseInstanceMigrationBuilder createCaseInstanceMigrationBuilder() {
        return new CaseInstanceMigrationBuilderImpl(this);
    }

    @Override
    public CaseInstanceMigrationBuilder createCaseInstanceMigrationBuilderFromCaseInstanceMigrationDocument(CaseInstanceMigrationDocument document) {
        return createCaseInstanceMigrationBuilder().fromCaseInstanceMigrationDocument(document);
    }
    
    @Override
    public HistoricCaseInstanceMigrationBuilder createHistoricCaseInstanceMigrationBuilder() {
        return new HistoricCaseInstanceMigrationBuilderImpl(this);
    }

    @Override
    public HistoricCaseInstanceMigrationBuilder createHistoricCaseInstanceMigrationBuilderFromHistoricCaseInstanceMigrationDocument(HistoricCaseInstanceMigrationDocument document) {
        return createHistoricCaseInstanceMigrationBuilder().fromHistoricCaseInstanceMigrationDocument(document);
    }

    @Override
    public CaseInstanceMigrationValidationResult validateMigrationForCaseInstance(String caseInstanceId, CaseInstanceMigrationDocument caseInstanceMigrationDocument) {
        return commandExecutor.execute(new CaseInstanceMigrationValidationCmd(caseInstanceId, caseInstanceMigrationDocument, configuration));
    }

    @Override
    public CaseInstanceMigrationValidationResult validateMigrationForCaseInstancesOfCaseDefinition(String caseDefinitionId, CaseInstanceMigrationDocument caseInstanceMigrationDocument) {
        return commandExecutor.execute(new CaseInstanceMigrationValidationCmd(caseInstanceMigrationDocument, caseDefinitionId, configuration));
    }

    @Override
    public CaseInstanceMigrationValidationResult validateMigrationForCaseInstancesOfCaseDefinition(String caseDefinitionKey, int caseDefinitionVersion, String caseDefinitionTenantId, CaseInstanceMigrationDocument caseInstanceMigrationDocument) {
        return commandExecutor.execute(new CaseInstanceMigrationValidationCmd(caseDefinitionKey, caseDefinitionVersion, 
                caseDefinitionTenantId, caseInstanceMigrationDocument, configuration));
    }

    @Override
    public void migrateCaseInstance(String caseInstanceId, CaseInstanceMigrationDocument caseInstanceMigrationDocument) {
        commandExecutor.execute(new CaseInstanceMigrationCmd(caseInstanceId, caseInstanceMigrationDocument, configuration));
    }
    
    @Override
    public void migrateHistoricCaseInstance(String caseInstanceId, HistoricCaseInstanceMigrationDocument historicCaseInstanceMigrationDocument) {
        commandExecutor.execute(new HistoricCaseInstanceMigrationCmd(caseInstanceId, historicCaseInstanceMigrationDocument, configuration));
    }

    @Override
    public void migrateCaseInstancesOfCaseDefinition(String caseDefinitionId, CaseInstanceMigrationDocument caseInstanceMigrationDocument) {
        commandExecutor.execute(new CaseInstanceMigrationCmd(caseInstanceMigrationDocument, caseDefinitionId, configuration));
    }
    
    @Override
    public void migrateHistoricCaseInstancesOfCaseDefinition(String caseDefinitionId, HistoricCaseInstanceMigrationDocument historicCaseInstanceMigrationDocument) {
        commandExecutor.execute(new HistoricCaseInstanceMigrationCmd(historicCaseInstanceMigrationDocument, caseDefinitionId, configuration));
    }

    @Override
    public void migrateCaseInstancesOfCaseDefinition(String caseDefinitionKey, int caseDefinitionVersion, String caseDefinitionTenantId, CaseInstanceMigrationDocument caseInstanceMigrationDocument) {
        commandExecutor.execute(new CaseInstanceMigrationCmd(caseDefinitionKey, caseDefinitionVersion, caseDefinitionTenantId, 
                caseInstanceMigrationDocument, configuration));
    }
    
    @Override
    public void migrateHistoricCaseInstancesOfCaseDefinition(String caseDefinitionKey, int caseDefinitionVersion, String caseDefinitionTenantId, HistoricCaseInstanceMigrationDocument historicCaseInstanceMigrationDocument) {
        commandExecutor.execute(new HistoricCaseInstanceMigrationCmd(caseDefinitionKey, caseDefinitionVersion, caseDefinitionTenantId, 
                historicCaseInstanceMigrationDocument, configuration));
    }

    @Override
    public Batch batchMigrateCaseInstancesOfCaseDefinition(String caseDefinitionId, CaseInstanceMigrationDocument caseInstanceMigrationDocument) {
        return commandExecutor.execute(new CaseInstanceMigrationBatchCmd(caseInstanceMigrationDocument, caseDefinitionId, configuration));
    }
    
    @Override
    public Batch batchMigrateHistoricCaseInstancesOfCaseDefinition(String caseDefinitionId, HistoricCaseInstanceMigrationDocument historicCaseInstanceMigrationDocument) {
        return commandExecutor.execute(new HistoricCaseInstanceMigrationBatchCmd(historicCaseInstanceMigrationDocument, caseDefinitionId, configuration));
    }

    @Override
    public Batch batchMigrateCaseInstancesOfCaseDefinition(String caseDefinitionKey, int caseDefinitionVersion, String caseDefinitionTenantId, CaseInstanceMigrationDocument caseInstanceMigrationDocument) {
        return commandExecutor.execute(new CaseInstanceMigrationBatchCmd(caseDefinitionKey, caseDefinitionVersion, 
                caseDefinitionTenantId, caseInstanceMigrationDocument, configuration));
    }
    
    @Override
    public Batch batchMigrateHistoricCaseInstancesOfCaseDefinition(String caseDefinitionKey, int caseDefinitionVersion, String caseDefinitionTenantId, HistoricCaseInstanceMigrationDocument historicCaseInstanceMigrationDocument) {
        return commandExecutor.execute(new HistoricCaseInstanceMigrationBatchCmd(caseDefinitionKey, caseDefinitionVersion, 
                caseDefinitionTenantId, historicCaseInstanceMigrationDocument, configuration));
    }

    @Override
    public CaseInstanceBatchMigrationResult getResultsOfBatchCaseInstanceMigration(String migrationBatchId) {
        return commandExecutor.execute(new GetCaseInstanceMigrationBatchResultCmd(migrationBatchId));
    }
}
