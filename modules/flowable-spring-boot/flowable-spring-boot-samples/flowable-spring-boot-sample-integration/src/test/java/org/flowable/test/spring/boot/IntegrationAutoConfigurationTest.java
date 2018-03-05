package org.flowable.test.spring.boot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.spring.boot.FlowableTransactionAutoConfiguration;
import org.flowable.spring.boot.ProcessEngineAutoConfiguration;
import org.flowable.spring.integration.FlowableInboundGateway;
import org.flowable.spring.integration.IntegrationActivityBehavior;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.autoconfigure.integration.IntegrationAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.support.GenericHandler;
import org.springframework.messaging.support.MessageBuilder;

/**
 * Test the Spring Integration inbound inboundGateway support.
 *
 * @author Josh Long
 */
public class IntegrationAutoConfigurationTest {

    @Configuration
    @Import(BaseConfiguration.class)
    public static class InboundGatewayConfiguration {
        @Bean
        public IntegrationActivityBehavior flowableDelegate(FlowableInboundGateway activitiInboundGateway) {
            return new IntegrationActivityBehavior(activitiInboundGateway);
        }

        @Bean
        public FlowableInboundGateway inboundGateway(ProcessEngine processEngine) {
            return new FlowableInboundGateway(processEngine, "customerId", "projectId", "orderId");
        }

        @Bean
        public IntegrationFlow inboundProcess(FlowableInboundGateway inboundGateway) {
            return IntegrationFlows
                    .from(inboundGateway)
                    .handle(new GenericHandler<DelegateExecution>() {
                        @Override
                        public Object handle(DelegateExecution execution, Map<String, Object> headers) {
                            return MessageBuilder.withPayload(execution)
                                    .setHeader("projectId", projectId)
                                    .setHeader("orderId", "246")
                                    .copyHeaders(headers).build();
                        }
                    })
                    .get();
        }

        @Bean(name = "analysingService")
        public AnalysingService service() {
            return new AnalysingService();
        }

        public static class AnalysingService {
            private final AtomicReference<String> stringAtomicReference = new AtomicReference<>();

            public void dump(String projectId) {
                this.stringAtomicReference.set(projectId);
            }

            public AtomicReference<String> getStringAtomicReference() {
                return stringAtomicReference;
            }
        }
    }

    public static final String projectId = "2143243";

    private AnnotationConfigApplicationContext context(Class<?>... clzz) {
        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext();
        annotationConfigApplicationContext.register(clzz);
        annotationConfigApplicationContext.refresh();
        return annotationConfigApplicationContext;
    }

    @Test
    public void testLaunchingGatewayProcessDefinition() throws Exception {
        AnnotationConfigApplicationContext applicationContext = this.context(InboundGatewayConfiguration.class);

        RepositoryService repositoryService = applicationContext.getBean(RepositoryService.class);
        RuntimeService runtimeService = applicationContext.getBean(RuntimeService.class);
        ProcessEngine processEngine = applicationContext.getBean(ProcessEngine.class);

        Assert.assertNotNull("the process engine should not be null", processEngine);
        Assert.assertNotNull("we should have a default repositoryService included", repositoryService);
        String integrationGatewayProcess = "integrationGatewayProcess";
        List<ProcessDefinition> processDefinitionList = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(integrationGatewayProcess)
                .list();
        ProcessDefinition processDefinition = processDefinitionList.iterator().next();
        Assert.assertEquals(integrationGatewayProcess, processDefinition.getKey());
        Map<String, Object> vars = new HashMap<>();
        vars.put("customerId", 232);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(integrationGatewayProcess, vars);
        Assert.assertNotNull("the processInstance should not be null", processInstance);
        Assert.assertEquals(projectId, applicationContext.getBean(InboundGatewayConfiguration.AnalysingService.class)
                .getStringAtomicReference().get());
    }

    @Configuration
    @Import({ DataSourceAutoConfiguration.class,
            FlowableTransactionAutoConfiguration.class,
            ProcessEngineAutoConfiguration.class,
            IntegrationAutoConfiguration.class })
    public static class BaseConfiguration {

        @Bean
        public TaskExecutor taskExecutor() {
            return new SimpleAsyncTaskExecutor();
        }
    }

}
