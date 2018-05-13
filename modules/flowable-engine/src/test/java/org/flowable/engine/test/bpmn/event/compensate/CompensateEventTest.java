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

package org.flowable.engine.test.bpmn.event.compensate;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricActivityInstanceQuery;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.EnableVerboseExecutionTreeLogging;

/**
 * @author Tijs Rademakers
 */
@EnableVerboseExecutionTreeLogging
public class CompensateEventTest extends PluggableFlowableTestCase {

    @Deployment
    public void testCompensateSubprocess() {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensateProcess");

        assertEquals(5, runtimeService.getVariable(processInstance.getId(), "undoBookHotel"));

        Execution execution = runtimeService.createExecutionQuery().activityId("beforeEnd").singleResult();
        runtimeService.trigger(execution.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Deployment
    public void testCompensateSubprocessWithoutActivityRef() {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensateProcess");

        assertEquals(5, runtimeService.getVariable(processInstance.getId(), "undoBookHotel"));

        Execution execution = runtimeService.createExecutionQuery().activityId("beforeEnd").singleResult();
        runtimeService.trigger(execution.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Deployment
    public void testCompensateSubprocessWithUserTask() {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensateProcess");

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("Manually undo book hotel", task.getName());
        taskService.complete(task.getId());

        Execution execution = runtimeService.createExecutionQuery().activityId("beforeEnd").singleResult();
        runtimeService.trigger(execution.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Deployment
    public void testCompensateSubprocessWithUserTask2() {

        // Same process as testCompensateSubprocessWithUserTask, but now the end event is reached first
        // (giving an exception before)

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensateProcess");
        Execution execution = runtimeService.createExecutionQuery().activityId("beforeEnd").singleResult();
        runtimeService.trigger(execution.getId());

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("Manually undo book hotel", task.getName());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment
    public void testCompensateMiSubprocess() {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensateProcess");

        assertEquals(5, runtimeService.getVariable(processInstance.getId(), "undoBookHotel"));

        Execution execution = runtimeService.createExecutionQuery().activityId("beforeEnd").singleResult();
        runtimeService.trigger(execution.getId());
        assertProcessEnded(processInstance.getId());

    }

    @Deployment
    public void testCompensateScope() {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensateProcess");

        assertEquals(5, runtimeService.getVariable(processInstance.getId(), "undoBookHotel"));
        assertEquals(5, runtimeService.getVariable(processInstance.getId(), "undoBookFlight"));

        Execution execution = runtimeService.createExecutionQuery().activityId("beforeEnd").singleResult();
        runtimeService.trigger(execution.getId());
        assertProcessEnded(processInstance.getId());

    }

    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/compensate/CompensateEventTest.SubProcess1.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/compensate/CompensateEventTest.SubProcess2.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/compensate/CompensateEventTest.testCompensateTwoSubprocesses.bpmn20.xml" })
    public void testCompensateTwoSubprocesses() {

        Map<String, Object> initialVariables = new HashMap<>();
        initialVariables.put("test", "begin");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensateProcess", initialVariables);

        assertNotNull(processInstance);

        // get task from first subprocess
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();

        Map<String, String> taskFormVariables = new HashMap<>();
        taskFormVariables.put("test", "begin");
        formService.submitTaskFormData(task.getId(), taskFormVariables);

        // get task from second subprocess
        task = taskService.createTaskQuery().singleResult();

        formService.submitTaskFormData(task.getId(), new HashMap<>());

        // get first task from main process
        task = taskService.createTaskQuery().singleResult();

        Object testVariable2 = runtimeService.getVariable(processInstance.getId(), "test2");
        assertNotNull(testVariable2);
        assertEquals("compensated2", testVariable2.toString());

        Object testVariable1 = runtimeService.getVariable(processInstance.getId(), "test1");
        assertNotNull(testVariable1);
        assertEquals("compensated1", testVariable1.toString());
    }

    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/compensate/CompensateEventTest.testCallActivityCompensationHandler.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/compensate/CompensationHandler.bpmn20.xml" })
    public void testCallActivityCompensationHandler() {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensateProcess");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertEquals(5, historyService.createHistoricActivityInstanceQuery().activityId("undoBookHotel").count());
        }

        Execution execution = runtimeService.createExecutionQuery().activityId("beforeEnd").singleResult();
        runtimeService.trigger(execution.getId());
        assertProcessEnded(processInstance.getId());

        assertEquals(0, runtimeService.createProcessInstanceQuery().count());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertEquals(6, historyService.createHistoricProcessInstanceQuery().count());
        }

    }

    public void testMultipleCompensationCatchEventsFails() {
        try {
            repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/bpmn/event/compensate/CompensateEventTest.testMultipleCompensationCatchEventsFails.bpmn20.xml").deploy();
            fail("exception expected");
        } catch (Exception e) {
        }
    }

    public void testInvalidActivityRefFails() {
        try {
            repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/bpmn/event/compensate/CompensateEventTest.testInvalidActivityRefFails.bpmn20.xml").deploy();
            fail("exception expected");
        } catch (Exception e) {
            if (!e.getMessage().contains("Invalid attribute value for 'activityRef':")) {
                fail("different exception expected");
            }
        }
    }

    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/compensate/CompensateEventTest.testCompensationStepEndRecorded.bpmn20.xml" })
    public void testCompensationStepEndTimeRecorded() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensationStepEndRecordedProcess");
        assertProcessEnded(processInstance.getId());
        assertEquals(0, runtimeService.createProcessInstanceQuery().count());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            final HistoricActivityInstanceQuery query = historyService.createHistoricActivityInstanceQuery().activityId("compensationScriptTask");
            assertEquals(1, query.count());
            final HistoricActivityInstance compensationScriptTask = query.singleResult();
            assertNotNull(compensationScriptTask);
            assertNotNull(compensationScriptTask.getEndTime());
            assertNotNull(compensationScriptTask.getDurationInMillis());
        }
    }

    @Deployment
    public void testCompensateWithSubprocess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensateProcess");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstance.getId()).activityId("bookHotel").singleResult();
            assertNotNull(historicActivityInstance.getEndTime());
        }

        // Triggering the task will trigger the compensation subprocess
        org.flowable.task.api.Task afterBookHotelTask = taskService.createTaskQuery().processInstanceId(processInstance.getId())
                .taskDefinitionKey("afterBookHotel").singleResult();
        taskService.complete(afterBookHotelTask.getId());

        org.flowable.task.api.Task compensationTask1 = taskService.createTaskQuery().processInstanceId(processInstance.getId())
                .taskDefinitionKey("compensateTask1").singleResult();
        assertNotNull(compensationTask1);

        org.flowable.task.api.Task compensationTask2 = taskService.createTaskQuery().processInstanceId(processInstance.getId())
                .taskDefinitionKey("compensateTask2").singleResult();
        assertNotNull(compensationTask2);

        taskService.complete(compensationTask1.getId());
        taskService.complete(compensationTask2.getId());

        org.flowable.task.api.Task compensationTask3 = taskService.createTaskQuery().processInstanceId(processInstance.getId())
                .taskDefinitionKey("compensateTask3").singleResult();
        assertNotNull(compensationTask3);
        taskService.complete(compensationTask3.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/compensate/CompensateEventTest.testCompensateWithSubprocess.bpmn20.xml" })
    public void testCompensateWithSubprocess2() {

        // Same as testCompensateWithSubprocess, but without throwing the compensation event
        // As such, to verify that the extra compensation executions have no effect on the regular process execution

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensateProcess",
                CollectionUtil.singletonMap("doCompensation", false));

        org.flowable.task.api.Task afterBookHotelTask = taskService.createTaskQuery().processInstanceId(processInstance.getId())
                .taskDefinitionKey("afterBookHotel").singleResult();
        taskService.complete(afterBookHotelTask.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment
    public void testCompensateNestedSubprocess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensateProcess");

        // Completing should trigger the compensations
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("afterNestedSubProcess").singleResult();
        assertNotNull(task);
        taskService.complete(task.getId());

        org.flowable.task.api.Task compensationTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("undoBookHotel").singleResult();
        assertNotNull(compensationTask);
        taskService.complete(compensationTask.getId());

        assertProcessEnded(processInstance.getId());

    }

}
