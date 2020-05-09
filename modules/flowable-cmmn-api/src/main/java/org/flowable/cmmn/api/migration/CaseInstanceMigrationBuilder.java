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

package org.flowable.cmmn.api.migration;

import java.util.Map;

import org.flowable.batch.api.Batch;

/**
 * @author Valentin Zickner
 */
public interface CaseInstanceMigrationBuilder {

    /**
     * Creates a CaseInstanceMigrationBuilder using the values of a CaseInstanceMigrationDocument
     *
     * @param caseInstanceMigrationDocument Migration document with pre-filled case information
     * @return Returns the builder
     * @see CaseInstanceMigrationDocument
     */
    CaseInstanceMigrationBuilder fromCaseInstanceMigrationDocument(CaseInstanceMigrationDocument caseInstanceMigrationDocument);

    /**
     * Specifies the case definition to migrate to, using the case definition id
     *
     * @param caseDefinitionId ID of the case definition to migrate to
     * @return Returns the builder
     * @see org.flowable.cmmn.api.repository.CaseDefinition
     */
    CaseInstanceMigrationBuilder migrateToCaseDefinition(String caseDefinitionId);

    /**
     * Specifies the case definition to migrate to, identified by its key and version
     *
     * @param caseDefinitionKey Key of the case definition to migrate to
     * @param caseDefinitionVersion Version of the case to migrate to
     * @return Returns the builder
     * @see org.flowable.cmmn.api.repository.CaseDefinition
     */
    CaseInstanceMigrationBuilder migrateToCaseDefinition(String caseDefinitionKey, int caseDefinitionVersion);

    /**
     * Specifies the case definition to migrate to, identified by its key and version and tenantId
     *
     * @param caseDefinitionKey Key of the case definition to migrate to
     * @param caseDefinitionVersion Version of the case to migrate to
     * @param caseDefinitionTenantId Tenant id of the case definition, must be part of the same tenant
     * @return Returns the builder
     * @see org.flowable.cmmn.api.repository.CaseDefinition
     */
    CaseInstanceMigrationBuilder migrateToCaseDefinition(String caseDefinitionKey, int caseDefinitionVersion, String caseDefinitionTenantId);

    /**
     * Specifies the tenantId of the case definition to migrate to
     *
     * @param caseDefinitionTenantId Tenant id of the case definition, must be part of the same tenant
     * @return Returns the builder
     */
    CaseInstanceMigrationBuilder withMigrateToCaseDefinitionTenantId(String caseDefinitionTenantId);

    /**
     * Adds an activate plan item definition mapping to the case instance migration plan.
     *
     * @param mapping Mapping for a specific plan item definition
     * @return Returns the builder
     * @see ActivatePlanItemDefinitionMapping
     */
    CaseInstanceMigrationBuilder addActivatePlanItemDefinitionMapping(ActivatePlanItemDefinitionMapping mapping);
    
    /**
     * Adds a terminate plan item definition mapping to the case instance migration plan.
     *
     * @param mapping Mapping for a specific plan item definition
     * @return Returns the builder
     * @see TerminatePlanItemDefinitionMapping
     */
    CaseInstanceMigrationBuilder addTerminatePlanItemDefinitionMapping(TerminatePlanItemDefinitionMapping mapping);
    
    /**
     * Adds a move to available plan item definition mapping to the case instance migration plan.
     *
     * @param mapping Mapping for a specific plan item definition
     * @return Returns the builder
     * @see MoveToAvailablePlanItemDefinitionMapping
     */
    CaseInstanceMigrationBuilder addMoveToAvailablePlanItemDefinitionMapping(MoveToAvailablePlanItemDefinitionMapping mapping);

    /**
     * Specifies a case instance variable that will also be available during the case migration
     *
     * @param variableName Name of the variable
     * @param variableValue Value of the variable
     * @return Returns the builder
     */
    CaseInstanceMigrationBuilder withCaseInstanceVariable(String variableName, Object variableValue);

    /**
     * Specified case instances variables that will also be available during case migration
     *
     * @param variables
     * @return Returns the builder
     */
    CaseInstanceMigrationBuilder withCaseInstanceVariables(Map<String, Object> variables);

    /**
     * Builds a CaseInstanceMigrationDocument
     *
     * @return Returns the builder
     * @see CaseInstanceMigrationDocument
     */
    CaseInstanceMigrationDocument getCaseInstanceMigrationDocument();

    /**
     * Starts the case instance migration for a case identified with the submitted caseInstanceId
     *
     * @param caseInstanceId
     */
    void migrate(String caseInstanceId);

    /**
     * Validates this case instance migration instructions for a given case instance identified by its caseInstanceId
     *
     * @param caseInstanceId
     * @return a CaseInstanceMigrationValidationResult that contains validation error messages - if any
     */
    CaseInstanceMigrationValidationResult validateMigration(String caseInstanceId);

    /**
     * Asynchronously starts the case instance migration for each case instances of a given case definition identified by the case definition id.
     *
     * @param caseDefinitionId
     */
    void migrateCaseInstances(String caseDefinitionId);

    /**
     * Starts the case instance migration for all case instances of a given case definition identified by the case definition id.
     *
     * @param caseDefinitionId
     */
    Batch batchMigrateCaseInstances(String caseDefinitionId);

    /**
     * Validates this case instance migration instruction for each case instance of a given case definition identified by the case definition id.
     *
     * @param caseDefinitionId
     * @return a CaseInstanceMigrationValidationResult that contains validation error messages - if any
     * @see CaseInstanceMigrationValidationResult
     */
    CaseInstanceMigrationValidationResult validateMigrationOfCaseInstances(String caseDefinitionId);

    /**
     * Starts the case instance migration for all case instances of a given case definition identified by the case definition key and version (optional tenantId).
     *
     * @param caseDefinitionKey
     * @param caseDefinitionVersion
     * @param caseDefinitionTenantId
     */
    void migrateCaseInstances(String caseDefinitionKey, int caseDefinitionVersion, String caseDefinitionTenantId);

    /**
     * Asynchronously starts the case instance migration for each case instances of a given case definition identified by the case definition key and version (optional tenantId).
     *
     * @param caseDefinitionKey
     * @param caseDefinitionVersion
     * @param caseDefinitionTenantId
     * @return an id of the created batch entity
     */
    Batch batchMigrateCaseInstances(String caseDefinitionKey, int caseDefinitionVersion, String caseDefinitionTenantId);

    /**
     * Validates this case instance migration instruction for each case instance of a given case definition identified by the case definition key and version (optional tenantId).
     *
     * @param caseDefinitionKey
     * @param caseDefinitionVersion
     * @param caseDefinitionTenantId
     * @return a CaseInstanceMigrationValidationResult that contains validation error messages - if any
     * @see CaseInstanceMigrationValidationResult
     */
    CaseInstanceMigrationValidationResult validateMigrationOfCaseInstances(String caseDefinitionKey, int caseDefinitionVersion, String caseDefinitionTenantId);

}
