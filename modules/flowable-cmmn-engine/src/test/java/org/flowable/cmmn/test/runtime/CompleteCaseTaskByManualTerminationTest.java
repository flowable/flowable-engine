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

import java.util.List;

import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstanceState;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.common.engine.api.constant.ReferenceTypes;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.junit.Test;

/**
 * Tests that a case task is being completed if its referenced case gets manually termianted through the API, rather than an exit sentry.
 *
 * @author Micha Kiener
 */
public class CompleteCaseTaskByManualTerminationTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/event/CaseInstanceEventsTest.testSimpleSubCase.cmmn",
            "org/flowable/cmmn/test/one-human-task-model.cmmn"
    })
    public void completeCaseTaskOnManualTerminationOfReferencedCaseTest() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("mainCase")
            .businessKey("main key")
            .name("name")
            .transientVariable("childBusinessKey", "child key")
            .start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).extracting(PlanItemInstance::getName).containsExactly("Case Task");
        assertThat(planItemInstances).extracting(PlanItemInstance::getState).containsExactly(ACTIVE);
        assertThat(planItemInstances).extracting(PlanItemInstance::getReferenceType).containsExactly(ReferenceTypes.PLAN_ITEM_CHILD_CASE);
        String childCaseId = planItemInstances.get(0).getReferenceId();

        // manually terminate the case through the API which must also complete the case task and hence complete the root case and end it
        cmmnRuntimeService.terminateCaseInstance(childCaseId);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.INSTANCE, cmmnEngineConfiguration)) {
            HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(childCaseId).singleResult();
            assertThat(historicCaseInstance.getState()).isEqualTo(CaseInstanceState.TERMINATED);
        }


        assertCaseInstanceEnded(childCaseId);
        assertCaseInstanceEnded(caseInstance.getId());
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/event/CaseInstanceEventsTest.testSimpleSubCase.cmmn",
            "org/flowable/cmmn/test/one-human-task-model.cmmn"
    })
    public void completeCaseTaskOnManualTriggerTest() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("mainCase")
            .businessKey("main key")
            .name("name")
            .transientVariable("childBusinessKey", "child key")
            .start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).extracting(PlanItemInstance::getName).containsExactly("Case Task");
        assertThat(planItemInstances).extracting(PlanItemInstance::getState).containsExactly(ACTIVE);
        assertThat(planItemInstances).extracting(PlanItemInstance::getReferenceType).containsExactly(ReferenceTypes.PLAN_ITEM_CHILD_CASE);
        String childCaseId = planItemInstances.get(0).getReferenceId();

        // manually terminate the case through the API which must also complete the case task and hence complete the root case and end it
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Case Task"));

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.INSTANCE, cmmnEngineConfiguration)) {
            HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(childCaseId).singleResult();
            assertThat(historicCaseInstance.getState()).isEqualTo(CaseInstanceState.TERMINATED);
        }

        assertCaseInstanceEnded(childCaseId);
        assertCaseInstanceEnded(caseInstance.getId());
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/event/CaseInstanceEventsTest.testSimpleSubCase.cmmn",
            "org/flowable/cmmn/test/one-human-task-model.cmmn"
    })
    public void completeCaseTaskByCompletingChildCase() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("mainCase")
            .businessKey("main key")
            .name("name")
            .transientVariable("childBusinessKey", "child key")
            .start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).extracting(PlanItemInstance::getName).containsExactly("Case Task");
        assertThat(planItemInstances).extracting(PlanItemInstance::getState).containsExactly(ACTIVE);
        assertThat(planItemInstances).extracting(PlanItemInstance::getReferenceType).containsExactly(ReferenceTypes.PLAN_ITEM_CHILD_CASE);
        String childCaseId = planItemInstances.get(0).getReferenceId();

        planItemInstances = getPlanItemInstances(childCaseId);
        assertThat(planItemInstances).hasSize(1);
        assertPlanItemInstanceState(planItemInstances, "The Task", ACTIVE);
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "The Task"));

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.INSTANCE, cmmnEngineConfiguration)) {
            HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(childCaseId).singleResult();
            assertThat(historicCaseInstance.getState()).isEqualTo(CaseInstanceState.COMPLETED);
        }

        assertCaseInstanceEnded(childCaseId);
        assertCaseInstanceEnded(caseInstance.getId());
    }
}
