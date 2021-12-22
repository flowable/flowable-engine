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

package org.flowable.engine.test.api.runtime.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.assertj.core.api.Condition;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.migration.ActivityMigrationMapping;
import org.flowable.engine.migration.ProcessInstanceMigrationBuilder;
import org.flowable.engine.migration.ProcessInstanceMigrationValidationResult;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author Dennis Federico
 */
public class ProcessInstanceMigrationCallActivityTest extends PluggableFlowableTestCase {

    @AfterEach
    protected void tearDown() {
        deleteDeployments();
    }

    @Test
    public void testValidationAutoMapOfMissingCallActivityInNewModel() {
        ProcessDefinition procDefWithCallActivity = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/two-tasks-with-call-activity.bpmn20.xml");
        ProcessDefinition procDefCallActivity = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml");
        ProcessDefinition procDefSimpleOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");

        //Start the processInstance and confirm the migration state to validate
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefWithCallActivity.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey)
                .isEqualTo("firstTask");
        assertThat(task)
                .extracting(Task::getProcessDefinitionId)
                .isEqualTo(procDefWithCallActivity.getId());
        completeTask(task);

        List<Execution> processExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(processExecutions)
                .extracting(Execution::getActivityId)
                .containsExactly("callActivity");
        assertThat(processExecutions)
                .extracting("processDefinitionId")
                .containsOnly(procDefWithCallActivity.getId());
        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
        List<Execution> subProcessExecutions = runtimeService.createExecutionQuery().processInstanceId(subProcessInstance.getId()).onlyChildExecutions().list();
        assertThat(subProcessExecutions)
                .extracting(Execution::getActivityId)
                .containsExactly("theTask");
        assertThat(subProcessExecutions)
                .extracting("processDefinitionId")
                .containsOnly(procDefCallActivity.getId());
        List<Task> subProcessTasks = taskService.createTaskQuery().processInstanceId(subProcessInstance.getId()).list();
        assertThat(subProcessTasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("theTask");
        assertThat(subProcessTasks)
                .extracting(Task::getProcessDefinitionId)
                .containsOnly(procDefCallActivity.getId());

        //Prepare migration and validate
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefSimpleOneTask.getId());
        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isFalse();
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEqualTo(Collections.singletonList(
                "Call activity 'callActivity' does not exist in the new model. It must be mapped explicitly for migration (or all its child activities)"));

        assertThatThrownBy(() -> processInstanceMigrationBuilder.migrate(processInstance.getId()))
                .as("Migration should not be possible")
                .isInstanceOf(FlowableException.class)
                .hasMessageContaining("Call activity 'callActivity' does not exist in the new model. It must be mapped explicitly for migration (or all its child activities)");

        completeProcessInstanceTasks(subProcessInstance.getId());
        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testValidationAutoMapOfCallActivityToDifferentActivityType() {
        ProcessDefinition procDefWithCallActivity = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/two-tasks-with-call-activity.bpmn20.xml");
        ProcessDefinition procDefSubProcess = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml");
        ProcessDefinition procDefWithoutCallActivity = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-process-named-call-activity.bpmn20.xml");

        //Start the processInstance and confirm the migration state to validate
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefWithCallActivity.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey)
                .isEqualTo("firstTask");
        completeTask(task);

        List<Execution> processExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(processExecutions)
                .extracting(Execution::getActivityId)
                .containsExactly("callActivity");
        assertThat(processExecutions)
                .extracting("processDefinitionId")
                .containsOnly(procDefWithCallActivity.getId());

        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
        List<Execution> subProcessExecutions = runtimeService.createExecutionQuery().processInstanceId(subProcessInstance.getId()).onlyChildExecutions().list();
        assertThat(subProcessExecutions)
                .extracting(Execution::getActivityId)
                .containsExactly("theTask");
        assertThat(subProcessExecutions)
                .extracting("processDefinitionId")
                .containsOnly(procDefSubProcess.getId());
        List<Task> subProcessTasks = taskService.createTaskQuery().processInstanceId(subProcessInstance.getId()).list();
        assertThat(subProcessTasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly(tuple("theTask", procDefSubProcess.getId()));

        //Prepare migration and validate
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefWithoutCallActivity.getId());
        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isFalse();
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEqualTo(Collections.singletonList(
                "Call activity 'callActivity' is not a Call Activity in the new model. It must be mapped explicitly for migration (or all its child activities)"));

        assertThatThrownBy(() -> processInstanceMigrationBuilder.migrate(processInstance.getId()))
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessage(
                        "Call activity 'callActivity' is not a Call Activity in the new model. It must be mapped explicitly for migration (or all its child activities)");

        completeProcessInstanceTasks(subProcessInstance.getId());
        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testValidationOfIncompleteMappingOfCallActivitySubProcessActivities() {
        ProcessDefinition procDefWithCallActivityV1 = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/two-tasks-with-call-activity-v3.bpmn20.xml");
        ProcessDefinition procDefSubProcessV1 = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/parallel-gateway-two-splits-four-tasks.bpmn20.xml");
        ProcessDefinition procDefWithCallActivityV2 = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/two-tasks-with-call-activity.bpmn20.xml");
        deployProcessDefinition("my deploy", "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml");

        //Start the processInstance and confirm the migration state to validate
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefWithCallActivityV1.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey)
                .isEqualTo("firstTask");
        completeTask(task);

        List<Execution> processExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(processExecutions)
                .extracting(Execution::getActivityId)
                .containsExactly("callActivity");
        assertThat(processExecutions)
                .extracting("processDefinitionId")
                .containsOnly(procDefWithCallActivityV1.getId());

        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
        task = taskService.createTaskQuery().processInstanceId(subProcessInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey)
                .isEqualTo("taskBefore");
        completeTask(task);
        List<Execution> subProcessExecutions = runtimeService.createExecutionQuery().processInstanceId(subProcessInstance.getId()).onlyChildExecutions().list();
        assertThat(subProcessExecutions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("oddFlowTask1", "evenFlowTask2");
        assertThat(subProcessExecutions)
                .extracting("processDefinitionId")
                .containsOnly(procDefSubProcessV1.getId());
        List<Task> subProcessTasks = taskService.createTaskQuery().processInstanceId(subProcessInstance.getId()).list();
        assertThat(subProcessTasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("oddFlowTask1", "evenFlowTask2");
        assertThat(subProcessTasks)
                .extracting(Task::getProcessDefinitionId)
                .containsOnly(procDefSubProcessV1.getId());

        //Prepare migration and validate
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefWithCallActivityV2.getId())
                .addActivityMigrationMapping(
                        ActivityMigrationMapping.createMappingFor("evenFlowTask2", "anyTask").inParentProcessOfCallActivityId("callActivity"));
        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isFalse();
        assertThat(processInstanceMigrationValidationResult.getValidationMessages())
                .isEqualTo(Collections.singletonList(
                        "Incomplete migration mapping for call activity. The call activity 'callActivity' called element is different in the new model. Running subProcess activities '[oddFlowTask1]' should also be mapped for migration (or the call activity itself)"));

        assertThatThrownBy(() -> processInstanceMigrationBuilder.migrate(processInstance.getId()))
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessage(
                        "Call activity 'callActivity' has a different called element in the new model. It must be mapped explicitly for migration (or all its child activities)");

        completeProcessInstanceTasks(subProcessInstance.getId());
        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testValidationOfInvalidActivityInMigrationMappingToCallActivitySubProcess() {
        ProcessDefinition procDefSimpleOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procDefWithCallActivity = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/two-tasks-with-call-activity.bpmn20.xml");
        deployProcessDefinition("my deploy", "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefSimpleOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procDefSimpleOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .contains("userTask1Id", procDefSimpleOneTask.getId());

        //Prepare and action the migration - to a new ProcessDefinition containing a call activity subProcess
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefWithCallActivity.getId())
                .addActivityMigrationMapping(
                        ActivityMigrationMapping.createMappingFor("userTask1Id", "wrongActivityId").inSubProcessOfCallActivityId("callActivity"));
        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isFalse();
        assertThat(processInstanceMigrationValidationResult.getValidationMessages())
                .isEqualTo(Collections.singletonList(
                        "Invalid mapping for 'userTask1Id' to 'wrongActivityId', cannot be found in the process definition with id 'oneTaskProcess'"));

        assertThatThrownBy(() -> processInstanceMigrationBuilder.migrate(processInstance.getId()))
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessage("Cannot find activity 'wrongActivityId' in process definition with id 'oneTaskProcess'");

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testValidationOfInvalidCallActivityInMigrateMappingToCallActivitySubProcess() {
        ProcessDefinition procDefSimpleOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procDefWithCallActivity = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/two-tasks-with-call-activity.bpmn20.xml");
        deployProcessDefinition("my deploy", "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefSimpleOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procDefSimpleOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .contains("userTask1Id", procDefSimpleOneTask.getId());

        //Prepare and action the migration - to a new ProcessDefinition containing a call activity subProcess
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefWithCallActivity.getId())
                .addActivityMigrationMapping(
                        ActivityMigrationMapping.createMappingFor("userTask1Id", "theTask").inSubProcessOfCallActivityId("wrongCallActivity"));
        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isFalse();
        assertThat(processInstanceMigrationValidationResult.getValidationMessages())
                .isEqualTo(Collections.singletonList(
                        "There's no call activity element with id 'wrongCallActivity' in the process definition with id 'twoTasksParentProcess'"));

        assertThatThrownBy(() -> processInstanceMigrationBuilder.migrate(processInstance.getId()))
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessage("Cannot find activity 'wrongCallActivity' in process definition with id 'twoTasksParentProcess'");

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateMovingActivityIntoCallActivitySubProcessSpecificVersion() {
        ProcessDefinition procDefSimpleOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml");
        ProcessDefinition procDefWithCallActivity = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/two-tasks-with-call-activity-v2.bpmn20.xml");
        ProcessDefinition procDefCallActivityV1 = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process-v2.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefSimpleOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("theTask");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefSimpleOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .contains("theTask", procDefSimpleOneTask.getId());

        //First migration attempt using latest "latest" version (default) - The first version of the subProcess contains "userTaskId" but the latest version does not
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefWithCallActivity.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("theTask", "userTask1Id").inSubProcessOfCallActivityId("callActivity"));
        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isFalse();
        assertThat(processInstanceMigrationValidationResult.getValidationMessages())
                .isEqualTo(Collections.singletonList("Invalid mapping for 'theTask' to 'userTask1Id', cannot be found in the process definition with id 'MP'"));

        assertThatThrownBy(() -> processInstanceMigrationBuilder.migrate(processInstance.getId()))
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessage("Cannot find activity 'userTask1Id' in process definition with id 'MP'");

        //Second migration attempt using and invalid "unExistent" version
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder2 = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefWithCallActivity.getId())
                .addActivityMigrationMapping(
                        ActivityMigrationMapping.createMappingFor("theTask", "userTask1Id").inSubProcessOfCallActivityId("callActivity", 5));
        processInstanceMigrationValidationResult = processInstanceMigrationBuilder2.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isFalse();
        assertThat(processInstanceMigrationValidationResult.getValidationMessages())
                .isEqualTo(Collections.singletonList("Invalid mapping for 'theTask' to 'userTask1Id', cannot be found in the process definition with id 'MP'"));

        assertThatThrownBy(() -> processInstanceMigrationBuilder2.migrate(processInstance.getId()))
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessage("Cannot find activity 'userTask1Id' in process definition with id 'MP'");

        //Second migration attempt specifies the version of the call activity subProcess
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder3 = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefWithCallActivity.getId())
                .addActivityMigrationMapping(
                        ActivityMigrationMapping.createMappingFor("theTask", "userTask1Id").inSubProcessOfCallActivityId("callActivity", 1));
        processInstanceMigrationValidationResult = processInstanceMigrationBuilder3.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isNullOrEmpty();

        processInstanceMigrationBuilder3.migrate(processInstance.getId());

        //Confirm migration
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("callActivity");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procDefWithCallActivity.getId());

        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
        assertThat(subProcessInstance).isNotNull();

        List<Execution> subProcessExecutions = runtimeService.createExecutionQuery().processInstanceId(subProcessInstance.getId()).onlyChildExecutions().list();
        assertThat(subProcessExecutions)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        assertThat(subProcessExecutions)
                .extracting("processDefinitionId")
                .containsOnly(procDefCallActivityV1.getId());

        List<Task> subProcessTasks = taskService.createTaskQuery().processInstanceId(subProcessInstance.getId()).list();
        assertThat(subProcessTasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .contains(tuple("userTask1Id", procDefCallActivityV1.getId()));

        //Complete process
        completeProcessInstanceTasks(subProcessInstance.getId());
        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateMovingActivityIntoCallActivitySubProcessWithCalledElementExpression() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml");
        ProcessDefinition procDefWithCallActivity = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/two-tasks-with-expression-call-activity.bpmn20.xml");
        ProcessDefinition procDefCallActivity = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
        assertThat(subProcessInstance).isNull();

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey)
                .isEqualTo("theTask");

        //First migration attempt fails because the callElement expression cannot be evaluated
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefWithCallActivity.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("theTask", "theTask").inSubProcessOfCallActivityId("callActivity"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isFalse();
        assertThat(processInstanceMigrationValidationResult.getValidationMessages())
                .isEqualTo(Collections.singletonList(
                        "no processes deployed with key '${simpleSubProcessExpression}' for call activity element with id 'callActivity' in the process definition with id 'twoTasksParentProcess'"));

        assertThatThrownBy(() -> processInstanceMigrationBuilder.migrate(processInstance.getId()))
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessage("Cannot resolve calledElement expression '${simpleSubProcessExpression}' of callActivity 'callActivity'");

        //Now we do the migration specifying the process variable to resolve the calledElement
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder2 = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefWithCallActivity.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("theTask", "theTask").inSubProcessOfCallActivityId("callActivity", 2))
                .withProcessInstanceVariable("simpleSubProcessExpression", "oneTaskProcess");

        processInstanceMigrationValidationResult = processInstanceMigrationBuilder2.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isNullOrEmpty();

        processInstanceMigrationBuilder2.migrate(processInstance.getId());

        //Confirm migration
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("callActivity");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procDefWithCallActivity.getId());

        subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
        assertThat(subProcessInstance).extracting(ProcessInstance::getProcessDefinitionId).isEqualTo(procDefCallActivity.getId());

        List<Execution> subProcessExecutions = runtimeService.createExecutionQuery().processInstanceId(subProcessInstance.getId()).onlyChildExecutions().list();
        assertThat(subProcessExecutions).extracting(Execution::getActivityId).containsExactly("theTask");
        assertThat(subProcessExecutions).extracting("processDefinitionId").containsOnly(procDefCallActivity.getId());

        List<Task> subProcessTasks = taskService.createTaskQuery().processInstanceId(subProcessInstance.getId()).list();
        assertThat(subProcessTasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .contains(tuple("theTask", procDefCallActivity.getId()));

        //Complete process
        completeProcessInstanceTasks(subProcessInstance.getId());
        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateMovingCallActivityToSimpleActivity() {
        ProcessDefinition procDefWithCallActivity = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/two-tasks-with-call-activity.bpmn20.xml");
        ProcessDefinition procDefSubProcess = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml");
        ProcessDefinition procDefSimpleOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefWithCallActivity.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey)
                .isEqualTo("firstTask");
        completeTask(task);

        //Confirm the state to migrate
        List<Execution> processExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(processExecutions)
                .extracting(Execution::getActivityId)
                .containsExactly("callActivity");
        assertThat(processExecutions)
                .extracting("processDefinitionId")
                .containsOnly(procDefWithCallActivity.getId());

        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
        List<Execution> subProcessExecutions = runtimeService.createExecutionQuery().processInstanceId(subProcessInstance.getId()).onlyChildExecutions().list();
        assertThat(subProcessExecutions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("theTask");
        assertThat(subProcessExecutions)
                .extracting("processDefinitionId")
                .containsOnly(procDefSubProcess.getId());
        List<Task> subProcessTasks = taskService.createTaskQuery().processInstanceId(subProcessInstance.getId()).list();
        assertThat(subProcessTasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .contains(tuple("theTask", procDefSubProcess.getId()));

        //Prepare and action the migration - to a new ProcessDefinition containing a call activity subProcess
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefSimpleOneTask.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("callActivity", "userTask1Id"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isNullOrEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm migration
        processExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(processExecutions)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        assertThat(processExecutions)
                .extracting("processDefinitionId")
                .containsOnly(procDefSimpleOneTask.getId());

        subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
        assertThat(subProcessInstance).isNull();

        List<Task> processTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(processTasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .contains(tuple("userTask1Id", procDefSimpleOneTask.getId()));

        completeTask(processTasks.get(0));

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateMovingParallelMultiInstanceCallActivityToSimpleActivity() {

        Condition<Execution> isInactiveExecution = new Condition<>(execution -> !((ExecutionEntity) execution).isActive(), "inactive execution");

        ProcessDefinition procDefWithCallActivity = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/parallel-multi-instance-call-activity.bpmn20.xml");
        ProcessDefinition procDefSubProcess = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml");
        ProcessDefinition procDefSimpleOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefWithCallActivity.getId(), Collections.singletonMap("nrOfLoops", 3));
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey)
                .isEqualTo("beforeMultiInstance");
        completeTask(task);

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        //MultiInstance parent and its parallel instances
        assertThat(executions).hasSize(4);
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsOnly("parallelMICallActivity");
        //Parallel MI root execution is inactive
        assertThat(executions).haveExactly(1, isInactiveExecution);
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procDefWithCallActivity.getId());

        Execution miRoot = executions.stream().filter(e -> ((ExecutionEntity) e).isMultiInstanceRoot()).findFirst().get();
        Map<String, Object> miRootVars = runtimeService.getVariables(miRoot.getId());
        assertThat(miRootVars)
                .extracting("nrOfActiveInstances", "nrOfCompletedInstances", "nrOfLoops")
                .containsExactly(3, 0, 3);

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).isEmpty();

        List<ProcessInstance> subProcessInstances = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).list();
        assertThat(subProcessInstances).hasSize(3);

        tasks = subProcessInstances.stream()
                .map(ProcessInstance::getId)
                .map(subProcId -> taskService.createTaskQuery().processInstanceId(subProcId).singleResult())
                .collect(Collectors.toList());

        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly(tuple("theTask", procDefSubProcess.getId()));

        //Prepare and action the migration
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefSimpleOneTask.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("parallelMICallActivity", "userTask1Id"));
        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());

        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isNullOrEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm migration
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procDefSimpleOneTask.getId());

        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
        assertThat(subProcessInstance).isNull();

        List<Task> processTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(processTasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly(tuple("userTask1Id", procDefSimpleOneTask.getId()));

        completeTask(processTasks.get(0));

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Disabled("Not supported - Cannot migrate to an arbitrary activity inside an MI subProcess")
    public void testInvalidMigrationMovingCallActivityToActivityInsideMultiInstance() {
        ProcessDefinition procDefWithCallActivity = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/two-tasks-with-call-activity.bpmn20.xml");
        ProcessDefinition procDefSubProcess = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml");
        ProcessDefinition procDefMITask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/parallel-multi-instance-subprocess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefWithCallActivity.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("firstTask");
        completeTask(task);

        //Confirm the state to migrate
        List<Execution> processExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(processExecutions)
                .extracting(Execution::getActivityId)
                .containsExactly("callActivity");
        assertThat(processExecutions)
                .extracting("processDefinitionId")
                .containsOnly(procDefWithCallActivity.getId());

        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
        List<Execution> subProcessExecutions = runtimeService.createExecutionQuery().processInstanceId(subProcessInstance.getId()).onlyChildExecutions().list();
        assertThat(subProcessExecutions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("theTask");
        assertThat(subProcessExecutions)
                .extracting("processDefinitionId")
                .containsOnly(procDefSubProcess.getId());
        List<Task> subProcessTasks = taskService.createTaskQuery().processInstanceId(subProcessInstance.getId()).list();
        assertThat(subProcessTasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly(tuple("theTask", procDefSubProcess.getId()));

        //Prepare and action the migration
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefMITask.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("callActivity", "subTask2"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isFalse();
        assertThat(processInstanceMigrationValidationResult.getValidationMessages())
                .isEqualTo(Collections.singletonList(
                        "Invalid mapping for 'callActivity' to 'subTask2', cannot migrate arbitrarily inside a Multi Instance container 'parallelMISubProcess' inside process definition with id 'parallelMultiInstanceSubProcess'"));

        assertThatThrownBy(() -> processInstanceMigrationBuilder.migrate(processInstance.getId()))
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessage("Cannot find activity 'wrongActivityId' in process definition for with id 'oneTaskProcess'");
    }

    @Test
    public void testMigrateMovingActivityIntoCallActivitySubProcess() {
        ProcessDefinition procDefSimpleOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procDefWithCallActivity = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/two-tasks-with-call-activity.bpmn20.xml");
        ProcessDefinition procDefCallActivity = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefSimpleOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procDefSimpleOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly("userTask1Id", procDefSimpleOneTask.getId());

        //Prepare and action the migration - to a new ProcessDefinition containing a call activity subProcess
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefWithCallActivity.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "theTask").inSubProcessOfCallActivityId("callActivity"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isNullOrEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm migration
        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
        assertThat(subProcessInstance).isNotNull();

        List<Execution> processExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(processExecutions)
                .extracting(Execution::getActivityId)
                .containsExactly("callActivity");
        assertThat(processExecutions)
                .extracting("processDefinitionId")
                .containsOnly(procDefWithCallActivity.getId());

        List<Execution> subProcessExecutions = runtimeService.createExecutionQuery().processInstanceId(subProcessInstance.getId()).onlyChildExecutions().list();
        assertThat(subProcessExecutions)
                .extracting(Execution::getActivityId)
                .containsExactly("theTask");
        assertThat(subProcessExecutions)
                .extracting("processDefinitionId")
                .containsOnly(procDefCallActivity.getId());

        List<Task> processTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(processTasks).isEmpty();

        List<Task> subProcessTasks = taskService.createTaskQuery().processInstanceId(subProcessInstance.getId()).list();
        assertThat(subProcessTasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly(tuple("theTask", procDefCallActivity.getId()));

        completeTask(subProcessTasks.get(0));

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly("secondTask", procDefWithCallActivity.getId());

        completeTask(task);

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateMovingActivityIntoCallActivitySubProcessV2() {
        ProcessDefinition procDefSimpleOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procDefWithCallActivity = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/two-tasks-with-call-activity-v2.bpmn20.xml");
        ProcessDefinition procDefCallActivity = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/two-tasks-simple-process.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefSimpleOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procDefSimpleOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly("userTask1Id", procDefSimpleOneTask.getId());

        //Prepare and action the migration - to a new ProcessDefinition containing a call activity subProcess
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefWithCallActivity.getId())
                .addActivityMigrationMapping(
                        ActivityMigrationMapping.createMappingFor("userTask1Id", "userTask2Id").inSubProcessOfCallActivityId("callActivity"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isNullOrEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
        assertThat(subProcessInstance).isNotNull();

        List<Execution> processExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(processExecutions)
                .extracting(Execution::getActivityId)
                .containsExactly("callActivity");
        assertThat(processExecutions)
                .extracting("processDefinitionId")
                .containsOnly(procDefWithCallActivity.getId());

        List<Execution> subProcessExecutions = runtimeService.createExecutionQuery().processInstanceId(subProcessInstance.getId()).onlyChildExecutions().list();
        assertThat(subProcessExecutions)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask2Id");
        assertThat(subProcessExecutions)
                .extracting("processDefinitionId")
                .containsOnly(procDefCallActivity.getId());

        List<Task> processTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(processTasks).isEmpty();

        List<Task> subProcessTasks = taskService.createTaskQuery().processInstanceId(subProcessInstance.getId()).list();
        assertThat(subProcessTasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly(tuple("userTask2Id", procDefCallActivity.getId()));

        completeTask(subProcessTasks.get(0));

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly("secondTask", procDefWithCallActivity.getId());

        completeTask(task);

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateMovingActivityIntoCallActivitySubProcessWithCallElementExpression() {
        ProcessDefinition procDefSimpleOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procDefWithCallActivity = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/two-tasks-with-call-activity.bpmn20.xml");
        ProcessDefinition procDefCallActivity = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefSimpleOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procDefSimpleOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly("userTask1Id", procDefSimpleOneTask.getId());

        //Prepare and action the migration - to a new ProcessDefinition containing a call activity subProcess
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefWithCallActivity.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "theTask").inSubProcessOfCallActivityId("callActivity"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isNullOrEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm migration
        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
        assertThat(subProcessInstance).isNotNull();

        List<Execution> processExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(processExecutions)
                .extracting(Execution::getActivityId)
                .containsExactly("callActivity");
        assertThat(processExecutions)
                .extracting("processDefinitionId")
                .containsOnly(procDefWithCallActivity.getId());

        List<Execution> subProcessExecutions = runtimeService.createExecutionQuery().processInstanceId(subProcessInstance.getId()).onlyChildExecutions().list();
        assertThat(subProcessExecutions)
                .extracting(Execution::getActivityId)
                .containsExactly("theTask");
        assertThat(subProcessExecutions)
                .extracting("processDefinitionId")
                .containsOnly(procDefCallActivity.getId());

        List<Task> processTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(processTasks).isEmpty();

        List<Task> subProcessTasks = taskService.createTaskQuery().processInstanceId(subProcessInstance.getId()).list();
        assertThat(subProcessTasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly(tuple("theTask", procDefCallActivity.getId()));

        completeTask(subProcessTasks.get(0));

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("secondTask");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefWithCallActivity.getId());
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly("secondTask", procDefWithCallActivity.getId());

        completeTask(task);

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateMovingCallActivitySubProcessActivityToSubProcessParent() {
        ProcessDefinition procDefWithCallActivity = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/two-tasks-with-call-activity.bpmn20.xml");
        ProcessDefinition procDefCallActivity = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml");
        ProcessDefinition procDefSimpleOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefWithCallActivity.getId());

        //Confirm the state to migrate
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly("firstTask", procDefWithCallActivity.getId());
        completeTask(task);

        List<Execution> processExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(processExecutions)
                .extracting(Execution::getActivityId)
                .containsExactly("callActivity");
        assertThat(processExecutions)
                .extracting("processDefinitionId")
                .containsOnly(procDefWithCallActivity.getId());
        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
        List<Execution> subProcessExecutions = runtimeService.createExecutionQuery().processInstanceId(subProcessInstance.getId()).onlyChildExecutions().list();
        assertThat(subProcessExecutions)
                .extracting(Execution::getActivityId)
                .containsExactly("theTask");
        assertThat(subProcessExecutions)
                .extracting("processDefinitionId")
                .containsOnly(procDefCallActivity.getId());
        List<Task> subProcessTasks = taskService.createTaskQuery().processInstanceId(subProcessInstance.getId()).list();
        assertThat(subProcessTasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly(tuple("theTask", procDefCallActivity.getId()));

        //Prepare and action the migration - to a new ProcessDefinition containing a call activity subProcess
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefSimpleOneTask.getId())
                .addActivityMigrationMapping(
                        ActivityMigrationMapping.createMappingFor("theTask", "userTask1Id").inParentProcessOfCallActivityId("callActivity"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isNullOrEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm migration
        subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
        assertThat(subProcessInstance).isNull();

        processExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(processExecutions)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        assertThat(processExecutions)
                .extracting("processDefinitionId")
                .containsOnly(procDefSimpleOneTask.getId());

        List<Task> processTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(processTasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly(tuple("userTask1Id", procDefSimpleOneTask.getId()));

        //Complete process
        completeTask(processTasks.get(0));
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateMovingParallelActivitiesToParallelCallActivitySubProcesses() {
        ProcessDefinition procDefParallelGateway = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/parallel-gateway-two-splits-four-tasks.bpmn20.xml");
        ProcessDefinition procDefParallelCallActivity = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/parallel-gateway-two-splits-with-call-activities.bpmn20.xml");
        ProcessDefinition procDefCallActivity1 = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml");
        ProcessDefinition procDefCallActivity2 = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefParallelGateway.getId());

        //Confirm state to migrate
        Task initialTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(initialTask).extracting(Task::getTaskDefinitionKey).isEqualTo("taskBefore");
        completeTask(initialTask);

        List<Execution> processExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(processExecutions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("oddFlowTask1", "evenFlowTask2");
        assertThat(processExecutions)
                .extracting("processDefinitionId")
                .containsOnly(procDefParallelGateway.getId());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("oddFlowTask1", "evenFlowTask2");
        assertThat(tasks)
                .extracting(Task::getProcessDefinitionId)
                .containsOnly(procDefParallelGateway.getId());

        //Prepare and action the migration
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefParallelCallActivity.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("oddFlowTask1", "callActivity1"))
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("evenFlowTask2", "callActivity2"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isNullOrEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        processExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(processExecutions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("callActivity1", "callActivity2");
        assertThat(processExecutions)
                .extracting("processDefinitionId")
                .containsOnly(procDefParallelCallActivity.getId());

        List<ProcessInstance> subProcesses = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).list();
        assertThat(subProcesses)
                .extracting(ProcessInstance::getProcessDefinitionId)
                .containsExactlyInAnyOrder(procDefCallActivity1.getId(), procDefCallActivity2.getId());

        tasks = subProcesses.stream()
                .map(ProcessInstance::getId)
                .map(subProcInstanceId -> taskService.createTaskQuery().processInstanceId(subProcInstanceId).list())
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("userTask1Id", "theTask");
        assertThat(tasks)
                .extracting(Task::getProcessDefinitionId)
                .containsExactlyInAnyOrder(procDefCallActivity1.getId(), procDefCallActivity2.getId());

        //Complete the process...
        tasks.forEach(this::completeTask);

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly("taskAfter", procDefParallelCallActivity.getId());

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateMovingParallelActivitiesToInsideParallelCallActivitySubProcesses() {
        ProcessDefinition procDefParallelGateway = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/parallel-gateway-two-splits-four-tasks.bpmn20.xml");
        ProcessDefinition procDefParallelCallActivity = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/parallel-gateway-two-splits-with-call-activities.bpmn20.xml");
        ProcessDefinition procDefCallActivity1 = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml");
        ProcessDefinition procDefCallActivity2 = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefParallelGateway.getId());

        //Confirm state to migrate
        Task initialTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(initialTask).extracting(Task::getTaskDefinitionKey).isEqualTo("taskBefore");
        completeTask(initialTask);

        List<Execution> processExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(processExecutions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("oddFlowTask1", "evenFlowTask2");
        assertThat(processExecutions)
                .extracting("processDefinitionId")
                .containsOnly(procDefParallelGateway.getId());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("oddFlowTask1", "evenFlowTask2");
        assertThat(tasks)
                .extracting(Task::getProcessDefinitionId)
                .containsOnly(procDefParallelGateway.getId());

        //Prepare and action the migration
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefParallelCallActivity.getId())
                .addActivityMigrationMapping(
                        ActivityMigrationMapping.createMappingFor("oddFlowTask1", "userTask1Id").inSubProcessOfCallActivityId("callActivity1"))
                .addActivityMigrationMapping(
                        ActivityMigrationMapping.createMappingFor("evenFlowTask2", "theTask").inSubProcessOfCallActivityId("callActivity2"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isNullOrEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        processExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(processExecutions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("callActivity1", "callActivity2");
        assertThat(processExecutions)
                .extracting("processDefinitionId")
                .containsOnly(procDefParallelCallActivity.getId());

        List<ProcessInstance> subProcesses = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).list();
        assertThat(subProcesses)
                .extracting(ProcessInstance::getProcessDefinitionId)
                .containsExactlyInAnyOrder(procDefCallActivity1.getId(), procDefCallActivity2.getId());

        tasks = subProcesses.stream()
                .map(ProcessInstance::getId)
                .map(subProcInstanceId -> taskService.createTaskQuery().processInstanceId(subProcInstanceId).list())
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("userTask1Id", "theTask");
        assertThat(tasks)
                .extracting(Task::getProcessDefinitionId)
                .containsExactlyInAnyOrder(procDefCallActivity1.getId(), procDefCallActivity2.getId());

        //Complete the process...
        tasks.forEach(this::completeTask);

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly("taskAfter", procDefParallelCallActivity.getId());

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateMovingParallelGatewayActivitiesToSingleActivityInsideCallActivitySubProcess() {
        ProcessDefinition procDefParallelGateway = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/parallel-gateway-two-splits-four-tasks.bpmn20.xml");
        ProcessDefinition procDefWithCallActivity = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/two-tasks-with-call-activity.bpmn20.xml");
        ProcessDefinition procDefCallActivity = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefParallelGateway.getId());

        //Confirm state to migrate
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("taskBefore");
        completeTask(task);

        List<Execution> processExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(processExecutions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("oddFlowTask1", "evenFlowTask2");
        assertThat(processExecutions)
                .extracting("processDefinitionId")
                .containsOnly(procDefParallelGateway.getId());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("oddFlowTask1", "evenFlowTask2");
        assertThat(tasks)
                .extracting(Task::getProcessDefinitionId)
                .containsOnly(procDefParallelGateway.getId());

        //Prepare and action the migration
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefWithCallActivity.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor(Arrays.asList("oddFlowTask1", "evenFlowTask2"), "theTask")
                        .inSubProcessOfCallActivityId("callActivity"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isNullOrEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm migration
        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
        assertThat(subProcessInstance).extracting(ProcessInstance::getProcessDefinitionId).isEqualTo(procDefCallActivity.getId());

        processExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(processExecutions)
                .extracting(Execution::getActivityId)
                .containsExactly("callActivity");
        assertThat(processExecutions)
                .extracting("processDefinitionId")
                .containsOnly(procDefWithCallActivity.getId());

        List<Execution> subProcessExecutions = runtimeService.createExecutionQuery().processInstanceId(subProcessInstance.getId()).onlyChildExecutions().list();
        assertThat(subProcessExecutions)
                .extracting(Execution::getActivityId)
                .containsExactly("theTask");
        assertThat(subProcessExecutions)
                .extracting("processDefinitionId")
                .containsOnly(procDefCallActivity.getId());

        List<Task> processTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(processTasks).isEmpty();

        List<Task> subProcessTasks = taskService.createTaskQuery().processInstanceId(subProcessInstance.getId()).list();
        assertThat(subProcessTasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly(tuple("theTask", procDefCallActivity.getId()));

        completeTask(subProcessTasks.get(0));

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly("secondTask", procDefWithCallActivity.getId());

        completeTask(task);

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateMovingParallelActivitiesToSingleActivityInsideCallActivitySubProcess() {
        ProcessDefinition procParallelTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/timer-parallel-task.bpmn20.xml");
        ProcessDefinition procDefWithCallActivity = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/two-tasks-with-call-activity.bpmn20.xml");
        ProcessDefinition procDefCallActivity = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procParallelTask.getId());
        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        //Confirm state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("timerBound", "processTask", "parallelTask");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procParallelTask.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("processTask", "parallelTask");
        assertThat(tasks)
                .extracting(Task::getProcessDefinitionId)
                .containsOnly(procParallelTask.getId());

        //Prepare and action the migration
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefWithCallActivity.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor(Arrays.asList("processTask", "parallelTask"), "theTask")
                        .inSubProcessOfCallActivityId("callActivity"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isNullOrEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm migration
        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
        assertThat(subProcessInstance).extracting(ProcessInstance::getProcessDefinitionId).isEqualTo(procDefCallActivity.getId());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("callActivity");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procDefWithCallActivity.getId());

        List<Execution> subProcessExecutions = runtimeService.createExecutionQuery().processInstanceId(subProcessInstance.getId()).onlyChildExecutions().list();
        assertThat(subProcessExecutions)
                .extracting(Execution::getActivityId)
                .containsExactly("theTask");
        assertThat(subProcessExecutions)
                .extracting("processDefinitionId")
                .containsOnly(procDefCallActivity.getId());

        List<Task> processTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(processTasks).isEmpty();

        List<Task> subProcessTasks = taskService.createTaskQuery().processInstanceId(subProcessInstance.getId()).list();
        assertThat(subProcessTasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly(tuple("theTask", procDefCallActivity.getId()));

        completeTask(subProcessTasks.get(0));

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly("secondTask", procDefWithCallActivity.getId());

        completeTask(task);

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateMovingParallelGatewayActivitiesToSingleActivityInSubProcessParent() {
        ProcessDefinition procDefWithCallActivity = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/two-tasks-with-expression-call-activity.bpmn20.xml");
        ProcessDefinition procDefCallActivity = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/parallel-gateway-two-splits-four-tasks.bpmn20.xml");
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml");

        ProcessInstance processInstance = runtimeService
                .startProcessInstanceById(procDefWithCallActivity.getId(), Collections.singletonMap("simpleSubProcessExpression", "startParallelProcess"));

        //Confirm state to migrate
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("taskBefore");
        completeTask(task);

        List<Execution> processExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(processExecutions)
                .extracting(Execution::getActivityId)
                .containsExactly("callActivity");
        assertThat(processExecutions)
                .extracting("processDefinitionId")
                .containsOnly(procDefWithCallActivity.getId());

        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
        assertThat(subProcessInstance).extracting(ProcessInstance::getProcessDefinitionId).isEqualTo(procDefCallActivity.getId());

        task = taskService.createTaskQuery().processInstanceId(subProcessInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("taskBefore");
        completeTask(task);

        List<Execution> subProcessExecutions = runtimeService.createExecutionQuery().processInstanceId(subProcessInstance.getId()).onlyChildExecutions().list();
        assertThat(subProcessExecutions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("oddFlowTask1", "evenFlowTask2");
        assertThat(subProcessExecutions)
                .extracting("processDefinitionId")
                .containsOnly(procDefCallActivity.getId());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(subProcessInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("oddFlowTask1", "evenFlowTask2");
        assertThat(tasks)
                .extracting(Task::getProcessDefinitionId)
                .containsOnly(procDefCallActivity.getId());

        //Prepare and action the migration
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefOneTask.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor(Arrays.asList("oddFlowTask1", "evenFlowTask2"), "theTask")
                        .inParentProcessOfCallActivityId("callActivity"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isNullOrEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm migration
        subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
        assertThat(subProcessInstance).isNull();

        processExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(processExecutions)
                .extracting(Execution::getActivityId)
                .containsExactly("theTask");
        assertThat(processExecutions)
                .extracting("processDefinitionId")
                .containsOnly(procDefOneTask.getId());

        List<Task> processTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(processTasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly(tuple("theTask", procDefOneTask.getId()));

        completeProcessInstanceTasks(processInstance.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateMovingSingleActivityToParallelActivitiesInsideCallActivitySubProcess() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml");
        ProcessDefinition procDefWithCallActivity = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/two-tasks-with-call-activity-v3.bpmn20.xml");
        ProcessDefinition procDefCallActivity = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/parallel-gateway-two-splits-four-tasks.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("theTask");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procDefOneTask.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly(tuple("theTask", procDefOneTask.getId()));

        //Prepare and action the migration
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefWithCallActivity.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("theTask", Arrays.asList("oddFlowTask1", "evenFlowTask4"))
                        .inSubProcessOfCallActivityId("callActivity"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isNullOrEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm migration
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("callActivity");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procDefWithCallActivity.getId());

        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
        assertThat(subProcessInstance).extracting(ProcessInstance::getProcessDefinitionId).isEqualTo(procDefCallActivity.getId());

        List<Execution> subProcessExecutions = runtimeService.createExecutionQuery().processInstanceId(subProcessInstance.getId()).onlyChildExecutions().list();
        assertThat(subProcessExecutions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("oddFlowTask1", "evenFlowTask4");
        assertThat(subProcessExecutions)
                .extracting("processDefinitionId")
                .containsOnly(procDefCallActivity.getId());

        List<Task> subProcessTasks = taskService.createTaskQuery().processInstanceId(subProcessInstance.getId()).list();
        assertThat(subProcessTasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("oddFlowTask1", "evenFlowTask4");
        assertThat(subProcessTasks)
                .extracting(Task::getProcessDefinitionId)
                .containsOnly(procDefCallActivity.getId());

        completeProcessInstanceTasks(subProcessInstance.getId());

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly("secondTask", procDefWithCallActivity.getId());

        completeTask(task);

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateMovingSingleActivityToParallelActivitiesInSubProcessParent() {
        ProcessDefinition procDefWithCallActivity = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/two-tasks-with-call-activity.bpmn20.xml");
        ProcessDefinition procDefCallActivity = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml");
        ProcessDefinition procDefParallelTasks = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/parallel-gateway-two-splits-four-tasks.bpmn20.xml");

        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefWithCallActivity.getId());

        //Confirm state to migrate
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("firstTask");
        completeTask(task);

        List<Execution> processExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(processExecutions)
                .extracting(Execution::getActivityId)
                .containsExactly("callActivity");
        assertThat(processExecutions)
                .extracting("processDefinitionId")
                .containsOnly(procDefWithCallActivity.getId());

        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
        assertThat(subProcessInstance).extracting(ProcessInstance::getProcessDefinitionId).isEqualTo(procDefCallActivity.getId());

        task = taskService.createTaskQuery().processInstanceId(subProcessInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("theTask");

        //Prepare and action the migration
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefParallelTasks.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("theTask", Arrays.asList("oddFlowTask1", "evenFlowTask4"))
                        .inParentProcessOfCallActivityId("callActivity"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isNullOrEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm migration
        subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
        assertThat(subProcessInstance).isNull();

        processExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(processExecutions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("oddFlowTask1", "evenFlowTask4");
        assertThat(processExecutions)
                .extracting("processDefinitionId")
                .containsOnly(procDefParallelTasks.getId());

        List<Task> processTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(processTasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("oddFlowTask1", "evenFlowTask4");
        assertThat(processTasks)
                .extracting(Task::getProcessDefinitionId)
                .containsOnly(procDefParallelTasks.getId());

        completeProcessInstanceTasks(processInstance.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateMovingParallelCallActivitySubProcessesToParallelActivitiesInSubProcessParent() {
        ProcessDefinition procDefParallelCallActivity = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/parallel-gateway-two-splits-with-call-activities.bpmn20.xml");
        ProcessDefinition procDefCallActivity1 = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml");
        ProcessDefinition procDefCallActivity2 = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procDefParallelGateway = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/parallel-gateway-two-splits-four-tasks.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefParallelCallActivity.getId());

        //Confirm state to migrate
        Task initialTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(initialTask).extracting(Task::getTaskDefinitionKey).isEqualTo("taskBefore");
        completeTask(initialTask);

        List<Execution> processExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(processExecutions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("callActivity1", "callActivity2");
        assertThat(processExecutions)
                .extracting("processDefinitionId")
                .containsOnly(procDefParallelCallActivity.getId());

        List<ProcessInstance> subProcesses = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).list();
        assertThat(subProcesses)
                .extracting(ProcessInstance::getProcessDefinitionId)
                .containsExactlyInAnyOrder(procDefCallActivity1.getId(), procDefCallActivity2.getId());

        List<Task> tasks = subProcesses.stream()
                .map(ProcessInstance::getId)
                .map(subProcInstanceId -> taskService.createTaskQuery().processInstanceId(subProcInstanceId).list())
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("userTask1Id", "theTask");
        assertThat(tasks)
                .extracting(Task::getProcessDefinitionId)
                .containsExactlyInAnyOrder(procDefCallActivity1.getId(), procDefCallActivity2.getId());

        //Prepare and action the migration
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefParallelGateway.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("callActivity1", "oddFlowTask1"))
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("callActivity2", "evenFlowTask2"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isNullOrEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        subProcesses = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).list();
        assertThat(subProcesses).isEmpty();

        processExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(processExecutions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("oddFlowTask1", "evenFlowTask2");
        assertThat(processExecutions)
                .extracting("processDefinitionId")
                .containsOnly(procDefParallelGateway.getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("oddFlowTask1", "evenFlowTask2");
        assertThat(tasks)
                .extracting(Task::getProcessDefinitionId)
                .containsOnly(procDefParallelGateway.getId());

        //Complete the process...
        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateMovingActivitiesInParallelCallActivitySubProcessesToParallelActivitiesInSubProcessParent() {
        ProcessDefinition procDefParallelCallActivity = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/parallel-gateway-two-splits-with-call-activities.bpmn20.xml");
        ProcessDefinition procDefCallActivity1 = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml");
        ProcessDefinition procDefCallActivity2 = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procDefParallelGateway = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/parallel-gateway-two-splits-four-tasks.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefParallelCallActivity.getId());

        //Confirm state to migrate
        Task initialTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(initialTask).extracting(Task::getTaskDefinitionKey).isEqualTo("taskBefore");
        completeTask(initialTask);

        List<Execution> processExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(processExecutions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("callActivity1", "callActivity2");
        assertThat(processExecutions)
                .extracting("processDefinitionId")
                .containsOnly(procDefParallelCallActivity.getId());

        List<ProcessInstance> subProcesses = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).list();
        assertThat(subProcesses)
                .extracting(ProcessInstance::getProcessDefinitionId)
                .containsExactlyInAnyOrder(procDefCallActivity1.getId(), procDefCallActivity2.getId());

        List<Task> tasks = subProcesses.stream()
                .map(ProcessInstance::getId)
                .map(subProcInstanceId -> taskService.createTaskQuery().processInstanceId(subProcInstanceId).list())
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("userTask1Id", "theTask");
        assertThat(tasks)
                .extracting(Task::getProcessDefinitionId)
                .containsExactlyInAnyOrder(procDefCallActivity1.getId(), procDefCallActivity2.getId());

        //Prepare and action the migration
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefParallelGateway.getId())
                .addActivityMigrationMapping(
                        ActivityMigrationMapping.createMappingFor("userTask1Id", "oddFlowTask1").inParentProcessOfCallActivityId("callActivity1"))
                .addActivityMigrationMapping(
                        ActivityMigrationMapping.createMappingFor("theTask", "evenFlowTask2").inParentProcessOfCallActivityId("callActivity2"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isNullOrEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        subProcesses = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).list();
        assertThat(subProcesses).isEmpty();

        processExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(processExecutions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("oddFlowTask1", "evenFlowTask2");
        assertThat(processExecutions)
                .extracting("processDefinitionId")
                .containsOnly(procDefParallelGateway.getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("oddFlowTask1", "evenFlowTask2");
        assertThat(tasks)
                .extracting(Task::getProcessDefinitionId)
                .containsOnly(procDefParallelGateway.getId());

        //Complete the process...
        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());

    }

    @Test
    public void testMigrateProcessWithCallActivityWithoutAlteringTheSubProcessDefinitionAndExecution() {
        ProcessDefinition procDefWithCallActivityV1 = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-with-call-activity.bpmn20.xml");
        ProcessDefinition procDefWithCallActivityV2 = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/two-tasks-with-call-activity.bpmn20.xml");
        ProcessDefinition procDefSubProcess = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml");

        //Start the processInstance and confirm the migration state to validate
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefWithCallActivityV1.getId());

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("firstTask");
        completeTask(task);

        List<Execution> processExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(processExecutions)
                .extracting(Execution::getActivityId)
                .containsExactly("callActivity");
        assertThat(processExecutions)
                .extracting("processDefinitionId")
                .containsOnly(procDefWithCallActivityV1.getId());

        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
        List<Execution> subProcessExecutions = runtimeService.createExecutionQuery().processInstanceId(subProcessInstance.getId()).onlyChildExecutions().list();
        assertThat(subProcessExecutions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("theTask");
        assertThat(subProcessExecutions)
                .extracting("processDefinitionId")
                .containsOnly(procDefSubProcess.getId());
        List<Task> subProcessTasks = taskService.createTaskQuery().processInstanceId(subProcessInstance.getId()).list();
        assertThat(subProcessTasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly(tuple("theTask", procDefSubProcess.getId()));

        //Prepare migration and validate
        //Call activity is not mapped explicitly or by auto-map since the call activity element exists by id and refers to the same discernible subProcess (call element)
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefWithCallActivityV2.getId());
        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isNullOrEmpty();
        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm migration
        processExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(processExecutions)
                .extracting(Execution::getActivityId)
                .containsExactly("callActivity");
        assertThat(processExecutions)
                .extracting("processDefinitionId")
                .containsOnly(procDefWithCallActivityV2.getId());

        //Same subProcess but its call activity execution its for the new definition
        processExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(processExecutions)
                .extracting(Execution::getActivityId)
                .containsExactly("callActivity");
        assertThat(processExecutions)
                .extracting("processDefinitionId")
                .containsOnly(procDefWithCallActivityV2.getId());
        ProcessInstance subProcessInstanceAfterMigration = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId())
                .singleResult();
        assertThat(subProcessInstanceAfterMigration).isEqualToComparingFieldByField(subProcessInstance);
        List<Execution> subProcessExecutionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(subProcessInstanceAfterMigration.getId())
                .onlyChildExecutions().list();
        assertThat(subProcessExecutionsAfterMigration)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("theTask");
        assertThat(subProcessExecutionsAfterMigration)
                .extracting("processDefinitionId")
                .containsOnly(procDefSubProcess.getId());
        subProcessExecutions.sort(Comparator.comparing(Execution::getId));
        subProcessExecutionsAfterMigration.sort(Comparator.comparing(Execution::getId));
        assertThat(subProcessExecutionsAfterMigration).usingFieldByFieldElementComparator().isEqualTo(subProcessExecutions);

        List<Task> subProcessTasksAfterMigration = taskService.createTaskQuery().processInstanceId(subProcessInstance.getId()).list();
        assertThat(subProcessTasksAfterMigration)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly(tuple("theTask", procDefSubProcess.getId()));

        subProcessTasks.sort(Comparator.comparing(Task::getId));
        subProcessTasksAfterMigration.sort(Comparator.comparing(Task::getId));
        assertThat(subProcessTasksAfterMigration).usingFieldByFieldElementComparator().isEqualTo(subProcessTasks);

        subProcessTasksAfterMigration.forEach(this::completeTask);

        //Check the new activity added with the migration
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly("secondTask", procDefWithCallActivityV2.getId());
    }

    @Test
    @Disabled("WIP - Not supported, must use the actual callActivity when it is a MultiInstance activity @see testMigrateMovingParallelMultiInstanceCallActivityToSimpleActivity")
    public void testMigrateMovingActivityInParallelMultiInstanceCallActivityToSubProcessParent() {
        ProcessDefinition procDefWithCallActivity = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/parallel-multi-instance-call-activity.bpmn20.xml");
        ProcessDefinition procDefCallActivity = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml");
        ProcessDefinition procDefTwoTasks = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/two-tasks-simple-process.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefWithCallActivity.getId(), Collections.singletonMap("nrOfLoops", 3));

        //Confirm the state to migrate
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly("beforeMultiInstance", procDefWithCallActivity.getId());

        completeTask(task);

        List<ProcessInstance> subProcesses = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).list();
        assertThat(subProcesses).hasSize(3);
        assertThat(subProcesses).extracting(ProcessInstance::getProcessDefinitionId).containsOnly(procDefCallActivity.getId());

        List<Task> parallelTasks = subProcesses.stream().map(ProcessInstance::getId)
                .map(subProcessId -> taskService.createTaskQuery().processInstanceId(subProcessId).list()).flatMap(Collection::stream)
                .collect(Collectors.toList());
        assertThat(parallelTasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly(tuple("theTask", procDefCallActivity.getId()));

        //Prepare and action the migration
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefTwoTasks.getId())
                .addActivityMigrationMapping(
                        ActivityMigrationMapping.createMappingFor("theTask", "userTask2Id").inParentProcessOfCallActivityId("parallelMICallActivity"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isNullOrEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm migration
        subProcesses = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).list();
        assertThat(subProcesses).isEmpty();

        List<Execution> processExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(processExecutions)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask2Id");
        assertThat(processExecutions)
                .extracting("processDefinitionId")
                .containsOnly(procDefTwoTasks.getId());

        List<Task> processTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(processTasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly(tuple("userTask2Id", procDefTwoTasks.getId()));

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    //TIMERS

    @Test
    public void testMigrateCallActivityToCallActivityWithTimer() {
        ProcessDefinition procDefv1 = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/two-tasks-with-call-activity-v2.bpmn20.xml");
        ProcessDefinition procDefv2 = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/two-tasks-with-call-activity-with-timer.bpmn20.xml");
        ProcessDefinition procDefSimpleOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");

        //Start the processInstance and confirm the migration state to validate
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefv1.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey)
                .isEqualTo("firstTask");
        assertThat(task)
                .extracting(Task::getProcessDefinitionId)
                .isEqualTo(procDefv1.getId());
        //complete task so the execution entity points at the callActivity
        completeTask(task);

        List<Execution> processExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(processExecutions)
                .extracting(Execution::getActivityId)
                .containsExactly("callActivity");
        assertThat(processExecutions)
                .extracting("processDefinitionId")
                .containsOnly(procDefv1.getId());

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(timerJob).isNull();

        //Prepare migration and validate
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefv2.getId());
        //.addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("callActivity", "callActivity"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions()
                .list();
        assertThat(executionsAfterMigration)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("callActivity", "boundaryTimerEvent");
        assertThat(executionsAfterMigration)
                .extracting("processDefinitionId")
                .containsOnly(procDefv2.getId());

        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
        List<Execution> subProcessExecutions = runtimeService.createExecutionQuery().processInstanceId(subProcessInstance.getId()).onlyChildExecutions().list();
        assertThat(subProcessExecutions)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        assertThat(subProcessExecutions)
                .extracting("processDefinitionId")
                .containsOnly(procDefSimpleOneTask.getId());
        List<Task> subProcessTasks = taskService.createTaskQuery().processInstanceId(subProcessInstance.getId()).list();
        assertThat(subProcessTasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("userTask1Id");
        assertThat(subProcessTasks)
                .extracting(Task::getProcessDefinitionId)
                .containsOnly(procDefSimpleOneTask.getId());

        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(timerJob).isNotNull();
    }

    @Test
    public void testMigrateCallActivityWithTimerToCallActivityWithoutTimer() {
        ProcessDefinition procDefv1 = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/two-tasks-with-call-activity-with-timer.bpmn20.xml");
        ProcessDefinition procDefv2 = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/two-tasks-with-call-activity-v2.bpmn20.xml");
        ProcessDefinition procDefSimpleOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");

        //Start the processInstance and confirm the migration state to validate
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefv1.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey)
                .isEqualTo("firstTask");
        assertThat(task)
                .extracting(Task::getProcessDefinitionId)
                .isEqualTo(procDefv1.getId());
        //complete task so the execution entity points at the callActivity
        completeTask(task);

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("callActivity", "boundaryTimerEvent");

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(timerJob).isNotNull();

        //Prepare migration and validate
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefv2.getId());

        ProcessInstanceMigrationValidationResult processInstanceMigrationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions()
                .list();
        assertThat(executionsAfterMigration)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("callActivity");
        assertThat(executionsAfterMigration)
                .extracting("processDefinitionId")
                .containsOnly(procDefv2.getId());

        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
        List<Execution> subProcessExecutions = runtimeService.createExecutionQuery().processInstanceId(subProcessInstance.getId()).onlyChildExecutions().list();
        assertThat(subProcessExecutions)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        assertThat(subProcessExecutions)
                .extracting("processDefinitionId")
                .containsOnly(procDefSimpleOneTask.getId());
        List<Task> subProcessTasks = taskService.createTaskQuery().processInstanceId(subProcessInstance.getId()).list();
        assertThat(subProcessTasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("userTask1Id");
        assertThat(subProcessTasks)
                .extracting(Task::getProcessDefinitionId)
                .containsOnly(procDefSimpleOneTask.getId());

        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(timerJob).isNull();
    }

    @Test
    public void testMigrateActivityToActivityInsideCallActivityWithTimer() {
        ProcessDefinition procDefv1 = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/two-tasks-simple-process.bpmn20.xml");
        ProcessDefinition procDefv2 = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/two-tasks-with-call-activity-with-timer.bpmn20.xml");
        ProcessDefinition procDefSimpleOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");

        //Start the processInstance and confirm the migration state to validate
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefv1.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey)
                .isEqualTo("userTask1Id");
        assertThat(task)
                .extracting(Task::getProcessDefinitionId)
                .isEqualTo(procDefv1.getId());

        List<Execution> processExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(processExecutions)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        assertThat(processExecutions)
                .extracting("processDefinitionId")
                .containsOnly(procDefv1.getId());

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(timerJob).isNull();

        //Prepare migration and validate
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefv2.getId())
                .addActivityMigrationMapping(
                        ActivityMigrationMapping.createMappingFor("userTask1Id", "userTask1Id").inSubProcessOfCallActivityId("callActivity"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions()
                .list();
        assertThat(executionsAfterMigration)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("callActivity", "boundaryTimerEvent");
        assertThat(executionsAfterMigration)
                .extracting("processDefinitionId")
                .containsOnly(procDefv2.getId());

        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(timerJob).isNotNull();
    }

    @Test
    public void testMigrateActivityInsideCallActivityWithTimerToActivityInParentProcess() {
        ProcessDefinition procDefv1 = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/two-tasks-with-call-activity-with-timer.bpmn20.xml");
        ProcessDefinition procDefv2 = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/two-tasks-simple-process.bpmn20.xml");
        ProcessDefinition procDefSimpleOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");

        //Start the processInstance and confirm the migration state to validate
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefv1.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        //complete task so the execution entity points at the callActivity
        completeTask(task);

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions()
                .list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("callActivity", "boundaryTimerEvent");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procDefv1.getId());

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(timerJob).isNotNull();

        //Prepare migration and validate
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefv2.getId())
                .addActivityMigrationMapping(
                        ActivityMigrationMapping.createMappingFor("userTask1Id", "userTask1Id").inParentProcessOfCallActivityId("callActivity"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey)
                .isEqualTo("userTask1Id");
        assertThat(task)
                .extracting(Task::getProcessDefinitionId)
                .isEqualTo(procDefv2.getId());

        List<Execution> processExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(processExecutions)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        assertThat(processExecutions)
                .extracting("processDefinitionId")
                .containsOnly(procDefv2.getId());

        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(timerJob).isNull();
    }
}
