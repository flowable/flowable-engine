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

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.activiti.domain.EndpointType;
import org.activiti.domain.ServerConfig;
import org.activiti.service.engine.SubmittedFormService;
import org.activiti.service.engine.exception.ActivitiServiceException;
import org.activiti.web.rest.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author Bassam Al-Sarori
 */
@RestController
public class SubmittedFormsClientResource extends AbstractClientResource {

  @Autowired
  protected SubmittedFormService clientService;

  @Autowired
  protected ObjectMapper objectMapper;

  @RequestMapping(value = "/rest/activiti/submitted-forms", method = RequestMethod.GET, produces = "application/json")
  public JsonNode listSubmittedForms(HttpServletRequest request) {
    JsonNode resultNode = null;
    ServerConfig serverConfig = retrieveServerConfig(EndpointType.FORM);
    Map<String, String[]> parameterMap = getRequestParametersWithoutServerId(request);

    try {
      resultNode = clientService.listSubmittedForms(serverConfig, parameterMap);

    } catch (ActivitiServiceException e) {
      throw new BadRequestException(e.getMessage());
    }

    return resultNode;
  }

  @RequestMapping(value = "/rest/activiti/form-submitted-forms/{formId}", method = RequestMethod.GET, produces = "application/json")
  public JsonNode listFormSubmittedForms(HttpServletRequest request, @PathVariable String formId) {
    ServerConfig serverConfig = retrieveServerConfig(EndpointType.FORM);

    try {

      ObjectNode bodyNode = objectMapper.createObjectNode();
      bodyNode.put("formDefinitionId", formId);

      return clientService.listFormSubmittedForms(serverConfig, bodyNode);
    } catch (ActivitiServiceException e) {
      throw new BadRequestException(e.getMessage());
    }
  }

  @RequestMapping(value = "/rest/activiti/task-submitted-form/{taskId}", method = RequestMethod.GET, produces = "application/json")
  public JsonNode getTaskSubmittedForm(@PathVariable String taskId) {
    ServerConfig serverConfig = retrieveServerConfig(EndpointType.FORM);

    try {
      ObjectNode bodyNode = objectMapper.createObjectNode();
      bodyNode.put("taskId", taskId);

      return clientService.getTaskSubmittedForm(serverConfig, bodyNode);

    } catch (ActivitiServiceException e) {
      throw new BadRequestException(e.getMessage());
    }

  }

  @RequestMapping(value = "/rest/activiti/process-submitted-forms/{processInstanceId}", method = RequestMethod.GET, produces = "application/json")
  public JsonNode getProcessSubmittedForms(@PathVariable String processInstanceId) {
    ServerConfig serverConfig = retrieveServerConfig(EndpointType.FORM);

    try {
      ObjectNode bodyNode = objectMapper.createObjectNode();
      bodyNode.put("processInstanceId", processInstanceId);

      return clientService.getProcessSubmittedForms(serverConfig, bodyNode);
    } catch (ActivitiServiceException e) {
      throw new BadRequestException(e.getMessage());
    }
  }

  @RequestMapping(value = "/rest/activiti/submitted-forms/{submittedFormId}", method = RequestMethod.GET, produces = "application/json")
  public JsonNode getSubmittedForm(@PathVariable String submittedFormId) {
    ServerConfig serverConfig = retrieveServerConfig(EndpointType.FORM);

    try {
      return clientService.getSubmittedForm(serverConfig, submittedFormId);

    } catch (ActivitiServiceException e) {
      throw new BadRequestException(e.getMessage());
    }
  }
}