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
package org.flowable.cmmn.rest.conf.engine;

import java.sql.Driver;

import javax.sql.DataSource;

import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.CmmnManagementService;
import org.flowable.cmmn.api.CmmnMigrationService;
import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.CmmnTaskService;
import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.rest.conf.MockFormHandlerRestApiInterceptor;
import org.flowable.cmmn.spring.CmmnEngineFactoryBean;
import org.flowable.cmmn.spring.SpringCmmnEngineConfiguration;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.idm.api.IdmIdentityService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.transaction.PlatformTransactionManager;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
public class EngineConfiguration {

    @Value("${jdbc.url:jdbc:h2:mem:flowable;DB_CLOSE_DELAY=1000}")
    protected String jdbcUrl;

    @Value("${jdbc.driver:org.h2.Driver}")
    protected Class<? extends Driver> jdbcDriver;

    @Value("${jdbc.username:sa}")
    protected String jdbcUsername;

    @Value("${jdbc.password:}")
    protected String jdbcPassword;

    @Bean
    public DataSource dataSource() {
        SimpleDriverDataSource ds = new SimpleDriverDataSource();
        ds.setDriverClass(jdbcDriver);

        // Connection settings
        ds.setUrl("jdbc:h2:mem:flowable;DB_CLOSE_DELAY=1000");
        ds.setUsername(jdbcUsername);
        ds.setPassword(jdbcPassword);

        return ds;
    }

    @Bean(name = "transactionManager")
    public PlatformTransactionManager annotationDrivenTransactionManager(DataSource dataSource) {
        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager();
        transactionManager.setDataSource(dataSource);
        return transactionManager;
    }

    @Bean(name = "cmmnEngine")
    public CmmnEngineFactoryBean cmmnEngineFactoryBean(CmmnEngineConfiguration cmmnEngineConfiguration) {
        CmmnEngineFactoryBean factoryBean = new CmmnEngineFactoryBean();
        factoryBean.setCmmnEngineConfiguration(cmmnEngineConfiguration);
        return factoryBean;
    }

    @Bean(name = "cmmnEngineConfiguration")
    public CmmnEngineConfiguration cmmnEngineConfiguration(DataSource dataSource, PlatformTransactionManager transactionManager) {
        SpringCmmnEngineConfiguration cmmnEngineConfiguration = new SpringCmmnEngineConfiguration();
        cmmnEngineConfiguration.setDataSource(dataSource);
        cmmnEngineConfiguration.setDatabaseSchemaUpdate(CmmnEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        cmmnEngineConfiguration.setTransactionManager(transactionManager);
        cmmnEngineConfiguration.setAsyncExecutorActivate(false);
        cmmnEngineConfiguration.setHistoryLevel(HistoryLevel.FULL);
        cmmnEngineConfiguration.setEnableEntityLinks(true);
        return cmmnEngineConfiguration;
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
    public CmmnTaskService taskService(CmmnEngine cmmnEngine) {
        return cmmnEngine.getCmmnTaskService();
    }

    @Bean
    public CmmnHistoryService historyService(CmmnEngine cmmnEngine) {
        return cmmnEngine.getCmmnHistoryService();
    }

    @Bean
    public CmmnManagementService managementService(CmmnEngine cmmnEngine) {
        return cmmnEngine.getCmmnManagementService();
    }

    @Bean
    public CmmnMigrationService cmmnMigrationService(CmmnEngine cmmnEngine) {
        return cmmnEngine.getCmmnMigrationService();
    }
    
    @Bean
    public IdmIdentityService idmIdentityService(CmmnEngine cmmnEngine) {
        return cmmnEngine.getCmmnEngineConfiguration().getIdmIdentityService();
    }

    @Bean
    public MockFormHandlerRestApiInterceptor formHandlerRestApiInterceptor(ObjectMapper objectMapper) {
        return new MockFormHandlerRestApiInterceptor(objectMapper);
    }
}
