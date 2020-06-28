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
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.AVAILABLE;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.WAITING_FOR_REPETITION;

import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.junit.Test;

/**
 * Testing the removal of orphaned event listeners in combination with repetition, inner stages and plan items.
 *
 * @author Micha Kiener
 */
public class OrphanEventListenerRemovalCombinedWithRepetitionTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/itemcontrol/OrphanEventListenerRemovalCombinedWithRepetitionTest.testRemovalOfOrphanedEventListeners.cmmn")
    public void testRemovalOfOrphanedEventListenersWithOuterStart() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("nestedRepetitionPlanItemsWithOrphanedEventListeners")
                .start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(5);

        assertPlanItemInstanceState(planItemInstances, "Stage A", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "start Task A 1", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "start Task A 2", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "start Stage A", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "kill Stage A", AVAILABLE);

        // start with the outer stage
        cmmnRuntimeService.completeUserEventListenerInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "start Stage A", AVAILABLE));

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(5);

        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "start Task A 1", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "start Task A 2", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "kill Stage A", AVAILABLE);

        // now kill Stage A, also killing Stage B and Task A (although never started), but must not kill the user tasks
        cmmnRuntimeService.completeUserEventListenerInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "kill Stage A", AVAILABLE));

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(2);

        assertPlanItemInstanceState(planItemInstances, "start Task A 1", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "start Task A 2", AVAILABLE);

        // starting Task A through both event listeners must also start the stages again
        cmmnRuntimeService.completeUserEventListenerInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "start Task A 1", AVAILABLE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(1);
        assertPlanItemInstanceState(planItemInstances, "start Task A 2", AVAILABLE);
        cmmnRuntimeService.completeUserEventListenerInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "start Task A 2", AVAILABLE));

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(4);

        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE, WAITING_FOR_REPETITION);

        // completing Task A must complete the case as there is no more way to activate it again, even though it has repetition
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task A", ACTIVE));

        assertCaseInstanceEnded(caseInstance);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/itemcontrol/OrphanEventListenerRemovalCombinedWithRepetitionTest.testRemovalOfOrphanedEventListeners.cmmn")
    public void testRemovalOfOrphanedEventListenersWithInnerStart() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("nestedRepetitionPlanItemsWithOrphanedEventListeners")
                .start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(5);

        assertPlanItemInstanceState(planItemInstances, "Stage A", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "start Task A 1", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "start Task A 2", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "start Stage A", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "kill Stage A", AVAILABLE);

        // start with the inner task directly
        cmmnRuntimeService.completeUserEventListenerInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "start Task A 1", AVAILABLE));
        cmmnRuntimeService.completeUserEventListenerInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "start Task A 2", AVAILABLE));

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(5);

        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE, WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "kill Stage A", AVAILABLE);

        // now kill Stage A, also killing Stage B and Task A (although never started), but must not kill the user tasks
        cmmnRuntimeService.completeUserEventListenerInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "kill Stage A", AVAILABLE));

        assertCaseInstanceEnded(caseInstance);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/itemcontrol/OrphanEventListenerRemovalCombinedWithRepetitionTwoTest.testRemovalOfOrphanedEventListeners.cmmn")
    public void testRemovalOfOrphanedEventListenersWithOuterStartAndInnerExitEventListener() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("nestedRepetitionPlanItemsWithOrphanedEventListenersTwo")
                .start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(6);

        assertPlanItemInstanceState(planItemInstances, "Stage A", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "start Task A 1", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "start Task A 2", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "start Stage A", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "kill Stage A", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "kill Task A", AVAILABLE);

        // start with the outer stage
        cmmnRuntimeService.completeUserEventListenerInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "start Stage A", AVAILABLE));

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(6);

        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "start Task A 1", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "start Task A 2", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "kill Stage A", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "kill Task A", AVAILABLE);

        // now kill Stage A, also killing Stage B and Task A (although never started), but must not kill the user tasks
        cmmnRuntimeService.completeUserEventListenerInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "kill Stage A", AVAILABLE));

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(3);

        assertPlanItemInstanceState(planItemInstances, "start Task A 1", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "start Task A 2", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "kill Task A", AVAILABLE);

        // starting Task A through both event listeners must also start the stages again
        cmmnRuntimeService.completeUserEventListenerInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "start Task A 1", AVAILABLE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(2);
        assertPlanItemInstanceState(planItemInstances, "start Task A 2", AVAILABLE);
        cmmnRuntimeService.completeUserEventListenerInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "start Task A 2", AVAILABLE));

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(5);

        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE, WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "kill Task A", AVAILABLE);

        // completing Task A must complete the case as there is no more way to activate it again, even though it has repetition
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task A", ACTIVE));

        assertCaseInstanceEnded(caseInstance);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();
    }
}
