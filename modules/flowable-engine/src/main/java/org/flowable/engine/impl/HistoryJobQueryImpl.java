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
import org.flowable.engine.runtime.HistoryJob;
import org.flowable.engine.runtime.HistoryJobQuery;

/**
 * @author Joram Barrez
 * @author Tom Baeyens
 * @author Falko Menge
 */
public class HistoryJobQueryImpl extends AbstractQuery<HistoryJobQuery, HistoryJob> implements HistoryJobQuery, Serializable {

    private static final long serialVersionUID = 1L;
    protected String id;
    protected String handlerType;
    protected boolean retriesLeft;
    protected boolean withException;
    protected String exceptionMessage;
    protected String tenantId;
    protected String tenantIdLike;
    protected boolean withoutTenantId;
    protected boolean noRetriesLeft;
    protected String lockOwner;
    protected boolean onlyLocked;
    protected boolean onlyUnlocked;

    public HistoryJobQueryImpl() {
    }

    public HistoryJobQueryImpl(CommandContext commandContext) {
        super(commandContext);
    }

    public HistoryJobQueryImpl(CommandExecutor commandExecutor) {
        super(commandExecutor);
    }

    public HistoryJobQuery jobId(String jobId) {
        if (jobId == null) {
            throw new FlowableIllegalArgumentException("Provided job id is null");
        }
        this.id = jobId;
        return this;
    }

    public HistoryJobQuery handlerType(String handlerType) {
        if (handlerType == null) {
            throw new FlowableIllegalArgumentException("Provided handlerType is null");
        }
        this.handlerType = handlerType;
        return this;
    }

    public HistoryJobQuery withRetriesLeft() {
        retriesLeft = true;
        return this;
    }

    public HistoryJobQuery noRetriesLeft() {
        noRetriesLeft = true;
        return this;
    }

    public HistoryJobQuery withException() {
        this.withException = true;
        return this;
    }

    public HistoryJobQuery exceptionMessage(String exceptionMessage) {
        if (exceptionMessage == null) {
            throw new FlowableIllegalArgumentException("Provided exception message is null");
        }
        this.exceptionMessage = exceptionMessage;
        return this;
    }

    public HistoryJobQuery jobTenantId(String tenantId) {
        if (tenantId == null) {
            throw new FlowableIllegalArgumentException("Provided tenant id is null");
        }
        this.tenantId = tenantId;
        return this;
    }

    public HistoryJobQuery jobTenantIdLike(String tenantIdLike) {
        if (tenantIdLike == null) {
            throw new FlowableIllegalArgumentException("Provided tenant id is null");
        }
        this.tenantIdLike = tenantIdLike;
        return this;
    }

    public HistoryJobQuery jobWithoutTenantId() {
        this.withoutTenantId = true;
        return this;
    }

    public HistoryJobQuery lockOwner(String lockOwner) {
        this.lockOwner = lockOwner;
        return this;
    }

    public HistoryJobQuery locked() {
        this.onlyLocked = true;
        return this;
    }

    public HistoryJobQuery unlocked() {
        this.onlyUnlocked = true;
        return this;
    }

    // sorting //////////////////////////////////////////

    public HistoryJobQuery orderByJobDuedate() {
        return orderBy(JobQueryProperty.DUEDATE);
    }

    public HistoryJobQuery orderByExecutionId() {
        return orderBy(JobQueryProperty.EXECUTION_ID);
    }

    public HistoryJobQuery orderByJobId() {
        return orderBy(JobQueryProperty.JOB_ID);
    }

    public HistoryJobQuery orderByProcessInstanceId() {
        return orderBy(JobQueryProperty.PROCESS_INSTANCE_ID);
    }

    public HistoryJobQuery orderByJobRetries() {
        return orderBy(JobQueryProperty.RETRIES);
    }

    public HistoryJobQuery orderByTenantId() {
        return orderBy(JobQueryProperty.TENANT_ID);
    }

    // results //////////////////////////////////////////

    public long executeCount(CommandContext commandContext) {
        checkQueryOk();
        return CommandContextUtil.getHistoryJobEntityManager(commandContext).findHistoryJobCountByQueryCriteria(this);
    }

    public List<HistoryJob> executeList(CommandContext commandContext) {
        checkQueryOk();
        return CommandContextUtil.getHistoryJobEntityManager(commandContext).findHistoryJobsByQueryCriteria(this);
    }

    // getters //////////////////////////////////////////

    public String getHandlerType() {
        return this.handlerType;
    }

    public boolean getRetriesLeft() {
        return retriesLeft;
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

    public boolean isNoRetriesLeft() {
        return noRetriesLeft;
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
