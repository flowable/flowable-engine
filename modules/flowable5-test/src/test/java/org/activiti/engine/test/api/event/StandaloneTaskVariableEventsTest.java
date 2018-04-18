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
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.variable.api.event.FlowableVariableEvent;

/**
 * Test case for all {@link FlowableEvent}s related to variables.
 * 
 * @author Frederik Heremans
 */
public class StandaloneTaskVariableEventsTest extends PluggableFlowableTestCase {

    private TestFlowable6VariableEventListener listener;

    /**
     * Test to check create, update an delete behavior for variables on a task not related to a process.
     */
    public void testTaskVariableStandalone() throws Exception {
        org.flowable.task.api.Task newTask = taskService.newTask();
        try {
            taskService.saveTask(newTask);

            taskService.setVariable(newTask.getId(), "testVariable", 123);
            taskService.setVariable(newTask.getId(), "testVariable", 456);
            taskService.removeVariable(newTask.getId(), "testVariable");

            assertEquals(3, listener.getEventsReceived().size());
            FlowableVariableEvent event = (FlowableVariableEvent) listener.getEventsReceived().get(0);
            assertEquals(FlowableEngineEventType.VARIABLE_CREATED, event.getType());
            assertNull(event.getProcessDefinitionId());
            assertNull(event.getExecutionId());
            assertNull(event.getProcessInstanceId());
            assertEquals(newTask.getId(), event.getTaskId());
            assertEquals("testVariable", event.getVariableName());
            assertEquals(123, event.getVariableValue());

            event = (FlowableVariableEvent) listener.getEventsReceived().get(1);
            assertEquals(FlowableEngineEventType.VARIABLE_UPDATED, event.getType());
            assertNull(event.getProcessDefinitionId());
            assertNull(event.getExecutionId());
            assertNull(event.getProcessInstanceId());
            assertEquals(newTask.getId(), event.getTaskId());
            assertEquals("testVariable", event.getVariableName());
            assertEquals(456, event.getVariableValue());

            event = (FlowableVariableEvent) listener.getEventsReceived().get(2);
            assertEquals(FlowableEngineEventType.VARIABLE_DELETED, event.getType());
            assertNull(event.getProcessDefinitionId());
            assertNull(event.getExecutionId());
            assertNull(event.getProcessInstanceId());
            assertEquals(newTask.getId(), event.getTaskId());
            assertEquals("testVariable", event.getVariableName());
            // deleted variable value is always null
            assertNull(event.getVariableValue());
        } finally {

            // Cleanup task and history to ensure a clean DB after test success/failure
            if (newTask.getId() != null) {
                taskService.deleteTask(newTask.getId());
                if (processEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
                    historyService.deleteHistoricTaskInstance(newTask.getId());
                }
            }
        }

    }

    @Override
    protected void initializeServices() {
        super.initializeServices();

        listener = new TestFlowable6VariableEventListener();
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
