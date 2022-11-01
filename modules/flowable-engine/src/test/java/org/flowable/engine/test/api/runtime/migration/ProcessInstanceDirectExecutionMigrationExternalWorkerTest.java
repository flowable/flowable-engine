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
import static org.assertj.core.api.Assertions.tuple;

import java.time.Duration;
import java.util.List;

import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.test.JobTestHelper;
import org.flowable.engine.migration.ProcessInstanceMigrationBuilder;
import org.flowable.engine.migration.ProcessInstanceMigrationValidationResult;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.api.runtime.changestate.ChangeStateEventListener;
import org.flowable.job.api.AcquiredExternalWorkerJob;
import org.flowable.job.api.ExternalWorkerJob;
import org.flowable.job.api.Job;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Bas Claessen
 */
public class ProcessInstanceDirectExecutionMigrationExternalWorkerTest extends AbstractProcessInstanceMigrationTest {

    private final ChangeStateEventListener changeStateEventListener = new ChangeStateEventListener();

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
                "org/flowable/engine/test/api/runtime/migration/one-external-worker-simple-process.bpmn20.xml");

        //Start an instance of the first version of the process for migration
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("MP");

        //Deploy second version of the same process
        ProcessDefinition version2ProcessDef = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-external-worker-simple-process.bpmn20.xml");
        assertThat(version1ProcessDef.getId()).isNotEqualTo(version2ProcessDef.getId());

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

        List<ExternalWorkerJob> externalWorkerJobs = managementService.createExternalWorkerJobQuery()
                .list();
        assertThat(externalWorkerJobs)
                .extracting(ExternalWorkerJob::getProcessDefinitionId, ExternalWorkerJob::getElementId, ExternalWorkerJob::getJobHandlerConfiguration)
                .containsExactly(tuple(version1ProcessDef.getId(), "externalWorker1Id", "topic1"));

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

        List<ExternalWorkerJob> migratedExternalWorkerJobs = managementService.createExternalWorkerJobQuery()
                .list();
        assertThat(migratedExternalWorkerJobs)
                .extracting(ExternalWorkerJob::getProcessDefinitionId, ExternalWorkerJob::getElementId)
                .containsExactly(tuple(version2ProcessDef.getId(), "externalWorker1Id"));

        //Acquire the external worker job
        List<AcquiredExternalWorkerJob> acquiredExternalWorkerJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("topic1", Duration.ofHours(1))
                .acquireAndLock(1, "workerId");
        assertThat(acquiredExternalWorkerJobs)
                .extracting(ExternalWorkerJob::getProcessDefinitionId, ExternalWorkerJob::getElementId)
                .containsExactly(tuple(version2ProcessDef.getId(), "externalWorker1Id"));

        //Complete the external worker job
        managementService.createExternalWorkerCompletionBuilder(acquiredExternalWorkerJobs.get(0).getId(), "workerId")
                .complete();

        // Start the async executor to process the completion of the external worker job
        JobTestHelper.waitForJobExecutorToProcessAllJobs(processEngineConfiguration, managementService, 1000L, 500L, true);
        assertThat(JobTestHelper.areJobsAvailable(managementService)).isFalse();

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testAcquiredBeforeMigration() {
        //Deploy first version of the process
        ProcessDefinition version1ProcessDef = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-external-worker-simple-process.bpmn20.xml");

        //Start an instance of the first version of the process for migration
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("MP");

        //Deploy second version of the same process
        ProcessDefinition version2ProcessDef = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-external-worker-simple-process.bpmn20.xml");
        assertThat(version1ProcessDef.getId()).isNotEqualTo(version2ProcessDef.getId());

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

        List<ExternalWorkerJob> externalWorkerJobs = managementService.createExternalWorkerJobQuery()
                .list();
        assertThat(externalWorkerJobs)
                .extracting(ExternalWorkerJob::getProcessDefinitionId, ExternalWorkerJob::getElementId, ExternalWorkerJob::getJobHandlerConfiguration)
                .containsExactly(tuple(version1ProcessDef.getId(), "externalWorker1Id", "topic1"));

        ProcessInstanceMigrationValidationResult validationResult = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(version2ProcessDef.getId())
                .validateMigration(processInstance.getId());
        assertThat(validationResult.isMigrationValid()).isTrue();

        //Acquire the external worker job
        List<AcquiredExternalWorkerJob> acquiredExternalWorkerJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("topic1", Duration.ofHours(1))
                .acquireAndLock(1, "workerId");
        assertThat(acquiredExternalWorkerJobs)
                .extracting(ExternalWorkerJob::getProcessDefinitionId, ExternalWorkerJob::getElementId)
                .containsExactly(tuple(version1ProcessDef.getId(), "externalWorker1Id"));

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

        List<ExternalWorkerJob> migratedExternalWorkerJobs = managementService.createExternalWorkerJobQuery()
                .list();
        assertThat(migratedExternalWorkerJobs)
                .extracting(ExternalWorkerJob::getProcessDefinitionId, ExternalWorkerJob::getElementId, ExternalWorkerJob::getJobHandlerConfiguration)
                .containsExactly(tuple(version2ProcessDef.getId(), "externalWorker1Id", "topic1"));

        //Complete the external worker job
        managementService.createExternalWorkerCompletionBuilder(acquiredExternalWorkerJobs.get(0).getId(), "workerId")
                .complete();

        // Start the async executor to process the completion of the external worker job
        JobTestHelper.waitForJobExecutorToProcessAllJobs(processEngineConfiguration, managementService, 1000L, 500L, true);
        assertThat(JobTestHelper.areJobsAvailable(managementService)).isFalse();

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testSuspendedBeforeMigration() {
        //Deploy first version of the process
        ProcessDefinition version1ProcessDef = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-external-worker-simple-process.bpmn20.xml");

        //Start an instance of the first version of the process for migration
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("MP");

        //Deploy second version of the same process
        ProcessDefinition version2ProcessDef = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-external-worker-simple-process.bpmn20.xml");
        assertThat(version1ProcessDef.getId()).isNotEqualTo(version2ProcessDef.getId());

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

        List<ExternalWorkerJob> externalWorkerJobs = managementService.createExternalWorkerJobQuery()
                .list();
        assertThat(externalWorkerJobs)
                .extracting(ExternalWorkerJob::getProcessDefinitionId, ExternalWorkerJob::getElementId, ExternalWorkerJob::getJobHandlerConfiguration)
                .containsExactly(tuple(version1ProcessDef.getId(), "externalWorker1Id", "topic1"));

        ProcessInstanceMigrationValidationResult validationResult = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(version2ProcessDef.getId())
                .validateMigration(processInstance.getId());
        assertThat(validationResult.isMigrationValid()).isTrue();

        //Suspend the process instance
        runtimeService.suspendProcessInstanceById(processInstance.getId());

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

        List<Job> migratedSuspendedJobs = managementService.createSuspendedJobQuery()
                .list();
        assertThat(migratedSuspendedJobs)
                .extracting(Job::getProcessDefinitionId, Job::getElementId, Job::getJobHandlerConfiguration)
                .containsExactly(tuple(version2ProcessDef.getId(), "externalWorker1Id", "topic1"));

        //Activate the process instance
        runtimeService.activateProcessInstanceById(processInstance.getId());

        //Acquire the external worker job
        List<AcquiredExternalWorkerJob> acquiredExternalWorkerJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("topic1", Duration.ofHours(1))
                .acquireAndLock(1, "workerId");
        assertThat(acquiredExternalWorkerJobs)
                .extracting(ExternalWorkerJob::getProcessDefinitionId, ExternalWorkerJob::getElementId)
                .containsExactly(tuple(version2ProcessDef.getId(), "externalWorker1Id"));

        //Complete the external worker job
        managementService.createExternalWorkerCompletionBuilder(acquiredExternalWorkerJobs.get(0).getId(), "workerId")
                .complete();

        // Start the async executor to process the completion of the external worker job
        JobTestHelper.waitForJobExecutorToProcessAllJobs(processEngineConfiguration, managementService, 1000L, 500L, true);
        assertThat(JobTestHelper.areJobsAvailable(managementService)).isFalse();

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testDeadLetterBeforeMigration() {
        //Deploy first version of the process
        ProcessDefinition version1ProcessDef = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-external-worker-simple-process.bpmn20.xml");

        //Start an instance of the first version of the process for migration
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("MP");

        //Deploy second version of the same process
        ProcessDefinition version2ProcessDef = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-external-worker-simple-process.bpmn20.xml");
        assertThat(version1ProcessDef.getId()).isNotEqualTo(version2ProcessDef.getId());

        List<ExternalWorkerJob> externalWorkerJobs = managementService.createExternalWorkerJobQuery()
                .list();
        assertThat(externalWorkerJobs)
                .extracting(ExternalWorkerJob::getProcessDefinitionId, ExternalWorkerJob::getElementId)
                .containsExactly(tuple(version1ProcessDef.getId(), "externalWorker1Id"));

        //Acquire the external worker job
        List<AcquiredExternalWorkerJob> acquiredExternalWorkerJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("topic1", Duration.ofHours(1))
                .acquireAndLock(1, "workerId");
        assertThat(acquiredExternalWorkerJobs)
                .extracting(ExternalWorkerJob::getProcessDefinitionId, ExternalWorkerJob::getElementId)
                .containsExactly(tuple(version1ProcessDef.getId(), "externalWorker1Id"));

        //Fail the external worker job
        managementService.createExternalWorkerJobFailureBuilder(acquiredExternalWorkerJobs.get(0).getId(), "workerId")
                .errorDetails("errorDetails")
                .retries(0)
                .fail();

        List<Job> deadLetterJobs = managementService.createDeadLetterJobQuery()
                .jobId(acquiredExternalWorkerJobs.get(0).getId())
                .list();
        assertThat(deadLetterJobs)
                .extracting(Job::getProcessDefinitionId, Job::getElementId)
                .containsExactly(tuple(version1ProcessDef.getId(), "externalWorker1Id"));

        // Migrate process
        processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(version2ProcessDef.getId())
                .migrate(processInstance.getId());

        List<Execution> executions = runtimeService.createExecutionQuery().list();
        assertThat(executions).hasSize(2); //includes root execution
        executions.stream()
                .map(e -> (ExecutionEntity) e)
                .forEach(e -> assertThat(e.getProcessDefinitionId()).isEqualTo(version2ProcessDef.getId()));

        List<Job> migratedDeadLetterJobs = managementService.createDeadLetterJobQuery()
                .list();
        assertThat(migratedDeadLetterJobs)
                .extracting(Job::getProcessDefinitionId, Job::getElementId)
                .containsExactly(tuple(version2ProcessDef.getId(), "externalWorker1Id"));

        managementService.moveDeadLetterJobToExecutableJob(migratedDeadLetterJobs.get(0).getId(), 0);

        //Acquire the external worker job again
        acquiredExternalWorkerJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("topic1", Duration.ofHours(1))
                .acquireAndLock(1, "workerId");
        assertThat(acquiredExternalWorkerJobs)
                .extracting(ExternalWorkerJob::getProcessDefinitionId, ExternalWorkerJob::getElementId)
                .containsExactly(tuple(version2ProcessDef.getId(), "externalWorker1Id"));

        //Complete the external worker job
        managementService.createExternalWorkerCompletionBuilder(acquiredExternalWorkerJobs.get(0).getId(), "workerId")
                .complete();

        // Start the async executor to process the completion of the external worker job
        JobTestHelper.waitForJobExecutorToProcessAllJobs(processEngineConfiguration, managementService, 1000L, 500L, true);
        assertThat(JobTestHelper.areJobsAvailable(managementService)).isFalse();

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testRetryBeforeMigration() {
        //Deploy first version of the process
        ProcessDefinition version1ProcessDef = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-external-worker-simple-process.bpmn20.xml");

        //Start an instance of the first version of the process for migration
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("MP");

        //Deploy second version of the same process
        ProcessDefinition version2ProcessDef = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/one-external-worker-simple-process.bpmn20.xml");
        assertThat(version1ProcessDef.getId()).isNotEqualTo(version2ProcessDef.getId());

        List<ExternalWorkerJob> externalWorkerJobs = managementService.createExternalWorkerJobQuery()
                .list();
        assertThat(externalWorkerJobs)
                .extracting(ExternalWorkerJob::getProcessDefinitionId, ExternalWorkerJob::getElementId)
                .containsExactly(tuple(version1ProcessDef.getId(), "externalWorker1Id"));

        //Acquire the external worker job
        List<AcquiredExternalWorkerJob> acquiredExternalWorkerJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("topic1", Duration.ofHours(1))
                .acquireAndLock(1, "workerId");
        assertThat(acquiredExternalWorkerJobs)
                .extracting(ExternalWorkerJob::getProcessDefinitionId, ExternalWorkerJob::getElementId)
                .containsExactly(tuple(version1ProcessDef.getId(), "externalWorker1Id"));

        //Fail the external worker job with a retry
        managementService.createExternalWorkerJobFailureBuilder(acquiredExternalWorkerJobs.get(0).getId(), "workerId")
                .errorDetails("errorDetails")
                .retries(1)
                .fail();

        externalWorkerJobs = managementService.createExternalWorkerJobQuery()
                .list();
        assertThat(externalWorkerJobs)
                .extracting(ExternalWorkerJob::getProcessDefinitionId, ExternalWorkerJob::getElementId, ExternalWorkerJob::getRetries)
                .containsExactly(tuple(version1ProcessDef.getId(), "externalWorker1Id", 1));

        // Migrate process
        processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(version2ProcessDef.getId())
                .migrate(processInstance.getId());

        List<Execution> executions = runtimeService.createExecutionQuery().list();
        assertThat(executions).hasSize(2); //includes root execution
        executions.stream()
                .map(e -> (ExecutionEntity) e)
                .forEach(e -> assertThat(e.getProcessDefinitionId()).isEqualTo(version2ProcessDef.getId()));

        List<ExternalWorkerJob> migratedExternalWorkerJobs = managementService.createExternalWorkerJobQuery()
                .list();
        assertThat(migratedExternalWorkerJobs)
                .extracting(Job::getProcessDefinitionId, Job::getElementId)
                .containsExactly(tuple(version2ProcessDef.getId(), "externalWorker1Id"));

        //Acquire the external worker job again
        acquiredExternalWorkerJobs = managementService.createExternalWorkerJobAcquireBuilder()
                .topic("topic1", Duration.ofHours(1))
                .acquireAndLock(1, "workerId");
        assertThat(acquiredExternalWorkerJobs)
                .extracting(ExternalWorkerJob::getProcessDefinitionId, ExternalWorkerJob::getElementId)
                .containsExactly(tuple(version2ProcessDef.getId(), "externalWorker1Id"));

        //Complete the external worker job
        managementService.createExternalWorkerCompletionBuilder(acquiredExternalWorkerJobs.get(0).getId(), "workerId")
                .complete();

        // Start the async executor to process the completion of the external worker job
        JobTestHelper.waitForJobExecutorToProcessAllJobs(processEngineConfiguration, managementService, 1000L, 500L, true);
        assertThat(JobTestHelper.areJobsAvailable(managementService)).isFalse();

        assertProcessEnded(processInstance.getId());
    }

}
