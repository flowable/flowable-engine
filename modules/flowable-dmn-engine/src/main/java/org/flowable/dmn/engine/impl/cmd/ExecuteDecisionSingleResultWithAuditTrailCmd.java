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
package org.flowable.dmn.engine.impl.cmd;

import java.util.Map;

import org.flowable.dmn.api.DmnDecisionTable;
import org.flowable.dmn.api.RuleEngineExecutionResult;
import org.flowable.dmn.api.RuleEngineExecutionSingleResult;
import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.dmn.engine.impl.util.CommandContextUtil;
import org.flowable.dmn.model.Decision;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.impl.interceptor.Command;
import org.flowable.engine.common.impl.interceptor.CommandContext;

/**
 * @author Tijs Rademakers
 * @author Yvo Swillens
 */
public class ExecuteDecisionSingleResultWithAuditTrailCmd extends AbstractExecuteDecisionCmd implements Command<RuleEngineExecutionSingleResult> {

    public ExecuteDecisionSingleResultWithAuditTrailCmd(String decisionKey, Map<String, Object> variables) {
        this.decisionKey = decisionKey;
        this.variables = variables;
    }

    public ExecuteDecisionSingleResultWithAuditTrailCmd(String decisionKey, String parentDeploymentId, Map<String, Object> variables) {
        this(decisionKey, variables);
        this.parentDeploymentId = parentDeploymentId;
    }

    public ExecuteDecisionSingleResultWithAuditTrailCmd(String decisionKey, String parentDeploymentId, Map<String, Object> variables, String tenantId) {
        this(decisionKey, parentDeploymentId, variables);
        this.tenantId = tenantId;
    }

    public RuleEngineExecutionSingleResult execute(CommandContext commandContext) {
        if (decisionKey == null) {
            throw new FlowableIllegalArgumentException("decisionKey is null");
        }

        DmnEngineConfiguration dmnEngineConfiguration = CommandContextUtil.getDmnEngineConfiguration();
        DmnDecisionTable decisionTable = resolveDecisionTable(dmnEngineConfiguration.getDeploymentManager());
        Decision decision = resolveDecision(dmnEngineConfiguration.getDeploymentManager(), decisionTable);

        RuleEngineExecutionResult executionResult = dmnEngineConfiguration.getRuleEngineExecutor().execute(decision, variables,
                dmnEngineConfiguration.getCustomExpressionFunctions(), dmnEngineConfiguration.getCustomPropertyHandlers());

        Map<String, Object> decisionResult = null;

        if (executionResult != null) {
            if (executionResult.getDecisionResult() != null && !executionResult.getDecisionResult().isEmpty()) {
                if (executionResult.getDecisionResult().size() > 1) {
                    throw new FlowableException("more than one result");
                }
                decisionResult = executionResult.getDecisionResult().get(0);
            }
            if (executionResult.getAuditTrail() != null) {
                executionResult.getAuditTrail().setDmnDeploymentId(decisionTable.getDeploymentId());
            }
        }

        RuleEngineExecutionSingleResult executionSingleResult = new RuleEngineExecutionSingleResult(decisionResult, executionResult.getAuditTrail());

        return executionSingleResult;
    }

}
