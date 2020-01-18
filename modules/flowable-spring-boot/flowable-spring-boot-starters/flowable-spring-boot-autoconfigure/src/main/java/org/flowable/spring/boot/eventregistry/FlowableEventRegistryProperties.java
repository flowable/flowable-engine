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
package org.flowable.spring.boot.eventregistry;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import org.flowable.spring.boot.FlowableServlet;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Properties for configuring the event registry engine.
 */
@ConfigurationProperties(prefix = "flowable.eventregistry")
public class FlowableEventRegistryProperties {

    /**
     * The name of the deployment for the event registry resources.
     */
    private String deploymentName = "SpringBootAutoDeployment";

    /**
     * The location where the event resources are located.
     * Default is {@code classpath*:/eventregistry/}
     */
    private String resourceLocation = "classpath*:/eventregistry/";

    /**
     * The suffixes for the resources that need to be scanned.
     * Default is {@code **.event} and {@code **.channel}
     */
    private List<String> resourceSuffixes = Arrays.asList("**.event", "**.channel");

    /**
     * Whether to perform deployment of resources, default is true.
     */
    private boolean deployResources = true;

    /**
     * Whether to enable the automatic detection of changes when done on other engines (but against same database).
     */
    private boolean enableChangeDetection = false;

    /**
     * If change detection is enabled, this duration configures how long it will take until the first check is done.
     */
    private Duration changeDetectionInitialDelay = Duration.ofSeconds(10);

    /**
     * If change detection is enabled, this duration configures the time between two consecutive checks.
     */
    private Duration changeDetectionDelay = Duration.ofSeconds(60);

    /**
     * Whether the event registry engine needs to be started.
     */
    private boolean enabled = true;

    /**
     * The servlet configuration for the event registry Rest API.
     */
    @NestedConfigurationProperty
    private final FlowableServlet servlet = new FlowableServlet("/event-registry-api", "Flowable Event Registry Rest API");

    public String getDeploymentName() {
        return deploymentName;
    }

    public void setDeploymentName(String deploymentName) {
        this.deploymentName = deploymentName;
    }

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

    public boolean isEnableChangeDetection() {
        return enableChangeDetection;
    }

    public void setEnableChangeDetection(boolean enableChangeDetection) {
        this.enableChangeDetection = enableChangeDetection;
    }

    public Duration getChangeDetectionInitialDelay() {
        return changeDetectionInitialDelay;
    }

    public void setChangeDetectionInitialDelay(Duration changeDetectionInitialDelay) {
        this.changeDetectionInitialDelay = changeDetectionInitialDelay;
    }

    public Duration getChangeDetectionDelay() {
        return changeDetectionDelay;
    }

    public void setChangeDetectionDelay(Duration changeDetectionDelay) {
        this.changeDetectionDelay = changeDetectionDelay;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public FlowableServlet getServlet() {
        return servlet;
    }
}
