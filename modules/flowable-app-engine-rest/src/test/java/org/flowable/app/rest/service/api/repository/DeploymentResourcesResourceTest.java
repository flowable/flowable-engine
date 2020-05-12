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
package org.flowable.app.rest.service.api.repository;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.flowable.app.api.repository.AppDeployment;
import org.flowable.app.rest.AppRestUrls;
import org.flowable.app.rest.service.BaseSpringRestTestCase;

import com.fasterxml.jackson.databind.JsonNode;

import net.javacrumbs.jsonunit.core.Option;

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
            AppDeployment deployment = repositoryService.createDeployment().name("Deployment 1")
                    .addClasspathResource("org/flowable/app/rest/service/api/repository/oneApp.app")
                    .addInputStream("test.txt", new ByteArrayInputStream("Test content".getBytes())).deploy();

            HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + AppRestUrls.createRelativeResourceUrl(AppRestUrls.URL_DEPLOYMENT_RESOURCES, deployment.getId()));
            CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);
            JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);
            assertThat(responseNode.isArray()).isTrue();
            assertThat(responseNode).hasSize(2);

            // Since resources can be returned in any arbitrary order, find the
            // Check URL's for the resource: test.txt
            assertThatJson(responseNode)
                    .when(Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_EXTRA_ARRAY_ITEMS, Option.IGNORING_ARRAY_ORDER)
                    .isEqualTo("["
                            + "  {"
                            + "    id: 'test.txt',"
                            + "    url: '" + SERVER_URL_PREFIX + AppRestUrls
                            .createRelativeResourceUrl(AppRestUrls.URL_DEPLOYMENT_RESOURCE, deployment.getId(), "test.txt") + "',"
                            + "    contentUrl: '" + SERVER_URL_PREFIX + AppRestUrls
                            .createRelativeResourceUrl(AppRestUrls.URL_DEPLOYMENT_RESOURCE_CONTENT, deployment.getId(), "test.txt") + "',"
                            + "    mediaType: null,"
                            + "    type: 'resource'"
                            + "  }"
                            + "]");

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
