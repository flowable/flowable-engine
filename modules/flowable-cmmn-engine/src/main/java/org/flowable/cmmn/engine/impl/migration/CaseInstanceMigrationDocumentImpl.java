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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.api.migration.ActivatePlanItemDefinitionMapping;
import org.flowable.cmmn.api.migration.CaseInstanceMigrationDocument;
import org.flowable.cmmn.api.migration.MoveToAvailablePlanItemDefinitionMapping;
import org.flowable.cmmn.api.migration.TerminatePlanItemDefinitionMapping;

/**
 * @author Valentin Zickner
 */
public class CaseInstanceMigrationDocumentImpl implements CaseInstanceMigrationDocument {

    protected String migrateToCaseDefinitionId;
    protected String migrateToCaseDefinitionKey;
    protected Integer migrateToCaseDefinitionVersion;
    protected String migrateToCaseDefinitionTenantId;
    protected List<ActivatePlanItemDefinitionMapping> activatePlanItemDefinitionMappings = new ArrayList<>();
    protected List<TerminatePlanItemDefinitionMapping> terminatePlanItemDefinitionMappings = new ArrayList<>();
    protected List<MoveToAvailablePlanItemDefinitionMapping> moveToAvailablePlanItemDefinitionMappings = new ArrayList<>();
    protected Map<String, Object> caseInstanceVariables = new HashMap<>();
    protected Map<String, Map<String, Object>> planItemLocalVariables = new HashMap<>();

    public static CaseInstanceMigrationDocument fromJson(String caseInstanceMigrationDocumentJson) {
        return CaseInstanceMigrationDocumentConverter.convertFromJson(caseInstanceMigrationDocumentJson);
    }

    public void setMigrateToCaseDefinitionId(String caseDefinitionId) {
        this.migrateToCaseDefinitionId = caseDefinitionId;
    }

    public void setMigrateToCaseDefinition(String caseDefinitionKey, Integer caseDefinitionVersion) {
        this.migrateToCaseDefinitionKey = caseDefinitionKey;
        this.migrateToCaseDefinitionVersion = caseDefinitionVersion;
    }

    public void setMigrateToCaseDefinition(String caseDefinitionKey, Integer caseDefinitionVersion, String caseDefinitionTenantId) {
        this.migrateToCaseDefinitionKey = caseDefinitionKey;
        this.migrateToCaseDefinitionVersion = caseDefinitionVersion;
        this.migrateToCaseDefinitionTenantId = caseDefinitionTenantId;
    }

    public void setActivatePlanItemDefinitionMappings(List<ActivatePlanItemDefinitionMapping> planItemDefinitionMappings) {
        this.activatePlanItemDefinitionMappings = planItemDefinitionMappings;
    }
    
    public void setTerminatePlanItemDefinitionMappings(List<TerminatePlanItemDefinitionMapping> planItemDefinitionMappings) {
        this.terminatePlanItemDefinitionMappings = planItemDefinitionMappings;
    }
    
    public void setMoveToAvailablePlanItemDefinitionMappings(List<MoveToAvailablePlanItemDefinitionMapping> planItemDefinitionMappings) {
        this.moveToAvailablePlanItemDefinitionMappings = planItemDefinitionMappings;
    }

    public void setCaseInstanceVariables(Map<String, Object> caseInstanceVariables) {
        this.caseInstanceVariables = caseInstanceVariables;
    }

    @Override
    public String getMigrateToCaseDefinitionId() {
        return this.migrateToCaseDefinitionId;
    }

    @Override
    public String getMigrateToCaseDefinitionKey() {
        return this.migrateToCaseDefinitionKey;
    }

    @Override
    public Integer getMigrateToCaseDefinitionVersion() {
        return this.migrateToCaseDefinitionVersion;
    }

    @Override
    public String getMigrateToCaseDefinitionTenantId() {
        return this.migrateToCaseDefinitionTenantId;
    }
    
    @Override
    public List<ActivatePlanItemDefinitionMapping> getActivatePlanItemDefinitionMappings() {
        return activatePlanItemDefinitionMappings;
    }

    @Override
    public List<TerminatePlanItemDefinitionMapping> getTerminatePlanItemDefinitionMappings() {
        return terminatePlanItemDefinitionMappings;
    }

    @Override
    public List<MoveToAvailablePlanItemDefinitionMapping> getMoveToAvailablePlanItemDefinitionMappings() {
        return moveToAvailablePlanItemDefinitionMappings;
    }

    @Override
    public Map<String, Map<String, Object>> getPlanItemLocalVariables() {
        return this.planItemLocalVariables;
    }

    @Override
    public Map<String, Object> getCaseInstanceVariables() {
        return this.caseInstanceVariables;
    }

    @Override
    public String asJsonString() {
        return CaseInstanceMigrationDocumentConverter.convertToJsonString(this);
    }
}
