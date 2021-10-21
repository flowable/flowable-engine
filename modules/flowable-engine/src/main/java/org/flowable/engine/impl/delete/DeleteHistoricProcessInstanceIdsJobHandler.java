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
import org.flowable.batch.api.BatchPart;
import org.flowable.batch.api.BatchService;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.common.engine.impl.util.ExceptionUtil;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.job.service.JobHandler;
import org.flowable.job.service.JobService;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.variable.api.delegate.VariableScope;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Filip Hrisafov
 */
public class DeleteHistoricProcessInstanceIdsJobHandler implements JobHandler {

    public static final String TYPE = "delete-historic-process-ids";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void execute(JobEntity job, String configuration, VariableScope variableScope, CommandContext commandContext) {
        ProcessEngineConfigurationImpl engineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        BatchService batchService = engineConfiguration.getBatchServiceConfiguration().getBatchService();

        BatchPart batchPart = batchService.getBatchPart(configuration);
        if (batchPart == null) {
            throw new FlowableIllegalArgumentException("There is no batch part with the id " + configuration);
        }

        ManagementService managementService = engineConfiguration.getManagementService();

        BatchPart computeBatchPart = managementService.createBatchPartQuery()
                .id(batchPart.getSearchKey())
                .singleResult();

        JsonNode computeBatchPartResult = getBatchPartResult(computeBatchPart, engineConfiguration);
        JsonNode idsToDelete = computeBatchPartResult.path("processInstanceIdsToDelete");
        Set<String> processInstanceIdsToDelete = new HashSet<>();

        if (idsToDelete.isArray()) {
            for (JsonNode idNode : idsToDelete) {
                processInstanceIdsToDelete.add(idNode.textValue());
            }
        }

        if (processInstanceIdsToDelete.isEmpty()) {
            throw new FlowableIllegalArgumentException("There are no process instance ids to delete");
        }

        HistoryService historyService = engineConfiguration.getHistoryService();
        List<HistoricProcessInstance> processInstances = historyService
                .createHistoricProcessInstanceQuery()
                .processInstanceIds(processInstanceIdsToDelete)
                .list();

        String status = DeleteProcessInstanceBatchConstants.STATUS_COMPLETED;
        ObjectNode resultNode = engineConfiguration.getObjectMapper().createObjectNode();

        for (HistoricProcessInstance processInstance : processInstances) {
            try {
                historyService.deleteHistoricProcessInstance(processInstance.getId());
                resultNode.withArray("processInstanceIdsDeleted")
                        .add(processInstance.getId());
            } catch (FlowableException ex) {
                status = DeleteProcessInstanceBatchConstants.STATUS_FAILED;
                resultNode.withArray("processInstanceIdsFailedToDelete")
                        .addObject()
                        .put("id", processInstance.getId())
                        .put("error", ex.getMessage())
                        .put("stacktrace", ExceptionUtils.getStackTrace(ex));
            }
        }

        batchService.completeBatchPart(batchPart.getId(), status, resultNode.toString());

        if (computeBatchPartResult.path("sequential").booleanValue()) {
            // If the computation was sequential we need to schedule the next job
            List<BatchPart> nextDeleteParts = engineConfiguration.getManagementService()
                    .createBatchPartQuery()
                    .batchId(batchPart.getBatchId())
                    .status(DeleteProcessInstanceBatchConstants.STATUS_WAITING)
                    .type(DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE)
                    .listPage(0, 2);

            boolean completeBatch = true;

            for (BatchPart nextDeletePart : nextDeleteParts) {
                if (!nextDeletePart.getId().equals(batchPart.getId())) {
                    completeBatch = false;
                    JobService jobService = engineConfiguration.getJobServiceConfiguration().getJobService();

                    JobEntity nextDeleteJob = jobService.createJob();
                    nextDeleteJob.setJobHandlerType(DeleteHistoricProcessInstanceIdsJobHandler.TYPE);
                    nextDeleteJob.setJobHandlerConfiguration(nextDeletePart.getId());
                    jobService.createAsyncJob(nextDeleteJob, false);
                    jobService.scheduleAsyncJob(nextDeleteJob);
                    break;
                }
            }

            if (completeBatch) {
                JobService jobService = engineConfiguration.getJobServiceConfiguration().getJobService();
                JobEntity nextDeleteJob = jobService.createJob();
                nextDeleteJob.setJobHandlerType(ComputeDeleteHistoricProcessInstanceStatusJobHandler.TYPE);
                nextDeleteJob.setJobHandlerConfiguration(batchPart.getBatchId());
                jobService.createAsyncJob(nextDeleteJob, false);
                jobService.scheduleAsyncJob(nextDeleteJob);
            }
        }

    }

    protected JsonNode getBatchPartResult(BatchPart batchPart, ProcessEngineConfigurationImpl engineConfiguration) {
        try {
            return engineConfiguration.getObjectMapper()
                    .readTree(batchPart.getResultDocumentJson(EngineConfigurationConstants.KEY_PROCESS_ENGINE_CONFIG));
        } catch (JsonProcessingException e) {
            ExceptionUtil.sneakyThrow(e);
            return null;
        }
    }
}
