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

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.flowable.job.api.HistoryJob;
import org.flowable.job.api.JobInfo;
import org.flowable.job.service.impl.asyncexecutor.message.AbstractMessageBasedJobManager;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

/**
 * @author Joram Barrez
 */
public class MessageBasedJobManager extends AbstractMessageBasedJobManager {
    
    protected JmsTemplate jmsTemplate;
    protected JmsTemplate historyJmsTemplate;

    @Override
    protected void sendMessage(final JobInfo job) {
        JmsTemplate actualJmsTemplate = (job instanceof HistoryJob) ? historyJmsTemplate : jmsTemplate;
        actualJmsTemplate.send(new MessageCreator() {
            @Override
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
