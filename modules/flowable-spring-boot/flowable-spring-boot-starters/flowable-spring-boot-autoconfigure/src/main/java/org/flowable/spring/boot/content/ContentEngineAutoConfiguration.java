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
package org.flowable.spring.boot.content;

import javax.sql.DataSource;

import org.flowable.app.spring.SpringAppEngineConfiguration;
import org.flowable.content.engine.configurator.ContentEngineConfigurator;
import org.flowable.content.spring.SpringContentEngineConfiguration;
import org.flowable.content.spring.configurator.SpringContentEngineConfigurator;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.AbstractEngineAutoConfiguration;
import org.flowable.spring.boot.BaseEngineConfigurationWithConfigurers;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.flowable.spring.boot.FlowableProperties;
import org.flowable.spring.boot.FlowableTransactionAutoConfiguration;
import org.flowable.spring.boot.ProcessEngineAutoConfiguration;
import org.flowable.spring.boot.ProcessEngineServicesAutoConfiguration;
import org.flowable.spring.boot.app.AppEngineAutoConfiguration;
import org.flowable.spring.boot.app.AppEngineServicesAutoConfiguration;
import org.flowable.spring.boot.condition.ConditionalOnAppEngine;
import org.flowable.spring.boot.condition.ConditionalOnContentEngine;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration Auto-Configuration} for the Content Engine
 *
 * @author Filip Hrisafov
 */
@Configuration
@ConditionalOnContentEngine
@EnableConfigurationProperties({
    FlowableProperties.class,
    FlowableContentProperties.class
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
public class ContentEngineAutoConfiguration extends AbstractEngineAutoConfiguration {

    protected final FlowableContentProperties contentProperties;

    public ContentEngineAutoConfiguration(FlowableProperties flowableProperties, FlowableContentProperties contentProperties) {
        super(flowableProperties);
        this.contentProperties = contentProperties;
    }

    @Bean
    @ConditionalOnMissingBean
    public SpringContentEngineConfiguration contentEngineConfiguration(DataSource dataSource, PlatformTransactionManager platformTransactionManager) {
        SpringContentEngineConfiguration configuration = new SpringContentEngineConfiguration();

        configuration.setTransactionManager(platformTransactionManager);
        configureEngine(configuration, dataSource);

        FlowableContentProperties.Storage storage = contentProperties.getStorage();
        configuration.setContentRootFolder(storage.getRootFolder());
        configuration.setCreateContentRootFolder(storage.getCreateRoot());

        return configuration;
    }

    @Configuration
    @ConditionalOnBean(type = {
        "org.flowable.spring.SpringProcessEngineConfiguration"
    })
    @ConditionalOnMissingBean(type = {
        "org.flowable.app.spring.SpringAppEngineConfiguration"
    })
    public static class ContentEngineProcessConfiguration extends BaseEngineConfigurationWithConfigurers<SpringContentEngineConfiguration> {
        
        @Bean
        @ConditionalOnMissingBean(name = "contentProcessEngineConfigurationConfigurer")
        public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> contentProcessEngineConfigurationConfigurer(
            ContentEngineConfigurator contentEngineConfigurator) {
            return processEngineConfiguration -> processEngineConfiguration.addConfigurator(contentEngineConfigurator);
        }

        @Bean
        @ConditionalOnMissingBean
        public ContentEngineConfigurator contentEngineConfigurator(SpringContentEngineConfiguration configuration) {
            SpringContentEngineConfigurator contentEngineConfigurator = new SpringContentEngineConfigurator();
            contentEngineConfigurator.setContentEngineConfiguration(configuration);
            
            invokeConfigurers(configuration);
            
            return contentEngineConfigurator;
        }
    }
    
    @Configuration
    @ConditionalOnAppEngine
    @ConditionalOnBean(type = {
        "org.flowable.app.spring.SpringAppEngineConfiguration"
    })
    public static class ContentEngineAppConfiguration extends BaseEngineConfigurationWithConfigurers<SpringContentEngineConfiguration> {
        
        @Bean
        @ConditionalOnMissingBean(name = "contentAppEngineConfigurationConfigurer")
        public EngineConfigurationConfigurer<SpringAppEngineConfiguration> contentAppEngineConfigurationConfigurer(
                        ContentEngineConfigurator contentEngineConfigurator) {
            
            return appEngineConfiguration -> appEngineConfiguration.addConfigurator(contentEngineConfigurator);
        }

        @Bean
        @ConditionalOnMissingBean
        public ContentEngineConfigurator contentEngineConfigurator(SpringContentEngineConfiguration configuration) {
            SpringContentEngineConfigurator contentEngineConfigurator = new SpringContentEngineConfigurator();
            contentEngineConfigurator.setContentEngineConfiguration(configuration);
            
            invokeConfigurers(configuration);
            
            return contentEngineConfigurator;
        }
    }
}
