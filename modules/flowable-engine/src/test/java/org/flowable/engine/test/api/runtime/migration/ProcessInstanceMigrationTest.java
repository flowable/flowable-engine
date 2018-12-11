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
import static org.junit.Assert.assertNotEquals;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.delegate.event.FlowableActivityEvent;
import org.flowable.engine.delegate.event.FlowableMessageEvent;
import org.flowable.engine.delegate.event.FlowableSignalEvent;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.migration.ProcessInstanceMigrationValidationResult;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.migration.ActivityMigrationMapping;
import org.flowable.engine.migration.ProcessInstanceMigrationBuilder;
import org.flowable.engine.migration.ProcessInstanceMigrationDocument;
import org.flowable.engine.migration.ProcessInstanceMigrationDocumentConverter;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.DataObject;
import org.flowable.engine.runtime.EventSubscription;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.api.runtime.changestate.ChangeStateEventListener;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Dennis Federico
 */
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
        ProcessDefinition version1ProcessDef = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");

        //Start and instance of the recent first version of the process for migration and one for reference
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("MP");

        //Deploy second version of the process
        ProcessDefinition version2ProcessDef = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/two-tasks-simple-process.bpmn20.xml");

        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
            .processDefinitionKey("MP")
            .list();

        assertEquals(2, processDefinitions.size());
        processDefinitions.sort(Comparator.comparingInt(ProcessDefinition::getVersion));
        assertEquals(processDefinitions.get(0).getId(), version1ProcessDef.getId());
        assertEquals(processDefinitions.get(1).getId(), version2ProcessDef.getId());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, executions.size()); //includes root execution
        executions.stream()
            .map(e -> (ExecutionEntity) e)
            .forEach(e -> assertEquals(version1ProcessDef.getId(), e.getProcessDefinitionId()));

        List<Task> tasks = taskService.createTaskQuery().list();
        assertEquals(1, tasks.size());
        assertEquals(version1ProcessDef.getId(), tasks.get(0).getProcessDefinitionId());
        assertEquals("userTask1Id", tasks.get(0).getTaskDefinitionKey());

        ProcessInstanceMigrationValidationResult validationResult = runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(version2ProcessDef.getId())
            .validateMigration(processInstance.getId());

        assertEquals(false, validationResult.hasErrors());
        assertEquals(true, validationResult.isMigrationValid());
        assertEquals(0, validationResult.getValidationMessages().size());

        //Migrate process
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(version2ProcessDef.getId());
        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        executions = runtimeService.createExecutionQuery().list();
        assertEquals(2, executions.size()); //includes root execution
        executions.stream()
            .map(e -> (ExecutionEntity) e)
            .forEach(e -> assertEquals(version2ProcessDef.getId(), e.getProcessDefinitionId()));

        tasks = taskService.createTaskQuery().list();
        assertEquals(1, tasks.size());
        assertEquals(version2ProcessDef.getId(), tasks.get(0).getProcessDefinitionId());
        assertEquals("userTask1Id", tasks.get(0).getTaskDefinitionKey()); //AutoMapped by Id

        //The first process version only had one activity, there should be a second activity in the process now
        taskService.complete(tasks.get(0).getId());
        tasks = taskService.createTaskQuery().list();
        assertEquals(1, tasks.size());
        assertEquals("userTask2Id", tasks.get(0).getTaskDefinitionKey());
        taskService.complete(tasks.get(0).getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testSimpleMigrationWithTaskMapping() {
        //Deploy first version of the process
        ProcessDefinition version1ProcessDef = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/two-tasks-simple-process.bpmn20.xml");

        //Start and instance of the recent first version of the process for migration and one for reference
        ProcessInstance processInstanceToMigrate = runtimeService.startProcessInstanceByKey("MP");
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstanceToMigrate.getId()).singleResult().getId());

        Task beforeMigrationTask = taskService.createTaskQuery().processInstanceId(processInstanceToMigrate.getId()).singleResult();

        //Deploy second version of the process
        ProcessDefinition version2ProcessDef = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/three-tasks-simple-process.bpmn20.xml");

        //Migrate process
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(version2ProcessDef.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask2Id", "intermediateTask"))
            .migrate(processInstanceToMigrate.getId());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstanceToMigrate.getId()).list();
        assertEquals(2, executions.size()); //includes root execution
        for (Execution execution : executions) {
            assertEquals(version2ProcessDef.getId(), ((ExecutionEntity) execution).getProcessDefinitionId());
        }

        Task task = taskService.createTaskQuery().processInstanceId(processInstanceToMigrate.getId()).singleResult();
        assertEquals(version2ProcessDef.getId(), task.getProcessDefinitionId());
        assertEquals("intermediateTask", task.getTaskDefinitionKey());
        assertEquals(beforeMigrationTask.getId(), task.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceToMigrate.getId()).singleResult();
            assertEquals(version2ProcessDef.getId(), historicProcessInstance.getProcessDefinitionId());

            List<HistoricTaskInstance> historicTaskInstances = historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstanceToMigrate.getId()).list();
            assertEquals(2, historicTaskInstances.size());
            for (HistoricTaskInstance historicTaskInstance : historicTaskInstances) {
                assertEquals(version2ProcessDef.getId(), historicTaskInstance.getProcessDefinitionId());
            }

            List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstanceToMigrate.getId()).list();
            assertEquals(3, historicActivityInstances.size());
            for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
                assertEquals(version2ProcessDef.getId(), historicActivityInstance.getProcessDefinitionId());
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
        ProcessDefinition version1ProcessDef = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/two-tasks-simple-process.bpmn20.xml");

        //Start and instance of the recent first version of the process for migration and one for reference
        ProcessInstance processInstanceToMigrate = runtimeService.startProcessInstanceByKey("MP");
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstanceToMigrate.getId()).singleResult().getId());

        Task beforeMigrationTask = taskService.createTaskQuery().processInstanceId(processInstanceToMigrate.getId()).singleResult();

        //Deploy second version of the process
        ProcessDefinition version2ProcessDef = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/three-tasks-simple-process.bpmn20.xml");

        ObjectMapper objectMapper = processEngineConfiguration.getObjectMapper();
        ObjectNode migrationNode = objectMapper.createObjectNode();
        migrationNode.put("toProcessDefinitionId", version2ProcessDef.getId());
        ArrayNode activitiesNode = migrationNode.putArray("activityMappings");
        ObjectNode activityNode = activitiesNode.addObject();
        activityNode.put("fromActivityId", "userTask2Id");
        activityNode.put("toActivityId", "intermediateTask");

        //Migrate process
        ProcessInstanceMigrationDocument migrationDocument = ProcessInstanceMigrationDocumentConverter.convertFromJson(migrationNode.toString());
        runtimeService.migrateProcessInstance(processInstanceToMigrate.getId(), migrationDocument);

        Task task = taskService.createTaskQuery().processInstanceId(processInstanceToMigrate.getId()).singleResult();
        assertEquals(version2ProcessDef.getId(), task.getProcessDefinitionId());
        assertEquals("intermediateTask", task.getTaskDefinitionKey());
        assertEquals(beforeMigrationTask.getId(), task.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceToMigrate.getId()).singleResult();
            assertEquals(version2ProcessDef.getId(), historicProcessInstance.getProcessDefinitionId());

            List<HistoricTaskInstance> historicTaskInstances = historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstanceToMigrate.getId()).list();
            assertEquals(2, historicTaskInstances.size());
            for (HistoricTaskInstance historicTaskInstance : historicTaskInstances) {
                assertEquals(version2ProcessDef.getId(), historicTaskInstance.getProcessDefinitionId());
            }

            List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstanceToMigrate.getId()).list();
            assertEquals(3, historicActivityInstances.size());
            for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
                assertEquals(version2ProcessDef.getId(), historicActivityInstance.getProcessDefinitionId());
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
        ProcessDefinition version1ProcessDef = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/parallel-gateway-two-tasks.bpmn20.xml");

        //Start and instance of the recent first version of the process for migration and one for reference
        ProcessInstance processInstanceToMigrate = runtimeService.startProcessInstanceByKey("startParallelProcess");
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstanceToMigrate.getId()).singleResult().getId());

        List<Task> parallelTasks = taskService.createTaskQuery().processInstanceId(processInstanceToMigrate.getId()).list();
        assertEquals(2, parallelTasks.size());

        //Deploy second version of the process
        ProcessDefinition version2ProcessDef = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/parallel-gateway-two-tasks-and-before.bpmn20.xml");

        List<String> fromActivityIds = new ArrayList<>();
        fromActivityIds.add("parallelTask1");
        fromActivityIds.add("parallelTask2");
        //Migrate process
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(version2ProcessDef.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor(fromActivityIds, "beforeTask"))
            .migrate(processInstanceToMigrate.getId());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstanceToMigrate.getId()).list();
        assertEquals(2, executions.size()); //includes root execution
        for (Execution execution : executions) {
            assertEquals(version2ProcessDef.getId(), ((ExecutionEntity) execution).getProcessDefinitionId());
        }

        Task task = taskService.createTaskQuery().processInstanceId(processInstanceToMigrate.getId()).singleResult();
        assertEquals(version2ProcessDef.getId(), task.getProcessDefinitionId());
        assertEquals("beforeTask", task.getTaskDefinitionKey());
        assertNotEquals(parallelTasks.get(0).getId(), task.getId());
        assertNotEquals(parallelTasks.get(1).getId(), task.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceToMigrate.getId()).singleResult();
            assertEquals(version2ProcessDef.getId(), historicProcessInstance.getProcessDefinitionId());

            List<HistoricTaskInstance> historicTaskInstances = historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstanceToMigrate.getId()).list();
            assertEquals(4, historicTaskInstances.size());
            for (HistoricTaskInstance historicTaskInstance : historicTaskInstances) {
                assertEquals(version2ProcessDef.getId(), historicTaskInstance.getProcessDefinitionId());
            }

            List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstanceToMigrate.getId()).list();
            assertEquals(6, historicActivityInstances.size());
            for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
                assertEquals(version2ProcessDef.getId(), historicActivityInstance.getProcessDefinitionId());
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
        ProcessDefinition version1ProcessDef = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/two-tasks-simple-process.bpmn20.xml");

        //Start and instance of the recent first version of the process for migration and one for reference
        ProcessInstance processInstanceToMigrate = runtimeService.startProcessInstanceByKey("MP");
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstanceToMigrate.getId()).singleResult().getId());

        Task lastTask = taskService.createTaskQuery().processInstanceId(processInstanceToMigrate.getId()).singleResult();

        //Deploy second version of the process
        ProcessDefinition version2ProcessDef = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/three-tasks-with-sub-process.bpmn20.xml");

        //Migrate process
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(version2ProcessDef.getId())
            .addActivityMigrationMapping(
                ActivityMigrationMapping.createMappingFor("userTask2Id", "subScriptTask")
                    .withLocalVariable("subprocessVariable", "passedValue"))
            .migrate(processInstanceToMigrate.getId());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstanceToMigrate.getId()).list();
        assertEquals(3, executions.size()); //includes root execution
        for (Execution execution : executions) {
            assertEquals(version2ProcessDef.getId(), ((ExecutionEntity) execution).getProcessDefinitionId());
        }

        Execution execution = runtimeService.createExecutionQuery().activityId("subProcess").singleResult();
        assertEquals("passedValue", runtimeService.getVariable(execution.getId(), "subprocessVariable"));
        assertFalse(runtimeService.hasVariable(execution.getProcessInstanceId(), "subprocessVariable"));
        assertEquals("hello", runtimeService.getVariable(execution.getId(), "anotherVar"));
        assertFalse(runtimeService.hasVariable(execution.getProcessInstanceId(), "anotherVar"));

        Task task = taskService.createTaskQuery().processInstanceId(processInstanceToMigrate.getId()).singleResult();
        assertEquals(version2ProcessDef.getId(), task.getProcessDefinitionId());
        assertEquals("subTask", task.getTaskDefinitionKey());
        assertNotEquals(lastTask.getId(), task.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceToMigrate.getId()).singleResult();
            assertEquals(version2ProcessDef.getId(), historicProcessInstance.getProcessDefinitionId());

            List<HistoricTaskInstance> historicTaskInstances = historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstanceToMigrate.getId()).list();
            assertEquals(3, historicTaskInstances.size());
            for (HistoricTaskInstance historicTaskInstance : historicTaskInstances) {
                assertEquals(version2ProcessDef.getId(), historicTaskInstance.getProcessDefinitionId());
            }

            List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstanceToMigrate.getId()).list();
            assertEquals(6, historicActivityInstances.size());
            for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
                assertEquals(version2ProcessDef.getId(), historicActivityInstance.getProcessDefinitionId());
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

        assertEquals(2, processDefinitions.size());

        ProcessDefinition version1ProcessDef = processDefinitions.stream().filter(d -> d.getVersion() == 1).findFirst().get();
        assertEquals(oneActivityProcessDeployment.getId(), version1ProcessDef.getDeploymentId());
        ProcessDefinition version2ProcessDef = processDefinitions.stream().filter(d -> d.getVersion() == 2).findFirst().get();
        assertEquals(twoActivitiesProcessDeployment.getId(), version2ProcessDef.getDeploymentId());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, executions.size()); //includes root execution
        executions.stream()
            .map(e -> (ExecutionEntity) e)
            .forEach(e -> assertEquals(version1ProcessDef.getId(), e.getProcessDefinitionId()));

        List<Task> tasks = taskService.createTaskQuery().list();
        assertEquals(1, tasks.size());
        assertEquals(version1ProcessDef.getId(), tasks.get(0).getProcessDefinitionId());
        assertEquals("userTask1Id", tasks.get(0).getTaskDefinitionKey());

        ProcessInstanceMigrationValidationResult validationResult = runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(version2ProcessDef.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "userTask1Id"))
            .validateMigration(processInstance.getId());

        assertEquals(false, validationResult.hasErrors());
        assertEquals(true, validationResult.isMigrationValid());
        assertEquals(0, validationResult.getValidationMessages().size());

        //Migrate process - moving the current execution explicitly
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(version2ProcessDef.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "userTask1Id"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        executions = runtimeService.createExecutionQuery().list();
        assertEquals(2, executions.size()); //includes root execution
        executions.stream()
            .map(e -> (ExecutionEntity) e)
            .forEach(e -> assertEquals(version2ProcessDef.getId(), e.getProcessDefinitionId()));

        tasks = taskService.createTaskQuery().list();
        assertEquals(1, tasks.size());
        assertEquals(version2ProcessDef.getId(), tasks.get(0).getProcessDefinitionId());
        assertEquals("userTask1Id", tasks.get(0).getTaskDefinitionKey());

        //This new process definition has two activities
        taskService.complete(tasks.get(0).getId());
        tasks = taskService.createTaskQuery().list();
        assertEquals(1, tasks.size());
        assertEquals(version2ProcessDef.getId(), tasks.get(0).getProcessDefinitionId());
        assertEquals("userTask2Id", tasks.get(0).getTaskDefinitionKey());
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

        assertEquals(2, processDefinitions.size());

        ProcessDefinition version1ProcessDef = processDefinitions.stream().filter(d -> d.getVersion() == 1).findFirst().get();
        assertEquals(oneActivityProcessDeployment.getId(), version1ProcessDef.getDeploymentId());
        ProcessDefinition version2ProcessDef = processDefinitions.stream().filter(d -> d.getVersion() == 2).findFirst().get();
        assertEquals(twoActivitiesProcessDeployment.getId(), version2ProcessDef.getDeploymentId());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, executions.size()); //includes root execution
        executions.stream()
            .map(e -> (ExecutionEntity) e)
            .forEach(e -> assertEquals(version1ProcessDef.getId(), e.getProcessDefinitionId()));

        List<Task> tasks = taskService.createTaskQuery().list();
        assertEquals(1, tasks.size());
        assertEquals(version1ProcessDef.getId(), tasks.get(0).getProcessDefinitionId());
        assertEquals("userTask1Id", tasks.get(0).getTaskDefinitionKey());

        ProcessInstanceMigrationValidationResult validationResult = runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(version2ProcessDef.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "userTask3Id"))
            .validateMigration(processInstance.getId());

        assertEquals(true, validationResult.hasErrors());
        assertEquals(false, validationResult.isMigrationValid());
        assertEquals(1, validationResult.getValidationMessages().size());

        validationResult = runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(version2ProcessDef.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "userTask2Id"))
            .validateMigration(processInstance.getId());

        assertEquals(false, validationResult.hasErrors());
        assertEquals(true, validationResult.isMigrationValid());
        assertEquals(0, validationResult.getValidationMessages().size());

        //Migrate process - moving the current execution explicitly
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(version2ProcessDef.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "userTask2Id"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        executions = runtimeService.createExecutionQuery().list();
        assertEquals(2, executions.size()); //includes root execution
        executions.stream()
            .map(e -> (ExecutionEntity) e)
            .forEach(e -> assertEquals(version2ProcessDef.getId(), e.getProcessDefinitionId()));

        tasks = taskService.createTaskQuery().list();
        assertEquals(1, tasks.size());
        assertEquals(version2ProcessDef.getId(), tasks.get(0).getProcessDefinitionId());
        assertEquals("userTask2Id", tasks.get(0).getTaskDefinitionKey());

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

        assertEquals(2, processDefinitions.size());

        ProcessDefinition version1ProcessDef = processDefinitions.stream().filter(d -> d.getVersion() == 1).findFirst().get();
        assertEquals(twoActivitiesProcessDeployment.getId(), version1ProcessDef.getDeploymentId());
        ProcessDefinition version2ProcessDef = processDefinitions.stream().filter(d -> d.getVersion() == 2).findFirst().get();
        assertEquals(oneActivityProcessDeployment.getId(), version2ProcessDef.getDeploymentId());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, executions.size()); //includes root execution
        executions.stream()
            .map(e -> (ExecutionEntity) e)
            .forEach(e -> assertEquals(version1ProcessDef.getId(), e.getProcessDefinitionId()));

        List<Task> tasks = taskService.createTaskQuery().list();
        assertEquals(1, tasks.size());
        assertEquals(version1ProcessDef.getId(), tasks.get(0).getProcessDefinitionId());
        assertEquals("userTask1Id", tasks.get(0).getTaskDefinitionKey());

        //We want to migrate from the next activity
        taskService.complete(tasks.get(0).getId());
        tasks = taskService.createTaskQuery().list();
        assertEquals(1, tasks.size());
        assertEquals(version1ProcessDef.getId(), tasks.get(0).getProcessDefinitionId());
        assertEquals("userTask2Id", tasks.get(0).getTaskDefinitionKey());

        //Migrate process - moving the current execution explicitly
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(version2ProcessDef.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask2Id", "userTask1Id"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        executions = runtimeService.createExecutionQuery().list();
        assertEquals(2, executions.size()); //includes root execution
        executions.stream()
            .map(e -> (ExecutionEntity) e)
            .forEach(e -> assertEquals(version2ProcessDef.getId(), e.getProcessDefinitionId()));

        tasks = taskService.createTaskQuery().list();
        assertEquals(1, tasks.size());
        assertEquals("userTask1Id", tasks.get(0).getTaskDefinitionKey());
        assertEquals(version2ProcessDef.getId(), tasks.get(0).getProcessDefinitionId());

        //This new process version only have one activity
        taskService.complete(tasks.get(0).getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testSimpleUserTaskDirectMigration() {

        //Almost all tests use UserTask, thus are direct migrations, but this one checks explicitly for changes in History
        //Deploy first version of the process
        ProcessDefinition version1ProcessDef = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");

        //Start and instance of the recent first version of the process for migration and one for reference
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("MP");

        //Deploy second version of the process
        ProcessDefinition version2ProcessDef = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/two-tasks-simple-process.bpmn20.xml");

        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
            .processDefinitionKey("MP")
            .list();

        assertEquals(2, processDefinitions.size());
        processDefinitions.sort(Comparator.comparingInt(ProcessDefinition::getVersion));
        assertEquals(processDefinitions.get(0).getId(), version1ProcessDef.getId());
        assertEquals(processDefinitions.get(1).getId(), version2ProcessDef.getId());

        List<Execution> executionsBefore = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, executionsBefore.size()); //includes root execution
        executionsBefore.stream()
            .map(e -> (ExecutionEntity) e)
            .forEach(e -> assertEquals(version1ProcessDef.getId(), e.getProcessDefinitionId()));

        List<Task> tasksBefore = taskService.createTaskQuery().list();
        assertEquals(1, tasksBefore.size());
        assertEquals(version1ProcessDef.getId(), tasksBefore.get(0).getProcessDefinitionId());
        assertEquals("userTask1Id", tasksBefore.get(0).getTaskDefinitionKey());

        List<HistoricActivityInstance> historicActivityInstancesBefore = null;
        List<HistoricTaskInstance> historicTaskInstancesBefore = null;
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            historicActivityInstancesBefore = historyService.createHistoricActivityInstanceQuery().orderByExecutionId().asc().list();

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                historicTaskInstancesBefore = historyService.createHistoricTaskInstanceQuery().orderByExecutionId().asc().list();
            }
        }

        //Migrate process
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(version2ProcessDef.getId());

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        List<Execution> executionsAfter = runtimeService.createExecutionQuery().list();
        assertEquals(2, executionsAfter.size()); //includes root execution
        executionsAfter.stream()
            .map(e -> (ExecutionEntity) e)
            .forEach(e -> assertEquals(version2ProcessDef.getId(), e.getProcessDefinitionId()));

        List<Task> tasksAfter = taskService.createTaskQuery().list();
        assertEquals(1, tasksAfter.size());
        assertEquals(version2ProcessDef.getId(), tasksAfter.get(0).getProcessDefinitionId());
        assertEquals("userTask1Id", tasksAfter.get(0).getTaskDefinitionKey()); //AutoMapped by Id

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            List<HistoricActivityInstance> historicActivityInstancesAfter = historyService.createHistoricActivityInstanceQuery().orderByExecutionId().asc().list();
            assertEquals(historicActivityInstancesBefore.size(), historicActivityInstancesAfter.size());
            assertThat(historicActivityInstancesBefore)
                .usingElementComparatorIgnoringFields("revision", "processDefinitionId")
                .containsExactlyInAnyOrderElementsOf(historicActivityInstancesAfter);

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTaskInstancesAfter = historyService.createHistoricTaskInstanceQuery().orderByExecutionId().asc().list();

                assertEquals(historicTaskInstancesBefore.size(), historicTaskInstancesAfter.size());
                assertThat(historicTaskInstancesBefore)
                    .usingElementComparatorIgnoringFields("revision", "processDefinitionId", "originalPersistentState", "lastUpdateTime")
                    .containsExactlyInAnyOrderElementsOf(historicTaskInstancesAfter);
            }
        }

        //The first process version only had one activity, there should be a second activity in the process now
        taskService.complete(tasksAfter.get(0).getId());
        tasksAfter = taskService.createTaskQuery().list();
        assertEquals(1, tasksAfter.size());
        assertEquals("userTask2Id", tasksAfter.get(0).getTaskDefinitionKey());
        taskService.complete(tasksAfter.get(0).getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testSimpleUserTaskDirectMigrationReAssign() {

        //Almost all tests use UserTask, thus are direct migrations, but this one checks explicitly for changes in History
        //Deploy first version of the process
        ProcessDefinition version1ProcessDef = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");

        //Start and instance of the recent first version of the process for migration and one for reference
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("MP");

        //Deploy second version of the process
        ProcessDefinition version2ProcessDef = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/two-tasks-simple-process.bpmn20.xml");

        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
            .processDefinitionKey("MP")
            .list();

        assertEquals(2, processDefinitions.size());
        processDefinitions.sort(Comparator.comparingInt(ProcessDefinition::getVersion));
        assertEquals(processDefinitions.get(0).getId(), version1ProcessDef.getId());
        assertEquals(processDefinitions.get(1).getId(), version2ProcessDef.getId());

        List<Execution> executionsBefore = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, executionsBefore.size()); //includes root execution
        executionsBefore.stream()
            .map(e -> (ExecutionEntity) e)
            .forEach(e -> assertEquals(version1ProcessDef.getId(), e.getProcessDefinitionId()));

        List<Task> tasksBefore = taskService.createTaskQuery().list();
        assertEquals(1, tasksBefore.size());
        assertEquals(version1ProcessDef.getId(), tasksBefore.get(0).getProcessDefinitionId());
        assertEquals("userTask1Id", tasksBefore.get(0).getTaskDefinitionKey());
        assertThat(tasksBefore).extracting(Task::getAssignee).containsNull();

        List<HistoricActivityInstance> historicActivityInstancesBefore = null;
        List<HistoricTaskInstance> historicTaskInstancesBefore = null;
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            historicActivityInstancesBefore = historyService.createHistoricActivityInstanceQuery().orderByExecutionId().asc().list();

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                historicTaskInstancesBefore = historyService.createHistoricTaskInstanceQuery().orderByExecutionId().asc().list();
            }
        }

        //Migrate process
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(version2ProcessDef.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "userTask1Id").withNewAssignee("kermit"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        List<Execution> executionsAfter = runtimeService.createExecutionQuery().list();
        assertEquals(2, executionsAfter.size()); //includes root execution
        executionsAfter.stream()
            .map(e -> (ExecutionEntity) e)
            .forEach(e -> assertEquals(version2ProcessDef.getId(), e.getProcessDefinitionId()));

        List<Task> tasksAfter = taskService.createTaskQuery().list();
        assertEquals(1, tasksAfter.size());
        assertEquals(version2ProcessDef.getId(), tasksAfter.get(0).getProcessDefinitionId());
        assertEquals("userTask1Id", tasksAfter.get(0).getTaskDefinitionKey()); //AutoMapped by Id
        assertThat(tasksAfter).extracting(Task::getAssignee).containsOnly("kermit");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            List<HistoricActivityInstance> historicActivityInstancesAfter = historyService.createHistoricActivityInstanceQuery().orderByExecutionId().asc().list();
            assertEquals(historicActivityInstancesBefore.size(), historicActivityInstancesAfter.size());
            assertThat(historicActivityInstancesBefore)
                .usingElementComparatorIgnoringFields("revision", "processDefinitionId", "assignee", "originalPersistentState")
                .containsExactlyInAnyOrderElementsOf(historicActivityInstancesAfter);

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTaskInstancesAfter = historyService.createHistoricTaskInstanceQuery().orderByExecutionId().asc().list();

                assertEquals(historicTaskInstancesBefore.size(), historicTaskInstancesAfter.size());
                assertThat(historicTaskInstancesBefore)
                    .usingElementComparatorIgnoringFields("revision", "processDefinitionId", "assignee", "originalPersistentState", "lastUpdateTime")
                    .containsExactlyInAnyOrderElementsOf(historicTaskInstancesAfter);
            }
        }

        //The first process version only had one activity, there should be a second activity in the process now
        taskService.complete(tasksAfter.get(0).getId());
        tasksAfter = taskService.createTaskQuery().list();
        assertEquals(1, tasksAfter.size());
        assertEquals("userTask2Id", tasksAfter.get(0).getTaskDefinitionKey());
        taskService.complete(tasksAfter.get(0).getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testSimpleMigrationWithinSimpleSubProcess() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-tasks-inside-subprocess.bpmn20.xml");
        ProcessDefinition procDefTwoTasks = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/two-tasks-inside-subprocess.bpmn20.xml");

        //Start an instance of a process with one task inside a subProcess
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm and move inside the subProcess
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, executions.size()); //includes root execution
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());

        //Should be only one task
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("BeforeSubProcess");
        completeTask(task);

        List<Execution> executionsBeforeMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executionsBeforeMigration.size()); //Includes subProcess
        assertThat(executionsBeforeMigration).extracting(Execution::getActivityId).containsExactlyInAnyOrder("SimpleSubProcess", "InsideSimpleSubProcess1");
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("InsideSimpleSubProcess1");

        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procDefTwoTasks.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("InsideSimpleSubProcess1", "InsideSimpleSubProcess2"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm and move inside the subProcess
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executionsAfterMigration.size()); //includes subProcess
        assertThat(executionsAfterMigration).extracting(Execution::getActivityId).containsExactlyInAnyOrder("SimpleSubProcess", "InsideSimpleSubProcess2");
        assertThat(executionsAfterMigration).extracting("processDefinitionId").containsOnly(procDefTwoTasks.getId());

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
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-tasks-inside-subprocess.bpmn20.xml");
        ProcessDefinition procDefTwoTasks = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/two-tasks-inside-subprocess.bpmn20.xml");

        //Start and instance of the recent first version of the process for migration and one for reference
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefTwoTasks.getId());

        //Confirm and move inside the subProcess
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, executions.size()); //includes root execution
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefTwoTasks.getId());

        //Move to the second task inside the SubProcess
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("BeforeSubProcess");
        completeTask(task);
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("InsideSimpleSubProcess1");
        completeTask(task);
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("InsideSimpleSubProcess2");

        List<Execution> executionsBeforeMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executionsBeforeMigration.size()); //Includes subProcess
        assertThat(executionsBeforeMigration).extracting("activityId").containsExactlyInAnyOrder("SimpleSubProcess", "InsideSimpleSubProcess2");

        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procDefOneTask.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("InsideSimpleSubProcess2", "InsideSimpleSubProcess1"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm and move inside the subProcess
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executionsAfterMigration.size()); //includes subProcess
        assertThat(executionsAfterMigration).extracting(Execution::getActivityId).containsExactlyInAnyOrder("SimpleSubProcess", "InsideSimpleSubProcess1");
        assertThat(executionsAfterMigration).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());

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
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-tasks-inside-subprocess.bpmn20.xml");
        ProcessDefinition procDefTwoTasks = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/two-tasks-inside-subprocess.bpmn20.xml");

        //Start and instance of the recent first version of the process for migration and one for reference
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, executions.size()); //includes root execution
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());

        //Confirm migration point before the subProcess
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("BeforeSubProcess");

        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procDefTwoTasks.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("BeforeSubProcess", "InsideSimpleSubProcess2"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm and move inside the subProcess
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executionsAfterMigration.size()); //includes subProcess
        assertThat(executionsAfterMigration).extracting(Execution::getActivityId).containsExactlyInAnyOrder("SimpleSubProcess", "InsideSimpleSubProcess2");
        assertThat(executionsAfterMigration).extracting("processDefinitionId").containsOnly(procDefTwoTasks.getId());

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
            assertEquals(2, userTasks.size());
            assertThat(userTasks).extracting(HistoricActivityInstance::getActivityId).containsOnly("AfterSubProcess", "InsideSimpleSubProcess2");
            assertThat(userTasks).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procDefTwoTasks.getId());
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testSimpleMigrationOutOfEmbeddedSubProcess() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-tasks-inside-subprocess.bpmn20.xml");
        ProcessDefinition procDefTwoTasks = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/two-tasks-inside-subprocess.bpmn20.xml");

        //Start an instance of the definition with two task inside the subProcess
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefTwoTasks.getId());

        //Confirm and move inside the subProcess
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, executions.size()); //includes root execution
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefTwoTasks.getId());

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("BeforeSubProcess");
        completeTask(task);
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("InsideSimpleSubProcess1");
        completeTask(task);
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("InsideSimpleSubProcess2");

        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procDefOneTask.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("InsideSimpleSubProcess2", "BeforeSubProcess"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm and move inside the subProcess
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(1, executionsAfterMigration.size()); //No subProcess
        assertThat(executionsAfterMigration).extracting(Execution::getActivityId).containsExactlyInAnyOrder("BeforeSubProcess");
        assertThat(executionsAfterMigration).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());

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
    public void testMigrateActivityFromProcessRootIntoNestedEmbeddedSubProcess() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-tasks-inside-subprocess.bpmn20.xml");
        ProcessDefinition procDefNested = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-tasks-nested-subprocess.bpmn20.xml");

        //Start an instance of the definition with two task inside the subProcess
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, executions.size()); //includes root execution
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("BeforeSubProcess");

        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procDefNested.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("BeforeSubProcess", "InsideNestedSubProcess"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(3, executionsAfterMigration.size());
        assertThat(executionsAfterMigration).extracting(Execution::getActivityId).containsExactlyInAnyOrder("OuterSubProcess", "SimpleSubProcess", "InsideNestedSubProcess");
        assertThat(executionsAfterMigration).extracting("processDefinitionId").containsOnly(procDefNested.getId());

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
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-tasks-inside-subprocess-with-data-object.bpmn20.xml");
        ProcessDefinition procDefNested = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-tasks-nested-subprocess-with-data-object.bpmn20.xml");

        //Start an instance of the definition with two task inside the subProcess
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, executions.size()); //includes root execution
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("BeforeSubProcess");

        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procDefNested.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("BeforeSubProcess", "InsideNestedSubProcess"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(3, executionsAfterMigration.size());
        assertThat(executionsAfterMigration).extracting(Execution::getActivityId).containsExactlyInAnyOrder("OuterSubProcess", "SimpleSubProcess", "InsideNestedSubProcess");
        assertThat(executionsAfterMigration).extracting("processDefinitionId").containsOnly(procDefNested.getId());

        //Should contain the dataObject of the new embedded process definition
        Execution nestedSubProcess = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).activityId("SimpleSubProcess").singleResult();
        assertNotNull(runtimeService.getVariableLocal(nestedSubProcess.getId(), "dataScopeNested", String.class));
        DataObject nameDataObject = runtimeService.getDataObjectLocal(nestedSubProcess.getId(), "dataScopeNested");
        assertNotNull(nameDataObject);
        assertEquals("nestedSubProcess", nameDataObject.getValue());

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
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-tasks-inside-subprocess.bpmn20.xml");
        ProcessDefinition procDefNested = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-tasks-nested-subprocess.bpmn20.xml");

        //Start an instance of the definition with two task inside the subProcess
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm and move inside the subProcess
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, executions.size()); //includes root execution
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("BeforeSubProcess");
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("InsideSimpleSubProcess1");

        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procDefNested.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("InsideSimpleSubProcess1", "InsideNestedSubProcess"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm - we move from a subProcess to a nestedSubProcess with the same name (SimpleSubProcess), the original is not created, but cancelled and created from the new model
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(3, executionsAfterMigration.size()); //2 subProcesses and 1 userTask
        assertThat(executionsAfterMigration).extracting(Execution::getActivityId).containsExactlyInAnyOrder("OuterSubProcess", "SimpleSubProcess", "InsideNestedSubProcess");
        assertThat(executionsAfterMigration).extracting("processDefinitionId").containsOnly(procDefNested.getId());

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
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-tasks-inside-subprocess-with-data-object.bpmn20.xml");
        ProcessDefinition procDefNested = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-tasks-nested-subprocess-with-data-object.bpmn20.xml");

        //Start an instance of the definition with two task inside the subProcess
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm and move inside the subProcess
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, executions.size()); //includes root execution
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("BeforeSubProcess");
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("InsideSimpleSubProcess1");

        Execution subProcessExecution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).activityId("SimpleSubProcess").singleResult();
        assertNotNull(runtimeService.getVariableLocal(subProcessExecution.getId(), "dataScope", String.class));
        DataObject nameDataObject = runtimeService.getDataObjectLocal(subProcessExecution.getId(), "dataScope");
        assertNotNull(nameDataObject);
        assertEquals("subProcess", nameDataObject.getValue());

        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procDefNested.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("InsideSimpleSubProcess1", "InsideNestedSubProcess"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm - we move from a subProcess to a nestedSubProcess with the same name (SimpleSubProcess), the original is not created, but cancelled and created from the new model
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(3, executionsAfterMigration.size()); //2 subProcesses and 1 userTask
        assertThat(executionsAfterMigration).extracting(Execution::getActivityId).containsExactlyInAnyOrder("OuterSubProcess", "SimpleSubProcess", "InsideNestedSubProcess");
        assertThat(executionsAfterMigration).extracting("processDefinitionId").containsOnly(procDefNested.getId());

        //Confirm we have the dataObject of the subProcess in the new definition (its a new SubProcess execution nonetheless)
        subProcessExecution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).activityId("SimpleSubProcess").singleResult();
        assertThat(runtimeService.getVariablesLocal(subProcessExecution.getId())).containsOnlyKeys("dataScopeNested");
        assertThat(runtimeService.getDataObjectsLocal(subProcessExecution.getId())).containsOnlyKeys("dataScopeNested");
        nameDataObject = runtimeService.getDataObjectLocal(subProcessExecution.getId(), "dataScopeNested");
        assertNotNull(nameDataObject);
        assertEquals("nestedSubProcess", nameDataObject.getValue());

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
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procDefOWithTimer = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/twoTasksProcessWithTimer.bpmn20.xml");

        //Start the processInstance without timer
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(1, executions.size());
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNull(timerJob);

        changeStateEventListener.clear();

        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procDefOWithTimer.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "firstTask"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executionsAfterMigration.size());
        assertThat(executionsAfterMigration).extracting(Execution::getActivityId).containsOnly("firstTask", "boundaryTimerEvent");
        assertThat(executionsAfterMigration).extracting("processDefinitionId").containsOnly(procDefOWithTimer.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("firstTask");
        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);
        Execution execution = runtimeService.createExecutionQuery().parentId(task.getExecutionId()).singleResult();
        Job job = managementService.createTimerJobQuery().executionId(execution.getId()).singleResult();
        assertThat(job).isEqualToIgnoringGivenFields(timerJob, "originalPersistentState", "customValuesByteArrayRef", "exceptionByteArrayRef");

        // Verify events
        assertTrue(changeStateEventListener.hasEvents());
        assertThat(changeStateEventListener.getEvents()).extracting(FlowableEvent::getType).containsExactly(FlowableEngineEventType.TIMER_SCHEDULED);
        Optional<FlowableEvent> timerEvent = changeStateEventListener.getEvents().stream().filter(event -> event.getType().equals(FlowableEngineEventType.TIMER_SCHEDULED)).findFirst();
        assertTrue(timerEvent.isPresent());
        FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent) timerEvent.get();
        Job timer = (Job) entityEvent.getEntity();
        assertEquals("boundaryTimerEvent", getJobActivityId(timer));

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
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("firstTask", "secondTask");
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procDefOWithTimer.getId());

            checkTaskInstance(procDefOWithTimer, processInstance, "firstTask", "secondTask");
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testMigrateActivityWithTimerToActivityWithoutTimerInNewDefinition() {
        ProcessDefinition procDefOWithTimer = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/twoTasksProcessWithTimer.bpmn20.xml");
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");

        //Start the processInstance with timer
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOWithTimer.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("firstTask", "boundaryTimerEvent");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOWithTimer.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("firstTask");

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);
        Execution execution = runtimeService.createExecutionQuery().parentId(task.getExecutionId()).singleResult();
        Job job = managementService.createTimerJobQuery().executionId(execution.getId()).singleResult();
        assertThat(job).isEqualToIgnoringGivenFields(timerJob, "originalPersistentState", "customValuesByteArrayRef", "exceptionByteArrayRef");

        changeStateEventListener.clear();

        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procDefOneTask.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("firstTask", "userTask1Id"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(1, executionsAfterMigration.size());
        assertThat(executionsAfterMigration).extracting(Execution::getActivityId).containsOnly("userTask1Id");
        assertThat(executionsAfterMigration).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());

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
        assertTrue(changeStateEventListener.hasEvents());
        assertThat(changeStateEventListener.getEvents()).extracting(FlowableEvent::getType).containsExactly(FlowableEngineEventType.JOB_CANCELED);
        Optional<FlowableEvent> jobCancelEvent = changeStateEventListener.getEvents().stream().filter(event -> event.getType().equals(FlowableEngineEventType.JOB_CANCELED)).findFirst();
        assertTrue(jobCancelEvent.isPresent());
        FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent) jobCancelEvent.get();
        Job timer = (Job) entityEvent.getEntity();
        assertEquals("boundaryTimerEvent", getJobActivityId(timer));

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
        assertEquals(2, executions.size());
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("firstTask", "boundaryTimerEvent");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTimer.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("firstTask");
        Job timerJob1 = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob1);

        changeStateEventListener.clear();

        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procDefTwoTimers.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("firstTask", "secondTask"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executionsAfterMigration.size());
        assertThat(executionsAfterMigration).extracting(Execution::getActivityId).containsExactlyInAnyOrder("secondTask", "secondTimerEvent");
        assertThat(executionsAfterMigration).extracting("processDefinitionId").containsOnly(procDefTwoTimers.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("secondTask");
        Job timerJob2 = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob2);
        Execution execution = runtimeService.createExecutionQuery().parentId(task.getExecutionId()).singleResult();
        Job job = managementService.createTimerJobQuery().executionId(execution.getId()).singleResult();
        assertThat(job).isEqualToIgnoringGivenFields(timerJob2, "originalPersistentState", "customValuesByteArrayRef", "exceptionByteArrayRef");
        assertThat(timerJob1).isNotEqualTo(timerJob2).extracting(Job::getExecutionId);

        // Verify events
        assertTrue(changeStateEventListener.hasEvents());
        assertThat(changeStateEventListener.getEvents()).extracting(FlowableEvent::getType).containsExactly(FlowableEngineEventType.JOB_CANCELED, FlowableEngineEventType.TIMER_SCHEDULED);

        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent) iterator.next();
        Job timer = (Job) entityEvent.getEntity();
        assertEquals("boundaryTimerEvent", getJobActivityId(timer));
        entityEvent = (FlowableEngineEntityEvent) iterator.next();
        timer = (Job) entityEvent.getEntity();
        assertEquals("secondTimerEvent", getJobActivityId(timer));

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
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");

        //Start the processInstance without timer
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefSubProcWithTimer.getId());

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("taskBefore");
        taskService.complete(task.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(3, executions.size());
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("subProcess", "subTask", "boundaryTimerEvent");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefSubProcWithTimer.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("subTask");
        Job timerJob1 = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob1);
        assertThat(timerJob1).extracting(Job::getProcessDefinitionId).isEqualTo(procDefSubProcWithTimer.getId());
        assertThat(timerJob1).extracting(Job::getJobHandlerConfiguration).toString().contains("boundaryTimerEvent");

        changeStateEventListener.clear();
        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procDefOneTask.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("subTask", "userTask1Id"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(1, executionsAfterMigration.size());
        assertThat(executionsAfterMigration).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executionsAfterMigration).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");

        Job timerJob2 = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNull(timerJob2);

        // Verify events
        assertTrue(changeStateEventListener.hasEvents());
        assertThat(changeStateEventListener.getEvents()).extracting(FlowableEvent::getType).containsExactly(FlowableEngineEventType.JOB_CANCELED, FlowableEngineEventType.ACTIVITY_CANCELLED);
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent) iterator.next();
        Job timer = (Job) entityEvent.getEntity();
        assertEquals("boundaryTimerEvent", getJobActivityId(timer));
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
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procDefSubProcWithTimer = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/oneTaskSubProcessWithTimer.bpmn20.xml");

        //Start the processInstance without timer
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());

        changeStateEventListener.clear();
        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procDefSubProcWithTimer.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "subTask"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(3, executionsAfterMigration.size());
        assertThat(executionsAfterMigration).extracting(Execution::getActivityId).containsExactlyInAnyOrder("subProcess", "subTask", "boundaryTimerEvent");
        assertThat(executionsAfterMigration).extracting("processDefinitionId").containsOnly(procDefSubProcWithTimer.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("subTask");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefSubProcWithTimer.getId());

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);
        assertThat(timerJob).extracting(Job::getProcessDefinitionId).isEqualTo(procDefSubProcWithTimer.getId());
        assertThat(timerJob).extracting(Job::getJobHandlerConfiguration).toString().contains("boundaryTimerEvent");

        // Verify events
        assertTrue(changeStateEventListener.hasEvents());
        assertThat(changeStateEventListener.getEvents()).extracting(FlowableEvent::getType).containsExactly(FlowableEngineEventType.ACTIVITY_STARTED, FlowableEngineEventType.TIMER_SCHEDULED);

        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        FlowableActivityEvent activityEvent = (FlowableActivityEvent) iterator.next();
        assertThat(activityEvent).extracting(FlowableActivityEvent::getActivityId).isEqualTo("subProcess");

        FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent) iterator.next();
        Job timer = (Job) entityEvent.getEntity();
        assertEquals("boundaryTimerEvent", getJobActivityId(timer));

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
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procDefSubProcWithTimer = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/oneTaskSubProcessWithTimer.bpmn20.xml");

        //Start the processInstance without timer
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());

        changeStateEventListener.clear();
        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procDefSubProcWithTimer.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "subTask"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(3, executionsAfterMigration.size());
        assertThat(executionsAfterMigration).extracting(Execution::getActivityId).containsExactlyInAnyOrder("subProcess", "subTask", "boundaryTimerEvent");
        assertThat(executionsAfterMigration).extracting("processDefinitionId").containsOnly(procDefSubProcWithTimer.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("subTask");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefSubProcWithTimer.getId());

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);
        assertThat(timerJob).extracting(Job::getProcessDefinitionId).isEqualTo(procDefSubProcWithTimer.getId());
        assertThat(timerJob).extracting(Job::getJobHandlerConfiguration).toString().contains("boundaryTimerEvent");

        // Verify events
        assertTrue(changeStateEventListener.hasEvents());
        assertThat(changeStateEventListener.getEvents()).extracting(FlowableEvent::getType).containsExactly(FlowableEngineEventType.ACTIVITY_STARTED, FlowableEngineEventType.TIMER_SCHEDULED);

        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        FlowableActivityEvent activityEvent = (FlowableActivityEvent) iterator.next();
        assertThat(activityEvent).extracting(FlowableActivityEvent::getActivityId).isEqualTo("subProcess");

        FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent) iterator.next();
        Job timer = (Job) entityEvent.getEntity();
        assertEquals("boundaryTimerEvent", getJobActivityId(timer));

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
        ProcessDefinition procVersion1 = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-with-timer-inside-embedded-subprocess.bpmn20.xml");
        ProcessDefinition procVersion2 = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/oneTaskWithTimerInSubProcess.bpmn20.xml");

        //Start the processInstance without timer
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procVersion1.getId());
        completeTask(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactlyInAnyOrder("subProcess", "subTask", "boundaryTimerEvent");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procVersion1.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("subTask");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procVersion1.getId());
        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);
        assertThat(timerJob).extracting(Job::getProcessDefinitionId).isEqualTo(procVersion1.getId());
        assertThat(timerJob).extracting(Job::getJobHandlerConfiguration).toString().contains("boundaryTimerEvent");

        changeStateEventListener.clear();
        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procVersion2.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("subTask", "subTask2"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executionsAfterMigration.size());
        assertThat(executionsAfterMigration).extracting(Execution::getActivityId).containsExactlyInAnyOrder("subProcess", "subTask2");
        assertThat(executionsAfterMigration).extracting("processDefinitionId").containsOnly(procVersion2.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("subTask2");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procVersion2.getId());

        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNull(timerJob);

        // Verify events
        assertTrue(changeStateEventListener.hasEvents());
        assertThat(changeStateEventListener.getEvents()).extracting(FlowableEvent::getType).containsExactly(FlowableEngineEventType.JOB_CANCELED, FlowableEngineEventType.ACTIVITY_CANCELLED, FlowableEngineEventType.ACTIVITY_STARTED);

        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent) iterator.next();
        Job timer = (Job) entityEvent.getEntity();
        assertEquals("boundaryTimerEvent", getJobActivityId(timer));

        FlowableActivityEvent activityEvent = (FlowableActivityEvent) iterator.next();
        assertThat(activityEvent).extracting(FlowableActivityEvent::getActivityId).isEqualTo("subProcess");

        activityEvent = (FlowableActivityEvent) iterator.next();
        assertThat(activityEvent).extracting(FlowableActivityEvent::getActivityId).isEqualTo("subProcess");

        assertFalse(iterator.hasNext());

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
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procDefTimerTaskInSubProcess = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/oneTaskWithTimerInSubProcess.bpmn20.xml");

        //Start the processInstance without timer
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());

        changeStateEventListener.clear();
        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procDefTimerTaskInSubProcess.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "subTask"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(3, executionsAfterMigration.size());
        assertThat(executionsAfterMigration).extracting(Execution::getActivityId).containsExactlyInAnyOrder("subProcess", "subTask", "boundaryTimerEvent");
        assertThat(executionsAfterMigration).extracting("processDefinitionId").containsOnly(procDefTimerTaskInSubProcess.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("subTask");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefTimerTaskInSubProcess.getId());

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);
        assertThat(timerJob).extracting(Job::getProcessDefinitionId).isEqualTo(procDefTimerTaskInSubProcess.getId());
        assertThat(timerJob).extracting(Job::getJobHandlerConfiguration).toString().contains("boundaryTimerEvent");
        //Job is attached to the activity
        Execution timerExecution = runtimeService.createExecutionQuery().parentId(task.getExecutionId()).singleResult();
        Job timerFromTask = managementService.createTimerJobQuery().executionId(timerExecution.getId()).singleResult();
        assertThat(timerJob).isEqualToIgnoringGivenFields(timerFromTask, "originalPersistentState", "customValuesByteArrayRef", "exceptionByteArrayRef");

        // Verify events
        assertTrue(changeStateEventListener.hasEvents());
        assertThat(changeStateEventListener.getEvents()).extracting(FlowableEvent::getType).containsExactly(FlowableEngineEventType.ACTIVITY_STARTED, FlowableEngineEventType.TIMER_SCHEDULED);

        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        FlowableActivityEvent activityEvent = (FlowableActivityEvent) iterator.next();
        assertThat(activityEvent).extracting(FlowableActivityEvent::getActivityId).isEqualTo("subProcess");

        FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent) iterator.next();
        Job timer = (Job) entityEvent.getEntity();
        assertEquals("boundaryTimerEvent", getJobActivityId(timer));

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
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procDefTimerTaskInSubProcess = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/oneTaskWithTimerInSubProcess.bpmn20.xml");

        //Start the processInstance without timer
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());

        changeStateEventListener.clear();
        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procDefTimerTaskInSubProcess.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "subTask"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(3, executionsAfterMigration.size());
        assertThat(executionsAfterMigration).extracting(Execution::getActivityId).containsExactlyInAnyOrder("subProcess", "subTask", "boundaryTimerEvent");
        assertThat(executionsAfterMigration).extracting("processDefinitionId").containsOnly(procDefTimerTaskInSubProcess.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("subTask");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefTimerTaskInSubProcess.getId());

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);
        assertThat(timerJob).extracting(Job::getProcessDefinitionId).isEqualTo(procDefTimerTaskInSubProcess.getId());
        assertThat(timerJob).extracting(Job::getJobHandlerConfiguration).toString().contains("boundaryTimerEvent");
        //Job is attached to the activity
        Execution timerExecution = runtimeService.createExecutionQuery().parentId(task.getExecutionId()).singleResult();
        Job timerFromTask = managementService.createTimerJobQuery().executionId(timerExecution.getId()).singleResult();
        assertThat(timerJob).isEqualToIgnoringGivenFields(timerFromTask, "originalPersistentState", "customValuesByteArrayRef", "exceptionByteArrayRef");

        // Verify events
        assertTrue(changeStateEventListener.hasEvents());
        assertThat(changeStateEventListener.getEvents()).extracting(FlowableEvent::getType).containsExactly(FlowableEngineEventType.ACTIVITY_STARTED, FlowableEngineEventType.TIMER_SCHEDULED);

        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        FlowableActivityEvent activityEvent = (FlowableActivityEvent) iterator.next();
        assertThat(activityEvent).extracting(FlowableActivityEvent::getActivityId).isEqualTo("subProcess");

        FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent) iterator.next();
        Job timer = (Job) entityEvent.getEntity();
        assertEquals("boundaryTimerEvent", getJobActivityId(timer));

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

    //-- Intermediate Signal Catch Events
    @Test
    public void testMigrateSimpleActivityToIntermediateSignalCatchingEventInNewDefinition() {
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procWithSignal = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.simpleIntermediateSignalCatchEvent.bpmn20.xml");

        //Start the processInstance without timer
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertTrue(eventSubscriptions.isEmpty());

        changeStateEventListener.clear();
        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procWithSignal.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "intermediateCatchEvent"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executionsAfterMigration).extracting(Execution::getActivityId).containsExactly("intermediateCatchEvent");
        assertThat(executionsAfterMigration).extracting("processDefinitionId").containsOnly(procWithSignal.getId());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).isEmpty();

        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).extracting(EventSubscription::getActivityId).containsExactly("intermediateCatchEvent");
        assertThat(eventSubscriptions).extracting(EventSubscription::getEventType).containsExactly("signal");

        // Verify events
        assertTrue(changeStateEventListener.hasEvents());
        assertThat(changeStateEventListener.getEvents()).extracting(FlowableEvent::getType).containsExactly(FlowableEngineEventType.ACTIVITY_CANCELLED, FlowableEngineEventType.ACTIVITY_SIGNAL_WAITING);

        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        FlowableActivityEvent activityEvent = (FlowableActivityEvent) iterator.next();
        assertThat(activityEvent).extracting(FlowableActivityEvent::getType).isEqualTo(FlowableEngineEventType.ACTIVITY_CANCELLED);
        assertThat(activityEvent).extracting(FlowableActivityEvent::getActivityId).isEqualTo("userTask1Id");

        FlowableSignalEvent signalEvent = (FlowableSignalEvent) iterator.next();
        assertThat(signalEvent).extracting(FlowableSignalEvent::getType).isEqualTo(FlowableEngineEventType.ACTIVITY_SIGNAL_WAITING);
        assertThat(signalEvent).extracting(FlowableSignalEvent::getActivityId).isEqualTo("intermediateCatchEvent");
        assertThat(signalEvent).extracting(FlowableSignalEvent::getSignalName).isEqualTo("mySignal");

        //Trigger the event
        runtimeService.signalEventReceived("someSignal");

        executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executionsAfterMigration).extracting(Execution::getActivityId).containsExactly("afterCatchEvent");
        assertThat(executionsAfterMigration).extracting("processDefinitionId").containsOnly(procWithSignal.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("afterCatchEvent");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procWithSignal.getId());
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertTrue(eventSubscriptions.isEmpty());

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
        ProcessDefinition procWithSignal = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.simpleIntermediateSignalCatchEvent.bpmn20.xml");
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");

        //Start the processInstance without timer
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procWithSignal.getId());

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("beforeCatchEvent");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procWithSignal.getId());
        completeTask(task);

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("intermediateCatchEvent");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procWithSignal.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).isEmpty();
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).extracting(EventSubscription::getActivityId).containsExactly("intermediateCatchEvent");
        assertThat(eventSubscriptions).extracting(EventSubscription::getEventType).containsExactly("signal");

        changeStateEventListener.clear();
        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procDefOneTask.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("intermediateCatchEvent", "userTask1Id"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executionsAfterMigration).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executionsAfterMigration).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());

        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        // Verify events
        assertTrue(changeStateEventListener.hasEvents());
        assertThat(changeStateEventListener.getEvents()).extracting(FlowableEvent::getType).containsExactly(FlowableEngineEventType.ACTIVITY_STARTED);

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
        ProcessDefinition procWithSignalVer1 = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.simpleIntermediateSignalCatchEvent.bpmn20.xml");
        ProcessDefinition procWithSignalVer2 = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/simple-intermediate-signal-catch-event.bpmn20.xml");

        //Start the processInstance without timer
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procWithSignalVer1.getId());

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("beforeCatchEvent");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procWithSignalVer1.getId());
        completeTask(task);

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("intermediateCatchEvent");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procWithSignalVer1.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).isEmpty();
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).extracting(EventSubscription::getActivityId).containsExactly("intermediateCatchEvent");
        assertThat(eventSubscriptions).extracting(EventSubscription::getEventType).containsExactly("signal");

        changeStateEventListener.clear();
        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procWithSignalVer2.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("intermediateCatchEvent", "newIntermediateCatchEvent"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executionsAfterMigration).extracting(Execution::getActivityId).containsExactly("newIntermediateCatchEvent");
        assertThat(executionsAfterMigration).extracting("processDefinitionId").containsOnly(procWithSignalVer2.getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).isEmpty();

        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).extracting(EventSubscription::getActivityId).containsExactly("newIntermediateCatchEvent");
        assertThat(eventSubscriptions).extracting(EventSubscription::getEventType).containsExactly("signal");

        // Verify events
        assertTrue(changeStateEventListener.hasEvents());
        assertThat(changeStateEventListener.getEvents()).extracting(FlowableEvent::getType).containsExactly(FlowableEngineEventType.ACTIVITY_SIGNAL_WAITING);

        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        FlowableSignalEvent signalEvent = (FlowableSignalEvent) iterator.next();
        assertThat(signalEvent).extracting(FlowableSignalEvent::getType).isEqualTo(FlowableEngineEventType.ACTIVITY_SIGNAL_WAITING);
        assertThat(signalEvent).extracting(FlowableSignalEvent::getActivityId).isEqualTo("newIntermediateCatchEvent");
        assertThat(signalEvent).extracting(FlowableSignalEvent::getSignalName).isEqualTo("myNewSignal");

        //Trigger the event
        runtimeService.signalEventReceived("someNewSignal");

        executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executionsAfterMigration).extracting(Execution::getActivityId).containsExactly("afterNewCatchEvent");
        assertThat(executionsAfterMigration).extracting("processDefinitionId").containsOnly(procWithSignalVer2.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("afterNewCatchEvent");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procWithSignalVer2.getId());
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertTrue(eventSubscriptions.isEmpty());

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
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procWithSignal = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.simpleIntermediateMessageCatchEvent.bpmn20.xml");

        //Start the processInstance without timer
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertTrue(eventSubscriptions.isEmpty());

        changeStateEventListener.clear();
        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procWithSignal.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "intermediateCatchEvent"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executionsAfterMigration).extracting(Execution::getActivityId).containsExactly("intermediateCatchEvent");
        assertThat(executionsAfterMigration).extracting("processDefinitionId").containsOnly(procWithSignal.getId());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).isEmpty();

        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).extracting(EventSubscription::getActivityId).containsExactly("intermediateCatchEvent");
        assertThat(eventSubscriptions).extracting(EventSubscription::getEventType).containsExactly("message");

        // Verify events
        assertTrue(changeStateEventListener.hasEvents());
        assertThat(changeStateEventListener.getEvents()).extracting(FlowableEvent::getType).containsExactly(FlowableEngineEventType.ACTIVITY_CANCELLED, FlowableEngineEventType.ACTIVITY_MESSAGE_WAITING);

        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        FlowableActivityEvent activityEvent = (FlowableActivityEvent) iterator.next();
        assertThat(activityEvent).extracting(FlowableActivityEvent::getType).isEqualTo(FlowableEngineEventType.ACTIVITY_CANCELLED);
        assertThat(activityEvent).extracting(FlowableActivityEvent::getActivityId).isEqualTo("userTask1Id");

        FlowableMessageEvent signalEvent = (FlowableMessageEvent) iterator.next();
        assertThat(signalEvent).extracting(FlowableMessageEvent::getType).isEqualTo(FlowableEngineEventType.ACTIVITY_MESSAGE_WAITING);
        assertThat(signalEvent).extracting(FlowableMessageEvent::getActivityId).isEqualTo("intermediateCatchEvent");
        assertThat(signalEvent).extracting(FlowableMessageEvent::getMessageName).isEqualTo("someMessage");

        //Trigger the event
        Execution messageCatchExecution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).activityId("intermediateCatchEvent").singleResult();
        runtimeService.messageEventReceived("someMessage", messageCatchExecution.getId());

        executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executionsAfterMigration).extracting(Execution::getActivityId).containsExactly("afterCatchEvent");
        assertThat(executionsAfterMigration).extracting("processDefinitionId").containsOnly(procWithSignal.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("afterCatchEvent");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procWithSignal.getId());
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertTrue(eventSubscriptions.isEmpty());

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
        ProcessDefinition procWithSignal = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.simpleIntermediateMessageCatchEvent.bpmn20.xml");
        ProcessDefinition procDefOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");

        //Start the processInstance without timer
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procWithSignal.getId());

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("beforeCatchEvent");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procWithSignal.getId());
        completeTask(task);

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("intermediateCatchEvent");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procWithSignal.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).isEmpty();
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).extracting(EventSubscription::getActivityId).containsExactly("intermediateCatchEvent");
        assertThat(eventSubscriptions).extracting(EventSubscription::getEventType).containsExactly("message");

        changeStateEventListener.clear();
        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procDefOneTask.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("intermediateCatchEvent", "userTask1Id"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executionsAfterMigration).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executionsAfterMigration).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());

        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).isEmpty();

        // Verify events
        assertTrue(changeStateEventListener.hasEvents());
        assertThat(changeStateEventListener.getEvents()).extracting(FlowableEvent::getType).containsExactly(FlowableEngineEventType.ACTIVITY_MESSAGE_CANCELLED, FlowableEngineEventType.ACTIVITY_STARTED);

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
        ProcessDefinition procWithSignalVer1 = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/changestate/RuntimeServiceChangeStateTest.simpleIntermediateMessageCatchEvent.bpmn20.xml");
        ProcessDefinition procWithSignalVer2 = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/simple-intermediate-message-catch-event.bpmn20.xml");

        //Start the processInstance without timer
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procWithSignalVer1.getId());

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("beforeCatchEvent");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procWithSignalVer1.getId());
        completeTask(task);

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("intermediateCatchEvent");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procWithSignalVer1.getId());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).isEmpty();
        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).extracting(EventSubscription::getActivityId).containsExactly("intermediateCatchEvent");
        assertThat(eventSubscriptions).extracting(EventSubscription::getEventType).containsExactly("message");

        changeStateEventListener.clear();
        //Migrate to the other processDefinition
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procWithSignalVer2.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("intermediateCatchEvent", "intermediateNewCatchEvent"));

        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        //Confirm
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executionsAfterMigration).extracting(Execution::getActivityId).containsExactly("intermediateNewCatchEvent");
        assertThat(executionsAfterMigration).extracting("processDefinitionId").containsOnly(procWithSignalVer2.getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).isEmpty();

        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(eventSubscriptions).extracting(EventSubscription::getActivityId).containsExactly("intermediateNewCatchEvent");
        assertThat(eventSubscriptions).extracting(EventSubscription::getEventType).containsExactly("message");

        // Verify events
        assertTrue(changeStateEventListener.hasEvents());
        assertThat(changeStateEventListener.getEvents()).extracting(FlowableEvent::getType).containsExactly(FlowableEngineEventType.ACTIVITY_MESSAGE_CANCELLED, FlowableEngineEventType.ACTIVITY_MESSAGE_WAITING);

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
        Execution messageCatchExecution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).activityId("intermediateNewCatchEvent").singleResult();
        runtimeService.messageEventReceived("someNewMessage", messageCatchExecution.getId());

        executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executionsAfterMigration).extracting(Execution::getActivityId).containsExactly("afterNewCatchEvent");
        assertThat(executionsAfterMigration).extracting("processDefinitionId").containsOnly(procWithSignalVer2.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("afterNewCatchEvent");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procWithSignalVer2.getId());
        eventSubscriptions = runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).list();
        assertTrue(eventSubscriptions.isEmpty());

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
}
