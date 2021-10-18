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

package org.flowable.batch.service.impl.persistence.entity;

import java.util.List;

import org.flowable.batch.api.Batch;
import org.flowable.batch.api.BatchBuilder;
import org.flowable.batch.api.BatchPart;
import org.flowable.batch.service.BatchServiceConfiguration;
import org.flowable.batch.service.impl.BatchQueryImpl;
import org.flowable.batch.service.impl.persistence.entity.data.BatchDataManager;
import org.flowable.common.engine.impl.persistence.entity.AbstractServiceEngineEntityManager;
import org.flowable.common.engine.impl.persistence.entity.ByteArrayRef;

public class BatchEntityManagerImpl
    extends AbstractServiceEngineEntityManager<BatchServiceConfiguration, BatchEntity, BatchDataManager>
    implements BatchEntityManager {

    public BatchEntityManagerImpl(BatchServiceConfiguration batchServiceConfiguration, BatchDataManager batchDataManager) {
        super(batchServiceConfiguration, batchServiceConfiguration.getEngineName(), batchDataManager);
    }
    
    @Override
    public List<Batch> findBatchesBySearchKey(String searchKey) {
        return dataManager.findBatchesBySearchKey(searchKey);
    }
    
    @Override
    public List<Batch> findAllBatches() {
        return dataManager.findAllBatches();
    }
    
    @Override
    public List<Batch> findBatchesByQueryCriteria(BatchQueryImpl batchQuery) {
        return dataManager.findBatchesByQueryCriteria(batchQuery);
    }

    @Override
    public long findBatchCountByQueryCriteria(BatchQueryImpl batchQuery) {
        return dataManager.findBatchCountByQueryCriteria(batchQuery);
    }

    @Override
    public BatchEntity createBatch(BatchBuilder batchBuilder) {
        BatchEntity batchEntity = dataManager.create();
        batchEntity.setBatchType(batchBuilder.getBatchType());
        batchEntity.setBatchSearchKey(batchBuilder.getSearchKey());
        batchEntity.setBatchSearchKey2(batchBuilder.getSearchKey2());
        batchEntity.setCreateTime(getClock().getCurrentTime());
        batchEntity.setStatus(batchBuilder.getStatus());
        batchEntity.setBatchDocumentJson(batchBuilder.getBatchDocumentJson(), serviceConfiguration.getEngineName());
        batchEntity.setTenantId(batchBuilder.getTenantId());
        
        dataManager.insert(batchEntity);
        
        return batchEntity;
    }

    @Override
    public Batch completeBatch(String batchId, String status) {
        BatchEntity batchEntity = findById(batchId);

        batchEntity.setCompleteTime(getClock().getCurrentTime());
        batchEntity.setStatus(status);

        return batchEntity;
    }

    @Override
    public void deleteBatches(BatchQueryImpl batchQuery) {
        dataManager.deleteBatches(batchQuery);
    }

    @Override
    public void delete(String batchId) {
        BatchEntity batch = dataManager.findById(batchId);
        List<BatchPart> batchParts = getBatchPartEntityManager().findBatchPartsByBatchId(batch.getId());
        if (batchParts != null && batchParts.size() > 0) {
            for (BatchPart batchPart : batchParts) {
                getBatchPartEntityManager().deleteBatchPartEntityAndResources((BatchPartEntity) batchPart);
            }
        }

        ByteArrayRef batchDocRefId = batch.getBatchDocRefId();

        if (batchDocRefId != null && batchDocRefId.getId() != null) {
            batchDocRefId.delete(serviceConfiguration.getEngineName());
        }

        delete(batch);
    }

    protected BatchPartEntityManager getBatchPartEntityManager() {
        return serviceConfiguration.getBatchPartEntityManager();
    }
}