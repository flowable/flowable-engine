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
package org.flowable.engine.impl.cmd;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.flowable.engine.common.impl.Page;
import org.flowable.engine.impl.asyncexecutor.AcquiredJobEntities;
import org.flowable.engine.impl.asyncexecutor.AsyncExecutor;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.impl.interceptor.Command;
import org.flowable.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.JobInfoEntity;
import org.flowable.engine.impl.persistence.entity.JobInfoEntityManager;

/**
 * @author Tijs Rademakers
 */
public class AcquireJobsCmd implements Command<AcquiredJobEntities> {

    private final AsyncExecutor asyncExecutor;
    private final int remainingCapacity;
    private final JobInfoEntityManager<? extends JobInfoEntity> jobEntityManager;
    
    public AcquireJobsCmd(AsyncExecutor asyncExecutor) {
        this.asyncExecutor = asyncExecutor;
        this.remainingCapacity = Integer.MAX_VALUE;
        this.jobEntityManager = asyncExecutor.getProcessEngineConfiguration().getJobEntityManager(); // backwards compatibility
    }

    public AcquireJobsCmd(AsyncExecutor asyncExecutor, int remainingCapacity, JobInfoEntityManager<? extends JobInfoEntity> jobEntityManager) {
        this.asyncExecutor = asyncExecutor;
        this.remainingCapacity = remainingCapacity;
        this.jobEntityManager = jobEntityManager;
    }

    public AcquiredJobEntities execute(CommandContext commandContext) {
        int maxResults = Math.min(remainingCapacity, asyncExecutor.getMaxAsyncJobsDuePerAcquisition());

        List<? extends JobInfoEntity> jobs = jobEntityManager.findJobsToExecute(new Page(0, maxResults)); 
        AcquiredJobEntities acquiredJobs = new AcquiredJobEntities();

        for (JobInfoEntity job : jobs) {
            lockJob(commandContext, job, asyncExecutor.getAsyncJobLockTimeInMillis());
            acquiredJobs.addJob(job);
        }

        return acquiredJobs;
    }

    protected void lockJob(CommandContext commandContext, JobInfoEntity job, int lockTimeInMillis) {
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTime(commandContext.getProcessEngineConfiguration().getClock().getCurrentTime());
        gregorianCalendar.add(Calendar.MILLISECOND, lockTimeInMillis);
        job.setLockOwner(asyncExecutor.getLockOwner());
        job.setLockExpirationTime(gregorianCalendar.getTime());
    }
}