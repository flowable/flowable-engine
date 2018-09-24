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
import org.flowable.ui.admin.service.engine.AppDefinitionService;
import org.flowable.ui.admin.service.engine.exception.FlowableServiceException;
import org.flowable.ui.common.service.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

@RestController
@RequestMapping("/app")
public class AppDefinitionClientResource extends AbstractClientResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppDefinitionClientResource.class);

    @Autowired
    protected AppDefinitionService clientService;

    @RequestMapping(value = "/rest/admin/app-definitions/{definitionId}", method = RequestMethod.GET, produces = "application/json")
    public JsonNode getCaseDefinition(@PathVariable String definitionId) {
        ServerConfig serverConfig = retrieveServerConfig(EndpointType.APP);
        try {
            return clientService.getAppDefinition(serverConfig, definitionId);
        } catch (FlowableServiceException e) {
            LOGGER.error("Error getting app definition {}", definitionId, e);
            throw new BadRequestException(e.getMessage());
        }
    }

    @RequestMapping(value = "/rest/admin/app-definitions/{definitionId}/process-definitions", method = RequestMethod.GET, produces = "application/json")
    public JsonNode getProcessDefinitions(@PathVariable String definitionId, @RequestParam(name="deploymentId", required=true) String deploymentId) {
        ServerConfig serverConfig = retrieveServerConfig(EndpointType.PROCESS);
        try {
            return clientService.getAppDefinitionProcessDefinitions(serverConfig, deploymentId);

        } catch (FlowableServiceException e) {
            LOGGER.error("Error getting process definitions for app definition {}", definitionId, e);
            throw new BadRequestException(e.getMessage());
        }
    }
    
    @RequestMapping(value = "/rest/admin/app-definitions/{definitionId}/case-definitions", method = RequestMethod.GET, produces = "application/json")
    public JsonNode getCaseDefinitions(@PathVariable String definitionId, @RequestParam(name="deploymentId", required=true) String deploymentId) {
        ServerConfig serverConfig = retrieveServerConfig(EndpointType.CMMN);
        try {
            return clientService.getAppDefinitionCaseDefinitions(serverConfig, deploymentId);

        } catch (FlowableServiceException e) {
            LOGGER.error("Error getting case definitions for app definition {}", definitionId, e);
            throw new BadRequestException(e.getMessage());
        }
    }

    @RequestMapping(value = "/rest/admin/app-definitions/{definitionId}/decision-tables", method = RequestMethod.GET, produces = "application/json")
    public JsonNode getDecisionTables(@PathVariable String definitionId, @RequestParam(name="deploymentId", required=true) String deploymentId) {
        ServerConfig serverConfig = retrieveServerConfig(EndpointType.DMN);
        try {
            return clientService.getAppDefinitionDecisionTables(serverConfig, deploymentId);

        } catch (FlowableServiceException e) {
            LOGGER.error("Error getting decision tables for app definition {}", definitionId, e);
            throw new BadRequestException(e.getMessage());
        }
    }
    
    @RequestMapping(value = "/rest/admin/app-definitions/{definitionId}/form-definitions", method = RequestMethod.GET, produces = "application/json")
    public JsonNode getFormDefinitions(@PathVariable String definitionId, @RequestParam(name="deploymentId", required=true) String deploymentId) {
        ServerConfig serverConfig = retrieveServerConfig(EndpointType.FORM);
        try {
            return clientService.getAppDefinitionFormDefinitions(serverConfig, deploymentId);

        } catch (FlowableServiceException e) {
            LOGGER.error("Error getting form definitions for app definition {}", definitionId, e);
            throw new BadRequestException(e.getMessage());
        }
    }
    
}
