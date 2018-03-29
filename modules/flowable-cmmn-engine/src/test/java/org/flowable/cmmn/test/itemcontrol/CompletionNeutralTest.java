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

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
        assertNotNull(caseInstance);

        //Check case setup
        assertEquals(4, cmmnRuntimeService.createPlanItemInstanceQuery().count());

        PlanItemInstance taskA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskA").singleResult();
        assertNotNull(taskA);
        assertEquals(PlanItemInstanceState.ACTIVE, taskA.getState());

        PlanItemInstance stageOne = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("stageOne").singleResult();
        assertNotNull(stageOne);
        assertEquals(PlanItemInstanceState.ACTIVE, stageOne.getState());

        PlanItemInstance taskB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskB").singleResult();
        assertNotNull(taskB);
        assertEquals(PlanItemInstanceState.AVAILABLE, taskB.getState());

        PlanItemInstance taskC = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskC").singleResult();
        assertNotNull(taskC);
        assertEquals(PlanItemInstanceState.ACTIVE, taskC.getState());

        //Trigger the test
        assertCaseInstanceNotEnded(caseInstance);
        cmmnRuntimeService.triggerPlanItemInstance(taskC.getId());

        taskA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskA").singleResult();
        assertNotNull(taskA);
        stageOne = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("stageOne").singleResult();
        assertNull(stageOne);
        taskB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskB").singleResult();
        assertNull(taskB);
        taskC = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskC").singleResult();
        assertNull(taskC);

        assertCaseInstanceNotEnded(caseInstance);

        cmmnRuntimeService.triggerPlanItemInstance(taskA.getId());
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testStagedEventListenerBypassed() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey(name.getMethodName()).start();
        assertNotNull(caseInstance);

        //Check case setup
        assertEquals(4, cmmnRuntimeService.createPlanItemInstanceQuery().count());

        PlanItemInstance taskA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskA").singleResult();
        assertNotNull(taskA);
        assertEquals(PlanItemInstanceState.ACTIVE, taskA.getState());

        PlanItemInstance stageOne = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("stageOne").singleResult();
        assertNotNull(stageOne);
        assertEquals(PlanItemInstanceState.ACTIVE, stageOne.getState());

        PlanItemInstance taskB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskB").singleResult();
        assertNotNull(taskB);
        assertEquals(PlanItemInstanceState.ACTIVE, taskB.getState());

        PlanItemInstance listener = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER).singleResult();
        assertNotNull(listener);
        assertEquals(PlanItemInstanceState.AVAILABLE, listener.getState());

        //Trigger the test
        cmmnRuntimeService.triggerPlanItemInstance(taskB.getId());
        assertCaseInstanceNotEnded(caseInstance);

        taskA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskA").singleResult();
        assertNotNull(taskA);
        stageOne = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("stageOne").singleResult();
        assertNull(stageOne);
        taskB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskB").singleResult();
        assertNull(taskB);
        listener = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER).singleResult();
        assertNull(listener);

        //End the case
        cmmnRuntimeService.triggerPlanItemInstance(taskA.getId());
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testEventListenerBypassed() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey(name.getMethodName()).start();
        assertNotNull(caseInstance);

        //Check case setup
        assertEquals(3, cmmnRuntimeService.createPlanItemInstanceQuery().count());

        PlanItemInstance taskA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskA").singleResult();
        assertNotNull(taskA);
        assertEquals(PlanItemInstanceState.ACTIVE, taskA.getState());

        PlanItemInstance taskB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskB").singleResult();
        assertNotNull(taskB);
        assertEquals(PlanItemInstanceState.ACTIVE, taskB.getState());

        PlanItemInstance listener = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER).singleResult();
        assertNotNull(listener);
        assertEquals(PlanItemInstanceState.AVAILABLE, listener.getState());

        //Trigger the test
        cmmnRuntimeService.triggerPlanItemInstance(taskB.getId());
        assertCaseInstanceNotEnded(caseInstance);

        taskA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskA").singleResult();
        assertNotNull(taskA);
        taskB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskB").singleResult();
        assertNull(taskB);
        listener = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER).singleResult();
        assertNotNull(listener);
        assertEquals(PlanItemInstanceState.AVAILABLE, listener.getState());

        //End the case
        cmmnRuntimeService.triggerPlanItemInstance(taskA.getId());
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testEmbeddedStage() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey(name.getMethodName()).start();
        assertNotNull(caseInstance);

        //Check case setup
        assertEquals(6, cmmnRuntimeService.createPlanItemInstanceQuery().count());

        PlanItemInstance taskA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskA").singleResult();
        assertNotNull(taskA);
        assertEquals(PlanItemInstanceState.ACTIVE, taskA.getState());

        PlanItemInstance stageOne = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("stageOne").singleResult();
        assertNotNull(stageOne);
        assertEquals(PlanItemInstanceState.ACTIVE, stageOne.getState());

        PlanItemInstance taskB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskB").singleResult();
        assertNotNull(taskB);
        assertEquals(PlanItemInstanceState.AVAILABLE, taskB.getState());

        PlanItemInstance stageTwo = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("completionNeutralStage").singleResult();
        assertNotNull(stageTwo);
        assertEquals(PlanItemInstanceState.ACTIVE, stageTwo.getState());

        PlanItemInstance taskC = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskC").singleResult();
        assertNotNull(taskC);
        assertEquals(PlanItemInstanceState.AVAILABLE, taskC.getState());

        PlanItemInstance taskD = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskD").singleResult();
        assertNotNull(taskD);
        assertEquals(PlanItemInstanceState.ACTIVE, taskD.getState());

        //Trigger the test
        cmmnRuntimeService.triggerPlanItemInstance(taskD.getId());
        assertCaseInstanceNotEnded(caseInstance);
        assertEquals(1, cmmnRuntimeService.createPlanItemInstanceQuery().count());
        taskA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskA").singleResult();
        assertNotNull(taskA);
        cmmnRuntimeService.triggerPlanItemInstance(taskA.getId());
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testRequiredPrecedence() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey(name.getMethodName()).start();
        assertNotNull(caseInstance);

        //Check case setup
        assertEquals(4, cmmnRuntimeService.createPlanItemInstanceQuery().count());

        PlanItemInstance taskA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskA").singleResult();
        assertNotNull(taskA);
        assertEquals(PlanItemInstanceState.ACTIVE, taskA.getState());

        PlanItemInstance stageOne = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("stageOne").singleResult();
        assertNotNull(stageOne);
        assertEquals(PlanItemInstanceState.ACTIVE, stageOne.getState());

        PlanItemInstance taskB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskB").singleResult();
        assertNotNull(taskB);
        assertEquals(PlanItemInstanceState.AVAILABLE, taskB.getState());

        PlanItemInstance taskC = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskC").singleResult();
        assertNotNull(taskC);
        assertEquals(PlanItemInstanceState.ACTIVE, taskC.getState());

        //Trigger the test
        cmmnRuntimeService.triggerPlanItemInstance(taskC.getId());
        assertCaseInstanceNotEnded(caseInstance);
        assertEquals(3, cmmnRuntimeService.createPlanItemInstanceQuery().count());

        taskA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskA").singleResult();
        assertNotNull(taskA);
        stageOne = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("stageOne").singleResult();
        assertNotNull(stageOne);
        taskB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskB").singleResult();
        assertNotNull(taskB);
        taskC = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskC").singleResult();
        assertNull(taskC);

        cmmnRuntimeService.triggerPlanItemInstance(taskA.getId());
        assertCaseInstanceNotEnded(caseInstance);
        assertEquals(2, cmmnRuntimeService.createPlanItemInstanceQuery().count());
        cmmnRuntimeService.triggerPlanItemInstance(taskB.getId());
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testRequiredPrecedenceDeepNest() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey(name.getMethodName()).start();
        assertNotNull(caseInstance);

        List<PlanItemInstance> list = cmmnRuntimeService.createPlanItemInstanceQuery().list();

        //Check case setup
        assertEquals(5, cmmnRuntimeService.createPlanItemInstanceQuery().count());

        PlanItemInstance taskA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskA").singleResult();
        assertNotNull(taskA);
        assertEquals(PlanItemInstanceState.ACTIVE, taskA.getState());

        PlanItemInstance stageOne = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("stageOne").singleResult();
        assertNotNull(stageOne);
        assertEquals(PlanItemInstanceState.AVAILABLE, stageOne.getState());

        List<PlanItemInstance> listeners = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER).list();
        assertEquals(3, listeners.size());
        listeners.forEach(l -> assertEquals(PlanItemInstanceState.AVAILABLE,l.getState()));

        //Trigger the test
        //Triggering Listener One will Activate StageOne which will complete as nothing ties it
        PlanItemInstance userEventOne = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER)
                .planItemDefinitionId("userEventOne").singleResult();
        cmmnRuntimeService.triggerPlanItemInstance(userEventOne.getId());
        stageOne = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("stageOne").singleResult();
        assertNull(stageOne);
        listeners = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER).list();
        assertEquals(2, listeners.size());
        assertCaseInstanceNotEnded(caseInstance);

        //The only thing keeping the case from ending is TaskA even with a deep nested required task, because its not AVAILABLE yet
        cmmnRuntimeService.triggerPlanItemInstance(taskA.getId());
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testRequiredPrecedenceDeepNest2() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey(name.getMethodName()).start();
        assertNotNull(caseInstance);

        //Check case setup
        assertEquals(5, cmmnRuntimeService.createPlanItemInstanceQuery().count());

        PlanItemInstance taskA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskA").singleResult();
        assertNotNull(taskA);
        assertEquals(PlanItemInstanceState.ACTIVE, taskA.getState());

        PlanItemInstance stageOne = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("stageOne").singleResult();
        assertNotNull(stageOne);
        assertEquals(PlanItemInstanceState.AVAILABLE, stageOne.getState());

        List<PlanItemInstance> listeners = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER).list();
        assertEquals(3, listeners.size());
        listeners.forEach(l -> assertEquals(PlanItemInstanceState.AVAILABLE,l.getState()));

        //Trigger the test
        //This time a task inside StageOne is required, thus it will not complete once activated
        PlanItemInstance userEvent = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER)
                .planItemDefinitionId("userEventOne").singleResult();
        cmmnRuntimeService.triggerPlanItemInstance(userEvent.getId());
        assertCaseInstanceNotEnded(caseInstance);

        assertEquals(6, cmmnRuntimeService.createPlanItemInstanceQuery().count());
        listeners = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER).list();
        assertEquals(2, listeners.size());
        taskA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskA").singleResult();
        assertNotNull(taskA);
        assertEquals(PlanItemInstanceState.ACTIVE, taskA.getState());
        stageOne = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("stageOne").singleResult();
        assertNotNull(stageOne);
        assertEquals(PlanItemInstanceState.ACTIVE, stageOne.getState());
        PlanItemInstance stageTwo = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("stageTwo").singleResult();
        assertNotNull(stageTwo);
        assertEquals(PlanItemInstanceState.AVAILABLE, stageTwo.getState());
        PlanItemInstance taskB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskB").singleResult();
        assertNotNull(taskB);
        assertEquals(PlanItemInstanceState.AVAILABLE, taskB.getState());

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
        assertNotNull(caseInstance);

        //Check case setup
        assertEquals(5, cmmnRuntimeService.createPlanItemInstanceQuery().count());

        PlanItemInstance taskA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskA").singleResult();
        assertNotNull(taskA);
        assertEquals(PlanItemInstanceState.ACTIVE, taskA.getState());

        PlanItemInstance stageOne = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("stageOne").singleResult();
        assertNotNull(stageOne);
        assertEquals(PlanItemInstanceState.AVAILABLE, stageOne.getState());

        List<PlanItemInstance> listeners = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER).list();
        assertEquals(3, listeners.size());
        listeners.forEach(l -> assertEquals(PlanItemInstanceState.AVAILABLE,l.getState()));


        //Trigger the test
        //This time a task inside StageOne is required, thus it will not complete once activated
        PlanItemInstance userEvent = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER)
                .planItemDefinitionId("userEventOne").singleResult();
        cmmnRuntimeService.triggerPlanItemInstance(userEvent.getId());
        assertCaseInstanceNotEnded(caseInstance);

        assertEquals(6, cmmnRuntimeService.createPlanItemInstanceQuery().count());
        listeners = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER).list();
        assertEquals(2, listeners.size());
        taskA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskA").singleResult();
        assertNotNull(taskA);
        assertEquals(PlanItemInstanceState.ACTIVE, taskA.getState());
        stageOne = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("stageOne").singleResult();
        assertNotNull(stageOne);
        assertEquals(PlanItemInstanceState.ACTIVE, stageOne.getState());
        PlanItemInstance stageTwo = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("stageTwo").singleResult();
        assertNotNull(stageTwo);
        assertEquals(PlanItemInstanceState.AVAILABLE, stageTwo.getState());
        PlanItemInstance taskB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskB").singleResult();
        assertNotNull(taskB);
        assertEquals(PlanItemInstanceState.AVAILABLE, taskB.getState());

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
        assertEquals(4, cmmnRuntimeService.createPlanItemInstanceQuery().count());
        listeners = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER).list();
        assertEquals(1, listeners.size());
        stageOne = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("stageOne").singleResult();
        assertNotNull(stageOne);
        assertEquals(PlanItemInstanceState.ACTIVE, stageOne.getState());
        stageTwo = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("stageTwo").singleResult();
        assertNotNull(stageTwo);
        assertEquals(PlanItemInstanceState.ACTIVE, stageTwo.getState());
        PlanItemInstance taskC = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("taskC").singleResult();
        assertNotNull(taskC);
        assertEquals(PlanItemInstanceState.AVAILABLE, taskC.getState());

        //Now we need to activate TaskC and complete it to end the case
        userEvent = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER)
                .planItemDefinitionId("userEventThree").singleResult();
        cmmnRuntimeService.triggerPlanItemInstance(userEvent.getId());
        cmmnRuntimeService.triggerPlanItemInstance(taskC.getId());
        assertCaseInstanceEnded(caseInstance);
    }
}
