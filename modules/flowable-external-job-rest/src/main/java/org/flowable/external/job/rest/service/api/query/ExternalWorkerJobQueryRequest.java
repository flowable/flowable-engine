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

import org.flowable.common.rest.api.PaginateRequest;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiParam;

/**
 * @author Filip Hrisafov
 */
@ApiModel(description = "Request that is used for querying external worker jobs")
public class ExternalWorkerJobQueryRequest extends PaginateRequest {

    protected String id;
    protected String processInstanceId;
    protected boolean withoutProcessInstanceId;
    protected String executionId;
    protected String processDefinitionId;
    protected String scopeId;
    protected boolean withoutScopeId;
    protected String subScopeId;
    protected String scopeDefinitionId;
    protected String scopeType;
    protected String elementId;
    protected String elementName;
    protected boolean withException;
    protected String exceptionMessage;
    protected String tenantId;
    protected String tenantIdLike;
    protected boolean withoutTenantId;
    protected boolean locked;
    protected boolean unlocked;
    protected boolean withoutScopeType;

    public String getId() {
        return id;
    }

    @ApiParam("Only return job with the given id")
    public void setId(String id) {
        this.id = id;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    @ApiParam("Only return jobs with the processInstanceId")
    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public boolean isWithoutProcessInstanceId() {
        return withoutProcessInstanceId;
    }

    @ApiParam("Only return jobs without a process instance id")
    public void setWithoutProcessInstanceId(boolean withoutProcessInstanceId) {
        this.withoutProcessInstanceId = withoutProcessInstanceId;
    }

    public String getExecutionId() {
        return executionId;
    }

    @ApiParam("Only return jobs with the given executionId")
    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    @ApiParam("Only return jobs with the given processDefinitionId")
    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    public String getScopeId() {
        return scopeId;
    }

    @ApiParam("Only return jobs with the given scopeId")
    public void setScopeId(String scopeId) {
        this.scopeId = scopeId;
    }
    
    public boolean isWithoutScopeId() {
        return withoutScopeId;
    }

    @ApiParam("Only return jobs without a scope id")
    public void setWithoutScopeId(boolean withoutScopeId) {
        this.withoutScopeId = withoutScopeId;
    }

    public String getSubScopeId() {
        return subScopeId;
    }

    @ApiParam("Only return jobs with the given subScopeId")
    public void setSubScopeId(String subScopeId) {
        this.subScopeId = subScopeId;
    }

    public String getScopeDefinitionId() {
        return scopeDefinitionId;
    }

    @ApiParam("Only return jobs with the given scopeDefinitionId")
    public void setScopeDefinitionId(String scopeDefinitionId) {
        this.scopeDefinitionId = scopeDefinitionId;
    }

    @ApiParam("Only return jobs with the given scope type")
    public String getScopeType() {
        return scopeType;
    }

    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    public String getElementId() {
        return elementId;
    }

    @ApiParam("Only return jobs with the given elementId")
    public void setElementId(String elementId) {
        this.elementId = elementId;
    }

    public String getElementName() {
        return elementName;
    }

    @ApiParam("Only return jobs with the given elementName")
    public void setElementName(String elementName) {
        this.elementName = elementName;
    }

    public boolean isWithException() {
        return withException;
    }

    @ApiParam("Only return jobs with an exception")
    public void setWithException(boolean withException) {
        this.withException = withException;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    @ApiParam("Only return jobs with the given exception message")
    public void setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
    }

    public String getTenantId() {
        return tenantId;
    }

    @ApiParam("Only return jobs with the given tenant id")
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getTenantIdLike() {
        return tenantIdLike;
    }

    @ApiParam("Only return jobs with a tenantId like the given value")
    public void setTenantIdLike(String tenantIdLike) {
        this.tenantIdLike = tenantIdLike;
    }

    public boolean isWithoutTenantId() {
        return withoutTenantId;
    }

    @ApiParam("Only return jobs without a tenantId")
    public void setWithoutTenantId(boolean withoutTenantId) {
        this.withoutTenantId = withoutTenantId;
    }

    public boolean isLocked() {
        return locked;
    }

    @ApiParam("Only return jobs that are locked")
    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    @ApiParam("Only return jobs that are unlocked")
    public void setUnlocked(boolean unlocked) {
        this.unlocked = unlocked;
    }

    public boolean isWithoutScopeType() {
        return withoutScopeType;
    }

    @ApiParam("Only return jobs without a scope type")
    public void setWithoutScopeType(boolean withoutScopeType) {
        this.withoutScopeType = withoutScopeType;
    }

}
