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

import org.flowable.batch.api.Batch;
import org.flowable.batch.api.BatchBuilder;
import org.flowable.batch.service.BatchServiceConfiguration;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;

/**
 * @author Tijs Rademakers
 */
public class BatchBuilderImpl implements BatchBuilder {
    
    protected BatchServiceConfiguration batchServiceConfiguration;
    protected CommandExecutor commandExecutor;
    
    protected String batchType;
    protected String searchKey;
    protected String searchKey2;
    protected String status;
    protected String batchDocumentJson;
    protected String tenantId;
    
    public BatchBuilderImpl() {}
    
    public BatchBuilderImpl(CommandExecutor commandExecutor, BatchServiceConfiguration batchServiceConfiguration) {
        this.commandExecutor = commandExecutor;
        this.batchServiceConfiguration = batchServiceConfiguration;
    }
    
    public BatchBuilderImpl(BatchServiceConfiguration batchServiceConfiguration) {
        this.batchServiceConfiguration = batchServiceConfiguration;
    }

    @Override
    public BatchBuilder batchType(String batchType) {
        this.batchType = batchType;
        return this;
    }

    @Override
    public BatchBuilder searchKey(String searchKey) {
        this.searchKey = searchKey;
        return this;
    }

    @Override
    public BatchBuilder searchKey2(String searchKey2) {
        this.searchKey2 = searchKey2;
        return this;
    }
    
    @Override
    public BatchBuilder status(String status) {
        this.status = status;
        return this;
    }

    @Override
    public BatchBuilder batchDocumentJson(String batchDocumentJson) {
        this.batchDocumentJson = batchDocumentJson;
        return this;
    }
    
    @Override
    public BatchBuilder tenantId(String tenantId) {
        this.tenantId = tenantId;
        return this;
    }

    @Override
    public Batch create() {
        if (commandExecutor != null) {
            BatchBuilder selfBatchBuilder = this;
            return commandExecutor.execute(new Command<Batch>() {

                @Override
                public Batch execute(CommandContext commandContext) {
                    return batchServiceConfiguration.getBatchEntityManager().createBatch(selfBatchBuilder);
                }
            });
            
        } else {
            return ((BatchServiceImpl) batchServiceConfiguration.getBatchService()).createBatch(this);
        }
    }

    @Override
    public String getBatchType() {
        return batchType;
    }

    @Override
    public String getSearchKey() {
        return searchKey;
    }

    @Override
    public String getSearchKey2() {
        return searchKey2;
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public String getBatchDocumentJson() {
        return batchDocumentJson;
    }
    
    @Override
    public String getTenantId() {
        return tenantId;
    }
}
