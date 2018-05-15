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

import java.util.Date;

import org.flowable.app.api.repository.AppDeployment;
import org.flowable.common.rest.util.DateToStringSerializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.swagger.annotations.ApiModelProperty;

/**
 * @author Tijs Rademakers
 */
public class AppDeploymentResponse {

    protected String id;
    protected String name;
    @JsonSerialize(using = DateToStringSerializer.class, as = Date.class)
    protected Date deploymentTime;
    protected String category;
    protected String url;
    protected String tenantId;

    public AppDeploymentResponse(AppDeployment deployment, String url) {
        setId(deployment.getId());
        setName(deployment.getName());
        setDeploymentTime(deployment.getDeploymentTime());
        setCategory(deployment.getCategory());
        setTenantId(deployment.getTenantId());
        setUrl(url);
    }

    @ApiModelProperty(example = "10")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @ApiModelProperty(example = "flowable-app-examples")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty(example = "2010-10-13T14:54:26.750+02:00")
    public Date getDeploymentTime() {
        return deploymentTime;
    }

    public void setDeploymentTime(Date deploymentTime) {
        this.deploymentTime = deploymentTime;
    }

    @ApiModelProperty(example = "examples")
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @ApiModelProperty(example = "http://localhost:8081/app-api/app-repository/deployments/10")
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @ApiModelProperty(example = "null")
    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
}
