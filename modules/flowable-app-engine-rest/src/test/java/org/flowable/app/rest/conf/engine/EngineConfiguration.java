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

import javax.sql.DataSource;

import org.flowable.app.api.AppManagementService;
import org.flowable.app.api.AppRepositoryService;
import org.flowable.app.engine.AppEngine;
import org.flowable.app.engine.AppEngineConfiguration;
import org.flowable.app.spring.AppEngineFactoryBean;
import org.flowable.app.spring.SpringAppEngineConfiguration;
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

    @Bean(name = "appEngineFactoryBean")
    public AppEngineFactoryBean appEngineFactoryBean() {
        AppEngineFactoryBean factoryBean = new AppEngineFactoryBean();
        factoryBean.setAppEngineConfiguration(appEngineConfiguration());
        return factoryBean;
    }

    @Bean(name = "appEngine")
    public AppEngine appEngine() {
        // Safe to call the getObject() on the @Bean annotated appEngineFactoryBean(), will be
        // the fully initialized object instanced from the factory and will NOT be created more than once
        try {
            return appEngineFactoryBean().getObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Bean(name = "appEngineConfiguration")
    public AppEngineConfiguration appEngineConfiguration() {
        SpringAppEngineConfiguration appEngineConfiguration = new SpringAppEngineConfiguration();
        appEngineConfiguration.setDataSource(dataSource());
        appEngineConfiguration.setDatabaseSchemaUpdate(AppEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        appEngineConfiguration.setTransactionManager(annotationDrivenTransactionManager());
        return appEngineConfiguration;
    }

    @Bean
    public AppRepositoryService appRepositoryService() {
        return appEngine().getAppRepositoryService();
    }

    @Bean
    public AppManagementService managementService() {
        return appEngine().getAppManagementService();
    }
}
