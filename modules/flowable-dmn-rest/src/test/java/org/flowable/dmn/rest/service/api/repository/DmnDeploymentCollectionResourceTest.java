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
package org.flowable.dmn.rest.service.api.repository;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Calendar;
import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.flowable.dmn.api.DmnDeployment;
import org.flowable.dmn.rest.service.api.BaseSpringDmnRestTestCase;
import org.flowable.dmn.rest.service.api.DmnRestUrls;
import org.flowable.dmn.rest.service.api.HttpMultipartHelper;

import com.fasterxml.jackson.databind.JsonNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * @author Yvo Swillens
 */
public class DmnDeploymentCollectionResourceTest extends BaseSpringDmnRestTestCase {

    /**
     * Test deploying single DMN file
     */
    public void testPostNewDeploymentDMNFile() throws Exception {

        try {
            HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_DEPLOYMENT_COLLECTION));

            httpPost.setEntity(HttpMultipartHelper.getMultiPartEntity("simple.dmn", "application/xml",
                    this.getClass().getClassLoader().getResourceAsStream("org/flowable/dmn/rest/service/api/repository/simple.dmn"), null));
            CloseableHttpResponse response = executeBinaryRequest(httpPost, HttpStatus.SC_CREATED);

            // Check deployment
            JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);

            String deploymentId = responseNode.get("id").textValue();
            assertThatJson(responseNode)
                    .when(Option.IGNORING_EXTRA_FIELDS)
                    .isEqualTo("{"
                            + "  id: " + responseNode.get("id") + ","
                            + "  url: '" + SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_DEPLOYMENT, deploymentId) + "',"
                            + "  category: " + responseNode.get("category") + ","
                            + "  name: " + responseNode.get("name") + ","
                            + "  deploymentTime: '${json-unit.any-string}',"
                            + "  tenantId: ''"
                            + "  }"
                    );

            // No deployment-category should have been set
            assertThat(responseNode.get("category").textValue()).isNull();

            // Check if process is actually deployed in the deployment
            List<String> resources = dmnRepositoryService.getDeploymentResourceNames(deploymentId);
            assertThat(resources).hasSize(1);
            assertThat(resources.get(0)).isEqualTo("simple.dmn");
            assertThat(dmnRepositoryService.createDeploymentQuery().deploymentId(deploymentId).count()).isEqualTo(1);
        } finally {
            // Always cleanup any created deployments, even if the test failed
            List<DmnDeployment> deployments = dmnRepositoryService.createDeploymentQuery().list();
            for (DmnDeployment deployment : deployments) {
                dmnRepositoryService.deleteDeployment(deployment.getId());
            }
        }
    }

    /**
     * Test deploying single DMN file
     */
    public void testPostNewDeploymentDMNFileDecisionService() throws Exception {

        try {
            HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_DEPLOYMENT_COLLECTION));

            httpPost.setEntity(HttpMultipartHelper.getMultiPartEntity("decision_service-1.dmn", "application/xml",
                    this.getClass().getClassLoader().getResourceAsStream("org/flowable/dmn/rest/service/api/repository/decision_service-1.dmn"), null));
            CloseableHttpResponse response = executeBinaryRequest(httpPost, HttpStatus.SC_CREATED);

            // Check deployment
            JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);

            String deploymentId = responseNode.get("id").textValue();
            assertThatJson(responseNode)
                    .when(Option.IGNORING_EXTRA_FIELDS)
                    .isEqualTo("{"
                            + "  id: " + responseNode.get("id") + ","
                            + "  url: '" + SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_DEPLOYMENT, deploymentId) + "',"
                            + "  category: " + responseNode.get("category") + ","
                            + "  name: " + responseNode.get("name") + ","
                            + "  deploymentTime: '${json-unit.any-string}',"
                            + "  tenantId: ''"
                            + "  }"
                    );

            // No deployment-category should have been set
            assertThat(responseNode.get("category").textValue()).isNull();

            // Check if process is actually deployed in the deployment
            List<String> resources = dmnRepositoryService.getDeploymentResourceNames(deploymentId);
            assertThat(resources).containsOnly("decision_service-1.dmn", "decision_service-1.decisionServiceOne.png");
            assertThat(dmnRepositoryService.createDeploymentQuery().deploymentId(deploymentId).count()).isEqualTo(1);
        } finally {
            // Always cleanup any created deployments, even if the test failed
            List<DmnDeployment> deployments = dmnRepositoryService.createDeploymentQuery().list();
            for (DmnDeployment deployment : deployments) {
                dmnRepositoryService.deleteDeployment(deployment.getId());
            }
        }
    }

    /**
     * Test getting deployments. GET dmn-repository/deployments
     */
    public void testGetDeployments() throws Exception {

        try {
            // Alter time to ensure different deployTimes
            Calendar yesterday = Calendar.getInstance();
            yesterday.add(Calendar.DAY_OF_MONTH, -1);
            dmnEngineConfiguration.getClock().setCurrentTime(yesterday.getTime());

            DmnDeployment firstDeployment = dmnRepositoryService.createDeployment().name("Deployment 1").category("DEF")
                    .addClasspathResource("org/flowable/dmn/rest/service/api/repository/simple.dmn")
                    .deploy();

            dmnEngineConfiguration.getClock().setCurrentTime(Calendar.getInstance().getTime());
            DmnDeployment secondDeployment = dmnRepositoryService.createDeployment().name("Deployment 2").category("ABC")
                    .addClasspathResource("org/flowable/dmn/rest/service/api/repository/simple.dmn")
                    .tenantId("myTenant").deploy();

            String baseUrl = DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_DEPLOYMENT_COLLECTION);
            assertResultsPresentInDataResponse(baseUrl, firstDeployment.getId(), secondDeployment.getId());

            // Check name filtering
            String url = baseUrl + "?name=" + encode("Deployment 1");
            assertResultsPresentInDataResponse(url, firstDeployment.getId());

            // Check name-like filtering
            url = baseUrl + "?nameLike=" + encode("%ment 2");
            assertResultsPresentInDataResponse(url, secondDeployment.getId());

            // Check category filtering
            url = baseUrl + "?category=DEF";
            assertResultsPresentInDataResponse(url, firstDeployment.getId());

            // Check category-not-equals filtering
            url = baseUrl + "?categoryNotEquals=DEF";
            assertResultsPresentInDataResponse(url, secondDeployment.getId());

            // Check tenantId filtering
            url = baseUrl + "?tenantId=myTenant";
            assertResultsPresentInDataResponse(url, secondDeployment.getId());

            // Check tenantId filtering
            url = baseUrl + "?tenantId=unexistingTenant";
            assertResultsPresentInDataResponse(url);

            // Check tenantId like filtering
            url = baseUrl + "?tenantIdLike=" + encode("%enant");
            assertResultsPresentInDataResponse(url, secondDeployment.getId());

            // Check without tenantId filtering
            url = baseUrl + "?withoutTenantId=true";
            assertResultsPresentInDataResponse(url, firstDeployment.getId());

            // Check ordering by name
            CloseableHttpResponse response = executeRequest(
                    new HttpGet(SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_DEPLOYMENT_COLLECTION) + "?sort=name&order=asc"),
                    HttpStatus.SC_OK);
            JsonNode dataNode = objectMapper.readTree(response.getEntity().getContent()).get("data");
            closeResponse(response);
            assertThatJson(dataNode)
                    .when(Option.IGNORING_EXTRA_FIELDS)
                    .isEqualTo("[ {"
                            + "      id: '" + firstDeployment.getId() + "'"
                            + "   }, {"
                            + "      id: '" + secondDeployment.getId() + "'"
                            + "   } ]"
                    );

            // Check ordering by deploy time
            response = executeRequest(new HttpGet(
                            SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_DEPLOYMENT_COLLECTION) + "?sort=deployTime&order=asc"),
                    HttpStatus.SC_OK);
            dataNode = objectMapper.readTree(response.getEntity().getContent()).get("data");
            closeResponse(response);
            assertThatJson(dataNode)
                    .when(Option.IGNORING_EXTRA_FIELDS)
                    .isEqualTo("[ {"
                            + "      id: '" + firstDeployment.getId() + "'"
                            + "   }, {"
                            + "      id: '" + secondDeployment.getId() + "'"
                            + "   } ]"
                    );

            // Check ordering by tenantId
            response = executeRequest(
                    new HttpGet(SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_DEPLOYMENT_COLLECTION) + "?sort=tenantId&order=desc"),
                    HttpStatus.SC_OK);
            dataNode = objectMapper.readTree(response.getEntity().getContent()).get("data");
            closeResponse(response);
            assertThatJson(dataNode)
                    .when(Option.IGNORING_EXTRA_FIELDS)
                    .isEqualTo("[ {"
                            + "      id: '" + secondDeployment.getId() + "'"
                            + "   }, {"
                            + "      id: '" + firstDeployment.getId() + "'"
                            + "   } ]"
                    );

            // Check paging
            response = executeRequest(new HttpGet(SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_DEPLOYMENT_COLLECTION)
                            + "?sort=deployTime&order=asc&start=1&size=1"),
                    HttpStatus.SC_OK);
            JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);
            assertThatJson(responseNode)
                    .when(Option.IGNORING_EXTRA_FIELDS)
                    .isEqualTo("{"
                            + "  data: [ {"
                            + "      id: '" + secondDeployment.getId() + "'"
                            + "        } ],"
                            + "  total: 2,"
                            + "  start: 1,"
                            + "  size: 1"
                            + " }"
                    );

        } finally {
            // Always cleanup any created deployments, even if the test failed
            List<DmnDeployment> deployments = dmnRepositoryService.createDeploymentQuery().list();
            for (DmnDeployment deployment : deployments) {
                dmnRepositoryService.deleteDeployment(deployment.getId());
            }
        }
    }

    /**
     * Test getting deployments. GET dmn-repository/deployments
     */
    public void testGetDeploymentsDecisionService() throws Exception {

        try {
            // Alter time to ensure different deployTimes
            Calendar yesterday = Calendar.getInstance();
            yesterday.add(Calendar.DAY_OF_MONTH, -1);
            dmnEngineConfiguration.getClock().setCurrentTime(yesterday.getTime());

            DmnDeployment firstDeployment = dmnRepositoryService.createDeployment().name("Deployment 1").category("DEF")
                    .addClasspathResource("org/flowable/dmn/rest/service/api/repository/decision_service-1.dmn")
                    .deploy();

            dmnEngineConfiguration.getClock().setCurrentTime(Calendar.getInstance().getTime());
            DmnDeployment secondDeployment = dmnRepositoryService.createDeployment().name("Deployment 2").category("ABC")
                    .addClasspathResource("org/flowable/dmn/rest/service/api/repository/decision_service-1.dmn")
                    .tenantId("myTenant").deploy();

            String baseUrl = DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_DEPLOYMENT_COLLECTION);
            assertResultsPresentInDataResponse(baseUrl, firstDeployment.getId(), secondDeployment.getId());

            // Check name filtering
            String url = baseUrl + "?name=" + encode("Deployment 1");
            assertResultsPresentInDataResponse(url, firstDeployment.getId());

            // Check name-like filtering
            url = baseUrl + "?nameLike=" + encode("%ment 2");
            assertResultsPresentInDataResponse(url, secondDeployment.getId());

            // Check category filtering
            url = baseUrl + "?category=DEF";
            assertResultsPresentInDataResponse(url, firstDeployment.getId());

            // Check category-not-equals filtering
            url = baseUrl + "?categoryNotEquals=DEF";
            assertResultsPresentInDataResponse(url, secondDeployment.getId());

            // Check tenantId filtering
            url = baseUrl + "?tenantId=myTenant";
            assertResultsPresentInDataResponse(url, secondDeployment.getId());

            // Check tenantId filtering
            url = baseUrl + "?tenantId=unexistingTenant";
            assertResultsPresentInDataResponse(url);

            // Check tenantId like filtering
            url = baseUrl + "?tenantIdLike=" + encode("%enant");
            assertResultsPresentInDataResponse(url, secondDeployment.getId());

            // Check without tenantId filtering
            url = baseUrl + "?withoutTenantId=true";
            assertResultsPresentInDataResponse(url, firstDeployment.getId());

            // Check ordering by name
            CloseableHttpResponse response = executeRequest(
                    new HttpGet(SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_DEPLOYMENT_COLLECTION) + "?sort=name&order=asc"),
                    HttpStatus.SC_OK);
            JsonNode dataNode = objectMapper.readTree(response.getEntity().getContent()).get("data");
            closeResponse(response);
            assertThatJson(dataNode)
                    .when(Option.IGNORING_EXTRA_FIELDS)
                    .isEqualTo("[ {"
                            + "      id: '" + firstDeployment.getId() + "'"
                            + "   }, {"
                            + "      id: '" + secondDeployment.getId() + "'"
                            + "   } ]"
                    );

            // Check ordering by deploy time
            response = executeRequest(new HttpGet(
                            SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_DEPLOYMENT_COLLECTION) + "?sort=deployTime&order=asc"),
                    HttpStatus.SC_OK);
            dataNode = objectMapper.readTree(response.getEntity().getContent()).get("data");
            closeResponse(response);
            assertThatJson(dataNode)
                    .when(Option.IGNORING_EXTRA_FIELDS)
                    .isEqualTo("[ {"
                            + "      id: '" + firstDeployment.getId() + "'"
                            + "   }, {"
                            + "      id: '" + secondDeployment.getId() + "'"
                            + "   } ]"
                    );

            // Check ordering by tenantId
            response = executeRequest(
                    new HttpGet(SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_DEPLOYMENT_COLLECTION) + "?sort=tenantId&order=desc"),
                    HttpStatus.SC_OK);
            dataNode = objectMapper.readTree(response.getEntity().getContent()).get("data");
            closeResponse(response);
            assertThatJson(dataNode)
                    .when(Option.IGNORING_EXTRA_FIELDS)
                    .isEqualTo("[ {"
                            + "      id: '" + secondDeployment.getId() + "'"
                            + "   }, {"
                            + "      id: '" + firstDeployment.getId() + "'"
                            + "   } ]"
                    );

            // Check paging
            response = executeRequest(new HttpGet(SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_DEPLOYMENT_COLLECTION)
                            + "?sort=deployTime&order=asc&start=1&size=1"),
                    HttpStatus.SC_OK);
            JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);
            assertThatJson(responseNode)
                    .when(Option.IGNORING_EXTRA_FIELDS)
                    .isEqualTo("{"
                            + "  data: [ {"
                            + "      id: '" + secondDeployment.getId() + "'"
                            + "        } ],"
                            + "  total: 2,"
                            + "  start: 1,"
                            + "  size: 1"
                            + " }"
                    );

        } finally {
            // Always cleanup any created deployments, even if the test failed
            List<DmnDeployment> deployments = dmnRepositoryService.createDeploymentQuery().list();
            for (DmnDeployment deployment : deployments) {
                dmnRepositoryService.deleteDeployment(deployment.getId());
            }
        }
    }
}
