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
package org.flowable.spring.boot.cmmn;

import java.util.Arrays;
import java.util.List;

import org.flowable.spring.boot.FlowableServlet;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Properties for configuring the CMMN engine.
 *
 * @author Filip Hrisafov
 */
@ConfigurationProperties(prefix = "flowable.cmmn")
public class FlowableCmmnProperties {

    /**
     * The name of the deployment for the CMMN resources.
     */
    private String deploymentName = "SpringBootAutoDeployment";

    /**
     * The location where the CMMN resources are located.
     * Default is {@code classpath*:/cases/}
     */
    private String resourceLocation = "classpath*:/cases/";

    /**
     * The suffixes for the resources that need to be scanned.
     * Default is {@code **.cmmn, **.cmmn11, **.cmmn.xml, **.cmmn11.xml}
     */
    private List<String> resourceSuffixes = Arrays.asList("**.cmmn", "**.cmmn11", "**.cmmn.xml", "**.cmmn11.xml");

    /**
     * Whether to perform deployment of resources, default is {@code true}.
     */
    private boolean deployResources = true;

    /**
     * Whether the CMMN engine needs to be started.
     */
    private boolean enabled = true;

    /**
     * Enables extra checks on the DMN xml that is parsed. See https://www.flowable.org/docs/userguide/index.html#advanced.safe.bpmn.xml
     * Unfortunately, this feature is not available on some platforms (JDK 6, JBoss), hence you need to disable if your platform does not allow the use of
     * StaxSource during XML parsing.
     */
    private boolean enableSafeXml = true;

    /**
     * The servlet configuration for the CMMN Rest API.
     */
    @NestedConfigurationProperty
    private final FlowableServlet servlet = new FlowableServlet("/cmmn-api", "Flowable CMMN Rest API");

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

    public boolean isEnableSafeXml() {
        return enableSafeXml;
    }

    public void setEnableSafeXml(boolean enableSafeXml) {
        this.enableSafeXml = enableSafeXml;
    }

    public FlowableServlet getServlet() {
        return servlet;
    }
}
