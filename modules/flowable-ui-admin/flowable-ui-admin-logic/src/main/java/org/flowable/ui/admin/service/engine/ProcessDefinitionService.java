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
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
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
public class ProcessDefinitionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessDefinitionService.class);

    @Autowired
    protected FlowableClientService clientUtil;

    @Autowired
    protected ObjectMapper objectMapper;

    public JsonNode listProcesDefinitions(ServerConfig serverConfig,
            Map<String, String[]> parameterMap, boolean latest) {

        URIBuilder builder = null;
        try {
            builder = new URIBuilder("repository/process-definitions");
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

    public JsonNode getProcessDefinition(ServerConfig serverConfig, String definitionId) {
        HttpGet get = new HttpGet(clientUtil.getServerUrl(serverConfig, "repository/process-definitions/" + definitionId));
        return clientUtil.executeRequest(get, serverConfig);
    }

    public JsonNode updateProcessDefinitionCategory(ServerConfig serverConfig, String definitionId, String category) {
        ObjectNode updateCall = objectMapper.createObjectNode();
        updateCall.put("category", category);

        URIBuilder builder = clientUtil.createUriBuilder("repository/process-definitions/" + definitionId);

        HttpPut put = clientUtil.createPut(builder, serverConfig);
        put.setEntity(clientUtil.createStringEntity(updateCall));

        return clientUtil.executeRequest(put, serverConfig);
    }

    public BpmnModel getProcessDefinitionModel(ServerConfig serverConfig, String definitionId) {
        HttpGet get = new HttpGet(clientUtil.getServerUrl(serverConfig, "repository/process-definitions/" + definitionId + "/resourcedata"));
        return executeRequestForXML(get, serverConfig, HttpStatus.SC_OK);
    }

    protected BpmnModel executeRequestForXML(HttpUriRequest request, ServerConfig serverConfig, int expectedStatusCode) {

        FlowableServiceException exception = null;
        CloseableHttpClient client = clientUtil.getHttpClient(serverConfig);
        try {
            try (CloseableHttpResponse response = client.execute(request)) {
                InputStream responseContent = response.getEntity().getContent();
                XMLInputFactory xif = XMLInputFactory.newInstance();
                InputStreamReader in = new InputStreamReader(responseContent, "UTF-8");
                XMLStreamReader xtr = xif.createXMLStreamReader(in);
                BpmnModel bpmnModel = new BpmnXMLConverter().convertToBpmnModel(xtr);

                boolean success = response.getStatusLine() != null && response.getStatusLine().getStatusCode() == expectedStatusCode;

                if (success) {
                    return bpmnModel;
                } else {
                    exception = new FlowableServiceException("An error occurred while calling Flowable: " + response.getStatusLine());
                }
            } catch (Exception e) {
                LOGGER.warn("Error consuming response from uri {}", request.getURI(), e);
                exception = clientUtil.wrapException(e, request);
            }
        } catch (Exception e) {
            LOGGER.error("Error executing request to uri {}", request.getURI(), e);
            exception = clientUtil.wrapException(e, request);

        } finally {
            try {
                client.close();
            } catch (Exception e) {
                LOGGER.warn("Error closing http client instance", e);
            }
        }

        if (exception != null) {
            throw exception;
        }

        return null;
    }

}
