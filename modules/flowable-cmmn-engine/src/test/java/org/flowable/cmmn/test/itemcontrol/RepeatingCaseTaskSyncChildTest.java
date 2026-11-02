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
package org.flowable.cmmn.test.itemcontrol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.ACTIVE;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.WAITING_FOR_REPETITION;

import java.util.Collections;
import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.test.FlowableCmmnTestCase;
import org.junit.jupiter.api.Test;

/**
 * Tests a collection-based repeating case task whose child case completes synchronously (it has no wait state),
 * followed by a human task with an entry sentry depending on the completion of that case task.
 *
 * <p>Regression for the agenda ordering issue where a completion evaluation (planned when the agenda becomes
 * stable, carrying no plan item lifecycle event) could run before the still-pending 'complete' event of the
 * synchronously completed case task. That completion evaluation would complete the stage/case before the human
 * task's single-on-part {@code onComplete} entry sentry had a chance to fire, discarding the human task.
 *
 * <p>The failure is order dependent: during agenda stabilization the completion evaluations of the parent and
 * child case instances are planned in the iteration order of a {@code HashSet} of case instance ids, which is
 * effectively random per run. Each scenario is therefore repeated so both orderings are reliably exercised.
 */
public class RepeatingCaseTaskSyncChildTest extends FlowableCmmnTestCase {

    protected static final int REPEAT = 20;

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/itemcontrol/RepeatingCaseTaskSyncChildTest.parent.cmmn",
            "org/flowable/cmmn/test/itemcontrol/RepeatingCaseTaskSyncChildTest.child.cmmn"
    })
    public void testRepeatingCaseTaskWithSynchronousChildAndSingleItem() {
        for (int i = 0; i < REPEAT; i++) {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("repeatingCaseTaskSyncChild")
                    .variable("taskOutputList", Collections.singletonList("single"))
                    .start();

            PlanItemInstance taskA = getSinglePlanItemInstance(caseInstance, "Task A", ACTIVE);
            assertThat(taskA).as("iteration %s: Task A should be active after start", i).isNotNull();

            // Completing Task A fires the on-part that activates the repeating case task. The single collection
            // item spawns one case task instance whose child case completes synchronously within this command.
            cmmnRuntimeService.triggerPlanItemInstance(taskA.getId());

            // The case must not have completed: the human task gated on the case task's completion must be active.
            assertThat(getPlanItemInstances(caseInstance, "Task C", ACTIVE))
                    .as("iteration %s: Task C should be active after the single case-task repetition completed", i)
                    .hasSize(1);

            // The repeating case task keeps its repetition template, now moved to the completion-neutral 'waiting for
            // repetition' state as its single created instance has completed, ready for a possible next on-part.
            assertThat(getPlanItemInstances(caseInstance, "Task B", WAITING_FOR_REPETITION))
                    .as("iteration %s: the case task repetition template should be waiting for repetition", i)
                    .hasSize(1);

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult())
                    .as("iteration %s: the case should still be active", i).isNotNull();
        }
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/itemcontrol/RepeatingCaseTaskSyncChildTest.stage.cmmn",
            "org/flowable/cmmn/test/itemcontrol/RepeatingCaseTaskSyncChildTest.child.cmmn"
    })
    public void testRepeatingCaseTaskWithSynchronousChildInsideAutoCompleteStage() {
        for (int i = 0; i < REPEAT; i++) {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("repeatingCaseTaskSyncChildStage")
                    .variable("taskOutputList", Collections.singletonList("single"))
                    .start();

            PlanItemInstance taskA = getSinglePlanItemInstance(caseInstance, "Task A", ACTIVE);
            assertThat(taskA).as("iteration %s: Task A should be active after start", i).isNotNull();

            cmmnRuntimeService.triggerPlanItemInstance(taskA.getId());

            // The auto-complete stage must not complete before the case task's completion activated Task C:
            // Task C active keeps the stage active.
            assertThat(getPlanItemInstances(caseInstance, "Task C", ACTIVE))
                    .as("iteration %s: Task C should be active after the single case-task repetition completed", i)
                    .hasSize(1);
            assertThat(getPlanItemInstances(caseInstance, "Stage", ACTIVE))
                    .as("iteration %s: the parent stage should still be active", i).hasSize(1);
        }
    }

    protected List<PlanItemInstance> getPlanItemInstances(CaseInstance caseInstance, String name, String state) {
        return cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceName(name)
                .planItemInstanceState(state)
                .list();
    }

    protected PlanItemInstance getSinglePlanItemInstance(CaseInstance caseInstance, String name, String state) {
        return cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceName(name)
                .planItemInstanceState(state)
                .singleResult();
    }
}
