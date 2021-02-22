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
public class PlanItemRepetitionOnPartMaxCountUnlimitedTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/itemcontrol/PlanItemRepetitionOnPartMaxCountUnlimitedTest.multipleTests.cmmn")
    public void testMaxCountOneWithManualActivation() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("repetitionMaxInstanceCountUnlimitedWithOnPart").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(9);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task C", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task D", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task E", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task F", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task G", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task H", AVAILABLE);

        // complete Task A which should enable Task B
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task A"));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(10);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED, WAITING_FOR_REPETITION);

        // completing Task A again must start another instance of B
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task A"));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(11);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED, ENABLED, WAITING_FOR_REPETITION);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/itemcontrol/PlanItemRepetitionOnPartMaxCountUnlimitedTest.multipleTests.cmmn")
    public void testMaxCountOneWithoutManualActivation() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("repetitionMaxInstanceCountUnlimitedWithOnPart").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(9);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task C", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task D", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task E", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task F", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task G", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task H", AVAILABLE);

        // complete Task C which should start Task D
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task C"));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(10);
        assertPlanItemInstanceState(planItemInstances, "Task C", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task D", ACTIVE, WAITING_FOR_REPETITION);

        // completing Task C again must start another instance of D
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task C"));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(11);
        assertPlanItemInstanceState(planItemInstances, "Task C", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task D", ACTIVE, ACTIVE, WAITING_FOR_REPETITION);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/itemcontrol/PlanItemRepetitionOnPartMaxCountUnlimitedTest.multipleTests.cmmn")
    public void testMaxCountOneWithManualActivationAndIfPart() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("repetitionMaxInstanceCountUnlimitedWithOnPart").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(9);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task C", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task D", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task E", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task F", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task G", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task H", AVAILABLE);

        // complete Task E which should not yet do anything as we didn't set yet the enabled flag
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task E"));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(9);
        assertPlanItemInstanceState(planItemInstances, "Task E", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task F", AVAILABLE);

        // enable Task F by setting its condition to true
        cmmnRuntimeService.setVariable(caseInstance.getId(), "enableTaskF", true);
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(10);
        assertPlanItemInstanceState(planItemInstances, "Task E", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task F", ENABLED, WAITING_FOR_REPETITION);

        // completing Task E again must start another instance of F
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task E"));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(11);
        assertPlanItemInstanceState(planItemInstances, "Task E", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task F", ENABLED, ENABLED, WAITING_FOR_REPETITION);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/itemcontrol/PlanItemRepetitionOnPartMaxCountUnlimitedTest.multipleTests.cmmn")
    public void testMaxCountOneWithoutManualActivationAndIfPart() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("repetitionMaxInstanceCountUnlimitedWithOnPart").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(9);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task C", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task D", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task E", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task F", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task G", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task H", AVAILABLE);

        // complete Task G should not yet enable Task G as its condition is not yet true
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task G"));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(9);
        assertPlanItemInstanceState(planItemInstances, "Task G", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task H", AVAILABLE);

        // enable Task H by setting its condition to true
        cmmnRuntimeService.setVariable(caseInstance.getId(), "enableTaskH", true);
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(10);
        assertPlanItemInstanceState(planItemInstances, "Task G", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task H", ACTIVE, WAITING_FOR_REPETITION);

        // completing Task G again must start another instance of H
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task G"));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(11);
        assertPlanItemInstanceState(planItemInstances, "Task G", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task H", ACTIVE, ACTIVE, WAITING_FOR_REPETITION);
    }
}
