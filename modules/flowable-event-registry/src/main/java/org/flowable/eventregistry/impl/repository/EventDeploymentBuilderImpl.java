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
package org.flowable.eventregistry.impl.repository;

import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;

import org.apache.commons.io.IOUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.api.EventDeploymentBuilder;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.flowable.eventregistry.impl.EventRepositoryServiceImpl;
import org.flowable.eventregistry.impl.persistence.entity.EventDeploymentEntity;
import org.flowable.eventregistry.impl.persistence.entity.EventResourceEntity;
import org.flowable.eventregistry.impl.persistence.entity.EventResourceEntityManager;
import org.flowable.eventregistry.impl.util.CommandContextUtil;

/**
 * @author Tijs Rademakers
 */
public class EventDeploymentBuilderImpl implements EventDeploymentBuilder, Serializable {

    private static final long serialVersionUID = 1L;
    protected static final String DEFAULT_ENCODING = "UTF-8";

    protected transient EventRepositoryServiceImpl repositoryService;
    protected transient EventResourceEntityManager resourceEntityManager;

    protected EventDeploymentEntity deployment;
    protected boolean isDuplicateFilterEnabled;

    public EventDeploymentBuilderImpl() {
        EventRegistryEngineConfiguration eventRegistryEngineConfiguration = CommandContextUtil.getEventRegistryConfiguration();
        this.repositoryService = (EventRepositoryServiceImpl) eventRegistryEngineConfiguration.getEventRepositoryService();
        this.deployment = eventRegistryEngineConfiguration.getDeploymentEntityManager().create();
        this.resourceEntityManager = eventRegistryEngineConfiguration.getResourceEntityManager();
    }

    @Override
    public EventDeploymentBuilder addInputStream(String resourceName, InputStream inputStream) {
        if (inputStream == null) {
            throw new FlowableException("inputStream for resource '" + resourceName + "' is null");
        }

        byte[] bytes = null;
        try {
            bytes = IOUtils.toByteArray(inputStream);
        } catch (Exception e) {
            throw new FlowableException("could not get byte array from resource '" + resourceName + "'", e);
        }

        if (bytes == null) {
            throw new FlowableException("byte array for resource '" + resourceName + "' is null");
        }

        EventResourceEntity resource = resourceEntityManager.create();
        resource.setName(resourceName);
        resource.setBytes(bytes);
        deployment.addResource(resource);
        return this;
    }

    @Override
    public EventDeploymentBuilder addClasspathResource(String resource) {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(resource);
        if (inputStream == null) {
            throw new FlowableException("resource '" + resource + "' not found");
        }
        return addInputStream(resource, inputStream);
    }

    @Override
    public EventDeploymentBuilder addString(String resourceName, String text) {
        if (text == null) {
            throw new FlowableException("text is null");
        }

        EventResourceEntity resource = resourceEntityManager.create();
        resource.setName(resourceName);
        try {
            resource.setBytes(text.getBytes(DEFAULT_ENCODING));
        } catch (UnsupportedEncodingException e) {
            throw new FlowableException("Unable to get event definition bytes.", e);
        }
        deployment.addResource(resource);
        return this;
    }

    @Override
    public EventDeploymentBuilder addEventDefinitionBytes(String resourceName, byte[] eventDefinitionBytes) {
        if (eventDefinitionBytes == null) {
            throw new FlowableException("event definition bytes is null");
        }

        EventResourceEntity resource = resourceEntityManager.create();
        resource.setName(resourceName);
        resource.setBytes(eventDefinitionBytes);
        deployment.addResource(resource);
        return this;
    }

    @Override
    public EventDeploymentBuilder addEventDefinition(String resourceName, String eventDefinition) {
        addString(resourceName, eventDefinition);
        return this;
    }
    
    @Override
    public EventDeploymentBuilder addChannelDefinitionBytes(String resourceName, byte[] channelDefinitionBytes) {
        if (channelDefinitionBytes == null) {
            throw new FlowableException("channel definition bytes is null");
        }

        EventResourceEntity resource = resourceEntityManager.create();
        resource.setName(resourceName);
        resource.setBytes(channelDefinitionBytes);
        deployment.addResource(resource);
        return this;
    }

    @Override
    public EventDeploymentBuilder addChannelDefinition(String resourceName, String channelDefinition) {
        addString(resourceName, channelDefinition);
        return this;
    }

    @Override
    public EventDeploymentBuilder name(String name) {
        deployment.setName(name);
        return this;
    }

    @Override
    public EventDeploymentBuilder category(String category) {
        deployment.setCategory(category);
        return this;
    }

    @Override
    public EventDeploymentBuilder tenantId(String tenantId) {
        deployment.setTenantId(tenantId);
        return this;
    }

    @Override
    public EventDeploymentBuilder parentDeploymentId(String parentDeploymentId) {
        deployment.setParentDeploymentId(parentDeploymentId);
        return this;
    }

    @Override
    public EventDeploymentBuilder enableDuplicateFiltering() {
        this.isDuplicateFilterEnabled = true;
        return this;
    }

    @Override
    public EventDeployment deploy() {
        return repositoryService.deploy(this);
    }

    // getters and setters
    // //////////////////////////////////////////////////////

    public EventDeploymentEntity getDeployment() {
        return deployment;
    }

    public boolean isDuplicateFilterEnabled() {
        return isDuplicateFilterEnabled;
    }
}
