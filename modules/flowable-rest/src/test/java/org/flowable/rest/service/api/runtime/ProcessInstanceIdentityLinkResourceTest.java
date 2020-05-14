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

package org.flowable.rest.service.api.runtime;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.api.RestUrls;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * Test for all REST-operations related to a identity links on a Process instance resource.
 *
 * @author Frederik Heremans
 */
public class ProcessInstanceIdentityLinkResourceTest extends BaseSpringRestTestCase {

    /**
     * Test getting all identity links.
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ProcessInstanceIdentityLinkResourceTest.process.bpmn20.xml" })
    public void testGetIdentityLinks() throws Exception {

        // Test candidate user/groups links + manual added identityLink
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        runtimeService.addUserIdentityLink(processInstance.getId(), "john", "customType");
        runtimeService.addUserIdentityLink(processInstance.getId(), "paul", "candidate");

        // Execute the request
        CloseableHttpResponse response = executeRequest(
                new HttpGet(SERVER_URL_PREFIX + RestUrls
                        .createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_IDENTITYLINKS_COLLECTION, processInstance.getId())), HttpStatus.SC_OK);

        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .when(Option.IGNORING_ARRAY_ORDER)
                .isEqualTo("[ {"
                        + "    url: '" + SERVER_URL_PREFIX + RestUrls
                        .createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_IDENTITYLINK, processInstance.getId(), "john", "customType") + "',"
                        + "    user: 'john',"
                        + "    group: null,"
                        + "    type: 'customType'"
                        + "}, {"
                        + "    url: '" + SERVER_URL_PREFIX + RestUrls
                        .createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_IDENTITYLINK, processInstance.getId(), "paul", "candidate") + "',"
                        + "    user: 'paul',"
                        + "    group: null,"
                        + "    type: 'candidate'"
                        + "} ]");
    }

    /**
     * Test creating an identity link.
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ProcessInstanceIdentityLinkResourceTest.process.bpmn20.xml" })
    public void testCreateIdentityLink() throws Exception {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        // Add user link
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("user", "kermit");
        requestNode.put("type", "myType");

        HttpPost httpPost = new HttpPost(
                SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_IDENTITYLINKS_COLLECTION, processInstance.getId()));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_CREATED);

        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .isEqualTo("{"
                        + "url: '" + SERVER_URL_PREFIX + RestUrls
                        .createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_IDENTITYLINK, processInstance.getId(), "kermit", "myType") + "',"
                        + "user: 'kermit',"
                        + "type: 'myType',"
                        + "group: null"
                        + "}");

        // Test with unexisting process
        httpPost = new HttpPost(
                SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_IDENTITYLINKS_COLLECTION, "unexistingprocess"));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_NOT_FOUND));

        // Test with no user
        requestNode = objectMapper.createObjectNode();
        requestNode.put("type", "myType");

        httpPost = new HttpPost(
                SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_IDENTITYLINKS_COLLECTION, processInstance.getId()));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_BAD_REQUEST));

        // Test with group (which is not supported on processes)
        requestNode = objectMapper.createObjectNode();
        requestNode.put("type", "myType");
        requestNode.put("group", "sales");
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_BAD_REQUEST));

        // Test with no type
        requestNode = objectMapper.createObjectNode();
        requestNode.put("user", "kermit");
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_BAD_REQUEST));
    }

    /**
     * Test getting a single identity link for a process instance.
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ProcessInstanceIdentityLinkResourceTest.process.bpmn20.xml" })
    public void testGetSingleIdentityLink() throws Exception {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        runtimeService.addUserIdentityLink(processInstance.getId(), "kermit", "myType");

        CloseableHttpResponse response = executeRequest(
                new HttpGet(SERVER_URL_PREFIX + RestUrls
                        .createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_IDENTITYLINK, processInstance.getId(), "kermit", "myType")), HttpStatus.SC_OK);

        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .isEqualTo("{"
                        + "url: '" + SERVER_URL_PREFIX + RestUrls
                        .createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_IDENTITYLINK, processInstance.getId(), "kermit", "myType") + "',"
                        + "user: 'kermit',"
                        + "type: 'myType',"
                        + "group: null"
                        + "}");

        // Test with unexisting process
        closeResponse(executeRequest(
                new HttpGet(SERVER_URL_PREFIX + RestUrls
                        .createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_IDENTITYLINK, RestUrls.SEGMENT_IDENTITYLINKS_FAMILY_USERS, "kermit",
                                "myType")),
                HttpStatus.SC_NOT_FOUND));
    }

    /**
     * Test deleting a single identity link for a process instance.
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/runtime/ProcessInstanceIdentityLinkResourceTest.process.bpmn20.xml" })
    public void testDeleteSingleIdentityLink() throws Exception {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        runtimeService.addUserIdentityLink(processInstance.getId(), "kermit", "myType");

        closeResponse(executeRequest(new HttpDelete(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_IDENTITYLINK, processInstance.getId(), "kermit", "myType")),
                HttpStatus.SC_NO_CONTENT));

        // Test with unexisting process identity link
        closeResponse(executeRequest(new HttpDelete(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_IDENTITYLINK, processInstance.getId(), "kermit", "myType")),
                HttpStatus.SC_NOT_FOUND));

        // Test with unexisting process
        closeResponse(executeRequest(
                new HttpDelete(SERVER_URL_PREFIX
                        + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_IDENTITYLINK, "unexistingprocess", RestUrls.SEGMENT_IDENTITYLINKS_FAMILY_USERS, "kermit", "myType")),
                HttpStatus.SC_NOT_FOUND));
    }
}
