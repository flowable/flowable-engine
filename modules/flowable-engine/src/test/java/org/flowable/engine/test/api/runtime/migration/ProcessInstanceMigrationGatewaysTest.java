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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.migration.ProcessInstanceMigrationValidationResult;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.migration.ActivityMigrationMapping;
import org.flowable.engine.migration.ProcessInstanceMigrationBuilder;
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
public class ProcessInstanceMigrationGatewaysTest extends PluggableFlowableTestCase {

    @AfterEach
    protected void tearDown() {
        deleteDeployments();
    }

    //-- Exclusive Gateway
    @Test
    public void testMigrateActivityToExclusiveGatewayStartWithDefaultFlow() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procWithExcGtw = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/exclusive-gateway-with-default-sequence.bpmn20.xml");

        //Start the processInstance without timer
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());

        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processInstanceMigrationService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procWithExcGtw.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "exclusiveGw"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("defaultFlowTask");
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("defaultFlowTask");

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            List<HistoricActivityInstance> taskExecutions = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("userTask")
                .list();
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("userTask1Id", "defaultFlowTask");
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procWithExcGtw.getId());

            List<HistoricActivityInstance> gtwExecution = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("exclusiveGateway")
                .list();
            assertThat(gtwExecution).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("exclusiveGw");
            assertThat(gtwExecution).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procWithExcGtw.getId());

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();
                assertThat(historicTasks).extracting(HistoricTaskInstance::getTaskDefinitionKey).containsExactly("userTask1Id", "defaultFlowTask");
                assertThat(historicTasks).extracting(HistoricTaskInstance::getProcessDefinitionId).containsOnly(procWithExcGtw.getId());
            }
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityToExclusiveGatewayStartWithConditionSpecifiedBeforeMigration() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procWithExcGtw = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/exclusive-gateway-with-default-sequence.bpmn20.xml");

        //Start the processInstance without timer
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());

        runtimeService.setVariable(executions.get(0).getId(), "input", 1);

        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processInstanceMigrationService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procWithExcGtw.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "exclusiveGw"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("theTask1");
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("theTask1");

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            List<HistoricActivityInstance> taskExecutions = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("userTask")
                .list();
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("userTask1Id", "theTask1");
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procWithExcGtw.getId());

            List<HistoricActivityInstance> gtwExecution = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("exclusiveGateway")
                .list();
            assertThat(gtwExecution).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("exclusiveGw");
            assertThat(gtwExecution).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procWithExcGtw.getId());

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();
                assertThat(historicTasks).extracting(HistoricTaskInstance::getTaskDefinitionKey).containsExactly("userTask1Id", "theTask1");
                assertThat(historicTasks).extracting(HistoricTaskInstance::getProcessDefinitionId).containsOnly(procWithExcGtw.getId());
            }
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityToExclusiveGatewayStartWithConditionSpecifiedDuringMigration() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procWithExcGtw = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/exclusive-gateway-with-default-sequence.bpmn20.xml");

        //Start the processInstance without timer
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());

        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processInstanceMigrationService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procWithExcGtw.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "exclusiveGw").withLocalVariable("input", Integer.valueOf(1)));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("theTask1");
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("theTask1");

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            List<HistoricActivityInstance> taskExecutions = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("userTask")
                .list();
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("userTask1Id", "theTask1");
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procWithExcGtw.getId());

            List<HistoricActivityInstance> gtwExecution = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("exclusiveGateway")
                .list();
            assertThat(gtwExecution).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("exclusiveGw");
            assertThat(gtwExecution).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procWithExcGtw.getId());

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();
                assertThat(historicTasks).extracting(HistoricTaskInstance::getTaskDefinitionKey).containsExactly("userTask1Id", "theTask1");
                assertThat(historicTasks).extracting(HistoricTaskInstance::getProcessDefinitionId).containsOnly(procWithExcGtw.getId());
            }
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityToExclusiveGatewayStartWithConditionSpecifiedAsProcessVariableDuringMigration() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procWithExcGtw = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/exclusive-gateway-with-default-sequence.bpmn20.xml");

        //Start the processInstance without timer
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());

        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processInstanceMigrationService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procWithExcGtw.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "exclusiveGw"))
            .withProcessInstanceVariable("input", 1);

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("theTask1");
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("theTask1");

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            List<HistoricActivityInstance> taskExecutions = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("userTask")
                .list();
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("userTask1Id", "theTask1");
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procWithExcGtw.getId());

            List<HistoricActivityInstance> gtwExecution = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("exclusiveGateway")
                .list();
            assertThat(gtwExecution).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("exclusiveGw");
            assertThat(gtwExecution).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procWithExcGtw.getId());

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();
                assertThat(historicTasks).extracting(HistoricTaskInstance::getTaskDefinitionKey).containsExactly("userTask1Id", "theTask1");
                assertThat(historicTasks).extracting(HistoricTaskInstance::getProcessDefinitionId).containsOnly(procWithExcGtw.getId());
            }
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityToActivityInsideExclusiveGateway() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procWithExcGtw = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/exclusive-gateway-with-default-sequence.bpmn20.xml");

        //Start the processInstance without timer
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());

        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processInstanceMigrationService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procWithExcGtw.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "theTask2"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("theTask2");
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("theTask2");

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            List<HistoricActivityInstance> taskExecutions = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("userTask")
                .list();
            //Direct migration
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("theTask2");
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procWithExcGtw.getId());

            List<HistoricActivityInstance> gtwExecution = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("exclusiveGateway")
                .list();
            assertThat(gtwExecution).isEmpty();

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();
                assertThat(historicTasks).extracting(HistoricTaskInstance::getTaskDefinitionKey).containsExactly("theTask2");
                assertThat(historicTasks).extracting(HistoricTaskInstance::getProcessDefinitionId).containsOnly(procWithExcGtw.getId());
            }
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityInsideExclusiveGatewayToActivityOutside() {
        ProcessDefinition procWithExcGtw = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/exclusive-gateway-with-default-sequence.bpmn20.xml");
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");

        //Start the processInstance without timer
        Map<String, Object> gtwCondition = Collections.singletonMap("input", Integer.valueOf(2));
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procWithExcGtw.getId(), gtwCondition);

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("theTask2");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procWithExcGtw.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("theTask2");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procWithExcGtw.getId());

        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processInstanceMigrationService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procDefOneTask.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("theTask2", "userTask1Id"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            List<HistoricActivityInstance> taskExecutions = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("userTask")
                .list();
            //Direct migration
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("userTask1Id");
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procDefOneTask.getId());

            List<HistoricActivityInstance> gtwExecution = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("exclusiveGateway")
                .list();
            assertThat(gtwExecution).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("exclusiveGw");
            assertThat(gtwExecution).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procDefOneTask.getId());

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();
                assertThat(historicTasks).extracting(HistoricTaskInstance::getTaskDefinitionKey).containsExactly("userTask1Id");
                assertThat(historicTasks).extracting(HistoricTaskInstance::getProcessDefinitionId).containsOnly(procDefOneTask.getId());
            }
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityToExclusiveGatewayStartInsideEmbeddedSubProcessDefaultFlow() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procWithExcGtw = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/exclusive-gateway-with-default-sequence-inside-embedded-subprocess.bpmn20.xml");

        //Start the processInstance without timer
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());

        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processInstanceMigrationService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procWithExcGtw.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "exclusiveGw"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("theSubProcess", "defaultFlowTask");
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("defaultFlowTask");

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            List<HistoricActivityInstance> taskExecutions = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("userTask")
                .list();
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("userTask1Id", "defaultFlowTask", "afterSubProcessTask");
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procWithExcGtw.getId());

            List<HistoricActivityInstance> subProcExecution = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("subProcess")
                .list();
            assertThat(subProcExecution).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("theSubProcess");
            assertThat(subProcExecution).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procWithExcGtw.getId());

            List<HistoricActivityInstance> gtwExecution = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("exclusiveGateway")
                .list();
            assertThat(gtwExecution).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("exclusiveGw");
            assertThat(gtwExecution).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procWithExcGtw.getId());

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();
                assertThat(historicTasks).extracting(HistoricTaskInstance::getTaskDefinitionKey).containsExactlyInAnyOrder("userTask1Id", "defaultFlowTask", "afterSubProcessTask");
                assertThat(historicTasks).extracting(HistoricTaskInstance::getProcessDefinitionId).containsOnly(procWithExcGtw.getId());
            }
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityToExclusiveGatewayStartInsideEmbeddedSubProcess() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procWithExcGtw = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/exclusive-gateway-with-default-sequence-inside-embedded-subprocess.bpmn20.xml");

        //Start the processInstance without timer
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());

        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processInstanceMigrationService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procWithExcGtw.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "exclusiveGw").withLocalVariables(Collections.singletonMap("input", Integer.valueOf(1))));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("theSubProcess", "theTask1");
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("theTask1");

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            List<HistoricActivityInstance> taskExecutions = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("userTask")
                .list();
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("userTask1Id", "theTask1", "afterSubProcessTask");
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procWithExcGtw.getId());

            List<HistoricActivityInstance> subProcExecution = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("subProcess")
                .list();
            assertThat(subProcExecution).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("theSubProcess");
            assertThat(subProcExecution).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procWithExcGtw.getId());

            List<HistoricActivityInstance> gtwExecution = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("exclusiveGateway")
                .list();
            assertThat(gtwExecution).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("exclusiveGw");
            assertThat(gtwExecution).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procWithExcGtw.getId());

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();
                assertThat(historicTasks).extracting(HistoricTaskInstance::getTaskDefinitionKey).containsExactly("userTask1Id", "theTask1", "afterSubProcessTask");
                assertThat(historicTasks).extracting(HistoricTaskInstance::getProcessDefinitionId).containsOnly(procWithExcGtw.getId());
            }
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityToActivityInsideExclusiveGatewayInsideEmbeddedSubProcess() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procWithExcGtw = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/exclusive-gateway-with-default-sequence-inside-embedded-subprocess.bpmn20.xml");

        //Start the processInstance without timer
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());

        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processInstanceMigrationService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procWithExcGtw.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "theTask2"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("theSubProcess", "theTask2");
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("theTask2");

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            List<HistoricActivityInstance> taskExecutions = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("userTask")
                .list();
            //Direct migration
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("theTask2", "afterSubProcessTask");
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procWithExcGtw.getId());

            List<HistoricActivityInstance> subProcExecution = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("subProcess")
                .list();
            assertThat(subProcExecution).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("theSubProcess");
            assertThat(subProcExecution).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procWithExcGtw.getId());

            List<HistoricActivityInstance> gtwExecution = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("exclusiveGateway")
                .list();
            assertThat(gtwExecution).isEmpty();

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();
                assertThat(historicTasks).extracting(HistoricTaskInstance::getTaskDefinitionKey).containsExactly("theTask2", "afterSubProcessTask");
                assertThat(historicTasks).extracting(HistoricTaskInstance::getProcessDefinitionId).containsOnly(procWithExcGtw.getId());
            }
        }

        assertProcessEnded(processInstance.getId());
    }

    //-- Parallel Gateway
    @Test
    public void testMigrateActivityToParallelGatewayFork() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procParallelGtw = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/parallel-gateway-two-splits-four-tasks.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());

        //Migrate each of the parallel task to a task in the parallel gateway
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processInstanceMigrationService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procParallelGtw.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "parallelFork"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("oddFlowTask1", "evenFlowTask2");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procParallelGtw.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("oddFlowTask1", "evenFlowTask2");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procParallelGtw.getId());

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            List<HistoricActivityInstance> subProcesses = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("userTask")
                .list();
            assertThat(subProcesses).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("userTask1Id", "oddFlowTask1", "oddFlowTask3", "evenFlowTask2", "evenFlowTask4", "taskAfter");
            assertThat(subProcesses).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procParallelGtw.getId());

            List<HistoricActivityInstance> gtwExecution = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("parallelGateway")
                .list();
            //Two flows to join in
            assertThat(gtwExecution).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("parallelFork", "parallelJoin", "parallelJoin");
            assertThat(gtwExecution).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procParallelGtw.getId());

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();
                assertThat(historicTasks).extracting(HistoricTaskInstance::getTaskDefinitionKey).containsExactlyInAnyOrder("userTask1Id", "oddFlowTask1", "oddFlowTask3", "evenFlowTask2", "evenFlowTask4", "taskAfter");
                assertThat(historicTasks).extracting(HistoricTaskInstance::getProcessDefinitionId).containsOnly(procParallelGtw.getId());
            }
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateParallelActivitiesToParallelGatewayActivities() {
        ProcessDefinition procParallelTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/timer-parallel-task.bpmn20.xml");
        ProcessDefinition procParallelGtw = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/parallel-gateway-two-splits-four-tasks.bpmn20.xml");

        //Start the processInstance and spawn the parallel task
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procParallelTask.getId());

        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("timerBound", "processTask", "parallelTask");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procParallelTask.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("processTask", "parallelTask");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procParallelTask.getId());

        //Migrate each of the parallel task to a task in the parallel gateway
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processInstanceMigrationService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procParallelGtw.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("processTask", "oddFlowTask1"))
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("parallelTask", "evenFlowTask4"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("oddFlowTask1", "evenFlowTask4");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procParallelGtw.getId());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("oddFlowTask1", "evenFlowTask4");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procParallelGtw.getId());

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            List<HistoricActivityInstance> subProcesses = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("userTask")
                .list();
            //Direct Migration
            assertThat(subProcesses).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("oddFlowTask1", "oddFlowTask3", "evenFlowTask4", "taskAfter");
            assertThat(subProcesses).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procParallelGtw.getId());

            List<HistoricActivityInstance> gtwExecution = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("parallelGateway")
                .list();
            //Two flows to join in, fork was not executed
            assertThat(gtwExecution).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("parallelJoin", "parallelJoin");
            assertThat(gtwExecution).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procParallelGtw.getId());

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();
                assertThat(historicTasks).extracting(HistoricTaskInstance::getTaskDefinitionKey).containsExactlyInAnyOrder("oddFlowTask1", "oddFlowTask3", "evenFlowTask4", "taskAfter");
                assertThat(historicTasks).extracting(HistoricTaskInstance::getProcessDefinitionId).containsOnly(procParallelGtw.getId());
            }
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityToParallelGatewayActivities() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procParallelGtw = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/parallel-gateway-two-splits-four-tasks.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());

        //Migrate each of the parallel task to a task in the parallel gateway
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processInstanceMigrationService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procParallelGtw.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", Arrays.asList("oddFlowTask3", "evenFlowTask2")));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("oddFlowTask3", "evenFlowTask2");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procParallelGtw.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("oddFlowTask3", "evenFlowTask2");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procParallelGtw.getId());

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            List<HistoricActivityInstance> subProcesses = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("userTask")
                .list();
            assertThat(subProcesses).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("userTask1Id", "oddFlowTask3", "evenFlowTask2", "evenFlowTask4", "taskAfter");
            assertThat(subProcesses).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procParallelGtw.getId());

            List<HistoricActivityInstance> gtwExecution = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("parallelGateway")
                .list();
            //Two flows to join in, fork was not executed
            assertThat(gtwExecution).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("parallelJoin", "parallelJoin");
            assertThat(gtwExecution).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procParallelGtw.getId());

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();
                assertThat(historicTasks).extracting(HistoricTaskInstance::getTaskDefinitionKey).containsExactlyInAnyOrder("userTask1Id", "oddFlowTask3", "evenFlowTask2", "evenFlowTask4", "taskAfter");
                assertThat(historicTasks).extracting(HistoricTaskInstance::getProcessDefinitionId).containsOnly(procParallelGtw.getId());
            }
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityToParallelGatewayActivitiesIncompleteActivityMapping() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procParallelGtw = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/parallel-gateway-three-splits.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());

        //Migrate each of the parallel task to a task in the parallel gateway
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processInstanceMigrationService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procParallelGtw.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", Arrays.asList("task1", "task3")));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("task1", "task3");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procParallelGtw.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("task1", "task3");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procParallelGtw.getId());

        //Try to complete the process
        completeProcessInstanceTasks(processInstance.getId());

        //Parallel Gateway join is not satisfied since there is a missing sequence flow
        List<Execution> parallelJoinExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(parallelJoinExecutions).extracting(Execution::getActivityId).containsOnly("parallelJoin");
    }

    @Test
    public void testMigrateActivityToParallelGatewayForkInsideEmbeddedSubProcess() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procParallelGtw = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/parallel-gateway-nested-in-embedded-subprocess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());

        //Migrate each of the parallel task to a task in the parallel gateway
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processInstanceMigrationService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procParallelGtw.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "parallelFork"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("subProcess", "oddFlowTask1", "evenFlowTask2");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procParallelGtw.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("oddFlowTask1", "evenFlowTask2");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procParallelGtw.getId());

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            List<HistoricActivityInstance> subProcesses = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("userTask")
                .list();
            assertThat(subProcesses).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("userTask1Id", "oddFlowTask1", "oddFlowTask3", "evenFlowTask2", "evenFlowTask4", "taskAfter");
            assertThat(subProcesses).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procParallelGtw.getId());

            List<HistoricActivityInstance> subProcExecution = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("subProcess")
                .list();
            assertThat(subProcExecution).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("subProcess");
            assertThat(subProcExecution).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procParallelGtw.getId());

            List<HistoricActivityInstance> gtwExecution = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("parallelGateway")
                .list();
            //Two flows to join in
            assertThat(gtwExecution).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("parallelFork", "parallelJoin", "parallelJoin");
            assertThat(gtwExecution).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procParallelGtw.getId());

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();
                assertThat(historicTasks).extracting(HistoricTaskInstance::getTaskDefinitionKey).containsExactlyInAnyOrder("userTask1Id", "oddFlowTask1", "oddFlowTask3", "evenFlowTask2", "evenFlowTask4", "taskAfter");
                assertThat(historicTasks).extracting(HistoricTaskInstance::getProcessDefinitionId).containsOnly(procParallelGtw.getId());
            }
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateParallelActivitiesToParallelGatewayActivitiesInsideEmbeddedSubProcess() {
        ProcessDefinition procParallelTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/timer-parallel-task.bpmn20.xml");
        ProcessDefinition procParallelGtw = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/parallel-gateway-nested-in-embedded-subprocess.bpmn20.xml");

        //Start the processInstance and spawn the parallel task
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procParallelTask.getId());

        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("timerBound", "processTask", "parallelTask");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procParallelTask.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("processTask", "parallelTask");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procParallelTask.getId());

        //Migrate each of the parallel task to a task in the parallel gateway
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processInstanceMigrationService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procParallelGtw.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("processTask", "oddFlowTask1"))
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("parallelTask", "evenFlowTask4"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("subProcess", "oddFlowTask1", "evenFlowTask4");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procParallelGtw.getId());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("oddFlowTask1", "evenFlowTask4");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procParallelGtw.getId());

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            List<HistoricActivityInstance> subProcesses = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("userTask")
                .list();
            //Direct Migration
            assertThat(subProcesses).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("oddFlowTask1", "oddFlowTask3", "evenFlowTask4", "taskAfter");
            assertThat(subProcesses).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procParallelGtw.getId());

            List<HistoricActivityInstance> subProcExecution = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("subProcess")
                .list();
            assertThat(subProcExecution).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("subProcess");
            assertThat(subProcExecution).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procParallelGtw.getId());

            List<HistoricActivityInstance> gtwExecution = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("parallelGateway")
                .list();
            //Two flows to join in, fork was not executed
            assertThat(gtwExecution).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("parallelJoin", "parallelJoin");
            assertThat(gtwExecution).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procParallelGtw.getId());

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();
                assertThat(historicTasks).extracting(HistoricTaskInstance::getTaskDefinitionKey).containsExactlyInAnyOrder("oddFlowTask1", "oddFlowTask3", "evenFlowTask4", "taskAfter");
                assertThat(historicTasks).extracting(HistoricTaskInstance::getProcessDefinitionId).containsOnly(procParallelGtw.getId());
            }
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityToParallelGatewayActivitiesInsideEmbeddedSubProcess() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procParallelGtw = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/parallel-gateway-nested-in-embedded-subprocess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());

        //Migrate each of the parallel task to a task in the parallel gateway
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processInstanceMigrationService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procParallelGtw.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", Arrays.asList("oddFlowTask3", "evenFlowTask2")));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("subProcess", "oddFlowTask3", "evenFlowTask2");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procParallelGtw.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("oddFlowTask3", "evenFlowTask2");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procParallelGtw.getId());

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            List<HistoricActivityInstance> subProcesses = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("userTask")
                .list();
            assertThat(subProcesses).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("userTask1Id", "oddFlowTask3", "evenFlowTask2", "evenFlowTask4", "taskAfter");
            assertThat(subProcesses).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procParallelGtw.getId());

            List<HistoricActivityInstance> subProcExecution = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("subProcess")
                .list();
            assertThat(subProcExecution).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("subProcess");
            assertThat(subProcExecution).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procParallelGtw.getId());

            List<HistoricActivityInstance> gtwExecution = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("parallelGateway")
                .list();
            //Two flows to join in, fork was not executed
            assertThat(gtwExecution).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("parallelJoin", "parallelJoin");
            assertThat(gtwExecution).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procParallelGtw.getId());

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();
                assertThat(historicTasks).extracting(HistoricTaskInstance::getTaskDefinitionKey).containsExactlyInAnyOrder("userTask1Id", "oddFlowTask3", "evenFlowTask2", "evenFlowTask4", "taskAfter");
                assertThat(historicTasks).extracting(HistoricTaskInstance::getProcessDefinitionId).containsOnly(procParallelGtw.getId());
            }
        }

        assertProcessEnded(processInstance.getId());
    }

    //-- Inclusive Gateway
    @Test
    public void testMigrateActivityToInclusiveGatewaySplitWithDefault() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procInclusiveGtw = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/inclusive-gateway-fork-and-join-with-default.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());

        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processInstanceMigrationService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procInclusiveGtw.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", Arrays.asList("gwFork")));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("taskEquals");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procInclusiveGtw.getId());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("taskEquals");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());

        //Complete gateway tasks
        tasks.forEach(this::completeTask);

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("taskAfter");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procInclusiveGtw.getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactly("taskAfter");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());

        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            List<HistoricActivityInstance> taskExecutions = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("userTask")
                .list();
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("userTask1Id", "taskEquals", "taskAfter");
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());

            List<HistoricActivityInstance> gtwExecution = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("inclusiveGateway")
                .list();
            assertThat(gtwExecution).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("gwJoin", "gwFork");
            assertThat(gtwExecution).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();
                assertThat(historicTasks).extracting(HistoricTaskInstance::getTaskDefinitionKey).containsExactlyInAnyOrder("userTask1Id", "taskEquals", "taskAfter");
                assertThat(historicTasks).extracting(HistoricTaskInstance::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());
            }
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityToInclusiveGatewaySplitNoOutgoingSequenceSelectionException() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procInclusiveGtw = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/inclusive-gateway-fork-and-join.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());

        try {
            ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processInstanceMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procInclusiveGtw.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", Arrays.asList("gwFork")).withLocalVariableForAllActivities("myConditionVar", 10));

            ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
            assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

            processInstanceMigrationBuilder.migrate(processInstance.getId());

            fail("No outgoing sequence for the inclusive gateway could've been selected");
        } catch (FlowableException e) {
            assertTextPresent("No outgoing sequence flow of element 'gwFork' could be selected for continuing the process", e.getMessage());
        }
    }

    @Test
    public void testMigrateActivityToInclusiveGatewaySplitWithConditionSpecifiedBeforeMigration() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procInclusiveGtw = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/inclusive-gateway-fork-and-join.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());

        runtimeService.setVariable(executions.get(0).getId(), "myConditionVar", 11);

        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processInstanceMigrationService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procInclusiveGtw.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", Arrays.asList("gwFork")));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("taskMore");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procInclusiveGtw.getId());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("taskMore");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());

        //Complete gateway tasks
        tasks.forEach(this::completeTask);

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("taskAfter");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procInclusiveGtw.getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactly("taskAfter");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());

        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            List<HistoricActivityInstance> taskExecutions = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("userTask")
                .list();
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("userTask1Id", "taskMore", "taskAfter");
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());

            List<HistoricActivityInstance> gtwExecution = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("inclusiveGateway")
                .list();
            assertThat(gtwExecution).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("gwJoin", "gwFork");
            assertThat(gtwExecution).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();
                assertThat(historicTasks).extracting(HistoricTaskInstance::getTaskDefinitionKey).containsExactlyInAnyOrder("userTask1Id", "taskMore", "taskAfter");
                assertThat(historicTasks).extracting(HistoricTaskInstance::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());
            }
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityToInclusiveGatewaySplitWithConditionSpecifiedDuringMigration() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procInclusiveGtw = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/inclusive-gateway-fork-and-join.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());

        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processInstanceMigrationService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procInclusiveGtw.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", Arrays.asList("gwFork")).withLocalVariableForAllActivities("myConditionVar", 11));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("taskMore");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procInclusiveGtw.getId());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("taskMore");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());

        //Complete gateway tasks
        tasks.forEach(this::completeTask);

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("taskAfter");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procInclusiveGtw.getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactly("taskAfter");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());

        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            List<HistoricActivityInstance> taskExecutions = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("userTask")
                .list();
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("userTask1Id", "taskMore", "taskAfter");
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());

            List<HistoricActivityInstance> gtwExecution = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("inclusiveGateway")
                .list();
            assertThat(gtwExecution).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("gwJoin", "gwFork");
            assertThat(gtwExecution).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();
                assertThat(historicTasks).extracting(HistoricTaskInstance::getTaskDefinitionKey).containsExactlyInAnyOrder("taskMore", "userTask1Id", "taskAfter");
                assertThat(historicTasks).extracting(HistoricTaskInstance::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());
            }
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityToInclusiveGatewaySplitWithConditionSpecifiedAsProcessVariableDuringMigration() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procInclusiveGtw = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/inclusive-gateway-fork-and-join.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());

        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processInstanceMigrationService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procInclusiveGtw.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", Arrays.asList("gwFork")))
            .withProcessInstanceVariable("myConditionVar", 11);

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("taskMore");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procInclusiveGtw.getId());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("taskMore");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());

        //Complete gateway tasks
        tasks.forEach(this::completeTask);

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("taskAfter");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procInclusiveGtw.getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactly("taskAfter");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());

        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            List<HistoricActivityInstance> taskExecutions = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("userTask")
                .list();
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("userTask1Id", "taskMore", "taskAfter");
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());

            List<HistoricActivityInstance> gtwExecution = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("inclusiveGateway")
                .list();
            assertThat(gtwExecution).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("gwJoin", "gwFork");
            assertThat(gtwExecution).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();
                assertThat(historicTasks).extracting(HistoricTaskInstance::getTaskDefinitionKey).containsExactlyInAnyOrder("userTask1Id", "taskAfter", "taskMore");
                assertThat(historicTasks).extracting(HistoricTaskInstance::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());
            }
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityToOneActivityInsideInclusiveGateway() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procInclusiveGtw = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/inclusive-gateway-fork-and-join-with-default.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());

        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processInstanceMigrationService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procInclusiveGtw.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", Arrays.asList("taskLess")));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("taskLess");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procInclusiveGtw.getId());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("taskLess");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());

        //Complete the task
        completeTask(tasks.get(0));

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("taskAfter");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procInclusiveGtw.getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactly("taskAfter");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());

        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            List<HistoricActivityInstance> taskExecutions = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("userTask")
                .list();
            //Direct migration
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("taskLess", "taskAfter");
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());

            List<HistoricActivityInstance> gtwExecution = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("inclusiveGateway")
                .list();
            //Join gateway only
            assertThat(gtwExecution).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("gwJoin");
            assertThat(gtwExecution).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();
                assertThat(historicTasks).extracting(HistoricTaskInstance::getTaskDefinitionKey).containsExactlyInAnyOrder("taskAfter", "taskLess");
                assertThat(historicTasks).extracting(HistoricTaskInstance::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());
            }
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityToParallelActivitiesInsideInclusiveGateway() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procInclusiveGtw = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/inclusive-gateway-fork-and-join.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());

        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processInstanceMigrationService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procInclusiveGtw.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", Arrays.asList("taskMore", "taskLess")));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("taskMore", "taskLess");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procInclusiveGtw.getId());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("taskMore", "taskLess");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());

        //Complete one sequence
        task = tasks.stream().filter(t -> t.getTaskDefinitionKey().equals("taskMore")).findFirst().get();
        completeTask(task);

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("gwJoin", "taskLess");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procInclusiveGtw.getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactly("taskLess");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());

        //Complete the rest
        tasks.forEach(this::completeTask);

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("taskAfter");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procInclusiveGtw.getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactly("taskAfter");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());

        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            List<HistoricActivityInstance> taskExecutions = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("userTask")
                .list();
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("userTask1Id", "taskLess", "taskMore", "taskAfter");
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());

            List<HistoricActivityInstance> gtwExecution = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("inclusiveGateway")
                .list();
            //One join per parallel task
            assertThat(gtwExecution).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("gwJoin", "gwJoin");
            assertThat(gtwExecution).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();
                assertThat(historicTasks).extracting(HistoricTaskInstance::getTaskDefinitionKey).containsExactlyInAnyOrder("userTask1Id", "taskLess", "taskMore", "taskAfter");
                assertThat(historicTasks).extracting(HistoricTaskInstance::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());
            }
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateParallelActivitiesToParallelActivitiesInsideInclusiveGateway() {
        ProcessDefinition procParallelTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/timer-parallel-task.bpmn20.xml");
        ProcessDefinition procInclusiveGtw = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/inclusive-gateway-fork-multiple-outgoing-sequences.bpmn20.xml");

        //Start the processInstance
        //Start the processInstance and spawn the parallel task
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procParallelTask.getId());

        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("timerBound", "processTask", "parallelTask");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procParallelTask.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("processTask", "parallelTask");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procParallelTask.getId());

        //Migrate each of the parallel task to a task in the parallel gateway
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processInstanceMigrationService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procInclusiveGtw.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("processTask", "taskMore"))
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("parallelTask", "taskLess"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("taskMore", "taskLess");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procInclusiveGtw.getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("taskMore", "taskLess");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());

        //Complete one sequence
        Task task = tasks.stream().filter(t -> t.getTaskDefinitionKey().equals("taskMore")).findFirst().get();
        completeTask(task);

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("gwJoin", "taskLess");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procInclusiveGtw.getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactly("taskLess");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());

        //Complete the rest
        tasks.forEach(this::completeTask);

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("taskAfter");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procInclusiveGtw.getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactly("taskAfter");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());

        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            List<HistoricActivityInstance> taskExecutions = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("userTask")
                .list();
            //Direct Migrations
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("taskMore", "taskLess", "taskAfter");
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());

            List<HistoricActivityInstance> gtwExecution = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("inclusiveGateway")
                .list();
            //One join per parallel task
            assertThat(gtwExecution).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("gwJoin", "gwJoin");
            assertThat(gtwExecution).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();
                assertThat(historicTasks).extracting(HistoricTaskInstance::getTaskDefinitionKey).containsExactlyInAnyOrder("taskMore", "taskLess", "taskAfter");
                assertThat(historicTasks).extracting(HistoricTaskInstance::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());
            }
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateInclusiveGatewayParallelActivitiesToSingleActivity() {
        ProcessDefinition procInclusiveGtw = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/inclusive-gateway-fork-multiple-outgoing-sequences.bpmn20.xml");
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procInclusiveGtw.getId(), Collections.singletonMap("myConditionVar", 10));
        completeTask(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("taskMore", "taskLess");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procInclusiveGtw.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("taskMore", "taskLess");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());

        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processInstanceMigrationService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procDefOneTask.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor(Arrays.asList("taskMore", "taskLess"), "userTask1Id"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("userTask1Id");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procDefOneTask.getId());

        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            List<HistoricActivityInstance> taskExecutions = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("userTask")
                .list();
            //Direct Migrations
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("taskBefore", "taskMore", "taskLess", "userTask1Id");
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procDefOneTask.getId());

            List<HistoricActivityInstance> gtwExecution = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("inclusiveGateway")
                .list();
            //Only the fork was executed
            assertThat(gtwExecution).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("gwFork");
            assertThat(gtwExecution).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procDefOneTask.getId());

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();
                assertThat(historicTasks).extracting(HistoricTaskInstance::getTaskDefinitionKey).containsExactlyInAnyOrder("taskBefore", "taskMore", "taskLess", "userTask1Id");
                assertThat(historicTasks).extracting(HistoricTaskInstance::getProcessDefinitionId).containsOnly(procDefOneTask.getId());
            }
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityToInclusiveGatewaySplitWithDefaultInsideEmbeddedSubProcess() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procInclusiveGtw = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/inclusive-gateway-fork-and-join-with-default-nested-in-embedded-subprocess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());

        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processInstanceMigrationService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procInclusiveGtw.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", Arrays.asList("gwFork")));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("subProcess", "taskEquals");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procInclusiveGtw.getId());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("taskEquals");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());

        //Complete gateway tasks
        tasks.forEach(this::completeTask);

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("taskAfter");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procInclusiveGtw.getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactly("taskAfter");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());

        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            List<HistoricActivityInstance> taskExecutions = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("userTask")
                .list();
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("userTask1Id", "taskEquals", "taskAfter");
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());

            List<HistoricActivityInstance> subProcExecution = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("subProcess")
                .list();
            assertThat(subProcExecution).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("subProcess");
            assertThat(subProcExecution).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());

            List<HistoricActivityInstance> gtwExecution = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("inclusiveGateway")
                .list();
            assertThat(gtwExecution).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("gwJoin", "gwFork");
            assertThat(gtwExecution).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();
                assertThat(historicTasks).extracting(HistoricTaskInstance::getTaskDefinitionKey).containsExactlyInAnyOrder("userTask1Id", "taskEquals", "taskAfter");
                assertThat(historicTasks).extracting(HistoricTaskInstance::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());
            }
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityToOneActivityInsideInclusiveGatewayInsideEmbeddedSubProcess() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procInclusiveGtw = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/inclusive-gateway-fork-and-join-with-default-nested-in-embedded-subprocess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());

        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processInstanceMigrationService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procInclusiveGtw.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", Arrays.asList("taskLess")));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("subProcess", "taskLess");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procInclusiveGtw.getId());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("taskLess");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());

        //Complete the task
        completeTask(tasks.get(0));

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("taskAfter");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procInclusiveGtw.getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactly("taskAfter");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());

        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            List<HistoricActivityInstance> taskExecutions = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("userTask")
                .list();
            //Direct migration
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("taskLess", "taskAfter");
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());

            List<HistoricActivityInstance> subProcExecution = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("subProcess")
                .list();
            assertThat(subProcExecution).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("subProcess");
            assertThat(subProcExecution).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());

            List<HistoricActivityInstance> gtwExecution = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("inclusiveGateway")
                .list();
            //Join gateway only
            assertThat(gtwExecution).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("gwJoin");
            assertThat(gtwExecution).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();
                assertThat(historicTasks).extracting(HistoricTaskInstance::getTaskDefinitionKey).containsExactlyInAnyOrder("taskAfter", "taskLess");
                assertThat(historicTasks).extracting(HistoricTaskInstance::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());
            }
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityToParallelActivitiesInsideInclusiveGatewayInsideEmbeddedSubProcess() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procInclusiveGtw = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/inclusive-gateway-fork-and-join-with-default-nested-in-embedded-subprocess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());

        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processInstanceMigrationService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procInclusiveGtw.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", Arrays.asList("taskMore", "taskLess")));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("subProcess", "taskMore", "taskLess");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procInclusiveGtw.getId());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("taskMore", "taskLess");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());

        //Complete one sequence
        task = tasks.stream().filter(t -> t.getTaskDefinitionKey().equals("taskMore")).findFirst().get();
        completeTask(task);

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("subProcess", "gwJoin", "taskLess");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procInclusiveGtw.getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactly("taskLess");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());

        //Complete the rest
        tasks.forEach(this::completeTask);

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("taskAfter");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procInclusiveGtw.getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactly("taskAfter");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());

        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            List<HistoricActivityInstance> taskExecutions = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("userTask")
                .list();
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("userTask1Id", "taskLess", "taskMore", "taskAfter");
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());

            List<HistoricActivityInstance> subProcExecution = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("subProcess")
                .list();
            assertThat(subProcExecution).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("subProcess");
            assertThat(subProcExecution).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());

            List<HistoricActivityInstance> gtwExecution = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("inclusiveGateway")
                .list();
            //One join per parallel task
            assertThat(gtwExecution).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("gwJoin", "gwJoin");
            assertThat(gtwExecution).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();
                assertThat(historicTasks).extracting(HistoricTaskInstance::getTaskDefinitionKey).containsExactlyInAnyOrder("userTask1Id", "taskLess", "taskMore", "taskAfter");
                assertThat(historicTasks).extracting(HistoricTaskInstance::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());
            }
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateInclusiveGatewayParallelActivitiesInsideEmbeddedSubProcessToSingleActivity() {
        ProcessDefinition procInclusiveGtw = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/inclusive-gateway-fork-multiple-outgoing-sequences-nested-in-embedded-subprocess.bpmn20.xml");
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procInclusiveGtw.getId(), Collections.singletonMap("myConditionVar", 10));
        completeTask(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("subProcess", "taskMore", "taskLess");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procInclusiveGtw.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("taskMore", "taskLess");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procInclusiveGtw.getId());

        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processInstanceMigrationService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procDefOneTask.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor(Arrays.asList("taskMore", "taskLess"), "userTask1Id"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactlyInAnyOrder("userTask1Id");
        assertThat(tasks).extracting(Task::getProcessDefinitionId).containsOnly(procDefOneTask.getId());

        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            List<HistoricActivityInstance> taskExecutions = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("userTask")
                .list();
            //Direct Migrations
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("taskBefore", "taskMore", "taskLess", "userTask1Id");
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procDefOneTask.getId());

            List<HistoricActivityInstance> gtwExecution = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("inclusiveGateway")
                .list();
            //Only the fork was executed
            assertThat(gtwExecution).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("gwFork");
            assertThat(gtwExecution).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procDefOneTask.getId());

            List<HistoricActivityInstance> subProcExecution = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("subProcess")
                .list();
            assertThat(subProcExecution).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("subProcess");
            assertThat(subProcExecution).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procDefOneTask.getId());

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();
                assertThat(historicTasks).extracting(HistoricTaskInstance::getTaskDefinitionKey).containsExactlyInAnyOrder("taskBefore", "taskMore", "taskLess", "userTask1Id");
                assertThat(historicTasks).extracting(HistoricTaskInstance::getProcessDefinitionId).containsOnly(procDefOneTask.getId());
            }
        }

        assertProcessEnded(processInstance.getId());
    }

}
