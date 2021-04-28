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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.JobTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ActivityInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.flowable.job.api.TimerJobQuery;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 */
public class BoundaryTimerEventTest extends PluggableFlowableTestCase {

    private static boolean listenerExecutedStartEvent;
    private static boolean listenerExecutedEndEvent;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    public static class MyExecutionListener implements ExecutionListener {
        private static final long serialVersionUID = 1L;

        @Override
        public void notify(DelegateExecution execution) {
            if ("end".equals(execution.getEventName())) {
                listenerExecutedEndEvent = true;
            } else if ("start".equals(execution.getEventName())) {
                listenerExecutedStartEvent = true;
            }
        }
    }

    /*
     * Test for when multiple boundary timer events are defined on the same user task
     *
     * Configuration: - timer 1 -> 2 hours -> secondTask - timer 2 -> 1 hour -> thirdTask - timer 3 -> 3 hours -> fourthTask
     *
     * See process image next to the process xml resource
     */
    @Test
    @Deployment
    public void testMultipleTimersOnUserTask() {

        // Set the clock fixed
        Date startTime = new Date();

        // After process start, there should be 3 timers created
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("multipleTimersOnUserTask");
        TimerJobQuery jobQuery = managementService.createTimerJobQuery().processInstanceId(pi.getId());
        List<Job> jobs = jobQuery.list();
        assertThat(jobs).hasSize(3);

        // After setting the clock to time '1 hour and 5 seconds', the second
        // timer should fire
        processEngineConfiguration.getClock().setCurrentTime(new Date(startTime.getTime() + ((60 * 60 * 1000) + 5000)));
        waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(7000L, 25L);
        assertThat(jobQuery.count()).isZero();

        // which means that the third task is reached
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("Third Task");
    }

    @Test
    @Deployment
    public void testTimerOnNestingOfSubprocesses() {

        Date testStartTime = processEngineConfiguration.getClock().getCurrentTime();

        runtimeService.startProcessInstanceByKey("timerOnNestedSubprocesses");
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("Inner subprocess task 1", "Inner subprocess task 2");

        // Timer will fire in 2 hours
        processEngineConfiguration.getClock().setCurrentTime(new Date(testStartTime.getTime() + ((2 * 60 * 60 * 1000) + 5000)));
        Job timer = managementService.createTimerJobQuery().singleResult();
        managementService.moveTimerToExecutableJob(timer.getId());
        managementService.executeJob(timer.getId());

        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("task outside subprocess");
    }

    @Test
    @Deployment
    public void testTimerOnSyncMultiInstanceActivity() {

        // Timer doesn't fire
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("myProcess");

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).hasSize(3);

        assertThat(managementService.createTimerJobQuery().count()).isEqualTo(1);

        taskService.complete(tasks.get(0).getId());
        assertThat(managementService.createTimerJobQuery().count()).isEqualTo(1);

        taskService.complete(tasks.get(1).getId());
        assertThat(managementService.createTimerJobQuery().count()).isEqualTo(1);

        taskService.complete(tasks.get(2).getId());
        assertThat(managementService.createTimerJobQuery().count()).isEqualTo(0);


        // Timer does fire
        processInstance = runtimeService.startProcessInstanceByKey("myProcess");
        Job job = managementService.moveTimerToExecutableJob(managementService.createTimerJobQuery().singleResult().getId());
        managementService.executeJob(job.getId());

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).list()).hasSize(0);
        assertThat(managementService.createTimerJobQuery().count()).isEqualTo(0);
    }

    @Test
    @Deployment
    public void testTimerOnAsyncMultiInstanceActivity() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testTimerOnAsyncMultiInstanceActivity");
        assertThat(managementService.createTimerJobQuery().count()).isEqualTo(1);

        // async-continuation into the async multi-instance activity
        for (Job job : managementService.createJobQuery().list()) {
            managementService.executeJob(job.getId());
        }

        assertThat(managementService.createTimerJobQuery().count()).isEqualTo(1);

        Job job = managementService.moveTimerToExecutableJob(managementService.createTimerJobQuery().singleResult().getId());
        managementService.executeJob(job.getId());

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).list()).hasSize(0);
        assertThat(managementService.createTimerJobQuery().count()).isEqualTo(0);
    }
    
    @Test
    @Deployment
    public void testExpressionOnTimer() {
        // Set the clock fixed
        Date startTime = new Date();

        HashMap<String, Object> variables = new HashMap<>();
        variables.put("duration", "PT1H");

        // After process start, there should be a timer created
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("testExpressionOnTimer", variables);

        TimerJobQuery jobQuery = managementService.createTimerJobQuery().processInstanceId(pi.getId());
        List<Job> jobs = jobQuery.list();
        assertThat(jobs).hasSize(1);

        // After setting the clock to time '1 hour and 5 seconds', the second
        // timer should fire
        processEngineConfiguration.getClock().setCurrentTime(new Date(startTime.getTime() + ((60 * 60 * 1000) + 5000)));
        waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(7000L, 25L);
        assertThat(jobQuery.count()).isZero();

        // start execution listener is not executed
        assertThat(listenerExecutedStartEvent).isFalse();
        assertThat(listenerExecutedEndEvent).isTrue();

        // which means the process has ended
        assertProcessEnded(pi.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(pi.getId()).activityId("boundaryTimer").singleResult()).isNotNull();
        }
    }

    @Test
    @Deployment
    public void testExpressionWithJavaDurationOnTimer() {
        // Set the clock fixed
        Date startTime = new Date();

        HashMap<String, Object> variables = new HashMap<>();
        variables.put("duration", Duration.ofHours(1));

        // After process start, there should be a timer created
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("testExpressionOnTimer", variables);

        TimerJobQuery jobQuery = managementService.createTimerJobQuery().processInstanceId(pi.getId());
        List<Job> jobs = jobQuery.list();
        assertThat(jobs).hasSize(1);

        // After setting the clock to time '1 hour and 5 seconds', the second
        // timer should fire
        processEngineConfiguration.getClock().setCurrentTime(new Date(startTime.getTime() + ((60 * 60 * 1000) + 5000)));
        waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(7000L, 25L);
        assertThat(jobQuery.count()).isZero();

        // start execution listener is not executed
        assertThat(listenerExecutedStartEvent).isFalse();
        assertThat(listenerExecutedEndEvent).isTrue();

        // which means the process has ended
        assertProcessEnded(pi.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(pi.getId()).activityId("boundaryTimer").singleResult()).isNotNull();
        }
    }

    @Test
    @Deployment
    public void testNullExpressionOnTimer() {

        HashMap<String, Object> variables = new HashMap<>();
        variables.put("duration", null);

        // After process start, there should be a timer created
        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("testNullExpressionOnTimer", variables))
                .as("Expected wrong due date exception")
                .isInstanceOf(FlowableException.class)
                .hasMessage("Due date could not be determined for timer job null");
    }
    
    @Test
    @Deployment
    public void testNullDueDateWithRepetition() {

        Map<String, Object> variables = new HashMap<>();
        variables.put("initiator", "1");
        variables.put("userId", "2");
        variables.put("dueDate", new Date(new Date().getTime() + 6 * 60 * 60 * 1000)); // 6 hours later
        variables.put("reminderTimeCycle", "0 0 0 1 1 ?");

        String processInstanceId = runtimeService.startProcessInstanceByKey("test-timers", variables).getProcessInstanceId();
        assertThat(processInstanceId).isNotNull();

        TimerJobQuery jobQuery = managementService.createTimerJobQuery().processInstanceId(processInstanceId);
        List<Job> jobs = jobQuery.list();
        assertThat(jobs).hasSize(1);
    }
    
    @Test
    @Deployment
    public void testNullDueDateWithWrongRepetition() {

        Map<String, Object> variables = new HashMap<>();
        variables.put("initiator", "1");
        variables.put("userId", "2");
        variables.put("dueDate", new Date(new Date().getTime() + 6 * 60 * 60 * 1000)); // 6 hours later
        variables.put("reminderTimeCycle", "0 0 0 1 1 ? 2000");

        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("test-timers", variables).getProcessInstanceId())
                .as("Expected wrong due date exception")
                .isInstanceOf(FlowableException.class)
                .hasMessage("Due date could not be determined for timer job 0 0 0 1 1 ? 2000");
    }

    @Test
    @Deployment
    public void testTimerInSingleTransactionProcess() {
        // make sure that if a PI completes in single transaction, JobEntities
        // associated with the execution are deleted.
        // broken before 5.10, see ACT-1133
        runtimeService.startProcessInstanceByKey("timerOnSubprocesses");
        assertThat(managementService.createJobQuery().count()).isZero();
    }

    @Test
    @Deployment
    public void testRepeatingTimerWithCancelActivity() {
        runtimeService.startProcessInstanceByKey("repeatingTimerAndCallActivity");
        assertThat(managementService.createTimerJobQuery().count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().count()).isEqualTo(1);

        // Firing job should cancel the user task, destroy the scope,
        // re-enter the task and recreate the task. A new timer should also be
        // created.
        // This didn't happen before 5.11 (new jobs kept being created). See
        // ACT-1427
        Job job = managementService.createTimerJobQuery().singleResult();
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());
        assertThat(managementService.createTimerJobQuery().count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().count()).isEqualTo(1);
    }

    @Test
    @Deployment
    public void testInfiniteRepeatingTimer() throws Exception {

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyy.MM.dd hh:mm");
        Date currentTime = simpleDateFormat.parse("2015.10.01 11:01");
        processEngineConfiguration.getClock().setCurrentTime(currentTime);

        Map<String, Object> vars = new HashMap<>();
        vars.put("timerString", "R/2015-10-01T11:00:00/PT24H");
        runtimeService.startProcessInstanceByKey("testTimerErrors", vars);

        long twentyFourHours = 24L * 60L * 60L * 1000L;

        Date previousDueDate = null;

        // Move clock, job should fire
        for (int i = 0; i < 30; i++) {
            Job job = managementService.createTimerJobQuery().singleResult();

            // Verify due date
            if (previousDueDate != null) {
                assertThat(job.getDuedate().getTime() - previousDueDate.getTime()).isGreaterThanOrEqualTo(twentyFourHours);
            }
            previousDueDate = job.getDuedate();

            currentTime = new Date(currentTime.getTime() + twentyFourHours + (60 * 1000));
            processEngineConfiguration.getClock().setCurrentTime(currentTime);
            String jobId = managementService.createTimerJobQuery().singleResult().getId();
            managementService.moveTimerToExecutableJob(jobId);
            managementService.executeJob(jobId);
        }

    }

    @Test
    @Deployment
    public void testRepeatTimerDuration() throws Exception {

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyy.MM.dd hh:mm");
        Date currentTime = simpleDateFormat.parse("2015.10.01 11:01");
        processEngineConfiguration.getClock().setCurrentTime(currentTime);

        runtimeService.startProcessInstanceByKey("repeattimertest");

        long twentyFourHours = 24L * 60L * 60L * 1000L;

        Date previousDueDate = null;

        // Move clock, job should fire
        for (int i = 0; i < 3; i++) {
            Job job = managementService.createTimerJobQuery().singleResult();

            // Verify due date
            if (previousDueDate != null) {
                assertThat(job.getDuedate().getTime() - previousDueDate.getTime()).isGreaterThanOrEqualTo(twentyFourHours);
            }
            previousDueDate = job.getDuedate();

            currentTime = new Date(currentTime.getTime() + twentyFourHours + (60 * 1000));
            processEngineConfiguration.getClock().setCurrentTime(currentTime);
            managementService.moveTimerToExecutableJob(job.getId());
            managementService.executeJob(job.getId());
        }

    }

    @Test
    @Deployment
    public void testBoundaryTimerEvent() throws Exception {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyy.MM.dd hh:mm");
        Date currentTime = simpleDateFormat.parse("2015.10.01 11:01");
        processEngineConfiguration.getClock().setCurrentTime(currentTime);

        Map<String, Object> vars = new HashMap<>();
        vars.put("patient", "kermit");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process1", vars);

        List<ActivityInstance> activityInstances = runtimeService.createActivityInstanceQuery().processInstanceId(processInstance.getId()).list();
        assertThat(activityInstances).hasSize(4);
        assertThat(activityInstances)
                .extracting(ActivityInstance::getActivityId)
                .contains("startEvent", "first_task", "boundaryTimerEvent");

        // just wait for 2 seconds to run any job if it's the case
        try {
            waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(2000, 200);
        } catch (Exception ex) {
            // expected exception because the boundary timer event created a timer job to be executed after 10 minutes
        }

        // there should be a userTask waiting for user input
        List<Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("First Task");
        List<Job> jobList = managementService.createTimerJobQuery().list();
        assertThat(jobList).hasSize(1);

        // let's see what's happening after 2 minutes
        // nothing should change since the timer have to executed after 10 minutes
        long twoMinutes = 2L * 60L * 1000L;

        currentTime = new Date(currentTime.getTime() + twoMinutes + 1000L);
        processEngineConfiguration.getClock().setCurrentTime(currentTime);

        try {
            waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(2000, 200);
        } catch (Exception ex) {
            // expected exception because the boundary timer event created a timer job to be executed after 10 minutes
        }

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("First Task");

        jobList = managementService.createTimerJobQuery().list();
        assertThat(jobList).hasSize(1);

        Job job = managementService.createTimerJobQuery().singleResult();
        assertThat(job.getElementId()).isEqualTo("boundaryTimerEvent");
        assertThat(job.getElementName()).isEqualTo("Timer event");

        // after another 8 minutes (the timer will have to execute because it wasa set to be executed @ 10 minutes after process start)
        long tenMinutes = 8L * 60L * 1000L;
        currentTime = new Date(currentTime.getTime() + tenMinutes);
        processEngineConfiguration.getClock().setCurrentTime(currentTime);

        try {
            waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(2000, 200);
        } catch (Exception ex) {
            ex.getCause();
            // expected exception because a new job is prepared
        }

        // there should be only one userTask and it should be the one triggered by the boundary timer event.
        // after the boundary event is triggered there should be no active job.
        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("Second Task");

        activityInstances = runtimeService.createActivityInstanceQuery().processInstanceId(processInstance.getId()).list();
        assertThat(activityInstances).hasSize(6);
        Map<String, ActivityInstance> activityInstanceMap = new HashMap<>();
        for (ActivityInstance activityInstance : activityInstances) {
            activityInstanceMap.put(activityInstance.getActivityId(), activityInstance);
        }
        assertThat(activityInstanceMap.get("startEvent").getEndTime()).isNotNull();
        assertThat(activityInstanceMap.get("first_task").getEndTime()).isNotNull();
        assertThat(activityInstanceMap.get("boundaryTimerEvent").getEndTime()).isNotNull();
        assertThat(activityInstanceMap.get("second_task").getEndTime()).isNull();

        jobList = managementService.createJobQuery().list();
        assertThat(jobList).isEmpty();
        jobList = managementService.createTimerJobQuery().list();
        assertThat(jobList).isEmpty();
    }

    @Test
    @Deployment
    public void testBoundaryTimerEvent2() throws Exception {

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyy.MM.dd hh:mm");
        Date currentTime = simpleDateFormat.parse("2015.10.01 11:01");
        processEngineConfiguration.getClock().setCurrentTime(currentTime);

        runtimeService.startProcessInstanceByKey("timerprocess");

        // just wait for 2 seconds to run any job if it's the case
        try {
            waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(2000, 200);
        } catch (Exception ex) {
            // expected exception because the boundary timer event created a timer job to be executed after 10 minutes
        }

        // there should be a userTask waiting for user input
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("Start");
        List<Job> jobList = managementService.createTimerJobQuery().list();
        assertThat(jobList).hasSize(1);

        // after another 2 minutes
        long tenMinutes = 2L * 60L * 1000L;
        currentTime = new Date(currentTime.getTime() + tenMinutes);
        processEngineConfiguration.getClock().setCurrentTime(currentTime);

        try {
            waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(2000, 200);
        } catch (Exception ex) {
            ex.getCause();
            // expected exception because a new job is prepared
        }

        // there should be no userTask
        tasks = taskService.createTaskQuery().list();
        assertThat(tasks).isEmpty();
        jobList = managementService.createJobQuery().list();
        assertThat(jobList).isEmpty();
        jobList = managementService.createTimerJobQuery().list();
        assertThat(jobList).isEmpty();
    }

    @Test
    @Deployment
    public void testRescheduleBoundaryTimerOnUserTask() {
        // startDate variable set to one hour from now
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, 1);
        long startTimeInMillis = calendar.getTime().getTime();

        Map<String, Object> variables = new HashMap<>();
        variables.put("startDate", calendar.getTime());
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("rescheduleTimer", variables);

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("Task 1");
        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(timerJob).isNotNull();
        long diffInMilliseconds = Math.abs(startTimeInMillis - timerJob.getDuedate().getTime());
        assertThat(diffInMilliseconds).isLessThan(100);

        // reschedule timer for two hours from now
        calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, 2);
        Job rescheduledTimerJob = managementService.rescheduleTimeDateJob(timerJob.getId(), sdf.format(calendar.getTime()));
        assertThat(rescheduledTimerJob).isNotNull();
        assertThat(rescheduledTimerJob.getId()).isNotSameAs(timerJob.getId());

        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(rescheduledTimerJob.getId()).isEqualTo(timerJob.getId());
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
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("Task 1");
        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(timerJob).isNotNull();

        // Move clock forward 2 hours from now
        calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, 2);
        processEngineConfiguration.getClock().setCurrentTime(calendar.getTime());
        waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(2000, 100);

        // Confirm timer has run
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("Task 3");
        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(timerJob).isNull();
    }
    
    @Test
    @Deployment
    public void testBoundaryTimerEventWithCategory() throws Exception {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyy.MM.dd hh:mm");
        Date currentTime = simpleDateFormat.parse("2015.10.01 11:01");
        processEngineConfiguration.getClock().setCurrentTime(currentTime);

        Map<String, Object> vars = new HashMap<>();
        vars.put("myVar", "yes");
        runtimeService.startProcessInstanceByKey("timerProcess", vars);

        // there should be a userTask waiting for user input
        List<Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks).hasSize(1);
        
        List<Job> jobList = managementService.createTimerJobQuery().list();
        assertThat(jobList)
                .extracting(Job::getCategory)
                .containsExactly("myCategory");

        // after another 8 minutes (the timer will have to execute because it was set to be executed @ 10 minutes after process start)
        long afterTenMinutes = 12L * 60L * 1000L;
        currentTime = new Date(currentTime.getTime() + afterTenMinutes);
        processEngineConfiguration.getClock().setCurrentTime(currentTime);

        waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(4000, 200);

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("Second Task");

        assertThat(managementService.createJobQuery().list()).isEmpty();
        assertThat(managementService.createTimerJobQuery().list()).isEmpty();
    }
    
    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/event/timer/BoundaryTimerEventTest.testBoundaryTimerEventWithCategory.bpmn20.xml")
    public void testBoundaryTimerEventWithCategoryEnabledConfigurationSet() throws Exception {
        try {
            processEngineConfiguration.getJobServiceConfiguration().addEnabledJobCategory("wrongValue");
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyy.MM.dd hh:mm");
            Date currentTime = simpleDateFormat.parse("2015.10.01 11:01");
            processEngineConfiguration.getClock().setCurrentTime(currentTime);

            runtimeService.startProcessInstanceByKey("timerProcess");
    
            // after another 8 minutes (the timer will have to execute because it was set to be executed @ 10 minutes after process start)
            long afterTenMinutes = 12L * 60L * 1000L;
            currentTime = new Date(currentTime.getTime() + afterTenMinutes);
            processEngineConfiguration.getClock().setCurrentTime(currentTime);
    
            assertThatThrownBy(() -> waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(4000, 200))
                    .as("job should still be there")
                    .isInstanceOf(Exception.class);

            assertThat(managementService.createTimerJobQuery().list()).hasSize(1);
    
            Task task = taskService.createTaskQuery().singleResult();
            assertThat(task.getName()).isEqualTo("First Task");
            
            processEngineConfiguration.getJobServiceConfiguration().addEnabledJobCategory("myCategory");
            
            waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(4000, 200);
            
            task = taskService.createTaskQuery().singleResult();
            assertThat(task.getName()).isEqualTo("Second Task");
            
            assertThat(managementService.createJobQuery().list()).isEmpty();
            assertThat(managementService.createTimerJobQuery().list()).isEmpty();
        } finally {
            processEngineConfiguration.getJobServiceConfiguration().setEnabledJobCategories(null);
        }
    }
    
    @Test
    @Deployment
    public void testBoundaryTimerEventWithCategoryExpression() throws Exception {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyy.MM.dd hh:mm");
        Date currentTime = simpleDateFormat.parse("2015.10.01 11:01");
        processEngineConfiguration.getClock().setCurrentTime(currentTime);

        Map<String, Object> vars = new HashMap<>();
        vars.put("myVar", "yes");
        vars.put("categoryValue", "testValue");
        runtimeService.startProcessInstanceByKey("timerProcess", vars);
        
        List<Job> jobList = managementService.createTimerJobQuery().list();
        assertThat(jobList)
                .extracting(Job::getCategory)
                .containsExactly("testValue");

        // after another 8 minutes (the timer will have to execute because it was set to be executed @ 10 minutes after process start)
        long afterTenMinutes = 12L * 60L * 1000L;
        currentTime = new Date(currentTime.getTime() + afterTenMinutes);
        processEngineConfiguration.getClock().setCurrentTime(currentTime);

        waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(4000, 200);

        List<Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("Second Task");
    }
    
    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/event/timer/BoundaryTimerEventTest.testBoundaryTimerEventWithCategoryExpression.bpmn20.xml")
    public void testBoundaryTimerEventWithCategoryExpressionEnabledConfigurationSet() throws Exception {
        try {
            processEngineConfiguration.getJobServiceConfiguration().addEnabledJobCategory("testValue");
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyy.MM.dd hh:mm");
            Date currentTime = simpleDateFormat.parse("2015.10.01 11:01");
            processEngineConfiguration.getClock().setCurrentTime(currentTime);

            Map<String, Object> vars = new HashMap<>();
            vars.put("categoryValue", "testValue");
            runtimeService.startProcessInstanceByKey("timerProcess", vars);
    
            // after another 8 minutes (the timer will have to execute because it was set to be executed @ 10 minutes after process start)
            long afterTenMinutes = 12L * 60L * 1000L;
            currentTime = new Date(currentTime.getTime() + afterTenMinutes);
            processEngineConfiguration.getClock().setCurrentTime(currentTime);
    
            waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(4000, 200);
            
            Task task = taskService.createTaskQuery().singleResult();
            assertThat(task.getName()).isEqualTo("Second Task");
            
            assertThat(managementService.createJobQuery().list()).isEmpty();
            assertThat(managementService.createTimerJobQuery().list()).isEmpty();
        } finally {
            processEngineConfiguration.getJobServiceConfiguration().setEnabledJobCategories(null);
        }
    }

    @Test
    @Deployment
    public void testRescheduleRepeatBoundaryTimer() {
        // startDate variable set to one hour from now
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, 1);
        long startTimeInMillis = calendar.getTime().getTime();

        Map<String, Object> variables = new HashMap<>();
        variables.put("startDate", calendar.getTime());
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("rescheduleTimer", variables);

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("Task 1");
        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(timerJob).isNotNull();
        long diffInMilliseconds = Math.abs(startTimeInMillis - timerJob.getDuedate().getTime());
        assertThat(diffInMilliseconds).isLessThan(100);

        // reschedule timer for two hours from now
        calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, 2);
        String timeCycle = "R/PT2H";
        Job rescheduledTimerJob = managementService.rescheduleTimerJob(timerJob.getId(), null, null, timeCycle, null, null);
        assertThat(rescheduledTimerJob).isNotNull();
        assertThat(rescheduledTimerJob.getId()).isNotSameAs(timerJob.getId());

        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(rescheduledTimerJob.getId()).isEqualTo(timerJob.getId());
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
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("Task 1");
        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(timerJob).isNotNull();

        // Move clock forward 2 hours from now
        calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, 2);
        processEngineConfiguration.getClock().setCurrentTime(calendar.getTime());
        waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(2000, 100);

        // Confirm timer has run
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("Task 3");
        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(timerJob).isNull();
    }

    @Test
    @Deployment
    public void testRescheduleBoundaryTimerOnSubProcess() {
        // startDate variable set to one hour from now
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, 1);
        long startTimeInMillis = calendar.getTime().getTime();

        Map<String, Object> variables = new HashMap<>();
        variables.put("startDate", calendar.getTime());
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("rescheduleTimer", variables);

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("Task 1");
        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(timerJob).isNotNull();
        long diffInMilliseconds = Math.abs(startTimeInMillis - timerJob.getDuedate().getTime());
        assertThat(diffInMilliseconds).isLessThan(100);

        // reschedule timer for two hours from now
        calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, 2);

        managementService.rescheduleTimeDateJob(timerJob.getId(), sdf.format(calendar.getTime()));

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
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("Task 1");
        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(timerJob).isNotNull();

        // Move clock forward 2 hours from now
        calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, 2);
        processEngineConfiguration.getClock().setCurrentTime(calendar.getTime());
        waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(2000, 100);

        // Confirm timer has run
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("Task 3");
        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(timerJob).isNull();
    }

    @Test
    @Deployment
    public void test3BoundaryTimerEvents() throws Exception {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("threeTimersProcess");
        assertThat(managementService.createJobQuery().processInstanceId(processInstance.getId()).count()).isZero();
        assertThat(managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(2);
        assertThat(managementService.createTimerJobQuery().executable().processInstanceId(processInstance.getId()).count()).isZero();
        
        // there should be a userTask waiting for user input
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("First Task");

        
        // first job fires after 1 hour
        Calendar currentCal = processEngineConfiguration.getClock().getCurrentCalendar();
        currentCal.add(Calendar.MINUTE, 61);
        processEngineConfiguration.getClock().setCurrentCalendar(currentCal);
        
        assertThat(managementService.createTimerJobQuery().executable().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
        
        Job timerJob = managementService.createTimerJobQuery().executable().processInstanceId(processInstance.getId()).singleResult();
        managementService.moveTimerToExecutableJob(timerJob.getId());
        
        assertThat(managementService.createJobQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
        assertThat(managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
        
        timerJob = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();
        
        managementService.executeJob(timerJob.getId());
        
        tasks = taskService.createTaskQuery().list();
        assertThat(tasks).hasSize(2);
        assertThat(managementService.createJobQuery().processInstanceId(processInstance.getId()).count()).isZero();
        assertThat(managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(2);
        assertThat(managementService.createTimerJobQuery().executable().processInstanceId(processInstance.getId()).count()).isZero();
        
        // second job fires after 1 hour
        currentCal.add(Calendar.MINUTE, 61);
        processEngineConfiguration.getClock().setCurrentCalendar(currentCal);
        
        assertThat(managementService.createTimerJobQuery().executable().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
        
        timerJob = managementService.createTimerJobQuery().executable().processInstanceId(processInstance.getId()).singleResult();
        managementService.moveTimerToExecutableJob(timerJob.getId());
        
        assertThat(managementService.createJobQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
        assertThat(managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);

        timerJob = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();
        
        managementService.executeJob(timerJob.getId());
        
        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("First Task");

        // last timer fires after another 2 hours
        currentCal.add(Calendar.MINUTE, 121);
        processEngineConfiguration.getClock().setCurrentCalendar(currentCal);
        
        assertThat(managementService.createTimerJobQuery().executable().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
        
        timerJob = managementService.createTimerJobQuery().executable().processInstanceId(processInstance.getId()).singleResult();
        managementService.moveTimerToExecutableJob(timerJob.getId());
        
        assertThat(managementService.createJobQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
        assertThat(managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).count()).isZero();
        
        timerJob = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();
        
        managementService.executeJob(timerJob.getId());
        
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
        
        processEngineConfiguration.getClock().reset();
    }

    @Test
    @Deployment
    public void test2Boundary1IntermediateTimerEvents() throws Exception {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("threeTimersProcess");

        assertThat(managementService.createJobQuery().processInstanceId(processInstance.getId()).count()).isZero();
        assertThat(managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(2);
        assertThat(managementService.createTimerJobQuery().executable().processInstanceId(processInstance.getId()).count()).isZero();

        // there should be a userTask waiting for user input
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("First Task");

        // first job fires after 1 hour
        Calendar currentCal = processEngineConfiguration.getClock().getCurrentCalendar();
        currentCal.add(Calendar.MINUTE, 61);
        processEngineConfiguration.getClock().setCurrentCalendar(currentCal);
        
        assertThat(managementService.createTimerJobQuery().executable().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
        
        Job timerJob = managementService.createTimerJobQuery().executable().processInstanceId(processInstance.getId()).singleResult();
        managementService.moveTimerToExecutableJob(timerJob.getId());
        
        assertThat(managementService.createJobQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
        assertThat(managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
        
        timerJob = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();
        
        managementService.executeJob(timerJob.getId());

        tasks = taskService.createTaskQuery().taskName("Reminder Task").list();
        assertThat(tasks).hasSize(1);
        taskService.complete(tasks.get(0).getId());
        
        assertThat(managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(2);

        // intermediate timer catch event job fires after 1 hour
        currentCal.add(Calendar.MINUTE, 61);
        processEngineConfiguration.getClock().setCurrentCalendar(currentCal);
        
        assertThat(managementService.createTimerJobQuery().executable().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
        
        timerJob = managementService.createTimerJobQuery().executable().processInstanceId(processInstance.getId()).singleResult();
        managementService.moveTimerToExecutableJob(timerJob.getId());
        
        assertThat(managementService.createJobQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
        assertThat(managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
        
        timerJob = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();
        
        managementService.executeJob(timerJob.getId());
        
        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("First Task");
        
        // last timer fires after another 2 hours
        currentCal.add(Calendar.MINUTE, 121);
        processEngineConfiguration.getClock().setCurrentCalendar(currentCal);
        
        assertThat(managementService.createTimerJobQuery().executable().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
        
        timerJob = managementService.createTimerJobQuery().executable().processInstanceId(processInstance.getId()).singleResult();
        managementService.moveTimerToExecutableJob(timerJob.getId());
        
        assertThat(managementService.createJobQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
        assertThat(managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).count()).isZero();
        
        timerJob = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();
        
        managementService.executeJob(timerJob.getId());
        
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
        
        processEngineConfiguration.getClock().reset();
    }

}
