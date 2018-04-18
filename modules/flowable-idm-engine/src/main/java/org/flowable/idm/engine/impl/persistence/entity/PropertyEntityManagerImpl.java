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

package org.flowable.idm.engine.impl.persistence.entity;

import java.util.List;

import org.flowable.common.engine.impl.persistence.entity.data.DataManager;
import org.flowable.idm.engine.IdmEngineConfiguration;
import org.flowable.idm.engine.impl.persistence.entity.data.PropertyDataManager;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class PropertyEntityManagerImpl extends AbstractEntityManager<IdmPropertyEntity> implements PropertyEntityManager {

    protected PropertyDataManager propertyDataManager;

    public PropertyEntityManagerImpl(IdmEngineConfiguration idmEngineConfiguration, PropertyDataManager propertyDataManager) {
        super(idmEngineConfiguration);
        this.propertyDataManager = propertyDataManager;
    }

    @Override
    protected DataManager<IdmPropertyEntity> getDataManager() {
        return propertyDataManager;
    }

    @Override
    public List<IdmPropertyEntity> findAll() {
        return propertyDataManager.findAll();
    }

}
