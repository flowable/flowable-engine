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

import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.FlowableObjectNotFoundException;

import java.util.Map;

/**
 * Service for executing DMN decisions (decision tables)
 *
 * @author Tijs Rademakers
 * @author Yvo Swillens
 */
public interface DmnRuleService {

  /**
   * Execute a decision identified by it's key.
   *
   * @param  decisionKey      the decision key, cannot be null
   * @param  inputVariables   map with input variables
   * @return                  the {@link RuleEngineExecutionResult} for this execution
   * @throws FlowableObjectNotFoundException
   *            when the decision with given key does not exist.
   * @throws FlowableException
   *           when an error occurs while executing the decision.
   */
  RuleEngineExecutionResult executeDecisionByKey(String decisionKey, Map<String, Object> inputVariables);

  /**
   * Execute a decision identified by it's key and tenant id.
   *
   * @param  decisionKey      the decision key, cannot be null
   * @param  inputVariables   map with input variables
   * @param  tenantId         the tenant id
   * @return                  the {@link RuleEngineExecutionResult} for this execution
   * @throws FlowableObjectNotFoundException
   *            when the decision with given key and tenant id does not exist.
   * @throws FlowableException
   *           when an error occurs while executing the decision.
   */
  RuleEngineExecutionResult executeDecisionByKeyAndTenantId(String decisionKey, Map<String, Object> inputVariables, String tenantId);

  /**
   * Execute a decision identified by it's key and parent deployment id.
   *
   * @param  decisionKey          the decision key, cannot be null
   * @param  parentDeploymentId   the parent deployment id
   * @param  inputVariables       map with input variables
   * @return                      the {@link RuleEngineExecutionResult} for this execution
   * @throws FlowableObjectNotFoundException
   *            when the decision with given key and parent deployment id does not exist.
   * @throws FlowableException
   *           when an error occurs while executing the decision.
   */
  RuleEngineExecutionResult executeDecisionByKeyAndParentDeploymentId(String decisionKey, String parentDeploymentId, Map<String, Object> inputVariables);

  /**
   * Execute a decision identified by it's key and parent deployment id.
   *
   * @param  decisionKey          the decision key, cannot be null
   * @param  parentDeploymentId   the parent deployment id
   * @param  inputVariables       map with input variables
   * @param  tenantId             the tenant id
   * @return                      the {@link RuleEngineExecutionResult} for this execution
   * @throws FlowableObjectNotFoundException
   *            when the decision with given key and parent deployment id and tenant id does not exist.
   * @throws FlowableException
   *           when an error occurs while executing the decision.
   */
  RuleEngineExecutionResult executeDecisionByKeyParentDeploymentIdAndTenantId(String decisionKey, String parentDeploymentId, Map<String, Object> inputVariables, String tenantId);
}
