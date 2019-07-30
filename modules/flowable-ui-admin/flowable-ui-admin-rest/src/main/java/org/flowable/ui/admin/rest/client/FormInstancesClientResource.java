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
package org.flowable.ui.admin.rest.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.ui.admin.domain.EndpointType;
import org.flowable.ui.admin.domain.ServerConfig;
import org.flowable.ui.admin.service.engine.FormInstanceService;
import org.flowable.ui.admin.service.engine.exception.FlowableServiceException;
import org.flowable.ui.common.service.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author Bassam Al-Sarori
 * @author Yvo Swillens
 */
@RestController
@RequestMapping("/app")
public class FormInstancesClientResource extends AbstractClientResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(FormInstancesClientResource.class);

    @Autowired
    protected FormInstanceService clientService;

    @Autowired
    protected ObjectMapper objectMapper;

    @GetMapping(value = "/rest/admin/form-definition-form-instances/{formDefinitionId}", produces = "application/json")
    public JsonNode listFormInstances(HttpServletRequest request, @PathVariable String formDefinitionId) {
        ServerConfig serverConfig = retrieveServerConfig(EndpointType.FORM);

        try {
            ObjectNode bodyNode = objectMapper.createObjectNode();
            bodyNode.put("formDefinitionId", formDefinitionId);

            return clientService.getFormInstances(serverConfig, bodyNode);
        } catch (FlowableServiceException e) {
            LOGGER.error("Error getting form instance", e);
            throw new BadRequestException(e.getMessage());
        }
    }

    @GetMapping(value = "/rest/admin/process-form-instances/{processInstanceId}", produces = "application/json")
    public JsonNode getProcessFormInstances(@PathVariable String processInstanceId) {
        ServerConfig serverConfig = retrieveServerConfig(EndpointType.FORM);

        try {
            ObjectNode bodyNode = objectMapper.createObjectNode();
            bodyNode.put("processInstanceId", processInstanceId);

            return clientService.getFormInstances(serverConfig, bodyNode);
        } catch (FlowableServiceException e) {
            LOGGER.error("Error getting form instances for process instance id {}", processInstanceId, e);
            throw new BadRequestException(e.getMessage());
        }
    }

    @GetMapping(value = "/rest/admin/case-form-instances/{caseInstanceId}", produces = "application/json")
    public JsonNode getCaseFormInstances(@PathVariable String caseInstanceId) {
        ServerConfig serverConfig = retrieveServerConfig(EndpointType.FORM);

        try {
            ObjectNode bodyNode = objectMapper.createObjectNode();
            bodyNode.put("scopeId", caseInstanceId);
            bodyNode.put("scopeType", ScopeTypes.CMMN);

            return clientService.getFormInstances(serverConfig, bodyNode);
        } catch (FlowableServiceException e) {
            LOGGER.error("Error getting form instances for case instance id {}", caseInstanceId, e);
            throw new BadRequestException(e.getMessage());
        }
    }

    @GetMapping(value = "/rest/admin/form-instances", produces = "application/json")
    public JsonNode getSubmittedForm(HttpServletRequest request) {
        ServerConfig serverConfig = retrieveServerConfig(EndpointType.FORM);

        Map<String, String[]> parameterMap = getRequestParametersWithoutServerId(request);

        return clientService.listFormInstances(serverConfig, parameterMap);
    }
}
