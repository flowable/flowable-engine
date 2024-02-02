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
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.assertj.core.groups.Tuple;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.eventsubscription.service.impl.persistence.entity.MessageEventSubscriptionEntity;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.IdentityLinkInfo;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.identitylink.api.history.HistoricIdentityLink;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 */
public class MessageExpressionTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testMessageEventsWithExpression() {
        assertMessageEventSubscriptions("startMessage2");

        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
            .messageName("startMessage2")
            .variable("catchMessage", "actualCatchMessageValue")
            .start();

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks).isEmpty();
        assertMessageEventSubscriptions("actualCatchMessageValue", "startMessage2");
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
    @Deployment(resources = "org/flowable/engine/test/bpmn/event/message/MessageExpressionTest.testMessageEventsWithExpression.bpmn20.xml")
    public void testMessageEventsWithOwnerAndAssignee() {
        assertMessageEventSubscriptions("startMessage2");

        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
            .messageName("startMessage2")
            .variable("catchMessage", "actualCatchMessageValue")
            .owner("kermit")
            .assignee("denise")
            .start();

        List<IdentityLink> identityLinks = runtimeService.getIdentityLinksForProcessInstance(processInstance.getId());
        assertThat(identityLinks)
            .extracting(IdentityLinkInfo::getType, IdentityLinkInfo::getUserId, IdentityLinkInfo::getGroupId, IdentityLinkInfo::getProcessInstanceId)
            .containsExactlyInAnyOrder(
                tuple(IdentityLinkType.OWNER, "kermit", null, processInstance.getId()),
                tuple(IdentityLinkType.ASSIGNEE, "denise", null, processInstance.getId())
            );

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            List<HistoricIdentityLink> historicIdentityLinks = historyService.getHistoricIdentityLinksForProcessInstance(processInstance.getId());
            assertThat(historicIdentityLinks)
                .extracting(IdentityLinkInfo::getType, IdentityLinkInfo::getUserId, IdentityLinkInfo::getGroupId, IdentityLinkInfo::getProcessInstanceId)
                .containsExactlyInAnyOrder(
                    tuple(IdentityLinkType.OWNER, "kermit", null, processInstance.getId()),
                    tuple(IdentityLinkType.ASSIGNEE, "denise", null, processInstance.getId())
                );
        }
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

    protected void assertMessageEventSubscriptions(String ... names) {
        Tuple[] tuples = new Tuple[names.length];
        for (int i = 0; i < names.length; i++) {
            tuples[i] = Tuple.tuple(MessageEventSubscriptionEntity.EVENT_TYPE, names[i]);
        }

        assertThat(runtimeService.createEventSubscriptionQuery().orderByEventName().asc().list())
            .extracting(EventSubscription::getEventType, EventSubscription::getEventName)
            .containsOnly(tuples);
    }

}
