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
package org.flowable.ui.admin.service.engine;

import java.util.Map;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.flowable.ui.admin.domain.ServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Service for invoking Flowable REST services.
 */
@Service
public class AppDefinitionService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AppDefinitionService.class);

    @Autowired
    protected FlowableClientService clientUtil;
    
    @Autowired
    protected ObjectMapper objectMapper;

    public JsonNode listAppDefinitions(ServerConfig serverConfig, Map<String, String[]> parameterMap) {
        URIBuilder builder = clientUtil.createUriBuilder("app-repository/app-definitions");

        for (String name : parameterMap.keySet()) {
            builder.addParameter(name, parameterMap.get(name)[0]);
        }
        HttpGet get = new HttpGet(clientUtil.getServerUrl(serverConfig, builder.toString()));
        return clientUtil.executeRequest(get, serverConfig);
    }

    public JsonNode getAppDefinition(ServerConfig serverConfig, String appDefinitionId) {
        HttpGet get = new HttpGet(clientUtil.getServerUrl(serverConfig, "app-repository/app-definitions/" + appDefinitionId));
        return clientUtil.executeRequest(get, serverConfig);
    }
    
    public JsonNode getAppDefinitionProcessDefinitions(ServerConfig serverConfig, String deploymentId) {
        return getDefinitions("repository/deployments?parentDeploymentId=" + deploymentId, 
                        "repository/process-definitions?deploymentId=", serverConfig);
    }
    
    public JsonNode getAppDefinitionCaseDefinitions(ServerConfig serverConfig, String deploymentId) {
        return getDefinitions("cmmn-repository/deployments?parentDeploymentId=" + deploymentId, 
                        "cmmn-repository/case-definitions?deploymentId=", serverConfig);
    }
    
    public JsonNode getAppDefinitionDecisionTables(ServerConfig serverConfig, String deploymentId) {
        return getDefinitions("dmn-repository/deployments?parentDeploymentId=" + deploymentId, 
                        "dmn-repository/decision-tables?deploymentId=", serverConfig);
    }
    
    public JsonNode getAppDefinitionFormDefinitions(ServerConfig serverConfig, String deploymentId) {
        return getDefinitions("form-repository/deployments?parentDeploymentId=" + deploymentId, 
                        "form-repository/form-definitions?deploymentId=", serverConfig);
    }
    
    protected JsonNode getDefinitions(String deploymentUrl, String definitionUrl, ServerConfig serverConfig) {
        HttpGet get = new HttpGet(clientUtil.getServerUrl(serverConfig, deploymentUrl));
        JsonNode deploymentsNode = clientUtil.executeRequest(get, serverConfig);
        if (deploymentsNode != null && deploymentsNode.has("data")) {
            JsonNode deploymentArrayNode = deploymentsNode.get("data");
            if (deploymentArrayNode.isNull() == false && deploymentArrayNode.isArray() && deploymentArrayNode.size() > 0) {
                String restDeploymentId = deploymentArrayNode.get(0).get("id").asText();
                HttpGet getDefinitions = new HttpGet(clientUtil.getServerUrl(serverConfig, definitionUrl + restDeploymentId));
                return clientUtil.executeRequest(getDefinitions, serverConfig);
            }
        }
        
        ObjectNode emptyDefinitionNode = objectMapper.createObjectNode();
        emptyDefinitionNode.put("size", 0);
        emptyDefinitionNode.putArray("data");
        return emptyDefinitionNode;
    }
}
