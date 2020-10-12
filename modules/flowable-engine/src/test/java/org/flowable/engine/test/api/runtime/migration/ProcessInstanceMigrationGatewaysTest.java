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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.migration.ActivityMigrationMapping;
import org.flowable.engine.migration.ProcessInstanceMigrationBuilder;
import org.flowable.engine.migration.ProcessInstanceMigrationValidationResult;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @author Dennis Federico
 */
public class ProcessInstanceMigrationGatewaysTest extends AbstractProcessInstanceMigrationTest {

    @AfterEach
    protected void tearDown() {
        deleteDeployments();
    }

    //-- Exclusive Gateway
    @Test
    public void testMigrateActivityToExclusiveGatewayStartWithDefaultFlow() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procWithExcGtw = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/exclusive-gateway-with-default-sequence.bpmn20.xml");

        //Start the processInstance without timer
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly("userTask1Id", procDefOneTask.getId());

        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procWithExcGtw.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "exclusiveGw"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();
        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("defaultFlowTask");
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("defaultFlowTask");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            checkActivityInstances(procWithExcGtw, processInstance, "userTask", "userTask1Id", "defaultFlowTask");
            checkActivityInstances(procWithExcGtw, processInstance, "exclusiveGateway", "exclusiveGw");
            checkTaskInstance(procWithExcGtw, processInstance, "userTask1Id", "defaultFlowTask");
        }

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            checkActivityInstances(procWithExcGtw, processInstance, "userTask", "userTask1Id", "defaultFlowTask");
            checkActivityInstances(procWithExcGtw, processInstance, "exclusiveGateway", "exclusiveGw");
            checkTaskInstance(procWithExcGtw, processInstance, "userTask1Id", "defaultFlowTask");
        }
    }

    @Test
    public void testMigrateActivityToExclusiveGatewayStartWithConditionSpecifiedBeforeMigration() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procWithExcGtw = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/exclusive-gateway-with-default-sequence.bpmn20.xml");

        //Start the processInstance without timer
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly("userTask1Id", procDefOneTask.getId());

        runtimeService.setVariable(executions.get(0).getId(), "input", 1);

        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procWithExcGtw.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "exclusiveGw"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("theTask1");
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("theTask1");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            checkActivityInstances(procWithExcGtw, processInstance, "userTask", "userTask1Id", "theTask1");
            checkActivityInstances(procWithExcGtw, processInstance, "exclusiveGateway", "exclusiveGw");

            checkTaskInstance(procWithExcGtw, processInstance, "userTask1Id", "theTask1");
        }

        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityToExclusiveGatewayStartWithConditionSpecifiedDuringMigration() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procWithExcGtw = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/exclusive-gateway-with-default-sequence.bpmn20.xml");

        //Start the processInstance without timer
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly("userTask1Id", procDefOneTask.getId());

        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procWithExcGtw.getId())
                .addActivityMigrationMapping(
                        ActivityMigrationMapping.createMappingFor("userTask1Id", "exclusiveGw").withLocalVariable("input", Integer.valueOf(1)));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("theTask1");
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("theTask1");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            checkActivityInstances(procWithExcGtw, processInstance, "userTask", "userTask1Id", "theTask1");
            checkActivityInstances(procWithExcGtw, processInstance, "exclusiveGateway", "exclusiveGw");

            checkTaskInstance(procWithExcGtw, processInstance, "userTask1Id", "theTask1");
        }

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());
        assertProcessEnded(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            checkActivityInstances(procWithExcGtw, processInstance, "userTask", "userTask1Id", "theTask1");
            checkActivityInstances(procWithExcGtw, processInstance, "exclusiveGateway", "exclusiveGw");

            checkTaskInstance(procWithExcGtw, processInstance, "userTask1Id", "theTask1");
        }

    }

    @Test
    public void testMigrateActivityToExclusiveGatewayStartWithConditionSpecifiedAsProcessVariableDuringMigration() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procWithExcGtw = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/exclusive-gateway-with-default-sequence.bpmn20.xml");

        //Start the processInstance without timer
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly("userTask1Id", procDefOneTask.getId());

        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procWithExcGtw.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "exclusiveGw"))
                .withProcessInstanceVariable("input", 1);

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("theTask1");
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("theTask1");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            checkActivityInstances(procWithExcGtw, processInstance, "userTask", "userTask1Id", "theTask1");
            checkActivityInstances(procWithExcGtw, processInstance, "exclusiveGateway", "exclusiveGw");

            checkTaskInstance(procWithExcGtw, processInstance, "userTask1Id", "theTask1");
        }

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            checkActivityInstances(procWithExcGtw, processInstance, "userTask", "userTask1Id", "theTask1");
            checkActivityInstances(procWithExcGtw, processInstance, "exclusiveGateway", "exclusiveGw");

            checkTaskInstance(procWithExcGtw, processInstance, "userTask1Id", "theTask1");
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityToActivityInsideExclusiveGateway() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procWithExcGtw = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/exclusive-gateway-with-default-sequence.bpmn20.xml");

        //Start the processInstance without timer
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly("userTask1Id", procDefOneTask.getId());

        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procWithExcGtw.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "theTask2"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("theTask2");
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("theTask2");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            checkActivityInstances(procWithExcGtw, processInstance, "userTask", "theTask2");

            List<HistoricActivityInstance> gtwExecution = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .activityType("exclusiveGateway")
                    .list();
            assertThat(gtwExecution).isEmpty();

            checkTaskInstance(procWithExcGtw, processInstance, "theTask2");
        }

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            checkActivityInstances(procWithExcGtw, processInstance, "userTask", "theTask2");

            List<HistoricActivityInstance> gtwExecution = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .activityType("exclusiveGateway")
                    .list();
            assertThat(gtwExecution).isEmpty();

            checkTaskInstance(procWithExcGtw, processInstance, "theTask2");
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityInsideExclusiveGatewayToActivityOutside() {
        ProcessDefinition procWithExcGtw = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/exclusive-gateway-with-default-sequence.bpmn20.xml");
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");

        //Start the processInstance without timer
        Map<String, Object> gtwCondition = Collections.singletonMap("input", Integer.valueOf(2));
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procWithExcGtw.getId(), gtwCondition);

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("theTask2");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procWithExcGtw.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly("theTask2", procWithExcGtw.getId());

        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefOneTask.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("theTask2", "userTask1Id"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            checkActivityInstances(procDefOneTask, processInstance, "userTask", "userTask1Id");
            checkActivityInstances(procDefOneTask, processInstance, "exclusiveGateway", "exclusiveGw");

            checkTaskInstance(procDefOneTask, processInstance, "userTask1Id");
        }

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            checkActivityInstances(procDefOneTask, processInstance, "userTask", "userTask1Id");
            checkActivityInstances(procDefOneTask, processInstance, "exclusiveGateway", "exclusiveGw");

            checkTaskInstance(procDefOneTask, processInstance, "userTask1Id");
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityToExclusiveGatewayStartInsideEmbeddedSubProcessDefaultFlow() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procWithExcGtw = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/exclusive-gateway-with-default-sequence-inside-embedded-subprocess.bpmn20.xml");

        //Start the processInstance without timer
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly("userTask1Id", procDefOneTask.getId());

        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procWithExcGtw.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "exclusiveGw"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("theSubProcess", "defaultFlowTask");
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("defaultFlowTask");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            checkActivityInstances(procWithExcGtw, processInstance, "userTask", "userTask1Id", "defaultFlowTask");
            checkActivityInstances(procWithExcGtw, processInstance, "subProcess", "theSubProcess");
            checkActivityInstances(procWithExcGtw, processInstance, "exclusiveGateway", "exclusiveGw");

            checkTaskInstance(procWithExcGtw, processInstance, "userTask1Id", "defaultFlowTask");
        }

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            checkActivityInstances(procWithExcGtw, processInstance, "userTask", "userTask1Id", "defaultFlowTask", "afterSubProcessTask");
            checkActivityInstances(procWithExcGtw, processInstance, "subProcess", "theSubProcess");
            checkActivityInstances(procWithExcGtw, processInstance, "exclusiveGateway", "exclusiveGw");

            checkTaskInstance(procWithExcGtw, processInstance, "userTask1Id", "defaultFlowTask", "afterSubProcessTask");
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityToExclusiveGatewayStartInsideEmbeddedSubProcess() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procWithExcGtw = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/exclusive-gateway-with-default-sequence-inside-embedded-subprocess.bpmn20.xml");

        //Start the processInstance without timer
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly("userTask1Id", procDefOneTask.getId());

        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procWithExcGtw.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "exclusiveGw")
                        .withLocalVariables(Collections.singletonMap("input", Integer.valueOf(1))));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("theSubProcess", "theTask1");
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("theTask1");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            checkActivityInstances(procWithExcGtw, processInstance, "userTask", "userTask1Id", "theTask1");
            checkActivityInstances(procWithExcGtw, processInstance, "subProcess", "theSubProcess");
            checkActivityInstances(procWithExcGtw, processInstance, "exclusiveGateway", "exclusiveGw");

            checkTaskInstance(procWithExcGtw, processInstance, "userTask1Id", "theTask1");
        }

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            checkActivityInstances(procWithExcGtw, processInstance, "userTask", "userTask1Id", "theTask1", "afterSubProcessTask");
            checkActivityInstances(procWithExcGtw, processInstance, "subProcess", "theSubProcess");
            checkActivityInstances(procWithExcGtw, processInstance, "exclusiveGateway", "exclusiveGw");

            checkTaskInstance(procWithExcGtw, processInstance, "userTask1Id", "theTask1", "afterSubProcessTask");
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityToActivityInsideExclusiveGatewayInsideEmbeddedSubProcess() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procWithExcGtw = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/exclusive-gateway-with-default-sequence-inside-embedded-subprocess.bpmn20.xml");

        //Start the processInstance without timer
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly("userTask1Id", procDefOneTask.getId());

        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procWithExcGtw.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "theTask2"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("theSubProcess", "theTask2");
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("theTask2");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            checkActivityInstances(procWithExcGtw, processInstance, "userTask", "theTask2");
            checkActivityInstances(procWithExcGtw, processInstance, "subProcess", "theSubProcess");
            List<HistoricActivityInstance> gtwExecution = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .activityType("exclusiveGateway")
                    .list();
            assertThat(gtwExecution).isEmpty();

            checkTaskInstance(procWithExcGtw, processInstance, "theTask2");
        }

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            checkActivityInstances(procWithExcGtw, processInstance, "userTask", "theTask2", "afterSubProcessTask");
            checkActivityInstances(procWithExcGtw, processInstance, "subProcess", "theSubProcess");
            List<HistoricActivityInstance> gtwExecution = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .activityType("exclusiveGateway")
                    .list();
            assertThat(gtwExecution).isEmpty();

            checkTaskInstance(procWithExcGtw, processInstance, "theTask2", "afterSubProcessTask");
        }

        assertProcessEnded(processInstance.getId());
    }

    //-- Parallel Gateway
    @Test
    public void testMigrateActivityToParallelGatewayFork() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procParallelGtw = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/parallel-gateway-two-splits-four-tasks.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly("userTask1Id", procDefOneTask.getId());

        //Migrate each of the parallel task to a task in the parallel gateway
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procParallelGtw.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "parallelFork"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("oddFlowTask1", "evenFlowTask2");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procParallelGtw.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("oddFlowTask1", "evenFlowTask2");
        assertThat(tasks)
                .extracting(Task::getProcessDefinitionId)
                .containsOnly(procParallelGtw.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            checkActivityInstances(procParallelGtw, processInstance, "userTask", "userTask1Id", "oddFlowTask1", "evenFlowTask2");
            checkActivityInstances(procParallelGtw, processInstance, "parallelGateway", "parallelFork");

            checkTaskInstance(procParallelGtw, processInstance, "userTask1Id", "oddFlowTask1", "evenFlowTask2");
        }

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            checkActivityInstances(procParallelGtw, processInstance, "userTask", "userTask1Id", "oddFlowTask1", "oddFlowTask3", "evenFlowTask2",
                    "evenFlowTask4", "taskAfter");
            checkActivityInstances(procParallelGtw, processInstance, "parallelGateway", "parallelFork", "parallelJoin", "parallelJoin");

            checkTaskInstance(procParallelGtw, processInstance, "userTask1Id", "oddFlowTask1", "oddFlowTask3", "evenFlowTask2", "evenFlowTask4", "taskAfter");
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateParallelActivitiesToParallelGatewayActivities() {
        ProcessDefinition procParallelTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/timer-parallel-task.bpmn20.xml");
        ProcessDefinition procParallelGtw = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/parallel-gateway-two-splits-four-tasks.bpmn20.xml");

        //Start the processInstance and spawn the parallel task
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procParallelTask.getId());

        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        //Confirm the state to migrate
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
        assertThat(tasks).
                extracting(Task::getProcessDefinitionId)
                .containsOnly(procParallelTask.getId());

        //Migrate each of the parallel task to a task in the parallel gateway
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procParallelGtw.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("processTask", "oddFlowTask1"))
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("parallelTask", "evenFlowTask4"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("oddFlowTask1", "evenFlowTask4");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procParallelGtw.getId());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("oddFlowTask1", "evenFlowTask4");
        assertThat(tasks)
                .extracting(Task::getProcessDefinitionId)
                .containsOnly(procParallelGtw.getId());

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            List<HistoricActivityInstance> subProcesses = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .activityType("userTask")
                    .list();
            //Direct Migration
            assertThat(subProcesses)
                    .extracting(HistoricActivityInstance::getActivityId)
                    .containsExactlyInAnyOrder("oddFlowTask1", "oddFlowTask3", "evenFlowTask4", "taskAfter");
            assertThat(subProcesses)
                    .extracting(HistoricActivityInstance::getProcessDefinitionId)
                    .containsOnly(procParallelGtw.getId());

            List<HistoricActivityInstance> gtwExecution = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .activityType("parallelGateway")
                    .list();
            //Two flows to join in, fork was not executed
            assertThat(gtwExecution)
                    .extracting(HistoricActivityInstance::getActivityId)
                    .containsExactlyInAnyOrder("parallelJoin", "parallelJoin");
            assertThat(gtwExecution)
                    .extracting(HistoricActivityInstance::getProcessDefinitionId)
                    .containsOnly(procParallelGtw.getId());

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                        .processInstanceId(processInstance.getId())
                        .list();
                assertThat(historicTasks)
                        .extracting(HistoricTaskInstance::getTaskDefinitionKey)
                        .containsExactlyInAnyOrder("oddFlowTask1", "oddFlowTask3", "evenFlowTask4", "taskAfter");
                assertThat(historicTasks)
                        .extracting(HistoricTaskInstance::getProcessDefinitionId)
                        .containsOnly(procParallelGtw.getId());
            }
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityToParallelGatewayActivities() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procParallelGtw = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/parallel-gateway-two-splits-four-tasks.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly("userTask1Id", procDefOneTask.getId());

        //Migrate each of the parallel task to a task in the parallel gateway
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procParallelGtw.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", Arrays.asList("oddFlowTask3", "evenFlowTask2")));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("oddFlowTask3", "evenFlowTask2");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procParallelGtw.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("oddFlowTask3", "evenFlowTask2");
        assertThat(tasks)
                .extracting(Task::getProcessDefinitionId)
                .containsOnly(procParallelGtw.getId());

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            List<HistoricActivityInstance> subProcesses = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .activityType("userTask")
                    .list();
            assertThat(subProcesses)
                    .extracting(HistoricActivityInstance::getActivityId)
                    .containsExactlyInAnyOrder("userTask1Id", "oddFlowTask3", "evenFlowTask2", "evenFlowTask4", "taskAfter");
            assertThat(subProcesses)
                    .extracting(HistoricActivityInstance::getProcessDefinitionId)
                    .containsOnly(procParallelGtw.getId());

            List<HistoricActivityInstance> gtwExecution = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .activityType("parallelGateway")
                    .list();
            //Two flows to join in, fork was not executed
            assertThat(gtwExecution)
                    .extracting(HistoricActivityInstance::getActivityId)
                    .containsExactlyInAnyOrder("parallelJoin", "parallelJoin");
            assertThat(gtwExecution)
                    .extracting(HistoricActivityInstance::getProcessDefinitionId)
                    .containsOnly(procParallelGtw.getId());

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                        .processInstanceId(processInstance.getId())
                        .list();
                assertThat(historicTasks)
                        .extracting(HistoricTaskInstance::getTaskDefinitionKey)
                        .containsExactlyInAnyOrder("userTask1Id", "oddFlowTask3", "evenFlowTask2", "evenFlowTask4", "taskAfter");
                assertThat(historicTasks)
                        .extracting(HistoricTaskInstance::getProcessDefinitionId)
                        .containsOnly(procParallelGtw.getId());
            }
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityToParallelGatewayActivitiesIncompleteActivityMapping() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procParallelGtw = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/parallel-gateway-three-splits.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly("userTask1Id", procDefOneTask.getId());

        //Migrate each of the parallel task to a task in the parallel gateway
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procParallelGtw.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", Arrays.asList("task1", "task3")));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("task1", "task3");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procParallelGtw.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("task1", "task3");
        assertThat(tasks)
                .extracting(Task::getProcessDefinitionId)
                .containsOnly(procParallelGtw.getId());

        //Try to complete the process
        completeProcessInstanceTasks(processInstance.getId());

        //Parallel Gateway join is not satisfied since there is a missing sequence flow
        List<Execution> parallelJoinExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(parallelJoinExecutions)
                .extracting(Execution::getActivityId)
                .containsOnly("parallelJoin");
    }

    @Test
    public void testMigrateActivityToParallelGatewayForkInsideEmbeddedSubProcess() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procParallelGtw = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/parallel-gateway-nested-in-embedded-subprocess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly("userTask1Id", procDefOneTask.getId());

        //Migrate each of the parallel task to a task in the parallel gateway
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procParallelGtw.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "parallelFork"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("subProcess", "oddFlowTask1", "evenFlowTask2");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procParallelGtw.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("oddFlowTask1", "evenFlowTask2");
        assertThat(tasks)
                .extracting(Task::getProcessDefinitionId)
                .containsOnly(procParallelGtw.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            checkActivityInstances(procParallelGtw, processInstance, "userTask", "userTask1Id", "oddFlowTask1", "evenFlowTask2");
            checkActivityInstances(procParallelGtw, processInstance, "subProcess", "subProcess");
            checkActivityInstances(procParallelGtw, processInstance, "parallelGateway", "parallelFork");

            checkTaskInstance(procParallelGtw, processInstance, "userTask1Id", "oddFlowTask1", "evenFlowTask2");
        }

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            checkActivityInstances(procParallelGtw, processInstance, "userTask", "userTask1Id", "oddFlowTask1", "oddFlowTask3", "evenFlowTask2",
                    "evenFlowTask4", "taskAfter");
            checkActivityInstances(procParallelGtw, processInstance, "subProcess", "subProcess");
            checkActivityInstances(procParallelGtw, processInstance, "parallelGateway", "parallelFork", "parallelJoin", "parallelJoin");

            List<HistoricActivityInstance> subProcExecution = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .activityType("subProcess")
                    .list();
            assertThat(subProcExecution)
                    .extracting(HistoricActivityInstance::getActivityId, HistoricActivityInstance::getProcessDefinitionId)
                    .containsExactly(tuple("subProcess", procParallelGtw.getId()));

            List<HistoricActivityInstance> gtwExecution = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .activityType("parallelGateway")
                    .list();
            //Two flows to join in
            assertThat(gtwExecution)
                    .extracting(HistoricActivityInstance::getActivityId)
                    .containsExactlyInAnyOrder("parallelFork", "parallelJoin", "parallelJoin");
            assertThat(gtwExecution)
                    .extracting(HistoricActivityInstance::getProcessDefinitionId)
                    .containsOnly(procParallelGtw.getId());

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                        .processInstanceId(processInstance.getId())
                        .list();
                assertThat(historicTasks)
                        .extracting(HistoricTaskInstance::getTaskDefinitionKey)
                        .containsExactlyInAnyOrder("userTask1Id", "oddFlowTask1", "oddFlowTask3", "evenFlowTask2", "evenFlowTask4", "taskAfter");
                assertThat(historicTasks)
                        .extracting(HistoricTaskInstance::getProcessDefinitionId)
                        .containsOnly(procParallelGtw.getId());
            }
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateParallelActivitiesToParallelGatewayActivitiesInsideEmbeddedSubProcess() {
        ProcessDefinition procParallelTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/timer-parallel-task.bpmn20.xml");
        ProcessDefinition procParallelGtw = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/parallel-gateway-nested-in-embedded-subprocess.bpmn20.xml");

        //Start the processInstance and spawn the parallel task
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procParallelTask.getId());

        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        //Confirm the state to migrate
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

        //Migrate each of the parallel task to a task in the parallel gateway
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procParallelGtw.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("processTask", "oddFlowTask1"))
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("parallelTask", "evenFlowTask4"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("subProcess", "oddFlowTask1", "evenFlowTask4");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procParallelGtw.getId());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("oddFlowTask1", "evenFlowTask4");
        assertThat(tasks)
                .extracting(Task::getProcessDefinitionId)
                .containsOnly(procParallelGtw.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procParallelGtw, processInstance, "userTask", "oddFlowTask1", "evenFlowTask4");
            checkActivityInstances(procParallelGtw, processInstance, "subProcess", "subProcess");

            checkTaskInstance(procParallelGtw, processInstance, "oddFlowTask1", "evenFlowTask4");
        }

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procParallelGtw, processInstance, "userTask", "oddFlowTask1", "oddFlowTask3", "evenFlowTask4", "taskAfter");
            checkActivityInstances(procParallelGtw, processInstance, "subProcess", "subProcess");
            checkActivityInstances(procParallelGtw, processInstance, "parallelGateway", "parallelJoin", "parallelJoin");

            checkTaskInstance(procParallelGtw, processInstance, "oddFlowTask1", "oddFlowTask3", "evenFlowTask4", "taskAfter");

            List<HistoricActivityInstance> subProcExecution = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .activityType("subProcess")
                    .list();
            assertThat(subProcExecution)
                    .extracting(HistoricActivityInstance::getActivityId, HistoricActivityInstance::getProcessDefinitionId)
                    .containsExactly(tuple("subProcess", procParallelGtw.getId()));

            List<HistoricActivityInstance> gtwExecution = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .activityType("parallelGateway")
                    .list();
            //Two flows to join in, fork was not executed
            assertThat(gtwExecution)
                    .extracting(HistoricActivityInstance::getActivityId)
                    .containsExactlyInAnyOrder("parallelJoin", "parallelJoin");
            assertThat(gtwExecution)
                    .extracting(HistoricActivityInstance::getProcessDefinitionId)
                    .containsOnly(procParallelGtw.getId());

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                        .processInstanceId(processInstance.getId())
                        .list();
                assertThat(historicTasks)
                        .extracting(HistoricTaskInstance::getTaskDefinitionKey)
                        .containsExactlyInAnyOrder("oddFlowTask1", "oddFlowTask3", "evenFlowTask4", "taskAfter");
                assertThat(historicTasks)
                        .extracting(HistoricTaskInstance::getProcessDefinitionId)
                        .containsOnly(procParallelGtw.getId());
            }
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityToParallelGatewayActivitiesInsideEmbeddedSubProcess() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procParallelGtw = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/parallel-gateway-nested-in-embedded-subprocess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly("userTask1Id", procDefOneTask.getId());

        //Migrate each of the parallel task to a task in the parallel gateway
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procParallelGtw.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", Arrays.asList("oddFlowTask3", "evenFlowTask2")));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("subProcess", "oddFlowTask3", "evenFlowTask2");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procParallelGtw.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("oddFlowTask3", "evenFlowTask2");
        assertThat(tasks)
                .extracting(Task::getProcessDefinitionId)
                .containsOnly(procParallelGtw.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procParallelGtw, processInstance, "userTask", "userTask1Id", "evenFlowTask2", "oddFlowTask3");
            checkActivityInstances(procParallelGtw, processInstance, "subProcess", "subProcess");

            checkTaskInstance(procParallelGtw, processInstance, "userTask1Id", "evenFlowTask2", "oddFlowTask3");
        }

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procParallelGtw, processInstance, "userTask", "userTask1Id", "oddFlowTask3", "evenFlowTask2", "evenFlowTask4", "taskAfter");
            checkActivityInstances(procParallelGtw, processInstance, "subProcess", "subProcess");
            checkActivityInstances(procParallelGtw, processInstance, "parallelGateway", "parallelJoin", "parallelJoin");

            List<HistoricActivityInstance> subProcExecution = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .activityType("subProcess")
                    .list();
            assertThat(subProcExecution)
                    .extracting(HistoricActivityInstance::getActivityId)
                    .containsExactly("subProcess");
            assertThat(subProcExecution)
                    .extracting(HistoricActivityInstance::getProcessDefinitionId)
                    .containsOnly(procParallelGtw.getId());

            List<HistoricActivityInstance> gtwExecution = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .activityType("parallelGateway")
                    .list();
            //Two flows to join in, fork was not executed
            assertThat(gtwExecution)
                    .extracting(HistoricActivityInstance::getActivityId)
                    .containsExactlyInAnyOrder("parallelJoin", "parallelJoin");
            assertThat(gtwExecution)
                    .extracting(HistoricActivityInstance::getProcessDefinitionId)
                    .containsOnly(procParallelGtw.getId());

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                        .processInstanceId(processInstance.getId())
                        .list();
                assertThat(historicTasks)
                        .extracting(HistoricTaskInstance::getTaskDefinitionKey)
                        .containsExactlyInAnyOrder("userTask1Id", "oddFlowTask3", "evenFlowTask2", "evenFlowTask4", "taskAfter");
                assertThat(historicTasks)
                        .extracting(HistoricTaskInstance::getProcessDefinitionId)
                        .containsOnly(procParallelGtw.getId());
            }
        }

        assertProcessEnded(processInstance.getId());
    }

    //-- Inclusive Gateway
    @Test
    public void testMigrateActivityToInclusiveGatewaySplitWithDefault() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procInclusiveGtw = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/inclusive-gateway-fork-and-join-with-default.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly("userTask1Id", procDefOneTask.getId());

        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procInclusiveGtw.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", Arrays.asList("gwFork")));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("taskEquals");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procInclusiveGtw.getId());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly(tuple("taskEquals", procInclusiveGtw.getId()));

        //Complete gateway tasks
        tasks.forEach(this::completeTask);

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("taskAfter");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procInclusiveGtw.getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactly("taskAfter");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly(tuple("taskAfter", procInclusiveGtw.getId()));

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            checkActivityInstances(procInclusiveGtw, processInstance, "inclusiveGateway", "gwFork", "gwJoin");

            checkTaskInstance(procInclusiveGtw, processInstance, "userTask1Id", "taskEquals", "taskAfter");
        }

        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            checkActivityInstances(procInclusiveGtw, processInstance, "userTask", "userTask1Id", "taskEquals", "taskAfter");
            checkActivityInstances(procInclusiveGtw, processInstance, "inclusiveGateway", "gwJoin", "gwFork");

            checkTaskInstance(procInclusiveGtw, processInstance, "userTask1Id", "taskEquals", "taskAfter");
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityToInclusiveGatewaySplitNoOutgoingSequenceSelectionException() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procInclusiveGtw = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/inclusive-gateway-fork-and-join.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly("userTask1Id", procDefOneTask.getId());

        assertThatThrownBy(() -> {
            ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                    .migrateToProcessDefinition(procInclusiveGtw.getId())
                    .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", Arrays.asList("gwFork"))
                            .withLocalVariableForAllActivities("myConditionVar", 10));

            ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                    .validateMigration(processInstance.getId());
            assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();

            processInstanceMigrationBuilder.migrate(processInstance.getId());
        })
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessage("No outgoing sequence flow of element 'gwFork' could be selected for continuing the process");
    }

    @Test
    public void testMigrateActivityToInclusiveGatewaySplitWithConditionSpecifiedBeforeMigration() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procInclusiveGtw = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/inclusive-gateway-fork-and-join.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsOnly("userTask1Id", procDefOneTask.getId());

        runtimeService.setVariable(executions.get(0).getId(), "myConditionVar", 11);

        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procInclusiveGtw.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", Arrays.asList("gwFork")));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("taskMore");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procInclusiveGtw.getId());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly(tuple("taskMore", procInclusiveGtw.getId()));

        //Complete gateway tasks
        tasks.forEach(this::completeTask);

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("taskAfter");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procInclusiveGtw.getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly(tuple("taskAfter", procInclusiveGtw.getId()));

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procInclusiveGtw, processInstance, "userTask", "userTask1Id", "taskMore", "taskAfter");
            checkActivityInstances(procInclusiveGtw, processInstance, "inclusiveGateway", "gwJoin", "gwFork");

            checkTaskInstance(procInclusiveGtw, processInstance, "userTask1Id", "taskMore", "taskAfter");

        }

        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            checkActivityInstances(procInclusiveGtw, processInstance, "userTask", "userTask1Id", "taskMore", "taskAfter");
            checkActivityInstances(procInclusiveGtw, processInstance, "inclusiveGateway", "gwJoin", "gwFork");

            checkTaskInstance(procInclusiveGtw, processInstance, "userTask1Id", "taskMore", "taskAfter");

        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityToInclusiveGatewaySplitWithConditionSpecifiedDuringMigration() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procInclusiveGtw = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/inclusive-gateway-fork-and-join.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly("userTask1Id", procDefOneTask.getId());

        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procInclusiveGtw.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", Arrays.asList("gwFork"))
                        .withLocalVariableForAllActivities("myConditionVar", 11));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("taskMore");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procInclusiveGtw.getId());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly(tuple("taskMore", procInclusiveGtw.getId()));

        //Complete gateway tasks
        tasks.forEach(this::completeTask);

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("taskAfter");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procInclusiveGtw.getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly(tuple("taskAfter", procInclusiveGtw.getId()));

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            checkActivityInstances(procInclusiveGtw, processInstance, "userTask", "userTask1Id", "taskMore", "taskAfter");
            checkActivityInstances(procInclusiveGtw, processInstance, "inclusiveGateway", "gwJoin", "gwFork");

            checkTaskInstance(procInclusiveGtw, processInstance, "taskMore", "userTask1Id", "taskAfter");

        }

        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            checkActivityInstances(procInclusiveGtw, processInstance, "userTask", "userTask1Id", "taskMore", "taskAfter");
            checkActivityInstances(procInclusiveGtw, processInstance, "inclusiveGateway", "gwJoin", "gwFork");

            checkTaskInstance(procInclusiveGtw, processInstance, "taskMore", "userTask1Id", "taskAfter");

        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityToInclusiveGatewaySplitWithConditionSpecifiedAsProcessVariableDuringMigration() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procInclusiveGtw = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/inclusive-gateway-fork-and-join.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly("userTask1Id", procDefOneTask.getId());

        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procInclusiveGtw.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", Arrays.asList("gwFork")))
                .withProcessInstanceVariable("myConditionVar", 11);

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("taskMore");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procInclusiveGtw.getId());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly(tuple("taskMore", procInclusiveGtw.getId()));

        //Complete gateway tasks
        tasks.forEach(this::completeTask);

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("taskAfter");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procInclusiveGtw.getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly(tuple("taskAfter", procInclusiveGtw.getId()));

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            checkActivityInstances(procInclusiveGtw, processInstance, "userTask", "userTask1Id", "taskMore", "taskAfter");
            checkActivityInstances(procInclusiveGtw, processInstance, "inclusiveGateway", "gwJoin", "gwFork");

            checkTaskInstance(procInclusiveGtw, processInstance, "taskMore", "userTask1Id", "taskAfter");

        }

        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            checkActivityInstances(procInclusiveGtw, processInstance, "userTask", "userTask1Id", "taskMore", "taskAfter");
            checkActivityInstances(procInclusiveGtw, processInstance, "inclusiveGateway", "gwJoin", "gwFork");

            checkTaskInstance(procInclusiveGtw, processInstance, "taskMore", "userTask1Id", "taskAfter");

        }
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityToOneActivityInsideInclusiveGateway() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procInclusiveGtw = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/inclusive-gateway-fork-and-join-with-default.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly("userTask1Id", procDefOneTask.getId());

        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procInclusiveGtw.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", Arrays.asList("taskLess")));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("taskLess");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procInclusiveGtw.getId());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly(tuple("taskLess", procInclusiveGtw.getId()));

        //Complete the task
        completeTask(tasks.get(0));

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("taskAfter");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procInclusiveGtw.getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("taskAfter");
        assertThat(tasks)
                .extracting(Task::getProcessDefinitionId)
                .containsOnly(procInclusiveGtw.getId());
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly(tuple("taskAfter", procInclusiveGtw.getId()));

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            checkActivityInstances(procInclusiveGtw, processInstance, "userTask", "taskLess", "taskAfter");
            checkActivityInstances(procInclusiveGtw, processInstance, "inclusiveGateway", "gwJoin");

            checkTaskInstance(procInclusiveGtw, processInstance, "taskAfter", "taskLess");
        }

        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            checkActivityInstances(procInclusiveGtw, processInstance, "userTask", "taskLess", "taskAfter");
            checkActivityInstances(procInclusiveGtw, processInstance, "inclusiveGateway", "gwJoin");

            checkTaskInstance(procInclusiveGtw, processInstance, "taskAfter", "taskLess");
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityToParallelActivitiesInsideInclusiveGateway() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procInclusiveGtw = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/inclusive-gateway-fork-and-join.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly("userTask1Id", procDefOneTask.getId());

        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procInclusiveGtw.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", Arrays.asList("taskMore", "taskLess")));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("taskMore", "taskLess");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procInclusiveGtw.getId());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("taskMore", "taskLess");
        assertThat(tasks)
                .extracting(Task::getProcessDefinitionId)
                .containsOnly(procInclusiveGtw.getId());

        //Complete one sequence
        task = tasks.stream().filter(t -> "taskMore".equals(t.getTaskDefinitionKey())).findFirst().get();
        completeTask(task);

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("gwJoin", "taskLess");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procInclusiveGtw.getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly(tuple("taskLess", procInclusiveGtw.getId()));

        //Complete the rest
        tasks.forEach(this::completeTask);

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("taskAfter");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procInclusiveGtw.getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly(tuple("taskAfter", procInclusiveGtw.getId()));

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            checkActivityInstances(procInclusiveGtw, processInstance, "userTask", "userTask1Id", "taskLess", "taskMore", "taskAfter");
            checkActivityInstances(procInclusiveGtw, processInstance, "inclusiveGateway", "gwJoin", "gwJoin");

            checkTaskInstance(procInclusiveGtw, processInstance, "userTask1Id", "taskLess", "taskMore", "taskAfter");
        }

        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            checkActivityInstances(procInclusiveGtw, processInstance, "userTask", "userTask1Id", "taskLess", "taskMore", "taskAfter");
            checkActivityInstances(procInclusiveGtw, processInstance, "inclusiveGateway", "gwJoin", "gwJoin");

            checkTaskInstance(procInclusiveGtw, processInstance, "userTask1Id", "taskLess", "taskMore", "taskAfter");
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateParallelActivitiesToParallelActivitiesInsideInclusiveGateway() {
        ProcessDefinition procParallelTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/timer-parallel-task.bpmn20.xml");
        ProcessDefinition procInclusiveGtw = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/inclusive-gateway-fork-multiple-outgoing-sequences.bpmn20.xml");

        //Start the processInstance
        //Start the processInstance and spawn the parallel task
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procParallelTask.getId());

        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        //Confirm the state to migrate
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

        //Migrate each of the parallel task to a task in the parallel gateway
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procInclusiveGtw.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("processTask", "taskMore"))
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("parallelTask", "taskLess"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("taskMore", "taskLess");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procInclusiveGtw.getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("taskMore", "taskLess");
        assertThat(tasks)
                .extracting(Task::getProcessDefinitionId)
                .containsOnly(procInclusiveGtw.getId());

        //Complete one sequence
        Task task = tasks.stream().filter(t -> "taskMore".equals(t.getTaskDefinitionKey())).findFirst().get();
        completeTask(task);

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("gwJoin", "taskLess");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procInclusiveGtw.getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly(tuple("taskLess", procInclusiveGtw.getId()));

        //Complete the rest
        tasks.forEach(this::completeTask);

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("taskAfter");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procInclusiveGtw.getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly(tuple("taskAfter", procInclusiveGtw.getId()));

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            checkActivityInstances(procInclusiveGtw, processInstance, "userTask", "taskMore", "taskLess", "taskAfter");
            checkActivityInstances(procInclusiveGtw, processInstance, "inclusiveGateway", "gwJoin", "gwJoin");

            checkTaskInstance(procInclusiveGtw, processInstance, "taskMore", "taskLess", "taskAfter");
        }

        completeProcessInstanceTasks(processInstance.getId());
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            checkActivityInstances(procInclusiveGtw, processInstance, "userTask", "taskMore", "taskLess", "taskAfter");
            checkActivityInstances(procInclusiveGtw, processInstance, "inclusiveGateway", "gwJoin", "gwJoin");

            checkTaskInstance(procInclusiveGtw, processInstance, "taskMore", "taskLess", "taskAfter");
        }
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateInclusiveGatewayParallelActivitiesToSingleActivity() {
        ProcessDefinition procInclusiveGtw = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/inclusive-gateway-fork-multiple-outgoing-sequences.bpmn20.xml");
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procInclusiveGtw.getId(), Collections.singletonMap("myConditionVar", 10));
        completeTask(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("taskMore", "taskLess");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procInclusiveGtw.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("taskMore", "taskLess");
        assertThat(tasks)
                .extracting(Task::getProcessDefinitionId)
                .containsOnly(procInclusiveGtw.getId());

        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefOneTask.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor(Arrays.asList("taskMore", "taskLess"), "userTask1Id"));

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
                .containsOnly(procDefOneTask.getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("userTask1Id");
        assertThat(tasks)
                .extracting(Task::getProcessDefinitionId)
                .containsOnly(procDefOneTask.getId());
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly(tuple("userTask1Id", procDefOneTask.getId()));

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            checkActivityInstances(procDefOneTask, processInstance, "userTask", "taskBefore", "taskMore", "taskLess", "userTask1Id");
            checkActivityInstances(procDefOneTask, processInstance, "inclusiveGateway", "gwFork");

            checkTaskInstance(procDefOneTask, processInstance, "taskBefore", "taskMore", "taskLess", "userTask1Id");
        }

        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            checkActivityInstances(procDefOneTask, processInstance, "userTask", "taskBefore", "taskMore", "taskLess", "userTask1Id");
            checkActivityInstances(procDefOneTask, processInstance, "inclusiveGateway", "gwFork");

            checkTaskInstance(procDefOneTask, processInstance, "taskBefore", "taskMore", "taskLess", "userTask1Id");
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityToInclusiveGatewaySplitWithDefaultInsideEmbeddedSubProcess() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procInclusiveGtw = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/inclusive-gateway-fork-and-join-with-default-nested-in-embedded-subprocess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly("userTask1Id", procDefOneTask.getId());

        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procInclusiveGtw.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", Arrays.asList("gwFork")));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("subProcess", "taskEquals");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procInclusiveGtw.getId());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly(tuple("taskEquals", procInclusiveGtw.getId()));

        //Complete gateway tasks
        tasks.forEach(this::completeTask);

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("taskAfter");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procInclusiveGtw.getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly(tuple("taskAfter", procInclusiveGtw.getId()));

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            checkActivityInstances(procInclusiveGtw, processInstance, "userTask", "userTask1Id", "taskEquals", "taskAfter");
            checkActivityInstances(procInclusiveGtw, processInstance, "subProcess", "subProcess");
            checkActivityInstances(procInclusiveGtw, processInstance, "inclusiveGateway", "gwJoin", "gwFork");

            checkTaskInstance(procInclusiveGtw, processInstance, "userTask1Id", "taskEquals", "taskAfter");
        }

        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            checkActivityInstances(procInclusiveGtw, processInstance, "userTask", "userTask1Id", "taskEquals", "taskAfter");
            checkActivityInstances(procInclusiveGtw, processInstance, "subProcess", "subProcess");
            checkActivityInstances(procInclusiveGtw, processInstance, "inclusiveGateway", "gwJoin", "gwFork");

            checkTaskInstance(procInclusiveGtw, processInstance, "userTask1Id", "taskEquals", "taskAfter");
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityToOneActivityInsideInclusiveGatewayInsideEmbeddedSubProcess() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procInclusiveGtw = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/inclusive-gateway-fork-and-join-with-default-nested-in-embedded-subprocess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly("userTask1Id", procDefOneTask.getId());

        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procInclusiveGtw.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", Arrays.asList("taskLess")));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("subProcess", "taskLess");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procInclusiveGtw.getId());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly(tuple("taskLess", procInclusiveGtw.getId()));

        //Complete the task
        completeTask(tasks.get(0));

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("taskAfter");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procInclusiveGtw.getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly(tuple("taskAfter", procInclusiveGtw.getId()));

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            checkActivityInstances(procInclusiveGtw, processInstance, "userTask", "taskLess", "taskAfter");
            checkActivityInstances(procInclusiveGtw, processInstance, "subProcess", "subProcess");
            checkActivityInstances(procInclusiveGtw, processInstance, "inclusiveGateway", "gwJoin");

            checkTaskInstance(procInclusiveGtw, processInstance, "taskAfter", "taskLess");

            List<HistoricActivityInstance> gtwExecution = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .activityType("inclusiveGateway")
                    .list();
            //Join gateway only
            assertThat(gtwExecution)
                    .extracting(HistoricActivityInstance::getActivityId)
                    .containsExactly("gwJoin");
            assertThat(gtwExecution)
                    .extracting(HistoricActivityInstance::getProcessDefinitionId)
                    .containsOnly(procInclusiveGtw.getId());
        }

        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            checkActivityInstances(procInclusiveGtw, processInstance, "userTask", "taskLess", "taskAfter");
            checkActivityInstances(procInclusiveGtw, processInstance, "subProcess", "subProcess");
            checkActivityInstances(procInclusiveGtw, processInstance, "inclusiveGateway", "gwJoin");

            checkTaskInstance(procInclusiveGtw, processInstance, "taskAfter", "taskLess");
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityToParallelActivitiesInsideInclusiveGatewayInsideEmbeddedSubProcess() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procInclusiveGtw = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/inclusive-gateway-fork-and-join-with-default-nested-in-embedded-subprocess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly("userTask1Id", procDefOneTask.getId());

        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procInclusiveGtw.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", Arrays.asList("taskMore", "taskLess")));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder
                .validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("subProcess", "taskMore", "taskLess");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procInclusiveGtw.getId());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("taskMore", "taskLess");
        assertThat(tasks)
                .extracting(Task::getProcessDefinitionId)
                .containsOnly(procInclusiveGtw.getId());

        //Complete one sequence
        task = tasks.stream().filter(t -> "taskMore".equals(t.getTaskDefinitionKey())).findFirst().get();
        completeTask(task);

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("subProcess", "gwJoin", "taskLess");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procInclusiveGtw.getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly(tuple("taskLess", procInclusiveGtw.getId()));

        //Complete the rest
        tasks.forEach(this::completeTask);

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("taskAfter");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procInclusiveGtw.getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly(tuple("taskAfter", procInclusiveGtw.getId()));

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            checkActivityInstances(procInclusiveGtw, processInstance, "userTask", "userTask1Id", "taskLess", "taskMore", "taskAfter");
            checkActivityInstances(procInclusiveGtw, processInstance, "subProcess", "subProcess");
            checkActivityInstances(procInclusiveGtw, processInstance, "inclusiveGateway", "gwJoin", "gwJoin");

            checkTaskInstance(procInclusiveGtw, processInstance, "userTask1Id", "taskLess", "taskMore", "taskAfter");
        }

        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            checkActivityInstances(procInclusiveGtw, processInstance, "userTask", "userTask1Id", "taskLess", "taskMore", "taskAfter");
            checkActivityInstances(procInclusiveGtw, processInstance, "subProcess", "subProcess");
            checkActivityInstances(procInclusiveGtw, processInstance, "inclusiveGateway", "gwJoin", "gwJoin");

            checkTaskInstance(procInclusiveGtw, processInstance, "userTask1Id", "taskLess", "taskMore", "taskAfter");
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateInclusiveGatewayParallelActivitiesInsideEmbeddedSubProcessToSingleActivity() {
        ProcessDefinition procInclusiveGtw = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/inclusive-gateway-fork-multiple-outgoing-sequences-nested-in-embedded-subprocess.bpmn20.xml");
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procInclusiveGtw.getId(), Collections.singletonMap("myConditionVar", 10));
        completeTask(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("subProcess", "taskMore", "taskLess");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procInclusiveGtw.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactlyInAnyOrder("taskMore", "taskLess");
        assertThat(tasks)
                .extracting(Task::getProcessDefinitionId)
                .containsOnly(procInclusiveGtw.getId());

        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefOneTask.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor(Arrays.asList("taskMore", "taskLess"), "userTask1Id"));

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
                .containsOnly(procDefOneTask.getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey, Task::getProcessDefinitionId)
                .containsExactly(tuple("userTask1Id", procDefOneTask.getId()));

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            checkActivityInstances(procDefOneTask, processInstance, "userTask", "taskBefore", "taskMore", "taskLess", "userTask1Id");
            checkActivityInstances(procDefOneTask, processInstance, "subProcess", "subProcess");
            checkActivityInstances(procDefOneTask, processInstance, "inclusiveGateway", "gwFork");

            checkTaskInstance(procDefOneTask, processInstance, "taskBefore", "taskMore", "taskLess", "userTask1Id");
        }

        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            checkActivityInstances(procDefOneTask, processInstance, "userTask", "taskBefore", "taskMore", "taskLess", "userTask1Id");
            checkActivityInstances(procDefOneTask, processInstance, "subProcess", "subProcess");
            checkActivityInstances(procDefOneTask, processInstance, "inclusiveGateway", "gwFork");

            checkTaskInstance(procDefOneTask, processInstance, "taskBefore", "taskMore", "taskLess", "userTask1Id");
        }
    }
}
