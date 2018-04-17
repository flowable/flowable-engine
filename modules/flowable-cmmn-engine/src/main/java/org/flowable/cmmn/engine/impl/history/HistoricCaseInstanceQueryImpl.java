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

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.history.HistoricCaseInstanceQuery;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.variable.service.impl.AbstractVariableQueryImpl;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class HistoricCaseInstanceQueryImpl extends AbstractVariableQueryImpl<HistoricCaseInstanceQuery, HistoricCaseInstance> implements HistoricCaseInstanceQuery {

    private static final long serialVersionUID = 1L;
    
    protected String caseDefinitionId;
    protected String caseDefinitionKey;
    protected Set<String> caseDefinitionKeys;
    protected Set<String> caseDefinitionIds;
    protected String caseDefinitionName;
    protected String caseDefinitionCategory;
    protected Integer caseDefinitionVersion;
    protected String caseInstanceId;
    protected Set<String> caseInstanceIds;
    protected String businessKey;
    protected String caseInstanceParentId;
    protected String deploymentId;
    protected List<String> deploymentIds;
    protected boolean finished;
    protected boolean unfinished;
    protected Date startedBefore;
    protected Date startedAfter;
    protected Date finishedBefore;
    protected Date finishedAfter;
    protected String startedBy;
    protected String callbackId;
    protected String callbackType;
    protected String tenantId;
    protected boolean withoutTenantId;
    protected boolean includeCaseVariables;
    protected Integer caseVariablesLimit;


    public HistoricCaseInstanceQueryImpl() {
    }

    public HistoricCaseInstanceQueryImpl(CommandContext commandContext) {
        super(commandContext);
    }

    public HistoricCaseInstanceQueryImpl(CommandExecutor commandExecutor) {
        super(commandExecutor);
    }

    @Override
    public HistoricCaseInstanceQueryImpl caseDefinitionId(String caseDefinitionId) {
        if (caseDefinitionId == null) {
            throw new FlowableIllegalArgumentException("Case definition id is null");
        }
        this.caseDefinitionId = caseDefinitionId;
        return this;
    }

    @Override
    public HistoricCaseInstanceQueryImpl caseDefinitionKey(String caseDefinitionKey) {
        if (caseDefinitionKey == null) {
            throw new FlowableIllegalArgumentException("Case definition key is null");
        }
        this.caseDefinitionKey = caseDefinitionKey;
        return this;
    }

    @Override
    public HistoricCaseInstanceQueryImpl caseDefinitionCategory(String caseDefinitionCategory) {
        if (caseDefinitionCategory == null) {
            throw new FlowableIllegalArgumentException("Case definition category is null");
        }
        this.caseDefinitionCategory = caseDefinitionCategory;
        return this;
    }

    @Override
    public HistoricCaseInstanceQueryImpl caseDefinitionName(String caseDefinitionName) {
        if (caseDefinitionName == null) {
            throw new FlowableIllegalArgumentException("Case definition name is null");
        }
        this.caseDefinitionName = caseDefinitionName;
        return this;
    }

    @Override
    public HistoricCaseInstanceQueryImpl caseDefinitionVersion(Integer caseDefinitionVersion) {
        if (caseDefinitionVersion == null) {
            throw new FlowableIllegalArgumentException("Case definition version is null");
        }
        this.caseDefinitionVersion = caseDefinitionVersion;
        return this;
    }

    @Override
    public HistoricCaseInstanceQueryImpl caseInstanceId(String caseInstanceId) {
        if (caseInstanceId == null) {
            throw new FlowableIllegalArgumentException("Case instance id is null");
        }
        this.caseInstanceId = caseInstanceId;
        return this;
    }
    
    @Override
    public HistoricCaseInstanceQueryImpl caseInstanceIds(Set<String> caseInstanceIds) {
        if (caseInstanceIds == null) {
            throw new FlowableIllegalArgumentException("Case instance ids is null");
        }
        this.caseInstanceIds = caseInstanceIds;
        return this;
    }

    @Override
    public HistoricCaseInstanceQueryImpl caseInstanceBusinessKey(String businessKey) {
        if (businessKey == null) {
            throw new FlowableIllegalArgumentException("Business key is null");
        }
        this.businessKey = businessKey;
        return this;
    }

    @Override
    public HistoricCaseInstanceQueryImpl caseDefinitionKeys(Set<String> caseDefinitionKeys) {
        if (caseDefinitionKeys == null) {
            throw new FlowableIllegalArgumentException("Case definition keys is null");
        }
        this.caseDefinitionKeys = caseDefinitionKeys;
        return this;
    }

    @Override
    public HistoricCaseInstanceQueryImpl caseInstanceParentId(String parentId) {
        if (parentId == null) {
            throw new FlowableIllegalArgumentException("Parent id is null");
        }
        this.caseInstanceParentId = parentId;
        return this;
    }
    
    @Override
    public HistoricCaseInstanceQueryImpl deploymentId(String deploymentId) {
        if (deploymentId == null) {
            throw new FlowableIllegalArgumentException("Deployment id is null");
        }
        this.deploymentId = deploymentId;
        return this;
    }
    
    @Override
    public HistoricCaseInstanceQueryImpl deploymentIds(List<String> deploymentIds) {
        if (deploymentIds == null) {
            throw new FlowableIllegalArgumentException("Deployment ids is null");
        }
        this.deploymentIds = deploymentIds;
        return this;
    }
    
    @Override
    public HistoricCaseInstanceQueryImpl finished() {
        this.finished = true;
        return this;
    }
    
    @Override
    public HistoricCaseInstanceQueryImpl unfinished() {
        this.unfinished = true;
        return this;
    }
    
    @Override
    public HistoricCaseInstanceQueryImpl finishedBefore(Date beforeTime) {
        if (beforeTime == null) {
            throw new FlowableIllegalArgumentException("before time is null");
        }
        this.finishedBefore = beforeTime;
        return this;
    }
    
    @Override
    public HistoricCaseInstanceQueryImpl finishedAfter(Date afterTime) {
        if (afterTime == null) {
            throw new FlowableIllegalArgumentException("after time is null");
        }
        this.finishedAfter = afterTime;
        return this;
    }

    @Override
    public HistoricCaseInstanceQueryImpl startedBefore(Date beforeTime) {
        if (beforeTime == null) {
            throw new FlowableIllegalArgumentException("before time is null");
        }
        this.startedBefore = beforeTime;
        return this;
    }

    @Override
    public HistoricCaseInstanceQueryImpl startedAfter(Date afterTime) {
        if (afterTime == null) {
            throw new FlowableIllegalArgumentException("after time is null");
        }
        this.startedAfter = afterTime;

        return this;
    }

    @Override
    public HistoricCaseInstanceQueryImpl startedBy(String userId) {
        if (userId == null) {
            throw new FlowableIllegalArgumentException("user id is null");
        }
        this.startedBy = userId;

        return this;
    }
    
    @Override
    public HistoricCaseInstanceQuery caseInstanceCallbackId(String callbackId) {
        this.callbackId = callbackId;
        return this;
    }
    
    @Override
    public HistoricCaseInstanceQuery caseInstanceCallbackType(String callbackType) {
        this.callbackType = callbackType;
        return this;
    }
    
    @Override
    public HistoricCaseInstanceQueryImpl caseInstanceTenantId(String tenantId) {
        if (tenantId == null) {
            throw new FlowableIllegalArgumentException("caseInstance tenant id is null");
        }
        this.tenantId = tenantId;
        return this;
    }

    @Override
    public HistoricCaseInstanceQueryImpl caseInstanceWithoutTenantId() {
        this.withoutTenantId = true;
        return this;
    }

    // ordering ////////////////////////////////////////////////////

    @Override
    public HistoricCaseInstanceQueryImpl orderByCaseInstanceId() {
        this.orderProperty = HistoricCaseInstanceQueryProperty.CASE_INSTANCE_ID;
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

    public long executeCount(CommandContext commandContext) {
        ensureVariablesInitialized();
        return CommandContextUtil.getHistoricCaseInstanceEntityManager(commandContext).countByCriteria(this);
    }

    public List<HistoricCaseInstance> executeList(CommandContext commandContext) {
        ensureVariablesInitialized();
        List<HistoricCaseInstance> results;
        if (includeCaseVariables) {
            results = CommandContextUtil.getHistoricCaseInstanceEntityManager(commandContext).findWithVariablesByQueryCriteria(this);
        } else {
            results = CommandContextUtil.getHistoricCaseInstanceEntityManager(commandContext).findByCriteria(this);
        }

        return results;
    }

    @Override
    public HistoricCaseInstanceQuery includeCaseVariables() {
        this.includeCaseVariables = true;
        return this;
    }

    @Override
    public HistoricCaseInstanceQuery limitCaseVariables(Integer historicCaseVariablesLimit) {
        this.caseVariablesLimit = historicCaseVariablesLimit;
        return this;
    }

    public String getCaseDefinitionId() {
        return caseDefinitionId;
    }

    public String getCaseDefinitionKey() {
        return caseDefinitionKey;
    }

    public Set<String> getCaseDefinitionKeys() {
        return caseDefinitionKeys;
    }

    public Set<String> getCaseDefinitionIds() {
        return caseDefinitionIds;
    }

    public String getCaseDefinitionName() {
        return caseDefinitionName;
    }

    public String getCaseDefinitionCategory() {
        return caseDefinitionCategory;
    }

    public Integer getCaseDefinitionVersion() {
        return caseDefinitionVersion;
    }

    public String getCaseInstanceId() {
        return caseInstanceId;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public String getCaseInstanceParentId() {
        return caseInstanceParentId;
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
    
    public String getCallbackId() {
        return callbackId;
    }

    public String getCallbackType() {
        return callbackType;
    }

    public String getTenantId() {
        return tenantId;
    }

    public boolean isWithoutTenantId() {
        return withoutTenantId;
    }

    public boolean isIncludeCaseVariables() {
        return includeCaseVariables;
    }

    public Integer getCaseVariablesLimit() {
        return caseVariablesLimit;
    }

    public String getMssqlOrDB2OrderBy() {
        String specialOrderBy = super.getOrderByColumns();
        if (specialOrderBy != null && specialOrderBy.length() > 0) {
            specialOrderBy = specialOrderBy.replace("RES.", "TEMPRES_");
        }
        return specialOrderBy;
    }

}