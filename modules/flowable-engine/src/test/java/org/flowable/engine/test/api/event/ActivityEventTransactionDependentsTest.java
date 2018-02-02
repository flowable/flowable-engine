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

import org.flowable.engine.common.api.delegate.event.FlowableEngineEventType;
import org.flowable.engine.common.api.delegate.event.FlowableEvent;
import org.flowable.engine.common.api.delegate.event.FlowableEventListener;
import org.flowable.engine.common.impl.cfg.TransactionState;
import org.flowable.engine.event.EventLogEntry;
import org.flowable.engine.impl.event.logger.EventLogger;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;

import java.util.ArrayList;
import java.util.List;

/**
 * Test case for all {@link FlowableEvent}s related to activities.
 *
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class ActivityEventTransactionDependentsTest extends PluggableFlowableTestCase {

    protected EventLogger databaseEventLogger;
    private StandardFlowableEventListener listener;
    private TestTransactionFlowableEventListener committedTransactionDependentListener;
    private ThrowingExceptionFlowableEventListener exceptionListener;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Database event logger setup
        databaseEventLogger = new EventLogger(processEngineConfiguration.getClock(), processEngineConfiguration.getObjectMapper());
        runtimeService.addEventListener(databaseEventLogger);
    }

    @Override
    protected void tearDown() throws Exception {

        if (listener != null) {
            listener.clearEventsReceived();
            processEngineConfiguration.getEventDispatcher().removeEventListener(listener);
        }

        if (exceptionListener != null) {
            processEngineConfiguration.getEventDispatcher().removeEventListener(exceptionListener);
            exceptionListener = null;
        }

        if (committedTransactionDependentListener != null) {
            processEngineConfiguration.getEventDispatcher().removeEventListener(committedTransactionDependentListener);
            committedTransactionDependentListener = null;
        }
        // Remove entries
        for (EventLogEntry eventLogEntry : managementService.getEventLogEntries(null, null)) {
            managementService.deleteEventLogEntry(eventLogEntry.getLogNumber());
        }

        // Database event logger teardown
        runtimeService.removeEventListener(databaseEventLogger);

        processEngineConfiguration.getEventDispatcher().setTransactionEnabled(false);
        super.tearDown();
    }

    @Override
    protected void initializeServices() {
        super.initializeServices();

        listener = new StandardFlowableEventListener();
        committedTransactionDependentListener = new TestTransactionFlowableEventListener();
        committedTransactionDependentListener.setOnTransaction(TransactionState.COMMITTED.name());
        processEngineConfiguration.getEventDispatcher().addEventListener(listener);
        processEngineConfiguration.getEventDispatcher().addEventListener(committedTransactionDependentListener);
        processEngineConfiguration.getEventDispatcher().setTransactionEnabled(true);
    }

    /**
     * Test starting and completed events for activity. Since these events are dispatched in the
     * core of the PVM, not all individual activity-type is tested. Rather, we test the main types
     * (tasks, gateways, events, subprocesses).
     */
    @Deployment
    public void testFailedTransaction() {
        // We're interested in the raw events, alter the listener to keep those as well

        exceptionListener = new ThrowingExceptionFlowableEventListener();
        processEngineConfiguration.getEventDispatcher().addEventListener(exceptionListener);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("activityProcess");
        assertNotNull(processInstance);

        assertEquals(27, listener.getEventsReceived().size());
        assertEquals(27, committedTransactionDependentListener.getEventsReceived().size());

        try {
            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            taskService.complete(task.getId());
        } catch (Exception e) {
            //NPE triggered from service task
        }
        assertEquals(28, listener.getEventsReceived().size());
        assertEquals(27, committedTransactionDependentListener.getEventsReceived().size());
        assertTrue(listener.isEventTriggered(FlowableEngineEventType.TASK_COMPLETED));
        assertFalse(committedTransactionDependentListener.isEventTriggered(FlowableEngineEventType.TASK_COMPLETED));

    }

    @Deployment
    public void testSuccessTransaction()  {
        // We're interested in the raw events, alter the listener to keep those as well

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("activityProcess");
        assertNotNull(processInstance);

        assertEquals(27, listener.getEventsReceived().size());
        assertEquals(27, committedTransactionDependentListener.getEventsReceived().size());

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        assertEquals(42, listener.getEventsReceived().size());
        assertEquals(42, committedTransactionDependentListener.getEventsReceived().size());
        assertTrue(listener.isEventTriggered(FlowableEngineEventType.TASK_COMPLETED));
        assertTrue(committedTransactionDependentListener.isEventTriggered(FlowableEngineEventType.TASK_COMPLETED));
    }

    @Deployment
    public void testElement() {
        // We're interested in the raw events, alter the listener to keep those as well

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("activityProcess");
        assertNotNull(processInstance);

        assertEquals(27, listener.getEventsReceived().size());
        assertEquals(27, committedTransactionDependentListener.getEventsReceived().size());

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        assertEquals(42, listener.getEventsReceived().size());
        assertEquals(42, committedTransactionDependentListener.getEventsReceived().size());
    }

    private class ThrowingExceptionFlowableEventListener implements FlowableEventListener {

        @Override
        public void onEvent(FlowableEvent event) {
            if (event.getType() == FlowableEngineEventType.TASK_COMPLETED) {
                LOGGER.debug("Throwing exception");
                String exception = null;
                //throwing NPE
                exception.equals("THROW EXCEPTION");
            } else if (event.getType() == FlowableEngineEventType.TASK_CREATED) {
                LOGGER.debug("TASK CREATED");
            }
        }

        @Override
        public boolean isFailOnException() {
            return true;
        }
    }

    private class StandardFlowableEventListener implements FlowableEventListener {

        private List<FlowableEvent> eventsReceived;

        public StandardFlowableEventListener() {
            this.eventsReceived = new ArrayList<>();
        }

        @Override
        public void onEvent(FlowableEvent event) {
            eventsReceived.add(event);
            LOGGER.debug("{} event added ... {}", event.getType(), eventsReceived.size());
        }

        public List<FlowableEvent> getEventsReceived() {
            return eventsReceived;
        }

        public boolean isEventTriggered(FlowableEngineEventType flowableEvent) {
            for (FlowableEvent event : eventsReceived) {
                if (event.getType() == flowableEvent) return true;
            }
            return false;
        }

        public void clearEventsReceived() {
            eventsReceived.clear();
        }

        @Override
        public boolean isFailOnException() {
            return false;
        }
    }

}
