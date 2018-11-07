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

package org.flowable.engine.test.bpmn.event.signal;

import java.util.List;

import org.flowable.engine.impl.EventSubscriptionQueryImpl;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;

/**
 * @author Tijs Rademakers
 */
public class SignalEventSubprocessTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testInterruptingUnderProcessDefinition() {
        testInterruptingUnderProcessDefinition(1, 3);
    }

    /**
     * Checks if unused event subscriptions are properly deleted.
     */
    @Test
    @Deployment
    public void testTwoInterruptingUnderProcessDefinition() {
        testInterruptingUnderProcessDefinition(2, 4);
    }

    private void testInterruptingUnderProcessDefinition(int expectedNumberOfEventSubscriptions, int numberOfExecutions) {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

        // the process instance must have a message event subscription:
        Execution execution = runtimeService.createExecutionQuery().signalEventSubscriptionName("newSignal").singleResult();
        assertNotNull(execution);
        assertEquals(expectedNumberOfEventSubscriptions, createEventSubscriptionQuery().count());
        assertEquals(numberOfExecutions, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        // if we trigger the usertask, the process terminates and the event subscription is removed:
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertEquals("task", task.getTaskDefinitionKey());
        taskService.complete(task.getId());
        assertEquals(0, createEventSubscriptionQuery().count());
        assertEquals(0, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());
        assertProcessEnded(processInstance.getId());

        // now we start a new instance but this time we trigger the event subprocess:
        processInstance = runtimeService.startProcessInstanceByKey("process");
        execution = runtimeService.createExecutionQuery().signalEventSubscriptionName("newSignal").singleResult();
        assertNotNull(execution);
        runtimeService.signalEventReceived("newSignal");

        task = taskService.createTaskQuery().singleResult();
        assertEquals("eventSubProcessTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());
        assertProcessEnded(processInstance.getId());
        assertEquals(0, createEventSubscriptionQuery().count());
        assertEquals(0, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());
    }

    @Test
    @Deployment
    public void testNonInterruptingUnderProcessDefinition() {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

        // the process instance must have a message event subscription:
        Execution execution = runtimeService.createExecutionQuery()
                .processInstanceId(processInstance.getId())
                .signalEventSubscriptionName("newSignal")
                .singleResult();

        assertNotNull(execution);
        assertEquals(1, createEventSubscriptionQuery().count());
        assertEquals(3, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        // if we trigger the usertask, the process terminates and the event subscription is removed:
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertEquals("task", task.getTaskDefinitionKey());
        taskService.complete(task.getId());
        assertEquals(0, createEventSubscriptionQuery().count());
        assertEquals(0, runtimeService.createExecutionQuery().count());

        // now we start a new instance but this time we trigger the event subprocess:
        processInstance = runtimeService.startProcessInstanceByKey("process");
        execution = runtimeService.createExecutionQuery()
                .processInstanceId(processInstance.getId())
                .signalEventSubscriptionName("newSignal")
                .singleResult();

        runtimeService.signalEventReceived("newSignal");

        assertEquals(2, taskService.createTaskQuery().count());

        // now let's first complete the task in the main flow:
        task = taskService.createTaskQuery().taskDefinitionKey("task").singleResult();
        taskService.complete(task.getId());

        // we still have 3 executions:
        assertEquals(3, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        // now let's complete the task in the event subprocess
        task = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask").singleResult();
        taskService.complete(task.getId());

        // done!
        assertEquals(0, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        // #################### again, the other way around:

        processInstance = runtimeService.startProcessInstanceByKey("process");
        execution = runtimeService.createExecutionQuery()
                .processInstanceId(processInstance.getId())
                .signalEventSubscriptionName("newSignal")
                .singleResult();
        assertNotNull(execution);

        runtimeService.signalEventReceived("newSignal");

        assertEquals(2, taskService.createTaskQuery().count());

        task = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask").singleResult();
        taskService.complete(task.getId());

        assertEquals(1, createEventSubscriptionQuery().count());

        // we still have 3 executions:
        assertEquals(3, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        task = taskService.createTaskQuery().taskDefinitionKey("task").singleResult();
        taskService.complete(task.getId());

        // done!
        assertEquals(0, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());
    }

    @Test
    @Deployment
    public void testNonInterruptingMultipleInstances() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
        assertEquals(3, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        Execution execution = runtimeService.createExecutionQuery()
                .processInstanceId(processInstance.getId())
                .signalEventSubscriptionName("newSignal")
                .singleResult();

        runtimeService.signalEventReceived("newSignal", execution.getId());
        assertEquals(5, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        assertEquals(2, taskService.createTaskQuery().count());
        assertEquals(1, createEventSubscriptionQuery().count());

        runtimeService.signalEventReceived("newSignal", execution.getId());
        assertEquals(7, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        assertEquals(3, taskService.createTaskQuery().count());
        assertEquals(1, createEventSubscriptionQuery().count());

        // now let's first complete the task in the main flow:
        org.flowable.task.api.Task task = taskService.createTaskQuery().taskDefinitionKey("task").singleResult();
        taskService.complete(task.getId());

        assertEquals(0, createEventSubscriptionQuery().count());

        // we still have 6 executions:
        assertEquals(5, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        // now let's complete the first task in the event subprocess
        task = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask").list().get(0);
        taskService.complete(task.getId());

        assertEquals(3, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        // complete the second task in the event subprocess
        task = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask").singleResult();
        taskService.complete(task.getId());

        // done!
        assertEquals(0, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());
    }

    @Test
    @Deployment
    public void testNonInterruptingSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
        assertEquals(3, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        Execution execution = runtimeService.createExecutionQuery()
                .processInstanceId(processInstance.getId())
                .signalEventSubscriptionName("newSignal")
                .singleResult();

        runtimeService.signalEventReceived("newSignal", execution.getId());
        assertEquals(6, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        assertEquals(2, taskService.createTaskQuery().count());
        assertEquals(1, createEventSubscriptionQuery().count());

        runtimeService.signalEventReceived("newSignal", execution.getId());
        assertEquals(9, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        assertEquals(3, taskService.createTaskQuery().count());
        assertEquals(1, createEventSubscriptionQuery().count());

        // now let's first complete the task in the main flow:
        org.flowable.task.api.Task task = taskService.createTaskQuery().taskDefinitionKey("task").singleResult();
        taskService.complete(task.getId());

        assertEquals(0, createEventSubscriptionQuery().count());

        // we still have 7 executions:
        assertEquals(7, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        // now let's complete the first task in the event subprocess
        task = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask").list().get(0);
        taskService.complete(task.getId());

        assertEquals(4, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        // complete the second task in the event subprocess
        task = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask").singleResult();
        taskService.complete(task.getId());

        // done!
        assertEquals(0, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());
    }

    @Test
    @Deployment
    public void testInterruptingSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
        assertEquals(3, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        Execution execution = runtimeService.createExecutionQuery()
                .processInstanceId(processInstance.getId())
                .signalEventSubscriptionName("eventSignal")
                .singleResult();

        runtimeService.signalEventReceived("eventSignal", execution.getId());
        assertEquals(5, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        assertEquals(1, taskService.createTaskQuery().count());
        assertEquals(0, createEventSubscriptionQuery().count());

        // now let's complete the task in the event subprocess
        org.flowable.task.api.Task task = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask").list().get(0);
        taskService.complete(task.getId());

        // done!
        assertEquals(0, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());
    }
    
    @Test
    @Deployment
    public void testStartingAdditionalTasks() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startingAdditionalTasks");
        assertEquals(3, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        Execution execution = runtimeService.createExecutionQuery()
                .processInstanceId(processInstance.getId())
                .signalEventSubscriptionName("Start another task")
                .singleResult();
        
        assertNotNull(execution);

        assertEquals(1, taskService.createTaskQuery().count());
        assertEquals(1, createEventSubscriptionQuery().count());

        // now let's first complete the task in the main flow:
        org.flowable.task.api.Task task = taskService.createTaskQuery().taskDefinitionKey("task1").singleResult();
        taskService.complete(task.getId());

        assertEquals(2, createEventSubscriptionQuery().count());

        execution = runtimeService.createExecutionQuery()
                .processInstanceId(processInstance.getId())
                .signalEventSubscriptionName("Start another sub task")
                .singleResult();

        runtimeService.signalEventReceived("Start another sub task", execution.getId());
        
        assertEquals(2, taskService.createTaskQuery().count());
        
        execution = runtimeService.createExecutionQuery()
                .processInstanceId(processInstance.getId())
                .signalEventSubscriptionName("Start another task")
                .singleResult();
        
        runtimeService.signalEventReceived("Start another task", execution.getId());
        
        assertEquals(3, taskService.createTaskQuery().count());
        
        task = taskService.createTaskQuery().taskDefinitionKey("additionalTask").singleResult();
        taskService.complete(task.getId());

        assertEquals(2, taskService.createTaskQuery().count());
        
        task = taskService.createTaskQuery().taskDefinitionKey("subTask1").singleResult();
        taskService.complete(task.getId());
        
        assertEquals(1, taskService.createTaskQuery().count());
        
        task = taskService.createTaskQuery().taskDefinitionKey("additionalSubTask").singleResult();
        taskService.complete(task.getId());
        
        assertEquals(1, taskService.createTaskQuery().count());
        
        task = taskService.createTaskQuery().taskDefinitionKey("task2").singleResult();
        taskService.complete(task.getId());

        // done!
        assertEquals(0, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());
    }
    
    @Test
    @Deployment(resources="org/flowable/engine/test/bpmn/event/signal/SignalEventSubprocessTest.testStartingAdditionalTasks.bpmn20.xml")
    public void testStartingAdditionalTasksNoNestedEventSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startingAdditionalTasks");
        assertEquals(3, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        Execution execution = runtimeService.createExecutionQuery()
                .processInstanceId(processInstance.getId())
                .signalEventSubscriptionName("Start another task")
                .singleResult();
        
        assertNotNull(execution);

        assertEquals(1, taskService.createTaskQuery().count());
        assertEquals(1, createEventSubscriptionQuery().count());

        // now let's first complete the task in the main flow:
        org.flowable.task.api.Task task = taskService.createTaskQuery().taskDefinitionKey("task1").singleResult();
        taskService.complete(task.getId());

        assertEquals(2, createEventSubscriptionQuery().count());
        assertEquals(1, taskService.createTaskQuery().count());
        
        task = taskService.createTaskQuery().taskDefinitionKey("subTask1").singleResult();
        taskService.complete(task.getId());
        
        assertEquals(1, createEventSubscriptionQuery().count());
        assertEquals(1, taskService.createTaskQuery().count());
        
        task = taskService.createTaskQuery().taskDefinitionKey("task2").singleResult();
        taskService.complete(task.getId());

        // done!
        assertEquals(0, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());
    }
    
    @Test
    @Deployment(resources="org/flowable/engine/test/bpmn/event/signal/SignalEventSubprocessTest.testStartingAdditionalTasks.bpmn20.xml")
    public void testStartingAdditionalTasksWithNestedEventSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startingAdditionalTasks");
        
        assertEquals(1, taskService.createTaskQuery().count());
        assertEquals(1, createEventSubscriptionQuery().count());

        // now let's first complete the task in the main flow:
        org.flowable.task.api.Task task = taskService.createTaskQuery().taskDefinitionKey("task1").singleResult();
        taskService.complete(task.getId());

        assertEquals(2, createEventSubscriptionQuery().count());
        assertEquals(1, taskService.createTaskQuery().count());
        
        Execution execution = runtimeService.createExecutionQuery()
                .processInstanceId(processInstance.getId())
                .signalEventSubscriptionName("Start another sub task")
                .singleResult();

        runtimeService.signalEventReceived("Start another sub task", execution.getId());
        
        assertEquals(2, createEventSubscriptionQuery().count());
        assertEquals(2, taskService.createTaskQuery().count());
        
        task = taskService.createTaskQuery().taskDefinitionKey("subTask1").singleResult();
        taskService.complete(task.getId());
        
        assertEquals(1, createEventSubscriptionQuery().count());
        assertEquals(1, taskService.createTaskQuery().count());
        
        task = taskService.createTaskQuery().taskDefinitionKey("additionalSubTask").singleResult();
        taskService.complete(task.getId());
        
        assertEquals(1, createEventSubscriptionQuery().count());
        assertEquals(1, taskService.createTaskQuery().count());
        
        execution = runtimeService.createExecutionQuery()
                .processInstanceId(processInstance.getId())
                .signalEventSubscriptionName("Start another task")
                .singleResult();
        
        runtimeService.signalEventReceived("Start another task", execution.getId());
        
        assertEquals(1, createEventSubscriptionQuery().count());
        assertEquals(2, taskService.createTaskQuery().count());
        
        execution = runtimeService.createExecutionQuery()
                .processInstanceId(processInstance.getId())
                .signalEventSubscriptionName("Start another task")
                .singleResult();
        
        runtimeService.signalEventReceived("Start another task", execution.getId());
        
        assertEquals(1, createEventSubscriptionQuery().count());
        assertEquals(3, taskService.createTaskQuery().count());
        
        task = taskService.createTaskQuery().taskDefinitionKey("task2").singleResult();
        taskService.complete(task.getId());
        
        assertEquals(0, createEventSubscriptionQuery().count());
        
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().taskDefinitionKey("additionalTask").list();
        assertEquals(2, tasks.size());
        
        taskService.complete(tasks.get(0).getId());
        
        assertEquals(0, createEventSubscriptionQuery().count());
        
        task = taskService.createTaskQuery().taskDefinitionKey("additionalTask").singleResult();
        taskService.complete(task.getId());

        // done!
        assertEquals(0, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());
    }
    
    @Test
    @Deployment
    public void testStartingAdditionalTasksInterrupting() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startingAdditionalTasks");
        assertEquals(3, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        Execution execution = runtimeService.createExecutionQuery()
                .processInstanceId(processInstance.getId())
                .signalEventSubscriptionName("Start another task")
                .singleResult();
        
        assertNotNull(execution);

        assertEquals(1, taskService.createTaskQuery().count());
        assertEquals(1, createEventSubscriptionQuery().count());

        // now let's first complete the task in the main flow:
        org.flowable.task.api.Task task = taskService.createTaskQuery().taskDefinitionKey("task1").singleResult();
        taskService.complete(task.getId());

        assertEquals(2, createEventSubscriptionQuery().count());

        execution = runtimeService.createExecutionQuery()
                .processInstanceId(processInstance.getId())
                .signalEventSubscriptionName("Start another sub task")
                .singleResult();

        runtimeService.signalEventReceived("Start another sub task", execution.getId());
        
        assertEquals(1, taskService.createTaskQuery().count());
        
        task = taskService.createTaskQuery().taskDefinitionKey("additionalSubTask").singleResult();
        taskService.complete(task.getId());
        
        assertEquals(1, taskService.createTaskQuery().count());
        
        task = taskService.createTaskQuery().taskDefinitionKey("task2").singleResult();
        taskService.complete(task.getId());

        // done!
        assertEquals(0, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());
    }
    
    @Test
    @Deployment(resources="org/flowable/engine/test/bpmn/event/signal/SignalEventSubprocessTest.testStartingAdditionalTasksInterrupting.bpmn20.xml")
    public void testStartingAdditionalTasksInterruptingWithMainEventSubProcessInterrupt() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startingAdditionalTasks");
        assertEquals(3, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        Execution execution = runtimeService.createExecutionQuery()
                .processInstanceId(processInstance.getId())
                .signalEventSubscriptionName("Start another task")
                .singleResult();
        
        assertNotNull(execution);

        assertEquals(1, taskService.createTaskQuery().count());
        assertEquals(1, createEventSubscriptionQuery().count());

        // now let's first complete the task in the main flow:
        org.flowable.task.api.Task task = taskService.createTaskQuery().taskDefinitionKey("task1").singleResult();
        taskService.complete(task.getId());

        assertEquals(2, createEventSubscriptionQuery().count());

        execution = runtimeService.createExecutionQuery()
                .processInstanceId(processInstance.getId())
                .signalEventSubscriptionName("Start another sub task")
                .singleResult();

        runtimeService.signalEventReceived("Start another sub task", execution.getId());
        
        assertEquals(1, taskService.createTaskQuery().count());
        
        execution = runtimeService.createExecutionQuery()
                .processInstanceId(processInstance.getId())
                .signalEventSubscriptionName("Start another task")
                .singleResult();
        
        runtimeService.signalEventReceived("Start another task", execution.getId());
        
        assertEquals(1, taskService.createTaskQuery().count());
        
        task = taskService.createTaskQuery().taskDefinitionKey("additionalTask").singleResult();
        taskService.complete(task.getId());

        // done!
        assertEquals(0, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());
    }

    private EventSubscriptionQueryImpl createEventSubscriptionQuery() {
        return new EventSubscriptionQueryImpl(processEngineConfiguration.getCommandExecutor());
    }

}
