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
package org.flowable.batch.api;

import java.util.List;

/**
 * Service which provides access to batch entities.
 * 
 * @author Tijs Rademakers
 */
public interface BatchService {
    
    Batch getBatch(String id);
    
    List<Batch> getAllBatches();
    
    List<Batch> findBatchesBySearchKey(String searchKey);
    
    List<Batch> findBatchesByQueryCriteria(BatchQuery batchQuery);

    long findBatchCountByQueryCriteria(BatchQuery batchQuery);
    
    BatchBuilder createBatchBuilder();
    
    void insertBatch(Batch batch);
    
    Batch updateBatch(Batch batch);
    
    void deleteBatch(String batchId);
    
    BatchPart getBatchPart(String id);
    
    List<BatchPart> findBatchPartsByBatchId(String batchId);
    
    List<BatchPart> findBatchPartsByBatchIdAndStatus(String batchId, String status);
    
    List<BatchPart> findBatchPartsByScopeIdAndType(String scopeId, String scopeType);
    
    BatchPart createBatchPart(Batch batch, String status, String scopeId, String subScopeId, String scopeType);
    
    BatchPart completeBatchPart(String batchPartId, String status, String resultJson);
    
    Batch completeBatch(String batchId, String status);

}
