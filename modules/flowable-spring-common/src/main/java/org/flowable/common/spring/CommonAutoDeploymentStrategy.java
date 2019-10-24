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

    protected boolean useLockForDeployments;
    protected Duration deploymentLockWaitTime;
    protected boolean throwExceptionOnDeploymentFailure;

    public CommonAutoDeploymentStrategy() {
        this(false, Duration.ofMinutes(5), true);
    }

    public CommonAutoDeploymentStrategy(boolean useLockForDeployments, Duration deploymentLockWaitTime) {
        this(useLockForDeployments, deploymentLockWaitTime, true);
    }

    public CommonAutoDeploymentStrategy(boolean useLockForDeployments, Duration deploymentLockWaitTime, boolean throwExceptionOnDeploymentFailure) {
        this.useLockForDeployments = useLockForDeployments;
        this.deploymentLockWaitTime = deploymentLockWaitTime;
        this.throwExceptionOnDeploymentFailure = throwExceptionOnDeploymentFailure;
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

    @Override
    public boolean handlesMode(final String mode) {
        return StringUtils.equalsIgnoreCase(mode, getDeploymentMode());
    }

    @Override
    public void deployResources(String deploymentNameHint, Resource[] resources, E engine) {
        if (useLockForDeployments) {
            LockManager deploymentLockManager = getLockManager(engine, deploymentNameHint);
            deploymentLockManager.waitForLockRunAndRelease(deploymentLockWaitTime, () -> {
                deployResourcesInternal(deploymentNameHint, resources, engine);
                return null;
            });
        } else {
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

    public boolean isUseLockForDeployments() {
        return useLockForDeployments;
    }

    public void setUseLockForDeployments(boolean useLockForDeployments) {
        this.useLockForDeployments = useLockForDeployments;
    }

    public Duration getDeploymentLockWaitTime() {
        return deploymentLockWaitTime;
    }

    public void setDeploymentLockWaitTime(Duration deploymentLockWaitTime) {
        this.deploymentLockWaitTime = deploymentLockWaitTime;
    }

    public boolean isThrowExceptionOnDeploymentFailure() {
        return throwExceptionOnDeploymentFailure;
    }

    public void setThrowExceptionOnDeploymentFailure(boolean throwExceptionOnDeploymentFailure) {
        this.throwExceptionOnDeploymentFailure = throwExceptionOnDeploymentFailure;
    }
}
