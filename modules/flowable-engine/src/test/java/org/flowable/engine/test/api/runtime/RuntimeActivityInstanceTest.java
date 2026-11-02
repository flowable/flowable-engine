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

package org.flowable.engine.test.api.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.persistence.entity.ActivityInstanceEntity;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.runtime.ActivityInstance;
import org.flowable.engine.runtime.ActivityInstanceQuery;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

/**
 * @author martin.grofcik
 * @author Joram Barrez
 */
public class RuntimeActivityInstanceTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testActivityInstanceNoop() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("noopProcess");

        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        ActivityInstance activityInstance = runtimeService.createActivityInstanceQuery().activityId("noop").singleResult();

        assertThat(activityInstance.getActivityId()).isEqualTo("noop");
        assertThat(activityInstance.getActivityType()).isEqualTo("serviceTask");
        assertThat(activityInstance.getProcessDefinitionId()).isNotNull();
        assertThat(activityInstance.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(activityInstance.getStartTime()).isNotNull();
        assertThat(activityInstance.getEndTime()).isNotNull();
        assertThat(activityInstance.getDurationInMillis()).isGreaterThanOrEqualTo(0);
        assertThat(activityInstance.getTransactionOrder()).isEqualTo(3);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertActivityInstancesAreSame(historyService.createHistoricActivityInstanceQuery().activityId("noop").singleResult(), activityInstance);
        }

        String executionId = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().singleResult().getId();
        this.runtimeService.trigger(executionId);
        
        assertThat(runtimeService.createActivityInstanceQuery().activityId("noop").count()).isZero();
    }

    @Test
    @Deployment
    public void testActivityInstanceReceive() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("receiveProcess");

        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        ActivityInstance activityInstance = runtimeService.createActivityInstanceQuery().activityId("receive").singleResult();

        assertThat(activityInstance.getActivityId()).isEqualTo("receive");
        assertThat(activityInstance.getActivityType()).isEqualTo("receiveTask");
        assertThat(activityInstance.getEndTime()).isNull();
        assertThat(activityInstance.getDurationInMillis()).isNull();
        assertThat(activityInstance.getProcessDefinitionId()).isNotNull();
        assertThat(activityInstance.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(activityInstance.getStartTime()).isNotNull();
        assertThat(activityInstance.getTransactionOrder()).isEqualTo(3);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery().activityId("receive").singleResult();
            assertActivityInstancesAreSame(historicActivityInstance, activityInstance);
        }

        Execution execution = runtimeService.createExecutionQuery().onlyChildExecutions().processInstanceId(processInstance.getId()).singleResult();
        runtimeService.trigger(execution.getId());

        activityInstance = runtimeService.createActivityInstanceQuery().activityId("receive").singleResult();

        assertThat(activityInstance.getActivityId()).isEqualTo("receive");
        assertThat(activityInstance.getActivityType()).isEqualTo("receiveTask");
        assertThat(activityInstance.getEndTime()).isNotNull();
        assertThat(activityInstance.getDurationInMillis()).isGreaterThanOrEqualTo(0);
        assertThat(activityInstance.getProcessDefinitionId()).isNotNull();
        assertThat(activityInstance.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(activityInstance.getStartTime()).isNotNull();

        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertActivityInstancesAreSame(historyService.createHistoricActivityInstanceQuery().activityId("receive").singleResult(), activityInstance);
        }

        runtimeService.trigger(execution.getId());

        assertThat(runtimeService.createActivityInstanceQuery().count()).isZero();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/history/oneTaskProcess.bpmn20.xml")
    public void testActivityInstanceUnfinished() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertThat(processInstance).isNotNull();
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        ActivityInstanceQuery activityInstanceQuery = runtimeService.createActivityInstanceQuery();

        long finishedActivityInstanceCount = activityInstanceQuery.finished().count();
        assertThat(finishedActivityInstanceCount).as("The Start event and sequenceFlow are completed").isEqualTo(2);

        long unfinishedActivityInstanceCount = activityInstanceQuery.unfinished().count();
        assertThat(unfinishedActivityInstanceCount).as("One active (unfinished) User org.flowable.task.service.Task").isEqualTo(1);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/history/oneTaskProcess.bpmn20.xml")
    public void testSequenceFlowActivityInstance() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertThat(processInstance).isNotNull();

        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        ActivityInstance sequenceFlow = runtimeService.createActivityInstanceQuery().activityType("sequenceFlow").singleResult();
        assertThat(sequenceFlow.getActivityId()).isEqualTo("flow1");
        assertThat(sequenceFlow.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(sequenceFlow.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
        assertThat(sequenceFlow.getExecutionId()).isNotNull();
        assertThat(sequenceFlow.getStartTime()).isNotNull();
        assertThat(sequenceFlow.getEndTime()).isEqualTo(sequenceFlow.getStartTime());
        assertThat(sequenceFlow.getDurationInMillis()).isZero();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/history/oneTaskProcessWithoutSequenceFlowIds.bpmn20.xml")
    public void testSequenceFlowActivityInstanceWithoutSequenceFlowId() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcessWithoutSequenceFlowIds");
        assertThat(processInstance).isNotNull();

        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        ActivityInstance sequenceFlow = runtimeService.createActivityInstanceQuery().activityType("sequenceFlow").singleResult();
        assertThat(sequenceFlow.getActivityId()).isEqualTo("_flow_theStart__theTask");
    }

    @Test
    @Deployment(resources= "org/flowable/engine/test/api/runtime/RuntimeActivityInstanceTest.testActivityInstanceNoop.bpmn20.xml")
    public void testActivityInstanceQuery() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("noopProcess");

        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        assertThat(runtimeService.createActivityInstanceQuery().activityId("nonExistingActivityId").list()).isEmpty();
        assertThat(runtimeService.createActivityInstanceQuery().activityId("noop").list()).hasSize(1);

        assertThat(runtimeService.createActivityInstanceQuery().activityType("nonExistingActivityType").list()).isEmpty();
        assertThat(runtimeService.createActivityInstanceQuery().activityType("serviceTask").list()).hasSize(1);

        assertThat(runtimeService.createActivityInstanceQuery().activityName("nonExistingActivityName").list()).isEmpty();
        assertThat(runtimeService.createActivityInstanceQuery().activityName("No operation").list()).hasSize(1);

        assertThat(runtimeService.createActivityInstanceQuery().taskAssignee("nonExistingAssignee").list()).isEmpty();
        assertThat(runtimeService.createActivityInstanceQuery().taskCompletedBy("nonExistingUserId").list()).isEmpty();

        assertThat(runtimeService.createActivityInstanceQuery().executionId("nonExistingExecutionId").list()).isEmpty();

        assertThat(runtimeService.createActivityInstanceQuery().processInstanceId(processInstance.getId()).list()).hasSize(5);

        assertThat(runtimeService.createActivityInstanceQuery().processInstanceId("nonExistingProcessInstanceId").list()).isEmpty();

        assertThat(runtimeService.createActivityInstanceQuery().processInstanceId(processInstance.getId()).list()).hasSize(5);

        assertThat(runtimeService.createActivityInstanceQuery().processInstanceIds(Set.of("someId", processInstance.getId())).list()).hasSize(5);

        assertThat(runtimeService.createActivityInstanceQuery().processDefinitionId("nonExistingProcessDefinitionId").list()).isEmpty();

        assertThat(runtimeService.createActivityInstanceQuery().processDefinitionId(processInstance.getProcessDefinitionId()).list()).hasSize(5);

        assertThat(runtimeService.createActivityInstanceQuery().unfinished().list()).hasSize(1);

        assertThat(runtimeService.createActivityInstanceQuery().finished().list()).hasSize(4);

        ActivityInstance activityInstance = runtimeService.createActivityInstanceQuery().list().get(0);
        assertThat(runtimeService.createActivityInstanceQuery().activityInstanceId(activityInstance.getId()).list()).hasSize(1);
    }

    @Test
    @Deployment
    public void testActivityInstanceForEventsQuery() {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("eventProcess");
        assertThat(taskService.createTaskQuery().count()).isEqualTo(1);
        runtimeService.signalEventReceived("signal");

        assertThatActivityInstancesAreSame("noop");
        assertThatActivityInstancesAreSame("userTask");
        assertThatActivityInstancesAreSame("intermediate-event");
        assertThatActivityInstancesAreSame("start");

        assertThatActivityInstancesAreSame("intermediate-event");

        assertThatActivityInstancesAreSame("start");

        runtimeService.trigger(runtimeService.createExecutionQuery().processInstanceId(pi.getId()).onlyChildExecutions().singleResult().getId());

        assertThat(runtimeService.createActivityInstanceQuery().processInstanceId(pi.getId()).count()).isZero();
    }

    protected void assertThatActivityInstancesAreSame(String userTask) {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertActivityInstancesAreSame(historyService.createHistoricActivityInstanceQuery().activityId(userTask).singleResult(),
                runtimeService.createActivityInstanceQuery().activityId(userTask).singleResult());
        }
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/history/HistoricActivityInstanceTest.testHistoricActivityInstanceProperties.bpmn20.xml")
    public void testActivityInstanceAssignee() {
        // Start process instance
        runtimeService.startProcessInstanceByKey("taskAssigneeProcess");

        // Get task list
        ActivityInstance activityInstance = runtimeService.createActivityInstanceQuery().activityId("theTask").singleResult();

        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(activityInstance.getTaskId()).isEqualTo(task.getId());
        assertThat(activityInstance.getAssignee()).isEqualTo("kermit");
        assertThat(activityInstance.getCompletedBy()).isEqualTo(null);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery().activityId("theTask").singleResult();
            assertActivityInstancesAreSame(historicActivityInstance, activityInstance);
        }
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/twoTasksProcess.bpmn20.xml")
    public void testAssigneeChange() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksProcess");
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();

        taskService.claim(task.getId(), "kermit");
        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getAssignee()).isEqualTo("kermit");

        taskService.setAssignee(task.getId(), "gonzo");
        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getAssignee()).isEqualTo("gonzo");

        ActivityInstance activityInstance = runtimeService.createActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityId("firstTask")
                .singleResult();
        assertThat(activityInstance.getAssignee()).isEqualTo("gonzo");

        assertThat(runtimeService.createActivityInstanceQuery().activityId("firstTask").taskAssignee("gonzo").count()).isOne();


        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .activityId("firstTask")
                    .singleResult();
            assertActivityInstancesAreSame(historicActivityInstance, activityInstance);

            assertThat(historyService.createHistoricActivityInstanceQuery().activityId("firstTask").taskAssignee("gonzo").count()).isOne();
        }
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/twoTasksProcess.bpmn20.xml")
    public void testCompletedBy() {
        runtimeService.startProcessInstanceByKey("twoTasksProcess");
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        taskService.claim(task.getId(), "kermit");
        taskService.complete(task.getId());

        ActivityInstance activityInstance = runtimeService.createActivityInstanceQuery().activityId("firstTask").singleResult();

        assertThat(activityInstance.getTaskId()).isEqualTo(task.getId());
        assertThat(activityInstance.getAssignee()).isEqualTo("kermit");
        assertThat(activityInstance.getCompletedBy()).isEqualTo("kermit");

        assertThat(runtimeService.createActivityInstanceQuery().activityId("firstTask").taskAssignee("kermit").count()).isOne();
        assertThat(runtimeService.createActivityInstanceQuery().activityId("firstTask").taskCompletedBy("kermit").count()).isOne();
        assertThat(runtimeService.createActivityInstanceQuery().activityId("firstTask").taskAssignee("kermit").taskCompletedBy("kermit").count()).isOne();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery().activityId("firstTask").singleResult();
            assertActivityInstancesAreSame(historicActivityInstance, activityInstance);

            assertThat(historyService.createHistoricActivityInstanceQuery().activityId("firstTask").taskAssignee("kermit").count()).isOne();
            assertThat(historyService.createHistoricActivityInstanceQuery().activityId("firstTask").taskCompletedBy("kermit").count()).isOne();
        }

        // Option 2: completer is different from assignee
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksProcess");
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.claim(task.getId(), "kermit");

        Authentication.setAuthenticatedUserId("gonzo");
        taskService.complete(task.getId(), "gonzo");
        Authentication.setAuthenticatedUserId(null);

        activityInstance = runtimeService.createActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityId("firstTask").singleResult();

        assertThat(activityInstance.getTaskId()).isEqualTo(task.getId());
        assertThat(activityInstance.getAssignee()).isEqualTo("kermit");
        assertThat(activityInstance.getCompletedBy()).isEqualTo("gonzo");

        assertThat(runtimeService.createActivityInstanceQuery().processInstanceId(processInstance.getId()).activityId("firstTask").taskAssignee("kermit").count()).isOne();
        assertThat(runtimeService.createActivityInstanceQuery().processInstanceId(processInstance.getId()).activityId("firstTask").taskCompletedBy("gonzo").count()).isOne();
        assertThat(runtimeService.createActivityInstanceQuery().processInstanceId(processInstance.getId()).activityId("firstTask").taskAssignee("kermit").taskCompletedBy("gonzo").count()).isOne();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstance.getId()).activityId("firstTask").singleResult();
            assertActivityInstancesAreSame(historicActivityInstance, activityInstance);

            assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).activityId("firstTask").taskAssignee("kermit").count()).isOne();
            assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).activityId("firstTask").taskCompletedBy("gonzo").count()).isOne();
            assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).activityId("firstTask").taskAssignee("kermit").taskCompletedBy("gonzo").count()).isOne();

        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/history/calledProcess.bpmn20.xml", "org/flowable/engine/test/history/HistoricActivityInstanceTest.testCallSimpleSubProcess.bpmn20.xml" })
    public void processInstanceEndRemovesAllActivityInstances() {
        runtimeService.startProcessInstanceByKey("callSimpleSubProcess");

        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
        assertThat(runtimeService.createActivityInstanceQuery().count()).isZero();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/history/calledProcess.bpmn20.xml", "org/flowable/engine/test/api/runtime/RuntimeActivityInstanceTest.testCallSimpleSubProcess.bpmn20.xml" })
    public void testActivityInstanceCalledProcessId() {
        runtimeService.startProcessInstanceByKey("callSimpleSubProcess");

        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        ActivityInstance activityInstance = runtimeService.createActivityInstanceQuery().activityId("callSubProcess").singleResult();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery().activityId("callSubProcess").singleResult();
            assertActivityInstancesAreSame(historicActivityInstance, activityInstance);

            HistoricProcessInstance oldInstance = historyService.createHistoricProcessInstanceQuery().processDefinitionKey("calledProcess").singleResult();

            assertThat(activityInstance.getCalledProcessInstanceId()).isEqualTo(oldInstance.getId());
        }
    }

    @Test
    @Deployment
    public void testSorting() {
        runtimeService.startProcessInstanceByKey("process");

        int expectedActivityInstances = 3;

        assertThat(runtimeService.createActivityInstanceQuery().orderByActivityInstanceId().asc().list()).hasSize(expectedActivityInstances);
        assertThat(runtimeService.createActivityInstanceQuery().orderByActivityInstanceStartTime().asc().list()).hasSize(expectedActivityInstances);
        assertThat(runtimeService.createActivityInstanceQuery().orderByActivityInstanceEndTime().asc().list()).hasSize(expectedActivityInstances);
        assertThat(runtimeService.createActivityInstanceQuery().orderByActivityInstanceDuration().asc().list()).hasSize(expectedActivityInstances);
        assertThat(runtimeService.createActivityInstanceQuery().orderByExecutionId().asc().list()).hasSize(expectedActivityInstances);
        assertThat(runtimeService.createActivityInstanceQuery().orderByProcessDefinitionId().asc().list()).hasSize(expectedActivityInstances);
        assertThat(runtimeService.createActivityInstanceQuery().orderByProcessInstanceId().asc().list()).hasSize(expectedActivityInstances);

        assertThat(runtimeService.createActivityInstanceQuery().orderByActivityInstanceId().desc().list()).hasSize(expectedActivityInstances);
        assertThat(runtimeService.createActivityInstanceQuery().orderByActivityInstanceStartTime().desc().list()).hasSize(expectedActivityInstances);
        assertThat(runtimeService.createActivityInstanceQuery().orderByActivityInstanceEndTime().desc().list()).hasSize(expectedActivityInstances);
        assertThat(runtimeService.createActivityInstanceQuery().orderByActivityInstanceDuration().desc().list()).hasSize(expectedActivityInstances);
        assertThat(runtimeService.createActivityInstanceQuery().orderByExecutionId().desc().list()).hasSize(expectedActivityInstances);
        assertThat(runtimeService.createActivityInstanceQuery().orderByProcessDefinitionId().desc().list()).hasSize(expectedActivityInstances);
        assertThat(runtimeService.createActivityInstanceQuery().orderByProcessInstanceId().desc().list()).hasSize(expectedActivityInstances);

        assertThat(runtimeService.createActivityInstanceQuery().orderByActivityInstanceId().asc().count()).isEqualTo(expectedActivityInstances);
        assertThat(runtimeService.createActivityInstanceQuery().orderByActivityInstanceStartTime().asc().count()).isEqualTo(expectedActivityInstances);
        assertThat(runtimeService.createActivityInstanceQuery().orderByActivityInstanceEndTime().asc().count()).isEqualTo(expectedActivityInstances);
        assertThat(runtimeService.createActivityInstanceQuery().orderByActivityInstanceDuration().asc().count()).isEqualTo(expectedActivityInstances);
        assertThat(runtimeService.createActivityInstanceQuery().orderByExecutionId().asc().count()).isEqualTo(expectedActivityInstances);
        assertThat(runtimeService.createActivityInstanceQuery().orderByProcessDefinitionId().asc().count()).isEqualTo(expectedActivityInstances);
        assertThat(runtimeService.createActivityInstanceQuery().orderByProcessInstanceId().asc().count()).isEqualTo(expectedActivityInstances);

        assertThat(runtimeService.createActivityInstanceQuery().orderByActivityInstanceId().desc().count()).isEqualTo(expectedActivityInstances);
        assertThat(runtimeService.createActivityInstanceQuery().orderByActivityInstanceStartTime().desc().count()).isEqualTo(expectedActivityInstances);
        assertThat(runtimeService.createActivityInstanceQuery().orderByActivityInstanceEndTime().desc().count()).isEqualTo(expectedActivityInstances);
        assertThat(runtimeService.createActivityInstanceQuery().orderByActivityInstanceDuration().desc().count()).isEqualTo(expectedActivityInstances);
        assertThat(runtimeService.createActivityInstanceQuery().orderByExecutionId().desc().count()).isEqualTo(expectedActivityInstances);
        assertThat(runtimeService.createActivityInstanceQuery().orderByProcessDefinitionId().desc().count()).isEqualTo(expectedActivityInstances);
        assertThat(runtimeService.createActivityInstanceQuery().orderByProcessInstanceId().desc().count()).isEqualTo(expectedActivityInstances);
    }

    @Test
    public void testInvalidSorting() {
        assertThatThrownBy(() -> runtimeService.createActivityInstanceQuery().asc().list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);

        assertThatThrownBy(() -> runtimeService.createActivityInstanceQuery().desc().list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);

        assertThatThrownBy(() -> runtimeService.createActivityInstanceQuery().orderByActivityInstanceDuration().list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    @Deployment
    public void testBoundaryEvent() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("boundaryEventProcess");
        // Complete the task with the boundary-event on it
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        taskService.complete(task.getId());

        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();

        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();

        // Now check the history when the boundary-event is fired
        processInstance = runtimeService.startProcessInstanceByKey("boundaryEventProcess");

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        Execution signalExecution = runtimeService.createExecutionQuery().signalEventSubscriptionName("alert").singleResult();
        runtimeService.signalEventReceived("alert", signalExecution.getId());
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);

        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        ActivityInstance activityInstance = runtimeService.createActivityInstanceQuery().activityId("boundary").processInstanceId(processInstance.getId())
                .singleResult();

        assertThat(activityInstance).isNotNull();
        assertThat(activityInstance.getStartTime()).isNotNull();
        assertThat(activityInstance.getEndTime()).isNotNull();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertActivityInstancesAreSame(historyService.createHistoricActivityInstanceQuery().activityId("boundary")
                    .processInstanceId(processInstance.getId()).singleResult(), activityInstance);
        }
    }

    @Test
    @Deployment
    public void testEventBasedGateway() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("catchSignal");
        Execution waitingExecution = runtimeService.createExecutionQuery().signalEventSubscriptionName("alert").singleResult();
        assertThat(waitingExecution).isNotNull();
        runtimeService.signalEventReceived("alert", waitingExecution.getId());

        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        assertThatActivityInstancesAreSame("eventBasedgateway");
    }

    /**
     * Test to validate fix for ACT-1549: endTime of joining parallel gateway is not set
     */
    @Test
    @Deployment(resources = "org/flowable/engine/test/history/HistoricActivityInstanceTest.testParallelJoinEndTime.bpmn20.xml")
    public void testParallelJoinEndTime() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("forkJoin");

        List<org.flowable.task.api.Task> tasksToComplete = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasksToComplete).hasSize(2);

        // Complete both tasks, second task-complete should end the fork-gateway and set time
        taskService.complete(tasksToComplete.get(0).getId());
        taskService.complete(tasksToComplete.get(1).getId());

        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        List<ActivityInstance> activityInstance = runtimeService.createActivityInstanceQuery().activityId("join").processInstanceId(processInstance.getId())
                .list();

        // History contains 2 entries for parallel join (one for each path
        // arriving in the join), should contain end-time
        assertThat(activityInstance).hasSize(2);
        assertThat(activityInstance)
                .flatExtracting(ActivityInstance::getEndTime)
                .doesNotContainNull();
    }

    @Test
    @Deployment
    public void testLoop() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("historic-activity-loops", CollectionUtil.singletonMap("input", 0));

        // completing 10 user tasks
        // 15 service tasks should have passed

        for (int i = 0; i < 10; i++) {
            org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            Number inputNumber = (Number) taskService.getVariable(task.getId(), "input");
            int input = inputNumber.intValue();
            assertThat(input).isEqualTo(i);
            taskService.complete(task.getId(), CollectionUtil.singletonMap("input", input + 1));
            task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        }

        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        // Verify history
        List<ActivityInstance> taskActivityInstances = runtimeService.createActivityInstanceQuery().activityType("userTask").list();
        assertThat(taskActivityInstances).hasSize(10);
        assertThat(taskActivityInstances)
                .flatExtracting(ActivityInstance::getEndTime, ActivityInstance::getEndTime)
                .doesNotContainNull();

        List<ActivityInstance> serviceTaskInstances = runtimeService.createActivityInstanceQuery().activityType("serviceTask").list();
        assertThat(serviceTaskInstances).hasSize(15);
        assertThat(taskActivityInstances)
                .flatExtracting(ActivityInstance::getEndTime, ActivityInstance::getEndTime)
                .doesNotContainNull();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testNativeActivityInstanceTest() {
        runtimeService.startProcessInstanceByKey("oneTaskProcess");
        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);
        assertThat(
                runtimeService.createNativeActivityInstanceQuery().sql("SELECT count(*) FROM " + managementService.getTableName(ActivityInstanceEntity.class))
                        .count()).isEqualTo(3);
        assertThat(
                runtimeService.createNativeActivityInstanceQuery().sql("SELECT * FROM " + managementService.getTableName(ActivityInstanceEntity.class)).list())
                .hasSize(3);
        assertThat(runtimeService.createNativeActivityInstanceQuery().sql("SELECT * FROM " + managementService.getTableName(ActivityInstanceEntity.class))
                .listPage(0, 1)).hasSize(1);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/history/oneTaskProcess.bpmn20.xml")
    public void upgradeFromHistoryToRuntimeActivities_completeTask() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertThat(processInstance).isNotNull();

        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        managementService.executeCommand(commandContext -> {
            CommandContextUtil.getActivityInstanceEntityManager(commandContext).deleteActivityInstancesByProcessInstanceId(processInstance.getId());
            return null;
        });

        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/history/oneTaskProcess.bpmn20.xml")
    public void upgradeFromHistoryToRuntimeActivities_changeAssignee() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertThat(processInstance).isNotNull();

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        
        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 10000, 200);

        managementService.executeCommand(commandContext -> {
            CommandContextUtil.getActivityInstanceEntityManager(commandContext).deleteActivityInstancesByProcessInstanceId(processInstance.getId());
            return null;
        });

        taskService.claim(task.getId(), "newAssignee");

        assertThatActivityInstancesAreSame("theTask");
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/history/oneTaskProcess.bpmn20.xml")
    public void upgradeFromHistoryToRuntimeActivities_changeOwner() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertThat(processInstance).isNotNull();

        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        managementService.executeCommand(commandContext -> {
            CommandContextUtil.getActivityInstanceEntityManager(commandContext).deleteActivityInstancesByProcessInstanceId(processInstance.getId());
            return null;
        });

        taskService.setOwner(task.getId(), "newOwner");
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).activityId("theTask").count())
                    .isEqualTo(1);
        }

        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcessWithExpression.bpmn20.xml" })
    public void testActivityNameIsResolved() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("oneTaskProcess")
                .variable("testVar", "someTestValue").start();
        ActivityInstance taskActivity = runtimeService.createActivityInstanceQuery().activityId("theTask").processInstanceId(processInstance.getId())
                .singleResult();
        assertThat(taskActivity.getActivityName()).isEqualTo("someTestValue");

        ActivityInstance flowActivityInstance = runtimeService.createActivityInstanceQuery().processInstanceId(processInstance.getId()).activityId("flow1")
                .singleResult();

        assertThat(flowActivityInstance.getActivityName()).isEqualTo("someTestValue");

    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/ServiceTaskWithNameExpression.bpmn20.xml" })
    public void testServiceTaskWithNameExpression() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("noopProcess")
                .variable("testVar", "someTestValue").start();
        ActivityInstance taskActivity = runtimeService.createActivityInstanceQuery().activityId("noop").processInstanceId(processInstance.getId())
                .singleResult();
        assertThat(taskActivity.getActivityName()).isEqualTo("someTestValue");


    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcessWithBeanExpression.bpmn20.xml")
    public void testTaskNameExpressionIsNotResolvedTwice() {
        NameProvider nameProvider = new NameProvider();
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().transientVariable("nameProvider", nameProvider)
                .processDefinitionKey("oneTaskProcess").start();
        ActivityInstance taskActivity = runtimeService.createActivityInstanceQuery().activityId("theTask").processInstanceId(processInstance.getId())
                .singleResult();
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        
        assertThat(taskActivity.getActivityName()).isEqualTo("someName");
        assertThat(task.getName()).isEqualTo("someName");
        assertThat(nameProvider.getCounter()).isEqualTo(1);

    }

    public static class NameProvider {

        int counter = 0;

        public String getName() {
            counter++;
            return "someName";
        }

        public int getCounter() {
            return counter;
        }
    }
}
