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
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.flowable.ui.admin.domain.ServerConfig;
import org.flowable.ui.admin.service.engine.exception.FlowableServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Service for invoking Flowable REST services.
 */
@Service
public class FormInstanceService {

    @Autowired
    protected FlowableClientService clientUtil;

    @Autowired
    protected ObjectMapper objectMapper;

    public JsonNode listFormInstances(ServerConfig serverConfig, Map<String, String[]> parameterMap) {
        URIBuilder builder = clientUtil.createUriBuilder("form/form-instances");

        for (String name : parameterMap.keySet()) {
            builder.addParameter(name, parameterMap.get(name)[0]);
        }
        HttpGet get = new HttpGet(clientUtil.getServerUrl(serverConfig, builder.toString()));
        return clientUtil.executeRequest(get, serverConfig);
    }

    public JsonNode getFormInstance(ServerConfig serverConfig, String formInstanceId) {
        HttpGet get = new HttpGet(clientUtil.getServerUrl(serverConfig, "form/form-instance/" + formInstanceId));
        return clientUtil.executeRequest(get, serverConfig);
    }

    public JsonNode getFormInstances(ServerConfig serverConfig, ObjectNode objectNode) {

        JsonNode resultNode = null;

        try {
            URIBuilder builder = clientUtil.createUriBuilder("query/form-instances");
            HttpPost post = clientUtil.createPost(builder.toString(), serverConfig);
            post.setEntity(clientUtil.createStringEntity(objectNode.toString()));

            resultNode = clientUtil.executeRequest(post, serverConfig);
        } catch (Exception ex) {
            throw new FlowableServiceException(ex.getMessage(), ex);
        }

        return resultNode;
    }

    public JsonNode getFormInstanceFormFieldValues(ServerConfig serverConfig, String formInstanceId) {

        ObjectNode returnNode = null;

        try {
            returnNode = objectMapper.createObjectNode();

            ObjectNode requestNode = objectMapper.createObjectNode();
            requestNode.put("formInstanceId", formInstanceId);

            URIBuilder builder = clientUtil.createUriBuilder("form/form-instance-model");
            HttpPost post = clientUtil.createPost(builder.toString(), serverConfig);
            post.setEntity(clientUtil.createStringEntity(requestNode.toString()));

            JsonNode resultNode = clientUtil.executeRequest(post, serverConfig);

            ArrayNode formFieldValues = objectMapper.createArrayNode();

            if (resultNode != null && resultNode.has("fields") && resultNode.get("fields").isArray()) {

                ArrayNode fieldsNode = (ArrayNode) resultNode.get("fields");
                for (JsonNode fieldNode : fieldsNode) {

                    ObjectNode formFieldValue = objectMapper.createObjectNode();

                    formFieldValue.set("id", fieldNode.get("id"));
                    formFieldValue.set("name", fieldNode.get("name"));
                    formFieldValue.set("type", fieldNode.get("type"));
                    formFieldValue.set("value", fieldNode.get("value"));

                    formFieldValues.add(formFieldValue);
                }
            }

            returnNode.put("size", formFieldValues.size());
            returnNode.put("total", formFieldValues.size());
            returnNode.set("data", formFieldValues);
        } catch (Exception ex) {
            throw new FlowableServiceException(ex.getMessage(), ex);
        }

        return returnNode;
    }

}
