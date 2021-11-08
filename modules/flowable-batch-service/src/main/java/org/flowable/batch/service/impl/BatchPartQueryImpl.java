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
package org.flowable.batch.service.impl;

import java.util.List;

import org.flowable.batch.api.BatchPart;
import org.flowable.batch.api.BatchPartQuery;
import org.flowable.batch.service.BatchServiceConfiguration;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.query.AbstractQuery;

/**
 * @author Filip Hrisafov
 */
public class BatchPartQueryImpl extends AbstractQuery<BatchPartQuery, BatchPart> implements BatchPartQuery {

    protected final BatchServiceConfiguration batchServiceConfiguration;

    protected String id;
    protected String type;
    protected String batchId;
    protected String searchKey;
    protected String searchKey2;
    protected String batchType;
    protected String batchSearchKey;
    protected String batchSearchKey2;
    protected String status;
    protected String scopeId;
    protected String subScopeId;
    protected String scopeType;
    protected String tenantId;
    protected String tenantIdLike;
    protected boolean withoutTenantId;
    protected boolean completed;

    public BatchPartQueryImpl(CommandExecutor commandExecutor, BatchServiceConfiguration batchServiceConfiguration) {
        super(commandExecutor);
        this.batchServiceConfiguration = batchServiceConfiguration;
    }

    @Override
    public long executeCount(CommandContext commandContext) {
        return batchServiceConfiguration.getBatchPartEntityManager().findBatchPartCountByQueryCriteria(this);
    }

    @Override
    public List<BatchPart> executeList(CommandContext commandContext) {
        return batchServiceConfiguration.getBatchPartEntityManager().findBatchPartsByQueryCriteria(this);
    }

    @Override
    public BatchPartQuery id(String id) {
        if (id == null) {
            throw new FlowableIllegalArgumentException("id is null");
        }
        this.id = id;
        return this;
    }

    @Override
    public BatchPartQuery batchId(String batchId) {
        if (batchId == null) {
            throw new FlowableIllegalArgumentException("batchId is null");
        }
        this.batchId = batchId;
        return this;
    }

    @Override
    public BatchPartQuery type(String type) {
        if (type == null) {
            throw new FlowableIllegalArgumentException("type is null");
        }
        this.type = type;
        return this;
    }

    @Override
    public BatchPartQuery searchKey(String searchKey) {
        if (searchKey == null) {
            throw new FlowableIllegalArgumentException("searchKey is null");
        }
        this.searchKey = searchKey;
        return this;
    }

    @Override
    public BatchPartQuery searchKey2(String searchKey2) {
        if (searchKey2 == null) {
            throw new FlowableIllegalArgumentException("searchKey2 is null");
        }
        this.searchKey2 = searchKey2;
        return this;
    }

    @Override
    public BatchPartQuery batchType(String batchType) {
        if (batchType == null) {
            throw new FlowableIllegalArgumentException("batchType is null");
        }
        this.batchType = batchType;
        return this;
    }

    @Override
    public BatchPartQuery batchSearchKey(String searchKey) {
        if (searchKey == null) {
            throw new FlowableIllegalArgumentException("batchSearchKey is null");
        }
        this.batchSearchKey = searchKey;
        return this;
    }

    @Override
    public BatchPartQuery batchSearchKey2(String searchKey2) {
        if (searchKey2 == null) {
            throw new FlowableIllegalArgumentException("batchSearchKey2 is null");
        }
        this.batchSearchKey2 = searchKey2;
        return this;
    }

    @Override
    public BatchPartQuery status(String status) {
        if (status == null) {
            throw new FlowableIllegalArgumentException("status is null");
        }
        this.status = status;
        return this;
    }

    @Override
    public BatchPartQuery scopeId(String scopeId) {
        if (scopeId == null) {
            throw new FlowableIllegalArgumentException("scopeId is null");
        }
        this.scopeId = scopeId;
        return this;
    }

    @Override
    public BatchPartQuery subScopeId(String subScopeId) {
        if (subScopeId == null) {
            throw new FlowableIllegalArgumentException("subScopeId is null");
        }
        this.subScopeId = subScopeId;
        return this;
    }

    @Override
    public BatchPartQuery scopeType(String scopeType) {
        if (scopeType == null) {
            throw new FlowableIllegalArgumentException("scopeType is null");
        }
        this.scopeType = scopeType;
        return this;
    }

    @Override
    public BatchPartQuery tenantId(String tenantId) {
        if (tenantId == null) {
            throw new FlowableIllegalArgumentException("tenantId is null");
        }
        this.tenantId = tenantId;
        return this;
    }

    @Override
    public BatchPartQuery tenantIdLike(String tenantIdLike) {
        if (tenantIdLike == null) {
            throw new FlowableIllegalArgumentException("tenantIdLike is null");
        }
        this.tenantIdLike = tenantIdLike;
        return this;
    }

    @Override
    public BatchPartQuery withoutTenantId() {
        this.withoutTenantId = true;
        return this;
    }

    @Override
    public BatchPartQuery completed() {
        this.completed = true;
        return this;
    }

    @Override
    public BatchPartQuery orderByBatchId() {
        return orderBy(BatchPartQueryProperty.BATCH_ID);
    }

    @Override
    public BatchPartQuery orderByCreateTime() {
        return orderBy(BatchPartQueryProperty.CREATE_TIME);
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getBatchId() {
        return batchId;
    }

    public String getSearchKey() {
        return searchKey;
    }

    public String getSearchKey2() {
        return searchKey2;
    }

    public String getBatchType() {
        return batchType;
    }

    public String getBatchSearchKey() {
        return batchSearchKey;
    }

    public String getBatchSearchKey2() {
        return batchSearchKey2;
    }

    public String getStatus() {
        return status;
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

    public String getTenantId() {
        return tenantId;
    }

    public String getTenantIdLike() {
        return tenantIdLike;
    }

    public boolean isWithoutTenantId() {
        return withoutTenantId;
    }

    public boolean isCompleted() {
        return completed;
    }
}
