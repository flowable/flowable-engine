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
import org.flowable.engine.impl.migration.ProcessInstanceMigrationValidationResult;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
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

/**
 * @author Dennis Federico
 */
public class ProcessInstanceMigrationTest extends PluggableFlowableTestCase {

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
        ProcessInstance processInstanceToMigrate = runtimeService.startProcessInstanceByKey("MP");

        //Deploy second version of the process
        ProcessDefinition version2ProcessDef = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/two-tasks-simple-process.bpmn20.xml");

        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
            .processDefinitionKey("MP")
            .list();

        assertEquals(2, processDefinitions.size());
        processDefinitions.sort(Comparator.comparingInt(ProcessDefinition::getVersion));
        assertEquals(processDefinitions.get(0).getId(), version1ProcessDef.getId());
        assertEquals(processDefinitions.get(1).getId(), version2ProcessDef.getId());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstanceToMigrate.getId()).list();
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
            .validateMigration(processInstanceToMigrate.getId());
        
        assertEquals(false, validationResult.hasErrors());
        assertEquals(true, validationResult.isMigrationValid());
        assertEquals(0, validationResult.getValidationMessages().size());

        //Migrate process
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(version2ProcessDef.getId())
            .migrate(processInstanceToMigrate.getId());

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
        ProcessInstance processInstanceToMigrate = runtimeService.startProcessInstanceByKey("MP");

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

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstanceToMigrate.getId()).list();
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
            .addActivityMigrationMapping("userTask1Id", "userTask1Id")
            .validateMigration(processInstanceToMigrate.getId());
        
        assertEquals(false, validationResult.hasErrors());
        assertEquals(true, validationResult.isMigrationValid());
        assertEquals(0, validationResult.getValidationMessages().size());

        //Migrate process - moving the current execution explicitly
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(version2ProcessDef.getId())
            .addActivityMigrationMapping("userTask1Id", "userTask1Id")
            .migrate(processInstanceToMigrate.getId());

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
        assertProcessEnded(processInstanceToMigrate.getId());
    }

    @Test
    public void testSimpleMigrationWithExplicitActivityMapping2() {
        //Deploy first version of the process
        Deployment oneActivityProcessDeployment = repositoryService.createDeployment()
            .name("My Process Deployment")
            .addClasspathResource("org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml")
            .deploy();

        //Start and instance of the recent first version of the process for migration and one for reference
        ProcessInstance processInstanceToMigrate = runtimeService.startProcessInstanceByKey("MP");

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

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstanceToMigrate.getId()).list();
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
            .addActivityMigrationMapping("userTask1Id", "userTask3Id")
            .validateMigration(processInstanceToMigrate.getId());
        
        assertEquals(true, validationResult.hasErrors());
        assertEquals(false, validationResult.isMigrationValid());
        assertEquals(1, validationResult.getValidationMessages().size());
        
        validationResult = runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(version2ProcessDef.getId())
            .addActivityMigrationMapping("userTask1Id", "userTask2Id")
            .validateMigration(processInstanceToMigrate.getId());
        
        assertEquals(false, validationResult.hasErrors());
        assertEquals(true, validationResult.isMigrationValid());
        assertEquals(0, validationResult.getValidationMessages().size());

        //Migrate process - moving the current execution explicitly
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(version2ProcessDef.getId())
            .addActivityMigrationMapping("userTask1Id", "userTask2Id")
            .migrate(processInstanceToMigrate.getId());

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
        assertProcessEnded(processInstanceToMigrate.getId());

    }

    @Test
    public void testSimpleMigrationWithExplicitActivityMapping3() {
        //Deploy first version of the process
        Deployment twoActivitiesProcessDeployment = repositoryService.createDeployment()
            .name("My Process Deployment")
            .addClasspathResource("org/flowable/engine/test/api/runtime/migration/two-tasks-simple-process.bpmn20.xml")
            .deploy();

        //Start and instance of the recent first version of the process for migration and one for reference
        ProcessInstance processInstanceToMigrate = runtimeService.startProcessInstanceByKey("MP");

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

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstanceToMigrate.getId()).list();
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
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(version2ProcessDef.getId())
            .addActivityMigrationMapping("userTask2Id", "userTask1Id")
            .migrate(processInstanceToMigrate.getId());

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
        assertProcessEnded(processInstanceToMigrate.getId());
    }

    @Test
    public void testSimpleUserTaskDirectMigration() {

        //Almost all tests use UserTask, thus are direct migrations, but this one checks explicitly for changes in History
        //Deploy first version of the process
        ProcessDefinition version1ProcessDef = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");

        //Start and instance of the recent first version of the process for migration and one for reference
        ProcessInstance processInstanceToMigrate = runtimeService.startProcessInstanceByKey("MP");

        //Deploy second version of the process
        ProcessDefinition version2ProcessDef = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/two-tasks-simple-process.bpmn20.xml");

        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
            .processDefinitionKey("MP")
            .list();

        assertEquals(2, processDefinitions.size());
        processDefinitions.sort(Comparator.comparingInt(ProcessDefinition::getVersion));
        assertEquals(processDefinitions.get(0).getId(), version1ProcessDef.getId());
        assertEquals(processDefinitions.get(1).getId(), version2ProcessDef.getId());

        List<Execution> executionsBefore = runtimeService.createExecutionQuery().processInstanceId(processInstanceToMigrate.getId()).list();
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
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(version2ProcessDef.getId())
            .migrate(processInstanceToMigrate.getId());

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
        assertProcessEnded(processInstanceToMigrate.getId());
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
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procDefTwoTasks.getId())
            .addActivityMigrationMapping("InsideSimpleSubProcess1", "InsideSimpleSubProcess2")
            .migrate(processInstance.getId());

        //Confirm and move inside the subProcess
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executionsAfterMigration.size()); //includes subProcess
        assertThat(executionsAfterMigration).extracting(Execution::getActivityId).containsExactlyInAnyOrder("SimpleSubProcess", "InsideSimpleSubProcess2");
        assertThat(executionsAfterMigration).extracting("processDefinitionId").containsOnly(procDefTwoTasks.getId());

        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History - should contain two SubProcesses and 3 userTask Activities (before and after subprocess and the migrated user taks within the subprocess)
            List<HistoricActivityInstance> subProcesses = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("subProcess")
                .list();
            assertEquals(2, subProcesses.size());
            assertThat(subProcesses).extracting(HistoricActivityInstance::getActivityId).containsOnly("SimpleSubProcess");
            assertThat(subProcesses).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procDefTwoTasks.getId());
    
            List<HistoricActivityInstance> userTasks = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("userTask")
                .list();
    
            assertThat(userTasks).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("BeforeSubProcess", "AfterSubProcess", "InsideSimpleSubProcess2");
            assertThat(userTasks).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procDefTwoTasks.getId());
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
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procDefOneTask.getId())
            .addActivityMigrationMapping("InsideSimpleSubProcess2", "InsideSimpleSubProcess1")
            .migrate(processInstance.getId());

        //Confirm and move inside the subProcess
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executionsAfterMigration.size()); //includes subProcess
        assertThat(executionsAfterMigration).extracting(Execution::getActivityId).containsExactlyInAnyOrder("SimpleSubProcess", "InsideSimpleSubProcess1");
        assertThat(executionsAfterMigration).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());

        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History - should contain two SubProcesses and 4 userTask Activities:
            //Two user tasks defined before and after the SubProcess
            //One task inside the subProcess that was completed before the migration (but the process definition reference should update anyway)
            //One tasks inside the subProcess that was completed after the migration
            List<HistoricActivityInstance> subProcesses = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("subProcess")
                .list();
            assertEquals(2, subProcesses.size());
            assertThat(subProcesses).extracting(HistoricActivityInstance::getActivityId).containsOnly("SimpleSubProcess");
            assertThat(subProcesses).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procDefOneTask.getId());
    
            List<HistoricActivityInstance> userTasks = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("userTask")
                .list();
            assertEquals(4, userTasks.size());
            //InsideSimpleSubProcess2 was migrated from the first definition but completed as InsideSimpleSubProcess1
            assertThat(userTasks).extracting(HistoricActivityInstance::getActivityId).containsOnly("BeforeSubProcess", "AfterSubProcess", "InsideSimpleSubProcess1");
            assertThat(userTasks).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procDefOneTask.getId());
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
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procDefTwoTasks.getId())
            .addActivityMigrationMapping("BeforeSubProcess", "InsideSimpleSubProcess2")
            .migrate(processInstance.getId());

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
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procDefOneTask.getId())
            .addActivityMigrationMapping("InsideSimpleSubProcess2", "BeforeSubProcess")
            .migrate(processInstance.getId());

        //Confirm and move inside the subProcess
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(1, executionsAfterMigration.size()); //No subProcess
        assertThat(executionsAfterMigration).extracting(Execution::getActivityId).containsExactlyInAnyOrder("BeforeSubProcess");
        assertThat(executionsAfterMigration).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History - should contain one SubProcess ended during the migration and 3 userTask Activities, two completed before the migration and the migrated
            HistoricActivityInstance subProcess = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("subProcess")
                .singleResult();
            assertThat(subProcess).extracting(HistoricActivityInstance::getActivityId).isEqualTo("SimpleSubProcess");
            assertThat(subProcess).extracting(HistoricActivityInstance::getProcessDefinitionId).isEqualTo(procDefOneTask.getId());
    
            List<HistoricActivityInstance> userTasks = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("userTask")
                .list();
            assertEquals(3, userTasks.size());
            assertThat(userTasks).extracting(HistoricActivityInstance::getActivityId).containsOnly("BeforeSubProcess", "InsideSimpleSubProcess1");
            assertThat(userTasks).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procDefOneTask.getId());
        }

        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History Again - should contain two SubProcess and 3 userTask Activities, two completed before the migration
            List<HistoricActivityInstance> subProcesses = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("subProcess")
                .list();
            assertEquals(2, subProcesses.size());
            assertThat(subProcesses).extracting(HistoricActivityInstance::getActivityId).containsOnly("SimpleSubProcess");
            assertThat(subProcesses).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procDefOneTask.getId());
    
            List<HistoricActivityInstance>userTasks = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("userTask")
                .list();
            assertEquals(5, userTasks.size());
            assertThat(userTasks).extracting(HistoricActivityInstance::getActivityId).containsOnly("BeforeSubProcess", "InsideSimpleSubProcess1", "AfterSubProcess");
            assertThat(userTasks).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procDefOneTask.getId());
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
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procDefNested.getId())
            .addActivityMigrationMapping("BeforeSubProcess", "InsideNestedSubProcess")
            .migrate(processInstance.getId());

        //Confirm
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(3, executionsAfterMigration.size());
        assertThat(executionsAfterMigration).extracting(Execution::getActivityId).containsExactlyInAnyOrder("OuterSubProcess", "SimpleSubProcess", "InsideNestedSubProcess");
        assertThat(executionsAfterMigration).extracting("processDefinitionId").containsOnly(procDefNested.getId());

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History - should contain one SubProcess ended during the migration and 3 userTask Activities, two completed before the migration and the migrated
            List<HistoricActivityInstance> subProcesses = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("subProcess")
                .list();
            assertThat(subProcesses).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("OuterSubProcess", "SimpleSubProcess");
            assertThat(subProcesses).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procDefNested.getId());
    
            List<HistoricActivityInstance> userTasks = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("userTask")
                .list();
            assertEquals(2, userTasks.size());
            assertThat(userTasks).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("InsideNestedSubProcess", "AfterSubProcess");
            assertThat(userTasks).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procDefNested.getId());
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
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procDefNested.getId())
            .addActivityMigrationMapping("BeforeSubProcess", "InsideNestedSubProcess")
            .migrate(processInstance.getId());

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

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History - should contain one SubProcess ended during the migration and 3 userTask Activities, two completed before the migration and the migrated
            List<HistoricActivityInstance> subProcesses = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("subProcess")
                .list();
            assertThat(subProcesses).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("OuterSubProcess", "SimpleSubProcess");
            assertThat(subProcesses).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procDefNested.getId());
    
            List<HistoricActivityInstance> userTasks = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("userTask")
                .list();
            assertEquals(2, userTasks.size());
            assertThat(userTasks).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("InsideNestedSubProcess", "AfterSubProcess");
            assertThat(userTasks).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procDefNested.getId());
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
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procDefNested.getId())
            .addActivityMigrationMapping("InsideSimpleSubProcess1", "InsideNestedSubProcess")
            .migrate(processInstance.getId());

        //Confirm - we move from a subProcess to a nestedSubProcess with the same name (SimpleSubProcess), the original is not created, but cancelled and created from the new model
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(3, executionsAfterMigration.size()); //2 subProcesses and 1 userTask
        assertThat(executionsAfterMigration).extracting(Execution::getActivityId).containsExactlyInAnyOrder("OuterSubProcess", "SimpleSubProcess", "InsideNestedSubProcess");
        assertThat(executionsAfterMigration).extracting("processDefinitionId").containsOnly(procDefNested.getId());

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History - should contain one SubProcess ended during the migration and 3 userTask Activities, two completed before the migration and the migrated
            List<HistoricActivityInstance> subProcesses = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("subProcess")
                .list();
            assertThat(subProcesses).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("SimpleSubProcess", "OuterSubProcess", "SimpleSubProcess");
            assertThat(subProcesses).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procDefNested.getId());
    
            List<HistoricActivityInstance> userTasks = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("userTask")
                .list();
            assertEquals(3, userTasks.size());
            assertThat(userTasks).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("BeforeSubProcess", "InsideNestedSubProcess", "AfterSubProcess");
            assertThat(userTasks).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procDefNested.getId());
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
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procDefNested.getId())
            .addActivityMigrationMapping("InsideSimpleSubProcess1", "InsideNestedSubProcess")
            .migrate(processInstance.getId());

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

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History - should contain one SubProcess ended during the migration and 3 userTask Activities, two completed before the migration and the migrated
            List<HistoricActivityInstance> subProcesses = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("subProcess")
                .list();
            assertThat(subProcesses).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("SimpleSubProcess", "OuterSubProcess", "SimpleSubProcess");
            assertThat(subProcesses).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procDefNested.getId());
    
            List<HistoricActivityInstance> userTasks = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("userTask")
                .list();
            assertEquals(3, userTasks.size());
            assertThat(userTasks).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("BeforeSubProcess", "InsideNestedSubProcess", "AfterSubProcess");
            assertThat(userTasks).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procDefNested.getId());
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
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procDefOWithTimer.getId())
            .addActivityMigrationMapping("userTask1Id", "firstTask")
            .migrate(processInstance.getId());

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

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            List<HistoricActivityInstance> taskExecutions = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("userTask")
                .list();
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getActivityId).containsExactly("firstTask", "secondTask");
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procDefOWithTimer.getId());
    
            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();
                assertEquals(2, historicTasks.size());
                assertThat(historicTasks).extracting(HistoricTaskInstance::getTaskDefinitionKey).containsOnly("firstTask", "secondTask");
                assertThat(historicTasks).extracting(HistoricTaskInstance::getProcessDefinitionId).containsOnly(procDefOWithTimer.getId());
            }
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
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procDefOneTask.getId())
            .addActivityMigrationMapping("firstTask", "userTask1Id")
            .migrate(processInstance.getId());

        //Confirm
        List<Execution> executionsAfterMigration = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(1, executionsAfterMigration.size());
        assertThat(executionsAfterMigration).extracting(Execution::getActivityId).containsOnly("userTask1Id");
        assertThat(executionsAfterMigration).extracting("processDefinitionId").containsOnly(procDefOneTask.getId());

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            List<HistoricActivityInstance> taskExecutions = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("userTask")
                .list();
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getActivityId).containsOnly("userTask1Id");
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procDefOneTask.getId());
    
            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();
                assertEquals(1, historicTasks.size());
                assertThat(historicTasks).extracting(HistoricTaskInstance::getTaskDefinitionKey).containsOnly("userTask1Id");
                assertThat(historicTasks).extracting(HistoricTaskInstance::getProcessDefinitionId).containsOnly(procDefOneTask.getId());
            }
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
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procDefTwoTimers.getId())
            .addActivityMigrationMapping("firstTask", "secondTask")
            .migrate(processInstance.getId());

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
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            List<HistoricActivityInstance> taskExecutions = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("userTask")
                .list();
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getActivityId).containsExactly("secondTask", "thirdTask");
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procDefTwoTimers.getId());
    
            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();
                assertEquals(2, historicTasks.size());
                assertThat(historicTasks).extracting(HistoricTaskInstance::getTaskDefinitionKey).containsOnly("secondTask", "thirdTask");
                assertThat(historicTasks).extracting(HistoricTaskInstance::getProcessDefinitionId).containsOnly(procDefTwoTimers.getId());
            }
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
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procDefOneTask.getId())
            .addActivityMigrationMapping("subTask", "userTask1Id")
            .migrate(processInstance.getId());

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

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            List<HistoricActivityInstance> taskExecutions = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("userTask")
                .list();
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getActivityId).containsExactly("taskBefore", "userTask1Id");
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procDefOneTask.getId());
    
            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();
                assertThat(historicTasks).extracting(HistoricTaskInstance::getTaskDefinitionKey).containsExactly("taskBefore", "userTask1Id");
                assertThat(historicTasks).extracting(HistoricTaskInstance::getProcessDefinitionId).containsOnly(procDefOneTask.getId());
            }
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
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procDefSubProcWithTimer.getId())
            .addActivityMigrationMapping("userTask1Id", "subTask")
            .migrate(processInstance.getId());

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

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            List<HistoricActivityInstance> taskExecutions = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("userTask")
                .list();
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getActivityId).containsExactly("subTask", "taskAfter");
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procDefSubProcWithTimer.getId());
    
            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();
                assertThat(historicTasks).extracting(HistoricTaskInstance::getTaskDefinitionKey).containsExactly("subTask", "taskAfter");
                assertThat(historicTasks).extracting(HistoricTaskInstance::getProcessDefinitionId).containsOnly(procDefSubProcWithTimer.getId());
            }
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
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procDefSubProcWithTimer.getId())
            .addActivityMigrationMapping("userTask1Id", "subTask")
            .migrate(processInstance.getId());

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
            //Check History
            List<HistoricActivityInstance> taskExecutions = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("userTask")
                .list();
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getActivityId).containsExactly("subTask");
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procDefSubProcWithTimer.getId());
    
            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();
                assertThat(historicTasks).extracting(HistoricTaskInstance::getTaskDefinitionKey).containsExactly("subTask");
                assertThat(historicTasks).extracting(HistoricTaskInstance::getProcessDefinitionId).containsOnly(procDefSubProcWithTimer.getId());
            }
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
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procVersion2.getId())
            .addActivityMigrationMapping("subTask", "subTask2")
            .migrate(processInstance.getId());

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

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            List<HistoricActivityInstance> taskExecutions = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("userTask")
                .list();
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("taskBefore", "subTask2", "taskAfter");
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procVersion2.getId());
    
            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();
                assertThat(historicTasks).extracting(HistoricTaskInstance::getTaskDefinitionKey).containsExactly("taskBefore", "subTask2", "taskAfter");
                assertThat(historicTasks).extracting(HistoricTaskInstance::getProcessDefinitionId).containsOnly(procVersion2.getId());
            }
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
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procDefTimerTaskInSubProcess.getId())
            .addActivityMigrationMapping("userTask1Id", "subTask")
            .migrate(processInstance.getId());

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

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            List<HistoricActivityInstance> taskExecutions = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("userTask")
                .list();
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getActivityId).containsExactly("subTask", "subTask2", "taskAfter");
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procDefTimerTaskInSubProcess.getId());
    
            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();
                assertThat(historicTasks).extracting(HistoricTaskInstance::getTaskDefinitionKey).containsExactly("subTask", "subTask2", "taskAfter");
                assertThat(historicTasks).extracting(HistoricTaskInstance::getProcessDefinitionId).containsOnly(procDefTimerTaskInSubProcess.getId());
            }
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
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procDefTimerTaskInSubProcess.getId())
            .addActivityMigrationMapping("userTask1Id", "subTask")
            .migrate(processInstance.getId());

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
            //Check History
            List<HistoricActivityInstance> taskExecutions = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("userTask")
                .list();
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getActivityId).containsExactly("subTask", "taskAfter");
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procDefTimerTaskInSubProcess.getId());
    
            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();
                assertThat(historicTasks).extracting(HistoricTaskInstance::getTaskDefinitionKey).containsExactly("subTask", "taskAfter");
                assertThat(historicTasks).extracting(HistoricTaskInstance::getProcessDefinitionId).containsOnly(procDefTimerTaskInSubProcess.getId());
            }
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
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procWithSignal.getId())
            .addActivityMigrationMapping("userTask1Id", "intermediateCatchEvent")
            .migrate(processInstance.getId());

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
        //TODO WIP -- Possible bug? the Signal name should be "someSignal"
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

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            List<HistoricActivityInstance> taskExecutions = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("userTask")
                .list();
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("userTask1Id", "afterCatchEvent");
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procWithSignal.getId());
    
            List<HistoricActivityInstance> eventExecutions = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("intermediateCatchEvent")
                .list();
            assertThat(eventExecutions).extracting(HistoricActivityInstance::getActivityId).containsExactly("intermediateCatchEvent");
            assertThat(eventExecutions).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procWithSignal.getId());
    
            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();
                assertThat(historicTasks).extracting(HistoricTaskInstance::getTaskDefinitionKey).containsExactly("userTask1Id", "afterCatchEvent");
                assertThat(historicTasks).extracting(HistoricTaskInstance::getProcessDefinitionId).containsOnly(procWithSignal.getId());
            }
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
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procDefOneTask.getId())
            .addActivityMigrationMapping("intermediateCatchEvent", "userTask1Id")
            .migrate(processInstance.getId());

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

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            List<HistoricActivityInstance> taskExecutions = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("userTask")
                .list();
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("beforeCatchEvent", "userTask1Id");
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procDefOneTask.getId());
    
            List<HistoricActivityInstance> eventExecutions = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("intermediateCatchEvent")
                .list();
            assertThat(eventExecutions).extracting(HistoricActivityInstance::getActivityId).containsExactly("intermediateCatchEvent");
            assertThat(eventExecutions).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procDefOneTask.getId());
    
            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();
                assertThat(historicTasks).extracting(HistoricTaskInstance::getTaskDefinitionKey).containsExactly("beforeCatchEvent", "userTask1Id");
                assertThat(historicTasks).extracting(HistoricTaskInstance::getProcessDefinitionId).containsOnly(procDefOneTask.getId());
            }
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
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procWithSignalVer2.getId())
            .addActivityMigrationMapping("intermediateCatchEvent", "newIntermediateCatchEvent")
            .migrate(processInstance.getId());

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
        //TODO WIP -- Possible bug? the Signal name should be "someNewSignal"
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

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            List<HistoricActivityInstance> taskExecutions = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("userTask")
                .list();
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("beforeCatchEvent", "afterNewCatchEvent");
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procWithSignalVer2.getId());
    
            List<HistoricActivityInstance> eventExecutions = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("intermediateCatchEvent")
                .list();
            assertThat(eventExecutions).extracting(HistoricActivityInstance::getActivityId).containsExactly("intermediateCatchEvent", "newIntermediateCatchEvent");
            assertThat(eventExecutions).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procWithSignalVer2.getId());
    
            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();
                assertThat(historicTasks).extracting(HistoricTaskInstance::getTaskDefinitionKey).containsExactly("beforeCatchEvent", "afterNewCatchEvent");
                assertThat(historicTasks).extracting(HistoricTaskInstance::getProcessDefinitionId).containsOnly(procWithSignalVer2.getId());
            }
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
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procWithSignal.getId())
            .addActivityMigrationMapping("userTask1Id", "intermediateCatchEvent")
            .migrate(processInstance.getId());

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

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            List<HistoricActivityInstance> taskExecutions = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("userTask")
                .list();
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("userTask1Id", "afterCatchEvent");
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procWithSignal.getId());
    
            List<HistoricActivityInstance> eventExecutions = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("intermediateCatchEvent")
                .list();
            assertThat(eventExecutions).extracting(HistoricActivityInstance::getActivityId).containsExactly("intermediateCatchEvent");
            assertThat(eventExecutions).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procWithSignal.getId());
    
            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();
                assertThat(historicTasks).extracting(HistoricTaskInstance::getTaskDefinitionKey).containsExactly("userTask1Id", "afterCatchEvent");
                assertThat(historicTasks).extracting(HistoricTaskInstance::getProcessDefinitionId).containsOnly(procWithSignal.getId());
            }
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
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procDefOneTask.getId())
            .addActivityMigrationMapping("intermediateCatchEvent", "userTask1Id")
            .migrate(processInstance.getId());

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

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            List<HistoricActivityInstance> taskExecutions = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("userTask")
                .list();
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("beforeCatchEvent", "userTask1Id");
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procDefOneTask.getId());
    
            List<HistoricActivityInstance> eventExecutions = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("intermediateCatchEvent")
                .list();
            assertThat(eventExecutions).extracting(HistoricActivityInstance::getActivityId).containsExactly("intermediateCatchEvent");
            assertThat(eventExecutions).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procDefOneTask.getId());
    
            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();
                assertThat(historicTasks).extracting(HistoricTaskInstance::getTaskDefinitionKey).containsExactly("beforeCatchEvent", "userTask1Id");
                assertThat(historicTasks).extracting(HistoricTaskInstance::getProcessDefinitionId).containsOnly(procDefOneTask.getId());
            }
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
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procWithSignalVer2.getId())
            .addActivityMigrationMapping("intermediateCatchEvent", "intermediateNewCatchEvent")
            .migrate(processInstance.getId());

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

        //Complete the process
        completeProcessInstanceTasks(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            //Check History
            List<HistoricActivityInstance> taskExecutions = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("userTask")
                .list();
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder("beforeCatchEvent", "afterNewCatchEvent");
            assertThat(taskExecutions).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procWithSignalVer2.getId());
    
            List<HistoricActivityInstance> eventExecutions = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType("intermediateCatchEvent")
                .list();
            assertThat(eventExecutions).extracting(HistoricActivityInstance::getActivityId).containsExactly("intermediateCatchEvent", "intermediateNewCatchEvent");
            assertThat(eventExecutions).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procWithSignalVer2.getId());
    
            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();
                assertThat(historicTasks).extracting(HistoricTaskInstance::getTaskDefinitionKey).containsExactly("beforeCatchEvent", "afterNewCatchEvent");
                assertThat(historicTasks).extracting(HistoricTaskInstance::getProcessDefinitionId).containsOnly(procWithSignalVer2.getId());
            }
        }

        assertProcessEnded(processInstance.getId());
    }
}
