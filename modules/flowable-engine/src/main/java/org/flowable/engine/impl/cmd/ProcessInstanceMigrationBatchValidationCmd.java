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
public class ProcessInstanceMigrationBatchValidationCmd implements Command<ProcessMigrationBatchEntity> {

    protected ProcessInstanceMigrationDocument processInstanceMigrationDocument;
    //TODO WIP - remove - batch validation of a single processInstance seems nonsensical
    //    protected String processInstanceId;
    protected String processDefinitionId;
    protected String processDefinitionKey;
    protected int processDefinitionVersion;
    protected String processDefinitionTenantId;

    //TODO WIP - remove - batch validation of a single processInstance seems nonsensical
    //    public static ProcessInstanceMigrationBatchValidationCmd forProcessInstance(String processInstanceId, ProcessInstanceMigrationDocument processInstanceMigrationDocument) {
    //
    //        if (processInstanceId == null) {
    //            throw new FlowableException("Must specify a process instance id to migrate");
    //        }
    //        if (processInstanceMigrationDocument == null) {
    //            throw new FlowableException("Must specify a process instance migration document");
    //        }
    //        ProcessInstanceMigrationBatchValidationCmd cmd = new ProcessInstanceMigrationBatchValidationCmd();
    //        cmd.processInstanceId = processInstanceId;
    //        cmd.processInstanceMigrationDocument = processInstanceMigrationDocument;
    //        return cmd;
    //    }

    public static ProcessInstanceMigrationBatchValidationCmd forProcessDefinition(String processDefinitionId, ProcessInstanceMigrationDocument processInstanceMigrationDocument) {

        if (processDefinitionId == null) {
            throw new FlowableException("Must specify a process definition id to migrate");
        }
        if (processInstanceMigrationDocument == null) {
            throw new FlowableException("Must specify a process instance migration document");
        }
        ProcessInstanceMigrationBatchValidationCmd cmd = new ProcessInstanceMigrationBatchValidationCmd();
        cmd.processDefinitionId = processDefinitionId;
        cmd.processInstanceMigrationDocument = processInstanceMigrationDocument;
        return cmd;
    }

    public static ProcessInstanceMigrationBatchValidationCmd forProcessDefinition(String processDefinitionKey, int processDefinitionVersion, String processDefinitionTenantId,
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
        ProcessInstanceMigrationBatchValidationCmd cmd = new ProcessInstanceMigrationBatchValidationCmd();
        cmd.processDefinitionKey = processDefinitionKey;
        cmd.processDefinitionVersion = processDefinitionVersion;
        cmd.processDefinitionTenantId = processDefinitionTenantId;
        cmd.processInstanceMigrationDocument = processInstanceMigrationDocument;
        return cmd;
    }

    @Override
    public ProcessMigrationBatchEntity execute(CommandContext commandContext) {

        ProcessInstanceMigrationManager migrationManager = CommandContextUtil.getProcessEngineConfiguration(commandContext).getProcessInstanceMigrationManager();
        
        //TODO WIP - remove - batch validation of a single processInstance seems nonsensical
        //        if (processInstanceId != null) {
        //            return migrationManager.batchValidateMigrateProcessInstance(processInstanceId, processInstanceMigrationDocument, commandContext);
        //        }

        if (processDefinitionId != null) {
            return migrationManager.batchValidateMigrateProcessInstancesOfProcessDefinition(processDefinitionId, processInstanceMigrationDocument, commandContext);
        }

        if (processDefinitionKey != null && processDefinitionVersion >= 0) {
            return migrationManager.batchValidateMigrateProcessInstancesOfProcessDefinition(processDefinitionKey, processDefinitionVersion, processDefinitionTenantId, processInstanceMigrationDocument, commandContext);
        }

        throw new FlowableException("Cannot validate process migration, not enough information");
    }

}
