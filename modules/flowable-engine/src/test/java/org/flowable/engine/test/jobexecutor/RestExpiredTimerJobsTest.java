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
package org.flowable.engine.test.jobexecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.job.api.Job;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.TimerJobService;
import org.flowable.job.service.impl.asyncexecutor.FindExpiredJobsCmd;
import org.flowable.job.service.impl.asyncexecutor.ResetExpiredJobsCmd;
import org.flowable.job.service.impl.persistence.entity.JobInfoEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
class RestExpiredTimerJobsTest extends JobExecutorTestCase {

    @Test
    void testRestExpiredTimerJobs() {

        Instant now = Instant.now();
        processEngineConfiguration.getClock().setCurrentTime(Date.from(now));

        CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutor();

        String lockedJobId = commandExecutor.execute(commandContext -> {
            TimerJobEntity timer = createTweetTimer("i'm coding a locked test", Date.from(now.plusSeconds(10)));
            TimerJobService timerJobService = CommandContextUtil.getProcessEngineConfiguration(commandContext).getJobServiceConfiguration().getTimerJobService();
            timerJobService.scheduleTimerJob(timer);
            return timer.getId();
        });

        commandExecutor.execute(commandContext -> {
            TimerJobService timerJobService = CommandContextUtil.getProcessEngineConfiguration(commandContext).getJobServiceConfiguration().getTimerJobService();
            TimerJobEntity timer = timerJobService.findTimerJobById(lockedJobId);
            timer.setLockOwner("test");
            timer.setLockExpirationTime(Date.from(now.plus(5, ChronoUnit.MINUTES)));
            return null;
        });

        Job lockedJob = managementService.createTimerJobQuery().jobId(lockedJobId).singleResult();
        assertThat(lockedJob).isNotNull();
        assertThat(((TimerJobEntity) lockedJob).getLockOwner()).isNotNull();
        assertThat(((TimerJobEntity) lockedJob).getLockExpirationTime()).isNotNull();

        String notLockedJobId = commandExecutor.execute(commandContext -> {
            TimerJobEntity timer = createTweetTimer("i'm coding an unlocked test", Date.from(now.plusSeconds(10)));
            TimerJobService timerJobService = CommandContextUtil.getProcessEngineConfiguration(commandContext).getJobServiceConfiguration().getTimerJobService();
            timerJobService.scheduleTimerJob(timer);
            return timer.getId();
        });

        int expiredJobsPagesSize = processEngineConfiguration.getAsyncExecutorResetExpiredJobsPageSize();

        JobServiceConfiguration jobServiceConfiguration = processEngineConfiguration.getJobServiceConfiguration();
        List<? extends JobInfoEntity> expiredJobs = managementService
                .executeCommand(new FindExpiredJobsCmd(expiredJobsPagesSize, jobServiceConfiguration.getTimerJobEntityManager(), jobServiceConfiguration));

        assertThat(expiredJobs).isEmpty();

        processEngineConfiguration.getClock().setCurrentTime(Date.from(now.plus(15, ChronoUnit.MINUTES)));

        expiredJobs = managementService
                .executeCommand(new FindExpiredJobsCmd(expiredJobsPagesSize, jobServiceConfiguration.getTimerJobEntityManager(), jobServiceConfiguration));

        assertThat(expiredJobs)
                .extracting(JobInfoEntity::getId, JobInfoEntity::getJobHandlerConfiguration)
                .containsExactlyInAnyOrder(
                        tuple(lockedJobId, "i'm coding a locked test")
                );

        managementService.executeCommand(new ResetExpiredJobsCmd(Collections.singleton(lockedJobId), jobServiceConfiguration.getTimerJobEntityManager(), jobServiceConfiguration));

        expiredJobs = managementService
                .executeCommand(new FindExpiredJobsCmd(expiredJobsPagesSize, jobServiceConfiguration.getTimerJobEntityManager(), jobServiceConfiguration));
        assertThat(expiredJobs).isEmpty();

        lockedJob = managementService.createTimerJobQuery().jobId(lockedJobId).singleResult();
        assertThat(lockedJob).isNotNull();
        assertThat(((TimerJobEntity) lockedJob).getLockOwner()).isNull();
        assertThat(((TimerJobEntity) lockedJob).getLockExpirationTime()).isNull();

        managementService.deleteTimerJob(notLockedJobId);
        managementService.deleteTimerJob(lockedJobId);
    }

}
