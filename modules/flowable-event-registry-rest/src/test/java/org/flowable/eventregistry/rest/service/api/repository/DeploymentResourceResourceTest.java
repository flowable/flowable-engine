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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.message.BasicHeader;
import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.rest.service.BaseSpringRestTestCase;
import org.flowable.eventregistry.rest.service.api.EventRestUrls;

import com.fasterxml.jackson.databind.JsonNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * Test for all REST-operations related to a resources that is part of a deployment.
 * 
 * @author Tijs Rademakers
 */
public class DeploymentResourceResourceTest extends BaseSpringRestTestCase {

    /**
     * Test getting a single resource, deployed in a deployment. GET repository/deployments/{deploymentId}/resources/{resourceId}
     */
    public void testGetDeploymentResource() throws Exception {
        try {
            String rawResourceName = "org/flowable/eventregistry/rest/service/api/repository/simpleEvent.event";
            EventDeployment deployment = repositoryService.createDeployment().name("Deployment 1").addClasspathResource(rawResourceName)
                    .addInputStream("test.txt", new ByteArrayInputStream("Test content".getBytes())).deploy();

            // Build up the URL manually to make sure resource-id gets encoded correctly as one piece
            HttpGet httpGet = new HttpGet(buildUrl(EventRestUrls.URL_DEPLOYMENT_RESOURCE, deployment.getId(), encode(rawResourceName)));
            httpGet.addHeader(new BasicHeader(HttpHeaders.ACCEPT, "application/json"));
            CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);
            JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);
            assertThatJson(responseNode)
                    .when(Option.IGNORING_EXTRA_FIELDS)
                    .isEqualTo("{"
                            + "url: '" + buildUrl(EventRestUrls.URL_DEPLOYMENT_RESOURCE, deployment.getId(), rawResourceName) + "',"
                            + "mediaType: 'application/json',"
                            + "contentUrl: '" + buildUrl(EventRestUrls.URL_DEPLOYMENT_RESOURCE_CONTENT, deployment.getId(), rawResourceName) + "',"
                            + "type: 'eventDefinition'"
                            + "}");

        } finally {
            // Always cleanup any created deployments, even if the test failed
            List<EventDeployment> deployments = repositoryService.createDeploymentQuery().list();
            for (EventDeployment deployment : deployments) {
                repositoryService.deleteDeployment(deployment.getId());
            }
        }
    }

    /**
     * Test getting a single resource for an unexisting deployment. GET repository/deployments/{deploymentId}/resources/{resourceId}
     */
    public void testGetDeploymentResourceUnexistingDeployment() throws Exception {
        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + EventRestUrls.createRelativeResourceUrl(EventRestUrls.URL_DEPLOYMENT_RESOURCE, "unexisting", "resource.png"));
        httpGet.addHeader(new BasicHeader(HttpHeaders.ACCEPT, "image/png,application/json"));
        closeResponse(executeRequest(httpGet, HttpStatus.SC_NOT_FOUND));
    }

    /**
     * Test getting an unexisting resource for an existing deployment. GET repository/deployments/{deploymentId}/resources/{resourceId}
     */
    public void testGetDeploymentResourceUnexistingResource() throws Exception {
        try {
            EventDeployment deployment = repositoryService.createDeployment().name("Deployment 1").addInputStream("test.txt", new ByteArrayInputStream("Test content".getBytes())).deploy();

            HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + EventRestUrls.createRelativeResourceUrl(EventRestUrls.URL_DEPLOYMENT_RESOURCE, deployment.getId(), "unexisting-resource.png"));
            httpGet.addHeader(new BasicHeader(HttpHeaders.ACCEPT, "image/png,application/json"));
            closeResponse(executeRequest(httpGet, HttpStatus.SC_NOT_FOUND));

        } finally {
            // Always cleanup any created deployments, even if the test failed
            List<EventDeployment> deployments = repositoryService.createDeploymentQuery().list();
            for (EventDeployment deployment : deployments) {
                repositoryService.deleteDeployment(deployment.getId());
            }
        }
    }

    /**
     * Test getting a deployment resource content. GET repository/deployments/{deploymentId}/resources/{resourceId}
     */
    public void testGetDeploymentResourceContent() throws Exception {
        try {
            EventDeployment deployment = repositoryService.createDeployment().name("Deployment 1").addClasspathResource("simpleChannel.channel").deploy();

            HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + EventRestUrls.createRelativeResourceUrl(EventRestUrls.URL_DEPLOYMENT_RESOURCE_CONTENT,
                    deployment.getId(), "simpleChannel.channel"));
            httpGet.addHeader(new BasicHeader(HttpHeaders.ACCEPT, "application/json"));
            CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);
            String responseAsString = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            closeResponse(response);
            assertThat(responseAsString).isNotNull();
            JsonNode channelNode = objectMapper.readTree(responseAsString);
            assertThatJson(channelNode)
                    .when(Option.IGNORING_EXTRA_FIELDS)
                    .isEqualTo("{"
                            + "key: 'myChannel',"
                            + "channelEventKeyDetection:"
                            + " {"
                            + "   fixedValue: 'myEvent'"
                            + " }"
                            + "}");

        } finally {
            // Always cleanup any created deployments, even if the test failed
            List<EventDeployment> deployments = repositoryService.createDeploymentQuery().list();
            for (EventDeployment deployment : deployments) {
                repositoryService.deleteDeployment(deployment.getId());
            }
        }
    }
}
