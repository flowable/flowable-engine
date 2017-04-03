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
package org.flowable.engine.impl.history.async;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.impl.util.IoUtil;
import org.flowable.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.jobexecutor.JobHandler;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.JobEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

public abstract class AbstractAsyncHistoryJobHandler implements JobHandler {

    protected boolean isJsonGzipCompressionEnabled;

    @Override
    public void execute(JobEntity job, String configuration, ExecutionEntity execution, CommandContext commandContext) {
        ObjectMapper objectMapper = commandContext.getProcessEngineConfiguration().getObjectMapper();
        ArrayNode historicalDataArrayNode = null;
        if (job.getAdvancedJobHandlerConfigurationByteArrayRef() != null) {
            try {

                byte[] bytes = job.getAdvancedJobHandlerConfigurationByteArrayRef().getBytes();
                if (isJsonGzipCompressionEnabled) {
                    bytes = decompress(bytes);
                }
                JsonNode objectNode = objectMapper.readTree(bytes);
                if (objectNode.isArray()) {
                    historicalDataArrayNode = (ArrayNode) objectNode;
                    processHistoryJson(commandContext, job, historicalDataArrayNode);
                }

            } catch (Exception e) {
                // The transaction will be rolled back and the job retries decremented,
                // which is different from unacquiring the job where the retries are not changed.
                if (e instanceof AsyncHistoryJobNotApplicableException) {
                    throw (AsyncHistoryJobNotApplicableException) e;
                } else {
                    throw new FlowableException("Could not deserialize async history json for job (id=" + job.getId() + ")", e);
                }
            }
        }
    }

    protected byte[] decompress(final byte[] compressed) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(compressed)) {
            try (GZIPInputStream gis = new GZIPInputStream(bais)) {
                return IoUtil.readInputStream(gis, "async-history-configuration");
            }
        } catch (IOException e) {
            throw new RuntimeException("Error while decompressing json bytes", e);
        }
    }

    protected abstract void processHistoryJson(CommandContext commandContext, JobEntity job, ArrayNode historicalDataArrayNode);

    public boolean isJsonGzipCompressionEnabled() {
        return isJsonGzipCompressionEnabled;
    }

    public void setJsonGzipCompressionEnabled(boolean isJsonGzipCompressionEnabled) {
        this.isJsonGzipCompressionEnabled = isJsonGzipCompressionEnabled;
    }

}
