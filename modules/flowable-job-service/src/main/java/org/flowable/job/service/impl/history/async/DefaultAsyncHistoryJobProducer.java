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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class DefaultAsyncHistoryJobProducer implements AsyncHistoryListener {

    @Override
    public List<HistoryJobEntity> historyDataGenerated(JobServiceConfiguration jobServiceConfiguration, List<ObjectNode> historyObjectNodes) {
        CommandContext commandContext = Context.getCommandContext();
        List<HistoryJobEntity> historyJobEntities = createJobsWithHistoricalData(commandContext, jobServiceConfiguration, historyObjectNodes);
        processHistoryJobEntities(commandContext, jobServiceConfiguration, historyObjectNodes, historyJobEntities);
        scheduleJobs(commandContext, jobServiceConfiguration, historyJobEntities);
        return historyJobEntities;
    }

    protected List<HistoryJobEntity> createJobsWithHistoricalData(CommandContext commandContext, 
            JobServiceConfiguration jobServiceConfiguration, List<ObjectNode> historyObjectNodes) {
        
        AsyncHistorySession asyncHistorySession = commandContext.getSession(AsyncHistorySession.class);
        if (jobServiceConfiguration.isAsyncHistoryJsonGroupingEnabled() && historyObjectNodes.size() >= jobServiceConfiguration.getAsyncHistoryJsonGroupingThreshold()) {
            String jobType = getJobType(jobServiceConfiguration, true);
            HistoryJobEntity jobEntity = createJob(commandContext, asyncHistorySession, jobServiceConfiguration, jobType);
            ArrayNode arrayNode = jobServiceConfiguration.getObjectMapper().createArrayNode();
            for (ObjectNode historyJsonNode : historyObjectNodes) {
                arrayNode.add(historyJsonNode);
            }
            addJsonToJob(commandContext, jobServiceConfiguration, jobEntity, arrayNode, jobServiceConfiguration.isAsyncHistoryJsonGzipCompressionEnabled());
            return Collections.singletonList(jobEntity);
            
        } else {
            List<HistoryJobEntity> historyJobEntities = new ArrayList<>(historyObjectNodes.size());
            String jobType = getJobType(jobServiceConfiguration, false);
            for (ObjectNode historyJsonNode : historyObjectNodes) {
                HistoryJobEntity jobEntity = createJob(commandContext, asyncHistorySession, jobServiceConfiguration, jobType);
                addJsonToJob(commandContext, jobServiceConfiguration, jobEntity, historyJsonNode, false);
                historyJobEntities.add(jobEntity);
            }
            return historyJobEntities;
            
        }
    }
    
    protected HistoryJobEntity createJob(CommandContext commandContext, AsyncHistorySession asyncHistorySession,
            JobServiceConfiguration jobServiceConfiguration, String jobType) {
        HistoryJobEntity currentJobEntity = jobServiceConfiguration.getHistoryJobEntityManager().create();
        currentJobEntity.setJobHandlerType(jobType);
        currentJobEntity.setRetries(jobServiceConfiguration.getAsyncHistoryExecutorNumberOfRetries());
        currentJobEntity.setTenantId(asyncHistorySession.getTenantId());
        currentJobEntity.setCreateTime(jobServiceConfiguration.getClock().getCurrentTime());
        currentJobEntity.setScopeType(jobServiceConfiguration.getHistoryJobExecutionScope());
        return currentJobEntity;
    }

    protected void scheduleJobs(CommandContext commandContext, JobServiceConfiguration jobServiceConfiguration, List<HistoryJobEntity> historyJobEntities) {
        for (HistoryJobEntity historyJobEntity : historyJobEntities) {
            jobServiceConfiguration.getJobManager().scheduleHistoryJob(historyJobEntity);
        }
    }

    protected void addJsonToJob(CommandContext commandContext, JobServiceConfiguration jobServiceConfiguration, HistoryJobEntity jobEntity, JsonNode rootObjectNode, boolean applyCompression) {
        try {
            byte[] bytes = jobServiceConfiguration.getObjectMapper().writeValueAsBytes(rootObjectNode);
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

    protected String getJobType(JobServiceConfiguration jobServiceConfiguration, boolean groupingEnabled) {
        if (groupingEnabled) {
            return jobServiceConfiguration.isAsyncHistoryJsonGzipCompressionEnabled() ?
                jobServiceConfiguration.getJobTypeAsyncHistoryZipped() : jobServiceConfiguration.getJobTypeAsyncHistory();
        } else {
            return jobServiceConfiguration.getJobTypeAsyncHistory();
        }
    }
    
    protected void processHistoryJobEntities(CommandContext commandContext, JobServiceConfiguration jobServiceConfiguration,
            List<ObjectNode> historyObjectNodes, List<HistoryJobEntity> historyJobEntities) {
        // Meant to be overridden in case something extra needs to happen with the created history job entities.
    }

}
