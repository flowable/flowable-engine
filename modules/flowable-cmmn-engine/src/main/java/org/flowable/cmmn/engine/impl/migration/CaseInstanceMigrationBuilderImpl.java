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

import java.util.Map;

import org.flowable.batch.api.Batch;
import org.flowable.cmmn.api.CmmnMigrationService;
import org.flowable.cmmn.api.migration.ActivatePlanItemDefinitionMapping;
import org.flowable.cmmn.api.migration.CaseInstanceMigrationBuilder;
import org.flowable.cmmn.api.migration.CaseInstanceMigrationDocument;
import org.flowable.cmmn.api.migration.CaseInstanceMigrationValidationResult;
import org.flowable.cmmn.api.migration.MoveToAvailablePlanItemDefinitionMapping;
import org.flowable.cmmn.api.migration.TerminatePlanItemDefinitionMapping;
import org.flowable.common.engine.api.FlowableException;

/**
 * @author Valentin Zickner
 */
public class CaseInstanceMigrationBuilderImpl implements CaseInstanceMigrationBuilder {

    protected CmmnMigrationService cmmnMigrationService;
    protected CaseInstanceMigrationDocumentBuilderImpl caseInstanceMigrationDocumentDocumentBuilder = new CaseInstanceMigrationDocumentBuilderImpl();

    public CaseInstanceMigrationBuilderImpl(CmmnMigrationService cmmnMigrationService) {
        this.cmmnMigrationService = cmmnMigrationService;
    }

    @Override
    public CaseInstanceMigrationBuilder fromCaseInstanceMigrationDocument(CaseInstanceMigrationDocument caseInstanceMigrationDocument) {
        this.caseInstanceMigrationDocumentDocumentBuilder.setCaseDefinitionToMigrateTo(caseInstanceMigrationDocument.getMigrateToCaseDefinitionId());
        this.caseInstanceMigrationDocumentDocumentBuilder.setCaseDefinitionToMigrateTo(caseInstanceMigrationDocument.getMigrateToCaseDefinitionKey(), caseInstanceMigrationDocument.getMigrateToCaseDefinitionVersion());
        this.caseInstanceMigrationDocumentDocumentBuilder.setTenantId(caseInstanceMigrationDocument.getMigrateToCaseDefinitionTenantId());
        this.caseInstanceMigrationDocumentDocumentBuilder.addActivatePlanItemDefinitionMappings(caseInstanceMigrationDocument.getActivatePlanItemDefinitionMappings());
        this.caseInstanceMigrationDocumentDocumentBuilder.addTerminatePlanItemDefinitionMappings(caseInstanceMigrationDocument.getTerminatePlanItemDefinitionMappings());
        this.caseInstanceMigrationDocumentDocumentBuilder.addMoveToAvailablePlanItemDefinitionMappings(caseInstanceMigrationDocument.getMoveToAvailablePlanItemDefinitionMappings());
        this.caseInstanceMigrationDocumentDocumentBuilder.addCaseInstanceVariables(caseInstanceMigrationDocument.getCaseInstanceVariables());
        return this;
    }

    @Override
    public CaseInstanceMigrationBuilder migrateToCaseDefinition(String caseDefinitionId) {
        this.caseInstanceMigrationDocumentDocumentBuilder.setCaseDefinitionToMigrateTo(caseDefinitionId);
        return this;
    }

    @Override
    public CaseInstanceMigrationBuilder migrateToCaseDefinition(String caseDefinitionKey, int caseDefinitionVersion) {
        this.caseInstanceMigrationDocumentDocumentBuilder.setCaseDefinitionToMigrateTo(caseDefinitionKey, caseDefinitionVersion);
        return this;
    }

    @Override
    public CaseInstanceMigrationBuilder migrateToCaseDefinition(String caseDefinitionKey, int caseDefinitionVersion, String caseDefinitionTenantId) {
        this.caseInstanceMigrationDocumentDocumentBuilder.setCaseDefinitionToMigrateTo(caseDefinitionKey, caseDefinitionVersion);
        this.caseInstanceMigrationDocumentDocumentBuilder.setTenantId(caseDefinitionTenantId);
        return this;
    }

    @Override
    public CaseInstanceMigrationBuilder withMigrateToCaseDefinitionTenantId(String caseDefinitionTenantId) {
        this.caseInstanceMigrationDocumentDocumentBuilder.setTenantId(caseDefinitionTenantId);
        return this;
    }

    @Override
    public CaseInstanceMigrationBuilder addActivatePlanItemDefinitionMapping(ActivatePlanItemDefinitionMapping mapping) {
        this.caseInstanceMigrationDocumentDocumentBuilder.addActivatePlanItemDefinitionMapping(mapping);
        return this;
    }

    @Override
    public CaseInstanceMigrationBuilder addTerminatePlanItemDefinitionMapping(TerminatePlanItemDefinitionMapping mapping) {
        this.caseInstanceMigrationDocumentDocumentBuilder.addTerminatePlanItemDefinitionMapping(mapping);
        return this;
    }

    @Override
    public CaseInstanceMigrationBuilder addMoveToAvailablePlanItemDefinitionMapping(MoveToAvailablePlanItemDefinitionMapping mapping) {
        this.caseInstanceMigrationDocumentDocumentBuilder.addMoveToAvailablePlanItemDefinitionMapping(mapping);
        return this;
    }

    @Override
    public CaseInstanceMigrationBuilder withCaseInstanceVariable(String variableName, Object variableValue) {
        this.caseInstanceMigrationDocumentDocumentBuilder.addCaseInstanceVariable(variableName, variableValue);
        return this;
    }

    @Override
    public CaseInstanceMigrationBuilder withCaseInstanceVariables(Map<String, Object> variables) {
        this.caseInstanceMigrationDocumentDocumentBuilder.addCaseInstanceVariables(variables);
        return this;
    }

    @Override
    public CaseInstanceMigrationDocument getCaseInstanceMigrationDocument() {
        return this.caseInstanceMigrationDocumentDocumentBuilder.build();
    }

    @Override
    public void migrate(String caseInstanceId) {
        getCmmnMigrationService().migrateCaseInstance(caseInstanceId, getCaseInstanceMigrationDocument());
    }

    @Override
    public CaseInstanceMigrationValidationResult validateMigration(String caseInstanceId) {
        return getCmmnMigrationService().validateMigrationForCaseInstance(caseInstanceId, getCaseInstanceMigrationDocument());
    }

    @Override
    public void migrateCaseInstances(String caseDefinitionId) {
        getCmmnMigrationService().migrateCaseInstancesOfCaseDefinition(caseDefinitionId, getCaseInstanceMigrationDocument());
    }

    @Override
    public Batch batchMigrateCaseInstances(String caseDefinitionId) {
        return getCmmnMigrationService().batchMigrateCaseInstancesOfCaseDefinition(caseDefinitionId, getCaseInstanceMigrationDocument());
    }

    @Override
    public CaseInstanceMigrationValidationResult validateMigrationOfCaseInstances(String caseDefinitionId) {
        return getCmmnMigrationService().validateMigrationForCaseInstancesOfCaseDefinition(caseDefinitionId, getCaseInstanceMigrationDocument());
    }

    @Override
    public void migrateCaseInstances(String caseDefinitionKey, int caseDefinitionVersion, String caseDefinitionTenantId) {
        getCmmnMigrationService().migrateCaseInstancesOfCaseDefinition(caseDefinitionKey, caseDefinitionVersion, caseDefinitionTenantId, getCaseInstanceMigrationDocument());
    }

    @Override
    public Batch batchMigrateCaseInstances(String caseDefinitionKey, int caseDefinitionVersion, String caseDefinitionTenantId) {
        return getCmmnMigrationService().batchMigrateCaseInstancesOfCaseDefinition(caseDefinitionKey, caseDefinitionVersion, caseDefinitionTenantId, getCaseInstanceMigrationDocument());
    }

    @Override
    public CaseInstanceMigrationValidationResult validateMigrationOfCaseInstances(String caseDefinitionKey, int caseDefinitionVersion, String caseDefinitionTenantId) {
        return getCmmnMigrationService().validateMigrationForCaseInstancesOfCaseDefinition(caseDefinitionKey, caseDefinitionVersion, caseDefinitionTenantId, getCaseInstanceMigrationDocument());
    }

    protected CmmnMigrationService getCmmnMigrationService() {
        if (cmmnMigrationService == null) {
            throw new FlowableException("CaseMigrationService cannot be null, Obtain your builder instance from the CaseMigrationService to access this feature");
        }
        return cmmnMigrationService;
    }

}
