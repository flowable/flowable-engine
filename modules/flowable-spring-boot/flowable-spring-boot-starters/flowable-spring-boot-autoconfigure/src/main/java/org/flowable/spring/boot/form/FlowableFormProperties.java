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
package org.flowable.spring.boot.form;

import java.util.Collections;
import java.util.List;

import org.flowable.spring.boot.FlowableServlet;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Properties for configuring the form engine.
 *
 * @author Filip Hrisafov
 * @author Javier Casal
 */
@ConfigurationProperties(prefix = "flowable.form")
public class FlowableFormProperties {

    /**
     * The name of the deployment for the form resources.
     */
    private String deploymentName = "SpringBootAutoDeployment";

    /**
     * The location where the form resources are located.
     * Default is {@code classpath*:/forms/}
     */
    private String resourceLocation = "classpath*:/forms/";

    /**
     * The suffixes for the resources that need to be scanned.
     * Default is {@code **.form}
     */
    private List<String> resourceSuffixes = Collections.singletonList("**.form");

    /**
     * Whether to perform deployment of resources, default is true.
     */
    private boolean deployResources = true;

    /**
     * Whether the form engine needs to be started.
     */
    private boolean enabled = true;

    /**
     * The servlet configuration for the Form Rest API.
     */
    @NestedConfigurationProperty
    private final FlowableServlet servlet = new FlowableServlet("/form-api", "Flowable Form Rest API");

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
