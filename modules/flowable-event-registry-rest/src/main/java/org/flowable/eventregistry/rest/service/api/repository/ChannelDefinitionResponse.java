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

package org.flowable.eventregistry.rest.service.api.repository;

import java.util.Date;

import org.flowable.common.rest.util.DateToStringSerializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.swagger.annotations.ApiModelProperty;

/**
 * @author Tijs Rademakers
 */
public class ChannelDefinitionResponse {

    protected String id;
    protected String url;
    protected String key;
    protected int version;
    protected String name;
    protected String type;
    protected String implementation;
    @JsonSerialize(using = DateToStringSerializer.class, as = Date.class)
    protected Date createTime;
    protected String description;
    protected String tenantId;
    protected String deploymentId;
    protected String deploymentUrl;
    protected String resourceName;
    protected String resource;
    protected String category;

    @ApiModelProperty(example = "oneChannel:1:4")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @ApiModelProperty(example = "http://localhost:8182/event-registry-repository/channel-definitions/oneChannel%3A1%3A4")
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @ApiModelProperty(example = "oneChannel")
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @ApiModelProperty(example = "1")
    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @ApiModelProperty(example = "The One Channel")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty(example = "inbound", allowableValues = "inbound,outbound")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @ApiModelProperty(example = "kafka")
    public String getImplementation() {
        return implementation;
    }

    public void setImplementation(String implementation) {
        this.implementation = implementation;
    }

    @ApiModelProperty(example = "2010-10-13T14:54:26.750+02:00")
    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @ApiModelProperty(example = "null")
    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @ApiModelProperty(example = "2")
    public String getDeploymentId() {
        return deploymentId;
    }

    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

    @ApiModelProperty(example = "http://localhost:8182/event-registry-repository/deployments/2")
    public String getDeploymentUrl() {
        return deploymentUrl;
    }

    public void setDeploymentUrl(String deploymentUrl) {
        this.deploymentUrl = deploymentUrl;
    }

    @ApiModelProperty(example = "Examples")
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }
    
    @ApiModelProperty(example = "oneChannel.channel")
    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    @ApiModelProperty(example = "http://localhost:8182/event-registry-repository/deployments/2/resources/oneChannel.channel", value = "Contains the actual deployed channel definition JSON.")
    public String getResource() {
        return resource;
    }

    @ApiModelProperty(example = "This is a channel definition for testing purposes")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
