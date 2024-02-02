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

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.eventsubscription.service.impl.EventSubscriptionQueryImpl;
import org.junit.jupiter.api.Test;

/**
 * @author Tijs Rademakers
 */
public class MessageIntermediateEventTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testSingleIntermediateMessageEvent() {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("process");

        List<String> activeActivityIds = runtimeService.getActiveActivityIds(pi.getId());
        assertThat(activeActivityIds).isNotNull();
        assertThat(activeActivityIds)
                .containsExactly("messageCatch");

        String messageName = "newInvoiceMessage";
        Execution execution = runtimeService.createExecutionQuery().messageEventSubscriptionName(messageName).singleResult();

        assertThat(execution).isNotNull();

        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().executionId(execution.getId()).singleResult();
        assertThat(eventSubscription).isNotNull();
        assertThat(eventSubscription.getEventName()).isEqualTo(messageName);

        eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(execution.getProcessInstanceId()).singleResult();
        assertThat(eventSubscription).isNotNull();
        assertThat(eventSubscription.getEventName()).isEqualTo(messageName);

        runtimeService.messageEventReceived(messageName, execution.getId());

        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();
        taskService.complete(task.getId());

    }

    @Test
    @Deployment
    public void testSingleIntermediateMessageExpressionEvent() {
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("myMessageName", "testMessage");
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("process", variableMap);

        List<String> activeActivityIds = runtimeService.getActiveActivityIds(pi.getId());
        assertThat(activeActivityIds).isNotNull();
        assertThat(activeActivityIds)
                .containsExactly("messageCatch");

        String messageName = "testMessage";
        Execution execution = runtimeService.createExecutionQuery().messageEventSubscriptionName(messageName).singleResult();
        assertThat(execution).isNotNull();

        runtimeService.messageEventReceived(messageName, execution.getId());

        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();
        taskService.complete(task.getId());
    }

    @Test
    @Deployment
    public void testConcurrentIntermediateMessageEvent() {

        ProcessInstance pi = runtimeService.startProcessInstanceByKey("process");

        List<String> activeActivityIds = runtimeService.getActiveActivityIds(pi.getId());
        assertThat(activeActivityIds).isNotNull();
        assertThat(activeActivityIds)
                .containsOnly("messageCatch1", "messageCatch2");

        String messageName = "newInvoiceMessage";
        List<Execution> executions = runtimeService.createExecutionQuery().messageEventSubscriptionName(messageName).list();

        assertThat(executions).isNotNull();
        assertThat(executions).hasSize(2);

        runtimeService.messageEventReceived(messageName, executions.get(0).getId());

        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task).isNull();

        runtimeService.messageEventReceived(messageName, executions.get(1).getId());

        task = taskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();

        taskService.complete(task.getId());
    }

    @Test
    @Deployment
    public void testAsyncTriggeredMessageEvent() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

        assertThat(processInstance).isNotNull();
        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).messageEventSubscriptionName("newMessage").singleResult();
        assertThat(execution).isNotNull();
        assertThat(createEventSubscriptionQuery().count()).isEqualTo(1);
        assertThat(runtimeService.createExecutionQuery().count()).isEqualTo(2);

        runtimeService.messageEventReceivedAsync("newMessage", execution.getId());

        assertThat(managementService.createJobQuery().messages().count()).isEqualTo(1);

        waitForJobExecutorToProcessAllJobs(8000L, 200L);
        assertThat(createEventSubscriptionQuery().count()).isZero();
        assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
        assertThat(managementService.createJobQuery().count()).isZero();
    }

    private EventSubscriptionQueryImpl createEventSubscriptionQuery() {
        return new EventSubscriptionQueryImpl(processEngineConfiguration.getCommandExecutor(), processEngineConfiguration.getEventSubscriptionServiceConfiguration());
    }
}
