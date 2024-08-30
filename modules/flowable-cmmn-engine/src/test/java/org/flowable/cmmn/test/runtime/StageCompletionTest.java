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

import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.task.api.Task;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class StageCompletionTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment
    public void testSetVariableInLifecycleListener() {

        /*
         * This test has a case which has a stage with a lifecycle listener that sets a variable through the cmmnRuntimeService.
         * Before introducing the flag 'isStateChangeUnprocessed' on PlanItemInstanceEntity, this would lead to the following problem:
         *
         *    startCaseInstance --> new CommandContext --> plan operations for initialization + moving stage plan item instance to state 'active'
         *   setVariable, through a service, will reuse the existing commandContext. However, the SetVariableCmd plans an explicit evaluation operation (for good reasons)
         *
         * When the evaluation operations executes, the stage has moved into the 'active' state, however when the lifecycle listener executes
         * no child plan item instances are created yet. The logic deems correctly that this constellation means the stage should complete.
         *
         * Introducing the stateChangeUnprocessed flag on the stage plan item instance fixes this problem: it avoids looking into stages that
         * are still being initialized but have a setup that would otherwise automatically complete the stage plan item instance.
         */

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertPlanItemInstanceState(caseInstance, "stageWithLifecycleListener", PlanItemInstanceState.AVAILABLE);

        // Triggering the user event listener activates the stage
        cmmnRuntimeService.completeUserEventListenerInstance(planItemInstances.stream()
                .filter(planItemInstance -> "A".equalsIgnoreCase(planItemInstance.getName())).findAny().get().getId());

        assertPlanItemInstanceState(caseInstance, "stageWithLifecycleListener", PlanItemInstanceState.ACTIVE);

        // Completing the user tasks should complete the case instance
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());

        assertCaseInstanceEnded(caseInstance);
    }

}
