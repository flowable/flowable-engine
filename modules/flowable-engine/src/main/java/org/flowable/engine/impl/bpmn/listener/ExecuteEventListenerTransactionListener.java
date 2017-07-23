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

import org.flowable.engine.common.api.delegate.event.FlowableEvent;
import org.flowable.engine.common.api.delegate.event.TransactionDependentFlowableEventListener;
import org.flowable.engine.common.impl.cfg.TransactionListener;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.delegate.ExecutionListener;

/**
 * A {@link TransactionListener} that invokes an {@link ExecutionListener}.
 *
 * @author Joram Barrez
 */
public class ExecuteEventListenerTransactionListener implements TransactionListener {

    protected TransactionDependentFlowableEventListener listener;
    protected FlowableEvent flowableEvent;

    public ExecuteEventListenerTransactionListener(TransactionDependentFlowableEventListener listener,
                                                   FlowableEvent flowableEvent) {
        this.listener = listener;
        this.flowableEvent = flowableEvent;
    }

    @Override
    public void execute(CommandContext commandContext) {
        listener.onEvent(flowableEvent);
//        CommandExecutor commandExecutor = CommandContextUtil.getProcessEngineConfiguration(commandContext).getCommandExecutor();
//        CommandConfig commandConfig = new CommandConfig(false, TransactionPropagation.REQUIRES_NEW);
//        commandExecutor.execute(commandConfig, new Command<Void>() {
//            public Void execute(CommandContext commandContext) {
//                listener.onEvent(flowableEvent);
//                return null;
//            }
//        });
    }

}
