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

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.rest.service.BaseSpringRestTestCase;
import org.flowable.cmmn.rest.service.api.CmmnRestUrls;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * Test for all REST-operations related to single a Process Definition resource.
 *
 * @author Tijs Rademakers
 */
public class CaseDefinitionResourceTest extends BaseSpringRestTestCase {

    /**
     * Test getting a single process definition. GET cmmn-repository/case-definitions/{caseDefinitionResource}
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testGetCaseDefinition() throws Exception {

        CaseDefinition caseDefinition = repositoryService.createCaseDefinitionQuery().singleResult();

        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_DEFINITION, caseDefinition.getId()));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + " id: '" + caseDefinition.getId() + "',"
                        + " key: '" + caseDefinition.getKey() + "',"
                        + " category: '" + caseDefinition.getCategory() + "',"
                        + " version: " + caseDefinition.getVersion() + ","
                        + " description: '" + caseDefinition.getDescription() + "',"
                        + " name: '" + caseDefinition.getName() + "',"
                        + " diagramResource: null,"
                        + " deploymentId: '" + caseDefinition.getDeploymentId() + "',"
                        + " deploymentUrl: '" + SERVER_URL_PREFIX + CmmnRestUrls
                        .createRelativeResourceUrl(CmmnRestUrls.URL_DEPLOYMENT, caseDefinition.getDeploymentId()) + "',"
                        + " url: '" + httpGet.getURI().toString() + "'"
                        + "}");

        assertThat(responseNode.get("graphicalNotationDefined").booleanValue()).isFalse();

        // Check URL's
        assertThat(URLDecoder.decode(responseNode.get("resource").textValue(), "UTF-8")).endsWith(CmmnRestUrls
                .createRelativeResourceUrl(CmmnRestUrls.URL_DEPLOYMENT_RESOURCE, caseDefinition.getDeploymentId(), caseDefinition.getResourceName()));
    }

    /**
     * Test getting a single process definition with a graphical notation defined. GET cmmn-repository/case-definitions/{caseDefinitionResource}
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/repeatingStage.cmmn" })
    public void testGetCaseDefinitionWithGraphicalNotation() throws Exception {

        CaseDefinition caseDefinition = repositoryService.createCaseDefinitionQuery().singleResult();

        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_DEFINITION, caseDefinition.getId()));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + " id: '" + caseDefinition.getId() + "',"
                        + " key: '" + caseDefinition.getKey() + "',"
                        + " category: '" + caseDefinition.getCategory() + "',"
                        + " version: " + caseDefinition.getVersion() + ","
                        + " description: null,"
                        + " name: '" + caseDefinition.getName() + "',"
                        + " deploymentId: '" + caseDefinition.getDeploymentId() + "',"
                        + " graphicalNotationDefined: true,"
                        + " deploymentUrl: '" + SERVER_URL_PREFIX + CmmnRestUrls
                        .createRelativeResourceUrl(CmmnRestUrls.URL_DEPLOYMENT, caseDefinition.getDeploymentId()) + "',"
                        + " url: '" + httpGet.getURI().toString() + "',"
                        + " resource: '" + SERVER_URL_PREFIX + CmmnRestUrls
                        .createRelativeResourceUrl(CmmnRestUrls.URL_DEPLOYMENT_RESOURCE, caseDefinition.getDeploymentId(), caseDefinition.getResourceName())
                        + "',"
                        + " diagramResource: '" + SERVER_URL_PREFIX + CmmnRestUrls
                        .createRelativeResourceUrl(CmmnRestUrls.URL_DEPLOYMENT_RESOURCE, caseDefinition.getDeploymentId(),
                                caseDefinition.getDiagramResourceName()) + "'"
                        + "}");
    }

    /**
     * Test getting an unexisting case-definition. GET repository/case-definitions/{caseDefinitionId}
     */
    public void testGetUnexistingCaseDefinition() throws Exception {
        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_DEFINITION, "unexisting"));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }

    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testGetProcessDefinitionResourceData() throws Exception {
        CaseDefinition caseDefinition = repositoryService.createCaseDefinitionQuery().singleResult();

        HttpGet httpGet = new HttpGet(
                SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_DEFINITION_RESOURCE_CONTENT, caseDefinition.getId()));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);

        // Check "OK" status
        String content = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
        closeResponse(response);
        assertThat(content).contains("This is a test documentation");
    }

    /**
     * Test getting resource content for an unexisting case definition .
     */
    public void testGetResourceContentForUnexistingCaseDefinition() throws Exception {
        HttpGet httpGet = new HttpGet(
                SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_DEFINITION_RESOURCE_CONTENT, "unexisting"));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }

    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testUpdateProcessDefinitionCategory() throws Exception {
        CaseDefinition caseDefinition = repositoryService.createCaseDefinitionQuery().singleResult();
        assertThat(repositoryService.createCaseDefinitionQuery().caseDefinitionCategory("http://flowable.org/cmmn").count()).isEqualTo(1);

        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("category", "updatedcategory");

        HttpPut httpPut = new HttpPut(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_DEFINITION, caseDefinition.getId()));
        httpPut.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPut, HttpStatus.SC_OK);

        // Check "OK" status
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode.get("category").textValue()).isEqualTo("updatedcategory");

        // Check actual entry in DB
        assertThat(repositoryService.createCaseDefinitionQuery().caseDefinitionCategory("updatedcategory").count()).isEqualTo(1);

    }

}
