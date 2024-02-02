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
package org.flowable.engine.test.bpmn.event.timer.compatibility;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.jupiter.api.Test;

public class IntermediateTimerEventRepeatCompatibilityTest extends TimerEventCompatibilityTest {

    @Test
    @Deployment
    public void testRepeatWithEnd() throws Throwable {

        // We need to make sure the time ends on .000, .003 or .007 due to SQL Server rounding to that
        Instant baseInstant = Instant.now().truncatedTo(ChronoUnit.SECONDS).plusMillis(337);

        // expect to stop boundary jobs after 20 minutes
        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();

        // expect to wait after completing task A for 1 hour even I set the end
        // date for 2 hours (the expression will trigger the execution)
        String endDateForIntermediate1 = fmt.print(new DateTime(baseInstant.plus(2, ChronoUnit.HOURS).getEpochSecond()));

        // expect to wait after completing task B for 1 hour and 30 minutes (the
        // end date will be reached, the expression will not be considered)
        String endDateForIntermediate2 = fmt.print(new DateTime(baseInstant.plus(1, ChronoUnit.HOURS).plus(30, ChronoUnit.MINUTES).getEpochSecond()));

        // reset the timer
        Instant nextTimeInstant = baseInstant;
        processEngineConfiguration.getClock().setCurrentTime(Date.from(nextTimeInstant));

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("repeatWithEnd");

        runtimeService.setVariable(processInstance.getId(), "EndDateForCatch1", endDateForIntermediate1);
        runtimeService.setVariable(processInstance.getId(), "EndDateForCatch2", endDateForIntermediate2);

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks).hasSize(1);

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsOnly("Task A");

        // Test Timer Catch Intermediate Events after completing org.flowable.task.service.Task B (endDate
        // not reached but it will be executed according to the expression)
        taskService.complete(tasks.get(0).getId());

        waitForJobExecutorToProcessAllJobs(2000, 500);
        // Expected that job isn't executed because the timer is in t0
        assertThat(managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult()).isNotNull();

        nextTimeInstant = nextTimeInstant.plus(1, ChronoUnit.HOURS); // after 1 hour the event must be triggered and the flow will go to the next step
        processEngineConfiguration.getClock().setCurrentTime(Date.from(nextTimeInstant));

        waitForJobExecutorToProcessAllJobs(2000, 500);
        // expect to execute because the time is reached.

        List<Job> jobs = managementService.createJobQuery().list();
        assertThat(jobs).isEmpty();

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsOnly("Task C");

        // Test Timer Catch Intermediate Events after completing org.flowable.task.service.Task C
        taskService.complete(tasks.get(0).getId());
        nextTimeInstant = nextTimeInstant.plus(1, ChronoUnit.HOURS); // after 1H 40 minutes from process start, the timer will trigger because of the endDate
        processEngineConfiguration.getClock().setCurrentTime(Date.from(nextTimeInstant));

        waitForJobExecutorToProcessAllJobs(2000, 500);
        // expect to execute because the end time is reached.

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricProcessInstance historicInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
            assertThat(historicInstance.getEndTime()).isNotNull();
        }

        // now All the process instances should be completed
        List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().list();
        assertThat(processInstances).isEmpty();

        // no jobs
        jobs = managementService.createJobQuery().list();
        assertThat(jobs).isEmpty();

        jobs = managementService.createTimerJobQuery().list();
        assertThat(jobs).isEmpty();

        // no tasks
        tasks = taskService.createTaskQuery().list();
        assertThat(tasks).isEmpty();

    }

}
