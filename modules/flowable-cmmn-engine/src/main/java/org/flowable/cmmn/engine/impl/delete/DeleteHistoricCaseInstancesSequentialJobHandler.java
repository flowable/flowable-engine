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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.flowable.batch.api.Batch;
import org.flowable.batch.api.BatchPart;
import org.flowable.batch.api.BatchService;
import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.history.HistoricCaseInstanceQuery;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableException;
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
public class DeleteHistoricCaseInstancesSequentialJobHandler implements JobHandler {

    public static final String TYPE = "delete-historic-cases-sequential";

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
        Batch batch = config.getBatch();
        if (config.hasError()) {
            failBatchPart(engineConfiguration, batchService, batchPart, batch, config.getError());
            return;
        }

        if (DeleteCaseInstanceBatchConstants.STATUS_STOPPED.equals(batch.getStatus())) {
            batchService.completeBatchPart(batchPart.getId(), DeleteCaseInstanceBatchConstants.STATUS_STOPPED, null);
            return;
        }

        int batchSize = config.getBatchSize();

        HistoricCaseInstanceQuery query = config.getQuery();
        // In the synchronous deletion, we are always deleting the first elements
        List<HistoricCaseInstance> historicCaseInstances = query.listPage(0, batchSize);
        ObjectNode resultNode = engineConfiguration.getObjectMapper().createObjectNode();
        Set<String> caseInstanceIdsToDelete = new HashSet<>();
        for (HistoricCaseInstance historicCaseInstance : historicCaseInstances) {
            caseInstanceIdsToDelete.add(historicCaseInstance.getId());
        }

        if (caseInstanceIdsToDelete.isEmpty()) {
            batchService.completeBatchPart(batchPart.getId(), DeleteCaseInstanceBatchConstants.STATUS_COMPLETED, resultNode.toString());
            completeBatch(batch, DeleteCaseInstanceBatchConstants.STATUS_COMPLETED, engineConfiguration);
            return;
        }

        String status = DeleteCaseInstanceBatchConstants.STATUS_COMPLETED;

        CmmnHistoryService historyService = engineConfiguration.getCmmnHistoryService();

        try {
            historyService.bulkDeleteHistoricCaseInstances(caseInstanceIdsToDelete);
            ArrayNode deletedCaseInstanceIdsNode = resultNode.withArray("caseInstanceIdsDeleted");
            caseInstanceIdsToDelete.forEach(deletedCaseInstanceIdsNode::add);

        } catch (FlowableException ex) {
            status = DeleteCaseInstanceBatchConstants.STATUS_FAILED;
            ArrayNode caseInstanceIdsFailedToDelete = resultNode.withArray("caseInstanceIdsFailedToDelete");
            caseInstanceIdsToDelete.forEach(caseInstanceIdsFailedToDelete::add);
            resultNode.put("error", ex.getMessage());
            resultNode.put("stacktrace", ExceptionUtils.getStackTrace(ex));
        }

        batchService.completeBatchPart(batchPart.getId(), status, resultNode.toString());

        if (DeleteCaseInstanceBatchConstants.STATUS_FAILED.equals(status)) {
            completeBatch(batch, DeleteCaseInstanceBatchConstants.STATUS_FAILED, engineConfiguration);
        } else {
            // Create the next batch part and schedule a job for it
            BatchPart nextBatchPart = engineConfiguration.getCmmnManagementService()
                    .createBatchPartBuilder(batch)
                    .type(DeleteCaseInstanceBatchConstants.BATCH_PART_DELETE_CASE_INSTANCES_TYPE)
                    .searchKey(String.valueOf(Integer.parseInt(batchPart.getSearchKey()) + 1))
                    .status(DeleteCaseInstanceBatchConstants.STATUS_WAITING)
                    .create();

            JobService jobService = engineConfiguration.getJobServiceConfiguration().getJobService();

            JobEntity nextJob = jobService.createJob();
            nextJob.setJobHandlerType(DeleteHistoricCaseInstancesSequentialJobHandler.TYPE);
            nextJob.setJobHandlerConfiguration(nextBatchPart.getId());
            jobService.createAsyncJob(nextJob, false);
            jobService.scheduleAsyncJob(nextJob);
        }
    }

    protected void failBatchPart(CmmnEngineConfiguration engineConfiguration, BatchService batchService, BatchPart batchPart, Batch batch,
            String resultJson) {
        batchService.completeBatchPart(batchPart.getId(), DeleteCaseInstanceBatchConstants.STATUS_FAILED, resultJson);
        completeBatch(batch, DeleteCaseInstanceBatchConstants.STATUS_FAILED, engineConfiguration);
    }

    protected void completeBatch(Batch batch, String status, CmmnEngineConfiguration engineConfiguration) {
        engineConfiguration.getBatchServiceConfiguration()
                .getBatchService()
                .completeBatch(batch.getId(), status);
    }
}
