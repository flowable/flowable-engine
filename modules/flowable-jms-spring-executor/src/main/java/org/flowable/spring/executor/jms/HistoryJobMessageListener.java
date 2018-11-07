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
package org.flowable.spring.executor.jms;

import javax.jms.Message;
import javax.jms.TextMessage;

import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.asyncexecutor.AsyncRunnableExecutionExceptionHandler;
import org.flowable.job.service.impl.asyncexecutor.ExecuteAsyncRunnable;
import org.flowable.job.service.impl.asyncexecutor.UnacquireAsyncHistoryJobExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HistoryJobMessageListener implements javax.jms.MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(HistoryJobMessageListener.class);

    protected JobServiceConfiguration jobServiceConfiguration;
    protected AsyncRunnableExecutionExceptionHandler exceptionHandler;
    
    public HistoryJobMessageListener() {
        this.exceptionHandler = new UnacquireAsyncHistoryJobExceptionHandler();
    }

    @Override
    public void onMessage(final Message message) {
        try {
            if (message instanceof TextMessage) {
                TextMessage textMessage = (TextMessage) message;
                String jobId = textMessage.getText();
                ExecuteAsyncRunnable executeAsyncRunnable = new ExecuteAsyncRunnable(jobId, 
                                jobServiceConfiguration, jobServiceConfiguration.getHistoryJobEntityManager(), exceptionHandler);
                executeAsyncRunnable.run();
            }
        } catch (Exception e) {
            logger.error("Exception when handling message from job queue", e);
        }
    }

    public JobServiceConfiguration getJobServiceConfiguration() {
        return jobServiceConfiguration;
    }

    public void setJobServiceConfiguration(JobServiceConfiguration jobServiceConfiguration) {
        this.jobServiceConfiguration = jobServiceConfiguration;
    }

}
