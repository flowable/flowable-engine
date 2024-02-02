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
package org.flowable.batch.service.impl.persistence.entity.data.impl;

import java.util.HashMap;
import java.util.List;

import org.flowable.batch.api.Batch;
import org.flowable.batch.service.BatchServiceConfiguration;
import org.flowable.batch.service.impl.BatchQueryImpl;
import org.flowable.batch.service.impl.persistence.entity.BatchEntity;
import org.flowable.batch.service.impl.persistence.entity.BatchEntityImpl;
import org.flowable.batch.service.impl.persistence.entity.data.BatchDataManager;
import org.flowable.common.engine.impl.cfg.IdGenerator;
import org.flowable.common.engine.impl.db.AbstractDataManager;

public class MybatisBatchDataManager extends AbstractDataManager<BatchEntity> implements BatchDataManager {

    protected BatchServiceConfiguration batchServiceConfiguration;
    
    public MybatisBatchDataManager(BatchServiceConfiguration batchServiceConfiguration) {
        this.batchServiceConfiguration = batchServiceConfiguration;
    }
    
    @Override
    public Class<? extends BatchEntity> getManagedEntityClass() {
        return BatchEntityImpl.class;
    }

    @Override
    public BatchEntity create() {
        return new BatchEntityImpl();
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public List<Batch> findBatchesBySearchKey(String searchKey) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("searchKey", searchKey);
        params.put("searchKey2", searchKey);
        
        return getDbSqlSession().selectList("selectBatchesBySearchKey", params);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Batch> findAllBatches() {
        return getDbSqlSession().selectList("selectAllBatches");
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public List<Batch> findBatchesByQueryCriteria(BatchQueryImpl batchQuery) {
        return getDbSqlSession().selectList("selectBatchByQueryCriteria", batchQuery, getManagedEntityClass());
    }

    @Override
    public long findBatchCountByQueryCriteria(BatchQueryImpl batchQuery) {
        return (Long) getDbSqlSession().selectOne("selectBatchCountByQueryCriteria", batchQuery);
    }

    @Override
    public void deleteBatches(BatchQueryImpl batchQuery) {
        getDbSqlSession().delete("bulkDeleteBytesForBatches", batchQuery, getManagedEntityClass());
        getDbSqlSession().delete("bulkDeleteBatchPartsForBatches", batchQuery, getManagedEntityClass());
        getDbSqlSession().delete("bulkDeleteBatches", batchQuery, getManagedEntityClass());
    }

    @Override
    protected IdGenerator getIdGenerator() {
        return batchServiceConfiguration.getIdGenerator();
    }
    
}
