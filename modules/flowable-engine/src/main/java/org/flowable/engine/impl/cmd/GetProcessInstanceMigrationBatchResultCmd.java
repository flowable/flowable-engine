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

import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.jobexecutor.ProcessInstanceMigrationJobHandler;
import org.flowable.engine.impl.migration.ProcessInstanceMigrationResultImpl;
import org.flowable.engine.impl.persistence.entity.ProcessMigrationBatchEntity;
import org.flowable.engine.impl.persistence.entity.ProcessMigrationBatchEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.migration.ProcessInstanceMigrationResult;
import org.flowable.engine.runtime.ProcessMigrationBatch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Dennis Federico
 */
public class GetProcessInstanceMigrationBatchResultCmd implements Command<ProcessInstanceMigrationResult> {

    public String batchId;

    public GetProcessInstanceMigrationBatchResultCmd(String batchId) {
        this.batchId = batchId;
    }

    @Override
    public ProcessInstanceMigrationResult execute(CommandContext commandContext) {

        ProcessMigrationBatchEntityManager batchManager = CommandContextUtil.getProcessEngineConfiguration(commandContext).getProcessMigrationBatchEntityManager();
        ProcessMigrationBatchEntity batch = batchManager.findById(batchId);

        if (batch != null) {
            ObjectMapper objectMapper = CommandContextUtil.getProcessEngineConfiguration(commandContext).getObjectMapper();
            ProcessInstanceMigrationResultImpl result = convertFromBatch(batch, objectMapper);
            if (batch.getBatchChildren() != null) {
                batch.getBatchChildren().stream()
                    .forEach(child -> result.addResultPart(convertFromBatch(child, objectMapper)));
            }
            return result;
        }
        return null;
    }

    protected ProcessInstanceMigrationResultImpl convertFromBatch(ProcessMigrationBatch batch, ObjectMapper objectMapper) {
        ProcessInstanceMigrationResultImpl result = new ProcessInstanceMigrationResultImpl();

        result.setBatchId(batch.getId());
        result.setProcessInstanceId(batch.getProcessInstanceId());

        if (batch.getCompleteTime() != null) {
            result.setStatus(ProcessInstanceMigrationResult.STATUS_COMPLETED);

            if (batch.getResult() != null) {
                try {
                    JsonNode resultNode = objectMapper.readTree(batch.getResult());
                    String resultStatus = null;
                    if (resultNode.has(ProcessInstanceMigrationJobHandler.RESULT_LABEL_MIGRATION_PROCESS)) {
                        resultStatus = resultNode.get(ProcessInstanceMigrationJobHandler.RESULT_LABEL_MIGRATION_PROCESS).asText();
                    }

                    String resultMessage = null;
                    if (resultNode.has(ProcessInstanceMigrationJobHandler.RESULT_LABEL_CAUSE)) {
                        resultMessage = resultNode.get(ProcessInstanceMigrationJobHandler.RESULT_LABEL_CAUSE).asText();
                    }

                    result.setResult(resultStatus, resultMessage);
                } catch (IOException e) {

                }
            }
        } else if (batch.getBatchChildren() == null) {
            result.setStatus(ProcessInstanceMigrationResult.STATUS_IN_PROGRESS);
        }
        return result;
    }
}
