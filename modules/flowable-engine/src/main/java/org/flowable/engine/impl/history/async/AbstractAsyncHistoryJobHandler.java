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

import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.job.service.HistoryJobHandler;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

public abstract class AbstractAsyncHistoryJobHandler implements HistoryJobHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAsyncHistoryJobHandler.class);
    
    protected boolean isAsyncHistoryJsonGroupingEnabled;

    @Override
    public void execute(HistoryJobEntity job, String configuration, CommandContext commandContext) {
        ObjectMapper objectMapper = CommandContextUtil.getProcessEngineConfiguration(commandContext).getObjectMapper();
        if (job.getAdvancedJobHandlerConfigurationByteArrayRef() != null) {
            try {

                byte[] bytes = getJobBytes(job);
                JsonNode historyNode = objectMapper.readTree(bytes);
                if (isAsyncHistoryJsonGroupingEnabled() && historyNode.isArray()) {
                    ArrayNode arrayNode = (ArrayNode) historyNode;
                    for (JsonNode jsonNode : arrayNode) {
                        processHistoryJson(commandContext, job, jsonNode);
                    }
                } else {
                    processHistoryJson(commandContext, job, historyNode);
                }
                
            } catch (AsyncHistoryJobNotApplicableException e) {
                throw e;

            } catch (Exception e) {
                
                LOGGER.warn("Could not execute history job", e);
                
                // The transaction will be rolled back and the job retries decremented,
                // which is different from unacquiring the job where the retries are not changed.
                throw new FlowableException("Could not deserialize async history json for job (id=" + job.getId() + ")", e);
            }
        }
    }

    protected byte[] getJobBytes(HistoryJobEntity job) {
        return job.getAdvancedJobHandlerConfigurationByteArrayRef().getBytes();
    }

    protected abstract void processHistoryJson(CommandContext commandContext, HistoryJobEntity job, JsonNode historyNode);

    public boolean isAsyncHistoryJsonGroupingEnabled() {
        return isAsyncHistoryJsonGroupingEnabled;
    }

    public void setAsyncHistoryJsonGroupingEnabled(boolean isAsyncHistoryJsonGroupingEnabled) {
        this.isAsyncHistoryJsonGroupingEnabled = isAsyncHistoryJsonGroupingEnabled;
    }
    
}
