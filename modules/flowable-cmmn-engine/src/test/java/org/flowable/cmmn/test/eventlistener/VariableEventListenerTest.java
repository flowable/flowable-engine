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
package org.flowable.cmmn.test.eventlistener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.task.api.Task;
import org.junit.Test;

public class VariableEventListenerTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment
    public void testTriggerVariableEventListener() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("variableListener").start();

        // 3 plan items reachable
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isEqualTo(3);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().list())
                .extracting(PlanItemInstance::getPlanItemDefinitionType, PlanItemInstance::getPlanItemDefinitionId, PlanItemInstance::getState)
                .containsExactlyInAnyOrder(
                        tuple(PlanItemDefinitionType.VARIABLE_EVENT_LISTENER, "variableEventListener", PlanItemInstanceState.AVAILABLE),
                        tuple(PlanItemDefinitionType.HUMAN_TASK, "taskA", PlanItemInstanceState.ACTIVE),
                        tuple(PlanItemDefinitionType.HUMAN_TASK, "taskB", PlanItemInstanceState.AVAILABLE)
                );

        // 1 variable Event Listener
        PlanItemInstance listenerInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.VARIABLE_EVENT_LISTENER)
                .singleResult();
        assertThat(listenerInstance).isNotNull();
        assertThat(listenerInstance.getPlanItemDefinitionId()).isEqualTo("variableEventListener");
        assertThat(listenerInstance.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().list())
                    .extracting(HistoricPlanItemInstance::getPlanItemDefinitionType, HistoricPlanItemInstance::getPlanItemDefinitionId, HistoricPlanItemInstance::getState)
                    .containsExactlyInAnyOrder(
                            tuple(PlanItemDefinitionType.VARIABLE_EVENT_LISTENER, "variableEventListener", PlanItemInstanceState.AVAILABLE),
                            tuple(PlanItemDefinitionType.HUMAN_TASK, "taskA", PlanItemInstanceState.ACTIVE),
                            tuple(PlanItemDefinitionType.HUMAN_TASK, "taskB", PlanItemInstanceState.AVAILABLE)
                    );
        }

        // create different variable
        cmmnRuntimeService.setVariable(caseInstance.getId(), "var2", "test");
        
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.VARIABLE_EVENT_LISTENER).count()).isEqualTo(1);

        // create var1 variable to trigger variable event listener
        cmmnRuntimeService.setVariable(caseInstance.getId(), "var1", "test");
        
        // variable event listener should be completed
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.VARIABLE_EVENT_LISTENER).count()).isZero();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().list())
                .extracting(PlanItemInstance::getPlanItemDefinitionType, PlanItemInstance::getPlanItemDefinitionId, PlanItemInstance::getState)
                .containsExactlyInAnyOrder(
                        tuple(PlanItemDefinitionType.HUMAN_TASK, "taskA", PlanItemInstanceState.ACTIVE),
                        tuple(PlanItemDefinitionType.HUMAN_TASK, "taskB", PlanItemInstanceState.ACTIVE)
                );

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskB").planItemInstanceStateActive().count()).isEqualTo(1);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().list())
                    .extracting(HistoricPlanItemInstance::getPlanItemDefinitionType, HistoricPlanItemInstance::getPlanItemDefinitionId, HistoricPlanItemInstance::getState)
                    .containsExactlyInAnyOrder(
                            tuple(PlanItemDefinitionType.VARIABLE_EVENT_LISTENER, "variableEventListener", PlanItemInstanceState.COMPLETED),
                            tuple(PlanItemDefinitionType.HUMAN_TASK, "taskA", PlanItemInstanceState.ACTIVE),
                            tuple(PlanItemDefinitionType.HUMAN_TASK, "taskB", PlanItemInstanceState.ACTIVE)
                    );
        }


        assertCaseInstanceNotEnded(caseInstance);
        cmmnTaskService.createTaskQuery().list().forEach(t -> cmmnTaskService.complete(t.getId()));
        assertCaseInstanceEnded(caseInstance);
    }
    
    @Test
    @CmmnDeployment
    public void testTriggerVariableEventListenerOnUpdate() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("variableListener").start();

        // create variable (should not trigger the variable listener
        cmmnRuntimeService.setVariable(caseInstance.getId(), "var1", "test");
        
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.VARIABLE_EVENT_LISTENER).count()).isEqualTo(1);

        // update var1 variable to trigger variable event listener
        cmmnRuntimeService.setVariable(caseInstance.getId(), "var1", "updated test");
        
        // variable event listener should be completed
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.VARIABLE_EVENT_LISTENER).count()).isZero();

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskB").planItemInstanceStateActive().count()).isEqualTo(1);

        assertCaseInstanceNotEnded(caseInstance);
        cmmnTaskService.createTaskQuery().list().forEach(t -> cmmnTaskService.complete(t.getId()));
        assertCaseInstanceEnded(caseInstance);
    }
    
    @Test
    @CmmnDeployment
    public void testTriggerVariableEventListenerTaskExpression() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("variableListener").start();
        
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.VARIABLE_EVENT_LISTENER).count()).isEqualTo(1);

        // complete task A to trigger variable expression
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("A");
        cmmnTaskService.complete(task.getId());
        
        // variable event listener should be completed
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.VARIABLE_EVENT_LISTENER).count()).isZero();

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskB").planItemInstanceStateActive().count()).isEqualTo(1);

        assertCaseInstanceNotEnded(caseInstance);
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("B");
        cmmnTaskService.complete(task.getId());
        assertCaseInstanceEnded(caseInstance);
    }
    
    @Test
    @CmmnDeployment
    public void testTriggerVariableEventListenerInStage() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("variableListener").start();
        
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.VARIABLE_EVENT_LISTENER).count()).isEqualTo(1);
 
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(2);
        
        // complete task A to trigger variable expression
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("taskA").singleResult();
        cmmnTaskService.complete(task.getId());
        
        // variable event listener should be completed
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.VARIABLE_EVENT_LISTENER).count()).isZero();

        assertCaseInstanceEnded(caseInstance);
    }
    
    @Test
    @CmmnDeployment
    public void testTriggerVariableEventListenerInStageOnlyCreate() {
        // start case instance with var1 variable to trigger immediately the variable listener
        
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("variableListener")
                .variable("var1", "test")
                .start();
        
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                .planItemDefinitionType(PlanItemDefinitionType.VARIABLE_EVENT_LISTENER).count()).isZero();
        
        // start case instance with var2, which should not trigger variable listener
        
        caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("variableListener")
                .variable("var2", "test")
                .start();
        
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                .planItemDefinitionType(PlanItemDefinitionType.VARIABLE_EVENT_LISTENER).count()).isEqualTo(1);
 
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(2);
        
        // complete task A, should trigger variable listener
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("taskA").singleResult();
        cmmnTaskService.complete(task.getId());
        
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                .planItemDefinitionType(PlanItemDefinitionType.VARIABLE_EVENT_LISTENER).count()).isZero();
        
        assertCaseInstanceEnded(caseInstance);
    }

}

