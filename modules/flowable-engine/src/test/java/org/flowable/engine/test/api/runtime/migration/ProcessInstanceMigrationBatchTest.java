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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.flowable.batch.api.Batch;
import org.flowable.batch.api.BatchPart;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.engine.impl.jobexecutor.ProcessInstanceMigrationStatusJobHandler;
import org.flowable.engine.impl.test.JobTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.migration.ProcessInstanceBatchMigrationPartResult;
import org.flowable.engine.migration.ProcessInstanceBatchMigrationResult;
import org.flowable.engine.migration.ProcessInstanceMigrationBuilder;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.api.runtime.changestate.ChangeStateEventListener;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ProcessInstanceMigrationBatchTest extends PluggableFlowableTestCase {

    private ChangeStateEventListener changeStateEventListener = new ChangeStateEventListener();

    @BeforeEach
    protected void setUp() {
        processEngine.getRuntimeService().addEventListener(changeStateEventListener);
    }

    @AfterEach
    protected void tearDown() {
        processEngine.getRuntimeService().removeEventListener(changeStateEventListener);
        List<Batch> batches = processEngine.getManagementService().getAllBatches();
        for (Batch batch : batches) {
            processEngine.getManagementService().deleteBatch(batch.getId());
        }
        deleteDeployments();
    }

    @Test
    public void testProcessMigrationBatchMissingMapping() {
        // Deploy first version of the process
        ProcessDefinition version1ProcessDef = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/two-tasks-simple-process.bpmn20.xml");

        // Start and instance of the recent first version of the process for migration and one for reference
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("MP");
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("MP");

        Task task = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).singleResult();
        completeTask(task);
        task = taskService.createTaskQuery().processInstanceId(processInstance2.getId()).singleResult();
        completeTask(task);

        task = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("userTask2Id");
        assertThat(task.getProcessDefinitionId()).isEqualTo(version1ProcessDef.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance2.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("userTask2Id");
        assertThat(task.getProcessDefinitionId()).isEqualTo(version1ProcessDef.getId());

        //Deploy second version of the process
        ProcessDefinition version2ProcessDef = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");

        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey("MP")
                .list();

        processDefinitions.sort(Comparator.comparingInt(ProcessDefinition::getVersion));
        assertThat(processDefinitions)
                .extracting(ProcessDefinition::getId)
                .containsExactly(version1ProcessDef.getId(), version2ProcessDef.getId());

        // Prepare the process Instance migration builder as usual
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(version2ProcessDef.getId());

        // Try batch migrate the process instances
        Batch migrationBatch = processInstanceMigrationBuilder.batchMigrateProcessInstances(version1ProcessDef.getId());
        assertThat(JobTestHelper.areJobsAvailable(managementService)).isTrue();

        // Confirm the batch is not finished
        ProcessInstanceBatchMigrationResult migrationResult = processMigrationService.getResultsOfBatchProcessInstanceMigration(migrationBatch.getId());

        // Partial Results
        assertThat(migrationResult).isNotNull();
        assertThat(migrationResult.getBatchId()).isEqualTo(migrationBatch.getId());
        assertThat(migrationResult.getStatus()).isEqualTo(ProcessInstanceBatchMigrationResult.STATUS_IN_PROGRESS);
        assertThat(migrationResult.getCompleteTime()).isNull();
        assertThat(migrationResult.getAllMigrationParts()).hasSize(2);
        assertThat(migrationResult.getWaitingMigrationParts()).hasSize(2);
        assertThat(migrationResult.getSuccessfulMigrationParts()).isEmpty();
        assertThat(migrationResult.getFailedMigrationParts()).isEmpty();

        for (ProcessInstanceBatchMigrationPartResult part : migrationResult.getAllMigrationParts()) {
            assertThat(part.getStatus()).isEqualTo(ProcessInstanceBatchMigrationResult.STATUS_WAITING);
            assertThat(part.getResult()).isEqualTo(ProcessInstanceBatchMigrationResult.STATUS_WAITING);
        }

        // Start async executor to process the batches
        JobTestHelper.waitForJobExecutorToProcessAllJobs(processEngineConfiguration, managementService, 1000L, 500L, true);
        assertThat(JobTestHelper.areJobsAvailable(managementService)).isFalse();

        List<Job> timerJobs = managementService.createTimerJobQuery().handlerType(ProcessInstanceMigrationStatusJobHandler.TYPE).list();
        for (Job timerJob : timerJobs) {
            Job executableJob = managementService.moveTimerToExecutableJob(timerJob.getId());
            managementService.executeJob(executableJob.getId());
        }

        // Confirm the batches have ended
        migrationResult = processMigrationService.getResultsOfBatchProcessInstanceMigration(migrationBatch.getId());
        assertThat(migrationResult).isNotNull();

        assertThat(migrationResult.getBatchId()).isEqualTo(migrationBatch.getId());
        assertThat(migrationResult.getStatus()).isEqualTo(ProcessInstanceBatchMigrationResult.STATUS_COMPLETED);
        assertThat(migrationResult.getCompleteTime()).isNotNull();
        assertThat(migrationResult.getAllMigrationParts()).hasSize(2);
        assertThat(migrationResult.getWaitingMigrationParts()).isEmpty();
        assertThat(migrationResult.getSuccessfulMigrationParts()).isEmpty();
        assertThat(migrationResult.getFailedMigrationParts()).hasSize(2);

        for (ProcessInstanceBatchMigrationPartResult part : migrationResult.getAllMigrationParts()) {
            assertThat(part.getStatus()).isEqualTo(ProcessInstanceBatchMigrationResult.STATUS_COMPLETED);
            assertThat(part.getResult()).isEqualTo(ProcessInstanceBatchMigrationResult.RESULT_FAIL);
            assertThat(part.getMigrationMessage()).isEqualTo("Migration Activity mapping missing for activity definition Id:'userTask2Id' or its MI Parent");
        }

        // Confirm no migration happened
        task = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("userTask2Id");
        assertThat(task.getProcessDefinitionId()).isEqualTo(version1ProcessDef.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance2.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("userTask2Id");
        assertThat(task.getProcessDefinitionId()).isEqualTo(version1ProcessDef.getId());

        completeProcessInstanceTasks(processInstance1.getId());
        completeProcessInstanceTasks(processInstance2.getId());
        assertProcessEnded(processInstance1.getId());
        assertProcessEnded(processInstance2.getId());

        managementService.deleteBatch(migrationBatch.getId());
    }

    @Test
    public void testProcessMigrationBatchPartialMissingMapping() {
        // Deploy first version of the process
        ProcessDefinition version1ProcessDef = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/two-tasks-simple-process.bpmn20.xml");

        //Start and instance of the recent first version of the process for migration and one for reference
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("MP");
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("MP");

        Task task = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).singleResult();
        completeTask(task);

        task = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("userTask2Id");
        assertThat(task.getProcessDefinitionId()).isEqualTo(version1ProcessDef.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance2.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("userTask1Id");
        assertThat(task.getProcessDefinitionId()).isEqualTo(version1ProcessDef.getId());

        // Deploy second version of the process
        ProcessDefinition version2ProcessDef = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");

        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey("MP")
                .list();

        assertThat(processDefinitions).hasSize(2);
        processDefinitions.sort(Comparator.comparingInt(ProcessDefinition::getVersion));

        assertThat(version1ProcessDef.getId()).isEqualTo(processDefinitions.get(0).getId());
        assertThat(version2ProcessDef.getId()).isEqualTo(processDefinitions.get(1).getId());

        // Prepare the process Instance migration builder as usual
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(version2ProcessDef.getId());

        // Try batch migrate the process instances
        Batch migrationBatch = processInstanceMigrationBuilder.batchMigrateProcessInstances(version1ProcessDef.getId());
        assertThat(JobTestHelper.areJobsAvailable(managementService)).isTrue();

        //Confirm the batch is not finished
        ProcessInstanceBatchMigrationResult migrationResult = processMigrationService.getResultsOfBatchProcessInstanceMigration(migrationBatch.getId());

        //Partial Results
        assertThat(migrationResult).isNotNull();
        assertThat(migrationResult.getBatchId()).isEqualTo(migrationBatch.getId());
        assertThat(migrationResult.getStatus()).isEqualTo(ProcessInstanceBatchMigrationResult.STATUS_IN_PROGRESS);
        assertThat(migrationResult.getCompleteTime()).isNull();
        assertThat(migrationResult.getAllMigrationParts()).hasSize(2);
        assertThat(migrationResult.getWaitingMigrationParts()).hasSize(2);
        assertThat(migrationResult.getSuccessfulMigrationParts()).isEmpty();
        assertThat(migrationResult.getFailedMigrationParts()).isEmpty();

        for (ProcessInstanceBatchMigrationPartResult part : migrationResult.getAllMigrationParts()) {
            assertThat(part.getStatus()).isEqualTo(ProcessInstanceBatchMigrationResult.STATUS_WAITING);
            assertThat(part.getResult()).isEqualTo(ProcessInstanceBatchMigrationResult.STATUS_WAITING);
        }

        // Start async executor to process the batches
        JobTestHelper.waitForJobExecutorToProcessAllJobs(processEngineConfiguration, managementService, 5000L, 500L, true);
        assertThat(JobTestHelper.areJobsAvailable(managementService)).isFalse();

        List<Job> timerJobs = managementService.createTimerJobQuery().handlerType(ProcessInstanceMigrationStatusJobHandler.TYPE).list();
        for (Job timerJob : timerJobs) {
            Job executableJob = managementService.moveTimerToExecutableJob(timerJob.getId());
            managementService.executeJob(executableJob.getId());
        }

        migrationResult = processMigrationService.getResultsOfBatchProcessInstanceMigration(migrationBatch.getId());
        assertThat(migrationResult).isNotNull();

        assertThat(migrationResult.getBatchId()).isEqualTo(migrationBatch.getId());
        assertThat(migrationResult.getStatus()).isEqualTo(ProcessInstanceBatchMigrationResult.STATUS_COMPLETED);
        assertThat(migrationResult.getCompleteTime()).isNotNull();
        assertThat(migrationResult.getAllMigrationParts()).hasSize(2);
        assertThat(migrationResult.getWaitingMigrationParts()).isEmpty();
        assertThat(migrationResult.getSuccessfulMigrationParts()).hasSize(1);
        assertThat(migrationResult.getFailedMigrationParts()).hasSize(1);

        for (ProcessInstanceBatchMigrationPartResult part : migrationResult.getSuccessfulMigrationParts()) {
            assertThat(part.getStatus()).isEqualTo(ProcessInstanceBatchMigrationResult.STATUS_COMPLETED);
            assertThat(part.getResult()).isEqualTo(ProcessInstanceBatchMigrationResult.RESULT_SUCCESS);
            assertThat(part.getMigrationMessage()).isNull();
        }

        for (ProcessInstanceBatchMigrationPartResult part : migrationResult.getFailedMigrationParts()) {
            assertThat(part.getStatus()).isEqualTo(ProcessInstanceBatchMigrationResult.STATUS_COMPLETED);
            assertThat(part.getResult()).isEqualTo(ProcessInstanceBatchMigrationResult.RESULT_FAIL);
            assertThat(part.getMigrationMessage()).isEqualTo("Migration Activity mapping missing for activity definition Id:'userTask2Id' or its MI Parent");
        }

        // Confirm the migration
        task = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("userTask2Id");
        assertThat(task.getProcessDefinitionId()).isEqualTo(version1ProcessDef.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance2.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("userTask1Id");

        // This task migrated
        assertThat(task.getProcessDefinitionId()).isEqualTo(version2ProcessDef.getId());

        completeProcessInstanceTasks(processInstance1.getId());
        completeProcessInstanceTasks(processInstance2.getId());
        assertProcessEnded(processInstance1.getId());
        assertProcessEnded(processInstance2.getId());

        managementService.deleteBatch(migrationBatch.getId());
    }

    @Test
    public void testProcessMigrationBatchTwentyMixedSuccessAndFails() {
        // Deploy first version of the process
        ProcessDefinition version1ProcessDef = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/two-tasks-simple-process.bpmn20.xml");

        // Instances that will validate and migrate properly
        List<String> successInstances = new ArrayList<>();

        // Instances that will fail validation and migration
        List<String> failedInstances = new ArrayList<>();

        for (int i = 0; i < 12; i++) {
            successInstances.add(runtimeService.startProcessInstanceByKey("MP").getId());
        }

        for (int i = 0; i < 8; i++) {
            failedInstances.add(runtimeService.startProcessInstanceByKey("MP").getId());
        }

        List<String> allInstances = new ArrayList<>(successInstances);
        allInstances.addAll(failedInstances);

        // Set the instances to fail in a state where they won't map properly during validation and migration
        for (String processInstanceId : failedInstances) {
            Task task = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();
            taskService.complete(task.getId());
        }

        // Deploy second version of the process
        ProcessDefinition version2ProcessDef = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");

        // Prepare the process instance migration builder
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService
                .createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(version2ProcessDef.getId());

        // Migrate the processes
        Batch migrationBatch = processInstanceMigrationBuilder.batchMigrateProcessInstances(version1ProcessDef.getId());
        assertThat(JobTestHelper.areJobsAvailable(managementService)).isTrue();

        // Partial Results - migration inProgress
        ProcessInstanceBatchMigrationResult migrationResult = processMigrationService.getResultsOfBatchProcessInstanceMigration(migrationBatch.getId());
        assertThat(migrationResult).isNotNull();
        assertThat(migrationResult.getBatchId()).isEqualTo(migrationBatch.getId());
        assertThat(migrationResult.getStatus()).isEqualTo(ProcessInstanceBatchMigrationResult.STATUS_IN_PROGRESS);
        assertThat(migrationResult.getCompleteTime()).isNull();
        assertThat(migrationResult.getAllMigrationParts()).hasSize(successInstances.size() + failedInstances.size());
        assertThat(migrationResult.getWaitingMigrationParts()).hasSize(successInstances.size() + failedInstances.size());
        assertThat(migrationResult.getSuccessfulMigrationParts()).isEmpty();
        assertThat(migrationResult.getFailedMigrationParts()).isEmpty();

        // Each batch part is inProgress
        for (ProcessInstanceBatchMigrationPartResult part : migrationResult.getAllMigrationParts()) {
            assertThat(part.getStatus()).isEqualTo(ProcessInstanceBatchMigrationResult.STATUS_WAITING);
            assertThat(part.getMigrationMessage()).isNull();
        }

        // Start async executor to process the batches
        JobTestHelper.waitForJobExecutorToProcessAllJobs(processEngineConfiguration, managementService, 10000L, 500L, true);
        assertThat(JobTestHelper.areJobsAvailable(managementService)).isFalse();

        List<Job> timerJobs = managementService.createTimerJobQuery().handlerType(ProcessInstanceMigrationStatusJobHandler.TYPE).list();
        for (Job timerJob : timerJobs) {
            Job executableJob = managementService.moveTimerToExecutableJob(timerJob.getId());
            managementService.executeJob(executableJob.getId());
        }

        //Confirm the batches have ended
        migrationResult = processMigrationService.getResultsOfBatchProcessInstanceMigration(migrationBatch.getId());
        assertThat(migrationResult).isNotNull();
        assertThat(migrationResult.getBatchId()).isEqualTo(migrationBatch.getId());
        assertThat(migrationResult.getStatus()).isEqualTo(ProcessInstanceBatchMigrationResult.STATUS_COMPLETED);
        assertThat(migrationResult.getCompleteTime()).isNotNull();
        assertThat(migrationResult.getAllMigrationParts()).hasSize(successInstances.size() + failedInstances.size());
        assertThat(migrationResult.getWaitingMigrationParts()).isEmpty();
        assertThat(migrationResult.getSuccessfulMigrationParts()).hasSameSizeAs(successInstances);
        assertThat(migrationResult.getFailedMigrationParts()).hasSameSizeAs(failedInstances);

        for (ProcessInstanceBatchMigrationPartResult part : migrationResult.getSuccessfulMigrationParts()) {
            assertThat(part.getStatus()).isEqualTo(ProcessInstanceBatchMigrationResult.STATUS_COMPLETED);
            assertThat(part.getResult()).isEqualTo(ProcessInstanceBatchMigrationResult.RESULT_SUCCESS);
            assertThat(part.getMigrationMessage()).isNull();
        }

        for (ProcessInstanceBatchMigrationPartResult part : migrationResult.getFailedMigrationParts()) {
            assertThat(part.getStatus()).isEqualTo(ProcessInstanceBatchMigrationResult.STATUS_COMPLETED);
            assertThat(part.getResult()).isEqualTo(ProcessInstanceBatchMigrationResult.RESULT_FAIL);
            assertThat(part.getMigrationMessage()).isEqualTo("Migration Activity mapping missing for activity definition Id:'userTask2Id' or its MI Parent");
        }

        List<Batch> searchBatches = managementService.findBatchesBySearchKey(version1ProcessDef.getId());
        assertThat(searchBatches).hasSize(1);
        assertThat(searchBatches.get(0).getId()).isEqualTo(migrationBatch.getId());
        assertThat(searchBatches.get(0).getBatchSearchKey()).isEqualTo(version1ProcessDef.getId());
        assertThat(searchBatches.get(0).getBatchSearchKey2()).isEqualTo(version2ProcessDef.getId());
        assertThat(searchBatches.get(0).getBatchType()).isEqualTo(Batch.PROCESS_MIGRATION_TYPE);
        assertThat(searchBatches.get(0).getCreateTime()).isNotNull();

        assertThat(managementService.createBatchQuery().searchKey(version1ProcessDef.getId()).count()).isEqualTo(1);
        assertThat(managementService.createBatchQuery().searchKey(version2ProcessDef.getId()).count()).isZero();
        assertThat(managementService.createBatchQuery().searchKey2(version1ProcessDef.getId()).count()).isZero();
        assertThat(managementService.createBatchQuery().searchKey2(version2ProcessDef.getId()).count()).isEqualTo(1);
        assertThat(managementService.createBatchQuery().createTimeLowerThan(new Date()).count()).isEqualTo(1);
        assertThat(managementService.createBatchQuery().createTimeHigherThan(new Date()).count()).isZero();

        assertThat(managementService.createBatchPartQuery().batchId(migrationBatch.getId()).count()).isEqualTo(20);
        assertThat(managementService.createBatchPartQuery().batchId(migrationBatch.getId()).list()).hasSize(20);
        assertThat(managementService.createBatchPartQuery().searchKey(version1ProcessDef.getId()).count()).isEqualTo(20);
        assertThat(managementService.createBatchPartQuery().searchKey(version1ProcessDef.getId()).list()).hasSize(20);
        assertThat(managementService.createBatchPartQuery().searchKey(version2ProcessDef.getId()).count()).isZero();
        assertThat(managementService.createBatchPartQuery().searchKey(version2ProcessDef.getId()).list()).isEmpty();
        assertThat(managementService.createBatchPartQuery().searchKey2(version1ProcessDef.getId()).count()).isZero();
        assertThat(managementService.createBatchPartQuery().searchKey2(version1ProcessDef.getId()).list()).isEmpty();
        assertThat(managementService.createBatchPartQuery().searchKey2(version2ProcessDef.getId()).count()).isEqualTo(20);
        assertThat(managementService.createBatchPartQuery().searchKey2(version2ProcessDef.getId()).list()).hasSize(20);
        assertThat(managementService.createBatchPartQuery().type(Batch.PROCESS_MIGRATION_TYPE).count()).isEqualTo(20);
        assertThat(managementService.createBatchPartQuery().type(Batch.PROCESS_MIGRATION_TYPE).list()).hasSize(20);
        assertThat(managementService.createBatchPartQuery().type("unknown").count()).isZero();
        assertThat(managementService.createBatchPartQuery().type("unknown").list()).isEmpty();
        assertThat(managementService.createBatchPartQuery().batchSearchKey(version1ProcessDef.getId()).count()).isEqualTo(20);
        assertThat(managementService.createBatchPartQuery().batchSearchKey(version1ProcessDef.getId()).list()).hasSize(20);
        assertThat(managementService.createBatchPartQuery().batchSearchKey(version2ProcessDef.getId()).count()).isZero();
        assertThat(managementService.createBatchPartQuery().batchSearchKey(version2ProcessDef.getId()).list()).isEmpty();
        assertThat(managementService.createBatchPartQuery().batchSearchKey2(version1ProcessDef.getId()).count()).isZero();
        assertThat(managementService.createBatchPartQuery().batchSearchKey2(version1ProcessDef.getId()).list()).isEmpty();
        assertThat(managementService.createBatchPartQuery().batchSearchKey2(version2ProcessDef.getId()).count()).isEqualTo(20);
        assertThat(managementService.createBatchPartQuery().batchSearchKey2(version2ProcessDef.getId()).list()).hasSize(20);
        assertThat(managementService.createBatchPartQuery().batchType(Batch.PROCESS_MIGRATION_TYPE).count()).isEqualTo(20);
        assertThat(managementService.createBatchPartQuery().batchType(Batch.PROCESS_MIGRATION_TYPE).list()).hasSize(20);
        assertThat(managementService.createBatchPartQuery().batchType("unknown").count()).isZero();
        assertThat(managementService.createBatchPartQuery().batchType("unknown").list()).isEmpty();
        assertThat(managementService.createBatchPartQuery().scopeType(ScopeTypes.CMMN).count()).isZero();
        assertThat(managementService.createBatchPartQuery().scopeType(ScopeTypes.CMMN).list()).isEmpty();
        assertThat(managementService.createBatchPartQuery().scopeType(ScopeTypes.BPMN).count()).isEqualTo(20);
        assertThat(managementService.createBatchPartQuery().scopeType(ScopeTypes.BPMN).list()).hasSize(20);
        assertThat(managementService.createBatchPartQuery().scopeId(allInstances.get(0)).scopeType(ScopeTypes.BPMN).count()).isEqualTo(1);
        assertThat(managementService.createBatchPartQuery().scopeId(allInstances.get(0)).scopeType(ScopeTypes.BPMN).singleResult()).isNotNull();

        List<BatchPart> searchBatchParts = managementService.findBatchPartsByBatchId(migrationBatch.getId());
        assertThat(searchBatchParts).hasSize(20);

        searchBatchParts = managementService.createBatchPartQuery().batchId(migrationBatch.getId()).list();
        assertThat(searchBatchParts).hasSize(20);
        for (BatchPart batchPart : searchBatchParts) {
            assertThat(batchPart.getBatchId()).isEqualTo(migrationBatch.getId());
            assertThat(batchPart.getType()).isEqualTo(Batch.PROCESS_MIGRATION_TYPE);
            assertThat(batchPart.getSearchKey()).isEqualTo(version1ProcessDef.getId());
            assertThat(batchPart.getSearchKey2()).isEqualTo(version2ProcessDef.getId());
            assertThat(batchPart.getBatchSearchKey()).isEqualTo(version1ProcessDef.getId());
            assertThat(batchPart.getBatchSearchKey2()).isEqualTo(version2ProcessDef.getId());
            assertThat(batchPart.getBatchType()).isEqualTo(Batch.PROCESS_MIGRATION_TYPE);
            assertThat(batchPart.getCreateTime()).isNotNull();
            assertThat(batchPart.getCompleteTime()).isNotNull();
            assertThat(batchPart.getScopeId()).isIn(allInstances);
            assertThat(batchPart.getScopeType()).isEqualTo(ScopeTypes.BPMN);
            assertThat(batchPart.getSubScopeId()).isNull();
        }

        // Confirm the migration of successfulParts
        for (String processInstanceId : successInstances) {
            Task task = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();
            assertThat(task.getTaskDefinitionKey()).isEqualTo("userTask1Id");
            assertThat(task.getProcessDefinitionId()).isEqualTo(version2ProcessDef.getId());
        }

        // Confirm the migration of failedParts
        for (String processInstanceId : failedInstances) {
            Task task = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();
            assertThat(task.getTaskDefinitionKey()).isEqualTo("userTask2Id");
            assertThat(task.getProcessDefinitionId()).isEqualTo(version1ProcessDef.getId());
        }

        // Complete the processes
        for (String processInstanceId : successInstances) {
            completeProcessInstanceTasks(processInstanceId);
            assertProcessEnded(processInstanceId);
        }

        for (String processInstanceId : failedInstances) {
            completeProcessInstanceTasks(processInstanceId);
            assertProcessEnded(processInstanceId);
        }

        managementService.deleteBatch(migrationBatch.getId());
    }

}