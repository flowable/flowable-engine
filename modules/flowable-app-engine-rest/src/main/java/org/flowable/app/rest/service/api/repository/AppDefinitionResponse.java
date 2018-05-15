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
package org.flowable.app.rest.service.api.repository;

import org.flowable.app.api.repository.AppDefinition;

import io.swagger.annotations.ApiModelProperty;

/**
 * @author Tijs Rademakers
 */
public class AppDefinitionResponse {

    protected String id;
    protected String url;
    protected String category;
    protected String name;
    protected String key;
    protected String description;
    protected int version;
    protected String resourceName;
    protected String deploymentId;
    protected String tenantId;

    public AppDefinitionResponse(AppDefinition appDefinition) {
        setId(appDefinition.getId());
        setCategory(appDefinition.getCategory());
        setName(appDefinition.getName());
        setKey(appDefinition.getKey());
        setDescription(appDefinition.getDescription());
        setVersion(appDefinition.getVersion());
        setResourceName(appDefinition.getResourceName());
        setDeploymentId(appDefinition.getDeploymentId());
        setTenantId(appDefinition.getTenantId());
    }

    @ApiModelProperty(example = "10")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @ApiModelProperty(example = "http://localhost:8182/app-repository/app-definitions/simpleApp")
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @ApiModelProperty(example = "Examples")
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @ApiModelProperty(example = "Simple App")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty(example = "simpleApp")
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @ApiModelProperty(example = "This is an app for testing purposes")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @ApiModelProperty(example = "1")
    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @ApiModelProperty(example = "SimpleSourceName")
    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    @ApiModelProperty(example = "818e4703-f1d2-11e6-8549-acde48001121")
    public String getDeploymentId() {
        return deploymentId;
    }

    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

    @ApiModelProperty(example = "null")
    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
}
