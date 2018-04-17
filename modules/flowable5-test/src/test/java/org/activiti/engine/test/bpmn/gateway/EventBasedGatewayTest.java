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

package org.activiti.engine.test.bpmn.gateway;

import java.util.Date;

import org.activiti.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.common.engine.impl.runtime.Clock;
import org.flowable.engine.impl.EventSubscriptionQueryImpl;
import org.flowable.engine.repository.DeploymentProperties;
import org.flowable.engine.runtime.EventSubscription;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.test.Deployment;

/**
 * @author Daniel Meyer
 */
public class EventBasedGatewayTest extends PluggableFlowableTestCase {

    @Deployment(resources = {
            "org/activiti/engine/test/bpmn/gateway/EventBasedGatewayTest.testCatchAlertAndTimer.bpmn20.xml",
            "org/activiti/engine/test/bpmn/gateway/EventBasedGatewayTest.throwAlertSignal.bpmn20.xml" })
    public void testCatchSignalCancelsTimer() {

        runtimeService.startProcessInstanceByKey("catchSignal");

        assertEquals(1, createEventSubscriptionQuery().count());
        assertEquals(1, runtimeService.createProcessInstanceQuery().count());
        assertEquals(1, managementService.createTimerJobQuery().count());

        runtimeService.startProcessInstanceByKey("throwSignal");

        assertEquals(0, createEventSubscriptionQuery().count());
        assertEquals(1, runtimeService.createProcessInstanceQuery().count());
        assertEquals(0, managementService.createTimerJobQuery().count());

        org.flowable.task.api.Task task = taskService.createTaskQuery()
                .taskName("afterSignal")
                .singleResult();

        assertNotNull(task);

        taskService.complete(task.getId());

    }

    @Deployment(resources = {
            "org/activiti/engine/test/bpmn/gateway/EventBasedGatewayTest.testCatchAlertAndTimer.bpmn20.xml"
    })
    public void testCatchTimerCancelsSignal() {
        Clock clock = processEngineConfiguration.getClock();
        processEngineConfiguration.resetClock();

        runtimeService.startProcessInstanceByKey("catchSignal");

        assertEquals(1, createEventSubscriptionQuery().count());
        assertEquals(1, runtimeService.createProcessInstanceQuery().count());
        assertEquals(1, managementService.createTimerJobQuery().count());

        clock.setCurrentTime(new Date(processEngineConfiguration.getClock().getCurrentTime().getTime() + 10000));
        processEngineConfiguration.setClock(clock);
        try {
            // wait for timer to fire
            waitForJobExecutorToProcessAllJobs(10000, 200);

            assertEquals(0, createEventSubscriptionQuery().count());
            assertEquals(1, runtimeService.createProcessInstanceQuery().count());
            assertEquals(0, managementService.createTimerJobQuery().count());

            org.flowable.task.api.Task task = taskService.createTaskQuery()
                    .taskName("afterTimer")
                    .singleResult();

            assertNotNull(task);

            taskService.complete(task.getId());
        } finally {
            processEngineConfiguration.resetClock();
        }
    }

    @Deployment
    public void testCatchSignalAndMessageAndTimer() {
        Clock clock = processEngineConfiguration.getClock();
        processEngineConfiguration.resetClock();

        runtimeService.startProcessInstanceByKey("catchSignal");

        assertEquals(2, createEventSubscriptionQuery().count());
        EventSubscriptionQueryImpl messageEventSubscriptionQuery = createEventSubscriptionQuery().eventType("message");
        assertEquals(1, messageEventSubscriptionQuery.count());
        assertEquals(1, createEventSubscriptionQuery().eventType("signal").count());
        assertEquals(1, runtimeService.createProcessInstanceQuery().count());
        assertEquals(1, managementService.createTimerJobQuery().count());

        // we can query for an execution with has both a signal AND message subscription
        Execution execution = runtimeService.createExecutionQuery()
                .messageEventSubscriptionName("newInvoice")
                .signalEventSubscriptionName("alert")
                .singleResult();
        assertNotNull(execution);

        clock.setCurrentTime(new Date(processEngineConfiguration.getClock().getCurrentTime().getTime() + 10000));
        processEngineConfiguration.setClock(clock);
        try {

            EventSubscription messageEventSubscription = messageEventSubscriptionQuery.singleResult();
            runtimeService.messageEventReceived(messageEventSubscription.getEventName(), messageEventSubscription.getExecutionId());

            assertEquals(0, createEventSubscriptionQuery().count());
            assertEquals(1, runtimeService.createProcessInstanceQuery().count());
            assertEquals(0, managementService.createTimerJobQuery().count());

            org.flowable.task.api.Task task = taskService.createTaskQuery()
                    .taskName("afterMessage")
                    .singleResult();

            assertNotNull(task);

            taskService.complete(task.getId());
        } finally {
            processEngineConfiguration.resetClock();
        }
    }

    public void testConnectedToActitiy() {

        try {
            repositoryService.createDeployment()
                    .addClasspathResource("org/activiti/engine/test/bpmn/gateway/EventBasedGatewayTest.testConnectedToActivity.bpmn20.xml")
                    .deploymentProperty(DeploymentProperties.DEPLOY_AS_FLOWABLE5_PROCESS_DEFINITION, Boolean.TRUE)
                    .deploy();
            fail("exception expected");
        } catch (Exception e) {
            if (!e.getMessage().contains("Event based gateway can only be connected to elements of type intermediateCatchEvent")) {
                fail("different exception expected");
            }
        }

    }

    private EventSubscriptionQueryImpl createEventSubscriptionQuery() {
        return new EventSubscriptionQueryImpl(processEngineConfiguration.getCommandExecutor());
    }

}
