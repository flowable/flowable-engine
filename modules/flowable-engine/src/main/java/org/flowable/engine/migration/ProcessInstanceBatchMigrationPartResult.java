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

package org.flowable.engine.migration;

public class ProcessInstanceBatchMigrationPartResult {

    protected String batchId;
    protected String status = ProcessInstanceBatchMigrationResult.STATUS_WAITING;
    protected String result;
    protected String processInstanceId;
    protected String sourceProcessDefinitionId;
    protected String targetProcessDefinitionId;
    protected String migrationMessage;

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getSourceProcessDefinitionId() {
        return sourceProcessDefinitionId;
    }

    public void setSourceProcessDefinitionId(String sourceProcessDefinitionId) {
        this.sourceProcessDefinitionId = sourceProcessDefinitionId;
    }

    public String getTargetProcessDefinitionId() {
        return targetProcessDefinitionId;
    }

    public void setTargetProcessDefinitionId(String targetProcessDefinitionId) {
        this.targetProcessDefinitionId = targetProcessDefinitionId;
    }

    public String getMigrationMessage() {
        return migrationMessage;
    }

    public void setMigrationMessage(String migrationMessage) {
        this.migrationMessage = migrationMessage;
    }
}
