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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.impl.DefaultTenantProvider;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.delegate.event.FlowableActivityEvent;
import org.flowable.engine.delegate.event.FlowableMessageEvent;
import org.flowable.engine.delegate.event.FlowableSignalEvent;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricDetail;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.HistoricDetailVariableInstanceUpdateEntity;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.migration.ActivityMigrationMapping;
import org.flowable.engine.migration.ProcessInstanceMigrationBuilder;
import org.flowable.engine.migration.ProcessInstanceMigrationDocument;
import org.flowable.engine.migration.ProcessInstanceMigrationDocumentConverter;
import org.flowable.engine.migration.ProcessInstanceMigrationValidationResult;
import org.flowable.engine.migration.Script;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.DataObject;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.api.runtime.changestate.ChangeStateEventListener;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntity;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ProcessInstanceMigrationTest extends AbstractProcessInstanceMigrationTest {

    private ChangeStateEventListener changeStateEventListener = new ChangeStateEventListener();

    @BeforeEach
    protected void setUp() {
        processEngine.getRuntimeService().addEventListener(changeStateEventListener);
    }

    @AfterEach
    protected void tearDown() {
        processEngine.getRuntimeService().removeEventListener(changeStateEventListener);
        deleteDeployments();
    }

    @Test
    public void testSimpleMigrationWithActivityAutoMapping() {
        //Deploy first version of the process
        ProcessDefinition version1ProcessDef = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");

        //Start and instance of the recent first version of the process for migration and one for reference
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("MP");

        //Deploy second version of the process
        ProcessDefinition version2ProcessDef = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/two-tasks-simple-process.bpmn20.xml");

        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey("MP")
                .list();

        assertThat(processDefinitions)
                .extracting(ProcessDefinition::getId)
                .containsExactlyInAnyOrder(version1ProcessDef.getId(), version2ProcessDef.getId());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(executions).hasSize(2); //includes root execution
        executions.stream()
                .map(e -> (ExecutionEntity) e)
                .forEach(e -> assertThat(e.getProcessDefinitionId()).isEqualTo(version1ProcessDef.getId()));

        List<Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getProcessDefinitionId, Task::getTaskDefinitionKey)
                .containsExactly(tuple(version1ProcessDef.getId(), "userTask1Id"));

        ProcessInstanceMigrationValidationResult validationResult = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(version2ProcessDef.getId())
                .validateMigration(processInstance.getId());

        assertThat(validationResult.isMigrationValid()).isTrue();

        // Migrate process
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(version2ProcessDef.getId());
        ProcessInstanceMigrationValidationResult processInstanceMigrationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        executions = runtimeService.createExecutionQuery().list();
        assertThat(executions).hasSize(2); //includes root execution
        executions.stream()
                .map(e -> (ExecutionEntity) e)
                .forEach(e -> assertThat(e.getProcessDefinitionId()).isEqualTo(version2ProcessDef.getId()));

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getProcessDefinitionId, Task::getTaskDefinitionKey)
                .containsExactly(tuple(version2ProcessDef.getId(), "userTask1Id")); //AutoMapped by Id

        //The first process version only had one activity, there should be a second activity in the process now
        taskService.complete(tasks.get(0).getId());
        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("userTask2Id");
        taskService.complete(tasks.get(0).getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testSimpleMigrationWithTaskMapping() {
        //Deploy first version of the process
        deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/two-tasks-simple-process.bpmn20.xml");

        //Start and instance of the recent first version of the process for migration and one for reference
        ProcessInstance processInstanceToMigrate = runtimeService.startProcessInstanceByKey("MP");
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstanceToMigrate.getId()).singleResult().getId());

        Task beforeMigrationTask = taskService.createTaskQuery().processInstanceId(processInstanceToMigrate.getId()).singleResult();

        //Deploy second version of the process
        ProcessDefinition version2ProcessDef = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/three-tasks-simple-process.bpmn20.xml");

        //Migrate process
        processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(version2ProcessDef.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask2Id", "intermediateTask"))
                .migrate(processInstanceToMigrate.getId());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstanceToMigrate.getId()).list();
        assertThat(executions).hasSize(2); //includes root execution
        for (Execution execution : executions) {
            assertThat(((ExecutionEntity) execution).getProcessDefinitionId()).isEqualTo(version2ProcessDef.getId());
        }

        Task task = taskService.createTaskQuery().processInstanceId(processInstanceToMigrate.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getProcessDefinitionId, Task::getTaskDefinitionKey, Task::getId)
                .containsExactly(version2ProcessDef.getId(), "intermediateTask", beforeMigrationTask.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstanceToMigrate.getId()).singleResult();
            assertThat(historicProcessInstance.getProcessDefinitionId()).isEqualTo(version2ProcessDef.getId());

            List<HistoricTaskInstance> historicTaskInstances = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstanceToMigrate.getId()).list();
            assertThat(historicTaskInstances).hasSize(2);
            for (HistoricTaskInstance historicTaskInstance : historicTaskInstances) {
                assertThat(historicTaskInstance.getProcessDefinitionId()).isEqualTo(version2ProcessDef.getId());
            }

            List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstanceToMigrate.getId()).list();
            assertThat(historicActivityInstances).hasSize(5);
            for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
                assertThat(historicActivityInstance.getProcessDefinitionId()).isEqualTo(version2ProcessDef.getId());
            }
        }

        // complete intermediate task
        taskService.complete(task.getId());

        // complete final task
        task = taskService.createTaskQuery().processInstanceId(processInstanceToMigrate.getId()).singleResult();
        taskService.complete(task.getId());

        assertProcessEnded(processInstanceToMigrate.getId());
    }

    @Test
    public void testSimpleMigrationWithTaskJsonMapping() {
        //Deploy first version of the process
        deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/two-tasks-simple-process.bpmn20.xml");

        //Start and instance of the recent first version of the process for migration and one for reference
        ProcessInstance processInstanceToMigrate = runtimeService.startProcessInstanceByKey("MP");
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstanceToMigrate.getId()).singleResult().getId());

        Task beforeMigrationTask = taskService.createTaskQuery().processInstanceId(processInstanceToMigrate.getId()).singleResult();

        //Deploy second version of the process
        ProcessDefinition version2ProcessDef = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/three-tasks-simple-process.bpmn20.xml");

        ObjectMapper objectMapper = processEngineConfiguration.getObjectMapper();
        ObjectNode migrationNode = objectMapper.createObjectNode();
        migrationNode.put("toProcessDefinitionId", version2ProcessDef.getId());
        ArrayNode activitiesNode = migrationNode.putArray("activityMappings");
        ObjectNode activityNode = activitiesNode.addObject();
        activityNode.put("fromActivityId", "userTask2Id");
        activityNode.put("toActivityId", "intermediateTask");

        //Migrate process
        ProcessInstanceMigrationDocument migrationDocument = ProcessInstanceMigrationDocumentConverter.convertFromJson(migrationNode.toString());
        processMigrationService.migrateProcessInstance(processInstanceToMigrate.getId(), migrationDocument);

        Task task = taskService.createTaskQuery().processInstanceId(processInstanceToMigrate.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getProcessDefinitionId, Task::getTaskDefinitionKey, Task::getId)
                .containsExactly(version2ProcessDef.getId(), "intermediateTask", beforeMigrationTask.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstanceToMigrate.getId()).singleResult();
            assertThat(historicProcessInstance.getProcessDefinitionId()).isEqualTo(version2ProcessDef.getId());

            List<HistoricTaskInstance> historicTaskInstances = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstanceToMigrate.getId()).list();
            assertThat(historicTaskInstances).hasSize(2);
            for (HistoricTaskInstance historicTaskInstance : historicTaskInstances) {
                assertThat(historicTaskInstance.getProcessDefinitionId()).isEqualTo(version2ProcessDef.getId());
            }

            List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstanceToMigrate.getId()).list();
            assertThat(historicActivityInstances).hasSize(5);
            for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
                assertThat(historicActivityInstance.getProcessDefinitionId()).isEqualTo(version2ProcessDef.getId());
            }
        }

        // complete intermediate task
        taskService.complete(task.getId());

        // complete final task
        task = taskService.createTaskQuery().processInstanceId(processInstanceToMigrate.getId()).singleResult();
        taskService.complete(task.getId());

        assertProcessEnded(processInstanceToMigrate.getId());
    }

    @Test
    public void testMigrationWithParallelTaskMapping() {
        //Deploy first version of the process
        deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/parallel-gateway-two-tasks.bpmn20.xml");

        //Start and instance of the recent first version of the process for migration and one for reference
        ProcessInstance processInstanceToMigrate = runtimeService.startProcessInstanceByKey("startParallelProcess");
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstanceToMigrate.getId()).singleResult().getId());

        List<Task> parallelTasks = taskService.createTaskQuery().processInstanceId(processInstanceToMigrate.getId()).list();
        assertThat(parallelTasks).hasSize(2);

        //Deploy second version of the process
        ProcessDefinition version2ProcessDef = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/parallel-gateway-two-tasks-and-before.bpmn20.xml");

        List<String> fromActivityIds = new ArrayList<>();
        fromActivityIds.add("parallelTask1");
        fromActivityIds.add("parallelTask2");
        //Migrate process
        processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(version2ProcessDef.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor(fromActivityIds, "beforeTask"))
                .migrate(processInstanceToMigrate.getId());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstanceToMigrate.getId()).list();
        assertThat(executions).hasSize(2); //includes root execution
        for (Execution execution : executions) {
            assertThat(((ExecutionEntity) execution).getProcessDefinitionId()).isEqualTo(version2ProcessDef.getId());
        }

        Task task = taskService.createTaskQuery().processInstanceId(processInstanceToMigrate.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getProcessDefinitionId, Task::getTaskDefinitionKey)
                .containsExactly(version2ProcessDef.getId(), "beforeTask");
        assertThat(parallelTasks.get(0).getId()).isNotEqualTo(task.getId());
        assertThat(parallelTasks.get(1).getId()).isNotEqualTo(task.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstanceToMigrate.getId()).singleResult();
            assertThat(historicProcessInstance.getProcessDefinitionId()).isEqualTo(version2ProcessDef.getId());

            List<HistoricTaskInstance> historicTaskInstances = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstanceToMigrate.getId()).list();
            assertThat(historicTaskInstances).hasSize(4);
            for (HistoricTaskInstance historicTaskInstance : historicTaskInstances) {
                assertThat(historicTaskInstance.getProcessDefinitionId()).isEqualTo(version2ProcessDef.getId());
            }

            List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstanceToMigrate.getId()).list();
            assertThat(historicActivityInstances).hasSize(10);
            for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
                assertThat(historicActivityInstance.getProcessDefinitionId()).isEqualTo(version2ProcessDef.getId());
            }
        }

        // complete before task
        taskService.complete(task.getId());

        // complete parallel task 1
        task = taskService.createTaskQuery().processInstanceId(processInstanceToMigrate.getId()).list().get(0);
        taskService.complete(task.getId());

        // complete parallel task 2
        task = taskService.createTaskQuery().processInstanceId(processInstanceToMigrate.getId()).singleResult();
        taskService.complete(task.getId());

        // complete final task
        task = taskService.createTaskQuery().processInstanceId(processInstanceToMigrate.getId()).singleResult();
        taskService.complete(task.getId());

        assertProcessEnded(processInstanceToMigrate.getId());
    }

    @Test
    public void testMigrationWithNewSubProcessScope() {
        //Deploy first version of the process
        deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/two-tasks-simple-process.bpmn20.xml");

        //Start and instance of the recent first version of the process for migration and one for reference
        ProcessInstance processInstanceToMigrate = runtimeService.startProcessInstanceByKey("MP");
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstanceToMigrate.getId()).singleResult().getId());

        Task lastTask = taskService.createTaskQuery().processInstanceId(processInstanceToMigrate.getId()).singleResult();

        //Deploy second version of the process
        ProcessDefinition version2ProcessDef = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/three-tasks-with-sub-process.bpmn20.xml");

        //Migrate process
        processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(version2ProcessDef.getId())
                .addActivityMigrationMapping(
                        ActivityMigrationMapping.createMappingFor("userTask2Id", "subScriptTask")
                                .withLocalVariable("subprocessVariable", "passedValue"))
                .migrate(processInstanceToMigrate.getId());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstanceToMigrate.getId()).list();
        assertThat(executions).hasSize(3); //includes root execution
        for (Execution execution : executions) {
            assertThat(((ExecutionEntity) execution).getProcessDefinitionId()).isEqualTo(version2ProcessDef.getId());
        }

        Execution execution = runtimeService.createExecutionQuery().activityId("subProcess").singleResult();
        assertThat(runtimeService.getVariable(execution.getId(), "subprocessVariable")).isEqualTo("passedValue");
        assertThat(runtimeService.hasVariable(execution.getProcessInstanceId(), "subprocessVariable")).isFalse();
        assertThat(runtimeService.getVariable(execution.getId(), "anotherVar")).isEqualTo("hello");

        assertThat(runtimeService.hasVariable(execution.getProcessInstanceId(), "anotherVar")).isFalse();

        Task task = taskService.createTaskQuery().processInstanceId(processInstanceToMigrate.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getProcessDefinitionId, Task::getTaskDefinitionKey)
                .containsExactly(version2ProcessDef.getId(), "subTask");
        assertThat(lastTask.getId()).isNotEqualTo(task.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstanceToMigrate.getId()).singleResult();
            assertThat(historicProcessInstance.getProcessDefinitionId()).isEqualTo(version2ProcessDef.getId());

            List<HistoricTaskInstance> historicTaskInstances = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstanceToMigrate.getId()).list();
            assertThat(historicTaskInstances).hasSize(3);
            for (HistoricTaskInstance historicTaskInstance : historicTaskInstances) {
                assertThat(historicTaskInstance.getProcessDefinitionId()).isEqualTo(version2ProcessDef.getId());
            }

            List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstanceToMigrate.getId()).list();
            assertThat(historicActivityInstances).hasSize(9);
            for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
                assertThat(historicActivityInstance.getProcessDefinitionId()).isEqualTo(version2ProcessDef.getId());
            }
        }

        // complete sub task
        taskService.complete(task.getId());

        // complete final task
        task = taskService.createTaskQuery().processInstanceId(processInstanceToMigrate.getId()).singleResult();
        taskService.complete(task.getId());

        assertProcessEnded(processInstanceToMigrate.getId());
    }

    @Test
    public void testSimpleMigrationOfProcessInstancesById() {
        //Deploy first version of the process
        Deployment oneActivityProcessDeployment = repositoryService.createDeployment()
                .name("My Process Deployment")
                .addClasspathResource("org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml")
                .deploy();

        //Start and instance of the recent first version of the process for migration and one for reference
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("MP");

        //Deploy second version of the process
        Deployment oneActivityProcessDeploymentV2 = repositoryService.createDeployment()
                .name("My Process Deployment")
                .addClasspathResource("org/flowable/engine/test/api/runtime/migration/one-task-simple-process-v2.bpmn20.xml")
                .deploy();

        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey("MP")
                .list();

        assertThat(processDefinitions).hasSize(2);

        ProcessDefinition version1ProcessDef = processDefinitions.stream().filter(d -> d.getVersion() == 1).findFirst().get();
        assertThat(version1ProcessDef.getDeploymentId()).isEqualTo(oneActivityProcessDeployment.getId());
        ProcessDefinition version2ProcessDef = processDefinitions.stream().filter(d -> d.getVersion() == 2).findFirst().get();
        assertThat(version2ProcessDef.getDeploymentId()).isEqualTo(oneActivityProcessDeploymentV2.getId());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(executions).hasSize(2); //includes root execution
        executions.stream()
                .map(e -> (ExecutionEntity) e)
                .forEach(e -> assertThat(e.getProcessDefinitionId()).isEqualTo(version1ProcessDef.getId()));

        List<Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getProcessDefinitionId()).isEqualTo(version1ProcessDef.getId());
        assertThat(tasks.get(0).getTaskDefinitionKey()).isEqualTo("userTask1Id");
        assertThat(tasks)
                .extracting(Task::getProcessDefinitionId, Task::getTaskDefinitionKey)
                .containsExactly(tuple(version1ProcessDef.getId(), "userTask1Id"));

        ProcessInstanceMigrationValidationResult validationResult = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(version2ProcessDef.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "theTask"))
                .validateMigration(processInstance.getId());

        assertThat(validationResult.isMigrationValid()).isTrue();

        //Migrate process - moving all instances
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(version2ProcessDef.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "theTask"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationResult = processInstanceMigrationBuilder.validateMigrationOfProcessInstances(version1ProcessDef.getId());
        assertThat(processInstanceMigrationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrateProcessInstances(version1ProcessDef.getId());

        executions = runtimeService.createExecutionQuery().list();
        assertThat(executions).hasSize(2); //includes root execution
        executions.stream()
                .map(e -> (ExecutionEntity) e)
                .forEach(e -> assertThat(e.getProcessDefinitionId()).isEqualTo(version2ProcessDef.getId()));

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getProcessDefinitionId, Task::getTaskDefinitionKey)
                .containsExactly(tuple(version2ProcessDef.getId(), "theTask"));

        taskService.complete(tasks.get(0).getId());
        assertProcessEnded(processInstance.getId());
    }
    
    @Test
    public void testSimpleMigrationOfProcessInstancesByKey() {
        //Deploy first version of the process
        Deployment oneActivityProcessDeployment = repositoryService.createDeployment()
                .name("My Process Deployment")
                .addClasspathResource("org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml")
                .deploy();

        //Start and instance of the recent first version of the process for migration and one for reference
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("MP");

        //Deploy second version of the process
        Deployment oneActivityProcessDeploymentV2 = repositoryService.createDeployment()
                .name("My Process Deployment")
                .addClasspathResource("org/flowable/engine/test/api/runtime/migration/one-task-simple-process-v2.bpmn20.xml")
                .deploy();

        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey("MP")
                .list();

        assertThat(processDefinitions).hasSize(2);

        ProcessDefinition version1ProcessDef = processDefinitions.stream().filter(d -> d.getVersion() == 1).findFirst().get();
        assertThat(version1ProcessDef.getDeploymentId()).isEqualTo(oneActivityProcessDeployment.getId());
        ProcessDefinition version2ProcessDef = processDefinitions.stream().filter(d -> d.getVersion() == 2).findFirst().get();
        assertThat(version2ProcessDef.getDeploymentId()).isEqualTo(oneActivityProcessDeploymentV2.getId());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(executions).hasSize(2); //includes root execution
        executions.stream()
                .map(e -> (ExecutionEntity) e)
                .forEach(e -> assertThat(e.getProcessDefinitionId()).isEqualTo(version1ProcessDef.getId()));

        List<Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getProcessDefinitionId()).isEqualTo(version1ProcessDef.getId());
        assertThat(tasks.get(0).getTaskDefinitionKey()).isEqualTo("userTask1Id");
        assertThat(tasks)
                .extracting(Task::getProcessDefinitionId, Task::getTaskDefinitionKey)
                .containsExactly(tuple(version1ProcessDef.getId(), "userTask1Id"));

        ProcessInstanceMigrationValidationResult validationResult = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(version2ProcessDef.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "theTask"))
                .validateMigration(processInstance.getId());

        assertThat(validationResult.isMigrationValid()).isTrue();

        //Migrate process - moving all instances
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(version2ProcessDef.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "theTask"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationResult = processInstanceMigrationBuilder.validateMigrationOfProcessInstances("MP", 1, null);
        assertThat(processInstanceMigrationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrateProcessInstances("MP", 1, null);

        executions = runtimeService.createExecutionQuery().list();
        assertThat(executions).hasSize(2); //includes root execution
        executions.stream()
                .map(e -> (ExecutionEntity) e)
                .forEach(e -> assertThat(e.getProcessDefinitionId()).isEqualTo(version2ProcessDef.getId()));

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getProcessDefinitionId, Task::getTaskDefinitionKey)
                .containsExactly(tuple(version2ProcessDef.getId(), "theTask"));

        taskService.complete(tasks.get(0).getId());
        assertProcessEnded(processInstance.getId());
    }
    
    @Test
    public void testSimpleMigrationWithExplicitActivityMapping1() {
        //Deploy first version of the process
        Deployment oneActivityProcessDeployment = repositoryService.createDeployment()
                .name("My Process Deployment")
                .addClasspathResource("org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml")
                .deploy();

        //Start and instance of the recent first version of the process for migration and one for reference
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("MP");

        //Deploy second version of the process
        Deployment twoActivitiesProcessDeployment = repositoryService.createDeployment()
                .name("My Process Deployment")
                .addClasspathResource("org/flowable/engine/test/api/runtime/migration/two-tasks-simple-process.bpmn20.xml")
                .deploy();

        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey("MP")
                .list();

        assertThat(processDefinitions).hasSize(2);

        ProcessDefinition version1ProcessDef = processDefinitions.stream().filter(d -> d.getVersion() == 1).findFirst().get();
        assertThat(version1ProcessDef.getDeploymentId()).isEqualTo(oneActivityProcessDeployment.getId());
        ProcessDefinition version2ProcessDef = processDefinitions.stream().filter(d -> d.getVersion() == 2).findFirst().get();
        assertThat(version2ProcessDef.getDeploymentId()).isEqualTo(twoActivitiesProcessDeployment.getId());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(executions).hasSize(2); //includes root execution
        executions.stream()
                .map(e -> (ExecutionEntity) e)
                .forEach(e -> assertThat(e.getProcessDefinitionId()).isEqualTo(version1ProcessDef.getId()));

        List<Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getProcessDefinitionId()).isEqualTo(version1ProcessDef.getId());
        assertThat(tasks.get(0).getTaskDefinitionKey()).isEqualTo("userTask1Id");
        assertThat(tasks)
                .extracting(Task::getProcessDefinitionId, Task::getTaskDefinitionKey)
                .containsExactly(tuple(version1ProcessDef.getId(), "userTask1Id"));

        ProcessInstanceMigrationValidationResult validationResult = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(version2ProcessDef.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "userTask1Id"))
                .validateMigration(processInstance.getId());

        assertThat(validationResult.isMigrationValid()).isTrue();

        //Migrate process - moving the current execution explicitly
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(version2ProcessDef.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "userTask1Id"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        executions = runtimeService.createExecutionQuery().list();
        assertThat(executions).hasSize(2); //includes root execution
        executions.stream()
                .map(e -> (ExecutionEntity) e)
                .forEach(e -> assertThat(e.getProcessDefinitionId()).isEqualTo(version2ProcessDef.getId()));

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getProcessDefinitionId, Task::getTaskDefinitionKey)
                .containsExactly(tuple(version2ProcessDef.getId(), "userTask1Id"));

        //This new process definition has two activities
        taskService.complete(tasks.get(0).getId());
        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getProcessDefinitionId, Task::getTaskDefinitionKey)
                .containsExactly(tuple(version2ProcessDef.getId(), "userTask2Id"));

        taskService.complete(tasks.get(0).getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testSimpleMigrationWithExplicitActivityMapping2() {
        //Deploy first version of the process
        Deployment oneActivityProcessDeployment = repositoryService.createDeployment()
                .name("My Process Deployment")
                .addClasspathResource("org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml")
                .deploy();

        //Start and instance of the recent first version of the process for migration and one for reference
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("MP");

        //Deploy second version of the process
        Deployment twoActivitiesProcessDeployment = repositoryService.createDeployment()
                .name("My Process Deployment")
                .addClasspathResource("org/flowable/engine/test/api/runtime/migration/two-tasks-simple-process.bpmn20.xml")
                .deploy();

        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey("MP")
                .list();

        assertThat(processDefinitions).hasSize(2);

        ProcessDefinition version1ProcessDef = processDefinitions.stream().filter(d -> d.getVersion() == 1).findFirst().get();
        assertThat(version1ProcessDef.getDeploymentId()).isEqualTo(oneActivityProcessDeployment.getId());
        ProcessDefinition version2ProcessDef = processDefinitions.stream().filter(d -> d.getVersion() == 2).findFirst().get();
        assertThat(version2ProcessDef.getDeploymentId()).isEqualTo(twoActivitiesProcessDeployment.getId());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(executions).hasSize(2); //includes root execution
        executions.stream()
                .map(e -> (ExecutionEntity) e)
                .forEach(e -> assertThat(e.getProcessDefinitionId()).isEqualTo(version1ProcessDef.getId()));

        List<Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getProcessDefinitionId, Task::getTaskDefinitionKey)
                .containsExactly(tuple(version1ProcessDef.getId(), "userTask1Id"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(version2ProcessDef.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "userTask3Id"))
                .validateMigration(processInstance.getId());

        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isFalse();
        assertThat(processInstanceMigrationValidationResult.getValidationMessages())
                .isEqualTo(Collections
                        .singletonList("Invalid mapping for 'userTask1Id' to 'userTask3Id', cannot be found in the process definition with id 'MP'"));

        processInstanceMigrationValidationResult = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(version2ProcessDef.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "userTask2Id"))
                .validateMigration(processInstance.getId());

        assertThat(processInstanceMigrationValidationResult.isMigrationValid()).isTrue();

        //Migrate process - moving the current execution explicitly
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(version2ProcessDef.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "userTask2Id"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        executions = runtimeService.createExecutionQuery().list();
        assertThat(executions).hasSize(2); //includes root execution
        executions.stream()
                .map(e -> (ExecutionEntity) e)
                .forEach(e -> assertThat(e.getProcessDefinitionId()).isEqualTo(version2ProcessDef.getId()));

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getProcessDefinitionId, Task::getTaskDefinitionKey)
                .containsExactly(tuple(version2ProcessDef.getId(), "userTask2Id"));

        //This new process definition has two activities, but we have mapped to the last activity explicitly
        taskService.complete(tasks.get(0).getId());
        assertProcessEnded(processInstance.getId());

    }

    @Test
    public void testSimpleMigrationWithExplicitActivityMapping3() {
        //Deploy first version of the process
        Deployment twoActivitiesProcessDeployment = repositoryService.createDeployment()
                .name("My Process Deployment")
                .addClasspathResource("org/flowable/engine/test/api/runtime/migration/two-tasks-simple-process.bpmn20.xml")
                .deploy();

        //Start and instance of the recent first version of the process for migration and one for reference
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("MP");

        //Deploy second version of the process
        Deployment oneActivityProcessDeployment = repositoryService.createDeployment()
                .name("My Process Deployment")
                .addClasspathResource("org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml")
                .deploy();

        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey("MP")
                .list();

        assertThat(processDefinitions).hasSize(2);

        ProcessDefinition version1ProcessDef = processDefinitions.stream().filter(d -> d.getVersion() == 1).findFirst().get();
        assertThat(version1ProcessDef.getDeploymentId()).isEqualTo(twoActivitiesProcessDeployment.getId());
        ProcessDefinition version2ProcessDef = processDefinitions.stream().filter(d -> d.getVersion() == 2).findFirst().get();
        assertThat(version2ProcessDef.getDeploymentId()).isEqualTo(oneActivityProcessDeployment.getId());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(executions).hasSize(2); //includes root execution
        executions.stream()
                .map(e -> (ExecutionEntity) e)
                .forEach(e -> assertThat(e.getProcessDefinitionId()).isEqualTo(version1ProcessDef.getId()));

        List<Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getProcessDefinitionId, Task::getTaskDefinitionKey)
                .containsExactly(tuple(version1ProcessDef.getId(), "userTask1Id"));

        //We want to migrate from the next activity
        taskService.complete(tasks.get(0).getId());
        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getProcessDefinitionId, Task::getTaskDefinitionKey)
                .containsExactly(tuple(version1ProcessDef.getId(), "userTask2Id"));

        //Migrate process - moving the current execution explicitly
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(version2ProcessDef.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask2Id", "userTask1Id"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        executions = runtimeService.createExecutionQuery().list();
        assertThat(executions).hasSize(2); //includes root execution
        executions.stream()
                .map(e -> (ExecutionEntity) e)
                .forEach(e -> assertThat(e.getProcessDefinitionId()).isEqualTo(version2ProcessDef.getId()));

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getProcessDefinitionId, Task::getTaskDefinitionKey)
                .containsExactly(tuple(version2ProcessDef.getId(), "userTask1Id"));

        //This new process version only have one activity
        taskService.complete(tasks.get(0).getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testSimpleUserTaskDirectMigration() {

        //Almost all tests use UserTask, thus are direct migrations, but this one checks explicitly for changes in History
        //Deploy first version of the process
        ProcessDefinition version1ProcessDef = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");

        //Start and instance of the recent first version of the process for migration and one for reference
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("MP");

        //Deploy second version of the process
        ProcessDefinition version2ProcessDef = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/two-tasks-simple-process.bpmn20.xml");

        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey("MP")
                .list();

        assertThat(processDefinitions)
                .extracting(ProcessDefinition::getId)
                .containsExactlyInAnyOrder(version1ProcessDef.getId(), version2ProcessDef.getId());

        List<Execution> executionsBefore = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(executionsBefore).hasSize(2); //includes root execution
        executionsBefore.stream()
                .map(e -> (ExecutionEntity) e)
                .forEach(e -> assertThat(e.getProcessDefinitionId()).isEqualTo(version1ProcessDef.getId()));

        List<Task> tasksBefore = taskService.createTaskQuery().list();
        assertThat(tasksBefore)
                .extracting(Task::getProcessDefinitionId, Task::getTaskDefinitionKey)
                .containsExactly(tuple(version1ProcessDef.getId(), "userTask1Id"));

        List<HistoricActivityInstance> historicActivityInstancesBefore = null;
        List<HistoricTaskInstance> historicTaskInstancesBefore = null;
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            historicActivityInstancesBefore = historyService.createHistoricActivityInstanceQuery().orderByExecutionId().asc().list();

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                historicTaskInstancesBefore = historyService.createHistoricTaskInstanceQuery().orderByExecutionId().asc().list();
            }
        }

        //Migrate process
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(version2ProcessDef.getId());

        ProcessInstanceMigrationValidationResult processInstanceMigrationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        List<Execution> executionsAfter = runtimeService.createExecutionQuery().list();
        assertThat(executionsAfter).hasSize(2); //includes root execution
        executionsAfter.stream()
                .map(e -> (ExecutionEntity) e)
                .forEach(e -> assertThat(e.getProcessDefinitionId()).isEqualTo(version2ProcessDef.getId()));

        List<Task> tasksAfter = taskService.createTaskQuery().list();
        assertThat(tasksAfter)
                .extracting(Task::getProcessDefinitionId, Task::getTaskDefinitionKey)
                .containsExactly(tuple(version2ProcessDef.getId(), "userTask1Id")); //AutoMapped by Id

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            List<HistoricActivityInstance> historicActivityInstancesAfter = historyService.createHistoricActivityInstanceQuery().orderByExecutionId().asc()
                    .list();
            assertThat(historicActivityInstancesAfter).hasSameSizeAs(historicActivityInstancesBefore);
            assertThat(historicActivityInstancesBefore)
                    .usingElementComparatorIgnoringFields("revision", "processDefinitionId")
                    .containsExactlyInAnyOrderElementsOf(historicActivityInstancesAfter);

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTaskInstancesAfter = historyService.createHistoricTaskInstanceQuery().orderByExecutionId().asc().list();

                assertThat(historicTaskInstancesAfter).hasSameSizeAs(historicTaskInstancesBefore);
                assertThat(historicTaskInstancesBefore)
                        .usingElementComparatorIgnoringFields("revision", "processDefinitionId", "originalPersistentState", "lastUpdateTime")
                        .containsExactlyInAnyOrderElementsOf(historicTaskInstancesAfter);
            }
        }

        //The first process version only had one activity, there should be a second activity in the process now
        taskService.complete(tasksAfter.get(0).getId());
        tasksAfter = taskService.createTaskQuery().list();
        assertThat(tasksAfter)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("userTask2Id");

        taskService.complete(tasksAfter.get(0).getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testSimpleUserTaskDirectMigrationReAssign() {

        //Almost all tests use UserTask, thus are direct migrations, but this one checks explicitly for changes in History
        //Deploy first version of the process
        ProcessDefinition version1ProcessDef = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");

        //Start and instance of the recent first version of the process for migration and one for reference
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("MP");

        //Deploy second version of the process
        ProcessDefinition version2ProcessDef = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/two-tasks-simple-process.bpmn20.xml");

        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey("MP")
                .list();

        assertThat(processDefinitions)
                .extracting(ProcessDefinition::getId)
                .containsExactlyInAnyOrder(version1ProcessDef.getId(), version2ProcessDef.getId());

        List<Execution> executionsBefore = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(executionsBefore).hasSize(2); //includes root execution
        executionsBefore.stream()
                .map(e -> (ExecutionEntity) e)
                .forEach(e -> assertThat(e.getProcessDefinitionId()).isEqualTo(version1ProcessDef.getId()));

        List<Task> tasksBefore = taskService.createTaskQuery().list();
        assertThat(tasksBefore)
                .extracting(Task::getProcessDefinitionId, Task::getTaskDefinitionKey)
                .containsExactly(tuple(version1ProcessDef.getId(), "userTask1Id"));
        assertThat(tasksBefore)
                .extracting(Task::getAssignee)
                .containsNull();

        List<HistoricActivityInstance> historicActivityInstancesBefore = null;
        List<HistoricTaskInstance> historicTaskInstancesBefore = null;
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            historicActivityInstancesBefore = historyService.createHistoricActivityInstanceQuery().orderByExecutionId().asc().list();

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                historicTaskInstancesBefore = historyService.createHistoricTaskInstanceQuery().orderByExecutionId().asc().list();
            }
        }

        //Migrate process
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(version2ProcessDef.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "userTask1Id").withNewAssignee("kermit"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        List<Execution> executionsAfter = runtimeService.createExecutionQuery().list();
        assertThat(executionsAfter).hasSize(2); //includes root execution
        executionsAfter.stream()
                .map(e -> (ExecutionEntity) e)
                .forEach(e -> assertThat(e.getProcessDefinitionId()).isEqualTo(version2ProcessDef.getId()));

        List<Task> tasksAfter = taskService.createTaskQuery().list();
        assertThat(tasksAfter)
                .extracting(Task::getProcessDefinitionId, Task::getTaskDefinitionKey, Task::getAssignee)
                .containsExactly(tuple(version2ProcessDef.getId(), "userTask1Id", "kermit")); //AutoMapped by Id

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            List<HistoricActivityInstance> historicActivityInstancesAfter = historyService.createHistoricActivityInstanceQuery().orderByExecutionId().asc()
                    .list();
            assertThat(historicActivityInstancesAfter).hasSameSizeAs(historicActivityInstancesBefore);
            assertThat(historicActivityInstancesBefore)
                    .usingElementComparatorIgnoringFields("revision", "processDefinitionId", "assignee", "originalPersistentState")
                    .containsExactlyInAnyOrderElementsOf(historicActivityInstancesAfter);

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTaskInstancesAfter = historyService.createHistoricTaskInstanceQuery().orderByExecutionId().asc().list();

                assertThat(historicTaskInstancesAfter).hasSameSizeAs(historicTaskInstancesBefore);
                assertThat(historicTaskInstancesBefore)
                        .usingElementComparatorIgnoringFields("revision", "processDefinitionId", "assignee", "originalPersistentState", "lastUpdateTime")
                        .containsExactlyInAnyOrderElementsOf(historicTaskInstancesAfter);
            }
        }

        //The first process version only had one activity, there should be a second activity in the process now
        taskService.complete(tasksAfter.get(0).getId());
        tasksAfter = taskService.createTaskQuery().list();
        assertThat(tasksAfter)
                .extracting(Task::getProcessDefinitionId, Task::getTaskDefinitionKey)
                .containsExactly(tuple(version2ProcessDef.getId(), "userTask2Id"));

        taskService.complete(tasksAfter.get(0).getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testSimpleMigrationWithinSimpleSubProcess() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-tasks-inside-subprocess.bpmn20.xml");
        ProcessDefinition procDefTwoTasks = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/two-tasks-inside-subprocess.bpmn20.xml");

        //Start an instance of a process with one task inside a subProcess
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm and move inside the subProcess
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(executions).hasSize(2); //includes root execution
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procDefOneTask.getId());

        //Should be only one task
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("BeforeSubProcess");
        completeTask(task);

        List<Execution> executionsBeforeMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions()
                .list();
        assertThat(executionsBeforeMigration)  //Includes subProcess
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("SimpleSubProcess", "InsideSimpleSubProcess1");
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("InsideSimpleSubProcess1");

        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefTwoTasks.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("InsideSimpleSubProcess1", "InsideSimpleSubProcess2"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm and move inside the subProcess
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions()
                .list();
        assertThat(executionsAfterMigration)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("SimpleSubProcess", "InsideSimpleSubProcess2"); //includes subProcess
        assertThat(executionsAfterMigration)
                .extracting("processDefinitionId")
                .containsOnly(procDefTwoTasks.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procDefTwoTasks, processInstance, "subProcess", "SimpleSubProcess", "SimpleSubProcess");
            checkActivityInstances(procDefTwoTasks, processInstance, "userTask", "BeforeSubProcess", "InsideSimpleSubProcess2");

            checkTaskInstance(procDefTwoTasks, processInstance, "BeforeSubProcess", "InsideSimpleSubProcess2");
        }

        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procDefTwoTasks, processInstance, "subProcess", "SimpleSubProcess", "SimpleSubProcess");
            checkActivityInstances(procDefTwoTasks, processInstance, "userTask", "BeforeSubProcess", "AfterSubProcess", "InsideSimpleSubProcess2");

            checkTaskInstance(procDefTwoTasks, processInstance, "BeforeSubProcess", "AfterSubProcess", "InsideSimpleSubProcess2");
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testSimpleMigrationWithinSimpleSubProcess2() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-tasks-inside-subprocess.bpmn20.xml");
        ProcessDefinition procDefTwoTasks = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/two-tasks-inside-subprocess.bpmn20.xml");

        //Start and instance of the recent first version of the process for migration and one for reference
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefTwoTasks.getId());

        //Confirm and move inside the subProcess
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(executions).hasSize(2); //includes root execution
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procDefTwoTasks.getId());

        //Move to the second task inside the SubProcess
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("BeforeSubProcess");
        completeTask(task);
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("InsideSimpleSubProcess1");
        completeTask(task);
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("InsideSimpleSubProcess2");

        List<Execution> executionsBeforeMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions()
                .list();
        assertThat(executionsBeforeMigration)
                .extracting("activityId")
                .containsExactlyInAnyOrder("SimpleSubProcess", "InsideSimpleSubProcess2"); // Includes subProcess

        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefOneTask.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("InsideSimpleSubProcess2", "InsideSimpleSubProcess1"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm and move inside the subProcess
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions()
                .list();
        assertThat(executionsAfterMigration)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("SimpleSubProcess", "InsideSimpleSubProcess1");  // Includes subProcess
        assertThat(executionsAfterMigration)
                .extracting("processDefinitionId")
                .containsOnly(procDefOneTask.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procDefOneTask, processInstance, "subProcess", "SimpleSubProcess", "SimpleSubProcess");
            checkActivityInstances(procDefOneTask, processInstance, "userTask", "BeforeSubProcess",
                    "InsideSimpleSubProcess1",
                    "InsideSimpleSubProcess1");

            checkTaskInstance(procDefOneTask, processInstance, "BeforeSubProcess",
                    "InsideSimpleSubProcess1",
                    "InsideSimpleSubProcess1");
        }

        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procDefOneTask, processInstance, "subProcess", "SimpleSubProcess", "SimpleSubProcess");
            checkActivityInstances(procDefOneTask, processInstance, "userTask", "BeforeSubProcess",
                    "InsideSimpleSubProcess1",
                    "InsideSimpleSubProcess1",
                    "AfterSubProcess");

            checkTaskInstance(procDefOneTask, processInstance, "BeforeSubProcess",
                    "InsideSimpleSubProcess1",
                    "InsideSimpleSubProcess1",
                    "AfterSubProcess");
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testSimpleMigrationIntoEmbeddedSubProcess() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-tasks-inside-subprocess.bpmn20.xml");
        ProcessDefinition procDefTwoTasks = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/two-tasks-inside-subprocess.bpmn20.xml");

        //Start and instance of the recent first version of the process for migration and one for reference
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(executions).hasSize(2); //includes root execution
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procDefOneTask.getId());

        //Confirm migration point before the subProcess
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("BeforeSubProcess");

        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefTwoTasks.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("BeforeSubProcess", "InsideSimpleSubProcess2"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm and move inside the subProcess
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions()
                .list();
        assertThat(executionsAfterMigration).hasSize(2);
        assertThat(executionsAfterMigration)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("SimpleSubProcess", "InsideSimpleSubProcess2");   //includes subProcess
        assertThat(executionsAfterMigration)
                .extracting("processDefinitionId")
                .containsOnly(procDefTwoTasks.getId());

        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History - should contain one SubProcess and 2 userTask Activities since the migratedUser task moved forward inside the last task of the subProcess
            HistoricActivityInstance subProcess = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .activityType("subProcess")
                    .singleResult();
            assertThat(subProcess).extracting(HistoricActivityInstance::getActivityId).isEqualTo("SimpleSubProcess");
            assertThat(subProcess).extracting(HistoricActivityInstance::getProcessDefinitionId).isEqualTo(procDefTwoTasks.getId());

            List<HistoricActivityInstance> userTasks = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .activityType("userTask")
                    .list();
            assertThat(userTasks)
                    .extracting(HistoricActivityInstance::getActivityId)
                    .containsExactlyInAnyOrder("AfterSubProcess", "InsideSimpleSubProcess2");
            assertThat(userTasks)
                    .extracting(HistoricActivityInstance::getProcessDefinitionId)
                    .containsOnly(procDefTwoTasks.getId());
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testSimpleMigrationOutOfEmbeddedSubProcess() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-tasks-inside-subprocess.bpmn20.xml");
        ProcessDefinition procDefTwoTasks = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/two-tasks-inside-subprocess.bpmn20.xml");

        //Start an instance of the definition with two task inside the subProcess
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefTwoTasks.getId());

        //Confirm and move inside the subProcess
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(executions).hasSize(2); //includes root execution
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procDefTwoTasks.getId());

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("BeforeSubProcess");
        completeTask(task);
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("InsideSimpleSubProcess1");
        completeTask(task);
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("InsideSimpleSubProcess2");

        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefOneTask.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("InsideSimpleSubProcess2", "BeforeSubProcess"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm and move inside the subProcess
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions()
                .list();
        assertThat(executionsAfterMigration)
                .extracting(Execution::getActivityId)
                .containsExactly("BeforeSubProcess"); //No subProcess
        assertThat(executionsAfterMigration)
                .extracting("processDefinitionId")
                .containsOnly(procDefOneTask.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procDefOneTask, processInstance, "subProcess", "SimpleSubProcess");
            checkActivityInstances(procDefOneTask, processInstance, "userTask", "BeforeSubProcess",
                    "InsideSimpleSubProcess1",
                    "BeforeSubProcess");

            checkTaskInstance(procDefOneTask, processInstance, "BeforeSubProcess",
                    "InsideSimpleSubProcess1",
                    "BeforeSubProcess");
        }

        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procDefOneTask, processInstance, "subProcess", "SimpleSubProcess", "SimpleSubProcess");
            checkActivityInstances(procDefOneTask, processInstance, "userTask", "BeforeSubProcess",
                    "InsideSimpleSubProcess1",
                    "BeforeSubProcess",
                    "InsideSimpleSubProcess1",
                    "AfterSubProcess");

            checkTaskInstance(procDefOneTask, processInstance, "BeforeSubProcess",
                    "InsideSimpleSubProcess1",
                    "BeforeSubProcess",
                    "InsideSimpleSubProcess1",
                    "AfterSubProcess");
        }

        assertProcessEnded(processInstance.getId());
    }
    
    @Test
    public void testReceiveTaskToUserTaskMigration() {
        deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/receive-task-process.bpmn20.xml");

        // Start and instance of the recent first version of the process for migration and one for reference
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("MP");

        // Deploy second version of the process
        ProcessDefinition version2ProcessDef = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/two-tasks-simple-process.bpmn20.xml");

        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey("MP")
                .list();

        assertThat(processDefinitions).hasSize(2);

        // Migrate process
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("receiveTask", "userTask1Id").withNewAssignee("johndoe"))
                .migrateToProcessDefinition(version2ProcessDef.getId());

        ProcessInstanceMigrationValidationResult processInstanceMigrationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        List<Execution> executionsAfter = runtimeService.createExecutionQuery().list();
        assertThat(executionsAfter).hasSize(2); //includes root execution
        for (Execution execution : executionsAfter) {
            assertThat(((ExecutionEntity) execution).getProcessDefinitionId()).isEqualTo(version2ProcessDef.getId());
        }

        List<Task> tasksAfter = taskService.createTaskQuery().list();
        assertThat(tasksAfter).hasSize(1);
        Task taskAfter = tasksAfter.get(0);
        assertThat(taskAfter.getProcessDefinitionId()).isEqualTo(version2ProcessDef.getId());
        assertThat(taskAfter.getTaskDefinitionKey()).isEqualTo("userTask1Id");
        assertThat(taskAfter.getAssignee()).isEqualTo("johndoe");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            List<HistoricTaskInstance> historicTaskInstancesAfter = historyService.createHistoricTaskInstanceQuery().list();
            assertThat(historicTaskInstancesAfter).hasSize(1);
            HistoricTaskInstance historicTaskAfter = historicTaskInstancesAfter.get(0);
            assertThat(historicTaskAfter.getProcessDefinitionId()).isEqualTo(version2ProcessDef.getId());
            assertThat(historicTaskAfter.getTaskDefinitionKey()).isEqualTo("userTask1Id");
            assertThat(historicTaskAfter.getAssignee()).isEqualTo("johndoe");
        }

        //The first process version only had one activity, there should be a second activity in the process now
        taskService.complete(tasksAfter.get(0).getId());
        tasksAfter = taskService.createTaskQuery().list();
        assertThat(tasksAfter)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("userTask2Id");

        taskService.complete(tasksAfter.get(0).getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityFromProcessRootIntoNestedEmbeddedSubProcess() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-tasks-inside-subprocess.bpmn20.xml");
        ProcessDefinition procDefNested = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-tasks-nested-subprocess.bpmn20.xml");

        //Start an instance of the definition with two task inside the subProcess
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(executions).hasSize(2); //includes root execution
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procDefOneTask.getId());

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("BeforeSubProcess");

        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefNested.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("BeforeSubProcess", "InsideNestedSubProcess"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions()
                .list();
        assertThat(executionsAfterMigration)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("OuterSubProcess", "SimpleSubProcess", "InsideNestedSubProcess");
        assertThat(executionsAfterMigration)
                .extracting("processDefinitionId")
                .containsOnly(procDefNested.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procDefNested, processInstance, "subProcess", "OuterSubProcess", "SimpleSubProcess");
            checkActivityInstances(procDefNested, processInstance, "userTask", "InsideNestedSubProcess");

            checkTaskInstance(procDefNested, processInstance, "InsideNestedSubProcess");
        }

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procDefNested, processInstance, "subProcess", "OuterSubProcess", "SimpleSubProcess");
            checkActivityInstances(procDefNested, processInstance, "userTask", "InsideNestedSubProcess", "AfterSubProcess");

            checkTaskInstance(procDefNested, processInstance, "InsideNestedSubProcess", "AfterSubProcess");
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityFromProcessRootIntoNestedEmbeddedSubProcessWithDataObject() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-tasks-inside-subprocess-with-data-object.bpmn20.xml");
        ProcessDefinition procDefNested = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-tasks-nested-subprocess-with-data-object.bpmn20.xml");

        //Start an instance of the definition with two task inside the subProcess
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(executions).hasSize(2); //includes root execution
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procDefOneTask.getId());

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("BeforeSubProcess");

        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefNested.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("BeforeSubProcess", "InsideNestedSubProcess"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions()
                .list();
        assertThat(executionsAfterMigration)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("OuterSubProcess", "SimpleSubProcess", "InsideNestedSubProcess");
        assertThat(executionsAfterMigration)
                .extracting("processDefinitionId")
                .containsOnly(procDefNested.getId());

        //Should contain the dataObject of the new embedded process definition
        Execution nestedSubProcess = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).activityId("SimpleSubProcess")
                .singleResult();
        assertThat(runtimeService.getVariableLocal(nestedSubProcess.getId(), "dataScopeNested", String.class)).isNotNull();
        DataObject nameDataObject = runtimeService.getDataObjectLocal(nestedSubProcess.getId(), "dataScopeNested");
        assertThat(nameDataObject).isNotNull();
        assertThat(nameDataObject.getValue()).isEqualTo("nestedSubProcess");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procDefNested, processInstance, "subProcess", "OuterSubProcess", "SimpleSubProcess");
            checkActivityInstances(procDefNested, processInstance, "userTask", "InsideNestedSubProcess");

            checkTaskInstance(procDefNested, processInstance, "InsideNestedSubProcess");
        }

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procDefNested, processInstance, "subProcess", "OuterSubProcess", "SimpleSubProcess");
            checkActivityInstances(procDefNested, processInstance, "userTask", "InsideNestedSubProcess", "AfterSubProcess");

            checkTaskInstance(procDefNested, processInstance, "InsideNestedSubProcess", "AfterSubProcess");
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityFromEmbeddedSubProcessIntoNestedEmbeddedSubProcess() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-tasks-inside-subprocess.bpmn20.xml");
        ProcessDefinition procDefNested = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-tasks-nested-subprocess.bpmn20.xml");

        //Start an instance of the definition with two task inside the subProcess
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm and move inside the subProcess
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(executions).hasSize(2); //includes root execution
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procDefOneTask.getId());

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("BeforeSubProcess");
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("InsideSimpleSubProcess1");

        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefNested.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("InsideSimpleSubProcess1", "InsideNestedSubProcess"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm - we move from a subProcess to a nestedSubProcess with the same name (SimpleSubProcess), the original is not created, but cancelled and created from the new model
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions()
                .list();
        assertThat(executionsAfterMigration)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("OuterSubProcess", "SimpleSubProcess", "InsideNestedSubProcess"); //2 subProcesses and 1 userTask
        assertThat(executionsAfterMigration)
                .extracting("processDefinitionId")
                .containsOnly(procDefNested.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procDefNested, processInstance, "subProcess", "SimpleSubProcess", "OuterSubProcess", "SimpleSubProcess");
            checkActivityInstances(procDefNested, processInstance, "userTask", "BeforeSubProcess", "InsideNestedSubProcess");

            checkTaskInstance(procDefNested, processInstance, "BeforeSubProcess", "InsideNestedSubProcess");
        }

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procDefNested, processInstance, "subProcess", "SimpleSubProcess", "OuterSubProcess", "SimpleSubProcess");
            checkActivityInstances(procDefNested, processInstance, "userTask", "BeforeSubProcess", "InsideNestedSubProcess", "AfterSubProcess");

            checkTaskInstance(procDefNested, processInstance, "BeforeSubProcess", "InsideNestedSubProcess", "AfterSubProcess");
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityFromEmbeddedSubProcessIntoNestedEmbeddedSubProcessWithDataObject() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-tasks-inside-subprocess-with-data-object.bpmn20.xml");
        ProcessDefinition procDefNested = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-tasks-nested-subprocess-with-data-object.bpmn20.xml");

        //Start an instance of the definition with two task inside the subProcess
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm and move inside the subProcess
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(executions).hasSize(2); //includes root execution
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procDefOneTask.getId());

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("BeforeSubProcess");
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("InsideSimpleSubProcess1");

        Execution subProcessExecution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).activityId("SimpleSubProcess")
                .singleResult();
        assertThat(runtimeService.getVariableLocal(subProcessExecution.getId(), "dataScope", String.class)).isNotNull();
        DataObject nameDataObject = runtimeService.getDataObjectLocal(subProcessExecution.getId(), "dataScope");
        assertThat(nameDataObject).isNotNull();
        assertThat(nameDataObject.getValue()).isEqualTo("subProcess");

        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefNested.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("InsideSimpleSubProcess1", "InsideNestedSubProcess"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm - we move from a subProcess to a nestedSubProcess with the same name (SimpleSubProcess), the original is not created, but cancelled and created from the new model
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions()
                .list();
        assertThat(executionsAfterMigration)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("OuterSubProcess", "SimpleSubProcess", "InsideNestedSubProcess"); //2 subProcesses and 1 userTask
        assertThat(executionsAfterMigration)
                .extracting("processDefinitionId")
                .containsOnly(procDefNested.getId());

        //Confirm we have the dataObject of the subProcess in the new definition (its a new SubProcess execution nonetheless)
        subProcessExecution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).activityId("SimpleSubProcess").singleResult();
        assertThat(runtimeService.getVariablesLocal(subProcessExecution.getId())).containsOnlyKeys("dataScopeNested");
        assertThat(runtimeService.getDataObjectsLocal(subProcessExecution.getId())).containsOnlyKeys("dataScopeNested");
        nameDataObject = runtimeService.getDataObjectLocal(subProcessExecution.getId(), "dataScopeNested");
        assertThat(nameDataObject).isNotNull();
        assertThat(nameDataObject.getValue()).isEqualTo("nestedSubProcess");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procDefNested, processInstance, "subProcess", "SimpleSubProcess", "OuterSubProcess", "SimpleSubProcess");
            checkActivityInstances(procDefNested, processInstance, "userTask", "BeforeSubProcess", "InsideNestedSubProcess");

            checkTaskInstance(procDefNested, processInstance, "BeforeSubProcess", "InsideNestedSubProcess");
        }

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procDefNested, processInstance, "subProcess", "SimpleSubProcess", "OuterSubProcess", "SimpleSubProcess");
            checkActivityInstances(procDefNested, processInstance, "userTask", "BeforeSubProcess", "InsideNestedSubProcess", "AfterSubProcess");

            checkTaskInstance(procDefNested, processInstance, "BeforeSubProcess", "InsideNestedSubProcess", "AfterSubProcess");
        }

        assertProcessEnded(processInstance.getId());
    }

    //-- Timers
    @Test
    public void testMigrateActivityToActivityWithTimerInNewDefinition() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procDefOWithTimer = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/twoTasksProcessWithTimer.bpmn20.xml");

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
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(timerJob).isNull();

        changeStateEventListener.clear();

        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefOWithTimer.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "firstTask"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions()
                .list();
        assertThat(executionsAfterMigration)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("firstTask", "boundaryTimerEvent");
        assertThat(executionsAfterMigration)
                .extracting("processDefinitionId")
                .containsOnly(procDefOWithTimer.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("firstTask");
        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(timerJob).isNotNull();
        Execution execution = runtimeService.createExecutionQuery().parentId(task.getExecutionId()).singleResult();
        Job job = managementService.createTimerJobQuery().executionId(execution.getId()).singleResult();
        assertThat(job)
                .isEqualToIgnoringGivenFields(timerJob, "originalPersistentState", "customValuesByteArrayRef", "exceptionByteArrayRef");

        // Verify events
        assertThat(changeStateEventListener.hasEvents()).isTrue();
        assertThat(changeStateEventListener.getEvents())
                .extracting(FlowableEvent::getType)
                .containsExactly(FlowableEngineEventType.TIMER_SCHEDULED);
        Optional<FlowableEvent> timerEvent = changeStateEventListener.getEvents().stream()
                .filter(event -> event.getType().equals(FlowableEngineEventType.TIMER_SCHEDULED)).findFirst();
        assertThat(timerEvent).isPresent();
        FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent) timerEvent.get();
        Job timer = (Job) entityEvent.getEntity();
        assertThat(getJobActivityId(timer)).isEqualTo("boundaryTimerEvent");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procDefOWithTimer, processInstance, "userTask", "firstTask");

            checkTaskInstance(procDefOWithTimer, processInstance, "firstTask");
        }

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
                    .containsExactlyInAnyOrder("firstTask", "secondTask");
            assertThat(taskExecutions)
                    .extracting(HistoricActivityInstance::getProcessDefinitionId)
                    .containsOnly(procDefOWithTimer.getId());

            checkTaskInstance(procDefOWithTimer, processInstance, "firstTask", "secondTask");
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityWithTimerToActivityWithoutTimerInNewDefinition() {
        ProcessDefinition procDefOWithTimer = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/twoTasksProcessWithTimer.bpmn20.xml");
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");

        //Start the processInstance with timer
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOWithTimer.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("firstTask", "boundaryTimerEvent");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procDefOWithTimer.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("firstTask");

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(timerJob).isNotNull();
        Execution execution = runtimeService.createExecutionQuery().parentId(task.getExecutionId()).singleResult();
        Job job = managementService.createTimerJobQuery().executionId(execution.getId()).singleResult();
        assertThat(job)
                .isEqualToIgnoringGivenFields(timerJob, "originalPersistentState", "customValuesByteArrayRef", "exceptionByteArrayRef");

        changeStateEventListener.clear();

        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefOneTask.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("firstTask", "userTask1Id"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions()
                .list();
        assertThat(executionsAfterMigration)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        assertThat(executionsAfterMigration)
                .extracting("processDefinitionId")
                .containsOnly(procDefOneTask.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procDefOneTask, processInstance, "userTask", "userTask1Id");

            checkTaskInstance(procDefOneTask, processInstance, "userTask1Id");
        }

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procDefOneTask, processInstance, "userTask", "userTask1Id");

            checkTaskInstance(procDefOneTask, processInstance, "userTask1Id");
        }

        // Verify JOB cancellation event
        assertThat(changeStateEventListener.hasEvents()).isTrue();
        assertThat(changeStateEventListener.getEvents())
                .extracting(FlowableEvent::getType)
                .containsExactly(FlowableEngineEventType.JOB_CANCELED);
        Optional<FlowableEvent> jobCancelEvent = changeStateEventListener.getEvents().stream()
                .filter(event -> event.getType().equals(FlowableEngineEventType.JOB_CANCELED)).findFirst();
        assertThat(jobCancelEvent).isPresent();
        FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent) jobCancelEvent.get();
        Job timer = (Job) entityEvent.getEntity();
        assertThat(getJobActivityId(timer)).isEqualTo("boundaryTimerEvent");

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityWithTimerToActivityWithTimerInNewDefinition() {
        ProcessDefinition procDefOneTimer = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/twoTasksProcessWithTimer.bpmn20.xml");
        ProcessDefinition procDefTwoTimers = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/twoTasksProcessWithTimers.bpmn20.xml");

        //Start the processInstance without timer
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTimer.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("firstTask", "boundaryTimerEvent");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procDefOneTimer.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("firstTask");
        Job timerJob1 = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(timerJob1).isNotNull();

        changeStateEventListener.clear();

        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefTwoTimers.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("firstTask", "secondTask"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions()
                .list();
        assertThat(executionsAfterMigration)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("secondTask", "secondTimerEvent");
        assertThat(executionsAfterMigration)
                .extracting("processDefinitionId")
                .containsOnly(procDefTwoTimers.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("secondTask");
        Job timerJob2 = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(timerJob2).isNotNull();
        Execution execution = runtimeService.createExecutionQuery().parentId(task.getExecutionId()).singleResult();
        Job job = managementService.createTimerJobQuery().executionId(execution.getId()).singleResult();
        assertThat(job)
                .isEqualToIgnoringGivenFields(timerJob2, "originalPersistentState", "customValuesByteArrayRef", "exceptionByteArrayRef");
        assertThat(timerJob1)
                .extracting(Job::getExecutionId)
                .isNotEqualTo(timerJob2);

        // Verify events
        assertThat(changeStateEventListener.hasEvents()).isTrue();
        assertThat(changeStateEventListener.getEvents())
                .extracting(FlowableEvent::getType)
                .containsExactly(FlowableEngineEventType.JOB_CANCELED, FlowableEngineEventType.TIMER_SCHEDULED);

        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent) iterator.next();
        Job timer = (Job) entityEvent.getEntity();
        assertThat(getJobActivityId(timer)).isEqualTo("boundaryTimerEvent");
        entityEvent = (FlowableEngineEntityEvent) iterator.next();
        timer = (Job) entityEvent.getEntity();
        assertThat(getJobActivityId(timer)).isEqualTo("secondTimerEvent");

        //Complete the process
        job = managementService.moveTimerToExecutableJob(timerJob2.getId());
        managementService.executeJob(job.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procDefTwoTimers, processInstance, "userTask", "secondTask", "thirdTask");

            checkTaskInstance(procDefTwoTimers, processInstance, "secondTask", "thirdTask");
        }

        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procDefTwoTimers, processInstance, "userTask", "secondTask", "thirdTask");

            checkTaskInstance(procDefTwoTimers, processInstance, "secondTask", "thirdTask");
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityInsideEmbeddedSubProcessWithTimerToActivityOutsideEmbeddedSubProcessWithoutTimerInNewDefinition() {
        ProcessDefinition procDefSubProcWithTimer = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/oneTaskSubProcessWithTimer.bpmn20.xml");
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");

        //Start the processInstance without timer
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefSubProcWithTimer.getId());

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("taskBefore");
        taskService.complete(task.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("subProcess", "subTask", "boundaryTimerEvent");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procDefSubProcWithTimer.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("subTask");
        Job timerJob1 = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(timerJob1).isNotNull();
        assertThat(timerJob1).extracting(Job::getProcessDefinitionId).isEqualTo(procDefSubProcWithTimer.getId());
        assertThat(timerJob1.getJobHandlerConfiguration()).contains("boundaryTimerEvent");

        changeStateEventListener.clear();
        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefOneTask.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("subTask", "userTask1Id"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions()
                .list();
        assertThat(executionsAfterMigration)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        assertThat(executionsAfterMigration)
                .extracting("processDefinitionId")
                .containsOnly(procDefOneTask.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");

        Job timerJob2 = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(timerJob2).isNull();

        // Verify events
        assertThat(changeStateEventListener.hasEvents()).isTrue();
        assertThat(changeStateEventListener.getEvents())
                .extracting(FlowableEvent::getType)
                .containsExactly(FlowableEngineEventType.JOB_CANCELED, FlowableEngineEventType.ACTIVITY_CANCELLED);
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent) iterator.next();
        Job timer = (Job) entityEvent.getEntity();
        assertThat(getJobActivityId(timer)).isEqualTo("boundaryTimerEvent");
        FlowableActivityEvent activityEvent = (FlowableActivityEvent) iterator.next();
        assertThat(activityEvent).extracting(FlowableActivityEvent::getActivityId).isEqualTo("subProcess");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procDefOneTask, processInstance, "userTask", "taskBefore", "userTask1Id");

            checkTaskInstance(procDefOneTask, processInstance, "taskBefore", "userTask1Id");
        }

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procDefOneTask, processInstance, "userTask", "taskBefore", "userTask1Id");

            checkTaskInstance(procDefOneTask, processInstance, "taskBefore", "userTask1Id");
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityToActivityInsideEmbeddedSubProcessWithTimerInNewDefinition() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procDefSubProcWithTimer = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/oneTaskSubProcessWithTimer.bpmn20.xml");

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
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());

        changeStateEventListener.clear();
        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefSubProcWithTimer.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "subTask"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions()
                .list();
        assertThat(executionsAfterMigration).
                extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("subProcess", "subTask", "boundaryTimerEvent");
        assertThat(executionsAfterMigration)
                .extracting("processDefinitionId")
                .containsOnly(procDefSubProcWithTimer.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("subTask");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefSubProcWithTimer.getId());

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(timerJob).isNotNull();
        assertThat(timerJob).extracting(Job::getProcessDefinitionId).isEqualTo(procDefSubProcWithTimer.getId());
        assertThat(timerJob.getJobHandlerConfiguration()).contains("boundaryTimerEvent");

        // Verify events
        assertThat(changeStateEventListener.hasEvents()).isTrue();
        assertThat(changeStateEventListener.getEvents())
                .extracting(FlowableEvent::getType)
                .containsExactly(FlowableEngineEventType.ACTIVITY_STARTED, FlowableEngineEventType.TIMER_SCHEDULED);

        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        FlowableActivityEvent activityEvent = (FlowableActivityEvent) iterator.next();
        assertThat(activityEvent).extracting(FlowableActivityEvent::getActivityId).isEqualTo("subProcess");

        FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent) iterator.next();
        Job timer = (Job) entityEvent.getEntity();
        assertThat(getJobActivityId(timer)).isEqualTo("boundaryTimerEvent");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procDefSubProcWithTimer, processInstance, "userTask", "subTask");

            checkTaskInstance(procDefSubProcWithTimer, processInstance, "subTask");
        }

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procDefSubProcWithTimer, processInstance, "userTask", "subTask", "taskAfter");

            checkTaskInstance(procDefSubProcWithTimer, processInstance, "subTask", "taskAfter");
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityToActivityInsideEmbeddedSubProcessWithTimerInNewDefinitionAndExecuteTimer() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procDefSubProcWithTimer = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/oneTaskSubProcessWithTimer.bpmn20.xml");

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
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());

        changeStateEventListener.clear();
        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefSubProcWithTimer.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "subTask"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions()
                .list();
        assertThat(executionsAfterMigration)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("subProcess", "subTask", "boundaryTimerEvent");
        assertThat(executionsAfterMigration)
                .extracting("processDefinitionId")
                .containsOnly(procDefSubProcWithTimer.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("subTask");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefSubProcWithTimer.getId());

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(timerJob).isNotNull();
        assertThat(timerJob).extracting(Job::getProcessDefinitionId).isEqualTo(procDefSubProcWithTimer.getId());
        assertThat(timerJob.getJobHandlerConfiguration()).contains("boundaryTimerEvent");

        // Verify events
        assertThat(changeStateEventListener.hasEvents()).isTrue();
        assertThat(changeStateEventListener.getEvents())
                .extracting(FlowableEvent::getType)
                .containsExactly(FlowableEngineEventType.ACTIVITY_STARTED, FlowableEngineEventType.TIMER_SCHEDULED);

        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        FlowableActivityEvent activityEvent = (FlowableActivityEvent) iterator.next();
        assertThat(activityEvent).extracting(FlowableActivityEvent::getActivityId).isEqualTo("subProcess");

        FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent) iterator.next();
        Job timer = (Job) entityEvent.getEntity();
        assertThat(getJobActivityId(timer)).isEqualTo("boundaryTimerEvent");

        //We will trigger the timer instead of the completing the task, it goes straight to the end

        //Complete the process
        Job executableJob = managementService.moveTimerToExecutableJob(timerJob.getId());
        managementService.executeJob(executableJob.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procDefSubProcWithTimer, processInstance, "userTask", "subTask");

            checkTaskInstance(procDefSubProcWithTimer, processInstance, "subTask");
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityWithTimerInsideEmbeddedSubProcessToActivityWithoutTimerInsideEmbeddedSubProcessInNewDefinition() {
        ProcessDefinition procVersion1 = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-with-timer-inside-embedded-subprocess.bpmn20.xml");
        ProcessDefinition procVersion2 = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/oneTaskWithTimerInSubProcess.bpmn20.xml");

        //Start the processInstance without timer
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procVersion1.getId());
        completeTask(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("subProcess", "subTask", "boundaryTimerEvent");
        assertThat(executions)
                .extracting("processDefinitionId").
                containsOnly(procVersion1.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("subTask");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procVersion1.getId());
        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(timerJob).isNotNull();
        assertThat(timerJob).extracting(Job::getProcessDefinitionId).isEqualTo(procVersion1.getId());
        assertThat(timerJob.getJobHandlerConfiguration()).contains("boundaryTimerEvent");

        changeStateEventListener.clear();
        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procVersion2.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("subTask", "subTask2"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions()
                .list();
        assertThat(executionsAfterMigration)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("subProcess", "subTask2");
        assertThat(executionsAfterMigration)
                .extracting("processDefinitionId")
                .containsOnly(procVersion2.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("subTask2");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procVersion2.getId());

        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(timerJob).isNull();

        // Verify events
        assertThat(changeStateEventListener.hasEvents()).isTrue();
        assertThat(changeStateEventListener.getEvents())
                .extracting(FlowableEvent::getType)
                .containsExactly(FlowableEngineEventType.JOB_CANCELED, FlowableEngineEventType.ACTIVITY_CANCELLED, FlowableEngineEventType.ACTIVITY_STARTED);

        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent) iterator.next();
        Job timer = (Job) entityEvent.getEntity();
        assertThat(getJobActivityId(timer)).isEqualTo("boundaryTimerEvent");

        FlowableActivityEvent activityEvent = (FlowableActivityEvent) iterator.next();
        assertThat(activityEvent).extracting(FlowableActivityEvent::getActivityId).isEqualTo("subProcess");

        activityEvent = (FlowableActivityEvent) iterator.next();
        assertThat(activityEvent).extracting(FlowableActivityEvent::getActivityId).isEqualTo("subProcess");

        assertThat(iterator.hasNext()).isFalse();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procVersion2, processInstance, "userTask", "taskBefore", "subTask2");

            checkTaskInstance(procVersion2, processInstance, "taskBefore", "subTask2");
        }

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procVersion2, processInstance, "userTask", "taskBefore", "subTask2", "taskAfter");

            checkTaskInstance(procVersion2, processInstance, "taskBefore", "subTask2", "taskAfter");
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityToActivityWithTimerInsideEmbeddedSubProcessInNewDefinition() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procDefTimerTaskInSubProcess = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/oneTaskWithTimerInSubProcess.bpmn20.xml");

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
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());

        changeStateEventListener.clear();
        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefTimerTaskInSubProcess.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "subTask"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions()
                .list();
        assertThat(executionsAfterMigration)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("subProcess", "subTask", "boundaryTimerEvent");
        assertThat(executionsAfterMigration)
                .extracting("processDefinitionId")
                .containsOnly(procDefTimerTaskInSubProcess.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("subTask");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefTimerTaskInSubProcess.getId());

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(timerJob).isNotNull();
        assertThat(timerJob).extracting(Job::getProcessDefinitionId).isEqualTo(procDefTimerTaskInSubProcess.getId());
        assertThat(timerJob.getJobHandlerConfiguration()).contains("boundaryTimerEvent");
        //Job is attached to the activity
        Execution timerExecution = runtimeService.createExecutionQuery().parentId(task.getExecutionId()).singleResult();
        Job timerFromTask = managementService.createTimerJobQuery().executionId(timerExecution.getId()).singleResult();
        assertThat(timerJob)
                .isEqualToIgnoringGivenFields(timerFromTask, "originalPersistentState", "customValuesByteArrayRef", "exceptionByteArrayRef");

        // Verify events
        assertThat(changeStateEventListener.hasEvents()).isTrue();
        assertThat(changeStateEventListener.getEvents())
                .extracting(FlowableEvent::getType)
                .containsExactly(FlowableEngineEventType.ACTIVITY_STARTED, FlowableEngineEventType.TIMER_SCHEDULED);

        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        FlowableActivityEvent activityEvent = (FlowableActivityEvent) iterator.next();
        assertThat(activityEvent).extracting(FlowableActivityEvent::getActivityId).isEqualTo("subProcess");

        FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent) iterator.next();
        Job timer = (Job) entityEvent.getEntity();
        assertThat(getJobActivityId(timer)).isEqualTo("boundaryTimerEvent");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procDefTimerTaskInSubProcess, processInstance, "userTask", "subTask");

            checkTaskInstance(procDefTimerTaskInSubProcess, processInstance, "subTask");
        }

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procDefTimerTaskInSubProcess, processInstance, "userTask", "subTask", "subTask2", "taskAfter");

            checkTaskInstance(procDefTimerTaskInSubProcess, processInstance, "subTask", "subTask2", "taskAfter");
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityToActivityWithTimerInsideEmbeddedSubProcessInNewDefinitionAndExecuteTimer() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procDefTimerTaskInSubProcess = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/oneTaskWithTimerInSubProcess.bpmn20.xml");

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
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());

        changeStateEventListener.clear();
        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefTimerTaskInSubProcess.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "subTask"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions()
                .list();
        assertThat(executionsAfterMigration)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("subProcess", "subTask", "boundaryTimerEvent");
        assertThat(executionsAfterMigration)
                .extracting("processDefinitionId")
                .containsOnly(procDefTimerTaskInSubProcess.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("subTask");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefTimerTaskInSubProcess.getId());

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(timerJob).isNotNull();
        assertThat(timerJob).extracting(Job::getProcessDefinitionId).isEqualTo(procDefTimerTaskInSubProcess.getId());
        assertThat(timerJob.getJobHandlerConfiguration()).contains("boundaryTimerEvent");
        //Job is attached to the activity
        Execution timerExecution = runtimeService.createExecutionQuery().parentId(task.getExecutionId()).singleResult();
        Job timerFromTask = managementService.createTimerJobQuery().executionId(timerExecution.getId()).singleResult();
        assertThat(timerJob)
                .isEqualToIgnoringGivenFields(timerFromTask, "originalPersistentState", "customValuesByteArrayRef", "exceptionByteArrayRef");

        // Verify events
        assertThat(changeStateEventListener.hasEvents()).isTrue();
        assertThat(changeStateEventListener.getEvents())
                .extracting(FlowableEvent::getType)
                .containsExactly(FlowableEngineEventType.ACTIVITY_STARTED, FlowableEngineEventType.TIMER_SCHEDULED);

        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        FlowableActivityEvent activityEvent = (FlowableActivityEvent) iterator.next();
        assertThat(activityEvent).extracting(FlowableActivityEvent::getActivityId).isEqualTo("subProcess");

        FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent) iterator.next();
        Job timer = (Job) entityEvent.getEntity();
        assertThat(getJobActivityId(timer)).isEqualTo("boundaryTimerEvent");

        //We will trigger the timer instead of the completing the task, it goes straight to the end of the subProcess, skipping one task
        Job executableJob = managementService.moveTimerToExecutableJob(timerJob.getId());
        managementService.executeJob(executableJob.getId());
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procDefTimerTaskInSubProcess, processInstance, "userTask", "subTask", "taskAfter");

            checkTaskInstance(procDefTimerTaskInSubProcess, processInstance, "subTask", "taskAfter");
        }

        assertProcessEnded(processInstance.getId());
    }
    
    @Test
    public void testMigrateActivityToActivityWithEventRegistryBoundaryEventInNewDefinition() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procDefOWithEventRegistry = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/twoTasksProcessWithEventRegistry.bpmn20.xml");

        //Start the processInstance without event registry event
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

        assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(0);

        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefOWithEventRegistry.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "firstTask"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions()
                .list();
        assertThat(executionsAfterMigration)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("firstTask", "boundaryEvent");
        assertThat(executionsAfterMigration)
                .extracting("processDefinitionId")
                .containsOnly(procDefOWithEventRegistry.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("firstTask");
        
        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription).isNotNull();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procDefOWithEventRegistry, processInstance, "userTask", "firstTask");
            checkTaskInstance(procDefOWithEventRegistry, processInstance, "firstTask");
        }

        // Complete the process
        completeProcessInstanceTasks(processInstance.getId());
        
        assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(0);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            List<HistoricActivityInstance> taskExecutions = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .activityType("userTask")
                    .list();
            assertThat(taskExecutions)
                    .extracting(HistoricActivityInstance::getActivityId)
                    .containsExactlyInAnyOrder("firstTask", "secondTask");
            assertThat(taskExecutions)
                    .extracting(HistoricActivityInstance::getProcessDefinitionId)
                    .containsOnly(procDefOWithEventRegistry.getId());

            checkTaskInstance(procDefOWithEventRegistry, processInstance, "firstTask", "secondTask");
        }

        assertProcessEnded(processInstance.getId());
    }
    
    @Test
    public void testMigrateActivityWithEventRegistryBoundaryEventInBothDefinitions() {
        ProcessDefinition procDefOWithEventRegistry = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/twoTasksProcessWithEventRegistry.bpmn20.xml");
        ProcessDefinition procDefOWithEventRegistry2 = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/twoTasksProcessWithEventRegistry.bpmn20.xml");

        //Start the processInstance without event registry event
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOWithEventRegistry.getId());

        assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription.getEventType()).isEqualTo("myEvent");

        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefOWithEventRegistry2.getId());

        ProcessInstanceMigrationValidationResult processInstanceMigrationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions()
                .list();
        assertThat(executionsAfterMigration)
                .extracting(Execution::getActivityId)
                .containsExactlyInAnyOrder("firstTask", "boundaryEvent");
        assertThat(executionsAfterMigration)
                .extracting("processDefinitionId")
                .containsOnly(procDefOWithEventRegistry2.getId());

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("firstTask");
        
        EventSubscription eventSubscription2 = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription2).isNotNull();
        assertThat(eventSubscription.getId()).isNotEqualTo(eventSubscription2.getId());

        // Complete the process
        completeProcessInstanceTasks(processInstance.getId());
        assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(0);

        assertProcessEnded(processInstance.getId());
    }
    
    @Test
    public void testMigrateActivityWithEventRegistryBoundaryEventInBothDefinitionsWithEventSubscriptionDeleted() {
        ProcessDefinition procDefOWithEventRegistry = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/twoTasksProcessWithEventRegistry.bpmn20.xml");
        ProcessDefinition procDefOWithEventRegistry2 = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/twoTasksProcessWithEventRegistry.bpmn20.xml");

        //Start the processInstance without event registry event
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOWithEventRegistry.getId());

        assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
        final EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription.getEventType()).isEqualTo("myEvent");
        managementService.executeCommand(new Command<Void>() {

            @Override
            public Void execute(CommandContext commandContext) {
                processEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService()
                    .deleteEventSubscription((EventSubscriptionEntity) eventSubscription);
                return null;
            }
        });
        
        assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(0);

        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefOWithEventRegistry2.getId());

        ProcessInstanceMigrationValidationResult processInstanceMigrationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());
        
        EventSubscription eventSubscription2 = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(eventSubscription2).isNotNull();

        // Complete the process
        completeProcessInstanceTasks(processInstance.getId());
        assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(0);

        assertProcessEnded(processInstance.getId());
    }

    //-- Intermediate Signal Catch Events
    @Test
    public void testMigrateSimpleActivityToIntermediateSignalCatchingEventInNewDefinition() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procWithSignal = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.simpleIntermediateSignalCatchEvent.bpmn20.xml");

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
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        changeStateEventListener.clear();
        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procWithSignal.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "intermediateCatchEvent"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions()
                .list();
        assertThat(executionsAfterMigration)
                .extracting(Execution::getActivityId)
                .containsExactly("intermediateCatchEvent");
        assertThat(executionsAfterMigration)
                .extracting("processDefinitionId")
                .containsOnly(procWithSignal.getId());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).isEmpty();

        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions)
                .extracting(EventSubscription::getActivityId, EventSubscription::getEventType)
                .containsExactly(tuple("intermediateCatchEvent", "signal"));

        // Verify events
        assertThat(changeStateEventListener.hasEvents()).isTrue();
        assertThat(changeStateEventListener.getEvents())
                .extracting(FlowableEvent::getType)
                .containsExactly(FlowableEngineEventType.ACTIVITY_CANCELLED, FlowableEngineEventType.ACTIVITY_SIGNAL_WAITING);

        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        FlowableActivityEvent activityEvent = (FlowableActivityEvent) iterator.next();
        assertThat(activityEvent).extracting(FlowableActivityEvent::getType).isEqualTo(FlowableEngineEventType.ACTIVITY_CANCELLED);
        assertThat(activityEvent).extracting(FlowableActivityEvent::getActivityId).isEqualTo("userTask1Id");

        FlowableSignalEvent signalEvent = (FlowableSignalEvent) iterator.next();
        assertThat(signalEvent).extracting(FlowableSignalEvent::getType).isEqualTo(FlowableEngineEventType.ACTIVITY_SIGNAL_WAITING);
        assertThat(signalEvent).extracting(FlowableSignalEvent::getActivityId).isEqualTo("intermediateCatchEvent");
        assertThat(signalEvent).extracting(FlowableSignalEvent::getSignalName).isEqualTo("someSignal");

        //Trigger the event
        runtimeService.signalEventReceived("someSignal");

        executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executionsAfterMigration)
                .extracting(Execution::getActivityId)
                .containsExactly("afterCatchEvent");
        assertThat(executionsAfterMigration)
                .extracting("processDefinitionId")
                .containsOnly(procWithSignal.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("afterCatchEvent");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procWithSignal.getId());
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procWithSignal, processInstance, "userTask", "userTask1Id", "afterCatchEvent");
            checkActivityInstances(procWithSignal, processInstance, "intermediateCatchEvent", "intermediateCatchEvent");

            checkTaskInstance(procWithSignal, processInstance, "userTask1Id", "afterCatchEvent");
        }

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procWithSignal, processInstance, "userTask", "userTask1Id", "afterCatchEvent");
            checkActivityInstances(procWithSignal, processInstance, "intermediateCatchEvent", "intermediateCatchEvent");

            checkTaskInstance(procWithSignal, processInstance, "userTask1Id", "afterCatchEvent");
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateIntermediateSignalCatchingEventToSimpleActivityInNewDefinition() {
        ProcessDefinition procWithSignal = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.simpleIntermediateSignalCatchEvent.bpmn20.xml");
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");

        //Start the processInstance without timer
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procWithSignal.getId());

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("beforeCatchEvent");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procWithSignal.getId());
        completeTask(task);

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("intermediateCatchEvent");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procWithSignal.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).isEmpty();
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions)
                .extracting(EventSubscription::getActivityId, EventSubscription::getEventType)
                .containsExactly(tuple("intermediateCatchEvent", "signal"));

        changeStateEventListener.clear();
        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefOneTask.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("intermediateCatchEvent", "userTask1Id"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions()
                .list();
        assertThat(executionsAfterMigration)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        assertThat(executionsAfterMigration)
                .extracting("processDefinitionId")
                .containsOnly(procDefOneTask.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());

        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        // Verify events
        assertThat(changeStateEventListener.hasEvents()).isTrue();
        assertThat(changeStateEventListener.getEvents())
                .extracting(FlowableEvent::getType)
                .containsExactly(FlowableEngineEventType.ACTIVITY_STARTED);

        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        FlowableActivityEvent activityEvent = (FlowableActivityEvent) iterator.next();
        assertThat(activityEvent).extracting(FlowableActivityEvent::getType).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent).extracting(FlowableActivityEvent::getActivityId).isEqualTo("userTask1Id");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procDefOneTask, processInstance, "userTask", "beforeCatchEvent", "userTask1Id");
            checkActivityInstances(procDefOneTask, processInstance, "intermediateCatchEvent", "intermediateCatchEvent");

            checkTaskInstance(procDefOneTask, processInstance, "beforeCatchEvent", "userTask1Id");
        }

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procDefOneTask, processInstance, "userTask", "beforeCatchEvent", "userTask1Id");
            checkActivityInstances(procDefOneTask, processInstance, "intermediateCatchEvent", "intermediateCatchEvent");

            checkTaskInstance(procDefOneTask, processInstance, "beforeCatchEvent", "userTask1Id");
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateIntermediateSignalCatchingEventToIntermediateSignalCatchingEventInNewDefinition() {
        ProcessDefinition procWithSignalVer1 = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.simpleIntermediateSignalCatchEvent.bpmn20.xml");
        ProcessDefinition procWithSignalVer2 = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/simple-intermediate-signal-catch-event.bpmn20.xml");

        //Start the processInstance without timer
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procWithSignalVer1.getId());

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("beforeCatchEvent");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procWithSignalVer1.getId());
        completeTask(task);

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("intermediateCatchEvent");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procWithSignalVer1.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).isEmpty();
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions)
                .extracting(EventSubscription::getActivityId, EventSubscription::getEventType)
                .containsExactly(tuple("intermediateCatchEvent", "signal"));
        changeStateEventListener.clear();
        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procWithSignalVer2.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("intermediateCatchEvent", "newIntermediateCatchEvent"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions()
                .list();
        assertThat(executionsAfterMigration)
                .extracting(Execution::getActivityId)
                .containsExactly("newIntermediateCatchEvent");
        assertThat(executionsAfterMigration)
                .extracting("processDefinitionId")
                .containsOnly(procWithSignalVer2.getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).isEmpty();

        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions)
                .extracting(EventSubscription::getActivityId, EventSubscription::getEventType)
                .containsExactly(tuple("newIntermediateCatchEvent", "signal"));

        // Verify events
        assertThat(changeStateEventListener.hasEvents()).isTrue();
        assertThat(changeStateEventListener.getEvents())
                .extracting(FlowableEvent::getType)
                .containsExactly(FlowableEngineEventType.ACTIVITY_SIGNAL_WAITING);

        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        FlowableSignalEvent signalEvent = (FlowableSignalEvent) iterator.next();
        assertThat(signalEvent).extracting(FlowableSignalEvent::getType).isEqualTo(FlowableEngineEventType.ACTIVITY_SIGNAL_WAITING);
        assertThat(signalEvent).extracting(FlowableSignalEvent::getActivityId).isEqualTo("newIntermediateCatchEvent");
        assertThat(signalEvent).extracting(FlowableSignalEvent::getSignalName).isEqualTo("someNewSignal");

        //Trigger the event
        runtimeService.signalEventReceived("someNewSignal");

        executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executionsAfterMigration)
                .extracting(Execution::getActivityId)
                .containsExactly("afterNewCatchEvent");
        assertThat(executionsAfterMigration)
                .extracting("processDefinitionId")
                .containsOnly(procWithSignalVer2.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("afterNewCatchEvent");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procWithSignalVer2.getId());
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procWithSignalVer2, processInstance, "userTask", "beforeCatchEvent", "afterNewCatchEvent");
            checkActivityInstances(procWithSignalVer2, processInstance, "intermediateCatchEvent", "intermediateCatchEvent", "newIntermediateCatchEvent");

            checkTaskInstance(procWithSignalVer2, processInstance, "beforeCatchEvent", "afterNewCatchEvent");
        }

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procWithSignalVer2, processInstance, "userTask", "beforeCatchEvent", "afterNewCatchEvent");
            checkActivityInstances(procWithSignalVer2, processInstance, "intermediateCatchEvent", "intermediateCatchEvent", "newIntermediateCatchEvent");

            checkTaskInstance(procWithSignalVer2, processInstance, "beforeCatchEvent", "afterNewCatchEvent");
        }

        assertProcessEnded(processInstance.getId());
    }

    //-- Intermediate Message Catch Events
    @Test
    public void testMigrateSimpleActivityToIntermediateMessageCatchingEventInNewDefinition() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procWithSignal = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.simpleIntermediateMessageCatchEvent.bpmn20.xml");

        //Start the processInstance without timer
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).
                extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        changeStateEventListener.clear();
        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procWithSignal.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "intermediateCatchEvent"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions()
                .list();
        assertThat(executionsAfterMigration)
                .extracting(Execution::getActivityId)
                .containsExactly("intermediateCatchEvent");
        assertThat(executionsAfterMigration)
                .extracting("processDefinitionId")
                .containsOnly(procWithSignal.getId());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).isEmpty();

        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions)
                .extracting(EventSubscription::getActivityId, EventSubscription::getEventType)
                .containsExactly(tuple("intermediateCatchEvent", "message"));

        // Verify events
        assertThat(changeStateEventListener.hasEvents()).isTrue();
        assertThat(changeStateEventListener.getEvents())
                .extracting(FlowableEvent::getType)
                .containsExactly(FlowableEngineEventType.ACTIVITY_CANCELLED, FlowableEngineEventType.ACTIVITY_MESSAGE_WAITING);

        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        FlowableActivityEvent activityEvent = (FlowableActivityEvent) iterator.next();
        assertThat(activityEvent).extracting(FlowableActivityEvent::getType).isEqualTo(FlowableEngineEventType.ACTIVITY_CANCELLED);
        assertThat(activityEvent).extracting(FlowableActivityEvent::getActivityId).isEqualTo("userTask1Id");

        FlowableMessageEvent signalEvent = (FlowableMessageEvent) iterator.next();
        assertThat(signalEvent).extracting(FlowableMessageEvent::getType).isEqualTo(FlowableEngineEventType.ACTIVITY_MESSAGE_WAITING);
        assertThat(signalEvent).extracting(FlowableMessageEvent::getActivityId).isEqualTo("intermediateCatchEvent");
        assertThat(signalEvent).extracting(FlowableMessageEvent::getMessageName).isEqualTo("someMessage");

        //Trigger the event
        Execution messageCatchExecution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).activityId("intermediateCatchEvent")
                .singleResult();
        runtimeService.messageEventReceived("someMessage", messageCatchExecution.getId());

        executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executionsAfterMigration)
                .extracting(Execution::getActivityId)
                .containsExactly("afterCatchEvent");
        assertThat(executionsAfterMigration)
                .extracting("processDefinitionId")
                .containsOnly(procWithSignal.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("afterCatchEvent");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procWithSignal.getId());
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procWithSignal, processInstance, "userTask", "userTask1Id", "afterCatchEvent");
            checkActivityInstances(procWithSignal, processInstance, "intermediateCatchEvent", "intermediateCatchEvent");

            checkTaskInstance(procWithSignal, processInstance, "userTask1Id", "afterCatchEvent");
        }

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procWithSignal, processInstance, "userTask", "userTask1Id", "afterCatchEvent");
            checkActivityInstances(procWithSignal, processInstance, "intermediateCatchEvent", "intermediateCatchEvent");

            checkTaskInstance(procWithSignal, processInstance, "userTask1Id", "afterCatchEvent");
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateIntermediateMessageCatchingEventToSimpleActivityInNewDefinition() {
        ProcessDefinition procWithSignal = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.simpleIntermediateMessageCatchEvent.bpmn20.xml");
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");

        //Start the processInstance without timer
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procWithSignal.getId());

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("beforeCatchEvent");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procWithSignal.getId());
        completeTask(task);

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("intermediateCatchEvent");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procWithSignal.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).isEmpty();
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions)
                .extracting(EventSubscription::getActivityId, EventSubscription::getEventType)
                .containsExactly(tuple("intermediateCatchEvent", "message"));

        changeStateEventListener.clear();
        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefOneTask.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("intermediateCatchEvent", "userTask1Id"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions()
                .list();
        assertThat(executionsAfterMigration)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        assertThat(executionsAfterMigration)
                .extracting("processDefinitionId")
                .containsOnly(procDefOneTask.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());

        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        // Verify events
        assertThat(changeStateEventListener.hasEvents()).isTrue();
        assertThat(changeStateEventListener.getEvents())
                .extracting(FlowableEvent::getType)
                .containsExactly(FlowableEngineEventType.ACTIVITY_MESSAGE_CANCELLED, FlowableEngineEventType.ACTIVITY_STARTED);

        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        FlowableActivityEvent signalEvent = (FlowableMessageEvent) iterator.next();
        assertThat(signalEvent).extracting(FlowableActivityEvent::getType).isEqualTo(FlowableEngineEventType.ACTIVITY_MESSAGE_CANCELLED);
        assertThat(signalEvent).extracting(FlowableActivityEvent::getActivityId).isEqualTo("intermediateCatchEvent");
        assertThat((FlowableMessageEvent) signalEvent).extracting(FlowableMessageEvent::getMessageName).isEqualTo("someMessage");

        FlowableActivityEvent activityEvent = (FlowableActivityEvent) iterator.next();
        assertThat(activityEvent).extracting(FlowableActivityEvent::getType).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED);
        assertThat(activityEvent).extracting(FlowableActivityEvent::getActivityId).isEqualTo("userTask1Id");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procDefOneTask, processInstance, "userTask", "beforeCatchEvent", "userTask1Id");
            checkActivityInstances(procDefOneTask, processInstance, "intermediateCatchEvent", "intermediateCatchEvent");

            checkTaskInstance(procDefOneTask, processInstance, "beforeCatchEvent", "userTask1Id");
        }

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procDefOneTask, processInstance, "userTask", "beforeCatchEvent", "userTask1Id");
            checkActivityInstances(procDefOneTask, processInstance, "intermediateCatchEvent", "intermediateCatchEvent");

            checkTaskInstance(procDefOneTask, processInstance, "beforeCatchEvent", "userTask1Id");
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateIntermediateMessageCatchingEventToIntermediateMessageCatchingEventInNewDefinition() {
        ProcessDefinition procWithSignalVer1 = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.simpleIntermediateMessageCatchEvent.bpmn20.xml");
        ProcessDefinition procWithSignalVer2 = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/simple-intermediate-message-catch-event.bpmn20.xml");

        //Start the processInstance without timer
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procWithSignalVer1.getId());

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("beforeCatchEvent");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procWithSignalVer1.getId());
        completeTask(task);

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("intermediateCatchEvent");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procWithSignalVer1.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).isEmpty();
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions)
                .extracting(EventSubscription::getActivityId, EventSubscription::getEventType)
                .containsExactly(tuple("intermediateCatchEvent", "message"));

        changeStateEventListener.clear();
        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procWithSignalVer2.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("intermediateCatchEvent", "intermediateNewCatchEvent"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions()
                .list();
        assertThat(executionsAfterMigration)
                .extracting(Execution::getActivityId)
                .containsExactly("intermediateNewCatchEvent");
        assertThat(executionsAfterMigration)
                .extracting("processDefinitionId")
                .containsOnly(procWithSignalVer2.getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).isEmpty();

        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions)
                .extracting(EventSubscription::getActivityId, EventSubscription::getEventType)
                .containsExactly(tuple("intermediateNewCatchEvent", "message"));

        // Verify events
        assertThat(changeStateEventListener.hasEvents()).isTrue();
        assertThat(changeStateEventListener.getEvents())
                .extracting(FlowableEvent::getType)
                .containsExactly(FlowableEngineEventType.ACTIVITY_MESSAGE_CANCELLED, FlowableEngineEventType.ACTIVITY_MESSAGE_WAITING);

        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        FlowableMessageEvent signalEvent = (FlowableMessageEvent) iterator.next();
        assertThat(signalEvent).extracting(FlowableMessageEvent::getType).isEqualTo(FlowableEngineEventType.ACTIVITY_MESSAGE_CANCELLED);
        assertThat(signalEvent).extracting(FlowableMessageEvent::getActivityId).isEqualTo("intermediateCatchEvent");
        assertThat(signalEvent).extracting(FlowableMessageEvent::getMessageName).isEqualTo("someMessage");

        signalEvent = (FlowableMessageEvent) iterator.next();
        assertThat(signalEvent).extracting(FlowableMessageEvent::getType).isEqualTo(FlowableEngineEventType.ACTIVITY_MESSAGE_WAITING);
        assertThat(signalEvent).extracting(FlowableMessageEvent::getActivityId).isEqualTo("intermediateNewCatchEvent");
        assertThat(signalEvent).extracting(FlowableMessageEvent::getMessageName).isEqualTo("someNewMessage");

        //Trigger the event
        Execution messageCatchExecution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId())
                .activityId("intermediateNewCatchEvent").singleResult();
        runtimeService.messageEventReceived("someNewMessage", messageCatchExecution.getId());

        executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executionsAfterMigration)
                .extracting(Execution::getActivityId)
                .containsExactly("afterNewCatchEvent");
        assertThat(executionsAfterMigration)
                .extracting("processDefinitionId")
                .containsOnly(procWithSignalVer2.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("afterNewCatchEvent");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procWithSignalVer2.getId());
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procWithSignalVer2, processInstance, "userTask", "beforeCatchEvent", "afterNewCatchEvent");
            checkActivityInstances(procWithSignalVer2, processInstance, "intermediateCatchEvent", "intermediateCatchEvent", "intermediateNewCatchEvent");

            checkTaskInstance(procWithSignalVer2, processInstance, "beforeCatchEvent", "afterNewCatchEvent");
        }

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procWithSignalVer2, processInstance, "userTask", "beforeCatchEvent", "afterNewCatchEvent");
            checkActivityInstances(procWithSignalVer2, processInstance, "intermediateCatchEvent", "intermediateCatchEvent", "intermediateNewCatchEvent");

            checkTaskInstance(procWithSignalVer2, processInstance, "beforeCatchEvent", "afterNewCatchEvent");
        }

        assertProcessEnded(processInstance.getId());
    }
    
    @Test
    public void testMigrateSimpleActivityToIntermediateEventRegistryCatchingEventInNewDefinition() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procWithEventRegistry = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.simpleIntermediateEventRegistryCatchEvent.bpmn20.xml");

        //Start the processInstance without timer
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procWithEventRegistry.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "intermediateCatchEvent"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions()
                .list();
        assertThat(executionsAfterMigration)
                .extracting(Execution::getActivityId)
                .containsExactly("intermediateCatchEvent");
        assertThat(executionsAfterMigration)
                .extracting("processDefinitionId")
                .containsOnly(procWithEventRegistry.getId());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).isEmpty();

        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions)
                .extracting(EventSubscription::getActivityId, EventSubscription::getEventType)
                .containsExactly(tuple("intermediateCatchEvent", "myEvent"));

        //Trigger the event
        runtimeService.trigger(runtimeService.createExecutionQuery().activityId("intermediateCatchEvent").singleResult().getId());

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("afterCatchEvent");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procWithEventRegistry.getId());
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procWithEventRegistry, processInstance, "userTask", "userTask1Id", "afterCatchEvent");
            checkActivityInstances(procWithEventRegistry, processInstance, "intermediateCatchEvent", "intermediateCatchEvent");

            checkTaskInstance(procWithEventRegistry, processInstance, "userTask1Id", "afterCatchEvent");
        }

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procWithEventRegistry, processInstance, "userTask", "userTask1Id", "afterCatchEvent");
            checkActivityInstances(procWithEventRegistry, processInstance, "intermediateCatchEvent", "intermediateCatchEvent");

            checkTaskInstance(procWithEventRegistry, processInstance, "userTask1Id", "afterCatchEvent");
        }

        assertProcessEnded(processInstance.getId());
    }
    
    @Test
    public void testMigrateIntermediateEventRegistryCatchingEventToSimpleActivityInNewDefinition() {
        ProcessDefinition procWithEventRegistry = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.simpleIntermediateEventRegistryCatchEvent.bpmn20.xml");
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");

        //Start the processInstance with event registry catch event
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procWithEventRegistry.getId());

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("beforeCatchEvent");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procWithEventRegistry.getId());
        completeTask(task);

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions)
                .extracting(Execution::getActivityId)
                .containsExactly("intermediateCatchEvent");
        assertThat(executions)
                .extracting("processDefinitionId")
                .containsOnly(procWithEventRegistry.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).isEmpty();
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions)
                .extracting(EventSubscription::getActivityId, EventSubscription::getEventType)
                .containsExactly(tuple("intermediateCatchEvent", "myEvent"));

        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(procDefOneTask.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("intermediateCatchEvent", "userTask1Id"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationResult.isMigrationValid()).isTrue();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions()
                .list();
        assertThat(executionsAfterMigration)
                .extracting(Execution::getActivityId)
                .containsExactly("userTask1Id");
        assertThat(executionsAfterMigration)
                .extracting("processDefinitionId")
                .containsOnly(procDefOneTask.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());

        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procDefOneTask, processInstance, "userTask", "beforeCatchEvent", "userTask1Id");
            checkActivityInstances(procDefOneTask, processInstance, "intermediateCatchEvent", "intermediateCatchEvent");

            checkTaskInstance(procDefOneTask, processInstance, "beforeCatchEvent", "userTask1Id");
        }

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            checkActivityInstances(procDefOneTask, processInstance, "userTask", "beforeCatchEvent", "userTask1Id");
            checkActivityInstances(procDefOneTask, processInstance, "intermediateCatchEvent", "intermediateCatchEvent");

            checkTaskInstance(procDefOneTask, processInstance, "beforeCatchEvent", "userTask1Id");
        }

        assertProcessEnded(processInstance.getId());
    }
    
    @Test
    public void testMultiTenantProcessInstanceMigrationWithDefaultTenantDefinition() {
        DefaultTenantProvider originalDefaultTenantValue = processEngineConfiguration.getDefaultTenantProvider();
        processEngineConfiguration.setDefaultTenantValue("default");
        processEngineConfiguration.setFallbackToDefaultTenant(true);
        
        try {
            // Deploy first version of the process
            Deployment deployment = repositoryService.createDeployment()
                    .name("my deploy")
                    .addClasspathResource("org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml")
                    .tenantId("default")
                    .deploy();
            
            ProcessDefinition version1ProcessDef = repositoryService.createProcessDefinitionQuery()
                    .deploymentId(deployment.getId()).singleResult();
    
            // Start and instance of the recent first version of the process for migration and one for reference
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKeyAndTenantId("MP", "tenant1");
    
            // Deploy second version of the process in default tenant
            deployment = repositoryService.createDeployment()
                    .name("my deploy")
                    .addClasspathResource("org/flowable/engine/test/api/runtime/migration/two-tasks-simple-process.bpmn20.xml")
                    .tenantId("default")
                    .deploy();
                
            ProcessDefinition version2ProcessDef = repositoryService.createProcessDefinitionQuery()
                    .deploymentId(deployment.getId()).singleResult();
    
            List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionKey("MP")
                    .list();
    
            assertThat(processDefinitions)
                    .extracting(ProcessDefinition::getId)
                    .containsExactlyInAnyOrder(version1ProcessDef.getId(), version2ProcessDef.getId());
    
            ProcessInstanceMigrationValidationResult validationResult = processMigrationService.createProcessInstanceMigrationBuilder()
                    .migrateToProcessDefinition(version2ProcessDef.getId())
                    .validateMigration(processInstance.getId());
    
            assertThat(validationResult.isMigrationValid()).isTrue();
    
            // Migrate process
            ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                    .migrateToProcessDefinition(version2ProcessDef.getId());
            ProcessInstanceMigrationValidationResult processInstanceMigrationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
            assertThat(processInstanceMigrationResult.isMigrationValid()).isTrue();
    
            processInstanceMigrationBuilder.migrate(processInstance.getId());
    
            List<Execution> executions = runtimeService.createExecutionQuery().list();
            assertThat(executions).hasSize(2); //includes root execution
            executions.stream()
                    .map(e -> (ExecutionEntity) e)
                    .forEach(e -> assertThat(e.getProcessDefinitionId()).isEqualTo(version2ProcessDef.getId()));
    
            List<Task> tasks = taskService.createTaskQuery().list();
            assertThat(tasks)
                    .extracting(Task::getProcessDefinitionId, Task::getTaskDefinitionKey)
                    .containsExactly(tuple(version2ProcessDef.getId(), "userTask1Id")); //AutoMapped by Id
    
            // The first process version only had one activity, there should be a second activity in the process now
            taskService.complete(tasks.get(0).getId());
            tasks = taskService.createTaskQuery().list();
            assertThat(tasks)
                    .extracting(Task::getTaskDefinitionKey)
                    .containsExactly("userTask2Id");
            taskService.complete(tasks.get(0).getId());
            assertProcessEnded(processInstance.getId());
            
        } finally {
            processEngineConfiguration.setFallbackToDefaultTenant(false);
            processEngineConfiguration.setDefaultTenantProvider(originalDefaultTenantValue);
        }
    }
    
    @Test
    public void testMultiTenantProcessInstanceMigrationWithTargetDefaultTenantDefinition() {
        DefaultTenantProvider originalDefaultTenantValue = processEngineConfiguration.getDefaultTenantProvider();
        processEngineConfiguration.setDefaultTenantValue("default");
        processEngineConfiguration.setFallbackToDefaultTenant(true);
        
        try {
            // Deploy first version of the process
            Deployment deployment = repositoryService.createDeployment()
                    .name("my deploy")
                    .addClasspathResource("org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml")
                    .tenantId("tenant1")
                    .deploy();
            
            ProcessDefinition version1ProcessDef = repositoryService.createProcessDefinitionQuery()
                    .deploymentId(deployment.getId()).singleResult();
    
            // Start and instance of the recent first version of the process for migration and one for reference
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKeyAndTenantId("MP", "tenant1");
    
            // Deploy second version of the process in default tenant
            deployment = repositoryService.createDeployment()
                    .name("my deploy")
                    .addClasspathResource("org/flowable/engine/test/api/runtime/migration/two-tasks-simple-process.bpmn20.xml")
                    .tenantId("default")
                    .deploy();
                
            ProcessDefinition version2ProcessDef = repositoryService.createProcessDefinitionQuery()
                    .deploymentId(deployment.getId()).singleResult();
    
            List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionKey("MP")
                    .list();
    
            assertThat(processDefinitions)
                    .extracting(ProcessDefinition::getId)
                    .containsExactlyInAnyOrder(version1ProcessDef.getId(), version2ProcessDef.getId());
    
            ProcessInstanceMigrationValidationResult validationResult = processMigrationService.createProcessInstanceMigrationBuilder()
                    .migrateToProcessDefinition(version2ProcessDef.getId())
                    .validateMigration(processInstance.getId());
    
            assertThat(validationResult.isMigrationValid()).isTrue();
    
            // Migrate process
            ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                    .migrateToProcessDefinition(version2ProcessDef.getId());
            ProcessInstanceMigrationValidationResult processInstanceMigrationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
            assertThat(processInstanceMigrationResult.isMigrationValid()).isTrue();
    
            processInstanceMigrationBuilder.migrate(processInstance.getId());
    
            List<Execution> executions = runtimeService.createExecutionQuery().list();
            assertThat(executions).hasSize(2); //includes root execution
            executions.stream()
                    .map(e -> (ExecutionEntity) e)
                    .forEach(e -> assertThat(e.getProcessDefinitionId()).isEqualTo(version2ProcessDef.getId()));
    
            List<Task> tasks = taskService.createTaskQuery().list();
            assertThat(tasks)
                    .extracting(Task::getProcessDefinitionId, Task::getTaskDefinitionKey)
                    .containsExactly(tuple(version2ProcessDef.getId(), "userTask1Id")); //AutoMapped by Id
    
            // The first process version only had one activity, there should be a second activity in the process now
            taskService.complete(tasks.get(0).getId());
            tasks = taskService.createTaskQuery().list();
            assertThat(tasks)
                    .extracting(Task::getTaskDefinitionKey)
                    .containsExactly("userTask2Id");
            taskService.complete(tasks.get(0).getId());
            assertProcessEnded(processInstance.getId());
            
        } finally {
            processEngineConfiguration.setFallbackToDefaultTenant(false);
            processEngineConfiguration.setDefaultTenantProvider(originalDefaultTenantValue);
        }
    }
    
    @Test
    public void testMultiTenantProcessInstanceMigrationWithDefaultTenantDefinitionFailsWithNoFallback() {
        DefaultTenantProvider originalDefaultTenantValue = processEngineConfiguration.getDefaultTenantProvider();
        processEngineConfiguration.setDefaultTenantValue("default");
        
        try {
            // Deploy first version of the process
            Deployment deployment = repositoryService.createDeployment()
                    .name("my deploy")
                    .addClasspathResource("org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml")
                    .tenantId("tenant1")
                    .deploy();
            
            ProcessDefinition version1ProcessDef = repositoryService.createProcessDefinitionQuery()
                    .deploymentId(deployment.getId()).singleResult();
    
            // Start and instance of the recent first version of the process for migration and one for reference
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKeyAndTenantId("MP", "tenant1");
    
            // Deploy second version of the process in default tenant
            deployment = repositoryService.createDeployment()
                    .name("my deploy")
                    .addClasspathResource("org/flowable/engine/test/api/runtime/migration/two-tasks-simple-process.bpmn20.xml")
                    .tenantId("default")
                    .deploy();
                
            ProcessDefinition version2ProcessDef = repositoryService.createProcessDefinitionQuery()
                    .deploymentId(deployment.getId()).singleResult();
    
            ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                    .migrateToProcessDefinition(version2ProcessDef.getId());
            
            assertThatThrownBy(() -> {
                processInstanceMigrationBuilder.migrate(processInstance.getId());
            }).isInstanceOf(FlowableException.class).hasMessage("Tenant mismatch between Process Instance ('tenant1') and Process Definition ('default') to migrate to");
    
            List<Execution> executions = runtimeService.createExecutionQuery().list();
            assertThat(executions).hasSize(2); //includes root execution
            executions.stream()
                    .map(e -> (ExecutionEntity) e)
                    .forEach(e -> assertThat(e.getProcessDefinitionId()).isEqualTo(version1ProcessDef.getId()));
            
        } finally {
            processEngineConfiguration.setDefaultTenantProvider(originalDefaultTenantValue);
        }
    }

    @Test
    public void preUpgradeScriptMigration() {
        //Deploy first version of the process
        deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/serializable-variable-process.bpmn20.xml");

        //Start and instance of the recent first version of the process for migration and one for reference
        ProcessInstance processInstanceToMigrate = runtimeService.startProcessInstanceByKey("MP", Collections.singletonMap("listVariable", new ArrayList()));
        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstanceToMigrate.getId()).onlyChildExecutions().singleResult();
        runtimeService.trigger(execution.getId());

        assertThat((List) runtimeService.getVariable(processInstanceToMigrate.getId(), "listVariable")).contains("new value");

        ProcessDefinition targetProcessDefinition = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/json-variable-process.bpmn20.xml");

        processMigrationService.createProcessInstanceMigrationBuilder().preUpgradeScript(new Script("groovy",
                "import com.fasterxml.jackson.databind.ObjectMapper\n"
                        + "import com.fasterxml.jackson.databind.node.ArrayNode\n"
                        + "import org.flowable.engine.impl.context.Context\n"
                        + "\n"
                        + "List<String> list  = execution.getVariable('listVariable')\n"
                        + "\n"
                        + "ObjectMapper mapper = Context.getProcessEngineConfiguration().getObjectMapper()\n"
                        + "\n"
                        + "ArrayNode jsonArray = mapper.createArrayNode()\n"
                        + "list.each {jsonArray.add(it)}\n"
                        + "\n"
                        + "execution.setVariable(\"listVariable\", jsonArray)"))
                .migrateToProcessDefinition(targetProcessDefinition.getId())
                .migrate(processInstanceToMigrate.getId());

        assertThatProcessVariableConverted(processInstanceToMigrate, execution);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
            // serializable - when starting the process instance
            // serializable - when triggering the received task (the value is updated so new detail is created)
            // json - when doing the migration new variable is set
            // json - when triggering the received task (the value is updated so new details is created 
            assertThatVariablesTypeHistoryIs(processInstanceToMigrate, "serializable", "serializable", "json", "json");
        }
    }

    @Test
    public void preUpgradeJavaDelegate() {
        //Deploy first version of the process
        deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/serializable-variable-process.bpmn20.xml");

        ProcessInstance processInstanceToMigrate = runtimeService.startProcessInstanceByKey("MP", Collections.singletonMap("listVariable", new ArrayList()));
        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstanceToMigrate.getId()).onlyChildExecutions().singleResult();
        runtimeService.trigger(execution.getId());

        assertThat((List) runtimeService.getVariable(processInstanceToMigrate.getId(), "listVariable")).contains("new value");

        ProcessDefinition targetProcessDefinition = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/json-variable-process.bpmn20.xml");

        processMigrationService.createProcessInstanceMigrationBuilder()
                .preUpgradeJavaDelegate("org.flowable.engine.test.api.runtime.migration.ConvertProcessVariable")
                .migrateToProcessDefinition(targetProcessDefinition.getId())
                .migrate(processInstanceToMigrate.getId());

        assertThatProcessVariableConverted(processInstanceToMigrate, execution);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
            // serializable - when starting the process instance
            // serializable - when triggering the received task (the value is updated so new detail is created)
            // json - when doing the migration new variable is set
            // json - when triggering the received task (the value is updated so new details is created
            assertThatVariablesTypeHistoryIs(processInstanceToMigrate, "serializable", "serializable", "json", "json");
        }
    }

    @Test
    public void preUpgradeJavaDelegateExpression() {
        //Deploy first version of the process
        deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/serializable-variable-process.bpmn20.xml");

        Map<String, Object> variables = new HashMap<>();
        variables.put("listVariable", new ArrayList());
        variables.put("convertProcessVariable", new ConvertProcessVariable()); // using instead of beans. Not nice, but it uses similar resolver as beans
        ProcessInstance processInstanceToMigrate = runtimeService
                .startProcessInstanceByKey("MP", variables);
        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstanceToMigrate.getId()).onlyChildExecutions()
                .singleResult();
        runtimeService.trigger(execution.getId());

        assertThat((List) runtimeService.getVariable(processInstanceToMigrate.getId(), "listVariable")).contains("new value");

        ProcessDefinition targetProcessDefinition = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/json-variable-process.bpmn20.xml");

        processMigrationService.createProcessInstanceMigrationBuilder()
                .preUpgradeJavaDelegateExpression("${convertProcessVariable}")
                .migrateToProcessDefinition(targetProcessDefinition.getId())
                .migrate(processInstanceToMigrate.getId());

        assertThatProcessVariableConverted(processInstanceToMigrate, execution);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
            // serializable - when starting the process instance (listVariable)
            // serializable - when starting the process instance (convertProcessVariable)
            // serializable - when triggering the received task (the value is updated so new detail is created)
            // json - when doing the migration new variable is set
            // json - when triggering the received task (the value is updated so new details is created
            assertThatVariablesTypeHistoryIs(processInstanceToMigrate, "serializable", "serializable", "serializable", "json", "json");
        }
    }

    @Test
    public void postUpgradeScriptMigration() {
        //Deploy first version of the process
        deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/serializable-variable-process.bpmn20.xml");

        //Start and instance of the recent first version of the process for migration and one for reference
        ProcessInstance processInstanceToMigrate = runtimeService.startProcessInstanceByKey("MP", Collections.singletonMap("listVariable", new ArrayList()));
        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstanceToMigrate.getId()).onlyChildExecutions().singleResult();
        runtimeService.trigger(execution.getId());

        assertThat((List) runtimeService.getVariable(processInstanceToMigrate.getId(), "listVariable")).contains("new value");

        ProcessDefinition targetProcessDefinition = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/json-variable-process.bpmn20.xml");

        processMigrationService.createProcessInstanceMigrationBuilder().postUpgradeScript(new Script("groovy",
                "import com.fasterxml.jackson.databind.ObjectMapper\n"
                        + "import com.fasterxml.jackson.databind.node.ArrayNode\n"
                        + "import org.flowable.engine.impl.context.Context\n"
                        + "\n"
                        + "List<String> list  = execution.getVariable('listVariable')\n"
                        + "\n"
                        + "ObjectMapper mapper = Context.getProcessEngineConfiguration().getObjectMapper()\n"
                        + "\n"
                        + "ArrayNode jsonArray = mapper.createArrayNode()\n"
                        + "list.each {jsonArray.add(it)}\n"
                        + "\n"
                        + "execution.setVariable(\"listVariable\", jsonArray)"))
                .migrateToProcessDefinition(targetProcessDefinition.getId())
                .migrate(processInstanceToMigrate.getId());

        assertThatProcessVariableConverted(processInstanceToMigrate, execution);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
            // serializable - when starting the process instance
            // serializable - when triggering the received task (the value is updated so new detail is created)
            // json - when doing the migration new variable is set
            // json - when triggering the received task (the value is updated so new details is created
            assertThatVariablesTypeHistoryIs(processInstanceToMigrate, "serializable", "serializable", "json", "json");
        }
    }

    @Test
    public void postUpgradeJavaDelegate() {
        //Deploy first version of the process
        deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/serializable-variable-process.bpmn20.xml");

        ProcessInstance processInstanceToMigrate = runtimeService.startProcessInstanceByKey("MP", Collections.singletonMap("listVariable", new ArrayList()));
        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstanceToMigrate.getId()).onlyChildExecutions().singleResult();
        runtimeService.trigger(execution.getId());

        assertThat((List) runtimeService.getVariable(processInstanceToMigrate.getId(), "listVariable")).contains("new value");

        ProcessDefinition targetProcessDefinition = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/json-variable-process.bpmn20.xml");

        processMigrationService.createProcessInstanceMigrationBuilder()
                .postUpgradeJavaDelegate("org.flowable.engine.test.api.runtime.migration.ConvertProcessVariable")
                .migrateToProcessDefinition(targetProcessDefinition.getId())
                .migrate(processInstanceToMigrate.getId());

        assertThatProcessVariableConverted(processInstanceToMigrate, execution);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
            // serializable - when starting the process instance
            // serializable - when triggering the received task (the value is updated so new detail is created)
            // json - when doing the migration new variable is set
            // json - when triggering the received task (the value is updated so new details is created
            assertThatVariablesTypeHistoryIs(processInstanceToMigrate, "serializable", "serializable", "json", "json");
        }
    }

    @Test
    public void postUpgradeJavaDelegateExpression() {
        //Deploy first version of the process
        deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/serializable-variable-process.bpmn20.xml");

        Map<String, Object> variables = new HashMap<>();
        variables.put("listVariable", new ArrayList());
        variables.put("convertProcessVariable", new ConvertProcessVariable()); // using instead of beans. Not nice, but it uses similar resolver as beans
        ProcessInstance processInstanceToMigrate = runtimeService
                .startProcessInstanceByKey("MP", variables);
        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstanceToMigrate.getId()).onlyChildExecutions()
                .singleResult();
        runtimeService.trigger(execution.getId());

        assertThat((List) runtimeService.getVariable(processInstanceToMigrate.getId(), "listVariable")).contains("new value");

        ProcessDefinition targetProcessDefinition = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/json-variable-process.bpmn20.xml");

        processMigrationService.createProcessInstanceMigrationBuilder()
                .postUpgradeJavaDelegateExpression("${convertProcessVariable}")
                .migrateToProcessDefinition(targetProcessDefinition.getId())
                .migrate(processInstanceToMigrate.getId());

        assertThatProcessVariableConverted(processInstanceToMigrate, execution);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
            assertThatVariablesTypeHistoryIs(processInstanceToMigrate, "serializable", "serializable", "serializable", "json", "json");
        }

    }

    private void assertThatVariablesTypeHistoryIs(ProcessInstance processInstanceToMigrate, String... values) {
        List<HistoricDetail> updateList = historyService.createHistoricDetailQuery().processInstanceId(processInstanceToMigrate.getId()).variableUpdates()
                .list();

        updateList.sort(Comparator.comparingInt(histDetail -> Integer.parseInt(histDetail.getId())));
        assertThat(updateList)
                .extracting(historicDetail -> ((HistoricDetailVariableInstanceUpdateEntity) historicDetail).getVariableType().getTypeName())
                .containsExactly(values);
    }

    private void assertThatProcessVariableConverted(ProcessInstance processInstanceToMigrate, Execution execution) {
        assertThat((ArrayNode) runtimeService.getVariable(processInstanceToMigrate.getId(), "listVariable"))
                .extracting(jsonNode -> jsonNode.asText())
                .containsExactly("new value");

        runtimeService.trigger(execution.getId());

        assertThat((ArrayNode) runtimeService.getVariable(processInstanceToMigrate.getId(), "listVariable"))
                .extracting(jsonNode -> jsonNode.asText())
                .containsExactly("new value", "new value 2");

        runtimeService.trigger(execution.getId());

        assertProcessEnded(processInstanceToMigrate.getId());
    }
}
