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

/**
 * Service for executing DMN decisions
 *
 * @author Tijs Rademakers
 * @author Yvo Swillens
 */
public interface DmnDecisionService {

    /**
     * Create a builder to execute a decision or decision service.
     *
     * @return the {@link ExecuteDecisionBuilder} build
     */
    ExecuteDecisionBuilder createExecuteDecisionBuilder();

    /**
     * Execute a single decision or a decision service depending on the provided decision key
     *
     * @return a Map with the decision(s) result(s). When multiple output decisions use the same
     * variable IDs the last occurrence will be present in the Map.
     * An {@link FlowableException} will be thrown when multiple rules were hit.
     */
    Map<String, Object> executeWithSingleResult(ExecuteDecisionBuilder builder);

    /**
     * Execute a single decision or a decision service depending on the provided decision key
     *
     * @return the {@link DecisionExecutionAuditContainer} when a decision was executed
     * or a {@link DecisionServiceExecutionAuditContainer} when a decision service was executed
     */
    DecisionExecutionAuditContainer executeWithAuditTrail(ExecuteDecisionBuilder builder);

    /**
     * Execute a single decision
     *
     * @return a List with decision result(s)
     */
    List<Map<String, Object>> executeDecision(ExecuteDecisionBuilder builder);

    /**
     * Execute a decision service
     *
     * @return a Map with decision result(s) per output decision
     */
    Map<String, List<Map<String, Object>>> executeDecisionService(ExecuteDecisionBuilder builder);

    /**
     * Execute a single decision
     *
     * @return a Map with the decision result.
     * An {@link FlowableException} will be thrown when multiple rules were hit.
     */
    Map<String, Object> executeDecisionWithSingleResult(ExecuteDecisionBuilder builder);

    /**
     * Execute a decision service
     *
     * @return a Map with the decision service result.
     * An {@link FlowableException} will be thrown when multiple rules were hit.
     */
    Map<String, Object> executeDecisionServiceWithSingleResult(ExecuteDecisionBuilder builder);

    /**
     * Execute a single decision
     *
     * @return a List with decision result(s)
     */
    DecisionExecutionAuditContainer executeDecisionWithAuditTrail(ExecuteDecisionBuilder builder);

    /**
     * Execute a decision service
     *
     * @return a {@link DecisionServiceExecutionAuditContainer} when a decision service was executed
     */
    DecisionServiceExecutionAuditContainer executeDecisionServiceWithAuditTrail(ExecuteDecisionBuilder builder);

}
