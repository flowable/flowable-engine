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

import java.util.List;

import org.flowable.engine.runtime.ProcessMigrationBatch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Dennis Federico
 */
public class GetProcessInstanceMigrationBatchValidationResultCmd extends AbstractGetProcessInstanceMigrationBatchResultCmd<List<String>> {

    protected String batchId;

    public GetProcessInstanceMigrationBatchValidationResultCmd(String batchId) {
        super(batchId);
    }

    @Override
    protected List<String> getResultFromBatch(ProcessMigrationBatch batch, JsonNode jsonNode, ObjectMapper objectMapper) {
        List<String> resultMessages = objectMapper.convertValue(jsonNode, new TypeReference<List<String>>() {

        });
        return resultMessages;
    }
}
