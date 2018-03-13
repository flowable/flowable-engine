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

import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.dmn.engine.configurator.DmnEngineConfigurator;
import org.flowable.dmn.spring.SpringDmnEngineConfiguration;
import org.flowable.dmn.spring.configurator.SpringDmnEngineConfigurator;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.AbstractEngineAutoConfiguration;
import org.flowable.spring.boot.FlowableProperties;
import org.flowable.spring.boot.FlowableTransactionAutoConfiguration;
import org.flowable.spring.boot.ProcessEngineAutoConfiguration;
import org.flowable.spring.boot.ProcessEngineConfigurationConfigurer;
import org.flowable.spring.boot.condition.ConditionalOnDmnEngine;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.List;

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
})
@AutoConfigureBefore({
    ProcessEngineAutoConfiguration.class
})
public class DmnEngineAutoConfiguration extends AbstractEngineAutoConfiguration {

    protected final FlowableDmnProperties dmnProperties;

    public DmnEngineAutoConfiguration(FlowableProperties flowableProperties, FlowableDmnProperties dmnProperties) {
        super(flowableProperties);
        this.dmnProperties = dmnProperties;
    }

    @Bean
    @ConditionalOnMissingBean
    public SpringDmnEngineConfiguration dmnEngineConfiguration(
        DataSource dataSource,
        PlatformTransactionManager platformTransactionManager
    ) throws IOException {
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
        return configuration;
    }

    @Configuration
    @ConditionalOnClass(SpringProcessEngineConfiguration.class)
    public static class DmnEngineProcessConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "dmnProcessEngineConfigurationConfigurer")
        public ProcessEngineConfigurationConfigurer dmnProcessEngineConfigurationConfigurer(
            DmnEngineConfigurator dmnEngineConfigurator
        ) {
            return processEngineConfiguration -> processEngineConfiguration.addConfigurator(dmnEngineConfigurator);
        }

        @Bean
        @ConditionalOnMissingBean
        public DmnEngineConfigurator dmnEngineConfigurator(DmnEngineConfiguration configuration) {
            SpringDmnEngineConfigurator dmnEngineConfigurator = new SpringDmnEngineConfigurator();
            dmnEngineConfigurator.setDmnEngineConfiguration(configuration);
            return dmnEngineConfigurator;
        }
    }
}

