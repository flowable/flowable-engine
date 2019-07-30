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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flowable.ui.admin.domain.EndpointType;
import org.flowable.ui.admin.domain.ServerConfig;
import org.flowable.ui.admin.service.engine.ContentItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author Yvo Swillens
 */
@RestController
@RequestMapping("/app")
public class ContentItemsClientResource extends AbstractClientResource {

    @Autowired
    protected ContentItemService clientService;

    @Autowired
    protected ObjectMapper objectMapper;

    /**
     * GET a list of content items.
     */
    @GetMapping(value = "/rest/admin/content-items", produces = "application/json")
    public JsonNode listFormDefinitions(HttpServletRequest request) {
        ServerConfig serverConfig = retrieveServerConfig(EndpointType.CONTENT);
        Map<String, String[]> parameterMap = getRequestParametersWithoutServerId(request);
        return clientService.listContentItems(serverConfig, parameterMap);
    }

    /**
     * GET process instance's list of content items.
     */
    @GetMapping(value = "/rest/admin/process-instance-content-items/{processInstanceId}", produces = "application/json")
    public JsonNode getProcessDefinitionForms(@PathVariable String processInstanceId, HttpServletRequest request) {
        ServerConfig serverConfig = retrieveServerConfig(EndpointType.CONTENT);

        Map<String, String[]> parameterMap = getRequestParametersWithoutServerId(request);
        String[] processInstanceIds = { processInstanceId };
        parameterMap.put("processInstanceId", processInstanceIds);

        return clientService.listContentItems(serverConfig, parameterMap);
    }
}
