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
package org.flowable.dmn.api;

import java.util.List;
import java.util.Map;

/**
 * Helper for execution a decision.
 * 
 * An instance can be obtained through {@link org.flowable.dmn.api.DmnRuleService#createExecuteDecisionBuilder()}.
 * 
 * decisionKey should be set before calling {@link #execute()} to execute a decision.
 * 
 * 
 * @author Tijs Rademakers
 */
public interface ExecuteDecisionBuilder {

    /**
     * Set the key of the decision
     **/
    ExecuteDecisionBuilder decisionKey(String decisionKey);
    
    /**
     * Set the parent deployment id
     */
    ExecuteDecisionBuilder parentDeploymentId(String parentDeploymentId);

    /**
     * Set the instance id
     **/
    ExecuteDecisionBuilder instanceId(String instanceId);
    
    /**
     * Set the execution id
     **/
    ExecuteDecisionBuilder executionId(String executionId);
    
    /**
     * Set the activity id
     **/
    ExecuteDecisionBuilder activityId(String activityId);
    
    /**
     * Set the scope type
     **/
    ExecuteDecisionBuilder scopeType(String scopeType);

    /**
     * Set the tenantId
     **/
    ExecuteDecisionBuilder tenantId(String tenantId);

    /**
     * Sets the variables
     */
    ExecuteDecisionBuilder variables(Map<String, Object> variables);

    /**
     * Adds a variable
     **/
    ExecuteDecisionBuilder variable(String variableName, Object value);

    /**
     * Executes a decision returning one or more output results with variables
     **/
    List<Map<String, Object>> execute();
    
    /**
     * Executes a decision returning one output result with variables
     **/
    Map<String, Object> executeWithSingleResult();
    
    /**
     * Executes a decision returning a result object including an audit trail
     **/
    DecisionExecutionAuditContainer executeWithAuditTrail();

}
