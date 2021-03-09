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
import org.flowable.job.service.impl.asyncexecutor.JobManager;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;

/**
 * @author Filip Hrisafov
 * @author Joram Barrez
 */
public class MoveTimerJobsToExecutableJobsCmd implements Command<Void> {

    protected JobManager jobManager;
    protected Collection<TimerJobEntity> timerJobs;
    protected boolean bulkMove;

    public MoveTimerJobsToExecutableJobsCmd(JobManager jobManager, Collection<TimerJobEntity> timerJobs, boolean bulkMove) {
        this.jobManager = jobManager;
        this.timerJobs = timerJobs;
        this.bulkMove = bulkMove;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        if (bulkMove) {
            jobManager.bulkMoveTimerJobsToExecutableJobs(timerJobs);

        } else {
            for (TimerJobEntity timerJob : timerJobs) {
                jobManager.moveTimerJobToExecutableJob(timerJob);
            }

        }
        return null;
    }
}
