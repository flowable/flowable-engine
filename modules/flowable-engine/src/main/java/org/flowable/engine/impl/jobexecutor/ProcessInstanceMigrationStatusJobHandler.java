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
package org.flowable.engine.impl.jobexecutor;

import java.util.List;

import org.flowable.batch.api.Batch;
import org.flowable.batch.api.BatchPart;
import org.flowable.batch.api.BatchService;
import org.flowable.batch.service.impl.persistence.entity.BatchEntity;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.migration.ProcessInstanceBatchMigrationResult;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.variable.api.delegate.VariableScope;

public class ProcessInstanceMigrationStatusJobHandler extends AbstractProcessInstanceMigrationJobHandler {

    public static final String TYPE = "process-migration-status";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void execute(JobEntity job, String configuration, VariableScope variableScope, CommandContext commandContext) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        BatchService batchService = processEngineConfiguration.getBatchServiceConfiguration().getBatchService();
        
        String batchId = getBatchIdFromHandlerCfg(configuration);
        Batch batch = batchService.getBatch(batchId);
        
        List<BatchPart> batchParts = batchService.findBatchPartsByBatchId(batchId);
        int completedBatchParts = 0;
        int failedBatchParts = 0;
        for (BatchPart batchPart : batchParts) {
            if (batchPart.getCompleteTime() != null) {
                completedBatchParts++;
                
                if (ProcessInstanceBatchMigrationResult.RESULT_FAIL.equals(batchPart.getStatus())) {
                    failedBatchParts++;
                }
            }
        }
        
        if (completedBatchParts == batchParts.size()) {
            batchService.completeBatch(batch.getId(), ProcessInstanceBatchMigrationResult.STATUS_COMPLETED);
            job.setRepeat(null);
        
        } else {
            if (batchParts.size() == 0) {
                updateBatchStatus(batch, "No batch parts", batchService);
                job.setRepeat(null);
            
            } else {
                int completedPercentage = completedBatchParts / batchParts.size() * 100;
                updateBatchStatus(batch, completedPercentage + "% completed, " + failedBatchParts + " failed", batchService);
            }
        }
    }
    
    protected void updateBatchStatus(Batch batch, String status, BatchService batchService) {
        ((BatchEntity) batch).setStatus(ProcessInstanceBatchMigrationResult.STATUS_COMPLETED);
        batchService.updateBatch(batch);
    }

}