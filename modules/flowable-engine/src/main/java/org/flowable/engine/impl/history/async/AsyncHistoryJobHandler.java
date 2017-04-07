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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.engine.impl.history.async.json.transformer.ActivityEndHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.ActivityStartHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.HistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.ProcessInstanceDeleteHistoryByProcessDefinitionIdJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.ProcessInstanceDeleteHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.ProcessInstanceEndHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.ProcessInstanceStartHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.TaskAssigneeChangedHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.TaskCreatedHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.TaskEndedHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.TaskPropertyChangedHistoryJsonTransformer;
import org.flowable.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.JobEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class AsyncHistoryJobHandler extends AbstractAsyncHistoryJobHandler {

    private static final Logger logger = LoggerFactory.getLogger(AsyncHistoryJobHandler.class);

    public static final String JOB_TYPE = "async-history";

    protected Map<String, List<HistoryJsonTransformer>> historyJsonTransformers = new HashMap<String, List<HistoryJsonTransformer>>();

    public AsyncHistoryJobHandler() {
        initDefaultTransformers();
    }

    @Override
    public String getType() {
        return JOB_TYPE;
    }

    protected void initDefaultTransformers() {
        addHistoryJsonTransformer(new ProcessInstanceStartHistoryJsonTransformer());
        addHistoryJsonTransformer(new ProcessInstanceEndHistoryJsonTransformer());
        addHistoryJsonTransformer(new ProcessInstanceDeleteHistoryJsonTransformer());
        addHistoryJsonTransformer(new ProcessInstanceDeleteHistoryByProcessDefinitionIdJsonTransformer());

        addHistoryJsonTransformer(new ActivityStartHistoryJsonTransformer());
        addHistoryJsonTransformer(new ActivityEndHistoryJsonTransformer());

        addHistoryJsonTransformer(new TaskCreatedHistoryJsonTransformer());
        addHistoryJsonTransformer(new TaskEndedHistoryJsonTransformer());

        addHistoryJsonTransformer(new TaskPropertyChangedHistoryJsonTransformer());
        addHistoryJsonTransformer(new TaskAssigneeChangedHistoryJsonTransformer());
    }

    public void addHistoryJsonTransformer(HistoryJsonTransformer historyJsonTransformer) {
        String type = historyJsonTransformer.getType();
        if (!historyJsonTransformers.containsKey(type)) {
            historyJsonTransformers.put(type, new ArrayList<HistoryJsonTransformer>());
        }
        historyJsonTransformers.get(historyJsonTransformer.getType()).add(historyJsonTransformer);
    }

    @Override
    protected void processHistoryJson(CommandContext commandContext, JobEntity job, ArrayNode historicalDataArrayNode) {
        for (JsonNode element : historicalDataArrayNode) {
            String type = element.get("type").asText();
            ObjectNode historicalJsonData = (ObjectNode) element.get("data");

            if (logger.isDebugEnabled()) {
                logger.debug("Handling async history job (id={}, type={})", job.getId(), type);
            }

            List<HistoryJsonTransformer> transformers = historyJsonTransformers.get(type);
            if (transformers != null && !transformers.isEmpty()) {
                for (HistoryJsonTransformer transformer : transformers) {
                    if (transformer.isApplicable(historicalJsonData, commandContext)) {
                        transformer.transformJson(job, historicalJsonData, commandContext);

                    } else {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Could not handle job (id={}) for transformer {}. as it is not applicable. Unacquiring.", 
                                    job.getId(), transformer.getType());
                        }
                        throw new AsyncHistoryJobNotApplicableException();

                    }
                }
            } else {
                logger.debug("Cannot transform history json: no transformers found for type {}", type);
            }
        }
    }

    public Map<String, List<HistoryJsonTransformer>> getHistoryJsonTransformers() {
        return historyJsonTransformers;
    }

    public void setHistoryJsonTransformers(Map<String, List<HistoryJsonTransformer>> historyJsonTransformers) {
        this.historyJsonTransformers = historyJsonTransformers;
    }

}
