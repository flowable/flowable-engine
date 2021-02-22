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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.flowable.job.api.TimerJobQuery;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 */
public class BoundaryTimerNonInterruptingEventTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testMultipleTimersOnUserTask() {
        // Set the clock fixed
        Date startTime = new Date();

        // After process start, there should be 3 timers created
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("nonInterruptingTimersOnUserTask");
        org.flowable.task.api.Task task1 = taskService.createTaskQuery().singleResult();
        assertThat(task1.getName()).isEqualTo("First Task");

        TimerJobQuery jobQuery = managementService.createTimerJobQuery().processInstanceId(pi.getId());
        List<Job> jobs = jobQuery.list();
        assertThat(jobs).hasSize(2);

        // After setting the clock to time '1 hour and 5 seconds', the first timer should fire
        processEngineConfiguration.getClock().setCurrentTime(new Date(startTime.getTime() + ((60 * 60 * 1000) + 5000)));
        Job job = managementService.createTimerJobQuery().executable().singleResult();
        assertThat(job).isNotNull();
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        // we still have one timer more to fire
        assertThat(jobQuery.count()).isEqualTo(1);

        // and we are still in the first state, but in the second state as well!
        assertThat(taskService.createTaskQuery().count()).isEqualTo(2);
        List<org.flowable.task.api.Task> taskList = taskService.createTaskQuery().orderByTaskName().desc().list();
        assertThat(taskList)
                .extracting(Task::getName)
                .containsExactly("First Task", "Escalation Task 1");

        // complete the task and end the forked execution
        taskService.complete(taskList.get(1).getId());

        // but we still have the original executions
        assertThat(taskService.createTaskQuery().count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().singleResult().getName()).isEqualTo("First Task");

        // After setting the clock to time '2 hour and 5 seconds', the second timer should fire
        processEngineConfiguration.getClock().setCurrentTime(new Date(startTime.getTime() + ((2 * 60 * 60 * 1000) + 5000)));
        waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(7000L, 25L);

        // no more timers to fire
        assertThat(jobQuery.count()).isZero();

        // and we are still in the first state, but in the next escalation state as well
        assertThat(taskService.createTaskQuery().count()).isEqualTo(2);
        taskList = taskService.createTaskQuery().orderByTaskName().desc().list();
        assertThat(taskList)
                .extracting(Task::getName)
                .containsExactly("First Task", "Escalation Task 2");

        // This time we end the main task
        taskService.complete(taskList.get(0).getId());

        // but we still have the escalation task
        assertThat(taskService.createTaskQuery().count()).isEqualTo(1);
        org.flowable.task.api.Task escalationTask = taskService.createTaskQuery().singleResult();
        assertThat(escalationTask.getName()).isEqualTo("Escalation Task 2");

        taskService.complete(escalationTask.getId());

        // now we are really done :-)
        assertProcessEnded(pi.getId());
    }

    @Test
    @Deployment
    public void testJoin() {
        // Set the clock fixed
        Date startTime = new Date();

        // After process start, there should be 3 timers created
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("testJoin");
        org.flowable.task.api.Task task1 = taskService.createTaskQuery().singleResult();
        assertThat(task1.getName()).isEqualTo("Main Task");

        TimerJobQuery jobQuery = managementService.createTimerJobQuery().processInstanceId(pi.getId());
        List<Job> jobs = jobQuery.list();
        assertThat(jobs).hasSize(1);

        // After setting the clock to time '1 hour and 5 seconds', the first timer should fire
        processEngineConfiguration.getClock().setCurrentTime(new Date(startTime.getTime() + ((60 * 60 * 1000) + 5000)));
        waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(7000L, 25L);

        // timer has fired
        assertThat(jobQuery.count()).isZero();

        // we now have both tasks
        assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

        // end the first
        taskService.complete(task1.getId());

        // we now have one task left
        assertThat(taskService.createTaskQuery().count()).isEqualTo(1);
        org.flowable.task.api.Task task2 = taskService.createTaskQuery().singleResult();
        assertThat(task2.getName()).isEqualTo("Escalation Task");

        // complete the task, the parallel gateway should fire
        taskService.complete(task2.getId());

        // and the process has ended
        assertProcessEnded(pi.getId());
    }

    @Test
    @Deployment
    public void testTimerOnConcurrentTasks() {
        String procId = runtimeService.startProcessInstanceByKey("nonInterruptingOnConcurrentTasks").getId();
        assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

        Job timer = managementService.createTimerJobQuery().singleResult();
        managementService.moveTimerToExecutableJob(timer.getId());
        managementService.executeJob(timer.getId());
        assertThat(taskService.createTaskQuery().count()).isEqualTo(3);

        // Complete task that was reached by non interrupting timer
        org.flowable.task.api.Task task = taskService.createTaskQuery().taskDefinitionKey("timerFiredTask").singleResult();
        taskService.complete(task.getId());
        assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

        // Complete other tasks
        for (org.flowable.task.api.Task t : taskService.createTaskQuery().list()) {
            taskService.complete(t.getId());
        }
        assertProcessEnded(procId);
    }

    // Difference with previous test: now the join will be reached first
    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/timer/BoundaryTimerNonInterruptingEventTest.testTimerOnConcurrentTasks.bpmn20.xml" })
    public void testTimerOnConcurrentTasks2() {
        String procId = runtimeService.startProcessInstanceByKey("nonInterruptingOnConcurrentTasks").getId();
        assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

        Job timer = managementService.createTimerJobQuery().singleResult();
        managementService.moveTimerToExecutableJob(timer.getId());
        managementService.executeJob(timer.getId());
        assertThat(taskService.createTaskQuery().count()).isEqualTo(3);

        // Complete 2 tasks that will trigger the join
        org.flowable.task.api.Task task = taskService.createTaskQuery().taskDefinitionKey("firstTask").singleResult();
        taskService.complete(task.getId());
        task = taskService.createTaskQuery().taskDefinitionKey("secondTask").singleResult();
        taskService.complete(task.getId());
        assertThat(taskService.createTaskQuery().count()).isEqualTo(1);

        // Finally, complete the task that was created due to the timer
        task = taskService.createTaskQuery().taskDefinitionKey("timerFiredTask").singleResult();
        taskService.complete(task.getId());

        assertProcessEnded(procId);
    }

    @Test
    @Deployment
    public void testTimerWithCycle() throws Exception {
        String processInstanceId = runtimeService.startProcessInstanceByKey("nonInterruptingCycle").getId();

        List<Job> jobs = managementService.createTimerJobQuery().processInstanceId(processInstanceId).list();
        assertThat(jobs).hasSize(1);

        // boundary events
        waitForJobExecutorToProcessAllJobs(2000, 100);

        // a new job must be prepared because there are indefinite number of repeats 1 hour interval");
        assertThat(managementService.createTimerJobQuery().processInstanceId(processInstanceId).count()).isEqualTo(1);

        moveByMinutes(60);
        waitForJobExecutorToProcessAllJobs(2000, 100);

        // a new job must be prepared because there are indefinite number of repeats 1 hour interval");
        assertThat(managementService.createTimerJobQuery().processInstanceId(processInstanceId).count()).isEqualTo(1);

        org.flowable.task.api.Task task = taskService.createTaskQuery().taskDefinitionKey("task").singleResult();
        taskService.complete(task.getId());

        moveByMinutes(60);
        assertThatCode(() -> { waitForJobExecutorToProcessAllJobs(2000, 100); })
                .as("No more jobs since the user completed the task")
                .doesNotThrowAnyException();
    }

    /*
     * see https://activiti.atlassian.net/browse/ACT-1173
     */
    @Test
    @Deployment
    public void testTimerOnEmbeddedSubprocess() {
        String id = runtimeService.startProcessInstanceByKey("nonInterruptingTimerOnEmbeddedSubprocess").getId();

        TaskQuery tq = taskService.createTaskQuery().taskAssignee("kermit");

        assertThat(tq.count()).isEqualTo(1);

        // Simulate timer
        Job timer = managementService.createTimerJobQuery().singleResult();
        managementService.moveTimerToExecutableJob(timer.getId());
        managementService.executeJob(timer.getId());

        tq = taskService.createTaskQuery().taskAssignee("kermit");

        assertThat(tq.count()).isEqualTo(2);

        List<org.flowable.task.api.Task> tasks = tq.list();

        taskService.complete(tasks.get(0).getId());
        taskService.complete(tasks.get(1).getId());

        assertProcessEnded(id);
    }

    /*
     * see https://activiti.atlassian.net/browse/ACT-1106
     */
    @Test
    @Deployment
    public void testReceiveTaskWithBoundaryTimer() {
        // Set the clock fixed
        HashMap<String, Object> variables = new HashMap<>();
        variables.put("timeCycle", "R/PT1H");

        // After process start, there should be a timer created
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("nonInterruptingCycle", variables);

        TimerJobQuery jobQuery = managementService.createTimerJobQuery().processInstanceId(pi.getId());
        List<Job> jobs = jobQuery.list();
        assertThat(jobs).hasSize(1);

        // The Execution Query should work normally and find executions in state "task"
        List<Execution> executions = runtimeService.createExecutionQuery().activityId("task").list();
        assertThat(executions).hasSize(1);
        List<String> activeActivityIds = runtimeService.getActiveActivityIds(executions.get(0).getId());
        Collections.sort(activeActivityIds);
        assertThat(activeActivityIds)
                .containsExactly("task", "timer");

        runtimeService.trigger(executions.get(0).getId());

        // After setting the clock to time '1 hour and 5 seconds', the second
        // timer should fire
        // processEngineConfiguration.getClock().setCurrentTime(new
        // Date(startTime.getTime() + ((60 * 60 * 1000) + 5000)));
        // waitForJobExecutorToProcessAllJobs(7000L, 25L);
        // assertThat(jobQuery.count())..isZero();

        // which means the process has ended
        assertProcessEnded(pi.getId());
    }

    @Test
    @Deployment
    public void testTimerOnConcurrentSubprocess() {
        String procId = runtimeService.startProcessInstanceByKey("testTimerOnConcurrentSubprocess").getId();
        assertThat(taskService.createTaskQuery().count()).isEqualTo(4);

        Job timer = managementService.createTimerJobQuery().singleResult();
        managementService.moveTimerToExecutableJob(timer.getId());
        managementService.executeJob(timer.getId());
        assertThat(taskService.createTaskQuery().count()).isEqualTo(5);

        // Complete 4 tasks that will trigger the join
        org.flowable.task.api.Task task = taskService.createTaskQuery().taskDefinitionKey("sub1task1").singleResult();
        taskService.complete(task.getId());
        task = taskService.createTaskQuery().taskDefinitionKey("sub1task2").singleResult();
        taskService.complete(task.getId());
        task = taskService.createTaskQuery().taskDefinitionKey("sub2task1").singleResult();
        taskService.complete(task.getId());
        task = taskService.createTaskQuery().taskDefinitionKey("sub2task2").singleResult();
        taskService.complete(task.getId());
        assertThat(taskService.createTaskQuery().count()).isEqualTo(1);

        // Finally, complete the task that was created due to the timer
        task = taskService.createTaskQuery().taskDefinitionKey("timerFiredTask").singleResult();
        taskService.complete(task.getId());

        assertProcessEnded(procId);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/event/timer/BoundaryTimerNonInterruptingEventTest.testTimerOnConcurrentSubprocess.bpmn20.xml")
    public void testTimerOnConcurrentSubprocess2() {
        String procId = runtimeService.startProcessInstanceByKey("testTimerOnConcurrentSubprocess").getId();
        assertThat(taskService.createTaskQuery().count()).isEqualTo(4);

        Job timer = managementService.createTimerJobQuery().singleResult();
        managementService.moveTimerToExecutableJob(timer.getId());
        managementService.executeJob(timer.getId());
        assertThat(taskService.createTaskQuery().count()).isEqualTo(5);

        org.flowable.task.api.Task task = taskService.createTaskQuery().taskDefinitionKey("sub1task1").singleResult();
        taskService.complete(task.getId());
        task = taskService.createTaskQuery().taskDefinitionKey("sub1task2").singleResult();
        taskService.complete(task.getId());

        // complete the task that was created due to the timer
        task = taskService.createTaskQuery().taskDefinitionKey("timerFiredTask").singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().taskDefinitionKey("sub2task1").singleResult();
        taskService.complete(task.getId());
        task = taskService.createTaskQuery().taskDefinitionKey("sub2task2").singleResult();
        taskService.complete(task.getId());
        assertThat(taskService.createTaskQuery().count()).isZero();

        assertProcessEnded(procId);
    }

    private void moveByMinutes(int minutes) throws Exception {
        processEngineConfiguration.getClock().setCurrentTime(new Date(processEngineConfiguration.getClock().getCurrentTime().getTime() + ((minutes * 60 * 1000))));
    }

}
