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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.api.runtime.UserEventListenerInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.task.api.Task;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

/**
 * @author Dennis Federico
 * @author Joram Barrez
 */
public class UserEventListenerTest extends FlowableCmmnTestCase {

    @Rule
    public TestName name = new TestName();

    @Test
    @CmmnDeployment
    public void testSimpleEnableTask() {
        //Simple use of the UserEventListener as EntryCriteria of a Task
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey(name.getMethodName()).start();
        assertThat(caseInstance).isNotNull();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isEqualTo(1);

        //3 PlanItems reachable
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isEqualTo(3);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().list())
                .extracting(PlanItemInstance::getPlanItemDefinitionType, PlanItemInstance::getPlanItemDefinitionId, PlanItemInstance::getState)
                .containsExactlyInAnyOrder(
                        tuple(PlanItemDefinitionType.USER_EVENT_LISTENER, "userEventListener", PlanItemInstanceState.AVAILABLE),
                        tuple(PlanItemDefinitionType.HUMAN_TASK, "taskA", PlanItemInstanceState.ACTIVE),
                        tuple(PlanItemDefinitionType.HUMAN_TASK, "taskB", PlanItemInstanceState.AVAILABLE)
                );

        //1 User Event Listener
        PlanItemInstance listenerInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER)
                .singleResult();
        assertThat(listenerInstance).isNotNull();
        assertThat(listenerInstance.getPlanItemDefinitionId()).isEqualTo("userEventListener");
        assertThat(listenerInstance.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);

        // Verify same result is returned from query
        UserEventListenerInstance userEventListenerInstance = cmmnRuntimeService.createUserEventListenerInstanceQuery().caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(userEventListenerInstance).isNotNull();
        assertThat(listenerInstance.getId()).isEqualTo(userEventListenerInstance.getId());
        assertThat(listenerInstance.getCaseDefinitionId()).isEqualTo(userEventListenerInstance.getCaseDefinitionId());
        assertThat(listenerInstance.getCaseInstanceId()).isEqualTo(userEventListenerInstance.getCaseInstanceId());
        assertThat(listenerInstance.getElementId()).isEqualTo(userEventListenerInstance.getElementId());
        assertThat(listenerInstance.getName()).isEqualTo(userEventListenerInstance.getName());
        assertThat(listenerInstance.getPlanItemDefinitionId()).isEqualTo(userEventListenerInstance.getPlanItemDefinitionId());
        assertThat(listenerInstance.getStageInstanceId()).isEqualTo(userEventListenerInstance.getStageInstanceId());
        assertThat(listenerInstance.getState()).isEqualTo(userEventListenerInstance.getState());

        assertThat(cmmnRuntimeService.createUserEventListenerInstanceQuery().count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createUserEventListenerInstanceQuery().list()).hasSize(1);

        assertThat(cmmnRuntimeService.createUserEventListenerInstanceQuery().caseDefinitionId(listenerInstance.getCaseDefinitionId()).singleResult())
                .isNotNull();

        //2 HumanTasks ... one active and other waiting (available)
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK).count()).isEqualTo(2);
        PlanItemInstance active = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .planItemInstanceStateActive().singleResult();
        assertThat(active).isNotNull();
        assertThat(active.getPlanItemDefinitionId()).isEqualTo("taskA");
        PlanItemInstance available = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .planItemInstanceStateAvailable().singleResult();
        assertThat(available).isNotNull();
        assertThat(available.getPlanItemDefinitionId()).isEqualTo("taskB");

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().list())
                    .extracting(HistoricPlanItemInstance::getPlanItemDefinitionType, HistoricPlanItemInstance::getPlanItemDefinitionId, HistoricPlanItemInstance::getState)
                    .containsExactlyInAnyOrder(
                            tuple(PlanItemDefinitionType.USER_EVENT_LISTENER, "userEventListener", PlanItemInstanceState.AVAILABLE),
                            tuple(PlanItemDefinitionType.HUMAN_TASK, "taskA", PlanItemInstanceState.ACTIVE),
                            tuple(PlanItemDefinitionType.HUMAN_TASK, "taskB", PlanItemInstanceState.AVAILABLE)
                    );
        }

        //Trigger the listener
        cmmnRuntimeService.completeUserEventListenerInstance(listenerInstance.getId());

        //UserEventListener should be completed
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER).count()).isZero();

        //Only 2 PlanItems left
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().list()).hasSize(2);

        //Both Human task should be "active" now, as the sentry kicks on the UserEventListener transition
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK).planItemInstanceStateActive()
                .count()).isEqualTo(2);

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().list())
                .extracting(PlanItemInstance::getPlanItemDefinitionType, PlanItemInstance::getPlanItemDefinitionId, PlanItemInstance::getState)
                .containsExactlyInAnyOrder(
                        tuple(PlanItemDefinitionType.HUMAN_TASK, "taskA", PlanItemInstanceState.ACTIVE),
                        tuple(PlanItemDefinitionType.HUMAN_TASK, "taskB", PlanItemInstanceState.ACTIVE)
                );

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().list())
                    .extracting(HistoricPlanItemInstance::getPlanItemDefinitionType, HistoricPlanItemInstance::getPlanItemDefinitionId, HistoricPlanItemInstance::getState)
                    .containsExactlyInAnyOrder(
                            tuple(PlanItemDefinitionType.USER_EVENT_LISTENER, "userEventListener", PlanItemInstanceState.COMPLETED),
                            tuple(PlanItemDefinitionType.HUMAN_TASK, "taskA", PlanItemInstanceState.ACTIVE),
                            tuple(PlanItemDefinitionType.HUMAN_TASK, "taskB", PlanItemInstanceState.ACTIVE)
                    );
        }

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
        assertThat(caseInstance).isNotNull();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isEqualTo(1);

        //4 PlanItems reachable
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().list()).hasSize(4);

        //1 Stage
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.STAGE).count()).isEqualTo(1);

        //1 User Event Listener
        PlanItemInstance listenerInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER)
                .singleResult();
        assertThat(listenerInstance).isNotNull();
        assertThat(listenerInstance.getPlanItemDefinitionId()).isEqualTo("userEventListener");
        assertThat(listenerInstance.getElementId()).isEqualTo("userEventListenerPlanItem");
        assertThat(listenerInstance.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);

        //2 Tasks on Active state
        List<PlanItemInstance> tasks = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType("task").list();
        assertThat(tasks)
                .extracting(PlanItemInstance::getState)
                .containsExactly(PlanItemInstanceState.ACTIVE, PlanItemInstanceState.ACTIVE);

        //Trigger the listener
        cmmnRuntimeService.triggerPlanItemInstance(listenerInstance.getId());

        //UserEventListener should be completed
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER).count()).isZero();

        //Only 2 PlanItems left (1 stage & 1 active task)
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().list()).hasSize(2);
        PlanItemInstance activeTask = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType("task").planItemInstanceStateActive()
                .singleResult();
        assertThat(activeTask).isNotNull();
        assertThat(activeTask.getPlanItemDefinitionId()).isEqualTo("taskB");

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
        assertThat(caseInstance).isNotNull();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isEqualTo(1);

        //3 PlanItems reachable
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().list()).hasSize(3);

        //1 Stage
        PlanItemInstance stage = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.STAGE).singleResult();
        assertThat(stage).isNotNull();

        //1 User Event Listener
        PlanItemInstance listenerInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER)
                .singleResult();
        assertThat(listenerInstance).isNotNull();
        assertThat(listenerInstance.getPlanItemDefinitionId()).isEqualTo("userEventListener");
        assertThat(listenerInstance.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);

        //1 Tasks on Active state
        PlanItemInstance task = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType("task").singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(task.getStageInstanceId()).isEqualTo(stage.getId());

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

        assertThat(caseInstance).isNotNull();

        for (int i = 0; i < 3; i++) {
            Task taskA = cmmnTaskService.createTaskQuery().active().taskDefinitionKey("taskA").singleResult();
            cmmnTaskService.complete(taskA.getId());
            assertCaseInstanceNotEnded(caseInstance);
        }

        PlanItemInstance userEvent = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER)
                .singleResult();
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

        assertThat(caseInstance).isNotNull();

        //TaskA on available state until the entry criteria occurs
        PlanItemInstance taskA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .planItemDefinitionId("taskA").singleResult();
        assertThat(taskA.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);

        //Trigger the userEvent
        PlanItemInstance userEvent = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER)
                .singleResult();
        cmmnRuntimeService.triggerPlanItemInstance(userEvent.getId());

        //UserEventListener is consumed
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER).count()).isZero();

        //Task is om Active state and a second task instance waiting for repetition
        Map<String, List<PlanItemInstance>> tasks = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .planItemDefinitionId("taskA")
                .list().stream()
                .collect(Collectors.groupingBy(PlanItemInstance::getState));
        assertThat(tasks)
                .hasSize(2)
                .containsKey(PlanItemInstanceState.ACTIVE);
        assertThat(tasks.get(PlanItemInstanceState.ACTIVE)).hasSize(1);
        assertThat(tasks).containsKey(PlanItemInstanceState.WAITING_FOR_REPETITION);
        assertThat(tasks.get(PlanItemInstanceState.WAITING_FOR_REPETITION)).hasSize(1);

        //Complete active task
        cmmnTaskService.complete(cmmnTaskService.createTaskQuery().taskDefinitionKey("taskA").active().singleResult().getId());

        //Only TaskB remains on Active state and one instance of TaskA waiting for repetition
        tasks = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .list().stream().collect(Collectors.groupingBy(PlanItemInstance::getPlanItemDefinitionId));
        assertThat(tasks).hasSize(2);
        assertThat(tasks.get("taskB").get(0).getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(tasks.get("taskA").get(0).getState()).isEqualTo(PlanItemInstanceState.WAITING_FOR_REPETITION);

        //Since WAITING_FOR_REPETITION is a "Semi-terminal", completing taskB should complete the stage and case
        cmmnTaskService.complete(cmmnTaskService.createTaskQuery().active().singleResult().getId());
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testStageWithoutFiringTheEvent() {
        //Test case where the only "standing" plainItem for a stage is a UserEventListener
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey(name.getMethodName())
                .start();
        assertThat(caseInstance).isNotNull();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isEqualTo(1);

        Map<String, List<PlanItemInstance>> planItems = cmmnRuntimeService.createPlanItemInstanceQuery()
                .list().stream().collect(Collectors.groupingBy(PlanItemInstance::getPlanItemDefinitionType));

        //3 types of planItems
        assertThat(planItems).hasSize(3);

        //1 User Event Listener
        assertThat(planItems.getOrDefault(PlanItemDefinitionType.USER_EVENT_LISTENER, Collections.emptyList())).hasSize(1);
        //1 Stage
        assertThat(planItems.getOrDefault(PlanItemDefinitionType.STAGE, Collections.emptyList())).hasSize(1);
        PlanItemInstance stage = planItems.get(PlanItemDefinitionType.STAGE).get(0);

        //1 Active Task (inside the stage)
        assertThat(planItems.getOrDefault("task", Collections.emptyList())).hasSize(1);
        PlanItemInstance task = planItems.get("task").get(0);
        assertThat(task.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(task.getStageInstanceId()).isEqualTo(stage.getId());

        //Complete the Task
        cmmnRuntimeService.triggerPlanItemInstance(task.getId());

        //Listener should be deleted as it's an orphan
        planItems = cmmnRuntimeService.createPlanItemInstanceQuery()
                .list().stream().collect(Collectors.groupingBy(PlanItemInstance::getPlanItemDefinitionType));
        assertThat(planItems).isEmpty();

        //Stage and case instance should have ended...
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testCaseWithoutFiringTheEvent() {
        //Test case where the only "standing" plainItem for a Case is a UserEventListener
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey(name.getMethodName())
                .start();
        assertThat(caseInstance).isNotNull();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isEqualTo(1);

        Map<String, List<PlanItemInstance>> planItems = cmmnRuntimeService.createPlanItemInstanceQuery()
                .list().stream().collect(Collectors.groupingBy(PlanItemInstance::getPlanItemDefinitionType));

        //3 types of planItems
        assertThat(planItems).hasSize(3);

        //1 Stage
        assertThat(planItems.getOrDefault(PlanItemDefinitionType.STAGE, Collections.emptyList())).hasSize(1);
        PlanItemInstance stage = planItems.get(PlanItemDefinitionType.STAGE).get(0);

        //1 Active Task (inside the stage)
        assertThat(planItems.getOrDefault("task", Collections.emptyList())).hasSize(1);
        PlanItemInstance task = planItems.get("task").get(0);
        assertThat(task.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(task.getStageInstanceId()).isEqualTo(stage.getId());

        //1 Available User Event Listener (outside the stage)
        assertThat(planItems.getOrDefault(PlanItemDefinitionType.USER_EVENT_LISTENER, Collections.emptyList())).hasSize(1);
        PlanItemInstance listener = planItems.get(PlanItemDefinitionType.USER_EVENT_LISTENER).get(0);
        assertThat(listener.getStageInstanceId()).isNull();

        //Complete the Task
        cmmnRuntimeService.triggerPlanItemInstance(task.getId());

        //Listener should be terminated as it's orphaned
        planItems = cmmnRuntimeService.createPlanItemInstanceQuery()
                .list().stream().collect(Collectors.groupingBy(PlanItemInstance::getPlanItemDefinitionType));
        assertThat(planItems).isEmpty();
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testAutocompleteStageWithoutFiringTheEvent() {
        //Test case where the only "standing" plainItem for a autocomplete stage is a UserEventListener
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey(name.getMethodName())
                .start();
        assertThat(caseInstance).isNotNull();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isEqualTo(1);

        //3 PlanItems reachable
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isEqualTo(3);

        //1 Stage
        PlanItemInstance stage = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.STAGE).singleResult();
        assertThat(stage).isNotNull();

        //1 User Event Listener
        List<PlanItemInstance> listeners = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER)
                .list();
        assertThat(listeners)
                .extracting(PlanItemInstance::getState)
                .containsExactly(PlanItemInstanceState.AVAILABLE);

        //1 Tasks on Active state
        PlanItemInstance task = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType("task").singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(task.getStageInstanceId()).isEqualTo(stage.getId());

        //Complete the Task
        cmmnRuntimeService.triggerPlanItemInstance(task.getId());

        //Stage and case instance should have ended...
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testUserEventListenerInstanceQuery() {
        //Test for UserEventListenerInstanceQuery
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey(name.getMethodName())
                .start();
        assertThat(caseInstance).isNotNull();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isEqualTo(1);

        //All planItemInstances
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isEqualTo(8);

        //UserEventListenerInstances
        assertThat(cmmnRuntimeService.createUserEventListenerInstanceQuery().stateAvailable().count()).isEqualTo(6);

        List<UserEventListenerInstance> events = cmmnRuntimeService.createUserEventListenerInstanceQuery().list();
        assertThat(events).hasSize(6);

        //All different Instances id's
        assertThat(events.stream().map(UserEventListenerInstance::getId).distinct().count()).isEqualTo(6);

        //UserEventListenerInstances inside Stage1
        String stage1Id = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.STAGE).planItemDefinitionId("stage1")
                .singleResult().getId();
        assertThat(cmmnRuntimeService.createUserEventListenerInstanceQuery().stageInstanceId(stage1Id).count()).isEqualTo(2);

        //UserEventListenerInstances inside Stage2
        String stage2Id = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.STAGE).planItemDefinitionId("stage2")
                .singleResult().getId();
        assertThat(cmmnRuntimeService.createUserEventListenerInstanceQuery().stageInstanceId(stage2Id).count()).isEqualTo(2);

        //UserEventListenerInstances not in a Stage
        assertThat(events.stream().filter(e -> e.getStageInstanceId() == null).count()).isEqualTo(2);

        //Test query by elementId
        assertThat(cmmnRuntimeService.createUserEventListenerInstanceQuery().elementId("caseUserEventListenerOne").singleResult()).isNotNull();

        //Test query by planItemDefinitionId
        assertThat(cmmnRuntimeService.createUserEventListenerInstanceQuery().planItemDefinitionId("caseUEL1").count()).isEqualTo(2);

        //Test sort Order - using the names because equals is not implemented in UserEventListenerInstance
        List<String> names = events.stream().map(UserEventListenerInstance::getName).sorted(Comparator.reverseOrder()).collect(Collectors.toList());
        List<String> namesDesc = cmmnRuntimeService.createUserEventListenerInstanceQuery()
                .stateAvailable().orderByName().desc().list().stream().map(UserEventListenerInstance::getName).collect(Collectors.toList());
        assertThat(names).isEqualTo(namesDesc);

        //TODO suspended state query (need to suspend the parent stage)

    }

    @Test
    @CmmnDeployment
    public void testUserEventInstanceDeletedWhenNotReferencedByExitSentry() {

        cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testUserEvent").start();
        assertThat(cmmnRuntimeService.createUserEventListenerInstanceQuery().singleResult()).isNotNull();

        // Completing task A and B completes Stage A.
        // This should also remove the user event listener, as nothing is referencing it anymore

        List<Task> tasks = cmmnTaskService.createTaskQuery().list();
        assertThat(tasks).hasSize(2);
        tasks.forEach(t -> cmmnTaskService.complete(t.getId()));

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("Stage A").singleResult()).isNull();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("Stage A").includeEnded().singleResult()).isNotNull();
        assertThat(cmmnRuntimeService.createUserEventListenerInstanceQuery().singleResult()).isNull();
    }

    @Test
    @CmmnDeployment
    public void testOrphanEventListenerMultipleSentries() {

        // The model here has one user event listener that will trigger the exit of two tasks (A and C) and trigger the activation of task B

        // Verify the model setup
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testOrphanEventListenerMultipleSentries").start();
        assertThat(cmmnRuntimeService.createUserEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult()).isNotNull();
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("A", "C");

        // Completing one tasks should not have impact on the user event listener
        cmmnTaskService.complete(tasks.get(0).getId());
        assertThat(cmmnRuntimeService.createUserEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult()).isNotNull();
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("C");

        // Completing the other task with the exit sentry should still keep the user event, as it's reference by the entry of B
        cmmnTaskService.complete(tasks.get(0).getId());
        assertThat(cmmnRuntimeService.createUserEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult()).isNotNull();
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        // Firing the user event listener should start B
        cmmnRuntimeService.completeUserEventListenerInstance(
                cmmnRuntimeService.createUserEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getId());
        assertThat(cmmnRuntimeService.createUserEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult()).isNull();
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
    }

    @Test
    @CmmnDeployment
    public void testOrphanEventListenerMultipleSentries2() {

        // Firing the event listener should exit A and C, and activate B
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testOrphanEventListenerMultipleSentries").start();
        cmmnRuntimeService.completeUserEventListenerInstance(
                cmmnRuntimeService.createUserEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getId());

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("B");

        cmmnTaskService.complete(task.getId());
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testMultipleEventListenersAsEntry() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testMultipleEventListenersAsEntry").start();
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        List<UserEventListenerInstance> userEventListenerInstances = cmmnRuntimeService.createUserEventListenerInstanceQuery()
                .caseInstanceId(caseInstance.getId()).orderByName().asc().list();
        assertThat(userEventListenerInstances)
                .extracting(UserEventListenerInstance::getName)
                .containsExactly("A", "B", "C");

        // Completing A should change nothing
        cmmnRuntimeService.completeUserEventListenerInstance(userEventListenerInstances.get(0).getId());
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
        userEventListenerInstances = cmmnRuntimeService.createUserEventListenerInstanceQuery()
                .caseInstanceId(caseInstance.getId()).orderByName().asc().list();
        assertThat(userEventListenerInstances)
                .extracting(UserEventListenerInstance::getName)
                .containsExactly("B", "C");

        // Completing B should activate the stage and remove the orphan event listener C
        cmmnRuntimeService.completeUserEventListenerInstance(userEventListenerInstances.get(0).getId());
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createUserEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
    }

    @Test
    @CmmnDeployment
    public void testMultipleEventListenersAsExit() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testMultipleEventListenersAsExit").start();
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(2);

        List<UserEventListenerInstance> userEventListenerInstances = cmmnRuntimeService.createUserEventListenerInstanceQuery()
                .caseInstanceId(caseInstance.getId()).orderByName().asc().list();
        assertThat(userEventListenerInstances)
                .extracting(UserEventListenerInstance::getName)
                .containsExactly("A", "B", "C");

        // Completing C should also remove A and B
        cmmnRuntimeService.completeUserEventListenerInstance(userEventListenerInstances.get(2).getId());
        assertThat(cmmnRuntimeService.createUserEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult().getName()).isEqualTo("Outside stage");

    }

    @Test
    @CmmnDeployment
    public void testMultipleEventListenersMixed() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testMultipleEventListenersMixed").start();
        List<UserEventListenerInstance> userEventListenerInstances = cmmnRuntimeService.createUserEventListenerInstanceQuery()
                .caseInstanceId(caseInstance.getId()).orderByName().asc().list();
        assertThat(userEventListenerInstances)
                .extracting(UserEventListenerInstance::getName)
                .containsExactly("A", "B", "C");
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("The task").singleResult()).isNull();

        // Completing C should make The task active
        cmmnRuntimeService.completeUserEventListenerInstance(userEventListenerInstances.get(2).getId());
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("The task").singleResult()).isNotNull();

        userEventListenerInstances = cmmnRuntimeService.createUserEventListenerInstanceQuery()
                .caseInstanceId(caseInstance.getId()).orderByName().asc().list();
        assertThat(userEventListenerInstances)
                .extracting(UserEventListenerInstance::getName)
                .containsExactly("A", "B");
    }

    @Test
    @CmmnDeployment
    public void testTimerAndUserEventListenerForEntry() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testTimerAndUserEventListenerForEntry").start();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(4);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.TIMER_EVENT_LISTENER).singleResult())
                .isNotNull();
        assertThat(cmmnTaskService.createTaskQuery().taskName("A").singleResult()).isNull();
        assertThat(cmmnManagementService.createTimerJobQuery().caseInstanceId(caseInstance.getId()).singleResult()).isNotNull();

        // Completing the user event listener should terminate the timer event listener
        UserEventListenerInstance userEventListenerInstance = cmmnRuntimeService.createUserEventListenerInstanceQuery().caseInstanceId(caseInstance.getId())
                .singleResult();
        cmmnRuntimeService.completeUserEventListenerInstance(userEventListenerInstance.getId());
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(2);
        assertThat(cmmnTaskService.createTaskQuery().taskName("A").singleResult()).isNotNull();

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.USER_EVENT_LISTENER).singleResult()).isNull();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.TIMER_EVENT_LISTENER).singleResult())
                .isNull();
        assertThat(cmmnManagementService.createTimerJobQuery().caseInstanceId(caseInstance.getId()).singleResult()).isNull();
    }

    @Test
    @CmmnDeployment
    public void testUserEventNotUsedForExit() {

        // This case definition has a stage with two tasks. An exit sentry with a user event exists for that stage.
        // This tests checks that, if the stage completes normally, the user event listener is removed as it becomes an orphan

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testUserEventNotUsedForExit").start();
        assertThat(cmmnRuntimeService.createUserEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);

        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("A", "B");
        tasks.forEach(task -> cmmnTaskService.complete(task.getId()));

        Task taskC = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(taskC.getName()).isEqualTo("C");

        assertThat(cmmnRuntimeService.createUserEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

    }

    @Test
    @CmmnDeployment
    public void testNestedUserEventListener() {

        // This case definition has user event listener connected to a task on the same level and a deeply nested task.
        // The deeply nested task hasn't been made available yet, but the event listener should not be deleted as it could still be made active later

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testNestedUserEventListener").start();
        assertThat(cmmnRuntimeService.createUserEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);

        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("D");

        // Complete the task on the same level should not trigger the deletion of the user event listener
        cmmnTaskService.complete(tasks.get(0).getId());
        assertThat(cmmnRuntimeService.createUserEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);

        // Setting the variable that guards the stage will make the nested task active
        cmmnRuntimeService.setVariable(caseInstance.getId(), "activateStage", true);
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("A", "B", "C");

        // Completing task C should remove the user event listener
        cmmnTaskService.complete(tasks.get(2).getId());
        assertThat(cmmnRuntimeService.createUserEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
    }

    @Test
    @CmmnDeployment
    public void testUserEventListenerForEntryAndExit() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testUserEventListenerForEntryAndExit").start();
        List<UserEventListenerInstance> userEventListenerInstances = cmmnRuntimeService.createUserEventListenerInstanceQuery()
                .caseInstanceId(caseInstance.getId()).orderByName().asc().list();
        assertThat(userEventListenerInstances)
                .extracting(UserEventListenerInstance::getName)
                .containsExactly("EventListenerA", "EventListenerB");

        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks).isEmpty();

        // Completing event listener A will activate task A and task B. User event listener B becomes obsolete is gets removed.
        cmmnRuntimeService.completeUserEventListenerInstance(userEventListenerInstances.get(0).getId());
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("Task A", "Task B");
        assertThat(cmmnRuntimeService.createUserEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).list()).isEmpty();
    }

    @Test
    @CmmnDeployment
    public void testUserEventListenerInNonAutoCompletableStage() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("tesUserEventListenerInNonAutoCompletableStage").start();

        UserEventListenerInstance userEventListenerInstance = cmmnRuntimeService.createUserEventListenerInstanceQuery().caseInstanceId(caseInstance.getId())
                .singleResult();
        cmmnRuntimeService.completeUserEventListenerInstance(userEventListenerInstance.getId());

        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        for (Task task : tasks) {
            cmmnTaskService.complete(task.getId());
        }
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testUserEventListenerInAutoCompletableStage() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testUserEventListenerInAutoCompletableStage").start();

        // B is the only required now. So completing it, should delete the event listener
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testRequiredUserEventListenerInAutoCompletableStage() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testRequiredUserEventListenerInAutoCompletableStage")
                .start();
        UserEventListenerInstance userEventListenerInstance = cmmnRuntimeService.createUserEventListenerInstanceQuery().caseInstanceId(caseInstance.getId())
                .singleResult();
        cmmnRuntimeService.completeUserEventListenerInstance(userEventListenerInstance.getId());
        assertCaseInstanceEnded(caseInstance);
    }

}

