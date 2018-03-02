package org.flowable.cmmn.rest.service.api.repository;

import java.util.Calendar;
import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.cmmn.rest.service.BaseSpringRestTestCase;
import org.flowable.cmmn.rest.service.api.RestUrls;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Test for all REST-operations related to the Deployment collection.
 * 
 * @author Tijs Rademakers
 */
public class DeploymentCollectionResourceTest extends BaseSpringRestTestCase {

    /**
     * Test getting deployments. GET cmmn-repository/deployments
     */
    public void testGetDeployments() throws Exception {

        try {
            // Alter time to ensure different deployTimes
            Calendar yesterday = Calendar.getInstance();
            yesterday.add(Calendar.DAY_OF_MONTH, -1);
            cmmnEngineConfiguration.getClock().setCurrentTime(yesterday.getTime());

            CmmnDeployment firstDeployment = repositoryService.createDeployment().name("Deployment 1").category("DEF").addClasspathResource("org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn")
                    .deploy();

            cmmnEngineConfiguration.getClock().setCurrentTime(Calendar.getInstance().getTime());
            CmmnDeployment secondDeployment = repositoryService.createDeployment().name("Deployment 2").category("ABC")
                    .addClasspathResource("org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn").tenantId("myTenant").deploy();

            String baseUrl = RestUrls.createRelativeResourceUrl(RestUrls.URL_DEPLOYMENT_COLLECTION);
            assertResultsPresentInDataResponse(baseUrl, firstDeployment.getId(), secondDeployment.getId());

            // Check name filtering
            String url = baseUrl + "?name=" + encode("Deployment 1");
            assertResultsPresentInDataResponse(url, firstDeployment.getId());

            // Check name-like filtering
            url = baseUrl + "?nameLike=" + encode("%ment 2");
            assertResultsPresentInDataResponse(url, secondDeployment.getId());

            // Check category filtering
            url = baseUrl + "?category=DEF";
            assertResultsPresentInDataResponse(url, firstDeployment.getId());

            // Check category-not-equals filtering
            url = baseUrl + "?categoryNotEquals=DEF";
            assertResultsPresentInDataResponse(url, secondDeployment.getId());

            // Check tenantId filtering
            url = baseUrl + "?tenantId=myTenant";
            assertResultsPresentInDataResponse(url, secondDeployment.getId());

            // Check tenantId filtering
            url = baseUrl + "?tenantId=unexistingTenant";
            assertResultsPresentInDataResponse(url);

            // Check tenantId like filtering
            url = baseUrl + "?tenantIdLike=" + encode("%enant");
            assertResultsPresentInDataResponse(url, secondDeployment.getId());

            // Check without tenantId filtering
            url = baseUrl + "?withoutTenantId=true";
            assertResultsPresentInDataResponse(url, firstDeployment.getId());

            // Check ordering by name
            CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_DEPLOYMENT_COLLECTION) + "?sort=name&order=asc"),
                    HttpStatus.SC_OK);
            JsonNode dataNode = objectMapper.readTree(response.getEntity().getContent()).get("data");
            closeResponse(response);
            assertEquals(2L, dataNode.size());
            assertEquals(firstDeployment.getId(), dataNode.get(0).get("id").textValue());
            assertEquals(secondDeployment.getId(), dataNode.get(1).get("id").textValue());

            // Check ordering by deploy time
            response = executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_DEPLOYMENT_COLLECTION) + "?sort=deployTime&order=asc"), HttpStatus.SC_OK);
            dataNode = objectMapper.readTree(response.getEntity().getContent()).get("data");
            closeResponse(response);
            assertEquals(2L, dataNode.size());
            assertEquals(firstDeployment.getId(), dataNode.get(0).get("id").textValue());
            assertEquals(secondDeployment.getId(), dataNode.get(1).get("id").textValue());

            // Check ordering by tenantId
            response = executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_DEPLOYMENT_COLLECTION) + "?sort=tenantId&order=desc"), HttpStatus.SC_OK);
            dataNode = objectMapper.readTree(response.getEntity().getContent()).get("data");
            closeResponse(response);
            assertEquals(2L, dataNode.size());
            assertEquals(secondDeployment.getId(), dataNode.get(0).get("id").textValue());
            assertEquals(firstDeployment.getId(), dataNode.get(1).get("id").textValue());

            // Check paging
            response = executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_DEPLOYMENT_COLLECTION) + "?sort=deployTime&order=asc&start=1&size=1"), HttpStatus.SC_OK);
            JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);
            dataNode = responseNode.get("data");
            assertEquals(1L, dataNode.size());
            assertEquals(secondDeployment.getId(), dataNode.get(0).get("id").textValue());
            assertEquals(2L, responseNode.get("total").longValue());
            assertEquals(1L, responseNode.get("start").longValue());
            assertEquals(1L, responseNode.get("size").longValue());

        } finally {
            // Always cleanup any created deployments, even if the test failed
            List<CmmnDeployment> deployments = repositoryService.createDeploymentQuery().list();
            for (CmmnDeployment deployment : deployments) {
                repositoryService.deleteDeployment(deployment.getId(), true);
            }
        }
    }
}
