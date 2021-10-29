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
import org.flowable.batch.api.BatchPart;
import org.flowable.batch.api.BatchService;
import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.CmmnManagementService;
import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.common.engine.impl.util.ExceptionUtil;
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
public class DeleteHistoricCaseInstanceIdsJobHandler implements JobHandler {

    public static final String TYPE = "delete-historic-case-ids";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void execute(JobEntity job, String configuration, VariableScope variableScope, CommandContext commandContext) {
        CmmnEngineConfiguration engineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        BatchService batchService = engineConfiguration.getBatchServiceConfiguration().getBatchService();

        BatchPart batchPart = batchService.getBatchPart(configuration);
        if (batchPart == null) {
            throw new FlowableIllegalArgumentException("There is no batch part with the id " + configuration);
        }

        CmmnManagementService managementService = engineConfiguration.getCmmnManagementService();

        BatchPart computeBatchPart = managementService.createBatchPartQuery()
                .id(batchPart.getSearchKey())
                .singleResult();

        JsonNode computeBatchPartResult = getBatchPartResult(computeBatchPart, engineConfiguration);
        JsonNode idsToDelete = computeBatchPartResult.path("caseInstanceIdsToDelete");
        Set<String> caseInstanceIdsToDelete = new HashSet<>();

        if (idsToDelete.isArray()) {
            for (JsonNode idNode : idsToDelete) {
                caseInstanceIdsToDelete.add(idNode.textValue());
            }
        }

        if (caseInstanceIdsToDelete.isEmpty()) {
            throw new FlowableIllegalArgumentException("There are no case instance ids to delete");
        }

        CmmnHistoryService historyService = engineConfiguration.getCmmnHistoryService();
        List<HistoricCaseInstance> caseInstances = historyService
                .createHistoricCaseInstanceQuery()
                .caseInstanceIds(caseInstanceIdsToDelete)
                .list();

        String status = DeleteCaseInstanceBatchConstants.STATUS_COMPLETED;
        ObjectNode resultNode = engineConfiguration.getObjectMapper().createObjectNode();

        for (HistoricCaseInstance caseInstance : caseInstances) {
            try {
                historyService.deleteHistoricCaseInstance(caseInstance.getId());
                resultNode.withArray("caseInstanceIdsDeleted")
                        .add(caseInstance.getId());
            } catch (FlowableException ex) {
                status = DeleteCaseInstanceBatchConstants.STATUS_FAILED;
                resultNode.withArray("caseInstanceIdsFailedToDelete")
                        .addObject()
                        .put("id", caseInstance.getId())
                        .put("error", ex.getMessage())
                        .put("stacktrace", ExceptionUtils.getStackTrace(ex));
            }
        }

        batchService.completeBatchPart(batchPart.getId(), status, resultNode.toString());

        if (computeBatchPartResult.path("sequential").booleanValue()) {
            // If the computation was sequential we need to schedule the next job
            List<BatchPart> nextDeleteParts = engineConfiguration.getCmmnManagementService()
                    .createBatchPartQuery()
                    .batchId(batchPart.getBatchId())
                    .status(DeleteCaseInstanceBatchConstants.STATUS_WAITING)
                    .type(DeleteCaseInstanceBatchConstants.BATCH_PART_DELETE_CASE_INSTANCES_TYPE)
                    .listPage(0, 2);

            boolean completeBatch = true;

            for (BatchPart nextDeletePart : nextDeleteParts) {
                if (!nextDeletePart.getId().equals(batchPart.getId())) {
                    completeBatch = false;
                    JobService jobService = engineConfiguration.getJobServiceConfiguration().getJobService();

                    JobEntity nextDeleteJob = jobService.createJob();
                    nextDeleteJob.setJobHandlerType(DeleteHistoricCaseInstanceIdsJobHandler.TYPE);
                    nextDeleteJob.setJobHandlerConfiguration(nextDeletePart.getId());
                    nextDeleteJob.setScopeType(ScopeTypes.CMMN);
                    jobService.createAsyncJob(nextDeleteJob, false);
                    jobService.scheduleAsyncJob(nextDeleteJob);
                    break;
                }
            }

            if (completeBatch) {
                JobService jobService = engineConfiguration.getJobServiceConfiguration().getJobService();
                JobEntity nextDeleteJob = jobService.createJob();
                nextDeleteJob.setJobHandlerType(ComputeDeleteHistoricCaseInstanceStatusJobHandler.TYPE);
                nextDeleteJob.setJobHandlerConfiguration(batchPart.getBatchId());
                nextDeleteJob.setScopeType(ScopeTypes.CMMN);
                jobService.createAsyncJob(nextDeleteJob, false);
                jobService.scheduleAsyncJob(nextDeleteJob);
            }
        }

    }

    protected JsonNode getBatchPartResult(BatchPart batchPart, CmmnEngineConfiguration engineConfiguration) {
        try {
            return engineConfiguration.getObjectMapper()
                    .readTree(batchPart.getResultDocumentJson(EngineConfigurationConstants.KEY_PROCESS_ENGINE_CONFIG));
        } catch (JsonProcessingException e) {
            ExceptionUtil.sneakyThrow(e);
            return null;
        }
    }
}
