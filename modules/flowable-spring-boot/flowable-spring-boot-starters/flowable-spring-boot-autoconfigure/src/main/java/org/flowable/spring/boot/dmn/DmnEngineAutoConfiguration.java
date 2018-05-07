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
package org.flowable.spring.boot.dmn;

import java.io.IOException;
import java.util.List;

import javax.sql.DataSource;

import org.flowable.app.spring.SpringAppEngineConfiguration;
import org.flowable.dmn.engine.configurator.DmnEngineConfigurator;
import org.flowable.dmn.spring.SpringDmnEngineConfiguration;
import org.flowable.dmn.spring.configurator.SpringDmnEngineConfigurator;
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
import org.flowable.spring.boot.condition.ConditionalOnDmnEngine;
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
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration} for the Dmn engine
 *
 * @author Filip Hrisafov
 */
@Configuration
@ConditionalOnDmnEngine
@EnableConfigurationProperties({
    FlowableProperties.class,
    FlowableDmnProperties.class
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
public class DmnEngineAutoConfiguration extends AbstractSpringEngineAutoConfiguration {

    protected final FlowableDmnProperties dmnProperties;

    public DmnEngineAutoConfiguration(FlowableProperties flowableProperties, FlowableDmnProperties dmnProperties) {
        super(flowableProperties);
        this.dmnProperties = dmnProperties;
    }

    @Bean
    @ConditionalOnMissingBean
    public SpringDmnEngineConfiguration dmnEngineConfiguration(DataSource dataSource, PlatformTransactionManager platformTransactionManager) throws IOException {
        SpringDmnEngineConfiguration configuration = new SpringDmnEngineConfiguration();

        List<Resource> resources = this.discoverDeploymentResources(
            dmnProperties.getResourceLocation(),
            dmnProperties.getResourceSuffixes(),
            dmnProperties.isDeployResources()
        );

        if (resources != null && !resources.isEmpty()) {
            configuration.setDeploymentResources(resources.toArray(new Resource[0]));
            configuration.setDeploymentName(dmnProperties.getDeploymentName());
        }


        configureSpringEngine(configuration, platformTransactionManager);
        configureEngine(configuration, dataSource);

        configuration.setHistoryEnabled(dmnProperties.isHistoryEnabled());
        configuration.setEnableSafeDmnXml(dmnProperties.isEnableSafeXml());
        configuration.setStrictMode(dmnProperties.isStrictMode());

        return configuration;
    }

    @Configuration
    @ConditionalOnBean(type = {
        "org.flowable.spring.SpringProcessEngineConfiguration"
    })
    @ConditionalOnMissingBean(type = {
        "org.flowable.app.spring.SpringAppEngineConfiguration"
    })
    public static class DmnEngineProcessConfiguration extends BaseEngineConfigurationWithConfigurers<SpringDmnEngineConfiguration> {

        @Bean
        @ConditionalOnMissingBean(name = "dmnProcessEngineConfigurationConfigurer")
        public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> dmnProcessEngineConfigurationConfigurer(
            DmnEngineConfigurator dmnEngineConfigurator
        ) {
            return processEngineConfiguration -> processEngineConfiguration.addConfigurator(dmnEngineConfigurator);
        }

        @Bean
        @ConditionalOnMissingBean
        public DmnEngineConfigurator dmnEngineConfigurator(SpringDmnEngineConfiguration configuration) {
            SpringDmnEngineConfigurator dmnEngineConfigurator = new SpringDmnEngineConfigurator();
            dmnEngineConfigurator.setDmnEngineConfiguration(configuration);
            
            invokeConfigurers(configuration);
            
            return dmnEngineConfigurator;
        }
    }
    
    @Configuration
    @ConditionalOnBean(type = {
        "org.flowable.app.spring.SpringAppEngineConfiguration"
    })
    public static class DmnEngineAppConfiguration extends BaseEngineConfigurationWithConfigurers<SpringDmnEngineConfiguration> {

        @Bean
        @ConditionalOnMissingBean(name = "dmnAppEngineConfigurationConfigurer")
        public EngineConfigurationConfigurer<SpringAppEngineConfiguration> dmnAppEngineConfigurationConfigurer(
            DmnEngineConfigurator dmnEngineConfigurator
        ) {
            return appEngineConfiguration -> appEngineConfiguration.addConfigurator(dmnEngineConfigurator);
        }

        @Bean
        @ConditionalOnMissingBean
        public DmnEngineConfigurator dmnEngineConfigurator(SpringDmnEngineConfiguration configuration) {
            SpringDmnEngineConfigurator dmnEngineConfigurator = new SpringDmnEngineConfigurator();
            dmnEngineConfigurator.setDmnEngineConfiguration(configuration);
            
            invokeConfigurers(configuration);
            
            return dmnEngineConfigurator;
        }
    }
}

