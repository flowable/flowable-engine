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
package org.flowable.spring.test.jupiter;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;

import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.CmmnManagementService;
import org.flowable.cmmn.api.CmmnMigrationService;
import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.CmmnTaskService;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.CmmnDeploymentId;
import org.flowable.cmmn.engine.test.FlowableCmmnTestHelper;
import org.flowable.cmmn.spring.CmmnEngineFactoryBean;
import org.flowable.cmmn.spring.SpringCmmnEngineConfiguration;
import org.flowable.cmmn.spring.impl.test.FlowableCmmnSpringExtension;
import org.flowable.idm.spring.configurator.SpringIdmEngineConfigurator;
import org.h2.Driver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.zaxxer.hikari.HikariDataSource;

/**
 * @author Filip Hrisafov
 */
@ExtendWith(FlowableCmmnSpringExtension.class)
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = CmmnSpringJunitJupiterTest.TestConfiguration.class)
public class CmmnSpringJunitJupiterTest {

    @Autowired
    private CmmnEngine cmmnEngine;

    @Autowired
    private CmmnRuntimeService cmmnRuntimeService;

    @Autowired
    private CmmnRepositoryService cmmnRepositoryService;

    @Test
    @CmmnDeployment
    public void simpleCaseTest(FlowableCmmnTestHelper flowableTestHelper, @CmmnDeploymentId String deploymentId, CmmnEngine extensionCmmnEngine) {
        cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("junitJupiterCase").start();

        assertThat(flowableTestHelper.getDeploymentIdFromDeploymentAnnotation())
                .isEqualTo(deploymentId)
                .isNotNull();
        assertThat(flowableTestHelper.getCmmnEngine())
                .as("Spring injected process engine")
                .isSameAs(cmmnEngine)
                .as("Extension injected process engine")
                .isSameAs(extensionCmmnEngine);

        CaseDefinition deployedCaseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().deploymentId(deploymentId).singleResult();
        assertThat(deployedCaseDefinition).isNotNull();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    static class TestConfiguration {

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
            dataSource.setMinimumIdle(0);
            dataSource.setJdbcUrl(jdbcUrl);
            dataSource.setDriverClassName(jdbcDriver);
            dataSource.setUsername(jdbcUsername);
            dataSource.setPassword(jdbcPassword);
            return dataSource;
        }

        @Bean
        public PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        public SpringCmmnEngineConfiguration cmmnEngineConfiguration(DataSource dataSource, PlatformTransactionManager transactionManager,
                SpringIdmEngineConfigurator idmEngineConfigurator) {
            SpringCmmnEngineConfiguration configuration = new SpringCmmnEngineConfiguration();
            configuration.setDataSource(dataSource);
            configuration.setTransactionManager(transactionManager);
            configuration.setDatabaseSchemaUpdate("true");
            configuration.setIdmEngineConfigurator(idmEngineConfigurator);
            return configuration;
        }

        @Bean
        public SpringIdmEngineConfigurator idmEngineConfigurator() {
            return new SpringIdmEngineConfigurator();
        }

        @Bean
        public CmmnEngineFactoryBean cmmnEngine(SpringCmmnEngineConfiguration cmmnEngineConfiguration) {
            CmmnEngineFactoryBean factoryBean = new CmmnEngineFactoryBean();
            factoryBean.setCmmnEngineConfiguration(cmmnEngineConfiguration);
            return factoryBean;
        }

        @Bean
        public CmmnRepositoryService cmmnRepositoryService(CmmnEngine cmmnEngine) {
            return cmmnEngine.getCmmnRepositoryService();
        }

        @Bean
        public CmmnMigrationService cmmnMigrationService(CmmnEngine cmmnEngine) {
            return cmmnEngine.getCmmnMigrationService();
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
        public CmmnHistoryService cmmnHistoryService(CmmnEngine cmmnEngine) {
            return cmmnEngine.getCmmnHistoryService();
        }

        @Bean
        public CmmnManagementService cmmnManagementService(CmmnEngine cmmnEngine) {
            return cmmnEngine.getCmmnManagementService();
        }
    }
}
