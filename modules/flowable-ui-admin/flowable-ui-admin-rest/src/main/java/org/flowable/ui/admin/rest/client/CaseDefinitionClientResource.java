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

import java.util.Collections;

import org.flowable.ui.admin.domain.EndpointType;
import org.flowable.ui.admin.domain.ServerConfig;
import org.flowable.ui.admin.service.engine.CaseDefinitionService;
import org.flowable.ui.admin.service.engine.CaseInstanceService;
import org.flowable.ui.admin.service.engine.JobService;
import org.flowable.ui.admin.service.engine.exception.FlowableServiceException;
import org.flowable.ui.common.service.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@RestController
@RequestMapping("/app")
public class CaseDefinitionClientResource extends AbstractClientResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseDefinitionClientResource.class);

    @Autowired
    protected CaseDefinitionService clientService;

    @Autowired
    protected CaseInstanceService caseInstanceService;

    @Autowired
    protected JobService jobService;

    @Autowired
    protected ObjectMapper objectMapper;

    /**
     * GET /rest/authenticate -> check if the user is authenticated, and return its login.
     */
    @RequestMapping(value = "/rest/admin/case-definitions/{definitionId}", method = RequestMethod.GET, produces = "application/json")
    public JsonNode getCaseDefinition(@PathVariable String definitionId) throws BadRequestException {
        ServerConfig serverConfig = retrieveServerConfig(EndpointType.CMMN);
        try {
            return clientService.getCaseDefinition(serverConfig, definitionId);
        } catch (FlowableServiceException e) {
            LOGGER.error("Error getting case definition {}", definitionId, e);
            throw new BadRequestException(e.getMessage());
        }
    }

    @RequestMapping(value = "/rest/admin/case-definitions/{definitionId}/case-instances", method = RequestMethod.GET, produces = "application/json")
    public JsonNode getProcessInstances(@PathVariable String definitionId) throws BadRequestException {
        ServerConfig serverConfig = retrieveServerConfig(EndpointType.CMMN);
        try {
            ObjectNode bodyNode = objectMapper.createObjectNode();
            bodyNode.put("caseDefinitionId", definitionId);
            return caseInstanceService.listCaseInstancesForCaseDefinition(bodyNode, serverConfig);

        } catch (FlowableServiceException e) {
            LOGGER.error("Error getting case instances for case definition {}", definitionId, e);
            throw new BadRequestException(e.getMessage());
        }
    }

    @RequestMapping(value = "/rest/admin/case-definitions/{definitionId}/jobs", method = RequestMethod.GET, produces = "application/json")
    public JsonNode getJobs(@PathVariable String definitionId) throws BadRequestException {
        ServerConfig serverConfig = retrieveServerConfig(EndpointType.CMMN);
        try {
            return jobService.listJobs(serverConfig, Collections.singletonMap("caseDefinitionId", new String[] { definitionId }));

        } catch (FlowableServiceException e) {
            LOGGER.error("Error getting jobs for case definition {}", definitionId, e);
            throw new BadRequestException(e.getMessage());
        }
    }
}
