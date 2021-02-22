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
 * Testing entry and exit sentries, both with conditions as a combination on a stage.
 *
 * @author Micha Kiener
 */
public class ExitSentryConditionWithSentryCombinationTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment
    public void testSentryCombination() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testSentry-test").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(4);
        assertPlanItemInstanceState(planItemInstances, "stage 1", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "start", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "fireMe1", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Milestone", AVAILABLE);

        cmmnRuntimeService.completeUserEventListenerInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "start", AVAILABLE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(5);
        assertPlanItemInstanceState(planItemInstances, "stage 1", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "fireMe1", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Complete Stage 1", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Milestone", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Milestone 1", AVAILABLE);

        cmmnRuntimeService.completeUserEventListenerInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "fireMe1", AVAILABLE));
        planItemInstances = getPlanItemInstances(caseInstance.getId());

        // the Milestone becomes orphaned as it can never be activated anymore as stage 1 is terminated
        assertThat(planItemInstances).hasSize(1);
        assertPlanItemInstanceState(planItemInstances, "Milestone", AVAILABLE);
    }
}
