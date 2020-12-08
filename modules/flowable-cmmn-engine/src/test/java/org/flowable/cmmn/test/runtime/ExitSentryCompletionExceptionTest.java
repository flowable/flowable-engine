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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.ACTIVE;

import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.junit.Test;

public class ExitSentryCompletionExceptionTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/ExitSentryCompletionExceptionTestCase.cmmn")
    public void testStageExitSentryCompletionFailureMessage() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("exitSentryCompletionExceptionTestCase")
                .start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(4);
        assertPlanItemInstanceState(planItemInstances, "Task B", ACTIVE);

        // Triggering Task B must fail as the stage is not yet completable.
        assertThatThrownBy(() -> cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task B")))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessageContaining("The plan item 'Task A (humanTask1)' prevented it from completion.");

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task A"));
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task B"));
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task C"));

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(1);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/ExitSentryCompletionExceptionTestCase.cmmn")
    public void testCaseExitSentryCompletionFailureMessage() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("exitSentryCompletionExceptionTestCase")
                .start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(4);
        assertPlanItemInstanceState(planItemInstances, "Task C", ACTIVE);

        // Triggering Task C must fail as the stage is not yet completable.
        assertThatThrownBy(() -> cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task C")))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessageContaining("The plan item 'Stage (expandedStage1)' prevented it from completion.");

        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task A"));
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task B"));
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task C"));

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(1);
        }
    }
}
