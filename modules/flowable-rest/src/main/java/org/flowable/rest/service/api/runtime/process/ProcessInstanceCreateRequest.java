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

package org.flowable.rest.service.api.runtime.process;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.StringUtils;
import org.flowable.rest.service.api.engine.variable.RestVariable;

import java.util.List;

/**
 * Modified to add a "returnVariables" flag, which determines whether the variables that exist within the process instance when the first wait state is encountered (or when the process instance
 * completes) should be returned or not.
 * 
 * @author Frederik Heremans
 * @author Ryan Johnston (@rjfsu)
 * @author Joram Barrez
 */
@ApiModel(description = "Only one of processDefinitionId, processDefinitionKey or message can be used in the request body")
public class ProcessInstanceCreateRequest {

    protected String processDefinitionId;
    protected String processDefinitionKey;
    protected String message;
    protected String name;
    protected String businessKey;
    protected List<RestVariable> variables;
    protected List<RestVariable> transientVariables;
    protected String tenantId;
    protected String overrideDefinitionTenantId;

    // Added by Ryan Johnston
    private boolean returnVariables;

    @ApiModelProperty(example = "oneTaskProcess:1:158")
    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    @ApiModelProperty(example = "oneTaskProcess")
    public String getProcessDefinitionKey() {
        return processDefinitionKey;
    }

    public void setProcessDefinitionKey(String processDefinitionKey) {
        this.processDefinitionKey = processDefinitionKey;
    }

    @ApiModelProperty(example = "myProcessInstanceName")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty(example = "myBusinessKey")
    public String getBusinessKey() {
        return businessKey;
    }

    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }

    @ApiModelProperty(example = "newOrderMessage")
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @ApiModelProperty(example = "tenant1")
    public String getTenantId() {
        return tenantId;
    }
    
    @ApiModelProperty(example = "overrideTenant1")
    public String getOverrideDefinitionTenantId() {
        return overrideDefinitionTenantId;
    }

    public void setOverrideDefinitionTenantId(String overrideDefinitionTenantId) {
        this.overrideDefinitionTenantId = overrideDefinitionTenantId;
    }

    @JsonTypeInfo(use = Id.CLASS, defaultImpl = RestVariable.class)
    public List<RestVariable> getVariables() {
        return variables;
    }

    public void setVariables(List<RestVariable> variables) {
        this.variables = variables;
    }

    @JsonTypeInfo(use = Id.CLASS, defaultImpl = RestVariable.class)
    public List<RestVariable> getTransientVariables() {
        return transientVariables;
    }

    public void setTransientVariables(List<RestVariable> transientVariables) {
        this.transientVariables = transientVariables;
    }

    @JsonIgnore
    public boolean isTenantSet() {
        return tenantId != null && !StringUtils.isEmpty(tenantId);
    }

    // Added by Ryan Johnston
    public boolean getReturnVariables() {
        return returnVariables;
    }

    // Added by Ryan Johnston
    public void setReturnVariables(boolean returnVariables) {
        this.returnVariables = returnVariables;
    }
}
