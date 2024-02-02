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
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.migration.ProcessInstanceMigrationDocument;
import org.flowable.engine.migration.ProcessInstanceMigrationManager;

public class ProcessInstanceMigrationCmd implements Command<Void> {
    
    protected String processInstanceId;
    protected String processDefinitionId;
    protected String processDefinitionKey;
    protected int processDefinitionVersion;
    protected String processDefinitionTenantId;
    protected ProcessInstanceMigrationDocument processInstanceMigrationDocument;

    public ProcessInstanceMigrationCmd(String processInstanceId, ProcessInstanceMigrationDocument processInstanceMigrationDocument) {
        if (processInstanceId == null) {
            throw new FlowableException("Must specify a process instance id to migrate");
        }
        
        if (processInstanceMigrationDocument == null) {
            throw new FlowableException("Must specify a process migration document to migrate");
        }

        this.processInstanceId = processInstanceId;
        this.processInstanceMigrationDocument = processInstanceMigrationDocument;
    }

    public ProcessInstanceMigrationCmd(ProcessInstanceMigrationDocument processInstanceMigrationDocument, String processDefinitionId) {
        if (processDefinitionId == null) {
            throw new FlowableException("Must specify a process definition id to migrate");
        }
        
        if (processInstanceMigrationDocument == null) {
            throw new FlowableException("Must specify a process migration document to migrate");
        }

        this.processDefinitionId = processDefinitionId;
        this.processInstanceMigrationDocument = processInstanceMigrationDocument;
    }

    public ProcessInstanceMigrationCmd(String processDefinitionKey, int processDefinitionVersion, String processDefinitionTenantId, ProcessInstanceMigrationDocument processInstanceMigrationDocument) {
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
    public Void execute(CommandContext commandContext) {
        ProcessInstanceMigrationManager migrationManager = CommandContextUtil.getProcessEngineConfiguration(commandContext).getProcessInstanceMigrationManager();

        if (processInstanceId != null) {
            migrationManager.migrateProcessInstance(processInstanceId, processInstanceMigrationDocument, commandContext);
        } else if (processDefinitionId != null) {
            migrationManager.migrateProcessInstancesOfProcessDefinition(processDefinitionId, processInstanceMigrationDocument, commandContext);
        } else if (processDefinitionKey != null && processDefinitionVersion >= 0) {
            migrationManager.migrateProcessInstancesOfProcessDefinition(processDefinitionKey, processDefinitionVersion, processDefinitionTenantId, processInstanceMigrationDocument, commandContext);
        } else {
            throw new FlowableException("Cannot migrate process(es), not enough information");
        }
        return null;
    }

}
