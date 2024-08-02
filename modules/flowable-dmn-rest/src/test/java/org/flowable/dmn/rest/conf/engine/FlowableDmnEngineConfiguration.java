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
package org.flowable.dmn.rest.conf.engine;

import javax.sql.DataSource;

import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.dmn.api.DmnDecisionService;
import org.flowable.dmn.api.DmnHistoryService;
import org.flowable.dmn.api.DmnRepositoryService;
import org.flowable.dmn.engine.DmnEngine;
import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.dmn.engine.impl.cfg.StandaloneInMemDmnEngineConfiguration;
import org.flowable.dmn.spring.DmnEngineFactoryBean;
import org.flowable.dmn.spring.SpringDmnEngineConfiguration;
import org.flowable.idm.spring.configurator.SpringIdmEngineConfigurator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import com.zaxxer.hikari.HikariDataSource;

/**
 * @author Yvo Swillens
 */
@Configuration(proxyBeanMethods = false)
public class FlowableDmnEngineConfiguration {

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
        dataSource.setJdbcUrl(jdbcUrl);
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

    @Bean(name = "dmnEngine")
    public DmnEngineFactoryBean dmnEngineFactoryBean(DmnEngineConfiguration dmnEngineConfiguration) {
        DmnEngineFactoryBean factoryBean = new DmnEngineFactoryBean();
        factoryBean.setDmnEngineConfiguration(dmnEngineConfiguration);
        return factoryBean;
    }

    @Bean(name = "dmnEngineConfiguration")
    public DmnEngineConfiguration dmnEngineConfiguration(DataSource dataSource, PlatformTransactionManager transactionManager,
            SpringIdmEngineConfigurator springIdmEngineConfigurator) {
        SpringDmnEngineConfiguration dmnEngineConfiguration = new SpringDmnEngineConfiguration();
        dmnEngineConfiguration.setDataSource(dataSource);
        dmnEngineConfiguration.setDatabaseSchemaUpdate(DmnEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        dmnEngineConfiguration.setTransactionManager(transactionManager);
        dmnEngineConfiguration.setIdmEngineConfigurator(springIdmEngineConfigurator);
        dmnEngineConfiguration.setHistoryEnabled(true);
        return dmnEngineConfiguration;
    }

    @Bean(name = "springIdmEngineConfigurator")
    public SpringIdmEngineConfigurator springIdmEngineConfigurator() {
        return new SpringIdmEngineConfigurator();
    }

    @Bean
    public DmnRepositoryService dmnRepositoryService(DmnEngine dmnEngine) {
        return dmnEngine.getDmnRepositoryService();
    }

    @Bean
    public DmnDecisionService dmnRuleService(DmnEngine dmnEngine) {
        return dmnEngine.getDmnDecisionService();
    }
    
    @Bean
    public DmnHistoryService dmnHistoryService(DmnEngine dmnEngine) {
        return dmnEngine.getDmnHistoryService();
    }
}
