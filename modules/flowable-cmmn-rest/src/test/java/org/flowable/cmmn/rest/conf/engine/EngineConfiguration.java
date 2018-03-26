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

import javax.sql.DataSource;

import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.CmmnManagementService;
import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.CmmnTaskService;
import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.spring.CmmnEngineFactoryBean;
import org.flowable.cmmn.spring.SpringCmmnEngineConfiguration;
import org.flowable.engine.common.impl.history.HistoryLevel;
import org.flowable.idm.api.IdmIdentityService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class EngineConfiguration {

    @Bean
    public DataSource dataSource() {
        SimpleDriverDataSource ds = new SimpleDriverDataSource();
        ds.setDriverClass(org.h2.Driver.class);

        // Connection settings
        ds.setUrl("jdbc:h2:mem:flowable;DB_CLOSE_DELAY=1000");
        ds.setUsername("sa");

        return ds;
    }

    @Bean(name = "transactionManager")
    public PlatformTransactionManager annotationDrivenTransactionManager() {
        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager();
        transactionManager.setDataSource(dataSource());
        return transactionManager;
    }

    @Bean(name = "cmmnEngineFactoryBean")
    public CmmnEngineFactoryBean cmmnEngineFactoryBean() {
        CmmnEngineFactoryBean factoryBean = new CmmnEngineFactoryBean();
        factoryBean.setCmmnEngineConfiguration(cmmnEngineConfiguration());
        return factoryBean;
    }

    @Bean(name = "cmmnEngine")
    public CmmnEngine cmmnEngine() {
        // Safe to call the getObject() on the @Bean annotated cmmnEngineFactoryBean(), will be
        // the fully initialized object instanced from the factory and will NOT be created more than once
        try {
            return cmmnEngineFactoryBean().getObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Bean(name = "cmmnEngineConfiguration")
    public CmmnEngineConfiguration cmmnEngineConfiguration() {
        SpringCmmnEngineConfiguration cmmnEngineConfiguration = new SpringCmmnEngineConfiguration();
        cmmnEngineConfiguration.setDataSource(dataSource());
        cmmnEngineConfiguration.setDatabaseSchemaUpdate(CmmnEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        cmmnEngineConfiguration.setTransactionManager(annotationDrivenTransactionManager());
        cmmnEngineConfiguration.setHistoryLevel(HistoryLevel.FULL);
        return cmmnEngineConfiguration;
    }

    @Bean
    public CmmnRepositoryService cmmnRepositoryService() {
        return cmmnEngine().getCmmnRepositoryService();
    }

    @Bean
    public CmmnRuntimeService cmmnRuntimeService() {
        return cmmnEngine().getCmmnRuntimeService();
    }

    @Bean
    public CmmnTaskService taskService() {
        return cmmnEngine().getCmmnTaskService();
    }

    @Bean
    public CmmnHistoryService historyService() {
        return cmmnEngine().getCmmnHistoryService();
    }

    @Bean
    public CmmnManagementService managementService() {
        return cmmnEngine().getCmmnManagementService();
    }
    
    @Bean
    public IdmIdentityService idmIdentityService() {
        return cmmnEngine().getCmmnEngineConfiguration().getIdmIdentityService();
    }
}
