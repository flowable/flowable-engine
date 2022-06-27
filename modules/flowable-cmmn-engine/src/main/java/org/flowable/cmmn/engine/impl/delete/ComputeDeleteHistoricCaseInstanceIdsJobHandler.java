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
package org.flowable.cmmn.engine.impl.delete;

import java.util.List;

import org.flowable.batch.api.Batch;
import org.flowable.batch.api.BatchPart;
import org.flowable.batch.api.BatchService;
import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.history.HistoricCaseInstanceQuery;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.service.JobHandler;
import org.flowable.job.service.JobService;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.variable.api.delegate.VariableScope;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Filip Hrisafov
 */
public class ComputeDeleteHistoricCaseInstanceIdsJobHandler implements JobHandler {

    public static final String TYPE = "compute-delete-historic-case-ids";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void execute(JobEntity job, String configuration, VariableScope variableScope, CommandContext commandContext) {
        CmmnEngineConfiguration engineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        BatchService batchService = engineConfiguration.getBatchServiceConfiguration().getBatchService();

        BatchDeleteCaseConfig config = BatchDeleteCaseConfig.create(configuration, engineConfiguration);
        BatchPart batchPart = config.getBatchPart();
        boolean sequentialExecution = config.isSequentialExecution();
        Batch batch = config.getBatch();
        if (config.hasError()) {
            failBatchPart(engineConfiguration, batchService, batchPart, batch, config.getError(), sequentialExecution);
            return;
        }

        int batchSize = config.getBatchSize();
        int batchPartNumber = Integer.parseInt(batchPart.getSearchKey());
        // The first result is the batch part number multiplied by the batch size
        // e.g. if this is the 5th batch part (batch part number 4) and the batch size is 100 the first result should start from 400
        int firstResult = batchPartNumber * batchSize;

        HistoricCaseInstanceQuery query = config.getQuery();
        List<HistoricCaseInstance> caseInstances = query.listPage(firstResult, batchSize);

        ObjectNode resultNode = engineConfiguration.getObjectMapper().createObjectNode();
        ArrayNode idsToDelete = resultNode.putArray("caseInstanceIdsToDelete");
        for (HistoricCaseInstance caseInstance : caseInstances) {
            idsToDelete.add(caseInstance.getId());
        }

        BatchPart batchPartForDelete = engineConfiguration.getCmmnManagementService()
                .createBatchPartBuilder(batch)
                .type(DeleteCaseInstanceBatchConstants.BATCH_PART_DELETE_CASE_INSTANCES_TYPE)
                .searchKey(batchPart.getId())
                .searchKey2(batchPart.getSearchKey())
                .status(DeleteCaseInstanceBatchConstants.STATUS_WAITING)
                .create();

        resultNode.put("deleteBatchPart", batchPartForDelete.getId());
        if (sequentialExecution) {
            resultNode.put("sequential", true);
            // If the computation was sequential we need to schedule the next job
            List<BatchPart> nextComputeParts = engineConfiguration.getCmmnManagementService()
                    .createBatchPartQuery()
                    .batchId(batch.getId())
                    .status(DeleteCaseInstanceBatchConstants.STATUS_WAITING)
                    .type(DeleteCaseInstanceBatchConstants.BATCH_PART_COMPUTE_IDS_TYPE)
                    .listPage(0, 2);

            // We are only going to start deletion if the batch is not failed
            boolean startDeletion = !DeleteCaseInstanceBatchConstants.STATUS_FAILED.equals(batch.getStatus());

            for (BatchPart nextComputePart : nextComputeParts) {
                if (!nextComputePart.getId().equals(batchPart.getId())) {
                    startDeletion = false;
                    JobService jobService = engineConfiguration.getJobServiceConfiguration().getJobService();

                    JobEntity nextComputeJob = jobService.createJob();
                    nextComputeJob.setJobHandlerType(ComputeDeleteHistoricCaseInstanceIdsJobHandler.TYPE);
                    nextComputeJob.setJobHandlerConfiguration(nextComputePart.getId());
                    nextComputeJob.setScopeType(ScopeTypes.CMMN);
                    jobService.createAsyncJob(nextComputeJob, false);
                    jobService.scheduleAsyncJob(nextComputeJob);
                    break;
                }
            }

            if (startDeletion) {
                JobService jobService = engineConfiguration.getJobServiceConfiguration().getJobService();
                JobEntity nextDeleteJob = jobService.createJob();
                nextDeleteJob.setJobHandlerType(DeleteHistoricCaseInstanceIdsJobHandler.TYPE);
                nextDeleteJob.setJobHandlerConfiguration(batchPartForDelete.getId());
                nextDeleteJob.setScopeType(ScopeTypes.CMMN);
                jobService.createAsyncJob(nextDeleteJob, false);
                jobService.scheduleAsyncJob(nextDeleteJob);
            }
        }

        batchService.completeBatchPart(batchPart.getId(), DeleteCaseInstanceBatchConstants.STATUS_COMPLETED, resultNode.toString());
    }

    protected void failBatchPart(CmmnEngineConfiguration engineConfiguration, BatchService batchService, BatchPart batchPart, Batch batch,
            String resultJson, boolean sequentialExecution) {
        batchService.completeBatchPart(batchPart.getId(), DeleteCaseInstanceBatchConstants.STATUS_FAILED, resultJson);
        if (sequentialExecution) {
            completeBatch(batch, DeleteCaseInstanceBatchConstants.STATUS_FAILED, engineConfiguration);
        }
    }

    protected void completeBatch(Batch batch, String status, CmmnEngineConfiguration engineConfiguration) {
        engineConfiguration.getBatchServiceConfiguration()
                .getBatchService()
                .completeBatch(batch.getId(), status);
    }

}
