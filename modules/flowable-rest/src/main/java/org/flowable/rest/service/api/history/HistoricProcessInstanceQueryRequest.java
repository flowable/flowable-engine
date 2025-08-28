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

package org.flowable.rest.service.api.history;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.flowable.common.rest.api.PaginateRequest;
import org.flowable.rest.service.api.engine.variable.QueryVariable;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

/**
 * @author Tijs Rademakers
 */
public class HistoricProcessInstanceQueryRequest extends PaginateRequest {

    private String processInstanceId;
    private List<String> processInstanceIds;
    private String processInstanceName;
    private String processInstanceNameLike;
    private String processInstanceNameLikeIgnoreCase;
    private String processBusinessKey;
    private String processBusinessKeyLike;
    private String processBusinessKeyLikeIgnoreCase;
    private String processBusinessStatus;
    private String processBusinessStatusLike;
    private String processBusinessStatusLikeIgnoreCase;
    private String processDefinitionId;
    private String processDefinitionKey;
    private String processDefinitionKeyLike;
    private String processDefinitionKeyLikeIgnoreCase;
    private List<String> processDefinitionKeys;
    private List<String> excludeProcessDefinitionKeys;
    private List<String> processDefinitionKeyIn;
    private List<String> processDefinitionKeyNotIn;
    private String processDefinitionName;
    private String processDefinitionNameLike;
    private String processDefinitionNameLikeIgnoreCase;
    private Integer processDefinitionVersion;
    private String processDefinitionCategory;
    private String processDefinitionCategoryLike;
    private String processDefinitionCategoryLikeIgnoreCase;
    private String deploymentId;
    private List<String> deploymentIdIn;
    private String superProcessInstanceId;
    private Boolean excludeSubprocesses;
    private Boolean finished;
    private String activeActivityId;
    private Set<String> activeActivityIds;
    private String involvedUser;
    private Date finishedAfter;
    private Date finishedBefore;
    private Date startedAfter;
    private Date startedBefore;
    private String startedBy;
    private String finishedBy;
    private String state;
    private Boolean includeProcessVariables;
    private Collection<String> includeProcessVariablesNames;
    private List<QueryVariable> variables;
    private String callbackId;
    private Set<String> callbackIds;
    private String callbackType;
    private String parentCaseInstanceId;
    private Boolean withoutCallbackId;
    private String tenantId;
    private String tenantIdLike;
    private String tenantIdLikeIgnoreCase;
    private Boolean withoutTenantId;
    private String rootScopeId;
    private String parentScopeId;

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public List<String> getProcessInstanceIds() {
        return processInstanceIds;
    }

    public void setProcessInstanceIds(List<String> processInstanceIds) {
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
    
    public String getProcessBusinessKeyLike() {
        return processBusinessKeyLike;
    }

    public void setProcessBusinessKeyLike(String processBusinessKeyLike) {
        this.processBusinessKeyLike = processBusinessKeyLike;
    }
    
    public String getProcessBusinessKeyLikeIgnoreCase() {
        return processBusinessKeyLikeIgnoreCase;
    }

    public void setProcessBusinessKeyLikeIgnoreCase(String processBusinessKeyLikeIgnoreCase) {
        this.processBusinessKeyLikeIgnoreCase = processBusinessKeyLikeIgnoreCase;
    }

    public String getProcessBusinessStatus() {
        return processBusinessStatus;
    }

    public void setProcessBusinessStatus(String processBusinessStatus) {
        this.processBusinessStatus = processBusinessStatus;
    }

    public String getProcessBusinessStatusLike() {
        return processBusinessStatusLike;
    }

    public void setProcessBusinessStatusLike(String processBusinessStatusLike) {
        this.processBusinessStatusLike = processBusinessStatusLike;
    }

    public String getProcessBusinessStatusLikeIgnoreCase() {
        return processBusinessStatusLikeIgnoreCase;
    }

    public void setProcessBusinessStatusLikeIgnoreCase(String processBusinessStatusLikeIgnoreCase) {
        this.processBusinessStatusLikeIgnoreCase = processBusinessStatusLikeIgnoreCase;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    public String getProcessDefinitionKey() {
        return processDefinitionKey;
    }

    public void setProcessDefinitionKey(String processDefinitionKey) {
        this.processDefinitionKey = processDefinitionKey;
    }

    public List<String> getProcessDefinitionKeys() {
        return processDefinitionKeys;
    }

    public void setProcessDefinitionKeys(List<String> processDefinitionKeys) {
        this.processDefinitionKeys = processDefinitionKeys;
    }

    public List<String> getExcludeProcessDefinitionKeys() {
        return excludeProcessDefinitionKeys;
    }

    public void setExcludeProcessDefinitionKeys(List<String> excludeProcessDefinitionKeys) {
        this.excludeProcessDefinitionKeys = excludeProcessDefinitionKeys;
    }

    public List<String> getProcessDefinitionKeyIn() {
        return processDefinitionKeyIn;
    }

    public void setProcessDefinitionKeyIn(List<String> processDefinitionKeyIn) {
        this.processDefinitionKeyIn = processDefinitionKeyIn;
    }

    public List<String> getProcessDefinitionKeyNotIn() {
        return processDefinitionKeyNotIn;
    }

    public void setProcessDefinitionKeyNotIn(List<String> processDefinitionKeyNotIn) {
        this.processDefinitionKeyNotIn = processDefinitionKeyNotIn;
    }

    public String getProcessDefinitionName() {
        return processDefinitionName;
    }

    public void setProcessDefinitionName(String processDefinitionName) {
        this.processDefinitionName = processDefinitionName;
    }
    
    public String getProcessDefinitionKeyLike() {
        return processDefinitionKeyLike;
    }

    public void setProcessDefinitionKeyLike(String processDefinitionKeyLike) {
        this.processDefinitionKeyLike = processDefinitionKeyLike;
    }

    public String getProcessDefinitionKeyLikeIgnoreCase() {
        return processDefinitionKeyLikeIgnoreCase;
    }

    public void setProcessDefinitionKeyLikeIgnoreCase(String processDefinitionKeyLikeIgnoreCase) {
        this.processDefinitionKeyLikeIgnoreCase = processDefinitionKeyLikeIgnoreCase;
    }

    public String getProcessDefinitionNameLike() {
        return processDefinitionNameLike;
    }

    public void setProcessDefinitionNameLike(String processDefinitionNameLike) {
        this.processDefinitionNameLike = processDefinitionNameLike;
    }

    public String getProcessDefinitionNameLikeIgnoreCase() {
        return processDefinitionNameLikeIgnoreCase;
    }

    public void setProcessDefinitionNameLikeIgnoreCase(String processDefinitionNameLikeIgnoreCase) {
        this.processDefinitionNameLikeIgnoreCase = processDefinitionNameLikeIgnoreCase;
    }

    public String getProcessDefinitionCategoryLike() {
        return processDefinitionCategoryLike;
    }

    public void setProcessDefinitionCategoryLike(String processDefinitionCategoryLike) {
        this.processDefinitionCategoryLike = processDefinitionCategoryLike;
    }

    public String getProcessDefinitionCategoryLikeIgnoreCase() {
        return processDefinitionCategoryLikeIgnoreCase;
    }

    public void setProcessDefinitionCategoryLikeIgnoreCase(String processDefinitionCategoryLikeIgnoreCase) {
        this.processDefinitionCategoryLikeIgnoreCase = processDefinitionCategoryLikeIgnoreCase;
    }

    public Integer getProcessDefinitionVersion() {
        return processDefinitionVersion;
    }

    public void setProcessDefinitionVersion(Integer processDefinitionVersion) {
        this.processDefinitionVersion = processDefinitionVersion;
    }

    public String getProcessDefinitionCategory() {
        return processDefinitionCategory;
    }

    public void setProcessDefinitionCategory(String processDefinitionCategory) {
        this.processDefinitionCategory = processDefinitionCategory;
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

    public Boolean getExcludeSubprocesses() {
        return excludeSubprocesses;
    }

    public void setExcludeSubprocesses(Boolean excludeSubprocesses) {
        this.excludeSubprocesses = excludeSubprocesses;
    }

    public Boolean getFinished() {
        return finished;
    }

    public void setFinished(Boolean finished) {
        this.finished = finished;
    }
    
    public String getActiveActivityId() {
        return activeActivityId;
    }

    public void setActiveActivityId(String activeActivityId) {
        this.activeActivityId = activeActivityId;
    }

    public Set<String> getActiveActivityIds() {
        return activeActivityIds;
    }

    public void setActiveActivityIds(Set<String> activeActivityIds) {
        this.activeActivityIds = activeActivityIds;
    }

    public String getInvolvedUser() {
        return involvedUser;
    }

    public void setInvolvedUser(String involvedUser) {
        this.involvedUser = involvedUser;
    }

    public Date getFinishedAfter() {
        return finishedAfter;
    }

    public void setFinishedAfter(Date finishedAfter) {
        this.finishedAfter = finishedAfter;
    }

    public Date getFinishedBefore() {
        return finishedBefore;
    }

    public void setFinishedBefore(Date finishedBefore) {
        this.finishedBefore = finishedBefore;
    }

    public Date getStartedAfter() {
        return startedAfter;
    }

    public void setStartedAfter(Date startedAfter) {
        this.startedAfter = startedAfter;
    }

    public Date getStartedBefore() {
        return startedBefore;
    }

    public void setStartedBefore(Date startedBefore) {
        this.startedBefore = startedBefore;
    }

    public String getStartedBy() {
        return startedBy;
    }

    public void setStartedBy(String startedBy) {
        this.startedBy = startedBy;
    }

    public String getFinishedBy() {
        return finishedBy;
    }

    public void setFinishedBy(String finishedBy) {
        this.finishedBy = finishedBy;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Boolean getIncludeProcessVariables() {
        return includeProcessVariables;
    }

    public void setIncludeProcessVariables(Boolean includeProcessVariables) {
        this.includeProcessVariables = includeProcessVariables;
    }

    public Collection<String> getIncludeProcessVariablesNames() {
        return includeProcessVariablesNames;
    }

    public void setIncludeProcessVariablesNames(Collection<String> includeProcessVariablesNames) {
        this.includeProcessVariablesNames = includeProcessVariablesNames;
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

    public String getParentCaseInstanceId() {
        return parentCaseInstanceId;
    }

    public void setParentCaseInstanceId(String parentCaseInstanceId) {
        this.parentCaseInstanceId = parentCaseInstanceId;
    }

    public Boolean getWithoutCallbackId() {
        return withoutCallbackId;
    }

    public void setWithoutCallbackId(Boolean withoutCallbackId) {
        this.withoutCallbackId = withoutCallbackId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getTenantIdLike() {
        return tenantIdLike;
    }

    public void setTenantIdLike(String tenantIdLike) {
        this.tenantIdLike = tenantIdLike;
    }
    
    public String getTenantIdLikeIgnoreCase() {
        return tenantIdLikeIgnoreCase;
    }

    public void setTenantIdLikeIgnoreCase(String tenantIdLikeIgnoreCase) {
        this.tenantIdLikeIgnoreCase = tenantIdLikeIgnoreCase;
    }

    public Boolean getWithoutTenantId() {
        return withoutTenantId;
    }

    public void setWithoutTenantId(Boolean withoutTenantId) {
        this.withoutTenantId = withoutTenantId;
    }

    public String getRootScopeId() {
        return rootScopeId;
    }

    public void setRootScopeId(String rootScopeId) {
        this.rootScopeId = rootScopeId;
    }

    public String getParentScopeId() {
        return parentScopeId;
    }

    public void setParentScopeId(String parentScopeId) {
        this.parentScopeId = parentScopeId;
    }

    public Set<String> getCallbackIds() {
        return callbackIds;
    }

    public void setCallbackIds(Set<String> callbackIds) {
        this.callbackIds = callbackIds;
    }
}
