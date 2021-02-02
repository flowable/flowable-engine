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

import java.sql.Driver;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.repository.CaseDefinitionQuery;
import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.cmmn.api.repository.CmmnDeploymentQuery;
import org.flowable.cmmn.converter.CmmnXMLException;
import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.cmmn.spring.CmmnEngineFactoryBean;
import org.flowable.cmmn.spring.SpringCmmnEngineConfiguration;
import org.flowable.common.engine.impl.util.IoUtil;
import org.flowable.common.spring.CommonAutoDeploymentStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class SpringAutoDeployTest {

    protected static final String DEFAULT_VALID_DEPLOYMENT_RESOURCES = "classpath*:/org/flowable/spring/test/autodeployment/simple*.cmmn";

    protected static final String DEFAULT_INVALID_DEPLOYMENT_RESOURCES = "classpath*:/org/flowable/spring/test/autodeployment/*simple*.cmmn";

    protected static final String DEFAULT_VALID_DIRECTORY_DEPLOYMENT_RESOURCES = "classpath*:/org/flowable/spring/test/autodeployment/**/simple*.cmmn";

    protected static final String DEFAULT_INVALID_DIRECTORY_DEPLOYMENT_RESOURCES = "classpath*:/org/flowable/spring/test/autodeployment/**/*simple*.cmmn";

    protected ConfigurableApplicationContext applicationContext;
    protected CmmnRepositoryService repositoryService;

    @AfterEach
    public void tearDown() throws Exception {
        removeAllDeployments();
        if (this.applicationContext != null) {
            this.applicationContext.close();
            this.applicationContext = null;
        }
        this.repositoryService = null;
    }

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
        applicationContext.register(SpringCmmnAutoDeployTestConfiguration.class);
        applicationContext.getEnvironment().getPropertySources()
                .addLast(new MapPropertySource("springAutoDeploy", properties));
        applicationContext.refresh();
        this.applicationContext = applicationContext;
        this.repositoryService = applicationContext.getBean(CmmnRepositoryService.class);
    }

    @Test
    public void testBasicActivitiSpringIntegration() {
        createAppContextWithoutDeploymentMode();
        List<CaseDefinition> caseDefinitions = repositoryService.createCaseDefinitionQuery().orderByCaseDefinitionKey().asc().list();

        Set<String> caseDefinitionKeys = new HashSet<>();
        for (CaseDefinition caseDefinition : caseDefinitions) {
            caseDefinitionKeys.add(caseDefinition.getKey());
        }

        Set<String> expectedCaseDefinitionKeys = new HashSet<>();
        expectedCaseDefinitionKeys.add("myCase");

        assertThat(caseDefinitionKeys).isEqualTo(expectedCaseDefinitionKeys);
    }

    @Test
    public void testNoRedeploymentForSpringContainerRestart() throws Exception {
        createAppContextWithoutDeploymentMode();
        CmmnDeploymentQuery deploymentQuery = repositoryService.createDeploymentQuery();
        assertThat(deploymentQuery.count()).isEqualTo(1);
        CaseDefinitionQuery caseDefinitionQuery = repositoryService.createCaseDefinitionQuery();
        assertThat(caseDefinitionQuery.count()).isEqualTo(1);

        // Creating a new app context with same resources doesn't lead to more deployments
        createAppContextWithoutDeploymentMode();
        assertThat(deploymentQuery.count()).isEqualTo(1);
        assertThat(caseDefinitionQuery.count()).isEqualTo(1);
    }

    // Updating the CMMN file should lead to a new deployment when restarting the Spring container
    @Test
    public void testResourceRedeploymentAfterCaseDefinitionChange() throws Exception {
        createAppContextWithoutDeploymentMode();
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(1);
        applicationContext.close();

        String filePath = "org/flowable/spring/test/autodeployment/simple-case.cmmn";
        String originalCaseFileContent = IoUtil.readFileAsString(filePath);
        String updatedCaseFileContent = originalCaseFileContent.replace("Case 1", "My simple case");
        assertThat(updatedCaseFileContent).hasSizeGreaterThan(originalCaseFileContent.length());
        IoUtil.writeStringToFile(updatedCaseFileContent, filePath);

        // Classic produced/consumer problem here:
        // The file is already written in Java, but not yet completely persisted by the OS
        // Constructing the new app context reads the same file which is sometimes not yet fully written to disk
        Thread.sleep(2000);

        try {
            createAppContextWithoutDeploymentMode();
        } finally {
            // Reset file content such that future test are not seeing something funny
            IoUtil.writeStringToFile(originalCaseFileContent, filePath);
        }

        // Assertions come AFTER the file write! Otherwise the CMMN file is
        // messed up if the assertions fail.
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(2);
        assertThat(repositoryService.createCaseDefinitionQuery().count()).isEqualTo(2);
    }

    @Test
    public void testAutoDeployWithDeploymentModeDefault() {
        createAppContextWithDefaultDeploymentMode();
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(1);
        assertThat(repositoryService.createCaseDefinitionQuery().count()).isEqualTo(1);
    }

    @Test
    public void testAutoDeployWithInvalidResourcesWithDeploymentModeDefault() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("deploymentMode", "default");
        properties.put("deploymentResources", DEFAULT_INVALID_DEPLOYMENT_RESOURCES);
        assertThatThrownBy(() -> createAppContext(properties))
                .hasCauseInstanceOf(CmmnXMLException.class);
        assertThat(repositoryService).isNull();

        // Some of the resources should have been deployed
        properties.put("deploymentResources", "classpath*:/notExisting*.cmmn");
        createAppContext(properties);
        assertThat(repositoryService.createCaseDefinitionQuery().list())
                .extracting(CaseDefinition::getKey)
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

        assertThat(repositoryService.createCaseDefinitionQuery().list())
                .extracting(CaseDefinition::getKey)
                .isEmpty();
        assertThat(repositoryService.createDeploymentQuery().count()).isZero();
    }

    @Test
    public void testAutoDeployWithDeploymentModeSingleResource() {
        createAppContextWithSingleResourceDeploymentMode();
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(1);
        assertThat(repositoryService.createCaseDefinitionQuery().count()).isEqualTo(1);
    }

    @Test
    public void testAutoDeployWithInvalidResourcesWithDeploymentModeSingleResource() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("deploymentMode", "single-resource");
        properties.put("deploymentResources", DEFAULT_INVALID_DEPLOYMENT_RESOURCES);
        assertThatThrownBy(() -> createAppContext(properties))
                .hasCauseInstanceOf(CmmnXMLException.class);
        assertThat(repositoryService).isNull();

        // Some of the resources should have been deployed
        properties.put("deploymentResources", "classpath*:/notExisting*.bpmn20.xml");
        createAppContext(properties);
        assertThat(repositoryService.createCaseDefinitionQuery().list())
                .extracting(CaseDefinition::getKey)
                .containsExactly("myCase");
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(1);
    }

    @Test
    public void testAutoDeployWithInvalidResourcesAndIgnoreExceptionOnDeploymentWithDeploymentModeSingleResource() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("deploymentMode", "single-resource");
        properties.put("deploymentResources", DEFAULT_INVALID_DEPLOYMENT_RESOURCES);
        properties.put("throwExceptionOnDeploymentFailure", false);
        createAppContext(properties);

        assertThat(repositoryService.createCaseDefinitionQuery().list())
                .extracting(CaseDefinition::getKey)
                .containsExactly("myCase");
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(1);
    }

    @Test
    public void testAutoDeployWithDeploymentModeResourceParentFolder() {
        createAppContextWithResourceParenFolderDeploymentMode();
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(2);
        assertThat(repositoryService.createCaseDefinitionQuery().count()).isEqualTo(2);
    }

    @Test
    public void testAutoDeployWithInvalidResourcesWithDeploymentModeResourceParentFolder() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("deploymentMode", "resource-parent-folder");
        properties.put("deploymentResources", DEFAULT_INVALID_DIRECTORY_DEPLOYMENT_RESOURCES);
        assertThatThrownBy(() -> createAppContext(properties))
                .hasCauseInstanceOf(CmmnXMLException.class);
        assertThat(repositoryService).isNull();

        // Start a new application context to verify that there are no deployments
        properties.put("deploymentResources", "classpath*:/notExisting*.cmmn");
        createAppContext(properties);

        assertThat(repositoryService.createCaseDefinitionQuery().list())
                .extracting(CaseDefinition::getKey)
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
        assertThat(repositoryService.createCaseDefinitionQuery().list())
                .extracting(CaseDefinition::getKey)
                .containsExactly("myCase2");
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(1);
    }

    // --Helper methods
    // ----------------------------------------------------------

    protected void removeAllDeployments() {
        if (repositoryService != null) {
            for (CmmnDeployment deployment : repositoryService.createDeploymentQuery().list()) {
                repositoryService.deleteDeployment(deployment.getId(), true);
            }
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class SpringCmmnAutoDeployTestConfiguration {

        @Bean
        public SimpleDriverDataSource dataSource(
                @Value("${jdbc.driver:org.h2.Driver}") Class<? extends Driver> driverClass,
                @Value("${jdbc.url:jdbc:h2:mem:flowable;DB_CLOSE_DELAY=1000}") String url,
                @Value("${jdbc.username:sa}") String username,
                @Value("${jdbc.password:}") String password
        ) {
            SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
            dataSource.setDriverClass(driverClass);
            dataSource.setUrl(url);
            dataSource.setUsername(username);
            dataSource.setPassword(password);

            return dataSource;
        }

        @Bean
        public PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        public SpringCmmnEngineConfiguration cmmnEngineConfiguration(DataSource dataSource, PlatformTransactionManager transactionManager,
                @Value("${databaseSchemaUpdate:true}") String databaseSchemaUpdate,
                @Value("${deploymentMode:#{null}}") String deploymentMode,
                @Value("${deploymentResources}") Resource[] deploymentResources,
                @Value("${throwExceptionOnDeploymentFailure:#{null}}") Boolean throwExceptionOnDeploymentFailure
        ) {
            SpringCmmnEngineConfiguration cmmnEngineConfiguration = new SpringCmmnEngineConfiguration();
            cmmnEngineConfiguration.setDataSource(dataSource);
            cmmnEngineConfiguration.setTransactionManager(transactionManager);
            cmmnEngineConfiguration.setDatabaseSchemaUpdate(databaseSchemaUpdate);

            if (deploymentMode != null) {
                cmmnEngineConfiguration.setDeploymentMode(deploymentMode);
            }

            cmmnEngineConfiguration.setDeploymentResources(deploymentResources);

            if (throwExceptionOnDeploymentFailure != null) {
                cmmnEngineConfiguration.getDeploymentStrategies()
                        .forEach(strategy -> {
                            if (strategy instanceof CommonAutoDeploymentStrategy) {
                                ((CommonAutoDeploymentStrategy<CmmnEngine>) strategy).getDeploymentProperties()
                                        .setThrowExceptionOnDeploymentFailure(throwExceptionOnDeploymentFailure);
                            }
                        });
            }

            return cmmnEngineConfiguration;
        }

        @Bean
        public CmmnEngineFactoryBean cmmnEngine(SpringCmmnEngineConfiguration cmmnEngineConfiguration) {
            CmmnEngineFactoryBean cmmnEngineFactoryBean = new CmmnEngineFactoryBean();
            cmmnEngineFactoryBean.setCmmnEngineConfiguration(cmmnEngineConfiguration);
            return cmmnEngineFactoryBean;
        }

        @Bean
        public CmmnRepositoryService repositoryService(CmmnEngine cmmnEngine) {
            return cmmnEngine.getCmmnRepositoryService();
        }
    }

}
