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
public class PlanItemRepetitionMaxInstanceCountWithNumberTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/itemcontrol/PlanItemRepetitionMaxInstanceCountWithNumberTest.multipleTests.cmmn")
    public void testMaxCountTenWithIfPartCombination() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("maxInstanceCountNumberTest").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(5);
        assertPlanItemInstanceState(planItemInstances, "Task A", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task C", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task D", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task E", AVAILABLE);

        // enable Task A which should create 10 instances
        cmmnRuntimeService.setVariable(caseInstance.getId(), "enableTaskA", true);

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(15);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE, ACTIVE, ACTIVE, ACTIVE, ACTIVE, ACTIVE, ACTIVE, ACTIVE, ACTIVE, ACTIVE,
                WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task C", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task D", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task E", AVAILABLE);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/itemcontrol/PlanItemRepetitionMaxInstanceCountWithNumberTest.multipleTests.cmmn")
    public void testMaxCountThreeWithManualTrigger() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("maxInstanceCountNumberTest").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(5);
        assertPlanItemInstanceState(planItemInstances, "Task A", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task C", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task D", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task E", AVAILABLE);

        // manually trigger Task C by completing Task B multiple times (actually 4 times, but as it is limited to count 3, there must only be 3 instances)
        startAndCompleteTaskMultipleTimes(caseInstance.getId(), "Task B", 4);

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(8);

        assertPlanItemInstanceState(planItemInstances, "Task A", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task C", ACTIVE, ACTIVE, ACTIVE, WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Task D", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task E", AVAILABLE);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/itemcontrol/PlanItemRepetitionMaxInstanceCountWithNumberTest.multipleTests.cmmn")
    public void testMaxCountFiveWithManualTriggerAndIfPartNonDeferred() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("maxInstanceCountNumberTest").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(5);
        assertPlanItemInstanceState(planItemInstances, "Task A", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task C", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task D", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task E", AVAILABLE);

        // enable Task E upfront for this test
        cmmnRuntimeService.setVariable(caseInstance.getId(), "enableTaskE", true);

        // manually trigger Task E by completing Task D multiple times (actually 6 times, but as it is limited to count 5, there must only be 5 instances)
        startAndCompleteTaskMultipleTimes(caseInstance.getId(), "Task D", 6);

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(10);

        assertPlanItemInstanceState(planItemInstances, "Task A", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task C", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task D", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task E", ACTIVE, ACTIVE, ACTIVE, ACTIVE, ACTIVE, WAITING_FOR_REPETITION);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/itemcontrol/PlanItemRepetitionMaxInstanceCountWithNumberTest.multipleTests.cmmn")
    public void testMaxCountFiveWithManualTriggerAndIfPartDeferred() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("maxInstanceCountNumberTest").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(5);
        assertPlanItemInstanceState(planItemInstances, "Task A", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task C", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task D", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task E", AVAILABLE);

        // manually trigger Task E by completing Task D three times, but as E is not yet enabled, it must not yet be active
        startAndCompleteTaskMultipleTimes(caseInstance.getId(), "Task D", 3);
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(5);
        assertPlanItemInstanceState(planItemInstances, "Task A", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task C", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task D", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task E", AVAILABLE);

        // now enable Task E
        cmmnRuntimeService.setVariable(caseInstance.getId(), "enableTaskE", true);

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(10);

        assertPlanItemInstanceState(planItemInstances, "Task A", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task C", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task D", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task E", ACTIVE, ACTIVE, ACTIVE, ACTIVE, ACTIVE, WAITING_FOR_REPETITION);
    }

    protected void startAndCompleteTaskMultipleTimes(String caseInstanceId, String taskName, int count) {
        for (int ii = 0; ii < count; ii++) {
            List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstanceId);
            cmmnRuntimeService.startPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, taskName, ENABLED));
            cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, taskName, ENABLED));
        }
    }
}
