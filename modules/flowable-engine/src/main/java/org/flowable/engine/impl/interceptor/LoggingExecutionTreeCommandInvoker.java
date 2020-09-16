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
package org.flowable.engine.impl.interceptor;

import org.flowable.common.engine.impl.agenda.AgendaOperationRunner;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.debug.ExecutionTreeUtil;
import org.flowable.engine.impl.agenda.AbstractOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 */
public class LoggingExecutionTreeCommandInvoker extends CommandInvoker {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingExecutionTreeCommandInvoker.class);

    public LoggingExecutionTreeCommandInvoker(AgendaOperationRunner agendaOperationRunner) {
        super(agendaOperationRunner);
    }

    @Override
    public void executeOperation(CommandContext commandContext, Runnable runnable) {
        if (runnable instanceof AbstractOperation) {
            AbstractOperation operation = (AbstractOperation) runnable;

            if (operation.getExecution() != null) {
                LOGGER.info("Execution tree while executing operation {}:", operation.getClass());
                LOGGER.info("{}", System.lineSeparator() + ExecutionTreeUtil.buildExecutionTree(operation.getExecution()));
            }

        }

        super.executeOperation(commandContext, runnable);
    }

}
