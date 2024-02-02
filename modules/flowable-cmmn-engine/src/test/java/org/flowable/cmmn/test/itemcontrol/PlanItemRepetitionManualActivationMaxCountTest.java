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
public class PlanItemRepetitionManualActivationMaxCountTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/itemcontrol/PlanItemRepetitionManualActivationMaxCountTest.multipleTests.cmmn")
    public void testMaxCountOneWithIfPartCombination() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("repetitionMaxInstanceCountTestTwo").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(3);
        assertPlanItemInstanceState(planItemInstances, "Task A", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task C", AVAILABLE);

        // enable Task A by setting its enable flag to true
        cmmnRuntimeService.setVariable(caseInstance.getId(), "enableTaskA", true);

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(4);
        assertPlanItemInstanceState(planItemInstances, "Task A", ENABLED, WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Task B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task C", AVAILABLE);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/itemcontrol/PlanItemRepetitionManualActivationMaxCountTest.multipleTests.cmmn")
    public void testMaxCountOneWithIfPartCombinationWithCompletion() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("repetitionMaxInstanceCountTestTwo").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(3);
        assertPlanItemInstanceState(planItemInstances, "Task A", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task C", AVAILABLE);

        // enable Task A by setting its enable flag to true
        cmmnRuntimeService.setVariable(caseInstance.getId(), "enableTaskA", true);

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(4);
        assertPlanItemInstanceState(planItemInstances, "Task A", ENABLED, WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Task B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task C", AVAILABLE);

        // start and then complete Task A
        cmmnRuntimeService.startPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task A", ENABLED));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(4);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE, WAITING_FOR_REPETITION);
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task A", ACTIVE));

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(4);
        assertPlanItemInstanceState(planItemInstances, "Task A", ENABLED, WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Task B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task C", AVAILABLE);

        // disable Task A again by setting its enable flag to false
        cmmnRuntimeService.setVariable(caseInstance.getId(), "enableTaskA", false);

        // start and then complete Task A
        cmmnRuntimeService.startPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task A", ENABLED));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(4);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE, WAITING_FOR_REPETITION);
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task A", ACTIVE));

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(3);
        assertPlanItemInstanceState(planItemInstances, "Task A", WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Task B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task C", AVAILABLE);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/itemcontrol/PlanItemRepetitionManualActivationMaxCountTest.multipleTests.cmmn")
    public void testBackwardsCompatibilityWithLifecycleListenerPreventingAnEndlessLoop() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("repetitionMaxInstanceCountTestTwo").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(3);
        assertPlanItemInstanceState(planItemInstances, "Task A", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task C", AVAILABLE);

        // enable Task B by setting its enable flag to true
        cmmnRuntimeService.setVariable(caseInstance.getId(), "enableTaskB", true);

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(4);
        assertPlanItemInstanceState(planItemInstances, "Task A", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED, WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Task C", AVAILABLE);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/itemcontrol/PlanItemRepetitionManualActivationMaxCountTest.multipleTests.cmmn")
    public void testUnlimitedInstancesWithRepetitionConditionControl() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("repetitionMaxInstanceCountTestTwo").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(3);
        assertPlanItemInstanceState(planItemInstances, "Task A", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task C", AVAILABLE);

        // enable Task C by setting its enable flag to true
        cmmnRuntimeService.setVariable(caseInstance.getId(), "enableTaskC", true);

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(7);
        assertPlanItemInstanceState(planItemInstances, "Task A", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task C", ENABLED, ENABLED, ENABLED, ENABLED, WAITING_FOR_REPETITION);
    }
}
