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

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.impl.event.FlowableEventDispatcherImpl;
import org.flowable.common.engine.impl.event.FlowableEngineEventImpl;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.delegate.event.BaseEntityEventListener;
import org.flowable.engine.delegate.event.impl.FlowableEntityEventImpl;
import org.flowable.engine.delegate.event.impl.FlowableProcessEventImpl;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.task.service.TaskServiceConfiguration;

/**
 * 
 * @author Frederik Heremans
 */
public abstract class FlowableEventDispatcherTest extends PluggableFlowableTestCase {

    protected FlowableEventDispatcher dispatcher;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        dispatcher = new FlowableEventDispatcherImpl();
    }

    /**
     * Test adding a listener and check if events are sent to it. Also checks that after removal, no events are received.
     */
    public void addAndRemoveEventListenerAllEvents() throws Exception {
        // Create a listener that just adds the events to a list
        TestFlowableEventListener newListener = new TestFlowableEventListener();

        // Add event-listener to dispatcher
        dispatcher.addEventListener(newListener);

        TaskServiceConfiguration taskServiceConfiguration = (TaskServiceConfiguration) processEngineConfiguration.getServiceConfigurations().get(EngineConfigurationConstants.KEY_TASK_SERVICE_CONFIG);
        FlowableEntityEventImpl event1 = new FlowableEntityEventImpl(taskServiceConfiguration.getTaskEntityManager().create(), FlowableEngineEventType.ENTITY_CREATED);
        FlowableEntityEventImpl event2 = new FlowableEntityEventImpl(taskServiceConfiguration.getTaskEntityManager().create(), FlowableEngineEventType.ENTITY_CREATED);

        // Dispatch events
        dispatcher.dispatchEvent(event1);
        dispatcher.dispatchEvent(event2);

        assertEquals(2, newListener.getEventsReceived().size());
        assertEquals(event1, newListener.getEventsReceived().get(0));
        assertEquals(event2, newListener.getEventsReceived().get(1));

        // Remove listener and dispatch events again, listener should not be
        // invoked
        dispatcher.removeEventListener(newListener);
        newListener.clearEventsReceived();
        dispatcher.dispatchEvent(event1);
        dispatcher.dispatchEvent(event2);

        assertTrue(newListener.getEventsReceived().isEmpty());
    }

    /**
     * Test adding a listener and check if events are sent to it, for the types it was registered for. Also checks that after removal, no events are received.
     */
    public void addAndRemoveEventListenerTyped() throws Exception {
        // Create a listener that just adds the events to a list
        TestFlowableEventListener newListener = new TestFlowableEventListener();

        // Add event-listener to dispatcher
        dispatcher.addEventListener(newListener, FlowableEngineEventType.ENTITY_CREATED, FlowableEngineEventType.ENTITY_DELETED);

        TaskServiceConfiguration taskServiceConfiguration = (TaskServiceConfiguration) processEngineConfiguration.getServiceConfigurations().get(EngineConfigurationConstants.KEY_TASK_SERVICE_CONFIG);
        FlowableEntityEventImpl event1 = new FlowableEntityEventImpl(taskServiceConfiguration.getTaskEntityManager().create(), FlowableEngineEventType.ENTITY_CREATED);
        FlowableEntityEventImpl event2 = new FlowableEntityEventImpl(taskServiceConfiguration.getTaskEntityManager().create(), FlowableEngineEventType.ENTITY_DELETED);
        FlowableEntityEventImpl event3 = new FlowableEntityEventImpl(taskServiceConfiguration.getTaskEntityManager().create(), FlowableEngineEventType.ENTITY_UPDATED);

        // Dispatch events, only 2 out of 3 should have entered the listener
        dispatcher.dispatchEvent(event1);
        dispatcher.dispatchEvent(event2);
        dispatcher.dispatchEvent(event3);

        assertEquals(2, newListener.getEventsReceived().size());
        assertEquals(event1, newListener.getEventsReceived().get(0));
        assertEquals(event2, newListener.getEventsReceived().get(1));

        // Remove listener and dispatch events again, listener should not be
        // invoked
        dispatcher.removeEventListener(newListener);
        newListener.clearEventsReceived();
        dispatcher.dispatchEvent(event1);
        dispatcher.dispatchEvent(event2);

        assertTrue(newListener.getEventsReceived().isEmpty());
    }

    /**
     * Test that adding a listener with a null-type is never called.
     */
    public void addAndRemoveEventListenerTypedNullType() throws Exception {

        // Create a listener that just adds the events to a list
        TestFlowableEventListener newListener = new TestFlowableEventListener();

        // Add event-listener to dispatcher
        dispatcher.addEventListener(newListener, (FlowableEngineEventType) null);

        TaskServiceConfiguration taskServiceConfiguration = (TaskServiceConfiguration) processEngineConfiguration.getServiceConfigurations().get(EngineConfigurationConstants.KEY_TASK_SERVICE_CONFIG);
        FlowableEntityEventImpl event1 = new FlowableEntityEventImpl(taskServiceConfiguration.getTaskEntityManager().create(), FlowableEngineEventType.ENTITY_CREATED);
        FlowableEntityEventImpl event2 = new FlowableEntityEventImpl(taskServiceConfiguration.getTaskEntityManager().create(), FlowableEngineEventType.ENTITY_DELETED);

        // Dispatch events, all should have entered the listener
        dispatcher.dispatchEvent(event1);
        dispatcher.dispatchEvent(event2);

        assertTrue(newListener.getEventsReceived().isEmpty());
    }

    /**
     * Test the {@link BaseEntityEventListener} shipped with Flowable.
     */
    public void baseEntityEventListener() throws Exception {
        TestBaseEntityEventListener listener = new TestBaseEntityEventListener();

        dispatcher.addEventListener(listener);

        TaskServiceConfiguration taskServiceConfiguration = (TaskServiceConfiguration) processEngineConfiguration.getServiceConfigurations().get(EngineConfigurationConstants.KEY_TASK_SERVICE_CONFIG);
        FlowableEntityEventImpl createEvent = new FlowableEntityEventImpl(taskServiceConfiguration.getTaskEntityManager().create(), FlowableEngineEventType.ENTITY_CREATED);
        FlowableEntityEventImpl deleteEvent = new FlowableEntityEventImpl(taskServiceConfiguration.getTaskEntityManager().create(), FlowableEngineEventType.ENTITY_DELETED);
        FlowableEntityEventImpl updateEvent = new FlowableEntityEventImpl(taskServiceConfiguration.getTaskEntityManager().create(), FlowableEngineEventType.ENTITY_UPDATED);
        FlowableEntityEventImpl otherEvent = new FlowableEntityEventImpl(taskServiceConfiguration.getTaskEntityManager().create(), FlowableEngineEventType.CUSTOM);

        // Dispatch create event
        dispatcher.dispatchEvent(createEvent);
        assertTrue(listener.isCreateReceived());
        assertFalse(listener.isUpdateReceived());
        assertFalse(listener.isCustomReceived());
        assertFalse(listener.isInitializeReceived());
        assertFalse(listener.isDeleteReceived());
        listener.reset();

        // Dispatch update event
        dispatcher.dispatchEvent(updateEvent);
        assertTrue(listener.isUpdateReceived());
        assertFalse(listener.isCreateReceived());
        assertFalse(listener.isCustomReceived());
        assertFalse(listener.isDeleteReceived());
        listener.reset();

        // Dispatch delete event
        dispatcher.dispatchEvent(deleteEvent);
        assertTrue(listener.isDeleteReceived());
        assertFalse(listener.isCreateReceived());
        assertFalse(listener.isCustomReceived());
        assertFalse(listener.isUpdateReceived());
        listener.reset();

        // Dispatch other event
        dispatcher.dispatchEvent(otherEvent);
        assertTrue(listener.isCustomReceived());
        assertFalse(listener.isCreateReceived());
        assertFalse(listener.isUpdateReceived());
        assertFalse(listener.isDeleteReceived());
        listener.reset();

        // Test typed entity-listener
        listener = new TestBaseEntityEventListener(org.flowable.task.api.Task.class);

        // Dispatch event for a task, should be received
        dispatcher.addEventListener(listener);
        dispatcher.dispatchEvent(createEvent);

        assertTrue(listener.isCreateReceived());
        listener.reset();

        // Dispatch event for a execution, should NOT be received
        FlowableEntityEventImpl createEventForExecution = new FlowableEntityEventImpl(new ExecutionEntityImpl(), FlowableEngineEventType.ENTITY_CREATED);

        dispatcher.dispatchEvent(createEventForExecution);
        assertFalse(listener.isCreateReceived());
    }

    /**
     * Test dispatching behavior when an exception occurs in the listener
     */
    public void exceptionInListener() throws Exception {
        // Create listener that doesn't force the dispatching to fail
        TestExceptionFlowableEventListener listener = new TestExceptionFlowableEventListener(false);
        TestFlowableEventListener secondListener = new TestFlowableEventListener();

        dispatcher.addEventListener(listener);
        dispatcher.addEventListener(secondListener);

        FlowableEngineEventImpl event = new FlowableProcessEventImpl(FlowableEngineEventType.ENTITY_CREATED);
        try {
            dispatcher.dispatchEvent(event);
            assertEquals(1, secondListener.getEventsReceived().size());
        } catch (Throwable t) {
            fail("No exception expected");
        }

        // Remove listeners
        dispatcher.removeEventListener(listener);
        dispatcher.removeEventListener(secondListener);

        // Create listener that forces the dispatching to fail
        listener = new TestExceptionFlowableEventListener(true);
        secondListener = new TestFlowableEventListener();
        dispatcher.addEventListener(listener);
        dispatcher.addEventListener(secondListener);

        try {
            dispatcher.dispatchEvent(event);
            fail("Exception expected");
        } catch (Throwable t) {
            assertTrue(t instanceof FlowableException);
            assertTrue(t.getCause() instanceof RuntimeException);
            assertEquals("Test exception", t.getCause().getMessage());

            // Second listener should NOT have been called
            assertEquals(0, secondListener.getEventsReceived().size());
        }
    }

    /**
     * Test conversion of string-value (and list) in list of {@link FlowableEngineEventType}s, used in configuration of process-engine
     * {@link ProcessEngineConfigurationImpl#setTypedEventListeners(java.util.Map)} .
     */
    public void activitiEventTypeParsing() throws Exception {
        // Check with empty null
        FlowableEngineEventType[] types = FlowableEngineEventType.getTypesFromString(null);
        assertNotNull(types);
        assertEquals(0, types.length);

        // Check with empty string
        types = FlowableEngineEventType.getTypesFromString("");
        assertNotNull(types);
        assertEquals(0, types.length);

        // Single value
        types = FlowableEngineEventType.getTypesFromString("ENTITY_CREATED");
        assertNotNull(types);
        assertEquals(1, types.length);
        assertEquals(FlowableEngineEventType.ENTITY_CREATED, types[0]);

        // Multiple value
        types = FlowableEngineEventType.getTypesFromString("ENTITY_CREATED,ENTITY_DELETED");
        assertNotNull(types);
        assertEquals(2, types.length);
        assertEquals(FlowableEngineEventType.ENTITY_CREATED, types[0]);
        assertEquals(FlowableEngineEventType.ENTITY_DELETED, types[1]);

        // Additional separators should be ignored
        types = FlowableEngineEventType.getTypesFromString(",ENTITY_CREATED,,ENTITY_DELETED,,,");
        assertNotNull(types);
        assertEquals(2, types.length);
        assertEquals(FlowableEngineEventType.ENTITY_CREATED, types[0]);
        assertEquals(FlowableEngineEventType.ENTITY_DELETED, types[1]);

        // Invalid type name
        try {
            FlowableEngineEventType.getTypesFromString("WHOOPS,ENTITY_DELETED");
            fail("Exception expected");
        } catch (FlowableIllegalArgumentException expected) {
            // Expected exception
            assertEquals("Invalid event-type: WHOOPS", expected.getMessage());
        }
    }
}
