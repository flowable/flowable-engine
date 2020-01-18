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
package org.flowable.eventregistry.spring.jms;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.model.InboundChannelModel;
import org.springframework.jms.listener.adapter.AbstractAdaptableMessageListener;

/**
 * @author Filip Hrisafov
 */
public class JmsChannelMessageListenerAdapter extends AbstractAdaptableMessageListener {

    protected EventRegistry eventRegistry;
    protected InboundChannelModel inboundChannelModel;

    public JmsChannelMessageListenerAdapter(EventRegistry eventRegistry, InboundChannelModel inboundChannelModel) {
        this.eventRegistry = eventRegistry;
        this.inboundChannelModel = inboundChannelModel;
    }

    @Override
    public void onMessage(Message message, Session session) throws JMSException {
        if (message instanceof TextMessage) {
            eventRegistry.eventReceived(inboundChannelModel, ((TextMessage) message).getText());
        } else {
            //TODO what about other message types
            throw new UnsupportedOperationException("Can only received TextMessage. Received: " + message);
        }
    }

    public EventRegistry getEventRegistry() {
        return eventRegistry;
    }

    public void setEventRegistry(EventRegistry eventRegistry) {
        this.eventRegistry = eventRegistry;
    }

    public InboundChannelModel getInboundChannelModel() {
        return inboundChannelModel;
    }

    public void setInboundChannelModel(InboundChannelModel inboundChannelModel) {
        this.inboundChannelModel = inboundChannelModel;
    }
}
