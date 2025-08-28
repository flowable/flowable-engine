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

package org.flowable.engine.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.query.CacheAwareQuery;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.db.SuspensionState;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.eventsubscription.service.impl.EventSubscriptionQueryValue;
import org.flowable.variable.service.impl.AbstractVariableQueryImpl;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 * @author Tijs Rademakers
 * @author Frederik Heremans
 * @author Falko Menge
 * @author Daniel Meyer
 */
public class ProcessInstanceQueryImpl extends AbstractVariableQueryImpl<ProcessInstanceQuery, ProcessInstance> implements 
        ProcessInstanceQuery, CacheAwareQuery<ExecutionEntity>, Serializable {

    private static final long serialVersionUID = 1L;
    
    protected ProcessEngineConfigurationImpl processEngineConfiguration;
    
    protected String executionId;
    protected String businessKey;
    protected String businessKeyLike;
    protected String businessKeyLikeIgnoreCase;
    protected boolean includeChildExecutionsWithBusinessKeyQuery;
    protected String businessStatus;
    protected String businessStatusLike;
    protected String businessStatusLikeIgnoreCase;
    protected String processDefinitionId;
    protected Set<String> processDefinitionIds;
    protected String processDefinitionCategory;
    protected String processDefinitionCategoryLike;
    protected String processDefinitionCategoryLikeIgnoreCase;
    protected String processDefinitionName;
    protected String processDefinitionNameLike;
    protected String processDefinitionNameLikeIgnoreCase;
    protected Integer processDefinitionVersion;
    protected Set<String> processInstanceIds;
    private List<List<String>> safeProcessInstanceIds;
    protected String processDefinitionKey;
    protected String processDefinitionKeyLike;
    protected String processDefinitionKeyLikeIgnoreCase;
    protected Set<String> processDefinitionKeys;
    protected Set<String> excludeProcessDefinitionKeys;
    protected String processDefinitionEngineVersion;
    protected String deploymentId;
    protected List<String> deploymentIds;
    protected String superProcessInstanceId;
    protected String subProcessInstanceId;
    protected boolean excludeSubprocesses;
    protected String involvedUser;
    protected IdentityLinkQueryObject involvedUserIdentityLink;
    protected Set<String> involvedGroups;
    private List<List<String>> safeInvolvedGroups;
    protected IdentityLinkQueryObject involvedGroupIdentityLink;
    protected SuspensionState suspensionState;
    protected boolean includeProcessVariables;
    protected Collection<String> variableNamesToInclude;
    protected boolean withJobException;
    protected String name;
    protected String nameLike;
    protected String nameLikeIgnoreCase;
    protected String rootScopeId;
    protected String parentScopeId;
    protected String activeActivityId;
    protected Set<String> activeActivityIds;
    protected String callbackId;
    protected Set<String> callbackIds;
    protected String callbackType;
    protected String parentCaseInstanceId;
    protected String referenceId;
    protected String referenceType;
    protected String locale;
    protected boolean withLocalizationFallback;

    protected String tenantId;
    protected String tenantIdLike;
    protected String tenantIdLikeIgnoreCase;
    protected boolean withoutTenantId;

    protected List<ProcessInstanceQueryImpl> orQueryObjects = new ArrayList<>();
    protected ProcessInstanceQueryImpl currentOrQueryObject;
    protected boolean inOrStatement;

    protected Date startedBefore;
    protected Date startedAfter;
    protected String startedBy;

    // Unused, see dynamic query
    protected String activityId;
    protected List<EventSubscriptionQueryValue> eventSubscriptions;
    protected boolean onlyChildExecutions;
    protected boolean onlyProcessInstanceExecutions;
    protected boolean onlySubProcessExecutions;
    protected String rootProcessInstanceId;

    public ProcessInstanceQueryImpl() {
    }

    public ProcessInstanceQueryImpl(CommandContext commandContext, ProcessEngineConfigurationImpl processEngineConfiguration) {
        super(commandContext, processEngineConfiguration.getVariableServiceConfiguration());
        this.processEngineConfiguration = processEngineConfiguration;
    }

    public ProcessInstanceQueryImpl(CommandExecutor commandExecutor, ProcessEngineConfigurationImpl processEngineConfiguration) {
        super(commandExecutor, processEngineConfiguration.getVariableServiceConfiguration());
        this.processEngineConfiguration = processEngineConfiguration;
    }

    @Override
    public ProcessInstanceQueryImpl processInstanceId(String processInstanceId) {
        if (processInstanceId == null) {
            throw new FlowableIllegalArgumentException("Process instance id is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.executionId = processInstanceId;
        } else {
            this.executionId = processInstanceId;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery processInstanceIds(Set<String> processInstanceIds) {
        if (processInstanceIds == null) {
            throw new FlowableIllegalArgumentException("Set of process instance ids is null");
        }
        if (processInstanceIds.isEmpty()) {
            throw new FlowableIllegalArgumentException("Set of process instance ids is empty");
        }

        if (inOrStatement) {
            this.currentOrQueryObject.processInstanceIds = processInstanceIds;
        } else {
            this.processInstanceIds = processInstanceIds;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery processInstanceBusinessKey(String businessKey) {
        if (businessKey == null) {
            throw new FlowableIllegalArgumentException("Business key is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.businessKey = businessKey;
        } else {
            this.businessKey = businessKey;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery processInstanceBusinessKey(String businessKey, String processDefinitionKey) {
        if (businessKey == null) {
            throw new FlowableIllegalArgumentException("Business key is null");
        }
        if (inOrStatement) {
            throw new FlowableIllegalArgumentException("This method is not supported in an OR statement");
        }

        this.businessKey = businessKey;
        this.processDefinitionKey = processDefinitionKey;
        return this;
    }

    @Override
    public ProcessInstanceQuery processInstanceBusinessKeyLike(String businessKeyLike) {
        if (inOrStatement) {
            this.currentOrQueryObject.businessKeyLike = businessKeyLike;
        } else {
            this.businessKeyLike = businessKeyLike;
        }
        return this;
    }
    
    @Override
    public ProcessInstanceQuery processInstanceBusinessKeyLikeIgnoreCase(String businessKeyLikeIgnoreCase) {
        if (inOrStatement) {
            this.currentOrQueryObject.businessKeyLikeIgnoreCase = businessKeyLikeIgnoreCase;
        } else {
            this.businessKeyLikeIgnoreCase = businessKeyLikeIgnoreCase;
        }
        return this;
    }
    
    @Override
    public ProcessInstanceQuery processInstanceBusinessStatus(String businessStatus) {
        if (businessStatus == null) {
            throw new FlowableIllegalArgumentException("Business status is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.businessStatus = businessStatus;
        } else {
            this.businessStatus = businessStatus;
        }
        return this;
    }
    
    @Override
    public ProcessInstanceQuery processInstanceBusinessStatusLike(String businessStatusLike) {
        if (inOrStatement) {
            this.currentOrQueryObject.businessStatusLike = businessStatusLike;
        } else {
            this.businessStatusLike = businessStatusLike;
        }
        return this;
    }
    
    @Override
    public ProcessInstanceQuery processInstanceBusinessStatusLikeIgnoreCase(String businessStatusLikeIgnoreCase) {
        if (inOrStatement) {
            this.currentOrQueryObject.businessStatusLikeIgnoreCase = businessStatusLikeIgnoreCase;
        } else {
            this.businessStatusLikeIgnoreCase = businessStatusLikeIgnoreCase;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery processInstanceTenantId(String tenantId) {
        if (tenantId == null) {
            throw new FlowableIllegalArgumentException("process instance tenant id is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.tenantId = tenantId;
        } else {
            this.tenantId = tenantId;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery processInstanceTenantIdLike(String tenantIdLike) {
        if (tenantIdLike == null) {
            throw new FlowableIllegalArgumentException("process instance tenant id is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.tenantIdLike = tenantIdLike;
        } else {
            this.tenantIdLike = tenantIdLike;
        }
        return this;
    }
    
    @Override
    public ProcessInstanceQuery processInstanceTenantIdLikeIgnoreCase(String tenantIdLikeIgnoreCase) {
        if (tenantIdLikeIgnoreCase == null) {
            throw new FlowableIllegalArgumentException("process instance tenant id is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.tenantIdLikeIgnoreCase = tenantIdLikeIgnoreCase;
        } else {
            this.tenantIdLikeIgnoreCase = tenantIdLikeIgnoreCase;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery processInstanceWithoutTenantId() {
        if (inOrStatement) {
            this.currentOrQueryObject.withoutTenantId = true;
        } else {
            this.withoutTenantId = true;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery processDefinitionCategory(String processDefinitionCategory) {
        if (processDefinitionCategory == null) {
            throw new FlowableIllegalArgumentException("Process definition category is null");
        }

        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionCategory = processDefinitionCategory;
        } else {
            this.processDefinitionCategory = processDefinitionCategory;
        }
        return this;
    }
    
    @Override
    public ProcessInstanceQuery processDefinitionCategoryLike(String processDefinitionCategoryLike) {
        if (processDefinitionCategoryLike == null) {
            throw new FlowableIllegalArgumentException("Process definition category is null");
        }

        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionCategoryLike = processDefinitionCategoryLike;
        } else {
            this.processDefinitionCategoryLike = processDefinitionCategoryLike;
        }
        return this;
    }
    
    @Override
    public ProcessInstanceQuery processDefinitionCategoryLikeIgnoreCase(String processDefinitionCategoryLikeIgnoreCase) {
        if (processDefinitionCategoryLikeIgnoreCase == null) {
            throw new FlowableIllegalArgumentException("Process definition category is null");
        }

        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionCategoryLikeIgnoreCase = processDefinitionCategoryLikeIgnoreCase;
        } else {
            this.processDefinitionCategoryLikeIgnoreCase = processDefinitionCategoryLikeIgnoreCase;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery processDefinitionName(String processDefinitionName) {
        if (processDefinitionName == null) {
            throw new FlowableIllegalArgumentException("Process definition name is null");
        }

        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionName = processDefinitionName;
        } else {
            this.processDefinitionName = processDefinitionName;
        }
        return this;
    }
    
    @Override
    public ProcessInstanceQuery processDefinitionNameLike(String processDefinitionNameLike) {
        if (processDefinitionNameLike == null) {
            throw new FlowableIllegalArgumentException("Process definition name is null");
        }

        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionNameLike = processDefinitionNameLike;
        } else {
            this.processDefinitionNameLike = processDefinitionNameLike;
        }
        return this;
    }
    
    @Override
    public ProcessInstanceQuery processDefinitionNameLikeIgnoreCase(String processDefinitionNameLikeIgnoreCase) {
        if (processDefinitionNameLikeIgnoreCase == null) {
            throw new FlowableIllegalArgumentException("Process definition name is null");
        }

        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionNameLikeIgnoreCase = processDefinitionNameLikeIgnoreCase;
        } else {
            this.processDefinitionNameLikeIgnoreCase = processDefinitionNameLikeIgnoreCase;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery processDefinitionVersion(Integer processDefinitionVersion) {
        if (processDefinitionVersion == null) {
            throw new FlowableIllegalArgumentException("Process definition version is null");
        }

        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionVersion = processDefinitionVersion;
        } else {
            this.processDefinitionVersion = processDefinitionVersion;
        }
        return this;
    }

    @Override
    public ProcessInstanceQueryImpl processDefinitionId(String processDefinitionId) {
        if (processDefinitionId == null) {
            throw new FlowableIllegalArgumentException("Process definition id is null");
        }

        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionId = processDefinitionId;
        } else {
            this.processDefinitionId = processDefinitionId;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery processDefinitionIds(Set<String> processDefinitionIds) {
        if (processDefinitionIds == null) {
            throw new FlowableIllegalArgumentException("Set of process definition ids is null");
        }
        if (processDefinitionIds.isEmpty()) {
            throw new FlowableIllegalArgumentException("Set of process definition ids is empty");
        }

        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionIds = processDefinitionIds;
        } else {
            this.processDefinitionIds = processDefinitionIds;
        }
        return this;
    }

    @Override
    public ProcessInstanceQueryImpl processDefinitionKey(String processDefinitionKey) {
        if (processDefinitionKey == null) {
            throw new FlowableIllegalArgumentException("Process definition key is null");
        }

        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionKey = processDefinitionKey;
        } else {
            this.processDefinitionKey = processDefinitionKey;
        }
        return this;
    }
    
    @Override
    public ProcessInstanceQueryImpl processDefinitionKeyLike(String processDefinitionKeyLike) {
        if (processDefinitionKeyLike == null) {
            throw new FlowableIllegalArgumentException("Process definition key is null");
        }

        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionKeyLike = processDefinitionKeyLike;
        } else {
            this.processDefinitionKeyLike = processDefinitionKeyLike;
        }
        return this;
    }
    
    @Override
    public ProcessInstanceQueryImpl processDefinitionKeyLikeIgnoreCase(String processDefinitionKeyLikeIgnoreCase) {
        if (processDefinitionKeyLikeIgnoreCase == null) {
            throw new FlowableIllegalArgumentException("Process definition key is null");
        }

        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionKeyLikeIgnoreCase = processDefinitionKeyLikeIgnoreCase;
        } else {
            this.processDefinitionKeyLikeIgnoreCase = processDefinitionKeyLikeIgnoreCase;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery processDefinitionKeys(Set<String> processDefinitionKeys) {
        if (processDefinitionKeys == null) {
            throw new FlowableIllegalArgumentException("Set of process definition keys is null");
        }
        if (processDefinitionKeys.isEmpty()) {
            throw new FlowableIllegalArgumentException("Set of process definition keys is empty");
        }

        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionKeys = processDefinitionKeys;
        } else {
            this.processDefinitionKeys = processDefinitionKeys;
        }
        return this;
    }
    
    @Override
    public ProcessInstanceQuery excludeProcessDefinitionKeys(Set<String> excludeProcessDefinitionKeys) {
        if (excludeProcessDefinitionKeys == null) {
            throw new FlowableIllegalArgumentException("Set of process definition keys is null");
        }
        if (excludeProcessDefinitionKeys.isEmpty()) {
            throw new FlowableIllegalArgumentException("Set of process definition keys is empty");
        }

        if (inOrStatement) {
            this.currentOrQueryObject.excludeProcessDefinitionKeys = excludeProcessDefinitionKeys;
        } else {
            this.excludeProcessDefinitionKeys = excludeProcessDefinitionKeys;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery processDefinitionEngineVersion(String processDefinitionEngineVersion) {
        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionEngineVersion = processDefinitionEngineVersion;
        } else {
            this.processDefinitionEngineVersion = processDefinitionEngineVersion;
        }
        return this;
    }

    @Override
    public ProcessInstanceQueryImpl deploymentId(String deploymentId) {
        if (inOrStatement) {
            this.currentOrQueryObject.deploymentId = deploymentId;
        } else {
            this.deploymentId = deploymentId;
        }
        return this;
    }

    @Override
    public ProcessInstanceQueryImpl deploymentIdIn(List<String> deploymentIds) {
        if (inOrStatement) {
            this.currentOrQueryObject.deploymentIds = deploymentIds;
        } else {
            this.deploymentIds = deploymentIds;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery superProcessInstanceId(String superProcessInstanceId) {
        if (inOrStatement) {
            this.currentOrQueryObject.superProcessInstanceId = superProcessInstanceId;
        } else {
            this.superProcessInstanceId = superProcessInstanceId;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery subProcessInstanceId(String subProcessInstanceId) {
        if (inOrStatement) {
            this.currentOrQueryObject.subProcessInstanceId = subProcessInstanceId;
        } else {
            this.subProcessInstanceId = subProcessInstanceId;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery excludeSubprocesses(boolean excludeSubprocesses) {
        if (inOrStatement) {
            this.currentOrQueryObject.excludeSubprocesses = excludeSubprocesses;
        } else {
            this.excludeSubprocesses = excludeSubprocesses;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery involvedUser(String involvedUser) {
        if (involvedUser == null) {
            throw new FlowableIllegalArgumentException("Involved user is null");
        }

        if (inOrStatement) {
            this.currentOrQueryObject.involvedUser = involvedUser;
        } else {
            this.involvedUser = involvedUser;
        }
        return this;
    }
    
    @Override
    public ProcessInstanceQuery involvedUser(String userId, String identityLinkType) {
        if (userId == null) {
            throw new FlowableIllegalArgumentException("userId is null");
        }
        if (identityLinkType == null) {
            throw new FlowableIllegalArgumentException("identityLinkType is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.involvedUserIdentityLink = new IdentityLinkQueryObject(userId, null, identityLinkType);
        } else {
            this.involvedUserIdentityLink = new IdentityLinkQueryObject(userId, null, identityLinkType);
        }
        return this;
    }
    
    @Override
    public ProcessInstanceQuery involvedGroup(String groupId, String identityLinkType) {
        if (groupId == null) {
            throw new FlowableIllegalArgumentException("groupId is null");
        }
        if (identityLinkType == null) {
            throw new FlowableIllegalArgumentException("identityLinkType is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.involvedGroupIdentityLink = new IdentityLinkQueryObject(null, groupId, identityLinkType);
        } else {
            this.involvedGroupIdentityLink = new IdentityLinkQueryObject(null, groupId, identityLinkType);
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery involvedGroups(Set<String> involvedGroups) {
        if (involvedGroups == null) {
            throw new FlowableIllegalArgumentException("Involved groups are null");
        }
        if (involvedGroups.isEmpty()) {
            throw new FlowableIllegalArgumentException("Involved groups are empty");
        }

        if (inOrStatement) {
            this.currentOrQueryObject.involvedGroups = involvedGroups;
        } else {
            this.involvedGroups = involvedGroups;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery active() {
        if (inOrStatement) {
            this.currentOrQueryObject.suspensionState = SuspensionState.ACTIVE;
        } else {
            this.suspensionState = SuspensionState.ACTIVE;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery suspended() {
        if (inOrStatement) {
            this.currentOrQueryObject.suspensionState = SuspensionState.SUSPENDED;
        } else {
            this.suspensionState = SuspensionState.SUSPENDED;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery includeProcessVariables() {
        this.includeProcessVariables = true;
        return this;
    }

    @Override
    public ProcessInstanceQuery includeProcessVariables(Collection<String> variableNames) {
        if (variableNames == null || variableNames.isEmpty()) {
            throw new FlowableIllegalArgumentException("variableNames are null or empty");
        }
        includeProcessVariables();
        this.variableNamesToInclude = new LinkedHashSet<>(variableNames);
        return this;
    }

    @Override
    public ProcessInstanceQuery withJobException() {
        this.withJobException = true;
        return this;
    }

    @Override
    public ProcessInstanceQuery processInstanceName(String name) {
        if (inOrStatement) {
            this.currentOrQueryObject.name = name;
        } else {
            this.name = name;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery processInstanceNameLike(String nameLike) {
        if (inOrStatement) {
            this.currentOrQueryObject.nameLike = nameLike;
        } else {
            this.nameLike = nameLike;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery processInstanceNameLikeIgnoreCase(String nameLikeIgnoreCase) {
        if (inOrStatement) {
            this.currentOrQueryObject.nameLikeIgnoreCase = nameLikeIgnoreCase.toLowerCase();
        } else {
            this.nameLikeIgnoreCase = nameLikeIgnoreCase.toLowerCase();
        }
        return this;
    }
    
    @Override
    public ProcessInstanceQuery processInstanceRootScopeId(String rootId) {
        if (inOrStatement) {
            this.currentOrQueryObject.rootScopeId = rootId;
        } else {
            this.rootScopeId = rootId;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery processInstanceParentScopeId(String parentId) {
        if (inOrStatement) {
            this.currentOrQueryObject.parentScopeId = parentId;
        } else {
            this.parentScopeId = parentId;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery activeActivityId(String activityId) {
        if (inOrStatement) {
            this.currentOrQueryObject.activeActivityId = activityId;
        } else {
            this.activeActivityId = activityId;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery activeActivityIds(Set<String> activityIds) {
        if (inOrStatement) {
            this.currentOrQueryObject.activeActivityIds = activityIds;
        } else {
            this.activeActivityIds = activityIds;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery processInstanceCallbackId(String callbackId) {
        if (inOrStatement) {
            this.currentOrQueryObject.callbackId = callbackId;
        } else {
            this.callbackId = callbackId;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery processInstanceCallbackIds(Set<String> callbackIds) {
        if (callbackIds == null || callbackIds.isEmpty()) {
            throw new FlowableIllegalArgumentException("callbackIds is null or empty");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.callbackIds = callbackIds;
        } else {
            this.callbackIds = callbackIds;
        }
        return this;
    }
    
    @Override
    public ProcessInstanceQuery processInstanceCallbackType(String callbackType) {
        if (inOrStatement) {
            this.currentOrQueryObject.callbackType = callbackType;
        } else {
            this.callbackType = callbackType;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery parentCaseInstanceId(String parentCaseInstanceId) {
        if (inOrStatement) {
            this.currentOrQueryObject.parentCaseInstanceId = parentCaseInstanceId;
        } else {
            this.parentCaseInstanceId = parentCaseInstanceId;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery processInstanceReferenceId(String referenceId) {
        if (inOrStatement) {
            this.currentOrQueryObject.referenceId = referenceId;
        } else {
            this.referenceId = referenceId;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery processInstanceReferenceType(String referenceType) {
        if (inOrStatement) {
            this.currentOrQueryObject.referenceType = referenceType;
        } else {
            this.referenceType = referenceType;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery or() {
        if (inOrStatement) {
            throw new FlowableException("the query is already in an or statement");
        }

        inOrStatement = true;
        if (commandContext != null) {
            currentOrQueryObject = new ProcessInstanceQueryImpl(commandContext, processEngineConfiguration);
        } else {
            currentOrQueryObject = new ProcessInstanceQueryImpl(commandExecutor, processEngineConfiguration);
        }
        orQueryObjects.add(currentOrQueryObject);
        return this;
    }

    @Override
    public ProcessInstanceQuery endOr() {
        if (!inOrStatement) {
            throw new FlowableException("endOr() can only be called after calling or()");
        }

        inOrStatement = false;
        currentOrQueryObject = null;
        return this;
    }

    @Override
    public ProcessInstanceQuery variableValueEquals(String variableName, Object variableValue) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueEquals(variableName, variableValue, false);
            return this;
        } else {
            return variableValueEquals(variableName, variableValue, false);
        }
    }

    @Override
    public ProcessInstanceQuery variableValueNotEquals(String variableName, Object variableValue) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueNotEquals(variableName, variableValue, false);
            return this;
        } else {
            return variableValueNotEquals(variableName, variableValue, false);
        }
    }

    @Override
    public ProcessInstanceQuery variableValueEquals(Object variableValue) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueEquals(variableValue, false);
            return this;
        } else {
            return variableValueEquals(variableValue, false);
        }
    }

    @Override
    public ProcessInstanceQuery variableValueEqualsIgnoreCase(String name, String value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueEqualsIgnoreCase(name, value, false);
            return this;
        } else {
            return variableValueEqualsIgnoreCase(name, value, false);
        }
    }

    @Override
    public ProcessInstanceQuery variableValueNotEqualsIgnoreCase(String name, String value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueNotEqualsIgnoreCase(name, value, false);
            return this;
        } else {
            return variableValueNotEqualsIgnoreCase(name, value, false);
        }
    }

    @Override
    public ProcessInstanceQuery variableValueGreaterThan(String name, Object value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueGreaterThan(name, value, false);
            return this;
        } else {
            return variableValueGreaterThan(name, value, false);
        }
    }

    @Override
    public ProcessInstanceQuery variableValueGreaterThanOrEqual(String name, Object value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueGreaterThanOrEqual(name, value, false);
            return this;
        } else {
            return variableValueGreaterThanOrEqual(name, value, false);
        }
    }

    @Override
    public ProcessInstanceQuery variableValueLessThan(String name, Object value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueLessThan(name, value, false);
            return this;
        } else {
            return variableValueLessThan(name, value, false);
        }
    }

    @Override
    public ProcessInstanceQuery variableValueLessThanOrEqual(String name, Object value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueLessThanOrEqual(name, value, false);
            return this;
        } else {
            return variableValueLessThanOrEqual(name, value, false);
        }
    }

    @Override
    public ProcessInstanceQuery variableValueLike(String name, String value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueLike(name, value, false);
            return this;
        } else {
            return variableValueLike(name, value, false);
        }
    }

    @Override
    public ProcessInstanceQuery variableValueLikeIgnoreCase(String name, String value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueLikeIgnoreCase(name, value, false);
            return this;
        } else {
            return variableValueLikeIgnoreCase(name, value, false);
        }
    }
    
    @Override
    public ProcessInstanceQuery variableExists(String name) {
        if (inOrStatement) {
            currentOrQueryObject.variableExists(name, false);
            return this;
        } else {
            return variableExists(name, false);
        }
    }
    
    @Override
    public ProcessInstanceQuery variableNotExists(String name) {
        if (inOrStatement) {
            currentOrQueryObject.variableNotExists(name, false);
            return this;
        } else {
            return variableNotExists(name, false);
        }
    }

    @Override
    public ProcessInstanceQuery locale(String locale) {
        this.locale = locale;
        return this;
    }

    @Override
    public ProcessInstanceQuery withLocalizationFallback() {
        withLocalizationFallback = true;
        return this;
    }

    @Override
    public ProcessInstanceQuery startedBefore(Date beforeTime) {
        if (inOrStatement) {
            this.currentOrQueryObject.startedBefore = beforeTime;
        } else {
            this.startedBefore = beforeTime;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery startedAfter(Date afterTime) {
        if (inOrStatement) {
            this.currentOrQueryObject.startedAfter = afterTime;
        } else {
            this.startedAfter = afterTime;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery startedBy(String userId) {
        if (inOrStatement) {
            this.currentOrQueryObject.startedBy = userId;
        } else {
            this.startedBy = userId;
        }
        return this;
    }

    @Override
    public ProcessInstanceQuery orderByProcessInstanceId() {
        this.orderProperty = ProcessInstanceQueryProperty.PROCESS_INSTANCE_ID;
        return this;
    }

    @Override
    public ProcessInstanceQuery orderByProcessDefinitionId() {
        this.orderProperty = ProcessInstanceQueryProperty.PROCESS_DEFINITION_ID;
        return this;
    }

    @Override
    public ProcessInstanceQuery orderByProcessDefinitionKey() {
        this.orderProperty = ProcessInstanceQueryProperty.PROCESS_DEFINITION_KEY;
        return this;
    }

    @Override
    public ProcessInstanceQuery orderByStartTime() {
        return orderBy(ProcessInstanceQueryProperty.PROCESS_START_TIME);
    }

    @Override
    public ProcessInstanceQuery orderByTenantId() {
        this.orderProperty = ProcessInstanceQueryProperty.TENANT_ID;
        return this;
    }

    // results /////////////////////////////////////////////////////////////////

    @Override
    public long executeCount(CommandContext commandContext) {
        ensureVariablesInitialized();
        
        if (processEngineConfiguration.getProcessInstanceQueryInterceptor() != null) {
            processEngineConfiguration.getProcessInstanceQueryInterceptor().beforeProcessInstanceQueryExecute(this);
        }
        
        return processEngineConfiguration.getExecutionEntityManager().findProcessInstanceCountByQueryCriteria(this);
    }

    @Override
    public List<ProcessInstance> executeList(CommandContext commandContext) {
        ensureVariablesInitialized();
        List<ProcessInstance> processInstances = null;
        
        if (processEngineConfiguration.getProcessInstanceQueryInterceptor() != null) {
            processEngineConfiguration.getProcessInstanceQueryInterceptor().beforeProcessInstanceQueryExecute(this);
        }
        
        if (includeProcessVariables) {
            processInstances = processEngineConfiguration.getExecutionEntityManager().findProcessInstanceAndVariablesByQueryCriteria(this);
        } else {
            processInstances = processEngineConfiguration.getExecutionEntityManager().findProcessInstanceByQueryCriteria(this);
        }

        if (processEngineConfiguration.getPerformanceSettings().isEnableLocalization() && processEngineConfiguration.getInternalProcessLocalizationManager() != null) {
            for (ProcessInstance processInstance : processInstances) {
                processEngineConfiguration.getInternalProcessLocalizationManager().localize(processInstance, locale, withLocalizationFallback);
            }
        }
        
        if (processEngineConfiguration.getProcessInstanceQueryInterceptor() != null) {
            processEngineConfiguration.getProcessInstanceQueryInterceptor().afterProcessInstanceQueryExecute(this, processInstances);
        }

        return processInstances;
    }

    @Override
    public void enhanceCachedValue(ExecutionEntity processInstance) {
        if (includeProcessVariables) {
            if (variableNamesToInclude == null) {
                processInstance.getQueryVariables().addAll(processEngineConfiguration.getVariableServiceConfiguration()
                        .getVariableService().findVariableInstancesByExecutionId(processInstance.getId()));
            } else {
                processInstance.getQueryVariables().addAll(processEngineConfiguration.getVariableServiceConfiguration()
                        .getVariableService()
                        .createInternalVariableInstanceQuery().executionId(processInstance.getId()).withoutTaskId()
                        .names(variableNamesToInclude)
                        .list());
            }
        }
    }

    @Override
    protected void ensureVariablesInitialized() {
        super.ensureVariablesInitialized();

        for (ProcessInstanceQueryImpl orQueryObject : orQueryObjects) {
            orQueryObject.ensureVariablesInitialized();
        }
    }

    // getters /////////////////////////////////////////////////////////////////

    public boolean getOnlyProcessInstances() {
        return true; // See dynamic query in runtime.mapping.xml
    }

    public String getProcessInstanceId() {
        return executionId;
    }
    
    @Override
    public String getId() {
        return executionId;
    }

    public String getRootProcessInstanceId() {
        return rootProcessInstanceId;
    }

    public Set<String> getProcessInstanceIds() {
        return processInstanceIds;
    }

    public String getRootScopeId() {
        return rootScopeId;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public String getBusinessKeyLike() {
        return businessKeyLike;
    }
    
    public String getBusinessKeyLikeIgnoreCase() {
        return businessKeyLikeIgnoreCase;
    }

    public String getBusinessStatus() {
        return businessStatus;
    }

    public String getBusinessStatusLike() {
        return businessStatusLike;
    }
    
    public String getBusinessStatusLikeIgnoreCase() {
        return businessStatusLikeIgnoreCase;
    }

    public boolean isIncludeChildExecutionsWithBusinessKeyQuery() {
        return includeChildExecutionsWithBusinessKeyQuery;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public Set<String> getProcessDefinitionIds() {
        return processDefinitionIds;
    }

    public String getProcessDefinitionCategory() {
        return processDefinitionCategory;
    }

    public String getProcessDefinitionCategoryLike() {
        return processDefinitionCategoryLike;
    }

    public String getProcessDefinitionCategoryLikeIgnoreCase() {
        return processDefinitionCategoryLikeIgnoreCase;
    }

    public String getParentScopeId() {
        return parentScopeId;
    }

    public String getProcessDefinitionName() {
        return processDefinitionName;
    }
    
    public String getProcessDefinitionNameLike() {
        return processDefinitionNameLike;
    }

    public String getProcessDefinitionNameLikeIgnoreCase() {
        return processDefinitionNameLikeIgnoreCase;
    }

    public Integer getProcessDefinitionVersion() {
        return processDefinitionVersion;
    }

    public String getProcessDefinitionKey() {
        return processDefinitionKey;
    }
    
    public String getProcessDefinitionKeyLike() {
        return processDefinitionKeyLike;
    }

    public String getProcessDefinitionKeyLikeIgnoreCase() {
        return processDefinitionKeyLikeIgnoreCase;
    }

    public Set<String> getProcessDefinitionKeys() {
        return processDefinitionKeys;
    }

    public Set<String> getExcludeProcessDefinitionKeys() {
        return excludeProcessDefinitionKeys;
    }

    public String getProcessDefinitionEngineVersion() {
        return processDefinitionEngineVersion;
    }

    public String getActivityId() {
        return null; // Unused, see dynamic query
    }

    public String getSuperProcessInstanceId() {
        return superProcessInstanceId;
    }

    public String getSubProcessInstanceId() {
        return subProcessInstanceId;
    }

    public boolean isExcludeSubprocesses() {
        return excludeSubprocesses;
    }

    public String getInvolvedUser() {
        return involvedUser;
    }

    public IdentityLinkQueryObject getInvolvedUserIdentityLink() {
        return involvedUserIdentityLink;
    }

    public IdentityLinkQueryObject getInvolvedGroupIdentityLink() {
        return involvedGroupIdentityLink;
    }

    public Set<String> getInvolvedGroups() {
        return involvedGroups;
    }

    public SuspensionState getSuspensionState() {
        return suspensionState;
    }

    public void setSuspensionState(SuspensionState suspensionState) {
        this.suspensionState = suspensionState;
    }

    public List<EventSubscriptionQueryValue> getEventSubscriptions() {
        return eventSubscriptions;
    }

    public void setEventSubscriptions(List<EventSubscriptionQueryValue> eventSubscriptions) {
        this.eventSubscriptions = eventSubscriptions;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getTenantIdLike() {
        return tenantIdLike;
    }
    
    public String getTenantIdLikeIgnoreCase() {
        return tenantIdLikeIgnoreCase;
    }

    public boolean isWithoutTenantId() {
        return withoutTenantId;
    }

    public String getName() {
        return name;
    }

    public String getNameLike() {
        return nameLike;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setNameLike(String nameLike) {
        this.nameLike = nameLike;
    }

    public String getExecutionId() {
        return executionId;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public List<String> getDeploymentIds() {
        return deploymentIds;
    }

    public boolean isIncludeProcessVariables() {
        return includeProcessVariables;
    }

    public Collection<String> getVariableNamesToInclude() {
        return variableNamesToInclude;
    }

    public boolean iswithException() {
        return withJobException;
    }

    public String getNameLikeIgnoreCase() {
        return nameLikeIgnoreCase;
    }
    
    public String getActiveActivityId() {
        return activeActivityId;
    }

    public Set<String> getActiveActivityIds() {
        return activeActivityIds;
    }

    public String getCallbackId() {
        return callbackId;
    }

    public Set<String> getCallbackIds() {
        return callbackIds;
    }

    public String getCallbackType() {
        return callbackType;
    }

    public String getParentCaseInstanceId() {
        return parentCaseInstanceId;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public List<ProcessInstanceQueryImpl> getOrQueryObjects() {
        return orQueryObjects;
    }

    /**
     * Methods needed for ibatis because of re-use of query-xml for executions. ExecutionQuery contains a parentId property.
     */

    public String getParentId() {
        return null;
    }

    public boolean isOnlyChildExecutions() {
        return onlyChildExecutions;
    }

    public boolean isOnlyProcessInstanceExecutions() {
        return onlyProcessInstanceExecutions;
    }

    public boolean isOnlySubProcessExecutions() {
        return onlySubProcessExecutions;
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

    public String getStartedBy() {
        return startedBy;
    }

    public void setStartedBy(String startedBy) {
        this.startedBy = startedBy;
    }

    public boolean isWithJobException() {
        return withJobException;
    }

    public String getLocale() {
        return locale;
    }

    public boolean isNeedsProcessDefinitionOuterJoin() {
        if (isNeedsPaging()) {
            if (AbstractEngineConfiguration.DATABASE_TYPE_ORACLE.equals(databaseType)
                    || AbstractEngineConfiguration.DATABASE_TYPE_DB2.equals(databaseType)
                    || AbstractEngineConfiguration.DATABASE_TYPE_MSSQL.equals(databaseType)) {
                // When using oracle, db2 or mssql we don't need outer join for the process definition join.
                // It is not needed because the outer join order by is done by the row number instead
                return false;
            }
        }

        return hasOrderByForColumn(ProcessInstanceQueryProperty.PROCESS_DEFINITION_KEY.getName());
    }

    public List<List<String>> getSafeProcessInstanceIds() {
        return safeProcessInstanceIds;
    }

    public void setSafeProcessInstanceIds(List<List<String>> safeProcessInstanceIds) {
        this.safeProcessInstanceIds = safeProcessInstanceIds;
    }

    public List<List<String>> getSafeInvolvedGroups() {
        return safeInvolvedGroups;
    }

    public void setSafeInvolvedGroups(List<List<String>> safeInvolvedGroups) {
        this.safeInvolvedGroups = safeInvolvedGroups;
    }
}
