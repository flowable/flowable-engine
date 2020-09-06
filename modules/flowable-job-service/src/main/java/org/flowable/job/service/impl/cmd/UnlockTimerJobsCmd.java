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

import java.util.Collection;

import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntityManager;

/**
 * @author Filip Hrisafov
 */
public class UnlockTimerJobsCmd implements Command<Void> {

    protected final Collection<TimerJobEntity> timerJobs;
    protected final JobServiceConfiguration jobServiceConfiguration;

    public UnlockTimerJobsCmd(Collection<TimerJobEntity> timerJobs, JobServiceConfiguration jobServiceConfiguration) {
        this.timerJobs = timerJobs;
        this.jobServiceConfiguration = jobServiceConfiguration;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        TimerJobEntityManager timerJobEntityManager = jobServiceConfiguration.getTimerJobEntityManager();
        for (TimerJobEntity timerJob : timerJobs) {
            TimerJobEntity dbTimerJob = timerJobEntityManager.findById(timerJob.getId());
            if (dbTimerJob != null) {
                dbTimerJob.setLockOwner(null);
                dbTimerJob.setLockExpirationTime(null);

            }
        }

        return null;
    }
}
