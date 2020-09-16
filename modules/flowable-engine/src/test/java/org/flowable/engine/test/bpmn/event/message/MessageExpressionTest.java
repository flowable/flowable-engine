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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.assertj.core.groups.Tuple;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.eventsubscription.service.impl.persistence.entity.MessageEventSubscriptionEntity;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 */
public class MessageExpressionTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testMessageEventsWithExpression() {
        assertMesageEventSubscriptions("startMessage2");

        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
            .messageName("startMessage2")
            .variable("catchMessage", "actualCatchMessageValue")
            .start();

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks).isEmpty();
        assertMesageEventSubscriptions("actualCatchMessageValue", "startMessage2");
        runtimeService.messageEventReceived(
            "actualCatchMessageValue",
            runtimeService.createEventSubscriptionQuery().eventName("actualCatchMessageValue").singleResult().getExecutionId(),
            CollectionUtil.singletonMap("boundaryMessage", "actualBoundaryMessage"));

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks).extracting(Task::getName).containsExactly("T1");

        runtimeService.messageEventReceived(
            "actualBoundaryMessage",
            runtimeService.createEventSubscriptionQuery().eventName("actualBoundaryMessage").singleResult().getExecutionId());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks).extracting(Task::getName).containsExactly("T2");
    }

    @Test
    @Deployment
    public void testMessageEventSubprocess() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
            .processDefinitionKey("testEventSubprocessWithMessageExpression")
            .variable("subprocessMessage", "actualMessageValue")
            .start();

        String executionId = runtimeService.createEventSubscriptionQuery()
            .eventName("actualMessageValue")
            .singleResult()
            .getExecutionId();
        assertThat(executionId).isNotNull();

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks).extracting(Task::getName).containsExactly("User task");

        runtimeService.messageEventReceived("actualMessageValue", executionId);

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks).extracting(Task::getName).containsExactly("T1");

    }

    protected void assertMesageEventSubscriptions(String ... names) {
        Tuple[] tuples = new Tuple[names.length];
        for (int i = 0; i < names.length; i++) {
            tuples[i] = Tuple.tuple(MessageEventSubscriptionEntity.EVENT_TYPE, names[i]);
        }

        assertThat(runtimeService.createEventSubscriptionQuery().orderByEventName().asc().list())
            .extracting(EventSubscription::getEventType, EventSubscription::getEventName)
            .containsOnly(tuples);
    }

}
