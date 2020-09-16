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
package org.flowable.eventregistry.rest.conf.engine;

import javax.sql.DataSource;

import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.eventregistry.api.EventManagementService;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.flowable.eventregistry.spring.SpringEventRegistryEngineConfiguration;
import org.flowable.eventregistry.spring.configurator.SpringEventRegistryConfigurator;
import org.flowable.idm.api.IdmIdentityService;
import org.flowable.idm.engine.IdmEngineConfiguration;
import org.flowable.idm.spring.SpringIdmEngineConfiguration;
import org.flowable.idm.spring.configurator.SpringIdmEngineConfigurator;
import org.flowable.spring.ProcessEngineFactoryBean;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import com.zaxxer.hikari.HikariDataSource;

@Configuration(proxyBeanMethods = false)
public class EngineConfiguration {

    @Value("${jdbc.url:jdbc:h2:mem:flowable;DB_CLOSE_DELAY=1000}")
    protected String jdbcUrl;

    @Value("${jdbc.driver:org.h2.Driver}")
    protected String jdbcDriver;

    @Value("${jdbc.username:sa}")
    protected String jdbcUsername;

    @Value("${jdbc.password:}")
    protected String jdbcPassword;

    @Bean
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:h2:mem:flowable;DB_CLOSE_DELAY=1000");
        dataSource.setDriverClassName(jdbcDriver);
        dataSource.setUsername(jdbcUsername);
        dataSource.setPassword(jdbcPassword);

        return dataSource;
    }

    @Bean(name = "transactionManager")
    public PlatformTransactionManager annotationDrivenTransactionManager(DataSource dataSource) {
        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager();
        transactionManager.setDataSource(dataSource);
        return transactionManager;
    }
    
    @Bean(name = "processEngine")
    public ProcessEngineFactoryBean processEngineFactoryBean(ProcessEngineConfigurationImpl processEngineConfiguration) {
        ProcessEngineFactoryBean factoryBean = new ProcessEngineFactoryBean();
        factoryBean.setProcessEngineConfiguration(processEngineConfiguration);
        return factoryBean;
    }

    @Bean(name = "processEngineConfiguration")
    public ProcessEngineConfigurationImpl processEngineConfiguration(DataSource dataSource, PlatformTransactionManager transactionManager,
                    SpringEventRegistryConfigurator eventRegistryConfigurator, SpringIdmEngineConfigurator idmEngineConfigurator) {
        
        SpringProcessEngineConfiguration processEngineConfiguration = new SpringProcessEngineConfiguration();
        processEngineConfiguration.setDataSource(dataSource);
        processEngineConfiguration.setDatabaseSchemaUpdate(EventRegistryEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        processEngineConfiguration.setTransactionManager(transactionManager);
        processEngineConfiguration.setDisableIdmEngine(true);
        processEngineConfiguration.setDisableEventRegistry(true);
        processEngineConfiguration.addConfigurator(eventRegistryConfigurator);
        processEngineConfiguration.addConfigurator(idmEngineConfigurator);
        return processEngineConfiguration;
    }
    
    @Bean(name = "eventRegistryConfigurator")
    public SpringEventRegistryConfigurator eventRegistryConfigurator(EventRegistryEngineConfiguration configuration) {
        SpringEventRegistryConfigurator configurator = new SpringEventRegistryConfigurator();
        configurator.setEventEngineConfiguration(configuration);
        return configurator;
    }

    @Bean(name = "eventRegistryEngineConfiguration")
    public EventRegistryEngineConfiguration eventRegistryEngineConfiguration(DataSource dataSource, PlatformTransactionManager transactionManager) {
        SpringEventRegistryEngineConfiguration configuration = new SpringEventRegistryEngineConfiguration();
        configuration.setDataSource(dataSource);
        configuration.setDatabaseSchemaUpdate(EventRegistryEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        configuration.setTransactionManager(transactionManager);
        return configuration;
    }
    
    @Bean(name = "idmEngineConfigurator")
    public SpringIdmEngineConfigurator idmEngineConfigurator(IdmEngineConfiguration configuration) {
        SpringIdmEngineConfigurator configurator = new SpringIdmEngineConfigurator();
        configurator.setIdmEngineConfiguration(configuration);
        return configurator;
    }

    @Bean(name = "idmEngineConfiguration")
    public IdmEngineConfiguration idmEngineConfiguration(DataSource dataSource, PlatformTransactionManager transactionManager) {
        SpringIdmEngineConfiguration configuration = new SpringIdmEngineConfiguration();
        configuration.setDataSource(dataSource);
        configuration.setDatabaseSchemaUpdate(EventRegistryEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        configuration.setTransactionManager(transactionManager);
        return configuration;
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
    public EventRepositoryService eventRepositoryService(ProcessEngine processEngine) {
        return getEventRegistryEngineConfiguration(processEngine).getEventRepositoryService();
    }
    
    @Bean
    public EventManagementService eventManagementService(ProcessEngine processEngine) {
        return getEventRegistryEngineConfiguration(processEngine).getEventManagementService();
    }
    
    @Bean
    public EventRegistry eventRegistry(ProcessEngine processEngine) {
        return getEventRegistryEngineConfiguration(processEngine).getEventRegistry();
    }
    
    @Bean
    public EventRegistryEngineConfiguration eventRegistryEngineConfiguration(ProcessEngine processEngine) {
        return getEventRegistryEngineConfiguration(processEngine);
    }
    
    @Bean
    public IdmIdentityService idmIdentityService(ProcessEngine processEngine) {
        return getIdmEngineConfiguration(processEngine).getIdmIdentityService();
    }
    
    protected EventRegistryEngineConfiguration getEventRegistryEngineConfiguration(ProcessEngine processEngine) {
        return (EventRegistryEngineConfiguration) processEngine.getProcessEngineConfiguration().getEngineConfigurations().get(EngineConfigurationConstants.KEY_EVENT_REGISTRY_CONFIG);
    }
    
    protected IdmEngineConfiguration getIdmEngineConfiguration(ProcessEngine processEngine) {
        return (IdmEngineConfiguration) processEngine.getProcessEngineConfiguration().getEngineConfigurations().get(EngineConfigurationConstants.KEY_IDM_ENGINE_CONFIG);
    }
}
