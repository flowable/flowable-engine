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

package org.flowable.engine.test.bpmn.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Date;

import org.flowable.engine.history.DeleteReason;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.eventsubscription.service.impl.EventSubscriptionQueryImpl;
import org.flowable.job.api.Job;
import org.junit.jupiter.api.Test;

/**
 * @author Daniel Meyer
 * @author Joram Barrez
 */
public class EventBasedGatewayTest extends PluggableFlowableTestCase {

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/gateway/EventBasedGatewayTest.testCatchAlertAndTimer.bpmn20.xml",
            "org/flowable/engine/test/bpmn/gateway/EventBasedGatewayTest.throwAlertSignal.bpmn20.xml" })
    public void testCatchSignalCancelsTimer() {

        ProcessInstance pi1 = runtimeService.startProcessInstanceByKey("catchSignal");

        assertThat(createEventSubscriptionQuery().count()).isEqualTo(1);
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);
        assertThat(managementService.createTimerJobQuery().count()).isEqualTo(1);

        runtimeService.startProcessInstanceByKey("throwSignal");

        assertThat(createEventSubscriptionQuery().count()).isZero();
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);
        assertThat(managementService.createJobQuery().count()).isZero();
        assertThat(managementService.createTimerJobQuery().count()).isZero();

        org.flowable.task.api.Task task = taskService.createTaskQuery().taskName("afterSignal").singleResult();
        assertThat(task).isNotNull();
        taskService.complete(task.getId());
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        assertHistoricActivitiesDeleteReason(pi1, DeleteReason.EVENT_BASED_GATEWAY_CANCEL, "timerEvent");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/gateway/EventBasedGatewayTest.testCatchAlertAndTimer.bpmn20.xml" })
    public void testCatchTimerCancelsSignal() {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("catchSignal");

        assertThat(createEventSubscriptionQuery().count()).isEqualTo(1);
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);
        assertThat(managementService.createTimerJobQuery().count()).isEqualTo(1);

        processEngineConfiguration.getClock().setCurrentTime(new Date(processEngineConfiguration.getClock().getCurrentTime().getTime() + 10000));

        // wait for timer to fire
        waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(10000, 100);

        assertThat(createEventSubscriptionQuery().count()).isZero();
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);
        assertThat(managementService.createJobQuery().count()).isZero();
        assertThat(managementService.createTimerJobQuery().count()).isZero();

        org.flowable.task.api.Task task = taskService.createTaskQuery().taskName("afterTimer").singleResult();

        assertThat(task).isNotNull();

        taskService.complete(task.getId());
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        assertHistoricActivitiesDeleteReason(processInstance, DeleteReason.EVENT_BASED_GATEWAY_CANCEL, "signalEvent");
    }

    @Test
    @Deployment
    public void testCatchSignalAndMessageAndTimer() {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("catchSignal");

        assertThat(createEventSubscriptionQuery().count()).isEqualTo(2);
        EventSubscriptionQueryImpl messageEventSubscriptionQuery = createEventSubscriptionQuery().eventType("message");
        assertThat(messageEventSubscriptionQuery.count()).isEqualTo(1);
        assertThat(createEventSubscriptionQuery().eventType("signal").count()).isEqualTo(1);
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);
        assertThat(managementService.createTimerJobQuery().count()).isEqualTo(1);

        Execution execution = runtimeService.createExecutionQuery().messageEventSubscriptionName("newInvoice").singleResult();
        assertThat(execution).isNotNull();

        execution = runtimeService.createExecutionQuery().signalEventSubscriptionName("alert").singleResult();
        assertThat(execution).isNotNull();

        processEngineConfiguration.getClock().setCurrentTime(new Date(processEngineConfiguration.getClock().getCurrentTime().getTime() + 10000));

        EventSubscription messageEventSubscription = messageEventSubscriptionQuery.singleResult();
        runtimeService.messageEventReceived(messageEventSubscription.getEventName(), messageEventSubscription.getExecutionId());

        assertThat(createEventSubscriptionQuery().count()).isZero();
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);
        assertThat(managementService.createTimerJobQuery().count()).isZero();
        assertThat(managementService.createJobQuery().count()).isZero();

        org.flowable.task.api.Task task = taskService.createTaskQuery().taskName("afterMessage").singleResult();
        assertThat(task).isNotNull();
        taskService.complete(task.getId());
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        assertHistoricActivitiesDeleteReason(processInstance, DeleteReason.EVENT_BASED_GATEWAY_CANCEL, "signalEvent");
        assertHistoricActivitiesDeleteReason(processInstance, DeleteReason.EVENT_BASED_GATEWAY_CANCEL, "timerEvent");
    }

    @Test
    public void testConnectedToActivity() {
        assertThatThrownBy(() -> repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/bpmn/gateway/EventBasedGatewayTest.testConnectedToActivity.bpmn20.xml").deploy())
                .isInstanceOf(Exception.class)
                .hasMessageContaining("Event based gateway can only be connected to elements of type intermediateCatchEvent");
    }

    @Test
    @Deployment
    public void testAsyncEventBasedGateway() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("asyncEventBasedGateway");

        // Trying to fire the signal should fail, job not yet created
        runtimeService.signalEventReceived("alert");
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNull();

        Job job = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).isNotNull();

        managementService.executeJob(job.getId());
        runtimeService.signalEventReceived("alert");
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("afterSignal");
    }

    private EventSubscriptionQueryImpl createEventSubscriptionQuery() {
        return new EventSubscriptionQueryImpl(processEngineConfiguration.getCommandExecutor(), processEngineConfiguration.getEventSubscriptionServiceConfiguration());
    }

}
