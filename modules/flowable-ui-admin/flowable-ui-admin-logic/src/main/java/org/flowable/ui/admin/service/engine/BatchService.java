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

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
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
public class BatchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchService.class);

    @Autowired
    protected FlowableClientService clientUtil;

    public JsonNode listBatches(ServerConfig serverConfig, Map<String, String[]> parameterMap) {
        URIBuilder builder = null;
        try {
            builder = new URIBuilder("management/batches");
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

    public JsonNode getBatch(ServerConfig serverConfig, String batchId) {
        HttpGet get = new HttpGet(clientUtil.getServerUrl(serverConfig, "management/batches/" + batchId));
        return clientUtil.executeRequest(get, serverConfig);
    }

    public String getBatchDocument(ServerConfig serverConfig, String batchId) {
        HttpGet get = new HttpGet(clientUtil.getServerUrl(serverConfig, "management/batches/" + batchId + "/batch-document"));
        return clientUtil.executeRequestAsString(get, serverConfig, HttpStatus.SC_OK);
    }

    public void deleteBatch(ServerConfig serverConfig, String batchId) {
        HttpDelete post = new HttpDelete(clientUtil.getServerUrl(serverConfig, "management/batches/" + batchId));
        clientUtil.executeRequestNoResponseBody(post, serverConfig, HttpStatus.SC_NO_CONTENT);
    }
    
    public JsonNode listBatchParts(ServerConfig serverConfig, String batchId, Map<String, String[]> parameterMap) {
        URIBuilder builder = null;
        try {
            builder = new URIBuilder("management/batches/" + batchId + "/batch-parts");
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
    
    public JsonNode getBatchPart(ServerConfig serverConfig, String batchPartId) {
        HttpGet get = new HttpGet(clientUtil.getServerUrl(serverConfig, "management/batch-parts/" + batchPartId));
        return clientUtil.executeRequest(get, serverConfig);
    }
    
    public String getBatchPartDocument(ServerConfig serverConfig, String batchPartId) {
        HttpGet get = new HttpGet(clientUtil.getServerUrl(serverConfig, "management/batch-parts/" + batchPartId + "/batch-part-document"));
        return clientUtil.executeRequestAsString(get, serverConfig, HttpStatus.SC_OK);
    }
}
