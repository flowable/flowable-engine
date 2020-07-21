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
package org.flowable.engine.test.api.v6;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.delegate.event.AbstractFlowableEngineEventListener;
import org.flowable.engine.delegate.event.FlowableActivityCancelledEvent;
import org.flowable.engine.delegate.event.FlowableActivityEvent;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;

public class Flowable6ExecutionTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testOneTaskProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        List<Execution> executionList = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(executionList).hasSize(2);

        Execution rootProcessInstance = null;
        Execution childExecution = null;

        for (Execution execution : executionList) {
            if (execution.getId().equals(execution.getProcessInstanceId())) {
                rootProcessInstance = execution;

                assertThat(execution.getActivityId()).isNull();

            } else {
                childExecution = execution;

                assertThat(execution.getId()).isNotEqualTo(execution.getProcessInstanceId());
                assertThat(execution.getActivityId()).isEqualTo("theTask");
            }
        }

        assertThat(rootProcessInstance).isNotNull();
        assertThat(childExecution).isNotNull();

        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getExecutionId()).isEqualTo(childExecution.getId());

        taskService.complete(task.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            List<HistoricActivityInstance> historicActivities = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();
            assertThat(historicActivities)
                    .extracting(HistoricActivityInstance::getActivityId, HistoricActivityInstance::getExecutionId)
                    .containsExactlyInAnyOrder(
                            tuple("theStart", childExecution.getId()),
                            tuple("flow1", childExecution.getId()),
                            tuple("theTask", childExecution.getId()),
                            tuple("flow2", childExecution.getId()),
                            tuple("theEnd", childExecution.getId())
                    );
        }
    }

    @Test
    @Deployment
    public void testOneNestedTaskProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneNestedTaskProcess");

        List<Execution> executionList = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(executionList).hasSize(2);

        Execution rootProcessInstance = null;
        Execution childExecution = null;

        for (Execution execution : executionList) {
            if (execution.getId().equals(execution.getProcessInstanceId())) {
                rootProcessInstance = execution;

                assertThat(execution.getActivityId()).isNull();

            } else {
                childExecution = execution;

                assertThat(execution.getId()).isNotEqualTo(execution.getProcessInstanceId());
                assertThat(execution.getActivityId()).isEqualTo("theTask1");
            }
        }

        assertThat(rootProcessInstance).isNotNull();
        assertThat(childExecution).isNotNull();

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getExecutionId()).isEqualTo(childExecution.getId());

        taskService.complete(task.getId());

        executionList = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(executionList).hasSize(3);

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("subTask");
        Execution subTaskExecution = runtimeService.createExecutionQuery().executionId(task.getExecutionId()).singleResult();
        assertThat(subTaskExecution.getActivityId()).isEqualTo("subTask");

        Execution subProcessExecution = runtimeService.createExecutionQuery().executionId(subTaskExecution.getParentId()).singleResult();
        assertThat(subProcessExecution.getActivityId()).isEqualTo("runSubProcess");
        assertThat(subProcessExecution.getParentId()).isEqualTo(rootProcessInstance.getId());

        taskService.complete(task.getId());

        executionList = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(executionList).hasSize(2);

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(childExecution.getId()).isNotEqualTo(task.getExecutionId());

        Execution finalTaskExecution = runtimeService.createExecutionQuery().executionId(task.getExecutionId()).singleResult();
        assertThat(finalTaskExecution.getActivityId()).isEqualTo("theTask2");

        assertThat(finalTaskExecution.getParentId()).isEqualTo(rootProcessInstance.getId());

        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            List<HistoricActivityInstance> historicActivities = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();
            assertThat(historicActivities)
                    .extracting(HistoricActivityInstance::getActivityId, HistoricActivityInstance::getExecutionId)
                    .containsExactlyInAnyOrder(
                            tuple("theStart", childExecution.getId()),
                            tuple("flow1", childExecution.getId()),
                            tuple("theTask1", childExecution.getId()),
                            tuple("flow2", childExecution.getId()),
                            tuple("runSubProcess", subProcessExecution.getId()),
                            tuple("subStart", subTaskExecution.getId()),
                            tuple("subflow1", subTaskExecution.getId()),
                            tuple("subTask", subTaskExecution.getId()),
                            tuple("subflow2", subTaskExecution.getId()),
                            tuple("subEnd", subTaskExecution.getId()),
                            tuple("flow3", finalTaskExecution.getId()),
                            tuple("theTask2", finalTaskExecution.getId()),
                            tuple("flow4", finalTaskExecution.getId()),
                            tuple("theEnd", finalTaskExecution.getId())
                    );
        }
    }

    @Test
    @Deployment
    public void testSubProcessWithTimer() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("subProcessWithTimer");

        List<Execution> executionList = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(executionList).hasSize(2);

        Execution rootProcessInstance = null;
        Execution childExecution = null;

        for (Execution execution : executionList) {
            if (execution.getId().equals(execution.getProcessInstanceId())) {
                rootProcessInstance = execution;

                assertThat(execution.getActivityId()).isNull();

            } else {
                childExecution = execution;

                assertThat(execution.getId()).isNotEqualTo(execution.getProcessInstanceId());
                assertThat(execution.getActivityId()).isEqualTo("theTask1");
            }
        }

        assertThat(rootProcessInstance).isNotNull();
        assertThat(childExecution).isNotNull();

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getExecutionId()).isEqualTo(childExecution.getId());

        taskService.complete(task.getId());

        executionList = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(executionList).hasSize(4);

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("subTask");
        Execution subTaskExecution = runtimeService.createExecutionQuery().executionId(task.getExecutionId()).singleResult();
        assertThat(subTaskExecution.getActivityId()).isEqualTo("subTask");

        Execution subProcessExecution = runtimeService.createExecutionQuery().executionId(subTaskExecution.getParentId()).singleResult();
        assertThat(subProcessExecution.getActivityId()).isEqualTo("runSubProcess");
        assertThat(subProcessExecution.getParentId()).isEqualTo(rootProcessInstance.getId());
        
        Execution timerExecution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).activityId("timerEvent").singleResult();
        assertThat(timerExecution).isNotNull();
        
        taskService.complete(task.getId());

        executionList = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(executionList).hasSize(2);

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(childExecution.getId()).isNotEqualTo(task.getExecutionId());

        Execution finalTaskExecution = runtimeService.createExecutionQuery().executionId(task.getExecutionId()).singleResult();
        assertThat(finalTaskExecution.getActivityId()).isEqualTo("theTask2");

        assertThat(finalTaskExecution.getParentId()).isEqualTo(rootProcessInstance.getId());

        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            List<HistoricActivityInstance> historicActivities = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();
            assertThat(historicActivities)
                    .extracting(HistoricActivityInstance::getActivityId, HistoricActivityInstance::getExecutionId)
                    .containsExactlyInAnyOrder(
                            tuple("theStart", childExecution.getId()),
                            tuple("flow1", childExecution.getId()),
                            tuple("theTask1", childExecution.getId()),
                            tuple("flow2", childExecution.getId()),
                            tuple("runSubProcess", subProcessExecution.getId()),
                            tuple("subStart", subTaskExecution.getId()),
                            tuple("subflow1", subTaskExecution.getId()),
                            tuple("subTask", subTaskExecution.getId()),
                            tuple("subflow2", subTaskExecution.getId()),
                            tuple("subEnd", subTaskExecution.getId()),
                            tuple("timerEvent", timerExecution.getId()),
                            tuple("flow5", finalTaskExecution.getId()),
                            tuple("theTask2", finalTaskExecution.getId()),
                            tuple("flow6", finalTaskExecution.getId()),
                            tuple("theEnd", finalTaskExecution.getId())
                    );
            for (HistoricActivityInstance historicActivityInstance : historicActivities) {
                String activityId = historicActivityInstance.getActivityId();
                if (activityId.contains("flow")) {
                    assertThat(historicActivityInstance.getEndTime()).isEqualTo(historicActivityInstance.getStartTime());
                }
            }
        }
    }

    @Test
    @Deployment
    public void testSubProcessEvents() {
        SubProcessEventListener listener = new SubProcessEventListener();
        processEngineConfiguration.getEventDispatcher().addEventListener(listener);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("subProcessEvents");

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        Execution subProcessExecution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).activityId("subProcess").singleResult();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());

        // Verify Events
        List<FlowableEvent> events = listener.getEventsReceived();
        assertThat(events).hasSize(2);

        FlowableActivityEvent event = (FlowableActivityEvent) events.get(0);
        assertThat(event.getActivityType()).isEqualTo("subProcess");
        assertThat(event.getExecutionId()).isEqualTo(subProcessExecution.getId());

        event = (FlowableActivityEvent) events.get(1);
        assertThat(event.getActivityType()).isEqualTo("subProcess");
        assertThat(event.getExecutionId()).isEqualTo(subProcessExecution.getId());

        processEngineConfiguration.getEventDispatcher().removeEventListener(listener);
    }

    @Test
    @Deployment
    void testCurrentActivityNamePresentDuringExecution() {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("currentActivityNameProcess");

        Map<String, Object> processVariables = runtimeService.getVariables(processInstance.getId());
        assertThat(processVariables)
            .contains(
                entry("serviceTaskActivityName", "The famous task"),
                entry("scriptTaskActivityName", "Script Task name")
            );
    }

    public class SubProcessEventListener extends AbstractFlowableEngineEventListener {

        private List<FlowableEvent> eventsReceived;

        public SubProcessEventListener() {
            eventsReceived = new ArrayList<>();
        }

        public List<FlowableEvent> getEventsReceived() {
            return eventsReceived;
        }

        public void clearEventsReceived() {
            eventsReceived.clear();
        }

        @Override
        protected void activityStarted(FlowableActivityEvent event) {
            if ("subProcess".equals(event.getActivityType())) {
                eventsReceived.add(event);
            }
        }

        @Override
        protected void activityCancelled(FlowableActivityCancelledEvent event) {
            if ("subProcess".equals(event.getActivityType())) {
                eventsReceived.add(event);
            }
        }

        @Override
        protected void activityCompleted(FlowableActivityEvent event) {
            if ("subProcess".equals(event.getActivityType())) {
                eventsReceived.add(event);
            }
        }
    }
}
