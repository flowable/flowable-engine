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
package org.flowable.dmn.rest.service.api.history;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.dmn.api.DmnDecisionTable;
import org.flowable.dmn.api.DmnDeployment;
import org.flowable.dmn.rest.service.api.BaseSpringDmnRestTestCase;
import org.flowable.dmn.rest.service.api.DmnRestUrls;

/**
 * @author Tijs Rademakers
 */
public class HistoricDecisionExecutionCollectionResourceTest extends BaseSpringDmnRestTestCase {

    /**
     * Test getting deployments. GET dmn-history/historic-decision-executions
     */
    public void testGetHistoricDecisionExecutions() throws Exception {

        try {
            DmnDeployment firstDeployment = dmnRepositoryService.createDeployment().name("Deployment 1")
                            .addClasspathResource("org/flowable/dmn/rest/service/api/repository/simple.dmn")
                            .deploy();

            DmnDecisionTable firstDecision = dmnRepositoryService.createDecisionTableQuery().decisionTableKey("decision").deploymentId(firstDeployment.getId()).singleResult();
            
            Map<String, Object> variables = new HashMap<>();
            variables.put("inputVariable1", 1);
            variables.put("inputVariable2", "test");
            dmnRuleService.createExecuteDecisionBuilder().decisionKey("decision").variables(variables).activityId("test1").instanceId("instance1").executeWithSingleResult();
            
            variables = new HashMap<>();
            variables.put("inputVariable1", 2);
            variables.put("inputVariable2", "test");
            dmnRuleService.createExecuteDecisionBuilder().decisionKey("decision").variables(variables).activityId("test2").instanceId("instance1").executeWithSingleResult();
            
            String executionId1 = dmnHistoryService.createHistoricDecisionExecutionQuery().activityId("test1").singleResult().getId();
            String executionId2 = dmnHistoryService.createHistoricDecisionExecutionQuery().activityId("test2").singleResult().getId();
            
            String baseUrl = DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_HISTORIC_DECISION_EXECUTION_COLLECTION);
            assertResultsPresentInDataResponse(baseUrl, executionId1, executionId2);

            // Verify

            String url = baseUrl + "?activityId=test1";
            assertResultsPresentInDataResponse(url, executionId1);

            url = baseUrl + "?instanceId=instance1";
            assertResultsPresentInDataResponse(url, executionId1, executionId2);
            
            url = baseUrl + "?instanceId=instance2";
            assertResultsPresentInDataResponse(url);
            
            url = baseUrl + "?decisionKey=" + firstDecision.getKey();
            assertResultsPresentInDataResponse(url, executionId1, executionId2);
            
            url = baseUrl + "?decisionKey=unexisting";
            assertResultsPresentInDataResponse(url);

            url = baseUrl + "?deploymentId=" + firstDeployment.getId();
            assertResultsPresentInDataResponse(url, executionId1, executionId2);
            
            url = baseUrl + "?deploymentId=unexisting";
            assertResultsPresentInDataResponse(url);
            
        } finally {
            // Always cleanup any created deployments, even if the test failed
            List<DmnDeployment> deployments = dmnRepositoryService.createDeploymentQuery().list();
            for (DmnDeployment deployment : deployments) {
                dmnRepositoryService.deleteDeployment(deployment.getId());
            }
        }
    }
}
