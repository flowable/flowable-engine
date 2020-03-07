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
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.engine.impl.bpmn.helper.MessageThrowingEventListener;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;

/**
 * Test case for all {@link FlowableEventListener}s that throw a message BPMN event when an {@link FlowableEvent} has been dispatched.
 *
 * @author Tijs Rademakers
 */
public class MessageThrowingEventListenerTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testThrowMessage() throws Exception {
        MessageThrowingEventListener listener = null;
        try {
            listener = new MessageThrowingEventListener();
            listener.setMessageName("Message");

            processEngineConfiguration.getEventDispatcher().addEventListener(listener, FlowableEngineEventType.TASK_ASSIGNED);

            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testMessage");
            assertThat(processInstance).isNotNull();

            // Fetch the task and reassign it to trigger the event-listener
            org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertThat(task).isNotNull();
            taskService.setAssignee(task.getId(), "kermit");

            // Boundary-event should have been messaged and a new task should be
            // available, on top of the already existing one, since the cancelActivity='false'
            task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("subTask").singleResult();
            assertThat(task).isNotNull();
            assertThat(task.getAssignee()).isEqualTo("kermit");

            org.flowable.task.api.Task boundaryTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("boundaryTask")
                    .singleResult();
            assertThat(boundaryTask).isNotNull();

        } finally {
            processEngineConfiguration.getEventDispatcher().removeEventListener(listener);
        }
    }

    @Test
    @Deployment
    public void testThrowMessageDefinedInProcessDefinition() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testMessage");
        assertThat(processInstance).isNotNull();

        // Fetch the task and re-assign it to trigger the event-listener
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        taskService.setAssignee(task.getId(), "kermit");

        // Boundary-event should have been messaged and a new task should be available, on top of the already
        // existing one, since the cancelActivity='false'
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("subTask").singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getAssignee()).isEqualTo("kermit");

        org.flowable.task.api.Task boundaryTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("boundaryTask")
                .singleResult();
        assertThat(boundaryTask).isNotNull();
    }

    @Test
    @Deployment
    public void testThrowMessageInterrupting() throws Exception {
        MessageThrowingEventListener listener = null;
        try {
            listener = new MessageThrowingEventListener();
            listener.setMessageName("Message");

            processEngineConfiguration.getEventDispatcher().addEventListener(listener, FlowableEngineEventType.TASK_ASSIGNED);

            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testMessage");
            assertThat(processInstance).isNotNull();

            // Fetch the task and reassign it to trigger the event-listener
            org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertThat(task).isNotNull();
            taskService.setAssignee(task.getId(), "kermit");

            // Boundary-event should have been messaged and a new task should be
            // available, the already existing one should be removed, since the cancelActivity='true'
            task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("subTask").singleResult();
            assertThat(task).isNull();

            org.flowable.task.api.Task boundaryTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("boundaryTask")
                    .singleResult();
            assertThat(boundaryTask).isNotNull();

            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        } finally {
            processEngineConfiguration.getEventDispatcher().removeEventListener(listener);
        }
    }
}
