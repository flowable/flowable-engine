package org.flowable.spring.boot;

import org.flowable.engine.ProcessEngine;
import org.flowable.form.api.FormEngineConfigurationApi;
import org.flowable.form.api.FormManagementService;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.api.FormService;
import org.flowable.form.engine.FormEngine;
import org.flowable.form.engine.FormEngineConfiguration;
import org.flowable.form.spring.FormEngineFactoryBean;
import org.flowable.spring.boot.condition.ConditionalOnFormEngine;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    FormEngineAutoConfiguration.class,
    ProcessEngineAutoConfiguration.class
})
public class FormEngineServicesAutoConfiguration {

    /**
     * If there is no process engine configuration, then trigger a creation of the form engine.
     */
    @Bean
    @ConditionalOnMissingBean({
        FormEngine.class,
        ProcessEngine.class
    })
    public FormEngineFactoryBean formEngine(FormEngineConfiguration formEngineConfiguration) {
        FormEngineFactoryBean factory = new FormEngineFactoryBean();
        factory.setFormEngineConfiguration(formEngineConfiguration);
        return factory;
    }

    @Bean
    public FormService formService(FormEngineConfigurationApi formEngine) {
        return formEngine.getFormService();
    }

    @Bean
    public FormRepositoryService formRepositoryService(FormEngineConfigurationApi formEngine) {
        return formEngine.getFormRepositoryService();
    }

    @Bean
    public FormManagementService formManagementService(FormEngineConfigurationApi formEngine) {
        return formEngine.getFormManagementService();
    }
}

