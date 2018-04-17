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

import org.flowable.common.engine.impl.persistence.StrongUuidGenerator;
import org.flowable.engine.DynamicBpmnService;
import org.flowable.engine.FormService;
import org.flowable.engine.HistoryService;
import org.flowable.engine.IdentityService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.flowable.spring.ProcessEngineFactoryBean;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.condition.ConditionalOnProcessEngine;
import org.flowable.spring.boot.idm.FlowableIdmProperties;
import org.flowable.spring.boot.process.FlowableProcessProperties;
import org.flowable.spring.boot.process.Process;
import org.flowable.spring.job.service.SpringAsyncExecutor;
import org.flowable.spring.job.service.SpringRejectedJobsHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
 */
@Configuration
@ConditionalOnProcessEngine
@EnableConfigurationProperties({
    FlowableProperties.class,
    FlowableMailProperties.class,
    FlowableProcessProperties.class,
    FlowableIdmProperties.class
})
@AutoConfigureAfter({
    FlowableTransactionAutoConfiguration.class
})
@Import({
    FlowableJobConfiguration.class
})
public class ProcessEngineAutoConfiguration extends AbstractSpringEngineAutoConfiguration {

    @Autowired(required = false)
    private List<EngineConfigurationConfigurer<SpringProcessEngineConfiguration>> processEngineConfigurationConfigurers = new ArrayList<>();
    protected final FlowableProcessProperties processProperties;
    protected final FlowableIdmProperties idmProperties;
    protected final FlowableMailProperties mailProperties;

    public ProcessEngineAutoConfiguration(FlowableProperties flowableProperties, FlowableProcessProperties processProperties,
        FlowableIdmProperties idmProperties, FlowableMailProperties mailProperties) {
        super(flowableProperties);
        this.processProperties = processProperties;
        this.idmProperties = idmProperties;
        this.mailProperties = mailProperties;
    }

    /**
     * The Async Executor must not be shared between the engines.
     * Therefore a dedicated one is always created.
     */
    @Bean
    @Process
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
    @ConditionalOnMissingBean
    public SpringProcessEngineConfiguration springProcessEngineConfiguration(DataSource dataSource, PlatformTransactionManager platformTransactionManager,
        @Process ObjectProvider<AsyncExecutor> asyncExecutorProvider) throws IOException {

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

        configureSpringEngine(conf, platformTransactionManager);
        configureEngine(conf, dataSource);

        conf.setDeploymentName(defaultText(flowableProperties.getDeploymentName(), conf.getDeploymentName()));

        conf.setDisableIdmEngine(!(flowableProperties.isDbIdentityUsed() && idmProperties.isEnabled()));

        conf.setAsyncExecutorActivate(flowableProperties.isAsyncExecutorActivate());

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

        processEngineConfigurationConfigurers.forEach(configurator -> configurator.configure(conf));

        return conf;
    }

    @Bean
    public ProcessEngineFactoryBean processEngine(SpringProcessEngineConfiguration configuration) throws Exception {
        ProcessEngineFactoryBean processEngineFactoryBean = new ProcessEngineFactoryBean();
        processEngineFactoryBean.setProcessEngineConfiguration(configuration);
        return processEngineFactoryBean;
    }

    @Bean
    @ConditionalOnMissingBean
    public RuntimeService runtimeServiceBean(ProcessEngine processEngine) {
        return processEngine.getRuntimeService();
    }

    @Bean
    @ConditionalOnMissingBean
    public RepositoryService repositoryServiceBean(ProcessEngine processEngine) {
        return processEngine.getRepositoryService();
    }

    @Bean
    @ConditionalOnMissingBean
    public TaskService taskServiceBean(ProcessEngine processEngine) {
        return processEngine.getTaskService();
    }

    @Bean
    @ConditionalOnMissingBean
    public HistoryService historyServiceBean(ProcessEngine processEngine) {
        return processEngine.getHistoryService();
    }

    @Bean
    @ConditionalOnMissingBean
    public ManagementService managementServiceBean(ProcessEngine processEngine) {
        return processEngine.getManagementService();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public DynamicBpmnService dynamicBpmnServiceBean(ProcessEngine processEngine) {
        return processEngine.getDynamicBpmnService();
    }

    @Bean
    @ConditionalOnMissingBean
    public FormService formServiceBean(ProcessEngine processEngine) {
        return processEngine.getFormService();
    }

    @Bean
    @ConditionalOnMissingBean
    public IdentityService identityServiceBean(ProcessEngine processEngine) {
        return processEngine.getIdentityService();
    }
}
