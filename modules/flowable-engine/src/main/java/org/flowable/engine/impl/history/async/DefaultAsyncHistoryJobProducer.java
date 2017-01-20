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
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.lang3.tuple.Pair;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.JobEntity;
import org.flowable.engine.impl.persistence.entity.JobEntityManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class DefaultAsyncHistoryJobProducer implements AsyncHistoryJobProducer {
  
  protected boolean isJsonGzipCompressionEnabled;

  @Override
  public JobEntity createAsyncHistoryJob(CommandContext commandContext) {
    return createJobWithHistoricalData(commandContext, commandContext.getSession(AsyncHistorySession.class));
  }
  
  protected JobEntity createJobWithHistoricalData(CommandContext commandContext, AsyncHistorySession asyncHistorySession) {
    JobEntity jobEntity = createAndInsertJobEntity(commandContext, asyncHistorySession);
    ArrayNode historyJson = generateJson(commandContext, asyncHistorySession);
    addJsonToJob(commandContext, jobEntity, historyJson);
    return jobEntity;
  }

  protected JobEntity createAndInsertJobEntity(CommandContext commandContext, AsyncHistorySession asyncHistorySession) {
    JobEntityManager jobEntityManager = commandContext.getJobEntityManager();
    JobEntity currentJobEntity = jobEntityManager.create();
    currentJobEntity.setJobType(JobEntity.JOB_TYPE_MESSAGE);
    currentJobEntity.setJobHandlerType(AsyncHistoryJobHandler.JOB_TYPE);
    currentJobEntity.setRetries(commandContext.getProcessEngineConfiguration().getAsyncExecutorNumberOfRetries());
    currentJobEntity.setExclusive(false);
    currentJobEntity.setTenantId(asyncHistorySession.getTenantId());
    jobEntityManager.insert(currentJobEntity);
    return currentJobEntity;
  }
  
  protected ArrayNode generateJson(CommandContext commandContext, AsyncHistorySession asyncHistorySession) {
    ArrayNode objectNode = commandContext.getProcessEngineConfiguration().getObjectMapper().createArrayNode();
    for (Pair<String, Map<String, String>> historicData : asyncHistorySession.getJobData()) {
      ObjectNode elementObjectNode = objectNode.addObject();
      elementObjectNode.put("type", historicData.getLeft());
      
      ObjectNode dataNode = elementObjectNode.putObject("data");
      Map<String, String> dataMap = historicData.getRight();
      for (String key : dataMap.keySet()) {
        dataNode.put(key, dataMap.get(key));
      }
    }
    return objectNode;
  }
  
  protected void addJsonToJob(CommandContext commandContext, JobEntity jobEntity, ArrayNode rootObjectNode) {
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
