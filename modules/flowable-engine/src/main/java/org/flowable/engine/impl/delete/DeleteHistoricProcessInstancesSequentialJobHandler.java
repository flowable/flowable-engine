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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.flowable.batch.api.Batch;
import org.flowable.batch.api.BatchPart;
import org.flowable.batch.api.BatchService;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.HistoryService;
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
public class DeleteHistoricProcessInstancesSequentialJobHandler implements JobHandler {

    public static final String TYPE = "delete-historic-processes-sequential";

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
        if (config.hasError()) {
            failBatchPart(engineConfiguration, batchService, batchPart, batch, config.getError());
            return;
        }

        int batchSize = config.getBatchSize();

        HistoricProcessInstanceQuery query = config.getQuery();
        // In the synchronous deletion, we are always deleting the first elements
        List<HistoricProcessInstance> historicProcessInstances = query.listPage(0, batchSize);
        ObjectNode resultNode = engineConfiguration.getObjectMapper().createObjectNode();
        Set<String> processInstanceIdsToDelete = new HashSet<>();
        for (HistoricProcessInstance historicProcessInstance : historicProcessInstances) {
            processInstanceIdsToDelete.add(historicProcessInstance.getId());
        }

        if (processInstanceIdsToDelete.isEmpty()) {
            batchService.completeBatchPart(batchPart.getId(), DeleteProcessInstanceBatchConstants.STATUS_COMPLETED, resultNode.toString());
            completeBatch(batch, DeleteProcessInstanceBatchConstants.STATUS_COMPLETED, engineConfiguration);
            return;
        }

        String status = DeleteProcessInstanceBatchConstants.STATUS_COMPLETED;

        HistoryService historyService = engineConfiguration.getHistoryService();

        try {
            historyService.bulkDeleteHistoricProcessInstances(processInstanceIdsToDelete);
            ArrayNode deletedProcessInstanceIdsNode = resultNode.withArray("processInstanceIdsDeleted");
            processInstanceIdsToDelete.forEach(deletedProcessInstanceIdsNode::add);

        } catch (FlowableException ex) {
            status = DeleteProcessInstanceBatchConstants.STATUS_FAILED;
            ArrayNode processInstanceIdsFailedToDelete = resultNode.withArray("processInstanceIdsFailedToDelete");
            processInstanceIdsToDelete.forEach(processInstanceIdsFailedToDelete::add);
            resultNode.put("error", ex.getMessage());
            resultNode.put("stacktrace", ExceptionUtils.getStackTrace(ex));
        }

        batchService.completeBatchPart(batchPart.getId(), status, resultNode.toString());

        if (DeleteProcessInstanceBatchConstants.STATUS_FAILED.equals(status)) {
            completeBatch(batch, DeleteProcessInstanceBatchConstants.STATUS_FAILED, engineConfiguration);
        } else {
            // Create the next batch part and schedule a job for it
            BatchPart nextBatchPart = engineConfiguration.getManagementService()
                    .createBatchPartBuilder(batch)
                    .type(DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE)
                    .searchKey(String.valueOf(Integer.parseInt(batchPart.getSearchKey()) + 1))
                    .status(DeleteProcessInstanceBatchConstants.STATUS_WAITING)
                    .create();

            JobService jobService = engineConfiguration.getJobServiceConfiguration().getJobService();

            JobEntity nextJob = jobService.createJob();
            nextJob.setJobHandlerType(DeleteHistoricProcessInstancesSequentialJobHandler.TYPE);
            nextJob.setJobHandlerConfiguration(nextBatchPart.getId());
            jobService.createAsyncJob(nextJob, false);
            jobService.scheduleAsyncJob(nextJob);
        }
    }

    protected void failBatchPart(ProcessEngineConfigurationImpl engineConfiguration, BatchService batchService, BatchPart batchPart, Batch batch,
            String resultJson) {
        batchService.completeBatchPart(batchPart.getId(), DeleteProcessInstanceBatchConstants.STATUS_FAILED, resultJson);
        completeBatch(batch, DeleteProcessInstanceBatchConstants.STATUS_FAILED, engineConfiguration);
    }

    protected void completeBatch(Batch batch, String status, ProcessEngineConfigurationImpl engineConfiguration) {
        engineConfiguration.getBatchServiceConfiguration()
                .getBatchService()
                .completeBatch(batch.getId(), status);
    }
}
