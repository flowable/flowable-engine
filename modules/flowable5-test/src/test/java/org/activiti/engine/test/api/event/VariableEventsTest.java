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

import java.util.HashMap;
import java.util.Map;

import org.activiti.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.variable.api.event.FlowableVariableEvent;

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
    @Deployment(resources = { "org/activiti/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testProcessInstanceVariableEvents() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertNotNull(processInstance);

        // Check create event
        runtimeService.setVariable(processInstance.getId(), "testVariable", "The value");
        assertEquals(1, listener.getEventsReceived().size());
        FlowableVariableEvent event = (FlowableVariableEvent) listener.getEventsReceived().get(0);
        assertEquals(FlowableEngineEventType.VARIABLE_CREATED, event.getType());
        assertEquals(processInstance.getProcessDefinitionId(), event.getProcessDefinitionId());
        assertEquals(processInstance.getId(), event.getExecutionId());
        assertEquals(processInstance.getId(), event.getProcessInstanceId());
        assertNull(event.getTaskId());
        assertEquals("testVariable", event.getVariableName());
        assertEquals("The value", event.getVariableValue());
        listener.clearEventsReceived();

        // Update variable
        runtimeService.setVariable(processInstance.getId(), "testVariable", "Updated value");
        assertEquals(1, listener.getEventsReceived().size());
        event = (FlowableVariableEvent) listener.getEventsReceived().get(0);
        assertEquals(FlowableEngineEventType.VARIABLE_UPDATED, event.getType());
        assertEquals(processInstance.getProcessDefinitionId(), event.getProcessDefinitionId());
        assertEquals(processInstance.getId(), event.getExecutionId());
        assertEquals(processInstance.getId(), event.getProcessInstanceId());
        assertNull(event.getTaskId());
        assertEquals("testVariable", event.getVariableName());
        assertEquals("Updated value", event.getVariableValue());
        listener.clearEventsReceived();

        // Delete variable
        runtimeService.removeVariable(processInstance.getId(), "testVariable");
        assertEquals(1, listener.getEventsReceived().size());
        event = (FlowableVariableEvent) listener.getEventsReceived().get(0);
        assertEquals(FlowableEngineEventType.VARIABLE_DELETED, event.getType());
        // process definition Id can't be recognized in DB flush
        assertNull(event.getProcessDefinitionId());
        assertEquals(processInstance.getId(), event.getExecutionId());
        assertEquals(processInstance.getId(), event.getProcessInstanceId());
        assertNull(event.getTaskId());
        assertEquals("testVariable", event.getVariableName());
        // deleted variable value is always null
        assertNull(event.getVariableValue());
        listener.clearEventsReceived();

        // Create, update and delete multiple variables
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("test", 123);
        vars.put("test2", 456);
        runtimeService.setVariables(processInstance.getId(), vars);
        runtimeService.setVariables(processInstance.getId(), vars);
        runtimeService.removeVariables(processInstance.getId(), vars.keySet());

        assertEquals(6, listener.getEventsReceived().size());
        assertEquals(FlowableEngineEventType.VARIABLE_CREATED, listener.getEventsReceived().get(0).getType());
        assertEquals(FlowableEngineEventType.VARIABLE_CREATED, listener.getEventsReceived().get(1).getType());
        assertEquals(FlowableEngineEventType.VARIABLE_UPDATED, listener.getEventsReceived().get(2).getType());
        assertEquals(FlowableEngineEventType.VARIABLE_UPDATED, listener.getEventsReceived().get(3).getType());
        assertEquals(FlowableEngineEventType.VARIABLE_DELETED, listener.getEventsReceived().get(4).getType());
        assertEquals(FlowableEngineEventType.VARIABLE_DELETED, listener.getEventsReceived().get(5).getType());
        listener.clearEventsReceived();

        // Delete nonexistent variable should not dispatch event
        runtimeService.removeVariable(processInstance.getId(), "unexistingVariable");
        assertTrue(listener.getEventsReceived().isEmpty());
    }

    @Deployment(resources = { "org/activiti/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testStartEndProcessInstanceVariableEvents() throws Exception {
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("var1", "value1");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);

        assertEquals(1, listener.getEventsReceived().size());
        assertEquals(FlowableEngineEventType.VARIABLE_CREATED, listener.getEventsReceived().get(0).getType());

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        assertEquals(2, listener.getEventsReceived().size());
        assertEquals(FlowableEngineEventType.VARIABLE_DELETED, listener.getEventsReceived().get(1).getType());
    }

    /**
     * Test create event of variables when process is started with variables passed in.
     */
    @Deployment(resources = { "org/activiti/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testProcessInstanceVariableEventsOnStart() throws Exception {

        HashMap<String, Object> vars = new HashMap<String, Object>();
        vars.put("testVariable", "The value");

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
        assertNotNull(processInstance);

        // Check create event
        assertEquals(1, listener.getEventsReceived().size());
        FlowableVariableEvent event = (FlowableVariableEvent) listener.getEventsReceived().get(0);
        assertEquals(FlowableEngineEventType.VARIABLE_CREATED, event.getType());
        assertEquals(processInstance.getProcessDefinitionId(), event.getProcessDefinitionId());
        assertEquals(processInstance.getId(), event.getExecutionId());
        assertEquals(processInstance.getId(), event.getProcessInstanceId());
        assertNull(event.getTaskId());
        assertEquals("testVariable", event.getVariableName());
        assertEquals("The value", event.getVariableValue());
        listener.clearEventsReceived();
    }

    /**
     * Test create, update and delete variables locally on a child-execution of the process instance.
     */
    @Deployment
    public void testProcessInstanceVariableEventsOnChildExecution() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("variableProcess");
        assertNotNull(processInstance);

        Execution child = runtimeService.createExecutionQuery().parentId(processInstance.getId()).singleResult();
        assertNotNull(child);

        runtimeService.setVariableLocal(child.getId(), "test", 1234567);

        assertEquals(1, listener.getEventsReceived().size());
        FlowableVariableEvent event = (FlowableVariableEvent) listener.getEventsReceived().get(0);
        assertEquals(FlowableEngineEventType.VARIABLE_CREATED, event.getType());

        // Execution and process-id should differ
        assertEquals(child.getId(), event.getExecutionId());
        assertEquals(processInstance.getId(), event.getProcessInstanceId());
    }

    /**
     * Test variable events when done within a process (eg. execution-listener)
     */
    @Deployment
    public void ActivitiEventType() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("variableProcess");
        assertNotNull(processInstance);

        assertEquals(3, listener.getEventsReceived().size());

        // Check create event
        FlowableVariableEvent event = (FlowableVariableEvent) listener.getEventsReceived().get(0);
        assertEquals(FlowableEngineEventType.VARIABLE_CREATED, event.getType());
        assertEquals(processInstance.getProcessDefinitionId(), event.getProcessDefinitionId());
        assertEquals(processInstance.getId(), event.getExecutionId());
        assertEquals(processInstance.getId(), event.getProcessInstanceId());
        assertNull(event.getTaskId());
        assertEquals("variable", event.getVariableName());
        assertEquals(123, event.getVariableValue());

        // Check update event
        event = (FlowableVariableEvent) listener.getEventsReceived().get(1);
        assertEquals(FlowableEngineEventType.VARIABLE_UPDATED, event.getType());
        assertEquals(processInstance.getProcessDefinitionId(), event.getProcessDefinitionId());
        assertEquals(processInstance.getId(), event.getExecutionId());
        assertEquals(processInstance.getId(), event.getProcessInstanceId());
        assertNull(event.getTaskId());
        assertEquals("variable", event.getVariableName());
        assertEquals(456, event.getVariableValue());

        // Check delete event
        event = (FlowableVariableEvent) listener.getEventsReceived().get(2);
        assertEquals(FlowableEngineEventType.VARIABLE_DELETED, event.getType());
        assertEquals(processInstance.getProcessDefinitionId(), event.getProcessDefinitionId());
        assertEquals(processInstance.getId(), event.getExecutionId());
        assertEquals(processInstance.getId(), event.getProcessInstanceId());
        assertNull(event.getTaskId());
        assertEquals("variable", event.getVariableName());
        assertEquals(456, event.getVariableValue());
    }

    /**
     * Test create, update and delete of task-local variables.
     */
    @Deployment(resources = { "org/activiti/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testTaskVariableEvents() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertNotNull(processInstance);

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(task);

        taskService.setVariableLocal(task.getId(), "testVariable", "The value");
        taskService.setVariableLocal(task.getId(), "testVariable", "Updated value");
        taskService.removeVariableLocal(task.getId(), "testVariable");

        // Check create event
        assertEquals(3, listener.getEventsReceived().size());
        FlowableVariableEvent event = (FlowableVariableEvent) listener.getEventsReceived().get(0);
        assertEquals(FlowableEngineEventType.VARIABLE_CREATED, event.getType());
        assertEquals(processInstance.getProcessDefinitionId(), event.getProcessDefinitionId());
        assertEquals(processInstance.getId(), event.getExecutionId());
        assertEquals(processInstance.getId(), event.getProcessInstanceId());
        assertEquals(task.getId(), event.getTaskId());
        assertEquals("testVariable", event.getVariableName());
        assertEquals("The value", event.getVariableValue());

        event = (FlowableVariableEvent) listener.getEventsReceived().get(1);
        assertEquals(FlowableEngineEventType.VARIABLE_UPDATED, event.getType());
        assertEquals(processInstance.getProcessDefinitionId(), event.getProcessDefinitionId());
        assertEquals(processInstance.getId(), event.getExecutionId());
        assertEquals(processInstance.getId(), event.getProcessInstanceId());
        assertEquals(task.getId(), event.getTaskId());
        assertEquals("testVariable", event.getVariableName());
        assertEquals("Updated value", event.getVariableValue());

        event = (FlowableVariableEvent) listener.getEventsReceived().get(2);
        assertEquals(FlowableEngineEventType.VARIABLE_DELETED, event.getType());
        assertNull(event.getProcessDefinitionId()); // process definition Id is set to null
        assertEquals(processInstance.getId(), event.getExecutionId());
        assertEquals(processInstance.getId(), event.getProcessInstanceId());
        assertEquals(task.getId(), event.getTaskId());
        assertEquals("testVariable", event.getVariableName());
        // deleted values are always null
        assertNull(event.getVariableValue());
        listener.clearEventsReceived();
    }

    /**
     * Test variable events when done within a process (eg. execution-listener)
     */
    @Deployment
    public void testTaskVariableEventsWithinProcess() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("variableProcess");
        assertNotNull(processInstance);

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId())
                .singleResult();
        assertNotNull(task);

        assertEquals(3, listener.getEventsReceived().size());

        // Check create event
        FlowableVariableEvent event = (FlowableVariableEvent) listener.getEventsReceived().get(0);
        assertEquals(FlowableEngineEventType.VARIABLE_CREATED, event.getType());
        assertEquals(processInstance.getProcessDefinitionId(), event.getProcessDefinitionId());
        assertEquals(processInstance.getId(), event.getExecutionId());
        assertEquals(processInstance.getId(), event.getProcessInstanceId());
        assertEquals(task.getId(), event.getTaskId());
        assertEquals("variable", event.getVariableName());
        assertEquals(123, event.getVariableValue());

        // Check update event
        event = (FlowableVariableEvent) listener.getEventsReceived().get(1);
        assertEquals(FlowableEngineEventType.VARIABLE_UPDATED, event.getType());
        assertEquals(processInstance.getProcessDefinitionId(), event.getProcessDefinitionId());
        assertEquals(processInstance.getId(), event.getExecutionId());
        assertEquals(processInstance.getId(), event.getProcessInstanceId());
        assertEquals(task.getId(), event.getTaskId());
        assertEquals("variable", event.getVariableName());
        assertEquals(456, event.getVariableValue());

        // Check delete event
        event = (FlowableVariableEvent) listener.getEventsReceived().get(2);
        assertEquals(FlowableEngineEventType.VARIABLE_DELETED, event.getType());
        // process definition Id can't be recognized in DB flush
        assertNull(event.getProcessDefinitionId());
        assertEquals(processInstance.getId(), event.getExecutionId());
        assertEquals(processInstance.getId(), event.getProcessInstanceId());
        assertEquals(task.getId(), event.getTaskId());
        assertEquals("variable", event.getVariableName());
        // deleted variable value is always null
        assertNull(event.getVariableValue());
    }

    @Override
    protected void initializeServices() {
        super.initializeServices();

        listener = new TestVariableEventListener();
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
