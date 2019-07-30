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
import org.flowable.ui.admin.domain.EndpointType;
import org.flowable.ui.admin.domain.ServerConfig;
import org.flowable.ui.admin.service.engine.JobService;
import org.flowable.ui.admin.service.engine.ProcessDefinitionService;
import org.flowable.ui.admin.service.engine.ProcessInstanceService;
import org.flowable.ui.admin.service.engine.exception.FlowableServiceException;
import org.flowable.ui.common.service.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

/**
 * REST controller for managing the current user's account.
 */
@RestController
@RequestMapping("/app")
public class ProcessDefinitionClientResource extends AbstractClientResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModelsClientResource.class);

    @Autowired
    protected ProcessDefinitionService clientService;

    @Autowired
    protected ProcessInstanceService processInstanceService;

    @Autowired
    protected JobService jobService;

    @Autowired
    protected ObjectMapper objectMapper;

    /**
     * GET /rest/authenticate -> check if the user is authenticated, and return its login.
     */
    @GetMapping(value = "/rest/admin/process-definitions/{definitionId}", produces = "application/json")
    public JsonNode getProcessDefinition(@PathVariable String definitionId) throws BadRequestException {
        ServerConfig serverConfig = retrieveServerConfig(EndpointType.PROCESS);
        try {
            return clientService.getProcessDefinition(serverConfig, definitionId);
        } catch (FlowableServiceException e) {
            LOGGER.error("Error getting process definition {}", definitionId, e);
            throw new BadRequestException(e.getMessage());
        }
    }

    @PutMapping(value = "/rest/admin/process-definitions/{definitionId}", produces = "application/json")
    public JsonNode updateProcessDefinitionCategory(@PathVariable String definitionId,
            @RequestBody ObjectNode updateBody) throws BadRequestException {

        ServerConfig serverConfig = retrieveServerConfig(EndpointType.PROCESS);
        if (updateBody.has("category")) {
            try {
                String category = null;
                if (!updateBody.get("category").isNull()) {
                    category = updateBody.get("category").asText();
                }
                return clientService.updateProcessDefinitionCategory(serverConfig, definitionId, category);
            } catch (FlowableServiceException e) {
                LOGGER.error("Error updating process definition category {}", definitionId, e);
                throw new BadRequestException(e.getMessage());
            }

        } else {
            LOGGER.error("No required category found in request body");
            throw new BadRequestException("Category is required in body");
        }
    }

    @GetMapping(value = "/rest/admin/process-definitions/{definitionId}/process-instances", produces = "application/json")
    public JsonNode getProcessInstances(@PathVariable String definitionId) throws BadRequestException {
        ServerConfig serverConfig = retrieveServerConfig(EndpointType.PROCESS);
        try {
            ObjectNode bodyNode = objectMapper.createObjectNode();
            bodyNode.put("processDefinitionId", definitionId);
            return processInstanceService.listProcesInstancesForProcessDefinition(bodyNode, serverConfig);

        } catch (FlowableServiceException e) {
            LOGGER.error("Error getting process instances for process definition {}", definitionId, e);
            throw new BadRequestException(e.getMessage());
        }
    }

    @GetMapping(value = "/rest/admin/process-definitions/{definitionId}/jobs", produces = "application/json")
    public JsonNode getJobs(@PathVariable String definitionId) throws BadRequestException {
        ServerConfig serverConfig = retrieveServerConfig(EndpointType.PROCESS);
        try {
            return jobService.listJobs(serverConfig, Collections.singletonMap("processDefinitionId", new String[] { definitionId }));

        } catch (FlowableServiceException e) {
            LOGGER.error("Error getting jobs for process definition {}", definitionId, e);
            throw new BadRequestException(e.getMessage());
        }
    }

    @PostMapping(value = "/rest/admin/process-definitions/{definitionId}/batch-migrate")
    @ResponseStatus(value = HttpStatus.OK)
    public void migrateInstancesOfProcessDefinition(@PathVariable String definitionId, @RequestBody String migrationDocument) throws BadRequestException {
        ServerConfig serverConfig = retrieveServerConfig(EndpointType.PROCESS);
        try {
            clientService.migrateInstancesOfProcessDefinition(serverConfig, definitionId, migrationDocument);
        } catch (FlowableServiceException e) {
            LOGGER.error("Error migrating instances of process definition {}", definitionId, e);
            throw new BadRequestException(e.getMessage());
        }
    }
}
