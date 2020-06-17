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
 * Testing the max instance count attribute on the repetition rule, preventing endless loops, if combining an entry sentry with if-part and repetition.
 *
 * @author Micha Kiener
 */
public class StageRepetitionMaxCountTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/itemcontrol/StageRepetitionMaxCountTest.multipleTests.cmmn")
    public void testMaxCountOneWithIfPartCombination() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("stageWithRepetitionAndMaxInstanceCount").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(2);
        assertPlanItemInstanceState(planItemInstances, "Stage A", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);

        // start Stage A which should start Task A as well
        cmmnRuntimeService.setVariable(caseInstance.getId(), "enableStageA", true);
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(4);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE, WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);

        // complete Task A which terminates Stage A and starts Stage B
        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task A", ACTIVE))
                .variable("enableStageA", false)
                .variable("enableStageB", true)
                .trigger();

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(4);
        assertPlanItemInstanceState(planItemInstances, "Stage A", WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE, WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Task B", ACTIVE);

        // complete Task B
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task B", ACTIVE));

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(4);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE, WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Stage B", WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/itemcontrol/StageRepetitionMaxCountTest.multipleTests.cmmn")
    public void testMaxCountOneWithIfPartCombinationWithEventDeferred() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("stageWithRepetitionAndMaxInstanceCount").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(2);
        assertPlanItemInstanceState(planItemInstances, "Stage A", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);

        // start Stage A which should start Task A as well
        cmmnRuntimeService.setVariable(caseInstance.getId(), "enableStageA", true);
        cmmnRuntimeService.setVariable(caseInstance.getId(), "enableStageB", true);
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(4);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE, WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);

        // complete Task A which terminates Stage A and starts Stage B
        cmmnRuntimeService.setVariable(caseInstance.getId(), "enableStageA", false);
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task A", ACTIVE));

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(4);
        assertPlanItemInstanceState(planItemInstances, "Stage A", WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE, WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Task B", ACTIVE);

        // complete Task B
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task B", ACTIVE));

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(4);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE, WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Stage B", WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
    }
}
