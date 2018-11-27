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

import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.engine.impl.migration.ProcessInstanceMigrationValidationResult;
import org.flowable.engine.impl.persistence.entity.ProcessMigrationBatchEntity;
import org.flowable.engine.impl.persistence.entity.ProcessMigrationBatchEntityManager;
import org.flowable.engine.impl.test.JobTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.migration.ActivityMigrationMapping;
import org.flowable.engine.migration.ProcessInstanceMigrationBuilder;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessMigrationBatch;
import org.flowable.engine.test.api.runtime.changestate.ChangeStateEventListener;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author Dennis Federico
 */
public class ProcessInstanceMigrationBatchTest extends PluggableFlowableTestCase {

    private ChangeStateEventListener changeStateEventListener = new ChangeStateEventListener();

    @BeforeEach
    protected void setUp() {
        processEngine.getRuntimeService().addEventListener(changeStateEventListener);
    }

    @AfterEach
    protected void tearDown() {
        processEngine.getRuntimeService().removeEventListener(changeStateEventListener);
        deleteProcessInstanceMigrationBatches();
        deleteDeployments();
    }

    @Test
    public void testSimpleBatchMigrationValidationMissingMapping() {
        //Deploy first version of the process
        ProcessDefinition version1ProcessDef = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/two-tasks-simple-process.bpmn20.xml");

        //Start and instance of the recent first version of the process for migration and one for reference
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("MP");
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("MP");

        Task task = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).singleResult();
        completeTask(task);
        task = taskService.createTaskQuery().processInstanceId(processInstance2.getId()).singleResult();
        completeTask(task);

        task = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask2Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(version1ProcessDef.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance2.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask2Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(version1ProcessDef.getId());

        //Deploy second version of the process
        ProcessDefinition version2ProcessDef = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");

        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
            .processDefinitionKey("MP")
            .list();

        assertEquals(2, processDefinitions.size());
        processDefinitions.sort(Comparator.comparingInt(ProcessDefinition::getVersion));
        assertEquals(processDefinitions.get(0).getId(), version1ProcessDef.getId());
        assertEquals(processDefinitions.get(1).getId(), version2ProcessDef.getId());

        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processInstanceMigrationService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(version2ProcessDef.getId());

        ProcessMigrationBatchEntity processMigrationBatchEntity = processInstanceMigrationBuilder.batchValidateMigrationOfProcessInstances(version1ProcessDef.getId());

        assertTrue(JobTestHelper.areJobsAvailable(managementService));
        executeJobExecutorForTime(1000L, 500L);

        assertFalse(JobTestHelper.areJobsAvailable(managementService));

        //TODO WIP - Hack until we have a service to query the batch with its results (eager fetch results) or aggregate by the service
        CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutor();
        ProcessMigrationBatchEntity newParent = commandExecutor.execute(commandContext -> {
            ProcessMigrationBatchEntityManager manager = CommandContextUtil.getProcessMigrationBatchEntityManager(commandContext);
            ProcessMigrationBatchEntity batchParent = manager.findById(processMigrationBatchEntity.getId());

            if (batchParent.getBatchChildren() != null) {
                batchParent.getBatchChildren().forEach(child -> child.getResult());
            }
            batchParent.getMigrationDocumentJson();

            return batchParent;
        });

        assertThat(newParent.getBatchChildren()).isNotNull();
        assertThat(newParent.getBatchChildren()).size().isEqualTo(2);
        List<ProcessMigrationBatch> batchChildren = newParent.getBatchChildren();
        assertThat(newParent.getBatchChildren()).extracting(ProcessMigrationBatch::getResult)
            .containsExactlyInAnyOrder("[Process instance (id:'" + processInstance1.getId() + "') has a running Activity (id:'userTask2Id') that is not mapped for migration (Or its Multi-Instance parent)]",
                "[Process instance (id:'" + processInstance2.getId() + "') has a running Activity (id:'userTask2Id') that is not mapped for migration (Or its Multi-Instance parent)]");

        commandExecutor.<Void>execute(commandContext -> {
            ProcessMigrationBatchEntityManager manager = CommandContextUtil.getProcessMigrationBatchEntityManager(commandContext);
            manager.deleteParentBatchAndChildrenAndResources(processMigrationBatchEntity.getId());
            return null;
        });

        //
        //        //Migrate process
        //        processInstanceMigrationBuilder.migrateProcessInstances(version1ProcessDef.getId());
        //
        //
        //        task = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).singleResult();
        //        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        //        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(version2ProcessDef.getId());
        //        task = taskService.createTaskQuery().processInstanceId(processInstance2.getId()).singleResult();
        //        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        //        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(version2ProcessDef.getId());

        completeProcessInstanceTasks(processInstance1.getId());
        completeProcessInstanceTasks(processInstance2.getId());
        assertProcessEnded(processInstance1.getId());
        assertProcessEnded(processInstance2.getId());
    }

    @Test
    @Disabled("WIP")
    public void testSimpleBatchMigrationWithActivityAutoMapping() {
        //Deploy first version of the process
        ProcessDefinition version1ProcessDef = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/two-tasks-simple-process.bpmn20.xml");

        //Start and instance of the recent first version of the process for migration and one for reference
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("MP");
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("MP");

        Task task = taskService.createTaskQuery().processInstanceId(processInstance2.getId()).singleResult();
        completeTask(task);

        task = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(version1ProcessDef.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance2.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask2Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(version1ProcessDef.getId());

        //Deploy second version of the process
        ProcessDefinition version2ProcessDef = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");

        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
            .processDefinitionKey("MP")
            .list();

        assertEquals(2, processDefinitions.size());
        processDefinitions.sort(Comparator.comparingInt(ProcessDefinition::getVersion));
        assertEquals(processDefinitions.get(0).getId(), version1ProcessDef.getId());
        assertEquals(processDefinitions.get(1).getId(), version2ProcessDef.getId());

        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processInstanceMigrationService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(version2ProcessDef.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask2Id", "userTask1Id"));
        ProcessInstanceMigrationValidationResult validationResult = processInstanceMigrationBuilder.validateMigrationOfProcessInstances(version1ProcessDef.getId());
        assertThat(validationResult.getValidationMessages()).isEmpty();

        //Migrate process
        processInstanceMigrationBuilder.migrateProcessInstances(version1ProcessDef.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(version2ProcessDef.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance2.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(version2ProcessDef.getId());

        completeProcessInstanceTasks(processInstance1.getId());
        completeProcessInstanceTasks(processInstance2.getId());

        assertProcessEnded(processInstance1.getId());
        assertProcessEnded(processInstance2.getId());
    }

}
