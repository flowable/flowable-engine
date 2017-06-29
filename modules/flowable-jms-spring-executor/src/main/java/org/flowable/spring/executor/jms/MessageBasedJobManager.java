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

import java.util.Date;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.flowable.engine.common.impl.cfg.TransactionState;
import org.flowable.engine.impl.asyncexecutor.DefaultJobManager;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.cfg.TransactionContext;
import org.flowable.engine.impl.cfg.TransactionListener;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.impl.history.async.AsyncHistorySession;
import org.flowable.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.HistoryJobEntity;
import org.flowable.engine.impl.persistence.entity.JobEntity;
import org.flowable.engine.impl.persistence.entity.JobInfoEntity;
import org.flowable.engine.runtime.HistoryJob;
import org.flowable.engine.runtime.JobInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

/**
 * @author Joram Barrez
 */
public class MessageBasedJobManager extends DefaultJobManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageBasedJobManager.class);

    protected JmsTemplate jmsTemplate;
    protected JmsTemplate historyJmsTemplate;

    public MessageBasedJobManager() {
        super(null);
    }

    public MessageBasedJobManager(ProcessEngineConfigurationImpl processEngineConfiguration) {
        super(processEngineConfiguration);
    }

    @Override
    protected void triggerExecutorIfNeeded(final JobEntity jobEntity) {
        sendMessage(jobEntity);
    }
    
    @Override
    public HistoryJobEntity scheduleHistoryJob(HistoryJobEntity historyJobEntity) {
        HistoryJobEntity returnValue = super.scheduleHistoryJob(historyJobEntity);
        sendMessage(returnValue);
        return returnValue;
    }

    @Override
    public void unacquire(final JobInfo job) {

        if (job instanceof JobInfoEntity) {
            JobInfoEntity jobInfoEntity = (JobInfoEntity) job;

            // When unacquiring, we up the lock time again., so that it isn't cleared by the reset expired thread.
            jobInfoEntity.setLockExpirationTime(new Date(processEngineConfiguration.getClock().getCurrentTime().getTime()
                    + processEngineConfiguration.getAsyncExecutor().getAsyncJobLockTimeInMillis()));
        }

        sendMessage(job);
    }

    protected void sendMessage(final JobInfo job) {
        TransactionContext transactionContext = Context.getTransactionContext();
        if (transactionContext != null) {
            Context.getTransactionContext().addTransactionListener(TransactionState.COMMITTED, new TransactionListener() {
                public void execute(CommandContext commandContext) {
                    internalSendMessage(job);
                }
            });
            
        } else if (job instanceof HistoryJobEntity) {
            CommandContext commandContext = Context.getCommandContext();
            AsyncHistorySession asyncHistorySession = commandContext.getSession(AsyncHistorySession.class);
            asyncHistorySession.addAsyncHistoryRunnableAfterCommit(new Runnable() {
                public void run() {
                    internalSendMessage(job);
                }
            });
            
        } else {
            LOGGER.warn("Could not send message for job " + job.getId() + ": no transaction context active nor is it a history job");
        }
    }
    
    protected void internalSendMessage(final JobInfo job) {
        JmsTemplate actualJmsTemplate = (job instanceof HistoryJob) ? historyJmsTemplate : jmsTemplate;
        actualJmsTemplate.send(new MessageCreator() {
            public Message createMessage(Session session) throws JMSException {
                return session.createTextMessage(job.getId());
            }
        });
    }

    public JmsTemplate getJmsTemplate() {
        return jmsTemplate;
    }

    public void setJmsTemplate(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    public JmsTemplate getHistoryJmsTemplate() {
        return historyJmsTemplate;
    }

    public void setHistoryJmsTemplate(JmsTemplate historyJmsTemplate) {
        this.historyJmsTemplate = historyJmsTemplate;
    }
    
}
