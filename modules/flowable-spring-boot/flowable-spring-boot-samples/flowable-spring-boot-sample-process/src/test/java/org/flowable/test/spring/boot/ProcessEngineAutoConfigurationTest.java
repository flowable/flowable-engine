package org.flowable.test.spring.boot;

import java.util.List;

import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.spring.boot.FlowableTransactionAutoConfiguration;
import org.flowable.spring.boot.ProcessEngineAutoConfiguration;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author Josh Long
 */
public class ProcessEngineAutoConfigurationTest {

    @Test
    public void processEngineWithBasicDataSource() throws Exception {
        AnnotationConfigApplicationContext context = this.context(
                DataSourceAutoConfiguration.class, FlowableTransactionAutoConfiguration.class,
                ProcessEngineAutoConfiguration.class);
        Assert.assertNotNull("the processEngine should not be null!", context.getBean(ProcessEngine.class));
    }

    @Test
    public void launchProcessDefinition() throws Exception {
        AnnotationConfigApplicationContext applicationContext = this.context(
                DataSourceAutoConfiguration.class, FlowableTransactionAutoConfiguration.class,
                ProcessEngineAutoConfiguration.class);
        RepositoryService repositoryService = applicationContext.getBean(RepositoryService.class);
        Assert.assertNotNull("we should have a default repositoryService included", repositoryService);
        Assert.assertEquals(2, repositoryService.createProcessDefinitionQuery().count());
        List<ProcessDefinition> processDefinitionList = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey("waiter")
                .list();
        Assert.assertNotNull(processDefinitionList);
        Assert.assertTrue(!processDefinitionList.isEmpty());
        ProcessDefinition processDefinition = processDefinitionList.iterator().next();
        Assert.assertEquals("waiter", processDefinition.getKey());
    }

    private AnnotationConfigApplicationContext context(Class<?>... clzz) {
        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext();
        annotationConfigApplicationContext.register(clzz);
        annotationConfigApplicationContext.refresh();
        return annotationConfigApplicationContext;
    }
}
