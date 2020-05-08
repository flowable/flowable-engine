package org.flowable.cmmn.engine.impl.migration;

import org.flowable.batch.api.Batch;
import org.flowable.cmmn.api.migration.CaseInstanceMigrationDocument;
import org.flowable.cmmn.api.migration.CaseInstanceMigrationValidationResult;
import org.flowable.common.engine.impl.interceptor.CommandContext;

public interface CaseInstanceMigrationManager {

    CaseInstanceMigrationValidationResult validateMigrateCaseInstancesOfCaseDefinition(String caseDefinitionKey, int caseDefinitionVersion, String caseDefinitionTenantId, CaseInstanceMigrationDocument document, CommandContext commandContext);

    CaseInstanceMigrationValidationResult validateMigrateCaseInstancesOfCaseDefinition(String caseDefinitionId, CaseInstanceMigrationDocument document, CommandContext commandContext);

    CaseInstanceMigrationValidationResult validateMigrateCaseInstance(String caseInstanceId, CaseInstanceMigrationDocument document, CommandContext commandContext);

    void migrateCaseInstance(String caseInstanceId, CaseInstanceMigrationDocument document, CommandContext commandContext);

    void migrateCaseInstancesOfCaseDefinition(String caseDefinitionKey, int caseDefinitionVersion, String caseDefinitionTenantId, CaseInstanceMigrationDocument document, CommandContext commandContext);

    void migrateCaseInstancesOfCaseDefinition(String caseDefinitionId, CaseInstanceMigrationDocument document, CommandContext commandContext);

    Batch batchMigrateCaseInstancesOfCaseDefinition(String caseDefinitionKey, int caseDefinitionVersion, String caseDefinitionTenantId, CaseInstanceMigrationDocument document, CommandContext commandContext);

    Batch batchMigrateCaseInstancesOfCaseDefinition(String caseDefinitionId, CaseInstanceMigrationDocument document, CommandContext commandContext);
}
