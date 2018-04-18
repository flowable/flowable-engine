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
package org.flowable.form.rest.service.api.repository;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import org.apache.commons.io.IOUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.rest.resolver.ContentTypeResolver;
import org.flowable.form.api.FormDefinition;
import org.flowable.form.api.FormDeployment;
import org.flowable.form.api.FormRepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.List;

/**
 * @author Yvo Swillens
 */
@RestController
@Api(tags = { "Form Definitions" }, description = "Manage Form Definitions", authorizations = { @Authorization(value = "basicAuth") })
public class FormDefinitionResourceDataResource {

    @Autowired
    protected FormRepositoryService formRepositoryService;

    @Autowired
    protected ContentTypeResolver contentTypeResolver;

    @ApiOperation(value = "Get a form definition resource content", nickname = "getFormDefinitionContent", tags = { "Form Definitions" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates both form definition and resource have been found and the resource data has been returned."),
            @ApiResponse(code = 404, message = "Indicates the requested form definition was not found or there is no resource with the given id present in the process definition. The status-description contains additional information.")
    })
    @GetMapping(value = "/form-repository/form-definitions/{formDefinitionId}/resourcedata", produces = "application/json")
    @ResponseBody
    //FIXME Name of the method ?
    public byte[] getDecisionTableResource(@ApiParam(name = "formDefinitionId") @PathVariable String formDefinitionId, HttpServletResponse response) {
        FormDefinition formDefinition = formRepositoryService.getFormDefinition(formDefinitionId);

        if (formDefinition == null) {
            throw new FlowableObjectNotFoundException("Could not find a form definition with id '" + formDefinitionId);
        }
        if (formDefinition.getDeploymentId() == null) {
            throw new FlowableException("No deployment id available");
        }
        if (formDefinition.getResourceName() == null) {
            throw new FlowableException("No resource name available");
        }

        // Check if deployment exists
        FormDeployment deployment = formRepositoryService.createDeploymentQuery().deploymentId(formDefinition.getDeploymentId()).singleResult();
        if (deployment == null) {
            throw new FlowableObjectNotFoundException("Could not find a deployment with id '" + formDefinition.getDeploymentId());
        }

        List<String> resourceList = formRepositoryService.getDeploymentResourceNames(formDefinition.getDeploymentId());

        if (resourceList.contains(formDefinition.getResourceName())) {
            final InputStream resourceStream = formRepositoryService.getResourceAsStream(
                    formDefinition.getDeploymentId(), formDefinition.getResourceName());

            String contentType = contentTypeResolver.resolveContentType(formDefinition.getResourceName());
            response.setContentType(contentType);
            try {
                return IOUtils.toByteArray(resourceStream);
            } catch (Exception e) {
                throw new FlowableException("Error converting resource stream", e);
            }
        } else {
            // Resource not found in deployment
            throw new FlowableObjectNotFoundException("Could not find a resource with id '" +
                    formDefinition.getResourceName() + "' in deployment '" + formDefinition.getDeploymentId());
        }
    }
}
