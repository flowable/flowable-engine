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
import java.util.Date;
import java.util.List;

import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.common.impl.interceptor.CommandExecutor;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.runtime.Job;
import org.flowable.engine.runtime.SuspendedJobQuery;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class SuspendedJobQueryImpl extends AbstractQuery<SuspendedJobQuery, Job> implements SuspendedJobQuery, Serializable {

    private static final long serialVersionUID = 1L;
    protected String id;
    protected String processInstanceId;
    protected String executionId;
    protected String handlerType;
    protected String processDefinitionId;
    protected boolean retriesLeft;
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
    protected boolean noRetriesLeft;

    public SuspendedJobQueryImpl() {
    }

    public SuspendedJobQueryImpl(CommandContext commandContext) {
        super(commandContext);
    }

    public SuspendedJobQueryImpl(CommandExecutor commandExecutor) {
        super(commandExecutor);
    }

    public SuspendedJobQueryImpl jobId(String jobId) {
        if (jobId == null) {
            throw new FlowableIllegalArgumentException("Provided job id is null");
        }
        this.id = jobId;
        return this;
    }

    public SuspendedJobQueryImpl processInstanceId(String processInstanceId) {
        if (processInstanceId == null) {
            throw new FlowableIllegalArgumentException("Provided process instance id is null");
        }
        this.processInstanceId = processInstanceId;
        return this;
    }

    public SuspendedJobQueryImpl processDefinitionId(String processDefinitionId) {
        if (processDefinitionId == null) {
            throw new FlowableIllegalArgumentException("Provided process definition id is null");
        }
        this.processDefinitionId = processDefinitionId;
        return this;
    }

    public SuspendedJobQueryImpl executionId(String executionId) {
        if (executionId == null) {
            throw new FlowableIllegalArgumentException("Provided execution id is null");
        }
        this.executionId = executionId;
        return this;
    }

    public SuspendedJobQueryImpl handlerType(String handlerType) {
        if (handlerType == null) {
            throw new FlowableIllegalArgumentException("Provided handlerType is null");
        }
        this.handlerType = handlerType;
        return this;
    }

    public SuspendedJobQueryImpl withRetriesLeft() {
        retriesLeft = true;
        return this;
    }

    public SuspendedJobQueryImpl executable() {
        executable = true;
        return this;
    }

    public SuspendedJobQueryImpl timers() {
        if (onlyMessages) {
            throw new FlowableIllegalArgumentException("Cannot combine onlyTimers() with onlyMessages() in the same query");
        }
        this.onlyTimers = true;
        return this;
    }

    public SuspendedJobQueryImpl messages() {
        if (onlyTimers) {
            throw new FlowableIllegalArgumentException("Cannot combine onlyTimers() with onlyMessages() in the same query");
        }
        this.onlyMessages = true;
        return this;
    }

    public SuspendedJobQueryImpl duedateHigherThan(Date date) {
        if (date == null) {
            throw new FlowableIllegalArgumentException("Provided date is null");
        }
        this.duedateHigherThan = date;
        return this;
    }

    public SuspendedJobQueryImpl duedateLowerThan(Date date) {
        if (date == null) {
            throw new FlowableIllegalArgumentException("Provided date is null");
        }
        this.duedateLowerThan = date;
        return this;
    }

    public SuspendedJobQueryImpl duedateHigherThen(Date date) {
        return duedateHigherThan(date);
    }

    public SuspendedJobQueryImpl duedateHigherThenOrEquals(Date date) {
        if (date == null) {
            throw new FlowableIllegalArgumentException("Provided date is null");
        }
        this.duedateHigherThanOrEqual = date;
        return this;
    }

    public SuspendedJobQueryImpl duedateLowerThen(Date date) {
        return duedateLowerThan(date);
    }

    public SuspendedJobQueryImpl duedateLowerThenOrEquals(Date date) {
        if (date == null) {
            throw new FlowableIllegalArgumentException("Provided date is null");
        }
        this.duedateLowerThanOrEqual = date;
        return this;
    }

    public SuspendedJobQueryImpl noRetriesLeft() {
        noRetriesLeft = true;
        return this;
    }

    public SuspendedJobQueryImpl withException() {
        this.withException = true;
        return this;
    }

    public SuspendedJobQueryImpl exceptionMessage(String exceptionMessage) {
        if (exceptionMessage == null) {
            throw new FlowableIllegalArgumentException("Provided exception message is null");
        }
        this.exceptionMessage = exceptionMessage;
        return this;
    }

    public SuspendedJobQueryImpl jobTenantId(String tenantId) {
        if (tenantId == null) {
            throw new FlowableIllegalArgumentException("Provided tenant id is null");
        }
        this.tenantId = tenantId;
        return this;
    }

    public SuspendedJobQueryImpl jobTenantIdLike(String tenantIdLike) {
        if (tenantIdLike == null) {
            throw new FlowableIllegalArgumentException("Provided tenant id is null");
        }
        this.tenantIdLike = tenantIdLike;
        return this;
    }

    public SuspendedJobQueryImpl jobWithoutTenantId() {
        this.withoutTenantId = true;
        return this;
    }

    // sorting //////////////////////////////////////////

    public SuspendedJobQuery orderByJobDuedate() {
        return orderBy(JobQueryProperty.DUEDATE);
    }

    public SuspendedJobQuery orderByExecutionId() {
        return orderBy(JobQueryProperty.EXECUTION_ID);
    }

    public SuspendedJobQuery orderByJobId() {
        return orderBy(JobQueryProperty.JOB_ID);
    }

    public SuspendedJobQuery orderByProcessInstanceId() {
        return orderBy(JobQueryProperty.PROCESS_INSTANCE_ID);
    }

    public SuspendedJobQuery orderByJobRetries() {
        return orderBy(JobQueryProperty.RETRIES);
    }

    public SuspendedJobQuery orderByTenantId() {
        return orderBy(JobQueryProperty.TENANT_ID);
    }

    // results //////////////////////////////////////////

    public long executeCount(CommandContext commandContext) {
        checkQueryOk();
        return CommandContextUtil.getSuspendedJobEntityManager(commandContext).findJobCountByQueryCriteria(this);
    }

    public List<Job> executeList(CommandContext commandContext) {
        checkQueryOk();
        return CommandContextUtil.getSuspendedJobEntityManager(commandContext).findJobsByQueryCriteria(this);
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

    public boolean getRetriesLeft() {
        return retriesLeft;
    }

    public boolean getExecutable() {
        return executable;
    }

    public Date getNow() {
        return CommandContextUtil.getProcessEngineConfiguration().getClock().getCurrentTime();
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

    public boolean isNoRetriesLeft() {
        return noRetriesLeft;
    }

}
