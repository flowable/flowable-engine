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

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.task.api.Task;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * @author Dennis Federico
 */
public class UserEventListenerTest extends FlowableCmmnTestCase {

    @Rule
    public TestName name = new TestName();

    @Test
    @CmmnDeployment
    public void testSimpleEnableTask() {
        //Simple use of the UserEventListener as EntryCriteria of a Task
        //Case deployed
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey(name.getMethodName()).start();
        assertNotNull(caseInstance);
        assertEquals(1, cmmnRuntimeService.createCaseInstanceQuery().count());

        //3 PlanItems reachable
        assertEquals(3, cmmnRuntimeService.createPlanItemInstanceQuery().list().size());

        //1 User Event Listener
        PlanItemInstance listenerInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER).singleResult();
        assertNotNull(listenerInstance);
        assertEquals("userEventListener", listenerInstance.getPlanItemDefinitionId());
        assertEquals(PlanItemInstanceState.AVAILABLE, listenerInstance.getState());

        //2 HumanTasks ... one active and other waiting (available)
        assertEquals(2, cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK).count());
        PlanItemInstance active = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK).planItemInstanceStateActive().singleResult();
        assertNotNull(active);
        assertEquals("taskA", active.getPlanItemDefinitionId());
        PlanItemInstance available = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK).planItemInstanceStateAvailable().singleResult();
        assertNotNull(available);
        assertEquals("taskB", available.getPlanItemDefinitionId());

        //Trigger the listener
        cmmnRuntimeService.triggerPlanItemInstance(listenerInstance.getId());

        //UserEventListener should be completed
        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER).count());

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
        //Test case where the UserEventListener is used to complete (ExitCriteria) of a task
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey(name.getMethodName()).start();
        assertNotNull(caseInstance);
        assertEquals(1, cmmnRuntimeService.createCaseInstanceQuery().count());

        //4 PlanItems reachable
        assertEquals(4, cmmnRuntimeService.createPlanItemInstanceQuery().list().size());

        //1 Stage
        assertEquals(1, cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.STAGE).count());

        //1 User Event Listener
        PlanItemInstance listenerInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER).singleResult();
        assertNotNull(listenerInstance);
        assertEquals("userEventListener", listenerInstance.getPlanItemDefinitionId());
        assertEquals("userEventListenerPlanItem", listenerInstance.getElementId());
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
        //Test case where the UserEventListener is used to complete (ExitCriteria) of a Stage
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey(name.getMethodName()).start();
        assertNotNull(caseInstance);
        assertEquals(1, cmmnRuntimeService.createCaseInstanceQuery().count());

        //3 PlanItems reachable
        assertEquals(3, cmmnRuntimeService.createPlanItemInstanceQuery().list().size());

        //1 Stage
        PlanItemInstance stage = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.STAGE).singleResult();
        assertNotNull(stage);

        //1 User Event Listener
        PlanItemInstance listenerInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER).singleResult();
        assertNotNull(listenerInstance);
        assertEquals("userEventListener", listenerInstance.getPlanItemDefinitionId());
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
    public void testRepetitionWithUserEventExitCriteria() {
        //Test case where a repeating task is completed by its ExitCriteria fired by a UserEvent
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey(name.getMethodName())
                .variable("whileTrue", "true")
                .start();

        assertNotNull(caseInstance);

        for (int i = 0; i < 3; i++) {
            Task taskA = cmmnTaskService.createTaskQuery().active().taskDefinitionKey("taskA").singleResult();
            cmmnTaskService.complete(taskA.getId());
            assertCaseInstanceNotEnded(caseInstance);
        }

        PlanItemInstance userEvent = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER).singleResult();
        cmmnRuntimeService.triggerPlanItemInstance(userEvent.getId());
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testRepetitionWithUserEventEntryCriteria() {
        //Test case that activates a repeating task (entryCriteria) with a UserEvent
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey(name.getMethodName())
                .variable("whileRepeatTaskA", "true")
                .start();

        assertNotNull(caseInstance);

        //TaskA on available state until the entry criteria occurs
        PlanItemInstance taskA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK).planItemDefinitionId("taskA").singleResult();
        assertEquals(PlanItemInstanceState.AVAILABLE, taskA.getState());

        //Trigger the userEvent
        PlanItemInstance userEvent = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER).singleResult();
        cmmnRuntimeService.triggerPlanItemInstance(userEvent.getId());

        //UserEventListener is consumed
        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER).count());

        //Task is om Active state and a second task instance waiting for repetition
        Map<String, List<PlanItemInstance>> tasks = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .planItemDefinitionId("taskA")
                .list().stream()
                .collect(Collectors.groupingBy(PlanItemInstance::getState));
        assertEquals(2, tasks.size());
        assertTrue(tasks.containsKey(PlanItemInstanceState.ACTIVE));
        assertEquals(1, tasks.get(PlanItemInstanceState.ACTIVE).size());
        assertTrue(tasks.containsKey(PlanItemInstanceState.WAITING_FOR_REPETITION));
        assertEquals(1, tasks.get(PlanItemInstanceState.WAITING_FOR_REPETITION).size());

        //Complete active task
        cmmnTaskService.complete(cmmnTaskService.createTaskQuery().taskDefinitionKey("taskA").active().singleResult().getId());

        //Only TaskB remains on Active state and one instance of TaskA waiting for repetition
        tasks = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .list().stream().collect(Collectors.groupingBy(PlanItemInstance::getPlanItemDefinitionId));
        assertEquals(2, tasks.size());
        assertEquals(PlanItemInstanceState.ACTIVE, tasks.get("taskB").get(0).getState());
        assertEquals(PlanItemInstanceState.WAITING_FOR_REPETITION, tasks.get("taskA").get(0).getState());

        //Since WAITING_FOR_REPETITION is a "Semi-terminal", completing taskB should complete the stage and case
        cmmnTaskService.complete(cmmnTaskService.createTaskQuery().active().singleResult().getId());
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testCompleteWithoutFireEvent() {
        //Case deployed
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testUserEventListenerCompleteWithoutFireEvent").start();
        assertNotNull(caseInstance);
        assertEquals(1, cmmnRuntimeService.createCaseInstanceQuery().count());

        //4 PlanItems reachable
        assertEquals(3, cmmnRuntimeService.createPlanItemInstanceQuery().count());

        //1 Stage
        PlanItemInstance stage = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.STAGE).singleResult();
        assertNotNull(stage);

        //2 User Event Listener
        List<PlanItemInstance> listeners = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER).list();
        assertEquals(1, listeners.size());
        listeners.forEach(l -> assertEquals(PlanItemInstanceState.AVAILABLE, l.getState()));

        //1 Tasks on Active state
        PlanItemInstance task = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType("task").singleResult();
        assertNotNull(task);
        assertEquals(PlanItemInstanceState.ACTIVE, task.getState());
        assertEquals(stage.getId(), task.getStageInstanceId());

        //Complete the Task
        cmmnRuntimeService.triggerPlanItemInstance(task.getId());
        debugPlanItemInstances();

        PlanItemInstance listener = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER).singleResult();
        cmmnRuntimeService.triggerPlanItemInstance(listener.getId());


        //Stage and case instance should have ended...
        assertCaseInstanceEnded(caseInstance);

//        cmmnRuntimeService.createPlanItemInstanceQuery().list()
//                .forEach(i -> System.out.printf("%s:%s:%s%n", i.getPlanItemDefinitionId(), i.getState(), i.getId()));
    }

    private void debugPlanItemInstances() {
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().list();
        System.out.printf("=============%n");
        planItemInstances.forEach(i -> System.out.printf("%s:%s:%s%n", i.getId(), i.getPlanItemDefinitionId(), i.getState()));
    }
}

