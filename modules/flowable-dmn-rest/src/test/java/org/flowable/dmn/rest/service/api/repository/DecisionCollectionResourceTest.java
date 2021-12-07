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

import java.util.List;

import org.flowable.dmn.api.DmnDecision;
import org.flowable.dmn.api.DmnDeployment;
import org.flowable.dmn.rest.service.api.BaseSpringDmnRestTestCase;
import org.flowable.dmn.rest.service.api.DmnRestUrls;

/**
 * @author Yvo Swillens
 */
public class DecisionCollectionResourceTest extends BaseSpringDmnRestTestCase {

    /**
     * Test getting deployments. GET dmn-repository/decisions
     */
    public void testGetDecisions() throws Exception {

        try {
            DmnDeployment firstDeployment = dmnRepositoryService.createDeployment()
                    .name("Deployment 1")
                    .parentDeploymentId("parent1")
                    .addClasspathResource("org/flowable/dmn/rest/service/api/repository/simple.dmn")
                    .category("cat one")
                    .deploy();

            DmnDeployment secondDeployment = dmnRepositoryService.createDeployment()
                    .name("Deployment 2")
                    .addClasspathResource("org/flowable/dmn/rest/service/api/repository/simple.dmn")
                    .parentDeploymentId("parent2")
                    .category("cat two")
                    .addClasspathResource("org/flowable/dmn/rest/service/api/repository/simple-2.dmn").deploy();

            DmnDeployment thirdDeployment = dmnRepositoryService.createDeployment()
                    .name("Deployment 3")
                    .addClasspathResource("org/flowable/dmn/rest/service/api/repository/decision_service-1.dmn")
                    .parentDeploymentId("parent3")
                    .category("cat three")
                    .deploy();

            DmnDeployment fourthDeployment = dmnRepositoryService.createDeployment()
                    .name("Deployment 4")
                    .addClasspathResource("org/flowable/dmn/rest/service/api/repository/decision_service-1.dmn")
                    .parentDeploymentId("parent4")
                    .category("cat four")
                    .addClasspathResource("org/flowable/dmn/rest/service/api/repository/decision_service-2.dmn").deploy();

            DmnDecision firstDefinition = dmnRepositoryService.createDecisionQuery().decisionKey("decision").deploymentId(firstDeployment.getId())
                    .singleResult();

            DmnDecision latestDefinition = dmnRepositoryService.createDecisionQuery().decisionKey("decision").deploymentId(secondDeployment.getId())
                    .singleResult();

            DmnDecision decisionTwo = dmnRepositoryService.createDecisionQuery().decisionKey("decisionTwo").deploymentId(secondDeployment.getId())
                    .singleResult();

            DmnDecision decisionServiceOne = dmnRepositoryService.createDecisionQuery().decisionKey("decisionServiceOne").deploymentId(thirdDeployment.getId())
                    .singleResult();

            DmnDecision latestDecisionServiceOne = dmnRepositoryService.createDecisionQuery().decisionKey("decisionServiceOne")
                    .deploymentId(fourthDeployment.getId()).singleResult();

            DmnDecision decisionServiceTwo = dmnRepositoryService.createDecisionQuery().decisionKey("evaluateMortgageRequestService")
                    .deploymentId(fourthDeployment.getId()).singleResult();

            String baseUrl = DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_DECISION_COLLECTION);
            assertResultsPresentInDataResponse(baseUrl, firstDefinition.getId(), decisionTwo.getId(), latestDefinition.getId(), decisionServiceOne.getId(),
                    latestDecisionServiceOne.getId(), decisionServiceTwo.getId());

            // Verify

            // Test name filtering
            String url = baseUrl + "?name=" + encode("Full Decision Two");
            assertResultsPresentInDataResponse(url, decisionTwo.getId());

            url = baseUrl + "?name=" + encode("Evaluate Mortgage Request Service");
            assertResultsPresentInDataResponse(url, decisionServiceTwo.getId());

            // Test nameLike filtering
            url = baseUrl + "?nameLike=" + encode("Full Decision Tw%");
            assertResultsPresentInDataResponse(url, decisionTwo.getId());

            url = baseUrl + "?nameLike=" + encode("Evaluate Mortgage Request S%");
            assertResultsPresentInDataResponse(url, decisionServiceTwo.getId());

            // Test key filtering
            url = baseUrl + "?key=decisionTwo";
            assertResultsPresentInDataResponse(url, decisionTwo.getId());

            url = baseUrl + "?key=evaluateMortgageRequestService";
            assertResultsPresentInDataResponse(url, decisionServiceTwo.getId());

            // Test returning multiple versions for the same key
            url = baseUrl + "?key=decision";
            assertResultsPresentInDataResponse(url, firstDefinition.getId(), latestDefinition.getId());

            // Test keyLike filtering
            url = baseUrl + "?keyLike=" + encode("%Two");
            assertResultsPresentInDataResponse(url, decisionTwo.getId());

            // Test resourceName filtering
            url = baseUrl + "?resourceName=org/flowable/dmn/rest/service/api/repository/simple-2.dmn";
            assertResultsPresentInDataResponse(url, decisionTwo.getId());

            // Test resourceNameLike filtering
            url = baseUrl + "?resourceNameLike=" + encode("%simple-2%");
            assertResultsPresentInDataResponse(url, decisionTwo.getId());

            // Test version filtering
            url = baseUrl + "?version=2";
            assertResultsPresentInDataResponse(url, latestDefinition.getId(), latestDecisionServiceOne.getId());

            // Test latest filtering
            url = baseUrl + "?latest=true";
            assertResultsPresentInDataResponse(url, latestDefinition.getId(), decisionTwo.getId(), latestDecisionServiceOne.getId(),
                    decisionServiceTwo.getId());
            url = baseUrl + "?latest=false";
            assertResultsPresentInDataResponse(url, firstDefinition.getId(), latestDefinition.getId(), decisionTwo.getId(), decisionServiceOne.getId(),
                    latestDecisionServiceOne.getId(), decisionServiceTwo.getId());

            // Test deploymentId
            url = baseUrl + "?deploymentId=" + secondDeployment.getId();
            assertResultsPresentInDataResponse(url, latestDefinition.getId(), decisionTwo.getId());

            // Test deploymentId
            url = baseUrl + "?deploymentId=" + fourthDeployment.getId();
            assertResultsPresentInDataResponse(url, latestDecisionServiceOne.getId(), decisionServiceTwo.getId());

            // Test parentDeploymentId
            url = baseUrl + "?parentDeploymentId=parent2";
            assertResultsPresentInDataResponse(url, latestDefinition.getId(), decisionTwo.getId());
            // Test parentDeploymentId
            url = baseUrl + "?parentDeploymentId=parent1";
            assertResultsPresentInDataResponse(url, firstDefinition.getId());

            // Test parentDeploymentId
            url = baseUrl + "?parentDeploymentId=parent4";
            assertResultsPresentInDataResponse(url, latestDecisionServiceOne.getId(), decisionServiceTwo.getId());
            // Test parentDeploymentId
            url = baseUrl + "?parentDeploymentId=parent3";
            assertResultsPresentInDataResponse(url, decisionServiceOne.getId());
        } finally {
            // Always cleanup any created deployments, even if the test failed
            List<DmnDeployment> deployments = dmnRepositoryService.createDeploymentQuery().list();
            for (DmnDeployment deployment : deployments) {
                dmnRepositoryService.deleteDeployment(deployment.getId());
            }
        }
    }
}
