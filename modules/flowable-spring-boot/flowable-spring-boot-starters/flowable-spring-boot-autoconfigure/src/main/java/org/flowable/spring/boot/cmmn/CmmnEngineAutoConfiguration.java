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
package org.flowable.spring.boot.cmmn;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.flowable.app.spring.SpringAppEngineConfiguration;
import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.cmmn.engine.configurator.CmmnEngineConfigurator;
import org.flowable.cmmn.spring.SpringCmmnEngineConfiguration;
import org.flowable.cmmn.spring.autodeployment.DefaultAutoDeploymentStrategy;
import org.flowable.cmmn.spring.autodeployment.ResourceParentFolderAutoDeploymentStrategy;
import org.flowable.cmmn.spring.autodeployment.SingleResourceAutoDeploymentStrategy;
import org.flowable.cmmn.spring.configurator.SpringCmmnEngineConfigurator;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.spring.AutoDeploymentStrategy;
import org.flowable.common.spring.CommonAutoDeploymentProperties;
import org.flowable.common.spring.async.SpringAsyncTaskExecutor;
import org.flowable.http.common.api.client.FlowableHttpClient;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.flowable.job.service.impl.asyncexecutor.AsyncJobExecutorConfiguration;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.AbstractSpringEngineAutoConfiguration;
import org.flowable.spring.boot.BaseEngineConfigurationWithConfigurers;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.flowable.spring.boot.FlowableAutoDeploymentProperties;
import org.flowable.spring.boot.FlowableHttpProperties;
import org.flowable.spring.boot.FlowableJobConfiguration;
import org.flowable.spring.boot.FlowableMailProperties;
import org.flowable.spring.boot.FlowableProperties;
import org.flowable.spring.boot.ProcessEngineAutoConfiguration;
import org.flowable.spring.boot.ProcessEngineServicesAutoConfiguration;
import org.flowable.spring.boot.app.AppEngineAutoConfiguration;
import org.flowable.spring.boot.app.AppEngineServicesAutoConfiguration;
import org.flowable.spring.boot.app.FlowableAppProperties;
import org.flowable.spring.boot.condition.ConditionalOnCmmnEngine;
import org.flowable.spring.boot.eventregistry.FlowableEventRegistryProperties;
import org.flowable.spring.boot.idm.FlowableIdmProperties;
import org.flowable.spring.job.service.SpringAsyncExecutor;
import org.flowable.spring.job.service.SpringRejectedJobsHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration} for the CMMN engine
 *
 * @author Filip Hrisafov
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnCmmnEngine
@EnableConfigurationProperties({
    FlowableProperties.class,
    FlowableMailProperties.class,
    FlowableAutoDeploymentProperties.class,
    FlowableIdmProperties.class,
    FlowableEventRegistryProperties.class,
    FlowableCmmnProperties.class,
    FlowableAppProperties.class,
    FlowableHttpProperties.class
})
@AutoConfigureAfter(value = {
    AppEngineAutoConfiguration.class,
    ProcessEngineAutoConfiguration.class,
}, name = {
    "org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration"
})
@AutoConfigureBefore({
    AppEngineServicesAutoConfiguration.class,
    ProcessEngineServicesAutoConfiguration.class
})
@Import({
    FlowableJobConfiguration.class
})
public class CmmnEngineAutoConfiguration extends AbstractSpringEngineAutoConfiguration {

    protected final FlowableCmmnProperties cmmnProperties;
    protected final FlowableIdmProperties idmProperties;
    protected final FlowableEventRegistryProperties eventProperties;
    protected final FlowableMailProperties mailProperties;
    protected final FlowableHttpProperties httpProperties;
    protected final FlowableAutoDeploymentProperties autoDeploymentProperties;

    public CmmnEngineAutoConfiguration(FlowableProperties flowableProperties, FlowableCmmnProperties cmmnProperties, FlowableIdmProperties idmProperties,
                    FlowableEventRegistryProperties eventProperties, FlowableMailProperties mailProperties, FlowableHttpProperties httpProperties, FlowableAutoDeploymentProperties autoDeploymentProperties) {
        
        super(flowableProperties);
        this.cmmnProperties = cmmnProperties;
        this.idmProperties = idmProperties;
        this.eventProperties = eventProperties;
        this.mailProperties = mailProperties;
        this.httpProperties = httpProperties;
        this.autoDeploymentProperties = autoDeploymentProperties;
    }

    @Bean
    @Cmmn
    @ConfigurationProperties(prefix = "flowable.cmmn.async.executor")
    public AsyncJobExecutorConfiguration cmmnAsyncExecutorConfiguration() {
        return new AsyncJobExecutorConfiguration();
    }

    /**
     * The Async Executor must not be shared between the engines.
     * Therefore a dedicated one is always created.
     */
    @Bean
    @Cmmn
    @ConditionalOnMissingBean(name = "cmmnAsyncExecutor")
    public SpringAsyncExecutor cmmnAsyncExecutor(
        @Cmmn AsyncJobExecutorConfiguration executorConfiguration,
        ObjectProvider<SpringRejectedJobsHandler> rejectedJobsHandler,
        @Cmmn ObjectProvider<SpringRejectedJobsHandler> cmmnRejectedJobsHandler
    ) {
        SpringAsyncExecutor asyncExecutor = new SpringAsyncExecutor(executorConfiguration);
        asyncExecutor.setRejectedJobsHandler(getIfAvailable(cmmnRejectedJobsHandler, rejectedJobsHandler));
        return asyncExecutor;
    }

    @Bean
    @ConditionalOnMissingBean
    public SpringCmmnEngineConfiguration cmmnEngineConfiguration(DataSource dataSource, PlatformTransactionManager platformTransactionManager,
        ObjectProvider<ObjectMapper> objectMapperProvider,
        @Cmmn ObjectProvider<AsyncExecutor> asyncExecutorProvider,
        ObjectProvider<AsyncListenableTaskExecutor> taskExecutor,
        @Cmmn ObjectProvider<AsyncListenableTaskExecutor> cmmnTaskExecutor,
        @Qualifier("applicationTaskExecutor") ObjectProvider<AsyncListenableTaskExecutor> applicationTaskExecutorProvider,
        ObjectProvider<FlowableHttpClient> flowableHttpClient,
        ObjectProvider<List<AutoDeploymentStrategy<CmmnEngine>>> cmmnAutoDeploymentStrategies)
        throws IOException {
        
        SpringCmmnEngineConfiguration configuration = new SpringCmmnEngineConfiguration();

        List<Resource> resources = this.discoverDeploymentResources(
            cmmnProperties.getResourceLocation(),
            cmmnProperties.getResourceSuffixes(),
            cmmnProperties.isDeployResources()
        );

        if (resources != null && !resources.isEmpty()) {
            configuration.setDeploymentResources(resources.toArray(new Resource[0]));
            configuration.setDeploymentName(cmmnProperties.getDeploymentName());
        }

        AsyncExecutor asyncExecutor = asyncExecutorProvider.getIfUnique();
        if (asyncExecutor != null) {
            configuration.setAsyncExecutor(asyncExecutor);
        }

        AsyncListenableTaskExecutor asyncTaskExecutor = getIfAvailable(cmmnTaskExecutor, taskExecutor);
        if (asyncTaskExecutor == null) {
            // Get the applicationTaskExecutor
            asyncTaskExecutor = applicationTaskExecutorProvider.getObject();
        }
        if (asyncTaskExecutor != null) {
            // The task executors are shared
            org.flowable.common.engine.api.async.AsyncTaskExecutor flowableTaskExecutor = new SpringAsyncTaskExecutor(asyncTaskExecutor);
            configuration.setAsyncTaskExecutor(flowableTaskExecutor);
            configuration.setAsyncHistoryTaskExecutor(flowableTaskExecutor);
        }


        configureSpringEngine(configuration, platformTransactionManager);
        configureEngine(configuration, dataSource);
        ObjectMapper objectMapper = objectMapperProvider.getIfAvailable();
        if (objectMapper != null) {
            configuration.setObjectMapper(objectMapper);
        }

        configuration.setDeploymentName(defaultText(cmmnProperties.getDeploymentName(), configuration.getDeploymentName()));

        configuration.setDisableIdmEngine(!idmProperties.isEnabled());
        configuration.setDisableEventRegistry(!eventProperties.isEnabled());

        configuration.setAsyncExecutorActivate(flowableProperties.isAsyncExecutorActivate());

        configuration.setMailServerHost(mailProperties.getHost());
        configuration.setMailServerPort(mailProperties.getPort());
        configuration.setMailServerSSLPort(mailProperties.getSSLPort());
        configuration.setMailServerUsername(mailProperties.getUsername());
        configuration.setMailServerPassword(mailProperties.getPassword());
        configuration.setMailServerDefaultFrom(mailProperties.getDefaultFrom());
        configuration.setMailServerForceTo(mailProperties.getForceTo());
        configuration.setMailServerUseSSL(mailProperties.isUseSsl());
        configuration.setMailServerUseTLS(mailProperties.isUseTls());
        configuration.setMailServerDefaultCharset(mailProperties.getDefaultCharset());

        configuration.getHttpClientConfig().setUseSystemProperties(httpProperties.isUseSystemProperties());
        configuration.getHttpClientConfig().setConnectionRequestTimeout(httpProperties.getConnectionRequestTimeout());
        configuration.getHttpClientConfig().setConnectTimeout(httpProperties.getConnectTimeout());
        configuration.getHttpClientConfig().setDisableCertVerify(httpProperties.isDisableCertVerify());
        configuration.getHttpClientConfig().setRequestRetryLimit(httpProperties.getRequestRetryLimit());
        configuration.getHttpClientConfig().setSocketTimeout(httpProperties.getSocketTimeout());
        configuration.getHttpClientConfig().setHttpClient(flowableHttpClient.getIfAvailable());

        //TODO Can it have different then the Process engine?
        configuration.setHistoryLevel(flowableProperties.getHistoryLevel());

        configuration.setEnableSafeCmmnXml(cmmnProperties.isEnableSafeXml());
        configuration.setEventRegistryStartCaseInstanceAsync(cmmnProperties.isEventRegistryStartCaseInstanceAsync());

        configuration.setFormFieldValidationEnabled(flowableProperties.isFormFieldValidationEnabled());

        // We cannot use orderedStream since we want to support Boot 1.5 which is on pre 5.x Spring
        List<AutoDeploymentStrategy<CmmnEngine>> deploymentStrategies = cmmnAutoDeploymentStrategies.getIfAvailable();
        if (deploymentStrategies == null) {
            deploymentStrategies = new ArrayList<>();
        }
        CommonAutoDeploymentProperties deploymentProperties = this.autoDeploymentProperties.deploymentPropertiesForEngine(ScopeTypes.CMMN);
        // Always add the out of the box auto deployment strategies as last
        deploymentStrategies.add(new DefaultAutoDeploymentStrategy(deploymentProperties));
        deploymentStrategies.add(new SingleResourceAutoDeploymentStrategy(deploymentProperties));
        deploymentStrategies.add(new ResourceParentFolderAutoDeploymentStrategy(deploymentProperties));
        configuration.setDeploymentStrategies(deploymentStrategies);

        configuration.setEnableHistoryCleaning(flowableProperties.isEnableHistoryCleaning());
        configuration.setHistoryCleaningTimeCycleConfig(flowableProperties.getHistoryCleaningCycle());
        configuration.setCleanInstancesEndedAfter(flowableProperties.getHistoryCleaningAfter());
        configuration.setCleanInstancesBatchSize(flowableProperties.getHistoryCleaningBatchSize());
        configuration.setCleanInstancesSequentially(flowableProperties.isHistoryCleaningSequential());

        return configuration;
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBean(type = {
        "org.flowable.spring.SpringProcessEngineConfiguration"
    })
    @ConditionalOnMissingBean(type = {
        "org.flowable.app.spring.SpringAppEngineConfiguration"
    })
    public static class CmmnEngineProcessConfiguration extends BaseEngineConfigurationWithConfigurers<SpringCmmnEngineConfiguration> {

        @Bean
        @ConditionalOnMissingBean(name = "cmmnProcessEngineConfigurationConfigurer")
        public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> cmmnProcessEngineConfigurationConfigurer(
            CmmnEngineConfigurator cmmnEngineConfigurator) {
            return processEngineConfiguration -> processEngineConfiguration.addConfigurator(cmmnEngineConfigurator);
        }

        @Bean
        @ConditionalOnMissingBean
        public CmmnEngineConfigurator cmmnEngineConfigurator(SpringCmmnEngineConfiguration cmmnEngineConfiguration) {
            SpringCmmnEngineConfigurator cmmnEngineConfigurator = new SpringCmmnEngineConfigurator();
            cmmnEngineConfigurator.setCmmnEngineConfiguration(cmmnEngineConfiguration);

            cmmnEngineConfiguration.setDisableIdmEngine(true);
            cmmnEngineConfiguration.setDisableEventRegistry(true);
            
            invokeConfigurers(cmmnEngineConfiguration);
            
            return cmmnEngineConfigurator;
        }
    }
    
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBean(type = {
        "org.flowable.app.spring.SpringAppEngineConfiguration"
    })
    public static class CmmnEngineAppConfiguration extends BaseEngineConfigurationWithConfigurers<SpringCmmnEngineConfiguration> {

        @Bean
        @ConditionalOnMissingBean(name = "cmmnAppEngineConfigurationConfigurer")
        public EngineConfigurationConfigurer<SpringAppEngineConfiguration> cmmnAppEngineConfigurationConfigurer(CmmnEngineConfigurator cmmnEngineConfigurator) {
            return appEngineConfiguration -> appEngineConfiguration.addConfigurator(cmmnEngineConfigurator);
        }

        @Bean
        @ConditionalOnMissingBean
        public CmmnEngineConfigurator cmmnEngineConfigurator(SpringCmmnEngineConfiguration cmmnEngineConfiguration) {
            SpringCmmnEngineConfigurator cmmnEngineConfigurator = new SpringCmmnEngineConfigurator();
            cmmnEngineConfigurator.setCmmnEngineConfiguration(cmmnEngineConfiguration);

            cmmnEngineConfiguration.setDisableIdmEngine(true);
            cmmnEngineConfiguration.setDisableEventRegistry(true);
            
            invokeConfigurers(cmmnEngineConfiguration);
            
            return cmmnEngineConfigurator;
        }
    }
}
