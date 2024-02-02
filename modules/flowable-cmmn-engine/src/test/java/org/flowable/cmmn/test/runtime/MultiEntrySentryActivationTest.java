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

import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.junit.Test;

/**
 * Testing stage activation with multiple sentries / on-parts and conditions.
 *
 * @author Micha Kiener
 */
public class MultiEntrySentryActivationTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment
    public void testMultiSentryActivation() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("caseActivationExampleWithMoreThanOneIncomingSentry")
                .start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(4);

        assertPlanItemInstanceState(planItemInstances, "Initial Stage", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Main Form", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage A", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);

        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(getPlanItemInstanceIdByNameAndState(planItemInstances, "Main Form", ACTIVE))
                .variable("activateStageA", true)
                .variable("activateStageB", false)
                .trigger();

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(3);

        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage A Form", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Stage A Form", ACTIVE));

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(2);

        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B Form", ACTIVE);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Stage B Form", ACTIVE));

        assertCaseInstanceEnded(caseInstance);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/MultiEntrySentryActivationTest.testMultiSentryActivation.cmmn")
    public void testMultiSentryActivationAlternatePath() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("caseActivationExampleWithMoreThanOneIncomingSentry")
                .start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(4);

        assertPlanItemInstanceState(planItemInstances, "Initial Stage", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Main Form", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage A", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);

        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(getPlanItemInstanceIdByNameAndState(planItemInstances, "Main Form", ACTIVE))
                .variable("activateStageA", false)
                .variable("activateStageB", true)
                .trigger();

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(3);

        assertPlanItemInstanceState(planItemInstances, "Stage A", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B Form", ACTIVE);

        cmmnRuntimeService.setVariable(caseInstance.getId(), "activateStageA", true);

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(4);

        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage A Form", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B Form", ACTIVE);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Stage A Form", ACTIVE));

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(2);

        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B Form", ACTIVE);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Stage B Form", ACTIVE));

        assertCaseInstanceEnded(caseInstance);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/MultiEntrySentryActivationTest.testMultiSentryActivation.cmmn")
    public void testMultiSentryActivationSameTime() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("caseActivationExampleWithMoreThanOneIncomingSentry")
                .start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(4);

        assertPlanItemInstanceState(planItemInstances, "Initial Stage", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Main Form", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage A", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", AVAILABLE);

        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(getPlanItemInstanceIdByNameAndState(planItemInstances, "Main Form", ACTIVE))
                .variable("activateStageA", true)
                .variable("activateStageB", true)
                .trigger();

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(4);

        assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage A Form", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B Form", ACTIVE);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Stage A Form", ACTIVE));

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(2);

        assertPlanItemInstanceState(planItemInstances, "Stage B", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B Form", ACTIVE);

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Stage B Form", ACTIVE));

        assertCaseInstanceEnded(caseInstance);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();
    }
}
