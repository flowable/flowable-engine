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
package org.flowable.cmmn.engine.impl.history;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.history.HistoricCaseInstanceQuery;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.IdentityLinkQueryObject;
import org.flowable.cmmn.engine.impl.cmd.DeleteHistoricCaseInstancesCmd;
import org.flowable.cmmn.engine.impl.cmd.DeleteRelatedDataOfRemovedHistoricCaseInstancesCmd;
import org.flowable.cmmn.engine.impl.cmd.DeleteTaskAndPlanItemInstanceDataOfRemovedHistoricCaseInstancesCmd;
import org.flowable.cmmn.engine.impl.delete.DeleteHistoricCaseInstancesUsingBatchesCmd;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricCaseInstanceEntity;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.query.CacheAwareQuery;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandConfig;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.persistence.cache.EntityCache;
import org.flowable.variable.service.impl.AbstractVariableQueryImpl;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInstanceEntity;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class HistoricCaseInstanceQueryImpl extends AbstractVariableQueryImpl<HistoricCaseInstanceQuery, HistoricCaseInstance> 
        implements HistoricCaseInstanceQuery, CacheAwareQuery<HistoricCaseInstanceEntity> {

    private static final long serialVersionUID = 1L;
    
    protected CmmnEngineConfiguration cmmnEngineConfiguration;
    
    protected String caseDefinitionId;
    protected String caseDefinitionKey;
    protected String caseDefinitionKeyLike;
    protected String caseDefinitionKeyLikeIgnoreCase;
    protected Set<String> caseDefinitionKeys;
    protected Set<String> excludeCaseDefinitionKeys;
    protected Set<String> caseDefinitionIds;
    protected String caseDefinitionName;
    protected String caseDefinitionNameLike;
    protected String caseDefinitionNameLikeIgnoreCase;
    protected String caseDefinitionCategory;
    protected String caseDefinitionCategoryLike;
    protected String caseDefinitionCategoryLikeIgnoreCase;
    protected Integer caseDefinitionVersion;
    protected String caseInstanceId;
    protected Set<String> caseInstanceIds;
    private List<List<String>> safeCaseInstanceIds;
    protected String caseInstanceName;
    protected String caseInstanceNameLike;
    protected String caseInstanceNameLikeIgnoreCase;
    protected String rootScopeId;
    protected String parentScopeId;
    protected String businessKey;
    protected String businessKeyLike;
    protected String businessKeyLikeIgnoreCase;
    protected String businessStatus;
    protected String businessStatusLike;
    protected String businessStatusLikeIgnoreCase;
    protected String caseInstanceParentId;
    protected boolean withoutCaseInstanceParentId;
    protected String deploymentId;
    protected List<String> deploymentIds;
    protected boolean finished;
    protected boolean unfinished;
    protected Date startedBefore;
    protected Date startedAfter;
    protected Date finishedBefore;
    protected Date finishedAfter;
    protected String startedBy;
    protected String finishedBy;
    protected String state;
    protected Date lastReactivatedBefore;
    protected Date lastReactivatedAfter;
    protected String lastReactivatedBy;
    protected String callbackId;
    protected String callbackType;
    protected String parentCaseInstanceId;
    protected boolean withoutCallbackId;
    protected String referenceId;
    protected String referenceType;
    protected String tenantId;
    protected String tenantIdLike;
    protected String tenantIdLikeIgnoreCase;
    protected boolean withoutTenantId;
    protected boolean includeCaseVariables;
    protected Collection<String> variableNamesToInclude;
    protected String activePlanItemDefinitionId;
    protected Set<String> activePlanItemDefinitionIds;
    protected String involvedUser;
    protected IdentityLinkQueryObject involvedUserIdentityLink;
    protected Set<String> involvedGroups;
    private List<List<String>> safeInvolvedGroups;
    protected IdentityLinkQueryObject involvedGroupIdentityLink;
    protected List<HistoricCaseInstanceQueryImpl> orQueryObjects = new ArrayList<>();
    protected HistoricCaseInstanceQueryImpl currentOrQueryObject;
    protected boolean inOrStatement;
    protected String locale;
    protected boolean withLocalizationFallback;
    protected boolean withoutSorting;
    protected boolean returnIdsOnly;
    protected Set<String> callbackIds;

    public HistoricCaseInstanceQueryImpl() {
    }

    public HistoricCaseInstanceQueryImpl(CommandContext commandContext, CmmnEngineConfiguration cmmnEngineConfiguration) {
        super(commandContext, cmmnEngineConfiguration.getVariableServiceConfiguration());
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
    }

    public HistoricCaseInstanceQueryImpl(CommandExecutor commandExecutor, CmmnEngineConfiguration cmmnEngineConfiguration) {
        super(commandExecutor, cmmnEngineConfiguration.getVariableServiceConfiguration());
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
    }

    @Override
    protected void ensureVariablesInitialized() {
        super.ensureVariablesInitialized();

        for (HistoricCaseInstanceQueryImpl orQueryObject : orQueryObjects) {
            orQueryObject.ensureVariablesInitialized();
        }
    }

    @Override
    public HistoricCaseInstanceQueryImpl caseDefinitionId(String caseDefinitionId) {
        if (caseDefinitionId == null) {
            throw new FlowableIllegalArgumentException("Case definition id is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.caseDefinitionId = caseDefinitionId;
        } else {
            this.caseDefinitionId = caseDefinitionId;
        }
        return this;
    }

    @Override
    public HistoricCaseInstanceQuery caseDefinitionIds(Set<String> caseDefinitionIds) {
        if (caseDefinitionIds == null) {
            throw new FlowableIllegalArgumentException("Case definition ids is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.caseDefinitionIds = caseDefinitionIds;
        } else {
            this.caseDefinitionIds = caseDefinitionIds;
        }
        return this;
    }

    @Override
    public HistoricCaseInstanceQueryImpl caseDefinitionKey(String caseDefinitionKey) {
        if (caseDefinitionKey == null) {
            throw new FlowableIllegalArgumentException("Case definition key is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.caseDefinitionKey = caseDefinitionKey;
        } else {
            this.caseDefinitionKey = caseDefinitionKey;
        }
        return this;
    }
    
    @Override
    public HistoricCaseInstanceQueryImpl caseDefinitionKeyLike(String caseDefinitionKeyLike) {
        if (caseDefinitionKeyLike == null) {
            throw new FlowableIllegalArgumentException("Case definition key is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.caseDefinitionKeyLike = caseDefinitionKeyLike;
        } else {
            this.caseDefinitionKeyLike = caseDefinitionKeyLike;
        }
        return this;
    }
    
    @Override
    public HistoricCaseInstanceQueryImpl caseDefinitionKeyLikeIgnoreCase(String caseDefinitionKeyLikeIgnoreCase) {
        if (caseDefinitionKeyLikeIgnoreCase == null) {
            throw new FlowableIllegalArgumentException("Case definition key is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.caseDefinitionKeyLikeIgnoreCase = caseDefinitionKeyLikeIgnoreCase;
        } else {
            this.caseDefinitionKeyLikeIgnoreCase = caseDefinitionKeyLikeIgnoreCase;
        }
        return this;
    }

    @Override
    public HistoricCaseInstanceQueryImpl caseDefinitionCategory(String caseDefinitionCategory) {
        if (caseDefinitionCategory == null) {
            throw new FlowableIllegalArgumentException("Case definition category is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.caseDefinitionCategory = caseDefinitionCategory;
        } else {
            this.caseDefinitionCategory = caseDefinitionCategory;
        }
        return this;
    }
    
    @Override
    public HistoricCaseInstanceQueryImpl caseDefinitionCategoryLike(String caseDefinitionCategoryLike) {
        if (caseDefinitionCategoryLike == null) {
            throw new FlowableIllegalArgumentException("Case definition category is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.caseDefinitionCategoryLike = caseDefinitionCategoryLike;
        } else {
            this.caseDefinitionCategoryLike = caseDefinitionCategoryLike;
        }
        return this;
    }
    
    @Override
    public HistoricCaseInstanceQueryImpl caseDefinitionCategoryLikeIgnoreCase(String caseDefinitionCategoryLikeIgnoreCase) {
        if (caseDefinitionCategoryLikeIgnoreCase == null) {
            throw new FlowableIllegalArgumentException("Case definition category is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.caseDefinitionCategoryLikeIgnoreCase = caseDefinitionCategoryLikeIgnoreCase;
        } else {
            this.caseDefinitionCategoryLikeIgnoreCase = caseDefinitionCategoryLikeIgnoreCase;
        }
        return this;
    }

    @Override
    public HistoricCaseInstanceQueryImpl caseDefinitionName(String caseDefinitionName) {
        if (caseDefinitionName == null) {
            throw new FlowableIllegalArgumentException("Case definition name is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.caseDefinitionName = caseDefinitionName;
        } else {
            this.caseDefinitionName = caseDefinitionName;
        }
        return this;
    }
    
    @Override
    public HistoricCaseInstanceQueryImpl caseDefinitionNameLike(String caseDefinitionNameLike) {
        if (caseDefinitionNameLike == null) {
            throw new FlowableIllegalArgumentException("Case definition name is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.caseDefinitionNameLike = caseDefinitionNameLike;
        } else {
            this.caseDefinitionNameLike = caseDefinitionNameLike;
        }
        return this;
    }
    
    @Override
    public HistoricCaseInstanceQueryImpl caseDefinitionNameLikeIgnoreCase(String caseDefinitionNameLikeIgnoreCase) {
        if (caseDefinitionNameLikeIgnoreCase == null) {
            throw new FlowableIllegalArgumentException("Case definition name is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.caseDefinitionNameLikeIgnoreCase = caseDefinitionNameLikeIgnoreCase;
        } else {
            this.caseDefinitionNameLikeIgnoreCase = caseDefinitionNameLikeIgnoreCase;
        }
        return this;
    }

    @Override
    public HistoricCaseInstanceQueryImpl caseDefinitionVersion(Integer caseDefinitionVersion) {
        if (caseDefinitionVersion == null) {
            throw new FlowableIllegalArgumentException("Case definition version is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.caseDefinitionVersion = caseDefinitionVersion;
        } else {
            this.caseDefinitionVersion = caseDefinitionVersion;
        }
        return this;
    }

    @Override
    public HistoricCaseInstanceQueryImpl caseInstanceId(String caseInstanceId) {
        if (caseInstanceId == null) {
            throw new FlowableIllegalArgumentException("Case instance id is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.caseInstanceId = caseInstanceId;
        } else {
            this.caseInstanceId = caseInstanceId;
        }
        return this;
    }

    @Override
    public HistoricCaseInstanceQueryImpl caseInstanceIds(Set<String> caseInstanceIds) {
        if (caseInstanceIds == null) {
            throw new FlowableIllegalArgumentException("Case instance ids is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.caseInstanceIds = caseInstanceIds;
        } else {
            this.caseInstanceIds = caseInstanceIds;
        }
        return this;
    }
    
    @Override
    public HistoricCaseInstanceQueryImpl caseInstanceName(String name) {
        if (inOrStatement) {
            this.currentOrQueryObject.caseInstanceName = name;
        } else {
            this.caseInstanceName = name;
        }
        return this;
    }
    
    @Override
    public HistoricCaseInstanceQueryImpl caseInstanceNameLike(String nameLike) {
        if (inOrStatement) {
            this.currentOrQueryObject.caseInstanceNameLike = nameLike;
        } else {
            this.caseInstanceNameLike = nameLike;
        }
        return this;
    }
    
    @Override
    public HistoricCaseInstanceQueryImpl caseInstanceNameLikeIgnoreCase(String nameLikeIgnoreCase) {
        if (inOrStatement) {
            this.currentOrQueryObject.caseInstanceNameLikeIgnoreCase = nameLikeIgnoreCase;
        } else {
            this.caseInstanceNameLikeIgnoreCase = nameLikeIgnoreCase;
        }
        return this;
    }

    @Override
    public HistoricCaseInstanceQueryImpl caseInstanceBusinessKey(String businessKey) {
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
    public HistoricCaseInstanceQueryImpl caseInstanceBusinessKeyLike(String businessKeyLike) {
        if (businessKeyLike == null) {
            throw new FlowableIllegalArgumentException("Business key is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.businessKeyLike = businessKeyLike;
        } else {
            this.businessKeyLike = businessKeyLike;
        }
        return this;
    }
    
    @Override
    public HistoricCaseInstanceQueryImpl caseInstanceBusinessKeyLikeIgnoreCase(String businessKeyLikeIgnoreCase) {
        if (businessKeyLikeIgnoreCase == null) {
            throw new FlowableIllegalArgumentException("Business key is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.businessKeyLikeIgnoreCase = businessKeyLikeIgnoreCase;
        } else {
            this.businessKeyLikeIgnoreCase = businessKeyLikeIgnoreCase;
        }
        return this;
    }
    
    @Override
    public HistoricCaseInstanceQueryImpl caseInstanceRootScopeId(String rootScopeId) {
        if (rootScopeId == null) {
            throw new FlowableIllegalArgumentException("rootScopeId is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.rootScopeId = rootScopeId;
        } else {
            this.rootScopeId = rootScopeId;
        }
        return this;
    }

    @Override
    public HistoricCaseInstanceQueryImpl caseInstanceParentScopeId(String parentScopeId) {
        if (parentScopeId == null) {
            throw new FlowableIllegalArgumentException("parentScopeId is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.parentScopeId = parentScopeId;
        } else {
            this.parentScopeId = parentScopeId;
        }
        return this;
    }


    @Override
    public HistoricCaseInstanceQueryImpl caseInstanceBusinessStatus(String businessStatus) {
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
    public HistoricCaseInstanceQueryImpl caseInstanceBusinessStatusLike(String businessStatusLike) {
        if (businessStatusLike == null) {
            throw new FlowableIllegalArgumentException("Business status is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.businessStatusLike = businessStatusLike;
        } else {
            this.businessStatusLike = businessStatusLike;
        }
        return this;
    }
    
    @Override
    public HistoricCaseInstanceQueryImpl caseInstanceBusinessStatusLikeIgnoreCase(String businessStatusLikeIgnoreCase) {
        if (businessStatusLikeIgnoreCase == null) {
            throw new FlowableIllegalArgumentException("Business status is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.businessStatusLikeIgnoreCase = businessStatusLikeIgnoreCase;
        } else {
            this.businessStatusLikeIgnoreCase = businessStatusLikeIgnoreCase;
        }
        return this;
    }

    @Override
    public HistoricCaseInstanceQueryImpl caseDefinitionKeys(Set<String> caseDefinitionKeys) {
        if (caseDefinitionKeys == null) {
            throw new FlowableIllegalArgumentException("Case definition keys is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.caseDefinitionKeys = caseDefinitionKeys;
        } else {
            this.caseDefinitionKeys = caseDefinitionKeys;
        }
        return this;
    }
    
    @Override
    public HistoricCaseInstanceQueryImpl excludeCaseDefinitionKeys(Set<String> excludeCaseDefinitionKeys) {
        if (excludeCaseDefinitionKeys == null) {
            throw new FlowableIllegalArgumentException("Case definition keys is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.excludeCaseDefinitionKeys = excludeCaseDefinitionKeys;
        } else {
            this.excludeCaseDefinitionKeys = excludeCaseDefinitionKeys;
        }
        return this;
    }

    @Override
    public HistoricCaseInstanceQueryImpl caseInstanceParentId(String parentId) {
        if (parentId == null) {
            throw new FlowableIllegalArgumentException("Parent id is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.caseInstanceParentId = parentId;
        } else {
            this.caseInstanceParentId = parentId;
        }
        return this;
    }
    
    @Override
    public HistoricCaseInstanceQuery withoutCaseInstanceParent() {
        if (inOrStatement) {
            this.currentOrQueryObject.withoutCaseInstanceParentId = true;
        } else {
            this.withoutCaseInstanceParentId = true;
        }
        return this;
    }

    @Override
    public HistoricCaseInstanceQueryImpl deploymentId(String deploymentId) {
        if (deploymentId == null) {
            throw new FlowableIllegalArgumentException("Deployment id is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.deploymentId = deploymentId;
        } else {
            this.deploymentId = deploymentId;
        }
        return this;
    }
    
    @Override
    public HistoricCaseInstanceQueryImpl deploymentIds(List<String> deploymentIds) {
        if (deploymentIds == null) {
            throw new FlowableIllegalArgumentException("Deployment ids is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.deploymentIds = deploymentIds;
        } else {
            this.deploymentIds = deploymentIds;
        }
        return this;
    }
    
    @Override
    public HistoricCaseInstanceQueryImpl finished() {
        if (inOrStatement) {
            this.currentOrQueryObject.finished = true;
        } else {
            this.finished = true;
        }
        return this;
    }
    
    @Override
    public HistoricCaseInstanceQueryImpl unfinished() {
        if (inOrStatement) {
            this.currentOrQueryObject.unfinished = true;
        } else {
            this.unfinished = true;
        }
        return this;
    }
    
    @Override
    public HistoricCaseInstanceQueryImpl finishedBefore(Date beforeTime) {
        if (beforeTime == null) {
            throw new FlowableIllegalArgumentException("before time is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.finishedBefore = beforeTime;
        } else {
            this.finishedBefore = beforeTime;
        }
        return this;
    }
    
    @Override
    public HistoricCaseInstanceQueryImpl finishedAfter(Date afterTime) {
        if (afterTime == null) {
            throw new FlowableIllegalArgumentException("after time is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.finishedAfter = afterTime;
        } else {
            this.finishedAfter = afterTime;
        }
        return this;
    }

    @Override
    public HistoricCaseInstanceQueryImpl startedBefore(Date beforeTime) {
        if (beforeTime == null) {
            throw new FlowableIllegalArgumentException("before time is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.startedBefore = beforeTime;
        } else {
            this.startedBefore = beforeTime;
        }
        return this;
    }

    @Override
    public HistoricCaseInstanceQueryImpl startedAfter(Date afterTime) {
        if (afterTime == null) {
            throw new FlowableIllegalArgumentException("after time is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.startedAfter = afterTime;
        } else {
            this.startedAfter = afterTime;
        }

        return this;
    }

    @Override
    public HistoricCaseInstanceQueryImpl startedBy(String userId) {
        if (userId == null) {
            throw new FlowableIllegalArgumentException("user id is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.startedBy = userId;
        } else {
            this.startedBy = userId;
        }

        return this;
    }

    @Override
    public HistoricCaseInstanceQueryImpl finishedBy(String userId) {
        if (userId == null) {
            throw new FlowableIllegalArgumentException("user id is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.finishedBy = userId;
        } else {
            this.finishedBy = userId;
        }

        return this;
    }
    
    @Override
    public HistoricCaseInstanceQueryImpl state(String state) {
        if (state == null) {
            throw new FlowableIllegalArgumentException("state is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.state = state;
        } else {
            this.state = state;
        }

        return this;
    }

    @Override
    public HistoricCaseInstanceQuery lastReactivatedBefore(Date beforeTime) {
        if (beforeTime == null) {
            throw new FlowableIllegalArgumentException("before time is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.lastReactivatedBefore = beforeTime;
        } else {
            this.lastReactivatedBefore = beforeTime;
        }
        return this;
    }

    @Override
    public HistoricCaseInstanceQuery lastReactivatedAfter(Date afterTime) {
        if (afterTime == null) {
            throw new FlowableIllegalArgumentException("after time is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.lastReactivatedAfter = afterTime;
        } else {
            this.lastReactivatedAfter = afterTime;
        }

        return this;
    }

    @Override
    public HistoricCaseInstanceQuery lastReactivatedBy(String userId) {
        if (userId == null) {
            throw new FlowableIllegalArgumentException("user id is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.lastReactivatedBy = userId;
        } else {
            this.lastReactivatedBy = userId;
        }

        return this;
    }

    @Override
    public HistoricCaseInstanceQuery caseInstanceCallbackId(String callbackId) {
        if (callbackId == null) {
            throw new FlowableIllegalArgumentException("callback id is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.callbackId = callbackId;
        } else {
            this.callbackId = callbackId;
        }
        return this;
    }

    @Override
    public HistoricCaseInstanceQuery caseInstanceCallbackIds(Set<String> callbackIds) {
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
    public HistoricCaseInstanceQuery caseInstanceCallbackType(String callbackType) {
        if (callbackType == null) {
            throw new FlowableIllegalArgumentException("callback type is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.callbackType = callbackType;
        } else {
            this.callbackType = callbackType;
        }
        return this;
    }
    
    @Override
    public HistoricCaseInstanceQuery parentCaseInstanceId(String parentCaseInstanceId) {
        if (parentCaseInstanceId == null) {
            throw new FlowableIllegalArgumentException("parent case instance id is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.parentCaseInstanceId = parentCaseInstanceId;
        } else {
            this.parentCaseInstanceId = parentCaseInstanceId;
        }
        return this;
    }

    @Override
    public HistoricCaseInstanceQuery withoutCaseInstanceCallbackId() {
        if (inOrStatement) {
            this.currentOrQueryObject.withoutCallbackId = true;
        } else {
            this.withoutCallbackId = true;
        }
        return this;
    }

    @Override
    public HistoricCaseInstanceQuery caseInstanceReferenceId(String referenceId) {
        if (referenceId == null) {
            throw new FlowableIllegalArgumentException("referenceId is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.referenceId = referenceId;
        } else {
            this.referenceId = referenceId;
        }
        return this;
    }

    @Override
    public HistoricCaseInstanceQuery caseInstanceReferenceType(String referenceType) {
        if (referenceType == null) {
            throw new FlowableIllegalArgumentException("referenceType is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.referenceType = referenceType;
        } else {
            this.referenceType = referenceType;
        }
        return this;
    }

    @Override
    public HistoricCaseInstanceQueryImpl caseInstanceTenantId(String tenantId) {
        if (tenantId == null) {
            throw new FlowableIllegalArgumentException("caseInstance tenant id is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.tenantId = tenantId;
        } else {
            this.tenantId = tenantId;
        }
        return this;
    }
    
    @Override
    public HistoricCaseInstanceQueryImpl caseInstanceTenantIdLike(String tenantIdLike) {
        if (tenantIdLike == null) {
            throw new FlowableIllegalArgumentException("caseInstance tenant id is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.tenantIdLike = tenantIdLike;
        } else {
            this.tenantIdLike = tenantIdLike;
        }
        return this;
    }
    
    @Override
    public HistoricCaseInstanceQueryImpl caseInstanceTenantIdLikeIgnoreCase(String tenantIdLikeIgnoreCase) {
        if (tenantIdLikeIgnoreCase == null) {
            throw new FlowableIllegalArgumentException("caseInstance tenant id is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.tenantIdLikeIgnoreCase = tenantIdLikeIgnoreCase;
        } else {
            this.tenantIdLikeIgnoreCase = tenantIdLikeIgnoreCase;
        }
        return this;
    }

    @Override
    public HistoricCaseInstanceQueryImpl caseInstanceWithoutTenantId() {
        if (inOrStatement) {
            this.currentOrQueryObject.withoutTenantId = true;
        } else {
            this.withoutTenantId = true;
        }
        return this;
    }

    // ordering ////////////////////////////////////////////////////

    @Override
    public HistoricCaseInstanceQueryImpl orderByCaseInstanceId() {
        this.orderProperty = HistoricCaseInstanceQueryProperty.CASE_INSTANCE_ID;
        return this;
    }

    @Override
    public HistoricCaseInstanceQueryImpl orderByCaseInstanceName() {
        this.orderProperty = HistoricCaseInstanceQueryProperty.CASE_INSTANCE_NAME;
        return this;
    }

    @Override
    public HistoricCaseInstanceQueryImpl orderByCaseDefinitionId() {
        this.orderProperty = HistoricCaseInstanceQueryProperty.CASE_DEFINITION_ID;
        return this;
    }

    @Override
    public HistoricCaseInstanceQueryImpl orderByCaseDefinitionKey() {
        this.orderProperty = HistoricCaseInstanceQueryProperty.CASE_DEFINITION_KEY;
        return this;
    }
    
    @Override
    public HistoricCaseInstanceQueryImpl orderByStartTime() {
        this.orderProperty = HistoricCaseInstanceQueryProperty.CASE_START_TIME;
        return this;
    }
    
    @Override
    public HistoricCaseInstanceQueryImpl orderByEndTime() {
        this.orderProperty = HistoricCaseInstanceQueryProperty.CASE_END_TIME;
        return this;
    }

    @Override
    public HistoricCaseInstanceQueryImpl orderByTenantId() {
        this.orderProperty = HistoricCaseInstanceQueryProperty.TENANT_ID;
        return this;
    }

    // results ////////////////////////////////////////////////////

    @Override
    public long executeCount(CommandContext commandContext) {
        ensureVariablesInitialized();
        return cmmnEngineConfiguration.getHistoricCaseInstanceEntityManager().countByCriteria(this);
    }

    @Override
    public List<HistoricCaseInstance> executeList(CommandContext commandContext) {
        ensureVariablesInitialized();
        
        if (withoutSorting) {
            setIgnoreOrderBy();
        }
        
        List<HistoricCaseInstance> results;
        if (returnIdsOnly) {
            results = cmmnEngineConfiguration.getHistoricCaseInstanceEntityManager().findIdsByCriteria(this);
            
        } else if (includeCaseVariables) {
            results = cmmnEngineConfiguration.getHistoricCaseInstanceEntityManager().findWithVariablesByQueryCriteria(this);

            if (caseInstanceId != null) {
                addCachedVariableForQueryById(commandContext, results);
            }

        } else {
            results = cmmnEngineConfiguration.getHistoricCaseInstanceEntityManager().findByCriteria(this);
        }

        if (cmmnEngineConfiguration.getCaseLocalizationManager() != null) {
            for (HistoricCaseInstance historicCaseInstance : results) {
                cmmnEngineConfiguration.getCaseLocalizationManager().localize(historicCaseInstance, locale, withLocalizationFallback);
            }
        }

        return results;
    }

    protected void addCachedVariableForQueryById(CommandContext commandContext, List<HistoricCaseInstance> results) {

        // Unlike the CaseInstanceEntityImpl, variables are not stored on the HistoricCaseInstanceEntityImpl.
        // The solution for the non-historical entity is to use the variable cache on the entity, inspect the variables
        // of the current transaction and add them if necessary.
        // For the historical entity, we need to detect this use case specifically (i.e. byId is used) and check the entityCache.

        for (HistoricCaseInstance historicCaseInstance : results) {
            if (Objects.equals(caseInstanceId, historicCaseInstance.getId())) {

                EntityCache entityCache = commandContext.getSession(EntityCache.class);
                List<HistoricVariableInstanceEntity> cachedVariableEntities = entityCache.findInCache(HistoricVariableInstanceEntity.class);
                for (HistoricVariableInstanceEntity cachedVariableEntity : cachedVariableEntities) {

                    if (historicCaseInstance.getId().equals(cachedVariableEntity.getScopeId())
                            && ScopeTypes.CMMN.equals(cachedVariableEntity.getScopeType())) {

                        // Variables from the cache have precedence
                        ((HistoricCaseInstanceEntity) historicCaseInstance).getQueryVariables().add(cachedVariableEntity);

                    }

                }

            }
        }
    }

    @Override
    public void enhanceCachedValue(HistoricCaseInstanceEntity caseInstance) {
        if (isIncludeCaseVariables()) {
            if (variableNamesToInclude == null) {
                caseInstance.getQueryVariables()
                        .addAll(cmmnEngineConfiguration.getVariableServiceConfiguration().getHistoricVariableInstanceEntityManager()
                                .findHistoricalVariableInstancesByScopeIdAndScopeType(caseInstance.getId(), ScopeTypes.CMMN));
            } else {
                caseInstance.getQueryVariables()
                        .addAll(cmmnEngineConfiguration.getVariableServiceConfiguration().getHistoricVariableInstanceEntityManager()
                                .findHistoricalVariableInstancesByScopeIdAndScopeType(caseInstance.getId(), ScopeTypes.CMMN, variableNamesToInclude));
            }
        }
    }

    @Override
    public void delete() {
        if (commandExecutor != null) {
            commandExecutor.execute(new DeleteHistoricCaseInstancesCmd(this));
        } else {
            new DeleteHistoricCaseInstancesCmd(this).execute(Context.getCommandContext());
        }
    }

    @Override
    @Deprecated
    public void deleteWithRelatedData() {
        if (commandExecutor != null) {
            CommandConfig config = new CommandConfig().transactionRequiresNew();
            commandExecutor.execute(config, new DeleteHistoricCaseInstancesCmd(this));
            commandExecutor.execute(config, new DeleteTaskAndPlanItemInstanceDataOfRemovedHistoricCaseInstancesCmd());
            commandExecutor.execute(config, new DeleteRelatedDataOfRemovedHistoricCaseInstancesCmd());
        } else {
            throw new FlowableException("deleting historic case instances with related data requires CommandExecutor");
        }
    }

    @Override
    public String deleteInParallelUsingBatch(int batchSize, String batchName) {
        return commandExecutor.execute(new DeleteHistoricCaseInstancesUsingBatchesCmd(this, batchSize, batchName, false));
    }

    @Override
    public String deleteSequentiallyUsingBatch(int batchSize, String batchName) {
        return commandExecutor.execute(new DeleteHistoricCaseInstancesUsingBatchesCmd(this, batchSize, batchName, true));
    }

    @Override
    public HistoricCaseInstanceQuery includeCaseVariables() {
        this.includeCaseVariables = true;
        return this;
    }

    @Override
    public HistoricCaseInstanceQuery includeCaseVariables(Collection<String> variableNames) {
        if (variableNames == null || variableNames.isEmpty()) {
            throw new FlowableIllegalArgumentException("variableNames is null or empty");
        }
        includeCaseVariables();
        this.variableNamesToInclude = new LinkedHashSet<>(variableNames);
        return this;
    }

    @Override
    public HistoricCaseInstanceQuery activePlanItemDefinitionId(String planItemDefinitionId) {
        if (planItemDefinitionId == null) {
            throw new FlowableIllegalArgumentException("planItemDefinitionId is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.activePlanItemDefinitionId = planItemDefinitionId;
        } else {
            this.activePlanItemDefinitionId = planItemDefinitionId;
        }
        return this;
    }
    
    @Override
    public HistoricCaseInstanceQuery activePlanItemDefinitionIds(Set<String> planItemDefinitionIds) {
        if (planItemDefinitionIds == null) {
            throw new FlowableIllegalArgumentException("planItemDefinitionIds is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.activePlanItemDefinitionIds = planItemDefinitionIds;
        } else {
            this.activePlanItemDefinitionIds = planItemDefinitionIds;
        }
        return this;
    }

    @Override
    public HistoricCaseInstanceQuery involvedUser(String userId) {
        if (userId == null) {
            throw new FlowableIllegalArgumentException("involvedUser is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.involvedUser = userId;
        } else {
            this.involvedUser = userId;
        }
        return this;
    }
    
    @Override
    public HistoricCaseInstanceQuery involvedUser(String userId, String identityLinkType) {
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
    public HistoricCaseInstanceQuery involvedGroup(String groupId, String identityLinkType) {
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
    public HistoricCaseInstanceQuery involvedGroups(Set<String> groupIds) {
        if (groupIds == null) {
            throw new FlowableIllegalArgumentException("groupIds are null");
        }
        if (groupIds.isEmpty()) {
            throw new FlowableIllegalArgumentException("groupIds are empty");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.involvedGroups = groupIds;
        } else {
            this.involvedGroups = groupIds;
        }
        return this;
    }

    @Override
    public HistoricCaseInstanceQuery or() {
        if (inOrStatement) {
            throw new FlowableException("the query is already in an or statement");
        }

        inOrStatement = true;
        if (commandContext != null) {
            currentOrQueryObject = new HistoricCaseInstanceQueryImpl(commandContext, cmmnEngineConfiguration);
        } else {
            currentOrQueryObject = new HistoricCaseInstanceQueryImpl(commandExecutor, cmmnEngineConfiguration);
        }
        orQueryObjects.add(currentOrQueryObject);
        return this;
    }

    @Override
    public HistoricCaseInstanceQuery endOr() {
        if (!inOrStatement) {
            throw new FlowableException("endOr() can only be called after calling or()");
        }

        inOrStatement = false;
        currentOrQueryObject = null;
        return this;
    }

    @Override
    public HistoricCaseInstanceQuery variableValueEquals(String variableName, Object variableValue) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueEquals(variableName, variableValue, false);
            return this;
        } else {
            return variableValueEquals(variableName, variableValue, false);
        }
    }

    @Override
    public HistoricCaseInstanceQuery variableValueNotEquals(String variableName, Object variableValue) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueNotEquals(variableName, variableValue, false);
            return this;
        } else {
            return variableValueNotEquals(variableName, variableValue, false);
        }
    }

    @Override
    public HistoricCaseInstanceQuery variableValueEquals(Object variableValue) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueEquals(variableValue, false);
            return this;
        } else {
            return variableValueEquals(variableValue, false);
        }
    }

    @Override
    public HistoricCaseInstanceQuery variableValueEqualsIgnoreCase(String name, String value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueEqualsIgnoreCase(name, value, false);
            return this;
        } else {
            return variableValueEqualsIgnoreCase(name, value, false);
        }
    }

    @Override
    public HistoricCaseInstanceQuery variableValueNotEqualsIgnoreCase(String name, String value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueNotEqualsIgnoreCase(name, value, false);
            return this;
        } else {
            return variableValueNotEqualsIgnoreCase(name, value, false);
        }
    }

    @Override
    public HistoricCaseInstanceQuery variableValueLikeIgnoreCase(String name, String value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueLikeIgnoreCase(name, value, false);
            return this;
        } else {
            return variableValueLikeIgnoreCase(name, value, false);
        }
    }

    @Override
    public HistoricCaseInstanceQuery variableValueGreaterThan(String name, Object value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueGreaterThan(name, value, false);
            return this;
        } else {
            return variableValueGreaterThan(name, value, false);
        }
    }

    @Override
    public HistoricCaseInstanceQuery variableValueGreaterThanOrEqual(String name, Object value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueGreaterThanOrEqual(name, value, false);
            return this;
        } else {
            return variableValueGreaterThanOrEqual(name, value, false);
        }
    }

    @Override
    public HistoricCaseInstanceQuery variableValueLessThan(String name, Object value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueLessThan(name, value, false);
            return this;
        } else {
            return variableValueLessThan(name, value, false);
        }
    }

    @Override
    public HistoricCaseInstanceQuery variableValueLessThanOrEqual(String name, Object value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueLessThanOrEqual(name, value, false);
            return this;
        } else {
            return variableValueLessThanOrEqual(name, value, false);
        }
    }

    @Override
    public HistoricCaseInstanceQuery variableValueLike(String name, String value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueLike(name, value, false);
            return this;
        } else {
            return variableValueLike(name, value, false);
        }
    }

    @Override
    public HistoricCaseInstanceQuery variableExists(String name) {
        if (inOrStatement) {
            currentOrQueryObject.variableExists(name, false);
            return this;
        } else {
            return variableExists(name, false);
        }
    }

    @Override
    public HistoricCaseInstanceQuery variableNotExists(String name) {
        if (inOrStatement) {
            currentOrQueryObject.variableNotExists(name, false);
            return this;
        } else {
            return variableNotExists(name, false);
        }
    }

    @Override
    public HistoricCaseInstanceQuery locale(String locale) {
        this.locale = locale;
        return this;
    }

    @Override
    public HistoricCaseInstanceQuery withLocalizationFallback() {
        this.withLocalizationFallback = true;
        return this;
    }
    
    @Override
    public HistoricCaseInstanceQuery withoutSorting() {
        this.withoutSorting = true;
        return this;
    }
    
    @Override
    public HistoricCaseInstanceQuery returnIdsOnly() {
        this.returnIdsOnly = true;
        return this;
    }

    public String getCaseDefinitionId() {
        return caseDefinitionId;
    }

    public String getCaseDefinitionKey() {
        return caseDefinitionKey;
    }
    
    public String getCaseDefinitionKeyLike() {
        return caseDefinitionKeyLike;
    }
    
    public String getCaseDefinitionKeyLikeIgnoreCase() {
        return caseDefinitionKeyLikeIgnoreCase;
    }

    public Set<String> getCaseDefinitionKeys() {
        return caseDefinitionKeys;
    }
    
    public Set<String> getExcludeCaseDefinitionKeys() {
        return excludeCaseDefinitionKeys;
    }

    public Set<String> getCaseDefinitionIds() {
        return caseDefinitionIds;
    }

    public String getCaseDefinitionName() {
        return caseDefinitionName;
    }
    
    public String getCaseDefinitionNameLike() {
        return caseDefinitionNameLike;
    }
    
    public String getCaseDefinitionNameLikeIgnoreCase() {
        return caseDefinitionNameLikeIgnoreCase;
    }

    public String getCaseDefinitionCategory() {
        return caseDefinitionCategory;
    }
    
    public String getCaseDefinitionCategoryLike() {
        return caseDefinitionCategoryLike;
    }
    
    public String getCaseDefinitionCategoryLikeIgnoreCase() {
        return caseDefinitionCategoryLikeIgnoreCase;
    }

    public Integer getCaseDefinitionVersion() {
        return caseDefinitionVersion;
    }

    public String getCaseInstanceId() {
        return caseInstanceId;
    }
    
    @Override
    public String getId() {
        return caseInstanceId;
    }

    public String getCaseInstanceName() {
        return caseInstanceName;
    }

    public String getCaseInstanceNameLike() {
        return caseInstanceNameLike;
    }

    public String getCaseInstanceNameLikeIgnoreCase() {
        return caseInstanceNameLikeIgnoreCase;
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

    public String getCaseInstanceParentId() {
        return caseInstanceParentId;
    }

    public boolean isWithoutCaseInstanceParentId() {
        return withoutCaseInstanceParentId;
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isUnfinished() {
        return unfinished;
    }

    public Date getStartedBefore() {
        return startedBefore;
    }

    public Date getStartedAfter() {
        return startedAfter;
    }

    public Date getFinishedBefore() {
        return finishedBefore;
    }

    public Date getFinishedAfter() {
        return finishedAfter;
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

    public Date getLastReactivatedBefore() {
        return lastReactivatedBefore;
    }

    public Date getLastReactivatedAfter() {
        return lastReactivatedAfter;
    }

    public String getLastReactivatedBy() {
        return lastReactivatedBy;
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

    public String getActivePlanItemDefinitionId() {
        return activePlanItemDefinitionId;
    }

    public Set<String> getActivePlanItemDefinitionIds() {
        return activePlanItemDefinitionIds;
    }

    public String getInvolvedUser() {
        return involvedUser;
    }

    public IdentityLinkQueryObject getInvolvedUserIdentityLink() {
        return involvedUserIdentityLink;
    }

    public Set<String> getInvolvedGroups() {
        return involvedGroups;
    }
    
    public IdentityLinkQueryObject getInvolvedGroupIdentityLink() {
        return involvedGroupIdentityLink;
    }

    public boolean isIncludeCaseVariables() {
        return includeCaseVariables;
    }

    public Collection<String> getVariableNamesToInclude() {
        return variableNamesToInclude;
    }

    public List<HistoricCaseInstanceQueryImpl> getOrQueryObjects() {
        return orQueryObjects;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public List<String> getDeploymentIds() {
        return deploymentIds;
    }

    public Set<String> getCaseInstanceIds() {
        return caseInstanceIds;
    }

    public boolean isNeedsCaseDefinitionOuterJoin() {
        if (isNeedsPaging()) {
            if (AbstractEngineConfiguration.DATABASE_TYPE_ORACLE.equals(databaseType)
                    || AbstractEngineConfiguration.DATABASE_TYPE_DB2.equals(databaseType)
                    || AbstractEngineConfiguration.DATABASE_TYPE_MSSQL.equals(databaseType)) {
                // When using oracle, db2 or mssql we don't need outer join for the process definition join.
                // It is not needed because the outer join order by is done by the row number instead
                return false;
            }
        }

        return hasOrderByForColumn(HistoricCaseInstanceQueryProperty.CASE_DEFINITION_KEY.getName());
    }

    public List<List<String>> getSafeCaseInstanceIds() {
        return safeCaseInstanceIds;
    }

    public void setSafeCaseInstanceIds(List<List<String>> safeCaseInstanceIds) {
        this.safeCaseInstanceIds = safeCaseInstanceIds;
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