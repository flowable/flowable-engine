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
package org.flowable.cmmn.engine.impl.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstanceQuery;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.IdentityLinkQueryObject;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.query.CacheAwareQuery;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.variable.service.impl.AbstractVariableQueryImpl;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class CaseInstanceQueryImpl extends AbstractVariableQueryImpl<CaseInstanceQuery, CaseInstance>
        implements CaseInstanceQuery, CacheAwareQuery<CaseInstanceEntity> {

    private static final long serialVersionUID = 1L;
    
    protected CmmnEngineConfiguration cmmnEngineConfiguration;

    protected String caseDefinitionId;
    protected String caseDefinitionKey;
    protected String caseDefinitionKeyLike;
    protected String caseDefinitionKeyLikeIgnoreCase;
    protected Set<String> caseDefinitionKeys;
    protected Set<String> excludeCaseDefinitionKeys;
    protected Set<String> caseDefinitionIds;
    protected String caseDefinitionCategory;
    protected String caseDefinitionCategoryLike;
    protected String caseDefinitionCategoryLikeIgnoreCase;
    protected String caseDefinitionName;
    protected String caseDefinitionNameLike;
    protected String caseDefinitionNameLikeIgnoreCase;
    protected Integer caseDefinitionVersion;
    protected String name;
    protected String nameLike;
    protected String nameLikeIgnoreCase;
    protected String rootScopeId;
    protected String parentScopeId;
    protected String businessKey;
    protected String businessKeyLike;
    protected String businessKeyLikeIgnoreCase;
    protected String businessStatus;
    protected String businessStatusLike;
    protected String businessStatusLikeIgnoreCase;
    protected String caseInstanceId;
    protected Set<String> caseInstanceIds;
    private List<List<String>> safeCaseInstanceIds;
    protected String caseInstanceParentId;
    protected String caseInstanceParentPlanItemInstanceId;
    protected Date startedBefore;
    protected Date startedAfter;
    protected String startedBy;
    protected String state;
    protected Date lastReactivatedBefore;
    protected Date lastReactivatedAfter;
    protected String lastReactivatedBy;
    protected String callbackId;
    protected Set<String> callbackIds;
    protected String callbackType;
    protected String parentCaseInstanceId;
    protected String referenceId;
    protected String referenceType;
    protected boolean completeable;
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

    protected List<CaseInstanceQueryImpl> orQueryObjects = new ArrayList<>();
    protected CaseInstanceQueryImpl currentOrQueryObject;
    protected boolean inOrStatement;

    protected String locale;
    protected boolean withLocalizationFallback;

    public CaseInstanceQueryImpl() {
    }

    public CaseInstanceQueryImpl(CommandContext commandContext, CmmnEngineConfiguration cmmnEngineConfiguration) {
        super(commandContext, cmmnEngineConfiguration.getVariableServiceConfiguration());
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
    }

    public CaseInstanceQueryImpl(CommandExecutor commandExecutor, CmmnEngineConfiguration cmmnEngineConfiguration) {
        super(commandExecutor, cmmnEngineConfiguration.getVariableServiceConfiguration());
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
    }

    @Override
    protected void ensureVariablesInitialized() {
        super.ensureVariablesInitialized();

        for (CaseInstanceQueryImpl orQueryObject : orQueryObjects) {
            orQueryObject.ensureVariablesInitialized();
        }
    }

    @Override
    public CaseInstanceQueryImpl caseDefinitionId(String caseDefinitionId) {
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
    public CaseInstanceQueryImpl caseDefinitionIds(Set<String> caseDefinitionIds) {
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
    public CaseInstanceQueryImpl caseDefinitionKey(String caseDefinitionKey) {
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
    public CaseInstanceQueryImpl caseDefinitionKeyLike(String caseDefinitionKeyLike) {
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
    public CaseInstanceQueryImpl caseDefinitionKeyLikeIgnoreCase(String caseDefinitionKeyLikeIgnoreCase) {
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
    public CaseInstanceQueryImpl caseDefinitionCategory(String caseDefinitionCategory) {
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
    public CaseInstanceQueryImpl caseDefinitionCategoryLike(String caseDefinitionCategoryLike) {
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
    public CaseInstanceQueryImpl caseDefinitionCategoryLikeIgnoreCase(String caseDefinitionCategoryLikeIgnoreCase) {
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
    public CaseInstanceQueryImpl caseDefinitionName(String caseDefinitionName) {
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
    public CaseInstanceQueryImpl caseDefinitionNameLike(String caseDefinitionNameLike) {
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
    public CaseInstanceQueryImpl caseDefinitionNameLikeIgnoreCase(String caseDefinitionNameLikeIgnoreCase) {
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
    public CaseInstanceQueryImpl caseDefinitionVersion(Integer caseDefinitionVersion) {
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
    public CaseInstanceQueryImpl caseInstanceId(String caseInstanceId) {
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
    public CaseInstanceQueryImpl caseInstanceIds(Set<String> caseInstanceIds) {
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
    public CaseInstanceQuery caseInstanceName(String name) {
        if (name == null) {
            throw new FlowableIllegalArgumentException("Name is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.name = name;
        } else {
            this.name = name;
        }
        return this;
    }

    @Override
    public CaseInstanceQuery caseInstanceNameLike(String nameLike) {
        if (nameLike == null) {
            throw new FlowableIllegalArgumentException("Name like is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.nameLike = nameLike;
        } else {
            this.nameLike = nameLike;
        }
        return this;
    }

    @Override
    public CaseInstanceQuery caseInstanceNameLikeIgnoreCase(String nameLikeIgnoreCase) {
        if (nameLikeIgnoreCase == null) {
            throw new FlowableIllegalArgumentException("Name like ignore case is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.nameLikeIgnoreCase = nameLikeIgnoreCase;
        } else {
            this.nameLikeIgnoreCase = nameLikeIgnoreCase;
        }
        return this;
    }

    @Override
    public CaseInstanceQuery caseInstanceRootScopeId(String rootScopeId) {
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
    public CaseInstanceQuery caseInstanceParentScopeId(String parentScopeId) {
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
    public CaseInstanceQueryImpl caseInstanceBusinessKey(String businessKey) {
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
    public CaseInstanceQueryImpl caseInstanceBusinessKeyLike(String businessKeyLike) {
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
    public CaseInstanceQueryImpl caseInstanceBusinessKeyLikeIgnoreCase(String businessKeyLikeIgnoreCase) {
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
    public CaseInstanceQueryImpl caseInstanceBusinessStatus(String businessStatus) {
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
    public CaseInstanceQueryImpl caseInstanceBusinessStatusLike(String businessStatusLike) {
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
    public CaseInstanceQueryImpl caseInstanceBusinessStatusLikeIgnoreCase(String businessStatusLikeIgnoreCase) {
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
    public CaseInstanceQueryImpl caseDefinitionKeys(Set<String> caseDefinitionKeys) {
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
    public CaseInstanceQueryImpl excludeCaseDefinitionKeys(Set<String> excludeCaseDefinitionKeys) {
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
    public CaseInstanceQueryImpl caseInstanceParentId(String parentId) {
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
    public CaseInstanceQueryImpl caseInstanceStartedBefore(Date beforeTime) {
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
    public CaseInstanceQueryImpl caseInstanceStartedAfter(Date afterTime) {
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
    public CaseInstanceQueryImpl caseInstanceStartedBy(String userId) {
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
    public CaseInstanceQueryImpl caseInstanceState(String state) {
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
    public CaseInstanceQuery caseInstanceLastReactivatedBefore(Date beforeTime) {
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
    public CaseInstanceQuery caseInstanceLastReactivatedAfter(Date afterTime) {
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
    public CaseInstanceQuery caseInstanceLastReactivatedBy(String userId) {
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
    public CaseInstanceQuery caseInstanceCallbackId(String callbackId) {
        if (callbackId == null) {
            throw new FlowableIllegalArgumentException("callbackId is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.callbackId = callbackId;
        } else {
            this.callbackId = callbackId;
        }
        return this;
    }

    @Override
    public CaseInstanceQuery caseInstanceCallbackIds(Set<String> callbackIds) {
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
    public CaseInstanceQuery caseInstanceCallbackType(String callbackType) {
        if (callbackType == null) {
            throw new FlowableIllegalArgumentException("callbackType is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.callbackType = callbackType;
        } else {
            this.callbackType = callbackType;
        }
        return this;
    }
    
    @Override
    public CaseInstanceQuery parentCaseInstanceId(String parentCaseInstanceId) {
        if (parentCaseInstanceId == null) {
            throw new FlowableIllegalArgumentException("parentCaseInstanceId is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.parentCaseInstanceId = parentCaseInstanceId;
        } else {
            this.parentCaseInstanceId = parentCaseInstanceId;
        }
        return this;
    }

    @Override
    public CaseInstanceQuery caseInstanceReferenceId(String referenceId) {
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
    public CaseInstanceQuery caseInstanceReferenceType(String referenceType) {
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
    public CaseInstanceQuery caseInstanceIsCompleteable() {
        if (inOrStatement) {
            this.currentOrQueryObject.completeable = true;
        } else {
            this.completeable = true;
        }
        return this;
    }

    @Override
    public CaseInstanceQueryImpl caseInstanceTenantId(String tenantId) {
        if (tenantId == null) {
            throw new FlowableIllegalArgumentException("tenant id is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.tenantId = tenantId;
        } else {
            this.tenantId = tenantId;
        }
        return this;
    }

    @Override
    public CaseInstanceQueryImpl caseInstanceTenantIdLike(String tenantIdLike) {
        if (tenantIdLike == null) {
            throw new FlowableIllegalArgumentException("tenant id is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.tenantIdLike = tenantIdLike;
        } else {
            this.tenantIdLike = tenantIdLike;
        }
        return this;
    }
    
    @Override
    public CaseInstanceQueryImpl caseInstanceTenantIdLikeIgnoreCase(String tenantIdLikeIgnoreCase) {
        if (tenantIdLikeIgnoreCase == null) {
            throw new FlowableIllegalArgumentException("tenant id is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.tenantIdLikeIgnoreCase = tenantIdLikeIgnoreCase;
        } else {
            this.tenantIdLikeIgnoreCase = tenantIdLikeIgnoreCase;
        }
        return this;
    }
    
    @Override
    public CaseInstanceQueryImpl caseInstanceWithoutTenantId() {
        if (inOrStatement) {
            this.currentOrQueryObject.withoutTenantId = true;
        } else {
            this.withoutTenantId = true;
        }

        return this;
    }
    
    @Override
    public CaseInstanceQuery activePlanItemDefinitionId(String planItemDefinitionId) {
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
    public CaseInstanceQuery activePlanItemDefinitionIds(Set<String> planItemDefinitionIds) {
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
    public CaseInstanceQuery involvedUser(String userId) {
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
    public CaseInstanceQuery involvedUser(String userId, String identityLinkType) {
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
    public CaseInstanceQuery involvedGroup(String groupId, String identityLinkType) {
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
    public CaseInstanceQuery involvedGroups(Set<String> groupIds) {
        if (groupIds == null) {
            throw new FlowableIllegalArgumentException("involvedGroups are null");
        }
        if (groupIds.isEmpty()) {
            throw new FlowableIllegalArgumentException("involvedGroups are empty");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.involvedGroups = groupIds;
        } else {
            this.involvedGroups = groupIds;
        }
        return this;
    }

    @Override
    public CaseInstanceQuery or() {
        if (inOrStatement) {
            throw new FlowableException("the query is already in an or statement");
        }

        inOrStatement = true;
        if (commandContext != null) {
            currentOrQueryObject = new CaseInstanceQueryImpl(commandContext, cmmnEngineConfiguration);
        } else {
            currentOrQueryObject = new CaseInstanceQueryImpl(commandExecutor, cmmnEngineConfiguration);
        }
        orQueryObjects.add(currentOrQueryObject);
        return this;
    }

    @Override
    public CaseInstanceQuery endOr() {
        if (!inOrStatement) {
            throw new FlowableException("endOr() can only be called after calling or()");
        }

        inOrStatement = false;
        currentOrQueryObject = null;
        return this;
    }
    
    @Override
    public CaseInstanceQuery variableValueEquals(String variableName, Object variableValue) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueEquals(variableName, variableValue, false);
            return this;
        } else {
            return variableValueEquals(variableName, variableValue, false);
        }
    }

    @Override
    public CaseInstanceQuery variableValueNotEquals(String variableName, Object variableValue) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueNotEquals(variableName, variableValue, false);
            return this;
        } else {
            return variableValueNotEquals(variableName, variableValue, false);
        }
    }

    @Override
    public CaseInstanceQuery variableValueEquals(Object variableValue) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueEquals(variableValue, false);
            return this;
        } else {
            return variableValueEquals(variableValue, false);
        }
    }

    @Override
    public CaseInstanceQuery variableValueEqualsIgnoreCase(String name, String value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueEqualsIgnoreCase(name, value, false);
            return this;
        } else {
            return variableValueEqualsIgnoreCase(name, value, false);
        }
    }

    @Override
    public CaseInstanceQuery variableValueNotEqualsIgnoreCase(String name, String value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueNotEqualsIgnoreCase(name, value, false);
            return this;
        } else {
            return variableValueNotEqualsIgnoreCase(name, value, false);
        }
    }

    @Override
    public CaseInstanceQuery variableValueGreaterThan(String name, Object value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueGreaterThan(name, value, false);
            return this;
        } else {
            return variableValueGreaterThan(name, value, false);
        }
    }

    @Override
    public CaseInstanceQuery variableValueGreaterThanOrEqual(String name, Object value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueGreaterThanOrEqual(name, value, false);
            return this;
        } else {
            return variableValueGreaterThanOrEqual(name, value, false);
        }
    }

    @Override
    public CaseInstanceQuery variableValueLessThan(String name, Object value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueLessThan(name, value, false);
            return this;
        } else {
            return variableValueLessThan(name, value, false);
        }
    }

    @Override
    public CaseInstanceQuery variableValueLessThanOrEqual(String name, Object value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueLessThanOrEqual(name, value, false);
            return this;
        } else {
            return variableValueLessThanOrEqual(name, value, false);
        }
    }

    @Override
    public CaseInstanceQuery variableValueLike(String name, String value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueLike(name, value, false);
            return this;
        } else {
            return variableValueLike(name, value, false);
        }
    }

    @Override
    public CaseInstanceQuery variableValueLikeIgnoreCase(String name, String value) {
        if (inOrStatement) {
            currentOrQueryObject.variableValueLikeIgnoreCase(name, value, false);
            return this;
        } else {
            return variableValueLikeIgnoreCase(name, value, false);
        }
    }

    @Override
    public CaseInstanceQuery variableExists(String name) {
        if (inOrStatement) {
            currentOrQueryObject.variableExists(name, false);
            return this;
        } else {
            return variableExists(name, false);
        }
    }

    @Override
    public CaseInstanceQuery variableNotExists(String name) {
        if (inOrStatement) {
            currentOrQueryObject.variableNotExists(name, false);
            return this;
        } else {
            return variableNotExists(name, false);
        }
    }

    // ordering ////////////////////////////////////////////////////

    @Override
    public CaseInstanceQueryImpl orderByCaseInstanceId() {
        this.orderProperty = CaseInstanceQueryProperty.CASE_INSTANCE_ID;
        return this;
    }

    @Override
    public CaseInstanceQueryImpl orderByCaseDefinitionId() {
        this.orderProperty = CaseInstanceQueryProperty.CASE_DEFINITION_ID;
        return this;
    }

    @Override
    public CaseInstanceQueryImpl orderByCaseDefinitionKey() {
        this.orderProperty = CaseInstanceQueryProperty.CASE_DEFINITION_KEY;
        return this;
    }

    @Override
    public CaseInstanceQueryImpl orderByStartTime() {
        this.orderProperty = CaseInstanceQueryProperty.CASE_START_TIME;
        return this;
    }

    @Override
    public CaseInstanceQueryImpl orderByTenantId() {
        this.orderProperty = CaseInstanceQueryProperty.TENANT_ID;
        return this;
    }

    @Override
    public CaseInstanceQueryImpl includeCaseVariables() {
        this.includeCaseVariables = true;
        return this;
    }

    @Override
    public CaseInstanceQuery includeCaseVariables(Collection<String> variableNames) {
        if (variableNames == null || variableNames.isEmpty()) {
            throw new FlowableIllegalArgumentException("variableNames is null or empty");
        }
        includeCaseVariables();
        this.variableNamesToInclude = new LinkedHashSet<>(variableNames);
        return this;
    }

    @Override
    public CaseInstanceQuery locale(String locale) {
        this.locale = locale;
        return this;
    }

    @Override
    public CaseInstanceQuery withLocalizationFallback() {
        this.withLocalizationFallback = true;
        return this;
    }

    // results ////////////////////////////////////////////////////

    @Override
    public long executeCount(CommandContext commandContext) {
        ensureVariablesInitialized();
        return cmmnEngineConfiguration.getCaseInstanceEntityManager().countByCriteria(this);
    }

    @Override
    public List<CaseInstance> executeList(CommandContext commandContext) {
        ensureVariablesInitialized();
        List<CaseInstance> caseInstances = null;
        if (this.isIncludeCaseVariables()) {
            caseInstances = cmmnEngineConfiguration.getCaseInstanceEntityManager().findWithVariablesByCriteria(this);
        } else {
            caseInstances = cmmnEngineConfiguration.getCaseInstanceEntityManager().findByCriteria(this);
        }

        if (cmmnEngineConfiguration.getCaseLocalizationManager() != null) {
            for (CaseInstance caseInstance : caseInstances) {
                cmmnEngineConfiguration.getCaseLocalizationManager().localize(caseInstance, locale, withLocalizationFallback);
            }
        }

        return caseInstances;
    }

    @Override
    public void enhanceCachedValue(CaseInstanceEntity caseInstance) {
        if (isIncludeCaseVariables()) {
            if (variableNamesToInclude == null) {
                caseInstance.getQueryVariables().addAll(cmmnEngineConfiguration.getVariableServiceConfiguration().getVariableService()
                        .findVariableInstanceByScopeIdAndScopeType(caseInstance.getId(), ScopeTypes.CMMN));
            } else {
                caseInstance.getQueryVariables().addAll(cmmnEngineConfiguration.getVariableServiceConfiguration().getVariableService()
                        .createInternalVariableInstanceQuery()
                        .scopeId(caseInstance.getId())
                        .withoutSubScopeId()
                        .scopeType(ScopeTypes.CMMN)
                        .names(variableNamesToInclude).list());
            }
        }
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

    public String getCaseDefinitionId() {
        return caseDefinitionId;
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

    public String getCaseDefinitionName() {
        return caseDefinitionName;
    }
    
    public String getCaseDefinitionNameLike() {
        return caseDefinitionNameLike;
    }
    
    public String getCaseDefinitionNameLikeIgnoreCase() {
        return caseDefinitionNameLikeIgnoreCase;
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

    public Set<String> getCaseInstanceIds() {
        return caseInstanceIds;
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

    public Date getLastReactivatedBefore() {
        return lastReactivatedBefore;
    }

    public Date getLastReactivatedAfter() {
        return lastReactivatedAfter;
    }

    public String getLastReactivatedBy() {
        return lastReactivatedBy;
    }

    public String getExecutionId() {
        return caseInstanceId;
    }

    public Set<String> getCaseDefinitionIds() {
        return caseDefinitionIds;
    }

    public Set<String> getCaseDefinitionKeys() {
        return caseDefinitionKeys;
    }

    public Set<String> getExcludeCaseDefinitionKeys() {
        return excludeCaseDefinitionKeys;
    }

    public String getParentId() {
        return caseInstanceParentId;
    }
    
    public String getCaseInstanceParentPlanItemInstanceId() {
        return caseInstanceParentPlanItemInstanceId;
    }

    public String getCaseInstanceParentId() {
        return caseInstanceParentId;
    }

    public Date getStartedBefore() {
        return startedBefore;
    }

    public Date getStartedAfter() {
        return startedAfter;
    }

    public String getStartedBy() {
        return startedBy;
    }

    public String getState() {
        return state;
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

    public String getName() {
        return name;
    }

    public String getNameLike() {
        return nameLike;
    }

    public String getNameLikeIgnoreCase() {
        return nameLikeIgnoreCase;
    }

    public String getRootScopeId() {
        return rootScopeId;
    }

    public String getParentScopeId() {
        return parentScopeId;
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

    public boolean isCompleteable() {
        return completeable;
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

    public IdentityLinkQueryObject getInvolvedGroupIdentityLink() {
        return involvedGroupIdentityLink;
    }

    public Set<String> getInvolvedGroups() {
        return involvedGroups;
    }

    public boolean isIncludeCaseVariables() {
        return includeCaseVariables;
    }

    public Collection<String> getVariableNamesToInclude() {
        return variableNamesToInclude;
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

        return hasOrderByForColumn(CaseInstanceQueryProperty.CASE_DEFINITION_KEY.getName());
    }

    public List<CaseInstanceQueryImpl> getOrQueryObjects() {
        return orQueryObjects;
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
}
