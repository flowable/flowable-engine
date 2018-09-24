package org.activiti.engine.test.bpmn.event.timer;

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

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.activiti.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.runtime.Clock;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

/**
 * @author Vasile Dirla
 */
public class IntermediateTimerEventRepeatWithEndTest extends PluggableFlowableTestCase {

    @Deployment
    public void testRepeatWithEnd() throws Throwable {
        Clock clock = processEngineConfiguration.getClock();
        Calendar calendar = Calendar.getInstance();
        Date baseTime = calendar.getTime();

        // expect to stop boundary jobs after 20 minutes
        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();

        calendar.setTime(baseTime);
        calendar.add(Calendar.HOUR, 2);
        // expect to wait after competing task B for 1 hour even I set the end date for 2 hours (the expression will trigger the execution)
        DateTime dt = new DateTime(calendar.getTime());
        String dateStr1 = fmt.print(dt);

        calendar.setTime(baseTime);
        calendar.add(Calendar.HOUR, 1);
        calendar.add(Calendar.MINUTE, 30);
        // expect to wait after competing task B for 1 hour and 30 minutes (the end date will be reached, the expression will not be considered)
        dt = new DateTime(calendar.getTime());
        String dateStr2 = fmt.print(dt);

        // reset the timer
        Calendar nextTimeCal = Calendar.getInstance();
        nextTimeCal.setTime(baseTime);
        clock.setCurrentTime(nextTimeCal.getTime());
        processEngineConfiguration.setClock(clock);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("repeatWithEnd");

        runtimeService.setVariable(processInstance.getId(), "EndDateForCatch1", dateStr1);
        runtimeService.setVariable(processInstance.getId(), "EndDateForCatch2", dateStr2);

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertEquals(1, tasks.size());

        tasks = taskService.createTaskQuery().list();
        assertEquals(1, tasks.size());
        org.flowable.task.api.Task task = tasks.get(0);
        assertEquals("Task A", task.getName());

        // Test Timer Catch Intermediate Events after completing org.flowable.task.service.Task B (endDate not reached but it will be executed according to the expression)
        taskService.complete(task.getId());

        waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(2000, 500);

        nextTimeCal.add(Calendar.HOUR, 1); // after 1 hour the event must be triggered and the flow will go to the next step
        clock.setCurrentTime(nextTimeCal.getTime());
        processEngineConfiguration.setClock(clock);

        waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(2000, 200);
        // expect to execute because the time is reached.

        List<Job> jobs = managementService.createJobQuery().list();
        assertEquals(0, jobs.size());
        jobs = managementService.createTimerJobQuery().list();
        assertEquals(0, jobs.size());

        tasks = taskService.createTaskQuery().list();
        assertEquals(1, tasks.size());
        task = tasks.get(0);
        assertEquals("Task C", task.getName());

        // Test Timer Catch Intermediate Events after completing org.flowable.task.service.Task C
        taskService.complete(task.getId());
        nextTimeCal.add(Calendar.MINUTE, 30); // after 1H 30 minutes from process start, the timer will trigger because of the endDate
        clock.setCurrentTime(nextTimeCal.getTime());
        processEngineConfiguration.setClock(clock);

        waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(2000, 500);
        // expect to execute because the end time is reached.

        if (processEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricProcessInstance historicInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .singleResult();

            assertNotNull(historicInstance.getEndTime());
        }

        // now all the process instances should be completed
        List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().list();
        assertEquals(0, processInstances.size());

        // no jobs
        jobs = managementService.createJobQuery().list();
        assertEquals(0, jobs.size());
        jobs = managementService.createTimerJobQuery().list();
        assertEquals(0, jobs.size());

        // no tasks
        tasks = taskService.createTaskQuery().list();
        assertEquals(0, tasks.size());

        processEngineConfiguration.resetClock();
    }

}
