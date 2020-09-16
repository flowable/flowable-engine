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
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.ENABLED;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.UNAVAILABLE;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.WAITING_FOR_REPETITION;

import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.junit.Test;

/**
 * Testing the exit event type combination with cross boundary activation and event listeners.
 *
 * @author Micha Kiener
 */
public class CrossBoundaryActivationWithExitEventTypeCombinationTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment
    public void testCrossBoundaryActivationWithExitEventTypeCombinationTest() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("crossBoundaryRepetitionTestCase")
            .start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(6);

        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE, WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "complete stage", UNAVAILABLE);

        // complete Task A and the user listener to go to Stage B
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task A", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        cmmnRuntimeService.completeUserEventListenerInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "complete stage", AVAILABLE));

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(4);

        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE, WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Decision", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Follow-up", AVAILABLE);

        // complete Decision with declined to activate a cross-boundary plan item in Stage A and terminate Stage B
        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(getPlanItemInstanceIdByNameAndState(planItemInstances, "Decision", ACTIVE))
            .variable("approval", "declined")
            .trigger();

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(6);

        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE, WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "complete stage", UNAVAILABLE);


        // complete Task A and the user listener to go to Stage B
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task A", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        cmmnRuntimeService.completeUserEventListenerInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "complete stage", AVAILABLE));

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(4);

        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE, WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Decision", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Follow-up", AVAILABLE);

        // complete Decision with declined to activate a cross-boundary plan item in Stage A and terminate Stage B (again)
        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(getPlanItemInstanceIdByNameAndState(planItemInstances, "Decision", ACTIVE))
            .variable("approval", "declined")
            .trigger();

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(6);

        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE, WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "complete stage", UNAVAILABLE);

        // complete Task A and the user listener to go to Stage B
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task A", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        cmmnRuntimeService.completeUserEventListenerInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "complete stage", AVAILABLE));

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(4);

        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE, WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Decision", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Follow-up", AVAILABLE);

        // complete Decision with declined to activate a cross-boundary plan item in Stage A and terminate Stage B
        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(getPlanItemInstanceIdByNameAndState(planItemInstances, "Decision", ACTIVE))
            .variable("approval", "approved")
            .trigger();

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(3);

        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE, WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Follow-up", ACTIVE);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Follow-up", ACTIVE));

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).isEmpty();

        assertCaseInstanceEnded(caseInstance);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();
    }
}
