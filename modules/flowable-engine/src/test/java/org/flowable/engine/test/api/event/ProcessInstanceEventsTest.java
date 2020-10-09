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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.engine.delegate.event.AbstractFlowableEngineEventListener;
import org.flowable.engine.delegate.event.FlowableActivityCancelledEvent;
import org.flowable.engine.delegate.event.FlowableCancelledEvent;
import org.flowable.engine.delegate.event.FlowableProcessStartedEvent;
import org.flowable.engine.delegate.event.impl.FlowableActivityCancelledEventImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test case for all {@link FlowableEvent}s related to process instances.
 *
 * @author Tijs Rademakers
 */
public class ProcessInstanceEventsTest extends PluggableFlowableTestCase {

    private TestInitializedEntityEventListener listener;

    /**
     * Test create, update and delete events of process instances.
     */
    @Test
    @Deployment
    public void testProcessInstanceEvents() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        assertThat(processInstance).isNotNull();

        // Check create-event
        assertProcessStartedEvents(processInstance);

        FlowableEngineEntityEvent event;

        listener.clearEventsReceived();
        FilteredStaticTestFlowableEventListener.clearEventsReceived();

        // Check update event when suspended/activated
        runtimeService.suspendProcessInstanceById(processInstance.getId());
        runtimeService.activateProcessInstanceById(processInstance.getId());

        assertThat(listener.getEventsReceived()).hasSize(4);
        assertThat(FilteredStaticTestFlowableEventListener.getEventsReceived()).hasSize(4);

        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
        assertThat(((ProcessInstance) event.getEntity()).getId()).isEqualTo(processInstance.getId());
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_SUSPENDED);
        assertThat(event.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(event.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(event.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertEventsEqual(event, FilteredStaticTestFlowableEventListener.getEventsReceived().get(0));

        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(1);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_SUSPENDED);
        assertThat(event.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(processInstance.getId()).isNotEqualTo(event.getExecutionId());
        assertThat(event.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertEventsEqual(event, FilteredStaticTestFlowableEventListener.getEventsReceived().get(1));

        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(2);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_ACTIVATED);
        assertThat(((ProcessInstance) event.getEntity()).getId()).isEqualTo(processInstance.getId());
        assertThat(event.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(event.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(event.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertEventsEqual(event, FilteredStaticTestFlowableEventListener.getEventsReceived().get(2));

        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(3);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_ACTIVATED);
        assertThat(event.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(processInstance.getId()).isNotEqualTo(event.getExecutionId());
        assertThat(event.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertEventsEqual(event, FilteredStaticTestFlowableEventListener.getEventsReceived().get(3));

        listener.clearEventsReceived();
        FilteredStaticTestFlowableEventListener.clearEventsReceived();

        // Check update event when process-definition is suspended (should
        // cascade suspend/activate all process instances)
        repositoryService.suspendProcessDefinitionById(processInstance.getProcessDefinitionId(), true, null);
        repositoryService.activateProcessDefinitionById(processInstance.getProcessDefinitionId(), true, null);

        assertThat(listener.getEventsReceived()).hasSize(4);
        assertThat(FilteredStaticTestFlowableEventListener.getEventsReceived()).hasSize(4);

        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
        assertThat(((ProcessInstance) event.getEntity()).getId()).isEqualTo(processInstance.getId());
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_SUSPENDED);
        assertThat(event.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(event.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(event.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertEventsEqual(event, FilteredStaticTestFlowableEventListener.getEventsReceived().get(0));

        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(1);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_SUSPENDED);
        assertThat(event.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(processInstance.getId()).isNotEqualTo(event.getExecutionId());
        assertThat(event.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertEventsEqual(event, FilteredStaticTestFlowableEventListener.getEventsReceived().get(1));

        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(2);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_ACTIVATED);
        assertThat(((ProcessInstance) event.getEntity()).getId()).isEqualTo(processInstance.getId());
        assertThat(event.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(event.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(event.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertEventsEqual(event, FilteredStaticTestFlowableEventListener.getEventsReceived().get(2));

        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(3);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_ACTIVATED);
        assertThat(event.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(processInstance.getId()).isNotEqualTo(event.getExecutionId());
        assertThat(event.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertEventsEqual(event, FilteredStaticTestFlowableEventListener.getEventsReceived().get(3));

        listener.clearEventsReceived();
        FilteredStaticTestFlowableEventListener.clearEventsReceived();

        // Check update-event when business-key is updated
        runtimeService.updateBusinessKey(processInstance.getId(), "thekey");
        assertThat(listener.getEventsReceived()).hasSize(1);
        assertThat(FilteredStaticTestFlowableEventListener.getEventsReceived()).hasSize(1);

        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
        assertThat(((ProcessInstance) event.getEntity()).getId()).isEqualTo(processInstance.getId());
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_UPDATED);
        assertThat(event.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(event.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(event.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertEventsEqual(event, FilteredStaticTestFlowableEventListener.getEventsReceived().get(0));
        listener.clearEventsReceived();
        FilteredStaticTestFlowableEventListener.clearEventsReceived();

        runtimeService.deleteProcessInstance(processInstance.getId(), "Testing events");

        List<FlowableEvent> processCancelledEvents = listener.filterEvents(FlowableEngineEventType.PROCESS_CANCELLED);
        assertThat(processCancelledEvents).hasSize(1);
        FlowableCancelledEvent cancelledEvent = (FlowableCancelledEvent) processCancelledEvents.get(0);
        assertThat(cancelledEvent.getType()).isEqualTo(FlowableEngineEventType.PROCESS_CANCELLED);
        assertThat(cancelledEvent.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(cancelledEvent.getExecutionId()).isEqualTo(processInstance.getId());
        assertEventsEqual(cancelledEvent, FilteredStaticTestFlowableEventListener.filterEvents(FlowableEngineEventType.PROCESS_CANCELLED).get(0));
        listener.clearEventsReceived();
        FilteredStaticTestFlowableEventListener.clearEventsReceived();
    }

    protected void assertProcessStartedEvents(ProcessInstance processInstance) {
        assertThat(listener.getEventsReceived()).hasSize(6);
        assertThat(FilteredStaticTestFlowableEventListener.getEventsReceived()).hasSize(6);
        assertThat(listener.getEventsReceived().get(0)).isInstanceOf(FlowableEngineEntityEvent.class);

        // process instance create event
        FlowableEngineEntityEvent event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
        assertThat(((ProcessInstance) event.getEntity()).getId()).isEqualTo(processInstance.getId());
        assertThat(event.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(event.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(event.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertEventsEqual(event, FilteredStaticTestFlowableEventListener.getEventsReceived().get(0));

        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(1);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.PROCESS_CREATED);
        assertThat(event.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(event.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(event.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertEventsEqual(event, FilteredStaticTestFlowableEventListener.getEventsReceived().get(1));

        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(2);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_INITIALIZED);
        assertThat(event.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(event.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(event.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertEventsEqual(event, FilteredStaticTestFlowableEventListener.getEventsReceived().get(2));

        // start event create event
        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(3);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
        assertThat(event.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(processInstance.getId()).isNotEqualTo(event.getExecutionId());
        assertThat(event.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertEventsEqual(event, FilteredStaticTestFlowableEventListener.getEventsReceived().get(3));

        // start event create initialized
        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(4);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_INITIALIZED);
        assertThat(event.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(processInstance.getId()).isNotEqualTo(event.getExecutionId());
        assertThat(event.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertEventsEqual(event, FilteredStaticTestFlowableEventListener.getEventsReceived().get(4));

        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(5);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.PROCESS_STARTED);
        assertThat(event.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(event.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertThat(event).isInstanceOf(FlowableProcessStartedEvent.class);
        assertThat(((FlowableProcessStartedEvent) event).getNestedProcessDefinitionId()).isNull();
        assertThat(((FlowableProcessStartedEvent) event).getNestedProcessInstanceId()).isNull();
        assertEventsEqual(event, FilteredStaticTestFlowableEventListener.getEventsReceived().get(5));
    }

    /**
     * Test create, update and delete events of process instances.
     */
    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/nestedSubProcess.bpmn20.xml",
            "org/flowable/engine/test/api/runtime/subProcess.bpmn20.xml" })
    public void testSubProcessInstanceEvents() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("nestedSimpleSubProcess");
        assertThat(processInstance).isNotNull();
        String processDefinitionId = processInstance.getProcessDefinitionId();

        // Check create-event one main process the second one Scope execution, and the third one subprocess
        assertThat(listener.getEventsReceived()).hasSize(12);
        assertThat(listener.getEventsReceived().get(0)).isInstanceOf(FlowableEngineEntityEvent.class);

        // process instance created event
        FlowableEngineEntityEvent event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
        assertThat(((ProcessInstance) event.getEntity()).getId()).isEqualTo(processInstance.getId());
        assertThat(event.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(event.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(event.getProcessDefinitionId()).isEqualTo(processDefinitionId);

        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(1);
        String processExecutionId = event.getExecutionId();
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.PROCESS_CREATED);
        assertThat(event.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(processExecutionId).isEqualTo(processInstance.getId());
        assertThat(event.getProcessDefinitionId()).isEqualTo(processDefinitionId);

        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(2);
        processExecutionId = event.getExecutionId();
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_INITIALIZED);
        assertThat(event.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(processExecutionId).isEqualTo(processInstance.getId());
        assertThat(event.getProcessDefinitionId()).isEqualTo(processDefinitionId);

        // start event created event
        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(3);
        processExecutionId = event.getExecutionId();
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
        assertThat(event.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(processInstance.getId()).isNotEqualTo(processExecutionId);
        assertThat(event.getProcessDefinitionId()).isEqualTo(processDefinitionId);

        // start event initialized event
        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(4);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_INITIALIZED);
        assertThat(event.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(processInstance.getId()).isNotEqualTo(((ExecutionEntity) event.getEntity()).getId());

        // Process start
        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(5);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.PROCESS_STARTED);
        assertThat(event.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(event.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertThat(event).isInstanceOf(FlowableProcessStartedEvent.class);
        assertThat(((FlowableProcessStartedEvent) event).getNestedProcessDefinitionId()).isNull();
        assertThat(((FlowableProcessStartedEvent) event).getNestedProcessInstanceId()).isNull();

        // sub process instance created event
        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(6);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
        ExecutionEntity subProcessEntity = (ExecutionEntity) event.getEntity();
        assertThat(subProcessEntity.getSuperExecutionId()).isEqualTo(processExecutionId);
        String subProcessInstanceId = subProcessEntity.getProcessInstanceId();

        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(7);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.PROCESS_CREATED);
        subProcessEntity = (ExecutionEntity) event.getEntity();
        assertThat(subProcessEntity.getSuperExecutionId()).isEqualTo(processExecutionId);

        // sub process instance initialized event
        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(8);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_INITIALIZED);
        assertThat(event.getExecutionId()).isEqualTo(subProcessInstanceId);
        String subProcessDefinitionId = ((ExecutionEntity) event.getEntity()).getProcessDefinitionId();
        assertThat(subProcessDefinitionId).isNotNull();

        // sub process instance child execution created event
        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(9);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
        assertThat(event.getProcessInstanceId()).isEqualTo(subProcessInstanceId);
        assertThat(subProcessInstanceId).isNotEqualTo(event.getExecutionId());
        subProcessDefinitionId = ((ExecutionEntity) event.getEntity()).getProcessDefinitionId();
        assertThat(subProcessDefinitionId).isNotNull();
        ProcessDefinition subProcessDefinition = repositoryService.getProcessDefinition(subProcessDefinitionId);
        assertThat(subProcessDefinition.getKey()).isEqualTo("simpleSubProcess");

        // sub process instance child execution initialized event
        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(10);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_INITIALIZED);
        assertThat(event.getProcessInstanceId()).isEqualTo(subProcessInstanceId);
        assertThat(subProcessInstanceId).isNotEqualTo(event.getExecutionId());
        subProcessDefinitionId = ((ExecutionEntity) event.getEntity()).getProcessDefinitionId();
        assertThat(subProcessDefinitionId).isNotNull();

        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(11);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.PROCESS_STARTED);
        assertThat(event.getProcessInstanceId()).isEqualTo(subProcessInstanceId);
        assertThat(event.getProcessDefinitionId()).isEqualTo(subProcessDefinitionId);
        assertThat(event).isInstanceOf(FlowableProcessStartedEvent.class);
        assertThat(((FlowableProcessStartedEvent) event).getNestedProcessDefinitionId()).isEqualTo(processDefinitionId);
        assertThat(((FlowableProcessStartedEvent) event).getNestedProcessInstanceId()).isEqualTo(processInstance.getId());

        listener.clearEventsReceived();
    }

    /**
     * Test process with signals start.
     */
    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/signal/SignalEventTest.testSignalWithGlobalScope.bpmn20.xml" })
    public void testSignalProcessInstanceStart() throws Exception {
        this.runtimeService.startProcessInstanceByKey("processWithSignalCatch");
        listener.clearEventsReceived();

        runtimeService.startProcessInstanceByKey("processWithSignalThrow");
        listener.clearEventsReceived();
    }

    /**
     * Test Start->End process on PROCESS_COMPLETED event
     */
    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/event/ProcessInstanceEventsTest.noneTaskProcess.bpmn20.xml" })
    public void testProcessCompleted_StartEnd() throws Exception {
        this.runtimeService.startProcessInstanceByKey("noneTaskProcess");

        assertThat(listener.filterEvents(FlowableEngineEventType.PROCESS_COMPLETED)).as("ActivitiEventType.PROCESS_COMPLETED was expected 1 time.").hasSize(1);
    }

    /**
     * Test Start->User org.flowable.task.service.Task process on PROCESS_COMPLETED event
     */
    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/event/ProcessInstanceEventsTest.noEndProcess.bpmn20.xml" })
    public void testProcessCompleted_NoEnd() throws Exception {
        ProcessInstance noEndProcess = this.runtimeService.startProcessInstanceByKey("noEndProcess");
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(noEndProcess.getId()).singleResult();
        taskService.complete(task.getId());

        assertThat(listener.filterEvents(FlowableEngineEventType.PROCESS_COMPLETED)).as("ActivitiEventType.PROCESS_COMPLETED was expected 1 time.").hasSize(1);
    }

    /**
     * Test +-->Task1 Start-<> +-->Task1
     * <p>
     * process on PROCESS_COMPLETED event
     */
    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/event/ProcessInstanceEventsTest.parallelGatewayNoEndProcess.bpmn20.xml" })
    public void testProcessCompleted_ParallelGatewayNoEnd() throws Exception {
        this.runtimeService.startProcessInstanceByKey("noEndProcess");

        assertThat(listener.filterEvents(FlowableEngineEventType.PROCESS_COMPLETED)).as("ActivitiEventType.PROCESS_COMPLETED was expected 1 time.").hasSize(1);
    }

    /**
     * <p>
     * Test +-->End1 Start-<> +-->End2
     * </p>
     * process on PROCESS_COMPLETED event
     */
    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/event/ProcessInstanceEventsTest.parallelGatewayTwoEndsProcess.bpmn20.xml" })
    public void testProcessCompleted_ParallelGatewayTwoEnds() throws Exception {
        this.runtimeService.startProcessInstanceByKey("noEndProcess");

        List<FlowableEvent> events = listener.filterEvents(FlowableEngineEventType.PROCESS_COMPLETED);
        assertThat(events).as("ActivitiEventType.PROCESS_COMPLETED was expected 1 time.").hasSize(1);
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.testTerminateInCallActivityMulitInstance.bpmn",
            "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.subProcessTerminateTerminateAll.bpmn20.xml" })
    public void testProcessCompleted_TerminateInCallActivityMultiInstanceTerminateAll() throws Exception {
        runtimeService.startProcessInstanceByKey("terminateEndEventExample");

        List<FlowableEvent> events = listener.filterEvents(FlowableEngineEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT);
        assertThat(events).as("FlowableEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT was expected 6 times.").hasSize(6);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testProcessInstanceCancelledEvents_cancel() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertThat(processInstance).isNotNull();
        listener.clearEventsReceived();

        runtimeService.deleteProcessInstance(processInstance.getId(), "delete_test");

        List<FlowableEvent> processCancelledEvents = listener.filterEvents(FlowableEngineEventType.PROCESS_CANCELLED);
        assertThat(processCancelledEvents).as("ActivitiEventType.PROCESS_CANCELLED was expected 1 time.").hasSize(1);
        FlowableCancelledEvent processCancelledEvent = (FlowableCancelledEvent) processCancelledEvents.get(0);
        assertThat(processCancelledEvent.getProcessInstanceId()).as("The process instance has to be the same as in deleteProcessInstance method call")
                .isEqualTo(processInstance.getId());
        assertThat(processCancelledEvent.getExecutionId()).as("The execution instance has to be the same as in deleteProcessInstance method call")
                .isEqualTo(processInstance.getId());
        assertThat(processCancelledEvent.getCause()).as("The cause has to be the same as in deleteProcessInstance method call").isEqualTo("delete_test");

        List<FlowableEvent> taskCancelledEvents = listener.filterEvents(FlowableEngineEventType.ACTIVITY_CANCELLED);
        assertThat(taskCancelledEvents).as("ActivitiEventType.ACTIVITY_CANCELLED was expected 1 time.").hasSize(1);
        FlowableActivityCancelledEvent activityCancelledEvent = (FlowableActivityCancelledEvent) taskCancelledEvents.get(0);
        assertThat(activityCancelledEvent.getProcessInstanceId()).as("The process instance has to be the same as in deleteProcessInstance method call")
                .isEqualTo(processInstance.getId());
        assertThat(activityCancelledEvent.getCause()).as("The cause has to be the same as in deleteProcessInstance method call").isEqualTo("delete_test");

        listener.clearEventsReceived();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/nestedSubProcess.bpmn20.xml",
            "org/flowable/engine/test/api/runtime/subProcess.bpmn20.xml" })
    public void testProcessInstanceCancelledEvents_cancelProcessHierarchy() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("nestedSimpleSubProcess");
        ProcessInstance subProcess = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
        assertThat(processInstance).isNotNull();
        listener.clearEventsReceived();

        runtimeService.deleteProcessInstance(processInstance.getId(), "delete_test");

        List<FlowableEvent> processCancelledEvents = listener.filterEvents(FlowableEngineEventType.PROCESS_CANCELLED);
        assertThat(processCancelledEvents).as("ActivitiEventType.PROCESS_CANCELLED was expected 2 times.").hasSize(2);
        FlowableCancelledEvent processCancelledEvent = (FlowableCancelledEvent) processCancelledEvents.get(0);
        assertThat(processCancelledEvent.getProcessInstanceId()).as("The process instance has to be the same as in deleteProcessInstance method call")
                .isEqualTo(subProcess.getId());
        assertThat(processCancelledEvent.getExecutionId()).as("The execution instance has to be the same as in deleteProcessInstance method call")
                .isEqualTo(subProcess.getId());
        assertThat(processCancelledEvent.getCause()).as("The cause has to be the same as in deleteProcessInstance method call").isEqualTo("delete_test");

        processCancelledEvent = (FlowableCancelledEvent) processCancelledEvents.get(1);
        assertThat(processCancelledEvent.getProcessInstanceId()).as("The process instance has to be the same as in deleteProcessInstance method call")
                .isEqualTo(processInstance.getId());
        assertThat(processCancelledEvent.getExecutionId()).as("The execution instance has to be the same as in deleteProcessInstance method call")
                .isEqualTo(processInstance.getId());
        assertThat(processCancelledEvent.getCause()).as("The cause has to be the same as in deleteProcessInstance method call").isEqualTo("delete_test");

        assertThat(this.taskService.createTaskQuery().processInstanceId(processInstance.getId()).count()).as("No task can be active for deleted process.")
                .isZero();

        List<FlowableEvent> taskCancelledEvents = listener.filterEvents(FlowableEngineEventType.ACTIVITY_CANCELLED);
        assertThat(taskCancelledEvents).as("ActivitiEventType.ACTIVITY_CANCELLED was expected 2 times.").hasSize(2);

        FlowableActivityCancelledEvent activityCancelledEvent = (FlowableActivityCancelledEvent) taskCancelledEvents.get(0);
        assertThat(activityCancelledEvent.getProcessInstanceId()).as("The process instance has to point to the subprocess").isEqualTo(subProcess.getId());
        assertThat(activityCancelledEvent.getCause()).as("The cause has to be the same as in deleteProcessInstance method call").isEqualTo("delete_test");

        activityCancelledEvent = (FlowableActivityCancelledEvent) taskCancelledEvents.get(1);
        assertThat(activityCancelledEvent.getProcessInstanceId()).as("The process instance has to point to the main process")
                .isEqualTo(processInstance.getId());
        assertThat(activityCancelledEvent.getActivityType()).as("expect callActivity type").isEqualTo("callActivity");
        assertThat(activityCancelledEvent.getCause()).as("The cause has to be the same as in deleteProcessInstance method call").isEqualTo("delete_test");

        listener.clearEventsReceived();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testProcessInstanceCancelledEvents_complete() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertThat(processInstance).isNotNull();

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        List<FlowableEvent> processCancelledEvents = listener.filterEvents(FlowableEngineEventType.PROCESS_CANCELLED);
        assertThat(processCancelledEvents).as("There should be no FlowableEventType.PROCESS_CANCELLED event after process complete.").isEmpty();
        List<FlowableEvent> taskCancelledEvents = listener.filterEvents(FlowableEngineEventType.ACTIVITY_CANCELLED);
        assertThat(taskCancelledEvents).as("There should be no FlowableEventType.ACTIVITY_CANCELLED event.").isEmpty();

    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testProcessInstanceTerminatedEvents_complete() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertThat(processInstance).isNotNull();

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        List<FlowableEvent> processTerminatedEvents = listener.filterEvents(FlowableEngineEventType.PROCESS_CANCELLED);
        assertThat(processTerminatedEvents).as("There should be no FlowableEventType.PROCESS_TERMINATED event after process complete.").isEmpty();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.testProcessTerminate.bpmn")
    public void testProcessInstanceTerminatedEvents() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

        long executionEntities = runtimeService.createExecutionQuery().processInstanceId(pi.getId()).count();
        assertThat(executionEntities).isEqualTo(3);

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preTerminateTask").singleResult();
        taskService.complete(task.getId());

        List<FlowableEvent> processTerminatedEvents = listener.filterEvents(FlowableEngineEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT);
        assertThat(processTerminatedEvents)
                .as("There should be exactly one FlowableEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT event after the task complete.")
                .hasSize(1);
        FlowableEngineEntityEvent processCompletedEvent = (FlowableEngineEntityEvent) processTerminatedEvents.get(0);
        assertThat(processCompletedEvent.getProcessInstanceId()).isEqualTo(pi.getProcessInstanceId());

        List<FlowableEvent> activityTerminatedEvents = listener.filterEvents(FlowableEngineEventType.ACTIVITY_CANCELLED);
        assertThat(activityTerminatedEvents)
                .as("There should be exactly two FlowableEventType.ACTIVITY_CANCELLED event after the task complete.")
                .hasSize(1);

        for (FlowableEvent event : activityTerminatedEvents) {

            FlowableActivityCancelledEventImpl activityEvent = (FlowableActivityCancelledEventImpl) event;
            if ("preNormalTerminateTask".equals(activityEvent.getActivityId())) {
                assertThat(activityEvent.getActivityId())
                        .as("The user task must be terminated")
                        .isEqualTo("preNormalTerminateTask");
                assertThat(((FlowNode) activityEvent.getCause()).getId())
                        .as("The cause must be terminate end event")
                        .isEqualTo("EndEvent_2");
            }
        }

    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.testTerminateInCallActivity.bpmn",
            "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.subProcessTerminate.bpmn" })
    public void testProcessInstanceTerminatedEvents_callActivity() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preNormalEnd").singleResult();
        taskService.complete(task.getId());

        assertProcessEnded(pi.getId());
        List<FlowableEvent> processTerminatedEvents = listener.filterEvents(FlowableEngineEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT);
        assertThat(processTerminatedEvents)
                .as("There should be exactly one FlowableEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT event after the task complete.")
                .hasSize(1);
        FlowableEngineEntityEvent processCompletedEvent = (FlowableEngineEntityEvent) processTerminatedEvents.get(0);
        assertThat(pi.getProcessInstanceId()).isNotEqualTo(processCompletedEvent.getProcessInstanceId());
        assertThat(processCompletedEvent.getProcessDefinitionId()).contains("terminateEndEventSubprocessExample");

    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.testTerminateInSubProcessWithBoundaryTerminateAll.bpmn20.xml" })
    public void testTerminateAllInSubProcess() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventWithBoundary");

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preTermInnerTask").singleResult();
        taskService.complete(task.getId());

        assertProcessEnded(pi.getId());
        List<FlowableEvent> processTerminatedEvents = listener.filterEvents(FlowableEngineEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT);
        assertThat(processTerminatedEvents)
                .as("There should be exactly one FlowableEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT event after the task complete.")
                .hasSize(1);
        FlowableEngineEntityEvent processCompletedEvent = (FlowableEngineEntityEvent) processTerminatedEvents.get(0);
        assertThat(processCompletedEvent.getProcessInstanceId()).isEqualTo(pi.getProcessInstanceId());

    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.testTerminateInParentProcess.bpmn",
            "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testProcessInstanceTerminatedEvents_terminateInParentProcess() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateParentProcess");

        // should terminate the called process and continue the parent
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preTerminateEnd").singleResult();
        taskService.complete(task.getId());

        assertProcessEnded(pi.getId());
        List<FlowableEvent> processTerminatedEvents = listener.filterEvents(FlowableEngineEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT);
        assertThat(processTerminatedEvents)
                .as("There should be exactly one FlowableEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT event after the task complete.")
                .hasSize(1);
        FlowableEngineEntityEvent processCompletedEvent = (FlowableEngineEntityEvent) processTerminatedEvents.get(0);
        assertThat(processCompletedEvent.getProcessInstanceId()).isEqualTo(pi.getProcessInstanceId());
        assertThat(processCompletedEvent.getProcessDefinitionId()).contains("terminateParentProcess");

        List<FlowableEvent> activityTerminatedEvents = listener.filterEvents(FlowableEngineEventType.ACTIVITY_CANCELLED);
        assertThat(activityTerminatedEvents)
                .as("Two activities must be cancelled.")
                .hasSize(2);

        for (FlowableEvent event : activityTerminatedEvents) {

            FlowableActivityCancelledEventImpl activityEvent = (FlowableActivityCancelledEventImpl) event;

            if ("theTask".equals(activityEvent.getActivityId())) {

                assertThat(activityEvent.getActivityId())
                        .as("The user task must be terminated in the called sub process.")
                        .isEqualTo("theTask");
                assertThat(((FlowNode) activityEvent.getCause()).getId())
                        .as("The cause must be terminate end event")
                        .isEqualTo("EndEvent_3");

            } else if ("CallActivity_1".equals(activityEvent.getActivityId())) {

                assertThat(activityEvent.getActivityId())
                        .as("The call activity must be terminated")
                        .isEqualTo("CallActivity_1");
                assertThat(((FlowNode) activityEvent.getCause()).getId())
                        .as("The cause must be terminate end event")
                        .isEqualTo("EndEvent_3");
            }

        }

    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/bpmn/event/error/BoundaryErrorEventTest.testCatchErrorOnCallActivity-parent.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/error/BoundaryErrorEventTest.subprocess.bpmn20.xml"
    })
    public void testProcessCompletedEvents_callActivityErrorEndEvent() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("catchErrorOnCallActivity");

        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("Task in subprocess");
        List<ProcessInstance> subProcesses = runtimeService.createProcessInstanceQuery().superProcessInstanceId(pi.getId()).list();
        assertThat(subProcesses).hasSize(1);

        // Completing the task will reach the end error event,
        // which is caught on the call activity boundary
        taskService.complete(task.getId());

        List<FlowableEvent> processCompletedEvents = listener.filterEvents(FlowableEngineEventType.PROCESS_COMPLETED_WITH_ERROR_END_EVENT);
        assertThat(processCompletedEvents)
                .as("There should be exactly an FlowableEventType.PROCESS_COMPLETED_WITH_ERROR_END_EVENT event after the task complete.")
                .hasSize(1);
        FlowableEngineEntityEvent processCompletedEvent = (FlowableEngineEntityEvent) processCompletedEvents.get(0);
        assertThat(processCompletedEvent.getExecutionId()).isEqualTo(subProcesses.get(0).getId());

        task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("Escalated Task");

        // Completing the task will end the process instance
        taskService.complete(task.getId());
        assertProcessEnded(pi.getId());
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testParallelCallActivity.bpmn20.xml",
            "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.externalSubProcess.bpmn20.xml" })
    public void testDeleteMultiInstanceCallActivityProcessInstance() {
        assertThat(taskService.createTaskQuery().count()).isZero();
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("miParallelCallActivity");
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(7);
        assertThat(taskService.createTaskQuery().count()).isEqualTo(12);
        this.listener.clearEventsReceived();

        runtimeService.deleteProcessInstance(processInstance.getId(), "testing instance deletion");

        assertThat(this.listener.getEventsReceived().get(0).getType()).as("Task cancelled event has to be fired.")
                .isEqualTo(FlowableEngineEventType.ACTIVITY_CANCELLED);
        assertThat(this.listener.getEventsReceived().get(2).getType()).as("SubProcess cancelled event has to be fired.")
                .isEqualTo(FlowableEngineEventType.PROCESS_CANCELLED);
        assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
        assertThat(taskService.createTaskQuery().count()).isZero();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/runtime/subProcessWithTerminateEnd.bpmn20.xml")
    public void testProcessInstanceTerminatedEventInSubProcess() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("subProcessWithTerminateEndTest");

        long executionEntities = runtimeService.createExecutionQuery().processInstanceId(pi.getId()).count();
        assertThat(executionEntities).isEqualTo(4);

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).list();
        assertThat(tasks).hasSize(1);

        Execution execution = runtimeService.createExecutionQuery().messageEventSubscriptionName("cancel").singleResult();
        assertThat(execution).isNotNull();

        // message received cancels the SubProcess. We expect an event for all flow elements
        // when the process state changes. We expect the activity cancelled event for the task within the
        // Subprocess and the SubProcess itself
        runtimeService.messageEventReceived("cancel", execution.getId());

        List<FlowableEvent> activityTerminatedEvents = listener.filterEvents(FlowableEngineEventType.ACTIVITY_CANCELLED);
        assertThat(activityTerminatedEvents).hasSize(2);

        boolean taskFound = false;
        boolean subProcessFound = false;
        for (FlowableEvent terminatedEvent : activityTerminatedEvents) {
            FlowableActivityCancelledEvent activityEvent = (FlowableActivityCancelledEvent) terminatedEvent;
            if ("userTask".equals(activityEvent.getActivityType())) {
                taskFound = true;
                assertThat(activityEvent.getActivityId()).isEqualTo("task");

            } else if ("subProcess".equals(activityEvent.getActivityType())) {
                subProcessFound = true;
                assertThat(activityEvent.getActivityId()).isEqualTo("embeddedSubprocess");
            }
        }

        assertThat(taskFound).isTrue();
        assertThat(subProcessFound).isTrue();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/runtime/multipleSubprocessTerminateEnd.bpmn20.xml")
    public void testProcessInstanceWithMultipleSubprocessAndTerminateEnd2() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("multiplesubProcessWithTerminateEndTest");

        List<Execution> subprocesses = runtimeService.createExecutionQuery().processInstanceId(pi.getId())
                .onlySubProcessExecutions().list();
        assertThat(subprocesses).hasSize(2);

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).list();
        assertThat(tasks).hasSize(2);

        org.flowable.task.api.Task task2 = null;
        for (org.flowable.task.api.Task task : tasks) {
            if ("Task in subprocess2".equals(task.getName())) {
                task2 = task;
                break;

            }
        }

        // Complete user task in subprocess2. This flows out of subprocess2 to
        // the terminate end event. This will cause subprocess1 to be cancelled along
        // with the user task, boundary event and intermediate catch event defined in or
        // on subprocess1.
        assertThat(task2).isNotNull();
        taskService.complete(task2.getId());

        // Subprocess2 completed and transitioned to terminate end. We expect
        // ACTIVITY_CANCELLED for Subprocess1, task1 defined in subprocess1, boundary event defined on
        // and the timer intermediate catch event defined in subprocess1
        boolean userTaskFound = false;
        boolean subprocessFound = false;
        boolean timerCatchEventFound = false;
        boolean boundaryEventFound = false;
        List<FlowableEvent> activityTerminatedEvents = listener
                .filterEvents(FlowableEngineEventType.ACTIVITY_CANCELLED);
        assertThat(activityTerminatedEvents).hasSize(4);
        for (FlowableEvent flowableEvent : activityTerminatedEvents) {
            FlowableActivityCancelledEvent activityCancelledEvent = (FlowableActivityCancelledEvent) flowableEvent;
            if ("intermediateCatchEvent".equals(activityCancelledEvent.getActivityType())) {
                assertThat(activityCancelledEvent.getActivityId()).isEqualTo("timer");
                timerCatchEventFound = true;
            } else if ("boundaryEvent".equals(activityCancelledEvent.getActivityType())) {
                boundaryEventFound = true;
            } else if ("userTask".equals(activityCancelledEvent.getActivityType())) {
                assertThat(activityCancelledEvent.getActivityName()).isEqualTo("Task in subprocess1");
                userTaskFound = true;
            } else if ("subProcess".equals(activityCancelledEvent.getActivityType())) {
                assertThat(activityCancelledEvent.getActivityId()).isEqualTo("subprocess1");
                subprocessFound = true;
            }
        }

        assertThat(timerCatchEventFound).isTrue();
        assertThat(boundaryEventFound).isTrue();
        assertThat(userTaskFound).isTrue();
        assertThat(subprocessFound).isTrue();

        List<FlowableEvent> processCompletedEvents = listener.filterEvents(FlowableEngineEventType.PROCESS_COMPLETED);
        assertThat(processCompletedEvents).isEmpty();

        List<FlowableEvent> processCompletedTerminateEndEvents = listener
                .filterEvents(FlowableEngineEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT);
        assertThat(processCompletedTerminateEndEvents).hasSize(1);

        // Only expect PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT, not
        // PROCESS_CANCELLED.
        List<FlowableEvent> processCanceledEvents = listener.filterEvents(FlowableEngineEventType.PROCESS_CANCELLED);
        assertThat(processCanceledEvents).isEmpty();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/event/ProcessInstanceEventsTest.testProcessInstanceEvents.bpmn20.xml")
    public void startAsyncProcessInstanceEvents() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("oneTaskProcess").startAsync();
        assertThat(processInstance).isNotNull();

        assertProcessStartedEvents(processInstance);
        listener.clearEventsReceived();
        FilteredStaticTestFlowableEventListener.clearEventsReceived();
    }

    private void assertEventsEqual(FlowableEvent event1, FlowableEvent event2) {
        assertThat(EqualsBuilder.reflectionEquals(event1, event2)).isTrue();

    }

    @BeforeEach
    protected void setUp() {
        this.listener = new TestInitializedEntityEventListener();
        processEngineConfiguration.getEventDispatcher().addEventListener(this.listener);
        FilteredStaticTestFlowableEventListener.clearEventsReceived();
    }

    @AfterEach
    protected void tearDown() throws Exception {

        if (listener != null) {
            listener.clearEventsReceived();
            processEngineConfiguration.getEventDispatcher().removeEventListener(listener);
        }
    }

    private class TestInitializedEntityEventListener extends AbstractFlowableEngineEventListener {

        private List<FlowableEvent> eventsReceived;

        public TestInitializedEntityEventListener() {

            eventsReceived = new ArrayList<>();
        }

        public List<FlowableEvent> getEventsReceived() {
            return eventsReceived;
        }

        public void clearEventsReceived() {
            eventsReceived.clear();
        }

        @Override
        public void onEvent(FlowableEvent event) {
            if (event instanceof FlowableEntityEvent && ProcessInstance.class.isAssignableFrom(((FlowableEntityEvent) event).getEntity().getClass())) {
                // check whether entity in the event is initialized before
                // adding to the list.
                assertThat(((ExecutionEntity) ((FlowableEntityEvent) event).getEntity()).getId()).isNotNull();
                eventsReceived.add(event);
            } else if (FlowableEngineEventType.PROCESS_CANCELLED == event.getType() || FlowableEngineEventType.ACTIVITY_CANCELLED == event.getType()) {
                eventsReceived.add(event);
            }
        }

        @Override
        public boolean isFailOnException() {
            return true;
        }

        public List<FlowableEvent> filterEvents(FlowableEngineEventType eventType) {// count
            // timer cancelled events
            List<FlowableEvent> filteredEvents = new ArrayList<>();
            List<FlowableEvent> eventsReceived = listener.getEventsReceived();
            for (FlowableEvent eventReceived : eventsReceived) {
                if (eventType == eventReceived.getType()) {
                    filteredEvents.add(eventReceived);
                }
            }
            return filteredEvents;
        }

    }

    public static class FilteredStaticTestFlowableEventListener extends StaticTestFlowableEventListener {

        @Override
        public void onEvent(FlowableEvent event) {
            if (event instanceof FlowableEntityEvent && ProcessInstance.class.isAssignableFrom(((FlowableEntityEvent) event).getEntity().getClass())) {
                // check whether entity in the event is initialized before
                // adding to the list.
                assertThat(((ExecutionEntity) ((FlowableEntityEvent) event).getEntity()).getId()).isNotNull();
                super.onEvent(event);
            } else if (FlowableEngineEventType.PROCESS_CANCELLED == event.getType() || FlowableEngineEventType.ACTIVITY_CANCELLED == event.getType()) {
                super.onEvent(event);
            }
        }

        static List<FlowableEvent> filterEvents(FlowableEngineEventType eventType) {
            List<FlowableEvent> filteredEvents = new ArrayList<>();
            for (FlowableEvent eventReceived : FilteredStaticTestFlowableEventListener.getEventsReceived()) {
                if (eventType == eventReceived.getType()) {
                    filteredEvents.add(eventReceived);
                }
            }
            return filteredEvents;
        }

    }
}
