package org.flowable.app.rest.service.api.repository;

import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.flowable.app.api.repository.AppDeployment;
import org.flowable.app.rest.AppRestUrls;
import org.flowable.app.rest.service.BaseSpringRestTestCase;
import org.flowable.app.rest.service.HttpMultipartHelper;
import org.flowable.common.engine.impl.util.ReflectUtil;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Test for all REST-operations related to a single Deployment resource.
 * 
 * @author Tijs Rademakers
 */
public class DeploymentResourceTest extends BaseSpringRestTestCase {

    /**
     * Test deploying singe app file. POST app-repository/deployments
     */
    public void testPostNewDeploymentAppFile() throws Exception {
        try {
            // Upload a valid BPMN-file using multipart-data
            HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + AppRestUrls.createRelativeResourceUrl(AppRestUrls.URL_DEPLOYMENT_COLLECTION));
            httpPost.setEntity(HttpMultipartHelper.getMultiPartEntity("oneApp.app", "application/json",
                    ReflectUtil.getResourceAsStream("org/flowable/app/rest/service/api/repository/oneApp.app"), null));
            CloseableHttpResponse response = executeBinaryRequest(httpPost, HttpStatus.SC_CREATED);

            // Check deployment
            JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);

            String deploymentId = responseNode.get("id").textValue();
            String name = responseNode.get("name").textValue();
            String category = responseNode.get("category").textValue();
            String deployTime = responseNode.get("deploymentTime").textValue();
            String url = responseNode.get("url").textValue();
            String tenantId = responseNode.get("tenantId").textValue();

            assertEquals("", tenantId);

            assertNotNull(deploymentId);
            assertEquals(1L, repositoryService.createDeploymentQuery().deploymentId(deploymentId).count());

            assertNotNull(name);
            assertEquals("oneApp", name);

            assertNotNull(url);
            assertTrue(url.endsWith(AppRestUrls.createRelativeResourceUrl(AppRestUrls.URL_DEPLOYMENT, deploymentId)));

            // No deployment-category should have been set
            assertNull(category);
            assertNotNull(deployTime);

            // Check if process is actually deployed in the deployment
            List<String> resources = repositoryService.getDeploymentResourceNames(deploymentId);
            assertEquals(1L, resources.size());
            assertEquals("oneApp.app", resources.get(0));
            assertEquals(1L, repositoryService.createAppDefinitionQuery().deploymentId(deploymentId).count());

        } finally {
            // Always cleanup any created deployments, even if the test failed
            List<AppDeployment> deployments = repositoryService.createDeploymentQuery().list();
            for (AppDeployment deployment : deployments) {
                repositoryService.deleteDeployment(deployment.getId(), true);
            }
        }
    }
    
    /**
     * Test deploying singe zip file. POST app-repository/deployments
     */
    public void testPostNewDeploymentZipFile() throws Exception {
        try {
            // Upload a valid BPMN-file using multipart-data
            HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + AppRestUrls.createRelativeResourceUrl(AppRestUrls.URL_DEPLOYMENT_COLLECTION));
            httpPost.setEntity(HttpMultipartHelper.getMultiPartEntity("vacationRequest.zip", "application/zip",
                    ReflectUtil.getResourceAsStream("org/flowable/app/rest/service/api/repository/vacationRequest.zip"), null));
            CloseableHttpResponse response = executeBinaryRequest(httpPost, HttpStatus.SC_CREATED);

            // Check deployment
            JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);

            String deploymentId = responseNode.get("id").textValue();
            String name = responseNode.get("name").textValue();
            String category = responseNode.get("category").textValue();
            String deployTime = responseNode.get("deploymentTime").textValue();
            String url = responseNode.get("url").textValue();
            String tenantId = responseNode.get("tenantId").textValue();

            assertEquals("", tenantId);

            assertNotNull(deploymentId);
            assertEquals(1L, repositoryService.createDeploymentQuery().deploymentId(deploymentId).count());

            assertNotNull(name);
            assertEquals("vacationRequest", name);

            assertNotNull(url);
            assertTrue(url.endsWith(AppRestUrls.createRelativeResourceUrl(AppRestUrls.URL_DEPLOYMENT, deploymentId)));

            // No deployment-category should have been set
            assertNull(category);
            assertNotNull(deployTime);

            // Check if process is actually deployed in the deployment
            List<String> resources = repositoryService.getDeploymentResourceNames(deploymentId);
            assertEquals(4L, resources.size());
            
            boolean vacationRequestAppFound = false;
            for (String resourceName : resources) {
                if ("vacationRequestApp.app".equals(resourceName)) {
                    vacationRequestAppFound = true;
                }
            }
            assertTrue(vacationRequestAppFound);
            
            assertEquals(1L, repositoryService.createAppDefinitionQuery().deploymentId(deploymentId).count());

        } finally {
            // Always cleanup any created deployments, even if the test failed
            List<AppDeployment> deployments = repositoryService.createDeploymentQuery().list();
            for (AppDeployment deployment : deployments) {
                repositoryService.deleteDeployment(deployment.getId(), true);
            }
        }
    }

    /**
     * Test deploying an invalid file. POST app-repository/deployments
     */
    public void testPostNewDeploymentInvalidFile() throws Exception {
        // Upload a valid App-file using multipart-data
        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + AppRestUrls.createRelativeResourceUrl(AppRestUrls.URL_DEPLOYMENT_COLLECTION));
        httpPost.setEntity(HttpMultipartHelper.getMultiPartEntity("oneApp.invalidfile", "application/json",
                ReflectUtil.getResourceAsStream("org/flowable/app/rest/service/api/repository/oneApp.app"), null));
        closeResponse(executeBinaryRequest(httpPost, HttpStatus.SC_BAD_REQUEST));
    }

    /**
     * Test getting a single deployment. GET app-repository/deployments/{deploymentId}
     */
    @org.flowable.app.engine.test.AppDeployment(resources = { "org/flowable/app/rest/service/api/repository/oneApp.app" })
    public void testGetDeployment() throws Exception {
        AppDeployment existingDeployment = repositoryService.createDeploymentQuery().singleResult();

        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + AppRestUrls.createRelativeResourceUrl(AppRestUrls.URL_DEPLOYMENT, existingDeployment.getId()));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);

        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        
        closeResponse(response);

        String deploymentId = responseNode.get("id").textValue();
        String name = responseNode.get("name").textValue();
        String category = responseNode.get("category").textValue();
        String deployTime = responseNode.get("deploymentTime").textValue();
        String url = responseNode.get("url").textValue();
        String tenantId = responseNode.get("tenantId").textValue();

        assertEquals("", tenantId);
        assertNotNull(deploymentId);
        assertEquals(existingDeployment.getId(), deploymentId);

        assertNotNull(name);
        assertEquals(existingDeployment.getName(), name);

        assertEquals(existingDeployment.getCategory(), category);

        assertNotNull(deployTime);

        assertNotNull(url);
        assertTrue(url.endsWith(AppRestUrls.createRelativeResourceUrl(AppRestUrls.URL_DEPLOYMENT, deploymentId)));
    }

    /**
     * Test getting an unexisting deployment. GET app-repository/deployments/{deploymentId}
     */
    public void testGetUnexistingDeployment() throws Exception {
        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + AppRestUrls.createRelativeResourceUrl(AppRestUrls.URL_DEPLOYMENT, "unexisting"));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }

    /**
     * Test deleting a single deployment. DELETE app-repository/deployments/{deploymentId}
     */
    @org.flowable.app.engine.test.AppDeployment(resources = { "org/flowable/app/rest/service/api/repository/oneApp.app" })
    public void testDeleteDeployment() throws Exception {
        AppDeployment existingDeployment = repositoryService.createDeploymentQuery().singleResult();
        assertNotNull(existingDeployment);

        // Delete the deployment
        HttpDelete httpDelete = new HttpDelete(SERVER_URL_PREFIX + AppRestUrls.createRelativeResourceUrl(AppRestUrls.URL_DEPLOYMENT, existingDeployment.getId()));
        CloseableHttpResponse response = executeRequest(httpDelete, HttpStatus.SC_NO_CONTENT);
        closeResponse(response);

        existingDeployment = repositoryService.createDeploymentQuery().singleResult();
        assertNull(existingDeployment);
    }

    /**
     * Test deleting an unexisting deployment. DELETE app-repository/deployments/{deploymentId}
     */
    public void testDeleteUnexistingDeployment() throws Exception {
        HttpDelete httpDelete = new HttpDelete(SERVER_URL_PREFIX + AppRestUrls.createRelativeResourceUrl(AppRestUrls.URL_DEPLOYMENT, "unexisting"));
        CloseableHttpResponse response = executeRequest(httpDelete, HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }
}
