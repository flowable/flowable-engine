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
package org.flowable.app.rest;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.entity.ContentType;
import org.flowable.app.api.repository.AppDefinition;
import org.flowable.app.api.repository.AppDeployment;
import org.flowable.app.rest.service.api.repository.AppDefinitionResponse;
import org.flowable.app.rest.service.api.repository.AppDeploymentResourceResponse;
import org.flowable.app.rest.service.api.repository.AppDeploymentResponse;
import org.flowable.common.rest.resolver.ContentTypeResolver;

/**
 * @author Tijs Rademakers
 */
public class AppRestResponseFactory {

    public AppDefinitionResponse createAppDefinitionResponse(AppDefinition appDefinition) {
        return createAppDefinitionResponse(appDefinition, createUrlBuilder());
    }

    public AppDefinitionResponse createAppDefinitionResponse(AppDefinition appDefinition, AppRestUrlBuilder urlBuilder) {
        AppDefinitionResponse response = new AppDefinitionResponse(appDefinition);
        response.setUrl(urlBuilder.buildUrl(AppRestUrls.URL_APP_DEFINITION, appDefinition.getId()));

        return response;
    }

    public List<AppDefinitionResponse> createAppDefinitionResponseList(List<AppDefinition> appDefinitions) {
        AppRestUrlBuilder urlBuilder = createUrlBuilder();
        List<AppDefinitionResponse> responseList = new ArrayList<>();
        for (AppDefinition appDefinition : appDefinitions) {
            responseList.add(createAppDefinitionResponse(appDefinition, urlBuilder));
        }
        return responseList;
    }

    public List<AppDeploymentResponse> createAppDeploymentResponseList(List<AppDeployment> deployments) {
        AppRestUrlBuilder urlBuilder = createUrlBuilder();
        List<AppDeploymentResponse> responseList = new ArrayList<>();
        for (AppDeployment deployment : deployments) {
            responseList.add(createAppDeploymentResponse(deployment, urlBuilder));
        }
        return responseList;
    }

    public AppDeploymentResponse createAppDeploymentResponse(AppDeployment deployment) {
        return createAppDeploymentResponse(deployment, createUrlBuilder());
    }

    public AppDeploymentResponse createAppDeploymentResponse(AppDeployment deployment, AppRestUrlBuilder urlBuilder) {
        return new AppDeploymentResponse(deployment, urlBuilder.buildUrl(AppRestUrls.URL_DEPLOYMENT, deployment.getId()));
    }
    
    public List<AppDeploymentResourceResponse> createDeploymentResourceResponseList(String deploymentId, List<String> resourceList, ContentTypeResolver contentTypeResolver) {
        AppRestUrlBuilder urlBuilder = createUrlBuilder();
        // Add additional metadata to the artifact-strings before returning
        List<AppDeploymentResourceResponse> responseList = new ArrayList<>();
        for (String resourceId : resourceList) {
            String contentType = null;
            if (resourceId.toLowerCase().endsWith(".app")) {
                contentType = ContentType.APPLICATION_JSON.getMimeType();
            } else {
                contentType = contentTypeResolver.resolveContentType(resourceId);
            }
            responseList.add(createDeploymentResourceResponse(deploymentId, resourceId, contentType, urlBuilder));
        }
        return responseList;
    }

    public AppDeploymentResourceResponse createDeploymentResourceResponse(String deploymentId, String resourceId, String contentType) {
        return createDeploymentResourceResponse(deploymentId, resourceId, contentType, createUrlBuilder());
    }

    public AppDeploymentResourceResponse createDeploymentResourceResponse(String deploymentId, String resourceId, String contentType, AppRestUrlBuilder urlBuilder) {
        // Create URL's
        String resourceUrl = urlBuilder.buildUrl(AppRestUrls.URL_DEPLOYMENT_RESOURCE, deploymentId, resourceId);
        String resourceContentUrl = urlBuilder.buildUrl(AppRestUrls.URL_DEPLOYMENT_RESOURCE_CONTENT, deploymentId, resourceId);

        // Determine type
        String type = "resource";
        if (resourceId.endsWith(".app")) {
            type = "appDefinition";
        }
        return new AppDeploymentResourceResponse(resourceId, resourceUrl, resourceContentUrl, contentType, type);
    }

    protected AppRestUrlBuilder createUrlBuilder() {
        return AppRestUrlBuilder.fromCurrentRequest();
    }
}
