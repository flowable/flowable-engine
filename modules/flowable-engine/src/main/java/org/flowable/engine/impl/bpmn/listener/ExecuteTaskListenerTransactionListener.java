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
package org.flowable.engine.impl.bpmn.listener;

import org.flowable.common.engine.impl.cfg.TransactionListener;
import org.flowable.common.engine.impl.cfg.TransactionPropagation;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandConfig;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.delegate.TransactionDependentTaskListener;

/**
 * A {@link TransactionListener} that invokes an {@link ExecutionListener}.
 * 
 * @author Joram Barrez
 */
public class ExecuteTaskListenerTransactionListener implements TransactionListener {

    protected TransactionDependentTaskListener listener;
    protected TransactionDependentTaskListenerExecutionScope scope;
    protected CommandExecutor commandExecutor;

    public ExecuteTaskListenerTransactionListener(TransactionDependentTaskListener listener,
            TransactionDependentTaskListenerExecutionScope scope, CommandExecutor commandExecutor) {
        this.listener = listener;
        this.scope = scope;
        this.commandExecutor = commandExecutor;
    }

    @Override
    public void execute(CommandContext commandContext) {
        CommandConfig commandConfig = new CommandConfig(false, TransactionPropagation.REQUIRES_NEW);
        commandExecutor.execute(commandConfig, new Command<Void>() {
            @Override
            public Void execute(CommandContext commandContext) {
                listener.notify(scope.getProcessInstanceId(), scope.getExecutionId(), scope.getTask(),
                        scope.getExecutionVariables(), scope.getCustomPropertiesMap());
                return null;
            }
        });
    }

}
