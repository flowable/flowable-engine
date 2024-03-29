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

package org.flowable.cmmn.api;

import org.flowable.batch.api.Batch;
import org.flowable.cmmn.api.migration.CaseInstanceBatchMigrationResult;
import org.flowable.cmmn.api.migration.CaseInstanceMigrationBuilder;
import org.flowable.cmmn.api.migration.CaseInstanceMigrationDocument;
import org.flowable.cmmn.api.migration.CaseInstanceMigrationValidationResult;
import org.flowable.cmmn.api.migration.HistoricCaseInstanceMigrationBuilder;
import org.flowable.cmmn.api.migration.HistoricCaseInstanceMigrationDocument;

/**
 * Service to manager case instance migrations.
 *
 * @author Valentin Zickner
 */
public interface CmmnMigrationService {

    CaseInstanceMigrationBuilder createCaseInstanceMigrationBuilder();

    CaseInstanceMigrationBuilder createCaseInstanceMigrationBuilderFromCaseInstanceMigrationDocument(CaseInstanceMigrationDocument document);
    
    HistoricCaseInstanceMigrationBuilder createHistoricCaseInstanceMigrationBuilder();

    HistoricCaseInstanceMigrationBuilder createHistoricCaseInstanceMigrationBuilderFromHistoricCaseInstanceMigrationDocument(HistoricCaseInstanceMigrationDocument document);

    CaseInstanceMigrationValidationResult validateMigrationForCaseInstance(String caseInstanceId, CaseInstanceMigrationDocument caseInstanceMigrationDocument);

    CaseInstanceMigrationValidationResult validateMigrationForCaseInstancesOfCaseDefinition(String caseDefinitionId, CaseInstanceMigrationDocument caseInstanceMigrationDocument);

    CaseInstanceMigrationValidationResult validateMigrationForCaseInstancesOfCaseDefinition(String caseDefinitionKey, int caseDefinitionVersion, String caseDefinitionTenantId, CaseInstanceMigrationDocument caseInstanceMigrationDocument);

    void migrateCaseInstance(String caseInstanceId, CaseInstanceMigrationDocument caseInstanceMigrationDocument);
    
    void migrateHistoricCaseInstance(String caseInstanceId, HistoricCaseInstanceMigrationDocument historicCaseInstanceMigrationDocument);

    void migrateCaseInstancesOfCaseDefinition(String caseDefinitionId, CaseInstanceMigrationDocument caseInstanceMigrationDocument);
    
    void migrateHistoricCaseInstancesOfCaseDefinition(String caseDefinitionId, HistoricCaseInstanceMigrationDocument historicCaseInstanceMigrationDocument);

    void migrateCaseInstancesOfCaseDefinition(String caseDefinitionKey, int caseDefinitionVersion, String caseDefinitionTenantId, CaseInstanceMigrationDocument caseInstanceMigrationDocument);
    
    void migrateHistoricCaseInstancesOfCaseDefinition(String caseDefinitionKey, int caseDefinitionVersion, String caseDefinitionTenantId, HistoricCaseInstanceMigrationDocument historicCaseInstanceMigrationDocument);

    Batch batchMigrateCaseInstancesOfCaseDefinition(String caseDefinitionId, CaseInstanceMigrationDocument caseInstanceMigrationDocument);
    
    Batch batchMigrateHistoricCaseInstancesOfCaseDefinition(String caseDefinitionId, HistoricCaseInstanceMigrationDocument historicCaseInstanceMigrationDocument);

    Batch batchMigrateCaseInstancesOfCaseDefinition(String caseDefinitionKey, int caseDefinitionVersion, String caseDefinitionTenantId, CaseInstanceMigrationDocument caseInstanceMigrationDocument);
    
    Batch batchMigrateHistoricCaseInstancesOfCaseDefinition(String caseDefinitionKey, int caseDefinitionVersion, String caseDefinitionTenantId, HistoricCaseInstanceMigrationDocument historicCaseInstanceMigrationDocument);

    CaseInstanceBatchMigrationResult getResultsOfBatchCaseInstanceMigration(String migrationBatchId);

}
