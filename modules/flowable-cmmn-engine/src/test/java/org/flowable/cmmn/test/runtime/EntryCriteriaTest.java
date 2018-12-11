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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
import org.flowable.task.api.Task;
import org.junit.Assert;
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

        List<HistoricMilestoneInstance> mileStones = cmmnHistoryService.createHistoricMilestoneInstanceQuery()
                .milestoneInstanceCaseInstanceId(caseInstance.getId())
                .orderByMilestoneName().asc()
                .list();
        assertEquals(1, mileStones.size());
        assertEquals("PlanItem Milestone One", mileStones.get(0).getName());

        HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertNotNull(historicCaseInstance);
        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(0, cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
    }

    @Test
    @CmmnDeployment
    public void testThreeEntryCriteriaOnPartsForWaitStates() {
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().singleResult();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionId(caseDefinition.getId()).start();
        assertEquals(0, cmmnRuntimeService.createMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstance.getId()).count());
        assertEquals(0, cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstance.getId()).count());

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .list();
        assertEquals(3, planItemInstances.size());

        for (PlanItemInstance planItemInstance : planItemInstances) {
            cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
        }

        HistoricMilestoneInstance mileStone = cmmnHistoryService.createHistoricMilestoneInstanceQuery()
                .milestoneInstanceCaseInstanceId(caseInstance.getId())
                .singleResult();
        assertEquals("PlanItem Milestone One", mileStone.getName());
        assertEquals(0, cmmnRuntimeService.createMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstance.getId()).count());

        HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertNotNull(historicCaseInstance);
    }

    @Test
    @CmmnDeployment
    public void testMultipleEntryCriteria() {
        // 3 sentries, each completion should trigger the milestone
        for (int i=0; i<3; i++) {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
            List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                    .orderByName().asc()
                    .list();
            assertEquals(3, planItemInstances.size());

            cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(i).getId());
            assertEquals(1, cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstance.getId()).count());

            // Triggering the other two should not have additional effects
            for (PlanItemInstance planItemInstance :cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceState(PlanItemInstanceState.ACTIVE).list()) {
                cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
            }

            assertEquals(1, cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstance.getId()).count());
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
        assertEquals(3, planItemInstances.size());

        // Triggering two plan items = 2 on parts satisfied
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(0).getId());
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(1).getId());

        cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());
        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(0, cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
    }

    @Test
    @CmmnDeployment
    public void testNestedSentrySatisfiedInSameEvaluation() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("A", task.getName());

        // Completing the task should trigger the outer stage and also the task in the nested stage in the same evaluation cycle
        cmmnTaskService.complete(task.getId());
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("B", task.getName());
    }

    @Test
    @CmmnDeployment
    public void testCrossBorderSentry() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCrossBorderSentry").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("A", task.getName());

        PlanItemInstance stage2PlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("Stage 2").singleResult();
        assertEquals(PlanItemInstanceState.ACTIVE, stage2PlanItemInstance.getState());

        PlanItemInstance stage3PlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("Stage 3").singleResult();
        assertEquals(PlanItemInstanceState.AVAILABLE, stage3PlanItemInstance.getState());

        assertNull(cmmnTaskService.createTaskQuery().taskName("B").singleResult());
        assertNull(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult());

        // Completing A should activate B and it's stage (that has a sentry that isn't yet satisfied, but due to B's activation it gets activated too)
        cmmnTaskService.complete(cmmnTaskService.createTaskQuery().taskName("A").singleResult().getId());

        stage2PlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("Stage 2").singleResult();
        assertEquals(PlanItemInstanceState.ACTIVE, stage2PlanItemInstance.getState());

        stage3PlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("Stage 3").singleResult();
        assertEquals(PlanItemInstanceState.ACTIVE, stage3PlanItemInstance.getState());

        assertNotNull(cmmnTaskService.createTaskQuery().taskName("B").singleResult());
        assertNotNull(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult());
        assertNull(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("A").singleResult());
    }

    @Test
    @CmmnDeployment
    public void testCrossBorderMultipleSentries() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCrossBorderSentry").start();
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertEquals(1, tasks.size());
        assertEquals("A", tasks.get(0).getName());

        PlanItemInstance stage2PlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("Stage 2").singleResult();
        assertEquals(PlanItemInstanceState.AVAILABLE, stage2PlanItemInstance.getState());

        PlanItemInstance stage3PlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("Stage 3").singleResult();
        assertNull(stage3PlanItemInstance);

        PlanItemInstance stage4PlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("Stage 4").singleResult();
        assertEquals(PlanItemInstanceState.AVAILABLE, stage4PlanItemInstance.getState());

        PlanItemInstance stage5PlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("Stage 5").singleResult();
        assertNull(stage5PlanItemInstance);

        // Completing A should activate B,C,D an E
        cmmnTaskService.complete(cmmnTaskService.createTaskQuery().taskName("A").singleResult().getId());

        stage2PlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("Stage 2").singleResult();
        assertEquals(PlanItemInstanceState.ACTIVE, stage2PlanItemInstance.getState());

        stage3PlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("Stage 3").singleResult();
        assertEquals(PlanItemInstanceState.ACTIVE, stage3PlanItemInstance.getState());

        stage4PlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("Stage 4").singleResult();
        assertEquals(PlanItemInstanceState.ACTIVE, stage4PlanItemInstance.getState());

        stage5PlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("Stage 5").singleResult();
        assertEquals(PlanItemInstanceState.ACTIVE, stage5PlanItemInstance.getState());

        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks).extracting(Task::getName).containsExactly("B", "C", "D", "E");
    }

    @Test
    @CmmnDeployment
    public void testCrossBorderSentryWithVariableFunction() {
        cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCrossBorderSentry").start();
        cmmnTaskService.complete(cmmnTaskService.createTaskQuery().taskName("A").singleResult().getId(), Collections.singletonMap("taskvar", 123));

        assertNotNull(cmmnTaskService.createTaskQuery().taskName("B").singleResult());

        PlanItemInstance stage2PlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("Stage 2").singleResult();
        assertEquals(PlanItemInstanceState.ACTIVE, stage2PlanItemInstance.getState());

        PlanItemInstance stage3PlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("Stage 3").singleResult();
        assertEquals(PlanItemInstanceState.ACTIVE, stage3PlanItemInstance.getState());

        assertNotNull(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult());
        assertNull(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("A").singleResult());
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
        assertThat(tasks).extracting(Task::getName).containsExactly("A", "B");
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
        Task taskA =  cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(taskA.getId());
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks).extracting(Task::getName).containsExactly("A", "B", "C");

        // Completing B should keep a plan item instance for B with 'repeating'
        cmmnTaskService.complete(tasks.get(1).getId());
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks).extracting(Task::getName).containsExactly("A", "C");

        PlanItemInstance planItemInstanceB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult();
        assertThat(planItemInstanceB.getState()).isEqualTo(PlanItemInstanceState.WAITING_FOR_REPETITION);

        // When setting the variables, stage 2 and 3 should be activated. Task B should be activated and a new plan item instance for the repeat for B should be created
        Map<String, Object> vars = new HashMap<>();
        vars.put("stageTwo", true);
        vars.put("stageThree", true);
        cmmnRuntimeService.setVariables(caseInstance.getId(), vars);

        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks).extracting(Task::getName).containsExactly("A", "B", "C");

        PlanItemInstance planItemInstanceB2 = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").planItemInstanceState(PlanItemInstanceState.WAITING_FOR_REPETITION).singleResult();
        assertThat(planItemInstanceB.getId()).isNotEqualTo(planItemInstanceB2.getId());
        assertThat(planItemInstanceB2.getState()).isEqualTo(PlanItemInstanceState.WAITING_FOR_REPETITION);

        // Completing A again should again activate B each time
        for (int i = 0; i < 9; i++) {
            cmmnTaskService.complete(tasks.get(0).getId());
            tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        }

        assertThat(tasks).extracting(Task::getName).containsExactly("A", "B", "B", "B", "B", "B", "B", "B", "B", "B", "B", "C"); // 9 + 1 already created before

        PlanItemInstance planItemInstanceB3 = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").planItemInstanceState(PlanItemInstanceState.WAITING_FOR_REPETITION).singleResult();
        assertThat(planItemInstanceB.getId()).isNotEqualTo(planItemInstanceB3.getId());
        assertThat(planItemInstanceB2.getId()).isNotEqualTo(planItemInstanceB3.getId());
        assertThat(planItemInstanceB3.getState()).isEqualTo(PlanItemInstanceState.WAITING_FOR_REPETITION);

        // Completing all instances of B and C should leave A only
        for (int i = 1; i < tasks.size(); i++) { // all except A
            cmmnTaskService.complete(tasks.get(i).getId());
        }
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks).extracting(Task::getName).containsExactly("A");

        // Completing A should again reactivate B and the parent stages
        cmmnTaskService.complete(tasks.get(0).getId());
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks).extracting(Task::getName).containsExactly("A", "B", "C");

        PlanItemInstance planItemInstanceStageTwo = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("Stage Two").singleResult();
        assertThat(planItemInstanceStageTwo.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);

        PlanItemInstance planItemInstanceStageThree = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("Stage Three").singleResult();
        assertThat(planItemInstanceStageThree.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
    }

    @Test
    @CmmnDeployment
    public void testRepeatingCrossBoundary3() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("myCase")
            .variable("myVar", true)
            .start();

        Task  task =  cmmnTaskService.createTaskQuery().singleResult();
        cmmnTaskService.complete(task.getId());

        task = cmmnTaskService.createTaskQuery().singleResult();
        Assert.assertEquals("Test 2",task.getName());

        for (int i = 0; i < 11; i++) {
            cmmnTaskService.complete(task.getId());

            task = cmmnTaskService.createTaskQuery().singleResult();
            Assert.assertEquals("Test",task.getName());
            cmmnTaskService.complete(task.getId());

            task = cmmnTaskService.createTaskQuery().singleResult();
            Assert.assertEquals("Test 2",task.getName());

        }

    }

}
