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
package org.flowable.dmn.rest.service.api;

import org.flowable.dmn.api.DmnDecisionTable;
import org.flowable.dmn.api.DmnDecisionTableQuery;
import org.flowable.dmn.api.DmnDeployment;
import org.flowable.dmn.api.DmnDeploymentQuery;
import org.flowable.dmn.api.DmnHistoricDecisionExecution;
import org.flowable.dmn.api.DmnHistoricDecisionExecutionQuery;
import org.flowable.dmn.rest.service.api.decision.DmnRuleServiceRequest;

public interface DmnRestApiInterceptor {

    void executeDecisionTable(DmnRuleServiceRequest request);
    
    void accessDecisionTableInfoById(DmnDecisionTable decisionTable);
    
    void accessDecisionTableInfoWithQuery(DmnDecisionTableQuery decisionTableQuery);
    
    void accessDeploymentById(DmnDeployment deployment);
    
    void accessDeploymentsWithQuery(DmnDeploymentQuery deploymentQuery);
    
    void executeNewDeploymentForTenantId(String tenantId);
    
    void deleteDeployment(DmnDeployment deployment);
    
    void accessDmnManagementInfo();
    
    void accessDecisionHistoryInfoById(DmnHistoricDecisionExecution historicExecutionQuery);

    void accessDecisionHistoryInfoWithQuery(DmnHistoricDecisionExecutionQuery historicExecutionQuery);
}
