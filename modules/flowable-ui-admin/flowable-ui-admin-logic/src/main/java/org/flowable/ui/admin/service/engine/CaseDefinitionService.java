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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.flowable.cmmn.converter.CmmnXmlConverter;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.ui.admin.domain.ServerConfig;
import org.flowable.ui.admin.service.engine.exception.FlowableServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Service for invoking Flowable REST services.
 */
@Service
public class CaseDefinitionService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CaseDefinitionService.class);

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
    
    public CmmnModel getCaseDefinitionModel(ServerConfig serverConfig, String definitionId) {
        HttpGet get = new HttpGet(clientUtil.getServerUrl(serverConfig, "cmmn-repository/case-definitions/" + definitionId + "/resourcedata"));
        return executeRequestForXML(get, serverConfig, HttpStatus.SC_OK);
    }

    public JsonNode getCaseDefinitionForms(ServerConfig serverConfig, String caseDefinitionId) {
        HttpGet get = new HttpGet(clientUtil.getServerUrl(serverConfig, "cmmn-repository/case-definitions/" + caseDefinitionId + "/form-definitions"));
        return clientUtil.executeRequest(get, serverConfig);
    }
    
    protected CmmnModel executeRequestForXML(HttpUriRequest request, ServerConfig serverConfig, int expectedStatusCode) {

        FlowableServiceException exception = null;
        CloseableHttpClient client = clientUtil.getHttpClient(serverConfig);
        try {
            try (CloseableHttpResponse response = client.execute(request)) {
                InputStream responseContent = response.getEntity().getContent();
                XMLInputFactory xif = XMLInputFactory.newInstance();
                InputStreamReader in = new InputStreamReader(responseContent, "UTF-8");
                XMLStreamReader xtr = xif.createXMLStreamReader(in);
                CmmnModel cmmmnModel = new CmmnXmlConverter().convertToCmmnModel(xtr);

                boolean success = response.getStatusLine() != null && response.getStatusLine().getStatusCode() == expectedStatusCode;

                if (success) {
                    return cmmmnModel;
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
