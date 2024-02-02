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
package org.flowable.engine.test.bpmn.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
public class EventSubProcessWithBoundaryEventsTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    void testSignalBoundaryEvent() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("eventSubProcessWithBoundary");

        Task task = taskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("taskInEventSubProcess");

        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().list();

        assertThat(eventSubscriptions)
                .extracting(EventSubscription::getEventType, EventSubscription::getEventName)
                .isEmpty();

        runtimeService.signalEventReceived("eventSignal");

        task = taskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("taskInEventSubProcess");

        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment
    void testMessageBoundaryEvent() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("eventSubProcessWithBoundary");

        Task task = taskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("taskInEventSubProcess");

        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().list();

        assertThat(eventSubscriptions)
                .extracting(EventSubscription::getEventType, EventSubscription::getEventName)
                .isEmpty();

        task = taskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("taskInEventSubProcess");

        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment
    void testTimerBoundaryEvent() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("eventSubProcessWithBoundary");

        Task task = taskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("taskInEventSubProcess");

        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().list();

        assertThat(eventSubscriptions)
                .extracting(EventSubscription::getEventType, EventSubscription::getEventName)
                .isEmpty();

        task = taskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("taskInEventSubProcess");

        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment
    void testErrorBoundaryEvent() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("eventSubProcessWithBoundary");

        Task task = taskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("taskAfterSubProcessBoundary");
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }
}
