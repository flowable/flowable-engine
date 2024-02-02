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
import java.util.Date;
import java.util.List;

import org.flowable.cmmn.rest.service.api.engine.variable.RestVariable;
import org.flowable.common.rest.util.DateToStringSerializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

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
    protected String businessStatus;
    @JsonSerialize(using = DateToStringSerializer.class, as = Date.class)
    protected Date startTime;
    protected String startUserId;
    protected String state;
    protected boolean ended;
    protected String caseDefinitionId;
    protected String caseDefinitionUrl;
    protected String caseDefinitionName;
    protected String caseDefinitionDescription;
    protected String parentId;
    protected String callbackId;
    protected String callbackType;
    protected String referenceId;
    protected String referenceType;
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
    
    @ApiModelProperty(example = "myBusinessStatus")
    public String getBusinessStatus() {
        return businessStatus;
    }

    public void setBusinessStatus(String businessStatus) {
        this.businessStatus = businessStatus;
    }

    @ApiModelProperty(example = "2019-04-17T10:17:43.902+0000")
    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    @ApiModelProperty(example = "aUserId")
    public String getStartUserId() {
        return startUserId;
    }

    public void setStartUserId(String startUserId) {
        this.startUserId = startUserId;
    }
    
    @ApiModelProperty(example = "active")
    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
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
    
    @ApiModelProperty(example = "aCaseDefinitionName")
    public String getCaseDefinitionName() {
        return caseDefinitionName;
    }

    public void setCaseDefinitionName(String caseDefinitionName) {
        this.caseDefinitionName = caseDefinitionName;
    }

    @ApiModelProperty(example = "A case definition description")
    public String getCaseDefinitionDescription() {
        return caseDefinitionDescription;
    }

    public void setCaseDefinitionDescription(String caseDefinitionDescription) {
        this.caseDefinitionDescription = caseDefinitionDescription;
    }
    
    @ApiModelProperty(example = "123")
    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    @ApiModelProperty(example = "123")
    public String getCallbackId() {
        return callbackId;
    }

    public void setCallbackId(String callbackId) {
        this.callbackId = callbackId;
    }

    @ApiModelProperty(example = "cmmn-1.1-to-cmmn-1.1-child-case")
    public String getCallbackType() {
        return callbackType;
    }

    public void setCallbackType(String callbackType) {
        this.callbackType = callbackType;
    }

    @ApiModelProperty(example = "123")
    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    @ApiModelProperty(example = "event-to-cmmn-1.1-case")
    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
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
