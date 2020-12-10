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

package org.flowable.cmmn.rest.service.api.management;

import java.util.Date;

import org.flowable.common.rest.util.DateToStringSerializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.swagger.annotations.ApiModelProperty;

/**
 * @author Frederik Heremans
 */
public class JobResponse {

    protected String id;
    protected String url;
    protected String caseInstanceId;
    protected String caseInstanceUrl;
    protected String caseDefinitionId;
    protected String caseDefinitionUrl;
    protected String planItemInstanceId;
    protected String elementId;
    protected String elementName;
    protected Integer retries;
    protected String exceptionMessage;
    @JsonSerialize(using = DateToStringSerializer.class, as = Date.class)
    protected Date dueDate;
    protected Date createTime;
    protected String tenantId;

    @ApiModelProperty(example = "8")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @ApiModelProperty(example = "http://localhost:8182/management/jobs/8")
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @ApiModelProperty(example = "5")
    public String getCaseInstanceId() {
        return caseInstanceId;
    }

    public void setCaseInstanceId(String caseInstanceId) {
        this.caseInstanceId = caseInstanceId;
    }

    @ApiModelProperty(example = "http://localhost:8182/cmmn-runtime/case-instances/5")
    public String getCaseInstanceUrl() {
        return caseInstanceUrl;
    }

    public void setCaseInstanceUrl(String caseInstanceUrl) {
        this.caseInstanceUrl = caseInstanceUrl;
    }

    @ApiModelProperty(example = "timerCase:1:4")
    public String getCaseDefinitionId() {
        return caseDefinitionId;
    }

    public void setCaseDefinitionId(String caseDefinitionId) {
        this.caseDefinitionId = caseDefinitionId;
    }

    @ApiModelProperty(example = "http://localhost:8182/cmmn-repository/case-definitions/timerCase%3A1%3A4")
    public String getCaseDefinitionUrl() {
        return caseDefinitionUrl;
    }

    public void setCaseDefinitionUrl(String caseDefinitionUrl) {
        this.caseDefinitionUrl = caseDefinitionUrl;
    }

    @ApiModelProperty(example = "7")
    public String getPlanItemInstanceId() {
        return planItemInstanceId;
    }

    public void setPlanItemInstanceId(String planItemInstanceId) {
        this.planItemInstanceId = planItemInstanceId;
    }

    @ApiModelProperty(example = "scriptTask1")
    public String getElementId() {
        return elementId;
    }

    public void setElementId(String elementId) {
        this.elementId = elementId;
    }

    @ApiModelProperty(example = "Script task")
    public String getElementName() {
        return elementName;
    }

    public void setElementName(String elementName) {
        this.elementName = elementName;
    }

    @ApiModelProperty(example = "3")
    public Integer getRetries() {
        return retries;
    }

    public void setRetries(Integer retries) {
        this.retries = retries;
    }

    @ApiModelProperty(example = "null")
    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public void setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
    }

    @ApiModelProperty(example = "2013-06-04T22:05:05.474+0000")
    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    @ApiModelProperty(example = "2013-06-03T22:05:05.474+0000")
    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @ApiModelProperty(example = "null")
    public String getTenantId() {
        return tenantId;
    }
}
