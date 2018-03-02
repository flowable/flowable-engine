package org.flowable.cmmn.rest.service.api.repository;

import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.cmmn.rest.service.BaseSpringRestTestCase;
import org.flowable.cmmn.rest.service.HttpMultipartHelper;
import org.flowable.cmmn.rest.service.api.RestUrls;
import org.flowable.engine.common.impl.util.ReflectUtil;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Test for all REST-operations related to a single Deployment resource.
 * 
 * @author Tijs Rademakers
 */
public class DeploymentResourceTest extends BaseSpringRestTestCase {

    /**
     * Test deploying singe cmmn file. POST cmmn-repository/deployments
     */
    public void testPostNewDeploymentCmmnFile() throws Exception {
        try {
            // Upload a valid BPMN-file using multipart-data
            HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_DEPLOYMENT_COLLECTION));
            httpPost.setEntity(HttpMultipartHelper.getMultiPartEntity("oneHumanTaskCase.cmmn", "application/xml",
                    ReflectUtil.getResourceAsStream("org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn"), null));
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
            assertEquals("oneHumanTaskCase", name);

            assertNotNull(url);
            assertTrue(url.endsWith(RestUrls.createRelativeResourceUrl(RestUrls.URL_DEPLOYMENT, deploymentId)));

            // No deployment-category should have been set
            assertNull(category);
            assertNotNull(deployTime);

            // Check if process is actually deployed in the deployment
            List<String> resources = repositoryService.getDeploymentResourceNames(deploymentId);
            assertEquals(1L, resources.size());
            assertEquals("oneHumanTaskCase.cmmn", resources.get(0));
            assertEquals(1L, repositoryService.createCaseDefinitionQuery().deploymentId(deploymentId).count());

        } finally {
            // Always cleanup any created deployments, even if the test failed
            List<CmmnDeployment> deployments = repositoryService.createDeploymentQuery().list();
            for (CmmnDeployment deployment : deployments) {
                repositoryService.deleteDeployment(deployment.getId(), true);
            }
        }
    }

    /**
     * Test deploying an invalid file. POST repository/deployments
     */
    public void testPostNewDeploymentInvalidFile() throws Exception {
        // Upload a valid BPMN-file using multipart-data
        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_DEPLOYMENT_COLLECTION));
        httpPost.setEntity(HttpMultipartHelper.getMultiPartEntity("oneTaskProcess.invalidfile", "application/xml",
                ReflectUtil.getResourceAsStream("org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn"), null));
        closeResponse(executeBinaryRequest(httpPost, HttpStatus.SC_BAD_REQUEST));
    }

    /**
     * Test getting a single deployment. GET repository/deployments/{deploymentId}
     */
    @org.flowable.cmmn.engine.test.CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testGetDeployment() throws Exception {
        CmmnDeployment existingDeployment = repositoryService.createDeploymentQuery().singleResult();

        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_DEPLOYMENT, existingDeployment.getId()));
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
        assertTrue(url.endsWith(RestUrls.createRelativeResourceUrl(RestUrls.URL_DEPLOYMENT, deploymentId)));
    }

    /**
     * Test getting an unexisting deployment. GET repository/deployments/{deploymentId}
     */
    public void testGetUnexistingDeployment() throws Exception {
        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_DEPLOYMENT, "unexisting"));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }

    /**
     * Test deleting a single deployment. DELETE repository/deployments/{deploymentId}
     */
    @org.flowable.cmmn.engine.test.CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testDeleteDeployment() throws Exception {
        CmmnDeployment existingDeployment = repositoryService.createDeploymentQuery().singleResult();
        assertNotNull(existingDeployment);

        // Delete the deployment
        HttpDelete httpDelete = new HttpDelete(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_DEPLOYMENT, existingDeployment.getId()));
        CloseableHttpResponse response = executeRequest(httpDelete, HttpStatus.SC_NO_CONTENT);
        closeResponse(response);

        existingDeployment = repositoryService.createDeploymentQuery().singleResult();
        assertNull(existingDeployment);
    }

    /**
     * Test deleting an unexisting deployment. DELETE repository/deployments/{deploymentId}
     */
    public void testDeleteUnexistingDeployment() throws Exception {
        HttpDelete httpDelete = new HttpDelete(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_DEPLOYMENT, "unexisting"));
        CloseableHttpResponse response = executeRequest(httpDelete, HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }
}
