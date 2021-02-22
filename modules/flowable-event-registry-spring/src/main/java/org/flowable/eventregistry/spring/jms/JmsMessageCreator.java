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

/**
 * A creator of a JMS {@link Message} from the raw event.
 *
 * @author Filip Hrisafov
 */
public interface JmsMessageCreator<T> {

    /**
     * Convert the raw event to a JMS Message using the supplied session
     * to create the message object.
     *
     * @param event the event to convert
     * @param session the Session to use for creating a JMS Message
     * @return the JMS Message
     * @throws javax.jms.JMSException if thrown by JMS API methods
     */
    Message toMessage(T event, Session session) throws JMSException;

}
