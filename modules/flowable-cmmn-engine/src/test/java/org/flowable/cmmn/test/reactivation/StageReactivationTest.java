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
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.AVAILABLE;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.COMPLETED;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.ENABLED;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.TERMINATED;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.UNAVAILABLE;

import java.util.List;

import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.common.engine.impl.identity.Authentication;
import org.junit.Test;

public class StageReactivationTest  extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/reactivation/Stage_Reactivation_Test_Case.cmmn.xml")
    public void reactivateStageAWithoutTaskBTest() {
        String previousUserId = Authentication.getAuthenticatedUserId();
        try {
            Authentication.setAuthenticatedUserId("JohnDoe");
            final HistoricCaseInstance historicCase = createAndFinishSimpleCase(false);

            CaseInstance reactivatedCase = cmmnHistoryService.createCaseReactivationBuilder(historicCase.getId())
                .transientVariable("reactivateStageA", true)
                .variable("ignoreTaskB", true)
                .reactivate();

            assertThat(reactivatedCase).isNotNull();

            // with reactivation on Stage A, it must become active again as well as its task A (no specific reactivation rule, but the default one on the
            // listener is "default")
            List<PlanItemInstance> planItemInstances = getAllPlanItemInstances(reactivatedCase.getId());
            assertThat(planItemInstances).isNotNull().hasSize(11);
            assertPlanItemInstanceState(planItemInstances, "Reactivate case", TERMINATED, COMPLETED, UNAVAILABLE);
            assertPlanItemInstanceState(planItemInstances, "Stage A", COMPLETED, ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Stage B", COMPLETED, AVAILABLE);
            assertPlanItemInstanceState(planItemInstances, "Task A", COMPLETED, ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task B", COMPLETED);
            assertPlanItemInstanceState(planItemInstances, "Task C", TERMINATED);

            // triggering task a must reactivate stage B
            planItemInstances = getPlanItemInstances(reactivatedCase.getId());
            assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
            cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task A"));

            // reactivating stage B must ignore task B and leave task C as it was not completed before, however, as the stage is autocomplete, it will complete
            // immediately and hence complete the case as well
            planItemInstances = getAllPlanItemInstances(reactivatedCase.getId());
            assertThat(planItemInstances).isNotNull().hasSize(0);

            assertCaseInstanceEnded(reactivatedCase);
        } finally {
            Authentication.setAuthenticatedUserId(previousUserId);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/reactivation/Stage_Reactivation_Test_Case.cmmn.xml")
    public void reactivateStageAWithTaskBTest() {
        String previousUserId = Authentication.getAuthenticatedUserId();
        try {
            Authentication.setAuthenticatedUserId("JohnDoe");
            final HistoricCaseInstance historicCase = createAndFinishSimpleCase(false);

            CaseInstance reactivatedCase = cmmnHistoryService.createCaseReactivationBuilder(historicCase.getId())
                .transientVariable("reactivateStageA", true)
                .reactivate();

            assertThat(reactivatedCase).isNotNull();

            // with reactivation on Stage A, it must become active again as well as its task A (no specific reactivation rule, but the default one on the
            // listener is "default")
            List<PlanItemInstance> planItemInstances = getAllPlanItemInstances(reactivatedCase.getId());
            assertThat(planItemInstances).isNotNull().hasSize(11);
            assertPlanItemInstanceState(planItemInstances, "Reactivate case", TERMINATED, COMPLETED, UNAVAILABLE);
            assertPlanItemInstanceState(planItemInstances, "Stage A", COMPLETED, ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Stage B", COMPLETED, AVAILABLE);
            assertPlanItemInstanceState(planItemInstances, "Task A", COMPLETED, ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task B", COMPLETED);
            assertPlanItemInstanceState(planItemInstances, "Task C", TERMINATED);

            // triggering task A must reactivate stage B
            planItemInstances = getPlanItemInstances(reactivatedCase.getId());
            assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
            cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task A"));

            // reactivating stage B must not ignore task B and leave task C as it was not completed before
            planItemInstances = getAllPlanItemInstances(reactivatedCase.getId());
            assertThat(planItemInstances).isNotNull().hasSize(13);
            assertPlanItemInstanceState(planItemInstances, "Reactivate case", TERMINATED, COMPLETED, UNAVAILABLE);
            assertPlanItemInstanceState(planItemInstances, "Stage A", COMPLETED, COMPLETED);
            assertPlanItemInstanceState(planItemInstances, "Stage B", COMPLETED, ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task A", COMPLETED, COMPLETED);
            assertPlanItemInstanceState(planItemInstances, "Task B", COMPLETED, ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task C", TERMINATED, ENABLED);

            // triggering task B must autocomplete stage B and the case
            planItemInstances = getPlanItemInstances(reactivatedCase.getId());
            assertPlanItemInstanceState(planItemInstances, "Task B", ACTIVE);
            cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task B"));

            assertCaseInstanceEnded(reactivatedCase);
        } finally {
            Authentication.setAuthenticatedUserId(previousUserId);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/reactivation/Stage_Reactivation_Test_Case.cmmn.xml")
    public void reactivateStageAWithTaskBAndCompletedTaskCTest() {
        String previousUserId = Authentication.getAuthenticatedUserId();
        try {
            Authentication.setAuthenticatedUserId("JohnDoe");
            final HistoricCaseInstance historicCase = createAndFinishSimpleCase(true);

            CaseInstance reactivatedCase = cmmnHistoryService.createCaseReactivationBuilder(historicCase.getId())
                .transientVariable("reactivateStageA", true)
                .reactivate();

            assertThat(reactivatedCase).isNotNull();

            // with reactivation on Stage A, it must become active again as well as its task A (no specific reactivation rule, but the default one on the
            // listener is "default")
            List<PlanItemInstance> planItemInstances = getAllPlanItemInstances(reactivatedCase.getId());
            assertThat(planItemInstances).isNotNull().hasSize(12);
            assertPlanItemInstanceState(planItemInstances, "Reactivate case", TERMINATED, COMPLETED, UNAVAILABLE);
            assertPlanItemInstanceState(planItemInstances, "Stage A", COMPLETED, ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Stage B", COMPLETED, AVAILABLE);
            assertPlanItemInstanceState(planItemInstances, "Task A", COMPLETED, ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task B", COMPLETED);
            assertPlanItemInstanceState(planItemInstances, "Task C", COMPLETED, TERMINATED);

            // triggering task A must reactivate stage B
            planItemInstances = getPlanItemInstances(reactivatedCase.getId());
            assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
            cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task A"));

            // reactivating stage B must not ignore task B and as task C was completed before, it must be ignored now
            planItemInstances = getAllPlanItemInstances(reactivatedCase.getId());
            assertThat(planItemInstances).isNotNull().hasSize(14);
            assertPlanItemInstanceState(planItemInstances, "Reactivate case", TERMINATED, COMPLETED, UNAVAILABLE);
            assertPlanItemInstanceState(planItemInstances, "Stage A", COMPLETED, COMPLETED);
            assertPlanItemInstanceState(planItemInstances, "Stage B", COMPLETED, ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task A", COMPLETED, COMPLETED);
            assertPlanItemInstanceState(planItemInstances, "Task B", COMPLETED, ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task C", ENABLED, COMPLETED, TERMINATED);

            // triggering task B must autocomplete stage B and the case
            planItemInstances = getPlanItemInstances(reactivatedCase.getId());
            assertPlanItemInstanceState(planItemInstances, "Task B", ACTIVE);
            cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task B"));

            assertCaseInstanceEnded(reactivatedCase);
        } finally {
            Authentication.setAuthenticatedUserId(previousUserId);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/reactivation/Stage_Reactivation_Test_Case.cmmn.xml")
    public void reactivateStageBTest() {
        String previousUserId = Authentication.getAuthenticatedUserId();
        try {
            Authentication.setAuthenticatedUserId("JohnDoe");
            final HistoricCaseInstance historicCase = createAndFinishSimpleCase(false);

            CaseInstance reactivatedCase = cmmnHistoryService.createCaseReactivationBuilder(historicCase.getId())
                .transientVariable("reactivateStageB", true)
                .reactivate();

            assertThat(reactivatedCase).isNotNull();

            // with reactivation on Stage B, it must become active again as well as its task B (no specific reactivation rule, but the default one on the
            // listener is "default")
            List<PlanItemInstance> planItemInstances = getAllPlanItemInstances(reactivatedCase.getId());
            assertThat(planItemInstances).isNotNull().hasSize(10);
            assertPlanItemInstanceState(planItemInstances, "Reactivate case", TERMINATED, COMPLETED, UNAVAILABLE);
            assertPlanItemInstanceState(planItemInstances, "Stage A", COMPLETED);
            assertPlanItemInstanceState(planItemInstances, "Stage B", COMPLETED, ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task A", COMPLETED);
            assertPlanItemInstanceState(planItemInstances, "Task B", COMPLETED, ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task C", TERMINATED);

            // triggering task B must finish stage B as it has autocomplete
            planItemInstances = getPlanItemInstances(reactivatedCase.getId());
            assertPlanItemInstanceState(planItemInstances, "Task B", ACTIVE);
            cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task B"));

            planItemInstances = getAllPlanItemInstances(reactivatedCase.getId());
            assertThat(planItemInstances).isNotNull().hasSize(0);

            assertCaseInstanceEnded(reactivatedCase);
        } finally {
            Authentication.setAuthenticatedUserId(previousUserId);
        }
    }

    protected HistoricCaseInstance createAndFinishSimpleCase(boolean completeTaskC) {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("stageReactivationTestCase")
            .start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task A"));

        // if we need task C to be completed we need to activate it before completing task B, otherwise the autocomplete rule of the stage will immediately
        // complete
        if (completeTaskC) {
            planItemInstances = getPlanItemInstances(caseInstance.getId());
            cmmnRuntimeService.startPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task C"));
        }

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertPlanItemInstanceState(planItemInstances, "Task B", ACTIVE);
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task B"));

        // now task C must be still around and we can complete it (if started before)
        if (completeTaskC) {
            planItemInstances = getPlanItemInstances(caseInstance.getId());
            assertPlanItemInstanceState(planItemInstances, "Task C", ACTIVE);
            cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task C"));
        }

        return cmmnHistoryService.createHistoricCaseInstanceQuery().finished().singleResult();
    }
}