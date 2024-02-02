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

import org.flowable.batch.api.Batch;
import org.flowable.batch.api.BatchBuilder;
import org.flowable.batch.api.BatchPart;
import org.flowable.batch.api.BatchQuery;
import org.flowable.batch.api.BatchService;
import org.flowable.batch.service.BatchServiceConfiguration;
import org.flowable.batch.service.impl.persistence.entity.BatchEntity;
import org.flowable.batch.service.impl.persistence.entity.BatchEntityManager;
import org.flowable.batch.service.impl.persistence.entity.BatchPartEntity;
import org.flowable.batch.service.impl.persistence.entity.BatchPartEntityManager;
import org.flowable.common.engine.impl.service.CommonServiceImpl;

/**
 * @author Tijs Rademakers
 */
public class BatchServiceImpl extends CommonServiceImpl<BatchServiceConfiguration> implements BatchService {

    public BatchServiceImpl(BatchServiceConfiguration batchServiceConfiguration) {
        super(batchServiceConfiguration);
    }
    
    @Override
    public BatchEntity getBatch(String id) {
        return getBatchEntityManager().findById(id);
    }
    
    @Override
    public List<Batch> findBatchesBySearchKey(String searchKey) {
        return getBatchEntityManager().findBatchesBySearchKey(searchKey);
    }
    
    @Override
    public List<Batch> getAllBatches() {
        return getBatchEntityManager().findAllBatches();
    }
    
    @Override
    public List<Batch> findBatchesByQueryCriteria(BatchQuery batchQuery) {
        return getBatchEntityManager().findBatchesByQueryCriteria((BatchQueryImpl) batchQuery);
    }

    @Override
    public long findBatchCountByQueryCriteria(BatchQuery batchQuery) {
        return getBatchEntityManager().findBatchCountByQueryCriteria((BatchQueryImpl) batchQuery);
    }
    
    @Override
    public BatchBuilder createBatchBuilder() {
        return new BatchBuilderImpl(configuration);
    }
    
    @Override
    public void insertBatch(Batch batch) {
        getBatchEntityManager().insert((BatchEntity) batch);
    }
    
    @Override
    public Batch updateBatch(Batch batch) {
        return getBatchEntityManager().update((BatchEntity) batch);
    }
    
    @Override
    public void deleteBatch(String batchId) {
        getBatchEntityManager().delete(batchId);
    }
    
    @Override
    public BatchPartEntity getBatchPart(String id) {
        return getBatchPartEntityManager().findById(id);
    }
    
    @Override
    public List<BatchPart> findBatchPartsByBatchId(String batchId) {
        return getBatchPartEntityManager().findBatchPartsByBatchId(batchId);
    }
    
    @Override
    public List<BatchPart> findBatchPartsByBatchIdAndStatus(String batchId, String status) {
        return getBatchPartEntityManager().findBatchPartsByBatchIdAndStatus(batchId, status);
    }
    
    @Override
    public List<BatchPart> findBatchPartsByScopeIdAndType(String scopeId, String scopeType) {
        return getBatchPartEntityManager().findBatchPartsByScopeIdAndType(scopeId, scopeType);
    }
    
    @Override
    public BatchPart createBatchPart(Batch batch, String status, String scopeId, String subScopeId, String scopeType) {
        return getBatchPartEntityManager().createBatchPart((BatchEntity) batch, status, scopeId, subScopeId, scopeType);
    }
    
    @Override
    public BatchPart completeBatchPart(String batchPartId, String status, String resultJson) {
        return getBatchPartEntityManager().completeBatchPart(batchPartId, status, resultJson);
    }
    
    @Override
    public Batch completeBatch(String batchId, String status) {
        return getBatchEntityManager().completeBatch(batchId, status);
    }

    public Batch createBatch(BatchBuilder batchBuilder) {
        return getBatchEntityManager().createBatch(batchBuilder);
    }
    
    public BatchEntityManager getBatchEntityManager() {
        return configuration.getBatchEntityManager();
    }
    
    public BatchPartEntityManager getBatchPartEntityManager() {
        return configuration.getBatchPartEntityManager();
    }
}
