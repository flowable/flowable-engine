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
package org.flowable.cmmn.test.itemcontrol;

import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.ACTIVE;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.AVAILABLE;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.junit.Test;

/**
 * Tests combinations of repetition with a collection variable.
 *
 * @author Micha Kiener
 */
public class PlanItemRepetitionWithCollectionVariableTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/itemcontrol/PlanItemRepetitionWithCollectionVariableTest.multipleTests.cmmn")
    public void testRepetitionOnCollectionTriggeredByUserTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("repetitionWithCollectionVariableTestOne").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertEquals(3, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task C", AVAILABLE);

        List<String> taskOutputList = Arrays.asList("A", "B", "C", "D");

        // complete Task A by providing the collection used for repetition
        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task A", ACTIVE))
            .variable("taskOutputList", taskOutputList)
            .trigger();

        // now we need to have 4 instances of Task B with adequate local variables
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertEquals(7, planItemInstances.size());
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B", ACTIVE, ACTIVE, ACTIVE, ACTIVE, AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task C", AVAILABLE);
    }

}
