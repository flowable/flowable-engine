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

package org.flowable.eventregistry.rest.service.api.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.eventregistry.rest.service.BaseSpringRestTestCase;
import org.flowable.eventregistry.rest.service.api.EventRestUrls;
import org.flowable.eventregistry.test.ChannelDeploymentAnnotation;
import org.flowable.eventregistry.test.EventDeploymentAnnotation;
import org.flowable.task.api.Task;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class EventInstanceCollectionResourceTest extends BaseSpringRestTestCase {

    @Deployment(resources = { "org/flowable/eventregistry/rest/service/api/repository/boundaryEvent.bpmn20.xml" })
    @EventDeploymentAnnotation(resources = { "org/flowable/eventregistry/rest/service/api/repository/simpleEvent.event" })
    @ChannelDeploymentAnnotation(resources = { "org/flowable/eventregistry/rest/service/api/repository/simpleChannel.channel" })
    public void testSendEvent() throws Exception {
        ProcessInstance processInstance = processRuntimeService.startProcessInstanceByKey("process", Collections.singletonMap("customerIdVar", "123"));
        Task task = processTaskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("task");
        
        ObjectNode requestNode = objectMapper.createObjectNode();
        
        // first send event that doesn't match process boundary event
        requestNode.put("eventDefinitionKey", "myEvent");
        requestNode.put("channelDefinitionKey", "myChannel");
        ObjectNode payloadNode = requestNode.putObject("eventPayload");
        payloadNode.put("customerId", "notExisting");
        payloadNode.put("productNumber", "p456");

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + EventRestUrls.createRelativeResourceUrl(EventRestUrls.URL_EVENT_INSTANCE_COLLECTION));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_NO_CONTENT);
        closeResponse(response);
        
        task = processTaskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("task");

        // now send event with matching correlation value
        requestNode.put("eventDefinitionKey", "myEvent");
        payloadNode = requestNode.putObject("eventPayload");
        payloadNode.put("customerId", "123");
        payloadNode.put("productNumber", "p456");

        httpPost = new HttpPost(SERVER_URL_PREFIX + EventRestUrls.createRelativeResourceUrl(EventRestUrls.URL_EVENT_INSTANCE_COLLECTION));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_NO_CONTENT);
        closeResponse(response);
        
        task = processTaskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("taskAfterBoundary");
    }
}
