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

package org.flowable.external.job.rest.service.api.query;

import java.util.Date;

import org.flowable.common.rest.util.DateToStringIso8601Serializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.swagger.annotations.ApiModelProperty;

/**
 * @author Filip Hrisafov
 */
public class ExternalWorkerJobResponse {

    @ApiModelProperty(value = "The id of the external job", example = "8", required = true)
    protected String id;

    @ApiModelProperty(value = "The url of the external job", example = "http://localhost:8182/external-job-api/jobs/8", required = true)
    protected String url;

    @ApiModelProperty(value = "The correlation id of the external job", example = "50")
    protected String correlationId;

    @ApiModelProperty(value = "The process instance id for the external job", example = "5")
    protected String processInstanceId;

    @ApiModelProperty(value = "The process definition id for the external job", example = "customerProcess:1:4")
    protected String processDefinitionId;

    @ApiModelProperty(value = "The execution id for the external job", example = "7")
    protected String executionId;

    @ApiModelProperty(value = "The scope id for the external job", example = "20")
    protected String scopeId;

    @ApiModelProperty(value = "The sub scope id for the external job", example = "21")
    protected String subScopeId;

    @ApiModelProperty(value = "The scope definition id for the external job", example = "customerCase:1:39")
    protected String scopeDefinitionId;

    @ApiModelProperty(value = "The scope type for the external job", example = "cmmn")
    protected String scopeType;

    @ApiModelProperty(value = "The id of the element in the model", example = "customer")
    protected String elementId;

    @ApiModelProperty(value = "The name of the element in the model", example = "Process Customer Task")
    protected String elementName;

    @ApiModelProperty(value = "The remaining number of retries", example = "3")
    protected Integer retries;

    @ApiModelProperty(value = "The exception message for the job", example = "null")
    protected String exceptionMessage;

    @ApiModelProperty(value = "The due date for the job", example = "2021-05-04T16:35:10.474Z")
    @JsonSerialize(using = DateToStringIso8601Serializer.class, as = Date.class)
    protected Date dueDate;

    @ApiModelProperty(value = "The creation time of the job", example = "2020-05-04T16:35:10.474Z")
    @JsonSerialize(using = DateToStringIso8601Serializer.class, as = Date.class)
    protected Date createTime;

    @ApiModelProperty(value = "The tenant if of the job", example = "flowable")
    protected String tenantId;

    @ApiModelProperty(value = "The id of the lock owner. If not set then the job is not locked", example = "worker1")
    protected String lockOwner;

    @ApiModelProperty(value = "The time when the lock expires", example = "2020-05-04T16:35:10.474Z")
    @JsonSerialize(using = DateToStringIso8601Serializer.class, as = Date.class)
    protected Date lockExpirationTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public String getScopeId() {
        return scopeId;
    }

    public void setScopeId(String scopeId) {
        this.scopeId = scopeId;
    }

    public String getSubScopeId() {
        return subScopeId;
    }

    public void setSubScopeId(String subScopeId) {
        this.subScopeId = subScopeId;
    }

    public String getScopeDefinitionId() {
        return scopeDefinitionId;
    }

    public void setScopeDefinitionId(String scopeDefinitionId) {
        this.scopeDefinitionId = scopeDefinitionId;
    }

    public String getScopeType() {
        return scopeType;
    }

    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    public String getElementId() {
        return elementId;
    }

    public void setElementId(String elementId) {
        this.elementId = elementId;
    }

    public String getElementName() {
        return elementName;
    }

    public void setElementName(String elementName) {
        this.elementName = elementName;
    }

    public Integer getRetries() {
        return retries;
    }

    public void setRetries(Integer retries) {
        this.retries = retries;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public void setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getLockOwner() {
        return lockOwner;
    }

    public void setLockOwner(String lockOwner) {
        this.lockOwner = lockOwner;
    }

    public Date getLockExpirationTime() {
        return lockExpirationTime;
    }

    public void setLockExpirationTime(Date lockExpirationTime) {
        this.lockExpirationTime = lockExpirationTime;
    }
}
