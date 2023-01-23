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

package org.flowable.cmmn.engine.impl.cmd;

import java.io.IOException;
import java.util.List;

import org.flowable.batch.api.Batch;
import org.flowable.batch.api.BatchPart;
import org.flowable.batch.api.BatchService;
import org.flowable.cmmn.api.migration.CaseInstanceBatchMigrationPartResult;
import org.flowable.cmmn.api.migration.CaseInstanceBatchMigrationResult;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Valentin Zickner
 */
public class GetCaseInstanceMigrationBatchResultCmd implements Command<CaseInstanceBatchMigrationResult> {

    protected static final String BATCH_RESULT_MESSAGE_LABEL = "resultMessage";
    protected String migrationBatchId;

    public GetCaseInstanceMigrationBatchResultCmd(String migrationBatchId) {
        this.migrationBatchId = migrationBatchId;
    }

    @Override
    public CaseInstanceBatchMigrationResult execute(CommandContext commandContext) {
        CmmnEngineConfiguration engineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        BatchService batchService = engineConfiguration.getBatchServiceConfiguration().getBatchService();
        Batch batch = batchService.getBatch(migrationBatchId);

        if (batch != null) {
            ObjectMapper objectMapper = engineConfiguration.getObjectMapper();
            CaseInstanceBatchMigrationResult result = convertFromBatch(batch, objectMapper);
            List<BatchPart> batchParts = batchService.findBatchPartsByBatchId(batch.getId());
            if (batchParts != null && !batchParts.isEmpty()) {
                for (BatchPart batchPart : batchParts) {
                    result.addMigrationPart(convertFromBatchPart(batchPart, objectMapper, engineConfiguration));
                }
            }
            return result;
        }

        return null;
    }

    protected CaseInstanceBatchMigrationResult convertFromBatch(Batch batch, ObjectMapper objectMapper) {
        CaseInstanceBatchMigrationResult result = new CaseInstanceBatchMigrationResult();

        result.setBatchId(batch.getId());
        result.setSourceCaseDefinitionId(batch.getBatchSearchKey());
        result.setTargetCaseDefinitionId(batch.getBatchSearchKey2());
        result.setStatus(batch.getStatus());
        result.setCompleteTime(batch.getCompleteTime());

        return result;
    }

    protected CaseInstanceBatchMigrationPartResult convertFromBatchPart(BatchPart batchPart, ObjectMapper objectMapper,
            CmmnEngineConfiguration engineConfiguration) {
        CaseInstanceBatchMigrationPartResult partResult = new CaseInstanceBatchMigrationPartResult();

        partResult.setBatchId(batchPart.getId());
        partResult.setCaseInstanceId(batchPart.getScopeId());
        partResult.setSourceCaseDefinitionId(batchPart.getBatchSearchKey());
        partResult.setTargetCaseDefinitionId(batchPart.getBatchSearchKey2());

        if (batchPart.getCompleteTime() != null) {
            partResult.setStatus(CaseInstanceBatchMigrationResult.STATUS_COMPLETED);
        }

        partResult.setResult(batchPart.getStatus());
        if (CaseInstanceBatchMigrationResult.RESULT_FAIL.equals(batchPart.getStatus()) &&
                batchPart.getResultDocumentJson(engineConfiguration.getEngineCfgKey()) != null) {

            try {
                JsonNode resultNode = objectMapper.readTree(batchPart.getResultDocumentJson(engineConfiguration.getEngineCfgKey()));
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
