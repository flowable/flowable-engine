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

package org.flowable.engine.impl.migration;

import java.util.Map;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.migration.ProcessInstanceMigrationBuilder;
import org.flowable.engine.migration.ProcessInstanceMigrationDocument;

/**
 * @author Dennis Federico
 */
public class ProcessInstanceMigrationBuilderImpl implements ProcessInstanceMigrationBuilder {

    protected RuntimeService runtimeService;
    protected ProcessInstanceMigrationDocumentBuilderImpl migrationDocumentBuilder = new ProcessInstanceMigrationDocumentBuilderImpl();

    public ProcessInstanceMigrationBuilderImpl(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @Override
    public ProcessInstanceMigrationBuilder fromProcessInstanceMigrationDocument(ProcessInstanceMigrationDocument document) {
        document.getMigrateToProcessDefinitionId().ifPresent(v -> migrationDocumentBuilder.migrateToProcessDefinitionId = v);
        document.getMigrateToProcessDefinitionKey().ifPresent(v -> migrationDocumentBuilder.migrateToProcessDefinitionKey = v);
        document.getMigrateToProcessDefinitionVersion().ifPresent(v -> migrationDocumentBuilder.migrateToProcessDefinitionVersion = v);
        document.getMigrateToProcessDefinitionTenantId().ifPresent(v -> migrationDocumentBuilder.migrateToProcessDefinitionTenantId = v);
        migrationDocumentBuilder.addActivityMigrationMappings(document.getActivityMigrationMappings());
        return this;
    }

    @Override
    public ProcessInstanceMigrationBuilder migrateToProcessDefinition(String processDefinitionId) {
        this.migrationDocumentBuilder.setProcessDefinitionToMigrateTo(processDefinitionId);
        return this;
    }

    @Override
    public ProcessInstanceMigrationBuilder migrateToProcessDefinition(String processDefinitionKey, String processDefinitionVersion) {
        this.migrationDocumentBuilder.setProcessDefinitionToMigrateTo(processDefinitionKey, processDefinitionVersion);
        return this;
    }

    @Override
    public ProcessInstanceMigrationBuilder migrateToProcessDefinition(String processDefinitionKey, String processDefinitionVersion, String processDefinitionTenantId) {
        this.migrationDocumentBuilder.setProcessDefinitionToMigrateTo(processDefinitionKey, processDefinitionVersion, processDefinitionTenantId);
        return this;
    }

    @Override
    public ProcessInstanceMigrationBuilder withMigrateToProcessDefinitionTenantId(String processDefinitionTenantId) {
        this.migrationDocumentBuilder.setTenantOfProcessDefinitionToMigrateTo(processDefinitionTenantId);
        return this;
    }

    @Override
    public ProcessInstanceMigrationBuilder addActivityMigrationMapping(String fromActivityId, String toActivityId) {
        this.migrationDocumentBuilder.addActivityMigrationMapping(fromActivityId, toActivityId);
        return this;
    }

    @Override
    public ProcessInstanceMigrationBuilder addActivityMigrationMappings(Map<String, String> activityMigrationMappings) {
        this.migrationDocumentBuilder.addActivityMigrationMappings(activityMigrationMappings);
        return this;
    }

    @Override
    public ProcessInstanceMigrationDocument getProcessInstanceMigrationDocument() {
        return this.migrationDocumentBuilder.build();
    }

    @Override
    public void migrate(String processInstanceId) {
        ProcessInstanceMigrationDocument document = migrationDocumentBuilder.build();
        runtimeService.migrateProcessInstance(processInstanceId, document);
    }

    @Override
    public void migrateProcessInstancesOf(String processDefinitionId) {
        ProcessInstanceMigrationDocument document = migrationDocumentBuilder.build();
        runtimeService.migrateProcessInstancesOfProcessDefinition(processDefinitionId, document);
    }

    @Override
    public void migrateProcessInstancesOf(String processDefinitionKey, String processDefinitionVersion, String processDefinitionTenantId) {
        ProcessInstanceMigrationDocument document = migrationDocumentBuilder.build();
        runtimeService.migrateProcessInstancesOfProcessDefinition(processDefinitionKey, processDefinitionVersion, processDefinitionTenantId, document);
    }

}
