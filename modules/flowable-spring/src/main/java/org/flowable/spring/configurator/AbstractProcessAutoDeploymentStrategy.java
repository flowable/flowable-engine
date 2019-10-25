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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.zip.ZipInputStream;

import org.flowable.common.engine.impl.lock.LockManager;
import org.flowable.common.spring.CommonAutoDeploymentProperties;
import org.flowable.common.spring.CommonAutoDeploymentStrategy;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.repository.DeploymentBuilder;
import org.springframework.core.io.Resource;

/**
 * Abstract base class for implementations of {@link org.flowable.common.spring.AutoDeploymentStrategy AutoDeploymentStrategy}
 * for the {@link ProcessEngine}.
 *
 * @author Tiese Barrell
 * @author Filip Hrisafov
 */
public abstract class AbstractProcessAutoDeploymentStrategy extends CommonAutoDeploymentStrategy<ProcessEngine> {

    public AbstractProcessAutoDeploymentStrategy() {
        super();
    }

    public AbstractProcessAutoDeploymentStrategy(CommonAutoDeploymentProperties deploymentProperties) {
        super(deploymentProperties);
    }

    @Override
    protected LockManager getLockManager(ProcessEngine engine, String deploymentNameHint) {
        return engine.getManagementService().getLockManager(determineLockName(deploymentNameHint));
    }

    protected void addResource(Resource resource, DeploymentBuilder deploymentBuilder) {
        String resourceName = determineResourceName(resource);
        addResource(resource, resourceName, deploymentBuilder);
    }

    protected void addResource(Resource resource, String resourceName, DeploymentBuilder deploymentBuilder) {
        try (InputStream inputStream = resource.getInputStream()) {
            if (resourceName.endsWith(".bar") || resourceName.endsWith(".zip") || resourceName.endsWith(".jar")) {
                try (ZipInputStream zipStream = new ZipInputStream(inputStream)) {
                    deploymentBuilder.addZipInputStream(zipStream);
                }
            } else {
                deploymentBuilder.addInputStream(resourceName, inputStream);
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to read resource " + resource, ex);
        }
    }

}
