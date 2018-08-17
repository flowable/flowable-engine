package org.flowable.engine.test.api.migration;

import java.util.List;
import java.util.stream.Collectors;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.impl.persistence.entity.DeploymentEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;

public class ProcessInstanceMigrationTest extends PluggableFlowableTestCase {

    public void testSimpleMigration() {
        //Deploy first version of the process
        Deployment version1Deployment = repositoryService.createDeployment()
            .name("My Process Deployment")
            .addClasspathResource("org/flowable/engine/test/api/runtime/migration/MyProcess-v1.bpmn20.xml")
            .deploy();

        //Start and instance of the recent first version of the process
        ProcessInstance version1ProcessInstance = runtimeService.startProcessInstanceByKey("MP");

        //Deploy second version of the process
        Deployment version2Deployment = repositoryService.createDeployment()
            .name("My Process Deployment")
            .addClasspathResource("org/flowable/engine/test/api/runtime/migration/MyProcess-v2.bpmn20.xml")
            .deploy();

        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
            .processDefinitionKey("MP")
            .processDefinitionWithoutTenantId()
            .list();

        assertEquals(2, processDefinitions.size());

        ProcessDefinition version1ProcessDef = processDefinitions.stream().filter(d -> d.getVersion() == 1).findFirst().get();
        ProcessDefinition version2ProcessDef = processDefinitions.stream().filter(d -> d.getVersion() == 2).findFirst().get();

        List<Execution> executionsBefore = runtimeService.createExecutionQuery().list();
        assertEquals(2, executionsBefore.size()); //includes root execution
        executionsBefore.stream()
            .map(e -> (ExecutionEntity) e)
            .forEach(e -> assertEquals(version1ProcessDef.getId(), e.getProcessDefinitionId()));

        List<Task> tasksBefore = taskService.createTaskQuery().list();
        assertEquals(1, tasksBefore.size());
        assertEquals(version1ProcessDef.getId(), tasksBefore.get(0).getProcessDefinitionId());

        runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(version2ProcessDef.getId())
            .addProcessInstanceToMigrate(version1ProcessInstance.getId())
            .migrate();

        List<Execution> executionsAfter = runtimeService.createExecutionQuery().list();
        assertEquals(2, executionsAfter.size()); //includes root execution
        executionsAfter.stream()
            .map(e -> (ExecutionEntity) e)
            .forEach(e -> assertEquals(version2ProcessDef.getId(), e.getProcessDefinitionId()));

        List<Task> tasksAfter = taskService.createTaskQuery().list();
        assertEquals(1, tasksAfter.size());
        assertEquals(version2ProcessDef.getId(), tasksAfter.get(0).getProcessDefinitionId());

        tasksAfter.stream().forEach(this::completeTask);

        tasksAfter = taskService.createTaskQuery().list();
        assertEquals(1, tasksAfter.size());

        repositoryService.deleteDeployment(version1Deployment.getId(), true);
        repositoryService.deleteDeployment(version2Deployment.getId(), true);
        System.out.printf("done");

    }

    public void testDebug() {
        org.flowable.engine.repository.Deployment version1 = repositoryService.createDeployment()
            .name("My Process Deployment")
            .addClasspathResource("org/flowable/engine/test/api/runtime/migration/MyProcess-v1.bpmn20.xml")
            .deploy();

        org.flowable.engine.repository.Deployment version2 = repositoryService.createDeployment()
            .name("My Process Deployment")
            .addClasspathResource("org/flowable/engine/test/api/runtime/migration/MyProcess-v2.bpmn20.xml")
            .deploy();

        ProcessDefinition mp = processEngineConfiguration.getCommandExecutor()
            .execute(commandContext -> CommandContextUtil.getProcessDefinitionEntityManager(commandContext).findProcessDefinitionByKeyAndVersionAndTenantId("MP", 1, null));

        List<ProcessDefinition> procDefs = repositoryService.createProcessDefinitionQuery().processDefinitionKey("MP").list();

        List<DeploymentEntity> deployments = repositoryService.createDeploymentQuery().list().stream().map(d -> (DeploymentEntity) d)
            .collect(Collectors.toList());

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("MP");

        BpmnModel bpmnModel = repositoryService.getBpmnModel(processInstance.getProcessDefinitionId());

        System.out.printf("done");

    }

    public void testMissingActivityMapping() {

    }

}
