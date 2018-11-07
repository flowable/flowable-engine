package org.flowable.app.rest.service.api.repository;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.message.BasicHeader;
import org.flowable.app.api.repository.AppDeployment;
import org.flowable.app.rest.AppRestUrls;
import org.flowable.app.rest.service.BaseSpringRestTestCase;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Test for all REST-operations related to a resources that is part of a deployment.
 * 
 * @author Tijs Rademakers
 */
public class DeploymentResourceResourceTest extends BaseSpringRestTestCase {

    /**
     * Test getting a single resource, deployed in a deployment. GET app-repository/deployments/{deploymentId}/resources/{resourceId}
     */
    public void testGetDeploymentResource() throws Exception {
        try {
            String rawResourceName = "org/flowable/app/rest/service/api/repository/oneApp.app";
            AppDeployment deployment = repositoryService.createDeployment().name("Deployment 1").addClasspathResource(rawResourceName)
                    .addInputStream("test.txt", new ByteArrayInputStream("Test content".getBytes())).deploy();

            // Build up the URL manually to make sure resource-id gets encoded correctly as one piece
            HttpGet httpGet = new HttpGet(buildUrl(AppRestUrls.URL_DEPLOYMENT_RESOURCE, deployment.getId(), encode(rawResourceName)));
            httpGet.addHeader(new BasicHeader(HttpHeaders.ACCEPT, "application/json"));
            CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);
            JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);

            // Check URL's for the resource
            assertEquals(responseNode.get("url").textValue(), buildUrl(AppRestUrls.URL_DEPLOYMENT_RESOURCE, deployment.getId(), rawResourceName));
            assertEquals(responseNode.get("contentUrl").textValue(), buildUrl(AppRestUrls.URL_DEPLOYMENT_RESOURCE_CONTENT, deployment.getId(), rawResourceName));
            assertEquals("application/json", responseNode.get("mediaType").textValue());
            assertEquals("appDefinition", responseNode.get("type").textValue());

        } finally {
            // Always cleanup any created deployments, even if the test failed
            List<AppDeployment> deployments = repositoryService.createDeploymentQuery().list();
            for (AppDeployment deployment : deployments) {
                repositoryService.deleteDeployment(deployment.getId(), true);
            }
        }
    }

    /**
     * Test getting a single resource for an unexisting deployment. GET app-repository/deployments/{deploymentId}/resources/{resourceId}
     */
    public void testGetDeploymentResourceUnexistingDeployment() throws Exception {
        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + AppRestUrls.createRelativeResourceUrl(AppRestUrls.URL_DEPLOYMENT_RESOURCE, "unexisting", "resource.png"));
        httpGet.addHeader(new BasicHeader(HttpHeaders.ACCEPT, "image/png,application/json"));
        closeResponse(executeRequest(httpGet, HttpStatus.SC_NOT_FOUND));
    }

    /**
     * Test getting an unexisting resource for an existing deployment. GET app-repository/deployments/{deploymentId}/resources/{resourceId}
     */
    public void testGetDeploymentResourceUnexistingResource() throws Exception {
        try {
            AppDeployment deployment = repositoryService.createDeployment().name("Deployment 1").addInputStream("test.txt", new ByteArrayInputStream("Test content".getBytes())).deploy();

            HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + AppRestUrls.createRelativeResourceUrl(AppRestUrls.URL_DEPLOYMENT_RESOURCE, deployment.getId(), "unexisting-resource.png"));
            httpGet.addHeader(new BasicHeader(HttpHeaders.ACCEPT, "image/png,application/json"));
            closeResponse(executeRequest(httpGet, HttpStatus.SC_NOT_FOUND));

        } finally {
            // Always cleanup any created deployments, even if the test failed
            List<AppDeployment> deployments = repositoryService.createDeploymentQuery().list();
            for (AppDeployment deployment : deployments) {
                repositoryService.deleteDeployment(deployment.getId(), true);
            }
        }
    }

    /**
     * Test getting a deployment resource content. GET app-repository/deployments/{deploymentId}/resources/{resourceId}
     */
    public void testGetDeploymentResourceContent() throws Exception {
        try {
            AppDeployment deployment = repositoryService.createDeployment().name("Deployment 1").addInputStream("test.txt", new ByteArrayInputStream("Test content".getBytes())).deploy();

            HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + AppRestUrls.createRelativeResourceUrl(AppRestUrls.URL_DEPLOYMENT_RESOURCE_CONTENT, deployment.getId(), "test.txt"));
            httpGet.addHeader(new BasicHeader(HttpHeaders.ACCEPT, "text/plain"));
            CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);
            String responseAsString = IOUtils.toString(response.getEntity().getContent(), "utf-8");
            closeResponse(response);
            assertNotNull(responseAsString);
            assertEquals("Test content", responseAsString);

        } finally {
            // Always cleanup any created deployments, even if the test failed
            List<AppDeployment> deployments = repositoryService.createDeploymentQuery().list();
            for (AppDeployment deployment : deployments) {
                repositoryService.deleteDeployment(deployment.getId(), true);
            }
        }
    }
}
