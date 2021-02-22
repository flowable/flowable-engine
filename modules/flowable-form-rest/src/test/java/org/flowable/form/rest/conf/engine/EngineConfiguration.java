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
package org.flowable.form.rest.conf.engine;

import java.sql.Driver;

import javax.sql.DataSource;

import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.form.api.FormManagementService;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.api.FormService;
import org.flowable.form.engine.FormEngine;
import org.flowable.form.engine.FormEngineConfiguration;
import org.flowable.form.spring.FormEngineFactoryBean;
import org.flowable.idm.api.IdmEngineConfigurationApi;
import org.flowable.idm.api.IdmIdentityService;
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

    @Bean(name = "formEngine")
    public FormEngineFactoryBean formEngineFactoryBean(FormEngineConfiguration formEngineConfiguration) {
        FormEngineFactoryBean factoryBean = new FormEngineFactoryBean();
        factoryBean.setFormEngineConfiguration(formEngineConfiguration);
        return factoryBean;
    }

    @Bean(name = "formEngineConfiguration")
    public FormEngineConfiguration formEngineConfiguration(DataSource dataSource, PlatformTransactionManager transactionManager) {

        TestSpringFormEngineConfiguration formEngineConfiguration = new TestSpringFormEngineConfiguration();
        formEngineConfiguration.setDataSource(dataSource);
        formEngineConfiguration.setDatabaseSchemaUpdate(FormEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        formEngineConfiguration.setTransactionManager(transactionManager);
        return formEngineConfiguration;
    }

    @Bean
    public FormRepositoryService formRepositoryService(FormEngine formEngine) {
        return formEngine.getFormRepositoryService();
    }

    @Bean
    public FormManagementService managementService(FormEngine formEngine) {
        return formEngine.getFormManagementService();
    }

    @Bean
    public FormService formService(FormEngine formEngine) {
        return formEngine.getFormService();
    }

    @Bean
    public IdmIdentityService idmIdentityService(FormEngine formEngine) {
        return ((IdmEngineConfigurationApi) formEngine.getFormEngineConfiguration().getEngineConfigurations()
                .get(EngineConfigurationConstants.KEY_IDM_ENGINE_CONFIG)).getIdmIdentityService();
    }

}
