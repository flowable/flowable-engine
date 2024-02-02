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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricActivityInstanceQuery;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.EnableVerboseExecutionTreeLogging;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntity;
import org.junit.jupiter.api.Test;

/**
 * @author Tijs Rademakers
 */
@EnableVerboseExecutionTreeLogging
public class CompensateEventTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testCompensateSubprocess() {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensateProcess");

        assertThat(runtimeService.getVariable(processInstance.getId(), "undoBookHotel")).isEqualTo(5);

        Execution execution = runtimeService.createExecutionQuery().activityId("beforeEnd").singleResult();
        runtimeService.trigger(execution.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment
    public void testCompensateServiceTask() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensateProcess");

        assertThat(runtimeService.getVariable(processInstance.getId(), "undoBookHotel")).isEqualTo(1);

        Execution execution = runtimeService.createExecutionQuery().activityId("beforeEnd").singleResult();
        runtimeService.trigger(execution.getId());
        assertProcessEnded(processInstance.getId());
    }
    
    @Test
    @Deployment
    public void testCompensateServiceTaskStartBackwardsCompatible() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensateProcess");

        assertThat(runtimeService.getVariable(processInstance.getId(), "undoBookHotel")).isEqualTo(1);

        Execution execution = runtimeService.createExecutionQuery().activityId("beforeEnd").singleResult();
        runtimeService.trigger(execution.getId());
        assertProcessEnded(processInstance.getId());
    }
    
    @Test
    @Deployment
    public void testCompensateServiceTaskBackwardsCompatible() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensateProcess");
        
        EventSubscriptionEntity eventSubscription = (EventSubscriptionEntity) runtimeService.createEventSubscriptionQuery()
                        .processInstanceId(processInstance.getId()).singleResult();
        eventSubscription.setActivityId("undoBookHotel");
        managementService.executeCommand(new Command<Void>() {

            @Override
            public Void execute(CommandContext commandContext) {
                processEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService()
                    .updateEventSubscription(eventSubscription);
                return null;
            }
        });
        
        Execution execution = runtimeService.createExecutionQuery().activityId("firstWait").singleResult();
        runtimeService.trigger(execution.getId());

        assertThat(runtimeService.getVariable(processInstance.getId(), "undoBookHotel")).isEqualTo(1);

        execution = runtimeService.createExecutionQuery().activityId("beforeEnd").singleResult();
        runtimeService.trigger(execution.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment
    public void testCompensateSubprocessWithoutActivityRef() {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensateProcess");

        assertThat(runtimeService.getVariable(processInstance.getId(), "undoBookHotel")).isEqualTo(5);

        Execution execution = runtimeService.createExecutionQuery().activityId("beforeEnd").singleResult();
        runtimeService.trigger(execution.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment
    public void testCompensateSubprocessWithUserTask() {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensateProcess");

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Manually undo book hotel");
        taskService.complete(task.getId());

        Execution execution = runtimeService.createExecutionQuery().activityId("beforeEnd").singleResult();
        runtimeService.trigger(execution.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment
    public void testCompensateSubprocessWithUserTask2() {

        // Same process as testCompensateSubprocessWithUserTask, but now the end event is reached first
        // (giving an exception before)

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensateProcess");
        Execution execution = runtimeService.createExecutionQuery().activityId("beforeEnd").singleResult();
        runtimeService.trigger(execution.getId());

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Manually undo book hotel");
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment
    public void testCompensateMiSubprocess() {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensateProcess");

        assertThat(runtimeService.getVariable(processInstance.getId(), "undoBookHotel")).isEqualTo(5);

        Execution execution = runtimeService.createExecutionQuery().activityId("beforeEnd").singleResult();
        runtimeService.trigger(execution.getId());
        assertProcessEnded(processInstance.getId());

    }

    @Test
    @Deployment
    public void testCompensateScope() {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensateProcess");

        assertThat(runtimeService.getVariable(processInstance.getId(), "undoBookHotel")).isEqualTo(5);
        assertThat(runtimeService.getVariable(processInstance.getId(), "undoBookFlight")).isEqualTo(5);

        Execution execution = runtimeService.createExecutionQuery().activityId("beforeEnd").singleResult();
        runtimeService.trigger(execution.getId());
        assertProcessEnded(processInstance.getId());

    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/compensate/CompensateEventTest.SubProcess1.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/compensate/CompensateEventTest.SubProcess2.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/compensate/CompensateEventTest.testCompensateTwoSubprocesses.bpmn20.xml" })
    public void testCompensateTwoSubprocesses() {

        Map<String, Object> initialVariables = new HashMap<>();
        initialVariables.put("test", "begin");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensateProcess", initialVariables);

        assertThat(processInstance).isNotNull();

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
        assertThat(testVariable2).hasToString("compensated2");

        Object testVariable1 = runtimeService.getVariable(processInstance.getId(), "test1");
        assertThat(testVariable1).hasToString("compensated1");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/compensate/CompensateEventTest.testCallActivityCompensationHandler.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/compensate/CompensationHandler.bpmn20.xml" })
    public void testCallActivityCompensationHandler() {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensateProcess");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertThat(historyService.createHistoricActivityInstanceQuery().activityId("undoBookHotel").count()).isEqualTo(5);
        }

        Execution execution = runtimeService.createExecutionQuery().activityId("beforeEnd").singleResult();
        runtimeService.trigger(execution.getId());
        assertProcessEnded(processInstance.getId());

        assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(6);
        }

    }

    @Test
    public void testMultipleCompensationCatchEventsFails() {
        assertThatThrownBy(() -> repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/bpmn/event/compensate/CompensateEventTest.testMultipleCompensationCatchEventsFails.bpmn20.xml").deploy())
                .isInstanceOf(Exception.class);
    }

    @Test
    public void testInvalidActivityRefFails() {
        assertThatThrownBy(() -> repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/bpmn/event/compensate/CompensateEventTest.testInvalidActivityRefFails.bpmn20.xml").deploy())
                .hasMessageContaining("Invalid attribute value for 'activityRef':")
                .isInstanceOf(Exception.class);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/compensate/CompensateEventTest.testCompensationStepEndRecorded.bpmn20.xml" })
    public void testCompensationStepEndTimeRecorded() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensationStepEndRecordedProcess");
        assertProcessEnded(processInstance.getId());
        assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            final HistoricActivityInstanceQuery query = historyService.createHistoricActivityInstanceQuery().activityId("compensationScriptTask");
            assertThat(query.count()).isEqualTo(1);
            final HistoricActivityInstance compensationScriptTask = query.singleResult();
            assertThat(compensationScriptTask).isNotNull();
            assertThat(compensationScriptTask.getEndTime()).isNotNull();
            assertThat(compensationScriptTask.getDurationInMillis()).isNotNull();
        }
    }

    @Test
    @Deployment
    public void testCompensateWithSubprocess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensateProcess");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstance.getId()).activityId("bookHotel").singleResult();
            assertThat(historicActivityInstance.getEndTime()).isNotNull();
        }

        // Triggering the task will trigger the compensation subprocess
        org.flowable.task.api.Task afterBookHotelTask = taskService.createTaskQuery().processInstanceId(processInstance.getId())
                .taskDefinitionKey("afterBookHotel").singleResult();
        taskService.complete(afterBookHotelTask.getId());

        org.flowable.task.api.Task compensationTask1 = taskService.createTaskQuery().processInstanceId(processInstance.getId())
                .taskDefinitionKey("compensateTask1").singleResult();
        assertThat(compensationTask1).isNotNull();

        org.flowable.task.api.Task compensationTask2 = taskService.createTaskQuery().processInstanceId(processInstance.getId())
                .taskDefinitionKey("compensateTask2").singleResult();
        assertThat(compensationTask2).isNotNull();

        taskService.complete(compensationTask1.getId());
        taskService.complete(compensationTask2.getId());

        org.flowable.task.api.Task compensationTask3 = taskService.createTaskQuery().processInstanceId(processInstance.getId())
                .taskDefinitionKey("compensateTask3").singleResult();
        assertThat(compensationTask3).isNotNull();
        taskService.complete(compensationTask3.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
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

    @Test
    @Deployment
    public void testCompensateNestedSubprocess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("compensateProcess");

        // Completing should trigger the compensations
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("afterNestedSubProcess").singleResult();
        assertThat(task).isNotNull();
        taskService.complete(task.getId());

        org.flowable.task.api.Task compensationTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("undoBookHotel").singleResult();
        assertThat(compensationTask).isNotNull();
        taskService.complete(compensationTask.getId());

        assertProcessEnded(processInstance.getId());

    }

}
