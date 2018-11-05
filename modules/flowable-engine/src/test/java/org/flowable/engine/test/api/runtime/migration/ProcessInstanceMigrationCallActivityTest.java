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

import java.util.List;

import org.flowable.engine.impl.migration.ProcessInstanceMigrationValidationResult;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.migration.ActivityMigrationMapping;
import org.flowable.engine.migration.ProcessInstanceMigrationBuilder;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
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
    public void testSimpleValidationOfMigrateActivityIntoCallActivitySubProcess() {
        ProcessDefinition procDefSimpleOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procDefWithCallActivity = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/two-tasks-with-call-activity.bpmn20.xml");
        ProcessDefinition procDefCallActivity = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefSimpleOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefSimpleOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefSimpleOneTask.getId());

        //Prepare and action the migration - to a new ProcessDefinition containing a call activity subProcess
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procDefWithCallActivity.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "theTask").inCallActivity("callActivity"));
        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.hasErrors()).isFalse();
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testValidationMigrateActivityIntoCallActivitySubProcessInvalidTargetActivityMapping() {
        ProcessDefinition procDefSimpleOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procDefWithCallActivity = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/two-tasks-with-call-activity.bpmn20.xml");
        ProcessDefinition procDefCallActivity = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefSimpleOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefSimpleOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefSimpleOneTask.getId());

        //Prepare and action the migration - to a new ProcessDefinition containing a call activity subProcess
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procDefWithCallActivity.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "wrongActivityId").inCallActivity("callActivity"));
        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.hasErrors()).isTrue();
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).contains("The target mapping for userTask1Id to wrongActivityId can not be found in the process definition with id 'oneTaskProcess'");

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testValidationMigrateActivityIntoCallActivitySubProcessInvalidCallActivity() {
        ProcessDefinition procDefSimpleOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procDefWithCallActivity = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/two-tasks-with-call-activity.bpmn20.xml");
        ProcessDefinition procDefCallActivity = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefSimpleOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefSimpleOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefSimpleOneTask.getId());

        //Prepare and action the migration - to a new ProcessDefinition containing a call activity subProcess
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procDefWithCallActivity.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "theTask").inCallActivity("wrongCallActivity"));
        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.hasErrors()).isTrue();
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).contains("There's no call activity element with id 'wrongCallActivity' in the process definition with id 'twoTasksParentProcess'");

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityIntoCallActivitySubProcess() {
        ProcessDefinition procDefSimpleOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procDefWithCallActivity = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/two-tasks-with-call-activity.bpmn20.xml");
        ProcessDefinition procDefCallActivity = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefSimpleOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefSimpleOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefSimpleOneTask.getId());

        //Prepare and action the migration - to a new ProcessDefinition containing a call activity subProcess
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procDefWithCallActivity.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "theTask").inCallActivity("callActivity"));
        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
        assertNotNull(subProcessInstance);

        List<Execution> processExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(processExecutions).extracting(Execution::getActivityId).containsExactly("callActivity");
        assertThat(processExecutions).extracting("processDefinitionId").containsOnly(procDefWithCallActivity.getId());

        List<Execution> subProcessExecutions = runtimeService.createExecutionQuery().processInstanceId(subProcessInstance.getId()).onlyChildExecutions().list();
        assertThat(subProcessExecutions).extracting(Execution::getActivityId).containsExactly("theTask");
        assertThat(subProcessExecutions).extracting("processDefinitionId").containsOnly(procDefCallActivity.getId());

        List<Task> processTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(processTasks).isEmpty();

        List<Task> subProcessTasks = taskService.createTaskQuery().processInstanceId(subProcessInstance.getId()).list();
        assertThat(subProcessTasks).extracting(Task::getTaskDefinitionKey).containsExactly("theTask");
        assertThat(subProcessTasks).extracting(Task::getProcessDefinitionId).containsOnly(procDefCallActivity.getId());

        completeTask(subProcessTasks.get(0));

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("secondTask");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefWithCallActivity.getId());

        completeTask(task);

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityIntoCallActivitySubProcessV2() {
        ProcessDefinition procDefSimpleOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procDefWithCallActivity = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/two-tasks-with-call-activity-v2.bpmn20.xml");
        ProcessDefinition procDefCallActivity = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/two-tasks-simple-process.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefSimpleOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefSimpleOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefSimpleOneTask.getId());

        //Prepare and action the migration - to a new ProcessDefinition containing a call activity subProcess
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procDefWithCallActivity.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "userTask2Id").inCallActivity("callActivity"));
        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
        assertNotNull(subProcessInstance);

        List<Execution> processExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(processExecutions).extracting(Execution::getActivityId).containsExactly("callActivity");
        assertThat(processExecutions).extracting("processDefinitionId").containsOnly(procDefWithCallActivity.getId());

        List<Execution> subProcessExecutions = runtimeService.createExecutionQuery().processInstanceId(subProcessInstance.getId()).onlyChildExecutions().list();
        assertThat(subProcessExecutions).extracting(Execution::getActivityId).containsExactly("userTask2Id");
        assertThat(subProcessExecutions).extracting("processDefinitionId").containsOnly(procDefCallActivity.getId());

        List<Task> processTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(processTasks).isEmpty();

        List<Task> subProcessTasks = taskService.createTaskQuery().processInstanceId(subProcessInstance.getId()).list();
        assertThat(subProcessTasks).extracting(Task::getTaskDefinitionKey).containsExactly("userTask2Id");
        assertThat(subProcessTasks).extracting(Task::getProcessDefinitionId).containsOnly(procDefCallActivity.getId());

        completeTask(subProcessTasks.get(0));

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("secondTask");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefWithCallActivity.getId());

        completeTask(task);

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Disabled("Not implemented yet!")
    public void testSimpleValidationOfMigrateActivityIntoCallActivitySubProcessParent() {
        ProcessDefinition procDefWithCallActivity = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/two-tasks-with-call-activity.bpmn20.xml");
        ProcessDefinition procDefCallActivity = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml");
        ProcessDefinition procDefSimpleOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefWithCallActivity.getId());

        //Confirm the state to migrate
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("firstTask");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefWithCallActivity.getId());
        completeTask(task);

        List<Execution> processExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(processExecutions).extracting(Execution::getActivityId).containsExactly("callActivity");
        assertThat(processExecutions).extracting("processDefinitionId").containsOnly(procDefWithCallActivity.getId());
        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
        List<Execution> subProcessExecutions = runtimeService.createExecutionQuery().processInstanceId(subProcessInstance.getId()).onlyChildExecutions().list();
        assertThat(subProcessExecutions).extracting(Execution::getActivityId).containsExactly("theTask");
        assertThat(subProcessExecutions).extracting("processDefinitionId").containsOnly(procDefCallActivity.getId());
        List<Task> subProcessTasks = taskService.createTaskQuery().processInstanceId(subProcessInstance.getId()).list();
        assertThat(subProcessTasks).extracting(Task::getTaskDefinitionKey).containsExactly("theTask");
        assertThat(subProcessTasks).extracting(Task::getProcessDefinitionId).containsOnly(procDefCallActivity.getId());

        //Prepare and action the migration - to a new ProcessDefinition containing a call activity subProcess
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procDefSimpleOneTask.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("theTask", "userTask1Id").inParentProcess());
        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Disabled("Not implemented yet!")
    public void testMigrateActivityToCallActivitySubProcessParent() {
        ProcessDefinition procDefWithCallActivity = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/two-tasks-with-call-activity.bpmn20.xml");
        ProcessDefinition procDefCallActivity = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml");
        ProcessDefinition procDefSimpleOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefWithCallActivity.getId());

        //Confirm the state to migrate
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("firstTask");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefWithCallActivity.getId());
        completeTask(task);

        List<Execution> processExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(processExecutions).extracting(Execution::getActivityId).containsExactly("callActivity");
        assertThat(processExecutions).extracting("processDefinitionId").containsOnly(procDefWithCallActivity.getId());
        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
        List<Execution> subProcessExecutions = runtimeService.createExecutionQuery().processInstanceId(subProcessInstance.getId()).onlyChildExecutions().list();
        assertThat(subProcessExecutions).extracting(Execution::getActivityId).containsExactly("theTask");
        assertThat(subProcessExecutions).extracting("processDefinitionId").containsOnly(procDefCallActivity.getId());
        List<Task> subProcessTasks = taskService.createTaskQuery().processInstanceId(subProcessInstance.getId()).list();
        assertThat(subProcessTasks).extracting(Task::getTaskDefinitionKey).containsExactly("theTask");
        assertThat(subProcessTasks).extracting(Task::getProcessDefinitionId).containsOnly(procDefCallActivity.getId());

        //Prepare and action the migration - to a new ProcessDefinition containing a call activity subProcess
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procDefWithCallActivity.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("theTask", "secondTask").inParentProcess());
        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
        assertNull(subProcessInstance);

        processExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(processExecutions).extracting(Execution::getActivityId).containsExactly("secondTask");
        assertThat(processExecutions).extracting("processDefinitionId").containsOnly(procDefWithCallActivity.getId());

        List<Task> processTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(processTasks).extracting(Task::getTaskDefinitionKey).containsExactly("secondTask");
        assertThat(processTasks).extracting(Task::getProcessDefinitionId).containsOnly(procDefWithCallActivity.getId());

        completeTask(processTasks.get(0));

        assertProcessEnded(processInstance.getId());
    }

}
