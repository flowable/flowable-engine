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

package org.flowable.engine.test.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricActivityInstanceQuery;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class HistoricActivityInstanceTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testHistoricActivityInstanceNoop() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("noopProcess");
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery().activityId("noop").singleResult();

        assertThat(historicActivityInstance.getActivityId()).isEqualTo("noop");
        assertThat(historicActivityInstance.getActivityType()).isEqualTo("serviceTask");
        assertThat(historicActivityInstance.getProcessDefinitionId()).isNotNull();
        assertThat(historicActivityInstance.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(historicActivityInstance.getStartTime()).isNotNull();
        assertThat(historicActivityInstance.getEndTime()).isNotNull();
        assertThat(historicActivityInstance.getDurationInMillis()).isGreaterThanOrEqualTo(0);
    }
    
    @Test
    @Deployment
    public void testOneTaskProcessActivityTypes() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                        .processDefinitionKey("oneTaskProcessActivityTypesProcess")
                        .overrideProcessDefinitionTenantId("tenant1")
                        .start();
    
        Set<String> activityTypes = new HashSet<>();
        activityTypes.add("startEvent");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            List<HistoricActivityInstance> historicActivityInstance = historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).activityTypes(activityTypes).list();
            assertThat(historicActivityInstance).hasSize(1);
    
            activityTypes.add("userTask");
            List<HistoricActivityInstance> historicActivityInstance2 = historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).activityTypes(activityTypes).list();
            assertThat(historicActivityInstance2).hasSize(2);

            Calendar hourAgo = Calendar.getInstance();
            hourAgo.add(Calendar.HOUR_OF_DAY, -1);
            Calendar hourFromNow = Calendar.getInstance();
            hourFromNow.add(Calendar.HOUR_OF_DAY, 1);
    
            // Start/end dates
            assertThat(historyService.createHistoricActivityInstanceQuery().activityId("theTask").finishedBefore(hourAgo.getTime()).count()).isZero();
            assertThat(historyService.createHistoricActivityInstanceQuery().activityId("theTask").finishedBefore(hourFromNow.getTime()).count()).isZero();
            assertThat(historyService.createHistoricActivityInstanceQuery().activityId("theTask").finishedAfter(hourAgo.getTime()).count()).isZero();
            assertThat(historyService.createHistoricActivityInstanceQuery().activityId("theTask").finishedAfter(hourFromNow.getTime()).count()).isZero();
            assertThat(historyService.createHistoricActivityInstanceQuery().activityId("theTask").startedBefore(hourFromNow.getTime()).count()).isEqualTo(1);
            assertThat(historyService.createHistoricActivityInstanceQuery().activityId("theTask").startedBefore(hourAgo.getTime()).count()).isZero();
            assertThat(historyService.createHistoricActivityInstanceQuery().activityId("theTask").startedAfter(hourAgo.getTime()).count()).isEqualTo(1);
            assertThat(historyService.createHistoricActivityInstanceQuery().activityId("theTask").startedAfter(hourFromNow.getTime()).count()).isZero();
            assertThat(historyService.createHistoricActivityInstanceQuery().activityId("theTask").startedAfter(hourFromNow.getTime()).startedBefore(hourAgo.getTime()).count()).isZero();
            
            // After finishing process
            taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId());
            
            HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 5000, 200);
            
            assertThat(historyService.createHistoricActivityInstanceQuery().activityId("theTask").finished().count()).isEqualTo(1);
            assertThat(historyService.createHistoricActivityInstanceQuery().activityId("theTask").finishedBefore(hourAgo.getTime()).count()).isZero();
            assertThat(historyService.createHistoricActivityInstanceQuery().activityId("theTask").finishedBefore(hourFromNow.getTime()).count()).isEqualTo(1);
            assertThat(historyService.createHistoricActivityInstanceQuery().activityId("theTask").finishedAfter(hourAgo.getTime()).count()).isEqualTo(1);
            assertThat(historyService.createHistoricActivityInstanceQuery().activityId("theTask").finishedAfter(hourFromNow.getTime()).count()).isZero();
            assertThat(historyService.createHistoricActivityInstanceQuery().activityId("theTask").finishedBefore(hourAgo.getTime()).finishedAfter(hourFromNow.getTime()).count()).isZero();
        }
    
        ProcessInstance processInstance2 = runtimeService.createProcessInstanceBuilder()
                        .processDefinitionKey("oneTaskProcessActivityTypesProcess")
                        .overrideProcessDefinitionTenantId("tenant1")
                        .start();
        
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance2.getId()).singleResult().getId());
        
        ProcessInstance otherTenantProcessInstance = runtimeService.createProcessInstanceBuilder()
                        .processDefinitionKey("oneTaskProcessActivityTypesProcess")
                        .overrideProcessDefinitionTenantId("tenant2")
                        .start();
        
        taskService.complete(taskService.createTaskQuery().processInstanceId(otherTenantProcessInstance.getId()).singleResult().getId());
        
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertThat(historyService.createHistoricActivityInstanceQuery().activityId("theTask").finished().count()).isEqualTo(3);
            
            List<String> tenantIds = new ArrayList<>();
            tenantIds.add("tenant1");
            tenantIds.add("tenant2");
            assertThat(historyService.createHistoricActivityInstanceQuery().activityId("theTask").finished().tenantIdIn(tenantIds).count()).isEqualTo(3);
            
            assertThat(historyService.createHistoricActivityInstanceQuery().activityId("theTask").finished().tenantIdIn(Collections.singletonList("tenant1")).count()).isEqualTo(2);
            
            assertThat(historyService.createHistoricActivityInstanceQuery().activityId("theTask").finished().tenantIdIn(Collections.singletonList("tenant2")).count()).isEqualTo(1);
            
            assertThat(historyService.createHistoricActivityInstanceQuery().activityId("theTask").finished().tenantIdIn(Collections.singletonList("unexisting")).count()).isZero();
        }
    }

    @Test
    @Deployment
    public void testHistoricActivityInstanceReceive() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("receiveProcess");
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery().activityId("receive").singleResult();
        assertActivityInstancesAreSame(historicActivityInstance, runtimeService.createActivityInstanceQuery().activityInstanceId(historicActivityInstance.getId()).singleResult());

        assertThat(historicActivityInstance.getActivityId()).isEqualTo("receive");
        assertThat(historicActivityInstance.getActivityType()).isEqualTo("receiveTask");
        assertThat(historicActivityInstance.getEndTime()).isNull();
        assertThat(historicActivityInstance.getDurationInMillis()).isNull();
        assertThat(historicActivityInstance.getProcessDefinitionId()).isNotNull();
        assertThat(historicActivityInstance.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(historicActivityInstance.getStartTime()).isNotNull();

        Execution execution = runtimeService.createExecutionQuery().onlyChildExecutions().processInstanceId(processInstance.getId()).singleResult();
        runtimeService.trigger(execution.getId());
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        historicActivityInstance = historyService.createHistoricActivityInstanceQuery().activityId("receive").singleResult();

        assertThat(historicActivityInstance.getActivityId()).isEqualTo("receive");
        assertThat(historicActivityInstance.getActivityType()).isEqualTo("receiveTask");
        assertThat(historicActivityInstance.getEndTime()).isNotNull();
        assertThat(historicActivityInstance.getDurationInMillis()).isGreaterThanOrEqualTo(0);
        assertThat(historicActivityInstance.getProcessDefinitionId()).isNotNull();
        assertThat(historicActivityInstance.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(historicActivityInstance.getStartTime()).isNotNull();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/history/oneTaskProcess.bpmn20.xml")
    public void testHistoricActivityInstanceUnfinished() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertThat(processInstance).isNotNull();
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        HistoricActivityInstanceQuery historicActivityInstanceQuery = historyService.createHistoricActivityInstanceQuery();

        long finishedActivityInstanceCount = historicActivityInstanceQuery.finished().count();
        assertThat(finishedActivityInstanceCount).as("The Start event and sequence flow are completed").isEqualTo(2);

        long unfinishedActivityInstanceCount = historicActivityInstanceQuery.unfinished().count();
        assertThat(unfinishedActivityInstanceCount).as("One active (unfinished) User org.flowable.task.service.Task").isEqualTo(1);
    }

    @Test
    @Deployment
    public void testHistoricActivityInstanceQuery() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("noopProcess");
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        assertThat(historyService.createHistoricActivityInstanceQuery().activityId("nonExistingActivityId").list()).isEmpty();
        assertThat(historyService.createHistoricActivityInstanceQuery().activityId("noop").list()).hasSize(1);

        assertThat(historyService.createHistoricActivityInstanceQuery().activityType("nonExistingActivityType").list()).isEmpty();
        assertThat(historyService.createHistoricActivityInstanceQuery().activityType("serviceTask").list()).hasSize(1);

        assertThat(historyService.createHistoricActivityInstanceQuery().activityName("nonExistingActivityName").list()).isEmpty();
        assertThat(historyService.createHistoricActivityInstanceQuery().activityName("No operation").list()).hasSize(1);

        assertThat(historyService.createHistoricActivityInstanceQuery().taskAssignee("nonExistingAssignee").list()).isEmpty();

        assertThat(historyService.createHistoricActivityInstanceQuery().executionId("nonExistingExecutionId").list()).isEmpty();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).list()).hasSize(5);
        } else {
            assertThat(historyService.createHistoricActivityInstanceQuery().executionId(processInstance.getId()).list()).isEmpty();
        }

        assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId("nonExistingProcessInstanceId").list()).isEmpty();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).list()).hasSize(5);
        } else {
            assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).list()).isEmpty();
        }

        assertThat(historyService.createHistoricActivityInstanceQuery().processDefinitionId("nonExistingProcessDefinitionId").list()).isEmpty();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertThat(historyService.createHistoricActivityInstanceQuery().processDefinitionId(processInstance.getProcessDefinitionId()).list()).hasSize(5);
        } else {
            assertThat(historyService.createHistoricActivityInstanceQuery().processDefinitionId(processInstance.getProcessDefinitionId()).list()).isEmpty();
        }

        assertThat(historyService.createHistoricActivityInstanceQuery().unfinished().list()).isEmpty();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertThat(historyService.createHistoricActivityInstanceQuery().finished().list()).hasSize(5);
        } else {
            assertThat(historyService.createHistoricActivityInstanceQuery().finished().list()).isEmpty();
        }

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery().list().get(0);
            assertThat(historyService.createHistoricActivityInstanceQuery().activityInstanceId(historicActivityInstance.getId()).list()).hasSize(1);
        }
    }

    @Test
    @Deployment
    public void testQueryByProcessInstanceIds() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("noopProcess");
        ProcessInstance otherProcessInstance = runtimeService.startProcessInstanceByKey("noopProcess");
        ProcessInstance instance3 = runtimeService.startProcessInstanceByKey("noopProcess");

        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
        assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceIds(Set.of(processInstance.getId(), otherProcessInstance.getId()))
                .list()).hasSize(10);
    }

    @Test
    @Deployment
    public void testHistoricActivityInstanceForEventsQuery() {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("eventProcess");
        assertThat(taskService.createTaskQuery().count()).isEqualTo(1);
        runtimeService.signalEventReceived("signal");
        assertProcessEnded(pi.getId());
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        assertThat(historyService.createHistoricActivityInstanceQuery().activityId("noop").list()).hasSize(1);
        assertThat(historyService.createHistoricActivityInstanceQuery().activityId("userTask").list()).hasSize(1);
        assertThat(historyService.createHistoricActivityInstanceQuery().activityId("intermediate-event").list()).hasSize(1);
        assertThat(historyService.createHistoricActivityInstanceQuery().activityId("start").list()).hasSize(1);
        assertThat(historyService.createHistoricActivityInstanceQuery().activityId("end").list()).hasSize(1);

        // TODO: Discuss if boundary events will occur in the log! 
        // assertThat(
        // historyService.createHistoricActivityInstanceQuery().activityId("boundaryEvent").list()).hasSize(1);

        HistoricActivityInstance intermediateEvent = historyService.createHistoricActivityInstanceQuery().activityId("intermediate-event").singleResult();
        assertThat(intermediateEvent.getStartTime()).isNotNull();
        assertThat(intermediateEvent.getEndTime()).isNotNull();

        HistoricActivityInstance startEvent = historyService.createHistoricActivityInstanceQuery().activityId("start").singleResult();
        assertThat(startEvent.getStartTime()).isNotNull();
        assertThat(startEvent.getEndTime()).isNotNull();

        HistoricActivityInstance endEvent = historyService.createHistoricActivityInstanceQuery().activityId("end").singleResult();
        assertThat(endEvent.getStartTime()).isNotNull();
        assertThat(endEvent.getEndTime()).isNotNull();
    }

    @Test
    @Deployment
    public void testHistoricActivityInstanceProperties() {
        // Start process instance
        runtimeService.startProcessInstanceByKey("taskAssigneeProcess");
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        // Get task list
        HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery().activityId("theTask").singleResult();
        assertActivityInstancesAreSame(historicActivityInstance, runtimeService.createActivityInstanceQuery().activityInstanceId(historicActivityInstance.getId()).singleResult());

        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(historicActivityInstance.getTaskId()).isEqualTo(task.getId());
        assertThat(historicActivityInstance.getAssignee()).isEqualTo("kermit");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/history/calledProcess.bpmn20.xml", "org/flowable/engine/test/history/HistoricActivityInstanceTest.testCallSimpleSubProcess.bpmn20.xml" })
    public void testHistoricActivityInstanceCalledProcessId() {
        runtimeService.startProcessInstanceByKey("callSimpleSubProcess");
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery().activityId("callSubProcess").singleResult();

        HistoricProcessInstance oldInstance = historyService.createHistoricProcessInstanceQuery().processDefinitionKey("calledProcess").singleResult();

        assertThat(historicActivityInstance.getCalledProcessInstanceId()).isEqualTo(oldInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/history/calledProcess.bpmn20.xml",
            "org/flowable/engine/test/history/HistoricActivityInstanceTest.testCallSimpleSubProcess.bpmn20.xml" })
    public void testHistoricActivityInstanceCalledProcessIds() {
        runtimeService.createProcessInstanceBuilder().processDefinitionKey("callSimpleSubProcess").start();
        runtimeService.createProcessInstanceBuilder().processDefinitionKey("callSimpleSubProcess").start();

        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        HistoricProcessInstance calledProcess = historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey("calledProcess").list().get(0);

        HistoricActivityInstance callingActivityInstance = historyService.createHistoricActivityInstanceQuery()
                .calledProcessInstanceIds(Set.of("someId", calledProcess.getId())).singleResult();

        assertThat(callingActivityInstance.getActivityId()).isEqualTo("callSubProcess");
        assertThat(callingActivityInstance.getCalledProcessInstanceId()).isEqualTo(calledProcess.getId());

    }

    @Test
    @Deployment
    public void testSorting() {
        runtimeService.startProcessInstanceByKey("process");

        int expectedActivityInstances;
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration, 20000)) {
            expectedActivityInstances = 3;
        } else {
            expectedActivityInstances = 0;
        }

        assertThat(historyService.createHistoricActivityInstanceQuery().orderByHistoricActivityInstanceId().asc().list()).hasSize(expectedActivityInstances);
        assertThat(historyService.createHistoricActivityInstanceQuery().orderByHistoricActivityInstanceStartTime().asc().list()).hasSize(expectedActivityInstances);
        assertThat(historyService.createHistoricActivityInstanceQuery().orderByHistoricActivityInstanceEndTime().asc().list()).hasSize(expectedActivityInstances);
        assertThat(historyService.createHistoricActivityInstanceQuery().orderByHistoricActivityInstanceDuration().asc().list()).hasSize(expectedActivityInstances);
        assertThat(historyService.createHistoricActivityInstanceQuery().orderByExecutionId().asc().list()).hasSize(expectedActivityInstances);
        assertThat(historyService.createHistoricActivityInstanceQuery().orderByProcessDefinitionId().asc().list()).hasSize(expectedActivityInstances);
        assertThat(historyService.createHistoricActivityInstanceQuery().orderByProcessInstanceId().asc().list()).hasSize(expectedActivityInstances);

        assertThat(historyService.createHistoricActivityInstanceQuery().orderByHistoricActivityInstanceId().desc().list()).hasSize(expectedActivityInstances);
        assertThat(historyService.createHistoricActivityInstanceQuery().orderByHistoricActivityInstanceStartTime().desc().list()).hasSize(expectedActivityInstances);
        assertThat(historyService.createHistoricActivityInstanceQuery().orderByHistoricActivityInstanceEndTime().desc().list()).hasSize(expectedActivityInstances);
        assertThat(historyService.createHistoricActivityInstanceQuery().orderByHistoricActivityInstanceDuration().desc().list()).hasSize(expectedActivityInstances);
        assertThat(historyService.createHistoricActivityInstanceQuery().orderByExecutionId().desc().list()).hasSize(expectedActivityInstances);
        assertThat(historyService.createHistoricActivityInstanceQuery().orderByProcessDefinitionId().desc().list()).hasSize(expectedActivityInstances);
        assertThat(historyService.createHistoricActivityInstanceQuery().orderByProcessInstanceId().desc().list()).hasSize(expectedActivityInstances);

        assertThat(historyService.createHistoricActivityInstanceQuery().orderByHistoricActivityInstanceId().asc().count()).isEqualTo(expectedActivityInstances);
        assertThat(historyService.createHistoricActivityInstanceQuery().orderByHistoricActivityInstanceStartTime().asc().count()).isEqualTo(expectedActivityInstances);
        assertThat(historyService.createHistoricActivityInstanceQuery().orderByHistoricActivityInstanceEndTime().asc().count()).isEqualTo(expectedActivityInstances);
        assertThat(historyService.createHistoricActivityInstanceQuery().orderByHistoricActivityInstanceDuration().asc().count()).isEqualTo(expectedActivityInstances);
        assertThat(historyService.createHistoricActivityInstanceQuery().orderByExecutionId().asc().count()).isEqualTo(expectedActivityInstances);
        assertThat(historyService.createHistoricActivityInstanceQuery().orderByProcessDefinitionId().asc().count()).isEqualTo(expectedActivityInstances);
        assertThat(historyService.createHistoricActivityInstanceQuery().orderByProcessInstanceId().asc().count()).isEqualTo(expectedActivityInstances);

        assertThat(historyService.createHistoricActivityInstanceQuery().orderByHistoricActivityInstanceId().desc().count()).isEqualTo(expectedActivityInstances);
        assertThat(historyService.createHistoricActivityInstanceQuery().orderByHistoricActivityInstanceStartTime().desc().count()).isEqualTo(expectedActivityInstances);
        assertThat(historyService.createHistoricActivityInstanceQuery().orderByHistoricActivityInstanceEndTime().desc().count()).isEqualTo(expectedActivityInstances);
        assertThat(historyService.createHistoricActivityInstanceQuery().orderByHistoricActivityInstanceDuration().desc().count()).isEqualTo(expectedActivityInstances);
        assertThat(historyService.createHistoricActivityInstanceQuery().orderByExecutionId().desc().count()).isEqualTo(expectedActivityInstances);
        assertThat(historyService.createHistoricActivityInstanceQuery().orderByProcessDefinitionId().desc().count()).isEqualTo(expectedActivityInstances);
        assertThat(historyService.createHistoricActivityInstanceQuery().orderByProcessInstanceId().desc().count()).isEqualTo(expectedActivityInstances);
    }

    @Test
    public void testInvalidSorting() {
        assertThatThrownBy(() -> historyService.createHistoricActivityInstanceQuery().asc().list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);

        assertThatThrownBy(() -> historyService.createHistoricActivityInstanceQuery().desc().list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);

        assertThatThrownBy(() -> historyService.createHistoricActivityInstanceQuery().orderByHistoricActivityInstanceDuration().list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    /**
     * Test to validate fix for ACT-1399: Boundary-event and event-based auditing
     */
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

        HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery().activityId("boundary").processInstanceId(processInstance.getId()).singleResult();
        assertThat(historicActivityInstance).isNotNull();

        // Now check the history when the boundary-event is fired
        processInstance = runtimeService.startProcessInstanceByKey("boundaryEventProcess");

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        Execution signalExecution = runtimeService.createExecutionQuery().signalEventSubscriptionName("alert").singleResult();
        runtimeService.signalEventReceived("alert", signalExecution.getId());
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        historicActivityInstance = historyService.createHistoricActivityInstanceQuery().activityId("boundary").processInstanceId(processInstance.getId()).singleResult();

        assertThat(historicActivityInstance).isNotNull();
        assertThat(historicActivityInstance.getStartTime()).isNotNull();
        assertThat(historicActivityInstance.getEndTime()).isNotNull();
    }

    /**
     * Test to validate fix for ACT-1399: Boundary-event and event-based auditing
     */
    @Test
    @Deployment
    public void testEventBasedGateway() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("catchSignal");
        Execution waitingExecution = runtimeService.createExecutionQuery().signalEventSubscriptionName("alert").singleResult();
        assertThat(waitingExecution).isNotNull();
        runtimeService.signalEventReceived("alert", waitingExecution.getId());

        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery().activityId("eventBasedgateway").processInstanceId(processInstance.getId()).singleResult();

        assertThat(historicActivityInstance).isNotNull();
    }

    /**
     * Test to validate fix for ACT-1549: endTime of joining parallel gateway is not set
     */
    @Test
    @Deployment
    public void testParallelJoinEndTime() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("forkJoin");

        List<org.flowable.task.api.Task> tasksToComplete = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasksToComplete).hasSize(2);

        // Complete both tasks, second task-complete should end the fork-gateway and set time
        taskService.complete(tasksToComplete.get(0).getId());
        taskService.complete(tasksToComplete.get(1).getId());
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        List<HistoricActivityInstance> historicActivityInstance = historyService.createHistoricActivityInstanceQuery().activityId("join").processInstanceId(processInstance.getId()).list();

        // History contains 2 entries for parallel join (one for each path
        // arriving in the join), should contain end-time
        assertThat(historicActivityInstance).hasSize(2);
        assertThat(historicActivityInstance.get(0).getEndTime()).isNotNull();
        assertThat(historicActivityInstance.get(1).getEndTime()).isNotNull();
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
        List<HistoricActivityInstance> taskActivityInstances = historyService.createHistoricActivityInstanceQuery().activityType("userTask").list();
        assertThat(taskActivityInstances).hasSize(10);
        for (HistoricActivityInstance historicActivityInstance : taskActivityInstances) {
            assertThat(historicActivityInstance.getStartTime()).isNotNull();
            assertThat(historicActivityInstance.getEndTime()).isNotNull();
        }

        List<HistoricActivityInstance> serviceTaskInstances = historyService.createHistoricActivityInstanceQuery().activityType("serviceTask").list();
        assertThat(serviceTaskInstances).hasSize(15);
        for (HistoricActivityInstance historicActivityInstance : serviceTaskInstances) {
            assertThat(historicActivityInstance.getStartTime()).isNotNull();
            assertThat(historicActivityInstance.getEndTime()).isNotNull();
        }
    }

    @Test
    @Deployment(
        resources = {
            "org/flowable/engine/test/api/runtime/callActivity.bpmn20.xml",
            "org/flowable/engine/test/api/runtime/calledActivity.bpmn20.xml"
        }
    )
    public void callSubProcess() {
        ProcessInstance pi = this.runtimeService.startProcessInstanceByKey("callActivity");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricActivityInstance callSubProcessActivityInstance = historyService.createHistoricActivityInstanceQuery().processInstanceId(pi.getId())
                .activityId("callSubProcess").singleResult();
            assertThat(callSubProcessActivityInstance.getCalledProcessInstanceId()).isEqualTo(
                runtimeService.createProcessInstanceQuery().superProcessInstanceId(pi.getId()).singleResult().getId());
        }
    }

}
