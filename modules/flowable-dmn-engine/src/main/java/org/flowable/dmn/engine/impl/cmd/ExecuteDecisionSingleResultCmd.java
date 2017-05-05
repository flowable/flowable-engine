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

import org.flowable.dmn.api.DmnDecisionTable;
import org.flowable.dmn.api.RuleEngineExecutionResult;
import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.dmn.engine.impl.interceptor.Command;
import org.flowable.dmn.engine.impl.interceptor.CommandContext;
import org.flowable.dmn.model.Decision;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.FlowableIllegalArgumentException;

import java.io.Serializable;
import java.util.Map;

/**
 * @author Tijs Rademakers
 * @author Yvo Swillens
 */
public class ExecuteDecisionSingleResultCmd extends AbstractExecuteDecisionCmd implements Command<Map<String, Object>> {

    public ExecuteDecisionSingleResultCmd(String decisionKey, Map<String, Object> variables) {
        this.decisionKey = decisionKey;
        this.variables = variables;
    }

    public ExecuteDecisionSingleResultCmd(String decisionKey, String parentDeploymentId, Map<String, Object> variables) {
        this(decisionKey, variables);
        this.parentDeploymentId = parentDeploymentId;
    }

    public ExecuteDecisionSingleResultCmd(String decisionKey, String parentDeploymentId, Map<String, Object> variables, String tenantId) {
        this(decisionKey, parentDeploymentId, variables);
        this.tenantId = tenantId;
    }

    public Map<String, Object> execute(CommandContext commandContext) {
        if (decisionKey == null) {
            throw new FlowableIllegalArgumentException("decisionKey is null");
        }
        
        DmnEngineConfiguration dmnEngineConfiguration = commandContext.getDmnEngineConfiguration();
        DmnDecisionTable decisionTable = resolveDecisionTable(dmnEngineConfiguration.getDeploymentManager());
        Decision decision = resolveDecision(dmnEngineConfiguration.getDeploymentManager(), decisionTable);

        RuleEngineExecutionResult executionResult = dmnEngineConfiguration.getRuleEngineExecutor().execute(decision, variables,
                dmnEngineConfiguration.getCustomExpressionFunctions(), dmnEngineConfiguration.getCustomPropertyHandlers());

        Map<String, Object> decisionResult = null;
        if (executionResult != null && executionResult.getDecisionResult() != null && !executionResult.getDecisionResult().isEmpty()) {
            if (executionResult.getDecisionResult().size() > 1) {
                throw new FlowableException("more than one result");
            }
            decisionResult = executionResult.getDecisionResult().get(0);
        }

        return decisionResult;
    }

}
