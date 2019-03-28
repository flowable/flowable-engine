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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.flowable.engine.repository.Deployment;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.api.RestUrls;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Test for all REST-operations related to listing the resources that are part of a deployment.
 * 
 * @author Frederik Heremans
 */
public class DeploymentResourcesResourceTest extends BaseSpringRestTestCase {

    /**
     * Test getting all resources for a single deployment. GET repository/deployments/{deploymentId}/resources
     */
    @Test
    public void testGetDeploymentResources() throws Exception {

        try {
            Deployment deployment = repositoryService.createDeployment().name("Deployment 1").addClasspathResource("org/flowable/rest/service/api/repository/oneTaskProcess.bpmn20.xml")
                    .addInputStream("test.txt", new ByteArrayInputStream("Test content".getBytes())).deploy();

            HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_DEPLOYMENT_RESOURCES, deployment.getId()));
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
            assertTrue(txtNode.get("url").textValue().endsWith(RestUrls.createRelativeResourceUrl(RestUrls.URL_DEPLOYMENT_RESOURCE, deployment.getId(), "test.txt")));
            assertTrue(txtNode.get("contentUrl").textValue().endsWith(RestUrls.createRelativeResourceUrl(RestUrls.URL_DEPLOYMENT_RESOURCE_CONTENT, deployment.getId(), "test.txt")));
            assertTrue(txtNode.get("mediaType").isNull());
            assertEquals("resource", txtNode.get("type").textValue());

        } finally {
            // Always cleanup any created deployments, even if the test failed
            List<Deployment> deployments = repositoryService.createDeploymentQuery().list();
            for (Deployment deployment : deployments) {
                repositoryService.deleteDeployment(deployment.getId(), true);
            }
        }
    }

    /**
     * Test getting all resources for a single unexisting deployment. GET repository/deployments/{deploymentId}/resources
     */
    @Test
    public void testGetDeploymentResourcesUnexistingDeployment() throws Exception {
        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_DEPLOYMENT_RESOURCES, "unexisting"));
        closeResponse(executeRequest(httpGet, HttpStatus.SC_NOT_FOUND));
    }
}
