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

import org.activiti.domain.EndpointType;
import org.activiti.domain.ServerConfig;
import org.activiti.service.engine.FormDefinitionService;
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
 * @author Yvo Swillens
 */
@RestController
public class FormDefinitionClientResource extends AbstractClientResource {

    @Autowired
    protected FormDefinitionService clientService;

    @RequestMapping(value = "/rest/activiti/form-definitions/{formDefinitionId}", method = RequestMethod.GET, produces = "application/json")
    public JsonNode getForm(@PathVariable String formDefinitionId) throws BadRequestException {

        ServerConfig serverConfig = retrieveServerConfig(EndpointType.FORM);
        try {
            return clientService.getForm(serverConfig, formDefinitionId);
        } catch (ActivitiServiceException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

//    @RequestMapping(value = "/rest/activiti/form-definitions/{formDefinitionId}/editorJson", method = RequestMethod.GET, produces = "application/json")
//    public JsonNode getEditorJsonForForm(@PathVariable String formDefinitionId) throws BadRequestException {
//
//        ServerConfig serverConfig = retrieveServerConfig(EndpointType.FORM);
//        try {
//            return clientService.getEditorJsonForForm(serverConfig, formDefinitionId);
//        } catch (ActivitiServiceException e) {
//            throw new BadRequestException(e.getMessage());
//        }
//    }
//
//    @RequestMapping(value = "/rest/activiti/process-definition-start-form-definition/{processDefinitionId}", method = RequestMethod.GET, produces = "application/json")
//    public JsonNode getProcessDefinitionStartForm(@PathVariable String processDefinitionId) throws BadRequestException {
//
//        ServerConfig serverConfig = retrieveServerConfig(EndpointType.FORM);
//        try {
//            return clientService.getProcessDefinitionStartForm(serverConfig, processDefinitionId);
//        } catch (ActivitiServiceException e) {
//            throw new BadRequestException(e.getMessage());
//        }
//    }

}