package org.flowable.spring.boot;

import org.flowable.form.engine.FormEngineConfiguration;
import org.flowable.form.engine.configurator.FormEngineConfigurator;
import org.flowable.form.spring.SpringFormEngineConfiguration;
import org.flowable.form.spring.configurator.SpringFormEngineConfigurator;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.condition.ConditionalOnFormEngine;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.IOException;

/**
 * Auto configuration for the form engine.
 *
 * @author Filip Hrisafov
 * @author Javier Casal
 */
@Configuration
@ConditionalOnFormEngine
@EnableConfigurationProperties({
    FlowableProperties.class,
    FlowableFormProperties.class
})
@AutoConfigureAfter({
    FlowableTransactionAutoConfiguration.class,
})
@AutoConfigureBefore({
    ProcessEngineAutoConfiguration.class
})
public class FormEngineAutoConfiguration extends AbstractEngineAutoConfiguration {

    protected final FlowableFormProperties formProperties;

    public FormEngineAutoConfiguration(FlowableProperties flowableProperties, FlowableFormProperties formProperties) {
        super(flowableProperties);
        this.formProperties = formProperties;
    }

    @Bean
    @ConditionalOnMissingBean
    public SpringFormEngineConfiguration formEngineConfiguration(
        DataSource dataSource,
        PlatformTransactionManager platformTransactionManager
    ) throws IOException {
        SpringFormEngineConfiguration configuration = new SpringFormEngineConfiguration();

        //TODO this should use auto deployment discovery. See #832

        configureSpringEngine(configuration, platformTransactionManager);
        configureEngine(configuration, dataSource);
        return configuration;
    }

    @Configuration
    @ConditionalOnClass(SpringProcessEngineConfiguration.class)
    public static class FormEngineProcessConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "formProcessEngineConfigurationConfigurer")
        public ProcessEngineConfigurationConfigurer formProcessEngineConfigurationConfigurer(
            FormEngineConfigurator formEngineConfigurator
        ) {
            return processEngineConfiguration -> processEngineConfiguration.addConfigurator(formEngineConfigurator);
        }

        @Bean
        @ConditionalOnMissingBean
        public FormEngineConfigurator formEngineConfigurator(FormEngineConfiguration configuration) {
            SpringFormEngineConfigurator formEngineConfigurator = new SpringFormEngineConfigurator();
            formEngineConfigurator.setFormEngineConfiguration(configuration);
            return formEngineConfigurator;
        }
    }
}

