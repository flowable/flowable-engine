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
import org.flowable.eventregistry.api.EventDefinition;
import org.flowable.eventregistry.impl.EventDefinitionQueryImpl;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.flowable.eventregistry.impl.persistence.entity.data.EventDefinitionDataManager;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class EventDefinitionEntityManagerImpl
        extends AbstractEngineEntityManager<EventRegistryEngineConfiguration, EventDefinitionEntity, EventDefinitionDataManager>
        implements EventDefinitionEntityManager {

    public EventDefinitionEntityManagerImpl(EventRegistryEngineConfiguration eventRegistryEngineConfiguration, EventDefinitionDataManager eventDefinitionDataManager) {
        super(eventRegistryEngineConfiguration, eventDefinitionDataManager);
    }

    @Override
    public EventDefinitionEntity findLatestEventDefinitionByKey(String eventDefinitionKey) {
        return dataManager.findLatestEventDefinitionByKey(eventDefinitionKey);
    }

    @Override
    public void deleteEventDefinitionsByDeploymentId(String deploymentId) {
        dataManager.deleteEventDefinitionsByDeploymentId(deploymentId);
    }

    @Override
    public List<EventDefinition> findEventDefinitionsByQueryCriteria(EventDefinitionQueryImpl eventQuery) {
        return dataManager.findEventDefinitionsByQueryCriteria(eventQuery);
    }

    @Override
    public long findEventDefinitionCountByQueryCriteria(EventDefinitionQueryImpl eventQuery) {
        return dataManager.findEventDefinitionCountByQueryCriteria(eventQuery);
    }

    @Override
    public EventDefinitionEntity findEventDefinitionByDeploymentAndKey(String deploymentId, String eventDefinitionKey) {
        return dataManager.findEventDefinitionByDeploymentAndKey(deploymentId, eventDefinitionKey);
    }

    @Override
    public EventDefinitionEntity findEventDefinitionByDeploymentAndKeyAndTenantId(String deploymentId, String eventDefinitionKey, String tenantId) {
        return dataManager.findEventDefinitionByDeploymentAndKeyAndTenantId(deploymentId, eventDefinitionKey, tenantId);
    }

    @Override
    public EventDefinitionEntity findLatestEventDefinitionByKeyAndTenantId(String eventDefinitionKey, String tenantId) {
        if (tenantId == null || EventRegistryEngineConfiguration.NO_TENANT_ID.equals(tenantId)) {
            return dataManager.findLatestEventDefinitionByKey(eventDefinitionKey);
        } else {
            return dataManager.findLatestEventDefinitionByKeyAndTenantId(eventDefinitionKey, tenantId);
        }
    }
    
    @Override
    public EventDefinitionEntity findEventDefinitionByKeyAndVersionAndTenantId(String eventDefinitionKey, Integer eventVersion, String tenantId) {
        if (tenantId == null || EventRegistryEngineConfiguration.NO_TENANT_ID.equals(tenantId)) {
            return dataManager.findEventDefinitionByKeyAndVersion(eventDefinitionKey, eventVersion);
        } else {
            return dataManager.findEventDefinitionByKeyAndVersionAndTenantId(eventDefinitionKey, eventVersion, tenantId);
        }
    }

    @Override
    public List<EventDefinition> findEventDefinitionsByNativeQuery(Map<String, Object> parameterMap) {
        return dataManager.findEventDefinitionsByNativeQuery(parameterMap);
    }

    @Override
    public long findEventDefinitionCountByNativeQuery(Map<String, Object> parameterMap) {
        return dataManager.findEventDefinitionCountByNativeQuery(parameterMap);
    }

    @Override
    public void updateEventDefinitionTenantIdForDeployment(String deploymentId, String newTenantId) {
        dataManager.updateEventDefinitionTenantIdForDeployment(deploymentId, newTenantId);
    }

}
