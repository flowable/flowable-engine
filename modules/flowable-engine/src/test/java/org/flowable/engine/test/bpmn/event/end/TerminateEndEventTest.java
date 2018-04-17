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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

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
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.delegate.event.AbstractFlowableEngineEventListener;
import org.flowable.engine.history.DeleteReason;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.history.HistoricTaskInstance;

/**
 * @author Nico Rehwaldt
 * @author Joram Barrez
 */
public class TerminateEndEventTest extends PluggableFlowableTestCase {

    public static int serviceTaskInvokedCount;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
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

    @Deployment
    public void testProcessTerminate() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

        long executionEntities = runtimeService.createExecutionQuery().processInstanceId(pi.getId()).count();
        assertEquals(3, executionEntities);

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
    
    @Deployment
    public void testTerminateExecutionListener() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preTerminateTask").singleResult();
        taskService.complete(task.getId());

        assertProcessEnded(pi.getId());
        
        assertEquals(1, TerminateExecutionListener.startCalled);
        assertEquals(1, TerminateExecutionListener.endCalled);
    }

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

    @Deployment
    public void testTerminateWithSubProcess() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

        // should terminate the process and
        long executionEntities = runtimeService.createExecutionQuery().processInstanceId(pi.getId()).count();
        assertEquals(4, executionEntities);

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

    @Deployment
    public void testTerminateWithSubProcess2() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

        // Completing the task -> terminal end event -> subprocess ends
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preNormalEnd").singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preTerminateEnd").singleResult();
        assertNotNull(task);
        taskService.complete(task.getId());

        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);

        assertHistoricProcessInstanceDeleteReason(pi, null);
        assertHistoricTasksDeleteReason(pi, null, "check before termination", "check before end");
        assertHistoricActivitiesDeleteReason(pi, null, "preNormalEnd", "preTerminateEnd");
        assertHistoricActivitiesDeleteReason(pi, DeleteReason.TERMINATE_END_EVENT, "SubProcess_1");
    }

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

    @Deployment(resources = {
            "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.testTerminateWithCallActivity.bpmn",
            "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.subProcessNoTerminate.bpmn"
    })
    public void testTerminateWithCallActivity() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(pi.getId()).singleResult();
        assertNotNull(subProcessInstance);

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

    @Deployment(resources = {
            "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.testTerminateWithCallActivityTerminateAll.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.subProcessNoTerminate.bpmn" })
    public void testTerminateWithCallActivityTerminateAll() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(pi.getId()).singleResult();
        assertNotNull(subProcessInstance);

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

    @Deployment(resources = {
            "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.testTerminateInExclusiveGatewayWithCallActivity.bpmn",
            "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.subProcessNoTerminate.bpmn"
    })
    public void testTerminateInExclusiveGatewayWithCallActivity() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample-terminateAfterExclusiveGateway");

        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(pi.getId()).singleResult();
        assertNotNull(subProcessInstance);

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preTerminateEnd").singleResult();
        Map<String, Object> variables = new HashMap<>();
        variables.put("input", 1);
        taskService.complete(task.getId(), variables);

        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);
    }

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

    @Deployment
    public void testTerminateInExclusiveGatewayWithMultiInstanceSubProcessTerminateAll() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample-terminateAfterExclusiveGateway");

        // Completing the task once should only destroy ONE multi instance
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("task").list();
        assertEquals(5, tasks.size());

        for (int i = 0; i < 5; i++) {
            taskService.complete(tasks.get(i).getId());
            assertTrue(runtimeService.createProcessInstanceQuery().processInstanceId(pi.getId()).count() > 0);
        }

        // Other task will now finish the process instance
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preTerminateEnd").singleResult();
        Map<String, Object> variables = new HashMap<>();
        variables.put("input", 1);
        taskService.complete(task.getId(), variables);

        assertEquals(0, runtimeService.createProcessInstanceQuery().processInstanceId(pi.getId()).count());
        assertHistoricProcessInstanceDetails(pi);
    }
    
    @Deployment
    public void testTerminateParallelGateway() throws Exception {
        final List<FlowableEvent> events = new ArrayList<>();
        processEngine.getRuntimeService().addEventListener(new AbstractFlowableEngineEventListener() {
            
            @Override
            public void onEvent(FlowableEvent event) {
                if (FlowableEngineEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT == event.getType() || FlowableEngineEventType.TASK_CREATED == event.getType()) {
                    events.add(event);
                }
                
                if (FlowableEngineEventType.ACTIVITY_CANCELLED == event.getType()) {
                    List<org.flowable.task.api.Task> list = Context.getProcessEngineConfiguration().getTaskService().createTaskQuery().list();
                    if (!list.isEmpty()) {
                        events.add(event);
                    }
                }
            }
            
            @Override
            public boolean isFailOnException() {
                return false;
            }
        });
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateParallel");
        
        HistoricTaskInstance historicTask = historyService.createHistoricTaskInstanceQuery().processInstanceId(pi.getId()).taskDefinitionKey("task").singleResult();
        assertNull(historicTask);
    }

    @Deployment
    public void testTerminateInSubProcess() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

        // should terminate the subprocess and continue the parent
        long executionEntities = runtimeService.createExecutionQuery().processInstanceId(pi.getId()).count();
        assertTrue(executionEntities > 0);

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preNormalEnd").singleResult();
        taskService.complete(task.getId());

        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);
    }

    @Deployment
    public void testTerminateInSubProcessTerminateAll() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");
        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);
    }

    @Deployment
    public void testTerminateInSubProcessWithBoundary() throws Exception {
        Date startTime = new Date();

        // Test terminating process via boundary timer

        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventWithBoundary");

        assertEquals(3, taskService.createTaskQuery().processInstanceId(pi.getId()).count());

        // Set clock time to '1 hour and 5 seconds' ahead to fire timer
        processEngineConfiguration.getClock().setCurrentTime(new Date(startTime.getTime() + ((60 * 60 * 1000) + 5000)));
        waitForJobExecutorToProcessAllJobs(5000L, 25L);

        // timer has fired
        assertEquals(0L, managementService.createJobQuery().count());

        assertProcessEnded(pi.getId());

        assertHistoricProcessInstanceDeleteReason(pi, DeleteReason.TERMINATE_END_EVENT);
        assertHistoricTasksDeleteReason(pi, DeleteReason.TERMINATE_END_EVENT, "check before normal end");
        assertHistoricActivitiesDeleteReason(pi, DeleteReason.TERMINATE_END_EVENT, "outerTask");

        // Test terminating subprocess

        pi = runtimeService.startProcessInstanceByKey("terminateEndEventWithBoundary");

        assertEquals(3, taskService.createTaskQuery().processInstanceId(pi.getId()).count());

        // a job for boundary event timer should exist
        assertEquals(1L, managementService.createTimerJobQuery().count());

        // Complete sub process task that leads to a terminate end event
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preTermInnerTask").singleResult();
        taskService.complete(task.getId());

        // 'preEndInnerTask' task in subprocess should have been terminated, only outerTask should exist
        assertEquals(1, taskService.createTaskQuery().processInstanceId(pi.getId()).count());

        // job for boundary event timer should have been removed
        assertEquals(0L, managementService.createTimerJobQuery().count());

        // complete outerTask
        task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("outerTask").singleResult();
        taskService.complete(task.getId());
        
        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);
    }

    @Deployment
    public void testTerminateInSubProcessWithBoundaryTerminateAll() throws Exception {
        // Test terminating subprocess

        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventWithBoundary");

        assertEquals(3, taskService.createTaskQuery().processInstanceId(pi.getId()).count());

        // Complete sub process task that leads to a terminate end event
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preTermInnerTask").singleResult();
        taskService.complete(task.getId());

        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);
    }

    @Deployment
    public void testTerminateInSubProcessConcurrent() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

        long executionEntities = runtimeService.createExecutionQuery().count();
        assertTrue(executionEntities > 0);

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preNormalEnd").singleResult();
        taskService.complete(task.getId());

        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);
    }

    @Deployment
    public void testTerminateInSubProcessConcurrentTerminateAll() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");
        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);
    }

    @Deployment
    public void testTerminateInSubProcessConcurrentTerminateAll2() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).list();
        assertEquals(2, tasks.size());

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskName("User Task").singleResult();
        taskService.complete(task.getId());

        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);
    }

    @Deployment
    public void testTerminateInSubProcessConcurrentMultiInstance() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).list();
        assertEquals(4, tasks.size()); // 3 user tasks in MI +1 (preNormalEnd) = 4 (2 were killed because it went directly to the terminate end event)

        long executionEntitiesCount = runtimeService.createExecutionQuery().count();
        assertEquals(9, executionEntitiesCount);

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preNormalEnd").singleResult();
        taskService.complete(task.getId());

        executionEntitiesCount = runtimeService.createExecutionQuery().count();
        assertEquals(8, executionEntitiesCount);

        tasks = taskService.createTaskQuery().list();
        for (org.flowable.task.api.Task t : tasks) {
            taskService.complete(t.getId());
        }

        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);
    }

    @Deployment
    public void testTerminateInSubProcessConcurrentMultiInstance2() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preNormalEnd").singleResult();
        taskService.complete(task.getId());

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).taskName("User Task").list();
        assertEquals(3, tasks.size());

        for (org.flowable.task.api.Task t : tasks) {
            taskService.complete(t.getId());
        }

        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);
    }

    @Deployment
    public void testTerminateInSubProcessConcurrentMultiInstanceTerminateAll() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");
        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);
    }

    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.testTerminateInCallActivityConcurrentCallActivity.bpmn",
            "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.testTerminateAfterUserTask.bpmn",
            "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testTerminateInCallActivityConcurrentCallActivity() throws Exception {
        // GIVEN - process instance starts and creates 2 subProcessInstances (with 2 user tasks - preTerminate and my task)
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventInCallActivityConcurrentCallActivity");
        assertThat(runtimeService.createProcessInstanceQuery().superProcessInstanceId(pi.getId()).list().size(), is(2));

        // WHEN - complete -> terminate end event
        org.flowable.task.api.Task preTerminate = taskService.createTaskQuery().taskName("preTerminate").singleResult();
        taskService.complete(preTerminate.getId());

        // THEN - super process is not finished together
        assertEquals(1, runtimeService.createProcessInstanceQuery().processInstanceId(pi.getId()).count());
    }

    @Deployment
    public void testTerminateInSubProcessMultiInstance() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

        long executionEntities = runtimeService.createExecutionQuery().count();
        assertTrue(executionEntities > 0);

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preNormalEnd").singleResult();
        taskService.complete(task.getId());

        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);
    }

    @Deployment
    public void testTerminateInSubProcessSequentialConcurrentMultiInstance() throws Exception {

        // Starting multi instance with 5 instances; terminating 2, finishing 3
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

        long remainingExecutions = runtimeService.createExecutionQuery().count();
        assertTrue(remainingExecutions > 0);

        // three finished
        assertEquals(3, serviceTaskInvokedCount2);

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preNormalEnd").singleResult();
        taskService.complete(task.getId());

        // last task remaining
        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);
    }

    @Deployment
    public void testTerminateInSubProcessSequentialConcurrentMultiInstanceTerminateAll() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");
        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);
    }

    @Deployment(resources = {
            "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.testTerminateInCallActivity.bpmn",
            "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.subProcessTerminate.bpmn"
    })
    public void testTerminateInCallActivity() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

        // should terminate the called process and continue the parent
        long executionEntities = runtimeService.createExecutionQuery().count();
        assertTrue(executionEntities > 0);

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preNormalEnd").singleResult();
        taskService.complete(task.getId());

        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);
    }

    @Deployment(resources = {
            "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.testTerminateInCallActivityMulitInstance.bpmn",
            "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.subProcessTerminate.bpmn"
    })
    public void testTerminateInCallActivityMultiInstance() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

        // should terminate the called process and continue the parent
        long executionEntities = runtimeService.createExecutionQuery().count();
        assertTrue(executionEntities > 0);

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preNormalEnd").singleResult();
        taskService.complete(task.getId());

        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);
    }

    @Deployment(resources = {
            "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.testTerminateInCallActivityMulitInstance.bpmn",
            "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.subProcessTerminateTerminateAll.bpmn20.xml" })
    public void testTerminateInCallActivityMultiInstanceTerminateAll() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");
        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);
    }

    @Deployment(resources = {
            "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.testTerminateInCallActivityConcurrent.bpmn",
            "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.subProcessConcurrentTerminate.bpmn"
    })
    public void testTerminateInCallActivityConcurrent() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

        // should terminate the called process and continue the parent
        long executionEntities = runtimeService.createExecutionQuery().count();
        assertTrue(executionEntities > 0);

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preNormalEnd").singleResult();
        taskService.complete(task.getId());

        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);
    }

    @Deployment
    public void testMiCallActivityParallel() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testMiCallActivity");

        List<org.flowable.task.api.Task> aTasks = taskService.createTaskQuery().taskName("A").list();
        assertEquals(5, aTasks.size());

        List<org.flowable.task.api.Task> bTasks = taskService.createTaskQuery().taskName("B").list();
        assertEquals(5, bTasks.size());

        // Completing B should terminate one instance (it goes to a terminate end event)
        int bTasksCompleted = 0;
        for (org.flowable.task.api.Task bTask : bTasks) {

            taskService.complete(bTask.getId());
            bTasksCompleted++;

            aTasks = taskService.createTaskQuery().taskName("A").list();
            assertEquals(5 - bTasksCompleted, aTasks.size());
        }

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("After call activity", task.getName());

        taskService.complete(task.getId());
        
        waitForHistoryJobExecutorToProcessAllJobs(5000, 100);
        
        assertProcessEnded(processInstance.getId());
        assertHistoricProcessInstanceDetails(processInstance);
    }

    @Deployment
    public void testMiCallActivitySequential() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testMiCallActivity");

        List<org.flowable.task.api.Task> aTasks = taskService.createTaskQuery().taskName("A").list();
        assertEquals(1, aTasks.size());

        List<org.flowable.task.api.Task> bTasks = taskService.createTaskQuery().taskName("B").list();
        assertEquals(1, bTasks.size());

        // Completing B should terminate one instance (it goes to a terminate end event)
        for (int i = 0; i < 9; i++) {

            org.flowable.task.api.Task bTask = taskService.createTaskQuery().taskName("B").singleResult();

            taskService.complete(bTask.getId());

            if (i != 8) {
                aTasks = taskService.createTaskQuery().taskName("A").list();
                assertEquals("Expected task for i=" + i, 1, aTasks.size());

                bTasks = taskService.createTaskQuery().taskName("B").list();
                assertEquals("Expected task for i=" + i, 1, bTasks.size());
            }
        }

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("After call activity", task.getName());

        taskService.complete(task.getId());
        
        assertProcessEnded(processInstance.getId());
        assertHistoricProcessInstanceDetails(processInstance);

    }

    @Deployment(resources = {
            "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.testTerminateInCallActivityConcurrent.bpmn",
            "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.subProcessConcurrentTerminateTerminateAll.bpmn20.xml"
    })
    public void testTerminateInCallActivityConcurrentTerminateAll() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");
        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);
    }

    @Deployment(resources = {
            "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.testTerminateInCallActivityConcurrentMulitInstance.bpmn",
            "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.subProcessConcurrentTerminate.bpmn"
    })
    public void testTerminateInCallActivityConcurrentMulitInstance() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

        // should terminate the called process and continue the parent
        long executionEntities = runtimeService.createExecutionQuery().count();
        assertTrue(executionEntities > 0);

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preNormalEnd").singleResult();
        taskService.complete(task.getId());

        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);
    }

    @Deployment(resources = {
            "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.testTerminateInCallActivityConcurrentMulitInstance.bpmn",
            "org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.subProcessConcurrentTerminateTerminateAll.bpmn20.xml" })
    public void testTerminateInCallActivityConcurrentMulitInstanceTerminateALl() throws Exception {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");
        assertProcessEnded(pi.getId());
        assertHistoricProcessInstanceDetails(pi);
    }

    @Deployment
    public void testTerminateNestedSubprocesses() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("TestTerminateNestedSubprocesses");

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).orderByTaskName().asc().list();
        assertEquals("A", tasks.get(0).getName());
        assertEquals("B", tasks.get(1).getName());
        assertEquals("D", tasks.get(2).getName());
        assertEquals("E", tasks.get(3).getName());
        assertEquals("F", tasks.get(4).getName());

        // Completing E should finish the lower subprocess and make 'H' active
        taskService.complete(tasks.get(3).getId());
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("H").singleResult();
        assertNotNull(task);

        // Completing A should make C active
        taskService.complete(tasks.get(0).getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("C").singleResult();
        assertNotNull(task);

        // Completing C should make I active
        taskService.complete(task.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("I").singleResult();
        assertNotNull(task);

        // Completing I and B should make G active
        taskService.complete(task.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("G").singleResult();
        assertNull(task);
        taskService.complete(tasks.get(1).getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("G").singleResult();
        assertNotNull(task);
    }

    @Deployment
    public void testTerminateNestedSubprocessesTerminateAll1() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("TestTerminateNestedSubprocesses");
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("E").singleResult();

        // Completing E leads to a terminate end event with terminate all set to true
        taskService.complete(task.getId());
        assertProcessEnded(processInstance.getId());
        assertHistoricProcessInstanceDetails(processInstance);
    }

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

    @Deployment
    public void testTerminateNestedMiSubprocesses() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("TestTerminateNestedMiSubprocesses");

        taskService.complete(taskService.createTaskQuery().taskName("A").singleResult().getId());

        // Should have 7 tasks C active
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("C").list();
        assertEquals(7, tasks.size());

        // Completing these should lead to task I being active
        for (org.flowable.task.api.Task task : tasks) {
            taskService.complete(task.getId());
        }

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("I").singleResult();
        assertNotNull(task);

        // Should have 3 instances of E active
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("E").list();
        assertEquals(3, tasks.size());

        // Completing these should make H active
        for (org.flowable.task.api.Task t : tasks) {
            taskService.complete(t.getId());
        }
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("H").singleResult();
        assertNotNull(task);
    }

    @Deployment
    public void testTerminateNestedMiSubprocessesSequential() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("TestTerminateNestedMiSubprocesses");

        taskService.complete(taskService.createTaskQuery().taskName("A").singleResult().getId());

        // Should have 7 tasks C active after each other
        for (int i = 0; i < 7; i++) {
            org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("C").singleResult();
            assertNotNull("Task was null for i = " + i, task);
            taskService.complete(task.getId());
        }

        // I should be active now
        assertNotNull(taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("I").singleResult());

        // Should have 3 instances of E active after each other
        for (int i = 0; i < 3; i++) {
            assertEquals(1, taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("D").count());
            assertEquals(1, taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("F").count());

            // Completing F should not finish the subprocess
            taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("F").singleResult().getId());

            org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("E").singleResult();
            taskService.complete(task.getId());
        }

        assertNotNull(taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("H").singleResult());
    }

    @Deployment
    public void testTerminateNestedMiSubprocessesTerminateAll1() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("TestTerminateNestedMiSubprocesses");
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("E").list().get(0);
        taskService.complete(task.getId());
        assertProcessEnded(processInstance.getId());
        assertHistoricProcessInstanceDetails(processInstance);
    }

    @Deployment
    public void testTerminateNestedMiSubprocessesTerminateAll2() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("TestTerminateNestedMiSubprocesses");
        taskService.complete(taskService.createTaskQuery().taskName("A").singleResult().getId());
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("C").list().get(0);
        taskService.complete(task.getId());
        assertProcessEnded(processInstance.getId());
        assertHistoricProcessInstanceDetails(processInstance);
    }

    @Deployment
    public void testTerminateNestedMiSubprocessesTerminateAll3() { // Same as 1, but sequential Multi-Instance
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("TestTerminateNestedMiSubprocesses");
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("E").list().get(0);
        taskService.complete(task.getId());
        assertProcessEnded(processInstance.getId());
        assertHistoricProcessInstanceDetails(processInstance);
    }

    @Deployment
    public void testTerminateNestedMiSubprocessesTerminateAll4() { // Same as 2, but sequential Multi-Instance
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("TestTerminateNestedMiSubprocesses");
        taskService.complete(taskService.createTaskQuery().taskName("A").singleResult().getId());
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("C").list().get(0);
        taskService.complete(task.getId());
        assertProcessEnded(processInstance.getId());
        assertHistoricProcessInstanceDetails(processInstance);
    }

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
        assertNull(task);
        taskService.complete(tasks.get(5).getId());

        // Multi instance call activity is sequential, so expecting 5 more times the same task
        for (int i = 0; i < 6; i++) {
            task = taskService.createTaskQuery().taskName("subprocess1_task").singleResult();
            assertNotNull("Task is null for index " + i, task);
            taskService.complete(task.getId());
        }

        tasks = assertTaskNames(processInstance,
                Arrays.asList("After A", "After C", "B", "B", "B", "B", "Before A", "Before A", "Before A", "Before B"));

    }

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
        assertNotNull(task);
        taskService.complete(task.getId());
        assertProcessEnded(processInstance.getId());
        assertHistoricProcessInstanceDetails(processInstance);

    }

    private List<org.flowable.task.api.Task> assertTaskNames(ProcessInstance processInstance, List<String> taskNames) {
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).orderByTaskName().asc().list();
        for (int i = 0; i < taskNames.size(); i++) {
            assertEquals("Task name at index " + i + " does not match", taskNames.get(i), tasks.get(i).getName());
        }
        return tasks;
    }

    public void testParseTerminateEndEventDefinitionWithExtensions() {
        org.flowable.engine.repository.Deployment deployment = repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/bpmn/event/end/TerminateEndEventTest.parseExtensionElements.bpmn20.xml").deploy();
        ProcessDefinition processDefinitionQuery = repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();
        BpmnModel bpmnModel = this.processEngineConfiguration.getProcessDefinitionCache()
                .get(processDefinitionQuery.getId()).getBpmnModel();

        Map<String, List<ExtensionElement>> extensionElements = bpmnModel.getProcesses().get(0)
                .findFlowElementsOfType(EndEvent.class).get(0).getExtensionElements();
        assertThat(extensionElements.size(), is(1));
        List<ExtensionElement> strangeProperties = extensionElements.get("strangeProperty");
        assertThat(strangeProperties.size(), is(1));
        ExtensionElement strangeProperty = strangeProperties.get(0);
        assertThat(strangeProperty.getNamespace(), is("http://activiti.org/bpmn"));
        assertThat(strangeProperty.getElementText(), is("value"));
        assertThat(strangeProperty.getAttributes().size(), is(1));
        ExtensionAttribute id = strangeProperty.getAttributes().get("id").get(0);
        assertThat(id.getName(), is("id"));
        assertThat(id.getValue(), is("strangeId"));

        repositoryService.deleteDeployment(deployment.getId());
    }

    // Unit test for ACT-4101 : NPE when there are multiple routes to terminateEndEvent, and both are reached
    @Deployment
    public void testThreeExecutionsArrivingInTerminateEndEvent() {
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("passed_QC", false);
        variableMap.put("has_bad_pixel_pattern", true);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("skybox_image_pull_request", variableMap);
        String processInstanceId = processInstance.getId();
        assertNotNull(processInstance);
        while (processInstance != null) {
            List<Execution> executionList = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
            String activityId = "";
            for (Execution execution : executionList) {
                activityId = execution.getActivityId();
                if (activityId == null
                        || activityId.equalsIgnoreCase("quality_control_passed_gateway")
                        || activityId.equalsIgnoreCase("parallelgateway1")
                        || activityId.equalsIgnoreCase("catch_bad_pixel_signal")
                        || activityId.equalsIgnoreCase("throw_bad_pixel_signal")
                        || activityId.equalsIgnoreCase("has_bad_pixel_pattern")
                        || activityId.equalsIgnoreCase("")) {
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

            assertNotNull(historicProcessInstance.getEndTime());
            assertNotNull(historicProcessInstance.getDurationInMillis());
            assertNotNull(historicProcessInstance.getEndActivityId());
        }
    }

    protected void assertHistoricProcessInstanceDeleteReason(ProcessInstance processInstance, String expectedDeleteReason) {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstance.getId()).singleResult();
            if (expectedDeleteReason == null) {
                assertNull(historicProcessInstance.getDeleteReason());
            } else {
                assertTrue(historicProcessInstance.getDeleteReason().startsWith(expectedDeleteReason));
            }
        }
    }

    @Override
    protected void assertHistoricTasksDeleteReason(ProcessInstance processInstance, String expectedDeleteReason, String... taskNames) {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            for (String taskName : taskNames) {
                List<HistoricTaskInstance> historicTaskInstances = historyService.createHistoricTaskInstanceQuery()
                        .processInstanceId(processInstance.getId()).taskName(taskName).list();
                assertTrue(historicTaskInstances.size() > 0);
                for (HistoricTaskInstance historicTaskInstance : historicTaskInstances) {
                    assertNotNull(historicTaskInstance.getEndTime());
                    if (expectedDeleteReason == null) {
                        assertNull(historicTaskInstance.getDeleteReason());
                    } else {
                        assertTrue(historicTaskInstance.getDeleteReason().startsWith(expectedDeleteReason));
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
                assertTrue(historicActiviyInstances.size() > 0);
                for (HistoricActivityInstance historicActiviyInstance : historicActiviyInstances) {
                    assertNotNull(historicActiviyInstance.getEndTime());
                    if (expectedDeleteReason == null) {
                        assertNull(historicActiviyInstance.getDeleteReason());
                    } else {
                        assertTrue(historicActiviyInstance.getDeleteReason().startsWith(expectedDeleteReason));
                    }
                }
            }
        }
    }

}
