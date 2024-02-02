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

import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

/**
 * @author Joram Barrez
 */
public class CompletionNeutralTest extends FlowableCmmnTestCase {

    @Rule
    public TestName name = new TestName();

    @Test
    @CmmnDeployment
    public void testSimpleStageCompletion() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey(name.getMethodName()).start();
        assertThat(caseInstance).isNotNull();

        //Check case setup
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isEqualTo(4);

        PlanItemInstance taskA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskA").singleResult();
        assertThat(taskA).isNotNull();
        assertThat(taskA.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);

        PlanItemInstance stageOne = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("stageOne").singleResult();
        assertThat(stageOne).isNotNull();
        assertThat(stageOne.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);

        PlanItemInstance taskB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskB").singleResult();
        assertThat(taskB).isNotNull();
        assertThat(taskB.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);

        PlanItemInstance taskC = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskC").singleResult();
        assertThat(taskC).isNotNull();
        assertThat(taskC.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);

        //Trigger the test
        assertCaseInstanceNotEnded(caseInstance);
        cmmnRuntimeService.triggerPlanItemInstance(taskC.getId());

        taskA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskA").singleResult();
        assertThat(taskA).isNotNull();
        stageOne = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("stageOne").singleResult();
        assertThat(stageOne).isNull();
        taskB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskB").singleResult();
        assertThat(taskB).isNull();
        taskC = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskC").singleResult();
        assertThat(taskC).isNull();

        assertCaseInstanceNotEnded(caseInstance);

        cmmnRuntimeService.triggerPlanItemInstance(taskA.getId());
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testStagedEventListenerBypassed() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey(name.getMethodName()).start();
        assertThat(caseInstance).isNotNull();

        //Check case setup
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isEqualTo(4);

        PlanItemInstance taskA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskA").singleResult();
        assertThat(taskA).isNotNull();
        assertThat(taskA.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);

        PlanItemInstance stageOne = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("stageOne").singleResult();
        assertThat(stageOne).isNotNull();
        assertThat(stageOne.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);

        PlanItemInstance taskB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskB").singleResult();
        assertThat(taskB).isNotNull();
        assertThat(taskB.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);

        PlanItemInstance listener = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER)
                .singleResult();
        assertThat(listener).isNotNull();
        assertThat(listener.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);

        //Trigger the test
        cmmnRuntimeService.triggerPlanItemInstance(taskB.getId());
        assertCaseInstanceNotEnded(caseInstance);

        taskA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskA").singleResult();
        assertThat(taskA).isNotNull();
        stageOne = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("stageOne").singleResult();
        assertThat(stageOne).isNull();
        taskB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskB").singleResult();
        assertThat(taskB).isNull();
        listener = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER).singleResult();
        assertThat(listener).isNull();

        //End the case
        cmmnRuntimeService.triggerPlanItemInstance(taskA.getId());
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testEventListenerBypassed() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey(name.getMethodName()).start();
        assertThat(caseInstance).isNotNull();

        //Check case setup
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isEqualTo(3);

        PlanItemInstance taskA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskA").singleResult();
        assertThat(taskA).isNotNull();
        assertThat(taskA.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);

        PlanItemInstance taskB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskB").singleResult();
        assertThat(taskB).isNotNull();
        assertThat(taskB.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);

        PlanItemInstance listener = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER)
                .singleResult();
        assertThat(listener).isNotNull();
        assertThat(listener.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);

        //Trigger the test
        cmmnRuntimeService.triggerPlanItemInstance(taskB.getId());
        assertCaseInstanceNotEnded(caseInstance);

        taskA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskA").singleResult();
        assertThat(taskA).isNotNull();
        taskB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskB").singleResult();
        assertThat(taskB).isNull();
        listener = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER).singleResult();
        assertThat(listener).isNull();

        //End the case
        cmmnRuntimeService.triggerPlanItemInstance(taskA.getId());
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testEmbeddedStage() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey(name.getMethodName()).start();
        assertThat(caseInstance).isNotNull();

        //Check case setup
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isEqualTo(6);

        PlanItemInstance taskA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskA").singleResult();
        assertThat(taskA).isNotNull();
        assertThat(taskA.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);

        PlanItemInstance stageOne = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("stageOne").singleResult();
        assertThat(stageOne).isNotNull();
        assertThat(stageOne.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);

        PlanItemInstance taskB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskB").singleResult();
        assertThat(taskB).isNotNull();
        assertThat(taskB.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);

        PlanItemInstance stageTwo = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("completionNeutralStage").singleResult();
        assertThat(stageTwo).isNotNull();
        assertThat(stageTwo.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);

        PlanItemInstance taskC = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskC").singleResult();
        assertThat(taskC).isNotNull();
        assertThat(taskC.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);

        PlanItemInstance taskD = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskD").singleResult();
        assertThat(taskD).isNotNull();
        assertThat(taskD.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);

        //Trigger the test
        cmmnRuntimeService.triggerPlanItemInstance(taskD.getId());
        assertCaseInstanceNotEnded(caseInstance);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isEqualTo(1);
        taskA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskA").singleResult();
        assertThat(taskA).isNotNull();
        cmmnRuntimeService.triggerPlanItemInstance(taskA.getId());
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testRequiredPrecedence() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey(name.getMethodName()).start();
        assertThat(caseInstance).isNotNull();

        //Check case setup
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isEqualTo(4);

        PlanItemInstance taskA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskA").singleResult();
        assertThat(taskA).isNotNull();
        assertThat(taskA.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);

        PlanItemInstance stageOne = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("stageOne").singleResult();
        assertThat(stageOne).isNotNull();
        assertThat(stageOne.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);

        PlanItemInstance taskB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskB").singleResult();
        assertThat(taskB).isNotNull();
        assertThat(taskB.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);

        PlanItemInstance taskC = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskC").singleResult();
        assertThat(taskC).isNotNull();
        assertThat(taskC.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);

        //Trigger the test
        cmmnRuntimeService.triggerPlanItemInstance(taskC.getId());
        assertCaseInstanceNotEnded(caseInstance);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isEqualTo(3);

        taskA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskA").singleResult();
        assertThat(taskA).isNotNull();
        stageOne = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("stageOne").singleResult();
        assertThat(stageOne).isNotNull();
        taskB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskB").singleResult();
        assertThat(taskB).isNotNull();
        taskC = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskC").singleResult();
        assertThat(taskC).isNull();

        cmmnRuntimeService.triggerPlanItemInstance(taskA.getId());
        assertCaseInstanceNotEnded(caseInstance);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isEqualTo(2);
        cmmnRuntimeService.triggerPlanItemInstance(taskB.getId());
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testRequiredPrecedenceDeepNest() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey(name.getMethodName()).start();
        assertThat(caseInstance).isNotNull();

        List<PlanItemInstance> list = cmmnRuntimeService.createPlanItemInstanceQuery().list();

        //Check case setup
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isEqualTo(5);

        PlanItemInstance taskA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskA").singleResult();
        assertThat(taskA).isNotNull();
        assertThat(taskA.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);

        PlanItemInstance stageOne = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("stageOne").singleResult();
        assertThat(stageOne).isNotNull();
        assertThat(stageOne.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);

        List<PlanItemInstance> listeners = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER)
                .list();
        assertThat(listeners).hasSize(3);
        listeners.forEach(l -> assertThat(l.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE));

        //Trigger the test
        //Triggering Listener One will Activate StageOne which will complete as nothing ties it
        PlanItemInstance userEventOne = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER)
                .planItemDefinitionId("userEventOne").singleResult();
        cmmnRuntimeService.triggerPlanItemInstance(userEventOne.getId());
        stageOne = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("stageOne").singleResult();
        assertThat(stageOne).isNull();

        // The listeners should all be removed
        listeners = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER).list();
        assertThat(listeners).isEmpty();
        assertCaseInstanceNotEnded(caseInstance);

        //The only thing keeping the case from ending is TaskA even with a deep nested required task, because its not AVAILABLE yet
        cmmnRuntimeService.triggerPlanItemInstance(taskA.getId());
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testRequiredPrecedenceDeepNest2() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey(name.getMethodName()).start();
        assertThat(caseInstance).isNotNull();

        //Check case setup
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isEqualTo(5);

        PlanItemInstance taskA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskA").singleResult();
        assertThat(taskA).isNotNull();
        assertThat(taskA.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);

        PlanItemInstance stageOne = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("stageOne").singleResult();
        assertThat(stageOne).isNotNull();
        assertThat(stageOne.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);

        List<PlanItemInstance> listeners = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER)
                .list();
        assertThat(listeners).hasSize(3);
        listeners.forEach(l -> assertThat(l.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE));

        //Trigger the test
        //This time a task inside StageOne is required, thus it will not complete once activated
        PlanItemInstance userEvent = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER)
                .planItemDefinitionId("userEventOne").singleResult();
        cmmnRuntimeService.triggerPlanItemInstance(userEvent.getId());
        assertCaseInstanceNotEnded(caseInstance);

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isEqualTo(6);
        listeners = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER).list();
        assertThat(listeners).hasSize(2);
        taskA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskA").singleResult();
        assertThat(taskA).isNotNull();
        assertThat(taskA.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        stageOne = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("stageOne").singleResult();
        assertThat(stageOne).isNotNull();
        assertThat(stageOne.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        PlanItemInstance stageTwo = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("stageTwo").singleResult();
        assertThat(stageTwo).isNotNull();
        assertThat(stageTwo.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
        PlanItemInstance taskB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskB").singleResult();
        assertThat(taskB).isNotNull();
        assertThat(taskB.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);

        //Completing taskB and then taskA should end the case
        //Order is important since required taskC nested in StageTwo is not yet available
        //And completing TaskA first will make taskC available
        //But first TaskB needs to become Active
        userEvent = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER)
                .planItemDefinitionId("userEventTwo").singleResult();
        cmmnRuntimeService.triggerPlanItemInstance(userEvent.getId());
        cmmnRuntimeService.triggerPlanItemInstance(taskB.getId());
        cmmnRuntimeService.triggerPlanItemInstance(taskA.getId());

        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testRequiredPrecedenceDeepNest3() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey(name.getMethodName()).start();
        assertThat(caseInstance).isNotNull();

        //Check case setup
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isEqualTo(5);

        PlanItemInstance taskA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskA").singleResult();
        assertThat(taskA).isNotNull();
        assertThat(taskA.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);

        PlanItemInstance stageOne = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("stageOne").singleResult();
        assertThat(stageOne).isNotNull();
        assertThat(stageOne.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);

        List<PlanItemInstance> listeners = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER)
                .list();
        assertThat(listeners).hasSize(3);
        listeners.forEach(l -> assertThat(l.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE));

        //Trigger the test
        //This time a task inside StageOne is required, thus it will not complete once activated
        PlanItemInstance userEvent = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER)
                .planItemDefinitionId("userEventOne").singleResult();
        cmmnRuntimeService.triggerPlanItemInstance(userEvent.getId());
        assertCaseInstanceNotEnded(caseInstance);

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isEqualTo(6);
        listeners = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER).list();
        assertThat(listeners).hasSize(2);
        taskA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskA").singleResult();
        assertThat(taskA).isNotNull();
        assertThat(taskA.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        stageOne = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("stageOne").singleResult();
        assertThat(stageOne).isNotNull();
        assertThat(stageOne.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        PlanItemInstance stageTwo = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("stageTwo").singleResult();
        assertThat(stageTwo).isNotNull();
        assertThat(stageTwo.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
        PlanItemInstance taskB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskB").singleResult();
        assertThat(taskB).isNotNull();
        assertThat(taskB.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);

        //This time we complete taskA first, making stageTwo Active,
        //making available the required taskC
        cmmnRuntimeService.triggerPlanItemInstance(taskA.getId());
        userEvent = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER)
                .planItemDefinitionId("userEventTwo").singleResult();
        cmmnRuntimeService.triggerPlanItemInstance(userEvent.getId());
        cmmnRuntimeService.triggerPlanItemInstance(taskB.getId());
        assertCaseInstanceNotEnded(caseInstance);

        List<PlanItemInstance> list = cmmnRuntimeService.createPlanItemInstanceQuery().list();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isEqualTo(4);
        listeners = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER).list();
        assertThat(listeners).hasSize(1);
        stageOne = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("stageOne").singleResult();
        assertThat(stageOne).isNotNull();
        assertThat(stageOne.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        stageTwo = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("stageTwo").singleResult();
        assertThat(stageTwo).isNotNull();
        assertThat(stageTwo.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        PlanItemInstance taskC = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskC").singleResult();
        assertThat(taskC).isNotNull();
        assertThat(taskC.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);

        //Now we need to activate TaskC and complete it to end the case
        userEvent = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER)
                .planItemDefinitionId("userEventThree").singleResult();
        cmmnRuntimeService.triggerPlanItemInstance(userEvent.getId());
        cmmnRuntimeService.triggerPlanItemInstance(taskC.getId());
        assertCaseInstanceEnded(caseInstance);
    }
}
