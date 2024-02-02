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
package org.flowable.app.rest.conf.engine;

import java.sql.Driver;

import javax.sql.DataSource;

import org.flowable.app.api.AppManagementService;
import org.flowable.app.api.AppRepositoryService;
import org.flowable.app.engine.AppEngine;
import org.flowable.app.engine.AppEngineConfiguration;
import org.flowable.app.spring.AppEngineFactoryBean;
import org.flowable.app.spring.SpringAppEngineConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.transaction.PlatformTransactionManager;

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

    @Bean(name = "appEngine")
    public AppEngineFactoryBean appEngineFactoryBean(AppEngineConfiguration appEngineConfiguration) {
        AppEngineFactoryBean factoryBean = new AppEngineFactoryBean();
        factoryBean.setAppEngineConfiguration(appEngineConfiguration);
        return factoryBean;
    }

    @Bean(name = "appEngineConfiguration")
    public AppEngineConfiguration appEngineConfiguration(DataSource dataSource, PlatformTransactionManager transactionManager) {
        SpringAppEngineConfiguration appEngineConfiguration = new SpringAppEngineConfiguration();
        appEngineConfiguration.setDataSource(dataSource);
        appEngineConfiguration.setDatabaseSchemaUpdate(AppEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        appEngineConfiguration.setTransactionManager(transactionManager);
        return appEngineConfiguration;
    }

    @Bean
    public AppRepositoryService appRepositoryService(AppEngine appEngine) {
        return appEngine.getAppRepositoryService();
    }

    @Bean
    public AppManagementService managementService(AppEngine appEngine) {
        return appEngine.getAppManagementService();
    }
}
