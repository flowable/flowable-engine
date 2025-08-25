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

package org.flowable.cmmn.rest.service.api.history.caze;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.flowable.cmmn.rest.service.api.engine.variable.RestVariable;
import org.flowable.common.rest.util.DateToStringSerializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.swagger.annotations.ApiModelProperty;

/**
 * @author Tijs Rademakers
 */
public class HistoricCaseInstanceResponse {

    protected String id;
    protected String url;
    protected String name;
    protected String businessKey;
    protected String businessStatus;
    protected String caseDefinitionId;
    protected String caseDefinitionUrl;
    protected String caseDefinitionName;
    protected String caseDefinitionDescription;
    @JsonSerialize(using = DateToStringSerializer.class, as = Date.class)
    protected Date startTime;
    @JsonSerialize(using = DateToStringSerializer.class, as = Date.class)
    protected Date endTime;
    protected String startUserId;
    protected String endUserId;
    protected String superProcessInstanceId;
    protected List<RestVariable> variables = new ArrayList<>();
    protected String tenantId;
    protected String state;
    protected String callbackId;
    protected String callbackType;
    protected String referenceId;
    protected String referenceType;

    @ApiModelProperty(example = "5")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @ApiModelProperty(example = "http://localhost:8182/cmmn-history/historic-case-instances/5")
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
    
    @ApiModelProperty(example = "myName")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty(example = "myKey")
    public String getBusinessKey() {
        return businessKey;
    }

    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }
    
    @ApiModelProperty(example = "myStatus")
    public String getBusinessStatus() {
        return businessStatus;
    }

    public void setBusinessStatus(String businessStatus) {
        this.businessStatus = businessStatus;
    }

    @ApiModelProperty(example = "oneTaskCase%3A1%3A4")
    public String getCaseDefinitionId() {
        return caseDefinitionId;
    }

    public void setCaseDefinitionId(String caseDefinitionId) {
        this.caseDefinitionId = caseDefinitionId;
    }

    @ApiModelProperty(example = "http://localhost:8182/cmmn-repository/case-definitions/oneTaskCaseProcess%3A1%3A4")
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

    @ApiModelProperty(example = "2013-04-17T10:17:43.902+0000")
    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    @ApiModelProperty(example = "2013-04-18T14:06:32.715+0000")
    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    @ApiModelProperty(example = "kermit")
    public String getStartUserId() {
        return startUserId;
    }

    public void setStartUserId(String startUserId) {
        this.startUserId = startUserId;
    }

    @ApiModelProperty(example = "kermit")
    public String getEndUserId() {
        return endUserId;
    }

    public void setEndUserId(String endUserId) {
        this.endUserId = endUserId;
    }

    @ApiModelProperty(example = "3")
    public String getSuperProcessInstanceId() {
        return superProcessInstanceId;
    }

    public void setSuperProcessInstanceId(String superProcessInstanceId) {
        this.superProcessInstanceId = superProcessInstanceId;
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

    @ApiModelProperty(example = "null")
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getTenantId() {
        return tenantId;
    }

    @ApiModelProperty(example = "active")
    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
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
}
