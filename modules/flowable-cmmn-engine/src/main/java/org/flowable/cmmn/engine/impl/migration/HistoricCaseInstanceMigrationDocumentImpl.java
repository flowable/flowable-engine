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

import org.flowable.cmmn.api.migration.HistoricCaseInstanceMigrationDocument;

public class HistoricCaseInstanceMigrationDocumentImpl implements HistoricCaseInstanceMigrationDocument {

    protected String migrateToCaseDefinitionId;
    protected String migrateToCaseDefinitionKey;
    protected Integer migrateToCaseDefinitionVersion;
    protected String migrateToCaseDefinitionTenantId;

    public static HistoricCaseInstanceMigrationDocument fromJson(String caseInstanceMigrationDocumentJson) {
        return HistoricCaseInstanceMigrationDocumentConverter.convertFromJson(caseInstanceMigrationDocumentJson);
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
    public String asJsonString() {
        return HistoricCaseInstanceMigrationDocumentConverter.convertToJsonString(this);
    }
}
