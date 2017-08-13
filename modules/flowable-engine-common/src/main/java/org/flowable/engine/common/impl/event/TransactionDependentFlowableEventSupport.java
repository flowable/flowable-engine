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
package org.flowable.engine.common.impl.event;

import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.api.delegate.event.FlowableEvent;
import org.flowable.engine.common.api.delegate.event.FlowableEventType;
import org.flowable.engine.common.api.delegate.event.TransactionDependentFlowableEventListener;
import org.flowable.engine.common.impl.cfg.TransactionContext;
import org.flowable.engine.common.impl.cfg.TransactionListener;
import org.flowable.engine.common.impl.cfg.TransactionState;
import org.flowable.engine.common.impl.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Class that allows adding and removing event listeners and dispatching events to the appropriate
 * listeners.
 *
 * @author Frederik Heremans
 */
public class TransactionDependentFlowableEventSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionDependentFlowableEventSupport.class);

    protected List<TransactionDependentFlowableEventListener> eventListeners;
    protected Map<FlowableEventType, List<TransactionDependentFlowableEventListener>> typedListeners;

    public TransactionDependentFlowableEventSupport() {
        eventListeners = new CopyOnWriteArrayList<>();
        typedListeners = new HashMap<>();
    }

    public synchronized void addEventListener(TransactionDependentFlowableEventListener listenerToAdd) {
        if (listenerToAdd == null) {
            throw new FlowableIllegalArgumentException("Listener cannot be null.");
        }
        if (!eventListeners.contains(listenerToAdd)) {
            eventListeners.add(listenerToAdd);
        }
    }

    public synchronized void addEventListener(TransactionDependentFlowableEventListener listenerToAdd, FlowableEventType... types) {
        if (listenerToAdd == null) {
            throw new FlowableIllegalArgumentException("Listener cannot be null.");
        }

        if (types == null || types.length == 0) {
            addEventListener(listenerToAdd);

        } else {
            for (FlowableEventType type : types) {
                addTypedEventListener(listenerToAdd, type);
            }
        }
    }

    public void removeEventListener(TransactionDependentFlowableEventListener listenerToRemove) {
        eventListeners.remove(listenerToRemove);

        for (List<TransactionDependentFlowableEventListener> listeners : typedListeners.values()) {
            listeners.remove(listenerToRemove);
        }
    }

    public void dispatchEvent(FlowableEvent event) {
        if (event == null) {
            throw new FlowableIllegalArgumentException("Event cannot be null.");
        }

        if (event.getType() == null) {
            throw new FlowableIllegalArgumentException("Event type cannot be null.");
        }

        // Call global listeners
        if (!eventListeners.isEmpty()) {
            for (TransactionDependentFlowableEventListener listener : eventListeners) {
                dispatchEvent(event, listener);
            }
        }

        // Call typed listeners, if any
        List<TransactionDependentFlowableEventListener> typed = typedListeners.get(event.getType());
        if (typed != null && !typed.isEmpty()) {
            for (TransactionDependentFlowableEventListener listener : typed) {
                dispatchEvent(event, listener);
            }
        }
    }

    protected void dispatchEvent(FlowableEvent event, TransactionDependentFlowableEventListener listener) {

        TransactionListener transactionListener = Context.getCommandContext().getCurrentEngineConfiguration().
                getTransactionDependentFactory().createFlowableTransactionEventListener(listener, event);

        TransactionContext transactionContext = Context.getTransactionContext();
        if (listener.getOnTransaction().equals(TransactionState.COMMITTING.name())) {
            transactionContext.addTransactionListener(TransactionState.COMMITTING, transactionListener);
        } else if (listener.getOnTransaction().equals(TransactionState.COMMITTED.name())) {
            transactionContext.addTransactionListener(TransactionState.COMMITTED, transactionListener);
        } else if (listener.getOnTransaction().equals(TransactionState.ROLLINGBACK.name())) {
            transactionContext.addTransactionListener(TransactionState.ROLLINGBACK, transactionListener);
        } else if (listener.getOnTransaction().equals(TransactionState.ROLLED_BACK.name())) {
            transactionContext.addTransactionListener(TransactionState.ROLLED_BACK, transactionListener);
        }


    }


    protected synchronized void addTypedEventListener(TransactionDependentFlowableEventListener listener, FlowableEventType type) {
        List<TransactionDependentFlowableEventListener> listeners = typedListeners.get(type);
        if (listeners == null) {
            // Add an empty list of listeners for this type
            listeners = new CopyOnWriteArrayList<>();
            typedListeners.put(type, listeners);
        }

        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
}
