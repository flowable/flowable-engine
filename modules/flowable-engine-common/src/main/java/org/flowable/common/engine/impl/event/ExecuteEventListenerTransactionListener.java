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
package org.flowable.common.engine.impl.event;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.common.engine.impl.cfg.TransactionListener;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExecuteEventListenerTransactionListener implements TransactionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecuteEventListenerTransactionListener.class);
    
    protected FlowableEventListener listener;
    protected FlowableEvent flowableEvent;

    public ExecuteEventListenerTransactionListener(FlowableEventListener listener, FlowableEvent flowableEvent) {
        this.listener = listener;
        this.flowableEvent = flowableEvent;
    }

    @Override
    public void execute(CommandContext commandContext) {
        try {
            listener.onEvent(flowableEvent);
        } catch (Throwable t) {
            if (listener.isFailOnException()) {
                throw new FlowableException("Exception while executing event-listener", t);
            } else {
                // Ignore the exception and continue notifying remaining listeners. The listener
                // explicitly states that the exception should not bubble up
                LOGGER.warn("Exception while executing event-listener, which was ignored", t);
            }
        }
    }
}
