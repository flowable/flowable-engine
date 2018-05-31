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
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstanceQuery;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.variable.service.impl.AbstractVariableQueryImpl;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class CaseInstanceQueryImpl extends AbstractVariableQueryImpl<CaseInstanceQuery, CaseInstance> implements CaseInstanceQuery {

    private static final long serialVersionUID = 1L;

    protected String caseDefinitionId;
    protected String caseDefinitionKey;
    protected Set<String> caseDefinitionKeys;
    protected Set<String> caseDefinitionIds;
    protected String caseDefinitionCategory;
    protected String caseDefinitionName;
    protected Integer caseDefinitionVersion;
    protected String businessKey;
    protected String caseInstanceId;
    protected Set<String> caseInstanceIds;
    protected String caseInstanceParentId;
    protected String caseInstanceParentPlanItemInstanceId;
    protected Date startedBefore;
    protected Date startedAfter;
    protected String startedBy;
    protected String callbackId;
    protected String callbackType;
    protected boolean completeable;
    protected String tenantId;
    protected String tenantIdLike;
    protected boolean withoutTenantId;
    protected boolean includeCaseVariables;
    protected String involvedUser;
    protected Set<String> involvedGroups;

    protected List<CaseInstanceQueryImpl> orQueryObjects = new ArrayList<>();
    protected CaseInstanceQueryImpl currentOrQueryObject;
    protected boolean inOrStatement;

    protected Integer caseInstanceVariablesLimit;

    public CaseInstanceQueryImpl() {
    }

    public CaseInstanceQueryImpl(CommandContext commandContext) {
        super(commandContext);
    }

    public CaseInstanceQueryImpl(CommandExecutor commandExecutor) {
        super(commandExecutor);
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
    public CaseInstanceQuery caseInstanceCallbackType(String callbackType) {
        if (callbackType == null) {
            throw new FlowableIllegalArgumentException("callbackType is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.callbackType= callbackType;
        } else {
            this.callbackType = callbackType;
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
    public CaseInstanceQueryImpl caseInstanceWithoutTenantId() {
        if (inOrStatement) {
            this.currentOrQueryObject.withoutTenantId = true;
        } else {
            this.withoutTenantId = true;
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
        currentOrQueryObject = new CaseInstanceQueryImpl();
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
    public CaseInstanceQuery limitCaseInstanceVariables(Integer caseInstanceVariablesLimit) {
        this.caseInstanceVariablesLimit = caseInstanceVariablesLimit;
        return this;
    }

    public Integer getCaseInstanceVariablesLimit() {
        return this.caseInstanceVariablesLimit;
    }

// results ////////////////////////////////////////////////////

    @Override
    public long executeCount(CommandContext commandContext) {
        ensureVariablesInitialized();
        return CommandContextUtil.getCaseInstanceEntityManager(commandContext).countByCriteria(this);
    }

    @Override
    public List<CaseInstance> executeList(CommandContext commandContext) {
        ensureVariablesInitialized();
        if (this.isIncludeCaseVariables()) {
            return CommandContextUtil.getCaseInstanceEntityManager(commandContext).findWithVariablesByCriteria(this);
        }
        return CommandContextUtil.getCaseInstanceEntityManager(commandContext).findByCriteria(this);
    }

    public String getCaseDefinitionKey() {
        return caseDefinitionKey;
    }

    public String getCaseDefinitionId() {
        return caseDefinitionId;
    }

    public String getCaseDefinitionCategory() {
        return caseDefinitionCategory;
    }

    public String getCaseDefinitionName() {
        return caseDefinitionName;
    }

    public Integer getCaseDefinitionVersion() {
        return caseDefinitionVersion;
    }

    public String getCaseInstanceId() {
        return caseInstanceId;
    }

    public Set<String> getCaseInstanceIds() {
        return caseInstanceIds;
    }

    public String getBusinessKey() {
        return businessKey;
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

    public String getParentId() {
        return caseInstanceParentId;
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

    public String getCallbackId() {
        return callbackId;
    }

    public String getCallbackType() {
        return callbackType;
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

    public boolean isWithoutTenantId() {
        return withoutTenantId;
    }

    public String getInvolvedUser() {
        return involvedUser;
    }

    public Set<String> getInvolvedGroups() {
        return involvedGroups;
    }

    public boolean isIncludeCaseVariables() {
        return includeCaseVariables;
    }

    public String getMssqlOrDB2OrderBy() {
        String specialOrderBy = super.getOrderByColumns();
        if (specialOrderBy != null && specialOrderBy.length() > 0) {
            specialOrderBy = specialOrderBy.replace("RES.", "TEMPRES_");
        }
        return specialOrderBy;
    }
}
