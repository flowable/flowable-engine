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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.GenericEventListenerInstance;
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.task.api.Task;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

/**
 * @author Tijs Rademakers
 */
public class GenericEventListenerTest extends FlowableCmmnTestCase {

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
        PlanItemInstance listenerInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.GENERIC_EVENT_LISTENER).singleResult();
        assertNotNull(listenerInstance);
        assertEquals("eventListener", listenerInstance.getPlanItemDefinitionId());
        assertEquals(PlanItemInstanceState.AVAILABLE, listenerInstance.getState());
        
        // Verify same result is returned from query
        GenericEventListenerInstance eventListenerInstance = cmmnRuntimeService.createGenericEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertNotNull(eventListenerInstance);
        assertEquals(eventListenerInstance.getId(), listenerInstance.getId());
        assertEquals(eventListenerInstance.getCaseDefinitionId(), listenerInstance.getCaseDefinitionId());
        assertEquals(eventListenerInstance.getCaseInstanceId(), listenerInstance.getCaseInstanceId());
        assertEquals(eventListenerInstance.getElementId(), listenerInstance.getElementId());
        assertEquals(eventListenerInstance.getName(), listenerInstance.getName());
        assertEquals(eventListenerInstance.getPlanItemDefinitionId(), listenerInstance.getPlanItemDefinitionId());
        assertEquals(eventListenerInstance.getStageInstanceId(), listenerInstance.getStageInstanceId());
        assertEquals(eventListenerInstance.getState(), listenerInstance.getState());
        
        assertEquals(1, cmmnRuntimeService.createGenericEventListenerInstanceQuery().count());
        assertEquals(1, cmmnRuntimeService.createGenericEventListenerInstanceQuery().list().size());
        
        assertNotNull(cmmnRuntimeService.createGenericEventListenerInstanceQuery().caseDefinitionId(listenerInstance.getCaseDefinitionId()).singleResult());
        assertNotNull(cmmnRuntimeService.createGenericEventListenerInstanceQuery().caseDefinitionId(listenerInstance.getCaseDefinitionId()).singleResult());

        //2 HumanTasks ... one active and other waiting (available)
        assertEquals(2, cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK).count());
        PlanItemInstance active = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK).planItemInstanceStateActive().singleResult();
        assertNotNull(active);
        assertEquals("taskA", active.getPlanItemDefinitionId());
        PlanItemInstance available = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK).planItemInstanceStateAvailable().singleResult();
        assertNotNull(available);
        assertEquals("taskB", available.getPlanItemDefinitionId());

        //Trigger the listener
        cmmnRuntimeService.completeGenericEventListenerInstance(listenerInstance.getId());

        //UserEventListener should be completed
        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.GENERIC_EVENT_LISTENER).count());

        //Only 2 PlanItems left
        assertEquals(2, cmmnRuntimeService.createPlanItemInstanceQuery().list().size());

        //Both Human task should be "active" now, as the sentry kicks on the UserEventListener transition
        assertEquals(2, cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK).planItemInstanceStateActive().count());

        //Finish the caseInstance
        assertCaseInstanceNotEnded(caseInstance);
        cmmnTaskService.createTaskQuery().list().forEach(t -> cmmnTaskService.complete(t.getId()));
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testTerminateTask() {
        //Test case where the EventListener is used to complete (ExitCriteria) of a task
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey(name.getMethodName()).start();
        assertNotNull(caseInstance);
        assertEquals(1, cmmnRuntimeService.createCaseInstanceQuery().count());

        //4 PlanItems reachable
        assertEquals(4, cmmnRuntimeService.createPlanItemInstanceQuery().list().size());

        //1 Stage
        assertEquals(1, cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.STAGE).count());

        //1 Event Listener
        PlanItemInstance listenerInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.GENERIC_EVENT_LISTENER).singleResult();
        assertNotNull(listenerInstance);
        assertEquals("eventListener", listenerInstance.getPlanItemDefinitionId());
        assertEquals("eventListenerPlanItem", listenerInstance.getElementId());
        assertEquals(PlanItemInstanceState.AVAILABLE, listenerInstance.getState());

        //2 Tasks on Active state
        List<PlanItemInstance> tasks = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType("task").list();
        assertEquals(2, tasks.size());
        tasks.forEach(t -> assertEquals(PlanItemInstanceState.ACTIVE, t.getState()));

        //Trigger the listener
        cmmnRuntimeService.triggerPlanItemInstance(listenerInstance.getId());

        //UserEventListener should be completed
        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER).count());

        //Only 2 PlanItems left (1 stage & 1 active task)
        assertEquals(2, cmmnRuntimeService.createPlanItemInstanceQuery().list().size());
        PlanItemInstance activeTask = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType("task").planItemInstanceStateActive().singleResult();
        assertNotNull(activeTask);
        assertEquals("taskB", activeTask.getPlanItemDefinitionId());

        //Finish the caseInstance
        assertCaseInstanceNotEnded(caseInstance);
        cmmnRuntimeService.triggerPlanItemInstance(activeTask.getId());
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testTerminateStage() {
        //Test case where the GenericEventListener is used to complete (ExitCriteria) of a Stage
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey(name.getMethodName()).start();
        assertNotNull(caseInstance);
        assertEquals(1, cmmnRuntimeService.createCaseInstanceQuery().count());

        //3 PlanItems reachable
        assertEquals(3, cmmnRuntimeService.createPlanItemInstanceQuery().list().size());

        //1 Stage
        PlanItemInstance stage = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.STAGE).singleResult();
        assertNotNull(stage);

        //1 User Event Listener
        PlanItemInstance listenerInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.GENERIC_EVENT_LISTENER).singleResult();
        assertNotNull(listenerInstance);
        assertEquals("eventListener", listenerInstance.getPlanItemDefinitionId());
        assertEquals(PlanItemInstanceState.AVAILABLE, listenerInstance.getState());

        //1 Tasks on Active state
        PlanItemInstance task = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType("task").singleResult();
        assertNotNull(task);
        assertEquals(PlanItemInstanceState.ACTIVE, task.getState());
        assertEquals(stage.getId(), task.getStageInstanceId());

        //Trigger the listener
        assertCaseInstanceNotEnded(caseInstance);
        cmmnRuntimeService.triggerPlanItemInstance(listenerInstance.getId());
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testGenericEventListenerInstanceQuery() {
        //Test for EventListenerInstanceQuery
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey(name.getMethodName())
                .start();
        assertNotNull(caseInstance);
        assertEquals(1, cmmnRuntimeService.createCaseInstanceQuery().count());

        //All planItemInstances
        assertEquals(8, cmmnRuntimeService.createPlanItemInstanceQuery().count());

        //UserEventListenerIntances
        assertEquals(6, cmmnRuntimeService.createGenericEventListenerInstanceQuery().stateAvailable().count());

        List<GenericEventListenerInstance> events = cmmnRuntimeService.createGenericEventListenerInstanceQuery().list();
        assertEquals(6, events.size());

        //All different Intances id's
        assertEquals(6, events.stream().map(GenericEventListenerInstance::getId).distinct().count());

        //UserEventListenerIntances inside Stage1
        String stage1Id = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.STAGE).planItemDefinitionId("stage1").singleResult().getId();
        assertEquals(2, cmmnRuntimeService.createGenericEventListenerInstanceQuery().stageInstanceId(stage1Id).count());

        //UserEventListenerIntances inside Stage2
        String stage2Id = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.STAGE).planItemDefinitionId("stage2").singleResult().getId();
        assertEquals(2, cmmnRuntimeService.createGenericEventListenerInstanceQuery().stageInstanceId(stage2Id).count());

        //UserEventListenerIntances not in a Stage
        assertEquals(2, events.stream().filter(e -> e.getStageInstanceId() == null).count());

        //Test query by elementId
        assertNotNull(cmmnRuntimeService.createGenericEventListenerInstanceQuery().elementId("caseEventListenerOne").singleResult());

        //Test query by planItemDefinitionId
        assertEquals(2, cmmnRuntimeService.createGenericEventListenerInstanceQuery().planItemDefinitionId("caseUEL1").count());

        //Test sort Order - using the names because equals is not implemented in UserEventListenerInstance
        List<String> names = events.stream().map(GenericEventListenerInstance::getName).sorted(Comparator.reverseOrder()).collect(Collectors.toList());
        List<String> namesDesc = cmmnRuntimeService.createGenericEventListenerInstanceQuery()
                .stateAvailable().orderByName().desc().list().stream().map(GenericEventListenerInstance::getName).collect(Collectors.toList());
        assertThat(names).isEqualTo(namesDesc);
    }

    @Test
    @CmmnDeployment
    public void testOrphanEventListenerMultipleSentries() {

        // The model here has one event listener that will trigger the exit of two tasks (A and C) and trigger the activation of task B

        // Verify the model setup
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testOrphanEventListenerMultipleSentries").start();
        assertNotNull(cmmnRuntimeService.createGenericEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult());
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertEquals(2, tasks.size());
        assertEquals("A", tasks.get(0).getName());
        assertEquals("C", tasks.get(1).getName());

        // Completing one tasks should not have impact on the event listener
        cmmnTaskService.complete(tasks.get(0).getId());
        assertNotNull(cmmnRuntimeService.createGenericEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult());
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertEquals(1, tasks.size());
        assertEquals("C", tasks.get(0).getName());

        // Completing the other task with the exit sentry should still keep the event, as it's reference by the entry of B
        cmmnTaskService.complete(tasks.get(0).getId());
        assertNotNull(cmmnRuntimeService.createGenericEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult());
        assertEquals(0, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());

        // Firing the event listener should start B
        cmmnRuntimeService.completeGenericEventListenerInstance(cmmnRuntimeService.createGenericEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getId());
        assertNull(cmmnRuntimeService.createGenericEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult());
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
    }

    @Test
    @CmmnDeployment
    public void testMultipleEventListenersAsEntry() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testMultipleEventListenersAsEntry").start();
        assertEquals(0, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());

        List<GenericEventListenerInstance> eventListenerInstances = cmmnRuntimeService.createGenericEventListenerInstanceQuery()
            .caseInstanceId(caseInstance.getId()).orderByName().asc().list();
        assertThat(eventListenerInstances).extracting(GenericEventListenerInstance::getName).containsExactly("A", "B", "C");

        // Completing A should change nothing
        cmmnRuntimeService.completeGenericEventListenerInstance(eventListenerInstances.get(0).getId());
        assertEquals(0, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        eventListenerInstances = cmmnRuntimeService.createGenericEventListenerInstanceQuery()
            .caseInstanceId(caseInstance.getId()).orderByName().asc().list();
        assertThat(eventListenerInstances).extracting(GenericEventListenerInstance::getName).containsExactly("B", "C");

        // Completing B should activate the stage and remove the orphan event listener C
        cmmnRuntimeService.completeGenericEventListenerInstance(eventListenerInstances.get(0).getId());
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        assertEquals(0, cmmnRuntimeService.createGenericEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).count());
    }

    // TODO: needs dedicated test setup with event registry
//    @Test
//    @CmmnDeployment
//    public void testGenericEventListenerWithEventType() {
//        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
//        assertEquals(1, cmmnTaskService.createTaskQuery().count());
//
//        // There should be one event subscription
//        List<EventSubscription> allEventSubscriptions = getAllEventSubscriptions();
//        assertEquals(1, allEventSubscriptions.size());
//    }
//
//    protected List<EventSubscription> getAllEventSubscriptions() {
//        return cmmnEngineConfiguration.getCommandExecutor().execute(new Command<List<EventSubscription>>() {
//
//            @Override
//            public List<EventSubscription> execute(CommandContext commandContext) {
//                return CommandContextUtil.getEventSubscriptionServiceConfiguration(commandContext)
//                    .getEventSubscriptionEntityManager().findEventSubscriptionsByQueryCriteria(new EventSubscriptionQueryImpl(commandContext));
//            }
//        });
//    }

}

