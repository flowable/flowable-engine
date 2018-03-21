package org.flowable.test.spring.boot;

import java.util.Map;

import org.flowable.engine.ProcessEngine;
import org.flowable.spring.boot.EndpointAutoConfiguration;
import org.flowable.spring.boot.FlowableTransactionAutoConfiguration;
import org.flowable.spring.boot.ProcessEngineAutoConfiguration;
import org.flowable.spring.boot.actuate.endpoint.ProcessEngineEndpoint;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.EndpointWebMvcAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.ManagementServerPropertiesAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.MetricFilterAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.MetricRepositoryAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.web.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * @author Josh Long
 */
public class EndpointAutoConfigurationTest {

    @Configuration
    @Import({ EmbeddedServletContainerAutoConfiguration.class,
            DispatcherServletAutoConfiguration.class,
            ServerPropertiesAutoConfiguration.class,
            HttpMessageConvertersAutoConfiguration.class,
            WebMvcAutoConfiguration.class })
    public static class EmbeddedContainerConfiguration {
    }

    @Configuration
    @Import({ DataSourceAutoConfiguration.class,
            MetricFilterAutoConfiguration.class, EndpointWebMvcAutoConfiguration.class,
            ManagementServerPropertiesAutoConfiguration.class,
            MetricRepositoryAutoConfiguration.class,
            FlowableTransactionAutoConfiguration.class,
            ProcessEngineAutoConfiguration.class,
            EndpointAutoConfiguration.class })
    @PropertySource("test-actuator-endpoint.properties")
    public static class EndpointConfiguration {

        @Bean
        public RestTemplate restTemplate() {
            return new RestTemplate();
        }
    }

    @Test
    public void mvcEndpoint() throws Throwable {

        AnnotationConfigEmbeddedWebApplicationContext applicationContext = new AnnotationConfigEmbeddedWebApplicationContext(CallbackEmbeddedContainerCustomizer.class, EmbeddedContainerConfiguration.class, EndpointConfiguration.class);

        ProcessEngine processEngine = applicationContext.getBean(ProcessEngine.class);
        org.junit.Assert.assertNotNull("the processEngine should not be null", processEngine);

        ProcessEngineEndpoint processEngineEndpoint = applicationContext.getBean(ProcessEngineEndpoint.class);
        org.junit.Assert.assertNotNull("the processEngineEndpoint should not be null", processEngineEndpoint);

        RestTemplate restTemplate = applicationContext.getBean(RestTemplate.class);

        CallbackEmbeddedContainerCustomizer container = applicationContext.getBean(CallbackEmbeddedContainerCustomizer.class);

        ResponseEntity<Map> mapResponseEntity = restTemplate.getForEntity("http://localhost:"+ container.getPortNumber() +"/flowable/", Map.class);

        Map map = mapResponseEntity.getBody();

        String[] criticalKeys = { "completedTaskCount", "openTaskCount", "cachedProcessDefinitionCount" };

        Map<?, ?> invokedResults = processEngineEndpoint.invoke();
        for (String k : criticalKeys) {
            org.junit.Assert.assertTrue(map.containsKey(k));
            org.junit.Assert.assertEquals(((Number) map.get(k)).longValue(), ((Number) invokedResults.get(k)).longValue());
        }
    }

    @Component
    @PropertySource(
            value = {"classpath:properties/${property:defaultValue}.properties"},
            ignoreResourceNotFound = true)
    public static class CallbackEmbeddedContainerCustomizer implements EmbeddedServletContainerCustomizer {

        @Value("${portNumber:9091}")
        protected int portNumber;

        @Override
        public void customize(ConfigurableEmbeddedServletContainer container) {
            container.setPort(portNumber);
        }

        public int getPortNumber() {
            return portNumber;
        }

        /**
         * Property placeholder configurer needed to process @Value annotations
         */
        @Bean
        public static PropertySourcesPlaceholderConfigurer propertyConfigurer() {
            return new PropertySourcesPlaceholderConfigurer();
        }
    }
}
