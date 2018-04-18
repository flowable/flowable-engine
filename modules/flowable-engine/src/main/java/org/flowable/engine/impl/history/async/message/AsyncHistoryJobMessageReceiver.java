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
package org.flowable.engine.impl.history.async.message;

import java.io.IOException;
import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.asyncexecutor.message.AsyncJobMessageReceiver;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.job.api.HistoryJob;
import org.flowable.job.service.HistoryJobService;
import org.flowable.job.service.impl.HistoryJobQueryImpl;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Experimental.
 * 
 * Similar to the {@link AsyncJobMessageReceiver}, but specifically for async history jobs.
 * 
 * @author Joram Barrez
 */
public class AsyncHistoryJobMessageReceiver {

    protected ProcessEngineConfigurationImpl processEngineConfiguration;
    protected AsyncHistoryJobMessageHandler asyncHistoryJobMessageHandler;
    
    public AsyncHistoryJobMessageReceiver() {
        
    }
    
    public AsyncHistoryJobMessageReceiver(ProcessEngineConfigurationImpl processEngineConfiguration, AsyncHistoryJobMessageHandler asyncHistoryJobMessageHandler) {
        this.processEngineConfiguration = processEngineConfiguration;
        this.asyncHistoryJobMessageHandler = asyncHistoryJobMessageHandler;
    }
    
    public void messageForJobReceived(final String jobId) {
        if (processEngineConfiguration == null) {
            throw new FlowableException("Programmatic error: this class needs a ProcessEngineConfigurationImpl instance");
        }
        if (asyncHistoryJobMessageHandler == null) {
            throw new FlowableException("Programmatic error: this class needs an AsyncHistoryJobMessageHandler instance.");
        }
        
        // Wrapping it in a command, as we want it all to be done in the same transaction
        // Furthermore, when accessing the configuration bytes, this needs to be done within a command context.
        processEngineConfiguration.getManagementService().executeCommand(new Command<Void>() {
            
            @Override
            public Void execute(CommandContext commandContext) {
                HistoryJobService historyJobService = CommandContextUtil.getHistoryJobService(commandContext);
                
                HistoryJobQueryImpl query = new HistoryJobQueryImpl();
                query.jobId(jobId);
                
                List<HistoryJob> jobs = historyJobService.findHistoryJobsByQueryCriteria(query);
                if (jobs == null || jobs.isEmpty()) {
                    throw new FlowableException("No history job found for id " + jobId);
                }
                if (jobs.size() > 1) {
                    throw new FlowableException("Multiple results for history job id " + jobId);
                }
                if (!(jobs.get(0) instanceof HistoryJobEntity)) {
                    throw new FlowableException("Job with id " + jobId + " is not an instance of history job entity, cannot handle this job");
                }
                
                HistoryJobEntity historyJobEntity = (HistoryJobEntity) jobs.get(0);
                if (asyncHistoryJobMessageHandler.handleJob(historyJobEntity, getHistoryJobData(historyJobEntity))) {
                    historyJobService.deleteHistoryJob(historyJobEntity);
                }
                
                return null;
            }
            
        });
    }
    
    protected JsonNode getHistoryJobData(HistoryJobEntity job) {
        ObjectMapper objectMapper = processEngineConfiguration.getObjectMapper();
        if (job.getAdvancedJobHandlerConfigurationByteArrayRef() != null) {
            try {
                return objectMapper.readTree(job.getAdvancedJobHandlerConfigurationByteArrayRef().getBytes());
            } catch (IOException e) {
                throw new FlowableException("Could not deserialize json for history job data", e);
            }
        }
        return null;
    }
    
    public ProcessEngineConfigurationImpl getProcessEngineConfiguration() {
        return processEngineConfiguration;
    }

    public void setProcessEngineConfiguration(ProcessEngineConfigurationImpl processEngineConfiguration) {
        this.processEngineConfiguration = processEngineConfiguration;
    }

    public AsyncHistoryJobMessageHandler getAsyncHistoryJobMessageHandler() {
        return asyncHistoryJobMessageHandler;
    }

    public void setAsyncHistoryJobMessageHandler(AsyncHistoryJobMessageHandler asyncHistoryJobMessageHandler) {
        this.asyncHistoryJobMessageHandler = asyncHistoryJobMessageHandler;
    }

}
