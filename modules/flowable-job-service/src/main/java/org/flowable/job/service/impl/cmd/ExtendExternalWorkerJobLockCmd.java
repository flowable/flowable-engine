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
package org.flowable.job.service.impl.cmd;

import java.time.Duration;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableForbiddenException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.FlowableOptimisticLockingException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.api.Job;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.persistence.entity.ExternalWorkerJobEntity;
import org.flowable.job.service.impl.persistence.entity.ExternalWorkerJobEntityManager;
import org.flowable.job.service.impl.util.CommandContextUtil;
import org.flowable.job.service.impl.util.ExternalWorkerScopeLockUpdater;

/**
 * Extends an active external-worker lock using optimistic concurrency.
 */
public class ExtendExternalWorkerJobLockCmd implements Command<Date> {

    protected final String jobId;
    protected final String workerId;
    protected final Duration lockDuration;
    protected final Date expectedLockExpirationTime;
    protected final JobServiceConfiguration jobServiceConfiguration;

    public ExtendExternalWorkerJobLockCmd(
            String jobId,
            String workerId,
            Duration lockDuration,
            Date expectedLockExpirationTime,
            JobServiceConfiguration jobServiceConfiguration) {

        this.jobId = jobId;
        this.workerId = workerId;
        this.lockDuration = lockDuration;
        this.expectedLockExpirationTime = expectedLockExpirationTime;
        this.jobServiceConfiguration = jobServiceConfiguration;
    }

    @Override
    public Date execute(CommandContext commandContext) {
        validateRequest();

        ExternalWorkerJobEntityManager jobEntityManager = jobServiceConfiguration.getExternalWorkerJobEntityManager();
        ExternalWorkerJobEntity job = jobEntityManager.findById(jobId);
        if (job == null) {
            throw new FlowableObjectNotFoundException("No external worker job found with id '" + jobId + "'", Job.class);
        }

        if (!workerId.equals(job.getLockOwner())) {
            throw new FlowableForbiddenException(workerId + " does not hold a lock on external worker job " + jobId);
        }

        Date currentExpirationTime = job.getLockExpirationTime();
        Date currentTime = jobServiceConfiguration.getClock().getCurrentTime();
        if (currentExpirationTime == null || !currentTime.before(currentExpirationTime)) {
            throw new FlowableOptimisticLockingException("The lock for external worker job " + jobId + " has expired");
        }

        if (!expectedLockExpirationTime.equals(currentExpirationTime)) {
            throw new FlowableOptimisticLockingException(
                    "The lock for external worker job " + jobId + " was changed by another request");
        }

        long lockDurationMillis = toLockDurationMillis(lockDuration);
        Date newExpirationTime;
        try {
            newExpirationTime = new Date(Math.addExact(currentTime.getTime(), lockDurationMillis));
        } catch (ArithmeticException exception) {
            throw new FlowableIllegalArgumentException("lockDuration is too large", exception);
        }

        job.setLockExpirationTime(newExpirationTime);
        jobEntityManager.update(job);

        if (job.isExclusive()) {
            extendExclusiveScopeLock(commandContext, job, newExpirationTime, currentTime);
        }

        return newExpirationTime;
    }

    protected void extendExclusiveScopeLock(
            CommandContext commandContext,
            ExternalWorkerJobEntity job,
            Date newExpirationTime,
            Date currentTime) {

        String tablePrefix = CommandContextUtil.getDbSqlSession(commandContext)
                .getDbSqlSessionFactory()
                .getDatabaseTablePrefix();
        if (job.getProcessInstanceId() != null) {
            ExternalWorkerScopeLockUpdater.extend(
                    tablePrefix + "ACT_RU_EXECUTION",
                    job.getProcessInstanceId(),
                    workerId,
                    newExpirationTime,
                    currentTime);
        } else if (ScopeTypes.CMMN.equals(job.getScopeType())) {
            ExternalWorkerScopeLockUpdater.extend(
                    tablePrefix + "ACT_CMMN_RU_CASE_INST",
                    job.getScopeId(),
                    workerId,
                    newExpirationTime,
                    currentTime);
        } else {
            throw new FlowableOptimisticLockingException(
                    "Cannot extend the exclusive scope lock for external worker job " + jobId);
        }
    }

    protected void validateRequest() {
        if (StringUtils.isEmpty(jobId)) {
            throw new FlowableIllegalArgumentException("jobId must not be empty");
        }
        if (StringUtils.isEmpty(workerId)) {
            throw new FlowableIllegalArgumentException("workerId must not be empty");
        }
        if (lockDuration == null || lockDuration.isZero() || lockDuration.isNegative()) {
            throw new FlowableIllegalArgumentException("lockDuration must be positive");
        }
        if (expectedLockExpirationTime == null) {
            throw new FlowableIllegalArgumentException("expectedLockExpirationTime is required");
        }
    }

    protected long toLockDurationMillis(Duration duration) {
        long durationMillis;
        try {
            durationMillis = duration.toMillis();
        } catch (ArithmeticException exception) {
            throw new FlowableIllegalArgumentException("lockDuration is too large", exception);
        }
        if (durationMillis < 1 || durationMillis > Integer.MAX_VALUE) {
            throw new FlowableIllegalArgumentException(
                    "lockDuration must be between 1 millisecond and " + Integer.MAX_VALUE + " milliseconds");
        }
        return durationMillis;
    }
}
