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

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.jupiter.api.Test;

/**
 * @author Vasile Dirla
 */
public class BoundaryTimerEventRepeatWithEndTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testRepeatWithEnd() throws Throwable {

        Calendar calendar = Calendar.getInstance();
        Date baseTime = calendar.getTime();

        calendar.add(Calendar.MINUTE, 20);
        // expect to stop boundary jobs after 20 minutes
        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        DateTime dt = new DateTime(calendar.getTime());
        String dateStr = fmt.print(dt);

        // reset the timer
        Calendar nextTimeCal = Calendar.getInstance();
        nextTimeCal.setTime(baseTime);
        processEngineConfiguration.getClock().setCurrentTime(nextTimeCal.getTime());

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("repeatWithEnd");

        runtimeService.setVariable(processInstance.getId(), "EndDateForBoundary", dateStr);

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("Task A");

        // Test Boundary Events
        // complete will cause timer to be created
        taskService.complete(tasks.get(0).getId());

        List<Job> jobs = managementService.createTimerJobQuery().list();
        assertThat(jobs).hasSize(1);

        // boundary events
        Job executableJob = managementService.moveTimerToExecutableJob(jobs.get(0).getId());
        managementService.executeJob(executableJob.getId());

        assertThat(managementService.createJobQuery().list()).isEmpty();
        jobs = managementService.createTimerJobQuery().list();
        assertThat(jobs).hasSize(1);

        nextTimeCal.add(Calendar.MINUTE, 15); // after 15 minutes
        processEngineConfiguration.getClock().setCurrentTime(nextTimeCal.getTime());

        executableJob = managementService.moveTimerToExecutableJob(jobs.get(0).getId());
        managementService.executeJob(executableJob.getId());

        assertThat(managementService.createJobQuery().list()).isEmpty();
        jobs = managementService.createTimerJobQuery().list();
        assertThat(jobs).hasSize(1);

        nextTimeCal.add(Calendar.MINUTE, 5); // after another 5 minutes (20 minutes and 1 second from the baseTime) the BoundaryEndTime is reached
        nextTimeCal.add(Calendar.SECOND, 1);
        processEngineConfiguration.getClock().setCurrentTime(nextTimeCal.getTime());

        executableJob = managementService.moveTimerToExecutableJob(jobs.get(0).getId());
        managementService.executeJob(executableJob.getId());

        jobs = managementService.createTimerJobQuery().list();
        assertThat(jobs).isEmpty();
        jobs = managementService.createJobQuery().list();
        assertThat(jobs).isEmpty();

        tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("Task B");
        taskService.complete(tasks.get(0).getId());

        jobs = managementService.createTimerJobQuery().list();
        assertThat(jobs).isEmpty();
        jobs = managementService.createJobQuery().list();
        assertThat(jobs).isEmpty();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricProcessInstance historicInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .singleResult();
            assertThat(historicInstance.getEndTime()).isNotNull();
        }

        // now all the process instances should be completed
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
