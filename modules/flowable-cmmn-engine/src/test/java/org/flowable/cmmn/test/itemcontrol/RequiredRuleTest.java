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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.api.runtime.UserEventListenerInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.task.api.Task;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class RequiredRuleTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment
    public void testOneRequiredHumanTask() {

        // The required task is made active, the non-required not.
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testOneRequiredHumanTask")
                .variable("required", true)
                .start();

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId()).orderByName().asc().list();
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName, PlanItemInstance::getState)
                .containsExactly(
                        tuple("Non-required task", PlanItemInstanceState.AVAILABLE),
                        tuple("Required task", PlanItemInstanceState.ACTIVE)
                );

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Required task");

        // Completing the task should autocomplete the plan model, as the plan model is autoComplete enabled
        cmmnTaskService.complete(task.getId());
        assertCaseInstanceEnded(caseInstance);

        // Both required and non-required task are made active.
        caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testOneRequiredHumanTask")
                .variable("required", true)
                .variable("nonRequired", true)
                .start();

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId()).orderByName().asc().list();
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName, PlanItemInstance::getState)
                .containsExactly(
                        tuple("Non-required task", PlanItemInstanceState.ACTIVE),
                        tuple("Required task", PlanItemInstanceState.ACTIVE)
                );

        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactly("Non-required task", "Required task");

        // Completing the required task should not autocomplete the plan model
        cmmnTaskService.complete(tasks.get(1).getId());
        assertCaseInstanceNotEnded(caseInstance);

        cmmnTaskService.complete(tasks.get(0).getId());
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testOneRequiredHumanTaskInStage() {

        // The required task is made active, the non-required not.
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testOneRequiredHumanTaskInStage")
                .variable("required", true)
                .start();

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId()).orderByName().asc().list();
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName, PlanItemInstance::getState)
                .containsExactly(
                        tuple("Non-required task", PlanItemInstanceState.AVAILABLE),
                        tuple("Other task", PlanItemInstanceState.ACTIVE),
                        tuple("Required task", PlanItemInstanceState.ACTIVE),
                        tuple("The Stage", PlanItemInstanceState.ACTIVE)
                );

        Task task = cmmnTaskService.createTaskQuery().taskName("Required task").singleResult();
        assertThat(task.getName()).isEqualTo("Required task");

        // Completing the task should autocomplete the stage
        cmmnTaskService.complete(task.getId());
        assertCaseInstanceNotEnded(caseInstance);
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);

        cmmnTaskService.complete(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult().getId());
        assertCaseInstanceEnded(caseInstance);

        // Both required and non-required task are made active.
        caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testOneRequiredHumanTaskInStage")
                .variable("required", true)
                .variable("nonRequired", true)
                .start();

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId()).orderByName().asc().list();
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName, PlanItemInstance::getState)
                .containsExactly(
                        tuple("Non-required task", PlanItemInstanceState.ACTIVE),
                        tuple("Other task", PlanItemInstanceState.ACTIVE),
                        tuple("Required task", PlanItemInstanceState.ACTIVE),
                        tuple("The Stage", PlanItemInstanceState.ACTIVE)
                );

        Task otherTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("Other task").singleResult();
        cmmnTaskService.complete(otherTask.getId());

        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("Non-required task", "Required task");

        cmmnTaskService.complete(tasks.get(1).getId());
        assertCaseInstanceNotEnded(caseInstance);

        cmmnTaskService.complete(tasks.get(0).getId());
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testNonAutoCompleteStageManualCompleteable() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testNonAutoCompleteStageManualCompleteable")
                .variable("required", true)
                .start();

        PlanItemInstance stagePlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.STAGE)
                .singleResult();
        assertThat(stagePlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(stagePlanItemInstance.isCompletable()).isFalse();

        // Completing the one task should mark the stage as completeable 
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Required task");
        cmmnTaskService.complete(task.getId());

        stagePlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceId(stagePlanItemInstance.getId()).singleResult();
        assertThat(stagePlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(stagePlanItemInstance.isCompletable()).isTrue();
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        // Making the other task active, should disable the completeable flag again
        cmmnRuntimeService.setVariables(caseInstance.getId(), CollectionUtil.singletonMap("nonRequired", true));
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);

        stagePlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceId(stagePlanItemInstance.getId()).singleResult();
        assertThat(stagePlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(stagePlanItemInstance.isCompletable()).isFalse();
    }

    @Test
    @CmmnDeployment
    public void testCompleteStageManually() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testNonAutoCompleteStageManualCompleteable")
                .variable("required", true)
                .start();

        final PlanItemInstance stagePlanItemInstance1 = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.STAGE)
                .singleResult();
        assertThat(stagePlanItemInstance1.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(stagePlanItemInstance1.isCompletable()).isFalse();

        assertThatThrownBy(() -> cmmnRuntimeService.completeStagePlanItemInstance(stagePlanItemInstance1.getId()))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Can only complete a stage plan item instance that is marked as completable (there might still be active plan item instance).");

        // Completing the one task should mark the stage as completeable 
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Required task");
        cmmnTaskService.complete(task.getId());

        PlanItemInstance stagePlanItemInstance2 = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceId(stagePlanItemInstance1.getId())
                .singleResult();
        assertThat(stagePlanItemInstance2.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(stagePlanItemInstance2.isCompletable()).isTrue();
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceCompletable().singleResult()).isNotNull();
        cmmnRuntimeService.completeStagePlanItemInstance(stagePlanItemInstance2.getId());
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testCompleteCaseInstanceManually() {
        CaseInstance caseInstance1 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testCompleteCaseInstanceManually")
                .variable("required", true)
                .start();

        assertThat(caseInstance1.isCompletable()).isFalse();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance1.getId()).planItemInstanceStateActive().count()).isEqualTo(2);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance1.getId()).planItemInstanceStateAvailable().count())
                .isEqualTo(1);

        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance1.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("Other task", "Required task");

        // Case should not be completeable
        assertThatThrownBy(() -> cmmnRuntimeService.completeCaseInstance(caseInstance1.getId()))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Can only complete a case instance which is marked as completeable. Check if there are active plan item instances.");

        // Completing both tasks should not auto complete the case, as the plan model is not auto complete
        for (Task task : tasks) {
            cmmnTaskService.complete(task.getId());
        }

        CaseInstance caseInstance2 = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance1.getId()).singleResult();
        assertThat(caseInstance2.isCompletable()).isTrue();
        cmmnRuntimeService.completeCaseInstance(caseInstance2.getId());
        assertCaseInstanceEnded(caseInstance2);
    }

    @Test
    @CmmnDeployment
    public void testComplexCase() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variable("dRequired", false)
                .variable("enableSubStage", true)
                .start();

        Task taskA = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(taskA).isNotNull();
        cmmnTaskService.complete(taskA.getId());

        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("D");

        // D is required. So completing D will auto complete the stage
        cmmnTaskService.complete(tasks.get(0).getId());
        assertThat(cmmnRuntimeService.createMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstance.getId()).count())
                .isEqualTo(2); // M1 is never reached. M2 and M3 are

        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("G");

        // G is the only required task. Completing it should complete the stage and case instance
        cmmnTaskService.complete(tasks.get(0).getId());
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testComplexCase02() {

        // Same as testComplexCase, but now B and E are manually enabled

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variable("dRequired", false)
                .variable("enableSubStage", false)
                .variable("booleanVar", true)
                .variable("subStageRequired", false)
                .start();

        Task taskA = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(taskA).isNotNull();
        cmmnTaskService.complete(taskA.getId());

        PlanItemInstance planItemInstanceB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult();
        assertThat(planItemInstanceB.getState()).isEqualTo(PlanItemInstanceState.ENABLED);
        cmmnRuntimeService.startPlanItemInstance(planItemInstanceB.getId());

        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("B", "D");

        // D is required. But B is still active
        cmmnTaskService.complete(tasks.get(1).getId());
        assertThat(cmmnRuntimeService.createMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstance.getId()).count()).isZero();
        cmmnTaskService.complete(tasks.get(0).getId());
        assertThat(cmmnRuntimeService.createMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstance.getId()).count()).isEqualTo(1);

        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("C");

        // There are no active tasks in the second stage (as the nested stage is not active and not required).
        // Stage should autocomplete immediately after task completion
        cmmnTaskService.complete(tasks.get(0).getId());
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void repetitiveStageWithRequiredItem() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("repetitionWithRequired")
                .start();

        // start human task and complete it
        UserEventListenerInstance userEventListenerInstance = cmmnRuntimeService.createUserEventListenerInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .name("Start from outside")
                .singleResult();
        cmmnRuntimeService.completeUserEventListenerInstance(userEventListenerInstance.getId());

        Task task = cmmnTaskService.createTaskQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(task).isNotNull();
        cmmnTaskService.complete(task.getId()); // runs in an infinitive loop in case required is not stage scoped

        // terminate instance since there is no other way to end it
        cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());
    }

}
