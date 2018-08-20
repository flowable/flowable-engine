package org.flowable.engine.test.api.migration;

import java.util.List;

import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;

public class ProcessInstanceMigrationTest extends PluggableFlowableTestCase {

    public void testSimpleMigrationWithActivityAutoMapping() {
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

        //Migrate process
        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(version2ProcessDef.getId())
            .addProcessInstanceToMigrate(processInstanceToMigrate.getId())
            .migrate();

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

        repositoryService.deleteDeployment(oneActivityProcessDeployment.getId(), true);
        repositoryService.deleteDeployment(twoActivitiesProcessDeployment.getId(), true);
    }

    public void testSimpleMigrationWithExplicitActivityMapping() {
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
            .addProcessInstanceToMigrate(processInstanceToMigrate.getId())
            .addActivityMigrationMapping("userTask1Id", "userTask1Id")
            .migrate();

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

        repositoryService.deleteDeployment(oneActivityProcessDeployment.getId(), true);
        repositoryService.deleteDeployment(twoActivitiesProcessDeployment.getId(), true);
    }

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
            .addProcessInstanceToMigrate(processInstanceToMigrate.getId())
            .addActivityMigrationMapping("userTask1Id", "userTask2Id")
            .migrate();

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

        repositoryService.deleteDeployment(oneActivityProcessDeployment.getId(), true);
        repositoryService.deleteDeployment(twoActivitiesProcessDeployment.getId(), true);
    }

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
            .addProcessInstanceToMigrate(processInstanceToMigrate.getId())
            .addActivityMigrationMapping("userTask2Id", "userTask1Id")
            .migrate();

        executions = runtimeService.createExecutionQuery().list();
        assertEquals(2, executions.size()); //includes root execution
        executions.stream()
            .map(e -> (ExecutionEntity) e)
            .forEach(e -> assertEquals(version2ProcessDef.getId(), e.getProcessDefinitionId()));

        tasks = taskService.createTaskQuery().list();
        assertEquals(1, tasks.size());
        assertEquals(version2ProcessDef.getId(), tasks.get(0).getProcessDefinitionId());
        assertEquals("userTask1Id", tasks.get(0).getTaskDefinitionKey());

        //This new process version only have one activity
        taskService.complete(tasks.get(0).getId());
        assertProcessEnded(processInstanceToMigrate.getId());

        repositoryService.deleteDeployment(oneActivityProcessDeployment.getId(), true);
        repositoryService.deleteDeployment(twoActivitiesProcessDeployment.getId(), true);
    }

}
