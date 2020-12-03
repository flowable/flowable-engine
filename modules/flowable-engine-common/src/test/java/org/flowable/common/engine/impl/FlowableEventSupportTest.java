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
package org.flowable.common.engine.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.delegate.event.AbstractFlowableEventListener;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventType;
import org.flowable.common.engine.impl.cfg.TransactionContext;
import org.flowable.common.engine.impl.cfg.TransactionListener;
import org.flowable.common.engine.impl.cfg.TransactionState;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.event.FlowableEventSupport;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
class FlowableEventSupportTest {

    private FlowableEventSupport flowableEventSupport = new FlowableEventSupport();

    @Test
    void addNullEventListenerShouldFail() {
        assertThatThrownBy(() -> flowableEventSupport.addEventListener(null))
            .isInstanceOf(FlowableIllegalArgumentException.class);

        assertThatThrownBy(() -> flowableEventSupport.addEventListener(null, new TestFlowableEventType("test")))
            .isInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    void dispatchNullEventShouldFail() {
        assertThatThrownBy(() -> flowableEventSupport.dispatchEvent(null))
            .isInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    void dispatchEventWithNullTypeShouldFail() {
        assertThatThrownBy(() -> flowableEventSupport.dispatchEvent(new TestFlowableEvent(null)))
            .isInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    void dispatchEventShouldProperlyDispatchToAllListeners() {
        TestFlowableEventListener global1Listener = new TestFlowableEventListener();
        TestFlowableEventListener global2Listener = new TestFlowableEventListener();
        flowableEventSupport.addEventListener(global1Listener);
        flowableEventSupport.addEventListener(global2Listener);

        TestFlowableEventListener test1Listener = new TestFlowableEventListener();
        TestFlowableEventListener test2Listener = new TestFlowableEventListener();
        TestFlowableEventType testEventType = new TestFlowableEventType("test");
        flowableEventSupport.addEventListener(test1Listener, testEventType);
        flowableEventSupport.addEventListener(test2Listener, testEventType);

        TestFlowableEventListener otherTestListener = new TestFlowableEventListener();
        TestFlowableEventType otherTestEventType = new TestFlowableEventType("otherTest");
        flowableEventSupport.addEventListener(otherTestListener, otherTestEventType);

        FlowableEvent globalEvent = new TestFlowableEvent(new TestFlowableEventType("global"));
        FlowableEvent test1Event = new TestFlowableEvent(testEventType);
        FlowableEvent test2Event = new TestFlowableEvent(testEventType);
        FlowableEvent otherTest1Event = new TestFlowableEvent(otherTestEventType);
        flowableEventSupport.dispatchEvent(globalEvent);
        flowableEventSupport.dispatchEvent(test1Event);
        flowableEventSupport.dispatchEvent(test2Event);
        flowableEventSupport.dispatchEvent(otherTest1Event);

        assertThat(global1Listener.getReceivedEvents())
            .as("global1 listener")
            .containsExactly(globalEvent, test1Event, test2Event, otherTest1Event);

        assertThat(global2Listener.getReceivedEvents())
            .as("global2 listener")
            .containsExactly(globalEvent, test1Event, test2Event, otherTest1Event);

        assertThat(test1Listener.getReceivedEvents())
            .as("test1 listener")
            .containsExactly(test1Event, test2Event);

        assertThat(test2Listener.getReceivedEvents())
            .as("test2 listener")
            .containsExactly(test1Event, test2Event);

        assertThat(otherTestListener.getReceivedEvents())
            .as("otherTest1 listener")
            .containsExactly(otherTest1Event);
    }

    @Test
    void dispatchEventShouldProperlyDispatchToAllListenersWhenTheyHaveExplicitEvents() {
        TestFlowableEventListener global1Listener = new TestFlowableEventListener();
        TestFlowableEventListener global2Listener = new TestFlowableEventListener();
        flowableEventSupport.addEventListener(global1Listener);
        flowableEventSupport.addEventListener(global2Listener);

        TestFlowableEventType testEventType = new TestFlowableEventType("test");
        TestFlowableEventListener test1Listener = new TestFlowableEventListener(testEventType);
        TestFlowableEventListener test2Listener = new TestFlowableEventListener(testEventType);
        flowableEventSupport.addEventListener(test1Listener);
        flowableEventSupport.addEventListener(test2Listener);

        TestFlowableEventListener otherTestListener = new TestFlowableEventListener();
        TestFlowableEventType otherTestEventType = new TestFlowableEventType("otherTest");
        flowableEventSupport.addEventListener(otherTestListener, otherTestEventType);

        FlowableEvent globalEvent = new TestFlowableEvent(new TestFlowableEventType("global"));
        FlowableEvent test1Event = new TestFlowableEvent(testEventType);
        FlowableEvent test2Event = new TestFlowableEvent(testEventType);
        FlowableEvent otherTest1Event = new TestFlowableEvent(otherTestEventType);
        flowableEventSupport.dispatchEvent(globalEvent);
        flowableEventSupport.dispatchEvent(test1Event);
        flowableEventSupport.dispatchEvent(test2Event);
        flowableEventSupport.dispatchEvent(otherTest1Event);

        assertThat(global1Listener.getReceivedEvents())
            .as("global1 listener")
            .containsExactly(globalEvent, test1Event, test2Event, otherTest1Event);

        assertThat(global2Listener.getReceivedEvents())
            .as("global2 listener")
            .containsExactly(globalEvent, test1Event, test2Event, otherTest1Event);

        assertThat(test1Listener.getReceivedEvents())
            .as("test1 listener")
            .containsExactly(test1Event, test2Event);

        assertThat(test2Listener.getReceivedEvents())
            .as("test2 listener")
            .containsExactly(test1Event, test2Event);

        assertThat(otherTestListener.getReceivedEvents())
            .as("otherTest1 listener")
            .containsExactly(otherTest1Event);
    }

    @Test
    void dispatchEventWithFailOnExceptionShouldStopDispatchingToOtherListeners() {
        TestFlowableEventListener globalListener = new TestFlowableEventListener();
        flowableEventSupport.addEventListener(globalListener);

        TestFlowableEventListener test1Listener = new TestFlowableEventListener();
        test1Listener.setExceptionToThrow(new RuntimeException("test1 listener exception"));
        test1Listener.setFailOnException(true);
        TestFlowableEventListener test2Listener = new TestFlowableEventListener();
        TestFlowableEventType testEventType = new TestFlowableEventType("test");
        flowableEventSupport.addEventListener(test1Listener, testEventType);
        flowableEventSupport.addEventListener(test2Listener, testEventType);

        FlowableEvent globalEvent = new TestFlowableEvent(new TestFlowableEventType("global"));
        FlowableEvent testEvent = new TestFlowableEvent(testEventType);
        flowableEventSupport.dispatchEvent(globalEvent);

        assertThatThrownBy(() -> flowableEventSupport.dispatchEvent(testEvent))
            .isExactlyInstanceOf(RuntimeException.class)
            .hasMessage("test1 listener exception");

        assertThat(globalListener.getReceivedEvents())
            .as("global1 listener")
            .containsExactly(globalEvent, testEvent);

        assertThat(test1Listener.getReceivedEvents())
            .as("test1 listener")
            .containsExactly(testEvent);

        assertThat(test2Listener.getReceivedEvents())
            .as("test2 listener")
            .isEmpty();
    }

    @Test
    void dispatchEventWithoutFailOnExceptionShouldStopDispatchingToOtherListeners() {
        TestFlowableEventListener globalListener = new TestFlowableEventListener();
        flowableEventSupport.addEventListener(globalListener);

        TestFlowableEventListener test1Listener = new TestFlowableEventListener();
        test1Listener.setExceptionToThrow(new RuntimeException("test1 listener exception"));
        TestFlowableEventListener test2Listener = new TestFlowableEventListener();
        TestFlowableEventType testEventType = new TestFlowableEventType("test");
        flowableEventSupport.addEventListener(test1Listener, testEventType);
        flowableEventSupport.addEventListener(test2Listener, testEventType);

        FlowableEvent globalEvent = new TestFlowableEvent(new TestFlowableEventType("global"));
        FlowableEvent testEvent = new TestFlowableEvent(testEventType);
        flowableEventSupport.dispatchEvent(globalEvent);
        flowableEventSupport.dispatchEvent(testEvent);

        assertThat(globalListener.getReceivedEvents())
            .as("global1 listener")
            .containsExactly(globalEvent, testEvent);

        assertThat(test1Listener.getReceivedEvents())
            .as("test1 listener")
            .containsExactly(testEvent);

        assertThat(test2Listener.getReceivedEvents())
            .as("test2 listener")
            .containsExactly(testEvent);
    }

    @Test
    void shouldNotDispatchEventToRemovedListener() {
        TestFlowableEventListener globalListener = new TestFlowableEventListener();
        flowableEventSupport.addEventListener(globalListener);

        FlowableEvent globalEvent = new TestFlowableEvent(new TestFlowableEventType("global"));
        flowableEventSupport.dispatchEvent(globalEvent);
        flowableEventSupport.removeEventListener(globalListener);

        FlowableEvent globalEvent2 = new TestFlowableEvent(new TestFlowableEventType("global2"));
        flowableEventSupport.dispatchEvent(globalEvent2);

        assertThat(globalListener.getReceivedEvents())
            .as("global listener")
            .containsExactly(globalEvent);
    }

    @Test
    void shouldNotDispatchEventToRemovedTypedListener() {
        TestFlowableEventListener typeListener = new TestFlowableEventListener();
        TestFlowableEventType type1 = new TestFlowableEventType("type1");
        flowableEventSupport.addEventListener(typeListener, type1);

        FlowableEvent globalEvent = new TestFlowableEvent(new TestFlowableEventType("global"));
        flowableEventSupport.dispatchEvent(globalEvent);

        FlowableEvent type1Event = new TestFlowableEvent(type1);
        flowableEventSupport.dispatchEvent(type1Event);
        flowableEventSupport.removeEventListener(typeListener);

        FlowableEvent type2Event = new TestFlowableEvent(type1);
        flowableEventSupport.dispatchEvent(type2Event);

        assertThat(typeListener.getReceivedEvents())
            .as("type listener")
            .containsExactly(type1Event);
    }

    @Test
    void shouldProperlyDispatchOnTransactionLifecycleListeners() {
        TestFlowableEventListener rolledBackListener = new TestFlowableEventListener();
        rolledBackListener.setOnTransaction(TransactionState.ROLLED_BACK.name());
        flowableEventSupport.addEventListener(rolledBackListener);

        TestFlowableEventListener rollingBackListener = new TestFlowableEventListener();
        rollingBackListener.setOnTransaction(TransactionState.ROLLINGBACK.name());
        flowableEventSupport.addEventListener(rollingBackListener);

        TestFlowableEventListener committedListener = new TestFlowableEventListener();
        committedListener.setOnTransaction(TransactionState.COMMITTED.name());
        flowableEventSupport.addEventListener(committedListener);

        TestFlowableEventListener committingListener = new TestFlowableEventListener();
        committingListener.setOnTransaction(TransactionState.COMMITTING.name());
        flowableEventSupport.addEventListener(committingListener);

        TestFlowableEventListener unknownTransactionListener = new TestFlowableEventListener();
        unknownTransactionListener.setOnTransaction("unknown");
        flowableEventSupport.addEventListener(unknownTransactionListener);

        TestFlowableEventListener normalListener = new TestFlowableEventListener();
        flowableEventSupport.addEventListener(normalListener);

        try {
            TestTransactionContext transactionContext = new TestTransactionContext();
            Context.setTransactionContext(transactionContext);
            TestFlowableEvent event = new TestFlowableEvent(new TestFlowableEventType("event"));

            flowableEventSupport.dispatchEvent(event);

            assertThat(rolledBackListener.getReceivedEvents())
                .as("rolledBackListener received events after normal dispatch")
                .isEmpty();
            assertThat(rollingBackListener.getReceivedEvents())
                .as("rollingBackListener received events after normal dispatch")
                .isEmpty();
            assertThat(committedListener.getReceivedEvents())
                .as("committedListener received events after normal dispatch")
                .isEmpty();
            assertThat(committingListener.getReceivedEvents())
                .as("committingListener received events after normal dispatch")
                .isEmpty();
            assertThat(normalListener.getReceivedEvents())
                .as("normalListener received events after normal dispatch")
                .containsExactly(event);
            assertThat(unknownTransactionListener.getReceivedEvents())
                .as("unknownListener received events after normal dispatch")
                .isEmpty();

            assertThat(transactionContext.getTransactionStateListeners())
                .containsOnlyKeys(TransactionState.COMMITTING, TransactionState.COMMITTED, TransactionState.ROLLED_BACK, TransactionState.ROLLINGBACK);

            transactionContext.getTransactionStateListeners().get(TransactionState.COMMITTING)
                .forEach(transactionListener -> transactionListener.execute(null));

            assertThat(rolledBackListener.getReceivedEvents())
                .as("rolledBackListener received events after committing")
                .isEmpty();
            assertThat(rollingBackListener.getReceivedEvents())
                .as("rollingBackListener received events after committing")
                .isEmpty();
            assertThat(committedListener.getReceivedEvents())
                .as("committedListener received events after committing")
                .isEmpty();
            assertThat(committingListener.getReceivedEvents())
                .as("committingListener received events after committing")
                .containsExactly(event);
            assertThat(unknownTransactionListener.getReceivedEvents())
                .as("unknownListener received events after committing")
                .isEmpty();

            transactionContext.getTransactionStateListeners().get(TransactionState.COMMITTED)
                .forEach(transactionListener -> transactionListener.execute(null));

            assertThat(rolledBackListener.getReceivedEvents())
                .as("rolledBackListener received events after committed")
                .isEmpty();
            assertThat(rollingBackListener.getReceivedEvents())
                .as("rollingBackListener received events after committed")
                .isEmpty();
            assertThat(committedListener.getReceivedEvents())
                .as("committedListener received events after committed")
                .containsExactly(event);
            assertThat(committingListener.getReceivedEvents())
                .as("committingListener received events after committed")
                .containsExactly(event);
            assertThat(unknownTransactionListener.getReceivedEvents())
                .as("unknownListener received events after committing")
                .isEmpty();

            transactionContext.getTransactionStateListeners().get(TransactionState.ROLLINGBACK)
                .forEach(transactionListener -> transactionListener.execute(null));

            assertThat(rolledBackListener.getReceivedEvents())
                .as("rolledBackListener received events after rolling back")
                .isEmpty();
            assertThat(rollingBackListener.getReceivedEvents())
                .as("rollingBackListener received events after rolling back")
                .containsExactly(event);
            assertThat(committedListener.getReceivedEvents())
                .as("committedListener received events after rolling back")
                .containsExactly(event);
            assertThat(committingListener.getReceivedEvents())
                .as("committingListener received events after rolling back")
                .containsExactly(event);
            assertThat(unknownTransactionListener.getReceivedEvents())
                .as("unknownListener received events after rolling back")
                .isEmpty();

            transactionContext.getTransactionStateListeners().get(TransactionState.ROLLED_BACK)
                .forEach(transactionListener -> transactionListener.execute(null));

            assertThat(rolledBackListener.getReceivedEvents())
                .as("rolledBackListener received events after rolled back")
                .containsExactly(event);
            assertThat(rollingBackListener.getReceivedEvents())
                .as("rollingBackListener received events after rolled back")
                .containsExactly(event);
            assertThat(committedListener.getReceivedEvents())
                .as("committedListener received events after rolled back")
                .containsExactly(event);
            assertThat(committingListener.getReceivedEvents())
                .as("committingListener received events after rolled back")
                .containsExactly(event);
            assertThat(unknownTransactionListener.getReceivedEvents())
                .as("unknownListener received events after rolled back")
                .isEmpty();

        } finally {
            Context.removeTransactionContext();
        }

    }

    @Test
    void shouldProperlyDispatchOnTransactionWithFailOnException() {
        TestFlowableEventListener rolledBackListener = new TestFlowableEventListener();
        rolledBackListener.setOnTransaction(TransactionState.ROLLED_BACK.name());
        rolledBackListener.setFailOnException(true);
        rolledBackListener.setExceptionToThrow(new RuntimeException("rolled back exception"));
        flowableEventSupport.addEventListener(rolledBackListener);

        TestFlowableEventListener rollingBackListener = new TestFlowableEventListener();
        rollingBackListener.setOnTransaction(TransactionState.ROLLINGBACK.name());
        rollingBackListener.setFailOnException(true);
        rollingBackListener.setExceptionToThrow(new RuntimeException("rolling back exception"));
        flowableEventSupport.addEventListener(rollingBackListener);

        TestFlowableEventListener committedListener = new TestFlowableEventListener();
        committedListener.setOnTransaction(TransactionState.COMMITTED.name());
        committedListener.setFailOnException(true);
        committedListener.setExceptionToThrow(new RuntimeException("committed exception"));
        flowableEventSupport.addEventListener(committedListener);

        TestFlowableEventListener committingListener = new TestFlowableEventListener();
        committingListener.setOnTransaction(TransactionState.COMMITTING.name());
        committingListener.setFailOnException(true);
        committingListener.setExceptionToThrow(new RuntimeException("committing exception"));
        flowableEventSupport.addEventListener(committingListener);

        TestFlowableEventListener unknownTransactionListener = new TestFlowableEventListener();
        unknownTransactionListener.setOnTransaction("unknown");
        unknownTransactionListener.setFailOnException(true);
        unknownTransactionListener.setExceptionToThrow(new RuntimeException("unknown exception"));
        flowableEventSupport.addEventListener(unknownTransactionListener);

        TestFlowableEventListener normalListener = new TestFlowableEventListener();
        flowableEventSupport.addEventListener(normalListener);

        try {
            TestTransactionContext transactionContext = new TestTransactionContext();
            Context.setTransactionContext(transactionContext);
            TestFlowableEvent event = new TestFlowableEvent(new TestFlowableEventType("event"));

            flowableEventSupport.dispatchEvent(event);

            assertThat(rolledBackListener.getReceivedEvents())
                .as("rolledBackListener received events after normal dispatch")
                .isEmpty();
            assertThat(rollingBackListener.getReceivedEvents())
                .as("rollingBackListener received events after normal dispatch")
                .isEmpty();
            assertThat(committedListener.getReceivedEvents())
                .as("committedListener received events after normal dispatch")
                .isEmpty();
            assertThat(committingListener.getReceivedEvents())
                .as("committingListener received events after normal dispatch")
                .isEmpty();
            assertThat(unknownTransactionListener.getReceivedEvents())
                .as("committingListener received events after normal dispatch")
                .isEmpty();
            assertThat(normalListener.getReceivedEvents())
                .as("normalListener received events after normal dispatch")
                .containsExactly(event);

            Map<TransactionState, List<TransactionListener>> transactionStateListeners = transactionContext.getTransactionStateListeners();
            assertThat(transactionStateListeners)
                .containsOnlyKeys(TransactionState.COMMITTING, TransactionState.COMMITTED, TransactionState.ROLLED_BACK, TransactionState.ROLLINGBACK);

            assertThatThrownBy(() -> transactionStateListeners.get(TransactionState.COMMITTING).get(0).execute(null))
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessage("committing exception");

            assertThatThrownBy(() -> transactionStateListeners.get(TransactionState.COMMITTED).get(0).execute(null))
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessage("committed exception");

            assertThatThrownBy(() -> transactionStateListeners.get(TransactionState.ROLLED_BACK).get(0).execute(null))
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessage("rolled back exception");

            assertThatThrownBy(() -> transactionStateListeners.get(TransactionState.ROLLINGBACK).get(0).execute(null))
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessage("rolling back exception");

        } finally {
            Context.removeTransactionContext();
        }

    }

    private static class TestFlowableEventType implements FlowableEventType {

        protected final String name;

        private TestFlowableEventType(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String toString() {
            return "TestFlowableEventType{" +
                "name='" + name + '\'' +
                '}';
        }
    }

    private static class TestFlowableEvent implements FlowableEvent {

        protected final FlowableEventType type;

        private TestFlowableEvent(FlowableEventType type) {
            this.type = type;
        }

        @Override
        public FlowableEventType getType() {
            return type;
        }

        @Override
        public String toString() {
            return "TestFlowableEvent{" +
                "type=" + type +
                '}';
        }
    }

    private static class TestFlowableEventListener extends AbstractFlowableEventListener {

        protected final List<FlowableEvent> receivedEvents = new ArrayList<>();
        protected boolean failOnException;
        protected RuntimeException exceptionToThrow;

        protected FlowableEventType eventType;

        public TestFlowableEventListener() {
            this(null);
        }

        protected TestFlowableEventListener(FlowableEventType eventType) {
            this.eventType = eventType;
        }

        @Override
        public void onEvent(FlowableEvent event) {
            receivedEvents.add(event);
            if (exceptionToThrow != null) {
                throw exceptionToThrow;
            }
        }

        @Override
        public boolean isFailOnException() {
            return failOnException;
        }

        @Override
        public Collection<? extends FlowableEventType> getTypes() {
            return eventType == null ? super.getTypes() : Collections.singleton(eventType);
        }

        public List<FlowableEvent> getReceivedEvents() {
            return receivedEvents;
        }

        public void setFailOnException(boolean failOnException) {
            this.failOnException = failOnException;
        }

        public RuntimeException getExceptionToThrow() {
            return exceptionToThrow;
        }

        public void setExceptionToThrow(RuntimeException exceptionToThrow) {
            this.exceptionToThrow = exceptionToThrow;
        }
    }

    private static class TestTransactionContext implements TransactionContext {

        protected final Map<TransactionState, List<TransactionListener>> transactionStateListeners = new HashMap<>();

        @Override
        public void commit() {

        }

        @Override
        public void rollback() {

        }

        @Override
        public void addTransactionListener(TransactionState transactionState, TransactionListener transactionListener) {
            transactionStateListeners.computeIfAbsent(transactionState, state -> new ArrayList<>()).add(transactionListener);
        }

        public Map<TransactionState, List<TransactionListener>> getTransactionStateListeners() {
            return transactionStateListeners;
        }
    }
}
