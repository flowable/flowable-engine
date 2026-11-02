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
import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.migration.CaseInstanceBatchMigrationPartResult;
import org.flowable.cmmn.api.migration.CaseInstanceBatchMigrationResult;
import org.flowable.cmmn.api.migration.HistoricCaseInstanceMigrationDocumentBuilder;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.impl.job.CaseInstanceMigrationStatusJobHandler;
import org.flowable.cmmn.engine.impl.migration.HistoricCaseInstanceMigrationDocumentBuilderImpl;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.cmmn.engine.test.impl.CmmnJobTestHelper;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.Test;

public class HistoricCaseInstanceMigrationBatchTest extends AbstractCaseMigrationTest {

    @Test
    void testHistoricCaseInstanceBatchMigrationSuccess() {
        // GIVEN
        CaseDefinition sourceCaseDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task.cmmn.xml");
        CaseInstance caseInstance1 = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseInstance caseInstance2 = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseInstance caseInstance3 = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/two-task.cmmn.xml");
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance1.getId()).singleResult();
        cmmnTaskService.complete(task.getId());
        
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance2.getId()).singleResult();
        cmmnTaskService.complete(task.getId());

        HistoricCaseInstanceMigrationDocumentBuilder migrationDoc = new HistoricCaseInstanceMigrationDocumentBuilderImpl()
                .setCaseDefinitionToMigrateTo(destinationDefinition.getId());

        Batch batch = cmmnMigrationService.createHistoricCaseInstanceMigrationBuilderFromHistoricCaseInstanceMigrationDocument(migrationDoc.build())
                .batchMigrateHistoricCaseInstances(sourceCaseDefinition.getId());

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

        HistoricCaseInstance caseInstance1AfterMigration = cmmnHistoryService.createHistoricCaseInstanceQuery()
                .caseInstanceId(caseInstance1.getId())
                .singleResult();
        HistoricCaseInstance caseInstance2AfterMigration = cmmnHistoryService.createHistoricCaseInstanceQuery()
                .caseInstanceId(caseInstance2.getId())
                .singleResult();

        for (CaseInstanceBatchMigrationPartResult part : migrationResult.getAllMigrationParts()) {
            assertThat(part.getStatus()).isEqualTo(CaseInstanceBatchMigrationResult.STATUS_COMPLETED);
            assertThat(part.getResult()).isEqualTo(CaseInstanceBatchMigrationResult.RESULT_SUCCESS);
        }

        assertAfterMigrationState(1, caseInstance1, destinationDefinition, caseInstance1AfterMigration, 2);
        assertAfterMigrationState(1, caseInstance2, destinationDefinition, caseInstance2AfterMigration, 2);
        
        HistoricCaseInstance caseInstance3AfterMigration = cmmnHistoryService.createHistoricCaseInstanceQuery()
                .caseInstanceId(caseInstance3.getId())
                .singleResult();
        assertThat(caseInstance3AfterMigration.getCaseDefinitionId()).isEqualTo(sourceCaseDefinition.getId());

        cmmnManagementService.deleteBatch(batch.getId());
    }

    @Test
    void testHistoricCaseInstanceBatchMigrationWithError() {
        // GIVEN
        CaseDefinition caseDefinitionVersion1 = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/two-task.cmmn.xml");
        CaseInstance caseInstance1 = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseInstance caseInstance2 = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseInstance caseInstance3 = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();

        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance1.getId()).list();
        for (Task task : tasks) {
            cmmnTaskService.complete(task.getId());
        }
        
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance2.getId()).list();
        for (Task task : tasks) {
            cmmnTaskService.complete(task.getId());
        }
        
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

        HistoricCaseInstanceMigrationDocumentBuilder migrationDoc = new HistoricCaseInstanceMigrationDocumentBuilderImpl()
                .setCaseDefinitionToMigrateTo(caseDefinitionVersion2.getId());

        Batch batch = cmmnMigrationService.createHistoricCaseInstanceMigrationBuilderFromHistoricCaseInstanceMigrationDocument(migrationDoc.build())
                .batchMigrateHistoricCaseInstances(caseDefinitionVersion1.getId());

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

        HistoricCaseInstance caseInstance1AfterMigration = cmmnHistoryService.createHistoricCaseInstanceQuery()
                .caseInstanceId(caseInstance1.getId())
                .singleResult();
        HistoricCaseInstance caseInstance2AfterMigration = cmmnHistoryService.createHistoricCaseInstanceQuery()
                .caseInstanceId(caseInstance2.getId())
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
            assertThat(part.getResult()).isEqualTo(CaseInstanceBatchMigrationResult.RESULT_FAIL);
            assertThat(part.getMigrationMessage()).contains("Tenant mismatch between");
            assertThat(part.getMigrationStacktrace()).contains("Tenant mismatch between");
        }
        
        assertThat(cmmnManagementService.createJobQuery().scopeId(caseInstance1.getId()).list()).hasSize(0);
        assertThat(cmmnManagementService.createTimerJobQuery().scopeId(caseInstance1.getId()).list()).hasSize(0);
        assertThat(cmmnManagementService.createDeadLetterJobQuery().scopeId(caseInstance1.getId()).list()).hasSize(0);
        
        assertThat(cmmnManagementService.createJobQuery().scopeId(caseInstance2.getId()).list()).hasSize(0);
        assertThat(cmmnManagementService.createTimerJobQuery().scopeId(caseInstance2.getId()).list()).hasSize(0);
        assertThat(cmmnManagementService.createDeadLetterJobQuery().scopeId(caseInstance2.getId()).list()).hasSize(0);

        assertAfterMigrationState(2, caseInstance1, caseDefinitionVersion1, caseInstance1AfterMigration, 1);
        assertAfterMigrationState(2, caseInstance2, caseDefinitionVersion1, caseInstance2AfterMigration, 1);

        cmmnManagementService.deleteBatch(batch.getId());
    }

    void assertAfterMigrationState(int numberOfPlanItems, CaseInstance caseInstance, CaseDefinition destinationDefinition, 
            HistoricCaseInstance caseInstanceAfterMigration, int caseDefinitionVersion) {
        
        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
            assertThat(caseInstanceAfterMigration.getCaseDefinitionKey()).isEqualTo("testCase");
            assertThat(caseInstanceAfterMigration.getCaseDefinitionName()).isEqualTo("Two Task Test Case");
            assertThat(caseInstanceAfterMigration.getCaseDefinitionVersion()).isEqualTo(caseDefinitionVersion);
            assertThat(caseInstanceAfterMigration.getCaseDefinitionDeploymentId()).isEqualTo(destinationDefinition.getDeploymentId());
        
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getCaseDefinitionId())
                    .isEqualTo(destinationDefinition.getId());

            List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                    .planItemInstanceCaseInstanceId(caseInstance.getId()).list();
            assertThat(historicPlanItemInstances).hasSize(numberOfPlanItems);
            for (HistoricPlanItemInstance historicPlanItemInstance : historicPlanItemInstances) {
                assertThat(historicPlanItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
            }

            List<HistoricTaskInstance> historicTasks = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).list();
            assertThat(historicTasks).hasSize(numberOfPlanItems);
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
