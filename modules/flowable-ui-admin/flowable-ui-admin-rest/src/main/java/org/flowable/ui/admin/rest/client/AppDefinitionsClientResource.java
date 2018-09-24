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

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.flowable.ui.admin.domain.EndpointType;
import org.flowable.ui.admin.domain.ServerConfig;
import org.flowable.ui.admin.service.engine.AppDefinitionService;
import org.flowable.ui.admin.service.engine.exception.FlowableServiceException;
import org.flowable.ui.common.service.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

@RestController
@RequestMapping("/app")
public class AppDefinitionsClientResource extends AbstractClientResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppDefinitionsClientResource.class);

    @Autowired
    protected AppDefinitionService clientService;

    @RequestMapping(value = "/rest/admin/app-definitions", method = RequestMethod.GET, produces = "application/json")
    public JsonNode listCaseDefinitions(HttpServletRequest request) {
        LOGGER.debug("REST request to get a list of app definitions");

        JsonNode resultNode = null;
        ServerConfig serverConfig = retrieveServerConfig(EndpointType.APP);
        Map<String, String[]> parameterMap = getRequestParametersWithoutServerId(request);

        try {
            resultNode = clientService.listAppDefinitions(serverConfig, parameterMap);

        } catch (FlowableServiceException e) {
            LOGGER.error("Error getting app definitions", e);
            throw new BadRequestException(e.getMessage());
        }

        return resultNode;
    }
}
