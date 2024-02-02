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

package org.flowable.cmmn.rest.service.api.runtime.caze;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.rest.service.api.engine.variable.RestVariable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Modified to add a "returnVariables" flag, which determines whether the variables that exist within the process instance when the first wait state is encountered (or when the case instance
 * completes) should be returned or not.
 * 
 * @author Tijs Rademakers
 */
@ApiModel(description = "Only one of caseDefinitionId or caseDefinitionKey can be used in the request body")
public class CaseInstanceCreateRequest {

    protected String caseDefinitionId;
    protected String caseDefinitionKey;
    protected String name;
    protected String businessKey;
    protected List<RestVariable> variables;
    protected List<RestVariable> transientVariables;
    protected List<RestVariable> startFormVariables;
    protected String outcome;
    protected String tenantId;
    protected String overrideDefinitionTenantId;

    protected boolean returnVariables;

    @ApiModelProperty(example = "oneTaskCase:1:158")
    public String getCaseDefinitionId() {
        return caseDefinitionId;
    }

    public void setCaseDefinitionId(String caseDefinitionId) {
        this.caseDefinitionId = caseDefinitionId;
    }

    @ApiModelProperty(example = "oneTaskCase")
    public String getCaseDefinitionKey() {
        return caseDefinitionKey;
    }

    public void setCaseDefinitionKey(String caseDefinitionKey) {
        this.caseDefinitionKey = caseDefinitionKey;
    }

    @ApiModelProperty(example = "My case name")
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

    @JsonTypeInfo(use = Id.CLASS, defaultImpl = RestVariable.class)
    public List<RestVariable> getStartFormVariables() {
        return startFormVariables;
    }

    public void setStartFormVariables(List<RestVariable> startFormVariables) {
        this.startFormVariables = startFormVariables;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    @JsonIgnore
    public boolean isTenantSet() {
        return tenantId != null && !StringUtils.isEmpty(tenantId);
    }

    public boolean getReturnVariables() {
        return returnVariables;
    }

    public void setReturnVariables(boolean returnVariables) {
        this.returnVariables = returnVariables;
    }
}
