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
import org.flowable.dmn.api.DmnDecision;
import org.flowable.dmn.api.DmnDeployment;
import org.flowable.dmn.api.DmnDeploymentQuery;
import org.flowable.dmn.api.DmnRepositoryService;
import org.flowable.dmn.rest.service.api.DmnRestApiInterceptor;
import org.flowable.dmn.rest.service.api.DmnRestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Yvo Swillens
 */
@RestController
public class BaseDecisionResource {

    @Autowired
    protected ContentTypeResolver contentTypeResolver;

    @Autowired
    protected DmnRestResponseFactory dmnRestResponseFactory;

    @Autowired
    protected DmnRepositoryService dmnRepositoryService;
    
    @Autowired(required=false)
    protected DmnRestApiInterceptor restApiInterceptor;

    /**
     * Returns the {@link DmnDecision} that is requested. Throws the right exceptions when bad request was made or decision was not found.
     */
    protected DmnDecision getDecisionFromRequest(String decisionId) {
        DmnDecision decision = dmnRepositoryService.getDecision(decisionId);

        if (decision == null) {
            throw new FlowableObjectNotFoundException("Could not find a decision with id '" + decisionId + "'.");
        }
        
        if (restApiInterceptor != null) {
            restApiInterceptor.accessDecisionTableInfoById(decision);
        }
        
        return decision;
    }

    protected byte[] getDeploymentResourceData(String deploymentId, String resourceId, HttpServletResponse response) {

        if (deploymentId == null) {
            throw new FlowableIllegalArgumentException("No deployment id provided");
        }
        if (resourceId == null) {
            throw new FlowableIllegalArgumentException("No resource id provided");
        }

        // Check if deployment exists
        DmnDeploymentQuery deploymentQuery = dmnRepositoryService.createDeploymentQuery().deploymentId(deploymentId);
        
        DmnDeployment deployment = deploymentQuery.singleResult();
        if (deployment == null) {
            throw new FlowableObjectNotFoundException("Could not find a deployment with id '" + deploymentId);
        }
        
        if (restApiInterceptor != null) {
            restApiInterceptor.accessDeploymentById(deployment);
        }

        List<String> resourceList = dmnRepositoryService.getDeploymentResourceNames(deploymentId);

        if (resourceList.contains(resourceId)) {
            final InputStream resourceStream = dmnRepositoryService.getResourceAsStream(deploymentId, resourceId);

            String contentType = contentTypeResolver.resolveContentType(resourceId);
            response.setContentType(contentType);
            try {
                return IOUtils.toByteArray(resourceStream);
            } catch (Exception e) {
                throw new FlowableException("Error converting resource stream", e);
            }
        } else {
            // Resource not found in deployment
            throw new FlowableObjectNotFoundException("Could not find a resource with id '" + resourceId + "' in deployment '" + deploymentId);
        }
    }
}
