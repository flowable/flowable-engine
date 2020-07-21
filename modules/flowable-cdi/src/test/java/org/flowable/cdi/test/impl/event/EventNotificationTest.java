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
package org.flowable.cdi.test.impl.event;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.cdi.test.CdiFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.junit.Test;

public class EventNotificationTest extends CdiFlowableTestCase {

    @Test
    @Deployment(resources = { "org/flowable/cdi/test/impl/event/EventNotificationTest.process1.bpmn20.xml" })
    public void testReceiveAll() {
        TestEventListener listenerBean = getBeanInstance(TestEventListener.class);
        listenerBean.reset();

        // assert that the bean has received 0 events
        assertThat(listenerBean.getEventsReceived()).isEmpty();
        runtimeService.startProcessInstanceByKey("process1");

        // assert that now the bean has received 11 events
        assertThat(listenerBean.getEventsReceived()).hasSize(11);
    }

    @Test
    @Deployment(resources = { "org/flowable/cdi/test/impl/event/EventNotificationTest.process1.bpmn20.xml", "org/flowable/cdi/test/impl/event/EventNotificationTest.process2.bpmn20.xml" })
    public void testSelectEventsPerProcessDefinition() {
        TestEventListener listenerBean = getBeanInstance(TestEventListener.class);
        listenerBean.reset();

        assertThat(listenerBean.getEventsReceivedByKey()).isEmpty();
        // start the 2 processes
        runtimeService.startProcessInstanceByKey("process1");
        runtimeService.startProcessInstanceByKey("process2");

        // assert that now the bean has received 11 events
        assertThat(listenerBean.getEventsReceivedByKey()).hasSize(11);
    }

    @Test
    @Deployment(resources = { "org/flowable/cdi/test/impl/event/EventNotificationTest.process1.bpmn20.xml" })
    public void testSelectEventsPerActivity() {
        TestEventListener listenerBean = getBeanInstance(TestEventListener.class);
        listenerBean.reset();

        assertThat(listenerBean.getEndActivityService1WithLoopCounter()).isZero();
        assertThat(listenerBean.getEndActivityService1WithoutLoopCounter()).isZero();
        assertThat(listenerBean.getStartActivityService1WithoutLoopCounter()).isZero();
        assertThat(listenerBean.getTakeTransitiont1()).isZero();

        // start the process
        runtimeService.startProcessInstanceByKey("process1");

        // assert
        assertThat(listenerBean.getEndActivityService1WithoutLoopCounter()).isEqualTo(1);
        assertThat(listenerBean.getStartActivityService1WithoutLoopCounter()).isEqualTo(1);
        assertThat(listenerBean.getTakeTransitiont1()).isEqualTo(1);
    }

    @Test
    @Deployment(resources = { "org/flowable/cdi/test/impl/event/TaskEventNotificationTest.process3.bpmn20.xml" })
    public void testCreateEventsPerActivity() {
        TestEventListener listenerBean = getBeanInstance(TestEventListener.class);
        listenerBean.reset();

        assertThat(listenerBean.getCreateTask1()).isZero();
        assertThat(listenerBean.getAssignTask1()).isZero();
        assertThat(listenerBean.getCompleteTask1()).isZero();
        assertThat(listenerBean.getDeleteTask3()).isZero();

        // start the process
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("process3");

        Task task = taskService.createTaskQuery().singleResult();

        taskService.claim(task.getId(), "auser");
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().singleResult();
        taskService.complete(task.getId());

        do {
            task = taskService.createTaskQuery().singleResult();
        } while (task == null);

        runtimeService.deleteProcessInstance(pi.getId(), "DELETED");

        // assert
        assertThat(listenerBean.getCreateTask1()).isEqualTo(1);
        assertThat(listenerBean.getCreateTask2()).isEqualTo(1);
        assertThat(listenerBean.getAssignTask1()).isEqualTo(1);
        assertThat(listenerBean.getCompleteTask1()).isEqualTo(1);
        assertThat(listenerBean.getCompleteTask2()).isEqualTo(1);
        assertThat(listenerBean.getDeleteTask3()).isEqualTo(1);
    }

}
