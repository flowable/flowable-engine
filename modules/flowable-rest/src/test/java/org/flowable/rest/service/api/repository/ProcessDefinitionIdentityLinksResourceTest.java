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
package org.flowable.rest.service.api.repository;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.test.Deployment;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.api.RestUrls;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * Test for all REST-operations related to single a Process Definition resource.
 * 
 * @author Frederik Heremans
 */
public class ProcessDefinitionIdentityLinksResourceTest extends BaseSpringRestTestCase {

    /**
     * Test getting identitylinks for a process definition.
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/repository/oneTaskProcess.bpmn20.xml" })
    public void testGetIdentityLinksForProcessDefinition() throws Exception {

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
        repositoryService.addCandidateStarterGroup(processDefinition.getId(), "admin");
        repositoryService.addCandidateStarterUser(processDefinition.getId(), "kermit");

        HttpGet httpGet = new HttpGet(
                SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_DEFINITION_IDENTITYLINKS_COLLECTION, processDefinition.getId()));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .when(Option.IGNORING_ARRAY_ORDER)
                .isEqualTo("["
                        + " {"
                        + "   url: '" + SERVER_URL_PREFIX + RestUrls
                        .createRelativeResourceUrl(RestUrls.URL_PROCESS_DEFINITION_IDENTITYLINK, processDefinition.getId(), "groups", "admin") + "',"
                        + "   user: null,"
                        + "   group: 'admin',"
                        + "   type: 'candidate'"
                        + " },"
                        + " {"
                        + "   url: '" + SERVER_URL_PREFIX + RestUrls
                        .createRelativeResourceUrl(RestUrls.URL_PROCESS_DEFINITION_IDENTITYLINK, processDefinition.getId(), "users", "kermit") + "',"
                        + "   user: 'kermit',"
                        + "   group: null,"
                        + "   type: 'candidate'"
                        + " }"
                        + "]");
    }

    @Test
    public void testGetIdentityLinksForUnexistingProcessDefinition() throws Exception {
        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_DEFINITION_IDENTITYLINKS_COLLECTION, "unexisting"));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }
    
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/repository/oneTaskProcess.bpmn20.xml" })
    public void testAddCandidateStarterToProcessDefinition() throws Exception {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

        // Create user candidate
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("user", "kermit");

        HttpPost httpPost = new HttpPost(
                SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_DEFINITION_IDENTITYLINKS_COLLECTION, processDefinition.getId()));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_CREATED);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .isEqualTo("{"
                        + "   url: '" + SERVER_URL_PREFIX + RestUrls
                        .createRelativeResourceUrl(RestUrls.URL_PROCESS_DEFINITION_IDENTITYLINK, processDefinition.getId(), "users", "kermit") + "',"
                        + "   user: 'kermit',"
                        + "   group: null,"
                        + "   type: 'candidate'"
                        + " }");

        List<IdentityLink> createdLinks = repositoryService.getIdentityLinksForProcessDefinition(processDefinition.getId());
        assertThat(createdLinks)
                .extracting(IdentityLink::getUserId, IdentityLink::getType)
                .containsExactly(tuple("kermit", "candidate"));
        repositoryService.deleteCandidateStarterUser(processDefinition.getId(), "kermit");

        // Create group candidate
        requestNode = objectMapper.createObjectNode();
        requestNode.put("group", "admin");

        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_CREATED);
        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .isEqualTo("{"
                        + "   url: '" + SERVER_URL_PREFIX + RestUrls
                        .createRelativeResourceUrl(RestUrls.URL_PROCESS_DEFINITION_IDENTITYLINK, processDefinition.getId(), "groups", "admin") + "',"
                        + "   user: null,"
                        + "   group: 'admin',"
                        + "   type: 'candidate'"
                        + "}");

        createdLinks = repositoryService.getIdentityLinksForProcessDefinition(processDefinition.getId());
        assertThat(createdLinks)
                .extracting(IdentityLink::getGroupId, IdentityLink::getType)
                .containsExactly(tuple("admin", "candidate"));
        repositoryService.deleteCandidateStarterUser(processDefinition.getId(), "admin");
    }

    @Test
    public void testAddCandidateStarterToUnexistingProcessDefinition() throws Exception {
        // Create user candidate
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("user", "kermit");

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_DEFINITION_IDENTITYLINKS_COLLECTION, "unexisting"));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }

    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/repository/oneTaskProcess.bpmn20.xml" })
    public void testGetCandidateStarterFromProcessDefinition() throws Exception {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
        repositoryService.addCandidateStarterGroup(processDefinition.getId(), "admin");
        repositoryService.addCandidateStarterUser(processDefinition.getId(), "kermit");

        // Get user candidate
        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + RestUrls
                .createRelativeResourceUrl(RestUrls.URL_PROCESS_DEFINITION_IDENTITYLINK, processDefinition.getId(), "users", "kermit"));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .isEqualTo("{"
                        + "   url: '" + SERVER_URL_PREFIX + RestUrls
                        .createRelativeResourceUrl(RestUrls.URL_PROCESS_DEFINITION_IDENTITYLINK, processDefinition.getId(), "users", "kermit") + "',"
                        + "   user: 'kermit',"
                        + "   group: null,"
                        + "   type: 'candidate'"
                        + " }");

        // Get group candidate
        httpGet = new HttpGet(SERVER_URL_PREFIX + RestUrls
                .createRelativeResourceUrl(RestUrls.URL_PROCESS_DEFINITION_IDENTITYLINK, processDefinition.getId(), "groups", "admin"));
        response = executeRequest(httpGet, HttpStatus.SC_OK);
        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .isEqualTo("{"
                        + "   url: '" + SERVER_URL_PREFIX + RestUrls
                        .createRelativeResourceUrl(RestUrls.URL_PROCESS_DEFINITION_IDENTITYLINK, processDefinition.getId(), "groups", "admin") + "',"
                        + "   user: null,"
                        + "   group: 'admin',"
                        + "   type: 'candidate'"
                        + "}");
    }

    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/repository/oneTaskProcess.bpmn20.xml" })
    public void testDeleteCandidateStarterFromProcessDefinition() throws Exception {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
        repositoryService.addCandidateStarterGroup(processDefinition.getId(), "admin");
        repositoryService.addCandidateStarterUser(processDefinition.getId(), "kermit");

        // Delete user candidate
        HttpDelete httpDelete = new HttpDelete(SERVER_URL_PREFIX + RestUrls
                .createRelativeResourceUrl(RestUrls.URL_PROCESS_DEFINITION_IDENTITYLINK, processDefinition.getId(), "users", "kermit"));
        CloseableHttpResponse response = executeRequest(httpDelete, HttpStatus.SC_NO_CONTENT);
        closeResponse(response);

        // Check if group-link remains
        List<IdentityLink> remainingLinks = repositoryService.getIdentityLinksForProcessDefinition(processDefinition.getId());
        assertThat(remainingLinks)
                .extracting(IdentityLink::getGroupId)
                .containsExactly("admin");

        // Delete group candidate
        httpDelete = new HttpDelete(SERVER_URL_PREFIX + RestUrls
                .createRelativeResourceUrl(RestUrls.URL_PROCESS_DEFINITION_IDENTITYLINK, processDefinition.getId(), "groups", "admin"));
        response = executeRequest(httpDelete, HttpStatus.SC_NO_CONTENT);
        closeResponse(response);

        // Check if all links are removed
        remainingLinks = repositoryService.getIdentityLinksForProcessDefinition(processDefinition.getId());
        assertThat(remainingLinks).isEmpty();
    }

    @Test
    public void testDeleteCandidateStarterFromUnexistingProcessDefinition() throws Exception {
        HttpDelete httpDelete = new HttpDelete(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_DEFINITION_IDENTITYLINK, "unexisting", "groups", "admin"));
        CloseableHttpResponse response = executeRequest(httpDelete, HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }

    @Test
    public void testGetCandidateStarterFromUnexistingProcessDefinition() throws Exception {
        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_DEFINITION_IDENTITYLINK, "unexisting", "groups", "admin"));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }
}
