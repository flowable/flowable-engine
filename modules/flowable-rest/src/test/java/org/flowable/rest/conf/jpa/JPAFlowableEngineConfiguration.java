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
import org.flowable.form.engine.FormEngineConfiguration;
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

@Configuration
public class JPAFlowableEngineConfiguration {

    @Autowired
    protected DataSource dataSource;

    @Autowired
    protected PlatformTransactionManager transactionManager;

    @Autowired
    protected EntityManagerFactory entityManagerFactory;

    @Bean(name = "processEngineFactoryBean")
    public ProcessEngineFactoryBean processEngineFactoryBean() {
        ProcessEngineFactoryBean factoryBean = new ProcessEngineFactoryBean();
        factoryBean.setProcessEngineConfiguration(processEngineConfiguration());
        return factoryBean;
    }

    @Bean(name = "processEngine")
    public ProcessEngine processEngine() {
        // Safe to call the getObject() on the @Bean annotated
        // processEngineFactoryBean(), will be
        // the fully initialized object instanced from the factory and will NOT
        // be created more than once
        try {
            return processEngineFactoryBean().getObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Bean(name = "processEngineConfiguration")
    public ProcessEngineConfigurationImpl processEngineConfiguration() {
        SpringProcessEngineConfiguration processEngineConfiguration = new SpringProcessEngineConfiguration();
        processEngineConfiguration.setDataSource(dataSource);
        processEngineConfiguration.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        processEngineConfiguration.setTransactionManager(transactionManager);
        processEngineConfiguration.setAsyncExecutorActivate(false);
        processEngineConfiguration.setJpaEntityManagerFactory(entityManagerFactory);
        processEngineConfiguration.setJpaHandleTransaction(false);
        processEngineConfiguration.setJpaHandleTransaction(false);
        processEngineConfiguration.setHistoryLevel(HistoryLevel.FULL);
        return processEngineConfiguration;
    }
    
    @Bean
    public SpringFormEngineConfigurator formEngineConfigurator() {
        SpringFormEngineConfigurator formEngineConfigurator =  new SpringFormEngineConfigurator();
        formEngineConfigurator.setFormEngineConfiguration(formEngineConfiguration());
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
    public RepositoryService repositoryService() {
        return processEngine().getRepositoryService();
    }

    @Bean
    public RuntimeService runtimeService() {
        return processEngine().getRuntimeService();
    }

    @Bean
    public TaskService taskService() {
        return processEngine().getTaskService();
    }

    @Bean
    public HistoryService historyService() {
        return processEngine().getHistoryService();
    }

    @Bean
    public FormService formService() {
        return processEngine().getFormService();
    }

    @Bean
    public IdentityService identityService() {
        return processEngine().getIdentityService();
    }

    @Bean
    public IdmIdentityService idmIdentityService() {
        return ((IdmEngineConfigurationApi) processEngine().getProcessEngineConfiguration().getEngineConfigurations()
                .get(EngineConfigurationConstants.KEY_IDM_ENGINE_CONFIG)).getIdmIdentityService();
    }

    @Bean
    public ManagementService managementService() {
        return processEngine().getManagementService();
    }

    @Bean
    public DynamicBpmnService dynamicBpmnService() {
        return processEngine().getDynamicBpmnService();
    }
    
    @Bean
    public FormRepositoryService formRepositoryService(ProcessEngine processEngine) {
        return formEngineConfiguration().getFormRepositoryService();
    }
    
    @Bean
    public org.flowable.form.api.FormService formEngineFormService(ProcessEngine processEngine) {
        return formEngineConfiguration().getFormService();
    }

    @Bean
    public ProcessMigrationService processInstanceMigrationService() {
        return processEngine().getProcessMigrationService();
    }
}
