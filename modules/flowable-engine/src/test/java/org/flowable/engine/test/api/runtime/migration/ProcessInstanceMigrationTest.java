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
import java.util.List;

import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @author Dennis Federico
 */
public class ProcessInstanceMigrationTest extends PluggableFlowableTestCase {

    @AfterEach
    protected void tearDown() throws Exception {
        deleteDeployments();
    }

    public ProcessDefinition deployProcessDefinition(String name, String path) {
        Deployment deployment = repositoryService.createDeployment()
            .name(name)
            .addClasspathResource(path)
            .deploy();

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
            .deploymentId(deployment.getId()).singleResult();

        return processDefinition;
    }

    @Test
    public void testSimpleMigrationWithActivityAutoMapping() {
        //Deploy first version of the process
        ProcessDefinition version1ProcessDef = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/MyProcess-v1.bpmn20.xml");

        //Start and instance of the recent first version of the process for migration and one for reference
        ProcessInstance processInstanceToMigrate = runtimeService.startProcessInstanceByKey("MP");

        //Deploy second version of the process
        ProcessDefinition version2ProcessDef = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/MyProcess-v2.bpmn20.xml");

        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
            .processDefinitionKey("MP")
            .processDefinitionWithoutTenantId()
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
            .addClasspathResource("org/flowable/engine/test/api/runtime/migration/MyProcess-v1.bpmn20.xml")
            .deploy();

        //Start and instance of the recent first version of the process for migration and one for reference
        ProcessInstance processInstanceToMigrate = runtimeService.startProcessInstanceByKey("MP");

        //Deploy second version of the process
        Deployment twoActivitiesProcessDeployment = repositoryService.createDeployment()
            .name("My Process Deployment")
            .addClasspathResource("org/flowable/engine/test/api/runtime/migration/MyProcess-v2.bpmn20.xml")
            .deploy();

        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
            .processDefinitionKey("MP")
            .processDefinitionWithoutTenantId()
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
            .addClasspathResource("org/flowable/engine/test/api/runtime/migration/MyProcess-v1.bpmn20.xml")
            .deploy();

        //Start and instance of the recent first version of the process for migration and one for reference
        ProcessInstance processInstanceToMigrate = runtimeService.startProcessInstanceByKey("MP");

        //Deploy second version of the process
        Deployment twoActivitiesProcessDeployment = repositoryService.createDeployment()
            .name("My Process Deployment")
            .addClasspathResource("org/flowable/engine/test/api/runtime/migration/MyProcess-v2.bpmn20.xml")
            .deploy();

        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
            .processDefinitionKey("MP")
            .processDefinitionWithoutTenantId()
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

        //This new process definition has two activities, but we have mapped to the last activity explicitely
        taskService.complete(tasks.get(0).getId());
        assertProcessEnded(processInstanceToMigrate.getId());

    }

    @Test
    public void testSimpleMigrationWithExplicitActivityMapping3() {
        //Deploy first version of the process
        Deployment twoActivitiesProcessDeployment = repositoryService.createDeployment()
            .name("My Process Deployment")
            .addClasspathResource("org/flowable/engine/test/api/runtime/migration/MyProcess-v2.bpmn20.xml")
            .deploy();

        //Start and instance of the recent first version of the process for migration and one for reference
        ProcessInstance processInstanceToMigrate = runtimeService.startProcessInstanceByKey("MP");

        //Deploy second version of the process
        Deployment oneActivityProcessDeployment = repositoryService.createDeployment()
            .name("My Process Deployment")
            .addClasspathResource("org/flowable/engine/test/api/runtime/migration/MyProcess-v1.bpmn20.xml")
            .deploy();

        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
            .processDefinitionKey("MP")
            .processDefinitionWithoutTenantId()
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
        ProcessDefinition version1ProcessDef = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/MyProcess-v1.bpmn20.xml");

        //Start and instance of the recent first version of the process for migration and one for reference
        ProcessInstance processInstanceToMigrate = runtimeService.startProcessInstanceByKey("MP");

        //Deploy second version of the process
        ProcessDefinition version2ProcessDef = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/MyProcess-v2.bpmn20.xml");

        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
            .processDefinitionKey("MP")
            .processDefinitionWithoutTenantId()
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

        List<HistoricActivityInstance> historicActivityInstancesBefore = historyService.createHistoricActivityInstanceQuery().orderByExecutionId().asc().list();
        List<HistoricTaskInstance> historicTaskInstancesBefore = historyService.createHistoricTaskInstanceQuery().orderByExecutionId().asc().list();

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

        List<HistoricActivityInstance> historicActivityInstancesAfter = historyService.createHistoricActivityInstanceQuery().orderByExecutionId().asc().list();
        List<HistoricTaskInstance> historicTaskInstancesAfter = historyService.createHistoricTaskInstanceQuery().orderByExecutionId().asc().list();

        assertEquals(historicActivityInstancesBefore.size(), historicActivityInstancesAfter.size());
        assertThat(historicActivityInstancesBefore)
            .usingElementComparatorIgnoringFields("revision", "processDefinitionId")
            .containsExactlyInAnyOrderElementsOf(historicActivityInstancesAfter);
        assertEquals(historicTaskInstancesBefore.size(), historicTaskInstancesAfter.size());
        assertThat(historicTaskInstancesBefore)
            .usingElementComparatorIgnoringFields("revision", "processDefinitionId", "originalPersistentState", "lastUpdateTime")
            .containsExactlyInAnyOrderElementsOf(historicTaskInstancesAfter);

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

        completeProcessTasks(processInstance.getId());

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

        completeProcessTasks(processInstance.getId());

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

        completeProcessTasks(processInstance.getId());

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

        completeProcessTasks(processInstance.getId());

        //Check History Again - should contain two SubProcess and 3 userTask Activities, two completed before the migration
        List<HistoricActivityInstance> subProcesses = historyService.createHistoricActivityInstanceQuery()
            .processInstanceId(processInstance.getId())
            .activityType("subProcess")
            .list();
        assertEquals(2, subProcesses.size());
        assertThat(subProcesses).extracting(HistoricActivityInstance::getActivityId).containsOnly("SimpleSubProcess");
        assertThat(subProcesses).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procDefOneTask.getId());

        userTasks = historyService.createHistoricActivityInstanceQuery()
            .processInstanceId(processInstance.getId())
            .activityType("userTask")
            .list();
        assertEquals(5, userTasks.size());
        assertThat(userTasks).extracting(HistoricActivityInstance::getActivityId).containsOnly("BeforeSubProcess", "InsideSimpleSubProcess1", "AfterSubProcess");
        assertThat(userTasks).extracting(HistoricActivityInstance::getProcessDefinitionId).containsOnly(procDefOneTask.getId());

        assertProcessEnded(processInstance.getId());
    }

    //TODO ... Nested embedded subprocesses

    protected void completeProcessTasks(String processInstanceId) {
        List<Task> tasks;
        do {
            tasks = taskService.createTaskQuery().processInstanceId(processInstanceId).list();
            tasks.forEach(this::completeTask);
        } while (!tasks.isEmpty());
    }

}
