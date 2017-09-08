package org.flowable.engine.delegate.event;

import org.flowable.engine.common.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.engine.common.api.delegate.event.FlowableEngineEventType;
import org.flowable.engine.common.api.delegate.event.FlowableEvent;
import org.flowable.engine.common.api.delegate.event.FlowableEventListener;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;

public class EventHelperTest extends PluggableFlowableTestCase {

    private EventHelperListener listener;

    /**
     * Test create, update and delete events of process instances.
     */
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testExecutionEvents() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertNotNull(processInstance);
    }

    @Override
    protected void initializeServices() {
        super.initializeServices();

        listener = new EventHelperListener();
        processEngineConfiguration.getEventDispatcher().addEventListener(listener);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        if (listener != null) {
            processEngineConfiguration.getEventDispatcher().removeEventListener(listener);
        }
    }

    protected class EventHelperListener implements FlowableEventListener {
        @Override
        public void onEvent(FlowableEvent event) {
            if (event instanceof FlowableEngineEntityEvent && FlowableEngineEventType.PROCESS_CREATED == event.getType()) {
                assertNotNull(EventHelper.getExecution(((ProcessInstance) ((FlowableEngineEntityEvent) event).getEntity()).getId()));
            }
        }

        @Override
        public boolean isFailOnException() {
            return true;
        }
    }
}
