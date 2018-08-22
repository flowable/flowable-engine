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
package org.flowable.spring.test.email;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.mail.NoSuchProviderException;
import javax.mail.Provider;
import javax.mail.Provider.Type;
import javax.mail.Session;
import javax.naming.NamingException;

import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.spring.impl.test.SpringFlowableTestCase;
import org.junit.BeforeClass;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.jndi.SimpleNamingContextBuilder;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration("classpath:org/flowable/spring/test/email/jndiEmailConfiguaration-context.xml")
public class JndiEmailTest extends SpringFlowableTestCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(JndiEmailTest.class);

    @BeforeEach
    public void setUp() {
        Properties props = new Properties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.provider.class", MockEmailTransport.class.getName());
        props.put("mail.smtp.class", MockEmailTransport.class.getName());
        props.put("mail.smtp.provider.vendor", "test");
        props.put("mail.smtp.provider.version", "0.0.0");

        Provider provider = new Provider(Type.TRANSPORT, "smtp", MockEmailTransport.class.getName(), "test", "1.0");
        Session mailSession = Session.getDefaultInstance(props);
        SimpleNamingContextBuilder builder = null;
        try {
            mailSession.setProvider(provider);
            builder = SimpleNamingContextBuilder.emptyActivatedContextBuilder();
            builder.bind("java:comp/env/Session", mailSession);
        } catch (NamingException e) {
            LOGGER.error("Naming error in email setup", e);
        } catch (NoSuchProviderException e) {
            LOGGER.error("provider error in email setup", e);
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/spring/test/email/EmailTaskUsingJndi.bpmn20.xml" })
    public void testEmailUsingJndi() {
        Map<String, Object> variables = new HashMap<>();
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("EmailJndiProcess", variables);
        assertEquals(0, runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count());
    }
}
