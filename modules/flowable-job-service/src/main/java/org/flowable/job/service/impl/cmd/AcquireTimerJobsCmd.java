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
import org.flowable.job.service.impl.asyncexecutor.AcquiredTimerJobEntities;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.flowable.job.service.impl.util.CommandContextUtil;

/**
 * @author Tijs Rademakers
 */
public class AcquireTimerJobsCmd implements Command<AcquiredTimerJobEntities> {

    private final AsyncExecutor asyncExecutor;

    public AcquireTimerJobsCmd(AsyncExecutor asyncExecutor) {
        this.asyncExecutor = asyncExecutor;
    }

    @Override
    public AcquiredTimerJobEntities execute(CommandContext commandContext) {
        AcquiredTimerJobEntities acquiredJobs = new AcquiredTimerJobEntities();
        List<TimerJobEntity> timerJobs = CommandContextUtil.getTimerJobEntityManager(commandContext)
                .findTimerJobsToExecute(new Page(0, asyncExecutor.getMaxAsyncJobsDuePerAcquisition()));

        for (TimerJobEntity job : timerJobs) {
            lockJob(commandContext, job, asyncExecutor.getAsyncJobLockTimeInMillis());
            acquiredJobs.addJob(job);
        }

        return acquiredJobs;
    }

    protected void lockJob(CommandContext commandContext, TimerJobEntity job, int lockTimeInMillis) {

        // This will trigger an optimistic locking exception when two concurrent executors
        // try to lock, as the revision will not match.

        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTime(CommandContextUtil.getJobServiceConfiguration(commandContext).getClock().getCurrentTime());
        gregorianCalendar.add(Calendar.MILLISECOND, lockTimeInMillis);
        job.setLockOwner(asyncExecutor.getLockOwner());
        job.setLockExpirationTime(gregorianCalendar.getTime());
    }
}