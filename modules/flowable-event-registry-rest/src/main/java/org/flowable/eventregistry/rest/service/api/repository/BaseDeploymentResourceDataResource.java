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

package org.flowable.eventregistry.rest.service.api.repository;

import java.io.InputStream;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.http.entity.ContentType;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.rest.resolver.ContentTypeResolver;
import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.rest.service.api.EventRegistryRestApiInterceptor;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Tijs Rademakers
 */
public class BaseDeploymentResourceDataResource {

    @Autowired
    protected ContentTypeResolver contentTypeResolver;

    @Autowired
    protected EventRepositoryService repositoryService;
    
    @Autowired(required=false)
    protected EventRegistryRestApiInterceptor restApiInterceptor;

    protected byte[] getDeploymentResourceData(String deploymentId, String resourceName, HttpServletResponse response) {

        if (deploymentId == null) {
            throw new FlowableIllegalArgumentException("No deployment id provided");
        }
        if (resourceName == null) {
            throw new FlowableIllegalArgumentException("No resource name provided");
        }

        // Check if deployment exists
        EventDeployment deployment = repositoryService.createDeploymentQuery().deploymentId(deploymentId).singleResult();
        if (deployment == null) {
            throw new FlowableObjectNotFoundException("Could not find a deployment with id '" + deploymentId + "'.", EventDeployment.class);
        }
        
        if (restApiInterceptor != null) {
            restApiInterceptor.accessDeploymentById(deployment);
        }

        List<String> resourceList = repositoryService.getDeploymentResourceNames(deploymentId);

        if (resourceList.contains(resourceName)) {
            final InputStream resourceStream = repositoryService.getResourceAsStream(deploymentId, resourceName);

            String contentType = null;
            if (resourceName.toLowerCase().endsWith(".event") || resourceName.toLowerCase().endsWith(".channel")) {
                contentType = ContentType.APPLICATION_JSON.getMimeType();
            } else {
                contentType = contentTypeResolver.resolveContentType(resourceName);
            }
            response.setContentType(contentType);
            
            try {
                return IOUtils.toByteArray(resourceStream);
            } catch (Exception e) {
                throw new FlowableException("Error converting resource stream", e);
            }
        } else {
            // Resource not found in deployment
            throw new FlowableObjectNotFoundException("Could not find a resource with name '" + resourceName + "' in deployment '" + deploymentId + "'.", String.class);
        }
    }
}
