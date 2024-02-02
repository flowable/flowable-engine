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
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.ACTIVE;

import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.common.engine.impl.identity.Authentication;
import org.junit.Test;

public class PreventDoublePlanItemInitializationTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/reactivation/Double_Plan_Item_Reactivation_Test.cmmn.xml")
    public void preventPlanItemFromDoubleReactivation() {
        String previousUserId = Authentication.getAuthenticatedUserId();
        try {
            Authentication.setAuthenticatedUserId("preventPlanItemFromDoubleReactivation_user");
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("doublePlanItemReactivationTest")
                .start();
            List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
            cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task A"));

            assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isZero();
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();
            assertCaseInstanceEnded(caseInstance);

            CaseInstance reactivatedCase = cmmnHistoryService.createCaseReactivationBuilder(caseInstance.getId()).reactivate();
            assertThat(reactivatedCase).isNotNull();

            planItemInstances = getPlanItemInstances(reactivatedCase.getId());
            assertThat(planItemInstances).isNotNull().hasSize(3);
            assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task B", ACTIVE);
        } finally {
            Authentication.setAuthenticatedUserId(previousUserId);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/reactivation/Double_Stage_Reactivation_Test.cmmn.xml")
    public void preventStageFromDoubleReactivation() {
        String previousUserId = Authentication.getAuthenticatedUserId();
        try {
            Authentication.setAuthenticatedUserId("preventStageFromDoubleReactivation_user");
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("doubleStageReactivationTest")
                .start();
            List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
            cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task A"));

            assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isZero();
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();
            assertCaseInstanceEnded(caseInstance);

            CaseInstance reactivatedCase = cmmnHistoryService.createCaseReactivationBuilder(caseInstance.getId()).reactivate();
            assertThat(reactivatedCase).isNotNull();

            planItemInstances = getPlanItemInstances(reactivatedCase.getId());
            assertThat(planItemInstances).isNotNull().hasSize(4);
            assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Reactivation Stage", ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task B", ACTIVE);
        } finally {
            Authentication.setAuthenticatedUserId(previousUserId);
        }
    }
}
