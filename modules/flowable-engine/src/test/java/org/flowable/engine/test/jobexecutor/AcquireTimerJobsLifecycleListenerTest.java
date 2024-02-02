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

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.job.api.Job;
import org.flowable.job.service.TimerJobService;
import org.flowable.job.service.impl.asyncexecutor.AcquireJobsRunnableConfiguration;
import org.flowable.job.service.impl.asyncexecutor.AcquireTimerJobsRunnable;
import org.flowable.job.service.impl.asyncexecutor.AcquireTimerLifecycleListener;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

/**
 * @author Filip Hrisafov
 */
class AcquireTimerJobsLifecycleListenerTest extends JobExecutorTestCase {

    @Override
    protected void configureConfiguration(ProcessEngineConfigurationImpl processEngineConfiguration) {
        super.configureConfiguration(processEngineConfiguration);
        processEngineConfiguration.getAsyncExecutorConfiguration().setDefaultTimerJobAcquireWaitTime(Duration.ofSeconds(1));
        processEngineConfiguration.getAsyncExecutorConfiguration().setMaxTimerJobsPerAcquisition(1);
        if (processEngineConfiguration.getAsyncExecutor() != null) {
            processEngineConfiguration.getAsyncExecutor().setMaxTimerJobsPerAcquisition(1);
        }
    }

    @Test
    void lifecycleMethodsAreInvoked() throws InterruptedException {
        Instant now = Instant.now();
        AsyncExecutor asyncExecutor = processEngineConfiguration.getAsyncExecutor();
        CountDownLatch waitingLatch = new CountDownLatch(2);
        TestAcquireTimerLifecycleListener listener = new TestAcquireTimerLifecycleListener(waitingLatch);
        AcquireTimerJobsRunnable runnable = new AcquireTimerJobsRunnable(asyncExecutor,
            processEngineConfiguration.getJobServiceConfiguration().getJobManager(), listener, AcquireJobsRunnableConfiguration.DEFAULT, 1);

        CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutor();

        commandExecutor.execute(commandContext -> {

            TimerJobService timerJobService = CommandContextUtil.getProcessEngineConfiguration(commandContext).getJobServiceConfiguration()
                    .getTimerJobService();

            TimerJobEntity timer1 = createTweetTimer("Timer 1", Date.from(now.plusSeconds(10)));
            timerJobService.scheduleTimerJob(timer1);

            TimerJobEntity timer2 = createTweetTimer("Timer 2", Date.from(now.plusSeconds(20)));
            timerJobService.scheduleTimerJob(timer2);

            return null;
        });

        processEngineConfiguration.getClock().setCurrentTime(Date.from(now.plusSeconds(60)));

        assertThat(managementService.createTimerJobQuery().list()).hasSize(2);
        assertThat(managementService.createJobQuery().list()).isEmpty();

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        CompletableFuture.runAsync(runnable, executorService);

        if (waitingLatch.await(10, TimeUnit.SECONDS)) {
            runnable.stop();
        } else {
            throw new AssertionFailedError("Acquire timer runnable did not run twice");
        }

        assertThat(managementService.createTimerJobQuery().list()).isEmpty();
        List<Job> jobs = managementService.createJobQuery().list();
        assertThat(jobs).hasSize(2);

        jobs.forEach(job -> managementService.deleteJob(job.getId()));

        assertThat(listener.statesByEngine)
                .containsOnlyKeys(ScopeTypes.BPMN);

        assertThat(listener.statesByEngine.get(ScopeTypes.BPMN))
                .extracting(State::getJobsAcquired, State::getMaxTimerJobsPerAcquisition, State::getMillisToWait, State::isAcquireCycleStopped)
                .containsExactly(
                        tuple(0, 1, 1000L, true),
                        tuple(0, 1, 1000L, true),
                        tuple(1, 1, 0L, true),
                        tuple(1, 1, 0L, true)
                );
    }

    static class TestAcquireTimerLifecycleListener implements AcquireTimerLifecycleListener {

        protected final CountDownLatch waitingLatch;
        protected final Map<String, Deque<State>> statesByEngine = new LinkedHashMap<>();

        TestAcquireTimerLifecycleListener(CountDownLatch waitingLatch) {
            this.waitingLatch = waitingLatch;
        }

        @Override
        public void startAcquiring(String engineName, int maxTimerJobsPerAcquisition) {
            statesByEngine.computeIfAbsent(engineName, key -> new LinkedList<>()).addFirst(new State());
        }

        @Override
        public void stopAcquiring(String engineName) {
            State state = statesByEngine.get(engineName).getFirst();
            state.acquireCycleStopped = true;
        }

        @Override
        public void acquiredJobs(String engineName, int jobsAcquired, int maxTimerJobsPerAcquisition) {
            State state = statesByEngine.get(engineName).getFirst();
            state.jobsAcquired += jobsAcquired;
            state.maxTimerJobsPerAcquisition = maxTimerJobsPerAcquisition;
        }

        @Override
        public void startWaiting(String engineName, long millisToWait) {
            State state = statesByEngine.get(engineName).getFirst();
            state.millisToWait = millisToWait;
            waitingLatch.countDown();
        }
    }

    static class State {

        protected int jobsAcquired = 0;
        protected int maxTimerJobsPerAcquisition = 0;
        protected long millisToWait = 0;
        protected boolean acquireCycleStopped;

        public int getJobsAcquired() {
            return jobsAcquired;
        }

        public int getMaxTimerJobsPerAcquisition() {
            return maxTimerJobsPerAcquisition;
        }

        public long getMillisToWait() {
            return millisToWait;
        }

        public boolean isAcquireCycleStopped() {
            return acquireCycleStopped;
        }
    }
}
