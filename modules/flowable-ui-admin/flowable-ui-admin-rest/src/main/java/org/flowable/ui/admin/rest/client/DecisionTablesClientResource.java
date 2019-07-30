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
import org.flowable.ui.admin.service.engine.DecisionTableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author Yvo Swillens
 * @author Bassam Al-Sarori
 */
@RestController
@RequestMapping("/app")
public class DecisionTablesClientResource extends AbstractClientResource {

    @Autowired
    protected DecisionTableService clientService;

    /**
     * GET list of deployed decision tables.
     */
    @GetMapping(value = "/rest/admin/decision-tables", produces = "application/json")
    public JsonNode listDecisionTables(HttpServletRequest request) {
        ServerConfig serverConfig = retrieveServerConfig(EndpointType.DMN);
        Map<String, String[]> parameterMap = getRequestParametersWithoutServerId(request);
        return clientService.listDecisionTables(serverConfig, parameterMap);
    }

    /**
     * GET process definition's list of deployed decision tables.
     */
    @GetMapping(value = "/rest/admin/process-definition-decision-tables/{processDefinitionId}", produces = "application/json")
    public JsonNode getProcessDefinitionDecisionTables(@PathVariable String processDefinitionId, HttpServletRequest request) {
        return clientService.getProcessDefinitionDecisionTables(retrieveServerConfig(EndpointType.PROCESS), processDefinitionId);
    }

    /**
     * GET case definition's list of deployed decision tables.
     */
    @GetMapping(value = "/rest/admin/case-definition-decision-tables/{caseDefinitionId}", produces = "application/json")
    public JsonNode getCaseDefinitionDecisionTables(@PathVariable String caseDefinitionId, HttpServletRequest request) {
        return clientService.getCaseDefinitionDecisionTables(retrieveServerConfig(EndpointType.CMMN), caseDefinitionId);
    }
}
