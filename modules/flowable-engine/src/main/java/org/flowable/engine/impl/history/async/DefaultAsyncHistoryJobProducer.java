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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.lang3.tuple.Pair;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.HistoryJobEntity;
import org.flowable.engine.impl.persistence.entity.HistoryJobEntityManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class DefaultAsyncHistoryJobProducer implements AsyncHistoryJobProducer {

    protected boolean isJsonGzipCompressionEnabled;

    @Override
    public void createAsyncHistoryJobs(CommandContext commandContext) {
        createJobsWithHistoricalData(commandContext, commandContext.getSession(AsyncHistorySession.class));
    }

    protected void createJobsWithHistoricalData(CommandContext commandContext, AsyncHistorySession asyncHistorySession) {
        List<Pair<String, Map<String, String>>> filteredJobs = filterHistoricData(asyncHistorySession.getJobData());
        for (Pair<String, Map<String, String>> historicData : filteredJobs) {
            HistoryJobEntity jobEntity = createAndInsertJobEntity(commandContext, asyncHistorySession);
            ObjectNode historyJson = generateJson(commandContext, historicData);
            addJsonToJob(commandContext, jobEntity, historyJson);
        }
    }
    
    protected List<Pair<String, Map<String, String>>> filterHistoricData(List<Pair<String, Map<String, String>>> jobData) {
        List<Pair<String, Map<String, String>>> filteredJobs = new ArrayList<>();
        Map<String, List<Pair<String, Map<String, String>>>> activityStartMap = new HashMap<>();
        Map<String, Pair<String, Map<String, String>>> variableUpdatedMap = new HashMap<>();
        
        for (Pair<String, Map<String, String>> historicData : jobData) {
            if ("activity-start".equals(historicData.getKey())) {
                
                String activityKey = historicData.getValue().get(HistoryJsonConstants.EXECUTION_ID) + "_" + 
                                historicData.getValue().get(HistoryJsonConstants.ACTIVITY_ID);
                
                List<Pair<String, Map<String, String>>> activityHistoricData = null;
                if (activityStartMap.containsKey(activityKey)) {
                    activityHistoricData = activityStartMap.get(activityKey);
                } else {
                    activityHistoricData = new ArrayList<>();
                }
                
                activityHistoricData.add(historicData);
                activityStartMap.put(activityKey, activityHistoricData);
                
            } else if ("variable-updated".equals(historicData.getKey())) {
                variableUpdatedMap.put(historicData.getValue().get(HistoryJsonConstants.ID), historicData);
            
            } else if (!"activity-end".equals(historicData.getKey())) {
                filteredJobs.add(historicData);
            }
        }
        
        for (Pair<String, Map<String, String>> historicData : jobData) {
            if ("activity-end".equals(historicData.getKey())) {
                
                String activityKey = historicData.getValue().get(HistoryJsonConstants.EXECUTION_ID) + "_" + 
                                historicData.getValue().get(HistoryJsonConstants.ACTIVITY_ID);
                
                if (activityStartMap.containsKey(activityKey)) {
                    List<Pair<String, Map<String, String>>> activityHistoricData = activityStartMap.get(activityKey);
                    filteredJobs.add(Pair.of("activity-full", historicData.getValue()));
                    activityHistoricData.remove(0);
                    if (activityHistoricData.size() == 0) {
                        activityStartMap.remove(activityKey);
                    } else {
                        activityStartMap.put(activityKey, activityHistoricData); 
                    }
                    
                } else {
                    filteredJobs.add(historicData);
                } 
            }
        }
        
        for (String activityStartKey : activityStartMap.keySet()) {
            List<Pair<String, Map<String, String>>> activityHistoricData = activityStartMap.get(activityStartKey);
            for (Pair<String, Map<String, String>> historicData : activityHistoricData) {
                filteredJobs.add(historicData);
            }
        }
        
        for (Pair<String, Map<String, String>> variableUpdatedData : variableUpdatedMap.values()) {
            filteredJobs.add(variableUpdatedData);
        }
        
        return filteredJobs;
    }

    protected HistoryJobEntity createAndInsertJobEntity(CommandContext commandContext, AsyncHistorySession asyncHistorySession) {
        ProcessEngineConfiguration processEngineConfiguration = commandContext.getProcessEngineConfiguration();
        HistoryJobEntityManager historyJobEntityManager = commandContext.getHistoryJobEntityManager();
        HistoryJobEntity currentJobEntity = historyJobEntityManager.create();
        currentJobEntity.setJobHandlerType(AsyncHistoryJobHandler.JOB_TYPE);
        currentJobEntity.setRetries(commandContext.getProcessEngineConfiguration().getAsyncHistoryExecutorNumberOfRetries());
        currentJobEntity.setTenantId(asyncHistorySession.getTenantId());
        currentJobEntity.setCreateTime(processEngineConfiguration.getClock().getCurrentTime());
        historyJobEntityManager.insert(currentJobEntity);
        return currentJobEntity;
    }

    protected ObjectNode generateJson(CommandContext commandContext, Pair<String, Map<String, String>> historicData) {
        ObjectNode elementObjectNode = commandContext.getProcessEngineConfiguration().getObjectMapper().createObjectNode();
        elementObjectNode.put("type", historicData.getLeft());

        ObjectNode dataNode = elementObjectNode.putObject("data");
        Map<String, String> dataMap = historicData.getRight();
        for (String key : dataMap.keySet()) {
            dataNode.put(key, dataMap.get(key));
        }
        
        System.out.println("!!!!!! generating history job with type " + historicData.getLeft() + " " + elementObjectNode);
        
        return elementObjectNode;
    }

    protected void addJsonToJob(CommandContext commandContext, HistoryJobEntity jobEntity, ObjectNode rootObjectNode) {
        try {
            byte[] bytes = commandContext.getProcessEngineConfiguration().getObjectMapper().writeValueAsBytes(rootObjectNode);
            if (isJsonGzipCompressionEnabled) {
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

}
