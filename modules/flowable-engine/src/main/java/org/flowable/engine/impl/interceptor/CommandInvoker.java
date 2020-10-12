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
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.AbstractCommandInterceptor;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandConfig;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.interceptor.CommandInterceptor;
import org.flowable.engine.FlowableEngineAgenda;
import org.flowable.engine.impl.agenda.AbstractOperation;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 */
public class CommandInvoker extends AbstractCommandInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandInvoker.class);

    protected AgendaOperationRunner agendaOperationRunner;

    public CommandInvoker(AgendaOperationRunner agendaOperationRunner) {
        this.agendaOperationRunner = agendaOperationRunner;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T execute(final CommandConfig config, final Command<T> command, CommandExecutor commandExecutor) {
        final CommandContext commandContext = Context.getCommandContext();
        
        FlowableEngineAgenda agenda = CommandContextUtil.getAgenda(commandContext);
        if (commandContext.isReused() && !agenda.isEmpty()) { // there is already an agenda loop being executed
            return (T) command.execute(commandContext);
            
        } else {

            // Execute the command.
            // This will produce operations that will be put on the agenda.
            agenda.planOperation(new Runnable() {
    
                @Override
                public void run() {
                    commandContext.setResult(command.execute(commandContext));
                }
            });
    
            // Run loop for agenda
            executeOperations(commandContext);
    
            // At the end, call the execution tree change listeners.
            // TODO: optimization: only do this when the tree has actually changed (ie check dbSqlSession).
            if (!commandContext.isReused() && CommandContextUtil.hasInvolvedExecutions(commandContext)) {
                agenda.planExecuteInactiveBehaviorsOperation();
                executeOperations(commandContext);
            }
    
            return (T) commandContext.getResult();
        }
    }

    protected void executeOperations(final CommandContext commandContext) {
        FlowableEngineAgenda agenda = CommandContextUtil.getAgenda(commandContext);
        while (!agenda.isEmpty()) {
            Runnable runnable = agenda.getNextOperation();
            executeOperation(commandContext, runnable);
        }
    }

    public void executeOperation(CommandContext commandContext, Runnable runnable) {
        if (runnable instanceof AbstractOperation) {
            AbstractOperation operation = (AbstractOperation) runnable;

            // Execute the operation if the operation has no execution (i.e. it's an operation not working on a process instance)
            // or the operation has an execution and it is not ended
            if (operation.getExecution() == null || !operation.getExecution().isEnded()) {

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Executing operation {}", operation.getClass());
                }

                agendaOperationRunner.executeOperation(commandContext, operation);

            }

        } else {

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Executing operation {}", runnable.getClass());
            }

            runnable.run();
        }
    }

    @Override
    public CommandInterceptor getNext() {
        return null;
    }

    @Override
    public void setNext(CommandInterceptor next) {
        throw new UnsupportedOperationException("CommandInvoker must be the last interceptor in the chain");
    }

    public AgendaOperationRunner getAgendaOperationRunner() {
        return agendaOperationRunner;
    }
    public void setAgendaOperationRunner(AgendaOperationRunner agendaOperationRunner) {
        this.agendaOperationRunner = agendaOperationRunner;
    }
}
