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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.job.api.Job;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.JobEntityImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Tom Baeyens
 */
public class JobExecutorCmdExceptionTest extends PluggableFlowableTestCase {

    protected TweetExceptionHandler tweetExceptionHandler = new TweetExceptionHandler();

    private CommandExecutor commandExecutor;

    @BeforeEach
    public void setUp() throws Exception {
        processEngineConfiguration.addJobHandler(tweetExceptionHandler);
        this.commandExecutor = processEngineConfiguration.getCommandExecutor();
    }

    @AfterEach
    public void tearDown() throws Exception {
        processEngineConfiguration.removeJobHandler(tweetExceptionHandler.getType());
    }

    @Test
    public void testJobCommandsWith2Exceptions() {
        commandExecutor.execute(new Command<String>() {

            @Override
            public String execute(CommandContext commandContext) {
                JobEntity message = createTweetExceptionMessage();
                CommandContextUtil.getProcessEngineConfiguration(commandContext).getJobServiceConfiguration().getJobService().scheduleAsyncJob(message);
                return message.getId();
            }
        });

        Job job = managementService.createJobQuery().singleResult();
        assertThat(job.getRetries()).isEqualTo(3);

        String jobId = job.getId();
        assertThatThrownBy(() -> managementService.executeJob(jobId))
                .isInstanceOf(Exception.class);

        job = managementService.createTimerJobQuery().singleResult();
        assertThat(job.getRetries()).isEqualTo(2);

        assertThatThrownBy(() -> {
            managementService.moveTimerToExecutableJob(jobId);
            managementService.executeJob(jobId);
        })
                .isInstanceOf(Exception.class);

        job = managementService.createTimerJobQuery().singleResult();
        assertThat(job.getRetries()).isEqualTo(1);

        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());
    }

    @Test
    public void testJobCommandsWith3Exceptions() {
        tweetExceptionHandler.setExceptionsRemaining(3);

        commandExecutor.execute(new Command<String>() {

            @Override
            public String execute(CommandContext commandContext) {
                JobEntity message = createTweetExceptionMessage();
                CommandContextUtil.getProcessEngineConfiguration(commandContext).getJobServiceConfiguration().getJobService().scheduleAsyncJob(message);
                return message.getId();
            }
        });

        Job job = managementService.createJobQuery().singleResult();
        assertThat(job.getRetries()).isEqualTo(3);
        String jobId = job.getId();
        assertThatThrownBy(() -> managementService.executeJob(jobId))
                .isInstanceOf(Exception.class);

        job = managementService.createTimerJobQuery().singleResult();
        assertThat(job.getRetries()).isEqualTo(2);

        assertThatThrownBy(() -> {
            managementService.moveTimerToExecutableJob(jobId);
            managementService.executeJob(jobId);
        })
                .isInstanceOf(Exception.class);

        job = managementService.createTimerJobQuery().singleResult();
        assertThat(job.getRetries()).isEqualTo(1);

        assertThatThrownBy(() -> {
            managementService.moveTimerToExecutableJob(jobId);
            managementService.executeJob(jobId);
        })
                .isInstanceOf(Exception.class);

        job = managementService.createDeadLetterJobQuery().singleResult();
        assertThat(job).isNotNull();

        managementService.deleteDeadLetterJob(job.getId());
    }

    protected JobEntity createTweetExceptionMessage() {
        JobEntity message = new JobEntityImpl();
        message.setJobType(JobEntity.JOB_TYPE_MESSAGE);
        message.setRetries(3);
        message.setJobHandlerType("tweet-exception");
        return message;
    }
}
