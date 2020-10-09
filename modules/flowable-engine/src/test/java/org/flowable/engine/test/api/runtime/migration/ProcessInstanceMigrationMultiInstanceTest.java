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
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.assertj.core.api.Condition;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.migration.ActivityMigrationMapping;
import org.flowable.engine.migration.ProcessInstanceMigrationBuilder;
import org.flowable.engine.migration.ProcessInstanceMigrationValidationResult;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author Dennis Federico
 */
public class ProcessInstanceMigrationMultiInstanceTest extends AbstractProcessInstanceMigrationTest {

    @AfterEach
    protected void tearDown() {
        deleteDeployments();
    }

    @Test
    public void testMigrateActivityToSequentialMultiInstanceActivity() {
        ProcessDefinition procSimpleOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procSequentialMultiInst = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/sequential-multi-instance-task.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procSimpleOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procSimpleOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly("userTask1Id", procSimpleOneTask.getId());

        //Prepare and action the migration
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procSequentialMultiInst.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "seqTasks"))
                .withProcessInstanceVariable("nrOfLoops", 3);
        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();
        processInstanceMigrationBuilder.migrate(processInstance.getId());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        //MI root execution and 1 instance
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("seqTasks", "seqTasks");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procSequentialMultiInst.getId());
        Execution miRoot = executions.stream().filter(e -> ((ExecutionEntity) e).isMultiInstanceRoot()).findFirst().get();
        Map<String, Object> miRootVars = runtimeService.getVariables(miRoot.getId());
        assertThat(miRootVars)
                .extracting("nrOfActiveInstances", "nrOfCompletedInstances", "nrOfLoops")
                .containsExactly(1, 0, 3);
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly("seqTasks", procSequentialMultiInst.getId());
        assertThat(taskService.getVariable(task.getId(), "loopCounter")).isEqualTo(0);

        //Complete one instance...
        taskService.complete(task.getId());

        //Next instance in the loop
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("seqTasks", "seqTasks");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procSequentialMultiInst.getId());
        miRoot = executions.stream().filter(e -> ((ExecutionEntity) e).isMultiInstanceRoot()).findFirst().get();
        miRootVars = runtimeService.getVariables(miRoot.getId());
        assertThat(miRootVars)
                .extracting("nrOfActiveInstances", "nrOfCompletedInstances", "nrOfLoops")
                .containsExactly(1, 1, 3);
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly("seqTasks", procSequentialMultiInst.getId());
        assertThat(taskService.getVariable(task.getId(), "loopCounter")).isEqualTo(1);

        //Complete this and the last
        taskService.complete(task.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        //Out of the loop
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("afterMultiInstance");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procSequentialMultiInst.getId());
        assertThat(executions.stream().anyMatch(e -> ((ExecutionEntity) e).isMultiInstanceRoot())).isFalse();
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly("afterMultiInstance", procSequentialMultiInst.getId());
        assertThat(taskService.getVariable(task.getId(), "loopCounter")).isNull();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procSequentialMultiInst, processInstance, "userTask", "userTask1Id", "seqTasks", "seqTasks", "seqTasks",
                    "afterMultiInstance");

            checkTaskInstance(procSequentialMultiInst, processInstance, "userTask1Id", "seqTasks", "seqTasks", "seqTasks", "afterMultiInstance");
        }

        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procSequentialMultiInst, processInstance, "userTask", "userTask1Id", "seqTasks", "seqTasks", "seqTasks",
                    "afterMultiInstance");

            checkTaskInstance(procSequentialMultiInst, processInstance, "userTask1Id", "seqTasks", "seqTasks", "seqTasks", "afterMultiInstance");
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityToParallelMultiInstanceActivity() {
        ProcessDefinition procSimpleOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procParallelMultiInst = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/parallel-multi-instance-task.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procSimpleOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procSimpleOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly("userTask1Id", procSimpleOneTask.getId());

        //Prepare and action the migration
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procParallelMultiInst.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "parallelTasks"))
                .withProcessInstanceVariable("nrOfLoops", 3);
        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();
        processInstanceMigrationBuilder.migrate(processInstance.getId());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        //MI root execution and 3 parallel instances
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("parallelTasks", "parallelTasks", "parallelTasks", "parallelTasks");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procParallelMultiInst.getId());
        Execution miRoot = executions.stream().filter(e -> ((ExecutionEntity) e).isMultiInstanceRoot()).findFirst().get();
        Map<String, Object> miRootVars = runtimeService.getVariables(miRoot.getId());
        assertThat(miRootVars)
                .extracting("nrOfActiveInstances", "nrOfCompletedInstances", "nrOfLoops")
                .containsExactly(3, 0, 3);
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly("userTask1Id", procSimpleOneTask.getId());
        assertThat(tasks).extracting(aTask -> taskService.getVariable(aTask.getId(), "loopCounter")).isNotNull();

        //Complete one instance...
        taskService.complete(tasks.get(1).getId());

        //Next instance in the loop
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        //MI Root and its 3 parallel instances
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("parallelTasks", "parallelTasks", "parallelTasks", "parallelTasks");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procParallelMultiInst.getId());
        miRoot = executions.stream().filter(e -> ((ExecutionEntity) e).isMultiInstanceRoot()).findFirst().get();
        miRootVars = runtimeService.getVariables(miRoot.getId());
        assertThat(miRootVars)
                .extracting("nrOfActiveInstances", "nrOfCompletedInstances", "nrOfLoops")
                .containsExactly(2, 1, 3);

        //Two executions are inactive, the completed instance and the MI root
        assertThat(executions).haveExactly(2, new Condition<>((Execution execution) -> !((ExecutionEntity) execution).isActive(), "inactive"));
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        //Two tasks remain for completion
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("parallelTasks", "parallelTasks");
        assertThat(tasks)
                .extracting(Task::getProcessDefinitionId)
                .containsOnly(procParallelMultiInst.getId());
        assertThat(tasks).extracting(aTask -> taskService.getVariable(aTask.getId(), "loopCounter")).isNotNull();

        //Complete the remaining parallel tasks
        tasks.forEach(this::completeTask);

        //Out of the loop
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("afterMultiInstance");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procParallelMultiInst.getId());
        assertThat(executions.stream().anyMatch(e -> ((ExecutionEntity) e).isMultiInstanceRoot())).isFalse();
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly("afterMultiInstance", procParallelMultiInst.getId());
        assertThat(taskService.getVariable(task.getId(), "loopCounter")).isNull();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procParallelMultiInst, processInstance, "userTask", "userTask1Id", "parallelTasks", "parallelTasks", "parallelTasks",
                    "afterMultiInstance");

            checkTaskInstance(procParallelMultiInst, processInstance, "userTask1Id", "parallelTasks", "parallelTasks", "parallelTasks", "afterMultiInstance");
        }

        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procParallelMultiInst, processInstance, "userTask", "userTask1Id", "parallelTasks", "parallelTasks", "parallelTasks",
                    "afterMultiInstance");

            checkTaskInstance(procParallelMultiInst, processInstance, "userTask1Id", "parallelTasks", "parallelTasks", "parallelTasks", "afterMultiInstance");
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateSequentialMultiInstanceActivityToSimpleActivity() {
        ProcessDefinition procSequentialMultiInst = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/sequential-multi-instance-task.bpmn20.xml");
        ProcessDefinition procSimpleOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procSequentialMultiInst.getId(), Collections.singletonMap("nrOfLoops", 3));
        completeTask(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        //MultiInstance parent and one instance of the task
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("seqTasks", "seqTasks");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procSequentialMultiInst.getId());
        Execution miRoot = executions.stream().filter(e -> ((ExecutionEntity) e).isMultiInstanceRoot()).findFirst().get();
        Map<String, Object> miRootVars = runtimeService.getVariables(miRoot.getId());
        assertThat(miRootVars)
                .extracting("nrOfActiveInstances", "nrOfCompletedInstances", "nrOfLoops")
                .containsExactly(1, 0, 3);

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly("seqTasks", procSequentialMultiInst.getId());
        Map<String, Object> miTaskVars = taskService.getVariables(task.getId());
        assertThat(miTaskVars)
                .contains(entry("loopCounter", 0));

        //Complete one...
        completeTask(task);

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        //MultiInstance parent and one instance of the task
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("seqTasks", "seqTasks");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procSequentialMultiInst.getId());

        miRoot = executions.stream().filter(e -> ((ExecutionEntity) e).isMultiInstanceRoot()).findFirst().get();
        miRootVars = runtimeService.getVariables(miRoot.getId());
        assertThat(miRootVars)
                .extracting("nrOfActiveInstances", "nrOfCompletedInstances", "nrOfLoops")
                .containsExactly(1, 1, 3);

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly("seqTasks", procSequentialMultiInst.getId());
        miTaskVars = taskService.getVariables(task.getId());
        assertThat(miTaskVars).extracting("loopCounter").isEqualTo(1);

        //Prepare and action the migration
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procSimpleOneTask.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("seqTasks", "userTask1Id"));
        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();
        processInstanceMigrationBuilder.migrate(processInstance.getId());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procSimpleOneTask.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly("userTask1Id", procSimpleOneTask.getId());
        assertThat(taskService.getVariable(task.getId(), "loopCounter")).isNull();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procSimpleOneTask, processInstance, "userTask", "beforeMultiInstance", "seqTasks", "seqTasks", "userTask1Id");

            checkTaskInstance(procSimpleOneTask, processInstance, "beforeMultiInstance", "seqTasks", "seqTasks", "userTask1Id");
        }

        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procSimpleOneTask, processInstance, "userTask", "beforeMultiInstance", "seqTasks", "seqTasks", "userTask1Id");

            checkTaskInstance(procSimpleOneTask, processInstance, "beforeMultiInstance", "seqTasks", "seqTasks", "userTask1Id");
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateParallelMultiInstanceActivityToSimpleActivity() {
        ProcessDefinition procParallelMultiInst = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/parallel-multi-instance-task.bpmn20.xml");
        ProcessDefinition procSimpleOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");

        Condition<Execution> isInactiveExecution = new Condition<>(execution -> !((ExecutionEntity) execution).isActive(), "inactive execution");
        Consumer<Task> hasLoopCounter = task -> {
            Map<String, Object> variables = taskService.getVariables(task.getId());
            assertThat(variables).extracting("loopCounter").isNotNull();
        };

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procParallelMultiInst.getId(), Collections.singletonMap("nrOfLoops", 3));
        completeTask(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        //MultiInstance parent and its parallel instances
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("parallelTasks", "parallelTasks", "parallelTasks", "parallelTasks");
        //Parallel MI root execution is inactive
        assertThat(executions).haveExactly(1, isInactiveExecution);
        assertThat(executions)
                .extracting("processDefinitionId").
                containsOnly(procParallelMultiInst.getId());

        Execution miRoot = executions.stream().filter(e -> ((ExecutionEntity) e).isMultiInstanceRoot()).findFirst().get();
        Map<String, Object> miRootVars = runtimeService.getVariables(miRoot.getId());
        assertThat(miRootVars)
                .extracting("nrOfActiveInstances", "nrOfCompletedInstances", "nrOfLoops")
                .containsExactly(3, 0, 3);

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).hasSize(3);
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly(tuple("parallelTasks", procParallelMultiInst.getId()));
        assertThat(tasks).allSatisfy(hasLoopCounter);

        //Complete one...
        completeTask(tasks.get(0));

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("parallelTasks", "parallelTasks", "parallelTasks", "parallelTasks");
        //MultiInstance parent and one completed/inactive execution
        assertThat(executions).haveExactly(2, isInactiveExecution);
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procParallelMultiInst.getId());

        miRoot = executions.stream().filter(e -> ((ExecutionEntity) e).isMultiInstanceRoot()).findFirst().get();
        miRootVars = runtimeService.getVariables(miRoot.getId());
        assertThat(miRootVars)
                .extracting("nrOfActiveInstances", "nrOfCompletedInstances", "nrOfLoops")
                .containsExactly(2, 1, 3);

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).hasSize(2);
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly(tuple("parallelTasks", procParallelMultiInst.getId()));
        assertThat(tasks).allSatisfy(hasLoopCounter);

        //Prepare and action the migration
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procSimpleOneTask.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("parallelTasks", "userTask1Id"));
        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();
        processInstanceMigrationBuilder.migrate(processInstance.getId());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procSimpleOneTask.getId());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly(tuple("userTask1Id", procSimpleOneTask.getId()));
        assertThat(taskService.getVariable(tasks.get(0).getId(), "loopCounter")).isNull();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procSimpleOneTask, processInstance, "userTask", "beforeMultiInstance", "parallelTasks", "parallelTasks", "parallelTasks",
                    "userTask1Id");

            checkTaskInstance(procSimpleOneTask, processInstance, "beforeMultiInstance", "parallelTasks", "parallelTasks", "parallelTasks", "userTask1Id");
        }

        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procSimpleOneTask, processInstance, "userTask", "beforeMultiInstance", "parallelTasks", "parallelTasks", "parallelTasks",
                    "userTask1Id");

            checkTaskInstance(procSimpleOneTask, processInstance, "beforeMultiInstance", "parallelTasks", "parallelTasks", "parallelTasks", "userTask1Id");
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateSequentialMultiInstanceActivityToParallelMultiInstanceActivity() {
        ProcessDefinition procSequentialMultiInst = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/sequential-multi-instance-task.bpmn20.xml");
        ProcessDefinition procParallelMultiInst = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/parallel-multi-instance-task.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procSequentialMultiInst.getId(), Collections.singletonMap("nrOfLoops", 3));
        completeTask(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        //MultiInstance parent and one instance of the task
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("seqTasks", "seqTasks");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procSequentialMultiInst.getId());
        Execution miRoot = executions.stream().filter(e -> ((ExecutionEntity) e).isMultiInstanceRoot()).findFirst().get();
        Map<String, Object> miRootVars = runtimeService.getVariables(miRoot.getId());
        assertThat(miRootVars)
                .extracting("nrOfActiveInstances", "nrOfCompletedInstances", "nrOfLoops")
                .containsExactly(1, 0, 3);

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly("seqTasks", procSequentialMultiInst.getId());
        Map<String, Object> miTaskVars = taskService.getVariables(task.getId());
        assertThat(miTaskVars)
                .contains(entry("loopCounter", 0));

        //Complete one...
        completeTask(task);

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        //MultiInstance parent and one instance of the task
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("seqTasks", "seqTasks");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procSequentialMultiInst.getId());

        miRoot = executions.stream().filter(e -> ((ExecutionEntity) e).isMultiInstanceRoot()).findFirst().get();
        miRootVars = runtimeService.getVariables(miRoot.getId());
        assertThat(miRootVars)
                .extracting("nrOfActiveInstances", "nrOfCompletedInstances", "nrOfLoops")
                .containsExactly(1, 1, 3);

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly("seqTasks", procSequentialMultiInst.getId());
        miTaskVars = taskService.getVariables(task.getId());
        assertThat(miTaskVars).extracting("loopCounter").isEqualTo(1);

        //Prepare and action the migration
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procParallelMultiInst.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("seqTasks", "parallelTasks"));
        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();
        processInstanceMigrationBuilder.migrate(processInstance.getId());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        //MI root execution and 3 parallel instances
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("parallelTasks", "parallelTasks", "parallelTasks", "parallelTasks");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procParallelMultiInst.getId());
        miRoot = executions.stream().filter(e -> ((ExecutionEntity) e).isMultiInstanceRoot()).findFirst().get();
        miRootVars = runtimeService.getVariables(miRoot.getId());
        assertThat(miRootVars)
                .extracting("nrOfActiveInstances", "nrOfCompletedInstances", "nrOfLoops")
                .containsExactly(3, 0, 3);
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("parallelTasks", "parallelTasks", "parallelTasks");
        assertThat(tasks)
                .extracting(Task::getProcessDefinitionId)
                .containsOnly(procParallelMultiInst.getId());
        assertThat(tasks).extracting(aTask -> taskService.getVariable(aTask.getId(), "loopCounter")).isNotNull();

        //Complete one instance...
        taskService.complete(tasks.get(1).getId());

        //Next instance in the loop
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        //MI Root and its 3 parallel instances
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("parallelTasks", "parallelTasks", "parallelTasks", "parallelTasks");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procParallelMultiInst.getId());
        miRoot = executions.stream().filter(e -> ((ExecutionEntity) e).isMultiInstanceRoot()).findFirst().get();
        miRootVars = runtimeService.getVariables(miRoot.getId());
        assertThat(miRootVars)
                .extracting("nrOfActiveInstances", "nrOfCompletedInstances", "nrOfLoops")
                .containsExactly(2, 1, 3);
        //Two executions are inactive, the completed instance and the MI root
        assertThat(executions).haveExactly(2, new Condition<>((Execution execution) -> !((ExecutionEntity) execution).isActive(), "inactive"));
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        //Two tasks remain for completion
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("parallelTasks", "parallelTasks");
        assertThat(tasks)
                .extracting(Task::getProcessDefinitionId)
                .containsOnly(procParallelMultiInst.getId());
        assertThat(tasks).extracting(aTask -> taskService.getVariable(aTask.getId(), "loopCounter")).isNotNull();

        //Complete the remaining parallel tasks
        tasks.forEach(this::completeTask);

        //Out of the loop
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("afterMultiInstance");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procParallelMultiInst.getId());
        assertThat(executions.stream().anyMatch(e -> ((ExecutionEntity) e).isMultiInstanceRoot())).isFalse();
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly("afterMultiInstance", procParallelMultiInst.getId());
        assertThat(taskService.getVariable(task.getId(), "loopCounter")).isNull();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procParallelMultiInst, processInstance, "userTask", "beforeMultiInstance", "seqTasks", "seqTasks", "parallelTasks",
                    "parallelTasks", "parallelTasks", "afterMultiInstance");

            checkTaskInstance(procParallelMultiInst, processInstance, "beforeMultiInstance", "seqTasks", "seqTasks", "parallelTasks", "parallelTasks",
                    "parallelTasks", "afterMultiInstance");
        }

        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procParallelMultiInst, processInstance, "userTask", "beforeMultiInstance", "seqTasks", "seqTasks", "parallelTasks",
                    "parallelTasks", "parallelTasks", "afterMultiInstance");

            checkTaskInstance(procParallelMultiInst, processInstance, "beforeMultiInstance", "seqTasks", "seqTasks", "parallelTasks", "parallelTasks",
                    "parallelTasks", "afterMultiInstance");
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateParallelMultiInstanceActivityToSequentialMultiInstanceActivity() {
        ProcessDefinition procParallelMultiInst = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/parallel-multi-instance-task.bpmn20.xml");
        ProcessDefinition procSequentialMultiInst = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/sequential-multi-instance-task.bpmn20.xml");

        Condition<Execution> isInactiveExecution = new Condition<>(execution -> !((ExecutionEntity) execution).isActive(), "inactive execution");
        Consumer<Task> hasLoopCounter = task -> {
            Map<String, Object> variables = taskService.getVariables(task.getId());
            assertThat(variables).extracting("loopCounter").isNotNull();
        };

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procParallelMultiInst.getId(), Collections.singletonMap("nrOfLoops", 3));
        completeTask(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        //MultiInstance parent and its parallel instances
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("parallelTasks", "parallelTasks", "parallelTasks", "parallelTasks");
        //Parallel MI root execution is inactive
        assertThat(executions).haveExactly(1, isInactiveExecution);
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procParallelMultiInst.getId());

        Execution miRoot = executions.stream().filter(e -> ((ExecutionEntity) e).isMultiInstanceRoot()).findFirst().get();
        Map<String, Object> miRootVars = runtimeService.getVariables(miRoot.getId());
        assertThat(miRootVars)
                .extracting("nrOfActiveInstances", "nrOfCompletedInstances", "nrOfLoops")
                .containsExactly(3, 0, 3);

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).hasSize(3);
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly(tuple("parallelTasks", procParallelMultiInst.getId()));
        assertThat(tasks).allSatisfy(hasLoopCounter);

        //Complete one...
        completeTask(tasks.get(0));

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("parallelTasks", "parallelTasks", "parallelTasks", "parallelTasks");
        //MultiInstance parent and one completed/inactive execution
        assertThat(executions).haveExactly(2, isInactiveExecution);
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procParallelMultiInst.getId());

        miRoot = executions.stream().filter(e -> ((ExecutionEntity) e).isMultiInstanceRoot()).findFirst().get();
        miRootVars = runtimeService.getVariables(miRoot.getId());
        assertThat(miRootVars)
                .extracting("nrOfActiveInstances", "nrOfCompletedInstances", "nrOfLoops")
                .containsExactly(2, 1, 3);

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).hasSize(2);
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly(tuple("parallelTasks", procParallelMultiInst.getId()));
        assertThat(tasks).allSatisfy(hasLoopCounter);

        //Prepare and action the migration
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procSequentialMultiInst.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("parallelTasks", "seqTasks"));
        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();
        processInstanceMigrationBuilder.migrate(processInstance.getId());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        //MI root execution and 1 instance
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("seqTasks", "seqTasks");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procSequentialMultiInst.getId());
        miRoot = executions.stream().filter(e -> ((ExecutionEntity) e).isMultiInstanceRoot()).findFirst().get();
        miRootVars = runtimeService.getVariables(miRoot.getId());
        assertThat(miRootVars)
                .extracting("nrOfActiveInstances", "nrOfCompletedInstances", "nrOfLoops")
                .containsExactly(1, 0, 3);
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly("seqTasks", procSequentialMultiInst.getId());
        assertThat(taskService.getVariable(task.getId(), "loopCounter")).isEqualTo(0);

        //Complete one instance...
        taskService.complete(task.getId());

        //Next instance in the loop
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("seqTasks", "seqTasks");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procSequentialMultiInst.getId());
        miRoot = executions.stream().filter(e -> ((ExecutionEntity) e).isMultiInstanceRoot()).findFirst().get();
        miRootVars = runtimeService.getVariables(miRoot.getId());
        assertThat(miRootVars)
                .extracting("nrOfActiveInstances", "nrOfCompletedInstances", "nrOfLoops")
                .containsExactly(1, 1, 3);
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly("seqTasks", procSequentialMultiInst.getId());
        assertThat(taskService.getVariable(task.getId(), "loopCounter")).isEqualTo(1);

        //Complete this and the last
        taskService.complete(task.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        //Out of the loop
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("afterMultiInstance");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procSequentialMultiInst.getId());
        assertThat(executions.stream().anyMatch(e -> ((ExecutionEntity) e).isMultiInstanceRoot())).isFalse();
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly("afterMultiInstance", procSequentialMultiInst.getId());
        assertThat(taskService.getVariable(task.getId(), "loopCounter")).isNull();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procSequentialMultiInst, processInstance, "userTask", "beforeMultiInstance", "parallelTasks", "parallelTasks",
                    "parallelTasks", "seqTasks", "seqTasks", "seqTasks", "afterMultiInstance");

            checkTaskInstance(procSequentialMultiInst, processInstance, "beforeMultiInstance", "parallelTasks", "parallelTasks", "parallelTasks", "seqTasks",
                    "seqTasks", "seqTasks", "afterMultiInstance");
        }

        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procSequentialMultiInst, processInstance, "userTask", "beforeMultiInstance", "parallelTasks", "parallelTasks",
                    "parallelTasks", "seqTasks", "seqTasks", "seqTasks", "afterMultiInstance");

            checkTaskInstance(procSequentialMultiInst, processInstance, "beforeMultiInstance", "parallelTasks", "parallelTasks", "parallelTasks", "seqTasks",
                    "seqTasks", "seqTasks", "afterMultiInstance");
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityToSequentialMultiInstanceSubProcess() {
        ProcessDefinition procSimpleOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procSequentialMultiInst = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/sequential-multi-instance-subprocess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procSimpleOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procSimpleOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly("userTask1Id", procSimpleOneTask.getId());

        //Prepare and action the migration
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procSequentialMultiInst.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "sequentialMISubProcess"))
                .withProcessInstanceVariable("nrOfLoops", 3);
        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();
        processInstanceMigrationBuilder.migrate(processInstance.getId());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        //MI subProcess root execution, actual subProcess and 1 task
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("sequentialMISubProcess", "sequentialMISubProcess", "subTask1");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procSequentialMultiInst.getId());
        Execution miRoot = executions.stream().filter(e -> ((ExecutionEntity) e).isMultiInstanceRoot()).findFirst().get();
        Map<String, Object> miRootVars = runtimeService.getVariables(miRoot.getId());
        assertThat(miRootVars)
                .extracting("nrOfActiveInstances", "nrOfCompletedInstances", "nrOfLoops")
                .containsExactly(1, 0, 3);
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly("subTask1", procSequentialMultiInst.getId());
        assertThat(taskService.getVariable(task.getId(), "loopCounter")).isEqualTo(0);

        //Complete tha task and the next in the subprocess
        completeTask(task);
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("subTask2");
        completeTask(task);

        //Should be in the next MI subProcess iteration
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        //MI subprocess root execution, actual subprocess and 1 task
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("sequentialMISubProcess", "sequentialMISubProcess", "subTask1");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procSequentialMultiInst.getId());
        miRoot = executions.stream().filter(e -> ((ExecutionEntity) e).isMultiInstanceRoot()).findFirst().get();
        miRootVars = runtimeService.getVariables(miRoot.getId());
        assertThat(miRootVars)
                .extracting("nrOfActiveInstances", "nrOfCompletedInstances", "nrOfLoops")
                .containsExactly(1, 1, 3);
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly("subTask1", procSequentialMultiInst.getId());
        assertThat(taskService.getVariable(task.getId(), "loopCounter")).isEqualTo(1);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procSequentialMultiInst, processInstance, "userTask", "userTask1Id", "subTask1", "subTask2", "subTask1");

            checkTaskInstance(procSequentialMultiInst, processInstance, "userTask1Id", "subTask1", "subTask2", "subTask1");
        }

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procSequentialMultiInst, processInstance, "userTask", "userTask1Id", "subTask1", "subTask2", "subTask1", "subTask2",
                    "subTask1", "subTask2", "afterMultiInstance");

            checkTaskInstance(procSequentialMultiInst, processInstance, "userTask1Id", "subTask1", "subTask2", "subTask1", "subTask2", "subTask1", "subTask2",
                    "afterMultiInstance");
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityToParallelMultiInstanceSubProcess() {
        ProcessDefinition procSimpleOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procParallelMultiInst = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/parallel-multi-instance-subprocess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procSimpleOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procSimpleOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly("userTask1Id", procSimpleOneTask.getId());

        //Prepare and action the migration
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procParallelMultiInst.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "parallelMISubProcess"))
                .withProcessInstanceVariable("nrOfLoops", 3);
        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();
        processInstanceMigrationBuilder.migrate(processInstance.getId());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        //MI subProcess root execution, 3 subProcesses and 3 tasks
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("parallelMISubProcess", "parallelMISubProcess", "parallelMISubProcess", "parallelMISubProcess", "subTask1", "subTask1",
                        "subTask1");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procParallelMultiInst.getId());
        assertThat(executions)
                .haveExactly(1, new Condition<>((Execution execution) -> ((ExecutionEntity) execution).isMultiInstanceRoot(), "is Multi Instance root"));
        Execution miRoot = executions.stream().filter(e -> ((ExecutionEntity) e).isMultiInstanceRoot()).findFirst().get();
        Map<String, Object> miRootVars = runtimeService.getVariables(miRoot.getId());
        assertThat(miRootVars)
                .extracting("nrOfActiveInstances", "nrOfCompletedInstances", "nrOfLoops")
                .containsExactly(3, 0, 3);
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly(tuple("subTask1", procParallelMultiInst.getId()));
        List<Integer> loopCounters = tasks.stream().map(aTask -> taskService.getVariable(aTask.getId(), "loopCounter", Integer.class))
                .collect(Collectors.toList());
        assertThat(loopCounters).containsExactlyInAnyOrder(0, 1, 2);

        //Complete the tasks with loopCounter 1
        task = tasks.stream().filter(aTask -> taskService.getVariable(aTask.getId(), "loopCounter", Integer.class) == 1).findFirst().get();
        completeTask(task);
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("subTask2").singleResult();
        completeTask(task);

        //Two subProcess Instances remain
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("parallelMISubProcess", "parallelMISubProcess", "parallelMISubProcess", "subTask1", "subTask1");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procParallelMultiInst.getId());
        assertThat(executions)
                .haveExactly(1, new Condition<>((Execution execution) -> ((ExecutionEntity) execution).isMultiInstanceRoot(), "is Multi Instance root"));
        miRoot = executions.stream().filter(e -> ((ExecutionEntity) e).isMultiInstanceRoot()).findFirst().get();
        miRootVars = runtimeService.getVariables(miRoot.getId());
        assertThat(miRootVars)
                .extracting("nrOfActiveInstances", "nrOfCompletedInstances", "nrOfLoops")
                .containsExactly(2, 1, 3);
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly(tuple("subTask1", procParallelMultiInst.getId()));
        loopCounters = tasks.stream().map(aTask -> taskService.getVariable(aTask.getId(), "loopCounter", Integer.class)).collect(Collectors.toList());
        assertThat(loopCounters).containsExactlyInAnyOrder(0, 2);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procParallelMultiInst, processInstance, "userTask", "userTask1Id", "subTask1", "subTask1", "subTask1", "subTask2");

            checkTaskInstance(procParallelMultiInst, processInstance, "userTask1Id", "subTask1", "subTask1", "subTask1", "subTask2");
        }

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procParallelMultiInst, processInstance, "userTask", "userTask1Id", "subTask1", "subTask1", "subTask1", "subTask2",
                    "subTask2", "subTask2", "afterMultiInstance");

            checkTaskInstance(procParallelMultiInst, processInstance, "userTask1Id", "subTask1", "subTask1", "subTask1", "subTask2", "subTask2", "subTask2",
                    "afterMultiInstance");
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateSequentialMultiInstanceSubProcessToSimpleActivity() {
        ProcessDefinition procSequentialMultiInst = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/sequential-multi-instance-subprocess.bpmn20.xml");
        ProcessDefinition procSimpleOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procSequentialMultiInst.getId(), Collections.singletonMap("nrOfLoops", 3));

        //Progress to the MI subProcess
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("beforeMultiInstance");
        completeTask(task);

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        //MI subProcess root execution, actual subProcess and 1 task
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("sequentialMISubProcess", "sequentialMISubProcess", "subTask1");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procSequentialMultiInst.getId());
        Execution miRoot = executions.stream().filter(e -> ((ExecutionEntity) e).isMultiInstanceRoot()).findFirst().get();
        Map<String, Object> miRootVars = runtimeService.getVariables(miRoot.getId());
        assertThat(miRootVars)
                .extracting("nrOfActiveInstances", "nrOfCompletedInstances", "nrOfLoops")
                .containsExactly(1, 0, 3);
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly("subTask1", procSequentialMultiInst.getId());
        assertThat(taskService.getVariable(task.getId(), "loopCounter")).isEqualTo(0);

        //Complete iteration...
        completeTask(task);
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("subTask2");
        completeTask(task);

        //Confirm Second iteration
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly("subTask1", procSequentialMultiInst.getId());
        assertThat(taskService.getVariable(task.getId(), "loopCounter")).isEqualTo(1);

        //Prepare and action the migration
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procSimpleOneTask.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("sequentialMISubProcess", "userTask1Id"));
        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();
        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procSimpleOneTask.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly("userTask1Id", procSimpleOneTask.getId());
        assertThat(taskService.getVariable(task.getId(), "loopCounter")).isNull();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procSimpleOneTask, processInstance, "userTask", "beforeMultiInstance", "subTask1", "subTask2", "subTask1", "userTask1Id");

            checkTaskInstance(procSimpleOneTask, processInstance, "beforeMultiInstance", "subTask1", "subTask2", "subTask1", "userTask1Id");
        }

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procSimpleOneTask, processInstance, "userTask", "beforeMultiInstance", "subTask1", "subTask2", "subTask1", "userTask1Id");

            checkTaskInstance(procSimpleOneTask, processInstance, "beforeMultiInstance", "subTask1", "subTask2", "subTask1", "userTask1Id");
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateParallelMultiInstanceSubProcessToSimpleActivity() {
        ProcessDefinition procParallelMultiInst = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/parallel-multi-instance-subprocess.bpmn20.xml");
        ProcessDefinition procSimpleOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procParallelMultiInst.getId(), Collections.singletonMap("nrOfLoops", 3));

        //Progress to the MI subProcess
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("beforeMultiInstance");
        completeTask(task);

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        //MI subProcess root execution, actual subProcess and 1 task
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("parallelMISubProcess", "parallelMISubProcess", "parallelMISubProcess", "parallelMISubProcess", "subTask1", "subTask1",
                        "subTask1");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procParallelMultiInst.getId());
        Execution miRoot = executions.stream().filter(e -> ((ExecutionEntity) e).isMultiInstanceRoot()).findFirst().get();
        Map<String, Object> miRootVars = runtimeService.getVariables(miRoot.getId());
        assertThat(miRootVars)
                .extracting("nrOfActiveInstances", "nrOfCompletedInstances", "nrOfLoops")
                .containsExactly(3, 0, 3);
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsOnly("subTask1");
        assertThat(tasks)
                .extracting(Task::getProcessDefinitionId)
                .containsOnly(procParallelMultiInst.getId());
        List<Integer> loopCounters = tasks.stream().map(aTask -> taskService.getVariable(aTask.getId(), "loopCounter", Integer.class))
                .collect(Collectors.toList());
        assertThat(loopCounters).containsExactlyInAnyOrder(0, 1, 2);

        //Complete the iteration with loopCounter 1
        task = tasks.stream().filter(aTask -> taskService.getVariable(aTask.getId(), "loopCounter", Integer.class) == 1).findFirst().get();
        completeTask(task);
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("subTask2").singleResult();
        completeTask(task);

        //Two subProcess Instances remain
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("parallelMISubProcess", "parallelMISubProcess", "parallelMISubProcess", "subTask1", "subTask1");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procParallelMultiInst.getId());
        assertThat(executions)
                .haveExactly(1, new Condition<>((Execution execution) -> ((ExecutionEntity) execution).isMultiInstanceRoot(), "is Multi Instance root"));
        miRoot = executions.stream().filter(e -> ((ExecutionEntity) e).isMultiInstanceRoot()).findFirst().get();
        miRootVars = runtimeService.getVariables(miRoot.getId());
        assertThat(miRootVars)
                .extracting("nrOfActiveInstances", "nrOfCompletedInstances", "nrOfLoops")
                .containsExactly(2, 1, 3);
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly(tuple("subTask1", procParallelMultiInst.getId()));
        loopCounters = tasks.stream().map(aTask -> taskService.getVariable(aTask.getId(), "loopCounter", Integer.class)).collect(Collectors.toList());
        assertThat(loopCounters).containsExactlyInAnyOrder(0, 2);

        //Complete one of the tasks
        completeTask(tasks.get(0));
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("subTask1", "subTask2");

        //Prepare and action the migration
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procSimpleOneTask.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("parallelMISubProcess", "userTask1Id"));
        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();
        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procSimpleOneTask.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly("userTask1Id", procSimpleOneTask.getId());
        assertThat(taskService.getVariable(task.getId(), "loopCounter")).isNull();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procSimpleOneTask, processInstance, "userTask", "beforeMultiInstance", "subTask1", "subTask1", "subTask1", "subTask2",
                    "subTask2", "userTask1Id");

            checkTaskInstance(procSimpleOneTask, processInstance, "beforeMultiInstance", "subTask1", "subTask1", "subTask1", "subTask2", "subTask2",
                    "userTask1Id");
        }

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procSimpleOneTask, processInstance, "userTask", "beforeMultiInstance", "subTask1", "subTask1", "subTask1", "subTask2",
                    "subTask2", "userTask1Id");

            checkTaskInstance(procSimpleOneTask, processInstance, "beforeMultiInstance", "subTask1", "subTask1", "subTask1", "subTask2", "subTask2",
                    "userTask1Id");
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Disabled("Not supported - Cannot migrate to an arbitrary activity inside an MI subProcess")
    public void testMigrateSimpleActivityToActivityInsideMultiInstanceSubProcess() {

    }

    @Test
    @Disabled("Not supported - Cannot migrate to an arbitrary activity inside an MI subProcess")
    public void testMigrateSimpleActivityToMultiInstanceSubProcessNestedInsideMultiInstanceSubProcess() {

    }

    @Test
    @Disabled("Not supported - Cannot migrate to an arbitrary activity inside an MI subProcess")
    public void testMigrateMultiInstanceSubProcessActivityToNestedMultiInstanceSubProcessActivity() {
    }

    @Test
    public void testMigrateNestedSequentialMultiInstanceSubProcessActivityToOuterSequentialSubProcessActivity() {
        ProcessDefinition procNestedSequentialMultiInst = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/nested-sequential-multi-instance-subprocesses.bpmn20.xml");
        ProcessDefinition procSequentialMultiInst = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/sequential-multi-instance-subprocess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procNestedSequentialMultiInst.getId());
        runtimeService.setVariables(processInstance.getId(), Collections.singletonMap("nrOfLoops", 2));
        runtimeService.setVariables(processInstance.getId(), Collections.singletonMap("nestedNrOfLoops", 3));

        //Progress to the MI subProcess
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("beforeMultiInstance");
        completeTask(task);
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("subTask1");
        completeTask(task);

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        //MI subProcess root execution, actual subProcess and 1 task
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("sequentialMISubProcess", "sequentialMISubProcess", "nestedSequentialMISubProcess", "nestedSequentialMISubProcess",
                        "nestedSubTask1");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procNestedSequentialMultiInst.getId());
        assertThat(executions).haveExactly(2, new Condition<>(execution -> ((ExecutionEntity) execution).isMultiInstanceRoot(), "is MultiInstance root"));
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly("nestedSubTask1", procNestedSequentialMultiInst.getId());
        assertThat(taskService.getVariable(task.getId(), "loopCounter")).isEqualTo(0);
        completeTask(task);
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("nestedSubTask2");
        completeTask(task);
        //Confirm one iteration of the nested MI subProcess is completed
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly("nestedSubTask1", procNestedSequentialMultiInst.getId());
        assertThat(taskService.getVariable(task.getId(), "loopCounter")).isEqualTo(1);

        //Prepare and action the migration
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procSequentialMultiInst.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("nestedSequentialMISubProcess", "sequentialMISubProcess"));
        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();
        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        //MI subProcess root execution, actual subProcess and 1 task
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("sequentialMISubProcess", "sequentialMISubProcess", "subTask1");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procSequentialMultiInst.getId());
        Execution miRoot = executions.stream().filter(e -> ((ExecutionEntity) e).isMultiInstanceRoot()).findFirst().get();
        Map<String, Object> miRootVars = runtimeService.getVariables(miRoot.getId());
        assertThat(miRootVars)
                .extracting("nrOfActiveInstances", "nrOfCompletedInstances", "nrOfLoops")
                .containsExactly(1, 0, 2);
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly("subTask1", procSequentialMultiInst.getId());
        assertThat(taskService.getVariable(task.getId(), "loopCounter")).isEqualTo(0);

        //Complete one iteration
        completeTask(task);
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("subTask2");
        completeTask(task);

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        //MI subProcess root execution, actual subProcess and 1 task
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("sequentialMISubProcess", "sequentialMISubProcess", "subTask1");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procSequentialMultiInst.getId());
        miRoot = executions.stream().filter(e -> ((ExecutionEntity) e).isMultiInstanceRoot()).findFirst().get();
        miRootVars = runtimeService.getVariables(miRoot.getId());
        assertThat(miRootVars)
                .extracting("nrOfActiveInstances", "nrOfCompletedInstances", "nrOfLoops")
                .containsExactly(1, 1, 2);
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly("subTask1", procSequentialMultiInst.getId());
        assertThat(taskService.getVariable(task.getId(), "loopCounter")).isEqualTo(1);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procSequentialMultiInst, processInstance, "userTask", "beforeMultiInstance", "subTask1", "nestedSubTask1", "nestedSubTask2",
                    "nestedSubTask1", "subTask1", "subTask2", "subTask1");

            checkTaskInstance(procSequentialMultiInst, processInstance, "beforeMultiInstance", "subTask1", "nestedSubTask1", "nestedSubTask2", "nestedSubTask1",
                    "subTask1", "subTask2", "subTask1");
        }

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procSequentialMultiInst, processInstance, "userTask", "beforeMultiInstance", "subTask1", "nestedSubTask1", "nestedSubTask2",
                    "nestedSubTask1", "subTask1", "subTask2", "subTask1", "subTask2", "afterMultiInstance");

            checkTaskInstance(procSequentialMultiInst, processInstance, "beforeMultiInstance", "subTask1", "nestedSubTask1", "nestedSubTask2", "nestedSubTask1",
                    "subTask1", "subTask2", "subTask1", "subTask2", "afterMultiInstance");
        }

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            List<HistoricActivityInstance> taskExecutions = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .activityType("userTask")
                    .list();
            assertThat(taskExecutions)
                    .extracting(HistoricActivityInstance::getActivityId)
                    .containsExactlyInAnyOrder("beforeMultiInstance", "subTask1", "nestedSubTask1", "nestedSubTask2", "nestedSubTask1", "subTask1", "subTask2",
                            "subTask1", "subTask2", "afterMultiInstance");
            assertThat(taskExecutions)
                    .extracting(HistoricActivityInstance::getProcessDefinitionId)
                    .containsOnly(procSequentialMultiInst.getId());

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                        .processInstanceId(processInstance.getId())
                        .list();
                assertThat(historicTasks)
                        .extracting(HistoricTaskInstance::getTaskDefinitionKey)
                        .containsExactlyInAnyOrder("beforeMultiInstance", "subTask1", "nestedSubTask1", "nestedSubTask2", "nestedSubTask1", "subTask1",
                                "subTask2", "subTask1", "subTask2", "afterMultiInstance");
                assertThat(historicTasks)
                        .extracting(HistoricTaskInstance::getProcessDefinitionId)
                        .containsOnly(procSequentialMultiInst.getId());
            }
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateNestedParallelMultiInstanceSubProcessActivityToOuterParallelSubProcessActivity() {
        ProcessDefinition procNestedParallelMultiInst = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/nested-parallel-multi-instance-subprocesses.bpmn20.xml");
        ProcessDefinition procParallelMultiInst = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/parallel-multi-instance-subprocess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procNestedParallelMultiInst.getId());
        runtimeService.setVariables(processInstance.getId(), Collections.singletonMap("nrOfLoops", 2));
        runtimeService.setVariables(processInstance.getId(), Collections.singletonMap("nestedNrOfLoops", 3));

        //Progress to the MI subProcess
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey)
                .isEqualTo("beforeMultiInstance");
        completeTask(task);
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsOnly("subTask1");
        tasks.forEach(this::completeTask);

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        //MI subProcess root execution, actual subProcess and 1 task
        assertThat(executions).hasSize(17);
        assertThat(executions).haveExactly(3, new Condition<>(execution -> "parallelMISubProcess".equals(execution.getActivityId()), "Outer MI SubProcess"));
        assertThat(executions).haveExactly(1,
                new Condition<>(execution -> "parallelMISubProcess".equals(execution.getActivityId()) && ((ExecutionEntity) execution).isMultiInstanceRoot(),
                        "Outer MI SubProcess roots"));
        assertThat(executions)
                .haveExactly(8, new Condition<>(execution -> "nestedParallelMISubProcess".equals(execution.getActivityId()), "Nested MI SubProcess"));
        assertThat(executions).haveExactly(2, new Condition<>(
                execution -> "nestedParallelMISubProcess".equals(execution.getActivityId()) && ((ExecutionEntity) execution).isMultiInstanceRoot(),
                "Nested MI SubProcess roots"));
        assertThat(executions).haveExactly(6, new Condition<>(execution -> "nestedSubTask1".equals(execution.getActivityId()), "Task Executions"));
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procNestedParallelMultiInst.getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).hasSize(6);
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly(tuple("nestedSubTask1", procNestedParallelMultiInst.getId()));

        //Prepare and action the migration
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procParallelMultiInst.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("nestedParallelMISubProcess", "parallelMISubProcess"));
        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();
        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //IMPORTANT: Might not be the expected result, but it is currently the correct one, as there are two MI roots (nestedParallelMISubProcess) migrating independently
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).hasSize(10);
        assertThat(executions)
                .haveExactly(6, new Condition<>(execution -> "parallelMISubProcess".equals(execution.getActivityId()), "Outer MI SubProcess"));
        assertThat(executions)
                .haveExactly(2, new Condition<>(
                        execution -> "parallelMISubProcess".equals(execution.getActivityId()) && ((ExecutionEntity) execution).isMultiInstanceRoot(),
                        "Outer MI SubProcess roots"));
        assertThat(executions)
                .haveExactly(4, new Condition<>(execution -> "subTask1".equals(execution.getActivityId()), "Task Executions"));
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procParallelMultiInst.getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).hasSize(4);
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly(tuple("subTask1", procParallelMultiInst.getId()));

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            List<HistoricActivityInstance> taskExecutions = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .activityType("userTask")
                    .list();
            assertThat(taskExecutions)
                    .extracting(HistoricActivityInstance::getActivityId)
                    .haveAtMost(1, new Condition<>((String s) -> "beforeMultiInstance".equals(s), "beforeMultiInstance completed task"));
            assertThat(taskExecutions)
                    .extracting(HistoricActivityInstance::getActivityId)
                    .haveAtMost(6, new Condition<>((String s) -> "nestedSubTask1".equals(s), "nestedSubTask1 completed task"));
            assertThat(taskExecutions)
                    .extracting(HistoricActivityInstance::getActivityId)
                    .haveAtMost(6, new Condition<>((String s) -> "subTask1".equals(s), "subTask1 completed task"));
            assertThat(taskExecutions)
                    .extracting(HistoricActivityInstance::getActivityId)
                    .haveAtMost(4, new Condition<>((String s) -> "subTask2".equals(s), "subTask2 completed task"));
            assertThat(taskExecutions)
                    .extracting(HistoricActivityInstance::getActivityId)
                    .haveAtMost(2, new Condition<>((String s) -> "afterMultiInstance".equals(s), "afterMultiInstance completed task"));
            assertThat(taskExecutions)
                    .extracting(HistoricActivityInstance::getProcessDefinitionId)
                    .containsOnly(procParallelMultiInst.getId());

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                        .processInstanceId(processInstance.getId())
                        .list();
                assertThat(historicTasks)
                        .extracting(HistoricTaskInstance::getTaskDefinitionKey)
                        .haveAtMost(1, new Condition<>((String s) -> "beforeMultiInstance".equals(s), "beforeMultiInstance completed task"));
                assertThat(historicTasks)
                        .extracting(HistoricTaskInstance::getTaskDefinitionKey)
                        .haveAtMost(6, new Condition<>((String s) -> "nestedSubTask1".equals(s), "nestedSubTask1 completed task"));
                assertThat(historicTasks)
                        .extracting(HistoricTaskInstance::getTaskDefinitionKey)
                        .haveAtMost(6, new Condition<>((String s) -> "subTask1".equals(s), "subTask1 completed task"));
                assertThat(historicTasks)
                        .extracting(HistoricTaskInstance::getTaskDefinitionKey)
                        .haveAtMost(4, new Condition<>((String s) -> "subTask2".equals(s), "subTask2 completed task"));
                assertThat(historicTasks)
                        .extracting(HistoricTaskInstance::getTaskDefinitionKey)
                        .haveAtMost(2, new Condition<>((String s) -> "afterMultiInstance".equals(s), "afterMultiInstance completed task"));
                assertThat(historicTasks)
                        .extracting(HistoricTaskInstance::getProcessDefinitionId)
                        .containsOnly(procParallelMultiInst.getId());
            }
        }

        assertProcessEnded(processInstance.getId());
    }
}
