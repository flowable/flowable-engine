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
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.COMPLETED;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.ENABLED;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.TERMINATED;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.WAITING_FOR_REPETITION;

import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.junit.Test;

/**
 * Testing exit sentries on plan items with exit type attribute and different combinations with repetition, manual activation and as well with if-parts.
 *
 * @author Micha Kiener
 */
public class PlanItemExitSentryTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/PlanItemExitSentryTest.multipleTests.cmmn")
    public void testExitSentryTerminatingEnabledTaskA() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("sentryExitTypeTestOne").start();

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
        cmmnRuntimeService.completeUserEventListenerInstance(getPlanItemInstanceIdByName(planItemInstances, "Kill active tasks A"));

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
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/PlanItemExitSentryTest.multipleTests.cmmn")
    public void testExitSentryTerminatingActiveTasksA() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("sentryExitTypeTestOne").start();

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

        // start Task A, check it to be in active state and then trigger its exit sentry, which will kill active task a, but leave it in enabled state
        cmmnRuntimeService.startPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task A"));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(10);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        cmmnRuntimeService.completeUserEventListenerInstance(getPlanItemInstanceIdByName(planItemInstances, "Kill active tasks A"));

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
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/PlanItemExitSentryTest.multipleTests.cmmn")
    public void testExitSentryTerminatingAvailableTaskB() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("sentryExitTypeTestOne").start();

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
        cmmnRuntimeService.completeUserEventListenerInstance(getPlanItemInstanceIdByName(planItemInstances, "Kill active tasks B"));

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
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/PlanItemExitSentryTest.multipleTests.cmmn")
    public void testExitSentryTerminatingEnabledTaskB() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("sentryExitTypeTestOne").start();

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
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED, WAITING_FOR_REPETITION);

        // trigger exit sentry with Task B still in enabled state -> nothing must be changed
        cmmnRuntimeService.completeUserEventListenerInstance(getPlanItemInstanceIdByName(planItemInstances, "Kill active tasks B"));

        planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(11);
        assertPlanItemInstanceState(planItemInstances, "Kill active and enabled tasks B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Kill active tasks A", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Kill active tasks B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED, WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Task C", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task D", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task E", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task F", AVAILABLE);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/PlanItemExitSentryTest.multipleTests.cmmn")
    public void testExitSentryTerminatingActiveTaskB() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("sentryExitTypeTestOne").start();

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
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED, WAITING_FOR_REPETITION);

        // now even start Task B, so it becomes active
        cmmnRuntimeService.startPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task B", ENABLED));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertPlanItemInstanceState(planItemInstances, "Task B", ACTIVE, WAITING_FOR_REPETITION);

        // trigger exit sentry with Task B still in active state -> must terminate the active task B, but leave it in waiting for repetition state
        cmmnRuntimeService.completeUserEventListenerInstance(getPlanItemInstanceIdByName(planItemInstances, "Kill active tasks B"));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertPlanItemInstanceState(planItemInstances, "Task B", WAITING_FOR_REPETITION);

        // re-enable task B by again settings its flag to true, satisfying its entry condition
        cmmnRuntimeService.setVariable(caseInstance.getId(), "enableTaskB", true);
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED, WAITING_FOR_REPETITION);

        planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(11);
        assertPlanItemInstanceState(planItemInstances, "Kill active and enabled tasks B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Kill active tasks A", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Kill active tasks B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED, WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Task C", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task D", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task E", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task F", AVAILABLE);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/PlanItemExitSentryTest.multipleTests.cmmn")
    public void testExitSentryTerminatingEnabledTaskBWithExitTypeAlsoTerminatingEnabledInstances() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("sentryExitTypeTestOne").start();

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
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED, WAITING_FOR_REPETITION);

        // trigger exit sentry with Task B still in enabled state -> must terminate the enabled task B, but leave it in waiting for repetition state
        cmmnRuntimeService.completeUserEventListenerInstance(getPlanItemInstanceIdByName(planItemInstances, "Kill active and enabled tasks B"));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertPlanItemInstanceState(planItemInstances, "Task B", WAITING_FOR_REPETITION);

        // re-enable task B by again settings its flag to true, satisfying its entry condition
        cmmnRuntimeService.setVariable(caseInstance.getId(), "enableTaskB", true);
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED, WAITING_FOR_REPETITION);

        planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(11);
        assertPlanItemInstanceState(planItemInstances, "Kill active and enabled tasks B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Kill active tasks A", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Kill active tasks B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED, WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Task C", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task D", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task E", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task F", AVAILABLE);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/PlanItemExitSentryTest.multipleTests.cmmn")
    public void testExitSentryTerminatingActiveTaskBWithAlternateExitType() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("sentryExitTypeTestOne").start();

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
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED, WAITING_FOR_REPETITION);

        // now even start Task B, so it becomes active
        cmmnRuntimeService.startPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task B", ENABLED));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertPlanItemInstanceState(planItemInstances, "Task B", ACTIVE, WAITING_FOR_REPETITION);

        // trigger exit sentry with Task B still in active state -> must terminate the active task B, but leave it in waiting for repetition state
        cmmnRuntimeService.completeUserEventListenerInstance(getPlanItemInstanceIdByName(planItemInstances, "Kill active and enabled tasks B"));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertPlanItemInstanceState(planItemInstances, "Task B", WAITING_FOR_REPETITION);

        // re-enable task B by again settings its flag to true, satisfying its entry condition
        cmmnRuntimeService.setVariable(caseInstance.getId(), "enableTaskB", true);
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED, WAITING_FOR_REPETITION);

        planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(11);
        assertPlanItemInstanceState(planItemInstances, "Kill active and enabled tasks B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Kill active tasks A", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Kill active tasks B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED, WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Task C", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task D", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task E", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task F", AVAILABLE);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/PlanItemExitSentryTest.multipleTests.cmmn")
    public void testExitSentryTerminatingMultipleActiveTaskBWithAlternateExitType() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("sentryExitTypeTestOne").start();

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
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED, WAITING_FOR_REPETITION);

        // now even start Task B, so it becomes active
        cmmnRuntimeService.startPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task B", ENABLED));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertPlanItemInstanceState(planItemInstances, "Task B", ACTIVE, WAITING_FOR_REPETITION);

        // repeat this for another two new tasks
        cmmnRuntimeService.setVariable(caseInstance.getId(), "enableTaskB", true);
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        cmmnRuntimeService.startPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task B", ENABLED));
        cmmnRuntimeService.setVariable(caseInstance.getId(), "enableTaskB", true);
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        cmmnRuntimeService.startPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task B", ENABLED));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertPlanItemInstanceState(planItemInstances, "Task B", ACTIVE, ACTIVE, ACTIVE, WAITING_FOR_REPETITION);

        // trigger exit sentry with Task B still in active state -> must terminate the active task B, but leave it in waiting for repetition state
        cmmnRuntimeService.completeUserEventListenerInstance(getPlanItemInstanceIdByName(planItemInstances, "Kill active and enabled tasks B"));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertPlanItemInstanceState(planItemInstances, "Task B", WAITING_FOR_REPETITION);

        // re-enable task B by again settings its flag to true, satisfying its entry condition
        cmmnRuntimeService.setVariable(caseInstance.getId(), "enableTaskB", true);
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED, WAITING_FOR_REPETITION);

        planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(11);
        assertPlanItemInstanceState(planItemInstances, "Kill active and enabled tasks B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Kill active tasks A", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Kill active tasks B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task A", ENABLED);
        assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED, WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(planItemInstances, "Task C", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task D", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task E", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task F", AVAILABLE);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/PlanItemExitSentryTest.multipleTests.cmmn")
    public void testExitSentryTerminatingActiveTaskD() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("sentryExitTypeTestOne").start();

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

        // complete Task C, which will start Task D (and start another Task C as it has repetition)
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task C", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertPlanItemInstanceState(planItemInstances, "Task C", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task D", ACTIVE, WAITING_FOR_REPETITION);

        // trigger exit sentry on Task D which must kill it, but leave it in waiting for repetition state
        cmmnRuntimeService.setVariable(caseInstance.getId(), "exitActiveTasksD", true);
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertPlanItemInstanceState(planItemInstances, "Task D", WAITING_FOR_REPETITION);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/PlanItemExitSentryTest.multipleTests.cmmn")
    public void testExitSentryTerminatingMultipleActiveTasksD() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("sentryExitTypeTestOne").start();

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

        // complete Task C, which will start Task D (and start another Task C as it has repetition)
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task C", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertPlanItemInstanceState(planItemInstances, "Task C", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task D", ACTIVE, WAITING_FOR_REPETITION);
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task C", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertPlanItemInstanceState(planItemInstances, "Task C", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task D", ACTIVE, ACTIVE, WAITING_FOR_REPETITION);

        // trigger exit sentry on Task D which must kill all active instances, but leave it in waiting for repetition state
        cmmnRuntimeService.setVariable(caseInstance.getId(), "exitActiveTasksD", true);
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertPlanItemInstanceState(planItemInstances, "Task D", WAITING_FOR_REPETITION);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/PlanItemExitSentryTest.multipleTests.cmmn")
    public void testExitSentryTerminatingActiveTaskF() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("sentryExitTypeTestOne").start();

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

        // complete Task E, which will make Task F available (and start another Task E as it has repetition)
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task E", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertPlanItemInstanceState(planItemInstances, "Task E", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task F", AVAILABLE);

        // start Task F by settings its flag to true, satisfying its if-part
        cmmnRuntimeService.setVariable(caseInstance.getId(), "enableTaskF", true);
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertPlanItemInstanceState(planItemInstances, "Task E", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task F", ACTIVE, WAITING_FOR_REPETITION);

        // trigger exit sentry on Task F which must kill it, but leave it in waiting for repetition state
        cmmnRuntimeService.setVariable(caseInstance.getId(), "exitActiveTasksF", true);
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertPlanItemInstanceState(planItemInstances, "Task F", WAITING_FOR_REPETITION);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/PlanItemExitSentryTest.multipleTests.cmmn")
    public void testExitSentryTerminatingMultipleActiveTaskF() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("sentryExitTypeTestOne").start();

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

        // complete Task E, which will make Task F available (and start another Task E as it has repetition)
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task E", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertPlanItemInstanceState(planItemInstances, "Task E", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task F", AVAILABLE);

        // start Task F by settings its flag to true, satisfying its if-part
        cmmnRuntimeService.setVariable(caseInstance.getId(), "enableTaskF", true);
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertPlanItemInstanceState(planItemInstances, "Task E", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task F", ACTIVE, WAITING_FOR_REPETITION);

        // complete another Task E which will directly start Task F again as the if-part is still satisfied
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task E", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task E", ACTIVE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertPlanItemInstanceState(planItemInstances, "Task E", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task F", ACTIVE, ACTIVE, ACTIVE, WAITING_FOR_REPETITION);

        // trigger exit sentry on Task F which must kill it, but leave it in waiting for repetition state
        cmmnRuntimeService.setVariable(caseInstance.getId(), "exitActiveTasksF", true);
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertPlanItemInstanceState(planItemInstances, "Task F", WAITING_FOR_REPETITION);
    }

    @Test
    @CmmnDeployment
    public void testEntryAndExitSentryUsingSameVariable() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myExample").start();

        // State after start
        assertPlanItemInstanceState(caseInstance, "Set true", ENABLED);
        assertPlanItemInstanceState(caseInstance, "Set false", ENABLED);
        assertPlanItemInstanceState(caseInstance, "Event deferred task", AVAILABLE);
        assertPlanItemInstanceState(caseInstance, "On Event Task", AVAILABLE);

        // Starting 'set true' will activate both user tasks
        PlanItemInstance setTruePlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("Set true").singleResult();
        cmmnRuntimeService.startPlanItemInstance(setTruePlanItemInstance.getId());

        assertPlanItemInstanceState(caseInstance, "Set true", ENABLED, COMPLETED);
        assertPlanItemInstanceState(caseInstance, "Set false", ENABLED);
        assertPlanItemInstanceState(caseInstance, "Event deferred task", ACTIVE, WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(caseInstance, "On Event Task", ACTIVE, WAITING_FOR_REPETITION);

        // Starting again shouldn't make a difference as maxInstanceCount is 1
        setTruePlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("Set true").singleResult();
        cmmnRuntimeService.startPlanItemInstance(setTruePlanItemInstance.getId());

        assertPlanItemInstanceState(caseInstance, "Set true", ENABLED, COMPLETED, COMPLETED);
        assertPlanItemInstanceState(caseInstance, "Set false", ENABLED);
        assertPlanItemInstanceState(caseInstance, "Event deferred task", ACTIVE, WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(caseInstance, "On Event Task", ACTIVE, WAITING_FOR_REPETITION);

        // Starting 'set false' should terminate the user tasks
        PlanItemInstance setFalsePlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("Set false").singleResult();
        cmmnRuntimeService.startPlanItemInstance(setFalsePlanItemInstance.getId());

        assertPlanItemInstanceState(caseInstance, "Set true", ENABLED, COMPLETED, COMPLETED);
        assertPlanItemInstanceState(caseInstance, "Set false", ENABLED, COMPLETED);
        assertPlanItemInstanceState(caseInstance, "Event deferred task", TERMINATED, WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(caseInstance, "On Event Task", TERMINATED, WAITING_FOR_REPETITION);

        // Starting again 'set true'
        setTruePlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("Set true").singleResult();
        cmmnRuntimeService.startPlanItemInstance(setTruePlanItemInstance.getId());

        assertPlanItemInstanceState(caseInstance, "Set true", ENABLED, COMPLETED, COMPLETED, COMPLETED);
        assertPlanItemInstanceState(caseInstance, "Set false", ENABLED, COMPLETED);
        assertPlanItemInstanceState(caseInstance, "Event deferred task", ACTIVE, TERMINATED, WAITING_FOR_REPETITION);
        assertPlanItemInstanceState(caseInstance, "On Event Task", ACTIVE, TERMINATED, WAITING_FOR_REPETITION);

    }

}
