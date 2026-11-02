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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.job.api.Job;
import org.junit.jupiter.api.Test;

/**
 * Tests that signal, message, and timer start events with expressions are correctly evaluated
 * at deployment time using a {@link org.flowable.common.engine.impl.el.DefinitionVariableContainer}.
 *
 * @author Christopher Welsch
 */
public class StartEventExpressionWithDefinitionContainerTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testSignalStartEventExpressionResolvesAtDeployment() {
        // The signal expression ${variableContainer.definitionKey}Signal should be evaluated at deployment time
        // and create a signal event subscription with name "signalExpressionProcessSignal"
        List<EventSubscription> subscriptions = runtimeService.createEventSubscriptionQuery()
                .eventType("signal")
                .list();
        assertThat(subscriptions).hasSize(1);
        assertThat(subscriptions.get(0).getEventName()).isEqualTo("signalExpressionProcessSignal");

        // Verify that sending the signal creates a process instance
        runtimeService.signalEventReceived("signalExpressionProcessSignal");
        List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey("signalExpressionProcess")
                .list();
        assertThat(instances).hasSize(1);
    }

    @Test
    @Deployment
    public void testMessageStartEventExpressionResolvesAtDeployment() {
        // The message expression ${variableContainer.definitionKey}Message should be evaluated at deployment time
        // and create a message event subscription with name "messageExpressionProcessMessage"
        List<EventSubscription> subscriptions = runtimeService.createEventSubscriptionQuery()
                .eventType("message")
                .list();
        assertThat(subscriptions).hasSize(1);
        assertThat(subscriptions.get(0).getEventName()).isEqualTo("messageExpressionProcessMessage");

        // Verify that sending the message creates a process instance
        ProcessInstance instance = runtimeService.startProcessInstanceByMessage("messageExpressionProcessMessage");
        assertThat(instance).isNotNull();
        assertThat(instance.getProcessDefinitionKey()).isEqualTo("messageExpressionProcess");
    }

    @Test
    @Deployment
    public void testTimerStartEventExpressionResolvesAtDeployment() {
        // The timer expression ${variableContainer.definitionKey == 'timerExpressionProcess' ? 'PT1H' : 'PT2H'}
        // should be evaluated at deployment time and create a timer job with PT1H duration
        List<Job> timerJobs = managementService.createTimerJobQuery().list();
        assertThat(timerJobs).hasSize(1);

        Job timerJob = timerJobs.get(0);
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey("timerExpressionProcess")
                .singleResult();
        assertThat(timerJob.getProcessDefinitionId()).isEqualTo(processDefinition.getId());

        // Move clock past the timer and execute it
        Date futureDate = Date.from(Instant.now().plus(2, ChronoUnit.HOURS));
        processEngineConfiguration.getClock().setCurrentTime(futureDate);
        waitForJobExecutorToProcessAllJobs(5000L, 200L);

        // Verify a process instance was created
        List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey("timerExpressionProcess")
                .list();
        assertThat(instances).hasSize(1);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/event/StartEventExpressionWithDefinitionContainerTest.testSignalStartEventExpressionResolvesAtDeployment.bpmn20.xml",
            tenantId = "tenantA")
    public void testSignalStartEventExpressionWithTenantId() {
        List<EventSubscription> subscriptions = runtimeService.createEventSubscriptionQuery()
                .eventType("signal")
                .tenantId("tenantA")
                .list();
        assertThat(subscriptions).hasSize(1);
        assertThat(subscriptions.get(0).getEventName()).isEqualTo("signalExpressionProcessSignal");
        assertThat(subscriptions.get(0).getTenantId()).isEqualTo("tenantA");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/event/StartEventExpressionWithDefinitionContainerTest.testMessageStartEventExpressionResolvesAtDeployment.bpmn20.xml",
            tenantId = "tenantA")
    public void testMessageStartEventExpressionWithTenantId() {
        List<EventSubscription> subscriptions = runtimeService.createEventSubscriptionQuery()
                .eventType("message")
                .tenantId("tenantA")
                .list();
        assertThat(subscriptions).hasSize(1);
        assertThat(subscriptions.get(0).getEventName()).isEqualTo("messageExpressionProcessMessage");
        assertThat(subscriptions.get(0).getTenantId()).isEqualTo("tenantA");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/event/StartEventExpressionWithDefinitionContainerTest.testTimerStartEventExpressionResolvesAtDeployment.bpmn20.xml",
            tenantId = "tenantA")
    public void testTimerStartEventExpressionWithTenantId() {
        List<Job> timerJobs = managementService.createTimerJobQuery()
                .jobTenantId("tenantA")
                .list();
        assertThat(timerJobs).hasSize(1);
        assertThat(timerJobs.get(0).getTenantId()).isEqualTo("tenantA");

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey("timerExpressionProcess")
                .processDefinitionTenantId("tenantA")
                .singleResult();
        assertThat(timerJobs.get(0).getProcessDefinitionId()).isEqualTo(processDefinition.getId());
    }
}
