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
import org.flowable.engine.migration.ProcessInstanceMigrationDocument;

/**
 * @author Dennis Federico
 */
public class AbstractProcessInstanceMigrationCmd {

    protected ProcessInstanceMigrationDocument processInstanceMigrationDocument;
    protected String processInstanceId;
    protected String processDefinitionId;
    protected String processDefinitionKey;
    protected int processDefinitionVersion;
    protected String processDefinitionTenantId;

    protected void validateProcessDefinitionArgument() {

    }

    protected static void requireNonNullProcessInstanceMigrationDocument(ProcessInstanceMigrationDocument document) {
        if (document == null) {
            throw new FlowableException("Must specify a process instance migration document");
        }
    }

    protected static void requireNonNullProcessDefinitionId(String processDefinitionId) {
        if (processDefinitionId == null) {
            throw new FlowableException("Must specify a process definition id to migrate");
        }
    }

    protected static void requireNonNullProcessDefinitionKey(String processDefinitionKey) {
        if (processDefinitionKey == null) {
            throw new FlowableException("Must specify the process definition key to migrate");
        }
    }

    protected static void requirePositiveProcessDefinitionVersion(int processDefinitionVersion) {
        if (processDefinitionVersion < 0) {
            throw new FlowableException("Must specify a positive definition version number to migrate");
        }
    }

    protected static void requireNonNullProcessInstanceId(String processInstanceId) {
        if (processInstanceId == null) {
            throw new FlowableException("Must specify a process instance id to migrate");
        }
    }

}

