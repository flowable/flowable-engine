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

import java.net.URLDecoder;
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

        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + EventRestUrls.createRelativeResourceUrl(EventRestUrls.URL_CHANNEL_DEFINITION, channelDefinition.getId()));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertEquals(channelDefinition.getId(), responseNode.get("id").textValue());
        assertEquals(channelDefinition.getKey(), responseNode.get("key").textValue());
        assertEquals(channelDefinition.getCategory(), responseNode.get("category").textValue());
        assertEquals(channelDefinition.getVersion(), responseNode.get("version").intValue());
        assertEquals(channelDefinition.getDescription(), responseNode.get("description").textValue());
        assertEquals(channelDefinition.getName(), responseNode.get("name").textValue());
        assertNotNull(responseNode.get("createTime").textValue());

        // Check URL's
        assertEquals(httpGet.getURI().toString(), responseNode.get("url").asText());
        assertEquals(channelDefinition.getDeploymentId(), responseNode.get("deploymentId").textValue());
        assertTrue(responseNode.get("deploymentUrl").textValue().endsWith(EventRestUrls.createRelativeResourceUrl(EventRestUrls.URL_DEPLOYMENT, channelDefinition.getDeploymentId())));
        assertTrue(URLDecoder.decode(responseNode.get("resource").textValue(), "UTF-8").endsWith(
                EventRestUrls.createRelativeResourceUrl(EventRestUrls.URL_DEPLOYMENT_RESOURCE, channelDefinition.getDeploymentId(), channelDefinition.getResourceName())));
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

        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + EventRestUrls.createRelativeResourceUrl(EventRestUrls.URL_CHANNEL_DEFINITION_RESOURCE_CONTENT, channelDefinition.getId()));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);

        // Check "OK" status
        String content = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
        closeResponse(response);
        assertNotNull(content);
        JsonNode eventNode = objectMapper.readTree(content);
        assertEquals("myChannel", eventNode.get("key").asText());
        assertEquals("myEvent", eventNode.get("channelEventKeyDetection").get("fixedValue").asText());
    }
    
    @ChannelDeploymentAnnotation(resources = { "simpleChannel.channel" })
    public void testGetChannelDefinitionModel() throws Exception {
        ChannelDefinition channelDefinition = repositoryService.createChannelDefinitionQuery().singleResult();

        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + EventRestUrls.createRelativeResourceUrl(EventRestUrls.URL_CHANNEL_DEFINITION_MODEL, channelDefinition.getId()));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);

        // Check "OK" status
        String content = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
        closeResponse(response);
        assertNotNull(content);
        JsonNode eventNode = objectMapper.readTree(content);
        assertEquals("myChannel", eventNode.get("key").asText());
        assertEquals("myEvent", eventNode.get("channelEventKeyDetection").get("fixedValue").asText());
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
