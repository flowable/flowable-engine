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
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.flowable.app.spring.SpringAppEngineConfiguration;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.cfg.IdGenerator;
import org.flowable.common.engine.impl.persistence.StrongUuidGenerator;
import org.flowable.common.spring.AutoDeploymentStrategy;
import org.flowable.common.spring.CommonAutoDeploymentProperties;
import org.flowable.common.spring.async.SpringAsyncTaskExecutor;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.configurator.ProcessEngineConfigurator;
import org.flowable.engine.spring.configurator.SpringProcessEngineConfigurator;
import org.flowable.http.common.api.client.FlowableHttpClient;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.flowable.job.service.impl.asyncexecutor.AsyncJobExecutorConfiguration;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.app.AppEngineAutoConfiguration;
import org.flowable.spring.boot.app.AppEngineServicesAutoConfiguration;
import org.flowable.spring.boot.app.FlowableAppProperties;
import org.flowable.spring.boot.condition.ConditionalOnProcessEngine;
import org.flowable.spring.boot.eventregistry.FlowableEventRegistryProperties;
import org.flowable.spring.boot.idm.FlowableIdmProperties;
import org.flowable.spring.boot.process.FlowableProcessProperties;
import org.flowable.spring.boot.process.Process;
import org.flowable.spring.boot.process.ProcessAsync;
import org.flowable.spring.boot.process.ProcessAsyncHistory;
import org.flowable.spring.configurator.DefaultAutoDeploymentStrategy;
import org.flowable.spring.configurator.ResourceParentFolderAutoDeploymentStrategy;
import org.flowable.spring.configurator.SingleResourceAutoDeploymentStrategy;
import org.flowable.spring.job.service.SpringAsyncExecutor;
import org.flowable.spring.job.service.SpringAsyncHistoryExecutor;
import org.flowable.spring.job.service.SpringRejectedJobsHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
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
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Provides sane definitions for the various beans required to be productive with Flowable in Spring.
 *
 * @author Josh Long
 * @author Filip Hrisafov
 * @author Javier Casal
 * @author Joram Barrez
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProcessEngine
@EnableConfigurationProperties({
    FlowableAutoDeploymentProperties.class,
    FlowableProperties.class,
    FlowableMailProperties.class,
    FlowableHttpProperties.class,
    FlowableProcessProperties.class,
    FlowableAppProperties.class,
    FlowableIdmProperties.class,
    FlowableEventRegistryProperties.class
})
@AutoConfigureAfter(value = {
    FlowableJpaAutoConfiguration.class,
    AppEngineAutoConfiguration.class,
}, name = {
    "org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration"
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
    protected final FlowableEventRegistryProperties eventProperties;
    protected final FlowableMailProperties mailProperties;
    protected final FlowableHttpProperties httpProperties;
    protected final FlowableAutoDeploymentProperties autoDeploymentProperties;

    public ProcessEngineAutoConfiguration(FlowableProperties flowableProperties, FlowableProcessProperties processProperties,
        FlowableAppProperties appProperties, FlowableIdmProperties idmProperties, 
        FlowableEventRegistryProperties eventProperties, FlowableMailProperties mailProperties,
        FlowableHttpProperties httpProperties, FlowableAutoDeploymentProperties autoDeploymentProperties) {
        
        super(flowableProperties);
        this.processProperties = processProperties;
        this.appProperties = appProperties;
        this.idmProperties = idmProperties;
        this.eventProperties = eventProperties;
        this.mailProperties = mailProperties;
        this.httpProperties = httpProperties;
        this.autoDeploymentProperties = autoDeploymentProperties;
    }

    @Bean
    @ProcessAsync
    @ConfigurationProperties(prefix = "flowable.process.async.executor")
    public AsyncJobExecutorConfiguration processAsyncExecutorConfiguration() {
        return new AsyncJobExecutorConfiguration();
    }

    /**
     * The Async Executor must not be shared between the engines.
     * Therefore a dedicated one is always created.
     */
    @Bean
    @ProcessAsync
    @ConditionalOnMissingBean(name = "processAsyncExecutor")
    public SpringAsyncExecutor processAsyncExecutor(
        @ProcessAsync AsyncJobExecutorConfiguration executorConfiguration,
        ObjectProvider<SpringRejectedJobsHandler> rejectedJobsHandler,
        @Process ObjectProvider<SpringRejectedJobsHandler> processRejectedJobsHandler
    ) {
        SpringAsyncExecutor asyncExecutor = new SpringAsyncExecutor(executorConfiguration);
        asyncExecutor.setRejectedJobsHandler(getIfAvailable(processRejectedJobsHandler, rejectedJobsHandler));
        return asyncExecutor;
    }
    
    @Bean
    @ProcessAsyncHistory
    @ConfigurationProperties(prefix = "flowable.process.async-history.executor")
    @ConditionalOnProperty(prefix = "flowable.process", name = "async-history.enable")
    public AsyncJobExecutorConfiguration processAsyncHistoryExecutorConfiguration() {
        return new AsyncJobExecutorConfiguration();
    }

    @Bean
    @ProcessAsyncHistory
    @ConditionalOnMissingBean(name = "asyncHistoryExecutor")
    @ConditionalOnProperty(prefix = "flowable.process", name = "async-history.enable")
    public SpringAsyncHistoryExecutor asyncHistoryExecutor(
        @ProcessAsyncHistory AsyncJobExecutorConfiguration executorConfiguration,
        ObjectProvider<SpringRejectedJobsHandler> rejectedJobsHandler,
        @Process ObjectProvider<SpringRejectedJobsHandler> processRejectedJobsHandler
    ) {
        SpringAsyncHistoryExecutor asyncHistoryExecutor = new SpringAsyncHistoryExecutor(executorConfiguration);
        asyncHistoryExecutor.setRejectedJobsHandler(getIfAvailable(processRejectedJobsHandler, rejectedJobsHandler));
        return asyncHistoryExecutor;
    }

    @Bean
    @ConditionalOnMissingBean
    public SpringProcessEngineConfiguration springProcessEngineConfiguration(DataSource dataSource, PlatformTransactionManager platformTransactionManager,
            ObjectProvider<ObjectMapper> objectMapperProvider,
            @Process ObjectProvider<IdGenerator> processIdGenerator,
            ObjectProvider<IdGenerator> globalIdGenerator,
            @ProcessAsync ObjectProvider<AsyncExecutor> asyncExecutorProvider,
            @Qualifier("applicationTaskExecutor") ObjectProvider<AsyncListenableTaskExecutor> applicationTaskExecutorProvider,
            @ProcessAsyncHistory ObjectProvider<AsyncExecutor> asyncHistoryExecutorProvider,
            ObjectProvider<AsyncListenableTaskExecutor> taskExecutor,
            @Process ObjectProvider<AsyncListenableTaskExecutor> processTaskExecutor,
            ObjectProvider<FlowableHttpClient> flowableHttpClient,
            ObjectProvider<List<AutoDeploymentStrategy<ProcessEngine>>> processEngineAutoDeploymentStrategies) throws IOException {

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

        AsyncListenableTaskExecutor asyncTaskExecutor = getIfAvailable(processTaskExecutor, taskExecutor);
        if (asyncTaskExecutor == null) {
            // Get the applicationTaskExecutor
            asyncTaskExecutor = applicationTaskExecutorProvider.getObject();
        }
        if (asyncTaskExecutor != null) {
            // The task executors are shared
            org.flowable.common.engine.api.async.AsyncTaskExecutor flowableTaskExecutor = new SpringAsyncTaskExecutor(asyncTaskExecutor);
            conf.setAsyncTaskExecutor(flowableTaskExecutor);
            conf.setAsyncHistoryTaskExecutor(flowableTaskExecutor);
        }

        AsyncExecutor springAsyncHistoryExecutor = asyncHistoryExecutorProvider.getIfUnique();
        if (springAsyncHistoryExecutor != null) {
            conf.setAsyncHistoryEnabled(true);
            conf.setAsyncHistoryExecutor(springAsyncHistoryExecutor);
        }

        ObjectMapper objectMapper = objectMapperProvider.getIfAvailable();
        if (objectMapper != null) {
            conf.setObjectMapper(objectMapper);
        }
        configureSpringEngine(conf, platformTransactionManager);
        configureEngine(conf, dataSource);

        conf.setDeploymentName(defaultText(flowableProperties.getDeploymentName(), conf.getDeploymentName()));

        conf.setDisableIdmEngine(!(flowableProperties.isDbIdentityUsed() && idmProperties.isEnabled()));
        conf.setDisableEventRegistry(!eventProperties.isEnabled());

        conf.setAsyncExecutorActivate(flowableProperties.isAsyncExecutorActivate());
        conf.setAsyncHistoryExecutorActivate(flowableProperties.isAsyncHistoryExecutorActivate());

        conf.setMailServerHost(mailProperties.getHost());
        conf.setMailServerPort(mailProperties.getPort());
        conf.setMailServerSSLPort(mailProperties.getSSLPort());
        conf.setMailServerUsername(mailProperties.getUsername());
        conf.setMailServerPassword(mailProperties.getPassword());
        conf.setMailServerDefaultFrom(mailProperties.getDefaultFrom());
        conf.setMailServerForceTo(mailProperties.getForceTo());
        conf.setMailServerUseSSL(mailProperties.isUseSsl());
        conf.setMailServerUseTLS(mailProperties.isUseTls());
        conf.setMailServerDefaultCharset(mailProperties.getDefaultCharset());

        conf.getHttpClientConfig().setUseSystemProperties(httpProperties.isUseSystemProperties());
        conf.getHttpClientConfig().setConnectionRequestTimeout(httpProperties.getConnectionRequestTimeout());
        conf.getHttpClientConfig().setConnectTimeout(httpProperties.getConnectTimeout());
        conf.getHttpClientConfig().setDisableCertVerify(httpProperties.isDisableCertVerify());
        conf.getHttpClientConfig().setRequestRetryLimit(httpProperties.getRequestRetryLimit());
        conf.getHttpClientConfig().setSocketTimeout(httpProperties.getSocketTimeout());
        conf.getHttpClientConfig().setHttpClient(flowableHttpClient.getIfAvailable());

        conf.setEnableProcessDefinitionHistoryLevel(processProperties.isEnableProcessDefinitionHistoryLevel());
        conf.setProcessDefinitionCacheLimit(processProperties.getDefinitionCacheLimit());
        conf.setEnableSafeBpmnXml(processProperties.isEnableSafeXml());
        conf.setEventRegistryStartProcessInstanceAsync(processProperties.isEventRegistryStartProcessInstanceAsync());

        conf.setHistoryLevel(flowableProperties.getHistoryLevel());
        
        conf.setActivityFontName(flowableProperties.getActivityFontName());
        conf.setAnnotationFontName(flowableProperties.getAnnotationFontName());
        conf.setLabelFontName(flowableProperties.getLabelFontName());

        conf.setFormFieldValidationEnabled(flowableProperties.isFormFieldValidationEnabled());

        conf.setEnableHistoryCleaning(flowableProperties.isEnableHistoryCleaning());
        conf.setHistoryCleaningTimeCycleConfig(flowableProperties.getHistoryCleaningCycle());
        conf.setCleanInstancesEndedAfter(flowableProperties.getHistoryCleaningAfter());
        conf.setCleanInstancesBatchSize(flowableProperties.getHistoryCleaningBatchSize());
        conf.setCleanInstancesSequentially(flowableProperties.isHistoryCleaningSequential());

        IdGenerator idGenerator = getIfAvailable(processIdGenerator, globalIdGenerator);
        if (idGenerator == null) {
            idGenerator = new StrongUuidGenerator();
        }
        conf.setIdGenerator(idGenerator);

        // We cannot use orderedStream since we want to support Boot 1.5 which is on pre 5.x Spring
        List<AutoDeploymentStrategy<ProcessEngine>> deploymentStrategies = processEngineAutoDeploymentStrategies.getIfAvailable();
        if (deploymentStrategies == null) {
            deploymentStrategies = new ArrayList<>();
        }
        CommonAutoDeploymentProperties deploymentProperties = this.autoDeploymentProperties.deploymentPropertiesForEngine(ScopeTypes.BPMN);

        // Always add the out of the box auto deployment strategies as last
        deploymentStrategies.add(new DefaultAutoDeploymentStrategy(deploymentProperties));
        deploymentStrategies.add(new SingleResourceAutoDeploymentStrategy(deploymentProperties));
        deploymentStrategies.add(new ResourceParentFolderAutoDeploymentStrategy(deploymentProperties));
        conf.setDeploymentStrategies(deploymentStrategies);

        return conf;
    }
    
    @Configuration(proxyBeanMethods = false)
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
            processEngineConfiguration.setDisableEventRegistry(true);
            
            invokeConfigurers(processEngineConfiguration);
            
            return processEngineConfigurator;
        }
    }
}
