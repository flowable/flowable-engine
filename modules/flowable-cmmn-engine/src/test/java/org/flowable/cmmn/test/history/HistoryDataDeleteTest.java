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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.Instant;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.flowable.batch.api.Batch;
import org.flowable.batch.api.BatchPart;
import org.flowable.cmmn.api.history.HistoricCaseInstanceQuery;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.impl.delete.DeleteCaseInstanceBatchConstants;
import org.flowable.cmmn.engine.impl.delete.DeleteHistoricCaseInstancesSequentialJobHandler;
import org.flowable.cmmn.engine.impl.job.CmmnHistoryCleanupJobHandler;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.cmmn.test.FlowableCmmnTestCase;
import org.flowable.cmmn.test.itemcontrol.RepetitionVariableAggregationTest;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.job.api.Job;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import net.javacrumbs.jsonunit.core.Option;

/**
 * @author Tijs Rademakers
 * @author Filip Hrisafov
 */
public class HistoryDataDeleteTest extends FlowableCmmnTestCase {

    protected Collection<String> batchesToRemove = new HashSet<>();

    @AfterEach
    public void tearDown() {
        batchesToRemove.forEach(cmmnManagementService::deleteBatch);
        Authentication.setAuthenticatedUserId(null);
        
        List<Job> jobs = cmmnManagementService.createJobQuery().list();
        for (Job job : jobs) {
            try {
                cmmnManagementService.deleteJob(job.getId());
            } catch(Exception e) {}
        }
        
        List<Job> timerJobs = cmmnManagementService.createTimerJobQuery().list();
        for (Job job : timerJobs) {
            try {
                cmmnManagementService.deleteTimerJob(job.getId());
            } catch(Exception e) {}
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/one-human-task-model.cmmn")
    public void testDeleteSingleHistoricCaseInstance() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();
        cmmnRuntimeService.setVariable(caseInstance.getId(), "testVar", "testValue");
        cmmnRuntimeService.setVariable(caseInstance.getId(), "numVar", 43);
        cmmnRuntimeService.setVariable(caseInstance.getId(), "serializableVar", new RepetitionVariableAggregationTest.TestSerializableVariable());

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.NONE, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
        }

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.setVariableLocal(task.getId(), "taskVar", "taskValue");
        cmmnTaskService.setVariableLocal(task.getId(), "taskSerializableVar", new RepetitionVariableAggregationTest.TestSerializableVariable());
        cmmnTaskService.complete(task.getId());

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.NONE, cmmnEngineConfiguration)) {
            HistoricCaseInstanceQuery query = cmmnHistoryService.createHistoricCaseInstanceQuery();
            Calendar cal = new GregorianCalendar();
            cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) + 1);
            query.finishedBefore(cal.getTime());

            query.deleteWithRelatedData();

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).count()).isZero();
            assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
            assertThat(cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstance.getId())).isEmpty();
            assertThat(cmmnHistoryService.getHistoricEntityLinkChildrenForCaseInstance(caseInstance.getId())).isEmpty();
            assertThat(cmmnHistoryService.createHistoricTaskLogEntryQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
            assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/human-task-milestone-model.cmmn")
    public void testDeleteHistoricInstancesWithFinishedBefore() {
        List<String> caseInstanceIds = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();
            caseInstanceIds.add(caseInstance.getId());
            cmmnRuntimeService.setVariable(caseInstance.getId(), "testVar", "testValue" + (i + 1));
            cmmnRuntimeService.setVariable(caseInstance.getId(), "numVar", (i + 1));
            cmmnRuntimeService.setVariable(caseInstance.getId(), "serializableVar", new RepetitionVariableAggregationTest.TestSerializableVariable());
        }

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.NONE, cmmnEngineConfiguration)) {

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isEqualTo(20);

        }

        for (int i = 0; i < 10; i++) {
            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstanceIds.get(i)).singleResult();
            cmmnTaskService.setVariableLocal(task.getId(), "taskVar", "taskValue" + (i + 1));
            cmmnTaskService.setVariableLocal(task.getId(), "taskSerializableVar", new RepetitionVariableAggregationTest.TestSerializableVariable());
            cmmnTaskService.complete(task.getId());
        }

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.NONE, cmmnEngineConfiguration)) {
            HistoricCaseInstanceQuery query = cmmnHistoryService.createHistoricCaseInstanceQuery();
            Calendar cal = new GregorianCalendar();
            cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) + 1);
            query.finishedBefore(cal.getTime());
            query.deleteWithRelatedData();

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isEqualTo(10);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().count()).isEqualTo(20);
            assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().count()).isEqualTo(10);

            for (int i = 0; i < 20; i++) {
                if (i < 10) {
                    assertThat(cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstanceIds.get(i))).isEmpty();
                    assertThat(cmmnHistoryService.createHistoricTaskLogEntryQuery().caseInstanceId(caseInstanceIds.get(i)).count()).isZero();
                    assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstanceIds.get(i)).count()).isZero();
                    assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstanceIds.get(i)).count())
                            .isZero();
                } else {
                    assertThat(cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstanceIds.get(i))).hasSize(1);
                    assertThat(cmmnHistoryService.createHistoricTaskLogEntryQuery().caseInstanceId(caseInstanceIds.get(i)).count()).isEqualTo(1);
                    assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstanceIds.get(i)).count()).isEqualTo(3);
                    assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstanceIds.get(i)).count())
                            .isEqualTo(1);
                }
            }
        }
    }
    
    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/human-task-milestone-model.cmmn")
    public void testDeleteHistoricInstances() {
        List<String> caseInstanceIds = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();
            caseInstanceIds.add(caseInstance.getId());
            cmmnRuntimeService.setVariable(caseInstance.getId(), "testVar", "testValue" + (i + 1));
            cmmnRuntimeService.setVariable(caseInstance.getId(), "numVar", (i + 1));
            cmmnRuntimeService.setVariable(caseInstance.getId(), "serializableVar", new RepetitionVariableAggregationTest.TestSerializableVariable());
        }

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.NONE, cmmnEngineConfiguration)) {

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isEqualTo(20);

        }

        for (int i = 0; i < 10; i++) {
            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstanceIds.get(i)).singleResult();
            cmmnTaskService.setVariableLocal(task.getId(), "taskVar", "taskValue" + (i + 1));
            cmmnTaskService.setVariableLocal(task.getId(), "taskSerializableVar", new RepetitionVariableAggregationTest.TestSerializableVariable());
            cmmnTaskService.complete(task.getId());
        }

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.NONE, cmmnEngineConfiguration)) {
            HistoricCaseInstanceQuery query = cmmnHistoryService.createHistoricCaseInstanceQuery();
            query.activePlanItemDefinitionId("noneexisting");
            query.deleteWithRelatedData();

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isEqualTo(20);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().count()).isEqualTo(40);
            assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().count()).isEqualTo(20);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/one-human-task-model.cmmn")
    public void testDeleteSingleHistoricCaseInstanceShouldDeleteTasks() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();
        cmmnRuntimeService.setVariable(caseInstance.getId(), "testVar", "testValue");
        cmmnRuntimeService.setVariable(caseInstance.getId(), "numVar", 43);
        cmmnRuntimeService.setVariable(caseInstance.getId(), "serializableVar", new RepetitionVariableAggregationTest.TestSerializableVariable());

        cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).list()).hasSize(1);
            assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstance.getId()).list()).hasSize(3);
            assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).list()).hasSize(1);
        }

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, cmmnEngineConfiguration)) {
            cmmnHistoryService.deleteHistoricCaseInstance(caseInstance.getId());
        }

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).list()).isEmpty();
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).list()).isEmpty();
            assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).list()).isEmpty();
            assertThat(cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstance.getId())).isEmpty();
            assertThat(cmmnHistoryService.getHistoricEntityLinkChildrenForCaseInstance(caseInstance.getId())).isEmpty();
            assertThat(cmmnHistoryService.createHistoricTaskLogEntryQuery().caseInstanceId(caseInstance.getId()).list()).isEmpty();
            assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstance.getId()).list()).isEmpty();
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/human-task-milestone-model.cmmn")
    public void testDeleteHistoricInstancesUsingBatch() {
        List<String> caseInstanceIds = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();
            caseInstanceIds.add(caseInstance.getId());
            cmmnRuntimeService.setVariable(caseInstance.getId(), "testVar", "testValue" + (i + 1));
            cmmnRuntimeService.setVariable(caseInstance.getId(), "numVar", (i + 1));
            cmmnRuntimeService.setVariable(caseInstance.getId(), "serializableVar", new RepetitionVariableAggregationTest.TestSerializableVariable());
        }

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isEqualTo(20);

        }

        for (int i = 0; i < 10; i++) {
            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstanceIds.get(i)).singleResult();
            cmmnTaskService.setVariableLocal(task.getId(), "taskVar", "taskValue" + (i + 1));
            cmmnTaskService.setVariableLocal(task.getId(), "taskSerializableVar", new RepetitionVariableAggregationTest.TestSerializableVariable());
            cmmnTaskService.complete(task.getId());
        }

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricCaseInstanceQuery query = cmmnHistoryService.createHistoricCaseInstanceQuery();
            Calendar cal = new GregorianCalendar();
            cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) + 1);
            query.finishedBefore(cal.getTime());
            String batchId = query.deleteSequentiallyUsingBatch(5, "Test Deletion");
            batchesToRemove.add(batchId);

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isEqualTo(20);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().count()).isEqualTo(40);
            assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().count()).isEqualTo(20);

            Batch batch = cmmnManagementService.createBatchQuery().batchId(batchId).singleResult();
            assertThat(batch).isNotNull();
            assertThat(batch.getStatus()).isEqualTo(DeleteCaseInstanceBatchConstants.STATUS_IN_PROGRESS);
            assertThat(batch.getBatchType()).isEqualTo(Batch.HISTORIC_CASE_DELETE_TYPE);
            assertThat(batch.getCompleteTime()).isNull();

            assertThat(cmmnManagementService.createBatchPartQuery().list())
                    .hasSize(1)
                    .allSatisfy(part -> {
                        assertThat(part.getStatus()).isEqualTo(DeleteCaseInstanceBatchConstants.STATUS_WAITING);
                        assertThat(part.getType()).isEqualTo(DeleteCaseInstanceBatchConstants.BATCH_PART_DELETE_CASE_INSTANCES_TYPE);
                    });

            assertThat(cmmnManagementService.createJobQuery().list())
                    .hasSize(1)
                    .allSatisfy(job -> {
                        assertThat(job.getJobHandlerType()).isEqualTo(DeleteHistoricCaseInstancesSequentialJobHandler.TYPE);
                    });

            waitForJobExecutorToProcessAllAsyncJobs();
            waitForAsyncHistoryExecutorToProcessAllJobs();
            assertThat(cmmnManagementService.createJobQuery().list()).isEmpty();
            assertThat(cmmnManagementService.createTimerJobQuery().list()).isEmpty();
            assertThat(cmmnManagementService.createBatchPartQuery().list())
                    .extracting(BatchPart::getStatus, BatchPart::getType)
                    .containsExactlyInAnyOrder(
                            tuple(DeleteCaseInstanceBatchConstants.STATUS_COMPLETED, DeleteCaseInstanceBatchConstants.BATCH_PART_DELETE_CASE_INSTANCES_TYPE),
                            tuple(DeleteCaseInstanceBatchConstants.STATUS_COMPLETED, DeleteCaseInstanceBatchConstants.BATCH_PART_DELETE_CASE_INSTANCES_TYPE),
                            tuple(DeleteCaseInstanceBatchConstants.STATUS_COMPLETED, DeleteCaseInstanceBatchConstants.BATCH_PART_DELETE_CASE_INSTANCES_TYPE)
                    );

            batch = cmmnManagementService.createBatchQuery().batchId(batchId).singleResult();
            assertThat(batch).isNotNull();
            assertThat(batch.getStatus()).isEqualTo(DeleteCaseInstanceBatchConstants.STATUS_COMPLETED);
            assertThat(batch.getBatchType()).isEqualTo(Batch.HISTORIC_CASE_DELETE_TYPE);
            assertThat(batch.getCompleteTime()).isNotNull();

            waitForAsyncHistoryExecutorToProcessAllJobs();
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
                    assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstanceIds.get(i)).count()).isEqualTo(3);
                    assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstanceIds.get(i)).count()).isEqualTo(1);
                }
            }
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/human-task-milestone-model.cmmn")
    public void testDeleteHistoricInstancesUsingBatchWithAuthenticatedUser() {
        Authentication.setAuthenticatedUserId("test-user");
        String batchId = cmmnHistoryService.createHistoricCaseInstanceQuery()
                .caseDefinitionKey("dummy")
                .deleteSequentiallyUsingBatch(5, "Test Deletion");
        batchesToRemove.add(batchId);

        Batch batch = cmmnManagementService.createBatchQuery().batchId(batchId).singleResult();
        assertThat(batch).isNotNull();
        assertThat(batch.getStatus()).isEqualTo(DeleteCaseInstanceBatchConstants.STATUS_COMPLETED);
        assertThat(batch.getBatchType()).isEqualTo(Batch.HISTORIC_CASE_DELETE_TYPE);
        assertThat(batch.getBatchSearchKey()).isEqualTo("Test Deletion");
        assertThat(batch.getBatchSearchKey2()).isEqualTo("test-user");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/human-task-milestone-model.cmmn")
    public void testDeleteHistoricInstancesUsingBatchAndDeleteFails() {
        List<String> caseInstanceIds = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();
            caseInstanceIds.add(caseInstance.getId());
            cmmnRuntimeService.setVariable(caseInstance.getId(), "testVar", "testValue" + (i + 1));
            cmmnRuntimeService.setVariable(caseInstance.getId(), "numVar", (i + 1));
            cmmnRuntimeService.setVariable(caseInstance.getId(), "serializableVar", new RepetitionVariableAggregationTest.TestSerializableVariable());
        }

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isEqualTo(20);
        }

        for (int i = 0; i < 10; i++) {
            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstanceIds.get(i)).singleResult();
            cmmnTaskService.setVariableLocal(task.getId(), "taskVar", "taskValue" + (i + 1));
            cmmnTaskService.setVariableLocal(task.getId(), "taskSerializableVar", new RepetitionVariableAggregationTest.TestSerializableVariable());
            cmmnTaskService.complete(task.getId());
        }

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            String batchId = cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .deleteSequentiallyUsingBatch(5, "Test Deletion");
            batchesToRemove.add(batchId);

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isEqualTo(20);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().count()).isEqualTo(40);
            assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().count()).isEqualTo(20);

            Batch batch = cmmnManagementService.createBatchQuery().batchId(batchId).singleResult();
            assertThat(batch).isNotNull();
            assertThat(batch.getStatus()).isEqualTo(DeleteCaseInstanceBatchConstants.STATUS_IN_PROGRESS);
            assertThat(batch.getBatchType()).isEqualTo(Batch.HISTORIC_CASE_DELETE_TYPE);
            assertThat(batch.getCompleteTime()).isNull();
            assertThatJson(batch.getBatchDocumentJson(ScopeTypes.CMMN))
                    .isEqualTo("{"
                            + "  numberOfInstances: 20,"
                            + "  batchSize: 5,"
                            + "  query: { },"
                            + "  sequential: true"
                            + "}");

            assertThat(cmmnManagementService.createBatchPartQuery().list())
                    .hasSize(1)
                    .allSatisfy(part -> {
                        assertThat(part.getStatus()).isEqualTo(DeleteCaseInstanceBatchConstants.STATUS_WAITING);
                        assertThat(part.getType()).isEqualTo(DeleteCaseInstanceBatchConstants.BATCH_PART_DELETE_CASE_INSTANCES_TYPE);
                    });

            assertThat(cmmnManagementService.createJobQuery().list())
                    .hasSize(1)
                    .allSatisfy(job -> {
                        assertThat(job.getJobHandlerType()).isEqualTo(DeleteHistoricCaseInstancesSequentialJobHandler.TYPE);
                    });

            waitForJobExecutorToProcessAllAsyncJobs();
            waitForAsyncHistoryExecutorToProcessAllJobs();
            assertThat(cmmnManagementService.createJobQuery().list()).isEmpty();
            assertThat(cmmnManagementService.createTimerJobQuery().list()).isEmpty();
            assertThat(cmmnManagementService.createBatchPartQuery().list())
                    .extracting(BatchPart::getStatus, BatchPart::getType)
                    .containsExactlyInAnyOrder(
                            tuple(DeleteCaseInstanceBatchConstants.STATUS_COMPLETED, DeleteCaseInstanceBatchConstants.BATCH_PART_DELETE_CASE_INSTANCES_TYPE),
                            tuple(DeleteCaseInstanceBatchConstants.STATUS_COMPLETED, DeleteCaseInstanceBatchConstants.BATCH_PART_DELETE_CASE_INSTANCES_TYPE),
                            tuple(DeleteCaseInstanceBatchConstants.STATUS_COMPLETED, DeleteCaseInstanceBatchConstants.BATCH_PART_DELETE_CASE_INSTANCES_TYPE),
                            tuple(DeleteCaseInstanceBatchConstants.STATUS_COMPLETED, DeleteCaseInstanceBatchConstants.BATCH_PART_DELETE_CASE_INSTANCES_TYPE),
                            tuple(DeleteCaseInstanceBatchConstants.STATUS_COMPLETED, DeleteCaseInstanceBatchConstants.BATCH_PART_DELETE_CASE_INSTANCES_TYPE)
                    );

            batch = cmmnManagementService.createBatchQuery().batchId(batchId).singleResult();
            assertThat(batch).isNotNull();
            assertThat(batch.getStatus()).isEqualTo(DeleteCaseInstanceBatchConstants.STATUS_COMPLETED);
            assertThat(batch.getBatchType()).isEqualTo(Batch.HISTORIC_CASE_DELETE_TYPE);
            assertThat(batch.getCompleteTime()).isNotNull();
            assertThatJson(batch.getBatchDocumentJson(ScopeTypes.CMMN))
                    .isEqualTo("{"
                            + "  numberOfInstances: 20,"
                            + "  batchSize: 5,"
                            + "  query: { },"
                            + "  sequential: true"
                            + "}");

            waitForAsyncHistoryExecutorToProcessAllJobs();
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isEqualTo(0);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().count()).isEqualTo(0);
            assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().count()).isEqualTo(0);

            for (int i = 0; i < 20; i++) {
                assertThat(cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstanceIds.get(i))).isEmpty();
                assertThat(cmmnHistoryService.createHistoricTaskLogEntryQuery().caseInstanceId(caseInstanceIds.get(i)).count()).isZero();
                assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstanceIds.get(i)).count()).isZero();
                assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstanceIds.get(i)).count()).isZero();
            }
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/human-task-milestone-model.cmmn")
    public void testDeleteHistoricInstancesWithOrAndVariableValueUsingBatch() {
        List<String> caseInstanceIds = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();
            caseInstanceIds.add(caseInstance.getId());
            cmmnRuntimeService.setVariable(caseInstance.getId(), "testVar", "testValue" + (i + 1));
            cmmnRuntimeService.setVariable(caseInstance.getId(), "numVar", (i + 1));
            cmmnRuntimeService.setVariable(caseInstance.getId(), "serializableVar", new RepetitionVariableAggregationTest.TestSerializableVariable());
        }

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isEqualTo(10);

        }

        for (int i = 0; i < 5; i++) {
            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstanceIds.get(i)).singleResult();
            cmmnTaskService.setVariableLocal(task.getId(), "taskVar", "taskValue" + (i + 1));
            cmmnTaskService.setVariableLocal(task.getId(), "taskSerializableVar", new RepetitionVariableAggregationTest.TestSerializableVariable());
            cmmnTaskService.complete(task.getId());
        }

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            String batchId = cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .finished()
                    .or()
                        .variableValueEquals("numVar", 1)
                        .variableValueEquals("numVar", 2)
                        .variableValueEquals("numVar", 3)
                        .variableValueEquals("numVar", 6)
                        .variableValueEquals("numVar", 7)
                    .endOr()
                    .deleteSequentiallyUsingBatch(5, "Test Deletion");
            batchesToRemove.add(batchId);

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isEqualTo(10);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().count()).isEqualTo(20);
            assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().count()).isEqualTo(10);

            Batch batch = cmmnManagementService.createBatchQuery().batchId(batchId).singleResult();
            assertThat(batch).isNotNull();
            assertThat(batch.getStatus()).isEqualTo(DeleteCaseInstanceBatchConstants.STATUS_IN_PROGRESS);
            assertThat(batch.getBatchType()).isEqualTo(Batch.HISTORIC_CASE_DELETE_TYPE);
            assertThatJson(batch.getBatchDocumentJson(ScopeTypes.CMMN))
                    .isEqualTo("{"
                            + "  numberOfInstances: 3,"
                            + "  batchSize: 5,"
                            + "  query: {"
                            + "    finished: true,"
                            + "    orQueryObjects: ["
                            + "      {"
                            + "        queryVariableValues: ["
                            + "          { name: 'numVar', operator: 'EQUALS', textValue: '1', longValue: 1, type: 'integer' },"
                            + "          { name: 'numVar', operator: 'EQUALS', textValue: '2', longValue: 2, type: 'integer' },"
                            + "          { name: 'numVar', operator: 'EQUALS', textValue: '3', longValue: 3, type: 'integer' },"
                            + "          { name: 'numVar', operator: 'EQUALS', textValue: '6', longValue: 6, type: 'integer' },"
                            + "          { name: 'numVar', operator: 'EQUALS', textValue: '7', longValue: 7, type: 'integer' }"
                            + "        ]"
                            + "      }"
                            + "    ]"
                            + "  },"
                            + "  sequential: true"
                            + "}");

            waitForJobExecutorToProcessAllAsyncJobs();
            waitForAsyncHistoryExecutorToProcessAllJobs();
            
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isEqualTo(7);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().count()).isEqualTo(14);
            assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().count()).isEqualTo(7);

            for (int i = 0; i < 10; i++) {
                if (i < 3) {
                    assertThat(cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstanceIds.get(i))).isEmpty();
                    assertThat(cmmnHistoryService.createHistoricTaskLogEntryQuery().caseInstanceId(caseInstanceIds.get(i)).count()).isZero();
                    assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstanceIds.get(i)).count()).isZero();
                    assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstanceIds.get(i)).count()).isZero();
                } else if (i < 5) {
                    assertThat(cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstanceIds.get(i))).hasSize(1);
                    assertThat(cmmnHistoryService.createHistoricTaskLogEntryQuery().caseInstanceId(caseInstanceIds.get(i)).count()).isEqualTo(2);
                    assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstanceIds.get(i)).count()).isEqualTo(5);
                    assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstanceIds.get(i)).count()).isEqualTo(1);
                } else {
                    assertThat(cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstanceIds.get(i))).hasSize(1);
                    assertThat(cmmnHistoryService.createHistoricTaskLogEntryQuery().caseInstanceId(caseInstanceIds.get(i)).count()).isEqualTo(1);
                    assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstanceIds.get(i)).count()).isEqualTo(3);
                    assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstanceIds.get(i)).count()).isEqualTo(1);
                }
            }
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/human-task-milestone-model.cmmn")
    public void testDeleteHistoricInstancesUsingBatchWhenNothingMatches() {
        List<String> caseInstanceIds = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();
            caseInstanceIds.add(caseInstance.getId());
            cmmnRuntimeService.setVariable(caseInstance.getId(), "testVar", "testValue" + (i + 1));
            cmmnRuntimeService.setVariable(caseInstance.getId(), "numVar", (i + 1));
            cmmnRuntimeService.setVariable(caseInstance.getId(), "serializableVar", new RepetitionVariableAggregationTest.TestSerializableVariable());
        }

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isEqualTo(10);

        }

        for (int i = 0; i < 5; i++) {
            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstanceIds.get(i)).singleResult();
            cmmnTaskService.setVariableLocal(task.getId(), "taskVar", "taskValue" + (i + 1));
            cmmnTaskService.setVariableLocal(task.getId(), "taskSerializableVar", new RepetitionVariableAggregationTest.TestSerializableVariable());
            cmmnTaskService.complete(task.getId());
        }

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            String batchId = cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .finished()
                    .caseDefinitionKey("dummy")
                    .deleteSequentiallyUsingBatch(5, "Test Deletion");
            batchesToRemove.add(batchId);

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isEqualTo(10);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().count()).isEqualTo(20);
            assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().count()).isEqualTo(10);

            Batch batch = cmmnManagementService.createBatchQuery().batchId(batchId).singleResult();
            assertThat(batch).isNotNull();
            assertThat(batch.getStatus()).isEqualTo(DeleteCaseInstanceBatchConstants.STATUS_COMPLETED);
            assertThat(batch.getBatchType()).isEqualTo(Batch.HISTORIC_CASE_DELETE_TYPE);
            assertThat(batch.getCompleteTime()).isNotNull();
            assertThatJson(batch.getBatchDocumentJson(ScopeTypes.CMMN))
                    .isEqualTo("{"
                            + "  numberOfInstances: 0,"
                            + "  batchSize: 5,"
                            + "  query: {"
                            + "    finished: true,"
                            + "    caseDefinitionKey: 'dummy'"
                            + "  },"
                            + "  sequential: true"
                            + "}");

            assertThat(cmmnManagementService.createBatchPartQuery().batchId(batchId).list()).isEmpty();
            assertThat(cmmnManagementService.createJobQuery().list()).isEmpty();
            assertThat(cmmnManagementService.createTimerJobQuery().list()).isEmpty();
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/human-task-milestone-model.cmmn")
    public void testDeleteHistoricInstancesUsingUnevenBatch() {
        List<String> caseInstanceIds = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();
            caseInstanceIds.add(caseInstance.getId());
            cmmnRuntimeService.setVariable(caseInstance.getId(), "testVar", "testValue" + (i + 1));
            cmmnRuntimeService.setVariable(caseInstance.getId(), "numVar", (i + 1));
            cmmnRuntimeService.setVariable(caseInstance.getId(), "serializableVar", new RepetitionVariableAggregationTest.TestSerializableVariable());
        }

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isEqualTo(20);
        }

        for (int i = 0; i < 10; i++) {
            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstanceIds.get(i)).singleResult();
            cmmnTaskService.setVariableLocal(task.getId(), "taskVar", "taskValue" + (i + 1));
            cmmnTaskService.setVariableLocal(task.getId(), "taskSerializableVar", new RepetitionVariableAggregationTest.TestSerializableVariable());
            cmmnTaskService.complete(task.getId());
        }

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricCaseInstanceQuery query = cmmnHistoryService.createHistoricCaseInstanceQuery();
            Calendar cal = new GregorianCalendar();
            cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) + 1);
            query.finishedBefore(cal.getTime());
            String batchId = query.deleteSequentiallyUsingBatch(7, "Test Deletion Uneven");
            batchesToRemove.add(batchId);

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isEqualTo(20);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().count()).isEqualTo(40);
            assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().count()).isEqualTo(20);

            Batch batch = cmmnManagementService.createBatchQuery().batchId(batchId).singleResult();
            assertThat(batch).isNotNull();
            assertThat(batch.getStatus()).isEqualTo(DeleteCaseInstanceBatchConstants.STATUS_IN_PROGRESS);
            assertThat(batch.getBatchType()).isEqualTo(Batch.HISTORIC_CASE_DELETE_TYPE);
            assertThat(batch.getBatchSearchKey()).isEqualTo("Test Deletion Uneven");
            assertThat(batch.getBatchSearchKey2()).isNull();
            assertThatJson(batch.getBatchDocumentJson(ScopeTypes.CMMN))
                    .isEqualTo("{"
                            + "  numberOfInstances: 10,"
                            + "  batchSize: 7,"
                            + "  query: {"
                            + "    finishedBefore: '${json-unit.any-string}'"
                            + "  },"
                            + "  sequential: true"
                            + "}");

            assertThat(cmmnManagementService.createBatchPartQuery().list())
                    .hasSize(1)
                    .allSatisfy(part -> {
                        assertThat(part.getStatus()).isEqualTo(DeleteCaseInstanceBatchConstants.STATUS_WAITING);
                        assertThat(part.getType()).isEqualTo(DeleteCaseInstanceBatchConstants.BATCH_PART_DELETE_CASE_INSTANCES_TYPE);
                    });

            assertThat(cmmnManagementService.createJobQuery().list())
                    .hasSize(1)
                    .allSatisfy(job -> {
                        assertThat(job.getJobHandlerType()).isEqualTo(DeleteHistoricCaseInstancesSequentialJobHandler.TYPE);
                    });

            waitForJobExecutorToProcessAllAsyncJobs();
            waitForAsyncHistoryExecutorToProcessAllJobs();
            
            assertThat(cmmnManagementService.createJobQuery().list()).isEmpty();
            assertThat(cmmnManagementService.createTimerJobQuery().list()).isEmpty();
            assertThat(cmmnManagementService.createBatchPartQuery().list())
                    .extracting(BatchPart::getStatus, BatchPart::getType)
                    .containsExactlyInAnyOrder(
                            tuple(DeleteCaseInstanceBatchConstants.STATUS_COMPLETED, DeleteCaseInstanceBatchConstants.BATCH_PART_DELETE_CASE_INSTANCES_TYPE),
                            tuple(DeleteCaseInstanceBatchConstants.STATUS_COMPLETED, DeleteCaseInstanceBatchConstants.BATCH_PART_DELETE_CASE_INSTANCES_TYPE),
                            tuple(DeleteCaseInstanceBatchConstants.STATUS_COMPLETED, DeleteCaseInstanceBatchConstants.BATCH_PART_DELETE_CASE_INSTANCES_TYPE)
                    );

            batch = cmmnManagementService.createBatchQuery().batchId(batchId).singleResult();
            assertThat(batch).isNotNull();
            assertThat(batch.getStatus()).isEqualTo(DeleteCaseInstanceBatchConstants.STATUS_COMPLETED);
            assertThat(batch.getBatchType()).isEqualTo(Batch.HISTORIC_CASE_DELETE_TYPE);
            assertThat(batch.getBatchSearchKey()).isEqualTo("Test Deletion Uneven");
            assertThat(batch.getBatchSearchKey2()).isNull();
            assertThatJson(batch.getBatchDocumentJson(ScopeTypes.CMMN))
                    .isEqualTo("{"
                            + "  numberOfInstances: 10,"
                            + "  batchSize: 7,"
                            + "  query: {"
                            + "    finishedBefore: '${json-unit.any-string}'"
                            + "  },"
                            + "  sequential: true"
                            + "}");

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isEqualTo(10);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().count()).isEqualTo(20);
            assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().count()).isEqualTo(10);

            for (int i = 0; i < 20; i++) {
                if (i < 10) {
                    assertThat(cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstanceIds.get(i))).isEmpty();
                    assertThat(cmmnHistoryService.createHistoricTaskLogEntryQuery().caseInstanceId(caseInstanceIds.get(i)).count()).isZero();
                    assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstanceIds.get(i)).count()).isZero();
                    assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstanceIds.get(i)).count())
                            .isZero();
                } else {
                    assertThat(cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstanceIds.get(i))).hasSize(1);
                    assertThat(cmmnHistoryService.createHistoricTaskLogEntryQuery().caseInstanceId(caseInstanceIds.get(i)).count()).isEqualTo(1);
                    assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstanceIds.get(i)).count()).isEqualTo(3);
                    assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstanceIds.get(i)).count())
                            .isEqualTo(1);
                }
            }
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/human-task-milestone-model.cmmn")
    public void testDeleteHistoricInstancesSequentiallyUsingBatch() {
        List<String> caseInstanceIds = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();
            caseInstanceIds.add(caseInstance.getId());
            cmmnRuntimeService.setVariable(caseInstance.getId(), "testVar", "testValue" + (i + 1));
            cmmnRuntimeService.setVariable(caseInstance.getId(), "numVar", (i + 1));
            cmmnRuntimeService.setVariable(caseInstance.getId(), "serializableVar", new RepetitionVariableAggregationTest.TestSerializableVariable());
        }

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isEqualTo(20);

        }

        for (int i = 0; i < 10; i++) {
            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstanceIds.get(i)).singleResult();
            cmmnTaskService.setVariableLocal(task.getId(), "taskVar", "taskValue" + (i + 1));
            cmmnTaskService.setVariableLocal(task.getId(), "taskSerializableVar", new RepetitionVariableAggregationTest.TestSerializableVariable());
            cmmnTaskService.complete(task.getId());
        }

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricCaseInstanceQuery query = cmmnHistoryService.createHistoricCaseInstanceQuery();
            Calendar cal = new GregorianCalendar();
            cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) + 1);
            query.finishedBefore(cal.getTime());
            String batchId = query.deleteSequentiallyUsingBatch(5, "Test Deletion");
            batchesToRemove.add(batchId);

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isEqualTo(20);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().count()).isEqualTo(40);
            assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().count()).isEqualTo(20);

            Batch batch = cmmnManagementService.createBatchQuery().batchId(batchId).singleResult();
            assertThat(batch).isNotNull();
            assertThat(batch.getStatus()).isEqualTo(DeleteCaseInstanceBatchConstants.STATUS_IN_PROGRESS);
            assertThat(batch.getBatchType()).isEqualTo(Batch.HISTORIC_CASE_DELETE_TYPE);
            assertThat(batch.getBatchSearchKey()).isEqualTo("Test Deletion");
            assertThat(batch.getBatchSearchKey2()).isNull();
            assertThat(batch.getCompleteTime()).isNull();
            assertThatJson(batch.getBatchDocumentJson(ScopeTypes.CMMN))
                    .isEqualTo("{"
                            + "  numberOfInstances: 10,"
                            + "  batchSize: 5,"
                            + "  sequential: true,"
                            + "  query: {"
                            + "    finishedBefore: '${json-unit.any-string}'"
                            + "  }"
                            + "}");

            assertThat(cmmnManagementService.createBatchPartQuery().list())
                    .extracting(BatchPart::getStatus, BatchPart::getType)
                    .containsExactlyInAnyOrder(
                            tuple(DeleteCaseInstanceBatchConstants.STATUS_WAITING, DeleteCaseInstanceBatchConstants.BATCH_PART_DELETE_CASE_INSTANCES_TYPE)
                    );

            assertThat(cmmnManagementService.createJobQuery().list())
                    .hasSize(1)
                    .allSatisfy(job -> {
                        assertThat(job.getJobHandlerType()).isEqualTo(DeleteHistoricCaseInstancesSequentialJobHandler.TYPE);
                    });

            Job batchJob = cmmnManagementService.createJobQuery().singleResult();
            cmmnManagementService.executeJob(batchJob.getId());

            assertThat(cmmnManagementService.createBatchPartQuery().list())
                    .extracting(BatchPart::getStatus, BatchPart::getType)
                    .containsExactlyInAnyOrder(
                            tuple(DeleteCaseInstanceBatchConstants.STATUS_COMPLETED, DeleteCaseInstanceBatchConstants.BATCH_PART_DELETE_CASE_INSTANCES_TYPE),
                            tuple(DeleteCaseInstanceBatchConstants.STATUS_WAITING, DeleteCaseInstanceBatchConstants.BATCH_PART_DELETE_CASE_INSTANCES_TYPE)
                    );

            assertThat(cmmnManagementService.createJobQuery().list())
                    .hasSize(1)
                    .allSatisfy(job -> {
                        assertThat(job.getJobHandlerType()).isEqualTo(DeleteHistoricCaseInstancesSequentialJobHandler.TYPE);
                    });

            batch = cmmnManagementService.createBatchQuery().batchId(batchId).singleResult();
            assertThat(batch).isNotNull();
            assertThat(batch.getStatus()).isEqualTo(DeleteCaseInstanceBatchConstants.STATUS_IN_PROGRESS);
            assertThat(batch.getBatchType()).isEqualTo(Batch.HISTORIC_CASE_DELETE_TYPE);
            assertThat(batch.getCompleteTime()).isNull();

            batchJob = cmmnManagementService.createJobQuery().singleResult();
            cmmnManagementService.executeJob(batchJob.getId());

            assertThat(cmmnManagementService.createBatchPartQuery().list())
                    .extracting(BatchPart::getStatus, BatchPart::getType)
                    .containsExactlyInAnyOrder(
                            tuple(DeleteCaseInstanceBatchConstants.STATUS_COMPLETED, DeleteCaseInstanceBatchConstants.BATCH_PART_DELETE_CASE_INSTANCES_TYPE),
                            tuple(DeleteCaseInstanceBatchConstants.STATUS_COMPLETED, DeleteCaseInstanceBatchConstants.BATCH_PART_DELETE_CASE_INSTANCES_TYPE),
                            tuple(DeleteCaseInstanceBatchConstants.STATUS_WAITING, DeleteCaseInstanceBatchConstants.BATCH_PART_DELETE_CASE_INSTANCES_TYPE)
                    );

            batch = cmmnManagementService.createBatchQuery().batchId(batchId).singleResult();
            assertThat(batch).isNotNull();
            assertThat(batch.getStatus()).isEqualTo(DeleteCaseInstanceBatchConstants.STATUS_IN_PROGRESS);
            assertThat(batch.getBatchType()).isEqualTo(Batch.HISTORIC_CASE_DELETE_TYPE);
            assertThat(batch.getCompleteTime()).isNull();

            batchJob = cmmnManagementService.createJobQuery().singleResult();
            cmmnManagementService.executeJob(batchJob.getId());

            assertThat(cmmnManagementService.createJobQuery().list()).isEmpty();
            assertThat(cmmnManagementService.createTimerJobQuery().list()).isEmpty();

            assertThat(cmmnManagementService.createBatchPartQuery().list())
                    .extracting(BatchPart::getStatus, BatchPart::getType)
                    .containsExactlyInAnyOrder(
                            tuple(DeleteCaseInstanceBatchConstants.STATUS_COMPLETED, DeleteCaseInstanceBatchConstants.BATCH_PART_DELETE_CASE_INSTANCES_TYPE),
                            tuple(DeleteCaseInstanceBatchConstants.STATUS_COMPLETED, DeleteCaseInstanceBatchConstants.BATCH_PART_DELETE_CASE_INSTANCES_TYPE),
                            tuple(DeleteCaseInstanceBatchConstants.STATUS_COMPLETED, DeleteCaseInstanceBatchConstants.BATCH_PART_DELETE_CASE_INSTANCES_TYPE)
                    );

            batch = cmmnManagementService.createBatchQuery().batchId(batchId).singleResult();
            assertThat(batch).isNotNull();
            assertThat(batch.getStatus()).isEqualTo(DeleteCaseInstanceBatchConstants.STATUS_COMPLETED);
            assertThat(batch.getBatchType()).isEqualTo(Batch.HISTORIC_CASE_DELETE_TYPE);
            assertThat(batch.getCompleteTime()).isNotNull();

            waitForAsyncHistoryExecutorToProcessAllJobs();

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isEqualTo(10);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().count()).isEqualTo(20);
            assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().count()).isEqualTo(10);

            for (int i = 0; i < 20; i++) {
                if (i < 10) {
                    assertThat(cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstanceIds.get(i))).isEmpty();
                    assertThat(cmmnHistoryService.createHistoricTaskLogEntryQuery().caseInstanceId(caseInstanceIds.get(i)).count()).isZero();
                    assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstanceIds.get(i)).count()).isZero();
                    assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstanceIds.get(i)).count())
                            .isZero();
                } else {
                    assertThat(cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstanceIds.get(i))).hasSize(1);
                    assertThat(cmmnHistoryService.createHistoricTaskLogEntryQuery().caseInstanceId(caseInstanceIds.get(i)).count()).isEqualTo(1);
                    assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstanceIds.get(i)).count()).isEqualTo(3);
                    assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstanceIds.get(i)).count())
                            .isEqualTo(1);
                }
            }
        }
    }

    @Test
    public void testDeleteHistoricInstancesWithAllQueryOptions() throws InvocationTargetException, IllegalAccessException {
        // This test is meant to validate that all query options are present when doing delete
        // If this test fails verify that the properties that are missing are added to DeleteHistoricCaseInstancesUsingBatchesCmd and BatchDeleteCaseConfig
        Map<String, String> methodNameToExpectedQueryPropertyName = new HashMap<>();
        methodNameToExpectedQueryPropertyName.put("caseInstanceBusinessKey", "businessKey");
        methodNameToExpectedQueryPropertyName.put("caseInstanceBusinessKeyLike", "businessKeyLike");
        methodNameToExpectedQueryPropertyName.put("caseInstanceBusinessKeyLikeIgnoreCase", "businessKeyLikeIgnoreCase");
        methodNameToExpectedQueryPropertyName.put("caseInstanceBusinessStatus", "businessStatus");
        methodNameToExpectedQueryPropertyName.put("caseInstanceBusinessStatusLike", "businessStatusLike");
        methodNameToExpectedQueryPropertyName.put("caseInstanceBusinessStatusLikeIgnoreCase", "businessStatusLikeIgnoreCase");
        methodNameToExpectedQueryPropertyName.put("caseInstanceCallbackType", "callbackType");
        methodNameToExpectedQueryPropertyName.put("caseInstanceCallbackId", "callbackId");
        methodNameToExpectedQueryPropertyName.put("caseInstanceCallbackIds", "callbackIds");
        methodNameToExpectedQueryPropertyName.put("withoutCaseInstanceCallbackId", "withoutCallbackId");
        methodNameToExpectedQueryPropertyName.put("caseInstanceReferenceType", "referenceType");
        methodNameToExpectedQueryPropertyName.put("caseInstanceReferenceId", "referenceId");
        methodNameToExpectedQueryPropertyName.put("caseInstanceWithoutTenantId", "withoutTenantId");
        methodNameToExpectedQueryPropertyName.put("caseInstanceTenantId", "tenantId");
        methodNameToExpectedQueryPropertyName.put("caseInstanceTenantIdLike", "tenantIdLike");
        methodNameToExpectedQueryPropertyName.put("caseInstanceTenantIdLikeIgnoreCase", "tenantIdLikeIgnoreCase");
        methodNameToExpectedQueryPropertyName.put("withoutCaseInstanceParent", "withoutCaseInstanceParentId");
        Set<String> methodsToIgnore = new HashSet<>();
        methodsToIgnore.add("limitCaseVariables");
        methodsToIgnore.add("parentCaseInstanceId");
        methodsToIgnore.add("includeCaseVariables");
        methodsToIgnore.add("locale");
        methodsToIgnore.add("withLocalizationFallback");
        methodsToIgnore.add("returnIdsOnly");
        methodsToIgnore.add("withoutSorting");
        methodsToIgnore.add("asc");
        methodsToIgnore.add("desc");
        methodsToIgnore.add("or");
        methodsToIgnore.add("endOr");
        methodsToIgnore.add("singleResult");
        Set<String> methodsWith2ParametersToIgnore = new HashSet<>();
        methodsWith2ParametersToIgnore.add("involvedUser");
        methodsWith2ParametersToIgnore.add("involvedGroup");
        HistoricCaseInstanceQuery query = cmmnHistoryService.createHistoricCaseInstanceQuery();
        Map<String, String> expectedParameters = new LinkedHashMap<>();

        Map<Method, Pair<String, Object>> methodsAndParametersForOr = new LinkedHashMap<>();

        for (Method method : HistoricCaseInstanceQuery.class.getMethods()) {
            String methodName = method.getName();
            if (methodsToIgnore.contains(methodName)
                    || methodName.startsWith("orderBy")
                    || methodName.startsWith("variable")
                    || (method.getParameterCount() == 2 && methodsWith2ParametersToIgnore.contains(methodName))
            ) {
                continue;
            }
            Class<?> returnType = method.getReturnType();
            if (!returnType.isInstance(query)) {
                // We only care about methods that return the query itself
                continue;
            }

            Instant baseTime = Instant.now().truncatedTo(ChronoUnit.SECONDS)
                    .minus(365, ChronoUnit.DAYS)
                    .with(ChronoField.MILLI_OF_SECOND, 563);

            Parameter[] parameters = method.getParameters();
            String propertyName = methodNameToExpectedQueryPropertyName.getOrDefault(methodName, methodName);
            if (parameters.length == 0) {
                expectedParameters.put(propertyName, "true");
                method.invoke(query);
                methodsAndParametersForOr.put(method, Pair.of("true", null));
            } else if (parameters.length == 1) {
                Parameter parameter = parameters[0];
                Class<?> parameterType = parameter.getType();
                Object parameterValue;
                Object parameterOrValue;
                String expectedValue;
                String expectedOrValue;
                if (parameterType.isAssignableFrom(String.class)) {
                    parameterValue = methodName + "Value";
                    expectedValue = "'" + parameterValue + "'";
                    parameterOrValue = methodName + "OrValue";
                    expectedOrValue = "'" + parameterOrValue + "'";
                } else if (parameterType.isAssignableFrom(Set.class)) {
                    String value1 = methodName + "SetValue1";
                    String value2 = methodName + "SetValue2";
                    parameterValue = new LinkedHashSet<>(Arrays.asList(value1, value2));
                    expectedValue = "["
                            + "  '" + value1 + "',"
                            + "  '" + value2 + "'"
                            + "]";

                    String value1Or = value1 + "Or";
                    String value2Or = value2 + "Or";
                    parameterOrValue = new LinkedHashSet<>(Arrays.asList(value1Or, value2Or));
                    expectedOrValue = "["
                            + "  '" + value1Or + "',"
                            + "  '" + value2Or + "'"
                            + "]";
                } else if (parameterType.isAssignableFrom(List.class)) {
                    String value1 = methodName + "ListValue1";
                    String value2 = methodName + "ListValue2";
                    parameterValue = Arrays.asList(value1, value2);
                    expectedValue = "["
                            + "  '" + value1 + "',"
                            + "  '" + value2 + "'"
                            + "]";

                    String value1Or = value1 + "Or";
                    String value2Or = value2 + "Or";
                    parameterOrValue = Arrays.asList(value1Or, value2Or);
                    expectedOrValue = "["
                            + "  '" + value1Or + "',"
                            + "  '" + value2Or + "'"
                            + "]";
                } else if (parameterType.isAssignableFrom(Integer.class)) {
                    parameterValue = methodName.hashCode();
                    expectedValue = parameterValue.toString();

                    parameterOrValue = methodName.hashCode() * 21;
                    expectedOrValue = parameterOrValue.toString();
                } else if (parameterType.isAssignableFrom(Date.class)) {
                    baseTime = baseTime.plus(10, ChronoUnit.DAYS);
                    parameterValue = Date.from(baseTime);
                    expectedValue = "'" + baseTime + "'";

                    Instant orTime = baseTime.plus(3, ChronoUnit.DAYS);
                    parameterOrValue = Date.from(orTime);
                    expectedOrValue = "'" + orTime + "'";
                } else {
                    throw new AssertionFailedError("No value could be resolved for method " + method);
                }

                expectedParameters.put(propertyName, expectedValue);
                method.invoke(query, parameterValue);
                methodsAndParametersForOr.put(method, Pair.of(expectedOrValue, parameterOrValue));
            } else {
                throw new AssertionFailedError("No value could be resolved for method " + method);
            }
        }

        query.or();
        for (Map.Entry<Method, Pair<String, Object>> entry : methodsAndParametersForOr.entrySet()) {
            Object argument = entry.getValue().getRight();
            if (argument == null) {
                entry.getKey().invoke(query);
            } else {
                entry.getKey().invoke(query, argument);
            }
        }
        query.endOr();

        String batchId = query.deleteSequentiallyUsingBatch(5, "Test Deletion");
        batchesToRemove.add(batchId);

        Batch batch = cmmnManagementService.createBatchQuery().batchId(batchId).singleResult();
        assertThat(batch).isNotNull();

        Function<Method, String> propertyNameProvider = m -> methodNameToExpectedQueryPropertyName.getOrDefault(m.getName(), m.getName());
        String expectedOrQueryValue = methodsAndParametersForOr.entrySet()
                .stream()
                .map(entry -> propertyNameProvider.apply(entry.getKey()) + ": " + entry.getValue().getLeft())
                .collect(Collectors.joining(","));

        expectedParameters.put("orQueryObjects", "[{" + expectedOrQueryValue + "}]");

        String expectedQueryValue = expectedParameters.entrySet()
                .stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining(","));

        assertThatJson(batch.getBatchDocumentJson(ScopeTypes.CMMN))
                .isEqualTo("{"
                        + "  numberOfInstances: 0,"
                        + "  batchSize: 5,"
                        + "  sequential: true,"
                        + "  query: {" + expectedQueryValue + "}"
                        + "}");
    }

    @Test
    public void testDeleteHistoricInstancesWithInvolvedOptions()  {
        String batchId = cmmnHistoryService.createHistoricCaseInstanceQuery()
                .involvedUser("kermit")
                .involvedUser("fozzie", IdentityLinkType.ASSIGNEE)
                .involvedGroups(new HashSet<>(Arrays.asList("sales", "hr")))
                .involvedGroup("admin", IdentityLinkType.CANDIDATE)
                .or()
                    .involvedUser("kermitOr")
                    .involvedUser("fozzieOr", IdentityLinkType.ASSIGNEE)
                    .involvedGroups(new HashSet<>(Arrays.asList("salesOr", "hrOr")))
                    .involvedGroup("adminOr", IdentityLinkType.CANDIDATE)
                .endOr()
                .deleteSequentiallyUsingBatch(10, "Test");
        batchesToRemove.add(batchId);

        Batch batch = cmmnManagementService.createBatchQuery().batchId(batchId).singleResult();
        assertThat(batch).isNotNull();

        assertThatJson(batch.getBatchDocumentJson(ScopeTypes.CMMN))
                .when(Option.IGNORING_ARRAY_ORDER)
                .isEqualTo("{"
                        + "  numberOfInstances: 0,"
                        + "  batchSize: 10,"
                        + "  sequential: true,"
                        + "  query: {"
                        + "    involvedUser: 'kermit',"
                        + "    involvedUserIdentityLink: {"
                        + "      userId: 'fozzie',"
                        + "      type: 'assignee'"
                        + "    },"
                        + "    involvedGroups: [ 'hr', 'sales' ],"
                        + "    involvedGroupIdentityLink: {"
                        + "      groupId: 'admin',"
                        + "      type: 'candidate'"
                        + "    },"
                        + "    orQueryObjects: ["
                        + "      {"
                        + "        involvedUser: 'kermitOr',"
                        + "        involvedUserIdentityLink: {"
                        + "          userId: 'fozzieOr',"
                        + "          type: 'assignee'"
                        + "        },"
                        + "        involvedGroups: [ 'hrOr', 'salesOr' ],"
                        + "        involvedGroupIdentityLink: {"
                        + "          groupId: 'adminOr',"
                        + "          type: 'candidate'"
                        + "        }"
                        + "      }"
                        + "    ]"
                        + "  }"
                        + "}");
    }

    @Test
    public void testHistoryCleanupTimerJobCorrectlyUpdated() {
        String originalConfig = cmmnEngineConfiguration.getHistoryCleaningTimeCycleConfig();
        String initialConfig = "0 0 1 * * ?";
        cmmnEngineConfiguration.setHistoryCleaningTimeCycleConfig(initialConfig);

        try {
            assertThat(cmmnManagementService.createTimerJobQuery().handlerType(CmmnHistoryCleanupJobHandler.TYPE).count()).isZero();

            cmmnManagementService.handleHistoryCleanupTimerJob();

            assertThat(cmmnManagementService.createTimerJobQuery().handlerType(CmmnHistoryCleanupJobHandler.TYPE).count()).isEqualTo(1);
            Job job = cmmnManagementService.createTimerJobQuery().handlerType(CmmnHistoryCleanupJobHandler.TYPE).singleResult();
            TimerJobEntity timerJob = (TimerJobEntity) job;
            assertThat(timerJob.getRepeat()).isEqualTo(initialConfig);

            cmmnManagementService.handleHistoryCleanupTimerJob();
            assertThat(cmmnManagementService.createTimerJobQuery().handlerType(CmmnHistoryCleanupJobHandler.TYPE).count()).isEqualTo(1);
            job = cmmnManagementService.createTimerJobQuery().handlerType(CmmnHistoryCleanupJobHandler.TYPE).singleResult();
            assertThat(job.getId()).isEqualTo(timerJob.getId());

            cmmnEngineConfiguration.setHistoryCleaningTimeCycleConfig("0 0 2 * * ?");

            cmmnManagementService.handleHistoryCleanupTimerJob();
            assertThat(cmmnManagementService.createTimerJobQuery().handlerType(CmmnHistoryCleanupJobHandler.TYPE).count()).isEqualTo(1);
            job = cmmnManagementService.createTimerJobQuery().handlerType(CmmnHistoryCleanupJobHandler.TYPE).singleResult();
            assertThat(job.getId()).isNotEqualTo(timerJob.getId());

            timerJob = (TimerJobEntity) job;
            assertThat(timerJob.getRepeat()).isEqualTo("0 0 2 * * ?");

        } finally {
            cmmnEngineConfiguration.setHistoryCleaningTimeCycleConfig(originalConfig);
            cmmnManagementService.createTimerJobQuery().handlerType(CmmnHistoryCleanupJobHandler.TYPE).list()
                    .forEach(job -> cmmnManagementService.deleteTimerJob(job.getId()));
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/human-task-milestone-model.cmmn")
    public void testDeleteHistoricInstancesUsingBatchWithStoppedBatch() {
        List<String> caseInstanceIds = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();
            caseInstanceIds.add(caseInstance.getId());
            cmmnRuntimeService.setVariable(caseInstance.getId(), "testVar", "testValue" + (i + 1));
            cmmnRuntimeService.setVariable(caseInstance.getId(), "numVar", (i + 1));
            cmmnRuntimeService.setVariable(caseInstance.getId(), "serializableVar", new RepetitionVariableAggregationTest.TestSerializableVariable());
        }

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isEqualTo(20);

        }

        for (int i = 0; i < 10; i++) {
            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstanceIds.get(i)).singleResult();
            cmmnTaskService.setVariableLocal(task.getId(), "taskVar", "taskValue" + (i + 1));
            cmmnTaskService.setVariableLocal(task.getId(), "taskSerializableVar", new RepetitionVariableAggregationTest.TestSerializableVariable());
            cmmnTaskService.complete(task.getId());
        }

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricCaseInstanceQuery query = cmmnHistoryService.createHistoricCaseInstanceQuery();
            Calendar cal = new GregorianCalendar();
            cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) + 1);
            query.finishedBefore(cal.getTime());
            String batchId = query.deleteSequentiallyUsingBatch(5, "Test Deletion");
            batchesToRemove.add(batchId);

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isEqualTo(20);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().count()).isEqualTo(40);
            assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().count()).isEqualTo(20);

            Batch batch = cmmnManagementService.createBatchQuery().batchId(batchId).singleResult();
            assertThat(batch).isNotNull();
            assertThat(batch.getStatus()).isEqualTo(DeleteCaseInstanceBatchConstants.STATUS_IN_PROGRESS);
            assertThat(batch.getBatchType()).isEqualTo(Batch.HISTORIC_CASE_DELETE_TYPE);
            assertThat(batch.getCompleteTime()).isNull();

            assertThat(cmmnManagementService.createBatchPartQuery().list())
                    .hasSize(1)
                    .allSatisfy(part -> {
                        assertThat(part.getStatus()).isEqualTo(DeleteCaseInstanceBatchConstants.STATUS_WAITING);
                        assertThat(part.getType()).isEqualTo(DeleteCaseInstanceBatchConstants.BATCH_PART_DELETE_CASE_INSTANCES_TYPE);
                    });

            assertThat(cmmnManagementService.createJobQuery().list())
                    .hasSize(1)
                    .allSatisfy(job -> {
                        assertThat(job.getJobHandlerType()).isEqualTo(DeleteHistoricCaseInstancesSequentialJobHandler.TYPE);
                    });

            Job job = cmmnManagementService.createJobQuery().singleResult();
            cmmnManagementService.executeJob(job.getId());

            assertThat(cmmnManagementService.createBatchPartQuery().list())
                    .extracting(BatchPart::getStatus, BatchPart::getType)
                    .containsExactlyInAnyOrder(
                            tuple(DeleteCaseInstanceBatchConstants.STATUS_COMPLETED, DeleteCaseInstanceBatchConstants.BATCH_PART_DELETE_CASE_INSTANCES_TYPE),
                            tuple(DeleteCaseInstanceBatchConstants.STATUS_WAITING, DeleteCaseInstanceBatchConstants.BATCH_PART_DELETE_CASE_INSTANCES_TYPE)
                    );

            cmmnEngineConfiguration.getCommandExecutor().execute(commandContext -> {
                CommandContextUtil.getCmmnEngineConfiguration(commandContext)
                        .getBatchServiceConfiguration()
                        .getBatchService()
                        .completeBatch(batchId, DeleteCaseInstanceBatchConstants.STATUS_STOPPED);
                return null;
            });

            job = cmmnManagementService.createJobQuery().singleResult();
            cmmnManagementService.executeJob(job.getId());

            assertThat(cmmnManagementService.createJobQuery().list()).isEmpty();
            assertThat(cmmnManagementService.createTimerJobQuery().list()).isEmpty();
            assertThat(cmmnManagementService.createBatchPartQuery().list())
                    .extracting(BatchPart::getStatus, BatchPart::getType)
                    .containsExactlyInAnyOrder(
                            tuple(DeleteCaseInstanceBatchConstants.STATUS_COMPLETED, DeleteCaseInstanceBatchConstants.BATCH_PART_DELETE_CASE_INSTANCES_TYPE),
                            tuple(DeleteCaseInstanceBatchConstants.STATUS_STOPPED, DeleteCaseInstanceBatchConstants.BATCH_PART_DELETE_CASE_INSTANCES_TYPE)
                    );

            waitForAsyncHistoryExecutorToProcessAllJobs();
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isEqualTo(15);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().count()).isEqualTo(30);
            assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().count()).isEqualTo(15);

            for (int i = 0; i < 20; i++) {
                if (i < 5) {
                    assertThat(cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstanceIds.get(i))).isEmpty();
                    assertThat(cmmnHistoryService.createHistoricTaskLogEntryQuery().caseInstanceId(caseInstanceIds.get(i)).count()).isZero();
                    assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstanceIds.get(i)).count()).isZero();
                    assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstanceIds.get(i)).count()).isZero();
                } else if (i < 10) {
                    assertThat(cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstanceIds.get(i))).hasSize(1);
                    assertThat(cmmnHistoryService.createHistoricTaskLogEntryQuery().caseInstanceId(caseInstanceIds.get(i)).count()).isEqualTo(2);
                    assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstanceIds.get(i)).count()).isEqualTo(5);
                    assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstanceIds.get(i)).count()).isEqualTo(1);
                } else {
                    assertThat(cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstanceIds.get(i))).hasSize(1);
                    assertThat(cmmnHistoryService.createHistoricTaskLogEntryQuery().caseInstanceId(caseInstanceIds.get(i)).count()).isEqualTo(1);
                    assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstanceIds.get(i)).count()).isEqualTo(3);
                    assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstanceIds.get(i)).count()).isEqualTo(1);
                }
            }
        }
    }

}
