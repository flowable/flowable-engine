package org.activiti.engine.test.bpmn.event.timer;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Date;
import java.util.List;

import org.activiti.engine.impl.test.ResourceFlowableTestCase;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.calendar.BusinessCalendar;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;

/**
 * testing custom calendar for timer definitions Created by martin.grofcik
 */
public class TimerCustomCalendarTest extends ResourceFlowableTestCase {

    public TimerCustomCalendarTest() {
        super("org/activiti/engine/test/bpmn/event/timer/TimerCustomCalendarTest.flowable.cfg.xml");
    }

    @Deployment
    public void testCycleTimer() {
        List<Job> jobs = this.managementService.createTimerJobQuery().list();

        assertThat("One job is scheduled", jobs.size(), is(1));
        assertThat("Job must be scheduled by custom business calendar to Date(0)", jobs.get(0).getDuedate(), is(new Date(0)));

        this.managementService.moveTimerToExecutableJob(jobs.get(0).getId());
        this.managementService.executeJob(jobs.get(0).getId());

        jobs = this.managementService.createTimerJobQuery().list();

        assertThat("One job is scheduled (repetition is 2x)", jobs.size(), is(1));
        assertThat("Job must be scheduled by custom business calendar to Date(0)", jobs.get(0).getDuedate(), is(new Date(0)));

        this.managementService.moveTimerToExecutableJob(jobs.get(0).getId());
        this.managementService.executeJob(jobs.get(0).getId());

        jobs = this.managementService.createTimerJobQuery().list();
        assertThat("There must be no job.", jobs.isEmpty());
    }

    @Deployment
    public void testCustomDurationTimerCalendar() {
        ProcessInstance processInstance = this.runtimeService.startProcessInstanceByKey("testCustomDurationCalendar");

        List<Job> jobs = this.managementService.createTimerJobQuery().list();

        assertThat("One job is scheduled", jobs.size(), is(1));
        assertThat("Job must be scheduled by custom business calendar to Date(0)", jobs.get(0).getDuedate(), is(new Date(0)));

        this.managementService.moveTimerToExecutableJob(jobs.get(0).getId());
        this.managementService.executeJob(jobs.get(0).getId());
        waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(10000, 200);

        this.runtimeService.trigger(processInstance.getId());
    }

    @Deployment
    public void testInvalidDurationTimerCalendar() {
        try {
            this.runtimeService.startProcessInstanceByKey("testCustomDurationCalendar");
            fail("Activiti exception expected - calendar not found");
        } catch (FlowableException e) {
            assertThat(e.getMessage(), containsString("INVALID does not exist"));
        }
    }

    @Deployment
    public void testBoundaryTimer() {
        this.runtimeService.startProcessInstanceByKey("testBoundaryTimer");

        List<Job> jobs = this.managementService.createTimerJobQuery().list();
        assertThat("One job is scheduled", jobs.size(), is(1));
        assertThat("Job must be scheduled by custom business calendar to Date(0)", jobs.get(0).getDuedate(), is(new Date(0)));

        this.managementService.moveTimerToExecutableJob(jobs.get(0).getId());
        this.managementService.executeJob(jobs.get(0).getId());
        waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(10000, 200);
    }

    public static class CustomBusinessCalendar implements BusinessCalendar {

        @Override
        public Date resolveDuedate(String duedateDescription) {
            return new Date(0);
        }

        @Override
        public Date resolveDuedate(String duedateDescription, int maxIterations) {
            return new Date(0);
        }

        @Override
        public Boolean validateDuedate(String duedateDescription, int maxIterations, Date endDate, Date newTimer) {
            return true;
        }

        @Override
        public Date resolveEndDate(String endDateString) {
            return new Date(0);
        }

    }

}
