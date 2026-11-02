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
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.query.AbstractQuery;
import org.flowable.job.api.HistoryJob;
import org.flowable.job.api.HistoryJobQuery;
import org.flowable.job.service.JobServiceConfiguration;

/**
 * @author Joram Barrez
 * @author Tom Baeyens
 * @author Falko Menge
 */
public class HistoryJobQueryImpl extends AbstractQuery<HistoryJobQuery, HistoryJob> implements HistoryJobQuery, Serializable {

    private static final long serialVersionUID = 1L;

    protected JobServiceConfiguration jobServiceConfiguration;

    protected String id;
    protected String handlerType;
    protected Collection<String> handlerTypes;
    protected boolean withException;
    protected String exceptionMessage;
    protected String scopeType;
    protected String tenantId;
    protected String tenantIdLike;
    protected boolean withoutTenantId;
    protected String lockOwner;
    protected boolean onlyLocked;
    protected boolean onlyUnlocked;
    protected boolean withoutScopeType;

    protected List<HistoryJobQueryImpl> orQueryObjects = new ArrayList<>();
    protected HistoryJobQueryImpl currentOrQueryObject;
    protected boolean inOrStatement;

    public HistoryJobQueryImpl() {
    }

    public HistoryJobQueryImpl(CommandContext commandContext, JobServiceConfiguration jobServiceConfiguration) {
        super(commandContext);
        this.jobServiceConfiguration = jobServiceConfiguration;
    }

    public HistoryJobQueryImpl(CommandExecutor commandExecutor, JobServiceConfiguration jobServiceConfiguration) {
        super(commandExecutor);
        this.jobServiceConfiguration = jobServiceConfiguration;
    }

    @Override
    public HistoryJobQuery jobId(String jobId) {
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
    public HistoryJobQuery handlerType(String handlerType) {
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
    public HistoryJobQuery handlerTypes(Collection<String> handlerTypes) {
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
    public HistoryJobQuery withException() {
        if (inOrStatement) {
            this.currentOrQueryObject.withException = true;
        } else {
            this.withException = true;
        }
        return this;
    }

    @Override
    public HistoryJobQuery exceptionMessage(String exceptionMessage) {
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
    public HistoryJobQuery scopeType(String scopeType) {
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
    public HistoryJobQuery jobTenantId(String tenantId) {
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
    public HistoryJobQuery jobTenantIdLike(String tenantIdLike) {
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
    public HistoryJobQuery jobWithoutTenantId() {
        if (inOrStatement) {
            this.currentOrQueryObject.withoutTenantId = true;
        } else {
            this.withoutTenantId = true;
        }
        return this;
    }

    @Override
    public HistoryJobQuery lockOwner(String lockOwner) {
        if (inOrStatement) {
            this.currentOrQueryObject.lockOwner = lockOwner;
        } else {
            this.lockOwner = lockOwner;
        }
        return this;
    }

    @Override
    public HistoryJobQuery locked() {
        if (inOrStatement) {
            this.currentOrQueryObject.onlyLocked = true;
        } else {
            this.onlyLocked = true;
        }
        return this;
    }

    @Override
    public HistoryJobQuery unlocked() {
        if (inOrStatement) {
            this.currentOrQueryObject.onlyUnlocked = true;
        } else {
            this.onlyUnlocked = true;
        }
        return this;
    }

    @Override
    public HistoryJobQuery withoutScopeType() {
        if (inOrStatement) {
            this.currentOrQueryObject.withoutScopeType = true;
        } else {
            this.withoutScopeType = true;
        }
        return this;
    }

    @Override
    public HistoryJobQuery or() {
        if (inOrStatement) {
            throw new FlowableException("the query is already in an or statement");
        }
        inOrStatement = true;
        if (commandContext != null) {
            currentOrQueryObject = new HistoryJobQueryImpl(commandContext, jobServiceConfiguration);
        } else {
            currentOrQueryObject = new HistoryJobQueryImpl(commandExecutor, jobServiceConfiguration);
        }
        orQueryObjects.add(currentOrQueryObject);
        return this;
    }

    @Override
    public HistoryJobQuery endOr() {
        if (!inOrStatement) {
            throw new FlowableException("endOr() can only be called after calling or()");
        }
        inOrStatement = false;
        currentOrQueryObject = null;
        return this;
    }

    // sorting //////////////////////////////////////////

    @Override
    public HistoryJobQuery orderByJobId() {
        return orderBy(JobQueryProperty.JOB_ID);
    }

    @Override
    public HistoryJobQuery orderByJobRetries() {
        return orderBy(JobQueryProperty.RETRIES);
    }

    @Override
    public HistoryJobQuery orderByTenantId() {
        return orderBy(JobQueryProperty.TENANT_ID);
    }

    // results //////////////////////////////////////////

    @Override
    public long executeCount(CommandContext commandContext) {
        return jobServiceConfiguration.getHistoryJobEntityManager().findHistoryJobCountByQueryCriteria(this);
    }

    @Override
    public List<HistoryJob> executeList(CommandContext commandContext) {
        return jobServiceConfiguration.getHistoryJobEntityManager().findHistoryJobsByQueryCriteria(this);
    }

    // getters //////////////////////////////////////////

    public String getHandlerType() {
        return this.handlerType;
    }

    public Collection<String> getHandlerTypes() {
        return handlerTypes;
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

    public String getScopeType() {
        return scopeType;
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

    public List<HistoryJobQueryImpl> getOrQueryObjects() {
        return orQueryObjects;
    }

}
