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

import static org.assertj.core.api.Assertions.assertThat;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.ACTIVE;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.AVAILABLE;

import java.util.Arrays;
import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.junit.Test;

/**
 * Tests combinations of repetition with a collection variable and condition (if-part).
 *
 * @author Micha Kiener
 */
public class PlanItemRepetitionWithCollectionVariableAndConditionDynamicNameTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/itemcontrol/PlanItemRepetitionWithCollectionVariableAndConditionDynamicNameTest.multipleTests.cmmn")
    public void testRepetitionOnCollectionTriggeredByUserTaskWithDeferredEvent() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("repetitionWithCollectionVariableTestThree").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(3);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B (na - na)", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task C (na - na)", AVAILABLE);

        List<String> taskOutputList = Arrays.asList("A", "B", "C", "D");

        // complete Task A by providing the collection used for repetition
        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task A", ACTIVE))
                .variable("taskOutputList", taskOutputList)
                .trigger();

        // as we didn't enable Task B yet, no instances must have been created
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(3);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B (na - na)", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task C (na - na)", AVAILABLE);

        // now enable the condition on Task B -> must trigger the repetition on the collection
        cmmnRuntimeService.setVariable(caseInstance.getId(), "enableTaskB", true);

        // now we need to have 4 instances of Task B with adequate local variables
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(7);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B (na - na)", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task B (A - 0)", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B (B - 1)", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B (C - 2)", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B (D - 3)", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task C (na - na)", AVAILABLE);

        // now let's complete all Tasks B -> nothing must happen additionally
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task B (A - 0)", ACTIVE));
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task B (B - 1)", ACTIVE));
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task B (C - 2)", ACTIVE));
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task B (D - 3)", ACTIVE));

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(3);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B (na - na)", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task C (na - na)", AVAILABLE);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/itemcontrol/PlanItemRepetitionWithCollectionVariableAndConditionDynamicNameTest.multipleTests.cmmn")
    public void testRepetitionOnCollectionTriggeredByUserTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("repetitionWithCollectionVariableTestThree").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(3);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B (na - na)", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task C (na - na)", AVAILABLE);

        // enable the condition on Task B upfront -> nothing yet to happen
        cmmnRuntimeService.setVariable(caseInstance.getId(), "enableTaskB", true);
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(3);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B (na - na)", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task C (na - na)", AVAILABLE);

        List<String> taskOutputList = Arrays.asList("A", "B", "C", "D");

        // complete Task A by providing the collection used for repetition
        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task A", ACTIVE))
                .variable("taskOutputList", taskOutputList)
                .trigger();

        // now we need to have 4 instances of Task B with adequate local variables
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(7);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B (na - na)", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task B (A - 0)", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B (B - 1)", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B (C - 2)", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B (D - 3)", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task C (na - na)", AVAILABLE);

        // now let's complete all Tasks B -> nothing must happen additionally
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task B (A - 0)", ACTIVE));
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task B (B - 1)", ACTIVE));
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task B (C - 2)", ACTIVE));
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task B (D - 3)", ACTIVE));

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(3);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B (na - na)", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task C (na - na)", AVAILABLE);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/itemcontrol/PlanItemRepetitionWithCollectionVariableAndConditionDynamicNameTest.multipleTests.cmmn")
    public void testRepetitionOnCollectionTriggeredByUserTaskSeveralTimes() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("repetitionWithCollectionVariableTestThree").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(3);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B (na - na)", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task C (na - na)", AVAILABLE);

        // enable the condition on Task B upfront -> nothing yet to happen
        cmmnRuntimeService.setVariable(caseInstance.getId(), "enableTaskB", true);

        List<String> taskOutputList = Arrays.asList("A", "B", "C", "D");

        // complete Task A by providing the collection used for repetition
        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task A", ACTIVE))
                .variable("taskOutputList", taskOutputList)
                .trigger();

        // now we need to have 4 instances of Task B with adequate local variables
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(7);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B (na - na)", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task B (A - 0)", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B (B - 1)", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B (C - 2)", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B (D - 3)", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task C (na - na)", AVAILABLE);

        // complete all active tasks
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task B (A - 0)", ACTIVE));
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task B (B - 1)", ACTIVE));
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task B (C - 2)", ACTIVE));
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task B (D - 3)", ACTIVE));

        taskOutputList = Arrays.asList("E", "F");

        // complete Task A again by providing a different collection used for repetition
        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task A", ACTIVE))
                .variable("taskOutputList", taskOutputList)
                .trigger();

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(5);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B (na - na)", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task B (E - 0)", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B (F - 1)", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task C (na - na)", AVAILABLE);

        // now let's complete all Tasks B -> nothing must happen additionally
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task B (E - 0)", ACTIVE));
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task B (F - 1)", ACTIVE));

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(3);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B (na - na)", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task C (na - na)", AVAILABLE);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/itemcontrol/PlanItemRepetitionWithCollectionVariableAndConditionDynamicNameTest.multipleTests.cmmn")
    public void testRepetitionOnCollectionTriggeredByUserTaskSeveralTimesWithPartialCompletionInBetween() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("repetitionWithCollectionVariableTestThree").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(3);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B (na - na)", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task C (na - na)", AVAILABLE);

        // enable the condition on Task B upfront -> nothing yet to happen
        cmmnRuntimeService.setVariable(caseInstance.getId(), "enableTaskB", true);

        List<String> taskOutputList = Arrays.asList("A", "B", "C", "D");

        // complete Task A by providing the collection used for repetition
        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task A", ACTIVE))
                .variable("taskOutputList", taskOutputList)
                .trigger();

        // now we need to have 4 instances of Task B with adequate local variables
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(7);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B (na - na)", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task B (A - 0)", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B (B - 1)", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B (C - 2)", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B (D - 3)", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task C (na - na)", AVAILABLE);

        // only complete two active Task B
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task B (A - 0)", ACTIVE));
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task B (B - 1)", ACTIVE));

        taskOutputList = Arrays.asList("E", "F");

        // complete Task A again by providing a different collection used for repetition
        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task A", ACTIVE))
                .variable("taskOutputList", taskOutputList)
                .trigger();

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(7);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B (na - na)", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task B (C - 2)", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B (D - 3)", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B (E - 0)", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B (F - 1)", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task C (na - na)", AVAILABLE);

        // now let's complete all Tasks B -> nothing must happen additionally
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task B (C - 2)", ACTIVE));
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task B (D - 3)", ACTIVE));
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task B (E - 0)", ACTIVE));
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task B (F - 1)", ACTIVE));

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(3);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B (na - na)", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task C (na - na)", AVAILABLE);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/itemcontrol/PlanItemRepetitionWithCollectionVariableAndConditionDynamicNameTest.multipleTests.cmmn")
    public void testRepetitionOnCollectionTriggeredByCollectionVariableSet() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("repetitionWithCollectionVariableTestThree").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(3);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B (na - na)", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task C (na - na)", AVAILABLE);

        // enable task C upfront (nothing must happen yet)
        cmmnRuntimeService.setVariable(caseInstance.getId(), "enableTaskC", true);
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(3);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B (na - na)", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task C (na - na)", AVAILABLE);

        List<String> myCollection = Arrays.asList("A", "B", "C", "D");

        // set the collection variable to kick-off the creation of the Task C repetition
        cmmnRuntimeService.setVariable(caseInstance.getId(), "myCollection", myCollection);

        // now we need to have 4 instances of Task C with adequate local variables
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(6);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B (na - na)", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task C (A - 0)", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task C (B - 1)", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task C (C - 2)", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task C (D - 3)", ACTIVE);

        // if we change the collection variable, nothing else must happen
        cmmnRuntimeService.setVariable(caseInstance.getId(), "myCollection", Arrays.asList("foo"));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(6);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B (na - na)", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task C (A - 0)", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task C (B - 1)", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task C (C - 2)", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task C (D - 3)", ACTIVE);

        // even if we remove the variable completely, nothing else must happen
        cmmnRuntimeService.removeVariable(caseInstance.getId(), "myCollection");
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(6);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B (na - na)", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task C (A - 0)", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task C (B - 1)", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task C (C - 2)", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task C (D - 3)", ACTIVE);

        // now let's complete all Tasks C -> nothing must happen additionally
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task C (A - 0)", ACTIVE));
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task C (B - 1)", ACTIVE));
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task C (C - 2)", ACTIVE));
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task C (D - 3)", ACTIVE));

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(2);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B (na - na)", AVAILABLE);
        assertNoPlanItemInstance(planItemInstances, "Task C");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/itemcontrol/PlanItemRepetitionWithCollectionVariableAndConditionDynamicNameTest.multipleTests.cmmn")
    public void testRepetitionOnCollectionTriggeredBySatisfyingIfPartAfterCollectionSet() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("repetitionWithCollectionVariableTestThree").start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(3);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B (na - na)", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task C (na - na)", AVAILABLE);

        List<String> myCollection = Arrays.asList("A", "B", "C", "D");

        // set the collection variable, but nothing must happen yet as the if-part is not yet satisfied
        cmmnRuntimeService.setVariable(caseInstance.getId(), "myCollection", myCollection);

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(3);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B (na - na)", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task C (na - na)", AVAILABLE);

        // enable task C which needs to kick-off the repetition on collection previously set
        cmmnRuntimeService.setVariable(caseInstance.getId(), "enableTaskC", true);

        // now we need to have 4 instances of Task C with adequate local variables
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(6);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B (na - na)", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task C (A - 0)", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task C (B - 1)", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task C (C - 2)", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task C (D - 3)", ACTIVE);

        // if we change the collection variable, nothing else must happen
        cmmnRuntimeService.setVariable(caseInstance.getId(), "myCollection", Arrays.asList("foo"));
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(6);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B (na - na)", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task C (A - 0)", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task C (B - 1)", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task C (C - 2)", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task C (D - 3)", ACTIVE);

        // even if we remove the variable completely, nothing else must happen
        cmmnRuntimeService.removeVariable(caseInstance.getId(), "myCollection");
        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(6);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B (na - na)", AVAILABLE);
        assertPlanItemInstanceState(planItemInstances, "Task C (A - 0)", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task C (B - 1)", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task C (C - 2)", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task C (D - 3)", ACTIVE);

        // now let's complete all Tasks C -> nothing must happen additionally
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task C (A - 0)", ACTIVE));
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task C (B - 1)", ACTIVE));
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task C (C - 2)", ACTIVE));
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByNameAndState(planItemInstances, "Task C (D - 3)", ACTIVE));

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(2);
        assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task B (na - na)", AVAILABLE);
        assertNoPlanItemInstance(planItemInstances, "Task C (na - na)");
    }
}
