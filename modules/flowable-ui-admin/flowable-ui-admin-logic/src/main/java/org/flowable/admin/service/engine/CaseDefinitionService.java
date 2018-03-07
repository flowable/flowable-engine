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
package org.flowable.admin.service.engine;

import java.util.Map;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.flowable.admin.domain.ServerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Service for invoking Flowable REST services.
 */
@Service
public class CaseDefinitionService {

    @Autowired
    protected FlowableClientService clientUtil;

    public JsonNode listCaseDefinitions(ServerConfig serverConfig, Map<String, String[]> parameterMap) {
        URIBuilder builder = clientUtil.createUriBuilder("cmmn-repository/case-definitions");

        for (String name : parameterMap.keySet()) {
            builder.addParameter(name, parameterMap.get(name)[0]);
        }
        HttpGet get = new HttpGet(clientUtil.getServerUrl(serverConfig, builder.toString()));
        return clientUtil.executeRequest(get, serverConfig);
    }

    public JsonNode getCaseDefinition(ServerConfig serverConfig, String caseDefinitionId) {
        HttpGet get = new HttpGet(clientUtil.getServerUrl(serverConfig, "cmmn-repository/case-definitions/" + caseDefinitionId));
        return clientUtil.executeRequest(get, serverConfig);
    }

    public JsonNode getCaseDefinitionForms(ServerConfig serverConfig, String caseDefinitionId) {
        HttpGet get = new HttpGet(clientUtil.getServerUrl(serverConfig, "repository/case-definitions/" + caseDefinitionId + "/form-definitions"));
        return clientUtil.executeRequest(get, serverConfig);
    }
}
