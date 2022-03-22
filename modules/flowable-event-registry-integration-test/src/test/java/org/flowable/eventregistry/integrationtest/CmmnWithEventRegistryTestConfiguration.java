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

import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.CmmnTaskService;
import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.cmmn.spring.CmmnEngineFactoryBean;
import org.flowable.cmmn.spring.SpringCmmnEngineConfiguration;
import org.flowable.eventregistry.api.ChannelModelProcessor;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.flowable.eventregistry.spring.SpringEventRegistryEngineConfiguration;
import org.flowable.eventregistry.spring.configurator.SpringEventRegistryConfigurator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.transaction.PlatformTransactionManager;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Filip Hrisafov
 */
@Configuration(proxyBeanMethods = false)
public class CmmnWithEventRegistryTestConfiguration {

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
    public SpringCmmnEngineConfiguration cmmnEngineConfiguration(DataSource dataSource, PlatformTransactionManager transactionManager,
            SpringEventRegistryConfigurator eventRegistryConfigurator) {
        
        SpringCmmnEngineConfiguration engineConfiguration = new SpringCmmnEngineConfiguration();
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

    @Bean("cmmnEngine")
    public CmmnEngineFactoryBean cmmnEngineFactoryBean(SpringCmmnEngineConfiguration cmmnEngineConfiguration) {
        CmmnEngineFactoryBean factoryBean = new CmmnEngineFactoryBean();
        factoryBean.setCmmnEngineConfiguration(cmmnEngineConfiguration);
        return factoryBean;
    }

    @Bean
    public CmmnRepositoryService cmmnRepositoryService(CmmnEngine cmmnEngine) {
        return cmmnEngine.getCmmnRepositoryService();
    }

    @Bean
    public CmmnRuntimeService cmmnRuntimeService(CmmnEngine cmmnEngine) {
        return cmmnEngine.getCmmnRuntimeService();
    }
    
    @Bean
    public CmmnTaskService cmmnTaskService(CmmnEngine cmmnEngine) {
        return cmmnEngine.getCmmnTaskService();
    }
}
