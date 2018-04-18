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

import javax.servlet.http.HttpServletRequest;

import org.flowable.ui.admin.domain.EndpointType;
import org.flowable.ui.admin.domain.ServerConfig;
import org.flowable.ui.admin.service.engine.FormInstanceService;
import org.flowable.ui.admin.service.engine.exception.FlowableServiceException;
import org.flowable.ui.common.service.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Yvo Swillens
 */
@RestController
@RequestMapping("/app")
public class FormInstanceClientResource extends AbstractClientResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(FormInstanceClientResource.class);

    @Autowired
    protected FormInstanceService clientService;

    @Autowired
    protected ObjectMapper objectMapper;

    @RequestMapping(value = "/rest/admin/form-instances/{formInstanceId}", method = RequestMethod.GET, produces = "application/json")
    public JsonNode getFormInstance(HttpServletRequest request, @PathVariable String formInstanceId) {
        ServerConfig serverConfig = retrieveServerConfig(EndpointType.FORM);
        return clientService.getFormInstance(serverConfig, formInstanceId);
    }

    @RequestMapping(value = "/rest/admin/task-form-instance/{taskId}", method = RequestMethod.GET, produces = "application/json")
    public JsonNode getTaskFormInstance(@PathVariable String taskId) {
        ServerConfig serverConfig = retrieveServerConfig(EndpointType.FORM);

        try {
            ObjectNode bodyNode = objectMapper.createObjectNode();
            bodyNode.put("taskId", taskId);

            return clientService.getFormInstances(serverConfig, bodyNode);

        } catch (FlowableServiceException e) {
            LOGGER.error("Error getting form instance for task id {}", taskId, e);
            throw new BadRequestException(e.getMessage());
        }
    }

    @RequestMapping(value = "/rest/admin/form-instances/{formInstanceId}/form-field-values", method = RequestMethod.GET, produces = "application/json")
    public JsonNode getFormInstanceFormFieldValues(HttpServletRequest request, @PathVariable String formInstanceId) {
        ServerConfig serverConfig = retrieveServerConfig(EndpointType.FORM);
        return clientService.getFormInstanceFormFieldValues(serverConfig, formInstanceId);
    }

}
