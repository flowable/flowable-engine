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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;
import org.flowable.job.service.impl.util.CommandContextUtil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class DefaultAsyncHistoryJobProducer implements AsyncHistoryListener {

    protected String jobTypeAsyncHistory;
    protected String jobTypeAsyncHistoryZipped;
    protected String historyJobScopeType;
    
    protected boolean isJsonGzipCompressionEnabled;
    protected boolean isAsyncHistoryJsonGroupingEnabled;
    protected int asyncHistoryJsonGroupingThreshold;
    
    public DefaultAsyncHistoryJobProducer(String jobTypeAsyncHistory, String jobTypeAsyncHistoryZipped) {
        this.jobTypeAsyncHistory = jobTypeAsyncHistory;
        this.jobTypeAsyncHistoryZipped = jobTypeAsyncHistoryZipped;
    }
    
    public DefaultAsyncHistoryJobProducer(String jobTypeAsyncHistory, String jobTypeAsyncHistoryZipped, String historyJobScopeType) {
       this(jobTypeAsyncHistory, jobTypeAsyncHistoryZipped);
       this.historyJobScopeType = historyJobScopeType;
    }
    
    @Override
    public List<HistoryJobEntity> historyDataGenerated(List<ObjectNode> historyObjectNodes) {
        List<HistoryJobEntity> historyJobEntities = createJobsWithHistoricalData(historyObjectNodes, Context.getCommandContext());
        processHistoryJobEntities(historyJobEntities);
        return historyJobEntities;
    }

    protected List<HistoryJobEntity> createJobsWithHistoricalData(List<ObjectNode> historyObjectNodes, CommandContext commandContext) {
        AsyncHistorySession asyncHistorySession = commandContext.getSession(AsyncHistorySession.class);
        if (isAsyncHistoryJsonGroupingEnabled && historyObjectNodes.size() >= asyncHistoryJsonGroupingThreshold) {
            String jobType = isJsonGzipCompressionEnabled ? jobTypeAsyncHistoryZipped : jobTypeAsyncHistory;
            HistoryJobEntity jobEntity = createAndInsertJobEntity(commandContext, asyncHistorySession, jobType);
            ArrayNode arrayNode = CommandContextUtil.getJobServiceConfiguration(commandContext).getObjectMapper().createArrayNode();
            for (ObjectNode historyJsonNode : historyObjectNodes) {
                arrayNode.add(historyJsonNode);
            }
            addJsonToJob(commandContext, jobEntity, arrayNode, isJsonGzipCompressionEnabled);
            return Collections.singletonList(jobEntity);
        } else {
            List<HistoryJobEntity> historyJobEntities = new ArrayList<>(historyObjectNodes.size());
            for (ObjectNode historyJsonNode : historyObjectNodes) {
                HistoryJobEntity jobEntity = createAndInsertJobEntity(commandContext, asyncHistorySession, jobTypeAsyncHistory);
                addJsonToJob(commandContext, jobEntity, historyJsonNode, false);
                historyJobEntities.add(jobEntity);
            }
            return historyJobEntities;
            
        }
    }
    
    protected HistoryJobEntity createAndInsertJobEntity(CommandContext commandContext, AsyncHistorySession asyncHistorySession, String jobType) {
        JobServiceConfiguration jobServiceConfiguration = CommandContextUtil.getJobServiceConfiguration(commandContext);
        HistoryJobEntity currentJobEntity = jobServiceConfiguration.getHistoryJobEntityManager().create();
        currentJobEntity.setJobHandlerType(jobType);
        currentJobEntity.setRetries(jobServiceConfiguration.getAsyncHistoryExecutorNumberOfRetries());
        currentJobEntity.setTenantId(asyncHistorySession.getTenantId());
        currentJobEntity.setCreateTime(jobServiceConfiguration.getClock().getCurrentTime());
        currentJobEntity.setScopeType(historyJobScopeType);
        CommandContextUtil.getJobManager(commandContext).scheduleHistoryJob(currentJobEntity);
        return currentJobEntity;
    }

    protected void addJsonToJob(CommandContext commandContext, HistoryJobEntity jobEntity, JsonNode rootObjectNode, boolean applyCompression) {
        try {
            byte[] bytes = CommandContextUtil.getJobServiceConfiguration(commandContext).getObjectMapper().writeValueAsBytes(rootObjectNode);
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
    
    protected void processHistoryJobEntities(List<HistoryJobEntity> historyJobEntities) {
        // Meant to be overidden in case something extra needs to happen with the created history job entities. 
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
