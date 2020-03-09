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
package org.flowable.dmn.engine.impl.agenda;

import org.flowable.common.engine.impl.agenda.AbstractAgenda;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.dmn.api.ExecuteDecisionContext;
import org.flowable.dmn.engine.impl.agenda.operation.DmnOperation;
import org.flowable.dmn.engine.impl.agenda.operation.ExecuteDecisionOperation;
import org.flowable.dmn.engine.impl.agenda.operation.ExecuteDecisionServiceOperation;
import org.flowable.dmn.model.Decision;
import org.flowable.dmn.model.DecisionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yvo Swillens
 */
public class DefaultDmnEngineAgenda extends AbstractAgenda implements DmnEngineAgenda {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDmnEngineAgenda.class);

    public DefaultDmnEngineAgenda(CommandContext commandContext) {
        super(commandContext);
    }

    public void addOperation(DmnOperation operation) {
        operations.addLast(operation);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Planned {}", operation);
        }
    }

    @Override
    public void planExecuteDecisionServiceOperation(ExecuteDecisionContext executeDecisionContext, DecisionService decisionService) {
        addOperation(new ExecuteDecisionServiceOperation(commandContext, executeDecisionContext, decisionService));
    }

    @Override
    public void planExecuteDecisionOperation(ExecuteDecisionContext executeDecisionContext, Decision decision) {
        addOperation(new ExecuteDecisionOperation(commandContext, executeDecisionContext, decision));
    }
}
