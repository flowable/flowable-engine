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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.variable.api.event.FlowableVariableEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test case for all {@link FlowableEvent}s related to variables.
 *
 * @author Frederik Heremans
 */
public class VariableEventsTest extends PluggableFlowableTestCase {

    private TestVariableEventListener listener;

    /**
     * Test create, update and delete variables on a process-instance, using the API.
     */
    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testProcessInstanceVariableEvents() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertThat(processInstance).isNotNull();

        // Check create event
        runtimeService.setVariable(processInstance.getId(), "testVariable", "The value");
        assertThat(listener.getEventsReceived()).hasSize(1);
        FlowableVariableEvent event = (FlowableVariableEvent) listener.getEventsReceived().get(0);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.VARIABLE_CREATED);
        assertThat(event.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertThat(event.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(event.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(event.getTaskId()).isNull();
        assertThat(event.getVariableName()).isEqualTo("testVariable");
        assertThat(event.getVariableValue()).isEqualTo("The value");
        listener.clearEventsReceived();

        // Update variable
        runtimeService.setVariable(processInstance.getId(), "testVariable", "Updated value");
        assertThat(listener.getEventsReceived()).hasSize(1);
        event = (FlowableVariableEvent) listener.getEventsReceived().get(0);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.VARIABLE_UPDATED);
        assertThat(event.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertThat(event.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(event.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(event.getTaskId()).isNull();
        assertThat(event.getVariableName()).isEqualTo("testVariable");
        assertThat(event.getVariableValue()).isEqualTo("Updated value");
        listener.clearEventsReceived();

        // Delete variable
        runtimeService.removeVariable(processInstance.getId(), "testVariable");
        assertThat(listener.getEventsReceived()).hasSize(1);
        event = (FlowableVariableEvent) listener.getEventsReceived().get(0);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.VARIABLE_DELETED);
        // process definition Id can't be recognized in DB flush
        assertThat(event.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertThat(event.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(event.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(event.getTaskId()).isNull();
        assertThat(event.getVariableName()).isEqualTo("testVariable");
        // deleted variable value is always null
        assertThat(event.getVariableValue()).isNull();
        listener.clearEventsReceived();

        // Create, update and delete multiple variables
        Map<String, Object> vars = new HashMap<>();
        vars.put("test", 123);
        vars.put("test2", 456);
        runtimeService.setVariables(processInstance.getId(), vars);
        runtimeService.setVariables(processInstance.getId(), vars);
        runtimeService.removeVariables(processInstance.getId(), vars.keySet());

        assertThat(listener.getEventsReceived())
                .extracting(FlowableEvent::getType)
                .containsExactly(
                        FlowableEngineEventType.VARIABLE_CREATED,
                        FlowableEngineEventType.VARIABLE_CREATED,
                        FlowableEngineEventType.VARIABLE_UPDATED,
                        FlowableEngineEventType.VARIABLE_UPDATED,
                        FlowableEngineEventType.VARIABLE_DELETED,
                        FlowableEngineEventType.VARIABLE_DELETED
                );
        listener.clearEventsReceived();

        // Delete nonexistent variable should not dispatch event
        runtimeService.removeVariable(processInstance.getId(), "unexistingVariable");
        assertThat(listener.getEventsReceived()).isEmpty();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testStartEndProcessInstanceVariableEvents() throws Exception {
        Map<String, Object> variables = new HashMap<>();
        variables.put("var1", "value1");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);

        assertThat(listener.getEventsReceived())
                .extracting(FlowableEvent::getType)
                .containsExactly(FlowableEngineEventType.VARIABLE_CREATED);

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        assertThat(listener.getEventsReceived()).hasSize(2);
        assertThat(listener.getEventsReceived().get(1).getType()).isEqualTo(FlowableEngineEventType.VARIABLE_DELETED);
    }

    /**
     * Test create event of variables when process is started with variables passed in.
     */
    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testProcessInstanceVariableEventsOnStart() throws Exception {

        HashMap<String, Object> vars = new HashMap<>();
        vars.put("testVariable", "The value");

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
        assertThat(processInstance).isNotNull();

        // Check create event
        assertThat(listener.getEventsReceived()).hasSize(1);
        FlowableVariableEvent event = (FlowableVariableEvent) listener.getEventsReceived().get(0);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.VARIABLE_CREATED);
        assertThat(event.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertThat(event.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(event.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(event.getTaskId()).isNull();
        assertThat(event.getVariableName()).isEqualTo("testVariable");
        assertThat(event.getVariableValue()).isEqualTo("The value");
        listener.clearEventsReceived();
    }

    /**
     * Test create, update and delete variables locally on a child-execution of the process instance.
     */
    @Test
    @Deployment
    public void testProcessInstanceVariableEventsOnChildExecution() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("variableProcess");
        assertThat(processInstance).isNotNull();

        Execution child = runtimeService.createExecutionQuery().parentId(processInstance.getId()).singleResult();
        assertThat(child).isNotNull();

        runtimeService.setVariableLocal(child.getId(), "test", 1234567);

        assertThat(listener.getEventsReceived())
                .extracting(FlowableEvent::getType)
                .containsExactly(FlowableEngineEventType.VARIABLE_CREATED);

        FlowableVariableEvent event = (FlowableVariableEvent) listener.getEventsReceived().get(0);
        // Execution and process-id should differ
        assertThat(event.getExecutionId()).isEqualTo(child.getId());
        assertThat(event.getProcessInstanceId()).isEqualTo(processInstance.getId());
    }

    @Test
    @Deployment
    public void testProcessInstanceVariableEventsOnCallActivity() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("callVariableProcess",
                Collections.singletonMap("parentVar1", "parentVar1Value"));
        assertThat(processInstance).isNotNull();

        assertThat(listener.getEventsReceived()).hasSize(6);
        int nrOfCreated = 0;
        int nrOfDeleted = 0;
        for (int i = 0; i < listener.getEventsReceived().size(); i++) {
            FlowableVariableEvent event = (FlowableVariableEvent) listener.getEventsReceived().get(i);
            if (event.getType() == FlowableEngineEventType.VARIABLE_CREATED) {

                nrOfCreated++;

                if ("parentVar1".equals(event.getVariableName())) {
                    assertThat(event.getType()).isEqualTo(FlowableEngineEventType.VARIABLE_CREATED);
                    assertThat(event.getVariableName()).isEqualTo("parentVar1");

                } else if ("subVar1".equals(event.getVariableName())) {
                    assertThat(event.getType()).isEqualTo(FlowableEngineEventType.VARIABLE_CREATED);
                    assertThat(event.getVariableName()).isEqualTo("subVar1");

                } else if ("parentVar2".equals(event.getVariableName())) {
                    assertThat(event.getType()).isEqualTo(FlowableEngineEventType.VARIABLE_CREATED);
                    assertThat(event.getVariableName()).isEqualTo("parentVar2");

                } else {
                    fail("Unknown variable name " + event.getVariableName());
                }

            } else if (event.getType() == FlowableEngineEventType.VARIABLE_DELETED) {

                nrOfDeleted++;

                if ("parentVar1".equals(event.getVariableName())) {
                    assertThat(event.getType()).isEqualTo(FlowableEngineEventType.VARIABLE_DELETED);
                    assertThat(event.getVariableName()).isEqualTo("parentVar1");

                } else if ("subVar1".equals(event.getVariableName())) {
                    assertThat(event.getType()).isEqualTo(FlowableEngineEventType.VARIABLE_DELETED);
                    assertThat(event.getVariableName()).isEqualTo("subVar1");

                } else if ("parentVar2".equals(event.getVariableName())) {
                    assertThat(event.getType()).isEqualTo(FlowableEngineEventType.VARIABLE_DELETED);
                    assertThat(event.getVariableName()).isEqualTo("parentVar2");

                } else {
                    fail("Unknown variable name " + event.getVariableName());
                }

            } else {
                fail("Unknown event type " + event.getType());
            }

        }

        assertThat(nrOfCreated).isEqualTo(3);
        assertThat(nrOfDeleted).isEqualTo(3);
    }

    /**
     * Test variable events when done within a process (eg. execution-listener)
     */
    @Test
    @Deployment
    public void testProcessInstanceVariableEventsWithinProcess() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("variableProcess");
        assertThat(processInstance).isNotNull();

        assertThat(listener.getEventsReceived()).hasSize(3);

        // Check create event
        FlowableVariableEvent event = (FlowableVariableEvent) listener.getEventsReceived().get(0);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.VARIABLE_CREATED);
        assertThat(event.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertThat(event.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(event.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(event.getTaskId()).isNull();
        assertThat(event.getVariableName()).isEqualTo("variable");
        assertThat(event.getVariableValue()).isEqualTo(123);

        // Check update event
        event = (FlowableVariableEvent) listener.getEventsReceived().get(1);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.VARIABLE_UPDATED);
        assertThat(event.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertThat(event.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(event.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(event.getTaskId()).isNull();
        assertThat(event.getVariableName()).isEqualTo("variable");
        assertThat(event.getVariableValue()).isEqualTo(456);

        // Check delete event
        event = (FlowableVariableEvent) listener.getEventsReceived().get(2);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.VARIABLE_DELETED);
        assertThat(event.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertThat(event.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(event.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(event.getTaskId()).isNull();
        assertThat(event.getVariableName()).isEqualTo("variable");
        // deleted values are always null
        assertThat(event.getVariableValue()).isNull();
    }

    /**
     * Test create, update and delete of task-local variables.
     */
    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testTaskVariableEvents() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertThat(processInstance).isNotNull();

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();

        taskService.setVariableLocal(task.getId(), "testVariable", "The value");
        taskService.setVariableLocal(task.getId(), "testVariable", "Updated value");
        taskService.removeVariableLocal(task.getId(), "testVariable");

        // Check create event
        assertThat(listener.getEventsReceived()).hasSize(3);
        FlowableVariableEvent event = (FlowableVariableEvent) listener.getEventsReceived().get(0);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.VARIABLE_CREATED);
        assertThat(event.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertThat(event.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(event.getTaskId()).isEqualTo(task.getId());
        assertThat(event.getVariableName()).isEqualTo("testVariable");
        assertThat(event.getVariableValue()).isEqualTo("The value");

        event = (FlowableVariableEvent) listener.getEventsReceived().get(1);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.VARIABLE_UPDATED);
        assertThat(event.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertThat(event.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(event.getTaskId()).isEqualTo(task.getId());
        assertThat(event.getVariableName()).isEqualTo("testVariable");
        assertThat(event.getVariableValue()).isEqualTo("Updated value");

        event = (FlowableVariableEvent) listener.getEventsReceived().get(2);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.VARIABLE_DELETED);
        assertThat(event.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertThat(event.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(event.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertThat(event.getTaskId()).isEqualTo(task.getId());
        assertThat(event.getVariableName()).isEqualTo("testVariable");
        // deleted values are always null
        assertThat(event.getVariableValue()).isNull();
        listener.clearEventsReceived();
    }

    /**
     * Test variable events when done within a process (eg. execution-listener)
     */
    @Test
    @Deployment
    public void testTaskVariableEventsWithinProcess() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("variableProcess");
        assertThat(processInstance).isNotNull();

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();

        assertThat(listener.getEventsReceived()).hasSize(3);

        // Check create event
        FlowableVariableEvent event = (FlowableVariableEvent) listener.getEventsReceived().get(0);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.VARIABLE_CREATED);
        assertThat(event.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertThat(event.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(event.getTaskId()).isEqualTo(task.getId());
        assertThat(event.getVariableName()).isEqualTo("variable");
        assertThat(event.getVariableValue()).isEqualTo(123);

        // Check update event
        event = (FlowableVariableEvent) listener.getEventsReceived().get(1);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.VARIABLE_UPDATED);
        assertThat(event.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertThat(event.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(event.getTaskId()).isEqualTo(task.getId());
        assertThat(event.getVariableName()).isEqualTo("variable");
        assertThat(event.getVariableValue()).isEqualTo(456);

        // Check delete event
        event = (FlowableVariableEvent) listener.getEventsReceived().get(2);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.VARIABLE_DELETED);
        // process definition Id can't be recognized in DB flush
        assertThat(event.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertThat(event.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(event.getTaskId()).isEqualTo(task.getId());
        assertThat(event.getVariableName()).isEqualTo("variable");
        // deleted variable value is always null
        assertThat(event.getVariableValue()).isNull();
    }

    /**
     * Test to check create, update an delete behavior for variables on a task not related to a process.
     */
    @Test
    public void testTaskVariableStandalone() throws Exception {
        org.flowable.task.api.Task newTask = taskService.newTask();
        try {
            taskService.saveTask(newTask);

            assertThatThrownBy(() -> taskService.setVariable(null, null, null))
                    .isInstanceOf(FlowableIllegalArgumentException.class)
                    .hasMessage("variableName is null");

            taskService.setVariable(newTask.getId(), "testVariable", 123);
            taskService.setVariable(newTask.getId(), "testVariable", 456);

            waitForJobExecutorToProcessAllHistoryJobs(7000, 200);

            taskService.removeVariable(newTask.getId(), "testVariable");

            assertThat(listener.getEventsReceived()).hasSize(3);
            FlowableVariableEvent event = (FlowableVariableEvent) listener.getEventsReceived().get(0);
            assertThat(event.getType()).isEqualTo(FlowableEngineEventType.VARIABLE_CREATED);
            assertThat(event.getProcessDefinitionId()).isNull();
            assertThat(event.getExecutionId()).isNull();
            assertThat(event.getProcessInstanceId()).isNull();
            assertThat(event.getTaskId()).isEqualTo(newTask.getId());
            assertThat(event.getVariableName()).isEqualTo("testVariable");
            assertThat(event.getVariableValue()).isEqualTo(123);

            event = (FlowableVariableEvent) listener.getEventsReceived().get(1);
            assertThat(event.getType()).isEqualTo(FlowableEngineEventType.VARIABLE_UPDATED);
            assertThat(event.getProcessDefinitionId()).isNull();
            assertThat(event.getExecutionId()).isNull();
            assertThat(event.getProcessInstanceId()).isNull();
            assertThat(event.getTaskId()).isEqualTo(newTask.getId());
            assertThat(event.getVariableName()).isEqualTo("testVariable");
            assertThat(event.getVariableValue()).isEqualTo(456);

            event = (FlowableVariableEvent) listener.getEventsReceived().get(2);
            assertThat(event.getType()).isEqualTo(FlowableEngineEventType.VARIABLE_DELETED);
            assertThat(event.getProcessDefinitionId()).isNull();
            assertThat(event.getExecutionId()).isNull();
            assertThat(event.getProcessInstanceId()).isNull();
            assertThat(event.getTaskId()).isEqualTo(newTask.getId());
            assertThat(event.getVariableName()).isEqualTo("testVariable");
            // deleted variable value is always null
            assertThat(event.getVariableValue()).isNull();
        } finally {

            // Cleanup task and history to ensure a clean DB after test success/failure
            if (newTask.getId() != null) {
                taskService.deleteTask(newTask.getId(), true);
            }
        }

    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/processVariableEvent.bpmn20.xml" })
    public void testProcessInstanceVariableEventsForModeledDataObjectOnStart() throws Exception {

        HashMap<String, Object> vars = new HashMap<>();
        vars.put("var2", "The value");

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("processVariableEvent", vars);
        assertThat(processInstance).isNotNull();

        // Check create event
        assertThat(listener.getEventsReceived()).hasSize(2);
        FlowableVariableEvent event = (FlowableVariableEvent) listener.getEventsReceived().get(0);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.VARIABLE_CREATED);
        assertThat(event.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertThat(event.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(event.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(event.getTaskId()).isNull();
        assertThat(event.getVariableName()).isEqualTo("var2");
        assertThat(event.getVariableValue()).isEqualTo("var2 value");

        event = (FlowableVariableEvent) listener.getEventsReceived().get(1);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.VARIABLE_UPDATED);
        assertThat(event.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertThat(event.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(event.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(event.getTaskId()).isNull();
        assertThat(event.getVariableName()).isEqualTo("var2");
        assertThat(event.getVariableValue()).isEqualTo("The value");

        listener.clearEventsReceived();
    }

    /**
     * Test variables event for modeled data objects on callActivity.
     */
    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/callActivity.bpmn20.xml",
            "org/flowable/engine/test/api/runtime/calledActivity.bpmn20.xml" })
    public void testProcessInstanceVariableEventsForModeledDataObjectOnCallActivityStart() throws Exception {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("callActivity");
        assertThat(processInstance).isNotNull();

        // Check create event
        assertThat(listener.getEventsReceived()).hasSize(3);

        FlowableVariableEvent event = (FlowableVariableEvent) listener.getEventsReceived().get(0);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.VARIABLE_CREATED);
        assertThat(event.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertThat(event.getExecutionId()).isEqualTo(processInstance.getId());
        assertThat(event.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(event.getTaskId()).isNull();
        assertThat(event.getVariableName()).isEqualTo("var1");
        assertThat(event.getVariableValue()).isEqualTo("var1 value");

        ExecutionEntity subprocessInstance = (ExecutionEntity) runtimeService.createExecutionQuery()
                .rootProcessInstanceId(processInstance.getId())
                .onlySubProcessExecutions()
                .singleResult();
        assertThat(subprocessInstance).isNotNull();

        event = (FlowableVariableEvent) listener.getEventsReceived().get(1);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.VARIABLE_CREATED);
        assertThat(event.getProcessDefinitionId()).isEqualTo(subprocessInstance.getProcessDefinitionId());
        assertThat(event.getExecutionId()).isEqualTo(subprocessInstance.getId());
        assertThat(event.getProcessInstanceId()).isEqualTo(subprocessInstance.getId());
        assertThat(event.getTaskId()).isNull();
        assertThat(event.getVariableName()).isEqualTo("var3");
        assertThat(event.getVariableValue()).isEqualTo("var3 value");

        event = (FlowableVariableEvent) listener.getEventsReceived().get(2);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.VARIABLE_UPDATED);
        assertThat(event.getProcessDefinitionId()).isEqualTo(subprocessInstance.getProcessDefinitionId());
        assertThat(event.getExecutionId()).isEqualTo(subprocessInstance.getId());
        assertThat(event.getProcessInstanceId()).isEqualTo(subprocessInstance.getId());
        assertThat(event.getTaskId()).isNull();
        assertThat(event.getVariableName()).isEqualTo("var3");
        assertThat(event.getVariableValue()).isEqualTo("var1 value");
    }

    @BeforeEach
    public void setUp() {
        listener = new TestVariableEventListener();
        processEngineConfiguration.getEventDispatcher().addEventListener(listener);
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (listener != null) {
            listener.clearEventsReceived();
            processEngineConfiguration.getEventDispatcher().removeEventListener(listener);
        }
    }
}
