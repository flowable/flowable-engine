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
package org.flowable.eventregistry.impl.persistence.entity.data.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.flowable.eventregistry.impl.persistence.entity.EventResourceEntity;
import org.flowable.eventregistry.impl.persistence.entity.EventResourceEntityImpl;
import org.flowable.eventregistry.impl.persistence.entity.data.AbstractEventDataManager;
import org.flowable.eventregistry.impl.persistence.entity.data.EventResourceDataManager;

/**
 * @author Joram Barrez
 */
public class MybatisEventResourceDataManager extends AbstractEventDataManager<EventResourceEntity> implements EventResourceDataManager {

    public MybatisEventResourceDataManager(EventRegistryEngineConfiguration eventRegistryConfiguration) {
        super(eventRegistryConfiguration);
    }

    @Override
    public Class<? extends EventResourceEntity> getManagedEntityClass() {
        return EventResourceEntityImpl.class;
    }

    @Override
    public EventResourceEntity create() {
        return new EventResourceEntityImpl();
    }

    @Override
    public void deleteResourcesByDeploymentId(String deploymentId) {
        getDbSqlSession().delete("deleteEventResourcesByDeploymentId", deploymentId, getManagedEntityClass());
    }

    @Override
    public EventResourceEntity findResourceByDeploymentIdAndResourceName(String deploymentId, String resourceName) {
        Map<String, Object> params = new HashMap<>();
        params.put("deploymentId", deploymentId);
        params.put("resourceName", resourceName);
        return (EventResourceEntity) getDbSqlSession().selectOne("selectEventResourceByDeploymentIdAndResourceName", params);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<EventResourceEntity> findResourcesByDeploymentId(String deploymentId) {
        return getDbSqlSession().selectList("selectEventResourcesByDeploymentId", deploymentId);
    }

}
