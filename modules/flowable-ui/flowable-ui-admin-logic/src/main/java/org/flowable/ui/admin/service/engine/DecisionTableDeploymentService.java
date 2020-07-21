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

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.flowable.ui.admin.domain.ServerConfig;
import org.flowable.ui.admin.service.engine.exception.FlowableServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for invoking Flowable REST services.
 */
@Service
public class DecisionTableDeploymentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DecisionTableDeploymentService.class);

    @Autowired
    protected FlowableClientService clientUtil;

    public JsonNode listDeployments(ServerConfig serverConfig, Map<String, String[]> parameterMap) {

        URIBuilder builder = null;
        try {
            builder = new URIBuilder("dmn-repository/deployments");
        } catch (Exception e) {
            LOGGER.error("Error building uri", e);
            throw new FlowableServiceException("Error building uri", e);
        }

        for (String name : parameterMap.keySet()) {
            builder.addParameter(name, parameterMap.get(name)[0]);
        }
        HttpGet get = new HttpGet(clientUtil.getServerUrl(serverConfig, builder.toString()));
        return clientUtil.executeRequest(get, serverConfig);
    }

    public JsonNode getDeployment(ServerConfig serverConfig, String deploymentId) {
        HttpGet get = new HttpGet(clientUtil.getServerUrl(serverConfig, "dmn-repository/deployments/" + deploymentId));
        return clientUtil.executeRequest(get, serverConfig);
    }

    public void deleteDeployment(ServerConfig serverConfig, HttpServletResponse httpResponse, String appDeploymentId) {
        HttpDelete delete = new HttpDelete(clientUtil.getServerUrl(serverConfig, clientUtil.createUriBuilder("dmn-repository/deployments/" + appDeploymentId)));
        clientUtil.execute(delete, httpResponse, serverConfig);
    }

    public JsonNode uploadDeployment(ServerConfig serverConfig, String name, InputStream inputStream) throws IOException {
        HttpPost post = new HttpPost(clientUtil.getServerUrl(serverConfig, "dmn-repository/deployments"));
        HttpEntity reqEntity = MultipartEntityBuilder.create()
                .addBinaryBody(name, IOUtils.toByteArray(inputStream), ContentType.APPLICATION_OCTET_STREAM, name)
                .build();
        post.setEntity(reqEntity);
        return clientUtil.executeRequest(post, serverConfig, 201);
    }

}
