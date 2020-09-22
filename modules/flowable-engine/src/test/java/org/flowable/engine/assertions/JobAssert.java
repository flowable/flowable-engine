package org.flowable.engine.assertions;

import java.util.Date;

import org.assertj.core.api.Assertions;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.runtime.ExecutionQuery;
import org.flowable.job.api.Job;
import org.flowable.job.api.JobQuery;

/**
 * Assertions for a {@link Job}
 */
public class JobAssert extends AbstractProcessAssert<JobAssert, Job> {

    protected JobAssert(ProcessEngine engine, Job actual) {
        super(engine, actual, JobAssert.class);
    }

    protected static JobAssert assertThat(ProcessEngine engine, Job actual) {
        return new JobAssert(engine, actual);
    }

    @Override
    protected Job getCurrent() {
        return jobQuery().jobId(actual.getId()).singleResult();
    }

    /**
     * Verifies the expectation of a specific id for the {@link Job}.
     *
     * @param expectedId the expected job id
     * @return this {@link JobAssert}
     */
    public JobAssert hasId(String expectedId) {
        Job current = getExistingCurrent();
        Assertions.assertThat(expectedId).isNotEmpty();
        String actualId = actual.getId();
        Assertions.assertThat(actualId)
                .overridingErrorMessage(
                        "Expecting %s to have id '%s', but found it to be '%s'",
                        toString(current), expectedId, actualId
                )
                .isEqualTo(expectedId);
        return this;
    }

    /**
     * Verifies the expectation of a specific due date for the {@link Job}.
     *
     * @param expectedDueDate the expected due date
     * @return this {@link JobAssert}
     */
    public JobAssert hasDueDate(Date expectedDueDate) {
        Job current = getExistingCurrent();
        Assertions.assertThat(expectedDueDate).isNotNull();
        Date actualDuedate = current.getDuedate();
        Assertions.assertThat(actualDuedate)
                .overridingErrorMessage(
                        "Expecting %s to be due at '%s', but found it to be due at '%s'",
                        toString(current), expectedDueDate, actualDuedate
                )
                .isEqualTo(expectedDueDate);
        return this;
    }

    /**
     * Verifies the expectation of a specific process instance id for the {@link Job}.
     *
     * @param expectedProcessInstanceId the expected process instance id
     * @return this {@link JobAssert}
     */
    public JobAssert hasProcessInstanceId(String expectedProcessInstanceId) {
        Job current = getExistingCurrent();
        Assertions.assertThat(expectedProcessInstanceId).isNotNull();
        String actualProcessInstanceId = current.getProcessInstanceId();
        Assertions.assertThat(actualProcessInstanceId)
                .overridingErrorMessage(
                        "Expecting %s to have process instance id '%s', but found it to be '%s'",
                        toString(current), expectedProcessInstanceId, actualProcessInstanceId
                )
                .isEqualTo(expectedProcessInstanceId);
        return this;
    }

    /**
     * Verifies the expectation of a specific execution id for the {@link Job}.
     *
     * @param expectedExecutionId the expected execution id
     * @return this {@link JobAssert}
     */
    public JobAssert hasExecutionId(String expectedExecutionId) {
        Job current = getExistingCurrent();
        Assertions.assertThat(expectedExecutionId).isNotNull();
        String actualExecutionId = current.getExecutionId();
        Assertions.assertThat(actualExecutionId)
                .overridingErrorMessage(
                        "Expecting %s to have execution id '%s', but found it to be '%s'",
                        toString(current), expectedExecutionId, actualExecutionId
                )
                .isEqualTo(expectedExecutionId);
        return this;
    }

    /**
     * Verifies the expectation of a specific number of retries left for the {@link Job}.
     *
     * @param expectedRetries the expected number of retries
     * @return this {@link JobAssert}
     */
    public JobAssert hasRetries(int expectedRetries) {
        Job current = getExistingCurrent();
        int actualRetries = current.getRetries();
        Assertions.assertThat(actualRetries)
                .overridingErrorMessage(
                        "Expecting %s to have %s retries left, but found %s retries",
                        toString(current), expectedRetries, actualRetries
                )
                .isEqualTo(expectedRetries);
        return this;
    }

    /**
     * Verifies the expectation of the existence of an exception message for the {@link Job}.
     *
     * @return this {@link JobAssert}
     */
    public JobAssert hasExceptionMessage() {
        Job current = getExistingCurrent();
        String actualExceptionMessage = current.getExceptionMessage();
        Assertions.assertThat(actualExceptionMessage)
                .overridingErrorMessage(
                        "Expecting %s to have an exception message, but found it to be null or empty: '%s'",
                        toString(current), actualExceptionMessage
                )
                .isNotEmpty();
        return this;
    }

    @Override
    protected String toString(Job job) {
        return job != null ?
                String.format("%s {" +
                                "id='%s', " +
                                "processInstanceId='%s', " +
                                "executionId='%s'}",
                        Job.class.getSimpleName(),
                        job.getId(),
                        job.getProcessInstanceId(),
                        job.getExecutionId())
                : null;
    }

    /*
     * JobQuery, automatically narrowed to {@link ProcessInstance} of actual {@link job}
     */
    @Override
    protected JobQuery jobQuery() {
        return super.jobQuery().processInstanceId(actual.getProcessInstanceId());
    }

    /*
     * ExecutionQuery, automatically narrowed to {@link ProcessInstance} of actual {@link job}
     */
    @Override
    protected ExecutionQuery executionQuery() {
        return super.executionQuery().processInstanceId(actual.getProcessInstanceId());
    }

}
