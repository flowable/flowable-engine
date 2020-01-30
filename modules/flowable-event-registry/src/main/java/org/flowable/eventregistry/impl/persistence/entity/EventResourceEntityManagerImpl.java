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

package org.flowable.eventregistry.impl.persistence.entity;

import java.util.List;

import org.flowable.common.engine.impl.persistence.entity.AbstractEngineEntityManager;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.flowable.eventregistry.impl.persistence.entity.data.EventResourceDataManager;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class EventResourceEntityManagerImpl
        extends AbstractEngineEntityManager<EventRegistryEngineConfiguration, EventResourceEntity, EventResourceDataManager>
        implements EventResourceEntityManager {

    public EventResourceEntityManagerImpl(EventRegistryEngineConfiguration eventRegistryConfiguration, EventResourceDataManager resourceDataManager) {
        super(eventRegistryConfiguration, resourceDataManager);
    }

    @Override
    public void deleteResourcesByDeploymentId(String deploymentId) {
        dataManager.deleteResourcesByDeploymentId(deploymentId);
    }

    @Override
    public EventResourceEntity findResourceByDeploymentIdAndResourceName(String deploymentId, String resourceName) {
        return dataManager.findResourceByDeploymentIdAndResourceName(deploymentId, resourceName);
    }

    @Override
    public List<EventResourceEntity> findResourcesByDeploymentId(String deploymentId) {
        return dataManager.findResourcesByDeploymentId(deploymentId);
    }

}
