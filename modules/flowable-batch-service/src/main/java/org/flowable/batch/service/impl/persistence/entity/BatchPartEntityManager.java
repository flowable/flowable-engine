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
import org.flowable.batch.api.BatchPartQuery;
import org.flowable.common.engine.impl.persistence.entity.EntityManager;

public interface BatchPartEntityManager extends EntityManager<BatchPartEntity> {
    
    List<BatchPart> findBatchPartsByBatchId(String batchId);
    
    List<BatchPart> findBatchPartsByBatchIdAndStatus(String batchId, String status);
    
    List<BatchPart> findBatchPartsByScopeIdAndType(String scopeId, String scopeType);

    List<BatchPart> findBatchPartsByQueryCriteria(BatchPartQuery batchPartQuery);

    long findBatchPartCountByQueryCriteria(BatchPartQuery batchPartQuery);

    BatchPartEntity createBatchPart(BatchEntity parentBatch, String status, String scopeId, String subScopeId, String scopeType);
    
    BatchPartEntity completeBatchPart(String batchPartId, String status, String resultJson);

    void deleteBatchPartEntityAndResources(BatchPartEntity batchPartEntity);
}