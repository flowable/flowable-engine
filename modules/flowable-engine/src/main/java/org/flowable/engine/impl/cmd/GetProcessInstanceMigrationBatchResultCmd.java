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
import java.util.stream.Collectors;

import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.ProcessMigrationBatchEntity;
import org.flowable.engine.impl.persistence.entity.ProcessMigrationBatchEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.runtime.ProcessMigrationBatch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Dennis Federico
 */
public class GetProcessInstanceMigrationBatchResultCmd implements Command<String> {

    public static String BATCH_ID_JSON_LABEL = "batchId";
    public static String BATCH_STATUS_JSON_LABEL = "status";
    public static String BATCH_RESULT_JSON_LABEL = "result";
    public static String BATCH_NUM_PARTS_JSON_LABEL = "numParts";
    public static String BATCH_PARTS_JSON_LABEL = "parts";
    public static String BATCH_PROCESS_INSTANCE_ID_JSON_LABEL = "processInstanceId";
    public static String BATCH_STATUS_COMPLETED_VALUE = "completed";
    public static String BATCH_STATUS_IN_PROGRESS_VALUE = "inProgress";
    public String batchId;

    public GetProcessInstanceMigrationBatchResultCmd(String batchId) {
        this.batchId = batchId;
    }

    @Override
    public String execute(CommandContext commandContext) {

        ProcessMigrationBatchEntityManager batchManager = CommandContextUtil.getProcessEngineConfiguration(commandContext).getProcessMigrationBatchEntityManager();
        ObjectMapper objectMapper = CommandContextUtil.getProcessEngineConfiguration(commandContext).getObjectMapper();
        ProcessMigrationBatchEntity batch = batchManager.findById(batchId);

        ObjectNode rootNode = createJsonNodeFromBatch(batch, objectMapper);

        if (batch.getBatchChildren() != null && !batch.getBatchChildren().isEmpty()) {
            rootNode.put(BATCH_NUM_PARTS_JSON_LABEL, batch.getBatchChildren().size());

            List<ObjectNode> childNodes = batch.getBatchChildren().stream()
                .map(childNode -> createJsonNodeFromBatch(childNode, objectMapper))
                .collect(Collectors.toList());

            ArrayNode childrenNode = objectMapper.createArrayNode().addAll(childNodes);
            rootNode.set(BATCH_PARTS_JSON_LABEL, childrenNode);
        }
        return rootNode.toString();
    }

    protected ObjectNode createJsonNodeFromBatch(ProcessMigrationBatch childBatch, ObjectMapper objectMapper) {
        ObjectNode childNode = objectMapper.createObjectNode();
        childNode.put(BATCH_ID_JSON_LABEL, childBatch.getId());
        if (childBatch.getProcessInstanceId() != null) {
            childNode.put(BATCH_PROCESS_INSTANCE_ID_JSON_LABEL, childBatch.getProcessInstanceId());
        }
        if (childBatch.getCompleteTime() != null) {
            childNode.put(BATCH_STATUS_JSON_LABEL, BATCH_STATUS_COMPLETED_VALUE);
            if (childBatch.getResult() != null) {
                JsonNode resultNode;
                try {
                    resultNode = objectMapper.readTree(childBatch.getResult());
                } catch (IOException e) {
                    resultNode = null;
                }

                if (resultNode != null) {
                    childNode.set(BATCH_RESULT_JSON_LABEL, resultNode);
                }
            }
        } else {
            childNode.put(BATCH_STATUS_JSON_LABEL, BATCH_STATUS_IN_PROGRESS_VALUE);
        }
        return childNode;
    }

}
