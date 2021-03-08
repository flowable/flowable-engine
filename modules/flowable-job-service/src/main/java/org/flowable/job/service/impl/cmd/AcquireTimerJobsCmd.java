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

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.flowable.common.engine.impl.Page;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class AcquireTimerJobsCmd implements Command<List<TimerJobEntity>> {

    protected AsyncExecutor asyncExecutor;
    protected boolean globalAcquireLockEnabled;

    public AcquireTimerJobsCmd(AsyncExecutor asyncExecutor) {
        this(asyncExecutor, false);
    }

    public AcquireTimerJobsCmd(AsyncExecutor asyncExecutor, boolean globalAcquireLockEnabled) {
        this.asyncExecutor = asyncExecutor;
        this.globalAcquireLockEnabled = globalAcquireLockEnabled;
    }

    @Override
    public List<TimerJobEntity> execute(CommandContext commandContext) {
        if (globalAcquireLockEnabled) {
            return acquireTimersWithGlobalAcquireLockEnabled();
        } else {
            return defaultAcquire(commandContext);
        }
    }

    protected List<TimerJobEntity> defaultAcquire(CommandContext commandContext) {
        JobServiceConfiguration jobServiceConfiguration = asyncExecutor.getJobServiceConfiguration();
        List<TimerJobEntity> timerJobs = fetchTimerJobs(jobServiceConfiguration);

        for (TimerJobEntity job : timerJobs) {
            lockJob(commandContext, job, asyncExecutor.getAsyncJobLockTimeInMillis(), jobServiceConfiguration);
        }

        return timerJobs;
    }

    protected List<TimerJobEntity> acquireTimersWithGlobalAcquireLockEnabled() {
        JobServiceConfiguration jobServiceConfiguration = asyncExecutor.getJobServiceConfiguration();
        List<TimerJobEntity> timerJobs = fetchTimerJobs(jobServiceConfiguration);

        if (!timerJobs.isEmpty()) {
            // When running with the global acquire lock, optimistic locking exceptions can't happen during acquire,
            // as at most one node will be acquiring at any given time.
            GregorianCalendar jobExpirationTime = calculateLockExpirationTime(asyncExecutor.getAsyncJobLockTimeInMillis(), jobServiceConfiguration);
            jobServiceConfiguration.getTimerJobEntityManager()
                .bulkUpdateJobLockWithoutRevisionCheck(timerJobs, asyncExecutor.getLockOwner(), jobExpirationTime.getTime());
        }

        return timerJobs;
    }

    protected List<TimerJobEntity> fetchTimerJobs(JobServiceConfiguration jobServiceConfiguration) {
        List<String> enabledCategories = jobServiceConfiguration.getEnabledJobCategories();
        return jobServiceConfiguration.getTimerJobEntityManager()
            .findJobsToExecute(enabledCategories, new Page(0, asyncExecutor.getMaxTimerJobsPerAcquisition()));
    }

    protected void lockJob(CommandContext commandContext, TimerJobEntity job, int lockTimeInMillis, JobServiceConfiguration jobServiceConfiguration) {

        // This will use the regular updates flush in the DbSqlSession
        // This will trigger an optimistic locking exception when two concurrent executors
        // try to lock, as the revision will not match.

        GregorianCalendar jobExpirationTime = calculateLockExpirationTime(lockTimeInMillis, jobServiceConfiguration);
        job.setLockOwner(asyncExecutor.getLockOwner());
        job.setLockExpirationTime(jobExpirationTime.getTime());
    }

    protected GregorianCalendar calculateLockExpirationTime(int lockTimeInMillis, JobServiceConfiguration jobServiceConfiguration) {
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTime(jobServiceConfiguration.getClock().getCurrentTime());
        gregorianCalendar.add(Calendar.MILLISECOND, lockTimeInMillis);
        return gregorianCalendar;
    }

}