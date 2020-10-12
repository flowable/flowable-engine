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
package org.flowable.dmn.engine.impl.interceptor;

import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.AbstractCommandInterceptor;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandConfig;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.interceptor.CommandInterceptor;
import org.flowable.dmn.engine.impl.agenda.DmnEngineAgenda;
import org.flowable.dmn.engine.impl.util.CommandContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yvo Swillens
 */
public class DmnCommandInvoker extends AbstractCommandInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DmnCommandInvoker.class);

    @SuppressWarnings("unchecked")
    @Override
    public <T> T execute(final CommandConfig config, final Command<T> command, CommandExecutor commandExecutor) {
        final CommandContext commandContext = Context.getCommandContext();
        final DmnEngineAgenda agenda = CommandContextUtil.getAgenda(commandContext);
        if (commandContext.isReused() && !agenda.isEmpty()) {
            commandContext.setResult(command.execute(commandContext));
        } else {
            agenda.planOperation(new Runnable() {
                @Override
                public void run() {
                    commandContext.setResult(command.execute(commandContext));
                }
            });

            executeOperations(commandContext, true); // true -> always store the case instance id for the regular operation loop, even if it's a no-op operation.
        }
        
        return (T) commandContext.getResult();
    }

    protected void executeOperations(CommandContext commandContext, boolean isStoreCaseInstanceIdOfNoOperation) {
        DmnEngineAgenda agenda = CommandContextUtil.getAgenda(commandContext);
        while (!agenda.isEmpty()) {
            Runnable runnable = agenda.getNextOperation();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Executing agenda operation {}", runnable);
            }
            runnable.run();
        }
    }

    @Override
    public void setNext(CommandInterceptor next) {
        throw new UnsupportedOperationException("CommandInvoker must be the last interceptor in the chain");
    }
    
}
