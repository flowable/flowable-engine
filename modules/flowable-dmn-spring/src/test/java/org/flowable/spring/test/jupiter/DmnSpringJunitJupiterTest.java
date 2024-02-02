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

import java.util.Map;

import javax.sql.DataSource;

import org.flowable.dmn.api.DmnDecisionService;
import org.flowable.dmn.api.DmnHistoryService;
import org.flowable.dmn.api.DmnManagementService;
import org.flowable.dmn.api.DmnRepositoryService;
import org.flowable.dmn.engine.DmnEngine;
import org.flowable.dmn.engine.test.DmnDeployment;
import org.flowable.dmn.engine.test.DmnDeploymentId;
import org.flowable.dmn.engine.test.FlowableDmnTestHelper;
import org.flowable.dmn.spring.DmnEngineFactoryBean;
import org.flowable.dmn.spring.SpringDmnEngineConfiguration;
import org.flowable.dmn.spring.impl.test.FlowableDmnSpringExtension;
import org.h2.Driver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Filip Hrisafov
 */
@ExtendWith(FlowableDmnSpringExtension.class)
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = DmnSpringJunitJupiterTest.TestConfiguration.class)
class DmnSpringJunitJupiterTest {

    @Autowired
    private DmnEngine dmnEngine;

    @Autowired
    private DmnDecisionService ruleService;

    @Autowired
    private DmnRepositoryService dmnRepositoryService;

    @Test
    @DmnDeployment
    void simpleDmnTest(FlowableDmnTestHelper flowableTestHelper, @DmnDeploymentId String deploymentId, DmnEngine extensionDmnEngine) {
        Map<String, Object> executionResult = ruleService.createExecuteDecisionBuilder()
            .decisionKey("extensionUsage")
            .variable("input1", "testString")
            .executeWithSingleResult();

        assertThat(executionResult).containsEntry("output1", "test1");

        assertThat(flowableTestHelper.getDeploymentIdFromDeploymentAnnotation())
            .isEqualTo(deploymentId)
            .isNotNull();
        assertThat(flowableTestHelper.getDmnEngine())
            .as("Spring injected dmn engine")
            .isSameAs(dmnEngine)
            .as("Extension injected dmn engine")
            .isSameAs(extensionDmnEngine);

        org.flowable.dmn.api.DmnDeployment deployment = dmnRepositoryService.createDeploymentQuery().deploymentId(deploymentId).singleResult();
        assertThat(deployment).isNotNull();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    static class TestConfiguration {

        @Bean
        public DataSource dataSource() {
            SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
            dataSource.setDriverClass(Driver.class);
            dataSource.setUrl("jdbc:h2:mem:flowable-dmn-jupiter;DB_CLOSE_DELAY=1000");
            dataSource.setUsername("sa");
            dataSource.setPassword("");

            return dataSource;
        }

        @Bean
        public PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        public SpringDmnEngineConfiguration dmnEngineConfiguration(DataSource dataSource, PlatformTransactionManager transactionManager) {
            SpringDmnEngineConfiguration configuration = new SpringDmnEngineConfiguration();
            configuration.setDataSource(dataSource);
            configuration.setTransactionManager(transactionManager);
            configuration.setDatabaseSchemaUpdate("true");
            return configuration;
        }

        @Bean
        public DmnEngineFactoryBean dmnEngine(SpringDmnEngineConfiguration dmnEngineConfiguration) {
            DmnEngineFactoryBean factoryBean = new DmnEngineFactoryBean();
            factoryBean.setDmnEngineConfiguration(dmnEngineConfiguration);
            return factoryBean;
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

        @Bean
        public DmnManagementService dmnManagementService(DmnEngine dmnEngine) {
            return dmnEngine.getDmnManagementService();
        }
    }
}
