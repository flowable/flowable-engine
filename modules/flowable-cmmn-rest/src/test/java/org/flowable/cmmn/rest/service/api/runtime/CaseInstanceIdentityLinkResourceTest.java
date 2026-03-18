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

package org.flowable.cmmn.rest.service.api.runtime;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.rest.service.BaseSpringRestTestCase;
import org.flowable.cmmn.rest.service.api.CmmnRestUrls;
import org.junit.jupiter.api.Test;

import net.javacrumbs.jsonunit.core.Option;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Test for all REST-operations related to a identity links on a Process instance resource.
 *
 * @author Frederik Heremans
 */
public class CaseInstanceIdentityLinkResourceTest extends BaseSpringRestTestCase {

    /**
     * Test getting all identity links.
     */
    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testGetIdentityLinks() throws Exception {

        // Test candidate user/groups links + manual added identityLink
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        runtimeService.addUserIdentityLink(caseInstance.getId(), "john", "customType");
        runtimeService.addUserIdentityLink(caseInstance.getId(), "paul", "candidate");

        // Execute the request
        CloseableHttpResponse response = executeRequest(
                new HttpGet(SERVER_URL_PREFIX + CmmnRestUrls
                        .createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_IDENTITYLINKS_COLLECTION, caseInstance.getId())), HttpStatus.SC_OK);

        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThat(responseNode.isArray()).isTrue();

        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_ARRAY_ORDER)
                .isEqualTo("["
                        + " {"
                        + " user: 'johnDoe',"
                        + " type: 'participant',"
                        + " group: null,"
                        + " url: '" + SERVER_URL_PREFIX + CmmnRestUrls
                        .createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_IDENTITYLINK, caseInstance.getId(), CmmnRestUrls.SEGMENT_IDENTITYLINKS_FAMILY_USERS, "johnDoe", "participant") + "'"
                        + " },"
                        + " {"
                        + " user: 'john',"
                        + " type: 'customType',"
                        + " group: null,"
                        + " url: '" + SERVER_URL_PREFIX + CmmnRestUrls
                        .createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_IDENTITYLINK, caseInstance.getId(), CmmnRestUrls.SEGMENT_IDENTITYLINKS_FAMILY_USERS, "john", "customType") + "'"
                        + " },"
                        + " {"
                        + " user: 'paul',"
                        + " type: 'candidate',"
                        + " group: null,"
                        + " url: '" + SERVER_URL_PREFIX + CmmnRestUrls
                        .createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_IDENTITYLINK, caseInstance.getId(), CmmnRestUrls.SEGMENT_IDENTITYLINKS_FAMILY_USERS, "paul", "candidate") + "'"
                        + " }"
                        + "]");
    }
    
    /**
     * Test creating an identity link.
     */
    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testCreateIdentityLink() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();

        // Add user link
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("user", "kermit");
        requestNode.put("type", "myType");

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + CmmnRestUrls
                .createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_IDENTITYLINKS_COLLECTION, caseInstance.getId()));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_CREATED);

        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .isEqualTo("{"
                        + "url: '" + SERVER_URL_PREFIX + CmmnRestUrls
                        .createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_IDENTITYLINK, caseInstance.getId(), CmmnRestUrls.SEGMENT_IDENTITYLINKS_FAMILY_USERS, "kermit", "myType") + "',"
                        + "user: 'kermit',"
                        + "type: 'myType',"
                        + "group: null"
                        + "}");

        // Test with unexisting process
        httpPost = new HttpPost(
                SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_IDENTITYLINKS_COLLECTION, "unexistingcase"));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_NOT_FOUND));

        // Test with no user
        requestNode = objectMapper.createObjectNode();
        requestNode.put("type", "myType");

        httpPost = new HttpPost(
                SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_IDENTITYLINKS_COLLECTION, caseInstance.getId()));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_BAD_REQUEST));

        // Test with group
        requestNode = objectMapper.createObjectNode();
        requestNode.put("type", "myType");
        requestNode.put("group", "sales");
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_CREATED);
        
        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .isEqualTo("{"
                        + "url: '" + SERVER_URL_PREFIX + CmmnRestUrls
                        .createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_IDENTITYLINK, caseInstance.getId(), CmmnRestUrls.SEGMENT_IDENTITYLINKS_FAMILY_GROUPS, "sales", "myType") + "',"
                        + "user: null,"
                        + "type: 'myType',"
                        + "group: 'sales'"
                        + "}");

        // Test with no type
        requestNode = objectMapper.createObjectNode();
        requestNode.put("user", "kermit");
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_BAD_REQUEST));
    }

    /**
     * Test getting a single identity link for a case instance.
     */
    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testGetSingleIdentityLink() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        runtimeService.addUserIdentityLink(caseInstance.getId(), "kermit", "myType");
        runtimeService.addGroupIdentityLink(caseInstance.getId(), "users", "someType");

        CloseableHttpResponse response = executeRequest(
                new HttpGet(SERVER_URL_PREFIX + CmmnRestUrls
                        .createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_IDENTITYLINK, caseInstance.getId(), CmmnRestUrls.SEGMENT_IDENTITYLINKS_FAMILY_USERS, "kermit", "myType")), HttpStatus.SC_OK);

        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .isEqualTo("{"
                        + "url: '" + SERVER_URL_PREFIX + CmmnRestUrls
                        .createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_IDENTITYLINK, caseInstance.getId(), CmmnRestUrls.SEGMENT_IDENTITYLINKS_FAMILY_USERS, "kermit", "myType") + "',"
                        + "user: 'kermit',"
                        + "type: 'myType',"
                        + "group: null"
                        + "}");

        // Test with unexisting process
        closeResponse(executeRequest(
                new HttpGet(SERVER_URL_PREFIX + CmmnRestUrls
                        .createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_IDENTITYLINK, "unexisting", CmmnRestUrls.SEGMENT_IDENTITYLINKS_FAMILY_USERS, "kermit", "myType")),
                HttpStatus.SC_NOT_FOUND));
        
        response = executeRequest(
                new HttpGet(SERVER_URL_PREFIX + CmmnRestUrls
                        .createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_IDENTITYLINK, caseInstance.getId(), CmmnRestUrls.SEGMENT_IDENTITYLINKS_FAMILY_GROUPS, "users", "someType")), HttpStatus.SC_OK);

        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .isEqualTo("{"
                        + "url: '" + SERVER_URL_PREFIX + CmmnRestUrls
                        .createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_IDENTITYLINK, caseInstance.getId(), CmmnRestUrls.SEGMENT_IDENTITYLINKS_FAMILY_GROUPS, "users", "someType") + "',"
                        + "user: null,"
                        + "type: 'someType',"
                        + "group: 'users'"
                        + "}");

        // Test with unexisting process
        closeResponse(executeRequest(
                new HttpGet(SERVER_URL_PREFIX + CmmnRestUrls
                        .createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_IDENTITYLINK, "unexisting", CmmnRestUrls.SEGMENT_IDENTITYLINKS_FAMILY_GROUPS, "kermit", "someType")),
                HttpStatus.SC_NOT_FOUND));
    }

    /**
     * Test deleting a single identity link for a case instance.
     */
    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testDeleteSingleIdentityLink() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        runtimeService.addUserIdentityLink(caseInstance.getId(), "kermit", "myType");
        runtimeService.addGroupIdentityLink(caseInstance.getId(), "users", "someType");

        closeResponse(executeRequest(new HttpDelete(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_IDENTITYLINK, caseInstance.getId(), CmmnRestUrls.SEGMENT_IDENTITYLINKS_FAMILY_USERS, "kermit", "myType")),
                HttpStatus.SC_NO_CONTENT));

        // Test with unexisting case identity link
        closeResponse(executeRequest(new HttpDelete(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_IDENTITYLINK, caseInstance.getId(), CmmnRestUrls.SEGMENT_IDENTITYLINKS_FAMILY_USERS, "kermit", "myType")),
                HttpStatus.SC_NOT_FOUND));

        // Test with unexisting case
        closeResponse(executeRequest(
                new HttpDelete(SERVER_URL_PREFIX
                        + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_IDENTITYLINK, "unexistingcase", CmmnRestUrls.SEGMENT_IDENTITYLINKS_FAMILY_USERS, "kermit", "myType")),
                HttpStatus.SC_NOT_FOUND));
        
        closeResponse(executeRequest(new HttpDelete(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_IDENTITYLINK, caseInstance.getId(), CmmnRestUrls.SEGMENT_IDENTITYLINKS_FAMILY_GROUPS, "users", "someType")),
                HttpStatus.SC_NO_CONTENT));

        // Test with unexisting case identity link
        closeResponse(executeRequest(new HttpDelete(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_IDENTITYLINK, caseInstance.getId(), CmmnRestUrls.SEGMENT_IDENTITYLINKS_FAMILY_GROUPS, "users", "someType")),
                HttpStatus.SC_NOT_FOUND));

        // Test with unexisting case
        closeResponse(executeRequest(
                new HttpDelete(SERVER_URL_PREFIX
                        + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_IDENTITYLINK, "unexistingcase", CmmnRestUrls.SEGMENT_IDENTITYLINKS_FAMILY_GROUPS, "users", "someType")),
                HttpStatus.SC_NOT_FOUND));
    }
}
