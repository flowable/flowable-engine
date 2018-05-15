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
package org.flowable.dmn.rest.service.api.repository;

import org.flowable.dmn.api.DmnDecisionTable;

import io.swagger.annotations.ApiModelProperty;

/**
 * @author Yvo Swillens
 */
public class DecisionTableResponse {

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

    public DecisionTableResponse(DmnDecisionTable decisionTable) {
        setId(decisionTable.getId());
        setCategory(decisionTable.getCategory());
        setName(decisionTable.getName());
        setKey(decisionTable.getKey());
        setDescription(decisionTable.getDescription());
        setVersion(decisionTable.getVersion());
        setResourceName(decisionTable.getResourceName());
        setDeploymentId(decisionTable.getDeploymentId());
        setTenantId(decisionTable.getTenantId());
    }

    @ApiModelProperty(example = "46b0379c-c0a1-11e6-bc93-6ab56fad108a")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @ApiModelProperty(example = "http://localhost:8080/flowable-rest/dmn-api/dmn-repository/decision-tables/46b0379c-c0a1-11e6-bc93-6ab56fad108a")
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @ApiModelProperty(example = "dmnTest")
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @ApiModelProperty(example = "Decision Table One")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty(example = "DecisionTableOne")
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @ApiModelProperty(example = "This is a simple description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @ApiModelProperty(example = "3")
    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @ApiModelProperty(example = "dmn-DecisionTableOne.dmn")
    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    @ApiModelProperty(example = "46aa6b3a-c0a1-11e6-bc93-6ab56fad108a")
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
