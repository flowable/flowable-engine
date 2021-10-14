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
package org.flowable.batch.service;

import org.flowable.batch.api.Batch;
import org.flowable.batch.api.BatchPart;
import org.flowable.batch.api.BatchPartBuilder;
import org.flowable.batch.service.impl.persistence.entity.BatchPartEntity;
import org.flowable.batch.service.impl.persistence.entity.BatchPartEntityManager;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;

/**
 * @author Filip Hrisafov
 */
public class BatchPartBuilderImpl implements BatchPartBuilder {

    protected final Batch batch;
    protected final BatchServiceConfiguration batchServiceConfiguration;
    protected final CommandExecutor commandExecutor;

    protected String type;
    protected String searchKey;
    protected String searchKey2;
    protected String status;
    protected String scopeId;
    protected String subScopeId;
    protected String scopeType;

    public BatchPartBuilderImpl(Batch batch, BatchServiceConfiguration batchServiceConfiguration) {
        this(batch, batchServiceConfiguration, null);
    }

    public BatchPartBuilderImpl(Batch batch, BatchServiceConfiguration batchServiceConfiguration, CommandExecutor commandExecutor) {
        this.batch = batch;
        this.commandExecutor = commandExecutor;
        this.batchServiceConfiguration = batchServiceConfiguration;
    }

    @Override
    public BatchPartBuilder type(String type) {
        if (type == null) {
            throw new FlowableIllegalArgumentException("type is null");
        }
        this.type = type;
        return this;
    }

    @Override
    public BatchPartBuilder searchKey(String searchKey) {
        this.searchKey = searchKey;
        return this;
    }

    @Override
    public BatchPartBuilder searchKey2(String searchKey2) {
        this.searchKey2 = searchKey2;
        return this;
    }

    @Override
    public BatchPartBuilder status(String status) {
        if (status == null) {
            throw new FlowableIllegalArgumentException("status is null");
        }
        this.status = status;
        return this;
    }

    @Override
    public BatchPartBuilder scopeId(String scopeId) {
        if (scopeId == null) {
            throw new FlowableIllegalArgumentException("scopeId is null");
        }
        this.scopeId = scopeId;
        return this;
    }

    @Override
    public BatchPartBuilder subScopeId(String subScopeId) {
        if (subScopeId == null) {
            throw new FlowableIllegalArgumentException("subScopeId is null");
        }
        this.subScopeId = subScopeId;
        return this;
    }

    @Override
    public BatchPartBuilder scopeType(String scopeType) {
        if (scopeType == null) {
            throw new FlowableIllegalArgumentException("scopeType is null");
        }
        this.scopeType = scopeType;
        return this;
    }

    @Override
    public BatchPart create() {
        if (batch == null) {
            throw new FlowableIllegalArgumentException("batch has to be provided");
        }

        if (type == null) {
            throw new FlowableIllegalArgumentException("type has to be provided");
        }
        if (commandExecutor != null) {
            return commandExecutor.execute(commandContext -> createSafe());
        } else {
            return createSafe();
        }
    }

    protected BatchPart createSafe() {
        BatchPartEntityManager partEntityManager = batchServiceConfiguration.getBatchPartEntityManager();
        BatchPartEntity batchPart = partEntityManager.create();
        batchPart.setBatchId(batch.getId());
        batchPart.setBatchType(batch.getBatchType());
        batchPart.setBatchSearchKey(batch.getBatchSearchKey());
        batchPart.setBatchSearchKey2(batch.getBatchSearchKey2());
        if (batch.getTenantId() != null) {
            batchPart.setTenantId(batch.getTenantId());
        }

        batchPart.setType(type);
        batchPart.setSearchKey(searchKey);
        batchPart.setSearchKey2(searchKey2);
        batchPart.setStatus(status);
        batchPart.setScopeId(scopeId);
        batchPart.setSubScopeId(subScopeId);
        batchPart.setScopeType(scopeType);
        batchPart.setCreateTime(batchServiceConfiguration.getClock().getCurrentTime());
        partEntityManager.insert(batchPart);

        return batchPart;
    }

}
