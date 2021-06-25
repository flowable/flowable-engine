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

import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandConfig;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.interceptor.CommandInvoker;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.job.api.Job;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.TimerJobService;
import org.flowable.job.service.impl.asyncexecutor.AcquireTimerJobsRunnable;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
class AcquireTimerJobsMultiNodeTest extends JobExecutorTestCase {

    protected CustomWaitCommandInvoker waitCommandInvoker;

    @Override
    protected void configureConfiguration(ProcessEngineConfigurationImpl processEngineConfiguration) {
        super.configureConfiguration(processEngineConfiguration);
        waitCommandInvoker = new CustomWaitCommandInvoker();
        processEngineConfiguration.setCommandInvoker(waitCommandInvoker);
    }

    @Test
    void testAcquireJobsInTheSameTime() {

        Instant now = Instant.now();
        processEngineConfiguration.getClock().setCurrentTime(Date.from(now));

        AsyncExecutor asyncExecutor = processEngineConfiguration.getAsyncExecutor();
        CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutor();

        String correlationId = commandExecutor.execute(commandContext -> {
            TimerJobEntity timer = createTweetTimer("i'm coding a test", Date.from(now.plusSeconds(10)));
            TimerJobService timerJobService = CommandContextUtil.getProcessEngineConfiguration(commandContext).getJobServiceConfiguration().getTimerJobService();
            timerJobService.scheduleTimerJob(timer);
            return timer.getCorrelationId();
        });

        processEngineConfiguration.getClock().setCurrentTime(Date.from(now.plusSeconds(30)));

        waitCommandInvoker.waitLatch = new CountDownLatch(1);
        waitCommandInvoker.workLatch = new CountDownLatch(2);

        ExecutorService executorService = Executors.newFixedThreadPool(2);

        JobServiceConfiguration jobServiceConfiguration = processEngineConfiguration.getJobServiceConfiguration();

        AcquireTimerJobsRunnable runnable1 = new AcquireTimerJobsRunnable(asyncExecutor, jobServiceConfiguration.getJobManager(), 1);
        CompletableFuture.runAsync(runnable1, executorService);

        AcquireTimerJobsRunnable runnable2 = new AcquireTimerJobsRunnable(asyncExecutor, jobServiceConfiguration.getJobManager(), 1);
        CompletableFuture.runAsync(runnable2, executorService);

        try {
            waitCommandInvoker.workLatch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        waitCommandInvoker.waitLatch.countDown();

        waitCommandInvoker.waitLatch = null;
        waitCommandInvoker.workLatch = null;

        runnable1.stop();
        runnable2.stop();
        try {
            executorService.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertThat(managementService.createTimerJobQuery().singleResult()).isNull();

        Job job = managementService.createJobQuery().singleResult();
        assertThat(job).isNotNull();
        assertThat(job.getCorrelationId()).isEqualTo(correlationId);
        assertThat(job).isInstanceOf(JobEntity.class);

        managementService.deleteJob(job.getId());
    }

    private static class CustomWaitCommandInvoker extends CommandInvoker {

        protected CountDownLatch workLatch;
        protected CountDownLatch waitLatch;

        public CustomWaitCommandInvoker() {
            super((commandContext, runnable) -> runnable.run());
        }

        @Override
        public <T> T execute(CommandConfig config, Command<T> command, CommandExecutor commandExecutor) {

            if (workLatch != null) {
                workLatch.countDown();
            }

            T result = super.execute(config, command, commandExecutor);

            if (waitLatch != null) {
                try {
                    waitLatch.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return result;
        }
    }
}
