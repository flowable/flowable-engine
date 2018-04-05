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

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModelProperty;

import org.flowable.common.rest.util.DateToStringSerializer;
import org.flowable.dmn.api.DmnDeployment;

import java.util.Date;

/**
 * @author Yvo Swillens
 */
public class DmnDeploymentResponse {

    protected String id;
    protected String name;
    @JsonSerialize(using = DateToStringSerializer.class, as = Date.class)
    protected Date deploymentTime;
    protected String category;
    protected String url;
    protected String parentDeploymentId;
    protected String tenantId;

    public DmnDeploymentResponse(DmnDeployment deployment, String url) {
        setId(deployment.getId());
        setName(deployment.getName());
        setDeploymentTime(deployment.getDeploymentTime());
        setCategory(deployment.getCategory());
        setParentDeploymentId(deployment.getParentDeploymentId());
        setTenantId(deployment.getTenantId());
        setUrl(url);
    }

    @ApiModelProperty(example = "03ab310d-c1de-11e6-a4f4-62ce84ef239e")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @ApiModelProperty(example = "dmnTest")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty(example = "2016-12-14T10:16:37.000+01:00")
    public Date getDeploymentTime() {
        return deploymentTime;
    }

    public void setDeploymentTime(Date deploymentTime) {
        this.deploymentTime = deploymentTime;
    }

    @ApiModelProperty(example = "dmnExamples")
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @ApiModelProperty(example = "http://localhost:8080/flowable-rest/dmn-api/dmn-repository/deployments/03ab310d-c1de-11e6-a4f4-62ce84ef239e")
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @ApiModelProperty(example = "17510")
    public String getParentDeploymentId() {
        return parentDeploymentId;
    }

    public void setParentDeploymentId(String parentDeploymentId) {
        this.parentDeploymentId = parentDeploymentId;
    }

    @ApiModelProperty(example = "null")
    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
}
