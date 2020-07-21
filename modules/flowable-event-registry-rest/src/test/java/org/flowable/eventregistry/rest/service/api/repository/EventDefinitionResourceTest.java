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

import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.flowable.eventregistry.api.EventDefinition;
import org.flowable.eventregistry.rest.service.BaseSpringRestTestCase;
import org.flowable.eventregistry.rest.service.api.EventRestUrls;
import org.flowable.eventregistry.test.EventDeploymentAnnotation;

import com.fasterxml.jackson.databind.JsonNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * Test for all REST-operations related to single an Event Definition resource.
 * 
 * @author Tijs Rademakers
 */
public class EventDefinitionResourceTest extends BaseSpringRestTestCase {

    /**
     * Test getting a single event definition. GET event-registry-repository/event-definitions/{eventDefinitionResource}
     */
    @EventDeploymentAnnotation(resources = { "org/flowable/eventregistry/rest/service/api/repository/simpleEvent.event" })
    public void testGetEventDefinition() throws Exception {

        EventDefinition eventDefinition = repositoryService.createEventDefinitionQuery().singleResult();

        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + EventRestUrls.createRelativeResourceUrl(EventRestUrls.URL_EVENT_DEFINITION, eventDefinition.getId()));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "id: '" + eventDefinition.getId() + "',"
                        + "url: '" + httpGet.getURI().toString() + "',"
                        + "key: '" + eventDefinition.getKey() + "',"
                        + "version: " + eventDefinition.getVersion() + ","
                        + "name: '" + eventDefinition.getName() + "',"
                        + "description: " + eventDefinition.getDescription() + ","
                        + "deploymentId: '" + eventDefinition.getDeploymentId() + "',"
                        + "deploymentUrl: '" + SERVER_URL_PREFIX + EventRestUrls
                        .createRelativeResourceUrl(EventRestUrls.URL_DEPLOYMENT, eventDefinition.getDeploymentId()) + "',"
                        + "resourceName: '" + eventDefinition.getResourceName() + "',"
                        + "category: " + eventDefinition.getCategory()
                        + "}");
    }

    /**
     * Test getting an unexisting event definition. GET event-registry-repository/event-definitions/{eventDefinitionId}
     */
    public void testGetUnexistingEventDefinition() throws Exception {
        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + EventRestUrls.createRelativeResourceUrl(EventRestUrls.URL_EVENT_DEFINITION, "unexisting"));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }

    @EventDeploymentAnnotation(resources = { "simpleEvent.event" })
    public void testGetEventDefinitionResourceData() throws Exception {
        EventDefinition eventDefinition = repositoryService.createEventDefinitionQuery().singleResult();

        HttpGet httpGet = new HttpGet(
                SERVER_URL_PREFIX + EventRestUrls.createRelativeResourceUrl(EventRestUrls.URL_EVENT_DEFINITION_RESOURCE_CONTENT, eventDefinition.getId()));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);

        // Check "OK" status
        String content = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
        closeResponse(response);
        assertThat(content).isNotNull();
        JsonNode eventNode = objectMapper.readTree(content);
        assertThatJson(eventNode)
                .when(Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_EXTRA_ARRAY_ITEMS)
                .isEqualTo("{"
                        + "key: 'myEvent',"
                        + "correlationParameters:"
                        + " [{"
                        + "   name: 'customerId'"
                        + " }]"
                        + "}");
    }
    
    @EventDeploymentAnnotation(resources = { "simpleEvent.event" })
    public void testGetEventDefinitionModel() throws Exception {
        EventDefinition eventDefinition = repositoryService.createEventDefinitionQuery().singleResult();

        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + EventRestUrls.createRelativeResourceUrl(EventRestUrls.URL_EVENT_DEFINITION_MODEL, eventDefinition.getId()));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);

        // Check "OK" status
        String content = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
        closeResponse(response);
        assertThat(content).isNotNull();
        assertThatJson(content)
                .isEqualTo("{"
                        + "  key: 'myEvent',"
                        + "  name: 'My event',"
                        + "  payload: ["
                        + "    { name: 'payload1', type: 'string' },"
                        + "    { name: 'payload2', type: 'integer' },"
                        + "    { name: 'customerId', type: 'string', correlationParameter: true }"
                        + "  ]"
                        + "}");
    }

    /**
     * Test getting resource content for an unexisting event definition .
     */
    public void testGetResourceContentForUnexistingEventDefinition() throws Exception {
        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + EventRestUrls.createRelativeResourceUrl(EventRestUrls.URL_EVENT_DEFINITION_RESOURCE_CONTENT, "unexisting"));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }

}
