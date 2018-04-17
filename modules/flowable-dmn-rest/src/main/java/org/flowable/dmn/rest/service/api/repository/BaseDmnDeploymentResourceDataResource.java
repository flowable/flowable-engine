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
package org.flowable.dmn.rest.service.api.repository;

import java.io.InputStream;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.rest.resolver.ContentTypeResolver;
import org.flowable.dmn.api.DmnDeployment;
import org.flowable.dmn.api.DmnRepositoryService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Yvo Swillens
 */
public class BaseDmnDeploymentResourceDataResource {

    @Autowired
    protected ContentTypeResolver contentTypeResolver;

    @Autowired
    protected DmnRepositoryService dmnRepositoryService;

    protected byte[] getDmnDeploymentResourceData(String deploymentId, String resourceName, HttpServletResponse response) {

        if (deploymentId == null) {
            throw new FlowableIllegalArgumentException("No deployment id provided");
        }
        if (resourceName == null) {
            throw new FlowableIllegalArgumentException("No resource name provided");
        }

        // Check if deployment exists
        DmnDeployment deployment = dmnRepositoryService.createDeploymentQuery().deploymentId(deploymentId).singleResult();
        if (deployment == null) {
            throw new FlowableObjectNotFoundException("Could not find a DMN deployment with id '" + deploymentId);
        }

        List<String> resourceList = dmnRepositoryService.getDeploymentResourceNames(deploymentId);

        if (resourceList.contains(resourceName)) {
            final InputStream resourceStream = dmnRepositoryService.getResourceAsStream(deploymentId, resourceName);

            String contentType = contentTypeResolver.resolveContentType(resourceName);
            response.setContentType(contentType);
            try {
                return IOUtils.toByteArray(resourceStream);
            } catch (Exception e) {
                throw new FlowableException("Error converting resource stream", e);
            }
        } else {
            // Resource not found in deployment
            throw new FlowableObjectNotFoundException("Could not find a resource with name '" + resourceName + "' in deployment '" + deploymentId);
        }
    }
}
