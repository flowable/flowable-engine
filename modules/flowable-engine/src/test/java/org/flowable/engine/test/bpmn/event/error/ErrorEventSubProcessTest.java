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
package org.flowable.engine.test.bpmn.event.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ActivityInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

/**
 * @author Tijs Rademakers
 * @author Filip Hrisafov
 */
public class ErrorEventSubProcessTest extends PluggableFlowableTestCase {
    
    private static final String STANDALONE_SUBPROCESS_FLAG_VARIABLE_NAME = "standalone";
    private static final String LOCAL_ERROR_FLAG_VARIABLE_NAME = "localError";
    private static final String PROCESS_KEY_UNDER_TEST = "helloWorldWithBothSubProcessTypes";

    @Test
    @Deployment
    // an event subprocesses takes precedence over a boundary event
    public void testEventSubprocessTakesPrecedence() {
        String procId = runtimeService.startProcessInstanceByKey("CatchErrorInEmbeddedSubProcess").getId();
        assertThatErrorHasBeenCaught(procId);
    }

    @Test
    @Deployment
    // an event subprocess with errorCode takes precedence over a catch-all handler
    public void testErrorCodeTakesPrecedence() {
        String procId = runtimeService.startProcessInstanceByKey("CatchErrorInEmbeddedSubProcess").getId();

        // The process will throw an error event, which is caught and escalated by a User org.flowable.task.service.Task
        assertThat(taskService.createTaskQuery().taskDefinitionKey("taskAfterErrorCatch2").count()).isEqualTo(1);
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("Escalated Task");

        // Completing the task will end the process instance
        taskService.complete(task.getId());
        assertProcessEnded(procId);

    }

    @Test
    @Deployment
    public void testCatchErrorInEmbeddedSubProcess() {
        String procId = runtimeService.startProcessInstanceByKey("CatchErrorInEmbeddedSubProcess").getId();
        assertThatErrorHasBeenCaught(procId);
    }

    @Test
    @Deployment
    public void testCatchErrorThrownByScriptTaskInEmbeddedSubProcess() {
        String procId = runtimeService.startProcessInstanceByKey("CatchErrorThrownByScriptTaskInEmbeddedSubProcess").getId();
        assertThatErrorHasBeenCaught(procId);
    }

    @Test
    @Deployment
    public void testCatchErrorThrownByScriptTaskInEmbeddedSubProcessWithErrorCode() {
        String procId = runtimeService.startProcessInstanceByKey("CatchErrorThrownByScriptTaskInEmbeddedSubProcessWithErrorCode").getId();
        assertThatErrorHasBeenCaught(procId);
    }

    @Test
    @Deployment
    public void testCatchErrorThrownByScriptTaskInTopLevelProcess() {
        String procId = runtimeService.startProcessInstanceByKey("CatchErrorThrownByScriptTaskInTopLevelProcess").getId();
        assertThatErrorHasBeenCaught(procId);
    }
    
    @Test
    @Deployment
    public void testMultipleCatchErrorInTopLevelProcess() {
        String procId = runtimeService.startProcessInstanceByKey("MultipleCatchErrorInTopLevelProcess").getId();
        Task task = taskService.createTaskQuery().processInstanceId(procId).singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("taskAfterErrorCatch2");
    }
    
    @Test
    @Deployment
    public void testMultipleCatchErrorInTopLevelProcessFirst() {
        String procId = runtimeService.startProcessInstanceByKey("MultipleCatchErrorInTopLevelProcess").getId();
        Task task = taskService.createTaskQuery().processInstanceId(procId).singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("taskAfterErrorCatch");
    }

    @Test
    @Deployment
    public void testCatchErrorThrownByScriptTaskInsideSubProcessInTopLevelProcess() {
        String procId = runtimeService.startProcessInstanceByKey("CatchErrorThrownByScriptTaskInsideSubProcessInTopLevelProcess").getId();
        assertThatErrorHasBeenCaught(procId);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/error/ErrorEventSubProcessTest.testThrowErrorInScriptTaskInsideCallActivitiCatchInTopLevelProcess.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/error/BoundaryErrorEventTest.testCatchErrorThrownByJavaDelegateOnCallActivity-child.bpmn20.xml" })
    public void testThrowErrorInScriptTaskInsideCallActivitiCatchInTopLevelProcess() {
        String procId = runtimeService.startProcessInstanceByKey("testThrowErrorInScriptTaskInsideCallActivitiCatchInTopLevelProcess").getId();
        assertThatErrorHasBeenCaught(procId);
    }
    
    @Test
    @Deployment(resources = {"org/flowable/engine/test/bpmn/event/error/ErrorEventSubProcessTest.testCatchMultipleRethrowParent.bpmn",
                    "org/flowable/engine/test/bpmn/event/error/ErrorEventSubProcessTest.testCatchMultipleRethrowSubProcess.bpmn"})
    public void testMultipleRethrowEvents() {
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put(LOCAL_ERROR_FLAG_VARIABLE_NAME, true);
        variableMap.put(STANDALONE_SUBPROCESS_FLAG_VARIABLE_NAME, true);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY_UNDER_TEST, variableMap);
        
        assertThat(processInstance.getId()).isNotNull();
    }

    @Test
    @Deployment
    public void testInterruptingSimpleActivities() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testSimpleErrorEventSubProcess");

        assertThat(runtimeService.createActivityInstanceQuery().list())
            .extracting(ActivityInstance::getActivityType, ActivityInstance::getActivityId)
            .containsExactlyInAnyOrder(
                tuple("startEvent", "start"),
                tuple("sequenceFlow", "flow1"),
                tuple("subProcess", "subProcess"),
                tuple("startEvent", "subProcessStart"),
                tuple("sequenceFlow", "subProcessFlow1"),
                tuple("endEvent", "subProcessEnd"),
                tuple("eventSubProcess", "errorEventSubProcess"),
                tuple("startEvent", "eventSubProcessStart"),
                tuple("sequenceFlow", "eventSubProcessFlow1"),
                tuple("userTask", "eventSubProcessTask1")
            );

        // Complete the user task in the event sub process
        Task eventSubProcessTask = taskService.createTaskQuery().singleResult();
        assertThat(eventSubProcessTask).isNotNull();
        taskService.complete(eventSubProcessTask.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertThat(historyService.createHistoricActivityInstanceQuery().list())
                .extracting(HistoricActivityInstance::getActivityType, HistoricActivityInstance::getActivityId)
                .containsExactlyInAnyOrder(
                    tuple("startEvent", "start"),
                    tuple("sequenceFlow", "flow1"),
                    tuple("subProcess", "subProcess"),
                    tuple("startEvent", "subProcessStart"),
                    tuple("sequenceFlow", "subProcessFlow1"),
                    tuple("endEvent", "subProcessEnd"),
                    tuple("eventSubProcess", "errorEventSubProcess"),
                    tuple("startEvent", "eventSubProcessStart"),
                    tuple("sequenceFlow", "eventSubProcessFlow1"),
                    tuple("userTask", "eventSubProcessTask1"),
                    tuple("sequenceFlow", "eventSubProcessFlow2"),
                    tuple("endEvent", "eventSubProcessEnd")
                );
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment
    public void testRetriggerEventSubProcessError() {
        runtimeService.startProcessInstanceByKey("retriggerEventSubProcess");

        Task task = taskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("taskAfterBoundary");

        assertThat(runtimeService.createActivityInstanceQuery().list())
                .extracting(ActivityInstance::getActivityType, ActivityInstance::getActivityId)
                .containsExactlyInAnyOrder(
                        tuple("startEvent", "start"),
                        tuple("sequenceFlow", "flow1"),
                        tuple("subProcess", "subProcess"),
                        tuple("startEvent", "subProcessStart"),
                        tuple("sequenceFlow", "subProcessFlow1"),
                        tuple("scriptTask", "scriptTask"),
                        tuple("eventSubProcess", "eventSubProcess"),
                        tuple("startEvent", "eventSubProcessStart"),
                        tuple("sequenceFlow", "eventFlow1"),
                        tuple("endEvent", "eventEnd"),
                        tuple("boundaryEvent", "subProcessErrorBoundary"),
                        tuple("sequenceFlow", "flow4"),
                        tuple("userTask", "taskAfterBoundary")
                );

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertThat(historyService.createHistoricActivityInstanceQuery().list())
                    .extracting(HistoricActivityInstance::getActivityType, HistoricActivityInstance::getActivityId)
                    .containsExactlyInAnyOrder(
                            tuple("startEvent", "start"),
                            tuple("sequenceFlow", "flow1"),
                            tuple("subProcess", "subProcess"),
                            tuple("startEvent", "subProcessStart"),
                            tuple("sequenceFlow", "subProcessFlow1"),
                            tuple("scriptTask", "scriptTask"),
                            tuple("eventSubProcess", "eventSubProcess"),
                            tuple("startEvent", "eventSubProcessStart"),
                            tuple("sequenceFlow", "eventFlow1"),
                            tuple("endEvent", "eventEnd"),
                            tuple("boundaryEvent", "subProcessErrorBoundary"),
                            tuple("sequenceFlow", "flow4"),
                            tuple("userTask", "taskAfterBoundary")
                    );
        }
    }

    private void assertThatErrorHasBeenCaught(String procId) {
        // The process will throw an error event,
        // which is caught and escalated by a User org.flowable.task.service.Task
        assertThat(taskService.createTaskQuery().count()).as("No tasks found in task list.").isEqualTo(1);
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("Escalated Task");

        // Completing the org.flowable.task.service.Task will end the process instance
        taskService.complete(task.getId());
        assertProcessEnded(procId);
    }

}
