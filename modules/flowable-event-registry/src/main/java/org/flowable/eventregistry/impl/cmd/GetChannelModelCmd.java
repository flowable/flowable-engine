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
package org.flowable.eventregistry.impl.cmd;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.impl.EventDeploymentQueryImpl;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.flowable.eventregistry.impl.persistence.deploy.ChannelDefinitionCacheEntry;
import org.flowable.eventregistry.impl.persistence.deploy.EventDeploymentManager;
import org.flowable.eventregistry.impl.persistence.entity.ChannelDefinitionEntity;
import org.flowable.eventregistry.impl.persistence.entity.ChannelDefinitionEntityManager;
import org.flowable.eventregistry.impl.util.CommandContextUtil;
import org.flowable.eventregistry.model.ChannelModel;

/**
 * @author Tijs Rademakers
 */
public class GetChannelModelCmd implements Command<ChannelModel>, Serializable {

    private static final long serialVersionUID = 1L;

    protected String channelDefinitionKey;
    protected String channelDefinitionId;
    protected String tenantId;
    protected String parentDeploymentId;

    public GetChannelModelCmd(String channelDefinitionKey, String channelDefinitionId) {
        this.channelDefinitionKey = channelDefinitionKey;
        this.channelDefinitionId = channelDefinitionId;
    }

    public GetChannelModelCmd(String channelDefinitionKey, String tenantId, String parentDeploymentId) {
        this(channelDefinitionKey, null);
        this.tenantId = tenantId;
        this.parentDeploymentId = parentDeploymentId;
    }

    @Override
    public ChannelModel execute(CommandContext commandContext) {
        EventRegistryEngineConfiguration eventEngineConfiguration = CommandContextUtil.getEventRegistryConfiguration(commandContext);
        EventDeploymentManager deploymentManager = eventEngineConfiguration.getDeploymentManager();
        ChannelDefinitionEntityManager channelDefinitionEntityManager = eventEngineConfiguration.getChannelDefinitionEntityManager();

        // Find the channel definition
        ChannelDefinitionEntity channelDefinitionEntity = null;
        if (channelDefinitionId != null) {

            channelDefinitionEntity = deploymentManager.findDeployedChannelDefinitionById(channelDefinitionId);
            if (channelDefinitionEntity == null) {
                throw new FlowableObjectNotFoundException("No channel definition found for id = '" + channelDefinitionId + "'", ChannelDefinitionEntity.class);
            }

        } else if (channelDefinitionKey != null && (tenantId == null || EventRegistryEngineConfiguration.NO_TENANT_ID.equals(tenantId)) && 
                        (parentDeploymentId == null || eventEngineConfiguration.isAlwaysLookupLatestDefinitionVersion())) {

            channelDefinitionEntity = deploymentManager.findDeployedLatestChannelDefinitionByKey(channelDefinitionKey);
            if (channelDefinitionEntity == null) {
                throw new FlowableObjectNotFoundException("No channel definition found for key '" + channelDefinitionKey + "'", ChannelDefinitionEntity.class);
            }

        } else if (channelDefinitionKey != null && tenantId != null && !EventRegistryEngineConfiguration.NO_TENANT_ID.equals(tenantId) && 
                        (parentDeploymentId == null || eventEngineConfiguration.isAlwaysLookupLatestDefinitionVersion())) {

            channelDefinitionEntity = channelDefinitionEntityManager.findLatestChannelDefinitionByKeyAndTenantId(channelDefinitionKey, tenantId);
            
            if (channelDefinitionEntity == null && eventEngineConfiguration.isFallbackToDefaultTenant()) {
                String defaultTenant = eventEngineConfiguration.getDefaultTenantProvider().getDefaultTenant(tenantId, ScopeTypes.EVENT_REGISTRY, channelDefinitionKey);
                if (StringUtils.isNotEmpty(defaultTenant)) {
                    channelDefinitionEntity = channelDefinitionEntityManager.findLatestChannelDefinitionByKeyAndTenantId(channelDefinitionKey, defaultTenant);
                } else {
                    channelDefinitionEntity = channelDefinitionEntityManager.findLatestChannelDefinitionByKey(channelDefinitionKey);
                }
            }

            if (channelDefinitionEntity == null) {
                throw new FlowableObjectNotFoundException("No channel definition found for key '" + channelDefinitionKey + "' for tenant identifier " + tenantId, ChannelDefinitionEntity.class);
            }

        } else if (channelDefinitionKey != null && (tenantId == null || EventRegistryEngineConfiguration.NO_TENANT_ID.equals(tenantId)) && parentDeploymentId != null) {

            List<EventDeployment> eventDeployments = deploymentManager.getDeploymentEntityManager().findDeploymentsByQueryCriteria(
                            new EventDeploymentQueryImpl().parentDeploymentId(parentDeploymentId));
            
            if (eventDeployments != null && eventDeployments.size() > 0) {
                channelDefinitionEntity = channelDefinitionEntityManager.findChannelDefinitionByDeploymentAndKey(eventDeployments.get(0).getId(), channelDefinitionKey);
            }
            
            if (channelDefinitionEntity == null) {
                channelDefinitionEntity = channelDefinitionEntityManager.findLatestChannelDefinitionByKey(channelDefinitionKey);
            }
            
            if (channelDefinitionEntity == null) {
                throw new FlowableObjectNotFoundException("No channel definition found for key '" + channelDefinitionKey +
                        "' for parent deployment id " + parentDeploymentId, ChannelDefinitionEntity.class);
            }

        } else if (channelDefinitionKey != null && tenantId != null && !EventRegistryEngineConfiguration.NO_TENANT_ID.equals(tenantId) && parentDeploymentId != null) {

            List<EventDeployment> eventDeployments = deploymentManager.getDeploymentEntityManager().findDeploymentsByQueryCriteria(
                            new EventDeploymentQueryImpl().parentDeploymentId(parentDeploymentId).deploymentTenantId(tenantId));
            
            if (eventDeployments != null && eventDeployments.size() > 0) {
                channelDefinitionEntity = channelDefinitionEntityManager.findChannelDefinitionByDeploymentAndKeyAndTenantId(
                                eventDeployments.get(0).getId(), channelDefinitionKey, tenantId);
            }
            
            if (channelDefinitionEntity == null) {
                channelDefinitionEntity = channelDefinitionEntityManager.findLatestChannelDefinitionByKeyAndTenantId(channelDefinitionKey, tenantId);
            }
            
            if (channelDefinitionEntity == null && eventEngineConfiguration.isFallbackToDefaultTenant()) {
                String defaultTenant = eventEngineConfiguration.getDefaultTenantProvider().getDefaultTenant(tenantId, ScopeTypes.EVENT_REGISTRY, channelDefinitionKey);
                if (StringUtils.isNotEmpty(defaultTenant)) {
                    channelDefinitionEntity = channelDefinitionEntityManager.findLatestChannelDefinitionByKeyAndTenantId(channelDefinitionKey, defaultTenant);
                } else {
                    channelDefinitionEntity = channelDefinitionEntityManager.findLatestChannelDefinitionByKey(channelDefinitionKey);
                }
            }
            
            if (channelDefinitionEntity == null) {
                throw new FlowableObjectNotFoundException("No channel definition found for key '" + channelDefinitionKey +
                        " for parent deployment id '" + parentDeploymentId + "' and for tenant identifier " + tenantId, ChannelDefinitionEntity.class);
            }

        } else {
            throw new FlowableObjectNotFoundException("channelDefinitionKey and channelDefinitionId are null");
        }

        ChannelDefinitionCacheEntry channelDefinitionCacheEntry = deploymentManager.resolveChannelDefinition(channelDefinitionEntity);
        return channelDefinitionCacheEntry.getChannelModel();
    }
}
