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

package org.flowable.engine.impl.cmd;

import java.io.IOException;
import java.util.List;

import org.flowable.batch.api.Batch;
import org.flowable.batch.api.BatchPart;
import org.flowable.batch.api.BatchService;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.migration.ProcessInstanceBatchMigrationPartResult;
import org.flowable.engine.migration.ProcessInstanceBatchMigrationResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GetProcessInstanceMigrationBatchResultCmd implements Command<ProcessInstanceBatchMigrationResult> {

    public static final String BATCH_RESULT_STATUS_LABEL = "resultStatus";
    public static final String BATCH_RESULT_MESSAGE_LABEL = "resultMessage";

    protected String batchId;

    public GetProcessInstanceMigrationBatchResultCmd(String batchId) {
        this.batchId = batchId;
    }

    @Override
    public ProcessInstanceBatchMigrationResult execute(CommandContext commandContext) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        BatchService batchService = processEngineConfiguration.getBatchServiceConfiguration().getBatchService();
        Batch batch = batchService.getBatch(batchId);

        if (batch != null) {
            ObjectMapper objectMapper = processEngineConfiguration.getObjectMapper();
            ProcessInstanceBatchMigrationResult result = convertFromBatch(batch, objectMapper);
            List<BatchPart> batchParts = batchService.findBatchPartsByBatchId(batch.getId());
            if (batchParts != null && !batchParts.isEmpty()) {
                for (BatchPart batchPart : batchParts) {
                    result.addMigrationPart(convertFromBatchPart(batchPart, objectMapper));
                }
            }
            return result;
        }
        
        return null;
    }

    protected ProcessInstanceBatchMigrationResult convertFromBatch(Batch batch, ObjectMapper objectMapper) {
        ProcessInstanceBatchMigrationResult result = new ProcessInstanceBatchMigrationResult();

        result.setBatchId(batch.getId());
        result.setSourceProcessDefinitionId(batch.getBatchSearchKey());
        result.setTargetProcessDefinitionId(batch.getBatchSearchKey2());
        result.setStatus(batch.getStatus());
        result.setCompleteTime(batch.getCompleteTime());

        return result;
    }

    protected ProcessInstanceBatchMigrationPartResult convertFromBatchPart(BatchPart batchPart, ObjectMapper objectMapper) {
        ProcessInstanceBatchMigrationPartResult partResult = new ProcessInstanceBatchMigrationPartResult();

        partResult.setBatchId(batchPart.getId());
        partResult.setProcessInstanceId(batchPart.getScopeId());
        partResult.setSourceProcessDefinitionId(batchPart.getBatchSearchKey());
        partResult.setTargetProcessDefinitionId(batchPart.getBatchSearchKey2());

        if (batchPart.getCompleteTime() != null) {
            partResult.setStatus(ProcessInstanceBatchMigrationResult.STATUS_COMPLETED);
        }
        
        partResult.setResult(batchPart.getStatus());
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        if (ProcessInstanceBatchMigrationResult.RESULT_FAIL.equals(batchPart.getStatus()) && 
                batchPart.getResultDocumentJson(processEngineConfiguration.getEngineCfgKey()) != null) {
            
            try {
                JsonNode resultNode = objectMapper.readTree(batchPart.getResultDocumentJson(processEngineConfiguration.getEngineCfgKey()));
                if (resultNode.has(BATCH_RESULT_MESSAGE_LABEL)) {
                    String resultMessage = resultNode.get(BATCH_RESULT_MESSAGE_LABEL).asText();
                    partResult.setMigrationMessage(resultMessage);
                }
                
            } catch (IOException e) {
                throw new FlowableException("Error reading batch part " + batchPart.getId());
            }
        }
        
        return partResult;
    }
}
