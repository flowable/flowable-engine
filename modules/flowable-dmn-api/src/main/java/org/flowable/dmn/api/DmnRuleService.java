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

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;

/**
 * Service for executing DMN decisions (decision tables)
 *
 * @author Tijs Rademakers
 * @author Yvo Swillens
 */
public interface DmnRuleService {

    /**
     * Create a builder to execute a decision.
     *
     * @return the {@link ExecuteDecisionBuilder} build
     */
    ExecuteDecisionBuilder createExecuteDecisionBuilder();

    /**
     * Execute a decision identified by it's key.
     *
     * @param decisionKey
     *            the decision key, cannot be null
     * @param inputVariables
     *            map with input variables
     * @return List with Maps containing rule outputs for this execution
     * @throws FlowableObjectNotFoundException
     *             when the decision with given key does not exist.
     * @throws FlowableException
     *             when an error occurs while executing the decision.
     * @deprecated Use the createExecuteDecisionBuilder method instead
     */
    List<Map<String, Object>> executeDecisionByKey(String decisionKey, Map<String, Object> inputVariables);

    /**
     * Execute a decision identified by it's key. Expecting a single result.
     *
     * @param decisionKey
     *            the decision key, cannot be null
     * @param inputVariables
     *            map with input variables
     * @return Map containing rule output(s) for this execution
     * @throws FlowableObjectNotFoundException
     *             when the decision with given key does not exist.
     * @throws FlowableException
     *             when execution has multiple rule results or when an error occurs while executing the decision.
     * @deprecated Use the createExecuteDecisionBuilder method instead
     */
    Map<String, Object> executeDecisionByKeySingleResult(String decisionKey, Map<String, Object> inputVariables);

    /**
     * Execute a decision identified by it's key.
     * Result contains audit trail.
     *
     * @param decisionKey
     *            the decision key, cannot be null
     * @param inputVariables
     *            map with input variables
     * @return the {@link DecisionExecutionAuditContainer} for this execution
     * @throws FlowableObjectNotFoundException
     *             when the decision with given key does not exist.
     * @throws FlowableException
     *             when an error occurs while executing the decision.
     * @deprecated Use the createExecuteDecisionBuilder method instead
     */
    DecisionExecutionAuditContainer executeDecisionByKeyWithAuditTrail(String decisionKey, Map<String, Object> inputVariables);

    /**
     * Execute a decision identified by it's key and tenant id
     *
     * @param decisionKey
     *            the decision key, cannot be null
     * @param inputVariables
     *            map with input variables
     * @return List with Maps containing rule outputs for this execution
     * @throws FlowableObjectNotFoundException
     *             when the decision with given key does not exist.
     * @throws FlowableException
     *             when an error occurs while executing the decision.
     * @deprecated Use the createExecuteDecisionBuilder method instead
     */
    List<Map<String, Object>> executeDecisionByKeyAndTenantId(String decisionKey, Map<String, Object> inputVariables, String tenantId);

    /**
     * Execute a decision identified by it's key and tenant id. Expecting a single result.
     *
     * @param decisionKey
     *            the decision key, cannot be null
     * @param inputVariables
     *            map with input variables
     * @return Map containing rule output(s) for this execution
     * @throws FlowableObjectNotFoundException
     *             when the decision with given key does not exist.
     * @throws FlowableException
     *             when execution has multiple rule results or when an error occurs while executing the decision.
     * @deprecated Use the createExecuteDecisionBuilder method instead
     */
    Map<String, Object> executeDecisionByKeyAndTenantIdSingleResult(String decisionKey, Map<String, Object> inputVariables, String tenantId);

    /**
     * Execute a decision identified by it's key and tenant id.
     * Result contains audit trail.
     *
     * @param decisionKey
     *            the decision key, cannot be null
     * @param inputVariables
     *            map with input variables
     * @return the {@link DecisionExecutionAuditContainer} for this execution
     * @throws FlowableObjectNotFoundException
     *             when the decision with given key does not exist.
     * @throws FlowableException
     *             when an error occurs while executing the decision.
     * @deprecated Use the createExecuteDecisionBuilder method instead
     */
    DecisionExecutionAuditContainer executeDecisionByKeyAndTenantIdWithAuditTrail(String decisionKey, Map<String, Object> inputVariables, String tenantId);

    /**
     * Execute a decision identified by it's key and parent deployment id.
     *
     * @param decisionKey
     *            the decision key, cannot be null
     * @param inputVariables
     *            map with input variables
     * @return List with Maps containing rule outputs for this execution
     * @throws FlowableObjectNotFoundException
     *             when the decision with given key does not exist.
     * @throws FlowableException
     *             when an error occurs while executing the decision.
     * @deprecated Use the createExecuteDecisionBuilder method instead
     */
    List<Map<String, Object>> executeDecisionByKeyAndParentDeploymentId(String decisionKey, String parentDeploymentId, Map<String, Object> inputVariables);

    /**
     * Execute a decision identified by it's key and parent deployment id. Expecting a single result.
     *
     * @param decisionKey
     *            the decision key, cannot be null
     * @param inputVariables
     *            map with input variables
     * @return Map containing rule output(s) for this execution
     * @throws FlowableObjectNotFoundException
     *             when the decision with given key does not exist.
     * @throws FlowableException
     *             when execution has multiple rule results or when an error occurs while executing the decision.
     * @deprecated Use the createExecuteDecisionBuilder method instead
     */
    Map<String, Object> executeDecisionByKeyAndParentDeploymentIdSingleResult(String decisionKey, String parentDeploymentId, Map<String, Object> inputVariables);

    /**
     * Execute a decision identified by it's key and parent deployment id.
     * Result contains audit trail.
     *
     * @param decisionKey
     *            the decision key, cannot be null
     * @param inputVariables
     *            map with input variables
     * @return the {@link DecisionExecutionAuditContainer} for this execution
     * @throws FlowableObjectNotFoundException
     *             when the decision with given key does not exist.
     * @throws FlowableException
     *             when an error occurs while executing the decision.
     * @deprecated Use the createExecuteDecisionBuilder method instead
     */
    DecisionExecutionAuditContainer executeDecisionByKeyAndParentDeploymentIdWithAuditTrail(String decisionKey, String parentDeploymentId, Map<String, Object> inputVariables, String tenantId);

    /**
     * Execute a decision identified by it's key, parent deployment id and tenant id.
     *
     * @param decisionKey
     *            the decision key, cannot be null
     * @param inputVariables
     *            map with input variables
     * @return List with Maps containing rule outputs for this execution
     * @throws FlowableObjectNotFoundException
     *             when the decision with given key does not exist.
     * @throws FlowableException
     *             when an error occurs while executing the decision.
     * @deprecated Use the createExecuteDecisionBuilder method instead
     */
    List<Map<String, Object>> executeDecisionByKeyParentDeploymentIdAndTenantId(String decisionKey, String parentDeploymentId, Map<String, Object> inputVariables, String tenantId);

    /**
     * Execute a decision identified by it's key, parent deployment id and tenant id. Expecting a single result.
     *
     * @param decisionKey
     *            the decision key, cannot be null
     * @param inputVariables
     *            map with input variables
     * @return Map containing rule output(s) for this execution
     * @throws FlowableObjectNotFoundException
     *             when the decision with given key does not exist.
     * @throws FlowableException
     *             when execution has multiple rule results or when an error occurs while executing the decision.
     * @deprecated Use the createExecuteDecisionBuilder method instead
     */
    Map<String, Object> executeDecisionByKeyParentDeploymentIdAndTenantIdSingleResult(String decisionKey, String parentDeploymentId, Map<String, Object> inputVariables, String tenantId);

    /**
     * Execute a decision identified by it's key, parent deployment id and tenant id.
     * Result contains audit trail.
     *
     * @param decisionKey
     *            the decision key, cannot be null
     * @param inputVariables
     *            map with input variables
     * @return the {@link DecisionExecutionAuditContainer} for this execution
     * @throws FlowableObjectNotFoundException
     *             when the decision with given key does not exist.
     * @throws FlowableException
     *             when an error occurs while executing the decision.
     * @deprecated Use the createExecuteDecisionBuilder method instead
     */
    DecisionExecutionAuditContainer executeDecisionByKeyParentDeploymentIdAndTenantIdWithAuditTrail(String decisionKey, String parentDeploymentId, Map<String, Object> inputVariables, String tenantId);

}
