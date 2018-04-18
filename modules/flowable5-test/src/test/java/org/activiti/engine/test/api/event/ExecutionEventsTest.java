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
package org.activiti.engine.test.api.event;

import org.activiti.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;

/**
 * Test case for all {@link FlowableEvent}s related to executions.
 * 
 * @author Frederik Heremans
 */
public class ExecutionEventsTest extends PluggableFlowableTestCase {

    private TestFlowableEntityEventListener listener;

    /**
     * Test create, update and delete events of process instances.
     */
    @Deployment(resources = { "org/activiti/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testExecutionEvents() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        assertNotNull(processInstance);

        // Check create-event
        assertEquals(3, listener.getEventsReceived().size());
        assertTrue(listener.getEventsReceived().get(0) instanceof FlowableEntityEvent);

        FlowableEntityEvent event = (FlowableEntityEvent) listener.getEventsReceived().get(0);
        assertEquals(FlowableEngineEventType.ENTITY_CREATED, event.getType());
        assertEquals(processInstance.getId(), ((org.activiti.engine.runtime.Execution) event.getEntity()).getId());

        event = (FlowableEntityEvent) listener.getEventsReceived().get(1);
        assertEquals(FlowableEngineEventType.ENTITY_INITIALIZED, event.getType());
        assertEquals(processInstance.getId(), ((org.activiti.engine.runtime.Execution) event.getEntity()).getId());
        listener.clearEventsReceived();

        // Check update event when suspended/activated
        runtimeService.suspendProcessInstanceById(processInstance.getId());
        runtimeService.activateProcessInstanceById(processInstance.getId());

        assertEquals(2, listener.getEventsReceived().size());
        event = (FlowableEntityEvent) listener.getEventsReceived().get(0);
        assertEquals(processInstance.getId(), ((org.activiti.engine.runtime.Execution) event.getEntity()).getId());
        assertEquals(FlowableEngineEventType.ENTITY_SUSPENDED, event.getType());
        event = (FlowableEntityEvent) listener.getEventsReceived().get(1);
        assertEquals(FlowableEngineEventType.ENTITY_ACTIVATED, event.getType());
        assertEquals(processInstance.getId(), ((org.activiti.engine.runtime.Execution) event.getEntity()).getId());
        listener.clearEventsReceived();

        // Check update event when process-definition is suspended (should cascade suspend/activate all process instances)
        repositoryService.suspendProcessDefinitionById(processInstance.getProcessDefinitionId(), true, null);
        repositoryService.activateProcessDefinitionById(processInstance.getProcessDefinitionId(), true, null);

        assertEquals(2, listener.getEventsReceived().size());
        event = (FlowableEntityEvent) listener.getEventsReceived().get(0);
        assertEquals(processInstance.getId(), ((org.activiti.engine.runtime.ProcessInstance) event.getEntity()).getId());
        assertEquals(FlowableEngineEventType.ENTITY_SUSPENDED, event.getType());
        event = (FlowableEntityEvent) listener.getEventsReceived().get(1);
        assertEquals(FlowableEngineEventType.ENTITY_ACTIVATED, event.getType());
        assertEquals(processInstance.getId(), ((org.activiti.engine.runtime.Execution) event.getEntity()).getId());
        listener.clearEventsReceived();

        // Check update-event when business-key is updated
        runtimeService.updateBusinessKey(processInstance.getId(), "thekey");
        assertEquals(1, listener.getEventsReceived().size());
        event = (FlowableEntityEvent) listener.getEventsReceived().get(0);
        assertEquals(processInstance.getId(), ((org.activiti.engine.runtime.Execution) event.getEntity()).getId());
        assertEquals(FlowableEngineEventType.ENTITY_UPDATED, event.getType());
        listener.clearEventsReceived();

        runtimeService.deleteProcessInstance(processInstance.getId(), "Testing events");

        event = (FlowableEntityEvent) listener.getEventsReceived().get(0);
        assertEquals(FlowableEngineEventType.ENTITY_DELETED, event.getType());
        assertEquals(processInstance.getId(), ((org.activiti.engine.runtime.Execution) event.getEntity()).getId());
        listener.clearEventsReceived();
    }

    @Override
    protected void initializeServices() {
        super.initializeServices();

        listener = new TestFlowableEntityEventListener(org.activiti.engine.runtime.Execution.class);
        processEngineConfiguration.getEventDispatcher().addEventListener(listener);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        if (listener != null) {
            listener.clearEventsReceived();
            processEngineConfiguration.getEventDispatcher().removeEventListener(listener);
        }
    }
}
