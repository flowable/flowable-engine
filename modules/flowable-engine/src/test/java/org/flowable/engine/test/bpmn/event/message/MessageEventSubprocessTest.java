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

package org.flowable.engine.test.bpmn.event.message;

import org.flowable.engine.impl.EventSubscriptionQueryImpl;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.task.Task;
import org.flowable.engine.test.Deployment;

/**
 * @author Tijs Rademakers
 */
public class MessageEventSubprocessTest extends PluggableFlowableTestCase {

    @Deployment
    public void testInterruptingUnderProcessDefinition() {
        testInterruptingUnderProcessDefinition(1, 3);
    }

    /**
     * Checks if unused event subscriptions are properly deleted.
     */
    @Deployment
    public void testTwoInterruptingUnderProcessDefinition() {
        testInterruptingUnderProcessDefinition(2, 4);
    }

    private void testInterruptingUnderProcessDefinition(int expectedNumberOfEventSubscriptions, int numberOfExecutions) {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

        // the process instance must have a message event subscription:
        Execution execution = runtimeService.createExecutionQuery().messageEventSubscriptionName("newMessage").singleResult();
        assertNotNull(execution);
        assertEquals(expectedNumberOfEventSubscriptions, createEventSubscriptionQuery().count());
        assertEquals(numberOfExecutions, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        // if we trigger the usertask, the process terminates and the event subscription is removed:
        Task task = taskService.createTaskQuery().singleResult();
        assertEquals("task", task.getTaskDefinitionKey());
        taskService.complete(task.getId());
        assertEquals(0, createEventSubscriptionQuery().count());
        assertEquals(0, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());
        assertProcessEnded(processInstance.getId());

        // now we start a new instance but this time we trigger the event subprocess:
        processInstance = runtimeService.startProcessInstanceByKey("process");
        execution = runtimeService.createExecutionQuery().messageEventSubscriptionName("newMessage").singleResult();
        assertNotNull(execution);
        runtimeService.messageEventReceived("newMessage", execution.getId());

        task = taskService.createTaskQuery().singleResult();
        assertEquals("eventSubProcessTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());
        assertProcessEnded(processInstance.getId());
        assertEquals(0, createEventSubscriptionQuery().count());
        assertEquals(0, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());
    }

    @Deployment
    public void testNonInterruptingUnderProcessDefinition() {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

        // the process instance must have a message event subscription:
        Execution execution = runtimeService.createExecutionQuery()
                .processInstanceId(processInstance.getId())
                .messageEventSubscriptionName("newMessage")
                .singleResult();

        assertNotNull(execution);
        assertEquals(1, createEventSubscriptionQuery().count());
        assertEquals(3, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        // if we trigger the usertask, the process terminates and the event subscription is removed:
        Task task = taskService.createTaskQuery().singleResult();
        assertEquals("task", task.getTaskDefinitionKey());
        taskService.complete(task.getId());
        assertEquals(0, createEventSubscriptionQuery().count());
        assertEquals(0, runtimeService.createExecutionQuery().count());

        // now we start a new instance but this time we trigger the event subprocess:
        processInstance = runtimeService.startProcessInstanceByKey("process");
        execution = runtimeService.createExecutionQuery()
                .processInstanceId(processInstance.getId())
                .messageEventSubscriptionName("newMessage")
                .singleResult();

        runtimeService.messageEventReceived("newMessage", execution.getId());

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
                .messageEventSubscriptionName("newMessage")
                .singleResult();

        runtimeService.messageEventReceived("newMessage", execution.getId());

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

    @Deployment
    public void testNonInterruptingMultipleInstances() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
        assertEquals(3, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        Execution execution = runtimeService.createExecutionQuery()
                .processInstanceId(processInstance.getId())
                .messageEventSubscriptionName("newMessage")
                .singleResult();

        runtimeService.messageEventReceived("newMessage", execution.getId());
        assertEquals(5, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        assertEquals(2, taskService.createTaskQuery().count());
        assertEquals(1, createEventSubscriptionQuery().count());

        runtimeService.messageEventReceived("newMessage", execution.getId());
        assertEquals(7, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        assertEquals(3, taskService.createTaskQuery().count());
        assertEquals(1, createEventSubscriptionQuery().count());

        // now let's first complete the task in the main flow:
        Task task = taskService.createTaskQuery().taskDefinitionKey("task").singleResult();
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

    @Deployment
    public void testNonInterruptingSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
        assertEquals(3, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        Execution execution = runtimeService.createExecutionQuery()
                .processInstanceId(processInstance.getId())
                .messageEventSubscriptionName("eventMessage")
                .singleResult();

        runtimeService.messageEventReceived("eventMessage", execution.getId());
        assertEquals(6, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        assertEquals(2, taskService.createTaskQuery().count());
        assertEquals(1, createEventSubscriptionQuery().count());

        runtimeService.messageEventReceived("eventMessage", execution.getId());
        assertEquals(9, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        assertEquals(3, taskService.createTaskQuery().count());
        assertEquals(1, createEventSubscriptionQuery().count());

        // now let's first complete the task in the main flow:
        Task task = taskService.createTaskQuery().taskDefinitionKey("task").singleResult();
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

    @Deployment
    public void testInterruptingSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
        assertEquals(3, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        Execution execution = runtimeService.createExecutionQuery()
                .processInstanceId(processInstance.getId())
                .messageEventSubscriptionName("eventMessage")
                .singleResult();

        runtimeService.messageEventReceived("eventMessage", execution.getId());
        assertEquals(5, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        assertEquals(1, taskService.createTaskQuery().count());
        assertEquals(0, createEventSubscriptionQuery().count());

        // now let's complete the task in the event subprocess
        Task task = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask").list().get(0);
        taskService.complete(task.getId());

        // done!
        assertEquals(0, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());
    }

    private EventSubscriptionQueryImpl createEventSubscriptionQuery() {
        return new EventSubscriptionQueryImpl(processEngineConfiguration.getCommandExecutor());
    }

}
