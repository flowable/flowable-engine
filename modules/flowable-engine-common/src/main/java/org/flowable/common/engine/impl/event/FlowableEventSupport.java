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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.common.engine.api.delegate.event.FlowableEventType;
import org.flowable.common.engine.impl.cfg.TransactionContext;
import org.flowable.common.engine.impl.cfg.TransactionState;
import org.flowable.common.engine.impl.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that allows adding and removing event listeners and dispatching events to the appropriate listeners.
 * 
 * @author Frederik Heremans
 */
public class FlowableEventSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowableEventSupport.class);

    protected List<FlowableEventListener> eventListeners;
    protected Map<FlowableEventType, List<FlowableEventListener>> typedListeners;

    public FlowableEventSupport() {
        eventListeners = new CopyOnWriteArrayList<>();
        typedListeners = new HashMap<>();
    }

    public synchronized void addEventListener(FlowableEventListener listenerToAdd) {
        if (listenerToAdd == null) {
            throw new FlowableIllegalArgumentException("Listener cannot be null.");
        }
        if (!eventListeners.contains(listenerToAdd)) {
            eventListeners.add(listenerToAdd);
        }
    }

    public synchronized void addEventListener(FlowableEventListener listenerToAdd, FlowableEventType... types) {
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

    public void removeEventListener(FlowableEventListener listenerToRemove) {
        eventListeners.remove(listenerToRemove);

        for (List<FlowableEventListener> listeners : typedListeners.values()) {
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
            for (FlowableEventListener listener : eventListeners) {
                dispatchEvent(event, listener);
            }
        }

        // Call typed listeners, if any
        List<FlowableEventListener> typed = typedListeners.get(event.getType());
        if (typed != null && !typed.isEmpty()) {
            for (FlowableEventListener listener : typed) {
                dispatchEvent(event, listener);
            }
        }
    }

    protected void dispatchEvent(FlowableEvent event, FlowableEventListener listener) {
        if (listener.isFireOnTransactionLifecycleEvent()) {
            dispatchTransactionEventListener(event, listener);
        } else {
            dispatchNormalEventListener(event, listener);
        }
    }

    protected void dispatchNormalEventListener(FlowableEvent event, FlowableEventListener listener) {
        try {
            listener.onEvent(event);
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

    protected void dispatchTransactionEventListener(FlowableEvent event, FlowableEventListener listener) {
        TransactionContext transactionContext = Context.getTransactionContext();
        if (transactionContext == null) {
            return;
        }
        
        ExecuteEventListenerTransactionListener transactionListener = new ExecuteEventListenerTransactionListener(listener, event); 
        if (listener.getOnTransaction().equalsIgnoreCase(TransactionState.COMMITTING.name())) {
            transactionContext.addTransactionListener(TransactionState.COMMITTING, transactionListener);
            
        } else if (listener.getOnTransaction().equalsIgnoreCase(TransactionState.COMMITTED.name())) {
            transactionContext.addTransactionListener(TransactionState.COMMITTED, transactionListener);
            
        } else if (listener.getOnTransaction().equalsIgnoreCase(TransactionState.ROLLINGBACK.name())) {
            transactionContext.addTransactionListener(TransactionState.ROLLINGBACK, transactionListener);
            
        } else if (listener.getOnTransaction().equalsIgnoreCase(TransactionState.ROLLED_BACK.name())) {
            transactionContext.addTransactionListener(TransactionState.ROLLED_BACK, transactionListener);
            
        } else {
            LOGGER.warn("Unrecognised TransactionState {}", listener.getOnTransaction());
        }
    }

    protected synchronized void addTypedEventListener(FlowableEventListener listener, FlowableEventType type) {
        List<FlowableEventListener> listeners = typedListeners.get(type);
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
