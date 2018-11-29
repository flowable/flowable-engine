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
package org.flowable.engine.impl.jobexecutor;

import java.io.IOException;

import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.job.service.JobHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Dennis Federico
 */
public abstract class AbstractProcessInstanceMigrationJobHandler implements JobHandler {

    protected static final String CFG_LABEL_BATCH_ID = "batchId";

    protected static String getBatchIdFromHandlerCfg(String handlerCfg) {
        try {
            JsonNode cfgAsJson = getObjectMapper().readTree(handlerCfg);
            if (cfgAsJson.has(CFG_LABEL_BATCH_ID)) {
                return cfgAsJson.get(CFG_LABEL_BATCH_ID).asText();
            }
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    public static String getHandlerCfgForBatchId(String batchId) {
        ObjectNode handlerCfg = getObjectMapper().createObjectNode();
        handlerCfg.put(CFG_LABEL_BATCH_ID, batchId);
        return handlerCfg.toString();
    }

    protected static ObjectMapper getObjectMapper() {
        if (CommandContextUtil.getCommandContext() != null) {
            return CommandContextUtil.getProcessEngineConfiguration().getObjectMapper();
        } else {
            return new ObjectMapper();
        }
    }
}

