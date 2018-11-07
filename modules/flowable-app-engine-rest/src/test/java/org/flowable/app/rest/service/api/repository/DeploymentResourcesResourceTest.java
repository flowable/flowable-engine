package org.flowable.app.rest.service.api.repository;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.flowable.app.api.repository.AppDeployment;
import org.flowable.app.rest.AppRestUrls;
import org.flowable.app.rest.service.BaseSpringRestTestCase;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Test for all REST-operations related to listing the resources that are part of a deployment.
 * 
 * @author Tijs Rademakers
 */
public class DeploymentResourcesResourceTest extends BaseSpringRestTestCase {

    /**
     * Test getting all resources for a single deployment. GET app-repository/deployments/{deploymentId}/resources
     */
    public void testGetDeploymentResources() throws Exception {

        try {
            AppDeployment deployment = repositoryService.createDeployment().name("Deployment 1").addClasspathResource("org/flowable/app/rest/service/api/repository/oneApp.app")
                    .addInputStream("test.txt", new ByteArrayInputStream("Test content".getBytes())).deploy();

            HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + AppRestUrls.createRelativeResourceUrl(AppRestUrls.URL_DEPLOYMENT_RESOURCES, deployment.getId()));
            CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);
            JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);
            assertTrue(responseNode.isArray());
            assertEquals(2, responseNode.size());

            // Since resources can be returned in any arbitrary order, find the
            // right one to check
            JsonNode txtNode = null;
            for (int i = 0; i < responseNode.size(); i++) {
                if ("test.txt".equals(responseNode.get(i).get("id").textValue())) {
                    txtNode = responseNode.get(i);
                    break;
                }
            }

            // Check URL's for the resource
            assertNotNull(txtNode);
            assertTrue(txtNode.get("url").textValue().endsWith(AppRestUrls.createRelativeResourceUrl(AppRestUrls.URL_DEPLOYMENT_RESOURCE, deployment.getId(), "test.txt")));
            assertTrue(txtNode.get("contentUrl").textValue().endsWith(AppRestUrls.createRelativeResourceUrl(AppRestUrls.URL_DEPLOYMENT_RESOURCE_CONTENT, deployment.getId(), "test.txt")));
            assertTrue(txtNode.get("mediaType").isNull());
            assertEquals("resource", txtNode.get("type").textValue());

        } finally {
            // Always cleanup any created deployments, even if the test failed
            List<AppDeployment> deployments = repositoryService.createDeploymentQuery().list();
            for (AppDeployment deployment : deployments) {
                repositoryService.deleteDeployment(deployment.getId(), true);
            }
        }
    }

    /**
     * Test getting all resources for a single unexisting deployment. GET app-repository/deployments/{deploymentId}/resources
     */
    public void testGetDeploymentResourcesUnexistingDeployment() throws Exception {
        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + AppRestUrls.createRelativeResourceUrl(AppRestUrls.URL_DEPLOYMENT_RESOURCES, "unexisting"));
        closeResponse(executeRequest(httpGet, HttpStatus.SC_NOT_FOUND));
    }
}
