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
import org.flowable.idm.engine.impl.persistence.entity.PropertyEntity;
import org.flowable.idm.engine.impl.persistence.entity.PropertyEntityImpl;
import org.flowable.idm.engine.impl.persistence.entity.data.AbstractDataManager;
import org.flowable.idm.engine.impl.persistence.entity.data.PropertyDataManager;

/**
 * @author Joram Barrez
 */
public class MybatisPropertyDataManager extends AbstractDataManager<PropertyEntity> implements PropertyDataManager {

    public MybatisPropertyDataManager(IdmEngineConfiguration idmEngineConfiguration) {
        super(idmEngineConfiguration);
    }

    @Override
    public Class<? extends PropertyEntity> getManagedEntityClass() {
        return PropertyEntityImpl.class;
    }

    @Override
    public PropertyEntity create() {
        return new PropertyEntityImpl();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<PropertyEntity> findAll() {
        return getDbSqlSession().selectList("selectProperties");
    }

}
