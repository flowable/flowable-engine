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

package org.flowable.job.service.impl;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.query.AbstractQuery;
import org.flowable.job.api.ExternalWorkerJob;
import org.flowable.job.api.ExternalWorkerJobQuery;
import org.flowable.job.service.JobServiceConfiguration;

/**
 * @author Filip Hrisafov
 */
public class ExternalWorkerJobQueryImpl extends AbstractQuery<ExternalWorkerJobQuery, ExternalWorkerJob> implements ExternalWorkerJobQuery, Serializable {

    private static final long serialVersionUID = 1L;
    
    protected JobServiceConfiguration jobServiceConfiguration;
    
    protected String id;
    protected String processInstanceId;
    protected boolean withoutProcessInstanceId;
    protected String executionId;
    protected String handlerType;
    protected Collection<String> handlerTypes;
    protected String processDefinitionId;
    protected String category;
    protected String categoryLike;
    protected String elementId;
    protected String elementName;
    protected String scopeId;
    protected boolean withoutScopeId;
    protected String subScopeId;
    protected String scopeType;
    protected boolean withoutScopeType;
    protected String scopeDefinitionId;
    protected String correlationId;
    protected Date duedateHigherThan;
    protected Date duedateLowerThan;
    protected Date duedateHigherThanOrEqual;
    protected Date duedateLowerThanOrEqual;
    protected boolean withException;
    protected String exceptionMessage;
    protected String tenantId;
    protected String tenantIdLike;
    protected boolean withoutTenantId;
    protected String authorizedUser;
    protected Collection<String> authorizedGroups;

    protected String lockOwner;
    protected boolean onlyLocked;
    protected boolean onlyUnlocked;

    public ExternalWorkerJobQueryImpl() {
    }

    public ExternalWorkerJobQueryImpl(CommandContext commandContext, JobServiceConfiguration jobServiceConfiguration) {
        super(commandContext);
        this.jobServiceConfiguration = jobServiceConfiguration;
    }

    public ExternalWorkerJobQueryImpl(CommandExecutor commandExecutor, JobServiceConfiguration jobServiceConfiguration) {
        super(commandExecutor);
        this.jobServiceConfiguration = jobServiceConfiguration;
    }

    @Override
    public ExternalWorkerJobQuery jobId(String jobId) {
        if (jobId == null) {
            throw new FlowableIllegalArgumentException("Provided job id is null");
        }
        this.id = jobId;
        return this;
    }

    @Override
    public ExternalWorkerJobQuery processInstanceId(String processInstanceId) {
        if (processInstanceId == null) {
            throw new FlowableIllegalArgumentException("Provided process instance id is null");
        }
        this.processInstanceId = processInstanceId;
        return this;
    }
    
    @Override
    public ExternalWorkerJobQuery withoutProcessInstanceId() {
        this.withoutProcessInstanceId = true;
        return this;
    }

    @Override
    public ExternalWorkerJobQuery processDefinitionId(String processDefinitionId) {
        if (processDefinitionId == null) {
            throw new FlowableIllegalArgumentException("Provided process definition id is null");
        }
        this.processDefinitionId = processDefinitionId;
        return this;
    }

    @Override
    public ExternalWorkerJobQuery category(String category) {
        if (category == null) {
            throw new FlowableIllegalArgumentException("Provided category is null");
        }
        this.category = category;
        return this;
    }

    @Override
    public ExternalWorkerJobQuery categoryLike(String categoryLike) {
        if (categoryLike == null) {
            throw new FlowableIllegalArgumentException("Provided categoryLike is null");
        }
        this.categoryLike = categoryLike;
        return this;
    }

    @Override
    public ExternalWorkerJobQuery elementId(String elementId) {
        if (elementId == null) {
            throw new FlowableIllegalArgumentException("Provided element id is null");
        }
        this.elementId = elementId;
        return this;
    }

    @Override
    public ExternalWorkerJobQuery elementName(String elementName) {
        if (elementName == null) {
            throw new FlowableIllegalArgumentException("Provided element name is null");
        }
        this.elementName = elementName;
        return this;
    }

    @Override
    public ExternalWorkerJobQueryImpl scopeId(String scopeId) {
        if (scopeId == null) {
            throw new FlowableIllegalArgumentException("Provided scope id is null");
        }
        this.scopeId = scopeId;
        return this;
    }
    
    @Override
    public ExternalWorkerJobQuery withoutScopeId() {
        this.withoutScopeId = true;
        return this;
    }

    @Override
    public ExternalWorkerJobQuery subScopeId(String subScopeId) {
        if (subScopeId == null) {
            throw new FlowableIllegalArgumentException("Provided sub scope id is null");
        }
        this.subScopeId = subScopeId;
        return this;
    }

    @Override
    public ExternalWorkerJobQueryImpl scopeType(String scopeType) {
        if (scopeType == null) {
            throw new FlowableIllegalArgumentException("Provided scope type is null");
        }
        this.scopeType = scopeType;
        return this;
    }

    @Override
    public ExternalWorkerJobQueryImpl withoutScopeType() {
        this.withoutScopeType = true;
        return this;
    }

    @Override
    public ExternalWorkerJobQuery scopeDefinitionId(String scopeDefinitionId) {
        if (scopeDefinitionId == null) {
            throw new FlowableIllegalArgumentException("Provided scope definitionid is null");
        }
        this.scopeDefinitionId = scopeDefinitionId;
        return this;
    }

    @Override
    public ExternalWorkerJobQuery caseInstanceId(String caseInstanceId) {
        if (caseInstanceId == null) {
            throw new FlowableIllegalArgumentException("Provided case instance id is null");
        }
        scopeId(caseInstanceId);
        scopeType(ScopeTypes.CMMN);
        return this;
    }

    @Override
    public ExternalWorkerJobQuery caseDefinitionId(String caseDefinitionId) {
        if (caseDefinitionId == null) {
            throw new FlowableIllegalArgumentException("Provided case definition id is null");
        }
        scopeDefinitionId(caseDefinitionId);
        scopeType(ScopeTypes.CMMN);
        return this;
    }

    @Override
    public ExternalWorkerJobQuery planItemInstanceId(String planItemInstanceId) {
        if (planItemInstanceId == null) {
            throw new FlowableIllegalArgumentException("Provided plan item instance id is null");
        }
        subScopeId(planItemInstanceId);
        scopeType(ScopeTypes.CMMN);
        return this;
    }

    @Override
    public ExternalWorkerJobQuery correlationId(String correlationId) {
        if (correlationId == null) {
            throw new FlowableIllegalArgumentException("Provided correlationId is null");
        }
        this.correlationId = correlationId;
        return this;
    }

    @Override
    public ExternalWorkerJobQuery executionId(String executionId) {
        if (executionId == null) {
            throw new FlowableIllegalArgumentException("Provided execution id is null");
        }
        this.executionId = executionId;
        return this;
    }

    @Override
    public ExternalWorkerJobQuery handlerType(String handlerType) {
        if (handlerType == null) {
            throw new FlowableIllegalArgumentException("Provided handlerType is null");
        }
        this.handlerType = handlerType;
        return this;
    }

    @Override
    public ExternalWorkerJobQuery handlerTypes(Collection<String> handlerTypes) {
        if (handlerTypes == null) {
            throw new FlowableIllegalArgumentException("Provided handlerTypes are null");
        }
        this.handlerTypes = handlerTypes;
        return this;
    }

    @Override
    public ExternalWorkerJobQuery duedateHigherThan(Date date) {
        if (date == null) {
            throw new FlowableIllegalArgumentException("Provided date is null");
        }
        this.duedateHigherThan = date;
        return this;
    }

    @Override
    public ExternalWorkerJobQuery duedateLowerThan(Date date) {
        if (date == null) {
            throw new FlowableIllegalArgumentException("Provided date is null");
        }
        this.duedateLowerThan = date;
        return this;
    }

    @Override
    public ExternalWorkerJobQuery withException() {
        this.withException = true;
        return this;
    }

    @Override
    public ExternalWorkerJobQuery exceptionMessage(String exceptionMessage) {
        if (exceptionMessage == null) {
            throw new FlowableIllegalArgumentException("Provided exception message is null");
        }
        this.exceptionMessage = exceptionMessage;
        return this;
    }

    @Override
    public ExternalWorkerJobQuery jobTenantId(String tenantId) {
        if (tenantId == null) {
            throw new FlowableIllegalArgumentException("Provided tenant id is null");
        }
        this.tenantId = tenantId;
        return this;
    }

    @Override
    public ExternalWorkerJobQuery jobTenantIdLike(String tenantIdLike) {
        if (tenantIdLike == null) {
            throw new FlowableIllegalArgumentException("Provided tenant id is null");
        }
        this.tenantIdLike = tenantIdLike;
        return this;
    }

    @Override
    public ExternalWorkerJobQuery jobWithoutTenantId() {
        this.withoutTenantId = true;
        return this;
    }

    @Override
    public ExternalWorkerJobQuery forUserOrGroups(String userId, Collection<String> groups) {
        if (userId == null && (groups == null || groups.isEmpty())) {
            throw new FlowableIllegalArgumentException("at least one of userId or groups must be provided");
        }

        this.authorizedUser = userId;
        this.authorizedGroups = groups;

        return this;
    }

    @Override
    public ExternalWorkerJobQuery lockOwner(String lockOwner) {
        this.lockOwner = lockOwner;
        return this;
    }

    @Override
    public ExternalWorkerJobQuery locked() {
        this.onlyLocked = true;
        return this;
    }

    @Override
    public ExternalWorkerJobQuery unlocked() {
        this.onlyUnlocked = true;
        return this;
    }

    // sorting //////////////////////////////////////////

    @Override
    public ExternalWorkerJobQuery orderByJobDuedate() {
        return orderBy(JobQueryProperty.DUEDATE);
    }

    @Override
    public ExternalWorkerJobQuery orderByJobCreateTime() {
        return orderBy(JobQueryProperty.CREATE_TIME);
    }

    @Override
    public ExternalWorkerJobQuery orderByExecutionId() {
        return orderBy(JobQueryProperty.EXECUTION_ID);
    }

    @Override
    public ExternalWorkerJobQuery orderByJobId() {
        return orderBy(JobQueryProperty.JOB_ID);
    }

    @Override
    public ExternalWorkerJobQuery orderByProcessInstanceId() {
        return orderBy(JobQueryProperty.PROCESS_INSTANCE_ID);
    }

    @Override
    public ExternalWorkerJobQuery orderByJobRetries() {
        return orderBy(JobQueryProperty.RETRIES);
    }

    @Override
    public ExternalWorkerJobQuery orderByTenantId() {
        return orderBy(JobQueryProperty.TENANT_ID);
    }

    // results //////////////////////////////////////////

    @Override
    public long executeCount(CommandContext commandContext) {
        return jobServiceConfiguration.getExternalWorkerJobEntityManager().findJobCountByQueryCriteria(this);
    }

    @Override
    public List<ExternalWorkerJob> executeList(CommandContext commandContext) {
        return jobServiceConfiguration.getExternalWorkerJobEntityManager().findJobsByQueryCriteria(this);
    }

    // getters //////////////////////////////////////////

    public String getProcessInstanceId() {
        return processInstanceId;
    }
    
    public boolean isWithoutProcessInstanceId() {
        return withoutProcessInstanceId;
    }

    public String getExecutionId() {
        return executionId;
    }

    public String getHandlerType() {
        return this.handlerType;
    }

    public Date getNow() {
        return jobServiceConfiguration.getClock().getCurrentTime();
    }

    public boolean isWithException() {
        return withException;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
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

    public String getAuthorizedUser() {
        return authorizedUser;
    }

    public Collection<String> getAuthorizedGroups() {
        return authorizedGroups;
    }

    public String getId() {
        return id;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public String getCategory() {
        return category;
    }

    public String getCategoryLike() {
        return categoryLike;
    }

    public String getElementId() {
        return elementId;
    }

    public String getElementName() {
        return elementName;
    }

    public String getScopeId() {
        return scopeId;
    }
    
    public boolean isWithoutScopeId() {
        return withoutScopeId;
    }

    public String getSubScopeId() {
        return subScopeId;
    }

    public String getScopeType() {
        return scopeType;
    }

    public boolean isWithoutScopeType() {
        return withoutScopeType;
    }

    public String getScopeDefinitionId() {
        return scopeDefinitionId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public Date getDuedateHigherThan() {
        return duedateHigherThan;
    }

    public Date getDuedateLowerThan() {
        return duedateLowerThan;
    }

    public Date getDuedateHigherThanOrEqual() {
        return duedateHigherThanOrEqual;
    }

    public Date getDuedateLowerThanOrEqual() {
        return duedateLowerThanOrEqual;
    }

    public String getLockOwner() {
        return lockOwner;
    }

    public boolean isOnlyLocked() {
        return onlyLocked;
    }

    public boolean isOnlyUnlocked() {
        return onlyUnlocked;
    }

}
