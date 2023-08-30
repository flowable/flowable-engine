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

import org.flowable.batch.api.Batch;

public interface HistoricCaseInstanceMigrationBuilder {

    /**
     * Creates a HistoricCaseInstanceMigrationBuilder using the values of a HistoricCaseInstanceMigrationDocument
     *
     * @param historicCaseInstanceMigrationDocument Migration document with pre-filled case information
     * @return Returns the builder
     * @see HistoricCaseInstanceMigrationDocument
     */
    HistoricCaseInstanceMigrationBuilder fromHistoricCaseInstanceMigrationDocument(HistoricCaseInstanceMigrationDocument historicCaseInstanceMigrationDocument);

    /**
     * Specifies the case definition to migrate to, using the case definition id
     *
     * @param caseDefinitionId ID of the case definition to migrate to
     * @return Returns the builder
     * @see org.flowable.cmmn.api.repository.CaseDefinition
     */
    HistoricCaseInstanceMigrationBuilder migrateToCaseDefinition(String caseDefinitionId);

    /**
     * Specifies the case definition to migrate to, identified by its key and version
     *
     * @param caseDefinitionKey Key of the case definition to migrate to
     * @param caseDefinitionVersion Version of the case to migrate to
     * @return Returns the builder
     * @see org.flowable.cmmn.api.repository.CaseDefinition
     */
    HistoricCaseInstanceMigrationBuilder migrateToCaseDefinition(String caseDefinitionKey, int caseDefinitionVersion);

    /**
     * Specifies the case definition to migrate to, identified by its key and version and tenantId
     *
     * @param caseDefinitionKey Key of the case definition to migrate to
     * @param caseDefinitionVersion Version of the case to migrate to
     * @param caseDefinitionTenantId Tenant id of the case definition, must be part of the same tenant
     * @return Returns the builder
     * @see org.flowable.cmmn.api.repository.CaseDefinition
     */
    HistoricCaseInstanceMigrationBuilder migrateToCaseDefinition(String caseDefinitionKey, int caseDefinitionVersion, String caseDefinitionTenantId);

    /**
     * Specifies the tenantId of the case definition to migrate to
     *
     * @param caseDefinitionTenantId Tenant id of the case definition, must be part of the same tenant
     * @return Returns the builder
     */
    HistoricCaseInstanceMigrationBuilder withMigrateToCaseDefinitionTenantId(String caseDefinitionTenantId);

    /**
     * Builds a HistoricCaseInstanceMigrationDocument
     *
     * @return Returns the builder
     * @see HistoricCaseInstanceMigrationDocument
     */
    HistoricCaseInstanceMigrationDocument getHistoricCaseInstanceMigrationDocument();

    /**
     * Starts the case instance migration for a case identified with the submitted caseInstanceId
     *
     * @param caseInstanceId
     */
    void migrate(String caseInstanceId);

    /**
     * Asynchronously starts the case instance migration for each case instances of a given case definition identified by the case definition id.
     *
     * @param caseDefinitionId
     */
    void migrateHistoricCaseInstances(String caseDefinitionId);

    /**
     * Starts the case instance migration for all case instances of a given case definition identified by the case definition id.
     *
     * @param caseDefinitionId
     */
    Batch batchMigrateHistoricCaseInstances(String caseDefinitionId);

    /**
     * Starts the case instance migration for all case instances of a given case definition identified by the case definition key and version (optional tenantId).
     *
     * @param caseDefinitionKey
     * @param caseDefinitionVersion
     * @param caseDefinitionTenantId
     */
    void migrateHistoricCaseInstances(String caseDefinitionKey, int caseDefinitionVersion, String caseDefinitionTenantId);

    /**
     * Asynchronously starts the case instance migration for each case instances of a given case definition identified by the case definition key and version (optional tenantId).
     *
     * @param caseDefinitionKey
     * @param caseDefinitionVersion
     * @param caseDefinitionTenantId
     * @return an id of the created batch entity
     */
    Batch batchMigrateHistoricCaseInstances(String caseDefinitionKey, int caseDefinitionVersion, String caseDefinitionTenantId);

}
