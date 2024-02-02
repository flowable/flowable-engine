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
import org.flowable.eventregistry.impl.persistence.entity.ChannelDefinitionEntity;
import org.flowable.eventregistry.impl.persistence.entity.ChannelDefinitionEntityManager;
import org.flowable.eventregistry.impl.persistence.entity.EventDeploymentEntity;
import org.flowable.eventregistry.impl.util.CommandContextUtil;

/**
 * Methods for working with deployments. Much of the actual work of {@link EventDefinitionDeployer} is done by orchestrating the different pieces of work this class does; by having them here, we allow
 * other deployers to make use of them.
 */
public class ChannelDefinitionDeploymentHelper {

    /**
     * Verifies that no two channel definitions share the same key, to prevent database unique index violation.
     * 
     * @throws FlowableException
     *             if any two channel definitions have the same key
     */
    public void verifyChannelDefinitionsDoNotShareKeys(Collection<ChannelDefinitionEntity> channelDefinitions) {
        Set<String> keySet = new LinkedHashSet<>();
        for (ChannelDefinitionEntity channelDefinition : channelDefinitions) {
            if (keySet.contains(channelDefinition.getKey())) {
                throw new FlowableException("The deployment contains channel definition with the same key, this is not allowed");
            }
            keySet.add(channelDefinition.getKey());
        }
    }

    /**
     * Updates all the channel definition entities to match the deployment's values for tenant, engine version, and deployment id.
     */
    public void copyDeploymentValuesToEventDefinitions(EventDeploymentEntity deployment, List<ChannelDefinitionEntity> channelDefinitions) {
        String tenantId = deployment.getTenantId();
        String deploymentId = deployment.getId();

        for (ChannelDefinitionEntity channelDefinition : channelDefinitions) {

            // event definition inherits the tenant id
            if (tenantId != null) {
                channelDefinition.setTenantId(tenantId);
            }

            channelDefinition.setDeploymentId(deploymentId);
        }
    }

    /**
     * Updates all the channel definition entities to have the correct resource names.
     */
    public void setResourceNamesOnEventDefinitions(ParsedDeployment parsedDeployment) {
        for (ChannelDefinitionEntity channelDefinition : parsedDeployment.getAllChannelDefinitions()) {
            String resourceName = parsedDeployment.getResourceForChannelDefinition(channelDefinition).getName();
            channelDefinition.setResourceName(resourceName);
        }
    }

    /**
     * Gets the persisted channel definition that matches this one for tenant and key. 
     * If none is found, returns null. This method assumes that the tenant and key are properly set on the
     * channel definition entity.
     */
    public ChannelDefinitionEntity getMostRecentVersionOfChannelDefinition(ChannelDefinitionEntity channelDefinition) {
        String key = channelDefinition.getKey();
        String tenantId = channelDefinition.getTenantId();
        ChannelDefinitionEntityManager channelDefinitionEntityManager = CommandContextUtil.getEventRegistryConfiguration().getChannelDefinitionEntityManager();

        ChannelDefinitionEntity existingDefinition = null;

        if (tenantId != null && !tenantId.equals(EventRegistryEngineConfiguration.NO_TENANT_ID)) {
            existingDefinition = channelDefinitionEntityManager.findLatestChannelDefinitionByKeyAndTenantId(key, tenantId);
        } else {
            existingDefinition = channelDefinitionEntityManager.findLatestChannelDefinitionByKey(key);
        }

        return existingDefinition;
    }

    /**
     * Gets the persisted version of the already-deployed channel definition.
     */
    public ChannelDefinitionEntity getPersistedInstanceOfChannelDefinition(ChannelDefinitionEntity channelDefinition) {
        String deploymentId = channelDefinition.getDeploymentId();
        if (StringUtils.isEmpty(channelDefinition.getDeploymentId())) {
            throw new FlowableIllegalArgumentException("Provided channel definition must have a deployment id.");
        }

        ChannelDefinitionEntityManager channelDefinitionEntityManager = CommandContextUtil.getEventRegistryConfiguration().getChannelDefinitionEntityManager();

        ChannelDefinitionEntity persistedChannelDefinition = null;
        if (channelDefinition.getTenantId() == null || EventRegistryEngineConfiguration.NO_TENANT_ID.equals(channelDefinition.getTenantId())) {
            persistedChannelDefinition = channelDefinitionEntityManager.findChannelDefinitionByDeploymentAndKey(deploymentId, channelDefinition.getKey());
        } else {
            persistedChannelDefinition = channelDefinitionEntityManager.findChannelDefinitionByDeploymentAndKeyAndTenantId(deploymentId,
                            channelDefinition.getKey(), channelDefinition.getTenantId());
        }

        return persistedChannelDefinition;
    }
}
