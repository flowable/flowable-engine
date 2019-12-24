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

import org.flowable.eventregistry.api.OutboundEventChannelAdapter;
import org.springframework.jms.core.JmsOperations;

/**
 * @author Filip Hrisafov
 */
public class JmsOperationsOutboundEventChannelAdapter implements OutboundEventChannelAdapter {

    protected JmsOperations jmsOperations;
    protected String destination;

    public JmsOperationsOutboundEventChannelAdapter(JmsOperations jmsOperations, String destination) {
        this.jmsOperations = jmsOperations;
        this.destination = destination;
    }

    @Override
    public void sendEvent(String rawEvent) {
        jmsOperations.convertAndSend(destination, rawEvent);
    }

    public JmsOperations getJmsOperations() {
        return jmsOperations;
    }

    public void setJmsOperations(JmsOperations jmsOperations) {
        this.jmsOperations = jmsOperations;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }
}
