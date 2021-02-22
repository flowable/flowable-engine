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
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.ACTIVE;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.AVAILABLE;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.ENABLED;

import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.junit.Test;

/**
 * Testing exit sentries on plan items with exit type attribute and different combinations without repetition, but manual activation and as well with if-parts.
 *
 * @author Micha Kiener
 */
public class PlanItemNoRepetitionExitSentryTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/PlanItemNoRepetitionExitSentryTest.multipleTests.cmmn")
    public void testExitSentryTerminatingEnabledTaskA() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("sentryExitTypeTestTwo").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(10);
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
        cmmnRuntimeService.completeUserEventListenerInstance(getPlanItemInstanceIdByName(planItemInstances,"Kill active tasks A"));

        planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(10);
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
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/PlanItemNoRepetitionExitSentryTest.multipleTests.cmmn")
    public void testExitSentryTerminatingActiveTasksA() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("sentryExitTypeTestTwo").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(10);
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

        // start Task A, check it to be in active state and then trigger its exit sentry, which will kill active task a
        cmmnRuntimeService.startPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task A"));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(10);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        cmmnRuntimeService.completeUserEventListenerInstance(getPlanItemInstanceIdByName(planItemInstances, "Kill active tasks A"));

        planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(8);
        assertNoPlanItemInstance(planItemInstances, "Task A");
        assertPlanItemInstanceState(planItemInstances, "Kill active and enabled tasks B", AVAILABLE);
        assertNoPlanItemInstance(planItemInstances, "Kill active tasks A");
        assertPlanItemInstanceState(planItemInstances, "Kill active tasks B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task C", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task D", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task E", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task F", AVAILABLE);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/PlanItemNoRepetitionExitSentryTest.multipleTests.cmmn")
    public void testExitSentryTerminatingAvailableTaskB() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("sentryExitTypeTestTwo").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(10);
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

        // trigger exit sentry with Task B still in available state -> nothing must be changed
        cmmnRuntimeService.completeUserEventListenerInstance(getPlanItemInstanceIdByName(planItemInstances,"Kill active tasks B"));

        planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(10);
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
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/PlanItemNoRepetitionExitSentryTest.multipleTests.cmmn")
    public void testExitSentryTerminatingEnabledTaskB() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("sentryExitTypeTestTwo").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(10);
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

        // enable task B by setting its flag to true, satisfying its entry condition
        cmmnRuntimeService.setVariable(caseInstance.getId(), "enableTaskB", true);
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED);

        // trigger exit sentry with Task B still in enabled state -> nothing must be changed
        cmmnRuntimeService.completeUserEventListenerInstance(getPlanItemInstanceIdByName(planItemInstances,"Kill active tasks B"));

        planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(10);
        assertPlanItemInstanceState(planItemInstances, "Kill active and enabled tasks B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Kill active tasks A", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Kill active tasks B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task C", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task D", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task E", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task F", AVAILABLE);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/PlanItemNoRepetitionExitSentryTest.multipleTests.cmmn")
    public void testExitSentryTerminatingActiveTaskB() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("sentryExitTypeTestTwo").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(10);
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

        // enable task B by setting its flag to true, satisfying its entry condition
        cmmnRuntimeService.setVariable(caseInstance.getId(), "enableTaskB", true);
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED);

        // now even start Task B, so it becomes active
        cmmnRuntimeService.startPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances,"Task B", ENABLED));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertPlanItemInstanceState(planItemInstances, "Task B", ACTIVE);

        // trigger exit sentry with Task B still in active state -> must terminate the active task B
        cmmnRuntimeService.completeUserEventListenerInstance(getPlanItemInstanceIdByName(planItemInstances,"Kill active tasks B"));
        planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(7);
        assertNoPlanItemInstance(planItemInstances, "Task B");
        assertNoPlanItemInstance(planItemInstances, "Kill active and enabled tasks B");
        assertPlanItemInstanceState(planItemInstances, "Kill active tasks A", AVAILABLE);
        assertNoPlanItemInstance(planItemInstances, "Kill active tasks B");
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task C", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task D", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task E", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task F", AVAILABLE);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/PlanItemNoRepetitionExitSentryTest.multipleTests.cmmn")
    public void testExitSentryTerminatingEnabledTaskBWithExitTypeAlsoTerminatingEnabledInstances() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("sentryExitTypeTestTwo").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(10);
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

        // enable task B by setting its flag to true, satisfying its entry condition
        cmmnRuntimeService.setVariable(caseInstance.getId(), "enableTaskB", true);
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED);

        // trigger exit sentry with Task B still in enabled state -> must terminate the enabled task B
        cmmnRuntimeService.completeUserEventListenerInstance(getPlanItemInstanceIdByName(planItemInstances,"Kill active and enabled tasks B"));
        planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(7);
        assertNoPlanItemInstance(planItemInstances, "Task B");
        assertNoPlanItemInstance(planItemInstances, "Kill active and enabled tasks B");
        assertPlanItemInstanceState(planItemInstances, "Kill active tasks A", AVAILABLE);
        assertNoPlanItemInstance(planItemInstances, "Kill active tasks B");
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task C", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task D", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task E", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task F", AVAILABLE);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/PlanItemNoRepetitionExitSentryTest.multipleTests.cmmn")
    public void testExitSentryTerminatingActiveTaskBWithAlternateExitType() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("sentryExitTypeTestTwo").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(10);
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

        // enable task B by setting its flag to true, satisfying its entry condition
        cmmnRuntimeService.setVariable(caseInstance.getId(), "enableTaskB", true);
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED);

        // now even start Task B, so it becomes active
        cmmnRuntimeService.startPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances,"Task B", ENABLED));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertPlanItemInstanceState(planItemInstances, "Task B", ACTIVE);

        // trigger exit sentry with Task B still in active state -> must terminate the active task B
        cmmnRuntimeService.completeUserEventListenerInstance(getPlanItemInstanceIdByName(planItemInstances,"Kill active and enabled tasks B"));

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(7);
        assertNoPlanItemInstance(planItemInstances, "Task B");
        assertNoPlanItemInstance(planItemInstances, "Kill active and enabled tasks B");
        assertPlanItemInstanceState(planItemInstances, "Kill active tasks A", AVAILABLE);
        assertNoPlanItemInstance(planItemInstances, "Kill active tasks B");
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task C", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task D", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task E", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task F", AVAILABLE);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/PlanItemNoRepetitionExitSentryTest.multipleTests.cmmn")
    public void testExitSentryTerminatingActiveTaskD() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("sentryExitTypeTestTwo").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(10);
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

        // complete Task C, which will start Task D
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task C", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertPlanItemInstanceState(planItemInstances, "Task D", ACTIVE);
        assertNoPlanItemInstance(planItemInstances, "Task C");

        // trigger exit sentry on Task D which must kill it
        cmmnRuntimeService.setVariable(caseInstance.getId(), "exitActiveTasksD", true);
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(8);
        assertNoPlanItemInstance(planItemInstances, "Task D");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/PlanItemNoRepetitionExitSentryTest.multipleTests.cmmn")
    public void testExitSentryTerminatingActiveTaskF() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("sentryExitTypeTestTwo").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(10);
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

        // complete Task E, which will make Task F available
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task E", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertNoPlanItemInstance(planItemInstances, "Task E");
        assertPlanItemInstanceState(planItemInstances, "Task F", AVAILABLE);

        // start Task F by settings its flag to true, satisfying its if-part
        cmmnRuntimeService.setVariable(caseInstance.getId(), "enableTaskF", true);
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertPlanItemInstanceState(planItemInstances, "Task F", ACTIVE);

        // trigger exit sentry on Task F which must kill it
        cmmnRuntimeService.setVariable(caseInstance.getId(), "exitActiveTasksF", true);
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(8);
        assertNoPlanItemInstance(planItemInstances, "Task F");
    }
}
