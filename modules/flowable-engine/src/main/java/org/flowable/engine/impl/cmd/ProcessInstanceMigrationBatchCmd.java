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

import org.flowable.batch.api.Batch;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.migration.ProcessInstanceMigrationDocument;
import org.flowable.engine.migration.ProcessInstanceMigrationManager;

public class ProcessInstanceMigrationBatchCmd implements Command<Batch> {
    
    protected String processDefinitionId;
    protected String processDefinitionKey;
    protected int processDefinitionVersion;
    protected String processDefinitionTenantId;
    protected ProcessInstanceMigrationDocument processInstanceMigrationDocument;

    public ProcessInstanceMigrationBatchCmd(String processDefinitionId, ProcessInstanceMigrationDocument processInstanceMigrationDocument) {
        if (processDefinitionId == null) {
            throw new FlowableException("Must specify a process definition id to migrate");
        }
        
        if (processInstanceMigrationDocument == null) {
            throw new FlowableException("Must specify a process migration document to migrate");
        }

        this.processDefinitionId = processDefinitionId;
        this.processInstanceMigrationDocument = processInstanceMigrationDocument;
    }

    public ProcessInstanceMigrationBatchCmd(String processDefinitionKey, int processDefinitionVersion, String processDefinitionTenantId,
                    ProcessInstanceMigrationDocument processInstanceMigrationDocument) {

        if (processDefinitionKey == null) {
            throw new FlowableException("Must specify a process definition key to migrate");
        }
        
        if (processInstanceMigrationDocument == null) {
            throw new FlowableException("Must specify a process migration document to migrate");
        }

        this.processDefinitionKey = processDefinitionKey;
        this.processDefinitionVersion = processDefinitionVersion;
        this.processDefinitionTenantId = processDefinitionTenantId;
        this.processInstanceMigrationDocument = processInstanceMigrationDocument;
    }

    @Override
    public Batch execute(CommandContext commandContext) {
        ProcessInstanceMigrationManager migrationManager = CommandContextUtil.getProcessEngineConfiguration(commandContext).getProcessInstanceMigrationManager();
        if (processDefinitionId != null) {
            return migrationManager.batchMigrateProcessInstancesOfProcessDefinition(processDefinitionId, processInstanceMigrationDocument, commandContext);
        }

        return migrationManager.batchMigrateProcessInstancesOfProcessDefinition(processDefinitionKey, processDefinitionVersion, 
                        processDefinitionTenantId, processInstanceMigrationDocument, commandContext);
    }

}
