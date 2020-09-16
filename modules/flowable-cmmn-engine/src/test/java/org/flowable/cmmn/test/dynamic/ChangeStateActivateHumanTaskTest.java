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
package org.flowable.cmmn.test.dynamic;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.task.api.Task;
import org.junit.Test;

public class ChangeStateActivateHumanTaskTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment
    public void testActivateHumanTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variable("activateFirstTask", false)
                .start();

        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        cmmnRuntimeService.createChangePlanItemStateBuilder()
                .caseInstanceId(caseInstance.getId())
                .activatePlanItemDefinitionId("task1")
                .changeState();

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Task One");

        cmmnTaskService.complete(task.getId());

        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Task Two");

        cmmnTaskService.complete(task.getId());
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/dynamic/ChangeStateActivateHumanTaskTest.testActivateHumanTask.cmmn")
    public void testActivateSecondHumanTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variable("activateFirstTask", true)
                .start();

        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);

        cmmnRuntimeService.createChangePlanItemStateBuilder()
                .caseInstanceId(caseInstance.getId())
                .activatePlanItemDefinitionId("task2")
                .changeState();

        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(2);

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("task1").singleResult();
        assertThat(task.getName()).isEqualTo("Task One");
        cmmnTaskService.complete(task.getId());

        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Task Two");

        cmmnTaskService.complete(task.getId());
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/dynamic/ChangeStateActivateHumanTaskTest.testActivateHumanTask.cmmn")
    public void testActivateSecondHumanTaskWithNoInitialTasks() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variable("activateFirstTask", false)
                .start();

        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        cmmnRuntimeService.createChangePlanItemStateBuilder()
                .caseInstanceId(caseInstance.getId())
                .activatePlanItemDefinitionId("task2")
                .changeState();

        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);

        cmmnRuntimeService.createChangePlanItemStateBuilder()
                .caseInstanceId(caseInstance.getId())
                .activatePlanItemDefinitionId("task1")
                .changeState();

        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(2);

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("task2").singleResult();
        assertThat(task.getName()).isEqualTo("Task Two");
        cmmnTaskService.complete(task.getId());

        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Task One");

        cmmnTaskService.complete(task.getId());
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testActivateHumanTaskAndMoveState() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variable("activateFirstTask", true)
                .start();

        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);

        cmmnRuntimeService.createChangePlanItemStateBuilder()
                .caseInstanceId(caseInstance.getId())
                .terminatePlanItemDefinitionId("task1")
                .activatePlanItemDefinitionId("task2")
                .activatePlanItemDefinitionId("task3")
                .changeState();

        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(2);

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("task2").singleResult();
        assertThat(task.getName()).isEqualTo("Task Two");
        cmmnTaskService.complete(task.getId());

        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Task Three");

        cmmnTaskService.complete(task.getId());
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testActivateHumanTaskInStage() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variable("activateFirstTask", false)
                .start();

        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().onlyStages().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().onlyStages().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);

        cmmnRuntimeService.createChangePlanItemStateBuilder()
                .caseInstanceId(caseInstance.getId())
                .activatePlanItemDefinitionId("task1")
                .changeState();

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Task One");

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().onlyStages().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
        PlanItemInstance dbPlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().onlyStages().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(dbPlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(dbPlanItemInstance.getId()).isEqualTo(planItemInstance.getId());

        cmmnTaskService.complete(task.getId());

        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Task Two");

        cmmnTaskService.complete(task.getId());
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testActivateHumanTaskInStageWithSentry() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variable("activateStage", false)
                .start();

        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().onlyStages().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().onlyStages().caseInstanceId(caseInstance.getId()).singleResult().getState())
                .isEqualTo(PlanItemInstanceState.AVAILABLE);

        cmmnRuntimeService.createChangePlanItemStateBuilder()
                .caseInstanceId(caseInstance.getId())
                .activatePlanItemDefinitionId("task1")
                .changeToAvailableStateByPlanItemDefinitionId("task2")
                .changeState();

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Task One");

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().onlyStages().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
        PlanItemInstance dbPlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().onlyStages().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(dbPlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);

        cmmnTaskService.complete(task.getId());

        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Task Two");

        cmmnTaskService.complete(task.getId());
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/dynamic/ChangeStateActivateHumanTaskTest.testActivateHumanTask.cmmn")
    public void testChangeHumanTaskStateToAvailable() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variable("activateFirstTask", true)
                .start();

        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);

        cmmnRuntimeService.setVariable(caseInstance.getId(), "activateFirstTask", false);

        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);

        cmmnRuntimeService.createChangePlanItemStateBuilder()
                .caseInstanceId(caseInstance.getId())
                .changeToAvailableStateByPlanItemDefinitionId("task1")
                .changeState();

        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        PlanItemInstance dbPlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemDefinitionId("task1")
                .singleResult();

        assertThat(dbPlanItemInstance).isNotNull();
        assertThat(dbPlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);

        cmmnRuntimeService.setVariable(caseInstance.getId(), "activateFirstTask", true);
        cmmnRuntimeService.evaluateCriteria(caseInstance.getId());

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Task One");

        cmmnTaskService.complete(task.getId());

        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Task Two");

        cmmnTaskService.complete(task.getId());
        assertCaseInstanceEnded(caseInstance);
    }

}
