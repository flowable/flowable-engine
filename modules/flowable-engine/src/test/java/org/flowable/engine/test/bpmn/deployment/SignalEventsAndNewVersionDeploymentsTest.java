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

package org.flowable.engine.test.bpmn.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.eventsubscription.api.EventSubscription;
import org.junit.jupiter.api.Test;

/**
 * A test specifically written to test how events (start/boundary) are handled when deploying a new version of a process definition.
 * 
 * @author Joram Barrez
 */
public class SignalEventsAndNewVersionDeploymentsTest extends PluggableFlowableTestCase {

    private static final String TEST_PROCESS_GLOBAL_BOUNDARY_SIGNAL = "org/flowable/engine/test/bpmn/deployment/SignalEventsAndNewVersionDeploymentsTest.testGlobalSignalBoundaryEvent.bpmn20.xml";

    private static final String TEST_PROCESS_START_SIGNAL = "org/flowable/engine/test/bpmn/deployment/SignalEventsAndNewVersionDeploymentsTest.testStartSignalEvent.bpmn20.xml";

    private static final String TEST_PROCESS_NO_EVENTS = "org/flowable/engine/test/bpmn/deployment/SignalEventsAndNewVersionDeploymentsTest.processWithoutEvents.bpmn20.xml";

    private static final String TEST_PROCESS_BOTH_START_AND_BOUNDARY_SIGNAL = "org/flowable/engine/test/bpmn/deployment/SignalEventsAndNewVersionDeploymentsTest.testBothBoundaryAndStartSignal.bpmn20.xml";

    private static final String TEST_PROCESS_BOTH_START_AND_BOUNDARY_SIGNAL_SAME_SIGNAL = "org/flowable/engine/test/bpmn/deployment/SignalEventsAndNewVersionDeploymentsTest.testBothBoundaryAndStartSignalSameSignal.bpmn20.xml";

    /*
     * BOUNDARY SIGNAL EVENT
     */

    @Test
    @Deployment
    public void testGlobalSignalBoundaryEvent() {
        runtimeService.startProcessInstanceByKey("signalTest");

        // Deploy new version of the same process. Original process should still be reachable via signal
        String deploymentId = deployBoundarySignalTestProcess();

        runtimeService.startProcessInstanceByKey("signalTest");
        assertThat(getAllEventSubscriptions()).hasSize(2);

        runtimeService.signalEventReceived("mySignal");
        assertThat(getAllEventSubscriptions()).isEmpty();

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks).hasSize(2);

        for (org.flowable.task.api.Task task : tasks) {
            assertThat(task.getName()).isEqualTo("Task after signal");
        }

        cleanup(deploymentId);
    }

    /**
     * Verifying that the event subscriptions do get removed when removing a deployment.
     */
    @Test
    public void testBoundaryEventSubscriptionDeletedOnDeploymentDelete() {
        String deploymentId = deployBoundarySignalTestProcess();
        runtimeService.startProcessInstanceByKey("signalTest");
        assertThat(taskService.createTaskQuery().singleResult().getName()).isEqualTo("My Task");

        String deploymentId2 = deployBoundarySignalTestProcess();
        runtimeService.startProcessInstanceByKey("signalTest");
        assertThat(taskService.createTaskQuery().count()).isEqualTo(2);
        assertThat(getAllEventSubscriptions()).hasSize(2);

        repositoryService.deleteDeployment(deploymentId, true);
        assertThat(taskService.createTaskQuery().singleResult().getName()).isEqualTo("My Task");
        assertThat(getAllEventSubscriptions()).hasSize(1);

        repositoryService.deleteDeployment(deploymentId2, true);
        assertThat(getAllEventSubscriptions()).isEmpty();
    }

    /**
     * Verifying that the event subscriptions do get removed when removing a process instance.
     */
    @Test
    public void testBoundaryEventSubscrptionsDeletedOnProcessInstanceDelete() {
        String deploymentId1 = deployBoundarySignalTestProcess();
        runtimeService.startProcessInstanceByKey("signalTest");
        assertThat(taskService.createTaskQuery().singleResult().getName()).isEqualTo("My Task");

        String deploymentId2 = deployBoundarySignalTestProcess();
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("signalTest");
        assertThat(taskService.createTaskQuery().count()).isEqualTo(2);
        assertThat(getAllEventSubscriptions()).hasSize(2);

        // Deleting PI of second deployment
        runtimeService.deleteProcessInstance(processInstance2.getId(), "testing");
        assertThat(taskService.createTaskQuery().singleResult().getName()).isEqualTo("My Task");
        assertThat(getAllEventSubscriptions()).hasSize(1);

        runtimeService.signalEventReceived("mySignal");
        assertThat(getAllEventSubscriptions()).isEmpty();
        assertThat(taskService.createTaskQuery().singleResult().getName()).isEqualTo("Task after signal");

        cleanup(deploymentId1, deploymentId2);
    }

    /*
     * START SIGNAL EVENT
     */

    @Test
    public void testStartSignalEvent() {
        String deploymentId1 = deployStartSignalTestProcess();
        assertThat(getAllEventSubscriptions()).hasSize(1);
        assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
        runtimeService.signalEventReceived("myStartSignal");
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);
        String deploymentId2 = deployStartSignalTestProcess();
        runtimeService.signalEventReceived("myStartSignal");
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(2);
        assertThat(getAllEventSubscriptions()).hasSize(1);

        cleanup(deploymentId1, deploymentId2);
    }

    @Test
    public void testSignalStartEventSubscriptionAfterDeploymentDelete() {

        // Deploy two version of process definition, delete latest and check if all is good

        String deploymentId1 = deployStartSignalTestProcess();
        List<EventSubscription> eventSubscriptions = getAllEventSubscriptions();
        assertThat(eventSubscriptions).hasSize(1);

        String deploymentId2 = deployStartSignalTestProcess();
        eventSubscriptions = getAllEventSubscriptions();
        assertThat(eventSubscriptions).hasSize(1);

        repositoryService.deleteDeployment(deploymentId2, true);
        eventSubscriptions = getAllEventSubscriptions();
        assertThat(eventSubscriptions).hasSize(1);

        cleanup(deploymentId1);
        assertThat(getAllEventSubscriptions()).isEmpty();

        // Deploy two versions of process definition, delete the first
        deploymentId1 = deployStartSignalTestProcess();
        deploymentId2 = deployStartSignalTestProcess();
        assertThat(getAllEventSubscriptions()).hasSize(1);
        repositoryService.deleteDeployment(deploymentId1, true);
        eventSubscriptions = getAllEventSubscriptions();
        assertThat(eventSubscriptions)
                .extracting(EventSubscription::getProcessDefinitionId)
                .containsExactly(repositoryService.createProcessDefinitionQuery().deploymentId(deploymentId2).singleResult().getId());

        cleanup(deploymentId2);
        assertThat(getAllEventSubscriptions()).isEmpty();
    }

    /**
     * v1 -> has start signal event v2 -> has no start signal event v3 -> has start signal event
     */
    @Test
    public void testDeployIntermediateVersionWithoutSignalStartEvent() {
        String deploymentId1 = deployStartSignalTestProcess();
        assertThat(getAllEventSubscriptions()).hasSize(1);
        assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
        runtimeService.signalEventReceived("myStartSignal");
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);
        assertEventSubscriptionsCount(1);

        String deploymentId2 = deployProcessWithoutEvents();
        assertThat(getAllEventSubscriptions()).isEmpty();
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);
        runtimeService.signalEventReceived("myStartSignal");
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);
        assertEventSubscriptionsCount(0);

        String deploymentId3 = deployStartSignalTestProcess();
        assertThat(getAllEventSubscriptions()).hasSize(1);
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);
        runtimeService.signalEventReceived("myStartSignal");
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(2);
        assertEventSubscriptionsCount(1);

        List<EventSubscription> eventSubscriptions = getAllEventSubscriptions();
        assertThat(eventSubscriptions)
                .extracting(EventSubscription::getProcessDefinitionId)
                .containsExactly(repositoryService.createProcessDefinitionQuery().deploymentId(deploymentId3).singleResult().getId());

        cleanup(deploymentId1, deploymentId2, deploymentId3);
    }

    @Test
    public void testDeleteDeploymentWithStartSignalEvents1() {
        String deploymentId1;
        String deploymentId2;
        String deploymentId3;
        deploymentId1 = deployStartSignalTestProcess();
        deploymentId2 = deployProcessWithoutEvents();
        deploymentId3 = deployStartSignalTestProcess();
        repositoryService.deleteDeployment(deploymentId3, true);
        assertEventSubscriptionsCount(0); // the latest is now the one without a signal start
        cleanup(deploymentId1, deploymentId2);
    }

    @Test
    public void testDeleteDeploymentWithStartSignalEvents2() {
        String deploymentId1 = deployStartSignalTestProcess();
        String deploymentId2 = deployProcessWithoutEvents();
        String deploymentId3 = deployStartSignalTestProcess();
        repositoryService.deleteDeployment(deploymentId2, true);
        assertEventSubscriptionsCount(1); // the latest is now the one with the signal
        runtimeService.signalEventReceived("myStartSignal");
        assertThat(runtimeService.createProcessInstanceQuery().singleResult().getProcessDefinitionId())
                .isEqualTo(repositoryService.createProcessDefinitionQuery().deploymentId(deploymentId3).singleResult().getId());
        cleanup(deploymentId1, deploymentId3);
    }

    @Test
    public void testDeleteDeploymentWithStartSignalEvents3() {
        String deploymentId1 = deployStartSignalTestProcess();
        String deploymentId2 = deployProcessWithoutEvents();
        String deploymentId3 = deployStartSignalTestProcess();
        repositoryService.deleteDeployment(deploymentId1, true);
        assertEventSubscriptionsCount(1); // the latest is now the one with the signal
        runtimeService.signalEventReceived("myStartSignal");
        assertThat(runtimeService.createProcessInstanceQuery().singleResult().getProcessDefinitionId())
                .isEqualTo(repositoryService.createProcessDefinitionQuery().deploymentId(deploymentId3).singleResult().getId());
        cleanup(deploymentId2, deploymentId3);
    }

    @Test
    public void testDeleteDeploymentWithStartSignalEvents4() {
        String deploymentId1 = deployStartSignalTestProcess();
        String deploymentId2 = deployProcessWithoutEvents();
        String deploymentId3 = deployStartSignalTestProcess();
        repositoryService.deleteDeployment(deploymentId2, true);
        repositoryService.deleteDeployment(deploymentId3, true);
        assertEventSubscriptionsCount(1); // the latest is now the one with the signal
        runtimeService.signalEventReceived("myStartSignal");
        assertThat(runtimeService.createProcessInstanceQuery().singleResult().getProcessDefinitionId())
                .isEqualTo(repositoryService.createProcessDefinitionQuery().deploymentId(deploymentId1).singleResult().getId());
        cleanup(deploymentId1);
    }

    @Test
    public void testDeleteDeploymentWithStartSignalEvents5() {
        String deploymentId1 = deployStartSignalTestProcess();
        String deploymentId2 = deployProcessWithoutEvents();
        assertEventSubscriptionsCount(0);
        runtimeService.signalEventReceived("myStartSignal");
        assertThat(runtimeService.createExecutionQuery().count()).isZero();
        repositoryService.deleteDeployment(deploymentId2, true);
        assertEventSubscriptionsCount(1); // the first is now the one with the signal
        runtimeService.signalEventReceived("myStartSignal");
        assertThat(runtimeService.createProcessInstanceQuery().singleResult().getProcessDefinitionId())
                .isEqualTo(repositoryService.createProcessDefinitionQuery().deploymentId(deploymentId1).singleResult().getId());
        cleanup(deploymentId1);
    }

    @Test
    public void testDeleteDeploymentWithStartSignalEvents6() {
        String deploymentId1 = deployStartSignalTestProcess();
        String deploymentId2 = deployProcessWithoutEvents();
        String deploymentId3 = deployStartSignalTestProcess();
        String deploymentId4 = deployProcessWithoutEvents();
        runtimeService.signalEventReceived("myStartSignal");
        assertThat(runtimeService.createExecutionQuery().count()).isZero();

        repositoryService.deleteDeployment(deploymentId2, true);
        repositoryService.deleteDeployment(deploymentId3, true);
        runtimeService.signalEventReceived("myStartSignal");
        assertThat(runtimeService.createExecutionQuery().count()).isZero();

        repositoryService.deleteDeployment(deploymentId1, true);
        runtimeService.signalEventReceived("myStartSignal");
        assertThat(runtimeService.createExecutionQuery().count()).isZero();
        cleanup(deploymentId4);
    }

    @Test
    public void testDeleteDeploymentWithStartSignalEvents7() {
        String deploymentId1 = deployStartSignalTestProcess();
        String deploymentId2 = deployProcessWithoutEvents();
        String deploymentId3 = deployStartSignalTestProcess();
        String deploymentId4 = deployProcessWithoutEvents();
        runtimeService.signalEventReceived("myStartSignal");
        assertThat(runtimeService.createExecutionQuery().count()).isZero();

        repositoryService.deleteDeployment(deploymentId2, true);
        repositoryService.deleteDeployment(deploymentId3, true);
        runtimeService.signalEventReceived("myStartSignal");
        assertThat(runtimeService.createExecutionQuery().count()).isZero();

        repositoryService.deleteDeployment(deploymentId4, true);
        runtimeService.signalEventReceived("myStartSignal");
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);
        cleanup(deploymentId1);
    }

    /*
     * BOTH BOUNDARY AND START SIGNAL
     */

    @Test
    public void testBothBoundaryAndStartEvent() {

        // Deploy process with both boundary and start event

        String deploymentId1 = deployProcessWithBothStartAndBoundarySignal();
        assertEventSubscriptionsCount(1);
        assertThat(runtimeService.createExecutionQuery().count()).isZero();

        runtimeService.signalEventReceived("myStartSignal");
        runtimeService.signalEventReceived("myStartSignal");
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(2);
        assertThat(getAllEventSubscriptions()).hasSize(3); // 1 for the start, 2 for the boundary

        // Deploy version with only a boundary signal
        String deploymentId2 = deployBoundarySignalTestProcess();
        runtimeService.signalEventReceived("myStartSignal");
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(2);
        assertEventSubscriptionsCount(2);

        // Deploy version with signal start
        String deploymentId3 = deployStartSignalTestProcess();
        runtimeService.signalEventReceived("myStartSignal");
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(3);
        assertEventSubscriptionsCount(3);

        // Delete last version again, making the one with the boundary the latest
        repositoryService.deleteDeployment(deploymentId3, true);
        runtimeService.signalEventReceived("myStartSignal");
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(2); // -1, cause process instance of deploymentId3 is gone too
        assertEventSubscriptionsCount(2);

        // Test the boundary signal
        runtimeService.signalEventReceived("myBoundarySignal");
        assertThat(taskService.createTaskQuery().taskName("Task after boundary signal").list()).hasSize(2);

        // Delete second version
        repositoryService.deleteDeployment(deploymentId2, true);
        runtimeService.signalEventReceived("myStartSignal");
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(3); // -1, cause process instance of deploymentId3 is gone too
        assertEventSubscriptionsCount(2);

        cleanup(deploymentId1);
    }

    @Test
    public void testBothBoundaryAndStartSameSignalId() {

        // Deploy process with both boundary and start event

        String deploymentId1 = deployProcessWithBothStartAndBoundarySignalSameSignal();
        assertEventSubscriptionsCount(1);
        assertThat(runtimeService.createExecutionQuery().count()).isZero();

        for (int i = 0; i < 9; i++) {
            // Every iteration will signal the boundary event of the previous iteration!
            runtimeService.signalEventReceived("mySignal");
            assertThat(getAllEventSubscriptions()).hasSize(2);
        }

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(9);
        }
        assertThat(getAllEventSubscriptions()).hasSize(2);

        runtimeService.signalEventReceived("myStartSignal");

        // Deploy version with only a start signal. The boundary events should still react though!
        String deploymentId2 = deployStartSignalTestProcess();
        runtimeService.signalEventReceived("myStartSignal");
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(2);
        assertEventSubscriptionsCount(2);

        cleanup(deploymentId1, deploymentId2);
    }

    /*
     * HELPERS
     */

    private String deployBoundarySignalTestProcess() {
        return deploy(TEST_PROCESS_GLOBAL_BOUNDARY_SIGNAL);
    }

    private String deployStartSignalTestProcess() {
        return deploy(TEST_PROCESS_START_SIGNAL);
    }

    private String deployProcessWithoutEvents() {
        return deploy(TEST_PROCESS_NO_EVENTS);
    }

    private String deployProcessWithBothStartAndBoundarySignal() {
        return deploy(TEST_PROCESS_BOTH_START_AND_BOUNDARY_SIGNAL);
    }

    private String deployProcessWithBothStartAndBoundarySignalSameSignal() {
        return deploy(TEST_PROCESS_BOTH_START_AND_BOUNDARY_SIGNAL_SAME_SIGNAL);
    }

    private String deploy(String path) {
        String deploymentId = repositoryService
                .createDeployment()
                .addClasspathResource(path)
                .deploy()
                .getId();
        return deploymentId;
    }

    private void cleanup(String... deploymentIds) {
        deleteDeployments();
    }

    private List<EventSubscription> getAllEventSubscriptions() {
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery()
                .orderByCreateDate()
                .desc()
                .list();

        for (EventSubscription eventSubscriptionEntity : eventSubscriptions) {
            assertThat(eventSubscriptionEntity.getEventType()).isEqualTo("signal");
            assertThat(eventSubscriptionEntity.getProcessDefinitionId()).isNotNull();
        }
        return eventSubscriptions;
    }

    private void assertEventSubscriptionsCount(int count) {
        assertThat(getAllEventSubscriptions()).hasSize(count);
    }

}
