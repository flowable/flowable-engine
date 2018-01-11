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
package org.flowable.idm.engine.impl.persistence.entity.data.impl;

import java.util.List;

import org.flowable.idm.engine.IdmEngineConfiguration;
import org.flowable.idm.engine.impl.persistence.entity.IdmByteArrayEntity;
import org.flowable.idm.engine.impl.persistence.entity.IdmByteArrayEntityImpl;
import org.flowable.idm.engine.impl.persistence.entity.data.AbstractIdmDataManager;
import org.flowable.idm.engine.impl.persistence.entity.data.ByteArrayDataManager;

/**
 * @author Joram Barrez
 */
public class MybatisByteArrayDataManager extends AbstractIdmDataManager<IdmByteArrayEntity> implements ByteArrayDataManager {

    public MybatisByteArrayDataManager(IdmEngineConfiguration idmEngineConfiguration) {
        super(idmEngineConfiguration);
    }

    @Override
    public IdmByteArrayEntity create() {
        return new IdmByteArrayEntityImpl();
    }

    @Override
    public Class<? extends IdmByteArrayEntity> getManagedEntityClass() {
        return IdmByteArrayEntityImpl.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<IdmByteArrayEntity> findAll() {
        return getDbSqlSession().selectList("selectIdmByteArrays");
    }

    @Override
    public void deleteByteArrayNoRevisionCheck(String byteArrayEntityId) {
        getDbSqlSession().delete("deleteIdmByteArrayNoRevisionCheck", byteArrayEntityId, getManagedEntityClass());
    }

}
