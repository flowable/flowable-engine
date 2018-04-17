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

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.engine.test.impl.CmmnJobTestHelper;
import org.flowable.cmmn.model.HumanTask;
import org.flowable.cmmn.model.Stage;
import org.flowable.cmmn.model.TimerEventListener;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class TimerEventListenerTest extends FlowableCmmnTestCase {
    
    @Test
    @CmmnDeployment
    public void testTimerExpressionDuration() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testTimerExpression").start();
        assertNotNull(caseInstance);
        assertEquals(1, cmmnRuntimeService.createCaseInstanceQuery().count());
        
        assertNotNull(cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.TIMER_EVENT_LISTENER).planItemInstanceStateAvailable().singleResult());
        assertNotNull(cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK).planItemInstanceStateAvailable().singleResult());
        
        assertEquals(1L, cmmnManagementService.createTimerJobQuery().count());
        assertEquals(1L, cmmnManagementService.createTimerJobQuery().scopeId(caseInstance.getId()).scopeType(ScopeTypes.CMMN).count());
        
        // User task should not be active before the timer triggers
        assertEquals(0L, cmmnTaskService.createTaskQuery().count());
        
        Job timerJob = cmmnManagementService.createTimerJobQuery().scopeDefinitionId(caseInstance.getCaseDefinitionId()).singleResult();
        assertNotNull(timerJob);
        cmmnManagementService.moveTimerToExecutableJob(timerJob.getId());
        cmmnManagementService.executeJob(timerJob.getId());
        
        // User task should be active after the timer has triggered
        assertEquals(1L, cmmnTaskService.createTaskQuery().count());
    }
    
    /**
     * Similar test as #testTimerExpressionDuration but with the real async executor, 
     * instead of manually triggering the timer. 
     */
    @Test
    @CmmnDeployment
    public void testTimerExpressionDurationWithRealAsyncExeutor() {
        Date startTime = setClockFixedToCurrentTime();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testTimerExpression").start();

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.AVAILABLE)
                .planItemDefinitionType(PlanItemDefinitionType.TIMER_EVENT_LISTENER)
                .singleResult();
        assertNotNull(planItemInstance);

        // User task should not be active before the timer triggers
        assertEquals(0L, cmmnTaskService.createTaskQuery().count());

        // Timer fires after 1 hour, so setting it to 1 hours + 1 second
        setClockTo(new Date(startTime.getTime() + (60 * 60 * 1000 + 1)));

        CmmnJobTestHelper.waitForJobExecutorToProcessAllJobs(cmmnEngineConfiguration, 5000L, 200L, true);

        // User task should be active after the timer has triggered 
        assertEquals(1L, cmmnTaskService.createTaskQuery().count());
    }
    
    @Test
    @CmmnDeployment
    public void testStageAfterTimer() {
        Date startTime = setClockFixedToCurrentTime();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testStageAfterTimerEventListener").start();
        
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.AVAILABLE)
                .planItemDefinitionType(PlanItemDefinitionType.TIMER_EVENT_LISTENER)
                .singleResult();
        assertNotNull(planItemInstance);
        
        assertEquals(0L, cmmnTaskService.createTaskQuery().count());
        
        // Timer fires after 1 day, so setting it to 1 day + 1 second
        setClockTo(new Date(startTime.getTime() + (24 * 60 * 60 * 1000 + 1)));
        
        CmmnJobTestHelper.waitForJobExecutorToProcessAllJobs(cmmnEngineConfiguration, 5000L, 200L, true);
        
        // User task should be active after the timer has triggered 
        assertEquals(2L, cmmnTaskService.createTaskQuery().count());
    }
    
    @Test
    @CmmnDeployment
    public void testTimerInStage() {
        Date startTime = setClockFixedToCurrentTime();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testTimerInStage").start();
        
        assertEquals(1L, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceState(PlanItemInstanceState.ACTIVE).count());
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.AVAILABLE)
                .planItemDefinitionType(PlanItemDefinitionType.TIMER_EVENT_LISTENER)
                .singleResult();
        assertNotNull(planItemInstance);
        
        assertEquals(1L, cmmnTaskService.createTaskQuery().count());
        
        // Timer fires after 3 hours, so setting it to 3 hours + 1 second
        setClockTo(new Date(startTime.getTime() + (3 * 60 * 60 * 1000 + 1)));
        
        CmmnJobTestHelper.waitForJobExecutorToProcessAllJobs(cmmnEngineConfiguration, 5000L, 200L, true);
        
        // User task should be active after the timer has triggered
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertEquals(2, tasks.size());
        assertEquals("A", tasks.get(0).getName());
        assertEquals("B", tasks.get(1).getName());
    }
    
    @Test
    @CmmnDeployment
    public void testExitPlanModelOnTimerOccurrence() {
        Date startTime = setClockFixedToCurrentTime();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testStageExitOnTimerOccurrence").start();
        
        assertEquals(3L, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE).planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK).count());
        assertEquals(1L, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.AVAILABLE).planItemDefinitionType(PlanItemDefinitionType.TIMER_EVENT_LISTENER).count());
        assertEquals(1L, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE).planItemDefinitionType(PlanItemDefinitionType.STAGE).count());
        assertEquals(4L, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE).count());
        
        // Timer fires after 24 hours, so setting it to 24 hours + 1 second
        setClockTo(new Date(startTime.getTime() + (24 * 60 * 60 * 1000 + 1)));
        
        CmmnJobTestHelper.waitForJobExecutorToProcessAllJobs(cmmnEngineConfiguration, 5000L, 200L, true);
        
        assertEquals(0L, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).count());
        assertEquals(0L, cmmnRuntimeService.createCaseInstanceQuery().count());
    }
    
    @Test
    @CmmnDeployment
    public void testDateExpression() {
        
        // Timer will fire on 2017-12-05T10:00
        // So moving the clock to the day before
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 2017);
        calendar.set(Calendar.MONTH, 12);
        calendar.set(Calendar.DAY_OF_MONTH, 4);
        calendar.set(Calendar.HOUR, 11);
        calendar.set(Calendar.MINUTE, 0);
        Date dayBefore = calendar.getTime();
        setClockTo(dayBefore);
        
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testDateExpression").start();
        
        assertEquals(1L, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.AVAILABLE).planItemDefinitionType(TimerEventListener.class.getSimpleName().toLowerCase()).count());
        assertEquals(2L, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.AVAILABLE).planItemDefinitionType(Stage.class.getSimpleName().toLowerCase()).count());
        assertEquals(0L, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE).planItemDefinitionType(Stage.class.getSimpleName().toLowerCase()).count());
        assertEquals(0L, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE).planItemDefinitionType(HumanTask.class.getSimpleName().toLowerCase()).count());
        assertEquals(1L, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.AVAILABLE).planItemDefinitionType(HumanTask.class.getSimpleName().toLowerCase()).count());
        
        assertEquals(0L, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        setClockTo(new Date(dayBefore.getTime() + (24 * 60 * 60 * 1000)));
        CmmnJobTestHelper.waitForJobExecutorToProcessAllJobs(cmmnEngineConfiguration, 5000L, 200L, true);
        
        assertEquals(0L, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE).planItemDefinitionType(TimerEventListener.class.getSimpleName().toLowerCase()).count());
        
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertEquals(3, tasks.size());
        for (Task task : tasks) {
            cmmnTaskService.complete(task.getId());
        }
        
        assertCaseInstanceEnded(caseInstance);
    }
    
    @Test
    @CmmnDeployment
    public void testTimerWithBeanExpression() {
        Date startTime = setClockFixedToCurrentTime();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testBean")
                .variable("startTime", startTime)
                .start();
        
        assertEquals(2L, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        assertEquals(2L, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE).planItemDefinitionType(Stage.class.getSimpleName().toLowerCase()).count());
        assertEquals(2L, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE).planItemDefinitionType(HumanTask.class.getSimpleName().toLowerCase()).count());
        
        setClockTo(new Date(startTime.getTime() + (2 * 60 * 60 * 1000)));
        CmmnJobTestHelper.waitForJobExecutorToProcessAllJobs(cmmnEngineConfiguration, 5000L, 200L, true);
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("A", task.getName());
        cmmnTaskService.complete(task.getId());
        assertCaseInstanceEnded(caseInstance);
    }
    
    @Test
    @CmmnDeployment
    public void testTimerStartTrigger() {
        // Completing the stage will be the start trigger for the timer.
        // The timer event will exit the whole plan model
        
        Date startTime = setClockFixedToCurrentTime();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testStartTrigger").start();
        
        assertEquals(1L, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                .planItemDefinitionType(TimerEventListener.class.getSimpleName().toLowerCase()).count());
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("A", task.getName());
        cmmnTaskService.complete(task.getId());
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("B", task.getName());
        cmmnTaskService.complete(task.getId());
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("C", task.getName());
        
        assertEquals(1L, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.AVAILABLE).planItemDefinitionType(TimerEventListener.class.getSimpleName().toLowerCase()).count());
        
        setClockTo(new Date(startTime.getTime() + (3 * 60 * 60 * 1000)));
        CmmnJobTestHelper.waitForJobExecutorToProcessAllJobs(cmmnEngineConfiguration, 5000L, 200L, true);
        assertCaseInstanceEnded(caseInstance);
    }
    
    @Test
    @CmmnDeployment
    public void testExitNestedStageThroughTimer() {
        Date startTime = setClockFixedToCurrentTime();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testExitNestedStageThroughTimer").start();
        assertEquals(3L, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionType(PlanItemDefinitionType.STAGE).count());
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("The task", task.getName());
        
        setClockTo(new Date(startTime.getTime() + (5 * 60 * 60 * 1000)));
        CmmnJobTestHelper.waitForJobExecutorToProcessAllJobs(cmmnEngineConfiguration, 10000L, 100L, true);
        assertEquals(0L, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        assertCaseInstanceEnded(caseInstance);
    }
    
    @Test
    @CmmnDeployment
    public void timerActivatesAndExitStages() {
        Date startTime = setClockFixedToCurrentTime();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("timerActivatesAndExitStages").start();
        
        List<Task> tasks = cmmnTaskService.createTaskQuery()
                .caseInstanceId(caseInstance.getId())
                .orderByTaskName().asc()
                .list();
        assertEquals(2, tasks.size());
        assertEquals("A", tasks.get(0).getName());
        assertEquals("B", tasks.get(1).getName());
        
        assertEquals(0L, cmmnManagementService.createTimerJobQuery().count());
        
        // Completing A activates the stage and the timer event listener
        cmmnTaskService.complete(tasks.get(0).getId());
        cmmnTaskService.complete(tasks.get(1).getId());
        
        // Timer event listener created a timer job
        assertEquals(1L, cmmnManagementService.createTimerJobQuery().count());
        tasks = cmmnTaskService.createTaskQuery()
                .caseInstanceId(caseInstance.getId())
                .orderByTaskName().asc()
                .list();
        assertEquals(3, tasks.size());
        assertEquals("Stage 1 task", tasks.get(0).getName());
        assertEquals("Stage 3 task 1", tasks.get(1).getName());
        assertEquals("Stage 3 task 2", tasks.get(2).getName());
        
        // Timer is set to 10 hours
        setClockTo(new Date(startTime.getTime() + (11 * 60 * 60 * 1000)));
        CmmnJobTestHelper.waitForJobExecutorToProcessAllJobs(cmmnEngineConfiguration, 10000L, 100L, true);
        
        tasks = cmmnTaskService.createTaskQuery()
                .caseInstanceId(caseInstance.getId())
                .orderByTaskName().asc()
                .list();
        assertEquals(2, tasks.size());
        assertEquals("Stage 1 task", tasks.get(0).getName());
        assertEquals("Stage 2 task", tasks.get(1).getName());
        
        for(Task task : tasks) {
            cmmnTaskService.complete(task.getId());
        }
        assertCaseInstanceEnded(caseInstance);
    }
    
}