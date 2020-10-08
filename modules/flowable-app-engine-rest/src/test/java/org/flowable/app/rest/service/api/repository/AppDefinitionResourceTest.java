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

import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.flowable.app.api.repository.AppDefinition;
import org.flowable.app.engine.test.AppDeployment;
import org.flowable.app.rest.AppRestUrls;
import org.flowable.app.rest.service.BaseSpringRestTestCase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * Test for all REST-operations related to single a Process Definition resource.
 * 
 * @author Tijs Rademakers
 */
public class AppDefinitionResourceTest extends BaseSpringRestTestCase {

    /**
     * Test getting a single app definition. GET app-repository/app-definitions/{appeDefinitionId}
     */
    @AppDeployment(resources = { "org/flowable/app/rest/service/api/repository/oneApp.app" })
    public void testGetAppDefinition() throws Exception {

        AppDefinition appDefinition = repositoryService.createAppDefinitionQuery().singleResult();

        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + AppRestUrls.createRelativeResourceUrl(AppRestUrls.URL_APP_DEFINITION, appDefinition.getId()));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "    id: '" + appDefinition.getId() + "',"
                        + "    key: '" + appDefinition.getKey() + "',"
                        + "    category: null,"
                        + "    version: " + appDefinition.getVersion() + ","
                        + "    description: null,"
                        + "    name: '" + appDefinition.getName() + "',"
                        + "    deploymentId: '" + appDefinition.getDeploymentId() + "',"
                        + "    url: '" + httpGet.getURI().toString() + "'"
                        + "}");
    }

    /**
     * Test getting an unexisting app definition. GET app-repository/app-definitions/{appeDefinitionId}
     */
    public void testGetUnexistingCaseDefinition() throws Exception {
        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + AppRestUrls.createRelativeResourceUrl(AppRestUrls.URL_APP_DEFINITION, "unexisting"));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }

    @AppDeployment(resources = { "org/flowable/app/rest/service/api/repository/oneApp.app" })
    public void testGetAppDefinitionResourceData() throws Exception {
        AppDefinition appDefinition = repositoryService.createAppDefinitionQuery().singleResult();

        HttpGet httpGet = new HttpGet(
                SERVER_URL_PREFIX + AppRestUrls.createRelativeResourceUrl(AppRestUrls.URL_APP_DEFINITION_RESOURCE_CONTENT, appDefinition.getId()));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);

        // Check "OK" status
        String content = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
        closeResponse(response);
        assertThat(content)
                .isNotNull()
                .contains("oneApp");
    }

    /**
     * Test getting resource content for an unexisting app definition .
     */
    public void testGetResourceContentForUnexistingAppDefinition() throws Exception {
        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + AppRestUrls.createRelativeResourceUrl(AppRestUrls.URL_APP_DEFINITION_RESOURCE_CONTENT, "unexisting"));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }

    @AppDeployment(resources = { "org/flowable/app/rest/service/api/repository/oneApp.app" })
    public void testUpdateAppDefinitionCategory() throws Exception {
        AppDefinition appDefinition = repositoryService.createAppDefinitionQuery().singleResult();
        assertThat(repositoryService.createAppDefinitionQuery().count()).isEqualTo(1);

        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("category", "updatedcategory");

        HttpPut httpPut = new HttpPut(SERVER_URL_PREFIX + AppRestUrls.createRelativeResourceUrl(AppRestUrls.URL_APP_DEFINITION, appDefinition.getId()));
        httpPut.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPut, HttpStatus.SC_OK);

        // Check "OK" status
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "    category: 'updatedcategory'"
                        + "}");

        // Check actual entry in DB
        assertThat(repositoryService.createAppDefinitionQuery().appDefinitionCategory("updatedcategory").count()).isEqualTo(1);

    }

}
