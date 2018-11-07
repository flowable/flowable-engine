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

import java.util.Date;

import org.flowable.engine.history.DeleteReason;
import org.flowable.engine.impl.EventSubscriptionQueryImpl;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.EventSubscription;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
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

        assertEquals(1, createEventSubscriptionQuery().count());
        assertEquals(1, runtimeService.createProcessInstanceQuery().count());
        assertEquals(1, managementService.createTimerJobQuery().count());

        runtimeService.startProcessInstanceByKey("throwSignal");

        assertEquals(0, createEventSubscriptionQuery().count());
        assertEquals(1, runtimeService.createProcessInstanceQuery().count());
        assertEquals(0, managementService.createJobQuery().count());
        assertEquals(0, managementService.createTimerJobQuery().count());

        org.flowable.task.api.Task task = taskService.createTaskQuery().taskName("afterSignal").singleResult();
        assertNotNull(task);
        taskService.complete(task.getId());
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        assertHistoricActivitiesDeleteReason(pi1, DeleteReason.EVENT_BASED_GATEWAY_CANCEL, "timerEvent");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/gateway/EventBasedGatewayTest.testCatchAlertAndTimer.bpmn20.xml" })
    public void testCatchTimerCancelsSignal() {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("catchSignal");

        assertEquals(1, createEventSubscriptionQuery().count());
        assertEquals(1, runtimeService.createProcessInstanceQuery().count());
        assertEquals(1, managementService.createTimerJobQuery().count());

        processEngineConfiguration.getClock().setCurrentTime(new Date(processEngineConfiguration.getClock().getCurrentTime().getTime() + 10000));

        // wait for timer to fire
        waitForJobExecutorToProcessAllJobs(10000, 100);

        assertEquals(0, createEventSubscriptionQuery().count());
        assertEquals(1, runtimeService.createProcessInstanceQuery().count());
        assertEquals(0, managementService.createJobQuery().count());
        assertEquals(0, managementService.createTimerJobQuery().count());

        org.flowable.task.api.Task task = taskService.createTaskQuery().taskName("afterTimer").singleResult();

        assertNotNull(task);

        taskService.complete(task.getId());
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        assertHistoricActivitiesDeleteReason(processInstance, DeleteReason.EVENT_BASED_GATEWAY_CANCEL, "signalEvent");
    }

    @Test
    @Deployment
    public void testCatchSignalAndMessageAndTimer() {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("catchSignal");

        assertEquals(2, createEventSubscriptionQuery().count());
        EventSubscriptionQueryImpl messageEventSubscriptionQuery = createEventSubscriptionQuery().eventType("message");
        assertEquals(1, messageEventSubscriptionQuery.count());
        assertEquals(1, createEventSubscriptionQuery().eventType("signal").count());
        assertEquals(1, runtimeService.createProcessInstanceQuery().count());
        assertEquals(1, managementService.createTimerJobQuery().count());

        Execution execution = runtimeService.createExecutionQuery().messageEventSubscriptionName("newInvoice").singleResult();
        assertNotNull(execution);

        execution = runtimeService.createExecutionQuery().signalEventSubscriptionName("alert").singleResult();
        assertNotNull(execution);

        processEngineConfiguration.getClock().setCurrentTime(new Date(processEngineConfiguration.getClock().getCurrentTime().getTime() + 10000));

        EventSubscription messageEventSubscription = messageEventSubscriptionQuery.singleResult();
        runtimeService.messageEventReceived(messageEventSubscription.getEventName(), messageEventSubscription.getExecutionId());

        assertEquals(0, createEventSubscriptionQuery().count());
        assertEquals(1, runtimeService.createProcessInstanceQuery().count());
        assertEquals(0, managementService.createTimerJobQuery().count());
        assertEquals(0, managementService.createJobQuery().count());

        org.flowable.task.api.Task task = taskService.createTaskQuery().taskName("afterMessage").singleResult();
        assertNotNull(task);
        taskService.complete(task.getId());
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        assertHistoricActivitiesDeleteReason(processInstance, DeleteReason.EVENT_BASED_GATEWAY_CANCEL, "signalEvent");
        assertHistoricActivitiesDeleteReason(processInstance, DeleteReason.EVENT_BASED_GATEWAY_CANCEL, "timerEvent");
    }

    @Test
    public void testConnectedToActivity() {

        try {
            repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/bpmn/gateway/EventBasedGatewayTest.testConnectedToActivity.bpmn20.xml").deploy();
            fail("exception expected");
        } catch (Exception e) {
            if (!e.getMessage().contains("Event based gateway can only be connected to elements of type intermediateCatchEvent")) {
                fail("different exception expected");
            }
        }

    }

    @Test
    @Deployment
    public void testAsyncEventBasedGateway() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("asyncEventBasedGateway");

        // Trying to fire the signal should fail, job not yet created
        runtimeService.signalEventReceived("alert");
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNull(task);

        Job job = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(job);

        managementService.executeJob(job.getId());
        runtimeService.signalEventReceived("alert");
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("afterSignal", task.getName());
    }

    private EventSubscriptionQueryImpl createEventSubscriptionQuery() {
        return new EventSubscriptionQueryImpl(processEngineConfiguration.getCommandExecutor());
    }

}
