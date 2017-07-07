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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.impl.context.Context;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.HistoryJobEntity;
import org.flowable.engine.impl.persistence.entity.HistoryJobEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class DefaultAsyncHistoryJobProducer implements AsyncHistoryListener {

    protected boolean isJsonGzipCompressionEnabled;
    protected boolean isAsyncHistoryJsonGroupingEnabled;
    protected int asyncHistoryJsonGroupingThreshold;
    
    @Override
    public void historyDataGenerated(List<ObjectNode> historyObjectNodes) {
        createJobsWithHistoricalData(historyObjectNodes, Context.getCommandContext());
    }

    protected void createJobsWithHistoricalData(List<ObjectNode> historyObjectNodes, CommandContext commandContext) {
        AsyncHistorySession asyncHistorySession = commandContext.getSession(AsyncHistorySession.class);
        if (isAsyncHistoryJsonGroupingEnabled && historyObjectNodes.size() >= asyncHistoryJsonGroupingThreshold) {
            String jobType = isJsonGzipCompressionEnabled ? AsyncHistoryJobZippedHandler.JOB_TYPE : AsyncHistoryJobHandler.JOB_TYPE;
            HistoryJobEntity jobEntity = createAndInsertJobEntity(commandContext, asyncHistorySession, jobType);
            ArrayNode arrayNode = CommandContextUtil.getProcessEngineConfiguration(commandContext).getObjectMapper().createArrayNode();
            for (ObjectNode historyJsonNode : historyObjectNodes) {
                arrayNode.add(historyJsonNode);
            }
            addJsonToJob(commandContext, jobEntity, arrayNode, isJsonGzipCompressionEnabled);
            
        } else {
            for (ObjectNode historyJsonNode : historyObjectNodes) {
                HistoryJobEntity jobEntity = createAndInsertJobEntity(commandContext, asyncHistorySession, AsyncHistoryJobHandler.JOB_TYPE);
                addJsonToJob(commandContext, jobEntity, historyJsonNode, false);
            }
            
        }
    }
    
    protected HistoryJobEntity createAndInsertJobEntity(CommandContext commandContext, AsyncHistorySession asyncHistorySession, String jobType) {
        ProcessEngineConfiguration processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        HistoryJobEntityManager historyJobEntityManager = CommandContextUtil.getHistoryJobEntityManager(commandContext);
        HistoryJobEntity currentJobEntity = historyJobEntityManager.create();
        currentJobEntity.setJobHandlerType(jobType);
        currentJobEntity.setRetries(CommandContextUtil.getProcessEngineConfiguration(commandContext).getAsyncHistoryExecutorNumberOfRetries());
        currentJobEntity.setTenantId(asyncHistorySession.getTenantId());
        currentJobEntity.setCreateTime(processEngineConfiguration.getClock().getCurrentTime());
        CommandContextUtil.getJobManager(commandContext).scheduleHistoryJob(currentJobEntity);
        return currentJobEntity;
    }

    protected void addJsonToJob(CommandContext commandContext, HistoryJobEntity jobEntity, JsonNode rootObjectNode, boolean applyCompression) {
        try {
            byte[] bytes = CommandContextUtil.getProcessEngineConfiguration(commandContext).getObjectMapper().writeValueAsBytes(rootObjectNode);
            if (applyCompression) {
                bytes = compress(bytes);
            }
            jobEntity.setAdvancedJobHandlerConfigurationBytes(bytes);
        } catch (JsonProcessingException e) {
            throw new FlowableException("Could not serialize historic data for async history", e);
        }
    }

    protected byte[] compress(final byte[] bytes) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (GZIPOutputStream gos = new GZIPOutputStream(baos)) {
                gos.write(bytes);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new FlowableException("Error while compressing json", e);
        }
    }

    public boolean isJsonGzipCompressionEnabled() {
        return isJsonGzipCompressionEnabled;
    }

    public void setJsonGzipCompressionEnabled(boolean isJsonGzipCompressionEnabled) {
        this.isJsonGzipCompressionEnabled = isJsonGzipCompressionEnabled;
    }

    public boolean isAsyncHistoryJsonGroupingEnabled() {
        return isAsyncHistoryJsonGroupingEnabled;
    }

    public void setAsyncHistoryJsonGroupingEnabled(boolean isAsyncHistoryJsonGroupingEnabled) {
        this.isAsyncHistoryJsonGroupingEnabled = isAsyncHistoryJsonGroupingEnabled;
    }

    public int getAsyncHistoryJsonGroupingThreshold() {
        return asyncHistoryJsonGroupingThreshold;
    }

    public void setAsyncHistoryJsonGroupingThreshold(int asyncHistoryJsonGroupingThreshold) {
        this.asyncHistoryJsonGroupingThreshold = asyncHistoryJsonGroupingThreshold;
    }
    
}
