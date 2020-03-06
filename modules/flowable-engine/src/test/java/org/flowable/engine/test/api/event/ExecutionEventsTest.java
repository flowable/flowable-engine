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

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testExecutionEvents() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        assertThat(processInstance).isNotNull();

        // Check create-event
        assertThat(listener.getEventsReceived()).hasSize(6);
        assertThat(listener.getEventsReceived().get(0)).isInstanceOf(FlowableEntityEvent.class);

        FlowableEntityEvent event = (FlowableEntityEvent) listener.getEventsReceived().get(0);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
        assertThat(((Execution) event.getEntity()).getProcessInstanceId()).isEqualTo(processInstance.getId());

        event = (FlowableEntityEvent) listener.getEventsReceived().get(1);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.PROCESS_CREATED);
        assertThat(((Execution) event.getEntity()).getProcessInstanceId()).isEqualTo(processInstance.getId());

        event = (FlowableEntityEvent) listener.getEventsReceived().get(2);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_INITIALIZED);
        assertThat(((Execution) event.getEntity()).getProcessInstanceId()).isEqualTo(processInstance.getId());

        event = (FlowableEntityEvent) listener.getEventsReceived().get(3);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
        assertThat(((Execution) event.getEntity()).getProcessInstanceId()).isEqualTo(processInstance.getId());

        event = (FlowableEntityEvent) listener.getEventsReceived().get(4);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_INITIALIZED);
        assertThat(((Execution) event.getEntity()).getProcessInstanceId()).isEqualTo(processInstance.getId());
        listener.clearEventsReceived();

        // Check update event when suspended/activated
        runtimeService.suspendProcessInstanceById(processInstance.getId());
        runtimeService.activateProcessInstanceById(processInstance.getId());

        assertThat(listener.getEventsReceived()).hasSize(4);
        event = (FlowableEntityEvent) listener.getEventsReceived().get(0);
        assertThat(((Execution) event.getEntity()).getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_SUSPENDED);

        event = (FlowableEntityEvent) listener.getEventsReceived().get(1);
        assertThat(((Execution) event.getEntity()).getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_SUSPENDED);

        event = (FlowableEntityEvent) listener.getEventsReceived().get(2);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_ACTIVATED);
        assertThat(((Execution) event.getEntity()).getProcessInstanceId()).isEqualTo(processInstance.getId());

        event = (FlowableEntityEvent) listener.getEventsReceived().get(3);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_ACTIVATED);
        assertThat(((Execution) event.getEntity()).getProcessInstanceId()).isEqualTo(processInstance.getId());

        listener.clearEventsReceived();

        // Check update event when process-definition is suspended (should
        // cascade suspend/activate all process instances)
        repositoryService.suspendProcessDefinitionById(processInstance.getProcessDefinitionId(), true, null);
        repositoryService.activateProcessDefinitionById(processInstance.getProcessDefinitionId(), true, null);

        assertThat(listener.getEventsReceived()).hasSize(4);

        event = (FlowableEntityEvent) listener.getEventsReceived().get(0);
        assertThat(((Execution) event.getEntity()).getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_SUSPENDED);

        event = (FlowableEntityEvent) listener.getEventsReceived().get(1);
        assertThat(((Execution) event.getEntity()).getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_SUSPENDED);

        event = (FlowableEntityEvent) listener.getEventsReceived().get(2);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_ACTIVATED);
        assertThat(((Execution) event.getEntity()).getProcessInstanceId()).isEqualTo(processInstance.getId());

        event = (FlowableEntityEvent) listener.getEventsReceived().get(3);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_ACTIVATED);
        assertThat(((Execution) event.getEntity()).getProcessInstanceId()).isEqualTo(processInstance.getId());

        listener.clearEventsReceived();

        // Check update-event when business-key is updated
        runtimeService.updateBusinessKey(processInstance.getId(), "thekey");
        assertThat(listener.getEventsReceived()).hasSize(1);
        event = (FlowableEntityEvent) listener.getEventsReceived().get(0);
        assertThat(((Execution) event.getEntity()).getId()).isEqualTo(processInstance.getId());
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_UPDATED);
        listener.clearEventsReceived();

        runtimeService.deleteProcessInstance(processInstance.getId(), "Testing events");

        event = (FlowableEntityEvent) listener.getEventsReceived().get(0);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_DELETED);
        assertThat(((Execution) event.getEntity()).getProcessInstanceId()).isEqualTo(processInstance.getId());
        listener.clearEventsReceived();
    }

    @BeforeEach
    protected void setUp() {

        listener = new TestFlowableEntityEventListener(Execution.class);
        processEngineConfiguration.getEventDispatcher().addEventListener(listener);
    }

    @AfterEach
    protected void tearDown() throws Exception {

        if (listener != null) {
            listener.clearEventsReceived();
            processEngineConfiguration.getEventDispatcher().removeEventListener(listener);
        }
    }
}
