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

package org.flowable.app.spring.test.autodeployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Driver;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.flowable.app.api.AppRepositoryService;
import org.flowable.app.api.repository.AppDefinition;
import org.flowable.app.api.repository.AppDefinitionQuery;
import org.flowable.app.api.repository.AppDeployment;
import org.flowable.app.api.repository.AppDeploymentQuery;
import org.flowable.app.engine.AppEngine;
import org.flowable.app.spring.AppEngineFactoryBean;
import org.flowable.app.spring.SpringAppEngineConfiguration;
import org.flowable.common.engine.api.FlowableException;
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

import com.fasterxml.jackson.core.JsonParseException;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class SpringAutoDeployTest {

    protected static final String DEFAULT_VALID_DEPLOYMENT_RESOURCES = "classpath*:/org/flowable/app/spring/test/autodeployment/simple*.app";

    protected static final String DEFAULT_INVALID_DEPLOYMENT_RESOURCES = "classpath*:/org/flowable/app/spring/test/autodeployment/*simple*.app";

    protected static final String DEFAULT_VALID_DIRECTORY_DEPLOYMENT_RESOURCES = "classpath*:/org/flowable/app/spring/test/autodeployment/**/simple*.app";

    protected static final String DEFAULT_INVALID_DIRECTORY_DEPLOYMENT_RESOURCES = "classpath*:/org/flowable/app/spring/test/autodeployment/**/*simple*.app";

    protected ConfigurableApplicationContext applicationContext;
    protected AppRepositoryService repositoryService;

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
        applicationContext.register(SpringAppAutoDeployTestConfiguration.class);
        applicationContext.getEnvironment().getPropertySources()
            .addLast(new MapPropertySource("springAutoDeploy", properties));
        applicationContext.refresh();
        this.applicationContext = applicationContext;
        this.repositoryService = applicationContext.getBean(AppRepositoryService.class);
    }

    @AfterEach
    public void tearDown() throws Exception {
        removeAllDeployments();
        if (this.applicationContext != null) {
            this.applicationContext.close();
            this.applicationContext = null;
        }
        this.repositoryService = null;
    }

    @Test
    public void testBasicSpringIntegration() {
        createAppContextWithoutDeploymentMode();
        List<AppDefinition> appDefinitions = repositoryService.createAppDefinitionQuery().orderByAppDefinitionKey().asc().list();

        Set<String> appDefinitionKeys = new HashSet<>();
        for (AppDefinition appDefinition : appDefinitions) {
            appDefinitionKeys.add(appDefinition.getKey());
        }

        Set<String> expectedAppDefinitionKeys = new HashSet<>();
        expectedAppDefinitionKeys.add("simpleApp");

        assertThat(appDefinitionKeys).isEqualTo(expectedAppDefinitionKeys);
    }

    @Test
    public void testNoRedeploymentForSpringContainerRestart() throws Exception {
        createAppContextWithoutDeploymentMode();
        AppDeploymentQuery deploymentQuery = repositoryService.createDeploymentQuery();
        assertThat(deploymentQuery.count()).isEqualTo(1);
        AppDefinitionQuery appDefinitionQuery = repositoryService.createAppDefinitionQuery();
        assertThat(appDefinitionQuery.count()).isEqualTo(1);

        // Creating a new app context with same resources doesn't lead to more deployments
        createAppContextWithoutDeploymentMode();
        assertThat(deploymentQuery.count()).isEqualTo(1);
        assertThat(appDefinitionQuery.count()).isEqualTo(1);
    }

    // Updating the app file should lead to a new deployment when restarting the Spring container
    @Test
    public void testResourceRedeploymentAfterAppDefinitionChange() throws Exception {
        createAppContextWithoutDeploymentMode();
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(1);
        applicationContext.close();

        String filePath = "org/flowable/app/spring/test/autodeployment/simple.app";
        String originalAppFileContent = IoUtil.readFileAsString(filePath);
        String updatedAppFileContent = originalAppFileContent.replace("Simple app", "My simple app");
        assertThat(updatedAppFileContent).hasSizeGreaterThan(originalAppFileContent.length());
        IoUtil.writeStringToFile(updatedAppFileContent, filePath);

        // Classic produced/consumer problem here:
        // The file is already written in Java, but not yet completely persisted by the OS
        // Constructing the new app context reads the same file which is sometimes not yet fully written to disk
        Thread.sleep(2000);

        try {
            createAppContextWithoutDeploymentMode();
        } finally {
            // Reset file content such that future test are not seeing something funny
            IoUtil.writeStringToFile(originalAppFileContent, filePath);
        }

        // Assertions come AFTER the file write! Otherwise the app file is
        // messed up if the assertions fail.
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(2);
        assertThat(repositoryService.createAppDefinitionQuery().count()).isEqualTo(2);
    }

    @Test
    public void testAutoDeployWithDeploymentModeDefault() {
        createAppContextWithDefaultDeploymentMode();
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(1);
        assertThat(repositoryService.createAppDefinitionQuery().count()).isEqualTo(1);
    }

    @Test
    public void testAutoDeployWithInvalidResourcesWithDeploymentModeDefault() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("deploymentMode", "default");
        properties.put("deploymentResources", DEFAULT_INVALID_DEPLOYMENT_RESOURCES);
        assertThatThrownBy(() -> createAppContext(properties))
            .hasMessageContaining("Error reading app resource")
            .hasCauseInstanceOf(FlowableException.class)
            .hasRootCauseInstanceOf(JsonParseException.class);
        assertThat(repositoryService).isNull();

        // Some of the resources should have been deployed
        properties.put("deploymentResources", "classpath*:/notExisting*.app");
        createAppContext(properties);
        assertThat(repositoryService.createAppDefinitionQuery().list())
            .extracting(AppDefinition::getKey)
            .containsExactlyInAnyOrder("simpleApp");
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(1);
    }

    @Test
    public void testAutoDeployWithInvalidResourcesAndIgnoreExceptionWithDeploymentModeDefault() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("deploymentMode", "default");
        properties.put("deploymentResources", DEFAULT_INVALID_DEPLOYMENT_RESOURCES);
        properties.put("throwExceptionOnDeploymentFailure", false);
        createAppContext(properties);

        assertThat(repositoryService.createAppDefinitionQuery().list())
            .extracting(AppDefinition::getKey)
            .containsExactlyInAnyOrder("simpleApp");
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(1);
    }

    @Test
    public void testAutoDeployWithDeploymentModeSingleResource() {
        createAppContextWithSingleResourceDeploymentMode();
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(1);
        assertThat(repositoryService.createAppDefinitionQuery().count()).isEqualTo(1);
    }

    @Test
    public void testAutoDeployWithDeploymentModeResourceParentFolder() {
        createAppContextWithResourceParenFolderDeploymentMode();
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(2);
        assertThat(repositoryService.createAppDefinitionQuery().count()).isEqualTo(2);
    }

    // --Helper methods
    // ----------------------------------------------------------

    protected void removeAllDeployments() {
        if (repositoryService != null) {
            for (AppDeployment deployment : repositoryService.createDeploymentQuery().list()) {
                repositoryService.deleteDeployment(deployment.getId(), true);
            }
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class SpringAppAutoDeployTestConfiguration {

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
        public SpringAppEngineConfiguration appEngineConfiguration(DataSource dataSource, PlatformTransactionManager transactionManager,
            @Value("${databaseSchemaUpdate:true}") String databaseSchemaUpdate,
            @Value("${deploymentMode:#{null}}") String deploymentMode,
            @Value("${deploymentResources}") Resource[] deploymentResources,
            @Value("${throwExceptionOnDeploymentFailure:#{null}}") Boolean throwExceptionOnDeploymentFailure
        ) {
            SpringAppEngineConfiguration appEngineConfiguration = new SpringAppEngineConfiguration();
            appEngineConfiguration.setDataSource(dataSource);
            appEngineConfiguration.setTransactionManager(transactionManager);
            appEngineConfiguration.setDatabaseSchemaUpdate(databaseSchemaUpdate);

            if (deploymentMode != null) {
                appEngineConfiguration.setDeploymentMode(deploymentMode);
            }

            appEngineConfiguration.setDeploymentResources(deploymentResources);

            if (throwExceptionOnDeploymentFailure != null) {
                appEngineConfiguration.getDeploymentStrategies()
                    .forEach(strategy -> {
                        if (strategy instanceof CommonAutoDeploymentStrategy) {
                            ((CommonAutoDeploymentStrategy<AppEngine>) strategy).getDeploymentProperties().setThrowExceptionOnDeploymentFailure(throwExceptionOnDeploymentFailure);
                        }
                    });
            }

            return appEngineConfiguration;
        }

        @Bean
        public AppEngineFactoryBean appEngine(SpringAppEngineConfiguration appEngineConfiguration) {
            AppEngineFactoryBean appEngineFactoryBean = new AppEngineFactoryBean();
            appEngineFactoryBean.setAppEngineConfiguration(appEngineConfiguration);
            return appEngineFactoryBean;
        }

        @Bean
        public AppRepositoryService repositoryService(AppEngine appEngine) {
            return appEngine.getAppRepositoryService();
        }
    }

}
