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
package org.flowable.cmmn.test.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Comparator;
import java.util.List;

import org.flowable.batch.api.Batch;
import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.migration.CaseInstanceBatchMigrationPartResult;
import org.flowable.cmmn.api.migration.CaseInstanceBatchMigrationResult;
import org.flowable.cmmn.api.migration.CaseInstanceMigrationDocumentBuilder;
import org.flowable.cmmn.api.migration.PlanItemDefinitionMappingBuilder;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.impl.job.CaseInstanceMigrationStatusJobHandler;
import org.flowable.cmmn.engine.impl.migration.CaseInstanceMigrationDocumentBuilderImpl;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.cmmn.engine.test.impl.CmmnJobTestHelper;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.Test;

public class CaseInstanceMigrationBatchTest extends AbstractCaseMigrationTest {

    @Test
    void testCaseInstanceBatchMigrationSuccess() {
        // GIVEN
        CaseDefinition sourceCaseDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task.cmmn.xml");
        CaseInstance caseInstance1 = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseInstance caseInstance2 = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/two-task.cmmn.xml");

        CaseInstanceMigrationDocumentBuilder migrationDoc = new CaseInstanceMigrationDocumentBuilderImpl()
                .setCaseDefinitionToMigrateTo(destinationDefinition.getId())
                .addActivatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createActivatePlanItemDefinitionMappingFor("humanTask2"));

        Batch batch = cmmnMigrationService.createCaseInstanceMigrationBuilderFromCaseInstanceMigrationDocument(migrationDoc.build())
                .batchMigrateCaseInstances(sourceCaseDefinition.getId());

        assertThat(CmmnJobTestHelper.areJobsAvailable(cmmnManagementService)).isTrue();

        CaseInstanceBatchMigrationResult migrationResultPriorProcessing = cmmnMigrationService.getResultsOfBatchCaseInstanceMigration(batch.getId());

        // assert created migration result and parts
        assertThat(migrationResultPriorProcessing).isNotNull();
        assertThat(migrationResultPriorProcessing.getBatchId()).isEqualTo(batch.getId());
        assertThat(migrationResultPriorProcessing.getStatus()).isEqualTo(CaseInstanceBatchMigrationResult.STATUS_IN_PROGRESS);
        assertThat(migrationResultPriorProcessing.getCompleteTime()).isNull();
        assertThat(migrationResultPriorProcessing.getAllMigrationParts()).hasSize(2);
        assertThat(migrationResultPriorProcessing.getWaitingMigrationParts()).hasSize(2);
        assertThat(migrationResultPriorProcessing.getSuccessfulMigrationParts()).isEmpty();
        assertThat(migrationResultPriorProcessing.getFailedMigrationParts()).isEmpty();

        for (CaseInstanceBatchMigrationPartResult part : migrationResultPriorProcessing.getAllMigrationParts()) {
            assertThat(part.getStatus()).isEqualTo(CaseInstanceBatchMigrationResult.STATUS_WAITING);
            assertThat(part.getResult()).isEqualTo(CaseInstanceBatchMigrationResult.STATUS_WAITING);
        }

        // WHEN
        // Start async executor to process the batches
        CmmnJobTestHelper.waitForJobExecutorToProcessAllAsyncJobs(cmmnEngineConfiguration, 5000L, 500L, true);
        assertThat(CmmnJobTestHelper.areJobsAvailable(cmmnManagementService)).isFalse();
        executeMigrationJobStatusHandlerTimerJob();

        // THEN
        CaseInstanceBatchMigrationResult migrationResult = cmmnMigrationService.getResultsOfBatchCaseInstanceMigration(batch.getId());
        assertThat(migrationResult).isNotNull();
        assertThat(migrationResult.getBatchId()).isEqualTo(batch.getId());
        assertThat(migrationResult.getStatus()).isEqualTo(CaseInstanceBatchMigrationResult.STATUS_COMPLETED);

        CaseInstance caseInstance1AfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance1.getId())
                .singleResult();
        CaseInstance caseInstance2AfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance1.getId())
                .singleResult();

        for (CaseInstanceBatchMigrationPartResult part : migrationResult.getAllMigrationParts()) {
            assertThat(part.getStatus()).isEqualTo(CaseInstanceBatchMigrationResult.STATUS_COMPLETED);
            assertThat(part.getStatus()).isEqualTo(CaseInstanceBatchMigrationResult.STATUS_COMPLETED);
            assertThat(part.getResult()).isEqualTo(CaseInstanceBatchMigrationResult.RESULT_SUCCESS);
            assertThat(part.getResult()).isEqualTo(CaseInstanceBatchMigrationResult.RESULT_SUCCESS);
        }

        assertAfterMigrationState(caseInstance1, destinationDefinition, caseInstance1AfterMigration, 2);
        assertAfterMigrationState(caseInstance2, destinationDefinition, caseInstance2AfterMigration, 2);

        cmmnManagementService.deleteBatch(batch.getId());
    }

    @Test
    void testCaseInstanceBatchMigrationWithError() {
        // GIVEN
        CaseDefinition caseDefinitionVersion1 = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/two-task.cmmn.xml");
        CaseInstance caseInstance1 = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseInstance caseInstance2 = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();

        CmmnDeployment deployment = cmmnRepositoryService.createDeployment()
                .name("test1")
                // Other tenant, migration throws an exception.
                .tenantId("otherTenant")
                .addClasspathResource("org/flowable/cmmn/test/migration/stage-linked-with-sentry.cmmn.xml")
                .deploy();
        CaseDefinition caseDefinitionVersion2 = cmmnRepositoryService.createCaseDefinitionQuery()
                .deploymentId(deployment.getId())
                .singleResult();

        List<CaseDefinition> caseDefinitions = cmmnRepositoryService.createCaseDefinitionQuery()
                .caseDefinitionKey("testCase")
                .list();

        assertThat(caseDefinitions).hasSize(2);
        caseDefinitions.sort(Comparator.comparingInt(CaseDefinition::getVersion));

        assertThat(caseDefinitionVersion1.getId()).isEqualTo(caseDefinitions.get(0).getId());
        assertThat(caseDefinitionVersion2.getId()).isEqualTo(caseDefinitions.get(1).getId());

        CaseInstanceMigrationDocumentBuilder migrationDoc = new CaseInstanceMigrationDocumentBuilderImpl()
                .setCaseDefinitionToMigrateTo(caseDefinitionVersion2.getId());

        Batch batch = cmmnMigrationService.createCaseInstanceMigrationBuilderFromCaseInstanceMigrationDocument(migrationDoc.build())
                .batchMigrateCaseInstances(caseDefinitionVersion1.getId());

        // assert created migration result and parts
        assertThat(CmmnJobTestHelper.areJobsAvailable(cmmnManagementService)).isTrue();
        CaseInstanceBatchMigrationResult migrationResultPriorProcessing = cmmnMigrationService.getResultsOfBatchCaseInstanceMigration(batch.getId());
        assertThat(migrationResultPriorProcessing).isNotNull();
        assertThat(migrationResultPriorProcessing.getBatchId()).isEqualTo(batch.getId());
        assertThat(migrationResultPriorProcessing.getStatus()).isEqualTo(CaseInstanceBatchMigrationResult.STATUS_IN_PROGRESS);
        assertThat(migrationResultPriorProcessing.getCompleteTime()).isNull();
        assertThat(migrationResultPriorProcessing.getAllMigrationParts()).hasSize(2);
        assertThat(migrationResultPriorProcessing.getWaitingMigrationParts()).hasSize(2);
        assertThat(migrationResultPriorProcessing.getSuccessfulMigrationParts()).isEmpty();
        assertThat(migrationResultPriorProcessing.getFailedMigrationParts()).isEmpty();

        // WHEN
        // Start async executor to process the batches
        CmmnJobTestHelper.waitForJobExecutorToProcessAllAsyncJobs(cmmnEngineConfiguration, 5000L, 500L, true);
        assertThat(CmmnJobTestHelper.areJobsAvailable(cmmnManagementService)).isFalse();

        CaseInstance caseInstance1AfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance1.getId())
                .singleResult();
        CaseInstance caseInstance2AfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance1.getId())
                .singleResult();

        executeMigrationJobStatusHandlerTimerJob();

        // THEN
        CaseInstanceBatchMigrationResult migrationResult = cmmnMigrationService.getResultsOfBatchCaseInstanceMigration(batch.getId());
        assertThat(migrationResult.getBatchId()).isEqualTo(batch.getId());
        assertThat(migrationResult.getStatus()).isEqualTo(CaseInstanceBatchMigrationResult.STATUS_COMPLETED);
        assertThat(migrationResult.getCompleteTime()).isNotNull();
        assertThat(migrationResult.getAllMigrationParts()).hasSize(2);
        assertThat(migrationResult.getWaitingMigrationParts()).isEmpty();
        assertThat(migrationResult.getSuccessfulMigrationParts()).hasSize(0);
        assertThat(migrationResult.getFailedMigrationParts()).hasSize(2);

        for (CaseInstanceBatchMigrationPartResult part : migrationResult.getAllMigrationParts()) {
            assertThat(part.getStatus()).isEqualTo(CaseInstanceBatchMigrationResult.STATUS_COMPLETED);
            assertThat(part.getStatus()).isEqualTo(CaseInstanceBatchMigrationResult.STATUS_COMPLETED);
            assertThat(part.getResult()).isEqualTo(CaseInstanceBatchMigrationResult.RESULT_FAIL);
            assertThat(part.getResult()).isEqualTo(CaseInstanceBatchMigrationResult.RESULT_FAIL);
        }

        assertAfterMigrationState(caseInstance1, caseDefinitionVersion1, caseInstance1AfterMigration, 1);
        assertAfterMigrationState(caseInstance2, caseDefinitionVersion1, caseInstance2AfterMigration, 1);

        cmmnManagementService.deleteBatch(batch.getId());
    }

    void assertAfterMigrationState(CaseInstance caseInstance, CaseDefinition destinationDefinition, CaseInstance caseInstanceAfterMigration,
            int caseDefinitionVersion) {
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(caseInstanceAfterMigration.getCaseDefinitionKey()).isEqualTo("testCase");
        assertThat(caseInstanceAfterMigration.getCaseDefinitionName()).isEqualTo("Two Task Test Case");
        assertThat(caseInstanceAfterMigration.getCaseDefinitionVersion()).isEqualTo(caseDefinitionVersion);
        assertThat(caseInstanceAfterMigration.getCaseDefinitionDeploymentId()).isEqualTo(destinationDefinition.getDeploymentId());
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .list();
        assertThat(planItemInstances).hasSize(2);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Task 1", "Task 2");
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.ACTIVE);

        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).hasSize(2);
        for (Task task : tasks) {
            assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
            cmmnTaskService.complete(task.getId());
        }

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getCaseDefinitionId())
                    .isEqualTo(destinationDefinition.getId());

            List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                    .planItemInstanceCaseInstanceId(caseInstance.getId()).list();
            assertThat(historicPlanItemInstances).hasSize(2);
            for (HistoricPlanItemInstance historicPlanItemInstance : historicPlanItemInstances) {
                assertThat(historicPlanItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
            }

            List<HistoricTaskInstance> historicTasks = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).list();
            assertThat(historicTasks).hasSize(2);
            for (HistoricTaskInstance historicTask : historicTasks) {
                assertThat(historicTask.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
            }
        }
    }

    protected void executeMigrationJobStatusHandlerTimerJob() {
        List<Job> timerJobs = cmmnManagementService.createTimerJobQuery().handlerType(CaseInstanceMigrationStatusJobHandler.TYPE).list();
        for (Job timerJob : timerJobs) {
            Job executableJob = cmmnManagementService.moveTimerToExecutableJob(timerJob.getId());
            cmmnManagementService.executeJob(executableJob.getId());
        }
    }
}
