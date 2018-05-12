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

import java.text.SimpleDateFormat;
import java.time.Instant;
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

public class IntermediateTimerEventTest extends PluggableFlowableTestCase {
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    @Deployment
    public void testCatchingTimerEvent() throws Exception {

        // Set the clock fixed
        Date startTime = new Date();

        // After process start, there should be timer created
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("intermediateTimerEventExample");
        TimerJobQuery jobQuery = managementService.createTimerJobQuery().processInstanceId(pi.getId());
        assertEquals(1, jobQuery.count());

        // After setting the clock to time '50minutes and 5 seconds', the second timer should fire
        processEngineConfiguration.getClock().setCurrentTime(new Date(startTime.getTime() + ((50 * 60 * 1000) + 5000)));
        waitForJobExecutorToProcessAllJobs(5000L, 25L);

        assertEquals(0, jobQuery.count());
        assertProcessEnded(pi.getProcessInstanceId());

    }

    @Deployment
    public void testTimerEventWithStartAndDuration() throws Exception {

        Calendar testStartCal = new GregorianCalendar(2016, 0, 1, 10, 0, 0);
        Date testStartTime = testStartCal.getTime();
        processEngineConfiguration.getClock().setCurrentTime(testStartTime);

        ProcessInstance pi = runtimeService.startProcessInstanceByKey("timerEventWithStartAndDuration");
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertEquals(1, tasks.size());
        org.flowable.task.api.Task task = tasks.get(0);
        assertEquals("Task A", task.getName());

        TimerJobQuery jobQuery = managementService.createTimerJobQuery().processInstanceId(pi.getId());
        assertEquals(0, jobQuery.count());

        Date startDate = new Date();
        runtimeService.setVariable(pi.getId(), "StartDate", startDate);
        taskService.complete(task.getId());

        jobQuery = managementService.createTimerJobQuery().processInstanceId(pi.getId());
        assertEquals(1, jobQuery.count());

        processEngineConfiguration.getClock().setCurrentTime(new Date(startDate.getTime() + 7000L));

        jobQuery = managementService.createTimerJobQuery().processInstanceId(pi.getId());
        assertEquals(1, jobQuery.count());
        jobQuery = managementService.createTimerJobQuery().processInstanceId(pi.getId()).executable();
        assertEquals(0, jobQuery.count());

        processEngineConfiguration.getClock().setCurrentTime(new Date(startDate.getTime() + 11000L));
        waitForJobExecutorToProcessAllJobs(15000L, 25L);

        jobQuery = managementService.createTimerJobQuery().processInstanceId(pi.getId());
        assertEquals(0, jobQuery.count());

        tasks = taskService.createTaskQuery().list();
        assertEquals(1, tasks.size());
        task = tasks.get(0);
        assertEquals("Task B", task.getName());
        taskService.complete(task.getId());

        assertProcessEnded(pi.getProcessInstanceId());

        processEngineConfiguration.getClock().reset();
    }

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
        ProcessInstance pi3 = runtimeService.startProcessInstanceByKey("intermediateTimerEventExample", variables2);

        assertEquals(1, managementService.createTimerJobQuery().processInstanceId(pi1.getId()).count());
        assertEquals(1, managementService.createTimerJobQuery().processInstanceId(pi2.getId()).count());
        assertEquals(1, managementService.createTimerJobQuery().processInstanceId(pi3.getId()).count());

        // After setting the clock to one second in the future the timers should fire
        List<Job> jobs = managementService.createTimerJobQuery().executable().list();
        assertEquals(3, jobs.size());
        for (Job job : jobs) {
            managementService.moveTimerToExecutableJob(job.getId());
            managementService.executeJob(job.getId());
        }

        assertEquals(0, managementService.createTimerJobQuery().processInstanceId(pi1.getId()).count());
        assertEquals(0, managementService.createTimerJobQuery().processInstanceId(pi2.getId()).count());
        assertEquals(0, managementService.createTimerJobQuery().processInstanceId(pi3.getId()).count());

        assertProcessEnded(pi1.getProcessInstanceId());
        assertProcessEnded(pi2.getProcessInstanceId());
        assertProcessEnded(pi3.getProcessInstanceId());
    }

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
        assertEquals(0, tasks.size());
        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);
        long diffInMilliseconds = Math.abs(startTimeInMillis - timerJob.getDuedate().getTime());
        assertTrue(diffInMilliseconds < 100);

        // reschedule timer for two hours from now
        calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, 2);
        Job rescheduledJob = managementService.rescheduleTimeDateJob(timerJob.getId(), sdf.format(calendar.getTime()));
        assertNotNull(rescheduledJob);
        assertNotNull(rescheduledJob.getId());
        assertNotSame(timerJob.getId(), rescheduledJob.getId());

        Job timer = managementService.createTimerJobQuery().singleResult();
        assertEquals(rescheduledJob.getId(), timer.getId());

        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        diffInMilliseconds = Math.abs(startTimeInMillis - timerJob.getDuedate().getTime());
        assertTrue(diffInMilliseconds > (59 * 60 * 1000));

        // Move clock forward 1 hour from now
        calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, 1);
        calendar.add(Calendar.MINUTE, 5);
        processEngineConfiguration.getClock().setCurrentTime(calendar.getTime());
        JobTestHelper.executeJobExecutorForTime(processEngineConfiguration, 1000, 100);

        // Confirm timer has not run
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(0, tasks.size());
        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);

        // Move clock forward 2 hours from now
        calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, 2);
        processEngineConfiguration.getClock().setCurrentTime(calendar.getTime());
        waitForJobExecutorToProcessAllJobs(2000, 100);

        // Confirm timer has run
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(1, tasks.size());
        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNull(timerJob);
    }

    @Deployment
    public void testParallelTimerEvents() throws Exception {
        // Set the clock fixed
        Date startTime = new Date();

        // After process start, there should be timer created
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("parallelIntermediateTimers");
        TimerJobQuery jobQuery = managementService.createTimerJobQuery().processInstanceId(pi.getId());
        assertEquals(2, jobQuery.count());

        // After setting the clock to time '50minutes and 5 seconds', the bouth timers should fire in parralel
        processEngineConfiguration.getClock().setCurrentTime(new Date(startTime.getTime() + ((50 * 60 * 1000) + 5000)));
        try {
            JobTestHelper.waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(
                    this.processEngineConfiguration, this.managementService, 5000L, 250L
            );

            assertEquals(0, jobQuery.count());
            assertProcessEnded(pi.getProcessInstanceId());
            assertEquals("Timer paths must be executed exactly 2 times without failure repetition",
                    2, IntermediateTimerEventTestCounter.getCount());
        } finally {
            processEngineConfiguration.getClock().reset();
        }
    }

}
