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

import java.util.List;

import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.api.listener.PlanItemInstanceLifecycleListener;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.model.Milestone;
import org.flowable.task.api.Task;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class MilestoneTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment
    public void testMilestoneVariable() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testMilestoneVariable").start();
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).extracting(Task::getName).containsExactly("A");

        // Completing A will reach milestone M1, which sets a variable that activates the second stage
        cmmnTaskService.complete(tasks.get(0).getId());
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).extracting(Task::getName).containsExactly("B");

        // Completing B will reach milestone M2, which sets a variable that activates task C
        cmmnTaskService.complete(tasks.get(0).getId());
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).extracting(Task::getName).containsExactly("C");

        cmmnTaskService.complete(tasks.get(0).getId());
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testRepeatingMilestoneWithLocalVariables() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testRepeatingMilestone").start();

        Assert.assertEquals(0, cmmnRuntimeService.createMilestoneInstanceQuery().count());

        for (int i = 1; i <= 10; i++) {
            cmmnTaskService.complete(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult().getId());
            assertMilestoneState(caseInstance.getId(), i);
        }
    }

    public void assertMilestoneState(String caseInstanceId, int nrOfExpectedCompletedMilestones) {
        Assert.assertEquals(nrOfExpectedCompletedMilestones, cmmnRuntimeService.createMilestoneInstanceQuery().count());

        List<PlanItemInstance> milestonePlanItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .planItemInstanceName("Milestone")
            .planItemDefinitionType(PlanItemDefinitionType.MILESTONE)
            .includeEnded()
            .list();
        assertThat(milestonePlanItemInstances).hasSize(nrOfExpectedCompletedMilestones + 1);

        String[] expectedStates = new String[nrOfExpectedCompletedMilestones + 1];
        for (int i = 0; i < nrOfExpectedCompletedMilestones; i++) {
            expectedStates[i] = PlanItemInstanceState.COMPLETED;
        }
        expectedStates[nrOfExpectedCompletedMilestones] = PlanItemInstanceState.WAITING_FOR_REPETITION;

        assertThat(milestonePlanItemInstances).extracting(PlanItemInstance::getState).contains(expectedStates);

        for (PlanItemInstance milestonePlanItemInstance : milestonePlanItemInstances) {
            assertThat(cmmnRuntimeService.getLocalVariables(milestonePlanItemInstance.getId())).containsKeys("displayOrder", "redoTask");
        }

        // Local variables shouldn't be returned when getting the case instance variables
        assertThat(cmmnRuntimeService.getVariables(caseInstanceId))
            .hasSize(1)
            .containsKey("milestone1Reached"); // the milestoneVariable
    }

    public static class TestPlanItemLifecycleListener implements PlanItemInstanceLifecycleListener {

        @Override
        public String getSourceState() {
            return null;
        }

        @Override
        public String getTargetState() {
            return null;
        }

        @Override
        public void stateChanged(final DelegatePlanItemInstance planItemInstance, final String oldState, final String newState) {
            if (planItemInstance.getPlanItemDefinitionType().equals(PlanItemDefinitionType.MILESTONE)) {

                final Milestone milestone = (Milestone) planItemInstance.getPlanItemDefinition();

                planItemInstance.setVariableLocal("displayOrder", milestone.getDisplayOrder());
                planItemInstance.setVariableLocal("redoTask", "redoValue");
            }
        }

    }


}
