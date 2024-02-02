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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.history.HistoricMilestoneInstance;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.task.api.Task;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class EntryCriteriaTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment
    public void testStartPassthroughCaseWithThreeEntryCriteriaOnParts() {
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().singleResult();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionId(caseDefinition.getId()).start();


        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            List<HistoricMilestoneInstance> mileStones = cmmnHistoryService.createHistoricMilestoneInstanceQuery()
                .milestoneInstanceCaseInstanceId(caseInstance.getId())
                .orderByMilestoneName().asc()
                .list();
            assertThat(mileStones)
                .extracting(HistoricMilestoneInstance::getName)
                .containsExactly("PlanItem Milestone One");

            HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
            assertThat(historicCaseInstance).isNotNull();
        }

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(1);
        }
    }

    @Test
    @CmmnDeployment
    public void testThreeEntryCriteriaOnPartsForWaitStates() {
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().singleResult();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionId(caseDefinition.getId()).start();
        assertThat(cmmnRuntimeService.createMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstance.getId()).count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstance.getId()).count()).isZero();
        }

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .list();
        assertThat(planItemInstances).hasSize(3);

        for (PlanItemInstance planItemInstance : planItemInstances) {
            cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
        }

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricMilestoneInstance mileStone = cmmnHistoryService.createHistoricMilestoneInstanceQuery()
                .milestoneInstanceCaseInstanceId(caseInstance.getId())
                .singleResult();
            assertThat(mileStone.getName()).isEqualTo("PlanItem Milestone One");
        }

        assertThat(cmmnRuntimeService.createMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstance.getId()).count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
            assertThat(historicCaseInstance).isNotNull();
        }
    }

    @Test
    @CmmnDeployment
    public void testMultipleEntryCriteria() {
        // 3 sentries, each completion should trigger the milestone
        for (int i = 0; i < 3; i++) {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
            List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                    .orderByName().asc()
                    .list();
            assertThat(planItemInstances).hasSize(3);

            cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(i).getId());

            if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
                assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
            }

            // Triggering the other two should not have additional effects
            for (PlanItemInstance planItemInstance : cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                    .planItemInstanceState(PlanItemInstanceState.ACTIVE).list()) {
                cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
            }

            if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
                assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
            }

            assertCaseInstanceEnded(caseInstance);
        }
    }

    @Test
    @CmmnDeployment
    public void testTerminateCaseInstanceAfterOneOutOfMultipleOnPartsSatisfied() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .orderByName().asc()
                .list();
        assertThat(planItemInstances).hasSize(3);

        // Triggering two plan items = 2 on parts satisfied
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(0).getId());
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(1).getId());

        cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count()).isZero();
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(1);
        }
    }

    @Test
    @CmmnDeployment
    public void testNestedSentrySatisfiedInSameEvaluation() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("A");

        // Completing the task should trigger the outer stage and also the task in the nested stage in the same evaluation cycle
        cmmnTaskService.complete(task.getId());
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("B");
    }

    @Test
    @CmmnDeployment
    public void testCrossBorderSentry() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCrossBorderSentry").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("A");

        PlanItemInstance stage2PlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("Stage 2").singleResult();
        assertThat(stage2PlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);

        PlanItemInstance stage3PlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("Stage 3").singleResult();
        assertThat(stage3PlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);

        assertThat(cmmnTaskService.createTaskQuery().taskName("B").singleResult()).isNull();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult()).isNull();

        // Completing A should activate B and it's stage (that has a sentry that isn't yet satisfied, but due to B's activation it gets activated too)
        cmmnTaskService.complete(cmmnTaskService.createTaskQuery().taskName("A").singleResult().getId());

        stage2PlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("Stage 2").singleResult();
        assertThat(stage2PlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);

        stage3PlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("Stage 3").singleResult();
        assertThat(stage3PlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);

        assertThat(cmmnTaskService.createTaskQuery().taskName("B").singleResult()).isNotNull();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult()).isNotNull();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("A").singleResult()).isNull();
    }

    @Test
    @CmmnDeployment
    public void testCrossBorderMultipleSentries() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCrossBorderSentry").start();
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("A");

        PlanItemInstance stage2PlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("Stage 2").singleResult();
        assertThat(stage2PlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);

        PlanItemInstance stage3PlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("Stage 3").singleResult();
        assertThat(stage3PlanItemInstance).isNull();

        PlanItemInstance stage4PlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("Stage 4").singleResult();
        assertThat(stage4PlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);

        PlanItemInstance stage5PlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("Stage 5").singleResult();
        assertThat(stage5PlanItemInstance).isNull();

        // Completing A should activate B,C,D an E
        cmmnTaskService.complete(cmmnTaskService.createTaskQuery().taskName("A").singleResult().getId());

        stage2PlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("Stage 2").singleResult();
        assertThat(stage2PlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);

        stage3PlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("Stage 3").singleResult();
        assertThat(stage3PlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);

        stage4PlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("Stage 4").singleResult();
        assertThat(stage4PlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);

        stage5PlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("Stage 5").singleResult();
        assertThat(stage5PlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);

        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("B", "C", "D", "E");
    }

    @Test
    @CmmnDeployment
    public void testCrossBorderSentryWithVariableFunction() {
        cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCrossBorderSentry").start();
        cmmnTaskService.complete(cmmnTaskService.createTaskQuery().taskName("A").singleResult().getId(), Collections.singletonMap("taskvar", 123));

        assertThat(cmmnTaskService.createTaskQuery().taskName("B").singleResult()).isNotNull();

        PlanItemInstance stage2PlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("Stage 2").singleResult();
        assertThat(stage2PlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);

        PlanItemInstance stage3PlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("Stage 3").singleResult();
        assertThat(stage3PlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult()).isNotNull();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("A").singleResult()).isNull();
    }

    @Test
    @CmmnDeployment
    public void testRepeatingCrossBoundary() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testRepeatingCrossBoundary")
                .variable("goIntoB", true)
                .variable("goIntoC", true)
                .start();

        // Complete B once, will terminate Stage B and C
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("A", "B");
        cmmnTaskService.complete(tasks.get(1).getId());

        // Completing A should again reactivate task B and thus also Stage B and C
        cmmnTaskService.complete(tasks.get(0).getId());

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("B");
    }

    @Test
    @CmmnDeployment
    public void testRepeatingCrossBoundary2() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCrossBoundaryWithRepetition").start();

        // Completing A will make B active. Task A is repeating.
        Task taskA = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(taskA.getId());
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("A", "B", "C");

        // Completing B should keep a plan item instance for B with 'repeating'
        cmmnTaskService.complete(tasks.get(1).getId());
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("A", "C");

        PlanItemInstance planItemInstanceB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult();
        assertThat(planItemInstanceB.getState()).isEqualTo(PlanItemInstanceState.WAITING_FOR_REPETITION);

        // When setting the variables, stage 2 and 3 should be activated. Task B should be activated and a new plan item instance for the repeat for B should be created
        Map<String, Object> vars = new HashMap<>();
        vars.put("stageTwo", true);
        vars.put("stageThree", true);
        cmmnRuntimeService.setVariables(caseInstance.getId(), vars);

        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("A", "B", "C");

        PlanItemInstance planItemInstanceB2 = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B")
                .planItemInstanceState(PlanItemInstanceState.WAITING_FOR_REPETITION).singleResult();
        assertThat(planItemInstanceB.getId()).isNotEqualTo(planItemInstanceB2.getId());
        assertThat(planItemInstanceB2.getState()).isEqualTo(PlanItemInstanceState.WAITING_FOR_REPETITION);

        // Completing A again should again activate B each time
        for (int i = 0; i < 9; i++) {
            cmmnTaskService.complete(tasks.get(0).getId());
            tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        }

        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("A", "B", "B", "B", "B", "B", "B", "B", "B", "B", "B", "C"); // 9 + 1 already created before

        PlanItemInstance planItemInstanceB3 = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B")
                .planItemInstanceState(PlanItemInstanceState.WAITING_FOR_REPETITION).singleResult();
        assertThat(planItemInstanceB.getId()).isNotEqualTo(planItemInstanceB3.getId());
        assertThat(planItemInstanceB2.getId()).isNotEqualTo(planItemInstanceB3.getId());
        assertThat(planItemInstanceB3.getState()).isEqualTo(PlanItemInstanceState.WAITING_FOR_REPETITION);

        // Completing all instances of B and C should leave A only
        for (int i = 1; i < tasks.size(); i++) { // all except A
            cmmnTaskService.complete(tasks.get(i).getId());
        }
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("A");

        // Completing A should again reactivate B and the parent stages
        cmmnTaskService.complete(tasks.get(0).getId());
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("A", "B", "C");

        PlanItemInstance planItemInstanceStageTwo = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("Stage Two").singleResult();
        assertThat(planItemInstanceStageTwo.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);

        PlanItemInstance planItemInstanceStageThree = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("Stage Three").singleResult();
        assertThat(planItemInstanceStageThree.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
    }

    @Test
    @CmmnDeployment
    public void testRepeatingCrossBoundary3() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variable("myVar", true)
                .start();

        Task task = cmmnTaskService.createTaskQuery().singleResult();
        cmmnTaskService.complete(task.getId());

        task = cmmnTaskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("Test 2");

        for (int i = 0; i < 11; i++) {
            cmmnTaskService.complete(task.getId());

            task = cmmnTaskService.createTaskQuery().singleResult();
            assertThat(task.getName()).isEqualTo("Test");
            cmmnTaskService.complete(task.getId());

            task = cmmnTaskService.createTaskQuery().singleResult();
            assertThat(task.getName()).isEqualTo("Test 2");

        }

    }

}
