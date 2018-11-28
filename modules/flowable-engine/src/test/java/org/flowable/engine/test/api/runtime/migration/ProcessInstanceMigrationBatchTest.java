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

import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.impl.migration.ProcessInstanceMigrationValidationResult;
import org.flowable.engine.impl.test.JobTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.migration.ProcessInstanceMigrationBuilder;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessMigrationBatch;
import org.flowable.engine.test.api.runtime.changestate.ChangeStateEventListener;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
    public void testSimpleBatchMigrationValidationInvalidTargetProcessDefinitionId() {
        //Deploy first version of the process
        ProcessDefinition version1ProcessDef = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/two-tasks-simple-process.bpmn20.xml");

        //Prepare the process Instance migration builder as usual
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processInstanceMigrationService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition("UnknownKey", 1);

        //Submit the batch
        try {
            String batchId = processInstanceMigrationBuilder.batchValidateMigrationOfProcessInstances(version1ProcessDef.getId());
            fail("Should not create the batches");
        } catch (FlowableException e) {
            assertTextPresent("no processes deployed with key 'UnknownKey'", e.getMessage());
        }
    }

    @Test
    public void testSimpleBatchMigrationValidationInvalidSourceProcessDefinitionId() {
        //Deploy first version of the process
        ProcessDefinition version1ProcessDef = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/two-tasks-simple-process.bpmn20.xml");

        //Prepare the process Instance migration builder as usual
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processInstanceMigrationService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(version1ProcessDef.getId());

        //Submit the batch
        try {
            String batchId = processInstanceMigrationBuilder.batchValidateMigrationOfProcessInstances("UnknownKey", 2, null);
            fail("Should not create the batches");
        } catch (FlowableException e) {
            assertTextPresent("no processes deployed with key 'UnknownKey'", e.getMessage());
        }
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

        //Prepare the process Instance migration builder as usual
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processInstanceMigrationService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(version2ProcessDef.getId());

        //Submit the batch
        String batchId = processInstanceMigrationBuilder.batchValidateMigrationOfProcessInstances(version1ProcessDef.getId());
        assertTrue(JobTestHelper.areJobsAvailable(managementService));

        //Confirm the batch is not finished
        ProcessMigrationBatch processMigrationBatch = processInstanceMigrationService.getProcessMigrationBatchById(batchId);
        assertThat(processMigrationBatch).extracting(ProcessMigrationBatch::isCompleted).isEqualTo(false);
        ProcessInstanceMigrationValidationResult validationResult = processInstanceMigrationService.getAggregatedResultOfBatchProcessInstanceMigrationValidation(batchId);
        assertThat(validationResult).isNull();

        //Start async executor to process the batches
        executeJobExecutorForTime(1000L, 500L);
        assertFalse(JobTestHelper.areJobsAvailable(managementService));

        //Confirm the batches have ended
        //        processMigrationBatch = processInstanceMigrationService.getProcessMigrationBatchById(batchId);
        processMigrationBatch = processInstanceMigrationService.getProcessMigrationBatchAndResourcesById(batchId);
        assertThat(processMigrationBatch).extracting(ProcessMigrationBatch::isCompleted).isEqualTo(true);
        validationResult = processInstanceMigrationService.getAggregatedResultOfBatchProcessInstanceMigrationValidation(batchId);
        assertThat(validationResult).isNotNull();
        assertThat(validationResult.getValidationMessages())
            .containsExactlyInAnyOrder("[Process instance (id:'" + processInstance1.getId() + "') has a running Activity (id:'userTask2Id') that is not mapped for migration (Or its Multi-Instance parent)]",
                "[Process instance (id:'" + processInstance2.getId() + "') has a running Activity (id:'userTask2Id') that is not mapped for migration (Or its Multi-Instance parent)]");

        processInstanceMigrationService.deleteBatchAndResourcesById(batchId);

        //TODO WIP - Batch migrate with error

        completeProcessInstanceTasks(processInstance1.getId());
        completeProcessInstanceTasks(processInstance2.getId());
        assertProcessEnded(processInstance1.getId());
        assertProcessEnded(processInstance2.getId());
    }

    @Test
    public void testSimpleBatchMigrationValidationMissingMappingPartialAutoMap() {
        //Deploy first version of the process
        ProcessDefinition version1ProcessDef = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/two-tasks-simple-process.bpmn20.xml");

        //Start and instance of the recent first version of the process for migration and one for reference
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("MP");
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("MP");

        Task task = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).singleResult();
        completeTask(task);

        task = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask2Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(version1ProcessDef.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance2.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
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

        //Prepare the process Instance migration builder as usual
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processInstanceMigrationService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(version2ProcessDef.getId());

        //Submit the batch
        String batchId = processInstanceMigrationBuilder.batchValidateMigrationOfProcessInstances(version1ProcessDef.getId());
        assertTrue(JobTestHelper.areJobsAvailable(managementService));

        //Confirm the batch is not finished
        ProcessMigrationBatch processMigrationBatch = processInstanceMigrationService.getProcessMigrationBatchById(batchId);
        assertThat(processMigrationBatch).extracting(ProcessMigrationBatch::isCompleted).isEqualTo(false);
        ProcessInstanceMigrationValidationResult validationResult = processInstanceMigrationService.getAggregatedResultOfBatchProcessInstanceMigrationValidation(batchId);
        assertThat(validationResult).isNull();

        //Start async executor to process the batches
        executeJobExecutorForTime(1000L, 500L);
        assertFalse(JobTestHelper.areJobsAvailable(managementService));

        //Confirm the batches have ended
        processMigrationBatch = processInstanceMigrationService.getProcessMigrationBatchById(batchId);
        assertThat(processMigrationBatch).extracting(ProcessMigrationBatch::isCompleted).isEqualTo(true);
        validationResult = processInstanceMigrationService.getAggregatedResultOfBatchProcessInstanceMigrationValidation(batchId);
        assertThat(validationResult).isNotNull();
        assertThat(validationResult.getValidationMessages())
            .containsExactlyInAnyOrder("[Process instance (id:'" + processInstance1.getId() + "') has a running Activity (id:'userTask2Id') that is not mapped for migration (Or its Multi-Instance parent)]");

        processInstanceMigrationService.deleteBatchAndResourcesById(batchId);

        //TODO WIP - Batch migrate with error

        completeProcessInstanceTasks(processInstance1.getId());
        completeProcessInstanceTasks(processInstance2.getId());
        assertProcessEnded(processInstance1.getId());
        assertProcessEnded(processInstance2.getId());
    }
}
