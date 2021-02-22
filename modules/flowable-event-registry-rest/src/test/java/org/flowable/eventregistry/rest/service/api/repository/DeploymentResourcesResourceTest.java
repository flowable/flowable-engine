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

import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.rest.service.BaseSpringRestTestCase;
import org.flowable.eventregistry.rest.service.api.EventRestUrls;

import com.fasterxml.jackson.databind.JsonNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * Test for all REST-operations related to listing the resources that are part of a deployment.
 * 
 * @author Frederik Heremans
 */
public class DeploymentResourcesResourceTest extends BaseSpringRestTestCase {

    /**
     * Test getting all resources for a single deployment. GET event-registry-repository/deployments/{deploymentId}/resources
     */
    public void testGetDeploymentResources() throws Exception {

        try {
            EventDeployment deployment = repositoryService.createDeployment().name("Deployment 1").addClasspathResource("simpleEvent.event").deploy();

            HttpGet httpGet = new HttpGet(
                    SERVER_URL_PREFIX + EventRestUrls.createRelativeResourceUrl(EventRestUrls.URL_DEPLOYMENT_RESOURCES, deployment.getId()));
            CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);
            JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);
            assertThat(responseNode.isArray()).isTrue();
            assertThatJson(responseNode)
                    .when(Option.IGNORING_EXTRA_FIELDS)
                    .isEqualTo("[{"
                            + "url: '" + SERVER_URL_PREFIX + EventRestUrls
                            .createRelativeResourceUrl(EventRestUrls.URL_DEPLOYMENT_RESOURCE, deployment.getId(), "simpleEvent.event") + "',"
                            + "mediaType: 'application/json',"
                            + "contentUrl: '" + SERVER_URL_PREFIX + EventRestUrls
                            .createRelativeResourceUrl(EventRestUrls.URL_DEPLOYMENT_RESOURCE_CONTENT, deployment.getId(), "simpleEvent.event") + "',"
                            + "type: 'eventDefinition'"
                            + "}]"
                    );

        } finally {
            // Always cleanup any created deployments, even if the test failed
            List<EventDeployment> deployments = repositoryService.createDeploymentQuery().list();
            for (EventDeployment deployment : deployments) {
                repositoryService.deleteDeployment(deployment.getId());
            }
        }
    }

    /**
     * Test getting all resources for a single unexisting deployment. GET event-registry-repository/deployments/{deploymentId}/resources
     */
    public void testGetDeploymentResourcesUnexistingDeployment() throws Exception {
        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + EventRestUrls.createRelativeResourceUrl(EventRestUrls.URL_DEPLOYMENT_RESOURCES, "unexisting"));
        closeResponse(executeRequest(httpGet, HttpStatus.SC_NOT_FOUND));
    }
}
