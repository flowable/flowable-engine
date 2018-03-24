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

import java.util.ArrayList;
import java.util.List;

import org.flowable.cmmn.rest.service.api.engine.variable.RestVariable;

import io.swagger.annotations.ApiModelProperty;

/**
 * Modified to add a "completed" flag, which lets the caller know if the case instance has run to completion without encountering a wait state or experiencing an error/ exception.
 * 
 * @author Tijs Rademakers
 */
public class CaseInstanceResponse {
    
    protected String id;
    protected String name;
    protected String url;
    protected String businessKey;
    protected boolean ended;
    protected String caseDefinitionId;
    protected String caseDefinitionUrl;
    protected List<RestVariable> variables = new ArrayList<>();
    protected String tenantId;
    protected boolean completed;

    @ApiModelProperty(example = "187")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    
    @ApiModelProperty(example = "processName")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty(example = "http://localhost:8182/cmmn-repository/case-definitions/caseOne%3A1%3A4")
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @ApiModelProperty(example = "myBusinessKey")
    public String getBusinessKey() {
        return businessKey;
    }

    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }

    public boolean isEnded() {
        return ended;
    }

    public void setEnded(boolean ended) {
        this.ended = ended;
    }

    @ApiModelProperty(example = "oneTaskCase:1:158")
    public String getCaseDefinitionId() {
        return caseDefinitionId;
    }

    public void setCaseDefinitionId(String caseDefinitionId) {
        this.caseDefinitionId = caseDefinitionId;
    }

    @ApiModelProperty(example = "http://localhost:8182/cmmn-repository/case-definitions/caseOne%3A1%3A4")
    public String getCaseDefinitionUrl() {
        return caseDefinitionUrl;
    }

    public void setCaseDefinitionUrl(String caseDefinitionUrl) {
        this.caseDefinitionUrl = caseDefinitionUrl;
    }

    public List<RestVariable> getVariables() {
        return variables;
    }

    public void setVariables(List<RestVariable> variables) {
        this.variables = variables;
    }

    public void addVariable(RestVariable variable) {
        variables.add(variable);
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @ApiModelProperty(example = "null")
    public String getTenantId() {
        return tenantId;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
