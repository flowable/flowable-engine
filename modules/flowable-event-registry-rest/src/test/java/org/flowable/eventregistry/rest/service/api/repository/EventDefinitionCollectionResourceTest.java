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

import java.util.List;

import org.flowable.eventregistry.api.EventDefinition;
import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.rest.service.BaseSpringRestTestCase;
import org.flowable.eventregistry.rest.service.api.EventRestUrls;

/**
 * Test for all REST-operations related to the Deployment collection.
 * 
 * @author Tijs Rademakers
 */
public class EventDefinitionCollectionResourceTest extends BaseSpringRestTestCase {

    /**
     * Test getting event definitions. GET event-registry-repository/event-definitions
     */
    public void testGetEventDefinitions() throws Exception {

        try {
            EventDeployment firstDeployment = repositoryService.createDeployment()
                    .name("Deployment 1")
                    .parentDeploymentId("parent1")
                    .addClasspathResource("org/flowable/eventregistry/rest/service/api/repository/simpleEvent.event")
                    .deploy();

            EventDefinition firstEventDef = repositoryService.createEventDefinitionQuery().eventDefinitionKey("myEvent").deploymentId(firstDeployment.getId()).singleResult();
            
            EventDeployment secondDeployment = repositoryService.createDeployment()
                    .name("Deployment 2")
                    .parentDeploymentId("parent2")
                    .addClasspathResource("org/flowable/eventregistry/rest/service/api/repository/simpleEvent.event")
                    .addClasspathResource("org/flowable/eventregistry/rest/service/api/repository/orderEvent.event").deploy();
            
            EventDeployment thirdDeployment = repositoryService.createDeployment().name("Deployment 3").addClasspathResource("org/flowable/eventregistry/rest/service/api/repository/simpleEvent2.event")
                            .addClasspathResource("org/flowable/eventregistry/rest/service/api/repository/orderEvent.event").deploy();

            EventDefinition myEventDef = repositoryService.createEventDefinitionQuery().eventDefinitionKey("myEvent").deploymentId(secondDeployment.getId()).singleResult();
            
            EventDefinition orderEventDef = repositoryService.createEventDefinitionQuery().eventDefinitionKey("myOrderEvent").deploymentId(secondDeployment.getId()).singleResult();
            
            EventDefinition myEventDef2 = repositoryService.createEventDefinitionQuery().eventDefinitionKey("myEvent").deploymentId(thirdDeployment.getId()).singleResult();
            
            EventDefinition orderEventDef2 = repositoryService.createEventDefinitionQuery().eventDefinitionKey("myOrderEvent").deploymentId(thirdDeployment.getId()).singleResult();
            
            // Test parameterless call
            String baseUrl = EventRestUrls.createRelativeResourceUrl(EventRestUrls.URL_EVENT_DEFINITION_COLLECTION);
            assertResultsPresentInDataResponse(baseUrl, firstEventDef.getId(), myEventDef.getId(), orderEventDef.getId(), myEventDef2.getId(), orderEventDef2.getId());

            // Verify

            // Test name filtering
            String url = baseUrl + "?name=" + encode("My order event");
            assertResultsPresentInDataResponse(url, orderEventDef.getId(), orderEventDef2.getId());

            // Test nameLike filtering
            url = baseUrl + "?nameLike=" + encode("My order%");
            assertResultsPresentInDataResponse(url, orderEventDef.getId(), orderEventDef2.getId());

            // Test nameLikeIgnorecase filtering
            url = baseUrl + "?nameLikeIgnoreCase=" + encode("my order%");
            assertResultsPresentInDataResponse(url, orderEventDef.getId(), orderEventDef2.getId());

            // Test key filtering
            url = baseUrl + "?key=myOrderEvent";
            assertResultsPresentInDataResponse(url, orderEventDef.getId(), orderEventDef2.getId());

            // Test keyLike filtering
            url = baseUrl + "?keyLike=" + encode("myOrder%");
            assertResultsPresentInDataResponse(url, orderEventDef.getId(), orderEventDef2.getId());

            // Test keyLikeIgnoreCase filtering
            url = baseUrl + "?keyLikeIgnoreCase=" + encode("myorder%");
            assertResultsPresentInDataResponse(url, orderEventDef.getId(), orderEventDef2.getId());

            // Test resourceName filtering
            url = baseUrl + "?resourceName=org/flowable/eventregistry/rest/service/api/repository/orderEvent.event";
            assertResultsPresentInDataResponse(url, orderEventDef.getId(), orderEventDef2.getId());

            // Test resourceNameLike filtering
            url = baseUrl + "?resourceNameLike=" + encode("%simpleEvent.eve%");
            assertResultsPresentInDataResponse(url, firstEventDef.getId(), myEventDef.getId());

            // Test version filtering
            url = baseUrl + "?version=3";
            assertResultsPresentInDataResponse(url, myEventDef2.getId());

            // Test latest filtering
            url = baseUrl + "?latest=true";
            assertResultsPresentInDataResponse(url, myEventDef2.getId(), orderEventDef2.getId());
            url = baseUrl + "?latest=false";
            assertResultsPresentInDataResponse(url, firstEventDef.getId(), myEventDef.getId(), orderEventDef.getId(), myEventDef2.getId(), orderEventDef2.getId());

            // Test deploymentId
            url = baseUrl + "?deploymentId=" + secondDeployment.getId();
            assertResultsPresentInDataResponse(url, myEventDef.getId(), orderEventDef.getId());

            // Test parentDeploymentId
            url = baseUrl + "?parentDeploymentId=parent2";
            assertResultsPresentInDataResponse(url, myEventDef.getId(), orderEventDef.getId());

            // Test parentDeploymentId
            url = baseUrl + "?parentDeploymentId=parent1";
            assertResultsPresentInDataResponse(url, firstEventDef.getId());

        } finally {
            // Always cleanup any created deployments, even if the test failed
            List<EventDeployment> deployments = repositoryService.createDeploymentQuery().list();
            for (EventDeployment deployment : deployments) {
                repositoryService.deleteDeployment(deployment.getId());
            }
        }
    }
}
