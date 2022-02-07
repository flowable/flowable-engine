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

package org.flowable.eventregistry.rest.service.api;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.entity.ContentType;
import org.flowable.common.rest.resolver.ContentTypeResolver;
import org.flowable.common.rest.util.RestUrlBuilder;
import org.flowable.eventregistry.api.ChannelDefinition;
import org.flowable.eventregistry.api.EventDefinition;
import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.impl.deployer.ParsedDeploymentBuilder;
import org.flowable.eventregistry.rest.service.api.repository.ChannelDefinitionResponse;
import org.flowable.eventregistry.rest.service.api.repository.DeploymentResourceResponse;
import org.flowable.eventregistry.rest.service.api.repository.EventDefinitionResponse;
import org.flowable.eventregistry.rest.service.api.repository.EventDeploymentResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Default implementation of a {@link EventRegistryRestResponseFactory}.
 */
public class EventRegistryRestResponseFactory {

    protected ObjectMapper objectMapper;

    public EventRegistryRestResponseFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<EventDeploymentResponse> createDeploymentResponseList(List<EventDeployment> deployments) {
        RestUrlBuilder urlBuilder = createUrlBuilder();
        List<EventDeploymentResponse> responseList = new ArrayList<>(deployments.size());
        for (EventDeployment instance : deployments) {
            responseList.add(createDeploymentResponse(instance, urlBuilder));
        }
        return responseList;
    }

    public EventDeploymentResponse createDeploymentResponse(EventDeployment deployment) {
        return createDeploymentResponse(deployment, createUrlBuilder());
    }

    public EventDeploymentResponse createDeploymentResponse(EventDeployment deployment, RestUrlBuilder urlBuilder) {
        return new EventDeploymentResponse(deployment, urlBuilder.buildUrl(EventRestUrls.URL_DEPLOYMENT, deployment.getId()));
    }

    public List<DeploymentResourceResponse> createDeploymentResourceResponseList(String deploymentId, List<String> resourceList, ContentTypeResolver contentTypeResolver) {
        RestUrlBuilder urlBuilder = createUrlBuilder();
        // Add additional metadata to the artifact-strings before returning
        List<DeploymentResourceResponse> responseList = new ArrayList<>(resourceList.size());
        for (String resourceId : resourceList) {
            String contentType = null;
            if (resourceId.toLowerCase().endsWith(".cmmn")) {
                contentType = ContentType.TEXT_XML.getMimeType();
            } else {
                contentType = contentTypeResolver.resolveContentType(resourceId);
            }
            responseList.add(createDeploymentResourceResponse(deploymentId, resourceId, contentType, urlBuilder));
        }
        return responseList;
    }

    public DeploymentResourceResponse createDeploymentResourceResponse(String deploymentId, String resourceId, String contentType) {
        return createDeploymentResourceResponse(deploymentId, resourceId, contentType, createUrlBuilder());
    }

    public DeploymentResourceResponse createDeploymentResourceResponse(String deploymentId, String resourceId, String contentType, RestUrlBuilder urlBuilder) {
        // Create URL's
        String resourceUrl = urlBuilder.buildUrl(EventRestUrls.URL_DEPLOYMENT_RESOURCE, deploymentId, resourceId);
        String resourceContentUrl = urlBuilder.buildUrl(EventRestUrls.URL_DEPLOYMENT_RESOURCE_CONTENT, deploymentId, resourceId);

        // Determine type
        String type = "resource";
        for (String suffix : ParsedDeploymentBuilder.EVENT_RESOURCE_SUFFIXES) {
            if (resourceId.endsWith(suffix)) {
                type = "eventDefinition";
                break;
            }
        }
        
        for (String suffix : ParsedDeploymentBuilder.CHANNEL_RESOURCE_SUFFIXES) {
            if (resourceId.endsWith(suffix)) {
                type = "channelDefinition";
                break;
            }
        }
        
        return new DeploymentResourceResponse(resourceId, resourceUrl, resourceContentUrl, contentType, type);
    }

    public List<EventDefinitionResponse> createEventDefinitionResponseList(List<EventDefinition> eventDefinitions) {
        RestUrlBuilder urlBuilder = createUrlBuilder();
        List<EventDefinitionResponse> responseList = new ArrayList<>(eventDefinitions.size());
        for (EventDefinition eventDef : eventDefinitions) {
            responseList.add(createEventDefinitionResponse(eventDef, urlBuilder));
        }
        return responseList;
    }

    public EventDefinitionResponse createEventDefinitionResponse(EventDefinition eventDefinition) {
        return createEventDefinitionResponse(eventDefinition, createUrlBuilder());
    }

    public EventDefinitionResponse createEventDefinitionResponse(EventDefinition eventDefinition, RestUrlBuilder urlBuilder) {
        EventDefinitionResponse response = new EventDefinitionResponse();
        response.setUrl(urlBuilder.buildUrl(EventRestUrls.URL_EVENT_DEFINITION, eventDefinition.getId()));
        response.setId(eventDefinition.getId());
        response.setKey(eventDefinition.getKey());
        response.setVersion(eventDefinition.getVersion());
        response.setCategory(eventDefinition.getCategory());
        response.setName(eventDefinition.getName());
        response.setDescription(eventDefinition.getDescription());
        response.setTenantId(eventDefinition.getTenantId());

        // Links to other resources
        response.setDeploymentId(eventDefinition.getDeploymentId());
        response.setDeploymentUrl(urlBuilder.buildUrl(EventRestUrls.URL_DEPLOYMENT, eventDefinition.getDeploymentId()));
        response.setResourceName(eventDefinition.getResourceName());
        response.setResource(urlBuilder.buildUrl(EventRestUrls.URL_DEPLOYMENT_RESOURCE, eventDefinition.getDeploymentId(), eventDefinition.getResourceName()));

        return response;
    }
    
    public List<ChannelDefinitionResponse> createChannelDefinitionResponseList(List<ChannelDefinition> channelDefinitions) {
        RestUrlBuilder urlBuilder = createUrlBuilder();
        List<ChannelDefinitionResponse> responseList = new ArrayList<>(channelDefinitions.size());
        for (ChannelDefinition channelDef : channelDefinitions) {
            responseList.add(createChannelDefinitionResponse(channelDef, urlBuilder));
        }
        return responseList;
    }

    public ChannelDefinitionResponse createChannelDefinitionResponse(ChannelDefinition channelDefinition) {
        return createChannelDefinitionResponse(channelDefinition, createUrlBuilder());
    }

    public ChannelDefinitionResponse createChannelDefinitionResponse(ChannelDefinition channelDefinition, RestUrlBuilder urlBuilder) {
        ChannelDefinitionResponse response = new ChannelDefinitionResponse();
        response.setUrl(urlBuilder.buildUrl(EventRestUrls.URL_CHANNEL_DEFINITION, channelDefinition.getId()));
        response.setId(channelDefinition.getId());
        response.setKey(channelDefinition.getKey());
        response.setVersion(channelDefinition.getVersion());
        response.setCategory(channelDefinition.getCategory());
        response.setName(channelDefinition.getName());
        response.setType(channelDefinition.getType());
        response.setImplementation(channelDefinition.getImplementation());
        response.setCreateTime(channelDefinition.getCreateTime());
        response.setDescription(channelDefinition.getDescription());
        response.setTenantId(channelDefinition.getTenantId());

        // Links to other resources
        response.setDeploymentId(channelDefinition.getDeploymentId());
        response.setResourceName(channelDefinition.getResourceName());
        response.setDeploymentUrl(urlBuilder.buildUrl(EventRestUrls.URL_DEPLOYMENT, channelDefinition.getDeploymentId()));
        response.setResource(urlBuilder.buildUrl(EventRestUrls.URL_DEPLOYMENT_RESOURCE, channelDefinition.getDeploymentId(), channelDefinition.getResourceName()));

        return response;
    }
    
    protected String formatUrl(String serverRootUrl, String[] fragments, Object... arguments) {
        StringBuilder urlBuilder = new StringBuilder(serverRootUrl);
        for (String urlFragment : fragments) {
            urlBuilder.append("/");
            urlBuilder.append(MessageFormat.format(urlFragment, arguments));
        }
        return urlBuilder.toString();
    }

    protected RestUrlBuilder createUrlBuilder() {
        return RestUrlBuilder.fromCurrentRequest();
    }

}
