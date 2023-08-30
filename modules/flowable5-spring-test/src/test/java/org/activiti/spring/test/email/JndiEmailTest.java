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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.activiti.spring.impl.test.SpringFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;

import jakarta.mail.NoSuchProviderException;
import jakarta.mail.Provider;
import jakarta.mail.Provider.Type;
import jakarta.mail.Session;

@ContextConfiguration("classpath:org/activiti/spring/test/email/jndiEmailConfiguaration-context.xml")
public class JndiEmailTest extends SpringFlowableTestCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(JndiEmailTest.class);

    @BeforeClass
    public void setUp() throws NoSuchProviderException, NamingException {
        Properties props = new Properties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.provider.class", MockEmailTransport.class.getName());
        props.put("mail.smtp.class", MockEmailTransport.class.getName());
        props.put("mail.smtp.provider.vendor", "test");
        props.put("mail.smtp.provider.version", "0.0.0");

        Provider provider = new Provider(Type.TRANSPORT, "smtp", MockEmailTransport.class.getName(), "test", "1.0");
        Session mailSession = Session.getDefaultInstance(props);
        mailSession.setProvider(provider);
        InitialContext ctx = new InitialContext();
        ctx.createSubcontext("java:comp")
                .createSubcontext("env")
                .bind("Session", mailSession);
    }

    @Test
    @Deployment(resources = { "org/activiti/spring/test/email/EmailTaskUsingJndi.bpmn20.xml" })
    public void testEmailUsingJndi() {
        Map<String, Object> variables = new HashMap<>();
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("EmailJndiProcess", variables);
        assertEquals(0, runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count());
    }
}
