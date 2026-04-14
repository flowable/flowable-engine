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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.query.AbstractQuery;
import org.flowable.job.api.Job;
import org.flowable.job.api.JobQuery;
import org.flowable.job.service.JobServiceConfiguration;

/**
 * @author Joram Barrez
 * @author Tom Baeyens
 * @author Falko Menge
 */
public class JobQueryImpl extends AbstractQuery<JobQuery, Job> implements JobQuery, Serializable {

    private static final long serialVersionUID = 1L;

    protected JobServiceConfiguration jobServiceConfiguration;

    protected String id;
    protected Collection<String> jobIds;
    protected String processInstanceId;
    protected boolean withoutProcessInstanceId;
    protected String executionId;
    protected String handlerType;
    protected Collection<String> handlerTypes;
    protected String processDefinitionId;
    protected String processDefinitionKey;
    protected String category;
    protected String categoryLike;
    protected String elementId;
    protected String elementName;
    protected String scopeId;
    protected boolean withoutScopeId;
    protected String subScopeId;
    protected String scopeType;
    protected String scopeDefinitionId;
    protected String caseDefinitionKey;
    protected String correlationId;
    protected boolean onlyTimers;
    protected boolean onlyMessages;
    protected Date duedateHigherThan;
    protected Date duedateLowerThan;
    protected Date duedateHigherThanOrEqual;
    protected Date duedateLowerThanOrEqual;
    protected boolean withException;
    protected String exceptionMessage;
    protected String tenantId;
    protected String tenantIdLike;
    protected boolean withoutTenantId;
    protected boolean withoutScopeType;

    protected String lockOwner;
    protected boolean onlyLocked;
    protected boolean onlyUnlocked;

    protected List<JobQueryImpl> orQueryObjects = new ArrayList<>();
    protected JobQueryImpl currentOrQueryObject;
    protected boolean inOrStatement;

    public JobQueryImpl() {
    }

    public JobQueryImpl(CommandContext commandContext, JobServiceConfiguration jobServiceConfiguration) {
        super(commandContext);
        this.jobServiceConfiguration = jobServiceConfiguration;
    }

    public JobQueryImpl(CommandExecutor commandExecutor, JobServiceConfiguration jobServiceConfiguration) {
        super(commandExecutor);
        this.jobServiceConfiguration = jobServiceConfiguration;
    }

    @Override
    public JobQuery jobId(String jobId) {
        if (jobId == null) {
            throw new FlowableIllegalArgumentException("Provided job id is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.id = jobId;
        } else {
            this.id = jobId;
        }
        return this;
    }

    @Override
    public JobQuery jobIds(Collection<String> jobIds) {
        if (jobIds == null) {
            throw new FlowableIllegalArgumentException("Provided job id list is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.jobIds = jobIds;
        } else {
            this.jobIds = jobIds;
        }
        return this;
    }

    @Override
    public JobQueryImpl processInstanceId(String processInstanceId) {
        if (processInstanceId == null) {
            throw new FlowableIllegalArgumentException("Provided process instance id is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.processInstanceId = processInstanceId;
        } else {
            this.processInstanceId = processInstanceId;
        }
        return this;
    }

    @Override
    public JobQuery withoutProcessInstanceId() {
        if (inOrStatement) {
            this.currentOrQueryObject.withoutProcessInstanceId = true;
        } else {
            this.withoutProcessInstanceId = true;
        }
        return this;
    }

    @Override
    public JobQueryImpl processDefinitionId(String processDefinitionId) {
        if (processDefinitionId == null) {
            throw new FlowableIllegalArgumentException("Provided process definition id is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionId = processDefinitionId;
        } else {
            this.processDefinitionId = processDefinitionId;
        }
        return this;
    }

    @Override
    public JobQueryImpl processDefinitionKey(String processDefinitionKey) {
        if (processDefinitionKey == null) {
            throw new FlowableIllegalArgumentException("Provided process definition key is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionKey = processDefinitionKey;
        } else {
            this.processDefinitionKey = processDefinitionKey;
        }
        return this;
    }

    @Override
    public JobQueryImpl category(String category) {
        if (category == null) {
            throw new FlowableIllegalArgumentException("Provided category is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.category = category;
        } else {
            this.category = category;
        }
        return this;
    }

    @Override
    public JobQueryImpl categoryLike(String categoryLike) {
        if (categoryLike == null) {
            throw new FlowableIllegalArgumentException("Provided categoryLike is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.categoryLike = categoryLike;
        } else {
            this.categoryLike = categoryLike;
        }
        return this;
    }

    @Override
    public JobQueryImpl elementId(String elementId) {
        if (elementId == null) {
            throw new FlowableIllegalArgumentException("Provided element id is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.elementId = elementId;
        } else {
            this.elementId = elementId;
        }
        return this;
    }

    @Override
    public JobQueryImpl elementName(String elementName) {
        if (elementName == null) {
            throw new FlowableIllegalArgumentException("Provided element name is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.elementName = elementName;
        } else {
            this.elementName = elementName;
        }
        return this;
    }

    @Override
    public JobQueryImpl scopeId(String scopeId) {
        if (scopeId == null) {
            throw new FlowableIllegalArgumentException("Provided scope id is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.scopeId = scopeId;
        } else {
            this.scopeId = scopeId;
        }
        return this;
    }
    
    @Override
    public JobQuery withoutScopeId() {
        if (inOrStatement) {
            this.currentOrQueryObject.withoutScopeId = true;
        } else {
            this.withoutScopeId = true;
        }
        return this;
    }

    @Override
    public JobQueryImpl subScopeId(String subScopeId) {
        if (subScopeId == null) {
            throw new FlowableIllegalArgumentException("Provided sub scope id is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.subScopeId = subScopeId;
        } else {
            this.subScopeId = subScopeId;
        }
        return this;
    }

    @Override
    public JobQueryImpl scopeType(String scopeType) {
        if (scopeType == null) {
            throw new FlowableIllegalArgumentException("Provided scope type is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.scopeType = scopeType;
        } else {
            this.scopeType = scopeType;
        }
        return this;
    }

    @Override
    public JobQueryImpl scopeDefinitionId(String scopeDefinitionId) {
        if (scopeDefinitionId == null) {
            throw new FlowableIllegalArgumentException("Provided scope definitionid is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.scopeDefinitionId = scopeDefinitionId;
        } else {
            this.scopeDefinitionId = scopeDefinitionId;
        }
        return this;
    }

    @Override
    public JobQueryImpl caseInstanceId(String caseInstanceId) {
        if (caseInstanceId == null) {
            throw new FlowableIllegalArgumentException("Provided case instance id is null");
        }
        scopeId(caseInstanceId);
        scopeType(ScopeTypes.CMMN);
        return this;
    }

    @Override
    public JobQueryImpl caseDefinitionId(String caseDefinitionId) {
        if (caseDefinitionId == null) {
            throw new FlowableIllegalArgumentException("Provided case definition id is null");
        }
        scopeDefinitionId(caseDefinitionId);
        scopeType(ScopeTypes.CMMN);
        return this;
    }

    @Override
    public JobQueryImpl caseDefinitionKey(String caseDefinitionKey) {
        if (caseDefinitionKey == null) {
            throw new FlowableIllegalArgumentException("Provided case definition id is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.caseDefinitionKey = caseDefinitionKey;
        } else {
            this.caseDefinitionKey = caseDefinitionKey;
        }
        return this;
    }

    @Override
    public JobQueryImpl planItemInstanceId(String planItemInstanceId) {
        if (planItemInstanceId == null) {
            throw new FlowableIllegalArgumentException("Provided plan item instance id is null");
        }
        subScopeId(planItemInstanceId);
        scopeType(ScopeTypes.CMMN);
        return this;
    }

    @Override
    public JobQueryImpl correlationId(String correlationId) {
        if (correlationId == null) {
            throw new FlowableIllegalArgumentException("Provided correlationId is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.correlationId = correlationId;
        } else {
            this.correlationId = correlationId;
        }
        return this;
    }

    @Override
    public JobQueryImpl executionId(String executionId) {
        if (executionId == null) {
            throw new FlowableIllegalArgumentException("Provided execution id is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.executionId = executionId;
        } else {
            this.executionId = executionId;
        }
        return this;
    }

    @Override
    public JobQueryImpl handlerType(String handlerType) {
        if (handlerType == null) {
            throw new FlowableIllegalArgumentException("Provided handlerType is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.handlerType = handlerType;
        } else {
            this.handlerType = handlerType;
        }
        return this;
    }

    @Override
    public JobQuery handlerTypes(Collection<String> handlerTypes) {
        if (handlerTypes == null) {
            throw new FlowableIllegalArgumentException("Provided handlerTypes are null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.handlerTypes = handlerTypes;
        } else {
            this.handlerTypes = handlerTypes;
        }
        return this;
    }

    @Override
    public JobQuery timers() {
        if (onlyMessages) {
            throw new FlowableIllegalArgumentException("Cannot combine onlyTimers() with onlyMessages() in the same query");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.onlyTimers = true;
        } else {
            this.onlyTimers = true;
        }
        return this;
    }

    @Override
    public JobQuery messages() {
        if (onlyTimers) {
            throw new FlowableIllegalArgumentException("Cannot combine onlyTimers() with onlyMessages() in the same query");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.onlyMessages = true;
        } else {
            this.onlyMessages = true;
        }
        return this;
    }

    @Override
    public JobQuery duedateHigherThan(Date date) {
        if (date == null) {
            throw new FlowableIllegalArgumentException("Provided date is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.duedateHigherThan = date;
        } else {
            this.duedateHigherThan = date;
        }
        return this;
    }

    @Override
    public JobQuery duedateLowerThan(Date date) {
        if (date == null) {
            throw new FlowableIllegalArgumentException("Provided date is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.duedateLowerThan = date;
        } else {
            this.duedateLowerThan = date;
        }
        return this;
    }

    @Override
    public JobQuery withException() {
        if (inOrStatement) {
            this.currentOrQueryObject.withException = true;
        } else {
            this.withException = true;
        }
        return this;
    }

    @Override
    public JobQuery exceptionMessage(String exceptionMessage) {
        if (exceptionMessage == null) {
            throw new FlowableIllegalArgumentException("Provided exception message is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.exceptionMessage = exceptionMessage;
        } else {
            this.exceptionMessage = exceptionMessage;
        }
        return this;
    }

    @Override
    public JobQuery jobTenantId(String tenantId) {
        if (tenantId == null) {
            throw new FlowableIllegalArgumentException("Provided tenant id is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.tenantId = tenantId;
        } else {
            this.tenantId = tenantId;
        }
        return this;
    }

    @Override
    public JobQuery jobTenantIdLike(String tenantIdLike) {
        if (tenantIdLike == null) {
            throw new FlowableIllegalArgumentException("Provided tenant id is null");
        }
        if (inOrStatement) {
            this.currentOrQueryObject.tenantIdLike = tenantIdLike;
        } else {
            this.tenantIdLike = tenantIdLike;
        }
        return this;
    }

    @Override
    public JobQuery jobWithoutTenantId() {
        if (inOrStatement) {
            this.currentOrQueryObject.withoutTenantId = true;
        } else {
            this.withoutTenantId = true;
        }
        return this;
    }

    @Override
    public JobQuery lockOwner(String lockOwner) {
        if (inOrStatement) {
            this.currentOrQueryObject.lockOwner = lockOwner;
        } else {
            this.lockOwner = lockOwner;
        }
        return this;
    }

    @Override
    public JobQuery locked() {
        if (inOrStatement) {
            this.currentOrQueryObject.onlyLocked = true;
        } else {
            this.onlyLocked = true;
        }
        return this;
    }

    @Override
    public JobQuery unlocked() {
        if (inOrStatement) {
            this.currentOrQueryObject.onlyUnlocked = true;
        } else {
            this.onlyUnlocked = true;
        }
        return this;
    }

    @Override
    public JobQuery withoutScopeType() {
        if (inOrStatement) {
            this.currentOrQueryObject.withoutScopeType = true;
        } else {
            this.withoutScopeType = true;
        }
        return this;
    }

    @Override
    public JobQuery or() {
        if (inOrStatement) {
            throw new FlowableException("the query is already in an or statement");
        }
        inOrStatement = true;
        if (commandContext != null) {
            currentOrQueryObject = new JobQueryImpl(commandContext, jobServiceConfiguration);
        } else {
            currentOrQueryObject = new JobQueryImpl(commandExecutor, jobServiceConfiguration);
        }
        orQueryObjects.add(currentOrQueryObject);
        return this;
    }

    @Override
    public JobQuery endOr() {
        if (!inOrStatement) {
            throw new FlowableException("endOr() can only be called after calling or()");
        }
        inOrStatement = false;
        currentOrQueryObject = null;
        return this;
    }

    // sorting //////////////////////////////////////////

    @Override
    public JobQuery orderByJobDuedate() {
        return orderBy(JobQueryProperty.DUEDATE);
    }

    @Override
    public JobQuery orderByJobCreateTime() {
        return orderBy(JobQueryProperty.CREATE_TIME);
    }

    @Override
    public JobQuery orderByExecutionId() {
        return orderBy(JobQueryProperty.EXECUTION_ID);
    }

    @Override
    public JobQuery orderByJobId() {
        return orderBy(JobQueryProperty.JOB_ID);
    }

    @Override
    public JobQuery orderByProcessInstanceId() {
        return orderBy(JobQueryProperty.PROCESS_INSTANCE_ID);
    }

    @Override
    public JobQuery orderByJobRetries() {
        return orderBy(JobQueryProperty.RETRIES);
    }

    @Override
    public JobQuery orderByTenantId() {
        return orderBy(JobQueryProperty.TENANT_ID);
    }

    // results //////////////////////////////////////////

    @Override
    public long executeCount(CommandContext commandContext) {
        return jobServiceConfiguration.getJobEntityManager().findJobCountByQueryCriteria(this);
    }

    @Override
    public List<Job> executeList(CommandContext commandContext) {
        return jobServiceConfiguration.getJobEntityManager().findJobsByQueryCriteria(this);
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

    public Collection<String> getHandlerTypes() {
	return this.handlerTypes;
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

    public String getId() {
        return id;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public String getProcessDefinitionKey() {
        return processDefinitionKey;
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

    public String getScopeDefinitionId() {
        return scopeDefinitionId;
    }

    public String getCaseDefinitionKey() {
        return caseDefinitionKey;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public boolean isOnlyTimers() {
        return onlyTimers;
    }

    public boolean isOnlyMessages() {
        return onlyMessages;
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

    public boolean isWithoutScopeType() {
        return withoutScopeType;
    }

    public List<JobQueryImpl> getOrQueryObjects() {
        return orQueryObjects;
    }
}
