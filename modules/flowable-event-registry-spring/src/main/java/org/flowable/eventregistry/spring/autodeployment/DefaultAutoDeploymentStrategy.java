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

package org.flowable.eventregistry.spring.autodeployment;

import org.flowable.common.spring.CommonAutoDeploymentProperties;
import org.flowable.eventregistry.api.EventDeploymentBuilder;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.impl.EventRegistryEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

/**
 * Default implementation of {@link org.flowable.common.spring.AutoDeploymentStrategy AutoDeploymentStrategy}
 * that groups all {@link Resource}s into a single deployment. This implementation is equivalent to the previously used implementation.
 */
public class DefaultAutoDeploymentStrategy extends AbstractEventAutoDeploymentStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAutoDeploymentStrategy.class);

    /**
     * The deployment mode this strategy handles.
     */
    public static final String DEPLOYMENT_MODE = "default";

    public DefaultAutoDeploymentStrategy() {
    }

    public DefaultAutoDeploymentStrategy(CommonAutoDeploymentProperties deploymentProperties) {
        super(deploymentProperties);
    }

    @Override
    protected String getDeploymentMode() {
        return DEPLOYMENT_MODE;
    }

    @Override
    protected void deployResourcesInternal(String deploymentNameHint, Resource[] resources, EventRegistryEngine engine) {
        EventRepositoryService repositoryService = engine.getEventRepositoryService();

        // Create a single deployment for all resources using the name hint as the literal name
        final EventDeploymentBuilder deploymentBuilder = repositoryService.createDeployment().enableDuplicateFiltering().name(deploymentNameHint);
        for (final Resource resource : resources) {
            addResource(resource, deploymentBuilder);
        }

        try {

            deploymentBuilder.deploy();

        } catch (RuntimeException e) {
            if (isThrowExceptionOnDeploymentFailure()) {
                throw e;
            } else {
                LOGGER.warn("Exception while autodeploying event definitions. "
                    + "This exception can be ignored if the root cause indicates a unique constraint violation, "
                    + "which is typically caused by two (or more) servers booting up at the exact same time and deploying the same definitions. ", e);
            }
        }

    }

}
