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
package org.flowable.eventregistry.integrationtest;

import java.sql.Driver;

import javax.sql.DataSource;

import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.eventregistry.api.ChannelModelProcessor;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.flowable.eventregistry.spring.SpringEventRegistryEngineConfiguration;
import org.flowable.eventregistry.spring.configurator.SpringEventRegistryConfigurator;
import org.flowable.spring.ProcessEngineFactoryBean;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.transaction.PlatformTransactionManager;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
public class BpmnWithEventRegistryTestConfiguration {

    @Bean
    public DataSource dataSource(
        @Value("${jdbc.url:jdbc:h2:mem:flowable-spring-jms-test;DB_CLOSE_DELAY=1000}") String jdbcUrl,
        @Value("${jdbc.driver:org.h2.Driver}") Class<? extends Driver> jdbcDriverClass,
        @Value("${jdbc.username:sa}") String jdbcUsername,
        @Value("${jdbc.password:}") String jdbcPassword
    ) {
        SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setUrl(jdbcUrl);
        dataSource.setDriverClass(jdbcDriverClass);
        dataSource.setUsername(jdbcUsername);
        dataSource.setPassword(jdbcPassword);
        return dataSource;
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
    
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
    
    @Bean
    public SpringProcessEngineConfiguration processEngineConfiguration(DataSource dataSource, PlatformTransactionManager transactionManager,
            SpringEventRegistryConfigurator eventRegistryConfigurator) {
        
        SpringProcessEngineConfiguration engineConfiguration = new SpringProcessEngineConfiguration();
        engineConfiguration.setDataSource(dataSource);
        engineConfiguration.setTransactionManager(transactionManager);
        engineConfiguration.setDatabaseSchemaUpdate(SpringEventRegistryEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        engineConfiguration.setEventRegistryConfigurator(eventRegistryConfigurator);

        return engineConfiguration;
    }
    
    @Bean
    public SpringEventRegistryConfigurator eventRegistryConfigurator(EventRegistryEngineConfiguration eventRegistryEngineConfiguration) {
        SpringEventRegistryConfigurator eventRegistryConfigurator = new SpringEventRegistryConfigurator();
        eventRegistryConfigurator.setEventEngineConfiguration(eventRegistryEngineConfiguration);
        return eventRegistryConfigurator;
    }
    
    @Bean
    public SpringEventRegistryEngineConfiguration eventRegistryEngineConfiguration(DataSource dataSource, PlatformTransactionManager transactionManager,
                    ObjectProvider<ChannelModelProcessor> channelDefinitionProcessors) {
        
        SpringEventRegistryEngineConfiguration engineConfiguration = new SpringEventRegistryEngineConfiguration();
        engineConfiguration.setDataSource(dataSource);
        engineConfiguration.setTransactionManager(transactionManager);
        engineConfiguration.setDatabaseSchemaUpdate(SpringEventRegistryEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);

        channelDefinitionProcessors.stream().forEach(engineConfiguration::addChannelModelProcessor);

        engineConfiguration.setEnableEventRegistryChangeDetection(true);

        return engineConfiguration;
    }

    @Bean("processEngine")
    public ProcessEngineFactoryBean processEngineFactoryBean(SpringProcessEngineConfiguration processEngineConfiguration) {
        ProcessEngineFactoryBean factoryBean = new ProcessEngineFactoryBean();
        factoryBean.setProcessEngineConfiguration(processEngineConfiguration);
        return factoryBean;
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
    public ManagementService managementService(ProcessEngine processEngine) {
        return processEngine.getManagementService();
    }
}
