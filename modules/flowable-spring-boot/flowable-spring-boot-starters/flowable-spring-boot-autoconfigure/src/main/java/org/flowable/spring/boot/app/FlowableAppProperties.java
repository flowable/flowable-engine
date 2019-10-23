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

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import org.flowable.spring.boot.FlowableServlet;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * @author Tijs Rademakers
 */
@ConfigurationProperties(prefix = "flowable.app")
public class FlowableAppProperties {
    
    /**
     * The location where the App resources are located.
     * Default is {@code classpath*:/apps/}
     */
    private String resourceLocation = "classpath*:/apps/";

    /**
     * The suffixes for the resources that need to be scanned.
     * Default is {@code **.zip, **.bar}
     */
    private List<String> resourceSuffixes = Arrays.asList("**.zip", "**.bar");

    /**
     * Whether to perform deployment of resources, default is {@code true}.
     */
    private boolean deployResources = true;

    /**
     * Whether to use a lock when performing the auto deployment.
     * If not set then the global default would be used.
     */
    private Boolean useLockForAutoDeployment;

    /**
     * Duration to wait for the auto deployment lock before giving up.
     * If not set then the global default would be used.
     */
    private Duration autoDeploymentLockWaitTime;

    /**
     * Whether to throw an exception if there was some kind of failure during the auto deployment.
     * If not set then the global default would be used.
     */
    private Boolean throwExceptionOnAutoDeploymentFailure;

    /**
     * The servlet configuration for the Process Rest API.
     */
    @NestedConfigurationProperty
    private final FlowableServlet servlet = new FlowableServlet("/app-api", "Flowable App Rest API");
    
    public FlowableServlet getServlet() {
        return servlet;
    }
    
    /**
     * Whether the App engine needs to be started.
     */
    private boolean enabled = true;
    
    public String getResourceLocation() {
        return resourceLocation;
    }

    public void setResourceLocation(String resourceLocation) {
        this.resourceLocation = resourceLocation;
    }

    public List<String> getResourceSuffixes() {
        return resourceSuffixes;
    }

    public void setResourceSuffixes(List<String> resourceSuffixes) {
        this.resourceSuffixes = resourceSuffixes;
    }

    public boolean isDeployResources() {
        return deployResources;
    }

    public void setDeployResources(boolean deployResources) {
        this.deployResources = deployResources;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getUseLockForAutoDeployment() {
        return useLockForAutoDeployment;
    }

    public void setUseLockForAutoDeployment(Boolean useLockForAutoDeployment) {
        this.useLockForAutoDeployment = useLockForAutoDeployment;
    }

    public Duration getAutoDeploymentLockWaitTime() {
        return autoDeploymentLockWaitTime;
    }

    public Boolean getThrowExceptionOnAutoDeploymentFailure() {
        return throwExceptionOnAutoDeploymentFailure;
    }

    public void setThrowExceptionOnAutoDeploymentFailure(Boolean throwExceptionOnAutoDeploymentFailure) {
        this.throwExceptionOnAutoDeploymentFailure = throwExceptionOnAutoDeploymentFailure;
    }

    public void setAutoDeploymentLockWaitTime(Duration autoDeploymentLockWaitTime) {
        this.autoDeploymentLockWaitTime = autoDeploymentLockWaitTime;
    }
}
