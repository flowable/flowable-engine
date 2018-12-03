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

package org.flowable.engine.impl.cmd;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.ProcessMigrationBatchEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.migration.ProcessInstanceMigrationDocument;
import org.flowable.engine.migration.ProcessInstanceMigrationManager;

/**
 * @author Dennis Federico
 */
public class ProcessInstanceMigrationBatchValidationCmd extends AbstractProcessInstanceMigrationCmd implements Command<String> {

    public static ProcessInstanceMigrationBatchValidationCmd forProcessDefinition(String processDefinitionId, ProcessInstanceMigrationDocument processInstanceMigrationDocument) {

        requireNonNullProcessDefinitionId(processDefinitionId);
        requireNonNullProcessInstanceMigrationDocument(processInstanceMigrationDocument);

        ProcessInstanceMigrationBatchValidationCmd cmd = new ProcessInstanceMigrationBatchValidationCmd();
        cmd.processDefinitionId = processDefinitionId;
        cmd.processInstanceMigrationDocument = processInstanceMigrationDocument;
        return cmd;
    }

    public static ProcessInstanceMigrationBatchValidationCmd forProcessDefinition(String processDefinitionKey, int processDefinitionVersion, String processDefinitionTenantId,
        ProcessInstanceMigrationDocument processInstanceMigrationDocument) {

        requireNonNullProcessDefinitionKey(processDefinitionKey);
        requirePositiveProcessDefinitionVersion(processDefinitionVersion);
        requireNonNullProcessInstanceMigrationDocument(processInstanceMigrationDocument);

        ProcessInstanceMigrationBatchValidationCmd cmd = new ProcessInstanceMigrationBatchValidationCmd();
        cmd.processDefinitionKey = processDefinitionKey;
        cmd.processDefinitionVersion = processDefinitionVersion;
        cmd.processDefinitionTenantId = processDefinitionTenantId;
        cmd.processInstanceMigrationDocument = processInstanceMigrationDocument;
        return cmd;
    }

    @Override
    public String execute(CommandContext commandContext) {

        ProcessInstanceMigrationManager migrationManager = CommandContextUtil.getProcessEngineConfiguration(commandContext).getProcessInstanceMigrationManager();
        if (processDefinitionId != null) {
            ProcessMigrationBatchEntity batchEntity = migrationManager.batchValidateMigrateProcessInstancesOfProcessDefinition(processDefinitionId, processInstanceMigrationDocument, commandContext);
            return batchEntity.getId();
        }

        if (processDefinitionKey != null && processDefinitionVersion >= 0) {
            ProcessMigrationBatchEntity batchEntity = migrationManager.batchValidateMigrateProcessInstancesOfProcessDefinition(processDefinitionKey, processDefinitionVersion, processDefinitionTenantId, processInstanceMigrationDocument, commandContext);
            return batchEntity.getId();
        }

        throw new FlowableException("Cannot validate process migration, not enough information");
    }

}
