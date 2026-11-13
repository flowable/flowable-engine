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

package org.flowable.engine.test.bpmn.subprocess.adhoc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.bpmn.model.FlowNode;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.delegate.event.FlowableActivityCancelledEvent;
import org.flowable.engine.history.DeleteReason;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.runtime.callback.ProcessInstanceState;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.api.event.TestFlowableActivityEventListener;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.Test;

/**
 * @author Tijs Rademakers
 */
public class AdhocSubProcessTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testSimpleAdhocSubProcess() {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("simpleSubProcess");
        Execution execution = runtimeService.createExecutionQuery().activityId("adhocSubProcess").singleResult();
        assertThat(execution).isNotNull();

        List<FlowNode> enabledActivities = runtimeService.getEnabledActivitiesFromAdhocSubProcess(execution.getId());
        assertThat(enabledActivities).hasSize(2);

        Execution newTaskExecution = runtimeService.executeActivityInAdhocSubProcess(execution.getId(), "subProcessTask");
        assertThat(newTaskExecution).isNotNull();
        assertThat(newTaskExecution.getId()).isNotNull();

        org.flowable.task.api.Task subProcessTask = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("subProcessTask").singleResult();
        assertThat(subProcessTask.getName()).isEqualTo("Task in subprocess");

        taskService.complete(subProcessTask.getId());

        enabledActivities = runtimeService.getEnabledActivitiesFromAdhocSubProcess(execution.getId());
        assertThat(enabledActivities).hasSize(2);

        runtimeService.completeAdhocSubProcess(execution.getId());

        org.flowable.task.api.Task afterTask = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        assertThat(afterTask.getName()).isEqualTo("After task");

        taskService.complete(afterTask.getId());

        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(pi.getId()).singleResult()).isNull();
    }

    @Test
    @Deployment
    public void testSimpleAdhocSubProcessViaExecution() {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("simpleSubProcess");
        List<Execution> executions = runtimeService.getAdhocSubProcessExecutions(pi.getId());
        assertThat(executions).hasSize(1);

        List<FlowNode> enabledActivities = runtimeService.getEnabledActivitiesFromAdhocSubProcess(executions.get(0).getId());
        assertThat(enabledActivities).hasSize(2);

        Execution newTaskExecution = runtimeService.executeActivityInAdhocSubProcess(executions.get(0).getId(), "subProcessTask");
        assertThat(newTaskExecution).isNotNull();
        assertThat(newTaskExecution.getId()).isNotNull();

        org.flowable.task.api.Task subProcessTask = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("subProcessTask").singleResult();
        assertThat(subProcessTask.getName()).isEqualTo("Task in subprocess");

        taskService.complete(subProcessTask.getId());

        executions = runtimeService.getAdhocSubProcessExecutions(pi.getId());
        assertThat(executions).hasSize(1);

        enabledActivities = runtimeService.getEnabledActivitiesFromAdhocSubProcess(executions.get(0).getId());
        assertThat(enabledActivities).hasSize(2);

        runtimeService.completeAdhocSubProcess(executions.get(0).getId());

        executions = runtimeService.getAdhocSubProcessExecutions(pi.getId());
        assertThat(executions).isEmpty();

        org.flowable.task.api.Task afterTask = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        assertThat(afterTask.getName()).isEqualTo("After task");

        taskService.complete(afterTask.getId());

        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(pi.getId()).singleResult()).isNull();
    }

    @Test
    @Deployment
    public void testSimpleCompletionCondition() {
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("completed", false);
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("simpleSubProcess", variableMap);
        Execution execution = runtimeService.createExecutionQuery().activityId("adhocSubProcess").singleResult();
        assertThat(execution).isNotNull();

        List<FlowNode> enabledActivities = runtimeService.getEnabledActivitiesFromAdhocSubProcess(execution.getId());
        assertThat(enabledActivities).hasSize(2);

        Execution newTaskExecution = runtimeService.executeActivityInAdhocSubProcess(execution.getId(), "subProcessTask");
        assertThat(newTaskExecution).isNotNull();
        assertThat(newTaskExecution.getId()).isNotNull();

        org.flowable.task.api.Task subProcessTask = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("subProcessTask").singleResult();
        assertThat(subProcessTask.getName()).isEqualTo("Task in subprocess");

        taskService.complete(subProcessTask.getId());

        enabledActivities = runtimeService.getEnabledActivitiesFromAdhocSubProcess(execution.getId());
        assertThat(enabledActivities).hasSize(2);

        newTaskExecution = runtimeService.executeActivityInAdhocSubProcess(execution.getId(), "subProcessTask2");

        subProcessTask = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        assertThat(subProcessTask.getName()).isEqualTo("Task2 in subprocess");

        variableMap = new HashMap<>();
        variableMap.put("completed", true);
        taskService.complete(subProcessTask.getId(), variableMap);

        org.flowable.task.api.Task afterTask = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        assertThat(afterTask.getName()).isEqualTo("After task");

        taskService.complete(afterTask.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {

            List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(pi.getId())
                    .orderByHistoricTaskInstanceEndTime()
                    .asc()
                    .list();

            // only check for existence and assume that the SQL processing has ordered the values correctly
            // see https://github.com/flowable/flowable-engine/issues/8
            assertThat(historicTasks)
                    .extracting(HistoricTaskInstance::getTaskDefinitionKey)
                    .containsExactlyInAnyOrder("subProcessTask", "subProcessTask2", "afterTask");
        }

        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(pi.getId()).singleResult()).isNull();
    }

    @Test
    @Deployment
    public void testParallelAdhocSubProcess() {
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("completed", false);
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("simpleSubProcess", variableMap);
        Execution execution = runtimeService.createExecutionQuery().activityId("adhocSubProcess").singleResult();
        assertThat(execution).isNotNull();

        List<FlowNode> enabledActivities = runtimeService.getEnabledActivitiesFromAdhocSubProcess(execution.getId());
        assertThat(enabledActivities).hasSize(2);

        runtimeService.executeActivityInAdhocSubProcess(execution.getId(), "subProcessTask");
        org.flowable.task.api.Task subProcessTask = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        assertThat(subProcessTask.getName()).isEqualTo("Task in subprocess");

        runtimeService.executeActivityInAdhocSubProcess(execution.getId(), "subProcessTask2");
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).list();
        assertThat(tasks).hasSize(2);

        variableMap = new HashMap<>();
        variableMap.put("completed", true);
        taskService.complete(subProcessTask.getId(), variableMap);

        org.flowable.task.api.Task afterTask = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        assertThat(afterTask.getName()).isEqualTo("After task");

        taskService.complete(afterTask.getId());

        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(pi.getId()).singleResult()).isNull();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/subprocess/adhoc/AdhocSubProcessTest.testParallelAdhocSubProcess.bpmn20.xml")
    public void testCompletionConditionSetsDeleteReasonOnRemainingTasks() {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("simpleSubProcess", Collections.singletonMap("completed", false));
        Execution execution = runtimeService.createExecutionQuery().activityId("adhocSubProcess").singleResult();

        runtimeService.executeActivityInAdhocSubProcess(execution.getId(), "subProcessTask");
        runtimeService.executeActivityInAdhocSubProcess(execution.getId(), "subProcessTask2");
        Task subProcessTask = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("subProcessTask").singleResult();
        Task subProcessTask2 = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("subProcessTask2").singleResult();

        taskService.complete(subProcessTask.getId(), Collections.singletonMap("completed", true));

        assertThat(taskService.createTaskQuery().processInstanceId(pi.getId()).list())
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("afterTask");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(pi.getId())
                    .activityId("subProcessTask").singleResult().getDeleteReason()).isNull();
            assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(pi.getId())
                    .activityId("subProcessTask2").singleResult().getDeleteReason()).isEqualTo(DeleteReason.ADHOC_SUBPROCESS_COMPLETED);
        }

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            HistoricTaskInstance completedTask = historyService.createHistoricTaskInstanceQuery().taskId(subProcessTask.getId()).singleResult();
            assertThat(completedTask.getDeleteReason()).isNull();

            HistoricTaskInstance cancelledTask = historyService.createHistoricTaskInstanceQuery().taskId(subProcessTask2.getId()).singleResult();
            assertThat(cancelledTask.getEndTime()).isNotNull();
            assertThat(cancelledTask.getDeleteReason()).isEqualTo(DeleteReason.ADHOC_SUBPROCESS_COMPLETED);
        }
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/bpmn/subprocess/adhoc/AdhocSubProcessTest.testCompletionConditionWithRemainingCallActivity.bpmn20.xml",
            "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml"
    })
    public void testCompletionConditionWithRemainingCallActivity() {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("simpleSubProcess", Collections.singletonMap("completed", false));
        Execution execution = runtimeService.createExecutionQuery().activityId("adhocSubProcess").singleResult();

        runtimeService.executeActivityInAdhocSubProcess(execution.getId(), "subProcessTask");
        runtimeService.executeActivityInAdhocSubProcess(execution.getId(), "callActivity");
        Task subProcessTask = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("subProcessTask").singleResult();
        ProcessInstance childProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(pi.getId()).singleResult();
        Task childTask = taskService.createTaskQuery().processInstanceId(childProcessInstance.getId()).singleResult();

        taskService.complete(subProcessTask.getId(), Collections.singletonMap("completed", true));

        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(childProcessInstance.getId()).count()).isZero();
        assertThat(taskService.createTaskQuery().processInstanceId(pi.getId()).list())
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("afterTask");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(pi.getId())
                    .activityId("callActivity").singleResult().getDeleteReason()).isEqualTo(DeleteReason.ADHOC_SUBPROCESS_COMPLETED);

            // The remaining activities are cancelled, so the child process instance is ended as cancelled
            HistoricProcessInstance historicChildProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(childProcessInstance.getId()).singleResult();
            assertThat(historicChildProcessInstance.getEndTime()).isNotNull();
            assertThat(historicChildProcessInstance.getState()).isEqualTo(ProcessInstanceState.CANCELLED);
            assertThat(historicChildProcessInstance.getDeleteReason()).isEqualTo(DeleteReason.ADHOC_SUBPROCESS_COMPLETED);
        }

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            HistoricTaskInstance historicChildTask = historyService.createHistoricTaskInstanceQuery().taskId(childTask.getId()).singleResult();
            assertThat(historicChildTask.getEndTime()).isNotNull();
            assertThat(historicChildTask.getDeleteReason()).isEqualTo(DeleteReason.ADHOC_SUBPROCESS_COMPLETED);
        }
    }

    @Test
    @Deployment
    public void testCompletionConditionWithMultipleOutgoingFlows() {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("simpleSubProcess", Collections.singletonMap("completed", false));
        Execution execution = runtimeService.createExecutionQuery().activityId("adhocSubProcess").singleResult();

        runtimeService.executeActivityInAdhocSubProcess(execution.getId(), "subProcessTask");
        runtimeService.executeActivityInAdhocSubProcess(execution.getId(), "subProcessTask2");
        Task subProcessTask = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("subProcessTask").singleResult();
        Task subProcessTask2 = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("subProcessTask2").singleResult();

        TestFlowableActivityEventListener listener = new TestFlowableActivityEventListener(true);
        runtimeService.addEventListener(listener, FlowableEngineEventType.ACTIVITY_CANCELLED);
        try {
            taskService.complete(subProcessTask.getId(), Collections.singletonMap("completed", true));
        } finally {
            runtimeService.removeEventListener(listener);
        }

        assertThat(listener.getEventsReceived())
                .singleElement()
                .isInstanceOfSatisfying(FlowableActivityCancelledEvent.class, event -> {
                    assertThat(event.getActivityId()).isEqualTo("subProcessTask2");
                    assertThat(((FlowNode) event.getCause()).getId()).isEqualTo("subProcessTask");
                });

        assertThat(taskService.createTaskQuery().processInstanceId(pi.getId()).list())
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("afterTask");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(pi.getId()).activityType("sequenceFlow").list())
                    .extracting(HistoricActivityInstance::getActivityId)
                    .containsExactlyInAnyOrder("flow1", "subflow1", "subflow2", "flow2");
            assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(pi.getId())
                    .activityId("subProcessTask2").singleResult().getDeleteReason()).isEqualTo(DeleteReason.ADHOC_SUBPROCESS_COMPLETED);
        }

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            HistoricTaskInstance cancelledTask = historyService.createHistoricTaskInstanceQuery().taskId(subProcessTask2.getId()).singleResult();
            assertThat(cancelledTask.getEndTime()).isNotNull();
            assertThat(cancelledTask.getDeleteReason()).isEqualTo(DeleteReason.ADHOC_SUBPROCESS_COMPLETED);
        }
    }

    @Test
    @Deployment
    public void testSequentialAdhocSubProcess() {
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("completed", false);
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("simpleSubProcess", variableMap);
        Execution execution = runtimeService.createExecutionQuery().activityId("adhocSubProcess").singleResult();
        assertThat(execution).isNotNull();

        List<FlowNode> enabledActivities = runtimeService.getEnabledActivitiesFromAdhocSubProcess(execution.getId());
        assertThat(enabledActivities).hasSize(2);

        runtimeService.executeActivityInAdhocSubProcess(execution.getId(), "subProcessTask");
        org.flowable.task.api.Task subProcessTask = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        assertThat(subProcessTask.getName()).isEqualTo("Task in subprocess");

        assertThatThrownBy(() -> runtimeService.executeActivityInAdhocSubProcess(execution.getId(), "subProcessTask2"))
                .as("exception expected because can only enable one activity in a sequential ad-hoc sub process")
                .isInstanceOf(FlowableException.class);

        taskService.complete(subProcessTask.getId());

        // now we can enable the activity
        runtimeService.executeActivityInAdhocSubProcess(execution.getId(), "subProcessTask2");

        subProcessTask = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        assertThat(subProcessTask.getName()).isEqualTo("Task2 in subprocess");

        variableMap = new HashMap<>();
        variableMap.put("completed", true);
        taskService.complete(subProcessTask.getId(), variableMap);

        org.flowable.task.api.Task afterTask = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        assertThat(afterTask.getName()).isEqualTo("After task");

        taskService.complete(afterTask.getId());

        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(pi.getId()).singleResult()).isNull();
    }

    @Test
    @Deployment
    public void testFlowsInAdhocSubProcess() {
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("completed", false);
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("simpleSubProcess", variableMap);
        Execution execution = runtimeService.createExecutionQuery().activityId("adhocSubProcess").singleResult();
        assertThat(execution).isNotNull();

        List<FlowNode> enabledActivities = runtimeService.getEnabledActivitiesFromAdhocSubProcess(execution.getId());
        assertThat(enabledActivities).hasSize(2);

        runtimeService.executeActivityInAdhocSubProcess(execution.getId(), "subProcessTask");
        org.flowable.task.api.Task subProcessTask = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        assertThat(subProcessTask.getName()).isEqualTo("Task in subprocess");

        taskService.complete(subProcessTask.getId());

        assertThatThrownBy(() -> runtimeService.executeActivityInAdhocSubProcess(execution.getId(), "subProcessTask2"))
                .as("exception expected because can only enable one activity in a sequential ad-hoc sub process")
                .isInstanceOf(FlowableException.class);

        subProcessTask = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        assertThat(subProcessTask.getName()).isEqualTo("The next task");

        variableMap = new HashMap<>();
        variableMap.put("completed", true);
        taskService.complete(subProcessTask.getId(), variableMap);

        org.flowable.task.api.Task afterTask = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        assertThat(afterTask.getName()).isEqualTo("After task");

        taskService.complete(afterTask.getId());

        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(pi.getId()).singleResult()).isNull();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/subprocess/adhoc/AdhocSubProcessTest.testFlowsInAdhocSubProcess.bpmn20.xml")
    public void testCompleteFlowBeforeEndInAdhocSubProcess() {
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("completed", false);
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("simpleSubProcess", variableMap);
        Execution execution = runtimeService.createExecutionQuery().activityId("adhocSubProcess").singleResult();
        assertThat(execution).isNotNull();

        List<FlowNode> enabledActivities = runtimeService.getEnabledActivitiesFromAdhocSubProcess(execution.getId());
        assertThat(enabledActivities).hasSize(2);

        runtimeService.executeActivityInAdhocSubProcess(execution.getId(), "subProcessTask");
        org.flowable.task.api.Task subProcessTask = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        assertThat(subProcessTask.getName()).isEqualTo("Task in subprocess");

        variableMap = new HashMap<>();
        variableMap.put("completed", true);
        taskService.complete(subProcessTask.getId(), variableMap);

        org.flowable.task.api.Task afterTask = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        assertThat(afterTask.getName()).isEqualTo("After task");

        taskService.complete(afterTask.getId());

        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(pi.getId()).singleResult()).isNull();
    }

    @Test
    @Deployment
    public void testParallelFlowsInAdhocSubProcess() {
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("completed", false);
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("simpleSubProcess", variableMap);
        Execution execution = runtimeService.createExecutionQuery().activityId("adhocSubProcess").singleResult();
        assertThat(execution).isNotNull();

        List<FlowNode> enabledActivities = runtimeService.getEnabledActivitiesFromAdhocSubProcess(execution.getId());
        assertThat(enabledActivities).hasSize(3);

        runtimeService.executeActivityInAdhocSubProcess(execution.getId(), "subProcessTask");
        org.flowable.task.api.Task subProcessTask = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        assertThat(subProcessTask.getName()).isEqualTo("Task in subprocess");

        runtimeService.executeActivityInAdhocSubProcess(execution.getId(), "subProcessTask2");
        runtimeService.executeActivityInAdhocSubProcess(execution.getId(), "subProcessTask3");

        org.flowable.task.api.Task subProcessTask2 = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("subProcessTask2").singleResult();
        assertThat(subProcessTask2.getName()).isEqualTo("Task2 in subprocess");
        taskService.complete(subProcessTask2.getId());

        org.flowable.task.api.Task sequentialTask2 = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("sequentialTask2").singleResult();
        assertThat(sequentialTask2.getName()).isEqualTo("The next task2");
        org.flowable.task.api.Task subProcessTask3 = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("subProcessTask3").singleResult();

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).list();
        assertThat(tasks).hasSize(3);

        variableMap = new HashMap<>();
        variableMap.put("completed", true);
        taskService.complete(subProcessTask.getId(), variableMap);

        org.flowable.task.api.Task afterTask = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        assertThat(afterTask.getName()).isEqualTo("After task");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            assertThat(historyService.createHistoricTaskInstanceQuery().taskIds(List.of(sequentialTask2.getId(), subProcessTask3.getId())).list())
                    .extracting(HistoricTaskInstance::getDeleteReason)
                    .containsExactly(DeleteReason.ADHOC_SUBPROCESS_COMPLETED, DeleteReason.ADHOC_SUBPROCESS_COMPLETED);
        }

        taskService.complete(afterTask.getId());

        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(pi.getId()).singleResult()).isNull();
    }

    @Test
    @Deployment
    public void testKeepRemainingInstancesAdhocSubProcess() {
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("completed", false);
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("simpleSubProcess", variableMap);
        Execution execution = runtimeService.createExecutionQuery().activityId("adhocSubProcess").singleResult();
        assertThat(execution).isNotNull();

        List<FlowNode> enabledActivities = runtimeService.getEnabledActivitiesFromAdhocSubProcess(execution.getId());
        assertThat(enabledActivities).hasSize(2);

        runtimeService.executeActivityInAdhocSubProcess(execution.getId(), "subProcessTask");
        org.flowable.task.api.Task subProcessTask = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        assertThat(subProcessTask.getName()).isEqualTo("Task in subprocess");

        runtimeService.executeActivityInAdhocSubProcess(execution.getId(), "subProcessTask2");
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).list();
        assertThat(tasks).hasSize(2);

        variableMap = new HashMap<>();
        variableMap.put("completed", true);
        taskService.complete(subProcessTask.getId(), variableMap);

        // ad-hoc sub process is not completed because of cancelRemainingInstances is set to false
        subProcessTask = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        assertThat(subProcessTask.getName()).isEqualTo("Task2 in subprocess");

        taskService.complete(subProcessTask.getId());

        // with no remaining executions the ad-hoc sub process will be completed
        org.flowable.task.api.Task afterTask = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        assertThat(afterTask.getName()).isEqualTo("After task");

        taskService.complete(afterTask.getId());

        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(pi.getId()).singleResult()).isNull();
    }

    @Test
    @Deployment
    public void testParallelFlowsWithKeepRemainingInstancesAdhocSubProcess() {
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("completed", false);
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("simpleSubProcess", variableMap);
        Execution execution = runtimeService.createExecutionQuery().activityId("adhocSubProcess").singleResult();
        assertThat(execution).isNotNull();

        List<FlowNode> enabledActivities = runtimeService.getEnabledActivitiesFromAdhocSubProcess(execution.getId());
        assertThat(enabledActivities).hasSize(3);

        runtimeService.executeActivityInAdhocSubProcess(execution.getId(), "subProcessTask");
        org.flowable.task.api.Task subProcessTask = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        assertThat(subProcessTask.getName()).isEqualTo("Task in subprocess");

        runtimeService.executeActivityInAdhocSubProcess(execution.getId(), "subProcessTask2");
        runtimeService.executeActivityInAdhocSubProcess(execution.getId(), "subProcessTask3");

        org.flowable.task.api.Task subProcessTask2 = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("subProcessTask2").singleResult();
        assertThat(subProcessTask2.getName()).isEqualTo("Task2 in subprocess");
        taskService.complete(subProcessTask2.getId());

        subProcessTask2 = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("sequentialTask2").singleResult();
        assertThat(subProcessTask2.getName()).isEqualTo("The next task2");

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).list();
        assertThat(tasks).hasSize(3);

        variableMap = new HashMap<>();
        variableMap.put("completed", true);
        taskService.complete(subProcessTask.getId(), variableMap);

        // ad-hoc sub process is not completed because of cancelRemainingInstances is set to false
        tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).list();
        assertThat(tasks).hasSize(3);

        subProcessTask = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("sequentialTask").singleResult();
        assertThat(subProcessTask.getName()).isEqualTo("The next task");

        taskService.complete(subProcessTask.getId(), variableMap);

        // ad-hoc sub process is not completed because of cancelRemainingInstances is set to false
        tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).list();
        assertThat(tasks).hasSize(2);

        taskService.complete(subProcessTask2.getId(), variableMap);

        // ad-hoc sub process is not completed because of cancelRemainingInstances is set to false
        org.flowable.task.api.Task subProcessTask3 = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        assertThat(subProcessTask3.getName()).isEqualTo("Task3 in subprocess");

        taskService.complete(subProcessTask3.getId(), variableMap);

        // with no remaining executions the ad-hoc sub process will be completed
        org.flowable.task.api.Task afterTask = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        assertThat(afterTask.getName()).isEqualTo("After task");

        taskService.complete(afterTask.getId());

        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(pi.getId()).singleResult()).isNull();
    }
}
