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

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.flowable.batch.api.Batch;
import org.flowable.batch.api.BatchQuery;
import org.flowable.batch.service.BatchServiceConfiguration;
import org.flowable.batch.service.impl.persistence.entity.BatchEntity;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.query.CacheAwareQuery;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.query.AbstractQuery;

public class BatchQueryImpl extends AbstractQuery<BatchQuery, Batch> implements BatchQuery, CacheAwareQuery<BatchEntity>, Serializable {

    private static final long serialVersionUID = 1L;
    
    protected BatchServiceConfiguration batchServiceConfiguration;
    
    protected String id;
    protected String batchType;
    protected String searchKey;
    protected String searchKey2;
    protected Date createTimeHigherThan;
    protected Date createTimeLowerThan;
    protected Date completeTimeHigherThan;
    protected Date completeTimeLowerThan;
    protected String status;
    protected String tenantId;
    protected String tenantIdLike;
    protected boolean withoutTenantId;

    public BatchQueryImpl() {
    }

    public BatchQueryImpl(CommandContext commandContext, BatchServiceConfiguration batchServiceConfiguration) {
        super(commandContext);
        this.batchServiceConfiguration = batchServiceConfiguration;
    }

    public BatchQueryImpl(CommandExecutor commandExecutor, BatchServiceConfiguration batchServiceConfiguration) {
        super(commandExecutor);
        this.batchServiceConfiguration = batchServiceConfiguration;
    }

    @Override
    public BatchQuery batchId(String batchId) {
        if (batchId == null) {
            throw new FlowableIllegalArgumentException("Provided batch id is null");
        }
        this.id = batchId;
        return this;
    }

    @Override
    public BatchQuery batchType(String batchType) {
        if (batchType == null) {
            throw new FlowableIllegalArgumentException("Provided batch type is null");
        }
        this.batchType = batchType;
        return this;
    }

    @Override
    public BatchQuery searchKey(String searchKey) {
        if (searchKey == null) {
            throw new FlowableIllegalArgumentException("Provided search key is null");
        }
        this.searchKey = searchKey;
        return this;
    }
    
    @Override
    public BatchQuery searchKey2(String searchKey) {
        if (searchKey == null) {
            throw new FlowableIllegalArgumentException("Provided search key is null");
        }
        this.searchKey2 = searchKey;
        return this;
    }
    
    @Override
    public BatchQuery createTimeHigherThan(Date date) {
        if (date == null) {
            throw new FlowableIllegalArgumentException("Provided date is null");
        }
        this.createTimeHigherThan = date;
        return this;
    }

    @Override
    public BatchQuery createTimeLowerThan(Date date) {
        if (date == null) {
            throw new FlowableIllegalArgumentException("Provided date is null");
        }
        this.createTimeLowerThan = date;
        return this;
    }
    
    @Override
    public BatchQuery completeTimeHigherThan(Date date) {
        if (date == null) {
            throw new FlowableIllegalArgumentException("Provided date is null");
        }
        this.completeTimeHigherThan = date;
        return this;
    }

    @Override
    public BatchQuery completeTimeLowerThan(Date date) {
        if (date == null) {
            throw new FlowableIllegalArgumentException("Provided date is null");
        }
        this.completeTimeLowerThan = date;
        return this;
    }
    
    @Override
    public BatchQuery status(String status) {
        if (status == null) {
            throw new FlowableIllegalArgumentException("Provided status is null");
        }
        this.status = status;
        return this;
    }
    
    @Override
    public BatchQuery tenantId(String tenantId) {
        if (tenantId == null) {
            throw new FlowableIllegalArgumentException("Provided tenant id is null");
        }
        this.tenantId = tenantId;
        return this;
    }

    @Override
    public BatchQuery tenantIdLike(String tenantIdLike) {
        if (tenantIdLike == null) {
            throw new FlowableIllegalArgumentException("Provided tenant id is null");
        }
        this.tenantIdLike = tenantIdLike;
        return this;
    }

    @Override
    public BatchQuery withoutTenantId() {
        this.withoutTenantId = true;
        return this;
    }

    // sorting //////////////////////////////////////////

    @Override
    public BatchQuery orderByBatchCreateTime() {
        return orderBy(BatchQueryProperty.CREATETIME);
    }

    @Override
    public BatchQuery orderByBatchId() {
        return orderBy(BatchQueryProperty.BATCH_ID);
    }

    @Override
    public BatchQuery orderByBatchTenantId() {
        return orderBy(BatchQueryProperty.TENANT_ID);
    }

    // results //////////////////////////////////////////

    @Override
    public long executeCount(CommandContext commandContext) {
        return batchServiceConfiguration.getBatchEntityManager().findBatchCountByQueryCriteria(this);
    }

    @Override
    public List<Batch> executeList(CommandContext commandContext) {
        return batchServiceConfiguration.getBatchEntityManager().findBatchesByQueryCriteria(this);
    }
    
    // getters //////////////////////////////////////////

    @Override
    public String getId() {
        return id;
    }

    public String getBatchType() {
        return batchType;
    }

    public String getSearchKey() {
        return searchKey;
    }

    public String getSearchKey2() {
        return searchKey2;
    }

    public Date getCreateTimeHigherThan() {
        return createTimeHigherThan;
    }

    public Date getCreateTimeLowerThan() {
        return createTimeLowerThan;
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
    
}
