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
package org.flowable.eventregistry.impl.model;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.api.model.EventModelBuilder;
import org.flowable.eventregistry.impl.EventRepositoryServiceImpl;
import org.flowable.eventregistry.json.converter.EventJsonConverter;
import org.flowable.eventregistry.model.EventModel;
import org.flowable.eventregistry.model.EventPayload;

/**
 * @author Joram Barrez
 */
public class EventModelBuilderImpl implements EventModelBuilder {

    protected EventRepositoryServiceImpl eventRepository;
    protected EventJsonConverter eventJsonConverter;
    
    protected String deploymentName;
    protected String resourceName;
    protected String category;
    protected String parentDeploymentId;
    protected String deploymentTenantId;

    protected String key;
    protected Map<String, EventPayload> eventPayloadDefinitions = new LinkedHashMap<>();

    public EventModelBuilderImpl(EventRepositoryServiceImpl eventRepository, EventJsonConverter eventJsonConverter) {
        this.eventRepository = eventRepository;
        this.eventJsonConverter = eventJsonConverter;
    }

    @Override
    public EventModelBuilder key(String key) {
        this.key = key;
        return this;
    }
    
    @Override
    public EventModelBuilder deploymentName(String deploymentName) {
        this.deploymentName = deploymentName;
        return this;
    }
    
    @Override
    public EventModelBuilder resourceName(String resourceName) {
        this.resourceName = resourceName;
        return this;
    }
    
    @Override
    public EventModelBuilder category(String category) {
        this.category = category;
        return this;
    }
    
    @Override
    public EventModelBuilder parentDeploymentId(String parentDeploymentId) {
        this.parentDeploymentId = parentDeploymentId;
        return this;
    }
    
    @Override
    public EventModelBuilder deploymentTenantId(String deploymentTenantId) {
        this.deploymentTenantId = deploymentTenantId;
        return this;
    }

    @Override
    public EventModelBuilder correlationParameter(String name, String type) {
        eventPayloadDefinitions.put(name, EventPayload.correlation(name, type));
        return this;
    }

    @Override
    public EventModelBuilder payload(String name, String type) {
        eventPayloadDefinitions.put(name, new EventPayload(name, type));
        return this;
    }
    
    @Override
    public EventModel createEventModel() {
        return buildEventModel();
    }

    @Override
    public EventDeployment deploy() {
        if (resourceName == null) {
            throw new FlowableIllegalArgumentException("A resource name is mandatory");
        }
        
        EventModel eventModel = buildEventModel();

        return eventRepository.createDeployment()
            .name(deploymentName)
            .addEventDefinition(resourceName, eventJsonConverter.convertToJson(eventModel))
            .category(category)
            .parentDeploymentId(parentDeploymentId)
            .tenantId(deploymentTenantId)
            .deploy();
    }

    protected EventModel buildEventModel() {
        EventModel eventModel = new EventModel();

        if (StringUtils.isNotEmpty(key)) {
            eventModel.setKey(key);
        } else {
            throw new FlowableIllegalArgumentException("An event definition key is mandatory");
        }

        eventModel.setPayload(eventPayloadDefinitions.values());

        return eventModel;
    }
}
