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
package org.flowable.spring.boot;

import java.io.IOException;
import java.util.List;

import javax.sql.DataSource;

import org.flowable.app.spring.SpringAppEngineConfiguration;
import org.flowable.common.engine.impl.persistence.StrongUuidGenerator;
import org.flowable.engine.configurator.ProcessEngineConfigurator;
import org.flowable.engine.spring.configurator.SpringProcessEngineConfigurator;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.app.AppEngineAutoConfiguration;
import org.flowable.spring.boot.app.AppEngineServicesAutoConfiguration;
import org.flowable.spring.boot.app.FlowableAppProperties;
import org.flowable.spring.boot.condition.ConditionalOnProcessEngine;
import org.flowable.spring.boot.idm.FlowableIdmProperties;
import org.flowable.spring.boot.process.FlowableProcessProperties;
import org.flowable.spring.boot.process.Process;
import org.flowable.spring.boot.process.ProcessAsync;
import org.flowable.spring.boot.process.ProcessAsyncHistory;
import org.flowable.spring.job.service.SpringAsyncExecutor;
import org.flowable.spring.job.service.SpringAsyncHistoryExecutor;
import org.flowable.spring.job.service.SpringRejectedJobsHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Provides sane definitions for the various beans required to be productive with Flowable in Spring.
 *
 * @author Josh Long
 * @author Filip Hrisafov
 * @author Javier Casal
 * @author Joram Barrez
 */
@Configuration
@ConditionalOnProcessEngine
@EnableConfigurationProperties({
    FlowableProperties.class,
    FlowableMailProperties.class,
    FlowableProcessProperties.class,
    FlowableAppProperties.class,
    FlowableIdmProperties.class
})
@AutoConfigureAfter({
    FlowableTransactionAutoConfiguration.class,
    AppEngineAutoConfiguration.class,
})
@AutoConfigureBefore({
    AppEngineServicesAutoConfiguration.class,
})
@Import({
    FlowableJobConfiguration.class
})
public class ProcessEngineAutoConfiguration extends AbstractSpringEngineAutoConfiguration {

    protected final FlowableProcessProperties processProperties;
    protected final FlowableAppProperties appProperties;
    protected final FlowableIdmProperties idmProperties;
    protected final FlowableMailProperties mailProperties;

    public ProcessEngineAutoConfiguration(FlowableProperties flowableProperties, FlowableProcessProperties processProperties,
                    FlowableAppProperties appProperties, FlowableIdmProperties idmProperties, FlowableMailProperties mailProperties) {
        
        super(flowableProperties);
        this.processProperties = processProperties;
        this.appProperties = appProperties;
        this.idmProperties = idmProperties;
        this.mailProperties = mailProperties;
    }

    /**
     * The Async Executor must not be shared between the engines.
     * Therefore a dedicated one is always created.
     */
    @Bean
    @ProcessAsync
    @ConfigurationProperties(prefix = "flowable.process.async.executor")
    @ConditionalOnMissingBean(name = "processAsyncExecutor")
    public SpringAsyncExecutor processAsyncExecutor(
        ObjectProvider<TaskExecutor> taskExecutor,
        @Process ObjectProvider<TaskExecutor> processTaskExecutor,
        ObjectProvider<SpringRejectedJobsHandler> rejectedJobsHandler,
        @Process ObjectProvider<SpringRejectedJobsHandler> processRejectedJobsHandler
    ) {
        return new SpringAsyncExecutor(
            getIfAvailable(processTaskExecutor, taskExecutor),
            getIfAvailable(processRejectedJobsHandler, rejectedJobsHandler)
        );
    }
    
    @Bean
    @ProcessAsyncHistory
    @ConfigurationProperties(prefix = "flowable.process.async-history.executor")
    @ConditionalOnMissingBean(name = "asyncHistoryExecutor")
    @ConditionalOnProperty(prefix = "flowable.process", name = "async-history.enable")
    public SpringAsyncHistoryExecutor asyncHistoryExecutor(
        ObjectProvider<TaskExecutor> taskExecutor,
        @Process ObjectProvider<TaskExecutor> processTaskExecutor,
        ObjectProvider<SpringRejectedJobsHandler> rejectedJobsHandler,
        @Process ObjectProvider<SpringRejectedJobsHandler> processRejectedJobsHandler
    ) {
        return new SpringAsyncHistoryExecutor(
            getIfAvailable(processTaskExecutor, taskExecutor),
            getIfAvailable(processRejectedJobsHandler, rejectedJobsHandler)
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public SpringProcessEngineConfiguration springProcessEngineConfiguration(DataSource dataSource, PlatformTransactionManager platformTransactionManager,
            @ProcessAsync ObjectProvider<AsyncExecutor> asyncExecutorProvider, 
            @ProcessAsyncHistory ObjectProvider<AsyncExecutor> asyncHistoryExecutorProvider) throws IOException {

        SpringProcessEngineConfiguration conf = new SpringProcessEngineConfiguration();

        List<Resource> resources = this.discoverDeploymentResources(
            flowableProperties.getProcessDefinitionLocationPrefix(),
            flowableProperties.getProcessDefinitionLocationSuffixes(),
            flowableProperties.isCheckProcessDefinitions()
        );

        if (resources != null && !resources.isEmpty()) {
            conf.setDeploymentResources(resources.toArray(new Resource[0]));
            conf.setDeploymentName(flowableProperties.getDeploymentName());
        }

        AsyncExecutor springAsyncExecutor = asyncExecutorProvider.getIfUnique();
        if (springAsyncExecutor != null) {
            conf.setAsyncExecutor(springAsyncExecutor);
        }
        
        AsyncExecutor springAsyncHistoryExecutor = asyncHistoryExecutorProvider.getIfUnique();
        if (springAsyncHistoryExecutor != null) {
            conf.setAsyncHistoryEnabled(true);
            conf.setAsyncHistoryExecutor(springAsyncHistoryExecutor);
        }

        configureSpringEngine(conf, platformTransactionManager);
        configureEngine(conf, dataSource);

        conf.setDeploymentName(defaultText(flowableProperties.getDeploymentName(), conf.getDeploymentName()));

        conf.setDisableIdmEngine(!(flowableProperties.isDbIdentityUsed() && idmProperties.isEnabled()));

        conf.setAsyncExecutorActivate(flowableProperties.isAsyncExecutorActivate());
        conf.setAsyncHistoryExecutorActivate(flowableProperties.isAsyncHistoryExecutorActivate());

        conf.setMailServerHost(mailProperties.getHost());
        conf.setMailServerPort(mailProperties.getPort());
        conf.setMailServerUsername(mailProperties.getUsername());
        conf.setMailServerPassword(mailProperties.getPassword());
        conf.setMailServerDefaultFrom(mailProperties.getDefaultFrom());
        conf.setMailServerUseSSL(mailProperties.isUseSsl());
        conf.setMailServerUseTLS(mailProperties.isUseTls());

        conf.setEnableProcessDefinitionHistoryLevel(processProperties.isEnableProcessDefinitionHistoryLevel());
        conf.setProcessDefinitionCacheLimit(processProperties.getDefinitionCacheLimit());
        conf.setEnableSafeBpmnXml(processProperties.isEnableSafeXml());

        conf.setHistoryLevel(flowableProperties.getHistoryLevel());

        conf.setIdGenerator(new StrongUuidGenerator());

        return conf;
    }
    
    @Configuration
    @ConditionalOnBean(type = {
        "org.flowable.app.spring.SpringAppEngineConfiguration"
    })
    public static class ProcessEngineAppConfiguration extends BaseEngineConfigurationWithConfigurers<SpringProcessEngineConfiguration> {

        @Bean
        @ConditionalOnMissingBean(name = "processAppEngineConfigurationConfigurer")
        public EngineConfigurationConfigurer<SpringAppEngineConfiguration> processAppEngineConfigurationConfigurer(ProcessEngineConfigurator processEngineConfigurator) {
            return appEngineConfiguration -> appEngineConfiguration.addConfigurator(processEngineConfigurator);
        }

        @Bean
        @ConditionalOnMissingBean
        public ProcessEngineConfigurator processEngineConfigurator(SpringProcessEngineConfiguration processEngineConfiguration) {
            SpringProcessEngineConfigurator processEngineConfigurator = new SpringProcessEngineConfigurator();
            processEngineConfigurator.setProcessEngineConfiguration(processEngineConfiguration);
            
            processEngineConfiguration.setDisableIdmEngine(true);
            
            invokeConfigurers(processEngineConfiguration);
            
            return processEngineConfigurator;
        }
    }
}
