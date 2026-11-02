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
import org.flowable.cmmn.api.migration.HistoricCaseInstanceMigrationBuilder;
import org.flowable.cmmn.api.migration.HistoricCaseInstanceMigrationDocument;
import org.flowable.common.engine.api.FlowableException;

public class HistoricCaseInstanceMigrationBuilderImpl implements HistoricCaseInstanceMigrationBuilder {

    protected CmmnMigrationService cmmnMigrationService;
    protected HistoricCaseInstanceMigrationDocumentBuilderImpl historicCaseInstanceMigrationDocumentDocumentBuilder = new HistoricCaseInstanceMigrationDocumentBuilderImpl();

    public HistoricCaseInstanceMigrationBuilderImpl(CmmnMigrationService cmmnMigrationService) {
        this.cmmnMigrationService = cmmnMigrationService;
    }

    @Override
    public HistoricCaseInstanceMigrationBuilder fromHistoricCaseInstanceMigrationDocument(HistoricCaseInstanceMigrationDocument caseInstanceMigrationDocument) {
        this.historicCaseInstanceMigrationDocumentDocumentBuilder.setCaseDefinitionToMigrateTo(caseInstanceMigrationDocument.getMigrateToCaseDefinitionId());
        this.historicCaseInstanceMigrationDocumentDocumentBuilder.setCaseDefinitionToMigrateTo(caseInstanceMigrationDocument.getMigrateToCaseDefinitionKey(), caseInstanceMigrationDocument.getMigrateToCaseDefinitionVersion());
        this.historicCaseInstanceMigrationDocumentDocumentBuilder.setTenantId(caseInstanceMigrationDocument.getMigrateToCaseDefinitionTenantId());
        return this;
    }

    @Override
    public HistoricCaseInstanceMigrationBuilder migrateToCaseDefinition(String caseDefinitionId) {
        this.historicCaseInstanceMigrationDocumentDocumentBuilder.setCaseDefinitionToMigrateTo(caseDefinitionId);
        return this;
    }

    @Override
    public HistoricCaseInstanceMigrationBuilder migrateToCaseDefinition(String caseDefinitionKey, int caseDefinitionVersion) {
        this.historicCaseInstanceMigrationDocumentDocumentBuilder.setCaseDefinitionToMigrateTo(caseDefinitionKey, caseDefinitionVersion);
        return this;
    }

    @Override
    public HistoricCaseInstanceMigrationBuilder migrateToCaseDefinition(String caseDefinitionKey, int caseDefinitionVersion, String caseDefinitionTenantId) {
        this.historicCaseInstanceMigrationDocumentDocumentBuilder.setCaseDefinitionToMigrateTo(caseDefinitionKey, caseDefinitionVersion);
        this.historicCaseInstanceMigrationDocumentDocumentBuilder.setTenantId(caseDefinitionTenantId);
        return this;
    }

    @Override
    public HistoricCaseInstanceMigrationBuilder withMigrateToCaseDefinitionTenantId(String caseDefinitionTenantId) {
        this.historicCaseInstanceMigrationDocumentDocumentBuilder.setTenantId(caseDefinitionTenantId);
        return this;
    }

    @Override
    public HistoricCaseInstanceMigrationDocument getHistoricCaseInstanceMigrationDocument() {
        return this.historicCaseInstanceMigrationDocumentDocumentBuilder.build();
    }

    @Override
    public void migrate(String caseInstanceId) {
        getCmmnMigrationService().migrateHistoricCaseInstance(caseInstanceId, getHistoricCaseInstanceMigrationDocument());
    }

    @Override
    public void migrateHistoricCaseInstances(String caseDefinitionId) {
        getCmmnMigrationService().migrateHistoricCaseInstancesOfCaseDefinition(caseDefinitionId, getHistoricCaseInstanceMigrationDocument());
    }

    @Override
    public Batch batchMigrateHistoricCaseInstances(String caseDefinitionId) {
        return getCmmnMigrationService().batchMigrateHistoricCaseInstancesOfCaseDefinition(caseDefinitionId, getHistoricCaseInstanceMigrationDocument());
    }

    @Override
    public void migrateHistoricCaseInstances(String caseDefinitionKey, int caseDefinitionVersion, String caseDefinitionTenantId) {
        getCmmnMigrationService().migrateHistoricCaseInstancesOfCaseDefinition(caseDefinitionKey, caseDefinitionVersion, caseDefinitionTenantId, getHistoricCaseInstanceMigrationDocument());
    }

    @Override
    public Batch batchMigrateHistoricCaseInstances(String caseDefinitionKey, int caseDefinitionVersion, String caseDefinitionTenantId) {
        return getCmmnMigrationService().batchMigrateHistoricCaseInstancesOfCaseDefinition(caseDefinitionKey, caseDefinitionVersion, caseDefinitionTenantId, getHistoricCaseInstanceMigrationDocument());
    }

    protected CmmnMigrationService getCmmnMigrationService() {
        if (cmmnMigrationService == null) {
            throw new FlowableException("CaseMigrationService cannot be null, Obtain your builder instance from the CaseMigrationService to access this feature");
        }
        return cmmnMigrationService;
    }

}
