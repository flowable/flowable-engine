package org.flowable.spring.boot;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.List;

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
    private String deploymentName;

    /**
     * The location where the form resources are located.
     * Default is {@code classpath*:/forms/}
     */
    private String resourceLocation = "classpath*:/forms/";

    /**
     * The suffixes for the resources that need to be scanned.
     * Default is {@code **.json}
     */
    private List<String> resourceSuffixes = Collections.singletonList("**.form");

    /**
     * Whether to perform deployment of resources, default is true.
     */
    private boolean deployResources = true;

    /**
     * Whether the form engine needs to be started.
     */
    private boolean enable = true;

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

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }
}
