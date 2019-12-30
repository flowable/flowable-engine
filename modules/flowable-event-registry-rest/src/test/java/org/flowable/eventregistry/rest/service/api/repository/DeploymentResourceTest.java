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
package org.flowable.eventregistry.rest.service.api.repository;

import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.flowable.common.engine.impl.util.ReflectUtil;
import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.rest.service.BaseSpringRestTestCase;
import org.flowable.eventregistry.rest.service.HttpMultipartHelper;
import org.flowable.eventregistry.rest.service.api.EventRestUrls;
import org.flowable.eventregistry.test.EventDeploymentAnnotation;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Test for all REST-operations related to a single Deployment resource.
 * 
 * @author Tijs Rademakers
 */
public class DeploymentResourceTest extends BaseSpringRestTestCase {

    /**
     * Test deploying singe event definition file. POST event-registry-repository/deployments
     */
    public void testPostNewDeploymentEventFile() throws Exception {
        try {
            HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + EventRestUrls.createRelativeResourceUrl(EventRestUrls.URL_DEPLOYMENT_COLLECTION));
            httpPost.setEntity(HttpMultipartHelper.getMultiPartEntity("simpleEvent.event", "application/json",
                    ReflectUtil.getResourceAsStream("org/flowable/eventregistry/rest/service/api/repository/simpleEvent.event"), null));
            CloseableHttpResponse response = executeBinaryRequest(httpPost, HttpStatus.SC_CREATED);

            // Check deployment
            JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);

            String newDeploymentId = responseNode.get("id").textValue();
            String name = responseNode.get("name").textValue();
            String category = responseNode.get("category").textValue();
            String deployTime = responseNode.get("deploymentTime").textValue();
            String url = responseNode.get("url").textValue();
            String tenantId = responseNode.get("tenantId").textValue();

            assertEquals("", tenantId);

            assertNotNull(newDeploymentId);
            assertEquals(1L, repositoryService.createDeploymentQuery().deploymentId(newDeploymentId).count());

            assertNotNull(name);
            assertEquals("simpleEvent", name);

            assertNotNull(url);
            assertTrue(url.endsWith(EventRestUrls.createRelativeResourceUrl(EventRestUrls.URL_DEPLOYMENT, newDeploymentId)));

            // No deployment-category should have been set
            assertNull(category);
            assertNotNull(deployTime);

            // Check if process is actually deployed in the deployment
            List<String> resources = repositoryService.getDeploymentResourceNames(newDeploymentId);
            assertEquals(1L, resources.size());
            assertEquals("simpleEvent.event", resources.get(0));
            assertEquals(1L, repositoryService.createEventDefinitionQuery().deploymentId(newDeploymentId).count());

        } finally {
            // Always cleanup any created deployments, even if the test failed
            List<EventDeployment> deployments = repositoryService.createDeploymentQuery().list();
            for (EventDeployment deployment : deployments) {
                repositoryService.deleteDeployment(deployment.getId());
            }
        }
    }

    /**
     * Test deploying an invalid file. POST repository/deployments
     */
    public void testPostNewDeploymentInvalidFile() throws Exception {
        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + EventRestUrls.createRelativeResourceUrl(EventRestUrls.URL_DEPLOYMENT_COLLECTION));
        httpPost.setEntity(HttpMultipartHelper.getMultiPartEntity("simpleEvent.invalidfile", "application/json",
                ReflectUtil.getResourceAsStream("org/flowable/eventregistry/rest/service/api/repository/simpleEvent.event"), null));
        closeResponse(executeBinaryRequest(httpPost, HttpStatus.SC_BAD_REQUEST));
    }

    /**
     * Test getting a single deployment. GET repository/deployments/{deploymentId}
     */
    @EventDeploymentAnnotation(resources = { "org/flowable/eventregistry/rest/service/api/repository/simpleEvent.event" })
    public void testGetDeployment() throws Exception {
        EventDeployment existingDeployment = repositoryService.createDeploymentQuery().singleResult();

        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + EventRestUrls.createRelativeResourceUrl(EventRestUrls.URL_DEPLOYMENT, existingDeployment.getId()));
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
        assertTrue(url.endsWith(EventRestUrls.createRelativeResourceUrl(EventRestUrls.URL_DEPLOYMENT, deploymentId)));
    }

    /**
     * Test getting an unexisting deployment. GET repository/deployments/{deploymentId}
     */
    public void testGetUnexistingDeployment() throws Exception {
        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + EventRestUrls.createRelativeResourceUrl(EventRestUrls.URL_DEPLOYMENT, "unexisting"));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }

    /**
     * Test deleting a single deployment. DELETE repository/deployments/{deploymentId}
     */
    public void testDeleteDeployment() throws Exception {
        repositoryService.createDeployment().name("Deployment 1").category("DEF")
                    .addClasspathResource("org/flowable/eventregistry/rest/service/api/repository/simpleEvent.event")
                    .deploy();
        
        EventDeployment existingDeployment = repositoryService.createDeploymentQuery().singleResult();
        assertNotNull(existingDeployment);

        // Delete the deployment
        HttpDelete httpDelete = new HttpDelete(SERVER_URL_PREFIX + EventRestUrls.createRelativeResourceUrl(EventRestUrls.URL_DEPLOYMENT, existingDeployment.getId()));
        CloseableHttpResponse response = executeRequest(httpDelete, HttpStatus.SC_NO_CONTENT);
        closeResponse(response);

        existingDeployment = repositoryService.createDeploymentQuery().singleResult();
        assertNull(existingDeployment);
    }

    /**
     * Test deleting an unexisting deployment. DELETE repository/deployments/{deploymentId}
     */
    public void testDeleteUnexistingDeployment() throws Exception {
        HttpDelete httpDelete = new HttpDelete(SERVER_URL_PREFIX + EventRestUrls.createRelativeResourceUrl(EventRestUrls.URL_DEPLOYMENT, "unexisting"));
        CloseableHttpResponse response = executeRequest(httpDelete, HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }
}
