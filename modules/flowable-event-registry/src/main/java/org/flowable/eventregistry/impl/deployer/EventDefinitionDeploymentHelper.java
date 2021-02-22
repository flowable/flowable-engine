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
package org.flowable.eventregistry.impl.deployer;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.flowable.eventregistry.impl.persistence.entity.EventDefinitionEntity;
import org.flowable.eventregistry.impl.persistence.entity.EventDefinitionEntityManager;
import org.flowable.eventregistry.impl.persistence.entity.EventDeploymentEntity;
import org.flowable.eventregistry.impl.util.CommandContextUtil;

/**
 * Methods for working with deployments. Much of the actual work of {@link EventDefinitionDeployer} is done by orchestrating the different pieces of work this class does; by having them here, we allow
 * other deployers to make use of them.
 */
public class EventDefinitionDeploymentHelper {

    /**
     * Verifies that no two event definitions share the same key, to prevent database unique index violation.
     * 
     * @throws FlowableException
     *             if any two event definitions have the same key
     */
    public void verifyEventDefinitionsDoNotShareKeys(Collection<EventDefinitionEntity> eventDefinitions) {
        Set<String> keySet = new LinkedHashSet<>();
        for (EventDefinitionEntity eventDefinition : eventDefinitions) {
            if (keySet.contains(eventDefinition.getKey())) {
                throw new FlowableException("The deployment contains event definition with the same key, this is not allowed");
            }
            keySet.add(eventDefinition.getKey());
        }
    }

    /**
     * Updates all the event definition entities to match the deployment's values for tenant, engine version, and deployment id.
     */
    public void copyDeploymentValuesToEventDefinitions(EventDeploymentEntity deployment, List<EventDefinitionEntity> eventDefinitions) {
        String tenantId = deployment.getTenantId();
        String deploymentId = deployment.getId();

        for (EventDefinitionEntity eventDefinition : eventDefinitions) {

            // event definition inherits the tenant id
            if (tenantId != null) {
                eventDefinition.setTenantId(tenantId);
            }

            eventDefinition.setDeploymentId(deploymentId);
        }
    }

    /**
     * Updates all the decision table entities to have the correct resource names.
     */
    public void setResourceNamesOnEventDefinitions(ParsedDeployment parsedDeployment) {
        for (EventDefinitionEntity eventDefinition : parsedDeployment.getAllEventDefinitions()) {
            String resourceName = parsedDeployment.getResourceForEventDefinition(eventDefinition).getName();
            eventDefinition.setResourceName(resourceName);
        }
    }

    /**
     * Gets the persisted event definition that matches this one for tenant and key. 
     * If none is found, returns null. This method assumes that the tenant and key are properly set on the
     * event definition entity.
     */
    public EventDefinitionEntity getMostRecentVersionOfEventDefinition(EventDefinitionEntity eventDefinition) {
        String key = eventDefinition.getKey();
        String tenantId = eventDefinition.getTenantId();
        EventDefinitionEntityManager eventDefinitionEntityManager = CommandContextUtil.getEventRegistryConfiguration().getEventDefinitionEntityManager();

        EventDefinitionEntity existingDefinition = null;

        if (tenantId != null && !tenantId.equals(EventRegistryEngineConfiguration.NO_TENANT_ID)) {
            existingDefinition = eventDefinitionEntityManager.findLatestEventDefinitionByKeyAndTenantId(key, tenantId);
        } else {
            existingDefinition = eventDefinitionEntityManager.findLatestEventDefinitionByKey(key);
        }

        return existingDefinition;
    }

    /**
     * Gets the persisted version of the already-deployed event definition.
     */
    public EventDefinitionEntity getPersistedInstanceOfEventDefinition(EventDefinitionEntity eventDefinition) {
        String deploymentId = eventDefinition.getDeploymentId();
        if (StringUtils.isEmpty(eventDefinition.getDeploymentId())) {
            throw new FlowableIllegalArgumentException("Provided event definition must have a deployment id.");
        }

        EventDefinitionEntityManager eventDefinitionEntityManager = CommandContextUtil.getEventRegistryConfiguration().getEventDefinitionEntityManager();

        EventDefinitionEntity persistedEventDefinition = null;
        if (eventDefinition.getTenantId() == null || EventRegistryEngineConfiguration.NO_TENANT_ID.equals(eventDefinition.getTenantId())) {
            persistedEventDefinition = eventDefinitionEntityManager.findEventDefinitionByDeploymentAndKey(deploymentId, eventDefinition.getKey());
        } else {
            persistedEventDefinition = eventDefinitionEntityManager.findEventDefinitionByDeploymentAndKeyAndTenantId(deploymentId,
                            eventDefinition.getKey(), eventDefinition.getTenantId());
        }

        return persistedEventDefinition;
    }
}
