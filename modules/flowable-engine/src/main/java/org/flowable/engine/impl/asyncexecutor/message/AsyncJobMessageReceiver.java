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
package org.flowable.engine.impl.asyncexecutor.message;

import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.api.Job;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.JobQueryImpl;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.JobEntityManager;

/**
 * Experimental.
 * 
 * Helper class to be used in a setup of async jobs handling where the job is 
 * inserted in the same database transaction as the runtime data in combination with sending message to a message queue.
 * 
 * Use a sublass of {@link AbstractMessageBasedJobManager} to send the message, this
 * class, with the proper handler instance set, will take care of handling it on the receiving side.
 * 
 * This class contains the boilerplate logic that is needed to fetch the job data 
 * and delete the job in case of a succesful processing.
 * 
 * @author Joram Barrez
 */
public class AsyncJobMessageReceiver {

    protected JobServiceConfiguration jobServiceConfiguration;
    protected AsyncJobMessageHandler asyncJobMessageHandler;
    
    public AsyncJobMessageReceiver() {
        
    }
    
    public AsyncJobMessageReceiver(JobServiceConfiguration jobServiceConfiguration, AsyncJobMessageHandler asyncJobMessageHandler) {
        this.jobServiceConfiguration = jobServiceConfiguration;
        this.asyncJobMessageHandler = asyncJobMessageHandler;
    }
    
    public void messageForJobReceived(final String jobId) {
        if (jobServiceConfiguration == null) {
            throw new FlowableException("Programmatic error: this class needs a JobServiceEngineConfiguration instance");
        }
        if (asyncJobMessageHandler == null) {
            throw new FlowableException("Programmatic error: this class needs an AsyncJobMessageHandler instance.");
        }
        
        jobServiceConfiguration.getCommandExecutor().execute(new Command<Void>() {
            
            @Override
            public Void execute(CommandContext commandContext) {
                JobEntityManager jobEntityManager = jobServiceConfiguration.getJobEntityManager();
                
                JobQueryImpl query = new JobQueryImpl();
                query.jobId(jobId);
                
                List<Job> jobs = jobEntityManager.findJobsByQueryCriteria(query);
                if (jobs == null || jobs.isEmpty()) {
                    throw new FlowableException("No job found for job id " + jobId);
                }
                if (jobs.size() > 1) {
                    throw new FlowableException("Multiple results for job id " + jobId);
                }
                if (!(jobs.get(0) instanceof JobEntity)) {
                    throw new FlowableException("Job with id " + jobId + " is not an instance of job entity, cannot handle this job");
                }
                
                JobEntity jobEntity = (JobEntity) jobs.get(0);
                if (asyncJobMessageHandler.handleJob(jobEntity)) {
                    jobEntityManager.delete(jobEntity);
                }
                return null;
            }
            
        });
    }
    
    public JobServiceConfiguration getJobServiceConfiguration() {
        return jobServiceConfiguration;
    }

    public void setJobServiceConfiguration(JobServiceConfiguration jobServiceConfiguration) {
        this.jobServiceConfiguration = jobServiceConfiguration;
    }

    public AsyncJobMessageHandler getAsyncJobMessageHandler() {
        return asyncJobMessageHandler;
    }

    public void setAsyncJobMessageHandler(AsyncJobMessageHandler asyncJobMessageHandler) {
        this.asyncJobMessageHandler = asyncJobMessageHandler;
    }
    
}
