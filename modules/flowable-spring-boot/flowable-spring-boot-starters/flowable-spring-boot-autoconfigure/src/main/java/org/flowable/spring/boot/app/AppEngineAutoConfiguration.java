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
package org.flowable.spring.boot.app;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.flowable.app.engine.AppEngine;
import org.flowable.app.spring.SpringAppEngineConfiguration;
import org.flowable.app.spring.autodeployment.DefaultAutoDeploymentStrategy;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.persistence.StrongUuidGenerator;
import org.flowable.common.spring.AutoDeploymentStrategy;
import org.flowable.common.spring.CommonAutoDeploymentProperties;
import org.flowable.spring.boot.AbstractSpringEngineAutoConfiguration;
import org.flowable.spring.boot.FlowableAutoDeploymentProperties;
import org.flowable.spring.boot.FlowableProperties;
import org.flowable.spring.boot.condition.ConditionalOnAppEngine;
import org.flowable.spring.boot.eventregistry.FlowableEventRegistryProperties;
import org.flowable.spring.boot.idm.FlowableIdmProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Provides sane definitions for the various beans required to be productive with Flowable in Spring.
 *
 * @author Tijs Rademakers
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnAppEngine
@EnableConfigurationProperties({
    FlowableProperties.class,
    FlowableAutoDeploymentProperties.class,
    FlowableAppProperties.class,
    FlowableIdmProperties.class,
    FlowableEventRegistryProperties.class,
})
public class AppEngineAutoConfiguration extends AbstractSpringEngineAutoConfiguration {

    protected final FlowableAppProperties appProperties;
    protected final FlowableIdmProperties idmProperties;
    protected final FlowableEventRegistryProperties eventProperties;
    protected final FlowableAutoDeploymentProperties autoDeploymentProperties;

    public AppEngineAutoConfiguration(FlowableProperties flowableProperties, FlowableAppProperties appProperties,
        FlowableIdmProperties idmProperties, FlowableEventRegistryProperties eventProperties, FlowableAutoDeploymentProperties autoDeploymentProperties) {
        super(flowableProperties);
        this.appProperties = appProperties;
        this.idmProperties = idmProperties;
        this.eventProperties = eventProperties;
        this.autoDeploymentProperties = autoDeploymentProperties;
    }

    @Bean
    @ConditionalOnMissingBean
    public SpringAppEngineConfiguration springAppEngineConfiguration(DataSource dataSource, PlatformTransactionManager platformTransactionManager,
        ObjectProvider<ObjectMapper> objectMapperProvider,
        ObjectProvider<List<AutoDeploymentStrategy<AppEngine>>> appAutoDeploymentStrategies) throws IOException {

        SpringAppEngineConfiguration conf = new SpringAppEngineConfiguration();

        List<Resource> resources = this.discoverDeploymentResources(
            appProperties.getResourceLocation(),
            appProperties.getResourceSuffixes(),
            appProperties.isDeployResources()
        );

        if (resources != null && !resources.isEmpty()) {
            conf.setDeploymentResources(resources.toArray(new Resource[0]));
        }

        configureSpringEngine(conf, platformTransactionManager);
        configureEngine(conf, dataSource);
        ObjectMapper objectMapper = objectMapperProvider.getIfAvailable();
        if (objectMapper != null) {
            conf.setObjectMapper(objectMapper);
        }

        conf.setIdGenerator(new StrongUuidGenerator());

        conf.setDisableIdmEngine(!idmProperties.isEnabled());
        conf.setDisableEventRegistry(!eventProperties.isEnabled());

        // We cannot use orderedStream since we want to support Boot 1.5 which is on pre 5.x Spring
        List<AutoDeploymentStrategy<AppEngine>> deploymentStrategies = appAutoDeploymentStrategies.getIfAvailable();
        if (deploymentStrategies == null) {
            deploymentStrategies = new ArrayList<>();
        }
        CommonAutoDeploymentProperties deploymentProperties = this.autoDeploymentProperties.deploymentPropertiesForEngine(ScopeTypes.APP);
        // Always add the out of the box auto deployment strategies as last
        deploymentStrategies.add(new DefaultAutoDeploymentStrategy(deploymentProperties));
        conf.setDeploymentStrategies(deploymentStrategies);

        return conf;
    }
}