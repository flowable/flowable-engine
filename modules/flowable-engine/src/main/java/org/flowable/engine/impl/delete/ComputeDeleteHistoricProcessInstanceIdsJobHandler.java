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
import org.flowable.batch.api.BatchService;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.job.service.JobHandler;
import org.flowable.job.service.JobService;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.variable.api.delegate.VariableScope;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Filip Hrisafov
 */
public class ComputeDeleteHistoricProcessInstanceIdsJobHandler implements JobHandler {

    public static final String TYPE = "compute-delete-historic-process-ids";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void execute(JobEntity job, String configuration, VariableScope variableScope, CommandContext commandContext) {
        ProcessEngineConfigurationImpl engineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        BatchService batchService = engineConfiguration.getBatchServiceConfiguration().getBatchService();

        BatchDeleteProcessConfig config = BatchDeleteProcessConfig.create(configuration, engineConfiguration);
        BatchPart batchPart = config.getBatchPart();
        Batch batch = config.getBatch();
        boolean sequentialExecution = config.isSequentialExecution();
        if (config.hasError()) {
            failBatchPart(engineConfiguration, batchService, batchPart, batch, config.getError(), sequentialExecution);
            return;
        }

        int batchSize = config.getBatchSize();
        int batchPartNumber = Integer.parseInt(batchPart.getSearchKey());
        // The first result is the batch part number multiplied by the batch size
        // e.g. if this is the 5th batch part (batch part number 4) and the batch size is 100 the first result should start from 400
        int firstResult = batchPartNumber * batchSize;

        HistoricProcessInstanceQuery query = config.getQuery();
        List<HistoricProcessInstance> processInstances = query.listPage(firstResult, batchSize);

        ObjectNode resultNode = engineConfiguration.getObjectMapper().createObjectNode();
        ArrayNode idsToDelete = resultNode.putArray("processInstanceIdsToDelete");
        for (HistoricProcessInstance processInstance : processInstances) {
            idsToDelete.add(processInstance.getId());
        }

        BatchPart batchPartForDelete = engineConfiguration.getManagementService()
                .createBatchPartBuilder(batch)
                .type(DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE)
                .searchKey(batchPart.getId())
                .searchKey2(batchPart.getSearchKey())
                .status(DeleteProcessInstanceBatchConstants.STATUS_WAITING)
                .create();

        resultNode.put("deleteBatchPart", batchPartForDelete.getId());
        if (sequentialExecution) {
            resultNode.put("sequential", true);
            // If the computation was sequential we need to schedule the next job
            List<BatchPart> nextComputeParts = engineConfiguration.getManagementService()
                    .createBatchPartQuery()
                    .batchId(batch.getId())
                    .status(DeleteProcessInstanceBatchConstants.STATUS_WAITING)
                    .type(DeleteProcessInstanceBatchConstants.BATCH_PART_COMPUTE_IDS_TYPE)
                    .listPage(0, 2);

            // We are only going to start deletion if the batch is not failed
            boolean startDeletion = !DeleteProcessInstanceBatchConstants.STATUS_FAILED.equals(batch.getStatus());

            for (BatchPart nextComputePart : nextComputeParts) {
                if (!nextComputePart.getId().equals(batchPart.getId())) {
                    startDeletion = false;
                    JobService jobService = engineConfiguration.getJobServiceConfiguration().getJobService();

                    JobEntity nextComputeJob = jobService.createJob();
                    nextComputeJob.setJobHandlerType(ComputeDeleteHistoricProcessInstanceIdsJobHandler.TYPE);
                    nextComputeJob.setJobHandlerConfiguration(nextComputePart.getId());
                    jobService.createAsyncJob(nextComputeJob, false);
                    jobService.scheduleAsyncJob(nextComputeJob);
                    break;
                }
            }

            if (startDeletion) {
                JobService jobService = engineConfiguration.getJobServiceConfiguration().getJobService();
                JobEntity nextDeleteJob = jobService.createJob();
                nextDeleteJob.setJobHandlerType(DeleteHistoricProcessInstanceIdsJobHandler.TYPE);
                nextDeleteJob.setJobHandlerConfiguration(batchPartForDelete.getId());
                jobService.createAsyncJob(nextDeleteJob, false);
                jobService.scheduleAsyncJob(nextDeleteJob);
            }
        }

        batchService.completeBatchPart(batchPart.getId(), DeleteProcessInstanceBatchConstants.STATUS_COMPLETED, resultNode.toString());
    }

    protected void failBatchPart(ProcessEngineConfigurationImpl engineConfiguration, BatchService batchService, BatchPart batchPart, Batch batch,
            String resultJson, boolean sequentialExecution) {
        batchService.completeBatchPart(batchPart.getId(), DeleteProcessInstanceBatchConstants.STATUS_FAILED, resultJson);
        if (sequentialExecution) {
            completeBatch(batch, DeleteProcessInstanceBatchConstants.STATUS_FAILED, engineConfiguration);
        }
    }

    protected void completeBatch(Batch batch, String status, ProcessEngineConfigurationImpl engineConfiguration) {
        engineConfiguration.getBatchServiceConfiguration()
                .getBatchService()
                .completeBatch(batch.getId(), status);
    }



}
