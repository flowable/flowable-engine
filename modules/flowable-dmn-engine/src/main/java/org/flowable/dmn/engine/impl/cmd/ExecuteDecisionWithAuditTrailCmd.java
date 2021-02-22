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

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.dmn.api.DecisionExecutionAuditContainer;
import org.flowable.dmn.api.ExecuteDecisionContext;
import org.flowable.dmn.engine.impl.ExecuteDecisionBuilderImpl;
import org.flowable.dmn.model.DmnDefinition;

/**
 * @author Tijs Rademakers
 * @author Yvo Swillens
 */
public class ExecuteDecisionWithAuditTrailCmd extends AbstractExecuteDecisionCmd implements Command<Void> {
    
    private static final long serialVersionUID = 1L;

    public ExecuteDecisionWithAuditTrailCmd(ExecuteDecisionBuilderImpl decisionBuilder) {
        super(decisionBuilder);
    }

    public ExecuteDecisionWithAuditTrailCmd(ExecuteDecisionContext executeDecisionContext) {
        super(executeDecisionContext);
    }
    
    public ExecuteDecisionWithAuditTrailCmd(String decisionKey, Map<String, Object> variables) {
        super(decisionKey, variables);
    }

    public ExecuteDecisionWithAuditTrailCmd(String decisionKey, String parentDeploymentId, Map<String, Object> variables) {
        this(decisionKey, variables);
        executeDecisionContext.setParentDeploymentId(parentDeploymentId);
    }

    public ExecuteDecisionWithAuditTrailCmd(String decisionKey, String parentDeploymentId, Map<String, Object> variables, String tenantId) {
        this(decisionKey, parentDeploymentId, variables);
        executeDecisionContext.setTenantId(tenantId);
    }

    @Override
    public Void execute(CommandContext commandContext) {
        if (executeDecisionContext.getDecisionKey() == null) {
            throw new FlowableIllegalArgumentException("decisionKey is null");
        }

        DmnDefinition definition;
        try {
            definition = resolveDefinition();
        } catch (FlowableException e) {
            DecisionExecutionAuditContainer container = new DecisionExecutionAuditContainer();
            container.setFailed();
            container.setExceptionMessage(e.getMessage());

            executeDecisionContext.setDecisionExecution(container);
            return null;
        }

        execute(commandContext, definition);

        return null;
    }

}
