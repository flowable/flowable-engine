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

import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.dmn.api.DecisionExecutionAuditContainer;
import org.flowable.dmn.api.DmnDecisionTable;
import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.dmn.engine.impl.ExecuteDecisionBuilderImpl;
import org.flowable.dmn.engine.impl.util.CommandContextUtil;
import org.flowable.dmn.model.Decision;
/**
 * @author Tijs Rademakers
 * @author Yvo Swillens
 */
public class ExecuteDecisionCmd extends AbstractExecuteDecisionCmd implements Command<List<Map<String, Object>>> {
    
    private static final long serialVersionUID = 1L;

    public ExecuteDecisionCmd(ExecuteDecisionBuilderImpl decisionBuilder) {
        super(decisionBuilder);
    }
    
    public ExecuteDecisionCmd(String decisionKey, Map<String, Object> variables) {
        super(decisionKey, variables);
    }

    public ExecuteDecisionCmd(String decisionKey, String parentDeploymentId, Map<String, Object> variables) {
        this(decisionKey, variables);
        executeDecisionInfo.setParentDeploymentId(parentDeploymentId);
    }

    public ExecuteDecisionCmd(String decisionKey, String parentDeploymentId, Map<String, Object> variables, String tenantId) {
        this(decisionKey, parentDeploymentId, variables);
        executeDecisionInfo.setTenantId(tenantId);
    }

    @Override
    public List<Map<String, Object>> execute(CommandContext commandContext) {
        if (getDecisionKey() == null) {
            throw new FlowableIllegalArgumentException("decisionKey is null");
        }

        DmnEngineConfiguration dmnEngineConfiguration = CommandContextUtil.getDmnEngineConfiguration();
        DmnDecisionTable decisionTable = resolveDecisionTable(dmnEngineConfiguration.getDeploymentManager());
        Decision decision = resolveDecision(dmnEngineConfiguration.getDeploymentManager(), decisionTable);

        DecisionExecutionAuditContainer executionResult = dmnEngineConfiguration.getRuleEngineExecutor().execute(decision, executeDecisionInfo);

        if (executionResult != null) {
            return executionResult.getDecisionResult();
        } else {
            return null;
        }
    }

}
