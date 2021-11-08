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
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.api.listener.PlanItemInstanceLifecycleListener;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.GenericEventListenerInstance;
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.api.runtime.UserEventListenerInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.engine.test.impl.CmmnJobTestHelper;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class RepetitionRuleTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment
    public void testSimpleRepeatingTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("repeatingTask").start();
        for (int i = 0; i < 5; i++) {
            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertThat(task).as("No task found for index " + i).isNotNull();
            assertThat(task.getName()).isEqualTo("My Task");
            cmmnTaskService.complete(task.getId());
        }
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testCustomCounterVariable() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("repeatingTask").start();
        for (int i = 0; i < 10; i++) {
            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertThat(task).as("No task found for index " + i).isNotNull();
            assertThat(task.getName()).isEqualTo("My Task");
            cmmnTaskService.complete(task.getId());
        }
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testRepeatingStage() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testRepeatingStage").start();
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("A", "Task outside stage");

        // Stage is repeated 3 times
        for (int i = 0; i < 3; i++) {
            cmmnTaskService.complete(tasks.get(0).getId()); // Completing A will make B and C active
            tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
            assertThat(tasks)
                    .extracting(Task::getName)
                    .containsExactly("B", "C", "Task outside stage");

            // Completing B and C should lead to a repetition of the stage
            cmmnTaskService.complete(tasks.get(0).getId()); // B
            cmmnTaskService.complete(tasks.get(1).getId()); // C

            tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        }

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Task outside stage");
        cmmnTaskService.complete(task.getId());

        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testNestedRepeatingStage() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testNestedRepeatingStage").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("A");

        // Completing A should:
        // - create a new instance of A (A is repeating)
        // - activate B

        cmmnTaskService.complete(task.getId());
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("A", "B");

        // Complete A should have no impact on the repeating of the nested stage3
        cmmnTaskService.complete(tasks.get(0).getId());
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("A", "B");

        // A is repeated 3 times
        cmmnTaskService.complete(tasks.get(0).getId());
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("B");

        // Completing B should activate C
        cmmnTaskService.complete(task.getId());
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("C");

        // Completing C should repeat the nested stage and activate B again
        cmmnTaskService.complete(task.getId());
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("B");
        cmmnTaskService.complete(task.getId());
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("C");

        // Completing C should end the case instance
        cmmnTaskService.complete(task.getId());
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testRepeatingTimer() {
        Date currentTime = setClockFixedToCurrentTime();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testRepeatingTimer").start();

        // Should have the task plan item state available
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .singleResult();
        assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);

        // Task should not be created yet
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        // And one for the timer event listener
        planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.TIMER_EVENT_LISTENER).singleResult();
        assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);

        // Should have a timer job available
        Job job = cmmnManagementService.createTimerJobQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(job).isNotNull();

        // Moving the timer 1 hour ahead, should create a task instance. 
        currentTime = new Date(currentTime.getTime() + (60 * 60 * 1000) + 10);
        setClockTo(currentTime);
        job = cmmnManagementService.moveTimerToExecutableJob(job.getId());
        cmmnManagementService.executeJob(job.getId());
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);

        // A plan item in state 'waiting for repetition' should exist for the task
        planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .planItemInstanceStateWaitingForRepetition()
                .singleResult();
        assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.WAITING_FOR_REPETITION);

        // This can be repeated forever
        for (int i = 0; i < 10; i++) {
            currentTime = new Date(currentTime.getTime() + (60 * 60 * 1000) + 10);
            setClockTo(currentTime);
            job = cmmnManagementService.createTimerJobQuery().caseInstanceId(caseInstance.getId()).singleResult();
            job = cmmnManagementService.moveTimerToExecutableJob(job.getId());
            cmmnManagementService.executeJob(job.getId());
            assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(i + 2);
        }

        // Completing all the tasks should still keep the case instance running
        for (Task task : cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list()) {
            cmmnTaskService.complete(task.getId());
        }
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isEqualTo(1);
        // There should also still be a plan item instance in the 'wait for repetition' state
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .planItemInstanceStateWaitingForRepetition()
                .singleResult()).isNotNull();

        // Terminating the case instance should remove the timer
        cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();
        assertThat(cmmnManagementService.createTimerJobQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
        cmmnEngineConfiguration.resetClock();
    }
    
    @Test
    @CmmnDeployment
    public void testRepeatingTimerWithCategory() {
        Date currentTime = setClockFixedToCurrentTime();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testRepeatingTimer").start();

        // Task should not be created yet
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        // Should have a timer job available
        Job job = cmmnManagementService.createTimerJobQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(job).isNotNull();
        assertThat(job.getCategory()).isEqualTo("myCategory");

        // Moving the timer 1 hour ahead, should create a task instance. 
        currentTime = new Date(currentTime.getTime() + (60 * 60 * 1000) + 10);
        setClockTo(currentTime);
        job = cmmnManagementService.moveTimerToExecutableJob(job.getId());
        cmmnManagementService.executeJob(job.getId());
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);

        // A plan item in state 'waiting for repetition' should exist for the task
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .planItemInstanceStateWaitingForRepetition()
                .singleResult();
        assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.WAITING_FOR_REPETITION);

        // This can be repeated forever
        for (int i = 0; i < 10; i++) {
            currentTime = new Date(currentTime.getTime() + (60 * 60 * 1000) + 10);
            setClockTo(currentTime);
            job = cmmnManagementService.createTimerJobQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertThat(job.getCategory()).isEqualTo("myCategory");
            job = cmmnManagementService.moveTimerToExecutableJob(job.getId());
            cmmnManagementService.executeJob(job.getId());
            assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(i + 2);
        }

        // Completing all the tasks should still keep the case instance running
        for (Task task : cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list()) {
            cmmnTaskService.complete(task.getId());
        }
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isEqualTo(1);
        // There should also still be a plan item instance in the 'wait for repetition' state
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .planItemInstanceStateWaitingForRepetition()
                .singleResult()).isNotNull();

        // Terminating the case instance should remove the timer
        cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();
        assertThat(cmmnManagementService.createTimerJobQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
        cmmnEngineConfiguration.resetClock();
    }
    
    @Test
    @CmmnDeployment
    public void testRepeatingTimerWithCategoryExpression() {
        Date currentTime = setClockFixedToCurrentTime();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testRepeatingTimer")
                .variable("categoryValue", "testValue")
                .start();

        // Task should not be created yet
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        // Should have a timer job available
        Job job = cmmnManagementService.createTimerJobQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(job).isNotNull();
        assertThat(job.getCategory()).isEqualTo("testValue");

        // Moving the timer 1 hour ahead, should create a task instance. 
        currentTime = new Date(currentTime.getTime() + (60 * 60 * 1000) + 10);
        setClockTo(currentTime);
        job = cmmnManagementService.moveTimerToExecutableJob(job.getId());
        cmmnManagementService.executeJob(job.getId());
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);

        // A plan item in state 'waiting for repetition' should exist for the task
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .planItemInstanceStateWaitingForRepetition()
                .singleResult();
        assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.WAITING_FOR_REPETITION);

        // This can be repeated forever
        for (int i = 0; i < 10; i++) {
            currentTime = new Date(currentTime.getTime() + (60 * 60 * 1000) + 10);
            setClockTo(currentTime);
            job = cmmnManagementService.createTimerJobQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertThat(job.getCategory()).isEqualTo("testValue");
            job = cmmnManagementService.moveTimerToExecutableJob(job.getId());
            cmmnManagementService.executeJob(job.getId());
            assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(i + 2);
        }

        // Completing all the tasks should still keep the case instance running
        for (Task task : cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list()) {
            cmmnTaskService.complete(task.getId());
        }
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isEqualTo(1);
        // There should also still be a plan item instance in the 'wait for repetition' state
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .planItemInstanceStateWaitingForRepetition()
                .singleResult()).isNotNull();

        // Terminating the case instance should remove the timer
        cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();
        assertThat(cmmnManagementService.createTimerJobQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
        cmmnEngineConfiguration.resetClock();
    }

    @Test
    @CmmnDeployment
    public void testRepeatingTimerWithCronExpression() {
        Date currentTime = setClockFixedToCurrentTime();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testRepeatingTimer").start();

        // Moving the timer 6 minutes should trigger the timer
        for (int i = 0; i < 3; i++) {
            currentTime = new Date(currentTime.getTime() + (6 * 60 * 1000));
            setClockTo(currentTime);

            Job job = cmmnManagementService.createTimerJobQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertThat(job.getDuedate().getTime() - currentTime.getTime()).isLessThanOrEqualTo(5 * 60 * 1000);
            job = cmmnManagementService.moveTimerToExecutableJob(job.getId());
            cmmnManagementService.executeJob(job.getId());

            assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(i + 1);
        }
    }

    @Test
    @CmmnDeployment
    public void testLimitedRepeatingTimer() {
        Date currentTime = setClockFixedToCurrentTime();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testLimitedRepeatingTimer").start();

        currentTime = new Date(currentTime.getTime() + (5 * 60 * 60 * 1000) + 10000);
        setClockTo(currentTime);

        Job job = cmmnManagementService.createTimerJobQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(job.getDuedate().getTime() - currentTime.getTime()).isLessThanOrEqualTo(5 * 60 * 1000);
        job = cmmnManagementService.moveTimerToExecutableJob(job.getId());
        cmmnManagementService.executeJob(job.getId());
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);

        // new timer should be scheduled
        assertThat(cmmnManagementService.createTimerJobQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);

        // Should only repeat two times
        currentTime = new Date(currentTime.getTime() + (5 * 60 * 60 * 1000) + 10000);
        setClockTo(currentTime);
        CmmnJobTestHelper.waitForJobExecutorToProcessAllJobs(cmmnEngineConfiguration, 10000L, 100L, true);

        assertThat(cmmnManagementService.createTimerJobQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
        assertThat(cmmnManagementService.createJobQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .planItemInstanceStateWaitingForRepetition()
                .singleResult()).isNotNull();

        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).hasSize(2);
        for (Task task : tasks) {
            cmmnTaskService.complete(task.getId());
        }

        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testLimitedRepeatingTimerIgnoredAfterFirst() {

        // No repetition rule for task A, hence only the first one will be listened too.

        Date currentTime = setClockFixedToCurrentTime();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testLimitedRepeatingTimer").start();

        currentTime = new Date(currentTime.getTime() + (5 * 60 * 60 * 1000) + 10000);
        setClockTo(currentTime);

        Job job = cmmnManagementService.createTimerJobQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(job.getDuedate().getTime() - currentTime.getTime()).isLessThanOrEqualTo(5 * 60 * 1000);
        job = cmmnManagementService.moveTimerToExecutableJob(job.getId());
        cmmnManagementService.executeJob(job.getId());
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);

        // new timer should NOT be scheduled. The orphan detection algorithm will take in account the waiting for repetition state and the fact its missing here
        assertThat(cmmnManagementService.createTimerJobQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        assertThat(cmmnManagementService.createJobQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        // Ignoring second occur event
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
    }

    @Test
    @CmmnDeployment
    public void testRepetitionRuleWithExitCriteria() {
        //Completion of taskB will transition taskA to "exit", skipping the evaluation of the repetition rule (Table 8.8 of CMM 1.1 Spec)
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testRepetitionRuleWithExitCriteria")
                .variable("whileTrue", "true")
                .start();

        assertThat(caseInstance).isNotNull();

        for (int i = 0; i < 3; i++) {
            Task taskA = cmmnTaskService.createTaskQuery().active().taskDefinitionKey("taskA").singleResult();
            cmmnTaskService.complete(taskA.getId());
            assertCaseInstanceNotEnded(caseInstance);
        }

        Task taskB = cmmnTaskService.createTaskQuery().active().taskDefinitionKey("taskB").singleResult();
        cmmnTaskService.complete(taskB.getId());
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testRepeatingEventListener() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testRepeatingUserEventListener").start();
        assertThat(cmmnTaskService.createTaskQuery().count()).isZero();

        for (int i = 0; i < 17; i++) {
            GenericEventListenerInstance genericEventListenerInstance = cmmnRuntimeService.createGenericEventListenerInstanceQuery()
                    .caseInstanceId(caseInstance.getId()).singleResult();
            assertThat(genericEventListenerInstance.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
            cmmnRuntimeService.completeGenericEventListenerInstance(genericEventListenerInstance.getId());
        }
        assertThat(cmmnTaskService.createTaskQuery().count()).isEqualTo(17L);
    }

    @Test
    @CmmnDeployment
    public void testRepeatingRuleUserEventListener() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testRepeatingUserEventListener")
                .variable("keepGoing", true)
                .start();
        assertThat(cmmnTaskService.createTaskQuery().count()).isZero();

        for (int i = 0; i < 3; i++) {
            UserEventListenerInstance userEventListenerInstance = cmmnRuntimeService.createUserEventListenerInstanceQuery()
                    .caseInstanceId(caseInstance.getId()).singleResult();
            assertThat(userEventListenerInstance.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);

            if (i == 2) {
                cmmnRuntimeService.setVariable(caseInstance.getId(), "keepGoing", false);
            }
            cmmnRuntimeService.completeGenericEventListenerInstance(userEventListenerInstance.getId());
        }

        assertThat(cmmnTaskService.createTaskQuery().count()).isEqualTo(3);
    }

    @Test
    @CmmnDeployment
    public void testPlanItemLocalVariablesWithCollection() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("repeatingTask")
                .transientVariable("myCollection", Arrays.asList("one", "two", "three"))
                .start();

        List<Task> tasks = cmmnTaskService.createTaskQuery()
                .orderByTaskPriority().asc()
                .list();

        assertThat(tasks).hasSize(3);

        assertThat(cmmnTaskService.getVariables(tasks.get(0).getId()))
                .containsOnly(
                        entry("repetitionCounter", 1),
                        entry("item", "one"),
                        entry("itemIndex", 0),
                        entry("initiator", null)
                );

        assertThat(cmmnTaskService.getVariables(tasks.get(1).getId()))
                .containsOnly(
                        entry("repetitionCounter", 2),
                        entry("item", "two"),
                        entry("itemIndex", 1),
                        entry("initiator", null)
                );

        assertThat(cmmnTaskService.getVariables(tasks.get(2).getId()))
                .containsOnly(
                        entry("repetitionCounter", 3),
                        entry("item", "three"),
                        entry("itemIndex", 2),
                        entry("initiator", null)
                );
    }

    @Test
    @CmmnDeployment
    public void testPlanItemLocalVariables() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("repeatingTask")
                .start();

        Task task = cmmnTaskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();

        assertThat(cmmnTaskService.getVariables(task.getId()))
                .containsOnly(
                        entry("repetitionCounter", 1),
                        entry("initiator", null)
                );

        cmmnTaskService.complete(task.getId());

        task = cmmnTaskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();

        assertThat(cmmnTaskService.getVariables(task.getId()))
                .containsOnly(
                        entry("repetitionCounter", 2),
                        entry("initiator", null)
                );

        cmmnTaskService.complete(task.getId());

        task = cmmnTaskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();

        assertThat(cmmnTaskService.getVariables(task.getId()))
                .containsOnly(
                        entry("repetitionCounter", 3),
                        entry("initiator", null)
                );

        cmmnTaskService.complete(task.getId());

        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testPlanItemInstanceLifecycleListener() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("repeatingTask").start();
        for (int i = 0; i < 10; i++) {

            TestPlanItemInstanceLifecycleListener.stateChanges.clear();

            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertThat(task).as("No task found for index " + i).isNotNull();
            assertThat(task.getName()).isEqualTo("My Task " + i);
            cmmnTaskService.complete(task.getId());

            assertThat(TestPlanItemInstanceLifecycleListener.stateChanges)
                .extracting(Pair::getKey, Pair::getValue)
                .containsExactly(
                    tuple(PlanItemInstanceState.ACTIVE, PlanItemInstanceState.COMPLETED),
                    tuple(null, PlanItemInstanceState.WAITING_FOR_REPETITION),
                    tuple(PlanItemInstanceState.WAITING_FOR_REPETITION, PlanItemInstanceState.ACTIVE)
                );
            TestPlanItemInstanceLifecycleListener.stateChanges.clear();
        }
    }

    public static class TestPlanItemInstanceLifecycleListener implements PlanItemInstanceLifecycleListener {

        public static List<Pair<String, String>> stateChanges = new ArrayList<>();

        @Override
        public String getSourceState() {
            return null;
        }

        @Override
        public String getTargetState() {
            return null;
        }

        @Override
        public void stateChanged(DelegatePlanItemInstance planItemInstance, String oldState, String newState) {
            stateChanges.add(Pair.of(oldState, newState));
        }

    }

}
