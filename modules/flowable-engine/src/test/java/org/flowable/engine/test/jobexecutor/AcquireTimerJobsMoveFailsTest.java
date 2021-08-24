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

import java.time.Instant;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableOptimisticLockingException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandConfig;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.engine.impl.cfg.DefaultInternalJobManager;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.interceptor.CommandInvoker;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.job.api.Job;
import org.flowable.job.service.TimerJobService;
import org.flowable.job.service.impl.asyncexecutor.AcquireTimerJobsRunnable;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.flowable.job.service.impl.cmd.AcquireTimerJobsCmd;
import org.flowable.job.service.impl.cmd.UnlockTimerJobsCmd;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
class AcquireTimerJobsMoveFailsTest extends JobExecutorTestCase {

    protected CustomThrowingInternalJobManager internalJobManager;
    protected CustomWaitCommandInvoker waitCommandInvoker;

    @Override
    protected void configureConfiguration(ProcessEngineConfigurationImpl processEngineConfiguration) {
        super.configureConfiguration(processEngineConfiguration);
        processEngineConfiguration.setAsyncExecutorMaxTimerJobsPerAcquisition(2);
        internalJobManager = new CustomThrowingInternalJobManager(processEngineConfiguration);
        processEngineConfiguration.setInternalJobManager(internalJobManager);
        waitCommandInvoker = new CustomWaitCommandInvoker();
        processEngineConfiguration.setCommandInvoker(waitCommandInvoker);
    }

    @Test
    void testMovingTimerJobToExecutableFails() throws InterruptedException {

        Instant now = Instant.now();
        processEngineConfiguration.getClock().setCurrentTime(Date.from(now));

        AsyncExecutor asyncExecutor = processEngineConfiguration.getAsyncExecutor();
        CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutor();

        String jobId = commandExecutor.execute(commandContext -> {
            TimerJobEntity timer = createTweetTimer("i'm coding a test", Date.from(now.plusSeconds(10)));
            TimerJobService timerJobService = CommandContextUtil.getProcessEngineConfiguration(commandContext).getJobServiceConfiguration().getTimerJobService();
            timerJobService.scheduleTimerJob(timer);
            return timer.getId();
        });

        processEngineConfiguration.getClock().setCurrentTime(Date.from(now.plusSeconds(30)));

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        waitCommandInvoker.acquireJobLatch = new CountDownLatch(1);
        waitCommandInvoker.unlockJobLatch = new CountDownLatch(1);
        internalJobManager.exceptionToThrow = new FlowableException("Moving job failed");

        AcquireTimerJobsRunnable runnable = new AcquireTimerJobsRunnable(asyncExecutor,
                processEngineConfiguration.getJobServiceConfiguration().getJobManager(), 1);
        CompletableFuture.runAsync(runnable, executorService);

        // wait for the acquire before stopping the loop
        waitCommandInvoker.acquireJobLatch.await(5, TimeUnit.SECONDS);
        runnable.stop();
        waitCommandInvoker.unlockJobLatch.await(5, TimeUnit.SECONDS);
        executorService.awaitTermination(5, TimeUnit.SECONDS);

        Job timerJob = managementService.createTimerJobQuery().singleResult();
        assertThat(timerJob).isNotNull();
        assertThat(timerJob.getId()).isEqualTo(jobId);
        assertThat(timerJob).isInstanceOf(TimerJobEntity.class);

        TimerJobEntity timerJobEntity = (TimerJobEntity) timerJob;
        assertThat(timerJobEntity.getLockOwner()).isNull();
        assertThat(timerJobEntity.getLockExpirationTime()).isNull();

        managementService.deleteTimerJob(jobId);
    }

    @Test
    void testMovingTimerJobToExecutableFailsDueToOptimisticLockingException() throws InterruptedException {

        Instant now = Instant.now();
        processEngineConfiguration.getClock().setCurrentTime(Date.from(now));

        AsyncExecutor asyncExecutor = processEngineConfiguration.getAsyncExecutor();
        CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutor();

        String jobId = commandExecutor.execute(commandContext -> {
            TimerJobEntity timer = createTweetTimer("i'm coding a test", Date.from(now.plusSeconds(10)));
            TimerJobService timerJobService = CommandContextUtil.getProcessEngineConfiguration(commandContext).getJobServiceConfiguration().getTimerJobService();
            timerJobService.scheduleTimerJob(timer);
            return timer.getId();
        });

        processEngineConfiguration.getClock().setCurrentTime(Date.from(now.plusSeconds(30)));

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        waitCommandInvoker.acquireJobLatch = new CountDownLatch(1);
        waitCommandInvoker.unlockJobLatch = new CountDownLatch(1);
        internalJobManager.exceptionToThrow = new FlowableOptimisticLockingException("Job already updated");

        AcquireTimerJobsRunnable runnable = new AcquireTimerJobsRunnable(asyncExecutor,
                processEngineConfiguration.getJobServiceConfiguration().getJobManager(), 1);
        CompletableFuture.runAsync(runnable, executorService);

        // wait for the acquire before stopping the loop
        waitCommandInvoker.acquireJobLatch.await(5, TimeUnit.SECONDS);
        runnable.stop();
        waitCommandInvoker.unlockJobLatch.await(5, TimeUnit.SECONDS);
        executorService.awaitTermination(5, TimeUnit.SECONDS);

        Job timerJob = managementService.createTimerJobQuery().singleResult();
        assertThat(timerJob).isNotNull();
        assertThat(timerJob.getId()).isEqualTo(jobId);
        assertThat(timerJob).isInstanceOf(TimerJobEntity.class);

        TimerJobEntity timerJobEntity = (TimerJobEntity) timerJob;
        assertThat(timerJobEntity.getLockOwner()).isNull();
        assertThat(timerJobEntity.getLockExpirationTime()).isNull();

        managementService.deleteTimerJob(jobId);
    }

    private static class CustomWaitCommandInvoker extends CommandInvoker {

        protected CountDownLatch acquireJobLatch;
        protected CountDownLatch unlockJobLatch;

        public CustomWaitCommandInvoker() {
            super((commandContext, runnable) -> runnable.run());
        }

        @Override
        public <T> T execute(CommandConfig config, Command<T> command, CommandExecutor commandExecutor) {

            T result = super.execute(config, command, commandExecutor);

            if (command instanceof AcquireTimerJobsCmd) {
                acquireJobLatch.countDown();
            } else if (command instanceof UnlockTimerJobsCmd) {
                unlockJobLatch.countDown();
            }

            return result;
        }
    }

    private static class CustomThrowingInternalJobManager extends DefaultInternalJobManager {

        protected FlowableException exceptionToThrow;

        public CustomThrowingInternalJobManager(ProcessEngineConfigurationImpl processEngineConfiguration) {
            super(processEngineConfiguration);
        }

        @Override
        protected boolean handleJobInsertInternal(Job job) {
            if (job instanceof JobEntity) {
                if (exceptionToThrow != null) {
                    throw exceptionToThrow;
                }
            }

            return super.handleJobInsertInternal(job);
        }
    }
}
