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
package org.flowable.cmmn.test.repetition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Date;
import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.engine.test.impl.CmmnJobTestHelper;
import org.flowable.cmmn.model.HumanTask;
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
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(HumanTask.class.getSimpleName().toLowerCase()).singleResult();
        assertEquals(PlanItemInstanceState.AVAILABLE, planItemInstance.getState());
        assertEquals(0L, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
        // Should have a timer job available
        Job job = cmmnManagementService.createTimerJobQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertNotNull(job);
        
        // Moving the timer 1 hour ahead, should create a task instance. 
        currentTime = new Date(currentTime.getTime() + (60 * 60 * 1000) + 10);
        setClockTo(currentTime);
        job = cmmnManagementService.moveTimerToExecutableJob(job.getId());
        cmmnManagementService.executeJob(job.getId());
        assertEquals(1L, cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count());
        
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
        
        // Terminating the case instance should remove the timer
        cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());
        assertEquals(0L, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).count());
        assertEquals(0L, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(0L, cmmnManagementService.createTimerJobQuery().caseInstanceId(caseInstance.getId()).count());
    }
    
}
