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

import java.util.List;

import org.flowable.batch.service.impl.persistence.entity.BatchByteArrayEntity;
import org.flowable.batch.service.impl.persistence.entity.BatchByteArrayEntityImpl;
import org.flowable.batch.service.impl.persistence.entity.data.BatchByteArrayDataManager;
import org.flowable.common.engine.impl.db.AbstractDataManager;

public class MybatisBatchByteArrayDataManager extends AbstractDataManager<BatchByteArrayEntity> implements BatchByteArrayDataManager {

    @Override
    public BatchByteArrayEntity create() {
        return new BatchByteArrayEntityImpl();
    }

    @Override
    public Class<? extends BatchByteArrayEntity> getManagedEntityClass() {
        return BatchByteArrayEntityImpl.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<BatchByteArrayEntity> findAll() {
        return getDbSqlSession().selectList("selectBatchByteArrays");
    }

    @Override
    public void deleteByteArrayNoRevisionCheck(String byteArrayEntityId) {
        getDbSqlSession().delete("deleteBatchByteArrayNoRevisionCheck", byteArrayEntityId, BatchByteArrayEntityImpl.class);
    }

}
