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

package org.flowable.spring.test.autodeployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.flowable.common.engine.impl.test.LoggingExtension;
import org.flowable.common.engine.impl.util.IoUtil;
import org.flowable.common.spring.CommonAutoDeploymentStrategy;
import org.flowable.dmn.api.DmnDecision;
import org.flowable.dmn.api.DmnDecisionQuery;
import org.flowable.dmn.api.DmnDeployment;
import org.flowable.dmn.api.DmnDeploymentQuery;
import org.flowable.dmn.api.DmnRepositoryService;
import org.flowable.dmn.engine.DmnEngine;
import org.flowable.dmn.spring.DmnEngineFactoryBean;
import org.flowable.dmn.spring.SpringDmnEngineConfiguration;
import org.flowable.dmn.xml.exception.DmnXMLException;
import org.flowable.idm.spring.configurator.SpringIdmEngineConfigurator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import com.zaxxer.hikari.HikariDataSource;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
@ExtendWith(LoggingExtension.class)
class SpringAutoDeployTest {

    protected static final String DEFAULT_VALID_DEPLOYMENT_RESOURCES = "classpath*:/org/flowable/spring/test/autodeployment/simple*.dmn";

    protected static final String DEFAULT_INVALID_DEPLOYMENT_RESOURCES = "classpath*:/org/flowable/spring/test/autodeployment/*simple*.dmn";

    protected static final String DEFAULT_VALID_DIRECTORY_DEPLOYMENT_RESOURCES = "classpath*:/org/flowable/spring/test/autodeployment/**/simple*.dmn";

    protected static final String DEFAULT_INVALID_DIRECTORY_DEPLOYMENT_RESOURCES = "classpath*:/org/flowable/spring/test/autodeployment/**/*simple*.dmn";

    protected ConfigurableApplicationContext applicationContext;
    protected DmnRepositoryService repositoryService;

    protected void createAppContextWithoutDeploymentMode() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("deploymentResources", DEFAULT_VALID_DEPLOYMENT_RESOURCES);
        createAppContext(properties);
    }

    protected void createAppContextWithDefaultDeploymentMode() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("deploymentMode", "default");
        properties.put("deploymentResources", DEFAULT_VALID_DEPLOYMENT_RESOURCES);
        createAppContext(properties);
    }
    protected void createAppContextWithSingleResourceDeploymentMode() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("deploymentMode", "single-resource");
        properties.put("deploymentResources", DEFAULT_VALID_DEPLOYMENT_RESOURCES);
        createAppContext(properties);
    }

    protected void createAppContextWithResourceParenFolderDeploymentMode() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("deploymentMode", "resource-parent-folder");
        properties.put("deploymentResources", DEFAULT_VALID_DIRECTORY_DEPLOYMENT_RESOURCES);
        createAppContext(properties);
    }

    protected void createAppContext(Map<String, Object> properties) {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.register(SpringDmnAutoDeployTestConfiguration.class);
        applicationContext.getEnvironment().getPropertySources()
            .addLast(new MapPropertySource("springAutoDeploy", properties));
        applicationContext.refresh();
        this.applicationContext = applicationContext;
        this.repositoryService = applicationContext.getBean(DmnRepositoryService.class);
    }

    @AfterEach
    void tearDown() {
        removeAllDeployments();
        if (this.applicationContext != null) {
            this.applicationContext.close();
            this.applicationContext = null;
        }
        this.repositoryService = null;
    }

    @Test
    public void testBasicActivitiSpringIntegration() {
        createAppContextWithoutDeploymentMode();
        List<DmnDecision> decisionTables = repositoryService.createDecisionQuery().orderByDecisionKey().asc().list();

        Set<String> decisionTableKeys = new HashSet<>();
        for (DmnDecision decisionTable : decisionTables) {
            decisionTableKeys.add(decisionTable.getKey());
        }

        Set<String> expectedDecisionTableKeys = new HashSet<>();
        expectedDecisionTableKeys.add("decision");
        expectedDecisionTableKeys.add("decision2");

        assertThat(decisionTableKeys).isEqualTo(expectedDecisionTableKeys);
    }

    @Test
    public void testNoRedeploymentForSpringContainerRestart() throws Exception {
        createAppContextWithoutDeploymentMode();
        DmnDeploymentQuery deploymentQuery = repositoryService.createDeploymentQuery();

        assertThat(deploymentQuery.count()).isEqualTo(1);
        DmnDecisionQuery decisionQuery = repositoryService.createDecisionQuery();
        assertThat(decisionQuery.count()).isEqualTo(2);

        // Creating a new app context with same resources doesn't lead to more deployments
        createAppContextWithoutDeploymentMode();
        assertThat(deploymentQuery.count()).isEqualTo(1);
        assertThat(decisionQuery.count()).isEqualTo(2);
    }

    // Updating the DMN file should lead to a new deployment when restarting the Spring container
    @Test
    public void testResourceRedeploymentAfterDecisionTableChange() throws Exception {
        createAppContextWithoutDeploymentMode();
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(1);
        applicationContext.close();

        String filePath = "org/flowable/spring/test/autodeployment/simple_1.dmn";
        String originalFormFileContent = IoUtil.readFileAsString(filePath);
        String updatedFormFileContent = originalFormFileContent.replace("Simple decision", "My simple decision");
        assertThat(updatedFormFileContent).hasSizeGreaterThan(originalFormFileContent.length());
        IoUtil.writeStringToFile(updatedFormFileContent, filePath);

        // Classic produced/consumer problem here:
        // The file is already written in Java, but not yet completely persisted by the OS
        // Constructing the new app context reads the same file which is sometimes not yet fully written to disk
        Thread.sleep(2000);

        try {
            createAppContextWithoutDeploymentMode();
        } finally {
            // Reset file content such that future test are not seeing something funny
            IoUtil.writeStringToFile(originalFormFileContent, filePath);
        }

        // Assertions come AFTER the file write! Otherwise the DMN file is
        // messed up if the assertions fail.
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(2);
        assertThat(repositoryService.createDecisionQuery().count()).isEqualTo(4);
    }

    @Test
    public void testAutoDeployWithDeploymentModeDefault() {
        createAppContextWithDefaultDeploymentMode();
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(1);
        assertThat(repositoryService.createDecisionQuery().count()).isEqualTo(2);
    }

    @Test
    public void testAutoDeployWithInvalidResourcesWithDeploymentModeDefault() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("deploymentMode", "default");
        properties.put("deploymentResources", DEFAULT_INVALID_DEPLOYMENT_RESOURCES);
        assertThatThrownBy(() -> createAppContext(properties))
                .hasCauseInstanceOf(DmnXMLException.class);
        assertThat(repositoryService).isNull();

        // Some of the resources should have been deployed
        properties.put("deploymentResources", "classpath*:/notExisting*.bpmn20.xml");
        createAppContext(properties);
        assertThat(repositoryService.createDecisionQuery().list())
                .extracting(DmnDecision::getKey)
                .isEmpty();
        assertThat(repositoryService.createDeploymentQuery().count()).isZero();
    }

    @Test
    public void testAutoDeployWithInvalidResourcesAndIgnoreExceptionWithDeploymentModeDefault() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("deploymentMode", "default");
        properties.put("deploymentResources", DEFAULT_INVALID_DEPLOYMENT_RESOURCES);
        properties.put("throwExceptionOnDeploymentFailure", false);
        createAppContext(properties);

        assertThat(repositoryService.createDecisionQuery().list())
                .extracting(DmnDecision::getKey)
                .isEmpty();
        assertThat(repositoryService.createDeploymentQuery().count()).isZero();
    }

    @Test
    public void testAutoDeployWithDeploymentModeSingleResource() {
        createAppContextWithSingleResourceDeploymentMode();
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(2);
        assertThat(repositoryService.createDecisionQuery().count()).isEqualTo(2);
    }

    @Test
    public void testAutoDeployWithInvalidResourcesWithDeploymentModeSingleResource() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("deploymentMode", "single-resource");
        properties.put("deploymentResources", DEFAULT_INVALID_DEPLOYMENT_RESOURCES);
        assertThatThrownBy(() -> createAppContext(properties))
            .hasCauseInstanceOf(DmnXMLException.class);
        assertThat(repositoryService).isNull();

        // Some of the resources should have been deployed
        properties.put("deploymentResources", "classpath*:/notExisting*.dmn");
        createAppContext(properties);
        assertThat(repositoryService.createDecisionQuery().list())
            .extracting(DmnDecision::getKey)
            .containsExactlyInAnyOrder("decision", "decision2");
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(2);
    }

    @Test
    public void testAutoDeployWithInvalidResourcesAndIgnoreExceptionOnDeploymentWithDeploymentModeSingleResource() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("deploymentMode", "single-resource");
        properties.put("deploymentResources", DEFAULT_INVALID_DEPLOYMENT_RESOURCES);
        properties.put("throwExceptionOnDeploymentFailure", false);
        createAppContext(properties);

        assertThat(repositoryService.createDecisionQuery().list())
            .extracting(DmnDecision::getKey)
            .containsExactlyInAnyOrder("decision", "decision2");
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(2);
    }

    @Test
    public void testAutoDeployWithDeploymentModeResourceParentFolder() {
        createAppContextWithResourceParenFolderDeploymentMode();
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(2);
        assertThat(repositoryService.createDecisionQuery().count()).isEqualTo(3);
    }

    @Test
    public void testAutoDeployWithInvalidResourcesWithDeploymentModeResourceParentFolder() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("deploymentMode", "resource-parent-folder");
        properties.put("deploymentResources", DEFAULT_INVALID_DIRECTORY_DEPLOYMENT_RESOURCES);
        assertThatThrownBy(() -> createAppContext(properties))
                .hasCauseInstanceOf(DmnXMLException.class);
        assertThat(repositoryService).isNull();

        // Start a new application context to verify that there are no deployments
        properties.put("deploymentResources", "classpath*:/notExisting*.dmn");
        createAppContext(properties);

        assertThat(repositoryService.createDecisionQuery().list())
                .extracting(DmnDecision::getKey)
                .isEmpty();
        assertThat(repositoryService.createDeploymentQuery().count()).isZero();
    }

    @Test
    public void testAutoDeployWithInvalidResourcesAndIgnoreExceptionOnDeploymentWithDeploymentModeResourceParentFolder() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("deploymentMode", "resource-parent-folder");
        properties.put("deploymentResources", DEFAULT_INVALID_DIRECTORY_DEPLOYMENT_RESOURCES);
        properties.put("throwExceptionOnDeploymentFailure", false);
        createAppContext(properties);
        assertThat(repositoryService.createDecisionQuery().list())
            .extracting(DmnDecision::getKey)
            .containsExactlyInAnyOrder("decision3");
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(1);
    }

    // --Helper methods
    // ----------------------------------------------------------

    private void removeAllDeployments() {
        if (repositoryService != null) {
            for (DmnDeployment deployment : repositoryService.createDeploymentQuery().list()) {
                repositoryService.deleteDeployment(deployment.getId());
            }
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class SpringDmnAutoDeployTestConfiguration {

        @Bean
        public DataSource dataSource(
            @Value("${jdbc.driver:org.h2.Driver}") String driver,
            @Value("${jdbc.url:jdbc:h2:mem:flowable;DB_CLOSE_DELAY=1000}") String url,
            @Value("${jdbc.username:sa}") String username,
            @Value("${jdbc.password:}") String password
        ) {
            HikariDataSource dataSource = new HikariDataSource();
            dataSource.setMinimumIdle(0);
            dataSource.setJdbcUrl(url);
            dataSource.setDriverClassName(driver);
            dataSource.setUsername(username);
            dataSource.setPassword(password);
            return dataSource;
        }

        @Bean
        public PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        public SpringDmnEngineConfiguration dmnEngineConfiguration(DataSource dataSource, PlatformTransactionManager transactionManager,
            SpringIdmEngineConfigurator springIdmEngineConfigurator,
            @Value("${databaseSchemaUpdate:true}") String databaseSchemaUpdate,
            @Value("${deploymentMode:#{null}}") String deploymentMode,
            @Value("${deploymentResources}") Resource[] deploymentResources,
            @Value("${throwExceptionOnDeploymentFailure:#{null}}") Boolean throwExceptionOnDeploymentFailure
        ) {
            SpringDmnEngineConfiguration dmnEngineConfiguration = new SpringDmnEngineConfiguration();
            dmnEngineConfiguration.setDataSource(dataSource);
            dmnEngineConfiguration.setTransactionManager(transactionManager);
            dmnEngineConfiguration.setDatabaseSchemaUpdate(databaseSchemaUpdate);

            if (deploymentMode != null) {
                dmnEngineConfiguration.setDeploymentMode(deploymentMode);
            }

            dmnEngineConfiguration.setDeploymentResources(deploymentResources);

            if (throwExceptionOnDeploymentFailure != null) {
                dmnEngineConfiguration.getDeploymentStrategies()
                    .forEach(strategy -> {
                        if (strategy instanceof CommonAutoDeploymentStrategy) {
                            ((CommonAutoDeploymentStrategy<DmnEngine>) strategy).getDeploymentProperties().setThrowExceptionOnDeploymentFailure(throwExceptionOnDeploymentFailure);
                        }
                    });
            }

            dmnEngineConfiguration.setIdmEngineConfigurator(springIdmEngineConfigurator);

            return dmnEngineConfiguration;
        }

        @Bean(name = "springIdmEngineConfigurator")
        public SpringIdmEngineConfigurator springIdmEngineConfigurator() {
            return new SpringIdmEngineConfigurator();
        }

        @Bean
        public DmnEngineFactoryBean dmnEngine(SpringDmnEngineConfiguration dmnEngineConfiguration) {
            DmnEngineFactoryBean dmnEngineFactoryBean = new DmnEngineFactoryBean();
            dmnEngineFactoryBean.setDmnEngineConfiguration(dmnEngineConfiguration);
            return dmnEngineFactoryBean;
        }

        @Bean
        public DmnRepositoryService repositoryService(DmnEngine dmnEngine) {
            return dmnEngine.getDmnRepositoryService();
        }
    }

}
