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
import org.flowable.ui.admin.service.engine.DecisionHistoricExecutionService;
import org.flowable.ui.admin.service.engine.exception.FlowableServiceException;
import org.flowable.ui.common.service.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Yvo Swillens
 */
@RestController
@RequestMapping("/app")
public class DecisionTableHistoricExecutionClientResource extends AbstractClientResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(DecisionTableHistoricExecutionClientResource.class);

    @Autowired
    protected DecisionHistoricExecutionService clientService;

    @GetMapping(value = "/rest/admin/decision-tables/history/{executionId}", produces = "application/json")
    public JsonNode getDecisionTableHistoricExecution(@PathVariable String executionId) throws BadRequestException {

        ServerConfig serverConfig = retrieveServerConfig(EndpointType.DMN);
        try {
            return clientService.getHistoricDecisionExecution(serverConfig, executionId);
        } catch (FlowableServiceException e) {
            LOGGER.error("Error getting historic decision table execution {}", executionId, e);
            throw new BadRequestException(e.getMessage());
        }
    }

    @GetMapping(value = "/rest/admin/decision-tables/history/{executionId}/auditdata", produces = "application/json")
    public JsonNode getDecisionTableHistoricExecutionAuditData(@PathVariable String executionId) throws BadRequestException {

        ServerConfig serverConfig = retrieveServerConfig(EndpointType.DMN);
        try {
            return clientService.getHistoricDecisionExecutionAuditData(serverConfig, executionId);
        } catch (FlowableServiceException e) {
            LOGGER.error("Error getting historic decision table execution {}", executionId, e);
            throw new BadRequestException(e.getMessage());
        }
    }
}
