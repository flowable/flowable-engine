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
import org.flowable.job.service.impl.persistence.entity.JobInfoEntity;
import org.flowable.job.service.impl.persistence.entity.JobInfoEntityManager;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class AcquireJobsCmd implements Command<List<? extends JobInfoEntity>> {

    protected AsyncExecutor asyncExecutor;
    protected int remainingCapacity;
    protected JobInfoEntityManager<? extends JobInfoEntity> jobEntityManager;
    protected boolean globalAcquireLockEnabled;
    
    public AcquireJobsCmd(AsyncExecutor asyncExecutor) {
        this(asyncExecutor, Integer.MAX_VALUE, asyncExecutor.getJobServiceConfiguration().getJobEntityManager());
    }

    public AcquireJobsCmd(AsyncExecutor asyncExecutor, int remainingCapacity, JobInfoEntityManager<? extends JobInfoEntity> jobEntityManager) {
        this(asyncExecutor, remainingCapacity, jobEntityManager, false);
    }

    public AcquireJobsCmd(AsyncExecutor asyncExecutor, int remainingCapacity,
            JobInfoEntityManager<? extends JobInfoEntity> jobEntityManager, boolean globalAcquireLockEnabled) {
        this.asyncExecutor = asyncExecutor;
        this.remainingCapacity = remainingCapacity;
        this.jobEntityManager = jobEntityManager;
        this.globalAcquireLockEnabled = globalAcquireLockEnabled;
    }

    @Override
    public List<? extends JobInfoEntity> execute(CommandContext commandContext) {
        if (globalAcquireLockEnabled) {
            return acquireWithGlobalAcquireLockEnabled(commandContext);
        } else {
            return defaultAcquire(commandContext);
        }
    }

    protected List<? extends JobInfoEntity> acquireWithGlobalAcquireLockEnabled(CommandContext commandContext) {
        int maxResults = Math.min(remainingCapacity, asyncExecutor.getMaxAsyncJobsDuePerAcquisition());
        List<String> enabledCategories = asyncExecutor.getJobServiceConfiguration().getEnabledJobCategories();

        // When running with the global acquire lock, optimistic locking exceptions can't happen during acquire,
        // as at most one node will be acquiring at any given time.
        GregorianCalendar jobExpirationTime = calculateLockExpirationTime(asyncExecutor.getAsyncJobLockTimeInMillis(), asyncExecutor.getJobServiceConfiguration());

        return jobEntityManager
            .findJobsToExecuteAndLockInBulk(enabledCategories, new Page(0, maxResults), asyncExecutor.getLockOwner(), jobExpirationTime.getTime());
    }

    protected List<? extends JobInfoEntity> defaultAcquire(CommandContext commandContext) {
        int maxResults = Math.min(remainingCapacity, asyncExecutor.getMaxAsyncJobsDuePerAcquisition());
        List<String> enabledCategories = asyncExecutor.getJobServiceConfiguration().getEnabledJobCategories();
        List<? extends JobInfoEntity> jobs = jobEntityManager.findJobsToExecute(enabledCategories, new Page(0, maxResults));

        for (JobInfoEntity job : jobs) {
            lockJob(commandContext, job, asyncExecutor.getAsyncJobLockTimeInMillis(), asyncExecutor.getJobServiceConfiguration());
        }

        return jobs;
    }

    protected List<? extends JobInfoEntity> fetchAsyncJobs() {
        int maxResults = Math.min(remainingCapacity, asyncExecutor.getMaxAsyncJobsDuePerAcquisition());
        List<String> enabledCategories = asyncExecutor.getJobServiceConfiguration().getEnabledJobCategories();
        return jobEntityManager.findJobsToExecute(enabledCategories, new Page(0, maxResults));
    }

    protected void lockJob(CommandContext commandContext, JobInfoEntity job, int lockTimeInMillis, JobServiceConfiguration jobServiceConfiguration) {
        GregorianCalendar gregorianCalendar = calculateLockExpirationTime(lockTimeInMillis, jobServiceConfiguration);
        job.setLockOwner(asyncExecutor.getLockOwner());
        job.setLockExpirationTime(gregorianCalendar.getTime());
    }

    protected GregorianCalendar calculateLockExpirationTime(int lockTimeInMillis, JobServiceConfiguration jobServiceConfiguration) {
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTime(jobServiceConfiguration.getClock().getCurrentTime());
        gregorianCalendar.add(Calendar.MILLISECOND, lockTimeInMillis);
        return gregorianCalendar;
    }
}
