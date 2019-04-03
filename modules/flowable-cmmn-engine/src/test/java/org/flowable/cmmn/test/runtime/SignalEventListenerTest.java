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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.api.runtime.SignalEventListenerInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.eventsubscription.api.EventSubscription;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

/**
 * @author Tijs Rademakers
 */
public class SignalEventListenerTest extends FlowableCmmnTestCase {

    @Rule
    public TestName name = new TestName();

    @Test
    @CmmnDeployment
    public void testSimpleEnableTask() {
        //Simple use of the UserEventListener as EntryCriteria of a Task
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey(name.getMethodName()).start();
        assertNotNull(caseInstance);
        assertEquals(1, cmmnRuntimeService.createCaseInstanceQuery().count());

        //3 PlanItems reachable
        assertEquals(3, cmmnRuntimeService.createPlanItemInstanceQuery().count());

        //1 User Event Listener
        PlanItemInstance listenerInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.SIGNAL_EVENT_LISTENER).singleResult();
        assertNotNull(listenerInstance);
        assertEquals("eventListener", listenerInstance.getPlanItemDefinitionId());
        assertEquals(PlanItemInstanceState.ACTIVE, listenerInstance.getState());
        
        // Verify same result is returned from query
        SignalEventListenerInstance eventListenerInstance = cmmnRuntimeService.createSignalEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertNotNull(eventListenerInstance);
        assertEquals(eventListenerInstance.getId(), listenerInstance.getId());
        assertEquals(eventListenerInstance.getCaseDefinitionId(), listenerInstance.getCaseDefinitionId());
        assertEquals(eventListenerInstance.getCaseInstanceId(), listenerInstance.getCaseInstanceId());
        assertEquals(eventListenerInstance.getElementId(), listenerInstance.getElementId());
        assertEquals(eventListenerInstance.getName(), listenerInstance.getName());
        assertEquals(eventListenerInstance.getPlanItemDefinitionId(), listenerInstance.getPlanItemDefinitionId());
        assertEquals(eventListenerInstance.getStageInstanceId(), listenerInstance.getStageInstanceId());
        assertEquals(eventListenerInstance.getState(), listenerInstance.getState());
        
        assertEquals(1, cmmnRuntimeService.createSignalEventListenerInstanceQuery().count());
        assertEquals(1, cmmnRuntimeService.createSignalEventListenerInstanceQuery().list().size());
        
        assertNotNull(cmmnRuntimeService.createSignalEventListenerInstanceQuery().caseDefinitionId(listenerInstance.getCaseDefinitionId()).singleResult());
        assertNotNull(cmmnRuntimeService.createSignalEventListenerInstanceQuery().caseDefinitionId(listenerInstance.getCaseDefinitionId()).singleResult());

        //2 HumanTasks ... one active and other waiting (available)
        assertEquals(2, cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK).count());
        PlanItemInstance active = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK).planItemInstanceStateActive().singleResult();
        assertNotNull(active);
        assertEquals("taskA", active.getPlanItemDefinitionId());
        PlanItemInstance available = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK).planItemInstanceStateAvailable().singleResult();
        assertNotNull(available);
        assertEquals("taskB", available.getPlanItemDefinitionId());
        
        EventSubscription eventSubscription = cmmnRuntimeService.createEventSubscriptionQuery().scopeId(eventListenerInstance.getCaseInstanceId()).singleResult();
        assertNotNull(eventSubscription);
        assertEquals(eventListenerInstance.getId(), eventSubscription.getSubScopeId());
        assertEquals(eventListenerInstance.getCaseDefinitionId(), eventSubscription.getScopeDefinitionId());
        assertEquals(ScopeTypes.CMMN, eventSubscription.getScopeType());
        assertEquals("testSignal", eventSubscription.getEventName());

        // Trigger the signal
        cmmnRuntimeService.triggerPlanItemInstance(listenerInstance.getId());

        // SignalEventListener should be completed
        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.SIGNAL_EVENT_LISTENER).count());

        // Only 2 PlanItems left
        assertEquals(2, cmmnRuntimeService.createPlanItemInstanceQuery().list().size());

        // Both Human task should be "active" now, as the sentry kicks on the SignalEventListener transition
        assertEquals(2, cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK).planItemInstanceStateActive().count());
        
        eventSubscription = cmmnRuntimeService.createEventSubscriptionQuery().scopeId(eventListenerInstance.getCaseInstanceId()).singleResult();
        assertNull(eventSubscription);

        // Finish the case instance
        assertCaseInstanceNotEnded(caseInstance);
        cmmnTaskService.createTaskQuery().list().forEach(t -> cmmnTaskService.complete(t.getId()));
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testTerminateStage() {
        //Test case where the SignalEventListener is used to complete (ExitCriteria) of a Stage
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey(name.getMethodName()).start();
        assertNotNull(caseInstance);
        assertEquals(1, cmmnRuntimeService.createCaseInstanceQuery().count());

        //3 PlanItems reachable
        assertEquals(3, cmmnRuntimeService.createPlanItemInstanceQuery().list().size());

        //1 Stage
        PlanItemInstance stage = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.STAGE).singleResult();
        assertNotNull(stage);

        //1 User Event Listener
        PlanItemInstance listenerInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.SIGNAL_EVENT_LISTENER).singleResult();
        assertNotNull(listenerInstance);
        assertEquals("eventListener", listenerInstance.getPlanItemDefinitionId());
        assertEquals(PlanItemInstanceState.ACTIVE, listenerInstance.getState());

        //1 Task on Active state
        PlanItemInstance task = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType("task").singleResult();
        assertNotNull(task);
        assertEquals(PlanItemInstanceState.ACTIVE, task.getState());
        assertEquals(stage.getId(), task.getStageInstanceId());

        //Trigger the listener
        assertCaseInstanceNotEnded(caseInstance);
        cmmnRuntimeService.triggerPlanItemInstance(listenerInstance.getId());
        assertCaseInstanceEnded(caseInstance);
    }

}

