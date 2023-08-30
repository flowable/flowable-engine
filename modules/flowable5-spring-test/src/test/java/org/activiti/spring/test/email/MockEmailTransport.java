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
package org.activiti.spring.test.email;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.URLName;

/**
 * @author Hariprasath Manivannan
 */
public class MockEmailTransport extends Transport {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockEmailTransport.class);

    public MockEmailTransport(Session smtpSession, URLName urlName) {
        super(smtpSession, urlName);
    }

    @Override
    public void sendMessage(Message message, Address[] addresses) throws MessagingException {
        try {
            LOGGER.info(message.getContent().toString());
        } catch (IOException ex) {
            LOGGER.error("Error occured while sending email" + ex);
        }
    }

    @Override
    public void connect() throws MessagingException {
    }

    @Override
    public void connect(String host, int port, String username, String password) throws MessagingException {
    }

    @Override
    public void connect(String host, String username, String password) throws MessagingException {
    }

    @Override
    public void close() {
    }
}
