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
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.flowable.ui.admin.domain.ServerConfig;
import org.flowable.ui.admin.service.engine.exception.FlowableServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for invoking Flowable event subscription REST services.
 */
@Service
public class EventSubscriptionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventSubscriptionService.class);

    @Autowired
    protected FlowableClientService clientUtil;

    public JsonNode listEventSubscriptions(ServerConfig serverConfig, Map<String, String[]> parameterMap) {

        URIBuilder builder = null;
        try {
            builder = new URIBuilder("runtime/event-subscriptions");
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

    public JsonNode getEventSubscription(ServerConfig serverConfig, String eventSubscriptionId) {
        HttpGet get = new HttpGet(clientUtil.getServerUrl(serverConfig, "runtime/event-subscriptions/" + eventSubscriptionId));
        return clientUtil.executeRequest(get, serverConfig);
    }

    public void triggerExecutionEvent(ServerConfig serverConfig, String eventType, String eventName, String executionId) {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        if ("message".equals(eventType)) {
            node.put("action", "messageEventReceived");
            node.put("messageName", eventName);

        } else if ("signal".equals(eventType)) {
            node.put("action", "signalEventReceived");
            node.put("signalName", eventName);

        } else {
            throw new FlowableServiceException("Unsupported event type " + eventType);
        }

        HttpPut put = clientUtil.createPut("runtime/executions/" + executionId, serverConfig);
        put.setEntity(clientUtil.createStringEntity(node));

        clientUtil.executeRequest(put, serverConfig);
    }

    public void triggerMessageEvent(ServerConfig serverConfig, String eventName, String tenantId) {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put("message", eventName);
        if (tenantId != null && tenantId.length() > 0) {
            node.put("tenantId", tenantId);
        }

        HttpPost post = clientUtil.createPost("runtime/process-instances", serverConfig);
        post.setEntity(clientUtil.createStringEntity(node));

        clientUtil.executeRequest(post, serverConfig);
    }

    public void triggerSignalEvent(ServerConfig serverConfig, String eventName) {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put("action", "signalEventReceived");
        node.put("signalName", eventName);

        HttpPut put = clientUtil.createPut("runtime/executions", serverConfig);
        put.setEntity(clientUtil.createStringEntity(node));

        clientUtil.executeRequest(put, serverConfig);
    }
}
