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

import java.util.List;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.history.HistoryLevel;
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
 */
public class RuntimeActivityInstanceTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testActivityInstanceNoop() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("noopProcess");
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        ActivityInstance activityInstance = runtimeService.createActivityInstanceQuery().activityId("noop").singleResult();

        assertEquals("noop", activityInstance.getActivityId());
        assertEquals("serviceTask", activityInstance.getActivityType());
        assertNotNull(activityInstance.getProcessDefinitionId());
        assertEquals(processInstance.getId(), activityInstance.getProcessInstanceId());
        assertNotNull(activityInstance.getStartTime());
        assertNotNull(activityInstance.getEndTime());
        assertTrue(activityInstance.getDurationInMillis() >= 0);

        assertActivityInstancesAreSame(historyService.createHistoricActivityInstanceQuery().activityId("noop").singleResult(), activityInstance);

        this.runtimeService.trigger(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().singleResult().getId());
        assertEquals(0L, runtimeService.createActivityInstanceQuery().activityId("noop").count());
    }

    @Test
    @Deployment
    public void testActivityInstanceReceive() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("receiveProcess");
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        ActivityInstance activityInstance = runtimeService.createActivityInstanceQuery().activityId("receive").singleResult();

        assertEquals("receive", activityInstance.getActivityId());
        assertEquals("receiveTask", activityInstance.getActivityType());
        assertNull(activityInstance.getEndTime());
        assertNull(activityInstance.getDurationInMillis());
        assertNotNull(activityInstance.getProcessDefinitionId());
        assertEquals(processInstance.getId(), activityInstance.getProcessInstanceId());
        assertNotNull(activityInstance.getStartTime());

        assertActivityInstancesAreSame(historyService.createHistoricActivityInstanceQuery().activityId("receive").singleResult(), activityInstance);

        Execution execution = runtimeService.createExecutionQuery().onlyChildExecutions().processInstanceId(processInstance.getId()).singleResult();
        runtimeService.trigger(execution.getId());

        activityInstance = runtimeService.createActivityInstanceQuery().activityId("receive").singleResult();

        assertEquals("receive", activityInstance.getActivityId());
        assertEquals("receiveTask", activityInstance.getActivityType());
        assertNotNull(activityInstance.getEndTime());
        assertTrue(activityInstance.getDurationInMillis() >= 0);
        assertNotNull(activityInstance.getProcessDefinitionId());
        assertEquals(processInstance.getId(), activityInstance.getProcessInstanceId());
        assertNotNull(activityInstance.getStartTime());

        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
        assertActivityInstancesAreSame(historyService.createHistoricActivityInstanceQuery().activityId("receive").singleResult(), activityInstance);

        runtimeService.trigger(execution.getId());

        assertEquals(0L, runtimeService.createActivityInstanceQuery().count());
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/history/oneTaskProcess.bpmn20.xml")
    public void testActivityInstanceUnfinished() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertNotNull(processInstance);
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        ActivityInstanceQuery activityInstanceQuery = runtimeService.createActivityInstanceQuery();

        long finishedActivityInstanceCount = activityInstanceQuery.finished().count();
        assertEquals("The Start event is completed", 1, finishedActivityInstanceCount);

        long unfinishedActivityInstanceCount = activityInstanceQuery.unfinished().count();
        assertEquals("One active (unfinished) User org.flowable.task.service.Task", 1, unfinishedActivityInstanceCount);
    }

    @Test
    @Deployment(resources= "org/flowable/engine/test/api/runtime/RuntimeActivityInstanceTest.testActivityInstanceNoop.bpmn20.xml")
    public void testActivityInstanceQuery() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("noopProcess");
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        assertEquals(0, runtimeService.createActivityInstanceQuery().activityId("nonExistingActivityId").list().size());
        assertEquals(1, runtimeService.createActivityInstanceQuery().activityId("noop").list().size());

        assertEquals(0, runtimeService.createActivityInstanceQuery().activityType("nonExistingActivityType").list().size());
        assertEquals(1, runtimeService.createActivityInstanceQuery().activityType("serviceTask").list().size());

        assertEquals(0, runtimeService.createActivityInstanceQuery().activityName("nonExistingActivityName").list().size());
        assertEquals(1, runtimeService.createActivityInstanceQuery().activityName("No operation").list().size());

        assertEquals(0, runtimeService.createActivityInstanceQuery().taskAssignee("nonExistingAssignee").list().size());

        assertEquals(0, runtimeService.createActivityInstanceQuery().executionId("nonExistingExecutionId").list().size());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertEquals(3, runtimeService.createActivityInstanceQuery().processInstanceId(processInstance.getId()).list().size());
        } else {
            assertEquals(0, runtimeService.createActivityInstanceQuery().executionId(processInstance.getId()).list().size());
        }

        assertEquals(0, runtimeService.createActivityInstanceQuery().processInstanceId("nonExistingProcessInstanceId").list().size());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertEquals(3, runtimeService.createActivityInstanceQuery().processInstanceId(processInstance.getId()).list().size());
        } else {
            assertEquals(0, runtimeService.createActivityInstanceQuery().processInstanceId(processInstance.getId()).list().size());
        }

        assertEquals(0, runtimeService.createActivityInstanceQuery().processDefinitionId("nonExistingProcessDefinitionId").list().size());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertEquals(3, runtimeService.createActivityInstanceQuery().processDefinitionId(processInstance.getProcessDefinitionId()).list().size());
        } else {
            assertEquals(0, runtimeService.createActivityInstanceQuery().processDefinitionId(processInstance.getProcessDefinitionId()).list().size());
        }

        assertEquals(1, runtimeService.createActivityInstanceQuery().unfinished().list().size());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertEquals(2, runtimeService.createActivityInstanceQuery().finished().list().size());
        } else {
            assertEquals(0, runtimeService.createActivityInstanceQuery().finished().list().size());
        }

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            ActivityInstance activityInstance = runtimeService.createActivityInstanceQuery().list().get(0);
            assertEquals(1, runtimeService.createActivityInstanceQuery().activityInstanceId(activityInstance.getId()).list().size());
        }
    }

    @Test
    @Deployment
    public void testActivityInstanceForEventsQuery() {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("eventProcess");
        assertEquals(1, taskService.createTaskQuery().count());
        runtimeService.signalEventReceived("signal");

        assertThatActivityInstancesAreSame("noop");
        assertThatActivityInstancesAreSame("userTask");
        assertThatActivityInstancesAreSame("intermediate-event");
        assertThatActivityInstancesAreSame("start");

        assertThatActivityInstancesAreSame("intermediate-event");

        assertThatActivityInstancesAreSame("start");

        runtimeService.trigger(runtimeService.createExecutionQuery().processInstanceId(pi.getId()).onlyChildExecutions().singleResult().getId());

        assertEquals(runtimeService.createActivityInstanceQuery().processInstanceId(pi.getId()).count(), 0L);
    }

    protected void assertThatActivityInstancesAreSame(String userTask) {
        assertActivityInstancesAreSame(historyService.createHistoricActivityInstanceQuery().activityId(userTask).singleResult(),
            runtimeService.createActivityInstanceQuery().activityId(userTask).singleResult());
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/history/HistoricActivityInstanceTest.testHistoricActivityInstanceProperties.bpmn20.xml")
    public void testActivityInstanceProperties() {
        // Start process instance
        runtimeService.startProcessInstanceByKey("taskAssigneeProcess");
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        // Get task list
        ActivityInstance activityInstance = runtimeService.createActivityInstanceQuery().activityId("theTask").singleResult();

        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertEquals(task.getId(), activityInstance.getTaskId());
        assertEquals("kermit", activityInstance.getAssignee());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/history/calledProcess.bpmn20.xml", "org/flowable/engine/test/history/HistoricActivityInstanceTest.testCallSimpleSubProcess.bpmn20.xml" })
    public void processInstanceEndRemovesAllActivityInstances() {
        runtimeService.startProcessInstanceByKey("callSimpleSubProcess");
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        assertEquals(runtimeService.createProcessInstanceQuery().count(), 0L);
        assertEquals(runtimeService.createActivityInstanceQuery().count(), 0L);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/history/calledProcess.bpmn20.xml", "org/flowable/engine/test/api/runtime/RuntimeActivityInstanceTest.testCallSimpleSubProcess.bpmn20.xml" })
    public void testActivityInstanceCalledProcessId() {
        runtimeService.startProcessInstanceByKey("callSimpleSubProcess");

        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        ActivityInstance activityInstance = runtimeService.createActivityInstanceQuery().activityId("callSubProcess").singleResult();

        HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery().activityId("callSubProcess").singleResult();
        assertActivityInstancesAreSame(historicActivityInstance, activityInstance);

        HistoricProcessInstance oldInstance = historyService.createHistoricProcessInstanceQuery().processDefinitionKey("calledProcess").singleResult();

        assertEquals(oldInstance.getId(), activityInstance.getCalledProcessInstanceId());
    }

    @Test
    @Deployment
    public void testSorting() {
        runtimeService.startProcessInstanceByKey("process");

        int expectedActivityInstances;
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            expectedActivityInstances = 2;
        } else {
            expectedActivityInstances = 0;
        }

        assertEquals(expectedActivityInstances, runtimeService.createActivityInstanceQuery().orderByActivityInstanceId().asc().list().size());
        assertEquals(expectedActivityInstances, runtimeService.createActivityInstanceQuery().orderByActivityInstanceStartTime().asc().list().size());
        assertEquals(expectedActivityInstances, runtimeService.createActivityInstanceQuery().orderByActivityInstanceEndTime().asc().list().size());
        assertEquals(expectedActivityInstances, runtimeService.createActivityInstanceQuery().orderByActivityInstanceDuration().asc().list().size());
        assertEquals(expectedActivityInstances, runtimeService.createActivityInstanceQuery().orderByExecutionId().asc().list().size());
        assertEquals(expectedActivityInstances, runtimeService.createActivityInstanceQuery().orderByProcessDefinitionId().asc().list().size());
        assertEquals(expectedActivityInstances, runtimeService.createActivityInstanceQuery().orderByProcessInstanceId().asc().list().size());

        assertEquals(expectedActivityInstances, runtimeService.createActivityInstanceQuery().orderByActivityInstanceId().desc().list().size());
        assertEquals(expectedActivityInstances, runtimeService.createActivityInstanceQuery().orderByActivityInstanceStartTime().desc().list().size());
        assertEquals(expectedActivityInstances, runtimeService.createActivityInstanceQuery().orderByActivityInstanceEndTime().desc().list().size());
        assertEquals(expectedActivityInstances, runtimeService.createActivityInstanceQuery().orderByActivityInstanceDuration().desc().list().size());
        assertEquals(expectedActivityInstances, runtimeService.createActivityInstanceQuery().orderByExecutionId().desc().list().size());
        assertEquals(expectedActivityInstances, runtimeService.createActivityInstanceQuery().orderByProcessDefinitionId().desc().list().size());
        assertEquals(expectedActivityInstances, runtimeService.createActivityInstanceQuery().orderByProcessInstanceId().desc().list().size());

        assertEquals(expectedActivityInstances, runtimeService.createActivityInstanceQuery().orderByActivityInstanceId().asc().count());
        assertEquals(expectedActivityInstances, runtimeService.createActivityInstanceQuery().orderByActivityInstanceStartTime().asc().count());
        assertEquals(expectedActivityInstances, runtimeService.createActivityInstanceQuery().orderByActivityInstanceEndTime().asc().count());
        assertEquals(expectedActivityInstances, runtimeService.createActivityInstanceQuery().orderByActivityInstanceDuration().asc().count());
        assertEquals(expectedActivityInstances, runtimeService.createActivityInstanceQuery().orderByExecutionId().asc().count());
        assertEquals(expectedActivityInstances, runtimeService.createActivityInstanceQuery().orderByProcessDefinitionId().asc().count());
        assertEquals(expectedActivityInstances, runtimeService.createActivityInstanceQuery().orderByProcessInstanceId().asc().count());

        assertEquals(expectedActivityInstances, runtimeService.createActivityInstanceQuery().orderByActivityInstanceId().desc().count());
        assertEquals(expectedActivityInstances, runtimeService.createActivityInstanceQuery().orderByActivityInstanceStartTime().desc().count());
        assertEquals(expectedActivityInstances, runtimeService.createActivityInstanceQuery().orderByActivityInstanceEndTime().desc().count());
        assertEquals(expectedActivityInstances, runtimeService.createActivityInstanceQuery().orderByActivityInstanceDuration().desc().count());
        assertEquals(expectedActivityInstances, runtimeService.createActivityInstanceQuery().orderByExecutionId().desc().count());
        assertEquals(expectedActivityInstances, runtimeService.createActivityInstanceQuery().orderByProcessDefinitionId().desc().count());
        assertEquals(expectedActivityInstances, runtimeService.createActivityInstanceQuery().orderByProcessInstanceId().desc().count());
    }

    @Test
    public void testInvalidSorting() {
        try {
            runtimeService.createActivityInstanceQuery().asc().list();
            fail();
        } catch (FlowableIllegalArgumentException e) {

        }

        try {
            runtimeService.createActivityInstanceQuery().desc().list();
            fail();
        } catch (FlowableIllegalArgumentException e) {

        }

        try {
            runtimeService.createActivityInstanceQuery().orderByActivityInstanceDuration().list();
            fail();
        } catch (FlowableIllegalArgumentException e) {

        }
    }

    @Test
    @Deployment
    public void testBoundaryEvent() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("boundaryEventProcess");
        // Complete the task with the boundary-event on it
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(task);
        taskService.complete(task.getId());

        assertEquals(0L, runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count());
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        // Check if there is NO historic activity instance for a boundary-event that has not triggered
        ActivityInstance activityInstance = runtimeService.createActivityInstanceQuery().activityId("boundary").processInstanceId(processInstance.getId()).singleResult();

        assertNull(activityInstance);

        // Now check the history when the boundary-event is fired
        processInstance = runtimeService.startProcessInstanceByKey("boundaryEventProcess");

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        Execution signalExecution = runtimeService.createExecutionQuery().signalEventSubscriptionName("alert").singleResult();
        runtimeService.signalEventReceived("alert", signalExecution.getId());
        assertEquals(1L, runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count());
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        activityInstance = runtimeService.createActivityInstanceQuery().activityId("boundary").processInstanceId(processInstance.getId()).singleResult();

        assertNotNull(activityInstance);
        assertNotNull(activityInstance.getStartTime());
        assertNotNull(activityInstance.getEndTime());

        assertActivityInstancesAreSame(historyService.createHistoricActivityInstanceQuery().
            activityId("boundary").
            processInstanceId(processInstance.getId()).
            singleResult(),
            activityInstance);
    }

    @Test
    @Deployment
    public void testEventBasedGateway() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("catchSignal");
        Execution waitingExecution = runtimeService.createExecutionQuery().signalEventSubscriptionName("alert").singleResult();
        assertNotNull(waitingExecution);
        runtimeService.signalEventReceived("alert", waitingExecution.getId());

        assertEquals(1L, runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count());
        
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
        assertEquals(2, tasksToComplete.size());

        // Complete both tasks, second task-complete should end the fork-gateway and set time
        taskService.complete(tasksToComplete.get(0).getId());
        taskService.complete(tasksToComplete.get(1).getId());
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        List<ActivityInstance> activityInstance = runtimeService.createActivityInstanceQuery().activityId("join").processInstanceId(processInstance.getId()).list();

        assertNotNull(activityInstance);

        // History contains 2 entries for parallel join (one for each path
        // arriving in the join), should contain end-time
        assertEquals(2, activityInstance.size());
        assertNotNull(activityInstance.get(0).getEndTime());
        assertNotNull(activityInstance.get(1).getEndTime());
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
            assertEquals(i, input);
            taskService.complete(task.getId(), CollectionUtil.singletonMap("input", input + 1));
            task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        }
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        // Verify history
        List<ActivityInstance> taskActivityInstances = runtimeService.createActivityInstanceQuery().activityType("userTask").list();
        assertEquals(10, taskActivityInstances.size());
        for (ActivityInstance activityInstance : taskActivityInstances) {
            assertNotNull(activityInstance.getStartTime());
            assertNotNull(activityInstance.getEndTime());
        }

        List<ActivityInstance> serviceTaskInstances = runtimeService.createActivityInstanceQuery().activityType("serviceTask").list();
        assertEquals(15, serviceTaskInstances.size());
        for (ActivityInstance activityInstance : serviceTaskInstances) {
            assertNotNull(activityInstance.getStartTime());
            assertNotNull(activityInstance.getEndTime());
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testNativeActivityInstanceTest() {
        runtimeService.startProcessInstanceByKey("oneTaskProcess");
        HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 7000, 200);
        assertEquals(2, runtimeService.createNativeActivityInstanceQuery().sql("SELECT count(*) FROM " + managementService.getTableName(ActivityInstanceEntity.class)).count());
        assertEquals(2, runtimeService.createNativeActivityInstanceQuery().sql("SELECT * FROM " + managementService.getTableName(ActivityInstanceEntity.class)).list().size());
        assertEquals(1, runtimeService.createNativeActivityInstanceQuery().sql("SELECT * FROM " + managementService.getTableName(ActivityInstanceEntity.class)).listPage(0, 1).size());
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/history/oneTaskProcess.bpmn20.xml")
    public void upgradeFromHistoryToRuntimeActivities_completeTask() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertNotNull(processInstance);

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
        assertNotNull(processInstance);

        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

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
        assertNotNull(processInstance);

        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        managementService.executeCommand(commandContext -> {
            CommandContextUtil.getActivityInstanceEntityManager(commandContext).deleteActivityInstancesByProcessInstanceId(processInstance.getId());
            return null;
        });

        taskService.setOwner(task.getId(), "newOwner");
        assertEquals(1, historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).activityId("theTask").count());
        taskService.complete(task.getId());


        assertProcessEnded(processInstance.getId());
    }

}
