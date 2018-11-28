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

import org.flowable.common.engine.impl.service.CommonEngineServiceImpl;
import org.flowable.engine.ProcessInstanceMigrationService;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.cmd.DeleteProcessMigrationBatchAndResourcesCmd;
import org.flowable.engine.impl.cmd.GetProcessInstanceMigrationBatchValidationResultCmd;
import org.flowable.engine.impl.cmd.GetProcessInstanceMigrationValidationBatchCmd;
import org.flowable.engine.impl.cmd.ProcessInstanceMigrationBatchValidationCmd;
import org.flowable.engine.impl.cmd.ProcessInstanceMigrationCmd;
import org.flowable.engine.impl.cmd.ProcessInstanceMigrationValidationCmd;
import org.flowable.engine.impl.migration.ProcessInstanceMigrationBuilderImpl;
import org.flowable.engine.impl.migration.ProcessInstanceMigrationValidationResult;
import org.flowable.engine.migration.ProcessInstanceMigrationBuilder;
import org.flowable.engine.migration.ProcessInstanceMigrationDocument;
import org.flowable.engine.runtime.ProcessMigrationBatch;

/**
 * @author Dennis
 */
public class ProcessInstanceMigrationServiceImpl extends CommonEngineServiceImpl<ProcessEngineConfigurationImpl> implements ProcessInstanceMigrationService {

    public ProcessInstanceMigrationServiceImpl(ProcessEngineConfigurationImpl configuration) {
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
        return commandExecutor.execute(ProcessInstanceMigrationValidationCmd.forProcessInstance(processInstanceId, processInstanceMigrationDocument));
    }

    @Override
    public ProcessInstanceMigrationValidationResult validateMigrationForProcessInstancesOfProcessDefinition(String processDefinitionId, ProcessInstanceMigrationDocument processInstanceMigrationDocument) {
        return commandExecutor.execute(ProcessInstanceMigrationValidationCmd.forProcessDefinition(processDefinitionId, processInstanceMigrationDocument));
    }

    @Override
    public ProcessInstanceMigrationValidationResult validateMigrationForProcessInstancesOfProcessDefinition(String processDefinitionKey, int processDefinitionVersion, String processDefinitionTenantId, ProcessInstanceMigrationDocument processInstanceMigrationDocument) {
        return commandExecutor.execute(ProcessInstanceMigrationValidationCmd.forProcessDefinition(processDefinitionKey, processDefinitionVersion, processDefinitionTenantId, processInstanceMigrationDocument));
    }

    @Override
    public String batchValidateMigrationForProcessInstancesOfProcessDefinition(String processDefinitionId, ProcessInstanceMigrationDocument processInstanceMigrationDocument) {
        return commandExecutor.execute(ProcessInstanceMigrationBatchValidationCmd.forProcessDefinition(processDefinitionId, processInstanceMigrationDocument));
    }

    @Override
    public String batchValidateMigrationForProcessInstancesOfProcessDefinition(String processDefinitionKey, int processDefinitionVersion, String processDefinitionTenantId, ProcessInstanceMigrationDocument processInstanceMigrationDocument) {
        return commandExecutor.execute(ProcessInstanceMigrationBatchValidationCmd.forProcessDefinition(processDefinitionKey, processDefinitionVersion, processDefinitionTenantId, processInstanceMigrationDocument));
    }

    @Override
    public ProcessInstanceMigrationValidationResult getAggregatedResultOfBatchProcessInstanceMigrationValidation(String migrationBatchId) {
        return commandExecutor.execute(new GetProcessInstanceMigrationBatchValidationResultCmd(migrationBatchId));
    }

    @Override
    public ProcessMigrationBatch getProcessMigrationBatchById(String migrationBatchId) {
        return commandExecutor.execute(new GetProcessInstanceMigrationValidationBatchCmd(migrationBatchId, false));
    }

    @Override
    public ProcessMigrationBatch getProcessMigrationBatchAndResourcesById(String migrationBatchId) {
        return commandExecutor.execute(new GetProcessInstanceMigrationValidationBatchCmd(migrationBatchId, true));
    }

    @Override
    public void deleteBatchAndResourcesById(String migrationBatchId) {
        commandExecutor.execute(new DeleteProcessMigrationBatchAndResourcesCmd(migrationBatchId));
    }

    @Override
    public void migrateProcessInstance(String processInstanceId, ProcessInstanceMigrationDocument processInstanceMigrationDocument) {
        commandExecutor.execute(ProcessInstanceMigrationCmd.forProcessInstance(processInstanceId, processInstanceMigrationDocument));
    }

    @Override
    public void migrateProcessInstancesOfProcessDefinition(String processDefinitionId, ProcessInstanceMigrationDocument processInstanceMigrationDocument) {
        commandExecutor.execute(ProcessInstanceMigrationCmd.forProcessDefinition(processDefinitionId, processInstanceMigrationDocument));
    }

    @Override
    public void migrateProcessInstancesOfProcessDefinition(String processDefinitionKey, int processDefinitionVersion, String processDefinitionTenantId, ProcessInstanceMigrationDocument processInstanceMigrationDocument) {
        commandExecutor.execute(ProcessInstanceMigrationCmd.forProcessDefinition(processDefinitionKey, processDefinitionVersion, processDefinitionTenantId, processInstanceMigrationDocument));
    }

    @Override
    public void batchMigrateProcessInstancesOfProcessDefinition(String processDefinitionId, ProcessInstanceMigrationDocument processInstanceMigrationDocument) {
        throw new UnsupportedOperationException("Not implemented yet!!!");
        //        commandExecutor.execute(ProcessInstanceMigrationCmd.forProcessDefinition(processDefinitionId, processInstanceMigrationDocument));
    }

    @Override
    public void batchMigrateProcessInstancesOfProcessDefinition(String processDefinitionKey, int processDefinitionVersion, String processDefinitionTenantId, ProcessInstanceMigrationDocument processInstanceMigrationDocument) {
        throw new UnsupportedOperationException("Not implemented yet!!!");
        //        commandExecutor.execute(ProcessInstanceMigrationCmd.forProcessDefinition(processDefinitionKey, processDefinitionVersion, processDefinitionTenantId, processInstanceMigrationDocument));
    }
}

