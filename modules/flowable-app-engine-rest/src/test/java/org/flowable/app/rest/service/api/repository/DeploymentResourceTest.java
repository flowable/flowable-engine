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

import net.javacrumbs.jsonunit.core.Option;

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

            assertThat(repositoryService.createDeploymentQuery().deploymentId(deploymentId).count()).isEqualTo(1L);

            assertThatJson(responseNode)
                    .when(Option.IGNORING_EXTRA_FIELDS)
                    .isEqualTo("{"
                            + " id: '${json-unit.any-string}',"
                            + " name: 'oneApp',"
                            + " category: null,"
                            + " deploymentTime: '${json-unit.any-string}',"
                            + " tenantId: \"\","
                            + " url: '" + SERVER_URL_PREFIX + AppRestUrls.createRelativeResourceUrl(AppRestUrls.URL_DEPLOYMENT, deploymentId) + "'"
                            + "}");

            // Check if process is actually deployed in the deployment
            List<String> resources = repositoryService.getDeploymentResourceNames(deploymentId);
            assertThat(resources).hasSize(1);
            assertThat(resources.get(0)).isEqualTo("oneApp.app");
            assertThat(repositoryService.createAppDefinitionQuery().deploymentId(deploymentId).count()).isEqualTo(1L);

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

            assertThat(repositoryService.createDeploymentQuery().deploymentId(deploymentId).count()).isEqualTo(1L);

            assertThatJson(responseNode)
                    .when(Option.IGNORING_EXTRA_FIELDS)
                    .isEqualTo("{"
                            + " id: '${json-unit.any-string}',"
                            + " name: 'vacationRequest',"
                            + " category: null,"
                            + " deploymentTime: '${json-unit.any-string}',"
                            + " tenantId: \"\","
                            + " url: '" + SERVER_URL_PREFIX + AppRestUrls
                            .createRelativeResourceUrl(AppRestUrls.URL_DEPLOYMENT, deploymentId) + "'"
                            + "}");

            // Check if process is actually deployed in the deployment
            List<String> resources = repositoryService.getDeploymentResourceNames(deploymentId);
            assertThat(resources).hasSize(4);

            boolean vacationRequestAppFound = false;
            for (String resourceName : resources) {
                if ("vacationRequestApp.app".equals(resourceName)) {
                    vacationRequestAppFound = true;
                    break;
                }
            }
            assertThat(vacationRequestAppFound).isTrue();

            assertThat(repositoryService.createAppDefinitionQuery().deploymentId(deploymentId).count()).isEqualTo(1L);

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

        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + " id: '" + existingDeployment.getId() + "',"
                        + " name: '" + existingDeployment.getName() + "',"
                        + " category: null,"
                        + " deploymentTime: '${json-unit.any-string}',"
                        + " tenantId: \"\","
                        + " url: '" + SERVER_URL_PREFIX + AppRestUrls
                        .createRelativeResourceUrl(AppRestUrls.URL_DEPLOYMENT, existingDeployment.getId()) + "'"
                        + "}");
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
        assertThat(existingDeployment).isNotNull();

        // Delete the deployment
        HttpDelete httpDelete = new HttpDelete(SERVER_URL_PREFIX + AppRestUrls.createRelativeResourceUrl(AppRestUrls.URL_DEPLOYMENT, existingDeployment.getId()));
        CloseableHttpResponse response = executeRequest(httpDelete, HttpStatus.SC_NO_CONTENT);
        closeResponse(response);

        existingDeployment = repositoryService.createDeploymentQuery().singleResult();
        assertThat(existingDeployment).isNull();
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
