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
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.flowable.ui.admin.domain.EndpointType;
import org.flowable.ui.admin.domain.ServerConfig;
import org.flowable.ui.admin.service.engine.ProcessInstanceService;
import org.flowable.ui.admin.service.engine.exception.FlowableServiceException;
import org.flowable.ui.common.service.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing the current user's account.
 */
@RestController
@RequestMapping("/app")
public class ProcessInstanceClientResource extends AbstractClientResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessInstanceClientResource.class);

    @Autowired
    protected ProcessInstanceService clientService;

    @GetMapping(value = "/rest/admin/process-instances/{processInstanceId}", produces = "application/json")
    public JsonNode getProcessInstance(@PathVariable String processInstanceId, @RequestParam(required = false, defaultValue = "false") boolean runtime) throws BadRequestException {

        ServerConfig serverConfig = retrieveServerConfig(EndpointType.PROCESS);
        try {
            return clientService.getProcessInstance(serverConfig, processInstanceId);
        } catch (FlowableServiceException e) {
            LOGGER.error("Error getting process instance {}", processInstanceId, e);
            throw new BadRequestException(e.getMessage());
        }
    }

    @GetMapping(value = "/rest/admin/process-instances/{processInstanceId}/tasks")
    public JsonNode getSubtasks(@PathVariable String processInstanceId) throws BadRequestException {
        ServerConfig serverConfig = retrieveServerConfig(EndpointType.PROCESS);
        try {
            return clientService.getTasks(serverConfig, processInstanceId);
        } catch (FlowableServiceException e) {
            LOGGER.error("Error getting tasks for process instance {}", processInstanceId, e);
            throw new BadRequestException(e.getMessage());
        }
    }

    @GetMapping(value = "/rest/admin/process-instances/{processInstanceId}/variables")
    public JsonNode getVariables(@PathVariable String processInstanceId) throws BadRequestException {
        ServerConfig serverConfig = retrieveServerConfig(EndpointType.PROCESS);
        try {
            return clientService.getVariables(serverConfig, processInstanceId);
        } catch (FlowableServiceException e) {
            LOGGER.error("Error getting variables for process instance {}", processInstanceId, e);
            throw new BadRequestException(e.getMessage());
        }
    }

    @PutMapping(value = "/rest/admin/process-instances/{processInstanceId}/variables/{variableName}")
    @ResponseStatus(value = HttpStatus.OK)
    public void updateVariable(@PathVariable String processInstanceId, @PathVariable String variableName, @RequestBody ObjectNode body) throws BadRequestException {
        ServerConfig serverConfig = retrieveServerConfig(EndpointType.PROCESS);
        try {
            clientService.updateVariable(serverConfig, processInstanceId, variableName, body);
        } catch (FlowableServiceException e) {
            LOGGER.error("Error updating variable {} for process instance {}", variableName, processInstanceId, e);
            throw new BadRequestException(e.getMessage());
        }
    }

    @PostMapping(value = "/rest/admin/process-instances/{processInstanceId}/variables")
    @ResponseStatus(value = HttpStatus.OK)
    public void createVariable(@PathVariable String processInstanceId, @RequestBody ObjectNode body) throws BadRequestException {
        ServerConfig serverConfig = retrieveServerConfig(EndpointType.PROCESS);
        try {
            clientService.createVariable(serverConfig, processInstanceId, body);
        } catch (FlowableServiceException e) {
            LOGGER.error("Error creating variable for process instance {}", processInstanceId, e);
            throw new BadRequestException(e.getMessage());
        }
    }

    @DeleteMapping(value = "/rest/admin/process-instances/{processInstanceId}/variables/{variableName}")
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteVariable(@PathVariable String processInstanceId, @PathVariable String variableName) throws BadRequestException {
        ServerConfig serverConfig = retrieveServerConfig(EndpointType.PROCESS);
        try {
            clientService.deleteVariable(serverConfig, processInstanceId, variableName);
        } catch (FlowableServiceException e) {
            LOGGER.error("Error deleting variable for process instance {}", processInstanceId, e);
            throw new BadRequestException(e.getMessage());
        }
    }

    @GetMapping(value = "/rest/admin/process-instances/{processInstanceId}/subprocesses")
    public JsonNode getSubProcesses(@PathVariable String processInstanceId) throws BadRequestException {
        ServerConfig serverConfig = retrieveServerConfig(EndpointType.PROCESS);
        try {
            return clientService.getSubProcesses(serverConfig, processInstanceId);
        } catch (FlowableServiceException e) {
            LOGGER.error("Error getting sub processes for process instance {}", processInstanceId, e);
            throw new BadRequestException(e.getMessage());
        }
    }

    @GetMapping(value = "/rest/admin/process-instances/{processInstanceId}/jobs")
    public JsonNode getJobs(@PathVariable String processInstanceId) throws BadRequestException {
        ServerConfig serverConfig = retrieveServerConfig(EndpointType.PROCESS);
        try {
            return clientService.getJobs(serverConfig, processInstanceId);
        } catch (FlowableServiceException e) {
            LOGGER.error("Error getting jobs for process instance {}", processInstanceId, e);
            throw new BadRequestException(e.getMessage());
        }
    }

    @PostMapping(value = "/rest/admin/process-instances/{processInstanceId}")
    @ResponseStatus(value = HttpStatus.OK)
    public void executeAction(@PathVariable String processInstanceId, @RequestBody JsonNode actionBody) throws BadRequestException {
        ServerConfig serverConfig = retrieveServerConfig(EndpointType.PROCESS);
        try {
            clientService.executeAction(serverConfig, processInstanceId, actionBody);
        } catch (FlowableServiceException e) {
            LOGGER.error("Error executing action on process instance {}", processInstanceId, e);
            throw new BadRequestException(e.getMessage());
        }
    }

    @PostMapping(value = "/rest/admin/process-instances/{processInstanceId}/change-state")
    @ResponseStatus(value = HttpStatus.OK)
    public void changeActivityState(@PathVariable String processInstanceId, @RequestBody JsonNode changeStateBody) throws BadRequestException {
        ServerConfig serverConfig = retrieveServerConfig(EndpointType.PROCESS);
        try {
            clientService.changeActivityState(serverConfig, processInstanceId, changeStateBody);
        } catch (FlowableServiceException e) {
            LOGGER.error("Error changing activity state for process instance {}", processInstanceId, e);
            throw new BadRequestException(e.getMessage());
        }
    }

    @PostMapping(value = "/rest/admin/process-instances/{processInstanceId}/migrate")
    @ResponseStatus(value = HttpStatus.OK)
    public void migrateProcessInstance(@PathVariable String processInstanceId, @RequestBody String migrationDocument) throws BadRequestException {
        ServerConfig serverConfig = retrieveServerConfig(EndpointType.PROCESS);
        try {
            clientService.migrateProcessInstance(serverConfig, processInstanceId, migrationDocument);
        } catch (FlowableServiceException e) {
            LOGGER.error("Error migrating process instance {}", processInstanceId, e);
            throw new BadRequestException(e.getMessage());
        }
    }

    @GetMapping(value = "/rest/admin/process-instances/{processInstanceId}/decision-executions")
    public JsonNode getDecisionExecutions(@PathVariable String processInstanceId) throws BadRequestException {
        ServerConfig serverConfig = retrieveServerConfig(EndpointType.DMN);
        try {
            return clientService.getDecisionExecutions(serverConfig, processInstanceId);
        } catch (FlowableServiceException e) {
            LOGGER.error("Error getting decision executions {}", processInstanceId, e);
            throw new BadRequestException(e.getMessage());
        }
    }
}
