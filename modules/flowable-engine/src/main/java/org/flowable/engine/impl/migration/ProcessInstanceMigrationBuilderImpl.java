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

import org.flowable.batch.api.Batch;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.ProcessMigrationService;
import org.flowable.engine.migration.ActivityMigrationMapping;
import org.flowable.engine.migration.ProcessInstanceMigrationBuilder;
import org.flowable.engine.migration.ProcessInstanceMigrationDocument;
import org.flowable.engine.migration.ProcessInstanceMigrationValidationResult;
import org.flowable.engine.migration.Script;

public class ProcessInstanceMigrationBuilderImpl implements ProcessInstanceMigrationBuilder {

    protected ProcessMigrationService processInstanceMigrationService;
    protected ProcessInstanceMigrationDocumentBuilderImpl migrationDocumentBuilder = new ProcessInstanceMigrationDocumentBuilderImpl();

    public ProcessInstanceMigrationBuilderImpl(ProcessMigrationService processInstanceMigrationService) {
        this.processInstanceMigrationService = processInstanceMigrationService;
    }

    @Override
    public ProcessInstanceMigrationBuilder fromProcessInstanceMigrationDocument(ProcessInstanceMigrationDocument document) {
        migrationDocumentBuilder.setProcessDefinitionToMigrateTo(document.getMigrateToProcessDefinitionId());
        migrationDocumentBuilder.setProcessDefinitionToMigrateTo(document.getMigrateToProcessDefinitionKey(), document.getMigrateToProcessDefinitionVersion());
        migrationDocumentBuilder.setTenantId(document.getMigrateToProcessDefinitionTenantId());
        migrationDocumentBuilder.addActivityMigrationMappings(document.getActivityMigrationMappings());
        migrationDocumentBuilder.setPreUpgradeScript(document.getPreUpgradeScript());
        migrationDocumentBuilder.setPreUpgradeJavaDelegate(document.getPreUpgradeJavaDelegate());
        migrationDocumentBuilder.setPreUpgradeJavaDelegateExpression(document.getPreUpgradeJavaDelegateExpression());
        migrationDocumentBuilder.setPostUpgradeScript(document.getPostUpgradeScript());
        migrationDocumentBuilder.setPostUpgradeJavaDelegate(document.getPostUpgradeJavaDelegate());
        migrationDocumentBuilder.setPostUpgradeJavaDelegateExpression(document.getPostUpgradeJavaDelegateExpression());
        return this;
    }

    @Override
    public ProcessInstanceMigrationBuilder migrateToProcessDefinition(String processDefinitionId) {
        this.migrationDocumentBuilder.setProcessDefinitionToMigrateTo(processDefinitionId);
        return this;
    }

    @Override
    public ProcessInstanceMigrationBuilder migrateToProcessDefinition(String processDefinitionKey, int processDefinitionVersion) {
        this.migrationDocumentBuilder.setProcessDefinitionToMigrateTo(processDefinitionKey, processDefinitionVersion);
        return this;
    }

    @Override
    public ProcessInstanceMigrationBuilder migrateToProcessDefinition(String processDefinitionKey, int processDefinitionVersion, String processDefinitionTenantId) {
        this.migrationDocumentBuilder.setProcessDefinitionToMigrateTo(processDefinitionKey, processDefinitionVersion);
        this.migrationDocumentBuilder.setTenantId(processDefinitionTenantId);
        return this;
    }

    @Override
    public ProcessInstanceMigrationBuilder withMigrateToProcessDefinitionTenantId(String processDefinitionTenantId) {
        this.migrationDocumentBuilder.setTenantId(processDefinitionTenantId);
        return this;
    }

    @Override
    public ProcessInstanceMigrationBuilder preUpgradeScript(Script script) {
        this.migrationDocumentBuilder.setPreUpgradeScript(script);
        return this;
    }

    @Override
    public ProcessInstanceMigrationBuilder preUpgradeJavaDelegate(String javaDelegateClassName) {
        this.migrationDocumentBuilder.setPreUpgradeJavaDelegate(javaDelegateClassName);
        return this;
    }

    @Override
    public ProcessInstanceMigrationBuilder preUpgradeJavaDelegateExpression(String expression) {
        this.migrationDocumentBuilder.setPreUpgradeJavaDelegateExpression(expression);
        return this;
    }

    @Override
    public ProcessInstanceMigrationBuilder postUpgradeScript(Script script) {
        this.migrationDocumentBuilder.setPostUpgradeScript(script);
        return this;
    }

    @Override
    public ProcessInstanceMigrationBuilder postUpgradeJavaDelegate(String javaDelegateClassName) {
        this.migrationDocumentBuilder.setPostUpgradeJavaDelegate(javaDelegateClassName);
        return this;
    }

    @Override
    public ProcessInstanceMigrationBuilder postUpgradeJavaDelegateExpression(String expression) {
        this.migrationDocumentBuilder.setPostUpgradeJavaDelegateExpression(expression);
        return this;
    }

    @Override
    public ProcessInstanceMigrationBuilder addActivityMigrationMapping(ActivityMigrationMapping mapping) {
        this.migrationDocumentBuilder.addActivityMigrationMapping(mapping);
        return this;
    }

    @Override
    public ProcessInstanceMigrationBuilder withProcessInstanceVariable(String variableName, Object variableValue) {
        this.migrationDocumentBuilder.processInstanceVariables.put(variableName, variableValue);
        return this;
    }

    @Override
    public ProcessInstanceMigrationBuilder withProcessInstanceVariables(Map<String, Object> variables) {
        this.migrationDocumentBuilder.processInstanceVariables.putAll(variables);
        return this;
    }

    @Override
    public ProcessInstanceMigrationDocument getProcessInstanceMigrationDocument() {
        return this.migrationDocumentBuilder.build();
    }

    @Override
    public void migrate(String processInstanceId) {
        ProcessInstanceMigrationDocument document = migrationDocumentBuilder.build();
        getProcessMigrationService().migrateProcessInstance(processInstanceId, document);
    }

    @Override
    public ProcessInstanceMigrationValidationResult validateMigration(String processInstanceId) {
        ProcessInstanceMigrationDocument document = migrationDocumentBuilder.build();
        return getProcessMigrationService().validateMigrationForProcessInstance(processInstanceId, document);
    }

    @Override
    public void migrateProcessInstances(String processDefinitionId) {
        ProcessInstanceMigrationDocument document = migrationDocumentBuilder.build();
        getProcessMigrationService().migrateProcessInstancesOfProcessDefinition(processDefinitionId, document);
    }

    @Override
    public ProcessInstanceMigrationValidationResult validateMigrationOfProcessInstances(String processDefinitionId) {
        ProcessInstanceMigrationDocument document = migrationDocumentBuilder.build();
        return getProcessMigrationService().validateMigrationForProcessInstancesOfProcessDefinition(processDefinitionId, document);
    }

    @Override
    public void migrateProcessInstances(String processDefinitionKey, int processDefinitionVersion, String processDefinitionTenantId) {
        ProcessInstanceMigrationDocument document = migrationDocumentBuilder.build();
        getProcessMigrationService().migrateProcessInstancesOfProcessDefinition(processDefinitionKey, processDefinitionVersion, processDefinitionTenantId, document);
    }

    @Override
    public Batch batchMigrateProcessInstances(String processDefinitionId) {
        ProcessInstanceMigrationDocument document = migrationDocumentBuilder.build();
        return getProcessMigrationService().batchMigrateProcessInstancesOfProcessDefinition(processDefinitionId, document);
    }

    @Override
    public Batch batchMigrateProcessInstances(String processDefinitionKey, int processDefinitionVersion, String processDefinitionTenantId) {
        ProcessInstanceMigrationDocument document = migrationDocumentBuilder.build();
        return getProcessMigrationService().batchMigrateProcessInstancesOfProcessDefinition(processDefinitionKey, processDefinitionVersion, processDefinitionTenantId, document);
    }

    @Override
    public ProcessInstanceMigrationValidationResult validateMigrationOfProcessInstances(String processDefinitionKey, int processDefinitionVersion, String processDefinitionTenantId) {
        ProcessInstanceMigrationDocument document = migrationDocumentBuilder.build();
        return getProcessMigrationService().validateMigrationForProcessInstancesOfProcessDefinition(processDefinitionKey, processDefinitionVersion, processDefinitionTenantId, document);
    }

    protected ProcessMigrationService getProcessMigrationService() {
        if (processInstanceMigrationService == null) {
            throw new FlowableException("ProcessInstanceMigrationService cannot be null, Obtain your builder instance from the ProcessInstanceMigrationService to access this feature");
        }
        return processInstanceMigrationService;
    }
}
