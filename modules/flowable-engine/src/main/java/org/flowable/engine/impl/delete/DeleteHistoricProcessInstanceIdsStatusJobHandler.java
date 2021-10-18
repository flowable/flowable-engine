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
package org.flowable.engine.impl.delete;

import java.util.List;

import org.flowable.batch.api.Batch;
import org.flowable.batch.api.BatchPart;
import org.flowable.batch.service.impl.persistence.entity.BatchEntity;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.ManagementService;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.job.service.JobHandler;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.variable.api.delegate.VariableScope;

/**
 * @author Filip Hrisafov
 */
public class DeleteHistoricProcessInstanceIdsStatusJobHandler implements JobHandler {

    public static final String TYPE = "delete-historic-process-status";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void execute(JobEntity job, String configuration, VariableScope variableScope, CommandContext commandContext) {
        ProcessEngineConfigurationImpl engineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        ManagementService managementService = engineConfiguration.getManagementService();
        Batch batch = managementService.createBatchQuery()
                .batchId(configuration)
                .singleResult();

        if (batch == null) {
            throw new FlowableIllegalArgumentException("There is no batch with the id " + configuration);
        }

        List<BatchPart> deleteBatchParts = managementService.createBatchPartQuery()
                .batchId(batch.getId())
                .type(DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE)
                .list();

        int completedDeletes = 0;
        int failedComputes = 0;

        for (BatchPart deleteBatchPart : deleteBatchParts) {
            if (deleteBatchPart.isCompleted()) {
                completedDeletes++;

                if (DeleteProcessInstanceBatchConstants.STATUS_FAILED.equals(deleteBatchPart.getStatus())) {
                    failedComputes++;
                }
            }
        }

        if (completedDeletes == deleteBatchParts.size()) {
            if (failedComputes == 0) {
                updateBatchStatus(batch, DeleteProcessInstanceBatchConstants.STATUS_COMPLETED, engineConfiguration);
            } else {
                updateBatchStatus(batch, DeleteProcessInstanceBatchConstants.STATUS_FAILED, engineConfiguration);
            }

            job.setRepeat(null);
        } else if (deleteBatchParts.isEmpty()) {
            updateBatchStatus(batch, DeleteProcessInstanceBatchConstants.STATUS_COMPLETED, engineConfiguration);
            job.setRepeat(null);
        }

    }

    protected void updateBatchStatus(Batch batch, String status, ProcessEngineConfigurationImpl engineConfiguration) {
        ((BatchEntity) batch).setStatus(status);
        engineConfiguration.getBatchServiceConfiguration()
                .getBatchService()
                .updateBatch(batch);
    }
}
