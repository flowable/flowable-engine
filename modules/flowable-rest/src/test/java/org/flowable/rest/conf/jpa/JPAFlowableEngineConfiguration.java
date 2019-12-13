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
package org.flowable.rest.conf.jpa;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.DynamicBpmnService;
import org.flowable.engine.FormService;
import org.flowable.engine.HistoryService;
import org.flowable.engine.IdentityService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.ProcessMigrationService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.engine.FormEngine;
import org.flowable.form.engine.FormEngineConfiguration;
import org.flowable.form.engine.FormEngines;
import org.flowable.form.engine.configurator.FormEngineConfigurator;
import org.flowable.form.spring.SpringFormEngineConfiguration;
import org.flowable.form.spring.configurator.SpringFormEngineConfigurator;
import org.flowable.idm.api.IdmEngineConfigurationApi;
import org.flowable.idm.api.IdmIdentityService;
import org.flowable.spring.ProcessEngineFactoryBean;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration(proxyBeanMethods = false)
public class JPAFlowableEngineConfiguration {

    @Autowired
    protected DataSource dataSource;

    @Autowired
    protected PlatformTransactionManager transactionManager;

    @Autowired
    protected EntityManagerFactory entityManagerFactory;

    @Bean(name = "processEngine")
    public ProcessEngineFactoryBean processEngineFactoryBean(ProcessEngineConfigurationImpl processEngineConfiguration) {
        ProcessEngineFactoryBean factoryBean = new ProcessEngineFactoryBean();
        factoryBean.setProcessEngineConfiguration(processEngineConfiguration);
        return factoryBean;
    }

    @Bean(name = "processEngineConfiguration")
    public ProcessEngineConfigurationImpl processEngineConfiguration(FormEngineConfigurator formEngineConfigurator) {
        SpringProcessEngineConfiguration processEngineConfiguration = new SpringProcessEngineConfiguration();
        processEngineConfiguration.setDataSource(dataSource);
        processEngineConfiguration.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        processEngineConfiguration.setTransactionManager(transactionManager);
        processEngineConfiguration.setAsyncExecutorActivate(false);
        processEngineConfiguration.setJpaEntityManagerFactory(entityManagerFactory);
        processEngineConfiguration.setJpaHandleTransaction(false);
        processEngineConfiguration.setJpaHandleTransaction(false);
        processEngineConfiguration.setHistoryLevel(HistoryLevel.FULL);
        processEngineConfiguration.addConfigurator(formEngineConfigurator);
        return processEngineConfiguration;
    }
    
    @Bean
    public SpringFormEngineConfigurator formEngineConfigurator(FormEngineConfiguration formEngineConfiguration) {
        SpringFormEngineConfigurator formEngineConfigurator =  new SpringFormEngineConfigurator();
        formEngineConfigurator.setFormEngineConfiguration(formEngineConfiguration);
        return formEngineConfigurator;
    }
    
    @Bean(name = "formEngineConfiguration")
    public FormEngineConfiguration formEngineConfiguration() {
        SpringFormEngineConfiguration formEngineConfiguration = new SpringFormEngineConfiguration();
        formEngineConfiguration.setDataSource(dataSource);
        formEngineConfiguration.setDatabaseSchemaUpdate(FormEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        formEngineConfiguration.setTransactionManager(transactionManager);
        return formEngineConfiguration;
    }

    @Bean
    public FormEngine formEngine(@SuppressWarnings("unused") ProcessEngine processEngine) {
        // The process engine needs to be injected, as otherwise it won't be initialized, which means that the FormEngine is not initialized yet
        if (!FormEngines.isInitialized()) {
            throw new IllegalStateException("form engine has not been initialized");
        }
        return FormEngines.getDefaultFormEngine();
    }


    @Bean
    public RepositoryService repositoryService(ProcessEngine processEngine) {
        return processEngine.getRepositoryService();
    }

    @Bean
    public RuntimeService runtimeService(ProcessEngine processEngine) {
        return processEngine.getRuntimeService();
    }

    @Bean
    public TaskService taskService(ProcessEngine processEngine) {
        return processEngine.getTaskService();
    }

    @Bean
    public HistoryService historyService(ProcessEngine processEngine) {
        return processEngine.getHistoryService();
    }

    @Bean
    public FormService formService(ProcessEngine processEngine) {
        return processEngine.getFormService();
    }

    @Bean
    public IdentityService identityService(ProcessEngine processEngine) {
        return processEngine.getIdentityService();
    }

    @Bean
    public IdmIdentityService idmIdentityService(ProcessEngine processEngine) {
        return ((IdmEngineConfigurationApi) processEngine.getProcessEngineConfiguration().getEngineConfigurations()
                .get(EngineConfigurationConstants.KEY_IDM_ENGINE_CONFIG)).getIdmIdentityService();
    }

    @Bean
    public ManagementService managementService(ProcessEngine processEngine) {
        return processEngine.getManagementService();
    }

    @Bean
    public DynamicBpmnService dynamicBpmnService(ProcessEngine processEngine) {
        return processEngine.getDynamicBpmnService();
    }
    
    @Bean
    public FormRepositoryService formRepositoryService(FormEngine formEngine) {
        return formEngine.getFormRepositoryService();
    }
    
    @Bean
    public org.flowable.form.api.FormService formEngineFormService(FormEngine formEngine) {
        return formEngine.getFormService();
    }

    @Bean
    public ProcessMigrationService processInstanceMigrationService(ProcessEngine processEngine) {
        return processEngine.getProcessMigrationService();
    }
}
