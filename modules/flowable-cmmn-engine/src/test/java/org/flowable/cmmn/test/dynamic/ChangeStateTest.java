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

import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.task.api.Task;
import org.junit.Test;

public class ChangeStateTest extends FlowableCmmnTestCase {

    protected String oneTaskCaseDeploymentId;

    @Test
    @CmmnDeployment
    public void testChangeHumanTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Task One");

        cmmnRuntimeService.createChangePlanItemStateBuilder()
                .caseInstanceId(caseInstance.getId())
                .terminatePlanItemDefinitionId("task1")
                .activatePlanItemDefinitionId("task2")
                .changeState();

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).includeEnded().list();
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getState)
                .containsExactlyInAnyOrder(PlanItemInstanceState.TERMINATED, PlanItemInstanceState.ACTIVE);

        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Task Two");

        cmmnTaskService.complete(task.getId());
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testChangeHumanTaskInStage() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Task One");

        cmmnRuntimeService.createChangePlanItemStateBuilder()
                .caseInstanceId(caseInstance.getId())
                .terminatePlanItemDefinitionId("task1")
                .activatePlanItemDefinitionId("task2")
                .changeState();

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).includeEnded().list();
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getState)
                .containsExactlyInAnyOrder(PlanItemInstanceState.TERMINATED, PlanItemInstanceState.ACTIVE, PlanItemInstanceState.ACTIVE);

        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Task Two");

        cmmnTaskService.complete(task.getId());
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testChangeHumanTaskToStage() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Task One");

        cmmnRuntimeService.createChangePlanItemStateBuilder()
                .caseInstanceId(caseInstance.getId())
                .terminatePlanItemDefinitionId("task1")
                .activatePlanItemDefinitionId("subTask1")
                .changeState();

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).includeEnded().list();
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getState)
                .containsExactlyInAnyOrder(PlanItemInstanceState.TERMINATED, PlanItemInstanceState.ACTIVE, PlanItemInstanceState.ACTIVE);

        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Sub task One");

        cmmnTaskService.complete(task.getId());
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testChangeHumanTaskFromStage() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Sub task One");

        cmmnRuntimeService.createChangePlanItemStateBuilder()
                .caseInstanceId(caseInstance.getId())
                .terminatePlanItemDefinitionId("subTask1")
                .activatePlanItemDefinitionId("task1")
                .changeState();

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).includeEnded().list();
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getState)
                .containsExactlyInAnyOrder(
                        PlanItemInstanceState.COMPLETED,
                        PlanItemInstanceState.ACTIVE,
                        PlanItemInstanceState.TERMINATED,
                        PlanItemInstanceState.COMPLETED,
                        PlanItemInstanceState.AVAILABLE);

        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Task One");

        cmmnTaskService.complete(task.getId());

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).includeEnded().list();
        assertThat(planItemInstances).hasSize(6);

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances).hasSize(2);

        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Sub task One");

        cmmnTaskService.complete(task.getId());

        assertCaseInstanceEnded(caseInstance);
    }
}
