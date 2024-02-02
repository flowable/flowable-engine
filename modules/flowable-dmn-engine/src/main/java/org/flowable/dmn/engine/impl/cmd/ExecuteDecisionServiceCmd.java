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
import org.flowable.dmn.model.DecisionService;
import org.flowable.dmn.model.DmnDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 * @author Yvo Swillens
 */
public class ExecuteDecisionServiceCmd extends AbstractExecuteDecisionCmd implements Command<Void> {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(ExecuteDecisionServiceCmd.class);

    public ExecuteDecisionServiceCmd(ExecuteDecisionBuilderImpl decisionBuilder) {
        super(decisionBuilder);
    }

    public ExecuteDecisionServiceCmd(String decisionKey, Map<String, Object> variables) {
        super(decisionKey, variables);
    }

    public ExecuteDecisionServiceCmd(String decisionKey, String parentDeploymentId, Map<String, Object> variables) {
        this(decisionKey, variables);
        executeDecisionContext.setParentDeploymentId(parentDeploymentId);
    }

    public ExecuteDecisionServiceCmd(String decisionKey, String parentDeploymentId, Map<String, Object> variables, String tenantId) {
        this(decisionKey, parentDeploymentId, variables);
        executeDecisionContext.setTenantId(tenantId);
    }

    public ExecuteDecisionServiceCmd(ExecuteDecisionContext executeDecisionContext) {
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

        if (decisionService == null) {
            throw new FlowableIllegalArgumentException("no decision service with id: '" + executeDecisionContext.getDecisionKey() + "' found in definition");
        }

        executeDecisionContext.setDmnElement(decisionService);
        CommandContextUtil.getAgenda(commandContext).planExecuteDecisionServiceOperation(executeDecisionContext, decisionService);
    }
}
