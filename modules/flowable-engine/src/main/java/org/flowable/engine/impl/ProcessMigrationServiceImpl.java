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
package org.flowable.engine.impl;

import org.flowable.batch.api.Batch;
import org.flowable.common.engine.impl.service.CommonEngineServiceImpl;
import org.flowable.engine.ProcessMigrationService;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.cmd.GetProcessInstanceMigrationBatchResultCmd;
import org.flowable.engine.impl.cmd.ProcessInstanceMigrationBatchCmd;
import org.flowable.engine.impl.cmd.ProcessInstanceMigrationCmd;
import org.flowable.engine.impl.cmd.ProcessInstanceMigrationValidationCmd;
import org.flowable.engine.impl.migration.ProcessInstanceMigrationBuilderImpl;
import org.flowable.engine.migration.ProcessInstanceBatchMigrationResult;
import org.flowable.engine.migration.ProcessInstanceMigrationBuilder;
import org.flowable.engine.migration.ProcessInstanceMigrationDocument;
import org.flowable.engine.migration.ProcessInstanceMigrationValidationResult;

public class ProcessMigrationServiceImpl extends CommonEngineServiceImpl<ProcessEngineConfigurationImpl> implements ProcessMigrationService {

    public ProcessMigrationServiceImpl(ProcessEngineConfigurationImpl configuration) {
        super(configuration);
    }

    @Override
    public ProcessInstanceMigrationBuilder createProcessInstanceMigrationBuilder() {
        return new ProcessInstanceMigrationBuilderImpl(this);
    }

    @Override
    public ProcessInstanceMigrationBuilder createProcessInstanceMigrationBuilderFromProcessInstanceMigrationDocument(ProcessInstanceMigrationDocument document) {
        return createProcessInstanceMigrationBuilder().fromProcessInstanceMigrationDocument(document);
    }

    @Override
    public ProcessInstanceMigrationValidationResult validateMigrationForProcessInstance(String processInstanceId, ProcessInstanceMigrationDocument processInstanceMigrationDocument) {
        return commandExecutor.execute(new ProcessInstanceMigrationValidationCmd(processInstanceId, processInstanceMigrationDocument));
    }

    @Override
    public ProcessInstanceMigrationValidationResult validateMigrationForProcessInstancesOfProcessDefinition(String processDefinitionId, ProcessInstanceMigrationDocument processInstanceMigrationDocument) {
        return commandExecutor.execute(new ProcessInstanceMigrationValidationCmd(processInstanceMigrationDocument, processDefinitionId));
    }

    @Override
    public ProcessInstanceMigrationValidationResult validateMigrationForProcessInstancesOfProcessDefinition(String processDefinitionKey, int processDefinitionVersion, String processDefinitionTenantId, ProcessInstanceMigrationDocument processInstanceMigrationDocument) {
        return commandExecutor.execute(new ProcessInstanceMigrationValidationCmd(processDefinitionKey, processDefinitionVersion, processDefinitionTenantId, processInstanceMigrationDocument));
    }

    @Override
    public void migrateProcessInstance(String processInstanceId, ProcessInstanceMigrationDocument processInstanceMigrationDocument) {
        commandExecutor.execute(new ProcessInstanceMigrationCmd(processInstanceId, processInstanceMigrationDocument));
    }

    @Override
    public void migrateProcessInstancesOfProcessDefinition(String processDefinitionId, ProcessInstanceMigrationDocument processInstanceMigrationDocument) {
        commandExecutor.execute(new ProcessInstanceMigrationCmd(processInstanceMigrationDocument, processDefinitionId));
    }

    @Override
    public void migrateProcessInstancesOfProcessDefinition(String processDefinitionKey, int processDefinitionVersion, String processDefinitionTenantId, ProcessInstanceMigrationDocument processInstanceMigrationDocument) {
        commandExecutor.execute(new ProcessInstanceMigrationCmd(processDefinitionKey, processDefinitionVersion, processDefinitionTenantId, processInstanceMigrationDocument));
    }

    @Override
    public Batch batchMigrateProcessInstancesOfProcessDefinition(String processDefinitionId, ProcessInstanceMigrationDocument processInstanceMigrationDocument) {
        return commandExecutor.execute(new ProcessInstanceMigrationBatchCmd(processDefinitionId, processInstanceMigrationDocument));
    }

    @Override
    public Batch batchMigrateProcessInstancesOfProcessDefinition(String processDefinitionKey, int processDefinitionVersion, String processDefinitionTenantId, ProcessInstanceMigrationDocument processInstanceMigrationDocument) {
        return commandExecutor.execute(new ProcessInstanceMigrationBatchCmd(processDefinitionKey, processDefinitionVersion, processDefinitionTenantId, processInstanceMigrationDocument));
    }

    @Override
    public ProcessInstanceBatchMigrationResult getResultsOfBatchProcessInstanceMigration(String migrationBatchId) {
        return commandExecutor.execute(new GetProcessInstanceMigrationBatchResultCmd(migrationBatchId));
    }
}

