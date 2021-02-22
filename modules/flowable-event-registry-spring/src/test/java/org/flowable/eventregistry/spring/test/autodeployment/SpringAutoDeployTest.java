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

package org.flowable.eventregistry.spring.test.autodeployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Driver;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.flowable.common.engine.impl.test.LoggingExtension;
import org.flowable.common.engine.impl.util.IoUtil;
import org.flowable.common.spring.CommonAutoDeploymentStrategy;
import org.flowable.eventregistry.api.EventDefinition;
import org.flowable.eventregistry.api.EventDefinitionQuery;
import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.api.EventDeploymentQuery;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.impl.EventRegistryEngine;
import org.flowable.eventregistry.spring.EventRegistryFactoryBean;
import org.flowable.eventregistry.spring.SpringEventRegistryEngineConfiguration;
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

@ExtendWith(LoggingExtension.class)
public class SpringAutoDeployTest {

    protected static final String DEFAULT_VALID_DEPLOYMENT_RESOURCES = "classpath*:/org/flowable/eventregistry/spring/test/autodeployment/simple*.*";

    protected static final String DEFAULT_INVALID_DEPLOYMENT_RESOURCES = "classpath*:/org/flowable/eventregistry/spring/test/autodeployment/*simple*.event";

    protected static final String DEFAULT_VALID_DIRECTORY_DEPLOYMENT_RESOURCES = "classpath*:/org/flowable/eventregistry/spring/test/autodeployment/**/simple*.event";

    protected static final String DEFAULT_INVALID_DIRECTORY_DEPLOYMENT_RESOURCES = "classpath*:/org/flowable/eventregistry/spring/test/autodeployment/**/*simple*.event";

    protected ConfigurableApplicationContext applicationContext;
    protected EventRepositoryService repositoryService;

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

    protected void createAppContextWithResourceParentFolderDeploymentMode() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("deploymentMode", "resource-parent-folder");
        properties.put("deploymentResources", DEFAULT_VALID_DIRECTORY_DEPLOYMENT_RESOURCES);
        createAppContext(properties);
    }

    protected void createAppContext(Map<String, Object> properties) {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.register(SpringEventAutoDeployTestConfiguration.class);
        applicationContext.getEnvironment().getPropertySources()
            .addLast(new MapPropertySource("springAutoDeploy", properties));
        applicationContext.refresh();
        this.applicationContext = applicationContext;
        this.repositoryService = applicationContext.getBean(EventRepositoryService.class);
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
        List<EventDefinition> eventDefinitions = repositoryService.createEventDefinitionQuery().orderByEventDefinitionKey().asc().list();

        Set<String> eventDefinitionKeys = new HashSet<>();
        for (EventDefinition eventDefinition : eventDefinitions) {
            eventDefinitionKeys.add(eventDefinition.getKey());
        }

        Set<String> expectedEventDefinitionKeys = new HashSet<>();
        expectedEventDefinitionKeys.add("myEvent");
        expectedEventDefinitionKeys.add("myEvent2");

        assertThat(eventDefinitionKeys).isEqualTo(expectedEventDefinitionKeys);
    }

    @Test
    public void testNoRedeploymentForSpringContainerRestart() throws Exception {
        createAppContextWithoutDeploymentMode();
        EventDeploymentQuery deploymentQuery = repositoryService.createDeploymentQuery();
        assertThat(deploymentQuery.count()).isOne();
        EventDefinitionQuery eventDefinitionQuery = repositoryService.createEventDefinitionQuery();
        assertThat(eventDefinitionQuery.count()).isEqualTo(2);

        // Creating a new app context with same resources doesn't lead to more deployments
        createAppContextWithoutDeploymentMode();
        assertThat(deploymentQuery.count()).isOne();
        assertThat(eventDefinitionQuery.count()).isEqualTo(2);
    }

    // Updating the event file should lead to a new deployment when restarting the Spring container
    @Test
    public void testResourceRedeploymentAfterEventDefinitionChange() throws Exception {
        createAppContextWithoutDeploymentMode();
        assertThat(repositoryService.createDeploymentQuery().count()).isOne();
        applicationContext.close();

        String filePath = "org/flowable/eventregistry/spring/test/autodeployment/simpleEvent.event";
        String originalEventFileContent = IoUtil.readFileAsString(filePath);
        String updatedEventFileContent = originalEventFileContent.replace("My event", "My event test");
        assertThat(updatedEventFileContent).hasSizeGreaterThan(originalEventFileContent.length());
        IoUtil.writeStringToFile(updatedEventFileContent, filePath);

        // Classic produced/consumer problem here:
        // The file is already written in Java, but not yet completely persisted by the OS
        // Constructing the new app context reads the same file which is sometimes not yet fully written to disk
        Thread.sleep(2000);

        try {
            createAppContextWithoutDeploymentMode();
        } finally {
            // Reset file content such that future test are not seeing something funny
            IoUtil.writeStringToFile(originalEventFileContent, filePath);
        }

        // Assertions come AFTER the file write! Otherwise the event file is
        // messed up if the assertions fail.
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(2);
        assertThat(repositoryService.createEventDefinitionQuery().count()).isEqualTo(4);
    }

    @Test
    public void testAutoDeployWithDeploymentModeDefault() {
        createAppContextWithDefaultDeploymentMode();
        assertThat(repositoryService.createDeploymentQuery().count()).isOne();
        assertThat(repositoryService.createEventDefinitionQuery().count()).isEqualTo(2);
        assertThat(repositoryService.createChannelDefinitionQuery().count()).isOne();
    }

    @Test
    public void testAutoDeployWithInvalidResourcesAndIgnoreExceptionWithDeploymentModeDefault() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("deploymentMode", "default");
        properties.put("deploymentResources", DEFAULT_INVALID_DEPLOYMENT_RESOURCES);
        properties.put("throwExceptionOnDeploymentFailure", false);
        createAppContext(properties);

        assertThat(repositoryService.createEventDefinitionQuery().list())
            .extracting(EventDefinition::getKey)
            .containsExactlyInAnyOrder("myEvent", "myEvent2");
        assertThat(repositoryService.createDeploymentQuery().count()).isOne();
    }

    @Test
    public void testAutoDeployWithDeploymentModeSingleResource() {
        createAppContextWithSingleResourceDeploymentMode();
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(3);
        assertThat(repositoryService.createEventDefinitionQuery().count()).isEqualTo(2);
        assertThat(repositoryService.createChannelDefinitionQuery().count()).isOne();
    }

    @Test
    public void testAutoDeployWithInvalidResourcesAndIgnoreExceptionOnDeploymentWithDeploymentModeSingleResource() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("deploymentMode", "single-resource");
        properties.put("deploymentResources", DEFAULT_INVALID_DEPLOYMENT_RESOURCES);
        properties.put("throwExceptionOnDeploymentFailure", false);
        createAppContext(properties);

        assertThat(repositoryService.createEventDefinitionQuery().list())
            .extracting(EventDefinition::getKey)
            .containsExactlyInAnyOrder("myEvent", "myEvent2");
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(2);
    }

    @Test
    public void testAutoDeployWithDeploymentModeResourceParentFolder() {
        createAppContextWithResourceParentFolderDeploymentMode();
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(2);
        assertThat(repositoryService.createEventDefinitionQuery().count()).isEqualTo(3);
    }

    @Test
    public void testAutoDeployWithInvalidResourcesAndIgnoreExceptionOnDeploymentWithDeploymentModeResourceParentFolder() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("deploymentMode", "resource-parent-folder");
        properties.put("deploymentResources", DEFAULT_INVALID_DIRECTORY_DEPLOYMENT_RESOURCES);
        properties.put("throwExceptionOnDeploymentFailure", false);
        createAppContext(properties);
        assertThat(repositoryService.createEventDefinitionQuery().list())
            .extracting(EventDefinition::getKey)
            .containsExactlyInAnyOrder("myEvent", "myEvent2", "myEvent3");
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(2);
    }

    // --Helper methods
    // ----------------------------------------------------------

    private void removeAllDeployments() {
        if (repositoryService != null) {
            for (EventDeployment deployment : repositoryService.createDeploymentQuery().list()) {
                repositoryService.deleteDeployment(deployment.getId());
            }
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class SpringEventAutoDeployTestConfiguration {

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
        public SpringEventRegistryEngineConfiguration eventRegistryConfiguration(DataSource dataSource, PlatformTransactionManager transactionManager,
            @Value("${databaseSchemaUpdate:true}") String databaseSchemaUpdate,
            @Value("${deploymentMode:#{null}}") String deploymentMode,
            @Value("${deploymentResources}") Resource[] deploymentResources,
            @Value("${throwExceptionOnDeploymentFailure:#{null}}") Boolean throwExceptionOnDeploymentFailure
        ) {
            SpringEventRegistryEngineConfiguration eventEngineConfiguration = new SpringEventRegistryEngineConfiguration();
            eventEngineConfiguration.setDataSource(dataSource);
            eventEngineConfiguration.setTransactionManager(transactionManager);
            eventEngineConfiguration.setDatabaseSchemaUpdate(databaseSchemaUpdate);

            if (deploymentMode != null) {
                eventEngineConfiguration.setDeploymentMode(deploymentMode);
            }

            eventEngineConfiguration.setDeploymentResources(deploymentResources);

            if (throwExceptionOnDeploymentFailure != null) {
                eventEngineConfiguration.getDeploymentStrategies()
                    .forEach(strategy -> {
                        if (strategy instanceof CommonAutoDeploymentStrategy) {
                            ((CommonAutoDeploymentStrategy<EventRegistryEngine>) strategy).getDeploymentProperties().setThrowExceptionOnDeploymentFailure(throwExceptionOnDeploymentFailure);
                        }
                    });
            }

            return eventEngineConfiguration;
        }

        @Bean
        public EventRegistryFactoryBean eventRegistryEngine(SpringEventRegistryEngineConfiguration eventEngineConfiguration) {
            EventRegistryFactoryBean eventEngineFactoryBean = new EventRegistryFactoryBean();
            eventEngineFactoryBean.setEventEngineConfiguration(eventEngineConfiguration);
            return eventEngineFactoryBean;
        }

        @Bean
        public EventRepositoryService eventRepositoryService(EventRegistryEngine eventRegistryEngine) {
            return eventRegistryEngine.getEventRepositoryService();
        }
    }

}
