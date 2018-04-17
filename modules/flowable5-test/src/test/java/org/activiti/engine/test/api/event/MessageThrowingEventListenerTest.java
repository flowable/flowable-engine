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

import org.activiti.engine.impl.bpmn.helper.MessageThrowingEventListener;
import org.activiti.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;

/**
 * Test case for all {@link FlowableEventListener}s that throw a message BPMN event when an {@link FlowableEvent} has been dispatched.
 * 
 * @author Frederik Heremans
 */
public class MessageThrowingEventListenerTest extends PluggableFlowableTestCase {

    @Deployment
    public void testThrowMessage() throws Exception {
        MessageThrowingEventListener listener = null;

        try {
            listener = new MessageThrowingEventListener();
            listener.setMessageName("Message");

            processEngineConfiguration.getEventDispatcher().addEventListener(listener, FlowableEngineEventType.TASK_ASSIGNED);

            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testMessage");
            assertNotNull(processInstance);

            // Fetch the task and re-assign it to trigger the event-listener
            org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId())
                    .singleResult();
            assertNotNull(task);
            taskService.setAssignee(task.getId(), "kermit");

            // Boundary-event should have been messaged and a new task should be available, on top of the already
            // existing one, since the cancelActivity='false'
            task = taskService.createTaskQuery().processInstanceId(processInstance.getId())
                    .taskDefinitionKey("subTask")
                    .singleResult();
            assertNotNull(task);
            assertEquals("kermit", task.getAssignee());

            org.flowable.task.api.Task boundaryTask = taskService.createTaskQuery().processInstanceId(processInstance.getId())
                    .taskDefinitionKey("boundaryTask")
                    .singleResult();
            assertNotNull(boundaryTask);

        } finally {
            processEngineConfiguration.getEventDispatcher().removeEventListener(listener);
        }
    }

    @Deployment
    public void testThrowMessageDefinedInProcessDefinition() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testMessage");
        assertNotNull(processInstance);

        // Fetch the task and re-assign it to trigger the event-listener
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId())
                .singleResult();
        assertNotNull(task);
        taskService.setAssignee(task.getId(), "kermit");

        // Boundary-event should have been messaged and a new task should be available, on top of the already
        // existing one, since the cancelActivity='false'
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId())
                .taskDefinitionKey("subTask")
                .singleResult();
        assertNotNull(task);
        assertEquals("kermit", task.getAssignee());

        org.flowable.task.api.Task boundaryTask = taskService.createTaskQuery().processInstanceId(processInstance.getId())
                .taskDefinitionKey("boundaryTask")
                .singleResult();
        assertNotNull(boundaryTask);
    }

    @Deployment
    public void testThrowMessageInterrupting() throws Exception {
        MessageThrowingEventListener listener = null;

        try {
            listener = new MessageThrowingEventListener();
            listener.setMessageName("Message");

            processEngineConfiguration.getEventDispatcher().addEventListener(listener, FlowableEngineEventType.TASK_ASSIGNED);

            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testMessage");
            assertNotNull(processInstance);

            // Fetch the task and re-assign it to trigger the event-listener
            org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId())
                    .singleResult();
            assertNotNull(task);
            taskService.setAssignee(task.getId(), "kermit");

            // Boundary-event should have been messaged and a new task should be available, the already
            // existing one should be removed, since the cancelActivity='true'
            task = taskService.createTaskQuery().processInstanceId(processInstance.getId())
                    .taskDefinitionKey("subTask")
                    .singleResult();
            assertNull(task);

            org.flowable.task.api.Task boundaryTask = taskService.createTaskQuery().processInstanceId(processInstance.getId())
                    .taskDefinitionKey("boundaryTask")
                    .singleResult();
            assertNotNull(boundaryTask);
        } finally {
            processEngineConfiguration.getEventDispatcher().removeEventListener(listener);
        }
    }
}
