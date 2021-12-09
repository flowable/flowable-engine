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

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.GenericEventListenerInstance;
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.task.api.Task;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public class GenericEventListenerTest extends FlowableCmmnTestCase {

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
                        tuple(PlanItemDefinitionType.GENERIC_EVENT_LISTENER, "eventListener", PlanItemInstanceState.AVAILABLE),
                        tuple(PlanItemDefinitionType.HUMAN_TASK, "taskA", PlanItemInstanceState.ACTIVE),
                        tuple(PlanItemDefinitionType.HUMAN_TASK, "taskB", PlanItemInstanceState.AVAILABLE)
                );

        //1 User Event Listener
        PlanItemInstance listenerInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemDefinitionType(PlanItemDefinitionType.GENERIC_EVENT_LISTENER).singleResult();
        assertThat(listenerInstance).isNotNull();
        assertThat(listenerInstance.getPlanItemDefinitionId()).isEqualTo("eventListener");
        assertThat(listenerInstance.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);

        // Verify same result is returned from query
        GenericEventListenerInstance eventListenerInstance = cmmnRuntimeService.createGenericEventListenerInstanceQuery().caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(eventListenerInstance).isNotNull();
        assertThat(listenerInstance.getId()).isEqualTo(eventListenerInstance.getId());
        assertThat(listenerInstance.getCaseDefinitionId()).isEqualTo(eventListenerInstance.getCaseDefinitionId());
        assertThat(listenerInstance.getCaseInstanceId()).isEqualTo(eventListenerInstance.getCaseInstanceId());
        assertThat(listenerInstance.getElementId()).isEqualTo(eventListenerInstance.getElementId());
        assertThat(listenerInstance.getName()).isEqualTo(eventListenerInstance.getName());
        assertThat(listenerInstance.getPlanItemDefinitionId()).isEqualTo(eventListenerInstance.getPlanItemDefinitionId());
        assertThat(listenerInstance.getStageInstanceId()).isEqualTo(eventListenerInstance.getStageInstanceId());
        assertThat(listenerInstance.getState()).isEqualTo(eventListenerInstance.getState());

        assertThat(cmmnRuntimeService.createGenericEventListenerInstanceQuery().count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createGenericEventListenerInstanceQuery().list()).hasSize(1);

        assertThat(cmmnRuntimeService.createGenericEventListenerInstanceQuery().caseDefinitionId(listenerInstance.getCaseDefinitionId()).singleResult())
                .isNotNull();

        //2 HumanTasks ... one active and other waiting (available)
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK).count()).isEqualTo(2);
        PlanItemInstance active = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .planItemInstanceStateActive()
                .singleResult();
        assertThat(active).isNotNull();
        assertThat(active.getPlanItemDefinitionId()).isEqualTo("taskA");
        PlanItemInstance available = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .planItemInstanceStateAvailable()
                .singleResult();
        assertThat(available).isNotNull();
        assertThat(available.getPlanItemDefinitionId()).isEqualTo("taskB");

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().list())
                    .extracting(HistoricPlanItemInstance::getPlanItemDefinitionType, HistoricPlanItemInstance::getPlanItemDefinitionId, HistoricPlanItemInstance::getState)
                    .containsExactlyInAnyOrder(
                            tuple(PlanItemDefinitionType.GENERIC_EVENT_LISTENER, "eventListener", PlanItemInstanceState.AVAILABLE),
                            tuple(PlanItemDefinitionType.HUMAN_TASK, "taskA", PlanItemInstanceState.ACTIVE),
                            tuple(PlanItemDefinitionType.HUMAN_TASK, "taskB", PlanItemInstanceState.AVAILABLE)
                    );
        }

        //Trigger the listener
        cmmnRuntimeService.completeGenericEventListenerInstance(listenerInstance.getId());

        //UserEventListener should be completed
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.GENERIC_EVENT_LISTENER).count()).isZero();

        //Only 2 PlanItems left
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().list()).hasSize(2);

        //Both Human task should be "active" now, as the sentry kicks on the UserEventListener transition
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .planItemInstanceStateActive()
                .count())
                .isEqualTo(2);

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
                            tuple(PlanItemDefinitionType.GENERIC_EVENT_LISTENER, "eventListener", PlanItemInstanceState.COMPLETED),
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
        //Test case where the EventListener is used to complete (ExitCriteria) of a task
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey(name.getMethodName()).start();
        assertThat(caseInstance).isNotNull();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isEqualTo(1);

        //4 PlanItems reachable
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().list()).hasSize(4);

        //1 Stage
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.STAGE).count()).isEqualTo(1);

        //1 Event Listener
        PlanItemInstance listenerInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemDefinitionType(PlanItemDefinitionType.GENERIC_EVENT_LISTENER).singleResult();
        assertThat(listenerInstance).isNotNull();
        assertThat(listenerInstance.getPlanItemDefinitionId()).isEqualTo("eventListener");
        assertThat(listenerInstance.getElementId()).isEqualTo("eventListenerPlanItem");
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
        //Test case where the GenericEventListener is used to complete (ExitCriteria) of a Stage
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey(name.getMethodName()).start();
        assertThat(caseInstance).isNotNull();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isEqualTo(1);

        //3 PlanItems reachable
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().list()).hasSize(3);

        //1 Stage
        PlanItemInstance stage = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.STAGE).singleResult();
        assertThat(stage).isNotNull();

        //1 User Event Listener
        PlanItemInstance listenerInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemDefinitionType(PlanItemDefinitionType.GENERIC_EVENT_LISTENER).singleResult();
        assertThat(listenerInstance).isNotNull();
        assertThat(listenerInstance.getPlanItemDefinitionId()).isEqualTo("eventListener");
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
    public void testGenericEventListenerInstanceQuery() {
        //Test for EventListenerInstanceQuery
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey(name.getMethodName())
                .start();
        assertThat(caseInstance).isNotNull();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isEqualTo(1);

        //All planItemInstances
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isEqualTo(8);

        //UserEventListenerInstances
        assertThat(cmmnRuntimeService.createGenericEventListenerInstanceQuery().stateAvailable().count()).isEqualTo(6);

        List<GenericEventListenerInstance> events = cmmnRuntimeService.createGenericEventListenerInstanceQuery().list();
        assertThat(events).hasSize(6);

        //All different Instances id's
        assertThat(events.stream().map(GenericEventListenerInstance::getId).distinct().count()).isEqualTo(6);

        //UserEventListenerInstances inside Stage1
        String stage1Id = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.STAGE).planItemDefinitionId("stage1")
                .singleResult().getId();
        assertThat(cmmnRuntimeService.createGenericEventListenerInstanceQuery().stageInstanceId(stage1Id).count()).isEqualTo(2);

        //UserEventListenerInstances inside Stage2
        String stage2Id = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.STAGE).planItemDefinitionId("stage2")
                .singleResult().getId();
        assertThat(cmmnRuntimeService.createGenericEventListenerInstanceQuery().stageInstanceId(stage2Id).count()).isEqualTo(2);

        //UserEventListenerInstances not in a Stage
        assertThat(events.stream().filter(e -> e.getStageInstanceId() == null).count()).isEqualTo(2);

        //Test query by elementId
        assertThat(cmmnRuntimeService.createGenericEventListenerInstanceQuery().elementId("caseEventListenerOne").singleResult()).isNotNull();

        //Test query by planItemDefinitionId
        assertThat(cmmnRuntimeService.createGenericEventListenerInstanceQuery().planItemDefinitionId("caseUEL1").count()).isEqualTo(2);

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
        assertThat(cmmnRuntimeService.createGenericEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult()).isNotNull();
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("A", "C");

        // Completing one tasks should not have impact on the event listener
        cmmnTaskService.complete(tasks.get(0).getId());
        assertThat(cmmnRuntimeService.createGenericEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult()).isNotNull();
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("C");

        // Completing the other task with the exit sentry should still keep the event, as it's reference by the entry of B
        cmmnTaskService.complete(tasks.get(0).getId());
        assertThat(cmmnRuntimeService.createGenericEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult()).isNotNull();
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        // Firing the event listener should start B
        cmmnRuntimeService.completeGenericEventListenerInstance(
                cmmnRuntimeService.createGenericEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getId());
        assertThat(cmmnRuntimeService.createGenericEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult()).isNull();
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
    }

    @Test
    @CmmnDeployment
    public void testMultipleEventListenersAsEntry() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testMultipleEventListenersAsEntry").start();
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        List<GenericEventListenerInstance> eventListenerInstances = cmmnRuntimeService.createGenericEventListenerInstanceQuery()
                .caseInstanceId(caseInstance.getId()).orderByName().asc().list();
        assertThat(eventListenerInstances)
                .extracting(GenericEventListenerInstance::getName)
                .containsExactly("A", "B", "C");

        // Completing A should change nothing
        cmmnRuntimeService.completeGenericEventListenerInstance(eventListenerInstances.get(0).getId());
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
        eventListenerInstances = cmmnRuntimeService.createGenericEventListenerInstanceQuery()
                .caseInstanceId(caseInstance.getId()).orderByName().asc().list();
        assertThat(eventListenerInstances)
                .extracting(GenericEventListenerInstance::getName)
                .containsExactly("B", "C");

        // Completing B should activate the stage and remove the orphan event listener C
        cmmnRuntimeService.completeGenericEventListenerInstance(eventListenerInstances.get(0).getId());
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createGenericEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
    }

    @Test
    @CmmnDeployment
    public void testWithRepetitionMultiple() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testRepetition")
                .variable("keepRepeating", true)
                .start();

        PlanItemInstance eventInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemDefinitionType(PlanItemDefinitionType.GENERIC_EVENT_LISTENER)
                .singleResult();
        cmmnRuntimeService.triggerPlanItemInstance(eventInstance.getId());
        
        cmmnRuntimeService.setVariable(caseInstance.getId(), "keepRepeating", false);

        cmmnTaskService.complete(cmmnTaskService.createTaskQuery().taskDefinitionKey("taskA").active().singleResult().getId());
        
        eventInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemDefinitionType(PlanItemDefinitionType.GENERIC_EVENT_LISTENER)
                .singleResult();
        cmmnRuntimeService.triggerPlanItemInstance(eventInstance.getId());
        
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
        
        assertCaseInstanceEnded(caseInstance);
    }
    
    @Test
    @CmmnDeployment
    public void testWithRepetitionParallel() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testRepetition")
                .variable("keepRepeating", true)
                .start();

        cmmnEngineConfiguration.getCommandExecutor().execute(new Command<Void>() {

            @Override
            public Void execute(CommandContext commandContext) {
                PlanItemInstance eventInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                        .planItemDefinitionType(PlanItemDefinitionType.GENERIC_EVENT_LISTENER)
                        .singleResult();
                cmmnRuntimeService.triggerPlanItemInstance(eventInstance.getId());
                
                cmmnRuntimeService.setVariable(caseInstance.getId(), "keepRepeating", false);
                
                eventInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                        .planItemDefinitionType(PlanItemDefinitionType.GENERIC_EVENT_LISTENER)
                        .singleResult();
                cmmnRuntimeService.triggerPlanItemInstance(eventInstance.getId());
                
                return null;
            }
            
        });
        
        cmmnTaskService.complete(cmmnTaskService.createTaskQuery().taskDefinitionKey("taskA").active().singleResult().getId());
        
        assertCaseInstanceEnded(caseInstance);
    }

}

