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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.history.HistoricPlanItemInstanceQuery;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.PlanItemLocalizationManager;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.cmmn.test.FlowableCmmnTestCase;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 */
public class HistoricPlanItemInstanceQueryTest extends FlowableCmmnTestCase {

    protected String deploymentId;
    protected String caseDefinitionId;

    @BeforeEach
    public void deployCaseDefinition() {
        deploymentId = addDeploymentForAutoCleanup(cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/history/HistoricPlanItemInstanceQueryTest.testQuery.cmmn")
                .deploy());
        caseDefinitionId = cmmnRepositoryService.createCaseDefinitionQuery()
                .deploymentId(deploymentId)
                .singleResult()
                .getId();
    }

    @Test
    public void testByCaseDefinitionId() {
        startInstances(5);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().list()).hasSize(20);
        }
    }

    @Test
    public void testByCaseInstanceId() {
        List<String> caseInstanceIds = startInstances(3);
        for (String caseInstanceId : caseInstanceIds) {
            if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
                assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstanceId).list()).hasSize(4);
            }
        }
    }

    @Test
    public void testByCaseInstanceIds() {
        List<String> caseInstanceIds = startInstances(3);
        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceIds(Set.of(caseInstanceIds.get(0),caseInstanceIds.get(1))).list()).hasSize(8);
        }
    }

    @Test
    public void testByStageInstanceId() {
        startInstances(1);
        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricPlanItemInstance planItemInstance = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceDefinitionType(PlanItemDefinitionType.STAGE)
                .planItemInstanceName("Stage one")
                .singleResult();
            assertThat(planItemInstance).isNotNull();
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceStageInstanceId(planItemInstance.getId()).count()).isEqualTo(2);
        }
    }

    @Test
    public void testByPlanItemInstanceId() {
        startInstances(1);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            List<HistoricPlanItemInstance> planItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery().list();
            for (HistoricPlanItemInstance planItemInstance : planItemInstances) {
                assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceId(planItemInstance.getId()).count()).isEqualTo(1);
            }
        }
    }

    @Test
    public void testByElementId() {
        startInstances(4);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceElementId("planItem3").list()).hasSize(4);
        }
    }

    @Test
    public void testByName() {
        startInstances(9);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceName("B").list()).hasSize(9);
        }
    }

    @Test
    public void testByState() {
        startInstances(1);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceState(PlanItemInstanceState.ACTIVE).list()).hasSize(2);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceState(PlanItemInstanceState.AVAILABLE).list()).hasSize(1);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceState(PlanItemInstanceState.ENABLED).list()).hasSize(1);
        }
    }

    @Test
    public void testByPlanItemDefinitionType() {
        startInstances(3);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceDefinitionType(PlanItemDefinitionType.HUMAN_TASK).list()).hasSize(6);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceDefinitionType(PlanItemDefinitionType.STAGE).list()).hasSize(6);
        }
    }

    @Test
    public void testByPlanItemDefinitionTypes() {
        startInstances(2);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceDefinitionTypes(Arrays.asList(PlanItemDefinitionType.STAGE, PlanItemDefinitionType.HUMAN_TASK)).list()).hasSize(8);
        }
    }

    @Test
    public void testByStateAndType() {
        startInstances(3);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .planItemInstanceDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .list())
                .hasSize(3);

            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceState(PlanItemInstanceState.ENABLED)
                .planItemInstanceDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .list())
                .hasSize(3);
        }
    }

    @Test
    public void testByAssignee() {
        startInstances(2);

        List<HistoricPlanItemInstance> planItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .list();
        assertThat(planItemInstances).hasSize(4);

        List<Task> tasks = cmmnTaskService.createTaskQuery().list();
        for (Task task : tasks) {
            cmmnTaskService.setAssignee(task.getId(), "gonzo");
        }

        planItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .planItemInstanceAssignee("gonzo")
                .list();
        assertThat(planItemInstances).hasSize(2);

        planItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .planItemInstanceAssignee("johnDoe")
                .list();
        assertThat(planItemInstances).hasSize(0);

    }

    @Test
    public void testByCompletedBy() {
        startInstances(3);

        List<Task> tasks = cmmnTaskService.createTaskQuery().list();
        for (Task task : tasks) {
            cmmnTaskService.setAssignee(task.getId(), "gonzo");
            cmmnTaskService.complete(task.getId(), "kermit");
        }

        List<HistoricPlanItemInstance> planItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .planItemInstanceAssignee("gonzo")
                .list();
        assertThat(planItemInstances).hasSize(3);

        planItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .planItemInstanceCompletedBy("kermit")
                .list();
        assertThat(planItemInstances).hasSize(3);

        planItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .planItemInstanceCompletedBy("johnDoe")
                .list();
        assertThat(planItemInstances).hasSize(0);

    }

    @Test
    public void testOrderBy() {
        startInstances(4);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByName().asc().list()).hasSize(16);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByName().desc().list()).hasSize(16);

            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByCreateTime().asc().list()).hasSize(16);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByCreateTime().desc().list()).hasSize(16);

            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByLastAvailableTime().asc().list()).hasSize(16);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByLastAvailableTime().desc().list()).hasSize(16);

            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByLastEnabledTime().asc().list()).hasSize(16);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByLastEnabledTime().desc().list()).hasSize(16);

            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByLastDisabledTime().asc().list()).hasSize(16);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByLastDisabledTime().desc().list()).hasSize(16);

            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByLastStartedTime().asc().list()).hasSize(16);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByLastStartedTime().desc().list()).hasSize(16);

            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByLastSuspendedTime().asc().list()).hasSize(16);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByLastSuspendedTime().desc().list()).hasSize(16);

            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByCompletedTime().asc().list()).hasSize(16);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByCompletedTime().desc().list()).hasSize(16);

            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByOccurredTime().asc().list()).hasSize(16);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByOccurredTime().desc().list()).hasSize(16);

            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByTerminatedTime().asc().list()).hasSize(16);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByTerminatedTime().desc().list()).hasSize(16);

            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByExitTime().asc().list()).hasSize(16);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByExitTime().desc().list()).hasSize(16);

            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByEndedTime().asc().list()).hasSize(16);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByEndedTime().desc().list()).hasSize(16);

            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByLastUpdatedTime().asc().list()).hasSize(16);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().orderByLastUpdatedTime().desc().list()).hasSize(16);
        }
    }

    @Test
    public void testLocalization() {
        startInstances(1);

        cmmnEngineConfiguration.setPlanItemLocalizationManager(new PlanItemLocalizationManager() {
            @Override
            public void localize(PlanItemInstance planItemInstance, String locale, boolean withLocalizationFallback) {

            }

            @Override
            public void localize(HistoricPlanItemInstance historicPlanItemInstance, String locale, boolean withLocalizationFallback) {
                if ("pt".equals(locale)) {
                    historicPlanItemInstance.setLocalizedName("Plano traduzido");
                }
            }
        });

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().list())
                    .extracting(HistoricPlanItemInstance::getName)
                    .containsExactlyInAnyOrder(
                            "Stage one",
                            "Stage two",
                            "A",
                            "B"
                    );
    
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().locale("pt").list())
                    .extracting(HistoricPlanItemInstance::getName)
                    .containsExactlyInAnyOrder(
                            "Plano traduzido",
                            "Plano traduzido",
                            "Plano traduzido",
                            "Plano traduzido"
                    );
        }
    }

    @Test
    public void testIncludeLocalVariables() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testQuery")
                .variable("caseVar","caseVarValur")
                .name("With string value")
                .start();

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().list();

        cmmnRuntimeService.setLocalVariable(planItemInstances.get(0).getId(), "localVar", "someValue");

        Task task = cmmnTaskService.createTaskQuery()
                .includeCaseVariables()
                .includeTaskLocalVariables()
                .singleResult();

        cmmnTaskService.complete(task.getId());
        HistoricPlanItemInstance planItemInstance = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceId(planItemInstances.get(0).getId()).singleResult();
        assertThat(planItemInstance.getPlanItemInstanceLocalVariables()).isEmpty();

        planItemInstance = cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceId(planItemInstances.get(0).getId()).includeLocalVariables()
                .singleResult();
        assertThat(planItemInstance.getPlanItemInstanceLocalVariables()).isNotNull();

        assertThat(planItemInstance.getPlanItemInstanceLocalVariables()).containsOnly(
                entry("localVar", "someValue")
        );
    }

    @Test
    @CmmnDeployment
    public void testByStarted() {
        // A starts immediately (active), B is gated behind an always-false entry criterion so it stays available and never starts
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testByStarted").start();

        // Complete A so it is started + ended, then terminate the case so B ends without ever being started
        Task taskA = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(taskA.getId());
        cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            List<HistoricPlanItemInstance> all = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                    .planItemInstanceCaseInstanceId(caseInstance.getId())
                    .orderByName().asc()
                    .list();
            assertThat(all).extracting(HistoricPlanItemInstance::getName).containsExactly("A", "B");

            List<HistoricPlanItemInstance> started = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                    .planItemInstanceCaseInstanceId(caseInstance.getId())
                    .started()
                    .list();
            assertThat(started)
                    .extracting(HistoricPlanItemInstance::getName)
                    .containsExactly("A");
            assertThat(started).allSatisfy(pi -> assertThat(pi.getLastStartedTime()).isNotNull());

            List<HistoricPlanItemInstance> notStarted = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                    .planItemInstanceCaseInstanceId(caseInstance.getId())
                    .notStarted()
                    .list();
            assertThat(notStarted)
                    .extracting(HistoricPlanItemInstance::getName)
                    .containsExactly("B");
            assertThat(notStarted).allSatisfy(pi -> assertThat(pi.getLastStartedTime()).isNull());

            // started and notStarted partition the full result set
            assertThat(started.size() + notStarted.size()).isEqualTo(all.size());
        }
    }

    @Test
    public void testOrQueryUnionWithBogusCondition() {
        List<String> caseInstanceIds = startInstances(2);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            // (bogus case definition id) OR (real case instance id) => only the 4 plan items of the first case instance
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                    .or()
                        .planItemInstanceCaseDefinitionId("undefinedId")
                        .planItemInstanceCaseInstanceId(caseInstanceIds.get(0))
                    .endOr()
                    .list()).hasSize(4);

            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                    .or()
                        .planItemInstanceCaseDefinitionId("undefinedId")
                        .planItemInstanceCaseInstanceId(caseInstanceIds.get(0))
                    .endOr()
                    .count()).isEqualTo(4);
        }
    }

    @Test
    public void testOrQueryUnionAcrossColumns() {
        List<String> caseInstanceIds = startInstances(2);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            // (all 4 plan items of case 0) OR (the 2 stages of every case) => 4 (case 0) + 2 (stages of case 1) = 6, overlap deduped
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                    .or()
                        .planItemInstanceCaseInstanceId(caseInstanceIds.get(0))
                        .planItemInstanceDefinitionType(PlanItemDefinitionType.STAGE)
                    .endOr()
                    .list()).hasSize(6);
        }
    }

    @Test
    public void testOrQueryWithCaseInstanceIds() {
        List<String> caseInstanceIds = startInstances(3);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            // (two case instances via id set) OR (bogus) => 8 plan items (4 per case instance)
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                    .or()
                        .planItemInstanceCaseInstanceIds(Set.of(caseInstanceIds.get(0), caseInstanceIds.get(1)))
                        .planItemInstanceCaseDefinitionId("undefinedId")
                    .endOr()
                    .list()).hasSize(8);
        }
    }

    @Test
    public void testOuterAndWithOr() {
        startInstances(2);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            // type == HUMAN_TASK AND (bogus OR real case definition id) => the 2 human tasks per case = 4
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                    .planItemInstanceDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                    .or()
                        .planItemInstanceCaseInstanceId("undefinedId")
                        .planItemInstanceCaseDefinitionId(caseDefinitionId)
                    .endOr()
                    .list()).hasSize(4);
        }
    }

    @Test
    public void testOrQueryCoversManyParameters() {
        List<String> caseInstanceIds = startInstances(2);

        if (!CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            return;
        }

        String firstCaseInstanceId = caseInstanceIds.get(0);

        // Resolve a couple of concrete ids from the first case instance to drive exact-match OR branches
        HistoricPlanItemInstance taskA = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceCaseInstanceId(firstCaseInstanceId)
                .planItemInstanceName("A")
                .singleResult();
        HistoricPlanItemInstance stageOne = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceCaseInstanceId(firstCaseInstanceId)
                .planItemInstanceName("Stage one")
                .singleResult();

        // Each OR pairs a real filter with a bogus one, so the result equals the real filter alone.
        // This exercises the OR SQL fragment of every listed column across list() and count().

        // planItemInstanceId => exactly 1
        assertOr(1, q -> q.planItemInstanceId(taskA.getId()));
        // planItemInstanceName => 1 per case instance
        assertOr(2, q -> q.planItemInstanceName("A"));
        assertOr(2, q -> q.planItemInstanceName("Stage one"));
        // planItemInstanceElementId => 1 per case instance
        assertOr(2, q -> q.planItemInstanceElementId("planItem3"));
        assertOr(2, q -> q.planItemInstanceElementId("planItem7"));
        // planItemInstanceState
        assertOr(4, q -> q.planItemInstanceState(PlanItemInstanceState.ACTIVE));
        assertOr(2, q -> q.planItemInstanceState(PlanItemInstanceState.AVAILABLE));
        assertOr(2, q -> q.planItemInstanceState(PlanItemInstanceState.ENABLED));
        // definition type / types
        assertOr(4, q -> q.planItemInstanceDefinitionType(PlanItemDefinitionType.STAGE));
        assertOr(4, q -> q.planItemInstanceDefinitionType(PlanItemDefinitionType.HUMAN_TASK));
        assertOr(8, q -> q.planItemInstanceDefinitionTypes(Arrays.asList(PlanItemDefinitionType.STAGE, PlanItemDefinitionType.HUMAN_TASK)));
        // stageInstanceId => the 2 human tasks (A, B) inside Stage one of the first case instance
        assertOr(2, q -> q.planItemInstanceStageInstanceId(stageOne.getId()));
        // date ranges over the whole set (8 plan items for 2 case instances)
        assertOr(8, q -> q.createdAfter(new Date(0)));
        assertOr(8, q -> q.createdBefore(new Date(System.currentTimeMillis() + 3600_000L)));
        // tenant (deployment has no tenant)
        assertOr(8, q -> q.planItemInstanceWithoutTenantId());
    }

    @Test
    public void testOrQueryUnionAcrossDistinctColumns() {
        startInstances(2);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            // (name == A) OR (elementId == planItem3 i.e. Stage one) => two distinct rows per case instance = 4
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                    .or()
                        .planItemInstanceName("A")
                        .planItemInstanceElementId("planItem3")
                    .endOr()
                    .list()).hasSize(4);
        }
    }

    /**
     * Runs {@code or(filter OR bogusCaseDefinitionId).endOr()} and asserts both list() and count() return expectedSize.
     * Pairing the real filter with a non-matching one isolates the real filter's OR branch.
     */
    private void assertOr(int expectedSize, java.util.function.UnaryOperator<HistoricPlanItemInstanceQuery> filter) {
        HistoricPlanItemInstanceQuery listQuery = cmmnHistoryService.createHistoricPlanItemInstanceQuery().or();
        filter.apply(listQuery).planItemInstanceCaseDefinitionId("undefinedId").endOr();
        assertThat(listQuery.list()).hasSize(expectedSize);

        HistoricPlanItemInstanceQuery countQuery = cmmnHistoryService.createHistoricPlanItemInstanceQuery().or();
        filter.apply(countQuery).planItemInstanceCaseDefinitionId("undefinedId").endOr();
        assertThat(countQuery.count()).isEqualTo(expectedSize);
    }

    @Test
    public void testOrThrowsWhenAlreadyInOrStatement() {
        assertThatThrownBy(() -> cmmnHistoryService.createHistoricPlanItemInstanceQuery().or().or())
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("The query is already in an or statement");
    }

    @Test
    public void testEndOrThrowsWhenNotInOrStatement() {
        assertThatThrownBy(() -> cmmnHistoryService.createHistoricPlanItemInstanceQuery().endOr())
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("endOr() can only be called after calling or()");
    }

    private List<String> startInstances(int numberOfInstances) {
        List<String> caseInstanceIds = new ArrayList<>();
        for (int i = 0; i < numberOfInstances; i++) {
            caseInstanceIds.add(cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testQuery").start().getId());
        }
        return caseInstanceIds;
    }

}
