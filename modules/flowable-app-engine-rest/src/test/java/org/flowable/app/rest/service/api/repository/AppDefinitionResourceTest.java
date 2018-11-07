package org.flowable.app.rest.service.api.repository;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.flowable.app.api.repository.AppDefinition;
import org.flowable.app.engine.test.AppDeployment;
import org.flowable.app.rest.AppRestUrls;
import org.flowable.app.rest.service.BaseSpringRestTestCase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Test for all REST-operations related to single a Process Definition resource.
 * 
 * @author Tijs Rademakers
 */
public class AppDefinitionResourceTest extends BaseSpringRestTestCase {

    /**
     * Test getting a single app definition. GET app-repository/app-definitions/{appeDefinitionId}
     */
    @AppDeployment(resources = { "org/flowable/app/rest/service/api/repository/oneApp.app" })
    public void testGetAppDefinition() throws Exception {

        AppDefinition appDefinition = repositoryService.createAppDefinitionQuery().singleResult();

        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + AppRestUrls.createRelativeResourceUrl(AppRestUrls.URL_APP_DEFINITION, appDefinition.getId()));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertEquals(appDefinition.getId(), responseNode.get("id").textValue());
        assertEquals(appDefinition.getKey(), responseNode.get("key").textValue());
        assertEquals(appDefinition.getCategory(), responseNode.get("category").textValue());
        assertEquals(appDefinition.getVersion(), responseNode.get("version").intValue());
        assertEquals(appDefinition.getDescription(), responseNode.get("description").textValue());
        assertEquals(appDefinition.getName(), responseNode.get("name").textValue());
        
        // Check URL's
        assertEquals(httpGet.getURI().toString(), responseNode.get("url").asText());
        assertEquals(appDefinition.getDeploymentId(), responseNode.get("deploymentId").textValue());
    }

    /**
     * Test getting an unexisting app definition. GET app-repository/app-definitions/{appeDefinitionId}
     */
    public void testGetUnexistingCaseDefinition() throws Exception {
        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + AppRestUrls.createRelativeResourceUrl(AppRestUrls.URL_APP_DEFINITION, "unexisting"));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }

    @AppDeployment(resources = { "org/flowable/app/rest/service/api/repository/oneApp.app" })
    public void testGetAppDefinitionResourceData() throws Exception {
        AppDefinition appDefinition = repositoryService.createAppDefinitionQuery().singleResult();

        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + AppRestUrls.createRelativeResourceUrl(AppRestUrls.URL_APP_DEFINITION_RESOURCE_CONTENT, appDefinition.getId()));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);

        // Check "OK" status
        String content = IOUtils.toString(response.getEntity().getContent(), "utf-8");
        closeResponse(response);
        assertNotNull(content);
        assertTrue(content.contains("oneApp"));
    }

    /**
     * Test getting resource content for an unexisting app definition .
     */
    public void testGetResourceContentForUnexistingAppDefinition() throws Exception {
        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + AppRestUrls.createRelativeResourceUrl(AppRestUrls.URL_APP_DEFINITION_RESOURCE_CONTENT, "unexisting"));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }

    @AppDeployment(resources = { "org/flowable/app/rest/service/api/repository/oneApp.app" })
    public void testUpdateAppDefinitionCategory() throws Exception {
        AppDefinition appDefinition = repositoryService.createAppDefinitionQuery().singleResult();
        assertEquals(1, repositoryService.createAppDefinitionQuery().count());

        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("category", "updatedcategory");

        HttpPut httpPut = new HttpPut(SERVER_URL_PREFIX + AppRestUrls.createRelativeResourceUrl(AppRestUrls.URL_APP_DEFINITION, appDefinition.getId()));
        httpPut.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPut, HttpStatus.SC_OK);

        // Check "OK" status
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertEquals("updatedcategory", responseNode.get("category").textValue());

        // Check actual entry in DB
        assertEquals(1, repositoryService.createAppDefinitionQuery().appDefinitionCategory("updatedcategory").count());

    }

}
