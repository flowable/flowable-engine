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
import org.flowable.idm.engine.impl.persistence.entity.IdmPropertyEntity;
import org.flowable.idm.engine.impl.persistence.entity.IdmPropertyEntityImpl;
import org.flowable.idm.engine.impl.persistence.entity.data.AbstractIdmDataManager;
import org.flowable.idm.engine.impl.persistence.entity.data.PropertyDataManager;

/**
 * @author Joram Barrez
 */
public class MybatisPropertyDataManager extends AbstractIdmDataManager<IdmPropertyEntity> implements PropertyDataManager {

    public MybatisPropertyDataManager(IdmEngineConfiguration idmEngineConfiguration) {
        super(idmEngineConfiguration);
    }

    @Override
    public Class<? extends IdmPropertyEntity> getManagedEntityClass() {
        return IdmPropertyEntityImpl.class;
    }

    @Override
    public IdmPropertyEntity create() {
        return new IdmPropertyEntityImpl();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<IdmPropertyEntity> findAll() {
        return getDbSqlSession().selectList("selectProperties");
    }

}
