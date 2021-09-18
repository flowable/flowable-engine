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
package org.flowable.cmmn.test.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.api.StageResponse;
import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.api.runtime.UserEventListenerInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.task.api.Task;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class StageTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment
    public void testOneNestedStage() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .orderByName().asc()
                .list();
        String[] expectedNames = new String[] { "Stage One", "Task One", "Task Three", "Task Two" };
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactly(expectedNames);

        // Finishing task 2 and 3 should complete the nested stage
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(2).getId());
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(3).getId());

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .orderByName().asc()
                .list();
        assertThat(planItemInstances).hasSize(1);
        expectedNames = new String[] { "Task One" };
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactly(expectedNames);

        // Finish case instance
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(0).getId());
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(1);
        }
    }

    @Test
    @CmmnDeployment
    public void testOneNestedStageNonBlocking() {
        cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(1);
        }
    }

    @Test
    @CmmnDeployment
    public void testTwoNestedStages() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .orderByName().asc()
                .list();
        String[] expectedNames = new String[] { "Stage One", "Stage Two", "Task Four", "Task One", "Task Three", "Task Two" };
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactly(expectedNames);

        // Complete inner nested stage
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(2).getId());
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .orderByName().asc()
                .list();
        expectedNames = new String[] { "Stage One", "Task One", "Task Three", "Task Two" };
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactly(expectedNames);
    }

    @Test
    @CmmnDeployment
    public void testTwoNestedStagesNonBlocking() {
        cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(1);
        }
    }

    @Test
    @CmmnDeployment
    public void testThreeNestedStages() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .orderByName().asc()
                .list();
        String[] expectedNames = new String[] { "Stage One", "Stage Three", "Stage Two",
                "Task Five", "Task Four", "Task One", "Task Three", "Task Two" };
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactly(expectedNames);

        // Complete inner nested stage (3th stage)
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(3).getId());
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .orderByName().asc()
                .list();
        expectedNames = new String[] { "Stage One", "Stage Two", "Task Four", "Task One",
                "Task Three", "Task Two" };
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactly(expectedNames);

        // Complete inner nested stage (2nd stage)
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(2).getId());
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .orderByName().asc()
                .list();
        expectedNames = new String[] { "Stage One", "Task One", "Task Three", "Task Two" };
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactly(expectedNames);

        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(2).getId());
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(3).getId());

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .orderByName().asc()
                .list();
        expectedNames = new String[] { "Task One" };
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactly(expectedNames);

        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(0).getId());
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(1);
        }
    }

    @Test
    @CmmnDeployment
    public void testThreeNestedStagesNonBlocking() {
        cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(1);
        }
    }

    @Test
    @CmmnDeployment
    public void testThreeNestedStagesWithCriteria() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .orderByName().asc()
                .list();
        String[] expectedNames = new String[] { "Stage A", "Task A", "Task B", "Task C" };
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactly(expectedNames);

        // Completing A and B triggers stage 2
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(1).getId());
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(2).getId());

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .orderByName().asc()
                .list();
        expectedNames = new String[] { "Stage A", "Stage B", "Stage C", "Task C", "Task D", "Task E" };
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactly(expectedNames);

        // Triggering Task C should exit stage 2, which should also exit the inner nested stage
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(3).getId());

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(1);
        }
    }

    @Test
    @CmmnDeployment
    public void testTerminateCaseInstanceNestedStages() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isEqualTo(8);

        cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(1);
        }
    }

    @Test
    @CmmnDeployment
    public void testRepeatingTerminatedStage() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();

        List<PlanItemInstance> activeStages = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).onlyStages()
                .planItemInstanceStateActive().list();
        assertThat(activeStages)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactly("expandedStage2");

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().includeEnded().list();
        assertPlanItemInstanceState(planItemInstances, "in progress", PlanItemInstanceState.AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Hidden", PlanItemInstanceState.ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Close", PlanItemInstanceState.AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Draft", PlanItemInstanceState.COMPLETED);
        assertPlanItemInstanceState(planItemInstances, "Close Task", PlanItemInstanceState.ACTIVE, PlanItemInstanceState.WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Service task 1", PlanItemInstanceState.AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Service task 2", PlanItemInstanceState.COMPLETED);

        Task closeTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(closeTask).isNotNull();
        cmmnTaskService.complete(closeTask.getId());

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().includeEnded().list();
        assertPlanItemInstanceState(planItemInstances, "in progress", PlanItemInstanceState.TERMINATED, PlanItemInstanceState.WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Hidden", PlanItemInstanceState.TERMINATED);
        assertPlanItemInstanceState(planItemInstances, "Close", PlanItemInstanceState.ACTIVE, PlanItemInstanceState.WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Draft", PlanItemInstanceState.COMPLETED);
        assertPlanItemInstanceState(planItemInstances, "Close Task", PlanItemInstanceState.COMPLETED, PlanItemInstanceState.TERMINATED);
        assertPlanItemInstanceState(planItemInstances, "Service task 1", PlanItemInstanceState.TERMINATED);
        assertPlanItemInstanceState(planItemInstances, "Service task 2", PlanItemInstanceState.COMPLETED);
        assertPlanItemInstanceState(planItemInstances, "Reopen Task", PlanItemInstanceState.ACTIVE);

        activeStages = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).onlyStages()
                .planItemInstanceStateActive().list();
        assertThat(activeStages)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactly("expandedStage3");

        Task reopenTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(reopenTask).isNotNull();
        cmmnTaskService.complete(reopenTask.getId());

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().includeEnded().list();
        assertPlanItemInstanceState(planItemInstances, "in progress", PlanItemInstanceState.TERMINATED, PlanItemInstanceState.WAITING_FOR_REPETITION,
                PlanItemInstanceState.ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Hidden", PlanItemInstanceState.TERMINATED, PlanItemInstanceState.ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Close", PlanItemInstanceState.TERMINATED, PlanItemInstanceState.WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Draft", PlanItemInstanceState.COMPLETED);
        assertPlanItemInstanceState(planItemInstances, "Close Task", PlanItemInstanceState.COMPLETED, PlanItemInstanceState.TERMINATED,
                PlanItemInstanceState.AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Service task 1", PlanItemInstanceState.TERMINATED, PlanItemInstanceState.COMPLETED,
                PlanItemInstanceState.WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Service task 2", PlanItemInstanceState.COMPLETED);
        assertPlanItemInstanceState(planItemInstances, "Reopen Task", PlanItemInstanceState.COMPLETED, PlanItemInstanceState.TERMINATED);
        assertPlanItemInstanceState(planItemInstances, "In progress Task", PlanItemInstanceState.ACTIVE);

        activeStages = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).onlyStages()
                .planItemInstanceStateActive().list();
        assertThat(activeStages)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactlyInAnyOrder("expandedStage1", "expandedStage2");

        Task inProgressTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(inProgressTask).isNotNull();
    }

    @Test
    @CmmnDeployment
    public void testStageFlagSet() {
        Date now = new Date();
        setClockTo(now);
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testCase")
                .variable("showInStage", true)
                .start();

        CaseInstance testStagesOnly = cmmnRuntimeService.createCaseInstanceQuery()
                .caseDefinitionKey("testCase")
                .singleResult();
        assertThat(testStagesOnly).isNotNull();

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(testStagesOnly.getId()).planItemDefinitionType("stage").count())
                .isEqualTo(3);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(testStagesOnly.getId()).onlyStages().count()).isEqualTo(3);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(testStagesOnly.getId()).onlyStages().list())
                .extracting(PlanItemInstance::isStage).containsOnly(true);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery().onlyStages().list();
            assertThat(historicPlanItemInstances).hasSize(3);
            Map<String, Boolean> stageIncludeInOverviewMap = new HashMap<>();
            for (HistoricPlanItemInstance historicPlanItemInstance : historicPlanItemInstances) {
                stageIncludeInOverviewMap.put(historicPlanItemInstance.getName(), historicPlanItemInstance.isShowInOverview());
            }

            assertThat(stageIncludeInOverviewMap.get("Stage 1")).isTrue();
            assertThat(stageIncludeInOverviewMap.get("Stage 1.1")).isFalse();
            assertThat(stageIncludeInOverviewMap.get("Stage 2")).isTrue();
        }

        cmmnRuntimeService
                .completeStagePlanItemInstance(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("Stage 1").singleResult().getId());

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery().onlyStages().list();
            assertThat(historicPlanItemInstances).hasSize(4);
            Map<String, Boolean> stageIncludeInOverviewMap = new HashMap<>();
            for (HistoricPlanItemInstance historicPlanItemInstance : historicPlanItemInstances) {
                stageIncludeInOverviewMap.put(historicPlanItemInstance.getName(), historicPlanItemInstance.isShowInOverview());
            }

            assertThat(stageIncludeInOverviewMap.get("Stage 1")).isTrue();
            assertThat(stageIncludeInOverviewMap.get("Stage 1.1")).isFalse();
            assertThat(stageIncludeInOverviewMap.get("Stage 2")).isTrue();
            assertThat(stageIncludeInOverviewMap.get("Stage 2.1")).isTrue();
        }
    }

    @Test
    @CmmnDeployment
    public void testGetStageOverview() {
        Date now = new Date();
        setClockTo(now);
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testCase")
                .variable("showInStage", true)
                .start();

        List<StageResponse> stages = cmmnRuntimeService.getStageOverview(caseInstance.getId());
        assertThat(stages).hasSize(3);

        Map<String, StageResponse> stageMap = new HashMap<>();
        for (StageResponse stageResponse : stages) {
            stageMap.put(stageResponse.getName(), stageResponse);
        }

        assertThat(stageMap.get("Stage 1").getName()).isEqualTo("Stage 1");
        assertThat(stageMap.get("Stage 1").isCurrent()).isTrue();
        assertThat(stageMap.get("Stage 1").isEnded()).isFalse();
        assertThat(stageMap.get("Stage 1").getEndTime()).isNull();
        assertThat(stageMap.get("Stage 2").getName()).isEqualTo("Stage 2");
        assertThat(stageMap.get("Stage 2").isCurrent()).isFalse();
        assertThat(stageMap.get("Stage 2").isEnded()).isFalse();
        assertThat(stageMap.get("Stage 2").getEndTime()).isNull();
        assertThat(stageMap.get("Stage 2.1").getName()).isEqualTo("Stage 2.1");
        assertThat(stageMap.get("Stage 2.1").isCurrent()).isFalse();
        assertThat(stageMap.get("Stage 2.1").isEnded()).isFalse();
        assertThat(stageMap.get("Stage 2.1").getEndTime()).isNull();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            stages = cmmnHistoryService.getStageOverview(caseInstance.getId());
            assertThat(stages).hasSize(3);

            stageMap = new HashMap<>();
            for (StageResponse stageResponse : stages) {
                stageMap.put(stageResponse.getName(), stageResponse);
            }

            assertThat(stageMap.get("Stage 1").getName()).isEqualTo("Stage 1");
            assertThat(stageMap.get("Stage 1").isCurrent()).isTrue();
            assertThat(stageMap.get("Stage 1").isEnded()).isFalse();
            assertThat(stageMap.get("Stage 1").getEndTime()).isNull();

            assertThat(stageMap.get("Stage 2").getName()).isEqualTo("Stage 2");
            assertThat(stageMap.get("Stage 2").isCurrent()).isFalse();
            assertThat(stageMap.get("Stage 2").isEnded()).isFalse();
            assertThat(stageMap.get("Stage 2").getEndTime()).isNull();

            assertThat(stageMap.get("Stage 2.1").getName()).isEqualTo("Stage 2.1");
            assertThat(stageMap.get("Stage 2.1").isCurrent()).isFalse();
            assertThat(stageMap.get("Stage 2.1").isEnded()).isFalse();
            assertThat(stageMap.get("Stage 2.1").getEndTime()).isNull();
        }

        cmmnRuntimeService
                .completeStagePlanItemInstance(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("Stage 1").singleResult().getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            stages = cmmnHistoryService.getStageOverview(caseInstance.getId());
            assertThat(stages).hasSize(3);

            stageMap = new HashMap<>();
            for (StageResponse stageResponse : stages) {
                stageMap.put(stageResponse.getName(), stageResponse);
            }

            assertThat(stageMap.get("Stage 1").getName()).isEqualTo("Stage 1");
            assertThat(stageMap.get("Stage 1").isCurrent()).isFalse();
            assertThat(stageMap.get("Stage 1").isEnded()).isTrue();
            assertThat(stageMap.get("Stage 1").getEndTime()).isNotNull();
            assertThat(stageMap.get("Stage 2").getName()).isEqualTo("Stage 2");
            assertThat(stageMap.get("Stage 2").isCurrent()).isFalse();
            assertThat(stageMap.get("Stage 2").isEnded()).isTrue();
            assertThat(stageMap.get("Stage 2").getEndTime()).isNotNull();
            assertThat(stageMap.get("Stage 2.1").getName()).isEqualTo("Stage 2.1");
            assertThat(stageMap.get("Stage 2.1").isCurrent()).isFalse();
            assertThat(stageMap.get("Stage 2.1").isEnded()).isTrue();
            assertThat(stageMap.get("Stage 2.1").getEndTime()).isNotNull();
        }
    }

    @Test
    @CmmnDeployment
    public void testGetStageAndMilestoneOverview() {
        Date now = new Date();
        setClockTo(now);
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testCase")
                .variable("showInStage", true)
                .variable("showMilestoneInOverview", true)
                .start();

        List<StageResponse> stages = cmmnRuntimeService.getStageOverview(caseInstance.getId());
        assertThat(stages).hasSize(5);

        Map<String, StageResponse> stageMap = new HashMap<>();
        for (StageResponse stageResponse : stages) {
            stageMap.put(stageResponse.getName(), stageResponse);
        }

        assertThat(stageMap.get("Milestone 1").getName()).isEqualTo("Milestone 1");
        assertThat(stageMap.get("Milestone 1").isCurrent()).isFalse();
        assertThat(stageMap.get("Milestone 1").isEnded()).isTrue();
        assertThat(stageMap.get("Milestone 1").getEndTime()).isNotNull();

        assertThat(stageMap.get("Stage 1").getName()).isEqualTo("Stage 1");
        assertThat(stageMap.get("Stage 1").isCurrent()).isTrue();
        assertThat(stageMap.get("Stage 1").isEnded()).isFalse();
        assertThat(stageMap.get("Stage 1").getEndTime()).isNull();

        assertThat(stageMap.get("Stage 2").getName()).isEqualTo("Stage 2");
        assertThat(stageMap.get("Stage 2").isCurrent()).isFalse();
        assertThat(stageMap.get("Stage 2").isEnded()).isFalse();
        assertThat(stageMap.get("Stage 2").getEndTime()).isNull();

        assertThat(stageMap.get("Milestone 2.1").getName()).isEqualTo("Milestone 2.1");
        assertThat(stageMap.get("Milestone 2.1").isCurrent()).isFalse();
        assertThat(stageMap.get("Milestone 2.1").isEnded()).isFalse();
        assertThat(stageMap.get("Milestone 2.1").getEndTime()).isNull();

        assertThat(stageMap.get("Stage 2.1").getName()).isEqualTo("Stage 2.1");
        assertThat(stageMap.get("Stage 2.1").isCurrent()).isFalse();
        assertThat(stageMap.get("Stage 2.1").isEnded()).isFalse();
        assertThat(stageMap.get("Stage 2.1").getEndTime()).isNull();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            stages = cmmnHistoryService.getStageOverview(caseInstance.getId());
            assertThat(stages).hasSize(5);

            stageMap = new HashMap<>();
            for (StageResponse stageResponse : stages) {
                stageMap.put(stageResponse.getName(), stageResponse);
            }

            assertThat(stageMap.get("Milestone 1").getName()).isEqualTo("Milestone 1");
            assertThat(stageMap.get("Milestone 1").isCurrent()).isFalse();
            assertThat(stageMap.get("Milestone 1").isEnded()).isTrue();
            assertThat(stageMap.get("Milestone 1").getEndTime()).isNotNull();

            assertThat(stageMap.get("Stage 1").getName()).isEqualTo("Stage 1");
            assertThat(stageMap.get("Stage 1").isCurrent()).isTrue();
            assertThat(stageMap.get("Stage 1").isEnded()).isFalse();
            assertThat(stageMap.get("Stage 1").getEndTime()).isNull();

            assertThat(stageMap.get("Stage 2").getName()).isEqualTo("Stage 2");
            assertThat(stageMap.get("Stage 2").isCurrent()).isFalse();
            assertThat(stageMap.get("Stage 2").isEnded()).isFalse();
            assertThat(stageMap.get("Stage 2").getEndTime()).isNull();

            assertThat(stageMap.get("Milestone 2.1").getName()).isEqualTo("Milestone 2.1");
            assertThat(stageMap.get("Milestone 2.1").isCurrent()).isFalse();
            assertThat(stageMap.get("Milestone 2.1").isEnded()).isFalse();
            assertThat(stageMap.get("Milestone 2.1").getEndTime()).isNull();

            assertThat(stageMap.get("Stage 2.1").getName()).isEqualTo("Stage 2.1");
            assertThat(stageMap.get("Stage 2.1").isCurrent()).isFalse();
            assertThat(stageMap.get("Stage 2.1").isEnded()).isFalse();
            assertThat(stageMap.get("Stage 2.1").getEndTime()).isNull();
        }


        cmmnRuntimeService
                .completeStagePlanItemInstance(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("Stage 1").singleResult().getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            stages = cmmnHistoryService.getStageOverview(caseInstance.getId());
            assertThat(stages).hasSize(5);

            stageMap = new HashMap<>();
            for (StageResponse stageResponse : stages) {
                stageMap.put(stageResponse.getName(), stageResponse);
            }

            assertThat(stageMap.get("Milestone 1").getName()).isEqualTo("Milestone 1");
            assertThat(stageMap.get("Milestone 1").isCurrent()).isFalse();
            assertThat(stageMap.get("Milestone 1").isEnded()).isTrue();
            assertThat(stageMap.get("Milestone 1").getEndTime()).isNotNull();

            assertThat(stageMap.get("Stage 1").getName()).isEqualTo("Stage 1");
            assertThat(stageMap.get("Stage 1").isCurrent()).isFalse();
            assertThat(stageMap.get("Stage 1").isEnded()).isTrue();
            assertThat(stageMap.get("Stage 1").getEndTime()).isNotNull();

            assertThat(stageMap.get("Stage 2").getName()).isEqualTo("Stage 2");
            assertThat(stageMap.get("Stage 2").isCurrent()).isFalse();
            assertThat(stageMap.get("Stage 2").isEnded()).isTrue();
            assertThat(stageMap.get("Stage 2").getEndTime()).isNotNull();

            assertThat(stageMap.get("Milestone 2.1").getName()).isEqualTo("Milestone 2.1");
            assertThat(stageMap.get("Milestone 2.1").isCurrent()).isFalse();
            assertThat(stageMap.get("Milestone 2.1").isEnded()).isTrue();
            assertThat(stageMap.get("Milestone 2.1").getEndTime()).isNotNull();

            assertThat(stageMap.get("Stage 2.1").getName()).isEqualTo("Stage 2.1");
            assertThat(stageMap.get("Stage 2.1").isCurrent()).isFalse();
            assertThat(stageMap.get("Stage 2.1").isEnded()).isTrue();
            assertThat(stageMap.get("Stage 2.1").getEndTime()).isNotNull();
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/StageTest.testGetStageAndMilestoneOverview.cmmn")
    public void testGetStageAndMilestoneOverviewWhenCaseIsTerminated() {
        Date now = new Date();
        setClockTo(now);
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testCase")
                .variable("showInStage", true)
                .variable("showMilestoneInOverview", true)
                .start();

        List<StageResponse> stages = cmmnRuntimeService.getStageOverview(caseInstance.getId());
        assertThat(stages).hasSize(5);

        Map<String, StageResponse> stageMap = new HashMap<>();
        for (StageResponse stageResponse : stages) {
            stageMap.put(stageResponse.getName(), stageResponse);
        }

        assertThat(stageMap.get("Milestone 1").getName()).isEqualTo("Milestone 1");
        assertThat(stageMap.get("Milestone 1").isCurrent()).isFalse();
        assertThat(stageMap.get("Milestone 1").isEnded()).isTrue();
        assertThat(stageMap.get("Milestone 1").getEndTime()).isNotNull();

        assertThat(stageMap.get("Stage 1").getName()).isEqualTo("Stage 1");
        assertThat(stageMap.get("Stage 1").isCurrent()).isTrue();
        assertThat(stageMap.get("Stage 1").isEnded()).isFalse();
        assertThat(stageMap.get("Stage 1").getEndTime()).isNull();

        assertThat(stageMap.get("Stage 2").getName()).isEqualTo("Stage 2");
        assertThat(stageMap.get("Stage 2").isCurrent()).isFalse();
        assertThat(stageMap.get("Stage 2").isEnded()).isFalse();
        assertThat(stageMap.get("Stage 2").getEndTime()).isNull();

        assertThat(stageMap.get("Milestone 2.1").getName()).isEqualTo("Milestone 2.1");
        assertThat(stageMap.get("Milestone 2.1").isCurrent()).isFalse();
        assertThat(stageMap.get("Milestone 2.1").isEnded()).isFalse();
        assertThat(stageMap.get("Milestone 2.1").getEndTime()).isNull();

        assertThat(stageMap.get("Stage 2.1").getName()).isEqualTo("Stage 2.1");
        assertThat(stageMap.get("Stage 2.1").isCurrent()).isFalse();
        assertThat(stageMap.get("Stage 2.1").isEnded()).isFalse();
        assertThat(stageMap.get("Stage 2.1").getEndTime()).isNull();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            stages = cmmnHistoryService.getStageOverview(caseInstance.getId());
            assertThat(stages).hasSize(5);

            stageMap = new HashMap<>();
            for (StageResponse stageResponse : stages) {
                stageMap.put(stageResponse.getName(), stageResponse);
            }

            assertThat(stageMap.get("Milestone 1").getName()).isEqualTo("Milestone 1");
            assertThat(stageMap.get("Milestone 1").isCurrent()).isFalse();
            assertThat(stageMap.get("Milestone 1").isEnded()).isTrue();
            assertThat(stageMap.get("Milestone 1").getEndTime()).isNotNull();

            assertThat(stageMap.get("Stage 1").getName()).isEqualTo("Stage 1");
            assertThat(stageMap.get("Stage 1").isCurrent()).isTrue();
            assertThat(stageMap.get("Stage 1").isEnded()).isFalse();
            assertThat(stageMap.get("Stage 1").getEndTime()).isNull();

            assertThat(stageMap.get("Stage 2").getName()).isEqualTo("Stage 2");
            assertThat(stageMap.get("Stage 2").isCurrent()).isFalse();
            assertThat(stageMap.get("Stage 2").isEnded()).isFalse();
            assertThat(stageMap.get("Stage 2").getEndTime()).isNull();

            assertThat(stageMap.get("Milestone 2.1").getName()).isEqualTo("Milestone 2.1");
            assertThat(stageMap.get("Milestone 2.1").isCurrent()).isFalse();
            assertThat(stageMap.get("Milestone 2.1").isEnded()).isFalse();
            assertThat(stageMap.get("Milestone 2.1").getEndTime()).isNull();

            assertThat(stageMap.get("Stage 2.1").getName()).isEqualTo("Stage 2.1");
            assertThat(stageMap.get("Stage 2.1").isCurrent()).isFalse();
            assertThat(stageMap.get("Stage 2.1").isEnded()).isFalse();
            assertThat(stageMap.get("Stage 2.1").getEndTime()).isNull();
        }

        cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            stages = cmmnHistoryService.getStageOverview(caseInstance.getId());
            assertThat(stages).hasSize(3);

            stageMap = new HashMap<>();
            for (StageResponse stageResponse : stages) {
                stageMap.put(stageResponse.getName(), stageResponse);
            }

            assertThat(stageMap.get("Milestone 1").getName()).isEqualTo("Milestone 1");
            assertThat(stageMap.get("Milestone 1").isCurrent()).isFalse();
            assertThat(stageMap.get("Milestone 1").isEnded()).isTrue();
            assertThat(stageMap.get("Milestone 1").getEndTime()).isNotNull();

            assertThat(stageMap.get("Stage 1").getName()).isEqualTo("Stage 1");
            assertThat(stageMap.get("Stage 1").isCurrent()).isFalse();
            assertThat(stageMap.get("Stage 1").isEnded()).isTrue();
            assertThat(stageMap.get("Stage 1").getEndTime()).isNotNull();

            assertThat(stageMap.get("Stage 2").getName()).isEqualTo("Stage 2");
            assertThat(stageMap.get("Stage 2").isCurrent()).isFalse();
            assertThat(stageMap.get("Stage 2").isEnded()).isTrue();
            assertThat(stageMap.get("Stage 2").getEndTime()).isNotNull();
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/StageTest.testGetStageAndMilestoneOverview.cmmn")
    public void testGetStageAndMilestoneOverviewWhenExpressionIsFalse() {
        Date now = new Date();
        setClockTo(now);
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testCase")
                .variable("showInStage", false)
                .variable("showMilestoneInOverview", false)
                .start();

        List<StageResponse> stages = cmmnRuntimeService.getStageOverview(caseInstance.getId());
        assertThat(stages).hasSize(3);

        Map<String, StageResponse> stageMap = new HashMap<>();
        for (StageResponse stageResponse : stages) {
            stageMap.put(stageResponse.getName(), stageResponse);
        }

        assertThat(stageMap.get("Milestone 1").getName()).isEqualTo("Milestone 1");
        assertThat(stageMap.get("Milestone 1").isCurrent()).isFalse();
        assertThat(stageMap.get("Milestone 1").isEnded()).isTrue();
        assertThat(stageMap.get("Milestone 1").getEndTime()).isNotNull();

        assertThat(stageMap.get("Stage 1").getName()).isEqualTo("Stage 1");
        assertThat(stageMap.get("Stage 1").isCurrent()).isTrue();
        assertThat(stageMap.get("Stage 1").isEnded()).isFalse();
        assertThat(stageMap.get("Stage 1").getEndTime()).isNull();

        assertThat(stageMap.get("Stage 2").getName()).isEqualTo("Stage 2");
        assertThat(stageMap.get("Stage 2").isCurrent()).isFalse();
        assertThat(stageMap.get("Stage 2").isEnded()).isFalse();
        assertThat(stageMap.get("Stage 2").getEndTime()).isNull();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            stages = cmmnHistoryService.getStageOverview(caseInstance.getId());
            assertThat(stages).hasSize(5);

            stageMap = new HashMap<>();
            for (StageResponse stageResponse : stages) {
                stageMap.put(stageResponse.getName(), stageResponse);
            }

            assertThat(stageMap.get("Milestone 1").getName()).isEqualTo("Milestone 1");
            assertThat(stageMap.get("Milestone 1").isCurrent()).isFalse();
            assertThat(stageMap.get("Milestone 1").isEnded()).isTrue();
            assertThat(stageMap.get("Milestone 1").getEndTime()).isNotNull();

            assertThat(stageMap.get("Stage 1").getName()).isEqualTo("Stage 1");
            assertThat(stageMap.get("Stage 1").isCurrent()).isTrue();
            assertThat(stageMap.get("Stage 1").isEnded()).isFalse();
            assertThat(stageMap.get("Stage 1").getEndTime()).isNull();

            assertThat(stageMap.get("Stage 2").getName()).isEqualTo("Stage 2");
            assertThat(stageMap.get("Stage 2").isCurrent()).isFalse();
            assertThat(stageMap.get("Stage 2").isEnded()).isFalse();
            assertThat(stageMap.get("Stage 2").getEndTime()).isNull();

            assertThat(stageMap.get("Stage 1").getName()).isEqualTo("Stage 1");
            assertThat(stageMap.get("Stage 1").isCurrent()).isTrue();
            assertThat(stageMap.get("Stage 1").isEnded()).isFalse();
            assertThat(stageMap.get("Stage 1").getEndTime()).isNull();

            assertThat(stageMap.get("Stage 2").getName()).isEqualTo("Stage 2");
            assertThat(stageMap.get("Stage 2").isCurrent()).isFalse();
            assertThat(stageMap.get("Stage 2").isEnded()).isFalse();
            assertThat(stageMap.get("Stage 2").getEndTime()).isNull();
        }

        cmmnRuntimeService
                .completeStagePlanItemInstance(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("Stage 1").singleResult().getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            stages = cmmnHistoryService.getStageOverview(caseInstance.getId());
            assertThat(stages).hasSize(3);

            stageMap = new HashMap<>();
            for (StageResponse stageResponse : stages) {
                stageMap.put(stageResponse.getName(), stageResponse);
            }

            assertThat(stageMap.get("Milestone 1").getName()).isEqualTo("Milestone 1");
            assertThat(stageMap.get("Milestone 1").isCurrent()).isFalse();
            assertThat(stageMap.get("Milestone 1").isEnded()).isTrue();
            assertThat(stageMap.get("Milestone 1").getEndTime()).isNotNull();

            assertThat(stageMap.get("Stage 1").getName()).isEqualTo("Stage 1");
            assertThat(stageMap.get("Stage 1").isCurrent()).isFalse();
            assertThat(stageMap.get("Stage 1").isEnded()).isTrue();
            assertThat(stageMap.get("Stage 1").getEndTime()).isNotNull();

            assertThat(stageMap.get("Stage 2").getName()).isEqualTo("Stage 2");
            assertThat(stageMap.get("Stage 2").isCurrent()).isFalse();
            assertThat(stageMap.get("Stage 2").isEnded()).isTrue();
            assertThat(stageMap.get("Stage 2").getEndTime()).isNotNull();
        }
    }

    @Test
    @CmmnDeployment
    public void testNestedRepeatingStageWithMultipleOnParts() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testNestedRepeatingStageWithMultipleOnParts").start();
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("Nested Task").singleResult()).isNotNull();

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertPlanItemInstanceState(planItemInstances, "stage1", PlanItemInstanceState.ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "stage2", PlanItemInstanceState.ACTIVE, PlanItemInstanceState.WAITING_FOR_REPETITION);

        UserEventListenerInstance exitStageEventListener = cmmnRuntimeService.createUserEventListenerInstanceQuery().name("exit stage").singleResult();
        cmmnRuntimeService.completeUserEventListenerInstance(exitStageEventListener.getId());
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("Nested Task").singleResult()).isNull();

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().includeEnded().caseInstanceId(caseInstance.getId()).list();
        assertPlanItemInstanceState(planItemInstances, "stage1", PlanItemInstanceState.TERMINATED);
        assertPlanItemInstanceState(planItemInstances, "stage2", PlanItemInstanceState.TERMINATED, PlanItemInstanceState.TERMINATED);

        List<UserEventListenerInstance> userEventListenerInstances = cmmnRuntimeService.createUserEventListenerInstanceQuery().
                caseInstanceId(caseInstance.getId()).list();
        userEventListenerInstances
                .forEach(userEventListenerInstance -> cmmnRuntimeService.completeUserEventListenerInstance(userEventListenerInstance.getId()));

        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("Nested Task").singleResult()).isNotNull();
        cmmnTaskService.createTaskQuery().list().forEach(task -> cmmnTaskService.complete(task.getId()));

        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testActivateTerminatedRepeatingStage() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testActivateTerminatedRepeatingStage")
                .start();

        assertPlanItemInstanceState(caseInstance, "A", PlanItemInstanceState.ENABLED);
        assertPlanItemInstanceState(caseInstance, "stage2", PlanItemInstanceState.AVAILABLE);

        cmmnRuntimeService
                .createPlanItemInstanceTransitionBuilder(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("A").singleResult().getId())
                .start();

        assertPlanItemInstanceState(caseInstance, "A", PlanItemInstanceState.ACTIVE);
        assertPlanItemInstanceState(caseInstance, "stage2", PlanItemInstanceState.TERMINATED, PlanItemInstanceState.WAITING_FOR_REPETITION);

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());

        assertPlanItemInstanceState(caseInstance, "A", PlanItemInstanceState.COMPLETED, PlanItemInstanceState.ENABLED);
        assertPlanItemInstanceState(caseInstance, "stage2", PlanItemInstanceState.TERMINATED, PlanItemInstanceState.WAITING_FOR_REPETITION,
                PlanItemInstanceState.ACTIVE);
        assertPlanItemInstanceState(caseInstance, "B", PlanItemInstanceState.ACTIVE);

        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("B");

        cmmnTaskService.complete(task.getId());
        assertPlanItemInstanceState(caseInstance, "A", PlanItemInstanceState.COMPLETED, PlanItemInstanceState.ENABLED);
        assertPlanItemInstanceState(caseInstance, "stage2", PlanItemInstanceState.TERMINATED, PlanItemInstanceState.WAITING_FOR_REPETITION,
                PlanItemInstanceState.COMPLETED);
        assertPlanItemInstanceState(caseInstance, "B", PlanItemInstanceState.COMPLETED);

        cmmnRuntimeService
                .createPlanItemInstanceTransitionBuilder(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("A").singleResult().getId())
                .start();
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());

        assertPlanItemInstanceState(caseInstance, "A", PlanItemInstanceState.COMPLETED, PlanItemInstanceState.COMPLETED, PlanItemInstanceState.ENABLED);
        assertPlanItemInstanceState(caseInstance, "stage2", PlanItemInstanceState.TERMINATED, PlanItemInstanceState.WAITING_FOR_REPETITION,
                PlanItemInstanceState.COMPLETED, PlanItemInstanceState.ACTIVE);
        assertPlanItemInstanceState(caseInstance, "B", PlanItemInstanceState.COMPLETED, PlanItemInstanceState.ACTIVE);
    }

    @Test
    @CmmnDeployment
    public void testRepeatingStageInTerminatedCase() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testRepeatingStageInTerminatedCase")
                .variable("terminateTheCase", true)
                .start();

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());

        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testRepeatingStageInTerminatedCase2() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testRepeatingStageInTerminatedCase")
                .variable("terminateTheCase", true)
                .start();

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());

        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testRepeatingStageInTerminatedCase3() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();

        List<PlanItemInstance> activeStages = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).onlyStages()
                .planItemInstanceStateActive().list();
        assertThat(activeStages)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactly("expandedStage2");

        Task closeTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(closeTask).isNotNull();
        cmmnTaskService.complete(closeTask.getId());

        Task reopenTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(reopenTask).isNotNull();
        cmmnTaskService.complete(reopenTask.getId());

        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testAllServiceTasksInRepeatedStages() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testRepeatingStageInTerminatedCase")
                .variable("terminateTheCase", true)
                .start();
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testForceComplete() {
        CaseInstance caseInstance = cmmnRuntimeService
                .createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .start();

        PlanItemInstance stagePlanItemInstance = cmmnRuntimeService
                .createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .onlyStages()
                .singleResult();

        assertThatThrownBy(() -> cmmnRuntimeService.completeStagePlanItemInstance(stagePlanItemInstance.getId(), false))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessageStartingWith("Can only complete a stage plan item instance that is marked as completable");
        assertCaseInstanceNotEnded(caseInstance);

        cmmnRuntimeService.completeStagePlanItemInstance(stagePlanItemInstance.getId(), true);
        assertCaseInstanceEnded(caseInstance);
    }
}
