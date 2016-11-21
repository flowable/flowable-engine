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
package org.activiti.web.rest.client;

import java.util.Collections;

import org.activiti.domain.EndpointType;
import org.activiti.domain.ServerConfig;
import org.activiti.service.engine.JobService;
import org.activiti.service.engine.ProcessDefinitionService;
import org.activiti.service.engine.ProcessInstanceService;
import org.activiti.service.engine.exception.ActivitiServiceException;
import org.activiti.web.rest.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * REST controller for managing the current user's account.
 */
@RestController
public class ProcessDefinitionClientResource extends AbstractClientResource {

  @Autowired
  protected ProcessDefinitionService clientService;

  @Autowired
  private ProcessInstanceService processInstanceService;

  @Autowired
  private JobService jobService;

  @Autowired
  protected ObjectMapper objectMapper;

  /**
   * GET /rest/authenticate -> check if the user is authenticated, and return
   * its login.
   */
  @RequestMapping(value = "/rest/activiti/process-definitions/{definitionId}", method = RequestMethod.GET, produces = "application/json")
  public JsonNode getProcessDefinition(@PathVariable String definitionId) throws BadRequestException {

    ServerConfig serverConfig = retrieveServerConfig(EndpointType.PROCESS);
    try {
      return clientService.getProcessDefinition(serverConfig, definitionId);
    } catch (ActivitiServiceException e) {
      throw new BadRequestException(e.getMessage());
    }
  }

  @RequestMapping(value = "/rest/activiti/process-definitions/{definitionId}", method = RequestMethod.PUT, produces = "application/json")
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
      } catch (ActivitiServiceException e) {
        e.printStackTrace();
        throw new BadRequestException(e.getMessage());
      }
    } else {
      throw new BadRequestException("Category is required in body");
    }
  }

  @RequestMapping(value = "/rest/activiti/process-definitions/{definitionId}/process-instances", method = RequestMethod.GET, produces = "application/json")
  public JsonNode getProcessInstances(@PathVariable String definitionId) throws BadRequestException {
    ServerConfig serverConfig = retrieveServerConfig(EndpointType.PROCESS);
    try {
      ObjectNode bodyNode = objectMapper.createObjectNode();
      bodyNode.put("processDefinitionId", definitionId);
      return processInstanceService.listProcesInstancesForProcessDefinition(bodyNode, serverConfig);
    } catch (ActivitiServiceException e) {
      throw new BadRequestException(e.getMessage());
    }
  }

  @RequestMapping(value = "/rest/activiti/process-definitions/{definitionId}/jobs", method = RequestMethod.GET, produces = "application/json")
  public JsonNode getJobs(@PathVariable String definitionId) throws BadRequestException {
    ServerConfig serverConfig = retrieveServerConfig(EndpointType.PROCESS);
    try {
      return jobService.listJobs(serverConfig, Collections.singletonMap("processDefinitionId", new String[]{definitionId}));
    } catch (ActivitiServiceException e) {
      throw new BadRequestException(e.getMessage());
    }
  }
}
