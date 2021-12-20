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
import org.flowable.eventregistry.api.ChannelDefinition;
import org.flowable.eventregistry.rest.service.BaseSpringRestTestCase;
import org.flowable.eventregistry.rest.service.api.EventRestUrls;
import org.flowable.eventregistry.test.ChannelDeploymentAnnotation;

import com.fasterxml.jackson.databind.JsonNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * Test for all REST-operations related to single a Channel Definition resource.
 * 
 * @author Tijs Rademakers
 */
public class ChannelDefinitionResourceTest extends BaseSpringRestTestCase {

    /**
     * Test getting a single event definition. GET event-registry-repository/channel-definitions/{channelDefinitionResource}
     */
    @ChannelDeploymentAnnotation(resources = { "org/flowable/eventregistry/rest/service/api/repository/simpleChannel.channel" })
    public void testGetChannelDefinition() throws Exception {

        ChannelDefinition channelDefinition = repositoryService.createChannelDefinitionQuery().singleResult();

        HttpGet httpGet = new HttpGet(
                SERVER_URL_PREFIX + EventRestUrls.createRelativeResourceUrl(EventRestUrls.URL_CHANNEL_DEFINITION, channelDefinition.getId()));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(responseNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "id: '" + channelDefinition.getId() + "',"
                        + "url: '" + httpGet.getURI().toString() + "',"
                        + "key: '" + channelDefinition.getKey() + "',"
                        + "version: " + channelDefinition.getVersion() + ","
                        + "name: '" + channelDefinition.getName() + "',"
                        + "description: '" + channelDefinition.getDescription() + "',"
                        + "type: '" + channelDefinition.getType() + "',"
                        + "implementation: '" + channelDefinition.getImplementation() + "',"
                        + "deploymentId: '" + channelDefinition.getDeploymentId() + "',"
                        + "deploymentUrl: '" + SERVER_URL_PREFIX + EventRestUrls
                        .createRelativeResourceUrl(EventRestUrls.URL_DEPLOYMENT, channelDefinition.getDeploymentId()) + "',"
                        + "resourceName: '" + channelDefinition.getResourceName() + "',"
                        + "category: '" + channelDefinition.getCategory() + "'"
                        + "}");
    }

    /**
     * Test getting an unexisting channel definition. GET event-registry-repository/channel-definitions/{channelDefinitionId}
     */
    public void testGetUnexistingChannelDefinition() throws Exception {
        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + EventRestUrls.createRelativeResourceUrl(EventRestUrls.URL_CHANNEL_DEFINITION, "unexisting"));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }

    @ChannelDeploymentAnnotation(resources = { "simpleChannel.channel" })
    public void testGetChannelDefinitionResourceData() throws Exception {
        ChannelDefinition channelDefinition = repositoryService.createChannelDefinitionQuery().singleResult();

        HttpGet httpGet = new HttpGet(
                SERVER_URL_PREFIX + EventRestUrls.createRelativeResourceUrl(EventRestUrls.URL_CHANNEL_DEFINITION_RESOURCE_CONTENT, channelDefinition.getId()));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);

        // Check "OK" status
        String content = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
        closeResponse(response);
        assertThat(content).isNotNull();
        JsonNode eventNode = objectMapper.readTree(content);
        assertThatJson(eventNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "key: 'myChannel',"
                        + "channelEventKeyDetection:"
                        + " {"
                        + "   fixedValue: 'myEvent'"
                        + " }"
                        + "}");
    }
    
    @ChannelDeploymentAnnotation(resources = { "simpleChannel.channel" })
    public void testGetChannelDefinitionModel() throws Exception {
        ChannelDefinition channelDefinition = repositoryService.createChannelDefinitionQuery().singleResult();

        HttpGet httpGet = new HttpGet(
                SERVER_URL_PREFIX + EventRestUrls.createRelativeResourceUrl(EventRestUrls.URL_CHANNEL_DEFINITION_MODEL, channelDefinition.getId()));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);

        // Check "OK" status
        String content = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
        closeResponse(response);
        assertThat(content).isNotNull();
        JsonNode eventNode = objectMapper.readTree(content);
        assertThatJson(eventNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "key: 'myChannel',"
                        + "channelEventKeyDetection:"
                        + " {"
                        + "   fixedValue: 'myEvent'"
                        + " }"
                        + "}");
    }

    /**
     * Test getting resource content for an unexisting channel definition .
     */
    public void testGetResourceContentForUnexistingChannelDefinition() throws Exception {
        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + EventRestUrls.createRelativeResourceUrl(EventRestUrls.URL_CHANNEL_DEFINITION_RESOURCE_CONTENT, "unexisting"));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }

}
