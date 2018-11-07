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

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.flowable.common.rest.api.PaginateRequest;
import org.flowable.rest.service.api.engine.variable.QueryVariable;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

/**
 * @author Frederik Heremans
 */
public class ProcessInstanceQueryRequest extends PaginateRequest {

    private String processInstanceId;
    private Set<String> processInstanceIds;
    private String processInstanceName;
    private String processInstanceNameLike;
    private String processInstanceNameLikeIgnoreCase;
    private String processBusinessKey;
    private String processDefinitionId;
    private Set<String> processDefinitionIds;
    private String processDefinitionKey;
    private Set<String> processDefinitionKeys;
    private String processDefinitionName;
    private String processDefinitionCategory;
    private Integer processDefinitionVersion;
    private String processDefinitionEngineVersion;
    private String deploymentId;
    private List<String> deploymentIdIn;
    private String superProcessInstanceId;
    private String subProcessInstanceId;
    private Boolean excludeSubprocesses;
    private String involvedUser;
    private String startedBy;
    private Date startedBefore;
    private Date startedAfter;
    private Boolean suspended;
    private Boolean includeProcessVariables;
    private List<QueryVariable> variables;
    private String callbackId;
    private String callbackType;
    private String tenantId;
    private String tenantIdLike;
    private Boolean withoutTenantId;

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public Set<String> getProcessInstanceIds() {
        return processInstanceIds;
    }

    public void setProcessInstanceIds(Set<String> processInstanceIds) {
        this.processInstanceIds = processInstanceIds;
    }

    public String getProcessInstanceName() {
        return processInstanceName;
    }

    public void setProcessInstanceName(String processInstanceName) {
        this.processInstanceName = processInstanceName;
    }

    public String getProcessInstanceNameLike() {
        return processInstanceNameLike;
    }

    public void setProcessInstanceNameLike(String processInstanceNameLike) {
        this.processInstanceNameLike = processInstanceNameLike;
    }

    public String getProcessInstanceNameLikeIgnoreCase() {
        return processInstanceNameLikeIgnoreCase;
    }

    public void setProcessInstanceNameLikeIgnoreCase(String processInstanceNameLikeIgnoreCase) {
        this.processInstanceNameLikeIgnoreCase = processInstanceNameLikeIgnoreCase;
    }

    public String getProcessBusinessKey() {
        return processBusinessKey;
    }

    public void setProcessBusinessKey(String processBusinessKey) {
        this.processBusinessKey = processBusinessKey;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    public Set<String> getProcessDefinitionIds() {
        return processDefinitionIds;
    }

    public void setProcessDefinitionIds(Set<String> processDefinitionIds) {
        this.processDefinitionIds = processDefinitionIds;
    }

    public String getProcessDefinitionKey() {
        return processDefinitionKey;
    }

    public void setProcessDefinitionKey(String processDefinitionKey) {
        this.processDefinitionKey = processDefinitionKey;
    }
    
    public Set<String> getProcessDefinitionKeys() {
        return processDefinitionKeys;
    }

    public void setProcessDefinitionKeys(Set<String> processDefinitionKeys) {
        this.processDefinitionKeys = processDefinitionKeys;
    }

    public String getProcessDefinitionName() {
        return processDefinitionName;
    }

    public void setProcessDefinitionName(String processDefinitionName) {
        this.processDefinitionName = processDefinitionName;
    }

    public String getProcessDefinitionCategory() {
        return processDefinitionCategory;
    }

    public void setProcessDefinitionCategory(String processDefinitionCategory) {
        this.processDefinitionCategory = processDefinitionCategory;
    }

    public Integer getProcessDefinitionVersion() {
        return processDefinitionVersion;
    }

    public void setProcessDefinitionVersion(Integer processDefinitionVersion) {
        this.processDefinitionVersion = processDefinitionVersion;
    }

    public String getProcessDefinitionEngineVersion() {
        return processDefinitionEngineVersion;
    }

    public void setProcessDefinitionEngineVersion(String processDefinitionEngineVersion) {
        this.processDefinitionEngineVersion = processDefinitionEngineVersion;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

    public List<String> getDeploymentIdIn() {
        return deploymentIdIn;
    }

    public void setDeploymentIdIn(List<String> deploymentIdIn) {
        this.deploymentIdIn = deploymentIdIn;
    }

    public String getSuperProcessInstanceId() {
        return superProcessInstanceId;
    }

    public void setSuperProcessInstanceId(String superProcessInstanceId) {
        this.superProcessInstanceId = superProcessInstanceId;
    }

    public String getSubProcessInstanceId() {
        return subProcessInstanceId;
    }

    public void setSubProcessInstanceId(String subProcessInstanceId) {
        this.subProcessInstanceId = subProcessInstanceId;
    }

    public Boolean getExcludeSubprocesses() {
        return excludeSubprocesses;
    }

    public void setExcludeSubprocesses(Boolean excludeSubprocesses) {
        this.excludeSubprocesses = excludeSubprocesses;
    }

    public String getStartedBy() {
        return startedBy;
    }

    public void setStartedBy(String startedBy) {
        this.startedBy = startedBy;
    }

    public Date getStartedBefore() {
        return startedBefore;
    }

    public void setStartedBefore(Date startedBefore) {
        this.startedBefore = startedBefore;
    }

    public Date getStartedAfter() {
        return startedAfter;
    }

    public void setStartedAfter(Date startedAfter) {
        this.startedAfter = startedAfter;
    }

    public String getInvolvedUser() {
        return involvedUser;
    }

    public void setInvolvedUser(String involvedUser) {
        this.involvedUser = involvedUser;
    }

    public Boolean getSuspended() {
        return suspended;
    }

    public void setSuspended(Boolean suspended) {
        this.suspended = suspended;
    }

    public Boolean getIncludeProcessVariables() {
        return includeProcessVariables;
    }

    public void setIncludeProcessVariables(Boolean includeProcessVariables) {
        this.includeProcessVariables = includeProcessVariables;
    }

    @JsonTypeInfo(use = Id.CLASS, defaultImpl = QueryVariable.class)
    public List<QueryVariable> getVariables() {
        return variables;
    }

    public void setVariables(List<QueryVariable> variables) {
        this.variables = variables;
    }

    public String getCallbackId() {
        return callbackId;
    }

    public void setCallbackId(String callbackId) {
        this.callbackId = callbackId;
    }

    public String getCallbackType() {
        return callbackType;
    }

    public void setCallbackType(String callbackType) {
        this.callbackType = callbackType;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setWithoutTenantId(Boolean withoutTenantId) {
        this.withoutTenantId = withoutTenantId;
    }

    public Boolean getWithoutTenantId() {
        return withoutTenantId;
    }

    public String getTenantIdLike() {
        return tenantIdLike;
    }

    public void setTenantIdLike(String tenantIdLike) {
        this.tenantIdLike = tenantIdLike;
    }
}
