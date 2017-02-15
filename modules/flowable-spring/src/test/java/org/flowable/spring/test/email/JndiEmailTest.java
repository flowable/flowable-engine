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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.jndi.SimpleNamingContextBuilder;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration("classpath:org/flowable/spring/test/email/jndiEmailConfiguaration-context.xml")
public class JndiEmailTest extends SpringFlowableTestCase {

    private static Logger logger = LoggerFactory.getLogger(JndiEmailTest.class);

    @BeforeClass
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
            logger.error("Naming error in email setup", e);
        } catch (NoSuchProviderException e) {
            logger.error("provider error in email setup", e);
        }
    }

    @Deployment(resources = { "org/flowable/spring/test/email/EmailTaskUsingJndi.bpmn20.xml" })
    public void testEmailUsingJndi() {
        Map<String, Object> variables = new HashMap<String, Object>();
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("EmailJndiProcess", variables);
        assertEquals(0, runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count());
    }
}
