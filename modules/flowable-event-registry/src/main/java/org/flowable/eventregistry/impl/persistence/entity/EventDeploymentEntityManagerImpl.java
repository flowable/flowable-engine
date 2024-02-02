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
import java.util.Map;

import org.flowable.common.engine.impl.persistence.entity.AbstractEngineEntityManager;
import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.impl.EventDeploymentQueryImpl;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.flowable.eventregistry.impl.persistence.entity.data.EventDeploymentDataManager;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class EventDeploymentEntityManagerImpl
        extends AbstractEngineEntityManager<EventRegistryEngineConfiguration, EventDeploymentEntity, EventDeploymentDataManager>
        implements EventDeploymentEntityManager {

    public EventDeploymentEntityManagerImpl(EventRegistryEngineConfiguration eventRegistryConfiguration, EventDeploymentDataManager deploymentDataManager) {
        super(eventRegistryConfiguration, deploymentDataManager);
    }

    @Override
    public void insert(EventDeploymentEntity deployment) {
        insert(deployment, true);
    }

    @Override
    public void insert(EventDeploymentEntity deployment, boolean fireEvent) {
        super.insert(deployment, fireEvent);

        for (EventResourceEntity resource : deployment.getResources().values()) {
            resource.setDeploymentId(deployment.getId());
            getResourceEntityManager().insert(resource);
        }
    }

    @Override
    public void deleteDeployment(String deploymentId) {
        deleteEventDefinitionsForDeployment(deploymentId);
        deleteChannelDefinitionsForDeployment(deploymentId);
        getResourceEntityManager().deleteResourcesByDeploymentId(deploymentId);
        delete(findById(deploymentId));
    }

    protected void deleteEventDefinitionsForDeployment(String deploymentId) {
        getEventDefinitionEntityManager().deleteEventDefinitionsByDeploymentId(deploymentId);
    }
    
    protected void deleteChannelDefinitionsForDeployment(String deploymentId) {
        getChannelDefinitionEntityManager().deleteChannelDefinitionsByDeploymentId(deploymentId);
    }

    @Override
    public long findDeploymentCountByQueryCriteria(EventDeploymentQueryImpl deploymentQuery) {
        return dataManager.findDeploymentCountByQueryCriteria(deploymentQuery);
    }

    @Override
    public List<EventDeployment> findDeploymentsByQueryCriteria(EventDeploymentQueryImpl deploymentQuery) {
        return dataManager.findDeploymentsByQueryCriteria(deploymentQuery);
    }

    @Override
    public List<String> getDeploymentResourceNames(String deploymentId) {
        return dataManager.getDeploymentResourceNames(deploymentId);
    }

    @Override
    public List<EventDeployment> findDeploymentsByNativeQuery(Map<String, Object> parameterMap) {
        return dataManager.findDeploymentsByNativeQuery(parameterMap);
    }

    @Override
    public long findDeploymentCountByNativeQuery(Map<String, Object> parameterMap) {
        return dataManager.findDeploymentCountByNativeQuery(parameterMap);
    }

    protected EventResourceEntityManager getResourceEntityManager() {
        return engineConfiguration.getResourceEntityManager();
    }

    protected EventDefinitionEntityManager getEventDefinitionEntityManager() {
        return engineConfiguration.getEventDefinitionEntityManager();
    }

    protected ChannelDefinitionEntityManager getChannelDefinitionEntityManager() {
        return engineConfiguration.getChannelDefinitionEntityManager();
    }
}
