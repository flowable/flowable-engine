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

import org.flowable.ui.admin.domain.EndpointType;
import org.flowable.ui.admin.domain.ServerConfig;
import org.flowable.ui.admin.service.engine.CaseInstanceService;
import org.flowable.ui.admin.service.engine.exception.FlowableServiceException;
import org.flowable.ui.common.service.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@RestController
@RequestMapping("/app")
public class CaseInstanceClientResource extends AbstractClientResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseInstanceClientResource.class);

    @Autowired
    protected CaseInstanceService clientService;

    @RequestMapping(value = "/rest/admin/case-instances/{caseInstanceId}", method = RequestMethod.GET, produces = "application/json")
    public JsonNode getProcessInstance(@PathVariable String caseInstanceId, @RequestParam(required = false, defaultValue = "false") boolean runtime) throws BadRequestException {

        ServerConfig serverConfig = retrieveServerConfig(EndpointType.CMMN);
        try {
            return clientService.getCaseInstance(serverConfig, caseInstanceId);
        } catch (FlowableServiceException e) {
            LOGGER.error("Error getting case instance {}", caseInstanceId, e);
            throw new BadRequestException(e.getMessage());
        }
    }

    @RequestMapping(value = "/rest/admin/case-instances/{caseInstanceId}/tasks", method = RequestMethod.GET)
    public JsonNode getSubtasks(@PathVariable String caseInstanceId) throws BadRequestException {
        ServerConfig serverConfig = retrieveServerConfig(EndpointType.CMMN);
        try {
            return clientService.getTasks(serverConfig, caseInstanceId);
        } catch (FlowableServiceException e) {
            LOGGER.error("Error getting tasks for case instance {}", caseInstanceId, e);
            throw new BadRequestException(e.getMessage());
        }
    }

    @RequestMapping(value = "/rest/admin/case-instances/{caseInstanceId}/variables", method = RequestMethod.GET)
    public JsonNode getVariables(@PathVariable String caseInstanceId) throws BadRequestException {
        ServerConfig serverConfig = retrieveServerConfig(EndpointType.CMMN);
        try {
            return clientService.getVariables(serverConfig, caseInstanceId);
        } catch (FlowableServiceException e) {
            LOGGER.error("Error getting variables for case instance {}", caseInstanceId, e);
            throw new BadRequestException(e.getMessage());
        }
    }

    @RequestMapping(value = "/rest/admin/case-instances/{caseInstanceId}/variables/{variableName}", method = RequestMethod.PUT)
    @ResponseStatus(value = HttpStatus.OK)
    public void updateVariable(@PathVariable String caseInstanceId, @PathVariable String variableName, @RequestBody ObjectNode body) throws BadRequestException {
        ServerConfig serverConfig = retrieveServerConfig(EndpointType.CMMN);
        try {
            clientService.updateVariable(serverConfig, caseInstanceId, variableName, body);
        } catch (FlowableServiceException e) {
            LOGGER.error("Error updating variable {} for case instance {}", variableName, caseInstanceId, e);
            throw new BadRequestException(e.getMessage());
        }
    }

    @RequestMapping(value = "/rest/admin/case-instances/{caseInstanceId}/variables", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void createVariable(@PathVariable String caseInstanceId, @RequestBody ObjectNode body) throws BadRequestException {
        ServerConfig serverConfig = retrieveServerConfig(EndpointType.CMMN);
        try {
            clientService.createVariable(serverConfig, caseInstanceId, body);
        } catch (FlowableServiceException e) {
            LOGGER.error("Error creating variable for case instance {}", caseInstanceId, e);
            throw new BadRequestException(e.getMessage());
        }
    }

    @RequestMapping(value = "/rest/admin/case-instances/{caseInstanceId}/variables/{variableName}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteVariable(@PathVariable String caseInstanceId, @PathVariable String variableName) throws BadRequestException {
        ServerConfig serverConfig = retrieveServerConfig(EndpointType.CMMN);
        try {
            clientService.deleteVariable(serverConfig, caseInstanceId, variableName);
        } catch (FlowableServiceException e) {
            LOGGER.error("Error deleting variable for case instance {}", caseInstanceId, e);
            throw new BadRequestException(e.getMessage());
        }
    }

    @RequestMapping(value = "/rest/admin/case-instances/{caseInstanceId}/jobs", method = RequestMethod.GET)
    public JsonNode getJobs(@PathVariable String caseInstanceId) throws BadRequestException {
        ServerConfig serverConfig = retrieveServerConfig(EndpointType.CMMN);
        try {
            return clientService.getJobs(serverConfig, caseInstanceId);
        } catch (FlowableServiceException e) {
            LOGGER.error("Error getting jobs for case instance {}", caseInstanceId, e);
            throw new BadRequestException(e.getMessage());
        }
    }

    @RequestMapping(value = "/rest/admin/case-instances/{caseInstanceId}", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void executeAction(@PathVariable String caseInstanceId, @RequestBody JsonNode actionBody) throws BadRequestException {
        ServerConfig serverConfig = retrieveServerConfig(EndpointType.CMMN);
        try {
            clientService.executeAction(serverConfig, caseInstanceId, actionBody);
        } catch (FlowableServiceException e) {
            LOGGER.error("Error executing action on case instance {}", caseInstanceId, e);
            throw new BadRequestException(e.getMessage());
        }
    }

    @RequestMapping(value = "/rest/admin/case-instances/{caseInstanceId}/decision-executions", method = RequestMethod.GET)
    public JsonNode getDecisionExecutions(@PathVariable String caseInstanceId) throws BadRequestException {
        ServerConfig serverConfig = retrieveServerConfig(EndpointType.DMN);
        try {
            return clientService.getDecisionExecutions(serverConfig, caseInstanceId);
        } catch (FlowableServiceException e) {
            LOGGER.error("Error getting decision executions {}", caseInstanceId, e);
            throw new BadRequestException(e.getMessage());
        }
    }
}
