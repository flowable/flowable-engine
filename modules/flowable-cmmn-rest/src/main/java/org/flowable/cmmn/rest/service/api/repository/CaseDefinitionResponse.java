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

package org.flowable.cmmn.rest.service.api.repository;

import io.swagger.annotations.ApiModelProperty;

/**
 * @author Tijs Rademakers
 */
public class CaseDefinitionResponse {

    private String id;
    private String url;
    private String key;
    private int version;
    private String name;
    private String description;
    private String tenantId;
    private String deploymentId;
    private String deploymentUrl;
    private String resource;
    private String diagramResource;
    private String category;
    private boolean graphicalNotationDefined;
    private boolean startFormDefined;

    @ApiModelProperty(example = "oneTaskCase:1:4")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @ApiModelProperty(example = "http://localhost:8182/cmmn-repository/case-definitions/oneTaskCase%3A1%3A4")
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @ApiModelProperty(example = "oneTaskCase")
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

    @ApiModelProperty(example = "The One Task Case")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    @ApiModelProperty(example = "http://localhost:8081/cmmn-repository/deployments/2")
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

    @ApiModelProperty(example = "http://localhost:8182/cmmn-repository/deployments/2/resources/testCase.cmmn", value = "Contains the actual deployed CMMN 1.1 xml.")
    public String getResource() {
        return resource;
    }

    @ApiModelProperty(example = "This is a case for testing purposes")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDiagramResource(String diagramResource) {
        this.diagramResource = diagramResource;
    }

    @ApiModelProperty(example = "http://localhost:8182/cmmn-repository/deployments/2/resources/testProcess.png", value = "Contains a graphical representation of the case, null when no diagram is available.")
    public String getDiagramResource() {
        return diagramResource;
    }

    public void setGraphicalNotationDefined(boolean graphicalNotationDefined) {
        this.graphicalNotationDefined = graphicalNotationDefined;
    }

    @ApiModelProperty(value = "Indicates the case definition contains graphical information (CMMN DI).")
    public boolean isGraphicalNotationDefined() {
        return graphicalNotationDefined;
    }

    public void setStartFormDefined(boolean startFormDefined) {
        this.startFormDefined = startFormDefined;
    }

    public boolean isStartFormDefined() {
        return startFormDefined;
    }
}
