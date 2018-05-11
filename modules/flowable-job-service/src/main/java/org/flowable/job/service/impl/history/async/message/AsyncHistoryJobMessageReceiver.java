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
package org.flowable.job.service.impl.history.async.message;

import java.io.IOException;
import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.job.api.HistoryJob;
import org.flowable.job.service.impl.HistoryJobQueryImpl;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntityManager;
import org.flowable.job.service.impl.util.CommandContextUtil;

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

    protected CommandExecutor commandExecutor;
    protected AsyncHistoryJobMessageHandler asyncHistoryJobMessageHandler;
    
    public AsyncHistoryJobMessageReceiver() {
        
    }
    
    public AsyncHistoryJobMessageReceiver(CommandExecutor commandExecutor, AsyncHistoryJobMessageHandler asyncHistoryJobMessageHandler) {
        this.commandExecutor = commandExecutor;
        this.asyncHistoryJobMessageHandler = asyncHistoryJobMessageHandler;
    }
    
    public void messageForJobReceived(final String jobId) {
        if (commandExecutor == null) {
            throw new FlowableException("Programmatic error: this class needs a CommandExecutor instance");
        }
        if (asyncHistoryJobMessageHandler == null) {
            throw new FlowableException("Programmatic error: this class needs an AsyncHistoryJobMessageHandler instance.");
        }
        
        // Wrapping it in a command, as we want it all to be done in the same transaction
        // Furthermore, when accessing the configuration bytes, this needs to be done within a command context.
        commandExecutor.execute(new Command<Void>() {
            
            @Override
            public Void execute(CommandContext commandContext) {
                
                HistoryJobEntityManager historyJobEntityManager = CommandContextUtil.getHistoryJobEntityManager(commandContext);
                
                HistoryJobQueryImpl query = new HistoryJobQueryImpl();
                query.jobId(jobId);
                
                List<HistoryJob> jobs = historyJobEntityManager.findHistoryJobsByQueryCriteria(query);
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
                if (asyncHistoryJobMessageHandler.handleJob(historyJobEntity, getHistoryJobData(commandContext, historyJobEntity))) {
                    historyJobEntityManager.delete(historyJobEntity);
                }
                
                return null;
            }
            
        });
    }
    
    protected JsonNode getHistoryJobData(CommandContext commandContext, HistoryJobEntity job) {
        ObjectMapper objectMapper = CommandContextUtil.getJobServiceConfiguration(commandContext).getObjectMapper();
        if (job.getAdvancedJobHandlerConfigurationByteArrayRef() != null) {
            try {
                return objectMapper.readTree(job.getAdvancedJobHandlerConfigurationByteArrayRef().getBytes());
            } catch (IOException e) {
                throw new FlowableException("Could not deserialize json for history job data", e);
            }
        }
        return null;
    }
    
    public CommandExecutor getCommandExecutor() {
        return commandExecutor;
    }

    public void setCommandExecutor(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }

    public AsyncHistoryJobMessageHandler getAsyncHistoryJobMessageHandler() {
        return asyncHistoryJobMessageHandler;
    }

    public void setAsyncHistoryJobMessageHandler(AsyncHistoryJobMessageHandler asyncHistoryJobMessageHandler) {
        this.asyncHistoryJobMessageHandler = asyncHistoryJobMessageHandler;
    }

}
