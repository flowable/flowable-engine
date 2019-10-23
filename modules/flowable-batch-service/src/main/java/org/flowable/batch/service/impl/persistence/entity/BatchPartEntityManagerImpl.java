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

import org.flowable.batch.api.BatchPart;
import org.flowable.batch.service.BatchServiceConfiguration;
import org.flowable.batch.service.impl.persistence.entity.data.BatchPartDataManager;
import org.flowable.common.engine.impl.persistence.entity.AbstractServiceEngineEntityManager;

public class BatchPartEntityManagerImpl
    extends AbstractServiceEngineEntityManager<BatchServiceConfiguration, BatchPartEntity, BatchPartDataManager>
    implements BatchPartEntityManager {

    public BatchPartEntityManagerImpl(BatchServiceConfiguration batchServiceConfiguration, BatchPartDataManager batchPartDataManager) {
        super(batchServiceConfiguration, batchPartDataManager);
    }

    @Override
    public List<BatchPart> findBatchPartsByBatchId(String batchId) {
        return dataManager.findBatchPartsByBatchId(batchId);
    }
    
    @Override
    public List<BatchPart> findBatchPartsByBatchIdAndStatus(String batchId, String status) {
        return dataManager.findBatchPartsByBatchIdAndStatus(batchId, status);
    }
    
    @Override
    public List<BatchPart> findBatchPartsByScopeIdAndType(String scopeId, String scopeType) {
        return dataManager.findBatchPartsByScopeIdAndType(scopeId, scopeType);
    }

    @Override
    public BatchPartEntity createBatchPart(BatchEntity parentBatch, String status, String scopeId, String subScopeId, String scopeType) {
        BatchPartEntity batchPartEntity = dataManager.create();
        batchPartEntity.setBatchId(parentBatch.getId());
        batchPartEntity.setBatchType(parentBatch.getBatchType());
        batchPartEntity.setScopeId(scopeId);
        batchPartEntity.setSubScopeId(subScopeId);
        batchPartEntity.setScopeType(scopeType);
        batchPartEntity.setBatchSearchKey(parentBatch.getBatchSearchKey());
        batchPartEntity.setBatchSearchKey2(parentBatch.getBatchSearchKey2());
        batchPartEntity.setStatus(status);
        batchPartEntity.setCreateTime(getClock().getCurrentTime());
        insert(batchPartEntity);
        
        return batchPartEntity;
    }
    
    @Override
    public BatchPartEntity completeBatchPart(String batchPartId, String status, String resultJson) {
        BatchPartEntity batchPartEntity = getBatchPartEntityManager().findById(batchPartId);
        batchPartEntity.setCompleteTime(getClock().getCurrentTime());
        batchPartEntity.setStatus(status);
        batchPartEntity.setResultDocumentJson(resultJson);
        
        return batchPartEntity;
    }

    @Override
    public void deleteBatchPartEntityAndResources(BatchPartEntity batchPartEntity) {
        BatchByteArrayRef resultDocRefId = batchPartEntity.getResultDocRefId();

        if (resultDocRefId != null && resultDocRefId.getId() != null) {
            resultDocRefId.delete();
        }

        delete(batchPartEntity);
    }

    protected BatchPartEntityManager getBatchPartEntityManager() {
        return serviceConfiguration.getBatchPartEntityManager();
    }
}