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
package org.flowable.cmmn.test.listener;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.UserEventListenerInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.task.api.Task;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class CreateConditionTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment
    public void testCreateConditionInPlanModelPlanItemInstance() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCreateConditionInPlanModelPlanItemInstance").start();

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER).list()).hasSize(0);

        // After case instance start human task A should be active, human taskA B should be enabled
        Task taskA = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(taskA.getName()).isEqualTo("A");

        PlanItemInstance planItemInstanceForB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateEnabled().singleResult();
        assertThat(planItemInstanceForB.getName()).isEqualTo("B");

        // Completing the human task A should mark the stage as completable
        cmmnTaskService.complete(taskA.getId());

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateEnabled().singleResult()).isNotNull();

        // The stage being completable, this should create the user event listener
        PlanItemInstance userEventListenerPlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
            .planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER)
            .singleResult();
        assertThat(userEventListenerPlanItemInstance).isNotNull();

        // Completing the user event listener should terminate the case
        UserEventListenerInstance userEventListenerInstance = cmmnRuntimeService.createUserEventListenerInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .singleResult();
        assertThat(userEventListenerInstance.getId()).isEqualTo(userEventListenerPlanItemInstance.getId());
        cmmnRuntimeService.completeUserEventListenerInstance(userEventListenerInstance.getId());
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testCreateConditionWithEventListenerInStage() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCreateCondition").start();

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER).list()).hasSize(0);

        // After case instance start human task A should be active, human taskA B should be enabled
        Task taskA = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(taskA.getName()).isEqualTo("A");

        PlanItemInstance planItemInstanceForB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateEnabled().singleResult();
        assertThat(planItemInstanceForB.getName()).isEqualTo("B");

        // Completing the human task A should mark the stage as completable
        cmmnTaskService.complete(taskA.getId());
        PlanItemInstance stage1PlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("Stage 1").singleResult();
        assertThat(stage1PlanItemInstance.isCompleteable()).isTrue();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().stageInstanceId(stage1PlanItemInstance.getId()).list()).isNotEmpty();

        // The stage being completable, this should create the user event listener
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER).list()).hasSize(1);

        // Completing the user event listener should exit stage 1 and activate Stage 2
        UserEventListenerInstance userEventListenerInstance = cmmnRuntimeService.createUserEventListenerInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .singleResult();
        cmmnRuntimeService.completeUserEventListenerInstance(userEventListenerInstance.getId());

        Task taskC = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(taskC.getName()).isEqualTo("C");
    }

}
