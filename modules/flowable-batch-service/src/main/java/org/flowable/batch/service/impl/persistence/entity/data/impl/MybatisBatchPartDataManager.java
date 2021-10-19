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

import org.flowable.batch.api.BatchPart;
import org.flowable.batch.service.BatchServiceConfiguration;
import org.flowable.batch.service.impl.BatchPartQueryImpl;
import org.flowable.batch.service.impl.persistence.entity.BatchPartEntity;
import org.flowable.batch.service.impl.persistence.entity.BatchPartEntityImpl;
import org.flowable.batch.service.impl.persistence.entity.data.BatchPartDataManager;
import org.flowable.common.engine.impl.cfg.IdGenerator;
import org.flowable.common.engine.impl.db.AbstractDataManager;

public class MybatisBatchPartDataManager extends AbstractDataManager<BatchPartEntity> implements BatchPartDataManager {

    protected BatchServiceConfiguration batchServiceConfiguration;
    
    public MybatisBatchPartDataManager(BatchServiceConfiguration batchServiceConfiguration) {
        this.batchServiceConfiguration = batchServiceConfiguration;
    }
    
    @Override
    public Class<? extends BatchPartEntity> getManagedEntityClass() {
        return BatchPartEntityImpl.class;
    }

    @Override
    public BatchPartEntity create() {
        return new BatchPartEntityImpl();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<BatchPart> findBatchPartsByBatchId(String batchId) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("batchId", batchId);
        
        return getDbSqlSession().selectList("selectBatchPartsByBatchId", params);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public List<BatchPart> findBatchPartsByBatchIdAndStatus(String batchId, String status) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("batchId", batchId);
        params.put("status", status);
        
        return getDbSqlSession().selectList("selectBatchPartsByBatchIdAndStatus", params);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public List<BatchPart> findBatchPartsByScopeIdAndType(String scopeId, String scopeType) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("scopeId", scopeId);
        params.put("scopeType", scopeType);
        
        return getDbSqlSession().selectList("selectBatchPartsByScopeIdAndScopeType", params);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public List<BatchPart> findBatchPartsByQueryCriteria(BatchPartQueryImpl batchPartQuery) {
        return getDbSqlSession().selectList("selectBatchPartsByQueryCriteria", batchPartQuery, getManagedEntityClass());
    }

    @Override
    public long findBatchPartCountByQueryCriteria(BatchPartQueryImpl batchPartQuery) {
        return (Long) getDbSqlSession().selectOne("selectBatchPartCountByQueryCriteria", batchPartQuery);
    }

    @Override
    protected IdGenerator getIdGenerator() {
        return batchServiceConfiguration.getIdGenerator();
    }
}
