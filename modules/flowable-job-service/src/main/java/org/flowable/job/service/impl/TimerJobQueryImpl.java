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
import java.util.Date;
import java.util.List;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.AbstractQuery;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.job.api.Job;
import org.flowable.job.api.TimerJobQuery;
import org.flowable.job.service.impl.util.CommandContextUtil;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class TimerJobQueryImpl extends AbstractQuery<TimerJobQuery, Job> implements TimerJobQuery, Serializable {

    private static final long serialVersionUID = 1L;
    protected String id;
    protected String processInstanceId;
    protected String executionId;
    protected String handlerType;
    protected String processDefinitionId;
    protected String scopeId;
    protected String subScopeId;
    protected String scopeType;
    protected String scopeDefinitionId;
    protected boolean executable;
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

    public TimerJobQueryImpl() {
    }

    public TimerJobQueryImpl(CommandContext commandContext) {
        super(commandContext);
    }

    public TimerJobQueryImpl(CommandExecutor commandExecutor) {
        super(commandExecutor);
    }

    @Override
    public TimerJobQueryImpl jobId(String jobId) {
        if (jobId == null) {
            throw new FlowableIllegalArgumentException("Provided job id is null");
        }
        this.id = jobId;
        return this;
    }

    @Override
    public TimerJobQueryImpl processInstanceId(String processInstanceId) {
        if (processInstanceId == null) {
            throw new FlowableIllegalArgumentException("Provided process instance id is null");
        }
        this.processInstanceId = processInstanceId;
        return this;
    }

    @Override
    public TimerJobQueryImpl processDefinitionId(String processDefinitionId) {
        if (processDefinitionId == null) {
            throw new FlowableIllegalArgumentException("Provided process definition id is null");
        }
        this.processDefinitionId = processDefinitionId;
        return this;
    }
    
    @Override
    public TimerJobQueryImpl scopeId(String scopeId) {
        if (scopeId == null) {
            throw new FlowableIllegalArgumentException("Provided scope id is null");
        }
        this.scopeId = scopeId;
        return this;
    }
    
    @Override
    public TimerJobQueryImpl subScopeId(String subScopeId) {
        if (scopeId == null) {
            throw new FlowableIllegalArgumentException("Provided sub scope id is null");
        }
        this.subScopeId = subScopeId;
        return this;
    }
    
    @Override
    public TimerJobQueryImpl scopeType(String scopeType) {
        if (scopeType == null) {
            throw new FlowableIllegalArgumentException("Provided scope type is null");
        }
        this.scopeType = scopeType;
        return this;
    }
    
    @Override
    public TimerJobQueryImpl scopeDefinitionId(String scopeDefinitionId) {
        if (scopeDefinitionId == null) {
            throw new FlowableIllegalArgumentException("Provided scope definitionid is null");
        }
        this.scopeDefinitionId = scopeDefinitionId;
        return this;
    }
    
    @Override
    public TimerJobQueryImpl caseInstanceId(String caseInstanceId) {
        if (caseInstanceId == null) {
            throw new FlowableIllegalArgumentException("Provided case instance id is null");
        }
        scopeId(caseInstanceId);
        scopeType(ScopeTypes.CMMN);
        return this;
    }
    
    @Override
    public TimerJobQueryImpl caseDefinitionId(String caseDefinitionId) {
        if (caseDefinitionId == null) {
            throw new FlowableIllegalArgumentException("Provided case definition id is null");
        }
        scopeDefinitionId(caseDefinitionId);
        scopeType(ScopeTypes.CMMN);
        return this;
    }
    
    @Override
    public TimerJobQueryImpl planItemInstanceId(String planItemInstanceId) {
        if (planItemInstanceId == null) {
            throw new FlowableIllegalArgumentException("Provided plan item instance id is null");
        }
        subScopeId(planItemInstanceId);
        scopeType(ScopeTypes.CMMN);
        return this;
    }

    @Override
    public TimerJobQueryImpl executionId(String executionId) {
        if (executionId == null) {
            throw new FlowableIllegalArgumentException("Provided execution id is null");
        }
        this.executionId = executionId;
        return this;
    }
    
    @Override
    public TimerJobQueryImpl handlerType(String handlerType) {
        if (handlerType == null) {
            throw new FlowableIllegalArgumentException("Provided handlerType is null");
        }
        this.handlerType = handlerType;
        return this;
    }

    @Override
    public TimerJobQueryImpl executable() {
        executable = true;
        return this;
    }

    @Override
    public TimerJobQueryImpl timers() {
        if (onlyMessages) {
            throw new FlowableIllegalArgumentException("Cannot combine onlyTimers() with onlyMessages() in the same query");
        }
        this.onlyTimers = true;
        return this;
    }

    @Override
    public TimerJobQueryImpl messages() {
        if (onlyTimers) {
            throw new FlowableIllegalArgumentException("Cannot combine onlyTimers() with onlyMessages() in the same query");
        }
        this.onlyMessages = true;
        return this;
    }

    @Override
    public TimerJobQueryImpl duedateHigherThan(Date date) {
        if (date == null) {
            throw new FlowableIllegalArgumentException("Provided date is null");
        }
        this.duedateHigherThan = date;
        return this;
    }

    @Override
    public TimerJobQueryImpl duedateLowerThan(Date date) {
        if (date == null) {
            throw new FlowableIllegalArgumentException("Provided date is null");
        }
        this.duedateLowerThan = date;
        return this;
    }

    @Override
    public TimerJobQueryImpl withException() {
        this.withException = true;
        return this;
    }

    @Override
    public TimerJobQueryImpl exceptionMessage(String exceptionMessage) {
        if (exceptionMessage == null) {
            throw new FlowableIllegalArgumentException("Provided exception message is null");
        }
        this.exceptionMessage = exceptionMessage;
        return this;
    }

    @Override
    public TimerJobQueryImpl jobTenantId(String tenantId) {
        if (tenantId == null) {
            throw new FlowableIllegalArgumentException("Provided tentant id is null");
        }
        this.tenantId = tenantId;
        return this;
    }

    @Override
    public TimerJobQueryImpl jobTenantIdLike(String tenantIdLike) {
        if (tenantIdLike == null) {
            throw new FlowableIllegalArgumentException("Provided tentant id is null");
        }
        this.tenantIdLike = tenantIdLike;
        return this;
    }

    @Override
    public TimerJobQueryImpl jobWithoutTenantId() {
        this.withoutTenantId = true;
        return this;
    }

    // sorting //////////////////////////////////////////

    @Override
    public TimerJobQuery orderByJobDuedate() {
        return orderBy(JobQueryProperty.DUEDATE);
    }

    @Override
    public TimerJobQuery orderByExecutionId() {
        return orderBy(JobQueryProperty.EXECUTION_ID);
    }

    @Override
    public TimerJobQuery orderByJobId() {
        return orderBy(JobQueryProperty.JOB_ID);
    }

    @Override
    public TimerJobQuery orderByProcessInstanceId() {
        return orderBy(JobQueryProperty.PROCESS_INSTANCE_ID);
    }

    @Override
    public TimerJobQuery orderByJobRetries() {
        return orderBy(JobQueryProperty.RETRIES);
    }

    @Override
    public TimerJobQuery orderByTenantId() {
        return orderBy(JobQueryProperty.TENANT_ID);
    }

    // results //////////////////////////////////////////

    @Override
    public long executeCount(CommandContext commandContext) {
        checkQueryOk();
        return CommandContextUtil.getTimerJobEntityManager(commandContext).findJobCountByQueryCriteria(this);
    }

    @Override
    public List<Job> executeList(CommandContext commandContext) {
        checkQueryOk();
        return CommandContextUtil.getTimerJobEntityManager(commandContext).findJobsByQueryCriteria(this);
    }

    // getters //////////////////////////////////////////

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public String getExecutionId() {
        return executionId;
    }
    
    public String getHandlerType() {
        return handlerType;
    }

    public boolean getExecutable() {
        return executable;
    }

    public Date getNow() {
        return CommandContextUtil.getJobServiceConfiguration().getClock().getCurrentTime();
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

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    public String getId() {
        return id;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }
    
    public String getScopeId() {
        return scopeId;
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

}
