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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
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
        for (int i=0; i<5; i++) {
            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertNotNull("No task found for index " + i, task);
            assertEquals("My Task", task.getName());
            cmmnTaskService.complete(task.getId());
        }
        assertCaseInstanceEnded(caseInstance);
    }
    
    @Test
    @CmmnDeployment
    public void testCustomCounterVariable() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("repeatingTask").start();
        for (int i=0; i<10; i++) {
            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertNotNull("No task found for index " + i, task);
            assertEquals("My Task", task.getName());
            cmmnTaskService.complete(task.getId());
        }
        assertCaseInstanceEnded(caseInstance);
    }
    
    @Test
    @CmmnDeployment
    public void testRepeatingStage() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testRepeatingStage").start();
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertEquals(2, tasks.size());
        assertEquals("A", tasks.get(0).getName());
        assertEquals("Task outside stage", tasks.get(1).getName());
        
        // Stage is repeated 3 times
        for (int i=0; i<3; i++) {
            cmmnTaskService.complete(tasks.get(0).getId()); // Completing A will make B and C active
            tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
            assertEquals(3, tasks.size());
            assertEquals("B", tasks.get(0).getName());
            assertEquals("C", tasks.get(1).getName());
            assertEquals("Task outside stage", tasks.get(2).getName());
            
            // Completing B and C should lead to a repetition of the stage
            cmmnTaskService.complete(tasks.get(0).getId()); // B
            cmmnTaskService.complete(tasks.get(1).getId()); // C
            
            tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        }
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("Task outside stage", task.getName());
        cmmnTaskService.complete(task.getId());
        
        assertCaseInstanceEnded(caseInstance);
    }
    
    @Test
    @CmmnDeployment
    public void testNestedRepeatingStage() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testNestedRepeatingStage").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("A", task.getName());
        
        // Completing A should:
        // - create a new instance of A (A is repeating)
        // - activate B
        
        cmmnTaskService.complete(task.getId());
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertEquals(2, tasks.size());
        assertEquals("A", tasks.get(0).getName());
        assertEquals("B", tasks.get(1).getName());
        
        // Complete A should have no impact on the repeating of the nested stage3
        cmmnTaskService.complete(tasks.get(0).getId());
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertEquals(2, tasks.size());
        assertEquals("A", tasks.get(0).getName());
        assertEquals("B", tasks.get(1).getName());
        
        // A is repeated 3 times
        cmmnTaskService.complete(tasks.get(0).getId());
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("B", task.getName());
        
        // Completing B should activate C
        cmmnTaskService.complete(task.getId());
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("C", task.getName());
        
        // Completing C should repeat the nested stage and activate B again
        cmmnTaskService.complete(task.getId());
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("B", task.getName());
        cmmnTaskService.complete(task.getId());
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("C", task.getName());
        
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
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK).singleResult();
        assertEquals(PlanItemInstanceState.AVAILABLE, planItemInstance.getState());
        
        // Task should not be created yet
        assertEquals(0L, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        // And one for the timer event listener
        planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.TIMER_EVENT_LISTENER).singleResult();
        assertEquals(PlanItemInstanceState.AVAILABLE, planItemInstance.getState());
        
        // Should have a timer job available
        Job job = cmmnManagementService.createTimerJobQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertNotNull(job);
        
        // Moving the timer 1 hour ahead, should create a task instance. 
        currentTime = new Date(currentTime.getTime() + (60 * 60 * 1000) + 10);
        setClockTo(currentTime);
        job = cmmnManagementService.moveTimerToExecutableJob(job.getId());
        cmmnManagementService.executeJob(job.getId());
        assertEquals(1L, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        // A plan item in state 'waiting for repetition' should exist for the yask
        planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .planItemInstanceStateWaitingForRepetition()
                .singleResult();
        assertEquals(PlanItemInstanceState.WAITING_FOR_REPETITION, planItemInstance.getState());
        
        // This can be repeated forever
        for (int i=0; i<10; i++) {
            currentTime = new Date(currentTime.getTime() + (60 * 60 * 1000) + 10);
            setClockTo(currentTime);
            job = cmmnManagementService.createTimerJobQuery().caseInstanceId(caseInstance.getId()).singleResult();
            job = cmmnManagementService.moveTimerToExecutableJob(job.getId());
            cmmnManagementService.executeJob(job.getId());
            assertEquals(i + 2, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        }
        
        // Completing all the tasks should still keep the case instance running
        for (Task task : cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list()) {
            cmmnTaskService.complete(task.getId());
        }
        assertEquals(0L, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        assertEquals(1L, cmmnRuntimeService.createCaseInstanceQuery().count());
        // There should also still be a plan item instance in the 'wait for repetition' state
        assertNotNull(cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .planItemInstanceStateWaitingForRepetition()
                .singleResult());
        
        // Terminating the case instance should remove the timer
        cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());
        assertEquals(0L, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).count());
        assertEquals(0L, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(0L, cmmnManagementService.createTimerJobQuery().caseInstanceId(caseInstance.getId()).count());
    }
    
    @Test
    @CmmnDeployment
    public void testRepeatingTimerWithCronExpression() {
        Date currentTime = setClockFixedToCurrentTime();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testRepeatingTimer").start();
        
        // Moving the timer 6 minutes should trigger the timer
        for (int i=0; i<3; i++) {
            currentTime = new Date(currentTime.getTime() + (6 * 60 * 1000));
            setClockTo(currentTime);
        
            Job job = cmmnManagementService.createTimerJobQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertTrue(job.getDuedate().getTime() - currentTime.getTime() <= (5 * 60 * 1000));
            job = cmmnManagementService.moveTimerToExecutableJob(job.getId());
            cmmnManagementService.executeJob(job.getId());
            
            assertEquals(i + 1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
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
        assertTrue(job.getDuedate().getTime() - currentTime.getTime() <= (5 * 60 * 1000));
        job = cmmnManagementService.moveTimerToExecutableJob(job.getId());
        cmmnManagementService.executeJob(job.getId());
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        // new timer should be scheduled
        assertEquals(1L, cmmnManagementService.createTimerJobQuery().caseInstanceId(caseInstance.getId()).count());
        
        // Should only repeat two times
        currentTime = new Date(currentTime.getTime() + (5 * 60 * 60 * 1000) + 10000);
        setClockTo(currentTime);
        CmmnJobTestHelper.waitForJobExecutorToProcessAllJobs(cmmnEngineConfiguration, 10000L, 100L, true);
        
        assertEquals(0L, cmmnManagementService.createTimerJobQuery().caseInstanceId(caseInstance.getId()).count());
        assertEquals(0L, cmmnManagementService.createJobQuery().caseInstanceId(caseInstance.getId()).count());

        assertNotNull(cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .planItemInstanceStateWaitingForRepetition()
                .singleResult());
        
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertEquals(2, tasks.size());
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
        assertTrue(job.getDuedate().getTime() - currentTime.getTime() <= (5 * 60 * 1000));
        job = cmmnManagementService.moveTimerToExecutableJob(job.getId());
        cmmnManagementService.executeJob(job.getId());
        assertEquals(1, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        // new timer should be scheduled
        assertEquals(1L, cmmnManagementService.createTimerJobQuery().caseInstanceId(caseInstance.getId()).count());
        
        // Should only repeat two times
        currentTime = new Date(currentTime.getTime() + (5 * 60 * 60 * 1000) + 10000);
        setClockTo(currentTime);
        CmmnJobTestHelper.waitForJobExecutorToProcessAllJobs(cmmnEngineConfiguration, 10000L, 100L, true);
        
        assertEquals(0L, cmmnManagementService.createTimerJobQuery().caseInstanceId(caseInstance.getId()).count());
        assertEquals(0L, cmmnManagementService.createJobQuery().caseInstanceId(caseInstance.getId()).count());
        
        // Ignoring second occur event
        assertEquals(1L, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
    }

    @Test
    @CmmnDeployment
    public void testRepetitionRuleWithExitCriteria() {
        //Completion of taskB will transition taskA to "exit", skipping the evaluation of the repetition rule (Table 8.8 of CMM 1.1 Spec)
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testRepetitionRuleWithExitCriteria")
                .variable("whileTrue", "true")
                .start();

        assertNotNull(caseInstance);

        for (int i = 0; i < 3; i++) {
            Task taskA = cmmnTaskService.createTaskQuery().active().taskDefinitionKey("taskA").singleResult();
            cmmnTaskService.complete(taskA.getId());
            assertCaseInstanceNotEnded(caseInstance);
        }

        Task taskB = cmmnTaskService.createTaskQuery().active().taskDefinitionKey("taskB").singleResult();
        cmmnTaskService.complete(taskB.getId());
        assertCaseInstanceEnded(caseInstance);
    }
}
