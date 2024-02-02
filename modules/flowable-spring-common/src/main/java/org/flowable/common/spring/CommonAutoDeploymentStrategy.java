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

package org.flowable.common.spring;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.impl.lock.LockManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ContextResource;
import org.springframework.core.io.Resource;

/**
 * Common base class for implementations of {@link AutoDeploymentStrategy}.
 * It has support for using a lock manager to lock before doing the deployment.
 *
 * @author Filip Hrisafov
 */
public abstract class CommonAutoDeploymentStrategy<E> implements AutoDeploymentStrategy<E> {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    protected CommonAutoDeploymentProperties deploymentProperties;

    public CommonAutoDeploymentStrategy() {
        this(new CommonAutoDeploymentProperties(false, Duration.ofMinutes(5), true));
    }

    public CommonAutoDeploymentStrategy(CommonAutoDeploymentProperties deploymentProperties) {
        this.deploymentProperties = deploymentProperties;
    }

    /**
     * Gets the deployment mode this strategy handles.
     *
     * @return the name of the deployment mode
     */
    protected abstract String getDeploymentMode();

    /**
     * Get the lock manager with the given {@code engine} and the {@code deploymentNameHint}.
     * Used when the strategy has been configured to use lock for deployments.
     *
     * @param engine the engine that can be used to get the lock manager.
     * @param deploymentNameHint the deployment name hint
     * @return the lock manager
     */
    protected abstract LockManager getLockManager(E engine, String deploymentNameHint);

    protected String determineLockName(String deploymentNameHint) {
        String lockName = getLockName();
        if (StringUtils.isBlank(lockName)) {
            return deploymentNameHint;
        }

        return lockName;
    }

    @Override
    public boolean handlesMode(final String mode) {
        return StringUtils.equalsIgnoreCase(mode, getDeploymentMode());
    }

    @Override
    public void deployResources(String deploymentNameHint, Resource[] resources, E engine) {
        if (isUseLockForDeployments()) {
            if (logger.isInfoEnabled()) {
                logger.info("Deploying resources {} using a lock for engine {} deployment name hint {}", Arrays.toString(resources), engine, deploymentNameHint);
            }
            LockManager deploymentLockManager = getLockManager(engine, deploymentNameHint);
            deploymentLockManager.waitForLockRunAndRelease(getDeploymentLockWaitTime(), () -> {
                deployResourcesInternal(deploymentNameHint, resources, engine);
                return null;
            });
        } else {
            if (logger.isInfoEnabled()) {
                logger.info("Deploying resources {} for engine {} deployment name hint {}", Arrays.toString(resources), engine, deploymentNameHint);
            }
            deployResourcesInternal(deploymentNameHint, resources, engine);
        }
    }

    /**
     * Methods that should be implemented by sub classes to perform the actual deployment.
     * If use lock for deployments is used than this methods is called with an already acquired lock.
     *
     * @param deploymentNameHint the hint for the name of deployment(s) performed
     * @param resources the resources to be deployed
     * @param engine the engine to use for deployment(s)
     */
    protected abstract void deployResourcesInternal(String deploymentNameHint, Resource[] resources, E engine);

    /**
     * Determines the name to be used for the provided resource.
     *
     * @param resource the resource to get the name for
     * @return the name of the resource
     */
    protected String determineResourceName(final Resource resource) {
        String resourceName = null;

        if (resource instanceof ContextResource) {
            resourceName = ((ContextResource) resource).getPathWithinContext();

        } else if (resource instanceof ByteArrayResource) {
            resourceName = resource.getDescription();

        } else {
            try {
                resourceName = resource.getFile().getAbsolutePath();
            } catch (IOException e) {
                resourceName = resource.getFilename();
            }
        }
        return resourceName;
    }

    public CommonAutoDeploymentProperties getDeploymentProperties() {
        return deploymentProperties;
    }

    public void setDeploymentProperties(CommonAutoDeploymentProperties deploymentProperties) {
        this.deploymentProperties = deploymentProperties;
    }

    public boolean isUseLockForDeployments() {
        return deploymentProperties.isUseLock();
    }

    public Duration getDeploymentLockWaitTime() {
        return deploymentProperties.getLockWaitTime();
    }

    public boolean isThrowExceptionOnDeploymentFailure() {
        return deploymentProperties.isThrowExceptionOnDeploymentFailure();
    }

    public String getLockName() {
        return deploymentProperties.getLockName();
    }
}
