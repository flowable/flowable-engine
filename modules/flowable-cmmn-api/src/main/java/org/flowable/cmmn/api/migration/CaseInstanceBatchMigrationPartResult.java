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

package org.flowable.cmmn.api.migration;

public class CaseInstanceBatchMigrationPartResult {

    protected String batchId;
    protected String status = CaseInstanceBatchMigrationResult.STATUS_WAITING;
    protected String result;
    protected String caseInstanceId;
    protected String sourceCaseDefinitionId;
    protected String targetCaseDefinitionId;
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

    public String getCaseInstanceId() {
        return caseInstanceId;
    }

    public void setCaseInstanceId(String caseInstanceId) {
        this.caseInstanceId = caseInstanceId;
    }

    public String getSourceCaseDefinitionId() {
        return sourceCaseDefinitionId;
    }

    public void setSourceCaseDefinitionId(String sourceCaseDefinitionId) {
        this.sourceCaseDefinitionId = sourceCaseDefinitionId;
    }

    public String getTargetCaseDefinitionId() {
        return targetCaseDefinitionId;
    }

    public void setTargetCaseDefinitionId(String targetCaseDefinitionId) {
        this.targetCaseDefinitionId = targetCaseDefinitionId;
    }

    public String getMigrationMessage() {
        return migrationMessage;
    }

    public void setMigrationMessage(String migrationMessage) {
        this.migrationMessage = migrationMessage;
    }
}
