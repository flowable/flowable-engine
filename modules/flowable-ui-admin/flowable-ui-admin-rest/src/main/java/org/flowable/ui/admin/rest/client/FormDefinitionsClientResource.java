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
import org.flowable.ui.admin.domain.EndpointType;
import org.flowable.ui.admin.domain.ServerConfig;
import org.flowable.ui.admin.service.engine.FormDefinitionService;
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
public class FormDefinitionsClientResource extends AbstractClientResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(FormDefinitionsClientResource.class);

    @Autowired
    protected FormDefinitionService clientService;

    /**
     * GET a list of deployed form definitions.
     */
    @GetMapping(value = "/rest/admin/form-definitions", produces = "application/json")
    public JsonNode listFormDefinitions(HttpServletRequest request) {
        ServerConfig serverConfig = retrieveServerConfig(EndpointType.FORM);
        Map<String, String[]> parameterMap = getRequestParametersWithoutServerId(request);
        return clientService.listForms(serverConfig, parameterMap);
    }

    /**
     * GET process definition's list of deployed form definitions.
     */
    @GetMapping(value = "/rest/admin/process-definition-form-definitions/{processDefinitionId}", produces = "application/json")
    public JsonNode getProcessDefinitionForms(@PathVariable String processDefinitionId, HttpServletRequest request) {
        return clientService.getProcessDefinitionForms(retrieveServerConfig(EndpointType.PROCESS), processDefinitionId);
    }

    /**
     * GET case definition's list of deployed form definitions.
     */
    @GetMapping(value = "/rest/admin/case-definition-form-definitions/{processDefinitionId}", produces = "application/json")
    public JsonNode getCaseDefinitionForms(@PathVariable String processDefinitionId, HttpServletRequest request) {
        return clientService.getCaseDefinitionForms(retrieveServerConfig(EndpointType.CMMN), processDefinitionId);
    }
}
