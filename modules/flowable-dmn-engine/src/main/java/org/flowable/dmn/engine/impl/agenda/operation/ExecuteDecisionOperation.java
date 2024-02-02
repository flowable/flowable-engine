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
package org.flowable.dmn.engine.impl.agenda.operation;

import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.dmn.api.DecisionExecutionAuditContainer;
import org.flowable.dmn.api.DecisionServiceExecutionAuditContainer;
import org.flowable.dmn.api.ExecuteDecisionContext;
import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.dmn.engine.impl.util.CommandContextUtil;
import org.flowable.dmn.model.Decision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExecuteDecisionOperation extends DmnOperation {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecuteDecisionOperation.class);

    protected final Decision decision;
    protected ExecuteDecisionContext executeDecisionContext;

    public ExecuteDecisionOperation(CommandContext commandContext, ExecuteDecisionContext executeDecisionContext, Decision decision) {
        super(commandContext);
        this.executeDecisionContext = executeDecisionContext;
        this.decision = decision;
    }

    @Override
    public void run() {
        DmnEngineConfiguration dmnEngineConfiguration = CommandContextUtil.getDmnEngineConfiguration();
        DecisionExecutionAuditContainer auditContainer = dmnEngineConfiguration
            .getRuleEngineExecutor()
            .execute(decision, executeDecisionContext);

        if (!executeDecisionContext.getDmnElement().equals(decision)) {
            // is part of a decision service execution
            ((DecisionServiceExecutionAuditContainer) executeDecisionContext.getDecisionExecution()).addChildDecisionExecution(decision.getId(), auditContainer);
        } else {
            executeDecisionContext.setDecisionExecution(auditContainer);
        }
    }
}
