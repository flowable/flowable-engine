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

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.eventregistry.api.InboundEvent;

/**
 * @author Filip Hrisafov
 */
public class JmsMessageInboundEvent implements InboundEvent {

    protected final Message message;
    protected Map<String, Object> headers;

    public JmsMessageInboundEvent(Message message) {
        this.message = message;
    }

    @Override
    public Message getRawEvent() {
        return message;
    }

    @Override
    public String getBody() {
        return convertEventToString();
    }

    @Override
    public Map<String, Object> getHeaders() {
        if (headers == null) {
            headers = retrieveHeaders();
        }
        return headers;
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> retrieveHeaders() {
        try {
            Map<String, Object> headers = new HashMap<>();
            Enumeration<String> headerNames = message.getPropertyNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                headers.put(headerName, message.getObjectProperty(headerName));
            }

            return headers;

        } catch (JMSException e) {
            throw new FlowableException("Could not get header information", e);
        }
    }

    protected String convertEventToString() {
        if (message instanceof TextMessage) {
            try {
                return ((TextMessage) message).getText();
            } catch (JMSException e) {
                throw new FlowableException("Could not get body information");
            }
        } else {
            return message.toString();
        }
    }
}
