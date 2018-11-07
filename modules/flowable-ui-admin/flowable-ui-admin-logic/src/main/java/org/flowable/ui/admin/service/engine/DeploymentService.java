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
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
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
public class DeploymentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeploymentService.class);

    @Autowired
    protected FlowableClientService clientUtil;

    @Autowired
    protected ObjectMapper objectMapper;

    public JsonNode listDeployments(ServerConfig serverConfig, Map<String, String[]> parameterMap) {

        URIBuilder builder = null;
        try {
            builder = new URIBuilder("repository/deployments");
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
        HttpGet get = new HttpGet(clientUtil.getServerUrl(serverConfig, "repository/deployments/" + deploymentId));
        return clientUtil.executeRequest(get, serverConfig);
    }

    // public JsonNode uploadDeployment(ServerConfig serverConfig, String name, String bpmn20Xml) throws IOException {
    // HttpPost post = new HttpPost(getServerUrl(serverConfig, "repository/deployments"));
    // HttpEntity reqEntity = MultipartEntityBuilder.create()
    // .addBinaryBody(name, bpmn20Xml.getBytes(), ContentType.APPLICATION_XML, name)
    // .build();
    // post.setEntity(reqEntity);
    // return executeRequest(post, serverConfig, 201);
    // }

    public JsonNode uploadDeployment(ServerConfig serverConfig, String name, InputStream inputStream) throws IOException {

        String deploymentKey = null;
        String deploymentName = null;

        byte[] inputStreamByteArray = IOUtils.toByteArray(inputStream);

        // special handling for exported bar files
        if (name != null && (name.endsWith(".zip") || name.endsWith(".bar"))) {
            JsonNode appDefinitionJson = getAppDefinitionJson(new ByteArrayInputStream(inputStreamByteArray));

            if (appDefinitionJson != null) {
                if (appDefinitionJson.has("key") && appDefinitionJson.get("key") != null) {
                    deploymentKey = appDefinitionJson.get("key").asText();
                }
                if (appDefinitionJson.has("name") && appDefinitionJson.get("name") != null) {
                    deploymentName = appDefinitionJson.get("name").asText();
                }
            }
        }

        URIBuilder uriBuilder = clientUtil.createUriBuilder("repository/deployments");

        if (StringUtils.isNotEmpty(deploymentKey)) {
            uriBuilder.addParameter("deploymentKey", encode(deploymentKey));
        }
        if (StringUtils.isNotEmpty(deploymentName)) {
            uriBuilder.addParameter("deploymentName", encode(deploymentName));
        }

        HttpPost post = new HttpPost(clientUtil.getServerUrl(serverConfig, uriBuilder));
        HttpEntity reqEntity = MultipartEntityBuilder.create()
                .addBinaryBody(name, inputStreamByteArray, ContentType.APPLICATION_OCTET_STREAM, name)
                .build();
        post.setEntity(reqEntity);
        return clientUtil.executeRequest(post, serverConfig, 201);
    }

    public void deleteDeployment(ServerConfig serverConfig, HttpServletResponse httpResponse, String appDeploymentId) {
        HttpDelete delete = new HttpDelete(clientUtil.getServerUrl(serverConfig, clientUtil.createUriBuilder("repository/deployments/" + appDeploymentId)));
        clientUtil.execute(delete, httpResponse, serverConfig);
    }

    protected JsonNode getAppDefinitionJson(ByteArrayInputStream bais) throws IOException {

        ZipInputStream zipIS = new ZipInputStream(bais);
        ZipEntry entry;
        JsonNode appDefinitionJson = null;

        while ((entry = zipIS.getNextEntry()) != null) {
            // check if entry is app definition
            if (entry.getName() != null && entry.getName().endsWith(".app")) {

                BufferedReader reader = new BufferedReader(new InputStreamReader(zipIS));
                String line = "";
                StringBuilder js = new StringBuilder();

                while ((line = reader.readLine()) != null) {
                    js.append(line).append("\n");
                }

                String json = js.toString();
                appDefinitionJson = objectMapper.readTree(json);

                reader.close();
                break;
            }
        }

        zipIS.close();

        return appDefinitionJson;
    }

    protected String encode(String string) {
        if (string != null) {
            try {
                return URLEncoder.encode(string, "UTF-8");
            } catch (UnsupportedEncodingException uee) {
                throw new IllegalStateException("JVM does not support UTF-8 encoding.", uee);
            }
        }
        return null;
    }
}
