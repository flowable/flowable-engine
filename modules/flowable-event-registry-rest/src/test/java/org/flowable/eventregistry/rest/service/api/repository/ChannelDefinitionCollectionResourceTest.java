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

import java.util.List;

import org.flowable.eventregistry.api.ChannelDefinition;
import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.rest.service.BaseSpringRestTestCase;
import org.flowable.eventregistry.rest.service.api.EventRestUrls;
import org.flowable.eventregistry.test.EventDeploymentAnnotation;

import com.fasterxml.jackson.databind.JsonNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * Test for all REST-operations related to the Deployment collection.
 * 
 * @author Tijs Rademakers
 */
public class ChannelDefinitionCollectionResourceTest extends BaseSpringRestTestCase {

    /**
     * Test getting channel definitions. GET event-registry-repository/channel-definitions
     */
    public void testGetChannelDefinitions() throws Exception {

        try {
            EventDeployment firstDeployment = repositoryService.createDeployment()
                    .name("Deployment 1")
                    .parentDeploymentId("parent1")
                    .addClasspathResource("org/flowable/eventregistry/rest/service/api/repository/simpleChannel.channel")
                    .deploy();

            ChannelDefinition firstChannelDef = repositoryService.createChannelDefinitionQuery().channelDefinitionKey("myChannel").deploymentId(firstDeployment.getId()).singleResult();
            
            EventDeployment secondDeployment = repositoryService.createDeployment()
                    .name("Deployment 2")
                    .parentDeploymentId("parent2")
                    .addClasspathResource("org/flowable/eventregistry/rest/service/api/repository/simpleChannel.channel")
                    .addClasspathResource("org/flowable/eventregistry/rest/service/api/repository/orderChannel.channel").deploy();
            
            EventDeployment thirdDeployment = repositoryService.createDeployment().name("Deployment 3").addClasspathResource("org/flowable/eventregistry/rest/service/api/repository/simpleChannel2.channel")
                            .addClasspathResource("org/flowable/eventregistry/rest/service/api/repository/orderChannel.channel").deploy();

            ChannelDefinition myChannelDef = repositoryService.createChannelDefinitionQuery().channelDefinitionKey("myChannel").deploymentId(secondDeployment.getId()).singleResult();
            
            ChannelDefinition orderChannelDef = repositoryService.createChannelDefinitionQuery().channelDefinitionKey("orderChannel").deploymentId(secondDeployment.getId()).singleResult();
            
            ChannelDefinition myChannelDef2 = repositoryService.createChannelDefinitionQuery().channelDefinitionKey("myChannel").deploymentId(thirdDeployment.getId()).singleResult();
            
            ChannelDefinition orderChannelDef2 = repositoryService.createChannelDefinitionQuery().channelDefinitionKey("orderChannel").deploymentId(thirdDeployment.getId()).singleResult();
            
            // Test parameterless call
            String baseUrl = EventRestUrls.createRelativeResourceUrl(EventRestUrls.URL_CHANNEL_DEFINITION_COLLECTION);
            assertResultsPresentInDataResponse(baseUrl, firstChannelDef.getId(), myChannelDef.getId(), orderChannelDef.getId(), myChannelDef2.getId(), orderChannelDef2.getId());

            // Verify

            // Test name filtering
            String url = baseUrl + "?name=" + encode("Order channel");
            assertResultsPresentInDataResponse(url, orderChannelDef.getId(), orderChannelDef2.getId());

            // Test nameLike filtering
            url = baseUrl + "?nameLike=" + encode("Order%");
            assertResultsPresentInDataResponse(url, orderChannelDef.getId(), orderChannelDef2.getId());

            // Test nameLikeIgnoreCase filtering
            url = baseUrl + "?nameLikeIgnoreCase=" + encode("order%");
            assertResultsPresentInDataResponse(url, orderChannelDef.getId(), orderChannelDef2.getId());

            // Test key filtering
            url = baseUrl + "?key=orderChannel";
            assertResultsPresentInDataResponse(url, orderChannelDef.getId(), orderChannelDef2.getId());

            // Test keyLike filtering
            url = baseUrl + "?keyLike=" + encode("order%");
            assertResultsPresentInDataResponse(url, orderChannelDef.getId(), orderChannelDef2.getId());

            // Test keyLikeIgnoreCase filtering
            url = baseUrl + "?keyLikeIgnoreCase=" + encode("Order%");
            assertResultsPresentInDataResponse(url, orderChannelDef.getId(), orderChannelDef2.getId());

            // Test resourceName filtering
            url = baseUrl + "?resourceName=org/flowable/eventregistry/rest/service/api/repository/orderChannel.channel";
            assertResultsPresentInDataResponse(url, orderChannelDef.getId(), orderChannelDef2.getId());

            // Test resourceNameLike filtering
            url = baseUrl + "?resourceNameLike=" + encode("%simpleChannel.cha%");
            assertResultsPresentInDataResponse(url, firstChannelDef.getId(), myChannelDef.getId());

            // Test version filtering
            url = baseUrl + "?version=3";
            assertResultsPresentInDataResponse(url, myChannelDef2.getId());

            // Test latest filtering
            url = baseUrl + "?latest=true";
            assertResultsPresentInDataResponse(url, myChannelDef2.getId(), orderChannelDef2.getId());
            url = baseUrl + "?latest=false";
            assertResultsPresentInDataResponse(url, firstChannelDef.getId(), myChannelDef.getId(), orderChannelDef.getId(), myChannelDef2.getId(), orderChannelDef2.getId());

            // Test deploymentId
            url = baseUrl + "?deploymentId=" + secondDeployment.getId();
            assertResultsPresentInDataResponse(url, myChannelDef.getId(), orderChannelDef.getId());

            // Test parentDeploymentId
            url = baseUrl + "?parentDeploymentId=parent2";
            assertResultsPresentInDataResponse(url, myChannelDef.getId(), orderChannelDef.getId());

            // Test parentDeploymentId
            url = baseUrl + "?parentDeploymentId=parent1";
            assertResultsPresentInDataResponse(url, firstChannelDef.getId());

        } finally {
            // Always cleanup any created deployments, even if the test failed
            List<EventDeployment> deployments = repositoryService.createDeploymentQuery().list();
            for (EventDeployment deployment : deployments) {
                repositoryService.deleteDeployment(deployment.getId());
            }
        }
    }

    @EventDeploymentAnnotation(resources = {
            "org/flowable/eventregistry/rest/service/api/repository/jmsInbound.channel",
            "org/flowable/eventregistry/rest/service/api/repository/jmsOutbound.channel",
            "org/flowable/eventregistry/rest/service/api/repository/kafkaInbound.channel",
            "org/flowable/eventregistry/rest/service/api/repository/kafkaOutbound.channel",
    })
    public void testQueryInChannelTypeAndImplementation() {
        JsonNode response = executeAndReadGetRequest("/event-registry-repository/channel-definitions");

        assertThatJson(response)
                .when(Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_ARRAY_ORDER)
                .inPath("data")
                .isEqualTo("["
                        + "  { key: 'jmsInbound', type: 'inbound', implementation: 'jms' },"
                        + "  { key: 'jmsOutbound', type: 'outbound', implementation: 'jms' },"
                        + "  { key: 'kafkaInbound', type: 'inbound', implementation: 'kafka' },"
                        + "  { key: 'kafkaOutbound', type: 'outbound', implementation: 'kafka' }"
                        + "]");

        response = executeAndReadGetRequest("/event-registry-repository/channel-definitions?onlyInbound=true");

        assertThatJson(response)
                .when(Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_ARRAY_ORDER)
                .inPath("data")
                .isEqualTo("["
                        + "  { key: 'jmsInbound', type: 'inbound', implementation: 'jms' },"
                        + "  { key: 'kafkaInbound', type: 'inbound', implementation: 'kafka' }"
                        + "]");

        response = executeAndReadGetRequest("/event-registry-repository/channel-definitions?onlyOutbound=true");

        assertThatJson(response)
                .when(Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_ARRAY_ORDER)
                .inPath("data")
                .isEqualTo("["
                        + "  { key: 'jmsOutbound', type: 'outbound', implementation: 'jms' },"
                        + "  { key: 'kafkaOutbound', type: 'outbound', implementation: 'kafka' }"
                        + "]");

        response = executeAndReadGetRequest("/event-registry-repository/channel-definitions?implementation=jms");

        assertThatJson(response)
                .when(Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_ARRAY_ORDER)
                .inPath("data")
                .isEqualTo("["
                        + "  { key: 'jmsInbound', type: 'inbound', implementation: 'jms' },"
                        + "  { key: 'jmsOutbound', type: 'outbound', implementation: 'jms' }"
                        + "]");

        response = executeAndReadGetRequest("/event-registry-repository/channel-definitions?implementation=dummy");

        assertThatJson(response)
                .when(Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_ARRAY_ORDER)
                .inPath("data")
                .isEqualTo("[]");
    }
}
