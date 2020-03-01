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

import org.flowable.cmmn.api.migration.CaseInstanceMigrationDocument;
import org.flowable.cmmn.api.migration.CaseInstanceMigrationDocumentBuilder;
import org.flowable.cmmn.api.migration.PlanItemMigrationMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Valentin Zickner
 */
public class CaseInstanceMigrationDocumentBuilderImpl implements CaseInstanceMigrationDocumentBuilder {

    protected String migrateToCaseDefinitionId;
    protected String migrateToCaseDefinitionKey;
    protected Integer migrateToCaseDefinitionVersion;
    protected String migrateToCaseDefinitionTenantId;
    protected List<PlanItemMigrationMapping> planItemMigrationMappings = new ArrayList<>();
    protected Map<String, Object> caseInstanceVariables = new HashMap<>();

    @Override
    public CaseInstanceMigrationDocumentBuilder setCaseDefinitionToMigrateTo(String caseDefinitionId) {
        this.migrateToCaseDefinitionId = caseDefinitionId;
        return this;
    }

    @Override
    public CaseInstanceMigrationDocumentBuilder setCaseDefinitionToMigrateTo(String caseDefinitionKey, Integer caseDefinitionVersion) {
        this.migrateToCaseDefinitionKey = caseDefinitionKey;
        this.migrateToCaseDefinitionVersion = caseDefinitionVersion;
        return this;
    }

    @Override
    public CaseInstanceMigrationDocumentBuilder setTenantId(String caseDefinitionTenantId) {
        this.migrateToCaseDefinitionTenantId = caseDefinitionTenantId;
        return this;
    }

    @Override
    public CaseInstanceMigrationDocumentBuilder addPlanItemMigrationMappings(List<PlanItemMigrationMapping> planItemMigrationMappings) {
        this.planItemMigrationMappings.addAll(planItemMigrationMappings);
        return this;
    }

    @Override
    public CaseInstanceMigrationDocumentBuilder addPlanItemMigrationMapping(PlanItemMigrationMapping planItemMigrationMapping) {
        this.planItemMigrationMappings.add(planItemMigrationMapping);
        return this;
    }

    @Override
    public CaseInstanceMigrationDocumentBuilder addCaseInstanceVariable(String variableName, Object variableValue) {
        this.caseInstanceVariables.put(variableName, variableValue);
        return this;
    }

    @Override
    public CaseInstanceMigrationDocumentBuilder addCaseInstanceVariables(Map<String, Object> caseInstanceVariables) {
        this.caseInstanceVariables.putAll(caseInstanceVariables);
        return this;
    }

    @Override
    public CaseInstanceMigrationDocument build() {
        CaseInstanceMigrationDocumentImpl caseInstanceMigrationDocument = new CaseInstanceMigrationDocumentImpl();
        caseInstanceMigrationDocument.setMigrateToCaseDefinitionId(this.migrateToCaseDefinitionId);
        caseInstanceMigrationDocument.setMigrateToCaseDefinition(this.migrateToCaseDefinitionKey, this.migrateToCaseDefinitionVersion, this.migrateToCaseDefinitionTenantId);
        caseInstanceMigrationDocument.setPlanItemMigrationMappings(this.planItemMigrationMappings);
        caseInstanceMigrationDocument.setCaseInstanceVariables(this.caseInstanceVariables);
        return caseInstanceMigrationDocument;
    }
}
