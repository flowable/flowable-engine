package org.flowable.cmmn.rest.service.api.repository;

import java.net.URLDecoder;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.rest.service.BaseSpringRestTestCase;
import org.flowable.cmmn.rest.service.api.RestUrls;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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

        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_CASE_DEFINITION, caseDefinition.getId()));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertEquals(caseDefinition.getId(), responseNode.get("id").textValue());
        assertEquals(caseDefinition.getKey(), responseNode.get("key").textValue());
        assertEquals(caseDefinition.getCategory(), responseNode.get("category").textValue());
        assertEquals(caseDefinition.getVersion(), responseNode.get("version").intValue());
        assertEquals(caseDefinition.getDescription(), responseNode.get("description").textValue());
        assertEquals(caseDefinition.getName(), responseNode.get("name").textValue());
        assertFalse(responseNode.get("graphicalNotationDefined").booleanValue());

        // Check URL's
        assertEquals(httpGet.getURI().toString(), responseNode.get("url").asText());
        assertEquals(caseDefinition.getDeploymentId(), responseNode.get("deploymentId").textValue());
        assertTrue(responseNode.get("deploymentUrl").textValue().endsWith(RestUrls.createRelativeResourceUrl(RestUrls.URL_DEPLOYMENT, caseDefinition.getDeploymentId())));
        assertTrue(URLDecoder.decode(responseNode.get("resource").textValue(), "UTF-8").endsWith(
                RestUrls.createRelativeResourceUrl(RestUrls.URL_DEPLOYMENT_RESOURCE, caseDefinition.getDeploymentId(), caseDefinition.getResourceName())));
        assertTrue(responseNode.get("diagramResource").isNull());
    }

    /**
     * Test getting a single process definition with a graphical notation defined. GET cmmn-repository/case-definitions/{caseDefinitionResource}
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/repeatingStage.cmmn" })
    public void testGetCaseDefinitionWithGraphicalNotation() throws Exception {

        CaseDefinition caseDefinition = repositoryService.createCaseDefinitionQuery().singleResult();

        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_CASE_DEFINITION, caseDefinition.getId()));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertEquals(caseDefinition.getId(), responseNode.get("id").textValue());
        assertEquals(caseDefinition.getKey(), responseNode.get("key").textValue());
        assertEquals(caseDefinition.getCategory(), responseNode.get("category").textValue());
        assertEquals(caseDefinition.getVersion(), responseNode.get("version").intValue());
        assertEquals(caseDefinition.getDescription(), responseNode.get("description").textValue());
        assertEquals(caseDefinition.getName(), responseNode.get("name").textValue());
        assertTrue(responseNode.get("graphicalNotationDefined").booleanValue());

        // Check URL's
        assertEquals(httpGet.getURI().toString(), responseNode.get("url").asText());
        assertEquals(caseDefinition.getDeploymentId(), responseNode.get("deploymentId").textValue());
        assertTrue(responseNode.get("deploymentUrl").textValue().endsWith(RestUrls.createRelativeResourceUrl(RestUrls.URL_DEPLOYMENT, caseDefinition.getDeploymentId())));
        assertTrue(URLDecoder.decode(responseNode.get("resource").textValue(), "UTF-8").endsWith(
                RestUrls.createRelativeResourceUrl(RestUrls.URL_DEPLOYMENT_RESOURCE, caseDefinition.getDeploymentId(), caseDefinition.getResourceName())));
        assertTrue(URLDecoder.decode(responseNode.get("diagramResource").textValue(), "UTF-8").endsWith(
                RestUrls.createRelativeResourceUrl(RestUrls.URL_DEPLOYMENT_RESOURCE, caseDefinition.getDeploymentId(), caseDefinition.getDiagramResourceName())));
    }

    /**
     * Test getting an unexisting case-definition. GET repository/case-definitions/{caseDefinitionId}
     */
    public void testGetUnexistingCaseDefinition() throws Exception {
        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_CASE_DEFINITION, "unexisting"));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }

    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testGetProcessDefinitionResourceData() throws Exception {
        CaseDefinition caseDefinition = repositoryService.createCaseDefinitionQuery().singleResult();

        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_CASE_DEFINITION_RESOURCE_CONTENT, caseDefinition.getId()));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);

        // Check "OK" status
        String content = IOUtils.toString(response.getEntity().getContent(), "utf-8");
        closeResponse(response);
        assertNotNull(content);
        assertTrue(content.contains("This is a test documentation"));
    }

    /**
     * Test getting resource content for an unexisting case definition .
     */
    public void testGetResourceContentForUnexistingCaseDefinition() throws Exception {
        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_CASE_DEFINITION_RESOURCE_CONTENT, "unexisting"));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }

    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testUpdateProcessDefinitionCategory() throws Exception {
        CaseDefinition caseDefinition = repositoryService.createCaseDefinitionQuery().singleResult();
        assertEquals(1, repositoryService.createCaseDefinitionQuery().caseDefinitionCategory("http://flowable.org/cmmn").count());

        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("category", "updatedcategory");

        HttpPut httpPut = new HttpPut(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_CASE_DEFINITION, caseDefinition.getId()));
        httpPut.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPut, HttpStatus.SC_OK);

        // Check "OK" status
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertEquals("updatedcategory", responseNode.get("category").textValue());

        // Check actual entry in DB
        assertEquals(1, repositoryService.createCaseDefinitionQuery().caseDefinitionCategory("updatedcategory").count());

    }

}
