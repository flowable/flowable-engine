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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import org.flowable.engine.FormService;
import org.flowable.engine.HistoryService;
import org.flowable.engine.IdentityService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.idm.api.IdmIdentityService;
import org.flowable.spring.ProcessEngineFactoryBean;
import org.flowable.spring.SpringAsyncExecutor;
import org.flowable.spring.SpringCallerRunsRejectedJobsHandler;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.SpringRejectedJobsHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.StringUtils;

/**
 * Provides sane definitions for the various beans required to be productive with Flowable in Spring.
 *
 * @author Josh Long
 */
public abstract class AbstractProcessEngineAutoConfiguration
        extends AbstractProcessEngineConfiguration {

    protected FlowableProperties flowableProperties;

    @Autowired
    private ResourcePatternResolver resourceLoader;

    @Autowired(required = false)
    private ProcessEngineConfigurationConfigurer processEngineConfigurationConfigurer;

    @Bean
    public SpringAsyncExecutor springAsyncExecutor(TaskExecutor taskExecutor) {
        return new SpringAsyncExecutor(taskExecutor, springRejectedJobsHandler());
    }

    @Bean
    public SpringRejectedJobsHandler springRejectedJobsHandler() {
        return new SpringCallerRunsRejectedJobsHandler();
    }

    protected SpringProcessEngineConfiguration baseSpringProcessEngineConfiguration(DataSource dataSource, PlatformTransactionManager platformTransactionManager,
            SpringAsyncExecutor springAsyncExecutor) throws IOException {

        List<Resource> procDefResources = this.discoverProcessDefinitionResources(
                this.resourceLoader, this.flowableProperties.getProcessDefinitionLocationPrefix(),
                this.flowableProperties.getProcessDefinitionLocationSuffixes(),
                this.flowableProperties.isCheckProcessDefinitions());

        SpringProcessEngineConfiguration conf = super.processEngineConfigurationBean(
                procDefResources.toArray(new Resource[procDefResources.size()]), dataSource,
                platformTransactionManager, springAsyncExecutor);

        conf.setDeploymentName(defaultText(flowableProperties.getDeploymentName(), conf.getDeploymentName()));
        conf.setDatabaseSchema(defaultText(flowableProperties.getDatabaseSchema(), conf.getDatabaseSchema()));
        conf.setDatabaseSchemaUpdate(defaultText(flowableProperties.getDatabaseSchemaUpdate(), conf.getDatabaseSchemaUpdate()));

        conf.setDbHistoryUsed(flowableProperties.isDbHistoryUsed());
        conf.setDisableIdmEngine(!flowableProperties.isDbIdentityUsed());

        conf.setAsyncExecutorActivate(flowableProperties.isAsyncExecutorActivate());

        conf.setMailServerHost(flowableProperties.getMailServerHost());
        conf.setMailServerPort(flowableProperties.getMailServerPort());
        conf.setMailServerUsername(flowableProperties.getMailServerUserName());
        conf.setMailServerPassword(flowableProperties.getMailServerPassword());
        conf.setMailServerDefaultFrom(flowableProperties.getMailServerDefaultFrom());
        conf.setMailServerUseSSL(flowableProperties.isMailServerUseSsl());
        conf.setMailServerUseTLS(flowableProperties.isMailServerUseTls());

        conf.setHistoryLevel(flowableProperties.getHistoryLevel());

        if (flowableProperties.getCustomMybatisMappers() != null) {
            conf.setCustomMybatisMappers(getCustomMybatisMapperClasses(flowableProperties.getCustomMybatisMappers()));
        }

        if (flowableProperties.getCustomMybatisXMLMappers() != null) {
            conf.setCustomMybatisXMLMappers(new HashSet<>(flowableProperties.getCustomMybatisXMLMappers()));
        }

        if (flowableProperties.getCustomMybatisMappers() != null) {
            conf.setCustomMybatisMappers(getCustomMybatisMapperClasses(flowableProperties.getCustomMybatisMappers()));
        }

        if (flowableProperties.getCustomMybatisXMLMappers() != null) {
            conf.setCustomMybatisXMLMappers(new HashSet<>(flowableProperties.getCustomMybatisXMLMappers()));
        }

        if (processEngineConfigurationConfigurer != null) {
            processEngineConfigurationConfigurer.configure(conf);
        }

        return conf;
    }

    protected Set<Class<?>> getCustomMybatisMapperClasses(List<String> customMyBatisMappers) {
        Set<Class<?>> mybatisMappers = new HashSet<>();
        for (String customMybatisMapperClassName : customMyBatisMappers) {
            try {
                Class customMybatisClass = Class.forName(customMybatisMapperClassName);
                mybatisMappers.add(customMybatisClass);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("Class " + customMybatisMapperClassName + " has not been found.", e);
            }
        }
        return mybatisMappers;
    }

    protected String defaultText(String deploymentName, String deploymentName1) {
        if (StringUtils.hasText(deploymentName))
            return deploymentName;
        return deploymentName1;
    }

    @Autowired
    protected void setFlowableProperties(FlowableProperties flowableProperties) {
        this.flowableProperties = flowableProperties;
    }

    protected FlowableProperties getFlowableProperties() {
        return this.flowableProperties;
    }

    @Bean
    public ProcessEngineFactoryBean processEngine(SpringProcessEngineConfiguration configuration) throws Exception {
        return super.springProcessEngineBean(configuration);
    }

    @Bean
    @ConditionalOnMissingBean
    @Override
    public RuntimeService runtimeServiceBean(ProcessEngine processEngine) {
        return super.runtimeServiceBean(processEngine);
    }

    @Bean
    @ConditionalOnMissingBean
    @Override
    public RepositoryService repositoryServiceBean(ProcessEngine processEngine) {
        return super.repositoryServiceBean(processEngine);
    }

    @Bean
    @ConditionalOnMissingBean
    @Override
    public TaskService taskServiceBean(ProcessEngine processEngine) {
        return super.taskServiceBean(processEngine);
    }

    @Bean
    @ConditionalOnMissingBean
    @Override
    public HistoryService historyServiceBean(ProcessEngine processEngine) {
        return super.historyServiceBean(processEngine);
    }

    @Bean
    @ConditionalOnMissingBean
    @Override
    public ManagementService managementServiceBeanBean(ProcessEngine processEngine) {
        return super.managementServiceBeanBean(processEngine);
    }

    @Bean
    @ConditionalOnMissingBean
    @Override
    public FormService formServiceBean(ProcessEngine processEngine) {
        return super.formServiceBean(processEngine);
    }

    @Bean
    @ConditionalOnMissingBean
    @Override
    public IdentityService identityServiceBean(ProcessEngine processEngine) {
        return super.identityServiceBean(processEngine);
    }
    
    @Bean
    @ConditionalOnMissingBean
    @Override
    public IdmIdentityService idmIdentityServiceBean(ProcessEngine processEngine) {
        return super.idmIdentityServiceBean(processEngine);
    }

    @Bean
    @ConditionalOnMissingBean
    public TaskExecutor taskExecutor() {
        return new SimpleAsyncTaskExecutor();
    }
}
