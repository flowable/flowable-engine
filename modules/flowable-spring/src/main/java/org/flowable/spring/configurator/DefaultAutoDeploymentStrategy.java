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

package org.flowable.spring.configurator;

import java.util.zip.ZipInputStream;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.DeploymentBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

/**
 * Default implementation of {@link AutoDeploymentStrategy} that groups all {@link Resource}s into a single deployment.
 * This implementation is equivalent to the previously used implementation.
 * 
 * @author Tiese Barrell
 * @author Joram Barrez
 */
public class DefaultAutoDeploymentStrategy extends AbstractAutoDeploymentStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAutoDeploymentStrategy.class);

    /**
     * The deployment mode this strategy handles.
     */
    public static final String DEPLOYMENT_MODE = "default";

    @Override
    protected String getDeploymentMode() {
        return DEPLOYMENT_MODE;
    }

    @Override
    public void deployResources(final String deploymentNameHint, final Resource[] resources, final RepositoryService repositoryService) {

        try {
            // Create a single deployment for all resources using the name hint as the literal name
            final DeploymentBuilder deploymentBuilder = repositoryService.createDeployment().enableDuplicateFiltering().name(deploymentNameHint);

            for (final Resource resource : resources) {
                final String resourceName = determineResourceName(resource);
                if (resourceName.endsWith(".bar") || resourceName.endsWith(".zip") || resourceName.endsWith(".jar")) {
                    deploymentBuilder.addZipInputStream(new ZipInputStream(resource.getInputStream()));
                } else {
                    deploymentBuilder.addInputStream(resourceName, resource.getInputStream());
                }

            }

            deploymentBuilder.deploy();

        } catch (Exception e) {
            // Any exception should not stop the bootup of the engine
            LOGGER.warn("Exception while autodeploying process definitions. "
                + "This exception can be ignored if the root cause indicates a unique constraint violation, "
                + "which is typically caused by two (or more) servers booting up at the exact same time and deploying the same definitions. ", e);
        }

    }

}
