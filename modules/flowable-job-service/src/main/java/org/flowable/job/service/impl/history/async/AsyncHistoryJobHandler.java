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
package org.flowable.job.service.impl.history.async;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.service.impl.history.async.transformer.HistoryJsonTransformer;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class AsyncHistoryJobHandler extends AbstractAsyncHistoryJobHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncHistoryJobHandler.class);

    protected Map<String, List<HistoryJsonTransformer>> historyJsonTransformers = new HashMap<>();
    
    public AsyncHistoryJobHandler(String jobType) {
        super(jobType);
    }

    public void addHistoryJsonTransformer(HistoryJsonTransformer historyJsonTransformer) {
        String type = historyJsonTransformer.getType();
        if (!historyJsonTransformers.containsKey(type)) {
            historyJsonTransformers.put(type, new ArrayList<>());
        }
        historyJsonTransformers.get(historyJsonTransformer.getType()).add(historyJsonTransformer);
    }

    @Override
    protected void processHistoryJson(CommandContext commandContext, HistoryJobEntity job, JsonNode historyNode) {
        
        String type = historyNode.get(HistoryJsonTransformer.FIELD_NAME_TYPE).asText();
        ObjectNode historicalJsonData = (ObjectNode) historyNode.get(HistoryJsonTransformer.FIELD_NAME_DATA);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Handling async history job (id={}, type={})", job.getId(), type);
        }

        List<HistoryJsonTransformer> transformers = historyJsonTransformers.get(type);
        if (transformers != null && !transformers.isEmpty()) {
            for (HistoryJsonTransformer transformer : transformers) {
                if (transformer.isApplicable(historicalJsonData, commandContext)) {
                    transformer.transformJson(job, historicalJsonData, commandContext);

                } else {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Could not handle history job (id={}) for transformer {}. as it is not applicable. Unacquiring. {}", job.getId(), transformer.getType(), historicalJsonData);
                    }
                    throw new AsyncHistoryJobNotApplicableException();

                }
            }
        } else {
            LOGGER.debug("Cannot transform history json: no transformers found for type {}", type);
        }
    }

    public Map<String, List<HistoryJsonTransformer>> getHistoryJsonTransformers() {
        return historyJsonTransformers;
    }

    public void setHistoryJsonTransformers(Map<String, List<HistoryJsonTransformer>> historyJsonTransformers) {
        this.historyJsonTransformers = historyJsonTransformers;
    }

}
