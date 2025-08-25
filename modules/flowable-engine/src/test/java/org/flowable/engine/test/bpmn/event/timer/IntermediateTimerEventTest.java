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

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.engine.impl.test.JobTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.flowable.job.api.TimerJobQuery;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

public class IntermediateTimerEventTest extends PluggableFlowableTestCase {
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    @Test
    @Deployment
    public void testCatchingTimerEvent() throws Exception {
        // Set the clock fixed
        Instant startTime = Instant.now();
        processEngineConfiguration.getClock().setCurrentTime(Date.from(startTime));

        // After process start, there should be timer created
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("intermediateTimerEventExample");
        TimerJobQuery jobQuery = managementService.createTimerJobQuery().processInstanceId(pi.getId());
        assertThat(jobQuery.count()).isEqualTo(1);
        
        Job job = managementService.createTimerJobQuery().elementId("timer").singleResult();
        assertThat(job.getElementId()).isEqualTo("timer");
        assertThat(job.getElementName()).isEqualTo("Timer catch");

        // After setting the clock to time '5 minutes and 5 seconds', the second timer should fire
        processEngineConfiguration.getClock().setCurrentTime(Date.from(startTime.plus(5, ChronoUnit.MINUTES).plus(5, ChronoUnit.SECONDS)));
        waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(7000L, 200L);

        assertThat(jobQuery.count()).isZero();
        assertProcessEnded(pi.getProcessInstanceId());

    }

    @Test
    @Deployment
    public void testTimerEventWithStartAndDuration() throws Exception {

        Calendar testStartCal = new GregorianCalendar(2016, 0, 1, 10, 0, 0);
        Date testStartTime = testStartCal.getTime();
        processEngineConfiguration.getClock().setCurrentTime(testStartTime);

        ProcessInstance pi = runtimeService.startProcessInstanceByKey("timerEventWithStartAndDuration");
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("Task A");

        TimerJobQuery jobQuery = managementService.createTimerJobQuery().processInstanceId(pi.getId());
        assertThat(jobQuery.count()).isZero();

        Date startDate = new Date();
        runtimeService.setVariable(pi.getId(), "StartDate", startDate);
        taskService.complete(tasks.get(0).getId());

        jobQuery = managementService.createTimerJobQuery().processInstanceId(pi.getId());
        assertThat(jobQuery.count()).isEqualTo(1);

        processEngineConfiguration.getClock().setCurrentTime(new Date(startDate.getTime() + 7000L));

        jobQuery = managementService.createTimerJobQuery().processInstanceId(pi.getId());
        assertThat(jobQuery.count()).isEqualTo(1);
        jobQuery = managementService.createTimerJobQuery().processInstanceId(pi.getId()).executable();
        assertThat(jobQuery.count()).isZero();

        processEngineConfiguration.getClock().setCurrentTime(new Date(startDate.getTime() + 11000L));
        waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(15000L, 200L);

        jobQuery = managementService.createTimerJobQuery().processInstanceId(pi.getId());
        assertThat(jobQuery.count()).isZero();

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("Task B");
        taskService.complete(tasks.get(0).getId());

        assertProcessEnded(pi.getProcessInstanceId());

        processEngineConfiguration.getClock().reset();
    }

    @Test
    @Deployment
    public void testExpression() {
        // Set the clock fixed
        HashMap<String, Object> variables1 = new HashMap<>();
        variables1.put("dueDate", new Date());

        HashMap<String, Object> variables2 = new HashMap<>();
        variables2.put("dueDate", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date()));

        HashMap<String, Object> variables3 = new HashMap<>();
        variables3.put("dueDate", Instant.now());

        // After process start, there should be timer created
        ProcessInstance pi1 = runtimeService.startProcessInstanceByKey("intermediateTimerEventExample", variables1);
        ProcessInstance pi2 = runtimeService.startProcessInstanceByKey("intermediateTimerEventExample", variables2);
        ProcessInstance pi3 = runtimeService.startProcessInstanceByKey("intermediateTimerEventExample", variables3);

        assertThat(managementService.createTimerJobQuery().processInstanceId(pi1.getId()).count()).isEqualTo(1);
        assertThat(managementService.createTimerJobQuery().processInstanceId(pi2.getId()).count()).isEqualTo(1);
        assertThat(managementService.createTimerJobQuery().processInstanceId(pi3.getId()).count()).isEqualTo(1);

        // After setting the clock to one second in the future the timers should fire
        List<Job> jobs = managementService.createTimerJobQuery().executable().list();
        assertThat(jobs).hasSize(3);
        for (Job job : jobs) {
            managementService.moveTimerToExecutableJob(job.getId());
            managementService.executeJob(job.getId());
        }

        assertThat(managementService.createTimerJobQuery().processInstanceId(pi1.getId()).count()).isZero();
        assertThat(managementService.createTimerJobQuery().processInstanceId(pi2.getId()).count()).isZero();
        assertThat(managementService.createTimerJobQuery().processInstanceId(pi3.getId()).count()).isZero();

        assertProcessEnded(pi1.getProcessInstanceId());
        assertProcessEnded(pi2.getProcessInstanceId());
        assertProcessEnded(pi3.getProcessInstanceId());
    }

    @Test
    @Deployment
    public void testLoop() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testLoop");

        // After looping 3 times, the process should end
        for (int i = 0; i < 3; i++) {
            Job timer = managementService.createTimerJobQuery().singleResult();
            managementService.moveTimerToExecutableJob(timer.getId());
            managementService.executeJob(timer.getId());
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment
    public void testLoopWithCycle() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testLoop");

        // After looping 3 times, the process should end. Cycle should NOT repeat itself
        for (int i = 0; i < 3; i++) {
            Job timer = managementService.createTimerJobQuery().singleResult();
            managementService.moveTimerToExecutableJob(timer.getId());
            managementService.executeJob(timer.getId());
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment
    public void testRescheduleTimer() {
        // startDate variable set to one hour from now
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, 1);
        long startTimeInMillis = calendar.getTime().getTime();

        Map<String, Object> variables = new HashMap<>();
        variables.put("startDate", calendar.getTime());
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("rescheduleTimer", variables);

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).isEmpty();
        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(timerJob).isNotNull();
        long diffInMilliseconds = Math.abs(startTimeInMillis - timerJob.getDuedate().getTime());
        assertThat(diffInMilliseconds).isLessThan(100);

        // reschedule timer for two hours from now
        calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, 2);
        Job rescheduledJob = managementService.rescheduleTimeDateJob(timerJob.getId(), sdf.format(calendar.getTime()));
        assertThat(rescheduledJob).isNotNull();
        assertThat(rescheduledJob.getId()).isNotSameAs(timerJob.getId());

        Job timer = managementService.createTimerJobQuery().singleResult();
        assertThat(timer.getId()).isEqualTo(rescheduledJob.getId());

        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        diffInMilliseconds = Math.abs(startTimeInMillis - timerJob.getDuedate().getTime());
        assertThat(diffInMilliseconds).isGreaterThan(59 * 60 * 1000);

        // Move clock forward 1 hour from now
        calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, 1);
        calendar.add(Calendar.MINUTE, 5);
        processEngineConfiguration.getClock().setCurrentTime(calendar.getTime());
        JobTestHelper.executeJobExecutorForTime(processEngineConfiguration, 1000, 100);

        // Confirm timer has not run
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).isEmpty();
        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(timerJob).isNotNull();

        // Move clock forward 2 hours from now
        calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, 2);
        processEngineConfiguration.getClock().setCurrentTime(calendar.getTime());
        waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(2000, 100);

        // Confirm timer has run
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).hasSize(1);
        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(timerJob).isNull();
    }

    @Test
    @Deployment
    public void testParallelTimerEvents() throws Exception {
        // Set the clock fixed
        Date startTime = new Date();

        // After process start, there should be timer created
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("parallelIntermediateTimers");
        TimerJobQuery jobQuery = managementService.createTimerJobQuery().processInstanceId(pi.getId());
        assertThat(jobQuery.count()).isEqualTo(2);

        // After setting the clock to time '50minutes and 5 seconds', the both timers should fire in parallel
        processEngineConfiguration.getClock().setCurrentTime(new Date(startTime.getTime() + ((50 * 60 * 1000) + 5000)));
        try {
            JobTestHelper.waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(
                    this.processEngineConfiguration, this.managementService, 20000L, 250L
            );

            assertThat(jobQuery.count()).isZero();
            assertProcessEnded(pi.getProcessInstanceId());
            assertThat(IntermediateTimerEventTestCounter.getCount())
                    .as("Timer paths must be executed 2 times (or more, with tx retries).isTrue() without failure repetition")
                    .isGreaterThanOrEqualTo(2);
        } finally {
            processEngineConfiguration.getClock().reset();
        }
    }

}
