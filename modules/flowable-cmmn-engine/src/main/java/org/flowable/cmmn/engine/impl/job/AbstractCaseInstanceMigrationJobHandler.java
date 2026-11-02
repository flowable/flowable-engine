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
package org.flowable.cmmn.engine.impl.job;

import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.job.service.JobHandler;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

public abstract class AbstractCaseInstanceMigrationJobHandler implements JobHandler {

    public static final String BATCH_RESULT_STATUS_LABEL = "resultStatus";
    public static final String BATCH_RESULT_MESSAGE_LABEL = "resultMessage";
    public static final String BATCH_RESULT_STACKTRACE_LABEL = "resultStacktrace";

    protected static final String CFG_LABEL_BATCH_ID = "batchId";
    protected static final String CFG_LABEL_BATCH_PART_ID = "batchPartId";

    protected static String getBatchIdFromHandlerCfg(String handlerCfg) {
        try {
            JsonNode cfgAsJson = getObjectMapper().readTree(handlerCfg);
            if (cfgAsJson.has(CFG_LABEL_BATCH_ID)) {
                return cfgAsJson.get(CFG_LABEL_BATCH_ID).asString();
            }
            return null;
        } catch (JacksonException e) {
            return null;
        }
    }

    protected static String getBatchPartIdFromHandlerCfg(String handlerCfg) {
        try {
            JsonNode cfgAsJson = getObjectMapper().readTree(handlerCfg);
            if (cfgAsJson.has(CFG_LABEL_BATCH_PART_ID)) {
                return cfgAsJson.get(CFG_LABEL_BATCH_PART_ID).asString();
            }
            return null;
        } catch (JacksonException e) {
            return null;
        }
    }

    public static String getHandlerCfgForBatchId(String batchId) {
        ObjectNode handlerCfg = getObjectMapper().createObjectNode();
        handlerCfg.put(CFG_LABEL_BATCH_ID, batchId);
        return handlerCfg.toString();
    }

    public static String getHandlerCfgForBatchPartId(String batchPartId) {
        ObjectNode handlerCfg = getObjectMapper().createObjectNode();
        handlerCfg.put(CFG_LABEL_BATCH_PART_ID, batchPartId);
        return handlerCfg.toString();
    }

    protected static ObjectMapper getObjectMapper() {
        if (CommandContextUtil.getCommandContext() != null) {
            return CommandContextUtil.getCmmnEngineConfiguration().getObjectMapper();
        } else {
            return JsonMapper.shared();
        }
    }
}

