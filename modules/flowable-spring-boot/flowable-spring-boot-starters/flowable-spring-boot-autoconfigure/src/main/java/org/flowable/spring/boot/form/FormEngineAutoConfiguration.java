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
package org.flowable.spring.boot.form;

import java.io.IOException;
import java.util.List;

import javax.sql.DataSource;

import org.flowable.app.spring.SpringAppEngineConfiguration;
import org.flowable.form.engine.configurator.FormEngineConfigurator;
import org.flowable.form.spring.SpringFormEngineConfiguration;
import org.flowable.form.spring.configurator.SpringFormEngineConfigurator;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.AbstractSpringEngineAutoConfiguration;
import org.flowable.spring.boot.BaseEngineConfigurationWithConfigurers;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.flowable.spring.boot.FlowableProperties;
import org.flowable.spring.boot.FlowableTransactionAutoConfiguration;
import org.flowable.spring.boot.ProcessEngineAutoConfiguration;
import org.flowable.spring.boot.ProcessEngineServicesAutoConfiguration;
import org.flowable.spring.boot.app.AppEngineAutoConfiguration;
import org.flowable.spring.boot.app.AppEngineServicesAutoConfiguration;
import org.flowable.spring.boot.condition.ConditionalOnFormEngine;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;

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
    AppEngineAutoConfiguration.class,
    ProcessEngineAutoConfiguration.class,
})
@AutoConfigureBefore({
    AppEngineServicesAutoConfiguration.class,
    ProcessEngineServicesAutoConfiguration.class,
})
public class FormEngineAutoConfiguration extends AbstractSpringEngineAutoConfiguration {

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

        List<Resource> resources = this.discoverDeploymentResources(
            formProperties.getResourceLocation(),
            formProperties.getResourceSuffixes(),
            formProperties.isDeployResources()
        );

        if (resources != null && !resources.isEmpty()) {
            configuration.setDeploymentResources(resources.toArray(new Resource[0]));
            configuration.setDeploymentName(formProperties.getDeploymentName());
        }

        configureSpringEngine(configuration, platformTransactionManager);
        configureEngine(configuration, dataSource);

        return configuration;
    }

    @Configuration
    @ConditionalOnBean(type = {
        "org.flowable.spring.SpringProcessEngineConfiguration"
    })
    @ConditionalOnMissingBean(type = {
        "org.flowable.app.spring.SpringAppEngineConfiguration"
    })
    public static class FormEngineProcessConfiguration extends BaseEngineConfigurationWithConfigurers<SpringFormEngineConfiguration> {

        @Bean
        @ConditionalOnMissingBean(name = "formProcessEngineConfigurationConfigurer")
        public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> formProcessEngineConfigurationConfigurer(
            FormEngineConfigurator formEngineConfigurator) {
            
            return processEngineConfiguration -> processEngineConfiguration.addConfigurator(formEngineConfigurator);
        }

        @Bean
        @ConditionalOnMissingBean
        public FormEngineConfigurator formEngineConfigurator(SpringFormEngineConfiguration configuration) {
            SpringFormEngineConfigurator formEngineConfigurator = new SpringFormEngineConfigurator();
            formEngineConfigurator.setFormEngineConfiguration(configuration);
            
            invokeConfigurers(configuration);
            
            return formEngineConfigurator;
        }
    }
    
    @Configuration
    @ConditionalOnBean(type = {
        "org.flowable.app.spring.SpringAppEngineConfiguration"
    })
    public static class FormEngineAppEngineConfiguration extends BaseEngineConfigurationWithConfigurers<SpringFormEngineConfiguration> {

        @Bean
        @ConditionalOnMissingBean(name = "formAppEngineConfigurationConfigurer")
        public EngineConfigurationConfigurer<SpringAppEngineConfiguration> formAppEngineConfigurationConfigurer(
            FormEngineConfigurator formEngineConfigurator) {
            
            return appEngineConfiguration -> appEngineConfiguration.addConfigurator(formEngineConfigurator);
        }

        @Bean
        @ConditionalOnMissingBean
        public FormEngineConfigurator formEngineConfigurator(SpringFormEngineConfiguration configuration) {
            SpringFormEngineConfigurator formEngineConfigurator = new SpringFormEngineConfigurator();
            formEngineConfigurator.setFormEngineConfiguration(configuration);
            
            invokeConfigurers(configuration);
            
            return formEngineConfigurator;
        }
    }
}

