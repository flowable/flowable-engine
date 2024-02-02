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
public class DecisionTableCollectionResourceTest extends BaseSpringDmnRestTestCase {

    /**
     * Test getting deployments. GET dmn-repository/deployments
     */
    public void testGetDecisionTables() throws Exception {

        try {
            DmnDeployment firstDeployment = dmnRepositoryService.createDeployment().name("Deployment 1").addClasspathResource("org/flowable/dmn/rest/service/api/repository/simple.dmn").category("cat one")
                    .deploy();

            DmnDeployment secondDeployment = dmnRepositoryService.createDeployment().name("Deployment 2").addClasspathResource("org/flowable/dmn/rest/service/api/repository/simple.dmn").category("cat two")
                    .addClasspathResource("org/flowable/dmn/rest/service/api/repository/simple-2.dmn").deploy();

            DmnDecision firstDefinition = dmnRepositoryService.createDecisionQuery().decisionKey("decision").deploymentId(firstDeployment.getId()).singleResult();

            DmnDecision latestDefinition = dmnRepositoryService.createDecisionQuery().decisionKey("decision").deploymentId(secondDeployment.getId()).singleResult();

            DmnDecision decisionTwo = dmnRepositoryService.createDecisionQuery().decisionKey("decisionTwo").deploymentId(secondDeployment.getId()).singleResult();

            String baseUrl = DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_DECISION_TABLE_COLLECTION);
            assertResultsPresentInDataResponse(baseUrl, firstDefinition.getId(), decisionTwo.getId(), latestDefinition.getId());

            // Verify

            // Test name filtering
            String url = baseUrl + "?name=" + encode("Full Decision Two");
            assertResultsPresentInDataResponse(url, decisionTwo.getId());

            // Test nameLike filtering
            url = baseUrl + "?nameLike=" + encode("Full Decision Tw%");
            assertResultsPresentInDataResponse(url, decisionTwo.getId());

            // Test key filtering
            url = baseUrl + "?key=decisionTwo";
            assertResultsPresentInDataResponse(url, decisionTwo.getId());

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
            assertResultsPresentInDataResponse(url, latestDefinition.getId());

            // Test latest filtering
            url = baseUrl + "?latest=true";
            assertResultsPresentInDataResponse(url, latestDefinition.getId(), decisionTwo.getId());
            url = baseUrl + "?latest=false";
            assertResultsPresentInDataResponse(url, firstDefinition.getId(), latestDefinition.getId(), decisionTwo.getId());

            // Test deploymentId
            url = baseUrl + "?deploymentId=" + secondDeployment.getId();
            assertResultsPresentInDataResponse(url, latestDefinition.getId(), decisionTwo.getId());
        } finally {
            // Always cleanup any created deployments, even if the test failed
            List<DmnDeployment> deployments = dmnRepositoryService.createDeploymentQuery().list();
            for (DmnDeployment deployment : deployments) {
                dmnRepositoryService.deleteDeployment(deployment.getId());
            }
        }
    }
}
