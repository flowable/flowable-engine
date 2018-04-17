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
package org.flowable.engine.test.api.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.common.engine.impl.cfg.TransactionState;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.test.Deployment;

public class TransactionEventListenerTest extends PluggableFlowableTestCase {

    protected TestTransactionEventListener onCommitListener;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        onCommitListener = new TestTransactionEventListener(TransactionState.COMMITTED.name());
        processEngineConfiguration.getEventDispatcher().addEventListener(onCommitListener);
    }

    @Override
    protected void tearDown() throws Exception {
        TestTransactionEventListener.eventsReceived.clear();
        if (onCommitListener != null) {
            processEngineConfiguration.getEventDispatcher().removeEventListener(onCommitListener);
            onCommitListener = null;
        }

        super.tearDown();
    }

    public void testRegularProcessExecution() {

        assertEquals(0, TestTransactionEventListener.eventsReceived.size());

        // In a 'normal' process execution, the transaction dependent event listener should
        // be similar to the normal event listener dispatching.

        deployOneTaskTestProcess();
        runtimeService.startProcessInstanceByKey("oneTaskProcess");

        int expectedCreatedEvents = 10;
        if (!processEngineConfiguration.getHistoryManager().isHistoryEnabled()) {
            expectedCreatedEvents = 7;
        }
        if (processEngineConfiguration.isAsyncHistoryEnabled()) {
            waitForHistoryJobExecutorToProcessAllJobs(5000L, 100L);
        }

        assertEquals(expectedCreatedEvents, TestTransactionEventListener.eventsReceived.get(FlowableEngineEventType.ENTITY_CREATED.name()).size());
        assertEquals(expectedCreatedEvents, TestTransactionEventListener.eventsReceived.get(FlowableEngineEventType.ENTITY_INITIALIZED.name()).size());
        assertEquals(1, TestTransactionEventListener.eventsReceived.get(FlowableEngineEventType.PROCESS_STARTED.name()).size());
        assertEquals(1, TestTransactionEventListener.eventsReceived.get(FlowableEngineEventType.TASK_CREATED.name()).size());

        TestTransactionEventListener.eventsReceived.clear();

        taskService.complete(taskService.createTaskQuery().singleResult().getId());
        assertEquals(1, TestTransactionEventListener.eventsReceived.get(FlowableEngineEventType.TASK_COMPLETED.name()).size());
        assertEquals(1, TestTransactionEventListener.eventsReceived.get(FlowableEngineEventType.PROCESS_COMPLETED.name()).size());
    }

    @Deployment
    public void testProcessExecutionWithRollback() {

        assertEquals(0, TestTransactionEventListener.eventsReceived.size());
        assertEquals(0, runtimeService.createProcessInstanceQuery().count());

        // Regular execution, no exception
        runtimeService.startProcessInstanceByKey("testProcessExecutionWithRollback", CollectionUtil.singletonMap("throwException", false));
        assertTrue(TestTransactionEventListener.eventsReceived.size() > 0);
        assertEquals(1, runtimeService.createProcessInstanceQuery().count());

        TestTransactionEventListener.eventsReceived.clear();

        // When process execution rolls back, the events should not be thrown, as they are only thrown on commit.
        try {
            runtimeService.startProcessInstanceByKey("testProcessExecutionWithRollback", CollectionUtil.singletonMap("throwException", true));
            fail();
        } catch (Exception e) {}
        assertEquals(0, TestTransactionEventListener.eventsReceived.size());
        assertEquals(1, runtimeService.createProcessInstanceQuery().count());
    }

    @Deployment
    public void testProcessDefinitionDefinedEventListener() {

        // Only let the event listener of the process definition listen
        processEngineConfiguration.getEventDispatcher().removeEventListener(onCommitListener);
        TestTransactionEventListener.eventsReceived.clear();

        assertEquals(0, TestTransactionEventListener.eventsReceived.size());
        runtimeService.startProcessInstanceByKey("testProcessExecutionWithRollback", CollectionUtil.singletonMap("throwException", false));
        assertTrue(TestTransactionEventListener.eventsReceived.size() > 0);
    }

    public static class TestTransactionEventListener implements FlowableEventListener {

        protected String onTransaction;
        public static Map<String, List<FlowableEvent>> eventsReceived = new HashMap<>();

        public TestTransactionEventListener() {
            this.onTransaction = TransactionState.COMMITTED.name();
        }

        public TestTransactionEventListener(String onTransaction) {
            this.onTransaction = onTransaction;
        }

        @Override
        public void onEvent(FlowableEvent event) {
            String eventType = event.getType().name();
            if (!eventsReceived.containsKey(eventType)) {
                eventsReceived.put(eventType, new ArrayList<FlowableEvent>());
            }
            eventsReceived.get(eventType).add(event);
        }

        @Override
        public boolean isFailOnException() {
            return false;
        }

        @Override
        public boolean isFireOnTransactionLifecycleEvent() {
            return true;
        }

        @Override
        public String getOnTransaction() {
            return onTransaction;
        }

    }

    public static class ThrowExceptionDelegate implements JavaDelegate {

        @Override
        public void execute(DelegateExecution execution) {
            boolean throwException = (Boolean) execution.getVariable("throwException");
            if (throwException) {
                throw new RuntimeException();
            }
        }

    }

}
