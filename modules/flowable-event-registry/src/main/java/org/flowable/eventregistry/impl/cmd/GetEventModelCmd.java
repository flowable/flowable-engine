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
import org.flowable.eventregistry.impl.persistence.deploy.EventDefinitionCacheEntry;
import org.flowable.eventregistry.impl.persistence.deploy.EventDeploymentManager;
import org.flowable.eventregistry.impl.persistence.entity.EventDefinitionEntity;
import org.flowable.eventregistry.impl.persistence.entity.EventDefinitionEntityManager;
import org.flowable.eventregistry.impl.util.CommandContextUtil;
import org.flowable.eventregistry.model.EventModel;

/**
 * @author Tijs Rademakers
 */
public class GetEventModelCmd implements Command<EventModel>, Serializable {

    private static final long serialVersionUID = 1L;

    protected String eventDefinitionKey;
    protected String eventDefinitionId;
    protected String tenantId;
    protected String parentDeploymentId;

    public GetEventModelCmd(String eventDefinitionKey, String eventDefinitionId) {
        this.eventDefinitionKey = eventDefinitionKey;
        this.eventDefinitionId = eventDefinitionId;
    }

    public GetEventModelCmd(String eventDefinitionKey, String tenantId, String parentDeploymentId) {
        this(eventDefinitionKey, null);
        this.parentDeploymentId = parentDeploymentId;
        this.tenantId = tenantId;
    }

    @Override
    public EventModel execute(CommandContext commandContext) {
        EventRegistryEngineConfiguration eventEngineConfiguration = CommandContextUtil.getEventRegistryConfiguration(commandContext);
        EventDeploymentManager deploymentManager = eventEngineConfiguration.getDeploymentManager();
        EventDefinitionEntityManager eventDefinitionEntityManager = eventEngineConfiguration.getEventDefinitionEntityManager();

        // Find the event definition
        EventDefinitionEntity eventDefinitionEntity = null;
        if (eventDefinitionId != null) {

            eventDefinitionEntity = deploymentManager.findDeployedEventDefinitionById(eventDefinitionId);
            if (eventDefinitionEntity == null) {
                throw new FlowableObjectNotFoundException("No event definition found for id = '" + eventDefinitionId + "'", EventDefinitionEntity.class);
            }

        } else if (eventDefinitionKey != null && (tenantId == null || EventRegistryEngineConfiguration.NO_TENANT_ID.equals(tenantId)) && 
                        (parentDeploymentId == null || eventEngineConfiguration.isAlwaysLookupLatestDefinitionVersion())) {

            eventDefinitionEntity = deploymentManager.findDeployedLatestEventDefinitionByKey(eventDefinitionKey);
            if (eventDefinitionEntity == null) {
                throw new FlowableObjectNotFoundException("No event definition found for key '" + eventDefinitionKey + "'", EventDefinitionEntity.class);
            }

        } else if (eventDefinitionKey != null && tenantId != null && !EventRegistryEngineConfiguration.NO_TENANT_ID.equals(tenantId) && 
                        (parentDeploymentId == null || eventEngineConfiguration.isAlwaysLookupLatestDefinitionVersion())) {

            eventDefinitionEntity = eventDefinitionEntityManager.findLatestEventDefinitionByKeyAndTenantId(eventDefinitionKey, tenantId);
            
            if (eventDefinitionEntity == null && eventEngineConfiguration.isFallbackToDefaultTenant()) {
                String defaultTenant = eventEngineConfiguration.getDefaultTenantProvider().getDefaultTenant(tenantId, ScopeTypes.EVENT_REGISTRY, eventDefinitionKey);
                if (StringUtils.isNotEmpty(defaultTenant)) {
                    eventDefinitionEntity = eventDefinitionEntityManager.findLatestEventDefinitionByKeyAndTenantId(eventDefinitionKey, defaultTenant);
                } else {
                    eventDefinitionEntity = eventDefinitionEntityManager.findLatestEventDefinitionByKey(eventDefinitionKey);
                }
            }
            
            if (eventDefinitionEntity == null) {
                throw new FlowableObjectNotFoundException("No event definition found for key '" + eventDefinitionKey + "' for tenant identifier '" + tenantId + "'", EventDefinitionEntity.class);
            }

        } else if (eventDefinitionKey != null && (tenantId == null || EventRegistryEngineConfiguration.NO_TENANT_ID.equals(tenantId)) && parentDeploymentId != null) {

            List<EventDeployment> eventDeployments = deploymentManager.getDeploymentEntityManager().findDeploymentsByQueryCriteria(
                            new EventDeploymentQueryImpl().parentDeploymentId(parentDeploymentId));
            
            if (eventDeployments != null && eventDeployments.size() > 0) {
                eventDefinitionEntity = eventDefinitionEntityManager.findEventDefinitionByDeploymentAndKey(eventDeployments.get(0).getId(), eventDefinitionKey);
            }
            
            if (eventDefinitionEntity == null) {
                eventDefinitionEntity = eventDefinitionEntityManager.findLatestEventDefinitionByKey(eventDefinitionKey);
            }
            
            if (eventDefinitionEntity == null) {
                throw new FlowableObjectNotFoundException("No event definition found for key '" + eventDefinitionKey +
                        "' for parent deployment id " + parentDeploymentId, EventDefinitionEntity.class);
            }

        } else if (eventDefinitionKey != null && tenantId != null && !EventRegistryEngineConfiguration.NO_TENANT_ID.equals(tenantId) && parentDeploymentId != null) {

            List<EventDeployment> eventDeployments = deploymentManager.getDeploymentEntityManager().findDeploymentsByQueryCriteria(
                            new EventDeploymentQueryImpl().parentDeploymentId(parentDeploymentId).deploymentTenantId(tenantId));
            
            if (eventDeployments != null && eventDeployments.size() > 0) {
                eventDefinitionEntity = eventDefinitionEntityManager.findEventDefinitionByDeploymentAndKeyAndTenantId(
                                eventDeployments.get(0).getId(), eventDefinitionKey, tenantId);
            }
            
            if (eventDefinitionEntity == null) {
                eventDefinitionEntity = eventDefinitionEntityManager.findLatestEventDefinitionByKeyAndTenantId(eventDefinitionKey, tenantId);
            }
            
            if (eventDefinitionEntity == null && eventEngineConfiguration.isFallbackToDefaultTenant()) {
                String defaultTenant = eventEngineConfiguration.getDefaultTenantProvider().getDefaultTenant(tenantId, ScopeTypes.EVENT_REGISTRY, eventDefinitionKey);
                if (StringUtils.isNotEmpty(defaultTenant)) {
                    eventDefinitionEntity = eventDefinitionEntityManager.findLatestEventDefinitionByKeyAndTenantId(eventDefinitionKey, defaultTenant);
                } else {
                    eventDefinitionEntity = eventDefinitionEntityManager.findLatestEventDefinitionByKey(eventDefinitionKey);
                }
            }
            
            if (eventDefinitionEntity == null) {
                throw new FlowableObjectNotFoundException("No event definition found for key '" + eventDefinitionKey +
                        " for parent deployment id '" + parentDeploymentId + "' and for tenant identifier " + tenantId, EventDefinitionEntity.class);
            }

        } else {
            throw new FlowableObjectNotFoundException("eventDefinitionKey and eventDefinitionId are null");
        }

        EventDefinitionCacheEntry eventDefinitionCacheEntry = deploymentManager.resolveEventDefinition(eventDefinitionEntity);
        return eventEngineConfiguration.getEventJsonConverter().convertToEventModel(eventDefinitionCacheEntry.getEventDefinitionJson());
    }
}
