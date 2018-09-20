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
import org.flowable.engine.impl.migration.ProcessInstanceMigrationValidationResult;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.migration.ProcessInstanceMigrationDocument;
import org.flowable.engine.migration.ProcessInstanceMigrationManager;

/**
 * @author Dennis Federico
 */
public class ProcessInstanceMigrationValidationCmd implements Command<ProcessInstanceMigrationValidationResult> {

    protected ProcessInstanceMigrationDocument processInstanceMigrationDocument;
    protected String processInstanceId;
    protected String processDefinitionId;
    protected String processDefinitionKey;
    protected int processDefinitionVersion;
    protected String processDefinitionTenantId;

    public static ProcessInstanceMigrationValidationCmd forProcessInstance(String processInstanceId, ProcessInstanceMigrationDocument processInstanceMigrationDocument) {

        if (processInstanceId == null) {
            throw new FlowableException("Must specify a process instance id to migrate");
        }
        if (processInstanceMigrationDocument == null) {
            throw new FlowableException("Must specify a process instance migration document");
        }
        ProcessInstanceMigrationValidationCmd cmd = new ProcessInstanceMigrationValidationCmd();
        cmd.processInstanceId = processInstanceId;
        cmd.processInstanceMigrationDocument = processInstanceMigrationDocument;
        return cmd;
    }

    public static ProcessInstanceMigrationValidationCmd forProcessDefinition(String processDefinitionId, ProcessInstanceMigrationDocument processInstanceMigrationDocument) {

        if (processDefinitionId == null) {
            throw new FlowableException("Must specify a process definition id to migrate");
        }
        if (processInstanceMigrationDocument == null) {
            throw new FlowableException("Must specify a process instance migration document");
        }
        ProcessInstanceMigrationValidationCmd cmd = new ProcessInstanceMigrationValidationCmd();
        cmd.processDefinitionId = processDefinitionId;
        cmd.processInstanceMigrationDocument = processInstanceMigrationDocument;
        return cmd;
    }

    public static ProcessInstanceMigrationValidationCmd forProcessDefinition(String processDefinitionKey, int processDefinitionVersion, String processDefinitionTenantId,
        ProcessInstanceMigrationDocument processInstanceMigrationDocument) {

        if (processDefinitionKey == null) {
            throw new FlowableException("Must specify the process definition key to migrate");
        }
        if (processDefinitionVersion < 0) {
            throw new FlowableException("Must specify a valid version number to migrate");
        }
        if (processInstanceMigrationDocument == null) {
            throw new FlowableException("Must specify a process instance migration document");
        }
        ProcessInstanceMigrationValidationCmd cmd = new ProcessInstanceMigrationValidationCmd();
        cmd.processDefinitionKey = processDefinitionKey;
        cmd.processDefinitionVersion = processDefinitionVersion;
        cmd.processDefinitionTenantId = processDefinitionTenantId;
        cmd.processInstanceMigrationDocument = processInstanceMigrationDocument;
        return cmd;
    }

    @Override
    public ProcessInstanceMigrationValidationResult execute(CommandContext commandContext) {

        ProcessInstanceMigrationManager migrationManager = CommandContextUtil.getProcessEngineConfiguration(commandContext).getProcessInstanceMigrationManager();

        if (processInstanceId != null) {
            return migrationManager.validateMigrateProcessInstance(processInstanceId, processInstanceMigrationDocument, commandContext);
        }

        if (processDefinitionId != null) {
            return migrationManager.validateMigrateProcessInstancesOfProcessDefinition(processDefinitionId, processInstanceMigrationDocument, commandContext);
        }

        if (processDefinitionKey != null && processDefinitionVersion >= 0) {
            return migrationManager.validateMigrateProcessInstancesOfProcessDefinition(processDefinitionKey, processDefinitionVersion, processDefinitionTenantId, processInstanceMigrationDocument, commandContext);
        }

        throw new FlowableException("Cannot validate process migration, not enough information");
    }

}
