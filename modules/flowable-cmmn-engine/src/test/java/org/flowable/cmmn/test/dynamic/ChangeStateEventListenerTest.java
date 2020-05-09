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

public class ChangeStateEventListenerTest extends FlowableCmmnTestCase {

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
                .containsExactlyInAnyOrder(PlanItemInstanceState.TERMINATED, PlanItemInstanceState.ACTIVE, PlanItemInstanceState.TERMINATED);

        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Task Two");

        cmmnTaskService.complete(task.getId());
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testChangeHumanTaskAndListener() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Task One");

        PlanItemInstance singlePlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                .planItemDefinitionId("eventListener").singleResult();
        assertThat(singlePlanItemInstance).isNotNull();
        assertThat(singlePlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);

        cmmnRuntimeService.completeUserEventListenerInstance(singlePlanItemInstance.getId());

        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).hasSize(2);

        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("task1").list();
        assertThat(tasks).hasSize(1);

        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("task2").list();
        assertThat(tasks).hasSize(1);

        cmmnRuntimeService.createChangePlanItemStateBuilder()
                .caseInstanceId(caseInstance.getId())
                .changeToAvailableStateByPlanItemDefinitionId("task2")
                .changeToAvailableStateByPlanItemDefinitionId("eventListener")
                .changeState();

        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).hasSize(1);

        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("task1").list();
        assertThat(tasks).hasSize(1);

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).includeEnded().list();
        assertThat(planItemInstances).hasSize(4);

        int planItem1Found = 0;
        boolean planItem2AvailableFound = false;
        boolean planItem3CompletedFound = false;
        boolean planItem3AvailableFound = false;
        for (PlanItemInstance planItemInstance : planItemInstances) {
            if ("planItem1".equals(planItemInstance.getElementId())) {
                planItem1Found++;
                assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);

            } else if ("planItem2".equals(planItemInstance.getElementId())) {
                assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
                planItem2AvailableFound = true;

            } else if ("planItem3".equals(planItemInstance.getElementId())) {
                if (PlanItemInstanceState.COMPLETED.equals(planItemInstance.getState())) {
                    planItem3CompletedFound = true;
                } else {
                    assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
                    planItem3AvailableFound = true;
                }
            }
        }

        assertThat(planItem1Found).isEqualTo(1);
        assertThat(planItem2AvailableFound).isTrue();
        assertThat(planItem3CompletedFound).isTrue();
        assertThat(planItem3AvailableFound).isTrue();

        // complete task 1 instances
        cmmnTaskService.complete(tasks.get(0).getId());

        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).hasSize(1);

        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("task2").list();
        assertThat(tasks).hasSize(1);

        // complete task 2 instance
        cmmnTaskService.complete(tasks.get(0).getId());

        assertCaseInstanceEnded(caseInstance);
    }

}
