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

import org.flowable.engine.impl.migration.ProcessInstanceMigrationValidationResult;
import org.flowable.engine.impl.persistence.entity.ProcessMigrationBatchEntity;
import org.flowable.engine.migration.ProcessInstanceMigrationBuilder;
import org.flowable.engine.migration.ProcessInstanceMigrationDocument;

/**
 * Service for preparing and execution ProcessInstanceMigration operations.
 *
 * @author Dennis Federico
 */
public interface ProcessInstanceMigrationService {

    ProcessInstanceMigrationBuilder createProcessInstanceMigrationBuilder();

    ProcessInstanceMigrationBuilder createProcessInstanceMigrationBuilderFromProcessInstanceMigrationDocument(ProcessInstanceMigrationDocument document);

    ProcessInstanceMigrationValidationResult validateMigrationForProcessInstance(String processInstanceId, ProcessInstanceMigrationDocument processInstanceMigrationDocument);

    ProcessInstanceMigrationValidationResult validateMigrationForProcessInstancesOfProcessDefinition(String processDefinitionId, ProcessInstanceMigrationDocument processInstanceMigrationDocument);

    ProcessInstanceMigrationValidationResult validateMigrationForProcessInstancesOfProcessDefinition(String processDefinitionKey, int processDefinitionVersion, String processDefinitionTenantId, ProcessInstanceMigrationDocument processInstanceMigrationDocument);

    //TODO WIP - Remove - batch validate of a single processInstance seems nonsensical
    //    ProcessMigrationBatchEntity batchValidateMigrationForProcessInstance(String processInstanceId, ProcessInstanceMigrationDocument processInstanceMigrationDocument);

    ProcessMigrationBatchEntity batchValidateMigrationForProcessInstancesOfProcessDefinition(String processDefinitionId, ProcessInstanceMigrationDocument processInstanceMigrationDocument);

    ProcessMigrationBatchEntity batchValidateMigrationForProcessInstancesOfProcessDefinition(String processDefinitionKey, int processDefinitionVersion, String processDefinitionTenantId, ProcessInstanceMigrationDocument processInstanceMigrationDocument);

    void migrateProcessInstance(String processInstanceId, ProcessInstanceMigrationDocument processInstanceMigrationDocument);

    void migrateProcessInstancesOfProcessDefinition(String processDefinitionId, ProcessInstanceMigrationDocument processInstanceMigrationDocument);

    void migrateProcessInstancesOfProcessDefinition(String processDefinitionKey, int processDefinitionVersion, String processDefinitionTenantId, ProcessInstanceMigrationDocument processInstanceMigrationDocument);
}

