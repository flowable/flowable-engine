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
package org.flowable.cmmn.test.reactivation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.ACTIVE;

import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.common.engine.api.FlowableIllegalStateException;
import org.flowable.common.engine.impl.identity.Authentication;
import org.junit.Test;

public class CaseReactivateEventListenerAvailableConditionTest extends FlowableCmmnTestCase {
    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/reactivation/Case_Reactivation_Condition_Test.cmmn.xml")
    public void reactivationEventListenerNotAvailableConditionTest() {
        String previousUserId = Authentication.getAuthenticatedUserId();
        try {
            Authentication.setAuthenticatedUserId("caseReactivationConditionTest_user");
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("caseReactivationConditionTest")
                .start();
            List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
            assertThat(planItemInstances).isNotNull().hasSize(3);
            assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);

            cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task A"));

            assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isZero();
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();
            assertCaseInstanceEnded(caseInstance);

            assertThatThrownBy(() -> cmmnHistoryService.createCaseReactivationBuilder(caseInstance.getId())
                .transientVariable("reactivatePermission", "deny")
                .reactivate())
                .isExactlyInstanceOf(FlowableIllegalStateException.class)
                .hasMessageContaining("The case instance " + caseInstance.getId()
                    + " cannot be reactivated, as the available condition of its reactivate event listener did not evaluate to true.");
        } finally {
            Authentication.setAuthenticatedUserId(previousUserId);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/reactivation/Case_Reactivation_Condition_Test.cmmn.xml")
    public void reactivationEventListenerAvailableConditionTest() {
        String previousUserId = Authentication.getAuthenticatedUserId();
        try {
            Authentication.setAuthenticatedUserId("caseReactivationConditionTest_user");
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("caseReactivationConditionTest")
                .start();
            List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
            assertThat(planItemInstances).isNotNull().hasSize(3);
            assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);

            cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task A"));

            assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isZero();
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();
            assertCaseInstanceEnded(caseInstance);

            CaseInstance reactivatedCase = cmmnHistoryService.createCaseReactivationBuilder(caseInstance.getId())
                .transientVariable("reactivatePermission", "allow")
                .reactivate();
            assertThat(reactivatedCase).isNotNull();

            planItemInstances = getPlanItemInstances(reactivatedCase.getId());
            assertThat(planItemInstances).isNotNull().hasSize(3);
            assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);

        } finally {
            Authentication.setAuthenticatedUserId(previousUserId);
        }
    }
}
