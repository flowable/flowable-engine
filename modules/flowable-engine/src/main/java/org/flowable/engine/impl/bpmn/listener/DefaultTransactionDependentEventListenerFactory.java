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
import org.flowable.engine.common.api.delegate.event.TransactionFlowableEventListener;
import org.flowable.engine.common.impl.cfg.TransactionListener;
import org.flowable.engine.common.impl.transaction.TransactionDependentFactory;
import org.flowable.engine.impl.bpmn.helper.DelegateExecutableTransactionEventListener;

public class DefaultTransactionDependentEventListenerFactory implements TransactionDependentFactory {
    
    @Override
    public TransactionListener createFlowableTransactionEventListener(TransactionFlowableEventListener listener, FlowableEvent event) {
        TransactionFlowableEventListener executionListener = new DelegateExecutableTransactionEventListener(listener);
        return new ExecuteEventListenerTransactionListener(executionListener, event);
    }
}
