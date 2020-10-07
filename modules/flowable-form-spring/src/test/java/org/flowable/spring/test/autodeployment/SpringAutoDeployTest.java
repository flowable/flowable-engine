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

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.test.LoggingExtension;
import org.flowable.common.engine.impl.util.IoUtil;
import org.flowable.common.spring.CommonAutoDeploymentStrategy;
import org.flowable.form.api.FormDefinition;
import org.flowable.form.api.FormDefinitionQuery;
import org.flowable.form.api.FormDeployment;
import org.flowable.form.api.FormDeploymentQuery;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.engine.FormEngine;
import org.flowable.form.spring.FormEngineFactoryBean;
import org.flowable.form.spring.SpringFormEngineConfiguration;
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
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.transaction.PlatformTransactionManager;

import com.fasterxml.jackson.core.JsonParseException;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
@ExtendWith(LoggingExtension.class)
public class SpringAutoDeployTest {

    protected static final String DEFAULT_VALID_DEPLOYMENT_RESOURCES = "classpath*:/org/flowable/spring/test/autodeployment/simple*.form";

    protected static final String DEFAULT_INVALID_DEPLOYMENT_RESOURCES = "classpath*:/org/flowable/spring/test/autodeployment/*simple*.form";

    protected static final String DEFAULT_VALID_DIRECTORY_DEPLOYMENT_RESOURCES = "classpath*:/org/flowable/spring/test/autodeployment/**/simple*.form";

    protected static final String DEFAULT_INVALID_DIRECTORY_DEPLOYMENT_RESOURCES = "classpath*:/org/flowable/spring/test/autodeployment/**/*simple*.form";

    protected ConfigurableApplicationContext applicationContext;
    protected FormRepositoryService repositoryService;

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
        applicationContext.register(SpringFormAutoDeployTestConfiguration.class);
        applicationContext.getEnvironment().getPropertySources()
            .addLast(new MapPropertySource("springAutoDeploy", properties));
        applicationContext.refresh();
        this.applicationContext = applicationContext;
        this.repositoryService = applicationContext.getBean(FormRepositoryService.class);
    }

    @AfterEach
    protected void tearDown() throws Exception {
        removeAllDeployments();
        if (this.applicationContext != null) {
            this.applicationContext.close();
            this.applicationContext = null;
        }
        this.repositoryService = null;
    }

    @Test
    public void testBasicFlowableSpringIntegration() {
        createAppContextWithoutDeploymentMode();
        List<FormDefinition> formDefinitions = repositoryService.createFormDefinitionQuery().orderByFormDefinitionKey().asc().list();

        Set<String> formDefinitionKeys = new HashSet<>();
        for (FormDefinition formDefinition : formDefinitions) {
            formDefinitionKeys.add(formDefinition.getKey());
        }

        Set<String> expectedFormDefinitionKeys = new HashSet<>();
        expectedFormDefinitionKeys.add("form1");
        expectedFormDefinitionKeys.add("form2");

        assertThat(formDefinitionKeys).isEqualTo(expectedFormDefinitionKeys);
    }

    @Test
    public void testNoRedeploymentForSpringContainerRestart() throws Exception {
        createAppContextWithoutDeploymentMode();
        FormDeploymentQuery deploymentQuery = repositoryService.createDeploymentQuery();
        assertThat(deploymentQuery.count()).isOne();
        FormDefinitionQuery formDefinitionQuery = repositoryService.createFormDefinitionQuery();
        assertThat(formDefinitionQuery.count()).isEqualTo(2);

        // Creating a new app context with same resources doesn't lead to more deployments
        createAppContextWithoutDeploymentMode();
        assertThat(deploymentQuery.count()).isOne();
        assertThat(formDefinitionQuery.count()).isEqualTo(2);
    }

    // Updating the form file should lead to a new deployment when restarting the Spring container
    @Test
    public void testResourceRedeploymentAfterFormDefinitionChange() throws Exception {
        createAppContextWithoutDeploymentMode();
        assertThat(repositoryService.createDeploymentQuery().count()).isOne();
        applicationContext.close();

        String filePath = "org/flowable/spring/test/autodeployment/simple.form";
        String originalFormFileContent = IoUtil.readFileAsString(filePath);
        String updatedFormFileContent = originalFormFileContent.replace("My first form", "My first forms");
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

        // Assertions come AFTER the file write! Otherwise the form file is
        // messed up if the assertions fail.
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(2);
        assertThat(repositoryService.createFormDefinitionQuery().count()).isEqualTo(4);
    }

    @Test
    public void testAutoDeployWithDeploymentModeDefault() {
        createAppContextWithDefaultDeploymentMode();
        assertThat(repositoryService.createDeploymentQuery().count()).isOne();
        assertThat(repositoryService.createFormDefinitionQuery().count()).isEqualTo(2);
    }

    @Test
    public void testAutoDeployWithInvalidResourcesWithDeploymentModeDefault() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("deploymentMode", "default");
        properties.put("deploymentResources", DEFAULT_INVALID_DEPLOYMENT_RESOURCES);
        assertThatThrownBy(() -> createAppContext(properties))
            .hasCauseInstanceOf(FlowableException.class)
            .hasMessageContaining("Error parsing form definition JSON")
            .hasRootCauseInstanceOf(JsonParseException.class);
        assertThat(repositoryService).isNull();

        // Some of the resources should have been deployed
        properties.put("deploymentResources", "classpath*:/notExisting*.form");
        createAppContext(properties);
        assertThat(repositoryService.createFormDefinitionQuery().list())
            .extracting(FormDefinition::getKey)
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

        assertThat(repositoryService.createFormDefinitionQuery().list())
            .extracting(FormDefinition::getKey)
            .isEmpty();
        assertThat(repositoryService.createDeploymentQuery().count()).isZero();
    }

    @Test
    public void testAutoDeployWithDeploymentModeSingleResource() {
        createAppContextWithSingleResourceDeploymentMode();
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(2);
        assertThat(repositoryService.createFormDefinitionQuery().count()).isEqualTo(2);
    }

    @Test
    public void testAutoDeployWithInvalidResourcesWithDeploymentModeSingleResource() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("deploymentMode", "single-resource");
        properties.put("deploymentResources", DEFAULT_INVALID_DEPLOYMENT_RESOURCES);
        assertThatThrownBy(() -> createAppContext(properties))
            .hasCauseInstanceOf(FlowableException.class)
            .hasMessageContaining("Error parsing form definition JSON")
            .hasRootCauseInstanceOf(JsonParseException.class);
        assertThat(repositoryService).isNull();

        // Some of the resources should have been deployed
        properties.put("deploymentResources", "classpath*:/notExisting*.form");
        createAppContext(properties);
        assertThat(repositoryService.createFormDefinitionQuery().list())
            .extracting(FormDefinition::getKey)
            .containsExactlyInAnyOrder("form1", "form2");
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(2);
    }

    @Test
    public void testAutoDeployWithInvalidResourcesAndIgnoreExceptionOnDeploymentWithDeploymentModeSingleResource() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("deploymentMode", "single-resource");
        properties.put("deploymentResources", DEFAULT_INVALID_DEPLOYMENT_RESOURCES);
        properties.put("throwExceptionOnDeploymentFailure", false);
        createAppContext(properties);

        assertThat(repositoryService.createFormDefinitionQuery().list())
            .extracting(FormDefinition::getKey)
            .containsExactlyInAnyOrder("form1", "form2");
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(2);
    }

    @Test
    public void testAutoDeployWithDeploymentModeResourceParentFolder() {
        createAppContextWithResourceParenFolderDeploymentMode();
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(2);
        assertThat(repositoryService.createFormDefinitionQuery().count()).isEqualTo(3);
    }

    @Test
    public void testAutoDeployWithInvalidResourcesWithDeploymentModeResourceParentFolder() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("deploymentMode", "resource-parent-folder");
        properties.put("deploymentResources", DEFAULT_INVALID_DIRECTORY_DEPLOYMENT_RESOURCES);
        assertThatThrownBy(() -> createAppContext(properties))
            .hasCauseInstanceOf(FlowableException.class)
            .hasMessageContaining("Error parsing form definition JSON")
            .hasRootCauseInstanceOf(JsonParseException.class);
        assertThat(repositoryService).isNull();

        // Start a new application context to verify that there are no deployments
        properties.put("deploymentResources", "classpath*:/notExisting*.form");
        createAppContext(properties);

        assertThat(repositoryService.createFormDefinitionQuery().list())
            .extracting(FormDefinition::getKey)
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
        assertThat(repositoryService.createFormDefinitionQuery().list())
            .extracting(FormDefinition::getKey)
            .containsExactlyInAnyOrder("form3");
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(1);
    }

    // --Helper methods
    // ----------------------------------------------------------

    private void removeAllDeployments() {
        if (repositoryService != null) {
            for (FormDeployment deployment : repositoryService.createDeploymentQuery().list()) {
                repositoryService.deleteDeployment(deployment.getId(), true);
            }
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class SpringFormAutoDeployTestConfiguration {

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
        public SpringFormEngineConfiguration processEngineConfiguration(DataSource dataSource, PlatformTransactionManager transactionManager,
            @Value("${databaseSchemaUpdate:true}") String databaseSchemaUpdate,
            @Value("${deploymentMode:#{null}}") String deploymentMode,
            @Value("${deploymentResources}") Resource[] deploymentResources,
            @Value("${throwExceptionOnDeploymentFailure:#{null}}") Boolean throwExceptionOnDeploymentFailure
        ) {
            SpringFormEngineConfiguration formEngineConfiguration = new SpringFormEngineConfiguration();
            formEngineConfiguration.setDataSource(dataSource);
            formEngineConfiguration.setTransactionManager(transactionManager);
            formEngineConfiguration.setDatabaseSchemaUpdate(databaseSchemaUpdate);

            if (deploymentMode != null) {
                formEngineConfiguration.setDeploymentMode(deploymentMode);
            }

            formEngineConfiguration.setDeploymentResources(deploymentResources);

            if (throwExceptionOnDeploymentFailure != null) {
                formEngineConfiguration.getDeploymentStrategies()
                    .forEach(strategy -> {
                        if (strategy instanceof CommonAutoDeploymentStrategy) {
                            ((CommonAutoDeploymentStrategy<FormEngine>) strategy).getDeploymentProperties().setThrowExceptionOnDeploymentFailure(throwExceptionOnDeploymentFailure);
                        }
                    });
            }

            return formEngineConfiguration;
        }

        @Bean
        public FormEngineFactoryBean formEngine(SpringFormEngineConfiguration formEngineConfiguration) {
            FormEngineFactoryBean formEngineFactoryBean = new FormEngineFactoryBean();
            formEngineFactoryBean.setFormEngineConfiguration(formEngineConfiguration);
            return formEngineFactoryBean;
        }

        @Bean
        public FormRepositoryService formRepositoryService(FormEngine formEngine) {
            return formEngine.getFormRepositoryService();
        }
    }

}
