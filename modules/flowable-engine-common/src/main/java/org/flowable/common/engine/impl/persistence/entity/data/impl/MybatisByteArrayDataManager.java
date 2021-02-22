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
package org.flowable.common.engine.impl.persistence.entity.data.impl;

import java.util.List;

import org.flowable.common.engine.impl.cfg.IdGenerator;
import org.flowable.common.engine.impl.db.AbstractDataManager;
import org.flowable.common.engine.impl.persistence.entity.ByteArrayEntity;
import org.flowable.common.engine.impl.persistence.entity.ByteArrayEntityImpl;
import org.flowable.common.engine.impl.persistence.entity.data.ByteArrayDataManager;

/**
 * @author Joram Barrez
 */
public class MybatisByteArrayDataManager extends AbstractDataManager<ByteArrayEntity> implements ByteArrayDataManager {

    protected IdGenerator idGenerator;
    
    public MybatisByteArrayDataManager(IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }
    
    @Override
    public ByteArrayEntity create() {
        return new ByteArrayEntityImpl();
    }

    @Override
    public Class<? extends ByteArrayEntity> getManagedEntityClass() {
        return ByteArrayEntityImpl.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ByteArrayEntity> findAll() {
        return getDbSqlSession().selectList("selectByteArrays");
    }

    @Override
    public void deleteByteArrayNoRevisionCheck(String byteArrayEntityId) {
        getDbSqlSession().delete("deleteByteArrayNoRevisionCheck", byteArrayEntityId, ByteArrayEntityImpl.class);
    }

    @Override
    protected IdGenerator getIdGenerator() {
        return idGenerator;
    }
}
