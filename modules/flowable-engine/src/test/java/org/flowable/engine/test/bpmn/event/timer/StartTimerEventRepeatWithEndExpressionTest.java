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

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.impl.runtime.Clock;
import org.flowable.common.engine.impl.util.DefaultClockImpl;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.api.event.TestFlowableEntityEventListener;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Vasile Dirla
 */
public class StartTimerEventRepeatWithEndExpressionTest extends PluggableFlowableTestCase {

    private TestFlowableEntityEventListener listener;

    @BeforeEach
    protected void setUp() throws Exception {
        listener = new TestFlowableEntityEventListener(Job.class);
        processEngineConfiguration.getEventDispatcher().addEventListener(listener);
    }

    @AfterEach
    protected void tearDown() throws Exception {
        if (listener != null) {
            processEngineConfiguration.getEventDispatcher().removeEventListener(listener);
        }
    }

    /**
     * Timer repetition
     */
    @Test
    public void testCycleDateStartTimerEvent() throws Exception {
        Clock previousClock = processEngineConfiguration.getClock();

        Clock testClock = new DefaultClockImpl();

        processEngineConfiguration.setClock(testClock);

        // We need to make sure the time ends on .000, .003 or .007 due to SQL Server rounding to that
        Instant instant = LocalDate.of(2025, Month.DECEMBER, 10).atStartOfDay(ZoneId.systemDefault()).toInstant().truncatedTo(ChronoUnit.SECONDS).plusMillis(540);
        testClock.setCurrentTime(Date.from(instant));

        // deploy the process
        repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/bpmn/event/timer/StartTimerEventRepeatWithEndExpressionTest.testCycleDateStartTimerEvent.bpmn20.xml").deploy();
        assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(1);

        // AFTER DEPLOYMENT
        // when the process is deployed there will be created a timerStartEvent
        // job which will wait to be executed.
        List<Job> jobs = managementService.createTimerJobQuery().list();
        assertThat(jobs).hasSize(1);

        // dueDate should be after 24 hours from the process deployment
        Instant dueDateInstant = instant.plus(1, ChronoUnit.DAYS);

        // check the due date is inside the 2 seconds range
        assertThat(Duration.between(jobs.get(0).getDuedate().toInstant(), dueDateInstant)).isLessThanOrEqualTo(Duration.ofSeconds(2));

        // No process instances
        List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().list();
        assertThat(processInstances).isEmpty();

        // No tasks
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks).isEmpty();

        // ADVANCE THE CLOCK
        // advance the clock to 11 dec -> the system will execute the pending
        // job and will create a new one
        moveByMinutes(60 * 24);
        waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(5000L, 200);

        // there must be a pending job because the endDate is not reached yet");
        assertThat(managementService.createTimerJobQuery().count()).isEqualTo(1);

        // After the first startEvent Execution should be one process instance started
        processInstances = runtimeService.createProcessInstanceQuery().list();
        assertThat(processInstances).hasSize(1);

        // one task to be executed (the userTask "Task A")
        tasks = taskService.createTaskQuery().list();
        assertThat(tasks).hasSize(1);

        // one new job will be created (and the old one will be deleted after execution)
        jobs = managementService.createTimerJobQuery().list();
        assertThat(jobs).hasSize(1);

        // 12th December 2025
        dueDateInstant = instant.plus(2, ChronoUnit.DAYS);
        assertThat(Duration.between(jobs.get(0).getDuedate().toInstant(), dueDateInstant)).isLessThanOrEqualTo(Duration.ofSeconds(2));

        // ADVANCE THE CLOCK SO THE END DATE WILL BE REACHED
        // 12 dec (last execution)
        moveByMinutes(60 * 24);
        waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(5000, 200);

        // After the second startEvent Execution should have 2 process instances started
        // (since the first one was not completed)
        processInstances = runtimeService.createProcessInstanceQuery().list();
        assertThat(processInstances).hasSize(2);

        // Because the endDate 12.dec.2025 is reached
        // the current job will be deleted after execution and a new one will
        // not be created.
        jobs = managementService.createTimerJobQuery().list();
        assertThat(jobs).isEmpty();
        jobs = managementService.createJobQuery().list();
        assertThat(jobs).isEmpty();

        // 2 tasks to be executed (the userTask "Task A")
        // one task for each process instance
        tasks = taskService.createTaskQuery().list();
        assertThat(tasks).hasSize(2);

        // count "timer fired" events
        int timerFiredCount = 0;
        List<FlowableEvent> eventsReceived = listener.getEventsReceived();
        for (FlowableEvent eventReceived : eventsReceived) {
            if (FlowableEngineEventType.TIMER_FIRED == eventReceived.getType()) {
                timerFiredCount++;
            }
        }

        // count "entity created" events
        int eventCreatedCount = 0;
        for (FlowableEvent eventReceived : eventsReceived) {
            if (FlowableEngineEventType.ENTITY_CREATED == eventReceived.getType()) {
                eventCreatedCount++;
            }
        }

        // count "entity deleted" events
        int eventDeletedCount = 0;
        for (FlowableEvent eventReceived : eventsReceived) {
            if (FlowableEngineEventType.ENTITY_DELETED == eventReceived.getType()) {
                eventDeletedCount++;
            }
        }
        assertThat(timerFiredCount).isEqualTo(2); // 2 timers fired
        assertThat(eventCreatedCount).isEqualTo(4); // 4 jobs created, each timer creates one timer and one executable job
        assertThat(eventDeletedCount).isEqualTo(4); // 4 jobs deleted, each timer results in deleting one timer and one executable job

        // for each processInstance
        // let's complete the userTasks where the process is hanging in order to
        // complete the processes.
        for (ProcessInstance processInstance : processInstances) {
            tasks = taskService.createTaskQuery().processInstanceId(processInstance.getProcessInstanceId()).list();
            assertThat(tasks)
                    .extracting(Task::getName)
                    .containsExactly("Task A");
            taskService.complete(tasks.get(0).getId());
        }

        // now All the process instances should be completed
        processInstances = runtimeService.createProcessInstanceQuery().list();
        assertThat(processInstances).isEmpty();

        // no jobs
        jobs = managementService.createTimerJobQuery().list();
        assertThat(jobs).isEmpty();
        jobs = managementService.createJobQuery().list();
        assertThat(jobs).isEmpty();

        // no tasks
        tasks = taskService.createTaskQuery().list();
        assertThat(tasks).isEmpty();

        listener.clearEventsReceived();
        processEngineConfiguration.setClock(previousClock);

        repositoryService.deleteDeployment(repositoryService.createDeploymentQuery().singleResult().getId(), true);

    }

    private void moveByMinutes(int minutes) throws Exception {
        processEngineConfiguration.getClock().setCurrentTime(new Date(processEngineConfiguration.getClock().getCurrentTime().getTime() + ((minutes * 60 * 1000))));
    }

}
