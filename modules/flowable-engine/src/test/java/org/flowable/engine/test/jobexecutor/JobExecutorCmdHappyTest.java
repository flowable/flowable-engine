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

import java.util.Date;
import java.util.List;

import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.job.api.Job;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.flowable.job.service.impl.cmd.AcquireTimerJobsCmd;
import org.flowable.job.service.impl.cmd.ExecuteAsyncJobCmd;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.junit.jupiter.api.Test;

/**
 * @author Tom Baeyens
 */
public class JobExecutorCmdHappyTest extends JobExecutorTestCase {

    @Test
    public void testJobCommandsWithMessage() {
        CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutor();

        String jobId = commandExecutor.execute(new Command<String>() {

            @Override
            public String execute(CommandContext commandContext) {
                JobEntity message = createTweetMessage("i'm coding a test");
                CommandContextUtil.getProcessEngineConfiguration(commandContext).getJobServiceConfiguration().getJobService().scheduleAsyncJob(message);
                return message.getId();
            }
        });

        Job job = managementService.createJobQuery().singleResult();
        assertThat(job).isNotNull();
        assertThat(job.getId()).isEqualTo(jobId);

        assertThat(tweetHandler.getMessages()).isEmpty();

        managementService.executeJob(job.getId());

        assertThat(tweetHandler.getMessages())
                .containsExactly("i'm coding a test");
    }

    static final long SOME_TIME = 928374923546L;
    static final long SECOND = 1000;

    @Test
    public void testJobCommandsWithTimer() {
        // clock gets automatically reset in LogTestCase.runTest
        processEngineConfiguration.getClock().setCurrentTime(new Date(SOME_TIME));

        AsyncExecutor asyncExecutor = processEngineConfiguration.getAsyncExecutor();
        CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutor();

        String jobId = commandExecutor.execute(new Command<String>() {

            @Override
            public String execute(CommandContext commandContext) {
                TimerJobEntity timer = createTweetTimer("i'm coding a test", new Date(SOME_TIME + (10 * SECOND)));
                CommandContextUtil.getProcessEngineConfiguration(commandContext).getJobServiceConfiguration().getTimerJobService().scheduleTimerJob(timer);
                return timer.getId();
            }
        });

        List<TimerJobEntity> acquiredJobs = commandExecutor.execute(new AcquireTimerJobsCmd(asyncExecutor));
        assertThat(acquiredJobs.size()).isZero();

        processEngineConfiguration.getClock().setCurrentTime(new Date(SOME_TIME + (20 * SECOND)));

        acquiredJobs = commandExecutor.execute(new AcquireTimerJobsCmd(asyncExecutor));
        assertThat(acquiredJobs.size()).isEqualTo(1);

        TimerJobEntity job = acquiredJobs.iterator().next();

        assertThat(job.getId()).isEqualTo(jobId);

        assertThat(tweetHandler.getMessages()).isEmpty();

        Job executableJob = managementService.moveTimerToExecutableJob(jobId);
        commandExecutor.execute(new ExecuteAsyncJobCmd(executableJob.getId(), processEngineConfiguration.getJobServiceConfiguration()));

        assertThat(tweetHandler.getMessages().get(0)).isEqualTo("i'm coding a test");
        assertThat(tweetHandler.getMessages()).hasSize(1);
    }
}