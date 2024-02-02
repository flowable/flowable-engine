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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ProcessInstanceBatchMigrationResult {
    
    public static final String STATUS_IN_PROGRESS = "inProgress";
    public static final String STATUS_WAITING = "waiting";
    public static final String STATUS_COMPLETED = "completed";
    
    public static final String RESULT_SUCCESS = "success";
    public static final String RESULT_FAIL = "fail";

    protected String batchId;
    protected String status;
    protected Date completeTime;
    protected String sourceProcessDefinitionId;
    protected String targetProcessDefinitionId;
    protected List<ProcessInstanceBatchMigrationPartResult> allMigrationParts = new ArrayList<>();
    protected List<ProcessInstanceBatchMigrationPartResult> succesfulMigrationParts = new ArrayList<>();
    protected List<ProcessInstanceBatchMigrationPartResult> failedMigrationParts = new ArrayList<>();
    protected List<ProcessInstanceBatchMigrationPartResult> waitingMigrationParts = new ArrayList<>();

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

    public Date getCompleteTime() {
        return completeTime;
    }

    public void setCompleteTime(Date completeTime) {
        this.completeTime = completeTime;
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

    public List<ProcessInstanceBatchMigrationPartResult> getAllMigrationParts() {
        return allMigrationParts;
    }

    public void addMigrationPart(ProcessInstanceBatchMigrationPartResult migrationPart) {
        if (allMigrationParts == null) {
            allMigrationParts = new ArrayList<>();
        }
        allMigrationParts.add(migrationPart);
        
        if (!STATUS_COMPLETED.equals(migrationPart.getStatus())) {
            if (waitingMigrationParts == null) {
                waitingMigrationParts = new ArrayList<>();
            }
            waitingMigrationParts.add(migrationPart);
        
        } else {
            if (RESULT_SUCCESS.equals(migrationPart.getResult())) {
                if (succesfulMigrationParts == null) {
                    succesfulMigrationParts = new ArrayList<>();
                }
                succesfulMigrationParts.add(migrationPart);
            
            } else {
                if (failedMigrationParts == null) {
                    failedMigrationParts = new ArrayList<>();
                }
                failedMigrationParts.add(migrationPart);
            }
        }
    }

    public List<ProcessInstanceBatchMigrationPartResult> getSuccessfulMigrationParts() {
        return succesfulMigrationParts;
    }

    public List<ProcessInstanceBatchMigrationPartResult> getFailedMigrationParts() {
        return failedMigrationParts;
    }

    public List<ProcessInstanceBatchMigrationPartResult> getWaitingMigrationParts() {
        return waitingMigrationParts;
    }

}
