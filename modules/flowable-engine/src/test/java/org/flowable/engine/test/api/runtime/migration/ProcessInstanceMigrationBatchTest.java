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

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.assertj.core.api.iterable.Extractor;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.impl.jobexecutor.ProcessInstanceMigrationJobHandler;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Dennis Federico
 */
public class ProcessInstanceMigrationBatchTest extends PluggableFlowableTestCase {

    private ObjectMapper objectMapper;
    private ChangeStateEventListener changeStateEventListener = new ChangeStateEventListener();

    @BeforeEach
    protected void setUp() {
        objectMapper = processEngineConfiguration.getObjectMapper();
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
        String validationBatchId = processInstanceMigrationBuilder.batchValidateMigrationOfProcessInstances(version1ProcessDef.getId());
        assertTrue(JobTestHelper.areJobsAvailable(managementService));

        //Confirm the batch is not finished
        ProcessMigrationBatch validationBatch = processInstanceMigrationService.getProcessMigrationBatchById(validationBatchId);
        assertThat(validationBatch).extracting(ProcessMigrationBatch::isCompleted).isEqualTo(false);
        List<ProcessInstanceMigrationValidationResult> validationResults = processInstanceMigrationService.getResultsOfBatchProcessInstanceMigrationValidation(validationBatchId);
        assertThat(validationResults).isNull();

        //Start async executor to process the batches
        executeJobExecutorForTime(1000L, 500L);
        assertFalse(JobTestHelper.areJobsAvailable(managementService));

        //Confirm the batches have ended
        validationBatch = processInstanceMigrationService.getProcessMigrationBatchById(validationBatchId);
        assertThat(validationBatch).extracting(ProcessMigrationBatch::isCompleted).isEqualTo(true);
        validationResults = processInstanceMigrationService.getResultsOfBatchProcessInstanceMigrationValidation(validationBatchId);
        assertThat(validationResults).isNotNull();
        assertThat(validationResults).size().isEqualTo(2);
        assertThat(validationResults).extracting(ProcessInstanceMigrationValidationResult::getProcessInstanceId)
            .containsExactlyInAnyOrder(processInstance1.getId(), processInstance2.getId());
        assertThat(validationResults).<List<String>>extracting(ProcessInstanceMigrationValidationResult::getValidationMessages)
            .containsExactlyInAnyOrder(Collections.singletonList("Process instance (id:'" + processInstance1.getId() + "') has a running Activity (id:'userTask2Id') that is not mapped for migration (Or its Multi-Instance parent)"),
                Collections.singletonList("Process instance (id:'" + processInstance2.getId() + "') has a running Activity (id:'userTask2Id') that is not mapped for migration (Or its Multi-Instance parent)"));

        //Try batch migrate the process instances
        String migrationBatchId = processInstanceMigrationBuilder.batchMigrateProcessInstances(version1ProcessDef.getId());
        assertTrue(JobTestHelper.areJobsAvailable(managementService));

        //Confirm the batch is not finished
        ProcessMigrationBatch migrationBatch = processInstanceMigrationService.getProcessMigrationBatchById(migrationBatchId);
        assertThat(migrationBatch).extracting(ProcessMigrationBatch::isCompleted).isEqualTo(false);
        List<String> migrationResults = processInstanceMigrationService.getResultsOfBatchProcessInstanceMigration(migrationBatchId);
        assertThat(migrationResults).isNull();

        //Start async executor to process the batches
        executeJobExecutorForTime(1000L, 500L);
        assertFalse(JobTestHelper.areJobsAvailable(managementService));

        //Confirm the batches have ended
        migrationBatch = processInstanceMigrationService.getProcessMigrationBatchById(migrationBatchId);
        assertThat(migrationBatch).extracting(ProcessMigrationBatch::isCompleted).isEqualTo(true);
        migrationResults = processInstanceMigrationService.getResultsOfBatchProcessInstanceMigration(migrationBatchId);
        assertThat(migrationResults).isNotNull();
        assertThat(migrationResults).size().isEqualTo(2);

        List<JsonNode> jsonDocuments = migrationResults.stream()
            .map(this::readTreeNoException)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        assertThat(jsonDocuments).extracting(jsonValueExtractor(ProcessInstanceMigrationJobHandler.RESULT_LABEL_PROCESS_INSTANCE_ID))
            .containsExactlyInAnyOrder(processInstance1.getId(), processInstance2.getId());

        assertThat(jsonDocuments).extracting(jsonValueExtractor(ProcessInstanceMigrationJobHandler.RESULT_LABEL_MIGRATION_PROCESS))
            .containsOnly(ProcessInstanceMigrationJobHandler.RESULT_VALUE_FAILED);

        assertThat(jsonDocuments).extracting(jsonValueExtractor(ProcessInstanceMigrationJobHandler.RESULT_LABEL_CAUSE, "No Error"))
            .containsExactlyInAnyOrder("Migration Activity mapping missing for activity definition Id:'userTask2Id' or its MI Parent",
                "Migration Activity mapping missing for activity definition Id:'userTask2Id' or its MI Parent");

        //Confirm no migration happened
        task = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask2Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(version1ProcessDef.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance2.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask2Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(version1ProcessDef.getId());

        completeProcessInstanceTasks(processInstance1.getId());
        completeProcessInstanceTasks(processInstance2.getId());
        assertProcessEnded(processInstance1.getId());
        assertProcessEnded(processInstance2.getId());

        processInstanceMigrationService.deleteBatchAndResourcesById(validationBatchId);
        processInstanceMigrationService.deleteBatchAndResourcesById(migrationBatchId);
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
        String validationBatchId = processInstanceMigrationBuilder.batchValidateMigrationOfProcessInstances(version1ProcessDef.getId());
        assertTrue(JobTestHelper.areJobsAvailable(managementService));

        //Confirm the batch is not finished
        ProcessMigrationBatch validationBatch = processInstanceMigrationService.getProcessMigrationBatchById(validationBatchId);
        assertThat(validationBatch).extracting(ProcessMigrationBatch::isCompleted).isEqualTo(false);
        List<ProcessInstanceMigrationValidationResult> validationResults = processInstanceMigrationService.getResultsOfBatchProcessInstanceMigrationValidation(validationBatchId);
        assertThat(validationResults).isNull();

        //Start async executor to process the batches
        executeJobExecutorForTime(1000L, 500L);
        assertFalse(JobTestHelper.areJobsAvailable(managementService));

        //Confirm the batches have ended
        validationBatch = processInstanceMigrationService.getProcessMigrationBatchById(validationBatchId);
        assertThat(validationBatch).extracting(ProcessMigrationBatch::isCompleted).isEqualTo(true);
        validationResults = processInstanceMigrationService.getResultsOfBatchProcessInstanceMigrationValidation(validationBatchId);
        assertThat(validationResults).isNotNull();
        assertThat(validationResults).size().isEqualTo(1);
        assertThat(validationResults).extracting(ProcessInstanceMigrationValidationResult::getProcessInstanceId)
            .containsExactlyInAnyOrder(processInstance1.getId());
        assertThat(validationResults).extracting(ProcessInstanceMigrationValidationResult::getValidationMessages)
            .containsExactlyInAnyOrder(Collections.singletonList("Process instance (id:'" + processInstance1.getId() + "') has a running Activity (id:'userTask2Id') that is not mapped for migration (Or its Multi-Instance parent)"));

        //Migrate the process
        //Try batch migrate the process instances
        String migrationBatchId = processInstanceMigrationBuilder.batchMigrateProcessInstances(version1ProcessDef.getId());
        assertTrue(JobTestHelper.areJobsAvailable(managementService));

        //Confirm the batch is not finished
        ProcessMigrationBatch migrationBatch = processInstanceMigrationService.getProcessMigrationBatchById(migrationBatchId);
        assertThat(migrationBatch).extracting(ProcessMigrationBatch::isCompleted).isEqualTo(false);
        List<String> migrationResults = processInstanceMigrationService.getResultsOfBatchProcessInstanceMigration(migrationBatchId);
        assertThat(migrationResults).isNull();

        //Start async executor to process the batches
        executeJobExecutorForTime(1000L, 500L);
        assertFalse(JobTestHelper.areJobsAvailable(managementService));

        //Confirm the batches have ended
        migrationBatch = processInstanceMigrationService.getProcessMigrationBatchById(migrationBatchId);
        assertThat(migrationBatch).extracting(ProcessMigrationBatch::isCompleted).isEqualTo(true);
        migrationResults = processInstanceMigrationService.getResultsOfBatchProcessInstanceMigration(migrationBatchId);
        assertThat(migrationResults).isNotNull();
        assertThat(migrationResults).size().isEqualTo(2);

        List<JsonNode> jsonDocuments = migrationResults.stream()
            .map(this::readTreeNoException)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        assertThat(jsonDocuments).extracting(jsonValueExtractor(ProcessInstanceMigrationJobHandler.RESULT_LABEL_PROCESS_INSTANCE_ID))
            .containsExactlyInAnyOrder(processInstance1.getId(), processInstance2.getId());

        assertThat(jsonDocuments).extracting(jsonValueExtractor(ProcessInstanceMigrationJobHandler.RESULT_LABEL_MIGRATION_PROCESS))
            .containsExactlyInAnyOrder(ProcessInstanceMigrationJobHandler.RESULT_VALUE_FAILED, ProcessInstanceMigrationJobHandler.RESULT_VALUE_SUCCESSFUL);

        assertThat(jsonDocuments).extracting(jsonValueExtractor(ProcessInstanceMigrationJobHandler.RESULT_LABEL_CAUSE, "No Error"))
            .containsExactlyInAnyOrder("Migration Activity mapping missing for activity definition Id:'userTask2Id' or its MI Parent", "No Error");

        //Confirm the migration
        task = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask2Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(version1ProcessDef.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance2.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        //This task migrated
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(version2ProcessDef.getId());

        completeProcessInstanceTasks(processInstance1.getId());
        completeProcessInstanceTasks(processInstance2.getId());
        assertProcessEnded(processInstance1.getId());
        assertProcessEnded(processInstance2.getId());

        processInstanceMigrationService.deleteBatchAndResourcesById(validationBatchId);
        processInstanceMigrationService.deleteBatchAndResourcesById(migrationBatchId);
    }

    private Extractor<JsonNode, String> jsonValueExtractor(String property) {
        return jsonValueExtractor(property, null);
    }

    private Extractor<JsonNode, String> jsonValueExtractor(String property, String defaultValue) {
        return jsonNode -> jsonNode.has(property) ? jsonNode.get(property).asText() : defaultValue;
    }

    private JsonNode readTreeNoException(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (IOException e) {
            return null;
        }
    }

}
