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
package org.flowable.cmmn.rest.service.api.repository;

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
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.rest.service.BaseSpringRestTestCase;
import org.flowable.cmmn.rest.service.api.CmmnRestUrls;
import org.flowable.identitylink.api.IdentityLink;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * Test for all REST-operations related to single a Case Definition resource.
 *
 * @author Tijs Rademakers
 */
public class CaseDefinitionIdentityLinksResourceTest extends BaseSpringRestTestCase {

    /**
     * Test getting identitylinks for a case definition.
     */
    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testGetIdentityLinksForProcessDefinition() throws Exception {

        CaseDefinition caseDefinition = repositoryService.createCaseDefinitionQuery().singleResult();
        repositoryService.addCandidateStarterGroup(caseDefinition.getId(), "admin");
        repositoryService.addCandidateStarterUser(caseDefinition.getId(), "kermit");

        HttpGet httpGet = new HttpGet(
                SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_DEFINITION_IDENTITYLINKS_COLLECTION, caseDefinition.getId()));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_ARRAY_ORDER)
                .isEqualTo("["
                        + "  {"
                        + "    user: 'kermit',"
                        + "    type: 'candidate',"
                        + "    group: null,"
                        + "    url: '" + SERVER_URL_PREFIX + CmmnRestUrls
                        .createRelativeResourceUrl(CmmnRestUrls.URL_CASE_DEFINITION_IDENTITYLINK, caseDefinition.getId(), "users", "kermit") + "'"
                        + "  },"
                        + "  {"
                        + "    group: 'admin',"
                        + "    type: 'candidate',"
                        + "    user: null,"
                        + "    url: '" + SERVER_URL_PREFIX + CmmnRestUrls
                        .createRelativeResourceUrl(CmmnRestUrls.URL_CASE_DEFINITION_IDENTITYLINK, caseDefinition.getId(), "groups", "admin") + "'"
                        + "  }"
                        + "]");
    }

    @Test
    public void testGetIdentityLinksForUnexistingCaseDefinition() throws Exception {
        HttpGet httpGet = new HttpGet(
                SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_DEFINITION_IDENTITYLINKS_COLLECTION, "unexisting"));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testAddCandidateStarterToCaseDefinition() throws Exception {
        CaseDefinition caseDefinition = repositoryService.createCaseDefinitionQuery().singleResult();

        // Create user candidate
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("user", "kermit");

        HttpPost httpPost = new HttpPost(
                SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_DEFINITION_IDENTITYLINKS_COLLECTION, caseDefinition.getId()));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_CREATED);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + " user: 'kermit',"
                        + " type: 'candidate',"
                        + " group: null,"
                        + " url: '" + SERVER_URL_PREFIX + CmmnRestUrls
                        .createRelativeResourceUrl(CmmnRestUrls.URL_CASE_DEFINITION_IDENTITYLINK, caseDefinition.getId(), "users", "kermit") + "'"
                        + "}");

        List<IdentityLink> createdLinks = repositoryService.getIdentityLinksForCaseDefinition(caseDefinition.getId());
        assertThat(createdLinks)
                .extracting(IdentityLink::getUserId, IdentityLink::getType)
                .containsExactly(tuple("kermit", "candidate"));
        repositoryService.deleteCandidateStarterUser(caseDefinition.getId(), "kermit");

        // Create group candidate
        requestNode = objectMapper.createObjectNode();
        requestNode.put("group", "admin");

        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_CREATED);
        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + " group: 'admin',"
                        + " type: 'candidate',"
                        + " user: null,"
                        + " url: '" + SERVER_URL_PREFIX + CmmnRestUrls
                        .createRelativeResourceUrl(CmmnRestUrls.URL_CASE_DEFINITION_IDENTITYLINK, caseDefinition.getId(), "groups", "admin") + "'"
                        + "}");

        createdLinks = repositoryService.getIdentityLinksForCaseDefinition(caseDefinition.getId());
        assertThat(createdLinks)
                .extracting(IdentityLink::getGroupId, IdentityLink::getType)
                .containsExactly(tuple("admin", "candidate"));
        repositoryService.deleteCandidateStarterUser(caseDefinition.getId(), "admin");
    }

    @Test
    public void testAddCandidateStarterToUnexistingCaseDefinition() throws Exception {
        // Create user candidate
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("user", "kermit");

        HttpPost httpPost = new HttpPost(
                SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_DEFINITION_IDENTITYLINKS_COLLECTION, "unexisting"));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCaseWithStarters.cmmn" })
    public void testGetCandidateStarterFromCaseDefinition() throws Exception {
        CaseDefinition caseDefinition = repositoryService.createCaseDefinitionQuery().singleResult();

        // Get user candidate
        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + CmmnRestUrls
                .createRelativeResourceUrl(CmmnRestUrls.URL_CASE_DEFINITION_IDENTITYLINK, caseDefinition.getId(), "users", "kermit"));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + " user: 'kermit',"
                        + " type: 'candidate',"
                        + " group: null,"
                        + " url: '" + SERVER_URL_PREFIX + CmmnRestUrls
                        .createRelativeResourceUrl(CmmnRestUrls.URL_CASE_DEFINITION_IDENTITYLINK, caseDefinition.getId(), "users", "kermit") + "'"
                        + "}");

        // Get group candidate
        httpGet = new HttpGet(SERVER_URL_PREFIX + CmmnRestUrls
                .createRelativeResourceUrl(CmmnRestUrls.URL_CASE_DEFINITION_IDENTITYLINK, caseDefinition.getId(), "groups", "admin"));
        response = executeRequest(httpGet, HttpStatus.SC_OK);
        responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + " group: 'admin',"
                        + " type: 'candidate',"
                        + " user: null,"
                        + " url: '" + SERVER_URL_PREFIX + CmmnRestUrls
                        .createRelativeResourceUrl(CmmnRestUrls.URL_CASE_DEFINITION_IDENTITYLINK, caseDefinition.getId(), "groups", "admin") + "'"
                        + "}");
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testDeleteCandidateStarterFromCaseDefinition() throws Exception {
        CaseDefinition caseDefinition = repositoryService.createCaseDefinitionQuery().singleResult();
        repositoryService.addCandidateStarterGroup(caseDefinition.getId(), "admin");
        repositoryService.addCandidateStarterUser(caseDefinition.getId(), "kermit");

        // Delete user candidate
        HttpDelete httpDelete = new HttpDelete(SERVER_URL_PREFIX + CmmnRestUrls
                .createRelativeResourceUrl(CmmnRestUrls.URL_CASE_DEFINITION_IDENTITYLINK, caseDefinition.getId(), "users", "kermit"));
        CloseableHttpResponse response = executeRequest(httpDelete, HttpStatus.SC_NO_CONTENT);
        closeResponse(response);

        // Check if group-link remains
        List<IdentityLink> remainingLinks = repositoryService.getIdentityLinksForCaseDefinition(caseDefinition.getId());
        assertThat(remainingLinks)
                .extracting(IdentityLink::getGroupId)
                .containsExactly("admin");

        // Delete group candidate
        httpDelete = new HttpDelete(SERVER_URL_PREFIX + CmmnRestUrls
                .createRelativeResourceUrl(CmmnRestUrls.URL_CASE_DEFINITION_IDENTITYLINK, caseDefinition.getId(), "groups", "admin"));
        response = executeRequest(httpDelete, HttpStatus.SC_NO_CONTENT);
        closeResponse(response);

        // Check if all links are removed
        remainingLinks = repositoryService.getIdentityLinksForCaseDefinition(caseDefinition.getId());
        assertThat(remainingLinks).isEmpty();
    }

    @Test
    public void testDeleteCandidateStarterFromUnexistingCaseDefinition() throws Exception {
        HttpDelete httpDelete = new HttpDelete(
                SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_DEFINITION_IDENTITYLINK, "unexisting", "groups", "admin"));
        CloseableHttpResponse response = executeRequest(httpDelete, HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }

    @Test
    public void testGetCandidateStarterFromUnexistingCaseDefinition() throws Exception {
        HttpGet httpGet = new HttpGet(
                SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_DEFINITION_IDENTITYLINK, "unexisting", "groups", "admin"));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }
}
