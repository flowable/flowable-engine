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
package org.flowable.engine;

import java.util.List;

import org.flowable.engine.migration.ProcessInstanceMigrationBuilder;
import org.flowable.engine.migration.ProcessInstanceMigrationDocument;
import org.flowable.engine.migration.ProcessInstanceMigrationResult;

/**
 * Service for preparing and execution ProcessInstanceMigration operations.
 *
 * @author Dennis Federico
 */
public interface ProcessInstanceMigrationService {

    ProcessInstanceMigrationBuilder createProcessInstanceMigrationBuilder();

    ProcessInstanceMigrationBuilder createProcessInstanceMigrationBuilderFromProcessInstanceMigrationDocument(ProcessInstanceMigrationDocument document);

    ProcessInstanceMigrationResult<List<String>> validateMigrationForProcessInstance(String processInstanceId, ProcessInstanceMigrationDocument processInstanceMigrationDocument);

    ProcessInstanceMigrationResult<List<String>> validateMigrationForProcessInstancesOfProcessDefinition(String processDefinitionId, ProcessInstanceMigrationDocument processInstanceMigrationDocument);

    ProcessInstanceMigrationResult<List<String>> validateMigrationForProcessInstancesOfProcessDefinition(String processDefinitionKey, int processDefinitionVersion, String processDefinitionTenantId, ProcessInstanceMigrationDocument processInstanceMigrationDocument);

    String batchValidateMigrationForProcessInstancesOfProcessDefinition(String processDefinitionId, ProcessInstanceMigrationDocument processInstanceMigrationDocument);

    String batchValidateMigrationForProcessInstancesOfProcessDefinition(String processDefinitionKey, int processDefinitionVersion, String processDefinitionTenantId, ProcessInstanceMigrationDocument processInstanceMigrationDocument);

    ProcessInstanceMigrationResult<List<String>> getResultsOfBatchProcessInstanceMigrationValidation(String migrationBatchId);

    //TODO WIP - Having the ProcessInstanceMigrationResult this Batch entities seem nonsensical - but batch timeStamps should be included in the Result Object
    //    ProcessMigrationBatch getProcessMigrationBatchById(String migrationBatchId);
    //    ProcessMigrationBatch getProcessMigrationBatchAndResourcesById(String migrationBatchId);

    void deleteBatchAndResourcesById(String migrationBatchId);

    void migrateProcessInstance(String processInstanceId, ProcessInstanceMigrationDocument processInstanceMigrationDocument);

    void migrateProcessInstancesOfProcessDefinition(String processDefinitionId, ProcessInstanceMigrationDocument processInstanceMigrationDocument);

    void migrateProcessInstancesOfProcessDefinition(String processDefinitionKey, int processDefinitionVersion, String processDefinitionTenantId, ProcessInstanceMigrationDocument processInstanceMigrationDocument);

    String batchMigrateProcessInstancesOfProcessDefinition(String processDefinitionId, ProcessInstanceMigrationDocument processInstanceMigrationDocument);

    String batchMigrateProcessInstancesOfProcessDefinition(String processDefinitionKey, int processDefinitionVersion, String processDefinitionTenantId, ProcessInstanceMigrationDocument processInstanceMigrationDocument);

    ProcessInstanceMigrationResult<String> getResultsOfBatchProcessInstanceMigration(String migrationBatchId);
}

