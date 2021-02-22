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
import static org.assertj.core.api.Assertions.fail;

import java.net.URISyntaxException;
import java.sql.Driver;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.flowable.bpmn.exceptions.XMLException;
import org.flowable.common.engine.impl.util.IoUtil;
import org.flowable.common.spring.CommonAutoDeploymentStrategy;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.impl.test.AbstractTestCase;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentQuery;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.spring.ProcessEngineFactoryBean;
import org.flowable.spring.SpringProcessEngineConfiguration;
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
 * @author Tom Baeyens
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class SpringAutoDeployTest extends AbstractTestCase {

    protected static final String DEFAULT_VALID_DEPLOYMENT_RESOURCES = "classpath*:/org/flowable/spring/test/autodeployment/autodeploy.*.bpmn20.xml";

    protected static final String DEFAULT_INVALID_DEPLOYMENT_RESOURCES = "classpath*:/org/flowable/spring/test/autodeployment/*autodeploy.*.bpmn20.xml";

    protected static final String DEFAULT_VALID_DIRECTORY_DEPLOYMENT_RESOURCES = "classpath*:/org/flowable/spring/test/autodeployment/**/autodeploy.*.bpmn20.xml";

    protected static final String DEFAULT_INVALID_DIRECTORY_DEPLOYMENT_RESOURCES = "classpath*:/org/flowable/spring/test/autodeployment/**/*autodeploy.*.bpmn20.xml";

    protected ConfigurableApplicationContext applicationContext;
    protected RepositoryService repositoryService;

    protected void createAppContextWithCreateDropDb() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("databaseSchemaUpdate", "create-drop");
        properties.put("deploymentResources", DEFAULT_VALID_DEPLOYMENT_RESOURCES);
        properties.put("jdbc.url", "jdbc:h2:mem:SpringAutoDeployTest;DB_CLOSE_DELAY=1000");
        createAppContext(properties);
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
        applicationContext.register(SpringAutoDeployTestConfiguration.class);
        applicationContext.getEnvironment().getPropertySources()
                .addLast(new MapPropertySource("springAutoDeploy", properties));
        applicationContext.refresh();
        this.applicationContext = applicationContext;
        this.repositoryService = applicationContext.getBean(RepositoryService.class);
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
    public void testBasicActivitiSpringIntegration() {
        createAppContextWithoutDeploymentMode();
        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().list();

        Set<String> processDefinitionKeys = new HashSet<>();
        for (ProcessDefinition processDefinition : processDefinitions) {
            processDefinitionKeys.add(processDefinition.getKey());
        }

        Set<String> expectedProcessDefinitionKeys = new HashSet<>();
        expectedProcessDefinitionKeys.add("a");
        expectedProcessDefinitionKeys.add("b");
        expectedProcessDefinitionKeys.add("c");

        assertThat(expectedProcessDefinitionKeys).isEqualTo(processDefinitionKeys);
    }

    @Test
    public void testNoRedeploymentForSpringContainerRestart() throws Exception {
        createAppContextWithoutDeploymentMode();
        DeploymentQuery deploymentQuery = repositoryService.createDeploymentQuery();
        assertThat(deploymentQuery.count()).isEqualTo(1);
        ProcessDefinitionQuery processDefinitionQuery = repositoryService.createProcessDefinitionQuery();
        assertThat(processDefinitionQuery.count()).isEqualTo(3);

        // Creating a new app context with same resources doesn't lead to more
        // deployments
        createAppContextWithoutDeploymentMode();
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(1);
        assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(3);
    }

    // Updating the bpmn20 file should lead to a new deployment when restarting
    // the Spring container
    @Test
    public void testResourceRedeploymentAfterProcessDefinitionChange() throws Exception {
        createAppContextWithoutDeploymentMode();
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(1);
        applicationContext.close();

        String filePath = "org/flowable/spring/test/autodeployment/autodeploy.a.bpmn20.xml";
        String originalBpmnFileContent = IoUtil.readFileAsString(filePath);
        String updatedBpmnFileContent = originalBpmnFileContent.replace("flow1", "fromStartToEndFlow");
        assertThat(updatedBpmnFileContent).hasSizeGreaterThan(originalBpmnFileContent.length());
        IoUtil.writeStringToFile(updatedBpmnFileContent, filePath);

        // Classic produced/consumer problem here:
        // The file is already written in Java, but not yet completely persisted
        // by the OS
        // Constructing the new app context reads the same file which is
        // sometimes not yet fully written to disk
        waitUntilFileIsWritten(filePath, updatedBpmnFileContent.length());

        try {
            createAppContextWithoutDeploymentMode();
        } finally {
            // Reset file content such that future test are not seeing something funny
            IoUtil.writeStringToFile(originalBpmnFileContent, filePath);
        }

        // Assertions come AFTER the file write! Otherwise the process file is
        // messed up if the assertions fail.
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(2);
        assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(6);
    }

    @Test
    public void testAutoDeployWithCreateDropOnCleanDb() {
        createAppContextWithCreateDropDb();
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(1);
        assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(3);
    }

    @Test
    public void testAutoDeployWithDeploymentModeDefault() {
        createAppContextWithDefaultDeploymentMode();
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(1);
        assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(3);
    }

    @Test
    public void testAutoDeployWithInvalidResourcesWithDeploymentModeDefault() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("deploymentMode", "default");
        properties.put("deploymentResources", DEFAULT_INVALID_DEPLOYMENT_RESOURCES);
        assertThatThrownBy(() -> createAppContext(properties))
                .hasCauseInstanceOf(XMLException.class);
        assertThat(repositoryService).isNull();

        // Some of the resources should have been deployed
        properties.put("deploymentResources", "classpath*:/notExisting*.bpmn20.xml");
        createAppContext(properties);
        assertThat(repositoryService.createProcessDefinitionQuery().list())
                .extracting(ProcessDefinition::getKey)
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

        assertThat(repositoryService.createProcessDefinitionQuery().list())
                .extracting(ProcessDefinition::getKey)
                .isEmpty();
        assertThat(repositoryService.createDeploymentQuery().count()).isZero();
    }

    @Test
    public void testAutoDeployWithDeploymentModeSingleResource() {
        createAppContextWithSingleResourceDeploymentMode();
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(3);
        assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(3);
    }

    @Test
    public void testAutoDeployWithInvalidResourcesWithDeploymentModeSingleResource() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("deploymentMode", "single-resource");
        properties.put("deploymentResources", DEFAULT_INVALID_DEPLOYMENT_RESOURCES);
        assertThatThrownBy(() -> createAppContext(properties))
                .hasCauseInstanceOf(XMLException.class);
        assertThat(repositoryService).isNull();

        // Some of the resources should have been deployed
        properties.put("deploymentResources", "classpath*:/notExisting*.bpmn20.xml");
        createAppContext(properties);
        assertThat(repositoryService.createProcessDefinitionQuery().list())
                .extracting(ProcessDefinition::getKey)
                .containsExactlyInAnyOrder("a", "b", "c");
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(3);
    }

    @Test
    public void testAutoDeployWithInvalidResourcesAndIgnoreExceptionOnDeploymentWithDeploymentModeSingleResource() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("deploymentMode", "single-resource");
        properties.put("deploymentResources", DEFAULT_INVALID_DEPLOYMENT_RESOURCES);
        properties.put("throwExceptionOnDeploymentFailure", false);
        createAppContext(properties);

        assertThat(repositoryService.createProcessDefinitionQuery().list())
                .extracting(ProcessDefinition::getKey)
                .containsExactlyInAnyOrder("a", "b", "c");
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(3);
    }

    @Test
    public void testAutoDeployWithDeploymentModeResourceParentFolder() {
        createAppContextWithResourceParenFolderDeploymentMode();
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(2);
        assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(4);
    }

    @Test
    public void testAutoDeployWithInvalidResourcesWithDeploymentModeResourceParentFolder() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("deploymentMode", "resource-parent-folder");
        properties.put("deploymentResources", DEFAULT_INVALID_DIRECTORY_DEPLOYMENT_RESOURCES);
        assertThatThrownBy(() -> createAppContext(properties))
                .hasCauseInstanceOf(XMLException.class);
        assertThat(repositoryService).isNull();

        // Start a new application context to verify that there are no deployments
        properties.put("deploymentResources", "classpath*:/notExisting*.bpmn20.xml");
        createAppContext(properties);

        assertThat(repositoryService.createProcessDefinitionQuery().list())
                .extracting(ProcessDefinition::getKey)
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
        assertThat(repositoryService.createProcessDefinitionQuery().list())
                .extracting(ProcessDefinition::getKey)
                .containsExactlyInAnyOrder("d");
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(1);
    }

    // --Helper methods
    // ----------------------------------------------------------

    private void removeAllDeployments() {
        if (repositoryService != null) {
            for (Deployment deployment : repositoryService.createDeploymentQuery().list()) {
                repositoryService.deleteDeployment(deployment.getId(), true);
            }
        }
    }

    private boolean waitUntilFileIsWritten(String filePath, int expectedBytes) throws URISyntaxException {
        while (IoUtil.getFile(filePath).length() != (long) expectedBytes) {
            try {
                wait(100L);
            } catch (InterruptedException e) {
                fail(e.getMessage());
            }
        }
        return true;
    }

    @Configuration(proxyBeanMethods = false)
    static class SpringAutoDeployTestConfiguration {

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
        public SpringProcessEngineConfiguration processEngineConfiguration(DataSource dataSource, PlatformTransactionManager transactionManager,
                @Value("${databaseSchemaUpdate:true}") String databaseSchemaUpdate,
                @Value("${deploymentMode:#{null}}") String deploymentMode,
                @Value("${deploymentResources}") Resource[] deploymentResources,
                @Value("${throwExceptionOnDeploymentFailure:#{null}}") Boolean throwExceptionOnDeploymentFailure
        ) {
            SpringProcessEngineConfiguration processEngineConfiguration = new SpringProcessEngineConfiguration();
            processEngineConfiguration.setDataSource(dataSource);
            processEngineConfiguration.setTransactionManager(transactionManager);
            processEngineConfiguration.setDatabaseSchemaUpdate(databaseSchemaUpdate);

            if (deploymentMode != null) {
                processEngineConfiguration.setDeploymentMode(deploymentMode);
            }

            processEngineConfiguration.setDeploymentResources(deploymentResources);

            if (throwExceptionOnDeploymentFailure != null) {
                processEngineConfiguration.getDeploymentStrategies()
                        .forEach(strategy -> {
                            if (strategy instanceof CommonAutoDeploymentStrategy) {
                                ((CommonAutoDeploymentStrategy<ProcessEngine>) strategy).getDeploymentProperties()
                                        .setThrowExceptionOnDeploymentFailure(throwExceptionOnDeploymentFailure);
                            }
                        });
            }

            return processEngineConfiguration;
        }

        @Bean
        public ProcessEngineFactoryBean processEngine(SpringProcessEngineConfiguration processEngineConfiguration) {
            ProcessEngineFactoryBean processEngineFactoryBean = new ProcessEngineFactoryBean();
            processEngineFactoryBean.setProcessEngineConfiguration(processEngineConfiguration);
            return processEngineFactoryBean;
        }

        @Bean
        public RepositoryService repositoryService(ProcessEngine processEngine) {
            return processEngine.getRepositoryService();
        }
    }

}
