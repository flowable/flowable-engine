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

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.flowable.dmn.engine.test.DmnDeployment;
import org.flowable.dmn.engine.test.DmnDeploymentAnnotation;
import org.flowable.dmn.rest.service.api.BaseSpringDmnRestTestCase;
import org.flowable.dmn.rest.service.api.DmnRestUrls;

import com.fasterxml.jackson.databind.JsonNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * @author Yvo Swillens
 */
public class DmnDeploymentResourceTest extends BaseSpringDmnRestTestCase {

    /**
     * Test getting a single deployment. GET dmn-repository/deployments/{deploymentId}
     */
    @DmnDeploymentAnnotation(resources = { "org/flowable/dmn/rest/service/api/repository/simple.dmn" })
    public void testGetDeployment() throws Exception {
        org.flowable.dmn.api.DmnDeployment existingDeployment = dmnRepositoryService.createDeploymentQuery().singleResult();

        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_DEPLOYMENT, existingDeployment.getId()));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);

        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        
        closeResponse(response);

        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  id: '" + existingDeployment.getId() + "',"
                        + "  url: '" + httpGet.getURI().toString() + "',"
                        + "  category: " + existingDeployment.getCategory() + ","
                        + "  name: '" + existingDeployment.getName() + "',"
                        + "  deploymentTime: '${json-unit.any-string}',"
                        + "  tenantId: ''"
                        + "  }"
                );
    }

    /**
     * Test getting a single deployment. GET dmn-repository/deployments/{deploymentId}
     */
    @DmnDeploymentAnnotation(resources = { "org/flowable/dmn/rest/service/api/repository/decision_service-1.dmn" })
    public void testGetDeploymentDecisionService() throws Exception {
        org.flowable.dmn.api.DmnDeployment existingDeployment = dmnRepositoryService.createDeploymentQuery().singleResult();

        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_DEPLOYMENT, existingDeployment.getId()));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);

        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());

        closeResponse(response);

        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "  id: '" + existingDeployment.getId() + "',"
                        + "  url: '" + httpGet.getURI().toString() + "',"
                        + "  category: " + existingDeployment.getCategory() + ","
                        + "  name: '" + existingDeployment.getName() + "',"
                        + "  deploymentTime: '${json-unit.any-string}',"
                        + "  tenantId: ''"
                        + "  }"
                );
    }

    /**
     * Test getting an unexisting deployment. GET dmn-repository/deployments/{deploymentId}
     */
    public void testGetUnexistingDeployment() throws Exception {
        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_DEPLOYMENT, "unexisting"));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }

    /**
     * Test deleting a single deployment. DELETE dmn-repository/deployments/{deploymentId}
     */
    @DmnDeployment(resources = { "org/flowable/dmn/rest/service/api/repository/simple.dmn" })
    public void testDeleteDeployment() throws Exception {
        dmnRepositoryService.createDeploymentQuery().singleResult();
        org.flowable.dmn.api.DmnDeployment existingDeployment = dmnRepositoryService.createDeploymentQuery().singleResult();
        assertThat(existingDeployment).isNotNull();

        // Delete the deployment
        HttpDelete httpDelete = new HttpDelete(SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_DEPLOYMENT, existingDeployment.getId()));
        CloseableHttpResponse response = executeRequest(httpDelete, HttpStatus.SC_NO_CONTENT);
        closeResponse(response);

        existingDeployment = dmnRepositoryService.createDeploymentQuery().singleResult();
        assertThat(existingDeployment).isNull();
    }

    /**
     * Test deleting a single deployment. DELETE dmn-repository/deployments/{deploymentId}
     */
    @DmnDeployment(resources = { "org/flowable/dmn/rest/service/api/repository/decision_service-1.dmn" })
    public void testDeleteDeploymentDecisionService() throws Exception {
        dmnRepositoryService.createDeploymentQuery().singleResult();
        org.flowable.dmn.api.DmnDeployment existingDeployment = dmnRepositoryService.createDeploymentQuery().singleResult();
        assertThat(existingDeployment).isNotNull();

        // Delete the deployment
        HttpDelete httpDelete = new HttpDelete(SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_DEPLOYMENT, existingDeployment.getId()));
        CloseableHttpResponse response = executeRequest(httpDelete, HttpStatus.SC_NO_CONTENT);
        closeResponse(response);

        existingDeployment = dmnRepositoryService.createDeploymentQuery().singleResult();
        assertThat(existingDeployment).isNull();
    }

    /**
     * Test deleting an unexisting deployment. DELETE dmn-repository/deployments/{deploymentId}
     */
    public void testDeleteUnexistingDeployment() throws Exception {
        HttpDelete httpDelete = new HttpDelete(SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_DEPLOYMENT, "unexisting"));
        CloseableHttpResponse response = executeRequest(httpDelete, HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }

}
