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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.query.CacheAwareQuery;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.interceptor.CommandConfig;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.persistence.cache.EntityCache;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.cmd.DeleteHistoricProcessInstancesCmd;
import org.flowable.engine.impl.cmd.DeleteRelatedDataOfRemovedHistoricProcessInstancesCmd;
import org.flowable.engine.impl.cmd.DeleteTaskAndActivityDataOfRemovedHistoricProcessInstancesCmd;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.impl.delete.DeleteHistoricProcessInstancesUsingBatchesCmd;
import org.flowable.engine.impl.persistence.entity.HistoricProcessInstanceEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.variable.service.impl.AbstractVariableQueryImpl;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInstanceEntity;

/**
 * @author Tom Baeyens
 * @author Tijs Rademakers
 * @author Falko Menge
 * @author Bernd Ruecker
 * @author Joram Barrez
 */
public class HistoricProcessInstanceQueryImpl extends AbstractVariableQueryImpl<HistoricProcessInstanceQuery, HistoricProcessInstance> 
        implements HistoricProcessInstanceQuery, CacheAwareQuery<HistoricProcessInstanceEntity> {

    private static final long serialVersionUID = 1L;
    
    protected ProcessEngineConfigurationImpl processEngineConfiguration;
    
    protected String processInstanceId;
    protected String processDefinitionId;
    protected String businessKey;
    protected String businessKeyLike;
    protected String businessKeyLikeIgnoreCase;
    protected String businessStatus;
    protected String businessStatusLike;
    protected String businessStatusLikeIgnoreCase;
    protected String deploymentId;
    protected List<String> deploymentIds;
    protected boolean finished;
    protected boolean unfinished;
    protected boolean deleted;
    protected boolean notDeleted;
    protected String startedBy;
    protected String finishedBy;
    protected String state;
    protected String superProcessInstanceId;
    protected boolean excludeSubprocesses;
    protected List<String> processDefinitionKeyIn;
    protected List<String> excludeProcessDefinitionKeys;
    protected List<String> processKeyNotIn;
    protected Date startedBefore;
    protected Date startedAfter;
    protected Date finishedBefore;
    protected Date finishedAfter;
    protected String processDefinitionKey;
    protected String processDefinitionKeyLike;
    protected String processDefinitionKeyLikeIgnoreCase;
    protected String processDefinitionCategory;
    protected String processDefinitionCategoryLike;
    protected String processDefinitionCategoryLikeIgnoreCase;
    protected String processDefinitionName;
    protected String processDefinitionNameLike;
    protected String processDefinitionNameLikeIgnoreCase;
    protected Integer processDefinitionVersion;
    protected Set<String> processInstanceIds;
    private List<List<String>> safeProcessInstanceIds;
    protected String activeActivityId;
    protected Set<String> activeActivityIds;
    protected String involvedUser;
    protected IdentityLinkQueryObject involvedUserIdentityLink;
    protected Set<String> involvedGroups;
    private List<List<String>> safeInvolvedGroups;
    protected IdentityLinkQueryObject involvedGroupIdentityLink;
    protected boolean includeProcessVariables;
    protected Collection<String> variableNamesToInclude;
    protected boolean withJobException;
    protected String tenantId;
    protected String tenantIdLike;
    protected String tenantIdLikeIgnoreCase;
    protected boolean withoutTenantId;
    protected String name;
    protected String nameLike;
    protected String nameLikeIgnoreCase;
    protected String rootScopeId;
    protected String parentScopeId;
    protected String callbackId;
    protected Set<String> callbackIds;
    protected String callbackType;
    protected String parentCaseInstanceId;
    protected boolean withoutCallbackId;
    protected String referenceId;
    protected String referenceType;
    protected String locale;
    protected boolean withLocalizationFallback;
    protected boolean withoutSorting;
    protected boolean returnIdsOnly;
    protected List<HistoricProcessInstanceQueryImpl> orQueryObjects = new ArrayList<>();
    protected HistoricProcessInstanceQueryImpl currentOrQueryObject;
    protected boolean inOrStatement;

    public HistoricProcessInstanceQueryImpl() {
    }

    public HistoricProcessInstanceQueryImpl(CommandContext commandContext, ProcessEngineConfigurationImpl processEngineConfiguration) {
        super(commandContext, processEngineConfiguration.getVariableServiceConfiguration());
        this.processEngineConfiguration = processEngineConfiguration;
    }

    public HistoricProcessInstanceQueryImpl(CommandExecutor commandExecutor, ProcessEngineConfigurationImpl processEngineConfiguration) {
        super(commandExecutor, processEngineConfiguration.getVariableServiceConfiguration());
        this.processEngineConfiguration = processEngineConfiguration;
    }

    @Override
    public HistoricProcessInstanceQueryImpl processInstanceId(String processInstanceId) {
        if (inOrStatement) {
            this.currentOrQueryObject.processInstanceId = processInstanceId;
        } else {
            this.processInstanceId = processInstanceId;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery processInstanceIds(Set<String> processInstanceIds) {
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
    public HistoricProcessInstanceQueryImpl processDefinitionId(String processDefinitionId) {
        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionId = processDefinitionId;
        } else {
            this.processDefinitionId = processDefinitionId;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery processDefinitionKey(String processDefinitionKey) {
        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionKey = processDefinitionKey;
        } else {
            this.processDefinitionKey = processDefinitionKey;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery processDefinitionKeyIn(List<String> processDefinitionKeys) {
        if (inOrStatement) {
            currentOrQueryObject.processDefinitionKeyIn = processDefinitionKeys;
        } else {
            this.processDefinitionKeyIn = processDefinitionKeys;
        }
        return this;
    }
    
    @Override
    public HistoricProcessInstanceQuery excludeProcessDefinitionKeys(List<String> excludeProcessDefinitionKeys) {
        if (inOrStatement) {
            currentOrQueryObject.excludeProcessDefinitionKeys = excludeProcessDefinitionKeys;
        } else {
            this.excludeProcessDefinitionKeys = excludeProcessDefinitionKeys;
        }
        return this;
    }
    
    @Override
    public HistoricProcessInstanceQuery processDefinitionKeyLike(String processDefinitionKeyLike) {
        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionKeyLike = processDefinitionKeyLike;
        } else {
            this.processDefinitionKeyLike = processDefinitionKeyLike;
        }
        return this;
    }
    
    @Override
    public HistoricProcessInstanceQuery processDefinitionKeyLikeIgnoreCase(String processDefinitionKeyLikeIgnoreCase) {
        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionKeyLikeIgnoreCase = processDefinitionKeyLikeIgnoreCase;
        } else {
            this.processDefinitionKeyLikeIgnoreCase = processDefinitionKeyLikeIgnoreCase;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery processDefinitionCategory(String processDefinitionCategory) {
        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionCategory = processDefinitionCategory;
        } else {
            this.processDefinitionCategory = processDefinitionCategory;
        }
        return this;
    }
    
    @Override
    public HistoricProcessInstanceQuery processDefinitionCategoryLike(String processDefinitionCategoryLike) {
        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionCategoryLike = processDefinitionCategoryLike;
        } else {
            this.processDefinitionCategoryLike = processDefinitionCategoryLike;
        }
        return this;
    }
    
    @Override
    public HistoricProcessInstanceQuery processDefinitionCategoryLikeIgnoreCase(String processDefinitionCategoryLikeIgnoreCase) {
        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionCategoryLikeIgnoreCase = processDefinitionCategoryLikeIgnoreCase;
        } else {
            this.processDefinitionCategoryLikeIgnoreCase = processDefinitionCategoryLikeIgnoreCase;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery processDefinitionName(String processDefinitionName) {
        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionName = processDefinitionName;
        } else {
            this.processDefinitionName = processDefinitionName;
        }
        return this;
    }
    
    @Override
    public HistoricProcessInstanceQuery processDefinitionNameLike(String processDefinitionNameLike) {
        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionNameLike = processDefinitionNameLike;
        } else {
            this.processDefinitionNameLike = processDefinitionNameLike;
        }
        return this;
    }
    
    @Override
    public HistoricProcessInstanceQuery processDefinitionNameLikeIgnoreCase(String processDefinitionNameLikeIgnoreCase) {
        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionNameLikeIgnoreCase = processDefinitionNameLikeIgnoreCase;
        } else {
            this.processDefinitionNameLikeIgnoreCase = processDefinitionNameLikeIgnoreCase;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery processDefinitionVersion(Integer processDefinitionVersion) {
        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionVersion = processDefinitionVersion;
        } else {
            this.processDefinitionVersion = processDefinitionVersion;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery processInstanceBusinessKey(String businessKey) {
        if (inOrStatement) {
            this.currentOrQueryObject.businessKey = businessKey;
        } else {
            this.businessKey = businessKey;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery processInstanceBusinessKeyLike(String businessKeyLike) {
        if (inOrStatement) {
            this.currentOrQueryObject.businessKeyLike = businessKeyLike;
        } else {
            this.businessKeyLike = businessKeyLike;
        }
        return this;
    }
    
    @Override
    public HistoricProcessInstanceQuery processInstanceBusinessKeyLikeIgnoreCase(String businessKeyLikeIgnoreCase) {
        if (inOrStatement) {
            this.currentOrQueryObject.businessKeyLikeIgnoreCase = businessKeyLikeIgnoreCase;
        } else {
            this.businessKeyLikeIgnoreCase = businessKeyLikeIgnoreCase;
        }
        return this;
    }
    
    @Override
    public HistoricProcessInstanceQuery processInstanceBusinessStatus(String businessStatus) {
        if (inOrStatement) {
            this.currentOrQueryObject.businessStatus = businessStatus;
        } else {
            this.businessStatus = businessStatus;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery processInstanceBusinessStatusLike(String businessStatusLike) {
        if (inOrStatement) {
            this.currentOrQueryObject.businessStatusLike = businessStatusLike;
        } else {
            this.businessStatusLike = businessStatusLike;
        }
        return this;
    }
    
    @Override
    public HistoricProcessInstanceQuery processInstanceBusinessStatusLikeIgnoreCase(String businessStatusLikeIgnoreCase) {
        if (inOrStatement) {
            this.currentOrQueryObject.businessStatusLikeIgnoreCase = businessStatusLikeIgnoreCase;
        } else {
            this.businessStatusLikeIgnoreCase = businessStatusLikeIgnoreCase;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery processInstanceRootScopeId(String rootScopeId) {
        if (inOrStatement) {
            this.currentOrQueryObject.rootScopeId = rootScopeId;
        } else {
            this.rootScopeId = rootScopeId;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery processInstanceParentScopeId(String parentId) {
        if (inOrStatement) {
            this.currentOrQueryObject.parentScopeId = parentId;
        } else {
            this.parentScopeId = parentId;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery deploymentId(String deploymentId) {
        if (inOrStatement) {
            this.currentOrQueryObject.deploymentId = deploymentId;
        } else {
            this.deploymentId = deploymentId;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery deploymentIdIn(List<String> deploymentIds) {
        if (inOrStatement) {
            currentOrQueryObject.deploymentIds = deploymentIds;
        } else {
            this.deploymentIds = deploymentIds;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery finished() {
        if (inOrStatement) {
            this.currentOrQueryObject.finished = true;
        } else {
            this.finished = true;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery unfinished() {
        if (inOrStatement) {
            this.currentOrQueryObject.unfinished = true;
        } else {
            this.unfinished = true;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery deleted() {
        if (inOrStatement) {
            this.currentOrQueryObject.deleted = true;
        } else {
            this.deleted = true;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery notDeleted() {
        if (inOrStatement) {
            this.currentOrQueryObject.notDeleted = true;
        } else {
            this.notDeleted = true;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery startedBy(String startedBy) {
        if (inOrStatement) {
            this.currentOrQueryObject.startedBy = startedBy;
        } else {
            this.startedBy = startedBy;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery finishedBy(String finishedBy) {
        if (inOrStatement) {
            this.currentOrQueryObject.finishedBy = finishedBy;
        } else {
            this.finishedBy = finishedBy;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery state(String state) {
        if (inOrStatement) {
            this.currentOrQueryObject.state = state;
        } else {
            this.state = state;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery processDefinitionKeyNotIn(List<String> processDefinitionKeys) {
        if (inOrStatement) {
            this.currentOrQueryObject.processKeyNotIn = processDefinitionKeys;
        } else {
            this.processKeyNotIn = processDefinitionKeys;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery startedAfter(Date startedAfter) {
        if (inOrStatement) {
            this.currentOrQueryObject.startedAfter = startedAfter;
        } else {
            this.startedAfter = startedAfter;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery startedBefore(Date startedBefore) {
        if (inOrStatement) {
            this.currentOrQueryObject.startedBefore = startedBefore;
        } else {
            this.startedBefore = startedBefore;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery finishedAfter(Date finishedAfter) {
        if (inOrStatement) {
            this.currentOrQueryObject.finishedAfter = finishedAfter;
        } else {
            this.finishedAfter = finishedAfter;
            this.finished = true;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery finishedBefore(Date finishedBefore) {
        if (inOrStatement) {
            this.currentOrQueryObject.finishedBefore = finishedBefore;
        } else {
            this.finishedBefore = finishedBefore;
            this.finished = true;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery superProcessInstanceId(String superProcessInstanceId) {
        if (inOrStatement) {
            this.currentOrQueryObject.superProcessInstanceId = superProcessInstanceId;
        } else {
            this.superProcessInstanceId = superProcessInstanceId;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery excludeSubprocesses(boolean excludeSubprocesses) {
        if (inOrStatement) {
            this.currentOrQueryObject.excludeSubprocesses = excludeSubprocesses;
        } else {
            this.excludeSubprocesses = excludeSubprocesses;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery activeActivityId(String activityId) {
        if (inOrStatement) {
            this.currentOrQueryObject.activeActivityId = activityId;
        } else {
            this.activeActivityId = activityId;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery activeActivityIds(Set<String> activityIds) {
        if (inOrStatement) {
            this.currentOrQueryObject.activeActivityIds = activityIds;
        } else {
            this.activeActivityIds = activityIds;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery involvedUser(String involvedUser) {
        if (inOrStatement) {
            this.currentOrQueryObject.involvedUser = involvedUser;
        } else {
            this.involvedUser = involvedUser;
        }
        return this;
    }
    
    @Override
    public HistoricProcessInstanceQuery involvedUser(String userId, String identityLinkType) {
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
    public HistoricProcessInstanceQuery involvedGroup(String groupId, String identityLinkType) {
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
    public HistoricProcessInstanceQuery involvedGroups(Set<String> involvedGroups) {
        if (involvedGroups == null) {
            throw new FlowableIllegalArgumentException("involvedGroups are null");
        }
        if (involvedGroups.isEmpty()) {
            throw new FlowableIllegalArgumentException("involvedGroups are empty");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.involvedGroups = involvedGroups;
        } else {
            this.involvedGroups = involvedGroups;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery includeProcessVariables() {
        this.includeProcessVariables = true;
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery includeProcessVariables(Collection<String> variableNames) {
        if (variableNames == null || variableNames.isEmpty()) {
            throw new FlowableIllegalArgumentException("variableNames are null or empty");
        }
        includeProcessVariables();
        this.variableNamesToInclude = new LinkedHashSet<>(variableNames);
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery withJobException() {
        if (inOrStatement) {
            this.currentOrQueryObject.withJobException = true;
        } else {
            this.withJobException = true;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery processInstanceTenantId(String tenantId) {
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
    public HistoricProcessInstanceQuery processInstanceTenantIdLike(String tenantIdLike) {
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
    public HistoricProcessInstanceQuery processInstanceTenantIdLikeIgnoreCase(String tenantIdLikeIgnoreCase) {
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
    public HistoricProcessInstanceQuery processInstanceWithoutTenantId() {
        if (inOrStatement) {
            this.currentOrQueryObject.withoutTenantId = true;
        } else {
            this.withoutTenantId = true;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery processInstanceName(String name) {
        if (inOrStatement) {
            this.currentOrQueryObject.name = name;
        } else {
            this.name = name;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery processInstanceNameLike(String nameLike) {
        if (inOrStatement) {
            this.currentOrQueryObject.nameLike = nameLike;
        } else {
            this.nameLike = nameLike;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery processInstanceNameLikeIgnoreCase(String nameLikeIgnoreCase) {
        if (inOrStatement) {
            this.currentOrQueryObject.nameLikeIgnoreCase = nameLikeIgnoreCase.toLowerCase();
        } else {
            this.nameLikeIgnoreCase = nameLikeIgnoreCase.toLowerCase();
        }
        return this;
    }
    
    @Override
    public HistoricProcessInstanceQuery processInstanceCallbackId(String callbackId) {
        if (inOrStatement) {
            currentOrQueryObject.callbackId = callbackId;
        } else {
            this.callbackId = callbackId;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery processInstanceCallbackIds(Set<String> callbackIds) {
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
    public HistoricProcessInstanceQuery processInstanceCallbackType(String callbackType) {
        if (inOrStatement) {
            currentOrQueryObject.callbackType = callbackType;
        } else {
            this.callbackType = callbackType;
        }
        return this;
    }
    
    @Override
    public HistoricProcessInstanceQuery parentCaseInstanceId(String parentCaseInstanceId) {
        if (inOrStatement) {
            currentOrQueryObject.parentCaseInstanceId = parentCaseInstanceId;
        } else {
            this.parentCaseInstanceId = parentCaseInstanceId;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery withoutProcessInstanceCallbackId() {
        if (inOrStatement) {
            currentOrQueryObject.withoutCallbackId = true;
        } else {
            this.withoutCallbackId = true;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery processInstanceReferenceId(String referenceId) {
        if (inOrStatement) {
            currentOrQueryObject.referenceId = referenceId;
        } else {
            this.referenceId = referenceId;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery processInstanceReferenceType(String referenceType) {
        if (inOrStatement) {
            currentOrQueryObject.referenceType = referenceType;
        } else {
            this.referenceType = referenceType;
        }
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery variableValueEquals(String variableName, Object variableValue) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueEquals(variableName, variableValue, false);
            return this;
        } else {
            return variableValueEquals(variableName, variableValue, false);
        }
    }
    
    @Override
    public HistoricProcessInstanceQuery localVariableValueEquals(String variableName, Object variableValue) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueEquals(variableName, variableValue, true);
            return this;
        } else {
            return variableValueEquals(variableName, variableValue, true);
        }
    }

    @Override
    public HistoricProcessInstanceQuery variableValueNotEquals(String variableName, Object variableValue) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueNotEquals(variableName, variableValue, false);
            return this;
        } else {
            return variableValueNotEquals(variableName, variableValue, false);
        }
    }
    
    @Override
    public HistoricProcessInstanceQuery localVariableValueNotEquals(String variableName, Object variableValue) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueNotEquals(variableName, variableValue, true);
            return this;
        } else {
            return variableValueNotEquals(variableName, variableValue, true);
        }
    }

    @Override
    public HistoricProcessInstanceQuery variableValueEquals(Object variableValue) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueEquals(variableValue, false);
            return this;
        } else {
            return variableValueEquals(variableValue, false);
        }
    }
    
    @Override
    public HistoricProcessInstanceQuery localVariableValueEquals(Object variableValue) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueEquals(variableValue, true);
            return this;
        } else {
            return variableValueEquals(variableValue, true);
        }
    }

    @Override
    public HistoricProcessInstanceQuery variableValueEqualsIgnoreCase(String name, String value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueEqualsIgnoreCase(name, value, false);
            return this;
        } else {
            return variableValueEqualsIgnoreCase(name, value, false);
        }
    }
    
    @Override
    public HistoricProcessInstanceQuery localVariableValueEqualsIgnoreCase(String name, String value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueEqualsIgnoreCase(name, value, true);
            return this;
        } else {
            return variableValueEqualsIgnoreCase(name, value, true);
        }
    }

    @Override
    public HistoricProcessInstanceQuery variableValueNotEqualsIgnoreCase(String name, String value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueNotEqualsIgnoreCase(name, value, false);
            return this;
        } else {
            return variableValueNotEqualsIgnoreCase(name, value, false);
        }
    }
    
    @Override
    public HistoricProcessInstanceQuery localVariableValueNotEqualsIgnoreCase(String name, String value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueNotEqualsIgnoreCase(name, value, true);
            return this;
        } else {
            return variableValueNotEqualsIgnoreCase(name, value, true);
        }
    }
    
    @Override
    public HistoricProcessInstanceQuery variableValueLikeIgnoreCase(String name, String value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueLikeIgnoreCase(name, value, false);
            return this;
        } else {
            return variableValueLikeIgnoreCase(name, value, false);
        }
    }
    
    @Override
    public HistoricProcessInstanceQuery localVariableValueLikeIgnoreCase(String name, String value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueLikeIgnoreCase(name, value, true);
            return this;
        } else {
            return variableValueLikeIgnoreCase(name, value, true);
        }
    }

    @Override
    public HistoricProcessInstanceQuery variableValueGreaterThan(String name, Object value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueGreaterThan(name, value, false);
            return this;
        } else {
            return variableValueGreaterThan(name, value, false);
        }
    }
    
    @Override
    public HistoricProcessInstanceQuery localVariableValueGreaterThan(String name, Object value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueGreaterThan(name, value, true);
            return this;
        } else {
            return variableValueGreaterThan(name, value, true);
        }
    }

    @Override
    public HistoricProcessInstanceQuery variableValueGreaterThanOrEqual(String name, Object value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueGreaterThanOrEqual(name, value, false);
            return this;
        } else {
            return variableValueGreaterThanOrEqual(name, value, false);
        }
    }
    
    @Override
    public HistoricProcessInstanceQuery localVariableValueGreaterThanOrEqual(String name, Object value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueGreaterThanOrEqual(name, value, true);
            return this;
        } else {
            return variableValueGreaterThanOrEqual(name, value, true);
        }
    }

    @Override
    public HistoricProcessInstanceQuery variableValueLessThan(String name, Object value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueLessThan(name, value, false);
            return this;
        } else {
            return variableValueLessThan(name, value, false);
        }
    }
    
    @Override
    public HistoricProcessInstanceQuery localVariableValueLessThan(String name, Object value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueLessThan(name, value, true);
            return this;
        } else {
            return variableValueLessThan(name, value, true);
        }
    }

    @Override
    public HistoricProcessInstanceQuery variableValueLessThanOrEqual(String name, Object value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueLessThanOrEqual(name, value, false);
            return this;
        } else {
            return variableValueLessThanOrEqual(name, value, false);
        }
    }
    
    @Override
    public HistoricProcessInstanceQuery localVariableValueLessThanOrEqual(String name, Object value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueLessThanOrEqual(name, value, true);
            return this;
        } else {
            return variableValueLessThanOrEqual(name, value, true);
        }
    }

    @Override
    public HistoricProcessInstanceQuery variableValueLike(String name, String value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueLike(name, value, false);
            return this;
        } else {
            return variableValueLike(name, value, false);
        }
    }
    
    @Override
    public HistoricProcessInstanceQuery localVariableValueLike(String name, String value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueLike(name, value, true);
            return this;
        } else {
            return variableValueLike(name, value, true);
        }
    }

    @Override
    public HistoricProcessInstanceQuery variableExists(String name) {
        if (inOrStatement) {
            currentOrQueryObject.variableExists(name, false);
            return this;
        } else {
            return variableExists(name, false);
        }
    }
    
    @Override
    public HistoricProcessInstanceQuery localVariableExists(String name) {
        if (inOrStatement) {
            currentOrQueryObject.variableExists(name, true);
            return this;
        } else {
            return variableExists(name, true);
        }
    }
    
    @Override
    public HistoricProcessInstanceQuery variableNotExists(String name) {
        if (inOrStatement) {
            currentOrQueryObject.variableNotExists(name, false);
            return this;
        } else {
            return variableNotExists(name, false);
        }
    }
    
    @Override
    public HistoricProcessInstanceQuery localVariableNotExists(String name) {
        if (inOrStatement) {
            currentOrQueryObject.variableNotExists(name, true);
            return this;
        } else {
            return variableNotExists(name, true);
        }
    }
    
    @Override
    public HistoricProcessInstanceQuery locale(String locale) {
        this.locale = locale;
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery withLocalizationFallback() {
        this.withLocalizationFallback = true;
        return this;
    }
    
    @Override
    public HistoricProcessInstanceQuery withoutSorting() {
        this.withoutSorting = true;
        return this;
    }
    
    @Override
    public HistoricProcessInstanceQuery returnIdsOnly() {
        this.returnIdsOnly = true;
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery or() {
        if (inOrStatement) {
            throw new FlowableException("the query is already in an or statement");
        }

        inOrStatement = true;
        if (commandContext != null) {
            currentOrQueryObject = new HistoricProcessInstanceQueryImpl(commandContext, processEngineConfiguration);
        } else {
            currentOrQueryObject = new HistoricProcessInstanceQueryImpl(commandExecutor, processEngineConfiguration);
        }
        orQueryObjects.add(currentOrQueryObject);
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery endOr() {
        if (!inOrStatement) {
            throw new FlowableException("endOr() can only be called after calling or()");
        }

        inOrStatement = false;
        currentOrQueryObject = null;
        return this;
    }

    @Override
    public HistoricProcessInstanceQuery orderByProcessInstanceBusinessKey() {
        return orderBy(HistoricProcessInstanceQueryProperty.BUSINESS_KEY);
    }

    @Override
    public HistoricProcessInstanceQuery orderByProcessInstanceDuration() {
        return orderBy(HistoricProcessInstanceQueryProperty.DURATION);
    }

    @Override
    public HistoricProcessInstanceQuery orderByProcessInstanceStartTime() {
        return orderBy(HistoricProcessInstanceQueryProperty.START_TIME);
    }

    @Override
    public HistoricProcessInstanceQuery orderByProcessInstanceEndTime() {
        return orderBy(HistoricProcessInstanceQueryProperty.END_TIME);
    }

    @Override
    public HistoricProcessInstanceQuery orderByProcessDefinitionId() {
        return orderBy(HistoricProcessInstanceQueryProperty.PROCESS_DEFINITION_ID);
    }

    @Override
    public HistoricProcessInstanceQuery orderByProcessInstanceId() {
        return orderBy(HistoricProcessInstanceQueryProperty.PROCESS_INSTANCE_ID_);
    }

    @Override
    public HistoricProcessInstanceQuery orderByTenantId() {
        return orderBy(HistoricProcessInstanceQueryProperty.TENANT_ID);
    }

    @Override
    public long executeCount(CommandContext commandContext) {
        ensureVariablesInitialized();
        
        if (processEngineConfiguration.getHistoricProcessInstanceQueryInterceptor() != null) {
            processEngineConfiguration.getHistoricProcessInstanceQueryInterceptor().beforeHistoricProcessInstanceQueryExecute(this);
        }
        
        return CommandContextUtil.getHistoricProcessInstanceEntityManager(commandContext).findHistoricProcessInstanceCountByQueryCriteria(this);
    }

    @Override
    public List<HistoricProcessInstance> executeList(CommandContext commandContext) {
        ensureVariablesInitialized();
        
        if (withoutSorting) {
            setIgnoreOrderBy();
        }
        
        List<HistoricProcessInstance> results = null;
        
        if (processEngineConfiguration.getHistoricProcessInstanceQueryInterceptor() != null) {
            processEngineConfiguration.getHistoricProcessInstanceQueryInterceptor().beforeHistoricProcessInstanceQueryExecute(this);
        }
        
        if (returnIdsOnly) {
            results = processEngineConfiguration.getHistoricProcessInstanceEntityManager().findHistoricProcessInstanceIdsByQueryCriteria(this);
            
        } else if (includeProcessVariables) {
            results = processEngineConfiguration.getHistoricProcessInstanceEntityManager().findHistoricProcessInstancesAndVariablesByQueryCriteria(this);

            if (processInstanceId != null) {
                addCachedVariableForQueryById(commandContext, results);
            }

        } else {
            results = processEngineConfiguration.getHistoricProcessInstanceEntityManager().findHistoricProcessInstancesByQueryCriteria(this);
        }

        if (processEngineConfiguration.getPerformanceSettings().isEnableLocalization() && processEngineConfiguration.getInternalProcessLocalizationManager() != null) {
            for (HistoricProcessInstance processInstance : results) {
                processEngineConfiguration.getInternalProcessLocalizationManager().localize(processInstance, locale, withLocalizationFallback);
            }
        }
        
        if (processEngineConfiguration.getHistoricProcessInstanceQueryInterceptor() != null) {
            processEngineConfiguration.getHistoricProcessInstanceQueryInterceptor().afterHistoricProcessInstanceQueryExecute(this, results);
        }

        return results;
    }

    protected void addCachedVariableForQueryById(CommandContext commandContext, List<HistoricProcessInstance> results) {

        // Unlike the ExecutionEntityImpl, variables are not stored on the HistoricExecutionEntityImpl.
        // The solution for the non-historical entity is to use the variable cache on the entity, inspect the variables
        // of the current transaction and add them if necessary.
        // For the historical entity, we need to detect this use case specifically (i.e. byId is used) and check the entityCache.

        for (HistoricProcessInstance historicProcessInstance : results) {
            if (Objects.equals(processInstanceId, historicProcessInstance.getId())) {

                EntityCache entityCache = commandContext.getSession(EntityCache.class);
                List<HistoricVariableInstanceEntity> cachedVariableEntities = entityCache.findInCache(HistoricVariableInstanceEntity.class);
                for (HistoricVariableInstanceEntity cachedVariableEntity : cachedVariableEntities) {

                    if (historicProcessInstance.getId().equals(cachedVariableEntity.getProcessInstanceId())) {

                        // Variables from the cache have precedence
                        ((HistoricProcessInstanceEntity) historicProcessInstance).getQueryVariables().add(cachedVariableEntity);

                    }

                }

            }
        }
    }

    @Override
    public void enhanceCachedValue(HistoricProcessInstanceEntity processInstance) {
        if (includeProcessVariables) {
            if (variableNamesToInclude == null) {
                processInstance.getQueryVariables()
                        .addAll(processEngineConfiguration.getVariableServiceConfiguration().getHistoricVariableInstanceEntityManager()
                                .findHistoricalVariableInstancesByProcessInstanceId(processInstance.getId()));
            } else {
                processInstance.getQueryVariables()
                        .addAll(processEngineConfiguration.getVariableServiceConfiguration().getHistoricVariableInstanceEntityManager()
                                .findHistoricalVariableInstancesByProcessInstanceId(processInstance.getId(), variableNamesToInclude));
            }
        }
    }

    @Override
    protected void ensureVariablesInitialized() {
        super.ensureVariablesInitialized();

        for (HistoricProcessInstanceQueryImpl orQueryObject : orQueryObjects) {
            orQueryObject.ensureVariablesInitialized();
        }
    }

    @Override
    public void delete() {
        if (commandExecutor != null) {
            commandExecutor.execute(new DeleteHistoricProcessInstancesCmd(this));
        } else {
            new DeleteHistoricProcessInstancesCmd(this).execute(Context.getCommandContext());
        }
    }

    @Override
    @Deprecated
    public void deleteWithRelatedData() {
        if (commandExecutor != null) {
            CommandConfig config = new CommandConfig().transactionRequiresNew();
            commandExecutor.execute(config, new DeleteHistoricProcessInstancesCmd(this));
            commandExecutor.execute(config, new DeleteTaskAndActivityDataOfRemovedHistoricProcessInstancesCmd());
            commandExecutor.execute(config, new DeleteRelatedDataOfRemovedHistoricProcessInstancesCmd());
        } else {
            throw new FlowableException("deleting historic process instances with related data requires CommandExecutor");
        }
    }

    @Override
    public String deleteInParallelUsingBatch(int batchSize, String batchName) {
        return commandExecutor.execute(new DeleteHistoricProcessInstancesUsingBatchesCmd(this, batchSize, batchName, false));
    }

    @Override
    public String deleteSequentiallyUsingBatch(int batchSize, String batchName) {
        return commandExecutor.execute(new DeleteHistoricProcessInstancesUsingBatchesCmd(this, batchSize, batchName, true));
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

    public boolean isOpen() {
        return unfinished;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
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

    public List<String> getProcessDefinitionKeyIn() {
        return processDefinitionKeyIn;
    }

    public List<String> getExcludeProcessDefinitionKeys() {
        return excludeProcessDefinitionKeys;
    }

    public String getProcessDefinitionIdLike() {
        return processDefinitionKey + ":%:%";
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

    public String getProcessDefinitionCategory() {
        return processDefinitionCategory;
    }
    
    public String getProcessDefinitionCategoryLike() {
        return processDefinitionCategoryLike;
    }
    
    public String getProcessDefinitionCategoryLikeIgnoreCase() {
        return processDefinitionCategoryLikeIgnoreCase;
    }

    public Integer getProcessDefinitionVersion() {
        return processDefinitionVersion;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }
    
    @Override
    public String getId() {
        return processInstanceId;
    }

    public Set<String> getProcessInstanceIds() {
        return processInstanceIds;
    }

    public String getStartedBy() {
        return startedBy;
    }

    public String getFinishedBy() {
        return finishedBy;
    }

    public String getState() {
        return state;
    }

    public String getSuperProcessInstanceId() {
        return superProcessInstanceId;
    }

    public boolean isExcludeSubprocesses() {
        return excludeSubprocesses;
    }

    public List<String> getProcessKeyNotIn() {
        return processKeyNotIn;
    }

    public Date getStartedAfter() {
        return startedAfter;
    }

    public Date getStartedBefore() {
        return startedBefore;
    }

    public Date getFinishedAfter() {
        return finishedAfter;
    }

    public Date getFinishedBefore() {
        return finishedBefore;
    }

    public String getActiveActivityId() {
        return activeActivityId;
    }

    public Set<String> getActiveActivityIds() {
        return activeActivityIds;
    }

    public String getInvolvedUser() {
        return involvedUser;
    }

    public Set<String> getInvolvedGroups() {
        return involvedGroups;
    }

    public String getName() {
        return name;
    }

    public String getNameLike() {
        return nameLike;
    }
    
    public String getNameLikeIgnoreCase() {
        return nameLikeIgnoreCase;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public List<String> getDeploymentIds() {
        return deploymentIds;
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isUnfinished() {
        return unfinished;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public boolean isNotDeleted() {
        return notDeleted;
    }

    public boolean isIncludeProcessVariables() {
        return includeProcessVariables;
    }

    public Collection<String> getVariableNamesToInclude() {
        return variableNamesToInclude;
    }

    public boolean isWithException() {
        return withJobException;
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

    public boolean isWithoutCallbackId() {
        return withoutCallbackId;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public List<HistoricProcessInstanceQueryImpl> getOrQueryObjects() {
        return orQueryObjects;
    }

    public IdentityLinkQueryObject getInvolvedUserIdentityLink() {
        return involvedUserIdentityLink;
    }

    public IdentityLinkQueryObject getInvolvedGroupIdentityLink() {
        return involvedGroupIdentityLink;
    }

    public boolean isWithJobException() {
        return withJobException;
    }

    public String getLocale() {
        return locale;
    }

    public boolean isWithLocalizationFallback() {
        return withLocalizationFallback;
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

        return hasOrderByForColumn(HistoricProcessInstanceQueryProperty.PROCESS_DEFINITION_KEY.getName());
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

    public String getRootScopeId() {
        return rootScopeId;
    }

    public String getParentScopeId() {
        return parentScopeId;
    }
}
