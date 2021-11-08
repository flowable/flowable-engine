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
import org.flowable.batch.api.BatchPartQuery;
import org.flowable.batch.service.impl.persistence.entity.BatchEntity;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.ManagementService;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.job.service.JobHandler;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.variable.api.delegate.VariableScope;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

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

        long totalBatchParts = createStatusQuery(batch, managementService).count();
        long totalCompleted = createStatusQuery(batch, managementService).completed().count();

        if (totalBatchParts == totalCompleted) {
            List<BatchPart> failedParts = createStatusQuery(batch, managementService)
                    .status(DeleteProcessInstanceBatchConstants.STATUS_FAILED)
                    .list();
            long totalFailed = failedParts.size();
            if (totalFailed == 0) {
                completeBatch(batch, DeleteProcessInstanceBatchConstants.STATUS_COMPLETED, engineConfiguration);
            } else {
                completeBatchFail(batch, failedParts, engineConfiguration);
            }

            job.setRepeat(null);
        } else if (totalBatchParts == 0) {
            completeBatch(batch, DeleteProcessInstanceBatchConstants.STATUS_COMPLETED, engineConfiguration);
            job.setRepeat(null);
        }

    }

    protected BatchPartQuery createStatusQuery(Batch batch, ManagementService managementService) {
        return managementService.createBatchPartQuery()
                .batchId(batch.getId())
                .type(DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE);
    }

    protected void completeBatch(Batch batch, String status, ProcessEngineConfigurationImpl engineConfiguration) {
        engineConfiguration.getBatchServiceConfiguration()
                .getBatchService()
                .completeBatch(batch.getId(), status);
    }

    protected void completeBatchFail(Batch batch, List<BatchPart> failedParts, ProcessEngineConfigurationImpl engineConfiguration) {
        completeBatch(batch, DeleteProcessInstanceBatchConstants.STATUS_FAILED, engineConfiguration);

        long totalFailedInstances = 0;

        ObjectMapper objectMapper = engineConfiguration.getObjectMapper();
        for (BatchPart failedPart : failedParts) {
            JsonNode node = readJson(failedPart.getResultDocumentJson(ScopeTypes.BPMN), objectMapper);
            if (node != null) {
                totalFailedInstances += node.path("processInstanceIdsFailedToDelete").size();
            }
        }

        ObjectNode batchDocument = (ObjectNode) readJson(batch.getBatchDocumentJson(ScopeTypes.BPMN), objectMapper);
        batchDocument.put("numberOfFailedInstances", totalFailedInstances);

        ((BatchEntity) batch).setBatchDocumentJson(batchDocument.toString(), ScopeTypes.BPMN);
    }

    protected JsonNode readJson(String json, ObjectMapper objectMapper) {
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new FlowableException("Failed to read json", e);
        }
    }
}
