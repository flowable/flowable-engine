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

package org.flowable.engine.test.bpmn.event.timer;

import java.util.List;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.junit.jupiter.api.Test;

/**
 * @author Tijs Rademakers
 */
public class TimerEventSubprocessTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testInterruptingUnderProcessDefinition() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

        // the process instance must have a timer job:
        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(job);

        // if we trigger the usertask, the process terminates and the timer job is removed:
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertEquals("task", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertEquals(0, managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).count());
        assertEquals(0, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());
        assertProcessEnded(processInstance.getId());

        // now we start a new instance but this time we trigger the timer job:
        processInstance = runtimeService.startProcessInstanceByKey("process");
        job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(job);
        job = managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        task = taskService.createTaskQuery().singleResult();
        assertEquals("eventSubProcessTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());
        assertProcessEnded(processInstance.getId());
        assertEquals(0, managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).count());
        assertEquals(0, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());
    }

    @Test
    @Deployment
    public void testNonInterruptingUnderProcessDefinition() {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

        // the process instance must have a timer job:
        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(job);
        assertEquals(3, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        // if we trigger the usertask, the process terminates and the event subscription is removed:
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertEquals("task", task.getTaskDefinitionKey());
        taskService.complete(task.getId());
        assertEquals(0, managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).count());
        assertEquals(0, runtimeService.createExecutionQuery().count());

        // now we start a new instance but this time we trigger the event subprocess:
        processInstance = runtimeService.startProcessInstanceByKey("process");
        job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(job);
        job = managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        assertEquals(2, taskService.createTaskQuery().count());

        // now let's first complete the task in the main flow:
        task = taskService.createTaskQuery().taskDefinitionKey("task").singleResult();
        taskService.complete(task.getId());

        // we still have 3 executions:
        assertEquals(3, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        // now let's complete the task in the event subprocess
        task = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask").singleResult();
        taskService.complete(task.getId());

        // done!
        assertEquals(0, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        // #################### again, the other way around:

        processInstance = runtimeService.startProcessInstanceByKey("process");
        job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(job);
        job = managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        assertEquals(2, taskService.createTaskQuery().count());

        task = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask").singleResult();
        taskService.complete(task.getId());

        assertEquals(0, managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).count());

        // we still have 3 executions:
        assertEquals(3, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        task = taskService.createTaskQuery().taskDefinitionKey("task").singleResult();
        taskService.complete(task.getId());

        // done!
        assertEquals(0, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());
    }

    @Test
    @Deployment
    public void testNonInterruptingSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
        assertEquals(3, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(job);
        assertEquals(3, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        job = managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());
        assertEquals(6, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        assertEquals(2, taskService.createTaskQuery().count());
        assertEquals(1, managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).count());

        job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        job = managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());
        assertEquals(9, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        assertEquals(3, taskService.createTaskQuery().count());
        assertEquals(1, managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).count());

        // now let's first complete the task in the main flow:
        org.flowable.task.api.Task task = taskService.createTaskQuery().taskDefinitionKey("task").singleResult();
        taskService.complete(task.getId());

        assertEquals(0, managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).count());

        // we still have 7 executions:
        assertEquals(7, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        // now let's complete the first task in the event subprocess
        task = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask").list().get(0);
        taskService.complete(task.getId());

        assertEquals(4, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        // complete the second task in the event subprocess
        task = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask").singleResult();
        taskService.complete(task.getId());

        // done!
        assertEquals(0, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());
    }

    @Test
    @Deployment
    public void testInterruptingSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
        assertEquals(3, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(job);
        assertEquals(3, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        job = managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());
        assertEquals(5, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        assertEquals(1, taskService.createTaskQuery().count());
        assertEquals(0, managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).count());

        // now let's complete the task in the event subprocess
        org.flowable.task.api.Task task = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask").list().get(0);
        taskService.complete(task.getId());

        // done!
        assertEquals(0, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());
    }
    
    @Test
    @Deployment
    public void testStartingAdditionalTasks() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startingAdditionalTasks");
        assertEquals(3, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(job);
        String firstTimerJobId = job.getId();

        assertEquals(1, taskService.createTaskQuery().count());

        // now let's first complete the task in the main flow:
        org.flowable.task.api.Task task = taskService.createTaskQuery().taskDefinitionKey("task1").singleResult();
        taskService.complete(task.getId());

        List<Job> jobs = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, jobs.size());
        
        String secondTimerJobId = null;
        for (Job timerJob : jobs) {
            if (!timerJob.getId().equals(firstTimerJobId)) {
                secondTimerJobId = timerJob.getId();
            }
        }

        job = managementService.moveTimerToExecutableJob(secondTimerJobId);
        managementService.executeJob(job.getId());
        
        assertEquals(2, taskService.createTaskQuery().count());
        
        job = managementService.moveTimerToExecutableJob(firstTimerJobId);
        managementService.executeJob(job.getId());
        
        assertEquals(3, taskService.createTaskQuery().count());
        
        task = taskService.createTaskQuery().taskDefinitionKey("additionalTask").singleResult();
        taskService.complete(task.getId());

        assertEquals(2, taskService.createTaskQuery().count());
        
        task = taskService.createTaskQuery().taskDefinitionKey("subTask1").singleResult();
        taskService.complete(task.getId());
        
        assertEquals(1, taskService.createTaskQuery().count());
        
        task = taskService.createTaskQuery().taskDefinitionKey("additionalSubTask").singleResult();
        taskService.complete(task.getId());
        
        assertEquals(1, taskService.createTaskQuery().count());
        
        task = taskService.createTaskQuery().taskDefinitionKey("task2").singleResult();
        taskService.complete(task.getId());

        // done!
        assertEquals(0, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());
    }
    
    @Test
    @Deployment(resources="org/flowable/engine/test/bpmn/event/timer/TimerEventSubprocessTest.testStartingAdditionalTasks.bpmn20.xml")
    public void testStartingAdditionalTasksNoNestedEventSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startingAdditionalTasks");
        assertEquals(3, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(job);

        assertEquals(1, taskService.createTaskQuery().count());

        // now let's first complete the task in the main flow:
        org.flowable.task.api.Task task = taskService.createTaskQuery().taskDefinitionKey("task1").singleResult();
        taskService.complete(task.getId());

        List<Job> jobs = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, jobs.size());
        
        assertEquals(1, taskService.createTaskQuery().count());
        
        task = taskService.createTaskQuery().taskDefinitionKey("subTask1").singleResult();
        taskService.complete(task.getId());
        
        jobs = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(1, jobs.size());
        assertEquals(1, taskService.createTaskQuery().count());
        
        task = taskService.createTaskQuery().taskDefinitionKey("task2").singleResult();
        taskService.complete(task.getId());

        // done!
        assertEquals(0, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());
    }
    
    @Test
    @Deployment(resources="org/flowable/engine/test/bpmn/event/timer/TimerEventSubprocessTest.testStartingAdditionalTasks.bpmn20.xml")
    public void testStartingAdditionalTasksWithNestedEventSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startingAdditionalTasks");
        
        assertEquals(1, taskService.createTaskQuery().count());
        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(job);
        String firstTimerJobId = job.getId();

        // now let's first complete the task in the main flow:
        org.flowable.task.api.Task task = taskService.createTaskQuery().taskDefinitionKey("task1").singleResult();
        taskService.complete(task.getId());

        assertEquals(1, taskService.createTaskQuery().count());
        List<Job> jobs = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, jobs.size());
        
        String secondTimerJobId = null;
        for (Job timerJob : jobs) {
            if (!timerJob.getId().equals(firstTimerJobId)) {
                secondTimerJobId = timerJob.getId();
            }
        }

        job = managementService.moveTimerToExecutableJob(secondTimerJobId);
        managementService.executeJob(job.getId());
        
        assertEquals(2, taskService.createTaskQuery().count());
        
        task = taskService.createTaskQuery().taskDefinitionKey("subTask1").singleResult();
        taskService.complete(task.getId());
        
        job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(job);
        assertEquals(1, taskService.createTaskQuery().count());
        
        task = taskService.createTaskQuery().taskDefinitionKey("additionalSubTask").singleResult();
        taskService.complete(task.getId());
        
        job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(job);
        assertEquals(1, taskService.createTaskQuery().count());
        
        job = managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());
        
        assertEquals(2, taskService.createTaskQuery().count());
        
        task = taskService.createTaskQuery().taskDefinitionKey("task2").singleResult();
        taskService.complete(task.getId());
        
        assertEquals(0, managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).count());
        
        task = taskService.createTaskQuery().taskDefinitionKey("additionalTask").singleResult();
        taskService.complete(task.getId());

        // done!
        assertEquals(0, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());
    }
    
    @Test
    @Deployment
    public void testStartingAdditionalTasksInterrupting() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startingAdditionalTasks");
        assertEquals(3, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(job);
        String firstTimerJobId = job.getId();

        assertEquals(1, taskService.createTaskQuery().count());

        // now let's first complete the task in the main flow:
        org.flowable.task.api.Task task = taskService.createTaskQuery().taskDefinitionKey("task1").singleResult();
        taskService.complete(task.getId());

        List<Job> jobs = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, jobs.size());

        String secondTimerJobId = null;
        for (Job timerJob : jobs) {
            if (!timerJob.getId().equals(firstTimerJobId)) {
                secondTimerJobId = timerJob.getId();
            }
        }

        job = managementService.moveTimerToExecutableJob(secondTimerJobId);
        managementService.executeJob(job.getId());
        
        assertEquals(1, taskService.createTaskQuery().count());
        
        task = taskService.createTaskQuery().taskDefinitionKey("additionalSubTask").singleResult();
        taskService.complete(task.getId());
        
        assertEquals(1, taskService.createTaskQuery().count());
        
        task = taskService.createTaskQuery().taskDefinitionKey("task2").singleResult();
        taskService.complete(task.getId());

        // done!
        assertEquals(0, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());
    }
    
    @Test
    @Deployment(resources="org/flowable/engine/test/bpmn/event/timer/TimerEventSubprocessTest.testStartingAdditionalTasksInterrupting.bpmn20.xml")
    public void testStartingAdditionalTasksInterruptingWithMainEventSubProcessInterrupt() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startingAdditionalTasks");
        assertEquals(3, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());

        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(job);
        String firstTimerJobId = job.getId();

        assertEquals(1, taskService.createTaskQuery().count());
        
        // now let's first complete the task in the main flow:
        org.flowable.task.api.Task task = taskService.createTaskQuery().taskDefinitionKey("task1").singleResult();
        taskService.complete(task.getId());

        List<Job> jobs = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, jobs.size());

        String secondTimerJobId = null;
        for (Job timerJob : jobs) {
            if (!timerJob.getId().equals(firstTimerJobId)) {
                secondTimerJobId = timerJob.getId();
            }
        }

        job = managementService.moveTimerToExecutableJob(secondTimerJobId);
        managementService.executeJob(job.getId());
        
        assertEquals(1, taskService.createTaskQuery().count());
        
        job = managementService.moveTimerToExecutableJob(firstTimerJobId);
        managementService.executeJob(job.getId());
        
        assertEquals(1, taskService.createTaskQuery().count());
        
        task = taskService.createTaskQuery().taskDefinitionKey("additionalTask").singleResult();
        taskService.complete(task.getId());

        // done!
        assertEquals(0, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count());
    }
}
