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
package org.flowable.rest.service.api.repository;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.flowable.common.engine.impl.util.ReflectUtil;
import org.flowable.engine.repository.Deployment;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.HttpMultipartHelper;
import org.flowable.rest.service.api.RestUrls;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import static org.junit.Assert.*;

/**
 * Test for all REST-operations related to a single Deployment resource.
 * 
 * @author Frederik Heremans
 */
public class DeploymentResourceTest extends BaseSpringRestTestCase {

    /**
     * Test deploying singe bpmn-file. POST repository/deployments
     */
    @Test
    public void testPostNewDeploymentBPMNFile() throws Exception {
        try {
            // Upload a valid BPMN-file using multipart-data
            HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_DEPLOYMENT_COLLECTION));
            httpPost.setEntity(HttpMultipartHelper.getMultiPartEntity("oneTaskProcess.bpmn20.xml", "application/xml",
                    ReflectUtil.getResourceAsStream("org/flowable/rest/service/api/repository/oneTaskProcess.bpmn20.xml"), null));
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
            assertEquals("oneTaskProcess", name);

            assertNotNull(url);
            assertTrue(url.endsWith(RestUrls.createRelativeResourceUrl(RestUrls.URL_DEPLOYMENT, deploymentId)));

            // No deployment-category should have been set
            assertNull(category);
            assertNotNull(deployTime);

            // Check if process is actually deployed in the deployment
            List<String> resources = repositoryService.getDeploymentResourceNames(deploymentId);
            assertEquals(1L, resources.size());
            assertEquals("oneTaskProcess.bpmn20.xml", resources.get(0));
            assertEquals(1L, repositoryService.createProcessDefinitionQuery().deploymentId(deploymentId).count());

        } finally {
            // Always cleanup any created deployments, even if the test failed
            List<Deployment> deployments = repositoryService.createDeploymentQuery().list();
            for (Deployment deployment : deployments) {
                repositoryService.deleteDeployment(deployment.getId(), true);
            }
        }
    }

    /**
     * Test deploying bar-file. POST repository/deployments
     */
    @Test
    public void testPostNewDeploymentBarFile() throws Exception {
        try {
            // Create zip with bpmn-file and resource
            ByteArrayOutputStream zipOutput = new ByteArrayOutputStream();
            ZipOutputStream zipStream = new ZipOutputStream(zipOutput);

            // Add bpmn-xml
            zipStream.putNextEntry(new ZipEntry("oneTaskProcess.bpmn20.xml"));
            IOUtils.copy(ReflectUtil.getResourceAsStream("org/flowable/rest/service/api/repository/oneTaskProcess.bpmn20.xml"), zipStream);
            zipStream.closeEntry();

            // Add text-resource
            zipStream.putNextEntry(new ZipEntry("test.txt"));
            IOUtils.write("Testing REST-deployment with tenant", zipStream);
            zipStream.closeEntry();
            zipStream.close();

            // Upload a bar-file using multipart-data
            HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_DEPLOYMENT_COLLECTION));
            httpPost.setEntity(HttpMultipartHelper.getMultiPartEntity("test-deployment.bar", "application/zip", new ByteArrayInputStream(zipOutput.toByteArray()), null));
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
            assertEquals("test-deployment", name);

            assertNotNull(url);
            assertTrue(url.endsWith(RestUrls.createRelativeResourceUrl(RestUrls.URL_DEPLOYMENT, deploymentId)));

            // No deployment-category should have been set
            assertNull(category);
            assertNotNull(deployTime);

            // Check if both resources are deployed and process is actually
            // deployed in the deployment
            List<String> resources = repositoryService.getDeploymentResourceNames(deploymentId);
            assertEquals(2L, resources.size());
            assertEquals(1L, repositoryService.createProcessDefinitionQuery().deploymentId(deploymentId).count());
        } finally {
            // Always cleanup any created deployments, even if the test failed
            List<Deployment> deployments = repositoryService.createDeploymentQuery().list();
            for (Deployment deployment : deployments) {
                repositoryService.deleteDeployment(deployment.getId(), true);
            }
        }
    }

    /**
     * Test deploying bar-file. POST repository/deployments
     */
    @Test
    public void testPostNewDeploymentBarFileWithTenantId() throws Exception {
        try {
            // Create zip with bpmn-file and resource
            ByteArrayOutputStream zipOutput = new ByteArrayOutputStream();
            ZipOutputStream zipStream = new ZipOutputStream(zipOutput);

            // Add bpmn-xml
            zipStream.putNextEntry(new ZipEntry("oneTaskProcess.bpmn20.xml"));
            IOUtils.copy(ReflectUtil.getResourceAsStream("org/flowable/rest/service/api/repository/oneTaskProcess.bpmn20.xml"), zipStream);
            zipStream.closeEntry();

            // Add text-resource
            zipStream.putNextEntry(new ZipEntry("test.txt"));
            IOUtils.write("Testing REST-deployment", zipStream);
            zipStream.closeEntry();
            zipStream.close();

            // Upload a bar-file using multipart-data
            HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_DEPLOYMENT_COLLECTION));
            httpPost.setEntity(HttpMultipartHelper.getMultiPartEntity("test-deployment.bar", "application/zip", new ByteArrayInputStream(zipOutput.toByteArray()),
                    Collections.singletonMap("tenantId", "myTenant")));
            CloseableHttpResponse response = executeBinaryRequest(httpPost, HttpStatus.SC_CREATED);

            // Check deployment
            JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);

            String tenantId = responseNode.get("tenantId").textValue();
            assertEquals("myTenant", tenantId);
            String id = responseNode.get("id").textValue();

            Deployment deployment = repositoryService.createDeploymentQuery().deploymentId(id).singleResult();
            assertNotNull(deployment);
            assertEquals("myTenant", deployment.getTenantId());

        } finally {
            // Always cleanup any created deployments, even if the test failed
            List<Deployment> deployments = repositoryService.createDeploymentQuery().list();
            for (Deployment deployment : deployments) {
                repositoryService.deleteDeployment(deployment.getId(), true);
            }
        }
    }

    /**
     * Test deploying an invalid file. POST repository/deployments
     */
    @Test
    public void testPostNewDeploymentInvalidFile() throws Exception {
        // Upload a valid BPMN-file using multipart-data
        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_DEPLOYMENT_COLLECTION));
        httpPost.setEntity(HttpMultipartHelper.getMultiPartEntity("oneTaskProcess.invalidfile", "application/xml",
                ReflectUtil.getResourceAsStream("org/flowable/rest/service/api/repository/oneTaskProcess.bpmn20.xml"), null));
        closeResponse(executeBinaryRequest(httpPost, HttpStatus.SC_BAD_REQUEST));
    }

    /**
     * Test getting a single deployment. GET repository/deployments/{deploymentId}
     */
    @Test
    @org.flowable.engine.test.Deployment(resources = { "org/flowable/rest/service/api/repository/oneTaskProcess.bpmn20.xml" })
    public void testGetDeployment() throws Exception {
        Deployment existingDeployment = repositoryService.createDeploymentQuery().singleResult();

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
    @Test
    public void testGetUnexistingDeployment() throws Exception {
        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_DEPLOYMENT, "unexisting"));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }

    /**
     * Test deleting a single deployment. DELETE repository/deployments/{deploymentId}
     */
    @Test
    @org.flowable.engine.test.Deployment(resources = { "org/flowable/rest/service/api/repository/oneTaskProcess.bpmn20.xml" })
    public void testDeleteDeployment() throws Exception {
        Deployment existingDeployment = repositoryService.createDeploymentQuery().singleResult();
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
    @Test
    public void testDeleteUnexistingDeployment() throws Exception {
        HttpDelete httpDelete = new HttpDelete(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_DEPLOYMENT, "unexisting"));
        CloseableHttpResponse response = executeRequest(httpDelete, HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }
}
