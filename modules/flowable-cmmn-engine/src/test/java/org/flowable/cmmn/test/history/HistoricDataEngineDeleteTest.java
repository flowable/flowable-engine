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
package org.flowable.cmmn.test.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.flowable.batch.api.Batch;
import org.flowable.batch.api.BatchPart;
import org.flowable.batch.api.BatchService;
import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.CmmnManagementService;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.CmmnTaskService;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.delete.ComputeDeleteHistoricCaseInstanceIdsJobHandler;
import org.flowable.cmmn.engine.impl.delete.ComputeDeleteHistoricCaseInstanceStatusJobHandler;
import org.flowable.cmmn.engine.impl.delete.DeleteCaseInstanceBatchConstants;
import org.flowable.cmmn.engine.impl.delete.DeleteHistoricCaseInstanceIdsJobHandler;
import org.flowable.cmmn.engine.impl.delete.DeleteHistoricCaseInstanceIdsStatusJobHandler;
import org.flowable.cmmn.engine.impl.job.CmmnHistoryCleanupJobHandler;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.test.CmmnConfigurationResource;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTest;
import org.flowable.cmmn.engine.test.impl.CmmnJobTestHelper;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.runtime.Clock;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@FlowableCmmnTest
@CmmnConfigurationResource("flowable.historyclean.cmmn.cfg.xml")
public class HistoricDataEngineDeleteTest {

    protected Collection<String> batchesToRemove = new HashSet<>();

    @AfterEach
    void tearDown(CmmnManagementService managementService) {
        batchesToRemove.forEach(managementService::deleteBatch);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/human-task-milestone-model.cmmn")
    public void testHistoryCleanupTimerJob(CmmnEngineConfiguration cmmnEngineConfiguration, CmmnRuntimeService cmmnRuntimeService,
            CmmnHistoryService cmmnHistoryService, CmmnTaskService cmmnTaskService, CmmnManagementService cmmnManagementService) {

        try {
            Clock clock = cmmnEngineConfiguration.getClock();
            Calendar cal = clock.getCurrentCalendar();
            cal.add(Calendar.DAY_OF_YEAR, -400);
            clock.setCurrentCalendar(cal);

            List<String> caseInstanceIds = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();
                caseInstanceIds.add(caseInstance.getId());
                cmmnRuntimeService.setVariable(caseInstance.getId(), "testVar", "testValue" + (i + 1));
                cmmnRuntimeService.setVariable(caseInstance.getId(), "numVar", (i + 1));

                Batch batch = cmmnManagementService.createBatchBuilder()
                        .batchType(Batch.HISTORIC_CASE_DELETE_TYPE)
                        .status(DeleteCaseInstanceBatchConstants.STATUS_IN_PROGRESS)
                        .batchDocumentJson(i % 2 == 0 ? "Batch document" : null)
                        .searchKey(caseInstance.getId())
                        .create();

                cmmnManagementService.createBatchPartBuilder(batch)
                        .type("test")
                        .status(DeleteCaseInstanceBatchConstants.STATUS_WAITING)
                        .create();

                if (i >= 10) {
                    batchesToRemove.add(batch.getId());
                }
            }

            Batch processBatch = cmmnManagementService.createBatchBuilder()
                    .batchType(Batch.HISTORIC_PROCESS_DELETE_TYPE)
                    .status(DeleteCaseInstanceBatchConstants.STATUS_IN_PROGRESS)
                    .batchDocumentJson("Batch document")
                    .create();
            batchesToRemove.add(processBatch.getId());

            cmmnManagementService.createBatchPartBuilder(processBatch)
                    .type("test")
                    .status(DeleteCaseInstanceBatchConstants.STATUS_WAITING)
                    .create();

            cmmnEngineConfiguration.getCommandExecutor().execute(commandContext -> {
                BatchService batchService = CommandContextUtil.getCmmnEngineConfiguration(commandContext)
                        .getBatchServiceConfiguration()
                        .getBatchService();

                Batch batch = cmmnManagementService.createBatchQuery().batchId(processBatch.getId()).singleResult();
                BatchPart batchPart = cmmnManagementService.createBatchPartQuery().batchId(batch.getId()).singleResult();

                batchService.completeBatchPart(batchPart.getId(), DeleteCaseInstanceBatchConstants.STATUS_COMPLETED, "Batch part result");
                batchService.completeBatch(batch.getId(), DeleteCaseInstanceBatchConstants.STATUS_COMPLETED);

                return null;
            });

            assertThat(cmmnManagementService.createBatchQuery().count()).isEqualTo(21);
            assertThat(cmmnManagementService.createBatchPartQuery().count()).isEqualTo(21);

            if (cmmnEngineConfiguration.getHistoryLevel() != HistoryLevel.NONE) {

                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isEqualTo(20);

                for (int i = 0; i < 10; i++) {
                    String caseInstanceId = caseInstanceIds.get(i);
                    Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstanceIds.get(i)).singleResult();
                    cmmnTaskService.setVariableLocal(task.getId(), "taskVar", "taskValue" + (i + 1));
                    cmmnTaskService.complete(task.getId());

                    cmmnEngineConfiguration.getCommandExecutor()
                            .execute(commandContext -> {
                                BatchService batchService = CommandContextUtil.getCmmnEngineConfiguration(commandContext)
                                        .getBatchServiceConfiguration()
                                        .getBatchService();

                                Batch batch = cmmnManagementService.createBatchQuery().searchKey(caseInstanceId).singleResult();
                                BatchPart batchPart = cmmnManagementService.createBatchPartQuery().batchId(batch.getId()).singleResult();

                                batchService.completeBatchPart(batchPart.getId(), DeleteCaseInstanceBatchConstants.STATUS_COMPLETED, "Batch part result");
                                batchService.completeBatch(batch.getId(), DeleteCaseInstanceBatchConstants.STATUS_COMPLETED);

                                return null;
                            });
                }

                assertThat(cmmnManagementService.createTimerJobQuery().handlerType(CmmnHistoryCleanupJobHandler.TYPE).count()).isEqualTo(1);

                Job executableJob = cmmnManagementService.moveTimerToExecutableJob(
                        cmmnManagementService.createTimerJobQuery().handlerType(CmmnHistoryCleanupJobHandler.TYPE).singleResult().getId());
                cmmnManagementService.executeJob(executableJob.getId());

                assertThat(cmmnManagementService.createTimerJobQuery().handlerType(CmmnHistoryCleanupJobHandler.TYPE).count()).isEqualTo(1);

                assertThat(cmmnManagementService.createBatchQuery().count()).isEqualTo(12);
                assertThat(cmmnManagementService.createBatchPartQuery().count()).isEqualTo(12);

                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isEqualTo(20);
                assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().count()).isEqualTo(40);
                assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().count()).isEqualTo(20);

                Batch batch = cmmnManagementService.createBatchQuery().searchKey("Flowable CMMN History Cleanup").singleResult();
                assertThat(batch).isNotNull();
                batchesToRemove.add(batch.getId());
                assertThat(batch.getStatus()).isEqualTo(DeleteCaseInstanceBatchConstants.STATUS_IN_PROGRESS);
                assertThat(batch.getBatchType()).isEqualTo(Batch.HISTORIC_CASE_DELETE_TYPE);
                assertThat(batch.getBatchSearchKey()).isEqualTo("Flowable CMMN History Cleanup");

                assertThat(cmmnManagementService.createBatchPartQuery().batchId(batch.getId()).list())
                        .hasSize(1)
                        .allSatisfy(part -> {
                            assertThat(part.getStatus()).isEqualTo(DeleteCaseInstanceBatchConstants.STATUS_WAITING);
                            assertThat(part.getType()).isEqualTo(DeleteCaseInstanceBatchConstants.BATCH_PART_COMPUTE_IDS_TYPE);
                        });

                assertThat(cmmnManagementService.createJobQuery().list())
                        .hasSize(1)
                        .allSatisfy(job -> {
                            assertThat(job.getJobHandlerType()).isEqualTo(ComputeDeleteHistoricCaseInstanceIdsJobHandler.TYPE);
                        });

                CmmnJobTestHelper.waitForJobExecutorToProcessAllAsyncJobs(cmmnEngineConfiguration, 10000, 200, true);
                assertThat(cmmnManagementService.createJobQuery().list()).isEmpty();
                Job timer = cmmnManagementService.createTimerJobQuery().handlerType(ComputeDeleteHistoricCaseInstanceStatusJobHandler.TYPE).singleResult();
                assertThat(timer).isNotNull();
                cmmnManagementService.moveTimerToExecutableJob(timer.getId());
                cmmnManagementService.executeJob(timer.getId());
                assertThat(cmmnManagementService.createBatchPartQuery().batchId(batch.getId()).list())
                        .extracting(BatchPart::getStatus, BatchPart::getType)
                        .containsExactlyInAnyOrder(
                                tuple(DeleteCaseInstanceBatchConstants.STATUS_COMPLETED, DeleteCaseInstanceBatchConstants.BATCH_PART_COMPUTE_IDS_TYPE),
                                tuple(DeleteCaseInstanceBatchConstants.STATUS_WAITING, DeleteCaseInstanceBatchConstants.BATCH_PART_DELETE_CASE_INSTANCES_TYPE)
                        );
                assertThat(cmmnManagementService.createJobQuery().list())
                        .hasSize(1)
                        .allSatisfy(job -> {
                            assertThat(job.getJobHandlerType()).isEqualTo(DeleteHistoricCaseInstanceIdsJobHandler.TYPE);
                        });

                CmmnJobTestHelper.waitForJobExecutorToProcessAllAsyncJobs(cmmnEngineConfiguration, 10000, 200, true);
                assertThat(cmmnManagementService.createJobQuery().list()).isEmpty();
                timer = cmmnManagementService.createTimerJobQuery().handlerType(DeleteHistoricCaseInstanceIdsStatusJobHandler.TYPE).singleResult();
                assertThat(timer).isNotNull();
                cmmnManagementService.moveTimerToExecutableJob(timer.getId());
                cmmnManagementService.executeJob(timer.getId());

                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isEqualTo(10);
                assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().count()).isEqualTo(20);
                assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().count()).isEqualTo(10);

                for (int i = 0; i < 20; i++) {
                    if (i < 10) {
                        assertThat(cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstanceIds.get(i))).isEmpty();
                        assertThat(cmmnHistoryService.createHistoricTaskLogEntryQuery().caseInstanceId(caseInstanceIds.get(i)).count()).isZero();
                        assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstanceIds.get(i)).count()).isZero();
                        assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstanceIds.get(i)).count()).isZero();

                    } else {
                        assertThat(cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstanceIds.get(i))).hasSize(1);
                        assertThat(cmmnHistoryService.createHistoricTaskLogEntryQuery().caseInstanceId(caseInstanceIds.get(i)).count()).isEqualTo(1);
                        assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstanceIds.get(i)).count()).isEqualTo(2);
                        assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstanceIds.get(i)).count())
                                .isEqualTo(1);
                    }
                }

                cmmnManagementService
                        .deleteTimerJob(cmmnManagementService.createTimerJobQuery().handlerType(CmmnHistoryCleanupJobHandler.TYPE).singleResult().getId());
            }

        } finally {
            cmmnEngineConfiguration.resetClock();
        }
    }
}