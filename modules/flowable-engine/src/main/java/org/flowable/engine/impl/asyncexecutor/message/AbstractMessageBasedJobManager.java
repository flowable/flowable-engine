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

import java.util.Date;

import org.flowable.engine.common.impl.cfg.TransactionContext;
import org.flowable.engine.common.impl.cfg.TransactionListener;
import org.flowable.engine.common.impl.cfg.TransactionState;
import org.flowable.engine.common.impl.context.Context;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.impl.asyncexecutor.DefaultJobManager;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.history.async.AsyncHistorySession;
import org.flowable.engine.impl.persistence.entity.HistoryJobEntity;
import org.flowable.engine.impl.persistence.entity.JobEntity;
import org.flowable.engine.impl.persistence.entity.JobInfoEntity;
import org.flowable.engine.runtime.HistoryJob;
import org.flowable.engine.runtime.JobInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class that contains the main logic to send information about an async history data job to a message queue.
 * Subclasses are responsible for implementing the actual sending logic.   
 * 
 * @author Joram Barrez
 */
public abstract class AbstractMessageBasedJobManager extends DefaultJobManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMessageBasedJobManager.class);

    public AbstractMessageBasedJobManager() {
        super(null);
    }

    public AbstractMessageBasedJobManager(ProcessEngineConfigurationImpl processEngineConfiguration) {
        super(processEngineConfiguration);
    }

    @Override
    protected void triggerExecutorIfNeeded(final JobEntity jobEntity) {
        prepareAndSendMessage(jobEntity);
    }
    
    @Override
    public HistoryJobEntity scheduleHistoryJob(HistoryJobEntity historyJobEntity) {
        HistoryJobEntity returnValue = super.scheduleHistoryJob(historyJobEntity);
        prepareAndSendMessage(returnValue);
        return returnValue;
    }

    @Override
    public void unacquire(JobInfo job) {

        if (job instanceof JobInfoEntity) {
            JobInfoEntity jobInfoEntity = (JobInfoEntity) job;

            // When unacquiring, we up the lock time again., so that it isn't cleared by the reset expired thread.
            jobInfoEntity.setLockExpirationTime(new Date(processEngineConfiguration.getClock().getCurrentTime().getTime()
                    + processEngineConfiguration.getAsyncExecutor().getAsyncJobLockTimeInMillis()));
        }

        prepareAndSendMessage(job);
    }
    
    @Override
    public void unacquireWithDecrementRetries(JobInfo job) {
        if (job instanceof HistoryJob) {
            HistoryJobEntity historyJobEntity = (HistoryJobEntity) job;
            if (historyJobEntity.getRetries() > 0) {
                historyJobEntity.setRetries(historyJobEntity.getRetries() - 1);
                unacquire(historyJobEntity);
            } else {
                processEngineConfiguration.getHistoryJobEntityManager().deleteNoCascade(historyJobEntity);
            }
        } else {
            unacquire(job);
        }
    }

    protected void prepareAndSendMessage(final JobInfo job) {
        TransactionContext transactionContext = Context.getTransactionContext();
        if (transactionContext != null) {
            Context.getTransactionContext().addTransactionListener(TransactionState.COMMITTED, new TransactionListener() {
                public void execute(CommandContext commandContext) {
                    sendMessage(job);
                }
            });
            
        } else if (job instanceof HistoryJobEntity) {
            CommandContext commandContext = Context.getCommandContext();
            AsyncHistorySession asyncHistorySession = commandContext.getSession(AsyncHistorySession.class);
            asyncHistorySession.addAsyncHistoryRunnableAfterCommit(new Runnable() {
                public void run() {
                    sendMessage(job);
                }
            });
            
        } else {
            LOGGER.warn("Could not send message for job {}: no transaction context active nor is it a history job", job.getId());
        }
    }
    
    /**
     * Subclasses need to implement this method: it should contain the actual sending of the message
     * using the job data provided in the parameter.  
     */
    protected abstract void sendMessage(JobInfo job);
    
}
