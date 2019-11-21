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

package org.flowable.app.spring.autodeployment;

import org.flowable.app.api.AppRepositoryService;
import org.flowable.app.api.repository.AppDeploymentBuilder;
import org.flowable.app.engine.AppEngine;
import org.flowable.common.spring.CommonAutoDeploymentProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

/**
 * Default Implementation of {@link org.flowable.common.spring.AutoDeploymentStrategy AutoDeploymentStrategy}
 * that performs a separate deployment for each resource by name.
 * 
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class DefaultAutoDeploymentStrategy extends AbstractAppAutoDeploymentStrategy {

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
    protected void deployResourcesInternal(String deploymentNameHint, Resource[] resources, AppEngine engine) {
        AppRepositoryService repositoryService = engine.getAppRepositoryService();

        // Create a separate deployment for each resource using the resource name

        for (final Resource resource : resources) {

            String resourceName = determineResourceName(resource);
            if (resourceName.contains("/")) {
                resourceName = resourceName.substring(resourceName.lastIndexOf('/') + 1);

            } else if (resourceName.contains("\\")) {
                resourceName = resourceName.substring(resourceName.lastIndexOf('\\') + 1);
            }
            final AppDeploymentBuilder deploymentBuilder = repositoryService.createDeployment().enableDuplicateFiltering().name(resourceName);
            addResource(resource, resourceName, deploymentBuilder);

            try {

                deploymentBuilder.deploy();

            } catch (RuntimeException e) {
                if (isThrowExceptionOnDeploymentFailure()) {
                    throw e;
                } else {
                    LOGGER.warn("Exception while autodeploying app definition for resource {}. "
                        + "This exception can be ignored if the root cause indicates a unique constraint violation, "
                        + "which is typically caused by two (or more) servers booting up at the exact same time and deploying the same definitions. ", resource, e);
                }
            }
        }
    }

}
