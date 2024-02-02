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
package org.flowable.engine.test.api.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.impl.runtime.Clock;
import org.flowable.common.engine.impl.util.DefaultClockImpl;
import org.flowable.engine.delegate.event.FlowableActivityEvent;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test case for all {@link FlowableEvent}s related to jobs.
 *
 * @author Frederik Heremans
 */
public class JobEventsTest extends PluggableFlowableTestCase {

    private TestFlowableEntityEventListener listener;
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    /**
     * Test create, update and delete events of jobs entities.
     */
    @Test
    @Deployment
    public void testJobEntityEvents() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testJobEvents");
        Job theJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(theJob).isNotNull();

        // Check if create-event has been dispatched
        assertThat(listener.getEventsReceived()).hasSize(3);
        FlowableEngineEvent event = (FlowableEngineEvent) listener.getEventsReceived().get(0);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
        checkEventContext(event, theJob);

        event = (FlowableEngineEvent) listener.getEventsReceived().get(1);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_INITIALIZED);
        checkEventContext(event, theJob);

        event = (FlowableEngineEvent) listener.getEventsReceived().get(2);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.TIMER_SCHEDULED);
        checkEventContext(event, theJob);

        listener.clearEventsReceived();

        // Update the job-entity. Check if update event is dispatched with update job entity
        managementService.setTimerJobRetries(theJob.getId(), 5);
        assertThat(listener.getEventsReceived()).hasSize(1);
        event = (FlowableEngineEvent) listener.getEventsReceived().get(0);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_UPDATED);
        Job updatedJob = (Job) ((FlowableEntityEvent) event).getEntity();
        assertThat(updatedJob.getRetries()).isEqualTo(5);
        checkEventContext(event, theJob);

        checkEventCount(0, FlowableEngineEventType.TIMER_SCHEDULED);
        listener.clearEventsReceived();

        // Force timer to fire
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        processEngineConfiguration.getClock().setCurrentTime(tomorrow.getTime());
        String jobId = managementService.createTimerJobQuery().singleResult().getId();
        managementService.moveTimerToExecutableJob(jobId);
        managementService.executeJob(jobId);

        // Check delete-event has been dispatched
        assertThat(listener.getEventsReceived()).hasSize(6);

        event = (FlowableEngineEvent) listener.getEventsReceived().get(0);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
        checkEventContext(event, theJob);

        event = (FlowableEngineEvent) listener.getEventsReceived().get(1);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_INITIALIZED);
        checkEventContext(event, theJob);

        // First, a timer fired event has been dispatched
        event = (FlowableEngineEvent) listener.getEventsReceived().get(3);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.TIMER_FIRED);
        checkEventContext(event, theJob);

        // Next, a delete event has been dispatched
        event = (FlowableEngineEvent) listener.getEventsReceived().get(4);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_DELETED);
        checkEventContext(event, theJob);

        // Finally, a complete event has been dispatched
        event = (FlowableEngineEvent) listener.getEventsReceived().get(5);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.JOB_EXECUTION_SUCCESS);
        checkEventContext(event, theJob);

        checkEventCount(0, FlowableEngineEventType.TIMER_SCHEDULED);
    }

    /**
     * Test job canceled and timer scheduled events for reschedule.
     */
    @Test
    @Deployment
    public void testJobEntityEventsForRescheduleTimer() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testJobEventsForReschedule");
        Job originalTimerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(originalTimerJob).isNotNull();

        // Check if create-event has been dispatched
        assertThat(listener.getEventsReceived()).hasSize(3);
        FlowableEngineEvent event = (FlowableEngineEvent) listener.getEventsReceived().get(0);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
        checkEventContext(event, originalTimerJob);

        event = (FlowableEngineEvent) listener.getEventsReceived().get(1);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_INITIALIZED);
        checkEventContext(event, originalTimerJob);

        event = (FlowableEngineEvent) listener.getEventsReceived().get(2);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.TIMER_SCHEDULED);
        checkEventContext(event, originalTimerJob);

        listener.clearEventsReceived();

        // Reschedule the timer
        managementService.rescheduleTimeDurationJob(originalTimerJob.getId(), "PT2H");

        Job rescheduledJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(rescheduledJob).isNotNull();
        assertThat(rescheduledJob.getId()).isNotSameAs(originalTimerJob.getId());

        assertThat(listener.getEventsReceived()).hasSize(5);
        event = (FlowableEngineEvent) listener.getEventsReceived().get(0);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_DELETED);
        checkEventContext(event, originalTimerJob);

        Job deletedJob = (Job) ((FlowableEntityEvent) event).getEntity();
        checkEventContext(event, deletedJob);

        event = (FlowableEngineEvent) listener.getEventsReceived().get(1);
        Job newJob = (Job) ((FlowableEntityEvent) event).getEntity();
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
        checkEventContext(event, newJob);
        checkEventContext(event, rescheduledJob);
        assertThat(rescheduledJob.getId()).isEqualTo(newJob.getId());

        event = (FlowableEngineEvent) listener.getEventsReceived().get(2);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_INITIALIZED);
        newJob = (Job) ((FlowableEntityEvent) event).getEntity();
        checkEventContext(event, newJob);

        event = (FlowableEngineEvent) listener.getEventsReceived().get(3);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.JOB_RESCHEDULED);
        Job newTimerJob = (Job) ((FlowableEntityEvent) event).getEntity();
        checkEventContext(event, rescheduledJob);
        assertThat(newTimerJob.getId()).isEqualTo(rescheduledJob.getId());
        assertThat(simpleDateFormat.format(newTimerJob.getDuedate())).isEqualTo(simpleDateFormat.format(rescheduledJob.getDuedate()));

        event = (FlowableEngineEvent) listener.getEventsReceived().get(4);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.TIMER_SCHEDULED);
        newJob = (Job) ((FlowableEntityEvent) event).getEntity();
        checkEventContext(event, newJob);

        listener.clearEventsReceived();

        // Force timer to fire
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        processEngineConfiguration.getClock().setCurrentTime(tomorrow.getTime());
        managementService.moveTimerToExecutableJob(rescheduledJob.getId());
        managementService.executeJob(rescheduledJob.getId());

        // Check delete-event has been dispatched
        assertThat(listener.getEventsReceived()).hasSize(6);

        event = (FlowableEngineEvent) listener.getEventsReceived().get(0);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
        checkEventContext(event, rescheduledJob);

        event = (FlowableEngineEvent) listener.getEventsReceived().get(1);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_INITIALIZED);
        checkEventContext(event, rescheduledJob);

        // First, a timer fired event has been dispatched
        event = (FlowableEngineEvent) listener.getEventsReceived().get(3);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.TIMER_FIRED);
        checkEventContext(event, rescheduledJob);

        // Next, a delete event has been dispatched
        event = (FlowableEngineEvent) listener.getEventsReceived().get(4);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_DELETED);
        checkEventContext(event, rescheduledJob);

        // Finally, a complete event has been dispatched
        event = (FlowableEngineEvent) listener.getEventsReceived().get(5);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.JOB_EXECUTION_SUCCESS);
        checkEventContext(event, rescheduledJob);

        checkEventCount(0, FlowableEngineEventType.TIMER_SCHEDULED);
    }

    /**
     * Timer repetition
     */
    @Test
    @Deployment
    public void testRepetitionJobEntityEvents() throws Exception {
        Clock previousClock = processEngineConfiguration.getClock();

        Clock testClock = new DefaultClockImpl();

        processEngineConfiguration.setClock(testClock);

        Date now = new Date();
        testClock.setCurrentTime(now);

        Calendar nowCalendar = new GregorianCalendar();

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testRepetitionJobEvents");
        Job theJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(theJob).isNotNull();

        // Check if create-event has been dispatched
        assertThat(listener.getEventsReceived()).hasSize(3);
        FlowableEngineEvent event = (FlowableEngineEvent) listener.getEventsReceived().get(0);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
        checkEventContext(event, theJob);

        event = (FlowableEngineEvent) listener.getEventsReceived().get(1);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_INITIALIZED);
        checkEventContext(event, theJob);

        event = (FlowableEngineEvent) listener.getEventsReceived().get(2);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.TIMER_SCHEDULED);
        checkEventContext(event, theJob);

        listener.clearEventsReceived();

        // no timer jobs will be fired
        waitForJobExecutorToProcessAllJobs(2000, 200);
        assertThat(listener.getEventsReceived()).isEmpty();
        assertThat(managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
        Job firstTimerInstance = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();

        nowCalendar.add(Calendar.HOUR, 1);
        nowCalendar.add(Calendar.MINUTE, 5);
        testClock.setCurrentTime(nowCalendar.getTime());

        // the timer job will be fired for the first time now
        waitForJobExecutorToProcessAllJobs(2000, 200);

        // a new timer should be created with the repeat
        assertThat(managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
        Job secondTimerInstance = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(secondTimerInstance.getId()).isNotSameAs(firstTimerInstance.getId());

        checkEventCount(1, FlowableEngineEventType.TIMER_FIRED);
        checkEventContext(filterEvents(FlowableEngineEventType.TIMER_FIRED).get(0), firstTimerInstance);
        checkEventCount(1, FlowableEngineEventType.TIMER_SCHEDULED);
        checkEventContext(filterEvents(FlowableEngineEventType.TIMER_SCHEDULED).get(0), secondTimerInstance);

        listener.clearEventsReceived();

        nowCalendar.add(Calendar.HOUR, 1);
        nowCalendar.add(Calendar.MINUTE, 5);
        testClock.setCurrentTime(nowCalendar.getTime());

        // the second timer job will be fired and no jobs should be remaining
        waitForJobExecutorToProcessAllJobs(2000, 200);
        assertThat(managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).count()).isZero();

        nowCalendar.add(Calendar.HOUR, 1);
        nowCalendar.add(Calendar.MINUTE, 5);
        testClock.setCurrentTime(nowCalendar.getTime());
        waitForJobExecutorToProcessAllJobs(2000, 200);

        assertThat(managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).count()).isZero();

        checkEventCount(1, FlowableEngineEventType.TIMER_FIRED);
        checkEventContext(filterEvents(FlowableEngineEventType.TIMER_FIRED).get(0), secondTimerInstance);
        checkEventCount(0, FlowableEngineEventType.TIMER_SCHEDULED);

        listener.clearEventsReceived();
        processEngineConfiguration.setClock(previousClock);
    }

    @Test
    @Deployment
    public void testJobCanceledEventOnBoundaryEvent() throws Exception {
        Clock testClock = new DefaultClockImpl();

        processEngineConfiguration.setClock(testClock);

        testClock.setCurrentTime(new Date());
        runtimeService.startProcessInstanceByKey("testTimerCancelledEvent");
        listener.clearEventsReceived();

        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();

        taskService.complete(task.getId());

        checkEventCount(1, FlowableEngineEventType.JOB_CANCELED);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/event/JobEventsTest.testJobCanceledEventOnBoundaryEvent.bpmn20.xml")
    public void testJobCanceledEventByManagementService() throws Exception {
        // GIVEN
        processEngineConfiguration.getClock().setCurrentTime(new Date());
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testTimerCancelledEvent");
        listener.clearEventsReceived();

        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();

        // WHEN
        managementService.deleteTimerJob(job.getId());

        // THEN
        checkEventCount(1, FlowableEngineEventType.JOB_CANCELED);
    }

    @Test
    public void testJobCanceledAndTimerStartEventOnProcessRedeploy() throws Exception {
        // GIVEN deploy process definition
        String deployment1 = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/engine/test/api/event/JobEventsTest.testTimerFiredForTimerStart.bpmn20.xml").deploy().getId();
        checkEventCount(1, FlowableEngineEventType.TIMER_SCHEDULED);
        listener.clearEventsReceived();

        // WHEN
        String deployment2 = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/engine/test/api/event/JobEventsTest.testTimerFiredForTimerStart.bpmn20.xml").deploy().getId();

        // THEN
        checkEventCount(1, FlowableEngineEventType.JOB_CANCELED);
        checkEventCount(1, FlowableEngineEventType.TIMER_SCHEDULED);

        listener.clearEventsReceived();

        repositoryService.deleteDeployment(deployment2);
        checkEventCount(1, FlowableEngineEventType.JOB_CANCELED);
        checkEventCount(1, FlowableEngineEventType.TIMER_SCHEDULED);

        listener.clearEventsReceived();

        repositoryService.deleteDeployment(deployment1);
        checkEventCount(1, FlowableEngineEventType.JOB_CANCELED);
    }

    private void checkEventCount(int expectedCount, FlowableEngineEventType eventType) {// count
        // timer
        // cancelled
        // events
        int timerCancelledCount = 0;
        List<FlowableEvent> eventsReceived = listener.getEventsReceived();
        for (FlowableEvent eventReceived : eventsReceived) {
            if (eventType == eventReceived.getType()) {
                timerCancelledCount++;
            }
        }
        assertThat(expectedCount).as(eventType.name() + " event was expected " + expectedCount + " times.")
                .isEqualTo(timerCancelledCount);
    }

    private List<FlowableEngineEvent> filterEvents(FlowableEngineEventType eventType) {
        List<FlowableEvent> eventsReceived = listener.getEventsReceived();
        List<FlowableEngineEvent> filteredEvents = new ArrayList<>();
        for (FlowableEvent eventReceived : eventsReceived) {
            if (eventType == eventReceived.getType()) {
                filteredEvents.add((FlowableEngineEvent) eventReceived);
            }
        }
        return filteredEvents;
    }

    /**
     * /** Test TIMER_FIRED event for timer start bpmn event.
     */
    @Test
    @Deployment
    public void testTimerFiredForTimerStart() throws Exception {
        // there should be one job after process definition deployment

        // Force timer to start the process
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        processEngineConfiguration.getClock().setCurrentTime(tomorrow.getTime());
        waitForJobExecutorToProcessAllJobs(2000, 100);

        // Check Timer fired event has been dispatched
        assertThat(listener.getEventsReceived()).hasSize(6);

        // timer entity created first
        assertThat(listener.getEventsReceived().get(0).getType()).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
        // timer entity initialized
        assertThat(listener.getEventsReceived().get(1).getType()).isEqualTo(FlowableEngineEventType.ENTITY_INITIALIZED);
        // timer entity deleted
        assertThat(listener.getEventsReceived().get(2).getType()).isEqualTo(FlowableEngineEventType.ENTITY_DELETED);
        // job fired
        assertThat(listener.getEventsReceived().get(3).getType()).isEqualTo(FlowableEngineEventType.TIMER_FIRED);
        // job executed successfully
        assertThat(listener.getEventsReceived().get(5).getType()).isEqualTo(FlowableEngineEventType.JOB_EXECUTION_SUCCESS);

        checkEventCount(0, FlowableEngineEventType.JOB_CANCELED);
    }

    /**
     * Test TIMER_FIRED event for intermediate timer bpmn event.
     */
    @Test
    @Deployment
    public void testTimerFiredForIntermediateTimer() throws Exception {
        runtimeService.startProcessInstanceByKey("testTimerFiredForIntermediateTimer");

        // Force timer to start the process
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        processEngineConfiguration.getClock().setCurrentTime(tomorrow.getTime());
        waitForJobExecutorToProcessAllJobs(2000, 100);

        checkEventCount(1, FlowableEngineEventType.TIMER_SCHEDULED);
        checkEventCount(0, FlowableEngineEventType.JOB_CANCELED);
        checkEventCount(1, FlowableEngineEventType.TIMER_FIRED);
    }

    /**
     * Test create, update and delete events of jobs entities.
     */
    @Test
    @Deployment
    public void testJobEntityEventsException() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testJobEvents");
        Job theJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(theJob).isNotNull();

        // Set retries to 1, to prevent multiple chains of events being thrown
        managementService.setTimerJobRetries(theJob.getId(), 1);

        // Force timer to fire
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        processEngineConfiguration.getClock().setCurrentTime(tomorrow.getTime());

        Job executableJob = managementService.moveTimerToExecutableJob(theJob.getId());

        listener.clearEventsReceived();

        assertThatThrownBy(() -> managementService.executeJob(executableJob.getId()))
                .isInstanceOf(FlowableException.class);

        theJob = managementService.createDeadLetterJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(theJob).isNotNull();
        assertThat(theJob.getElementId()).isEqualTo("timer");
        assertThat(theJob.getElementName()).isEqualTo("Timer");

        // Check delete-event has been dispatched
        assertThat(listener.getEventsReceived()).hasSize(9);

        // First, the timer was fired
        FlowableEngineEvent event = (FlowableEngineEvent) listener.getEventsReceived().get(0);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.TIMER_FIRED);
        checkEventContext(event, theJob);

        // Second, the job-entity was deleted, as the job was executed
        event = (FlowableEngineEvent) listener.getEventsReceived().get(1);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_DELETED);
        checkEventContext(event, theJob);

        // Next, a job failed event is dispatched
        event = (FlowableEngineEvent) listener.getEventsReceived().get(2);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.JOB_EXECUTION_FAILURE);
        checkEventContext(event, theJob);

        event = (FlowableEngineEvent) listener.getEventsReceived().get(3);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.JOB_MOVED_TO_DEADLETTER);
        checkEventContext(event, theJob);

        // Finally, an timer create event is received and the job count is decremented
        event = (FlowableEngineEvent) listener.getEventsReceived().get(4);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
        checkEventContext(event, theJob);

        event = (FlowableEngineEvent) listener.getEventsReceived().get(5);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_INITIALIZED);
        checkEventContext(event, theJob);

        // original job is deleted
        event = (FlowableEngineEvent) listener.getEventsReceived().get(6);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_DELETED);
        checkEventContext(event, theJob);

        // timer job updated
        event = (FlowableEngineEvent) listener.getEventsReceived().get(7);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_UPDATED);
        checkEventContext(event, theJob);

        event = (FlowableEngineEvent) listener.getEventsReceived().get(8);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.JOB_RETRIES_DECREMENTED);
        assertThat(((Job) ((FlowableEntityEvent) event).getEntity()).getRetries()).isZero();
        checkEventContext(event, theJob);
    }

    @Test
    @Deployment
    public void testTerminateEndEvent() throws Exception {
        Clock previousClock = processEngineConfiguration.getClock();

        TestFlowableEventListener activitiEventListener = new TestFlowableEventListener();
        processEngineConfiguration.getEventDispatcher().addEventListener(activitiEventListener);
        Clock testClock = new DefaultClockImpl();

        processEngineConfiguration.setClock(testClock);

        testClock.setCurrentTime(new Date());
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testTerminateEndEvent");
        listener.clearEventsReceived();

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Inside Task");

        // Force timer to trigger so that subprocess will flow to terminate end event
        Calendar later = Calendar.getInstance();
        later.add(Calendar.YEAR, 1);
        processEngineConfiguration.getClock().setCurrentTime(later.getTime());
        waitForJobExecutorToProcessAllJobs(2000, 100);

        // Process Cancelled event should not be sent for the subprocess
        List<FlowableEvent> eventsReceived = activitiEventListener.getEventsReceived();
        assertThat(eventsReceived)
                .extracting(FlowableEvent::getType)
                .as("Should not have received PROCESS_CANCELLED event")
                .doesNotContain(FlowableEngineEventType.PROCESS_CANCELLED);

        // validate the activityType string
        for (FlowableEvent eventReceived : eventsReceived) {
            if (FlowableEngineEventType.ACTIVITY_CANCELLED == eventReceived.getType()) {
                FlowableActivityEvent event = (FlowableActivityEvent) eventReceived;
                String activityType = event.getActivityType();
                if (!"userTask".equals(activityType) && (!"subProcess".equals(activityType)) && (!"endEvent".equals(activityType))) {
                    fail("Unexpected activity type: " + activityType);
                }
            }
        }

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Outside Task");

        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());

        processEngineConfiguration.setClock(previousClock);
    }

    protected void checkEventContext(FlowableEngineEvent event, Job entity) {
        assertThat(event.getProcessInstanceId()).isEqualTo(entity.getProcessInstanceId());
        assertThat(event.getProcessDefinitionId()).isEqualTo(entity.getProcessDefinitionId());
        assertThat(event.getExecutionId()).isNotNull();

        assertThat(event).isInstanceOf(FlowableEntityEvent.class);
        FlowableEntityEvent entityEvent = (FlowableEntityEvent) event;
        assertThat(entityEvent.getEntity()).isInstanceOf(Job.class);
        assertThat(((Job) entityEvent.getEntity()).getId()).isEqualTo(entity.getId());
    }

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
        processEngineConfiguration.getClock().reset();
    }
}
