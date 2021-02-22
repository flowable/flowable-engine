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
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.flowable.task.api.Task;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.jupiter.api.Test;

public class BoundaryTimerEventRepeatCompatibilityTest extends TimerEventCompatibilityTest {

    @Test
    @Deployment
    public void testRepeatWithoutEnd() throws Throwable {

        // We need to make sure the time ends on .000, .003 or .007 due to SQL Server rounding to that
        Instant baseInstant = Instant.now().truncatedTo(ChronoUnit.SECONDS).plusMillis(337);

        // expect to stop boundary jobs after 20 minutes
        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        DateTime dt = new DateTime(new DateTime(baseInstant.plus(20, ChronoUnit.MINUTES).getEpochSecond()));
        String dateStr = fmt.print(dt);

        // reset the timer
        Instant nextTimeInstant = baseInstant;
        processEngineConfiguration.getClock().setCurrentTime(Date.from(nextTimeInstant));

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("repeatWithEnd");

        runtimeService.setVariable(processInstance.getId(), "EndDateForBoundary", dateStr);

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsOnly("Task A");

        // Test Boundary Events
        // complete will cause timer to be created
        taskService.complete(tasks.get(0).getId());

        List<Job> jobs = managementService.createTimerJobQuery().list();
        assertThat(jobs).hasSize(1);

        // change the job in old mode (the configuration should not be json in
        // "old mode" but a simple string).
        TimerJobEntity job = (TimerJobEntity) jobs.get(0);
        changeConfigurationToPlainText(job);

        // boundary events

        waitForJobExecutorToProcessAllJobs(7000, 100);

        // a new job must be prepared because there are 10 repeats 2 seconds interval
        jobs = managementService.createTimerJobQuery().list();
        assertThat(jobs).hasSize(1);

        for (int i = 0; i < 9; i++) {
            nextTimeInstant = nextTimeInstant.plus(2, ChronoUnit.SECONDS);
            processEngineConfiguration.getClock().setCurrentTime(Date.from(nextTimeInstant));
            waitForJobExecutorToProcessAllJobs(7000, 100);
            // a new job must be prepared because there are 10 repeats 2 seconds interval

            jobs = managementService.createTimerJobQuery().list();
            assertThat(jobs).hasSize(1);
        }

        nextTimeInstant = nextTimeInstant.plus(2, ChronoUnit.SECONDS);
        processEngineConfiguration.getClock().setCurrentTime(Date.from(nextTimeInstant));

        assertThatCode(() -> { waitForJobExecutorToProcessAllJobs(7000, 100); })
                .as("Should not have any other jobs because the endDate is reached")
                .doesNotThrowAnyException();

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsOnly("Task B");
        taskService.complete(tasks.get(0).getId());

        assertThatCode(() -> { waitForJobExecutorToProcessAllJobs(7000, 500); })
                .as("No jobs should be active here.")
                .doesNotThrowAnyException();

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
