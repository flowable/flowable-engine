package org.flowable.test.spring.boot;

import javax.persistence.EntityManagerFactory;

import org.flowable.engine.ProcessEngine;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.FlowableTransactionAutoConfiguration;
import org.flowable.spring.boot.ProcessEngineAutoConfiguration;
import org.flowable.spring.boot.ProcessEngineServicesAutoConfiguration;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author Josh Long
 */
public class ProcessEngineAutoConfigurationTest {

    @Test
    public void processEngineWithJpaEntityManager() throws Exception {
        AnnotationConfigApplicationContext context = this.context(DataSourceAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class,
                FlowableTransactionAutoConfiguration.class,
                ProcessEngineServicesAutoConfiguration.class,
                ProcessEngineAutoConfiguration.class
        );
        Assert.assertNotNull("entityManagerFactory should not be null", context.getBean(EntityManagerFactory.class));
        Assert.assertNotNull("the processEngine should not be null!", context.getBean(ProcessEngine.class));
        SpringProcessEngineConfiguration configuration = context.getBean(SpringProcessEngineConfiguration.class);
        Assert.assertNotNull("the " + SpringProcessEngineConfiguration.class.getName() + " should not be null", configuration);
        Assert.assertNotNull(configuration.getJpaEntityManagerFactory());
    }

    private AnnotationConfigApplicationContext context(Class<?>... clzz) {
        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext();
        annotationConfigApplicationContext.register(clzz);
        annotationConfigApplicationContext.refresh();
        return annotationConfigApplicationContext;
    }
}
