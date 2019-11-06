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

import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.ACTIVE;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.AVAILABLE;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.ENABLED;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.junit.Test;

public class PlanItemExitSentryTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/PlanItemExitSentryTest.multipleTests.cmmn")
    public void testExitSentryTerminatingEnabledTaskA() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("sentryExitTypeTestOne").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertEquals(10, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Kill active and enabled tasks B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Kill active tasks A", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Kill active tasks B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task C", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task D", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task E", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task F", AVAILABLE);

        // trigger exit sentry with Task A still in enabled state -> nothing must be changed
        cmmnRuntimeService.completeUserEventListenerInstance(getPlanItemInstanceIdByName(planItemInstances,"Task A"));

        planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertEquals(10, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Kill active and enabled tasks B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Kill active tasks A", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Kill active tasks B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task C", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task D", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task E", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task F", AVAILABLE);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/PlanItemExitSentryTest.multipleTests.cmmn")
    public void testExitSentryTerminatingActiveTasksA() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("sentryExitTypeTestOne").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertEquals(10, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Kill active and enabled tasks B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Kill active tasks A", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Kill active tasks B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task C", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task D", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task E", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task F", AVAILABLE);

        // start Task A, check it to be in active state and then trigger its exit sentry, which will kill active task a, but leave it in enabled state
        cmmnRuntimeService.startPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task A"));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertEquals(10, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        cmmnRuntimeService.completeUserEventListenerInstance(getPlanItemInstanceIdByName(planItemInstances, "Kill active tasks A"));

        planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertEquals(10, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Kill active and enabled tasks B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Kill active tasks A", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Kill active tasks B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task C", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task D", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task E", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task F", AVAILABLE);
    }
}
