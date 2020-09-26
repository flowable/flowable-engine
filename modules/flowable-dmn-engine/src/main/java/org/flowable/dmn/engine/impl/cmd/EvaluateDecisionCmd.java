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

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.dmn.api.ExecuteDecisionContext;
import org.flowable.dmn.engine.impl.ExecuteDecisionBuilderImpl;
import org.flowable.dmn.engine.impl.util.CommandContextUtil;
import org.flowable.dmn.model.Decision;
import org.flowable.dmn.model.DecisionService;
import org.flowable.dmn.model.DmnDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yvo Swillens
 */
public class EvaluateDecisionCmd extends AbstractExecuteDecisionCmd implements Command<Void> {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(EvaluateDecisionCmd.class);

    public EvaluateDecisionCmd(ExecuteDecisionBuilderImpl decisionBuilder) {
        super(decisionBuilder);
    }

    public EvaluateDecisionCmd(String decisionKey, Map<String, Object> variables) {
        super(decisionKey, variables);
    }

    public EvaluateDecisionCmd(String decisionKey, String parentDeploymentId, Map<String, Object> variables) {
        this(decisionKey, variables);
        executeDecisionContext.setParentDeploymentId(parentDeploymentId);
    }

    public EvaluateDecisionCmd(String decisionKey, String parentDeploymentId, Map<String, Object> variables, String tenantId) {
        this(decisionKey, parentDeploymentId, variables);
        executeDecisionContext.setTenantId(tenantId);
    }

    public EvaluateDecisionCmd(ExecuteDecisionContext executeDecisionContext) {
        super(executeDecisionContext);
    }

    @Override
    public Void execute(CommandContext commandContext) {
        if (executeDecisionContext.getDecisionKey() == null) {
            throw new FlowableIllegalArgumentException("decisionKey is null");
        }

        DmnDefinition definition = resolveDefinition();

        execute(commandContext, definition);

        return null;
    }

    @Override
    protected void execute(CommandContext commandContext, DmnDefinition definition) {
        DecisionService decisionService = definition.getDecisionServiceById(executeDecisionContext.getDecisionKey());

        // executing a DecisionService is the default but will fallback to Decision
        if (decisionService != null) {
            executeDecisionContext.setDmnElement(decisionService);

            CommandContextUtil.getAgenda(commandContext).planExecuteDecisionServiceOperation(executeDecisionContext, decisionService);
        } else {
            Decision decision = definition.getDecisionById(executeDecisionContext.getDecisionKey());
            executeDecisionContext.setDmnElement(decision);

            CommandContextUtil.getAgenda(commandContext).planExecuteDecisionOperation(executeDecisionContext, decision);
        }
    }
}
