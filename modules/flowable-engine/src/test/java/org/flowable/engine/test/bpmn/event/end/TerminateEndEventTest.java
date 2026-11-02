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
package org.flowable.engine.test.bpmn.event.end;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.ExtensionAttribute;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventType;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.delegate.event.AbstractFlowableEngineEventListener;
import org.flowable.engine.history.DeleteReason;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.impl.runtime.callback.ProcessInstanceState;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Nico Rehwaldt
 * @author Joram Barrez
 */
public class TerminateEndEventTest extends PluggableFlowableTestCase {

    public static int serviceTaskInvokedCount;

    @BeforeEach
    protected void setUp() throws Exception {
        serviceTaskInvokedCount = 0;
        serviceTaskInvokedCount2 = 0;
    }

    public static class CountDelegate implements JavaDelegate {

        @Override
        public void execute(DelegateExecution execution) {
            serviceTaskInvokedCount++;

            // leave only 3 out of n subprocesses
            execution.setVariableLocal("terminate", serviceTaskInvokedCount > 3);
        }
    }

    public static int serviceTaskInvokedCount2;

    public static class CountDelegate2 implements JavaDelegate {

        @Override
        public void execute(DelegateExecution execution) {
            serviceTaskInvokedCount2++;
        }
    }

    @Test
    @Deployment
    public void testProcessTerminate() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

        long executionEntities = runtimeService.createExecutionQuery().processInstanceId(pi.getId()).count();
        assertThat(executionEntities).isEqualTo(3);

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preTerminateTask").singleResult();
        taskService.complete(task.getId());

        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);

        assertHistoricProcessInstanceDeleteReason(pi, DeleteReason.TERMINATE_END_EVENT);
        assertHistoricProcessInstanceState(pi, ProcessInstanceState.COMPLETED);
        assertHistoricTasksDeleteReason(pi, null, "check before termination");
        assertHistoricTasksDeleteReason(pi, DeleteReason.TERMINATE_END_EVENT, "check before end");
        assertHistoricActivitiesDeleteReason(pi, DeleteReason.TERMINATE_END_EVENT, "preNormalTerminateTask");
        assertHistoricActivitiesDeleteReason(pi, null, "preTerminateTask");
    }
    
    @Test
    @Deployment
    public void testTerminateExecutionListener() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preTerminateTask").singleResult();
        taskService.complete(task.getId());

        assertProcessEnded(pi.getId());
        
        assertThat(TerminateExecutionListener.startCalled).isEqualTo(1);
        assertThat(TerminateExecutionListener.endCalled).isEqualTo(1);
    }

    @Test
    @Deployment
    public void testProcessTerminateAll() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preTerminateTask").singleResult();
        taskService.complete(task.getId());

        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);

        assertHistoricProcessInstanceDeleteReason(pi, DeleteReason.TERMINATE_END_EVENT);
        assertHistoricTasksDeleteReason(pi, null, "check before termination");
        assertHistoricTasksDeleteReason(pi, DeleteReason.TERMINATE_END_EVENT, "check before end");
        assertHistoricActivitiesDeleteReason(pi, DeleteReason.TERMINATE_END_EVENT, "preNormalTerminateTask");
        assertHistoricActivitiesDeleteReason(pi, null, "preTerminateTask");
    }

    @Test
    @Deployment
    public void testTerminateWithSubProcess() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

        // should terminate the process and
        long executionEntities = runtimeService.createExecutionQuery().processInstanceId(pi.getId()).count();
        assertThat(executionEntities).isEqualTo(4);

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preTerminateEnd").singleResult();
        taskService.complete(task.getId());

        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);

        assertHistoricProcessInstanceDeleteReason(pi, DeleteReason.TERMINATE_END_EVENT);
        assertHistoricTasksDeleteReason(pi, null, "check before termination");
        assertHistoricTasksDeleteReason(pi, DeleteReason.TERMINATE_END_EVENT, "check before end");
        assertHistoricActivitiesDeleteReason(pi, DeleteReason.TERMINATE_END_EVENT, "preNormalEnd");
        assertHistoricActivitiesDeleteReason(pi, null, "preTerminateEnd");
    }

    @Test
    @Deployment
    public void testTerminateWithSubProcess2() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

        // Completing the task -> terminal end event -> subprocess ends
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preNormalEnd").singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preTerminateEnd").singleResult();
        assertThat(task).isNotNull();
        taskService.complete(task.getId());

        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);

        assertHistoricProcessInstanceDeleteReason(pi, null);
        assertHistoricTasksDeleteReason(pi, null, "check before termination", "check before end");
        assertHistoricActivitiesDeleteReason(pi, null, "preNormalEnd", "preTerminateEnd");
        assertHistoricActivitiesDeleteReason(pi, DeleteReason.TERMINATE_END_EVENT, "SubProcess_1");
    }

    @Test
    @Deployment
    public void testTerminateWithSubProcessTerminateAll() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

        // Completing the task -> terminal end event -> all ends (terminate all)
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preNormalEnd").singleResult();
        taskService.complete(task.getId());

        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);

        assertHistoricProcessInstanceDeleteReason(pi, DeleteReason.TERMINATE_END_EVENT);
        assertHistoricTasksDeleteReason(pi, null, "check before end");
        assertHistoricTasksDeleteReason(pi, DeleteReason.TERMINATE_END_EVENT, "check before termination");
        assertHistoricActivitiesDeleteReason(pi, DeleteReason.TERMINATE_END_EVENT, "preTerminateEnd");
        assertHistoricActivitiesDeleteReason(pi, null, "preNormalEnd");
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.testTerminateWithCallActivity.bpmn",
            "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.subProcessNoTerminate.bpmn"
    })
    public void testTerminateWithCallActivity() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(pi.getId()).singleResult();
        assertThat(subProcessInstance).isNotNull();

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preTerminateEnd").singleResult();
        taskService.complete(task.getId());

        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);

        assertHistoricProcessInstanceDeleteReason(pi, DeleteReason.TERMINATE_END_EVENT);
        assertHistoricTasksDeleteReason(pi, null, "check before termination");
        assertHistoricTasksDeleteReason(subProcessInstance, DeleteReason.TERMINATE_END_EVENT, "Perform Sample");
        assertHistoricActivitiesDeleteReason(pi, null, "preTerminateEnd");
        assertHistoricActivitiesDeleteReason(subProcessInstance, DeleteReason.TERMINATE_END_EVENT, "task");
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.testTerminateWithCallActivityTerminateAll.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.subProcessNoTerminate.bpmn" })
    public void testTerminateWithCallActivityTerminateAll() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(pi.getId()).singleResult();
        assertThat(subProcessInstance).isNotNull();

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId())
                .taskDefinitionKey("preTerminateEnd").singleResult();
        taskService.complete(task.getId());

        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);

        assertHistoricProcessInstanceDeleteReason(pi, DeleteReason.TERMINATE_END_EVENT);
        assertHistoricTasksDeleteReason(pi, null, "check before termination");
        assertHistoricTasksDeleteReason(subProcessInstance, DeleteReason.TERMINATE_END_EVENT, "Perform Sample");
        assertHistoricActivitiesDeleteReason(pi, null, "preTerminateEnd");
        assertHistoricActivitiesDeleteReason(subProcessInstance, DeleteReason.TERMINATE_END_EVENT, "task");
    }
    
    @Test
    @Deployment
    public void testTerminateInEventSubProcess() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminate");

        Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("task").singleResult();
        assertThat(task).isNotNull();
        
        runtimeService.signalEventReceived("signal");

        assertProcessEnded(pi.getId());
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.testTerminateInExclusiveGatewayWithCallActivity.bpmn",
            "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.subProcessNoTerminate.bpmn"
    })
    public void testTerminateInExclusiveGatewayWithCallActivity() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample-terminateAfterExclusiveGateway");

        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(pi.getId()).singleResult();
        assertThat(subProcessInstance).isNotNull();

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preTerminateEnd").singleResult();
        Map<String, Object> variables = new HashMap<>();
        variables.put("input", 1);
        taskService.complete(task.getId(), variables);

        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);
    }

    @Test
    @Deployment
    public void testTerminateInExclusiveGatewayWithMultiInstanceSubProcess() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample-terminateAfterExclusiveGateway");

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preTerminateEnd").singleResult();
        Map<String, Object> variables = new HashMap<>();
        variables.put("input", 1);
        taskService.complete(task.getId(), variables);

        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);

        assertHistoricProcessInstanceDeleteReason(pi, DeleteReason.TERMINATE_END_EVENT);
        assertHistoricTasksDeleteReason(pi, null, "check before termination");
        assertHistoricTasksDeleteReason(pi, DeleteReason.TERMINATE_END_EVENT, "User Task");
        assertHistoricActivitiesDeleteReason(pi, null, "preTerminateEnd");
        assertHistoricActivitiesDeleteReason(pi, DeleteReason.TERMINATE_END_EVENT, "task");
    }

    @Test
    @Deployment
    public void testTerminateInExclusiveGatewayWithMultiInstanceSubProcessTerminateAll() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample-terminateAfterExclusiveGateway");

        // Completing the task once should only destroy ONE multi instance
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("task").list();
        assertThat(tasks).hasSize(5);

        for (int i = 0; i < 5; i++) {
            taskService.complete(tasks.get(i).getId());
            assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(pi.getId()).count()).isGreaterThan(0);
        }

        // Other task will now finish the process instance
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preTerminateEnd").singleResult();
        Map<String, Object> variables = new HashMap<>();
        variables.put("input", 1);
        taskService.complete(task.getId(), variables);

        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(pi.getId()).count()).isZero();
        assertHistoricProcessInstanceDetails(pi);
    }
    
    @Test
    @Deployment
    public void testTerminateParallelGateway() throws Exception {
        final List<FlowableEventType> events = new ArrayList<>();
        processEngine.getRuntimeService().addEventListener(new AbstractFlowableEngineEventListener() {
            
            @Override
            public void onEvent(FlowableEvent event) {
                if (FlowableEngineEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT == event.getType() || FlowableEngineEventType.TASK_CREATED == event.getType()) {
                    events.add(event.getType());
                }
                
                if (FlowableEngineEventType.ACTIVITY_CANCELLED == event.getType()) {
                    List<org.flowable.task.api.Task> list = Context.getProcessEngineConfiguration().getTaskService().createTaskQuery().list();
                    if (!list.isEmpty()) {
                        events.add(event.getType());
                    }
                }
            }
            
            @Override
            public boolean isFailOnException() {
                return false;
            }
        });
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateParallel");
        assertThat(events)
                .contains(FlowableEngineEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT);
        
        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);
    }

    @Test
    @Deployment
    public void testTerminateInSubProcess() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

        // should terminate the subprocess and continue the parent
        long executionEntities = runtimeService.createExecutionQuery().processInstanceId(pi.getId()).count();
        assertThat(executionEntities).isGreaterThan(0);

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preNormalEnd").singleResult();
        taskService.complete(task.getId());

        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);
    }

    @Test
    @Deployment
    public void testTerminateInSubProcessTerminateAll() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");
        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);
    }

    @Test
    @Deployment
    public void testTerminateInSubProcessWithBoundary() throws Exception {
        Date startTime = new Date();

        // Test terminating process via boundary timer

        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventWithBoundary");

        assertThat(taskService.createTaskQuery().processInstanceId(pi.getId()).count()).isEqualTo(3);

        // Set clock time to '1 hour and 5 seconds' ahead to fire timer
        processEngineConfiguration.getClock().setCurrentTime(new Date(startTime.getTime() + ((60 * 60 * 1000) + 5000)));
        waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(7000L, 25L);

        // timer has fired
        assertThat(managementService.createJobQuery().count()).isZero();

        assertProcessEnded(pi.getId());

        assertHistoricProcessInstanceDeleteReason(pi, DeleteReason.TERMINATE_END_EVENT);
        assertHistoricTasksDeleteReason(pi, DeleteReason.TERMINATE_END_EVENT, "check before normal end");
        assertHistoricActivitiesDeleteReason(pi, DeleteReason.TERMINATE_END_EVENT, "outerTask");

        // Test terminating subprocess

        pi = runtimeService.startProcessInstanceByKey("terminateEndEventWithBoundary");

        assertThat(taskService.createTaskQuery().processInstanceId(pi.getId()).count()).isEqualTo(3);

        // a job for boundary event timer should exist
        assertThat(managementService.createTimerJobQuery().count()).isEqualTo(1L);

        // Complete sub process task that leads to a terminate end event
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preTermInnerTask").singleResult();
        taskService.complete(task.getId());

        // 'preEndInnerTask' task in subprocess should have been terminated, only outerTask should exist
        assertThat(taskService.createTaskQuery().processInstanceId(pi.getId()).count()).isEqualTo(1);

        // job for boundary event timer should have been removed
        assertThat(managementService.createTimerJobQuery().count()).isZero();

        // complete outerTask
        task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("outerTask").singleResult();
        taskService.complete(task.getId());
        
        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);
    }

    @Test
    @Deployment
    public void testTerminateInSubProcessWithBoundaryTerminateAll() throws Exception {
        // Test terminating subprocess

        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventWithBoundary");

        assertThat(taskService.createTaskQuery().processInstanceId(pi.getId()).count()).isEqualTo(3);

        // Complete sub process task that leads to a terminate end event
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preTermInnerTask").singleResult();
        taskService.complete(task.getId());

        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);
    }

    @Test
    @Deployment
    public void testTerminateInSubProcessConcurrent() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

        long executionEntities = runtimeService.createExecutionQuery().count();
        assertThat(executionEntities).isGreaterThan(0);

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preNormalEnd").singleResult();
        taskService.complete(task.getId());

        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);
    }

    @Test
    @Deployment
    public void testTerminateInSubProcessConcurrentTerminateAll() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");
        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);
    }

    @Test
    @Deployment
    public void testTerminateInSubProcessConcurrentTerminateAll2() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).list();
        assertThat(tasks).hasSize(2);

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskName("User Task").singleResult();
        taskService.complete(task.getId());

        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);
    }

    @Test
    @Deployment
    public void testTerminateInSubProcessConcurrentMultiInstance() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).list();
        assertThat(tasks).hasSize(4); // 3 user tasks in MI +1 (preNormalEnd) = 4 (2 were killed because it went directly to the terminate end event)

        long executionEntitiesCount = runtimeService.createExecutionQuery().count();
        assertThat(executionEntitiesCount).isEqualTo(9);

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preNormalEnd").singleResult();
        taskService.complete(task.getId());

        executionEntitiesCount = runtimeService.createExecutionQuery().count();
        assertThat(executionEntitiesCount).isEqualTo(8);

        tasks = taskService.createTaskQuery().list();
        for (org.flowable.task.api.Task t : tasks) {
            taskService.complete(t.getId());
        }

        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);
    }

    @Test
    @Deployment
    public void testTerminateInSubProcessConcurrentMultiInstance2() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preNormalEnd").singleResult();
        taskService.complete(task.getId());

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).taskName("User Task").list();
        assertThat(tasks).hasSize(3);

        for (org.flowable.task.api.Task t : tasks) {
            taskService.complete(t.getId());
        }

        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);
    }

    @Test
    @Deployment
    public void testTerminateInSubProcessConcurrentMultiInstanceTerminateAll() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");
        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.testTerminateInCallActivityConcurrentCallActivity.bpmn",
            "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.testTerminateAfterUserTask.bpmn",
            "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testTerminateInCallActivityConcurrentCallActivity() throws Exception {
        // GIVEN - process instance starts and creates 2 subProcessInstances (with 2 user tasks - preTerminate and my task)
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventInCallActivityConcurrentCallActivity");
        assertThat(runtimeService.createProcessInstanceQuery().superProcessInstanceId(pi.getId()).list()).hasSize(2);

        // WHEN - complete -> terminate end event
        org.flowable.task.api.Task preTerminate = taskService.createTaskQuery().taskName("preTerminate").singleResult();
        taskService.complete(preTerminate.getId());

        // THEN - super process is not finished together
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(pi.getId()).count()).isEqualTo(1);
    }

    @Test
    @Deployment
    public void testTerminateInSubProcessMultiInstance() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

        long executionEntities = runtimeService.createExecutionQuery().count();
        assertThat(executionEntities).isGreaterThan(0);

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preNormalEnd").singleResult();
        taskService.complete(task.getId());

        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);
    }

    @Test
    @Deployment
    public void testTerminateInSubProcessSequentialConcurrentMultiInstance() throws Exception {

        // Starting multi instance with 5 instances; terminating 2, finishing 3
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

        long remainingExecutions = runtimeService.createExecutionQuery().count();
        assertThat(remainingExecutions).isGreaterThan(0);

        // three finished
        assertThat(serviceTaskInvokedCount2).isEqualTo(3);

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preNormalEnd").singleResult();
        taskService.complete(task.getId());

        // last task remaining
        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);
    }

    @Test
    @Deployment
    public void testTerminateInSubProcessSequentialConcurrentMultiInstanceTerminateAll() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");
        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.testTerminateInCallActivity.bpmn",
            "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.subProcessTerminate.bpmn"
    })
    public void testTerminateInCallActivity() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

        // should terminate the called process and continue the parent
        long executionEntities = runtimeService.createExecutionQuery().count();
        assertThat(executionEntities).isGreaterThan(0);

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preNormalEnd").singleResult();
        taskService.complete(task.getId());

        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.testTerminateInCallActivityMulitInstance.bpmn",
            "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.subProcessTerminate.bpmn"
    })
    public void testTerminateInCallActivityMultiInstance() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

        // should terminate the called process and continue the parent
        long executionEntities = runtimeService.createExecutionQuery().count();
        assertThat(executionEntities).isGreaterThan(0);

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preNormalEnd").singleResult();
        taskService.complete(task.getId());

        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.testTerminateInCallActivityMulitInstance.bpmn",
            "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.subProcessTerminateTerminateAll.bpmn20.xml" })
    public void testTerminateInCallActivityMultiInstanceTerminateAll() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");
        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.testTerminateInCallActivityConcurrent.bpmn",
            "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.subProcessConcurrentTerminate.bpmn"
    })
    public void testTerminateInCallActivityConcurrent() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

        // should terminate the called process and continue the parent
        long executionEntities = runtimeService.createExecutionQuery().count();
        assertThat(executionEntities).isGreaterThan(0);

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preNormalEnd").singleResult();
        taskService.complete(task.getId());

        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);
    }

    @Test
    @Deployment
    public void testMiCallActivityParallel() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testMiCallActivity");

        List<org.flowable.task.api.Task> aTasks = taskService.createTaskQuery().taskName("A").list();
        assertThat(aTasks).hasSize(5);

        List<org.flowable.task.api.Task> bTasks = taskService.createTaskQuery().taskName("B").list();
        assertThat(bTasks).hasSize(5);

        // Completing B should terminate one instance (it goes to a terminate end event)
        int bTasksCompleted = 0;
        for (org.flowable.task.api.Task bTask : bTasks) {

            taskService.complete(bTask.getId());
            bTasksCompleted++;

            aTasks = taskService.createTaskQuery().taskName("A").list();
            assertThat(aTasks).hasSize(5 - bTasksCompleted);
        }

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("After call activity");

        taskService.complete(task.getId());
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
        
        assertProcessEnded(processInstance.getId());
        assertHistoricProcessInstanceDetails(processInstance);
    }

    @Test
    @Deployment
    public void testMiCallActivitySequential() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testMiCallActivity");

        List<org.flowable.task.api.Task> aTasks = taskService.createTaskQuery().taskName("A").list();
        assertThat(aTasks).hasSize(1);

        List<org.flowable.task.api.Task> bTasks = taskService.createTaskQuery().taskName("B").list();
        assertThat(bTasks).hasSize(1);

        // Completing B should terminate one instance (it goes to a terminate end event)
        for (int i = 0; i < 9; i++) {

            org.flowable.task.api.Task bTask = taskService.createTaskQuery().taskName("B").singleResult();

            taskService.complete(bTask.getId());
            
            HistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(processEngineConfiguration, managementService, 20000, 200);

            if (i != 8) {
                aTasks = taskService.createTaskQuery().taskName("A").list();
                assertThat(aTasks).hasSize(1);

                bTasks = taskService.createTaskQuery().taskName("B").list();
                assertThat(bTasks).hasSize(1);
            }
        }

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("After call activity");

        taskService.complete(task.getId());
        
        assertProcessEnded(processInstance.getId());
        assertHistoricProcessInstanceDetails(processInstance);

    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.testTerminateInCallActivityConcurrent.bpmn",
            "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.subProcessConcurrentTerminateTerminateAll.bpmn20.xml"
    })
    public void testTerminateInCallActivityConcurrentTerminateAll() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");
        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.testTerminateInCallActivityConcurrentMulitInstance.bpmn",
            "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.subProcessConcurrentTerminate.bpmn"
    })
    public void testTerminateInCallActivityConcurrentMulitInstance() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

        // should terminate the called process and continue the parent
        long executionEntities = runtimeService.createExecutionQuery().count();
        assertThat(executionEntities).isGreaterThan(0);

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preNormalEnd").singleResult();
        taskService.complete(task.getId());

        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.testTerminateInCallActivityConcurrentMulitInstance.bpmn",
            "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.subProcessConcurrentTerminateTerminateAll.bpmn20.xml" })
    public void testTerminateInCallActivityConcurrentMulitInstanceTerminateALl() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");
        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);
    }

    @Test
    @Deployment
    public void testTerminateNestedSubprocesses() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("TestTerminateNestedSubprocesses");

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("A", "B", "D", "E", "F");

        // Completing E should finish the lower subprocess and make 'H' active
        taskService.complete(tasks.get(3).getId());
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("H").singleResult();
        assertThat(task).isNotNull();

        // Completing A should make C active
        taskService.complete(tasks.get(0).getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("C").singleResult();
        assertThat(task).isNotNull();

        // Completing C should make I active
        taskService.complete(task.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("I").singleResult();
        assertThat(task).isNotNull();

        // Completing I and B should make G active
        taskService.complete(task.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("G").singleResult();
        assertThat(task).isNull();
        taskService.complete(tasks.get(1).getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("G").singleResult();
        assertThat(task).isNotNull();
    }

    @Test
    @Deployment
    public void testTerminateNestedSubprocessesTerminateAll1() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("TestTerminateNestedSubprocesses");
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("E").singleResult();

        // Completing E leads to a terminate end event with terminate all set to true
        taskService.complete(task.getId());
        assertProcessEnded(processInstance.getId());
        assertHistoricProcessInstanceDetails(processInstance);
    }

    @Test
    @Deployment
    public void testTerminateNestedSubprocessesTerminateAll2() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("TestTerminateNestedSubprocesses");
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("A").singleResult();

        // Completing A and C leads to a terminate end event with terminate all set to true
        taskService.complete(task.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("C").singleResult();
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment
    public void testTerminateNestedMiSubprocesses() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("TestTerminateNestedMiSubprocesses");

        taskService.complete(taskService.createTaskQuery().taskName("A").singleResult().getId());

        // Should have 7 tasks C active
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("C").list();
        assertThat(tasks).hasSize(7);

        // Completing these should lead to task I being active
        for (org.flowable.task.api.Task task : tasks) {
            taskService.complete(task.getId());
        }

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("I").singleResult();
        assertThat(task).isNotNull();

        // Should have 3 instances of E active
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("E").list();
        assertThat(tasks).hasSize(3);

        // Completing these should make H active
        for (org.flowable.task.api.Task t : tasks) {
            taskService.complete(t.getId());
        }
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("H").singleResult();
        assertThat(task).isNotNull();
    }

    @Test
    @Deployment
    public void testTerminateNestedMiSubprocessesSequential() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("TestTerminateNestedMiSubprocesses");

        taskService.complete(taskService.createTaskQuery().taskName("A").singleResult().getId());

        // Should have 7 tasks C active after each other
        for (int i = 0; i < 7; i++) {
            org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("C").singleResult();
            assertThat(task).isNotNull();
            taskService.complete(task.getId());
        }

        // I should be active now
        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("I").singleResult()).isNotNull();

        // Should have 3 instances of E active after each other
        for (int i = 0; i < 3; i++) {
            assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("D").count()).isEqualTo(1);
            assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("F").count()).isEqualTo(1);

            // Completing F should not finish the subprocess
            taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("F").singleResult().getId());

            org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("E").singleResult();
            taskService.complete(task.getId());
        }

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("H").singleResult()).isNotNull();
    }

    @Test
    @Deployment
    public void testTerminateNestedMiSubprocessesTerminateAll1() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("TestTerminateNestedMiSubprocesses");
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("E").list().get(0);
        taskService.complete(task.getId());
        assertProcessEnded(processInstance.getId());
        assertHistoricProcessInstanceDetails(processInstance);
    }

    @Test
    @Deployment
    public void testTerminateNestedMiSubprocessesTerminateAll2() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("TestTerminateNestedMiSubprocesses");
        taskService.complete(taskService.createTaskQuery().taskName("A").singleResult().getId());
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("C").list().get(0);
        taskService.complete(task.getId());
        assertProcessEnded(processInstance.getId());
        assertHistoricProcessInstanceDetails(processInstance);
    }

    @Test
    @Deployment
    public void testTerminateNestedMiSubprocessesTerminateAll3() { // Same as 1, but sequential Multi-Instance
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("TestTerminateNestedMiSubprocesses");
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("E").list().get(0);
        taskService.complete(task.getId());
        assertProcessEnded(processInstance.getId());
        assertHistoricProcessInstanceDetails(processInstance);
    }

    @Test
    @Deployment
    public void testTerminateNestedMiSubprocessesTerminateAll4() { // Same as 2, but sequential Multi-Instance
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("TestTerminateNestedMiSubprocesses");
        taskService.complete(taskService.createTaskQuery().taskName("A").singleResult().getId());
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("C").list().get(0);
        taskService.complete(task.getId());
        assertProcessEnded(processInstance.getId());
        assertHistoricProcessInstanceDetails(processInstance);
    }

    @Test
    @Deployment
    public void testNestedCallActivities() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("TestNestedCallActivities");

        // Verify the tasks
        List<org.flowable.task.api.Task> tasks = assertTaskNames(processInstance,
                Arrays.asList("B", "B", "B", "B", "Before A", "Before A", "Before A", "Before A", "Before B", "Before C"));

        // Completing 'before c'
        taskService.complete(tasks.get(9).getId());
        tasks = assertTaskNames(processInstance,
                Arrays.asList("After C", "B", "B", "B", "B", "Before A", "Before A", "Before A", "Before A", "Before B"));

        // Completing 'before A' of one instance
        org.flowable.task.api.Task task = taskService.createTaskQuery().taskName("task_subprocess_1").singleResult();
        assertThat(task).isNull();
        taskService.complete(tasks.get(5).getId());

        // Multi instance call activity is sequential, so expecting 5 more times the same task
        for (int i = 0; i < 6; i++) {
            task = taskService.createTaskQuery().taskName("subprocess1_task").singleResult();
            assertThat(task).isNotNull();
            taskService.complete(task.getId());
        }

        tasks = assertTaskNames(processInstance,
                Arrays.asList("After A", "After C", "B", "B", "B", "B", "Before A", "Before A", "Before A", "Before B"));

    }

    @Test
    @Deployment
    public void testNestedCallActivitiesTerminateAll() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("TestNestedCallActivities");

        // Verify the tasks
        List<org.flowable.task.api.Task> tasks = assertTaskNames(processInstance,
                Arrays.asList("B", "B", "B", "B", "Before A", "Before A", "Before A", "Before A", "Before B", "Before C"));

        // Completing 'Before B' should lead to process instance termination
        taskService.complete(tasks.get(8).getId());
        assertProcessEnded(processInstance.getId());
        assertHistoricProcessInstanceDetails(processInstance);

        // Completing 'Before C' too
        processInstance = runtimeService.startProcessInstanceByKey("TestNestedCallActivities");
        tasks = assertTaskNames(processInstance,
                Arrays.asList("B", "B", "B", "B", "Before A", "Before A", "Before A", "Before A", "Before B", "Before C"));
        taskService.complete(tasks.get(9).getId());
        assertProcessEnded(processInstance.getId());
        assertHistoricProcessInstanceDetails(processInstance);

        // Now the tricky one. 'Before A' leads to 'callActivity A', which calls subprocess02 which terminates
        processInstance = runtimeService.startProcessInstanceByKey("TestNestedCallActivities");
        tasks = assertTaskNames(processInstance,
                Arrays.asList("B", "B", "B", "B", "Before A", "Before A", "Before A", "Before A", "Before B", "Before C"));
        taskService.complete(tasks.get(5).getId());
        org.flowable.task.api.Task task = taskService.createTaskQuery().taskName("subprocess1_task").singleResult();
        assertThat(task).isNotNull();
        taskService.complete(task.getId());
        assertProcessEnded(processInstance.getId());
        assertHistoricProcessInstanceDetails(processInstance);

    }

    private List<org.flowable.task.api.Task> assertTaskNames(ProcessInstance processInstance, List<String> taskNames) {
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).orderByTaskName().asc().list();
        for (int i = 0; i < taskNames.size(); i++) {
            assertThat(tasks.get(i).getName()).as("Task name at index " + i + " does not match").isEqualTo(taskNames.get(i));
        }
        return tasks;
    }

    @Test
    public void testParseTerminateEndEventDefinitionWithExtensions() {
        org.flowable.engine.repository.Deployment deployment = repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.parseExtensionElements.bpmn20.xml").deploy();
        ProcessDefinition processDefinitionQuery = repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();
        BpmnModel bpmnModel = this.processEngineConfiguration.getProcessDefinitionCache()
                .get(processDefinitionQuery.getId()).getBpmnModel();

        Map<String, List<ExtensionElement>> extensionElements = bpmnModel.getProcesses().get(0)
                .findFlowElementsOfType(EndEvent.class).get(0).getExtensionElements();
        assertThat(extensionElements).hasSize(1);
        List<ExtensionElement> strangeProperties = extensionElements.get("strangeProperty");
        assertThat(strangeProperties).hasSize(1);
        ExtensionElement strangeProperty = strangeProperties.get(0);
        assertThat(strangeProperty.getNamespace()).isEqualTo("http://activiti.org/bpmn");
        assertThat(strangeProperty.getElementText()).isEqualTo("value");
        assertThat(strangeProperty.getAttributes()).hasSize(1);
        ExtensionAttribute id = strangeProperty.getAttributes().get("id").get(0);
        assertThat(id.getName()).isEqualTo("id");
        assertThat(id.getValue()).isEqualTo("strangeId");

        repositoryService.deleteDeployment(deployment.getId());
    }

    // Unit test for ACT-4101 : NPE when there are multiple routes to terminateEndEvent, and both are reached
    @Test
    @Deployment
    public void testThreeExecutionsArrivingInTerminateEndEvent() {
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("passed_QC", false);
        variableMap.put("has_bad_pixel_pattern", true);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("skybox_image_pull_request", variableMap);
        String processInstanceId = processInstance.getId();
        assertThat(processInstance).isNotNull();
        while (processInstance != null) {
            List<Execution> executionList = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
            String activityId = "";
            for (Execution execution : executionList) {
                activityId = execution.getActivityId();
                if (activityId == null
                        || "quality_control_passed_gateway".equalsIgnoreCase(activityId)
                        || "parallelgateway1".equalsIgnoreCase(activityId)
                        || "catch_bad_pixel_signal".equalsIgnoreCase(activityId)
                        || "throw_bad_pixel_signal".equalsIgnoreCase(activityId)
                        || "has_bad_pixel_pattern".equalsIgnoreCase(activityId)
                        || "".equalsIgnoreCase(activityId)) {
                    continue;
                }
                runtimeService.trigger(execution.getId());
            }
            processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
        }

        assertProcessEnded(processInstanceId);
        assertHistoricProcessInstanceDetails(processInstanceId);
    }

    protected void assertHistoricProcessInstanceDetails(ProcessInstance pi) {
        assertHistoricProcessInstanceDetails(pi.getId());
    }

    protected void assertHistoricProcessInstanceDetails(String processInstanceId) {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstanceId).singleResult();

            assertThat(historicProcessInstance.getEndTime()).isNotNull();
            assertThat(historicProcessInstance.getDurationInMillis()).isNotNull();
            assertThat(historicProcessInstance.getEndActivityId()).isNotNull();
        }
    }

    protected void assertHistoricProcessInstanceDeleteReason(ProcessInstance processInstance, String expectedDeleteReason) {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstance.getId()).singleResult();
            if (expectedDeleteReason == null) {
                assertThat(historicProcessInstance.getDeleteReason()).isNull();
            } else {
                assertThat(historicProcessInstance.getDeleteReason()).startsWith(expectedDeleteReason);
            }
        }
    }

    protected void assertHistoricProcessInstanceState(ProcessInstance processInstance, String expectedState) {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstance.getId()).singleResult();
            if (expectedState == null) {
                assertThat(historicProcessInstance.getState()).isNull();
            } else {
                assertThat(historicProcessInstance.getState()).startsWith(expectedState);
            }
        }
    }

    @Override
    protected void assertHistoricTasksDeleteReason(ProcessInstance processInstance, String expectedDeleteReason, String... taskNames) {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            for (String taskName : taskNames) {
                List<HistoricTaskInstance> historicTaskInstances = historyService.createHistoricTaskInstanceQuery()
                        .processInstanceId(processInstance.getId()).taskName(taskName).list();
                assertThat(historicTaskInstances).isNotEmpty();
                for (HistoricTaskInstance historicTaskInstance : historicTaskInstances) {
                    assertThat(historicTaskInstance.getEndTime()).isNotNull();
                    if (expectedDeleteReason == null) {
                        assertThat(historicTaskInstance.getDeleteReason()).isNull();
                    } else {
                        assertThat(historicTaskInstance.getDeleteReason()).startsWith(expectedDeleteReason);
                    }
                }
            }
        }
    }

    @Override
    protected void assertHistoricActivitiesDeleteReason(ProcessInstance processInstance, String expectedDeleteReason, String... activityIds) {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            for (String activityId : activityIds) {
                List<HistoricActivityInstance> historicActiviyInstances = historyService.createHistoricActivityInstanceQuery()
                        .activityId(activityId).processInstanceId(processInstance.getId()).list();
                assertThat(historicActiviyInstances).isNotEmpty();
                for (HistoricActivityInstance historicActiviyInstance : historicActiviyInstances) {
                    assertThat(historicActiviyInstance.getEndTime()).isNotNull();
                    if (expectedDeleteReason == null) {
                        assertThat(historicActiviyInstance.getDeleteReason()).isNull();
                    } else {
                        assertThat(historicActiviyInstance.getDeleteReason()).startsWith(expectedDeleteReason);
                    }
                }
            }
        }
    }

}
